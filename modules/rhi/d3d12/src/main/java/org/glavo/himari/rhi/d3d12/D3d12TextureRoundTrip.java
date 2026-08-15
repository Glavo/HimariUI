package org.glavo.himari.rhi.d3d12;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Objects;

/// Records one D3D12 texture upload or render-target clear that was copied back to the CPU.
///
/// @param width the pixel width
/// @param height the pixel height
/// @param rgba unassociated 8-bit sRGB pixels in row-major RGBA order
/// @param copied whether `CopyTextureRegion` completed
@NotNullByDefault
public record D3d12TextureRoundTrip(
        int width,
        int height,
        byte[] rgba,
        boolean copied
) {
    /// Validates the observation.
    public D3d12TextureRoundTrip {
        Objects.requireNonNull(rgba, "rgba");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        int expected = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (rgba.length != expected) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        rgba = rgba.clone();
    }

    /// Returns a copy of the read-back pixels.
    ///
    /// @return the pixels
    public byte @Unmodifiable [] rgba() {
        return rgba.clone();
    }

    /// Returns the largest absolute 8-bit channel difference against `expected`.
    ///
    /// @param expected the reference RGBA buffer
    /// @return the maximum channel delta in `0..255`
    public int maxChannelDelta(byte[] expected) {
        Objects.requireNonNull(expected, "expected");
        if (expected.length != rgba.length) {
            throw new IllegalArgumentException("Expected RGBA length must match the read-back buffer");
        }
        int max = 0;
        for (int index = 0; index < rgba.length; index++) {
            int delta = Math.abs((expected[index] & 0xFF) - (rgba[index] & 0xFF));
            if (delta > max) {
                max = delta;
            }
        }
        return max;
    }

    /// Returns how many bytes match `expected` exactly.
    ///
    /// @param expected the reference RGBA buffer
    /// @return the matching byte count
    public int matchedBytes(byte[] expected) {
        Objects.requireNonNull(expected, "expected");
        if (expected.length != rgba.length) {
            throw new IllegalArgumentException("Expected RGBA length must match the read-back buffer");
        }
        int matched = 0;
        for (int index = 0; index < rgba.length; index++) {
            if (expected[index] == rgba[index]) {
                matched++;
            }
        }
        return matched;
    }
}
