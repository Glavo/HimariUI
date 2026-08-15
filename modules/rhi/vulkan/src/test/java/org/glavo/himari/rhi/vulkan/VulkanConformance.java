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
        boolean logicalDevice = capabilities != null && capabilities.logicalDeviceCreated();
        int family = capabilities == null ? -1 : capabilities.graphicsQueueFamily();
        boolean presented = false;
        boolean cleared = false;
        boolean swapchainCreated = false;
        String presentDetail = probe.detail();
        if ("passed".equals(probe.status())
                && System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("windows")) {
            org.glavo.himari.platform.windows.WindowsPlatform platform =
                    new org.glavo.himari.platform.windows.WindowsBackend().open().toCompletableFuture().get();
            try (VulkanDevice device = VulkanDevice.open()) {
                org.glavo.himari.platform.windows.WindowsWindow window = platform.createWindow(
                        org.glavo.himari.platform.api.WindowRequest.toplevel(
                                new org.glavo.himari.platform.api.WindowConfiguration(
                                        "HimariUI Vulkan Conformance",
                                        new org.glavo.himari.platform.api.LogicalRect(16.0, 16.0, 320.0, 240.0),
                                        true,
                                        org.glavo.himari.platform.api.WindowState.NORMAL
                                )
                        ),
                        event -> { }
                ).toCompletableFuture().get();
                platform.pump();
                VulkanPresentation presentation = device.presentSdr(
                        window.moduleHandle(),
                        window.nativeHandle(),
                        320,
                        240
                );
                presented = presentation.presented();
                cleared = presentation.cleared();
                swapchainCreated = presentation.swapchainCreated();
                presentDetail = presentation.format() + "/" + presentation.presentMode();
                if (!presented || !cleared || !swapchainCreated) {
                    throw new IllegalStateException("Vulkan swapchain did not clear and present");
                }
                window.closeAsync().toCompletableFuture().get();
                platform.pump();
            } finally {
                platform.close();
            }
        }
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m5-vulkan",
                          "workPackage": "VULKAN-001",
                          "status": "%s",
                          "physicalDeviceCount": %d,
                          "logicalDeviceCreated": %s,
                          "graphicsQueueFamily": %d,
                          "swapchainCreated": %s,
                          "sdrPresent": %s,
                          "hdrAssumed": false,
                          "detail": "%s"
                        }
                        """.formatted(
                        probe.status(),
                        devices,
                        logicalDevice,
                        family,
                        swapchainCreated,
                        presented && cleared,
                        escape(presentDetail)
                ),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("capabilities.json"),
                """
                        {
                          "status": "%s",
                          "physicalDeviceCount": %d,
                          "logicalDeviceCreated": %s,
                          "graphicsQueueFamily": %d,
                          "win32SurfaceCreated": %s,
                          "swapchainCreated": %s,
                          "hdrPresentationEnabled": false,
                          "presentationMode": "color-managed-sdr"
                        }
                        """.formatted(
                        probe.status(),
                        devices,
                        logicalDevice,
                        family,
                        swapchainCreated,
                        swapchainCreated
                ),
                StandardCharsets.UTF_8
        );
        Files.writeString(output.resolve("validation.log"), "not-run\n", StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("presentation.json"),
                """
                        {
                          "mode": "%s",
                          "cleared": %s,
                          "presented": %s,
                          "hdrMetadataApplied": false
                        }
                        """.formatted(swapchainCreated ? "swapchain-sdr" : "uninitialized", cleared, presented),
                StandardCharsets.UTF_8
        );
        if (!Files.exists(output.resolve("native-load.log"))) {
            Files.writeString(output.resolve("native-load.log"), "no-framework-native-load\n", StandardCharsets.UTF_8);
        }
        System.out.println("VULKAN-001 " + probe.status() + ": " + presentDetail);
    }

    /// Escapes one JSON string fragment.
    ///
    /// @param value the raw text
    /// @return the escaped text
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "'");
    }
}
