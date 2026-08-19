package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Stores named glaze colors that do not fit in [`ThemeTokens`], [`ThemeSurfaces`],
/// [`ThemeOverlays`], or [`ThemeWashes`].
///
/// Those earlier records are at the JVM constructor limit, so later first-stable
/// named colors live here and encode as a separate theme payload.
///
/// @param name the glaze-pack name
/// @param yalehoundotaArgb the yalehoundota glaze color
/// @param senmurvotaArgb the senmurvota glaze color
/// @param simurghotaArgb the simurghota glaze color
/// @param sphinxotaArgb the sphinxota glaze color
/// @param chimeraotaArgb the chimeraota glaze color
/// @param hydraotaArgb the hydraota glaze color
/// @param krakenotaArgb the krakenota glaze color
/// @param leviathanotaArgb the leviathanota glaze color
/// @param behemothotaArgb the behemothota glaze color
/// @param hippogriffotaArgb the hippogriffota glaze color
/// @param manticoreotaArgb the manticoreota glaze color
/// @param amphipterotaArgb the amphipterota glaze color
/// @param yalehounditaArgb the yalehoundita glaze color
/// @param senmurvitaArgb the senmurvita glaze color
/// @param simurghitaArgb the simurghita glaze color
/// @param sphinxitaArgb the sphinxita glaze color
/// @param chimeraitaArgb the chimeraita glaze color
/// @param hydraitaArgb the hydraita glaze color
/// @param krakenitaArgb the krakenita glaze color
/// @param leviathanitaArgb the leviathanita glaze color
/// @param behemothitaArgb the behemothita glaze color
/// @param hippogriffitaArgb the hippogriffita glaze color
/// @param manticoreitaArgb the manticoreita glaze color
/// @param amphipteritaArgb the amphipterita glaze color
/// @param yalehoundataArgb the yalehoundata glaze color
/// @param senmurvataArgb the senmurvata glaze color
/// @param simurghataArgb the simurghata glaze color
/// @param sphinxataArgb the sphinxata glaze color
/// @param chimeraataArgb the chimeraata glaze color
/// @param hydraataArgb the hydraata glaze color
/// @param krakenataArgb the krakenata glaze color
/// @param leviathanataArgb the leviathanata glaze color
/// @param behemothataArgb the behemothata glaze color
/// @param hippogriffataArgb the hippogriffata glaze color
/// @param manticoreataArgb the manticoreata glaze color
/// @param amphipterataArgb the amphipterata glaze color
/// @param yalehoundosaArgb the yalehoundosa glaze color
/// @param senmurvosaArgb the senmurvosa glaze color
/// @param simurghosaArgb the simurghosa glaze color
/// @param sphinxosaArgb the sphinxosa glaze color
/// @param chimeraosaArgb the chimeraosa glaze color
/// @param hydraosaArgb the hydraosa glaze color
/// @param krakenosaArgb the krakenosa glaze color
/// @param leviathanosaArgb the leviathanosa glaze color
/// @param behemothosaArgb the behemothosa glaze color
/// @param hippogriffosaArgb the hippogriffosa glaze color
/// @param manticoreosaArgb the manticoreosa glaze color
/// @param amphipterosaArgb the amphipterosa glaze color
/// @param yalehoundusaArgb the yalehoundusa glaze color
/// @param senmurvusaArgb the senmurvusa glaze color
/// @param simurghusaArgb the simurghusa glaze color
/// @param sphinxusaArgb the sphinxusa glaze color
/// @param chimerausaArgb the chimerausa glaze color
/// @param hydrausaArgb the hydrausa glaze color
/// @param krakenusaArgb the krakenusa glaze color
/// @param leviathanusaArgb the leviathanusa glaze color
/// @param behemothusaArgb the behemothusa glaze color
/// @param hippogriffusaArgb the hippogriffusa glaze color
/// @param manticoreusaArgb the manticoreusa glaze color
/// @param amphipterusaArgb the amphipterusa glaze color
/// @param yalehoundisaArgb the yalehoundisa glaze color
/// @param senmurvisaArgb the senmurvisa glaze color
/// @param simurghisaArgb the simurghisa glaze color
/// @param sphinxisaArgb the sphinxisa glaze color
/// @param chimeraisaArgb the chimeraisa glaze color
/// @param hydraisaArgb the hydraisa glaze color
/// @param krakenisaArgb the krakenisa glaze color
/// @param leviathanisaArgb the leviathanisa glaze color
/// @param behemothisaArgb the behemothisa glaze color
/// @param hippogriffisaArgb the hippogriffisa glaze color
/// @param manticoreisaArgb the manticoreisa glaze color
/// @param amphipterisaArgb the amphipterisa glaze color
/// @param yalehoundaseArgb the yalehoundase glaze color
/// @param senmurvaseArgb the senmurvase glaze color
/// @param simurghaseArgb the simurghase glaze color
/// @param sphinxaseArgb the sphinxase glaze color
/// @param chimeraaseArgb the chimeraase glaze color
/// @param hydraaseArgb the hydraase glaze color
/// @param krakenaseArgb the krakenase glaze color
/// @param leviathanaseArgb the leviathanase glaze color
/// @param behemothaseArgb the behemothase glaze color
/// @param hippogriffaseArgb the hippogriffase glaze color
/// @param manticoreaseArgb the manticorease glaze color
/// @param amphipteraseArgb the amphipterase glaze color
/// @param yalehoundiseArgb the yalehoundise glaze color
/// @param senmurviseArgb the senmurvise glaze color
/// @param simurghiseArgb the simurghise glaze color
/// @param sphinxiseArgb the sphinxise glaze color
/// @param chimeraiseArgb the chimeraise glaze color
/// @param hydraiseArgb the hydraise glaze color
/// @param krakeniseArgb the krakenise glaze color
/// @param leviathaniseArgb the leviathanise glaze color
/// @param behemothiseArgb the behemothise glaze color
/// @param hippogriffiseArgb the hippogriffise glaze color
/// @param manticoreiseArgb the manticoreise glaze color
/// @param amphipteriseArgb the amphipterise glaze color
/// @param yalehoundeseArgb the yalehoundese glaze color
/// @param senmurveseArgb the senmurvese glaze color
/// @param simurgheseArgb the simurghese glaze color
/// @param sphinxeseArgb the sphinxese glaze color
/// @param chimeraeseArgb the chimeraese glaze color
/// @param hydraeseArgb the hydraese glaze color
/// @param krakeneseArgb the krakenese glaze color
/// @param leviathaneseArgb the leviathanese glaze color
/// @param behemotheseArgb the behemothese glaze color
/// @param hippogriffeseArgb the hippogriffese glaze color
/// @param manticoreeseArgb the manticoreese glaze color
/// @param amphiptereseArgb the amphipterese glaze color
/// @param yalehoundaneArgb the yalehoundane glaze color
/// @param senmurvaneArgb the senmurvane glaze color
/// @param simurghaneArgb the simurghane glaze color
/// @param sphinxaneArgb the sphinxane glaze color
/// @param chimeraaneArgb the chimeraane glaze color
/// @param hydraaneArgb the hydraane glaze color
/// @param krakenaneArgb the krakenane glaze color
/// @param leviathananeArgb the leviathanane glaze color
/// @param behemothaneArgb the behemothane glaze color
/// @param hippogriffaneArgb the hippogriffane glaze color
/// @param manticoreaneArgb the manticoreane glaze color
/// @param amphipteraneArgb the amphipterane glaze color
/// @param yalehoundoneArgb the yalehoundone glaze color
/// @param senmurvoneArgb the senmurvone glaze color
/// @param simurghoneArgb the simurghone glaze color
/// @param sphinxoneArgb the sphinxone glaze color
/// @param chimeraoneArgb the chimeraone glaze color
/// @param hydraoneArgb the hydraone glaze color
/// @param krakenoneArgb the krakenone glaze color
/// @param leviathanoneArgb the leviathanone glaze color
/// @param behemothoneArgb the behemothone glaze color
/// @param hippogriffoneArgb the hippogriffone glaze color
/// @param manticoreoneArgb the manticoreone glaze color
/// @param amphipteroneArgb the amphipterone glaze color
/// @param senmurvuneArgb the senmurvune glaze color
/// @param simurghuneArgb the simurghune glaze color
/// @param sphinxuneArgb the sphinxune glaze color
/// @param chimerauneArgb the chimeraune glaze color
/// @param hydrauneArgb the hydraune glaze color
/// @param krakenuneArgb the krakenune glaze color
/// @param leviathanuneArgb the leviathanune glaze color
/// @param behemothuneArgb the behemothune glaze color
/// @param hippogriffuneArgb the hippogriffune glaze color
/// @param manticoreuneArgb the manticoreune glaze color
/// @param amphipteruneArgb the amphipterune glaze color
/// @param senmurveneArgb the senmurvene glaze color
/// @param simurgheneArgb the simurghene glaze color
/// @param sphinxeneArgb the sphinxene glaze color
/// @param chimeraeneArgb the chimeraene glaze color
/// @param hydraeneArgb the hydraene glaze color
/// @param krakeneneArgb the krakenene glaze color
/// @param leviathaneneArgb the leviathanene glaze color
/// @param behemotheneArgb the behemothene glaze color
/// @param hippogriffeneArgb the hippogriffene glaze color
/// @param manticoreeneArgb the manticoreene glaze color
/// @param amphiptereneArgb the amphipterene glaze color
/// @param yalehoundoleArgb the yalehoundole glaze color
/// @param senmurvoleArgb the senmurvole glaze color
/// @param simurgholeArgb the simurghole glaze color
/// @param sphinxoleArgb the sphinxole glaze color
/// @param chimeraoleArgb the chimeraole glaze color
/// @param hydraoleArgb the hydraole glaze color
/// @param krakenoleArgb the krakenole glaze color
/// @param leviathanoleArgb the leviathanole glaze color
/// @param behemotholeArgb the behemothole glaze color
/// @param hippogriffoleArgb the hippogriffole glaze color
/// @param manticoreoleArgb the manticoreole glaze color
/// @param amphipteroleArgb the amphipterole glaze color
/// @param simurghaleArgb the simurghale glaze color
/// @param sphinxaleArgb the sphinxale glaze color
/// @param chimeraaleArgb the chimeraale glaze color
/// @param hydraaleArgb the hydraale glaze color
/// @param krakenaleArgb the krakenale glaze color
/// @param leviathanaleArgb the leviathanale glaze color
/// @param behemothaleArgb the behemothale glaze color
/// @param hippogriffaleArgb the hippogriffale glaze color
/// @param manticorealeArgb the manticoreale glaze color
/// @param amphipteraleArgb the amphipterale glaze color
/// @param yalehoundeleArgb the yalehoundele glaze color
/// @param senmurveleArgb the senmurvele glaze color
/// @param simurgheleArgb the simurghele glaze color
/// @param sphinxeleArgb the sphinxele glaze color
/// @param chimeraeleArgb the chimeraele glaze color
/// @param hydraeleArgb the hydraele glaze color
/// @param krakeneleArgb the krakenele glaze color
/// @param leviathaneleArgb the leviathanele glaze color
/// @param behemotheleArgb the behemothele glaze color
/// @param hippogriffeleArgb the hippogriffele glaze color
/// @param manticoreeleArgb the manticoreele glaze color
/// @param amphiptereleArgb the amphipterele glaze color
/// @param yalehoundaelArgb the yalehoundael glaze color
/// @param senmurvaelArgb the senmurvael glaze color
/// @param simurghaelArgb the simurghael glaze color
/// @param sphinxaelArgb the sphinxael glaze color
/// @param chimeraaelArgb the chimeraael glaze color
/// @param hydraaelArgb the hydraael glaze color
/// @param krakenaelArgb the krakenael glaze color
/// @param leviathanaelArgb the leviathanael glaze color
/// @param behemothaelArgb the behemothael glaze color
/// @param hippogriffaelArgb the hippogriffael glaze color
/// @param manticoreaelArgb the manticoreael glaze color
/// @param amphipteraelArgb the amphipterael glaze color
/// @param yalehoundoelArgb the yalehoundoel glaze color
/// @param senmurvoelArgb the senmurvoel glaze color
/// @param simurghoelArgb the simurghoel glaze color
/// @param sphinxoelArgb the sphinxoel glaze color
/// @param chimeraoelArgb the chimeraoel glaze color
/// @param hydraoelArgb the hydraoel glaze color
/// @param krakenoelArgb the krakenoel glaze color
/// @param leviathanoelArgb the leviathanoel glaze color
/// @param behemothoelArgb the behemothoel glaze color
/// @param hippogriffoelArgb the hippogriffoel glaze color
/// @param manticoreoelArgb the manticoreoel glaze color
/// @param amphipteroelArgb the amphipteroel glaze color
/// @param yalehoundielArgb the yalehoundiel glaze color
/// @param senmurvielArgb the senmurviel glaze color
/// @param simurghielArgb the simurghiel glaze color
/// @param sphinxielArgb the sphinxiel glaze color
/// @param chimeraielArgb the chimeraiel glaze color
/// @param hydraielArgb the hydraiel glaze color
/// @param krakenielArgb the krakeniel glaze color
/// @param leviathanielArgb the leviathaniel glaze color
/// @param behemothielArgb the behemothiel glaze color
/// @param hippogriffielArgb the hippogriffiel glaze color
/// @param manticoreielArgb the manticoreiel glaze color
/// @param amphipterielArgb the amphipteriel glaze color
/// @param yalehounduelArgb the yalehounduel glaze color
/// @param senmurvuelArgb the senmurvuel glaze color
/// @param simurghuelArgb the simurghuel glaze color
/// @param sphinxuelArgb the sphinxuel glaze color
/// @param chimerauelArgb the chimerauel glaze color
/// @param hydrauelArgb the hydrauel glaze color
/// @param krakenuelArgb the krakenuel glaze color
/// @param leviathanuelArgb the leviathanuel glaze color
/// @param behemothuelArgb the behemothuel glaze color
/// @param hippogriffuelArgb the hippogriffuel glaze color
/// @param manticoreuelArgb the manticoreuel glaze color
/// @param amphipteruelArgb the amphipteruel glaze color
/// @param yalehoundyneArgb the yalehoundyne glaze color
/// @param senmurvyneArgb the senmurvyne glaze color
/// @param simurghyneArgb the simurghyne glaze color
/// @param sphinxyneArgb the sphinxyne glaze color
/// @param chimerayneArgb the chimerayne glaze color
/// @param hydrayneArgb the hydrayne glaze color
/// @param krakenyneArgb the krakenyne glaze color
/// @param leviathanyneArgb the leviathanyne glaze color
/// @param behemothyneArgb the behemothyne glaze color
/// @param hippogriffyneArgb the hippogriffyne glaze color
/// @param manticoreyneArgb the manticoreyne glaze color
/// @param amphipteryneArgb the amphipteryne glaze color
/// @param yalehoundyteArgb the yalehoundyte glaze color
/// @param senmurvyteArgb the senmurvyte glaze color
/// @param simurghyteArgb the simurghyte glaze color
/// @param sphinxyteArgb the sphinxyte glaze color
/// @param chimerayteArgb the chimerayte glaze color
@NotNullByDefault
public record ThemeGlazes(
        String name,
        int yalehoundotaArgb,
        int senmurvotaArgb,
        int simurghotaArgb,
        int sphinxotaArgb,
        int chimeraotaArgb,
        int hydraotaArgb,
        int krakenotaArgb,
        int leviathanotaArgb,
        int behemothotaArgb,
        int hippogriffotaArgb,
        int manticoreotaArgb,
        int amphipterotaArgb,
        int yalehounditaArgb,
        int senmurvitaArgb,
        int simurghitaArgb,
        int sphinxitaArgb,
        int chimeraitaArgb,
        int hydraitaArgb,
        int krakenitaArgb,
        int leviathanitaArgb,
        int behemothitaArgb,
        int hippogriffitaArgb,
        int manticoreitaArgb,
        int amphipteritaArgb,
        int yalehoundataArgb,
        int senmurvataArgb,
        int simurghataArgb,
        int sphinxataArgb,
        int chimeraataArgb,
        int hydraataArgb,
        int krakenataArgb,
        int leviathanataArgb,
        int behemothataArgb,
        int hippogriffataArgb,
        int manticoreataArgb,
        int amphipterataArgb,
        int yalehoundosaArgb,
        int senmurvosaArgb,
        int simurghosaArgb,
        int sphinxosaArgb,
        int chimeraosaArgb,
        int hydraosaArgb,
        int krakenosaArgb,
        int leviathanosaArgb,
        int behemothosaArgb,
        int hippogriffosaArgb,
        int manticoreosaArgb,
        int amphipterosaArgb,
        int yalehoundusaArgb,
        int senmurvusaArgb,
        int simurghusaArgb,
        int sphinxusaArgb,
        int chimerausaArgb,
        int hydrausaArgb,
        int krakenusaArgb,
        int leviathanusaArgb,
        int behemothusaArgb,
        int hippogriffusaArgb,
        int manticoreusaArgb,
        int amphipterusaArgb,
        int yalehoundisaArgb,
        int senmurvisaArgb,
        int simurghisaArgb,
        int sphinxisaArgb,
        int chimeraisaArgb,
        int hydraisaArgb,
        int krakenisaArgb,
        int leviathanisaArgb,
        int behemothisaArgb,
        int hippogriffisaArgb,
        int manticoreisaArgb,
        int amphipterisaArgb,
        int yalehoundaseArgb,
        int senmurvaseArgb,
        int simurghaseArgb,
        int sphinxaseArgb,
        int chimeraaseArgb,
        int hydraaseArgb,
        int krakenaseArgb,
        int leviathanaseArgb,
        int behemothaseArgb,
        int hippogriffaseArgb,
        int manticoreaseArgb,
        int amphipteraseArgb,
        int yalehoundiseArgb,
        int senmurviseArgb,
        int simurghiseArgb,
        int sphinxiseArgb,
        int chimeraiseArgb,
        int hydraiseArgb,
        int krakeniseArgb,
        int leviathaniseArgb,
        int behemothiseArgb,
        int hippogriffiseArgb,
        int manticoreiseArgb,
        int amphipteriseArgb,
        int yalehoundeseArgb,
        int senmurveseArgb,
        int simurgheseArgb,
        int sphinxeseArgb,
        int chimeraeseArgb,
        int hydraeseArgb,
        int krakeneseArgb,
        int leviathaneseArgb,
        int behemotheseArgb,
        int hippogriffeseArgb,
        int manticoreeseArgb,
        int amphiptereseArgb,
        int yalehoundaneArgb,
        int senmurvaneArgb,
        int simurghaneArgb,
        int sphinxaneArgb,
        int chimeraaneArgb,
        int hydraaneArgb,
        int krakenaneArgb,
        int leviathananeArgb,
        int behemothaneArgb,
        int hippogriffaneArgb,
        int manticoreaneArgb,
        int amphipteraneArgb,
        int yalehoundoneArgb,
        int senmurvoneArgb,
        int simurghoneArgb,
        int sphinxoneArgb,
        int chimeraoneArgb,
        int hydraoneArgb,
        int krakenoneArgb,
        int leviathanoneArgb,
        int behemothoneArgb,
        int hippogriffoneArgb,
        int manticoreoneArgb,
        int amphipteroneArgb,
        int senmurvuneArgb,
        int simurghuneArgb,
        int sphinxuneArgb,
        int chimerauneArgb,
        int hydrauneArgb,
        int krakenuneArgb,
        int leviathanuneArgb,
        int behemothuneArgb,
        int hippogriffuneArgb,
        int manticoreuneArgb,
        int amphipteruneArgb,
        int senmurveneArgb,
        int simurgheneArgb,
        int sphinxeneArgb,
        int chimeraeneArgb,
        int hydraeneArgb,
        int krakeneneArgb,
        int leviathaneneArgb,
        int behemotheneArgb,
        int hippogriffeneArgb,
        int manticoreeneArgb,
        int amphiptereneArgb,
        int yalehoundoleArgb,
        int senmurvoleArgb,
        int simurgholeArgb,
        int sphinxoleArgb,
        int chimeraoleArgb,
        int hydraoleArgb,
        int krakenoleArgb,
        int leviathanoleArgb,
        int behemotholeArgb,
        int hippogriffoleArgb,
        int manticoreoleArgb,
        int amphipteroleArgb,
        int simurghaleArgb,
        int sphinxaleArgb,
        int chimeraaleArgb,
        int hydraaleArgb,
        int krakenaleArgb,
        int leviathanaleArgb,
        int behemothaleArgb,
        int hippogriffaleArgb,
        int manticorealeArgb,
        int amphipteraleArgb,
        int yalehoundeleArgb,
        int senmurveleArgb,
        int simurgheleArgb,
        int sphinxeleArgb,
        int chimeraeleArgb,
        int hydraeleArgb,
        int krakeneleArgb,
        int leviathaneleArgb,
        int behemotheleArgb,
        int hippogriffeleArgb,
        int manticoreeleArgb,
        int amphiptereleArgb,
        int yalehoundaelArgb,
        int senmurvaelArgb,
        int simurghaelArgb,
        int sphinxaelArgb,
        int chimeraaelArgb,
        int hydraaelArgb,
        int krakenaelArgb,
        int leviathanaelArgb,
        int behemothaelArgb,
        int hippogriffaelArgb,
        int manticoreaelArgb,
        int amphipteraelArgb,
        int yalehoundoelArgb,
        int senmurvoelArgb,
        int simurghoelArgb,
        int sphinxoelArgb,
        int chimeraoelArgb,
        int hydraoelArgb,
        int krakenoelArgb,
        int leviathanoelArgb,
        int behemothoelArgb,
        int hippogriffoelArgb,
        int manticoreoelArgb,
        int amphipteroelArgb,
        int yalehoundielArgb,
        int senmurvielArgb,
        int simurghielArgb,
        int sphinxielArgb,
        int chimeraielArgb,
        int hydraielArgb,
        int krakenielArgb,
        int leviathanielArgb,
        int behemothielArgb,
        int hippogriffielArgb,
        int manticoreielArgb,
        int amphipterielArgb,
        int yalehounduelArgb,
        int senmurvuelArgb,
        int simurghuelArgb,
        int sphinxuelArgb,
        int chimerauelArgb,
        int hydrauelArgb,
        int krakenuelArgb,
        int leviathanuelArgb,
        int behemothuelArgb,
        int hippogriffuelArgb,
        int manticoreuelArgb,
        int amphipteruelArgb,
        int yalehoundyneArgb,
        int senmurvyneArgb,
        int simurghyneArgb,
        int sphinxyneArgb,
        int chimerayneArgb,
        int hydrayneArgb,
        int krakenyneArgb,
        int leviathanyneArgb,
        int behemothyneArgb,
        int hippogriffyneArgb,
        int manticoreyneArgb,
        int amphipteryneArgb,
        int yalehoundyteArgb,
        int senmurvyteArgb,
        int simurghyteArgb,
        int sphinxyteArgb,
        int chimerayteArgb
) {
    /// Validates the glazes.
    public ThemeGlazes {
        Objects.requireNonNull(name, "name");
    }

    /// Returns the standard extra glazes.
    ///
    /// @return the glazes
    public static ThemeGlazes standard() {
        return new ThemeGlazes(
                "standard",
                0xFF71BD07,
                0xFF74C20E,
                0xFF77C715,
                0xFF7ACC1C,
                0xFF7DD123,
                0xFF80D62A,
                0xFF83DB31,
                0xFF86E038,
                0xFF89E53F,
                0xFF8CEA46,
                0xFF8FEF4D,
                0xFF92F454,
                0xFF95F95B,
                0xFF98FE62,
                0xFF9C0369,
                0xFF9F0870,
                0xFFA20D77,
                0xFFA5127E,
                0xFFA81785,
                0xFFAB1C8C,
                0xFFAE2193,
                0xFFB1269A,
                0xFFB42BA1,
                0xFFB730A8,
                0xFFBA35AF,
                0xFFBD3AB6,
                0xFFC03FBD,
                0xFFC344C4,
                0xFFC649CB,
                0xFFC94ED2,
                0xFFCC53D9,
                0xFFCF58E0,
                0xFFD25DE7,
                0xFFD562EE,
                0xFFD867F5,
                0xFFDB6CFC,
                0xFFDE7203,
                0xFFE1770A,
                0xFFE47C11,
                0xFFE78118,
                0xFFEA861F,
                0xFFED8B26,
                0xFFF0902D,
                0xFFF39534,
                0xFFF69A3B,
                0xFFF99F42,
                0xFFFCA449,
                0xFFFFA950,
                0xFF030405,
                0xFF06090C,
                0xFF090E13,
                0xFF0C131A,
                0xFF0F1821,
                0xFF121D28,
                0xFF15222F,
                0xFF182736,
                0xFF1B2C3D,
                0xFF1E3144,
                0xFF21364B,
                0xFF243B52,
                0xFF274059,
                0xFF2A4560,
                0xFF2D4A67,
                0xFF304F6E,
                0xFF335475,
                0xFF36597C,
                0xFF395E83,
                0xFF3C638A,
                0xFF3F6891,
                0xFF426D98,
                0xFF45729F,
                0xFF4877A6,
                0xFF4B7CAD,
                0xFF4E81B4,
                0xFF5186BB,
                0xFF548BC2,
                0xFF5790C9,
                0xFF5A95D0,
                0xFF5D9AD7,
                0xFF609FDE,
                0xFF63A4E5,
                0xFF66A9EC,
                0xFF69AEF3,
                0xFF6CB3FA,
                0xFF6FB901,
                0xFF72BE08,
                0xFF75C30F,
                0xFF78C816,
                0xFF7BCD1D,
                0xFF7ED224,
                0xFF81D72B,
                0xFF84DC32,
                0xFF87E139,
                0xFF8AE640,
                0xFF8DEB47,
                0xFF90F04E,
                0xFF93F555,
                0xFF96FA5C,
                0xFF99FF63,
                0xFF9D046A,
                0xFFA00971,
                0xFFA30E78,
                0xFFA6137F,
                0xFFA91886,
                0xFFAC1D8D,
                0xFFAF2294,
                0xFFB2279B,
                0xFFB52CA2,
                0xFFB831A9,
                0xFFBB36B0,
                0xFFBE3BB7,
                0xFFC140BE,
                0xFFC445C5,
                0xFFC74ACC,
                0xFFCA4FD3,
                0xFFCD54DA,
                0xFFD059E1,
                0xFFD35EE8,
                0xFFD663EF,
                0xFFD968F6,
                0xFFDC6DFD,
                0xFFDF7304,
                0xFFE2780B,
                0xFFE57D12,
                0xFFE88219,
                0xFFEB8720,
                0xFFEE8C27,
                0xFFF1912E,
                0xFFF49635,
                0xFFF79B3C,
                0xFFFAA043,
                0xFFFDA54A,
                0xFF040506,
                0xFF070A0D,
                0xFF0A0F14,
                0xFF0D141B,
                0xFF101922,
                0xFF131E29,
                0xFF162330,
                0xFF192837,
                0xFF1C2D3E,
                0xFF1F3245,
                0xFF22374C,
                0xFF253C53,
                0xFF28415A,
                0xFF2B4661,
                0xFF2E4B68,
                0xFF31506F,
                0xFF345576,
                0xFF375A7D,
                0xFF3A5F84,
                0xFF3D648B,
                0xFF406992,
                0xFF436E99,
                0xFF4673A0,
                0xFF4978A7,
                0xFF4C7DAE,
                0xFF4F82B5,
                0xFF5287BC,
                0xFF558CC3,
                0xFF5891CA,
                0xFF5B96D1,
                0xFF5E9BD8,
                0xFF61A0DF,
                0xFF64A5E6,
                0xFF67AAED,
                0xFF6AAFF4,
                0xFF6DB4FB,
                0xFF70BA02,
                0xFF73BF09,
                0xFF76C410,
                0xFF79C917,
                0xFF7CCE1E,
                0xFF7FD325,
                0xFF82D82C,
                0xFF85DD33,
                0xFF88E23A,
                0xFF8BE741,
                0xFF8EEC48,
                0xFF91F14F,
                0xFF94F656,
                0xFF97FB5D,
                0xFF9B0064,
                0xFF9E056B,
                0xFFA10A72,
                0xFFA40F79,
                0xFFA71480,
                0xFFAA1987,
                0xFFAD1E8E,
                0xFFB02395,
                0xFFB3289C,
                0xFFB62DA3,
                0xFFB932AA,
                0xFFBC37B1,
                0xFFBF3CB8,
                0xFFC241BF,
                0xFFC546C6,
                0xFFC84BCD,
                0xFFCB50D4,
                0xFFCE55DB,
                0xFFD15AE2,
                0xFFD45FE9,
                0xFFD764F0,
                0xFFDA69F7,
                0xFFDD6EFE,
                0xFFE07405,
                0xFFE3790C,
                0xFFE67E13,
                0xFFE9831A,
                0xFFEC8821,
                0xFFEF8D28,
                0xFFF2922F,
                0xFFF59736,
                0xFFF89C3D,
                0xFFFBA144,
                0xFFFEA64B,
                0xFF050607,
                0xFF080B0E,
                0xFF0B1015,
                0xFF0E151C,
                0xFF111A23,
                0xFF141F2A,
                0xFF172431,
                0xFF1A2938,
                0xFF1D2E3F,
                0xFF203346,
                0xFF23384D,
                0xFF263D54,
                0xFF29425B,
                0xFF2C4762,
                0xFF2F4C69,
                0xFF325170,
                0xFF355677,
                0xFF385B7E,
                0xFF3B6085,
                0xFF3E658C,
                0xFF416A93,
                0xFF446F9A,
                0xFF4774A1,
                0xFF4A79A8,
                0xFF4D7EAF,
                0xFF5083B6,
                0xFF5388BD,
                0xFF568DC4,
                0xFF5992CB,
                0xFF5C97D2,
                0xFF5F9CD9,
                0xFF62A1E0,
                0xFF65A6E7,
                0xFF68ABEE,
                0xFF6BB0F5,
                0xFF6EB5FC,
                0xFF71BB03
        );
    }

    /// Returns the high-contrast extra glazes.
    ///
    /// @return the glazes
    public static ThemeGlazes highContrastTheme() {
        return new ThemeGlazes(
                "high-contrast",
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
                0xFF000000,
                0xFFFFFFFF
        );
    }

    /// Encodes these glazes as a UTF-8 pipe-separated payload.
    ///
    /// @return the payload bytes
    public byte[] encode() {
        return (name
                + '|'
                + Integer.toUnsignedString(yalehoundotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterotaArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehounditaArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvitaArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghitaArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxitaArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraitaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraitaArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenitaArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanitaArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothitaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffitaArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreitaArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteritaArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundataArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvataArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghataArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxataArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraataArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraataArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenataArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanataArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothataArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffataArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreataArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterataArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterosaArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundusaArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvusaArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghusaArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxusaArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerausaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydrausaArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenusaArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanusaArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothusaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffusaArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreusaArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterusaArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterisaArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteraseArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundiseArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurviseArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghiseArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxiseArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraiseArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraiseArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakeniseArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathaniseArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothiseArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffiseArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreiseArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteriseArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundeseArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurveseArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurgheseArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxeseArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraeseArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraeseArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakeneseArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathaneseArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemotheseArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffeseArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreeseArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphiptereseArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundaneArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvaneArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghaneArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxaneArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraaneArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraaneArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenaneArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathananeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothaneArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffaneArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreaneArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteraneArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundoneArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvoneArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghoneArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxoneArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraoneArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraoneArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenoneArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanoneArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothoneArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffoneArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreoneArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteroneArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvuneArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghuneArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxuneArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerauneArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydrauneArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenuneArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanuneArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothuneArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffuneArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreuneArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteruneArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurveneArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurgheneArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxeneArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraeneArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraeneArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakeneneArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathaneneArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemotheneArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffeneArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreeneArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphiptereneArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundoleArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvoleArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurgholeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxoleArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraoleArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraoleArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenoleArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanoleArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemotholeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffoleArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreoleArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteroleArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghaleArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxaleArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraaleArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraaleArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenaleArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanaleArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothaleArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffaleArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticorealeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteraleArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundeleArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurveleArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurgheleArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxeleArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraeleArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraeleArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakeneleArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathaneleArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemotheleArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffeleArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreeleArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphiptereleArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundaelArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvaelArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghaelArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxaelArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraaelArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraaelArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenaelArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanaelArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothaelArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffaelArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreaelArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteraelArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundoelArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvoelArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghoelArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxoelArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraoelArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraoelArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenoelArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanoelArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothoelArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffoelArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreoelArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteroelArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundielArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvielArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghielArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxielArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraielArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraielArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenielArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanielArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothielArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffielArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreielArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterielArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehounduelArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvuelArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghuelArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxuelArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerauelArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydrauelArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenuelArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanuelArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothuelArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffuelArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreuelArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteruelArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundyneArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvyneArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghyneArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxyneArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerayneArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydrayneArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenyneArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanyneArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothyneArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffyneArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreyneArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteryneArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundyteArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvyteArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghyteArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxyteArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerayteArgb, 16)).getBytes(StandardCharsets.UTF_8);
    }

    /// Decodes a payload produced by [`#encode()`].
    ///
    /// @param bytes the resource bytes
    /// @return the glazes
    public static ThemeGlazes decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        String[] fields = new String(bytes, StandardCharsets.UTF_8).split("\\|", -1);
        if (fields.length != 254) {
            throw new IllegalArgumentException("Theme glazes must have two-hundred-fifty-four fields");
        }
        return new ThemeGlazes(
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
