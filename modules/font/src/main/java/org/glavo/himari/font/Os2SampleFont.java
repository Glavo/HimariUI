package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/// Generates a TrueType face with a version-2 `OS/2` table and a Windows Unicode family name.
@NotNullByDefault
public final class Os2SampleFont {
    /// Isolated letter advance.
    public static final int ADVANCE_LETTER = 10;

    /// Glyph of `A`.
    public static final int GLYPH_A = 2;

    /// Stored `usWeightClass`.
    public static final int WEIGHT_CLASS = 700;

    /// Stored `usWidthClass`.
    public static final int WIDTH_CLASS = 3;

    /// Stored `xAvgCharWidth`.
    public static final int AVG_CHAR_WIDTH = 5;

    /// Stored `fsType` preview-and-print embedding.
    public static final int FS_TYPE = 0x0004;

    /// Stored PANOSE classification.
    public static final byte[] PANOSE = {2, 11, 6, 4, 2, 2, 2, 2, 2, 4};

    /// Stored `achVendID`.
    public static final String VENDOR = "HMRI";

    /// Stored `fsSelection`.
    public static final int FS_SELECTION = Os2Table.FS_BOLD;

    /// Stored `sTypoAscender`.
    public static final int TYPO_ASCENDER = 8;

    /// Stored `sTypoDescender`.
    public static final int TYPO_DESCENDER = -2;

    /// Stored `sTypoLineGap`.
    public static final int TYPO_LINE_GAP = 1;

    /// Stored `usWinAscent`.
    public static final int WIN_ASCENT = 8;

    /// Stored `usWinDescent`.
    public static final int WIN_DESCENT = 2;

    /// Stored Windows Unicode `nameID 0`.
    public static final String COPYRIGHT = "Copyright 2026 Himari";

    /// Stored Windows Unicode `nameID 1`.
    public static final String FAMILY = "HimariOs2";

    /// Stored Windows Unicode `nameID 3`.
    public static final String UNIQUE = "HimariOs2:2026";

    /// Stored Windows Unicode `nameID 2`.
    public static final String STYLE = "Bold";

    /// Stored Windows Unicode `nameID 4`.
    public static final String FULL = "HimariOs2 Bold";

    /// Stored Windows Unicode `nameID 5`.
    public static final String VERSION = "Version 1.000";

    /// Stored Windows Unicode `nameID 6`.
    public static final String POSTSCRIPT = "HimariOs2-Bold";

    /// Stored Windows Unicode `nameID 7`.
    public static final String TRADEMARK = "Himari";

    /// Stored Windows Unicode `nameID 8`.
    public static final String MANUFACTURER = "HimariType";

    /// Stored Windows Unicode `nameID 9`.
    public static final String DESIGNER = "Himari";

    /// Stored Windows Unicode `nameID 10`.
    public static final String DESCRIPTION = "OS/2 sample";

    /// Stored Windows Unicode `nameID 16`.
    public static final String TYPOGRAPHIC_FAMILY = "HimariOs2";

    /// Stored Windows Unicode `nameID 17`.
    public static final String TYPOGRAPHIC_SUBFAMILY = "Bold";

    /// Stored `sxHeight`.
    public static final int X_HEIGHT = 5;

    /// Stored `sCapHeight`.
    public static final int CAP_HEIGHT = 7;

    /// Stored Windows Unicode `nameID 11`.
    public static final String VENDOR_URL = "https://himari.test";

    /// Stored Windows Unicode `nameID 13`.
    public static final String LICENSE = "OFL";

    /// Stored `usDefaultChar`.
    public static final int DEFAULT_CHAR = 0;

    /// Stored `usBreakChar`.
    public static final int BREAK_CHAR = 32;

    /// Stored `usMaxContext`.
    public static final int MAX_CONTEXT = 2;

    /// Stored Windows Unicode `nameID 12`.
    public static final String DESIGNER_URL = "https://designer.himari.test";

    /// Stored Windows Unicode `nameID 14`.
    public static final String LICENSE_URL = "https://license.himari.test";

    /// Stored `ulUnicodeRange1` Basic Latin bit.
    public static final int UNICODE_RANGE1 = 0x00000001;

    /// Stored `ulUnicodeRange2` Latin-1 Supplement bit.
    public static final int UNICODE_RANGE2 = 0x00000002;

    /// Stored `ulUnicodeRange3`.
    public static final int UNICODE_RANGE3 = 0x00000004;

    /// Stored `ulUnicodeRange4`.
    public static final int UNICODE_RANGE4 = 0x00000008;

    /// Stored `ulCodePageRange1` 1252 bit.
    public static final int CODE_PAGE_RANGE1 = 0x00000001;

    /// Stored `ulCodePageRange2`.
    public static final int CODE_PAGE_RANGE2 = 0x00000002;

    /// Stored `ySubscriptXSize`.
    public static final int SUBSCRIPT_X_SIZE = 4;

    /// Stored `ySubscriptYSize`.
    public static final int SUBSCRIPT_Y_SIZE = 4;

    /// Stored `ySubscriptXOffset`.
    public static final int SUBSCRIPT_X_OFFSET = 0;

    /// Stored `ySubscriptYOffset`.
    public static final int SUBSCRIPT_Y_OFFSET = -2;

    /// Stored `ySuperscriptXSize`.
    public static final int SUPERSCRIPT_X_SIZE = 4;

    /// Stored `ySuperscriptYSize`.
    public static final int SUPERSCRIPT_Y_SIZE = 4;

    /// Stored `ySuperscriptXOffset`.
    public static final int SUPERSCRIPT_X_OFFSET = 0;

    /// Stored `ySuperscriptYOffset`.
    public static final int SUPERSCRIPT_Y_OFFSET = 4;

    /// Stored `yStrikeoutSize`.
    public static final int STRIKEOUT_SIZE = 1;

    /// Stored `yStrikeoutPosition`.
    public static final int STRIKEOUT_POSITION = 3;

    /// Stored `sFamilyClass` sans-serif.
    public static final int FAMILY_CLASS = 0x0800;

    /// Stored Windows Unicode `nameID 19`.
    public static final String SAMPLE_TEXT = "Sample";

    /// Stored Windows Unicode `nameID 18`.
    public static final String COMPATIBLE_FULL = "HimariOs2 Bold";

    /// Stored Windows Unicode `nameID 20`.
    public static final String POST_SCRIPT_CID = "HimariOs2-Bold-CID";

    /// Stored Windows Unicode `nameID 25`.
    public static final String VARIATIONS_POST_SCRIPT_PREFIX = "HimariOs2Bold";

    /// Stored Windows Unicode `nameID 21`.
    public static final String WWS_FAMILY = "HimariOs2";

    /// Stored Windows Unicode `nameID 22`.
    public static final String WWS_SUBFAMILY = "Bold";

    /// Stored `usFirstCharIndex`.
    public static final int FIRST_CHAR_INDEX = 32;

    /// Stored `usLastCharIndex`.
    public static final int LAST_CHAR_INDEX = 'A';

    /// Stored `post.italicAngle` in degrees.
    public static final float ITALIC_ANGLE = 10.0f;

    /// Stored `post.underlinePosition`.
    public static final int UNDERLINE_POSITION = -2;

    /// Stored `post.underlineThickness`.
    public static final int UNDERLINE_THICKNESS = 1;

    /// Glyph count including `.notdef` and space.
    private static final int GLYPH_COUNT = 3;

    /// Prevents instantiation.
    private Os2SampleFont() {
    }

    /// Builds the OS/2 sample font.
    ///
    /// @return the parsed font
    public static SfntFont create() {
        return new SfntFont(bytes());
    }

    /// Builds the font image.
    ///
    /// @return a read-only SFNT file
    public static MemorySegment bytes() {
        LinkedHashMap<String, byte[]> tables = new LinkedHashMap<>();
        tables.put("cmap", cmap());
        byte[] glyf = glyf();
        tables.put("glyf", glyf);
        tables.put("OS/2", os2());
        tables.put("head", head());
        tables.put("hhea", hhea());
        tables.put("hmtx", hmtx());
        tables.put("loca", loca(glyf.length));
        tables.put("maxp", maxp());
        tables.put("name", name());
        tables.put("post", post());
        return MemorySegment.ofArray(BitmapSfntFont.wrap(tables)).asReadOnly();
    }

    /// Writes a format-4 cmap for space and `A`.
    private static byte[] cmap() {
        ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.BIG_ENDIAN);
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
        buffer.putShort((short) 6);
        buffer.putShort((short) 4);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) 0);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) 0xFFFF);
        buffer.putShort((short) (1 - 32));
        buffer.putShort((short) (GLYPH_A - 'A'));
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort(lengthPos, (short) (buffer.position() - format4));
        byte[] bytes = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(bytes);
        return bytes;
    }

    /// Writes a version-2 `OS/2` table.
    private static byte[] os2() {
        ByteBuffer buffer = ByteBuffer.allocate(96).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 2);
        buffer.putShort((short) AVG_CHAR_WIDTH);
        buffer.putShort((short) WEIGHT_CLASS);
        buffer.putShort((short) WIDTH_CLASS);
        buffer.putShort((short) FS_TYPE);
        buffer.putShort((short) SUBSCRIPT_X_SIZE);
        buffer.putShort((short) SUBSCRIPT_Y_SIZE);
        buffer.putShort((short) SUBSCRIPT_X_OFFSET);
        buffer.putShort((short) SUBSCRIPT_Y_OFFSET);
        buffer.putShort((short) SUPERSCRIPT_X_SIZE);
        buffer.putShort((short) SUPERSCRIPT_Y_SIZE);
        buffer.putShort((short) SUPERSCRIPT_X_OFFSET);
        buffer.putShort((short) SUPERSCRIPT_Y_OFFSET);
        buffer.putShort((short) STRIKEOUT_SIZE);
        buffer.putShort((short) STRIKEOUT_POSITION);
        buffer.putShort((short) FAMILY_CLASS);
        buffer.position(32);
        buffer.put(PANOSE);
        buffer.putInt(UNICODE_RANGE1);
        buffer.putInt(UNICODE_RANGE2);
        buffer.putInt(UNICODE_RANGE3);
        buffer.putInt(UNICODE_RANGE4);
        buffer.position(58);
        buffer.put(VENDOR.getBytes(StandardCharsets.US_ASCII));
        buffer.position(62);
        buffer.putShort((short) FS_SELECTION);
        buffer.putShort((short) 32);
        buffer.putShort((short) 'A');
        buffer.putShort((short) TYPO_ASCENDER);
        buffer.putShort((short) TYPO_DESCENDER);
        buffer.putShort((short) TYPO_LINE_GAP);
        buffer.putShort((short) WIN_ASCENT);
        buffer.putShort((short) WIN_DESCENT);
        buffer.putInt(CODE_PAGE_RANGE1);
        buffer.putInt(CODE_PAGE_RANGE2);
        buffer.position(86);
        buffer.putShort((short) X_HEIGHT);
        buffer.putShort((short) CAP_HEIGHT);
        buffer.putShort((short) DEFAULT_CHAR);
        buffer.putShort((short) BREAK_CHAR);
        buffer.putShort((short) MAX_CONTEXT);
        return buffer.array();
    }

    /// Writes a rectangle outline for `A`.
    private static byte[] glyf() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BitmapSfntFont.writeSimpleRect(output, 0, 0, 8, 8);
        return output.toByteArray();
    }

    /// Writes long `loca` offsets.
    private static byte[] loca(int glyfLength) {
        ByteBuffer loca = ByteBuffer.allocate((GLYPH_COUNT + 1) * 4).order(ByteOrder.BIG_ENDIAN);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(0);
        loca.putInt(glyfLength);
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
        buffer.putShort((short) 8);
        buffer.putLong(0);
        buffer.putLong(0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 8);
        buffer.putShort((short) 8);
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
        buffer.putShort((short) 8);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) ADVANCE_LETTER);
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
        buffer.putShort((short) GLYPH_COUNT);
        return buffer.array();
    }

    /// Writes the hmtx table.
    private static byte[] hmtx() {
        ByteBuffer buffer = ByteBuffer.allocate(GLYPH_COUNT * 4).order(ByteOrder.BIG_ENDIAN);
        int[] advances = {0, 3, ADVANCE_LETTER};
        for (int advance : advances) {
            buffer.putShort((short) advance);
            buffer.putShort((short) 0);
        }
        return buffer.array();
    }

    /// Writes the maxp table.
    private static byte[] maxp() {
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00010000);
        buffer.putShort((short) GLYPH_COUNT);
        buffer.putShort((short) 3);
        buffer.putShort((short) 1);
        return buffer.array();
    }

    /// Writes Windows Unicode copyright through designer names.
    private static byte[] name() {
        byte[] copyright = COPYRIGHT.getBytes(StandardCharsets.UTF_16BE);
        byte[] family = FAMILY.getBytes(StandardCharsets.UTF_16BE);
        byte[] unique = UNIQUE.getBytes(StandardCharsets.UTF_16BE);
        byte[] style = STYLE.getBytes(StandardCharsets.UTF_16BE);
        byte[] full = FULL.getBytes(StandardCharsets.UTF_16BE);
        byte[] version = VERSION.getBytes(StandardCharsets.UTF_16BE);
        byte[] postScript = POSTSCRIPT.getBytes(StandardCharsets.UTF_16BE);
        byte[] trademark = TRADEMARK.getBytes(StandardCharsets.UTF_16BE);
        byte[] manufacturer = MANUFACTURER.getBytes(StandardCharsets.UTF_16BE);
        byte[] designer = DESIGNER.getBytes(StandardCharsets.UTF_16BE);
        byte[] description = DESCRIPTION.getBytes(StandardCharsets.UTF_16BE);
        byte[] typoFamily = TYPOGRAPHIC_FAMILY.getBytes(StandardCharsets.UTF_16BE);
        byte[] typoSubfamily = TYPOGRAPHIC_SUBFAMILY.getBytes(StandardCharsets.UTF_16BE);
        byte[] vendorUrl = VENDOR_URL.getBytes(StandardCharsets.UTF_16BE);
        byte[] license = LICENSE.getBytes(StandardCharsets.UTF_16BE);
        byte[] designerUrl = DESIGNER_URL.getBytes(StandardCharsets.UTF_16BE);
        byte[] licenseUrl = LICENSE_URL.getBytes(StandardCharsets.UTF_16BE);
        byte[] wwsFamily = WWS_FAMILY.getBytes(StandardCharsets.UTF_16BE);
        byte[] wwsSubfamily = WWS_SUBFAMILY.getBytes(StandardCharsets.UTF_16BE);
        byte[] sampleText = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_16BE);
        byte[] compatibleFull = COMPATIBLE_FULL.getBytes(StandardCharsets.UTF_16BE);
        byte[] postScriptCid = POST_SCRIPT_CID.getBytes(StandardCharsets.UTF_16BE);
        byte[] variationsPrefix = VARIATIONS_POST_SCRIPT_PREFIX.getBytes(StandardCharsets.UTF_16BE);
        int storage = 6 + 276;
        ByteBuffer buffer = ByteBuffer.allocate(
                storage
                        + copyright.length
                        + family.length
                        + unique.length
                        + style.length
                        + full.length
                        + version.length
                        + postScript.length
                        + trademark.length
                        + manufacturer.length
                        + designer.length
                        + description.length
                        + typoFamily.length
                        + typoSubfamily.length
                        + vendorUrl.length
                        + license.length
                        + designerUrl.length
                        + licenseUrl.length
                        + wwsFamily.length
                        + wwsSubfamily.length
                        + sampleText.length
                        + compatibleFull.length
                        + postScriptCid.length
                        + variationsPrefix.length
        ).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0);
        buffer.putShort((short) 23);
        buffer.putShort((short) storage);
        int offset = 0;
        putNameRecord(buffer, 0, offset, copyright.length);
        offset += copyright.length;
        putNameRecord(buffer, 1, offset, family.length);
        offset += family.length;
        putNameRecord(buffer, 3, offset, unique.length);
        offset += unique.length;
        putNameRecord(buffer, 2, offset, style.length);
        offset += style.length;
        putNameRecord(buffer, 4, offset, full.length);
        offset += full.length;
        putNameRecord(buffer, 5, offset, version.length);
        offset += version.length;
        putNameRecord(buffer, 6, offset, postScript.length);
        offset += postScript.length;
        putNameRecord(buffer, 7, offset, trademark.length);
        offset += trademark.length;
        putNameRecord(buffer, 8, offset, manufacturer.length);
        offset += manufacturer.length;
        putNameRecord(buffer, 9, offset, designer.length);
        offset += designer.length;
        putNameRecord(buffer, 10, offset, description.length);
        offset += description.length;
        putNameRecord(buffer, 16, offset, typoFamily.length);
        offset += typoFamily.length;
        putNameRecord(buffer, 17, offset, typoSubfamily.length);
        offset += typoSubfamily.length;
        putNameRecord(buffer, 11, offset, vendorUrl.length);
        offset += vendorUrl.length;
        putNameRecord(buffer, 13, offset, license.length);
        offset += license.length;
        putNameRecord(buffer, 12, offset, designerUrl.length);
        offset += designerUrl.length;
        putNameRecord(buffer, 14, offset, licenseUrl.length);
        offset += licenseUrl.length;
        putNameRecord(buffer, 21, offset, wwsFamily.length);
        offset += wwsFamily.length;
        putNameRecord(buffer, 22, offset, wwsSubfamily.length);
        offset += wwsSubfamily.length;
        putNameRecord(buffer, 19, offset, sampleText.length);
        offset += sampleText.length;
        putNameRecord(buffer, 18, offset, compatibleFull.length);
        offset += compatibleFull.length;
        putNameRecord(buffer, 20, offset, postScriptCid.length);
        offset += postScriptCid.length;
        putNameRecord(buffer, 25, offset, variationsPrefix.length);
        buffer.put(copyright);
        buffer.put(family);
        buffer.put(unique);
        buffer.put(style);
        buffer.put(full);
        buffer.put(version);
        buffer.put(postScript);
        buffer.put(trademark);
        buffer.put(manufacturer);
        buffer.put(designer);
        buffer.put(description);
        buffer.put(typoFamily);
        buffer.put(typoSubfamily);
        buffer.put(vendorUrl);
        buffer.put(license);
        buffer.put(designerUrl);
        buffer.put(licenseUrl);
        buffer.put(wwsFamily);
        buffer.put(wwsSubfamily);
        buffer.put(sampleText);
        buffer.put(compatibleFull);
        buffer.put(postScriptCid);
        buffer.put(variationsPrefix);
        return buffer.array();
    }

    /// Writes one Windows Unicode name record.
    private static void putNameRecord(ByteBuffer buffer, int nameId, int offset, int length) {
        buffer.putShort((short) 3);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0x0409);
        buffer.putShort((short) nameId);
        buffer.putShort((short) length);
        buffer.putShort((short) offset);
    }

    /// Writes a version-3 `post` header with a ten-degree italic and a fixed pitch.
    private static byte[] post() {
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x00030000);
        buffer.putInt(0x000A0000);
        buffer.putShort((short) -2);
        buffer.putShort((short) 1);
        buffer.putInt(1);
        return buffer.array();
    }
}
