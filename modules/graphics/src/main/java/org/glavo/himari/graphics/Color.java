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

    /// Interpolates this color with `other` in OKLCH, then returns extended-linear sRGB.
    ///
    /// Hue travels the shorter arc. `t` is unclamped.
    ///
    /// @param other the end color
    /// @param t the mix factor; `0` is this color and `1` is `other`
    /// @return the interpolated extended-linear color
    public Color interpolateOklch(Color other, float t) {
        Objects.requireNonNull(other, "other");
        if (!Float.isFinite(t)) {
            throw new IllegalArgumentException("t must be finite");
        }
        float[] start = toOklch();
        float[] end = other.toOklch();
        float complement = 1.0f - t;
        float hueDelta = end[2] - start[2];
        if (hueDelta > 180.0f) {
            hueDelta -= 360.0f;
        } else if (hueDelta < -180.0f) {
            hueDelta += 360.0f;
        }
        Color mixed = fromOklch(
                start[0] * complement + end[0] * t,
                start[1] * complement + end[1] * t,
                start[2] + hueDelta * t,
                toExtendedLinear().alpha * complement + other.toExtendedLinear().alpha * t
        );
        return mixed.toExtendedLinear();
    }

    /// Interpolates this color with `other` in CIE LCh, then returns extended-linear sRGB.
    ///
    /// Hue travels the shorter arc. `t` is unclamped.
    ///
    /// @param other the end color
    /// @param t the mix factor; `0` is this color and `1` is `other`
    /// @return the interpolated extended-linear color
    public Color interpolateCieLch(Color other, float t) {
        Objects.requireNonNull(other, "other");
        if (!Float.isFinite(t)) {
            throw new IllegalArgumentException("t must be finite");
        }
        float[] start = toCieLch();
        float[] end = other.toCieLch();
        float complement = 1.0f - t;
        float hueDelta = end[2] - start[2];
        if (hueDelta > 180.0f) {
            hueDelta -= 360.0f;
        } else if (hueDelta < -180.0f) {
            hueDelta += 360.0f;
        }
        Color mixed = fromCieLch(
                start[0] * complement + end[0] * t,
                start[1] * complement + end[1] * t,
                start[2] + hueDelta * t,
                toExtendedLinear().alpha * complement + other.toExtendedLinear().alpha * t
        );
        return mixed.toExtendedLinear();
    }

    /// Interpolates this color with `other` in CIE Lab, then returns extended-linear sRGB.
    ///
    /// `t` is unclamped.
    ///
    /// @param other the end color
    /// @param t the mix factor; `0` is this color and `1` is `other`
    /// @return the interpolated extended-linear color
    public Color interpolateCieLab(Color other, float t) {
        Objects.requireNonNull(other, "other");
        if (!Float.isFinite(t)) {
            throw new IllegalArgumentException("t must be finite");
        }
        float[] start = toCieLab();
        float[] end = other.toCieLab();
        float complement = 1.0f - t;
        Color mixed = fromCieLab(
                start[0] * complement + end[0] * t,
                start[1] * complement + end[1] * t,
                start[2] * complement + end[2] * t,
                toExtendedLinear().alpha * complement + other.toExtendedLinear().alpha * t
        );
        return mixed.toExtendedLinear();
    }

    /// Interpolates this color with `other` in CIE Luv, then returns extended-linear sRGB.
    ///
    /// `t` is unclamped.
    ///
    /// @param other the end color
    /// @param t the mix factor; `0` is this color and `1` is `other`
    /// @return the interpolated extended-linear color
    public Color interpolateCieLuv(Color other, float t) {
        Objects.requireNonNull(other, "other");
        if (!Float.isFinite(t)) {
            throw new IllegalArgumentException("t must be finite");
        }
        float[] start = toCieLuv();
        float[] end = other.toCieLuv();
        float complement = 1.0f - t;
        Color mixed = fromCieLuv(
                start[0] * complement + end[0] * t,
                start[1] * complement + end[1] * t,
                start[2] * complement + end[2] * t,
                toExtendedLinear().alpha * complement + other.toExtendedLinear().alpha * t
        );
        return mixed.toExtendedLinear();
    }

    /// Interpolates this color with `other` in CIE XYZ D50, then returns extended-linear sRGB.
    ///
    /// `t` is unclamped.
    ///
    /// @param other the end color
    /// @param t the mix factor; `0` is this color and `1` is `other`
    /// @return the interpolated extended-linear color
    public Color interpolateXyzD50(Color other, float t) {
        Objects.requireNonNull(other, "other");
        if (!Float.isFinite(t)) {
            throw new IllegalArgumentException("t must be finite");
        }
        float[] start = toXyzD50();
        float[] end = other.toXyzD50();
        float complement = 1.0f - t;
        return fromXyzD50(
                start[0] * complement + end[0] * t,
                start[1] * complement + end[1] * t,
                start[2] * complement + end[2] * t,
                toExtendedLinear().alpha * complement + other.toExtendedLinear().alpha * t
        );
    }

    /// Interpolates this color with `other` in CIE XYZ D65, then returns extended-linear sRGB.
    ///
    /// `t` is unclamped.
    ///
    /// @param other the end color
    /// @param t the mix factor; `0` is this color and `1` is `other`
    /// @return the interpolated extended-linear color
    public Color interpolateXyzD65(Color other, float t) {
        Objects.requireNonNull(other, "other");
        if (!Float.isFinite(t)) {
            throw new IllegalArgumentException("t must be finite");
        }
        float[] start = toXyzD65();
        float[] end = other.toXyzD65();
        float complement = 1.0f - t;
        return fromXyzD65(
                start[0] * complement + end[0] * t,
                start[1] * complement + end[1] * t,
                start[2] * complement + end[2] * t,
                toExtendedLinear().alpha * complement + other.toExtendedLinear().alpha * t
        );
    }

    /// Replaces the CIE XYZ D50 components, then returns extended-linear sRGB.
    ///
    /// @param x the X component
    /// @param y the Y component
    /// @param z the Z component
    /// @return the edited extended-linear color
    public Color withXyzD50(float x, float y, float z) {
        return fromXyzD50(x, y, z, toExtendedLinear().alpha);
    }

    /// Replaces the CIE XYZ D65 components, then returns extended-linear sRGB.
    ///
    /// @param x the X component
    /// @param y the Y component
    /// @param z the Z component
    /// @return the edited extended-linear color
    public Color withXyzD65(float x, float y, float z) {
        return fromXyzD65(x, y, z, toExtendedLinear().alpha);
    }

    /// Replaces CIE XYZ D50 `X`, then returns extended-linear sRGB.
    ///
    /// @param x the X component
    /// @return the edited extended-linear color
    public Color withXyzD50X(float x) {
        float[] xyz = toXyzD50();
        return fromXyzD50(x, xyz[1], xyz[2], toExtendedLinear().alpha);
    }

    /// Replaces CIE XYZ D50 `Y`, then returns extended-linear sRGB.
    ///
    /// @param y the Y component
    /// @return the edited extended-linear color
    public Color withXyzD50Y(float y) {
        float[] xyz = toXyzD50();
        return fromXyzD50(xyz[0], y, xyz[2], toExtendedLinear().alpha);
    }

    /// Replaces CIE XYZ D50 `Z`, then returns extended-linear sRGB.
    ///
    /// @param z the Z component
    /// @return the edited extended-linear color
    public Color withXyzD50Z(float z) {
        float[] xyz = toXyzD50();
        return fromXyzD50(xyz[0], xyz[1], z, toExtendedLinear().alpha);
    }

    /// Replaces CIE XYZ D65 `X`, then returns extended-linear sRGB.
    ///
    /// @param x the X component
    /// @return the edited extended-linear color
    public Color withXyzD65X(float x) {
        float[] xyz = toXyzD65();
        return fromXyzD65(x, xyz[1], xyz[2], toExtendedLinear().alpha);
    }

    /// Replaces CIE XYZ D65 `Y`, then returns extended-linear sRGB.
    ///
    /// @param y the Y component
    /// @return the edited extended-linear color
    public Color withXyzD65Y(float y) {
        float[] xyz = toXyzD65();
        return fromXyzD65(xyz[0], y, xyz[2], toExtendedLinear().alpha);
    }

    /// Replaces CIE XYZ D65 `Z`, then returns extended-linear sRGB.
    ///
    /// @param z the Z component
    /// @return the edited extended-linear color
    public Color withXyzD65Z(float z) {
        float[] xyz = toXyzD65();
        return fromXyzD65(xyz[0], xyz[1], z, toExtendedLinear().alpha);
    }

    /// Replaces the tagged red primary and keeps this encoding.
    ///
    /// @param red the red primary
    /// @return the edited color
    public Color withRed(float red) {
        return new Color(encoding, red, green, blue, alpha);
    }

    /// Replaces the tagged green primary and keeps this encoding.
    ///
    /// @param green the green primary
    /// @return the edited color
    public Color withGreen(float green) {
        return new Color(encoding, red, green, blue, alpha);
    }

    /// Replaces the tagged blue primary and keeps this encoding.
    ///
    /// @param blue the blue primary
    /// @return the edited color
    public Color withBlue(float blue) {
        return new Color(encoding, red, green, blue, alpha);
    }

    /// Replaces the tagged red, green, and blue primaries and keeps this encoding.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @return the edited color
    public Color withRgb(float red, float green, float blue) {
        return new Color(encoding, red, green, blue, alpha);
    }

    /// Replaces the tagged primaries and linear coverage and keeps this encoding.
    ///
    /// @param red the red primary
    /// @param green the green primary
    /// @param blue the blue primary
    /// @param alpha the linear coverage
    /// @return the edited color
    public Color withRgba(float red, float green, float blue, float alpha) {
        return new Color(encoding, red, green, blue, alpha);
    }

    /// Returns the extended-linear color whose primaries are multiplied by linear coverage.
    ///
    /// Conversion uses [`#toExtendedLinear()`] so premultiplication is linear-light.
    ///
    /// @return the associated color
    public Color premultiply() {
        Color linear = toExtendedLinear();
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                linear.red * linear.alpha,
                linear.green * linear.alpha,
                linear.blue * linear.alpha,
                linear.alpha
        );
    }

    /// Restores unassociated primaries from linear-light associated coverage.
    ///
    /// Conversion uses [`#toExtendedLinear()`]. A zero-coverage color returns extended-linear
    /// zeros so the inverse of [`#premultiply()`] stays defined.
    ///
    /// @return the unassociated color
    public Color unpremultiply() {
        Color linear = toExtendedLinear();
        if (linear.alpha == 0.0f) {
            return new Color(ColorEncoding.EXTENDED_LINEAR, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                linear.red / linear.alpha,
                linear.green / linear.alpha,
                linear.blue / linear.alpha,
                linear.alpha
        );
    }

    /// Composites this color over `backdrop` with linear-light source-over.
    ///
    /// Both colors are converted through [`#premultiply()`]. The result is the unassociated
    /// extended-linear composite from [`#unpremultiply()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color over(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = premultiply();
        Color dst = backdrop.premultiply();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                src.red + dst.red * inv,
                src.green + dst.green * inv,
                src.blue + dst.blue * inv,
                src.alpha + dst.alpha * inv
        ).unpremultiply();
    }

    /// Composites this color into `backdrop` with linear-light source-in.
    ///
    /// Both colors are converted through [`#premultiply()`]. The result is the unassociated
    /// extended-linear composite from [`#unpremultiply()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color in(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = premultiply();
        Color dst = backdrop.premultiply();
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                src.red * dst.alpha,
                src.green * dst.alpha,
                src.blue * dst.alpha,
                src.alpha * dst.alpha
        ).unpremultiply();
    }

    /// Composites this color against `backdrop` with linear-light source-out.
    ///
    /// Both colors are converted through [`#premultiply()`]. The result is the unassociated
    /// extended-linear composite from [`#unpremultiply()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color out(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = premultiply();
        Color dst = backdrop.premultiply();
        float inv = 1.0f - dst.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                src.red * inv,
                src.green * inv,
                src.blue * inv,
                src.alpha * inv
        ).unpremultiply();
    }

    /// Composites this color over `backdrop` with linear-light source-atop.
    ///
    /// Both colors are converted through [`#premultiply()`]. The result is the unassociated
    /// extended-linear composite from [`#unpremultiply()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color atop(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = premultiply();
        Color dst = backdrop.premultiply();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                src.red * dst.alpha + dst.red * inv,
                src.green * dst.alpha + dst.green * inv,
                src.blue * dst.alpha + dst.blue * inv,
                src.alpha * dst.alpha + dst.alpha * inv
        ).unpremultiply();
    }

    /// Composites this color under `backdrop` with linear-light destination-over.
    ///
    /// Both colors are converted through [`#premultiply()`]. The result is the unassociated
    /// extended-linear composite from [`#unpremultiply()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color destOver(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        return backdrop.over(this);
    }

    /// Composites this color with `backdrop` using linear-light exclusive-or.
    ///
    /// Both colors are converted through [`#premultiply()`]. The result is the unassociated
    /// extended-linear composite from [`#unpremultiply()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color xor(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = premultiply();
        Color dst = backdrop.premultiply();
        float srcKeep = 1.0f - dst.alpha;
        float dstKeep = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                src.red * srcKeep + dst.red * dstKeep,
                src.green * srcKeep + dst.green * dstKeep,
                src.blue * srcKeep + dst.blue * dstKeep,
                src.alpha * srcKeep + dst.alpha * dstKeep
        ).unpremultiply();
    }

    /// Composites this color into `backdrop` with linear-light destination-in.
    ///
    /// Both colors are converted through [`#premultiply()`]. The result is the unassociated
    /// extended-linear composite from [`#unpremultiply()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color destIn(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        return backdrop.in(this);
    }

    /// Composites this color against `backdrop` with linear-light destination-out.
    ///
    /// Both colors are converted through [`#premultiply()`]. The result is the unassociated
    /// extended-linear composite from [`#unpremultiply()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color destOut(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        return backdrop.out(this);
    }

    /// Composites this color under `backdrop` with linear-light destination-atop.
    ///
    /// Both colors are converted through [`#premultiply()`]. The result is the unassociated
    /// extended-linear composite from [`#unpremultiply()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color destAtop(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        return backdrop.atop(this);
    }

    /// Adds this color to `backdrop` with linear-light Porter-Duff plus.
    ///
    /// Both colors are converted through [`#premultiply()`]. Associated primaries may exceed
    /// `1`. Coverage is clamped to `[0, 1]` so the result stays a valid [`Color`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color plus(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = premultiply();
        Color dst = backdrop.premultiply();
        float alpha = src.alpha + dst.alpha;
        if (alpha > 1.0f) {
            alpha = 1.0f;
        }
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                src.red + dst.red,
                src.green + dst.green,
                src.blue + dst.blue,
                alpha
        ).unpremultiply();
    }

    /// Discards both this color and `backdrop`, returning transparent extended-linear black.
    ///
    /// @param backdrop the destination color
    /// @return the empty composite
    public Color clear(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        return new Color(ColorEncoding.EXTENDED_LINEAR, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    /// Returns this color as unassociated extended-linear, ignoring `backdrop`.
    ///
    /// @param backdrop the destination color
    /// @return this color in the working encoding
    public Color source(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        return toExtendedLinear();
    }

    /// Returns `backdrop` as unassociated extended-linear, ignoring this color.
    ///
    /// @param backdrop the destination color
    /// @return the destination color in the working encoding
    public Color dest(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        return backdrop.toExtendedLinear();
    }

    /// Multiplies unassociated linear primaries and uses source-over coverage.
    ///
    /// Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color multiply(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                src.red * dst.red,
                src.green * dst.green,
                src.blue * dst.blue,
                src.alpha + dst.alpha * inv
        );
    }

    /// Screens unassociated linear primaries and uses source-over coverage.
    ///
    /// Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color screen(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                1.0f - (1.0f - src.red) * (1.0f - dst.red),
                1.0f - (1.0f - src.green) * (1.0f - dst.green),
                1.0f - (1.0f - src.blue) * (1.0f - dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Overlays unassociated linear primaries and uses source-over coverage.
    ///
    /// Destination channels at or below `0.5` multiply; brighter channels screen.
    /// Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color overlay(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                overlayChannel(src.red, dst.red),
                overlayChannel(src.green, dst.green),
                overlayChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Hard-lights unassociated linear primaries and uses source-over coverage.
    ///
    /// This is [`#overlay(Color)`] with the operands swapped.
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color hardLight(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        return backdrop.overlay(this);
    }

    /// Combines one pair of unassociated linear primaries for [`#overlay(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float overlayChannel(float source, float destination) {
        if (destination <= 0.5f) {
            return 2.0f * source * destination;
        }
        return 1.0f - 2.0f * (1.0f - source) * (1.0f - destination);
    }

    /// Soft-lights unassociated linear primaries and uses source-over coverage.
    ///
    /// The channel formula follows CSS Compositing `soft-light`. Negative destination
    /// primaries use `0` for the brightening square-root branch. Both colors are
    /// converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color softLight(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                softLightChannel(src.red, dst.red),
                softLightChannel(src.green, dst.green),
                softLightChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Combines one pair of unassociated linear primaries for [`#softLight(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float softLightChannel(float source, float destination) {
        if (source <= 0.5f) {
            return destination - (1.0f - 2.0f * source) * destination * (1.0f - destination);
        }
        float d;
        if (destination <= 0.25f) {
            d = ((16.0f * destination - 12.0f) * destination + 4.0f) * destination;
        } else {
            d = (float) Math.sqrt(Math.max(0.0, destination));
        }
        return destination + (2.0f * source - 1.0f) * (d - destination);
    }

    /// Color-dodges unassociated linear primaries and uses source-over coverage.
    ///
    /// A source primary of `1` yields `1`. Both colors are converted through
    /// [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color colorDodge(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                colorDodgeChannel(src.red, dst.red),
                colorDodgeChannel(src.green, dst.green),
                colorDodgeChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Combines one pair of unassociated linear primaries for [`#colorDodge(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float colorDodgeChannel(float source, float destination) {
        if (source >= 1.0f) {
            return 1.0f;
        }
        return destination / (1.0f - source);
    }

    /// Color-burns unassociated linear primaries and uses source-over coverage.
    ///
    /// A source primary of `0` yields `0` unless the destination is `1`. Both colors
    /// are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color colorBurn(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                colorBurnChannel(src.red, dst.red),
                colorBurnChannel(src.green, dst.green),
                colorBurnChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Combines one pair of unassociated linear primaries for [`#colorBurn(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float colorBurnChannel(float source, float destination) {
        if (source <= 0.0f) {
            return destination >= 1.0f ? 1.0f : 0.0f;
        }
        return 1.0f - (1.0f - destination) / source;
    }

    /// Differences unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is the absolute difference of the source and destination primaries.
    /// Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color difference(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                Math.abs(src.red - dst.red),
                Math.abs(src.green - dst.green),
                Math.abs(src.blue - dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Excludes unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `source + destination - 2 * source * destination`.
    /// Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color exclusion(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                src.red + dst.red - 2.0f * src.red * dst.red,
                src.green + dst.green - 2.0f * src.green * dst.green,
                src.blue + dst.blue - 2.0f * src.blue * dst.blue,
                src.alpha + dst.alpha * inv
        );
    }

    /// Replaces `backdrop` hue with this color's sRGB hue.
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color hue(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        return backdrop.withHue(toHsl()[0]);
    }

    /// Replaces `backdrop` saturation with this color's sRGB saturation.
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color saturation(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        return backdrop.withSaturation(toHsl()[1]);
    }

    /// Replaces `backdrop` lightness with this color's sRGB lightness.
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color luminosity(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        return backdrop.withLightness(toHsl()[2]);
    }

    /// Replaces `backdrop` hue and saturation with this color's sRGB hue and saturation.
    ///
    /// Lightness is taken from `backdrop`. This is the CSS non-separable `color` blend.
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color color(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        float[] source = toHsl();
        return backdrop.withHue(source[0]).withSaturation(source[1]);
    }

    /// Darkens unassociated linear primaries by taking the channel-wise minimum.
    ///
    /// Coverage uses source-over. Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color darken(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                Math.min(src.red, dst.red),
                Math.min(src.green, dst.green),
                Math.min(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Lightens unassociated linear primaries by taking the channel-wise maximum.
    ///
    /// Coverage uses source-over. Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color lighten(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                Math.max(src.red, dst.red),
                Math.max(src.green, dst.green),
                Math.max(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Pin-lights unassociated linear primaries and uses source-over coverage.
    ///
    /// Source channels above `0.5` replace darker destination values; darker source
    /// channels replace lighter destination values. Both colors are converted through
    /// [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color pinLight(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                pinLightChannel(src.red, dst.red),
                pinLightChannel(src.green, dst.green),
                pinLightChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Combines one pair of unassociated linear primaries for [`#pinLight(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float pinLightChannel(float source, float destination) {
        if (source > 0.5f) {
            return Math.max(destination, 2.0f * source - 1.0f);
        }
        return Math.min(destination, 2.0f * source);
    }

    /// Vivid-lights unassociated linear primaries and uses source-over coverage.
    ///
    /// Source channels above `0.5` color-dodge; darker source channels color-burn.
    /// Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color vividLight(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                vividLightChannel(src.red, dst.red),
                vividLightChannel(src.green, dst.green),
                vividLightChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Combines one pair of unassociated linear primaries for [`#vividLight(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float vividLightChannel(float source, float destination) {
        if (source > 0.5f) {
            return colorDodgeChannel(2.0f * source - 1.0f, destination);
        }
        return colorBurnChannel(2.0f * source, destination);
    }

    /// Linear-lights unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `destination + 2 * source - 1`. Both colors are converted
    /// through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color linearLight(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                dst.red + 2.0f * src.red - 1.0f,
                dst.green + 2.0f * src.green - 1.0f,
                dst.blue + 2.0f * src.blue - 1.0f,
                src.alpha + dst.alpha * inv
        );
    }

    /// Hard-mixes unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel thresholds [`#vividLight(Color)`] at `0.5` to `0` or `1`.
    /// Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color hardMix(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                hardMixChannel(src.red, dst.red),
                hardMixChannel(src.green, dst.green),
                hardMixChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Combines one pair of unassociated linear primaries for [`#hardMix(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float hardMixChannel(float source, float destination) {
        return vividLightChannel(source, destination) < 0.5f ? 0.0f : 1.0f;
    }

    /// Plus-darkens unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `destination + source - 1`. Both colors are converted
    /// through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color plusDarker(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                dst.red + src.red - 1.0f,
                dst.green + src.green - 1.0f,
                dst.blue + src.blue - 1.0f,
                src.alpha + dst.alpha * inv
        );
    }

    /// Plus-lightens unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `destination + source`. Both colors are converted through
    /// [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color plusLighter(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                dst.red + src.red,
                dst.green + src.green,
                dst.blue + src.blue,
                src.alpha + dst.alpha * inv
        );
    }

    /// Negates unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `1 - abs(1 - destination - source)`. Both colors are
    /// converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color negation(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                1.0f - Math.abs(1.0f - dst.red - src.red),
                1.0f - Math.abs(1.0f - dst.green - src.green),
                1.0f - Math.abs(1.0f - dst.blue - src.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Phoenix-blends unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `min(source, destination) - max(source, destination) + 1`.
    /// Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color phoenix(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                phoenixChannel(src.red, dst.red),
                phoenixChannel(src.green, dst.green),
                phoenixChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Combines one pair of unassociated linear primaries for [`#phoenix(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float phoenixChannel(float source, float destination) {
        return Math.min(source, destination) - Math.max(source, destination) + 1.0f;
    }

    /// Reflects unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `destination * destination / (1 - source)` unless the source
    /// is at least `1`, in which case the result is `1`. Both colors are converted
    /// through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color reflect(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                reflectChannel(src.red, dst.red),
                reflectChannel(src.green, dst.green),
                reflectChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Combines one pair of unassociated linear primaries for [`#reflect(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float reflectChannel(float source, float destination) {
        if (source >= 1.0f) {
            return 1.0f;
        }
        return destination * destination / (1.0f - source);
    }

    /// Glows unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `source * source / (1 - destination)` unless the destination
    /// is at least `1`, in which case the result is `1`. Both colors are converted
    /// through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color glow(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                glowChannel(src.red, dst.red),
                glowChannel(src.green, dst.green),
                glowChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Combines one pair of unassociated linear primaries for [`#glow(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float glowChannel(float source, float destination) {
        if (destination >= 1.0f) {
            return 1.0f;
        }
        return source * source / (1.0f - destination);
    }

    /// Freezes unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `1 - (1 - destination)^2 / source` unless the source is at
    /// most `0`, in which case a destination of `1` yields `1` and any darker
    /// destination yields `0`. Both colors are converted through
    /// [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color freeze(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                freezeChannel(src.red, dst.red),
                freezeChannel(src.green, dst.green),
                freezeChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Combines one pair of unassociated linear primaries for [`#freeze(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float freezeChannel(float source, float destination) {
        if (source <= 0.0f) {
            return destination >= 1.0f ? 1.0f : 0.0f;
        }
        float inverse = 1.0f - destination;
        return 1.0f - inverse * inverse / source;
    }

    /// Heats unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is [`#freeze(Color)`] with the source and destination swapped.
    /// Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color heat(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                freezeChannel(dst.red, src.red),
                freezeChannel(dst.green, src.green),
                freezeChannel(dst.blue, src.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Averages unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `(source + destination) / 2`. Both colors are converted
    /// through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color average(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                (src.red + dst.red) * 0.5f,
                (src.green + dst.green) * 0.5f,
                (src.blue + dst.blue) * 0.5f,
                src.alpha + dst.alpha * inv
        );
    }

    /// Subtracts this color from the backdrop and uses source-over coverage.
    ///
    /// Each channel is `destination - source`. Both colors are converted through
    /// [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color subtract(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                dst.red - src.red,
                dst.green - src.green,
                dst.blue - src.blue,
                src.alpha + dst.alpha * inv
        );
    }

    /// Divides the backdrop by this color and uses source-over coverage.
    ///
    /// Each channel is `destination / source` unless the source is `0`, in which
    /// case a destination of `0` yields `0` and any other destination yields `1`.
    /// Both colors are converted through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color divide(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                divideChannel(src.red, dst.red),
                divideChannel(src.green, dst.green),
                divideChannel(src.blue, dst.blue),
                src.alpha + dst.alpha * inv
        );
    }

    /// Combines one pair of unassociated linear primaries for [`#divide(Color)`].
    ///
    /// @param source the source primary
    /// @param destination the destination primary
    /// @return the blended primary
    private static float divideChannel(float source, float destination) {
        if (source == 0.0f) {
            return destination == 0.0f ? 0.0f : 1.0f;
        }
        return destination / source;
    }

    /// Grain-extracts unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `destination - source + 0.5`. Both colors are converted
    /// through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color grainExtract(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                dst.red - src.red + 0.5f,
                dst.green - src.green + 0.5f,
                dst.blue - src.blue + 0.5f,
                src.alpha + dst.alpha * inv
        );
    }

    /// Grain-merges unassociated linear primaries and uses source-over coverage.
    ///
    /// Each channel is `destination + source - 0.5`. Both colors are converted
    /// through [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color grainMerge(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                dst.red + src.red - 0.5f,
                dst.green + src.green - 0.5f,
                dst.blue + src.blue - 0.5f,
                src.alpha + dst.alpha * inv
        );
    }

    /// Inverse-subtracts the backdrop from this color and uses source-over coverage.
    ///
    /// Each channel is `source - destination`. Both colors are converted through
    /// [`#toExtendedLinear()`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color inverseSubtract(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        Color src = toExtendedLinear();
        Color dst = backdrop.toExtendedLinear();
        float inv = 1.0f - src.alpha;
        return new Color(
                ColorEncoding.EXTENDED_LINEAR,
                src.red - dst.red,
                src.green - dst.green,
                src.blue - dst.blue,
                src.alpha + dst.alpha * inv
        );
    }

    /// Replaces `backdrop` HSV value with this color's sRGB HSV value.
    ///
    /// Hue and saturation are taken from `backdrop`. This is the GIMP/Photoshop
    /// non-separable `Value` blend.
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color hsvValue(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        float[] dest = backdrop.toHsv();
        return fromHsv(dest[0], dest[1], toHsv()[2], backdrop.toSrgb().alpha);
    }

    /// Replaces `backdrop` HSV hue with this color's sRGB HSV hue.
    ///
    /// Saturation and value are taken from `backdrop`.
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color hsvHue(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        float[] dest = backdrop.toHsv();
        return fromHsv(toHsv()[0], dest[1], dest[2], backdrop.toSrgb().alpha);
    }

    /// Replaces `backdrop` HSV saturation with this color's sRGB HSV saturation.
    ///
    /// Hue and value are taken from `backdrop`.
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color hsvSaturation(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        float[] dest = backdrop.toHsv();
        return fromHsv(dest[0], toHsv()[1], dest[2], backdrop.toSrgb().alpha);
    }

    /// Replaces `backdrop` HSV hue and saturation with this color's sRGB HSV hue
    /// and saturation.
    ///
    /// Value is taken from `backdrop`. This is the HSV counterpart of [`#color(Color)`].
    ///
    /// @param backdrop the destination color
    /// @return the composite
    public Color hsvColor(Color backdrop) {
        Objects.requireNonNull(backdrop, "backdrop");
        float[] source = toHsv();
        float[] dest = backdrop.toHsv();
        return fromHsv(source[0], source[1], dest[2], backdrop.toSrgb().alpha);
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

    /// Returns the sRGB photographic negative of this color, preserving alpha.
    ///
    /// @return the inverted sRGB color
    public Color invert() {
        Color srgb = toSrgb();
        return srgb(1.0f - srgb.red, 1.0f - srgb.green, 1.0f - srgb.blue, srgb.alpha);
    }

    /// Returns the sRGB complementary color on the HSL hue wheel, preserving alpha.
    ///
    /// @return the complementary sRGB color
    public Color complementary() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 180.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns a lighter sRGB copy by adding `amount` to HSL lightness.
    ///
    /// @param amount the lightness increase in `[0, 1]`
    /// @return the lightened sRGB color
    public Color lighten(float amount) {
        if (!Float.isFinite(amount) || amount < 0.0f || amount > 1.0f) {
            throw new IllegalArgumentException("amount must be finite and in [0, 1]");
        }
        float[] hsl = toHsl();
        return fromHsl(hsl[0], hsl[1], Math.min(1.0f, hsl[2] + amount), toSrgb().alpha);
    }

    /// Returns a darker sRGB copy by subtracting `amount` from HSL lightness.
    ///
    /// @param amount the lightness decrease in `[0, 1]`
    /// @return the darkened sRGB color
    public Color darken(float amount) {
        if (!Float.isFinite(amount) || amount < 0.0f || amount > 1.0f) {
            throw new IllegalArgumentException("amount must be finite and in [0, 1]");
        }
        float[] hsl = toHsl();
        return fromHsl(hsl[0], hsl[1], Math.max(0.0f, hsl[2] - amount), toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation increased by `amount`.
    ///
    /// @param amount the saturation increase in `[0, 1]`
    /// @return the saturated sRGB color
    public Color saturate(float amount) {
        if (!Float.isFinite(amount) || amount < 0.0f || amount > 1.0f) {
            throw new IllegalArgumentException("amount must be finite and in [0, 1]");
        }
        float[] hsl = toHsl();
        return fromHsl(hsl[0], Math.min(1.0f, hsl[1] + amount), hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation decreased by `amount`.
    ///
    /// @param amount the saturation decrease in `[0, 1]`
    /// @return the desaturated sRGB color
    public Color desaturate(float amount) {
        if (!Float.isFinite(amount) || amount < 0.0f || amount > 1.0f) {
            throw new IllegalArgumentException("amount must be finite and in [0, 1]");
        }
        float[] hsl = toHsl();
        return fromHsl(hsl[0], Math.max(0.0f, hsl[1] - amount), hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL hue rotated by `degrees`.
    ///
    /// @param degrees the signed hue shift
    /// @return the recolored sRGB color
    public Color hueRotate(float degrees) {
        if (!Float.isFinite(degrees)) {
            throw new IllegalArgumentException("degrees must be finite");
        }
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + degrees, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with the given absolute HSL hue.
    ///
    /// @param hueDegrees the hue in degrees
    /// @return the recolored sRGB color
    public Color withHue(float hueDegrees) {
        if (!Float.isFinite(hueDegrees)) {
            throw new IllegalArgumentException("hue must be finite");
        }
        float[] hsl = toHsl();
        return fromHsl(hueDegrees, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with the given absolute HSL saturation.
    ///
    /// @param saturation the saturation in `[0, 1]`
    /// @return the recolored sRGB color
    public Color withSaturation(float saturation) {
        float[] hsl = toHsl();
        return fromHsl(hsl[0], saturation, hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with the given absolute HSL lightness.
    ///
    /// @param lightness the lightness in `[0, 1]`
    /// @return the recolored sRGB color
    public Color withLightness(float lightness) {
        float[] hsl = toHsl();
        return fromHsl(hsl[0], hsl[1], lightness, toSrgb().alpha);
    }

    /// Returns an sRGB copy with the given absolute HSV value.
    ///
    /// @param value the value in `[0, 1]`
    /// @return the recolored sRGB color
    public Color withHsvValue(float value) {
        float[] hsv = toHsv();
        return fromHsv(hsv[0], hsv[1], value, toSrgb().alpha);
    }

    /// Returns an sRGB copy with the given absolute HSV hue.
    ///
    /// @param hueDegrees the hue in degrees
    /// @return the recolored sRGB color
    public Color withHsvHue(float hueDegrees) {
        if (!Float.isFinite(hueDegrees)) {
            throw new IllegalArgumentException("hue must be finite");
        }
        float[] hsv = toHsv();
        return fromHsv(hueDegrees, hsv[1], hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with the given absolute HSV saturation.
    ///
    /// @param saturation the saturation in `[0, 1]`
    /// @return the recolored sRGB color
    public Color withHsvSaturation(float saturation) {
        float[] hsv = toHsv();
        return fromHsv(hsv[0], saturation, hsv[2], toSrgb().alpha);
    }

    /// Returns the sRGB complementary color on the HSV hue wheel, preserving alpha.
    ///
    /// @return the complementary sRGB color
    public Color hsvComplementary() {
        float[] hsv = toHsv();
        return fromHsv(hsv[0] + 180.0f, hsv[1], hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV hue rotated by `degrees`.
    ///
    /// @param degrees the signed hue shift
    /// @return the recolored sRGB color
    public Color hsvRotate(float degrees) {
        if (!Float.isFinite(degrees)) {
            throw new IllegalArgumentException("degrees must be finite");
        }
        float[] hsv = toHsv();
        return fromHsv(hsv[0] + degrees, hsv[1], hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV saturation increased by `amount`.
    ///
    /// @param amount the saturation increase in `[0, 1]`
    /// @return the saturated sRGB color
    public Color hsvSaturate(float amount) {
        if (!Float.isFinite(amount) || amount < 0.0f || amount > 1.0f) {
            throw new IllegalArgumentException("amount must be finite and in [0, 1]");
        }
        float[] hsv = toHsv();
        return fromHsv(hsv[0], Math.min(1.0f, hsv[1] + amount), hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV saturation decreased by `amount`.
    ///
    /// @param amount the saturation decrease in `[0, 1]`
    /// @return the desaturated sRGB color
    public Color hsvDesaturate(float amount) {
        if (!Float.isFinite(amount) || amount < 0.0f || amount > 1.0f) {
            throw new IllegalArgumentException("amount must be finite and in [0, 1]");
        }
        float[] hsv = toHsv();
        return fromHsv(hsv[0], Math.max(0.0f, hsv[1] - amount), hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV value increased by `amount`.
    ///
    /// @param amount the value increase in `[0, 1]`
    /// @return the brightened sRGB color
    public Color hsvBrighten(float amount) {
        if (!Float.isFinite(amount) || amount < 0.0f || amount > 1.0f) {
            throw new IllegalArgumentException("amount must be finite and in [0, 1]");
        }
        float[] hsv = toHsv();
        return fromHsv(hsv[0], hsv[1], Math.min(1.0f, hsv[2] + amount), toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV value decreased by `amount`.
    ///
    /// @param amount the value decrease in `[0, 1]`
    /// @return the darkened sRGB color
    public Color hsvDarken(float amount) {
        if (!Float.isFinite(amount) || amount < 0.0f || amount > 1.0f) {
            throw new IllegalArgumentException("amount must be finite and in [0, 1]");
        }
        float[] hsv = toHsv();
        return fromHsv(hsv[0], hsv[1], Math.max(0.0f, hsv[2] - amount), toSrgb().alpha);
    }

    /// Interpolates this color with `other` in HSV, then returns extended-linear sRGB.
    ///
    /// Hue travels the shorter arc. `t` is unclamped.
    ///
    /// @param other the end color
    /// @param t the mix factor; `0` is this color and `1` is `other`
    /// @return the interpolated extended-linear color
    public Color interpolateHsv(Color other, float t) {
        Objects.requireNonNull(other, "other");
        if (!Float.isFinite(t)) {
            throw new IllegalArgumentException("t must be finite");
        }
        float[] start = toHsv();
        float[] end = other.toHsv();
        float complement = 1.0f - t;
        float hueDelta = end[0] - start[0];
        if (hueDelta > 180.0f) {
            hueDelta -= 360.0f;
        } else if (hueDelta < -180.0f) {
            hueDelta += 360.0f;
        }
        Color mixed = fromHsv(
                start[0] + hueDelta * t,
                start[1] * complement + end[1] * t,
                start[2] * complement + end[2] * t,
                toSrgb().alpha * complement + other.toSrgb().alpha * t
        );
        return mixed.toExtendedLinear();
    }

    /// Interpolates this color with `other` in HSL, then returns extended-linear sRGB.
    ///
    /// Hue travels the shorter arc. `t` is unclamped.
    ///
    /// @param other the end color
    /// @param t the mix factor; `0` is this color and `1` is `other`
    /// @return the interpolated extended-linear color
    public Color interpolateHsl(Color other, float t) {
        Objects.requireNonNull(other, "other");
        if (!Float.isFinite(t)) {
            throw new IllegalArgumentException("t must be finite");
        }
        float[] start = toHsl();
        float[] end = other.toHsl();
        float complement = 1.0f - t;
        float hueDelta = end[0] - start[0];
        if (hueDelta > 180.0f) {
            hueDelta -= 360.0f;
        } else if (hueDelta < -180.0f) {
            hueDelta += 360.0f;
        }
        Color mixed = fromHsl(
                start[0] + hueDelta * t,
                start[1] * complement + end[1] * t,
                start[2] * complement + end[2] * t,
                toSrgb().alpha * complement + other.toSrgb().alpha * t
        );
        return mixed.toExtendedLinear();
    }

    /// Returns an sRGB copy with HSV value inverted.
    ///
    /// @return the value-inverted sRGB color
    public Color hsvInvertValue() {
        float[] hsv = toHsv();
        return fromHsv(hsv[0], hsv[1], 1.0f - hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV saturation inverted.
    ///
    /// @return the saturation-inverted sRGB color
    public Color hsvInvertSaturation() {
        float[] hsv = toHsv();
        return fromHsv(hsv[0], 1.0f - hsv[1], hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with the given absolute HSV components.
    ///
    /// @param hueDegrees the hue in degrees
    /// @param saturation the saturation in `[0, 1]`
    /// @param value the value in `[0, 1]`
    /// @return the recolored sRGB color
    public Color withHsv(float hueDegrees, float saturation, float value) {
        if (!Float.isFinite(hueDegrees)) {
            throw new IllegalArgumentException("hue must be finite");
        }
        if (!Float.isFinite(saturation) || saturation < 0.0f || saturation > 1.0f) {
            throw new IllegalArgumentException("saturation must be finite and in [0, 1]");
        }
        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException("value must be finite and in [0, 1]");
        }
        return fromHsv(hueDegrees, saturation, value, toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL lightness inverted.
    ///
    /// @return the lightness-inverted sRGB color
    public Color hslInvertLightness() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0], hsl[1], 1.0f - hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation inverted.
    ///
    /// @return the saturation-inverted sRGB color
    public Color hslInvertSaturation() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0], 1.0f - hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with the given absolute HSL components.
    ///
    /// @param hueDegrees the hue in degrees
    /// @param saturation the saturation in `[0, 1]`
    /// @param lightness the lightness in `[0, 1]`
    /// @return the recolored sRGB color
    public Color withHsl(float hueDegrees, float saturation, float lightness) {
        if (!Float.isFinite(hueDegrees)) {
            throw new IllegalArgumentException("hue must be finite");
        }
        if (!Float.isFinite(saturation) || saturation < 0.0f || saturation > 1.0f) {
            throw new IllegalArgumentException("saturation must be finite and in [0, 1]");
        }
        if (!Float.isFinite(lightness) || lightness < 0.0f || lightness > 1.0f) {
            throw new IllegalArgumentException("lightness must be finite and in [0, 1]");
        }
        return fromHsl(hueDegrees, saturation, lightness, toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV hue reflected about `0` degrees.
    ///
    /// This is `360 - hue`, not the complementary `hue + 180` rotation.
    ///
    /// @return the hue-reflected sRGB color
    public Color hsvInvertHue() {
        float[] hsv = toHsv();
        return fromHsv(360.0f - hsv[0], hsv[1], hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL hue reflected about `0` degrees.
    ///
    /// This is `360 - hue`, not the complementary `hue + 180` rotation.
    ///
    /// @return the hue-reflected sRGB color
    public Color hslInvertHue() {
        float[] hsl = toHsl();
        return fromHsl(360.0f - hsl[0], hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue reflected about `0` degrees.
    ///
    /// This is `360 - hue`, not the complementary `hue + 180` rotation.
    ///
    /// @return the hue-reflected sRGB color
    public Color oklchInvertHue() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], 360.0f - oklch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLCH lightness inverted.
    ///
    /// Lightness is reflected as `1 - L`.
    ///
    /// @return the lightness-inverted sRGB color
    public Color oklchInvertLightness() {
        float[] oklch = toOklch();
        return fromOklch(1.0f - oklch[0], oklch[1], oklch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE L* inverted.
    ///
    /// Lightness is reflected as `100 - L`.
    ///
    /// @return the lightness-inverted sRGB color
    public Color cieLabInvertL() {
        float[] lab = toCieLab();
        return fromCieLab(100.0f - lab[0], lab[1], lab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue reflected about `0` degrees.
    ///
    /// This is `360 - hue`, not the complementary `hue + 180` rotation.
    ///
    /// @return the hue-reflected sRGB color
    public Color cieLchInvertHue() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], 360.0f - lch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE a* negated.
    ///
    /// @return the a*-negated sRGB color
    public Color cieLabInvertA() {
        float[] lab = toCieLab();
        return fromCieLab(lab[0], -lab[1], lab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE b* negated.
    ///
    /// @return the b*-negated sRGB color
    public Color cieLabInvertB() {
        float[] lab = toCieLab();
        return fromCieLab(lab[0], lab[1], -lab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLab a negated.
    ///
    /// @return the a-negated sRGB color
    public Color oklabInvertA() {
        float[] oklab = toOklab();
        return fromOklab(oklab[0], -oklab[1], oklab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLab b negated.
    ///
    /// @return the b-negated sRGB color
    public Color oklabInvertB() {
        float[] oklab = toOklab();
        return fromOklab(oklab[0], oklab[1], -oklab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLab lightness inverted.
    ///
    /// Lightness is reflected as `1 - L`.
    ///
    /// @return the lightness-inverted sRGB color
    public Color oklabInvertLightness() {
        float[] oklab = toOklab();
        return fromOklab(1.0f - oklab[0], oklab[1], oklab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE a* and b* both negated.
    ///
    /// @return the a*/b*-negated sRGB color
    public Color cieLabInvertAb() {
        float[] lab = toCieLab();
        return fromCieLab(lab[0], -lab[1], -lab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLab a and b both negated.
    ///
    /// @return the a/b-negated sRGB color
    public Color oklabInvertAb() {
        float[] oklab = toOklab();
        return fromOklab(oklab[0], -oklab[1], -oklab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns the complementary color on the OKLCH hue wheel, preserving alpha.
    ///
    /// @return the complementary sRGB color
    public Color oklchComplementary() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 180.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns the complementary color on the CIE LCh hue wheel, preserving alpha.
    ///
    /// @return the complementary sRGB color
    public Color cieLchComplementary() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 180.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh lightness inverted.
    ///
    /// Lightness is reflected as `100 - L`.
    ///
    /// @return the lightness-inverted sRGB color
    public Color cieLchInvertLightness() {
        float[] lch = toCieLch();
        return fromCieLch(100.0f - lch[0], lch[1], lch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE L* inverted in Luv.
    ///
    /// Lightness is reflected as `100 - L`.
    ///
    /// @return the lightness-inverted sRGB color
    public Color cieLuvInvertL() {
        float[] luv = toCieLuv();
        return fromCieLuv(100.0f - luv[0], luv[1], luv[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE u* negated.
    ///
    /// @return the u*-negated sRGB color
    public Color cieLuvInvertU() {
        float[] luv = toCieLuv();
        return fromCieLuv(luv[0], -luv[1], luv[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE v* negated.
    ///
    /// @return the v*-negated sRGB color
    public Color cieLuvInvertV() {
        float[] luv = toCieLuv();
        return fromCieLuv(luv[0], luv[1], -luv[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE u* and v* both negated.
    ///
    /// @return the u*/v*-negated sRGB color
    public Color cieLuvInvertUv() {
        float[] luv = toCieLuv();
        return fromCieLuv(luv[0], -luv[1], -luv[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE L* inverted and v* negated.
    ///
    /// Lightness is reflected as `100 - L`.
    ///
    /// @return the L*/v*-inverted sRGB color
    public Color cieLuvInvertLv() {
        float[] luv = toCieLuv();
        return fromCieLuv(100.0f - luv[0], luv[1], -luv[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE L* inverted and u* negated.
    ///
    /// Lightness is reflected as `100 - L`.
    ///
    /// @return the L*/u*-inverted sRGB color
    public Color cieLuvInvertLu() {
        float[] luv = toCieLuv();
        return fromCieLuv(100.0f - luv[0], -luv[1], luv[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE L* inverted and u* and v* both negated.
    ///
    /// Lightness is reflected as `100 - L`.
    ///
    /// @return the L*/u*/v*-inverted sRGB color
    public Color cieLuvInvertLuv() {
        float[] luv = toCieLuv();
        return fromCieLuv(100.0f - luv[0], -luv[1], -luv[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLab L inverted and a negated.
    ///
    /// Lightness is reflected as `1 - L`.
    ///
    /// @return the L/a-inverted sRGB color
    public Color oklabInvertLa() {
        float[] oklab = toOklab();
        return fromOklab(1.0f - oklab[0], -oklab[1], oklab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLab L inverted and b negated.
    ///
    /// Lightness is reflected as `1 - L`.
    ///
    /// @return the L/b-inverted sRGB color
    public Color oklabInvertLb() {
        float[] oklab = toOklab();
        return fromOklab(1.0f - oklab[0], oklab[1], -oklab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLab L inverted and a and b both negated.
    ///
    /// Lightness is reflected as `1 - L`.
    ///
    /// @return the L/a/b-inverted sRGB color
    public Color oklabInvertLab() {
        float[] oklab = toOklab();
        return fromOklab(1.0f - oklab[0], -oklab[1], -oklab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE L* inverted and a* negated.
    ///
    /// Lightness is reflected as `100 - L`.
    ///
    /// @return the L*/a*-inverted sRGB color
    public Color cieLabInvertLa() {
        float[] lab = toCieLab();
        return fromCieLab(100.0f - lab[0], -lab[1], lab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE L* inverted and b* negated.
    ///
    /// Lightness is reflected as `100 - L`.
    ///
    /// @return the L*/b*-inverted sRGB color
    public Color cieLabInvertLb() {
        float[] lab = toCieLab();
        return fromCieLab(100.0f - lab[0], lab[1], -lab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE L* inverted and a* and b* both negated.
    ///
    /// Lightness is reflected as `100 - L`.
    ///
    /// @return the L*/a*/b*-inverted sRGB color
    public Color cieLabInvertLab() {
        float[] lab = toCieLab();
        return fromCieLab(100.0f - lab[0], -lab[1], -lab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh lightness inverted and hue reflected.
    ///
    /// Lightness is reflected as `100 - L`. Hue is reflected as `360 - h`.
    ///
    /// @return the L/hue-inverted sRGB color
    public Color cieLchInvertLh() {
        float[] lch = toCieLch();
        return fromCieLch(100.0f - lch[0], lch[1], 360.0f - lch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLCH lightness inverted and hue reflected.
    ///
    /// Lightness is reflected as `1 - L`. Hue is reflected as `360 - h`.
    ///
    /// @return the L/hue-inverted sRGB color
    public Color oklchInvertLh() {
        float[] oklch = toOklch();
        return fromOklch(1.0f - oklch[0], oklch[1], 360.0f - oklch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue reflected and value inverted.
    ///
    /// Hue is reflected as `360 - h`. Value is reflected as `1 - v`.
    ///
    /// @return the hue/value-inverted sRGB color
    public Color hsvInvertHv() {
        float[] hsv = toHsv();
        return fromHsv(360.0f - hsv[0], hsv[1], 1.0f - hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL hue reflected and lightness inverted.
    ///
    /// Hue is reflected as `360 - h`. Lightness is reflected as `1 - l`.
    ///
    /// @return the hue/lightness-inverted sRGB color
    public Color hslInvertHl() {
        float[] hsl = toHsl();
        return fromHsl(360.0f - hsl[0], hsl[1], 1.0f - hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV hue reflected and saturation inverted.
    ///
    /// Hue is reflected as `360 - h`. Saturation is reflected as `1 - s`.
    ///
    /// @return the hue/saturation-inverted sRGB color
    public Color hsvInvertHs() {
        float[] hsv = toHsv();
        return fromHsv(360.0f - hsv[0], 1.0f - hsv[1], hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL hue reflected and saturation inverted.
    ///
    /// Hue is reflected as `360 - h`. Saturation is reflected as `1 - s`.
    ///
    /// @return the hue/saturation-inverted sRGB color
    public Color hslInvertHs() {
        float[] hsl = toHsl();
        return fromHsl(360.0f - hsl[0], 1.0f - hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV saturation and value inverted.
    ///
    /// Saturation is reflected as `1 - s`. Value is reflected as `1 - v`.
    ///
    /// @return the saturation/value-inverted sRGB color
    public Color hsvInvertSv() {
        float[] hsv = toHsv();
        return fromHsv(hsv[0], 1.0f - hsv[1], 1.0f - hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation and lightness inverted.
    ///
    /// Saturation is reflected as `1 - s`. Lightness is reflected as `1 - l`.
    ///
    /// @return the saturation/lightness-inverted sRGB color
    public Color hslInvertSl() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0], 1.0f - hsl[1], 1.0f - hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV hue reflected and saturation and value inverted.
    ///
    /// Hue is reflected as `360 - h`. Saturation is `1 - s`. Value is `1 - v`.
    ///
    /// @return the fully inverted HSV sRGB color
    public Color hsvInvertHsv() {
        float[] hsv = toHsv();
        return fromHsv(360.0f - hsv[0], 1.0f - hsv[1], 1.0f - hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL hue reflected and saturation and lightness inverted.
    ///
    /// Hue is reflected as `360 - h`. Saturation is `1 - s`. Lightness is `1 - l`.
    ///
    /// @return the fully inverted HSL sRGB color
    public Color hslInvertHsl() {
        float[] hsl = toHsl();
        return fromHsl(360.0f - hsl[0], 1.0f - hsl[1], 1.0f - hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH lightness inverted and chroma cleared.
    ///
    /// Lightness is reflected as `1 - L`. Chroma is set to `0`.
    ///
    /// @return the lightness-inverted achromatic sRGB color
    public Color oklchInvertLc() {
        float[] oklch = toOklch();
        return fromOklch(1.0f - oklch[0], 0.0f, oklch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh lightness inverted and chroma cleared.
    ///
    /// Lightness is reflected as `100 - L`. Chroma is set to `0`.
    ///
    /// @return the lightness-inverted achromatic sRGB color
    public Color cieLchInvertLc() {
        float[] lch = toCieLch();
        return fromCieLch(100.0f - lch[0], 0.0f, lch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLab lightness inverted and a and b cleared.
    ///
    /// Lightness is reflected as `1 - L`.
    ///
    /// @return the lightness-inverted achromatic sRGB color
    public Color oklabInvertLc() {
        float[] oklab = toOklab();
        return fromOklab(1.0f - oklab[0], 0.0f, 0.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE L* inverted and a* and b* cleared.
    ///
    /// Lightness is reflected as `100 - L`.
    ///
    /// @return the lightness-inverted achromatic sRGB color
    public Color cieLabInvertLc() {
        float[] lab = toCieLab();
        return fromCieLab(100.0f - lab[0], 0.0f, 0.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE L* inverted and u* and v* cleared.
    ///
    /// Lightness is reflected as `100 - L`.
    ///
    /// @return the lightness-inverted achromatic sRGB color
    public Color cieLuvInvertLc() {
        float[] luv = toCieLuv();
        return fromCieLuv(100.0f - luv[0], 0.0f, 0.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh chroma cleared.
    ///
    /// @return the achromatic sRGB color at the same lightness
    public Color cieLchInvertC() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], 0.0f, lch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLCH chroma cleared.
    ///
    /// @return the achromatic sRGB color at the same lightness
    public Color oklchInvertC() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], 0.0f, oklch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLab a and b cleared.
    ///
    /// @return the achromatic sRGB color at the same lightness
    public Color oklabInvertC() {
        float[] oklab = toOklab();
        return fromOklab(oklab[0], 0.0f, 0.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE a* and b* cleared.
    ///
    /// @return the achromatic sRGB color at the same lightness
    public Color cieLabInvertC() {
        float[] lab = toCieLab();
        return fromCieLab(lab[0], 0.0f, 0.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE u* and v* cleared.
    ///
    /// @return the achromatic sRGB color at the same lightness
    public Color cieLuvInvertC() {
        float[] luv = toCieLuv();
        return fromCieLuv(luv[0], 0.0f, 0.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV saturation cleared.
    ///
    /// @return the achromatic sRGB color at the same value
    public Color hsvInvertC() {
        float[] hsv = toHsv();
        return fromHsv(hsv[0], 0.0f, hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation cleared.
    ///
    /// @return the achromatic sRGB color at the same lightness
    public Color hslInvertC() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0], 0.0f, hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH chroma cleared and hue reflected.
    ///
    /// Chroma is set to `0`. Hue is reflected as `360 - h`.
    ///
    /// @return the hue-reflected achromatic sRGB color
    public Color oklchInvertCh() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], 0.0f, 360.0f - oklch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh chroma cleared and hue reflected.
    ///
    /// Chroma is set to `0`. Hue is reflected as `360 - h`.
    ///
    /// @return the hue-reflected achromatic sRGB color
    public Color cieLchInvertCh() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], 0.0f, 360.0f - lch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV saturation cleared and hue reflected.
    ///
    /// Saturation is set to `0`. Hue is reflected as `360 - h`.
    ///
    /// @return the hue-reflected achromatic sRGB color
    public Color hsvInvertCh() {
        float[] hsv = toHsv();
        return fromHsv(360.0f - hsv[0], 0.0f, hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation cleared and hue reflected.
    ///
    /// Saturation is set to `0`. Hue is reflected as `360 - h`.
    ///
    /// @return the hue-reflected achromatic sRGB color
    public Color hslInvertCh() {
        float[] hsl = toHsl();
        return fromHsl(360.0f - hsl[0], 0.0f, hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH lightness inverted, chroma cleared, and hue reflected.
    ///
    /// Lightness is `1 - L`. Chroma is `0`. Hue is `360 - h`.
    ///
    /// @return the fully inverted achromatic sRGB color
    public Color oklchInvertLch() {
        float[] oklch = toOklch();
        return fromOklch(1.0f - oklch[0], 0.0f, 360.0f - oklch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh lightness inverted, chroma cleared, and hue reflected.
    ///
    /// Lightness is `100 - L`. Chroma is `0`. Hue is `360 - h`.
    ///
    /// @return the fully inverted achromatic sRGB color
    public Color cieLchInvertLch() {
        float[] lch = toCieLch();
        return fromCieLch(100.0f - lch[0], 0.0f, 360.0f - lch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV saturation cleared, value inverted, and hue reflected.
    ///
    /// Saturation is `0`. Value is `1 - v`. Hue is `360 - h`.
    ///
    /// @return the fully inverted achromatic sRGB color
    public Color hsvInvertLch() {
        float[] hsv = toHsv();
        return fromHsv(360.0f - hsv[0], 0.0f, 1.0f - hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation cleared, lightness inverted, and hue reflected.
    ///
    /// Saturation is `0`. Lightness is `1 - l`. Hue is `360 - h`.
    ///
    /// @return the fully inverted achromatic sRGB color
    public Color hslInvertLch() {
        float[] hsl = toHsl();
        return fromHsl(360.0f - hsl[0], 0.0f, 1.0f - hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV saturation cleared and value inverted.
    ///
    /// Saturation is `0`. Value is `1 - v`.
    ///
    /// @return the value-inverted achromatic sRGB color
    public Color hsvInvertLc() {
        float[] hsv = toHsv();
        return fromHsv(hsv[0], 0.0f, 1.0f - hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation cleared and lightness inverted.
    ///
    /// Saturation is `0`. Lightness is `1 - l`.
    ///
    /// @return the lightness-inverted achromatic sRGB color
    public Color hslInvertLc() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0], 0.0f, 1.0f - hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH lightness inverted and complementary hue.
    ///
    /// Lightness is `1 - L`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary lightness-inverted sRGB color
    public Color oklchComplementaryInvertL() {
        float[] oklch = toOklch();
        return fromOklch(1.0f - oklch[0], oklch[1], oklch[2] + 180.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh lightness inverted and complementary hue.
    ///
    /// Lightness is `100 - L`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary lightness-inverted sRGB color
    public Color cieLchComplementaryInvertL() {
        float[] lch = toCieLch();
        return fromCieLch(100.0f - lch[0], lch[1], lch[2] + 180.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with OKLCH chroma cleared and complementary hue.
    ///
    /// Chroma is `0`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary achromatic sRGB color
    public Color oklchComplementaryInvertC() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], 0.0f, oklch[2] + 180.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh chroma cleared and complementary hue.
    ///
    /// Chroma is `0`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary achromatic sRGB color
    public Color cieLchComplementaryInvertC() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], 0.0f, lch[2] + 180.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV saturation cleared and complementary hue.
    ///
    /// Saturation is `0`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary achromatic sRGB color
    public Color hsvComplementaryInvertC() {
        float[] hsv = toHsv();
        return fromHsv(hsv[0] + 180.0f, 0.0f, hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation cleared and complementary hue.
    ///
    /// Saturation is `0`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary achromatic sRGB color
    public Color hslComplementaryInvertC() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 180.0f, 0.0f, hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV value inverted and complementary hue.
    ///
    /// Value is `1 - V`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary value-inverted sRGB color
    public Color hsvComplementaryInvertV() {
        float[] hsv = toHsv();
        return fromHsv(hsv[0] + 180.0f, hsv[1], 1.0f - hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL lightness inverted and complementary hue.
    ///
    /// Lightness is `1 - L`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary lightness-inverted sRGB color
    public Color hslComplementaryInvertL() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 180.0f, hsl[1], 1.0f - hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV saturation cleared, value inverted, and complementary hue.
    ///
    /// Saturation is `0`. Value is `1 - V`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary achromatic value-inverted sRGB color
    public Color hsvComplementaryInvertLc() {
        float[] hsv = toHsv();
        return fromHsv(hsv[0] + 180.0f, 0.0f, 1.0f - hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation cleared, lightness inverted, and complementary hue.
    ///
    /// Saturation is `0`. Lightness is `1 - L`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary achromatic lightness-inverted sRGB color
    public Color hslComplementaryInvertLc() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 180.0f, 0.0f, 1.0f - hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH lightness inverted, chroma cleared, and complementary hue.
    ///
    /// Lightness is `1 - L`. Chroma is `0`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary achromatic lightness-inverted sRGB color
    public Color oklchComplementaryInvertLc() {
        float[] oklch = toOklch();
        return fromOklch(1.0f - oklch[0], 0.0f, oklch[2] + 180.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh lightness inverted, chroma cleared, and complementary hue.
    ///
    /// Lightness is `100 - L`. Chroma is `0`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary achromatic lightness-inverted sRGB color
    public Color cieLchComplementaryInvertLc() {
        float[] lch = toCieLch();
        return fromCieLch(100.0f - lch[0], 0.0f, lch[2] + 180.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV saturation inverted and complementary hue.
    ///
    /// Saturation is `1 - S`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary saturation-inverted sRGB color
    public Color hsvComplementaryInvertS() {
        float[] hsv = toHsv();
        return fromHsv(hsv[0] + 180.0f, 1.0f - hsv[1], hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation inverted and complementary hue.
    ///
    /// Saturation is `1 - S`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary saturation-inverted sRGB color
    public Color hslComplementaryInvertS() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 180.0f, 1.0f - hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSV saturation and value inverted and complementary hue.
    ///
    /// Saturation is `1 - S`. Value is `1 - V`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary saturation-and-value-inverted sRGB color
    public Color hsvComplementaryInvertSv() {
        float[] hsv = toHsv();
        return fromHsv(hsv[0] + 180.0f, 1.0f - hsv[1], 1.0f - hsv[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with HSL saturation and lightness inverted and complementary hue.
    ///
    /// Saturation is `1 - S`. Lightness is `1 - L`. Hue is rotated by `180` degrees.
    ///
    /// @return the complementary saturation-and-lightness-inverted sRGB color
    public Color hslComplementaryInvertSl() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 180.0f, 1.0f - hsl[1], 1.0f - hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `90` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate90() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 90.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `90` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate90() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 90.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `90` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate90() {
        return hsvRotate(90.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `90` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate90() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 90.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `270` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate270() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 270.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `270` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate270() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 270.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `270` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate270() {
        return hsvRotate(270.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `270` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate270() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 270.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `45` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate45() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 45.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `45` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate45() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 45.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `45` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate45() {
        return hsvRotate(45.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `45` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate45() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 45.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `135` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate135() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 135.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `135` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate135() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 135.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `135` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate135() {
        return hsvRotate(135.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `135` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate135() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 135.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `225` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate225() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 225.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `225` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate225() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 225.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `225` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate225() {
        return hsvRotate(225.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `225` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate225() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 225.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `315` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate315() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 315.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `315` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate315() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 315.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `315` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate315() {
        return hsvRotate(315.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `315` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate315() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 315.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `30` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate30() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 30.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `30` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate30() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 30.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `30` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate30() {
        return hsvRotate(30.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `30` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate30() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 30.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `60` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate60() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 60.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `60` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate60() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 60.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `60` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate60() {
        return hsvRotate(60.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `60` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate60() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 60.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `120` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate120() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 120.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `120` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate120() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 120.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `120` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate120() {
        return hsvRotate(120.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `120` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate120() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 120.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `150` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate150() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 150.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `150` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate150() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 150.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `150` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate150() {
        return hsvRotate(150.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `150` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate150() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 150.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `75` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate75() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 75.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `75` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate75() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 75.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `75` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate75() {
        return hsvRotate(75.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `75` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate75() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 75.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `105` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate105() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 105.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `105` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate105() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 105.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `105` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate105() {
        return hsvRotate(105.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `105` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate105() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 105.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `165` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate165() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 165.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `165` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate165() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 165.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `165` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate165() {
        return hsvRotate(165.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `165` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate165() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 165.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `15` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate15() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 15.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `15` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate15() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 15.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `15` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate15() {
        return hsvRotate(15.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `15` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate15() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 15.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `195` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate195() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 195.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `195` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate195() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 195.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `195` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate195() {
        return hsvRotate(195.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `195` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate195() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 195.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `210` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate210() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 210.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `210` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate210() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 210.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `210` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate210() {
        return hsvRotate(210.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `210` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate210() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 210.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `240` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate240() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 240.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `240` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate240() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 240.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `240` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate240() {
        return hsvRotate(240.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `240` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate240() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 240.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `255` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate255() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 255.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `255` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate255() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 255.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `255` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate255() {
        return hsvRotate(255.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `255` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate255() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 255.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `300` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate300() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 300.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `300` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate300() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 300.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `300` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate300() {
        return hsvRotate(300.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `300` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate300() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 300.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `330` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate330() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 330.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `330` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate330() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 330.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `330` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate330() {
        return hsvRotate(330.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `330` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate330() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 330.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `345` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate345() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 345.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `345` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate345() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 345.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `345` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate345() {
        return hsvRotate(345.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `345` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate345() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 345.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `5` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate5() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 5.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `5` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate5() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 5.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `5` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate5() {
        return hsvRotate(5.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `5` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate5() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 5.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `10` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate10() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 10.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `10` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate10() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 10.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `10` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate10() {
        return hsvRotate(10.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `10` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate10() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 10.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `20` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate20() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 20.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `20` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate20() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 20.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `20` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate20() {
        return hsvRotate(20.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `20` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate20() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 20.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `25` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate25() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 25.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `25` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate25() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 25.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `25` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate25() {
        return hsvRotate(25.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `25` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate25() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 25.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `35` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate35() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 35.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `35` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate35() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 35.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `35` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate35() {
        return hsvRotate(35.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `35` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate35() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 35.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `40` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate40() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 40.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `40` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate40() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 40.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `40` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate40() {
        return hsvRotate(40.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `40` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate40() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 40.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `50` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate50() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 50.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `50` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate50() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 50.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `50` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate50() {
        return hsvRotate(50.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `50` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate50() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 50.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `55` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate55() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 55.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `55` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate55() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 55.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `55` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate55() {
        return hsvRotate(55.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `55` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate55() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 55.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `65` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate65() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 65.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `65` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate65() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 65.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `65` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate65() {
        return hsvRotate(65.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `65` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate65() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 65.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `70` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate70() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 70.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `70` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate70() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 70.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `70` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate70() {
        return hsvRotate(70.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `70` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate70() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 70.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `80` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate80() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 80.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `80` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate80() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 80.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `80` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate80() {
        return hsvRotate(80.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `80` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate80() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 80.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `85` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate85() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 85.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `85` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate85() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 85.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `85` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate85() {
        return hsvRotate(85.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `85` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate85() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 85.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `95` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate95() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 95.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `95` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate95() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 95.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `95` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate95() {
        return hsvRotate(95.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `95` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate95() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 95.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `100` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate100() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 100.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `100` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate100() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 100.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `100` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate100() {
        return hsvRotate(100.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `100` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate100() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 100.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `110` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate110() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 110.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `110` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate110() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 110.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `110` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate110() {
        return hsvRotate(110.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `110` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate110() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 110.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `115` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate115() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 115.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `115` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate115() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 115.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `115` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate115() {
        return hsvRotate(115.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `115` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate115() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 115.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `125` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate125() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 125.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `125` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate125() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 125.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `125` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate125() {
        return hsvRotate(125.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `125` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate125() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 125.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `130` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate130() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 130.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `130` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate130() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 130.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `130` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate130() {
        return hsvRotate(130.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `130` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate130() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 130.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `140` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate140() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 140.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `140` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate140() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 140.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `140` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate140() {
        return hsvRotate(140.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `140` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate140() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 140.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `145` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate145() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 145.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `145` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate145() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 145.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `145` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate145() {
        return hsvRotate(145.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `145` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate145() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 145.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `155` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate155() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 155.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `155` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate155() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 155.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `155` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate155() {
        return hsvRotate(155.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `155` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate155() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 155.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `160` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate160() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 160.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `160` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate160() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 160.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `160` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate160() {
        return hsvRotate(160.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `160` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate160() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 160.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `170` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate170() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 170.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `170` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate170() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 170.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `170` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate170() {
        return hsvRotate(170.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `170` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate170() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 170.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `175` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate175() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 175.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `175` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate175() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 175.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `175` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate175() {
        return hsvRotate(175.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `175` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate175() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 175.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `185` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate185() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 185.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `185` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate185() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 185.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `185` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate185() {
        return hsvRotate(185.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `185` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate185() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 185.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `190` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate190() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 190.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `190` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate190() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 190.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `190` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate190() {
        return hsvRotate(190.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `190` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate190() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 190.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `200` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate200() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 200.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `200` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate200() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 200.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `200` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate200() {
        return hsvRotate(200.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `200` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate200() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 200.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `205` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate205() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 205.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `205` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate205() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 205.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `205` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate205() {
        return hsvRotate(205.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `205` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate205() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 205.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `215` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate215() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 215.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `215` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate215() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 215.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `215` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate215() {
        return hsvRotate(215.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `215` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate215() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 215.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `220` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate220() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 220.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `220` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate220() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 220.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `220` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate220() {
        return hsvRotate(220.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `220` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate220() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 220.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `230` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate230() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 230.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `230` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate230() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 230.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `230` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate230() {
        return hsvRotate(230.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `230` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate230() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 230.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `235` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate235() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 235.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `235` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate235() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 235.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `235` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate235() {
        return hsvRotate(235.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `235` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate235() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 235.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `245` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate245() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 245.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `245` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate245() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 245.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `245` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate245() {
        return hsvRotate(245.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `245` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate245() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 245.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `250` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate250() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 250.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `250` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate250() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 250.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `250` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate250() {
        return hsvRotate(250.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `250` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate250() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 250.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `260` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate260() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 260.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `260` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate260() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 260.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `260` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate260() {
        return hsvRotate(260.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `260` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate260() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 260.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `265` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate265() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 265.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `265` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate265() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 265.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `265` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate265() {
        return hsvRotate(265.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `265` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate265() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 265.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `275` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate275() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 275.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `275` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate275() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 275.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `275` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate275() {
        return hsvRotate(275.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `275` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate275() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 275.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `280` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate280() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 280.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `280` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate280() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 280.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `280` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate280() {
        return hsvRotate(280.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `280` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate280() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 280.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `285` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate285() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 285.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `285` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate285() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 285.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `285` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate285() {
        return hsvRotate(285.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `285` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate285() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 285.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `290` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate290() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 290.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `290` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate290() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 290.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `290` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate290() {
        return hsvRotate(290.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `290` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate290() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 290.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `295` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate295() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 295.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `295` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate295() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 295.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `295` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate295() {
        return hsvRotate(295.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `295` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate295() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 295.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `305` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate305() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 305.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `305` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate305() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 305.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `305` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate305() {
        return hsvRotate(305.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `305` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate305() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 305.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `310` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate310() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 310.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `310` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate310() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 310.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `310` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate310() {
        return hsvRotate(310.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `310` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate310() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 310.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `320` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate320() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 320.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `320` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate320() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 320.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `320` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate320() {
        return hsvRotate(320.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `320` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate320() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 320.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `325` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate325() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 325.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `325` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate325() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 325.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `325` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate325() {
        return hsvRotate(325.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `325` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate325() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 325.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `335` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate335() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 335.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `335` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate335() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 335.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `335` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate335() {
        return hsvRotate(335.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `335` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate335() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 335.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `340` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate340() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 340.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `340` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate340() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 340.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `340` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate340() {
        return hsvRotate(340.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `340` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate340() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 340.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `350` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate350() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 350.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `350` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate350() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 350.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `350` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate350() {
        return hsvRotate(350.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `350` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate350() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 350.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `355` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate355() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 355.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `355` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate355() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 355.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `355` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate355() {
        return hsvRotate(355.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `355` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate355() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 355.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `1` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate1() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 1.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `1` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate1() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 1.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `1` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate1() {
        return hsvRotate(1.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `1` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate1() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 1.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `2` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate2() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 2.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `2` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate2() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 2.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `2` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate2() {
        return hsvRotate(2.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `2` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate2() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 2.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `3` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate3() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 3.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `3` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate3() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 3.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `3` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate3() {
        return hsvRotate(3.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `3` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate3() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 3.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `4` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate4() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 4.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `4` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate4() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 4.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `4` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate4() {
        return hsvRotate(4.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `4` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate4() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 4.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `6` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate6() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 6.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `6` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate6() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 6.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `6` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate6() {
        return hsvRotate(6.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `6` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate6() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 6.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `7` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate7() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 7.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `7` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate7() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 7.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `7` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate7() {
        return hsvRotate(7.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `7` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate7() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 7.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `8` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate8() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 8.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `8` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate8() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 8.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `8` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate8() {
        return hsvRotate(8.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `8` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate8() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 8.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `9` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate9() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 9.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `9` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate9() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 9.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `9` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate9() {
        return hsvRotate(9.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `9` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate9() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 9.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `11` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate11() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 11.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `11` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate11() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 11.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `11` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate11() {
        return hsvRotate(11.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `11` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate11() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 11.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `12` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate12() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 12.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `12` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate12() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 12.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `12` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate12() {
        return hsvRotate(12.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `12` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate12() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 12.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `13` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate13() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 13.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `13` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate13() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 13.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `13` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate13() {
        return hsvRotate(13.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `13` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate13() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 13.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `14` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate14() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 14.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `14` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate14() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 14.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `14` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate14() {
        return hsvRotate(14.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `14` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate14() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 14.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `16` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate16() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 16.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `16` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate16() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 16.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `16` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate16() {
        return hsvRotate(16.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `16` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate16() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 16.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `17` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate17() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 17.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `17` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate17() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 17.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `17` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate17() {
        return hsvRotate(17.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `17` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate17() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 17.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `18` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate18() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 18.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `18` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate18() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 18.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `18` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate18() {
        return hsvRotate(18.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `18` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate18() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 18.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `19` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate19() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 19.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `19` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate19() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 19.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `19` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate19() {
        return hsvRotate(19.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `19` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate19() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 19.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `21` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate21() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 21.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `21` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate21() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 21.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `21` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate21() {
        return hsvRotate(21.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `21` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate21() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 21.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `22` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate22() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 22.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `22` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate22() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 22.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `22` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate22() {
        return hsvRotate(22.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `22` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate22() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 22.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `23` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate23() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 23.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `23` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate23() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 23.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `23` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate23() {
        return hsvRotate(23.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `23` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate23() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 23.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `24` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate24() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 24.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `24` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate24() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 24.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `24` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate24() {
        return hsvRotate(24.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `24` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate24() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 24.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `26` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate26() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 26.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `26` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate26() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 26.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `26` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate26() {
        return hsvRotate(26.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `26` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate26() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 26.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `27` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate27() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 27.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `27` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate27() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 27.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `27` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate27() {
        return hsvRotate(27.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `27` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate27() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 27.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `28` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate28() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 28.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `28` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate28() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 28.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `28` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate28() {
        return hsvRotate(28.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `28` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate28() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 28.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `29` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate29() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 29.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `29` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate29() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 29.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `29` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate29() {
        return hsvRotate(29.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `29` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate29() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 29.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `31` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate31() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 31.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `31` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate31() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 31.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `31` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate31() {
        return hsvRotate(31.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `31` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate31() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 31.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `32` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate32() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 32.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `32` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate32() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 32.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `32` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate32() {
        return hsvRotate(32.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `32` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate32() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 32.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `33` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate33() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 33.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `33` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate33() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 33.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `33` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate33() {
        return hsvRotate(33.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `33` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate33() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 33.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `34` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate34() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 34.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `34` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate34() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 34.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `34` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate34() {
        return hsvRotate(34.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `34` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate34() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 34.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `36` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate36() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 36.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `36` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate36() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 36.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `36` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate36() {
        return hsvRotate(36.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `36` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate36() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 36.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `37` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate37() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 37.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `37` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate37() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 37.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `37` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate37() {
        return hsvRotate(37.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `37` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate37() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 37.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `38` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate38() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 38.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `38` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate38() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 38.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `38` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate38() {
        return hsvRotate(38.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `38` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate38() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 38.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `39` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate39() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 39.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `39` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate39() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 39.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `39` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate39() {
        return hsvRotate(39.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `39` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate39() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 39.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `41` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate41() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 41.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `41` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate41() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 41.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `41` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate41() {
        return hsvRotate(41.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `41` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate41() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 41.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `42` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate42() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 42.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `42` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate42() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 42.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `42` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate42() {
        return hsvRotate(42.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `42` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate42() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 42.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `43` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate43() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 43.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `43` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate43() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 43.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `43` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate43() {
        return hsvRotate(43.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `43` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate43() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 43.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `44` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate44() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 44.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `44` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate44() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 44.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `44` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate44() {
        return hsvRotate(44.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `44` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate44() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 44.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `46` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate46() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 46.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `46` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate46() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 46.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `46` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate46() {
        return hsvRotate(46.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `46` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate46() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 46.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `47` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate47() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 47.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `47` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate47() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 47.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `47` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate47() {
        return hsvRotate(47.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `47` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate47() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 47.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `48` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate48() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 48.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `48` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate48() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 48.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `48` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate48() {
        return hsvRotate(48.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `48` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate48() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 48.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `49` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate49() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 49.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `49` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate49() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 49.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `49` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate49() {
        return hsvRotate(49.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `49` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate49() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 49.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `51` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate51() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 51.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `51` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate51() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 51.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `51` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate51() {
        return hsvRotate(51.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `51` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate51() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 51.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `52` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate52() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 52.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `52` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate52() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 52.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `52` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate52() {
        return hsvRotate(52.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `52` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate52() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 52.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `53` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate53() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 53.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `53` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate53() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 53.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `53` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate53() {
        return hsvRotate(53.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `53` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate53() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 53.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `54` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate54() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 54.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `54` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate54() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 54.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `54` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate54() {
        return hsvRotate(54.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `54` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate54() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 54.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `56` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate56() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 56.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `56` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate56() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 56.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `56` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate56() {
        return hsvRotate(56.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `56` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate56() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 56.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `57` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate57() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 57.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `57` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate57() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 57.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `57` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate57() {
        return hsvRotate(57.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `57` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate57() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 57.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `58` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate58() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 58.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `58` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate58() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 58.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `58` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate58() {
        return hsvRotate(58.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `58` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate58() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 58.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `59` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate59() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 59.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `59` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate59() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 59.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `59` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate59() {
        return hsvRotate(59.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `59` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate59() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 59.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `61` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate61() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 61.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `61` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate61() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 61.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `61` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate61() {
        return hsvRotate(61.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `61` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate61() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 61.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `62` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate62() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 62.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `62` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate62() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 62.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `62` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate62() {
        return hsvRotate(62.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `62` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate62() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 62.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `63` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate63() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 63.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `63` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate63() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 63.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `63` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate63() {
        return hsvRotate(63.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `63` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate63() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 63.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `64` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate64() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 64.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `64` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate64() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 64.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `64` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate64() {
        return hsvRotate(64.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `64` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate64() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 64.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `66` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate66() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 66.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `66` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate66() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 66.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `66` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate66() {
        return hsvRotate(66.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `66` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate66() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 66.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `67` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate67() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 67.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `67` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate67() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 67.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `67` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate67() {
        return hsvRotate(67.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `67` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate67() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 67.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `68` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate68() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 68.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `68` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate68() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 68.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `68` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate68() {
        return hsvRotate(68.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `68` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate68() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 68.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `69` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate69() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 69.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `69` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate69() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 69.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `69` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate69() {
        return hsvRotate(69.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `69` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate69() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 69.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `71` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate71() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 71.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `71` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate71() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 71.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `71` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate71() {
        return hsvRotate(71.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `71` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate71() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 71.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `72` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate72() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 72.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `72` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate72() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 72.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `72` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate72() {
        return hsvRotate(72.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `72` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate72() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 72.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `73` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate73() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 73.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `73` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate73() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 73.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `73` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate73() {
        return hsvRotate(73.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `73` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate73() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 73.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `74` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate74() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 74.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `74` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate74() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 74.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `74` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate74() {
        return hsvRotate(74.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `74` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate74() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 74.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `76` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate76() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 76.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `76` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate76() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 76.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `76` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate76() {
        return hsvRotate(76.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `76` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate76() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 76.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `77` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate77() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 77.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `77` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate77() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 77.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `77` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate77() {
        return hsvRotate(77.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `77` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate77() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 77.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `78` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate78() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 78.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `78` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate78() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 78.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `78` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate78() {
        return hsvRotate(78.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `78` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate78() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 78.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `79` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate79() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 79.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `79` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate79() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 79.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `79` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate79() {
        return hsvRotate(79.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `79` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate79() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 79.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `81` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate81() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 81.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `81` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate81() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 81.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `81` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate81() {
        return hsvRotate(81.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `81` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate81() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 81.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `82` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate82() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 82.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `82` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate82() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 82.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `82` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate82() {
        return hsvRotate(82.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `82` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate82() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 82.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `83` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate83() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 83.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `83` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate83() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 83.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `83` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate83() {
        return hsvRotate(83.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `83` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate83() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 83.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `84` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate84() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 84.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `84` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate84() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 84.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `84` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate84() {
        return hsvRotate(84.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `84` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate84() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 84.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `86` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate86() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 86.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `86` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate86() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 86.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `86` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate86() {
        return hsvRotate(86.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `86` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate86() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 86.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `87` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate87() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 87.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `87` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate87() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 87.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `87` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate87() {
        return hsvRotate(87.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `87` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate87() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 87.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `88` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate88() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 88.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `88` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate88() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 88.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `88` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate88() {
        return hsvRotate(88.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `88` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate88() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 88.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `89` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate89() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 89.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `89` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate89() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 89.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `89` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate89() {
        return hsvRotate(89.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `89` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate89() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 89.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `91` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate91() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 91.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `91` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate91() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 91.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `91` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate91() {
        return hsvRotate(91.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `91` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate91() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 91.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `92` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate92() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 92.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `92` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate92() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 92.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `92` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate92() {
        return hsvRotate(92.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `92` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate92() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 92.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `93` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate93() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 93.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `93` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate93() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 93.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `93` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate93() {
        return hsvRotate(93.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `93` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate93() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 93.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `94` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate94() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 94.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `94` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate94() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 94.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `94` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate94() {
        return hsvRotate(94.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `94` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate94() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 94.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `96` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate96() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 96.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `96` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate96() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 96.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `96` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate96() {
        return hsvRotate(96.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `96` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate96() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 96.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `97` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate97() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 97.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `97` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate97() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 97.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `97` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate97() {
        return hsvRotate(97.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `97` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate97() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 97.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `98` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate98() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 98.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `98` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate98() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 98.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `98` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate98() {
        return hsvRotate(98.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `98` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate98() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 98.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `99` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate99() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 99.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `99` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate99() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 99.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `99` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate99() {
        return hsvRotate(99.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `99` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate99() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 99.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `101` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate101() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 101.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `101` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate101() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 101.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `101` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate101() {
        return hsvRotate(101.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `101` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate101() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 101.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `102` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate102() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 102.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `102` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate102() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 102.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `102` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate102() {
        return hsvRotate(102.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `102` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate102() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 102.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `103` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate103() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 103.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `103` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate103() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 103.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `103` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate103() {
        return hsvRotate(103.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `103` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate103() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 103.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `104` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate104() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 104.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `104` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate104() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 104.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `104` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate104() {
        return hsvRotate(104.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `104` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate104() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 104.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `106` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate106() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 106.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `106` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate106() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 106.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `106` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate106() {
        return hsvRotate(106.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `106` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate106() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 106.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `107` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate107() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 107.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `107` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate107() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 107.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `107` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate107() {
        return hsvRotate(107.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `107` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate107() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 107.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `108` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate108() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 108.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `108` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate108() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 108.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `108` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate108() {
        return hsvRotate(108.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `108` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate108() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 108.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `109` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate109() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 109.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `109` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate109() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 109.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `109` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate109() {
        return hsvRotate(109.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `109` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate109() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 109.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `111` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate111() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 111.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `111` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate111() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 111.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `111` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate111() {
        return hsvRotate(111.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `111` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate111() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 111.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `112` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate112() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 112.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `112` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate112() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 112.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `112` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate112() {
        return hsvRotate(112.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `112` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate112() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 112.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `113` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate113() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 113.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `113` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate113() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 113.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `113` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate113() {
        return hsvRotate(113.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `113` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate113() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 113.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `114` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate114() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 114.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `114` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate114() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 114.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `114` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate114() {
        return hsvRotate(114.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `114` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate114() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 114.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `116` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate116() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 116.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `116` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate116() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 116.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `116` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate116() {
        return hsvRotate(116.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `116` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate116() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 116.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `117` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate117() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 117.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `117` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate117() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 117.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `117` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate117() {
        return hsvRotate(117.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `117` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate117() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 117.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `118` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate118() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 118.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `118` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate118() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 118.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `118` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate118() {
        return hsvRotate(118.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `118` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate118() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 118.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `119` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate119() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 119.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `119` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate119() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 119.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `119` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate119() {
        return hsvRotate(119.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `119` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate119() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 119.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `121` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate121() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 121.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `121` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate121() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 121.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `121` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate121() {
        return hsvRotate(121.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `121` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate121() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 121.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `122` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate122() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 122.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `122` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate122() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 122.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `122` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate122() {
        return hsvRotate(122.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `122` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate122() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 122.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `123` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate123() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 123.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `123` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate123() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 123.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `123` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate123() {
        return hsvRotate(123.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `123` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate123() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 123.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `124` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate124() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 124.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `124` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate124() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 124.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `124` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate124() {
        return hsvRotate(124.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `124` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate124() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 124.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `126` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate126() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 126.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `126` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate126() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 126.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `126` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate126() {
        return hsvRotate(126.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `126` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate126() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 126.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `127` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate127() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 127.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `127` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate127() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 127.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `127` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate127() {
        return hsvRotate(127.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `127` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate127() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 127.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `128` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate128() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 128.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `128` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate128() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 128.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `128` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate128() {
        return hsvRotate(128.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `128` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate128() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 128.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `129` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate129() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 129.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `129` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate129() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 129.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `129` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate129() {
        return hsvRotate(129.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `129` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate129() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 129.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `131` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate131() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 131.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `131` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate131() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 131.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `131` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate131() {
        return hsvRotate(131.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `131` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate131() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 131.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `132` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate132() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 132.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `132` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate132() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 132.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `132` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate132() {
        return hsvRotate(132.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `132` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate132() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 132.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `133` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate133() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 133.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `133` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate133() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 133.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `133` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate133() {
        return hsvRotate(133.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `133` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate133() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 133.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `134` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate134() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 134.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `134` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate134() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 134.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `134` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate134() {
        return hsvRotate(134.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `134` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate134() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 134.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `136` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate136() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 136.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `136` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate136() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 136.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `136` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate136() {
        return hsvRotate(136.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `136` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate136() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 136.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `137` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate137() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 137.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `137` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate137() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 137.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `137` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate137() {
        return hsvRotate(137.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `137` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate137() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 137.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `138` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate138() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 138.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `138` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate138() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 138.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `138` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate138() {
        return hsvRotate(138.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `138` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate138() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 138.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `139` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate139() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 139.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `139` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate139() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 139.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `139` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate139() {
        return hsvRotate(139.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `139` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate139() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 139.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `141` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate141() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 141.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `141` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate141() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 141.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `141` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate141() {
        return hsvRotate(141.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `141` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate141() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 141.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `142` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate142() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 142.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `142` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate142() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 142.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `142` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate142() {
        return hsvRotate(142.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `142` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate142() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 142.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `143` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate143() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 143.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `143` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate143() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 143.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `143` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate143() {
        return hsvRotate(143.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `143` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate143() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 143.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `144` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate144() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 144.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `144` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate144() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 144.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `144` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate144() {
        return hsvRotate(144.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `144` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate144() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 144.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `146` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate146() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 146.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `146` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate146() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 146.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `146` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate146() {
        return hsvRotate(146.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `146` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate146() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 146.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `147` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate147() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 147.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `147` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate147() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 147.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `147` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate147() {
        return hsvRotate(147.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `147` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate147() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 147.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `148` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate148() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 148.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `148` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate148() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 148.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `148` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate148() {
        return hsvRotate(148.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `148` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate148() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 148.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `149` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate149() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 149.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `149` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate149() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 149.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `149` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate149() {
        return hsvRotate(149.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `149` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate149() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 149.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `151` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate151() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 151.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `151` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate151() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 151.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `151` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate151() {
        return hsvRotate(151.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `151` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate151() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 151.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `152` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate152() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 152.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `152` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate152() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 152.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `152` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate152() {
        return hsvRotate(152.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `152` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate152() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 152.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `153` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate153() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 153.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `153` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate153() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 153.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `153` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate153() {
        return hsvRotate(153.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `153` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate153() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 153.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `154` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate154() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 154.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `154` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate154() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 154.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `154` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate154() {
        return hsvRotate(154.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `154` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate154() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 154.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `156` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate156() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 156.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `156` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate156() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 156.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `156` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate156() {
        return hsvRotate(156.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `156` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate156() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 156.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `157` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate157() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 157.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `157` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate157() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 157.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `157` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate157() {
        return hsvRotate(157.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `157` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate157() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 157.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `158` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate158() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 158.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `158` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate158() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 158.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `158` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate158() {
        return hsvRotate(158.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `158` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate158() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 158.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `159` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate159() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 159.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `159` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate159() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 159.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `159` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate159() {
        return hsvRotate(159.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `159` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate159() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 159.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `161` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate161() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 161.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `161` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate161() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 161.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `161` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate161() {
        return hsvRotate(161.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `161` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate161() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 161.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `162` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate162() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 162.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `162` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate162() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 162.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `162` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate162() {
        return hsvRotate(162.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `162` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate162() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 162.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `163` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate163() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 163.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `163` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate163() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 163.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `163` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate163() {
        return hsvRotate(163.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `163` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate163() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 163.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `164` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate164() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 164.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `164` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate164() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 164.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `164` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate164() {
        return hsvRotate(164.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `164` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate164() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 164.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `166` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate166() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 166.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `166` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate166() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 166.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `166` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate166() {
        return hsvRotate(166.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `166` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate166() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 166.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `167` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate167() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 167.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `167` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate167() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 167.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `167` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate167() {
        return hsvRotate(167.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `167` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate167() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 167.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `168` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate168() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 168.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `168` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate168() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 168.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `168` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate168() {
        return hsvRotate(168.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `168` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate168() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 168.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `169` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate169() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 169.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `169` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate169() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 169.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `169` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate169() {
        return hsvRotate(169.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `169` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate169() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 169.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `171` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate171() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 171.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `171` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate171() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 171.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `171` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate171() {
        return hsvRotate(171.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `171` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate171() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 171.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `172` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate172() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 172.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `172` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate172() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 172.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `172` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate172() {
        return hsvRotate(172.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `172` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate172() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 172.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `173` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate173() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 173.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `173` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate173() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 173.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `173` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate173() {
        return hsvRotate(173.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `173` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate173() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 173.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `174` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate174() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 174.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `174` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate174() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 174.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `174` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate174() {
        return hsvRotate(174.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `174` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate174() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 174.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `176` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate176() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 176.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `176` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate176() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 176.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `176` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate176() {
        return hsvRotate(176.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `176` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate176() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 176.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `177` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate177() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 177.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `177` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate177() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 177.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `177` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate177() {
        return hsvRotate(177.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `177` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate177() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 177.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `178` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate178() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 178.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `178` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate178() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 178.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `178` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate178() {
        return hsvRotate(178.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `178` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate178() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 178.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `179` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate179() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 179.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `179` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate179() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 179.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `179` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate179() {
        return hsvRotate(179.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `179` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate179() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 179.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `180` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate180() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 180.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `180` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate180() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 180.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `180` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate180() {
        return hsvRotate(180.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `180` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate180() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 180.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `181` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate181() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 181.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `181` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate181() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 181.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `181` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate181() {
        return hsvRotate(181.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `181` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate181() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 181.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `182` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate182() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 182.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `182` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate182() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 182.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `182` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate182() {
        return hsvRotate(182.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `182` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate182() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 182.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `183` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate183() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 183.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `183` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate183() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 183.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `183` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate183() {
        return hsvRotate(183.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `183` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate183() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 183.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `184` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate184() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 184.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `184` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate184() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 184.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `184` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate184() {
        return hsvRotate(184.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `184` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate184() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 184.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `186` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate186() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 186.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `186` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate186() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 186.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `186` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate186() {
        return hsvRotate(186.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `186` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate186() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 186.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `187` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate187() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 187.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `187` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate187() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 187.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `187` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate187() {
        return hsvRotate(187.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `187` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate187() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 187.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `188` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate188() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 188.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `188` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate188() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 188.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `188` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate188() {
        return hsvRotate(188.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `188` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate188() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 188.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `189` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate189() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 189.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `189` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate189() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 189.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `189` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate189() {
        return hsvRotate(189.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `189` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate189() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 189.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `191` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate191() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 191.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `191` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate191() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 191.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `191` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate191() {
        return hsvRotate(191.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `191` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate191() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 191.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `192` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate192() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 192.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `192` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate192() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 192.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `192` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate192() {
        return hsvRotate(192.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `192` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate192() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 192.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `193` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate193() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 193.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `193` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate193() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 193.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `193` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate193() {
        return hsvRotate(193.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `193` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate193() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 193.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `194` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate194() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 194.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `194` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate194() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 194.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `194` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate194() {
        return hsvRotate(194.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `194` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate194() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 194.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `196` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate196() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 196.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `196` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate196() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 196.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `196` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate196() {
        return hsvRotate(196.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `196` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate196() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 196.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `197` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate197() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 197.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `197` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate197() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 197.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `197` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate197() {
        return hsvRotate(197.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `197` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate197() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 197.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `198` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate198() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 198.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `198` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate198() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 198.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `198` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate198() {
        return hsvRotate(198.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `198` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate198() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 198.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `199` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate199() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 199.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `199` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate199() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 199.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `199` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate199() {
        return hsvRotate(199.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `199` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate199() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 199.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `201` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate201() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 201.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `201` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate201() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 201.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `201` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate201() {
        return hsvRotate(201.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `201` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate201() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 201.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `202` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate202() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 202.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `202` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate202() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 202.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `202` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate202() {
        return hsvRotate(202.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `202` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate202() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 202.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `203` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate203() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 203.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `203` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate203() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 203.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `203` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate203() {
        return hsvRotate(203.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `203` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate203() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 203.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `204` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate204() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 204.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `204` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate204() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 204.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `204` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate204() {
        return hsvRotate(204.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `204` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate204() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 204.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `206` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate206() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 206.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `206` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate206() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 206.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `206` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate206() {
        return hsvRotate(206.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `206` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate206() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 206.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `207` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate207() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 207.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `207` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate207() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 207.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `207` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate207() {
        return hsvRotate(207.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `207` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate207() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 207.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `208` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate208() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 208.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `208` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate208() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 208.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `208` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate208() {
        return hsvRotate(208.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `208` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate208() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 208.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `209` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate209() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 209.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `209` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate209() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 209.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `209` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate209() {
        return hsvRotate(209.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `209` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate209() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 209.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `211` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate211() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 211.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `211` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate211() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 211.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `211` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate211() {
        return hsvRotate(211.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `211` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate211() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 211.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `212` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate212() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 212.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `212` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate212() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 212.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `212` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate212() {
        return hsvRotate(212.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `212` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate212() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 212.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `213` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate213() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 213.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `213` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate213() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 213.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `213` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate213() {
        return hsvRotate(213.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `213` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate213() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 213.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `214` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate214() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 214.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `214` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate214() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 214.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `214` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate214() {
        return hsvRotate(214.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `214` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate214() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 214.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `216` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate216() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 216.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `216` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate216() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 216.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `216` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate216() {
        return hsvRotate(216.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `216` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate216() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 216.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `217` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate217() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 217.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `217` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate217() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 217.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `217` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate217() {
        return hsvRotate(217.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `217` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate217() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 217.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `218` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate218() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 218.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `218` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate218() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 218.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `218` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate218() {
        return hsvRotate(218.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `218` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate218() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 218.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `219` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate219() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 219.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `219` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate219() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 219.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `219` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate219() {
        return hsvRotate(219.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `219` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate219() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 219.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `221` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate221() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 221.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `221` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate221() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 221.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `221` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate221() {
        return hsvRotate(221.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `221` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate221() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 221.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `222` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate222() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 222.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `222` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate222() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 222.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `222` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate222() {
        return hsvRotate(222.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `222` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate222() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 222.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `223` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate223() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 223.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `223` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate223() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 223.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `223` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate223() {
        return hsvRotate(223.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `223` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate223() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 223.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `224` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate224() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 224.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `224` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate224() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 224.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `224` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate224() {
        return hsvRotate(224.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `224` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate224() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 224.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `226` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate226() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 226.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `226` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate226() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 226.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `226` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate226() {
        return hsvRotate(226.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `226` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate226() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 226.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `227` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate227() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 227.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `227` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate227() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 227.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `227` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate227() {
        return hsvRotate(227.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `227` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate227() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 227.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `228` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate228() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 228.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `228` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate228() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 228.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `228` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate228() {
        return hsvRotate(228.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `228` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate228() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 228.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `229` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate229() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 229.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `229` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate229() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 229.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `229` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate229() {
        return hsvRotate(229.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `229` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate229() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 229.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with OKLCH hue rotated by `231` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color oklchRotate231() {
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], oklch[2] + 231.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with CIE LCh hue rotated by `231` degrees.
    ///
    /// Lightness and chroma are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color cieLchRotate231() {
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], lch[2] + 231.0f, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with HSV hue rotated by `231` degrees.
    ///
    /// Saturation and value are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hsvRotate231() {
        return hsvRotate(231.0f);
    }

    /// Returns an sRGB copy with HSL hue rotated by `231` degrees.
    ///
    /// Saturation and lightness are unchanged.
    ///
    /// @return the hue-rotated sRGB color
    public Color hslRotate231() {
        float[] hsl = toHsl();
        return fromHsl(hsl[0] + 231.0f, hsl[1], hsl[2], toSrgb().alpha);
    }

    /// Returns an sRGB copy with the given absolute OKLCH chroma.
    ///
    /// @param chroma the OKLCH chroma, nonnegative
    /// @return the recolored sRGB color
    public Color withChroma(float chroma) {
        if (!Float.isFinite(chroma) || chroma < 0.0f) {
            throw new IllegalArgumentException("chroma must be finite and nonnegative");
        }
        float[] oklch = toOklch();
        return fromOklch(oklch[0], chroma, oklch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute OKLCH hue.
    ///
    /// @param hueDegrees the hue in degrees
    /// @return the recolored sRGB color
    public Color withOklchHue(float hueDegrees) {
        if (!Float.isFinite(hueDegrees)) {
            throw new IllegalArgumentException("hue must be finite");
        }
        float[] oklch = toOklch();
        return fromOklch(oklch[0], oklch[1], hueDegrees, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute OKLCH lightness.
    ///
    /// @param lightness the OKLCH L component, nonnegative
    /// @return the recolored sRGB color
    public Color withOklchLightness(float lightness) {
        if (!Float.isFinite(lightness) || lightness < 0.0f) {
            throw new IllegalArgumentException("lightness must be finite and nonnegative");
        }
        float[] oklch = toOklch();
        return fromOklch(lightness, oklch[1], oklch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute OKLab lightness.
    ///
    /// @param lightness the OKLab L component, nonnegative
    /// @return the recolored sRGB color
    public Color withOklabL(float lightness) {
        if (!Float.isFinite(lightness) || lightness < 0.0f) {
            throw new IllegalArgumentException("lightness must be finite and nonnegative");
        }
        float[] oklab = toOklab();
        return fromOklab(lightness, oklab[1], oklab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute OKLab `a` component.
    ///
    /// @param a the OKLab a component
    /// @return the recolored sRGB color
    public Color withOklabA(float a) {
        if (!Float.isFinite(a)) {
            throw new IllegalArgumentException("a must be finite");
        }
        float[] oklab = toOklab();
        return fromOklab(oklab[0], a, oklab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute OKLab `b` component.
    ///
    /// @param b the OKLab b component
    /// @return the recolored sRGB color
    public Color withOklabB(float b) {
        if (!Float.isFinite(b)) {
            throw new IllegalArgumentException("b must be finite");
        }
        float[] oklab = toOklab();
        return fromOklab(oklab[0], oklab[1], b, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute CIE LCh hue.
    ///
    /// @param hueDegrees the hue in degrees
    /// @return the recolored sRGB color
    public Color withCieLchHue(float hueDegrees) {
        if (!Float.isFinite(hueDegrees)) {
            throw new IllegalArgumentException("hue must be finite");
        }
        float[] lch = toCieLch();
        return fromCieLch(lch[0], lch[1], hueDegrees, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute CIE LCh chroma.
    ///
    /// @param chroma the CIE LCh chroma, nonnegative
    /// @return the recolored sRGB color
    public Color withCieLchChroma(float chroma) {
        if (!Float.isFinite(chroma) || chroma < 0.0f) {
            throw new IllegalArgumentException("chroma must be finite and nonnegative");
        }
        float[] lch = toCieLch();
        return fromCieLch(lch[0], chroma, lch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute CIE LCh lightness.
    ///
    /// @param lightness the CIE L* component, nonnegative
    /// @return the recolored sRGB color
    public Color withCieLchLightness(float lightness) {
        if (!Float.isFinite(lightness) || lightness < 0.0f) {
            throw new IllegalArgumentException("lightness must be finite and nonnegative");
        }
        float[] lch = toCieLch();
        return fromCieLch(lightness, lch[1], lch[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute CIE Lab lightness.
    ///
    /// @param lightness the CIE L* component, nonnegative
    /// @return the recolored sRGB color
    public Color withCieLabL(float lightness) {
        if (!Float.isFinite(lightness) || lightness < 0.0f) {
            throw new IllegalArgumentException("lightness must be finite and nonnegative");
        }
        float[] lab = toCieLab();
        return fromCieLab(lightness, lab[1], lab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute CIE Lab `a` component.
    ///
    /// @param a the CIE Lab a component
    /// @return the recolored sRGB color
    public Color withCieLabA(float a) {
        if (!Float.isFinite(a)) {
            throw new IllegalArgumentException("a must be finite");
        }
        float[] lab = toCieLab();
        return fromCieLab(lab[0], a, lab[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute CIE Lab `b` component.
    ///
    /// @param b the CIE Lab b component
    /// @return the recolored sRGB color
    public Color withCieLabB(float b) {
        if (!Float.isFinite(b)) {
            throw new IllegalArgumentException("b must be finite");
        }
        float[] lab = toCieLab();
        return fromCieLab(lab[0], lab[1], b, toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute CIE Luv lightness.
    ///
    /// @param lightness the CIE L* component, nonnegative
    /// @return the recolored sRGB color
    public Color withCieLuvL(float lightness) {
        if (!Float.isFinite(lightness) || lightness < 0.0f) {
            throw new IllegalArgumentException("lightness must be finite and nonnegative");
        }
        float[] luv = toCieLuv();
        return fromCieLuv(lightness, luv[1], luv[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute CIE Luv `u` component.
    ///
    /// @param u the CIE Luv u component
    /// @return the recolored sRGB color
    public Color withCieLuvU(float u) {
        if (!Float.isFinite(u)) {
            throw new IllegalArgumentException("u must be finite");
        }
        float[] luv = toCieLuv();
        return fromCieLuv(luv[0], u, luv[2], toSrgb().alpha).toSrgb();
    }

    /// Returns an sRGB copy with the given absolute CIE Luv `v` component.
    ///
    /// @param v the CIE Luv v component
    /// @return the recolored sRGB color
    public Color withCieLuvV(float v) {
        if (!Float.isFinite(v)) {
            throw new IllegalArgumentException("v must be finite");
        }
        float[] luv = toCieLuv();
        return fromCieLuv(luv[0], luv[1], v, toSrgb().alpha).toSrgb();
    }

    /// Encodes this color as `#RRGGBB` when opaque, otherwise `#AARRGGBB`.
    ///
    /// @return the hex string
    public String toHex() {
        int argb = toArgb();
        if ((argb >>> 24) == 0xFF) {
            return String.format("#%06X", argb & 0x00FFFFFF);
        }
        return String.format("#%08X", argb);
    }

    /// Creates an sRGB color from `#RRGGBB` or `#AARRGGBB`.
    ///
    /// The leading `#` may be omitted. Eight-digit forms are packed ARGB.
    ///
    /// @param hex the hex string
    /// @return the sRGB color
    public static Color fromHex(String hex) {
        Objects.requireNonNull(hex, "hex");
        String body = hex.startsWith("#") ? hex.substring(1) : hex;
        if (body.length() == 6) {
            return fromArgb(0xFF000000 | Integer.parseUnsignedInt(body, 16));
        }
        if (body.length() == 8) {
            return fromArgb((int) Long.parseUnsignedLong(body, 16));
        }
        throw new IllegalArgumentException("hex must be #RRGGBB or #AARRGGBB");
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
