package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;

/// Caches rasterized glyph masks until a declared byte budget is exhausted.
///
/// Entries are keyed by face identity, glyph id, and pixel height. A miss rasters through
/// [`GlyphRasterizer`]. Insertion that would exceed [`#maxBytes()`] is rejected and left
/// uncached. The cache is not thread-safe.
@NotNullByDefault
public final class RasterCache {
    /// Maximum stored coverage bytes.
    private final int maxBytes;

    /// Interned masks.
    private final HashMap<CacheKey, GlyphMask> entries = new HashMap<>();

    /// Sum of interned coverage array lengths.
    private int byteCount;

    /// Creates an empty cache with a positive byte budget.
    ///
    /// @param maxBytes the positive occupancy budget in coverage bytes
    public RasterCache(int maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("Raster cache byte budget must be positive");
        }
        this.maxBytes = maxBytes;
    }

    /// Returns the occupancy budget.
    ///
    /// @return the positive maximum stored coverage bytes
    public int maxBytes() {
        return maxBytes;
    }

    /// Returns the stored coverage-byte occupancy.
    ///
    /// @return the nonnegative occupancy
    public int byteCount() {
        return byteCount;
    }

    /// Returns the number of interned masks.
    ///
    /// @return the nonnegative count
    public int glyphCount() {
        return entries.size();
    }

    /// Returns a previously interned mask.
    ///
    /// @param font the face
    /// @param glyphId the glyph identity
    /// @param pixelHeight the destination em height
    /// @return the mask, or `null` when the glyph is not cached
    public @Nullable GlyphMask locate(SfntFont font, int glyphId, int pixelHeight) {
        Objects.requireNonNull(font, "font");
        return entries.get(new CacheKey(font, glyphId, pixelHeight));
    }

    /// Rasters `glyphId` on a miss when the remaining budget can hold the mask.
    ///
    /// A zero-size mask is stored and occupies zero bytes. A mask that would exceed the budget is
    /// not stored.
    ///
    /// @param font the face
    /// @param glyphId the glyph identity
    /// @param pixelHeight the positive destination em height
    /// @return the interned mask, or `null` when the budget cannot accept it
    public @Nullable GlyphMask intern(SfntFont font, int glyphId, int pixelHeight) {
        Objects.requireNonNull(font, "font");
        CacheKey key = new CacheKey(font, glyphId, pixelHeight);
        @Nullable GlyphMask existing = entries.get(key);
        if (existing != null) {
            return existing;
        }
        GlyphMask mask = GlyphRasterizer.rasterize(font, glyphId, pixelHeight);
        int cost = mask.coverage().length;
        if (byteCount > maxBytes - cost) {
            return null;
        }
        entries.put(key, mask);
        byteCount += cost;
        return mask;
    }

    /// Removes every interned mask.
    public void clear() {
        entries.clear();
        byteCount = 0;
    }

    /// Identity-based intern key.
    ///
    /// @param font the face instance
    /// @param glyphId the glyph identity
    /// @param pixelHeight the destination em height
    private record CacheKey(SfntFont font, int glyphId, int pixelHeight) {
        /// Validates the key.
        private CacheKey {
            Objects.requireNonNull(font, "font");
            if (glyphId < 0 || pixelHeight <= 0) {
                throw new IllegalArgumentException("Raster cache key glyph and pixel height must be valid");
            }
        }
    }
}
