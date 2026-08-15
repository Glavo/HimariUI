package org.glavo.himari.spikes.wayland;

import org.glavo.himari.platform.wayland.WaylandProbe;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes M0 Wayland evidence from the production backend.
@NotNullByDefault
public final class WaylandSpike {
    /// Prevents instantiation.
    private WaylandSpike() {
    }

    /// Writes Wayland probe evidence.
    ///
    /// @param arguments one output directory
    /// @throws IOException if evidence cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        WaylandProbe probe = WaylandProbe.run();
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        String detail = escape(probe.detail());
        Files.writeString(output.resolve("native-load.log"), "no-framework-native-load\n", StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("events.json"),
                """
                        {
                          "status": "%s",
                          "fileDescriptor": %d,
                          "xdgWmBaseAdvertised": %s,
                          "toplevelCreated": %s
                        }
                        """.formatted(
                        probe.status(),
                        probe.fileDescriptor(),
                        probe.xdgWmBaseAdvertised(),
                        probe.toplevelCreated()
                ),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("capabilities.json"),
                """
                        {
                          "hdrAssumed": false,
                          "shmAdvertised": %s,
                          "seatAdvertised": %s,
                          "decorationManagerAdvertised": %s
                        }
                        """.formatted(
                        probe.shmAdvertised(),
                        probe.seatAdvertised(),
                        probe.decorationManagerAdvertised()
                ),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("presentation.json"),
                "{\"mode\":\"software-sdr\",\"hdrMetadataApplied\":false}\n",
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m0-wayland-window",
                          "workPackage": "SPIKE-WAYLAND-001",
                          "status": "%s",
                          "fileDescriptor": %d,
                          "xdgWmBaseAdvertised": %s,
                          "toplevelCreated": %s,
                          "shmAdvertised": %s,
                          "seatAdvertised": %s,
                          "decorationManagerAdvertised": %s,
                          "hdrAssumed": false,
                          "detail": "%s"
                        }
                        """.formatted(
                        probe.status(),
                        probe.fileDescriptor(),
                        probe.xdgWmBaseAdvertised(),
                        probe.toplevelCreated(),
                        probe.shmAdvertised(),
                        probe.seatAdvertised(),
                        probe.decorationManagerAdvertised(),
                        detail
                ),
                StandardCharsets.UTF_8
        );
        System.out.println("SPIKE-WAYLAND-001 " + probe.status() + ": " + probe.detail());
    }

    /// Escapes one JSON string fragment.
    ///
    /// @param value the raw text
    /// @return the escaped text
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "'");
    }
}
