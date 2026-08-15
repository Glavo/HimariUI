package org.glavo.himari.samples.counter;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the shipped V0 CounterApp entry point.
@NotNullByDefault
final class V0CounterAppTest {
    /// Drives two activations through the real sample and checks PNG plus scene replay.
    @Test
    void rendersDeterministicPngAndScene() {
        V0CounterApp.Result first = V0CounterApp.run(2);
        V0CounterApp.Result second = V0CounterApp.run(2);
        assertEquals(2, first.count());
        assertEquals("Count: 2", first.label());
        assertTrue(first.focusObserved());
        assertTrue(first.semanticsExposeActivate());
        assertEquals((byte) 0x89, first.png().get(ValueLayout.JAVA_BYTE, 0L));
        assertEquals((byte) 0x50, first.png().get(ValueLayout.JAVA_BYTE, 1L));
        assertTrue(first.png().byteSize() > 64L);
        assertTrue(first.extendedLinear().byteSize() > 64L);
        assertEquals(first.sceneJson(), second.sceneJson());
        MemorySegment firstPng = first.png();
        MemorySegment secondPng = second.png();
        assertEquals(firstPng.byteSize(), secondPng.byteSize());
        for (long index = 0L; index < firstPng.byteSize(); index++) {
            assertEquals(firstPng.get(ValueLayout.JAVA_BYTE, index), secondPng.get(ValueLayout.JAVA_BYTE, index));
        }
    }
}
