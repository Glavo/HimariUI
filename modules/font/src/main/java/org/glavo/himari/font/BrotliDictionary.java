package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/// RFC 7932 static dictionary used by [`Brotli#decompress(byte[])`].
///
/// Words of length 4 through 24 live in the Appendix A `DICT` image. A copy whose distance is
/// greater than the current backward window is a dictionary reference: the copy length selects the
/// word bucket, and the excess distance selects the word index plus one of the 121 Appendix B
/// transforms.
@NotNullByDefault
final class BrotliDictionary {
    /// RFC 7932 Appendix A `DICT` length.
    static final int DICT_SIZE = 122784;

    /// Number of RFC 7932 Appendix B transforms.
    static final int TRANSFORM_COUNT = 121;

    /// Shortest dictionary word.
    private static final int MIN_WORD = 4;

    /// Longest dictionary word.
    private static final int MAX_WORD = 24;

    /// Identity transform type.
    private static final int IDENTITY = 0;

    /// Uppercase-first transform type.
    private static final int UPPERCASE_FIRST = 10;

    /// Uppercase-all transform type.
    private static final int UPPERCASE_ALL = 11;

    /// `NDBITS[length]` from RFC 7932 Appendix A.
    private static final int[] SIZE_BITS = {
            0, 0, 0, 0, 10, 10, 11, 11, 10, 10,
            10, 10, 10, 9, 9, 8, 7, 7, 8, 7,
            7, 6, 6, 5, 5
    };

    /// Byte offset of the first word of each length in [`#DICT`].
    private static final int[] OFFSETS = {
            0, 0, 0, 0,
            0, 4096, 9216, 21504, 35840, 44032,
            53248, 63488, 74752, 87040, 93696,
            100864, 104704, 106752, 108928, 113536,
            115968, 118528, 119872, 121280, 122016
    };

    /// RFC 7932 Appendix B prefix and suffix strings, `#`-delimited, with UTF-8 octets stored as
    /// Latin-1 code units.
    private static final String PREFIX_SUFFIX_SRC = "# #s #, #e #.# the #.com/#\u00C2\u00A0# of # and"
            + " # in # to #\"#\">#\n#]# for # a # that #. # with #'# from # by #. The # on # as # is #ing"
            + " #\n\t#:#ed #(# at #ly #=\"# of the #. This #,# not #er #al #='#ful #ive #less #est #ize #"
            + "ous #";

    /// RFC 7932 Appendix B transform triplets packed as `char - 32`.
    private static final String TRANSFORMS_SRC = "     !! ! ,  *!  &!  \" !  ) *   * -  ! # !  #!*!  "
            + "+  ,$ !  -  %  .  / #   0  1 .  \"   2  3!*   4%  ! # /   5  6  7  8 0  1 &   $   9 +   : "
            + " ;  < '  !=  >  ?! 4  @ 4  2  &   A *# (   B  C& ) %  ) !*# *-% A +! *.  D! %'  & E *6  F "
            + " G% ! *A *%  H! D  I!+!  J!+   K +- *4! A  L!*4  M  N +6  O!*% +.! K *G  P +%(  ! G *D +D "
            + " Q +# *K!*G!+D!+# +G +A +4!+% +K!+4!*D!+K!*K";

    /// Concatenated prefix and suffix octets.
    private static final byte[] PREFIX_SUFFIX = new byte[167];

    /// Start index of each prefix or suffix in [`#PREFIX_SUFFIX`].
    private static final int[] PREFIX_SUFFIX_HEADS = new int[51];

    /// Triplets `(prefix, type, suffix)` for each of the 121 transforms.
    private static final int[] TRIPLETS = new int[TRANSFORM_COUNT * 3];

    /// RFC 7932 Appendix A `DICT` image.
    private static final byte[] DICT = loadDictionary();

    static {
        unpackTransforms();
    }

    /// Prevents instantiation.
    private BrotliDictionary() {
    }

    /// Applies the dictionary word and transform selected by `copyLength` and `address`.
    ///
    /// @param copyLength the insert-and-copy copy length, which is the base word length
    /// @param address `distance - maxDistance - 1`
    /// @return the transformed word, which may be shorter or longer than `copyLength`
    static byte[] transform(int copyLength, int address) {
        if (copyLength < MIN_WORD || copyLength > MAX_WORD) {
            throw new IllegalArgumentException("Brotli dictionary copy length must be in 4..24");
        }
        int nbits = SIZE_BITS[copyLength];
        int wordId = address & ((1 << nbits) - 1);
        int transformId = address >>> nbits;
        return apply(copyLength, wordId, transformId);
    }

    /// Applies transform `transformId` to dictionary word `wordId` of length `copyLength`.
    ///
    /// @param copyLength the base word length
    /// @param wordId the word index in that length bucket
    /// @param transformId the Appendix B transform index
    /// @return the transformed word
    static byte[] apply(int copyLength, int wordId, int transformId) {
        if (copyLength < MIN_WORD || copyLength > MAX_WORD) {
            throw new IllegalArgumentException("Brotli dictionary copy length must be in 4..24");
        }
        int nbits = SIZE_BITS[copyLength];
        if (wordId < 0 || wordId >= (1 << nbits)) {
            throw new IllegalArgumentException("Brotli dictionary word is out of range");
        }
        if (transformId < 0 || transformId >= TRANSFORM_COUNT) {
            throw new IllegalArgumentException("Brotli dictionary transform is out of range");
        }
        int src = OFFSETS[copyLength] + wordId * copyLength;
        int prefixIdx = TRIPLETS[transformId * 3];
        int type = TRIPLETS[transformId * 3 + 1];
        int suffixIdx = TRIPLETS[transformId * 3 + 2];
        int omitFirst = type >= 12 && type <= 20 ? type - 11 : 0;
        int omitLast = type >= 1 && type <= 9 ? type : 0;
        if (omitFirst > copyLength) {
            omitFirst = copyLength;
        }
        int keep = copyLength - omitFirst - omitLast;
        if (keep < 0) {
            keep = 0;
        }
        int prefixStart = PREFIX_SUFFIX_HEADS[prefixIdx];
        int prefixEnd = PREFIX_SUFFIX_HEADS[prefixIdx + 1];
        int suffixStart = PREFIX_SUFFIX_HEADS[suffixIdx];
        int suffixEnd = PREFIX_SUFFIX_HEADS[suffixIdx + 1];
        byte[] out = new byte[(prefixEnd - prefixStart) + keep + (suffixEnd - suffixStart)];
        int dest = 0;
        for (int index = prefixStart; index < prefixEnd; index++) {
            out[dest++] = PREFIX_SUFFIX[index];
        }
        int wordStart = dest;
        System.arraycopy(DICT, src + omitFirst, out, dest, keep);
        dest += keep;
        if (type == UPPERCASE_FIRST || type == UPPERCASE_ALL) {
            uppercase(out, wordStart, keep, type == UPPERCASE_ALL);
        }
        for (int index = suffixStart; index < suffixEnd; index++) {
            out[dest++] = PREFIX_SUFFIX[index];
        }
        return out;
    }

    /// Finds the longest identity dictionary word contained in `input`.
    ///
    /// @param input the uncompressed bytes
    /// @return `{offset, length, wordId}` or `null` when no word matches
    static int @Nullable [] findIdentity(byte[] input) {
        for (int length = MAX_WORD; length >= MIN_WORD; length--) {
            int nbits = SIZE_BITS[length];
            int nwords = 1 << nbits;
            int base = OFFSETS[length];
            for (int offset = 0; offset + length <= input.length; offset++) {
                for (int wordId = 0; wordId < nwords; wordId++) {
                    if (matches(input, offset, base + wordId * length, length)) {
                        return new int[] {offset, length, wordId};
                    }
                }
            }
        }
        return null;
    }

    /// Returns whether `input[offset..)` equals `DICT[dictOffset..)` for `length` bytes.
    private static boolean matches(byte[] input, int offset, int dictOffset, int length) {
        for (int index = 0; index < length; index++) {
            if (input[offset + index] != DICT[dictOffset + index]) {
                return false;
            }
        }
        return true;
    }

    /// Uppercases `length` bytes at `offset` as specified for the uppercase transforms.
    private static void uppercase(byte[] word, int offset, int length, boolean all) {
        int remaining = all ? length : Math.min(1, length);
        int index = offset;
        while (remaining > 0) {
            int lead = word[index] & 0xFF;
            if (lead < 0xC0) {
                if (lead >= 'a' && lead <= 'z') {
                    word[index] ^= 32;
                }
                index += 1;
                remaining -= 1;
            } else if (lead < 0xE0) {
                if (remaining >= 2 && index + 1 < word.length) {
                    word[index + 1] ^= 32;
                }
                index += 2;
                remaining -= 2;
            } else {
                if (remaining >= 3 && index + 2 < word.length) {
                    word[index + 2] ^= 5;
                }
                index += 3;
                remaining -= 3;
            }
            if (!all) {
                break;
            }
        }
    }

    /// Unpacks Appendix B prefixes, suffixes, and transform triplets.
    private static void unpackTransforms() {
        int head = 1;
        int write = 0;
        for (int index = 0; index < PREFIX_SUFFIX_SRC.length(); index++) {
            int value = PREFIX_SUFFIX_SRC.charAt(index);
            if (value == '#') {
                PREFIX_SUFFIX_HEADS[head++] = write;
            } else {
                PREFIX_SUFFIX[write++] = (byte) value;
            }
        }
        if (TRANSFORMS_SRC.length() != TRANSFORM_COUNT * 3) {
            throw new IllegalStateException("RFC 7932 transform table is the wrong length");
        }
        for (int index = 0; index < TRIPLETS.length; index++) {
            TRIPLETS[index] = TRANSFORMS_SRC.charAt(index) - 32;
        }
    }

    /// Loads the Appendix A `DICT` image from the module resource.
    private static byte[] loadDictionary() {
        try (InputStream in = BrotliDictionary.class.getResourceAsStream("brotli-dictionary.bin")) {
            if (in == null) {
                throw new IllegalStateException("RFC 7932 Brotli dictionary is missing");
            }
            byte[] data = in.readAllBytes();
            if (data.length != DICT_SIZE) {
                throw new IllegalStateException("RFC 7932 Brotli dictionary must be " + DICT_SIZE + " bytes");
            }
            return data;
        } catch (IOException ex) {
            throw new IllegalStateException("RFC 7932 Brotli dictionary failed to load", ex);
        }
    }
}
