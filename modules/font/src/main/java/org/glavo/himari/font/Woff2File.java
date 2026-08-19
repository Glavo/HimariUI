package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Unwraps a WOFF2 collection of SFNT tables into a raw SFNT/OTTO file.
///
/// Table payloads are concatenated in directory order and inflated with [`Brotli`]. [`#wrap(byte[])`]
/// stores every table with the null transform (`glyf`/`loca` version 3, every other table version
/// 0). [`#wrapTransformed(byte[])`] emits the WOFF2 version-0 `glyf`/`loca` transform and the
/// version-1 `hmtx` transform when the side bearings match `xMin`. [`#unwrap(MemorySegment)`]
/// reconstructs those transforms through [`Woff2Glyf`]. Metadata and private blocks are ignored.
/// A TTC collection directory yields the first face. A buffer whose signature is not `wOF2` is
/// returned unchanged.
@NotNullByDefault
public final class Woff2File {
    /// `wOF2` signature.
    public static final int SIGNATURE = 0x774F4632;

    /// Known tag index that means a custom four-byte tag follows.
    private static final int CUSTOM_TAG = 63;

    /// Null-transform version for `glyf` and `loca`.
    private static final int NULL_GLYF_LOCA = 3;

    /// Known WOFF2 tags, index 0 through 62.
    private static final String[] KNOWN_TAGS = {
            "cmap", "head", "hhea", "hmtx", "maxp", "name", "OS/2", "post",
            "cvt ", "fpgm", "glyf", "loca", "prep", "CFF ", "VORG", "EBDT",
            "EBLC", "gasp", "hdmx", "kern", "LTSH", "PCLT", "VDMX", "vhea",
            "vmtx", "BASE", "GDEF", "GPOS", "GSUB", "EBSC", "JSTF", "MATH",
            "CBDT", "CBLC", "COLR", "CPAL", "SVG ", "sbix", "acnt", "avar",
            "bdat", "bloc", "bsln", "cvar", "fdsc", "feat", "fmtx", "fvar",
            "gvar", "hsty", "just", "lcar", "mort", "morx", "opbd", "prop",
            "trak", "Zapf", "Silf", "Glat", "Gloc", "Feat", "Sill"
    };

    /// Prevents instantiation.
    private Woff2File() {
    }

    /// Returns whether `bytes` begins with the WOFF2 signature.
    ///
    /// @param bytes the file image
    /// @return whether the signature is `wOF2`
    public static boolean isWoff2(MemorySegment bytes) {
        if (bytes.byteSize() < 4) {
            return false;
        }
        return bytes.asByteBuffer().order(ByteOrder.BIG_ENDIAN).getInt(0) == SIGNATURE;
    }

    /// Unwraps a WOFF2 image, or returns `bytes` when the signature is not `wOF2`.
    ///
    /// @param bytes the file image
    /// @return a raw SFNT/OTTO image
    public static MemorySegment unwrap(MemorySegment bytes) {
        ByteBuffer buffer = bytes.asByteBuffer().order(ByteOrder.BIG_ENDIAN);
        if (buffer.remaining() < 48) {
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
        int compressedSize = buffer.getInt();
        buffer.getShort();
        buffer.getShort();
        buffer.getInt();
        buffer.getInt();
        buffer.getInt();
        buffer.getInt();
        buffer.getInt();
        if (length != bytes.byteSize() || tableCount < 1 || compressedSize < 0) {
            throw new IllegalArgumentException("WOFF2 header length or table count is invalid");
        }
        String[] tags = new String[tableCount];
        int[] origLengths = new int[tableCount];
        int[] streamLengths = new int[tableCount];
        int[] transforms = new int[tableCount];
        for (int index = 0; index < tableCount; index++) {
            if (!buffer.hasRemaining()) {
                throw new IllegalArgumentException("WOFF2 table directory is truncated");
            }
            int flags = Byte.toUnsignedInt(buffer.get());
            int tagIndex = flags & 0x3F;
            int transform = (flags >>> 6) & 0x3;
            String tag;
            if (tagIndex == CUSTOM_TAG) {
                if (buffer.remaining() < 4) {
                    throw new IllegalArgumentException("WOFF2 custom tag is truncated");
                }
                byte[] tagBytes = new byte[4];
                buffer.get(tagBytes);
                tag = new String(tagBytes, StandardCharsets.US_ASCII);
            } else {
                tag = KNOWN_TAGS[tagIndex];
            }
            int orig = readBase128(buffer);
            boolean transformed = isTransformed(tag, transform);
            int stream = orig;
            if (transformed) {
                stream = readBase128(buffer);
                if (tag.equals("loca") && stream != 0) {
                    throw new IllegalArgumentException("WOFF2 transformed loca must have transformLength 0");
                }
            }
            tags[index] = tag;
            origLengths[index] = orig;
            streamLengths[index] = stream;
            transforms[index] = transform;
        }
        int faceFlavor = flavor;
        int[] faceTables = null;
        if (flavor == TtcFile.SIGNATURE) {
            buffer.getInt();
            int fonts = read255UInt16(buffer);
            if (fonts < 1) {
                throw new IllegalArgumentException("WOFF2 collection has no fonts");
            }
            int faceTableCount = read255UInt16(buffer);
            faceFlavor = buffer.getInt();
            faceTables = new int[faceTableCount];
            for (int index = 0; index < faceTableCount; index++) {
                int tableIndex = read255UInt16(buffer);
                if (tableIndex < 0 || tableIndex >= tableCount) {
                    throw new IllegalArgumentException("WOFF2 collection table index is out of range");
                }
                faceTables[index] = tableIndex;
            }
            for (int font = 1; font < fonts; font++) {
                int skipped = read255UInt16(buffer);
                buffer.getInt();
                for (int index = 0; index < skipped; index++) {
                    read255UInt16(buffer);
                }
            }
        }
        if (buffer.remaining() < compressedSize) {
            throw new IllegalArgumentException("WOFF2 Brotli payload is truncated");
        }
        byte[] compressed = new byte[compressedSize];
        buffer.get(compressed);
        byte[] inflated = Brotli.decompress(compressed);
        int expected = 0;
        for (int stream : streamLengths) {
            expected += stream;
        }
        if (inflated.length != expected) {
            throw new IllegalArgumentException("WOFF2 Brotli payload did not inflate to the directory size");
        }
        byte[][] payloads = new byte[tableCount][];
        int offset = 0;
        for (int index = 0; index < tableCount; index++) {
            byte[] payload = new byte[streamLengths[index]];
            System.arraycopy(inflated, offset, payload, 0, payload.length);
            offset += payload.length;
            payloads[index] = payload;
        }
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        if (faceTables == null) {
            for (int index = 0; index < tableCount; index++) {
                tables.put(tags[index], payloads[index]);
            }
        } else {
            for (int tableIndex : faceTables) {
                tables.put(tags[tableIndex], payloads[tableIndex]);
            }
        }
        reconstructTransforms(tables, tags, origLengths, transforms, faceTables);
        return MemorySegment.ofArray(BitmapSfntFont.wrap(faceFlavor, tables));
    }

    /// Reconstructs transformed `glyf`, `loca`, and `hmtx` payloads in `tables`.
    private static void reconstructTransforms(
            LinkedHashMap<String, byte[]> tables,
            String[] tags,
            int[] origLengths,
            int[] transforms,
            int @Nullable [] faceTables
    ) {
        boolean glyfTransformed = false;
        boolean hmtxTransformed = false;
        int locaOrig = -1;
        int[] members = faceTables != null ? faceTables : identity(tags.length);
        for (int member : members) {
            if (tags[member].equals("glyf") && isTransformed("glyf", transforms[member])) {
                glyfTransformed = true;
            }
            if (tags[member].equals("loca")) {
                locaOrig = origLengths[member];
            }
            if (tags[member].equals("hmtx") && isTransformed("hmtx", transforms[member])) {
                hmtxTransformed = true;
            }
        }
        if (glyfTransformed) {
            if (locaOrig < 0) {
                throw new IllegalArgumentException("WOFF2 transformed glyf requires a loca entry");
            }
            byte[] transformedGlyf = tables.get("glyf");
            if (transformedGlyf == null) {
                throw new IllegalArgumentException("WOFF2 transformed glyf payload is missing");
            }
            Woff2Glyf.Tables rebuilt = Woff2Glyf.reconstruct(transformedGlyf, locaOrig);
            tables.put("glyf", rebuilt.glyf());
            tables.put("loca", rebuilt.loca());
        }
        if (hmtxTransformed) {
            byte[] hhea = tables.get("hhea");
            byte[] maxp = tables.get("maxp");
            byte[] glyf = tables.get("glyf");
            byte[] loca = tables.get("loca");
            byte[] hmtx = tables.get("hmtx");
            if (hhea == null || maxp == null || glyf == null || loca == null || hmtx == null) {
                throw new IllegalArgumentException("WOFF2 transformed hmtx requires hhea, maxp, glyf, and loca");
            }
            tables.put("hmtx", Woff2Glyf.reconstructHmtx(
                    hmtx,
                    glyf,
                    loca,
                    Short.toUnsignedInt(ByteBuffer.wrap(maxp).order(ByteOrder.BIG_ENDIAN).getShort(4)),
                    Short.toUnsignedInt(ByteBuffer.wrap(hhea).order(ByteOrder.BIG_ENDIAN).getShort(34))
            ));
        }
    }

    /// Returns `0 .. length-1`.
    private static int[] identity(int length) {
        int[] values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = index;
        }
        return values;
    }

    /// Wraps a raw SFNT/OTTO image, or the first face of a TTC, as a WOFF2 file.
    ///
    /// A TTC input is stored with flavor `ttcf` and a one-font collection directory.
    ///
    /// @param sfnt the SFNT or TTC bytes
    /// @return the WOFF2 bytes
    public static byte[] wrap(byte[] sfnt) {
        return wrap(sfnt, false);
    }

    /// Wraps a raw SFNT/OTTO image as a WOFF2 file.
    ///
    /// @param sfnt the SFNT or TTC bytes
    /// @param commands whether to emit [`Brotli#compressCommands(byte[])`] instead of the
    ///                 trivial uncompressed stream
    /// @return the WOFF2 bytes
    public static byte[] wrap(byte[] sfnt, boolean commands) {
        return wrap(sfnt, commands, false);
    }

    /// Wraps a raw SFNT/OTTO image as WOFF2 with the version-0 `glyf`/`loca` transform.
    ///
    /// When `hmtx` left side bearings match glyph `xMin`, the version-1 `hmtx` transform is also
    /// applied. `DSIG` is dropped. `head` flag bit 11 is set.
    ///
    /// @param sfnt the SFNT or TTC bytes
    /// @return the WOFF2 bytes
    public static byte[] wrapTransformed(byte[] sfnt) {
        return wrap(sfnt, false, true);
    }

    /// Wraps a raw SFNT/OTTO image as WOFF2 whose Brotli stream uses a static-dictionary distance.
    ///
    /// The concatenated table payload must contain an RFC 7932 identity dictionary word.
    ///
    /// @param sfnt the SFNT or TTC bytes
    /// @return the WOFF2 bytes
    public static byte[] wrapWithStaticDictionary(byte[] sfnt) {
        return wrap(sfnt, false, false, true);
    }

    /// Wraps a raw SFNT/OTTO image as a WOFF2 file.
    ///
    /// @param sfnt the SFNT or TTC bytes
    /// @param commands whether to emit [`Brotli#compressCommands(byte[])`]
    /// @param transformGlyf whether to apply the WOFF2 `glyf`/`loca` (and eligible `hmtx`) transforms
    /// @return the WOFF2 bytes
    public static byte[] wrap(byte[] sfnt, boolean commands, boolean transformGlyf) {
        return wrap(sfnt, commands, transformGlyf, false);
    }

    /// Wraps a raw SFNT/OTTO image as a WOFF2 file.
    ///
    /// @param sfnt the SFNT or TTC bytes
    /// @param commands whether to emit [`Brotli#compressCommands(byte[])`]
    /// @param transformGlyf whether to apply the WOFF2 `glyf`/`loca` (and eligible `hmtx`) transforms
    /// @param dictionary whether to emit [`Brotli#compressWithStaticDictionary(byte[])`]
    /// @return the WOFF2 bytes
    private static byte[] wrap(byte[] sfnt, boolean commands, boolean transformGlyf, boolean dictionary) {
        boolean collection = TtcFile.isTtc(MemorySegment.ofArray(sfnt));
        byte[] face = collection
                ? TtcFile.firstFont(MemorySegment.ofArray(sfnt)).toArray(java.lang.foreign.ValueLayout.JAVA_BYTE)
                : sfnt;
        ByteBuffer source = ByteBuffer.wrap(face).order(ByteOrder.BIG_ENDIAN);
        if (source.remaining() < 12) {
            throw new IllegalArgumentException("SFNT header is truncated");
        }
        int faceFlavor = source.getInt();
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
        LinkedHashMap<String, byte[]> raw = new LinkedHashMap<>();
        for (int index = 0; index < tableCount; index++) {
            if (tags[index].equals("DSIG") && transformGlyf) {
                continue;
            }
            byte[] payload = new byte[lengths[index]];
            System.arraycopy(face, offsets[index], payload, 0, payload.length);
            raw.put(tags[index], payload);
        }
        if (transformGlyf) {
            byte[] head = raw.get("head");
            if (head != null && head.length >= 18) {
                int flags = Short.toUnsignedInt(ByteBuffer.wrap(head).order(ByteOrder.BIG_ENDIAN).getShort(16));
                flags |= 1 << 11;
                ByteBuffer.wrap(head).order(ByteOrder.BIG_ENDIAN).putShort(16, (short) flags);
            }
        }
        String[] emitTags = raw.keySet().toArray(String[]::new);
        byte[][] emitPayloads = new byte[emitTags.length][];
        int[] emitOrig = new int[emitTags.length];
        int[] emitTransform = new int[emitTags.length];
        int[] emitStream = new int[emitTags.length];
        boolean haveGlyf = raw.containsKey("glyf") && raw.containsKey("loca") && raw.containsKey("maxp");
        byte[] transformedGlyf = null;
        byte[] transformedHmtx = null;
        if (transformGlyf && haveGlyf) {
            byte[] maxp = raw.get("maxp");
            byte[] head = raw.get("head");
            int numGlyphs = Short.toUnsignedInt(ByteBuffer.wrap(maxp).order(ByteOrder.BIG_ENDIAN).getShort(4));
            int indexFormat = head != null && head.length >= 52
                    ? Short.toUnsignedInt(ByteBuffer.wrap(head).order(ByteOrder.BIG_ENDIAN).getShort(50))
                    : 1;
            transformedGlyf = Woff2Glyf.transform(raw.get("glyf"), raw.get("loca"), numGlyphs, indexFormat);
            byte[] hmtx = raw.get("hmtx");
            byte[] hhea = raw.get("hhea");
            if (hmtx != null && hhea != null && hhea.length >= 36) {
                transformedHmtx = Woff2Glyf.transformHmtx(
                        hmtx,
                        raw.get("glyf"),
                        raw.get("loca"),
                        numGlyphs,
                        Short.toUnsignedInt(ByteBuffer.wrap(hhea).order(ByteOrder.BIG_ENDIAN).getShort(34))
                );
            }
        }
        ByteArrayOutputStream concatenated = new ByteArrayOutputStream();
        for (int index = 0; index < emitTags.length; index++) {
            String tag = emitTags[index];
            byte[] payload = raw.get(tag);
            emitOrig[index] = payload.length;
            if (transformGlyf && tag.equals("glyf") && transformedGlyf != null) {
                emitPayloads[index] = transformedGlyf;
                emitTransform[index] = 0;
                emitStream[index] = transformedGlyf.length;
            } else if (transformGlyf && tag.equals("loca") && transformedGlyf != null) {
                emitPayloads[index] = new byte[0];
                emitTransform[index] = 0;
                emitStream[index] = 0;
            } else if (transformGlyf && tag.equals("hmtx") && transformedHmtx != null) {
                emitPayloads[index] = transformedHmtx;
                emitTransform[index] = 1;
                emitStream[index] = transformedHmtx.length;
            } else {
                emitPayloads[index] = payload;
                emitTransform[index] = tag.equals("glyf") || tag.equals("loca") ? NULL_GLYF_LOCA : 0;
                emitStream[index] = payload.length;
            }
            concatenated.writeBytes(emitPayloads[index]);
        }
        byte[] payload = concatenated.toByteArray();
        byte[] compressed = dictionary
                ? Brotli.compressWithStaticDictionary(payload)
                : commands ? Brotli.compressCommands(payload) : Brotli.compress(payload);
        ByteArrayOutputStream directory = new ByteArrayOutputStream();
        for (int index = 0; index < emitTags.length; index++) {
            writeDirectoryEntry(
                    directory,
                    emitTags[index],
                    emitOrig[index],
                    emitTransform[index],
                    isTransformed(emitTags[index], emitTransform[index]) ? emitStream[index] : -1
            );
        }
        byte[] collectionDirectory = collection ? collectionDirectory(faceFlavor, emitTags.length) : new byte[0];
        byte[] dir = directory.toByteArray();
        int fileSize = 48 + dir.length + collectionDirectory.length + compressed.length;
        int headerFlavor = collection ? TtcFile.SIGNATURE : faceFlavor;
        ByteBuffer output = ByteBuffer.allocate(fileSize).order(ByteOrder.BIG_ENDIAN);
        output.putInt(SIGNATURE);
        output.putInt(headerFlavor);
        output.putInt(fileSize);
        output.putShort((short) emitTags.length);
        output.putShort((short) 0);
        output.putInt(sfnt.length);
        output.putInt(compressed.length);
        output.putShort((short) 1);
        output.putShort((short) 0);
        output.putInt(0);
        output.putInt(0);
        output.putInt(0);
        output.putInt(0);
        output.putInt(0);
        output.put(dir);
        output.put(collectionDirectory);
        output.put(compressed);
        return output.array();
    }

    /// Encodes a one-font collection directory for the first face.
    private static byte[] collectionDirectory(int faceFlavor, int tableCount) {
        ByteArrayOutputStream directory = new ByteArrayOutputStream();
        writeUInt32(directory, 0x00010000);
        write255UInt16(directory, 1);
        write255UInt16(directory, tableCount);
        writeUInt32(directory, faceFlavor);
        for (int index = 0; index < tableCount; index++) {
            write255UInt16(directory, index);
        }
        return directory.toByteArray();
    }

    /// Writes a big-endian 32-bit integer.
    private static void writeUInt32(ByteArrayOutputStream output, int value) {
        output.write((value >>> 24) & 0xFF);
        output.write((value >>> 16) & 0xFF);
        output.write((value >>> 8) & 0xFF);
        output.write(value & 0xFF);
    }

    /// Writes one table-directory entry.
    ///
    /// @param transformLength `-1` when the table is not transformed
    private static void writeDirectoryEntry(
            ByteArrayOutputStream directory,
            String tag,
            int origLength,
            int transform,
            int transformLength
    ) {
        int known = indexOfKnown(tag);
        if (known >= 0) {
            directory.write(known | (transform << 6));
        } else {
            directory.write(CUSTOM_TAG | (transform << 6));
            directory.writeBytes(tag.getBytes(StandardCharsets.US_ASCII));
        }
        writeBase128(directory, origLength);
        if (transformLength >= 0) {
            writeBase128(directory, transformLength);
        }
    }

    /// Returns whether `tag` uses a non-null transform at `version`.
    private static boolean isTransformed(String tag, int version) {
        if (tag.equals("glyf") || tag.equals("loca")) {
            return version != NULL_GLYF_LOCA;
        }
        if (tag.equals("hmtx")) {
            return version == 1;
        }
        return version != 0;
    }

    /// Returns the known-tag index, or `-1` when `tag` is custom.
    private static int indexOfKnown(String tag) {
        for (int index = 0; index < KNOWN_TAGS.length; index++) {
            if (KNOWN_TAGS[index].equals(tag)) {
                return index;
            }
        }
        return -1;
    }

    /// Reads a UIntBase128 value.
    private static int readBase128(ByteBuffer buffer) {
        int result = 0;
        for (int index = 0; index < 5; index++) {
            if (!buffer.hasRemaining()) {
                throw new IllegalArgumentException("WOFF2 UIntBase128 is truncated");
            }
            int value = Byte.toUnsignedInt(buffer.get());
            if (index == 0 && value == 0x80) {
                throw new IllegalArgumentException("WOFF2 UIntBase128 has a leading zero");
            }
            result = (result << 7) | (value & 0x7F);
            if ((value & 0x80) == 0) {
                return result;
            }
        }
        throw new IllegalArgumentException("WOFF2 UIntBase128 is longer than five bytes");
    }

    /// Reads a 255UInt16 value.
    private static int read255UInt16(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("WOFF2 255UInt16 is truncated");
        }
        int code = Byte.toUnsignedInt(buffer.get());
        if (code == 253) {
            if (buffer.remaining() < 2) {
                throw new IllegalArgumentException("WOFF2 255UInt16 word is truncated");
            }
            return Short.toUnsignedInt(buffer.getShort());
        }
        if (code == 254) {
            if (buffer.remaining() < 3) {
                throw new IllegalArgumentException("WOFF2 255UInt16 triple is truncated");
            }
            int high = Byte.toUnsignedInt(buffer.get());
            return (high << 16) | Short.toUnsignedInt(buffer.getShort());
        }
        if (code == 255) {
            if (buffer.remaining() < 4) {
                throw new IllegalArgumentException("WOFF2 255UInt16 quad is truncated");
            }
            return buffer.getInt();
        }
        return code;
    }

    /// Writes a 255UInt16 value.
    private static void write255UInt16(ByteArrayOutputStream output, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("WOFF2 255UInt16 cannot encode a negative value");
        }
        if (value < 253) {
            output.write(value);
            return;
        }
        if (value <= 0xFFFF) {
            output.write(253);
            output.write((value >>> 8) & 0xFF);
            output.write(value & 0xFF);
            return;
        }
        if (value <= 0xFFFFFF) {
            output.write(254);
            output.write((value >>> 16) & 0xFF);
            output.write((value >>> 8) & 0xFF);
            output.write(value & 0xFF);
            return;
        }
        output.write(255);
        writeUInt32(output, value);
    }

    /// Writes a UIntBase128 value.
    private static void writeBase128(ByteArrayOutputStream output, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("WOFF2 UIntBase128 cannot encode a negative length");
        }
        int bytes = 1;
        int probe = value;
        while (probe > 0x7F) {
            probe >>>= 7;
            bytes++;
        }
        for (int index = bytes - 1; index >= 0; index--) {
            int chunk = (value >>> (7 * index)) & 0x7F;
            if (index > 0) {
                chunk |= 0x80;
            }
            output.write(chunk);
        }
    }
}
