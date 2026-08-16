package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/// Applies GPOS lookup type 2 pair positioning and format-0 `kern` pairs as X-advance deltas.
///
/// Other GPOS lookup types are skipped. Missing tables return a zero adjustment.
@NotNullByDefault
final class GposPositioning {
    /// Empty positioning.
    static final GposPositioning NONE = new GposPositioning(new int[0], new short[0]);

    /// Packed `(left << 16) | right` keys, sorted.
    private final int[] keys;

    /// Parallel X-advance deltas in font units.
    private final short[] deltas;

    /// Creates a pair map.
    ///
    /// @param keys the packed keys
    /// @param deltas the deltas
    private GposPositioning(int[] keys, short[] deltas) {
        this.keys = keys;
        this.deltas = deltas;
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

    /// Parses GPOS type-2 pairs, then overlays format-0 `kern` pairs that GPOS did not name.
    ///
    /// @param gpos the GPOS table, or `null`
    /// @param kern the `kern` table, or `null`
    /// @return the positioning
    static GposPositioning parse(@Nullable ByteBuffer gpos, @Nullable ByteBuffer kern) {
        PairSink sink = new PairSink();
        if (gpos != null && gpos.remaining() >= 10) {
            readGpos(gpos.duplicate().order(ByteOrder.BIG_ENDIAN), sink);
        }
        if (kern != null && kern.remaining() >= 4) {
            readKern(kern.duplicate().order(ByteOrder.BIG_ENDIAN), sink);
        }
        return sink.toPositioning();
    }

    /// Reads GPOS lookup type 2 pair positioning.
    private static void readGpos(ByteBuffer buffer, PairSink sink) {
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
        boolean[] selected = selectKernLookups(buffer, featureList, lookupOffsets.length);
        for (int index = 0; index < lookupOffsets.length; index++) {
            if (selected != null && !selected[index]) {
                continue;
            }
            readLookup(buffer, lookupOffsets[index], sink);
        }
    }

    /// Marks lookups listed by a `kern` feature, or `null` to apply every type-2 lookup.
    private static boolean @Nullable [] selectKernLookups(ByteBuffer buffer, int featureList, int lookupCount) {
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
            if (tag != 0x6B65726E) {
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

    /// Reads one lookup. Non-type-2 lookups are ignored.
    private static void readLookup(ByteBuffer buffer, int offset, PairSink sink) {
        if (offset + 6 > buffer.limit()) {
            return;
        }
        buffer.position(offset);
        int type = Short.toUnsignedInt(buffer.getShort());
        int flag = Short.toUnsignedInt(buffer.getShort());
        int subtableCount = Short.toUnsignedInt(buffer.getShort());
        if (type != 2) {
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
            readPairPos(buffer, subtable, sink);
        }
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

        /// Sorts keys and returns an immutable map.
        ///
        /// @return the positioning
        private GposPositioning toPositioning() {
            if (count == 0) {
                return NONE;
            }
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
            int[] sortedKeys = new int[count];
            short[] sortedDeltas = new short[count];
            for (int index = 0; index < count; index++) {
                sortedKeys[index] = keys[order[index]];
                sortedDeltas[index] = deltas[order[index]];
            }
            return new GposPositioning(sortedKeys, sortedDeltas);
        }
    }
}
