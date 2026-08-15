package org.glavo.himari.rhi.metal;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes production Metal device evidence or an environment block.
@NotNullByDefault
public final class MetalConformance {
    /// Prevents instantiation.
    private MetalConformance() {
    }

    /// Probes the system Metal device and writes artifacts.
    ///
    /// @param arguments one output directory
    /// @throws Exception if artifacts cannot be written
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        MetalProbe probe = MetalProbe.run();
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m7-metal",
                          "workPackage": "METAL-001",
                          "status": "%s",
                          "hdrAssumed": false,
                          "detail": "%s"
                        }
                        """.formatted(probe.status(), escape(probe.detail())),
                StandardCharsets.UTF_8
        );
        Files.writeString(output.resolve("capabilities.json"), "{\"hdrAssumed\":false}\n", StandardCharsets.UTF_8);
        Files.writeString(output.resolve("validation.log"), "not-run\n", StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("presentation.json"),
                "{\"mode\":\"uninitialized\",\"hdrMetadataApplied\":false}\n",
                StandardCharsets.UTF_8
        );
        if (!Files.exists(output.resolve("native-load.log"))) {
            Files.writeString(output.resolve("native-load.log"), "no-framework-native-load\n", StandardCharsets.UTF_8);
        }
        System.out.println("METAL-001 " + probe.status() + ": " + probe.detail());
    }

    /// Escapes one JSON string fragment.
    ///
    /// @param value the raw text
    /// @return the escaped text
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "'");
    }
}
