package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Parses a bounded ICC `textDescriptionType` (`desc`), `textType` (`text`), or
/// `multiLocalizedUnicodeType` (`mluc`) payload.
@NotNullByDefault
public final class IccProfileText {
    /// Type `'desc'`.
    public static final int TYPE_DESC = 0x6465_7363;

    /// Type `'text'`.
    public static final int TYPE_TEXT = 0x7465_7874;

    /// Type `'mluc'`.
    public static final int TYPE_MLUC = 0x6D6C_7563;

    /// Maximum accepted ASCII character count, including the terminating NUL.
    public static final int MAX_ASCII = 1024;

    /// Maximum accepted UTF-16 code units in one `mluc` record.
    public static final int MAX_MLUC_CHARS = 256;

    /// Prevents instantiation.
    private IccProfileText() {
    }

    /// Reads the 7-bit ASCII or first `mluc` description from a text tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the ASCII text without a trailing NUL
    public static String parse(byte[] bytes, int offset, int size) {
        return parseEmbedded(bytes, offset, size).text();
    }

    /// Reads an embedded `desc`, `text`, or `mluc` element and reports the bytes consumed.
    ///
    /// For `desc`, the consumed length includes the Unicode and Macintosh ScriptCode
    /// tails after the ASCII count. For `text`, it includes the terminating NUL. For
    /// `mluc`, the first record's UTF-16BE string is returned and the consumed length
    /// is the enclosing element size.
    ///
    /// @param bytes the profile bytes
    /// @param offset the element start
    /// @param size the remaining bytes in the enclosing tag
    /// @return the ASCII text and the number of bytes consumed
    public static Embedded parseEmbedded(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < 12 || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC text tag is outside the profile");
        }
        int type = u32(bytes, offset);
        if (type == TYPE_TEXT) {
            String text = asciiCString(bytes, offset + 8, size - 8);
            return new Embedded(text, 8 + text.length() + 1);
        }
        if (type == TYPE_MLUC) {
            return parseMluc(bytes, offset, size);
        }
        if (type != TYPE_DESC) {
            throw new IllegalArgumentException("ICC text tag is not desc, text, or mluc");
        }
        int count = u32(bytes, offset + 8);
        if (count < 1 || count > MAX_ASCII) {
            throw new IllegalArgumentException("ICC desc ASCII count is outside the accepted bounds");
        }
        if (size < 12 + count) {
            throw new IllegalArgumentException("ICC desc ASCII exceeds the tag");
        }
        String text = asciiCounted(bytes, offset + 12, count);
        int afterAscii = 12 + count;
        if (size < afterAscii + 8) {
            return new Embedded(text, afterAscii);
        }
        int unicodeCount = u32(bytes, offset + afterAscii + 4);
        if (unicodeCount < 0 || unicodeCount > MAX_ASCII) {
            throw new IllegalArgumentException("ICC desc Unicode count is outside the accepted bounds");
        }
        int consumed = afterAscii + 8 + unicodeCount * 2 + 70;
        if (consumed > size) {
            throw new IllegalArgumentException("ICC desc Unicode or ScriptCode tail exceeds the tag");
        }
        return new Embedded(text, consumed);
    }

    /// One embedded `desc` or `text` element.
    ///
    /// @param text the ASCII payload without a trailing NUL
    /// @param size the number of bytes consumed from the enclosing tag
    public record Embedded(String text, int size) {
        /// Validates the element.
        public Embedded {
            Objects.requireNonNull(text, "text");
            if (size < 1) {
                throw new IllegalArgumentException("ICC text element size must be positive");
            }
        }
    }

    /// Reads the first `mluc` record as UTF-16BE without unpaired surrogates.
    ///
    /// @param bytes the profile bytes
    /// @param offset the element start
    /// @param size the remaining bytes in the enclosing tag
    /// @return the first localized string and the element size
    private static Embedded parseMluc(byte[] bytes, int offset, int size) {
        if (size < 28) {
            throw new IllegalArgumentException("ICC mluc tag is truncated");
        }
        int records = u32(bytes, offset + 8);
        int recordSize = u32(bytes, offset + 12);
        if (records < 1 || recordSize != 12 || size < 16 + records * 12) {
            throw new IllegalArgumentException("ICC mluc directory is malformed");
        }
        int stringBytes = u32(bytes, offset + 20);
        int stringRelative = u32(bytes, offset + 24);
        if (stringRelative < 0 || stringBytes < 0 || (stringBytes & 1) != 0
                || stringRelative > size - stringBytes) {
            throw new IllegalArgumentException("ICC mluc string is outside the tag");
        }
        int units = stringBytes / 2;
        if (units > MAX_MLUC_CHARS) {
            throw new IllegalArgumentException("ICC mluc string exceeds the accepted bound");
        }
        int start = offset + stringRelative;
        StringBuilder text = new StringBuilder(units);
        for (int index = 0; index < units; index++) {
            int unit = u16(bytes, start + index * 2);
            if (unit >= 0xD800 && unit <= 0xDFFF) {
                throw new IllegalArgumentException("ICC mluc strings must not contain unpaired surrogates");
            }
            text.append((char) unit);
        }
        return new Embedded(text.toString(), size);
    }

    /// Reads `count` bytes as 7-bit ASCII and drops a trailing NUL.
    private static String asciiCounted(byte[] bytes, int offset, int count) {
        int length = count;
        if (bytes[offset + count - 1] == 0) {
            length = count - 1;
        }
        for (int index = 0; index < length; index++) {
            int value = bytes[offset + index] & 0xFF;
            if (value == 0 || value > 0x7F) {
                throw new IllegalArgumentException("ICC desc ASCII must be 7-bit and contain no interior NUL");
            }
        }
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

    /// Reads a NUL-terminated 7-bit ASCII field of at most `limit` bytes.
    private static String asciiCString(byte[] bytes, int offset, int limit) {
        int length = 0;
        while (length < limit) {
            int value = bytes[offset + length] & 0xFF;
            if (value == 0) {
                return new String(bytes, offset, length, StandardCharsets.US_ASCII);
            }
            if (value > 0x7F) {
                throw new IllegalArgumentException("ICC text must be 7-bit ASCII");
            }
            length++;
        }
        throw new IllegalArgumentException("ICC text is not NUL-terminated");
    }

    /// Reads a big-endian unsigned 16-bit integer.
    private static int u16(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    /// Reads a big-endian unsigned 32-bit integer as a signed Java `int`.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }
}
