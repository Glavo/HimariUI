package org.glavo.himari.rhi.vulkan;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the shipped Vulkan probe either enumerates devices or reports a block.
@NotNullByDefault
final class VulkanDeviceTest {
    /// Runs the probe against the shipped loader path.
    @Test
    void enumeratesDevicesOrBlocks() {
        VulkanProbe probe = VulkanProbe.run();
        assertTrue(probe.status().equals("passed") || probe.status().equals("environment-blocked"));
        if (probe.capabilities() != null) {
            assertTrue(probe.capabilities().physicalDeviceCount() > 0);
            assertTrue(probe.capabilities().logicalDeviceCreated());
            assertTrue(probe.capabilities().graphicsQueueFamily() >= 0);
            assertFalse(probe.capabilities().hdrPresentationEnabled());
        }
    }
}
