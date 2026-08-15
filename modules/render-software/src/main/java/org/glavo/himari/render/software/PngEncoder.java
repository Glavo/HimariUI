package org.glavo.himari.render.software;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/// Encodes 8-bit RGBA images as PNG.
@NotNullByDefault
public final class PngEncoder {
    /// The PNG signature.
    private static final byte[] SIGNATURE = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    /// Prevents instantiation.
    private PngEncoder() {
    }

    /// Encodes one 8-bit RGBA image.
    ///
    /// @param width the positive width
    /// @param height the positive height
    /// @param rgba row-major unassociated RGBA samples
    /// @return the PNG file
    public static byte[] encodeRgba(int width, int height, byte[] rgba) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("PNG extents must be positive");
        }
        Objects.requireNonNull(rgba, "rgba");
        if (rgba.length != Math.multiplyExact(Math.multiplyExact(width, height), 4)) {
            throw new IllegalArgumentException("RGBA buffer length does not match extents");
        }
        byte[] raw = new byte[height * (1 + width * 4)];
        int dest = 0;
        int source = 0;
        for (int row = 0; row < height; row++) {
            raw[dest++] = 0;
            int rowBytes = width * 4;
            System.arraycopy(rgba, source, raw, dest, rowBytes);
            dest += rowBytes;
            source += rowBytes;
        }
        byte[] deflated = deflate(raw);
        ByteArrayOutputStream output = new ByteArrayOutputStream(deflated.length + 80);
        try {
            output.write(SIGNATURE);
            writeChunk(output, "IHDR", ihdr(width, height));
            writeChunk(output, "IDAT", deflated);
            writeChunk(output, "IEND", new byte[0]);
        } catch (IOException exception) {
            throw new IllegalStateException("PNG encoding failed", exception);
        }
        return output.toByteArray();
    }

    /// Builds the IHDR payload.
    ///
    /// @param width the width
    /// @param height the height
    /// @return the payload
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
    ///
    /// @param output the destination
    /// @param type the chunk type
    /// @param payload the payload
    private static void writeChunk(ByteArrayOutputStream output, String type, byte[] payload) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(payload.length);
        output.write(header.array());
        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        output.write(typeBytes);
        output.write(payload);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(payload);
        ByteBuffer crcBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt((int) crc.getValue());
        output.write(crcBuffer.array());
    }

    /// Deflates the filtered image.
    ///
    /// @param raw the filtered bytes
    /// @return the zlib payload
    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        deflater.setInput(raw);
        deflater.finish();
        byte[] buffer = new byte[Math.max(64, raw.length / 2)];
        ByteArrayOutputStream output = new ByteArrayOutputStream(raw.length);
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            output.write(buffer, 0, count);
        }
        deflater.end();
        return output.toByteArray();
    }
}
