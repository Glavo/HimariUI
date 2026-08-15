package org.glavo.himari.rhi.vulkan;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Records one Vulkan instance and device-enumeration attempt.
///
/// @param status `passed` or `environment-blocked`
/// @param detail a pointer-free diagnostic
/// @param capabilities the snapshot, or `null` when blocked
@NotNullByDefault
public record VulkanProbe(String status, String detail, @Nullable VulkanCapabilities capabilities) {
    /// Validates the observation.
    public VulkanProbe {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(detail, "detail");
    }

    /// Attempts to open a device and records a block when the loader is absent.
    ///
    /// @return the observation
    public static VulkanProbe run() {
        if (VulkanLibraries.loaderName().isEmpty()) {
            return new VulkanProbe(
                    "environment-blocked",
                    "Vulkan loader is not a first-stable target on " + System.getProperty("os.name", ""),
                    null
            );
        }
        try (VulkanDevice device = VulkanDevice.open()) {
            return new VulkanProbe(
                    "passed",
                    "vkCreateDevice created a logical device on queue family "
                            + device.capabilities().graphicsQueueFamily(),
                    device.capabilities()
            );
        } catch (RuntimeException failure) {
            return new VulkanProbe(
                    "environment-blocked",
                    "Vulkan loader or instance failed: " + failure.getMessage(),
                    null
            );
        }
    }
}
