package org.glavo.himari.objc;

import org.glavo.himari.objc.generated.ObjcBlockLayouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the documented 64-bit block layout and the host-gated symbol probe.
@NotNullByDefault
final class ObjcBlockLayoutTest {
    /// Checks generated offsets against the documented Apple ABI.
    @Test
    void matchesDocumentedAbi() {
        assertEquals(32L, ObjcBlockLayouts.BLOCK_LAYOUT.byteSize());
        assertEquals(ObjcBlockLayout.ABI64.isaOffset(), ObjcBlockLayouts.BLOCK_LAYOUT_ISA_OFFSET);
        assertEquals(ObjcBlockLayout.ABI64.flagsOffset(), ObjcBlockLayouts.BLOCK_LAYOUT_FLAGS_OFFSET);
        assertEquals(ObjcBlockLayout.ABI64.invokeOffset(), ObjcBlockLayouts.BLOCK_LAYOUT_INVOKE_OFFSET);
        assertEquals(ObjcBlockLayout.ABI64.descriptorOffset(), ObjcBlockLayouts.BLOCK_LAYOUT_DESCRIPTOR_OFFSET);
    }

    /// Runs the shipped probe.
    @Test
    void probeReportsLayoutOrBlock() {
        ObjcBlockProbe probe = ObjcBlockProbe.run();
        assertEquals(32L, probe.layoutByteSize());
        assertEquals(ObjcBlockPolicy.PREFER_BLOCK_FREE_APIS, probe.policy());
        assertTrue(probe.status().equals("layout-verified") || probe.status().equals("environment-blocked"));
        if (!ObjcBlockLibraries.supportedHost()) {
            assertEquals("environment-blocked", probe.status());
        }
    }
}
