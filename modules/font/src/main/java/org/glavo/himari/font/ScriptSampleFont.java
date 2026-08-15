package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates a checked SFNT that maps ASCII, Hebrew, Arabic, Thai, Lao, Hangul, and Presentation Forms.
///
/// Presentation-form glyphs are distinct from their nominal letters so joining and Hebrew
/// composition can be observed through `cmap` identities.
@NotNullByDefault
public final class ScriptSampleFont {
    /// Units per em.
    public static final int UNITS_PER_EM = 8;

    /// First ASCII code point.
    private static final int ASCII_FIRST = 32;

    /// Last ASCII code point.
    private static final int ASCII_LAST = 126;

    /// Glyph id of `U+0020`.
    private static final int GLYPH_SPACE = 1;

    /// First Hebrew-mark glyph.
    private static final int GLYPH_HEBREW_MARK = 96;

    /// First Hebrew-letter glyph (`U+05D0`).
    private static final int GLYPH_HEBREW_LETTER = 99;

    /// First Arabic-letter glyph (`U+0621`).
    private static final int GLYPH_ARABIC_LETTER = 126;

    /// First Arabic-mark glyph (`U+064B`).
    private static final int GLYPH_ARABIC_MARK = 168;

    /// Glyph of `U+FB2A`.
    private static final int GLYPH_FB2A = 176;

    /// Glyph of `U+FB30`.
    private static final int GLYPH_FB30 = 178;

    /// Glyph of `U+FE80`.
    private static final int GLYPH_FE80 = 180;

    /// Last Presentation Forms-B code point stored in this font.
    private static final int FE_LAST = 0xFEF4;

    /// Glyph of choseong `U+1100`.
    private static final int GLYPH_HANGUL_LEAD = GLYPH_FE80 + (FE_LAST - 0xFE80) + 1;

    /// Glyph of jungseong `U+1161`.
    private static final int GLYPH_HANGUL_VOWEL = GLYPH_HANGUL_LEAD + 1;

    /// Glyph of jongseong `U+11A8`.
    private static final int GLYPH_HANGUL_TRAIL = GLYPH_HANGUL_VOWEL + 1;

    /// Glyph of syllable `U+AC00`.
    private static final int GLYPH_HANGUL_GA = GLYPH_HANGUL_TRAIL + 1;

    /// Glyph of syllable `U+AC01`.
    private static final int GLYPH_HANGUL_GAG = GLYPH_HANGUL_GA + 1;

    /// First Thai glyph (`U+0E01`).
    private static final int GLYPH_THAI = GLYPH_HANGUL_GAG + 1;

    /// Last Thai code point stored in this font.
    private static final int THAI_LAST = 0x0E4E;

    /// First Lao glyph (`U+0E81`).
    private static final int GLYPH_LAO = GLYPH_THAI + (THAI_LAST - 0x0E01) + 1;

    /// Last Lao code point stored in this font.
    private static final int LAO_LAST = 0x0ECD;

    /// Prevents instantiation.
    private ScriptSampleFont() {
    }

    /// Builds the script sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the script sample font image.
    ///
    /// @return a read-only SFNT file
    public static MemorySegment bytes() {
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        tables.put("cmap", cmap());
        byte[] glyf = glyf();
        tables.put("glyf", glyf);
        tables.put("head", head());
        tables.put("hhea", hhea());
        tables.put("hmtx", hmtx());
        tables.put("loca", loca(glyf.length));
        tables.put("maxp", maxp());
        tables.put("name", name());
        tables.put("post", post());
        return MemorySegment.ofArray(BitmapSfntFont.wrap(tables)).asReadOnly();
    }

    /// Glyph count including `.notdef`.
    ///
    /// @return the count
    static int glyphCount() {
        return GLYPH_LAO + (LAO_LAST - 0x0E81) + 1;
    }

    /// Returns whether `glyphId` is an empty outline.
    private static boolean emptyGlyph(int glyphId) {
        if (glyphId <= GLYPH_SPACE
                || (glyphId >= GLYPH_HEBREW_MARK && glyphId < GLYPH_HEBREW_LETTER)
                || (glyphId >= GLYPH_ARABIC_MARK && glyphId < GLYPH_FB2A)) {
            return true;
        }
        if (glyphId >= GLYPH_THAI && glyphId < GLYPH_LAO) {
            return thaiLaoMark(0x0E01 + (glyphId - GLYPH_THAI));
        }
        return glyphId >= GLYPH_LAO && glyphId < glyphCount()
                && thaiLaoMark(0x0E81 + (glyphId - GLYPH_LAO));
    }

    /// Returns whether a Thai or Lao code point is a zero-advance mark.
    private static boolean thaiLaoMark(int codePoint) {
        int thai = codePoint & ~0x80;
        return thai == 0x0E31
                || (thai >= 0x0E34 && thai <= 0x0E3A)
                || (thai >= 0x0E47 && thai <= 0x0E4E);
    }

    /// Writes the format-4 cmap.
    private static byte[] cmap() {
        int[] starts = {
                ASCII_FIRST, 0x05BC, 0x05C1, 0x05D0, 0x0621, 0x064B,
                0x0E01, 0x0E81, 0x1100, 0x1161, 0x11A8, 0xAC00, 0xFB2A, 0xFB30, 0xFE80, 0xFFFF
        };
        int[] ends = {
                ASCII_LAST, 0x05BC, 0x05C2, 0x05EA, 0x064A, 0x0652,
                THAI_LAST, LAO_LAST, 0x1100, 0x1161, 0x11A8, 0xAC01, 0xFB2B, 0xFB31, FE_LAST, 0xFFFF
        };
        int[] firstGlyphs = {
                GLYPH_SPACE, GLYPH_HEBREW_MARK, GLYPH_HEBREW_MARK + 1, GLYPH_HEBREW_LETTER,
                GLYPH_ARABIC_LETTER, GLYPH_ARABIC_MARK,
                GLYPH_THAI, GLYPH_LAO,
                GLYPH_HANGUL_LEAD, GLYPH_HANGUL_VOWEL, GLYPH_HANGUL_TRAIL, GLYPH_HANGUL_GA,
                GLYPH_FB2A, GLYPH_FB30, GLYPH_FE80, 0
        };
        int segCount = starts.length;
        ByteBuffer buffer = ByteBuffer.allocate(384).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 3);
        buffer.putShort((short) 1);
        buffer.putInt(12);
        int format4 = buffer.position();
        buffer.putShort((short) 4);
        int lengthPos = buffer.position();
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) (segCount * 2));
        int search = Integer.highestOneBit(segCount) * 2;
        buffer.putShort((short) search);
        buffer.putShort((short) Integer.numberOfTrailingZeros(Integer.highestOneBit(segCount)));
        buffer.putShort((short) (segCount * 2 - search));
        for (int end : ends) {
            buffer.putShort((short) end);
        }
        buffer.putShort((short) 0);
        for (int start : starts) {
            buffer.putShort((short) start);
        }
        for (int index = 0; index < segCount; index++) {
            int delta = index == segCount - 1 ? 1 : firstGlyphs[index] - starts[index];
            buffer.putShort((short) delta);
        }
        for (int index = 0; index < segCount; index++) {
            buffer.putShort((short) 0);
        }
        buffer.putShort(lengthPos, (short) (buffer.position() - format4));
        byte[] bytes = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(bytes);
        return bytes;
    }

    /// Writes glyf data.
    private static byte[] glyf() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int glyph = 0; glyph < glyphCount(); glyph++) {
            if (!emptyGlyph(glyph)) {
                BitmapSfntFont.writeSimpleRect(output, 0, 0, 5, 7);
            }
        }
        return output.toByteArray();
    }

    /// Writes long `loca` offsets matching [glyf()].
    private static byte[] loca(int glyfLength) {
        int outlined = 0;
        for (int glyph = 0; glyph < glyphCount(); glyph++) {
            if (!emptyGlyph(glyph)) {
                outlined++;
            }
        }
        int glyphBytes = outlined == 0 ? 0 : glyfLength / outlined;
        ByteBuffer loca = ByteBuffer.allocate((glyphCount() + 1) * 4).order(ByteOrder.BIG_ENDIAN);
        int running = 0;
        loca.putInt(0);
        for (int glyph = 0; glyph < glyphCount(); glyph++) {
            if (!emptyGlyph(glyph)) {
                running += glyphBytes;
            }
            loca.putInt(running);
        }
        return loca.array();
    }

    /// Writes the head table.
    private static byte[] head() {
        ByteBuffer buffer = ByteBuffer.allocate(54).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00010000);
        buffer.putInt(0x00010000);
        buffer.putInt(0);
        buffer.putInt(0x5F0F3CF5);
        buffer.putShort((short) 0);
        buffer.putShort((short) UNITS_PER_EM);
        buffer.putLong(0);
        buffer.putLong(0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 5);
        buffer.putShort((short) 7);
        buffer.putShort((short) 0);
        buffer.putShort((short) 8);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        return buffer.array();
    }

    /// Writes the hhea table.
    private static byte[] hhea() {
        ByteBuffer buffer = ByteBuffer.allocate(36).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00010000);
        buffer.putShort((short) 7);
        buffer.putShort((short) -1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 6);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) glyphCount());
        return buffer.array();
    }

    /// Writes the hmtx table.
    private static byte[] hmtx() {
        ByteBuffer buffer = ByteBuffer.allocate(glyphCount() * 4).order(ByteOrder.BIG_ENDIAN);
        for (int glyph = 0; glyph < glyphCount(); glyph++) {
            int advance = emptyGlyph(glyph) ? (glyph == GLYPH_SPACE ? 3 : 0) : 6;
            buffer.putShort((short) advance);
            buffer.putShort((short) 0);
        }
        return buffer.array();
    }

    /// Writes the maxp table.
    private static byte[] maxp() {
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00010000);
        buffer.putShort((short) glyphCount());
        buffer.putShort((short) 4);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        return buffer.array();
    }

    /// Writes a name table with one family name.
    private static byte[] name() {
        byte[] family = "HimariScript".getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(18 + family.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 18);
        buffer.putShort((short) 3);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0x0409);
        buffer.putShort((short) 1);
        buffer.putShort((short) family.length);
        buffer.putShort((short) 0);
        buffer.put(family);
        return buffer.array();
    }

    /// Writes a dummy post table.
    private static byte[] post() {
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00030000);
        return buffer.array();
    }
}
