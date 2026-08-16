package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/// Packs grayscale glyph masks into one fixed-size CPU sheet.
///
/// Glyphs are interned by face identity, glyph id, and pixel height. A miss rasters through
/// [`GlyphRasterizer`] and shelf-packs the mask with one pixel of padding. Insertion that does not
/// fit the remaining sheet is left uncached and returns `null`; [`#clear()`] is the eviction
/// operation. The atlas is not thread-safe.
@NotNullByDefault
public final class GlyphAtlas {
    /// Empty packed glyph reused for zero-size masks.
    private static final AtlasGlyph EMPTY = new AtlasGlyph(0, 0, 0, 0);

    /// One pixel of padding on the right and top of each packed mask.
    private static final int PADDING = 1;

    /// Sheet width in pixels.
    private final int width;

    /// Sheet height in pixels.
    private final int height;

    /// Row-major coverage; row 0 is the bottom edge.
    private final byte[] coverage;

    /// Packed glyphs by face identity, glyph id, and pixel height.
    private final HashMap<CacheKey, AtlasGlyph> entries;

    /// Next free X on the current shelf.
    private int cursorX;

    /// Bottom of the current shelf.
    private int cursorY;

    /// Height of the current shelf, including padding.
    private int shelfHeight;

    /// Maximum interned glyphs, including empty masks.
    private final int maxGlyphs;

    /// Creates an empty atlas with no occupancy budget.
    ///
    /// @param width the positive sheet width
    /// @param height the positive sheet height
    public GlyphAtlas(int width, int height) {
        this(width, height, Integer.MAX_VALUE);
    }

    /// Creates an empty atlas that rejects intern beyond `maxGlyphs`.
    ///
    /// @param width the positive sheet width
    /// @param height the positive sheet height
    /// @param maxGlyphs the positive occupancy budget
    public GlyphAtlas(int width, int height, int maxGlyphs) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Atlas extents must be positive");
        }
        if (maxGlyphs <= 0) {
            throw new IllegalArgumentException("Atlas glyph budget must be positive");
        }
        this.width = width;
        this.height = height;
        this.maxGlyphs = maxGlyphs;
        this.coverage = new byte[Math.multiplyExact(width, height)];
        this.entries = new HashMap<>();
    }

    /// Returns the sheet width.
    ///
    /// @return the positive width
    public int width() {
        return width;
    }

    /// Returns the sheet height.
    ///
    /// @return the positive height
    public int height() {
        return height;
    }

    /// Returns the number of interned glyphs, including empty masks.
    ///
    /// @return the nonnegative occupancy
    public int glyphCount() {
        return entries.size();
    }

    /// Returns the occupancy budget.
    ///
    /// @return the positive maximum interned glyph count
    public int maxGlyphs() {
        return maxGlyphs;
    }

    /// Returns a copy of the packed sheet.
    ///
    /// @return the coverage mask
    public GlyphMask snapshot() {
        return new GlyphMask(width, height, coverage.clone());
    }

    /// Returns a previously interned glyph.
    ///
    /// @param font the face
    /// @param glyphId the glyph identity
    /// @param pixelHeight the destination em height
    /// @return the packed rectangle, or `null` when the glyph is not in the sheet
    public @Nullable AtlasGlyph locate(SfntFont font, int glyphId, int pixelHeight) {
        Objects.requireNonNull(font, "font");
        return entries.get(new CacheKey(font, glyphId, pixelHeight));
    }

    /// Rasters `glyphId` on a miss and packs it when the sheet has room.
    ///
    /// A zero-size mask is recorded as a zero rectangle and does not occupy sheet pixels. A mask
    /// that does not fit is not stored; the caller may raster it outside the atlas.
    ///
    /// @param font the face
    /// @param glyphId the glyph identity
    /// @param pixelHeight the positive destination em height
    /// @return the packed rectangle, or `null` when the mask does not fit
    public @Nullable AtlasGlyph intern(SfntFont font, int glyphId, int pixelHeight) {
        Objects.requireNonNull(font, "font");
        CacheKey key = new CacheKey(font, glyphId, pixelHeight);
        @Nullable AtlasGlyph existing = entries.get(key);
        if (existing != null) {
            return existing;
        }
        if (entries.size() >= maxGlyphs) {
            return null;
        }
        GlyphMask mask = GlyphRasterizer.rasterize(font, glyphId, pixelHeight);
        AtlasGlyph packed = pack(mask);
        if (packed == null) {
            return null;
        }
        entries.put(key, packed);
        return packed;
    }

    /// Removes every packed glyph and zeros the sheet.
    public void clear() {
        entries.clear();
        cursorX = 0;
        cursorY = 0;
        shelfHeight = 0;
        Arrays.fill(coverage, (byte) 0);
    }

    /// Shelf-packs `mask` or returns `null` when it does not fit.
    ///
    /// @param mask the rasterized glyph
    /// @return the packed rectangle
    private @Nullable AtlasGlyph pack(GlyphMask mask) {
        int maskWidth = mask.width();
        int maskHeight = mask.height();
        if (maskWidth == 0 || maskHeight == 0) {
            return EMPTY;
        }
        if (maskWidth > width || maskHeight > height) {
            return null;
        }
        int occupiedWidth = maskWidth + PADDING;
        int occupiedHeight = maskHeight + PADDING;
        if (cursorX > 0 && cursorX + maskWidth > width) {
            cursorX = 0;
            cursorY += shelfHeight;
            shelfHeight = 0;
        }
        if (cursorY + maskHeight > height) {
            return null;
        }
        int x = cursorX;
        int y = cursorY;
        blit(mask, x, y);
        cursorX = x + occupiedWidth;
        if (occupiedHeight > shelfHeight) {
            shelfHeight = occupiedHeight;
        }
        return new AtlasGlyph(x, y, maskWidth, maskHeight);
    }

    /// Copies `mask` into the sheet at `(x, y)`.
    ///
    /// @param mask the source
    /// @param x the left edge
    /// @param y the bottom edge
    private void blit(GlyphMask mask, int x, int y) {
        byte[] source = mask.coverage();
        int maskWidth = mask.width();
        int maskHeight = mask.height();
        for (int row = 0; row < maskHeight; row++) {
            int dest = (y + row) * width + x;
            System.arraycopy(source, row * maskWidth, coverage, dest, maskWidth);
        }
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
                throw new IllegalArgumentException("Atlas key glyph and pixel height must be valid");
            }
        }
    }
}
