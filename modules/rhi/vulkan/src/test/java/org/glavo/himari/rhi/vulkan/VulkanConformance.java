package org.glavo.himari.rhi.vulkan;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes production Vulkan instance evidence or an environment block.
@NotNullByDefault
public final class VulkanConformance {
    /// Prevents instantiation.
    private VulkanConformance() {
    }

    /// Probes the loader and writes artifacts.
    ///
    /// @param arguments one output directory
    /// @throws Exception if artifacts cannot be written
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        VulkanProbe probe = VulkanProbe.run();
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        @Nullable VulkanCapabilities capabilities = probe.capabilities();
        int devices = capabilities == null ? 0 : capabilities.physicalDeviceCount();
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m5-vulkan",
                          "workPackage": "VULKAN-001",
                          "status": "%s",
                          "physicalDeviceCount": %d,
                          "hdrAssumed": false,
                          "detail": "%s"
                        }
                        """.formatted(probe.status(), devices, escape(probe.detail())),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("capabilities.json"),
                """
                        {
                          "status": "%s",
                          "physicalDeviceCount": %d,
                          "hdrPresentationEnabled": false,
                          "presentationMode": "color-managed-sdr"
                        }
                        """.formatted(probe.status(), devices),
                StandardCharsets.UTF_8
        );
        Files.writeString(output.resolve("validation.log"), "not-run\n", StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("presentation.json"),
                "{\"mode\":\"uninitialized\",\"hdrMetadataApplied\":false}\n",
                StandardCharsets.UTF_8
        );
        if (!Files.exists(output.resolve("native-load.log"))) {
            Files.writeString(output.resolve("native-load.log"), "no-framework-native-load\n", StandardCharsets.UTF_8);
        }
        System.out.println("VULKAN-001 " + probe.status() + ": " + probe.detail());
    }

    /// Escapes one JSON string fragment.
    ///
    /// @param value the raw text
    /// @return the escaped text
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "'");
    }
}
