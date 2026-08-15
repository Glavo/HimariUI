package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Describes one deterministic native ABI probe document.
///
/// @param protocolVersion the probe protocol version
/// @param fixtures the conformance fixture identifiers covered by the probe
/// @param target the exact target compiled and executed by the probe
/// @param compiler the C compiler identity recorded at compile time
/// @param types the measured scalar, pointer, handle, and integer-set layouts
/// @param aggregates the measured structure and union layouts
/// @param callbacks the measured function-pointer ABI checks
/// @param checks the fixed functional ABI checks
@NotNullByDefault
public record AbiProbe(
        int protocolVersion,
        @Unmodifiable List<String> fixtures,
        Target target,
        Compiler compiler,
        @Unmodifiable List<TypeLayout> types,
        @Unmodifiable List<AggregateLayout> aggregates,
        @Unmodifiable List<CallbackLayout> callbacks,
        Checks checks
) {
    /// Creates an immutable probe document.
    public AbiProbe {
        fixtures = copy(fixtures, "fixtures");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(compiler, "compiler");
        types = copy(types, "types");
        aggregates = copy(aggregates, "aggregates");
        callbacks = copy(callbacks, "callbacks");
        Objects.requireNonNull(checks, "checks");
    }

    /// Copies a list and rejects null elements.
    ///
    /// @param source the source list
    /// @param name the parameter name used in diagnostics
    /// @param <T> the element type
    /// @return the immutable copy
    private static <T> @Unmodifiable List<T> copy(List<T> source, String name) {
        Objects.requireNonNull(source, name);
        return source.stream()
                .map(value -> Objects.requireNonNull(value, name + " element"))
                .toList();
    }

    /// Identifies the exact operating system, architecture, and data model measured by a probe.
    ///
    /// @param operatingSystem the normalized operating-system identifier
    /// @param architecture the normalized processor-architecture identifier
    /// @param byteOrder the native byte order
    /// @param addressSize the native address size in bytes
    /// @param addressAlignment the native address alignment in bytes
    @NotNullByDefault
    public record Target(
            String operatingSystem,
            String architecture,
            AbiSchema.ByteOrder byteOrder,
            long addressSize,
            long addressAlignment
    ) {
        /// Creates a target measurement.
        public Target {
            Objects.requireNonNull(operatingSystem, "operatingSystem");
            Objects.requireNonNull(architecture, "architecture");
            Objects.requireNonNull(byteOrder, "byteOrder");
            requirePositive(addressSize, "addressSize");
            requirePositive(addressAlignment, "addressAlignment");
        }
    }

    /// Identifies the C compiler that produced the probe executable.
    ///
    /// @param family the normalized compiler family
    /// @param major the compiler major version
    /// @param minor the compiler minor version
    /// @param patch the compiler patch version
    @NotNullByDefault
    public record Compiler(String family, int major, int minor, int patch) {
        /// Creates a compiler identity.
        public Compiler {
            Objects.requireNonNull(family, "family");
            requireNonNegative(major, "major");
            requireNonNegative(minor, "minor");
            requireNonNegative(patch, "patch");
        }
    }

    /// Describes one measured non-aggregate type layout.
    ///
    /// @param name the canonical ABI type name
    /// @param byteSize the measured storage size in bytes
    /// @param alignment the measured alignment in bytes
    @NotNullByDefault
    public record TypeLayout(String name, long byteSize, long alignment) {
        /// Creates a type-layout measurement.
        public TypeLayout {
            Objects.requireNonNull(name, "name");
            requirePositive(byteSize, "byteSize");
            requirePositive(alignment, "alignment");
        }
    }

    /// Describes one measured structure or union layout.
    ///
    /// @param name the canonical ABI aggregate name
    /// @param byteSize the measured complete size in bytes
    /// @param alignment the measured aggregate alignment in bytes
    /// @param fields the measured field layouts in declaration order
    @NotNullByDefault
    public record AggregateLayout(
            String name,
            long byteSize,
            long alignment,
            @Unmodifiable List<FieldLayout> fields
    ) {
        /// Creates an aggregate-layout measurement.
        public AggregateLayout {
            Objects.requireNonNull(name, "name");
            requirePositive(byteSize, "byteSize");
            requirePositive(alignment, "alignment");
            fields = copy(fields, "fields");
        }
    }

    /// Describes one measured aggregate field or bitfield.
    ///
    /// @param name the field name
    /// @param byteOffset the byte offset of the field storage
    /// @param bitOffset the least-significant bit offset within storage, or `null` for a normal field
    /// @param bitWidth the bit width, or `null` for a normal field
    @NotNullByDefault
    public record FieldLayout(
            String name,
            long byteOffset,
            @Nullable Integer bitOffset,
            @Nullable Integer bitWidth
    ) {
        /// Creates a field-layout measurement.
        public FieldLayout {
            Objects.requireNonNull(name, "name");
            requireNonNegative(byteOffset, "byteOffset");
            if ((bitOffset == null) != (bitWidth == null)) {
                throw new IllegalArgumentException("bitOffset and bitWidth must both be null or both be present");
            }
            if (bitOffset != null) {
                requireNonNegative(bitOffset, "bitOffset");
                requirePositive(bitWidth, "bitWidth");
            }
        }
    }

    /// Describes one measured native callback function-pointer contract.
    ///
    /// @param name the canonical callback name
    /// @param callingConvention the compiled calling convention
    /// @param pointerSize the function-pointer size in bytes
    /// @param pointerAlignment the function-pointer alignment in bytes
    /// @param invocationResult the result of the fixed callback round trip
    @NotNullByDefault
    public record CallbackLayout(
            String name,
            AbiSchema.CallingConvention callingConvention,
            long pointerSize,
            long pointerAlignment,
            long invocationResult
    ) {
        /// Creates a callback-layout measurement.
        public CallbackLayout {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(callingConvention, "callingConvention");
            requirePositive(pointerSize, "pointerSize");
            requirePositive(pointerAlignment, "pointerAlignment");
        }
    }

    /// Stores fixed behavior checks that exercise ABI paths not expressible by `sizeof` or `offsetof`.
    ///
    /// @param structureReturnLeft the signed member returned by value from a C function
    /// @param structureReturnRight the unsigned member returned by value from a C function
    /// @param variadicSum the result of the fixed variadic call
    @NotNullByDefault
    public record Checks(int structureReturnLeft, long structureReturnRight, int variadicSum) {
    }

    /// Requires a positive long value.
    ///
    /// @param value the candidate value
    /// @param name the field name used in diagnostics
    private static void requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }

    /// Requires a non-negative long value.
    ///
    /// @param value the candidate value
    /// @param name the field name used in diagnostics
    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative: " + value);
        }
    }
}
