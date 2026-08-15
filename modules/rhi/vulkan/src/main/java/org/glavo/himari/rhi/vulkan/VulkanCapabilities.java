package org.glavo.himari.rhi.vulkan;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Reports a truthful SDR Vulkan capability snapshot.
///
/// @param physicalDeviceCount the enumerated physical-device count
/// @param logicalDeviceCreated whether `vkCreateDevice` succeeded
/// @param graphicsQueueFamily the selected graphics queue family, or `-1`
/// @param win32SurfaceCreated whether a Win32 surface was created
/// @param hdrPresentationEnabled always `false` for this first-stable backend
/// @param presentationMode the explicit effective presentation mode
@NotNullByDefault
public record VulkanCapabilities(
        int physicalDeviceCount,
        boolean logicalDeviceCreated,
        int graphicsQueueFamily,
        boolean win32SurfaceCreated,
        boolean hdrPresentationEnabled,
        String presentationMode
) {
    /// Validates the snapshot.
    public VulkanCapabilities {
        if (physicalDeviceCount < 0) {
            throw new IllegalArgumentException("physicalDeviceCount must be nonnegative");
        }
        Objects.requireNonNull(presentationMode, "presentationMode");
        if (hdrPresentationEnabled) {
            throw new IllegalArgumentException("Production Vulkan first-stable presentation is SDR only");
        }
    }
}
