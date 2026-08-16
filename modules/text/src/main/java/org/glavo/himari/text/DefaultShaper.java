package org.glavo.himari.text;

import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/// Maps clusters through `cmap` and `hmtx`, applying first-stable script presentation.
///
/// Latin, Greek, Cyrillic, Han, and Kana use one-to-one `cmap` mapping. Arabic joining letters
/// apply GSUB `isol`/`init`/`medi`/`fina` when the font lists those features, otherwise they
/// select Presentation Forms-B when the font maps those forms. Hebrew letter-plus-mark pairs
/// compose onto Presentation Forms-A when the font maps the composed form. Hangul
/// choseong/jungseong/jongseong sequences compose onto syllables when the font maps the syllable.
/// Thai and Lao decompose SARA AM and reorder Nikhahit over above-base marks; left vowels stay
/// in Unicode visual order. Consecutive pairs then receive GPOS type-2 or format-0 `kern`
/// X-advance adjustments. The shaper does not write editor state and does not reorder RTL runs.
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
                    font.metrics(glyphId).advanceWidth()
            );
            cluster++;
            index += Character.charCount(codePoint);
        }
        applyPairs(font, glyphs, count);
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
            if (index + 1 < count && HangulSyllable.isLead(codePoint) && HangulSyllable.isVowel(points[index + 1])) {
                int trail = 0;
                int hangulConsumed = 2;
                if (index + 2 < count && HangulSyllable.isTrail(points[index + 2])) {
                    trail = points[index + 2];
                    hangulConsumed = 3;
                }
                int syllable = HangulSyllable.compose(codePoint, points[index + 1], trail);
                if (syllable != 0 && font.glyphId(syllable) != 0) {
                    mapped = syllable;
                    consumed = hangulConsumed;
                }
            }
            if (consumed == 1 && index + 1 < count) {
                int composed = HebrewPresentation.compose(codePoint, points[index + 1]);
                if (composed != 0 && font.glyphId(composed) != 0) {
                    mapped = composed;
                    consumed = 2;
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
                    font.metrics(glyphId).advanceWidth()
            );
            written++;
            index += consumed;
        }
        if (written != count) {
            glyphs = Arrays.copyOf(glyphs, written);
        }
        applyPairs(font, glyphs, written);
        return Collections.unmodifiableList(Arrays.asList(glyphs));
    }

    /// Applies GPOS/`kern` pair X-advance deltas in place and clamps each advance to be nonnegative.
    private static void applyPairs(SfntFont font, ShapedGlyph[] glyphs, int count) {
        for (int index = 0; index < count - 1; index++) {
            int delta = font.pairAdjustment(glyphs[index].glyphId(), glyphs[index + 1].glyphId());
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
                    advance
            );
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
                    || HangulSyllable.isJamo(codePoint)
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
