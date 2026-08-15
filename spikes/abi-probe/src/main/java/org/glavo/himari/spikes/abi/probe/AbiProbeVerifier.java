package org.glavo.himari.spikes.abi.probe;

import org.glavo.himari.spikes.abi.generated.AbiProbeLayouts;
import org.glavo.himari.tools.ffi.schema.AbiProbe;
import org.glavo.himari.tools.ffi.schema.AbiSchema;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/// Compares native probe measurements with the canonical schema and generated Java layouts.
@NotNullByDefault
final class AbiProbeVerifier {
    /// The exact protocol fixtures owned by ABI-PROBE-001.
    private static final List<String> FIXTURES = List.of(
            "abi-minimum-layouts-v1",
            "abi-callback-conventions-v1"
    );

    /// Prevents instantiation of this utility class.
    private AbiProbeVerifier() {
    }

    /// Compares every probe measurement and fixed behavior check.
    ///
    /// @param schema the canonical target-resolved layout schema
    /// @param probe the native probe document
    /// @param expectedHost the Java process target that must match the compiled probe
    /// @return the complete deterministic comparison
    static Comparison compare(AbiSchema schema, AbiProbe probe, AbiProbe.Target expectedHost) {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(probe, "probe");
        Objects.requireNonNull(expectedHost, "expectedHost");
        List<String> mismatches = new ArrayList<>();

        requireEqual(mismatches, "protocolVersion", 1, probe.protocolVersion());
        requireEqual(mismatches, "fixtures", FIXTURES, probe.fixtures());
        compareTarget(schema.target(), probe.target(), expectedHost, mismatches);
        if (probe.compiler().family().equals("unknown")) {
            mismatches.add("compiler.family: an identified compiler family is required");
        }

        Map<String, AbiSchema.TypeDefinition> schemaTypes = new LinkedHashMap<>();
        for (AbiSchema.TypeDefinition type : schema.types()) {
            schemaTypes.put(type.name(), type);
        }
        int typeCount = compareTypes(schema, schemaTypes, probe.types(), mismatches);
        AggregateCounts aggregateCounts = compareAggregates(schema, probe.aggregates(), mismatches);
        int callbackCount = compareCallbacks(schema, probe.callbacks(), mismatches);
        compareChecks(probe.checks(), mismatches);

        return new Comparison(
                typeCount,
                aggregateCounts.aggregates(),
                aggregateCounts.fields(),
                callbackCount,
                3,
                mismatches
        );
    }

    /// Detects the exact host ABI properties observable by the Java process.
    ///
    /// @return the normalized host target
    /// @throws UnsupportedOperationException if the host is outside the required desktop target set
    static AbiProbe.Target hostTarget() {
        String operatingSystemName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String operatingSystem;
        if (operatingSystemName.contains("windows")) {
            operatingSystem = "windows";
        } else if (operatingSystemName.contains("linux")) {
            operatingSystem = "linux";
        } else if (operatingSystemName.contains("mac") || operatingSystemName.contains("darwin")) {
            operatingSystem = "macos";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + operatingSystemName);
        }

        String architectureName = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        String architecture = switch (architectureName) {
            case "amd64", "x86_64" -> "x86_64";
            case "aarch64", "arm64" -> "arm64";
            default -> throw new UnsupportedOperationException("Unsupported architecture: " + architectureName);
        };
        AbiSchema.ByteOrder byteOrder = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
                ? AbiSchema.ByteOrder.LITTLE_ENDIAN
                : AbiSchema.ByteOrder.BIG_ENDIAN;
        return new AbiProbe.Target(
                operatingSystem,
                architecture,
                byteOrder,
                ValueLayout.ADDRESS.byteSize(),
                ValueLayout.ADDRESS.byteAlignment()
        );
    }

    /// Compares schema, native-probe, and Java-process target descriptions.
    ///
    /// @param schemaTarget the canonical schema target
    /// @param probeTarget the compiled probe target
    /// @param expectedHost the executing Java target
    /// @param mismatches the mutable mismatch sink
    private static void compareTarget(
            AbiSchema.Target schemaTarget,
            AbiProbe.Target probeTarget,
            AbiProbe.Target expectedHost,
            List<String> mismatches
    ) {
        requireEqual(mismatches, "target.operatingSystem.host", expectedHost.operatingSystem(), probeTarget.operatingSystem());
        requireEqual(mismatches, "target.architecture.host", expectedHost.architecture(), probeTarget.architecture());
        requireEqual(mismatches, "target.byteOrder.host", expectedHost.byteOrder(), probeTarget.byteOrder());
        requireEqual(mismatches, "target.addressSize.host", expectedHost.addressSize(), probeTarget.addressSize());
        requireEqual(
                mismatches,
                "target.addressAlignment.host",
                expectedHost.addressAlignment(),
                probeTarget.addressAlignment()
        );

        boolean operatingSystemMatches = schemaTarget.operatingSystem().equals("portable-desktop")
                || schemaTarget.operatingSystem().equals(probeTarget.operatingSystem());
        if (!operatingSystemMatches) {
            mismatches.add("target.operatingSystem.schema: expected " + schemaTarget.operatingSystem()
                    + ", got " + probeTarget.operatingSystem());
        }
        boolean architectureMatches = schemaTarget.architecture().equals("generic64")
                ? probeTarget.architecture().equals("x86_64") || probeTarget.architecture().equals("arm64")
                : schemaTarget.architecture().equals(probeTarget.architecture());
        if (!architectureMatches) {
            mismatches.add("target.architecture.schema: expected " + schemaTarget.architecture()
                    + ", got " + probeTarget.architecture());
        }
        requireEqual(mismatches, "target.byteOrder.schema", schemaTarget.byteOrder(), probeTarget.byteOrder());
        requireEqual(mismatches, "target.addressSize.schema", schemaTarget.addressSize(), probeTarget.addressSize());
        requireEqual(
                mismatches,
                "target.addressAlignment.schema",
                schemaTarget.addressAlignment(),
                probeTarget.addressAlignment()
        );
    }

    /// Compares every non-aggregate schema type with native and generated Java layouts.
    ///
    /// @param schema the canonical schema
    /// @param schemaTypes the schema declarations by name
    /// @param measuredTypes the native type measurements
    /// @param mismatches the mutable mismatch sink
    /// @return the expected type count
    private static int compareTypes(
            AbiSchema schema,
            Map<String, AbiSchema.TypeDefinition> schemaTypes,
            List<AbiProbe.TypeLayout> measuredTypes,
            List<String> mismatches
    ) {
        Map<String, AbiProbe.TypeLayout> measured = indexTypes(measuredTypes);
        int expectedCount = 0;
        for (AbiSchema.TypeDefinition type : schema.types()) {
            if (type instanceof AbiSchema.AggregateType
                    || type instanceof AbiSchema.PrimitiveType primitive
                    && primitive.kind() == AbiSchema.PrimitiveKind.VOID) {
                continue;
            }
            expectedCount++;
            String path = "types." + type.name();
            @Nullable AbiProbe.TypeLayout actual = measured.get(type.name());
            if (actual == null) {
                mismatches.add(path + ": missing native measurement");
                continue;
            }
            ExpectedLayout expected = expectedLayout(schema, schemaTypes, type);
            compareLayout(path + ".schema", expected.byteSize(), expected.alignment(), actual, mismatches);
            @Nullable MemoryLayout generated = generatedLayout(type.name());
            if (generated == null) {
                mismatches.add(path + ": missing generated Java layout mapping");
            } else {
                compareLayout(path + ".java", generated.byteSize(), generated.byteAlignment(), actual, mismatches);
            }
        }
        for (String measuredName : measured.keySet()) {
            AbiSchema.TypeDefinition type = schemaTypes.get(measuredName);
            if (type == null || type instanceof AbiSchema.AggregateType
                    || type instanceof AbiSchema.PrimitiveType primitive
                    && primitive.kind() == AbiSchema.PrimitiveKind.VOID) {
                mismatches.add("types." + measuredName + ": unexpected native measurement");
            }
        }
        requireEqual(mismatches, "types.count", expectedCount, measured.size());
        return expectedCount;
    }

    /// Compares every aggregate and field layout.
    ///
    /// @param schema the canonical schema
    /// @param measuredAggregates the native aggregate measurements
    /// @param mismatches the mutable mismatch sink
    /// @return aggregate and field counts
    private static AggregateCounts compareAggregates(
            AbiSchema schema,
            List<AbiProbe.AggregateLayout> measuredAggregates,
            List<String> mismatches
    ) {
        Map<String, AbiProbe.AggregateLayout> measured = indexAggregates(measuredAggregates);
        int aggregateCount = 0;
        int fieldCount = 0;
        for (AbiSchema.TypeDefinition type : schema.types()) {
            if (!(type instanceof AbiSchema.AggregateType aggregate)) {
                continue;
            }
            aggregateCount++;
            fieldCount += aggregate.fields().size();
            String path = "aggregates." + aggregate.name();
            @Nullable AbiProbe.AggregateLayout actual = measured.get(aggregate.name());
            if (actual == null) {
                mismatches.add(path + ": missing native measurement");
                continue;
            }
            requireEqual(mismatches, path + ".byteSize.schema", aggregate.byteSize(), actual.byteSize());
            requireEqual(mismatches, path + ".alignment.schema", aggregate.alignment(), actual.alignment());
            @Nullable MemoryLayout generated = generatedLayout(aggregate.name());
            if (generated == null) {
                mismatches.add(path + ": missing generated Java layout mapping");
            } else {
                requireEqual(mismatches, path + ".byteSize.java", generated.byteSize(), actual.byteSize());
                requireEqual(mismatches, path + ".alignment.java", generated.byteAlignment(), actual.alignment());
            }
            compareFields(aggregate, actual, mismatches);
        }
        for (String measuredName : measured.keySet()) {
            boolean expected = schema.types().stream()
                    .anyMatch(type -> type instanceof AbiSchema.AggregateType && type.name().equals(measuredName));
            if (!expected) {
                mismatches.add("aggregates." + measuredName + ": unexpected native measurement");
            }
        }
        requireEqual(mismatches, "aggregates.count", aggregateCount, measured.size());
        return new AggregateCounts(aggregateCount, fieldCount);
    }

    /// Compares all fields of one aggregate.
    ///
    /// @param aggregate the schema aggregate
    /// @param measured the native aggregate measurement
    /// @param mismatches the mutable mismatch sink
    private static void compareFields(
            AbiSchema.AggregateType aggregate,
            AbiProbe.AggregateLayout measured,
            List<String> mismatches
    ) {
        Map<String, AbiProbe.FieldLayout> fields = indexFields(measured.fields());
        for (AbiSchema.AggregateField field : aggregate.fields()) {
            String path = "aggregates." + aggregate.name() + ".fields." + field.name();
            @Nullable AbiProbe.FieldLayout actual = fields.get(field.name());
            if (actual == null) {
                mismatches.add(path + ": missing native measurement");
                continue;
            }
            requireEqual(mismatches, path + ".byteOffset.schema", field.byteOffset(), actual.byteOffset());
            requireEqual(mismatches, path + ".bitOffset.schema", field.bitOffset(), actual.bitOffset());
            requireEqual(mismatches, path + ".bitWidth.schema", field.bitWidth(), actual.bitWidth());
            @Nullable GeneratedField generated = generatedField(aggregate.name(), field.name());
            if (generated == null) {
                mismatches.add(path + ": missing generated Java field mapping");
            } else {
                requireEqual(mismatches, path + ".byteOffset.java", generated.byteOffset(), actual.byteOffset());
                requireEqual(mismatches, path + ".bitOffset.java", generated.bitOffset(), actual.bitOffset());
                requireEqual(mismatches, path + ".bitWidth.java", generated.bitWidth(), actual.bitWidth());
            }
        }
        for (String measuredName : fields.keySet()) {
            boolean expected = aggregate.fields().stream().anyMatch(field -> field.name().equals(measuredName));
            if (!expected) {
                mismatches.add("aggregates." + aggregate.name() + ".fields." + measuredName
                        + ": unexpected native measurement");
            }
        }
        requireEqual(
                mismatches,
                "aggregates." + aggregate.name() + ".fields.count",
                aggregate.fields().size(),
                fields.size()
        );
    }

    /// Compares every callback pointer layout, convention, and invocation result.
    ///
    /// @param schema the canonical schema
    /// @param measuredCallbacks the native callback measurements
    /// @param mismatches the mutable mismatch sink
    /// @return the expected callback count
    private static int compareCallbacks(
            AbiSchema schema,
            List<AbiProbe.CallbackLayout> measuredCallbacks,
            List<String> mismatches
    ) {
        Map<String, AbiProbe.CallbackLayout> measured = indexCallbacks(measuredCallbacks);
        for (AbiSchema.CallbackDefinition callback : schema.callbacks()) {
            String path = "callbacks." + callback.name();
            @Nullable AbiProbe.CallbackLayout actual = measured.get(callback.name());
            if (actual == null) {
                mismatches.add(path + ": missing native measurement");
                continue;
            }
            requireEqual(
                    mismatches,
                    path + ".callingConvention",
                    callback.callingConvention(),
                    actual.callingConvention()
            );
            @Nullable MemoryLayout generated = generatedCallbackPointer(callback.name());
            if (generated == null) {
                mismatches.add(path + ": missing generated Java callback mapping");
            } else {
                requireEqual(mismatches, path + ".pointerSize", generated.byteSize(), actual.pointerSize());
                requireEqual(
                        mismatches,
                        path + ".pointerAlignment",
                        generated.byteAlignment(),
                        actual.pointerAlignment()
                );
            }
            requireEqual(mismatches, path + ".invocationResult", 142L, actual.invocationResult());
        }
        for (String measuredName : measured.keySet()) {
            boolean expected = schema.callbacks().stream().anyMatch(callback -> callback.name().equals(measuredName));
            if (!expected) {
                mismatches.add("callbacks." + measuredName + ": unexpected native measurement");
            }
        }
        requireEqual(mismatches, "callbacks.count", schema.callbacks().size(), measured.size());
        return schema.callbacks().size();
    }

    /// Compares fixed structure-return and variadic-call results.
    ///
    /// @param checks the native functional checks
    /// @param mismatches the mutable mismatch sink
    private static void compareChecks(AbiProbe.Checks checks, List<String> mismatches) {
        requireEqual(mismatches, "checks.structureReturnLeft", -7, checks.structureReturnLeft());
        requireEqual(mismatches, "checks.structureReturnRight", 42L, checks.structureReturnRight());
        requireEqual(mismatches, "checks.variadicSum", 6, checks.variadicSum());
    }

    /// Compares one native layout against expected size and alignment.
    ///
    /// @param path the mismatch path
    /// @param expectedSize the expected byte size
    /// @param expectedAlignment the expected byte alignment
    /// @param actual the native measurement
    /// @param mismatches the mutable mismatch sink
    private static void compareLayout(
            String path,
            long expectedSize,
            long expectedAlignment,
            AbiProbe.TypeLayout actual,
            List<String> mismatches
    ) {
        requireEqual(mismatches, path + ".byteSize", expectedSize, actual.byteSize());
        requireEqual(mismatches, path + ".alignment", expectedAlignment, actual.alignment());
    }

    /// Resolves an expected schema layout recursively.
    ///
    /// @param schema the canonical schema
    /// @param types the declarations by name
    /// @param type the declaration to resolve
    /// @return the expected size and alignment
    private static ExpectedLayout expectedLayout(
            AbiSchema schema,
            Map<String, AbiSchema.TypeDefinition> types,
            AbiSchema.TypeDefinition type
    ) {
        return switch (type) {
            case AbiSchema.PrimitiveType primitive -> new ExpectedLayout(
                    primitive.byteSize(),
                    primitive.alignment()
            );
            case AbiSchema.PointerType ignored -> new ExpectedLayout(
                    schema.target().addressSize(),
                    schema.target().addressAlignment()
            );
            case AbiSchema.HandleType handle -> expectedLayout(schema, types, requireType(types, handle.representation()));
            case AbiSchema.IntegerSetType integerSet -> expectedLayout(
                    schema,
                    types,
                    requireType(types, integerSet.representation())
            );
            case AbiSchema.AggregateType aggregate -> new ExpectedLayout(
                    aggregate.byteSize(),
                    aggregate.alignment()
            );
        };
    }

    /// Resolves a required schema type reference.
    ///
    /// @param types the declarations by name
    /// @param reference the reference to resolve
    /// @return the referenced declaration
    private static AbiSchema.TypeDefinition requireType(
            Map<String, AbiSchema.TypeDefinition> types,
            AbiSchema.TypeRef reference
    ) {
        @Nullable AbiSchema.TypeDefinition type = types.get(reference.name());
        if (type == null) {
            throw new IllegalArgumentException("Unresolved schema type: " + reference.name());
        }
        return type;
    }

    /// Returns a generated Java layout for one schema type.
    ///
    /// @param name the schema type name
    /// @return the generated layout, or `null` when the fixture mapping is incomplete
    private static @Nullable MemoryLayout generatedLayout(String name) {
        return switch (name) {
            case "u8" -> AbiProbeLayouts.U8;
            case "i32" -> AbiProbeLayouts.I32;
            case "u32" -> AbiProbeLayouts.U32;
            case "f64" -> AbiProbeLayouts.F64;
            case "const_u8_ptr" -> AbiProbeLayouts.CONST_U8_PTR;
            case "void_ptr" -> AbiProbeLayouts.VOID_PTR;
            case "fixture_handle" -> AbiProbeLayouts.FIXTURE_HANDLE;
            case "fixture_flags" -> AbiProbeLayouts.FIXTURE_FLAGS;
            case "fixture_pair" -> AbiProbeLayouts.FIXTURE_PAIR;
            case "fixture_bits" -> AbiProbeLayouts.FIXTURE_BITS;
            case "fixture_value" -> AbiProbeLayouts.FIXTURE_VALUE;
            default -> null;
        };
    }

    /// Returns generated Java field metadata for one aggregate field.
    ///
    /// @param aggregate the aggregate name
    /// @param field the field name
    /// @return generated metadata, or `null` when the fixture mapping is incomplete
    private static @Nullable GeneratedField generatedField(String aggregate, String field) {
        return switch (aggregate + "." + field) {
            case "fixture_pair.left" -> new GeneratedField(AbiProbeLayouts.FIXTURE_PAIR_LEFT_OFFSET, null, null);
            case "fixture_pair.right" -> new GeneratedField(AbiProbeLayouts.FIXTURE_PAIR_RIGHT_OFFSET, null, null);
            case "fixture_bits.mode" -> new GeneratedField(
                    AbiProbeLayouts.FIXTURE_BITS_MODE_OFFSET,
                    AbiProbeLayouts.FIXTURE_BITS_MODE_BIT_OFFSET,
                    AbiProbeLayouts.FIXTURE_BITS_MODE_BIT_WIDTH
            );
            case "fixture_bits.ready" -> new GeneratedField(
                    AbiProbeLayouts.FIXTURE_BITS_READY_OFFSET,
                    AbiProbeLayouts.FIXTURE_BITS_READY_BIT_OFFSET,
                    AbiProbeLayouts.FIXTURE_BITS_READY_BIT_WIDTH
            );
            case "fixture_value.integer" -> new GeneratedField(
                    AbiProbeLayouts.FIXTURE_VALUE_INTEGER_OFFSET,
                    null,
                    null
            );
            case "fixture_value.floating" -> new GeneratedField(
                    AbiProbeLayouts.FIXTURE_VALUE_FLOATING_OFFSET,
                    null,
                    null
            );
            default -> null;
        };
    }

    /// Returns the generated pointer layout for one callback.
    ///
    /// @param name the callback name
    /// @return the generated pointer layout, or `null` when the fixture mapping is incomplete
    private static @Nullable MemoryLayout generatedCallbackPointer(String name) {
        return name.equals("fixture_visit_callback") ? AbiProbeLayouts.FIXTURE_VISIT_CALLBACK_POINTER : null;
    }

    /// Indexes type measurements by canonical name.
    ///
    /// @param layouts the measurements
    /// @return the insertion-ordered index
    private static Map<String, AbiProbe.TypeLayout> indexTypes(List<AbiProbe.TypeLayout> layouts) {
        Map<String, AbiProbe.TypeLayout> result = new LinkedHashMap<>();
        layouts.forEach(layout -> result.put(layout.name(), layout));
        return result;
    }

    /// Indexes aggregate measurements by canonical name.
    ///
    /// @param layouts the measurements
    /// @return the insertion-ordered index
    private static Map<String, AbiProbe.AggregateLayout> indexAggregates(List<AbiProbe.AggregateLayout> layouts) {
        Map<String, AbiProbe.AggregateLayout> result = new LinkedHashMap<>();
        layouts.forEach(layout -> result.put(layout.name(), layout));
        return result;
    }

    /// Indexes field measurements by canonical name.
    ///
    /// @param layouts the measurements
    /// @return the insertion-ordered index
    private static Map<String, AbiProbe.FieldLayout> indexFields(List<AbiProbe.FieldLayout> layouts) {
        Map<String, AbiProbe.FieldLayout> result = new LinkedHashMap<>();
        layouts.forEach(layout -> result.put(layout.name(), layout));
        return result;
    }

    /// Indexes callback measurements by canonical name.
    ///
    /// @param layouts the measurements
    /// @return the insertion-ordered index
    private static Map<String, AbiProbe.CallbackLayout> indexCallbacks(List<AbiProbe.CallbackLayout> layouts) {
        Map<String, AbiProbe.CallbackLayout> result = new LinkedHashMap<>();
        layouts.forEach(layout -> result.put(layout.name(), layout));
        return result;
    }

    /// Records a mismatch when two values differ.
    ///
    /// @param mismatches the mutable mismatch sink
    /// @param path the compared value path
    /// @param expected the expected value
    /// @param actual the actual value
    private static void requireEqual(
            List<String> mismatches,
            String path,
            @Nullable Object expected,
            @Nullable Object actual
    ) {
        if (!Objects.equals(expected, actual)) {
            mismatches.add(path + ": expected " + expected + ", got " + actual);
        }
    }

    /// Records a mismatch when two integral values differ.
    ///
    /// @param mismatches the mutable mismatch sink
    /// @param path the compared value path
    /// @param expected the expected value
    /// @param actual the actual value
    private static void requireEqual(List<String> mismatches, String path, long expected, long actual) {
        if (expected != actual) {
            mismatches.add(path + ": expected " + expected + ", got " + actual);
        }
    }

    /// Describes a complete comparison result.
    ///
    /// @param typeLayouts the verified non-aggregate layout count
    /// @param aggregateLayouts the verified aggregate layout count
    /// @param fieldLayouts the verified field layout count
    /// @param callbacks the verified callback ABI count
    /// @param functionalChecks the fixed behavior-check count
    /// @param mismatches all deterministic mismatch diagnostics
    @NotNullByDefault
    record Comparison(
            int typeLayouts,
            int aggregateLayouts,
            int fieldLayouts,
            int callbacks,
            int functionalChecks,
            @Unmodifiable List<String> mismatches
    ) {
        /// Creates an immutable comparison result.
        Comparison {
            mismatches = List.copyOf(mismatches);
        }

        /// Returns whether every comparison passed.
        ///
        /// @return whether no mismatch was observed
        boolean passed() {
            return mismatches.isEmpty();
        }
    }

    /// Stores expected size and alignment for one schema type.
    ///
    /// @param byteSize the expected byte size
    /// @param alignment the expected byte alignment
    @NotNullByDefault
    private record ExpectedLayout(long byteSize, long alignment) {
    }

    /// Stores generated field position metadata.
    ///
    /// @param byteOffset the generated byte offset
    /// @param bitOffset the generated bit offset, or `null`
    /// @param bitWidth the generated bit width, or `null`
    @NotNullByDefault
    private record GeneratedField(
            long byteOffset,
            @Nullable Integer bitOffset,
            @Nullable Integer bitWidth
    ) {
    }

    /// Stores expected aggregate and field counts.
    ///
    /// @param aggregates the aggregate count
    /// @param fields the aggregate-field count
    @NotNullByDefault
    private record AggregateCounts(int aggregates, int fields) {
    }
}
