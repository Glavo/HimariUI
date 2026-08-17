package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/// Encodes and decodes Quite OK Image files as unassociated 8-bit RGBA.
///
/// The codec accepts only finite dimensions whose pixel count fits in an `int` and rejects
/// truncated or overlong streams. It is the first-stable debug image format from section 17.1.
@NotNullByDefault
public final class QoiImage {
    /// `qoif` magic.
    private static final int MAGIC = 0x716f6966;

    /// Maximum accepted width or height.
    private static final int MAX_EDGE = 16_384;

    /// Index cache size from the QOI specification.
    private static final int INDEX_SIZE = 64;

    /// Prevents instantiation.
    private QoiImage() {
    }

    /// Encodes row-major unassociated RGBA8 pixels as a QOI file.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the QOI bytes
    public static byte @Unmodifiable [] encode(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        ByteBuffer output = ByteBuffer.allocate(14 + pixelCount * 5 + 8).order(ByteOrder.BIG_ENDIAN);
        output.putInt(MAGIC);
        output.putInt(width);
        output.putInt(height);
        output.put((byte) 4);
        output.put((byte) 0);
        int[] index = new int[INDEX_SIZE];
        int previous = 0x000000FF;
        int run = 0;
        for (int pixel = 0; pixel < pixelCount; pixel++) {
            int offset = pixel * 4;
            int rgba32 = rgba32(rgba[offset], rgba[offset + 1], rgba[offset + 2], rgba[offset + 3]);
            if (rgba32 == previous) {
                run++;
                if (run == 62 || pixel == pixelCount - 1) {
                    output.put((byte) (0xC0 | (run - 1)));
                    run = 0;
                }
                continue;
            }
            if (run > 0) {
                output.put((byte) (0xC0 | (run - 1)));
                run = 0;
            }
            int hash = hash(rgba32);
            if (index[hash] == rgba32) {
                output.put((byte) hash);
            } else {
                index[hash] = rgba32;
                int dr = channel(rgba32, 24) - channel(previous, 24);
                int dg = channel(rgba32, 16) - channel(previous, 16);
                int db = channel(rgba32, 8) - channel(previous, 8);
                int da = channel(rgba32, 0) - channel(previous, 0);
                if (da == 0 && inDiff(dr) && inDiff(dg) && inDiff(db)) {
                    output.put((byte) (0x40 | (dr + 2) << 4 | (dg + 2) << 2 | (db + 2)));
                } else if (da == 0 && inLumaGreen(dg) && inLumaChroma(dr - dg) && inLumaChroma(db - dg)) {
                    output.put((byte) (0x80 | (dg + 32)));
                    output.put((byte) ((dr - dg + 8) << 4 | (db - dg + 8)));
                } else if (da == 0) {
                    output.put((byte) 0xFE);
                    output.put((byte) channel(rgba32, 24));
                    output.put((byte) channel(rgba32, 16));
                    output.put((byte) channel(rgba32, 8));
                } else {
                    output.put((byte) 0xFF);
                    output.put((byte) channel(rgba32, 24));
                    output.put((byte) channel(rgba32, 16));
                    output.put((byte) channel(rgba32, 8));
                    output.put((byte) channel(rgba32, 0));
                }
            }
            previous = rgba32;
        }
        output.putLong(1L);
        byte[] encoded = new byte[output.position()];
        output.flip();
        output.get(encoded);
        return encoded;
    }

    /// Decodes a QOI file into row-major unassociated RGBA8 pixels.
    ///
    /// @param bytes the QOI stream
    /// @return the decoded image
    public static Decoded decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < 22) {
            throw new IllegalArgumentException("QOI stream is truncated");
        }
        ByteBuffer input = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        if (input.getInt() != MAGIC) {
            throw new IllegalArgumentException("QOI magic must be qoif");
        }
        int width = input.getInt();
        int height = input.getInt();
        int channels = input.get() & 0xFF;
        int colorspace = input.get() & 0xFF;
        if (channels != 3 && channels != 4) {
            throw new IllegalArgumentException("QOI channels must be 3 or 4");
        }
        if (colorspace > 1) {
            throw new IllegalArgumentException("QOI colorspace must be 0 or 1");
        }
        int pixelCount = checkedPixelCount(width, height);
        byte[] rgba = new byte[pixelCount * 4];
        int[] index = new int[INDEX_SIZE];
        int previous = 0x000000FF;
        int pixel = 0;
        while (pixel < pixelCount) {
            if (input.remaining() < 8) {
                throw new IllegalArgumentException("QOI stream is truncated");
            }
            int tag = input.get() & 0xFF;
            int rgba32;
            if (tag == 0xFE) {
                rgba32 = rgba32(input.get(), input.get(), input.get(), (byte) channel(previous, 0));
            } else if (tag == 0xFF) {
                rgba32 = rgba32(input.get(), input.get(), input.get(), input.get());
            } else if ((tag & 0xC0) == 0x00) {
                rgba32 = index[tag];
            } else if ((tag & 0xC0) == 0x40) {
                int dr = ((tag >>> 4) & 0x03) - 2;
                int dg = ((tag >>> 2) & 0x03) - 2;
                int db = (tag & 0x03) - 2;
                rgba32 = add(previous, dr, dg, db, 0);
            } else if ((tag & 0xC0) == 0x80) {
                int next = input.get() & 0xFF;
                int dg = (tag & 0x3F) - 32;
                int dr = ((next >>> 4) & 0x0F) - 8 + dg;
                int db = (next & 0x0F) - 8 + dg;
                rgba32 = add(previous, dr, dg, db, 0);
            } else {
                int run = (tag & 0x3F) + 1;
                if (pixel + run > pixelCount) {
                    throw new IllegalArgumentException("QOI run exceeds pixel count");
                }
                writePixel(rgba, pixel, previous);
                for (int extra = 1; extra < run; extra++) {
                    writePixel(rgba, pixel + extra, previous);
                }
                pixel += run;
                continue;
            }
            index[hash(rgba32)] = rgba32;
            writePixel(rgba, pixel, rgba32);
            previous = rgba32;
            pixel++;
        }
        if (input.remaining() != 8 || input.getLong() != 1L) {
            throw new IllegalArgumentException("QOI end marker is missing");
        }
        return new Decoded(width, height, rgba);
    }

    /// Returns `width * height` after rejecting non-positive or oversized images.
    private static int checkedPixelCount(int width, int height) {
        if (width <= 0 || height <= 0 || width > MAX_EDGE || height > MAX_EDGE) {
            throw new IllegalArgumentException("QOI dimensions must be in (0, " + MAX_EDGE + "]");
        }
        return Math.multiplyExact(width, height);
    }

    /// Packs one RGBA pixel as `0xRRGGBBAA`.
    private static int rgba32(byte red, byte green, byte blue, byte alpha) {
        return (red & 0xFF) << 24 | (green & 0xFF) << 16 | (blue & 0xFF) << 8 | (alpha & 0xFF);
    }

    /// Returns one packed channel.
    private static int channel(int rgba32, int shift) {
        return (rgba32 >>> shift) & 0xFF;
    }

    /// Adds signed channel deltas to a packed pixel.
    private static int add(int rgba32, int dr, int dg, int db, int da) {
        return rgba32(
                (byte) (channel(rgba32, 24) + dr),
                (byte) (channel(rgba32, 16) + dg),
                (byte) (channel(rgba32, 8) + db),
                (byte) (channel(rgba32, 0) + da)
        );
    }

    /// Returns whether a difference fits a QOI DIFF chunk.
    private static boolean inDiff(int delta) {
        return delta >= -2 && delta <= 1;
    }

    /// Returns whether a green difference fits a QOI LUMA chunk.
    private static boolean inLumaGreen(int delta) {
        return delta >= -32 && delta <= 31;
    }

    /// Returns whether a red/blue bias fits a QOI LUMA chunk.
    private static boolean inLumaChroma(int delta) {
        return delta >= -8 && delta <= 7;
    }

    /// Returns the QOI index hash of a packed pixel.
    private static int hash(int rgba32) {
        int red = channel(rgba32, 24);
        int green = channel(rgba32, 16);
        int blue = channel(rgba32, 8);
        int alpha = channel(rgba32, 0);
        return (red * 3 + green * 5 + blue * 7 + alpha * 11) & 63;
    }

    /// Writes one packed pixel into `rgba` at `pixel`.
    private static void writePixel(byte[] rgba, int pixel, int rgba32) {
        int offset = pixel * 4;
        rgba[offset] = (byte) channel(rgba32, 24);
        rgba[offset + 1] = (byte) channel(rgba32, 16);
        rgba[offset + 2] = (byte) channel(rgba32, 8);
        rgba[offset + 3] = (byte) channel(rgba32, 0);
    }

    /// Stores one decoded QOI image.
    ///
    /// @param width the pixel width
    /// @param height the pixel height
    /// @param rgba unassociated RGBA8 pixels
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
