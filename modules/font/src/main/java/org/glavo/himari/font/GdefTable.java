package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/// Reads a GDEF glyph-class, mark-attach ClassDef, and optional MarkGlyphSets.
///
/// First-stable faces use ClassDef format 1 or 2 and MarkGlyphSets format 1. Missing tables and
/// unassigned glyphs report class `0`. Class `1` is a base, class `2` is a ligature, and class
/// `3` is a combining mark. Version 1.2 `MarkGlyphSetsDef` feeds `UseMarkFilteringSet`.
@NotNullByDefault
public final class GdefTable {
    /// Empty table used when GDEF is absent.
    public static final GdefTable NONE = new GdefTable(ClassDef.EMPTY, ClassDef.EMPTY, new int[0][]);

    /// Glyph class `Base`.
    public static final int CLASS_BASE = 1;

    /// Glyph class `Ligature`.
    public static final int CLASS_LIGATURE = 2;

    /// Glyph class `Mark`.
    public static final int CLASS_MARK = 3;

    /// Lookup flag `IgnoreBaseGlyphs`.
    public static final int FLAG_IGNORE_BASE = 0x0002;

    /// Lookup flag `IgnoreLigatures`.
    public static final int FLAG_IGNORE_LIGATURE = 0x0004;

    /// Lookup flag `IgnoreMarks`.
    public static final int FLAG_IGNORE_MARKS = 0x0008;

    /// Lookup flag `UseMarkFilteringSet`.
    public static final int FLAG_MARK_FILTER = 0x0010;

    /// GlyphClassDef.
    private final ClassDef glyphClass;

    /// MarkAttachClassDef used by `MarkAttachmentType` lookups.
    private final ClassDef markAttach;

    /// Sorted glyph ids per MarkGlyphSet, empty when the 1.2 table is absent.
    private final int[][] markSets;

    /// Creates a table.
    ///
    /// @param glyphClass the glyph classes
    /// @param markAttach the mark-attach classes
    /// @param markSets the mark-filter coverage sets
    private GdefTable(ClassDef glyphClass, ClassDef markAttach, int[][] markSets) {
        this.glyphClass = glyphClass;
        this.markAttach = markAttach;
        this.markSets = markSets;
    }

    /// Parses a GDEF table, or returns [`#NONE`].
    ///
    /// @param table the GDEF bytes, or `null`
    /// @return the table
    static GdefTable parse(@Nullable ByteBuffer table) {
        if (table == null || table.remaining() < 12) {
            return NONE;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        int start = buffer.position();
        int major = Short.toUnsignedInt(buffer.getShort());
        int minor = Short.toUnsignedInt(buffer.getShort());
        if (major != 1) {
            return NONE;
        }
        int glyphClassOffset = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        buffer.getShort();
        int markAttachOffset = Short.toUnsignedInt(buffer.getShort());
        int markSetsOffset = 0;
        if (minor >= 2 && buffer.remaining() >= 2) {
            markSetsOffset = Short.toUnsignedInt(buffer.getShort());
        }
        ClassDef glyphClass = glyphClassOffset == 0
                ? ClassDef.EMPTY
                : ClassDef.read(buffer, start + glyphClassOffset);
        ClassDef markAttach = markAttachOffset == 0
                ? ClassDef.EMPTY
                : ClassDef.read(buffer, start + markAttachOffset);
        int[][] markSets = markSetsOffset == 0
                ? new int[0][]
                : readMarkSets(buffer, start + markSetsOffset);
        return new GdefTable(glyphClass, markAttach, markSets);
    }

    /// Reads MarkGlyphSets format 1 into sorted coverage arrays.
    private static int[][] readMarkSets(ByteBuffer buffer, int offset) {
        if (offset + 4 > buffer.limit()) {
            return new int[0][];
        }
        int saved = buffer.position();
        buffer.position(offset);
        if (Short.toUnsignedInt(buffer.getShort()) != 1) {
            buffer.position(saved);
            return new int[0][];
        }
        int count = Short.toUnsignedInt(buffer.getShort());
        int[][] sets = new int[count][];
        int[] coverageOffsets = new int[count];
        for (int index = 0; index < count; index++) {
            if (buffer.remaining() < 4) {
                sets[index] = new int[0];
                continue;
            }
            coverageOffsets[index] = offset + buffer.getInt();
        }
        for (int index = 0; index < count; index++) {
            sets[index] = coverageOffsets[index] == 0 ? new int[0] : readCoverage(buffer, coverageOffsets[index]);
        }
        buffer.position(saved);
        return sets;
    }

    /// Reads Coverage format 1 or 2 as sorted unique glyph ids.
    private static int[] readCoverage(ByteBuffer buffer, int offset) {
        if (offset + 4 > buffer.limit()) {
            return new int[0];
        }
        int saved = buffer.position();
        buffer.position(offset);
        int format = Short.toUnsignedInt(buffer.getShort());
        if (format == 1) {
            int count = Short.toUnsignedInt(buffer.getShort());
            int[] glyphs = new int[count];
            int written = 0;
            for (int index = 0; index < count && buffer.remaining() >= 2; index++) {
                glyphs[written++] = Short.toUnsignedInt(buffer.getShort());
            }
            buffer.position(saved);
            if (written != glyphs.length) {
                glyphs = Arrays.copyOf(glyphs, written);
            }
            Arrays.sort(glyphs);
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
            buffer.getShort();
            int needed = written + Math.max(0, last - first + 1);
            if (glyphs.length < needed) {
                glyphs = Arrays.copyOf(glyphs, needed);
            }
            for (int glyph = first; glyph <= last; glyph++) {
                glyphs[written++] = glyph;
            }
        }
        buffer.position(saved);
        if (written != glyphs.length) {
            glyphs = Arrays.copyOf(glyphs, written);
        }
        Arrays.sort(glyphs);
        return glyphs;
    }

    /// Returns the GDEF glyph class, or `0` when unassigned.
    ///
    /// @param glyphId the glyph
    /// @return the class
    public int glyphClass(int glyphId) {
        return glyphClass.classOf(glyphId);
    }

    /// Returns whether GDEF classifies `glyphId` as a combining mark.
    ///
    /// @param glyphId the glyph
    /// @return whether it is a mark
    public boolean isMark(int glyphId) {
        return glyphClass.classOf(glyphId) == CLASS_MARK;
    }

    /// Returns whether GDEF classifies `glyphId` as a base glyph.
    ///
    /// @param glyphId the glyph
    /// @return whether it is a base
    public boolean isBase(int glyphId) {
        return glyphClass.classOf(glyphId) == CLASS_BASE;
    }

    /// Returns whether GDEF classifies `glyphId` as a ligature glyph.
    ///
    /// @param glyphId the glyph
    /// @return whether it is a ligature
    public boolean isLigature(int glyphId) {
        return glyphClass.classOf(glyphId) == CLASS_LIGATURE;
    }

    /// Returns the mark-attach class, or `0`.
    ///
    /// @param glyphId the glyph
    /// @return the class
    public int markAttachClass(int glyphId) {
        return markAttach.classOf(glyphId);
    }

    /// Returns whether `glyphId` is covered by MarkGlyphSet `setIndex`.
    ///
    /// @param glyphId the glyph
    /// @param setIndex the MarkGlyphSet index from the lookup
    /// @return whether the glyph is in the set
    public boolean inMarkSet(int glyphId, int setIndex) {
        if (setIndex < 0 || setIndex >= markSets.length) {
            return false;
        }
        return Arrays.binarySearch(markSets[setIndex], glyphId) >= 0;
    }

    /// Returns whether a lookup with `lookupFlag` skips `glyphId`.
    ///
    /// `IgnoreMarks` skips every class-3 mark. `MarkAttachmentType` skips marks whose attach
    /// class differs from the high byte. `UseMarkFilteringSet` skips marks absent from
    /// `markSetIndex`. Base and ligature skips consult class 1 and 2.
    ///
    /// @param glyphId the glyph
    /// @param lookupFlag the OpenType lookup flag
    /// @param markSetIndex the mark-filter set, ignored unless `UseMarkFilteringSet` is set
    /// @return whether the glyph is skipped
    public boolean skip(int glyphId, int lookupFlag, int markSetIndex) {
        int glyphClassValue = glyphClass.classOf(glyphId);
        if ((lookupFlag & FLAG_IGNORE_BASE) != 0 && glyphClassValue == CLASS_BASE) {
            return true;
        }
        if ((lookupFlag & FLAG_IGNORE_LIGATURE) != 0 && glyphClassValue == CLASS_LIGATURE) {
            return true;
        }
        if (glyphClassValue != CLASS_MARK) {
            return false;
        }
        if ((lookupFlag & FLAG_IGNORE_MARKS) != 0) {
            return true;
        }
        int attachType = (lookupFlag >>> 8) & 0xFF;
        if (attachType != 0 && markAttach.classOf(glyphId) != attachType) {
            return true;
        }
        return (lookupFlag & FLAG_MARK_FILTER) != 0 && !inMarkSet(glyphId, markSetIndex);
    }

    /// Returns the index of the first glyph in `[start, end)` that `lookupFlag` keeps, or `-1`.
    ///
    /// @param glyphIds the mapped glyphs
    /// @param start the first index, inclusive
    /// @param end the last index, exclusive
    /// @param lookupFlag the OpenType lookup flag
    /// @param markSetIndex the mark-filter set
    /// @return the kept index, or `-1`
    public int firstKeptIndex(int[] glyphIds, int start, int end, int lookupFlag, int markSetIndex) {
        for (int index = start; index < end; index++) {
            if (!skip(glyphIds[index], lookupFlag, markSetIndex)) {
                return index;
            }
        }
        return -1;
    }

    /// Returns the index of the last kept glyph at or before `fromInclusive`, or `-1`.
    ///
    /// @param glyphIds the mapped glyphs
    /// @param fromInclusive the first index to inspect, walking toward `0`
    /// @param lookupFlag the OpenType lookup flag
    /// @param markSetIndex the mark-filter set
    /// @return the kept index, or `-1`
    public int prevKeptIndex(int[] glyphIds, int fromInclusive, int lookupFlag, int markSetIndex) {
        for (int index = fromInclusive; index >= 0; index--) {
            if (!skip(glyphIds[index], lookupFlag, markSetIndex)) {
                return index;
            }
        }
        return -1;
    }

    /// Stores one ClassDef format 1 or 2.
    private static final class ClassDef {
        /// Empty class map.
        private static final ClassDef EMPTY = new ClassDef(new int[0], new int[0], new int[0]);

        /// Range starts, or format-1 start followed by implicit consecutive ids.
        private final int[] starts;

        /// Range ends, inclusive.
        private final int[] ends;

        /// Class values parallel to [`#starts`].
        private final int[] classes;

        /// Creates a class map.
        private ClassDef(int[] starts, int[] ends, int[] classes) {
            this.starts = starts;
            this.ends = ends;
            this.classes = classes;
        }

        /// Reads one ClassDef.
        private static ClassDef read(ByteBuffer buffer, int offset) {
            if (offset + 4 > buffer.limit()) {
                return EMPTY;
            }
            int saved = buffer.position();
            buffer.position(offset);
            int format = Short.toUnsignedInt(buffer.getShort());
            if (format == 1) {
                if (buffer.remaining() < 4) {
                    buffer.position(saved);
                    return EMPTY;
                }
                int start = Short.toUnsignedInt(buffer.getShort());
                int count = Short.toUnsignedInt(buffer.getShort());
                int[] starts = new int[count];
                int[] ends = new int[count];
                int[] classes = new int[count];
                for (int index = 0; index < count; index++) {
                    if (buffer.remaining() < 2) {
                        break;
                    }
                    starts[index] = start + index;
                    ends[index] = start + index;
                    classes[index] = Short.toUnsignedInt(buffer.getShort());
                }
                buffer.position(saved);
                return new ClassDef(starts, ends, classes);
            }
            if (format != 2) {
                buffer.position(saved);
                return EMPTY;
            }
            int count = Short.toUnsignedInt(buffer.getShort());
            int[] starts = new int[count];
            int[] ends = new int[count];
            int[] classes = new int[count];
            for (int index = 0; index < count; index++) {
                if (buffer.remaining() < 6) {
                    break;
                }
                starts[index] = Short.toUnsignedInt(buffer.getShort());
                ends[index] = Short.toUnsignedInt(buffer.getShort());
                classes[index] = Short.toUnsignedInt(buffer.getShort());
            }
            buffer.position(saved);
            return new ClassDef(starts, ends, classes);
        }

        /// Returns the class, or `0`.
        private int classOf(int glyphId) {
            for (int index = 0; index < starts.length; index++) {
                if (glyphId >= starts[index] && glyphId <= ends[index]) {
                    return classes[index];
                }
            }
            return 0;
        }
    }
}
