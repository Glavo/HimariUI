package org.glavo.himari.platform.wayland;

import org.glavo.himari.platform.wayland.linux.WaylandLinuxHost;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.Locale;

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
        if (!WaylandLinuxHost.supported()) {
            assertEquals("environment-blocked", probe.status());
            assertEquals(-1, probe.fileDescriptor());
        }
    }

    /// The Linux host package does not claim support on a non-Linux client.
    @Test
    void linuxHostIsGatedOnOsName() {
        boolean linux = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
        assertEquals(linux, WaylandLinuxHost.supported());
    }
}
