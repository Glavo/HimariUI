package org.glavo.himari.rhi.vulkan;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records one deterministic SDR swapchain present.
///
/// @param presented whether `vkQueuePresentKHR` succeeded
/// @param cleared whether `vkCmdClearColorImage` was recorded
/// @param swapchainCreated whether `vkCreateSwapchainKHR` succeeded
/// @param imageIndex the acquired swapchain image index
/// @param imageCount the swapchain image count
/// @param format the selected `VkFormat` name
/// @param colorSpace the selected `VkColorSpaceKHR` name
/// @param presentMode the selected `VkPresentModeKHR` name
/// @param hdrMetadataApplied always `false`
@NotNullByDefault
public record VulkanPresentation(
        boolean presented,
        boolean cleared,
        boolean swapchainCreated,
        int imageIndex,
        int imageCount,
        String format,
        String colorSpace,
        String presentMode,
        boolean hdrMetadataApplied
) {
    /// Validates the observation.
    public VulkanPresentation {
        if (imageIndex < 0 || imageCount < 0) {
            throw new IllegalArgumentException("imageIndex and imageCount must be nonnegative");
        }
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(colorSpace, "colorSpace");
        Objects.requireNonNull(presentMode, "presentMode");
        if (hdrMetadataApplied) {
            throw new IllegalArgumentException("Production Vulkan first-stable present must not apply HDR metadata");
        }
    }
}
