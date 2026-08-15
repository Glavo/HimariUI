package org.glavo.himari.platform.wayland;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes production Wayland display evidence or an environment block.
@NotNullByDefault
public final class WaylandConformance {
    /// Prevents instantiation.
    private WaylandConformance() {
    }

    /// Probes the compositor connection and writes artifacts.
    ///
    /// @param arguments one output directory
    /// @throws Exception if artifacts cannot be written
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        WaylandProbe probe = WaylandProbe.run();
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m5-wayland",
                          "workPackage": "WAYLAND-001",
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
                        escape(probe.detail())
                ),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("events.json"),
                "{\"status\":\"" + probe.status() + "\"}\n",
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("capabilities.json"),
                "{\"hdrAssumed\":false}\n",
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("presentation.json"),
                "{\"mode\":\"software-sdr\"}\n",
                StandardCharsets.UTF_8
        );
        if (!Files.exists(output.resolve("native-load.log"))) {
            Files.writeString(output.resolve("native-load.log"), "no-framework-native-load\n", StandardCharsets.UTF_8);
        }
        System.out.println("WAYLAND-001 " + probe.status() + ": " + probe.detail());
    }

    /// Escapes one JSON string fragment.
    ///
    /// @param value the raw text
    /// @return the escaped text
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "'");
    }
}
