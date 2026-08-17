package org.glavo.himari.text;

import org.glavo.himari.font.ScriptSampleFont;
import org.glavo.himari.font.SfntFont;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/// Writes M8 Arabic, Hebrew, Hangul, Thai, and Lao shaping evidence.
@NotNullByDefault
public final class ShapeConformance {
    /// Prevents instantiation.
    private ShapeConformance() {
    }

    /// Shapes Arabic joining and Hebrew composition, then writes the report.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        SfntFont font = ScriptSampleFont.create();
        List<ShapedGlyph> isolated = DefaultShaper.shape(font, "\u0628");
        List<ShapedGlyph> triple = DefaultShaper.shape(font, "\u0628\u0628\u0628");
        List<ShapedGlyph> marked = DefaultShaper.shape(font, "\u0628\u064E\u062A");
        List<ShapedGlyph> hebrew = DefaultShaper.shape(font, "\u05D0\u05D1");
        List<ShapedGlyph> dagesh = DefaultShaper.shape(font, "\u05D1\u05BC");
        List<ShapedGlyph> yodHiriq = DefaultShaper.shape(font, "\u05D9\u05B4");
        List<ShapedGlyph> alefPatah = DefaultShaper.shape(font, "\u05D0\u05B7");
        List<ShapedGlyph> hangulLv = DefaultShaper.shape(font, "\u1100\u1161");
        List<ShapedGlyph> hangulLvt = DefaultShaper.shape(font, "\u1100\u1161\u11A8");
        List<ShapedGlyph> thaiAm = DefaultShaper.shape(font, "\u0E14\u0E4B\u0E33");
        List<ShapedGlyph> thaiLeft = DefaultShaper.shape(font, "\u0E40\u0E01");
        List<ShapedGlyph> laoAm = DefaultShaper.shape(font, "\u0E81\u0EB3");
        if (isolated.size() != 1 || isolated.getFirst().codePoint() != 0xFE8F || isolated.getFirst().glyphId() <= 0) {
            throw new IllegalStateException("Isolated Beh did not map to Presentation Forms-B");
        }
        if (triple.size() != 3
                || triple.get(0).codePoint() != 0xFE91
                || triple.get(1).codePoint() != 0xFE92
                || triple.get(2).codePoint() != 0xFE90) {
            throw new IllegalStateException("Beh run did not select init/medi/fina");
        }
        if (marked.size() != 3
                || marked.get(0).codePoint() != 0xFE91
                || marked.get(1).cluster() != 0
                || marked.get(2).codePoint() != 0xFE96) {
            throw new IllegalStateException("Arabic joining did not skip a fatha");
        }
        if (hebrew.size() != 2 || hebrew.get(0).codePoint() != 0x05D0 || hebrew.get(1).codePoint() != 0x05D1) {
            throw new IllegalStateException("Unmarked Hebrew was not one-to-one");
        }
        if (dagesh.size() != 1 || dagesh.getFirst().codePoint() != 0xFB31 || dagesh.getFirst().glyphId() <= 0) {
            throw new IllegalStateException("Hebrew dagesh did not compose");
        }
        if (yodHiriq.size() != 1 || yodHiriq.getFirst().codePoint() != 0xFB1D || yodHiriq.getFirst().glyphId() <= 0) {
            throw new IllegalStateException("Hebrew yod-hiriq did not compose");
        }
        if (alefPatah.size() != 1 || alefPatah.getFirst().codePoint() != 0xFB2E || alefPatah.getFirst().glyphId() <= 0) {
            throw new IllegalStateException("Hebrew alef-patah did not compose");
        }
        List<ShapedGlyph> betRafe = DefaultShaper.shape(font, "\u05D1\u05BF");
        if (betRafe.size() != 1 || betRafe.getFirst().codePoint() != 0xFB4C || betRafe.getFirst().glyphId() <= 0) {
            throw new IllegalStateException("Hebrew bet-rafe did not compose");
        }
        List<ShapedGlyph> alefLamed = DefaultShaper.shape(font, "\u05D0\u05DC");
        if (alefLamed.size() != 1 || alefLamed.getFirst().codePoint() != 0xFB4F || alefLamed.getFirst().glyphId() <= 0) {
            throw new IllegalStateException("Hebrew alef-lamed did not compose");
        }
        List<ShapedGlyph> yodYodPatah = DefaultShaper.shape(font, "\u05D9\u05D9\u05B7");
        if (yodYodPatah.size() != 1
                || yodYodPatah.getFirst().codePoint() != 0xFB1F
                || yodYodPatah.getFirst().glyphId() <= 0) {
            throw new IllegalStateException("Hebrew yod-yod-patah did not compose");
        }
        if (hangulLv.size() != 1 || hangulLv.getFirst().codePoint() != 0xAC00 || hangulLv.getFirst().glyphId() <= 0) {
            throw new IllegalStateException("Hangul LV jamo did not compose");
        }
        if (hangulLvt.size() != 1 || hangulLvt.getFirst().codePoint() != 0xAC01) {
            throw new IllegalStateException("Hangul LVT jamo did not compose");
        }
        if (thaiAm.size() != 4
                || thaiAm.get(0).codePoint() != 0x0E14
                || thaiAm.get(1).codePoint() != 0x0E4D
                || thaiAm.get(2).codePoint() != 0x0E4B
                || thaiAm.get(3).codePoint() != 0x0E32
                || thaiAm.get(1).cluster() != 0) {
            throw new IllegalStateException("Thai SARA AM did not decompose and reorder Nikhahit");
        }
        if (thaiLeft.size() != 2
                || thaiLeft.get(0).codePoint() != 0x0E40
                || thaiLeft.get(1).codePoint() != 0x0E01) {
            throw new IllegalStateException("Thai left vowel did not stay in visual order");
        }
        if (laoAm.size() != 3
                || laoAm.get(0).codePoint() != 0x0E81
                || laoAm.get(1).codePoint() != 0x0ECD
                || laoAm.get(2).codePoint() != 0x0EB2) {
            throw new IllegalStateException("Lao SARA AM did not decompose");
        }
        List<ShapedGlyph> finalKaf = DefaultShaper.shape(font, "\u05D0\u05DB");
        if (finalKaf.size() != 2 || finalKaf.get(1).codePoint() != 0x05DA) {
            throw new IllegalStateException("Hebrew final kaf was not selected");
        }
        List<ShapedGlyph> yiddish = DefaultShaper.shape(font, "\u05D5\u05D5");
        if (yiddish.size() != 1 || yiddish.getFirst().codePoint() != 0x05F0) {
            throw new IllegalStateException("Yiddish double vav did not compose");
        }
        List<ShapedGlyph> hangulMissing = DefaultShaper.shape(font, "\uAC04");
        if (hangulMissing.size() != 3
                || hangulMissing.get(0).codePoint() != 0x1100
                || hangulMissing.get(1).codePoint() != 0x1161
                || hangulMissing.get(2).codePoint() != 0x11AB) {
            throw new IllegalStateException("Missing Hangul syllable did not decompose");
        }
        List<ShapedGlyph> laoHo = DefaultShaper.shape(font, "\u0EAB\u0E99");
        if (laoHo.size() != 1 || laoHo.getFirst().codePoint() != 0x0EDC) {
            throw new IllegalStateException("Lao ho-no did not compose");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m8-shape",
                          "workPackage": "SHAPE-ARABIC-001",
                          "status": "passed",
                          "arabicIsolated": %d,
                          "arabicInitial": %d,
                          "arabicMedial": %d,
                          "arabicFinal": %d,
                          "arabicMarkCluster": %d,
                          "hebrewUnmarked": %d,
                          "hebrewComposed": %d,
                          "hangulLv": %d,
                          "hangulLvt": %d,
                          "thaiNikhahit": %d,
                          "thaiLeftVowel": %d,
                          "laoNikhahit": %d
                        }
                        """.formatted(
                        isolated.getFirst().codePoint(),
                        triple.get(0).codePoint(),
                        triple.get(1).codePoint(),
                        triple.get(2).codePoint(),
                        marked.get(1).cluster(),
                        hebrew.size(),
                        dagesh.getFirst().codePoint(),
                        hangulLv.getFirst().codePoint(),
                        hangulLvt.getFirst().codePoint(),
                        thaiAm.get(1).codePoint(),
                        thaiLeft.get(0).codePoint(),
                        laoAm.get(1).codePoint()
                ),
                StandardCharsets.UTF_8
        );
    }
}
