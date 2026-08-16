package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/// Applies GSUB lookup type 1 (single substitution) for named features.
///
/// Other lookup types are skipped. Missing tables, unknown features, and glyphs outside coverage
/// leave the input identity unchanged.
@NotNullByDefault
final class GsubSubstitutions {
    /// Empty substitutions.
    static final GsubSubstitutions NONE = new GsubSubstitutions(new Feature[0]);

    /// Features in table order.
    private final Feature[] features;

    /// Creates a substitution table.
    ///
    /// @param features the features
    private GsubSubstitutions(Feature[] features) {
        this.features = features;
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
            for (SingleSubst subst : feature.lookups) {
                current = subst.apply(current);
            }
        }
        return current;
    }

    /// Parses a GSUB table, or returns [`#NONE`] when the header is absent.
    ///
    /// @param table the GSUB bytes, or `null`
    /// @return the substitutions
    static GsubSubstitutions parse(@Nullable ByteBuffer table) {
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
        SingleSubst[] lookups = readLookups(buffer, lookupList);
        return new GsubSubstitutions(readFeatures(buffer, featureList, lookups));
    }

    /// Reads the feature list.
    private static Feature[] readFeatures(ByteBuffer buffer, int featureList, SingleSubst[] lookups) {
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
            features[index] = new Feature(tag, readFeatureLookups(buffer, offset, lookups));
        }
        return features;
    }

    /// Resolves lookup indices for one feature.
    private static SingleSubst[] readFeatureLookups(ByteBuffer buffer, int offset, SingleSubst[] lookups) {
        if (offset + 4 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB feature table is truncated");
        }
        int saved = buffer.position();
        buffer.position(offset);
        buffer.getShort();
        int count = Short.toUnsignedInt(buffer.getShort());
        SingleSubst[] selected = new SingleSubst[count];
        int written = 0;
        for (int index = 0; index < count; index++) {
            int lookupIndex = Short.toUnsignedInt(buffer.getShort());
            if (lookupIndex < lookups.length && lookups[lookupIndex] != null) {
                selected[written++] = lookups[lookupIndex];
            }
        }
        buffer.position(saved);
        if (written == selected.length) {
            return selected;
        }
        return Arrays.copyOf(selected, written);
    }

    /// Reads the lookup list, keeping only type-1 subtables.
    private static SingleSubst[] readLookups(ByteBuffer buffer, int lookupList) {
        if (lookupList + 2 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB lookup list is truncated");
        }
        buffer.position(lookupList);
        int count = Short.toUnsignedInt(buffer.getShort());
        SingleSubst[] lookups = new SingleSubst[count];
        int[] offsets = new int[count];
        for (int index = 0; index < count; index++) {
            offsets[index] = lookupList + Short.toUnsignedInt(buffer.getShort());
        }
        for (int index = 0; index < count; index++) {
            lookups[index] = readLookup(buffer, offsets[index]);
        }
        return lookups;
    }

    /// Reads one lookup. Non-type-1 lookups become `null`.
    private static @Nullable SingleSubst readLookup(ByteBuffer buffer, int offset) {
        if (offset + 6 > buffer.limit()) {
            throw new IllegalArgumentException("GSUB lookup is truncated");
        }
        buffer.position(offset);
        int type = Short.toUnsignedInt(buffer.getShort());
        int flag = Short.toUnsignedInt(buffer.getShort());
        int subtableCount = Short.toUnsignedInt(buffer.getShort());
        if (type != 1 || subtableCount == 0) {
            return null;
        }
        int first = offset + Short.toUnsignedInt(buffer.getShort());
        if ((flag & 0x0010) != 0 && buffer.remaining() >= 2) {
            buffer.getShort();
        }
        return readSingleSubst(buffer, first);
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

    /// Stores one named feature and its type-1 lookups.
    ///
    /// @param tag the feature tag
    /// @param lookups the type-1 lookups in apply order
    private record Feature(int tag, SingleSubst[] lookups) {
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
    }
}
