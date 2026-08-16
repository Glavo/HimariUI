package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

/// Selects how leftover advance is assigned after wrapping.
@NotNullByDefault
public enum LineAlignment {
    /// Keep shaped advances. Leftover width stays after the last glyph.
    START,

    /// Distribute leftover width onto U+0020 on every non-last paragraph line.
    ///
    /// The last line of a hard-broken paragraph, a line with no space, and a line that already
    /// fills `maxWidth` stay unchanged. Extra units are split as evenly as integer advances allow;
    /// the remainder is added one unit at a time from the first space.
    JUSTIFY
}
