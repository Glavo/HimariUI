package org.glavo.himari.text;

import org.glavo.himari.font.GlyphLigature;
import org.glavo.himari.font.MarkPlacement;
import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/// Maps clusters through NFC, `cmap` and `hmtx`, applying first-stable script presentation.
///
/// Latin, Greek, Cyrillic, Han, and Kana use one-to-one `cmap` mapping. Arabic joining letters
/// apply GSUB `isol`/`init`/`medi`/`fina` when the font lists those features, otherwise they
/// select Presentation Forms-B when the font maps those forms. LAM plus an alef variant compose
/// onto Presentation Forms-B lam-alef when the font maps the ligature; transparent marks between
/// LAM and alef stay in the LAM cluster. LAM plus LAM plus HEH compose onto isolated Allah
/// `U+FDF2` when the font maps that ligature. Alef wasla `U+0671` joins as a right-joining letter
/// and maps onto `U+FB50`/`U+FB51`. Ligature and multi-code-point compositions set
/// [`ShapedGlyph#unsafeToBreak()`]. Arabic shadda plus tanwin/vowel/superscript alef compose onto
/// Presentation Forms-A when the font maps those forms. Hebrew letter-plus-mark pairs compose onto Presentation
/// Forms-A when the font maps the composed form. Word-final kaf/mem/nun/pe/tsadi select final
/// letters. Yiddish double-vav, vav-yod, and double-yod map to `U+05F0`–`U+05F2`. Hangul
/// choseong/jungseong/jongseong sequences, including Hangul Compatibility Jamo, compose onto
/// syllables when the font maps the syllable; a precomposed syllable missing from `cmap`
/// decomposes onto modern jamo. Thai and Lao decompose SARA AM and reorder Nikhahit over
/// above-base marks; left vowels stay in Unicode visual order. Lao ho plus no/mo compose onto
/// `U+0EDC` / `U+0EDD` when the font maps those ligatures. The glyph stream then applies GSUB type-2 `ccmp` decompositions and
/// type-4 `rlig`/`liga` ligatures when the font lists those lookups. GSUB `calt` then applies
/// type-5 two-glyph context and type-6 one-lookahead chain substitutions. Consecutive pairs then
/// receive GPOS type-2 or format-0 `kern` X-advance adjustments, plus `IgnoreMarks` and
/// `MarkAttachmentType` pair and chain lookups. Marks covered by GPOS type 4 attach to the
/// preceding base.
/// U+00AD is emitted with a zero advance so unused soft hyphens do not occupy the line.
/// The shaper does not write editor state and does not reorder RTL runs.
@NotNullByDefault
public final class DefaultShaper {
    /// Prevents instantiation.
    private DefaultShaper() {
    }

    /// Shapes a string.
    ///
    /// @param font the font
    /// @param text the source text
    /// @return the shaped glyphs
    public static @Unmodifiable List<ShapedGlyph> shape(SfntFont font, String text) {
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(text, "text");
        text = UnicodeNormalize.nfc(text);
        int utf16Length = text.length();
        if (utf16Length == 0) {
            return List.of();
        }
        int count = text.codePointCount(0, utf16Length);
        if (!needsPresentation(text, utf16Length)) {
            return shapeSimple(font, text, utf16Length, count);
        }
        return shapeComplex(font, text, utf16Length, count);
    }

    /// Maps one-to-one clusters without presentation substitution.
    private static @Unmodifiable List<ShapedGlyph> shapeSimple(
            SfntFont font,
            String text,
            int utf16Length,
            int count
    ) {
        ShapedGlyph[] glyphs = new ShapedGlyph[count];
        int cluster = 0;
        for (int index = 0; index < utf16Length; ) {
            int codePoint = text.codePointAt(index);
            int glyphId = font.glyphId(codePoint);
            glyphs[cluster] = new ShapedGlyph(
                    codePoint,
                    glyphId,
                    cluster,
                    advanceOf(font, codePoint, glyphId)
            );
            cluster++;
            index += Character.charCount(codePoint);
        }
        glyphs = applyDecompositions(font, glyphs, count);
        int simpleCount = applyLigatures(font, glyphs, glyphs.length);
        applyContext(font, glyphs, simpleCount);
        applyReverse(font, glyphs, simpleCount);
        applyPairs(font, glyphs, simpleCount);
        applyMarks(font, glyphs, simpleCount);
        if (simpleCount != count) {
            glyphs = Arrays.copyOf(glyphs, simpleCount);
        }
        return Collections.unmodifiableList(Arrays.asList(glyphs));
    }

    /// Applies Arabic joining forms and Hebrew mark composition, then maps through `cmap`.
    private static @Unmodifiable List<ShapedGlyph> shapeComplex(
            SfntFont font,
            String text,
            int utf16Length,
            int count
    ) {
        int[] points = new int[count];
        int decoded = 0;
        for (int index = 0; index < utf16Length; ) {
            int codePoint = text.codePointAt(index);
            points[decoded++] = codePoint;
            index += Character.charCount(codePoint);
        }
        int[] clusters = identityClusters(count);
        @Nullable ThaiLao.Expansion thai = ThaiLao.expand(points, count);
        if (thai != null) {
            points = thai.points();
            clusters = thai.clusters();
            count = thai.count();
        }
        ArabicForm[] forms = new ArabicForm[count];
        ArabicJoining.forms(points, count, forms);
        ShapedGlyph[] glyphs = new ShapedGlyph[count];
        int written = 0;
        int lastLetterCluster = 0;
        for (int index = 0; index < count; ) {
            int codePoint = points[index];
            int mapped = codePoint;
            int consumed = 1;
            int cluster = clusters[index];
            int lead = HangulSyllable.asLead(codePoint);
            int vowel = index + 1 < count ? HangulSyllable.asVowel(points[index + 1]) : 0;
            if (lead != 0 && vowel != 0) {
                int trail = 0;
                int hangulConsumed = 2;
                if (index + 2 < count) {
                    int mappedTrail = HangulSyllable.asTrail(points[index + 2]);
                    if (mappedTrail != 0) {
                        trail = mappedTrail;
                        hangulConsumed = 3;
                    }
                }
                int syllable = HangulSyllable.compose(lead, vowel, trail);
                if (syllable != 0 && font.glyphId(syllable) != 0) {
                    mapped = syllable;
                    consumed = hangulConsumed;
                }
            }
            if (consumed == 1 && HangulSyllable.isSyllable(codePoint) && font.glyphId(codePoint) == 0) {
                int[] parts = HangulSyllable.decompose(codePoint);
                if (written + parts.length > glyphs.length) {
                    glyphs = Arrays.copyOf(glyphs, written + parts.length + (count - index));
                }
                for (int part : parts) {
                    int partGlyph = font.glyphId(part);
                    glyphs[written++] = new ShapedGlyph(
                            part,
                            partGlyph,
                            cluster,
                            advanceOf(font, part, partGlyph)
                    );
                }
                lastLetterCluster = cluster;
                index++;
                continue;
            }
            if (consumed == 1 && index + 2 < count) {
                int triple = HebrewPresentation.compose(codePoint, points[index + 1], points[index + 2]);
                if (triple != 0 && font.glyphId(triple) != 0) {
                    mapped = triple;
                    consumed = 3;
                }
            }
            if (consumed == 1 && index + 1 < count) {
                int yiddish = HebrewPresentation.yiddishLigature(codePoint, points[index + 1]);
                if (yiddish != 0 && font.glyphId(yiddish) != 0) {
                    mapped = yiddish;
                    consumed = 2;
                }
            }
            if (consumed == 1 && index + 1 < count) {
                int shadda = ArabicPresentation.shaddaLigature(codePoint, points[index + 1]);
                if (shadda != 0 && font.glyphId(shadda) != 0) {
                    mapped = shadda;
                    consumed = 2;
                }
            }
            if (consumed == 1 && index + 1 < count) {
                int lao = ThaiLao.laoLigature(codePoint, points[index + 1]);
                if (lao != 0 && font.glyphId(lao) != 0) {
                    mapped = lao;
                    consumed = 2;
                }
            }
            if (consumed == 1 && index + 1 < count) {
                int composed = HebrewPresentation.compose(codePoint, points[index + 1]);
                if (composed != 0 && font.glyphId(composed) != 0) {
                    mapped = composed;
                    consumed = 2;
                }
            }
            if (consumed == 1 && HebrewPresentation.isLetter(codePoint)
                    && !followingHebrewLetter(points, count, index)) {
                int fin = HebrewPresentation.finalForm(codePoint);
                if (fin != 0 && font.glyphId(fin) != 0) {
                    mapped = fin;
                }
            }
            if (consumed == 1 && ArabicPresentation.isLam(codePoint)) {
                int allah = ArabicPresentation.allahLigature(points, index, count);
                if (allah != 0 && font.glyphId(allah) != 0) {
                    int length = ArabicPresentation.allahLength(points, index, count);
                    int ligatureGlyph = font.glyphId(allah);
                    glyphs[written++] = new ShapedGlyph(
                            allah,
                            ligatureGlyph,
                            cluster,
                            advanceOf(font, allah, ligatureGlyph),
                            0,
                            0,
                            0,
                            true
                    );
                    lastLetterCluster = cluster;
                    index += length;
                    continue;
                }
                int alefIndex = index + 1;
                while (alefIndex < count && ArabicJoining.isTransparent(points[alefIndex])) {
                    alefIndex++;
                }
                if (alefIndex < count && ArabicPresentation.isAlef(points[alefIndex])) {
                    int ligature = ArabicPresentation.lamAlef(points[alefIndex], forms[index]);
                    if (ligature != 0 && font.glyphId(ligature) != 0) {
                        int ligatureGlyph = font.glyphId(ligature);
                        glyphs[written++] = new ShapedGlyph(
                                ligature,
                                ligatureGlyph,
                                cluster,
                                advanceOf(font, ligature, ligatureGlyph),
                                0,
                                0,
                                0,
                                true
                        );
                        lastLetterCluster = cluster;
                        for (int mark = index + 1; mark < alefIndex; mark++) {
                            int markPoint = points[mark];
                            int markGlyph = font.glyphId(markPoint);
                            glyphs[written++] = new ShapedGlyph(
                                    markPoint,
                                    markGlyph,
                                    lastLetterCluster,
                                    advanceOf(font, markPoint, markGlyph)
                            );
                        }
                        index = alefIndex + 1;
                        continue;
                    }
                }
            }
            int glyphId;
            if (consumed == 1 && forms[index] != ArabicForm.NONE) {
                int nominalGlyph = font.glyphId(codePoint);
                int substituted = font.substitute(nominalGlyph, forms[index].featureTag());
                if (substituted != nominalGlyph) {
                    glyphId = substituted;
                } else {
                    int presentation = ArabicPresentation.apply(codePoint, forms[index]);
                    if (presentation != codePoint && font.glyphId(presentation) != 0) {
                        mapped = presentation;
                    }
                    glyphId = font.glyphId(mapped);
                }
            } else {
                glyphId = font.glyphId(mapped);
            }
            if (consumed == 1 && ArabicJoining.type(codePoint) == JoiningType.TRANSPARENT && written > 0) {
                cluster = lastLetterCluster;
            } else {
                lastLetterCluster = cluster;
            }
            glyphs[written] = new ShapedGlyph(
                    mapped,
                    glyphId,
                    cluster,
                    advanceOf(font, mapped, glyphId),
                    0,
                    0,
                    0,
                    consumed > 1
            );
            written++;
            index += consumed;
        }
        glyphs = applyDecompositions(font, glyphs, written);
        written = applyLigatures(font, glyphs, glyphs.length);
        if (written != glyphs.length) {
            glyphs = Arrays.copyOf(glyphs, written);
        }
        applyContext(font, glyphs, written);
        applyReverse(font, glyphs, written);
        applyPairs(font, glyphs, written);
        applyMarks(font, glyphs, written);
        return Collections.unmodifiableList(Arrays.asList(glyphs));
    }

    /// Returns whether a Hebrew letter follows `index`, skipping combining marks.
    private static boolean followingHebrewLetter(int[] points, int count, int index) {
        for (int cursor = index + 1; cursor < count; cursor++) {
            int codePoint = points[cursor];
            if (HebrewPresentation.isMark(codePoint)) {
                continue;
            }
            return HebrewPresentation.isLetter(codePoint);
        }
        return false;
    }

    /// Returns the layout advance, forcing U+00AD to zero so an unused soft hyphen does not take
    /// space.
    ///
    /// @param font the face
    /// @param codePoint the mapped code point
    /// @param glyphId the mapped glyph
    /// @return the nonnegative advance
    private static int advanceOf(SfntFont font, int codePoint, int glyphId) {
        if (codePoint == 0x00AD || BidiOrder.isControl(codePoint)) {
            return 0;
        }
        return font.metrics(glyphId).advanceWidth();
    }

    /// Collapses GSUB type-4 `rlig` then `liga` matches in place.
    ///
    /// @param font the face
    /// @param glyphs the mapped glyphs
    /// @param count the used length
    /// @return the used length after ligatures
    private static int applyLigatures(SfntFont font, ShapedGlyph[] glyphs, int count) {
        if (count < 2) {
            return count;
        }
        int[] ids = new int[count];
        for (int index = 0; index < count; index++) {
            ids[index] = glyphs[index].glyphId();
        }
        int written = 0;
        int index = 0;
        while (index < count) {
            int remaining = count - index;
            @Nullable GlyphLigature match = font.ligature(ids, index, remaining, SfntFont.TAG_RLIG);
            if (match == null) {
                match = font.ligature(ids, index, remaining, SfntFont.TAG_LIGA);
            }
            if (match == null) {
                glyphs[written++] = glyphs[index++];
                continue;
            }
            ShapedGlyph first = glyphs[index];
            int ligatureId = match.glyphId();
            glyphs[written++] = new ShapedGlyph(
                    first.codePoint(),
                    ligatureId,
                    first.cluster(),
                    advanceOf(font, first.codePoint(), ligatureId),
                    first.xOffset(),
                    first.yOffset(),
                    first.fontIndex(),
                    true
            );
            index += match.consumed();
        }
        return written;
    }

    /// Applies GSUB type-2 `ccmp` sequences, expanding one glyph into one or more glyphs.
    private static ShapedGlyph[] applyDecompositions(SfntFont font, ShapedGlyph[] glyphs, int count) {
        int extra = 0;
        int[][] sequences = null;
        for (int index = 0; index < count; index++) {
            int @Nullable [] sequence = font.decompose(glyphs[index].glyphId(), SfntFont.TAG_CCMP);
            if (sequence == null || sequence.length == 0) {
                continue;
            }
            if (sequences == null) {
                sequences = new int[count][];
            }
            sequences[index] = sequence;
            extra += sequence.length - 1;
        }
        if (sequences == null) {
            return count == glyphs.length ? glyphs : Arrays.copyOf(glyphs, count);
        }
        ShapedGlyph[] expanded = new ShapedGlyph[count + extra];
        int written = 0;
        for (int index = 0; index < count; index++) {
            int @Nullable [] sequence = sequences[index];
            ShapedGlyph glyph = glyphs[index];
            if (sequence == null) {
                expanded[written++] = glyph;
                continue;
            }
            for (int glyphId : sequence) {
                expanded[written++] = new ShapedGlyph(
                        glyph.codePoint(),
                        glyphId,
                        glyph.cluster(),
                        advanceOf(font, glyph.codePoint(), glyphId),
                        glyph.xOffset(),
                        glyph.yOffset(),
                        glyph.fontIndex()
                );
            }
        }
        return expanded;
    }

    /// Applies GSUB type-8 reverse-chain substitutions from the end of the run.
    private static void applyReverse(SfntFont font, ShapedGlyph[] glyphs, int count) {
        int[] glyphIds = new int[count];
        for (int index = 0; index < count; index++) {
            glyphIds[index] = glyphs[index].glyphId();
        }
        for (int index = count - 1; index >= 0; index--) {
            if (index + 1 >= count) {
                continue;
            }
            int current = glyphIds[index];
            int nextId = font.reverseSubstitute(glyphIds, index, count - index, SfntFont.TAG_CALT);
            if (nextId == current) {
                continue;
            }
            glyphIds[index] = nextId;
            ShapedGlyph glyph = glyphs[index];
            glyphs[index] = new ShapedGlyph(
                    glyph.codePoint(),
                    nextId,
                    glyph.cluster(),
                    advanceOf(font, glyph.codePoint(), nextId),
                    glyph.xOffset(),
                    glyph.yOffset(),
                    glyph.fontIndex()
            );
        }
    }

    /// Applies GSUB `calt` type-5 and type-6 substitutions in place.
    private static void applyContext(SfntFont font, ShapedGlyph[] glyphs, int count) {
        int[] glyphIds = new int[count];
        for (int index = 0; index < count; index++) {
            glyphIds[index] = glyphs[index].glyphId();
        }
        for (int index = 0; index < count; index++) {
            int current = glyphIds[index];
            int remaining = count - index;
            int nextId = remaining > 1
                    ? font.contextSubstitute(glyphIds, index, remaining, SfntFont.TAG_CALT)
                    : current;
            if (remaining > 1) {
                int chained = font.chainSubstitute(glyphIds, index, remaining, SfntFont.TAG_CALT);
                if (chained != current) {
                    nextId = chained;
                }
            }
            if (nextId == current) {
                continue;
            }
            glyphIds[index] = nextId;
            ShapedGlyph glyph = glyphs[index];
            glyphs[index] = new ShapedGlyph(
                    glyph.codePoint(),
                    nextId,
                    glyph.cluster(),
                    advanceOf(font, glyph.codePoint(), nextId),
                    glyph.xOffset(),
                    glyph.yOffset(),
                    glyph.fontIndex()
            );
        }
    }

    /// Applies GPOS/`kern` pair X-advance deltas in place and clamps each advance to be nonnegative.
    ///
    /// Adjacent, `IgnoreMarks`, `MarkAttachmentType`, `IgnoreBaseGlyphs`, `IgnoreLigatures`, and
    /// `UseMarkFilteringSet` lookups all apply through [`SfntFont#pairAdjustment(int[], int, int)`]
    /// and [`SfntFont#chainAdjustment(int[], int, int)`].
    private static void applyPairs(SfntFont font, ShapedGlyph[] glyphs, int count) {
        int[] glyphIds = new int[count];
        for (int index = 0; index < count; index++) {
            glyphIds[index] = glyphs[index].glyphId();
        }
        for (int index = 0; index < count; index++) {
            int remaining = count - index;
            int delta = font.singleAdjustment(glyphIds[index]);
            if (remaining > 1) {
                delta += font.pairAdjustment(glyphIds, index, remaining);
            }
            if (remaining > 2) {
                delta += font.chainAdjustment(glyphIds, index, remaining);
            }
            if (delta == 0) {
                continue;
            }
            int advance = glyphs[index].xAdvance() + delta;
            if (advance < 0) {
                advance = 0;
            }
            ShapedGlyph current = glyphs[index];
            glyphs[index] = new ShapedGlyph(
                    current.codePoint(),
                    current.glyphId(),
                    current.cluster(),
                    advance,
                    current.xOffset(),
                    current.yOffset(),
                    current.fontIndex()
            );
        }
    }

    /// Attaches GPOS marks to the preceding mark, then to the preceding base.
    private static void applyMarks(SfntFont font, ShapedGlyph[] glyphs, int count) {
        int baseIndex = -1;
        for (int index = 0; index < count; index++) {
            int glyphId = glyphs[index].glyphId();
            if (font.isMark(glyphId)) {
                @Nullable MarkPlacement placement = null;
                if (index > 0) {
                    placement = font.markPlacement(glyphId, glyphs[index - 1].glyphId());
                }
                if (placement == null && baseIndex >= 0) {
                    placement = font.markPlacement(glyphId, glyphs[baseIndex].glyphId());
                }
                if (placement == null) {
                    continue;
                }
                ShapedGlyph current = glyphs[index];
                glyphs[index] = new ShapedGlyph(
                        current.codePoint(),
                        current.glyphId(),
                        current.cluster(),
                        current.xAdvance(),
                        placement.xOffset(),
                        placement.yOffset(),
                        current.fontIndex()
                );
            } else {
                baseIndex = index;
            }
        }
    }

    /// Returns one cluster identity per code point.
    ///
    /// @param count the code-point count
    /// @return `0 .. count-1`
    private static int[] identityClusters(int count) {
        int[] clusters = new int[count];
        for (int index = 0; index < count; index++) {
            clusters[index] = index;
        }
        return clusters;
    }

    /// Returns whether `text` needs script presentation analysis.
    private static boolean needsPresentation(String text, int utf16Length) {
        for (int index = 0; index < utf16Length; ) {
            int codePoint = text.codePointAt(index);
            if (ArabicJoining.isArabicLetter(codePoint)
                    || HebrewPresentation.isLetter(codePoint)
                    || codePoint == 0x05B9
                    || codePoint == 0x05BA
                    || codePoint == 0x05C7
                    || HangulSyllable.isJamo(codePoint)
                    || HangulSyllable.isSyllable(codePoint)
                    || ThaiLao.isThaiOrLao(codePoint)
                    || ArabicJoining.isTransparent(codePoint)
                    || codePoint == 0x200C
                    || codePoint == 0x200D) {
                return true;
            }
            index += Character.charCount(codePoint);
        }
        return false;
    }
}
