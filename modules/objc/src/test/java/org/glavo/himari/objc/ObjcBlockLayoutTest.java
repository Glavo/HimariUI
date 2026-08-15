package org.glavo.himari.objc;

import org.glavo.himari.objc.generated.ObjcBlockLayouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

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

    /// Fills a 32-byte block object without loading `libobjc`.
    @Test
    void fillsDocumentedObjectLayout() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment isa = MemorySegment.ofAddress(0x11L);
            MemorySegment invoke = MemorySegment.ofAddress(0x22L);
            MemorySegment descriptor = MemorySegment.ofAddress(0x33L);
            ObjcBlockObject object = ObjcBlockObject.allocate(arena, isa, 1 << 25, invoke, descriptor);
            assertEquals(32L, object.pointer().byteSize());
            assertEquals(0x11L, object.isa().address());
            assertEquals(1 << 25, object.flags());
            assertEquals(0x22L, object.invoke().address());
            assertEquals(0x33L, object.descriptor().address());
            assertEquals(0, object.pointer().get(ValueLayout.JAVA_INT, ObjcBlockLayout.ABI64.reservedOffset()));
        }
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
