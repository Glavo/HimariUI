package org.glavo.himari.objc;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes production Objective-C block ABI evidence or an environment block.
@NotNullByDefault
public final class ObjcBlockConformance {
    /// Prevents instantiation.
    private ObjcBlockConformance() {
    }

    /// Verifies the block layout and writes artifacts.
    ///
    /// @param arguments one output directory
    /// @throws Exception if artifacts cannot be written
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        ObjcBlockProbe probe = ObjcBlockProbe.run();
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m7-objc-block",
                          "workPackage": "OBJC-001",
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
        Files.writeString(
                output.resolve("lifetime.json"),
                "{\"copied\":false,\"layoutByteSize\":" + probe.layoutByteSize() + "}\n",
                StandardCharsets.UTF_8
        );
        if (!Files.exists(output.resolve("native-load.log"))) {
            Files.writeString(output.resolve("native-load.log"), "no-framework-native-load\n", StandardCharsets.UTF_8);
        }
        System.out.println("OBJC-001 " + probe.status() + ": " + probe.detail());
    }

    /// Escapes one JSON string fragment.
    ///
    /// @param value the raw text
    /// @return the escaped text
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "'");
    }
}
