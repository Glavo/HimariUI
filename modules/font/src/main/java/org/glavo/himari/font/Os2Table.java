package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/// Reads a first-stable OpenType `OS/2` table.
///
/// Version 0 supplies `usWeightClass`, `usWidthClass`, `fsType`, PANOSE, Unicode ranges,
/// `fsSelection`, `usFirstCharIndex`, `usLastCharIndex`, `sTypoAscender`, `sTypoDescender`,
/// `sTypoLineGap`, `usWinAscent`, and `usWinDescent`. Version 1 adds code-page ranges.
/// Version 2 also supplies `sxHeight` and `sCapHeight`. A missing or truncated table reports
/// regular weight, medium width, no embedding restriction, an empty PANOSE, no selection
/// bits, and zero typographic metrics.
@NotNullByDefault
public final class Os2Table {
    /// `usWeightClass` for Regular.
    public static final int WEIGHT_REGULAR = 400;

    /// `usWidthClass` for Medium.
    public static final int WIDTH_MEDIUM = 5;

    /// Shared empty table used when `OS/2` is absent.
    static final Os2Table EMPTY = new Os2Table(
            WEIGHT_REGULAR,
            WIDTH_MEDIUM,
            0,
            0,
            new byte[10],
            "",
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
    );

    /// `fsSelection` bold bit.
    public static final int FS_BOLD = 0x0020;

    /// CSS-style weight class, typically `100`–`900`.
    private final int weightClass;

    /// `usWidthClass`, typically `1`–`9`.
    private final int widthClass;

    /// `xAvgCharWidth` in font units.
    private final int avgCharWidth;

    /// `fsType` embedding bits.
    private final int fsType;

    /// Ten-byte PANOSE classification.
    private final byte @Unmodifiable [] panose;

    /// `achVendID` four-character vendor tag, or empty when absent.
    private final String vendorId;

    /// `fsSelection` bitfield.
    private final int fsSelection;

    /// `sTypoAscender` in font units.
    private final int typoAscender;

    /// `sTypoDescender` in font units.
    private final int typoDescender;

    /// `sTypoLineGap` in font units.
    private final int typoLineGap;

    /// `usWinAscent` in font units.
    private final int winAscent;

    /// `usWinDescent` in font units.
    private final int winDescent;

    /// `usFirstCharIndex`.
    private final int firstCharIndex;

    /// `usLastCharIndex`.
    private final int lastCharIndex;

    /// `sxHeight` in font units, or `0` before version 2.
    private final int xHeight;

    /// `sCapHeight` in font units, or `0` before version 2.
    private final int capHeight;

    /// `usDefaultChar`.
    private final int defaultChar;

    /// `usBreakChar`.
    private final int breakChar;

    /// `usMaxContext`.
    private final int maxContext;

    /// `ulUnicodeRange1`.
    private final int unicodeRange1;

    /// `ulUnicodeRange2`.
    private final int unicodeRange2;

    /// `ulUnicodeRange3`.
    private final int unicodeRange3;

    /// `ulUnicodeRange4`.
    private final int unicodeRange4;

    /// `ulCodePageRange1`, or `0` before version 1.
    private final int codePageRange1;

    /// `ulCodePageRange2`, or `0` before version 1.
    private final int codePageRange2;

    /// `ySubscriptXSize`.
    private final int subscriptXSize;

    /// `ySubscriptYSize`.
    private final int subscriptYSize;

    /// `ySubscriptXOffset`.
    private final int subscriptXOffset;

    /// `ySubscriptYOffset`.
    private final int subscriptYOffset;

    /// `ySuperscriptXSize`.
    private final int superscriptXSize;

    /// `ySuperscriptYSize`.
    private final int superscriptYSize;

    /// `ySuperscriptXOffset`.
    private final int superscriptXOffset;

    /// `ySuperscriptYOffset`.
    private final int superscriptYOffset;

    /// `yStrikeoutSize`.
    private final int strikeoutSize;

    /// `yStrikeoutPosition`.
    private final int strikeoutPosition;

    /// `sFamilyClass`.
    private final int familyClass;

    /// Creates a table.
    ///
    /// @param weightClass the weight class
    /// @param widthClass the width class
    /// @param avgCharWidth the average character width
    /// @param fsType the embedding bits
    /// @param panose the PANOSE bytes
    /// @param vendorId the vendor tag
    /// @param fsSelection the selection bits
    /// @param typoAscender the typographic ascender
    /// @param typoDescender the typographic descender
    /// @param typoLineGap the typographic line gap
    /// @param winAscent the Windows ascender
    /// @param winDescent the Windows descender
    /// @param firstCharIndex the first Unicode BMP index
    /// @param lastCharIndex the last Unicode BMP index
    /// @param xHeight the x-height
    /// @param capHeight the cap height
    /// @param defaultChar the default character
    /// @param breakChar the break character
    /// @param maxContext the maximum lookup context
    /// @param unicodeRange1 the first Unicode range bits
    /// @param unicodeRange2 the second Unicode range bits
    /// @param unicodeRange3 the third Unicode range bits
    /// @param unicodeRange4 the fourth Unicode range bits
    /// @param codePageRange1 the first code-page range bits
    /// @param codePageRange2 the second code-page range bits
    /// @param subscriptXSize the subscript x size
    /// @param subscriptYSize the subscript y size
    /// @param subscriptXOffset the subscript x offset
    /// @param subscriptYOffset the subscript y offset
    /// @param superscriptXSize the superscript x size
    /// @param superscriptYSize the superscript y size
    /// @param superscriptXOffset the superscript x offset
    /// @param superscriptYOffset the superscript y offset
    /// @param strikeoutSize the strikeout size
    /// @param strikeoutPosition the strikeout position
    /// @param familyClass the IBM family class
    private Os2Table(
            int weightClass,
            int widthClass,
            int avgCharWidth,
            int fsType,
            byte[] panose,
            String vendorId,
            int fsSelection,
            int typoAscender,
            int typoDescender,
            int typoLineGap,
            int winAscent,
            int winDescent,
            int firstCharIndex,
            int lastCharIndex,
            int xHeight,
            int capHeight,
            int defaultChar,
            int breakChar,
            int maxContext,
            int unicodeRange1,
            int unicodeRange2,
            int unicodeRange3,
            int unicodeRange4,
            int codePageRange1,
            int codePageRange2,
            int subscriptXSize,
            int subscriptYSize,
            int subscriptXOffset,
            int subscriptYOffset,
            int superscriptXSize,
            int superscriptYSize,
            int superscriptXOffset,
            int superscriptYOffset,
            int strikeoutSize,
            int strikeoutPosition,
            int familyClass
    ) {
        this.weightClass = weightClass;
        this.widthClass = widthClass;
        this.avgCharWidth = avgCharWidth;
        this.fsType = fsType;
        this.panose = Arrays.copyOf(panose, 10);
        this.vendorId = java.util.Objects.requireNonNull(vendorId, "vendorId");
        this.fsSelection = fsSelection;
        this.typoAscender = typoAscender;
        this.typoDescender = typoDescender;
        this.typoLineGap = typoLineGap;
        this.winAscent = winAscent;
        this.winDescent = winDescent;
        this.firstCharIndex = firstCharIndex;
        this.lastCharIndex = lastCharIndex;
        this.xHeight = xHeight;
        this.capHeight = capHeight;
        this.defaultChar = defaultChar;
        this.breakChar = breakChar;
        this.maxContext = maxContext;
        this.unicodeRange1 = unicodeRange1;
        this.unicodeRange2 = unicodeRange2;
        this.unicodeRange3 = unicodeRange3;
        this.unicodeRange4 = unicodeRange4;
        this.codePageRange1 = codePageRange1;
        this.codePageRange2 = codePageRange2;
        this.subscriptXSize = subscriptXSize;
        this.subscriptYSize = subscriptYSize;
        this.subscriptXOffset = subscriptXOffset;
        this.subscriptYOffset = subscriptYOffset;
        this.superscriptXSize = superscriptXSize;
        this.superscriptYSize = superscriptYSize;
        this.superscriptXOffset = superscriptXOffset;
        this.superscriptYOffset = superscriptYOffset;
        this.strikeoutSize = strikeoutSize;
        this.strikeoutPosition = strikeoutPosition;
        this.familyClass = familyClass;
    }

    /// Parses an `OS/2` table, or returns [`#EMPTY`].
    ///
    /// @param table the table bytes, or `null`
    /// @return the table
    static Os2Table parse(@Nullable ByteBuffer table) {
        if (table == null || table.remaining() < 78) {
            return EMPTY;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        buffer.getShort();
        int avgCharWidth = buffer.getShort();
        int weightClass = Short.toUnsignedInt(buffer.getShort());
        int widthClass = Short.toUnsignedInt(buffer.getShort());
        int fsType = Short.toUnsignedInt(buffer.getShort());
        int subscriptXSize = buffer.getShort();
        int subscriptYSize = buffer.getShort();
        int subscriptXOffset = buffer.getShort();
        int subscriptYOffset = buffer.getShort();
        int superscriptXSize = buffer.getShort();
        int superscriptYSize = buffer.getShort();
        int superscriptXOffset = buffer.getShort();
        int superscriptYOffset = buffer.getShort();
        int strikeoutSize = buffer.getShort();
        int strikeoutPosition = buffer.getShort();
        int familyClass = buffer.getShort();
        buffer.position(32);
        byte[] panose = new byte[10];
        buffer.get(panose);
        int unicodeRange1 = buffer.getInt();
        int unicodeRange2 = buffer.getInt();
        int unicodeRange3 = buffer.getInt();
        int unicodeRange4 = buffer.getInt();
        buffer.position(58);
        byte[] vendorBytes = new byte[4];
        buffer.get(vendorBytes);
        String vendorId = new String(vendorBytes, java.nio.charset.StandardCharsets.US_ASCII);
        buffer.position(62);
        int fsSelection = Short.toUnsignedInt(buffer.getShort());
        int firstCharIndex = Short.toUnsignedInt(buffer.getShort());
        int lastCharIndex = Short.toUnsignedInt(buffer.getShort());
        int typoAscender = buffer.getShort();
        int typoDescender = buffer.getShort();
        int typoLineGap = buffer.getShort();
        int winAscent = Short.toUnsignedInt(buffer.getShort());
        int winDescent = Short.toUnsignedInt(buffer.getShort());
        int xHeight = 0;
        int capHeight = 0;
        int defaultChar = 0;
        int breakChar = 0;
        int maxContext = 0;
        int codePageRange1 = 0;
        int codePageRange2 = 0;
        if (buffer.capacity() >= 86) {
            buffer.position(78);
            codePageRange1 = buffer.getInt();
            codePageRange2 = buffer.getInt();
        }
        if (buffer.capacity() >= 96) {
            buffer.position(86);
            xHeight = buffer.getShort();
            capHeight = buffer.getShort();
            defaultChar = Short.toUnsignedInt(buffer.getShort());
            breakChar = Short.toUnsignedInt(buffer.getShort());
            maxContext = Short.toUnsignedInt(buffer.getShort());
        } else if (buffer.capacity() >= 90) {
            buffer.position(86);
            xHeight = buffer.getShort();
            capHeight = buffer.getShort();
        }
        return new Os2Table(
                weightClass,
                widthClass,
                avgCharWidth,
                fsType,
                panose,
                vendorId,
                fsSelection,
                typoAscender,
                typoDescender,
                typoLineGap,
                winAscent,
                winDescent,
                firstCharIndex,
                lastCharIndex,
                xHeight,
                capHeight,
                defaultChar,
                breakChar,
                maxContext,
                unicodeRange1,
                unicodeRange2,
                unicodeRange3,
                unicodeRange4,
                codePageRange1,
                codePageRange2,
                subscriptXSize,
                subscriptYSize,
                subscriptXOffset,
                subscriptYOffset,
                superscriptXSize,
                superscriptYSize,
                superscriptXOffset,
                superscriptYOffset,
                strikeoutSize,
                strikeoutPosition,
                familyClass
        );
    }

    /// Returns `usWeightClass`.
    ///
    /// @return the weight class
    public int weightClass() {
        return weightClass;
    }

    /// Returns `usWidthClass`.
    ///
    /// @return the width class
    public int widthClass() {
        return widthClass;
    }

    /// Returns `xAvgCharWidth`.
    ///
    /// @return the average character width
    public int avgCharWidth() {
        return avgCharWidth;
    }

    /// Returns `fsType`.
    ///
    /// @return the embedding bits
    public int fsType() {
        return fsType;
    }

    /// Returns the PANOSE classification bytes.
    ///
    /// @return a copy-free ten-byte array
    public byte @Unmodifiable [] panose() {
        return panose;
    }

    /// Returns `achVendID`.
    ///
    /// @return the four-character vendor tag, or empty when absent
    public String vendorId() {
        return vendorId;
    }

    /// Returns `fsSelection`.
    ///
    /// @return the selection bits
    public int fsSelection() {
        return fsSelection;
    }

    /// Returns `sTypoAscender`.
    ///
    /// @return the typographic ascender
    public int typoAscender() {
        return typoAscender;
    }

    /// Returns `sTypoDescender`.
    ///
    /// @return the typographic descender
    public int typoDescender() {
        return typoDescender;
    }

    /// Returns `sTypoLineGap`.
    ///
    /// @return the typographic line gap
    public int typoLineGap() {
        return typoLineGap;
    }

    /// Returns `usWinAscent`.
    ///
    /// @return the Windows ascender
    public int winAscent() {
        return winAscent;
    }

    /// Returns `usWinDescent`.
    ///
    /// @return the Windows descender
    public int winDescent() {
        return winDescent;
    }

    /// Returns `usFirstCharIndex`.
    ///
    /// @return the first Unicode BMP index
    public int firstCharIndex() {
        return firstCharIndex;
    }

    /// Returns `usLastCharIndex`.
    ///
    /// @return the last Unicode BMP index
    public int lastCharIndex() {
        return lastCharIndex;
    }

    /// Returns `sxHeight`.
    ///
    /// @return the x-height, or `0` before version 2
    public int xHeight() {
        return xHeight;
    }

    /// Returns `sCapHeight`.
    ///
    /// @return the cap height, or `0` before version 2
    public int capHeight() {
        return capHeight;
    }

    /// Returns `usDefaultChar`.
    ///
    /// @return the default character
    public int defaultChar() {
        return defaultChar;
    }

    /// Returns `usBreakChar`.
    ///
    /// @return the break character
    public int breakChar() {
        return breakChar;
    }

    /// Returns `usMaxContext`.
    ///
    /// @return the maximum lookup context
    public int maxContext() {
        return maxContext;
    }

    /// Returns `ulUnicodeRange1`.
    ///
    /// @return the first Unicode range bits
    public int unicodeRange1() {
        return unicodeRange1;
    }

    /// Returns `ulUnicodeRange2`.
    ///
    /// @return the second Unicode range bits
    public int unicodeRange2() {
        return unicodeRange2;
    }

    /// Returns `ulUnicodeRange3`.
    ///
    /// @return the third Unicode range bits
    public int unicodeRange3() {
        return unicodeRange3;
    }

    /// Returns `ulUnicodeRange4`.
    ///
    /// @return the fourth Unicode range bits
    public int unicodeRange4() {
        return unicodeRange4;
    }

    /// Returns `ulCodePageRange1`.
    ///
    /// @return the first code-page range bits, or `0` before version 1
    public int codePageRange1() {
        return codePageRange1;
    }

    /// Returns `ulCodePageRange2`.
    ///
    /// @return the second code-page range bits, or `0` before version 1
    public int codePageRange2() {
        return codePageRange2;
    }

    /// Returns `ySubscriptXSize`.
    ///
    /// @return the subscript x size
    public int subscriptXSize() {
        return subscriptXSize;
    }

    /// Returns `ySubscriptYSize`.
    ///
    /// @return the subscript y size
    public int subscriptYSize() {
        return subscriptYSize;
    }

    /// Returns `ySubscriptXOffset`.
    ///
    /// @return the subscript x offset
    public int subscriptXOffset() {
        return subscriptXOffset;
    }

    /// Returns `ySubscriptYOffset`.
    ///
    /// @return the subscript y offset
    public int subscriptYOffset() {
        return subscriptYOffset;
    }

    /// Returns `ySuperscriptXSize`.
    ///
    /// @return the superscript x size
    public int superscriptXSize() {
        return superscriptXSize;
    }

    /// Returns `ySuperscriptYSize`.
    ///
    /// @return the superscript y size
    public int superscriptYSize() {
        return superscriptYSize;
    }

    /// Returns `ySuperscriptXOffset`.
    ///
    /// @return the superscript x offset
    public int superscriptXOffset() {
        return superscriptXOffset;
    }

    /// Returns `ySuperscriptYOffset`.
    ///
    /// @return the superscript y offset
    public int superscriptYOffset() {
        return superscriptYOffset;
    }

    /// Returns `yStrikeoutSize`.
    ///
    /// @return the strikeout size
    public int strikeoutSize() {
        return strikeoutSize;
    }

    /// Returns `yStrikeoutPosition`.
    ///
    /// @return the strikeout position
    public int strikeoutPosition() {
        return strikeoutPosition;
    }

    /// Returns `sFamilyClass`.
    ///
    /// @return the IBM family class
    public int familyClass() {
        return familyClass;
    }
}
