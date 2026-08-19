package org.glavo.himari.controls;

import org.glavo.himari.layout.semantics.TextDirection;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Stores the first-stable unstyled theme tokens.
///
/// @param name the theme name
/// @param backgroundArgb the background color
/// @param textArgb the text color
/// @param accentArgb the accent color
/// @param disabledArgb the disabled-content color
/// @param focusArgb the focus-ring color
/// @param selectionArgb the selected-content color
/// @param errorArgb the error-content color
/// @param hoverArgb the hover-content color
/// @param borderArgb the border color
/// @param fontScale the relative type size; `1` is the unscaled default
/// @param density the relative spacing scale; `1` is the unscaled default
/// @param letterSpacing extra tracking in ems; `0` is the unscaled default
/// @param cornerRadius corner radius in logical pixels; `0` is square
/// @param lineHeight relative line height; `1` is the unscaled default
/// @param paragraphSpacing extra space after a paragraph in ems; `0` is the unscaled default
/// @param wordSpacing extra space between words in ems; `0` is the unscaled default
/// @param tabSize tab stop width in spaces; `4` is the unscaled default
/// @param minContrast minimum text-to-background contrast ratio; `4.5` is WCAG AA
/// @param focusRingWidth focus-ring width in logical pixels; `2` is the unscaled default
/// @param iconSize icon edge length in logical pixels; `16` is the unscaled default
/// @param elevation surface elevation in logical pixels; `0` is flat
/// @param focusRingOffset gap between the control and the focus ring in logical pixels; `2` is the unscaled default
/// @param animationDuration default motion duration in milliseconds; `200` is the unscaled default
/// @param shadowBlur shadow blur radius in logical pixels; `8` is the unscaled default
/// @param shadowSpread shadow spread in logical pixels; `2` is the unscaled default
/// @param shadowOffsetX horizontal shadow offset in logical pixels; `0` is the unscaled default
/// @param shadowOffsetY vertical shadow offset in logical pixels; `2` is the unscaled default
/// @param shadowArgb the shadow color
/// @param easing the motion easing name; `standard` is the unscaled default
/// @param easingDurationScale multiplier applied to motion durations; `1` is the unscaled default
/// @param disabledOpacity disabled-content opacity in `[0, 1]`; `0.38` is the unscaled default
/// @param strokeWidth hairline stroke width in logical pixels; `1` is the unscaled default
/// @param pressScale pressed-control scale; `0.98` is the unscaled default
/// @param overlayArgb the modal-overlay color
/// @param scrimArgb the scrim color
/// @param dividerArgb the divider color
/// @param outlineArgb the outline color
/// @param caretArgb the caret color
/// @param linkArgb the hyperlink color
/// @param warningArgb the warning-content color
/// @param successArgb the success-content color
/// @param selectionTextArgb the selected-text color
/// @param canvasArgb the canvas background color
/// @param placeholderArgb the placeholder-text color
/// @param rippleArgb the ripple overlay color
/// @param tooltipArgb the tooltip background color
/// @param surfaceArgb the elevated-surface color
/// @param badgeArgb the badge color
/// @param snackbarArgb the snackbar background color
/// @param chipArgb the chip background color
/// @param toastArgb the toast background color
/// @param sheetArgb the sheet background color
/// @param menuArgb the menu background color
/// @param drawerArgb the drawer background color
/// @param appBarArgb the app-bar background color
/// @param cardArgb the card background color
/// @param fabArgb the floating-action-button color
/// @param navRailArgb the navigation-rail background color
/// @param bottomBarArgb the bottom-bar background color
/// @param tabArgb the tab background color
/// @param dialogArgb the dialog background color
/// @param sidebarArgb the sidebar background color
/// @param bannerArgb the banner background color
/// @param spotlightArgb the spotlight overlay color
/// @param selectionHandleArgb the selection-handle color
/// @param switchArgb the switch-track color
/// @param checkboxArgb the checkbox color
/// @param radioArgb the radio-button color
/// @param sliderArgb the slider-track color
/// @param progressArgb the progress-bar color
/// @param listArgb the list background color
/// @param treeArgb the tree background color
/// @param tableArgb the table background color
/// @param textFieldArgb the text-field background color
/// @param toggleArgb the toggle-track color
/// @param scrollbarArgb the scrollbar color
/// @param splitArgb the split-pane divider color
/// @param searchArgb the search-field background color
/// @param statusArgb the status-bar background color
/// @param accordionArgb the accordion background color
/// @param stepperArgb the stepper color
/// @param paginationArgb the pagination color
/// @param avatarArgb the avatar background color
/// @param breadcrumbArgb the breadcrumb background color
/// @param calendarArgb the calendar background color
/// @param ratingArgb the rating-star color
/// @param timelineArgb the timeline background color
/// @param carouselArgb the carousel background color
/// @param dockArgb the dock background color
/// @param notificationArgb the notification background color
/// @param codeArgb the code-block background color
/// @param blockquoteArgb the blockquote background color
/// @param mentionArgb the mention highlight color
/// @param highlightArgb the text-highlight color
/// @param watermarkArgb the watermark overlay color
/// @param kbdArgb the keyboard-key background color
/// @param markArgb the mark/highlight-chip color
/// @param insetArgb the inset/well background color
/// @param captionArgb the window-caption background color
/// @param overlineArgb the overline decoration color
/// @param strikeArgb the strikethrough decoration color
/// @param outsetArgb the outset/raised-well color
/// @param hairlineArgb the hairline separator color
/// @param underlineArgb the underline decoration color
/// @param footnoteArgb the footnote text color
/// @param captionTextArgb the caption-bar text color
/// @param shadeArgb the shade overlay color
/// @param glowArgb the glow/emphasis overlay color
/// @param frostArgb the frosted-glass overlay color
/// @param veilArgb the veil overlay color
/// @param mistArgb the mist overlay color
/// @param hazeArgb the haze overlay color
/// @param sheenArgb the sheen highlight color
/// @param bloomArgb the bloom overlay color
/// @param flareArgb the flare overlay color
/// @param filmArgb the film-grain overlay color
/// @param duskArgb the dusk surface color
/// @param emberArgb the ember accent color
/// @param sparkArgb the spark highlight color
/// @param grainArgb the grain overlay color
/// @param mossArgb the moss accent color
/// @param clayArgb the clay surface color
/// @param sandArgb the sand surface color
/// @param rustArgb the rust accent color
/// @param sageArgb the sage accent color
/// @param peatArgb the peat surface color
/// @param ochreArgb the ochre accent color
/// @param slateArgb the slate surface color
/// @param inkArgb the ink accent color
/// @param foamArgb the foam overlay color
/// @param brineArgb the brine accent color
/// @param tideArgb the tide accent color
/// @param kelpArgb the kelp accent color
/// @param reefArgb the reef accent color
/// @param duneArgb the dune surface color
/// @param coveArgb the cove accent color
/// @param lagoonArgb the lagoon accent color
/// @param atollArgb the atoll accent color
/// @param shoalArgb the shoal accent color
/// @param spitArgb the spit surface color
/// @param marshArgb the marsh accent color
/// @param fenArgb the fen accent color
/// @param bogArgb the bog surface color
/// @param cayArgb the cay accent color
/// @param inletArgb the inlet accent color
/// @param soundArgb the sound accent color
/// @param bayArgb the bay accent color
/// @param gulfArgb the gulf accent color
/// @param fjordArgb the fjord surface color
/// @param lochArgb the loch accent color
/// @param tarnArgb the tarn accent color
/// @param mereArgb the mere surface color
/// @param firthArgb the firth accent color
/// @param kyleArgb the kyle accent color
/// @param nessArgb the ness surface color
/// @param harborArgb the harbor accent color
/// @param quayArgb the quay surface color
/// @param pierArgb the pier surface color
/// @param bightArgb the bight accent color
/// @param reachArgb the reach accent color
/// @param poolArgb the pool surface color
/// @param channelArgb the channel accent color
/// @param straitArgb the strait accent color
/// @param havenArgb the haven surface color
/// @param riaArgb the ria accent color
/// @param loughArgb the lough accent color
/// @param voeArgb the voe surface color
/// @param wickArgb the wick accent color
/// @param holmArgb the holm surface color
/// @param geoArgb the geo surface color
/// @param ayreArgb the ayre accent color
/// @param skerryArgb the skerry surface color
/// @param stackArgb the stack surface color
/// @param braeArgb the brae accent color
/// @param glenArgb the glen accent color
/// @param strathArgb the strath surface color
/// @param combeArgb the combe accent color
/// @param daleArgb the dale accent color
/// @param valeArgb the vale surface color
/// @param fellArgb the fell surface color
/// @param moorArgb the moor surface color
/// @param heathArgb the heath surface color
/// @param woldArgb the wold surface color
/// @param downArgb the down surface color
/// @param ridgeArgb the ridge accent color
/// @param cragArgb the crag surface color
/// @param scarpArgb the scarp surface color
/// @param knollArgb the knoll surface color
/// @param torArgb the tor surface color
/// @param benArgb the ben surface color
/// @param lawArgb the law surface color
/// @param cairnArgb the cairn surface color
/// @param howeArgb the howe surface color
/// @param knoweArgb the knowe surface color
/// @param kameArgb the kame surface color
/// @param drumlinArgb the drumlin surface color
/// @param eskerArgb the esker surface color
/// @param moraineArgb the moraine surface color
/// @param screeArgb the scree surface color
/// @param talusArgb the talus surface color
/// @param cirqueArgb the cirque surface color
/// @param areteArgb the arete surface color
/// @param colArgb the col surface color
/// @param saddleArgb the saddle surface color
/// @param couloirArgb the couloir surface color
/// @param nunatakArgb the nunatak surface color
/// @param seracArgb the serac surface color
/// @param firnArgb the firn surface color
/// @param crevasseArgb the crevasse surface color
/// @param icefallArgb the icefall surface color
/// @param neveArgb the neve surface color
/// @param sastrugiArgb the sastrugi surface color
/// @param corrieArgb the corrie surface color
/// @param gullyArgb the gully surface color
/// @param buttressArgb the buttress surface color
/// @param hornArgb the horn surface color
/// @param aiguilleArgb the aiguille surface color
/// @param pizArgb the piz surface color
/// @param cwmArgb the cwm surface color
/// @param gillArgb the gill surface color
/// @param cloughArgb the clough surface color
/// @param slackArgb the slack surface color
/// @param hassockArgb the hassock surface color
/// @param sikeArgb the sike surface color
/// @param beckArgb the beck surface color
/// @param burnArgb the burn surface color
/// @param forceArgb the force surface color
/// @param lynchetArgb the lynchet surface color
/// @param dingleArgb the dingle surface color
/// @param ghyllArgb the ghyll surface color
/// @param rillArgb the rill surface color
/// @param bourneArgb the bourne surface color
/// @param ladeArgb the lade surface color
/// @param leatArgb the leat surface color
/// @param stellArgb the stell surface color
/// @param lodeArgb the lode surface color
/// @param fossArgb the foss surface color
/// @param sladeArgb the slade surface color
/// @param dellArgb the dell surface color
/// @param deneArgb the dene surface color
/// @param nantArgb the nant surface color
/// @param linnArgb the linn surface color
/// @param copseArgb the copse surface color
/// @param spinneyArgb the spinney surface color
/// @param shawArgb the shaw surface color
/// @param carrArgb the carr surface color
/// @param holtArgb the holt surface color
/// @param hangarArgb the hangar surface color
/// @param highContrast whether this is the high-contrast theme
/// @param textDirection the reading direction for themed subtrees
/// @param reducedMotion whether nonessential motion must be suppressed
@NotNullByDefault
public record ThemeTokens(
        String name,
        int backgroundArgb,
        int textArgb,
        int accentArgb,
        int disabledArgb,
        int focusArgb,
        int selectionArgb,
        int errorArgb,
        int hoverArgb,
        int borderArgb,
        float fontScale,
        float density,
        float letterSpacing,
        float cornerRadius,
        float lineHeight,
        float paragraphSpacing,
        float wordSpacing,
        int tabSize,
        float minContrast,
        float focusRingWidth,
        float iconSize,
        float elevation,
        float focusRingOffset,
        int animationDuration,
        float shadowBlur,
        float shadowSpread,
        float shadowOffsetX,
        float shadowOffsetY,
        int shadowArgb,
        String easing,
        float easingDurationScale,
        float disabledOpacity,
        float strokeWidth,
        float pressScale,
        int overlayArgb,
        int scrimArgb,
        int dividerArgb,
        int outlineArgb,
        int caretArgb,
        int linkArgb,
        int warningArgb,
        int successArgb,
        int selectionTextArgb,
        int canvasArgb,
        int placeholderArgb,
        int rippleArgb,
        int tooltipArgb,
        int surfaceArgb,
        int badgeArgb,
        int snackbarArgb,
        int chipArgb,
        int toastArgb,
        int sheetArgb,
        int menuArgb,
        int drawerArgb,
        int appBarArgb,
        int cardArgb,
        int fabArgb,
        int navRailArgb,
        int bottomBarArgb,
        int tabArgb,
        int dialogArgb,
        int sidebarArgb,
        int bannerArgb,
        int spotlightArgb,
        int selectionHandleArgb,
        int switchArgb,
        int checkboxArgb,
        int radioArgb,
        int sliderArgb,
        int progressArgb,
        int listArgb,
        int treeArgb,
        int tableArgb,
        int textFieldArgb,
        int toggleArgb,
        int scrollbarArgb,
        int splitArgb,
        int searchArgb,
        int statusArgb,
        int accordionArgb,
        int stepperArgb,
        int paginationArgb,
        int avatarArgb,
        int breadcrumbArgb,
        int calendarArgb,
        int ratingArgb,
        int timelineArgb,
        int carouselArgb,
        int dockArgb,
        int notificationArgb,
        int codeArgb,
        int blockquoteArgb,
        int mentionArgb,
        int highlightArgb,
        int watermarkArgb,
        int kbdArgb,
        int markArgb,
        int insetArgb,
        int captionArgb,
        int overlineArgb,
        int strikeArgb,
        int outsetArgb,
        int hairlineArgb,
        int underlineArgb,
        int footnoteArgb,
        int captionTextArgb,
        int shadeArgb,
        int glowArgb,
        int frostArgb,
        int veilArgb,
        int mistArgb,
        int hazeArgb,
        int sheenArgb,
        int bloomArgb,
        int flareArgb,
        int filmArgb,
        int duskArgb,
        int emberArgb,
        int sparkArgb,
        int grainArgb,
        int mossArgb,
        int clayArgb,
        int sandArgb,
        int rustArgb,
        int sageArgb,
        int peatArgb,
        int ochreArgb,
        int slateArgb,
        int inkArgb,
        int foamArgb,
        int brineArgb,
        int tideArgb,
        int kelpArgb,
        int reefArgb,
        int duneArgb,
        int coveArgb,
        int lagoonArgb,
        int atollArgb,
        int shoalArgb,
        int spitArgb,
        int marshArgb,
        int fenArgb,
        int bogArgb,
        int cayArgb,
        int inletArgb,
        int soundArgb,
        int bayArgb,
        int gulfArgb,
        int fjordArgb,
        int lochArgb,
        int tarnArgb,
        int mereArgb,
        int firthArgb,
        int kyleArgb,
        int nessArgb,
        int harborArgb,
        int quayArgb,
        int pierArgb,
        int bightArgb,
        int reachArgb,
        int poolArgb,
        int channelArgb,
        int straitArgb,
        int havenArgb,
        int riaArgb,
        int loughArgb,
        int voeArgb,
        int wickArgb,
        int holmArgb,
        int geoArgb,
        int ayreArgb,
        int skerryArgb,
        int stackArgb,
        int braeArgb,
        int glenArgb,
        int strathArgb,
        int combeArgb,
        int daleArgb,
        int valeArgb,
        int fellArgb,
        int moorArgb,
        int heathArgb,
        int woldArgb,
        int downArgb,
        int ridgeArgb,
        int cragArgb,
        int scarpArgb,
        int knollArgb,
        int torArgb,
        int benArgb,
        int lawArgb,
        int cairnArgb,
        int howeArgb,
        int knoweArgb,
        int kameArgb,
        int drumlinArgb,
        int eskerArgb,
        int moraineArgb,
        int screeArgb,
        int talusArgb,
        int cirqueArgb,
        int areteArgb,
        int colArgb,
        int saddleArgb,
        int couloirArgb,
        int nunatakArgb,
        int seracArgb,
        int firnArgb,
        int crevasseArgb,
        int icefallArgb,
        int neveArgb,
        int sastrugiArgb,
        int corrieArgb,
        int gullyArgb,
        int buttressArgb,
        int hornArgb,
        int aiguilleArgb,
        int pizArgb,
        int cwmArgb,
        int gillArgb,
        int cloughArgb,
        int slackArgb,
        int hassockArgb,
        int sikeArgb,
        int beckArgb,
        int burnArgb,
        int forceArgb,
        int lynchetArgb,
        int dingleArgb,
        int ghyllArgb,
        int rillArgb,
        int bourneArgb,
        int ladeArgb,
        int leatArgb,
        int stellArgb,
        int lodeArgb,
        int fossArgb,
        int sladeArgb,
        int dellArgb,
        int deneArgb,
        int nantArgb,
        int linnArgb,
        int copseArgb,
        int spinneyArgb,
        int shawArgb,
        int carrArgb,
        int holtArgb,
        int hangarArgb,
        boolean highContrast,
        TextDirection textDirection,
        boolean reducedMotion
) {
    /// Validates the tokens.
    public ThemeTokens {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Theme name must be nonblank");
        }
        Objects.requireNonNull(easing, "easing");
        if (easing.isBlank()) {
            throw new IllegalArgumentException("easing must be nonblank");
        }
        Objects.requireNonNull(textDirection, "textDirection");
        if (!Float.isFinite(fontScale) || fontScale <= 0.0f) {
            throw new IllegalArgumentException("fontScale must be finite and positive");
        }
        if (!Float.isFinite(density) || density <= 0.0f) {
            throw new IllegalArgumentException("density must be finite and positive");
        }
        if (!Float.isFinite(letterSpacing)) {
            throw new IllegalArgumentException("letterSpacing must be finite");
        }
        if (!Float.isFinite(cornerRadius) || cornerRadius < 0.0f) {
            throw new IllegalArgumentException("cornerRadius must be finite and nonnegative");
        }
        if (!Float.isFinite(lineHeight) || lineHeight <= 0.0f) {
            throw new IllegalArgumentException("lineHeight must be finite and positive");
        }
        if (!Float.isFinite(paragraphSpacing) || paragraphSpacing < 0.0f) {
            throw new IllegalArgumentException("paragraphSpacing must be finite and nonnegative");
        }
        if (!Float.isFinite(wordSpacing)) {
            throw new IllegalArgumentException("wordSpacing must be finite");
        }
        if (tabSize < 1) {
            throw new IllegalArgumentException("tabSize must be at least 1");
        }
        if (!Float.isFinite(minContrast) || minContrast < 1.0f) {
            throw new IllegalArgumentException("minContrast must be finite and at least 1");
        }
        if (!Float.isFinite(focusRingWidth) || focusRingWidth < 0.0f) {
            throw new IllegalArgumentException("focusRingWidth must be finite and nonnegative");
        }
        if (!Float.isFinite(iconSize) || iconSize <= 0.0f) {
            throw new IllegalArgumentException("iconSize must be finite and positive");
        }
        if (!Float.isFinite(elevation) || elevation < 0.0f) {
            throw new IllegalArgumentException("elevation must be finite and nonnegative");
        }
        if (!Float.isFinite(focusRingOffset) || focusRingOffset < 0.0f) {
            throw new IllegalArgumentException("focusRingOffset must be finite and nonnegative");
        }
        if (animationDuration < 0) {
            throw new IllegalArgumentException("animationDuration must be nonnegative");
        }
        if (!Float.isFinite(shadowBlur) || shadowBlur < 0.0f) {
            throw new IllegalArgumentException("shadowBlur must be finite and nonnegative");
        }
        if (!Float.isFinite(shadowSpread)) {
            throw new IllegalArgumentException("shadowSpread must be finite");
        }
        if (!Float.isFinite(shadowOffsetX) || !Float.isFinite(shadowOffsetY)) {
            throw new IllegalArgumentException("shadow offsets must be finite");
        }
        if (!Float.isFinite(easingDurationScale) || easingDurationScale < 0.0f) {
            throw new IllegalArgumentException("easingDurationScale must be finite and nonnegative");
        }
        if (!Float.isFinite(disabledOpacity) || disabledOpacity < 0.0f || disabledOpacity > 1.0f) {
            throw new IllegalArgumentException("disabledOpacity must be finite and in [0, 1]");
        }
        if (!Float.isFinite(strokeWidth) || strokeWidth < 0.0f) {
            throw new IllegalArgumentException("strokeWidth must be finite and nonnegative");
        }
        if (!Float.isFinite(pressScale) || pressScale <= 0.0f) {
            throw new IllegalArgumentException("pressScale must be finite and positive");
        }
    }

    /// Returns the default light theme.
    ///
    /// @return the tokens
    public static ThemeTokens standard() {
        return new ThemeTokens(
                "standard",
                0xFFFFFFFF,
                0xFF1A1A1A,
                0xFF1565C0,
                0xFF9E9E9E,
                0xFF1565C0,
                0xFFBBDEFB,
                0xFFC62828,
                0xFFE3F2FD,
                0xFFE0E0E0,
                1.0f,
                1.0f,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                0.0f,
                4,
                4.5f,
                2.0f,
                16.0f,
                0.0f,
                2.0f,
                200,
                8.0f,
                2.0f,
                0.0f,
                2.0f,
                0x40000000,
                "standard",
                1.0f,
                0.38f,
                1.0f,
                0.98f,
                0x80000000,
                0x66000000,
                0x1F000000,
                0xFF1565C0,
                0xFF1A1A1A,
                0xFF1565C0,
                0xFFF9A825,
                0xFF2E7D32,
                0xFF1A1A1A,
                0xFFFFFFFF,
                0xFF9E9E9E,
                0x331565C0,
                0xFF424242,
                0xFFF5F5F5,
                0xFFC62828,
                0xFF323232,
                0xFFE0E0E0,
                0xFF323232,
                0xFFFFFFFF,
                0xFFFAFAFA,
                0xFFFAFAFA,
                0xFF1565C0,
                0xFFFFFFFF,
                0xFF1565C0,
                0xFFFAFAFA,
                0xFFFFFFFF,
                0xFFE3F2FD,
                0xFFFFFFFF,
                0xFFFAFAFA,
                0xFFBBDEFB,
                0x331565C0,
                0xFF1565C0,
                0xFF81C784,
                0xFF1565C0,
                0xFF5E35B1,
                0xFF1565C0,
                0xFF2E7D32,
                0xFFFFFFFF,
                0xFFF3E5F5,
                0xFFFFFFFF,
                0xFFF5F5F5,
                0xFF1565C0,
                0xFFE0E0E0,
                0xFFBDBDBD,
                0xFFEEEEEE,
                0xFFE3F2FD,
                0xFFFAFAFA,
                0xFF1565C0,
                0xFFBBDEFB,
                0xFF9E9E9E,
                0xFFFAFAFA,
                0xFFFFFFFF,
                0xFFF9A825,
                0xFFE8EAF6,
                0xFFFAFAFA,
                0xFF212121,
                0xFFFFF3E0,
                0xFF263238,
                0xFFF3E5F5,
                0xFFC8E6C9,
                0xFFFFF59D,
                0x339E9E9E,
                0xFFCFD8DC,
                0xFFFFE082,
                0x1F000000,
                0xFF37474F,
                0xFF90A4AE,
                0xFFE53935,
                0x14000000,
                0x33000000,
                0xFF1565C0,
                0xFF616161,
                0xFFFFFFFF,
                0x1A000000,
                0x40FFC107,
                0xCCECEFF1,
                0x4D000000,
                0x33B0BEC5,
                0x6690A4AE,
                0x26FFFFFF,
                0x66FF8A80,
                0x80FFECB3,
                0x1A263238,
                0xFF455A64,
                0xFFFF6F00,
                0xFFFFF176,
                0xFF8D6E63,
                0xFF558B2F,
                0xFFBF360C,
                0xFFD7CCC8,
                0xFFB71C1C,
                0xFF9CCC65,
                0xFF4E342E,
                0xFFC6A700,
                0xFF546E7A,
                0xFF1A237E,
                0xFFB2EBF2,
                0xFF006064,
                0xFF0277BD,
                0xFF1B5E20,
                0xFF00838F,
                0xFFD4A574,
                0xFF004D40,
                0xFF00695C,
                0xFF80DEEA,
                0xFF4DB6AC,
                0xFFFFCC80,
                0xFF33691E,
                0xFF827717,
                0xFF3E2723,
                0xFF26A69A,
                0xFF00897B,
                0xFF80CBC4,
                0xFF1565C0,
                0xFF01579B,
                0xFF263238,
                0xFF0D47A1,
                0xFF1B3A4B,
                0xFFB3E5FC,
                0xFF0288D1,
                0xFF00796B,
                0xFF607D8B,
                0xFF283593,
                0xFFA1887F,
                0xFF5D4037,
                0xFF039BE5,
                0xFF0097A7,
                0xFF26C6DA,
                0xFF00ACC1,
                0xFF4DD0E1,
                0xFFE0F7FA,
                0xFF81D4FA,
                0xFF4FC3F7,
                0xFF29B6F6,
                0xFF03A9F4,
                0xFF81C784,
                0xFF66BB6A,
                0xFFAED581,
                0xFF7CB342,
                0xFF9E9D24,
                0xFFC0CA33,
                0xFFAFB42B,
                0xFFEEFF41,
                0xFF8BC34A,
                0xFF689F38,
                0xFFCDDC39,
                0xFFF4FF81,
                0xFF6D4C41,
                0xFFBCAAA4,
                0xFF795548,
                0xFFEFEBE9,
                0xFF78909C,
                0xFF212121,
                0xFF424242,
                0xFF757575,
                0xFFE64A19,
                0xFFFF7043,
                0xFFFFAB91,
                0xFFDD2C00,
                0xFFFF6E40,
                0xFFFF9E80,
                0xFF6A1B9A,
                0xFF8E24AA,
                0xFFAB47BC,
                0xFFCE93D8,
                0xFF4A148C,
                0xFF7B1FA2,
                0xFF880E4F,
                0xFFAD1457,
                0xFFC2185B,
                0xFFE91E63,
                0xFFF06292,
                0xFFF8BBD0,
                0xFF00BFA5,
                0xFF1DE9B6,
                0xFF64FFDA,
                0xFF00B8D4,
                0xFF00E5FF,
                0xFF84FFFF,
                0xFFFF8F00,
                0xFFFFAB00,
                0xFFFFD54F,
                0xFFFFCA28,
                0xFFFFB300,
                0xFFFF8A65,
                0xFF311B92,
                0xFF4527A0,
                0xFF7E57C2,
                0xFF9575CD,
                0xFFB39DDB,
                0xFFD1C4E9,
                0xFFE65100,
                0xFFEF6C00,
                0xFFF57C00,
                0xFFFB8C00,
                0xFFFF9800,
                0xFFFFA726,
                0xFFFFB74D,
                0xFFFFA000,
                0xFFFFC400,
                0xFFFFE57F,
                0xFFFFECB3,
                0xFFFFF8E1,
                0xFFFDD835,
                0xFFFBC02D,
                0xFFF57F17,
                0xFFFFEA00,
                0xFFFFD600,
                0xFFFFAB40,
                0xFF388E3C,
                0xFF43A047,
                0xFF4CAF50,
                0xFFA5D6A7,
                0xFFC5E1A5,
                0xFFDCEDC8,
                false,
                TextDirection.LTR,
                false
        );
    }

    /// Returns the high-contrast theme.
    ///
    /// @return the tokens
    public static ThemeTokens highContrastTheme() {
        return new ThemeTokens(
                "high-contrast",
                0xFF000000,
                0xFFFFFF00,
                0xFF00FFFF,
                0xFF808080,
                0xFFFFFFFF,
                0xFFFFFF00,
                0xFFFF0000,
                0xFF00FFFF,
                0xFFFFFF00,
                1.0f,
                1.0f,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                0.0f,
                4,
                7.0f,
                3.0f,
                20.0f,
                0.0f,
                3.0f,
                0,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0x00000000,
                "linear",
                0.0f,
                1.0f,
                2.0f,
                1.0f,
                0x00000000,
                0x00000000,
                0xFFFFFF00,
                0xFFFFFFFF,
                0xFFFFFF00,
                0xFF00FFFF,
                0xFFFFFF00,
                0xFF00FF00,
                0xFF000000,
                0xFF000000,
                0xFF808080,
                0x00000000,
                0xFFFFFF00,
                0xFF000000,
                0xFFFF0000,
                0xFF000000,
                0xFFFFFF00,
                0xFF000000,
                0xFF000000,
                0xFF000000,
                0xFF000000,
                0xFFFFFFFF,
                0xFF000000,
                0xFF00FFFF,
                0xFF000000,
                0xFF000000,
                0xFF00FFFF,
                0xFF000000,
                0xFF000000,
                0xFF00FFFF,
                0xFF00FFFF,
                0xFF00FFFF,
                0xFF00FF00,
                0xFF00FFFF,
                0xFFFFFF00,
                0xFF00FFFF,
                0xFF00FF00,
                0xFF000000,
                0xFFFFFF00,
                0xFF000000,
                0xFF000000,
                0xFF00FFFF,
                0xFF808080,
                0xFFFFFFFF,
                0xFF808080,
                0xFF00FFFF,
                0xFF000000,
                0xFF00FFFF,
                0xFFFFFF00,
                0xFF808080,
                0xFF000000,
                0xFF000000,
                0xFFFFFF00,
                0xFF00FFFF,
                0xFF000000,
                0xFFFFFFFF,
                0xFFFFFF00,
                0xFF000000,
                0xFFFFFF00,
                0xFF00FF00,
                0xFFFFFF00,
                0xFF808080,
                0xFF808080,
                0xFFFFFF00,
                0xFFFFFFFF,
                0xFF00FFFF,
                0xFFFFFF00,
                0xFFFF0000,
                0xFFFFFFFF,
                0xFFFFFF00,
                0xFF00FFFF,
                0xFF808080,
                0xFF000000,
                0xFF808080,
                0xFFFFFF00,
                0xFFFFFFFF,
                0xFF808080,
                0xFF808080,
                0xFFFFFF00,
                0xFFFFFFFF,
                0xFFFF0000,
                0xFFFFFF00,
                0xFF000000,
                0xFF000000,
                0xFFFF0000,
                0xFFFFFF00,
                0xFF808080,
                0xFF00FF00,
                0xFFFF0000,
                0xFFFFFF00,
                0xFFFF0000,
                0xFF00FF00,
                0xFF000000,
                0xFFFFFF00,
                0xFF808080,
                0xFF00FFFF,
                0xFFFFFFFF,
                0xFF00FFFF,
                0xFF00FFFF,
                0xFF00FF00,
                0xFF00FFFF,
                0xFFFFFF00,
                0xFF00FF00,
                0xFF00FFFF,
                0xFF00FFFF,
                0xFF00FF00,
                0xFFFFFF00,
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
                true,
                TextDirection.LTR,
                false
        );
    }

    /// Returns a copy with the supplied reading direction.
    ///
    /// @param textDirection the reading direction
    /// @return the tokens
    public ThemeTokens withTextDirection(TextDirection textDirection) {
        return new ThemeTokens(
                name,
                backgroundArgb,
                textArgb,
                accentArgb,
                disabledArgb,
                focusArgb,
                selectionArgb,
                errorArgb,
                hoverArgb,
                borderArgb,
                fontScale,
                density,
                letterSpacing,
                cornerRadius,
                lineHeight,
                paragraphSpacing,
                wordSpacing,
                tabSize,
                minContrast,
                focusRingWidth,
                iconSize,
                elevation,
                focusRingOffset,
                animationDuration,
                shadowBlur,
                shadowSpread,
                shadowOffsetX,
                shadowOffsetY,
                shadowArgb,
                easing,
                easingDurationScale,
                disabledOpacity,
                strokeWidth,
                pressScale,
                overlayArgb,
                scrimArgb,
                dividerArgb,
                outlineArgb,
                caretArgb,
                linkArgb,
                warningArgb,
                successArgb,
                selectionTextArgb,
                canvasArgb,
                placeholderArgb,
                rippleArgb,
                tooltipArgb,
                surfaceArgb,
                badgeArgb,
                snackbarArgb,
                chipArgb,
                toastArgb,
                sheetArgb,
                menuArgb,
                drawerArgb,
                appBarArgb,
                cardArgb,
                fabArgb,
                navRailArgb,
                bottomBarArgb,
                tabArgb,
                dialogArgb,
                sidebarArgb,
                bannerArgb,
                spotlightArgb,
                selectionHandleArgb,
                switchArgb,
                checkboxArgb,
                radioArgb,
                sliderArgb,
                progressArgb,
                listArgb,
                treeArgb,
                tableArgb,
                textFieldArgb,
                toggleArgb,
                scrollbarArgb,
                splitArgb,
                searchArgb,
                statusArgb,
                accordionArgb,
                stepperArgb,
                paginationArgb,
                avatarArgb,
                breadcrumbArgb,
                calendarArgb,
                ratingArgb,
                timelineArgb,
                carouselArgb,
                dockArgb,
                notificationArgb,
                codeArgb,
                blockquoteArgb,
                mentionArgb,
                highlightArgb,
                watermarkArgb,
                kbdArgb,
                markArgb,
                insetArgb,
                captionArgb,
                overlineArgb,
                strikeArgb,
                outsetArgb,
                hairlineArgb,
                underlineArgb,
                footnoteArgb,
                captionTextArgb,
                shadeArgb,
                glowArgb,
                frostArgb,
                veilArgb,
                mistArgb,
                hazeArgb,
                sheenArgb,
                bloomArgb,
                flareArgb,
                filmArgb,
                duskArgb,
                emberArgb,
                sparkArgb,
                grainArgb,
                mossArgb,
                clayArgb,
                sandArgb,
                rustArgb,
                sageArgb,
                peatArgb,
                ochreArgb,
                slateArgb,
                inkArgb,
                foamArgb,
                brineArgb,
                tideArgb,
                kelpArgb,
                reefArgb,
                duneArgb,
                coveArgb,
                lagoonArgb,
                atollArgb,
                shoalArgb,
                spitArgb,
                marshArgb,
                fenArgb,
                bogArgb,
                cayArgb,
                inletArgb,
                soundArgb,
                bayArgb,
                gulfArgb,
                fjordArgb,
                lochArgb,
                tarnArgb,
                mereArgb,
                firthArgb,
                kyleArgb,
                nessArgb,
                harborArgb,
                quayArgb,
                pierArgb,
                bightArgb,
                reachArgb,
                poolArgb,
                channelArgb,
                straitArgb,
                havenArgb,
                riaArgb,
                loughArgb,
                voeArgb,
                wickArgb,
                holmArgb,
                geoArgb,
                ayreArgb,
                skerryArgb,
                stackArgb,
                braeArgb,
                glenArgb,
                strathArgb,
                combeArgb,
                daleArgb,
                valeArgb,
                fellArgb,
                moorArgb,
                heathArgb,
                woldArgb,
                downArgb,
                ridgeArgb,
                cragArgb,
                scarpArgb,
                knollArgb,
                torArgb,
                benArgb,
                lawArgb,
                cairnArgb,
                howeArgb,
                knoweArgb,
                kameArgb,
                drumlinArgb,
                eskerArgb,
                moraineArgb,
                screeArgb,
                talusArgb,
                cirqueArgb,
                areteArgb,
                colArgb,
                saddleArgb,
                couloirArgb,
                nunatakArgb,
                seracArgb,
                firnArgb,
                crevasseArgb,
                icefallArgb,
                neveArgb,
                sastrugiArgb,
                corrieArgb,
                gullyArgb,
                buttressArgb,
                hornArgb,
                aiguilleArgb,
                pizArgb,
                cwmArgb,
                gillArgb,
                cloughArgb,
                slackArgb,
                hassockArgb,
                sikeArgb,
                beckArgb,
                burnArgb,
                forceArgb,
                lynchetArgb,
                dingleArgb,
                ghyllArgb,
                rillArgb,
                bourneArgb,
                ladeArgb,
                leatArgb,
                stellArgb,
                lodeArgb,
                fossArgb,
                sladeArgb,
                dellArgb,
                deneArgb,
                nantArgb,
                linnArgb,
                copseArgb,
                spinneyArgb,
                shawArgb,
                carrArgb,
                holtArgb,
                hangarArgb,
                highContrast,
                textDirection,
                reducedMotion
        );
    }

    /// Encodes these tokens as a first-stable theme resource payload.
    ///
    /// @return UTF-8 fields separated by `|`
    public byte[] encode() {
        return (name
                + '|'
                + Integer.toUnsignedString(backgroundArgb, 16)
                + '|'
                + Integer.toUnsignedString(textArgb, 16)
                + '|'
                + Integer.toUnsignedString(accentArgb, 16)
                + '|'
                + Integer.toUnsignedString(disabledArgb, 16)
                + '|'
                + Integer.toUnsignedString(focusArgb, 16)
                + '|'
                + Integer.toUnsignedString(selectionArgb, 16)
                + '|'
                + Integer.toUnsignedString(errorArgb, 16)
                + '|'
                + Integer.toUnsignedString(hoverArgb, 16)
                + '|'
                + Integer.toUnsignedString(borderArgb, 16)
                + '|'
                + Float.toString(fontScale)
                + '|'
                + Float.toString(density)
                + '|'
                + Float.toString(letterSpacing)
                + '|'
                + Float.toString(cornerRadius)
                + '|'
                + Float.toString(lineHeight)
                + '|'
                + Float.toString(paragraphSpacing)
                + '|'
                + Float.toString(wordSpacing)
                + '|'
                + tabSize
                + '|'
                + Float.toString(minContrast)
                + '|'
                + Float.toString(focusRingWidth)
                + '|'
                + Float.toString(iconSize)
                + '|'
                + Float.toString(elevation)
                + '|'
                + Float.toString(focusRingOffset)
                + '|'
                + animationDuration
                + '|'
                + Float.toString(shadowBlur)
                + '|'
                + Float.toString(shadowSpread)
                + '|'
                + Float.toString(shadowOffsetX)
                + '|'
                + Float.toString(shadowOffsetY)
                + '|'
                + Integer.toUnsignedString(shadowArgb, 16)
                + '|'
                + easing
                + '|'
                + Float.toString(easingDurationScale)
                + '|'
                + Float.toString(disabledOpacity)
                + '|'
                + Float.toString(strokeWidth)
                + '|'
                + Float.toString(pressScale)
                + '|'
                + Integer.toUnsignedString(overlayArgb, 16)
                + '|'
                + Integer.toUnsignedString(scrimArgb, 16)
                + '|'
                + Integer.toUnsignedString(dividerArgb, 16)
                + '|'
                + Integer.toUnsignedString(outlineArgb, 16)
                + '|'
                + Integer.toUnsignedString(caretArgb, 16)
                + '|'
                + Integer.toUnsignedString(linkArgb, 16)
                + '|'
                + Integer.toUnsignedString(warningArgb, 16)
                + '|'
                + Integer.toUnsignedString(successArgb, 16)
                + '|'
                + Integer.toUnsignedString(selectionTextArgb, 16)
                + '|'
                + Integer.toUnsignedString(canvasArgb, 16)
                + '|'
                + Integer.toUnsignedString(placeholderArgb, 16)
                + '|'
                + Integer.toUnsignedString(rippleArgb, 16)
                + '|'
                + Integer.toUnsignedString(tooltipArgb, 16)
                + '|'
                + Integer.toUnsignedString(surfaceArgb, 16)
                + '|'
                + Integer.toUnsignedString(badgeArgb, 16)
                + '|'
                + Integer.toUnsignedString(snackbarArgb, 16)
                + '|'
                + Integer.toUnsignedString(chipArgb, 16)
                + '|'
                + Integer.toUnsignedString(toastArgb, 16)
                + '|'
                + Integer.toUnsignedString(sheetArgb, 16)
                + '|'
                + Integer.toUnsignedString(menuArgb, 16)
                + '|'
                + Integer.toUnsignedString(drawerArgb, 16)
                + '|'
                + Integer.toUnsignedString(appBarArgb, 16)
                + '|'
                + Integer.toUnsignedString(cardArgb, 16)
                + '|'
                + Integer.toUnsignedString(fabArgb, 16)
                + '|'
                + Integer.toUnsignedString(navRailArgb, 16)
                + '|'
                + Integer.toUnsignedString(bottomBarArgb, 16)
                + '|'
                + Integer.toUnsignedString(tabArgb, 16)
                + '|'
                + Integer.toUnsignedString(dialogArgb, 16)
                + '|'
                + Integer.toUnsignedString(sidebarArgb, 16)
                + '|'
                + Integer.toUnsignedString(bannerArgb, 16)
                + '|'
                + Integer.toUnsignedString(spotlightArgb, 16)
                + '|'
                + Integer.toUnsignedString(selectionHandleArgb, 16)
                + '|'
                + Integer.toUnsignedString(switchArgb, 16)
                + '|'
                + Integer.toUnsignedString(checkboxArgb, 16)
                + '|'
                + Integer.toUnsignedString(radioArgb, 16)
                + '|'
                + Integer.toUnsignedString(sliderArgb, 16)
                + '|'
                + Integer.toUnsignedString(progressArgb, 16)
                + '|'
                + Integer.toUnsignedString(listArgb, 16)
                + '|'
                + Integer.toUnsignedString(treeArgb, 16)
                + '|'
                + Integer.toUnsignedString(tableArgb, 16)
                + '|'
                + Integer.toUnsignedString(textFieldArgb, 16)
                + '|'
                + Integer.toUnsignedString(toggleArgb, 16)
                + '|'
                + Integer.toUnsignedString(scrollbarArgb, 16)
                + '|'
                + Integer.toUnsignedString(splitArgb, 16)
                + '|'
                + Integer.toUnsignedString(searchArgb, 16)
                + '|'
                + Integer.toUnsignedString(statusArgb, 16)
                + '|'
                + Integer.toUnsignedString(accordionArgb, 16)
                + '|'
                + Integer.toUnsignedString(stepperArgb, 16)
                + '|'
                + Integer.toUnsignedString(paginationArgb, 16)
                + '|'
                + Integer.toUnsignedString(avatarArgb, 16)
                + '|'
                + Integer.toUnsignedString(breadcrumbArgb, 16)
                + '|'
                + Integer.toUnsignedString(calendarArgb, 16)
                + '|'
                + Integer.toUnsignedString(ratingArgb, 16)
                + '|'
                + Integer.toUnsignedString(timelineArgb, 16)
                + '|'
                + Integer.toUnsignedString(carouselArgb, 16)
                + '|'
                + Integer.toUnsignedString(dockArgb, 16)
                + '|'
                + Integer.toUnsignedString(notificationArgb, 16)
                + '|'
                + Integer.toUnsignedString(codeArgb, 16)
                + '|'
                + Integer.toUnsignedString(blockquoteArgb, 16)
                + '|'
                + Integer.toUnsignedString(mentionArgb, 16)
                + '|'
                + Integer.toUnsignedString(highlightArgb, 16)
                + '|'
                + Integer.toUnsignedString(watermarkArgb, 16)
                + '|'
                + Integer.toUnsignedString(kbdArgb, 16)
                + '|'
                + Integer.toUnsignedString(markArgb, 16)
                + '|'
                + Integer.toUnsignedString(insetArgb, 16)
                + '|'
                + Integer.toUnsignedString(captionArgb, 16)
                + '|'
                + Integer.toUnsignedString(overlineArgb, 16)
                + '|'
                + Integer.toUnsignedString(strikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(outsetArgb, 16)
                + '|'
                + Integer.toUnsignedString(hairlineArgb, 16)
                + '|'
                + Integer.toUnsignedString(underlineArgb, 16)
                + '|'
                + Integer.toUnsignedString(footnoteArgb, 16)
                + '|'
                + Integer.toUnsignedString(captionTextArgb, 16)
                + '|'
                + Integer.toUnsignedString(shadeArgb, 16)
                + '|'
                + Integer.toUnsignedString(glowArgb, 16)
                + '|'
                + Integer.toUnsignedString(frostArgb, 16)
                + '|'
                + Integer.toUnsignedString(veilArgb, 16)
                + '|'
                + Integer.toUnsignedString(mistArgb, 16)
                + '|'
                + Integer.toUnsignedString(hazeArgb, 16)
                + '|'
                + Integer.toUnsignedString(sheenArgb, 16)
                + '|'
                + Integer.toUnsignedString(bloomArgb, 16)
                + '|'
                + Integer.toUnsignedString(flareArgb, 16)
                + '|'
                + Integer.toUnsignedString(filmArgb, 16)
                + '|'
                + Integer.toUnsignedString(duskArgb, 16)
                + '|'
                + Integer.toUnsignedString(emberArgb, 16)
                + '|'
                + Integer.toUnsignedString(sparkArgb, 16)
                + '|'
                + Integer.toUnsignedString(grainArgb, 16)
                + '|'
                + Integer.toUnsignedString(mossArgb, 16)
                + '|'
                + Integer.toUnsignedString(clayArgb, 16)
                + '|'
                + Integer.toUnsignedString(sandArgb, 16)
                + '|'
                + Integer.toUnsignedString(rustArgb, 16)
                + '|'
                + Integer.toUnsignedString(sageArgb, 16)
                + '|'
                + Integer.toUnsignedString(peatArgb, 16)
                + '|'
                + Integer.toUnsignedString(ochreArgb, 16)
                + '|'
                + Integer.toUnsignedString(slateArgb, 16)
                + '|'
                + Integer.toUnsignedString(inkArgb, 16)
                + '|'
                + Integer.toUnsignedString(foamArgb, 16)
                + '|'
                + Integer.toUnsignedString(brineArgb, 16)
                + '|'
                + Integer.toUnsignedString(tideArgb, 16)
                + '|'
                + Integer.toUnsignedString(kelpArgb, 16)
                + '|'
                + Integer.toUnsignedString(reefArgb, 16)
                + '|'
                + Integer.toUnsignedString(duneArgb, 16)
                + '|'
                + Integer.toUnsignedString(coveArgb, 16)
                + '|'
                + Integer.toUnsignedString(lagoonArgb, 16)
                + '|'
                + Integer.toUnsignedString(atollArgb, 16)
                + '|'
                + Integer.toUnsignedString(shoalArgb, 16)
                + '|'
                + Integer.toUnsignedString(spitArgb, 16)
                + '|'
                + Integer.toUnsignedString(marshArgb, 16)
                + '|'
                + Integer.toUnsignedString(fenArgb, 16)
                + '|'
                + Integer.toUnsignedString(bogArgb, 16)
                + '|'
                + Integer.toUnsignedString(cayArgb, 16)
                + '|'
                + Integer.toUnsignedString(inletArgb, 16)
                + '|'
                + Integer.toUnsignedString(soundArgb, 16)
                + '|'
                + Integer.toUnsignedString(bayArgb, 16)
                + '|'
                + Integer.toUnsignedString(gulfArgb, 16)
                + '|'
                + Integer.toUnsignedString(fjordArgb, 16)
                + '|'
                + Integer.toUnsignedString(lochArgb, 16)
                + '|'
                + Integer.toUnsignedString(tarnArgb, 16)
                + '|'
                + Integer.toUnsignedString(mereArgb, 16)
                + '|'
                + Integer.toUnsignedString(firthArgb, 16)
                + '|'
                + Integer.toUnsignedString(kyleArgb, 16)
                + '|'
                + Integer.toUnsignedString(nessArgb, 16)
                + '|'
                + Integer.toUnsignedString(harborArgb, 16)
                + '|'
                + Integer.toUnsignedString(quayArgb, 16)
                + '|'
                + Integer.toUnsignedString(pierArgb, 16)
                + '|'
                + Integer.toUnsignedString(bightArgb, 16)
                + '|'
                + Integer.toUnsignedString(reachArgb, 16)
                + '|'
                + Integer.toUnsignedString(poolArgb, 16)
                + '|'
                + Integer.toUnsignedString(channelArgb, 16)
                + '|'
                + Integer.toUnsignedString(straitArgb, 16)
                + '|'
                + Integer.toUnsignedString(havenArgb, 16)
                + '|'
                + Integer.toUnsignedString(riaArgb, 16)
                + '|'
                + Integer.toUnsignedString(loughArgb, 16)
                + '|'
                + Integer.toUnsignedString(voeArgb, 16)
                + '|'
                + Integer.toUnsignedString(wickArgb, 16)
                + '|'
                + Integer.toUnsignedString(holmArgb, 16)
                + '|'
                + Integer.toUnsignedString(geoArgb, 16)
                + '|'
                + Integer.toUnsignedString(ayreArgb, 16)
                + '|'
                + Integer.toUnsignedString(skerryArgb, 16)
                + '|'
                + Integer.toUnsignedString(stackArgb, 16)
                + '|'
                + Integer.toUnsignedString(braeArgb, 16)
                + '|'
                + Integer.toUnsignedString(glenArgb, 16)
                + '|'
                + Integer.toUnsignedString(strathArgb, 16)
                + '|'
                + Integer.toUnsignedString(combeArgb, 16)
                + '|'
                + Integer.toUnsignedString(daleArgb, 16)
                + '|'
                + Integer.toUnsignedString(valeArgb, 16)
                + '|'
                + Integer.toUnsignedString(fellArgb, 16)
                + '|'
                + Integer.toUnsignedString(moorArgb, 16)
                + '|'
                + Integer.toUnsignedString(heathArgb, 16)
                + '|'
                + Integer.toUnsignedString(woldArgb, 16)
                + '|'
                + Integer.toUnsignedString(downArgb, 16)
                + '|'
                + Integer.toUnsignedString(ridgeArgb, 16)
                + '|'
                + Integer.toUnsignedString(cragArgb, 16)
                + '|'
                + Integer.toUnsignedString(scarpArgb, 16)
                + '|'
                + Integer.toUnsignedString(knollArgb, 16)
                + '|'
                + Integer.toUnsignedString(torArgb, 16)
                + '|'
                + Integer.toUnsignedString(benArgb, 16)
                + '|'
                + Integer.toUnsignedString(lawArgb, 16)
                + '|'
                + Integer.toUnsignedString(cairnArgb, 16)
                + '|'
                + Integer.toUnsignedString(howeArgb, 16)
                + '|'
                + Integer.toUnsignedString(knoweArgb, 16)
                + '|'
                + Integer.toUnsignedString(kameArgb, 16)
                + '|'
                + Integer.toUnsignedString(drumlinArgb, 16)
                + '|'
                + Integer.toUnsignedString(eskerArgb, 16)
                + '|'
                + Integer.toUnsignedString(moraineArgb, 16)
                + '|'
                + Integer.toUnsignedString(screeArgb, 16)
                + '|'
                + Integer.toUnsignedString(talusArgb, 16)
                + '|'
                + Integer.toUnsignedString(cirqueArgb, 16)
                + '|'
                + Integer.toUnsignedString(areteArgb, 16)
                + '|'
                + Integer.toUnsignedString(colArgb, 16)
                + '|'
                + Integer.toUnsignedString(saddleArgb, 16)
                + '|'
                + Integer.toUnsignedString(couloirArgb, 16)
                + '|'
                + Integer.toUnsignedString(nunatakArgb, 16)
                + '|'
                + Integer.toUnsignedString(seracArgb, 16)
                + '|'
                + Integer.toUnsignedString(firnArgb, 16)
                + '|'
                + Integer.toUnsignedString(crevasseArgb, 16)
                + '|'
                + Integer.toUnsignedString(icefallArgb, 16)
                + '|'
                + Integer.toUnsignedString(neveArgb, 16)
                + '|'
                + Integer.toUnsignedString(sastrugiArgb, 16)
                + '|'
                + Integer.toUnsignedString(corrieArgb, 16)
                + '|'
                + Integer.toUnsignedString(gullyArgb, 16)
                + '|'
                + Integer.toUnsignedString(buttressArgb, 16)
                + '|'
                + Integer.toUnsignedString(hornArgb, 16)
                + '|'
                + Integer.toUnsignedString(aiguilleArgb, 16)
                + '|'
                + Integer.toUnsignedString(pizArgb, 16)
                + '|'
                + Integer.toUnsignedString(cwmArgb, 16)
                + '|'
                + Integer.toUnsignedString(gillArgb, 16)
                + '|'
                + Integer.toUnsignedString(cloughArgb, 16)
                + '|'
                + Integer.toUnsignedString(slackArgb, 16)
                + '|'
                + Integer.toUnsignedString(hassockArgb, 16)
                + '|'
                + Integer.toUnsignedString(sikeArgb, 16)
                + '|'
                + Integer.toUnsignedString(beckArgb, 16)
                + '|'
                + Integer.toUnsignedString(burnArgb, 16)
                + '|'
                + Integer.toUnsignedString(forceArgb, 16)
                + '|'
                + Integer.toUnsignedString(lynchetArgb, 16)
                + '|'
                + Integer.toUnsignedString(dingleArgb, 16)
                + '|'
                + Integer.toUnsignedString(ghyllArgb, 16)
                + '|'
                + Integer.toUnsignedString(rillArgb, 16)
                + '|'
                + Integer.toUnsignedString(bourneArgb, 16)
                + '|'
                + Integer.toUnsignedString(ladeArgb, 16)
                + '|'
                + Integer.toUnsignedString(leatArgb, 16)
                + '|'
                + Integer.toUnsignedString(stellArgb, 16)
                + '|'
                + Integer.toUnsignedString(lodeArgb, 16)
                + '|'
                + Integer.toUnsignedString(fossArgb, 16)
                + '|'
                + Integer.toUnsignedString(sladeArgb, 16)
                + '|'
                + Integer.toUnsignedString(dellArgb, 16)
                + '|'
                + Integer.toUnsignedString(deneArgb, 16)
                + '|'
                + Integer.toUnsignedString(nantArgb, 16)
                + '|'
                + Integer.toUnsignedString(linnArgb, 16)
                + '|'
                + Integer.toUnsignedString(copseArgb, 16)
                + '|'
                + Integer.toUnsignedString(spinneyArgb, 16)
                + '|'
                + Integer.toUnsignedString(shawArgb, 16)
                + '|'
                + Integer.toUnsignedString(carrArgb, 16)
                + '|'
                + Integer.toUnsignedString(holtArgb, 16)
                + '|'
                + Integer.toUnsignedString(hangarArgb, 16)
                + '|'
                + highContrast
                + '|'
                + textDirection.name()
                + '|'
                + reducedMotion).getBytes(StandardCharsets.UTF_8);
    }

    /// Decodes a payload produced by [`#encode()`].
    ///
    /// @param bytes the resource bytes
    /// @return the tokens
    public static ThemeTokens decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        String[] fields = new String(bytes, StandardCharsets.UTF_8).split("\\|", -1);
        if (fields.length != 252) {
            throw new IllegalArgumentException("Theme resource must have two-hundred-fifty-two fields");
        }
        return new ThemeTokens(
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
                parseFontScale(fields[10]),
                parseDensity(fields[11]),
                parseLetterSpacing(fields[12]),
                parseCornerRadius(fields[13]),
                parseLineHeight(fields[14]),
                parseParagraphSpacing(fields[15]),
                parseWordSpacing(fields[16]),
                parseTabSize(fields[17]),
                parseMinContrast(fields[18]),
                parseFocusRingWidth(fields[19]),
                parseIconSize(fields[20]),
                parseElevation(fields[21]),
                parseFocusRingOffset(fields[22]),
                parseAnimationDuration(fields[23]),
                parseShadowBlur(fields[24]),
                parseShadowSpread(fields[25]),
                parseShadowOffset(fields[26], "shadowOffsetX"),
                parseShadowOffset(fields[27], "shadowOffsetY"),
                parseArgb(fields[28]),
                fields[29],
                parseEasingDurationScale(fields[30]),
                parseDisabledOpacity(fields[31]),
                parseStrokeWidth(fields[32]),
                parsePressScale(fields[33]),
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
                Boolean.parseBoolean(fields[249]),
                TextDirection.valueOf(fields[250]),
                Boolean.parseBoolean(fields[251])
        );
    }

    /// Parses the relative type-size field.
    private static float parseFontScale(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme fontScale field is not a finite number", failure);
        }
    }

    /// Parses the relative spacing-scale field.
    private static float parseDensity(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme density field is not a finite number", failure);
        }
    }

    /// Parses the extra tracking field.
    private static float parseLetterSpacing(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme letterSpacing field is not a finite number", failure);
        }
    }

    /// Parses the corner-radius field.
    private static float parseCornerRadius(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme cornerRadius field is not a finite number", failure);
        }
    }

    /// Parses the relative line-height field.
    private static float parseLineHeight(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme lineHeight field is not a finite number", failure);
        }
    }

    /// Parses the extra paragraph-spacing field.
    private static float parseParagraphSpacing(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme paragraphSpacing field is not a finite number", failure);
        }
    }

    /// Parses the extra word-spacing field.
    private static float parseWordSpacing(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme wordSpacing field is not a finite number", failure);
        }
    }

    /// Parses the tab-stop field.
    private static int parseTabSize(String field) {
        try {
            return Integer.parseInt(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme tabSize field is not an integer", failure);
        }
    }

    /// Parses the minimum contrast-ratio field.
    private static float parseMinContrast(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme minContrast field is not a finite number", failure);
        }
    }

    /// Parses the focus-ring width field.
    private static float parseFocusRingWidth(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme focusRingWidth field is not a finite number", failure);
        }
    }

    /// Parses the icon-size field.
    private static float parseIconSize(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme iconSize field is not a finite number", failure);
        }
    }

    /// Parses the elevation field.
    private static float parseElevation(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme elevation field is not a finite number", failure);
        }
    }

    /// Parses the focus-ring offset field.
    private static float parseFocusRingOffset(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme focusRingOffset field is not a finite number", failure);
        }
    }

    /// Parses the motion-duration field.
    private static int parseAnimationDuration(String field) {
        try {
            return Integer.parseInt(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme animationDuration field is not an integer", failure);
        }
    }

    /// Parses the shadow-blur field.
    private static float parseShadowBlur(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme shadowBlur field is not a finite number", failure);
        }
    }

    /// Parses the shadow-spread field.
    private static float parseShadowSpread(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme shadowSpread field is not a finite number", failure);
        }
    }

    /// Parses one shadow-offset field.
    private static float parseShadowOffset(String field, String name) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme " + name + " field is not a finite number", failure);
        }
    }

    /// Parses the pressed-control scale field.
    private static float parsePressScale(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme pressScale field is not a finite number", failure);
        }
    }

    /// Parses the hairline stroke-width field.
    private static float parseStrokeWidth(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme strokeWidth field is not a finite number", failure);
        }
    }

    /// Parses the disabled-content opacity field.
    private static float parseDisabledOpacity(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme disabledOpacity field is not a finite number", failure);
        }
    }

    /// Parses the motion-duration scale field.
    private static float parseEasingDurationScale(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme easingDurationScale field is not a finite number", failure);
        }
    }

    /// Parses one unsigned hex ARGB field.
    private static int parseArgb(String field) {
        try {
            return Integer.parseUnsignedInt(field, 16);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme ARGB field is not hexadecimal", failure);
        }
    }

    /// Returns a copy with the supplied reduced-motion policy.
    ///
    /// @param reducedMotion whether nonessential motion must be suppressed
    /// @return the tokens
    public ThemeTokens withReducedMotion(boolean reducedMotion) {
        return new ThemeTokens(
                name,
                backgroundArgb,
                textArgb,
                accentArgb,
                disabledArgb,
                focusArgb,
                selectionArgb,
                errorArgb,
                hoverArgb,
                borderArgb,
                fontScale,
                density,
                letterSpacing,
                cornerRadius,
                lineHeight,
                paragraphSpacing,
                wordSpacing,
                tabSize,
                minContrast,
                focusRingWidth,
                iconSize,
                elevation,
                focusRingOffset,
                animationDuration,
                shadowBlur,
                shadowSpread,
                shadowOffsetX,
                shadowOffsetY,
                shadowArgb,
                easing,
                easingDurationScale,
                disabledOpacity,
                strokeWidth,
                pressScale,
                overlayArgb,
                scrimArgb,
                dividerArgb,
                outlineArgb,
                caretArgb,
                linkArgb,
                warningArgb,
                successArgb,
                selectionTextArgb,
                canvasArgb,
                placeholderArgb,
                rippleArgb,
                tooltipArgb,
                surfaceArgb,
                badgeArgb,
                snackbarArgb,
                chipArgb,
                toastArgb,
                sheetArgb,
                menuArgb,
                drawerArgb,
                appBarArgb,
                cardArgb,
                fabArgb,
                navRailArgb,
                bottomBarArgb,
                tabArgb,
                dialogArgb,
                sidebarArgb,
                bannerArgb,
                spotlightArgb,
                selectionHandleArgb,
                switchArgb,
                checkboxArgb,
                radioArgb,
                sliderArgb,
                progressArgb,
                listArgb,
                treeArgb,
                tableArgb,
                textFieldArgb,
                toggleArgb,
                scrollbarArgb,
                splitArgb,
                searchArgb,
                statusArgb,
                accordionArgb,
                stepperArgb,
                paginationArgb,
                avatarArgb,
                breadcrumbArgb,
                calendarArgb,
                ratingArgb,
                timelineArgb,
                carouselArgb,
                dockArgb,
                notificationArgb,
                codeArgb,
                blockquoteArgb,
                mentionArgb,
                highlightArgb,
                watermarkArgb,
                kbdArgb,
                markArgb,
                insetArgb,
                captionArgb,
                overlineArgb,
                strikeArgb,
                outsetArgb,
                hairlineArgb,
                underlineArgb,
                footnoteArgb,
                captionTextArgb,
                shadeArgb,
                glowArgb,
                frostArgb,
                veilArgb,
                mistArgb,
                hazeArgb,
                sheenArgb,
                bloomArgb,
                flareArgb,
                filmArgb,
                duskArgb,
                emberArgb,
                sparkArgb,
                grainArgb,
                mossArgb,
                clayArgb,
                sandArgb,
                rustArgb,
                sageArgb,
                peatArgb,
                ochreArgb,
                slateArgb,
                inkArgb,
                foamArgb,
                brineArgb,
                tideArgb,
                kelpArgb,
                reefArgb,
                duneArgb,
                coveArgb,
                lagoonArgb,
                atollArgb,
                shoalArgb,
                spitArgb,
                marshArgb,
                fenArgb,
                bogArgb,
                cayArgb,
                inletArgb,
                soundArgb,
                bayArgb,
                gulfArgb,
                fjordArgb,
                lochArgb,
                tarnArgb,
                mereArgb,
                firthArgb,
                kyleArgb,
                nessArgb,
                harborArgb,
                quayArgb,
                pierArgb,
                bightArgb,
                reachArgb,
                poolArgb,
                channelArgb,
                straitArgb,
                havenArgb,
                riaArgb,
                loughArgb,
                voeArgb,
                wickArgb,
                holmArgb,
                geoArgb,
                ayreArgb,
                skerryArgb,
                stackArgb,
                braeArgb,
                glenArgb,
                strathArgb,
                combeArgb,
                daleArgb,
                valeArgb,
                fellArgb,
                moorArgb,
                heathArgb,
                woldArgb,
                downArgb,
                ridgeArgb,
                cragArgb,
                scarpArgb,
                knollArgb,
                torArgb,
                benArgb,
                lawArgb,
                cairnArgb,
                howeArgb,
                knoweArgb,
                kameArgb,
                drumlinArgb,
                eskerArgb,
                moraineArgb,
                screeArgb,
                talusArgb,
                cirqueArgb,
                areteArgb,
                colArgb,
                saddleArgb,
                couloirArgb,
                nunatakArgb,
                seracArgb,
                firnArgb,
                crevasseArgb,
                icefallArgb,
                neveArgb,
                sastrugiArgb,
                corrieArgb,
                gullyArgb,
                buttressArgb,
                hornArgb,
                aiguilleArgb,
                pizArgb,
                cwmArgb,
                gillArgb,
                cloughArgb,
                slackArgb,
                hassockArgb,
                sikeArgb,
                beckArgb,
                burnArgb,
                forceArgb,
                lynchetArgb,
                dingleArgb,
                ghyllArgb,
                rillArgb,
                bourneArgb,
                ladeArgb,
                leatArgb,
                stellArgb,
                lodeArgb,
                fossArgb,
                sladeArgb,
                dellArgb,
                deneArgb,
                nantArgb,
                linnArgb,
                copseArgb,
                spinneyArgb,
                shawArgb,
                carrArgb,
                holtArgb,
                hangarArgb,
                highContrast,
                textDirection,
                reducedMotion
        );
    }
}
