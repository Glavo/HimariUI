package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies software-pipeline intern against the declared occupancy budget.
@NotNullByDefault
final class PipelineCacheTest {
    /// Rejects a new key once the occupancy budget is full.
    @Test
    void rejectsInternBeyondEntryBudget() {
        PipelineCache cache = new PipelineCache(1);
        PipelineCache.Key triangles = new PipelineCache.Key("triangles", "rgba8", "src-over");
        PipelineCache.Key lines = new PipelineCache.Key("lines", "rgba8", "src-over");
        @Nullable PipelineCache.Entry first = cache.intern(triangles);
        assertNotNull(first);
        assertEquals(1, first.id());
        assertSame(first, cache.intern(triangles));
        assertEquals(1, cache.entryCount());
        assertEquals(1, cache.maxEntries());
        assertNull(cache.intern(lines));
        assertNull(cache.locate(lines));
        cache.clear();
        assertEquals(0, cache.entryCount());
        assertNotNull(cache.intern(lines));
    }
}
