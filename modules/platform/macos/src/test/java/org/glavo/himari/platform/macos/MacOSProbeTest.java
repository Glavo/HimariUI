package org.glavo.himari.platform.macos;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the shipped macOS probe reports a runtime resolve or an environment block.
@NotNullByDefault
final class MacOSProbeTest {
    /// Runs the probe and checks the truthful SDR contract.
    @Test
    void reportsResolveOrEnvironmentBlock() {
        MacOSProbe probe = MacOSProbe.run();
        assertFalse(probe.hdrAssumed());
        assertTrue(probe.status().equals("resolved") || probe.status().equals("environment-blocked"));
        if (!MacOSLibraries.supportedHost()) {
            assertEquals("environment-blocked", probe.status());
        }
    }
}
