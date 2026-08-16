package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that HotSpot redefine stays environment-blocked on this check path.
@NotNullByDefault
final class HotSpotRedefineProbeTest {
    /// Standard HotSpot without a javaagent cannot redefine production classes.
    @Test
    void standardHotSpotIsEnvironmentBlocked() {
        HotSpotRedefineProbe.Result result = HotSpotRedefineProbe.probe();
        assertFalse(result.redefined());
        assertTrue(result.environmentBlocked());
        assertTrue(result.detail().startsWith("environment-blocked:"));
    }
}
