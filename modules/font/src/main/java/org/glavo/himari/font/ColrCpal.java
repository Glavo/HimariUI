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
/// Version-1 support flattens `PaintColrLayers`, `PaintGlyph`, `PaintSolid`, `PaintVarSolid`,
/// wrap paints 12–31 (`PaintTransform` through `PaintSkewAroundCenter`, including
/// `PaintVarTransform`, `PaintVarRotate`, and `PaintVarSkew`), `PaintComposite`,
/// `PaintLinearGradient`, `PaintRadialGradient`, `PaintSweepGradient`, and the variable
/// gradient forms 5/7/9 (first color-stop palette), plus `PaintColrGlyph`, into the same
/// layer list used by v0. `PaintVarSolid` and the first `VarColorLine` stop may apply an
/// 8-bit ItemVariationStore delta to the palette index. Variable wrap paints unwrap to the
/// nested paint the same way as their non-variable forms. Other paint formats are ignored. A missing table pair yields no color
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
            0,
            new int[0],
            new int[0],
            new int[0],
            0.0f,
            0.0f,
            0.0f,
            new byte[0]
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

    /// ItemVariationStore inner index for each flattened layer, or `-1`.
    private final int[] layerVarInners;

    /// Base `PaintVarTranslate` X offsets for each flattened layer.
    private final int[] layerTranslateXs;

    /// ItemVariationStore inner index for each translate X, or `-1`.
    private final int[] layerTranslateInners;

    /// First-stable variation-region start.
    private final float regionStart;

    /// First-stable variation-region peak.
    private final float regionPeak;

    /// First-stable variation-region end.
    private final float regionEnd;

    /// One 8-bit palette-index delta per inner index.
    private final byte[] varDeltas;

    /// Creates a parsed table pair.
    private ColrCpal(
            int[] baseGlyphs,
            int[] firstLayers,
            int[] layerCounts,
            int[] layerGlyphs,
            int[] layerPalettes,
            PaletteColor[] colors,
            int[] paletteStarts,
            int entriesPerPalette,
            int[] layerVarInners,
            int[] layerTranslateXs,
            int[] layerTranslateInners,
            float regionStart,
            float regionPeak,
            float regionEnd,
            byte[] varDeltas
    ) {
        this.baseGlyphs = baseGlyphs;
        this.firstLayers = firstLayers;
        this.layerCounts = layerCounts;
        this.layerGlyphs = layerGlyphs;
        this.layerPalettes = layerPalettes;
        this.colors = colors;
        this.paletteStarts = paletteStarts;
        this.entriesPerPalette = entriesPerPalette;
        this.layerVarInners = layerVarInners;
        this.layerTranslateXs = layerTranslateXs;
        this.layerTranslateInners = layerTranslateInners;
        this.regionStart = regionStart;
        this.regionPeak = regionPeak;
        this.regionEnd = regionEnd;
        this.varDeltas = varDeltas;
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
        int[] layerVarInners = new int[0];
        int[] layerTranslateXs = new int[0];
        int[] layerTranslateInners = new int[0];
        float regionStart = 0.0f;
        float regionPeak = 0.0f;
        float regionEnd = 0.0f;
        byte[] varDeltas = new byte[0];
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
            int storeOffset = 0;
            if (color.remaining() >= 12) {
                color.getInt();
                color.getInt();
                storeOffset = color.getInt();
            }
            V1Layers flattened = flattenV1(color, baseGlyphList, layerList);
            baseGlyphs = flattened.baseGlyphs;
            firstLayers = flattened.firstLayers;
            layerCounts = flattened.layerCounts;
            layerGlyphs = flattened.layerGlyphs;
            layerPalettes = flattened.layerPalettes;
            layerVarInners = flattened.varInners;
            layerTranslateXs = flattened.translateXs;
            layerTranslateInners = flattened.translateInners;
            if (storeOffset > 0) {
                ItemStore store = readItemStore(color, storeOffset);
                regionStart = store.start;
                regionPeak = store.peak;
                regionEnd = store.end;
                varDeltas = store.deltas;
            }
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
                entries,
                layerVarInners,
                layerTranslateXs,
                layerTranslateInners,
                regionStart,
                regionPeak,
                regionEnd,
                varDeltas
        );
    }

    /// Flattens a first-stable COLR v1 paint subset into v0-style layer arrays.
    private static V1Layers flattenV1(ByteBuffer color, int baseGlyphList, int layerList) {
        if (baseGlyphList <= 0 || baseGlyphList + 4 > color.capacity()) {
            return new V1Layers(new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0]);
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
        ArrayList<Integer> varInners = new ArrayList<>();
        ArrayList<Integer> translateXs = new ArrayList<>();
        ArrayList<Integer> translateInners = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            firstLayers[index] = glyphs.size();
            collectPaint(
                    color,
                    paints[index],
                    layerList,
                    baseGlyphs,
                    paints,
                    glyphs,
                    palettes,
                    varInners,
                    translateXs,
                    translateInners,
                    0,
                    -1,
                    0
            );
            layerCounts[index] = glyphs.size() - firstLayers[index];
        }
        int[] layerGlyphs = new int[glyphs.size()];
        int[] layerPalettes = new int[palettes.size()];
        int[] layerVars = new int[varInners.size()];
        int[] layerXs = new int[translateXs.size()];
        int[] layerXInners = new int[translateInners.size()];
        for (int index = 0; index < glyphs.size(); index++) {
            layerGlyphs[index] = glyphs.get(index);
            layerPalettes[index] = palettes.get(index);
            layerVars[index] = varInners.get(index);
            layerXs[index] = translateXs.get(index);
            layerXInners[index] = translateInners.get(index);
        }
        return new V1Layers(
                baseGlyphs, firstLayers, layerCounts, layerGlyphs, layerPalettes, layerVars, layerXs, layerXInners
        );
    }

    /// Collects first-stable paint nodes into glyph/palette pairs.
    private static void collectPaint(
            ByteBuffer color,
            int paint,
            int layerList,
            int[] baseGlyphs,
            int[] paints,
            ArrayList<Integer> glyphs,
            ArrayList<Integer> palettes,
            ArrayList<Integer> varInners,
            ArrayList<Integer> translateXs,
            ArrayList<Integer> translateInners,
            int wrapX,
            int wrapInner,
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
                    collectPaint(
                            color,
                            child,
                            layerList,
                            baseGlyphs,
                            paints,
                            glyphs,
                            palettes,
                            varInners,
                            translateXs,
                            translateInners,
                            wrapX,
                            wrapInner,
                            depth + 1
                    );
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
            int palette = fillPalette(color, child);
            if (palette >= 0) {
                glyphs.add(glyph);
                palettes.add(palette);
                varInners.add(varInnerOf(color, child));
                translateXs.add(wrapX);
                translateInners.add(wrapInner);
            }
            return;
        }
        if (format == 11) {
            if (color.remaining() < 2) {
                return;
            }
            int nested = paintOf(baseGlyphs, paints, Short.toUnsignedInt(color.getShort()));
            if (nested > 0) {
                collectPaint(
                        color,
                        nested,
                        layerList,
                        baseGlyphs,
                        paints,
                        glyphs,
                        palettes,
                        varInners,
                        translateXs,
                        translateInners,
                        wrapX,
                        wrapInner,
                        depth + 1
                );
            }
            return;
        }
        if (format == 15) {
            if (color.remaining() < 11) {
                return;
            }
            int child = paint + offset24(color);
            int dx = color.getShort();
            color.getShort();
            int inner = varIndex(color.getInt());
            collectPaint(
                    color,
                    child,
                    layerList,
                    baseGlyphs,
                    paints,
                    glyphs,
                    palettes,
                    varInners,
                    translateXs,
                    translateInners,
                    dx,
                    inner,
                    depth + 1
            );
            return;
        }
        if (format >= 12 && format <= 31) {
            if (color.remaining() < 3) {
                return;
            }
            int child = paint + offset24(color);
            collectPaint(
                    color,
                    child,
                    layerList,
                    baseGlyphs,
                    paints,
                    glyphs,
                    palettes,
                    varInners,
                    translateXs,
                    translateInners,
                    wrapX,
                    wrapInner,
                    depth + 1
            );
            return;
        }
        if (format == 32) {
            if (color.remaining() < 7) {
                return;
            }
            int source = paint + offset24(color);
            color.get();
            int backdrop = paint + offset24(color);
            collectPaint(
                    color,
                    backdrop,
                    layerList,
                    baseGlyphs,
                    paints,
                    glyphs,
                    palettes,
                    varInners,
                    translateXs,
                    translateInners,
                    wrapX,
                    wrapInner,
                    depth + 1
            );
            collectPaint(
                    color,
                    source,
                    layerList,
                    baseGlyphs,
                    paints,
                    glyphs,
                    palettes,
                    varInners,
                    translateXs,
                    translateInners,
                    wrapX,
                    wrapInner,
                    depth + 1
            );
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

    /// Returns the palette of a `PaintSolid` or the first gradient color stop, or `-1`.
    private static int fillPalette(ByteBuffer color, int paint) {
        int current = paint;
        for (int depth = 0; depth < 8; depth++) {
            int solid = solidPalette(color, current);
            if (solid >= 0) {
                return solid;
            }
            int gradient = gradientPalette(color, current);
            if (gradient >= 0) {
                return gradient;
            }
            if (current < 0 || current + 1 > color.capacity()) {
                return -1;
            }
            color.clear();
            color.position(current);
            int format = color.get() & 0xFF;
            if (format < 12 || format > 31 || color.remaining() < 3) {
                return -1;
            }
            current = current + offset24(color);
        }
        return -1;
    }

    /// Returns the palette index of a `PaintSolid`, or `-1`.
    private static int solidPalette(ByteBuffer color, int paint) {
        if (paint < 0 || paint + 3 > color.capacity()) {
            return -1;
        }
        color.clear();
        color.position(paint);
        int format = color.get() & 0xFF;
        if ((format != 2 && format != 3) || color.remaining() < 2) {
            return -1;
        }
        return Short.toUnsignedInt(color.getShort());
    }

    /// Returns the ItemVariationStore inner index of a `PaintVarSolid` or variable gradient, or `-1`.
    private static int varInnerOf(ByteBuffer color, int paint) {
        if (paint < 0 || paint + 1 > color.capacity()) {
            return -1;
        }
        color.clear();
        color.position(paint);
        int format = color.get() & 0xFF;
        if (format == 3) {
            if (color.remaining() < 8) {
                return -1;
            }
            color.getShort();
            color.getShort();
            return varIndex(color.getInt());
        }
        if (format != 5 && format != 7 && format != 9) {
            return -1;
        }
        if (color.remaining() < 3) {
            return -1;
        }
        int colorLine = paint + offset24(color);
        if (colorLine < 0 || colorLine + 13 > color.capacity()) {
            return -1;
        }
        color.clear();
        color.position(colorLine);
        color.get();
        int stops = Short.toUnsignedInt(color.getShort());
        if (stops < 1 || color.remaining() < 10) {
            return -1;
        }
        color.getShort();
        color.getShort();
        color.getShort();
        return varIndex(color.getInt());
    }

    /// Returns a 16-bit inner index, or `-1` when `varIndexBase` is unused.
    private static int varIndex(int varIndexBase) {
        return varIndexBase == -1 ? -1 : varIndexBase & 0xFFFF;
    }

    /// Returns the first color-stop palette of a linear, radial, or sweep gradient, or `-1`.
    private static int gradientPalette(ByteBuffer color, int paint) {
        if (paint < 0 || paint + 4 > color.capacity()) {
            return -1;
        }
        color.clear();
        color.position(paint);
        int format = color.get() & 0xFF;
        if (format < 4 || format > 9 || color.remaining() < 3) {
            return -1;
        }
        int colorLine = paint + offset24(color);
        if (colorLine < 0 || colorLine + 3 > color.capacity()) {
            return -1;
        }
        color.clear();
        color.position(colorLine);
        color.get();
        int stops = Short.toUnsignedInt(color.getShort());
        if (stops < 1 || color.remaining() < 6) {
            return -1;
        }
        color.getShort();
        return Short.toUnsignedInt(color.getShort());
    }

    /// Returns the paint offset of `glyphId` in the BaseGlyphList, or `0`.
    private static int paintOf(int[] baseGlyphs, int[] paints, int glyphId) {
        int index = Arrays.binarySearch(baseGlyphs, glyphId);
        return index >= 0 ? paints[index] : 0;
    }

    /// Parses a first-stable one-region 8-bit ItemVariationStore, or an empty store.
    private static ItemStore readItemStore(ByteBuffer color, int storeOffset) {
        if (storeOffset < 0 || storeOffset + 12 > color.capacity()) {
            return new ItemStore(0.0f, 0.0f, 0.0f, new byte[0]);
        }
        color.clear();
        color.position(storeOffset);
        int format = Short.toUnsignedInt(color.getShort());
        int regionListOffset = color.getInt();
        int dataCount = Short.toUnsignedInt(color.getShort());
        if (format != 1 || dataCount < 1 || color.remaining() < 4) {
            return new ItemStore(0.0f, 0.0f, 0.0f, new byte[0]);
        }
        int dataOffset = color.getInt();
        int regionAbs = storeOffset + regionListOffset;
        int dataAbs = storeOffset + dataOffset;
        if (regionAbs < 0 || dataAbs < 0 || regionAbs + 10 > color.capacity()) {
            return new ItemStore(0.0f, 0.0f, 0.0f, new byte[0]);
        }
        color.clear();
        color.position(regionAbs);
        int declaredAxes = Short.toUnsignedInt(color.getShort());
        int regionCount = Short.toUnsignedInt(color.getShort());
        if (declaredAxes < 1 || regionCount < 1 || color.remaining() < 6) {
            return new ItemStore(0.0f, 0.0f, 0.0f, new byte[0]);
        }
        float start = color.getShort() / 16384.0f;
        float peak = color.getShort() / 16384.0f;
        float end = color.getShort() / 16384.0f;
        if (dataAbs + 8 > color.capacity()) {
            return new ItemStore(0.0f, 0.0f, 0.0f, new byte[0]);
        }
        color.clear();
        color.position(dataAbs);
        int itemCount = Short.toUnsignedInt(color.getShort());
        int wordDeltaCount = Short.toUnsignedInt(color.getShort());
        int regionIndexCount = Short.toUnsignedInt(color.getShort());
        if (itemCount < 1 || (wordDeltaCount & 0x7FFF) != 0 || regionIndexCount != 1
                || color.remaining() < 2 + itemCount) {
            return new ItemStore(0.0f, 0.0f, 0.0f, new byte[0]);
        }
        color.getShort();
        byte[] deltas = new byte[itemCount];
        color.get(deltas);
        return new ItemStore(start, peak, end, deltas);
    }

    /// Returns the scaled palette-index delta for `inner`.
    private int varDelta(int inner, float[] normalized) {
        if (inner < 0 || inner >= varDeltas.length || regionPeak == 0.0f || normalized.length == 0) {
            return 0;
        }
        float coord = normalized[0];
        if (coord < regionStart || coord > regionEnd) {
            return 0;
        }
        float scalar;
        if (coord == regionPeak) {
            scalar = 1.0f;
        } else if (coord < regionPeak) {
            float span = regionPeak - regionStart;
            scalar = span == 0.0f ? 0.0f : (coord - regionStart) / span;
        } else {
            float span = regionEnd - regionPeak;
            scalar = span == 0.0f ? 0.0f : (regionEnd - coord) / span;
        }
        return Math.round(scalar * varDeltas[inner]);
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
            int[] layerPalettes,
            int[] varInners,
            int[] translateXs,
            int[] translateInners
    ) {
    }

    /// One-axis ItemVariationStore subset.
    ///
    /// @param start region start
    /// @param peak region peak
    /// @param end region end
    /// @param deltas 8-bit inner deltas
    private record ItemStore(float start, float peak, float end, byte[] deltas) {
    }

    /// Returns the COLR layers for `glyphId` in `palette` at the default instance.
    ///
    /// @param glyphId the base glyph
    /// @param palette the palette index
    /// @return the layers, empty when the glyph is not a color base
    @Unmodifiable List<ColorLayer> layers(int glyphId, int palette) {
        return layers(glyphId, palette, new float[0]);
    }

    /// Returns the COLR layers for `glyphId` in `palette` at `normalized` coordinates.
    ///
    /// @param glyphId the base glyph
    /// @param palette the palette index
    /// @param normalized avar-mapped axis coordinates
    /// @return the layers, empty when the glyph is not a color base
    @Unmodifiable List<ColorLayer> layers(int glyphId, int palette, float[] normalized) {
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
            int slot = first + index;
            int paletteIndex = layerPalettes[slot];
            if (slot < layerVarInners.length) {
                paletteIndex += varDelta(layerVarInners[slot], normalized);
                if (paletteIndex < 0) {
                    paletteIndex = 0;
                } else if (paletteIndex != PaletteColor.FOREGROUND && paletteIndex >= entriesPerPalette) {
                    paletteIndex = entriesPerPalette - 1;
                }
            }
            @Nullable PaletteColor color = colorAt(palette, paletteIndex);
            int translateX = 0;
            if (slot < layerTranslateXs.length) {
                translateX = layerTranslateXs[slot];
                if (slot < layerTranslateInners.length) {
                    translateX += varDelta(layerTranslateInners[slot], normalized);
                }
            }
            layers[index] = new ColorLayer(layerGlyphs[slot], paletteIndex, color, translateX);
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
