package org.glavo.himari.layout;

import org.jetbrains.annotations.NotNullByDefault;

/// First-class alignment-line distances published after measure.
///
/// @param baseline the distance from the top edge to the text baseline
/// @param centerX the distance from the left edge to the horizontal center
/// @param centerY the distance from the top edge to the vertical center
@NotNullByDefault
public record AlignmentLines(float baseline, float centerX, float centerY) {
    /// Empty lines before the first measure.
    public static final AlignmentLines ZERO = new AlignmentLines(0.0f, 0.0f, 0.0f);

    /// Validates the lines.
    public AlignmentLines {
        if (!Float.isFinite(baseline) || !Float.isFinite(centerX) || !Float.isFinite(centerY)
                || baseline < 0.0f || centerX < 0.0f || centerY < 0.0f) {
            throw new IllegalArgumentException("Alignment lines must be finite and nonnegative");
        }
    }
}
