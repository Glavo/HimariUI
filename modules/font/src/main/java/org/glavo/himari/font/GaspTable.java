package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Reads an OpenType `gasp` table for unhinted grayscale decisions.
///
/// First-stable honors `GASP_DOGRAY` and `GASP_SYMMETRIC_SMOOTHING` without a hinting VM.
/// `GASP_GRIDFIT` bits are recorded but do not run bytecode. A missing table allows grayscale
/// at every ppem, matching the unhinted default.
@NotNullByDefault
public final class GaspTable {
    /// Empty table used when `gasp` is absent.
    public static final GaspTable NONE = new GaspTable(new int[0], new int[0]);

    /// Grid-fit flag. Recorded only; first-stable does not run a bytecode VM.
    public static final int GRIDFIT = 0x0001;

    /// Grayscale anti-aliasing flag.
    public static final int DOGRAY = 0x0002;

    /// Symmetric ClearType grid-fit flag.
    public static final int SYMMETRIC_GRIDFIT = 0x0004;

    /// Symmetric ClearType smoothing flag, treated as grayscale permission.
    public static final int SYMMETRIC_SMOOTHING = 0x0008;

    /// Inclusive maximum ppem for each range, in table order.
    private final int[] maxPpems;

    /// Behavior flags parallel to [`#maxPpems`].
    private final int[] flags;

    /// Creates a table.
    ///
    /// @param maxPpems the range maxima
    /// @param flags the range flags
    private GaspTable(int[] maxPpems, int[] flags) {
        this.maxPpems = maxPpems;
        this.flags = flags;
    }

    /// Parses a `gasp` table, or returns [`#NONE`].
    ///
    /// @param table the table bytes, or `null`
    /// @return the table
    static GaspTable parse(@Nullable ByteBuffer table) {
        if (table == null || table.remaining() < 4) {
            return NONE;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        buffer.getShort();
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count < 1 || buffer.remaining() < count * 4L) {
            return NONE;
        }
        int[] maxPpems = new int[count];
        int[] flags = new int[count];
        for (int index = 0; index < count; index++) {
            maxPpems[index] = Short.toUnsignedInt(buffer.getShort());
            flags[index] = Short.toUnsignedInt(buffer.getShort());
        }
        return new GaspTable(maxPpems, flags);
    }

    /// Returns the flags for `ppem`.
    ///
    /// A missing table reports [`#DOGRAY`]. An out-of-range ppem uses the last range.
    ///
    /// @param ppem the destination pixels-per-em
    /// @return the behavior flags
    public int flagsAt(int ppem) {
        if (maxPpems.length == 0) {
            return DOGRAY;
        }
        for (int index = 0; index < maxPpems.length; index++) {
            if (ppem <= maxPpems[index]) {
                return flags[index];
            }
        }
        return flags[flags.length - 1];
    }

    /// Returns whether unhinted grayscale is permitted at `ppem`.
    ///
    /// @param ppem the destination pixels-per-em
    /// @return whether grayscale coverage may be produced
    public boolean allowsGrayscale(int ppem) {
        return (flagsAt(ppem) & (DOGRAY | SYMMETRIC_SMOOTHING)) != 0;
    }

    /// Returns whether vertical-only grid fitting is requested at `ppem`.
    ///
    /// First-stable honors this without a hinting VM by snapping the unhinted
    /// outline bounding box to the destination pixel grid.
    ///
    /// @param ppem the destination pixels-per-em
    /// @return whether grid fitting is requested
    public boolean gridFits(int ppem) {
        return (flagsAt(ppem) & (GRIDFIT | SYMMETRIC_GRIDFIT)) != 0;
    }
}
