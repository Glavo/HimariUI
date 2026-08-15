package org.glavo.himari.spikes.objc;

import org.glavo.himari.objc.ObjcBlockProbe;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes M0 Objective-C block evidence from the production ABI module.
@NotNullByDefault
public final class ObjcBlockSpike {
    /// Prevents instantiation.
    private ObjcBlockSpike() {
    }

    /// Writes block-probe evidence.
    ///
    /// @param arguments one output directory
    /// @throws IOException if evidence cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        ObjcBlockProbe probe = ObjcBlockProbe.run();
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("lifetime.json"),
                """
                        {
                          "copied": false,
                          "layoutByteSize": %d,
                          "policy": "%s"
                        }
                        """.formatted(probe.layoutByteSize(), probe.policy().name()),
                StandardCharsets.UTF_8
        );
        Files.writeString(output.resolve("native-load.log"), "no-framework-native-load\n", StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m0-objc-block",
                          "workPackage": "SPIKE-OBJC-BLOCK-001",
                          "status": "%s",
                          "policy": "%s",
                          "layoutByteSize": %d,
                          "detail": "%s"
                        }
                        """.formatted(
                        probe.status(),
                        probe.policy().name(),
                        probe.layoutByteSize(),
                        escape(probe.detail())
                ),
                StandardCharsets.UTF_8
        );
        System.out.println("SPIKE-OBJC-BLOCK-001 " + probe.status() + ": " + probe.detail());
    }

    /// Escapes one JSON string fragment.
    ///
    /// @param value the raw text
    /// @return the escaped text
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "'");
    }
}
