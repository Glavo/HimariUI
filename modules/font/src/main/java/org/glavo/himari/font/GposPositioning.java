package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/// Applies GPOS single, pair, cursive, mark, contextual, and chained positioning.
///
/// Type-1 X-advance values are published through [`#singleAdjustment(int)`]. Type-7 Format-1
/// two-glyph rules flatten into a pair map. Type-8 Format-1 rules with one lookahead glyph
/// publish [`#chainAdjustment(int, int, int)`]. Type-8 rules may require up to nine preceding
/// backtrack glyphs, matched by [`#chainAdjustment(int[], int, int)`]. Lookups with `IgnoreMarks` (`0x0008`) write
/// [`#skipPairAdjustment(int, int)`] and [`#skipChainAdjustment(int, int, int)`]. Lookups with
/// a non-zero `MarkAttachmentType` (`0xFF00`) write the attach maps. Other formats are skipped.
@NotNullByDefault
final class GposPositioning {
    /// Empty positioning.
    static final GposPositioning NONE = empty();

    /// Shared empty nine-slot backtrack walk.
    private static final int[] EMPTY_BACKS = {0, 0, 0, 0, 0, 0, 0, 0, 0};

    /// Packed `(left << 16) | right` keys, sorted.
    private final int[] keys;

    /// Parallel X-advance deltas in font units.
    private final short[] deltas;

    /// Sorted single-positioning glyphs.
    private final int[] singleGlyphs;

    /// Parallel single X-advance deltas.
    private final short[] singleDeltas;

    /// Packed `(current << 16) | next` keys for type-8 rules.
    private final int[] chainPairs;

    /// Parallel lookahead glyphs for type-8 rules.
    private final int[] chainLooks;

    /// Parallel type-8 X-advance deltas.
    private final short[] chainDeltas;

    /// Packed required preceding glyphs for type-8 rules, or `0` when unused.
    private final long[] chainBacks;

    /// Fifth required preceding glyph for type-8 rules, or `0` when unused.
    private final int[] chainFifths;

    /// Seventh required preceding glyph for type-8 rules, or `0` when unused.
    private final int[] chainSevenths;

    /// Ninth required preceding glyph for type-8 rules, or `0` when unused.
    private final int[] chainNinths;

    /// Packed `(mark << 16) | base` keys, sorted.
    private final int[] markKeys;

    /// Parallel mark X offsets.
    private final short[] markXs;

    /// Parallel mark Y offsets.
    private final short[] markYs;

    /// Sorted unique mark glyph ids.
    private final int[] markGlyphs;

    /// Packed `IgnoreMarks` pair keys, sorted.
    private final int[] skipKeys;

    /// Parallel `IgnoreMarks` pair deltas.
    private final short[] skipDeltas;

    /// Packed `IgnoreMarks` type-8 current/next keys.
    private final int[] skipChainPairs;

    /// Parallel `IgnoreMarks` type-8 lookahead glyphs.
    private final int[] skipChainLooks;

    /// Parallel `IgnoreMarks` type-8 deltas.
    private final short[] skipChainDeltas;

    /// Packed required preceding glyphs for `IgnoreMarks` type-8 rules, or `0`.
    private final long[] skipChainBacks;

    /// Packed `MarkAttachmentType` pair keys.
    private final int[] attachKeys;

    /// Parallel `MarkAttachmentType` pair deltas.
    private final short[] attachDeltas;

    /// Parallel pair lookup attach classes.
    private final int[] attachTypes;

    /// Packed `MarkAttachmentType` type-8 current/next keys.
    private final int[] attachChainPairs;

    /// Parallel `MarkAttachmentType` type-8 lookahead glyphs.
    private final int[] attachChainLooks;

    /// Parallel `MarkAttachmentType` type-8 deltas.
    private final short[] attachChainDeltas;

    /// Parallel type-8 lookup attach classes.
    private final int[] attachChainTypes;

    /// Packed required preceding glyphs for attach type-8 rules, or `0`.
    private final long[] attachChainBacks;

    /// Unique non-zero `MarkAttachmentType` values used by pair or chain lookups.
    private final int[] attachmentTypes;

    /// GDEF classes used by flagged pair and chain matching.
    private final GdefTable gdef;

    /// Pairs whose lookups use `IgnoreBaseGlyphs`, `IgnoreLigatures`, or `UseMarkFilteringSet`.
    private final FlaggedPair[] flaggedPairs;

    /// Chains whose lookups use those same class-skip flags.
    private final FlaggedChain[] flaggedChains;

    /// Creates pair, skip, attach, and mark maps.
    ///
    /// @param keys the adjacent pair keys
    /// @param deltas the adjacent pair deltas
    /// @param singleGlyphs the single-positioning glyphs
    /// @param singleDeltas the single X-advance deltas
    /// @param chainPairs the adjacent chained current/next keys
    /// @param chainLooks the adjacent chained lookahead glyphs
    /// @param chainDeltas the adjacent chained X-advance deltas
    /// @param chainBacks the required preceding glyphs, or `0`
    /// @param chainFifths the fifth required preceding glyph, or `0`
    /// @param chainSevenths the seventh required preceding glyph, or `0`
    /// @param chainNinths the ninth required preceding glyph, or `0`
    /// @param markKeys the mark/base keys
    /// @param markXs the mark X offsets
    /// @param markYs the mark Y offsets
    /// @param markGlyphs the unique mark glyph ids
    /// @param skipKeys the `IgnoreMarks` pair keys
    /// @param skipDeltas the `IgnoreMarks` pair deltas
    /// @param skipChainPairs the `IgnoreMarks` chain keys
    /// @param skipChainLooks the `IgnoreMarks` chain lookaheads
    /// @param skipChainDeltas the `IgnoreMarks` chain deltas
    /// @param skipChainBacks the `IgnoreMarks` required preceding glyphs, or `0`
    /// @param attachKeys the attach-class pair keys
    /// @param attachDeltas the attach-class pair deltas
    /// @param attachTypes the attach-class pair lookup classes
    /// @param attachChainPairs the attach-class chain keys
    /// @param attachChainLooks the attach-class chain lookaheads
    /// @param attachChainDeltas the attach-class chain deltas
    /// @param attachChainTypes the attach-class chain lookup classes
    /// @param attachChainBacks the attach-class required preceding glyphs, or `0`
    /// @param attachmentTypes the unique attach classes
    /// @param gdef the GDEF classes
    /// @param flaggedPairs the class-skip pair rules
    /// @param flaggedChains the class-skip chain rules
    private GposPositioning(
            int[] keys,
            short[] deltas,
            int[] singleGlyphs,
            short[] singleDeltas,
            int[] chainPairs,
            int[] chainLooks,
            short[] chainDeltas,
            long[] chainBacks,
            int[] chainFifths,
            int[] chainSevenths,
            int[] chainNinths,
            int[] markKeys,
            short[] markXs,
            short[] markYs,
            int[] markGlyphs,
            int[] skipKeys,
            short[] skipDeltas,
            int[] skipChainPairs,
            int[] skipChainLooks,
            short[] skipChainDeltas,
            long[] skipChainBacks,
            int[] attachKeys,
            short[] attachDeltas,
            int[] attachTypes,
            int[] attachChainPairs,
            int[] attachChainLooks,
            short[] attachChainDeltas,
            int[] attachChainTypes,
            long[] attachChainBacks,
            int[] attachmentTypes,
            GdefTable gdef,
            FlaggedPair[] flaggedPairs,
            FlaggedChain[] flaggedChains
    ) {
        this.keys = keys;
        this.deltas = deltas;
        this.singleGlyphs = singleGlyphs;
        this.singleDeltas = singleDeltas;
        this.chainPairs = chainPairs;
        this.chainLooks = chainLooks;
        this.chainDeltas = chainDeltas;
        this.chainBacks = chainBacks;
        this.chainFifths = chainFifths;
        this.chainSevenths = chainSevenths;
        this.chainNinths = chainNinths;
        this.markKeys = markKeys;
        this.markXs = markXs;
        this.markYs = markYs;
        this.markGlyphs = markGlyphs;
        this.skipKeys = skipKeys;
        this.skipDeltas = skipDeltas;
        this.skipChainPairs = skipChainPairs;
        this.skipChainLooks = skipChainLooks;
        this.skipChainDeltas = skipChainDeltas;
        this.skipChainBacks = skipChainBacks;
        this.attachKeys = attachKeys;
        this.attachDeltas = attachDeltas;
        this.attachTypes = attachTypes;
        this.attachChainPairs = attachChainPairs;
        this.attachChainLooks = attachChainLooks;
        this.attachChainDeltas = attachChainDeltas;
        this.attachChainTypes = attachChainTypes;
        this.attachChainBacks = attachChainBacks;
        this.attachmentTypes = attachmentTypes;
        this.gdef = gdef;
        this.flaggedPairs = flaggedPairs;
        this.flaggedChains = flaggedChains;
    }

    /// Returns an empty table.
    private static GposPositioning empty() {
        int[] ints = new int[0];
        short[] shorts = new short[0];
        long[] longs = new long[0];
        return new GposPositioning(
                ints, shorts, ints, shorts, ints, ints, shorts, longs, ints, ints, ints, ints, shorts, shorts, ints,
                ints, shorts, ints, ints, shorts, longs, ints, shorts, ints, ints, ints, shorts, ints, longs, ints,
                GdefTable.NONE,
                new FlaggedPair[0],
                new FlaggedChain[0]
        );
    }

    /// Returns the X-advance delta for the pair `(left, right)`.
    ///
    /// @param left the first glyph
    /// @param right the second glyph
    /// @return the signed delta, or `0`
    int pairAdjustment(int left, int right) {
        return pairDelta(keys, deltas, left, right);
    }

    /// Returns the type-1 X-advance for `glyphId`.
    ///
    /// @param glyphId the glyph
    /// @return the signed delta, or `0`
    int singleAdjustment(int glyphId) {
        int index = Arrays.binarySearch(singleGlyphs, glyphId);
        return index >= 0 ? singleDeltas[index] : 0;
    }

    /// Returns the type-8 X-advance for `(current, next, lookahead)`.
    ///
    /// @param current the first input glyph
    /// @param next the second input glyph
    /// @param lookahead the first lookahead glyph
    /// @return the signed delta, or `0`
    int chainAdjustment(int current, int next, int lookahead) {
        return chainDelta(
                chainPairs, chainLooks, chainDeltas, chainBacks, chainFifths, chainSevenths, chainNinths,
                current, next, lookahead, EMPTY_BACKS
        );
    }

    /// Returns the `IgnoreMarks` pair X-advance for `(left, right)`.
    ///
    /// @param left the first non-skipped glyph
    /// @param right the next non-mark glyph
    /// @return the signed delta, or `0`
    int skipPairAdjustment(int left, int right) {
        return pairDelta(skipKeys, skipDeltas, left, right);
    }

    /// Returns the `IgnoreMarks` type-8 X-advance.
    ///
    /// @param current the first input glyph
    /// @param next the next non-mark glyph
    /// @param lookahead the following non-mark glyph
    /// @return the signed delta, or `0`
    int skipChainAdjustment(int current, int next, int lookahead) {
        return chainDelta(
                skipChainPairs,
                skipChainLooks,
                skipChainDeltas,
                skipChainBacks,
                new int[0],
                new int[0],
                new int[0],
                current,
                next,
                lookahead,
                EMPTY_BACKS
        );
    }

    /// Returns the `MarkAttachmentType` pair X-advance for class `attachType`.
    ///
    /// @param left the first glyph
    /// @param right the next glyph that is not a skipped mark
    /// @param attachType the lookup high-byte class
    /// @return the signed delta, or `0`
    int attachPairAdjustment(int left, int right, int attachType) {
        if (left < 0 || right < 0 || left > 0xFFFF || right > 0xFFFF) {
            return 0;
        }
        int key = (left << 16) | right;
        for (int index = 0; index < attachKeys.length; index++) {
            if (attachKeys[index] == key && attachTypes[index] == attachType) {
                return attachDeltas[index];
            }
        }
        return 0;
    }

    /// Returns the `MarkAttachmentType` type-8 X-advance for class `attachType`.
    ///
    /// @param current the first input glyph
    /// @param next the next non-skipped glyph
    /// @param lookahead the following non-skipped glyph
    /// @param attachType the lookup high-byte class
    /// @return the signed delta, or `0`
    int attachChainAdjustment(int current, int next, int lookahead, int attachType) {
        return attachChainAdjustment(current, next, lookahead, attachType, 0, 0, 0);
    }

    /// Returns the `MarkAttachmentType` type-8 X-advance, honoring required backtrack glyphs.
    ///
    /// @param current the first input glyph
    /// @param next the next non-skipped glyph
    /// @param lookahead the following non-skipped glyph
    /// @param attachType the lookup high-byte class
    /// @param backNear the nearest kept preceding glyph, or `0`
    /// @param backFar the next kept preceding glyph, or `0`
    /// @return the signed delta, or `0`
    int attachChainAdjustment(
            int current,
            int next,
            int lookahead,
            int attachType,
            int backNear,
            int backMid
    ) {
        return attachChainAdjustment(current, next, lookahead, attachType, backNear, backMid, 0);
    }

    /// Returns the `MarkAttachmentType` type-8 X-advance, honoring three backtrack glyphs.
    ///
    /// @param current the first input glyph
    /// @param next the next non-skipped glyph
    /// @param lookahead the following non-skipped glyph
    /// @param attachType the lookup high-byte class
    /// @param backNear the nearest kept preceding glyph, or `0`
    /// @param backMid the next kept preceding glyph, or `0`
    /// @param backFar the next kept preceding glyph, or `0`
    /// @return the signed delta, or `0`
    int attachChainAdjustment(
            int current,
            int next,
            int lookahead,
            int attachType,
            int backNear,
            int backMid,
            int backFar
    ) {
        return attachChainAdjustment(current, next, lookahead, attachType, backNear, backMid, backFar, 0);
    }

    /// Returns the `MarkAttachmentType` type-8 X-advance, honoring four backtrack glyphs.
    ///
    /// @param current the first input glyph
    /// @param next the next non-skipped glyph
    /// @param lookahead the following non-skipped glyph
    /// @param attachType the lookup high-byte class
    /// @param backNear the nearest kept preceding glyph, or `0`
    /// @param backMid the next kept preceding glyph, or `0`
    /// @param backFar the next kept preceding glyph, or `0`
    /// @param backFarther the farthest kept preceding glyph, or `0`
    /// @return the signed delta, or `0`
    int attachChainAdjustment(
            int current,
            int next,
            int lookahead,
            int attachType,
            int backNear,
            int backMid,
            int backFar,
            int backFarther
    ) {
        if (current < 0 || next < 0 || lookahead < 0 || current > 0xFFFF || next > 0xFFFF) {
            return 0;
        }
        int key = (current << 16) | next;
        for (int index = 0; index < attachChainPairs.length; index++) {
            if (attachChainPairs[index] == key
                    && attachChainLooks[index] == lookahead
                    && attachChainTypes[index] == attachType
                    && backMatches(attachChainBacks[index], 0, 0, 0, 0, 0, backNear, backMid, backFar, backFarther, 0, 0, 0, 0, 0)) {
                return attachChainDeltas[index];
            }
        }
        return 0;
    }

    /// Returns the unique `MarkAttachmentType` values present in this table.
    ///
    /// @return the classes, possibly empty
    int[] attachmentTypes() {
        return attachmentTypes;
    }

    /// Applies exact, skip, attach, and class-skip pair lookups at `start`.
    ///
    /// @param glyphIds the mapped glyphs
    /// @param start the first glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @return the summed X-advance delta
    int pairAdjustment(int[] glyphIds, int start, int remaining) {
        if (start < 0 || remaining < 2 || start + remaining > glyphIds.length) {
            return 0;
        }
        int left = glyphIds[start];
        int end = start + remaining;
        int delta = pairAdjustment(left, glyphIds[start + 1]);
        int skipped = gdef.firstKeptIndex(glyphIds, start + 1, end, GdefTable.FLAG_IGNORE_MARKS, 0);
        if (skipped >= 0) {
            delta += skipPairAdjustment(left, glyphIds[skipped]);
        }
        for (int attachType : attachmentTypes) {
            int attached = gdef.firstKeptIndex(glyphIds, start + 1, end, attachType << 8, 0);
            if (attached >= 0) {
                delta += attachPairAdjustment(left, glyphIds[attached], attachType);
            }
        }
        for (FlaggedPair rule : flaggedPairs) {
            if (rule.left != left) {
                continue;
            }
            int index = gdef.firstKeptIndex(glyphIds, start + 1, end, rule.flag, rule.markSet);
            if (index >= 0 && glyphIds[index] == rule.right) {
                delta += rule.delta;
            }
        }
        return delta;
    }

    /// Applies exact, skip, attach, and class-skip type-8 lookups at `start`.
    ///
    /// @param glyphIds the mapped glyphs
    /// @param start the first glyph index
    /// @param remaining the number of glyphs available from `start`
    /// @return the summed X-advance delta
    int chainAdjustment(int[] glyphIds, int start, int remaining) {
        if (start < 0 || remaining < 3 || start + remaining > glyphIds.length) {
            return 0;
        }
        int current = glyphIds[start];
        int end = start + remaining;
        int[] unflaggedBacks = keptBacks(glyphIds, start, 0, 0);
        int delta = chainDelta(
                chainPairs,
                chainLooks,
                chainDeltas,
                chainBacks,
                chainFifths,
                chainSevenths,
                chainNinths,
                current,
                glyphIds[start + 1],
                glyphIds[start + 2],
                unflaggedBacks
        );
        int skippedNext = gdef.firstKeptIndex(glyphIds, start + 1, end, GdefTable.FLAG_IGNORE_MARKS, 0);
        int skippedLook = skippedNext >= 0
                ? gdef.firstKeptIndex(glyphIds, skippedNext + 1, end, GdefTable.FLAG_IGNORE_MARKS, 0)
                : -1;
        if (skippedNext >= 0 && skippedLook >= 0) {
            delta += chainDelta(
                    skipChainPairs,
                    skipChainLooks,
                    skipChainDeltas,
                    skipChainBacks,
                    new int[0],
                    new int[0],
                    new int[0],
                    current,
                    glyphIds[skippedNext],
                    glyphIds[skippedLook],
                    keptBacks(glyphIds, start, GdefTable.FLAG_IGNORE_MARKS, 0)
            );
        }
        for (int attachType : attachmentTypes) {
            int flag = attachType << 8;
            int attachedNext = gdef.firstKeptIndex(glyphIds, start + 1, end, flag, 0);
            int attachedLook = attachedNext >= 0
                    ? gdef.firstKeptIndex(glyphIds, attachedNext + 1, end, flag, 0)
                    : -1;
            if (attachedNext >= 0 && attachedLook >= 0) {
                int[] attachedBacks = keptBacks(glyphIds, start, flag, 0);
                delta += attachChainAdjustment(
                        current,
                        glyphIds[attachedNext],
                        glyphIds[attachedLook],
                        attachType,
                        attachedBacks[0],
                        attachedBacks[1],
                        attachedBacks[2],
                        attachedBacks[3]
                );
            }
        }
        for (FlaggedChain rule : flaggedChains) {
            if (rule.current != current) {
                continue;
            }
            if (rule.back != 0) {
                int[] flaggedBacks = keptBacks(glyphIds, start, rule.flag, rule.markSet);
                if (flaggedBacks[0] != rule.back
                        || (rule.far != 0 && flaggedBacks[1] != rule.far)
                        || (rule.farther != 0 && flaggedBacks[2] != rule.farther)
                        || (rule.farthest != 0 && flaggedBacks[3] != rule.farthest)) {
                    continue;
                }
            }
            int nextIndex = gdef.firstKeptIndex(glyphIds, start + 1, end, rule.flag, rule.markSet);
            int lookIndex = nextIndex >= 0
                    ? gdef.firstKeptIndex(glyphIds, nextIndex + 1, end, rule.flag, rule.markSet)
                    : -1;
            if (nextIndex >= 0
                    && lookIndex >= 0
                    && glyphIds[nextIndex] == rule.next
                    && glyphIds[lookIndex] == rule.look) {
                delta += rule.delta;
            }
        }
        return delta;
    }

    /// Binary-searches a sorted pair map.
    private static int pairDelta(int[] keys, short[] deltas, int left, int right) {
        if (left < 0 || right < 0 || left > 0xFFFF || right > 0xFFFF) {
            return 0;
        }
        int key = (left << 16) | right;
        int index = Arrays.binarySearch(keys, key);
        return index >= 0 ? deltas[index] : 0;
    }

    /// Linear-searches a chain map.
    ///
    /// Packed backtrack words store nearest through fourth preceding glyphs in 16-bit
    /// lanes. A zero lane is unused.
    private static int chainDelta(
            int[] pairs,
            int[] looks,
            short[] deltas,
            long[] backs,
            int[] fifths,
            int[] sevenths,
            int[] ninths,
            int current,
            int next,
            int lookahead,
            int[] walked
    ) {
        if (current < 0 || next < 0 || lookahead < 0 || current > 0xFFFF || next > 0xFFFF) {
            return 0;
        }
        int key = (current << 16) | next;
        int near = walked.length > 0 ? walked[0] : 0;
        int mid = walked.length > 1 ? walked[1] : 0;
        int far = walked.length > 2 ? walked[2] : 0;
        int farther = walked.length > 3 ? walked[3] : 0;
        int fifth = walked.length > 4 ? walked[4] : 0;
        int sixth = walked.length > 5 ? walked[5] : 0;
        int seventh = walked.length > 6 ? walked[6] : 0;
        int eighth = walked.length > 7 ? walked[7] : 0;
        int ninth = walked.length > 8 ? walked[8] : 0;
        for (int index = 0; index < pairs.length; index++) {
            int packedTail = index < fifths.length ? fifths[index] : 0;
            int requiredFifth = packedTail & 0xFFFF;
            int requiredSixth = (packedTail >>> 16) & 0xFFFF;
            int packedFar = index < sevenths.length ? sevenths[index] : 0;
            int requiredSeventh = packedFar & 0xFFFF;
            int requiredEighth = (packedFar >>> 16) & 0xFFFF;
            int requiredNinth = index < ninths.length ? ninths[index] : 0;
            if (pairs[index] == key
                    && looks[index] == lookahead
                    && backMatches(
                            index < backs.length ? backs[index] : 0L,
                            requiredFifth,
                            requiredSixth,
                            requiredSeventh,
                            requiredEighth,
                            requiredNinth,
                            near,
                            mid,
                            far,
                            farther,
                            fifth,
                            sixth,
                            seventh,
                            eighth,
                            ninth)) {
                return deltas[index];
            }
        }
        return 0;
    }

    /// Packs up to four backtrack glyphs. Index 0 is nearest.
    private static long packBack(int near, int mid, int far, int farther) {
        return (near & 0xFFFFL)
                | ((mid & 0xFFFFL) << 16)
                | ((far & 0xFFFFL) << 32)
                | ((farther & 0xFFFFL) << 48);
    }

    /// Reads up to `max` backtrack glyph or class ids. Index 0 is nearest.
    ///
    /// @return `{near, mid, far, farther}`, or `null` when the count exceeds `max` or the table is truncated
    private static int @Nullable [] readBacktrackIds(ByteBuffer buffer, int max) {
        if (buffer.remaining() < 2) {
            return null;
        }
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count > max) {
            return null;
        }
        int[] ids = new int[9];
        for (int index = 0; index < count; index++) {
            if (buffer.remaining() < 2) {
                return null;
            }
            ids[index] = Short.toUnsignedInt(buffer.getShort());
        }
        return ids;
    }

    /// Returns whether `packed` is unused or matches the walked preceding glyphs.
    private static boolean backMatches(
            long packed,
            int requiredFifth,
            int requiredSixth,
            int requiredSeventh,
            int requiredEighth,
            int requiredNinth,
            int backNear,
            int backMid,
            int backFar,
            int backFarther,
            int backFifth,
            int backSixth,
            int backSeventh,
            int backEighth,
            int backNinth
    ) {
        int requiredNear = (int) (packed & 0xFFFFL);
        int requiredMid = (int) ((packed >>> 16) & 0xFFFFL);
        int requiredFar = (int) ((packed >>> 32) & 0xFFFFL);
        int requiredFarther = (int) ((packed >>> 48) & 0xFFFFL);
        if (requiredNear != 0 && requiredNear != backNear) {
            return false;
        }
        if (requiredMid != 0 && requiredMid != backMid) {
            return false;
        }
        if (requiredFar != 0 && requiredFar != backFar) {
            return false;
        }
        if (requiredFarther != 0 && requiredFarther != backFarther) {
            return false;
        }
        if (requiredFifth != 0 && requiredFifth != backFifth) {
            return false;
        }
        if (requiredSixth != 0 && requiredSixth != backSixth) {
            return false;
        }
        if (requiredSeventh != 0 && requiredSeventh != backSeventh) {
            return false;
        }
        if (requiredEighth != 0 && requiredEighth != backEighth) {
            return false;
        }
        return requiredNinth == 0 || requiredNinth == backNinth;
    }

    /// Walks at most nine kept preceding glyphs under `lookupFlag`.
    ///
    /// @return `{near, mid, far, farther, fifth, sixth, seventh, eighth, ninth}`, using `0` when a slot is missing
    private int[] keptBacks(int[] glyphIds, int start, int lookupFlag, int markSet) {
        int[] ids = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int cursor = start - 1;
        for (int slot = 0; slot < 9; slot++) {
            int index = gdef.prevKeptIndex(glyphIds, cursor, lookupFlag, markSet);
            if (index < 0) {
                break;
            }
            ids[slot] = glyphIds[index];
            cursor = index - 1;
        }
        return ids;
    }

    /// Returns whether `glyphId` appears in a mark coverage table.
    ///
    /// @param glyphId the glyph
    /// @return whether the glyph is a GPOS mark
    boolean isMark(int glyphId) {
        return Arrays.binarySearch(markGlyphs, glyphId) >= 0;
    }

    /// Returns the mark-to-base placement, or `null` when the pair is uncovered.
    ///
    /// @param markGlyph the mark glyph
    /// @param baseGlyph the base glyph
    /// @return the placement
    @Nullable MarkPlacement markPlacement(int markGlyph, int baseGlyph) {
        if (markGlyph < 0 || baseGlyph < 0 || markGlyph > 0xFFFF || baseGlyph > 0xFFFF) {
            return null;
        }
        int key = (markGlyph << 16) | baseGlyph;
        int index = Arrays.binarySearch(markKeys, key);
        if (index < 0) {
            return null;
        }
        return new MarkPlacement(markXs[index], markYs[index]);
    }

    /// Parses GPOS type-2 pairs, then overlays format-0 `kern` pairs that GPOS did not name.
    ///
    /// @param gpos the GPOS table, or `null`
    /// @param kern the `kern` table, or `null`
    /// @return the positioning
    static GposPositioning parse(@Nullable ByteBuffer gpos, @Nullable ByteBuffer kern) {
        return parse(gpos, kern, GdefTable.NONE);
    }

    /// Parses GPOS against `gdef` so class-skip flags can match later.
    ///
    /// @param gpos the GPOS table, or `null`
    /// @param kern the `kern` table, or `null`
    /// @param gdef the GDEF classes
    /// @return the positioning
    static GposPositioning parse(@Nullable ByteBuffer gpos, @Nullable ByteBuffer kern, GdefTable gdef) {
        PairSink pairs = new PairSink();
        PairSink skipPairs = new PairSink();
        AttachPairSink attachPairs = new AttachPairSink();
        FlaggedPairSink flaggedPairs = new FlaggedPairSink();
        MarkSink marks = new MarkSink();
        SingleSink singles = new SingleSink();
        ChainSink chains = new ChainSink();
        ChainSink skipChains = new ChainSink();
        AttachChainSink attachChains = new AttachChainSink();
        FlaggedChainSink flaggedChains = new FlaggedChainSink();
        if (gpos != null && gpos.remaining() >= 10) {
            readGpos(
                    gpos.duplicate().order(ByteOrder.BIG_ENDIAN),
                    pairs,
                    skipPairs,
                    attachPairs,
                    flaggedPairs,
                    marks,
                    singles,
                    chains,
                    skipChains,
                    attachChains,
                    flaggedChains
            );
        }
        if (kern != null && kern.remaining() >= 4) {
            readKern(kern.duplicate().order(ByteOrder.BIG_ENDIAN), pairs);
        }
        return finish(
                pairs,
                skipPairs,
                attachPairs,
                flaggedPairs,
                marks,
                singles,
                chains,
                skipChains,
                attachChains,
                flaggedChains,
                gdef
        );
    }

    /// Reads GPOS lookups for pairs, marks, singles, and first-stable contextual rules.
    private static void readGpos(
            ByteBuffer buffer,
            PairSink pairs,
            PairSink skipPairs,
            AttachPairSink attachPairs,
            FlaggedPairSink flaggedPairs,
            MarkSink marks,
            SingleSink singles,
            ChainSink chains,
            ChainSink skipChains,
            AttachChainSink attachChains,
            FlaggedChainSink flaggedChains
    ) {
        int start = buffer.position();
        int major = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        if (major != 1) {
            return;
        }
        buffer.getShort();
        int featureList = start + Short.toUnsignedInt(buffer.getShort());
        int lookupList = start + Short.toUnsignedInt(buffer.getShort());
        int[] lookupOffsets = readLookupOffsets(buffer, lookupList);
        boolean[] kernSelected = selectFeatureLookups(buffer, featureList, lookupOffsets.length, 0x6B65726E);
        boolean[] cursSelected = selectFeatureLookups(buffer, featureList, lookupOffsets.length, 0x63757273);
        boolean[] markSelected = selectFeatureLookups(buffer, featureList, lookupOffsets.length, 0x6D61726B);
        boolean[] mkmkSelected = selectFeatureLookups(buffer, featureList, lookupOffsets.length, 0x6D6B6D6B);
        for (int index = 0; index < lookupOffsets.length; index++) {
            int type = peekLookupType(buffer, lookupOffsets[index]);
            if ((type == 1 || type == 2 || type == 7 || type == 8)
                    && kernSelected != null && !kernSelected[index]) {
                continue;
            }
            if (type == 3 && (cursSelected == null || !cursSelected[index])) {
                continue;
            }
            if ((type == 4 || type == 5) && markSelected != null && !markSelected[index]) {
                continue;
            }
            if (type == 6 && (mkmkSelected == null || !mkmkSelected[index])) {
                continue;
            }
            readLookup(
                    buffer,
                    lookupOffsets[index],
                    lookupOffsets,
                    pairs,
                    skipPairs,
                    attachPairs,
                    flaggedPairs,
                    marks,
                    singles,
                    chains,
                    skipChains,
                    attachChains,
                    flaggedChains
            );
        }
    }

    /// Peeks a lookup type without leaving the buffer at that lookup.
    private static int peekLookupType(ByteBuffer buffer, int offset) {
        if (offset + 2 > buffer.limit()) {
            return 0;
        }
        int saved = buffer.position();
        buffer.position(offset);
        int type = Short.toUnsignedInt(buffer.getShort());
        buffer.position(saved);
        return type;
    }

    /// Marks lookups listed by `featureTag`, or `null` when that feature is absent.
    private static boolean @Nullable [] selectFeatureLookups(
            ByteBuffer buffer,
            int featureList,
            int lookupCount,
            int featureTag
    ) {
        if (featureList + 2 > buffer.limit()) {
            return null;
        }
        int saved = buffer.position();
        buffer.position(featureList);
        int count = Short.toUnsignedInt(buffer.getShort());
        boolean[] selected = new boolean[lookupCount];
        boolean found = false;
        for (int index = 0; index < count; index++) {
            if (buffer.remaining() < 6) {
                break;
            }
            int tag = buffer.getInt();
            int offset = featureList + Short.toUnsignedInt(buffer.getShort());
            if (tag != featureTag) {
                continue;
            }
            found = true;
            markFeatureLookups(buffer, offset, selected);
        }
        buffer.position(saved);
        return found ? selected : null;
    }

    /// Marks lookup indices from one feature table.
    private static void markFeatureLookups(ByteBuffer buffer, int offset, boolean[] selected) {
        if (offset + 4 > buffer.limit()) {
            return;
        }
        int saved = buffer.position();
        buffer.position(offset);
        buffer.getShort();
        int count = Short.toUnsignedInt(buffer.getShort());
        for (int index = 0; index < count && buffer.remaining() >= 2; index++) {
            int lookupIndex = Short.toUnsignedInt(buffer.getShort());
            if (lookupIndex < selected.length) {
                selected[lookupIndex] = true;
            }
        }
        buffer.position(saved);
    }

    /// Reads lookup-list offsets.
    private static int[] readLookupOffsets(ByteBuffer buffer, int lookupList) {
        if (lookupList + 2 > buffer.limit()) {
            return new int[0];
        }
        buffer.position(lookupList);
        int count = Short.toUnsignedInt(buffer.getShort());
        int[] offsets = new int[count];
        for (int index = 0; index < count; index++) {
            offsets[index] = lookupList + Short.toUnsignedInt(buffer.getShort());
        }
        return offsets;
    }

    /// Reads one lookup. Types other than 1 through 8 are ignored.
    ///
    /// `IgnoreMarks` (`0x0008`) wins over `MarkAttachmentType` when both bits are set, matching
    /// the OpenType rule that class-3 marks are then skipped unconditionally.
    private static void readLookup(
            ByteBuffer buffer,
            int offset,
            int[] lookupOffsets,
            PairSink pairs,
            PairSink skipPairs,
            AttachPairSink attachPairs,
            FlaggedPairSink flaggedPairs,
            MarkSink marks,
            SingleSink singles,
            ChainSink chains,
            ChainSink skipChains,
            AttachChainSink attachChains,
            FlaggedChainSink flaggedChains
    ) {
        if (offset + 6 > buffer.limit()) {
            return;
        }
        buffer.position(offset);
        int type = Short.toUnsignedInt(buffer.getShort());
        int flag = Short.toUnsignedInt(buffer.getShort());
        int subtableCount = Short.toUnsignedInt(buffer.getShort());
        if (type < 1 || type > 8) {
            return;
        }
        int[] subtables = new int[subtableCount];
        for (int index = 0; index < subtableCount; index++) {
            subtables[index] = offset + Short.toUnsignedInt(buffer.getShort());
        }
        int markSet = 0;
        if ((flag & GdefTable.FLAG_MARK_FILTER) != 0 && buffer.remaining() >= 2) {
            markSet = Short.toUnsignedInt(buffer.getShort());
        }
        boolean ignoreMarks = (flag & GdefTable.FLAG_IGNORE_MARKS) != 0;
        int attachType = (flag >>> 8) & 0xFF;
        boolean classSkip = (flag & (GdefTable.FLAG_IGNORE_BASE
                | GdefTable.FLAG_IGNORE_LIGATURE
                | GdefTable.FLAG_MARK_FILTER)) != 0;
        for (int subtable : subtables) {
            if (type == 1) {
                readSinglePos(buffer, subtable, singles);
            } else if (type == 2) {
                if (classSkip) {
                    PairSink isolated = new PairSink();
                    readPairPos(buffer, subtable, isolated);
                    flaggedPairs.addFrom(isolated, flag, markSet);
                } else {
                    routePairPos(buffer, subtable, ignoreMarks, attachType, pairs, skipPairs, attachPairs);
                }
            } else if (type == 3) {
                readCursive(buffer, subtable, pairs);
            } else if (type == 5) {
                readMarkLig(buffer, subtable, marks);
            } else if (type == 7) {
                if (classSkip) {
                    PairSink isolated = new PairSink();
                    readContextPos(buffer, subtable, lookupOffsets, isolated);
                    flaggedPairs.addFrom(isolated, flag, markSet);
                } else {
                    routeContextPos(
                            buffer,
                            subtable,
                            lookupOffsets,
                            ignoreMarks,
                            attachType,
                            pairs,
                            skipPairs,
                            attachPairs
                    );
                }
            } else if (type == 8) {
                if (classSkip) {
                    ChainSink isolated = new ChainSink();
                    readChainPos(buffer, subtable, lookupOffsets, isolated);
                    flaggedChains.addFrom(isolated, flag, markSet);
                } else {
                    routeChainPos(
                            buffer,
                            subtable,
                            lookupOffsets,
                            ignoreMarks,
                            attachType,
                            chains,
                            skipChains,
                            attachChains
                    );
                }
            } else {
                readMarkBase(buffer, subtable, marks);
            }
        }
    }

    /// Reads a type-2 pair subtable into the map selected by the lookup flag.
    private static void routePairPos(
            ByteBuffer buffer,
            int subtable,
            boolean ignoreMarks,
            int attachType,
            PairSink pairs,
            PairSink skipPairs,
            AttachPairSink attachPairs
    ) {
        if (ignoreMarks) {
            readPairPos(buffer, subtable, skipPairs);
            return;
        }
        if (attachType != 0) {
            PairSink isolated = new PairSink();
            readPairPos(buffer, subtable, isolated);
            attachPairs.addFrom(isolated, attachType);
            return;
        }
        readPairPos(buffer, subtable, pairs);
    }

    /// Reads a type-7 context subtable into the map selected by the lookup flag.
    private static void routeContextPos(
            ByteBuffer buffer,
            int subtable,
            int[] lookupOffsets,
            boolean ignoreMarks,
            int attachType,
            PairSink pairs,
            PairSink skipPairs,
            AttachPairSink attachPairs
    ) {
        if (ignoreMarks) {
            readContextPos(buffer, subtable, lookupOffsets, skipPairs);
            return;
        }
        if (attachType != 0) {
            PairSink isolated = new PairSink();
            readContextPos(buffer, subtable, lookupOffsets, isolated);
            attachPairs.addFrom(isolated, attachType);
            return;
        }
        readContextPos(buffer, subtable, lookupOffsets, pairs);
    }

    /// Reads a type-8 chain subtable into the map selected by the lookup flag.
    private static void routeChainPos(
            ByteBuffer buffer,
            int subtable,
            int[] lookupOffsets,
            boolean ignoreMarks,
            int attachType,
            ChainSink chains,
            ChainSink skipChains,
            AttachChainSink attachChains
    ) {
        if (ignoreMarks) {
            readChainPos(buffer, subtable, lookupOffsets, skipChains);
            return;
        }
        if (attachType != 0) {
            ChainSink isolated = new ChainSink();
            readChainPos(buffer, subtable, lookupOffsets, isolated);
            attachChains.addFrom(isolated, attachType);
            return;
        }
        readChainPos(buffer, subtable, lookupOffsets, chains);
    }

    /// Reads a type-1 SinglePos format-1 X-advance.
    private static void readSinglePos(ByteBuffer buffer, int offset, SingleSink singles) {
        if (offset + 6 > buffer.limit()) {
            return;
        }
        buffer.position(offset);
        if (Short.toUnsignedInt(buffer.getShort()) != 1) {
            return;
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int valueFormat = Short.toUnsignedInt(buffer.getShort());
        int delta = readXAdvance(buffer, valueFormat);
        for (int glyph : readCoverageGlyphs(buffer, coverageOffset)) {
            singles.put(glyph, (short) delta);
        }
    }

    /// Reads a type-7 ContextPos format-1 or format-3 two-glyph rule into the pair map.
    private static void readContextPos(ByteBuffer buffer, int offset, int[] lookupOffsets, PairSink pairs) {
        if (offset + 6 > buffer.limit()) {
            return;
        }
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        if (format == 3) {
            readContextPosFormat3(buffer, offset, lookupOffsets, pairs);
            return;
        }
        if (format == 2) {
            readContextPosFormat2(buffer, offset, lookupOffsets, pairs);
            return;
        }
        if (format != 1) {
            return;
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int setCount = Short.toUnsignedInt(buffer.getShort());
        int[] glyphs = readCoverageGlyphs(buffer, coverageOffset);
        int limit = Math.min(setCount, glyphs.length);
        int[] setOffsets = new int[limit];
        for (int index = 0; index < limit; index++) {
            if (buffer.remaining() < 2) {
                return;
            }
            setOffsets[index] = offset + Short.toUnsignedInt(buffer.getShort());
        }
        for (int index = 0; index < limit; index++) {
            applyContextRules(buffer, setOffsets[index], glyphs[index], lookupOffsets, pairs);
        }
    }

    /// Applies two-glyph PosRules from one PosRuleSet.
    private static void applyContextRules(
            ByteBuffer buffer,
            int setOffset,
            int first,
            int[] lookupOffsets,
            PairSink pairs
    ) {
        if (setOffset + 2 > buffer.limit()) {
            return;
        }
        int saved = buffer.position();
        buffer.position(setOffset);
        int ruleCount = Short.toUnsignedInt(buffer.getShort());
        int[] rules = new int[ruleCount];
        for (int index = 0; index < ruleCount && buffer.remaining() >= 2; index++) {
            rules[index] = setOffset + Short.toUnsignedInt(buffer.getShort());
        }
        buffer.position(saved);
        for (int rule : rules) {
            if (rule + 6 > buffer.limit()) {
                continue;
            }
            saved = buffer.position();
            buffer.position(rule);
            int glyphCount = Short.toUnsignedInt(buffer.getShort());
            int lookupCount = Short.toUnsignedInt(buffer.getShort());
            if (glyphCount != 2 || lookupCount < 1 || buffer.remaining() < 2) {
                buffer.position(saved);
                continue;
            }
            int second = Short.toUnsignedInt(buffer.getShort());
            if (buffer.remaining() < 4) {
                buffer.position(saved);
                continue;
            }
            int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
            int lookupIndex = Short.toUnsignedInt(buffer.getShort());
            buffer.position(saved);
            if (sequenceIndex != 0 || lookupIndex >= lookupOffsets.length) {
                continue;
            }
            int delta = lookupXAdvance(buffer, lookupOffsets[lookupIndex]);
            if (delta != 0) {
                pairs.putIfAbsent(first, second, (short) delta);
            }
        }
    }

    /// Reads ContextPos format 2: a ClassDef plus two-class PosClassRules.
    private static void readContextPosFormat2(
            ByteBuffer buffer,
            int offset,
            int[] lookupOffsets,
            PairSink pairs
    ) {
        if (buffer.remaining() < 6) {
            return;
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int classDefOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int setCount = Short.toUnsignedInt(buffer.getShort());
        int[] setOffsets = new int[setCount];
        for (int index = 0; index < setCount; index++) {
            if (buffer.remaining() < 2) {
                return;
            }
            int relative = Short.toUnsignedInt(buffer.getShort());
            setOffsets[index] = relative == 0 ? 0 : offset + relative;
        }
        int[] firsts = readCoverageGlyphs(buffer, coverageOffset);
        ClassMap classes = ClassMap.read(buffer, classDefOffset);
        for (int first : firsts) {
            int firstClass = classes.classOf(first);
            if (firstClass < 0 || firstClass >= setCount || setOffsets[firstClass] == 0) {
                continue;
            }
            applyClassContextRules(buffer, setOffsets[firstClass], first, classes, lookupOffsets, pairs);
        }
    }

    /// Applies two-class PosClassRules from one PosClassSet.
    private static void applyClassContextRules(
            ByteBuffer buffer,
            int setOffset,
            int first,
            ClassMap classes,
            int[] lookupOffsets,
            PairSink pairs
    ) {
        if (setOffset + 2 > buffer.limit()) {
            return;
        }
        int saved = buffer.position();
        buffer.position(setOffset);
        int ruleCount = Short.toUnsignedInt(buffer.getShort());
        int[] rules = new int[ruleCount];
        for (int index = 0; index < ruleCount && buffer.remaining() >= 2; index++) {
            rules[index] = setOffset + Short.toUnsignedInt(buffer.getShort());
        }
        buffer.position(saved);
        for (int rule : rules) {
            if (rule + 8 > buffer.limit()) {
                continue;
            }
            saved = buffer.position();
            buffer.position(rule);
            int glyphCount = Short.toUnsignedInt(buffer.getShort());
            int lookupCount = Short.toUnsignedInt(buffer.getShort());
            if (glyphCount != 2 || lookupCount < 1 || buffer.remaining() < 6) {
                buffer.position(saved);
                continue;
            }
            int secondClass = Short.toUnsignedInt(buffer.getShort());
            int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
            int lookupIndex = Short.toUnsignedInt(buffer.getShort());
            buffer.position(saved);
            if (sequenceIndex != 0 || lookupIndex >= lookupOffsets.length) {
                continue;
            }
            int delta = lookupXAdvance(buffer, lookupOffsets[lookupIndex]);
            if (delta == 0) {
                continue;
            }
            for (int second : classes.glyphsOf(secondClass)) {
                pairs.putIfAbsent(first, second, (short) delta);
            }
        }
    }

    /// Reads ContextPos format 3: two coverage tables and a nested type-1 X-advance.
    private static void readContextPosFormat3(
            ByteBuffer buffer,
            int offset,
            int[] lookupOffsets,
            PairSink pairs
    ) {
        if (buffer.remaining() < 4) {
            return;
        }
        int glyphCount = Short.toUnsignedInt(buffer.getShort());
        int lookupCount = Short.toUnsignedInt(buffer.getShort());
        if (glyphCount != 2 || lookupCount < 1 || buffer.remaining() < 4 + lookupCount * 4) {
            return;
        }
        int firstCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        int secondCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
        int lookupIndex = Short.toUnsignedInt(buffer.getShort());
        if (sequenceIndex != 0 || lookupIndex >= lookupOffsets.length) {
            return;
        }
        int delta = lookupXAdvance(buffer, lookupOffsets[lookupIndex]);
        if (delta == 0) {
            return;
        }
        for (int first : readCoverageGlyphs(buffer, firstCoverage)) {
            for (int second : readCoverageGlyphs(buffer, secondCoverage)) {
                pairs.putIfAbsent(first, second, (short) delta);
            }
        }
    }

    /// Reads a type-8 ChainContextPos format-1 or format-3 rule with one lookahead glyph.
    private static void readChainPos(ByteBuffer buffer, int offset, int[] lookupOffsets, ChainSink chains) {
        if (offset + 6 > buffer.limit()) {
            return;
        }
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        if (format == 3) {
            readChainPosFormat3(buffer, offset, lookupOffsets, chains);
            return;
        }
        if (format == 2) {
            readChainPosFormat2(buffer, offset, lookupOffsets, chains);
            return;
        }
        if (format != 1) {
            return;
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int setCount = Short.toUnsignedInt(buffer.getShort());
        int[] glyphs = readCoverageGlyphs(buffer, coverageOffset);
        int limit = Math.min(setCount, glyphs.length);
        int[] setOffsets = new int[limit];
        for (int index = 0; index < limit; index++) {
            if (buffer.remaining() < 2) {
                return;
            }
            setOffsets[index] = offset + Short.toUnsignedInt(buffer.getShort());
        }
        for (int index = 0; index < limit; index++) {
            applyChainRules(buffer, setOffsets[index], glyphs[index], lookupOffsets, chains);
        }
    }

    /// Applies first-stable chain rules from one ChainPosRuleSet.
    private static void applyChainRules(
            ByteBuffer buffer,
            int setOffset,
            int first,
            int[] lookupOffsets,
            ChainSink chains
    ) {
        if (setOffset + 2 > buffer.limit()) {
            return;
        }
        int saved = buffer.position();
        buffer.position(setOffset);
        int ruleCount = Short.toUnsignedInt(buffer.getShort());
        int[] rules = new int[ruleCount];
        for (int index = 0; index < ruleCount && buffer.remaining() >= 2; index++) {
            rules[index] = setOffset + Short.toUnsignedInt(buffer.getShort());
        }
        buffer.position(saved);
        for (int rule : rules) {
            if (rule + 10 > buffer.limit()) {
                continue;
            }
            saved = buffer.position();
            buffer.position(rule);
            int @Nullable [] backs = readBacktrackIds(buffer, 9);
            if (backs == null) {
                buffer.position(saved);
                continue;
            }
            int inputCount = Short.toUnsignedInt(buffer.getShort());
            if (inputCount != 2 || buffer.remaining() < 2) {
                buffer.position(saved);
                continue;
            }
            int second = Short.toUnsignedInt(buffer.getShort());
            int lookaheadCount = Short.toUnsignedInt(buffer.getShort());
            if (lookaheadCount != 1 || buffer.remaining() < 2) {
                buffer.position(saved);
                continue;
            }
            int lookahead = Short.toUnsignedInt(buffer.getShort());
            int lookupCount = Short.toUnsignedInt(buffer.getShort());
            if (lookupCount < 1 || buffer.remaining() < 4) {
                buffer.position(saved);
                continue;
            }
            int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
            int lookupIndex = Short.toUnsignedInt(buffer.getShort());
            buffer.position(saved);
            if (sequenceIndex != 0 || lookupIndex >= lookupOffsets.length) {
                continue;
            }
            int delta = lookupXAdvance(buffer, lookupOffsets[lookupIndex]);
            if (delta != 0) {
                chains.put(first, second, lookahead, (short) delta, backs[0], backs[1], backs[2], backs[3], backs[4], backs[5], backs[6], backs[7], backs[8]);
            }
        }
    }

    /// Reads ChainContextPos format 2: class-based input and one lookahead class.
    private static void readChainPosFormat2(
            ByteBuffer buffer,
            int offset,
            int[] lookupOffsets,
            ChainSink chains
    ) {
        if (buffer.remaining() < 10) {
            return;
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
                return;
            }
            int relative = Short.toUnsignedInt(buffer.getShort());
            setOffsets[index] = relative == 0 ? 0 : offset + relative;
        }
        int[] firsts = readCoverageGlyphs(buffer, coverageOffset);
        ClassMap backClasses = backClassOffset == 0 ? ClassMap.EMPTY : ClassMap.read(buffer, backClassOffset);
        ClassMap inputClasses = ClassMap.read(buffer, inputClassOffset);
        ClassMap lookClasses = ClassMap.read(buffer, lookClassOffset);
        for (int first : firsts) {
            int firstClass = inputClasses.classOf(first);
            if (firstClass < 0 || firstClass >= setCount || setOffsets[firstClass] == 0) {
                continue;
            }
            applyClassChainRules(
                    buffer,
                    setOffsets[firstClass],
                    first,
                    backClasses,
                    inputClasses,
                    lookClasses,
                    lookupOffsets,
                    chains
            );
        }
    }

    /// Applies first-stable ChainClassRules from one ChainClassSet.
    private static void applyClassChainRules(
            ByteBuffer buffer,
            int setOffset,
            int first,
            ClassMap backClasses,
            ClassMap inputClasses,
            ClassMap lookClasses,
            int[] lookupOffsets,
            ChainSink chains
    ) {
        if (setOffset + 2 > buffer.limit()) {
            return;
        }
        int saved = buffer.position();
        buffer.position(setOffset);
        int ruleCount = Short.toUnsignedInt(buffer.getShort());
        int[] rules = new int[ruleCount];
        for (int index = 0; index < ruleCount && buffer.remaining() >= 2; index++) {
            rules[index] = setOffset + Short.toUnsignedInt(buffer.getShort());
        }
        buffer.position(saved);
        for (int rule : rules) {
            if (rule + 12 > buffer.limit()) {
                continue;
            }
            saved = buffer.position();
            buffer.position(rule);
            int @Nullable [] backClassesIds = readBacktrackIds(buffer, 9);
            if (backClassesIds == null) {
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
            int lookupCount = Short.toUnsignedInt(buffer.getShort());
            if (lookupCount < 1 || buffer.remaining() < 4) {
                buffer.position(saved);
                continue;
            }
            int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
            int lookupIndex = Short.toUnsignedInt(buffer.getShort());
            buffer.position(saved);
            if (sequenceIndex != 0 || lookupIndex >= lookupOffsets.length) {
                continue;
            }
            int delta = lookupXAdvance(buffer, lookupOffsets[lookupIndex]);
            if (delta == 0) {
                continue;
            }
            int[] nearGlyphs = backClassesIds[0] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassesIds[0]);
            int[] midGlyphs = backClassesIds[1] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassesIds[1]);
            int[] farGlyphs = backClassesIds[2] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassesIds[2]);
            int[] fartherGlyphs = backClassesIds[3] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassesIds[3]);
            int[] fifthGlyphs = backClassesIds[4] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassesIds[4]);
            int[] sixthGlyphs = backClassesIds[5] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassesIds[5]);
            int[] seventhGlyphs = backClassesIds[6] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassesIds[6]);
            int[] eighthGlyphs = backClassesIds[7] == 0 ? new int[] {0} : backClasses.glyphsOf(backClassesIds[7]);
            for (int eighth : eighthGlyphs) {
            for (int seventh : seventhGlyphs) {
                for (int sixth : sixthGlyphs) {
                    for (int fifth : fifthGlyphs) {
                        for (int farther : fartherGlyphs) {
                            for (int far : farGlyphs) {
                                for (int mid : midGlyphs) {
                                    for (int near : nearGlyphs) {
                                        for (int second : inputClasses.glyphsOf(secondClass)) {
                                            for (int look : lookClasses.glyphsOf(lookClass)) {
                                                chains.put(
                                                        first,
                                                        second,
                                                        look,
                                                        (short) delta,
                                                        near,
                                                        mid,
                                                        far,
                                                        farther,
                                                        fifth,
                                                        sixth,
                                                        seventh,
                                                        eighth,
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
    }

    /// Reads ChainContextPos format 3 with two input coverages and one lookahead coverage.
    private static void readChainPosFormat3(
            ByteBuffer buffer,
            int offset,
            int[] lookupOffsets,
            ChainSink chains
    ) {
        if (buffer.remaining() < 2) {
            return;
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
        if (backtrackCount > 9) {
            return;
        }
        if (backtrackCount >= 1) {
            if (buffer.remaining() < 2) {
                return;
            }
            nearCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 2) {
            if (buffer.remaining() < 2) {
                return;
            }
            midCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 3) {
            if (buffer.remaining() < 2) {
                return;
            }
            farCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 4) {
            if (buffer.remaining() < 2) {
                return;
            }
            fartherCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 5) {
            if (buffer.remaining() < 2) {
                return;
            }
            fifthCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 6) {
            if (buffer.remaining() < 2) {
                return;
            }
            sixthCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 7) {
            if (buffer.remaining() < 2) {
                return;
            }
            seventhCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount >= 8) {
            if (buffer.remaining() < 2) {
                return;
            }
            eighthCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if (backtrackCount == 9) {
            if (buffer.remaining() < 2) {
                return;
            }
            buffer.getShort();
        }
        if (buffer.remaining() < 2) {
            return;
        }
        int inputCount = Short.toUnsignedInt(buffer.getShort());
        if (inputCount != 2 || buffer.remaining() < 4) {
            return;
        }
        int firstCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        int secondCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        if (buffer.remaining() < 2) {
            return;
        }
        int lookaheadCount = Short.toUnsignedInt(buffer.getShort());
        if (lookaheadCount != 1 || buffer.remaining() < 2) {
            return;
        }
        int lookaheadCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        if (buffer.remaining() < 2) {
            return;
        }
        int lookupCount = Short.toUnsignedInt(buffer.getShort());
        if (lookupCount < 1 || buffer.remaining() < 4) {
            return;
        }
        int sequenceIndex = Short.toUnsignedInt(buffer.getShort());
        int lookupIndex = Short.toUnsignedInt(buffer.getShort());
        if (sequenceIndex != 0 || lookupIndex >= lookupOffsets.length) {
            return;
        }
        int delta = lookupXAdvance(buffer, lookupOffsets[lookupIndex]);
        if (delta == 0) {
            return;
        }
        int[] firsts = readCoverageGlyphs(buffer, firstCoverage);
        int[] seconds = readCoverageGlyphs(buffer, secondCoverage);
        int[] looks = readCoverageGlyphs(buffer, lookaheadCoverage);
        int[] nearGlyphs = nearCoverage == 0 ? new int[] {0} : readCoverageGlyphs(buffer, nearCoverage);
        int[] midGlyphs = midCoverage == 0 ? new int[] {0} : readCoverageGlyphs(buffer, midCoverage);
        int[] farGlyphs = farCoverage == 0 ? new int[] {0} : readCoverageGlyphs(buffer, farCoverage);
        int[] fartherGlyphs = fartherCoverage == 0 ? new int[] {0} : readCoverageGlyphs(buffer, fartherCoverage);
        int[] fifthGlyphs = fifthCoverage == 0 ? new int[] {0} : readCoverageGlyphs(buffer, fifthCoverage);
        int[] sixthGlyphs = sixthCoverage == 0 ? new int[] {0} : readCoverageGlyphs(buffer, sixthCoverage);
        int[] seventhGlyphs = seventhCoverage == 0 ? new int[] {0} : readCoverageGlyphs(buffer, seventhCoverage);
        int[] eighthGlyphs = eighthCoverage == 0 ? new int[] {0} : readCoverageGlyphs(buffer, eighthCoverage);
        for (int eighth : eighthGlyphs) {
        for (int seventh : seventhGlyphs) {
            for (int sixth : sixthGlyphs) {
                for (int fifth : fifthGlyphs) {
                    for (int farther : fartherGlyphs) {
                        for (int far : farGlyphs) {
                            for (int mid : midGlyphs) {
                                for (int near : nearGlyphs) {
                                    for (int first : firsts) {
                                        for (int second : seconds) {
                                            for (int look : looks) {
                                                chains.put(
                                                        first,
                                                        second,
                                                        look,
                                                        (short) delta,
                                                        near,
                                                        mid,
                                                        far,
                                                        farther,
                                                        fifth,
                                                        sixth,
                                                        seventh,
                                                        eighth,
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
    }

    /// Returns the type-1 X-advance of the first subtable of `lookupOffset`, or `0`.
    private static int lookupXAdvance(ByteBuffer buffer, int lookupOffset) {
        if (lookupOffset + 8 > buffer.limit()) {
            return 0;
        }
        int saved = buffer.position();
        buffer.position(lookupOffset);
        int type = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        int subtableCount = Short.toUnsignedInt(buffer.getShort());
        if (type != 1 || subtableCount < 1 || buffer.remaining() < 2) {
            buffer.position(saved);
            return 0;
        }
        int subtable = lookupOffset + Short.toUnsignedInt(buffer.getShort());
        buffer.position(saved);
        SingleSink isolated = new SingleSink();
        readSinglePos(buffer, subtable, isolated);
        return isolated.count == 0 ? 0 : isolated.deltas[0];
    }

    /// Reads a type-3 cursive attachment subtable as exit-to-entry X-advance pairs.
    private static void readCursive(ByteBuffer buffer, int offset, PairSink pairs) {
        if (offset + 6 > buffer.limit()) {
            return;
        }
        buffer.position(offset);
        if (Short.toUnsignedInt(buffer.getShort()) != 1) {
            return;
        }
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count < 1 || buffer.remaining() < count * 4) {
            return;
        }
        int[] entryOffsets = new int[count];
        int[] exitOffsets = new int[count];
        for (int index = 0; index < count; index++) {
            int entry = Short.toUnsignedInt(buffer.getShort());
            int exit = Short.toUnsignedInt(buffer.getShort());
            entryOffsets[index] = entry == 0 ? 0 : offset + entry;
            exitOffsets[index] = exit == 0 ? 0 : offset + exit;
        }
        int[] glyphs = readCoverageGlyphs(buffer, coverageOffset);
        int limit = Math.min(count, glyphs.length);
        short[] entryXs = new short[limit];
        short[] exitXs = new short[limit];
        boolean[] hasEntry = new boolean[limit];
        boolean[] hasExit = new boolean[limit];
        for (int index = 0; index < limit; index++) {
            if (entryOffsets[index] != 0) {
                @Nullable Short entryX = readAnchorX(buffer, entryOffsets[index]);
                if (entryX != null) {
                    entryXs[index] = entryX;
                    hasEntry[index] = true;
                }
            }
            if (exitOffsets[index] != 0) {
                @Nullable Short exitX = readAnchorX(buffer, exitOffsets[index]);
                if (exitX != null) {
                    exitXs[index] = exitX;
                    hasExit[index] = true;
                }
            }
        }
        for (int left = 0; left < limit; left++) {
            if (!hasExit[left]) {
                continue;
            }
            for (int right = 0; right < limit; right++) {
                if (!hasEntry[right]) {
                    continue;
                }
                pairs.putIfAbsent(glyphs[left], glyphs[right], (short) (exitXs[left] - entryXs[right]));
            }
        }
    }

    /// Reads the X coordinate of a format-1, 2, or 3 anchor.
    private static @Nullable Short readAnchorX(ByteBuffer buffer, int offset) {
        if (offset + 6 > buffer.limit()) {
            return null;
        }
        int saved = buffer.position();
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        if (format < 1 || format > 3) {
            buffer.position(saved);
            return null;
        }
        short x = buffer.getShort();
        buffer.position(saved);
        return x;
    }

    /// Reads a type-5 mark-to-ligature subtable, using component 0 of each ligature.
    private static void readMarkLig(ByteBuffer buffer, int offset, MarkSink sink) {
        if (offset + 12 > buffer.limit()) {
            return;
        }
        buffer.position(offset);
        if (Short.toUnsignedInt(buffer.getShort()) != 1) {
            return;
        }
        int markCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        int ligaCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        int classCount = Short.toUnsignedInt(buffer.getShort());
        int markArray = offset + Short.toUnsignedInt(buffer.getShort());
        int ligaArray = offset + Short.toUnsignedInt(buffer.getShort());
        if (classCount == 0) {
            return;
        }
        int[] markGlyphs = readCoverageGlyphs(buffer, markCoverage);
        int[] ligaGlyphs = readCoverageGlyphs(buffer, ligaCoverage);
        int[] markClasses = new int[markGlyphs.length];
        int[] markXs = new int[markGlyphs.length];
        int[] markYs = new int[markGlyphs.length];
        if (!readMarkArray(buffer, markArray, markClasses, markXs, markYs)) {
            return;
        }
        if (ligaArray + 2 > buffer.limit()) {
            return;
        }
        int saved = buffer.position();
        buffer.position(ligaArray);
        int ligaCount = Short.toUnsignedInt(buffer.getShort());
        int limit = Math.min(ligaCount, ligaGlyphs.length);
        int[] attachOffsets = new int[limit];
        for (int index = 0; index < limit; index++) {
            if (buffer.remaining() < 2) {
                buffer.position(saved);
                return;
            }
            attachOffsets[index] = ligaArray + Short.toUnsignedInt(buffer.getShort());
        }
        buffer.position(saved);
        for (int liga = 0; liga < limit; liga++) {
            int attach = attachOffsets[liga];
            if (attach + 2 > buffer.limit()) {
                continue;
            }
            saved = buffer.position();
            buffer.position(attach);
            int components = Short.toUnsignedInt(buffer.getShort());
            if (components < 1 || buffer.remaining() < classCount * 2) {
                buffer.position(saved);
                continue;
            }
            int[] componentXs = new int[classCount];
            int[] componentYs = new int[classCount];
            boolean[] present = new boolean[classCount];
            for (int markClass = 0; markClass < classCount; markClass++) {
                int anchorOffset = Short.toUnsignedInt(buffer.getShort());
                if (anchorOffset == 0) {
                    continue;
                }
                int[] point = readAnchor(buffer, attach + anchorOffset);
                if (point == null) {
                    continue;
                }
                componentXs[markClass] = point[0];
                componentYs[markClass] = point[1];
                present[markClass] = true;
            }
            buffer.position(saved);
            for (int mark = 0; mark < markGlyphs.length; mark++) {
                int markClass = markClasses[mark];
                if (markClass < 0 || markClass >= classCount || !present[markClass]) {
                    continue;
                }
                sink.put(
                        markGlyphs[mark],
                        ligaGlyphs[liga],
                        (short) (componentXs[markClass] - markXs[mark]),
                        (short) (componentYs[markClass] - markYs[mark])
                );
            }
        }
    }

    /// Reads a type-4 mark-to-base subtable.
    private static void readMarkBase(ByteBuffer buffer, int offset, MarkSink sink) {
        if (offset + 12 > buffer.limit()) {
            return;
        }
        buffer.position(offset);
        if (Short.toUnsignedInt(buffer.getShort()) != 1) {
            return;
        }
        int markCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        int baseCoverage = offset + Short.toUnsignedInt(buffer.getShort());
        int classCount = Short.toUnsignedInt(buffer.getShort());
        int markArray = offset + Short.toUnsignedInt(buffer.getShort());
        int baseArray = offset + Short.toUnsignedInt(buffer.getShort());
        if (classCount == 0) {
            return;
        }
        int[] markGlyphs = readCoverageGlyphs(buffer, markCoverage);
        int[] baseGlyphs = readCoverageGlyphs(buffer, baseCoverage);
        int[] markClasses = new int[markGlyphs.length];
        int[] markXs = new int[markGlyphs.length];
        int[] markYs = new int[markGlyphs.length];
        if (!readMarkArray(buffer, markArray, markClasses, markXs, markYs)) {
            return;
        }
        int[] baseXs = new int[baseGlyphs.length * classCount];
        int[] baseYs = new int[baseGlyphs.length * classCount];
        boolean[] present = new boolean[baseGlyphs.length * classCount];
        if (!readBaseArray(buffer, baseArray, classCount, baseXs, baseYs, present)) {
            return;
        }
        for (int mark = 0; mark < markGlyphs.length; mark++) {
            int markClass = markClasses[mark];
            if (markClass < 0 || markClass >= classCount) {
                continue;
            }
            for (int base = 0; base < baseGlyphs.length; base++) {
                int cell = base * classCount + markClass;
                if (!present[cell]) {
                    continue;
                }
                sink.put(
                        markGlyphs[mark],
                        baseGlyphs[base],
                        (short) (baseXs[cell] - markXs[mark]),
                        (short) (baseYs[cell] - markYs[mark])
                );
            }
        }
    }

    /// Reads mark classes and anchors in coverage order.
    private static boolean readMarkArray(
            ByteBuffer buffer,
            int offset,
            int[] classes,
            int[] xs,
            int[] ys
    ) {
        if (offset + 2 > buffer.limit()) {
            return false;
        }
        int saved = buffer.position();
        buffer.position(offset);
        int count = Short.toUnsignedInt(buffer.getShort());
        int limit = Math.min(count, classes.length);
        for (int index = 0; index < limit; index++) {
            if (buffer.remaining() < 4) {
                buffer.position(saved);
                return false;
            }
            classes[index] = Short.toUnsignedInt(buffer.getShort());
            int anchor = offset + Short.toUnsignedInt(buffer.getShort());
            int[] point = readAnchor(buffer, anchor);
            if (point == null) {
                buffer.position(saved);
                return false;
            }
            xs[index] = point[0];
            ys[index] = point[1];
        }
        buffer.position(saved);
        return true;
    }

    /// Reads base anchors in coverage order.
    private static boolean readBaseArray(
            ByteBuffer buffer,
            int offset,
            int classCount,
            int[] xs,
            int[] ys,
            boolean[] present
    ) {
        if (offset + 2 > buffer.limit()) {
            return false;
        }
        int saved = buffer.position();
        buffer.position(offset);
        int count = Short.toUnsignedInt(buffer.getShort());
        int bases = xs.length / classCount;
        int limit = Math.min(count, bases);
        for (int base = 0; base < limit; base++) {
            for (int markClass = 0; markClass < classCount; markClass++) {
                if (buffer.remaining() < 2) {
                    buffer.position(saved);
                    return false;
                }
                int anchorOffset = Short.toUnsignedInt(buffer.getShort());
                if (anchorOffset == 0) {
                    continue;
                }
                int[] point = readAnchor(buffer, offset + anchorOffset);
                if (point == null) {
                    continue;
                }
                int cell = base * classCount + markClass;
                xs[cell] = point[0];
                ys[cell] = point[1];
                present[cell] = true;
            }
        }
        buffer.position(saved);
        return true;
    }

    /// Reads Anchor format 1, 2, or 3 coordinates.
    private static int @Nullable [] readAnchor(ByteBuffer buffer, int offset) {
        if (offset + 6 > buffer.limit()) {
            return null;
        }
        int saved = buffer.position();
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        if (format < 1 || format > 3) {
            buffer.position(saved);
            return null;
        }
        int x = buffer.getShort();
        int y = buffer.getShort();
        buffer.position(saved);
        return new int[]{x, y};
    }

    /// Reads a type-2 pair-positioning subtable.
    private static void readPairPos(ByteBuffer buffer, int offset, PairSink sink) {
        if (offset + 8 > buffer.limit()) {
            return;
        }
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        int coverageOffset = offset + Short.toUnsignedInt(buffer.getShort());
        int valueFormat1 = Short.toUnsignedInt(buffer.getShort());
        int valueFormat2 = Short.toUnsignedInt(buffer.getShort());
        int[] leftGlyphs = readCoverageGlyphs(buffer, coverageOffset);
        if (format == 1) {
            readPairPosFormat1(buffer, offset, valueFormat1, valueFormat2, leftGlyphs, sink);
            return;
        }
        if (format == 2) {
            readPairPosFormat2(buffer, offset, valueFormat1, valueFormat2, leftGlyphs, sink);
        }
    }

    /// Reads format-1 pair sets.
    private static void readPairPosFormat1(
            ByteBuffer buffer,
            int offset,
            int valueFormat1,
            int valueFormat2,
            int[] leftGlyphs,
            PairSink sink
    ) {
        if (buffer.remaining() < 2) {
            return;
        }
        int pairSetCount = Short.toUnsignedInt(buffer.getShort());
        int record1 = valueRecordSize(valueFormat1);
        int record2 = valueRecordSize(valueFormat2);
        int pairBytes = 2 + record1 + record2;
        for (int index = 0; index < pairSetCount; index++) {
            if (buffer.remaining() < 2) {
                return;
            }
            int pairSet = offset + Short.toUnsignedInt(buffer.getShort());
            if (index >= leftGlyphs.length || pairSet + 2 > buffer.limit()) {
                continue;
            }
            int saved = buffer.position();
            buffer.position(pairSet);
            int pairCount = Short.toUnsignedInt(buffer.getShort());
            int left = leftGlyphs[index];
            for (int pair = 0; pair < pairCount && buffer.remaining() >= pairBytes; pair++) {
                int right = Short.toUnsignedInt(buffer.getShort());
                int delta = readXAdvance(buffer, valueFormat1);
                skipValue(buffer, valueFormat2);
                sink.putIfAbsent(left, right, (short) delta);
            }
            buffer.position(saved);
        }
    }

    /// Reads format-2 class-pair positioning and expands every non-zero X-advance cell.
    ///
    /// ClassDef format 1 and 2 are both accepted. Covered left glyphs are paired with every
    /// glyph that ClassDef2 assigns a class.
    private static void readPairPosFormat2(
            ByteBuffer buffer,
            int offset,
            int valueFormat1,
            int valueFormat2,
            int[] leftGlyphs,
            PairSink sink
    ) {
        if (buffer.remaining() < 8) {
            return;
        }
        int classDef1Offset = offset + Short.toUnsignedInt(buffer.getShort());
        int classDef2Offset = offset + Short.toUnsignedInt(buffer.getShort());
        int class1Count = Short.toUnsignedInt(buffer.getShort());
        int class2Count = Short.toUnsignedInt(buffer.getShort());
        if (class1Count < 1 || class2Count < 1) {
            return;
        }
        int record1 = valueRecordSize(valueFormat1);
        int record2 = valueRecordSize(valueFormat2);
        int cell = record1 + record2;
        short[][] deltas = new short[class1Count][class2Count];
        for (int class1 = 0; class1 < class1Count; class1++) {
            for (int class2 = 0; class2 < class2Count; class2++) {
                if (buffer.remaining() < cell) {
                    return;
                }
                deltas[class1][class2] = (short) readXAdvance(buffer, valueFormat1);
                skipValue(buffer, valueFormat2);
            }
        }
        ClassMap class1 = ClassMap.read(buffer, classDef1Offset);
        ClassMap class2 = ClassMap.read(buffer, classDef2Offset);
        int[] rights = class2.assignedGlyphs();
        for (int left : leftGlyphs) {
            int leftClass = class1.classOf(left);
            if (leftClass < 0 || leftClass >= class1Count) {
                continue;
            }
            for (int right : rights) {
                int rightClass = class2.classOf(right);
                if (rightClass < 0 || rightClass >= class2Count) {
                    continue;
                }
                short delta = deltas[leftClass][rightClass];
                if (delta != 0) {
                    sink.putIfAbsent(left, right, delta);
                }
            }
        }
    }

    /// Reads coverage glyph ids in coverage-index order.
    private static int[] readCoverageGlyphs(ByteBuffer buffer, int offset) {
        if (offset + 4 > buffer.limit()) {
            return new int[0];
        }
        int saved = buffer.position();
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        if (format == 1) {
            int count = Short.toUnsignedInt(buffer.getShort());
            int[] glyphs = new int[count];
            for (int index = 0; index < count && buffer.remaining() >= 2; index++) {
                glyphs[index] = Short.toUnsignedInt(buffer.getShort());
            }
            buffer.position(saved);
            return glyphs;
        }
        if (format != 2) {
            buffer.position(saved);
            return new int[0];
        }
        int rangeCount = Short.toUnsignedInt(buffer.getShort());
        int[] glyphs = new int[0];
        int written = 0;
        for (int range = 0; range < rangeCount && buffer.remaining() >= 6; range++) {
            int first = Short.toUnsignedInt(buffer.getShort());
            int last = Short.toUnsignedInt(buffer.getShort());
            int startIndex = Short.toUnsignedInt(buffer.getShort());
            int needed = startIndex + (last - first) + 1;
            if (glyphs.length < needed) {
                glyphs = Arrays.copyOf(glyphs, needed);
            }
            for (int glyph = first; glyph <= last; glyph++) {
                glyphs[startIndex + (glyph - first)] = glyph;
                written = Math.max(written, startIndex + (glyph - first) + 1);
            }
        }
        buffer.position(saved);
        return written == glyphs.length ? glyphs : Arrays.copyOf(glyphs, written);
    }

    /// Reads a Microsoft/OT format-0 horizontal `kern` table.
    private static void readKern(ByteBuffer buffer, PairSink sink) {
        buffer.getShort();
        int tables = Short.toUnsignedInt(buffer.getShort());
        for (int index = 0; index < tables && buffer.remaining() >= 6; index++) {
            int subStart = buffer.position();
            buffer.getShort();
            int length = Short.toUnsignedInt(buffer.getShort());
            int coverage = Short.toUnsignedInt(buffer.getShort());
            int format = coverage >>> 8;
            boolean horizontal = (coverage & 0x0001) != 0;
            if (format == 0 && horizontal && buffer.remaining() >= 8) {
                int pairs = Short.toUnsignedInt(buffer.getShort());
                buffer.getShort();
                buffer.getShort();
                buffer.getShort();
                for (int pair = 0; pair < pairs && buffer.remaining() >= 6; pair++) {
                    int left = Short.toUnsignedInt(buffer.getShort());
                    int right = Short.toUnsignedInt(buffer.getShort());
                    short value = buffer.getShort();
                    sink.putIfAbsent(left, right, value);
                }
            }
            int next = subStart + Math.max(length, 6);
            if (next > buffer.limit()) {
                break;
            }
            buffer.position(next);
        }
    }

    /// Returns the size of one value record.
    private static int valueRecordSize(int format) {
        int size = 0;
        for (int bit = 0; bit < 8; bit++) {
            if ((format & (1 << bit)) != 0) {
                size += 2;
            }
        }
        return size;
    }

    /// Reads `XAdvance` from a value record and consumes the record.
    private static int readXAdvance(ByteBuffer buffer, int format) {
        int advance = 0;
        if ((format & 0x0001) != 0) {
            buffer.getShort();
        }
        if ((format & 0x0002) != 0) {
            buffer.getShort();
        }
        if ((format & 0x0004) != 0) {
            advance = buffer.getShort();
        }
        skipValue(buffer, format & ~0x0007);
        return advance;
    }

    /// Skips one value record.
    private static void skipValue(ByteBuffer buffer, int format) {
        int remaining = valueRecordSize(format);
        if (buffer.remaining() < remaining) {
            buffer.position(buffer.limit());
            return;
        }
        buffer.position(buffer.position() + remaining);
    }

    /// Accumulates unique pairs. Later sources do not overwrite earlier ones.
    private static final class PairSink {
        /// Packed keys.
        private int[] keys = new int[8];

        /// Deltas.
        private short[] deltas = new short[8];

        /// Count.
        private int count;

        /// Inserts a pair when the key is new.
        ///
        /// @param left the first glyph
        /// @param right the second glyph
        /// @param delta the X-advance delta
        private void putIfAbsent(int left, int right, short delta) {
            int key = (left << 16) | (right & 0xFFFF);
            for (int index = 0; index < count; index++) {
                if (keys[index] == key) {
                    return;
                }
            }
            if (count == keys.length) {
                keys = Arrays.copyOf(keys, keys.length * 2);
                deltas = Arrays.copyOf(deltas, deltas.length * 2);
            }
            keys[count] = key;
            deltas[count] = delta;
            count++;
        }
    }

    /// Accumulates pair keys tagged with a `MarkAttachmentType` class.
    private static final class AttachPairSink {
        /// Packed keys.
        private int[] keys = new int[8];

        /// Deltas.
        private short[] deltas = new short[8];

        /// Lookup attach classes.
        private int[] types = new int[8];

        /// Count.
        private int count;

        /// Copies unique pairs from `source` under `attachType`.
        ///
        /// @param source the parsed pairs
        /// @param attachType the lookup high-byte class
        private void addFrom(PairSink source, int attachType) {
            for (int index = 0; index < source.count; index++) {
                putIfAbsent(source.keys[index], attachType, source.deltas[index]);
            }
        }

        /// Inserts a tagged pair when the key and class are new.
        private void putIfAbsent(int key, int attachType, short delta) {
            for (int index = 0; index < count; index++) {
                if (keys[index] == key && types[index] == attachType) {
                    return;
                }
            }
            if (count == keys.length) {
                keys = Arrays.copyOf(keys, keys.length * 2);
                deltas = Arrays.copyOf(deltas, deltas.length * 2);
                types = Arrays.copyOf(types, types.length * 2);
            }
            keys[count] = key;
            deltas[count] = delta;
            types[count] = attachType;
            count++;
        }
    }

    /// Accumulates unique mark-to-base placements.
    private static final class MarkSink {
        /// Packed keys.
        private int[] keys = new int[8];

        /// X offsets.
        private short[] xs = new short[8];

        /// Y offsets.
        private short[] ys = new short[8];

        /// Mark glyph ids, not unique.
        private int[] marks = new int[8];

        /// Count.
        private int count;

        /// Inserts a placement, replacing a duplicate key.
        ///
        /// @param mark the mark glyph
        /// @param base the base glyph
        /// @param x the X offset
        /// @param y the Y offset
        private void put(int mark, int base, short x, short y) {
            int key = (mark << 16) | (base & 0xFFFF);
            for (int index = 0; index < count; index++) {
                if (keys[index] == key) {
                    xs[index] = x;
                    ys[index] = y;
                    return;
                }
            }
            if (count == keys.length) {
                keys = Arrays.copyOf(keys, keys.length * 2);
                xs = Arrays.copyOf(xs, xs.length * 2);
                ys = Arrays.copyOf(ys, ys.length * 2);
                marks = Arrays.copyOf(marks, marks.length * 2);
            }
            keys[count] = key;
            xs[count] = x;
            ys[count] = y;
            marks[count] = mark;
            count++;
        }
    }

    /// Accumulates type-1 single X-advance values.
    private static final class SingleSink {
        /// Glyph ids.
        private int[] glyphs = new int[8];

        /// Deltas.
        private short[] deltas = new short[8];

        /// Count.
        private int count;

        /// Inserts or replaces a single adjustment.
        private void put(int glyph, short delta) {
            for (int index = 0; index < count; index++) {
                if (glyphs[index] == glyph) {
                    deltas[index] = delta;
                    return;
                }
            }
            if (count == glyphs.length) {
                glyphs = Arrays.copyOf(glyphs, glyphs.length * 2);
                deltas = Arrays.copyOf(deltas, deltas.length * 2);
            }
            glyphs[count] = glyph;
            deltas[count] = delta;
            count++;
        }
    }

    /// Accumulates type-8 chain X-advance values.
    private static final class ChainSink {
        /// Packed current/next keys.
        private int[] pairs = new int[8];

        /// Lookahead glyphs.
        private int[] looks = new int[8];

        /// Deltas.
        private short[] deltas = new short[8];

        /// Packed required preceding glyphs, or `0`.
        private long[] backs = new long[8];

        /// Fifth required preceding glyph, or `0`.
        private int[] fifths = new int[8];

        /// Seventh required preceding glyph, or `0`.
        private int[] sevenths = new int[8];

        /// Ninth required preceding glyph, or `0`.
        private int[] ninths = new int[8];

        /// Count.
        private int count;

        /// Inserts a chain rule when the current/next/look/backtrack tuple is new.
        private void put(
                int current,
                int next,
                int lookahead,
                short delta,
                int backNear,
                int backMid,
                int backFar,
                int backFarther,
                int backFifth,
                int backSixth,
                int backSeventh,
                int backEighth,
                int backNinth
        ) {
            long packed = packBack(backNear, backMid, backFar, backFarther);
            int packedTail = (backFifth & 0xFFFF) | ((backSixth & 0xFFFF) << 16);
            int packedFar = (backSeventh & 0xFFFF) | ((backEighth & 0xFFFF) << 16);
            int key = (current << 16) | (next & 0xFFFF);
            for (int index = 0; index < count; index++) {
                if (pairs[index] == key
                        && looks[index] == lookahead
                        && backs[index] == packed
                        && fifths[index] == packedTail
                        && sevenths[index] == packedFar
                        && ninths[index] == backNinth) {
                    return;
                }
            }
            if (count == pairs.length) {
                pairs = Arrays.copyOf(pairs, pairs.length * 2);
                looks = Arrays.copyOf(looks, looks.length * 2);
                deltas = Arrays.copyOf(deltas, deltas.length * 2);
                backs = Arrays.copyOf(backs, backs.length * 2);
                fifths = Arrays.copyOf(fifths, fifths.length * 2);
                sevenths = Arrays.copyOf(sevenths, sevenths.length * 2);
                ninths = Arrays.copyOf(ninths, ninths.length * 2);
            }
            pairs[count] = key;
            looks[count] = lookahead;
            deltas[count] = delta;
            backs[count] = packed;
            fifths[count] = packedTail;
            sevenths[count] = packedFar;
            ninths[count] = backNinth;
            count++;
        }
    }

    /// Accumulates type-8 triples tagged with a `MarkAttachmentType` class.
    private static final class AttachChainSink {
        /// Packed current/next keys.
        private int[] pairs = new int[8];

        /// Lookahead glyphs.
        private int[] looks = new int[8];

        /// Deltas.
        private short[] deltas = new short[8];

        /// Lookup attach classes.
        private int[] types = new int[8];

        /// Packed required preceding glyphs.
        private long[] backs = new long[8];

        /// Count.
        private int count;

        /// Copies unique triples from `source` under `attachType`.
        ///
        /// @param source the parsed chains
        /// @param attachType the lookup high-byte class
        private void addFrom(ChainSink source, int attachType) {
            for (int index = 0; index < source.count; index++) {
                putIfAbsent(
                        source.pairs[index],
                        source.looks[index],
                        attachType,
                        source.deltas[index],
                        source.backs[index]
                );
            }
        }

        /// Inserts a tagged triple when the key, lookahead, class, and backtrack are new.
        private void putIfAbsent(int key, int lookahead, int attachType, short delta, long packedBack) {
            for (int index = 0; index < count; index++) {
                if (pairs[index] == key
                        && looks[index] == lookahead
                        && types[index] == attachType
                        && backs[index] == packedBack) {
                    return;
                }
            }
            if (count == pairs.length) {
                pairs = Arrays.copyOf(pairs, pairs.length * 2);
                looks = Arrays.copyOf(looks, looks.length * 2);
                deltas = Arrays.copyOf(deltas, deltas.length * 2);
                types = Arrays.copyOf(types, types.length * 2);
                backs = Arrays.copyOf(backs, backs.length * 2);
            }
            pairs[count] = key;
            looks[count] = lookahead;
            deltas[count] = delta;
            types[count] = attachType;
            backs[count] = packedBack;
            count++;
        }
    }

    /// One pair whose lookup skips bases, ligatures, or marks outside a filter set.
    ///
    /// @param left the first input glyph
    /// @param right the next kept glyph
    /// @param delta the X-advance
    /// @param flag the lookup flag
    /// @param markSet the mark-filter set
    private record FlaggedPair(int left, int right, short delta, int flag, int markSet) {
    }

    /// One type-8 triple whose lookup skips bases, ligatures, or marks outside a filter set.
    ///
    /// @param current the first input glyph
    /// @param next the next kept glyph
    /// @param look the following kept glyph
    /// @param delta the X-advance
    /// @param flag the lookup flag
    /// @param markSet the mark-filter set
    /// @param back the nearest required preceding glyph, or `0`
    /// @param far the next required preceding glyph, or `0`
    /// @param farther the next required preceding glyph, or `0`
    /// @param farthest the farthest required preceding glyph, or `0`
    private record FlaggedChain(
            int current,
            int next,
            int look,
            short delta,
            int flag,
            int markSet,
            int back,
            int far,
            int farther,
            int farthest
    ) {
    }

    /// Accumulates class-skip pair rules.
    private static final class FlaggedPairSink {
        /// Rules.
        private FlaggedPair[] rules = new FlaggedPair[4];

        /// Count.
        private int count;

        /// Copies unique pairs from `source` under `flag` and `markSet`.
        ///
        /// @param source the parsed pairs
        /// @param flag the lookup flag
        /// @param markSet the mark-filter set
        private void addFrom(PairSink source, int flag, int markSet) {
            for (int index = 0; index < source.count; index++) {
                int left = source.keys[index] >>> 16;
                int right = source.keys[index] & 0xFFFF;
                putIfAbsent(new FlaggedPair(left, right, source.deltas[index], flag, markSet));
            }
        }

        /// Inserts a rule when the pair, flag, and set are new.
        private void putIfAbsent(FlaggedPair rule) {
            for (int index = 0; index < count; index++) {
                if (rules[index].equals(rule)) {
                    return;
                }
            }
            if (count == rules.length) {
                rules = Arrays.copyOf(rules, rules.length * 2);
            }
            rules[count++] = rule;
        }
    }

    /// Accumulates class-skip chain rules.
    private static final class FlaggedChainSink {
        /// Rules.
        private FlaggedChain[] rules = new FlaggedChain[4];

        /// Count.
        private int count;

        /// Copies unique triples from `source` under `flag` and `markSet`.
        ///
        /// @param source the parsed chains
        /// @param flag the lookup flag
        /// @param markSet the mark-filter set
        private void addFrom(ChainSink source, int flag, int markSet) {
            for (int index = 0; index < source.count; index++) {
                int current = source.pairs[index] >>> 16;
                int next = source.pairs[index] & 0xFFFF;
                long packed = source.backs[index];
                putIfAbsent(new FlaggedChain(
                        current,
                        next,
                        source.looks[index],
                        source.deltas[index],
                        flag,
                        markSet,
                        (int) (packed & 0xFFFFL),
                        (int) ((packed >>> 16) & 0xFFFFL),
                        (int) ((packed >>> 32) & 0xFFFFL),
                        (int) ((packed >>> 48) & 0xFFFFL)
                ));
            }
        }

        /// Inserts a rule when the triple, flag, and set are new.
        private void putIfAbsent(FlaggedChain rule) {
            for (int index = 0; index < count; index++) {
                if (rules[index].equals(rule)) {
                    return;
                }
            }
            if (count == rules.length) {
                rules = Arrays.copyOf(rules, rules.length * 2);
            }
            rules[count++] = rule;
        }
    }

    /// Sorts pair, single, chain, skip, attach, and mark maps.
    private static GposPositioning finish(
            PairSink pairs,
            PairSink skipPairs,
            AttachPairSink attachPairs,
            FlaggedPairSink flaggedPairs,
            MarkSink marks,
            SingleSink singles,
            ChainSink chains,
            ChainSink skipChains,
            AttachChainSink attachChains,
            FlaggedChainSink flaggedChains,
            GdefTable gdef
    ) {
        int[] pairOrder = sortOrder(pairs.keys, pairs.count);
        int[] sortedPairKeys = new int[pairs.count];
        short[] sortedDeltas = new short[pairs.count];
        for (int index = 0; index < pairs.count; index++) {
            sortedPairKeys[index] = pairs.keys[pairOrder[index]];
            sortedDeltas[index] = pairs.deltas[pairOrder[index]];
        }
        int[] markOrder = sortOrder(marks.keys, marks.count);
        int[] sortedMarkKeys = new int[marks.count];
        short[] sortedXs = new short[marks.count];
        short[] sortedYs = new short[marks.count];
        int[] markGlyphs = new int[marks.count];
        for (int index = 0; index < marks.count; index++) {
            int source = markOrder[index];
            sortedMarkKeys[index] = marks.keys[source];
            sortedXs[index] = marks.xs[source];
            sortedYs[index] = marks.ys[source];
            markGlyphs[index] = marks.marks[source];
        }
        Arrays.sort(markGlyphs);
        int unique = 0;
        for (int index = 0; index < markGlyphs.length; index++) {
            if (unique == 0 || markGlyphs[index] != markGlyphs[unique - 1]) {
                markGlyphs[unique++] = markGlyphs[index];
            }
        }
        if (unique != markGlyphs.length) {
            markGlyphs = Arrays.copyOf(markGlyphs, unique);
        }
        int[] singleOrder = sortOrder(singles.glyphs, singles.count);
        int[] sortedSingleGlyphs = new int[singles.count];
        short[] sortedSingleDeltas = new short[singles.count];
        for (int index = 0; index < singles.count; index++) {
            int source = singleOrder[index];
            sortedSingleGlyphs[index] = singles.glyphs[source];
            sortedSingleDeltas[index] = singles.deltas[source];
        }
        int[] chainPairs = Arrays.copyOf(chains.pairs, chains.count);
        int[] chainLooks = Arrays.copyOf(chains.looks, chains.count);
        short[] chainDeltas = Arrays.copyOf(chains.deltas, chains.count);
        long[] chainBacks = Arrays.copyOf(chains.backs, chains.count);
        int[] chainFifths = Arrays.copyOf(chains.fifths, chains.count);
        int[] chainSevenths = Arrays.copyOf(chains.sevenths, chains.count);
        int[] chainNinths = Arrays.copyOf(chains.ninths, chains.count);
        int[] skipOrder = sortOrder(skipPairs.keys, skipPairs.count);
        int[] sortedSkipKeys = new int[skipPairs.count];
        short[] sortedSkipDeltas = new short[skipPairs.count];
        for (int index = 0; index < skipPairs.count; index++) {
            sortedSkipKeys[index] = skipPairs.keys[skipOrder[index]];
            sortedSkipDeltas[index] = skipPairs.deltas[skipOrder[index]];
        }
        int[] skipChainPairs = Arrays.copyOf(skipChains.pairs, skipChains.count);
        int[] skipChainLooks = Arrays.copyOf(skipChains.looks, skipChains.count);
        short[] skipChainDeltas = Arrays.copyOf(skipChains.deltas, skipChains.count);
        long[] skipChainBacks = Arrays.copyOf(skipChains.backs, skipChains.count);
        int[] copiedAttachKeys = Arrays.copyOf(attachPairs.keys, attachPairs.count);
        short[] copiedAttachDeltas = Arrays.copyOf(attachPairs.deltas, attachPairs.count);
        int[] copiedAttachTypes = Arrays.copyOf(attachPairs.types, attachPairs.count);
        int[] copiedAttachChainPairs = Arrays.copyOf(attachChains.pairs, attachChains.count);
        int[] copiedAttachChainLooks = Arrays.copyOf(attachChains.looks, attachChains.count);
        short[] copiedAttachChainDeltas = Arrays.copyOf(attachChains.deltas, attachChains.count);
        int[] copiedAttachChainTypes = Arrays.copyOf(attachChains.types, attachChains.count);
        long[] copiedAttachChainBacks = Arrays.copyOf(attachChains.backs, attachChains.count);
        int[] attachmentTypes = uniqueTypes(copiedAttachTypes, copiedAttachChainTypes);
        FlaggedPair[] copiedFlaggedPairs = Arrays.copyOf(flaggedPairs.rules, flaggedPairs.count);
        FlaggedChain[] copiedFlaggedChains = Arrays.copyOf(flaggedChains.rules, flaggedChains.count);
        if (pairs.count == 0
                && marks.count == 0
                && singles.count == 0
                && chains.count == 0
                && skipPairs.count == 0
                && skipChains.count == 0
                && attachPairs.count == 0
                && attachChains.count == 0
                && flaggedPairs.count == 0
                && flaggedChains.count == 0) {
            return NONE;
        }
        return new GposPositioning(
                sortedPairKeys,
                sortedDeltas,
                sortedSingleGlyphs,
                sortedSingleDeltas,
                chainPairs,
                chainLooks,
                chainDeltas,
                chainBacks,
                chainFifths,
                chainSevenths,
                chainNinths,
                sortedMarkKeys,
                sortedXs,
                sortedYs,
                markGlyphs,
                sortedSkipKeys,
                sortedSkipDeltas,
                skipChainPairs,
                skipChainLooks,
                skipChainDeltas,
                skipChainBacks,
                copiedAttachKeys,
                copiedAttachDeltas,
                copiedAttachTypes,
                copiedAttachChainPairs,
                copiedAttachChainLooks,
                copiedAttachChainDeltas,
                copiedAttachChainTypes,
                copiedAttachChainBacks,
                attachmentTypes,
                gdef,
                copiedFlaggedPairs,
                copiedFlaggedChains
        );
    }

    /// Returns the sorted unique attach classes from pair and chain maps.
    private static int[] uniqueTypes(int[] pairTypes, int[] chainTypes) {
        int[] merged = Arrays.copyOf(pairTypes, pairTypes.length + chainTypes.length);
        System.arraycopy(chainTypes, 0, merged, pairTypes.length, chainTypes.length);
        Arrays.sort(merged);
        int unique = 0;
        for (int index = 0; index < merged.length; index++) {
            if (merged[index] == 0) {
                continue;
            }
            if (unique == 0 || merged[index] != merged[unique - 1]) {
                merged[unique++] = merged[index];
            }
        }
        return unique == merged.length ? merged : Arrays.copyOf(merged, unique);
    }

    /// Returns an insertion-sorted index order for `keys[0, count)`.
    private static int[] sortOrder(int[] keys, int count) {
        int[] order = new int[count];
        for (int index = 0; index < count; index++) {
            order[index] = index;
        }
        for (int index = 1; index < count; index++) {
            int item = order[index];
            int walk = index;
            while (walk > 0 && keys[order[walk - 1]] > keys[item]) {
                order[walk] = order[walk - 1];
                walk--;
            }
            order[walk] = item;
        }
        return order;
    }
}
