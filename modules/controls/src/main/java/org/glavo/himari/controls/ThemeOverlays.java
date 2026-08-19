package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Stores named overlay colors that do not fit in [`ThemeTokens`] or [`ThemeSurfaces`].
///
/// Both earlier records are at the JVM constructor limit, so later first-stable
/// named colors live here and encode as a separate theme payload.
///
/// @param name the overlay-pack name
/// @param tinctureArgb the tincture overlay color
/// @param orleArgb the orle overlay color
/// @param fessArgb the fess overlay color
/// @param paleArgb the pale overlay color
/// @param bendArgb the bend overlay color
/// @param chevronArgb the chevron overlay color
/// @param saltireArgb the saltire overlay color
/// @param chiefArgb the chief overlay color
/// @param baseArgb the base overlay color
/// @param dexterArgb the dexter overlay color
/// @param sinisterArgb the sinister overlay color
/// @param gulesArgb the gules overlay color
/// @param argentArgb the argent overlay color
/// @param azureArgb the azure overlay color
/// @param sableArgb the sable overlay color
/// @param purpureArgb the purpure overlay color
/// @param tenneArgb the tenne overlay color
/// @param sanguineArgb the sanguine overlay color
/// @param murreyArgb the murrey overlay color
/// @param ermineArgb the ermine overlay color
/// @param vairArgb the vair overlay color
/// @param potentArgb the potent overlay color
/// @param lozengeArgb the lozenge overlay color
/// @param palyArgb the paly overlay color
/// @param barryArgb the barry overlay color
/// @param gyronnyArgb the gyronny overlay color
/// @param checkyArgb the checky overlay color
/// @param frettyArgb the fretty overlay color
/// @param crusilyArgb the crusily overlay color
/// @param estoileArgb the estoile overlay color
/// @param mulletArgb the mullet overlay color
/// @param roundelArgb the roundel overlay color
/// @param annuletArgb the annulet overlay color
/// @param cantonArgb the canton overlay color
/// @param goutteArgb the goutte overlay color
/// @param floryArgb the flory overlay color
/// @param labelArgb the label overlay color
/// @param pileArgb the pile overlay color
/// @param bordureArgb the bordure overlay color
/// @param tressureArgb the tressure overlay color
/// @param gyronArgb the gyron overlay color
/// @param cotiseArgb the cotise overlay color
/// @param endorseArgb the endorse overlay color
/// @param ribandArgb the riband overlay color
/// @param closetArgb the closet overlay color
/// @param barruletArgb the barrulet overlay color
/// @param escutcheonArgb the escutcheon overlay color
/// @param blazonArgb the blazon overlay color
/// @param hatchmentArgb the hatchment overlay color
/// @param cadencyArgb the cadency overlay color
/// @param mantlingArgb the mantling overlay color
/// @param compartmentArgb the compartment overlay color
/// @param torseArgb the torse overlay color
/// @param helmArgb the helm overlay color
/// @param mottoArgb the motto overlay color
/// @param wreathArgb the wreath overlay color
/// @param badgeArgb the badge overlay color
/// @param chargeArgb the charge overlay color
/// @param ordinaryArgb the ordinary overlay color
/// @param achievementArgb the achievement overlay color
/// @param supporterArgb the supporter overlay color
/// @param crestArgb the crest overlay color
/// @param torseletArgb the torselet overlay color
/// @param liveryArgb the livery overlay color
/// @param pennonArgb the pennon overlay color
/// @param gonfanonArgb the gonfanon overlay color
/// @param tabardArgb the tabard overlay color
/// @param surcoatArgb the surcoat overlay color
/// @param banneretArgb the banneret overlay color
/// @param guidonArgb the guidon overlay color
/// @param ensignArgb the ensign overlay color
/// @param oriflammeArgb the oriflamme overlay color
/// @param vexillumArgb the vexillum overlay color
/// @param labarumArgb the labarum overlay color
/// @param banderoleArgb the banderole overlay color
/// @param streamerArgb the streamer overlay color
/// @param pennoncelleArgb the pennoncelle overlay color
/// @param gonfalonArgb the gonfalon overlay color
/// @param cornetArgb the cornet overlay color
/// @param guidoncelArgb the guidoncel overlay color
/// @param pennoncelArgb the pennoncel overlay color
/// @param banneroleArgb the bannerole overlay color
/// @param fanionArgb the fanion overlay color
/// @param burgeeArgb the burgee overlay color
/// @param jackArgb the jack overlay color
/// @param ensignletArgb the ensignlet overlay color
/// @param gonfanoncelArgb the gonfanoncel overlay color
/// @param banderolletArgb the banderollet overlay color
/// @param vexilloidArgb the vexilloid overlay color
/// @param labarumletArgb the labarumlet overlay color
/// @param oriflammetteArgb the oriflammette overlay color
/// @param guidonetteArgb the guidonette overlay color
/// @param pennonetteArgb the pennonette overlay color
/// @param fanionetteArgb the fanionette overlay color
/// @param burgeeletArgb the burgeelet overlay color
/// @param streamerletArgb the streamerlet overlay color
/// @param cornetletArgb the cornetlet overlay color
/// @param banneroletteArgb the bannerolette overlay color
/// @param jackletArgb the jacklet overlay color
/// @param vexillonArgb the vexillon overlay color
/// @param labaroidArgb the labaroid overlay color
/// @param gonfanonetteArgb the gonfanonette overlay color
/// @param pennonculeArgb the pennoncule overlay color
/// @param guidonculeArgb the guidoncule overlay color
/// @param fanionculeArgb the fanioncule overlay color
/// @param burgeeculeArgb the burgeecule overlay color
/// @param streamerculeArgb the streamercule overlay color
/// @param oriflamculeArgb the oriflamcule overlay color
/// @param vexilluleArgb the vexillule overlay color
/// @param labarumculeArgb the labarumcule overlay color
/// @param cornetculeArgb the cornetcule overlay color
/// @param jackculeArgb the jackcule overlay color
/// @param banneroculeArgb the bannerocule overlay color
/// @param gonfanonculeArgb the gonfanoncule overlay color
/// @param pennonuleArgb the pennonule overlay color
/// @param guidonuleArgb the guidonule overlay color
/// @param fanionuleArgb the fanionule overlay color
/// @param burgeeuleArgb the burgeeule overlay color
/// @param streameruleArgb the streamerule overlay color
/// @param oriflamuleArgb the oriflamule overlay color
/// @param flauncheArgb the flaunche overlay color
/// @param rustreArgb the rustre overlay color
/// @param mascleArgb the mascle overlay color
/// @param fusilArgb the fusil overlay color
/// @param billetArgb the billet overlay color
/// @param masonedArgb the masoned overlay color
/// @param papellonyArgb the papellony overlay color
/// @param plumettyArgb the plumetty overlay color
/// @param quarterlyArgb the quarterly overlay color
/// @param tierceArgb the tierce overlay color
/// @param griffinArgb the griffin overlay color
/// @param unicornArgb the unicorn overlay color
/// @param lioncelArgb the lioncel overlay color
/// @param martletArgb the martlet overlay color
/// @param wyvernArgb the wyvern overlay color
/// @param talbotArgb the talbot overlay color
/// @param pelicanArgb the pelican overlay color
/// @param cockatriceArgb the cockatrice overlay color
/// @param basiliskArgb the basilisk overlay color
/// @param alphynArgb the alphyn overlay color
/// @param yaleArgb the yale overlay color
/// @param keythongArgb the keythong overlay color
/// @param bagwynArgb the bagwyn overlay color
/// @param opinicusArgb the opinicus overlay color
/// @param ypotryllArgb the ypotryll overlay color
/// @param cockatriceletArgb the cockatricelet overlay color
/// @param basiliskletArgb the basilisklet overlay color
/// @param alphynletArgb the alphynlet overlay color
/// @param yaleletArgb the yalelet overlay color
/// @param keythongletArgb the keythonglet overlay color
/// @param bagwynletArgb the bagwynlet overlay color
/// @param opinicusletArgb the opinicuslet overlay color
/// @param lionceauArgb the lionceau overlay color
/// @param martletletArgb the martletlet overlay color
/// @param wyvernletArgb the wyvernlet overlay color
/// @param talbotletArgb the talbotlet overlay color
/// @param camelopardArgb the camelopard overlay color
/// @param bonnaconArgb the bonnacon overlay color
/// @param parandrusArgb the parandrus overlay color
/// @param musimonArgb the musimon overlay color
/// @param cinnamologusArgb the cinnamologus overlay color
/// @param theowArgb the theow overlay color
/// @param calopusArgb the calopus overlay color
/// @param allocamelusArgb the allocamelus overlay color
/// @param manticoreArgb the manticore overlay color
/// @param amphiptereArgb the amphiptere overlay color
/// @param cocatriceArgb the cocatrice overlay color
/// @param leogryphArgb the leogryph overlay color
/// @param hippogriffArgb the hippogriff overlay color
/// @param senmurvArgb the senmurv overlay color
/// @param simurghArgb the simurgh overlay color
/// @param sphinxArgb the sphinx overlay color
/// @param chimeraArgb the chimera overlay color
/// @param hydraArgb the hydra overlay color
/// @param krakenArgb the kraken overlay color
/// @param leviathanArgb the leviathan overlay color
/// @param behemothArgb the behemoth overlay color
/// @param yalehoundArgb the yalehound overlay color
/// @param cocatriceloidArgb the cocatriceloid overlay color
/// @param leogryphletArgb the leogryphlet overlay color
/// @param yalehoundletArgb the yalehoundlet overlay color
/// @param senmurvletArgb the senmurvlet overlay color
/// @param simurghletArgb the simurghlet overlay color
/// @param sphinxletArgb the sphinxlet overlay color
/// @param chimeraletArgb the chimeralet overlay color
/// @param hydraletArgb the hydralet overlay color
/// @param krakenletArgb the krakenlet overlay color
/// @param leviathanletArgb the leviathanlet overlay color
/// @param behemothletArgb the behemothlet overlay color
/// @param hippogriffletArgb the hippogrifflet overlay color
/// @param manticoreletArgb the manticorelet overlay color
/// @param amphipterletArgb the amphipterlet overlay color
/// @param yalehoundoidArgb the yalehoundoid overlay color
/// @param senmurvoidArgb the senmurvoid overlay color
/// @param simurghoidArgb the simurghoid overlay color
/// @param sphinxoidArgb the sphinxoid overlay color
/// @param chimeraoidArgb the chimeraoid overlay color
/// @param hydraoidArgb the hydraoid overlay color
/// @param krakenoidArgb the krakenoid overlay color
/// @param leviathanoidArgb the leviathanoid overlay color
/// @param behemothoidArgb the behemothoid overlay color
/// @param hippogrifoidArgb the hippogrifoid overlay color
/// @param manticoroidArgb the manticoroid overlay color
/// @param amphipteroidArgb the amphipteroid overlay color
/// @param yalehounduleArgb the yalehoundule overlay color
/// @param senmurvuleArgb the senmurvule overlay color
/// @param simurghuleArgb the simurghule overlay color
/// @param sphinxuleArgb the sphinxule overlay color
/// @param chimerauleArgb the chimeraule overlay color
/// @param hydrauleArgb the hydraule overlay color
/// @param krakenuleArgb the krakenule overlay color
/// @param leviathanuleArgb the leviathanule overlay color
/// @param behemothuleArgb the behemothule overlay color
/// @param hippogriffuleArgb the hippogriffule overlay color
/// @param manticoreuleArgb the manticoreule overlay color
/// @param amphipteruleArgb the amphipterule overlay color
/// @param yalehoundineArgb the yalehoundine overlay color
/// @param senmurvineArgb the senmurvine overlay color
/// @param simurghineArgb the simurghine overlay color
/// @param sphinxineArgb the sphinxine overlay color
/// @param chimeraineArgb the chimeraine overlay color
/// @param hydraineArgb the hydraine overlay color
/// @param krakenineArgb the krakenine overlay color
/// @param leviathanineArgb the leviathanine overlay color
/// @param behemothineArgb the behemothine overlay color
/// @param hippogriffineArgb the hippogriffine overlay color
/// @param manticoreineArgb the manticoreine overlay color
/// @param amphipterineArgb the amphipterine overlay color
/// @param yalehoundianArgb the yalehoundian overlay color
/// @param senmurvianArgb the senmurvian overlay color
/// @param simurghianArgb the simurghian overlay color
/// @param sphinxianArgb the sphinxian overlay color
/// @param chimeraianArgb the chimeraian overlay color
/// @param hydraianArgb the hydraian overlay color
/// @param krakenianArgb the krakenian overlay color
/// @param leviathanianArgb the leviathanian overlay color
/// @param behemothianArgb the behemothian overlay color
/// @param hippogriffianArgb the hippogriffian overlay color
/// @param manticoreianArgb the manticoreian overlay color
/// @param amphipterianArgb the amphipterian overlay color
/// @param yalehoundishArgb the yalehoundish overlay color
/// @param senmurvishArgb the senmurvish overlay color
/// @param simurghishArgb the simurghish overlay color
/// @param sphinxishArgb the sphinxish overlay color
/// @param chimeraishArgb the chimeraish overlay color
/// @param hydraishArgb the hydraish overlay color
/// @param krakenishArgb the krakenish overlay color
/// @param leviathanishArgb the leviathanish overlay color
/// @param behemothishArgb the behemothish overlay color
/// @param hippogriffishArgb the hippogriffish overlay color
/// @param manticoreishArgb the manticoreish overlay color
/// @param amphipterishArgb the amphipterish overlay color
/// @param yalehoundaryArgb the yalehoundary overlay color
@NotNullByDefault
public record ThemeOverlays(
        String name,
        int tinctureArgb,
        int orleArgb,
        int fessArgb,
        int paleArgb,
        int bendArgb,
        int chevronArgb,
        int saltireArgb,
        int chiefArgb,
        int baseArgb,
        int dexterArgb,
        int sinisterArgb,
        int gulesArgb,
        int argentArgb,
        int azureArgb,
        int sableArgb,
        int purpureArgb,
        int tenneArgb,
        int sanguineArgb,
        int murreyArgb,
        int ermineArgb,
        int vairArgb,
        int potentArgb,
        int lozengeArgb,
        int palyArgb,
        int barryArgb,
        int gyronnyArgb,
        int checkyArgb,
        int frettyArgb,
        int crusilyArgb,
        int estoileArgb,
        int mulletArgb,
        int roundelArgb,
        int annuletArgb,
        int cantonArgb,
        int goutteArgb,
        int floryArgb,
        int labelArgb,
        int pileArgb,
        int bordureArgb,
        int tressureArgb,
        int gyronArgb,
        int cotiseArgb,
        int endorseArgb,
        int ribandArgb,
        int closetArgb,
        int barruletArgb,
        int escutcheonArgb,
        int blazonArgb,
        int hatchmentArgb,
        int cadencyArgb,
        int mantlingArgb,
        int compartmentArgb,
        int torseArgb,
        int helmArgb,
        int mottoArgb,
        int wreathArgb,
        int badgeArgb,
        int chargeArgb,
        int ordinaryArgb,
        int achievementArgb,
        int supporterArgb,
        int crestArgb,
        int torseletArgb,
        int liveryArgb,
        int pennonArgb,
        int gonfanonArgb,
        int tabardArgb,
        int surcoatArgb,
        int banneretArgb,
        int guidonArgb,
        int ensignArgb,
        int oriflammeArgb,
        int vexillumArgb,
        int labarumArgb,
        int banderoleArgb,
        int streamerArgb,
        int pennoncelleArgb,
        int gonfalonArgb,
        int cornetArgb,
        int guidoncelArgb,
        int pennoncelArgb,
        int banneroleArgb,
        int fanionArgb,
        int burgeeArgb,
        int jackArgb,
        int ensignletArgb,
        int gonfanoncelArgb,
        int banderolletArgb,
        int vexilloidArgb,
        int labarumletArgb,
        int oriflammetteArgb,
        int guidonetteArgb,
        int pennonetteArgb,
        int fanionetteArgb,
        int burgeeletArgb,
        int streamerletArgb,
        int cornetletArgb,
        int banneroletteArgb,
        int jackletArgb,
        int vexillonArgb,
        int labaroidArgb,
        int gonfanonetteArgb,
        int pennonculeArgb,
        int guidonculeArgb,
        int fanionculeArgb,
        int burgeeculeArgb,
        int streamerculeArgb,
        int oriflamculeArgb,
        int vexilluleArgb,
        int labarumculeArgb,
        int cornetculeArgb,
        int jackculeArgb,
        int banneroculeArgb,
        int gonfanonculeArgb,
        int pennonuleArgb,
        int guidonuleArgb,
        int fanionuleArgb,
        int burgeeuleArgb,
        int streameruleArgb,
        int oriflamuleArgb,
        int flauncheArgb,
        int rustreArgb,
        int mascleArgb,
        int fusilArgb,
        int billetArgb,
        int masonedArgb,
        int papellonyArgb,
        int plumettyArgb,
        int quarterlyArgb,
        int tierceArgb,
        int griffinArgb,
        int unicornArgb,
        int lioncelArgb,
        int martletArgb,
        int wyvernArgb,
        int talbotArgb,
        int pelicanArgb,
        int cockatriceArgb,
        int basiliskArgb,
        int alphynArgb,
        int yaleArgb,
        int keythongArgb,
        int bagwynArgb,
        int opinicusArgb,
        int ypotryllArgb,
        int cockatriceletArgb,
        int basiliskletArgb,
        int alphynletArgb,
        int yaleletArgb,
        int keythongletArgb,
        int bagwynletArgb,
        int opinicusletArgb,
        int lionceauArgb,
        int martletletArgb,
        int wyvernletArgb,
        int talbotletArgb,
        int camelopardArgb,
        int bonnaconArgb,
        int parandrusArgb,
        int musimonArgb,
        int cinnamologusArgb,
        int theowArgb,
        int calopusArgb,
        int allocamelusArgb,
        int manticoreArgb,
        int amphiptereArgb,
        int cocatriceArgb,
        int leogryphArgb,
        int hippogriffArgb,
        int senmurvArgb,
        int simurghArgb,
        int sphinxArgb,
        int chimeraArgb,
        int hydraArgb,
        int krakenArgb,
        int leviathanArgb,
        int behemothArgb,
        int yalehoundArgb,
        int cocatriceloidArgb,
        int leogryphletArgb,
        int yalehoundletArgb,
        int senmurvletArgb,
        int simurghletArgb,
        int sphinxletArgb,
        int chimeraletArgb,
        int hydraletArgb,
        int krakenletArgb,
        int leviathanletArgb,
        int behemothletArgb,
        int hippogriffletArgb,
        int manticoreletArgb,
        int amphipterletArgb,
        int yalehoundoidArgb,
        int senmurvoidArgb,
        int simurghoidArgb,
        int sphinxoidArgb,
        int chimeraoidArgb,
        int hydraoidArgb,
        int krakenoidArgb,
        int leviathanoidArgb,
        int behemothoidArgb,
        int hippogrifoidArgb,
        int manticoroidArgb,
        int amphipteroidArgb,
        int yalehounduleArgb,
        int senmurvuleArgb,
        int simurghuleArgb,
        int sphinxuleArgb,
        int chimerauleArgb,
        int hydrauleArgb,
        int krakenuleArgb,
        int leviathanuleArgb,
        int behemothuleArgb,
        int hippogriffuleArgb,
        int manticoreuleArgb,
        int amphipteruleArgb,
        int yalehoundineArgb,
        int senmurvineArgb,
        int simurghineArgb,
        int sphinxineArgb,
        int chimeraineArgb,
        int hydraineArgb,
        int krakenineArgb,
        int leviathanineArgb,
        int behemothineArgb,
        int hippogriffineArgb,
        int manticoreineArgb,
        int amphipterineArgb,
        int yalehoundianArgb,
        int senmurvianArgb,
        int simurghianArgb,
        int sphinxianArgb,
        int chimeraianArgb,
        int hydraianArgb,
        int krakenianArgb,
        int leviathanianArgb,
        int behemothianArgb,
        int hippogriffianArgb,
        int manticoreianArgb,
        int amphipterianArgb,
        int yalehoundishArgb,
        int senmurvishArgb,
        int simurghishArgb,
        int sphinxishArgb,
        int chimeraishArgb,
        int hydraishArgb,
        int krakenishArgb,
        int leviathanishArgb,
        int behemothishArgb,
        int hippogriffishArgb,
        int manticoreishArgb,
        int amphipterishArgb,
        int yalehoundaryArgb
) {
    /// Validates the overlays.
    public ThemeOverlays {
        Objects.requireNonNull(name, "name");
    }

    /// Returns the standard extra overlays.
    ///
    /// @return the overlays
    public static ThemeOverlays standard() {
        return new ThemeOverlays(
                "standard",
                0xFFA5B4FC,
                0xFFC7D2FE,
                0xFFE0E7FF,
                0xFFEEF2FF,
                0xFFF5F3FF,
                0xFFFAF5FF,
                0xFF0C4A6E,
                0xFF075985,
                0xFF0369A1,
                0xFF0284C7,
                0xFF0EA5E9,
                0xFF38BDF8,
                0xFF7DD3FC,
                0xFFBAE6FD,
                0xFFE0F2FE,
                0xFFF0F9FF,
                0xFF082F49,
                0xFFCFFAFE,
                0xFFECFEFF,
                0xFF083344,
                0xFF1C1917,
                0xFF292524,
                0xFF44403C,
                0xFF57534E,
                0xFF78716C,
                0xFFA8A29E,
                0xFFD6D3D1,
                0xFFE7E5E4,
                0xFFF5F5F4,
                0xFFFAFAF9,
                0xFF0F172A,
                0xFF1E293B,
                0xFF334155,
                0xFF475569,
                0xFF64748B,
                0xFF94A3B8,
                0xFFCBD5E1,
                0xFFE2E8F0,
                0xFFF1F5F9,
                0xFFF8FAFC,
                0xFF020617,
                0xFF111827,
                0xFF1F2937,
                0xFF374151,
                0xFF4B5563,
                0xFF6B7280,
                0xFF9CA3AF,
                0xFFD1D5DB,
                0xFFE5E7EB,
                0xFFF3F4F6,
                0xFFF9FAFB,
                0xFF030712,
                0xFF18181B,
                0xFF27272A,
                0xFF3F3F46,
                0xFF52525B,
                0xFF71717A,
                0xFFA1A1AA,
                0xFFD4D4D8,
                0xFFE4E4E7,
                0xFFF4F4F5,
                0xFF09090B,
                0xFF171717,
                0xFF262626,
                0xFF404040,
                0xFF525252,
                0xFF737373,
                0xFFA3A3A3,
                0xFFD4D4D4,
                0xFFE5E5E5,
                0xFFF5F5F5,
                0xFF0A0A0A,
                0xFF0C0A09,
                0xFF1A2E05,
                0xFF365314,
                0xFF3F6212,
                0xFF4D7C0F,
                0xFF65A30D,
                0xFF84CC16,
                0xFFA3E635,
                0xFFBEF264,
                0xFFD9F99D,
                0xFFECFCCB,
                0xFFF7FEE7,
                0xFF052E16,
                0xFF064E3B,
                0xFF065F46,
                0xFF047857,
                0xFF059669,
                0xFF10B981,
                0xFF34D399,
                0xFF6EE7B7,
                0xFFA7F3D0,
                0xFFD1FAE5,
                0xFFECFDF5,
                0xFF022C22,
                0xFF042F2E,
                0xFFCCFBF1,
                0xFFF0FDFA,
                0xFF172554,
                0xFF1E3A8A,
                0xFF1E40AF,
                0xFF1D4ED8,
                0xFF2563EB,
                0xFF3B82F6,
                0xFF60A5FA,
                0xFF93C5FD,
                0xFFBFDBFE,
                0xFF450A0A,
                0xFF7F1D1D,
                0xFF991B1B,
                0xFFB91C1C,
                0xFFDC2626,
                0xFFEF4444,
                0xFFF87171,
                0xFFFCA5A5,
                0xFFFECACA,
                0xFFFEE2E2,
                0xFFFEF2F2,
                0xFF1E3A5F,
                0xFF3B0764,
                0xFF581C87,
                0xFF6B21A8,
                0xFF7E22CE,
                0xFF9333EA,
                0xFFA855F7,
                0xFF500724,
                0xFF9D174D,
                0xFFBE185D,
                0xFFDB2777,
                0xFFEC4899,
                0xFFF472B6,
                0xFFF9A8D4,
                0xFFFBCFE8,
                0xFFFCE7F3,
                0xFFFDF2F8,
                0xFF831843,
                0xFFFFEDD5,
                0xFF422006,
                0xFF713F12,
                0xFF854D0E,
                0xFFA16207,
                0xFFCA8A04,
                0xFFEAB308,
                0xFFFACC15,
                0xFFFDE047,
                0xFFFEF08A,
                0xFFFEF9C3,
                0xFFFEFCE8,
                0xFFDCFCE7,
                0xFFF0FDF4,
                0xFFDBEAFE,
                0xFFEFF6FF,
                0xFF0B1220,
                0xFF121212,
                0xFF2A2A2A,
                0xFF3A3A3A,
                0xFF4A4A4A,
                0xFF101828,
                0xFF1D2939,
                0xFF344054,
                0xFF475467,
                0xFF667085,
                0xFF98A2B3,
                0xFFD0D5DD,
                0xFFEAECF0,
                0xFFF2F4F7,
                0xFF0C111D,
                0xFF161B26,
                0xFF1F242F,
                0xFF2B303B,
                0xFF3F444E,
                0xFF010203,
                0xFF102030,
                0xFF203040,
                0xFF304050,
                0xFF405060,
                0xFF506070,
                0xFF607080,
                0xFF708090,
                0xFF8090A0,
                0xFF90A0B0,
                0xFFA0B0C0,
                0xFFB0C0D0,
                0xFFC0D0E0,
                0xFFD0E0F0,
                0xFFE0F0FF,
                0xFF112233,
                0xFF223344,
                0xFF334455,
                0xFF445566,
                0xFF556677,
                0xFF667788,
                0xFF778899,
                0xFF8899AA,
                0xFF99AABB,
                0xFFAABBCC,
                0xFFBBCCDD,
                0xFFCCDDEE,
                0xFFDDEEFF,
                0xFF001122,
                0xFF002233,
                0xFF003344,
                0xFF004455,
                0xFF005566,
                0xFF006677,
                0xFF007788,
                0xFF008899,
                0xFF0099AA,
                0xFF00AABB,
                0xFF00BBCC,
                0xFF00CCDD,
                0xFF00DDEE,
                0xFF00EEFF,
                0xFF110011,
                0xFF220022,
                0xFF330033,
                0xFF440044,
                0xFF550055,
                0xFF660066,
                0xFF770077,
                0xFF880088,
                0xFF990099,
                0xFFAA00AA,
                0xFFBB00BB,
                0xFFCC00CC,
                0xFFDD00DD,
                0xFFEE00EE,
                0xFF111122,
                0xFF222233,
                0xFF333344,
                0xFF444455,
                0xFF555566,
                0xFF666677,
                0xFF777788,
                0xFF888899,
                0xFF9999AA,
                0xFFAAAABB,
                0xFFBBBBCC,
                0xFFCCCCDD,
                0xFFDDDDEE,
                0xFFEEEEFF,
                0xFF112211,
                0xFF223322,
                0xFF334433,
                0xFF445544,
                0xFF556655,
                0xFF667766,
                0xFF778877,
                0xFF889988,
                0xFF99AA99,
                0xFFAABBAA,
                0xFFBBCCBB
        );
    }

    /// Returns the high-contrast extra overlays.
    ///
    /// @return the overlays
    public static ThemeOverlays highContrastTheme() {
        return new ThemeOverlays(
                "high-contrast",
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
                0xFF00FFFF,
                0xFF808080,
                0xFF00FF00,
                0xFFFFFF00
        );
    }

    /// Encodes these overlays as a first-stable theme resource payload.
    ///
    /// @return UTF-8 fields separated by `|`
    public byte[] encode() {
        return (name
                + '|'
                + Integer.toUnsignedString(tinctureArgb, 16)
                + '|'
                + Integer.toUnsignedString(orleArgb, 16)
                + '|'
                + Integer.toUnsignedString(fessArgb, 16)
                + '|'
                + Integer.toUnsignedString(paleArgb, 16)
                + '|'
                + Integer.toUnsignedString(bendArgb, 16)
                + '|'
                + Integer.toUnsignedString(chevronArgb, 16)
                + '|'
                + Integer.toUnsignedString(saltireArgb, 16)
                + '|'
                + Integer.toUnsignedString(chiefArgb, 16)
                + '|'
                + Integer.toUnsignedString(baseArgb, 16)
                + '|'
                + Integer.toUnsignedString(dexterArgb, 16)
                + '|'
                + Integer.toUnsignedString(sinisterArgb, 16)
                + '|'
                + Integer.toUnsignedString(gulesArgb, 16)
                + '|'
                + Integer.toUnsignedString(argentArgb, 16)
                + '|'
                + Integer.toUnsignedString(azureArgb, 16)
                + '|'
                + Integer.toUnsignedString(sableArgb, 16)
                + '|'
                + Integer.toUnsignedString(purpureArgb, 16)
                + '|'
                + Integer.toUnsignedString(tenneArgb, 16)
                + '|'
                + Integer.toUnsignedString(sanguineArgb, 16)
                + '|'
                + Integer.toUnsignedString(murreyArgb, 16)
                + '|'
                + Integer.toUnsignedString(ermineArgb, 16)
                + '|'
                + Integer.toUnsignedString(vairArgb, 16)
                + '|'
                + Integer.toUnsignedString(potentArgb, 16)
                + '|'
                + Integer.toUnsignedString(lozengeArgb, 16)
                + '|'
                + Integer.toUnsignedString(palyArgb, 16)
                + '|'
                + Integer.toUnsignedString(barryArgb, 16)
                + '|'
                + Integer.toUnsignedString(gyronnyArgb, 16)
                + '|'
                + Integer.toUnsignedString(checkyArgb, 16)
                + '|'
                + Integer.toUnsignedString(frettyArgb, 16)
                + '|'
                + Integer.toUnsignedString(crusilyArgb, 16)
                + '|'
                + Integer.toUnsignedString(estoileArgb, 16)
                + '|'
                + Integer.toUnsignedString(mulletArgb, 16)
                + '|'
                + Integer.toUnsignedString(roundelArgb, 16)
                + '|'
                + Integer.toUnsignedString(annuletArgb, 16)
                + '|'
                + Integer.toUnsignedString(cantonArgb, 16)
                + '|'
                + Integer.toUnsignedString(goutteArgb, 16)
                + '|'
                + Integer.toUnsignedString(floryArgb, 16)
                + '|'
                + Integer.toUnsignedString(labelArgb, 16)
                + '|'
                + Integer.toUnsignedString(pileArgb, 16)
                + '|'
                + Integer.toUnsignedString(bordureArgb, 16)
                + '|'
                + Integer.toUnsignedString(tressureArgb, 16)
                + '|'
                + Integer.toUnsignedString(gyronArgb, 16)
                + '|'
                + Integer.toUnsignedString(cotiseArgb, 16)
                + '|'
                + Integer.toUnsignedString(endorseArgb, 16)
                + '|'
                + Integer.toUnsignedString(ribandArgb, 16)
                + '|'
                + Integer.toUnsignedString(closetArgb, 16)
                + '|'
                + Integer.toUnsignedString(barruletArgb, 16)
                + '|'
                + Integer.toUnsignedString(escutcheonArgb, 16)
                + '|'
                + Integer.toUnsignedString(blazonArgb, 16)
                + '|'
                + Integer.toUnsignedString(hatchmentArgb, 16)
                + '|'
                + Integer.toUnsignedString(cadencyArgb, 16)
                + '|'
                + Integer.toUnsignedString(mantlingArgb, 16)
                + '|'
                + Integer.toUnsignedString(compartmentArgb, 16)
                + '|'
                + Integer.toUnsignedString(torseArgb, 16)
                + '|'
                + Integer.toUnsignedString(helmArgb, 16)
                + '|'
                + Integer.toUnsignedString(mottoArgb, 16)
                + '|'
                + Integer.toUnsignedString(wreathArgb, 16)
                + '|'
                + Integer.toUnsignedString(badgeArgb, 16)
                + '|'
                + Integer.toUnsignedString(chargeArgb, 16)
                + '|'
                + Integer.toUnsignedString(ordinaryArgb, 16)
                + '|'
                + Integer.toUnsignedString(achievementArgb, 16)
                + '|'
                + Integer.toUnsignedString(supporterArgb, 16)
                + '|'
                + Integer.toUnsignedString(crestArgb, 16)
                + '|'
                + Integer.toUnsignedString(torseletArgb, 16)
                + '|'
                + Integer.toUnsignedString(liveryArgb, 16)
                + '|'
                + Integer.toUnsignedString(pennonArgb, 16)
                + '|'
                + Integer.toUnsignedString(gonfanonArgb, 16)
                + '|'
                + Integer.toUnsignedString(tabardArgb, 16)
                + '|'
                + Integer.toUnsignedString(surcoatArgb, 16)
                + '|'
                + Integer.toUnsignedString(banneretArgb, 16)
                + '|'
                + Integer.toUnsignedString(guidonArgb, 16)
                + '|'
                + Integer.toUnsignedString(ensignArgb, 16)
                + '|'
                + Integer.toUnsignedString(oriflammeArgb, 16)
                + '|'
                + Integer.toUnsignedString(vexillumArgb, 16)
                + '|'
                + Integer.toUnsignedString(labarumArgb, 16)
                + '|'
                + Integer.toUnsignedString(banderoleArgb, 16)
                + '|'
                + Integer.toUnsignedString(streamerArgb, 16)
                + '|'
                + Integer.toUnsignedString(pennoncelleArgb, 16)
                + '|'
                + Integer.toUnsignedString(gonfalonArgb, 16)
                + '|'
                + Integer.toUnsignedString(cornetArgb, 16)
                + '|'
                + Integer.toUnsignedString(guidoncelArgb, 16)
                + '|'
                + Integer.toUnsignedString(pennoncelArgb, 16)
                + '|'
                + Integer.toUnsignedString(banneroleArgb, 16)
                + '|'
                + Integer.toUnsignedString(fanionArgb, 16)
                + '|'
                + Integer.toUnsignedString(burgeeArgb, 16)
                + '|'
                + Integer.toUnsignedString(jackArgb, 16)
                + '|'
                + Integer.toUnsignedString(ensignletArgb, 16)
                + '|'
                + Integer.toUnsignedString(gonfanoncelArgb, 16)
                + '|'
                + Integer.toUnsignedString(banderolletArgb, 16)
                + '|'
                + Integer.toUnsignedString(vexilloidArgb, 16)
                + '|'
                + Integer.toUnsignedString(labarumletArgb, 16)
                + '|'
                + Integer.toUnsignedString(oriflammetteArgb, 16)
                + '|'
                + Integer.toUnsignedString(guidonetteArgb, 16)
                + '|'
                + Integer.toUnsignedString(pennonetteArgb, 16)
                + '|'
                + Integer.toUnsignedString(fanionetteArgb, 16)
                + '|'
                + Integer.toUnsignedString(burgeeletArgb, 16)
                + '|'
                + Integer.toUnsignedString(streamerletArgb, 16)
                + '|'
                + Integer.toUnsignedString(cornetletArgb, 16)
                + '|'
                + Integer.toUnsignedString(banneroletteArgb, 16)
                + '|'
                + Integer.toUnsignedString(jackletArgb, 16)
                + '|'
                + Integer.toUnsignedString(vexillonArgb, 16)
                + '|'
                + Integer.toUnsignedString(labaroidArgb, 16)
                + '|'
                + Integer.toUnsignedString(gonfanonetteArgb, 16)
                + '|'
                + Integer.toUnsignedString(pennonculeArgb, 16)
                + '|'
                + Integer.toUnsignedString(guidonculeArgb, 16)
                + '|'
                + Integer.toUnsignedString(fanionculeArgb, 16)
                + '|'
                + Integer.toUnsignedString(burgeeculeArgb, 16)
                + '|'
                + Integer.toUnsignedString(streamerculeArgb, 16)
                + '|'
                + Integer.toUnsignedString(oriflamculeArgb, 16)
                + '|'
                + Integer.toUnsignedString(vexilluleArgb, 16)
                + '|'
                + Integer.toUnsignedString(labarumculeArgb, 16)
                + '|'
                + Integer.toUnsignedString(cornetculeArgb, 16)
                + '|'
                + Integer.toUnsignedString(jackculeArgb, 16)
                + '|'
                + Integer.toUnsignedString(banneroculeArgb, 16)
                + '|'
                + Integer.toUnsignedString(gonfanonculeArgb, 16)
                + '|'
                + Integer.toUnsignedString(pennonuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(guidonuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(fanionuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(burgeeuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(streameruleArgb, 16)
                + '|'
                + Integer.toUnsignedString(oriflamuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(flauncheArgb, 16)
                + '|'
                + Integer.toUnsignedString(rustreArgb, 16)
                + '|'
                + Integer.toUnsignedString(mascleArgb, 16)
                + '|'
                + Integer.toUnsignedString(fusilArgb, 16)
                + '|'
                + Integer.toUnsignedString(billetArgb, 16)
                + '|'
                + Integer.toUnsignedString(masonedArgb, 16)
                + '|'
                + Integer.toUnsignedString(papellonyArgb, 16)
                + '|'
                + Integer.toUnsignedString(plumettyArgb, 16)
                + '|'
                + Integer.toUnsignedString(quarterlyArgb, 16)
                + '|'
                + Integer.toUnsignedString(tierceArgb, 16)
                + '|'
                + Integer.toUnsignedString(griffinArgb, 16)
                + '|'
                + Integer.toUnsignedString(unicornArgb, 16)
                + '|'
                + Integer.toUnsignedString(lioncelArgb, 16)
                + '|'
                + Integer.toUnsignedString(martletArgb, 16)
                + '|'
                + Integer.toUnsignedString(wyvernArgb, 16)
                + '|'
                + Integer.toUnsignedString(talbotArgb, 16)
                + '|'
                + Integer.toUnsignedString(pelicanArgb, 16)
                + '|'
                + Integer.toUnsignedString(cockatriceArgb, 16)
                + '|'
                + Integer.toUnsignedString(basiliskArgb, 16)
                + '|'
                + Integer.toUnsignedString(alphynArgb, 16)
                + '|'
                + Integer.toUnsignedString(yaleArgb, 16)
                + '|'
                + Integer.toUnsignedString(keythongArgb, 16)
                + '|'
                + Integer.toUnsignedString(bagwynArgb, 16)
                + '|'
                + Integer.toUnsignedString(opinicusArgb, 16)
                + '|'
                + Integer.toUnsignedString(ypotryllArgb, 16)
                + '|'
                + Integer.toUnsignedString(cockatriceletArgb, 16)
                + '|'
                + Integer.toUnsignedString(basiliskletArgb, 16)
                + '|'
                + Integer.toUnsignedString(alphynletArgb, 16)
                + '|'
                + Integer.toUnsignedString(yaleletArgb, 16)
                + '|'
                + Integer.toUnsignedString(keythongletArgb, 16)
                + '|'
                + Integer.toUnsignedString(bagwynletArgb, 16)
                + '|'
                + Integer.toUnsignedString(opinicusletArgb, 16)
                + '|'
                + Integer.toUnsignedString(lionceauArgb, 16)
                + '|'
                + Integer.toUnsignedString(martletletArgb, 16)
                + '|'
                + Integer.toUnsignedString(wyvernletArgb, 16)
                + '|'
                + Integer.toUnsignedString(talbotletArgb, 16)
                + '|'
                + Integer.toUnsignedString(camelopardArgb, 16)
                + '|'
                + Integer.toUnsignedString(bonnaconArgb, 16)
                + '|'
                + Integer.toUnsignedString(parandrusArgb, 16)
                + '|'
                + Integer.toUnsignedString(musimonArgb, 16)
                + '|'
                + Integer.toUnsignedString(cinnamologusArgb, 16)
                + '|'
                + Integer.toUnsignedString(theowArgb, 16)
                + '|'
                + Integer.toUnsignedString(calopusArgb, 16)
                + '|'
                + Integer.toUnsignedString(allocamelusArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphiptereArgb, 16)
                + '|'
                + Integer.toUnsignedString(cocatriceArgb, 16)
                + '|'
                + Integer.toUnsignedString(leogryphArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundArgb, 16)
                + '|'
                + Integer.toUnsignedString(cocatriceloidArgb, 16)
                + '|'
                + Integer.toUnsignedString(leogryphletArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundletArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvletArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghletArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxletArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraletArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraletArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenletArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanletArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothletArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffletArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreletArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterletArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundoidArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvoidArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghoidArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxoidArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraoidArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraoidArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenoidArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanoidArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothoidArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogrifoidArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoroidArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteroidArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehounduleArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimerauleArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydrauleArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreuleArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipteruleArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundineArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvineArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghineArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxineArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraineArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraineArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenineArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanineArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothineArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffineArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreineArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterineArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundianArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvianArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghianArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxianArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraianArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraianArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenianArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanianArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothianArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffianArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreianArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterianArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundishArgb, 16)
                + '|'
                + Integer.toUnsignedString(senmurvishArgb, 16)
                + '|'
                + Integer.toUnsignedString(simurghishArgb, 16)
                + '|'
                + Integer.toUnsignedString(sphinxishArgb, 16)
                + '|'
                + Integer.toUnsignedString(chimeraishArgb, 16)
                + '|'
                + Integer.toUnsignedString(hydraishArgb, 16)
                + '|'
                + Integer.toUnsignedString(krakenishArgb, 16)
                + '|'
                + Integer.toUnsignedString(leviathanishArgb, 16)
                + '|'
                + Integer.toUnsignedString(behemothishArgb, 16)
                + '|'
                + Integer.toUnsignedString(hippogriffishArgb, 16)
                + '|'
                + Integer.toUnsignedString(manticoreishArgb, 16)
                + '|'
                + Integer.toUnsignedString(amphipterishArgb, 16)
                + '|'
                + Integer.toUnsignedString(yalehoundaryArgb, 16)).getBytes(StandardCharsets.UTF_8);
    }

    /// Decodes a payload produced by [`#encode()`].
    ///
    /// @param bytes the resource bytes
    /// @return the overlays
    public static ThemeOverlays decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        String[] fields = new String(bytes, StandardCharsets.UTF_8).split("\\|", -1);
        if (fields.length != 254) {
            throw new IllegalArgumentException("Theme overlays must have two-hundred-fifty-four fields");
        }
        return new ThemeOverlays(
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
