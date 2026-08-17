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
/// Presentation-form glyphs are distinct from their nominal letters so joining, lam-alef, and
/// Hebrew composition can be observed through `cmap` identities.
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
    private static final int GLYPH_FB30 = 180;

    /// Glyph of `U+FB4B`.
    private static final int GLYPH_FB4B = 182;

    /// Glyph of `U+FE80`.
    private static final int GLYPH_FE80 = 183;

    /// Last Presentation Forms-B code point stored in this font, including lam-alef ligatures.
    private static final int FE_LAST = 0xFEFC;

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

    /// Glyph of `U+FB1D`.
    private static final int GLYPH_FB1D = GLYPH_LAO + (LAO_LAST - 0x0E81) + 1;

    /// Glyph of `U+FB2E`.
    private static final int GLYPH_FB2E = GLYPH_FB1D + 1;

    /// Glyph of `U+FB2F`.
    private static final int GLYPH_FB2F = GLYPH_FB2E + 1;

    /// Glyph of `U+FB4C`.
    private static final int GLYPH_FB4C = GLYPH_FB2F + 1;

    /// Glyph of `U+FB1F`.
    private static final int GLYPH_FB1F = GLYPH_FB4C + 4;

    /// Glyph of `U+FB4F`.
    private static final int GLYPH_FB4F = GLYPH_FB4C + 3;

    /// Glyph of Yiddish double vav `U+05F0`.
    private static final int GLYPH_YIDDISH = GLYPH_FB4C + 5;

    /// Glyph of Lao ho-no `U+0EDC`.
    private static final int GLYPH_LAO_LIGATURE = GLYPH_YIDDISH + 3;

    /// Glyph of shadda-plus-fatha `U+FC60`.
    private static final int GLYPH_SHADDA = GLYPH_LAO_LIGATURE + 2;

    /// Glyph of isolated Allah `U+FDF2`.
    private static final int GLYPH_ALLAH = GLYPH_SHADDA + 6;

    /// Glyph of alef wasla `U+0671`.
    private static final int GLYPH_WASLA = GLYPH_ALLAH + 1;

    /// Glyph of isolated alef wasla `U+FB50`.
    private static final int GLYPH_FB50 = GLYPH_WASLA + 1;

    /// Glyph of peh `U+067E`.
    private static final int GLYPH_PEH = GLYPH_FB50 + 2;

    /// Glyph of tcheh `U+0686`.
    private static final int GLYPH_TCHEH = GLYPH_PEH + 1;

    /// Glyph of isolated peh `U+FB56`.
    private static final int GLYPH_FB56 = GLYPH_TCHEH + 1;

    /// Glyph of isolated tcheh `U+FB7A`.
    private static final int GLYPH_FB7A = GLYPH_FB56 + 4;

    /// Glyph of tteh `U+0679`.
    private static final int GLYPH_TTEH = GLYPH_FB7A + 4;

    /// Glyph of jeh `U+0698`.
    private static final int GLYPH_JEH = GLYPH_TTEH + 1;

    /// Glyph of veh `U+06A4`.
    private static final int GLYPH_VEH = GLYPH_JEH + 1;

    /// Glyph of keheh `U+06A9`.
    private static final int GLYPH_KEHEH = GLYPH_VEH + 1;

    /// Glyph of gaf `U+06AF`.
    private static final int GLYPH_GAF = GLYPH_KEHEH + 1;

    /// Glyph of farsi yeh `U+06CC`.
    private static final int GLYPH_FARSI = GLYPH_GAF + 1;

    /// Glyph of isolated tteh `U+FB66`.
    private static final int GLYPH_FB66 = GLYPH_FARSI + 1;

    /// Glyph of isolated veh `U+FB6A`.
    private static final int GLYPH_FB6A = GLYPH_FB66 + 4;

    /// Glyph of isolated jeh `U+FB8A`.
    private static final int GLYPH_FB8A = GLYPH_FB6A + 4;

    /// Glyph of isolated keheh `U+FB8E`.
    private static final int GLYPH_FB8E = GLYPH_FB8A + 2;

    /// Glyph of isolated gaf `U+FB92`.
    private static final int GLYPH_FB92 = GLYPH_FB8E + 4;

    /// Glyph of noon ghunna `U+06BA`.
    private static final int GLYPH_NOON_GHUNNA = GLYPH_FB92 + 4;

    /// Glyph of yeh barree `U+06D2`.
    private static final int GLYPH_YEH_BARREE = GLYPH_NOON_GHUNNA + 1;

    /// Glyph of isolated noon ghunna `U+FB9E`.
    private static final int GLYPH_FB9E = GLYPH_YEH_BARREE + 1;

    /// Glyph of isolated yeh barree `U+FBAE`.
    private static final int GLYPH_FBAE = GLYPH_FB9E + 2;

    /// Glyph of isolated farsi yeh `U+FBFC`.
    private static final int GLYPH_FBFC = GLYPH_FBAE + 2;

    /// Glyph of final nun plus dagesh `U+FB3F`.
    private static final int GLYPH_FB3F = GLYPH_FBFC + 4;

    /// Glyph of final tsadi plus dagesh `U+FB45`.
    private static final int GLYPH_FB45 = GLYPH_FB3F + 1;

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
        return GLYPH_FB45 + 1;
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
        if (glyphId >= GLYPH_LAO && glyphId < GLYPH_FB1D) {
            return thaiLaoMark(0x0E81 + (glyphId - GLYPH_LAO));
        }
        return false;
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
                ASCII_FIRST, 0x05B9, 0x05BC, 0x05C1, 0x05C7, 0x05D0, 0x05F0, 0x0621, 0x064B,
                0x0671, 0x0679, 0x067E, 0x0686, 0x0698, 0x06A4, 0x06A9, 0x06AF, 0x06BA, 0x06CC, 0x06D2,
                0x0E01, 0x0E81, 0x0EDC, 0x1100, 0x1161, 0x11A8, 0xAC00,
                0xFB1D, 0xFB1F, 0xFB2A, 0xFB2E, 0xFB30, 0xFB3F, 0xFB45, 0xFB4B, 0xFB4C, 0xFB50,
                0xFB56, 0xFB66, 0xFB6A, 0xFB7A, 0xFB8A, 0xFB8E, 0xFB92, 0xFB9E, 0xFBAE, 0xFBFC,
                0xFC5E, 0xFDF2, 0xFE80, 0xFFFF
        };
        int[] ends = {
                ASCII_LAST, 0x05BA, 0x05BC, 0x05C2, 0x05C7, 0x05EA, 0x05F2, 0x064A, 0x0652,
                0x0671, 0x0679, 0x067E, 0x0686, 0x0698, 0x06A4, 0x06A9, 0x06AF, 0x06BA, 0x06CC, 0x06D2,
                THAI_LAST, LAO_LAST, 0x0EDD, 0x1100, 0x1161, 0x11A8, 0xAC01,
                0xFB1D, 0xFB1F, 0xFB2D, 0xFB2F, 0xFB31, 0xFB3F, 0xFB45, 0xFB4B, 0xFB4F, 0xFB51,
                0xFB59, 0xFB69, 0xFB6D, 0xFB7D, 0xFB8B, 0xFB91, 0xFB95, 0xFB9F, 0xFBAF, 0xFBFF,
                0xFC63, 0xFDF2, FE_LAST, 0xFFFF
        };
        int[] firstGlyphs = {
                GLYPH_SPACE, GLYPH_HEBREW_MARK, GLYPH_HEBREW_MARK, GLYPH_HEBREW_MARK + 1, GLYPH_HEBREW_MARK,
                GLYPH_HEBREW_LETTER, GLYPH_YIDDISH,
                GLYPH_ARABIC_LETTER, GLYPH_ARABIC_MARK,
                GLYPH_WASLA, GLYPH_TTEH, GLYPH_PEH, GLYPH_TCHEH, GLYPH_JEH, GLYPH_VEH, GLYPH_KEHEH, GLYPH_GAF,
                GLYPH_NOON_GHUNNA, GLYPH_FARSI, GLYPH_YEH_BARREE,
                GLYPH_THAI, GLYPH_LAO, GLYPH_LAO_LIGATURE,
                GLYPH_HANGUL_LEAD, GLYPH_HANGUL_VOWEL, GLYPH_HANGUL_TRAIL, GLYPH_HANGUL_GA,
                GLYPH_FB1D, GLYPH_FB1F, GLYPH_FB2A, GLYPH_FB2E, GLYPH_FB30, GLYPH_FB3F, GLYPH_FB45, GLYPH_FB4B,
                GLYPH_FB4C, GLYPH_FB50,
                GLYPH_FB56, GLYPH_FB66, GLYPH_FB6A, GLYPH_FB7A, GLYPH_FB8A, GLYPH_FB8E, GLYPH_FB92, GLYPH_FB9E,
                GLYPH_FBAE, GLYPH_FBFC,
                GLYPH_SHADDA, GLYPH_ALLAH, GLYPH_FE80, 0
        };
        int segCount = starts.length;
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.BIG_ENDIAN);
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
