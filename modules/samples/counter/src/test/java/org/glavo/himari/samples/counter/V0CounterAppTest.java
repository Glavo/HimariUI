package org.glavo.himari.samples.counter;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

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
        assertEquals((byte) 0x89, first.png()[0]);
        assertEquals((byte) 0x50, first.png()[1]);
        assertTrue(first.png().length > 64);
        assertTrue(first.extendedLinear().length > 64);
        assertEquals(first.sceneJson(), second.sceneJson());
        byte[] firstPng = first.png();
        byte[] secondPng = second.png();
        assertEquals(firstPng.length, secondPng.length);
        for (int index = 0; index < firstPng.length; index++) {
            assertEquals(firstPng[index], secondPng[index]);
        }
    }
}
