package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.ByteBuffer;
import java.util.Arrays;

/// Reads an OpenType ClassDef format 1 or 2.
///
/// Unassigned glyphs report class `0`. Assigned glyphs are enumerated so a format-2 pair matrix
/// can expand every class member.
@NotNullByDefault
final class ClassMap {
    /// Empty map used when a ClassDef offset is missing or truncated.
    static final ClassMap EMPTY = new ClassMap(new int[0], new int[0], new int[0]);

    /// Inclusive range starts, or format-1 consecutive ids.
    private final int[] starts;

    /// Inclusive range ends.
    private final int[] ends;

    /// Class values parallel to [`#starts`].
    private final int[] classes;

    /// Creates a class map.
    ///
    /// @param starts the range starts
    /// @param ends the range ends
    /// @param classes the class values
    private ClassMap(int[] starts, int[] ends, int[] classes) {
        this.starts = starts;
        this.ends = ends;
        this.classes = classes;
    }

    /// Reads one ClassDef at `offset`.
    ///
    /// @param buffer the table buffer
    /// @param offset the ClassDef offset
    /// @return the map, or [`#EMPTY`]
    static ClassMap read(ByteBuffer buffer, int offset) {
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
            return new ClassMap(starts, ends, classes);
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
        return new ClassMap(starts, ends, classes);
    }

    /// Returns the class, or `0` when unassigned.
    ///
    /// @param glyphId the glyph
    /// @return the class
    int classOf(int glyphId) {
        for (int index = 0; index < starts.length; index++) {
            if (glyphId >= starts[index] && glyphId <= ends[index]) {
                return classes[index];
            }
        }
        return 0;
    }

    /// Returns every glyph assigned `classValue`.
    ///
    /// Class `0` is the implicit default and is not enumerated.
    ///
    /// @param classValue the class
    /// @return the glyphs, possibly empty
    int[] glyphsOf(int classValue) {
        if (classValue == 0) {
            return new int[0];
        }
        int total = 0;
        for (int index = 0; index < starts.length; index++) {
            if (classes[index] != classValue) {
                continue;
            }
            total += Math.max(0, ends[index] - starts[index] + 1);
        }
        int[] glyphs = new int[total];
        int written = 0;
        for (int index = 0; index < starts.length; index++) {
            if (classes[index] != classValue) {
                continue;
            }
            for (int glyph = starts[index]; glyph <= ends[index]; glyph++) {
                glyphs[written++] = glyph;
            }
        }
        return written == glyphs.length ? glyphs : Arrays.copyOf(glyphs, written);
    }

    /// Returns every glyph assigned a non-zero class, in table order.
    ///
    /// @return the glyphs, possibly empty
    int[] assignedGlyphs() {
        int total = 0;
        for (int index = 0; index < starts.length; index++) {
            if (classes[index] == 0) {
                continue;
            }
            total += Math.max(0, ends[index] - starts[index] + 1);
        }
        int[] glyphs = new int[total];
        int written = 0;
        for (int index = 0; index < starts.length; index++) {
            if (classes[index] == 0) {
                continue;
            }
            for (int glyph = starts[index]; glyph <= ends[index]; glyph++) {
                glyphs[written++] = glyph;
            }
        }
        return written == glyphs.length ? glyphs : Arrays.copyOf(glyphs, written);
    }
}
