package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Stores named film colors that do not fit in [`ThemeTokens`], [`ThemeSurfaces`],
/// [`ThemeOverlays`], [`ThemeWashes`], [`ThemeGlazes`], or [`ThemeSheens`].
///
/// Those earlier records are at the JVM constructor limit, so later first-stable
/// named colors live here and encode as a separate theme payload.
///
/// @param name the film-pack name
/// @param behemothoalArgb the behemothoal film color
/// @param hippogriffoalArgb the hippogriffoal film color
/// @param manticoreoalArgb the manticoreoal film color
/// @param amphipteroalArgb the amphipteroal film color
/// @param yalehoundialArgb the yalehoundial film color
/// @param senmurvialArgb the senmurvial film color
/// @param simurghialArgb the simurghial film color
/// @param sphinxialArgb the sphinxial film color
/// @param chimeraialArgb the chimeraial film color
/// @param hydraialArgb the hydraial film color
/// @param behemothialArgb the behemothial film color
/// @param hippogriffialArgb the hippogriffial film color
/// @param manticoreialArgb the manticoreial film color
/// @param amphipterialArgb the amphipterial film color
/// @param yalehoundulfArgb the yalehoundulf film color
/// @param senmurvulfArgb the senmurvulf film color
/// @param simurghulfArgb the simurghulf film color
/// @param sphinxulfArgb the sphinxulf film color
/// @param chimeraulfArgb the chimeraulf film color
/// @param hydraulfArgb the hydraulf film color
/// @param krakenulfArgb the krakenulf film color
/// @param leviathanulfArgb the leviathanulf film color
/// @param behemothulfArgb the behemothulf film color
/// @param hippogriffulfArgb the hippogriffulf film color
/// @param manticoreulfArgb the manticoreulf film color
/// @param amphipterulfArgb the amphipterulf film color
/// @param yalehoundolfArgb the yalehoundolf film color
/// @param senmurvolfArgb the senmurvolf film color
/// @param simurgholfArgb the simurgholf film color
/// @param sphinxolfArgb the sphinxolf film color
/// @param chimeraolfArgb the chimeraolf film color
/// @param hydraolfArgb the hydraolf film color
/// @param krakenolfArgb the krakenolf film color
/// @param leviathanolfArgb the leviathanolf film color
/// @param behemotholfArgb the behemotholf film color
/// @param hippogriffolfArgb the hippogriffolf film color
/// @param manticoreolfArgb the manticoreolf film color
/// @param amphipterolfArgb the amphipterolf film color
/// @param yalehoundelfArgb the yalehoundelf film color
/// @param senmurvelfArgb the senmurvelf film color
/// @param simurghelfArgb the simurghelf film color
/// @param sphinxelfArgb the sphinxelf film color
/// @param chimeraelfArgb the chimeraelf film color
/// @param hydraelfArgb the hydraelf film color
/// @param krakenelfArgb the krakenelf film color
/// @param leviathanelfArgb the leviathanelf film color
@NotNullByDefault
public record ThemeFilms(
        String name,
        int behemothoalArgb,
        int hippogriffoalArgb,
        int manticoreoalArgb,
        int amphipteroalArgb,
        int yalehoundialArgb,
        int senmurvialArgb,
        int simurghialArgb,
        int sphinxialArgb,
        int chimeraialArgb,
        int hydraialArgb,
        int behemothialArgb,
        int hippogriffialArgb,
        int manticoreialArgb,
        int amphipterialArgb,
        int yalehoundulfArgb,
        int senmurvulfArgb,
        int simurghulfArgb,
        int sphinxulfArgb,
        int chimeraulfArgb,
        int hydraulfArgb,
        int krakenulfArgb,
        int leviathanulfArgb,
        int behemothulfArgb,
        int hippogriffulfArgb,
        int manticoreulfArgb,
        int amphipterulfArgb,
        int yalehoundolfArgb,
        int senmurvolfArgb,
        int simurgholfArgb,
        int sphinxolfArgb,
        int chimeraolfArgb,
        int hydraolfArgb,
        int krakenolfArgb,
        int leviathanolfArgb,
        int behemotholfArgb,
        int hippogriffolfArgb,
        int manticoreolfArgb,
        int amphipterolfArgb,
        int yalehoundelfArgb,
        int senmurvelfArgb,
        int simurghelfArgb,
        int sphinxelfArgb,
        int chimeraelfArgb,
        int hydraelfArgb,
        int krakenelfArgb,
        int leviathanelfArgb
) {
    /// Validates the films.
    public ThemeFilms {
        Objects.requireNonNull(name, "name");
    }

    /// Returns the standard extra films.
    ///
    /// @return the films
    public static ThemeFilms standard() {
        return new ThemeFilms(
                "standard",
                0xFF79C713,
                0xFF7CCC1A,
                0xFF7FD121,
                0xFF82D628,
                0xFF85DB2F,
                0xFF88E036,
                0xFF8BE53D,
                0xFF8EEA44,
                0xFF91EF4B,
                0xFF94F452,
                0xFF97F959,
                0xFF9AFE60,
                0xFF9E0367,
                0xFFA1086E,
                0xFFA40D75,
                0xFFA7127C,
                0xFFAA1783,
                0xFFAD1C8A,
                0xFFB02191,
                0xFFB32698,
                0xFFB62B9F,
                0xFFB930A6,
                0xFFBC35AD,
                0xFFBF3AB4,
                0xFFC23FBB,
                0xFFC544C2,
                0xFFC849C9,
                0xFFCB4ED0,
                0xFFCE53D7,
                0xFFD158DE,
                0xFFD45DE5,
                0xFFD762EC,
                0xFFDA67F3,
                0xFFDD6CFA,
                0xFFE07101,
                0xFFE37608,
                0xFFE67B0F,
                0xFFE98016,
                0xFFEC851D,
                0xFFEF8A24,
                0xFFF28F2B,
                0xFFF59432,
                0xFFF89939,
                0xFFFB9E40,
                0xFFFEA347,
                0xFF08090A
        );
    }

    /// Returns the high-contrast extra films.
    ///
    /// @return the films
    public static ThemeFilms highContrastTheme() {
        return new ThemeFilms(
                "high-contrast",
                0xFF808080,
                0xFF00FF00,
                0xFFFFFF00,
                0xFF000000,
                0xFFFFFFFF,
                0xFF00FFFF,
                0xFF808080,
                0xFF00FF00,
                0xFFFFFF00,
                0xFF000000,
                0xFFFFFFFF,
                0xFF00FFFF,
                0xFF808080,
                0xFF00FF00,
                0xFFFFFF00,
                0xFF000000,
                0xFFFFFFFF,
                0xFF00FFFF,
                0xFF808080,
                0xFF00FF00,
                0xFFFFFF00,
                0xFF000000,
                0xFFFFFFFF,
                0xFF00FFFF,
                0xFF808080,
                0xFF00FF00,
                0xFFFFFF00,
                0xFF000000,
                0xFFFFFFFF,
                0xFF00FFFF,
                0xFF808080,
                0xFF00FF00,
                0xFFFFFF00,
                0xFF000000,
                0xFFFFFFFF,
                0xFF00FFFF,
                0xFF808080,
                0xFF00FF00,
                0xFFFFFF00,
                0xFF000000,
                0xFFFFFFFF,
                0xFF00FFFF,
                0xFF808080,
                0xFF00FF00,
                0xFFFFFF00,
                0xFF000000
        );
    }

    /// Encodes these films as a UTF-8 pipe-separated payload.
    ///
    /// @return the payload bytes
    public byte[] encode() {
        return (name
                + '|'
                + Integer.toUnsignedString(behemothoalArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffoalArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreoalArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteroalArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundialArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvialArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghialArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxialArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraialArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraialArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothialArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffialArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreialArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterialArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundolfArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvolfArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurgholfArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxolfArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraolfArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraolfArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenolfArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanolfArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemotholfArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffolfArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreolfArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterolfArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundelfArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvelfArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghelfArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxelfArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraelfArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraelfArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenelfArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanelfArgb, 16)).getBytes(StandardCharsets.UTF_8);
    }

    /// Decodes a payload produced by [`#encode()`].
    ///
    /// @param bytes the resource bytes
    /// @return the films
    public static ThemeFilms decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        String[] fields = new String(bytes, StandardCharsets.UTF_8).split("\\|", -1);
        if (fields.length != 47) {
            throw new IllegalArgumentException("Theme films must have forty-seven fields");
        }
        return new ThemeFilms(
                fields[0],
                parseArgb(fields[1]),
                parseArgb(fields[2]),
                parseArgb(fields[3]),
                parseArgb(fields[4]),
                parseArgb(fields[5]),
                parseArgb(fields[6]),
                parseArgb(fields[7]),
                parseArgb(fields[8]),
                parseArgb(fields[9]),
                parseArgb(fields[10]),
                parseArgb(fields[11]),
                parseArgb(fields[12]),
                parseArgb(fields[13]),
                parseArgb(fields[14]),
                parseArgb(fields[15]),
                parseArgb(fields[16]),
                parseArgb(fields[17]),
                parseArgb(fields[18]),
                parseArgb(fields[19]),
                parseArgb(fields[20]),
                parseArgb(fields[21]),
                parseArgb(fields[22]),
                parseArgb(fields[23]),
                parseArgb(fields[24]),
                parseArgb(fields[25]),
                parseArgb(fields[26]),
                parseArgb(fields[27]),
                parseArgb(fields[28]),
                parseArgb(fields[29]),
                parseArgb(fields[30]),
                parseArgb(fields[31]),
                parseArgb(fields[32]),
                parseArgb(fields[33]),
                parseArgb(fields[34]),
                parseArgb(fields[35]),
                parseArgb(fields[36]),
                parseArgb(fields[37]),
                parseArgb(fields[38]),
                parseArgb(fields[39]),
                parseArgb(fields[40]),
                parseArgb(fields[41]),
                parseArgb(fields[42]),
                parseArgb(fields[43]),
                parseArgb(fields[44]),
                parseArgb(fields[45]),
                parseArgb(fields[46])
        );
    }

    /// Parses one unsigned hex ARGB field.
    ///
    /// @param field the hex digits
    /// @return the ARGB word
    private static int parseArgb(String field) {
        try {
            return Integer.parseUnsignedInt(field, 16);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme ARGB field is not hexadecimal", failure);
        }
    }
}
