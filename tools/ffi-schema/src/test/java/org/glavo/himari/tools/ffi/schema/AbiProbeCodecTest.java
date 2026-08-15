package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies strict native ABI probe decoding and deterministic encoding.
@NotNullByDefault
final class AbiProbeCodecTest {
    /// Verifies semantic round-trip preservation and byte-stable canonical encoding.
    @Test
    void roundTripsCanonicalProbeDeterministically() {
        AbiProbe probe = AbiProbeCodec.read("abi-probe-v1", fixture());
        String first = AbiProbeCodec.write(probe);
        AbiProbe decoded = AbiProbeCodec.read("canonical", first);
        String second = AbiProbeCodec.write(decoded);

        assertEquals(probe, decoded);
        assertEquals(first, second);
    }

    /// Verifies that undeclared protocol fields are rejected.
    @Test
    void rejectsUnknownRootMember() {
        String invalid = fixture().replace(
                "  \"protocolVersion\": 1,",
                "  \"protocolVersion\": 1,\n  \"unexpected\": true,"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AbiProbeCodec.read("unknown-member", invalid)
        );
        assertTrue(exception.getMessage().contains("unknown member 'unexpected'"));
    }

    /// Verifies that incomplete protocol objects are rejected.
    @Test
    void rejectsMissingRequiredMember() {
        String invalid = fixture().replace("  \"checks\": {", "  \"renamedChecks\": {");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AbiProbeCodec.read("missing-member", invalid)
        );
        assertTrue(exception.getMessage().contains("missing required member 'checks'"));
    }

    /// Verifies that duplicate declaration names are rejected before comparison.
    @Test
    void rejectsDuplicateTypeName() {
        String invalid = fixture().replace(
                "{\"name\": \"u8\", \"byteSize\": 1, \"alignment\": 1}",
                "{\"name\": \"i32\", \"byteSize\": 1, \"alignment\": 1}"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AbiProbeCodec.read("duplicate-type", invalid)
        );
        assertTrue(exception.getMessage().contains("duplicates name 'i32'"));
    }

    /// Reads the canonical protocol fixture.
    ///
    /// @return the complete UTF-8 fixture
    private static String fixture() {
        try (InputStream input = Objects.requireNonNull(
                AbiProbeCodecTest.class.getResourceAsStream("/abi-probe-v1.json"),
                "Missing abi-probe-v1.json"
        )) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read abi-probe-v1.json", exception);
        }
    }
}
