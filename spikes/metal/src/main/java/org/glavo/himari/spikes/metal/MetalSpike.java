package org.glavo.himari.spikes.metal;

import org.glavo.himari.rhi.metal.MetalProbe;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes M0 Metal evidence from the production backend.
@NotNullByDefault
public final class MetalSpike {
    /// Prevents instantiation.
    private MetalSpike() {
    }

    /// Writes Metal probe evidence.
    ///
    /// @param arguments one output directory
    /// @throws IOException if evidence cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        MetalProbe probe = MetalProbe.run();
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        boolean queue = probe.capabilities() != null && probe.capabilities().commandQueueCreated();
        boolean committed = probe.capabilities() != null && probe.capabilities().commandBufferCommitted();
        Files.writeString(
                output.resolve("capabilities.json"),
                """
                        {
                          "hdrAssumed": false,
                          "commandQueueCreated": %s,
                          "commandBufferCommitted": %s,
                          "presentationMode": "color-managed-sdr"
                        }
                        """.formatted(queue, committed),
                StandardCharsets.UTF_8
        );
        Files.writeString(output.resolve("validation.log"), "not-run\n", StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("presentation.json"),
                "{\"mode\":\"command-queue\",\"hdrMetadataApplied\":false}\n",
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m0-metal-surface",
                          "workPackage": "SPIKE-METAL-001",
                          "status": "%s",
                          "commandQueueCreated": %s,
                          "commandBufferCommitted": %s,
                          "hdrAssumed": false,
                          "detail": "%s"
                        }
                        """.formatted(probe.status(), queue, committed, escape(probe.detail())),
                StandardCharsets.UTF_8
        );
        System.out.println("SPIKE-METAL-001 " + probe.status() + ": " + probe.detail());
    }

    /// Escapes one JSON string fragment.
    ///
    /// @param value the raw text
    /// @return the escaped text
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "'");
    }
}
