package org.glavo.himari.platform.wayland;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the shipped Wayland probe reports a connect or an environment block.
@NotNullByDefault
final class WaylandProbeTest {
    /// Runs the probe and checks the truthful SDR contract.
    @Test
    void reportsConnectOrEnvironmentBlock() {
        WaylandProbe probe = WaylandProbe.run();
        assertFalse(probe.hdrAssumed());
        assertTrue(probe.status().equals("connected") || probe.status().equals("environment-blocked"));
        if (!WaylandLibraries.supportedHost()) {
            assertEquals("environment-blocked", probe.status());
            assertEquals(-1, probe.fileDescriptor());
        }
    }
}
