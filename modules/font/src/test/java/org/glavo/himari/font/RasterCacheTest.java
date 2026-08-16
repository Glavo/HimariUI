package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies raster-mask intern against the declared byte budget.
@NotNullByDefault
final class RasterCacheTest {
    /// Rejects a second intern once the remaining budget cannot hold another mask.
    @Test
    void rejectsInternBeyondByteBudget() {
        SfntFont font = BitmapSfntFont.create();
        GlyphMask firstMask = GlyphRasterizer.rasterize(font, font.glyphId('A'), 16);
        RasterCache cache = new RasterCache(firstMask.coverage().length);
        @Nullable GlyphMask interned = cache.intern(font, font.glyphId('A'), 16);
        assertNotNull(interned);
        assertSame(interned, cache.intern(font, font.glyphId('A'), 16));
        assertEquals(1, cache.glyphCount());
        assertEquals(firstMask.coverage().length, cache.byteCount());
        assertEquals(firstMask.coverage().length, cache.maxBytes());
        assertNull(cache.intern(font, font.glyphId('B'), 16));
        assertEquals(1, cache.glyphCount());
        assertNull(cache.locate(font, font.glyphId('B'), 16));
        cache.clear();
        assertEquals(0, cache.glyphCount());
        assertEquals(0, cache.byteCount());
        assertNotNull(cache.intern(font, font.glyphId('B'), 16));
    }
}
