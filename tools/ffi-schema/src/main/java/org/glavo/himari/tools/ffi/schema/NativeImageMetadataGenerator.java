package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Generates GraalVM Native Image reachability metadata for generated FFM bindings.
///
/// The generated foreign-call layouts describe the exact `ValueLayout.JAVA_*` layouts emitted by
/// [FfmJavaGenerator]. Callback descriptors are registered both as unbound function-pointer downcalls and as generic
/// upcalls because generated callback targets bind application callback and failure-sink state at run time.
@NotNullByDefault
public final class NativeImageMetadataGenerator {
    /// Fully qualified generated-binding support type used by callback adapters.
    private static final String CALLBACK_FAILURE_SINK = "org.glavo.himari.ffi.CallbackFailureSink";

    /// Fully qualified Java carrier used for addresses and aggregate values.
    private static final String MEMORY_SEGMENT = "java.lang.foreign.MemorySegment";

    /// Prevents instantiation of this utility class.
    private NativeImageMetadataGenerator() {
    }

    /// Generates the complete metadata document without writing it.
    ///
    /// @param schema the validated target-resolved ABI schema
    /// @return the deterministic UTF-8 JSON content
    /// @throws IllegalArgumentException if the schema uses a callable form unsupported by the Java generator
    public static String generate(AbiSchema schema) {
        Objects.requireNonNull(schema, "schema");
        AbiSchemaValidator.requireValid(schema);
        return new GenerationContext(schema).generate();
    }

    /// Generates and writes one metadata document.
    ///
    /// The parent directory is created when necessary and an existing file is replaced.
    ///
    /// @param schema the validated target-resolved ABI schema
    /// @param outputFile the metadata file to create
    /// @return `outputFile`
    /// @throws IllegalArgumentException if generation or writing fails
    public static Path generate(AbiSchema schema, Path outputFile) {
        Objects.requireNonNull(outputFile, "outputFile");
        String content = generate(schema);
        try {
            @Nullable Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputFile, content, StandardCharsets.UTF_8);
            return outputFile;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot write Native Image metadata " + outputFile, exception);
        }
    }

    /// Holds resolved schema indexes while emitting one deterministic metadata document.
    @NotNullByDefault
    private static final class GenerationContext {
        /// Source ABI schema.
        private final AbiSchema schema;

        /// Type declarations indexed by canonical name.
        private final Map<String, AbiSchema.TypeDefinition> types = new LinkedHashMap<>();

        /// Callback declarations indexed by canonical name.
        private final Map<String, AbiSchema.CallbackDefinition> callbacks = new LinkedHashMap<>();

        /// Fully qualified generated binding class name.
        private final String bindingClass;

        /// Creates one generation context.
        ///
        /// @param schema the source ABI schema
        private GenerationContext(AbiSchema schema) {
            this.schema = schema;
            for (AbiSchema.TypeDefinition type : schema.types()) {
                types.put(type.name(), type);
            }
            for (AbiSchema.CallbackDefinition callback : schema.callbacks()) {
                callbacks.put(callback.name(), callback);
            }
            this.bindingClass = schema.namespace() + '.' + upperCamel(schema.library()) + "FfmBindings";
            validateCallableProfile();
        }

        /// Generates the complete JSON document.
        ///
        /// @return deterministic metadata with one trailing newline
        private String generate() {
            List<ReflectionMethod> reflectionMethods = schema.callbacks().stream()
                    .map(this::reflectionMethod)
                    .toList();
            List<ForeignCall> downcalls = downcalls();
            List<ForeignCall> upcalls = deduplicateAndSort(schema.callbacks().stream()
                    .map(callback -> foreignCall(callback.result(), callback.parameters(), false))
                    .toList());

            StringBuilder output = new StringBuilder();
            output.append("{\n");
            boolean previousSection = false;
            if (!reflectionMethods.isEmpty()) {
                appendReflection(output, reflectionMethods);
                previousSection = true;
            }
            if (previousSection) {
                output.append(",\n");
            }
            appendForeign(output, downcalls, upcalls);
            return output.append("}\n").toString();
        }

        /// Rejects call forms that [FfmJavaGenerator] cannot emit exactly.
        private void validateCallableProfile() {
            for (AbiSchema.CallbackDefinition callback : schema.callbacks()) {
                if (callback.callingConvention() != AbiSchema.CallingConvention.SYSTEM) {
                    throw unsupported(callback.name(), "callback calling convention must be SYSTEM");
                }
            }
            for (AbiSchema.FunctionDefinition function : schema.functions()) {
                if (function.callingConvention() != AbiSchema.CallingConvention.SYSTEM) {
                    throw unsupported(function.name(), "function calling convention must be SYSTEM");
                }
                if (function.variadicFrom() != null) {
                    throw unsupported(function.name(), "variadic functions require explicit generated variants");
                }
            }
        }

        /// Returns every generated function and function-pointer downcall signature.
        ///
        /// @return deduplicated signatures in canonical order
        private @Unmodifiable List<ForeignCall> downcalls() {
            List<ForeignCall> calls = new ArrayList<>();
            for (AbiSchema.FunctionDefinition function : schema.functions()) {
                calls.add(foreignCall(
                        function.result(),
                        function.parameters(),
                        function.errorPolicy() != AbiSchema.ErrorPolicy.NONE
                ));
            }
            for (AbiSchema.CallbackDefinition callback : schema.callbacks()) {
                calls.add(foreignCall(callback.result(), callback.parameters(), false));
            }
            return deduplicateAndSort(calls);
        }

        /// Creates one generated callback-adapter reflection registration.
        ///
        /// @param callback the callback declaration
        /// @return the exact generated adapter method
        private ReflectionMethod reflectionMethod(AbiSchema.CallbackDefinition callback) {
            String callbackType = upperCamel(callback.name());
            List<String> parameters = new ArrayList<>();
            parameters.add(bindingClass + '$' + callbackType);
            parameters.add(CALLBACK_FAILURE_SINK);
            callback.parameters().stream()
                    .map(AbiSchema.Parameter::type)
                    .map(this::javaCarrier)
                    .forEach(parameters::add);
            return new ReflectionMethod("invoke" + callbackType, parameters);
        }

        /// Creates one foreign-call registration from a callable descriptor.
        ///
        /// @param result the callable return value
        /// @param parameters the ordered callable parameters
        /// @param captureCallState whether the generated downcall captures native error state
        /// @return the resolved foreign call
        private ForeignCall foreignCall(
                AbiSchema.ReturnValue result,
                List<AbiSchema.Parameter> parameters,
                boolean captureCallState
        ) {
            return new ForeignCall(
                    metadataLayout(result.type()),
                    parameters.stream().map(AbiSchema.Parameter::type).map(this::metadataLayout).toList(),
                    captureCallState
            );
        }

        /// Deduplicates registrations and sorts them independently of schema traversal details.
        ///
        /// @param calls source registrations
        /// @return immutable canonical registrations
        private static @Unmodifiable List<ForeignCall> deduplicateAndSort(List<ForeignCall> calls) {
            Set<ForeignCall> unique = new LinkedHashSet<>(calls);
            return unique.stream().sorted(ForeignCall.ORDER).toList();
        }

        /// Resolves one type to the GraalVM foreign-metadata layout syntax.
        ///
        /// @param reference the schema type reference
        /// @return the exact metadata layout
        private String metadataLayout(AbiSchema.TypeRef reference) {
            @Nullable AbiSchema.TypeDefinition type = types.get(reference.name());
            if (type == null) {
                if (callbacks.containsKey(reference.name())) {
                    return "void*";
                }
                throw unsupported(reference.name(), "unresolved metadata layout reference");
            }
            return switch (type) {
                case AbiSchema.PrimitiveType primitive -> primitiveLayout(primitive);
                case AbiSchema.PointerType ignored -> "void*";
                case AbiSchema.HandleType handle -> metadataLayout(handle.representation());
                case AbiSchema.AggregateType aggregate -> aggregateLayout(aggregate);
                case AbiSchema.IntegerSetType integerSet -> metadataLayout(integerSet.representation());
            };
        }

        /// Resolves one primitive emitted with `ValueLayout.JAVA_*`.
        ///
        /// @param primitive the primitive declaration
        /// @return the corresponding fixed JNI metadata type
        private static String primitiveLayout(AbiSchema.PrimitiveType primitive) {
            return switch (primitive.kind()) {
                case VOID -> "void";
                case INTEGER -> switch (primitive.byteSize()) {
                    case 1 -> "jbyte";
                    case 2 -> "jshort";
                    case 4 -> "jint";
                    case 8 -> "jlong";
                    default -> throw unsupported(primitive.name(), "unsupported integer metadata width");
                };
                case FLOATING -> switch (primitive.byteSize()) {
                    case 4 -> "jfloat";
                    case 8 -> "jdouble";
                    default -> throw unsupported(primitive.name(), "unsupported floating-point metadata width");
                };
            };
        }

        /// Resolves one aggregate layout including explicit structure padding.
        ///
        /// @param aggregate the aggregate declaration
        /// @return the nested struct or union metadata syntax
        private String aggregateLayout(AbiSchema.AggregateType aggregate) {
            List<String> elements = aggregate.kind() == AbiSchema.AggregateKind.STRUCT
                    ? structureElements(aggregate)
                    : unionElements(aggregate);
            String factory = aggregate.kind() == AbiSchema.AggregateKind.STRUCT ? "struct" : "union";
            return factory + '(' + String.join(",", elements) + ')';
        }

        /// Returns physical structure elements with bitfield storage deduplicated.
        ///
        /// @param aggregate the structure declaration
        /// @return ordered metadata elements
        private @Unmodifiable List<String> structureElements(AbiSchema.AggregateType aggregate) {
            Map<String, StorageField> bitfieldStorage = new LinkedHashMap<>();
            List<StorageField> fields = new ArrayList<>();
            for (AbiSchema.AggregateField field : aggregate.fields()) {
                if (field.bitOffset() == null) {
                    fields.add(storageField(field));
                } else {
                    String key = field.byteOffset() + ":" + field.type().name();
                    if (!bitfieldStorage.containsKey(key)) {
                        StorageField storage = storageField(field);
                        bitfieldStorage.put(key, storage);
                        fields.add(storage);
                    }
                }
            }
            fields.sort(Comparator.comparingLong(StorageField::byteOffset));
            List<String> elements = new ArrayList<>();
            long cursor = 0L;
            for (StorageField field : fields) {
                if (field.byteOffset() > cursor) {
                    elements.add(padding(field.byteOffset() - cursor));
                }
                elements.add(field.layout());
                cursor = field.byteOffset() + field.byteSize();
            }
            if (cursor < aggregate.byteSize()) {
                elements.add(padding(aggregate.byteSize() - cursor));
            }
            return List.copyOf(elements);
        }

        /// Returns union members plus the declared-size member emitted by [FfmJavaGenerator].
        ///
        /// @param aggregate the union declaration
        /// @return ordered metadata elements
        private @Unmodifiable List<String> unionElements(AbiSchema.AggregateType aggregate) {
            List<String> elements = aggregate.fields().stream()
                    .map(AbiSchema.AggregateField::type)
                    .map(this::metadataLayout)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            elements.add(padding(aggregate.byteSize()));
            return List.copyOf(elements);
        }

        /// Resolves one aggregate field to physical storage.
        ///
        /// @param field the source aggregate field
        /// @return its offset, size, and metadata layout
        private StorageField storageField(AbiSchema.AggregateField field) {
            return new StorageField(field.byteOffset(), layoutSize(field.type()), metadataLayout(field.type()));
        }

        /// Returns the byte size of one generated layout.
        ///
        /// @param reference the schema type reference
        /// @return the target-resolved byte size
        private long layoutSize(AbiSchema.TypeRef reference) {
            @Nullable AbiSchema.TypeDefinition type = types.get(reference.name());
            if (type == null) {
                if (callbacks.containsKey(reference.name())) {
                    return schema.target().addressSize();
                }
                throw unsupported(reference.name(), "unresolved layout-size reference");
            }
            return switch (type) {
                case AbiSchema.PrimitiveType primitive -> primitive.byteSize();
                case AbiSchema.PointerType ignored -> schema.target().addressSize();
                case AbiSchema.HandleType handle -> layoutSize(handle.representation());
                case AbiSchema.AggregateType aggregate -> aggregate.byteSize();
                case AbiSchema.IntegerSetType integerSet -> layoutSize(integerSet.representation());
            };
        }

        /// Resolves one schema reference to its generated Java callback-adapter carrier.
        ///
        /// @param reference the schema type reference
        /// @return the fully qualified carrier name or primitive keyword
        private String javaCarrier(AbiSchema.TypeRef reference) {
            @Nullable AbiSchema.TypeDefinition type = types.get(reference.name());
            if (type == null) {
                if (callbacks.containsKey(reference.name())) {
                    return MEMORY_SEGMENT;
                }
                throw unsupported(reference.name(), "unresolved Java carrier reference");
            }
            return switch (type) {
                case AbiSchema.PrimitiveType primitive -> switch (primitive.kind()) {
                    case VOID -> "void";
                    case INTEGER -> switch (primitive.byteSize()) {
                        case 1 -> "byte";
                        case 2 -> "short";
                        case 4 -> "int";
                        case 8 -> "long";
                        default -> throw unsupported(primitive.name(), "unsupported integer carrier width");
                    };
                    case FLOATING -> switch (primitive.byteSize()) {
                        case 4 -> "float";
                        case 8 -> "double";
                        default -> throw unsupported(primitive.name(), "unsupported floating-point carrier width");
                    };
                };
                case AbiSchema.PointerType ignored -> MEMORY_SEGMENT;
                case AbiSchema.HandleType handle -> javaCarrier(handle.representation());
                case AbiSchema.AggregateType ignored -> MEMORY_SEGMENT;
                case AbiSchema.IntegerSetType integerSet -> javaCarrier(integerSet.representation());
            };
        }

        /// Appends reflection metadata for the generated callback adapters.
        ///
        /// @param output destination JSON buffer
        /// @param methods adapter methods in schema order
        private void appendReflection(StringBuilder output, List<ReflectionMethod> methods) {
            output.append("  \"reflection\": [\n")
                    .append("    {\n")
                    .append("      \"type\": ").append(quote(bindingClass)).append(",\n")
                    .append("      \"methods\": [\n");
            for (int methodIndex = 0; methodIndex < methods.size(); methodIndex++) {
                ReflectionMethod method = methods.get(methodIndex);
                output.append("        {\n")
                        .append("          \"name\": ").append(quote(method.name())).append(",\n")
                        .append("          \"parameterTypes\": ");
                appendStringArray(output, method.parameterTypes(), 10);
                output.append("\n        }");
                output.append(methodIndex + 1 < methods.size() ? ",\n" : "\n");
            }
            output.append("      ]\n")
                    .append("    }\n")
                    .append("  ]");
        }

        /// Appends foreign downcall and upcall metadata.
        ///
        /// @param output destination JSON buffer
        /// @param downcalls exact generated downcall registrations
        /// @param upcalls exact generic upcall registrations
        private static void appendForeign(
                StringBuilder output,
                List<ForeignCall> downcalls,
                List<ForeignCall> upcalls
        ) {
            output.append("  \"foreign\": {\n")
                    .append("    \"downcalls\": [\n");
            appendForeignCalls(output, downcalls, 6);
            output.append("    ]");
            if (!upcalls.isEmpty()) {
                output.append(",\n")
                        .append("    \"upcalls\": [\n");
                appendForeignCalls(output, upcalls, 6);
                output.append("    ]\n");
            } else {
                output.append('\n');
            }
            output.append("  }\n");
        }

        /// Appends one array body of foreign-call registrations.
        ///
        /// @param output destination JSON buffer
        /// @param calls registrations to append
        /// @param indentation object indentation width
        private static void appendForeignCalls(StringBuilder output, List<ForeignCall> calls, int indentation) {
            String prefix = " ".repeat(indentation);
            for (int index = 0; index < calls.size(); index++) {
                ForeignCall call = calls.get(index);
                output.append(prefix).append("{\n")
                        .append(prefix).append("  \"returnType\": ").append(quote(call.returnType())).append(",\n")
                        .append(prefix).append("  \"parameterTypes\": ");
                appendStringArray(output, call.parameterTypes(), indentation + 2);
                if (call.captureCallState()) {
                    output.append(",\n")
                            .append(prefix).append("  \"options\": {\n")
                            .append(prefix).append("    \"captureCallState\": true\n")
                            .append(prefix).append("  }\n");
                } else {
                    output.append('\n');
                }
                output.append(prefix).append('}');
                output.append(index + 1 < calls.size() ? ",\n" : "\n");
            }
        }
    }

    /// Describes one generated callback adapter method.
    ///
    /// @param name exact generated static method name
    /// @param parameterTypes exact Java parameter types in declaration order
    @NotNullByDefault
    private record ReflectionMethod(String name, @Unmodifiable List<String> parameterTypes) {
        /// Creates an immutable reflection method registration.
        private ReflectionMethod {
            parameterTypes = List.copyOf(parameterTypes);
        }
    }

    /// Describes one foreign stub signature and linker-option shape.
    ///
    /// @param returnType Native Image metadata return layout
    /// @param parameterTypes Native Image metadata parameter layouts
    /// @param captureCallState whether the stub captures ambient native error state
    @NotNullByDefault
    private record ForeignCall(
            String returnType,
            @Unmodifiable List<String> parameterTypes,
            boolean captureCallState
    ) {
        /// Canonical signature ordering independent of schema declaration traversal.
        private static final Comparator<ForeignCall> ORDER = Comparator
                .comparing(ForeignCall::returnType)
                .thenComparing(call -> String.join("\u0000", call.parameterTypes()))
                .thenComparing(ForeignCall::captureCallState);

        /// Creates an immutable foreign-call registration.
        private ForeignCall {
            parameterTypes = List.copyOf(parameterTypes);
        }
    }

    /// Describes one physical aggregate storage field.
    ///
    /// @param byteOffset target-resolved byte offset
    /// @param byteSize target-resolved storage size
    /// @param layout metadata layout syntax
    @NotNullByDefault
    private record StorageField(long byteOffset, long byteSize, String layout) {
    }

    /// Formats one positive padding layout.
    ///
    /// @param byteSize padding size in bytes
    /// @return the metadata padding syntax
    private static String padding(long byteSize) {
        if (byteSize <= 0L) {
            throw new IllegalArgumentException("Padding size must be positive: " + byteSize);
        }
        return "padding(" + byteSize + ')';
    }

    /// Converts an ABI identifier to the upper-camel spelling used by [FfmJavaGenerator].
    ///
    /// @param value the ABI identifier
    /// @return the generated type name
    private static String upperCamel(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean capitalize = true;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '_') {
                capitalize = true;
            } else if (capitalize) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    /// Appends one JSON string array using expanded formatting for non-empty values.
    ///
    /// @param output destination JSON buffer
    /// @param values raw string values
    /// @param indentation indentation width of the array property
    private static void appendStringArray(StringBuilder output, List<String> values, int indentation) {
        if (values.isEmpty()) {
            output.append("[]");
            return;
        }
        String elementPrefix = " ".repeat(indentation + 2);
        String closingPrefix = " ".repeat(indentation);
        output.append("[\n");
        for (int index = 0; index < values.size(); index++) {
            output.append(elementPrefix).append(quote(values.get(index)));
            output.append(index + 1 < values.size() ? ",\n" : "\n");
        }
        output.append(closingPrefix).append(']');
    }

    /// Returns a JSON string literal.
    ///
    /// @param value the raw string value
    /// @return escaped and quoted JSON text
    private static String quote(String value) {
        StringBuilder output = new StringBuilder(value.length() + 2).append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20) {
                        output.append("\\u").append(String.format("%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        return output.append('"').toString();
    }

    /// Creates an unsupported-generator-profile failure.
    ///
    /// @param declaration the failing declaration
    /// @param detail the unsupported feature
    /// @return the exception to throw
    private static IllegalArgumentException unsupported(String declaration, String detail) {
        return new IllegalArgumentException(
                "Cannot generate Native Image metadata for FFM declaration '" + declaration + "': " + detail
        );
    }
}
