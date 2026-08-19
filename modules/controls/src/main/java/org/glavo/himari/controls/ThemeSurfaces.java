package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Stores named surface colors that do not fit in [`ThemeTokens`].
///
/// The [`ThemeTokens`] record constructor is at the JVM parameter limit, so later
/// first-stable surfaces live here and encode as a separate theme payload.
///
/// @param name the surface-pack name
/// @param groveArgb the grove surface color
/// @param gladeArgb the glade surface color
/// @param thicketArgb the thicket surface color
/// @param brakeArgb the brake surface color
/// @param covertArgb the covert surface color
/// @param rideArgb the ride surface color
/// @param wealdArgb the weald surface color
/// @param chaseArgb the chase surface color
/// @param parkArgb the park surface color
/// @param coppiceArgb the coppice surface color
/// @param frithArgb the frith surface color
/// @param woodArgb the wood surface color
/// @param hangerArgb the hanger surface color
/// @param boskArgb the bosk surface color
/// @param standArgb the stand surface color
/// @param toftArgb the toft surface color
/// @param croftArgb the croft surface color
/// @param closeArgb the close surface color
/// @param leyArgb the ley surface color
/// @param meadArgb the mead surface color
/// @param pastureArgb the pasture surface color
/// @param leaArgb the lea surface color
/// @param swardArgb the sward surface color
/// @param downlandArgb the downland surface color
/// @param fieldArgb the field surface color
/// @param paddockArgb the paddock surface color
/// @param meadowArgb the meadow surface color
/// @param greenArgb the green surface color
/// @param commonArgb the common surface color
/// @param acreArgb the acre surface color
/// @param garthArgb the garth surface color
/// @param yardArgb the yard surface color
/// @param plotArgb the plot surface color
/// @param allotmentArgb the allotment surface color
/// @param gardenArgb the garden surface color
/// @param orchardArgb the orchard surface color
/// @param vineyardArgb the vineyard surface color
/// @param warrenArgb the warren surface color
/// @param hopfieldArgb the hopfield surface color
/// @param vineryArgb the vinery surface color
/// @param trellisArgb the trellis surface color
/// @param arbourArgb the arbour surface color
/// @param pergolaArgb the pergola surface color
/// @param espalierArgb the espalier surface color
/// @param palisadeArgb the palisade surface color
/// @param hedgerowArgb the hedgerow surface color
/// @param bowerArgb the bower surface color
/// @param alleyArgb the alley surface color
/// @param pleachArgb the pleach surface color
/// @param lychgateArgb the lychgate surface color
/// @param wicketArgb the wicket surface color
/// @param stileArgb the stile surface color
/// @param posternArgb the postern surface color
/// @param turnstileArgb the turnstile surface color
/// @param hatchArgb the hatch surface color
/// @param lodgeArgb the lodge surface color
/// @param porchArgb the porch surface color
/// @param byreArgb the byre surface color
/// @param shipponArgb the shippon surface color
/// @param linhayArgb the linhay surface color
/// @param bartonArgb the barton surface color
/// @param milkingArgb the milking surface color
/// @param shippenArgb the shippen surface color
/// @param dairyArgb the dairy surface color
/// @param parlourArgb the parlour surface color
/// @param stallArgb the stall surface color
/// @param foldArgb the fold surface color
/// @param styArgb the sty surface color
/// @param coopArgb the coop surface color
/// @param loftArgb the loft surface color
/// @param granaryArgb the granary surface color
/// @param cribArgb the crib surface color
/// @param siloArgb the silo surface color
/// @param barnArgb the barn surface color
/// @param hovelArgb the hovel surface color
/// @param mistalArgb the mistal surface color
/// @param rickArgb the rick surface color
/// @param poundArgb the pound surface color
/// @param lairageArgb the lairage surface color
/// @param shedArgb the shed surface color
/// @param henhouseArgb the henhouse surface color
/// @param piggeryArgb the piggery surface color
/// @param middenArgb the midden surface color
/// @param dutchArgb the dutch surface color
/// @param bothyArgb the bothy surface color
/// @param shielingArgb the shieling surface color
/// @param cotArgb the cot surface color
/// @param kirkArgb the kirk surface color
/// @param manseArgb the manse surface color
/// @param clachanArgb the clachan surface color
/// @param bothanArgb the bothan surface color
/// @param townshipArgb the township surface color
/// @param fermtounArgb the fermtoun surface color
/// @param hamletArgb the hamlet surface color
/// @param thorpArgb the thorp surface color
/// @param burghArgb the burgh surface color
/// @param steadingArgb the steading surface color
/// @param grangeArgb the grange surface color
/// @param milntonArgb the milnton surface color
/// @param fermholdArgb the fermhold surface color
/// @param inbyeArgb the inbye surface color
/// @param outbyeArgb the outbye surface color
/// @param outgangArgb the outgang surface color
/// @param loaningArgb the loaning surface color
/// @param riggArgb the rigg surface color
/// @param loanendArgb the loanend surface color
/// @param baulkArgb the baulk surface color
/// @param loanheadArgb the loanhead surface color
/// @param headrigArgb the headrig surface color
/// @param rigsideArgb the rigside surface color
/// @param loanfootArgb the loanfoot surface color
/// @param headlandArgb the headland surface color
/// @param rigendArgb the rigend surface color
/// @param loanmidArgb the loanmid surface color
/// @param furrowArgb the furrow surface color
/// @param selionArgb the selion surface color
/// @param furlongArgb the furlong surface color
/// @param hideArgb the hide surface color
/// @param virgateArgb the virgate surface color
/// @param carucateArgb the carucate surface color
/// @param bovateArgb the bovate surface color
/// @param oxgangArgb the oxgang surface color
/// @param nookArgb the nook surface color
/// @param goadArgb the goad surface color
/// @param roodArgb the rood surface color
/// @param perchArgb the perch surface color
/// @param ferlingArgb the ferling surface color
/// @param oxlandArgb the oxland surface color
/// @param plowgateArgb the plowgate surface color
/// @param morgenArgb the morgen surface color
/// @param jugerumArgb the jugerum surface color
/// @param arpentArgb the arpent surface color
/// @param sulungArgb the sulung surface color
/// @param hidageArgb the hidage surface color
/// @param geldArgb the geld surface color
/// @param carucageArgb the carucage surface color
/// @param jochArgb the joch surface color
/// @param iugumArgb the iugum surface color
/// @param tenementArgb the tenement surface color
/// @param goreArgb the gore surface color
/// @param buttArgb the butt surface color
/// @param stripArgb the strip surface color
/// @param poleArgb the pole surface color
/// @param chainArgb the chain surface color
/// @param messuageArgb the messuage surface color
/// @param demesneArgb the demesne surface color
/// @param glebeArgb the glebe surface color
/// @param tithingArgb the tithing surface color
/// @param wapentakeArgb the wapentake surface color
/// @param hundredArgb the hundred surface color
/// @param burgageArgb the burgage surface color
/// @param assartArgb the assart surface color
/// @param intakeArgb the intake surface color
/// @param intackArgb the intack surface color
/// @param journalArgb the journal surface color
/// @param purlieuArgb the purlieu surface color
/// @param wasteArgb the waste surface color
/// @param severalArgb the several surface color
/// @param lammasArgb the lammas surface color
/// @param escheatArgb the escheat surface color
/// @param assartageArgb the assartage surface color
/// @param shackArgb the shack surface color
/// @param socageArgb the socage surface color
/// @param bordarArgb the bordar surface color
/// @param villeinArgb the villein surface color
/// @param boonworkArgb the boonwork surface color
/// @param cotsetleArgb the cotsetle surface color
/// @param frankalmoinArgb the frankalmoin surface color
/// @param merchetArgb the merchet surface color
/// @param heriotArgb the heriot surface color
/// @param tallageArgb the tallage surface color
/// @param scutageArgb the scutage surface color
/// @param gavelArgb the gavel surface color
/// @param chevageArgb the chevage surface color
/// @param aidArgb the aid surface color
/// @param pannageArgb the pannage surface color
/// @param murageArgb the murage surface color
/// @param pontageArgb the pontage surface color
/// @param pavageArgb the pavage surface color
/// @param lastageArgb the lastage surface color
/// @param pickageArgb the pickage surface color
/// @param tronageArgb the tronage surface color
/// @param wharfageArgb the wharfage surface color
/// @param amercementArgb the amercement surface color
/// @param cranageArgb the cranage surface color
/// @param keelageArgb the keelage surface color
/// @param anchorageArgb the anchorage surface color
/// @param groundageArgb the groundage surface color
/// @param scavageArgb the scavage surface color
/// @param primageArgb the primage surface color
/// @param stowageArgb the stowage surface color
/// @param lighterageArgb the lighterage surface color
/// @param demurrageArgb the demurrage surface color
/// @param salvageArgb the salvage surface color
/// @param moorageArgb the moorage surface color
/// @param quayageArgb the quayage surface color
/// @param lockageArgb the lockage surface color
/// @param pierageArgb the pierage surface color
/// @param dockageArgb the dockage surface color
/// @param cellarageArgb the cellarage surface color
/// @param gallonageArgb the gallonage surface color
/// @param tunnageArgb the tunnage surface color
/// @param seigniorageArgb the seigniorage surface color
/// @param poundageArgb the poundage surface color
/// @param brokerageArgb the brokerage surface color
/// @param pilotageArgb the pilotage surface color
/// @param havageArgb the havage surface color
/// @param beamageArgb the beamage surface color
/// @param sternageArgb the sternage surface color
/// @param ferriageArgb the ferriage surface color
/// @param meterageArgb the meterage surface color
/// @param ullageArgb the ullage surface color
/// @param breakageArgb the breakage surface color
/// @param cartageArgb the cartage surface color
/// @param haulageArgb the haulage surface color
/// @param freightageArgb the freightage surface color
/// @param drayageArgb the drayage surface color
/// @param porterageArgb the porterage surface color
/// @param leakageArgb the leakage surface color
/// @param packageArgb the package surface color
/// @param storageArgb the storage surface color
/// @param averageArgb the average surface color
/// @param wreckageArgb the wreckage surface color
/// @param wastageArgb the wastage surface color
/// @param spoilageArgb the spoilage surface color
/// @param cordageArgb the cordage surface color
/// @param mileageArgb the mileage surface color
/// @param postageArgb the postage surface color
/// @param tonnageArgb the tonnage surface color
/// @param coinageArgb the coinage surface color
/// @param yardageArgb the yardage surface color
/// @param acreageArgb the acreage surface color
/// @param windageArgb the windage surface color
/// @param waterageArgb the waterage surface color
/// @param tankageArgb the tankage surface color
/// @param factorageArgb the factorage surface color
/// @param customageArgb the customage surface color
/// @param cooperageArgb the cooperage surface color
/// @param gaugageArgb the gaugage surface color
/// @param lockerageArgb the lockerage surface color
/// @param scowageArgb the scowage surface color
/// @param bargeageArgb the bargeage surface color
/// @param boatageArgb the boatage surface color
/// @param craftageArgb the craftage surface color
/// @param shippageArgb the shippage surface color
/// @param weirageArgb the weirage surface color
/// @param millageArgb the millage surface color
/// @param sluiceageArgb the sluiceage surface color
/// @param portageArgb the portage surface color
/// @param wagonageArgb the wagonage surface color
/// @param voyageArgb the voyage surface color
/// @param tollageArgb the tollage surface color
/// @param carriageArgb the carriage surface color
@NotNullByDefault
public record ThemeSurfaces(
        String name,
        int groveArgb,
        int gladeArgb,
        int thicketArgb,
        int brakeArgb,
        int covertArgb,
        int rideArgb,
        int wealdArgb,
        int chaseArgb,
        int parkArgb,
        int coppiceArgb,
        int frithArgb,
        int woodArgb,
        int hangerArgb,
        int boskArgb,
        int standArgb,
        int toftArgb,
        int croftArgb,
        int closeArgb,
        int leyArgb,
        int meadArgb,
        int pastureArgb,
        int leaArgb,
        int swardArgb,
        int downlandArgb,
        int fieldArgb,
        int paddockArgb,
        int meadowArgb,
        int greenArgb,
        int commonArgb,
        int acreArgb,
        int garthArgb,
        int yardArgb,
        int plotArgb,
        int allotmentArgb,
        int gardenArgb,
        int orchardArgb,
        int vineyardArgb,
        int warrenArgb,
        int hopfieldArgb,
        int vineryArgb,
        int trellisArgb,
        int arbourArgb,
        int pergolaArgb,
        int espalierArgb,
        int palisadeArgb,
        int hedgerowArgb,
        int bowerArgb,
        int alleyArgb,
        int pleachArgb,
        int lychgateArgb,
        int wicketArgb,
        int stileArgb,
        int posternArgb,
        int turnstileArgb,
        int hatchArgb,
        int lodgeArgb,
        int porchArgb,
        int byreArgb,
        int shipponArgb,
        int linhayArgb,
        int bartonArgb,
        int milkingArgb,
        int shippenArgb,
        int dairyArgb,
        int parlourArgb,
        int stallArgb,
        int foldArgb,
        int styArgb,
        int coopArgb,
        int loftArgb,
        int granaryArgb,
        int cribArgb,
        int siloArgb,
        int barnArgb,
        int hovelArgb,
        int mistalArgb,
        int rickArgb,
        int poundArgb,
        int lairageArgb,
        int shedArgb,
        int henhouseArgb,
        int piggeryArgb,
        int middenArgb,
        int dutchArgb,
        int bothyArgb,
        int shielingArgb,
        int cotArgb,
        int kirkArgb,
        int manseArgb,
        int clachanArgb,
        int bothanArgb,
        int townshipArgb,
        int fermtounArgb,
        int hamletArgb,
        int thorpArgb,
        int burghArgb,
        int steadingArgb,
        int grangeArgb,
        int milntonArgb,
        int fermholdArgb,
        int inbyeArgb,
        int outbyeArgb,
        int outgangArgb,
        int loaningArgb,
        int riggArgb,
        int loanendArgb,
        int baulkArgb,
        int loanheadArgb,
        int headrigArgb,
        int rigsideArgb,
        int loanfootArgb,
        int headlandArgb,
        int rigendArgb,
        int loanmidArgb,
        int furrowArgb,
        int selionArgb,
        int furlongArgb,
        int hideArgb,
        int virgateArgb,
        int carucateArgb,
        int bovateArgb,
        int oxgangArgb,
        int nookArgb,
        int goadArgb,
        int roodArgb,
        int perchArgb,
        int ferlingArgb,
        int oxlandArgb,
        int plowgateArgb,
        int morgenArgb,
        int jugerumArgb,
        int arpentArgb,
        int sulungArgb,
        int hidageArgb,
        int geldArgb,
        int carucageArgb,
        int jochArgb,
        int iugumArgb,
        int tenementArgb,
        int goreArgb,
        int buttArgb,
        int stripArgb,
        int poleArgb,
        int chainArgb,
        int messuageArgb,
        int demesneArgb,
        int glebeArgb,
        int tithingArgb,
        int wapentakeArgb,
        int hundredArgb,
        int burgageArgb,
        int assartArgb,
        int intakeArgb,
        int intackArgb,
        int journalArgb,
        int purlieuArgb,
        int wasteArgb,
        int severalArgb,
        int lammasArgb,
        int escheatArgb,
        int assartageArgb,
        int shackArgb,
        int socageArgb,
        int bordarArgb,
        int villeinArgb,
        int boonworkArgb,
        int cotsetleArgb,
        int frankalmoinArgb,
        int merchetArgb,
        int heriotArgb,
        int tallageArgb,
        int scutageArgb,
        int gavelArgb,
        int chevageArgb,
        int aidArgb,
        int pannageArgb,
        int murageArgb,
        int pontageArgb,
        int pavageArgb,
        int lastageArgb,
        int pickageArgb,
        int tronageArgb,
        int wharfageArgb,
        int amercementArgb,
        int cranageArgb,
        int keelageArgb,
        int anchorageArgb,
        int groundageArgb,
        int scavageArgb,
        int primageArgb,
        int stowageArgb,
        int lighterageArgb,
        int demurrageArgb,
        int salvageArgb,
        int moorageArgb,
        int quayageArgb,
        int lockageArgb,
        int pierageArgb,
        int dockageArgb,
        int cellarageArgb,
        int gallonageArgb,
        int tunnageArgb,
        int seigniorageArgb,
        int poundageArgb,
        int brokerageArgb,
        int pilotageArgb,
        int havageArgb,
        int beamageArgb,
        int sternageArgb,
        int ferriageArgb,
        int meterageArgb,
        int ullageArgb,
        int breakageArgb,
        int cartageArgb,
        int haulageArgb,
        int freightageArgb,
        int drayageArgb,
        int porterageArgb,
        int leakageArgb,
        int packageArgb,
        int storageArgb,
        int averageArgb,
        int wreckageArgb,
        int wastageArgb,
        int spoilageArgb,
        int cordageArgb,
        int mileageArgb,
        int postageArgb,
        int tonnageArgb,
        int coinageArgb,
        int yardageArgb,
        int acreageArgb,
        int windageArgb,
        int waterageArgb,
        int tankageArgb,
        int factorageArgb,
        int customageArgb,
        int cooperageArgb,
        int gaugageArgb,
        int lockerageArgb,
        int scowageArgb,
        int bargeageArgb,
        int boatageArgb,
        int craftageArgb,
        int shippageArgb,
        int weirageArgb,
        int millageArgb,
        int sluiceageArgb,
        int portageArgb,
        int wagonageArgb,
        int voyageArgb,
        int tollageArgb,
        int carriageArgb
) {
    /// Validates the surfaces.
    public ThemeSurfaces {
        Objects.requireNonNull(name, "name");
    }

    /// Returns the standard extra surfaces.
    ///
    /// @return the surfaces
    public static ThemeSurfaces standard() {
        return new ThemeSurfaces(
                "standard",
                0xFFF1F8E9,
                0xFFE8F5E9,
                0xFFB9F6CA,
                0xFF69F0AE,
                0xFF00E676,
                0xFF00C853,
                0xFF76FF03,
                0xFF64DD17,
                0xFFAEEA00,
                0xFFC6FF00,
                0xFFD4E157,
                0xFFDCE775,
                0xFFB2FF59,
                0xFFCCFF90,
                0xFFE6EE9C,
                0xFFF0F4C3,
                0xFFF9FBE7,
                0xFFB2DFDB,
                0xFFFFC107,
                0xFFFFD740,
                0xFFFFFDE7,
                0xFFFFF9C4,
                0xFFFFE0B2,
                0xFFE1F5FE,
                0xFF90CAF9,
                0xFF64B5F6,
                0xFF42A5F5,
                0xFF2196F3,
                0xFF1E88E5,
                0xFF1976D2,
                0xFF82B1FF,
                0xFF448AFF,
                0xFF2979FF,
                0xFF2962FF,
                0xFF304FFE,
                0xFF536DFE,
                0xFF7C4DFF,
                0xFF651FFF,
                0xFF6200EA,
                0xFFAA00FF,
                0xFFD500F9,
                0xFFE040FB,
                0xFFEA80FC,
                0xFFBA68C8,
                0xFFE1BEE7,
                0xFFF48FB1,
                0xFF9C27B0,
                0xFFEC407A,
                0xFFFF8A80,
                0xFFFF5252,
                0xFFFF1744,
                0xFFD50000,
                0xFFC51162,
                0xFFF50057,
                0xFFFF80AB,
                0xFFFF4081,
                0xFFFFCDD2,
                0xFFEF9A9A,
                0xFFE57373,
                0xFFEF5350,
                0xFFD32F2F,
                0xFFFF5722,
                0xFFF4511E,
                0xFFFFCCBC,
                0xFFFF6D00,
                0xFFFF9100,
                0xFFFFAB00,
                0xFFFFC400,
                0xFFFFD180,
                0xFFFF6E40,
                0xFFFF3D00,
                0xFFDD2C00,
                0xFFBF360C,
                0xFF3E2723,
                0xFF5D4037,
                0xFF6D4C41,
                0xFF795548,
                0xFF8D6E63,
                0xFFA1887F,
                0xFFBCAAA4,
                0xFFEFEBE9,
                0xFF263238,
                0xFF455A64,
                0xFF607D8B,
                0xFF78909C,
                0xFFCFD8DC,
                0xFFECEFF1,
                0xFF01579B,
                0xFF0277BD,
                0xFF0288D1,
                0xFF039BE5,
                0xFF03A9F4,
                0xFF29B6F6,
                0xFF4FC3F7,
                0xFF81D4FA,
                0xFFB3E5FC,
                0xFF80DEEA,
                0xFF4DD0E1,
                0xFF26C6DA,
                0xFF00BCD4,
                0xFF00ACC1,
                0xFF0097A7,
                0xFF00838F,
                0xFF006064,
                0xFF004D40,
                0xFF00695C,
                0xFF00796B,
                0xFF00897B,
                0xFF1B5E20,
                0xFF2E7D32,
                0xFF388E3C,
                0xFF43A047,
                0xFF4CAF50,
                0xFF66BB6A,
                0xFF81C784,
                0xFFA5D6A7,
                0xFFC8E6C9,
                0xFF33691E,
                0xFF558B2F,
                0xFF7CB342,
                0xFF9CCC65,
                0xFFAED581,
                0xFFDCEDC8,
                0xFF8BC34A,
                0xFF689F38,
                0xFF827717,
                0xFFC5CAE9,
                0xFF9FA8DA,
                0xFF7986CB,
                0xFF5C6BC0,
                0xFF3F51B5,
                0xFF3949AB,
                0xFF303F9F,
                0xFFEDE7F6,
                0xFF673AB7,
                0xFF512DA8,
                0xFFB388FF,
                0xFFD81B60,
                0xFF8C9EFF,
                0xFF3D5AFE,
                0xFFFCE4EC,
                0xFFD84315,
                0xFFFFEBEE,
                0xFFF44336,
                0xFFB0BEC5,
                0xFF0091EA,
                0xFF00B0FF,
                0xFF18FFFF,
                0xFF40C4FF,
                0xFF80D8FF,
                0xFFB2FEFA,
                0xFFA7FFEB,
                0xFFFFFF8D,
                0xFFFBE9E7,
                0xFFE0F2F1,
                0xFFFFEE58,
                0xFFFFEB3B,
                0xFF009688,
                0xFFA5F3FC,
                0xFF67E8F9,
                0xFF22D3EE,
                0xFF06B6D4,
                0xFF0891B2,
                0xFF0E7490,
                0xFF155E75,
                0xFF164E63,
                0xFF99F6E4,
                0xFF5EEAD4,
                0xFF2DD4BF,
                0xFF14B8A6,
                0xFF0D9488,
                0xFF0F766E,
                0xFF115E59,
                0xFF134E4A,
                0xFFBBF7D0,
                0xFF86EFAC,
                0xFF4ADE80,
                0xFF22C55E,
                0xFF16A34A,
                0xFF15803D,
                0xFF166534,
                0xFF14532D,
                0xFFD9F99D,
                0xFFBEF264,
                0xFFA3E635,
                0xFF84CC16,
                0xFF65A30D,
                0xFF4D7C0F,
                0xFF3F6212,
                0xFF365314,
                0xFF1A2E05,
                0xFFECFCCB,
                0xFFFFFBEB,
                0xFFFEF3C7,
                0xFFFDE68A,
                0xFFFCD34D,
                0xFFFBBF24,
                0xFFF59E0B,
                0xFFD97706,
                0xFFB45309,
                0xFF92400E,
                0xFF78350F,
                0xFF451A03,
                0xFFFFF7ED,
                0xFFFED7AA,
                0xFFFDBA74,
                0xFFFB923C,
                0xFFF97316,
                0xFFEA580C,
                0xFFC2410C,
                0xFF9A3412,
                0xFF7C2D12,
                0xFF431407,
                0xFFFFF1F2,
                0xFFFFE4E6,
                0xFFFECDD3,
                0xFFFDA4AF,
                0xFFFB7185,
                0xFFF43F5E,
                0xFFE11D48,
                0xFFBE123C,
                0xFF9F1239,
                0xFF881337,
                0xFF4C0519,
                0xFFFAE8FF,
                0xFFF5D0FE,
                0xFFF0ABFC,
                0xFFE879F9,
                0xFFD946EF,
                0xFFC026D3,
                0xFFA21CAF,
                0xFF86198F,
                0xFF701A75,
                0xFF4A044E,
                0xFFFDF4FF,
                0xFFF3E8FF,
                0xFFE9D5FF,
                0xFFDDD6FE,
                0xFFC4B5FD,
                0xFFA78BFA,
                0xFF8B5CF6,
                0xFF7C3AED,
                0xFF6D28D9,
                0xFF5B21B6,
                0xFF4C1D95,
                0xFF2E1065,
                0xFF1E1B4B,
                0xFF312E81,
                0xFF3730A3,
                0xFF4338CA,
                0xFF4F46E5,
                0xFF6366F1,
                0xFF818CF8
        );
    }

    /// Returns the high-contrast extra surfaces.
    ///
    /// @return the surfaces
    public static ThemeSurfaces highContrastTheme() {
        return new ThemeSurfaces(
                "high-contrast",
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
                0xFF00FFFF,
                0xFF808080,
                0xFF00FF00
        );
    }

    /// Encodes these surfaces as a first-stable theme resource payload.
    ///
    /// @return UTF-8 fields separated by `|`
    public byte[] encode() {
        return (name
                + '|'
                + Integer.toUnsignedString(groveArgb, 16)
                + '|'
                + Integer.toUnsignedString(gladeArgb, 16)
                + '|'
                + Integer.toUnsignedString(thicketArgb, 16)
                + '|'
                + Integer.toUnsignedString(brakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(covertArgb, 16)
                + '|'
                + Integer.toUnsignedString(rideArgb, 16)
                + '|'
                + Integer.toUnsignedString(wealdArgb, 16)
                + '|'
                + Integer.toUnsignedString(chaseArgb, 16)
                + '|'
                + Integer.toUnsignedString(parkArgb, 16)
                + '|'
                + Integer.toUnsignedString(coppiceArgb, 16)
                + '|'
                + Integer.toUnsignedString(frithArgb, 16)
                + '|'
                + Integer.toUnsignedString(woodArgb, 16)
                + '|'
                + Integer.toUnsignedString(hangerArgb, 16)
                + '|'
                + Integer.toUnsignedString(boskArgb, 16)
                + '|'
                + Integer.toUnsignedString(standArgb, 16)
                + '|'
                + Integer.toUnsignedString(toftArgb, 16)
                + '|'
                + Integer.toUnsignedString(croftArgb, 16)
                + '|'
                + Integer.toUnsignedString(closeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leyArgb, 16)
                + '|'
                + Integer.toUnsignedString(meadArgb, 16)
                + '|'
                + Integer.toUnsignedString(pastureArgb, 16)
                + '|'
                + Integer.toUnsignedString(leaArgb, 16)
                + '|'
                + Integer.toUnsignedString(swardArgb, 16)
                + '|'
                + Integer.toUnsignedString(downlandArgb, 16)
                + '|'
                + Integer.toUnsignedString(fieldArgb, 16)
                + '|'
                + Integer.toUnsignedString(paddockArgb, 16)
                + '|'
                + Integer.toUnsignedString(meadowArgb, 16)
                + '|'
                + Integer.toUnsignedString(greenArgb, 16)
                + '|'
                + Integer.toUnsignedString(commonArgb, 16)
                + '|'
                + Integer.toUnsignedString(acreArgb, 16)
                + '|'
                + Integer.toUnsignedString(garthArgb, 16)
                + '|'
                + Integer.toUnsignedString(yardArgb, 16)
                + '|'
                + Integer.toUnsignedString(plotArgb, 16)
                + '|'
                + Integer.toUnsignedString(allotmentArgb, 16)
                + '|'
                + Integer.toUnsignedString(gardenArgb, 16)
                + '|'
                + Integer.toUnsignedString(orchardArgb, 16)
                + '|'
                + Integer.toUnsignedString(vineyardArgb, 16)
                + '|'
                + Integer.toUnsignedString(warrenArgb, 16)
                + '|'
                + Integer.toUnsignedString(hopfieldArgb, 16)
                + '|'
                + Integer.toUnsignedString(vineryArgb, 16)
                + '|'
                + Integer.toUnsignedString(trellisArgb, 16)
                + '|'
                + Integer.toUnsignedString(arbourArgb, 16)
                + '|'
                + Integer.toUnsignedString(pergolaArgb, 16)
                + '|'
                + Integer.toUnsignedString(espalierArgb, 16)
                + '|'
                + Integer.toUnsignedString(palisadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hedgerowArgb, 16)
                + '|'
                + Integer.toUnsignedString(bowerArgb, 16)
                + '|'
                + Integer.toUnsignedString(alleyArgb, 16)
                + '|'
                + Integer.toUnsignedString(pleachArgb, 16)
                + '|'
                + Integer.toUnsignedString(lychgateArgb, 16)
                + '|'
                + Integer.toUnsignedString(wicketArgb, 16)
                + '|'
                + Integer.toUnsignedString(stileArgb, 16)
                + '|'
                + Integer.toUnsignedString(posternArgb, 16)
                + '|'
                + Integer.toUnsignedString(turnstileArgb, 16)
                + '|'
                + Integer.toUnsignedString(hatchArgb, 16)
                + '|'
                + Integer.toUnsignedString(lodgeArgb, 16)
                + '|'
                + Integer.toUnsignedString(porchArgb, 16)
                + '|'
                + Integer.toUnsignedString(byreArgb, 16)
                + '|'
                + Integer.toUnsignedString(shipponArgb, 16)
                + '|'
                + Integer.toUnsignedString(linhayArgb, 16)
                + '|'
                + Integer.toUnsignedString(bartonArgb, 16)
                + '|'
                + Integer.toUnsignedString(milkingArgb, 16)
                + '|'
                + Integer.toUnsignedString(shippenArgb, 16)
                + '|'
                + Integer.toUnsignedString(dairyArgb, 16)
                + '|'
                + Integer.toUnsignedString(parlourArgb, 16)
                + '|'
                + Integer.toUnsignedString(stallArgb, 16)
                + '|'
                + Integer.toUnsignedString(foldArgb, 16)
                + '|'
                + Integer.toUnsignedString(styArgb, 16)
                + '|'
                + Integer.toUnsignedString(coopArgb, 16)
                + '|'
                + Integer.toUnsignedString(loftArgb, 16)
                + '|'
                + Integer.toUnsignedString(granaryArgb, 16)
                + '|'
                + Integer.toUnsignedString(cribArgb, 16)
                + '|'
                + Integer.toUnsignedString(siloArgb, 16)
                + '|'
                + Integer.toUnsignedString(barnArgb, 16)
                + '|'
                + Integer.toUnsignedString(hovelArgb, 16)
                + '|'
                + Integer.toUnsignedString(mistalArgb, 16)
                + '|'
                + Integer.toUnsignedString(rickArgb, 16)
                + '|'
                + Integer.toUnsignedString(poundArgb, 16)
                + '|'
                + Integer.toUnsignedString(lairageArgb, 16)
                + '|'
                + Integer.toUnsignedString(shedArgb, 16)
                + '|'
                + Integer.toUnsignedString(henhouseArgb, 16)
                + '|'
                + Integer.toUnsignedString(piggeryArgb, 16)
                + '|'
                + Integer.toUnsignedString(middenArgb, 16)
                + '|'
                + Integer.toUnsignedString(dutchArgb, 16)
                + '|'
                + Integer.toUnsignedString(bothyArgb, 16)
                + '|'
                + Integer.toUnsignedString(shielingArgb, 16)
                + '|'
                + Integer.toUnsignedString(cotArgb, 16)
                + '|'
                + Integer.toUnsignedString(kirkArgb, 16)
                + '|'
                + Integer.toUnsignedString(manseArgb, 16)
                + '|'
                + Integer.toUnsignedString(clachanArgb, 16)
                + '|'
                + Integer.toUnsignedString(bothanArgb, 16)
                + '|'
                + Integer.toUnsignedString(townshipArgb, 16)
                + '|'
                + Integer.toUnsignedString(fermtounArgb, 16)
                + '|'
                + Integer.toUnsignedString(hamletArgb, 16)
                + '|'
                + Integer.toUnsignedString(thorpArgb, 16)
                + '|'
                + Integer.toUnsignedString(burghArgb, 16)
                + '|'
                + Integer.toUnsignedString(steadingArgb, 16)
                + '|'
                + Integer.toUnsignedString(grangeArgb, 16)
                + '|'
                + Integer.toUnsignedString(milntonArgb, 16)
                + '|'
                + Integer.toUnsignedString(fermholdArgb, 16)
                + '|'
                + Integer.toUnsignedString(inbyeArgb, 16)
                + '|'
                + Integer.toUnsignedString(outbyeArgb, 16)
                + '|'
                + Integer.toUnsignedString(outgangArgb, 16)
                + '|'
                + Integer.toUnsignedString(loaningArgb, 16)
                + '|'
                + Integer.toUnsignedString(riggArgb, 16)
                + '|'
                + Integer.toUnsignedString(loanendArgb, 16)
                + '|'
                + Integer.toUnsignedString(baulkArgb, 16)
                + '|'
                + Integer.toUnsignedString(loanheadArgb, 16)
                + '|'
                + Integer.toUnsignedString(headrigArgb, 16)
                + '|'
                + Integer.toUnsignedString(rigsideArgb, 16)
                + '|'
                + Integer.toUnsignedString(loanfootArgb, 16)
                + '|'
                + Integer.toUnsignedString(headlandArgb, 16)
                + '|'
                + Integer.toUnsignedString(rigendArgb, 16)
                + '|'
                + Integer.toUnsignedString(loanmidArgb, 16)
                + '|'
                + Integer.toUnsignedString(furrowArgb, 16)
                + '|'
                + Integer.toUnsignedString(selionArgb, 16)
                + '|'
                + Integer.toUnsignedString(furlongArgb, 16)
                + '|'
                + Integer.toUnsignedString(hideArgb, 16)
                + '|'
                + Integer.toUnsignedString(virgateArgb, 16)
                + '|'
                + Integer.toUnsignedString(carucateArgb, 16)
                + '|'
                + Integer.toUnsignedString(bovateArgb, 16)
                + '|'
                + Integer.toUnsignedString(oxgangArgb, 16)
                + '|'
                + Integer.toUnsignedString(nookArgb, 16)
                + '|'
                + Integer.toUnsignedString(goadArgb, 16)
                + '|'
                + Integer.toUnsignedString(roodArgb, 16)
                + '|'
                + Integer.toUnsignedString(perchArgb, 16)
                + '|'
                + Integer.toUnsignedString(ferlingArgb, 16)
                + '|'
                + Integer.toUnsignedString(oxlandArgb, 16)
                + '|'
                + Integer.toUnsignedString(plowgateArgb, 16)
                + '|'
                + Integer.toUnsignedString(morgenArgb, 16)
                + '|'
                + Integer.toUnsignedString(jugerumArgb, 16)
                + '|'
                + Integer.toUnsignedString(arpentArgb, 16)
                + '|'
                + Integer.toUnsignedString(sulungArgb, 16)
                + '|'
                + Integer.toUnsignedString(hidageArgb, 16)
                + '|'
                + Integer.toUnsignedString(geldArgb, 16)
                + '|'
                + Integer.toUnsignedString(carucageArgb, 16)
                + '|'
                + Integer.toUnsignedString(jochArgb, 16)
                + '|'
                + Integer.toUnsignedString(iugumArgb, 16)
                + '|'
                + Integer.toUnsignedString(tenementArgb, 16)
                + '|'
                + Integer.toUnsignedString(goreArgb, 16)
                + '|'
                + Integer.toUnsignedString(buttArgb, 16)
                + '|'
                + Integer.toUnsignedString(stripArgb, 16)
                + '|'
                + Integer.toUnsignedString(poleArgb, 16)
                + '|'
                + Integer.toUnsignedString(chainArgb, 16)
                + '|'
                + Integer.toUnsignedString(messuageArgb, 16)
                + '|'
                + Integer.toUnsignedString(demesneArgb, 16)
                + '|'
                + Integer.toUnsignedString(glebeArgb, 16)
                + '|'
                + Integer.toUnsignedString(tithingArgb, 16)
                + '|'
                + Integer.toUnsignedString(wapentakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(hundredArgb, 16)
                + '|'
                + Integer.toUnsignedString(burgageArgb, 16)
                + '|'
                + Integer.toUnsignedString(assartArgb, 16)
                + '|'
                + Integer.toUnsignedString(intakeArgb, 16)
                + '|'
                + Integer.toUnsignedString(intackArgb, 16)
                + '|'
                + Integer.toUnsignedString(journalArgb, 16)
                + '|'
                + Integer.toUnsignedString(purlieuArgb, 16)
                + '|'
                + Integer.toUnsignedString(wasteArgb, 16)
                + '|'
                + Integer.toUnsignedString(severalArgb, 16)
                + '|'
                + Integer.toUnsignedString(lammasArgb, 16)
                + '|'
                + Integer.toUnsignedString(escheatArgb, 16)
                + '|'
                + Integer.toUnsignedString(assartageArgb, 16)
                + '|'
                + Integer.toUnsignedString(shackArgb, 16)
                + '|'
                + Integer.toUnsignedString(socageArgb, 16)
                + '|'
                + Integer.toUnsignedString(bordarArgb, 16)
                + '|'
                + Integer.toUnsignedString(villeinArgb, 16)
                + '|'
                + Integer.toUnsignedString(boonworkArgb, 16)
                + '|'
                + Integer.toUnsignedString(cotsetleArgb, 16)
                + '|'
                + Integer.toUnsignedString(frankalmoinArgb, 16)
                + '|'
                + Integer.toUnsignedString(merchetArgb, 16)
                + '|'
                + Integer.toUnsignedString(heriotArgb, 16)
                + '|'
                + Integer.toUnsignedString(tallageArgb, 16)
                + '|'
                + Integer.toUnsignedString(scutageArgb, 16)
                + '|'
                + Integer.toUnsignedString(gavelArgb, 16)
                + '|'
                + Integer.toUnsignedString(chevageArgb, 16)
                + '|'
                + Integer.toUnsignedString(aidArgb, 16)
                + '|'
                + Integer.toUnsignedString(pannageArgb, 16)
                + '|'
                + Integer.toUnsignedString(murageArgb, 16)
                + '|'
                + Integer.toUnsignedString(pontageArgb, 16)
                + '|'
                + Integer.toUnsignedString(pavageArgb, 16)
                + '|'
                + Integer.toUnsignedString(lastageArgb, 16)
                + '|'
                + Integer.toUnsignedString(pickageArgb, 16)
                + '|'
                + Integer.toUnsignedString(tronageArgb, 16)
                + '|'
                + Integer.toUnsignedString(wharfageArgb, 16)
                + '|'
                + Integer.toUnsignedString(amercementArgb, 16)
                + '|'
                + Integer.toUnsignedString(cranageArgb, 16)
                + '|'
                + Integer.toUnsignedString(keelageArgb, 16)
                + '|'
                + Integer.toUnsignedString(anchorageArgb, 16)
                + '|'
                + Integer.toUnsignedString(groundageArgb, 16)
                + '|'
                + Integer.toUnsignedString(scavageArgb, 16)
                + '|'
                + Integer.toUnsignedString(primageArgb, 16)
                + '|'
                + Integer.toUnsignedString(stowageArgb, 16)
                + '|'
                + Integer.toUnsignedString(lighterageArgb, 16)
                + '|'
                + Integer.toUnsignedString(demurrageArgb, 16)
                + '|'
                + Integer.toUnsignedString(salvageArgb, 16)
                + '|'
                + Integer.toUnsignedString(moorageArgb, 16)
                + '|'
                + Integer.toUnsignedString(quayageArgb, 16)
                + '|'
                + Integer.toUnsignedString(lockageArgb, 16)
                + '|'
                + Integer.toUnsignedString(pierageArgb, 16)
                + '|'
                + Integer.toUnsignedString(dockageArgb, 16)
                + '|'
                + Integer.toUnsignedString(cellarageArgb, 16)
                + '|'
                + Integer.toUnsignedString(gallonageArgb, 16)
                + '|'
                + Integer.toUnsignedString(tunnageArgb, 16)
                + '|'
                + Integer.toUnsignedString(seigniorageArgb, 16)
                + '|'
                + Integer.toUnsignedString(poundageArgb, 16)
                + '|'
                + Integer.toUnsignedString(brokerageArgb, 16)
                + '|'
                + Integer.toUnsignedString(pilotageArgb, 16)
                + '|'
                + Integer.toUnsignedString(havageArgb, 16)
                + '|'
                + Integer.toUnsignedString(beamageArgb, 16)
                + '|'
                + Integer.toUnsignedString(sternageArgb, 16)
                + '|'
                + Integer.toUnsignedString(ferriageArgb, 16)
                + '|'
                + Integer.toUnsignedString(meterageArgb, 16)
                + '|'
                + Integer.toUnsignedString(ullageArgb, 16)
                + '|'
                + Integer.toUnsignedString(breakageArgb, 16)
                + '|'
                + Integer.toUnsignedString(cartageArgb, 16)
                + '|'
                + Integer.toUnsignedString(haulageArgb, 16)
                + '|'
                + Integer.toUnsignedString(freightageArgb, 16)
                + '|'
                + Integer.toUnsignedString(drayageArgb, 16)
                + '|'
                + Integer.toUnsignedString(porterageArgb, 16)
                + '|'
                + Integer.toUnsignedString(leakageArgb, 16)
                + '|'
                + Integer.toUnsignedString(packageArgb, 16)
                + '|'
                + Integer.toUnsignedString(storageArgb, 16)
                + '|'
                + Integer.toUnsignedString(averageArgb, 16)
                + '|'
                + Integer.toUnsignedString(wreckageArgb, 16)
                + '|'
                + Integer.toUnsignedString(wastageArgb, 16)
                + '|'
                + Integer.toUnsignedString(spoilageArgb, 16)
                + '|'
                + Integer.toUnsignedString(cordageArgb, 16)
                + '|'
                + Integer.toUnsignedString(mileageArgb, 16)
                + '|'
                + Integer.toUnsignedString(postageArgb, 16)
                + '|'
                + Integer.toUnsignedString(tonnageArgb, 16)
                + '|'
                + Integer.toUnsignedString(coinageArgb, 16)
                + '|'
                + Integer.toUnsignedString(yardageArgb, 16)
                + '|'
                + Integer.toUnsignedString(acreageArgb, 16)
                + '|'
                + Integer.toUnsignedString(windageArgb, 16)
                + '|'
                + Integer.toUnsignedString(waterageArgb, 16)
                + '|'
                + Integer.toUnsignedString(tankageArgb, 16)
                + '|'
                + Integer.toUnsignedString(factorageArgb, 16)
                + '|'
                + Integer.toUnsignedString(customageArgb, 16)
                + '|'
                + Integer.toUnsignedString(cooperageArgb, 16)
                + '|'
                + Integer.toUnsignedString(gaugageArgb, 16)
                + '|'
                + Integer.toUnsignedString(lockerageArgb, 16)
                + '|'
                + Integer.toUnsignedString(scowageArgb, 16)
                + '|'
                + Integer.toUnsignedString(bargeageArgb, 16)
                + '|'
                + Integer.toUnsignedString(boatageArgb, 16)
                + '|'
                + Integer.toUnsignedString(craftageArgb, 16)
                + '|'
                + Integer.toUnsignedString(shippageArgb, 16)
                + '|'
                + Integer.toUnsignedString(weirageArgb, 16)
                + '|'
                + Integer.toUnsignedString(millageArgb, 16)
                + '|'
                + Integer.toUnsignedString(sluiceageArgb, 16)
                + '|'
                + Integer.toUnsignedString(portageArgb, 16)
                + '|'
                + Integer.toUnsignedString(wagonageArgb, 16)
                + '|'
                + Integer.toUnsignedString(voyageArgb, 16)
                + '|'
                + Integer.toUnsignedString(tollageArgb, 16)
                + '|'
                + Integer.toUnsignedString(carriageArgb, 16)).getBytes(StandardCharsets.UTF_8);
    }

    /// Decodes a payload produced by [`#encode()`].
    ///
    /// @param bytes the resource bytes
    /// @return the surfaces
    public static ThemeSurfaces decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        String[] fields = new String(bytes, StandardCharsets.UTF_8).split("\\|", -1);
        if (fields.length != 254) {
            throw new IllegalArgumentException("Theme surfaces must have two-hundred-fifty-four fields");
        }
        return new ThemeSurfaces(
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
