package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies atomic theme, style, image, and font publish through [`ResourceReload`].
@NotNullByDefault
final class ResourceReloadTest {
    /// Publishes one theme payload and notifies only that watcher.
    @Test
    void publishesThemeWithoutNotifyingOtherKeys() {
        ResourceReload reload = new ResourceReload();
        AtomicInteger themeGenerations = new AtomicInteger();
        AtomicInteger fontGenerations = new AtomicInteger();
        reload.watch(ResourceKind.THEME, "app", themeGenerations::set);
        reload.watch(ResourceKind.FONT, "ui", fontGenerations::set);
        MemorySegment bytes = MemorySegment.ofArray(new byte[] {1, 2, 3});
        ResourceReloadOutcome outcome = reload.publish(ResourceKind.THEME, "app", bytes);
        assertEquals(1, outcome.generation());
        assertTrue(outcome.published());
        assertEquals(1, outcome.notifiedKeys());
        assertEquals(1, themeGenerations.get());
        assertEquals(0, fontGenerations.get());
        ResourceSnapshot current = reload.current(ResourceKind.THEME, "app");
        assertNotNull(current);
        assertEquals(3, current.bytes().byteSize());
        assertEquals(1, current.generation());
    }

    /// Retains the last valid payload when a later publish is rejected.
    @Test
    void rejectedPublishKeepsLastValid() {
        ResourceReload reload = new ResourceReload();
        MemorySegment first = MemorySegment.ofArray(new byte[] {9});
        reload.publish(ResourceKind.STYLE, "sheet", first);
        ResourceReloadOutcome rejected = reload.reject(ResourceKind.STYLE, "sheet");
        assertTrue(rejected.failed());
        assertFalse(rejected.published());
        assertEquals(1, rejected.generation());
        assertTrue(rejected.lastValidRetained());
        ResourceSnapshot current = reload.current(ResourceKind.STYLE, "sheet");
        assertNotNull(current);
        assertEquals(1, current.bytes().byteSize());
        assertEquals(1, current.generation());
    }

    /// Publishes theme and image under one generation and releases the superseded theme.
    @Test
    void batchPublishSharesGenerationAndReleasesSuperseded() {
        ResourceReload reload = new ResourceReload();
        MemorySegment oldTheme = MemorySegment.ofArray(new byte[] {1});
        reload.publish(ResourceKind.THEME, "app", oldTheme);
        AtomicInteger theme = new AtomicInteger();
        AtomicInteger image = new AtomicInteger();
        reload.watch(ResourceKind.THEME, "app", theme::set);
        reload.watch(ResourceKind.IMAGE, "icon", image::set);
        ResourceReloadOutcome outcome = reload.publishAll(
                new ResourceKind[] {ResourceKind.THEME, ResourceKind.IMAGE},
                new String[] {"app", "icon"},
                new MemorySegment[] {
                        MemorySegment.ofArray(new byte[] {2, 2}),
                        MemorySegment.ofArray(new byte[] {3, 3, 3})
                }
        );
        assertEquals(2, outcome.generation());
        assertEquals(2, outcome.notifiedKeys());
        assertEquals(2, theme.get());
        assertEquals(2, image.get());
        assertEquals(1, reload.released().size());
        assertEquals(1, reload.released().getFirst().byteSize());
        ResourceSnapshot themeSnapshot = reload.current(ResourceKind.THEME, "app");
        assertNotNull(themeSnapshot);
        assertEquals(2, themeSnapshot.bytes().byteSize());
        assertEquals(2, themeSnapshot.generation());
    }
}
