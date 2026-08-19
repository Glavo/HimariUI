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
/// @param yalehoundadeArgb the yalehoundade wash color
/// @param senmurvadeArgb the senmurvade wash color
/// @param simurghadeArgb the simurghade wash color
/// @param sphinxadeArgb the sphinxade wash color
/// @param chimeraadeArgb the chimeraade wash color
/// @param hydraadeArgb the hydraade wash color
/// @param krakenadeArgb the krakenade wash color
/// @param leviathanadeArgb the leviathanade wash color
/// @param behemothadeArgb the behemothade wash color
/// @param hippogriffadeArgb the hippogriffade wash color
/// @param manticoreadeArgb the manticoreade wash color
/// @param amphipteradeArgb the amphipterade wash color
/// @param yalehoundureArgb the yalehoundure wash color
/// @param senmurvureArgb the senmurvure wash color
/// @param simurghureArgb the simurghure wash color
/// @param sphinxureArgb the sphinxure wash color
/// @param chimeraureArgb the chimeraure wash color
/// @param hydraureArgb the hydraure wash color
/// @param krakenureArgb the krakenure wash color
/// @param leviathanureArgb the leviathanure wash color
/// @param behemothureArgb the behemothure wash color
/// @param hippogriffureArgb the hippogriffure wash color
/// @param manticoreureArgb the manticoreure wash color
/// @param amphipterureArgb the amphipterure wash color
/// @param yalehoundiceArgb the yalehoundice wash color
/// @param senmurviceArgb the senmurvice wash color
/// @param simurghiceArgb the simurghice wash color
/// @param sphinxiceArgb the sphinxice wash color
/// @param chimeraiceArgb the chimeraice wash color
/// @param hydraiceArgb the hydraice wash color
/// @param krakeniceArgb the krakenice wash color
/// @param leviathaniceArgb the leviathanice wash color
/// @param behemothiceArgb the behemothice wash color
/// @param hippogrifficeArgb the hippogriffice wash color
/// @param manticoreiceArgb the manticoreice wash color
/// @param amphiptericeArgb the amphipterice wash color
/// @param yalehoundomeArgb the yalehoundome wash color
/// @param senmurvomeArgb the senmurvome wash color
/// @param simurghomeArgb the simurghome wash color
/// @param sphinxomeArgb the sphinxome wash color
/// @param chimeraomeArgb the chimeraome wash color
/// @param hydraomeArgb the hydraome wash color
/// @param krakenomeArgb the krakenome wash color
/// @param leviathanomeArgb the leviathanome wash color
/// @param behemothomeArgb the behemothome wash color
/// @param hippogriffomeArgb the hippogriffome wash color
/// @param manticoreomeArgb the manticoreome wash color
/// @param amphipteromeArgb the amphipterome wash color
/// @param yalehoundeanArgb the yalehoundean wash color
/// @param senmurveanArgb the senmurvean wash color
/// @param simurgheanArgb the simurghean wash color
/// @param sphinxeanArgb the sphinxean wash color
/// @param chimeraeanArgb the chimeraean wash color
/// @param hydraeanArgb the hydraean wash color
/// @param krakeneanArgb the krakenean wash color
/// @param leviathaneanArgb the leviathanean wash color
/// @param behemotheanArgb the behemothean wash color
/// @param hippogriffeanArgb the hippogriffean wash color
/// @param manticoreeanArgb the manticoreean wash color
/// @param amphiptereanArgb the amphipterean wash color
/// @param yalehoundeumArgb the yalehoundeum wash color
/// @param senmurveumArgb the senmurveum wash color
/// @param simurgheumArgb the simurgheum wash color
/// @param sphinxeumArgb the sphinxeum wash color
/// @param chimeraeumArgb the chimeraeum wash color
/// @param hydraeumArgb the hydraeum wash color
/// @param krakeneumArgb the krakeneum wash color
/// @param leviathaneumArgb the leviathaneum wash color
/// @param behemotheumArgb the behemotheum wash color
/// @param hippogriffeumArgb the hippogriffeum wash color
/// @param manticoreeumArgb the manticoreeum wash color
/// @param amphiptereumArgb the amphiptereum wash color
/// @param yalehoundiumArgb the yalehoundium wash color
/// @param senmurviumArgb the senmurvium wash color
/// @param simurghiumArgb the simurghium wash color
/// @param sphinxiumArgb the sphinxium wash color
/// @param chimeraiumArgb the chimeraium wash color
/// @param hydraiumArgb the hydraium wash color
/// @param krakeniumArgb the krakenium wash color
/// @param leviathaniumArgb the leviathanium wash color
/// @param behemothiumArgb the behemothium wash color
/// @param hippogriffiumArgb the hippogriffium wash color
/// @param manticoreiumArgb the manticoreium wash color
/// @param amphipteriumArgb the amphipterium wash color
/// @param yalehoundolaArgb the yalehoundola wash color
/// @param senmurvolaArgb the senmurvola wash color
/// @param simurgholaArgb the simurghola wash color
/// @param sphinxolaArgb the sphinxola wash color
/// @param chimeraolaArgb the chimeraola wash color
/// @param hydraolaArgb the hydraola wash color
/// @param krakenolaArgb the krakenola wash color
/// @param leviathanolaArgb the leviathanola wash color
/// @param behemotholaArgb the behemothola wash color
/// @param hippogriffolaArgb the hippogriffola wash color
/// @param manticoreolaArgb the manticoreola wash color
/// @param amphipterolaArgb the amphipterola wash color
/// @param yalehoundulaArgb the yalehoundula wash color
/// @param senmurvulaArgb the senmurvula wash color
/// @param simurghulaArgb the simurghula wash color
/// @param sphinxulaArgb the sphinxula wash color
/// @param chimeraulaArgb the chimeraula wash color
/// @param hydraulaArgb the hydraula wash color
/// @param krakenulaArgb the krakenula wash color
/// @param leviathanulaArgb the leviathanula wash color
/// @param behemothulaArgb the behemothula wash color
/// @param hippogriffulaArgb the hippogriffula wash color
/// @param manticoreulaArgb the manticoreula wash color
/// @param amphipterulaArgb the amphipterula wash color
/// @param yalehoundetaArgb the yalehoundeta wash color
/// @param senmurvetaArgb the senmurveta wash color
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
        int amphipterileArgb,
        int yalehoundadeArgb,
        int senmurvadeArgb,
        int simurghadeArgb,
        int sphinxadeArgb,
        int chimeraadeArgb,
        int hydraadeArgb,
        int krakenadeArgb,
        int leviathanadeArgb,
        int behemothadeArgb,
        int hippogriffadeArgb,
        int manticoreadeArgb,
        int amphipteradeArgb,
        int yalehoundureArgb,
        int senmurvureArgb,
        int simurghureArgb,
        int sphinxureArgb,
        int chimeraureArgb,
        int hydraureArgb,
        int krakenureArgb,
        int leviathanureArgb,
        int behemothureArgb,
        int hippogriffureArgb,
        int manticoreureArgb,
        int amphipterureArgb,
        int yalehoundiceArgb,
        int senmurviceArgb,
        int simurghiceArgb,
        int sphinxiceArgb,
        int chimeraiceArgb,
        int hydraiceArgb,
        int krakeniceArgb,
        int leviathaniceArgb,
        int behemothiceArgb,
        int hippogrifficeArgb,
        int manticoreiceArgb,
        int amphiptericeArgb,
        int yalehoundomeArgb,
        int senmurvomeArgb,
        int simurghomeArgb,
        int sphinxomeArgb,
        int chimeraomeArgb,
        int hydraomeArgb,
        int krakenomeArgb,
        int leviathanomeArgb,
        int behemothomeArgb,
        int hippogriffomeArgb,
        int manticoreomeArgb,
        int amphipteromeArgb,
        int yalehoundeanArgb,
        int senmurveanArgb,
        int simurgheanArgb,
        int sphinxeanArgb,
        int chimeraeanArgb,
        int hydraeanArgb,
        int krakeneanArgb,
        int leviathaneanArgb,
        int behemotheanArgb,
        int hippogriffeanArgb,
        int manticoreeanArgb,
        int amphiptereanArgb,
        int yalehoundeumArgb,
        int senmurveumArgb,
        int simurgheumArgb,
        int sphinxeumArgb,
        int chimeraeumArgb,
        int hydraeumArgb,
        int krakeneumArgb,
        int leviathaneumArgb,
        int behemotheumArgb,
        int hippogriffeumArgb,
        int manticoreeumArgb,
        int amphiptereumArgb,
        int yalehoundiumArgb,
        int senmurviumArgb,
        int simurghiumArgb,
        int sphinxiumArgb,
        int chimeraiumArgb,
        int hydraiumArgb,
        int krakeniumArgb,
        int leviathaniumArgb,
        int behemothiumArgb,
        int hippogriffiumArgb,
        int manticoreiumArgb,
        int amphipteriumArgb,
        int yalehoundolaArgb,
        int senmurvolaArgb,
        int simurgholaArgb,
        int sphinxolaArgb,
        int chimeraolaArgb,
        int hydraolaArgb,
        int krakenolaArgb,
        int leviathanolaArgb,
        int behemotholaArgb,
        int hippogriffolaArgb,
        int manticoreolaArgb,
        int amphipterolaArgb,
        int yalehoundulaArgb,
        int senmurvulaArgb,
        int simurghulaArgb,
        int sphinxulaArgb,
        int chimeraulaArgb,
        int hydraulaArgb,
        int krakenulaArgb,
        int leviathanulaArgb,
        int behemothulaArgb,
        int hippogriffulaArgb,
        int manticoreulaArgb,
        int amphipterulaArgb,
        int yalehoundetaArgb,
        int senmurvetaArgb
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
                0xFF223950,
                0xFF253E57,
                0xFF28435E,
                0xFF2B4865,
                0xFF2E4D6C,
                0xFF315273,
                0xFF34577A,
                0xFF375C81,
                0xFF3A6188,
                0xFF3D668F,
                0xFF406B96,
                0xFF43709D,
                0xFF4675A4,
                0xFF497AAB,
                0xFF4C7FB2,
                0xFF4F84B9,
                0xFF5289C0,
                0xFF558EC7,
                0xFF5893CE,
                0xFF5B98D5,
                0xFF5E9DDC,
                0xFF61A2E3,
                0xFF64A7EA,
                0xFF67ACF1,
                0xFF6AB1F8,
                0xFF6DB6FF,
                0xFF70BC06,
                0xFF73C10D,
                0xFF76C614,
                0xFF79CB1B,
                0xFF7CD022,
                0xFF7FD529,
                0xFF82DA30,
                0xFF85DF37,
                0xFF88E43E,
                0xFF8BE945,
                0xFF8EEE4C,
                0xFF91F353,
                0xFF94F85A,
                0xFF97FD61,
                0xFF9B0268,
                0xFF9E076F,
                0xFFA10C76,
                0xFFA4117D,
                0xFFA71684,
                0xFFAA1B8B,
                0xFFAD2092,
                0xFFB02599,
                0xFFB32AA0,
                0xFFB62FA7,
                0xFFB934AE,
                0xFFBC39B5,
                0xFFBF3EBC,
                0xFFC243C3,
                0xFFC548CA,
                0xFFC84DD1,
                0xFFCB52D8,
                0xFFCE57DF,
                0xFFD15CE6,
                0xFFD461ED,
                0xFFD766F4,
                0xFFDA6BFB,
                0xFFDD7102,
                0xFFE07609,
                0xFFE37B10,
                0xFFE68017,
                0xFFE9851E,
                0xFFEC8A25,
                0xFFEF8F2C,
                0xFFF29433,
                0xFFF5993A,
                0xFFF89E41,
                0xFFFBA348,
                0xFFFEA84F,
                0xFF020304,
                0xFF05080B,
                0xFF080D12,
                0xFF0B1219,
                0xFF0E1720,
                0xFF111C27,
                0xFF14212E,
                0xFF172635,
                0xFF1A2B3C,
                0xFF1D3043,
                0xFF20354A,
                0xFF233A51,
                0xFF263F58,
                0xFF29445F,
                0xFF2C4966,
                0xFF2F4E6D,
                0xFF325374,
                0xFF35587B,
                0xFF385D82,
                0xFF3B6289,
                0xFF3E6790,
                0xFF416C97,
                0xFF44719E,
                0xFF4776A5,
                0xFF4A7BAC,
                0xFF4D80B3,
                0xFF5085BA,
                0xFF538AC1,
                0xFF568FC8,
                0xFF5994CF,
                0xFF5C99D6,
                0xFF5F9EDD,
                0xFF62A3E4,
                0xFF65A8EB,
                0xFF68ADF2,
                0xFF6BB2F9,
                0xFF6EB800
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
                0xFF000000
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
                + Integer.toUnsignedString(amphipterileArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteradeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundureArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvureArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghureArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxureArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraureArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraureArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenureArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanureArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothureArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffureArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreureArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterureArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundiceArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurviceArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghiceArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxiceArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraiceArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraiceArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakeniceArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathaniceArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothiceArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogrifficeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreiceArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphiptericeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundomeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvomeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghomeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxomeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraomeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraomeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenomeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanomeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothomeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffomeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreomeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteromeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundeanArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurveanArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurgheanArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxeanArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraeanArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraeanArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakeneanArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathaneanArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemotheanArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffeanArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreeanArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphiptereanArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundeumArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurveumArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurgheumArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxeumArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraeumArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraeumArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakeneumArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathaneumArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemotheumArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffeumArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreeumArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphiptereumArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundiumArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurviumArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghiumArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxiumArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraiumArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraiumArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakeniumArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathaniumArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothiumArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffiumArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreiumArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteriumArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundolaArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvolaArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurgholaArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxolaArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraolaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraolaArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenolaArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanolaArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemotholaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffolaArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreolaArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterolaArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterulaArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundetaArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvetaArgb, 16)).getBytes(StandardCharsets.UTF_8);
    }

    /// Decodes a payload produced by [`#encode()`].
    ///
    /// @param bytes the resource bytes
    /// @return the washes
    public static ThemeWashes decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        String[] fields = new String(bytes, StandardCharsets.UTF_8).split("\\|", -1);
        if (fields.length != 254) {
            throw new IllegalArgumentException("Theme washes must have two-hundred-fifty-four fields");
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
                parseArgb(fields[143]),
                parseArgb(fields[144]),
                parseArgb(fields[145]),
                parseArgb(fields[146]),
                parseArgb(fields[147]),
                parseArgb(fields[148]),
                parseArgb(fields[149]),
                parseArgb(fields[150]),
                parseArgb(fields[151]),
                parseArgb(fields[152]),
                parseArgb(fields[153]),
                parseArgb(fields[154]),
                parseArgb(fields[155]),
                parseArgb(fields[156]),
                parseArgb(fields[157]),
                parseArgb(fields[158]),
                parseArgb(fields[159]),
                parseArgb(fields[160]),
                parseArgb(fields[161]),
                parseArgb(fields[162]),
                parseArgb(fields[163]),
                parseArgb(fields[164]),
                parseArgb(fields[165]),
                parseArgb(fields[166]),
                parseArgb(fields[167]),
                parseArgb(fields[168]),
                parseArgb(fields[169]),
                parseArgb(fields[170]),
                parseArgb(fields[171]),
                parseArgb(fields[172]),
                parseArgb(fields[173]),
                parseArgb(fields[174]),
                parseArgb(fields[175]),
                parseArgb(fields[176]),
                parseArgb(fields[177]),
                parseArgb(fields[178]),
                parseArgb(fields[179]),
                parseArgb(fields[180]),
                parseArgb(fields[181]),
                parseArgb(fields[182]),
                parseArgb(fields[183]),
                parseArgb(fields[184]),
                parseArgb(fields[185]),
                parseArgb(fields[186]),
                parseArgb(fields[187]),
                parseArgb(fields[188]),
                parseArgb(fields[189]),
                parseArgb(fields[190]),
                parseArgb(fields[191]),
                parseArgb(fields[192]),
                parseArgb(fields[193]),
                parseArgb(fields[194]),
                parseArgb(fields[195]),
                parseArgb(fields[196]),
                parseArgb(fields[197]),
                parseArgb(fields[198]),
                parseArgb(fields[199]),
                parseArgb(fields[200]),
                parseArgb(fields[201]),
                parseArgb(fields[202]),
                parseArgb(fields[203]),
                parseArgb(fields[204]),
                parseArgb(fields[205]),
                parseArgb(fields[206]),
                parseArgb(fields[207]),
                parseArgb(fields[208]),
                parseArgb(fields[209]),
                parseArgb(fields[210]),
                parseArgb(fields[211]),
                parseArgb(fields[212]),
                parseArgb(fields[213]),
                parseArgb(fields[214]),
                parseArgb(fields[215]),
                parseArgb(fields[216]),
                parseArgb(fields[217]),
                parseArgb(fields[218]),
                parseArgb(fields[219]),
                parseArgb(fields[220]),
                parseArgb(fields[221]),
                parseArgb(fields[222]),
                parseArgb(fields[223]),
                parseArgb(fields[224]),
                parseArgb(fields[225]),
                parseArgb(fields[226]),
                parseArgb(fields[227]),
                parseArgb(fields[228]),
                parseArgb(fields[229]),
                parseArgb(fields[230]),
                parseArgb(fields[231]),
                parseArgb(fields[232]),
                parseArgb(fields[233]),
                parseArgb(fields[234]),
                parseArgb(fields[235]),
                parseArgb(fields[236]),
                parseArgb(fields[237]),
                parseArgb(fields[238]),
                parseArgb(fields[239]),
                parseArgb(fields[240]),
                parseArgb(fields[241]),
                parseArgb(fields[242]),
                parseArgb(fields[243]),
                parseArgb(fields[244]),
                parseArgb(fields[245]),
                parseArgb(fields[246]),
                parseArgb(fields[247]),
                parseArgb(fields[248]),
                parseArgb(fields[249]),
                parseArgb(fields[250]),
                parseArgb(fields[251]),
                parseArgb(fields[252]),
                parseArgb(fields[253])
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
