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

    /// Creates a linear BT.2020 color.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @param alpha the alpha
    /// @return the color
    public static Color linearBt2020(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.LINEAR_BT2020, red, green, blue, alpha);
    }

    /// Creates an encoded BT.2020 color.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @param alpha the alpha
    /// @return the color
    public static Color bt2020(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.BT2020, red, green, blue, alpha);
    }

    /// Creates a BT.2100 PQ color.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @param alpha the alpha
    /// @return the color
    public static Color bt2100Pq(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.BT2100_PQ, red, green, blue, alpha);
    }

    /// Creates a BT.2100 HLG color.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @param alpha the alpha
    /// @return the color
    public static Color bt2100Hlg(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.BT2100_HLG, red, green, blue, alpha);
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
                 BT2020, LINEAR_BT2020, BT2100_PQ, BT2100_HLG, A98, LINEAR_A98,
                 PROPHOTO, LINEAR_PROPHOTO, BT709, LINEAR_BT709 -> {
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
            case LINEAR_A98 -> a98LinearToExtended(red, green, blue, alpha);
            case A98 -> a98LinearToExtended(decodeA98(red), decodeA98(green), decodeA98(blue), alpha);
            case LINEAR_PROPHOTO -> proPhotoLinearToExtended(red, green, blue, alpha);
            case PROPHOTO -> proPhotoLinearToExtended(
                    decodeProPhoto(red),
                    decodeProPhoto(green),
                    decodeProPhoto(blue),
                    alpha
            );
            case LINEAR_BT709 -> extendedLinear(red, green, blue, alpha);
            case BT709 -> extendedLinear(decodeBt2020(red), decodeBt2020(green), decodeBt2020(blue), alpha);
        };
    }

    /// Converts this color to linear BT.2020, clipping each primary into `[0, 1]`.
    ///
    /// @return the linear BT.2020 color
    public Color toLinearBt2020() {
        Color linear = toExtendedLinear();
        float[] bt2020 = extendedToLinearBt2020(linear.red, linear.green, linear.blue);
        return new Color(
                ColorEncoding.LINEAR_BT2020,
                clamp01(bt2020[0]),
                clamp01(bt2020[1]),
                clamp01(bt2020[2]),
                linear.alpha
        );
    }

    /// Converts this color to encoded BT.2020, clipping each primary into `[0, 1]`.
    ///
    /// @return the encoded BT.2020 color
    public Color toBt2020() {
        Color linear = toLinearBt2020();
        return new Color(
                ColorEncoding.BT2020,
                encodeBt2020(linear.red),
                encodeBt2020(linear.green),
                encodeBt2020(linear.blue),
                linear.alpha
        );
    }

    /// Converts this color to BT.2100 PQ at 100-nit reference white.
    ///
    /// Linear BT.2020 `1.0` encodes as 100 nits. Components are clipped to the PQ domain.
    ///
    /// @return the PQ-encoded color
    public Color toBt2100Pq() {
        Color linear = toLinearBt2020();
        return new Color(
                ColorEncoding.BT2100_PQ,
                encodePq(linear.red * PQ_REFERENCE_WHITE_NITS),
                encodePq(linear.green * PQ_REFERENCE_WHITE_NITS),
                encodePq(linear.blue * PQ_REFERENCE_WHITE_NITS),
                linear.alpha
        );
    }

    /// Converts this color to BT.2100 HLG.
    ///
    /// Linear BT.2020 `1.0` is the HLG system peak. Components are clipped to `[0, 1]` before
    /// the OETF.
    ///
    /// @return the HLG-encoded color
    public Color toBt2100Hlg() {
        Color linear = toLinearBt2020();
        return new Color(
                ColorEncoding.BT2100_HLG,
                encodeHlg(linear.red),
                encodeHlg(linear.green),
                encodeHlg(linear.blue),
                linear.alpha
        );
    }

    /// Converts this color to linear Display-P3, clipping each primary into `[0, 1]`.
    ///
    /// @return the linear Display-P3 color
    /// Adobe RGB (1998) gamma `563/256`.
    private static final float A98_GAMMA = 563.0f / 256.0f;

    /// Creates a linear Adobe RGB (1998) color.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @param alpha the alpha
    /// @return the color
    public static Color linearA98(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.LINEAR_A98, red, green, blue, alpha);
    }

    /// Creates an encoded Adobe RGB (1998) color.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @param alpha the alpha
    /// @return the color
    public static Color a98(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.A98, red, green, blue, alpha);
    }

    /// Converts this color to linear Adobe RGB (1998), clipping each primary into `[0, 1]`.
    ///
    /// @return the linear A98 color
    public Color toLinearA98() {
        Color linear = toExtendedLinear();
        float[] a98 = extendedToLinearA98(linear.red, linear.green, linear.blue);
        return new Color(
                ColorEncoding.LINEAR_A98,
                clamp01(a98[0]),
                clamp01(a98[1]),
                clamp01(a98[2]),
                linear.alpha
        );
    }

    /// Converts this color to encoded Adobe RGB (1998), clipping each primary into `[0, 1]`.
    ///
    /// @return the encoded A98 color
    public Color toA98() {
        Color linear = toLinearA98();
        return new Color(
                ColorEncoding.A98,
                encodeA98(linear.red),
                encodeA98(linear.green),
                encodeA98(linear.blue),
                linear.alpha
        );
    }

    /// Converts linear Adobe RGB (1998) to extended-linear sRGB through CIE XYZ D65.
    private static Color a98LinearToExtended(float red, float green, float blue, float alpha) {
        float x = 0.5766690429f * red + 0.1855582379f * green + 0.1882286462f * blue;
        float y = 0.2973449753f * red + 0.6273635663f * green + 0.0752914583f * blue;
        float z = 0.0270313614f * red + 0.0706888525f * green + 0.9913375365f * blue;
        return xyzD65ToExtended(x, y, z, alpha);
    }

    /// Converts extended-linear sRGB to linear Adobe RGB (1998).
    private static float[] extendedToLinearA98(float red, float green, float blue) {
        float[] xyz = extendedToXyzD65(red, green, blue);
        return new float[] {
                2.0415879039f * xyz[0] + -0.5650069742f * xyz[1] + -0.3447313510f * xyz[2],
                -0.9692436364f * xyz[0] + 1.8759675014f * xyz[1] + 0.0415550578f * xyz[2],
                0.0134442806f * xyz[0] + -0.1183623922f * xyz[1] + 1.0151749947f * xyz[2]
        };
    }

    /// Encodes one linear Adobe RGB component with gamma `563/256`.
    private static float encodeA98(float linear) {
        if (linear <= 0.0f) {
            return 0.0f;
        }
        return (float) Math.pow(linear, 1.0 / A98_GAMMA);
    }

    /// Decodes one Adobe RGB component with gamma `563/256`.
    private static float decodeA98(float encoded) {
        if (encoded <= 0.0f) {
            return 0.0f;
        }
        return (float) Math.pow(encoded, A98_GAMMA);
    }

    /// ProPhoto RGB / ROMM gamma.
    private static final float PROPHOTO_GAMMA = 1.8f;

    /// Creates a linear ProPhoto RGB color.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @param alpha the alpha
    /// @return the color
    public static Color linearProPhoto(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.LINEAR_PROPHOTO, red, green, blue, alpha);
    }

    /// Creates an encoded ProPhoto RGB color.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @param alpha the alpha
    /// @return the color
    public static Color proPhoto(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.PROPHOTO, red, green, blue, alpha);
    }

    /// Converts this color to linear ProPhoto RGB, clipping each primary into `[0, 1]`.
    ///
    /// @return the linear ProPhoto color
    public Color toLinearProPhoto() {
        Color linear = toExtendedLinear();
        float[] rgb = extendedToLinearProPhoto(linear.red, linear.green, linear.blue);
        return new Color(
                ColorEncoding.LINEAR_PROPHOTO,
                clamp01(rgb[0]),
                clamp01(rgb[1]),
                clamp01(rgb[2]),
                linear.alpha
        );
    }

    /// Converts this color to encoded ProPhoto RGB, clipping each primary into `[0, 1]`.
    ///
    /// @return the encoded ProPhoto color
    public Color toProPhoto() {
        Color linear = toLinearProPhoto();
        return new Color(
                ColorEncoding.PROPHOTO,
                encodeProPhoto(linear.red),
                encodeProPhoto(linear.green),
                encodeProPhoto(linear.blue),
                linear.alpha
        );
    }

    /// Converts linear ProPhoto RGB to extended-linear sRGB through CIE XYZ D50.
    private static Color proPhotoLinearToExtended(float red, float green, float blue, float alpha) {
        float x = 0.7977604897f * red + 0.1351917482f * green + 0.0313532860f * blue;
        float y = 0.2880711282f * red + 0.7118432178f * green + 0.0000856540f * blue;
        float z = 0.8252095025f * blue;
        return fromXyzD50(x, y, z, alpha);
    }

    /// Converts extended-linear sRGB to linear ProPhoto RGB through CIE XYZ D50.
    private static float[] extendedToLinearProPhoto(float red, float green, float blue) {
        float[] xyz = extendedLinear(red, green, blue, 1.0f).toXyzD50();
        return new float[] {
                1.3458033056f * xyz[0] + -0.2555920982f * xyz[1] + -0.0511063716f * xyz[2],
                -0.5446242472f * xyz[0] + 1.5082375968f * xyz[1] + 0.0205360859f * xyz[2],
                1.2118134813f * xyz[2]
        };
    }

    /// Encodes one linear ProPhoto component with gamma 1.8.
    private static float encodeProPhoto(float linear) {
        if (linear <= 0.0f) {
            return 0.0f;
        }
        return (float) Math.pow(linear, 1.0 / PROPHOTO_GAMMA);
    }

    /// Decodes one ProPhoto component with gamma 1.8.
    private static float decodeProPhoto(float encoded) {
        if (encoded <= 0.0f) {
            return 0.0f;
        }
        return (float) Math.pow(encoded, PROPHOTO_GAMMA);
    }

    /// Creates a linear Rec.709 / BT.709 color.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @param alpha the alpha
    /// @return the color
    public static Color linearBt709(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.LINEAR_BT709, red, green, blue, alpha);
    }

    /// Creates an encoded Rec.709 / BT.709 color.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @param alpha the alpha
    /// @return the color
    public static Color bt709(float red, float green, float blue, float alpha) {
        return new Color(ColorEncoding.BT709, red, green, blue, alpha);
    }

    /// Converts this color to linear Rec.709, clipping each primary into `[0, 1]`.
    ///
    /// Rec.709 uses the same primaries as sRGB, so this is a clip of the extended-linear
    /// working encoding.
    ///
    /// @return the linear Rec.709 color
    public Color toLinearBt709() {
        Color linear = toExtendedLinear();
        return new Color(
                ColorEncoding.LINEAR_BT709,
                clamp01(linear.red),
                clamp01(linear.green),
                clamp01(linear.blue),
                linear.alpha
        );
    }

    /// Converts this color to encoded Rec.709 using the BT.709 OETF.
    ///
    /// @return the encoded Rec.709 color
    public Color toBt709() {
        Color linear = toLinearBt709();
        return new Color(
                ColorEncoding.BT709,
                encodeBt2020(linear.red),
                encodeBt2020(linear.green),
                encodeBt2020(linear.blue),
                linear.alpha
        );
    }

    /// Converts this color to linear Display-P3, clipping each primary into `[0, 1]`.
    ///
    /// @return the linear Display-P3 color
    public Color toLinearDisplayP3() {
        Color linear = toExtendedLinear();
        float[] p3 = extendedToLinearP3(linear.red, linear.green, linear.blue);
        return new Color(
                ColorEncoding.LINEAR_DISPLAY_P3,
                clamp01(p3[0]),
                clamp01(p3[1]),
                clamp01(p3[2]),
                linear.alpha
        );
    }

    /// Converts this color to encoded Display-P3, clipping each primary into `[0, 1]`.
    ///
    /// Display-P3 uses the sRGB transfer. Components are encoded after the linear P3 conversion.
    ///
    /// @return the encoded Display-P3 color
    public Color toDisplayP3() {
        Color linear = toLinearDisplayP3();
        return new Color(
                ColorEncoding.DISPLAY_P3,
                encodeSrgb(linear.red),
                encodeSrgb(linear.green),
                encodeSrgb(linear.blue),
                linear.alpha
        );
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

    /// Converts extended-linear sRGB into linear Display-P3 D65.
    ///
    /// The matrix is the inverse of [#p3LinearToExtended] published as CSS Color 4 sRGB→P3.
    ///
    /// @param red the linear sRGB red
    /// @param green the linear sRGB green
    /// @param blue the linear sRGB blue
    /// @return `{R, G, B}` in linear Display-P3
    private static float[] extendedToLinearP3(float red, float green, float blue) {
        return new float[] {
            0.822461968361196f * red + 0.177538031638804f * green,
            0.033194198630632f * red + 0.966805801369368f * green,
            0.017082630783304f * red + 0.072397440658258f * green + 0.910519928558438f * blue
        };
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

    /// Converts extended-linear sRGB into linear BT.2020 through CIE XYZ D65.
    ///
    /// @param red the linear sRGB red
    /// @param green the linear sRGB green
    /// @param blue the linear sRGB blue
    /// @return `{R, G, B}` in linear BT.2020
    private static float[] extendedToLinearBt2020(float red, float green, float blue) {
        float[] xyz = extendedToXyzD65(red, green, blue);
        return new float[] {
            1.716651187971268f * xyz[0] + -0.355670783776392f * xyz[1] + -0.253366281373660f * xyz[2],
            -0.666684351832489f * xyz[0] + 1.616481236634939f * xyz[1] + 0.015768545813911f * xyz[2],
            0.017639857445311f * xyz[0] + -0.042770613257809f * xyz[1] + 0.942103121235474f * xyz[2]
        };
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

    /// Converts this color to CIE XYZ D65 through the extended-linear working encoding.
    ///
    /// @return `{X, Y, Z}`
    public float[] toXyzD65() {
        Color linear = toExtendedLinear();
        return extendedToXyzD65(linear.red, linear.green, linear.blue);
    }

    /// Returns CIE relative luminance `Y` for D65 with `Y = 1` at the white point.
    ///
    /// @return the Y tristimulus
    public float relativeLuminance() {
        return toXyzD65()[1];
    }

    /// Returns the WCAG 2 contrast ratio against `other`.
    ///
    /// The ratio is `(L1 + 0.05) / (L2 + 0.05)` using CIE relative luminance, with `L1`
    /// the lighter of the two colors.
    ///
    /// @param other the comparison color
    /// @return the contrast ratio, at least `1`
    public float contrastRatio(Color other) {
        Objects.requireNonNull(other, "other");
        float first = relativeLuminance();
        float second = other.relativeLuminance();
        float lighter = Math.max(first, second);
        float darker = Math.min(first, second);
        return (lighter + 0.05f) / (darker + 0.05f);
    }

    /// Returns CIE76 ΔE*ab against `other` in D65 L*a*b*.
    ///
    /// @param other the comparison color
    /// @return the Euclidean Lab distance, at least `0`
    public float deltaE76(Color other) {
        Objects.requireNonNull(other, "other");
        float[] first = toCieLab();
        float[] second = other.toCieLab();
        float dL = first[0] - second[0];
        float da = first[1] - second[1];
        float db = first[2] - second[2];
        return (float) Math.sqrt(dL * dL + da * da + db * db);
    }

    /// Returns CIEDE2000 ΔE₀₀ against `other` in D65 L*a*b*.
    ///
    /// The implementation follows Sharma, Wu, and Dalal, *The CIEDE2000 Color-Difference
    /// Formula: Implementation Notes, Supplementary Test Data, and Mathematical Observations*,
    /// with `kL = kC = kH = 1`.
    ///
    /// @param other the comparison color
    /// @return the CIEDE2000 distance, at least `0`
    public float deltaE2000(Color other) {
        Objects.requireNonNull(other, "other");
        float[] first = toCieLab();
        float[] second = other.toCieLab();
        return (float) ciede2000(first[0], first[1], first[2], second[0], second[1], second[2]);
    }

    /// Returns CIE94 ΔE*94 against `other` in D65 L*a*b* with graphic-arts weights.
    ///
    /// The implementation uses `kL = kC = kH = 1`, `K1 = 0.045`, and `K2 = 0.015`.
    ///
    /// @param other the comparison color
    /// @return the CIE94 distance, at least `0`
    public float deltaE94(Color other) {
        Objects.requireNonNull(other, "other");
        float[] first = toCieLab();
        float[] second = other.toCieLab();
        double dL = first[0] - second[0];
        double da = first[1] - second[1];
        double db = first[2] - second[2];
        double c1 = Math.hypot(first[1], first[2]);
        double c2 = Math.hypot(second[1], second[2]);
        double dC = c1 - c2;
        double dH2 = Math.max(0.0, da * da + db * db - dC * dC);
        double sc = 1.0 + 0.045 * c1;
        double sh = 1.0 + 0.015 * c1;
        return (float) Math.sqrt(dL * dL + (dC / sc) * (dC / sc) + dH2 / (sh * sh));
    }

    /// Returns a copy of this color with `alpha` as the linear coverage.
    ///
    /// @param alpha the linear coverage in `[0, 1]`
    /// @return the recolored value
    public Color withAlpha(float alpha) {
        return new Color(encoding, red, green, blue, alpha);
    }

    /// Returns an extended-linear gray with this color's CIE relative luminance.
    ///
    /// @return the gray
    public Color grayscale() {
        Color linear = toExtendedLinear();
        float y = relativeLuminance();
        return extendedLinear(y, y, y, linear.alpha);
    }

    /// Interpolates this color with `other` in Oklab, then returns extended-linear sRGB.
    ///
    /// @param other the end color
    /// @param t the mix factor; `0` is this color and `1` is `other`
    /// @return the interpolated extended-linear color
    public Color interpolateOklab(Color other, float t) {
        Objects.requireNonNull(other, "other");
        if (!Float.isFinite(t)) {
            throw new IllegalArgumentException("t must be finite");
        }
        float[] start = toOklab();
        float[] end = other.toOklab();
        float complement = 1.0f - t;
        Color mixed = fromOklab(
                start[0] * complement + end[0] * t,
                start[1] * complement + end[1] * t,
                start[2] * complement + end[2] * t,
                toExtendedLinear().alpha * complement + other.toExtendedLinear().alpha * t
        );
        return mixed.toExtendedLinear();
    }

    /// Packs this color as 8-bit sRGB ARGB.
    ///
    /// Components are converted through [`#toSrgb()`] and rounded into `[0, 255]`.
    ///
    /// @return the packed ARGB word
    public int toArgb() {
        Color srgb = toSrgb();
        return (quantize8(srgb.alpha) << 24)
                | (quantize8(srgb.red) << 16)
                | (quantize8(srgb.green) << 8)
                | quantize8(srgb.blue);
    }

    /// Creates an sRGB color from a packed 8-bit ARGB word.
    ///
    /// @param argb the packed ARGB word
    /// @return the sRGB color
    public static Color fromArgb(int argb) {
        return srgb(
                ((argb >>> 16) & 0xFF) / 255.0f,
                ((argb >>> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f,
                ((argb >>> 24) & 0xFF) / 255.0f
        );
    }

    /// Returns sRGB hue, saturation, and lightness.
    ///
    /// Hue is in degrees in `[0, 360)`. Saturation and lightness are in `[0, 1]`.
    /// Neutral colors report hue `0`.
    ///
    /// @return `{hue, saturation, lightness}`
    public float[] toHsl() {
        Color srgb = toSrgb();
        float max = Math.max(srgb.red, Math.max(srgb.green, srgb.blue));
        float min = Math.min(srgb.red, Math.min(srgb.green, srgb.blue));
        float lightness = (max + min) * 0.5f;
        float delta = max - min;
        float saturation;
        if (delta < 1.0e-6f) {
            saturation = 0.0f;
        } else {
            saturation = delta / (1.0f - Math.abs(2.0f * lightness - 1.0f));
        }
        return new float[] {rgbHueDegrees(srgb.red, srgb.green, srgb.blue, max, delta), saturation, lightness};
    }

    /// Creates an sRGB color from hue, saturation, and lightness.
    ///
    /// @param hueDegrees the hue in degrees
    /// @param saturation the saturation in `[0, 1]`
    /// @param lightness the lightness in `[0, 1]`
    /// @param alpha the linear coverage
    /// @return the sRGB color
    public static Color fromHsl(float hueDegrees, float saturation, float lightness, float alpha) {
        requireUnitPair(saturation, lightness);
        float chroma = (1.0f - Math.abs(2.0f * lightness - 1.0f)) * saturation;
        float[] rgb = rgbFromHueChroma(hueDegrees, chroma);
        float match = lightness - chroma * 0.5f;
        return srgb(rgb[0] + match, rgb[1] + match, rgb[2] + match, alpha);
    }

    /// Returns sRGB hue, saturation, and value.
    ///
    /// Hue is in degrees in `[0, 360)`. Saturation and value are in `[0, 1]`.
    /// Neutral colors report hue `0`.
    ///
    /// @return `{hue, saturation, value}`
    public float[] toHsv() {
        Color srgb = toSrgb();
        float max = Math.max(srgb.red, Math.max(srgb.green, srgb.blue));
        float min = Math.min(srgb.red, Math.min(srgb.green, srgb.blue));
        float delta = max - min;
        float saturation = max < 1.0e-6f ? 0.0f : delta / max;
        return new float[] {rgbHueDegrees(srgb.red, srgb.green, srgb.blue, max, delta), saturation, max};
    }

    /// Creates an sRGB color from hue, saturation, and value.
    ///
    /// @param hueDegrees the hue in degrees
    /// @param saturation the saturation in `[0, 1]`
    /// @param value the value in `[0, 1]`
    /// @param alpha the linear coverage
    /// @return the sRGB color
    public static Color fromHsv(float hueDegrees, float saturation, float value, float alpha) {
        requireUnitPair(saturation, value);
        float chroma = value * saturation;
        float[] rgb = rgbFromHueChroma(hueDegrees, chroma);
        float match = value - chroma;
        return srgb(rgb[0] + match, rgb[1] + match, rgb[2] + match, alpha);
    }

    /// Returns the RGB hue in `[0, 360)` for a component triple.
    private static float rgbHueDegrees(float red, float green, float blue, float max, float delta) {
        if (delta < 1.0e-6f) {
            return 0.0f;
        }
        float hue;
        if (max == red) {
            hue = ((green - blue) / delta) % 6.0f;
        } else if (max == green) {
            hue = (blue - red) / delta + 2.0f;
        } else {
            hue = (red - green) / delta + 4.0f;
        }
        hue *= 60.0f;
        if (hue < 0.0f) {
            hue += 360.0f;
        }
        return hue;
    }

    /// Maps a hue and chroma onto an RGB triple before adding the lightness/value match.
    private static float[] rgbFromHueChroma(float hueDegrees, float chroma) {
        if (!Float.isFinite(hueDegrees)) {
            throw new IllegalArgumentException("hue must be finite");
        }
        float sector = (((hueDegrees % 360.0f) + 360.0f) % 360.0f) / 60.0f;
        float x = chroma * (1.0f - Math.abs(sector % 2.0f - 1.0f));
        int index = (int) Math.floor(sector);
        return switch (index) {
            case 0 -> new float[] {chroma, x, 0.0f};
            case 1 -> new float[] {x, chroma, 0.0f};
            case 2 -> new float[] {0.0f, chroma, x};
            case 3 -> new float[] {0.0f, x, chroma};
            case 4 -> new float[] {x, 0.0f, chroma};
            default -> new float[] {chroma, 0.0f, x};
        };
    }

    /// Rejects a saturation/lightness or saturation/value pair outside `[0, 1]`.
    private static void requireUnitPair(float first, float second) {
        if (!Float.isFinite(first) || first < 0.0f || first > 1.0f
                || !Float.isFinite(second) || second < 0.0f || second > 1.0f) {
            throw new IllegalArgumentException("HSL/HSV components must be finite and in [0, 1]");
        }
    }

    /// Rounds one unit component into `[0, 255]`.
    private static int quantize8(float component) {
        return Math.max(0, Math.min(255, Math.round(component * 255.0f)));
    }

    /// Creates an extended-linear color from CIE XYZ D65.
    ///
    /// @param x the X component
    /// @param y the Y component
    /// @param z the Z component
    /// @param alpha the alpha
    /// @return the extended-linear color
    public static Color fromXyzD65(float x, float y, float z, float alpha) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("CIE XYZ components must be finite");
        }
        return xyzD65ToExtended(x, y, z, alpha);
    }

    /// Converts this color to CIE L*a*b* relative to D65 with `Y = 1`.
    ///
    /// @return `{L, a, b}`
    public float[] toCieLab() {
        float[] xyz = toXyzD65();
        return xyzD65ToLab(xyz[0], xyz[1], xyz[2]);
    }

    /// Creates an extended-linear color from CIE L*a*b* relative to D65 with `Y = 1`.
    ///
    /// @param lightness the L* component
    /// @param a the a* component
    /// @param b the b* component
    /// @param alpha the alpha
    /// @return the extended-linear color
    public static Color fromCieLab(float lightness, float a, float b, float alpha) {
        float[] xyz = labToXyzD65(lightness, a, b);
        return xyzD65ToExtended(xyz[0], xyz[1], xyz[2], alpha);
    }

    /// Converts this color to CIE L*C*h° relative to D65.
    ///
    /// Hue is degrees in `[0, 360)`. Neutral colors report hue `0`.
    ///
    /// @return `{L, C, h}`
    public float[] toCieLch() {
        float[] lab = toCieLab();
        float chroma = (float) Math.hypot(lab[1], lab[2]);
        if (chroma < 0.1f) {
            return new float[] {lab[0], 0.0f, 0.0f};
        }
        float hue = (float) Math.toDegrees(Math.atan2(lab[2], lab[1]));
        if (hue < 0.0f) {
            hue += 360.0f;
        }
        return new float[] {lab[0], chroma, hue};
    }

    /// Creates an extended-linear color from CIE L*C*h° relative to D65.
    ///
    /// @param lightness the L* component
    /// @param chroma the nonnegative C* component
    /// @param hueDegrees the hue in degrees
    /// @param alpha the alpha
    /// @return the extended-linear color
    public static Color fromCieLch(float lightness, float chroma, float hueDegrees, float alpha) {
        if (!Float.isFinite(chroma) || chroma < 0.0f || !Float.isFinite(hueDegrees)) {
            throw new IllegalArgumentException("CIE LCh chroma must be finite and nonnegative");
        }
        double radians = Math.toRadians(hueDegrees);
        return fromCieLab(
                lightness,
                (float) (chroma * Math.cos(radians)),
                (float) (chroma * Math.sin(radians)),
                alpha
        );
    }

    /// Converts this color to CIE XYZ D50 through Bradford adaptation of [`#toXyzD65()`].
    ///
    /// @return `{X, Y, Z}` relative to D50
    public float[] toXyzD50() {
        float[] d65 = toXyzD65();
        return ChromaticAdaptation.bradford(
                d65[0],
                d65[1],
                d65[2],
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z,
                ChromaticAdaptation.D50_X,
                ChromaticAdaptation.D50_Y,
                ChromaticAdaptation.D50_Z
        );
    }

    /// Creates an extended-linear color from CIE XYZ D50.
    ///
    /// @param x the X component
    /// @param y the Y component
    /// @param z the Z component
    /// @param alpha the alpha
    /// @return the extended-linear color
    public static Color fromXyzD50(float x, float y, float z, float alpha) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("CIE XYZ components must be finite");
        }
        float[] d65 = ChromaticAdaptation.bradford(
                x,
                y,
                z,
                ChromaticAdaptation.D50_X,
                ChromaticAdaptation.D50_Y,
                ChromaticAdaptation.D50_Z,
                ChromaticAdaptation.D65_X,
                ChromaticAdaptation.D65_Y,
                ChromaticAdaptation.D65_Z
        );
        return xyzD65ToExtended(d65[0], d65[1], d65[2], alpha);
    }

    /// Converts this color to CIE L*u*v* relative to D65 with `Y = 1`.
    ///
    /// @return `{L, u, v}`
    public float[] toCieLuv() {
        float[] xyz = toXyzD65();
        return xyzD65ToLuv(xyz[0], xyz[1], xyz[2]);
    }

    /// Creates an extended-linear color from CIE L*u*v* relative to D65 with `Y = 1`.
    ///
    /// @param lightness the L* component
    /// @param u the u* component
    /// @param v the v* component
    /// @param alpha the alpha
    /// @return the extended-linear color
    public static Color fromCieLuv(float lightness, float u, float v, float alpha) {
        float[] xyz = luvToXyzD65(lightness, u, v);
        return xyzD65ToExtended(xyz[0], xyz[1], xyz[2], alpha);
    }

    /// Interpolates this color toward `other` in the extended-linear working encoding.
    ///
    /// Out-of-gamut components are preserved. `t` is not clamped.
    ///
    /// @param other the destination color
    /// @param t the mix factor; `0` is this color and `1` is `other`
    /// @return the interpolated extended-linear color
    /// Converts this color to OKLab through the extended-linear working encoding.
    ///
    /// @return `{L, a, b}`
    public float[] toOklab() {
        Color linear = toExtendedLinear();
        double lmsL = 0.4122214708 * linear.red + 0.5363325363 * linear.green + 0.0514459929 * linear.blue;
        double lmsM = 0.2119034982 * linear.red + 0.6806995451 * linear.green + 0.1073969566 * linear.blue;
        double lmsS = 0.0883024619 * linear.red + 0.2817188376 * linear.green + 0.6299787005 * linear.blue;
        double l = Math.cbrt(lmsL);
        double m = Math.cbrt(lmsM);
        double s = Math.cbrt(lmsS);
        return new float[] {
                (float) (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s),
                (float) (1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s),
                (float) (0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s)
        };
    }

    /// Converts this color to OKLCH through [`#toOklab()`].
    ///
    /// Hue is degrees in `[0, 360)`. Neutral colors report hue `0`.
    ///
    /// @return `{L, C, h}`
    public float[] toOklch() {
        float[] lab = toOklab();
        float chroma = (float) Math.hypot(lab[1], lab[2]);
        if (chroma < 0.001f) {
            return new float[] {lab[0], 0.0f, 0.0f};
        }
        float hue = (float) Math.toDegrees(Math.atan2(lab[2], lab[1]));
        if (hue < 0.0f) {
            hue += 360.0f;
        }
        return new float[] {lab[0], chroma, hue};
    }

    /// Creates an extended-linear color from OKLCH.
    ///
    /// @param lightness the L component
    /// @param chroma the nonnegative C component
    /// @param hueDegrees the hue in degrees
    /// @param alpha the alpha
    /// @return the extended-linear color
    public static Color fromOklch(float lightness, float chroma, float hueDegrees, float alpha) {
        if (!Float.isFinite(chroma) || chroma < 0.0f || !Float.isFinite(hueDegrees)) {
            throw new IllegalArgumentException("OKLCH chroma must be finite and nonnegative");
        }
        double radians = Math.toRadians(hueDegrees);
        return fromOklab(
                lightness,
                (float) (chroma * Math.cos(radians)),
                (float) (chroma * Math.sin(radians)),
                alpha
        );
    }

    /// Creates an extended-linear color from OKLab.
    ///
    /// @param lightness the L component
    /// @param a the a component
    /// @param b the b component
    /// @param alpha the alpha
    /// @return the extended-linear color
    public static Color fromOklab(float lightness, float a, float b, float alpha) {
        if (!Float.isFinite(lightness) || !Float.isFinite(a) || !Float.isFinite(b)) {
            throw new IllegalArgumentException("OKLab components must be finite");
        }
        double l = lightness + 0.3963377774 * a + 0.2158037573 * b;
        double m = lightness - 0.1055613458 * a - 0.0638541728 * b;
        double s = lightness - 0.0894841775 * a - 1.2914855480 * b;
        l = l * l * l;
        m = m * m * m;
        s = s * s * s;
        return extendedLinear(
                (float) (4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
                (float) (-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
                (float) (-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s),
                alpha
        );
    }

    /// Interpolates this color toward `other` in the extended-linear working encoding.
    ///
    /// Out-of-gamut components are preserved. `t` is not clamped.
    ///
    /// @param other the destination color
    /// @param t the mix factor; `0` is this color and `1` is `other`
    /// @return the interpolated extended-linear color
    public Color interpolate(Color other, float t) {
        Objects.requireNonNull(other, "other");
        if (!Float.isFinite(t)) {
            throw new IllegalArgumentException("t must be finite");
        }
        Color start = toExtendedLinear();
        Color end = other.toExtendedLinear();
        float complement = 1.0f - t;
        return extendedLinear(
                start.red * complement + end.red * t,
                start.green * complement + end.green * t,
                start.blue * complement + end.blue * t,
                start.alpha * complement + end.alpha * t
        );
    }

    /// Computes CIEDE2000 between two D65 L*a*b* triples with `kL = kC = kH = 1`.
    private static double ciede2000(double l1, double a1, double b1, double l2, double a2, double b2) {
        double c1 = Math.hypot(a1, b1);
        double c2 = Math.hypot(a2, b2);
        double cBar = (c1 + c2) / 2.0;
        double cBar7 = Math.pow(cBar, 7.0);
        double g = 0.5 * (1.0 - Math.sqrt(cBar7 / (cBar7 + 6103515625.0)));
        double a1p = (1.0 + g) * a1;
        double a2p = (1.0 + g) * a2;
        double c1p = Math.hypot(a1p, b1);
        double c2p = Math.hypot(a2p, b2);
        double h1p = hueDegrees(b1, a1p, c1p);
        double h2p = hueDegrees(b2, a2p, c2p);
        double dLp = l2 - l1;
        double dCp = c2p - c1p;
        double dhp;
        if (c1p * c2p == 0.0) {
            dhp = 0.0;
        } else if (Math.abs(h2p - h1p) <= 180.0) {
            dhp = h2p - h1p;
        } else if (h2p - h1p > 180.0) {
            dhp = h2p - h1p - 360.0;
        } else {
            dhp = h2p - h1p + 360.0;
        }
        double dHp = 2.0 * Math.sqrt(c1p * c2p) * Math.sin(Math.toRadians(dhp / 2.0));
        double lBarp = (l1 + l2) / 2.0;
        double cBarp = (c1p + c2p) / 2.0;
        double hBarp;
        if (c1p * c2p == 0.0) {
            hBarp = h1p + h2p;
        } else if (Math.abs(h1p - h2p) <= 180.0) {
            hBarp = (h1p + h2p) / 2.0;
        } else if (h1p + h2p < 360.0) {
            hBarp = (h1p + h2p + 360.0) / 2.0;
        } else {
            hBarp = (h1p + h2p - 360.0) / 2.0;
        }
        double t = 1.0
                - 0.17 * Math.cos(Math.toRadians(hBarp - 30.0))
                + 0.24 * Math.cos(Math.toRadians(2.0 * hBarp))
                + 0.32 * Math.cos(Math.toRadians(3.0 * hBarp + 6.0))
                - 0.20 * Math.cos(Math.toRadians(4.0 * hBarp - 63.0));
        double sl = 1.0 + (0.015 * (lBarp - 50.0) * (lBarp - 50.0))
                / Math.sqrt(20.0 + (lBarp - 50.0) * (lBarp - 50.0));
        double sc = 1.0 + 0.045 * cBarp;
        double sh = 1.0 + 0.015 * cBarp * t;
        double dTheta = 30.0 * Math.exp(-Math.pow((hBarp - 275.0) / 25.0, 2.0));
        double cBarp7 = Math.pow(cBarp, 7.0);
        double rc = 2.0 * Math.sqrt(cBarp7 / (cBarp7 + 6103515625.0));
        double rt = -Math.sin(Math.toRadians(2.0 * dTheta)) * rc;
        double lTerm = dLp / sl;
        double cTerm = dCp / sc;
        double hTerm = dHp / sh;
        return Math.sqrt(lTerm * lTerm + cTerm * cTerm + hTerm * hTerm + rt * cTerm * hTerm);
    }

    /// Returns CIEDE2000 hue in degrees in `[0, 360)`, or `0` when chroma is zero.
    private static double hueDegrees(double b, double ap, double chroma) {
        if (chroma == 0.0) {
            return 0.0;
        }
        double hue = Math.toDegrees(Math.atan2(b, ap));
        return hue >= 0.0 ? hue : hue + 360.0;
    }

    /// Converts CIE XYZ D65 into CIE L*a*b*.
    private static float[] xyzD65ToLab(float x, float y, float z) {
        float fx = labPivot(x / ChromaticAdaptation.D65_X);
        float fy = labPivot(y / ChromaticAdaptation.D65_Y);
        float fz = labPivot(z / ChromaticAdaptation.D65_Z);
        return new float[] {
                116.0f * fy - 16.0f,
                500.0f * (fx - fy),
                200.0f * (fy - fz)
        };
    }

    /// Converts CIE L*a*b* into CIE XYZ D65.
    private static float[] labToXyzD65(float lightness, float a, float b) {
        if (!Float.isFinite(lightness) || !Float.isFinite(a) || !Float.isFinite(b)) {
            throw new IllegalArgumentException("CIE Lab components must be finite");
        }
        float fy = (lightness + 16.0f) / 116.0f;
        float fx = a / 500.0f + fy;
        float fz = fy - b / 200.0f;
        return new float[] {
                ChromaticAdaptation.D65_X * labInverse(fx),
                ChromaticAdaptation.D65_Y * labInverse(fy),
                ChromaticAdaptation.D65_Z * labInverse(fz)
        };
    }

    /// Converts CIE XYZ D65 into CIE L*u*v*.
    private static float[] xyzD65ToLuv(float x, float y, float z) {
        float lightness = 116.0f * labPivot(y / ChromaticAdaptation.D65_Y) - 16.0f;
        float[] white = cieUv(ChromaticAdaptation.D65_X, ChromaticAdaptation.D65_Y, ChromaticAdaptation.D65_Z);
        float[] uv = cieUv(x, y, z);
        return new float[] {
                lightness,
                13.0f * lightness * (uv[0] - white[0]),
                13.0f * lightness * (uv[1] - white[1])
        };
    }

    /// Converts CIE L*u*v* into CIE XYZ D65.
    private static float[] luvToXyzD65(float lightness, float u, float v) {
        if (!Float.isFinite(lightness) || !Float.isFinite(u) || !Float.isFinite(v)) {
            throw new IllegalArgumentException("CIE Luv components must be finite");
        }
        if (lightness <= 0.0f) {
            return new float[] {0.0f, 0.0f, 0.0f};
        }
        float[] white = cieUv(ChromaticAdaptation.D65_X, ChromaticAdaptation.D65_Y, ChromaticAdaptation.D65_Z);
        float up = u / (13.0f * lightness) + white[0];
        float vp = v / (13.0f * lightness) + white[1];
        float y = ChromaticAdaptation.D65_Y * labInverse((lightness + 16.0f) / 116.0f);
        if (Math.abs(vp) < 1.0e-8f) {
            return new float[] {0.0f, y, 0.0f};
        }
        float x = y * 9.0f * up / (4.0f * vp);
        float z = y * (12.0f - 3.0f * up - 20.0f * vp) / (4.0f * vp);
        return new float[] {x, y, z};
    }

    /// Returns CIE `u', v'` for one XYZ triple.
    private static float[] cieUv(float x, float y, float z) {
        float denom = x + 15.0f * y + 3.0f * z;
        if (Math.abs(denom) < 1.0e-8f) {
            return new float[] {0.0f, 0.0f};
        }
        return new float[] {4.0f * x / denom, 9.0f * y / denom};
    }

    /// CIE Lab forward pivot.
    private static float labPivot(float ratio) {
        float delta = 6.0f / 29.0f;
        if (ratio > delta * delta * delta) {
            return (float) Math.cbrt(ratio);
        }
        return ratio / (3.0f * delta * delta) + 4.0f / 29.0f;
    }

    /// CIE Lab inverse pivot.
    private static float labInverse(float pivot) {
        float delta = 6.0f / 29.0f;
        if (pivot > delta) {
            return pivot * pivot * pivot;
        }
        return 3.0f * delta * delta * (pivot - 4.0f / 29.0f);
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

    /// Applies the BT.2100 HLG OETF.
    ///
    /// Scene-linear `1.0` is the HLG system peak.
    ///
    /// @param linear the scene-linear component
    /// @return the HLG-encoded component
    static float encodeHlg(float linear) {
        float clipped = Math.max(linear, 0.0f);
        if (clipped <= 1.0f / 12.0f) {
            return (float) Math.sqrt(3.0f * clipped);
        }
        return (float) (0.17883277 * Math.log(12.0 * clipped - 0.28466892) + 0.5599107295);
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
