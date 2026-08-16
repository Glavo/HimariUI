package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/// Unwraps a WOFF1 collection of SFNT tables into a raw SFNT/OTTO file.
///
/// Compressed table payloads use zlib. Metadata and private blocks are ignored. A buffer whose
/// signature is not `wOFF` is returned unchanged.
@NotNullByDefault
public final class WoffFile {
    /// `wOFF` signature.
    public static final int SIGNATURE = 0x774F4646;

    /// Prevents instantiation.
    private WoffFile() {
    }

    /// Returns whether `bytes` begins with the WOFF1 signature.
    ///
    /// @param bytes the file image
    /// @return whether the signature is `wOFF`
    public static boolean isWoff(MemorySegment bytes) {
        if (bytes.byteSize() < 4) {
            return false;
        }
        return bytes.asByteBuffer().order(ByteOrder.BIG_ENDIAN).getInt(0) == SIGNATURE;
    }

    /// Unwraps a WOFF1 image, or returns `bytes` when the signature is not `wOFF`.
    ///
    /// @param bytes the file image
    /// @return a raw SFNT/OTTO image
    public static MemorySegment unwrap(MemorySegment bytes) {
        ByteBuffer buffer = bytes.asByteBuffer().order(ByteOrder.BIG_ENDIAN);
        if (buffer.remaining() < 44) {
            return bytes;
        }
        int signature = buffer.getInt();
        if (signature != SIGNATURE) {
            buffer.rewind();
            return bytes;
        }
        int flavor = buffer.getInt();
        int length = buffer.getInt();
        int tableCount = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        buffer.getInt();
        buffer.getShort();
        buffer.getShort();
        buffer.getInt();
        buffer.getInt();
        buffer.getInt();
        buffer.getInt();
        buffer.getInt();
        if (length != bytes.byteSize() || tableCount < 1) {
            throw new IllegalArgumentException("WOFF header length or table count is invalid");
        }
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        int[] offsets = new int[tableCount];
        int[] compLengths = new int[tableCount];
        int[] origLengths = new int[tableCount];
        String[] tags = new String[tableCount];
        for (int index = 0; index < tableCount; index++) {
            if (buffer.remaining() < 20) {
                throw new IllegalArgumentException("WOFF table directory is truncated");
            }
            byte[] tagBytes = new byte[4];
            buffer.get(tagBytes);
            tags[index] = new String(tagBytes, StandardCharsets.US_ASCII);
            offsets[index] = buffer.getInt();
            compLengths[index] = buffer.getInt();
            origLengths[index] = buffer.getInt();
            buffer.getInt();
        }
        byte[] source = new byte[buffer.capacity()];
        bytes.asByteBuffer().get(source);
        for (int index = 0; index < tableCount; index++) {
            int offset = offsets[index];
            int compressed = compLengths[index];
            int original = origLengths[index];
            if (offset < 0 || compressed < 0 || original < 0
                    || (long) offset + (long) compressed > source.length) {
                throw new IllegalArgumentException("WOFF table payload is out of range");
            }
            byte[] payload = new byte[compressed];
            System.arraycopy(source, offset, payload, 0, compressed);
            if (compressed == original) {
                tables.put(tags[index], payload);
            } else {
                tables.put(tags[index], inflate(payload, original));
            }
        }
        return MemorySegment.ofArray(BitmapSfntFont.wrap(flavor, tables));
    }

    /// Wraps a raw SFNT/OTTO image as an uncompressed WOFF1 file.
    ///
    /// @param sfnt the SFNT bytes
    /// @return the WOFF1 bytes
    public static byte[] wrapUncompressed(byte[] sfnt) {
        ByteBuffer source = ByteBuffer.wrap(sfnt).order(ByteOrder.BIG_ENDIAN);
        if (source.remaining() < 12) {
            throw new IllegalArgumentException("SFNT header is truncated");
        }
        int flavor = source.getInt();
        int tableCount = Short.toUnsignedInt(source.getShort());
        source.getShort();
        source.getShort();
        source.getShort();
        String[] tags = new String[tableCount];
        int[] offsets = new int[tableCount];
        int[] lengths = new int[tableCount];
        for (int index = 0; index < tableCount; index++) {
            byte[] tagBytes = new byte[4];
            source.get(tagBytes);
            tags[index] = new String(tagBytes, StandardCharsets.US_ASCII);
            source.getInt();
            offsets[index] = source.getInt();
            lengths[index] = source.getInt();
        }
        int header = 44 + tableCount * 20;
        int running = header;
        int[] woffOffsets = new int[tableCount];
        int fileSize = header;
        for (int index = 0; index < tableCount; index++) {
            woffOffsets[index] = running;
            int padded = (lengths[index] + 3) & ~3;
            running += padded;
            fileSize += padded;
        }
        ByteBuffer output = ByteBuffer.allocate(fileSize).order(ByteOrder.BIG_ENDIAN);
        output.putInt(SIGNATURE);
        output.putInt(flavor);
        output.putInt(fileSize);
        output.putShort((short) tableCount);
        output.putShort((short) 0);
        output.putInt(sfnt.length);
        output.putShort((short) 1);
        output.putShort((short) 0);
        output.putInt(0);
        output.putInt(0);
        output.putInt(0);
        output.putInt(0);
        output.putInt(0);
        for (int index = 0; index < tableCount; index++) {
            output.put(tags[index].getBytes(StandardCharsets.US_ASCII));
            output.putInt(woffOffsets[index]);
            output.putInt(lengths[index]);
            output.putInt(lengths[index]);
            output.putInt(0);
        }
        for (int index = 0; index < tableCount; index++) {
            output.position(woffOffsets[index]);
            output.put(sfnt, offsets[index], lengths[index]);
        }
        return output.array();
    }

    /// Wraps a raw SFNT/OTTO image as a zlib-compressed WOFF1 file.
    ///
    /// Every table payload is deflated even when the compressed size is not smaller than the
    /// original, so [`#unwrap(MemorySegment)`] must inflate each directory entry.
    ///
    /// @param sfnt the SFNT bytes
    /// @return the WOFF1 bytes
    public static byte[] wrapCompressed(byte[] sfnt) {
        ByteBuffer source = ByteBuffer.wrap(sfnt).order(ByteOrder.BIG_ENDIAN);
        if (source.remaining() < 12) {
            throw new IllegalArgumentException("SFNT header is truncated");
        }
        int flavor = source.getInt();
        int tableCount = Short.toUnsignedInt(source.getShort());
        source.getShort();
        source.getShort();
        source.getShort();
        String[] tags = new String[tableCount];
        int[] offsets = new int[tableCount];
        int[] lengths = new int[tableCount];
        byte[][] compressed = new byte[tableCount][];
        for (int index = 0; index < tableCount; index++) {
            byte[] tagBytes = new byte[4];
            source.get(tagBytes);
            tags[index] = new String(tagBytes, StandardCharsets.US_ASCII);
            source.getInt();
            offsets[index] = source.getInt();
            lengths[index] = source.getInt();
        }
        int header = 44 + tableCount * 20;
        int running = header;
        int[] woffOffsets = new int[tableCount];
        int fileSize = header;
        for (int index = 0; index < tableCount; index++) {
            byte[] original = new byte[lengths[index]];
            System.arraycopy(sfnt, offsets[index], original, 0, lengths[index]);
            compressed[index] = deflate(original);
            woffOffsets[index] = running;
            int padded = (compressed[index].length + 3) & ~3;
            running += padded;
            fileSize += padded;
        }
        ByteBuffer output = ByteBuffer.allocate(fileSize).order(ByteOrder.BIG_ENDIAN);
        output.putInt(SIGNATURE);
        output.putInt(flavor);
        output.putInt(fileSize);
        output.putShort((short) tableCount);
        output.putShort((short) 0);
        output.putInt(sfnt.length);
        output.putShort((short) 1);
        output.putShort((short) 0);
        output.putInt(0);
        output.putInt(0);
        output.putInt(0);
        output.putInt(0);
        output.putInt(0);
        for (int index = 0; index < tableCount; index++) {
            output.put(tags[index].getBytes(StandardCharsets.US_ASCII));
            output.putInt(woffOffsets[index]);
            output.putInt(compressed[index].length);
            output.putInt(lengths[index]);
            output.putInt(0);
        }
        for (int index = 0; index < tableCount; index++) {
            output.position(woffOffsets[index]);
            output.put(compressed[index]);
        }
        return output.array();
    }

    /// Deflates a table payload with zlib.
    private static byte[] deflate(byte[] original) {
        Deflater deflater = new Deflater();
        try {
            deflater.setInput(original);
            deflater.finish();
            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(16, original.length));
            byte[] chunk = new byte[256];
            while (!deflater.finished()) {
                int written = deflater.deflate(chunk);
                output.write(chunk, 0, written);
            }
            return output.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /// Inflates a zlib table payload to `original` bytes.
    private static byte[] inflate(byte[] compressed, int original) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            byte[] inflated = new byte[original];
            int written = inflater.inflate(inflated);
            if (written != original || !inflater.finished()) {
                throw new IllegalArgumentException("WOFF zlib payload did not inflate to the declared length");
            }
            return inflated;
        } catch (DataFormatException failure) {
            throw new IllegalArgumentException("WOFF zlib payload is malformed", failure);
        } finally {
            inflater.end();
        }
    }
}
