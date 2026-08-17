package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// Effective presentation configuration negotiated for one surface.
///
/// @param format the destination pixel format
/// @param encoding the destination color encoding
/// @param alpha how alpha is stored
/// @param luminance the destination luminance range
/// @param mappingOwner who performs the last tone/gamut map
/// @param metadata published content-light values
/// @param capabilityGeneration the surface capability generation
@NotNullByDefault
public record PresentationColorConfiguration(
        PixelFormat format,
        ColorEncoding encoding,
        AlphaInterpretation alpha,
        LuminanceRange luminance,
        MappingOwner mappingOwner,
        ContentLightMetadata metadata,
        long capabilityGeneration
) {
    /// First-stable SDR sRGB configuration.
    public static final PresentationColorConfiguration SDR_SRGB = new PresentationColorConfiguration(
            PixelFormat.RGBA8,
            ColorEncoding.SRGB,
            AlphaInterpretation.UNASSOCIATED,
            LuminanceRange.SDR,
            MappingOwner.FRAMEWORK,
            ContentLightMetadata.NONE,
            1L
    );

    /// Validates a finite generation.
    public PresentationColorConfiguration {
        if (capabilityGeneration <= 0L) {
            throw new IllegalArgumentException("capabilityGeneration must be positive");
        }
    }
}
