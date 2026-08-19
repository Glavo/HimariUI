package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores one `STAT` design axis.
///
/// @param tag the four-byte axis tag
/// @param nameId the `name` table ID for the axis name
/// @param ordering the recommended axis-ordering index
@NotNullByDefault
public record StatAxis(int tag, int nameId, int ordering) {
    /// Validates the name identity.
    public StatAxis {
        if (nameId < 0 || ordering < 0) {
            throw new IllegalArgumentException("STAT axis nameId and ordering must be nonnegative");
        }
    }

    /// Returns the tag as four ASCII bytes when they are printable.
    ///
    /// @return the tag string
    public String tagString() {
        return new String(new char[] {
                (char) ((tag >>> 24) & 0xFF),
                (char) ((tag >>> 16) & 0xFF),
                (char) ((tag >>> 8) & 0xFF),
                (char) (tag & 0xFF)
        });
    }
}
