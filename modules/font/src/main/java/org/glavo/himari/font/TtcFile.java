package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Unwraps the first face of a TrueType Collection (`ttcf`) into a raw SFNT/OTTO image.
///
/// Later faces, DSIG, and the header itself are ignored. A buffer whose signature is not `ttcf`
/// is returned unchanged.
@NotNullByDefault
public final class TtcFile {
    /// `ttcf` signature.
    public static final int SIGNATURE = 0x74746366;

    /// Prevents instantiation.
    private TtcFile() {
    }

    /// Returns whether `bytes` begins with the TTC signature.
    ///
    /// @param bytes the file image
    /// @return whether the signature is `ttcf`
    public static boolean isTtc(MemorySegment bytes) {
        if (bytes.byteSize() < 4) {
            return false;
        }
        return bytes.asByteBuffer().order(ByteOrder.BIG_ENDIAN).getInt(0) == SIGNATURE;
    }

    /// Returns the first SFNT face, or `bytes` when the signature is not `ttcf`.
    ///
    /// @param bytes the file image
    /// @return a raw SFNT/OTTO image
    public static MemorySegment firstFont(MemorySegment bytes) {
        ByteBuffer buffer = bytes.asByteBuffer().order(ByteOrder.BIG_ENDIAN);
        if (buffer.remaining() < 16) {
            return bytes;
        }
        if (buffer.getInt() != SIGNATURE) {
            return bytes;
        }
        buffer.getShort();
        buffer.getShort();
        int count = buffer.getInt();
        if (count < 1 || buffer.remaining() < 4) {
            throw new IllegalArgumentException("TTC font count is invalid");
        }
        int offset = buffer.getInt();
        if (offset < 0 || offset >= bytes.byteSize()) {
            throw new IllegalArgumentException("TTC first-font offset is out of range");
        }
        return bytes.asSlice(offset);
    }

    /// Wraps one SFNT face as a single-font TTC.
    ///
    /// @param sfnt the SFNT bytes
    /// @return the TTC bytes
    public static byte[] wrap(byte[] sfnt) {
        int offset = 16;
        ByteBuffer output = ByteBuffer.allocate(offset + sfnt.length).order(ByteOrder.BIG_ENDIAN);
        output.putInt(SIGNATURE);
        output.putShort((short) 1);
        output.putShort((short) 0);
        output.putInt(1);
        output.putInt(offset);
        output.put(sfnt);
        return output.array();
    }
}
