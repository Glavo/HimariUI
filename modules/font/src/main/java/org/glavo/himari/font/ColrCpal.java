package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// Parses COLR v0 base/layer records, a first-stable COLR v1 paint subset, and CPAL palettes.
///
/// Version-1 support flattens `PaintColrLayers`, `PaintGlyph`, and `PaintSolid` into the same
/// layer list used by v0. Other paint formats are ignored. A missing table pair yields no color
/// layers. Palette index [`PaletteColor#FOREGROUND`] leaves [`ColorLayer#color()`] `null`.
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
    /// @return the parsed pair; empty when either table is missing
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
        int[] baseGlyphs;
        int[] firstLayers;
        int[] layerCounts;
        int[] layerGlyphs;
        int[] layerPalettes;
        if (colrVersion == 0) {
            int baseCount = Short.toUnsignedInt(color.getShort());
            int baseOffset = color.getInt();
            int layerOffset = color.getInt();
            int layerCount = Short.toUnsignedInt(color.getShort());
            baseGlyphs = new int[baseCount];
            firstLayers = new int[baseCount];
            layerCounts = new int[baseCount];
            seek(color, baseOffset, baseCount * 6);
            for (int index = 0; index < baseCount; index++) {
                baseGlyphs[index] = Short.toUnsignedInt(color.getShort());
                firstLayers[index] = Short.toUnsignedInt(color.getShort());
                layerCounts[index] = Short.toUnsignedInt(color.getShort());
                if (index > 0 && baseGlyphs[index] <= baseGlyphs[index - 1]) {
                    throw new IllegalArgumentException("COLR base glyph records must be sorted");
                }
            }
            layerGlyphs = new int[layerCount];
            layerPalettes = new int[layerCount];
            seek(color, layerOffset, layerCount * 4);
            for (int index = 0; index < layerCount; index++) {
                layerGlyphs[index] = Short.toUnsignedInt(color.getShort());
                layerPalettes[index] = Short.toUnsignedInt(color.getShort());
            }
        } else if (colrVersion == 1) {
            if (color.remaining() < 32) {
                throw new IllegalArgumentException("COLR v1 header is truncated");
            }
            color.getShort();
            color.getInt();
            color.getInt();
            color.getShort();
            int baseGlyphList = color.getInt();
            int layerList = color.getInt();
            V1Layers flattened = flattenV1(color, baseGlyphList, layerList);
            baseGlyphs = flattened.baseGlyphs;
            firstLayers = flattened.firstLayers;
            layerCounts = flattened.layerCounts;
            layerGlyphs = flattened.layerGlyphs;
            layerPalettes = flattened.layerPalettes;
        } else {
            return EMPTY;
        }
        palette.clear();
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

    /// Flattens a first-stable COLR v1 paint subset into v0-style layer arrays.
    private static V1Layers flattenV1(ByteBuffer color, int baseGlyphList, int layerList) {
        if (baseGlyphList <= 0 || baseGlyphList + 4 > color.capacity()) {
            return new V1Layers(new int[0], new int[0], new int[0], new int[0], new int[0]);
        }
        color.clear();
        color.position(baseGlyphList);
        int count = color.getInt();
        if (count < 0 || count > 256 || baseGlyphList + 4 + count * 5 > color.capacity()) {
            throw new IllegalArgumentException("COLR v1 BaseGlyphList is truncated");
        }
        int[] baseGlyphs = new int[count];
        int[] paints = new int[count];
        for (int index = 0; index < count; index++) {
            baseGlyphs[index] = Short.toUnsignedInt(color.getShort());
            paints[index] = baseGlyphList + offset24(color);
            if (index > 0 && baseGlyphs[index] <= baseGlyphs[index - 1]) {
                throw new IllegalArgumentException("COLR v1 base glyph records must be sorted");
            }
        }
        int[] firstLayers = new int[count];
        int[] layerCounts = new int[count];
        ArrayList<Integer> glyphs = new ArrayList<>();
        ArrayList<Integer> palettes = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            firstLayers[index] = glyphs.size();
            collectPaint(color, paints[index], layerList, glyphs, palettes, 0);
            layerCounts[index] = glyphs.size() - firstLayers[index];
        }
        int[] layerGlyphs = new int[glyphs.size()];
        int[] layerPalettes = new int[palettes.size()];
        for (int index = 0; index < glyphs.size(); index++) {
            layerGlyphs[index] = glyphs.get(index);
            layerPalettes[index] = palettes.get(index);
        }
        return new V1Layers(baseGlyphs, firstLayers, layerCounts, layerGlyphs, layerPalettes);
    }

    /// Collects first-stable paint nodes into glyph/palette pairs.
    private static void collectPaint(
            ByteBuffer color,
            int paint,
            int layerList,
            ArrayList<Integer> glyphs,
            ArrayList<Integer> palettes,
            int depth
    ) {
        if (depth > 8 || paint < 0 || paint + 1 > color.capacity()) {
            return;
        }
        color.clear();
        color.position(paint);
        int format = color.get() & 0xFF;
        if (format == 1) {
            if (color.remaining() < 5 || layerList <= 0) {
                return;
            }
            int numLayers = color.get() & 0xFF;
            int first = color.getInt();
            for (int index = 0; index < numLayers; index++) {
                int child = layerPaint(color, layerList, first + index);
                if (child > 0) {
                    collectPaint(color, child, layerList, glyphs, palettes, depth + 1);
                }
            }
            return;
        }
        if (format == 10) {
            if (color.remaining() < 5) {
                return;
            }
            int child = paint + offset24(color);
            int glyph = Short.toUnsignedInt(color.getShort());
            int palette = solidPalette(color, child);
            if (palette >= 0) {
                glyphs.add(glyph);
                palettes.add(palette);
            }
        }
    }

    /// Returns the paint offset at `layerIndex` of the LayerList, or `0`.
    private static int layerPaint(ByteBuffer color, int layerList, int layerIndex) {
        if (layerList + 4 > color.capacity()) {
            return 0;
        }
        color.clear();
        color.position(layerList);
        int numLayers = color.getInt();
        if (layerIndex < 0 || layerIndex >= numLayers || color.remaining() < (layerIndex + 1) * 3) {
            return 0;
        }
        color.position(layerList + 4 + layerIndex * 3);
        return layerList + offset24(color);
    }

    /// Returns the palette index of a `PaintSolid`, or `-1`.
    private static int solidPalette(ByteBuffer color, int paint) {
        if (paint < 0 || paint + 3 > color.capacity()) {
            return -1;
        }
        color.clear();
        color.position(paint);
        if ((color.get() & 0xFF) != 2 || color.remaining() < 2) {
            return -1;
        }
        return Short.toUnsignedInt(color.getShort());
    }

    /// Reads a 24-bit offset.
    private static int offset24(ByteBuffer color) {
        int high = color.get() & 0xFF;
        int mid = color.get() & 0xFF;
        int low = color.get() & 0xFF;
        return (high << 16) | (mid << 8) | low;
    }

    /// Stores flattened COLR v1 arrays.
    private record V1Layers(
            int[] baseGlyphs,
            int[] firstLayers,
            int[] layerCounts,
            int[] layerGlyphs,
            int[] layerPalettes
    ) {
    }

    /// Returns the COLR layers for `glyphId` in `palette`.
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
