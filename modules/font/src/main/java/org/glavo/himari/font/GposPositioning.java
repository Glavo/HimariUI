package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/// Applies GPOS pair positioning, format-0 `kern` pairs, and type-4 mark-to-base placement.
///
/// Other GPOS lookup types are skipped. Missing tables return a zero adjustment or no mark.
@NotNullByDefault
final class GposPositioning {
    /// Empty positioning.
    static final GposPositioning NONE = new GposPositioning(
            new int[0],
            new short[0],
            new int[0],
            new short[0],
            new short[0],
            new int[0]
    );

    /// Packed `(left << 16) | right` keys, sorted.
    private final int[] keys;

    /// Parallel X-advance deltas in font units.
    private final short[] deltas;

    /// Packed `(mark << 16) | base` keys, sorted.
    private final int[] markKeys;

    /// Parallel mark X offsets.
    private final short[] markXs;

    /// Parallel mark Y offsets.
    private final short[] markYs;

    /// Sorted unique mark glyph ids.
    private final int[] markGlyphs;

    /// Creates pair and mark maps.
    ///
    /// @param keys the pair keys
    /// @param deltas the pair deltas
    /// @param markKeys the mark/base keys
    /// @param markXs the mark X offsets
    /// @param markYs the mark Y offsets
    /// @param markGlyphs the unique mark glyph ids
    private GposPositioning(
            int[] keys,
            short[] deltas,
            int[] markKeys,
            short[] markXs,
            short[] markYs,
            int[] markGlyphs
    ) {
        this.keys = keys;
        this.deltas = deltas;
        this.markKeys = markKeys;
        this.markXs = markXs;
        this.markYs = markYs;
        this.markGlyphs = markGlyphs;
    }

    /// Returns the X-advance delta for the pair `(left, right)`.
    ///
    /// @param left the first glyph
    /// @param right the second glyph
    /// @return the signed delta, or `0`
    int pairAdjustment(int left, int right) {
        if (left < 0 || right < 0 || left > 0xFFFF || right > 0xFFFF) {
            return 0;
        }
        int key = (left << 16) | right;
        int index = Arrays.binarySearch(keys, key);
        return index >= 0 ? deltas[index] : 0;
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
        PairSink pairs = new PairSink();
        MarkSink marks = new MarkSink();
        if (gpos != null && gpos.remaining() >= 10) {
            readGpos(gpos.duplicate().order(ByteOrder.BIG_ENDIAN), pairs, marks);
        }
        if (kern != null && kern.remaining() >= 4) {
            readKern(kern.duplicate().order(ByteOrder.BIG_ENDIAN), pairs);
        }
        return finish(pairs, marks);
    }

    /// Reads GPOS lookup type 2 pair positioning and type 4 mark-to-base.
    private static void readGpos(ByteBuffer buffer, PairSink pairs, MarkSink marks) {
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
        boolean[] markSelected = selectFeatureLookups(buffer, featureList, lookupOffsets.length, 0x6D61726B);
        for (int index = 0; index < lookupOffsets.length; index++) {
            int type = peekLookupType(buffer, lookupOffsets[index]);
            if (type == 2 && kernSelected != null && !kernSelected[index]) {
                continue;
            }
            if (type == 4 && markSelected != null && !markSelected[index]) {
                continue;
            }
            readLookup(buffer, lookupOffsets[index], pairs, marks);
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

    /// Reads one lookup. Types other than 2 and 4 are ignored.
    private static void readLookup(ByteBuffer buffer, int offset, PairSink pairs, MarkSink marks) {
        if (offset + 6 > buffer.limit()) {
            return;
        }
        buffer.position(offset);
        int type = Short.toUnsignedInt(buffer.getShort());
        int flag = Short.toUnsignedInt(buffer.getShort());
        int subtableCount = Short.toUnsignedInt(buffer.getShort());
        if (type != 2 && type != 4) {
            return;
        }
        int[] subtables = new int[subtableCount];
        for (int index = 0; index < subtableCount; index++) {
            subtables[index] = offset + Short.toUnsignedInt(buffer.getShort());
        }
        if ((flag & 0x0010) != 0 && buffer.remaining() >= 2) {
            buffer.getShort();
        }
        for (int subtable : subtables) {
            if (type == 2) {
                readPairPos(buffer, subtable, pairs);
            } else {
                readMarkBase(buffer, subtable, marks);
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
            readPairPosFormat2(buffer, valueFormat1, valueFormat2, leftGlyphs, sink);
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

    /// Reads format-2 class-pair positioning for class-zero pairs only when both class defs are trivial.
    ///
    /// Non-trivial class tables are skipped; constructed fonts use format 1.
    private static void readPairPosFormat2(
            ByteBuffer buffer,
            int valueFormat1,
            int valueFormat2,
            int[] leftGlyphs,
            PairSink sink
    ) {
        if (buffer.remaining() < 8 || leftGlyphs.length != 1) {
            return;
        }
        buffer.getShort();
        buffer.getShort();
        int class1Count = Short.toUnsignedInt(buffer.getShort());
        int class2Count = Short.toUnsignedInt(buffer.getShort());
        if (class1Count < 1 || class2Count < 1) {
            return;
        }
        int record1 = valueRecordSize(valueFormat1);
        int record2 = valueRecordSize(valueFormat2);
        int cell = record1 + record2;
        if (buffer.remaining() < cell) {
            return;
        }
        int delta = readXAdvance(buffer, valueFormat1);
        skipValue(buffer, valueFormat2);
        sink.putIfAbsent(leftGlyphs[0], leftGlyphs[0], (short) delta);
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

    /// Sorts pair and mark maps.
    private static GposPositioning finish(PairSink pairs, MarkSink marks) {
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
        if (pairs.count == 0 && marks.count == 0) {
            return NONE;
        }
        return new GposPositioning(
                sortedPairKeys,
                sortedDeltas,
                sortedMarkKeys,
                sortedXs,
                sortedYs,
                markGlyphs
        );
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
