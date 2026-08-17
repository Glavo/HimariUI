package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/// Applies GSUB lookup types 1–6 and 8. Type 7 ExtensionSubst Format 1 unwraps onto those types.
///
/// Other lookup types are skipped. Missing tables, unknown features, and glyphs outside coverage
/// leave the input identity unchanged. Type-5 two-glyph rules and type-6 one-lookahead rules
/// resolve a nested type-1 lookup on the first input glyph. Type-2 sequences replace one glyph.
/// Type-3 returns the first alternate. Type-8 reverse chaining substitutes one covered glyph when
/// the following glyph is in the lookahead coverage. Type-6 chain rules may require one
/// preceding backtrack glyph. Ligature and context lookups with
/// `IgnoreMarks` skip GDEF class-3 marks. Lookups with a non-zero `MarkAttachmentType` skip
/// marks whose GDEF mark-attach class differs from that value.
@NotNullByDefault
final class GsubSubstitutions {
    /// Empty substitutions.
    static final GsubSubstitutions NONE = new GsubSubstitutions(new Feature[0], GdefTable.NONE);

    /// Features in table order.
    private final Feature[] features;

    /// GDEF classes used by `IgnoreMarks` ligature matching.
    private final GdefTable gdef;

    /// Creates a substitution table.
    ///
    /// @param features the features
    /// @param gdef the GDEF classes
    private GsubSubstitutions(Feature[] features, GdefTable gdef) {
        this.features = features;
        this.gdef = gdef;
    }

    /// Applies every type-1 lookup listed by `featureTag`.
    ///
    /// @param glyphId the input glyph
    /// @param featureTag a four-byte OpenType tag as a big-endian `int`
    /// @return the substituted glyph, or `glyphId`
    int apply(int glyphId, int featureTag) {
        int current = glyphId;
        for (Feature feature : features) {
            if (feature.tag != featureTag) {
                continue;
            }
            for (SingleSubst subst : feature.singles) {
                current = subst.apply(current);
            }
        }
        return current;
    }

    /// Applies the first type-2 multiple substitution listed by `featureTag`.
    ///
    /// @param glyphId the input glyph
    /// @param featureTag a four-byte OpenType tag
    /// @return the substitute sequence, or `null` when the glyph is unchanged
    int @Nullable [] decompose(int glyphId, int featureTag) {
        for (Feature feature : features) {
            if (feature.tag != featureTag) {
                continue;
            }
            for (MultipleSubst subst : feature.multiples) {
                int @Nullable [] sequence = subst.apply(glyphId);
                if (sequence != null) {
                    return sequence;
                }
            }
        }
        return null;
    }

    /// Applies the first type-3 alternate listed by `featureTag`.
    ///
    /// @param glyphId the input glyph
    /// @param featureTag a four-byte OpenType tag
    /// @return the first alternate, or `glyphId`
    int alternate(int glyphId, int featureTag) {
        int current = glyphId;
        for (Feature feature : features) {
            if (feature.tag != featureTag) {
                continue;
            }
            for (AlternateSubst subst : feature.alternates) {
                current = subst.apply(current);
            }
        }
        return current;
    }

    /// Applies the first type-4 ligature listed by `featureTag` at `start`.
    ///
    /// @param glyphIds the mapped glyph identities
    /// @param start the first glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @param featureTag a four-byte OpenType tag as a big-endian `int`
    /// @return the match, or `null`
    @Nullable GlyphLigature ligature(int[] glyphIds, int start, int remaining, int featureTag) {
        Objects.requireNonNull(glyphIds, "glyphIds");
        if (start < 0 || remaining < 2 || start + remaining > glyphIds.length) {
            return null;
        }
        for (Feature feature : features) {
            if (feature.tag != featureTag) {
                continue;
            }
            for (LigatureSubst subst : feature.ligatures) {
                @Nullable GlyphLigature match = subst.apply(glyphIds, start, remaining, gdef);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    /// Applies the first type-5 two-glyph context rule listed by `featureTag`.
    ///
    /// @param current the first input glyph
    /// @param next the second input glyph
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or `current`
    int contextSubstitute(int current, int next, int featureTag) {
        return contextSubstitute(current, next, next, featureTag);
    }

    /// Applies the first type-5 two-glyph context rule, optionally skipping marks.
    ///
    /// @param current the first input glyph
    /// @param next the immediately following glyph
    /// @param skippedNext the first non-mark after `current`
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or `current`
    int contextSubstitute(int current, int next, int skippedNext, int featureTag) {
        return contextSubstitute(current, next, skippedNext, next, featureTag);
    }

    /// Applies a type-5 rule, using `attachSkipped` when the lookup has `MarkAttachmentType`.
    ///
    /// @param current the first input glyph
    /// @param next the immediately following glyph
    /// @param skippedNext the first non-mark after `current`
    /// @param attachSkipped the first glyph a `MarkAttachmentType` lookup does not skip
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or `current`
    int contextSubstitute(int current, int next, int skippedNext, int attachSkipped, int featureTag) {
        for (Feature feature : features) {
            if (feature.tag != featureTag) {
                continue;
            }
            for (ContextRule rule : feature.contexts) {
                int candidate = ruleCandidate(rule.ignoreMarks, rule.attachType, next, skippedNext, attachSkipped);
                if (rule.current == current && rule.next == candidate) {
                    return rule.substitute;
                }
            }
        }
        return current;
    }

    /// Applies a type-5 rule by walking `glyphIds` with the lookup skip flags.
    ///
    /// @param glyphIds the mapped glyphs
    /// @param start the first glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or the glyph at `start`
    int contextSubstitute(int[] glyphIds, int start, int remaining, int featureTag) {
        Objects.requireNonNull(glyphIds, "glyphIds");
        if (start < 0 || remaining < 2 || start + remaining > glyphIds.length) {
            return start >= 0 && start < glyphIds.length ? glyphIds[start] : 0;
        }
        int current = glyphIds[start];
        for (Feature feature : features) {
            if (feature.tag != featureTag) {
                continue;
            }
            for (ContextRule rule : feature.contexts) {
                int candidateIndex = gdef.firstKeptIndex(
                        glyphIds,
                        start + 1,
                        start + remaining,
                        rule.lookupFlag,
                        rule.markSet
                );
                int candidate = candidateIndex >= 0 ? glyphIds[candidateIndex] : -1;
                if (rule.current == current && rule.next == candidate) {
                    return rule.substitute;
                }
            }
        }
        return current;
    }

    /// Applies the first type-6 one-lookahead chain rule listed by `featureTag`.
    ///
    /// @param current the first input glyph
    /// @param next the second input glyph
    /// @param lookahead the first lookahead glyph
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or `current`
    int chainSubstitute(int current, int next, int lookahead, int featureTag) {
        return chainSubstitute(current, next, lookahead, next, lookahead, featureTag);
    }

    /// Applies the first type-6 chain rule, optionally skipping marks.
    ///
    /// @param current the first input glyph
    /// @param next the immediately following glyph
    /// @param lookahead the first lookahead glyph
    /// @param skippedNext the first non-mark after `current`
    /// @param skippedLookahead the first non-mark after `skippedNext`
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or `current`
    int chainSubstitute(
            int current,
            int next,
            int lookahead,
            int skippedNext,
            int skippedLookahead,
            int featureTag
    ) {
        for (Feature feature : features) {
            if (feature.tag != featureTag) {
                continue;
            }
            for (ChainRule rule : feature.chains) {
                int candidateNext = ruleCandidate(rule.ignoreMarks, rule.attachType, next, skippedNext, skippedNext);
                int candidateLook = ruleCandidate(
                        rule.ignoreMarks,
                        rule.attachType,
                        lookahead,
                        skippedLookahead,
                        skippedLookahead
                );
                if (rule.backtrack != 0
                        || rule.backtrackFar != 0
                        || rule.backtrackFarther != 0
                        || rule.backtrackFarthest != 0
                        || rule.backtrackFifth != 0
                        || rule.backtrackSixth != 0
                        || rule.backtrackSeventh != 0
                        || rule.backtrackEighth != 0
                        || rule.backtrackNinth != 0
                        || rule.backtrackTenth != 0
                        || rule.backtrackEleventh != 0
                        || rule.backtrackTwelfth != 0
                        || rule.backtrackThirteenth != 0
                        || rule.backtrackFourteenth != 0
                        || rule.backtrackFifteenth != 0
                        || rule.backtrackSixteenth != 0
                        || rule.backtrackSeventeenth != 0
                        || rule.backtrackEighteenth != 0
                        || rule.backtrackNineteenth != 0
                        || rule.backtrackTwentieth != 0
                        || rule.backtrackTwentyFirst != 0
                        || rule.backtrackTwentySecond != 0
                        || rule.backtrackTwentyThird != 0
                        || rule.backtrackTwentyFourth != 0
                        || rule.backtrackTwentyFifth != 0
                        || rule.backtrackTwentySixth != 0
                        || rule.backtrackTwentySeventh != 0
                        || rule.backtrackTwentyEighth != 0
                        || rule.backtrackTwentyNinth != 0
                        || rule.backtrackThirtieth != 0
                        || rule.backtrackThirtyFirst != 0
                        || rule.backtrackThirtySecond != 0
                        || rule.backtrackThirtyThird != 0
                        || rule.backtrackThirtyFourth != 0
                        || rule.backtrackThirtyFifth != 0
                        || rule.backtrackThirtySixth != 0
                        || rule.backtrackThirtySeventh != 0
                        || rule.backtrackThirtyEighth != 0
                        || rule.backtrackThirtyNinth != 0
                        || rule.backtrackFortieth != 0) {
                    continue;
                }
                if (rule.current == current && rule.next == candidateNext && rule.lookahead == candidateLook) {
                    return rule.substitute;
                }
            }
        }
        return current;
    }

    /// Applies a type-6 rule by walking `glyphIds` with the lookup skip flags.
    ///
    /// @param glyphIds the mapped glyphs
    /// @param start the first glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted first glyph, or the glyph at `start`
    int chainSubstitute(int[] glyphIds, int start, int remaining, int featureTag) {
        Objects.requireNonNull(glyphIds, "glyphIds");
        if (start < 0 || remaining < 1 || start + remaining > glyphIds.length) {
            return start >= 0 && start < glyphIds.length ? glyphIds[start] : 0;
        }
        int current = glyphIds[start];
        int end = start + remaining;
        int next = start + 1 < end ? glyphIds[start + 1] : -1;
        int lookahead = start + 2 < end ? glyphIds[start + 2] : -1;
        for (Feature feature : features) {
            if (feature.tag != featureTag) {
                continue;
            }
            for (ChainRule rule : feature.chains) {
                if (!backsMatch(
                        glyphIds,
                        start,
                        rule.backtrack,
                        rule.backtrackFar,
                        rule.backtrackFarther,
                        rule.backtrackFarthest,
                        rule.backtrackFifth,
                        rule.backtrackSixth,
                        rule.backtrackSeventh,
                        rule.backtrackEighth,
                        rule.backtrackNinth,
                        rule.backtrackTenth,
                        rule.backtrackEleventh,
                        rule.backtrackTwelfth,
                        rule.backtrackThirteenth,
                        rule.backtrackFourteenth,
                        rule.backtrackFifteenth,
                        rule.backtrackSixteenth,
                        rule.backtrackSeventeenth,
                        rule.backtrackEighteenth,
                        rule.backtrackNineteenth,
                        rule.backtrackTwentieth,
                        rule.backtrackTwentyFirst,
                        rule.backtrackTwentySecond,
                        rule.backtrackTwentyThird,
                        rule.backtrackTwentyFourth,
                        rule.backtrackTwentyFifth,
                        rule.backtrackTwentySixth,
                        rule.backtrackTwentySeventh,
                        rule.backtrackTwentyEighth,
                        rule.backtrackTwentyNinth,
                        rule.backtrackThirtieth,
                        rule.backtrackThirtyFirst,
                        rule.backtrackThirtySecond,
                        rule.backtrackThirtyThird,
                        rule.backtrackThirtyFourth,
                        rule.backtrackThirtyFifth,
                        rule.backtrackThirtySixth,
                        rule.backtrackThirtySeventh,
                        rule.backtrackThirtyEighth,
                        rule.backtrackThirtyNinth,
                        rule.backtrackFortieth,
                        rule.lookupFlag,
                        rule.markSet
                )) {
                    continue;
                }
                int nextIndex = gdef.firstKeptIndex(glyphIds, start + 1, end, rule.lookupFlag, rule.markSet);
                if (nextIndex < 0) {
                    continue;
                }
                int lookIndex = gdef.firstKeptIndex(glyphIds, nextIndex + 1, end, rule.lookupFlag, rule.markSet);
                if (lookIndex < 0) {
                    continue;
                }
                if (rule.current == current
                        && rule.next == glyphIds[nextIndex]
                        && rule.lookahead == glyphIds[lookIndex]) {
                    return rule.substitute;
                }
            }
        }
        return current;
    }

    /// Returns whether required backtrack glyphs are present before `start`.
    private boolean backsMatch(
            int[] glyphIds,
            int start,
            int backNear,
            int backMid,
            int backFar,
            int backFarther,
            int backFifth,
            int backSixth,
            int backSeventh,
            int backEighth,
            int backNinth,
            int backTenth,
            int backEleventh,
            int backTwelfth,
            int backThirteenth,
            int backFourteenth,
            int backFifteenth,
            int backSixteenth,
            int backSeventeenth,
            int backEighteenth,
            int backNineteenth,
            int backTwentieth,
            int backTwentyFirst,
            int backTwentySecond,
            int backTwentyThird,
            int backTwentyFourth,
            int backTwentyFifth,
            int backTwentySixth,
            int backTwentySeventh,
            int backTwentyEighth,
            int backTwentyNinth,
            int backThirtieth,
            int backThirtyFirst,
            int backThirtySecond,
            int backThirtyThird,
            int backThirtyFourth,
            int backThirtyFifth,
            int backThirtySixth,
            int backThirtySeventh,
            int backThirtyEighth,
            int backThirtyNinth,
            int backFortieth,
            int lookupFlag,
            int markSet
    ) {
        if (backNear == 0 && backMid == 0 && backFar == 0 && backFarther == 0
                && backFifth == 0 && backSixth == 0 && backSeventh == 0 && backEighth == 0
                && backNinth == 0 && backTenth == 0 && backEleventh == 0 && backTwelfth == 0
                && backThirteenth == 0 && backFourteenth == 0 && backFifteenth == 0
                && backSixteenth == 0 && backSeventeenth == 0 && backEighteenth == 0
                && backNineteenth == 0 && backTwentieth == 0 && backTwentyFirst == 0
                && backTwentySecond == 0 && backTwentyThird == 0 && backTwentyFourth == 0
                && backTwentyFifth == 0 && backTwentySixth == 0 && backTwentySeventh == 0
                && backTwentyEighth == 0 && backTwentyNinth == 0 && backThirtieth == 0
                && backThirtyFirst == 0 && backThirtySecond == 0
                && backThirtyThird == 0 && backThirtyFourth == 0
                && backThirtyFifth == 0 && backThirtySixth == 0
                && backThirtySeventh == 0 && backThirtyEighth == 0
                && backThirtyNinth == 0 && backFortieth == 0) {
            return true;
        }
        if (backNear == 0) {
            return false;
        }
        int nearIndex = gdef.prevKeptIndex(glyphIds, start - 1, lookupFlag, markSet);
        if (nearIndex < 0 || glyphIds[nearIndex] != backNear) {
            return false;
        }
        if (backMid == 0 && backFar == 0 && backFarther == 0 && backFifth == 0 && backSixth == 0
                && backSeventh == 0 && backEighth == 0) {
            return true;
        }
        int midIndex = gdef.prevKeptIndex(glyphIds, nearIndex - 1, lookupFlag, markSet);
        if (backMid != 0 && (midIndex < 0 || glyphIds[midIndex] != backMid)) {
            return false;
        }
        if (backFar == 0 && backFarther == 0 && backFifth == 0 && backSixth == 0
                && backSeventh == 0 && backEighth == 0) {
            return true;
        }
        int cursor = backMid == 0 ? nearIndex : midIndex;
        int farIndex = gdef.prevKeptIndex(glyphIds, cursor - 1, lookupFlag, markSet);
        if (backFar != 0 && (farIndex < 0 || glyphIds[farIndex] != backFar)) {
            return false;
        }
        if (backFarther == 0 && backFifth == 0 && backSixth == 0 && backSeventh == 0 && backEighth == 0) {
            return true;
        }
        int fartherCursor = backFar == 0 ? cursor : farIndex;
        int fartherIndex = gdef.prevKeptIndex(glyphIds, fartherCursor - 1, lookupFlag, markSet);
        if (backFarther != 0 && (fartherIndex < 0 || glyphIds[fartherIndex] != backFarther)) {
            return false;
        }
        if (backFifth == 0 && backSixth == 0 && backSeventh == 0 && backEighth == 0) {
            return true;
        }
        int fifthCursor = backFarther == 0 ? fartherCursor : fartherIndex;
        int fifthIndex = gdef.prevKeptIndex(glyphIds, fifthCursor - 1, lookupFlag, markSet);
        if (backFifth != 0 && (fifthIndex < 0 || glyphIds[fifthIndex] != backFifth)) {
            return false;
        }
        if (backSixth == 0 && backSeventh == 0 && backEighth == 0) {
            return true;
        }
        int sixthCursor = backFifth == 0 ? fifthCursor : fifthIndex;
        int sixthIndex = gdef.prevKeptIndex(glyphIds, sixthCursor - 1, lookupFlag, markSet);
        if (backSixth != 0 && (sixthIndex < 0 || glyphIds[sixthIndex] != backSixth)) {
            return false;
        }
        if (backSeventh == 0 && backEighth == 0 && backNinth == 0 && backTenth == 0) {
            return true;
        }
        int seventhCursor = backSixth == 0 ? sixthCursor : sixthIndex;
        int seventhIndex = gdef.prevKeptIndex(glyphIds, seventhCursor - 1, lookupFlag, markSet);
        if (backSeventh != 0 && (seventhIndex < 0 || glyphIds[seventhIndex] != backSeventh)) {
            return false;
        }
        if (backEighth == 0 && backNinth == 0 && backTenth == 0) {
            return true;
        }
        int eighthCursor = backSeventh == 0 ? seventhCursor : seventhIndex;
        int eighthIndex = gdef.prevKeptIndex(glyphIds, eighthCursor - 1, lookupFlag, markSet);
        if (backEighth != 0 && (eighthIndex < 0 || glyphIds[eighthIndex] != backEighth)) {
            return false;
        }
        if (backNinth == 0 && backTenth == 0) {
            return true;
        }
        int ninthCursor = backEighth == 0 ? eighthCursor : eighthIndex;
        int ninthIndex = gdef.prevKeptIndex(glyphIds, ninthCursor - 1, lookupFlag, markSet);
        if (backNinth != 0 && (ninthIndex < 0 || glyphIds[ninthIndex] != backNinth)) {
            return false;
        }
        if (backTenth == 0 && backEleventh == 0) {
            return true;
        }
        int tenthCursor = backNinth == 0 ? ninthCursor : ninthIndex;
        int tenthIndex = gdef.prevKeptIndex(glyphIds, tenthCursor - 1, lookupFlag, markSet);
        if (backTenth != 0 && (tenthIndex < 0 || glyphIds[tenthIndex] != backTenth)) {
            return false;
        }
        if (backEleventh == 0 && backTwelfth == 0) {
            return true;
        }
        int eleventhCursor = backTenth == 0 ? tenthCursor : tenthIndex;
        int eleventhIndex = gdef.prevKeptIndex(glyphIds, eleventhCursor - 1, lookupFlag, markSet);
        if (backEleventh != 0 && (eleventhIndex < 0 || glyphIds[eleventhIndex] != backEleventh)) {
            return false;
        }
        if (backTwelfth == 0 && backThirteenth == 0) {
            return true;
        }
        int twelfthCursor = backEleventh == 0 ? eleventhCursor : eleventhIndex;
        int twelfthIndex = gdef.prevKeptIndex(glyphIds, twelfthCursor - 1, lookupFlag, markSet);
        if (backTwelfth != 0 && (twelfthIndex < 0 || glyphIds[twelfthIndex] != backTwelfth)) {
            return false;
        }
        if (backThirteenth == 0 && backFourteenth == 0 && backFifteenth == 0) {
            return true;
        }
        int thirteenthCursor = backTwelfth == 0 ? twelfthCursor : twelfthIndex;
        int thirteenthIndex = gdef.prevKeptIndex(glyphIds, thirteenthCursor - 1, lookupFlag, markSet);
        if (backThirteenth != 0 && (thirteenthIndex < 0 || glyphIds[thirteenthIndex] != backThirteenth)) {
            return false;
        }
        if (backFourteenth == 0 && backFifteenth == 0) {
            return true;
        }
        int fourteenthCursor = backThirteenth == 0 ? thirteenthCursor : thirteenthIndex;
        int fourteenthIndex = gdef.prevKeptIndex(glyphIds, fourteenthCursor - 1, lookupFlag, markSet);
        if (backFourteenth != 0 && (fourteenthIndex < 0 || glyphIds[fourteenthIndex] != backFourteenth)) {
            return false;
        }
        if (backFifteenth == 0 && backSixteenth == 0 && backSeventeenth == 0) {
            return true;
        }
        int fifteenthCursor = backFourteenth == 0 ? fourteenthCursor : fourteenthIndex;
        int fifteenthIndex = gdef.prevKeptIndex(glyphIds, fifteenthCursor - 1, lookupFlag, markSet);
        if (backFifteenth != 0 && (fifteenthIndex < 0 || glyphIds[fifteenthIndex] != backFifteenth)) {
            return false;
        }
        if (backSixteenth == 0 && backSeventeenth == 0) {
            return true;
        }
        int sixteenthCursor = backFifteenth == 0 ? fifteenthCursor : fifteenthIndex;
        int sixteenthIndex = gdef.prevKeptIndex(glyphIds, sixteenthCursor - 1, lookupFlag, markSet);
        if (backSixteenth != 0 && (sixteenthIndex < 0 || glyphIds[sixteenthIndex] != backSixteenth)) {
            return false;
        }
        if (backSeventeenth == 0 && backEighteenth == 0) {
            return true;
        }
        int seventeenthCursor = backSixteenth == 0 ? sixteenthCursor : sixteenthIndex;
        int seventeenthIndex = gdef.prevKeptIndex(glyphIds, seventeenthCursor - 1, lookupFlag, markSet);
        if (backSeventeenth != 0 && (seventeenthIndex < 0 || glyphIds[seventeenthIndex] != backSeventeenth)) {
            return false;
        }
        if (backEighteenth == 0 && backNineteenth == 0) {
            return true;
        }
        int eighteenthCursor = backSeventeenth == 0 ? seventeenthCursor : seventeenthIndex;
        int eighteenthIndex = gdef.prevKeptIndex(glyphIds, eighteenthCursor - 1, lookupFlag, markSet);
        if (backEighteenth != 0 && (eighteenthIndex < 0 || glyphIds[eighteenthIndex] != backEighteenth)) {
            return false;
        }
        if (backNineteenth == 0 && backTwentieth == 0) {
            return true;
        }
        int nineteenthCursor = backEighteenth == 0 ? eighteenthCursor : eighteenthIndex;
        int nineteenthIndex = gdef.prevKeptIndex(glyphIds, nineteenthCursor - 1, lookupFlag, markSet);
        if (backNineteenth != 0 && (nineteenthIndex < 0 || glyphIds[nineteenthIndex] != backNineteenth)) {
            return false;
        }
        if (backTwentieth == 0 && backTwentyFirst == 0) {
            return true;
        }
        int twentiethCursor = backNineteenth == 0 ? nineteenthCursor : nineteenthIndex;
        int twentiethIndex = gdef.prevKeptIndex(glyphIds, twentiethCursor - 1, lookupFlag, markSet);
        if (backTwentieth != 0 && (twentiethIndex < 0 || glyphIds[twentiethIndex] != backTwentieth)) {
            return false;
        }
        if (backTwentyFirst == 0 && backTwentySecond == 0) {
            return true;
        }
        int twentyFirstCursor = backTwentieth == 0 ? twentiethCursor : twentiethIndex;
        int twentyFirstIndex = gdef.prevKeptIndex(glyphIds, twentyFirstCursor - 1, lookupFlag, markSet);
        if (backTwentyFirst != 0 && (twentyFirstIndex < 0 || glyphIds[twentyFirstIndex] != backTwentyFirst)) {
            return false;
        }
        if (backTwentySecond == 0 && backTwentyThird == 0) {
            return true;
        }
        int twentySecondCursor = backTwentyFirst == 0 ? twentyFirstCursor : twentyFirstIndex;
        int twentySecondIndex = gdef.prevKeptIndex(glyphIds, twentySecondCursor - 1, lookupFlag, markSet);
        if (backTwentySecond != 0 && (twentySecondIndex < 0 || glyphIds[twentySecondIndex] != backTwentySecond)) {
            return false;
        }
        if (backTwentyThird == 0 && backTwentyFourth == 0) {
            return true;
        }
        int twentyThirdCursor = backTwentySecond == 0 ? twentySecondCursor : twentySecondIndex;
        int twentyThirdIndex = gdef.prevKeptIndex(glyphIds, twentyThirdCursor - 1, lookupFlag, markSet);
        if (backTwentyThird != 0 && (twentyThirdIndex < 0 || glyphIds[twentyThirdIndex] != backTwentyThird)) {
            return false;
        }
        if (backTwentyFourth == 0 && backTwentyFifth == 0 && backTwentySixth == 0) {
            return true;
        }
        int twentyFourthCursor = backTwentyThird == 0 ? twentyThirdCursor : twentyThirdIndex;
        int twentyFourthIndex = gdef.prevKeptIndex(glyphIds, twentyFourthCursor - 1, lookupFlag, markSet);
        if (backTwentyFourth != 0 && (twentyFourthIndex < 0 || glyphIds[twentyFourthIndex] != backTwentyFourth)) {
            return false;
        }
        if (backTwentyFifth == 0 && backTwentySixth == 0) {
            return true;
        }
        int twentyFifthCursor = backTwentyFourth == 0 ? twentyFourthCursor : twentyFourthIndex;
        int twentyFifthIndex = gdef.prevKeptIndex(glyphIds, twentyFifthCursor - 1, lookupFlag, markSet);
        if (backTwentyFifth != 0 && (twentyFifthIndex < 0 || glyphIds[twentyFifthIndex] != backTwentyFifth)) {
            return false;
        }
        if (backTwentySixth == 0 && backTwentySeventh == 0 && backTwentyEighth == 0) {
            return true;
        }
        int twentySixthCursor = backTwentyFifth == 0 ? twentyFifthCursor : twentyFifthIndex;
        int twentySixthIndex = gdef.prevKeptIndex(glyphIds, twentySixthCursor - 1, lookupFlag, markSet);
        if (backTwentySixth != 0 && (twentySixthIndex < 0 || glyphIds[twentySixthIndex] != backTwentySixth)) {
            return false;
        }
        if (backTwentySeventh == 0 && backTwentyEighth == 0) {
            return true;
        }
        int twentySeventhCursor = backTwentySixth == 0 ? twentySixthCursor : twentySixthIndex;
        int twentySeventhIndex = gdef.prevKeptIndex(glyphIds, twentySeventhCursor - 1, lookupFlag, markSet);
        if (backTwentySeventh != 0 && (twentySeventhIndex < 0 || glyphIds[twentySeventhIndex] != backTwentySeventh)) {
            return false;
        }
        if (backTwentyEighth == 0 && backTwentyNinth == 0 && backThirtieth == 0) {
            return true;
        }
        int twentyEighthCursor = backTwentySeventh == 0 ? twentySeventhCursor : twentySeventhIndex;
        int twentyEighthIndex = gdef.prevKeptIndex(glyphIds, twentyEighthCursor - 1, lookupFlag, markSet);
        if (backTwentyEighth != 0 && (twentyEighthIndex < 0 || glyphIds[twentyEighthIndex] != backTwentyEighth)) {
            return false;
        }
        if (backTwentyNinth == 0 && backThirtieth == 0) {
            return true;
        }
        int twentyNinthCursor = backTwentyEighth == 0 ? twentyEighthCursor : twentyEighthIndex;
        int twentyNinthIndex = gdef.prevKeptIndex(glyphIds, twentyNinthCursor - 1, lookupFlag, markSet);
        if (backTwentyNinth != 0 && (twentyNinthIndex < 0 || glyphIds[twentyNinthIndex] != backTwentyNinth)) {
            return false;
        }
        if (backThirtieth == 0 && backThirtyFirst == 0 && backThirtySecond == 0) {
            return true;
        }
        int thirtiethCursor = backTwentyNinth == 0 ? twentyNinthCursor : twentyNinthIndex;
        int thirtiethIndex = gdef.prevKeptIndex(glyphIds, thirtiethCursor - 1, lookupFlag, markSet);
        if (backThirtieth != 0 && (thirtiethIndex < 0 || glyphIds[thirtiethIndex] != backThirtieth)) {
            return false;
        }
        if (backThirtyFirst == 0 && backThirtySecond == 0) {
            return true;
        }
        int thirtyFirstCursor = backThirtieth == 0 ? thirtiethCursor : thirtiethIndex;
        int thirtyFirstIndex = gdef.prevKeptIndex(glyphIds, thirtyFirstCursor - 1, lookupFlag, markSet);
        if (backThirtyFirst != 0 && (thirtyFirstIndex < 0 || glyphIds[thirtyFirstIndex] != backThirtyFirst)) {
            return false;
        }
        if (backThirtySecond == 0 && backThirtyThird == 0 && backThirtyFourth == 0) {
            return true;
        }
        int thirtySecondCursor = backThirtyFirst == 0 ? thirtyFirstCursor : thirtyFirstIndex;
        int thirtySecondIndex = gdef.prevKeptIndex(glyphIds, thirtySecondCursor - 1, lookupFlag, markSet);
        if (backThirtySecond != 0 && (thirtySecondIndex < 0 || glyphIds[thirtySecondIndex] != backThirtySecond)) {
            return false;
        }
        if (backThirtyThird == 0 && backThirtyFourth == 0) {
            return true;
        }
        int thirtyThirdCursor = backThirtySecond == 0 ? thirtySecondCursor : thirtySecondIndex;
        int thirtyThirdIndex = gdef.prevKeptIndex(glyphIds, thirtyThirdCursor - 1, lookupFlag, markSet);
        if (backThirtyThird != 0 && (thirtyThirdIndex < 0 || glyphIds[thirtyThirdIndex] != backThirtyThird)) {
            return false;
        }
        if (backThirtyFourth == 0 && backThirtyFifth == 0 && backThirtySixth == 0) {
            return true;
        }
        int thirtyFourthCursor = backThirtyThird == 0 ? thirtyThirdCursor : thirtyThirdIndex;
        int thirtyFourthIndex = gdef.prevKeptIndex(glyphIds, thirtyFourthCursor - 1, lookupFlag, markSet);
        if (backThirtyFourth != 0 && (thirtyFourthIndex < 0 || glyphIds[thirtyFourthIndex] != backThirtyFourth)) {
            return false;
        }
        if (backThirtyFifth == 0 && backThirtySixth == 0) {
            return true;
        }
        int thirtyFifthCursor = backThirtyFourth == 0 ? thirtyFourthCursor : thirtyFourthIndex;
        int thirtyFifthIndex = gdef.prevKeptIndex(glyphIds, thirtyFifthCursor - 1, lookupFlag, markSet);
        if (backThirtyFifth != 0 && (thirtyFifthIndex < 0 || glyphIds[thirtyFifthIndex] != backThirtyFifth)) {
            return false;
        }
        if (backThirtySixth == 0 && backThirtySeventh == 0 && backThirtyEighth == 0) {
            return true;
        }
        int thirtySixthCursor = backThirtyFifth == 0 ? thirtyFifthCursor : thirtyFifthIndex;
        int thirtySixthIndex = gdef.prevKeptIndex(glyphIds, thirtySixthCursor - 1, lookupFlag, markSet);
        if (backThirtySixth != 0 && (thirtySixthIndex < 0 || glyphIds[thirtySixthIndex] != backThirtySixth)) {
            return false;
        }
        if (backThirtySeventh == 0 && backThirtyEighth == 0) {
            return true;
        }
        int thirtySeventhCursor = backThirtySixth == 0 ? thirtySixthCursor : thirtySixthIndex;
        int thirtySeventhIndex = gdef.prevKeptIndex(glyphIds, thirtySeventhCursor - 1, lookupFlag, markSet);
        if (backThirtySeventh != 0 && (thirtySeventhIndex < 0 || glyphIds[thirtySeventhIndex] != backThirtySeventh)) {
            return false;
        }
        if (backThirtyEighth == 0 && backThirtyNinth == 0 && backFortieth == 0) {
            return true;
        }
        int thirtyEighthCursor = backThirtySeventh == 0 ? thirtySeventhCursor : thirtySeventhIndex;
        int thirtyEighthIndex = gdef.prevKeptIndex(glyphIds, thirtyEighthCursor - 1, lookupFlag, markSet);
        if (backThirtyEighth != 0 && (thirtyEighthIndex < 0 || glyphIds[thirtyEighthIndex] != backThirtyEighth)) {
            return false;
        }
        if (backThirtyNinth == 0 && backFortieth == 0) {
            return true;
        }
        int thirtyNinthCursor = backThirtyEighth == 0 ? thirtyEighthCursor : thirtyEighthIndex;
        int thirtyNinthIndex = gdef.prevKeptIndex(glyphIds, thirtyNinthCursor - 1, lookupFlag, markSet);
        if (backThirtyNinth != 0 && (thirtyNinthIndex < 0 || glyphIds[thirtyNinthIndex] != backThirtyNinth)) {
            return false;
        }
        if (backFortieth == 0) {
            return true;
        }
        int fortiethCursor = backThirtyNinth == 0 ? thirtyNinthCursor : thirtyNinthIndex;
        int fortiethIndex = gdef.prevKeptIndex(glyphIds, fortiethCursor - 1, lookupFlag, markSet);
        return fortiethIndex >= 0 && glyphIds[fortiethIndex] == backFortieth;
    }

    /// Reads up to `max` backtrack glyph or class ids. Index 0 is nearest.
    private static int @Nullable [] readBacktrackIds(ByteBuffer buffer, int max) {
        if (buffer.remaining() < 2) {
            return null;
        }
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count > max) {
            return null;
        }
        int[] ids = new int[40];
        for (int index = 0; index < count; index++) {
            if (buffer.remaining() < 2) {
                return null;
            }
            ids[index] = Short.toUnsignedInt(buffer.getShort());
        }
        return ids;
    }

    /// Copies coverage glyphs, dropping negative slots.
    private static int[] coverageGlyphs(Coverage coverage) {
        int[] glyphs = new int[Math.max(0, coverage.size())];
        int written = 0;
        for (int index = 0; index < coverage.size(); index++) {
            int glyph = coverage.glyphAt(index);
            if (glyph >= 0) {
                glyphs[written++] = glyph;
            }
        }
        return written == 0 ? new int[] {0} : written == glyphs.length ? glyphs : Arrays.copyOf(glyphs, written);
    }

    /// Selects the input glyph for a lookup flag.
    private static int ruleCandidate(
            boolean ignoreMarks,
            int attachType,
            int next,
            int skippedNext,
            int attachSkipped
    ) {
        if (ignoreMarks) {
            return skippedNext;
        }
        if (attachType != 0) {
            return attachSkipped;
        }
        return next;
    }

    /// Returns the first glyph in `[start, end)` that the skip flags keep, or `-1`.
    private int firstKept(int[] glyphIds, int start, int end, boolean ignoreMarks, int attachType) {
        int index = firstKeptIndex(glyphIds, start, end, ignoreMarks, attachType);
        return index >= 0 ? glyphIds[index] : -1;
    }

    /// Returns the index of the first kept glyph in `[start, end)`, or `-1`.
    private int firstKeptIndex(int[] glyphIds, int start, int end, boolean ignoreMarks, int attachType) {
        for (int index = start; index < end; index++) {
            if (!skipped(glyphIds[index], ignoreMarks, attachType)) {
                return index;
            }
        }
        return -1;
    }

    /// Returns whether a lookup with these flags skips `glyphId`.
    private boolean skipped(int glyphId, boolean ignoreMarks, int attachType) {
        int flag = ignoreMarks ? GdefTable.FLAG_IGNORE_MARKS : attachType << 8;
        return gdef.skip(glyphId, flag, 0);
    }

    /// Returns whether a lookup with `lookupFlag` skips `glyphId`.
    private boolean skipped(int glyphId, int lookupFlag, int markSet) {
        return gdef.skip(glyphId, lookupFlag, markSet);
    }

    /// Applies the first type-8 reverse-chain substitution listed by `featureTag`.
    ///
    /// @param current the input glyph
    /// @param lookahead the glyph after `current`
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted glyph, or `current`
    int reverseSubstitute(int current, int lookahead, int featureTag) {
        for (Feature feature : features) {
            if (feature.tag != featureTag) {
                continue;
            }
            for (ReverseRule rule : feature.reverses) {
                if (rule.backtrack != 0
                        || rule.backtrackFar != 0
                        || rule.backtrackFarther != 0
                        || rule.backtrackFarthest != 0
                        || rule.backtrackFifth != 0
                        || rule.backtrackSixth != 0
                        || rule.backtrackSeventh != 0
                        || rule.backtrackEighth != 0
                        || rule.backtrackNinth != 0
                        || rule.backtrackTenth != 0
                        || rule.backtrackEleventh != 0
                        || rule.backtrackTwelfth != 0
                        || rule.backtrackThirteenth != 0
                        || rule.backtrackFourteenth != 0
                        || rule.backtrackFifteenth != 0
                        || rule.backtrackSixteenth != 0
                        || rule.backtrackSeventeenth != 0
                        || rule.backtrackEighteenth != 0
                        || rule.backtrackNineteenth != 0
                        || rule.backtrackTwentieth != 0
                        || rule.backtrackTwentyFirst != 0
                        || rule.backtrackTwentySecond != 0
                        || rule.backtrackTwentyThird != 0
                        || rule.backtrackTwentyFourth != 0
                        || rule.backtrackTwentyFifth != 0
                        || rule.backtrackTwentySixth != 0
                        || rule.backtrackTwentySeventh != 0
                        || rule.backtrackTwentyEighth != 0
                        || rule.backtrackTwentyNinth != 0
                        || rule.backtrackThirtieth != 0
                        || rule.backtrackThirtyFirst != 0
                        || rule.backtrackThirtySecond != 0
                        || rule.backtrackThirtyThird != 0
                        || rule.backtrackThirtyFourth != 0
                        || rule.backtrackThirtyFifth != 0
                        || rule.backtrackThirtySixth != 0
                        || rule.backtrackThirtySeventh != 0
                        || rule.backtrackThirtyEighth != 0
                        || rule.backtrackThirtyNinth != 0
                        || rule.backtrackFortieth != 0) {
                    continue;
                }
                if (rule.current == current && rule.lookahead == lookahead) {
                    return rule.substitute;
                }
            }
        }
        return current;
    }

    /// Applies a type-8 reverse rule by walking lookahead glyphs with the lookup skip flags.
    ///
    /// @param glyphIds the mapped glyphs
    /// @param start the input glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @param featureTag a four-byte OpenType tag
    /// @return the substituted glyph, or the glyph at `start`
    int reverseSubstitute(int[] glyphIds, int start, int remaining, int featureTag) {
        Objects.requireNonNull(glyphIds, "glyphIds");
        if (start < 0 || remaining < 1 || start + remaining > glyphIds.length) {
            return start >= 0 && start < glyphIds.length ? glyphIds[start] : 0;
        }
        int current = glyphIds[start];
        int end = start + remaining;
        for (Feature feature : features) {
            if (feature.tag != featureTag) {
                continue;
            }
            for (ReverseRule rule : feature.reverses) {
                if (!backsMatch(
                        glyphIds,
                        start,
                        rule.backtrack,
                        rule.backtrackFar,
                        rule.backtrackFarther,
                        rule.backtrackFarthest,
                        rule.backtrackFifth,
                        rule.backtrackSixth,
                        rule.backtrackSeventh,
                        rule.backtrackEighth,
                        rule.backtrackNinth,
                        rule.backtrackTenth,
                        rule.backtrackEleventh,
                        rule.backtrackTwelfth,
                        rule.backtrackThirteenth,
                        rule.backtrackFourteenth,
                        rule.backtrackFifteenth,
                        rule.backtrackSixteenth,
                        rule.backtrackSeventeenth,
                        rule.backtrackEighteenth,
                        rule.backtrackNineteenth,
                        rule.backtrackTwentieth,
                        rule.backtrackTwentyFirst,
                        rule.backtrackTwentySecond,
                        rule.backtrackTwentyThird,
                        rule.backtrackTwentyFourth,
                        rule.backtrackTwentyFifth,
                        rule.backtrackTwentySixth,
                        rule.backtrackTwentySeventh,
                        rule.backtrackTwentyEighth,
                        rule.backtrackTwentyNinth,
                        rule.backtrackThirtieth,
                        rule.backtrackThirtyFirst,
                        rule.backtrackThirtySecond,
                        rule.backtrackThirtyThird,
                        rule.backtrackThirtyFourth,
                        rule.backtrackThirtyFifth,
                        rule.backtrackThirtySixth,
                        rule.backtrackThirtySeventh,
                        rule.backtrackThirtyEighth,
                        rule.backtrackThirtyNinth,
                        rule.backtrackFortieth,
                        rule.lookupFlag,
                        rule.markSet
                )) {
                    continue;
                }
                int lookIndex = gdef.firstKeptIndex(glyphIds, start + 1, end, rule.lookupFlag, rule.markSet);
                if (lookIndex >= 0 && rule.current == current && rule.lookahead == glyphIds[lookIndex]) {
                    return rule.substitute;
                }
            }
        }
        return current;
    }

    /// Parses a GSUB table, or returns [`#NONE`] when the header is absent.
    ///
    /// @param table the GSUB bytes, or `null`
    /// @return the substitutions
    static GsubSubstitutions parse(@Nullable ByteBuffer table) {
        return parse(table, GdefTable.NONE);
    }

    /// Parses a GSUB table against `gdef`.
    ///
    /// @param table the GSUB bytes, or `null`
    /// @param gdef the GDEF classes
    /// @return the substitutions
    static GsubSubstitutions parse(@Nullable ByteBuffer table, GdefTable gdef) {
        if (table == null || table.remaining() < 10) {
            return NONE;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        int start = buffer.position();
        int major = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        if (major != 1) {
            return NONE;
        }
        buffer.getShort();
        int featureList = start + Short.toUnsignedInt(buffer.getShort());
        int lookupList = start + Short.toUnsignedInt(buffer.getShort());
        LookupTable lookups = readLookups(buffer, lookupList);
        return new GsubSubstitutions(readFeatures(buffer, featureList, lookups), gdef);
    }

    /// Reads the feature list.
    private static Feature[] readFeatures(ByteBuffer buffer, int featureList, LookupTable lookups) {
        if (featureList + 2 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB feature list is truncated");
        }
        buffer.position(featureList);
        int count = Short.toUnsignedInt(buffer.getShort());
        Feature[] features = new Feature[count];
        for (int index = 0; index < count; index++) {
            if (buffer.remaining() < 6) {
                throw new IllegalArgumentException("GSUB feature record is truncated");
            }
            int tag = buffer.getInt();
            int offset = featureList + Short.toUnsignedInt(buffer.getShort());
            features[index] = readFeature(buffer, tag, offset, lookups);
        }
        return features;
    }

    /// Resolves lookup indices for one feature.
    private static Feature readFeature(ByteBuffer buffer, int tag, int offset, LookupTable lookups) {
        if (offset + 4 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB feature table is truncated");
        }
        int saved = buffer.position();
        buffer.position(offset);
        buffer.getShort();
        int count = Short.toUnsignedInt(buffer.getShort());
        SingleSubst[] singles = new SingleSubst[count];
        LigatureSubst[] ligatures = new LigatureSubst[count];
        MultipleSubst[] multiples = new MultipleSubst[count];
        AlternateSubst[] alternates = new AlternateSubst[count];
        ContextRule[] contexts = new ContextRule[count];
        ChainRule[] chains = new ChainRule[count];
        ReverseRule[] reverses = new ReverseRule[count];
        int singleCount = 0;
        int ligatureCount = 0;
        int multipleCount = 0;
        int alternateCount = 0;
        int contextCount = 0;
        int chainCount = 0;
        int reverseCount = 0;
        for (int index = 0; index < count; index++) {
            int lookupIndex = Short.toUnsignedInt(buffer.getShort());
            if (lookupIndex >= lookups.singles.length) {
                continue;
            }
            if (lookups.singles[lookupIndex] != null) {
                singles[singleCount++] = lookups.singles[lookupIndex];
            }
            if (lookups.ligatures[lookupIndex] != null) {
                ligatures[ligatureCount++] = lookups.ligatures[lookupIndex];
            }
            if (lookups.multiples[lookupIndex] != null) {
                multiples[multipleCount++] = lookups.multiples[lookupIndex];
            }
            if (lookups.alternates[lookupIndex] != null) {
                alternates[alternateCount++] = lookups.alternates[lookupIndex];
            }
            if (lookups.contexts[lookupIndex] != null) {
                for (ContextRule rule : lookups.contexts[lookupIndex]) {
                    if (contextCount == contexts.length) {
                        contexts = Arrays.copyOf(contexts, contexts.length * 2);
                    }
                    contexts[contextCount++] = rule;
                }
            }
            if (lookups.chains[lookupIndex] != null) {
                for (ChainRule rule : lookups.chains[lookupIndex]) {
                    if (chainCount == chains.length) {
                        chains = Arrays.copyOf(chains, chains.length * 2);
                    }
                    chains[chainCount++] = rule;
                }
            }
            if (lookups.reverses[lookupIndex] != null) {
                for (ReverseRule rule : lookups.reverses[lookupIndex]) {
                    if (reverseCount == reverses.length) {
                        reverses = Arrays.copyOf(reverses, reverses.length * 2);
                    }
                    reverses[reverseCount++] = rule;
                }
            }
        }
        buffer.position(saved);
        return new Feature(
                tag,
                singleCount == singles.length ? singles : Arrays.copyOf(singles, singleCount),
                ligatureCount == ligatures.length ? ligatures : Arrays.copyOf(ligatures, ligatureCount),
                multipleCount == multiples.length ? multiples : Arrays.copyOf(multiples, multipleCount),
                alternateCount == alternates.length ? alternates : Arrays.copyOf(alternates, alternateCount),
                contextCount == contexts.length ? contexts : Arrays.copyOf(contexts, contextCount),
                chainCount == chains.length ? chains : Arrays.copyOf(chains, chainCount),
                reverseCount == reverses.length ? reverses : Arrays.copyOf(reverses, reverseCount)
        );
    }

    /// Reads the lookup list, keeping type-1 through type-4 subtables after type-7 unwrap.
    private static LookupTable readLookups(ByteBuffer buffer, int lookupList) {
        if (lookupList + 2 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB lookup list is truncated");
        }
        buffer.position(lookupList);
        int count = Short.toUnsignedInt(buffer.getShort());
        SingleSubst[] singles = new SingleSubst[count];
        LigatureSubst[] ligatures = new LigatureSubst[count];
        MultipleSubst[] multiples = new MultipleSubst[count];
        AlternateSubst[] alternates = new AlternateSubst[count];
        ContextRule[][] contexts = new ContextRule[count][];
        ChainRule[][] chains = new ChainRule[count][];
        ReverseRule[][] reverses = new ReverseRule[count][];
        int[] offsets = new int[count];
        for (int index = 0; index < count; index++) {
            offsets[index] = lookupList + Short.toUnsignedInt(buffer.getShort());
        }
        for (int index = 0; index < count; index++) {
            readLookup(buffer, offsets[index], singles, ligatures, multiples, alternates, reverses, index);
        }
        for (int index = 0; index < count; index++) {
            readContextLookup(buffer, offsets[index], offsets, singles, contexts, chains, index);
        }
        return new LookupTable(singles, ligatures, multiples, alternates, contexts, chains, reverses);
    }

    /// Reads one lookup into the matching slot arrays.
    private static void readLookup(
            ByteBuffer buffer,
            int offset,
            SingleSubst[] singles,
            LigatureSubst[] ligatures,
            MultipleSubst[] multiples,
            AlternateSubst[] alternates,
            ReverseRule[][] reverses,
            int index
    ) {
        if (offset + 6 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB lookup is truncated");
        }
        buffer.position(offset);
        int type = Short.toUnsignedInt(buffer.getShort());
        int flag = Short.toUnsignedInt(buffer.getShort());
        int subtableCount = Short.toUnsignedInt(buffer.getShort());
        if (subtableCount == 0) {
            return;
        }
        int first = offset + Short.toUnsignedInt(buffer.getShort());
        for (int extra = 1; extra < subtableCount && buffer.remaining() >= 2; extra++) {
            buffer.getShort();
        }
        int markSet = 0;
        if ((flag & GdefTable.FLAG_MARK_FILTER) != 0 && buffer.remaining() >= 2) {
            markSet = Short.toUnsignedInt(buffer.getShort());
        }
        int subtable = first;
        if (type == 7) {
            int unwrappedType = unwrapExtensionType(buffer, first);
            int unwrappedOffset = unwrapExtensionOffset(buffer, first);
            if (unwrappedType < 0 || unwrappedOffset < 0) {
                return;
            }
            type = unwrappedType;
            subtable = unwrappedOffset;
        }
        if (type == 1) {
            singles[index] = readSingleSubst(buffer, subtable);
        } else if (type == 2) {
            multiples[index] = readMultipleSubst(buffer, subtable);
        } else if (type == 3) {
            alternates[index] = readAlternateSubst(buffer, subtable);
        } else if (type == 4) {
            ligatures[index] = readLigatureSubst(buffer, subtable, flag, markSet);
        } else if (type == 8) {
            reverses[index] = readReverseSubst(buffer, subtable, flag, markSet);
        }
    }

    /// Reads a type-7 Format-1 extension lookup type, or `-1`.
    private static int unwrapExtensionType(ByteBuffer buffer, int offset) {
        if (offset + 8 > buffer.limit()) {
            return -1;
        }
        int saved = buffer.position();
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        int type = Short.toUnsignedInt(buffer.getShort());
        buffer.position(saved);
        return format == 1 ? type : -1;
    }

    /// Reads a type-7 Format-1 extension subtable offset, or `-1`.
    private static int unwrapExtensionOffset(ByteBuffer buffer, int offset) {
        if (offset + 8 > buffer.limit()) {
            return -1;
        }
        int saved = buffer.position();
        buffer.position(offset + 4);
        long relative = Integer.toUnsignedLong(buffer.getInt());
        buffer.position(saved);
        if (relative > Integer.MAX_VALUE - offset) {
            return -1;
        }
        return offset + (int) relative;
    }

    /// Reads a type-2 multiple substitution subtable.
    private static MultipleSubst readMultipleSubst(ByteBuffer buffer, int offset) {
        return new MultipleSubst(readSequenceTable(buffer, offset, 1));
    }

    /// Reads a type-3 alternate substitution subtable.
    private static AlternateSubst readAlternateSubst(ByteBuffer buffer, int offset) {
        return new AlternateSubst(readSequenceTable(buffer, offset, 1));
    }

    /// Reads a coverage-indexed list of glyph sequences used by type 2 and type 3.
    ///
    /// @param minGlyphs the inclusive minimum sequence length
    private static SequenceTable readSequenceTable(ByteBuffer buffer, int offset, int minGlyphs) {
        if (offset + 6 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB sequence subst is truncated");
        }
        int saved = buffer.position();
        buffer.position(offset);
        if (Short.toUnsignedInt(buffer.getShort()) != 1) {
            buffer.position(saved);
            throw new IllegalArgumentException("Unsupported GSUB sequence subst format");
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int count = Short.toUnsignedInt(buffer.getShort());
        int[] setOffsets = new int[count];
        for (int index = 0; index < count; index++) {
            if (buffer.remaining() < 2) {
                throw new IllegalArgumentException("GSUB sequence offset is truncated");
            }
            setOffsets[index] = offset + Short.toUnsignedInt(buffer.getShort());
        }
        Coverage coverage = readCoverage(buffer, coverageOffset);
        int[][] sequences = new int[count][];
        for (int index = 0; index < count; index++) {
            sequences[index] = readGlyphSequence(buffer, setOffsets[index], minGlyphs);
        }
        buffer.position(saved);
        return new SequenceTable(coverage, sequences);
    }

    /// Reads one glyph sequence.
    private static int[] readGlyphSequence(ByteBuffer buffer, int offset, int minGlyphs) {
        if (offset + 2 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB glyph sequence is truncated");
        }
        int saved = buffer.position();
        buffer.position(offset);
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count < minGlyphs) {
            throw new IllegalArgumentException("GSUB glyph sequence is empty");
        }
        int[] glyphs = new int[count];
        for (int index = 0; index < count; index++) {
            if (buffer.remaining() < 2) {
                throw new IllegalArgumentException("GSUB glyph sequence is truncated");
            }
            glyphs[index] = Short.toUnsignedInt(buffer.getShort());
        }
        buffer.position(saved);
        return glyphs;
    }

    /// Reads type-5 and type-6 lookups after type-1 slots exist.
    private static void readContextLookup(
            ByteBuffer buffer,
            int offset,
            int[] lookupOffsets,
            SingleSubst[] singles,
            ContextRule[][] contexts,
            ChainRule[][] chains,
            int index
    ) {
        if (offset + 6 > buffer.limit()) {
            return;
        }
        buffer.position(offset);
        int type = Short.toUnsignedInt(buffer.getShort());
        int flag = Short.toUnsignedInt(buffer.getShort());
        int subtableCount = Short.toUnsignedInt(buffer.getShort());
        if (subtableCount == 0 || (type != 5 && type != 6)) {
            return;
        }
        int first = offset + Short.toUnsignedInt(buffer.getShort());
        for (int extra = 1; extra < subtableCount && buffer.remaining() >= 2; extra++) {
            buffer.getShort();
        }
        int markSet = 0;
        if ((flag & GdefTable.FLAG_MARK_FILTER) != 0 && buffer.remaining() >= 2) {
            markSet = Short.toUnsignedInt(buffer.getShort());
        }
        boolean ignoreMarks = (flag & GdefTable.FLAG_IGNORE_MARKS) != 0;
        int attachType = (flag >>> 8) & 0xFF;
        if (type == 5) {
            contexts[index] = readContextSubst(buffer, first, singles, ignoreMarks, attachType, flag, markSet);
        } else {
            chains[index] = readChainSubst(buffer, first, singles, ignoreMarks, attachType, flag, markSet);
        }
    }

    /// Reads a type-1 single substitution subtable.
    private static SingleSubst readSingleSubst(ByteBuffer buffer, int offset) {
        if (offset + 6 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB single subst is truncated");
        }
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        Coverage coverage = readCoverage(buffer, coverageOffset);
        if (format == 1) {
            int delta = buffer.getShort();
            return new SingleSubst(coverage, delta, null);
        }
        if (format != 2) {
            throw new IllegalArgumentException("Unsupported GSUB single subst format " + format);
        }
        int count = Short.toUnsignedInt(buffer.getShort());
        int[] substitutes = new int[count];
        for (int index = 0; index < count; index++) {
            substitutes[index] = Short.toUnsignedInt(buffer.getShort());
        }
        return new SingleSubst(coverage, 0, substitutes);
    }

    /// Reads a type-5 ContextSubst format-1 two-glyph rule set.
    private static ContextRule[] readContextSubst(
            ByteBuffer buffer,
            int offset,
            SingleSubst[] singles,
            boolean ignoreMarks,
            int attachType,
            int lookupFlag,
            int markSet
    ) {
        if (offset + 6 > buffer.limit()) {
            return new ContextRule[0];
        }
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        if (format == 3) {
            return readContextSubstFormat3(buffer, offset, singles, ignoreMarks, attachType, lookupFlag, markSet);
        }
        if (format == 2) {
            return readContextSubstFormat2(buffer, offset, singles, ignoreMarks, attachType, lookupFlag, markSet);
        }
        if (format != 1) {
            return new ContextRule[0];
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int setCount = Short.toUnsignedInt(buffer.getShort());
        Coverage coverage = readCoverage(buffer, coverageOffset);
        int[] setOffsets = new int[setCount];
        for (int index = 0; index < setCount; index++) {
            if (buffer.remaining() < 2) {
                break;
            }
            setOffsets[index] = offset + Short.toUnsignedInt(buffer.getShort());
        }
        ContextRule[] rules = new ContextRule[setCount];
        int written = 0;
        for (int index = 0; index < setCount; index++) {
            @Nullable ContextRule rule = readContextRule(
                    buffer,
                    setOffsets[index],
                    coverage.glyphAt(index),
                    singles,
                    ignoreMarks,
                    attachType,
                    lookupFlag,
                    markSet
            );
            if (rule != null) {
                rules[written++] = rule;
            }
        }
        return written == rules.length ? rules : Arrays.copyOf(rules, written);
    }

    /// Reads one two-glyph SubRule.
    private static @Nullable ContextRule readContextRule(
            ByteBuffer buffer,
            int offset,
            int first,
            SingleSubst[] singles,
            boolean ignoreMarks,
            int attachType,
            int lookupFlag,
            int markSet
    ) {
        if (offset + 6 > buffer.limit() || first < 0) {
            return null;
        }
        int saved = buffer.position();
        buffer.position(offset);
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count < 1) {
            buffer.position(saved);
            return null;
        }
        int ruleOffset = offset + Short.toUnsignedInt(buffer.getShort());
        buffer.position(ruleOffset);
        if (buffer.remaining() < 8) {
            buffer.position(saved);
            return null;
        }
        int glyphCount = Short.toUnsignedInt(buffer.getShort());
        int substCount = Short.toUnsignedInt(buffer.getShort());
        if (glyphCount != 2 || substCount < 1 || buffer.remaining() < 6) {
            buffer.position(saved);
            return null;
        }
        int next = Short.toUnsignedInt(buffer.getShort());
        int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
        int lookupIndex = Short.toUnsignedInt(buffer.getShort());
        buffer.position(saved);
        if (sequenceIndex != 0 || lookupIndex >= singles.length || singles[lookupIndex] == null) {
            return null;
        }
        return new ContextRule(
                first,
                next,
                singles[lookupIndex].apply(first),
                ignoreMarks,
                attachType,
                lookupFlag,
                markSet
        );
    }

    /// Reads ContextSubst format 3: two coverage tables and a nested type-1 substitute.
    private static ContextRule[] readContextSubstFormat3(
            ByteBuffer buffer,
            int offset,
            SingleSubst[] singles,
            boolean ignoreMarks,
            int attachType,
            int lookupFlag,
            int markSet
    ) {
        if (buffer.remaining() < 4) {
            return new ContextRule[0];
        }
        int glyphCount = Short.toUnsignedInt(buffer.getShort());
        int substCount = Short.toUnsignedInt(buffer.getShort());
        if (glyphCount != 2 || substCount < 1 || buffer.remaining() < 4 + substCount * 4) {
            return new ContextRule[0];
        }
        int firstCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        int secondCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
        int lookupIndex = Short.toUnsignedInt(buffer.getShort());
        if (sequenceIndex != 0 || lookupIndex >= singles.length || singles[lookupIndex] == null) {
            return new ContextRule[0];
        }
        Coverage firsts = readCoverage(buffer, firstCoverage);
        Coverage seconds = readCoverage(buffer, secondCoverage);
        ContextRule[] rules = new ContextRule[Math.max(0, firsts.size() * seconds.size())];
        int written = 0;
        for (int firstIndex = 0; firstIndex < firsts.size(); firstIndex++) {
            int first = firsts.glyphAt(firstIndex);
            int substitute = singles[lookupIndex].apply(first);
            for (int secondIndex = 0; secondIndex < seconds.size(); secondIndex++) {
                int second = seconds.glyphAt(secondIndex);
                if (first < 0 || second < 0) {
                    continue;
                }
                rules[written++] = new ContextRule(
                        first,
                        second,
                        substitute,
                        ignoreMarks,
                        attachType,
                        lookupFlag,
                        markSet
                );
            }
        }
        return written == rules.length ? rules : Arrays.copyOf(rules, written);
    }

    /// Reads ContextSubst format 2: a ClassDef plus two-class ClassRules.
    private static ContextRule[] readContextSubstFormat2(
            ByteBuffer buffer,
            int offset,
            SingleSubst[] singles,
            boolean ignoreMarks,
            int attachType,
            int lookupFlag,
            int markSet
    ) {
        if (buffer.remaining() < 6) {
            return new ContextRule[0];
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int classDefOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int setCount = Short.toUnsignedInt(buffer.getShort());
        int[] setOffsets = new int[setCount];
        for (int index = 0; index < setCount; index++) {
            if (buffer.remaining() < 2) {
                return new ContextRule[0];
            }
            int relative = Short.toUnsignedInt(buffer.getShort());
            setOffsets[index] = relative == 0 ? 0 : offset + relative;
        }
        Coverage coverage = readCoverage(buffer, coverageOffset);
        ClassMap classes = ClassMap.read(buffer, classDefOffset);
        ContextRule[] rules = new ContextRule[Math.max(4, coverage.size())];
        int written = 0;
        for (int index = 0; index < coverage.size(); index++) {
            int first = coverage.glyphAt(index);
            if (first < 0) {
                continue;
            }
            int firstClass = classes.classOf(first);
            if (firstClass < 0 || firstClass >= setCount || setOffsets[firstClass] == 0) {
                continue;
            }
            if (setOffsets[firstClass] + 2 > buffer.limit()) {
                continue;
            }
            int saved = buffer.position();
            buffer.position(setOffsets[firstClass]);
            int ruleCount = Short.toUnsignedInt(buffer.getShort());
            int[] ruleOffsets = new int[ruleCount];
            for (int ruleIndex = 0; ruleIndex < ruleCount && buffer.remaining() >= 2; ruleIndex++) {
                ruleOffsets[ruleIndex] = setOffsets[firstClass] + Short.toUnsignedInt(buffer.getShort());
            }
            buffer.position(saved);
            for (int ruleOffset : ruleOffsets) {
                if (ruleOffset + 8 > buffer.limit()) {
                    continue;
                }
                saved = buffer.position();
                buffer.position(ruleOffset);
                int glyphCount = Short.toUnsignedInt(buffer.getShort());
                int substCount = Short.toUnsignedInt(buffer.getShort());
                if (glyphCount != 2 || substCount < 1 || buffer.remaining() < 6) {
                    buffer.position(saved);
                    continue;
                }
                int secondClass = Short.toUnsignedInt(buffer.getShort());
                int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
                int lookupIndex = Short.toUnsignedInt(buffer.getShort());
                buffer.position(saved);
                if (sequenceIndex != 0 || lookupIndex >= singles.length || singles[lookupIndex] == null) {
                    continue;
                }
                int substitute = singles[lookupIndex].apply(first);
                for (int second : classes.glyphsOf(secondClass)) {
                    if (written == rules.length) {
                        rules = Arrays.copyOf(rules, rules.length * 2);
                    }
                    rules[written++] = new ContextRule(
                            first,
                            second,
                            substitute,
                            ignoreMarks,
                            attachType,
                            lookupFlag,
                            markSet
                    );
                }
            }
        }
        return written == rules.length ? rules : Arrays.copyOf(rules, written);
    }

    /// Reads a type-6 ChainContextSubst format-1 one-lookahead rule set.
    private static ChainRule[] readChainSubst(
            ByteBuffer buffer,
            int offset,
            SingleSubst[] singles,
            boolean ignoreMarks,
            int attachType,
            int lookupFlag,
            int markSet
    ) {
        if (offset + 6 > buffer.limit()) {
            return new ChainRule[0];
        }
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        if (format == 3) {
            return readChainSubstFormat3(buffer, offset, singles, ignoreMarks, attachType, lookupFlag, markSet);
        }
        if (format == 2) {
            return readChainSubstFormat2(buffer, offset, singles, ignoreMarks, attachType, lookupFlag, markSet);
        }
        if (format != 1) {
            return new ChainRule[0];
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int setCount = Short.toUnsignedInt(buffer.getShort());
        Coverage coverage = readCoverage(buffer, coverageOffset);
        int[] setOffsets = new int[setCount];
        for (int index = 0; index < setCount; index++) {
            if (buffer.remaining() < 2) {
                break;
            }
            setOffsets[index] = offset + Short.toUnsignedInt(buffer.getShort());
        }
        ChainRule[] rules = new ChainRule[setCount];
        int written = 0;
        for (int index = 0; index < setCount; index++) {
            @Nullable ChainRule rule = readChainRule(
                    buffer,
                    setOffsets[index],
                    coverage.glyphAt(index),
                    singles,
                    ignoreMarks,
                    attachType,
                    lookupFlag,
                    markSet
            );
            if (rule != null) {
                rules[written++] = rule;
            }
        }
        return written == rules.length ? rules : Arrays.copyOf(rules, written);
    }

    /// Reads one first-stable chain SubRule.
    private static @Nullable ChainRule readChainRule(
            ByteBuffer buffer,
            int offset,
            int first,
            SingleSubst[] singles,
            boolean ignoreMarks,
            int attachType,
            int lookupFlag,
            int markSet
    ) {
        if (offset + 6 > buffer.limit() || first < 0) {
            return null;
        }
        int saved = buffer.position();
        buffer.position(offset);
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count < 1) {
            buffer.position(saved);
            return null;
        }
        int ruleOffset = offset + Short.toUnsignedInt(buffer.getShort());
        buffer.position(ruleOffset);
        if (buffer.remaining() < 12) {
            buffer.position(saved);
            return null;
        }
        int @Nullable [] backs = readBacktrackIds(buffer, 40);
        if (backs == null) {
            buffer.position(saved);
            return null;
        }
        int inputCount = Short.toUnsignedInt(buffer.getShort());
        if (inputCount != 2 || buffer.remaining() < 2) {
            buffer.position(saved);
            return null;
        }
        int next = Short.toUnsignedInt(buffer.getShort());
        int lookaheadCount = Short.toUnsignedInt(buffer.getShort());
        if (lookaheadCount != 1 || buffer.remaining() < 2) {
            buffer.position(saved);
            return null;
        }
        int lookahead = Short.toUnsignedInt(buffer.getShort());
        int substCount = Short.toUnsignedInt(buffer.getShort());
        if (substCount < 1 || buffer.remaining() < 4) {
            buffer.position(saved);
            return null;
        }
        int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
        int lookupIndex = Short.toUnsignedInt(buffer.getShort());
        buffer.position(saved);
        if (sequenceIndex != 0 || lookupIndex >= singles.length || singles[lookupIndex] == null) {
            return null;
        }
        return new ChainRule(
                first,
                next,
                lookahead,
                singles[lookupIndex].apply(first),
                ignoreMarks,
                attachType,
                lookupFlag,
                markSet,
                backs[0],
                backs[1],
                backs[2],
                backs[3],
                backs[4],
                backs[5],
                backs[6],
                backs[7],
                backs[8],
                backs[9],
                backs[10],
                backs[11],
                backs[12],
                backs[13],
                backs[14],
                backs[15],
                backs[16],
                backs[17],
                backs[18],
                backs[19],
                backs[20],
                backs[21],
                backs[22],
                backs[23],
                backs[24],
                backs[25],
                backs[26],
                backs[27],
                backs[28],
                backs[29],
                backs[30],
                backs[31],
                backs[32],
                backs[33],
                backs[34],
                backs[35],
                backs[36],
                backs[37],
                backs[38],
                backs[39]
        );
    }

    /// Reads ChainContextSubst format 2: class-based input and one lookahead class.
    private static ChainRule[] readChainSubstFormat2(
            ByteBuffer buffer,
            int offset,
            SingleSubst[] singles,
            boolean ignoreMarks,
            int attachType,
            int lookupFlag,
            int markSet
    ) {
        if (buffer.remaining() < 10) {
            return new ChainRule[0];
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int backClassRel = Short.toUnsignedInt(buffer.getShort());
        int backClassOffset = backClassRel == 0 ? 0 : offset + backClassRel;
        int inputClassOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int lookClassOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int setCount = Short.toUnsignedInt(buffer.getShort());
        int[] setOffsets = new int[setCount];
        for (int index = 0; index < setCount; index++) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            int relative = Short.toUnsignedInt(buffer.getShort());
            setOffsets[index] = relative == 0 ? 0 : offset + relative;
        }
        Coverage coverage = readCoverage(buffer, coverageOffset);
        ClassMap backClasses = backClassOffset == 0 ? ClassMap.EMPTY : ClassMap.read(buffer, backClassOffset);
        ClassMap inputClasses = ClassMap.read(buffer, inputClassOffset);
        ClassMap lookClasses = ClassMap.read(buffer, lookClassOffset);
        ChainRule[] rules = new ChainRule[Math.max(4, coverage.size())];
        int written = 0;
        for (int index = 0; index < coverage.size(); index++) {
            int first = coverage.glyphAt(index);
            if (first < 0) {
                continue;
            }
            int firstClass = inputClasses.classOf(first);
            if (firstClass < 0 || firstClass >= setCount || setOffsets[firstClass] == 0) {
                continue;
            }
            if (setOffsets[firstClass] + 2 > buffer.limit()) {
                continue;
            }
            int saved = buffer.position();
            buffer.position(setOffsets[firstClass]);
            int ruleCount = Short.toUnsignedInt(buffer.getShort());
            int[] ruleOffsets = new int[ruleCount];
            for (int ruleIndex = 0; ruleIndex < ruleCount && buffer.remaining() >= 2; ruleIndex++) {
                ruleOffsets[ruleIndex] = setOffsets[firstClass] + Short.toUnsignedInt(buffer.getShort());
            }
            buffer.position(saved);
            for (int ruleOffset : ruleOffsets) {
                if (ruleOffset + 12 > buffer.limit()) {
                    continue;
                }
                saved = buffer.position();
                buffer.position(ruleOffset);
                int @Nullable [] backClassIds = readBacktrackIds(buffer, 40);
                if (backClassIds == null) {
                    buffer.position(saved);
                    continue;
                }
                int inputCount = Short.toUnsignedInt(buffer.getShort());
                if (inputCount != 2 || buffer.remaining() < 2) {
                    buffer.position(saved);
                    continue;
                }
                int secondClass = Short.toUnsignedInt(buffer.getShort());
                int lookaheadCount = Short.toUnsignedInt(buffer.getShort());
                if (lookaheadCount != 1 || buffer.remaining() < 2) {
                    buffer.position(saved);
                    continue;
                }
                int lookClass = Short.toUnsignedInt(buffer.getShort());
                int substCount = Short.toUnsignedInt(buffer.getShort());
                if (substCount < 1 || buffer.remaining() < 4) {
                    buffer.position(saved);
                    continue;
                }
                int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
                int lookupIndex = Short.toUnsignedInt(buffer.getShort());
                buffer.position(saved);
                if (sequenceIndex != 0 || lookupIndex >= singles.length || singles[lookupIndex] == null) {
                    continue;
                }
                int substitute = singles[lookupIndex].apply(first);
                int[] nearGlyphs = backClassIds[0] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassIds[0]);
                int[] midGlyphs = backClassIds[1] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassIds[1]);
                int[] farGlyphs = backClassIds[2] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassIds[2]);
                int[] fartherGlyphs = backClassIds[3] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassIds[3]);
                int[] fifthGlyphs = backClassIds[4] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassIds[4]);
                int[] sixthGlyphs = backClassIds[5] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassIds[5]);
                int[] seventhGlyphs = backClassIds[6] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassIds[6]);
                for (int seventh : seventhGlyphs) {
                    for (int sixth : sixthGlyphs) {
                        for (int fifth : fifthGlyphs) {
                            for (int farther : fartherGlyphs) {
                                for (int far : farGlyphs) {
                                    for (int mid : midGlyphs) {
                                        for (int near : nearGlyphs) {
                                            for (int second : inputClasses.glyphsOf(secondClass)) {
                                                for (int look : lookClasses.glyphsOf(lookClass)) {
                                                    if (written == rules.length) {
                                                        rules = Arrays.copyOf(rules, rules.length * 2);
                                                    }
                                                    rules[written++] = new ChainRule(
                                                            first,
                                                            second,
                                                            look,
                                                            substitute,
                                                            ignoreMarks,
                                                            attachType,
                                                            lookupFlag,
                                                            markSet,
                                                            near,
                                                            mid,
                                                            far,
                                                            farther,
                                                            fifth,
                                                            sixth,
                                                            seventh,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            0
                                                    );
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return written == rules.length ? rules : Arrays.copyOf(rules, written);
    }

    /// Reads ChainContextSubst format 3 with two input coverages and one lookahead coverage.
    private static ChainRule[] readChainSubstFormat3(
            ByteBuffer buffer,
            int offset,
            SingleSubst[] singles,
            boolean ignoreMarks,
            int attachType,
            int lookupFlag,
            int markSet
    ) {
        if (buffer.remaining() < 2) {
            return new ChainRule[0];
        }
        int backtrackCount = Short.toUnsignedInt(buffer.getShort());
        int nearCoverage = 0;
        int midCoverage = 0;
        int farCoverage = 0;
        int fartherCoverage = 0;
        int fifthCoverage = 0;
        int sixthCoverage = 0;
        int seventhCoverage = 0;
        int eighthCoverage = 0;
        if (backtrackCount > 40) {
            return new ChainRule[0];
        }
        if (backtrackCount >= 1) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            nearCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 2) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            midCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 3) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            farCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 4) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            fartherCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 5) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            fifthCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 6) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            sixthCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 7) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            seventhCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 8) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            eighthCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 9) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount >= 10) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount >= 11) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 12) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 13) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 14) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 15) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 16) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 17) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 18) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 19) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 20) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 21) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 22) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 23) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 24) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 25) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 26) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 27) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 28) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 29) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 30) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 31) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 32) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 33) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 34) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 35) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 36) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 37) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 38) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 39) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 40) {
            if (buffer.remaining() < 2) {
                return new ChainRule[0];
            }
            buffer.getShort();
        }
        if (buffer.remaining() < 2) {
            return new ChainRule[0];
        }
        int inputCount = Short.toUnsignedInt(buffer.getShort());
        if (inputCount != 2 || buffer.remaining() < 4) {
            return new ChainRule[0];
        }
        int firstCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        int secondCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        if (buffer.remaining() < 2) {
            return new ChainRule[0];
        }
        int lookaheadCount = Short.toUnsignedInt(buffer.getShort());
        if (lookaheadCount != 1 || buffer.remaining() < 2) {
            return new ChainRule[0];
        }
        int lookaheadCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        if (buffer.remaining() < 2) {
            return new ChainRule[0];
        }
        int substCount = Short.toUnsignedInt(buffer.getShort());
        if (substCount < 1 || buffer.remaining() < 4) {
            return new ChainRule[0];
        }
        int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
        int lookupIndex = Short.toUnsignedInt(buffer.getShort());
        if (sequenceIndex != 0 || lookupIndex >= singles.length || singles[lookupIndex] == null) {
            return new ChainRule[0];
        }
        Coverage firsts = readCoverage(buffer, firstCoverage);
        Coverage seconds = readCoverage(buffer, secondCoverage);
        Coverage looks = readCoverage(buffer, lookaheadCoverage);
        int[] nearGlyphs = nearCoverage == 0 ? new int[] {0} : coverageGlyphs(readCoverage(buffer, nearCoverage));
        int[] midGlyphs = midCoverage == 0 ? new int[] {0} : coverageGlyphs(readCoverage(buffer, midCoverage));
        int[] farGlyphs = farCoverage == 0 ? new int[] {0} : coverageGlyphs(readCoverage(buffer, farCoverage));
        int[] fartherGlyphs = fartherCoverage == 0 ? new int[] {0} : coverageGlyphs(readCoverage(buffer, fartherCoverage));
        int[] fifthGlyphs = fifthCoverage == 0 ? new int[] {0} : coverageGlyphs(readCoverage(buffer, fifthCoverage));
        int[] sixthGlyphs = sixthCoverage == 0 ? new int[] {0} : coverageGlyphs(readCoverage(buffer, sixthCoverage));
        int[] seventhGlyphs = seventhCoverage == 0 ? new int[] {0} : coverageGlyphs(readCoverage(buffer, seventhCoverage));
        ChainRule[] rules = new ChainRule[Math.max(
                0,
                firsts.size() * seconds.size() * looks.size()
                        * nearGlyphs.length * midGlyphs.length * farGlyphs.length
                        * fartherGlyphs.length * fifthGlyphs.length * sixthGlyphs.length
                        * seventhGlyphs.length
        )];
        int written = 0;
        for (int firstIndex = 0; firstIndex < firsts.size(); firstIndex++) {
            int first = firsts.glyphAt(firstIndex);
            int substitute = singles[lookupIndex].apply(first);
            for (int secondIndex = 0; secondIndex < seconds.size(); secondIndex++) {
                int second = seconds.glyphAt(secondIndex);
                for (int lookIndex = 0; lookIndex < looks.size(); lookIndex++) {
                    int look = looks.glyphAt(lookIndex);
                    if (first < 0 || second < 0 || look < 0) {
                        continue;
                    }
                    for (int seventh : seventhGlyphs) {
                        for (int sixth : sixthGlyphs) {
                            for (int fifth : fifthGlyphs) {
                                for (int farther : fartherGlyphs) {
                                    for (int far : farGlyphs) {
                                        for (int mid : midGlyphs) {
                                            for (int near : nearGlyphs) {
                                                rules[written++] = new ChainRule(
                                                        first,
                                                        second,
                                                        look,
                                                        substitute,
                                                        ignoreMarks,
                                                        attachType,
                                                        lookupFlag,
                                                        markSet,
                                                        near,
                                                        mid,
                                                        far,
                                                        farther,
                                                        fifth,
                                                        sixth,
                                                        seventh,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0,
                                                        0
                                                );
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return written == rules.length ? rules : Arrays.copyOf(rules, written);
    }

    /// Reads a type-8 reverse-chain format-1 one-lookahead substitution.
    private static ReverseRule[] readReverseSubst(ByteBuffer buffer, int offset, int lookupFlag, int markSet) {
        if (offset + 12 > buffer.limit()) {
            return new ReverseRule[0];
        }
        int saved = buffer.position();
        buffer.position(offset);
        if (Short.toUnsignedInt(buffer.getShort()) != 1) {
            buffer.position(saved);
            return new ReverseRule[0];
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int backtrackCount = Short.toUnsignedInt(buffer.getShort());
        if (backtrackCount > 40) {
            buffer.position(saved);
            return new ReverseRule[0];
        }
        int nearCoverageOffset = 0;
        int midCoverageOffset = 0;
        int farCoverageOffset = 0;
        int fartherCoverageOffset = 0;
        int fifthCoverageOffset = 0;
        int sixthCoverageOffset = 0;
        int seventhCoverageOffset = 0;
        int eighthCoverageOffset = 0;
        if (backtrackCount >= 1) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            nearCoverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 2) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            midCoverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 3) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            farCoverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 4) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            fartherCoverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 5) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            fifthCoverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 6) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            sixthCoverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 7) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            seventhCoverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 8) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            eighthCoverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 9) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount >= 10) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount >= 11) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 12) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 13) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 14) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 15) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 16) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 17) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 18) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 19) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 20) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 21) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        if (backtrackCount == 22) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            buffer.getShort();
        }
        int lookaheadCount = Short.toUnsignedInt(buffer.getShort());
        if (lookaheadCount != 1 || buffer.remaining() < 2) {
            buffer.position(saved);
            return new ReverseRule[0];
        }
        int lookaheadCoverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int glyphCount = Short.toUnsignedInt(buffer.getShort());
        int[] substitutes = new int[glyphCount];
        for (int index = 0; index < glyphCount; index++) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return new ReverseRule[0];
            }
            substitutes[index] = Short.toUnsignedInt(buffer.getShort());
        }
        Coverage coverage = readCoverage(buffer, coverageOffset);
        Coverage lookahead = readCoverage(buffer, lookaheadCoverageOffset);
        int look = lookahead.glyphAt(0);
        int backtrack = 0;
        int backtrackFar = 0;
        int backtrackFarther = 0;
        int backtrackFarthest = 0;
        int backtrackFifth = 0;
        if (nearCoverageOffset != 0) {
            backtrack = readCoverage(buffer, nearCoverageOffset).glyphAt(0);
        }
        if (midCoverageOffset != 0) {
            backtrackFar = readCoverage(buffer, midCoverageOffset).glyphAt(0);
        }
        if (farCoverageOffset != 0) {
            backtrackFarther = readCoverage(buffer, farCoverageOffset).glyphAt(0);
        }
        if (fartherCoverageOffset != 0) {
            backtrackFarthest = readCoverage(buffer, fartherCoverageOffset).glyphAt(0);
        }
        if (fifthCoverageOffset != 0) {
            backtrackFifth = readCoverage(buffer, fifthCoverageOffset).glyphAt(0);
        }
        int backtrackSixth = 0;
        if (sixthCoverageOffset != 0) {
            backtrackSixth = readCoverage(buffer, sixthCoverageOffset).glyphAt(0);
        }
        int backtrackSeventh = 0;
        if (seventhCoverageOffset != 0) {
            backtrackSeventh = readCoverage(buffer, seventhCoverageOffset).glyphAt(0);
        }
        int backtrackEighth = 0;
        if (eighthCoverageOffset != 0) {
            backtrackEighth = readCoverage(buffer, eighthCoverageOffset).glyphAt(0);
        }
        ReverseRule[] rules = new ReverseRule[glyphCount];
        int written = 0;
        for (int index = 0; index < glyphCount; index++) {
            int current = coverage.glyphAt(index);
            if (current < 0 || look < 0) {
                continue;
            }
            rules[written++] = new ReverseRule(
                    current,
                    look,
                    substitutes[index],
                    lookupFlag,
                    markSet,
                    backtrack,
                    backtrackFar,
                    backtrackFarther,
                    backtrackFarthest,
                    backtrackFifth,
                    backtrackSixth,
                    backtrackSeventh,
                    backtrackEighth,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }
        buffer.position(saved);
        return written == rules.length ? rules : Arrays.copyOf(rules, written);
    }

    /// Reads a type-4 ligature substitution subtable.
    private static LigatureSubst readLigatureSubst(
            ByteBuffer buffer,
            int offset,
            int lookupFlag,
            int markSet
    ) {
        if (offset + 6 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB ligature subst is truncated");
        }
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        if (format != 1) {
            throw new IllegalArgumentException("Unsupported GSUB ligature subst format " + format);
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int setCount = Short.toUnsignedInt(buffer.getShort());
        int[] setOffsets = new int[setCount];
        for (int index = 0; index < setCount; index++) {
            if (buffer.remaining() < 2) {
                throw new IllegalArgumentException("GSUB ligature set offset is truncated");
            }
            setOffsets[index] = offset + Short.toUnsignedInt(buffer.getShort());
        }
        Coverage coverage = readCoverage(buffer, coverageOffset);
        Ligature[][] sets = new Ligature[setCount][];
        for (int index = 0; index < setCount; index++) {
            sets[index] = readLigatureSet(buffer, setOffsets[index]);
        }
        return new LigatureSubst(coverage, sets, lookupFlag, markSet);
    }

    /// Reads one ligature set.
    private static Ligature[] readLigatureSet(ByteBuffer buffer, int offset) {
        if (offset + 2 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB ligature set is truncated");
        }
        int saved = buffer.position();
        buffer.position(offset);
        int count = Short.toUnsignedInt(buffer.getShort());
        int[] ligatureOffsets = new int[count];
        for (int index = 0; index < count; index++) {
            if (buffer.remaining() < 2) {
                throw new IllegalArgumentException("GSUB ligature offset is truncated");
            }
            ligatureOffsets[index] = offset + Short.toUnsignedInt(buffer.getShort());
        }
        Ligature[] ligatures = new Ligature[count];
        for (int index = 0; index < count; index++) {
            ligatures[index] = readLigature(buffer, ligatureOffsets[index]);
        }
        buffer.position(saved);
        return ligatures;
    }

    /// Reads one ligature rule.
    private static Ligature readLigature(ByteBuffer buffer, int offset) {
        if (offset + 4 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB ligature is truncated");
        }
        int saved = buffer.position();
        buffer.position(offset);
        int glyph = Short.toUnsignedInt(buffer.getShort());
        int componentCount = Short.toUnsignedInt(buffer.getShort());
        if (componentCount < 2) {
            throw new IllegalArgumentException("GSUB ligature must consume at least two glyphs");
        }
        int[] rest = new int[componentCount - 1];
        for (int index = 0; index < rest.length; index++) {
            if (buffer.remaining() < 2) {
                throw new IllegalArgumentException("GSUB ligature component is truncated");
            }
            rest[index] = Short.toUnsignedInt(buffer.getShort());
        }
        buffer.position(saved);
        return new Ligature(glyph, rest);
    }

    /// Reads a coverage table.
    private static Coverage readCoverage(ByteBuffer buffer, int offset) {
        if (offset + 4 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB coverage is truncated");
        }
        int saved = buffer.position();
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        if (format == 1) {
            int count = Short.toUnsignedInt(buffer.getShort());
            int[] glyphs = new int[count];
            for (int index = 0; index < count; index++) {
                glyphs[index] = Short.toUnsignedInt(buffer.getShort());
            }
            buffer.position(saved);
            return new Coverage(glyphs, null, null, null);
        }
        if (format != 2) {
            throw new IllegalArgumentException("Unsupported GSUB coverage format " + format);
        }
        int count = Short.toUnsignedInt(buffer.getShort());
        int[] starts = new int[count];
        int[] ends = new int[count];
        int[] startIndices = new int[count];
        for (int index = 0; index < count; index++) {
            starts[index] = Short.toUnsignedInt(buffer.getShort());
            ends[index] = Short.toUnsignedInt(buffer.getShort());
            startIndices[index] = Short.toUnsignedInt(buffer.getShort());
        }
        buffer.position(saved);
        return new Coverage(null, starts, ends, startIndices);
    }

    /// Stores one named feature and its type-1 through type-6 lookups.
    ///
    /// @param tag the feature tag
    /// @param singles the type-1 lookups in apply order
    /// @param ligatures the type-4 lookups in apply order
    /// @param multiples the type-2 lookups in apply order
    /// @param alternates the type-3 lookups in apply order
    /// @param contexts flattened type-5 two-glyph rules
    /// @param chains flattened type-6 one-lookahead rules
    /// @param reverses flattened type-8 one-lookahead reverse rules
    private record Feature(
            int tag,
            SingleSubst[] singles,
            LigatureSubst[] ligatures,
            MultipleSubst[] multiples,
            AlternateSubst[] alternates,
            ContextRule[] contexts,
            ChainRule[] chains,
            ReverseRule[] reverses
    ) {
    }

    /// Stores parsed lookup slots aligned by lookup-list index.
    ///
    /// @param singles type-1 lookups, or `null` slots
    /// @param ligatures type-4 lookups, or `null` slots
    /// @param multiples type-2 lookups, or `null` slots
    /// @param alternates type-3 lookups, or `null` slots
    /// @param contexts type-5 rules per lookup, or `null` slots
    /// @param chains type-6 rules per lookup, or `null` slots
    /// @param reverses type-8 rules per lookup, or `null` slots
    private record LookupTable(
            SingleSubst[] singles,
            LigatureSubst[] ligatures,
            MultipleSubst[] multiples,
            AlternateSubst[] alternates,
            ContextRule[][] contexts,
            ChainRule[][] chains,
            ReverseRule[][] reverses
    ) {
    }

    /// Coverage plus per-index glyph sequences.
    ///
    /// @param coverage the input coverage
    /// @param sequences sequences in coverage order
    private record SequenceTable(Coverage coverage, int[][] sequences) {
        /// Returns the sequence for `glyphId`, or `null`.
        private int @Nullable [] sequence(int glyphId) {
            int index = coverage.indexOf(glyphId);
            if (index < 0 || index >= sequences.length) {
                return null;
            }
            return sequences[index];
        }
    }

    /// Stores one type-2 multiple substitution.
    private static final class MultipleSubst {
        /// Coverage-indexed sequences.
        private final SequenceTable table;

        /// Creates a multiple substitution.
        ///
        /// @param table the sequences
        private MultipleSubst(SequenceTable table) {
            this.table = table;
        }

        /// Returns the substitute sequence, or `null`.
        ///
        /// @param glyphId the input
        /// @return the sequence
        private int @Nullable [] apply(int glyphId) {
            return table.sequence(glyphId);
        }
    }

    /// Stores one type-3 alternate substitution.
    private static final class AlternateSubst {
        /// Coverage-indexed alternate sets.
        private final SequenceTable table;

        /// Creates an alternate substitution.
        ///
        /// @param table the alternate sets
        private AlternateSubst(SequenceTable table) {
            this.table = table;
        }

        /// Returns the first alternate, or `glyphId`.
        ///
        /// @param glyphId the input
        /// @return the alternate
        private int apply(int glyphId) {
            int @Nullable [] set = table.sequence(glyphId);
            if (set == null || set.length == 0) {
                return glyphId;
            }
            return set[0];
        }
    }

    /// One type-5 two-glyph substitution.
    ///
    /// @param current the first input glyph
    /// @param next the second input glyph
    /// @param substitute the replacement for `current`
    /// @param ignoreMarks whether the lookup skips every GDEF mark
    /// @param attachType the `MarkAttachmentType` class, or `0`
    /// @param lookupFlag the full lookup flag word
    /// @param markSet the `UseMarkFilteringSet` index, or `0`
    private record ContextRule(
            int current,
            int next,
            int substitute,
            boolean ignoreMarks,
            int attachType,
            int lookupFlag,
            int markSet
    ) {
    }

    /// One type-6 one-lookahead substitution.
    ///
    /// @param current the first input glyph
    /// @param next the second input glyph
    /// @param lookahead the first lookahead glyph
    /// @param substitute the replacement for `current`
    /// @param ignoreMarks whether the lookup skips every GDEF mark
    /// @param attachType the `MarkAttachmentType` class, or `0`
    /// @param lookupFlag the full lookup flag word
    /// @param markSet the `UseMarkFilteringSet` index, or `0`
    /// @param backtrack the nearest required preceding glyph, or `0` when unused
    /// @param backtrackFar the next required preceding glyph, or `0` when unused
    /// @param backtrackFarther the next required preceding glyph, or `0` when unused
    /// @param backtrackFarthest the fourth required preceding glyph, or `0` when unused
    /// @param backtrackFifth the fifth required preceding glyph, or `0` when unused
    /// @param backtrackSixth the sixth required preceding glyph, or `0` when unused
    /// @param backtrackSeventh the seventh required preceding glyph, or `0` when unused
    /// @param backtrackEighth the eighth required preceding glyph, or `0` when unused
    /// @param backtrackNinth the ninth required preceding glyph, or `0` when unused
    /// @param backtrackTenth the tenth required preceding glyph, or `0` when unused
    /// @param backtrackEleventh the eleventh required preceding glyph, or `0` when unused
    /// @param backtrackTwelfth the twelfth required preceding glyph, or `0` when unused
    /// @param backtrackThirteenth the thirteenth required preceding glyph, or `0` when unused
    /// @param backtrackFourteenth the fourteenth required preceding glyph, or `0` when unused
    /// @param backtrackFifteenth the fifteenth required preceding glyph, or `0` when unused
    /// @param backtrackSixteenth the sixteenth required preceding glyph, or `0` when unused
    /// @param backtrackSeventeenth the seventeenth required preceding glyph, or `0` when unused
    /// @param backtrackEighteenth the eighteenth required preceding glyph, or `0` when unused
    /// @param backtrackNineteenth the nineteenth required preceding glyph, or `0` when unused
    /// @param backtrackTwentieth the twentieth required preceding glyph, or `0` when unused
    /// @param backtrackTwentyFirst the twenty-first required preceding glyph, or `0` when unused
    /// @param backtrackTwentySecond the twenty-second required preceding glyph, or `0` when unused
    /// @param backtrackTwentyThird the twenty-third required preceding glyph, or `0` when unused
    /// @param backtrackTwentyFourth the twenty-fourth required preceding glyph, or `0` when unused
    /// @param backtrackTwentyFifth the twenty-fifth required preceding glyph, or `0` when unused
    /// @param backtrackTwentySixth the twenty-sixth required preceding glyph, or `0` when unused
    /// @param backtrackTwentySeventh the twenty-seventh required preceding glyph, or `0` when unused
    /// @param backtrackTwentyEighth the twenty-eighth required preceding glyph, or `0` when unused
    /// @param backtrackTwentyNinth the twenty-ninth required preceding glyph, or `0` when unused
    /// @param backtrackThirtieth the thirtieth required preceding glyph, or `0` when unused
    /// @param backtrackThirtyFirst the thirty-first required preceding glyph, or `0` when unused
    /// @param backtrackThirtySecond the thirty-second required preceding glyph, or `0` when unused
    /// @param backtrackThirtyThird the thirty-third required preceding glyph, or `0` when unused
    /// @param backtrackThirtyFourth the thirty-fourth required preceding glyph, or `0` when unused
    /// @param backtrackThirtyFifth the thirty-fifth required preceding glyph, or `0` when unused
    /// @param backtrackThirtySixth the thirty-sixth required preceding glyph, or `0` when unused
    /// @param backtrackThirtySeventh the thirty-seventh required preceding glyph, or `0` when unused
    /// @param backtrackThirtyEighth the thirty-eighth required preceding glyph, or `0` when unused
    /// @param backtrackThirtyNinth the thirty-ninth required preceding glyph, or `0` when unused
    /// @param backtrackFortieth the fortieth required preceding glyph, or `0` when unused
    private record ChainRule(
            int current,
            int next,
            int lookahead,
            int substitute,
            boolean ignoreMarks,
            int attachType,
            int lookupFlag,
            int markSet,
            int backtrack,
            int backtrackFar,
            int backtrackFarther,
            int backtrackFarthest,
            int backtrackFifth,
            int backtrackSixth,
            int backtrackSeventh,
            int backtrackEighth,
            int backtrackNinth,
            int backtrackTenth,
            int backtrackEleventh,
            int backtrackTwelfth,
            int backtrackThirteenth,
            int backtrackFourteenth,
            int backtrackFifteenth,
            int backtrackSixteenth,
            int backtrackSeventeenth,
            int backtrackEighteenth,
            int backtrackNineteenth,
            int backtrackTwentieth,
            int backtrackTwentyFirst,
            int backtrackTwentySecond,
            int backtrackTwentyThird,
            int backtrackTwentyFourth,
            int backtrackTwentyFifth,
            int backtrackTwentySixth,
            int backtrackTwentySeventh,
            int backtrackTwentyEighth,
            int backtrackTwentyNinth,
            int backtrackThirtieth,
            int backtrackThirtyFirst,
            int backtrackThirtySecond,
            int backtrackThirtyThird,
            int backtrackThirtyFourth,
            int backtrackThirtyFifth,
            int backtrackThirtySixth,
            int backtrackThirtySeventh,
            int backtrackThirtyEighth,
            int backtrackThirtyNinth,
            int backtrackFortieth
    ) {
    }

    /// One type-8 reverse-chain substitution.
    ///
    /// @param current the input glyph
    /// @param lookahead the following glyph
    /// @param substitute the replacement for `current`
    /// @param lookupFlag the full lookup flag word
    /// @param markSet the `UseMarkFilteringSet` index, or `0`
    /// @param backtrack the nearest required preceding glyph, or `0` when unused
    /// @param backtrackFar the next required preceding glyph, or `0` when unused
    /// @param backtrackFarther the next required preceding glyph, or `0` when unused
    /// @param backtrackFarthest the fourth required preceding glyph, or `0` when unused
    /// @param backtrackFifth the fifth required preceding glyph, or `0` when unused
    /// @param backtrackSixth the sixth required preceding glyph, or `0` when unused
    /// @param backtrackSeventh the seventh required preceding glyph, or `0` when unused
    /// @param backtrackEighth the eighth required preceding glyph, or `0` when unused
    /// @param backtrackNinth the ninth required preceding glyph, or `0` when unused
    /// @param backtrackTenth the tenth required preceding glyph, or `0` when unused
    /// @param backtrackEleventh the eleventh required preceding glyph, or `0` when unused
    /// @param backtrackTwelfth the twelfth required preceding glyph, or `0` when unused
    /// @param backtrackThirteenth the thirteenth required preceding glyph, or `0` when unused
    /// @param backtrackFourteenth the fourteenth required preceding glyph, or `0` when unused
    /// @param backtrackFifteenth the fifteenth required preceding glyph, or `0` when unused
    /// @param backtrackSixteenth the sixteenth required preceding glyph, or `0` when unused
    /// @param backtrackSeventeenth the seventeenth required preceding glyph, or `0` when unused
    /// @param backtrackEighteenth the eighteenth required preceding glyph, or `0` when unused
    /// @param backtrackNineteenth the nineteenth required preceding glyph, or `0` when unused
    /// @param backtrackTwentieth the twentieth required preceding glyph, or `0` when unused
    /// @param backtrackTwentyFirst the twenty-first required preceding glyph, or `0` when unused
    /// @param backtrackTwentySecond the twenty-second required preceding glyph, or `0` when unused
    /// @param backtrackTwentyThird the twenty-third required preceding glyph, or `0` when unused
    /// @param backtrackTwentyFourth the twenty-fourth required preceding glyph, or `0` when unused
    /// @param backtrackTwentyFifth the twenty-fifth required preceding glyph, or `0` when unused
    /// @param backtrackTwentySixth the twenty-sixth required preceding glyph, or `0` when unused
    /// @param backtrackTwentySeventh the twenty-seventh required preceding glyph, or `0` when unused
    /// @param backtrackTwentyEighth the twenty-eighth required preceding glyph, or `0` when unused
    /// @param backtrackTwentyNinth the twenty-ninth required preceding glyph, or `0` when unused
    /// @param backtrackThirtieth the thirtieth required preceding glyph, or `0` when unused
    /// @param backtrackThirtyFirst the thirty-first required preceding glyph, or `0` when unused
    /// @param backtrackThirtySecond the thirty-second required preceding glyph, or `0` when unused
    /// @param backtrackThirtyThird the thirty-third required preceding glyph, or `0` when unused
    /// @param backtrackThirtyFourth the thirty-fourth required preceding glyph, or `0` when unused
    /// @param backtrackThirtyFifth the thirty-fifth required preceding glyph, or `0` when unused
    /// @param backtrackThirtySixth the thirty-sixth required preceding glyph, or `0` when unused
    /// @param backtrackThirtySeventh the thirty-seventh required preceding glyph, or `0` when unused
    /// @param backtrackThirtyEighth the thirty-eighth required preceding glyph, or `0` when unused
    /// @param backtrackThirtyNinth the thirty-ninth required preceding glyph, or `0` when unused
    /// @param backtrackFortieth the fortieth required preceding glyph, or `0` when unused
    private record ReverseRule(
            int current,
            int lookahead,
            int substitute,
            int lookupFlag,
            int markSet,
            int backtrack,
            int backtrackFar,
            int backtrackFarther,
            int backtrackFarthest,
            int backtrackFifth,
            int backtrackSixth,
            int backtrackSeventh,
            int backtrackEighth,
            int backtrackNinth,
            int backtrackTenth,
            int backtrackEleventh,
            int backtrackTwelfth,
            int backtrackThirteenth,
            int backtrackFourteenth,
            int backtrackFifteenth,
            int backtrackSixteenth,
            int backtrackSeventeenth,
            int backtrackEighteenth,
            int backtrackNineteenth,
            int backtrackTwentieth,
            int backtrackTwentyFirst,
            int backtrackTwentySecond,
            int backtrackTwentyThird,
            int backtrackTwentyFourth,
            int backtrackTwentyFifth,
            int backtrackTwentySixth,
            int backtrackTwentySeventh,
            int backtrackTwentyEighth,
            int backtrackTwentyNinth,
            int backtrackThirtieth,
            int backtrackThirtyFirst,
            int backtrackThirtySecond,
            int backtrackThirtyThird,
            int backtrackThirtyFourth,
            int backtrackThirtyFifth,
            int backtrackThirtySixth,
            int backtrackThirtySeventh,
            int backtrackThirtyEighth,
            int backtrackThirtyNinth,
            int backtrackFortieth
    ) {
    }

    /// Stores one type-4 ligature substitution.
    private static final class LigatureSubst {
        /// Coverage of first glyphs.
        private final Coverage coverage;

        /// Ligature sets in coverage order.
        private final Ligature[][] sets;

        /// Full lookup flag word used by [`GdefTable#skip(int, int, int)`].
        private final int lookupFlag;

        /// `UseMarkFilteringSet` index, or `0`.
        private final int markSet;

        /// Creates a ligature substitution.
        ///
        /// @param coverage the first-glyph coverage
        /// @param sets the ligature sets
        /// @param lookupFlag the lookup flag word
        /// @param markSet the mark-filter set index
        private LigatureSubst(Coverage coverage, Ligature[][] sets, int lookupFlag, int markSet) {
            this.coverage = coverage;
            this.sets = sets;
            this.lookupFlag = lookupFlag;
            this.markSet = markSet;
        }

        /// Applies the first matching ligature in table order.
        ///
        /// @param glyphIds the mapped glyphs
        /// @param start the first glyph index
        /// @param remaining the available length
        /// @param gdef the GDEF classes
        /// @return the match, or `null`
        private @Nullable GlyphLigature apply(int[] glyphIds, int start, int remaining, GdefTable gdef) {
            int coverageIndex = coverage.indexOf(glyphIds[start]);
            if (coverageIndex < 0 || coverageIndex >= sets.length) {
                return null;
            }
            Ligature[] candidates = sets[coverageIndex];
            for (Ligature ligature : candidates) {
                @Nullable GlyphLigature match = match(ligature, glyphIds, start, remaining, gdef);
                if (match != null) {
                    return match;
                }
            }
            return null;
        }

        /// Matches one ligature, optionally skipping marks.
        private @Nullable GlyphLigature match(
                Ligature ligature,
                int[] glyphIds,
                int start,
                int remaining,
                GdefTable gdef
        ) {
            int cursor = 1;
            for (int component : ligature.rest) {
                while (cursor < remaining && gdef.skip(glyphIds[start + cursor], lookupFlag, markSet)) {
                    cursor++;
                }
                if (cursor >= remaining || glyphIds[start + cursor] != component) {
                    return null;
                }
                cursor++;
            }
            return new GlyphLigature(ligature.glyph, cursor);
        }

        /// Returns whether this lookup skips `glyphId`.
        private boolean skip(int glyphId, GdefTable gdef) {
            return gdef.skip(glyphId, lookupFlag, markSet);
        }
    }

    /// Stores one ligature rule.
    ///
    /// @param glyph the substitute glyph
    /// @param rest the remaining component glyph ids
    private record Ligature(int glyph, int[] rest) {
    }

    /// Stores one type-1 substitution.
    private static final class SingleSubst {
        /// Coverage of input glyphs.
        private final Coverage coverage;

        /// Format-1 delta, ignored when [`#substitutes`] is present.
        private final int delta;

        /// Format-2 substitute glyphs, or `null` for format 1.
        private final int @Nullable [] substitutes;

        /// Creates a substitution.
        ///
        /// @param coverage the coverage
        /// @param delta the format-1 delta
        /// @param substitutes the format-2 array, or `null`
        private SingleSubst(Coverage coverage, int delta, int @Nullable [] substitutes) {
            this.coverage = coverage;
            this.delta = delta;
            this.substitutes = substitutes;
        }

        /// Applies this substitution.
        ///
        /// @param glyphId the input
        /// @return the output
        private int apply(int glyphId) {
            int index = coverage.indexOf(glyphId);
            if (index < 0) {
                return glyphId;
            }
            if (substitutes != null) {
                if (index >= substitutes.length) {
                    return glyphId;
                }
                return substitutes[index];
            }
            return (glyphId + delta) & 0xFFFF;
        }
    }

    /// Stores coverage format 1 or 2.
    private static final class Coverage {
        /// Format-1 glyph array, or `null`.
        private final int @Nullable [] glyphs;

        /// Format-2 range starts, or `null`.
        private final int @Nullable [] starts;

        /// Format-2 range ends, or `null`.
        private final int @Nullable [] ends;

        /// Format-2 start coverage indices, or `null`.
        private final int @Nullable [] startIndices;

        /// Creates coverage.
        ///
        /// @param glyphs format-1 glyphs
        /// @param starts format-2 starts
        /// @param ends format-2 ends
        /// @param startIndices format-2 start indices
        private Coverage(
                int @Nullable [] glyphs,
                int @Nullable [] starts,
                int @Nullable [] ends,
                int @Nullable [] startIndices
        ) {
            this.glyphs = glyphs;
            this.starts = starts;
            this.ends = ends;
            this.startIndices = startIndices;
        }

        /// Returns the coverage index, or `-1`.
        ///
        /// @param glyphId the glyph
        /// @return the index
        private int indexOf(int glyphId) {
            if (glyphs != null) {
                int index = Arrays.binarySearch(glyphs, glyphId);
                return index >= 0 ? index : -1;
            }
            if (starts == null || ends == null || startIndices == null) {
                return -1;
            }
            for (int index = 0; index < starts.length; index++) {
                if (glyphId >= starts[index] && glyphId <= ends[index]) {
                    return startIndices[index] + (glyphId - starts[index]);
                }
            }
            return -1;
        }

        /// Returns the number of covered glyphs.
        ///
        /// @return the coverage length
        private int size() {
            if (glyphs != null) {
                return glyphs.length;
            }
            if (starts == null || ends == null || startIndices == null) {
                return 0;
            }
            int max = 0;
            for (int index = 0; index < starts.length; index++) {
                int last = startIndices[index] + (ends[index] - starts[index]) + 1;
                if (last > max) {
                    max = last;
                }
            }
            return max;
        }

        /// Returns the glyph at a coverage index, or `-1`.
        ///
        /// @param coverageIndex the coverage index
        /// @return the glyph id
        private int glyphAt(int coverageIndex) {
            if (glyphs != null) {
                return coverageIndex >= 0 && coverageIndex < glyphs.length ? glyphs[coverageIndex] : -1;
            }
            if (starts == null || ends == null || startIndices == null) {
                return -1;
            }
            for (int index = 0; index < starts.length; index++) {
                int first = startIndices[index];
                int last = first + (ends[index] - starts[index]);
                if (coverageIndex >= first && coverageIndex <= last) {
                    return starts[index] + (coverageIndex - first);
                }
            }
            return -1;
        }
    }
}
