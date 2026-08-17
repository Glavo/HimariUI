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
/// 8-bit ItemVariationStore delta to the palette index. `PaintVarTranslate` may apply a store
/// delta to `dx` and `dy`, `PaintVarScale` to `scaleX`/`scaleY`, `PaintVarScaleUniform` to both
/// scale axes, `PaintVarRotate` to the angle, `PaintVarSkew` to both skew angles,
/// `PaintVarTransform` to `xx`/`yx`/`xy`/`yy`/`dx`/`dy`, around-center paints to `centerX` and
/// `centerY`, `PaintVarScaleAroundCenter` to scale plus center, and
/// `PaintVarSkewAroundCenter` to skew plus center.
/// Variable wrap
/// paints unwrap to the nested paint the same way as their non-variable forms. Other paint
/// formats are ignored. A missing table pair yields no color layers. Palette index
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
            0,
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
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

    /// Base `PaintVarScale` X factors as `F2DOT14` for each flattened layer.
    private final int[] layerScaleXs;

    /// ItemVariationStore inner index for each scale X, or `-1`.
    private final int[] layerScaleInners;

    /// Base `PaintVarRotate` angles as `F2DOT14` for each flattened layer.
    private final int[] layerRotates;

    /// ItemVariationStore inner index for each rotate angle, or `-1`.
    private final int[] layerRotateInners;

    /// Base `PaintVarTranslate` Y offsets for each flattened layer.
    private final int[] layerTranslateYs;

    /// ItemVariationStore inner index for each translate Y, or `-1`.
    private final int[] layerTranslateYInners;

    /// Base `PaintVarSkew` X angles as `F2DOT14` for each flattened layer.
    private final int[] layerSkewXs;

    /// ItemVariationStore inner index for each skew X, or `-1`.
    private final int[] layerSkewInners;

    /// Base `PaintVarScale` Y factors as `F2DOT14` for each flattened layer.
    private final int[] layerScaleYs;

    /// ItemVariationStore inner index for each scale Y, or `-1`.
    private final int[] layerScaleYInners;

    /// Base `PaintVarSkew` Y angles as `F2DOT14` for each flattened layer.
    private final int[] layerSkewYs;

    /// ItemVariationStore inner index for each skew Y, or `-1`.
    private final int[] layerSkewYInners;

    /// Base `PaintVarTransform` `xx` values as `16.16` for each flattened layer.
    private final int[] layerTransformXxs;

    /// ItemVariationStore inner index for each transform `xx`, or `-1`.
    private final int[] layerTransformInners;

    /// Base around-center X offsets for each flattened layer.
    private final int[] layerCenterXs;

    /// ItemVariationStore inner index for each center X, or `-1`.
    private final int[] layerCenterInners;

    /// Base `PaintVarTransform` `yx` values as `16.16` for each flattened layer.
    private final int[] layerTransformYxs;

    /// ItemVariationStore inner index for each transform `yx`, or `-1`.
    private final int[] layerTransformYxInners;

    /// Base around-center Y offsets for each flattened layer.
    private final int[] layerCenterYs;

    /// ItemVariationStore inner index for each center Y, or `-1`.
    private final int[] layerCenterYInners;

    /// Base `PaintVarTransform` `xy` values as `16.16` for each flattened layer.
    private final int[] layerTransformXys;

    /// ItemVariationStore inner index for each transform `xy`, or `-1`.
    private final int[] layerTransformXyInners;

    /// Base `PaintVarTransform` `yy` values as `16.16` for each flattened layer.
    private final int[] layerTransformYys;

    /// ItemVariationStore inner index for each transform `yy`, or `-1`.
    private final int[] layerTransformYyInners;

    /// Base `PaintVarTransform` `dx` values as `16.16` for each flattened layer.
    private final int[] layerTransformDxs;

    /// ItemVariationStore inner index for each transform `dx`, or `-1`.
    private final int[] layerTransformDxInners;

    /// Base `PaintVarTransform` `dy` values as `16.16` for each flattened layer.
    private final int[] layerTransformDys;

    /// ItemVariationStore inner index for each transform `dy`, or `-1`.
    private final int[] layerTransformDyInners;

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
            int[] layerScaleXs,
            int[] layerScaleInners,
            int[] layerRotates,
            int[] layerRotateInners,
            int[] layerTranslateYs,
            int[] layerTranslateYInners,
            int[] layerSkewXs,
            int[] layerSkewInners,
            int[] layerScaleYs,
            int[] layerScaleYInners,
            int[] layerSkewYs,
            int[] layerSkewYInners,
            int[] layerTransformXxs,
            int[] layerTransformInners,
            int[] layerCenterXs,
            int[] layerCenterInners,
            int[] layerTransformYxs,
            int[] layerTransformYxInners,
            int[] layerCenterYs,
            int[] layerCenterYInners,
            int[] layerTransformXys,
            int[] layerTransformXyInners,
            int[] layerTransformYys,
            int[] layerTransformYyInners,
            int[] layerTransformDxs,
            int[] layerTransformDxInners,
            int[] layerTransformDys,
            int[] layerTransformDyInners,
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
        this.layerScaleXs = layerScaleXs;
        this.layerScaleInners = layerScaleInners;
        this.layerRotates = layerRotates;
        this.layerRotateInners = layerRotateInners;
        this.layerTranslateYs = layerTranslateYs;
        this.layerTranslateYInners = layerTranslateYInners;
        this.layerSkewXs = layerSkewXs;
        this.layerSkewInners = layerSkewInners;
        this.layerScaleYs = layerScaleYs;
        this.layerScaleYInners = layerScaleYInners;
        this.layerSkewYs = layerSkewYs;
        this.layerSkewYInners = layerSkewYInners;
        this.layerTransformXxs = layerTransformXxs;
        this.layerTransformInners = layerTransformInners;
        this.layerCenterXs = layerCenterXs;
        this.layerCenterInners = layerCenterInners;
        this.layerTransformYxs = layerTransformYxs;
        this.layerTransformYxInners = layerTransformYxInners;
        this.layerCenterYs = layerCenterYs;
        this.layerCenterYInners = layerCenterYInners;
        this.layerTransformXys = layerTransformXys;
        this.layerTransformXyInners = layerTransformXyInners;
        this.layerTransformYys = layerTransformYys;
        this.layerTransformYyInners = layerTransformYyInners;
        this.layerTransformDxs = layerTransformDxs;
        this.layerTransformDxInners = layerTransformDxInners;
        this.layerTransformDys = layerTransformDys;
        this.layerTransformDyInners = layerTransformDyInners;
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
        int[] layerScaleXs = new int[0];
        int[] layerScaleInners = new int[0];
        int[] layerRotates = new int[0];
        int[] layerRotateInners = new int[0];
        int[] layerTranslateYs = new int[0];
        int[] layerTranslateYInners = new int[0];
        int[] layerSkewXs = new int[0];
        int[] layerSkewInners = new int[0];
        int[] layerScaleYs = new int[0];
        int[] layerScaleYInners = new int[0];
        int[] layerSkewYs = new int[0];
        int[] layerSkewYInners = new int[0];
        int[] layerTransformXxs = new int[0];
        int[] layerTransformInners = new int[0];
        int[] layerCenterXs = new int[0];
        int[] layerCenterInners = new int[0];
        int[] layerTransformYxs = new int[0];
        int[] layerTransformYxInners = new int[0];
        int[] layerCenterYs = new int[0];
        int[] layerCenterYInners = new int[0];
        int[] layerTransformXys = new int[0];
        int[] layerTransformXyInners = new int[0];
        int[] layerTransformYys = new int[0];
        int[] layerTransformYyInners = new int[0];
        int[] layerTransformDxs = new int[0];
        int[] layerTransformDxInners = new int[0];
        int[] layerTransformDys = new int[0];
        int[] layerTransformDyInners = new int[0];
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
            layerScaleXs = flattened.scaleXs;
            layerScaleInners = flattened.scaleInners;
            layerRotates = flattened.rotates;
            layerRotateInners = flattened.rotateInners;
            layerTranslateYs = flattened.translateYs;
            layerTranslateYInners = flattened.translateYInners;
            layerSkewXs = flattened.skewXs;
            layerSkewInners = flattened.skewInners;
            layerScaleYs = flattened.scaleYs;
            layerScaleYInners = flattened.scaleYInners;
            layerSkewYs = flattened.skewYs;
            layerSkewYInners = flattened.skewYInners;
            layerTransformXxs = flattened.transformXxs;
            layerTransformInners = flattened.transformInners;
            layerCenterXs = flattened.centerXs;
            layerCenterInners = flattened.centerInners;
            layerTransformYxs = flattened.transformYxs;
            layerTransformYxInners = flattened.transformYxInners;
            layerCenterYs = flattened.centerYs;
            layerCenterYInners = flattened.centerYInners;
            layerTransformXys = flattened.transformXys;
            layerTransformXyInners = flattened.transformXyInners;
            layerTransformYys = flattened.transformYys;
            layerTransformYyInners = flattened.transformYyInners;
            layerTransformDxs = flattened.transformDxs;
            layerTransformDxInners = flattened.transformDxInners;
            layerTransformDys = flattened.transformDys;
            layerTransformDyInners = flattened.transformDyInners;
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
                layerScaleXs,
                layerScaleInners,
                layerRotates,
                layerRotateInners,
                layerTranslateYs,
                layerTranslateYInners,
                layerSkewXs,
                layerSkewInners,
                layerScaleYs,
                layerScaleYInners,
                layerSkewYs,
                layerSkewYInners,
                layerTransformXxs,
                layerTransformInners,
                layerCenterXs,
                layerCenterInners,
                layerTransformYxs,
                layerTransformYxInners,
                layerCenterYs,
                layerCenterYInners,
                layerTransformXys,
                layerTransformXyInners,
                layerTransformYys,
                layerTransformYyInners,
                layerTransformDxs,
                layerTransformDxInners,
                layerTransformDys,
                layerTransformDyInners,
                regionStart,
                regionPeak,
                regionEnd,
                varDeltas
        );
    }

    /// Flattens a first-stable COLR v1 paint subset into v0-style layer arrays.
    private static V1Layers flattenV1(ByteBuffer color, int baseGlyphList, int layerList) {
        if (baseGlyphList <= 0 || baseGlyphList + 4 > color.capacity()) {
            return new V1Layers(
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0]
            );
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
        ArrayList<Integer> scaleXs = new ArrayList<>();
        ArrayList<Integer> scaleInners = new ArrayList<>();
        ArrayList<Integer> rotates = new ArrayList<>();
        ArrayList<Integer> rotateInners = new ArrayList<>();
        ArrayList<Integer> translateYs = new ArrayList<>();
        ArrayList<Integer> translateYInners = new ArrayList<>();
        ArrayList<Integer> skewXs = new ArrayList<>();
        ArrayList<Integer> skewInners = new ArrayList<>();
        ArrayList<Integer> scaleYs = new ArrayList<>();
        ArrayList<Integer> scaleYInners = new ArrayList<>();
        ArrayList<Integer> skewYs = new ArrayList<>();
        ArrayList<Integer> skewYInners = new ArrayList<>();
        ArrayList<Integer> transformXxs = new ArrayList<>();
        ArrayList<Integer> transformInners = new ArrayList<>();
        ArrayList<Integer> centerXs = new ArrayList<>();
        ArrayList<Integer> centerInners = new ArrayList<>();
        ArrayList<Integer> transformYxs = new ArrayList<>();
        ArrayList<Integer> transformYxInners = new ArrayList<>();
        ArrayList<Integer> centerYs = new ArrayList<>();
        ArrayList<Integer> centerYInners = new ArrayList<>();
        ArrayList<Integer> transformXys = new ArrayList<>();
        ArrayList<Integer> transformXyInners = new ArrayList<>();
        ArrayList<Integer> transformYys = new ArrayList<>();
        ArrayList<Integer> transformYyInners = new ArrayList<>();
        ArrayList<Integer> transformDxs = new ArrayList<>();
        ArrayList<Integer> transformDxInners = new ArrayList<>();
        ArrayList<Integer> transformDys = new ArrayList<>();
        ArrayList<Integer> transformDyInners = new ArrayList<>();
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
                    0,
                    -1,
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
        int[] layerScales = new int[scaleXs.size()];
        int[] layerScaleVars = new int[scaleInners.size()];
        int[] layerAngles = new int[rotates.size()];
        int[] layerAngleVars = new int[rotateInners.size()];
        int[] layerYs = new int[translateYs.size()];
        int[] layerYInners = new int[translateYInners.size()];
        int[] layerSkews = new int[skewXs.size()];
        int[] layerSkewVars = new int[skewInners.size()];
        int[] layerScaleYVals = new int[scaleYs.size()];
        int[] layerScaleYVars = new int[scaleYInners.size()];
        int[] layerSkewYVals = new int[skewYs.size()];
        int[] layerSkewYVars = new int[skewYInners.size()];
        int[] layerXx = new int[transformXxs.size()];
        int[] layerXxVars = new int[transformInners.size()];
        int[] layerCenters = new int[centerXs.size()];
        int[] layerCenterVars = new int[centerInners.size()];
        int[] layerYx = new int[transformYxs.size()];
        int[] layerYxVars = new int[transformYxInners.size()];
        int[] layerCenterYVals = new int[centerYs.size()];
        int[] layerCenterYVars = new int[centerYInners.size()];
        int[] layerXy = new int[transformXys.size()];
        int[] layerXyVars = new int[transformXyInners.size()];
        int[] layerYy = new int[transformYys.size()];
        int[] layerYyVars = new int[transformYyInners.size()];
        int[] layerDx = new int[transformDxs.size()];
        int[] layerDxVars = new int[transformDxInners.size()];
        int[] layerDy = new int[transformDys.size()];
        int[] layerDyVars = new int[transformDyInners.size()];
        for (int index = 0; index < glyphs.size(); index++) {
            layerGlyphs[index] = glyphs.get(index);
            layerPalettes[index] = palettes.get(index);
            layerVars[index] = varInners.get(index);
            layerXs[index] = translateXs.get(index);
            layerXInners[index] = translateInners.get(index);
            layerScales[index] = scaleXs.get(index);
            layerScaleVars[index] = scaleInners.get(index);
            layerAngles[index] = rotates.get(index);
            layerAngleVars[index] = rotateInners.get(index);
            layerYs[index] = translateYs.get(index);
            layerYInners[index] = translateYInners.get(index);
            layerSkews[index] = skewXs.get(index);
            layerSkewVars[index] = skewInners.get(index);
            layerScaleYVals[index] = scaleYs.get(index);
            layerScaleYVars[index] = scaleYInners.get(index);
            layerSkewYVals[index] = skewYs.get(index);
            layerSkewYVars[index] = skewYInners.get(index);
            layerXx[index] = transformXxs.get(index);
            layerXxVars[index] = transformInners.get(index);
            layerCenters[index] = centerXs.get(index);
            layerCenterVars[index] = centerInners.get(index);
            layerYx[index] = transformYxs.get(index);
            layerYxVars[index] = transformYxInners.get(index);
            layerCenterYVals[index] = centerYs.get(index);
            layerCenterYVars[index] = centerYInners.get(index);
            layerXy[index] = transformXys.get(index);
            layerXyVars[index] = transformXyInners.get(index);
            layerYy[index] = transformYys.get(index);
            layerYyVars[index] = transformYyInners.get(index);
            layerDx[index] = transformDxs.get(index);
            layerDxVars[index] = transformDxInners.get(index);
            layerDy[index] = transformDys.get(index);
            layerDyVars[index] = transformDyInners.get(index);
        }
        return new V1Layers(
                baseGlyphs,
                firstLayers,
                layerCounts,
                layerGlyphs,
                layerPalettes,
                layerVars,
                layerXs,
                layerXInners,
                layerScales,
                layerScaleVars,
                layerAngles,
                layerAngleVars,
                layerYs,
                layerYInners,
                layerSkews,
                layerSkewVars,
                layerScaleYVals,
                layerScaleYVars,
                layerSkewYVals,
                layerSkewYVars,
                layerXx,
                layerXxVars,
                layerCenters,
                layerCenterVars,
                layerYx,
                layerYxVars,
                layerCenterYVals,
                layerCenterYVars,
                layerXy,
                layerXyVars,
                layerYy,
                layerYyVars,
                layerDx,
                layerDxVars,
                layerDy,
                layerDyVars
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
            ArrayList<Integer> scaleXs,
            ArrayList<Integer> scaleInners,
            ArrayList<Integer> rotates,
            ArrayList<Integer> rotateInners,
            ArrayList<Integer> translateYs,
            ArrayList<Integer> translateYInners,
            ArrayList<Integer> skewXs,
            ArrayList<Integer> skewInners,
            ArrayList<Integer> scaleYs,
            ArrayList<Integer> scaleYInners,
            ArrayList<Integer> skewYs,
            ArrayList<Integer> skewYInners,
            ArrayList<Integer> transformXxs,
            ArrayList<Integer> transformInners,
            ArrayList<Integer> centerXs,
            ArrayList<Integer> centerInners,
            ArrayList<Integer> transformYxs,
            ArrayList<Integer> transformYxInners,
            ArrayList<Integer> centerYs,
            ArrayList<Integer> centerYInners,
            ArrayList<Integer> transformXys,
            ArrayList<Integer> transformXyInners,
            ArrayList<Integer> transformYys,
            ArrayList<Integer> transformYyInners,
            ArrayList<Integer> transformDxs,
            ArrayList<Integer> transformDxInners,
            ArrayList<Integer> transformDys,
            ArrayList<Integer> transformDyInners,
            int wrapX,
            int wrapInner,
            int wrapScale,
            int wrapScaleInner,
            int wrapRotate,
            int wrapRotateInner,
            int wrapY,
            int wrapYInner,
            int wrapSkew,
            int wrapSkewInner,
            int wrapScaleY,
            int wrapScaleYInner,
            int wrapSkewY,
            int wrapSkewYInner,
            int wrapTransformXx,
            int wrapTransformInner,
            int wrapCenterX,
            int wrapCenterInner,
            int wrapTransformYx,
            int wrapTransformYxInner,
            int wrapCenterY,
            int wrapCenterYInner,
            int wrapTransformXy,
            int wrapTransformXyInner,
            int wrapTransformYy,
            int wrapTransformYyInner,
            int wrapTransformDx,
            int wrapTransformDxInner,
            int wrapTransformDy,
            int wrapTransformDyInner,
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
                            scaleXs,
                            scaleInners,
                            rotates,
                            rotateInners,
                            translateYs,
                            translateYInners,
                            skewXs,
                            skewInners,
                            scaleYs,
                            scaleYInners,
                            skewYs,
                            skewYInners,
                            transformXxs,
                            transformInners,
                            centerXs,
                            centerInners,
                            transformYxs,
                            transformYxInners,
                            centerYs,
                            centerYInners,
                            transformXys,
                            transformXyInners,
                            transformYys,
                            transformYyInners,
                            transformDxs,
                            transformDxInners,
                            transformDys,
                            transformDyInners,
                            wrapX,
                            wrapInner,
                            wrapScale,
                            wrapScaleInner,
                            wrapRotate,
                            wrapRotateInner,
                            wrapY,
                            wrapYInner,
                            wrapSkew,
                            wrapSkewInner,
                            wrapScaleY,
                            wrapScaleYInner,
                            wrapSkewY,
                            wrapSkewYInner,
                            wrapTransformXx,
                            wrapTransformInner,
                            wrapCenterX,
                            wrapCenterInner,
                            wrapTransformYx,
                            wrapTransformYxInner,
                            wrapCenterY,
                            wrapCenterYInner,
                            wrapTransformXy,
                            wrapTransformXyInner,
                            wrapTransformYy,
                            wrapTransformYyInner,
                            wrapTransformDx,
                            wrapTransformDxInner,
                            wrapTransformDy,
                            wrapTransformDyInner,
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
                scaleXs.add(wrapScale);
                scaleInners.add(wrapScaleInner);
                rotates.add(wrapRotate);
                rotateInners.add(wrapRotateInner);
                translateYs.add(wrapY);
                translateYInners.add(wrapYInner);
                skewXs.add(wrapSkew);
                skewInners.add(wrapSkewInner);
                scaleYs.add(wrapScaleY);
                scaleYInners.add(wrapScaleYInner);
                skewYs.add(wrapSkewY);
                skewYInners.add(wrapSkewYInner);
                transformXxs.add(wrapTransformXx);
                transformInners.add(wrapTransformInner);
                centerXs.add(wrapCenterX);
                centerInners.add(wrapCenterInner);
                transformYxs.add(wrapTransformYx);
                transformYxInners.add(wrapTransformYxInner);
                centerYs.add(wrapCenterY);
                centerYInners.add(wrapCenterYInner);
                transformXys.add(wrapTransformXy);
                transformXyInners.add(wrapTransformXyInner);
                transformYys.add(wrapTransformYy);
                transformYyInners.add(wrapTransformYyInner);
                transformDxs.add(wrapTransformDx);
                transformDxInners.add(wrapTransformDxInner);
                transformDys.add(wrapTransformDy);
                transformDyInners.add(wrapTransformDyInner);
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
                        scaleXs,
                        scaleInners,
                        rotates,
                        rotateInners,
                        translateYs,
                        translateYInners,
                        skewXs,
                        skewInners,
                        scaleYs,
                        scaleYInners,
                        skewYs,
                        skewYInners,
                        transformXxs,
                        transformInners,
                        centerXs,
                        centerInners,
                        transformYxs,
                        transformYxInners,
                        centerYs,
                        centerYInners,
                        transformXys,
                        transformXyInners,
                        transformYys,
                        transformYyInners,
                        transformDxs,
                        transformDxInners,
                        transformDys,
                        transformDyInners,
                        wrapX,
                        wrapInner,
                        wrapScale,
                        wrapScaleInner,
                        wrapRotate,
                        wrapRotateInner,
                        wrapY,
                        wrapYInner,
                        wrapSkew,
                        wrapSkewInner,
                        wrapScaleY,
                        wrapScaleYInner,
                        wrapSkewY,
                        wrapSkewYInner,
                        wrapTransformXx,
                        wrapTransformInner,
                        wrapCenterX,
                        wrapCenterInner,
                        wrapTransformYx,
                        wrapTransformYxInner,
                        wrapCenterY,
                        wrapCenterYInner,
                        wrapTransformXy,
                        wrapTransformXyInner,
                        wrapTransformYy,
                        wrapTransformYyInner,
                        wrapTransformDx,
                        wrapTransformDxInner,
                        wrapTransformDy,
                        wrapTransformDyInner,
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
            int dy = color.getShort();
            int inner = varIndex(color.getInt());
            int dyInner = inner < 0 ? -1 : inner + 1;
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    dx,
                    inner,
                    wrapScale,
                    wrapScaleInner,
                    wrapRotate,
                    wrapRotateInner,
                    dy,
                    dyInner,
                    wrapSkew,
                    wrapSkewInner,
                    wrapScaleY,
                    wrapScaleYInner,
                    wrapSkewY,
                    wrapSkewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    wrapCenterX,
                    wrapCenterInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    wrapCenterY,
                    wrapCenterYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
                    depth + 1
            );
            return;
        }
        if (format == 17) {
            if (color.remaining() < 11) {
                return;
            }
            int child = paint + offset24(color);
            int scaleX = color.getShort();
            int scaleY = color.getShort();
            int inner = varIndex(color.getInt());
            int scaleYInner = inner < 0 ? -1 : inner + 1;
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    scaleX,
                    inner,
                    wrapRotate,
                    wrapRotateInner,
                    wrapY,
                    wrapYInner,
                    wrapSkew,
                    wrapSkewInner,
                    scaleY,
                    scaleYInner,
                    wrapSkewY,
                    wrapSkewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    wrapCenterX,
                    wrapCenterInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    wrapCenterY,
                    wrapCenterYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
                    depth + 1
            );
            return;
        }
        if (format == 25) {
            if (color.remaining() < 9) {
                return;
            }
            int child = paint + offset24(color);
            int angle = color.getShort();
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    wrapScale,
                    wrapScaleInner,
                    angle,
                    inner,
                    wrapY,
                    wrapYInner,
                    wrapSkew,
                    wrapSkewInner,
                    wrapScaleY,
                    wrapScaleYInner,
                    wrapSkewY,
                    wrapSkewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    wrapCenterX,
                    wrapCenterInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    wrapCenterY,
                    wrapCenterYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
                    depth + 1
            );
            return;
        }
        if (format == 29) {
            if (color.remaining() < 11) {
                return;
            }
            int child = paint + offset24(color);
            int skew = color.getShort();
            int skewY = color.getShort();
            int inner = varIndex(color.getInt());
            int skewYInner = inner < 0 ? -1 : inner + 1;
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    wrapScale,
                    wrapScaleInner,
                    wrapRotate,
                    wrapRotateInner,
                    wrapY,
                    wrapYInner,
                    skew,
                    inner,
                    wrapScaleY,
                    wrapScaleYInner,
                    skewY,
                    skewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    wrapCenterX,
                    wrapCenterInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    wrapCenterY,
                    wrapCenterYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
                    depth + 1
            );
            return;
        }
        if (format == 21) {
            if (color.remaining() < 9) {
                return;
            }
            int child = paint + offset24(color);
            int scale = color.getShort();
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    scale,
                    inner,
                    wrapRotate,
                    wrapRotateInner,
                    wrapY,
                    wrapYInner,
                    wrapSkew,
                    wrapSkewInner,
                    scale,
                    inner,
                    wrapSkewY,
                    wrapSkewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    wrapCenterX,
                    wrapCenterInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    wrapCenterY,
                    wrapCenterYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
                    depth + 1
            );
            return;
        }
        if (format == 19) {
            if (color.remaining() < 15) {
                return;
            }
            int child = paint + offset24(color);
            int scaleX = color.getShort();
            int scaleY = color.getShort();
            int centerX = color.getShort();
            int centerY = color.getShort();
            int inner = varIndex(color.getInt());
            int scaleYInner = inner < 0 ? -1 : inner + 1;
            int centerXInner = inner < 0 ? -1 : inner + 2;
            int centerYInner = inner < 0 ? -1 : inner + 3;
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    scaleX,
                    inner,
                    wrapRotate,
                    wrapRotateInner,
                    wrapY,
                    wrapYInner,
                    wrapSkew,
                    wrapSkewInner,
                    scaleY,
                    scaleYInner,
                    wrapSkewY,
                    wrapSkewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    centerX,
                    centerXInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    centerY,
                    centerYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
                    depth + 1
            );
            return;
        }
        if (format == 23) {
            if (color.remaining() < 13) {
                return;
            }
            int child = paint + offset24(color);
            int scale = color.getShort();
            int centerX = color.getShort();
            int centerY = color.getShort();
            int inner = varIndex(color.getInt());
            int centerXInner = inner < 0 ? -1 : inner + 1;
            int centerYInner = inner < 0 ? -1 : inner + 2;
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    scale,
                    inner,
                    wrapRotate,
                    wrapRotateInner,
                    wrapY,
                    wrapYInner,
                    wrapSkew,
                    wrapSkewInner,
                    scale,
                    inner,
                    wrapSkewY,
                    wrapSkewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    centerX,
                    centerXInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    centerY,
                    centerYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
                    depth + 1
            );
            return;
        }
        if (format == 31) {
            if (color.remaining() < 15) {
                return;
            }
            int child = paint + offset24(color);
            int skewX = color.getShort();
            int skewY = color.getShort();
            int centerX = color.getShort();
            int centerY = color.getShort();
            int inner = varIndex(color.getInt());
            int skewYInner = inner < 0 ? -1 : inner + 1;
            int centerXInner = inner < 0 ? -1 : inner + 2;
            int centerYInner = inner < 0 ? -1 : inner + 3;
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    wrapScale,
                    wrapScaleInner,
                    wrapRotate,
                    wrapRotateInner,
                    wrapY,
                    wrapYInner,
                    skewX,
                    inner,
                    wrapScaleY,
                    wrapScaleYInner,
                    skewY,
                    skewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    centerX,
                    centerXInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    centerY,
                    centerYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
                    depth + 1
            );
            return;
        }
        if (format == 13) {
            if (color.remaining() < 31) {
                return;
            }
            int child = paint + offset24(color);
            int xx = color.getInt();
            int yx = color.getInt();
            int xy = color.getInt();
            int yy = color.getInt();
            int dx = color.getInt();
            int dy = color.getInt();
            int inner = varIndex(color.getInt());
            int yxInner = inner < 0 ? -1 : inner + 1;
            int xyInner = inner < 0 ? -1 : inner + 2;
            int yyInner = inner < 0 ? -1 : inner + 3;
            int dxInner = inner < 0 ? -1 : inner + 4;
            int dyInner = inner < 0 ? -1 : inner + 5;
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    wrapScale,
                    wrapScaleInner,
                    wrapRotate,
                    wrapRotateInner,
                    wrapY,
                    wrapYInner,
                    wrapSkew,
                    wrapSkewInner,
                    wrapScaleY,
                    wrapScaleYInner,
                    wrapSkewY,
                    wrapSkewYInner,
                    xx,
                    inner,
                    wrapCenterX,
                    wrapCenterInner,
                    yx,
                    yxInner,
                    wrapCenterY,
                    wrapCenterYInner,
                    xy,
                    xyInner,
                    yy,
                    yyInner,
                    dx,
                    dxInner,
                    dy,
                    dyInner,
                    depth + 1
            );
            return;
        }
        if (format == 27) {
            if (color.remaining() < 13) {
                return;
            }
            int child = paint + offset24(color);
            int angle = color.getShort();
            int center = color.getShort();
            int centerY = color.getShort();
            int inner = varIndex(color.getInt());
            int centerInner = inner < 0 ? -1 : inner + 1;
            int centerYInner = inner < 0 ? -1 : inner + 2;
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    wrapScale,
                    wrapScaleInner,
                    angle,
                    inner,
                    wrapY,
                    wrapYInner,
                    wrapSkew,
                    wrapSkewInner,
                    wrapScaleY,
                    wrapScaleYInner,
                    wrapSkewY,
                    wrapSkewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    center,
                    centerInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    centerY,
                    centerYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    wrapScale,
                    wrapScaleInner,
                    wrapRotate,
                    wrapRotateInner,
                    wrapY,
                    wrapYInner,
                    wrapSkew,
                    wrapSkewInner,
                    wrapScaleY,
                    wrapScaleYInner,
                    wrapSkewY,
                    wrapSkewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    wrapCenterX,
                    wrapCenterInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    wrapCenterY,
                    wrapCenterYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    wrapScale,
                    wrapScaleInner,
                    wrapRotate,
                    wrapRotateInner,
                    wrapY,
                    wrapYInner,
                    wrapSkew,
                    wrapSkewInner,
                    wrapScaleY,
                    wrapScaleYInner,
                    wrapSkewY,
                    wrapSkewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    wrapCenterX,
                    wrapCenterInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    wrapCenterY,
                    wrapCenterYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
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
                    scaleXs,
                    scaleInners,
                    rotates,
                    rotateInners,
                    translateYs,
                    translateYInners,
                    skewXs,
                    skewInners,
                    scaleYs,
                    scaleYInners,
                    skewYs,
                    skewYInners,
                    transformXxs,
                    transformInners,
                    centerXs,
                    centerInners,
                    transformYxs,
                    transformYxInners,
                    centerYs,
                    centerYInners,
                    transformXys,
                    transformXyInners,
                    transformYys,
                    transformYyInners,
                    transformDxs,
                    transformDxInners,
                    transformDys,
                    transformDyInners,
                    wrapX,
                    wrapInner,
                    wrapScale,
                    wrapScaleInner,
                    wrapRotate,
                    wrapRotateInner,
                    wrapY,
                    wrapYInner,
                    wrapSkew,
                    wrapSkewInner,
                    wrapScaleY,
                    wrapScaleYInner,
                    wrapSkewY,
                    wrapSkewYInner,
                    wrapTransformXx,
                    wrapTransformInner,
                    wrapCenterX,
                    wrapCenterInner,
                    wrapTransformYx,
                    wrapTransformYxInner,
                    wrapCenterY,
                    wrapCenterYInner,
                    wrapTransformXy,
                    wrapTransformXyInner,
                    wrapTransformYy,
                    wrapTransformYyInner,
                    wrapTransformDx,
                    wrapTransformDxInner,
                    wrapTransformDy,
                    wrapTransformDyInner,
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
            int[] translateInners,
            int[] scaleXs,
            int[] scaleInners,
            int[] rotates,
            int[] rotateInners,
            int[] translateYs,
            int[] translateYInners,
            int[] skewXs,
            int[] skewInners,
            int[] scaleYs,
            int[] scaleYInners,
            int[] skewYs,
            int[] skewYInners,
            int[] transformXxs,
            int[] transformInners,
            int[] centerXs,
            int[] centerInners,
            int[] transformYxs,
            int[] transformYxInners,
            int[] centerYs,
            int[] centerYInners,
            int[] transformXys,
            int[] transformXyInners,
            int[] transformYys,
            int[] transformYyInners,
            int[] transformDxs,
            int[] transformDxInners,
            int[] transformDys,
            int[] transformDyInners
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
            int scaleX = 0;
            if (slot < layerScaleXs.length) {
                scaleX = layerScaleXs[slot];
                if (slot < layerScaleInners.length) {
                    scaleX += varDelta(layerScaleInners[slot], normalized);
                }
            }
            int rotate = 0;
            if (slot < layerRotates.length) {
                rotate = layerRotates[slot];
                if (slot < layerRotateInners.length) {
                    rotate += varDelta(layerRotateInners[slot], normalized);
                }
            }
            int translateY = 0;
            if (slot < layerTranslateYs.length) {
                translateY = layerTranslateYs[slot];
                if (slot < layerTranslateYInners.length) {
                    translateY += varDelta(layerTranslateYInners[slot], normalized);
                }
            }
            int skewX = 0;
            if (slot < layerSkewXs.length) {
                skewX = layerSkewXs[slot];
                if (slot < layerSkewInners.length) {
                    skewX += varDelta(layerSkewInners[slot], normalized);
                }
            }
            int scaleY = 0;
            if (slot < layerScaleYs.length) {
                scaleY = layerScaleYs[slot];
                if (slot < layerScaleYInners.length) {
                    scaleY += varDelta(layerScaleYInners[slot], normalized);
                }
            }
            int skewY = 0;
            if (slot < layerSkewYs.length) {
                skewY = layerSkewYs[slot];
                if (slot < layerSkewYInners.length) {
                    skewY += varDelta(layerSkewYInners[slot], normalized);
                }
            }
            int transformXx = 0;
            if (slot < layerTransformXxs.length) {
                transformXx = layerTransformXxs[slot];
                if (slot < layerTransformInners.length) {
                    transformXx += varDelta(layerTransformInners[slot], normalized);
                }
            }
            int centerX = 0;
            if (slot < layerCenterXs.length) {
                centerX = layerCenterXs[slot];
                if (slot < layerCenterInners.length) {
                    centerX += varDelta(layerCenterInners[slot], normalized);
                }
            }
            int transformYx = 0;
            if (slot < layerTransformYxs.length) {
                transformYx = layerTransformYxs[slot];
                if (slot < layerTransformYxInners.length) {
                    transformYx += varDelta(layerTransformYxInners[slot], normalized);
                }
            }
            int centerY = 0;
            if (slot < layerCenterYs.length) {
                centerY = layerCenterYs[slot];
                if (slot < layerCenterYInners.length) {
                    centerY += varDelta(layerCenterYInners[slot], normalized);
                }
            }
            int transformXy = 0;
            if (slot < layerTransformXys.length) {
                transformXy = layerTransformXys[slot];
                if (slot < layerTransformXyInners.length) {
                    transformXy += varDelta(layerTransformXyInners[slot], normalized);
                }
            }
            int transformYy = 0;
            if (slot < layerTransformYys.length) {
                transformYy = layerTransformYys[slot];
                if (slot < layerTransformYyInners.length) {
                    transformYy += varDelta(layerTransformYyInners[slot], normalized);
                }
            }
            int transformDx = 0;
            if (slot < layerTransformDxs.length) {
                transformDx = layerTransformDxs[slot];
                if (slot < layerTransformDxInners.length) {
                    transformDx += varDelta(layerTransformDxInners[slot], normalized);
                }
            }
            int transformDy = 0;
            if (slot < layerTransformDys.length) {
                transformDy = layerTransformDys[slot];
                if (slot < layerTransformDyInners.length) {
                    transformDy += varDelta(layerTransformDyInners[slot], normalized);
                }
            }
            layers[index] = new ColorLayer(
                    layerGlyphs[slot],
                    paletteIndex,
                    color,
                    translateX,
                    scaleX,
                    rotate,
                    translateY,
                    skewX,
                    scaleY,
                    skewY,
                    transformXx,
                    centerX,
                    transformYx,
                    centerY,
                    transformXy,
                    transformYy,
                    transformDx,
                    transformDy
            );
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
