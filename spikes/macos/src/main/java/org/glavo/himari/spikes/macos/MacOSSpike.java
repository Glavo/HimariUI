package org.glavo.himari.spikes.macos;

import org.glavo.himari.platform.macos.MacOSProbe;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes M0 macOS evidence from the production backend.
@NotNullByDefault
public final class MacOSSpike {
    /// Prevents instantiation.
    private MacOSSpike() {
    }

    /// Writes macOS probe evidence.
    ///
    /// @param arguments one output directory
    /// @throws IOException if evidence cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        MacOSProbe probe = MacOSProbe.run();
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(output.resolve("native-load.log"), "no-framework-native-load\n", StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("events.json"),
                "{\"status\":\"" + probe.status() + "\"}\n",
                StandardCharsets.UTF_8
        );
        Files.writeString(output.resolve("capabilities.json"), "{\"hdrAssumed\":false}\n", StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("results.json"),
                "{\"profile\":\"m0-macos-window\",\"status\":\"" + probe.status() + "\",\"detail\":\""
                        + probe.detail().replace("\"", "'") + "\"}\n",
                StandardCharsets.UTF_8
        );
        System.out.println("SPIKE-MAC-001 " + probe.status() + ": " + probe.detail());
    }
}
