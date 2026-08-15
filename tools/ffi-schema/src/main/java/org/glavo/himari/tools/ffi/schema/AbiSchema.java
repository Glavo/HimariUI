package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/// Describes one target-resolved, versioned ABI namespace consumed by binding generators.
///
/// @param schemaVersion the canonical schema format version
/// @param namespace the Java-style namespace used to isolate generated declarations
/// @param library the logical system-library name
/// @param target the ABI target whose sizes and byte order apply
/// @param types the named primitive, pointer, handle, aggregate, enum, and flags declarations
/// @param callbacks the named native-to-Java callback signatures
/// @param functions the named Java-to-native function signatures
@NotNullByDefault
public record AbiSchema(
        int schemaVersion,
        String namespace,
        String library,
        Target target,
        @Unmodifiable List<TypeDefinition> types,
        @Unmodifiable List<CallbackDefinition> callbacks,
        @Unmodifiable List<FunctionDefinition> functions
) {
    /// Creates a schema with declaration lists copied and sorted by canonical name.
    public AbiSchema {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(library, "library");
        Objects.requireNonNull(target, "target");
        types = types.stream()
                .map(type -> Objects.requireNonNull(type, "types element"))
                .sorted(Comparator.comparing(TypeDefinition::name))
                .toList();
        callbacks = callbacks.stream()
                .map(callback -> Objects.requireNonNull(callback, "callbacks element"))
                .sorted(Comparator.comparing(CallbackDefinition::name))
                .toList();
        functions = functions.stream()
                .map(function -> Objects.requireNonNull(function, "functions element"))
                .sorted(Comparator.comparing(FunctionDefinition::name))
                .toList();
    }

    /// Describes the resolved data model used by one schema document.
    ///
    /// @param operatingSystem the stable operating-system identifier
    /// @param architecture the stable processor-architecture identifier
    /// @param byteOrder the byte order used by scalar and aggregate layouts
    /// @param addressSize the native address size in bytes
    /// @param addressAlignment the native address alignment in bytes
    @NotNullByDefault
    public record Target(
            String operatingSystem,
            String architecture,
            ByteOrder byteOrder,
            int addressSize,
            int addressAlignment
    ) {
        /// Creates a target descriptor.
        public Target {
            Objects.requireNonNull(operatingSystem, "operatingSystem");
            Objects.requireNonNull(architecture, "architecture");
            Objects.requireNonNull(byteOrder, "byteOrder");
        }
    }

    /// Defines a named ABI type declaration.
    @NotNullByDefault
    public sealed interface TypeDefinition permits AggregateType, HandleType, IntegerSetType, PointerType, PrimitiveType {
        /// Returns the declaration name used by [TypeRef].
        ///
        /// @return the canonical declaration name
        String name();

        /// Returns the platform-version availability contract.
        ///
        /// @return the availability metadata
        Availability availability();
    }

    /// Defines a fixed-width scalar or void type.
    ///
    /// @param name the canonical declaration name
    /// @param kind the scalar representation category
    /// @param byteSize the storage size in bytes, or zero only for `VOID`
    /// @param alignment the natural alignment in bytes
    /// @param signedness the integer signedness or `NONE` for non-integers
    /// @param availability the platform-version availability contract
    @NotNullByDefault
    public record PrimitiveType(
            String name,
            PrimitiveKind kind,
            int byteSize,
            int alignment,
            Signedness signedness,
            Availability availability
    ) implements TypeDefinition {
        /// Creates a primitive declaration.
        public PrimitiveType {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(signedness, "signedness");
            Objects.requireNonNull(availability, "availability");
        }
    }

    /// Defines a native address whose pointee type remains explicit.
    ///
    /// @param name the canonical declaration name
    /// @param pointee the referenced pointee declaration
    /// @param constant whether writes through the address are forbidden by the native contract
    /// @param availability the platform-version availability contract
    @NotNullByDefault
    public record PointerType(
            String name,
            TypeRef pointee,
            boolean constant,
            Availability availability
    ) implements TypeDefinition {
        /// Creates a pointer declaration.
        public PointerType {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(pointee, "pointee");
            Objects.requireNonNull(availability, "availability");
        }
    }

    /// Defines an opaque native handle with a known ABI representation.
    ///
    /// @param name the canonical declaration name
    /// @param representation the primitive or pointer representation
    /// @param invalidValues the immutable numeric sentinel values that are never valid handles
    /// @param availability the platform-version availability contract
    @NotNullByDefault
    public record HandleType(
            String name,
            TypeRef representation,
            @Unmodifiable List<Long> invalidValues,
            Availability availability
    ) implements TypeDefinition {
        /// Creates a handle declaration with sorted immutable invalid values.
        public HandleType {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(representation, "representation");
            invalidValues = invalidValues.stream()
                    .map(value -> Objects.requireNonNull(value, "invalidValues element"))
                    .sorted()
                    .toList();
            Objects.requireNonNull(availability, "availability");
        }
    }

    /// Defines a structure or union with target-resolved size, alignment, packing, and fields.
    ///
    /// @param name the canonical declaration name
    /// @param kind whether fields use structure or union placement
    /// @param byteSize the complete aggregate size in bytes
    /// @param alignment the aggregate alignment in bytes
    /// @param packing the maximum field alignment in bytes, or zero for natural alignment
    /// @param fields the fields in declaration order
    /// @param availability the platform-version availability contract
    @NotNullByDefault
    public record AggregateType(
            String name,
            AggregateKind kind,
            long byteSize,
            int alignment,
            int packing,
            @Unmodifiable List<AggregateField> fields,
            Availability availability
    ) implements TypeDefinition {
        /// Creates an aggregate declaration with an immutable field list.
        public AggregateType {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(kind, "kind");
            fields = fields.stream()
                    .map(field -> Objects.requireNonNull(field, "fields element"))
                    .toList();
            Objects.requireNonNull(availability, "availability");
        }
    }

    /// Defines one field or bitfield in an aggregate.
    ///
    /// @param name the field name
    /// @param type the field storage type
    /// @param byteOffset the field storage offset in bytes
    /// @param bitOffset the least-significant bit offset within storage, or `null` for a normal field
    /// @param bitWidth the bit width, or `null` for a normal field
    @NotNullByDefault
    public record AggregateField(
            String name,
            TypeRef type,
            long byteOffset,
            @Nullable Integer bitOffset,
            @Nullable Integer bitWidth
    ) {
        /// Creates an aggregate field declaration.
        public AggregateField {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
        }
    }

    /// Defines an enum or flags declaration with an integer representation.
    ///
    /// @param name the canonical declaration name
    /// @param kind whether values are exclusive enum constants or composable flags
    /// @param representation the integer primitive representation
    /// @param values the named numeric values
    /// @param availability the platform-version availability contract
    @NotNullByDefault
    public record IntegerSetType(
            String name,
            IntegerSetKind kind,
            TypeRef representation,
            @Unmodifiable List<IntegerValue> values,
            Availability availability
    ) implements TypeDefinition {
        /// Creates an integer-set declaration with values sorted by name.
        public IntegerSetType {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(representation, "representation");
            values = values.stream()
                    .map(value -> Objects.requireNonNull(value, "values element"))
                    .sorted(Comparator.comparing(IntegerValue::name))
                    .toList();
            Objects.requireNonNull(availability, "availability");
        }
    }

    /// Defines one enum constant or named flag bit pattern.
    ///
    /// @param name the canonical value name
    /// @param value the signed 64-bit numeric representation
    @NotNullByDefault
    public record IntegerValue(String name, long value) {
        /// Creates an integer-set value.
        public IntegerValue {
            Objects.requireNonNull(name, "name");
        }
    }

    /// Defines a native-to-Java callback function-pointer signature.
    ///
    /// @param name the canonical callback type name
    /// @param result the callback return contract
    /// @param parameters the ordered callback parameters
    /// @param callingConvention the native calling convention
    /// @param threadRestriction the permitted callback execution context
    /// @param lifetime the required upcall-stub lifetime
    /// @param exceptionPolicy the required containment behavior for Java failures
    /// @param availability the platform-version availability contract
    @NotNullByDefault
    public record CallbackDefinition(
            String name,
            ReturnValue result,
            @Unmodifiable List<Parameter> parameters,
            CallingConvention callingConvention,
            ThreadRestriction threadRestriction,
            CallbackLifetime lifetime,
            CallbackExceptionPolicy exceptionPolicy,
            Availability availability
    ) {
        /// Creates a callback declaration with an immutable parameter list.
        public CallbackDefinition {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(result, "result");
            parameters = parameters.stream()
                    .map(parameter -> Objects.requireNonNull(parameter, "parameters element"))
                    .toList();
            Objects.requireNonNull(callingConvention, "callingConvention");
            Objects.requireNonNull(threadRestriction, "threadRestriction");
            Objects.requireNonNull(lifetime, "lifetime");
            Objects.requireNonNull(exceptionPolicy, "exceptionPolicy");
            Objects.requireNonNull(availability, "availability");
        }
    }

    /// Defines a Java-to-native function signature and symbol policy.
    ///
    /// @param name the canonical Java-facing function name
    /// @param symbol the exact native symbol name
    /// @param result the function return contract
    /// @param parameters the ordered fixed parameters
    /// @param callingConvention the native calling convention
    /// @param variadicFrom the fixed-parameter count before an ellipsis, or `null` for a fixed signature
    /// @param errorPolicy the immediately captured native error state
    /// @param threadRestriction the permitted call context
    /// @param availability the platform-version availability contract
    @NotNullByDefault
    public record FunctionDefinition(
            String name,
            String symbol,
            ReturnValue result,
            @Unmodifiable List<Parameter> parameters,
            CallingConvention callingConvention,
            @Nullable Integer variadicFrom,
            ErrorPolicy errorPolicy,
            ThreadRestriction threadRestriction,
            Availability availability
    ) {
        /// Creates a function declaration with an immutable parameter list.
        public FunctionDefinition {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(symbol, "symbol");
            Objects.requireNonNull(result, "result");
            parameters = parameters.stream()
                    .map(parameter -> Objects.requireNonNull(parameter, "parameters element"))
                    .toList();
            Objects.requireNonNull(callingConvention, "callingConvention");
            Objects.requireNonNull(errorPolicy, "errorPolicy");
            Objects.requireNonNull(threadRestriction, "threadRestriction");
            Objects.requireNonNull(availability, "availability");
        }
    }

    /// Defines one callable parameter and its semantic qualifiers.
    ///
    /// @param name the stable parameter name
    /// @param type the ABI type reference
    /// @param direction the native data-flow direction
    /// @param nullability the null-address contract
    /// @param ownership the resource ownership transition
    @NotNullByDefault
    public record Parameter(
            String name,
            TypeRef type,
            ParameterDirection direction,
            Nullability nullability,
            Ownership ownership
    ) {
        /// Creates a callable parameter.
        public Parameter {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(direction, "direction");
            Objects.requireNonNull(nullability, "nullability");
            Objects.requireNonNull(ownership, "ownership");
        }
    }

    /// Defines a callable return type and its semantic qualifiers.
    ///
    /// @param type the ABI type reference
    /// @param nullability the null-address contract
    /// @param ownership the returned resource ownership
    @NotNullByDefault
    public record ReturnValue(TypeRef type, Nullability nullability, Ownership ownership) {
        /// Creates a callable return contract.
        public ReturnValue {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(nullability, "nullability");
            Objects.requireNonNull(ownership, "ownership");
        }
    }

    /// Refers to a named type or callback declaration in the same schema.
    ///
    /// @param name the canonical declaration name
    @NotNullByDefault
    public record TypeRef(String name) {
        /// Creates a type reference.
        public TypeRef {
            Objects.requireNonNull(name, "name");
        }
    }

    /// Defines introduction, deprecation, and unavailability metadata for a declaration.
    ///
    /// @param introduced the first supported platform version, or `null` when unspecified
    /// @param deprecated the first deprecated platform version, or `null` when not deprecated
    /// @param unavailable whether the declaration is unavailable for the resolved target
    @NotNullByDefault
    public record Availability(
            @Nullable String introduced,
            @Nullable String deprecated,
            boolean unavailable
    ) {
        /// Availability metadata with no version restrictions.
        public static final Availability UNRESTRICTED = new Availability(null, null, false);
    }

    /// Selects a concrete byte order.
    @NotNullByDefault
    public enum ByteOrder {
        /// Least-significant byte first.
        LITTLE_ENDIAN,

        /// Most-significant byte first.
        BIG_ENDIAN
    }

    /// Selects the primitive representation category.
    @NotNullByDefault
    public enum PrimitiveKind {
        /// No storage and valid only as a callable return type or pointer pointee.
        VOID,

        /// A fixed-width integer representation.
        INTEGER,

        /// An IEEE-style floating-point representation.
        FLOATING
    }

    /// Selects integer signedness.
    @NotNullByDefault
    public enum Signedness {
        /// Signedness does not apply to the declaration.
        NONE,

        /// Signed two's-complement integer interpretation.
        SIGNED,

        /// Unsigned integer interpretation.
        UNSIGNED
    }

    /// Selects aggregate field placement.
    @NotNullByDefault
    public enum AggregateKind {
        /// Fields occupy their declared non-overlapping offsets.
        STRUCT,

        /// Fields overlap at byte offset zero.
        UNION
    }

    /// Selects integer-set semantics.
    @NotNullByDefault
    public enum IntegerSetKind {
        /// Values represent exclusive named constants.
        ENUM,

        /// Values represent composable bit patterns.
        FLAGS
    }

    /// Selects a native calling convention.
    @NotNullByDefault
    public enum CallingConvention {
        /// The target platform's standard C ABI convention.
        SYSTEM,

        /// The explicit C declaration convention.
        CDECL,

        /// The 32-bit Windows standard-call convention.
        STDCALL,

        /// The 32-bit Windows instance-method convention.
        THISCALL
    }

    /// Selects immediate native error-state capture.
    @NotNullByDefault
    public enum ErrorPolicy {
        /// No ambient native error state is part of the call contract.
        NONE,

        /// Capture `errno` before another native call can overwrite it.
        ERRNO,

        /// Capture Win32 `GetLastError` state before another native call can overwrite it.
        GET_LAST_ERROR
    }

    /// Selects the execution context permitted for a callable.
    @NotNullByDefault
    public enum ThreadRestriction {
        /// Any attached native or Java thread may execute the callable.
        ANY,

        /// Execution must remain on the initiating caller context.
        CALLER,

        /// Execution must occur on the platform UI context.
        UI,

        /// Execution must occur on the render context.
        RENDER
    }

    /// Selects native data flow for a parameter.
    @NotNullByDefault
    public enum ParameterDirection {
        /// Native code consumes the incoming value or pointed-to data.
        IN,

        /// Native code initializes pointed-to data for the caller.
        OUT,

        /// Native code consumes and may replace pointed-to data.
        IN_OUT
    }

    /// Selects the null-address contract for a callable value.
    @NotNullByDefault
    public enum Nullability {
        /// Nullability does not apply or is not specified by the source contract.
        UNSPECIFIED,

        /// A zero native address is forbidden.
        NON_NULL,

        /// A zero native address is permitted and represents absence.
        NULLABLE
    }

    /// Selects native resource ownership semantics.
    @NotNullByDefault
    public enum Ownership {
        /// No resource ownership applies to the value.
        NONE,

        /// The callee may use the value only for the documented call or scope.
        BORROWED,

        /// The caller owns the returned or incoming resource and must release it.
        OWNED,

        /// Ownership moves from the caller to the callee.
        TRANSFERRED,

        /// Native code retains an independently releasable reference.
        RETAINED
    }

    /// Selects the lifetime required for an upcall stub.
    @NotNullByDefault
    public enum CallbackLifetime {
        /// The callback is valid only for one lexical native call scope.
        SCOPED,

        /// Native code may retain the callback until an explicit release event.
        RETAINED,

        /// Framework code explicitly owns and closes the callback lifetime.
        MANUAL
    }

    /// Selects Java failure handling at a native callback boundary.
    @NotNullByDefault
    public enum CallbackExceptionPolicy {
        /// Catch every Java failure and route it to the generated containment path.
        CONTAIN
    }
}
