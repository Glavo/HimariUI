package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// Display or content luminance range in nits.
///
/// @param minNits the nonnegative minimum luminance
/// @param maxNits the maximum luminance, at least [`#minNits`]
/// @param referenceWhiteNits the reference white, inside `[minNits, maxNits]`
@NotNullByDefault
public record LuminanceRange(float minNits, float maxNits, float referenceWhiteNits) {
    /// First-stable SDR range with a 100-nit reference white.
    public static final LuminanceRange SDR = new LuminanceRange(0.0f, 100.0f, 100.0f);

    /// Validates a finite ordered range.
    public LuminanceRange {
        if (!Float.isFinite(minNits) || !Float.isFinite(maxNits) || !Float.isFinite(referenceWhiteNits)) {
            throw new IllegalArgumentException("Luminance values must be finite");
        }
        if (minNits < 0.0f || maxNits < minNits
                || referenceWhiteNits < minNits || referenceWhiteNits > maxNits) {
            throw new IllegalArgumentException("Luminance range must be ordered and nonnegative");
        }
    }
}
