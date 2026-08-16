package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one tagged color value.
///
/// Components are finite. Non-extended encodings must lie in `[0, 1]`. Alpha is linear coverage
/// in `[0, 1]` for every encoding.
///
/// @param encoding the tagged encoding
/// @param red the red primary
/// @param green the green primary
/// @param blue the blue primary
/// @param alpha the linear coverage
@NotNullByDefault
public record Color(ColorEncoding encoding, float red, float green, float blue, float alpha) {
    /// Opaque sRGB black.
    public static final Color SRGB_BLACK = srgb(0.0f, 0.0f, 0.0f, 1.0f);

    /// Opaque sRGB white.
    public static final Color SRGB_WHITE = srgb(1.0f, 1.0f, 1.0f, 1.0f);

    /// Validates the color.
    public Color {
        Objects.requireNonNull(encoding, "encoding");
        if (!Float.isFinite(red) || !Float.isFinite(green) || !Float.isFinite(blue) || !Float.isFinite(alpha)) {
            throw new IllegalArgumentException("Color components must be finite");
        }
        if (alpha < 0.0f || alpha > 1.0f) {
            throw new IllegalArgumentException("Alpha must be in [0, 1]");
        }
        if (encoding != ColorEncoding.EXTENDED_LINEAR
                && (outsideUnit(red) || outsideUnit(green) || outsideUnit(blue))) {
            throw new IllegalArgumentException(encoding + " components must be in [0, 1]");
        }
    }

    /// Creates an sRGB color.
    ///
    /// @param red the red component
    /// @param green the green component
    /// @param blue the blue component
    /// @param alpha the alpha
    /// @return the color
    public static Color srgb(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.SRGB, red, green, blue, alpha);
    }

    /// Creates an extended-linear color.
    ///
    /// @param red the red component
    /// @param green the green component
    /// @param blue the blue component
    /// @param alpha the alpha
    /// @return the color
    public static Color extendedLinear(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.EXTENDED_LINEAR, red, green, blue, alpha);
    }

    /// Converts this color to encoded sRGB, clipping extended-linear components into `[0, 1]`.
    ///
    /// Display-P3 values are converted through the extended-linear working encoding so out-of-sRGB
    /// primaries are clipped only at this SDR encoding step.
    ///
    /// @return the sRGB color
    public Color toSrgb() {
        return switch (encoding) {
            case SRGB -> this;
            case LINEAR_SRGB, EXTENDED_LINEAR, DISPLAY_P3, LINEAR_DISPLAY_P3,
                 BT2020, LINEAR_BT2020, BT2100_PQ, BT2100_HLG -> {
                Color linear = toExtendedLinear();
                yield srgb(
                        encodeSrgb(clamp01(linear.red)),
                        encodeSrgb(clamp01(linear.green)),
                        encodeSrgb(clamp01(linear.blue)),
                        linear.alpha
                );
            }
        };
    }

    /// Converts this color to extended-linear sRGB working values.
    ///
    /// Display-P3 and BT.2020 conversions keep out-of-sRGB components. They are not clipped here.
    /// BT.2100 PQ is decoded to nits and divided by 100-nit reference white. BT.2100 HLG uses the
    /// inverse OETF; scene-linear `1.0` is the HLG system peak.
    ///
    /// @return the extended-linear color
    public Color toExtendedLinear() {
        return switch (encoding) {
            case EXTENDED_LINEAR -> this;
            case LINEAR_SRGB -> extendedLinear(red, green, blue, alpha);
            case SRGB -> extendedLinear(decodeSrgb(red), decodeSrgb(green), decodeSrgb(blue), alpha);
            case LINEAR_DISPLAY_P3 -> p3LinearToExtended(red, green, blue, alpha);
            case DISPLAY_P3 -> p3LinearToExtended(decodeSrgb(red), decodeSrgb(green), decodeSrgb(blue), alpha);
            case LINEAR_BT2020 -> bt2020LinearToExtended(red, green, blue, alpha);
            case BT2020 -> bt2020LinearToExtended(decodeBt2020(red), decodeBt2020(green), decodeBt2020(blue), alpha);
            case BT2100_PQ -> bt2020LinearToExtended(
                    decodePq(red) / PQ_REFERENCE_WHITE_NITS,
                    decodePq(green) / PQ_REFERENCE_WHITE_NITS,
                    decodePq(blue) / PQ_REFERENCE_WHITE_NITS,
                    alpha
            );
            case BT2100_HLG -> bt2020LinearToExtended(decodeHlg(red), decodeHlg(green), decodeHlg(blue), alpha);
        };
    }

    /// Returns packed 8-bit sRGB with unassociated alpha.
    ///
    /// @return `0xAARRGGBB`
    public int toSrgbArgb8() {
        Color srgb = toSrgb();
        return (Math.round(srgb.alpha * 255.0f) << 24)
                | (Math.round(srgb.red * 255.0f) << 16)
                | (Math.round(srgb.green * 255.0f) << 8)
                | Math.round(srgb.blue * 255.0f);
    }

    /// PQ reference white used when converting BT.2100 PQ into extended-linear.
    static final float PQ_REFERENCE_WHITE_NITS = 100.0f;

    /// Converts linear Display-P3 D65 into extended-linear sRGB.
    ///
    /// @param red the linear P3 red
    /// @param green the linear P3 green
    /// @param blue the linear P3 blue
    /// @param alpha the alpha
    /// @return the extended-linear color
    private static Color p3LinearToExtended(float red, float green, float blue, float alpha) {
        return extendedLinear(
                1.224940176f * red + -0.224940176f * green,
                -0.042056955f * red + 1.042056955f * green,
                -0.019637555f * red + -0.078636046f * green + 1.098273601f * blue,
                alpha
        );
    }

    /// Converts linear BT.2020 D65 into extended-linear sRGB through CIE XYZ.
    ///
    /// @param red the linear BT.2020 red
    /// @param green the linear BT.2020 green
    /// @param blue the linear BT.2020 blue
    /// @param alpha the alpha
    /// @return the extended-linear color
    private static Color bt2020LinearToExtended(float red, float green, float blue, float alpha) {
        float x = 0.6369580483f * red + 0.1446169036f * green + 0.1688809752f * blue;
        float y = 0.2627002120f * red + 0.6779980716f * green + 0.0593017165f * blue;
        float z = 0.0280726930f * green + 1.0609850577f * blue;
        return xyzD65ToExtended(x, y, z, alpha);
    }

    /// Converts CIE XYZ D65 into extended-linear sRGB.
    ///
    /// @param x the X tristimulus
    /// @param y the Y tristimulus
    /// @param z the Z tristimulus
    /// @param alpha the alpha
    /// @return the extended-linear color
    static Color xyzD65ToExtended(float x, float y, float z, float alpha) {
        return extendedLinear(
                3.2409699419f * x + -1.5373831776f * y + -0.4986107603f * z,
                -0.9692436363f * x + 1.8759675015f * y + 0.0415550574f * z,
                0.0556300797f * x + -0.2039769589f * y + 1.0569715142f * z,
                alpha
        );
    }

    /// Converts extended-linear sRGB into CIE XYZ D65.
    ///
    /// @param red the linear red
    /// @param green the linear green
    /// @param blue the linear blue
    /// @return `{X, Y, Z}`
    static float[] extendedToXyzD65(float red, float green, float blue) {
        return new float[] {
            0.4123907993f * red + 0.3575843394f * green + 0.1804807884f * blue,
            0.2126390059f * red + 0.7151686788f * green + 0.0721923154f * blue,
            0.0193308187f * red + 0.1191947798f * green + 0.9505321522f * blue
        };
    }

    /// Decodes one BT.2020/BT.709 OETF component.
    ///
    /// @param encoded the encoded component
    /// @return the linear component
    static float decodeBt2020(float encoded) {
        if (encoded < 0.0812428583f) {
            return encoded / 4.5f;
        }
        return (float) Math.pow((encoded + 0.0992968268f) / 1.0992968268f, 1.0 / 0.45);
    }

    /// Encodes one linear component with the BT.2020/BT.709 OETF.
    ///
    /// @param linear the linear component
    /// @return the encoded component
    static float encodeBt2020(float linear) {
        if (linear < 0.0180539685f) {
            return linear * 4.5f;
        }
        return Math.fma(1.0992968268f, (float) Math.pow(linear, 0.45), -0.0992968268f);
    }

    /// Decodes one BT.2100 PQ component to absolute nits.
    ///
    /// @param encoded the PQ-encoded component
    /// @return the luminance in nits
    static float decodePq(float encoded) {
        if (encoded <= 0.0f) {
            return 0.0f;
        }
        double n = Math.pow(encoded, 1.0 / 78.84375);
        double numerator = Math.max(n - 0.8359375, 0.0);
        double denominator = 18.8515625 - 18.6875 * n;
        if (denominator <= 0.0) {
            return 0.0f;
        }
        return (float) (10_000.0 * Math.pow(numerator / denominator, 1.0 / 0.1593017578125));
    }

    /// Encodes an absolute luminance in nits as BT.2100 PQ.
    ///
    /// @param nits the luminance in nits
    /// @return the PQ-encoded component
    static float encodePq(float nits) {
        double y = Math.clamp(nits / 10_000.0, 0.0, 1.0);
        double n = Math.pow(y, 0.1593017578125);
        return (float) Math.pow((0.8359375 + 18.8515625 * n) / (1.0 + 18.6875 * n), 78.84375);
    }

    /// Applies the BT.2100 HLG inverse OETF.
    ///
    /// Scene-linear `1.0` is the HLG system peak, not SDR reference white.
    ///
    /// @param encoded the HLG-encoded component
    /// @return the scene-linear component
    static float decodeHlg(float encoded) {
        if (encoded <= 0.5f) {
            return (encoded * encoded) / 3.0f;
        }
        return (float) ((Math.exp((encoded - 0.5599107295) / 0.17883277) + 0.28466892) / 12.0);
    }

    /// Encodes one linear sRGB component.
    ///
    /// @param linear the linear component
    /// @return the encoded component
    static float encodeSrgb(float linear) {
        if (linear <= 0.0031308f) {
            return linear * 12.92f;
        }
        return Math.fma(1.055f, (float) Math.pow(linear, 1.0 / 2.4), -0.055f);
    }

    /// Decodes one encoded sRGB component.
    ///
    /// @param encoded the encoded component
    /// @return the linear component
    static float decodeSrgb(float encoded) {
        if (encoded <= 0.04045f) {
            return encoded / 12.92f;
        }
        return (float) Math.pow((encoded + 0.055f) / 1.055f, 2.4);
    }

    /// Returns whether a component is outside `[0, 1]`.
    ///
    /// @param value the component
    /// @return whether the value is outside the unit interval
    private static boolean outsideUnit(float value) {
        return value < 0.0f || value > 1.0f;
    }

    /// Clamps a component into `[0, 1]`.
    ///
    /// @param value the component
    /// @return the clamped value
    private static float clamp01(float value) {
        return Math.clamp(value, 0.0f, 1.0f);
    }
}
