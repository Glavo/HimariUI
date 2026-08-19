package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Stores named sheen colors that do not fit in [`ThemeTokens`], [`ThemeSurfaces`],
/// [`ThemeOverlays`], [`ThemeWashes`], or [`ThemeGlazes`].
///
/// Those earlier records are at the JVM constructor limit, so later first-stable
/// named colors live here and encode as a separate theme payload.
///
/// @param name the sheen-pack name
/// @param hydrayteArgb the hydrayte sheen color
/// @param krakenyteArgb the krakenyte sheen color
/// @param leviathanyteArgb the leviathanyte sheen color
/// @param behemothyteArgb the behemothyte sheen color
/// @param hippogriffyteArgb the hippogriffyte sheen color
/// @param manticoreyteArgb the manticoreyte sheen color
/// @param amphipteryteArgb the amphipteryte sheen color
/// @param yalehoundymeArgb the yalehoundyme sheen color
/// @param senmurvymeArgb the senmurvyme sheen color
/// @param simurghymeArgb the simurghyme sheen color
/// @param sphinxymeArgb the sphinxyme sheen color
/// @param chimeraymeArgb the chimerayme sheen color
/// @param hydraymeArgb the hydrayme sheen color
/// @param krakenymeArgb the krakenyme sheen color
/// @param leviathanymeArgb the leviathanyme sheen color
/// @param behemothymeArgb the behemothyme sheen color
/// @param hippogriffymeArgb the hippogriffyme sheen color
/// @param manticoreymeArgb the manticoreyme sheen color
/// @param amphipterymeArgb the amphipteryme sheen color
/// @param yalehoundyseArgb the yalehoundyse sheen color
/// @param senmurvyseArgb the senmurvyse sheen color
/// @param simurghyseArgb the simurghyse sheen color
/// @param sphinxyseArgb the sphinxyse sheen color
/// @param chimerayseArgb the chimerayse sheen color
/// @param hydrayseArgb the hydrayse sheen color
/// @param krakenyseArgb the krakenyse sheen color
/// @param leviathanyseArgb the leviathanyse sheen color
/// @param behemothyseArgb the behemothyse sheen color
/// @param hippogriffyseArgb the hippogriffyse sheen color
/// @param manticoreyseArgb the manticoreyse sheen color
/// @param amphipteryseArgb the amphipteryse sheen color
/// @param yalehoundykeArgb the yalehoundyke sheen color
/// @param senmurvykeArgb the senmurvyke sheen color
/// @param simurghykeArgb the simurghyke sheen color
/// @param sphinxykeArgb the sphinxyke sheen color
/// @param chimeraykeArgb the chimerayke sheen color
/// @param hydraykeArgb the hydrayke sheen color
/// @param krakenykeArgb the krakenyke sheen color
/// @param leviathanykeArgb the leviathanyke sheen color
/// @param behemothykeArgb the behemothyke sheen color
/// @param hippogriffykeArgb the hippogriffyke sheen color
/// @param manticoreykeArgb the manticoreyke sheen color
/// @param amphipterykeArgb the amphipteryke sheen color
/// @param yalehoundypeArgb the yalehoundype sheen color
/// @param senmurvypeArgb the senmurvype sheen color
/// @param simurghypeArgb the simurghype sheen color
/// @param sphinxypeArgb the sphinxype sheen color
/// @param chimeraypeArgb the chimeraype sheen color
/// @param hydraypeArgb the hydraype sheen color
/// @param krakenypeArgb the krakenype sheen color
/// @param leviathanypeArgb the leviathanype sheen color
/// @param behemothypeArgb the behemothype sheen color
/// @param hippogriffypeArgb the hippogriffype sheen color
/// @param manticoreypeArgb the manticoreype sheen color
/// @param amphipterypeArgb the amphipterype sheen color
/// @param yalehoundydeArgb the yalehoundyde sheen color
/// @param senmurvydeArgb the senmurvyde sheen color
/// @param simurghydeArgb the simurghyde sheen color
/// @param sphinxydeArgb the sphinxyde sheen color
/// @param chimeraydeArgb the chimerayde sheen color
/// @param hydraydeArgb the hydrayde sheen color
/// @param krakenydeArgb the krakenyde sheen color
/// @param leviathanydeArgb the leviathanyde sheen color
/// @param behemothydeArgb the behemothyde sheen color
/// @param hippogriffydeArgb the hippogriffyde sheen color
/// @param manticoreydeArgb the manticoreyde sheen color
/// @param amphipterydeArgb the amphipteryde sheen color
/// @param yalehoundybeArgb the yalehoundybe sheen color
/// @param senmurvybeArgb the senmurvybe sheen color
/// @param simurghybeArgb the simurghybe sheen color
/// @param sphinxybeArgb the sphinxybe sheen color
/// @param chimeraybeArgb the chimeraybe sheen color
/// @param hydraybeArgb the hydraybe sheen color
/// @param krakenybeArgb the krakenybe sheen color
/// @param leviathanybeArgb the leviathanybe sheen color
/// @param behemothybeArgb the behemothybe sheen color
/// @param hippogriffybeArgb the hippogriffybe sheen color
/// @param manticoreybeArgb the manticoreybe sheen color
/// @param amphipterybeArgb the amphipterybe sheen color
/// @param yalehoundyfeArgb the yalehoundyfe sheen color
/// @param senmurvyfeArgb the senmurvyfe sheen color
/// @param simurghyfeArgb the simurghyfe sheen color
/// @param sphinxyfeArgb the sphinxyfe sheen color
/// @param chimerayfeArgb the chimerayfe sheen color
/// @param hydrayfeArgb the hydrayfe sheen color
/// @param krakenyfeArgb the krakenyfe sheen color
/// @param leviathanyfeArgb the leviathanyfe sheen color
/// @param behemothyfeArgb the behemothyfe sheen color
/// @param hippogriffyfeArgb the hippogriffyfe sheen color
/// @param manticoreyfeArgb the manticoreyfe sheen color
/// @param amphipteryfeArgb the amphipteryfe sheen color
/// @param yalehoundygeArgb the yalehoundyge sheen color
/// @param senmurvygeArgb the senmurvyge sheen color
/// @param simurghygeArgb the simurghyge sheen color
/// @param sphinxygeArgb the sphinxyge sheen color
/// @param chimeraygeArgb the chimerayge sheen color
/// @param hydraygeArgb the hydrayge sheen color
/// @param krakenygeArgb the krakenyge sheen color
/// @param leviathanygeArgb the leviathanyge sheen color
/// @param behemothygeArgb the behemothyge sheen color
/// @param hippogriffygeArgb the hippogriffyge sheen color
/// @param manticoreygeArgb the manticoreyge sheen color
/// @param amphipterygeArgb the amphipteryge sheen color
/// @param yalehoundyheArgb the yalehoundyhe sheen color
/// @param senmurvyheArgb the senmurvyhe sheen color
/// @param simurghyheArgb the simurghyhe sheen color
/// @param sphinxyheArgb the sphinxyhe sheen color
/// @param chimerayheArgb the chimerayhe sheen color
/// @param hydrayheArgb the hydrayhe sheen color
/// @param krakenyheArgb the krakenyhe sheen color
/// @param leviathanyheArgb the leviathanyhe sheen color
/// @param behemothyheArgb the behemothyhe sheen color
/// @param hippogriffyheArgb the hippogriffyhe sheen color
/// @param manticoreyheArgb the manticoreyhe sheen color
/// @param amphipteryheArgb the amphipteryhe sheen color
/// @param yalehoundyjeArgb the yalehoundyje sheen color
/// @param senmurvyjeArgb the senmurvyje sheen color
/// @param simurghyjeArgb the simurghyje sheen color
/// @param sphinxyjeArgb the sphinxyje sheen color
/// @param chimerayjeArgb the chimerayje sheen color
/// @param hydrayjeArgb the hydrayje sheen color
/// @param krakenyjeArgb the krakenyje sheen color
/// @param leviathanyjeArgb the leviathanyje sheen color
/// @param behemothyjeArgb the behemothyje sheen color
/// @param hippogriffyjeArgb the hippogriffyje sheen color
/// @param manticoreyjeArgb the manticoreyje sheen color
/// @param amphipteryjeArgb the amphipteryje sheen color
/// @param yalehoundyleArgb the yalehoundyle sheen color
/// @param senmurvyleArgb the senmurvyle sheen color
/// @param simurghyleArgb the simurghyle sheen color
/// @param sphinxyleArgb the sphinxyle sheen color
/// @param chimerayleArgb the chimerayle sheen color
/// @param hydrayleArgb the hydrayle sheen color
/// @param krakenyleArgb the krakenyle sheen color
/// @param leviathanyleArgb the leviathanyle sheen color
/// @param behemothyleArgb the behemothyle sheen color
/// @param hippogriffyleArgb the hippogriffyle sheen color
/// @param manticoreyleArgb the manticoreyle sheen color
/// @param amphipteryleArgb the amphipteryle sheen color
/// @param yalehoundyreArgb the yalehoundyre sheen color
/// @param senmurvyreArgb the senmurvyre sheen color
/// @param simurghyreArgb the simurghyre sheen color
/// @param sphinxyreArgb the sphinxyre sheen color
/// @param krakenyreArgb the krakenyre sheen color
/// @param leviathanyreArgb the leviathanyre sheen color
/// @param behemothyreArgb the behemothyre sheen color
/// @param hippogriffyreArgb the hippogriffyre sheen color
/// @param manticoreyreArgb the manticoreyre sheen color
/// @param amphipteryreArgb the amphipteryre sheen color
/// @param yalehoundormArgb the yalehoundorm sheen color
/// @param senmurvormArgb the senmurvorm sheen color
/// @param simurghormArgb the simurghorm sheen color
/// @param sphinxormArgb the sphinxorm sheen color
/// @param chimeraormArgb the chimeraorm sheen color
/// @param hydraormArgb the hydraorm sheen color
/// @param krakenormArgb the krakenorm sheen color
/// @param leviathanormArgb the leviathanorm sheen color
/// @param behemothormArgb the behemothorm sheen color
/// @param hippogrifformArgb the hippogrifform sheen color
/// @param manticoreormArgb the manticoreorm sheen color
/// @param amphipterormArgb the amphipterorm sheen color
/// @param yalehoundarmArgb the yalehoundarm sheen color
/// @param senmurvarmArgb the senmurvarm sheen color
/// @param simurgharmArgb the simurgharm sheen color
/// @param sphinxarmArgb the sphinxarm sheen color
/// @param chimeraarmArgb the chimeraarm sheen color
/// @param hydraarmArgb the hydraarm sheen color
/// @param krakenarmArgb the krakenarm sheen color
/// @param leviathanarmArgb the leviathanarm sheen color
/// @param behemotharmArgb the behemotharm sheen color
/// @param hippogriffarmArgb the hippogriffarm sheen color
/// @param manticorearmArgb the manticorearm sheen color
/// @param amphipterarmArgb the amphipterarm sheen color
/// @param yalehoundermArgb the yalehounderm sheen color
/// @param senmurvermArgb the senmurverm sheen color
/// @param simurghermArgb the simurgherm sheen color
/// @param sphinxermArgb the sphinxerm sheen color
/// @param chimeraermArgb the chimeraerm sheen color
/// @param hydraermArgb the hydraerm sheen color
/// @param krakenermArgb the krakenerm sheen color
/// @param leviathanermArgb the leviathanerm sheen color
/// @param behemothermArgb the behemotherm sheen color
/// @param hippogriffermArgb the hippogrifferm sheen color
/// @param manticoreermArgb the manticoreerm sheen color
/// @param amphipterermArgb the amphiptererm sheen color
/// @param yalehoundikeArgb the yalehoundike sheen color
/// @param senmurvikeArgb the senmurvike sheen color
/// @param simurghikeArgb the simurghike sheen color
/// @param sphinxikeArgb the sphinxike sheen color
/// @param chimeraikeArgb the chimeraike sheen color
/// @param hydraikeArgb the hydraike sheen color
/// @param krakenikeArgb the krakenike sheen color
/// @param leviathanikeArgb the leviathanike sheen color
/// @param behemothikeArgb the behemothike sheen color
/// @param hippogriffikeArgb the hippogriffike sheen color
/// @param manticoreikeArgb the manticoreike sheen color
/// @param amphipterikeArgb the amphipterike sheen color
/// @param yalehoundokeArgb the yalehoundoke sheen color
/// @param senmurvokeArgb the senmurvoke sheen color
/// @param simurghokeArgb the simurghoke sheen color
/// @param sphinxokeArgb the sphinxoke sheen color
/// @param chimeraokeArgb the chimeraoke sheen color
/// @param hydraokeArgb the hydraoke sheen color
/// @param krakenokeArgb the krakenoke sheen color
/// @param leviathanokeArgb the leviathanoke sheen color
/// @param behemothokeArgb the behemothoke sheen color
/// @param hippogriffokeArgb the hippogriffoke sheen color
/// @param manticoreokeArgb the manticoreoke sheen color
/// @param amphipterokeArgb the amphipteroke sheen color
/// @param yalehoundakeArgb the yalehoundake sheen color
/// @param senmurvakeArgb the senmurvake sheen color
/// @param simurghakeArgb the simurghake sheen color
/// @param sphinxakeArgb the sphinxake sheen color
/// @param chimeraakeArgb the chimeraake sheen color
/// @param hydraakeArgb the hydraake sheen color
/// @param krakenakeArgb the krakenake sheen color
/// @param leviathanakeArgb the leviathanake sheen color
/// @param behemothakeArgb the behemothake sheen color
/// @param hippogriffakeArgb the hippogriffake sheen color
/// @param manticoreakeArgb the manticoreake sheen color
/// @param amphipterakeArgb the amphipterake sheen color
/// @param yalehoundekeArgb the yalehoundeke sheen color
/// @param senmurvekeArgb the senmurveke sheen color
/// @param simurghekeArgb the simurgheke sheen color
/// @param sphinxekeArgb the sphinxeke sheen color
/// @param chimeraekeArgb the chimeraeke sheen color
/// @param hydraekeArgb the hydraeke sheen color
/// @param krakenekeArgb the krakeneke sheen color
/// @param leviathanekeArgb the leviathaneke sheen color
/// @param behemothekeArgb the behemotheke sheen color
/// @param hippogriffekeArgb the hippogriffeke sheen color
/// @param manticoreekeArgb the manticoreeke sheen color
/// @param amphipterekeArgb the amphiptereke sheen color
/// @param yalehoundealArgb the yalehoundeal sheen color
/// @param senmurvealArgb the senmurveal sheen color
/// @param simurghealArgb the simurgheal sheen color
/// @param sphinxealArgb the sphinxeal sheen color
/// @param chimeraealArgb the chimeraeal sheen color
/// @param hydraealArgb the hydraeal sheen color
/// @param krakenealArgb the krakeneal sheen color
/// @param leviathanealArgb the leviathaneal sheen color
/// @param behemothealArgb the behemotheal sheen color
/// @param hippogriffealArgb the hippogriffeal sheen color
/// @param manticoreealArgb the manticoreeal sheen color
/// @param amphipterealArgb the amphiptereal sheen color
/// @param yalehoundoalArgb the yalehoundoal sheen color
/// @param senmurvoalArgb the senmurvoal sheen color
/// @param simurghoalArgb the simurghoal sheen color
/// @param sphinxoalArgb the sphinxoal sheen color
/// @param chimeraoalArgb the chimeraoal sheen color
/// @param hydraoalArgb the hydraoal sheen color
/// @param krakenoalArgb the krakenoal sheen color
/// @param leviathanoalArgb the leviathanoal sheen color
@NotNullByDefault
public record ThemeSheens(
        String name,
        int hydrayteArgb,
        int krakenyteArgb,
        int leviathanyteArgb,
        int behemothyteArgb,
        int hippogriffyteArgb,
        int manticoreyteArgb,
        int amphipteryteArgb,
        int yalehoundymeArgb,
        int senmurvymeArgb,
        int simurghymeArgb,
        int sphinxymeArgb,
        int chimeraymeArgb,
        int hydraymeArgb,
        int krakenymeArgb,
        int leviathanymeArgb,
        int behemothymeArgb,
        int hippogriffymeArgb,
        int manticoreymeArgb,
        int amphipterymeArgb,
        int yalehoundyseArgb,
        int senmurvyseArgb,
        int simurghyseArgb,
        int sphinxyseArgb,
        int chimerayseArgb,
        int hydrayseArgb,
        int krakenyseArgb,
        int leviathanyseArgb,
        int behemothyseArgb,
        int hippogriffyseArgb,
        int manticoreyseArgb,
        int amphipteryseArgb,
        int yalehoundykeArgb,
        int senmurvykeArgb,
        int simurghykeArgb,
        int sphinxykeArgb,
        int chimeraykeArgb,
        int hydraykeArgb,
        int krakenykeArgb,
        int leviathanykeArgb,
        int behemothykeArgb,
        int hippogriffykeArgb,
        int manticoreykeArgb,
        int amphipterykeArgb,
        int yalehoundypeArgb,
        int senmurvypeArgb,
        int simurghypeArgb,
        int sphinxypeArgb,
        int chimeraypeArgb,
        int hydraypeArgb,
        int krakenypeArgb,
        int leviathanypeArgb,
        int behemothypeArgb,
        int hippogriffypeArgb,
        int manticoreypeArgb,
        int amphipterypeArgb,
        int yalehoundydeArgb,
        int senmurvydeArgb,
        int simurghydeArgb,
        int sphinxydeArgb,
        int chimeraydeArgb,
        int hydraydeArgb,
        int krakenydeArgb,
        int leviathanydeArgb,
        int behemothydeArgb,
        int hippogriffydeArgb,
        int manticoreydeArgb,
        int amphipterydeArgb,
        int yalehoundybeArgb,
        int senmurvybeArgb,
        int simurghybeArgb,
        int sphinxybeArgb,
        int chimeraybeArgb,
        int hydraybeArgb,
        int krakenybeArgb,
        int leviathanybeArgb,
        int behemothybeArgb,
        int hippogriffybeArgb,
        int manticoreybeArgb,
        int amphipterybeArgb,
        int yalehoundyfeArgb,
        int senmurvyfeArgb,
        int simurghyfeArgb,
        int sphinxyfeArgb,
        int chimerayfeArgb,
        int hydrayfeArgb,
        int krakenyfeArgb,
        int leviathanyfeArgb,
        int behemothyfeArgb,
        int hippogriffyfeArgb,
        int manticoreyfeArgb,
        int amphipteryfeArgb,
        int yalehoundygeArgb,
        int senmurvygeArgb,
        int simurghygeArgb,
        int sphinxygeArgb,
        int chimeraygeArgb,
        int hydraygeArgb,
        int krakenygeArgb,
        int leviathanygeArgb,
        int behemothygeArgb,
        int hippogriffygeArgb,
        int manticoreygeArgb,
        int amphipterygeArgb,
        int yalehoundyheArgb,
        int senmurvyheArgb,
        int simurghyheArgb,
        int sphinxyheArgb,
        int chimerayheArgb,
        int hydrayheArgb,
        int krakenyheArgb,
        int leviathanyheArgb,
        int behemothyheArgb,
        int hippogriffyheArgb,
        int manticoreyheArgb,
        int amphipteryheArgb,
        int yalehoundyjeArgb,
        int senmurvyjeArgb,
        int simurghyjeArgb,
        int sphinxyjeArgb,
        int chimerayjeArgb,
        int hydrayjeArgb,
        int krakenyjeArgb,
        int leviathanyjeArgb,
        int behemothyjeArgb,
        int hippogriffyjeArgb,
        int manticoreyjeArgb,
        int amphipteryjeArgb,
        int yalehoundyleArgb,
        int senmurvyleArgb,
        int simurghyleArgb,
        int sphinxyleArgb,
        int chimerayleArgb,
        int hydrayleArgb,
        int krakenyleArgb,
        int leviathanyleArgb,
        int behemothyleArgb,
        int hippogriffyleArgb,
        int manticoreyleArgb,
        int amphipteryleArgb,
        int yalehoundyreArgb,
        int senmurvyreArgb,
        int simurghyreArgb,
        int sphinxyreArgb,
        int krakenyreArgb,
        int leviathanyreArgb,
        int behemothyreArgb,
        int hippogriffyreArgb,
        int manticoreyreArgb,
        int amphipteryreArgb,
        int yalehoundormArgb,
        int senmurvormArgb,
        int simurghormArgb,
        int sphinxormArgb,
        int chimeraormArgb,
        int hydraormArgb,
        int krakenormArgb,
        int leviathanormArgb,
        int behemothormArgb,
        int hippogrifformArgb,
        int manticoreormArgb,
        int amphipterormArgb,
        int yalehoundarmArgb,
        int senmurvarmArgb,
        int simurgharmArgb,
        int sphinxarmArgb,
        int chimeraarmArgb,
        int hydraarmArgb,
        int krakenarmArgb,
        int leviathanarmArgb,
        int behemotharmArgb,
        int hippogriffarmArgb,
        int manticorearmArgb,
        int amphipterarmArgb,
        int yalehoundermArgb,
        int senmurvermArgb,
        int simurghermArgb,
        int sphinxermArgb,
        int chimeraermArgb,
        int hydraermArgb,
        int krakenermArgb,
        int leviathanermArgb,
        int behemothermArgb,
        int hippogriffermArgb,
        int manticoreermArgb,
        int amphipterermArgb,
        int yalehoundikeArgb,
        int senmurvikeArgb,
        int simurghikeArgb,
        int sphinxikeArgb,
        int chimeraikeArgb,
        int hydraikeArgb,
        int krakenikeArgb,
        int leviathanikeArgb,
        int behemothikeArgb,
        int hippogriffikeArgb,
        int manticoreikeArgb,
        int amphipterikeArgb,
        int yalehoundokeArgb,
        int senmurvokeArgb,
        int simurghokeArgb,
        int sphinxokeArgb,
        int chimeraokeArgb,
        int hydraokeArgb,
        int krakenokeArgb,
        int leviathanokeArgb,
        int behemothokeArgb,
        int hippogriffokeArgb,
        int manticoreokeArgb,
        int amphipterokeArgb,
        int yalehoundakeArgb,
        int senmurvakeArgb,
        int simurghakeArgb,
        int sphinxakeArgb,
        int chimeraakeArgb,
        int hydraakeArgb,
        int krakenakeArgb,
        int leviathanakeArgb,
        int behemothakeArgb,
        int hippogriffakeArgb,
        int manticoreakeArgb,
        int amphipterakeArgb,
        int yalehoundekeArgb,
        int senmurvekeArgb,
        int simurghekeArgb,
        int sphinxekeArgb,
        int chimeraekeArgb,
        int hydraekeArgb,
        int krakenekeArgb,
        int leviathanekeArgb,
        int behemothekeArgb,
        int hippogriffekeArgb,
        int manticoreekeArgb,
        int amphipterekeArgb,
        int yalehoundealArgb,
        int senmurvealArgb,
        int simurghealArgb,
        int sphinxealArgb,
        int chimeraealArgb,
        int hydraealArgb,
        int krakenealArgb,
        int leviathanealArgb,
        int behemothealArgb,
        int hippogriffealArgb,
        int manticoreealArgb,
        int amphipterealArgb,
        int yalehoundoalArgb,
        int senmurvoalArgb,
        int simurghoalArgb,
        int sphinxoalArgb,
        int chimeraoalArgb,
        int hydraoalArgb,
        int krakenoalArgb,
        int leviathanoalArgb
) {
    /// Validates the sheens.
    public ThemeSheens {
        Objects.requireNonNull(name, "name");
    }

    /// Returns the standard extra sheens.
    ///
    /// @return the sheens
    public static ThemeSheens standard() {
        return new ThemeSheens(
                "standard",
                0xFF74C00A,
                0xFF77C511,
                0xFF7ACA18,
                0xFF7DCF1F,
                0xFF80D426,
                0xFF83D92D,
                0xFF86DE34,
                0xFF89E33B,
                0xFF8CE842,
                0xFF8FED49,
                0xFF92F250,
                0xFF95F757,
                0xFF98FC5E,
                0xFF9C0165,
                0xFF9F066C,
                0xFFA20B73,
                0xFFA5107A,
                0xFFA81581,
                0xFFAB1A88,
                0xFFAE1F8F,
                0xFFB12496,
                0xFFB4299D,
                0xFFB72EA4,
                0xFFBA33AB,
                0xFFBD38B2,
                0xFFC03DB9,
                0xFFC342C0,
                0xFFC647C7,
                0xFFC94CCE,
                0xFFCC51D5,
                0xFFCF56DC,
                0xFFD25BE3,
                0xFFD560EA,
                0xFFD865F1,
                0xFFDB6AF8,
                0xFFDE6FFF,
                0xFFE17506,
                0xFFE47A0D,
                0xFFE77F14,
                0xFFEA841B,
                0xFFED8922,
                0xFFF08E29,
                0xFFF39330,
                0xFFF69837,
                0xFFF99D3E,
                0xFFFCA245,
                0xFFFFA74C,
                0xFF060708,
                0xFF090C0F,
                0xFF0C1116,
                0xFF0F161D,
                0xFF121B24,
                0xFF15202B,
                0xFF182532,
                0xFF1B2A39,
                0xFF1E2F40,
                0xFF213447,
                0xFF24394E,
                0xFF273E55,
                0xFF2A435C,
                0xFF2D4863,
                0xFF304D6A,
                0xFF335271,
                0xFF365778,
                0xFF395C7F,
                0xFF3C6186,
                0xFF3F668D,
                0xFF426B94,
                0xFF45709B,
                0xFF4875A2,
                0xFF4B7AA9,
                0xFF4E7FB0,
                0xFF5184B7,
                0xFF5489BE,
                0xFF578EC5,
                0xFF5A93CC,
                0xFF5D98D3,
                0xFF609DDA,
                0xFF63A2E1,
                0xFF66A7E8,
                0xFF69ACEF,
                0xFF6CB1F6,
                0xFF6FB6FD,
                0xFF72BC04,
                0xFF75C10B,
                0xFF78C612,
                0xFF7BCB19,
                0xFF7ED020,
                0xFF81D527,
                0xFF84DA2E,
                0xFF87DF35,
                0xFF8AE43C,
                0xFF8DE943,
                0xFF90EE4A,
                0xFF93F351,
                0xFF96F858,
                0xFF99FD5F,
                0xFF9D0266,
                0xFFA0076D,
                0xFFA30C74,
                0xFFA6117B,
                0xFFA91682,
                0xFFAC1B89,
                0xFFAF2090,
                0xFFB22597,
                0xFFB52A9E,
                0xFFB82FA5,
                0xFFBB34AC,
                0xFFBE39B3,
                0xFFC13EBA,
                0xFFC443C1,
                0xFFC748C8,
                0xFFCA4DCF,
                0xFFCD52D6,
                0xFFD057DD,
                0xFFD35CE4,
                0xFFD661EB,
                0xFFD966F2,
                0xFFDC6BF9,
                0xFFDF7100,
                0xFFE27607,
                0xFFE57B0E,
                0xFFE88015,
                0xFFEB851C,
                0xFFEE8A23,
                0xFFF18F2A,
                0xFFF49431,
                0xFFF79938,
                0xFFFA9E3F,
                0xFFFDA346,
                0xFF060708,
                0xFF070809,
                0xFF0A0D10,
                0xFF0D1217,
                0xFF10171E,
                0xFF131C25,
                0xFF16212C,
                0xFF192633,
                0xFF1C2B3A,
                0xFF1F3041,
                0xFF223548,
                0xFF253A4F,
                0xFF283F56,
                0xFF2B445D,
                0xFF2E4964,
                0xFF314E6B,
                0xFF345372,
                0xFF375879,
                0xFF3A5D80,
                0xFF3D6287,
                0xFF40678E,
                0xFF436C95,
                0xFF46719C,
                0xFF4976A3,
                0xFF4C7BAA,
                0xFF4F80B1,
                0xFF5285B8,
                0xFF558ABF,
                0xFF588FC6,
                0xFF5B94CD,
                0xFF5E99D4,
                0xFF619EDB,
                0xFF64A3E2,
                0xFF67A8E9,
                0xFF6AADF0,
                0xFF6DB2F7,
                0xFF70B7FE,
                0xFF73BD05,
                0xFF76C20C,
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
                0xFF060708,
                0xFF070809,
                0xFF0A0D10,
                0xFF0D1217,
                0xFF10171E,
                0xFF131C25,
                0xFF16212C,
                0xFF192633,
                0xFF1C2B3A,
                0xFF1F3041,
                0xFF223548,
                0xFF253A4F,
                0xFF283F56,
                0xFF2B445D,
                0xFF2E4964,
                0xFF314E6B,
                0xFF345372,
                0xFF375879,
                0xFF3A5D80,
                0xFF3D6287,
                0xFF40678E,
                0xFF436C95,
                0xFF46719C,
                0xFF4976A3,
                0xFF4C7BAA,
                0xFF4F80B1,
                0xFF5285B8,
                0xFF558ABF,
                0xFF588FC6,
                0xFF5B94CD,
                0xFF5E99D4,
                0xFF619EDB,
                0xFF64A3E2,
                0xFF67A8E9,
                0xFF6AADF0,
                0xFF6DB2F7,
                0xFF70B7FE,
                0xFF73BD05,
                0xFF76C20C
        );
    }

    /// Returns the high-contrast extra sheens.
    ///
    /// @return the sheens
    public static ThemeSheens highContrastTheme() {
        return new ThemeSheens(
                "high-contrast",
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
                0xFFFFFFFF,
                0xFF00FFFF
        );
    }

    /// Encodes these sheens as a UTF-8 pipe-separated payload.
    ///
    /// @return the payload bytes
    public byte[] encode() {
        return (name
                + '|'
                + Integer.toUnsignedString(hydrayteArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenyteArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanyteArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothyteArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffyteArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreyteArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteryteArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterymeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundyseArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvyseArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghyseArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxyseArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerayseArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydrayseArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenyseArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanyseArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothyseArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffyseArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreyseArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteryseArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterykeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterypeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterydeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterybeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundyfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvyfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghyfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxyfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerayfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydrayfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenyfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanyfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothyfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffyfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreyfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteryfeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterygeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundyheArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvyheArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghyheArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxyheArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerayheArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydrayheArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenyheArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanyheArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothyheArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffyheArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreyheArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteryheArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundyjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvyjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghyjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxyjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerayjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydrayjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenyjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanyjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothyjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffyjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreyjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteryjeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundyleArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvyleArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghyleArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxyleArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerayleArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydrayleArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenyleArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanyleArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothyleArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffyleArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreyleArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteryleArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundyreArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvyreArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghyreArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxyreArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenyreArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanyreArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothyreArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffyreArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreyreArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteryreArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundormArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvormArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghormArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxormArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraormArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraormArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenormArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanormArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothormArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogrifformArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreormArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterormArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundarmArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvarmArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurgharmArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxarmArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraarmArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraarmArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenarmArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanarmArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemotharmArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffarmArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticorearmArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterarmArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundermArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvermArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghermArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxermArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraermArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraermArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenermArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanermArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothermArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffermArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreermArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterermArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterokeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterekeArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundealArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvealArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghealArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxealArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraealArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraealArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenealArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanealArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothealArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffealArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreealArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterealArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundoalArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvoalArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghoalArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxoalArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraoalArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraoalArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenoalArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanoalArgb, 16)).getBytes(StandardCharsets.UTF_8);
    }

    /// Decodes a payload produced by [`#encode()`].
    ///
    /// @param bytes the resource bytes
    /// @return the sheens
    public static ThemeSheens decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        String[] fields = new String(bytes, StandardCharsets.UTF_8).split("\\|", -1);
        if (fields.length != 254) {
            throw new IllegalArgumentException("Theme sheens must have two-hundred-fifty-four fields");
        }
        return new ThemeSheens(
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
