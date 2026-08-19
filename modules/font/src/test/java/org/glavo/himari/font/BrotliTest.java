package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Drives the shipped [`Brotli`] compressor and decompressor.
@NotNullByDefault
final class BrotliTest {
    /// Empty input uses the RFC 11.1 empty stream.
    @Test
    void emptyRoundTrips() {
        byte[] empty = new byte[0];
        byte[] compressed = Brotli.compress(empty);
        assertEquals(1, compressed.length);
        assertEquals(6, compressed[0]);
        assertArrayEquals(empty, Brotli.decompress(compressed));
    }

    /// Trivial uncompressed meta-blocks inflate through [`Brotli#decompress(byte[])`].
    @Test
    void trivialStreamRoundTrips() {
        byte[] original = "HimariUI WOFF2 Brotli leftover".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(original, Brotli.decompress(Brotli.compress(original)));
    }

    /// Compressed commands with an LZ77 copy inflate through the same decoder.
    @Test
    void commandStreamCopiesRepeatedBytes() {
        byte[] original = "abababab".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(original, Brotli.decompress(Brotli.compressCommands(original)));
    }

    /// A larger alphabet uses a complex literal prefix inside a compressed meta-block.
    @Test
    void commandStreamUsesComplexLiteralPrefix() {
        byte[] original = "WOFF2 Brotli leftover 13.4".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(original, Brotli.decompress(Brotli.compressCommands(original)));
    }

    /// The first Appendix A word inflates through a static-dictionary distance.
    @Test
    void staticDictionaryIdentityWordInflates() {
        byte[] expected = "time".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, Brotli.decompress(Brotli.compressStaticDictionary(4, 0, 0)));
    }

    /// Appendix B uppercase-first produces `Time` from dictionary word 0.
    @Test
    void staticDictionaryUppercaseFirstInflates() {
        byte[] expected = "Time".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, Brotli.decompress(Brotli.compressStaticDictionary(4, 0, 9)));
    }

    /// Literals around an identity dictionary word inflate through the same decoder.
    @Test
    void staticDictionaryWordInsideLiteralsInflates() {
        byte[] original = ">>time<<".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(original, Brotli.decompress(Brotli.compressWithStaticDictionary(original)));
    }
}
