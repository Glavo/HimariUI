package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/// Validates cross-declaration, layout, and callable invariants in an [AbiSchema].
@NotNullByDefault
public final class AbiSchemaValidator {
    /// The sole schema format accepted by the first canonical model.
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    /// Matches portable C-style declaration identifiers.
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /// Matches dot-separated Java-style namespaces.
    private static final Pattern NAMESPACE = Pattern.compile(
            "[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)*"
    );

    /// Matches stable lowercase target identifiers.
    private static final Pattern TARGET_IDENTIFIER = Pattern.compile("[a-z0-9]+(?:[-_][a-z0-9]+)*");

    /// Matches dotted numeric platform versions.
    private static final Pattern PLATFORM_VERSION = Pattern.compile("[0-9]+(?:\\.[0-9]+)*");

    /// Prevents instantiation of this utility class.
    private AbiSchemaValidator() {
    }

    /// Returns every validation issue in deterministic declaration order.
    ///
    /// @param schema the schema to validate
    /// @return an immutable issue list, empty when the schema is valid
    public static @Unmodifiable List<Issue> validate(AbiSchema schema) {
        List<Issue> issues = new ArrayList<>();
        validateHeader(schema, issues);

        Map<String, AbiSchema.TypeDefinition> types = new LinkedHashMap<>();
        for (int index = 0; index < schema.types().size(); index++) {
            AbiSchema.TypeDefinition type = schema.types().get(index);
            String path = "types[" + index + "]";
            validateIdentifier(type.name(), path + ".name", issues);
            @Nullable AbiSchema.TypeDefinition previous = types.putIfAbsent(type.name(), type);
            if (previous != null) {
                issue(issues, path + ".name", "duplicates type declaration '" + type.name() + "'");
            }
            validateAvailability(type.availability(), path + ".availability", issues);
        }

        Map<String, AbiSchema.CallbackDefinition> callbacks = new LinkedHashMap<>();
        for (int index = 0; index < schema.callbacks().size(); index++) {
            AbiSchema.CallbackDefinition callback = schema.callbacks().get(index);
            String path = "callbacks[" + index + "]";
            validateIdentifier(callback.name(), path + ".name", issues);
            @Nullable AbiSchema.CallbackDefinition previous = callbacks.putIfAbsent(callback.name(), callback);
            if (previous != null) {
                issue(issues, path + ".name", "duplicates callback declaration '" + callback.name() + "'");
            }
            if (types.containsKey(callback.name())) {
                issue(issues, path + ".name", "collides with type declaration '" + callback.name() + "'");
            }
            validateAvailability(callback.availability(), path + ".availability", issues);
        }

        validateTypes(schema, types, callbacks, issues);
        validateCallbacks(schema, types, callbacks, issues);
        validateFunctions(schema, types, callbacks, issues);
        return List.copyOf(issues);
    }

    /// Throws when a schema contains one or more validation issues.
    ///
    /// @param schema the schema to validate
    /// @throws IllegalArgumentException when validation fails
    public static void requireValid(AbiSchema schema) {
        List<Issue> issues = validate(schema);
        if (!issues.isEmpty()) {
            throw new IllegalArgumentException(formatIssues(issues));
        }
    }

    /// Validates document-level identity and target properties.
    ///
    /// @param schema the schema being validated
    /// @param issues the mutable issue sink
    private static void validateHeader(AbiSchema schema, List<Issue> issues) {
        if (schema.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            issue(issues, "schemaVersion", "must equal " + SUPPORTED_SCHEMA_VERSION);
        }
        if (schema.types().isEmpty()) {
            issue(issues, "types", "must contain at least one type declaration");
        }
        if (!NAMESPACE.matcher(schema.namespace()).matches()) {
            issue(issues, "namespace", "must be a lowercase dot-separated Java namespace");
        }
        validateIdentifier(schema.library(), "library", issues);

        AbiSchema.Target target = schema.target();
        if (!TARGET_IDENTIFIER.matcher(target.operatingSystem()).matches()) {
            issue(issues, "target.operatingSystem", "must be a stable lowercase target identifier");
        }
        if (!TARGET_IDENTIFIER.matcher(target.architecture()).matches()) {
            issue(issues, "target.architecture", "must be a stable lowercase target identifier");
        }
        if (target.addressSize() != 4 && target.addressSize() != 8) {
            issue(issues, "target.addressSize", "must be 4 or 8 bytes");
        }
        if (!isPowerOfTwo(target.addressAlignment())
                || target.addressAlignment() > target.addressSize()) {
            issue(issues, "target.addressAlignment", "must be a power of two no greater than addressSize");
        }
    }

    /// Validates every concrete type declaration.
    ///
    /// @param schema the schema being validated
    /// @param types the type registry
    /// @param callbacks the callback registry
    /// @param issues the mutable issue sink
    private static void validateTypes(
            AbiSchema schema,
            Map<String, AbiSchema.TypeDefinition> types,
            Map<String, AbiSchema.CallbackDefinition> callbacks,
            List<Issue> issues
    ) {
        for (int index = 0; index < schema.types().size(); index++) {
            AbiSchema.TypeDefinition type = schema.types().get(index);
            String path = "types[" + index + "]";
            switch (type) {
                case AbiSchema.PrimitiveType primitive -> validatePrimitive(primitive, path, issues);
                case AbiSchema.PointerType pointer -> requireResolved(
                        pointer.pointee(),
                        path + ".pointee",
                        types,
                        callbacks,
                        issues
                );
                case AbiSchema.HandleType handle -> validateHandle(handle, path, types, callbacks, issues);
                case AbiSchema.AggregateType aggregate -> validateAggregate(
                        schema,
                        aggregate,
                        path,
                        types,
                        callbacks,
                        issues
                );
                case AbiSchema.IntegerSetType integerSet -> validateIntegerSet(
                        integerSet,
                        path,
                        types,
                        callbacks,
                        issues
                );
            }
        }
    }

    /// Validates one fixed-width primitive declaration.
    ///
    /// @param primitive the primitive to validate
    /// @param path the declaration path
    /// @param issues the mutable issue sink
    private static void validatePrimitive(AbiSchema.PrimitiveType primitive, String path, List<Issue> issues) {
        switch (primitive.kind()) {
            case VOID -> {
                if (primitive.byteSize() != 0) {
                    issue(issues, path + ".byteSize", "VOID must have zero size");
                }
                if (primitive.alignment() != 1) {
                    issue(issues, path + ".alignment", "VOID must use alignment 1");
                }
                if (primitive.signedness() != AbiSchema.Signedness.NONE) {
                    issue(issues, path + ".signedness", "VOID must use NONE");
                }
            }
            case INTEGER -> {
                if (!Set.of(1, 2, 4, 8).contains(primitive.byteSize())) {
                    issue(issues, path + ".byteSize", "INTEGER must use 1, 2, 4, or 8 bytes");
                }
                if (primitive.signedness() == AbiSchema.Signedness.NONE) {
                    issue(issues, path + ".signedness", "INTEGER must declare SIGNED or UNSIGNED");
                }
                validateScalarAlignment(primitive, path, issues);
            }
            case FLOATING -> {
                if (!Set.of(4, 8).contains(primitive.byteSize())) {
                    issue(issues, path + ".byteSize", "FLOATING must use 4 or 8 bytes");
                }
                if (primitive.signedness() != AbiSchema.Signedness.NONE) {
                    issue(issues, path + ".signedness", "FLOATING must use NONE");
                }
                validateScalarAlignment(primitive, path, issues);
            }
        }
    }

    /// Validates alignment shared by stored scalar kinds.
    ///
    /// @param primitive the primitive declaration
    /// @param path the declaration path
    /// @param issues the mutable issue sink
    private static void validateScalarAlignment(
            AbiSchema.PrimitiveType primitive,
            String path,
            List<Issue> issues
    ) {
        if (!isPowerOfTwo(primitive.alignment()) || primitive.alignment() > primitive.byteSize()) {
            issue(issues, path + ".alignment", "must be a power of two no greater than byteSize");
        }
    }

    /// Validates one opaque handle declaration.
    ///
    /// @param handle the handle declaration
    /// @param path the declaration path
    /// @param types the type registry
    /// @param callbacks the callback registry
    /// @param issues the mutable issue sink
    private static void validateHandle(
            AbiSchema.HandleType handle,
            String path,
            Map<String, AbiSchema.TypeDefinition> types,
            Map<String, AbiSchema.CallbackDefinition> callbacks,
            List<Issue> issues
    ) {
        if (!requireResolved(handle.representation(), path + ".representation", types, callbacks, issues)) {
            return;
        }
        @Nullable AbiSchema.TypeDefinition representation = types.get(handle.representation().name());
        if (!(representation instanceof AbiSchema.PrimitiveType primitive
                && primitive.kind() == AbiSchema.PrimitiveKind.INTEGER)
                && !(representation instanceof AbiSchema.PointerType)) {
            issue(issues, path + ".representation", "must refer to an integer primitive or pointer");
        }
        if (new HashSet<>(handle.invalidValues()).size() != handle.invalidValues().size()) {
            issue(issues, path + ".invalidValues", "must not contain duplicates");
        }
    }

    /// Validates one aggregate layout and every field placement.
    ///
    /// @param schema the containing schema
    /// @param aggregate the aggregate declaration
    /// @param path the declaration path
    /// @param types the type registry
    /// @param callbacks the callback registry
    /// @param issues the mutable issue sink
    private static void validateAggregate(
            AbiSchema schema,
            AbiSchema.AggregateType aggregate,
            String path,
            Map<String, AbiSchema.TypeDefinition> types,
            Map<String, AbiSchema.CallbackDefinition> callbacks,
            List<Issue> issues
    ) {
        if (aggregate.byteSize() <= 0) {
            issue(issues, path + ".byteSize", "must be positive");
        }
        if (!isPowerOfTwo(aggregate.alignment())) {
            issue(issues, path + ".alignment", "must be a positive power of two");
        } else if (aggregate.byteSize() > 0 && aggregate.byteSize() % aggregate.alignment() != 0) {
            issue(issues, path + ".byteSize", "must be a multiple of alignment");
        }
        if (aggregate.packing() != 0 && !isPowerOfTwo(aggregate.packing())) {
            issue(issues, path + ".packing", "must be zero or a power of two");
        }
        if (aggregate.fields().isEmpty()) {
            issue(issues, path + ".fields", "must not be empty");
        }

        Set<String> fieldNames = new LinkedHashSet<>();
        List<Interval> ordinaryIntervals = new ArrayList<>();
        List<BitInterval> bitIntervals = new ArrayList<>();
        for (int index = 0; index < aggregate.fields().size(); index++) {
            AbiSchema.AggregateField field = aggregate.fields().get(index);
            String fieldPath = path + ".fields[" + index + "]";
            validateIdentifier(field.name(), fieldPath + ".name", issues);
            if (!fieldNames.add(field.name())) {
                issue(issues, fieldPath + ".name", "duplicates field '" + field.name() + "'");
            }
            if (field.byteOffset() < 0) {
                issue(issues, fieldPath + ".byteOffset", "must be non-negative");
            }
            if (aggregate.kind() == AbiSchema.AggregateKind.UNION && field.byteOffset() != 0) {
                issue(issues, fieldPath + ".byteOffset", "must be zero for a union field");
            }

            boolean resolved = requireResolved(field.type(), fieldPath + ".type", types, callbacks, issues);
            @Nullable Layout layout = resolved
                    ? layoutOf(field.type().name(), schema.target(), types, callbacks, new HashSet<>())
                    : null;
            if (resolved && layout == null) {
                issue(issues, fieldPath + ".type", "has a cyclic or non-storable direct layout");
            }
            if (layout != null) {
                boolean fits = field.byteOffset() >= 0
                        && layout.byteSize() >= 0
                        && layout.byteSize() <= aggregate.byteSize()
                        && field.byteOffset() <= aggregate.byteSize() - layout.byteSize();
                if (!fits) {
                    issue(issues, fieldPath, "extends past aggregate byteSize");
                }
                int effectiveAlignment = aggregate.packing() == 0
                        ? layout.alignment()
                        : Math.min(layout.alignment(), aggregate.packing());
                if (effectiveAlignment > 0 && field.byteOffset() >= 0
                        && field.byteOffset() % effectiveAlignment != 0) {
                    issue(issues, fieldPath + ".byteOffset", "violates effective field alignment " + effectiveAlignment);
                }
                @Nullable BitInterval bitInterval = validateBitfield(field, fieldPath, layout, types, issues);
                if (fits && aggregate.kind() == AbiSchema.AggregateKind.STRUCT && field.bitWidth() == null) {
                    Interval current = new Interval(
                            field.byteOffset(),
                            field.byteOffset() + layout.byteSize(),
                            fieldPath
                    );
                    ordinaryIntervals.stream()
                            .filter(previous -> current.start() < previous.end() && previous.start() < current.end())
                            .forEach(previous -> issue(
                                    issues,
                                    fieldPath,
                                    "overlaps ordinary field at " + previous.path()
                            ));
                    bitIntervals.stream()
                            .filter(previous -> current.start() < previous.storageEnd()
                                    && previous.storageStart() < current.end())
                            .forEach(previous -> issue(
                                    issues,
                                    fieldPath,
                                    "overlaps bitfield storage at " + previous.path()
                            ));
                    ordinaryIntervals.add(current);
                } else if (fits && aggregate.kind() == AbiSchema.AggregateKind.STRUCT && bitInterval != null) {
                    ordinaryIntervals.stream()
                            .filter(previous -> bitInterval.storageStart() < previous.end()
                                    && previous.start() < bitInterval.storageEnd())
                            .forEach(previous -> issue(
                                    issues,
                                    fieldPath,
                                    "overlaps ordinary field at " + previous.path()
                            ));
                    bitIntervals.stream()
                            .filter(previous -> bitInterval.bitStart() < previous.bitEnd()
                                    && previous.bitStart() < bitInterval.bitEnd())
                            .forEach(previous -> issue(
                                    issues,
                                    fieldPath,
                                    "overlaps bitfield at " + previous.path()
                            ));
                    bitIntervals.add(bitInterval);
                }
            }
        }
        if (hasDirectAggregateCycle(aggregate.name(), aggregate.name(), types, new HashSet<>())) {
            issue(issues, path + ".fields", "contains a direct aggregate layout cycle");
        }
    }

    /// Validates the paired bitfield metadata and storage range.
    ///
    /// @param field the field declaration
    /// @param path the field path
    /// @param layout the resolved storage layout
    /// @param types the type registry
    /// @param issues the mutable issue sink
    private static @Nullable BitInterval validateBitfield(
            AbiSchema.AggregateField field,
            String path,
            Layout layout,
            Map<String, AbiSchema.TypeDefinition> types,
            List<Issue> issues
    ) {
        if ((field.bitOffset() == null) != (field.bitWidth() == null)) {
            issue(issues, path, "must declare bitOffset and bitWidth together");
            return null;
        }
        if (field.bitOffset() == null) {
            return null;
        }

        @Nullable AbiSchema.TypeDefinition storage = types.get(field.type().name());
        boolean integerStorage = storage instanceof AbiSchema.PrimitiveType primitive
                && primitive.kind() == AbiSchema.PrimitiveKind.INTEGER
                || storage instanceof AbiSchema.IntegerSetType;
        if (!integerStorage) {
            issue(issues, path + ".type", "bitfield storage must be an integer primitive, enum, or flags type");
            return null;
        }
        int offset = field.bitOffset();
        int width = field.bitWidth();
        long storageBits = layout.byteSize() * 8L;
        if (offset < 0 || width <= 0 || (long) offset + width > storageBits) {
            issue(issues, path, "bit range must fit within the declared storage type");
            return null;
        }
        try {
            long storageStart = field.byteOffset();
            long storageEnd = Math.addExact(storageStart, layout.byteSize());
            long absoluteStart = Math.addExact(Math.multiplyExact(storageStart, 8L), offset);
            long absoluteEnd = Math.addExact(absoluteStart, width);
            return new BitInterval(storageStart, storageEnd, absoluteStart, absoluteEnd, path);
        } catch (ArithmeticException exception) {
            issue(issues, path, "bitfield offsets overflow signed 64-bit arithmetic");
            return null;
        }
    }

    /// Returns whether following directly embedded aggregate fields reaches the starting aggregate.
    ///
    /// @param root the aggregate whose layout is being checked
    /// @param current the aggregate currently being traversed
    /// @param types the type registry
    /// @param visiting aggregates on the current traversal path
    /// @return whether the traversal finds a direct aggregate layout cycle containing `root`
    private static boolean hasDirectAggregateCycle(
            String root,
            String current,
            Map<String, AbiSchema.TypeDefinition> types,
            Set<String> visiting
    ) {
        if (!visiting.add(current)) {
            return false;
        }
        try {
            @Nullable AbiSchema.TypeDefinition currentType = types.get(current);
            if (!(currentType instanceof AbiSchema.AggregateType aggregate)) {
                return false;
            }
            for (AbiSchema.AggregateField field : aggregate.fields()) {
                String referencedName = field.type().name();
                if (root.equals(referencedName)) {
                    return true;
                }
                if (types.get(referencedName) instanceof AbiSchema.AggregateType
                        && hasDirectAggregateCycle(root, referencedName, types, visiting)) {
                    return true;
                }
            }
            return false;
        } finally {
            visiting.remove(current);
        }
    }

    /// Validates one enum or flags declaration.
    ///
    /// @param integerSet the declaration to validate
    /// @param path the declaration path
    /// @param types the type registry
    /// @param callbacks the callback registry
    /// @param issues the mutable issue sink
    private static void validateIntegerSet(
            AbiSchema.IntegerSetType integerSet,
            String path,
            Map<String, AbiSchema.TypeDefinition> types,
            Map<String, AbiSchema.CallbackDefinition> callbacks,
            List<Issue> issues
    ) {
        if (!requireResolved(integerSet.representation(), path + ".representation", types, callbacks, issues)) {
            return;
        }
        @Nullable AbiSchema.TypeDefinition representation = types.get(integerSet.representation().name());
        if (!(representation instanceof AbiSchema.PrimitiveType primitive)
                || primitive.kind() != AbiSchema.PrimitiveKind.INTEGER) {
            issue(issues, path + ".representation", "must refer to an integer primitive");
        }
        if (integerSet.values().isEmpty()) {
            issue(issues, path + ".values", "must not be empty");
        }
        Set<String> names = new HashSet<>();
        for (int index = 0; index < integerSet.values().size(); index++) {
            AbiSchema.IntegerValue value = integerSet.values().get(index);
            String valuePath = path + ".values[" + index + "]";
            validateIdentifier(value.name(), valuePath + ".name", issues);
            if (!names.add(value.name())) {
                issue(issues, valuePath + ".name", "duplicates value '" + value.name() + "'");
            }
        }
    }

    /// Validates every callback signature.
    ///
    /// @param schema the schema being validated
    /// @param types the type registry
    /// @param callbacks the callback registry
    /// @param issues the mutable issue sink
    private static void validateCallbacks(
            AbiSchema schema,
            Map<String, AbiSchema.TypeDefinition> types,
            Map<String, AbiSchema.CallbackDefinition> callbacks,
            List<Issue> issues
    ) {
        for (int index = 0; index < schema.callbacks().size(); index++) {
            AbiSchema.CallbackDefinition callback = schema.callbacks().get(index);
            String path = "callbacks[" + index + "]";
            validateCallable(callback.result(), callback.parameters(), path, types, callbacks, issues);
            if (callback.callingConvention() != AbiSchema.CallingConvention.SYSTEM
                    && callback.callingConvention() != AbiSchema.CallingConvention.CDECL) {
                issue(issues, path + ".callingConvention", "is not supported for generated upcalls");
            }
            if (callback.exceptionPolicy() != AbiSchema.CallbackExceptionPolicy.CONTAIN) {
                issue(issues, path + ".exceptionPolicy", "must contain Java failures at the native boundary");
            }
        }
    }

    /// Validates every downcall function signature and native symbol.
    ///
    /// @param schema the schema being validated
    /// @param types the type registry
    /// @param callbacks the callback registry
    /// @param issues the mutable issue sink
    private static void validateFunctions(
            AbiSchema schema,
            Map<String, AbiSchema.TypeDefinition> types,
            Map<String, AbiSchema.CallbackDefinition> callbacks,
            List<Issue> issues
    ) {
        Set<String> names = new HashSet<>();
        for (int index = 0; index < schema.functions().size(); index++) {
            AbiSchema.FunctionDefinition function = schema.functions().get(index);
            String path = "functions[" + index + "]";
            validateIdentifier(function.name(), path + ".name", issues);
            if (!names.add(function.name())) {
                issue(issues, path + ".name", "duplicates function '" + function.name() + "'");
            }
            if (function.symbol().isBlank()) {
                issue(issues, path + ".symbol", "must not be blank");
            }
            validateAvailability(function.availability(), path + ".availability", issues);
            validateCallable(function.result(), function.parameters(), path, types, callbacks, issues);
            if (function.variadicFrom() != null
                    && (function.variadicFrom() <= 0
                    || function.variadicFrom() != function.parameters().size())) {
                issue(issues, path + ".variadicFrom", "must equal the positive fixed-parameter count");
            }
        }
    }

    /// Validates common result and parameter contracts.
    ///
    /// @param result the callable result
    /// @param parameters the callable parameters
    /// @param path the callable path
    /// @param types the type registry
    /// @param callbacks the callback registry
    /// @param issues the mutable issue sink
    private static void validateCallable(
            AbiSchema.ReturnValue result,
            List<AbiSchema.Parameter> parameters,
            String path,
            Map<String, AbiSchema.TypeDefinition> types,
            Map<String, AbiSchema.CallbackDefinition> callbacks,
            List<Issue> issues
    ) {
        boolean resultResolved = requireResolved(result.type(), path + ".result.type", types, callbacks, issues);
        if (resultResolved) {
            validateQualifiers(
                    result.type(),
                    result.nullability(),
                    result.ownership(),
                    path + ".result",
                    types,
                    callbacks,
                    issues
            );
            if (isVoid(result.type(), types)) {
                if (result.nullability() != AbiSchema.Nullability.UNSPECIFIED
                        || result.ownership() != AbiSchema.Ownership.NONE) {
                    issue(issues, path + ".result", "VOID must use UNSPECIFIED nullability and NONE ownership");
                }
            }
        }

        Set<String> names = new HashSet<>();
        for (int index = 0; index < parameters.size(); index++) {
            AbiSchema.Parameter parameter = parameters.get(index);
            String parameterPath = path + ".parameters[" + index + "]";
            validateIdentifier(parameter.name(), parameterPath + ".name", issues);
            if (!names.add(parameter.name())) {
                issue(issues, parameterPath + ".name", "duplicates parameter '" + parameter.name() + "'");
            }
            if (!requireResolved(parameter.type(), parameterPath + ".type", types, callbacks, issues)) {
                continue;
            }
            if (isVoid(parameter.type(), types)) {
                issue(issues, parameterPath + ".type", "VOID is not a valid parameter type");
            }
            boolean addressLike = isAddressLike(parameter.type(), types, callbacks);
            if (parameter.direction() != AbiSchema.ParameterDirection.IN && !addressLike) {
                issue(issues, parameterPath + ".direction", "OUT and IN_OUT require an address-like type");
            }
            validateQualifiers(
                    parameter.type(),
                    parameter.nullability(),
                    parameter.ownership(),
                    parameterPath,
                    types,
                    callbacks,
                    issues
            );
        }
    }

    /// Validates nullability and ownership against a resolved ABI type.
    ///
    /// @param type the referenced type
    /// @param nullability the declared nullability
    /// @param ownership the declared ownership
    /// @param path the value path
    /// @param types the type registry
    /// @param callbacks the callback registry
    /// @param issues the mutable issue sink
    private static void validateQualifiers(
            AbiSchema.TypeRef type,
            AbiSchema.Nullability nullability,
            AbiSchema.Ownership ownership,
            String path,
            Map<String, AbiSchema.TypeDefinition> types,
            Map<String, AbiSchema.CallbackDefinition> callbacks,
            List<Issue> issues
    ) {
        boolean addressLike = isAddressLike(type, types, callbacks);
        if (!addressLike && nullability != AbiSchema.Nullability.UNSPECIFIED) {
            issue(issues, path + ".nullability", "must be UNSPECIFIED for a non-address type");
        }
        if (!addressLike && ownership != AbiSchema.Ownership.NONE) {
            issue(issues, path + ".ownership", "must be NONE for a non-address type");
        }
    }

    /// Validates one availability object.
    ///
    /// @param availability the metadata to validate
    /// @param path the metadata path
    /// @param issues the mutable issue sink
    private static void validateAvailability(
            AbiSchema.Availability availability,
            String path,
            List<Issue> issues
    ) {
        validatePlatformVersion(availability.introduced(), path + ".introduced", issues);
        validatePlatformVersion(availability.deprecated(), path + ".deprecated", issues);
    }

    /// Validates one nullable dotted platform version.
    ///
    /// @param version the candidate version
    /// @param path the metadata path
    /// @param issues the mutable issue sink
    private static void validatePlatformVersion(
            @Nullable String version,
            String path,
            List<Issue> issues
    ) {
        if (version != null && !PLATFORM_VERSION.matcher(version).matches()) {
            issue(issues, path, "must be a dotted numeric version");
        }
    }

    /// Requires a reference to resolve to a type or callback declaration.
    ///
    /// @param reference the type reference
    /// @param path the reference path
    /// @param types the type registry
    /// @param callbacks the callback registry
    /// @param issues the mutable issue sink
    /// @return whether the reference resolved
    private static boolean requireResolved(
            AbiSchema.TypeRef reference,
            String path,
            Map<String, AbiSchema.TypeDefinition> types,
            Map<String, AbiSchema.CallbackDefinition> callbacks,
            List<Issue> issues
    ) {
        if (types.containsKey(reference.name()) || callbacks.containsKey(reference.name())) {
            return true;
        }
        issue(issues, path, "does not resolve declaration '" + reference.name() + "'");
        return false;
    }

    /// Resolves the storage size and alignment of a named declaration.
    ///
    /// @param name the declaration name
    /// @param target the ABI target
    /// @param types the type registry
    /// @param callbacks the callback registry
    /// @param visiting declarations on the current direct-layout path
    /// @return the storage layout, or `null` for a cycle or unresolved declaration
    private static @Nullable Layout layoutOf(
            String name,
            AbiSchema.Target target,
            Map<String, AbiSchema.TypeDefinition> types,
            Map<String, AbiSchema.CallbackDefinition> callbacks,
            Set<String> visiting
    ) {
        if (callbacks.containsKey(name)) {
            return new Layout(target.addressSize(), target.addressAlignment());
        }
        @Nullable AbiSchema.TypeDefinition type = types.get(name);
        if (type == null || !visiting.add(name)) {
            return null;
        }
        try {
            return switch (type) {
                case AbiSchema.PrimitiveType primitive -> primitive.kind() == AbiSchema.PrimitiveKind.VOID
                        ? null
                        : new Layout(primitive.byteSize(), primitive.alignment());
                case AbiSchema.PointerType ignored -> new Layout(target.addressSize(), target.addressAlignment());
                case AbiSchema.HandleType handle -> layoutOf(
                        handle.representation().name(),
                        target,
                        types,
                        callbacks,
                        visiting
                );
                case AbiSchema.AggregateType aggregate -> new Layout(
                        aggregate.byteSize(),
                        aggregate.alignment()
                );
                case AbiSchema.IntegerSetType integerSet -> layoutOf(
                        integerSet.representation().name(),
                        target,
                        types,
                        callbacks,
                        visiting
                );
            };
        } finally {
            visiting.remove(name);
        }
    }

    /// Returns whether a type has address semantics for qualifiers and output parameters.
    ///
    /// @param reference the resolved type reference
    /// @param types the type registry
    /// @param callbacks the callback registry
    /// @return whether the type is a pointer, handle, or callback
    private static boolean isAddressLike(
            AbiSchema.TypeRef reference,
            Map<String, AbiSchema.TypeDefinition> types,
            Map<String, AbiSchema.CallbackDefinition> callbacks
    ) {
        @Nullable AbiSchema.TypeDefinition type = types.get(reference.name());
        return type instanceof AbiSchema.PointerType
                || type instanceof AbiSchema.HandleType
                || callbacks.containsKey(reference.name());
    }

    /// Returns whether a type reference denotes a void primitive.
    ///
    /// @param reference the resolved type reference
    /// @param types the type registry
    /// @return whether the type is void
    private static boolean isVoid(
            AbiSchema.TypeRef reference,
            Map<String, AbiSchema.TypeDefinition> types
    ) {
        return types.get(reference.name()) instanceof AbiSchema.PrimitiveType primitive
                && primitive.kind() == AbiSchema.PrimitiveKind.VOID;
    }

    /// Validates a declaration identifier.
    ///
    /// @param value the candidate identifier
    /// @param path the value path
    /// @param issues the mutable issue sink
    private static void validateIdentifier(String value, String path, List<Issue> issues) {
        if (!IDENTIFIER.matcher(value).matches()) {
            issue(issues, path, "must be a portable C-style identifier");
        }
    }

    /// Returns whether an integer is a positive power of two.
    ///
    /// @param value the candidate value
    /// @return whether the value is a positive power of two
    private static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    /// Adds one validation issue.
    ///
    /// @param issues the mutable issue sink
    /// @param path the failing value path
    /// @param message the invariant violation
    private static void issue(List<Issue> issues, String path, String message) {
        issues.add(new Issue(path, message));
    }

    /// Describes one deterministic schema validation failure.
    ///
    /// @param path the canonical document path
    /// @param message the violated invariant
    @NotNullByDefault
    public record Issue(String path, String message) {
        /// Creates a validation issue.
        ///
        /// @throws IllegalArgumentException if `path` or `message` is blank
        public Issue {
            if (path.isBlank()) {
                throw new IllegalArgumentException("path must not be blank");
            }
            if (message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
        }

        /// Returns the canonical `path: message` representation.
        ///
        /// @return the formatted issue
        @Override
        public String toString() {
            return path + ": " + message;
        }
    }

    /// Formats a complete validation failure message.
    ///
    /// @param issues the validation failures
    /// @return the multiline message
    private static String formatIssues(List<Issue> issues) {
        return issues.stream()
                .map(Issue::toString)
                .collect(Collectors.joining("\n", "Invalid ABI schema:\n", ""));
    }

    /// Captures a resolved storage size and alignment.
    ///
    /// @param byteSize the storage size in bytes
    /// @param alignment the storage alignment in bytes
    @NotNullByDefault
    private record Layout(long byteSize, int alignment) {
    }

    /// Captures one occupied byte interval in a structure.
    ///
    /// @param start the inclusive byte offset
    /// @param end the exclusive byte offset
    /// @param path the owning field path
    @NotNullByDefault
    private record Interval(long start, long end, String path) {
    }

    /// Captures the storage and occupied bit intervals of one bitfield.
    ///
    /// @param storageStart the inclusive byte offset of the storage unit
    /// @param storageEnd the exclusive byte offset of the storage unit
    /// @param bitStart the inclusive absolute bit offset
    /// @param bitEnd the exclusive absolute bit offset
    /// @param path the owning field path
    @NotNullByDefault
    private record BitInterval(
            long storageStart,
            long storageEnd,
            long bitStart,
            long bitEnd,
            String path
    ) {
    }
}
