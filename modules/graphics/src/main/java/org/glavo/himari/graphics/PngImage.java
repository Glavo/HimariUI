package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/// Encodes and decodes 8-bit PNG rasters as unassociated sRGB RGBA.
///
/// The encoder writes filter-0 IDAT rows. The decoder accepts 8-bit greyscale, RGB, and RGBA
/// with filters 0–4 and rejects interlaced streams.
@NotNullByDefault
public final class PngImage {
    /// PNG signature.
    private static final byte[] SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    /// Prevents instantiation.
    private PngImage() {
    }

    /// Returns whether `bytes` begin with the PNG signature.
    ///
    /// @param bytes the candidate stream
    /// @return whether the stream is PNG
    public static boolean isPng(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < SIGNATURE.length; index++) {
            if (bytes[index] != SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }

    /// Encodes row-major unassociated RGBA8 pixels as PNG.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the PNG bytes
    public static byte @Unmodifiable [] encode(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        byte[] raw = new byte[height * (1 + width * 4)];
        int dest = 0;
        int source = 0;
        for (int row = 0; row < height; row++) {
            raw[dest++] = 0;
            System.arraycopy(rgba, source, raw, dest, width * 4);
            dest += width * 4;
            source += width * 4;
        }
        byte[] deflated = deflate(raw);
        ByteBuffer output = ByteBuffer.allocate(8 + 12 + 13 + 12 + deflated.length + 12)
                .order(ByteOrder.BIG_ENDIAN);
        output.put(SIGNATURE);
        writeChunk(output, "IHDR", ihdr(width, height));
        writeChunk(output, "IDAT", deflated);
        writeChunk(output, "IEND", new byte[0]);
        return Arrays.copyOf(output.array(), output.position());
    }

    /// Decodes an 8-bit PNG stream into row-major unassociated RGBA8.
    ///
    /// @param bytes the PNG stream
    /// @return the decoded image
    public static Decoded decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (!isPng(bytes)) {
            throw new IllegalArgumentException("PNG signature is missing");
        }
        int offset = 8;
        int width = 0;
        int height = 0;
        int bitDepth = 0;
        int colorType = 0;
        byte[] idat = new byte[0];
        boolean sawIhdr = false;
        while (offset + 12 <= bytes.length) {
            int length = readBe(bytes, offset);
            if (length < 0 || offset + 12 + length > bytes.length) {
                throw new IllegalArgumentException("PNG chunk is truncated");
            }
            int type = readBe(bytes, offset + 4);
            int data = offset + 8;
            if (type == fourcc("IHDR")) {
                if (length < 13) {
                    throw new IllegalArgumentException("PNG IHDR is truncated");
                }
                width = readBe(bytes, data);
                height = readBe(bytes, data + 4);
                bitDepth = bytes[data + 8] & 0xFF;
                colorType = bytes[data + 9] & 0xFF;
                int interlace = bytes[data + 12] & 0xFF;
                if (bitDepth != 8 || interlace != 0 || (colorType != 0 && colorType != 2 && colorType != 6)) {
                    throw new IllegalArgumentException("PNG first-stable decode is 8-bit grey/RGB/RGBA");
                }
                checkedPixelCount(width, height);
                sawIhdr = true;
            } else if (type == fourcc("IDAT")) {
                byte[] next = new byte[idat.length + length];
                System.arraycopy(idat, 0, next, 0, idat.length);
                System.arraycopy(bytes, data, next, idat.length, length);
                idat = next;
            } else if (type == fourcc("IEND")) {
                break;
            }
            offset += 12 + length;
        }
        if (!sawIhdr) {
            throw new IllegalArgumentException("PNG IHDR is missing");
        }
        int channels = colorType == 0 ? 1 : colorType == 2 ? 3 : 4;
        byte[] raw = inflate(idat, height * (1 + width * channels));
        byte[] rgba = unfilter(raw, width, height, channels);
        return new Decoded(width, height, rgba);
    }

    /// Rebuilds RGBA rows from PNG filtered scanlines.
    private static byte[] unfilter(byte[] raw, int width, int height, int channels) {
        int stride = width * channels;
        byte[] rgba = new byte[width * height * 4];
        byte[] prev = new byte[stride];
        byte[] curr = new byte[stride];
        int source = 0;
        for (int y = 0; y < height; y++) {
            int filter = raw[source++] & 0xFF;
            System.arraycopy(raw, source, curr, 0, stride);
            source += stride;
            for (int x = 0; x < stride; x++) {
                int left = x >= channels ? curr[x - channels] & 0xFF : 0;
                int up = prev[x] & 0xFF;
                int upLeft = x >= channels ? prev[x - channels] & 0xFF : 0;
                int sample = curr[x] & 0xFF;
                curr[x] = (byte) switch (filter) {
                    case 0 -> sample;
                    case 1 -> sample + left;
                    case 2 -> sample + up;
                    case 3 -> sample + ((left + up) / 2);
                    case 4 -> sample + paeth(left, up, upLeft);
                    default -> throw new IllegalArgumentException("PNG filter is unsupported");
                };
            }
            for (int x = 0; x < width; x++) {
                int dest = (y * width + x) * 4;
                if (channels == 1) {
                    byte gray = curr[x];
                    rgba[dest] = gray;
                    rgba[dest + 1] = gray;
                    rgba[dest + 2] = gray;
                    rgba[dest + 3] = (byte) 255;
                } else if (channels == 3) {
                    int src = x * 3;
                    rgba[dest] = curr[src];
                    rgba[dest + 1] = curr[src + 1];
                    rgba[dest + 2] = curr[src + 2];
                    rgba[dest + 3] = (byte) 255;
                } else {
                    System.arraycopy(curr, x * 4, rgba, dest, 4);
                }
            }
            System.arraycopy(curr, 0, prev, 0, stride);
        }
        return rgba;
    }

    /// Returns the Paeth predictor.
    private static int paeth(int left, int up, int upLeft) {
        int p = left + up - upLeft;
        int pa = Math.abs(p - left);
        int pb = Math.abs(p - up);
        int pc = Math.abs(p - upLeft);
        if (pa <= pb && pa <= pc) {
            return left;
        }
        if (pb <= pc) {
            return up;
        }
        return upLeft;
    }

    /// Builds an IHDR payload.
    private static byte[] ihdr(int width, int height) {
        ByteBuffer buffer = ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(width);
        buffer.putInt(height);
        buffer.put((byte) 8);
        buffer.put((byte) 6);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        return buffer.array();
    }

    /// Writes one PNG chunk.
    private static void writeChunk(ByteBuffer output, String type, byte[] payload) {
        output.putInt(payload.length);
        int typeOffset = output.position();
        output.put(type.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        output.put(payload);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(output.array(), typeOffset, 4 + payload.length);
        output.putInt((int) crc.getValue());
    }

    /// Deflates `raw` scanlines.
    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        deflater.setInput(raw);
        deflater.finish();
        byte[] buffer = new byte[raw.length + 64];
        int size = 0;
        while (!deflater.finished()) {
            if (size == buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length * 2);
            }
            size += deflater.deflate(buffer, size, buffer.length - size);
        }
        deflater.end();
        return Arrays.copyOf(buffer, size);
    }

    /// Inflates `idat` to exactly `expected` bytes.
    private static byte[] inflate(byte[] idat, int expected) {
        Inflater inflater = new Inflater();
        inflater.setInput(idat);
        byte[] raw = new byte[expected];
        try {
            int size = inflater.inflate(raw);
            if (size != expected || !inflater.finished()) {
                throw new IllegalArgumentException("PNG IDAT size does not match IHDR");
            }
        } catch (DataFormatException exception) {
            throw new IllegalArgumentException("PNG IDAT is invalid", exception);
        } finally {
            inflater.end();
        }
        return raw;
    }

    /// Reads a big-endian 32-bit integer.
    private static int readBe(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) << 24
                | (bytes[offset + 1] & 0xFF) << 16
                | (bytes[offset + 2] & 0xFF) << 8
                | (bytes[offset + 3] & 0xFF);
    }

    /// Returns a four-character code.
    private static int fourcc(String text) {
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return (bytes[0] & 0xFF) << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    /// Returns `width * height` after rejecting non-positive or oversized images.
    private static int checkedPixelCount(int width, int height) {
        if (width <= 0 || height <= 0 || width > PixelBuffer.MAX_EDGE || height > PixelBuffer.MAX_EDGE) {
            throw new IllegalArgumentException("PNG dimensions must be in (0, " + PixelBuffer.MAX_EDGE + "]");
        }
        return Math.multiplyExact(width, height);
    }

    /// Stores one decoded PNG image.
    ///
    /// @param width the pixel width
    /// @param height the pixel height
    /// @param rgba RGBA8 pixels
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
