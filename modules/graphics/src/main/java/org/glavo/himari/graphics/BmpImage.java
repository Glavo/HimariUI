package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/// Encodes and decodes uncompressed Windows BITMAPINFOHEADER 24-bit BMP files.
///
/// Pixels are stored as unassociated 8-bit RGBA in row-major top-down order. The on-disk
/// layout is bottom-up BGR with 4-byte row padding. This is the first-stable debug BMP
/// path from section 17.1.
@NotNullByDefault
public final class BmpImage {
    /// `BM` magic.
    private static final int MAGIC = 0x4D42;

    /// BITMAPFILEHEADER plus BITMAPINFOHEADER.
    private static final int HEADER_SIZE = 54;

    /// Maximum accepted width or height.
    private static final int MAX_EDGE = 16_384;

    /// Prevents instantiation.
    private BmpImage() {
    }

    /// Encodes row-major unassociated RGBA8 pixels as a 24-bit BMP.
    ///
    /// Alpha is dropped. Each row is padded to a multiple of four bytes.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the BMP bytes
    public static byte @Unmodifiable [] encode(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        int stride = rowStride(width);
        int pixelBytes = stride * height;
        ByteBuffer output = ByteBuffer.allocate(HEADER_SIZE + pixelBytes).order(ByteOrder.LITTLE_ENDIAN);
        output.putShort((short) MAGIC);
        output.putInt(HEADER_SIZE + pixelBytes);
        output.putInt(0);
        output.putInt(HEADER_SIZE);
        output.putInt(40);
        output.putInt(width);
        output.putInt(height);
        output.putShort((short) 1);
        output.putShort((short) 24);
        output.putInt(0);
        output.putInt(pixelBytes);
        output.putInt(0);
        output.putInt(0);
        output.putInt(0);
        output.putInt(0);
        for (int y = height - 1; y >= 0; y--) {
            int rowStart = output.position();
            for (int x = 0; x < width; x++) {
                int offset = (y * width + x) * 4;
                output.put(rgba[offset + 2]);
                output.put(rgba[offset + 1]);
                output.put(rgba[offset]);
            }
            output.position(rowStart + stride);
        }
        return output.array();
    }

    /// Decodes an uncompressed 24-bit BMP into row-major unassociated RGBA8 pixels.
    ///
    /// @param bytes the BMP stream
    /// @return the decoded image
    public static Decoded decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < HEADER_SIZE) {
            throw new IllegalArgumentException("BMP stream is truncated");
        }
        ByteBuffer input = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if ((input.getShort() & 0xFFFF) != MAGIC) {
            throw new IllegalArgumentException("BMP magic must be BM");
        }
        input.getInt();
        input.getInt();
        int offBits = input.getInt();
        int header = input.getInt();
        if (header < 40) {
            throw new IllegalArgumentException("BMP must use BITMAPINFOHEADER or later");
        }
        int width = input.getInt();
        int height = input.getInt();
        int planes = input.getShort() & 0xFFFF;
        int bitCount = input.getShort() & 0xFFFF;
        int compression = input.getInt();
        if (planes != 1 || bitCount != 24 || compression != 0) {
            throw new IllegalArgumentException("BMP must be uncompressed 24-bit");
        }
        int pixelCount = checkedPixelCount(width, Math.abs(height));
        int stride = rowStride(width);
        int absHeight = Math.abs(height);
        if (offBits < HEADER_SIZE || (long) offBits + (long) stride * absHeight > bytes.length) {
            throw new IllegalArgumentException("BMP pixel data is truncated");
        }
        byte[] rgba = new byte[pixelCount * 4];
        boolean topDown = height < 0;
        for (int y = 0; y < absHeight; y++) {
            int sourceY = topDown ? y : absHeight - 1 - y;
            int row = offBits + sourceY * stride;
            for (int x = 0; x < width; x++) {
                int dest = (y * width + x) * 4;
                int source = row + x * 3;
                rgba[dest] = bytes[source + 2];
                rgba[dest + 1] = bytes[source + 1];
                rgba[dest + 2] = bytes[source];
                rgba[dest + 3] = (byte) 255;
            }
        }
        return new Decoded(width, absHeight, rgba);
    }

    /// Returns `width * height` after rejecting non-positive or oversized images.
    private static int checkedPixelCount(int width, int height) {
        if (width <= 0 || height <= 0 || width > MAX_EDGE || height > MAX_EDGE) {
            throw new IllegalArgumentException("BMP dimensions must be in (0, " + MAX_EDGE + "]");
        }
        return Math.multiplyExact(width, height);
    }

    /// Returns the 4-byte-aligned BGR row stride.
    private static int rowStride(int width) {
        return (width * 3 + 3) & ~3;
    }

    /// Stores one decoded BMP image.
    ///
    /// @param width the pixel width
    /// @param height the pixel height
    /// @param rgba unassociated RGBA8 pixels with opaque alpha
    public record Decoded(int width, int height, byte @Unmodifiable [] rgba) {
        /// Validates the decoded image.
        public Decoded {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Decoded size must be positive");
            }
            Objects.requireNonNull(rgba, "rgba");
            if (rgba.length != Math.multiplyExact(width, height) * 4) {
                throw new IllegalArgumentException("RGBA length must match width * height * 4");
            }
            rgba = Arrays.copyOf(rgba, rgba.length);
        }
    }
}
