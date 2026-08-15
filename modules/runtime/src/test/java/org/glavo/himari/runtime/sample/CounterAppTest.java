package org.glavo.himari.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// Verifies the shipped SAMPLE-001 CounterApp entry point.
@NotNullByDefault
final class CounterAppTest {
    /// Drives two injected activations through the real sample.
    @Test
    void incrementsLabelThroughShippedEntry() {
        CounterApp.SampleSnapshot first = CounterApp.run(2);
        CounterApp.SampleSnapshot second = CounterApp.run(2);
        assertEquals(2, first.count());
        assertEquals("Count: 2", first.label());
        assertEquals(first.toJson(), second.toJson());
        assertFalse(first.toJson().contains("nativeLibraryLoaded\": true"));
    }
}