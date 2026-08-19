package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Reads a checked SFNT, TTC first face, WOFF1, or WOFF2 directory, `cmap` format 4/12, `hmtx`, TrueType `loca`/`glyf` or
/// CFF/CFF2 Type 2 outlines, optional GSUB, GDEF, GPOS/`kern`, COLR v0/v1/CPAL, `fvar`, `avar`, `gvar`,
/// `HVAR`, `VVAR`, `MVAR`, `STAT`, `sbix`, CBLC/CBDT, EBLC/EBDT, `gasp`, uncompressed `SVG `, `OS/2`,
/// `name` family and style, and `post` italic/pitch.
///
/// The font file is retained as a read-only [MemorySegment] so the same view can back a heap array
/// or a later mapped file. Sequential table decoding uses [ByteBuffer] cursors over those slices.
@NotNullByDefault
public final class SfntFont {
    /// The retained font file.
    private final MemorySegment data;

    /// Table directory by tag.
    private final @Unmodifiable Map<String, TableRecord> tables;

    /// Units per em.
    private final int unitsPerEm;

    /// Number of glyphs.
    private final int glyphCount;

    /// Format-4 cmap: start codes.
    private final int[] cmapStart;

    /// Format-4 cmap: end codes.
    private final int[] cmapEnd;

    /// Format-4 cmap: idDeltas.
    private final short[] cmapDelta;

    /// Format-4 cmap: idRangeOffset.
    private final int[] cmapRangeOffset;

    /// Format-4 cmap: glyph id array referenced by range offsets.
    private final int[] cmapGlyphIds;

    /// Format-12 cmap: start code points, empty when absent.
    private final int[] cmap12Start;

    /// Format-12 cmap: end code points, empty when absent.
    private final int[] cmap12End;

    /// Format-12 cmap: start glyph ids, empty when absent.
    private final int[] cmap12Glyph;

    /// Advance widths.
    private final int[] advances;

    /// loca offsets into glyf, empty for a CFF face.
    private final int[] loca;

    /// CFF/CFF2 outlines, or `null` for a TrueType face.
    private final @Nullable CffOutlines cff;

    /// GDEF glyph classes, empty when the table is absent.
    private final GdefTable gdef;

    /// GSUB type-1 substitutions, or empty when the table is absent.
    private final GsubSubstitutions gsub;

    /// GPOS type-2 and format-0 `kern` pair adjustments, or empty when both tables are absent.
    private final GposPositioning gpos;

    /// COLR v0 layers and CPAL palettes, empty when either table is absent.
    private final ColrCpal colr;

    /// `fvar` axes, empty when the table is absent.
    private final FvarTable fvar;

    /// `gvar` tuple deltas, empty when the table is absent.
    private final GvarTable gvar;

    /// `avar` axis maps, empty when the table is absent.
    private final AvarTable avar;

    /// `HVAR` advance deltas, empty when the table is absent.
    private final HvarTable hvar;

    /// `VVAR` vertical-advance deltas, empty when the table is absent.
    private final HvarTable vvar;

    /// `MVAR` font-wide metric deltas, empty when the table is absent.
    private final MvarTable mvar;

    /// `STAT` design axes and named instances, empty when the table is absent.
    private final StatTable stat;

    /// Default `hhea` ascender in font units.
    private final int ascender;

    /// Vertical advances from `vmtx`, or zeros when the table is absent.
    private final int[] verticalAdvances;

    /// First `sbix` strike, empty when the table is absent.
    private final SbixTable sbix;

    /// First CBLC/CBDT format-1 strike, empty when the tables are absent.
    private final CbdtCblc cbdt;

    /// First EBLC/EBDT format-1 strike, empty when the tables are absent.
    private final CbdtCblc ebdt;

    /// `gasp` grayscale ranges, empty when the table is absent.
    private final GaspTable gasp;

    /// OpenType `SVG ` documents, empty when the table is absent.
    private final SvgTable svg;

    /// `OS/2` metrics, empty when the table is absent.
    private final Os2Table os2;

    /// `name` family string, empty when the table is absent.
    private final NameTable names;

    /// `post` header, empty when the table is absent.
    private final PostTable post;

    /// Shared empty normalized instance used for the default outline.
    private static final float[] DEFAULT_NORMALIZED = new float[0];

    /// Creates a font from heap SFNT bytes.
    ///
    /// @param bytes the complete font file
    public SfntFont(byte[] bytes) {
        this(MemorySegment.ofArray(Objects.requireNonNull(bytes, "bytes").clone()));
    }

    /// Creates a font from an SFNT memory image.
    ///
    /// The constructor copies `bytes` onto the heap so the font does not retain the caller's arena
    /// or array.
    ///
    /// @param bytes the complete font file
    public SfntFont(MemorySegment bytes) {
        Objects.requireNonNull(bytes, "bytes");
        MemorySegment sfnt = Woff2File.isWoff2(bytes)
                ? Woff2File.unwrap(bytes)
                : WoffFile.isWoff(bytes) ? WoffFile.unwrap(bytes) : bytes;
        if (TtcFile.isTtc(sfnt)) {
            sfnt = TtcFile.firstFont(sfnt);
        }
        this.data = MemorySegment.ofArray(sfnt.toArray(ValueLayout.JAVA_BYTE)).asReadOnly();
        ByteBuffer buffer = cursor(data);
        if (buffer.remaining() < 12) {
            throw new IllegalArgumentException("SFNT header is truncated");
        }
        buffer.getInt();
        int tableCount = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        buffer.getShort();
        buffer.getShort();
        LinkedHashMap<String, TableRecord> directory = new LinkedHashMap<>();
        for (int index = 0; index < tableCount; index++) {
            if (buffer.remaining() < 16) {
                throw new IllegalArgumentException("SFNT table directory is truncated");
            }
            byte[] tagBytes = new byte[4];
            buffer.get(tagBytes);
            buffer.getInt();
            int offset = buffer.getInt();
            int length = buffer.getInt();
            if (offset < 0 || length < 0 || (long) offset + (long) length > data.byteSize()) {
                throw new IllegalArgumentException("SFNT table is out of range");
            }
            directory.put(new String(tagBytes, StandardCharsets.US_ASCII), new TableRecord(offset, length));
        }
        this.tables = Map.copyOf(directory);
        ByteBuffer head = table("head");
        if (head.remaining() < 54) {
            throw new IllegalArgumentException("head table is truncated");
        }
        head.position(18);
        this.unitsPerEm = Short.toUnsignedInt(head.getShort());
        ByteBuffer maxp = table("maxp");
        if (maxp.remaining() < 6) {
            throw new IllegalArgumentException("maxp table is truncated");
        }
        maxp.position(4);
        this.glyphCount = Short.toUnsignedInt(maxp.getShort());
        CmapTables cmap = readCmap();
        this.cmapStart = cmap.format4.startCodes;
        this.cmapEnd = cmap.format4.endCodes;
        this.cmapDelta = cmap.format4.idDeltas;
        this.cmapRangeOffset = cmap.format4.idRangeOffsets;
        this.cmapGlyphIds = cmap.format4.glyphIds;
        this.cmap12Start = cmap.format12.startCodes;
        this.cmap12End = cmap.format12.endCodes;
        this.cmap12Glyph = cmap.format12.startGlyphs;
        this.advances = readAdvances();
        this.ascender = readAscender();
        @Nullable ByteBuffer cffTable = findTable("CFF ");
        @Nullable ByteBuffer cff2Table = findTable("CFF2");
        if (tables.containsKey("glyf") && tables.containsKey("loca")) {
            this.loca = readLoca(head);
            this.cff = null;
        } else if (cffTable != null || cff2Table != null) {
            this.loca = new int[0];
            this.cff = CffOutlines.parse(cffTable, cff2Table);
        } else {
            throw new IllegalArgumentException("SFNT has neither glyf/loca nor CFF/CFF2");
        }
        this.gdef = GdefTable.parse(findTable("GDEF"));
        this.gsub = GsubSubstitutions.parse(findTable("GSUB"), this.gdef);
        this.gpos = GposPositioning.parse(findTable("GPOS"), findTable("kern"), this.gdef);
        this.colr = ColrCpal.parse(findTable("COLR"), findTable("CPAL"));
        this.fvar = FvarTable.parse(findTable("fvar"));
        this.gvar = GvarTable.parse(findTable("gvar"), glyphCount);
        this.avar = AvarTable.parse(findTable("avar"), fvar.axes().size());
        this.hvar = HvarTable.parse(findTable("HVAR"), fvar.axes().size());
        this.vvar = HvarTable.parse(findTable("VVAR"), fvar.axes().size());
        this.mvar = MvarTable.parse(findTable("MVAR"), fvar.axes().size());
        this.stat = StatTable.parse(findTable("STAT"));
        this.verticalAdvances = readVerticalAdvances();
        this.sbix = SbixTable.parse(findTable("sbix"), glyphCount);
        this.cbdt = CbdtCblc.parse(findTable("CBLC"), findTable("CBDT"), CbdtCblc.TAG_CBDT);
        this.ebdt = CbdtCblc.parse(findTable("EBLC"), findTable("EBDT"), CbdtCblc.TAG_EBDT);
        this.gasp = GaspTable.parse(findTable("gasp"));
        this.svg = SvgTable.parse(findTable("SVG "));
        this.os2 = Os2Table.parse(findTable("OS/2"));
        this.names = NameTable.parse(findTable("name"));
        this.post = PostTable.parse(findTable("post"));
    }

    /// Returns the retained font file.
    ///
    /// @return a read-only file image
    public MemorySegment bytes() {
        return data;
    }

    /// Returns units per em.
    ///
    /// @return the em size
    public int unitsPerEm() {
        return unitsPerEm;
    }

    /// Returns the `gasp` behavior flags at `ppem`.
    ///
    /// A missing table reports [`GaspTable#DOGRAY`].
    ///
    /// @param ppem the destination pixels-per-em
    /// @return the flags
    public int gaspFlags(int ppem) {
        return gasp.flagsAt(ppem);
    }

    /// Returns whether unhinted grayscale is permitted at `ppem`.
    ///
    /// @param ppem the destination pixels-per-em
    /// @return whether grayscale coverage may be produced
    public boolean gaspAllowsGrayscale(int ppem) {
        return gasp.allowsGrayscale(ppem);
    }

    /// Returns whether `gasp` requests vertical-only grid fitting at `ppem`.
    ///
    /// @param ppem the destination pixels-per-em
    /// @return whether the rasterizer will snap the outline box to the pixel grid
    public boolean gaspGridFits(int ppem) {
        return gasp.gridFits(ppem);
    }

    /// Returns whether `gasp` requests symmetric grid fitting at `ppem`.
    ///
    /// @param ppem the destination pixels-per-em
    /// @return whether the rasterizer will snap the outline x-box to the pixel grid
    public boolean gaspSymmetricGridFits(int ppem) {
        return gasp.symmetricGridFits(ppem);
    }

    /// Returns the default `hhea` ascender.
    ///
    /// @return the ascender in font units
    public int ascender() {
        return ascender(defaultVariation());
    }

    /// Returns the ascender at `axisValues`, applying an `MVAR` `hasc` delta when present.
    ///
    /// @param axisValues design-space coordinates, one per axis
    /// @return the varied ascender
    public int ascender(float[] axisValues) {
        Objects.requireNonNull(axisValues, "axisValues");
        return ascender + mvar.hascDelta(instanceCoords(axisValues));
    }

    /// Returns whether `cmap` maps `codePoint` to a nonzero glyph.
    ///
    /// Glyph `0` is `.notdef`. A missing mapping does not search another face.
    ///
    /// @param codePoint the code point
    /// @return whether this face covers the code point
    public boolean hasGlyph(int codePoint) {
        return glyphId(codePoint) != 0;
    }

    /// Maps a Unicode code point through `cmap`.
    ///
    /// @param codePoint the code point
    /// @return the glyph id, or `0`
    public int glyphId(int codePoint) {
        int format4 = glyphIdFormat4(codePoint);
        if (format4 != 0) {
            return format4;
        }
        return glyphIdFormat12(codePoint);
    }

    /// Maps through the format-4 table.
    private int glyphIdFormat4(int codePoint) {
        for (int index = 0; index < cmapEnd.length; index++) {
            if (codePoint >= cmapStart[index] && codePoint <= cmapEnd[index]) {
                if (cmapRangeOffset[index] == 0) {
                    return (codePoint + cmapDelta[index]) & 0xFFFF;
                }
                int glyphIndex = cmapRangeOffset[index] / 2 + (codePoint - cmapStart[index]) - (cmapEnd.length - index);
                if (glyphIndex < 0 || glyphIndex >= cmapGlyphIds.length) {
                    return 0;
                }
                int glyphId = cmapGlyphIds[glyphIndex];
                if (glyphId == 0) {
                    return 0;
                }
                return (glyphId + cmapDelta[index]) & 0xFFFF;
            }
        }
        return 0;
    }

    /// Maps through the format-12 table.
    private int glyphIdFormat12(int codePoint) {
        for (int index = 0; index < cmap12End.length; index++) {
            if (codePoint >= cmap12Start[index] && codePoint <= cmap12End[index]) {
                long glyphId = (long) cmap12Glyph[index] + (long) (codePoint - cmap12Start[index]);
                if (glyphId <= 0L || glyphId > 0xFFFFL) {
                    return 0;
                }
                return (int) glyphId;
            }
        }
        return 0;
    }

    /// Returns horizontal metrics for a glyph at the default instance.
    ///
    /// @param glyphId the glyph id
    /// @return the metrics
    public GlyphMetrics metrics(int glyphId) {
        return metrics(glyphId, defaultVariation());
    }

    /// Returns horizontal and vertical metrics for a glyph at `axisValues`.
    ///
    /// Design-space coordinates follow [`#variationAxes()`] order and are remapped by `avar`
    /// before `gvar`, `HVAR`, and `VVAR`. A simple `gvar` glyph adds phantom deltas; `HVAR` then
    /// adds its advance-width delta and `VVAR` adds its advance-height delta. A negative varied
    /// advance is clamped to `0`.
    ///
    /// @param glyphId the glyph id
    /// @param axisValues design-space coordinates, one per axis
    /// @return the metrics
    public GlyphMetrics metrics(int glyphId, float[] axisValues) {
        Objects.requireNonNull(axisValues, "axisValues");
        if (glyphId < 0 || glyphId >= advances.length) {
            throw new IllegalArgumentException("Unknown glyph " + glyphId);
        }
        float[] normalized = instanceCoords(axisValues);
        int pointCount = simplePointCount(glyphId);
        int advance = advances[glyphId]
                + gvar.advanceDelta(glyphId, pointCount, normalized)
                + hvar.advanceDelta(glyphId, normalized);
        if (advance < 0) {
            advance = 0;
        }
        int height = verticalAdvances[glyphId] + vvar.advanceDelta(glyphId, normalized);
        if (height < 0) {
            height = 0;
        }
        int lsb = gvar.leftSideBearingDelta(glyphId, pointCount, normalized);
        return new GlyphMetrics(glyphId, advance, lsb, height);
    }

    /// Returns whether this face stores TrueType `glyf` outlines.
    ///
    /// @return whether `glyf`/`loca` are present
    public boolean hasTrueTypeOutlines() {
        return cff == null;
    }

    /// Returns whether this face stores CFF2 rather than CFF 1.
    ///
    /// @return whether the `CFF2` table supplied the outlines
    public boolean hasCff2Outlines() {
        return cff != null && cff.isCff2();
    }

    /// Walks the default-instance outline for `glyphId` into `pen`.
    ///
    /// Empty glyphs emit no commands. TrueType simple contours include implied on-curve midpoints
    /// as untruncated averages, and composites expand up to 16 nested components. CFF/CFF2 glyphs
    /// emit Type 2 lines and cubics; hints are skipped. Coordinates are font units with y upward.
    /// The default instance applies no `gvar` deltas.
    ///
    /// @param glyphId the glyph identity
    /// @param pen the destination
    public void outline(int glyphId, OutlinePen pen) {
        outlineNormalized(glyphId, pen, DEFAULT_NORMALIZED);
    }

    /// Walks the outline for `glyphId` at design-space `axisValues`.
    ///
    /// Coordinates follow [`#variationAxes()`] order, are clamped to each axis min/max, and are
    /// normalized then remapped by `avar` before `gvar` interpolation. A shorter array uses the
    /// default for missing axes.
    /// Extra values are ignored. CFF/CFF2 faces ignore `axisValues` because this subset has no
    /// CFF2 variation store.
    ///
    /// @param glyphId the glyph identity
    /// @param pen the destination
    /// @param axisValues design-space coordinates, one per axis
    public void outline(int glyphId, OutlinePen pen, float[] axisValues) {
        Objects.requireNonNull(axisValues, "axisValues");
        outlineNormalized(glyphId, pen, instanceCoords(axisValues));
    }

    /// Normalizes design-space coordinates and applies `avar`.
    private float[] instanceCoords(float[] axisValues) {
        return avar.map(fvar.normalize(axisValues));
    }

    /// Walks a TrueType or CFF outline after axis normalization.
    private void outlineNormalized(int glyphId, OutlinePen pen, float[] normalized) {
        if (cff != null) {
            cff.outline(glyphId, pen);
            return;
        }
        OutlineWalker.walk(this, glyphId, pen, 0, normalized);
    }

    /// Applies `gvar` contour deltas at `normalized` to a simple glyph's point arrays.
    ///
    /// @param glyphId the glyph
    /// @param xs contour x coordinates
    /// @param ys contour y coordinates
    /// @param normalized normalized axis coordinates
    void applyGvar(int glyphId, float[] xs, float[] ys, float[] normalized) {
        gvar.apply(glyphId, xs, ys, normalized);
    }

    /// Returns the simple-glyph point count used to locate `gvar` phantoms.
    ///
    /// Empty, composite, and CFF glyphs return `0`.
    ///
    /// @param glyphId the glyph
    /// @return the contour point count
    private int simplePointCount(int glyphId) {
        if (cff != null || loca.length == 0) {
            return 0;
        }
        ByteBuffer glyf = glyf(glyphId);
        if (glyf.remaining() < 2) {
            return 0;
        }
        short contours = glyf.getShort();
        if (contours <= 0 || glyf.remaining() < 8 + contours * 2) {
            return 0;
        }
        glyf.position(glyf.position() + 8);
        int last = -1;
        for (int index = 0; index < contours; index++) {
            last = Short.toUnsignedInt(glyf.getShort());
        }
        return last + 1;
    }

    /// GSUB `rlig` feature tag.
    public static final int TAG_RLIG = 0x726C6967;

    /// GSUB `liga` feature tag.
    public static final int TAG_LIGA = 0x6C696761;

    /// GSUB `calt` feature tag.
    public static final int TAG_CALT = 0x63616C74;

    /// GSUB `ccmp` feature tag.
    public static final int TAG_CCMP = 0x63636D70;

    /// GSUB `aalt` feature tag.
    public static final int TAG_AALT = 0x61616C74;

    /// Applies GSUB single substitutions listed by `featureTag`.
    ///
    /// Lookups other than type 1 are skipped. A missing GSUB table or feature returns `glyphId`.
    ///
    /// @param glyphId the input glyph
    /// @param featureTag a four-byte OpenType tag as a big-endian `int`
    /// @return the substituted glyph, or `glyphId`
    public int substitute(int glyphId, int featureTag) {
        return gsub.apply(glyphId, featureTag);
    }

    /// Applies the first GSUB type-2 multiple substitution listed by `featureTag`.
    ///
    /// Type-7 ExtensionSubst wrappers are unwrapped before the type-2 subtable is read. A missing
    /// table, missing feature, or glyph outside coverage returns `null`.
    ///
    /// @param glyphId the input glyph
    /// @param featureTag a four-byte OpenType tag as a big-endian `int`
    /// @return the substitute sequence, or `null`
    public int @Nullable [] decompose(int glyphId, int featureTag) {
        return gsub.decompose(glyphId, featureTag);
    }

    /// Applies the first GSUB type-3 alternate listed by `featureTag`.
    ///
    /// The first-stable subset returns the first glyph of the matching AlternateSet. A missing
    /// table, missing feature, or glyph outside coverage returns `glyphId`.
    ///
    /// @param glyphId the input glyph
    /// @param featureTag a four-byte OpenType tag as a big-endian `int`
    /// @return the first alternate, or `glyphId`
    public int alternate(int glyphId, int featureTag) {
        return gsub.alternate(glyphId, featureTag);
    }

    /// Applies the first GSUB type-4 ligature listed by `featureTag` at `start`.
    ///
    /// Lookups other than type 4 are skipped. A missing table, missing feature, or failed match
    /// returns `null`. The first matching ligature in table order wins.
    ///
    /// @param glyphIds the mapped glyph identities
    /// @param start the first glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @param featureTag a four-byte OpenType tag as a big-endian `int`
    /// @return the match, or `null`
    public @Nullable GlyphLigature ligature(int[] glyphIds, int start, int remaining, int featureTag) {
        return gsub.ligature(glyphIds, start, remaining, featureTag);
    }

    /// Applies a GSUB type-5 two-glyph context substitution listed by `featureTag`.
    ///
    /// @param current the first input glyph
    /// @param next the second input glyph
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or `current`
    public int contextSubstitute(int current, int next, int featureTag) {
        return gsub.contextSubstitute(current, next, featureTag);
    }

    /// Applies a type-5 rule, using `skippedNext` when the lookup has `IgnoreMarks`.
    ///
    /// @param current the first input glyph
    /// @param next the immediately following glyph
    /// @param skippedNext the first non-mark after `current`
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or `current`
    public int contextSubstitute(int current, int next, int skippedNext, int featureTag) {
        return gsub.contextSubstitute(current, next, skippedNext, featureTag);
    }

    /// Applies a type-5 rule by walking `glyphIds` with `IgnoreMarks` and `MarkAttachmentType`.
    ///
    /// @param glyphIds the mapped glyph identities
    /// @param start the first glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or the glyph at `start`
    public int contextSubstitute(int[] glyphIds, int start, int remaining, int featureTag) {
        return gsub.contextSubstitute(glyphIds, start, remaining, featureTag);
    }

    /// Applies a GSUB type-6 one-lookahead chain substitution listed by `featureTag`.
    ///
    /// @param current the first input glyph
    /// @param next the second input glyph
    /// @param lookahead the first lookahead glyph
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or `current`
    public int chainSubstitute(int current, int next, int lookahead, int featureTag) {
        return gsub.chainSubstitute(current, next, lookahead, featureTag);
    }

    /// Applies a type-6 rule, using skipped glyphs when the lookup has `IgnoreMarks`.
    ///
    /// @param current the first input glyph
    /// @param next the immediately following glyph
    /// @param lookahead the first lookahead glyph
    /// @param skippedNext the first non-mark after `current`
    /// @param skippedLookahead the first non-mark after `skippedNext`
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or `current`
    public int chainSubstitute(
            int current,
            int next,
            int lookahead,
            int skippedNext,
            int skippedLookahead,
            int featureTag
    ) {
        return gsub.chainSubstitute(current, next, lookahead, skippedNext, skippedLookahead, featureTag);
    }

    /// Applies a type-6 rule by walking `glyphIds` with `IgnoreMarks` and `MarkAttachmentType`.
    ///
    /// @param glyphIds the mapped glyph identities
    /// @param start the first glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or the glyph at `start`
    public int chainSubstitute(int[] glyphIds, int start, int remaining, int featureTag) {
        return gsub.chainSubstitute(glyphIds, start, remaining, featureTag);
    }

    /// Applies a GSUB type-8 reverse-chain substitution listed by `featureTag`.
    ///
    /// The first-stable subset matches one lookahead glyph and no backtrack. A missing table,
    /// missing feature, or failed match returns `current`.
    ///
    /// @param current the input glyph
    /// @param lookahead the following glyph
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted glyph, or `current`
    public int reverseSubstitute(int current, int lookahead, int featureTag) {
        return gsub.reverseSubstitute(current, lookahead, featureTag);
    }

    /// Applies a type-8 reverse rule by walking lookahead glyphs with skip flags.
    ///
    /// @param glyphIds the mapped glyphs
    /// @param start the input glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted glyph, or the glyph at `start`
    public int reverseSubstitute(int[] glyphIds, int start, int remaining, int featureTag) {
        return gsub.reverseSubstitute(glyphIds, start, remaining, featureTag);
    }

    /// Returns the GDEF glyph class, or `0` when unassigned or GDEF is absent.
    ///
    /// @param glyphId the glyph
    /// @return the class
    public int glyphClass(int glyphId) {
        return gdef.glyphClass(glyphId);
    }

    /// Returns whether GDEF classifies `glyphId` as a combining mark.
    ///
    /// @param glyphId the glyph
    /// @return whether it is a mark
    public boolean isGdefMark(int glyphId) {
        return gdef.isMark(glyphId);
    }

    /// Returns whether GDEF classifies `glyphId` as a base glyph.
    ///
    /// @param glyphId the glyph
    /// @return whether it is a base
    public boolean isGdefBase(int glyphId) {
        return gdef.isBase(glyphId);
    }

    /// Returns whether GDEF classifies `glyphId` as a ligature glyph.
    ///
    /// @param glyphId the glyph
    /// @return whether it is a ligature
    public boolean isGdefLigature(int glyphId) {
        return gdef.isLigature(glyphId);
    }

    /// Returns whether `glyphId` is covered by GDEF MarkGlyphSet `setIndex`.
    ///
    /// @param glyphId the glyph
    /// @param setIndex the mark-filter set
    /// @return whether the glyph is in the set
    public boolean inMarkSet(int glyphId, int setIndex) {
        return gdef.inMarkSet(glyphId, setIndex);
    }

    /// Returns the GDEF mark-attach class, or `0` when unassigned or GDEF is absent.
    ///
    /// @param glyphId the glyph
    /// @return the class
    public int markAttachClass(int glyphId) {
        return gdef.markAttachClass(glyphId);
    }

    /// Returns the unique GPOS `MarkAttachmentType` values present in this face.
    ///
    /// @return the classes, possibly empty
    public int @Unmodifiable [] markAttachmentTypes() {
        return gpos.attachmentTypes();
    }

    /// Returns the GPOS/`kern` X-advance delta for the consecutive pair `(left, right)`.
    ///
    /// Type-2 pair positioning, format-0 `kern`, type-3 cursive exit-to-entry X deltas, and
    /// type-7 two-glyph context rules without skip flags share this map. `IgnoreMarks` and
    /// `MarkAttachmentType` lookups use [`#skipPairAdjustment(int, int)`] and
    /// [`#attachPairAdjustment(int, int, int)`]. A missing pair or missing table returns `0`.
    /// The delta is in font units and may be negative.
    ///
    /// @param left the first glyph
    /// @param right the second glyph
    /// @return the signed X-advance adjustment applied to `left`
    public int pairAdjustment(int left, int right) {
        return gpos.pairAdjustment(left, right);
    }

    /// Returns the GPOS `IgnoreMarks` pair X-advance for `(left, right)`.
    ///
    /// Type-2 and type-7 lookups with flag `0x0008` share this map. `right` is the first
    /// following glyph that is not a GDEF mark.
    ///
    /// @param left the first glyph
    /// @param right the next non-mark glyph
    /// @return the signed adjustment, or `0`
    public int skipPairAdjustment(int left, int right) {
        return gpos.skipPairAdjustment(left, right);
    }

    /// Returns the GPOS `MarkAttachmentType` pair X-advance for class `attachType`.
    ///
    /// Type-2 and type-7 lookups with a non-zero high-byte flag share this map. `right` is the
    /// first following glyph that is not a mark whose attach class differs from `attachType`.
    ///
    /// @param left the first glyph
    /// @param right the next non-skipped glyph
    /// @param attachType the lookup high-byte class
    /// @return the signed adjustment, or `0`
    public int attachPairAdjustment(int left, int right, int attachType) {
        return gpos.attachPairAdjustment(left, right, attachType);
    }

    /// Applies every stored pair lookup at `start`, honoring skip flags.
    ///
    /// @param glyphIds the mapped glyphs
    /// @param start the first glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @return the summed X-advance delta
    public int pairAdjustment(int[] glyphIds, int start, int remaining) {
        return gpos.pairAdjustment(glyphIds, start, remaining);
    }

    /// Returns the GPOS type-1 X-advance for `glyphId`.
    ///
    /// @param glyphId the glyph
    /// @return the signed adjustment, or `0`
    public int singleAdjustment(int glyphId) {
        return gpos.singleAdjustment(glyphId);
    }

    /// Returns the GPOS type-8 X-advance for the triple `(current, next, lookahead)`.
    ///
    /// Adjacent lookups without skip flags or backtrack use this map. Rules that require a
    /// preceding glyph are applied only through [`#chainAdjustment(int[], int, int)`].
    /// `IgnoreMarks` and `MarkAttachmentType` lookups use [`#skipChainAdjustment(int, int, int)`]
    /// and [`#attachChainAdjustment(int, int, int, int)`].
    ///
    /// @param current the first input glyph
    /// @param next the second input glyph
    /// @param lookahead the first lookahead glyph
    /// @return the signed adjustment, or `0`
    public int chainAdjustment(int current, int next, int lookahead) {
        return gpos.chainAdjustment(current, next, lookahead);
    }

    /// Returns the GPOS `IgnoreMarks` type-8 X-advance.
    ///
    /// @param current the first input glyph
    /// @param next the next non-mark glyph
    /// @param lookahead the following non-mark glyph
    /// @return the signed adjustment, or `0`
    public int skipChainAdjustment(int current, int next, int lookahead) {
        return gpos.skipChainAdjustment(current, next, lookahead);
    }

    /// Returns the GPOS `MarkAttachmentType` type-8 X-advance for class `attachType`.
    ///
    /// @param current the first input glyph
    /// @param next the next non-skipped glyph
    /// @param lookahead the following non-skipped glyph
    /// @param attachType the lookup high-byte class
    /// @return the signed adjustment, or `0`
    public int attachChainAdjustment(int current, int next, int lookahead, int attachType) {
        return gpos.attachChainAdjustment(current, next, lookahead, attachType);
    }

    /// Returns the GPOS `MarkAttachmentType` type-8 X-advance, honoring required backtrack glyphs.
    ///
    /// @param current the first input glyph
    /// @param next the next non-skipped glyph
    /// @param lookahead the following non-skipped glyph
    /// @param attachType the lookup high-byte class
    /// @param backNear the nearest kept preceding glyph, or `0`
    /// @param backFar the next kept preceding glyph, or `0`
    /// @return the signed adjustment, or `0`
    public int attachChainAdjustment(
            int current,
            int next,
            int lookahead,
            int attachType,
            int backNear,
            int backMid
    ) {
        return gpos.attachChainAdjustment(current, next, lookahead, attachType, backNear, backMid, 0);
    }

    /// Returns the GPOS `MarkAttachmentType` type-8 X-advance with three backtrack glyphs.
    ///
    /// @param current the first input glyph
    /// @param next the next non-skipped glyph
    /// @param lookahead the following non-skipped glyph
    /// @param attachType the lookup high-byte class
    /// @param backNear the nearest kept preceding glyph, or `0`
    /// @param backMid the next kept preceding glyph, or `0`
    /// @param backFar the farthest kept preceding glyph, or `0`
    /// @return the signed adjustment, or `0`
    public int attachChainAdjustment(
            int current,
            int next,
            int lookahead,
            int attachType,
            int backNear,
            int backMid,
            int backFar
    ) {
        return gpos.attachChainAdjustment(current, next, lookahead, attachType, backNear, backMid, backFar);
    }

    /// Applies every stored type-8 lookup at `start`, honoring skip flags.
    ///
    /// @param glyphIds the mapped glyphs
    /// @param start the first glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @return the summed X-advance delta
    public int chainAdjustment(int[] glyphIds, int start, int remaining) {
        return gpos.chainAdjustment(glyphIds, start, remaining);
    }

    /// Returns whether `glyphId` is covered by a GPOS mark table.
    ///
    /// @param glyphId the glyph
    /// @return whether the glyph is a mark
    public boolean isMark(int glyphId) {
        return gpos.isMark(glyphId);
    }

    /// Returns the mark-to-base placement for `(markGlyph, baseGlyph)`.
    ///
    /// @param markGlyph the mark glyph
    /// @param baseGlyph the base glyph
    /// @return the placement, or `null` when uncovered
    public @Nullable MarkPlacement markPlacement(int markGlyph, int baseGlyph) {
        return gpos.markPlacement(markGlyph, baseGlyph);
    }

    /// Returns COLR v0 or flattened v1 layers for `glyphId` from palette `0`.
    ///
    /// @param glyphId the base glyph
    /// @return the layers, empty when the glyph is not a color base
    public @Unmodifiable List<ColorLayer> colorLayers(int glyphId) {
        return colorLayers(glyphId, 0);
    }

    /// Returns COLR v0 or flattened v1 layers for `glyphId` from `palette`.
    ///
    /// @param glyphId the base glyph
    /// @param palette the CPAL palette index
    /// @return the layers, empty when the glyph is not a color base
    public @Unmodifiable List<ColorLayer> colorLayers(int glyphId, int palette) {
        return colorLayers(glyphId, palette, defaultVariation());
    }

    /// Returns COLR v0 or flattened v1 layers for `glyphId` from `palette` at `axisValues`.
    ///
    /// Design-space coordinates follow [`#variationAxes()`] order and are remapped by `avar`
    /// before a COLR ItemVariationStore delta is applied to `PaintVarSolid` palette indices
    /// and `PaintVarTranslate` X offsets.
    ///
    /// @param glyphId the base glyph
    /// @param palette the CPAL palette index
    /// @param axisValues design-space axis values
    /// @return the layers, empty when the glyph is not a color base
    public @Unmodifiable List<ColorLayer> colorLayers(int glyphId, int palette, float[] axisValues) {
        return colr.layers(glyphId, palette, instanceCoords(axisValues));
    }

    /// Returns one CPAL color, or `null` for the foreground sentinel.
    ///
    /// @param palette the palette index
    /// @param entry the entry, or [`PaletteColor#FOREGROUND`]
    /// @return the color
    public @Nullable PaletteColor paletteColor(int palette, int entry) {
        return colr.colorAt(palette, entry);
    }

    /// Returns the `fvar` axes in file order.
    ///
    /// @return the axes, empty when `fvar` is absent
    public @Unmodifiable List<VariationAxis> variationAxes() {
        return fvar.axes();
    }

    /// Returns the `STAT` design axes in file order.
    ///
    /// @return the axes, empty when `STAT` is absent
    public @Unmodifiable List<StatAxis> statAxes() {
        return stat.axes();
    }

    /// Returns the `STAT` format-1 named instances in file order.
    ///
    /// @return the instances, empty when `STAT` is absent
    public @Unmodifiable List<StatNamedInstance> statNamedInstances() {
        return stat.namedInstances();
    }

    /// Returns `STAT.elidedFallbackNameID`, or `0` when the table is absent.
    ///
    /// @return the name ID
    public int statElidedFallbackNameId() {
        return stat.elidedFallbackNameId();
    }

    /// Returns the default variation instance, one coordinate per axis.
    ///
    /// @return the default coordinates
    public float @Unmodifiable [] defaultVariation() {
        return fvar.defaultInstance();
    }

    /// Returns the first `sbix` strike for `glyphId`.
    ///
    /// @param glyphId the glyph
    /// @return the bitmap, or `null` when the slot is empty or `sbix` is absent
    public @Nullable EmbeddedBitmap embeddedBitmap(int glyphId) {
        return sbix.glyph(glyphId);
    }

    /// Returns the first CBLC/CBDT format-1 strike for `glyphId`.
    ///
    /// @param glyphId the glyph
    /// @return the bitmap, or `null` when the slot is empty or the tables are absent
    public @Nullable EmbeddedBitmap colorBitmap(int glyphId) {
        return cbdt.glyph(glyphId);
    }

    /// Returns the first EBLC/EBDT format-1 strike for `glyphId`.
    ///
    /// @param glyphId the glyph
    /// @return the bitmap, or `null` when the slot is empty or the tables are absent
    public @Nullable EmbeddedBitmap grayscaleBitmap(int glyphId) {
        return ebdt.glyph(glyphId);
    }

    /// Returns the first uncompressed SVG document covering `glyphId`.
    ///
    /// @param glyphId the glyph
    /// @return the UTF-8 SVG document, or `null` when the table is absent or the glyph has no entry
    public @Nullable String svgDocument(int glyphId) {
        return svg.document(glyphId);
    }

    /// Returns `OS/2.usWeightClass`, or Regular when the table is absent.
    ///
    /// @return the weight class
    public int weightClass() {
        return os2.weightClass();
    }

    /// Returns `OS/2.usWidthClass`, or Medium when the table is absent.
    ///
    /// @return the width class
    public int widthClass() {
        return os2.widthClass();
    }

    /// Returns `OS/2.xAvgCharWidth`, or `0` when the table is absent.
    ///
    /// @return the average character width
    public int avgCharWidth() {
        return os2.avgCharWidth();
    }

    /// Returns `OS/2.usFirstCharIndex`, or `0` when the table is absent.
    ///
    /// @return the first Unicode BMP index
    public int firstCharIndex() {
        return os2.firstCharIndex();
    }

    /// Returns `OS/2.usLastCharIndex`, or `0` when the table is absent.
    ///
    /// @return the last Unicode BMP index
    public int lastCharIndex() {
        return os2.lastCharIndex();
    }

    /// Returns `OS/2.sxHeight`, or `0` when the table is absent or older than version 2.
    ///
    /// @return the x-height
    public int xHeight() {
        return os2.xHeight();
    }

    /// Returns `OS/2.sCapHeight`, or `0` when the table is absent or older than version 2.
    ///
    /// @return the cap height
    public int capHeight() {
        return os2.capHeight();
    }

    /// Returns `OS/2.usDefaultChar`, or `0` when the table is absent or older than version 2.
    ///
    /// @return the default character
    public int defaultChar() {
        return os2.defaultChar();
    }

    /// Returns `OS/2.usBreakChar`, or `0` when the table is absent or older than version 2.
    ///
    /// @return the break character
    public int breakChar() {
        return os2.breakChar();
    }

    /// Returns `OS/2.usMaxContext`, or `0` when the table is absent or older than version 2.
    ///
    /// @return the maximum lookup context
    public int maxContext() {
        return os2.maxContext();
    }

    /// Returns `OS/2.ulUnicodeRange1`, or `0` when the table is absent.
    ///
    /// @return the first Unicode range bits
    public int unicodeRange1() {
        return os2.unicodeRange1();
    }

    /// Returns `OS/2.ulUnicodeRange2`, or `0` when the table is absent.
    ///
    /// @return the second Unicode range bits
    public int unicodeRange2() {
        return os2.unicodeRange2();
    }

    /// Returns `OS/2.ulUnicodeRange3`, or `0` when the table is absent.
    ///
    /// @return the third Unicode range bits
    public int unicodeRange3() {
        return os2.unicodeRange3();
    }

    /// Returns `OS/2.ulUnicodeRange4`, or `0` when the table is absent.
    ///
    /// @return the fourth Unicode range bits
    public int unicodeRange4() {
        return os2.unicodeRange4();
    }

    /// Returns `OS/2.ulCodePageRange1`, or `0` when the table is absent or older than version 1.
    ///
    /// @return the first code-page range bits
    public int codePageRange1() {
        return os2.codePageRange1();
    }

    /// Returns `OS/2.ulCodePageRange2`, or `0` when the table is absent or older than version 1.
    ///
    /// @return the second code-page range bits
    public int codePageRange2() {
        return os2.codePageRange2();
    }

    /// Returns `OS/2.ySubscriptXSize`, or `0` when the table is absent.
    ///
    /// @return the subscript x size
    public int subscriptXSize() {
        return os2.subscriptXSize();
    }

    /// Returns `OS/2.ySubscriptYSize`, or `0` when the table is absent.
    ///
    /// @return the subscript y size
    public int subscriptYSize() {
        return os2.subscriptYSize();
    }

    /// Returns `OS/2.ySubscriptXOffset`, or `0` when the table is absent.
    ///
    /// @return the subscript x offset
    public int subscriptXOffset() {
        return os2.subscriptXOffset();
    }

    /// Returns `OS/2.ySubscriptYOffset`, or `0` when the table is absent.
    ///
    /// @return the subscript y offset
    public int subscriptYOffset() {
        return os2.subscriptYOffset();
    }

    /// Returns `OS/2.ySuperscriptXSize`, or `0` when the table is absent.
    ///
    /// @return the superscript x size
    public int superscriptXSize() {
        return os2.superscriptXSize();
    }

    /// Returns `OS/2.ySuperscriptYSize`, or `0` when the table is absent.
    ///
    /// @return the superscript y size
    public int superscriptYSize() {
        return os2.superscriptYSize();
    }

    /// Returns `OS/2.ySuperscriptXOffset`, or `0` when the table is absent.
    ///
    /// @return the superscript x offset
    public int superscriptXOffset() {
        return os2.superscriptXOffset();
    }

    /// Returns `OS/2.ySuperscriptYOffset`, or `0` when the table is absent.
    ///
    /// @return the superscript y offset
    public int superscriptYOffset() {
        return os2.superscriptYOffset();
    }

    /// Returns `OS/2.yStrikeoutSize`, or `0` when the table is absent.
    ///
    /// @return the strikeout size
    public int strikeoutSize() {
        return os2.strikeoutSize();
    }

    /// Returns `OS/2.yStrikeoutPosition`, or `0` when the table is absent.
    ///
    /// @return the strikeout position
    public int strikeoutPosition() {
        return os2.strikeoutPosition();
    }

    /// Returns `OS/2.sFamilyClass`, or `0` when the table is absent.
    ///
    /// @return the IBM family class
    public int familyClass() {
        return os2.familyClass();
    }

    /// Returns `OS/2.fsType`, or `0` when the table is absent.
    ///
    /// @return the embedding bits
    public int fsType() {
        return os2.fsType();
    }

    /// Returns `OS/2` PANOSE bytes, or ten zeros when the table is absent.
    ///
    /// @return the PANOSE classification
    public byte @Unmodifiable [] panose() {
        return os2.panose();
    }

    /// Returns `OS/2.achVendID`, or empty when the table is absent.
    ///
    /// @return the vendor tag
    public String vendorId() {
        return os2.vendorId();
    }

    /// Returns `OS/2.fsSelection`, or `0` when the table is absent.
    ///
    /// @return the selection bits
    public int fsSelection() {
        return os2.fsSelection();
    }

    /// Returns `OS/2.sTypoAscender`, or `0` when the table is absent.
    ///
    /// @return the typographic ascender
    public int typoAscender() {
        return os2.typoAscender();
    }

    /// Returns `OS/2.sTypoDescender`, or `0` when the table is absent.
    ///
    /// @return the typographic descender
    public int typoDescender() {
        return os2.typoDescender();
    }

    /// Returns `OS/2.sTypoLineGap`, or `0` when the table is absent.
    ///
    /// @return the typographic line gap
    public int typoLineGap() {
        return os2.typoLineGap();
    }

    /// Returns `OS/2.usWinAscent`, or `0` when the table is absent.
    ///
    /// @return the Windows ascender
    public int winAscent() {
        return os2.winAscent();
    }

    /// Returns `OS/2.usWinDescent`, or `0` when the table is absent.
    ///
    /// @return the Windows descender
    public int winDescent() {
        return os2.winDescent();
    }

    /// Returns the Windows Unicode or Macintosh copyright string.
    ///
    /// @return the copyright, or `null` when `name` has no `nameID 0` record
    public @Nullable String copyright() {
        return names.copyright();
    }

    /// Returns the Windows Unicode or Macintosh unique font identifier.
    ///
    /// @return the unique identifier, or `null` when `name` has no `nameID 3` record
    public @Nullable String uniqueId() {
        return names.uniqueId();
    }

    /// Returns the Windows Unicode or Macintosh family name.
    ///
    /// @return the family, or `null` when `name` has no `nameID 1` record
    public @Nullable String familyName() {
        return names.familyName();
    }

    /// Returns the Windows Unicode or Macintosh style name.
    ///
    /// @return the style, or `null` when `name` has no `nameID 2` record
    public @Nullable String styleName() {
        return names.styleName();
    }

    /// Returns the Windows Unicode or Macintosh full name.
    ///
    /// @return the full name, or `null` when `name` has no `nameID 4` record
    public @Nullable String fullName() {
        return names.fullName();
    }

    /// Returns the Windows Unicode or Macintosh version string.
    ///
    /// @return the version, or `null` when `name` has no `nameID 5` record
    public @Nullable String versionString() {
        return names.versionString();
    }

    /// Returns the Windows Unicode or Macintosh PostScript name.
    ///
    /// @return the PostScript name, or `null` when `name` has no `nameID 6` record
    public @Nullable String postScriptName() {
        return names.postScriptName();
    }

    /// Returns the Windows Unicode or Macintosh trademark string.
    ///
    /// @return the trademark, or `null` when `name` has no `nameID 7` record
    public @Nullable String trademark() {
        return names.trademark();
    }

    /// Returns the Windows Unicode or Macintosh manufacturer string.
    ///
    /// @return the manufacturer, or `null` when `name` has no `nameID 8` record
    public @Nullable String manufacturer() {
        return names.manufacturer();
    }

    /// Returns the Windows Unicode or Macintosh designer string.
    ///
    /// @return the designer, or `null` when `name` has no `nameID 9` record
    public @Nullable String designer() {
        return names.designer();
    }

    /// Returns the Windows Unicode or Macintosh description string.
    ///
    /// @return the description, or `null` when `name` has no `nameID 10` record
    public @Nullable String description() {
        return names.description();
    }

    /// Returns the Windows Unicode or Macintosh typographic family name.
    ///
    /// @return the typographic family, or `null` when `name` has no `nameID 16` record
    public @Nullable String typographicFamily() {
        return names.typographicFamily();
    }

    /// Returns the Windows Unicode or Macintosh typographic subfamily name.
    ///
    /// @return the typographic subfamily, or `null` when `name` has no `nameID 17` record
    public @Nullable String typographicSubfamily() {
        return names.typographicSubfamily();
    }

    /// Returns the Windows Unicode or Macintosh vendor URL.
    ///
    /// @return the vendor URL, or `null` when `name` has no `nameID 11` record
    public @Nullable String vendorUrl() {
        return names.vendorUrl();
    }

    /// Returns the Windows Unicode or Macintosh license string.
    ///
    /// @return the license, or `null` when `name` has no `nameID 13` record
    public @Nullable String license() {
        return names.license();
    }

    /// Returns the Windows Unicode or Macintosh designer URL.
    ///
    /// @return the designer URL, or `null` when `name` has no `nameID 12` record
    public @Nullable String designerUrl() {
        return names.designerUrl();
    }

    /// Returns the Windows Unicode or Macintosh license URL.
    ///
    /// @return the license URL, or `null` when `name` has no `nameID 14` record
    public @Nullable String licenseUrl() {
        return names.licenseUrl();
    }

    /// Returns the Windows Unicode or Macintosh WWS family name.
    ///
    /// @return the WWS family, or `null` when `name` has no `nameID 21` record
    public @Nullable String wwsFamily() {
        return names.wwsFamily();
    }

    /// Returns the Windows Unicode or Macintosh WWS subfamily name.
    ///
    /// @return the WWS subfamily, or `null` when `name` has no `nameID 22` record
    public @Nullable String wwsSubfamily() {
        return names.wwsSubfamily();
    }

    /// Returns the Windows Unicode or Macintosh sample text.
    ///
    /// @return the sample text, or `null` when `name` has no `nameID 19` record
    public @Nullable String sampleText() {
        return names.sampleText();
    }

    /// Returns the Windows Unicode or Macintosh compatible full name.
    ///
    /// @return the compatible full name, or `null` when `name` has no `nameID 18` record
    public @Nullable String compatibleFull() {
        return names.compatibleFull();
    }

    /// Returns the Windows Unicode or Macintosh PostScript CID findfont name.
    ///
    /// @return the CID name, or `null` when `name` has no `nameID 20` record
    public @Nullable String postScriptCid() {
        return names.postScriptCid();
    }

    /// Returns the Windows Unicode or Macintosh Variations PostScript name prefix.
    ///
    /// @return the prefix, or `null` when `name` has no `nameID 25` record
    public @Nullable String variationsPostScriptPrefix() {
        return names.variationsPostScriptPrefix();
    }

    /// Returns the Windows Unicode or Macintosh light-background palette name.
    ///
    /// @return the palette name, or `null` when `name` has no `nameID 23` record
    public @Nullable String lightBackgroundPalette() {
        return names.lightBackgroundPalette();
    }

    /// Returns the Windows Unicode or Macintosh dark-background palette name.
    ///
    /// @return the palette name, or `null` when `name` has no `nameID 24` record
    public @Nullable String darkBackgroundPalette() {
        return names.darkBackgroundPalette();
    }

    /// Returns `post.italicAngle` in degrees, or `0` when the table is absent.
    ///
    /// @return the italic angle
    public float italicAngle() {
        return post.italicAngle();
    }

    /// Returns `post.underlinePosition` in font units, or `0` when the table is absent.
    ///
    /// @return the underline position
    public int underlinePosition() {
        return post.underlinePosition();
    }

    /// Returns `post.underlineThickness` in font units, or `0` when the table is absent.
    ///
    /// @return the underline thickness
    public int underlineThickness() {
        return post.underlineThickness();
    }

    /// Returns whether `post.isFixedPitch` is set.
    ///
    /// @return whether the face is monospaced
    public boolean fixedPitch() {
        return post.fixedPitch();
    }

    /// Returns a big-endian glyf cursor, empty for a space or `.notdef` with no outline.
    ///
    /// @param glyphId the glyph id
    /// @return the glyf cursor
    public ByteBuffer glyf(int glyphId) {
        if (glyphId < 0 || glyphId + 1 >= loca.length) {
            throw new IllegalArgumentException("Unknown glyph " + glyphId);
        }
        int start = loca[glyphId];
        int end = loca[glyphId + 1];
        if (end < start) {
            throw new IllegalArgumentException("glyf loca range is inverted");
        }
        TableRecord glyf = requireTable("glyf");
        return cursor(data.asSlice((long) glyf.offset + (long) start, (long) end - (long) start));
    }

    /// Returns a big-endian table cursor.
    ///
    /// @param tag the table tag
    /// @return the table
    private ByteBuffer table(String tag) {
        TableRecord record = requireTable(tag);
        return cursor(data.asSlice(record.offset, record.length));
    }

    /// Returns a table cursor when `tag` is present.
    ///
    /// @param tag the table tag
    /// @return the table, or `null`
    private @Nullable ByteBuffer findTable(String tag) {
        @Nullable TableRecord record = tables.get(tag);
        if (record == null) {
            return null;
        }
        return cursor(data.asSlice(record.offset, record.length));
    }

    /// Requires a table record.
    ///
    /// @param tag the tag
    /// @return the record
    private TableRecord requireTable(String tag) {
        @Nullable TableRecord record = tables.get(tag);
        if (record == null) {
            throw new IllegalArgumentException("Missing SFNT table " + tag);
        }
        return record;
    }

    /// Reads Unicode format-4 and format-12 cmap subtables.
    ///
    /// @return the parsed tables; a missing format is empty
    private CmapTables readCmap() {
        ByteBuffer cmap = table("cmap");
        if (cmap.remaining() < 4) {
            throw new IllegalArgumentException("cmap is truncated");
        }
        cmap.getShort();
        int records = Short.toUnsignedInt(cmap.getShort());
        int format4 = -1;
        int format4Score = -1;
        int format12 = -1;
        int format12Score = -1;
        for (int index = 0; index < records; index++) {
            int platform = Short.toUnsignedInt(cmap.getShort());
            int encoding = Short.toUnsignedInt(cmap.getShort());
            int offset = cmap.getInt();
            if (platform != 0 && platform != 3) {
                continue;
            }
            if (offset < 0 || offset + 2 > cmap.limit()) {
                continue;
            }
            int mark = cmap.position();
            cmap.position(offset);
            int format = Short.toUnsignedInt(cmap.getShort());
            cmap.position(mark);
            int candidate = platform == 3 && (encoding == 1 || encoding == 10) ? 3 : platform == 0 ? 2 : 1;
            if (format == 4 && candidate > format4Score) {
                format4Score = candidate;
                format4 = offset;
            } else if (format == 12 && candidate > format12Score) {
                format12Score = candidate;
                format12 = offset;
            }
        }
        if (format4 < 0 && format12 < 0) {
            throw new IllegalArgumentException("cmap has no Unicode format-4 or format-12 record");
        }
        return new CmapTables(
                format4 < 0 ? CmapFormat4.empty() : parseFormat4(cmap, format4),
                format12 < 0 ? CmapFormat12.empty() : parseFormat12(cmap, format12)
        );
    }

    /// Parses a format-4 subtable at `offset`.
    ///
    /// @param cmap the cmap table
    /// @param offset the subtable offset
    /// @return the segments
    private static CmapFormat4 parseFormat4(ByteBuffer cmap, int offset) {
        cmap.position(offset);
        cmap.getShort();
        cmap.getShort();
        cmap.getShort();
        int segCount = Short.toUnsignedInt(cmap.getShort()) / 2;
        cmap.getShort();
        cmap.getShort();
        cmap.getShort();
        int[] endCodes = new int[segCount];
        for (int index = 0; index < segCount; index++) {
            endCodes[index] = Short.toUnsignedInt(cmap.getShort());
        }
        cmap.getShort();
        int[] startCodes = new int[segCount];
        short[] deltas = new short[segCount];
        int[] rangeOffsets = new int[segCount];
        for (int index = 0; index < segCount; index++) {
            startCodes[index] = Short.toUnsignedInt(cmap.getShort());
        }
        for (int index = 0; index < segCount; index++) {
            deltas[index] = cmap.getShort();
        }
        for (int index = 0; index < segCount; index++) {
            rangeOffsets[index] = Short.toUnsignedInt(cmap.getShort());
        }
        int remaining = cmap.remaining() / 2;
        int[] glyphIds = new int[remaining];
        for (int index = 0; index < remaining; index++) {
            glyphIds[index] = Short.toUnsignedInt(cmap.getShort());
        }
        return new CmapFormat4(startCodes, endCodes, deltas, rangeOffsets, glyphIds);
    }

    /// Parses a format-12 subtable at `offset`.
    ///
    /// @param cmap the cmap table
    /// @param offset the subtable offset
    /// @return the sequential map groups
    private static CmapFormat12 parseFormat12(ByteBuffer cmap, int offset) {
        cmap.position(offset);
        if (cmap.remaining() < 16) {
            throw new IllegalArgumentException("cmap format 12 is truncated");
        }
        cmap.getShort();
        cmap.getShort();
        cmap.getInt();
        cmap.getInt();
        int groups = cmap.getInt();
        if (groups < 0 || cmap.remaining() < groups * 12) {
            throw new IllegalArgumentException("cmap format 12 groups are truncated");
        }
        int[] startCodes = new int[groups];
        int[] endCodes = new int[groups];
        int[] startGlyphs = new int[groups];
        for (int index = 0; index < groups; index++) {
            startCodes[index] = cmap.getInt();
            endCodes[index] = cmap.getInt();
            startGlyphs[index] = cmap.getInt();
            if (Integer.compareUnsigned(endCodes[index], startCodes[index]) < 0) {
                throw new IllegalArgumentException("cmap format 12 group is inverted");
            }
        }
        return new CmapFormat12(startCodes, endCodes, startGlyphs);
    }

    /// Reads the `hhea` ascender.
    private int readAscender() {
        ByteBuffer hhea = table("hhea");
        if (hhea.remaining() < 6) {
            throw new IllegalArgumentException("hhea is truncated");
        }
        hhea.position(4);
        return hhea.getShort();
    }

    /// Reads advance widths from `hmtx`.
    ///
    /// @return the advances
    private int[] readAdvances() {
        ByteBuffer hhea = table("hhea");
        if (hhea.remaining() < 36) {
            throw new IllegalArgumentException("hhea is truncated");
        }
        hhea.position(34);
        int metricsCount = Short.toUnsignedInt(hhea.getShort());
        ByteBuffer hmtx = table("hmtx");
        int[] values = new int[glyphCount];
        int last = 0;
        for (int index = 0; index < glyphCount; index++) {
            if (index < metricsCount) {
                last = Short.toUnsignedInt(hmtx.getShort());
                hmtx.getShort();
            }
            values[index] = last;
        }
        return values;
    }

    /// Reads vertical advances from `vmtx`, or zeros when `vhea`/`vmtx` are absent.
    ///
    /// @return the advances
    private int[] readVerticalAdvances() {
        @Nullable ByteBuffer vhea = findTable("vhea");
        @Nullable ByteBuffer vmtx = findTable("vmtx");
        int[] values = new int[glyphCount];
        if (vhea == null || vmtx == null || vhea.remaining() < 36) {
            return values;
        }
        ByteBuffer header = vhea.duplicate().order(java.nio.ByteOrder.BIG_ENDIAN);
        header.position(34);
        int metricsCount = Short.toUnsignedInt(header.getShort());
        ByteBuffer metrics = vmtx.duplicate().order(java.nio.ByteOrder.BIG_ENDIAN);
        int last = 0;
        for (int index = 0; index < glyphCount; index++) {
            if (index < metricsCount && metrics.remaining() >= 4) {
                last = Short.toUnsignedInt(metrics.getShort());
                metrics.getShort();
            }
            values[index] = last;
        }
        return values;
    }

    /// Reads `loca` offsets.
    ///
    /// @param head the head table
    /// @return the offsets
    private int[] readLoca(ByteBuffer head) {
        head.position(50);
        int format = head.getShort();
        ByteBuffer locaTable = table("loca");
        int[] offsets = new int[glyphCount + 1];
        for (int index = 0; index < offsets.length; index++) {
            offsets[index] = format == 0
                    ? Short.toUnsignedInt(locaTable.getShort()) * 2
                    : locaTable.getInt();
        }
        return offsets;
    }

    /// Returns a big-endian cursor over `segment`.
    ///
    /// @param segment the slice
    /// @return the cursor
    private static ByteBuffer cursor(MemorySegment segment) {
        return segment.asByteBuffer().order(ByteOrder.BIG_ENDIAN);
    }

    /// Stores one table directory record.
    ///
    /// @param offset the file offset
    /// @param length the length
    private record TableRecord(int offset, int length) {
    }

    /// Stores parsed Unicode cmap subtables.
    ///
    /// @param format4 the format-4 segments
    /// @param format12 the format-12 groups
    private record CmapTables(CmapFormat4 format4, CmapFormat12 format12) {
    }

    /// Stores a parsed format-4 cmap.
    ///
    /// @param startCodes start codes
    /// @param endCodes end codes
    /// @param idDeltas id deltas
    /// @param idRangeOffsets range offsets
    /// @param glyphIds glyph id array
    private record CmapFormat4(
            int[] startCodes,
            int[] endCodes,
            short[] idDeltas,
            int[] idRangeOffsets,
            int[] glyphIds
    ) {
        /// Returns an empty format-4 table.
        ///
        /// @return empty segments
        private static CmapFormat4 empty() {
            return new CmapFormat4(new int[0], new int[0], new short[0], new int[0], new int[0]);
        }
    }

    /// Stores a parsed format-12 cmap.
    ///
    /// @param startCodes start code points
    /// @param endCodes end code points
    /// @param startGlyphs start glyph ids
    private record CmapFormat12(int[] startCodes, int[] endCodes, int[] startGlyphs) {
        /// Returns an empty format-12 table.
        ///
        /// @return empty groups
        private static CmapFormat12 empty() {
            return new CmapFormat12(new int[0], new int[0], new int[0]);
        }
    }
}
