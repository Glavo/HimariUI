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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonArray;
import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonNull;
import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonNumber;
import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonObject;
import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonString;
import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonValue;

/// Reads and writes the strict, deterministic native ABI probe protocol.
@NotNullByDefault
public final class AbiProbeCodec {
    /// The only protocol version accepted by this codec.
    public static final int PROTOCOL_VERSION = 1;

    /// The protocol schema identifier.
    public static final String SCHEMA_ID = "schema/abi-probe.schema.json";

    /// Matches canonical ABI declaration names.
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /// Matches stable conformance fixture identifiers.
    private static final Pattern FIXTURE = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /// The operating systems represented by protocol version 1.
    private static final Set<String> OPERATING_SYSTEMS = Set.of("linux", "windows", "macos");

    /// The processor architectures represented by protocol version 1.
    private static final Set<String> ARCHITECTURES = Set.of("x86_64", "arm64");

    /// The compiler families represented by protocol version 1.
    private static final Set<String> COMPILERS = Set.of("clang", "gcc", "msvc", "unknown");

    /// Prevents instantiation of this utility class.
    private AbiProbeCodec() {
    }

    /// Reads one UTF-8 probe document.
    ///
    /// @param path the source path
    /// @return the decoded probe
    /// @throws IllegalArgumentException if the document cannot be read or violates the protocol
    public static AbiProbe read(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            return read(path.toString(), Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot read ABI probe " + path, exception);
        }
    }

    /// Reads one probe document from text.
    ///
    /// @param sourceName the source name used in diagnostics
    /// @param source the complete JSON source
    /// @return the decoded probe
    /// @throws IllegalArgumentException if the document violates the protocol
    public static AbiProbe read(String sourceName, String source) {
        Objects.requireNonNull(sourceName, "sourceName");
        Objects.requireNonNull(source, "source");
        JsonObject root = StrictJson.parseObject(sourceName, source);
        requireExactKeys(root, "$", Set.of(
                "protocolVersion",
                "fixtures",
                "target",
                "compiler",
                "types",
                "aggregates",
                "callbacks",
                "checks"
        ));
        int protocolVersion = intMember(root, "protocolVersion", "$", 0, Integer.MAX_VALUE);
        if (protocolVersion != PROTOCOL_VERSION) {
            throw failure("$.protocolVersion", "unsupported protocol version " + protocolVersion);
        }
        List<String> fixtures = decodeFixtures(arrayMember(root, "fixtures", "$"), "$.fixtures");
        AbiProbe probe = new AbiProbe(
                protocolVersion,
                fixtures,
                decodeTarget(objectMember(root, "target", "$"), "$.target"),
                decodeCompiler(objectMember(root, "compiler", "$"), "$.compiler"),
                decodeTypes(arrayMember(root, "types", "$"), "$.types"),
                decodeAggregates(arrayMember(root, "aggregates", "$"), "$.aggregates"),
                decodeCallbacks(arrayMember(root, "callbacks", "$"), "$.callbacks"),
                decodeChecks(objectMember(root, "checks", "$"), "$.checks")
        );
        requireNonEmpty(probe.fixtures(), "$.fixtures");
        requireNonEmpty(probe.types(), "$.types");
        requireNonEmpty(probe.aggregates(), "$.aggregates");
        requireNonEmpty(probe.callbacks(), "$.callbacks");
        return probe;
    }

    /// Returns the canonical JSON encoding of one probe document.
    ///
    /// @param probe the probe to encode
    /// @return the deterministic JSON document ending in one newline
    public static String write(AbiProbe probe) {
        Objects.requireNonNull(probe, "probe");
        StringBuilder output = new StringBuilder(4096);
        output.append("{\n");
        member(output, 2, "protocolVersion", Integer.toString(probe.protocolVersion()), true);
        output.append("  \"fixtures\": [");
        for (int index = 0; index < probe.fixtures().size(); index++) {
            if (index > 0) {
                output.append(", ");
            }
            appendQuoted(output, probe.fixtures().get(index));
        }
        output.append("],\n");
        appendTarget(output, probe.target());
        appendCompiler(output, probe.compiler());
        appendTypes(output, probe.types());
        appendAggregates(output, probe.aggregates());
        appendCallbacks(output, probe.callbacks());
        appendChecks(output, probe.checks());
        output.append("}\n");
        return output.toString();
    }

    /// Decodes and validates fixture identifiers.
    ///
    /// @param values the parsed fixture values
    /// @param path the array path
    /// @return the immutable fixture list
    private static @Unmodifiable List<String> decodeFixtures(List<JsonValue> values, String path) {
        List<String> fixtures = new ArrayList<>(values.size());
        Set<String> names = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String fixture = asString(values.get(index), path + "[" + index + "]");
            if (!FIXTURE.matcher(fixture).matches()) {
                throw failure(path + "[" + index + "]", "must be a stable lowercase fixture identifier");
            }
            if (!names.add(fixture)) {
                throw failure(path + "[" + index + "]", "duplicates fixture '" + fixture + "'");
            }
            fixtures.add(fixture);
        }
        return List.copyOf(fixtures);
    }

    /// Decodes an exact target descriptor.
    ///
    /// @param object the parsed object
    /// @param path the object path
    /// @return the target measurement
    private static AbiProbe.Target decodeTarget(JsonObject object, String path) {
        requireExactKeys(object, path, Set.of(
                "operatingSystem",
                "architecture",
                "byteOrder",
                "addressSize",
                "addressAlignment"
        ));
        String operatingSystem = stringMember(object, "operatingSystem", path);
        if (!OPERATING_SYSTEMS.contains(operatingSystem)) {
            throw failure(path + ".operatingSystem", "unsupported operating system '" + operatingSystem + "'");
        }
        String architecture = stringMember(object, "architecture", path);
        if (!ARCHITECTURES.contains(architecture)) {
            throw failure(path + ".architecture", "unsupported architecture '" + architecture + "'");
        }
        return new AbiProbe.Target(
                operatingSystem,
                architecture,
                enumMember(object, "byteOrder", path, AbiSchema.ByteOrder.class),
                positiveLongMember(object, "addressSize", path),
                positiveLongMember(object, "addressAlignment", path)
        );
    }

    /// Decodes a compiler identity.
    ///
    /// @param object the parsed object
    /// @param path the object path
    /// @return the compiler identity
    private static AbiProbe.Compiler decodeCompiler(JsonObject object, String path) {
        requireExactKeys(object, path, Set.of("family", "major", "minor", "patch"));
        String family = stringMember(object, "family", path);
        if (!COMPILERS.contains(family)) {
            throw failure(path + ".family", "unsupported compiler family '" + family + "'");
        }
        return new AbiProbe.Compiler(
                family,
                intMember(object, "major", path, 0, Integer.MAX_VALUE),
                intMember(object, "minor", path, 0, Integer.MAX_VALUE),
                intMember(object, "patch", path, 0, Integer.MAX_VALUE)
        );
    }

    /// Decodes measured non-aggregate layouts.
    ///
    /// @param values the parsed layout values
    /// @param path the array path
    /// @return the immutable layout list
    private static @Unmodifiable List<AbiProbe.TypeLayout> decodeTypes(List<JsonValue> values, String path) {
        List<AbiProbe.TypeLayout> layouts = new ArrayList<>(values.size());
        Set<String> names = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemPath = path + "[" + index + "]";
            JsonObject object = asObject(values.get(index), itemPath);
            requireExactKeys(object, itemPath, Set.of("name", "byteSize", "alignment"));
            String name = identifierMember(object, "name", itemPath);
            requireUnique(names, name, itemPath + ".name");
            layouts.add(new AbiProbe.TypeLayout(
                    name,
                    positiveLongMember(object, "byteSize", itemPath),
                    positiveLongMember(object, "alignment", itemPath)
            ));
        }
        return List.copyOf(layouts);
    }

    /// Decodes measured aggregate layouts.
    ///
    /// @param values the parsed aggregate values
    /// @param path the array path
    /// @return the immutable aggregate list
    private static @Unmodifiable List<AbiProbe.AggregateLayout> decodeAggregates(
            List<JsonValue> values,
            String path
    ) {
        List<AbiProbe.AggregateLayout> aggregates = new ArrayList<>(values.size());
        Set<String> names = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemPath = path + "[" + index + "]";
            JsonObject object = asObject(values.get(index), itemPath);
            requireExactKeys(object, itemPath, Set.of("name", "byteSize", "alignment", "fields"));
            String name = identifierMember(object, "name", itemPath);
            requireUnique(names, name, itemPath + ".name");
            List<AbiProbe.FieldLayout> fields = decodeFields(
                    arrayMember(object, "fields", itemPath),
                    itemPath + ".fields"
            );
            requireNonEmpty(fields, itemPath + ".fields");
            aggregates.add(new AbiProbe.AggregateLayout(
                    name,
                    positiveLongMember(object, "byteSize", itemPath),
                    positiveLongMember(object, "alignment", itemPath),
                    fields
            ));
        }
        return List.copyOf(aggregates);
    }

    /// Decodes measured aggregate fields.
    ///
    /// @param values the parsed field values
    /// @param path the array path
    /// @return the immutable field list
    private static @Unmodifiable List<AbiProbe.FieldLayout> decodeFields(List<JsonValue> values, String path) {
        List<AbiProbe.FieldLayout> fields = new ArrayList<>(values.size());
        Set<String> names = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemPath = path + "[" + index + "]";
            JsonObject object = asObject(values.get(index), itemPath);
            requireExactKeys(object, itemPath, Set.of("name", "byteOffset", "bitOffset", "bitWidth"));
            String name = identifierMember(object, "name", itemPath);
            requireUnique(names, name, itemPath + ".name");
            fields.add(new AbiProbe.FieldLayout(
                    name,
                    longMember(object, "byteOffset", itemPath, 0, Long.MAX_VALUE),
                    nullableIntMember(object, "bitOffset", itemPath, 0, Integer.MAX_VALUE),
                    nullableIntMember(object, "bitWidth", itemPath, 1, Integer.MAX_VALUE)
            ));
        }
        return List.copyOf(fields);
    }

    /// Decodes measured callback layouts.
    ///
    /// @param values the parsed callback values
    /// @param path the array path
    /// @return the immutable callback list
    private static @Unmodifiable List<AbiProbe.CallbackLayout> decodeCallbacks(
            List<JsonValue> values,
            String path
    ) {
        List<AbiProbe.CallbackLayout> callbacks = new ArrayList<>(values.size());
        Set<String> names = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemPath = path + "[" + index + "]";
            JsonObject object = asObject(values.get(index), itemPath);
            requireExactKeys(object, itemPath, Set.of(
                    "name",
                    "callingConvention",
                    "pointerSize",
                    "pointerAlignment",
                    "invocationResult"
            ));
            String name = identifierMember(object, "name", itemPath);
            requireUnique(names, name, itemPath + ".name");
            callbacks.add(new AbiProbe.CallbackLayout(
                    name,
                    enumMember(object, "callingConvention", itemPath, AbiSchema.CallingConvention.class),
                    positiveLongMember(object, "pointerSize", itemPath),
                    positiveLongMember(object, "pointerAlignment", itemPath),
                    longMember(object, "invocationResult", itemPath, Long.MIN_VALUE, Long.MAX_VALUE)
            ));
        }
        return List.copyOf(callbacks);
    }

    /// Decodes fixed functional ABI checks.
    ///
    /// @param object the parsed checks object
    /// @param path the object path
    /// @return the decoded checks
    private static AbiProbe.Checks decodeChecks(JsonObject object, String path) {
        requireExactKeys(object, path, Set.of("structureReturnLeft", "structureReturnRight", "variadicSum"));
        return new AbiProbe.Checks(
                intMember(object, "structureReturnLeft", path, Integer.MIN_VALUE, Integer.MAX_VALUE),
                longMember(object, "structureReturnRight", path, 0, 0xffff_ffffL),
                intMember(object, "variadicSum", path, Integer.MIN_VALUE, Integer.MAX_VALUE)
        );
    }

    /// Appends the target object and its trailing comma.
    ///
    /// @param output the output buffer
    /// @param target the target to encode
    private static void appendTarget(StringBuilder output, AbiProbe.Target target) {
        output.append("  \"target\": {\n");
        quotedMember(output, 4, "operatingSystem", target.operatingSystem(), true);
        quotedMember(output, 4, "architecture", target.architecture(), true);
        quotedMember(output, 4, "byteOrder", enumeration(target.byteOrder()), true);
        member(output, 4, "addressSize", Long.toString(target.addressSize()), true);
        member(output, 4, "addressAlignment", Long.toString(target.addressAlignment()), false);
        output.append("  },\n");
    }

    /// Appends the compiler object and its trailing comma.
    ///
    /// @param output the output buffer
    /// @param compiler the compiler identity to encode
    private static void appendCompiler(StringBuilder output, AbiProbe.Compiler compiler) {
        output.append("  \"compiler\": {\n");
        quotedMember(output, 4, "family", compiler.family(), true);
        member(output, 4, "major", Integer.toString(compiler.major()), true);
        member(output, 4, "minor", Integer.toString(compiler.minor()), true);
        member(output, 4, "patch", Integer.toString(compiler.patch()), false);
        output.append("  },\n");
    }

    /// Appends the measured type array and its trailing comma.
    ///
    /// @param output the output buffer
    /// @param layouts the layouts to encode
    private static void appendTypes(StringBuilder output, List<AbiProbe.TypeLayout> layouts) {
        output.append("  \"types\": [\n");
        for (int index = 0; index < layouts.size(); index++) {
            AbiProbe.TypeLayout layout = layouts.get(index);
            output.append("    {\"name\": ");
            appendQuoted(output, layout.name());
            output.append(", \"byteSize\": ").append(layout.byteSize())
                    .append(", \"alignment\": ").append(layout.alignment()).append('}');
            output.append(index + 1 < layouts.size() ? ",\n" : "\n");
        }
        output.append("  ],\n");
    }

    /// Appends the measured aggregate array and its trailing comma.
    ///
    /// @param output the output buffer
    /// @param aggregates the aggregates to encode
    private static void appendAggregates(StringBuilder output, List<AbiProbe.AggregateLayout> aggregates) {
        output.append("  \"aggregates\": [\n");
        for (int index = 0; index < aggregates.size(); index++) {
            AbiProbe.AggregateLayout aggregate = aggregates.get(index);
            output.append("    {\n");
            quotedMember(output, 6, "name", aggregate.name(), true);
            member(output, 6, "byteSize", Long.toString(aggregate.byteSize()), true);
            member(output, 6, "alignment", Long.toString(aggregate.alignment()), true);
            output.append("      \"fields\": [\n");
            for (int fieldIndex = 0; fieldIndex < aggregate.fields().size(); fieldIndex++) {
                AbiProbe.FieldLayout field = aggregate.fields().get(fieldIndex);
                output.append("        {\"name\": ");
                appendQuoted(output, field.name());
                output.append(", \"byteOffset\": ").append(field.byteOffset())
                        .append(", \"bitOffset\": ").append(nullableNumber(field.bitOffset()))
                        .append(", \"bitWidth\": ").append(nullableNumber(field.bitWidth())).append('}');
                output.append(fieldIndex + 1 < aggregate.fields().size() ? ",\n" : "\n");
            }
            output.append("      ]\n    }");
            output.append(index + 1 < aggregates.size() ? ",\n" : "\n");
        }
        output.append("  ],\n");
    }

    /// Appends the measured callback array and its trailing comma.
    ///
    /// @param output the output buffer
    /// @param callbacks the callbacks to encode
    private static void appendCallbacks(StringBuilder output, List<AbiProbe.CallbackLayout> callbacks) {
        output.append("  \"callbacks\": [\n");
        for (int index = 0; index < callbacks.size(); index++) {
            AbiProbe.CallbackLayout callback = callbacks.get(index);
            output.append("    {\n");
            quotedMember(output, 6, "name", callback.name(), true);
            quotedMember(output, 6, "callingConvention", enumeration(callback.callingConvention()), true);
            member(output, 6, "pointerSize", Long.toString(callback.pointerSize()), true);
            member(output, 6, "pointerAlignment", Long.toString(callback.pointerAlignment()), true);
            member(output, 6, "invocationResult", Long.toString(callback.invocationResult()), false);
            output.append("    }");
            output.append(index + 1 < callbacks.size() ? ",\n" : "\n");
        }
        output.append("  ],\n");
    }

    /// Appends the final functional checks object.
    ///
    /// @param output the output buffer
    /// @param checks the checks to encode
    private static void appendChecks(StringBuilder output, AbiProbe.Checks checks) {
        output.append("  \"checks\": {\n");
        member(output, 4, "structureReturnLeft", Integer.toString(checks.structureReturnLeft()), true);
        member(output, 4, "structureReturnRight", Long.toString(checks.structureReturnRight()), true);
        member(output, 4, "variadicSum", Integer.toString(checks.variadicSum()), false);
        output.append("  }\n");
    }

    /// Appends one JSON object member whose value is already encoded.
    ///
    /// @param output the output buffer
    /// @param indentation the indentation width
    /// @param name the member name
    /// @param value the encoded JSON value
    /// @param comma whether to append a trailing comma
    private static void member(StringBuilder output, int indentation, String name, String value, boolean comma) {
        output.append(" ".repeat(indentation));
        appendQuoted(output, name);
        output.append(": ").append(value);
        if (comma) {
            output.append(',');
        }
        output.append('\n');
    }

    /// Appends one JSON string member.
    ///
    /// @param output the output buffer
    /// @param indentation the indentation width
    /// @param name the member name
    /// @param value the decoded string value
    /// @param comma whether to append a trailing comma
    private static void quotedMember(
            StringBuilder output,
            int indentation,
            String name,
            String value,
            boolean comma
    ) {
        StringBuilder encoded = new StringBuilder(value.length() + 2);
        appendQuoted(encoded, value);
        member(output, indentation, name, encoded.toString(), comma);
    }

    /// Appends a quoted and escaped JSON string.
    ///
    /// @param output the output buffer
    /// @param value the decoded value
    private static void appendQuoted(StringBuilder output, String value) {
        output.append('"');
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
                        output.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        output.append('"');
    }

    /// Returns a nullable integer's JSON representation.
    ///
    /// @param value the nullable integer
    /// @return `null` or the decimal representation
    private static String nullableNumber(@Nullable Integer value) {
        return value == null ? "null" : value.toString();
    }

    /// Returns a lowercase protocol spelling for an enum constant.
    ///
    /// @param value the enum value
    /// @return the protocol spelling
    private static String enumeration(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    /// Requires an object to contain exactly the expected members.
    ///
    /// @param object the parsed object
    /// @param path the object path
    /// @param expected the exact member names
    private static void requireExactKeys(JsonObject object, String path, Set<String> expected) {
        Set<String> actual = object.members().keySet();
        for (String name : expected) {
            if (!actual.contains(name)) {
                throw failure(path, "missing required member '" + name + "'");
            }
        }
        for (String name : actual) {
            if (!expected.contains(name)) {
                throw failure(path, "unknown member '" + name + "'");
            }
        }
    }

    /// Returns a required object member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing path
    /// @return the member value
    private static JsonValue requiredMember(JsonObject object, String name, String path) {
        @Nullable JsonValue value = object.members().get(name);
        if (value == null) {
            throw failure(path, "missing required member '" + name + "'");
        }
        return value;
    }

    /// Returns a required object-valued member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing path
    /// @return the object value
    private static JsonObject objectMember(JsonObject object, String name, String path) {
        return asObject(requiredMember(object, name, path), path + "." + name);
    }

    /// Returns a required array-valued member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing path
    /// @return the immutable array elements
    private static @Unmodifiable List<JsonValue> arrayMember(JsonObject object, String name, String path) {
        JsonValue value = requiredMember(object, name, path);
        if (value instanceof JsonArray array) {
            return array.elements();
        }
        throw failure(path + "." + name, "must be an array");
    }

    /// Returns a required string-valued member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing path
    /// @return the decoded string
    private static String stringMember(JsonObject object, String name, String path) {
        return asString(requiredMember(object, name, path), path + "." + name);
    }

    /// Returns and validates a required identifier member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing path
    /// @return the identifier
    private static String identifierMember(JsonObject object, String name, String path) {
        String value = stringMember(object, name, path);
        if (!IDENTIFIER.matcher(value).matches()) {
            throw failure(path + "." + name, "must be a portable C-style identifier");
        }
        return value;
    }

    /// Returns a required positive integral member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing path
    /// @return the positive value
    private static long positiveLongMember(JsonObject object, String name, String path) {
        return longMember(object, name, path, 1, Long.MAX_VALUE);
    }

    /// Returns a required integral member in an inclusive range.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing path
    /// @param minimum the minimum permitted value
    /// @param maximum the maximum permitted value
    /// @return the decoded value
    private static long longMember(
            JsonObject object,
            String name,
            String path,
            long minimum,
            long maximum
    ) {
        JsonValue value = requiredMember(object, name, path);
        String memberPath = path + "." + name;
        if (!(value instanceof JsonNumber number) || !number.integral()) {
            throw failure(memberPath, "must be an integer");
        }
        long decoded = number.value().longValue();
        if (decoded < minimum || decoded > maximum) {
            throw failure(memberPath, "must be in [" + minimum + ", " + maximum + "]");
        }
        return decoded;
    }

    /// Returns a required 32-bit integral member in an inclusive range.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing path
    /// @param minimum the minimum permitted value
    /// @param maximum the maximum permitted value
    /// @return the decoded value
    private static int intMember(
            JsonObject object,
            String name,
            String path,
            int minimum,
            int maximum
    ) {
        return Math.toIntExact(longMember(object, name, path, minimum, maximum));
    }

    /// Returns a nullable 32-bit integral member in an inclusive range.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing path
    /// @param minimum the minimum permitted value when present
    /// @param maximum the maximum permitted value when present
    /// @return the decoded value or `null`
    private static @Nullable Integer nullableIntMember(
            JsonObject object,
            String name,
            String path,
            int minimum,
            int maximum
    ) {
        JsonValue value = requiredMember(object, name, path);
        if (value instanceof JsonNull) {
            return null;
        }
        if (!(value instanceof JsonNumber number) || !number.integral()) {
            throw failure(path + "." + name, "must be null or an integer");
        }
        long decoded = number.value().longValue();
        if (decoded < minimum || decoded > maximum) {
            throw failure(path + "." + name, "must be in [" + minimum + ", " + maximum + "]");
        }
        return Math.toIntExact(decoded);
    }

    /// Returns a required enum member using lowercase protocol spelling.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing path
    /// @param enumType the destination enum type
    /// @param <E> the enum type
    /// @return the decoded enum value
    private static <E extends Enum<E>> E enumMember(
            JsonObject object,
            String name,
            String path,
            Class<E> enumType
    ) {
        String value = stringMember(object, name, path);
        try {
            return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw failure(path + "." + name, "unknown value '" + value + "'");
        }
    }

    /// Casts one parsed value to an object.
    ///
    /// @param value the parsed value
    /// @param path the value path
    /// @return the object value
    private static JsonObject asObject(JsonValue value, String path) {
        if (value instanceof JsonObject object) {
            return object;
        }
        throw failure(path, "must be an object");
    }

    /// Casts one parsed value to a string.
    ///
    /// @param value the parsed value
    /// @param path the value path
    /// @return the decoded string
    private static String asString(JsonValue value, String path) {
        if (value instanceof JsonString string) {
            return string.value();
        }
        throw failure(path, "must be a string");
    }

    /// Rejects an empty collection.
    ///
    /// @param values the candidate collection
    /// @param path the collection path
    private static void requireNonEmpty(List<?> values, String path) {
        if (values.isEmpty()) {
            throw failure(path, "must not be empty");
        }
    }

    /// Rejects a duplicate declaration name.
    ///
    /// @param names the names already observed
    /// @param name the candidate name
    /// @param path the candidate path
    private static void requireUnique(Set<String> names, String name, String path) {
        if (!names.add(name)) {
            throw failure(path, "duplicates name '" + name + "'");
        }
    }

    /// Creates a stable protocol failure.
    ///
    /// @param path the failing value path
    /// @param message the failure detail
    /// @return the exception to throw
    private static IllegalArgumentException failure(String path, String message) {
        return new IllegalArgumentException(path + ": " + message);
    }
}
