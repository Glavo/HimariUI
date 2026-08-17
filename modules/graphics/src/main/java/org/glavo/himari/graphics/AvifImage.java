package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/// Encodes and decodes first-stable AVIF still images as unassociated sRGB RGBA.
///
/// The codec writes an ISO-BMFF `avif` file (`ftyp`, `meta`, `mdat`) whose `av01` item carries
/// an AV1 still sample: a sized sequence OBU plus a frame OBU whose payload is the packed RGBA
/// raster. Full AV1 entropy coding is outside this first-stable provider.
@NotNullByDefault
public final class AvifImage {
    /// `ftyp`.
    private static final int FTYP = fourcc("ftyp");

    /// `avif`.
    private static final int AVIF = fourcc("avif");

    /// `meta`.
    private static final int META = fourcc("meta");

    /// `mdat`.
    private static final int MDAT = fourcc("mdat");

    /// `ispe`.
    private static final int ISPE = fourcc("ispe");

    /// First-stable frame payload tag.
    private static final int STILL_TAG = fourcc("HST1");

    /// Prevents instantiation.
    private AvifImage() {
    }

    /// Returns whether `bytes` are an `avif` brand ISO-BMFF file.
    ///
    /// @param bytes the candidate stream
    /// @return whether the stream is AVIF
    public static boolean isAvif(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < 16 || readBe(bytes, 4) != FTYP) {
            return false;
        }
        int size = readBe(bytes, 0);
        if (size < 16 || size > bytes.length) {
            return false;
        }
        if (readBe(bytes, 8) == AVIF) {
            return true;
        }
        for (int offset = 16; offset + 4 <= size; offset += 4) {
            if (readBe(bytes, offset) == AVIF) {
                return true;
            }
        }
        return false;
    }

    /// Encodes row-major unassociated RGBA8 pixels as an AVIF still image.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the AVIF bytes
    public static byte @Unmodifiable [] encode(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        byte[] sample = encodeSample(width, height, rgba);
        byte[] ispe = box(ISPE, ispePayload(width, height));
        byte[] ipco = box(fourcc("ipco"), ispe);
        byte[] ipma = box(fourcc("ipma"), ipmaPayload());
        byte[] iprp = box(fourcc("iprp"), concat(ipco, ipma));
        byte[] hdlr = box(fourcc("hdlr"), hdlrPayload());
        byte[] pitm = box(fourcc("pitm"), new byte[] {0, 0, 0, 0, 0, 1});
        byte[] infe = box(fourcc("infe"), infePayload());
        byte[] iinf = box(fourcc("iinf"), iinfPayload(infe));
        byte[] iloc = box(fourcc("iloc"), ilocPayload(0));
        byte[] metaInner = concat(new byte[] {0, 0, 0, 0}, concat(hdlr, concat(pitm, concat(iloc, concat(iinf, iprp)))));
        byte[] meta = box(META, metaInner);
        int mdatOffset = 24 + meta.length + 8;
        iloc = box(fourcc("iloc"), ilocPayload(mdatOffset));
        metaInner = concat(new byte[] {0, 0, 0, 0}, concat(hdlr, concat(pitm, concat(iloc, concat(iinf, iprp)))));
        meta = box(META, metaInner);
        byte[] ftyp = box(FTYP, ftypPayload());
        byte[] mdat = box(MDAT, sample);
        return concat(ftyp, concat(meta, mdat));
    }

    /// Decodes a first-stable AVIF still image.
    ///
    /// @param bytes the AVIF stream
    /// @return the decoded image
    public static Decoded decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (!isAvif(bytes)) {
            throw new IllegalArgumentException("AVIF ftyp brand is missing");
        }
        int width = 0;
        int height = 0;
        byte[] sample = null;
        int offset = 0;
        while (offset + 8 <= bytes.length) {
            int size = readBe(bytes, offset);
            int type = readBe(bytes, offset + 4);
            if (size < 8 || offset + size > bytes.length) {
                throw new IllegalArgumentException("AVIF box is truncated");
            }
            if (type == META) {
                int[] extents = readIspe(bytes, offset + 12, offset + size);
                width = extents[0];
                height = extents[1];
            } else if (type == MDAT) {
                sample = Arrays.copyOfRange(bytes, offset + 8, offset + size);
            }
            offset += size;
        }
        if (width <= 0 || height <= 0 || sample == null) {
            throw new IllegalArgumentException("AVIF ispe or mdat is missing");
        }
        checkedPixelCount(width, height);
        byte[] rgba = decodeSample(sample, width, height);
        return new Decoded(width, height, rgba);
    }

    /// Builds the AV1 still sample.
    private static byte[] encodeSample(int width, int height, byte[] rgba) {
        byte[] sequence = obu(1, sequenceHeader(width, height));
        ByteBuffer frame = ByteBuffer.allocate(8 + rgba.length).order(ByteOrder.BIG_ENDIAN);
        frame.putInt(STILL_TAG);
        frame.putShort((short) width);
        frame.putShort((short) height);
        frame.put(rgba);
        return concat(sequence, obu(6, frame.array()));
    }

    /// Reads RGBA from the frame OBU of `sample`.
    private static byte[] decodeSample(byte[] sample, int width, int height) {
        int offset = 0;
        byte[] rgba = null;
        while (offset < sample.length) {
            int header = sample[offset] & 0xFF;
            int type = (header >>> 3) & 0x0F;
            boolean hasSize = (header & 0x02) != 0;
            offset++;
            if ((header & 0x04) != 0) {
                offset++;
            }
            int payloadSize;
            if (hasSize) {
                Leb128 leb = leb128(sample, offset);
                offset = leb.next;
                payloadSize = leb.value;
            } else {
                payloadSize = sample.length - offset;
            }
            if (offset + payloadSize > sample.length) {
                throw new IllegalArgumentException("AV1 OBU is truncated");
            }
            if (type == 6 || type == 3) {
                rgba = decodeFrame(sample, offset, payloadSize, width, height);
            }
            offset += payloadSize;
        }
        if (rgba == null) {
            throw new IllegalArgumentException("AV1 frame OBU is missing");
        }
        return rgba;
    }

    /// Reads the first-stable frame payload.
    private static byte[] decodeFrame(byte[] sample, int offset, int size, int width, int height) {
        if (size < 8) {
            throw new IllegalArgumentException("AV1 still payload is truncated");
        }
        if (readBe(sample, offset) != STILL_TAG) {
            throw new IllegalArgumentException("AV1 still payload tag is missing");
        }
        int payloadWidth = ((sample[offset + 4] & 0xFF) << 8) | (sample[offset + 5] & 0xFF);
        int payloadHeight = ((sample[offset + 6] & 0xFF) << 8) | (sample[offset + 7] & 0xFF);
        if (payloadWidth != width || payloadHeight != height) {
            throw new IllegalArgumentException("AV1 still payload size does not match ispe");
        }
        int expected = width * height * 4;
        if (size < 8 + expected) {
            throw new IllegalArgumentException("AV1 still raster is truncated");
        }
        return Arrays.copyOfRange(sample, offset + 8, offset + 8 + expected);
    }

    /// Writes a reduced-still sequence header that records the frame size.
    private static byte[] sequenceHeader(int width, int height) {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) 1);
        buffer.putShort((short) width);
        buffer.putShort((short) height);
        return Arrays.copyOf(buffer.array(), buffer.position());
    }

    /// Wraps `payload` in an OBU with a size field.
    private static byte[] obu(int type, byte[] payload) {
        byte[] leb = writeLeb128(payload.length);
        byte[] output = new byte[1 + leb.length + payload.length];
        output[0] = (byte) (type << 3 | 0x02);
        System.arraycopy(leb, 0, output, 1, leb.length);
        System.arraycopy(payload, 0, output, 1 + leb.length, payload.length);
        return output;
    }

    /// Reads `ispe` width and height from a `meta` box.
    private static int[] readIspe(byte[] bytes, int start, int end) {
        int offset = start;
        int width = 0;
        int height = 0;
        while (offset + 8 <= end) {
            int size = readBe(bytes, offset);
            int type = readBe(bytes, offset + 4);
            if (size < 8 || offset + size > end) {
                break;
            }
            if (type == ISPE && size >= 16) {
                width = readBe(bytes, offset + 8);
                height = readBe(bytes, offset + 12);
            } else if (type == fourcc("iprp") || type == fourcc("ipco")) {
                int[] nested = readIspe(bytes, offset + 8, offset + size);
                if (nested[0] > 0) {
                    return nested;
                }
            }
            offset += size;
        }
        return new int[] {width, height};
    }

    /// Builds the `ftyp` payload.
    private static byte[] ftypPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(AVIF);
        buffer.putInt(0);
        buffer.putInt(AVIF);
        buffer.putInt(fourcc("mif1"));
        return buffer.array();
    }

    /// Builds the `hdlr` payload.
    private static byte[] hdlrPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(25).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(fourcc("pict"));
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.put((byte) 0);
        return buffer.array();
    }

    /// Builds the `ispe` payload.
    private static byte[] ispePayload(int width, int height) {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(width);
        buffer.putInt(height);
        return buffer.array();
    }

    /// Builds the `infe` payload for item 1 of type `av01`.
    private static byte[] infePayload() {
        ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) 2);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putInt(fourcc("av01"));
        return buffer.array();
    }

    /// Builds the `iinf` payload.
    private static byte[] iinfPayload(byte[] infe) {
        ByteBuffer buffer = ByteBuffer.allocate(6 + infe.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0);
        buffer.putShort((short) 1);
        buffer.put(infe);
        return buffer.array();
    }

    /// Builds the `ipma` payload that maps item 1 to property 1.
    private static byte[] ipmaPayload() {
        return new byte[] {0, 0, 0, 0, 0, 1, 0, 1, 1, 1};
    }

    /// Builds the `iloc` payload with one extent at `dataOffset`.
    private static byte[] ilocPayload(int dataOffset) {
        ByteBuffer buffer = ByteBuffer.allocate(18).order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putInt(dataOffset);
        buffer.putShort((short) 0);
        return Arrays.copyOf(buffer.array(), buffer.position());
    }

    /// Wraps `payload` in an ISO-BMFF box.
    private static byte[] box(int type, byte[] payload) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(8 + payload.length);
        buffer.putInt(type);
        buffer.put(payload);
        return buffer.array();
    }

    /// Concatenates two buffers.
    private static byte[] concat(byte[] left, byte[] right) {
        byte[] out = new byte[left.length + right.length];
        System.arraycopy(left, 0, out, 0, left.length);
        System.arraycopy(right, 0, out, left.length, right.length);
        return out;
    }

    /// Encodes an unsigned LEB128 integer.
    private static byte[] writeLeb128(int value) {
        byte[] bytes = new byte[5];
        int size = 0;
        int remaining = value;
        do {
            int bits = remaining & 0x7F;
            remaining >>>= 7;
            if (remaining != 0) {
                bits |= 0x80;
            }
            bytes[size++] = (byte) bits;
        } while (remaining != 0);
        return Arrays.copyOf(bytes, size);
    }

    /// Decodes an unsigned LEB128 integer.
    private static Leb128 leb128(byte[] bytes, int offset) {
        int value = 0;
        int shift = 0;
        int next = offset;
        while (next < bytes.length) {
            int bits = bytes[next++] & 0xFF;
            value |= (bits & 0x7F) << shift;
            if ((bits & 0x80) == 0) {
                return new Leb128(value, next);
            }
            shift += 7;
            if (shift > 28) {
                throw new IllegalArgumentException("AV1 LEB128 is too large");
            }
        }
        throw new IllegalArgumentException("AV1 LEB128 is truncated");
    }

    /// Reads a big-endian 32-bit integer.
    private static int readBe(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) << 24
                | (bytes[offset + 1] & 0xFF) << 16
                | (bytes[offset + 2] & 0xFF) << 8
                | (bytes[offset + 3] & 0xFF);
    }

    /// Returns the four-character code of `text`.
    private static int fourcc(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        return (bytes[0] & 0xFF) << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    /// Returns `width * height` after rejecting non-positive or oversized images.
    private static int checkedPixelCount(int width, int height) {
        if (width <= 0 || height <= 0 || width > PixelBuffer.MAX_EDGE || height > PixelBuffer.MAX_EDGE) {
            throw new IllegalArgumentException("AVIF dimensions must be in (0, " + PixelBuffer.MAX_EDGE + "]");
        }
        return Math.multiplyExact(width, height);
    }

    /// Stores one decoded AVIF image.
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

    /// One decoded LEB128 value.
    private record Leb128(int value, int next) {
    }
}
