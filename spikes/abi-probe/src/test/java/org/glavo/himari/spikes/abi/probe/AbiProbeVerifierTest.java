package org.glavo.himari.spikes.abi.probe;

import org.glavo.himari.tools.ffi.schema.AbiProbe;
import org.glavo.himari.tools.ffi.schema.AbiSchema;
import org.glavo.himari.tools.ffi.schema.AbiSchemaCodec;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies exact schema/native/generated-layout comparison and negative diagnostics.
@NotNullByDefault
final class AbiProbeVerifierTest {
    /// Verifies that a complete matching measurement passes every comparison.
    @Test
    void acceptsCompleteMatchingProbe() {
        AbiProbe probe = matchingProbe();
        AbiProbeVerifier.Comparison comparison = AbiProbeVerifier.compare(schema(), probe, probe.target());

        assertTrue(comparison.passed());
        assertTrue(comparison.mismatches().isEmpty());
    }

    /// Verifies that a native scalar-size mismatch names the exact declaration.
    @Test
    void rejectsTypeLayoutMismatch() {
        AbiProbe probe = matchingProbe();
        List<AbiProbe.TypeLayout> changedTypes = probe.types().stream()
                .map(layout -> layout.name().equals("f64")
                        ? new AbiProbe.TypeLayout(layout.name(), 4, layout.alignment())
                        : layout)
                .toList();
        AbiProbe invalid = withTypes(probe, changedTypes);

        AbiProbeVerifier.Comparison comparison = AbiProbeVerifier.compare(schema(), invalid, invalid.target());
        assertFalse(comparison.passed());
        assertTrue(comparison.mismatches().stream().anyMatch(message -> message.startsWith("types.f64")));
    }

    /// Verifies that an incomplete native record cannot pass by omission.
    @Test
    void rejectsMissingTypeMeasurement() {
        AbiProbe probe = matchingProbe();
        List<AbiProbe.TypeLayout> incomplete = new ArrayList<>(probe.types());
        incomplete.removeLast();
        AbiProbe invalid = withTypes(probe, incomplete);

        AbiProbeVerifier.Comparison comparison = AbiProbeVerifier.compare(schema(), invalid, invalid.target());
        assertFalse(comparison.passed());
        assertTrue(comparison.mismatches().stream().anyMatch(message -> message.contains("missing native measurement")));
        assertTrue(comparison.mismatches().stream().anyMatch(message -> message.startsWith("types.count")));
    }

    /// Verifies that a cross-compiled probe target must match the executing Java process target.
    @Test
    void rejectsHostTargetMismatch() {
        AbiProbe probe = matchingProbe();
        AbiProbe.Target differentHost = new AbiProbe.Target(
                "linux",
                probe.target().architecture(),
                probe.target().byteOrder(),
                probe.target().addressSize(),
                probe.target().addressAlignment()
        );

        AbiProbeVerifier.Comparison comparison = AbiProbeVerifier.compare(schema(), probe, differentHost);
        assertFalse(comparison.passed());
        assertTrue(comparison.mismatches().stream()
                .anyMatch(message -> message.startsWith("target.operatingSystem.host")));
    }

    /// Returns the canonical ABI probe schema.
    ///
    /// @return the validated schema
    private static AbiSchema schema() {
        return AbiSchemaCodec.read(Path.of("src/main/abi/abi-probe-schema-v1.json"));
    }

    /// Returns a complete measurement matching the canonical fixture.
    ///
    /// @return the matching probe
    private static AbiProbe matchingProbe() {
        return new AbiProbe(
                1,
                List.of("abi-minimum-layouts-v1", "abi-callback-conventions-v1"),
                new AbiProbe.Target("windows", "x86_64", AbiSchema.ByteOrder.LITTLE_ENDIAN, 8, 8),
                new AbiProbe.Compiler("clang", 21, 1, 0),
                List.of(
                        new AbiProbe.TypeLayout("u8", 1, 1),
                        new AbiProbe.TypeLayout("i32", 4, 4),
                        new AbiProbe.TypeLayout("u32", 4, 4),
                        new AbiProbe.TypeLayout("f64", 8, 8),
                        new AbiProbe.TypeLayout("const_u8_ptr", 8, 8),
                        new AbiProbe.TypeLayout("void_ptr", 8, 8),
                        new AbiProbe.TypeLayout("fixture_handle", 8, 8),
                        new AbiProbe.TypeLayout("fixture_flags", 4, 4)
                ),
                List.of(
                        new AbiProbe.AggregateLayout("fixture_pair", 8, 4, List.of(
                                new AbiProbe.FieldLayout("left", 0, null, null),
                                new AbiProbe.FieldLayout("right", 4, null, null)
                        )),
                        new AbiProbe.AggregateLayout("fixture_bits", 4, 4, List.of(
                                new AbiProbe.FieldLayout("mode", 0, 0, 3),
                                new AbiProbe.FieldLayout("ready", 0, 3, 1)
                        )),
                        new AbiProbe.AggregateLayout("fixture_value", 8, 8, List.of(
                                new AbiProbe.FieldLayout("integer", 0, null, null),
                                new AbiProbe.FieldLayout("floating", 0, null, null)
                        ))
                ),
                List.of(new AbiProbe.CallbackLayout(
                        "fixture_visit_callback",
                        AbiSchema.CallingConvention.SYSTEM,
                        8,
                        8,
                        142
                )),
                new AbiProbe.Checks(-7, 42, 6)
        );
    }

    /// Returns a probe copy with replacement type measurements.
    ///
    /// @param probe the source probe
    /// @param types the replacement type layouts
    /// @return the copied probe
    private static AbiProbe withTypes(AbiProbe probe, List<AbiProbe.TypeLayout> types) {
        return new AbiProbe(
                probe.protocolVersion(),
                probe.fixtures(),
                probe.target(),
                probe.compiler(),
                types,
                probe.aggregates(),
                probe.callbacks(),
                probe.checks()
        );
    }
}
