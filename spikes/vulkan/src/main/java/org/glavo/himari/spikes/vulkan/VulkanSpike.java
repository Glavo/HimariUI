package org.glavo.himari.spikes.vulkan;

import org.glavo.himari.rhi.vulkan.VulkanProbe;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes M0 Vulkan evidence from the production backend.
@NotNullByDefault
public final class VulkanSpike {
    /// Prevents instantiation.
    private VulkanSpike() {
    }

    /// Writes Vulkan probe evidence.
    ///
    /// @param arguments one output directory
    /// @throws IOException if evidence cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        VulkanProbe probe = VulkanProbe.run();
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        String status = "passed".equals(probe.status()) ? "passed" : "environment-blocked";
        Files.writeString(output.resolve("native-load.log"), "no-framework-native-load\n", StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("capabilities.json"),
                "{\"status\":\"" + status + "\",\"hdrAssumed\":false}\n",
                StandardCharsets.UTF_8
        );
        Files.writeString(output.resolve("validation.log"), "not-run\n", StandardCharsets.UTF_8);
        Files.writeString(output.resolve("presentation.json"), "{\"mode\":\"uninitialized\"}\n", StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("results.json"),
                "{\"profile\":\"m0-vulkan-surface\",\"status\":\"" + status + "\",\"detail\":\""
                        + probe.detail().replace("\"", "'") + "\"}\n",
                StandardCharsets.UTF_8
        );
        System.out.println("SPIKE-VK-001 " + status + ": " + probe.detail());
    }
}
