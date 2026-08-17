package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// Chromatically adapts CIE XYZ between illuminants.
///
/// Bradford is the M3 D50/D65 baseline already used by [`IccProfile`]. CAT02 is the additional
/// first-stable method required by `COLOR-REF-001` beyond that baseline. Both methods scale
/// cone responses and invert the same published matrices.
@NotNullByDefault
public final class ChromaticAdaptation {
    /// CIE XYZ of illuminant D65 with `Y = 1`.
    public static final float D65_X = 0.95047f;

    /// CIE XYZ of illuminant D65 with `Y = 1`.
    public static final float D65_Y = 1.0f;

    /// CIE XYZ of illuminant D65 with `Y = 1`.
    public static final float D65_Z = 1.08883f;

    /// CIE XYZ of illuminant D50 with `Y = 1`.
    public static final float D50_X = 0.96422f;

    /// CIE XYZ of illuminant D50 with `Y = 1`.
    public static final float D50_Y = 1.0f;

    /// CIE XYZ of illuminant D50 with `Y = 1`.
    public static final float D50_Z = 0.82521f;

    /// CIE XYZ of illuminant A with `Y = 1`.
    public static final float A_X = 1.09850f;

    /// CIE XYZ of illuminant A with `Y = 1`.
    public static final float A_Y = 1.0f;

    /// CIE XYZ of illuminant A with `Y = 1`.
    public static final float A_Z = 0.35585f;

    /// Prevents instantiation.
    private ChromaticAdaptation() {
    }

    /// Adapts `x,y,z` from one white point to another with Bradford.
    ///
    /// @param x the source X
    /// @param y the source Y
    /// @param z the source Z
    /// @param sourceX the source white X
    /// @param sourceY the source white Y
    /// @param sourceZ the source white Z
    /// @param destinationX the destination white X
    /// @param destinationY the destination white Y
    /// @param destinationZ the destination white Z
    /// @return `{X, Y, Z}` in the destination white
    public static float[] bradford(
            float x,
            float y,
            float z,
            float sourceX,
            float sourceY,
            float sourceZ,
            float destinationX,
            float destinationY,
            float destinationZ
    ) {
        return adapt(
                x,
                y,
                z,
                sourceX,
                sourceY,
                sourceZ,
                destinationX,
                destinationY,
                destinationZ,
                0.8951f,
                0.2664f,
                -0.1614f,
                -0.7502f,
                1.7135f,
                0.0367f,
                0.0389f,
                -0.0685f,
                1.0296f,
                0.9869929f,
                -0.1470543f,
                0.1599627f,
                0.4323053f,
                0.5183603f,
                0.0492912f,
                -0.0085287f,
                0.0400428f,
                0.9684867f
        );
    }

    /// Adapts `x,y,z` from one white point to another with CAT02.
    ///
    /// @param x the source X
    /// @param y the source Y
    /// @param z the source Z
    /// @param sourceX the source white X
    /// @param sourceY the source white Y
    /// @param sourceZ the source white Z
    /// @param destinationX the destination white X
    /// @param destinationY the destination white Y
    /// @param destinationZ the destination white Z
    /// @return `{X, Y, Z}` in the destination white
    public static float[] cat02(
            float x,
            float y,
            float z,
            float sourceX,
            float sourceY,
            float sourceZ,
            float destinationX,
            float destinationY,
            float destinationZ
    ) {
        return adapt(
                x,
                y,
                z,
                sourceX,
                sourceY,
                sourceZ,
                destinationX,
                destinationY,
                destinationZ,
                0.7328f,
                0.4296f,
                -0.1624f,
                -0.7036f,
                1.6975f,
                0.0061f,
                0.0030f,
                0.0136f,
                0.9834f,
                1.0961238f,
                -0.2788690f,
                0.1827452f,
                0.4543690f,
                0.4735332f,
                0.0720978f,
                -0.0096276f,
                -0.0056980f,
                1.0153255f
        );
    }

    /// Scales cone responses and inverts `forward`.
    private static float[] adapt(
            float x,
            float y,
            float z,
            float sourceX,
            float sourceY,
            float sourceZ,
            float destinationX,
            float destinationY,
            float destinationZ,
            float f00,
            float f01,
            float f02,
            float f10,
            float f11,
            float f12,
            float f20,
            float f21,
            float f22,
            float i00,
            float i01,
            float i02,
            float i10,
            float i11,
            float i12,
            float i20,
            float i21,
            float i22
    ) {
        requireFinite(x, y, z, "XYZ");
        requireFinite(sourceX, sourceY, sourceZ, "source white");
        requireFinite(destinationX, destinationY, destinationZ, "destination white");
        float[] source = cone(sourceX, sourceY, sourceZ, f00, f01, f02, f10, f11, f12, f20, f21, f22);
        float[] destination = cone(
                destinationX,
                destinationY,
                destinationZ,
                f00,
                f01,
                f02,
                f10,
                f11,
                f12,
                f20,
                f21,
                f22
        );
        float[] response = cone(x, y, z, f00, f01, f02, f10, f11, f12, f20, f21, f22);
        if (source[0] == 0.0f || source[1] == 0.0f || source[2] == 0.0f) {
            throw new IllegalArgumentException("source white must have nonzero cone responses");
        }
        response[0] *= destination[0] / source[0];
        response[1] *= destination[1] / source[1];
        response[2] *= destination[2] / source[2];
        return new float[] {
                i00 * response[0] + i01 * response[1] + i02 * response[2],
                i10 * response[0] + i11 * response[1] + i12 * response[2],
                i20 * response[0] + i21 * response[1] + i22 * response[2]
        };
    }

    /// Applies one 3×3 cone matrix.
    private static float[] cone(
            float x,
            float y,
            float z,
            float m00,
            float m01,
            float m02,
            float m10,
            float m11,
            float m12,
            float m20,
            float m21,
            float m22
    ) {
        return new float[] {
                m00 * x + m01 * y + m02 * z,
                m10 * x + m11 * y + m12 * z,
                m20 * x + m21 * y + m22 * z
        };
    }

    /// Rejects non-finite tristimulus values.
    private static void requireFinite(float x, float y, float z, String label) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
    }
}
