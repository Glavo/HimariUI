package org.glavo.himari.rhi.metal;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the shipped Metal probe creates a device or reports an environment block.
@NotNullByDefault
final class MetalDeviceTest {
    /// Runs the probe and checks the truthful SDR contract.
    @Test
    void createsDeviceOrBlocks() {
        MetalProbe probe = MetalProbe.run();
        assertFalse(probe.hdrAssumed());
        assertTrue(probe.status().equals("created") || probe.status().equals("environment-blocked"));
        if (!MetalLibraries.supportedHost()) {
            assertEquals("environment-blocked", probe.status());
        }
    }
}
