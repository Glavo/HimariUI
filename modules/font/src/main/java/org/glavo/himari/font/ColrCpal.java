package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// Parses COLR v0 base/layer records and CPAL palettes.
///
/// Version-1 paint trees are ignored. A missing table pair yields no color layers. Palette index
/// [`PaletteColor#FOREGROUND`] leaves [`ColorLayer#color()`] `null`.
@NotNullByDefault
final class ColrCpal {
    /// Shared empty parser.
    static final ColrCpal EMPTY = new ColrCpal(
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new PaletteColor[0],
            new int[0],
            0
    );

    /// Sorted base glyph ids.
    private final int[] baseGlyphs;

    /// First layer index for each base.
    private final int[] firstLayers;

    /// Layer counts for each base.
    private final int[] layerCounts;

    /// Layer glyph ids.
    private final int[] layerGlyphs;

    /// Layer palette indices.
    private final int[] layerPalettes;

    /// Flat CPAL color records.
    private final PaletteColor[] colors;

    /// Start index into [`#colors`] for each palette.
    private final int[] paletteStarts;

    /// Entries per palette.
    private final int entriesPerPalette;

    /// Creates a parsed table pair.
    private ColrCpal(
            int[] baseGlyphs,
            int[] firstLayers,
            int[] layerCounts,
            int[] layerGlyphs,
            int[] layerPalettes,
            PaletteColor[] colors,
            int[] paletteStarts,
            int entriesPerPalette
    ) {
        this.baseGlyphs = baseGlyphs;
        this.firstLayers = firstLayers;
        this.layerCounts = layerCounts;
        this.layerGlyphs = layerGlyphs;
        this.layerPalettes = layerPalettes;
        this.colors = colors;
        this.paletteStarts = paletteStarts;
        this.entriesPerPalette = entriesPerPalette;
    }

    /// Parses optional `COLR` and `CPAL` tables.
    ///
    /// @param colr the COLR table, or `null`
    /// @param cpal the CPAL table, or `null`
    /// @return the parsed pair; empty when either table is missing or not v0
    static ColrCpal parse(@Nullable ByteBuffer colr, @Nullable ByteBuffer cpal) {
        if (colr == null || cpal == null) {
            return EMPTY;
        }
        ByteBuffer color = colr.duplicate().order(java.nio.ByteOrder.BIG_ENDIAN);
        ByteBuffer palette = cpal.duplicate().order(java.nio.ByteOrder.BIG_ENDIAN);
        color.clear();
        palette.clear();
        if (color.remaining() < 14 || palette.remaining() < 12) {
            throw new IllegalArgumentException("COLR or CPAL header is truncated");
        }
        int colrVersion = Short.toUnsignedInt(color.getShort());
        if (colrVersion != 0) {
            return EMPTY;
        }
        int baseCount = Short.toUnsignedInt(color.getShort());
        int baseOffset = color.getInt();
        int layerOffset = color.getInt();
        int layerCount = Short.toUnsignedInt(color.getShort());
        int[] baseGlyphs = new int[baseCount];
        int[] firstLayers = new int[baseCount];
        int[] layerCounts = new int[baseCount];
        seek(color, baseOffset, baseCount * 6);
        for (int index = 0; index < baseCount; index++) {
            baseGlyphs[index] = Short.toUnsignedInt(color.getShort());
            firstLayers[index] = Short.toUnsignedInt(color.getShort());
            layerCounts[index] = Short.toUnsignedInt(color.getShort());
            if (index > 0 && baseGlyphs[index] <= baseGlyphs[index - 1]) {
                throw new IllegalArgumentException("COLR base glyph records must be sorted");
            }
        }
        int[] layerGlyphs = new int[layerCount];
        int[] layerPalettes = new int[layerCount];
        seek(color, layerOffset, layerCount * 4);
        for (int index = 0; index < layerCount; index++) {
            layerGlyphs[index] = Short.toUnsignedInt(color.getShort());
            layerPalettes[index] = Short.toUnsignedInt(color.getShort());
        }
        int cpalVersion = Short.toUnsignedInt(palette.getShort());
        if (cpalVersion > 1) {
            throw new IllegalArgumentException("Unsupported CPAL version " + cpalVersion);
        }
        int entries = Short.toUnsignedInt(palette.getShort());
        int palettes = Short.toUnsignedInt(palette.getShort());
        int colorCount = Short.toUnsignedInt(palette.getShort());
        int colorOffset = palette.getInt();
        if (palette.remaining() < palettes * 2) {
            throw new IllegalArgumentException("CPAL palette indices are truncated");
        }
        int[] paletteStarts = new int[palettes];
        for (int index = 0; index < palettes; index++) {
            paletteStarts[index] = Short.toUnsignedInt(palette.getShort());
        }
        seek(palette, colorOffset, colorCount * 4);
        PaletteColor[] colors = new PaletteColor[colorCount];
        for (int index = 0; index < colorCount; index++) {
            int blue = palette.get() & 0xFF;
            int green = palette.get() & 0xFF;
            int red = palette.get() & 0xFF;
            int alpha = palette.get() & 0xFF;
            colors[index] = new PaletteColor(red, green, blue, alpha);
        }
        return new ColrCpal(
                baseGlyphs,
                firstLayers,
                layerCounts,
                layerGlyphs,
                layerPalettes,
                colors,
                paletteStarts,
                entries
        );
    }

    /// Returns the COLR v0 layers for `glyphId` in `palette`.
    ///
    /// @param glyphId the base glyph
    /// @param palette the palette index
    /// @return the layers, empty when the glyph is not a color base
    @Unmodifiable List<ColorLayer> layers(int glyphId, int palette) {
        int base = Arrays.binarySearch(baseGlyphs, glyphId);
        if (base < 0) {
            return List.of();
        }
        int first = firstLayers[base];
        int count = layerCounts[base];
        if (first < 0 || count < 0 || first + count > layerGlyphs.length) {
            throw new IllegalArgumentException("COLR layer range is out of bounds");
        }
        ColorLayer[] layers = new ColorLayer[count];
        for (int index = 0; index < count; index++) {
            int paletteIndex = layerPalettes[first + index];
            @Nullable PaletteColor color = colorAt(palette, paletteIndex);
            layers[index] = new ColorLayer(layerGlyphs[first + index], paletteIndex, color);
        }
        return Collections.unmodifiableList(Arrays.asList(layers));
    }

    /// Returns one palette color, or `null` for the foreground sentinel.
    ///
    /// @param palette the palette index
    /// @param entry the entry, or [`PaletteColor#FOREGROUND`]
    /// @return the color
    @Nullable PaletteColor colorAt(int palette, int entry) {
        if (entry == PaletteColor.FOREGROUND) {
            return null;
        }
        if (palette < 0 || palette >= paletteStarts.length) {
            throw new IllegalArgumentException("Unknown palette " + palette);
        }
        if (entry < 0 || entry >= entriesPerPalette) {
            throw new IllegalArgumentException("Unknown palette entry " + entry);
        }
        int record = paletteStarts[palette] + entry;
        if (record < 0 || record >= colors.length) {
            throw new IllegalArgumentException("CPAL color record is out of range");
        }
        return colors[record];
    }

    /// Positions `buffer` at `offset` and requires `length` remaining bytes.
    private static void seek(ByteBuffer buffer, int offset, int length) {
        if (offset < 0 || length < 0 || (long) offset + (long) length > buffer.capacity()) {
            throw new IllegalArgumentException("COLR/CPAL slice is out of range");
        }
        buffer.clear();
        buffer.position(offset);
        buffer.limit(offset + length);
    }
}
