package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Reads a first-stable OpenType `post` header.
///
/// Version 2 and 3 share the same 32-byte header. A missing or truncated table reports a zero
/// italic angle, zero underline metrics, and a proportional face.
@NotNullByDefault
public final class PostTable {
    /// Shared empty table used when `post` is absent.
    static final PostTable EMPTY = new PostTable(0, 0, 0, 0);

    /// `italicAngle` as a 16.16 fixed value.
    private final int italicAngleFixed;

    /// `underlinePosition` in font units.
    private final int underlinePosition;

    /// `underlineThickness` in font units.
    private final int underlineThickness;

    /// `isFixedPitch` stored as a 32-bit flag.
    private final int fixedPitch;

    /// Creates a table.
    ///
    /// @param italicAngleFixed the 16.16 italic angle
    /// @param underlinePosition the underline position
    /// @param underlineThickness the underline thickness
    /// @param fixedPitch the pitch flag
    private PostTable(int italicAngleFixed, int underlinePosition, int underlineThickness, int fixedPitch) {
        this.italicAngleFixed = italicAngleFixed;
        this.underlinePosition = underlinePosition;
        this.underlineThickness = underlineThickness;
        this.fixedPitch = fixedPitch;
    }

    /// Parses a `post` table, or returns [`#EMPTY`].
    ///
    /// @param table the table bytes, or `null`
    /// @return the table
    static PostTable parse(@Nullable ByteBuffer table) {
        if (table == null || table.remaining() < 16) {
            return EMPTY;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        buffer.getInt();
        int italicAngleFixed = buffer.getInt();
        int underlinePosition = buffer.getShort();
        int underlineThickness = buffer.getShort();
        int fixedPitch = buffer.getInt();
        return new PostTable(italicAngleFixed, underlinePosition, underlineThickness, fixedPitch);
    }

    /// Returns the italic angle in degrees.
    ///
    /// @return `italicAngle` as a float
    public float italicAngle() {
        return italicAngleFixed / 65536.0f;
    }

    /// Returns `underlinePosition` in font units.
    ///
    /// @return the underline position
    public int underlinePosition() {
        return underlinePosition;
    }

    /// Returns `underlineThickness` in font units.
    ///
    /// @return the underline thickness
    public int underlineThickness() {
        return underlineThickness;
    }

    /// Returns whether the face is monospaced.
    ///
    /// @return whether `isFixedPitch` is nonzero
    public boolean fixedPitch() {
        return fixedPitch != 0;
    }
}
