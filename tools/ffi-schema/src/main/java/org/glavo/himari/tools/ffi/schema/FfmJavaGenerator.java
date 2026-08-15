package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Generates fixed-signature Java 25 FFM layouts, downcalls, and contained upcalls from an [AbiSchema].
///
/// The first generator profile accepts target-resolved fixed functions using the system calling convention,
/// immediate `errno` or `GetLastError` capture, and function pointers whose return values have scalar, address, or
/// aggregate carriers. It deliberately rejects declarations that cannot yet be emitted without weakening exact
/// invocation typing.
@NotNullByDefault
public final class FfmJavaGenerator {
    /// Java language keywords and restricted literals that cannot be emitted as declaration names.
    private static final Set<String> JAVA_RESERVED_WORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "false",
            "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
            "int", "interface", "long", "native", "new", "null", "package", "private", "protected",
            "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "true", "try", "var", "void", "volatile", "while",
            "yield", "record", "sealed", "permits", "non-sealed", "when", "_"
    );

    /// Prevents instantiation of this utility class.
    private FfmJavaGenerator() {
    }

    /// Generates the layout and binding sources for one canonical schema.
    ///
    /// Existing files with the two generated names are replaced. Other files in `outputDirectory` are preserved.
    ///
    /// @param schema the validated target-resolved ABI schema
    /// @param outputDirectory the generated-source root
    /// @return the two generated source paths in stable order
    /// @throws IllegalArgumentException if the schema uses an unsupported signature or the files cannot be written
    public static @Unmodifiable List<Path> generate(AbiSchema schema, Path outputDirectory) {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        AbiSchemaValidator.requireValid(schema);

        GenerationContext context = new GenerationContext(schema);
        context.validateSupportedProfile();
        Path packageDirectory = outputDirectory.resolve(schema.namespace().replace('.', '/'));
        Path layoutsPath = packageDirectory.resolve(context.prefix() + "Layouts.java");
        Path bindingsPath = packageDirectory.resolve(context.prefix() + "FfmBindings.java");
        try {
            Files.createDirectories(packageDirectory);
            Files.writeString(layoutsPath, context.generateLayouts(), StandardCharsets.UTF_8);
            Files.writeString(bindingsPath, context.generateBindings(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot write generated FFM sources under " + outputDirectory, exception);
        }
        return List.of(layoutsPath, bindingsPath);
    }

    /// Holds resolved schema indexes while emitting one deterministic source pair.
    @NotNullByDefault
    private static final class GenerationContext {
        /// The source schema.
        private final AbiSchema schema;

        /// Type declarations indexed by canonical name.
        private final Map<String, AbiSchema.TypeDefinition> types;

        /// Callback declarations indexed by canonical name.
        private final Map<String, AbiSchema.CallbackDefinition> callbacks;

        /// The generated Java class prefix.
        private final String prefix;

        /// Creates a generation context and its declaration indexes.
        ///
        /// @param schema the source schema
        private GenerationContext(AbiSchema schema) {
            this.schema = schema;
            this.types = new LinkedHashMap<>();
            for (AbiSchema.TypeDefinition type : schema.types()) {
                types.put(type.name(), type);
            }
            this.callbacks = new LinkedHashMap<>();
            for (AbiSchema.CallbackDefinition callback : schema.callbacks()) {
                callbacks.put(callback.name(), callback);
            }
            this.prefix = upperCamel(schema.library());
        }

        /// Returns the generated class prefix.
        ///
        /// @return the class prefix
        private String prefix() {
            return prefix;
        }

        /// Rejects schema features not representable by the first exact-signature generator profile.
        private void validateSupportedProfile() {
            requireJavaIdentifier(
                    prefix,
                    "class prefix derived from library '" + schema.library() + "'"
            );
            Set<String> layoutNames = new HashSet<>();
            for (AbiSchema.TypeDefinition type : schema.types()) {
                String layoutName = constant(type.name());
                requireJavaIdentifier(layoutName, "layout constant derived from type '" + type.name() + "'");
                requireUniqueGeneratedName(layoutNames, layoutName, "type layout", type.name());
                if (type instanceof AbiSchema.AggregateType aggregate) {
                    Set<String> fieldNames = new HashSet<>();
                    for (AbiSchema.AggregateField field : aggregate.fields()) {
                        requireUniqueGeneratedName(
                                fieldNames,
                                constant(field.name()),
                                "aggregate field",
                                aggregate.name() + "." + field.name()
                        );
                    }
                }
            }

            Set<String> descriptorNames = new HashSet<>();
            Set<String> callbackTypeNames = new HashSet<>();
            for (AbiSchema.CallbackDefinition callback : schema.callbacks()) {
                String callbackTypeName = upperCamel(callback.name());
                requireJavaIdentifier(
                        callbackTypeName,
                        "callback interface derived from '" + callback.name() + "'"
                );
                requireUniqueGeneratedName(
                        descriptorNames,
                        constant(callback.name()) + "_DESCRIPTOR",
                        "callable descriptor",
                        callback.name()
                );
                requireUniqueGeneratedName(
                        callbackTypeNames,
                        callbackTypeName,
                        "callback interface",
                        callback.name()
                );
                if (callback.callingConvention() != AbiSchema.CallingConvention.SYSTEM) {
                    throw unsupported(callback.name(), "callback calling convention must be SYSTEM");
                }
                validateParameters(callback.name(), callback.parameters());
            }

            for (AbiSchema.FunctionDefinition function : schema.functions()) {
                requireJavaIdentifier(function.name(), "function '" + function.name() + "'");
                requireUniqueGeneratedName(
                        descriptorNames,
                        constant(function.name()) + "_DESCRIPTOR",
                        "callable descriptor",
                        function.name()
                );
                if (function.callingConvention() != AbiSchema.CallingConvention.SYSTEM) {
                    throw unsupported(function.name(), "function calling convention must be SYSTEM");
                }
                if (function.variadicFrom() != null) {
                    throw unsupported(function.name(), "variadic functions require explicit generated variants");
                }
                validateParameters(function.name(), function.parameters());
            }
        }

        /// Validates generated Java names for one parameter list.
        ///
        /// @param callableName the owning callable name
        /// @param parameters the ordered parameters
        private static void validateParameters(String callableName, List<AbiSchema.Parameter> parameters) {
            for (AbiSchema.Parameter parameter : parameters) {
                requireJavaIdentifier(
                        parameter.name(),
                        "parameter '" + callableName + "." + parameter.name() + "'"
                );
            }
        }

        /// Emits the generated layout and descriptor class.
        ///
        /// @return the complete Java source
        private String generateLayouts() {
            String className = prefix + "Layouts";
            StringBuilder source = new StringBuilder();
            line(source, 0, "package " + schema.namespace() + ";");
            line(source, 0, "");
            line(source, 0, "import org.jetbrains.annotations.NotNullByDefault;");
            line(source, 0, "");
            line(source, 0, "import java.lang.foreign.FunctionDescriptor;");
            line(source, 0, "import java.lang.foreign.MemoryLayout;");
            line(source, 0, "import java.lang.foreign.ValueLayout;");
            line(source, 0, "import java.nio.ByteOrder;");
            line(source, 0, "");
            line(source, 0, "/// Defines target-resolved layouts and exact function descriptors generated from `"
                    + javaDoc(schema.library()) + "`.");
            line(source, 0, "@SuppressWarnings(\"restricted\")");
            line(source, 0, "@NotNullByDefault");
            line(source, 0, "public final class " + className + " {");
            line(source, 1, "/// Prevents instantiation of this generated utility class.");
            line(source, 1, "private " + className + "() {");
            line(source, 1, "}");

            for (AbiSchema.TypeDefinition type : nonAggregateTypes()) {
                if (type instanceof AbiSchema.PrimitiveType primitive
                        && primitive.kind() == AbiSchema.PrimitiveKind.VOID) {
                    continue;
                }
                line(source, 0, "");
                line(source, 1, "/// Layout of `" + javaDoc(type.name()) + "`.");
                line(source, 1, "public static final MemoryLayout " + constant(type.name()) + " = "
                        + layoutExpression(type.name(), type.name()) + ";");
            }
            for (AbiSchema.AggregateType aggregate : orderedAggregates()) {
                line(source, 0, "");
                line(source, 1, "/// Layout of `" + javaDoc(aggregate.name()) + "`.");
                line(source, 1, "public static final MemoryLayout " + constant(aggregate.name()) + " =");
                appendAggregateExpression(source, aggregate, 2);
                source.append(";\n");
                for (AbiSchema.AggregateField field : aggregate.fields()) {
                    String base = constant(aggregate.name()) + "_" + constant(field.name());
                    line(source, 0, "");
                    line(source, 1, "/// Byte offset of `" + javaDoc(aggregate.name() + "." + field.name()) + "`.");
                    line(source, 1, "public static final long " + base + "_OFFSET = " + field.byteOffset() + "L;");
                    if (field.bitOffset() != null) {
                        line(source, 0, "");
                        line(source, 1, "/// Bit offset of `" + javaDoc(aggregate.name() + "." + field.name())
                                + "` within its storage unit.");
                        line(source, 1, "public static final int " + base + "_BIT_OFFSET = "
                                + field.bitOffset() + ";");
                        line(source, 0, "");
                        line(source, 1, "/// Bit width of `" + javaDoc(aggregate.name() + "." + field.name()) + "`.");
                        line(source, 1, "public static final int " + base + "_BIT_WIDTH = "
                                + field.bitWidth() + ";");
                    }
                }
            }

            for (AbiSchema.CallbackDefinition callback : schema.callbacks()) {
                line(source, 0, "");
                line(source, 1, "/// Address layout of callback `" + javaDoc(callback.name()) + "`.");
                line(source, 1, "public static final MemoryLayout " + constant(callback.name())
                        + "_POINTER = ValueLayout.ADDRESS.withByteAlignment("
                        + schema.target().addressAlignment() + "L).withName(\"" + javaString(callback.name()) + "\");");
                line(source, 0, "");
                line(source, 1, "/// Exact descriptor of callback `" + javaDoc(callback.name()) + "`.");
                line(source, 1, "public static final FunctionDescriptor " + constant(callback.name())
                        + "_DESCRIPTOR = " + descriptorExpression(callback.result(), callback.parameters()) + ";");
            }
            for (AbiSchema.FunctionDefinition function : schema.functions()) {
                line(source, 0, "");
                line(source, 1, "/// Exact descriptor of function `" + javaDoc(function.name()) + "`.");
                line(source, 1, "public static final FunctionDescriptor " + constant(function.name())
                        + "_DESCRIPTOR = " + descriptorExpression(function.result(), function.parameters()) + ";");
            }
            line(source, 0, "}");
            return source.toString();
        }

        /// Emits the generated exact-invocation binding class.
        ///
        /// @return the complete Java source
        private String generateBindings() {
            String layoutsClass = prefix + "Layouts";
            String className = prefix + "FfmBindings";
            boolean hasAggregateReturn = schema.functions().stream()
                    .anyMatch(function -> resolvedType(function.result().type()) instanceof AbiSchema.AggregateType)
                    || schema.callbacks().stream()
                    .anyMatch(callback -> resolvedType(callback.result().type()) instanceof AbiSchema.AggregateType);
            boolean hasCallbacks = !schema.callbacks().isEmpty();
            boolean hasNativeErrorCapture = schema.functions().stream()
                    .anyMatch(function -> function.errorPolicy() != AbiSchema.ErrorPolicy.NONE);

            StringBuilder source = new StringBuilder();
            line(source, 0, "package " + schema.namespace() + ";");
            line(source, 0, "");
            if (hasCallbacks) {
                line(source, 0, "import org.glavo.himari.ffi.CallbackFailureSink;");
            }
            line(source, 0, "import org.jetbrains.annotations.NotNullByDefault;");
            line(source, 0, "");
            line(source, 0, "import java.lang.foreign.Arena;");
            line(source, 0, "import java.lang.foreign.Linker;");
            if (hasNativeErrorCapture) {
                line(source, 0, "import java.lang.foreign.MemoryLayout;");
            }
            line(source, 0, "import java.lang.foreign.MemorySegment;");
            if (hasAggregateReturn) {
                line(source, 0, "import java.lang.foreign.SegmentAllocator;");
            }
            line(source, 0, "import java.lang.foreign.SymbolLookup;");
            line(source, 0, "import java.lang.invoke.MethodHandle;");
            if (hasCallbacks) {
                line(source, 0, "import java.lang.invoke.MethodHandles;");
            }
            line(source, 0, "import java.lang.invoke.MethodType;");
            if (hasNativeErrorCapture) {
                line(source, 0, "import java.lang.foreign.ValueLayout;");
            }
            line(source, 0, "import java.util.Objects;");
            line(source, 0, "");
            line(source, 0, "/// Provides fixed-signature FFM calls generated from `" + javaDoc(schema.library()) + "`.");
            line(source, 0, "///");
            line(source, 0, "/// The supplied symbol lookup and every callback arena must remain alive while their handles are used.");
            line(source, 0, "@SuppressWarnings(\"restricted\")");
            line(source, 0, "@NotNullByDefault");
            line(source, 0, "public final class " + className + " {");
            line(source, 1, "/// The process native linker shared by this generated binding.");
            line(source, 1, "private static final Linker LINKER = Linker.nativeLinker();");

            if (hasNativeErrorCapture) {
                line(source, 0, "");
                line(source, 1, "/// Native layout used to capture thread-local call state immediately after a downcall.");
                line(source, 1, "private static final MemoryLayout CAPTURE_STATE_LAYOUT = Linker.Option.captureStateLayout();");
                for (AbiSchema.ErrorPolicy policy : AbiSchema.ErrorPolicy.values()) {
                    if (policy == AbiSchema.ErrorPolicy.NONE || schema.functions().stream()
                            .noneMatch(function -> function.errorPolicy() == policy)) {
                        continue;
                    }
                    line(source, 0, "");
                    line(source, 1, "/// Byte offset of captured native state `" + captureStateName(policy) + "`.");
                    line(source, 1, "private static final long " + captureOffsetConstant(policy) + " =");
                    line(source, 2, "CAPTURE_STATE_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(\""
                            + captureStateName(policy) + "\"));");
                }
            }

            for (AbiSchema.CallbackDefinition callback : schema.callbacks()) {
                String callbackType = upperCamel(callback.name());
                line(source, 0, "");
                line(source, 1, "/// Fixed adapter handle for callback `" + javaDoc(callback.name()) + "`.");
                line(source, 1, "private static final MethodHandle " + constant(callback.name()) + "_ADAPTER = callbackAdapter(");
                line(source, 2, "\"invoke" + callbackType + "\",");
                line(source, 2, callbackMethodType(callback, true));
                line(source, 1, ");");
                line(source, 0, "");
                line(source, 1, "/// Exact unbound downcall handle for function pointer `"
                        + javaDoc(callback.name()) + "`.");
                line(source, 1, "private static final MethodHandle " + constant(callback.name())
                        + "_DOWNCALL = requireType(");
                line(source, 2, "LINKER.downcallHandle(" + prefix + "Layouts."
                        + constant(callback.name()) + "_DESCRIPTOR),");
                line(source, 2, callbackDowncallMethodType(callback) + ",");
                line(source, 2, "\"" + javaString(callback.name()) + " function pointer\"");
                line(source, 1, ");");
                if (resolvedType(callback.result().type()) instanceof AbiSchema.AggregateType) {
                    line(source, 0, "");
                    line(source, 1, "/// Process-lifetime zero value returned after a contained aggregate callback failure.");
                    line(source, 1, "private static final MemorySegment " + constant(callback.name())
                            + "_ZERO_RETURN = Arena.global().allocate(" + prefix + "Layouts."
                            + constant(callback.result().type().name()) + ");");
                }
            }

            for (AbiSchema.FunctionDefinition function : schema.functions()) {
                line(source, 0, "");
                line(source, 1, "/// Exact downcall handle for native symbol `" + javaDoc(function.symbol()) + "`.");
                line(source, 1, "private final MethodHandle " + function.name() + "Handle;");
            }

            line(source, 0, "");
            line(source, 1, "/// Links every required native symbol once and verifies its exact method type.");
            line(source, 1, "///");
            line(source, 1, "/// @param symbols the fixed native library lookup");
            line(source, 1, "/// @throws java.util.NoSuchElementException if a required symbol is absent");
            line(source, 1, "/// @throws IllegalArgumentException if a linked handle has an incompatible descriptor");
            line(source, 1, "public " + className + "(SymbolLookup symbols) {");
            line(source, 2, "Objects.requireNonNull(symbols, \"symbols\");");
            for (AbiSchema.FunctionDefinition function : schema.functions()) {
                line(source, 2, "this." + function.name() + "Handle = requireType(");
                line(source, 3, "LINKER.downcallHandle(symbols.findOrThrow(\""
                        + javaString(function.symbol()) + "\"), " + layoutsClass + "."
                        + constant(function.name()) + "_DESCRIPTOR" + linkerOptionSuffix(function) + "),");
                line(source, 3, functionMethodType(function) + ",");
                line(source, 3, "\"" + javaString(function.symbol()) + "\"");
                line(source, 2, ");");
            }
            line(source, 1, "}");

            for (AbiSchema.FunctionDefinition function : schema.functions()) {
                appendFunctionMethod(source, function);
            }
            for (AbiSchema.CallbackDefinition callback : schema.callbacks()) {
                appendCallbackMembers(source, callback, layoutsClass);
            }

            line(source, 0, "");
            line(source, 1, "/// Requires a linked handle to have the generated exact carrier type.");
            line(source, 1, "///");
            line(source, 1, "/// @param handle the linked downcall handle");
            line(source, 1, "/// @param expected the generated exact type");
            line(source, 1, "/// @param symbol the native symbol used in diagnostics");
            line(source, 1, "/// @return `handle` after verification");
            line(source, 1, "private static MethodHandle requireType(MethodHandle handle, MethodType expected, String symbol) {");
            line(source, 2, "if (!handle.type().equals(expected)) {");
            line(source, 3, "throw new IllegalArgumentException(\"Unexpected method type for \" + symbol");
            line(source, 4, "+ \": expected \" + expected + \", got \" + handle.type());");
            line(source, 2, "}");
            line(source, 2, "return handle;");
            line(source, 1, "}");

            if (hasCallbacks) {
                line(source, 0, "");
                line(source, 1, "/// Resolves one generated static callback adapter.");
                line(source, 1, "///");
                line(source, 1, "/// @param name the fixed adapter method name");
                line(source, 1, "/// @param type the fixed adapter method type");
                line(source, 1, "/// @return the resolved adapter handle");
                line(source, 1, "private static MethodHandle callbackAdapter(String name, MethodType type) {");
                line(source, 2, "try {");
                line(source, 3, "return MethodHandles.lookup().findStatic(" + className + ".class, name, type);");
                line(source, 2, "} catch (ReflectiveOperationException exception) {");
                line(source, 3, "throw new ExceptionInInitializerError(exception);");
                line(source, 2, "}");
                line(source, 1, "}");

                line(source, 0, "");
                line(source, 1, "/// Publishes a callback failure without allowing a sink failure to cross the native boundary.");
                line(source, 1, "///");
                line(source, 1, "/// @param failures the application failure sink");
                line(source, 1, "/// @param failure the contained callback failure");
                line(source, 1, "private static void publishFailure(CallbackFailureSink failures, Throwable failure) {");
                line(source, 2, "try {");
                line(source, 3, "failures.publish(failure);");
                line(source, 2, "} catch (Throwable ignored) {");
                line(source, 3, "// Containment takes precedence over diagnostics at a native callback boundary.");
                line(source, 2, "}");
                line(source, 1, "}");
            }
            line(source, 0, "}");
            return source.toString();
        }

        /// Appends one generated fixed-signature downcall method.
        ///
        /// @param source the output buffer
        /// @param function the function declaration
        private void appendFunctionMethod(StringBuilder source, AbiSchema.FunctionDefinition function) {
            @Nullable AbiSchema.TypeDefinition resultType = resolvedType(function.result().type());
            boolean aggregateReturn = resultType instanceof AbiSchema.AggregateType;
            boolean capturesError = function.errorPolicy() != AbiSchema.ErrorPolicy.NONE;
            String nativeReturnType = carrierType(function.result().type());
            String returnType = capturesError ? errorResultType(function) : nativeReturnType;
            Set<String> occupiedNames = mutableParameterNames(function.parameters());
            String resultAllocatorName = aggregateReturn
                    ? reserveGeneratedName(occupiedNames, "resultAllocator")
                    : "";
            String callStateArenaName = capturesError
                    ? reserveGeneratedName(occupiedNames, "callStateArena")
                    : "";
            String callStateName = capturesError
                    ? reserveGeneratedName(occupiedNames, "callState")
                    : "";
            String nativeResultName = capturesError && !isVoid(function.result().type())
                    ? reserveGeneratedName(occupiedNames, "nativeResult")
                    : "";
            String errorCodeName = capturesError
                    ? reserveGeneratedName(occupiedNames, "errorCode")
                    : "";
            String caughtFailureName = reserveGeneratedName(occupiedNames, "failure");
            line(source, 0, "");
            line(source, 1, "/// Invokes native symbol `" + javaDoc(function.symbol()) + "` with its exact generated type.");
            line(source, 1, "///");
            if (aggregateReturn) {
                line(source, 1, "/// @param " + resultAllocatorName
                        + " the allocator that owns the returned aggregate segment");
            }
            for (AbiSchema.Parameter parameter : function.parameters()) {
                line(source, 1, "/// @param " + parameter.name() + " native parameter `" + javaDoc(parameter.name()) + "`");
            }
            if (capturesError) {
                line(source, 1, "/// @return the native return value and immediately captured `"
                        + captureStateName(function.errorPolicy()) + "` state");
            } else if (!isVoid(function.result().type())) {
                line(source, 1, "/// @return the native return value");
            }
            line(source, 1, "/// @throws IllegalStateException if a symbol or argument segment scope is no longer alive");
            StringBuilder declaration = new StringBuilder("public ").append(returnType).append(' ')
                    .append(function.name()).append('(');
            List<String> parameters = new ArrayList<>();
            if (aggregateReturn) {
                parameters.add("SegmentAllocator " + resultAllocatorName);
            }
            for (AbiSchema.Parameter parameter : function.parameters()) {
                parameters.add(carrierType(parameter.type()) + " " + parameter.name());
            }
            declaration.append(String.join(", ", parameters)).append(") {");
            line(source, 1, declaration.toString());
            line(source, 2, capturesError
                    ? "try (Arena " + callStateArenaName + " = Arena.ofConfined()) {"
                    : "try {");
            String arguments = invocationArguments(
                    function.parameters(),
                    resultAllocatorName,
                    callStateName
            );
            if (capturesError) {
                line(source, 3, "MemorySegment " + callStateName + " = " + callStateArenaName
                        + ".allocate(CAPTURE_STATE_LAYOUT);");
            }
            if (isVoid(function.result().type())) {
                line(source, 3, function.name() + "Handle.invokeExact(" + arguments + ");");
                if (capturesError) {
                    line(source, 3, "int " + errorCodeName + " = " + callStateName
                            + ".get(ValueLayout.JAVA_INT, "
                            + captureOffsetConstant(function.errorPolicy()) + ");");
                    line(source, 3, "return new " + returnType + "(" + errorCodeName + ");");
                }
            } else {
                if (capturesError) {
                    line(source, 3, nativeReturnType + " " + nativeResultName + " = ("
                            + nativeReturnType + ") " + function.name()
                            + "Handle.invokeExact(" + arguments + ");");
                    line(source, 3, "int " + errorCodeName + " = " + callStateName
                            + ".get(ValueLayout.JAVA_INT, "
                            + captureOffsetConstant(function.errorPolicy()) + ");");
                    line(source, 3, "return new " + returnType + "(" + nativeResultName + ", "
                            + errorCodeName + ");");
                } else {
                    line(source, 3, "return (" + nativeReturnType + ") " + function.name()
                            + "Handle.invokeExact(" + arguments + ");");
                }
            }
            line(source, 2, "} catch (RuntimeException | Error " + caughtFailureName + ") {");
            line(source, 3, "throw " + caughtFailureName + ";");
            line(source, 2, "} catch (Throwable " + caughtFailureName + ") {");
            line(source, 3, "throw new AssertionError(\"Unexpected checked failure from "
                    + javaString(function.symbol()) + "\", " + caughtFailureName + ");");
            line(source, 2, "}");
            line(source, 1, "}");

            if (capturesError) {
                appendErrorResultRecord(source, function, nativeReturnType);
            }
        }

        /// Appends the immutable result type for one native call-state policy.
        ///
        /// @param source the output buffer
        /// @param function the error-capturing function
        /// @param nativeReturnType the exact Java carrier of the native result
        private void appendErrorResultRecord(
                StringBuilder source,
                AbiSchema.FunctionDefinition function,
                String nativeReturnType
        ) {
            String typeName = errorResultType(function);
            line(source, 0, "");
            line(source, 1, "/// Captures native symbol `" + javaDoc(function.symbol())
                    + "` and its immediate `" + captureStateName(function.errorPolicy()) + "` state.");
            line(source, 1, "///");
            if (!isVoid(function.result().type())) {
                line(source, 1, "/// @param value the native return value");
            }
            line(source, 1, "/// @param errorCode the captured native error code");
            line(source, 1, "@NotNullByDefault");
            String components = isVoid(function.result().type())
                    ? "int errorCode"
                    : nativeReturnType + " value, int errorCode";
            line(source, 1, "public record " + typeName + "(" + components + ") {");
            line(source, 1, "}");
        }

        /// Appends one callback interface, stub factory, and containment adapter.
        ///
        /// @param source the output buffer
        /// @param callback the callback declaration
        /// @param layoutsClass the generated layout class name
        private void appendCallbackMembers(
                StringBuilder source,
                AbiSchema.CallbackDefinition callback,
                String layoutsClass
        ) {
            String callbackType = upperCamel(callback.name());
            String factoryName = "create" + callbackType + "Stub";
            String returnType = carrierType(callback.result().type());
            boolean aggregateReturn = resolvedType(callback.result().type()) instanceof AbiSchema.AggregateType;
            Set<String> pointerNames = mutableParameterNames(callback.parameters());
            String functionAddressName = reserveGeneratedName(pointerNames, "function");
            String resultAllocatorName = aggregateReturn
                    ? reserveGeneratedName(pointerNames, "resultAllocator")
                    : "";
            String pointerFailureName = reserveGeneratedName(pointerNames, "failure");
            Set<String> adapterNames = mutableParameterNames(callback.parameters());
            String adapterCallbackName = reserveGeneratedName(adapterNames, "callback");
            String adapterFailuresName = reserveGeneratedName(adapterNames, "failures");
            String adapterResultName = aggregateReturn
                    ? reserveGeneratedName(adapterNames, "callbackResult")
                    : "";
            String adapterFailureName = reserveGeneratedName(adapterNames, "failure");
            line(source, 0, "");
            line(source, 1, "/// Invokes function pointer `" + javaDoc(callback.name())
                    + "` with its exact generated type.");
            line(source, 1, "///");
            line(source, 1, "/// @param " + functionAddressName + " the non-null native function address");
            if (aggregateReturn) {
                line(source, 1, "/// @param " + resultAllocatorName
                        + " the allocator that owns the returned aggregate segment");
            }
            for (AbiSchema.Parameter parameter : callback.parameters()) {
                line(source, 1, "/// @param " + parameter.name() + " native parameter `"
                        + javaDoc(parameter.name()) + "`");
            }
            if (!isVoid(callback.result().type())) {
                line(source, 1, "/// @return the native function-pointer result");
            }
            line(source, 1, "/// @throws IllegalStateException if the function or argument segment scope is no longer alive");
            String pointerParameters = parameterDeclarations(callback.parameters());
            List<String> generatedPointerParameters = new ArrayList<>();
            generatedPointerParameters.add("MemorySegment " + functionAddressName);
            if (aggregateReturn) {
                generatedPointerParameters.add("SegmentAllocator " + resultAllocatorName);
            }
            if (!pointerParameters.isEmpty()) {
                generatedPointerParameters.add(pointerParameters);
            }
            line(source, 1, "public static " + returnType + " invoke" + callbackType
                    + "Pointer(" + String.join(", ", generatedPointerParameters) + ") {");
            line(source, 2, "try {");
            String pointerArguments = parameterNames(callback.parameters());
            List<String> generatedPointerArguments = new ArrayList<>();
            generatedPointerArguments.add(functionAddressName);
            if (aggregateReturn) {
                generatedPointerArguments.add(resultAllocatorName);
            }
            if (!pointerArguments.isEmpty()) {
                generatedPointerArguments.add(pointerArguments);
            }
            String invocationArguments = String.join(", ", generatedPointerArguments);
            if (isVoid(callback.result().type())) {
                line(source, 3, constant(callback.name()) + "_DOWNCALL.invokeExact("
                        + invocationArguments + ");");
            } else {
                line(source, 3, "return (" + returnType + ") " + constant(callback.name())
                        + "_DOWNCALL.invokeExact(" + invocationArguments + ");");
            }
            line(source, 2, "} catch (RuntimeException | Error " + pointerFailureName + ") {");
            line(source, 3, "throw " + pointerFailureName + ";");
            line(source, 2, "} catch (Throwable " + pointerFailureName + ") {");
            line(source, 3, "throw new AssertionError(\"Unexpected checked failure from "
                    + javaString(callback.name()) + " function pointer\", " + pointerFailureName + ");");
            line(source, 2, "}");
            line(source, 1, "}");

            line(source, 0, "");
            line(source, 1, "/// Creates a contained upcall stub for callback `" + javaDoc(callback.name()) + "`.");
            line(source, 1, "///");
            line(source, 1, "/// @param callback the Java callback implementation");
            line(source, 1, "/// @param failures the sink for failures caught before returning to native code");
            line(source, 1, "/// @param arena the arena controlling the upcall-stub lifetime");
            line(source, 1, "/// @return the native function pointer");
            line(source, 1, "public MemorySegment " + factoryName + "(");
            line(source, 2, callbackType + " callback,");
            line(source, 2, "CallbackFailureSink failures,");
            line(source, 2, "Arena arena");
            line(source, 1, ") {");
            line(source, 2, "Objects.requireNonNull(callback, \"callback\");");
            line(source, 2, "Objects.requireNonNull(failures, \"failures\");");
            line(source, 2, "Objects.requireNonNull(arena, \"arena\");");
            line(source, 2, "MethodHandle target = " + constant(callback.name())
                    + "_ADAPTER.bindTo(callback).bindTo(failures);");
            line(source, 2, "return LINKER.upcallStub(target, " + layoutsClass + "."
                    + constant(callback.name()) + "_DESCRIPTOR, arena);");
            line(source, 1, "}");

            line(source, 0, "");
            line(source, 1, "/// Implements callback `" + javaDoc(callback.name()) + "` using exact Java carriers.");
            line(source, 1, "@FunctionalInterface");
            line(source, 1, "@NotNullByDefault");
            line(source, 1, "public interface " + callbackType + " {");
            line(source, 2, "/// Executes one native callback invocation.");
            line(source, 2, "///");
            for (AbiSchema.Parameter parameter : callback.parameters()) {
                line(source, 2, "/// @param " + parameter.name() + " native parameter `"
                        + javaDoc(parameter.name()) + "`");
            }
            if (!isVoid(callback.result().type())) {
                line(source, 2, "/// @return the native callback result");
                if (aggregateReturn) {
                    line(source, 2, "/// The returned segment must remain alive and accessible until the upcall returns");
                    line(source, 2, "/// and must contain at least the declared aggregate layout size.");
                }
            }
            line(source, 2, "/// @throws Throwable if application callback processing fails; the generated adapter contains it");
            String callbackParameters = parameterDeclarations(callback.parameters());
            line(source, 2, returnType + " invoke(" + callbackParameters + ") throws Throwable;");
            line(source, 1, "}");

            line(source, 0, "");
            line(source, 1, "/// Contains one invocation of callback `" + javaDoc(callback.name()) + "`.");
            line(source, 1, "///");
            line(source, 1, "/// @param " + adapterCallbackName + " the Java implementation");
            line(source, 1, "/// @param " + adapterFailuresName + " the failure sink");
            for (AbiSchema.Parameter parameter : callback.parameters()) {
                line(source, 1, "/// @param " + parameter.name() + " native parameter `"
                        + javaDoc(parameter.name()) + "`");
            }
            if (!isVoid(callback.result().type())) {
                line(source, 1, "/// @return the callback result or the ABI-safe zero value after failure");
            }
            line(source, 1, "private static " + returnType + " invoke" + callbackType + "(");
            List<String> adapterParameters = new ArrayList<>();
            adapterParameters.add(callbackType + " " + adapterCallbackName);
            adapterParameters.add("CallbackFailureSink " + adapterFailuresName);
            for (AbiSchema.Parameter parameter : callback.parameters()) {
                adapterParameters.add(carrierType(parameter.type()) + " " + parameter.name());
            }
            for (int index = 0; index < adapterParameters.size(); index++) {
                String suffix = index + 1 < adapterParameters.size() ? "," : "";
                line(source, 2, adapterParameters.get(index) + suffix);
            }
            line(source, 1, ") {");
            line(source, 2, "try {");
            String callbackArguments = parameterNames(callback.parameters());
            if (isVoid(callback.result().type())) {
                line(source, 3, adapterCallbackName + ".invoke(" + callbackArguments + ");");
            } else if (aggregateReturn) {
                line(source, 3, "MemorySegment " + adapterResultName + " = Objects.requireNonNull("
                        + adapterCallbackName + ".invoke(" + callbackArguments + "), \"callback result\");");
                line(source, 3, "if (" + adapterResultName + ".byteSize() < " + layoutsClass + "."
                        + constant(callback.result().type().name()) + ".byteSize()) {");
                line(source, 4, "throw new IllegalArgumentException(\"Aggregate callback result is too small\");");
                line(source, 3, "}");
                line(source, 3, "return " + adapterResultName + ";");
            } else {
                line(source, 3, "return " + adapterCallbackName + ".invoke(" + callbackArguments + ");");
            }
            line(source, 2, "} catch (Throwable " + adapterFailureName + ") {");
            line(source, 3, "publishFailure(" + adapterFailuresName + ", " + adapterFailureName + ");");
            if (!isVoid(callback.result().type())) {
                String fallback = aggregateReturn
                        ? constant(callback.name()) + "_ZERO_RETURN"
                        : fallbackValue(callback.result().type());
                line(source, 3, "return " + fallback + ";");
            }
            line(source, 2, "}");
            line(source, 1, "}");
        }

        /// Appends a multi-line aggregate layout expression without its terminating semicolon.
        ///
        /// @param source the output buffer
        /// @param aggregate the aggregate declaration
        /// @param indentation the base indentation
        private void appendAggregateExpression(
                StringBuilder source,
                AbiSchema.AggregateType aggregate,
                int indentation
        ) {
            List<String> elements = aggregate.kind() == AbiSchema.AggregateKind.STRUCT
                    ? structureElements(aggregate)
                    : unionElements(aggregate);
            String factory = aggregate.kind() == AbiSchema.AggregateKind.STRUCT ? "structLayout" : "unionLayout";
            line(source, indentation, "MemoryLayout." + factory + "(");
            for (int index = 0; index < elements.size(); index++) {
                String suffix = index + 1 < elements.size() ? "," : "";
                line(source, indentation + 1, elements.get(index) + suffix);
            }
            line(source, indentation, ").withByteAlignment(" + aggregate.alignment() + "L)");
            source.append("    ".repeat(indentation)).append(".withName(\"")
                    .append(javaString(aggregate.name())).append("\")");
        }

        /// Returns layout elements for one structure, including explicit ABI padding.
        ///
        /// @param aggregate the structure declaration
        /// @return ordered layout expressions
        private @Unmodifiable List<String> structureElements(AbiSchema.AggregateType aggregate) {
            Map<String, StorageField> bitfieldStorage = new LinkedHashMap<>();
            List<StorageField> fields = new ArrayList<>();
            for (AbiSchema.AggregateField field : aggregate.fields()) {
                if (field.bitOffset() == null) {
                    fields.add(storageField(aggregate, field, field.name()));
                } else {
                    String key = field.byteOffset() + ":" + field.type().name();
                    if (!bitfieldStorage.containsKey(key)) {
                        StorageField storage = storageField(aggregate, field, field.name() + "_storage");
                        bitfieldStorage.put(key, storage);
                        fields.add(storage);
                    }
                }
            }
            fields.sort((left, right) -> Long.compare(left.byteOffset(), right.byteOffset()));
            List<String> elements = new ArrayList<>();
            long cursor = 0;
            for (StorageField field : fields) {
                if (field.byteOffset() > cursor) {
                    elements.add("MemoryLayout.paddingLayout(" + (field.byteOffset() - cursor) + "L)");
                }
                elements.add(field.expression());
                cursor = field.byteOffset() + field.byteSize();
            }
            if (cursor < aggregate.byteSize()) {
                elements.add("MemoryLayout.paddingLayout(" + (aggregate.byteSize() - cursor) + "L)");
            }
            return List.copyOf(elements);
        }

        /// Returns layout elements for one union, including an explicit declared-size member.
        ///
        /// @param aggregate the union declaration
        /// @return ordered layout expressions
        private @Unmodifiable List<String> unionElements(AbiSchema.AggregateType aggregate) {
            List<String> elements = new ArrayList<>();
            for (AbiSchema.AggregateField field : aggregate.fields()) {
                elements.add(storageField(aggregate, field, field.name()).expression());
            }
            elements.add("MemoryLayout.paddingLayout(" + aggregate.byteSize() + "L)");
            return List.copyOf(elements);
        }

        /// Resolves one aggregate field into a physical storage element.
        ///
        /// @param aggregate the containing aggregate
        /// @param field the source field
        /// @param layoutName the emitted layout member name
        /// @return the storage element
        private StorageField storageField(
                AbiSchema.AggregateType aggregate,
                AbiSchema.AggregateField field,
                String layoutName
        ) {
            long size = layoutSize(field.type());
            int naturalAlignment = layoutAlignment(field.type());
            int effectiveAlignment = aggregate.packing() == 0
                    ? naturalAlignment
                    : Math.min(naturalAlignment, aggregate.packing());
            String expression = layoutExpression(field.type().name(), null)
                    + ".withByteAlignment(" + effectiveAlignment + "L)"
                    + ".withName(\"" + javaString(layoutName) + "\")";
            return new StorageField(field.byteOffset(), size, expression);
        }

        /// Returns non-aggregate declarations in canonical schema order.
        ///
        /// @return the immutable declaration list
        private @Unmodifiable List<AbiSchema.TypeDefinition> nonAggregateTypes() {
            return schema.types().stream()
                    .filter(type -> !(type instanceof AbiSchema.AggregateType))
                    .toList();
        }

        /// Returns aggregate declarations in direct-layout dependency order.
        ///
        /// @return the immutable ordered aggregate list
        private @Unmodifiable List<AbiSchema.AggregateType> orderedAggregates() {
            List<AbiSchema.AggregateType> ordered = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            Set<String> visiting = new HashSet<>();
            for (AbiSchema.TypeDefinition type : schema.types()) {
                if (type instanceof AbiSchema.AggregateType aggregate) {
                    visitAggregate(aggregate, visited, visiting, ordered);
                }
            }
            return List.copyOf(ordered);
        }

        /// Visits one aggregate and its directly embedded aggregate dependencies.
        ///
        /// @param aggregate the aggregate to visit
        /// @param visited completed aggregate names
        /// @param visiting aggregate names on the current path
        /// @param ordered the output list
        private void visitAggregate(
                AbiSchema.AggregateType aggregate,
                Set<String> visited,
                Set<String> visiting,
                List<AbiSchema.AggregateType> ordered
        ) {
            if (visited.contains(aggregate.name())) {
                return;
            }
            if (!visiting.add(aggregate.name())) {
                throw unsupported(aggregate.name(), "direct aggregate layout cycle");
            }
            for (AbiSchema.AggregateField field : aggregate.fields()) {
                if (types.get(field.type().name()) instanceof AbiSchema.AggregateType dependency) {
                    visitAggregate(dependency, visited, visiting, ordered);
                }
            }
            visiting.remove(aggregate.name());
            visited.add(aggregate.name());
            ordered.add(aggregate);
        }

        /// Returns the Java expression for a named ABI layout.
        ///
        /// @param name the type or callback name
        /// @param emittedName the optional layout name to attach
        /// @return the Java layout expression
        private String layoutExpression(String name, @Nullable String emittedName) {
            @Nullable AbiSchema.TypeDefinition type = types.get(name);
            String expression;
            if (type == null) {
                if (!callbacks.containsKey(name)) {
                    throw unsupported(name, "unresolved layout reference");
                }
                expression = "ValueLayout.ADDRESS.withByteAlignment("
                        + schema.target().addressAlignment() + "L)";
            } else {
                expression = switch (type) {
                    case AbiSchema.PrimitiveType primitive -> primitiveExpression(primitive);
                    case AbiSchema.PointerType pointer -> pointerExpression(pointer);
                    case AbiSchema.HandleType handle -> layoutExpression(handle.representation().name(), null);
                    case AbiSchema.AggregateType aggregate -> constant(aggregate.name());
                    case AbiSchema.IntegerSetType integerSet -> layoutExpression(
                            integerSet.representation().name(),
                            null
                    );
                };
            }
            return emittedName == null
                    ? expression
                    : expression + ".withName(\"" + javaString(emittedName) + "\")";
        }

        /// Returns the Java expression for one primitive layout.
        ///
        /// @param primitive the primitive declaration
        /// @return the Java layout expression
        private String primitiveExpression(AbiSchema.PrimitiveType primitive) {
            if (primitive.kind() == AbiSchema.PrimitiveKind.VOID) {
                throw unsupported(primitive.name(), "VOID has no storage layout");
            }
            String base = switch (primitive.kind()) {
                case VOID -> throw new AssertionError("VOID handled above");
                case INTEGER -> switch (primitive.byteSize()) {
                    case 1 -> "ValueLayout.JAVA_BYTE";
                    case 2 -> "ValueLayout.JAVA_SHORT";
                    case 4 -> "ValueLayout.JAVA_INT";
                    case 8 -> "ValueLayout.JAVA_LONG";
                    default -> throw unsupported(primitive.name(), "unsupported integer width");
                };
                case FLOATING -> switch (primitive.byteSize()) {
                    case 4 -> "ValueLayout.JAVA_FLOAT";
                    case 8 -> "ValueLayout.JAVA_DOUBLE";
                    default -> throw unsupported(primitive.name(), "unsupported floating-point width");
                };
            };
            String byteOrder = schema.target().byteOrder() == AbiSchema.ByteOrder.LITTLE_ENDIAN
                    ? "ByteOrder.LITTLE_ENDIAN"
                    : "ByteOrder.BIG_ENDIAN";
            return base + ".withOrder(" + byteOrder + ").withByteAlignment(" + primitive.alignment() + "L)";
        }

        /// Returns the Java expression for one pointer layout.
        ///
        /// @param pointer the pointer declaration
        /// @return the Java layout expression
        private String pointerExpression(AbiSchema.PointerType pointer) {
            String expression = "ValueLayout.ADDRESS";
            @Nullable AbiSchema.TypeDefinition pointee = types.get(pointer.pointee().name());
            if (pointee instanceof AbiSchema.PrimitiveType primitive
                    && primitive.kind() != AbiSchema.PrimitiveKind.VOID) {
                expression += ".withTargetLayout(" + primitiveExpression(primitive) + ")";
            } else if (pointee instanceof AbiSchema.IntegerSetType integerSet) {
                expression += ".withTargetLayout("
                        + layoutExpression(integerSet.representation().name(), null) + ")";
            }
            return expression + ".withByteAlignment(" + schema.target().addressAlignment() + "L)";
        }

        /// Returns a complete fixed function or callback descriptor expression.
        ///
        /// @param result the return contract
        /// @param parameters the ordered parameters
        /// @return the Java descriptor expression
        private String descriptorExpression(
                AbiSchema.ReturnValue result,
                List<AbiSchema.Parameter> parameters
        ) {
            String arguments = String.join(", ", parameters.stream()
                    .map(parameter -> layoutReference(parameter.type()))
                    .toList());
            if (isVoid(result.type())) {
                return arguments.isEmpty()
                        ? "FunctionDescriptor.ofVoid()"
                        : "FunctionDescriptor.ofVoid(" + arguments + ")";
            }
            String resultLayout = layoutReference(result.type());
            return arguments.isEmpty()
                    ? "FunctionDescriptor.of(" + resultLayout + ")"
                    : "FunctionDescriptor.of(" + resultLayout + ", " + arguments + ")";
        }

        /// Returns the generated layout constant for one type or callback reference.
        ///
        /// @param reference the schema reference
        /// @return the generated constant name
        private String layoutReference(AbiSchema.TypeRef reference) {
            return callbacks.containsKey(reference.name())
                    ? constant(reference.name()) + "_POINTER"
                    : constant(reference.name());
        }

        /// Returns the exact Java carrier type for one schema reference.
        ///
        /// @param reference the schema reference
        /// @return the Java source type
        private String carrierType(AbiSchema.TypeRef reference) {
            @Nullable AbiSchema.TypeDefinition type = types.get(reference.name());
            if (type == null) {
                if (callbacks.containsKey(reference.name())) {
                    return "MemorySegment";
                }
                throw unsupported(reference.name(), "unresolved carrier reference");
            }
            return switch (type) {
                case AbiSchema.PrimitiveType primitive -> primitiveCarrier(primitive);
                case AbiSchema.PointerType ignored -> "MemorySegment";
                case AbiSchema.HandleType handle -> carrierType(handle.representation());
                case AbiSchema.AggregateType ignored -> "MemorySegment";
                case AbiSchema.IntegerSetType integerSet -> carrierType(integerSet.representation());
            };
        }

        /// Returns the exact Java carrier type for one primitive declaration.
        ///
        /// @param primitive the primitive declaration
        /// @return the Java source type
        private static String primitiveCarrier(AbiSchema.PrimitiveType primitive) {
            return switch (primitive.kind()) {
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
        }

        /// Returns the exact generated method type for one downcall.
        ///
        /// @param function the function declaration
        /// @return the Java method-type expression
        private String functionMethodType(AbiSchema.FunctionDefinition function) {
            List<String> parameterTypes = new ArrayList<>();
            if (resolvedType(function.result().type()) instanceof AbiSchema.AggregateType) {
                parameterTypes.add("SegmentAllocator.class");
            }
            if (function.errorPolicy() != AbiSchema.ErrorPolicy.NONE) {
                parameterTypes.add("MemorySegment.class");
            }
            for (AbiSchema.Parameter parameter : function.parameters()) {
                parameterTypes.add(carrierType(parameter.type()) + ".class");
            }
            return methodTypeExpression(carrierType(function.result().type()), parameterTypes);
        }

        /// Returns the exact generated method type for one callback adapter.
        ///
        /// @param callback the callback declaration
        /// @param includeAdapterState whether callback and failure-sink receivers precede ABI parameters
        /// @return the Java method-type expression
        private String callbackMethodType(
                AbiSchema.CallbackDefinition callback,
                boolean includeAdapterState
        ) {
            List<String> parameterTypes = new ArrayList<>();
            if (includeAdapterState) {
                parameterTypes.add(upperCamel(callback.name()) + ".class");
                parameterTypes.add("CallbackFailureSink.class");
            }
            for (AbiSchema.Parameter parameter : callback.parameters()) {
                parameterTypes.add(carrierType(parameter.type()) + ".class");
            }
            return methodTypeExpression(carrierType(callback.result().type()), parameterTypes);
        }

        /// Returns the exact generated method type for an unbound function-pointer downcall.
        ///
        /// @param callback the function-pointer declaration
        /// @return the Java method-type expression including the leading target address
        private String callbackDowncallMethodType(AbiSchema.CallbackDefinition callback) {
            List<String> parameterTypes = new ArrayList<>();
            parameterTypes.add("MemorySegment.class");
            if (resolvedType(callback.result().type()) instanceof AbiSchema.AggregateType) {
                parameterTypes.add("SegmentAllocator.class");
            }
            for (AbiSchema.Parameter parameter : callback.parameters()) {
                parameterTypes.add(carrierType(parameter.type()) + ".class");
            }
            return methodTypeExpression(carrierType(callback.result().type()), parameterTypes);
        }

        /// Formats a generated `MethodType.methodType` expression.
        ///
        /// @param returnType the Java source return type
        /// @param parameterTypes the Java class-literal expressions
        /// @return the method-type expression
        private static String methodTypeExpression(String returnType, List<String> parameterTypes) {
            String suffix = parameterTypes.isEmpty() ? "" : ", " + String.join(", ", parameterTypes);
            return "MethodType.methodType(" + returnType + ".class" + suffix + ")";
        }

        /// Returns invocation arguments in exact method-handle order.
        ///
        /// @param parameters the schema parameters
        /// @param resultAllocatorName the result-allocator parameter name, or an empty string when absent
        /// @param callStateName the capture-state segment name, or an empty string when absent
        /// @return the comma-separated invocation argument list
        private static String invocationArguments(
                List<AbiSchema.Parameter> parameters,
                String resultAllocatorName,
                String callStateName
        ) {
            List<String> names = new ArrayList<>();
            if (!resultAllocatorName.isEmpty()) {
                names.add(resultAllocatorName);
            }
            if (!callStateName.isEmpty()) {
                names.add(callStateName);
            }
            parameters.stream().map(AbiSchema.Parameter::name).forEach(names::add);
            return String.join(", ", names);
        }

        /// Returns a mutable set containing every native parameter name.
        ///
        /// @param parameters the native parameters
        /// @return a new mutable set of occupied Java names
        private static Set<String> mutableParameterNames(List<AbiSchema.Parameter> parameters) {
            Set<String> names = new HashSet<>();
            parameters.stream().map(AbiSchema.Parameter::name).forEach(names::add);
            return names;
        }

        /// Reserves a generated Java name that does not collide with an existing parameter or local.
        ///
        /// Underscores are appended until the name can be added to `occupiedNames`.
        ///
        /// @param occupiedNames the mutable names already in scope
        /// @param preferredName the preferred generated name
        /// @return the reserved unique name
        private static String reserveGeneratedName(Set<String> occupiedNames, String preferredName) {
            String candidate = preferredName;
            while (!occupiedNames.add(candidate)) {
                candidate += "_";
            }
            return candidate;
        }

        /// Returns the generated nested result type for one error-capturing function.
        ///
        /// @param function the function declaration
        /// @return the result record name
        private static String errorResultType(AbiSchema.FunctionDefinition function) {
            return upperCamel(function.name()) + "Result";
        }

        /// Returns the FFM call-state spelling for one schema error policy.
        ///
        /// @param policy the non-`NONE` error policy
        /// @return the linker call-state name
        private static String captureStateName(AbiSchema.ErrorPolicy policy) {
            return switch (policy) {
                case NONE -> throw new IllegalArgumentException("NONE has no captured call state");
                case ERRNO -> "errno";
                case GET_LAST_ERROR -> "GetLastError";
            };
        }

        /// Returns the generated byte-offset constant for one captured error policy.
        ///
        /// @param policy the non-`NONE` error policy
        /// @return the Java constant name
        private static String captureOffsetConstant(AbiSchema.ErrorPolicy policy) {
            return constant(captureStateName(policy)) + "_OFFSET";
        }

        /// Returns the optional generated linker argument for one native function.
        ///
        /// @param function the function declaration
        /// @return an empty string or a comma-prefixed linker option expression
        private static String linkerOptionSuffix(AbiSchema.FunctionDefinition function) {
            if (function.errorPolicy() == AbiSchema.ErrorPolicy.NONE) {
                return "";
            }
            return ", Linker.Option.captureCallState(\""
                    + captureStateName(function.errorPolicy()) + "\")";
        }

        /// Returns Java parameter declarations for one callback interface method.
        ///
        /// @param parameters the callback parameters
        /// @return the comma-separated declarations
        private String parameterDeclarations(List<AbiSchema.Parameter> parameters) {
            return String.join(", ", parameters.stream()
                    .map(parameter -> carrierType(parameter.type()) + " " + parameter.name())
                    .toList());
        }

        /// Returns Java argument names for one callback invocation.
        ///
        /// @param parameters the callback parameters
        /// @return the comma-separated names
        private static String parameterNames(List<AbiSchema.Parameter> parameters) {
            return String.join(", ", parameters.stream().map(AbiSchema.Parameter::name).toList());
        }

        /// Returns the ABI-safe zero fallback for one callback result.
        ///
        /// @param reference the callback result type
        /// @return the Java fallback expression
        private String fallbackValue(AbiSchema.TypeRef reference) {
            @Nullable AbiSchema.TypeDefinition type = types.get(reference.name());
            if (type == null) {
                if (callbacks.containsKey(reference.name())) {
                    return "MemorySegment.NULL";
                }
                throw unsupported(reference.name(), "unresolved callback fallback type");
            }
            return switch (type) {
                case AbiSchema.PrimitiveType primitive -> switch (primitive.kind()) {
                    case VOID -> throw unsupported(reference.name(), "VOID has no fallback value");
                    case INTEGER -> primitive.byteSize() == 8 ? "0L" : "0";
                    case FLOATING -> primitive.byteSize() == 4 ? "0.0f" : "0.0d";
                };
                case AbiSchema.PointerType ignored -> "MemorySegment.NULL";
                case AbiSchema.HandleType handle -> fallbackValue(handle.representation());
                case AbiSchema.AggregateType ignored -> throw unsupported(
                        reference.name(),
                        "aggregate callback returns have no generated fallback"
                );
                case AbiSchema.IntegerSetType integerSet -> fallbackValue(integerSet.representation());
            };
        }

        /// Returns the target-resolved storage size of one type or callback.
        ///
        /// @param reference the schema reference
        /// @return the byte size
        private long layoutSize(AbiSchema.TypeRef reference) {
            @Nullable AbiSchema.TypeDefinition type = types.get(reference.name());
            if (type == null) {
                if (callbacks.containsKey(reference.name())) {
                    return schema.target().addressSize();
                }
                throw unsupported(reference.name(), "unresolved layout size");
            }
            return switch (type) {
                case AbiSchema.PrimitiveType primitive -> primitive.byteSize();
                case AbiSchema.PointerType ignored -> schema.target().addressSize();
                case AbiSchema.HandleType handle -> layoutSize(handle.representation());
                case AbiSchema.AggregateType aggregate -> aggregate.byteSize();
                case AbiSchema.IntegerSetType integerSet -> layoutSize(integerSet.representation());
            };
        }

        /// Returns the target-resolved storage alignment of one type or callback.
        ///
        /// @param reference the schema reference
        /// @return the byte alignment
        private int layoutAlignment(AbiSchema.TypeRef reference) {
            @Nullable AbiSchema.TypeDefinition type = types.get(reference.name());
            if (type == null) {
                if (callbacks.containsKey(reference.name())) {
                    return schema.target().addressAlignment();
                }
                throw unsupported(reference.name(), "unresolved layout alignment");
            }
            return switch (type) {
                case AbiSchema.PrimitiveType primitive -> primitive.alignment();
                case AbiSchema.PointerType ignored -> schema.target().addressAlignment();
                case AbiSchema.HandleType handle -> layoutAlignment(handle.representation());
                case AbiSchema.AggregateType aggregate -> aggregate.alignment();
                case AbiSchema.IntegerSetType integerSet -> layoutAlignment(integerSet.representation());
            };
        }

        /// Resolves a schema reference to a concrete type declaration.
        ///
        /// @param reference the schema reference
        /// @return the concrete type, or `null` for a callback
        private @Nullable AbiSchema.TypeDefinition resolvedType(AbiSchema.TypeRef reference) {
            return types.get(reference.name());
        }

        /// Returns whether a reference denotes the void primitive.
        ///
        /// @param reference the schema reference
        /// @return whether the reference is void
        private boolean isVoid(AbiSchema.TypeRef reference) {
            return types.get(reference.name()) instanceof AbiSchema.PrimitiveType primitive
                    && primitive.kind() == AbiSchema.PrimitiveKind.VOID;
        }
    }

    /// Converts an ABI identifier to an upper-camel Java type name.
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

    /// Converts an ABI identifier to an upper-snake Java constant name.
    ///
    /// @param value the ABI identifier
    /// @return the generated constant name
    private static String constant(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    /// Requires an ABI identifier to also be usable as a Java declaration name.
    ///
    /// @param value the identifier
    /// @param context the diagnostic context
    private static void requireJavaIdentifier(String value, String context) {
        if (value.isEmpty() || !Character.isJavaIdentifierStart(value.charAt(0))) {
            throw new IllegalArgumentException(
                    "Cannot generate " + context + ": not a valid Java identifier '" + value + "'"
            );
        }
        for (int index = 1; index < value.length(); index++) {
            if (!Character.isJavaIdentifierPart(value.charAt(index))) {
                throw new IllegalArgumentException(
                        "Cannot generate " + context + ": not a valid Java identifier '" + value + "'"
                );
            }
        }
        if (JAVA_RESERVED_WORDS.contains(value)) {
            throw new IllegalArgumentException("Cannot generate " + context + ": Java reserved word '" + value + "'");
        }
    }

    /// Requires one derived Java identifier to remain unique in its generated namespace.
    ///
    /// @param names the occupied generated names
    /// @param generatedName the candidate name
    /// @param kind the generated namespace description
    /// @param sourceName the source declaration name
    private static void requireUniqueGeneratedName(
            Set<String> names,
            String generatedName,
            String kind,
            String sourceName
    ) {
        if (!names.add(generatedName)) {
            throw new IllegalArgumentException(
                    "Generated " + kind + " name '" + generatedName + "' collides at '" + sourceName + "'"
            );
        }
    }

    /// Creates an unsupported-generator-profile failure.
    ///
    /// @param declaration the failing declaration
    /// @param detail the unsupported feature
    /// @return the exception to throw
    private static IllegalArgumentException unsupported(String declaration, String detail) {
        return new IllegalArgumentException("Cannot generate FFM declaration '" + declaration + "': " + detail);
    }

    /// Escapes text for inclusion in a Java string literal without surrounding quotes.
    ///
    /// @param value the raw string
    /// @return the escaped string contents
    private static String javaString(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    /// Escapes Markdown-sensitive characters used in generated one-line Javadoc code spans.
    ///
    /// @param value the raw text
    /// @return the escaped text
    private static String javaDoc(String value) {
        return value.replace("`", "&#96;");
    }

    /// Appends one generated source line using four-space indentation.
    ///
    /// @param output the source buffer
    /// @param indentation the non-negative indentation level
    /// @param value the line contents
    private static void line(StringBuilder output, int indentation, String value) {
        output.append("    ".repeat(indentation)).append(value).append('\n');
    }

    /// Describes one physical aggregate storage element.
    ///
    /// @param byteOffset the byte offset in the aggregate
    /// @param byteSize the element size in bytes
    /// @param expression the generated layout expression
    @NotNullByDefault
    private record StorageField(long byteOffset, long byteSize, String expression) {
    }
}
