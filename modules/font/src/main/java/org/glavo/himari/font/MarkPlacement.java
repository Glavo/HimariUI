package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

/// Stores a mark-to-base placement in font units.
///
/// @param xOffset the signed X offset applied to the mark
/// @param yOffset the signed Y offset applied to the mark
@NotNullByDefault
public record MarkPlacement(int xOffset, int yOffset) {
}
