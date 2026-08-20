package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that JBR enhanced redefinition stays environment-blocked on this check path.
@NotNullByDefault
final class JbrRedefineProbeTest {
    /// Stock HotSpot without JBR/DCEVM cannot run the enhanced-redefinition profile.
    @Test
    void stockHotSpotIsEnvironmentBlocked() {
        JbrRedefineProbe.Result result = JbrRedefineProbe.probe();
        assertFalse(result.redefined());
        assertTrue(result.environmentBlocked());
        assertTrue(result.detail().startsWith("environment-blocked:"));
    }
}
