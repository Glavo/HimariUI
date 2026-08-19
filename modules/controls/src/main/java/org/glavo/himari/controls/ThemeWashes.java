package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Stores named wash colors that do not fit in [`ThemeTokens`], [`ThemeSurfaces`],
/// or [`ThemeOverlays`].
///
/// Those earlier records are at the JVM constructor limit, so later first-stable
/// named colors live here and encode as a separate theme payload.
///
/// @param name the wash-pack name
/// @param senmurvaryArgb the senmurvary wash color
/// @param simurgharyArgb the simurghary wash color
/// @param sphinxaryArgb the sphinxary wash color
/// @param chimeraaryArgb the chimeraary wash color
/// @param hydraaryArgb the hydraary wash color
/// @param krakenaryArgb the krakenary wash color
/// @param leviathanaryArgb the leviathanary wash color
/// @param behemotharyArgb the behemothary wash color
/// @param hippogriffaryArgb the hippogriffary wash color
/// @param manticorearyArgb the manticoreary wash color
/// @param amphipteraryArgb the amphipterary wash color
/// @param yalehoundousArgb the yalehoundous wash color
/// @param senmurvousArgb the senmurvous wash color
/// @param simurghousArgb the simurghous wash color
/// @param sphinxousArgb the sphinxous wash color
/// @param chimeraousArgb the chimeraous wash color
/// @param hydraousArgb the hydraous wash color
/// @param krakenousArgb the krakenous wash color
/// @param leviathanousArgb the leviathanous wash color
/// @param behemothousArgb the behemothous wash color
/// @param hippogriffousArgb the hippogriffous wash color
/// @param manticoreousArgb the manticoreous wash color
/// @param amphipterousArgb the amphipterous wash color
/// @param yalehoundiveArgb the yalehoundive wash color
/// @param senmurviveArgb the senmurvive wash color
/// @param simurghiveArgb the simurghive wash color
/// @param sphinxiveArgb the sphinxive wash color
/// @param chimeraiveArgb the chimeraive wash color
/// @param hydraiveArgb the hydraive wash color
/// @param krakeniveArgb the krakenive wash color
/// @param leviathaniveArgb the leviathanive wash color
/// @param behemothiveArgb the behemothive wash color
/// @param hippogriffiveArgb the hippogriffive wash color
/// @param manticoreiveArgb the manticoreive wash color
/// @param amphipteriveArgb the amphipterive wash color
/// @param yalehoundoryArgb the yalehoundory wash color
/// @param senmurvoryArgb the senmurvory wash color
/// @param simurghoryArgb the simurghory wash color
/// @param sphinxoryArgb the sphinxory wash color
/// @param chimeraoryArgb the chimeraory wash color
/// @param hydraoryArgb the hydraory wash color
/// @param krakenoryArgb the krakenory wash color
/// @param leviathanoryArgb the leviathanory wash color
/// @param behemothoryArgb the behemothory wash color
/// @param hippogrifforyArgb the hippogriffory wash color
/// @param manticoreoryArgb the manticoreory wash color
/// @param amphipteroryArgb the amphipterory wash color
/// @param yalehoundantArgb the yalehoundant wash color
/// @param senmurvantArgb the senmurvant wash color
/// @param simurghantArgb the simurghant wash color
/// @param sphinxantArgb the sphinxant wash color
/// @param chimeraantArgb the chimeraant wash color
/// @param hydraantArgb the hydraant wash color
/// @param krakenialArgb the krakenial wash color
/// @param leviathanialArgb the leviathanial wash color
/// @param behemothantArgb the behemothant wash color
/// @param hippogriffantArgb the hippogriffant wash color
/// @param manticoreantArgb the manticoreant wash color
/// @param amphipterantArgb the amphipterant wash color
/// @param yalehoundentArgb the yalehoundent wash color
/// @param senmurventArgb the senmurvent wash color
/// @param simurghentArgb the simurghent wash color
/// @param sphinxentArgb the sphinxent wash color
/// @param chimeraentArgb the chimeraent wash color
/// @param hydraentArgb the hydraent wash color
/// @param krakenentArgb the krakenent wash color
/// @param leviathanentArgb the leviathanent wash color
/// @param behemothentArgb the behemothent wash color
/// @param hippogriffentArgb the hippogriffent wash color
/// @param manticoreentArgb the manticoreent wash color
/// @param amphipterentArgb the amphipterent wash color
/// @param yalehoundistArgb the yalehoundist wash color
/// @param senmurvistArgb the senmurvist wash color
/// @param simurghistArgb the simurghist wash color
/// @param sphinxistArgb the sphinxist wash color
/// @param chimeraistArgb the chimeraist wash color
/// @param hydraistArgb the hydraist wash color
/// @param krakenistArgb the krakenist wash color
/// @param leviathanistArgb the leviathanist wash color
/// @param behemothistArgb the behemothist wash color
/// @param hippogriffistArgb the hippogriffist wash color
/// @param manticoreistArgb the manticoreist wash color
/// @param amphipteristArgb the amphipterist wash color
/// @param yalehounditeArgb the yalehoundite wash color
/// @param senmurviteArgb the senmurvite wash color
/// @param simurghiteArgb the simurghite wash color
/// @param sphinxiteArgb the sphinxite wash color
/// @param chimeraiteArgb the chimeraite wash color
/// @param hydraiteArgb the hydraite wash color
/// @param krakeniteArgb the krakenite wash color
/// @param leviathaniteArgb the leviathanite wash color
/// @param behemothiteArgb the behemothite wash color
/// @param hippogriffiteArgb the hippogriffite wash color
/// @param manticoreiteArgb the manticoreite wash color
/// @param amphipteriteArgb the amphipterite wash color
/// @param yalehoundfulArgb the yalehoundful wash color
/// @param senmurvfulArgb the senmurvful wash color
/// @param simurghfulArgb the simurghful wash color
/// @param sphinxfulArgb the sphinxful wash color
/// @param chimerfulArgb the chimerful wash color
/// @param hydrafulArgb the hydraful wash color
/// @param krakenfulArgb the krakenful wash color
/// @param leviathanfulArgb the leviathanful wash color
/// @param behemothfulArgb the behemothful wash color
/// @param hippogrifulArgb the hippogriful wash color
/// @param manticorefulArgb the manticoreful wash color
/// @param amphipterfulArgb the amphipterful wash color
/// @param yalehoundoseArgb the yalehoundose wash color
/// @param senmurvoseArgb the senmurvose wash color
/// @param simurghoseArgb the simurghose wash color
/// @param sphinxoseArgb the sphinxose wash color
/// @param chimeraoseArgb the chimeraose wash color
/// @param hydraoseArgb the hydraose wash color
/// @param krakenoseArgb the krakenose wash color
/// @param leviathanoseArgb the leviathanose wash color
/// @param behemothoseArgb the behemothose wash color
/// @param hippogriffoseArgb the hippogriffose wash color
/// @param manticoreoseArgb the manticoreose wash color
/// @param amphipteroseArgb the amphipterose wash color
/// @param yalehoundualArgb the yalehoundual wash color
/// @param senmurvualArgb the senmurvual wash color
/// @param simurghualArgb the simurghual wash color
/// @param sphinxualArgb the sphinxual wash color
/// @param chimeraualArgb the chimeraual wash color
/// @param hydraualArgb the hydraual wash color
/// @param krakenualArgb the krakenual wash color
/// @param leviathanualArgb the leviathanual wash color
/// @param behemothualArgb the behemothual wash color
/// @param hippogriffualArgb the hippogriffual wash color
/// @param manticoreualArgb the manticoreual wash color
/// @param amphipterualArgb the amphipterual wash color
/// @param yalehoundileArgb the yalehoundile wash color
/// @param senmurvileArgb the senmurvile wash color
/// @param simurghileArgb the simurghile wash color
/// @param sphinxileArgb the sphinxile wash color
/// @param chimeraileArgb the chimeraile wash color
/// @param hydraileArgb the hydraile wash color
/// @param krakenileArgb the krakenile wash color
/// @param leviathanileArgb the leviathanile wash color
/// @param behemothileArgb the behemothile wash color
/// @param hippogriffileArgb the hippogriffile wash color
/// @param manticoreileArgb the manticoreile wash color
/// @param amphipterileArgb the amphipterile wash color
@NotNullByDefault
public record ThemeWashes(
        String name,
        int senmurvaryArgb,
        int simurgharyArgb,
        int sphinxaryArgb,
        int chimeraaryArgb,
        int hydraaryArgb,
        int krakenaryArgb,
        int leviathanaryArgb,
        int behemotharyArgb,
        int hippogriffaryArgb,
        int manticorearyArgb,
        int amphipteraryArgb,
        int yalehoundousArgb,
        int senmurvousArgb,
        int simurghousArgb,
        int sphinxousArgb,
        int chimeraousArgb,
        int hydraousArgb,
        int krakenousArgb,
        int leviathanousArgb,
        int behemothousArgb,
        int hippogriffousArgb,
        int manticoreousArgb,
        int amphipterousArgb,
        int yalehoundiveArgb,
        int senmurviveArgb,
        int simurghiveArgb,
        int sphinxiveArgb,
        int chimeraiveArgb,
        int hydraiveArgb,
        int krakeniveArgb,
        int leviathaniveArgb,
        int behemothiveArgb,
        int hippogriffiveArgb,
        int manticoreiveArgb,
        int amphipteriveArgb,
        int yalehoundoryArgb,
        int senmurvoryArgb,
        int simurghoryArgb,
        int sphinxoryArgb,
        int chimeraoryArgb,
        int hydraoryArgb,
        int krakenoryArgb,
        int leviathanoryArgb,
        int behemothoryArgb,
        int hippogrifforyArgb,
        int manticoreoryArgb,
        int amphipteroryArgb,
        int yalehoundantArgb,
        int senmurvantArgb,
        int simurghantArgb,
        int sphinxantArgb,
        int chimeraantArgb,
        int hydraantArgb,
        int krakenialArgb,
        int leviathanialArgb,
        int behemothantArgb,
        int hippogriffantArgb,
        int manticoreantArgb,
        int amphipterantArgb,
        int yalehoundentArgb,
        int senmurventArgb,
        int simurghentArgb,
        int sphinxentArgb,
        int chimeraentArgb,
        int hydraentArgb,
        int krakenentArgb,
        int leviathanentArgb,
        int behemothentArgb,
        int hippogriffentArgb,
        int manticoreentArgb,
        int amphipterentArgb,
        int yalehoundistArgb,
        int senmurvistArgb,
        int simurghistArgb,
        int sphinxistArgb,
        int chimeraistArgb,
        int hydraistArgb,
        int krakenistArgb,
        int leviathanistArgb,
        int behemothistArgb,
        int hippogriffistArgb,
        int manticoreistArgb,
        int amphipteristArgb,
        int yalehounditeArgb,
        int senmurviteArgb,
        int simurghiteArgb,
        int sphinxiteArgb,
        int chimeraiteArgb,
        int hydraiteArgb,
        int krakeniteArgb,
        int leviathaniteArgb,
        int behemothiteArgb,
        int hippogriffiteArgb,
        int manticoreiteArgb,
        int amphipteriteArgb,
        int yalehoundfulArgb,
        int senmurvfulArgb,
        int simurghfulArgb,
        int sphinxfulArgb,
        int chimerfulArgb,
        int hydrafulArgb,
        int krakenfulArgb,
        int leviathanfulArgb,
        int behemothfulArgb,
        int hippogrifulArgb,
        int manticorefulArgb,
        int amphipterfulArgb,
        int yalehoundoseArgb,
        int senmurvoseArgb,
        int simurghoseArgb,
        int sphinxoseArgb,
        int chimeraoseArgb,
        int hydraoseArgb,
        int krakenoseArgb,
        int leviathanoseArgb,
        int behemothoseArgb,
        int hippogriffoseArgb,
        int manticoreoseArgb,
        int amphipteroseArgb,
        int yalehoundualArgb,
        int senmurvualArgb,
        int simurghualArgb,
        int sphinxualArgb,
        int chimeraualArgb,
        int hydraualArgb,
        int krakenualArgb,
        int leviathanualArgb,
        int behemothualArgb,
        int hippogriffualArgb,
        int manticoreualArgb,
        int amphipterualArgb,
        int yalehoundileArgb,
        int senmurvileArgb,
        int simurghileArgb,
        int sphinxileArgb,
        int chimeraileArgb,
        int hydraileArgb,
        int krakenileArgb,
        int leviathanileArgb,
        int behemothileArgb,
        int hippogriffileArgb,
        int manticoreileArgb,
        int amphipterileArgb
) {
    /// Validates the washes.
    public ThemeWashes {
        Objects.requireNonNull(name, "name");
    }

    /// Returns the standard extra washes.
    ///
    /// @return the washes
    public static ThemeWashes standard() {
        return new ThemeWashes(
                "standard",
                0xFFCCDDCC,
                0xFFDDEEDD,
                0xFFEEFFEE,
                0xFF221122,
                0xFF332233,
                0xFF443344,
                0xFF554455,
                0xFF665566,
                0xFF776677,
                0xFF887788,
                0xFF998899,
                0xFFAA99AA,
                0xFFBBAABB,
                0xFFCCBBCC,
                0xFFDDCCDD,
                0xFFEEDDEE,
                0xFF001100,
                0xFF002200,
                0xFF003300,
                0xFF004400,
                0xFF005500,
                0xFF006600,
                0xFF007700,
                0xFF008800,
                0xFF009900,
                0xFF00AA00,
                0xFF00BB00,
                0xFF00CC00,
                0xFF00DD00,
                0xFF00EE00,
                0xFF110000,
                0xFF220000,
                0xFF330000,
                0xFF440000,
                0xFF550000,
                0xFF660000,
                0xFF770000,
                0xFF880000,
                0xFF990000,
                0xFFAA0000,
                0xFFBB0000,
                0xFFCC0000,
                0xFFDD0000,
                0xFFEE0000,
                0xFF001111,
                0xFF002222,
                0xFF003333,
                0xFF004444,
                0xFF005555,
                0xFF006666,
                0xFF007777,
                0xFF008888,
                0xFF009999,
                0xFF00AAAA,
                0xFF00BBBB,
                0xFF00CCCC,
                0xFF00DDDD,
                0xFF00EEEE,
                0xFF111100,
                0xFF222200,
                0xFF333300,
                0xFF444400,
                0xFF555500,
                0xFF666600,
                0xFF777700,
                0xFF888800,
                0xFF999900,
                0xFFAAAA00,
                0xFFBBBB00,
                0xFFCCCC00,
                0xFFDDDD00,
                0xFFEEEE00,
                0xFF000011,
                0xFF000022,
                0xFF000033,
                0xFF000044,
                0xFF000055,
                0xFF000066,
                0xFF000077,
                0xFF000088,
                0xFF000099,
                0xFF0000AA,
                0xFF0000BB,
                0xFF0000CC,
                0xFF0000DD,
                0xFF0000EE,
                0xFF0000FF,
                0xFF111111,
                0xFF222222,
                0xFF333333,
                0xFF444444,
                0xFF555555,
                0xFF666666,
                0xFF777777,
                0xFF888888,
                0xFF999999,
                0xFFAAAAAA,
                0xFFBBBBBB,
                0xFFCCCCCC,
                0xFFDDDDDD,
                0xFF112200,
                0xFF223300,
                0xFF334400,
                0xFF445500,
                0xFF556600,
                0xFF667700,
                0xFF778800,
                0xFF000000,
                0xFF000011,
                0xFF000022,
                0xFF000033,
                0xFF000044,
                0xFF000055,
                0xFF000066,
                0xFF000077,
                0xFF000088,
                0xFF000099,
                0xFF0000AA,
                0xFF0000BB,
                0xFF010203,
                0xFF04070A,
                0xFF070C11,
                0xFF0A1118,
                0xFF0D161F,
                0xFF101B26,
                0xFF13202D,
                0xFF162534,
                0xFF192A3B,
                0xFF1C2F42,
                0xFF1F3449,
                0xFF223950,
                0xFF010203,
                0xFF04070A,
                0xFF070C11,
                0xFF0A1118,
                0xFF0D161F,
                0xFF101B26,
                0xFF13202D,
                0xFF162534,
                0xFF192A3B,
                0xFF1C2F42,
                0xFF1F3449,
                0xFF223950
        );
    }

    /// Returns the high-contrast extra washes.
    ///
    /// @return the washes
    public static ThemeWashes highContrastTheme() {
        return new ThemeWashes(
                "high-contrast",
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
                0xFF00FF00
        );
    }

    /// Encodes these washes as a UTF-8 pipe-separated payload.
    ///
    /// @return the payload bytes
    public byte[] encode() {
        return (name
                + '|'
                + Integer.toUnsignedString(senmurvaryArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurgharyArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxaryArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraaryArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraaryArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenaryArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanaryArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemotharyArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffaryArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticorearyArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteraryArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundousArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvousArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghousArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxousArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraousArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraousArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenousArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanousArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothousArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffousArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreousArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterousArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundiveArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurviveArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghiveArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxiveArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraiveArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraiveArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakeniveArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathaniveArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothiveArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffiveArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreiveArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteriveArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundoryArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvoryArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghoryArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxoryArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraoryArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraoryArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenoryArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanoryArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothoryArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogrifforyArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreoryArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteroryArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundantArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvantArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghantArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxantArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraantArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraantArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenialArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanialArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothantArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffantArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreantArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterantArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundentArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurventArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghentArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxentArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraentArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraentArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenentArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanentArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothentArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffentArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreentArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterentArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundistArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvistArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghistArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxistArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraistArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraistArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenistArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanistArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothistArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffistArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreistArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteristArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehounditeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurviteArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghiteArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxiteArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraiteArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraiteArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakeniteArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathaniteArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothiteArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffiteArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreiteArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteriteArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundfulArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvfulArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghfulArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxfulArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerfulArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydrafulArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenfulArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanfulArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothfulArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogrifulArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticorefulArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterfulArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundoseArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvoseArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghoseArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxoseArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraoseArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraoseArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenoseArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanoseArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothoseArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffoseArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreoseArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteroseArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundualArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvualArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghualArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxualArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraualArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraualArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenualArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanualArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothualArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffualArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreualArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterualArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundileArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvileArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghileArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxileArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraileArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraileArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenileArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanileArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothileArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffileArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreileArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterileArgb, 16)).getBytes(StandardCharsets.UTF_8);
    }

    /// Decodes a payload produced by [`#encode()`].
    ///
    /// @param bytes the resource bytes
    /// @return the washes
    public static ThemeWashes decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        String[] fields = new String(bytes, StandardCharsets.UTF_8).split("\\|", -1);
        if (fields.length != 144) {
            throw new IllegalArgumentException("Theme washes must have one-hundred-forty-four fields");
        }
        return new ThemeWashes(
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
                parseArgb(fields[46]),
                parseArgb(fields[47]),
                parseArgb(fields[48]),
                parseArgb(fields[49]),
                parseArgb(fields[50]),
                parseArgb(fields[51]),
                parseArgb(fields[52]),
                parseArgb(fields[53]),
                parseArgb(fields[54]),
                parseArgb(fields[55]),
                parseArgb(fields[56]),
                parseArgb(fields[57]),
                parseArgb(fields[58]),
                parseArgb(fields[59]),
                parseArgb(fields[60]),
                parseArgb(fields[61]),
                parseArgb(fields[62]),
                parseArgb(fields[63]),
                parseArgb(fields[64]),
                parseArgb(fields[65]),
                parseArgb(fields[66]),
                parseArgb(fields[67]),
                parseArgb(fields[68]),
                parseArgb(fields[69]),
                parseArgb(fields[70]),
                parseArgb(fields[71]),
                parseArgb(fields[72]),
                parseArgb(fields[73]),
                parseArgb(fields[74]),
                parseArgb(fields[75]),
                parseArgb(fields[76]),
                parseArgb(fields[77]),
                parseArgb(fields[78]),
                parseArgb(fields[79]),
                parseArgb(fields[80]),
                parseArgb(fields[81]),
                parseArgb(fields[82]),
                parseArgb(fields[83]),
                parseArgb(fields[84]),
                parseArgb(fields[85]),
                parseArgb(fields[86]),
                parseArgb(fields[87]),
                parseArgb(fields[88]),
                parseArgb(fields[89]),
                parseArgb(fields[90]),
                parseArgb(fields[91]),
                parseArgb(fields[92]),
                parseArgb(fields[93]),
                parseArgb(fields[94]),
                parseArgb(fields[95]),
                parseArgb(fields[96]),
                parseArgb(fields[97]),
                parseArgb(fields[98]),
                parseArgb(fields[99]),
                parseArgb(fields[100]),
                parseArgb(fields[101]),
                parseArgb(fields[102]),
                parseArgb(fields[103]),
                parseArgb(fields[104]),
                parseArgb(fields[105]),
                parseArgb(fields[106]),
                parseArgb(fields[107]),
                parseArgb(fields[108]),
                parseArgb(fields[109]),
                parseArgb(fields[110]),
                parseArgb(fields[111]),
                parseArgb(fields[112]),
                parseArgb(fields[113]),
                parseArgb(fields[114]),
                parseArgb(fields[115]),
                parseArgb(fields[116]),
                parseArgb(fields[117]),
                parseArgb(fields[118]),
                parseArgb(fields[119]),
                parseArgb(fields[120]),
                parseArgb(fields[121]),
                parseArgb(fields[122]),
                parseArgb(fields[123]),
                parseArgb(fields[124]),
                parseArgb(fields[125]),
                parseArgb(fields[126]),
                parseArgb(fields[127]),
                parseArgb(fields[128]),
                parseArgb(fields[129]),
                parseArgb(fields[130]),
                parseArgb(fields[131]),
                parseArgb(fields[132]),
                parseArgb(fields[133]),
                parseArgb(fields[134]),
                parseArgb(fields[135]),
                parseArgb(fields[136]),
                parseArgb(fields[137]),
                parseArgb(fields[138]),
                parseArgb(fields[139]),
                parseArgb(fields[140]),
                parseArgb(fields[141]),
                parseArgb(fields[142]),
                parseArgb(fields[143])
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
