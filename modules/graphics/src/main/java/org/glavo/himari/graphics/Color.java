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
