package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// CIE XYZ white point with `Y = 1`.
///
/// @param x the X tristimulus
/// @param y the Y tristimulus
/// @param z the Z tristimulus
@NotNullByDefault
public record WhitePoint(float x, float y, float z) {
    /// CIE illuminant D65.
    public static final WhitePoint D65 = new WhitePoint(
            ChromaticAdaptation.D65_X,
            ChromaticAdaptation.D65_Y,
            ChromaticAdaptation.D65_Z
    );

    /// CIE illuminant D50.
    public static final WhitePoint D50 = new WhitePoint(
            ChromaticAdaptation.D50_X,
            ChromaticAdaptation.D50_Y,
            ChromaticAdaptation.D50_Z
    );

    /// CIE illuminant A.
    public static final WhitePoint A = new WhitePoint(
            ChromaticAdaptation.A_X,
            ChromaticAdaptation.A_Y,
            ChromaticAdaptation.A_Z
    );

    /// Validates finite tristimulus values.
    public WhitePoint {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("White-point XYZ must be finite");
        }
    }
}
