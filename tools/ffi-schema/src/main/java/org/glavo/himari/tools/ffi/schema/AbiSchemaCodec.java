package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonArray;
import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonBoolean;
import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonNull;
import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonNumber;
import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonObject;
import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonString;
import static org.glavo.himari.tools.ffi.schema.StrictJson.JsonValue;

/// Reads and writes the strict canonical JSON representation of [AbiSchema].
@NotNullByDefault
public final class AbiSchemaCodec {
    /// The schema identifier embedded in every version-1 document.
    public static final String SCHEMA_ID = "schema/ffi-schema.schema.json";

    /// Prevents instantiation of this utility class.
    private AbiSchemaCodec() {
    }

    /// Reads, structurally decodes, and semantically validates a UTF-8 schema document.
    ///
    /// @param path the schema path
    /// @return the validated schema
    /// @throws IllegalArgumentException if the document cannot be read, parsed, decoded, or validated
    public static AbiSchema read(Path path) {
        return decode(StrictJson.parseObject(path), path.toString());
    }

    /// Parses, structurally decodes, and semantically validates schema JSON held in memory.
    ///
    /// @param sourceName the name used in diagnostics
    /// @param json the complete JSON document
    /// @return the validated schema
    /// @throws IllegalArgumentException if `json` cannot be parsed, decoded, or validated
    public static AbiSchema read(String sourceName, String json) {
        return decode(StrictJson.parseObject(sourceName, json), sourceName);
    }

    /// Returns the deterministic canonical JSON representation of a valid schema.
    ///
    /// @param schema the schema to encode
    /// @return canonical JSON ending in one newline
    /// @throws IllegalArgumentException if `schema` is not semantically valid
    public static String write(AbiSchema schema) {
        AbiSchemaValidator.requireValid(schema);
        StringBuilder output = new StringBuilder();
        appendJson(output, encode(schema), 0);
        return output.append('\n').toString();
    }

    /// Writes a deterministic canonical UTF-8 schema document, replacing any existing file.
    ///
    /// @param path the destination path
    /// @param schema the schema to encode
    /// @throws IllegalArgumentException if `schema` is invalid or the destination cannot be written
    public static void write(Path path, AbiSchema schema) {
        try {
            @Nullable Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, write(schema), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot write ABI schema " + path, exception);
        }
    }

    /// Decodes the top-level schema object and applies semantic validation.
    ///
    /// @param document the parsed object
    /// @param sourceName the name used in diagnostics
    /// @return the validated schema
    private static AbiSchema decode(JsonObject document, String sourceName) {
        requireExactKeys(document, sourceName, Set.of(
                "$schema",
                "schemaVersion",
                "namespace",
                "library",
                "target",
                "types",
                "callbacks",
                "functions"
        ));
        String schemaId = stringMember(document, "$schema", sourceName);
        if (!schemaId.equals(SCHEMA_ID)) {
            throw failure(sourceName + ".$schema", "must equal '" + SCHEMA_ID + "'");
        }

        AbiSchema schema = new AbiSchema(
                intMember(document, "schemaVersion", sourceName),
                stringMember(document, "namespace", sourceName),
                stringMember(document, "library", sourceName),
                decodeTarget(objectMember(document, "target", sourceName), sourceName + ".target"),
                decodeTypes(arrayMember(document, "types", sourceName), sourceName + ".types"),
                decodeCallbacks(arrayMember(document, "callbacks", sourceName), sourceName + ".callbacks"),
                decodeFunctions(arrayMember(document, "functions", sourceName), sourceName + ".functions")
        );
        AbiSchemaValidator.requireValid(schema);
        return schema;
    }

    /// Decodes an ABI target object.
    ///
    /// @param object the parsed target object
    /// @param path the object path
    /// @return the target descriptor
    private static AbiSchema.Target decodeTarget(JsonObject object, String path) {
        requireExactKeys(object, path, Set.of(
                "operatingSystem",
                "architecture",
                "byteOrder",
                "addressSize",
                "addressAlignment"
        ));
        return new AbiSchema.Target(
                stringMember(object, "operatingSystem", path),
                stringMember(object, "architecture", path),
                enumMember(object, "byteOrder", path, AbiSchema.ByteOrder.class),
                intMember(object, "addressSize", path),
                intMember(object, "addressAlignment", path)
        );
    }

    /// Decodes all named type declarations.
    ///
    /// @param values the parsed declaration array
    /// @param path the array path
    /// @return a mutable decoded list
    private static List<AbiSchema.TypeDefinition> decodeTypes(List<JsonValue> values, String path) {
        List<AbiSchema.TypeDefinition> types = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String itemPath = path + "[" + index + "]";
            JsonObject object = asObject(values.get(index), itemPath);
            String kind = stringMember(object, "kind", itemPath);
            types.add(switch (kind) {
                case "primitive" -> decodePrimitive(object, itemPath);
                case "pointer" -> decodePointer(object, itemPath);
                case "handle" -> decodeHandle(object, itemPath);
                case "aggregate" -> decodeAggregate(object, itemPath);
                case "integer-set" -> decodeIntegerSet(object, itemPath);
                default -> throw failure(itemPath + ".kind", "unsupported type kind '" + kind + "'");
            });
        }
        return types;
    }

    /// Decodes one primitive declaration.
    ///
    /// @param object the parsed declaration
    /// @param path the declaration path
    /// @return the primitive type
    private static AbiSchema.PrimitiveType decodePrimitive(JsonObject object, String path) {
        requireExactKeys(object, path, Set.of(
                "kind",
                "name",
                "primitiveKind",
                "byteSize",
                "alignment",
                "signedness",
                "availability"
        ));
        return new AbiSchema.PrimitiveType(
                stringMember(object, "name", path),
                enumMember(object, "primitiveKind", path, AbiSchema.PrimitiveKind.class),
                intMember(object, "byteSize", path),
                intMember(object, "alignment", path),
                enumMember(object, "signedness", path, AbiSchema.Signedness.class),
                decodeAvailability(objectMember(object, "availability", path), path + ".availability")
        );
    }

    /// Decodes one pointer declaration.
    ///
    /// @param object the parsed declaration
    /// @param path the declaration path
    /// @return the pointer type
    private static AbiSchema.PointerType decodePointer(JsonObject object, String path) {
        requireExactKeys(object, path, Set.of("kind", "name", "pointee", "constant", "availability"));
        return new AbiSchema.PointerType(
                stringMember(object, "name", path),
                typeRefMember(object, "pointee", path),
                booleanMember(object, "constant", path),
                decodeAvailability(objectMember(object, "availability", path), path + ".availability")
        );
    }

    /// Decodes one opaque handle declaration.
    ///
    /// @param object the parsed declaration
    /// @param path the declaration path
    /// @return the handle type
    private static AbiSchema.HandleType decodeHandle(JsonObject object, String path) {
        requireExactKeys(object, path, Set.of(
                "kind",
                "name",
                "representation",
                "invalidValues",
                "availability"
        ));
        List<JsonValue> values = arrayMember(object, "invalidValues", path);
        List<Long> invalidValues = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            invalidValues.add(asLong(values.get(index), path + ".invalidValues[" + index + "]"));
        }
        return new AbiSchema.HandleType(
                stringMember(object, "name", path),
                typeRefMember(object, "representation", path),
                invalidValues,
                decodeAvailability(objectMember(object, "availability", path), path + ".availability")
        );
    }

    /// Decodes one structure or union declaration.
    ///
    /// @param object the parsed declaration
    /// @param path the declaration path
    /// @return the aggregate type
    private static AbiSchema.AggregateType decodeAggregate(JsonObject object, String path) {
        requireExactKeys(object, path, Set.of(
                "kind",
                "name",
                "aggregateKind",
                "byteSize",
                "alignment",
                "packing",
                "fields",
                "availability"
        ));
        List<JsonValue> values = arrayMember(object, "fields", path);
        List<AbiSchema.AggregateField> fields = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String fieldPath = path + ".fields[" + index + "]";
            JsonObject field = asObject(values.get(index), fieldPath);
            requireExactKeys(field, fieldPath, Set.of("name", "type", "byteOffset", "bitOffset", "bitWidth"));
            fields.add(new AbiSchema.AggregateField(
                    stringMember(field, "name", fieldPath),
                    typeRefMember(field, "type", fieldPath),
                    longMember(field, "byteOffset", fieldPath),
                    nullableIntMember(field, "bitOffset", fieldPath),
                    nullableIntMember(field, "bitWidth", fieldPath)
            ));
        }
        return new AbiSchema.AggregateType(
                stringMember(object, "name", path),
                enumMember(object, "aggregateKind", path, AbiSchema.AggregateKind.class),
                longMember(object, "byteSize", path),
                intMember(object, "alignment", path),
                intMember(object, "packing", path),
                fields,
                decodeAvailability(objectMember(object, "availability", path), path + ".availability")
        );
    }

    /// Decodes one enum or flags declaration.
    ///
    /// @param object the parsed declaration
    /// @param path the declaration path
    /// @return the integer-set type
    private static AbiSchema.IntegerSetType decodeIntegerSet(JsonObject object, String path) {
        requireExactKeys(object, path, Set.of(
                "kind",
                "name",
                "integerSetKind",
                "representation",
                "values",
                "availability"
        ));
        List<JsonValue> values = arrayMember(object, "values", path);
        List<AbiSchema.IntegerValue> integerValues = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String valuePath = path + ".values[" + index + "]";
            JsonObject value = asObject(values.get(index), valuePath);
            requireExactKeys(value, valuePath, Set.of("name", "value"));
            integerValues.add(new AbiSchema.IntegerValue(
                    stringMember(value, "name", valuePath),
                    longMember(value, "value", valuePath)
            ));
        }
        return new AbiSchema.IntegerSetType(
                stringMember(object, "name", path),
                enumMember(object, "integerSetKind", path, AbiSchema.IntegerSetKind.class),
                typeRefMember(object, "representation", path),
                integerValues,
                decodeAvailability(objectMember(object, "availability", path), path + ".availability")
        );
    }

    /// Decodes all callback declarations.
    ///
    /// @param values the parsed callback array
    /// @param path the array path
    /// @return a mutable decoded list
    private static List<AbiSchema.CallbackDefinition> decodeCallbacks(List<JsonValue> values, String path) {
        List<AbiSchema.CallbackDefinition> callbacks = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String itemPath = path + "[" + index + "]";
            JsonObject object = asObject(values.get(index), itemPath);
            requireExactKeys(object, itemPath, Set.of(
                    "name",
                    "result",
                    "parameters",
                    "callingConvention",
                    "threadRestriction",
                    "lifetime",
                    "exceptionPolicy",
                    "availability"
            ));
            callbacks.add(new AbiSchema.CallbackDefinition(
                    stringMember(object, "name", itemPath),
                    decodeReturnValue(objectMember(object, "result", itemPath), itemPath + ".result"),
                    decodeParameters(arrayMember(object, "parameters", itemPath), itemPath + ".parameters"),
                    enumMember(object, "callingConvention", itemPath, AbiSchema.CallingConvention.class),
                    enumMember(object, "threadRestriction", itemPath, AbiSchema.ThreadRestriction.class),
                    enumMember(object, "lifetime", itemPath, AbiSchema.CallbackLifetime.class),
                    enumMember(object, "exceptionPolicy", itemPath, AbiSchema.CallbackExceptionPolicy.class),
                    decodeAvailability(objectMember(object, "availability", itemPath), itemPath + ".availability")
            ));
        }
        return callbacks;
    }

    /// Decodes all function declarations.
    ///
    /// @param values the parsed function array
    /// @param path the array path
    /// @return a mutable decoded list
    private static List<AbiSchema.FunctionDefinition> decodeFunctions(List<JsonValue> values, String path) {
        List<AbiSchema.FunctionDefinition> functions = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String itemPath = path + "[" + index + "]";
            JsonObject object = asObject(values.get(index), itemPath);
            requireExactKeys(object, itemPath, Set.of(
                    "name",
                    "symbol",
                    "result",
                    "parameters",
                    "callingConvention",
                    "variadicFrom",
                    "errorPolicy",
                    "threadRestriction",
                    "availability"
            ));
            functions.add(new AbiSchema.FunctionDefinition(
                    stringMember(object, "name", itemPath),
                    stringMember(object, "symbol", itemPath),
                    decodeReturnValue(objectMember(object, "result", itemPath), itemPath + ".result"),
                    decodeParameters(arrayMember(object, "parameters", itemPath), itemPath + ".parameters"),
                    enumMember(object, "callingConvention", itemPath, AbiSchema.CallingConvention.class),
                    nullableIntMember(object, "variadicFrom", itemPath),
                    enumMember(object, "errorPolicy", itemPath, AbiSchema.ErrorPolicy.class),
                    enumMember(object, "threadRestriction", itemPath, AbiSchema.ThreadRestriction.class),
                    decodeAvailability(objectMember(object, "availability", itemPath), itemPath + ".availability")
            ));
        }
        return functions;
    }

    /// Decodes an ordered parameter array.
    ///
    /// @param values the parsed parameter array
    /// @param path the array path
    /// @return a mutable decoded list
    private static List<AbiSchema.Parameter> decodeParameters(List<JsonValue> values, String path) {
        List<AbiSchema.Parameter> parameters = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String itemPath = path + "[" + index + "]";
            JsonObject object = asObject(values.get(index), itemPath);
            requireExactKeys(object, itemPath, Set.of("name", "type", "direction", "nullability", "ownership"));
            parameters.add(new AbiSchema.Parameter(
                    stringMember(object, "name", itemPath),
                    typeRefMember(object, "type", itemPath),
                    enumMember(object, "direction", itemPath, AbiSchema.ParameterDirection.class),
                    enumMember(object, "nullability", itemPath, AbiSchema.Nullability.class),
                    enumMember(object, "ownership", itemPath, AbiSchema.Ownership.class)
            ));
        }
        return parameters;
    }

    /// Decodes a callable return object.
    ///
    /// @param object the parsed return object
    /// @param path the object path
    /// @return the return contract
    private static AbiSchema.ReturnValue decodeReturnValue(JsonObject object, String path) {
        requireExactKeys(object, path, Set.of("type", "nullability", "ownership"));
        return new AbiSchema.ReturnValue(
                typeRefMember(object, "type", path),
                enumMember(object, "nullability", path, AbiSchema.Nullability.class),
                enumMember(object, "ownership", path, AbiSchema.Ownership.class)
        );
    }

    /// Decodes declaration availability metadata.
    ///
    /// @param object the parsed availability object
    /// @param path the object path
    /// @return the availability contract
    private static AbiSchema.Availability decodeAvailability(JsonObject object, String path) {
        requireExactKeys(object, path, Set.of("introduced", "deprecated", "unavailable"));
        return new AbiSchema.Availability(
                nullableStringMember(object, "introduced", path),
                nullableStringMember(object, "deprecated", path),
                booleanMember(object, "unavailable", path)
        );
    }

    /// Encodes the complete schema as an ordered JSON object tree.
    ///
    /// @param schema the valid schema
    /// @return the encoded root object
    private static JsonObject encode(AbiSchema schema) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("$schema", string(SCHEMA_ID));
        members.put("schemaVersion", number(schema.schemaVersion()));
        members.put("namespace", string(schema.namespace()));
        members.put("library", string(schema.library()));
        members.put("target", encodeTarget(schema.target()));
        members.put("types", array(schema.types().stream().map(AbiSchemaCodec::encodeType).toList()));
        members.put("callbacks", array(schema.callbacks().stream().map(AbiSchemaCodec::encodeCallback).toList()));
        members.put("functions", array(schema.functions().stream().map(AbiSchemaCodec::encodeFunction).toList()));
        return object(members);
    }

    /// Encodes one target descriptor.
    ///
    /// @param target the target to encode
    /// @return the encoded object
    private static JsonObject encodeTarget(AbiSchema.Target target) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("operatingSystem", string(target.operatingSystem()));
        members.put("architecture", string(target.architecture()));
        members.put("byteOrder", enumeration(target.byteOrder()));
        members.put("addressSize", number(target.addressSize()));
        members.put("addressAlignment", number(target.addressAlignment()));
        return object(members);
    }

    /// Encodes one concrete type declaration.
    ///
    /// @param type the type to encode
    /// @return the encoded object
    private static JsonObject encodeType(AbiSchema.TypeDefinition type) {
        return switch (type) {
            case AbiSchema.PrimitiveType primitive -> encodePrimitive(primitive);
            case AbiSchema.PointerType pointer -> encodePointer(pointer);
            case AbiSchema.HandleType handle -> encodeHandle(handle);
            case AbiSchema.AggregateType aggregate -> encodeAggregate(aggregate);
            case AbiSchema.IntegerSetType integerSet -> encodeIntegerSet(integerSet);
        };
    }

    /// Encodes one primitive declaration.
    ///
    /// @param primitive the primitive to encode
    /// @return the encoded object
    private static JsonObject encodePrimitive(AbiSchema.PrimitiveType primitive) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("kind", string("primitive"));
        members.put("name", string(primitive.name()));
        members.put("primitiveKind", enumeration(primitive.kind()));
        members.put("byteSize", number(primitive.byteSize()));
        members.put("alignment", number(primitive.alignment()));
        members.put("signedness", enumeration(primitive.signedness()));
        members.put("availability", encodeAvailability(primitive.availability()));
        return object(members);
    }

    /// Encodes one pointer declaration.
    ///
    /// @param pointer the pointer to encode
    /// @return the encoded object
    private static JsonObject encodePointer(AbiSchema.PointerType pointer) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("kind", string("pointer"));
        members.put("name", string(pointer.name()));
        members.put("pointee", string(pointer.pointee().name()));
        members.put("constant", bool(pointer.constant()));
        members.put("availability", encodeAvailability(pointer.availability()));
        return object(members);
    }

    /// Encodes one opaque handle declaration.
    ///
    /// @param handle the handle to encode
    /// @return the encoded object
    private static JsonObject encodeHandle(AbiSchema.HandleType handle) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("kind", string("handle"));
        members.put("name", string(handle.name()));
        members.put("representation", string(handle.representation().name()));
        members.put("invalidValues", array(handle.invalidValues().stream().map(AbiSchemaCodec::number).toList()));
        members.put("availability", encodeAvailability(handle.availability()));
        return object(members);
    }

    /// Encodes one structure or union declaration.
    ///
    /// @param aggregate the aggregate to encode
    /// @return the encoded object
    private static JsonObject encodeAggregate(AbiSchema.AggregateType aggregate) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("kind", string("aggregate"));
        members.put("name", string(aggregate.name()));
        members.put("aggregateKind", enumeration(aggregate.kind()));
        members.put("byteSize", number(aggregate.byteSize()));
        members.put("alignment", number(aggregate.alignment()));
        members.put("packing", number(aggregate.packing()));
        members.put("fields", array(aggregate.fields().stream().map(AbiSchemaCodec::encodeField).toList()));
        members.put("availability", encodeAvailability(aggregate.availability()));
        return object(members);
    }

    /// Encodes one aggregate field.
    ///
    /// @param field the field to encode
    /// @return the encoded object
    private static JsonObject encodeField(AbiSchema.AggregateField field) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("name", string(field.name()));
        members.put("type", string(field.type().name()));
        members.put("byteOffset", number(field.byteOffset()));
        members.put("bitOffset", nullableNumber(field.bitOffset()));
        members.put("bitWidth", nullableNumber(field.bitWidth()));
        return object(members);
    }

    /// Encodes one enum or flags declaration.
    ///
    /// @param integerSet the integer set to encode
    /// @return the encoded object
    private static JsonObject encodeIntegerSet(AbiSchema.IntegerSetType integerSet) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("kind", string("integer-set"));
        members.put("name", string(integerSet.name()));
        members.put("integerSetKind", enumeration(integerSet.kind()));
        members.put("representation", string(integerSet.representation().name()));
        members.put("values", array(integerSet.values().stream().map(AbiSchemaCodec::encodeIntegerValue).toList()));
        members.put("availability", encodeAvailability(integerSet.availability()));
        return object(members);
    }

    /// Encodes one named enum or flag value.
    ///
    /// @param value the value to encode
    /// @return the encoded object
    private static JsonObject encodeIntegerValue(AbiSchema.IntegerValue value) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("name", string(value.name()));
        members.put("value", number(value.value()));
        return object(members);
    }

    /// Encodes one callback declaration.
    ///
    /// @param callback the callback to encode
    /// @return the encoded object
    private static JsonObject encodeCallback(AbiSchema.CallbackDefinition callback) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("name", string(callback.name()));
        members.put("result", encodeReturnValue(callback.result()));
        members.put("parameters", array(callback.parameters().stream().map(AbiSchemaCodec::encodeParameter).toList()));
        members.put("callingConvention", enumeration(callback.callingConvention()));
        members.put("threadRestriction", enumeration(callback.threadRestriction()));
        members.put("lifetime", enumeration(callback.lifetime()));
        members.put("exceptionPolicy", enumeration(callback.exceptionPolicy()));
        members.put("availability", encodeAvailability(callback.availability()));
        return object(members);
    }

    /// Encodes one function declaration.
    ///
    /// @param function the function to encode
    /// @return the encoded object
    private static JsonObject encodeFunction(AbiSchema.FunctionDefinition function) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("name", string(function.name()));
        members.put("symbol", string(function.symbol()));
        members.put("result", encodeReturnValue(function.result()));
        members.put("parameters", array(function.parameters().stream().map(AbiSchemaCodec::encodeParameter).toList()));
        members.put("callingConvention", enumeration(function.callingConvention()));
        members.put("variadicFrom", nullableNumber(function.variadicFrom()));
        members.put("errorPolicy", enumeration(function.errorPolicy()));
        members.put("threadRestriction", enumeration(function.threadRestriction()));
        members.put("availability", encodeAvailability(function.availability()));
        return object(members);
    }

    /// Encodes one callable parameter.
    ///
    /// @param parameter the parameter to encode
    /// @return the encoded object
    private static JsonObject encodeParameter(AbiSchema.Parameter parameter) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("name", string(parameter.name()));
        members.put("type", string(parameter.type().name()));
        members.put("direction", enumeration(parameter.direction()));
        members.put("nullability", enumeration(parameter.nullability()));
        members.put("ownership", enumeration(parameter.ownership()));
        return object(members);
    }

    /// Encodes one callable return contract.
    ///
    /// @param result the return value to encode
    /// @return the encoded object
    private static JsonObject encodeReturnValue(AbiSchema.ReturnValue result) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("type", string(result.type().name()));
        members.put("nullability", enumeration(result.nullability()));
        members.put("ownership", enumeration(result.ownership()));
        return object(members);
    }

    /// Encodes declaration availability metadata.
    ///
    /// @param availability the metadata to encode
    /// @return the encoded object
    private static JsonObject encodeAvailability(AbiSchema.Availability availability) {
        Map<String, JsonValue> members = orderedMembers();
        members.put("introduced", nullableString(availability.introduced()));
        members.put("deprecated", nullableString(availability.deprecated()));
        members.put("unavailable", bool(availability.unavailable()));
        return object(members);
    }

    /// Appends a JSON value using two-space indentation and stable object order.
    ///
    /// @param output the destination builder
    /// @param value the value to encode
    /// @param indentation the current indentation width
    private static void appendJson(StringBuilder output, JsonValue value, int indentation) {
        switch (value) {
            case JsonObject object -> appendObject(output, object, indentation);
            case JsonArray array -> appendArray(output, array, indentation);
            case JsonString string -> appendQuoted(output, string.value());
            case JsonNumber number -> output.append(number.value());
            case JsonBoolean bool -> output.append(bool.value());
            case JsonNull ignored -> output.append("null");
        }
    }

    /// Appends one canonical JSON object.
    ///
    /// @param output the destination builder
    /// @param object the object to encode
    /// @param indentation the current indentation width
    private static void appendObject(StringBuilder output, JsonObject object, int indentation) {
        if (object.members().isEmpty()) {
            output.append("{}");
            return;
        }
        output.append("{\n");
        int index = 0;
        for (Map.Entry<String, JsonValue> entry : object.members().entrySet()) {
            appendIndentation(output, indentation + 2);
            appendQuoted(output, entry.getKey());
            output.append(": ");
            appendJson(output, entry.getValue(), indentation + 2);
            if (++index < object.members().size()) {
                output.append(',');
            }
            output.append('\n');
        }
        appendIndentation(output, indentation);
        output.append('}');
    }

    /// Appends one canonical JSON array.
    ///
    /// @param output the destination builder
    /// @param array the array to encode
    /// @param indentation the current indentation width
    private static void appendArray(StringBuilder output, JsonArray array, int indentation) {
        if (array.elements().isEmpty()) {
            output.append("[]");
            return;
        }
        output.append("[\n");
        for (int index = 0; index < array.elements().size(); index++) {
            appendIndentation(output, indentation + 2);
            appendJson(output, array.elements().get(index), indentation + 2);
            if (index + 1 < array.elements().size()) {
                output.append(',');
            }
            output.append('\n');
        }
        appendIndentation(output, indentation);
        output.append(']');
    }

    /// Appends a quoted and escaped JSON string.
    ///
    /// @param output the destination builder
    /// @param value the decoded string value
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

    /// Appends a fixed number of spaces.
    ///
    /// @param output the destination builder
    /// @param count the non-negative space count
    private static void appendIndentation(StringBuilder output, int count) {
        output.append(" ".repeat(count));
    }

    /// Returns a mutable insertion-ordered member map.
    ///
    /// @return a new empty member map
    private static Map<String, JsonValue> orderedMembers() {
        return new LinkedHashMap<>();
    }

    /// Wraps an ordered member map as an immutable JSON object.
    ///
    /// @param members the object members
    /// @return the JSON object
    private static JsonObject object(Map<String, JsonValue> members) {
        return new JsonObject(members);
    }

    /// Wraps a value list as an immutable JSON array.
    ///
    /// @param values the array values
    /// @return the JSON array
    private static JsonArray array(List<? extends JsonValue> values) {
        return new JsonArray(List.copyOf(values));
    }

    /// Wraps a Java string as a JSON string.
    ///
    /// @param value the decoded string
    /// @return the JSON string
    private static JsonString string(String value) {
        return new JsonString(value);
    }

    /// Wraps a nullable Java string as a JSON string or null.
    ///
    /// @param value the nullable decoded string
    /// @return the JSON value
    private static JsonValue nullableString(@Nullable String value) {
        return value == null ? JsonNull.INSTANCE : string(value);
    }

    /// Wraps a Java integer as an integral JSON number.
    ///
    /// @param value the integer value
    /// @return the JSON number
    private static JsonNumber number(long value) {
        return new JsonNumber(value, true);
    }

    /// Wraps a nullable Java integer as a JSON number or null.
    ///
    /// @param value the nullable integer value
    /// @return the JSON value
    private static JsonValue nullableNumber(@Nullable Integer value) {
        return value == null ? JsonNull.INSTANCE : number(value);
    }

    /// Wraps a Java Boolean as a JSON Boolean.
    ///
    /// @param value the Boolean value
    /// @return the JSON Boolean
    private static JsonBoolean bool(boolean value) {
        return new JsonBoolean(value);
    }

    /// Encodes an enum using lowercase underscore-separated text.
    ///
    /// @param value the enum value
    /// @return the JSON string
    private static JsonString enumeration(Enum<?> value) {
        return string(value.name().toLowerCase(Locale.ROOT));
    }

    /// Returns a required JSON object member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing object path
    /// @return the decoded object
    private static JsonObject objectMember(JsonObject object, String name, String path) {
        return asObject(member(object, name, path), path + "." + name);
    }

    /// Returns a required JSON array member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing object path
    /// @return the immutable array elements
    private static @Unmodifiable List<JsonValue> arrayMember(JsonObject object, String name, String path) {
        JsonValue value = member(object, name, path);
        if (value instanceof JsonArray array) {
            return array.elements();
        }
        throw failure(path + "." + name, "must be an array");
    }

    /// Returns a required JSON string member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing object path
    /// @return the decoded string
    private static String stringMember(JsonObject object, String name, String path) {
        JsonValue value = member(object, name, path);
        if (value instanceof JsonString string) {
            return string.value();
        }
        throw failure(path + "." + name, "must be a string");
    }

    /// Returns a nullable JSON string member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing object path
    /// @return the decoded string, or `null` for JSON null
    private static @Nullable String nullableStringMember(JsonObject object, String name, String path) {
        JsonValue value = member(object, name, path);
        if (value == JsonNull.INSTANCE) {
            return null;
        }
        if (value instanceof JsonString string) {
            return string.value();
        }
        throw failure(path + "." + name, "must be a string or null");
    }

    /// Returns a required JSON Boolean member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing object path
    /// @return the Boolean value
    private static boolean booleanMember(JsonObject object, String name, String path) {
        JsonValue value = member(object, name, path);
        if (value instanceof JsonBoolean bool) {
            return bool.value();
        }
        throw failure(path + "." + name, "must be a Boolean");
    }

    /// Returns a required 32-bit integral JSON member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing object path
    /// @return the integral value
    private static int intMember(JsonObject object, String name, String path) {
        long value = longMember(object, name, path);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw failure(path + "." + name, "must fit in a signed 32-bit integer");
        }
        return (int) value;
    }

    /// Returns a nullable 32-bit integral JSON member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing object path
    /// @return the integral value, or `null` for JSON null
    private static @Nullable Integer nullableIntMember(JsonObject object, String name, String path) {
        JsonValue value = member(object, name, path);
        if (value == JsonNull.INSTANCE) {
            return null;
        }
        long decoded = asLong(value, path + "." + name);
        if (decoded < Integer.MIN_VALUE || decoded > Integer.MAX_VALUE) {
            throw failure(path + "." + name, "must fit in a signed 32-bit integer");
        }
        return (int) decoded;
    }

    /// Returns a required 64-bit integral JSON member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing object path
    /// @return the integral value
    private static long longMember(JsonObject object, String name, String path) {
        return asLong(member(object, name, path), path + "." + name);
    }

    /// Decodes a required named type reference from a string member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing object path
    /// @return the type reference
    private static AbiSchema.TypeRef typeRefMember(JsonObject object, String name, String path) {
        return new AbiSchema.TypeRef(stringMember(object, name, path));
    }

    /// Decodes a lowercase enum member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing object path
    /// @param enumType the expected enum class
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
            throw failure(path + "." + name, "has unsupported value '" + value + "'");
        }
    }

    /// Returns a JSON member after verifying its presence.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param path the containing object path
    /// @return the member value
    private static JsonValue member(JsonObject object, String name, String path) {
        if (!object.members().containsKey(name)) {
            throw failure(path, "is missing member '" + name + "'");
        }
        return object.members().getOrDefault(name, JsonNull.INSTANCE);
    }

    /// Requires a parsed value to be an object.
    ///
    /// @param value the candidate value
    /// @param path the value path
    /// @return the object value
    private static JsonObject asObject(JsonValue value, String path) {
        if (value instanceof JsonObject object) {
            return object;
        }
        throw failure(path, "must be an object");
    }

    /// Requires a parsed value to be an integral signed 64-bit number.
    ///
    /// @param value the candidate value
    /// @param path the value path
    /// @return the integral value
    private static long asLong(JsonValue value, String path) {
        if (value instanceof JsonNumber number && number.integral()) {
            return number.value().longValue();
        }
        throw failure(path, "must be an integer");
    }

    /// Requires an object to contain exactly the expected keys.
    ///
    /// @param object the object to inspect
    /// @param path the object path
    /// @param expectedKeys the exact key set
    private static void requireExactKeys(JsonObject object, String path, Set<String> expectedKeys) {
        if (object.members().keySet().equals(expectedKeys)) {
            return;
        }
        Set<String> missing = new java.util.TreeSet<>(expectedKeys);
        missing.removeAll(object.members().keySet());
        Set<String> unexpected = new java.util.TreeSet<>(object.members().keySet());
        unexpected.removeAll(expectedKeys);
        throw failure(path, "has missing keys " + missing + " and unexpected keys " + unexpected);
    }

    /// Creates a structural codec failure.
    ///
    /// @param path the failing document path
    /// @param message the failure detail
    /// @return the exception to throw
    private static IllegalArgumentException failure(String path, String message) {
        return new IllegalArgumentException(path + ": " + message);
    }
}
