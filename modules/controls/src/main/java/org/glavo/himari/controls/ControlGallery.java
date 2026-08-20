package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.bootstrap.BootstrapLabel;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.input.gesture.GestureArena;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.TextDirection;
import org.glavo.himari.runtime.animation.AnimationReplacementPolicy;
import org.glavo.himari.runtime.animation.AnimationTransaction;
import org.glavo.himari.runtime.animation.MotionImportance;
import org.glavo.himari.runtime.animation.MotionPolicy;
import org.glavo.himari.runtime.animation.MotionSpec;
import org.glavo.himari.runtime.reload.ResourceKind;
import org.glavo.himari.runtime.reload.ResourceReload;
import org.glavo.himari.runtime.reload.ResourceSnapshot;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.ValueLayout;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/// Builds one column that exercises the first-stable unstyled control set.
@NotNullByDefault
public final class ControlGallery {
    /// Button used by the gallery.
    private final Button button;

    /// Icon button used by the gallery.
    private final IconButton iconButton;

    /// Image used by the gallery.
    private final Image image;

    /// Drawing surface used by the gallery.
    private final Canvas canvas;

    /// Spacer used by the gallery.
    private final Spacer spacer;

    /// Checkbox used by the gallery.
    private final Checkbox checkbox;

    /// Radio group used by the gallery.
    private final Radio radio;

    /// Combo box used by the gallery.
    private final ComboBox combo;

    /// Date picker used by the gallery.
    private final DatePicker datePicker;

    /// Time picker used by the gallery.
    private final TimePicker timePicker;

    /// Color picker used by the gallery.
    private final ColorPicker colorPicker;

    /// Integer stepper used by the gallery.
    private final NumberStepper stepper;

    /// Disclosure used by the gallery.
    private final Disclosure disclosure;

    /// Search field used by the gallery.
    private final SearchField search;

    /// Separator used by the gallery.
    private final Separator separator;

    /// Toolbar used by the gallery.
    private final Toolbar toolbar;

    /// Breadcrumb used by the gallery.
    private final Breadcrumb breadcrumb;

    /// Hyperlink used by the gallery.
    private final Hyperlink link;

    /// Accordion used by the gallery.
    private final Accordion accordion;

    /// Matched-geometry pair used by the gallery.
    private final SharedElement shared;

    /// Toggle used by the gallery.
    private final Toggle toggle;

    /// Slider used by the gallery.
    private final Slider slider;

    /// Scrollbar used by the gallery.
    private final Scrollbar scrollbar;

    /// Progress indicator used by the gallery.
    private final Progress progress;

    /// Scroll viewport used by the gallery.
    private final ScrollViewport scroll;

    /// Lazy list used by the gallery.
    private final LazyList list;

    /// Virtualized table used by the gallery.
    private final LazyTable table;

    /// Virtualized grid used by the gallery.
    private final LazyGrid grid;

    /// Text field used by the gallery.
    private final TextField field;

    /// Multiline text area used by the gallery.
    private final TextArea area;

    /// Polite live-region status used by the gallery.
    private final LiveStatus status;

    /// In-window popup used by the gallery.
    private final Popup popup;

    /// Menu used by the gallery.
    private final Menu menu;

    /// Dialog used by the gallery.
    private final Dialog dialog;

    /// Tooltip used by the gallery.
    private final Tooltip tooltip;

    /// Tabs used by the gallery.
    private final Tabs tabs;

    /// Split pane used by the gallery.
    private final SplitPane split;

    /// Flex row used by the gallery.
    private final Flex flex;

    /// Wrapping flow used by the gallery.
    private final Flow flow;

    /// Constraint grid used by the gallery.
    private final Grid layoutGrid;

    /// Tree used by the gallery.
    private final Tree tree;

    /// Theme tokens used by the gallery.
    private ThemeTokens theme;

    /// Extra theme surfaces that do not fit in [`ThemeTokens`].
    private ThemeSurfaces surfaces;

    /// Extra theme overlays that do not fit in [`ThemeTokens`] or [`ThemeSurfaces`].
    private ThemeOverlays overlays;

    /// Extra theme washes that do not fit in [`ThemeTokens`], [`ThemeSurfaces`], or [`ThemeOverlays`].
    private ThemeWashes washes;

    /// Extra theme glazes that do not fit in [`ThemeTokens`], [`ThemeSurfaces`],
    /// [`ThemeOverlays`], or [`ThemeWashes`].
    private ThemeGlazes glazes;

    /// Extra theme sheens that do not fit in [`ThemeTokens`], [`ThemeSurfaces`],
    /// [`ThemeOverlays`], [`ThemeWashes`], or [`ThemeGlazes`].
    private ThemeSheens sheens;

    /// Extra theme films that do not fit in [`ThemeTokens`], [`ThemeSurfaces`],
    /// [`ThemeOverlays`], [`ThemeWashes`], [`ThemeGlazes`], or [`ThemeSheens`].
    private ThemeFilms films;

    /// Last UTF-8 font family installed by [#applyFontReload(ResourceReload, String)].
    private @Nullable String fontFamily;

    /// External button activation counter.
    private final AtomicInteger externalClicks = new AtomicInteger();

    /// Gesture arena used by the gallery pointer path.
    private final GestureArena gestures = new GestureArena();

    /// Monotonic pointer clock used when callers omit a timestamp.
    private long pointerClock;

    /// Creates a gallery with default initial values.
    public ControlGallery() {
        this.button = new Button("Increment", externalClicks::incrementAndGet);
        this.iconButton = new IconButton("plus", () -> { });
        this.image = new Image("logo");
        this.canvas = new Canvas("Sketch", new Size(48.0f, 32.0f));
        this.spacer = new Spacer(new Size(8.0f, 8.0f));
        this.checkbox = new Checkbox("Agree");
        this.radio = new Radio(List.of("A", "B"));
        this.combo = new ComboBox(List.of("Red", "Green", "Blue"));
        this.datePicker = new DatePicker(2026, 8, 20);
        this.timePicker = new TimePicker(13, 45);
        this.colorPicker = new ColorPicker(0x33, 0x66, 0x99);
        this.stepper = new NumberStepper(0, 10, 1, 3);
        this.disclosure = new Disclosure("More");
        this.search = new SearchField();
        this.separator = new Separator();
        this.toolbar = new Toolbar(List.of("Cut", "Copy", "Paste"));
        this.breadcrumb = new Breadcrumb(List.of("Home", "Docs", "API"));
        this.link = new Hyperlink("Plans", "plans");
        this.accordion = new Accordion(List.of("Alpha", "Beta", "Gamma"));
        this.shared = new SharedElement("gallery", "hero");
        this.toggle = new Toggle("Muted");
        this.slider = new Slider("Volume", 0.0f, 10.0f, 1.0f, 3.0f);
        this.scrollbar = new Scrollbar("Thumb", 0.0f, 100.0f, 10.0f, 20.0f);
        this.progress = new Progress("Load", 0.0f, 1.0f, 0.25f);
        this.scroll = new ScrollViewport(16.0f);
        this.list = new LazyList(20, 4);
        this.table = new LazyTable(2, 1);
        this.table.addRow("r0", 20.0f);
        this.table.addRow("r1", 20.0f);
        this.table.addRow("r2", 24.0f);
        this.table.addRow("r3", 20.0f);
        this.table.addRow("r4", 20.0f);
        this.table.setViewport(0.0f, 40.0f);
        this.grid = new LazyGrid(8, 2, 2);
        this.field = new TextField();
        this.area = new TextArea();
        this.status = new LiveStatus("Ready");
        this.popup = new Popup("Overlay");
        this.menu = new Menu("File", List.of(new Button("Open", () -> { })));
        this.dialog = new Dialog("Confirm");
        this.tooltip = new Tooltip("Hint");
        this.tabs = new Tabs(List.of("One", "Two"));
        this.split = new SplitPane(0.4f);
        this.flex = new Flex(1.0f, 1.0f);
        this.flow = new Flow();
        this.layoutGrid = new Grid(2);
        this.tree = new Tree(List.of(
                new Tree.Item("root", "Root", 0, true),
                new Tree.Item("child", "Child", 1, false)
        ));
        this.theme = ThemeTokens.standard();
        this.surfaces = ThemeSurfaces.standard();
        this.overlays = ThemeOverlays.standard();
        this.washes = ThemeWashes.standard();
        this.glazes = ThemeGlazes.standard();
        this.sheens = ThemeSheens.standard();
        this.films = ThemeFilms.standard();
    }

    /// Returns the button.
    ///
    /// @return the button
    public Button button() {
        return button;
    }

    /// Returns the icon button.
    ///
    /// @return the icon button
    public IconButton iconButton() {
        return iconButton;
    }

    /// Returns the image.
    ///
    /// @return the image
    public Image image() {
        return image;
    }

    /// Returns the drawing surface.
    ///
    /// @return the canvas
    public Canvas canvas() {
        return canvas;
    }

    /// Returns the spacer.
    ///
    /// @return the spacer
    public Spacer spacer() {
        return spacer;
    }

    /// Returns the checkbox.
    ///
    /// @return the checkbox
    public Checkbox checkbox() {
        return checkbox;
    }

    /// Returns the radio group.
    ///
    /// @return the radio group
    public Radio radio() {
        return radio;
    }

    /// Returns the combo box.
    ///
    /// @return the combo box
    public ComboBox combo() {
        return combo;
    }

    /// Returns the date picker.
    ///
    /// @return the date picker
    public DatePicker datePicker() {
        return datePicker;
    }

    /// Returns the time picker.
    ///
    /// @return the time picker
    public TimePicker timePicker() {
        return timePicker;
    }

    /// Returns the color picker.
    ///
    /// @return the color picker
    public ColorPicker colorPicker() {
        return colorPicker;
    }

    /// Returns the integer stepper.
    ///
    /// @return the stepper
    public NumberStepper stepper() {
        return stepper;
    }

    /// Returns the disclosure.
    ///
    /// @return the disclosure
    public Disclosure disclosure() {
        return disclosure;
    }

    /// Returns the search field.
    ///
    /// @return the search field
    public SearchField search() {
        return search;
    }

    /// Returns the separator.
    ///
    /// @return the separator
    public Separator separator() {
        return separator;
    }

    /// Returns the toolbar.
    ///
    /// @return the toolbar
    public Toolbar toolbar() {
        return toolbar;
    }

    /// Returns the breadcrumb.
    ///
    /// @return the breadcrumb
    public Breadcrumb breadcrumb() {
        return breadcrumb;
    }

    /// Returns the hyperlink.
    ///
    /// @return the hyperlink
    public Hyperlink link() {
        return link;
    }

    /// Returns the accordion.
    ///
    /// @return the accordion
    public Accordion accordion() {
        return accordion;
    }

    /// Returns the matched-geometry pair.
    ///
    /// @return the shared element
    public SharedElement shared() {
        return shared;
    }

    /// Returns the toggle.
    ///
    /// @return the toggle
    public Toggle toggle() {
        return toggle;
    }

    /// Returns the slider.
    ///
    /// @return the slider
    public Slider slider() {
        return slider;
    }

    /// Returns the scrollbar.
    ///
    /// @return the scrollbar
    public Scrollbar scrollbar() {
        return scrollbar;
    }

    /// Returns the progress indicator.
    ///
    /// @return the progress
    public Progress progress() {
        return progress;
    }

    /// Returns the scroll viewport.
    ///
    /// @return the viewport
    public ScrollViewport scroll() {
        return scroll;
    }

    /// Returns the lazy list.
    ///
    /// @return the list
    public LazyList list() {
        return list;
    }

    /// Returns the virtualized table.
    ///
    /// @return the table
    public LazyTable table() {
        return table;
    }

    /// Returns the virtualized grid.
    ///
    /// @return the grid
    public LazyGrid grid() {
        return grid;
    }

    /// Returns the text field.
    ///
    /// @return the field
    public TextField field() {
        return field;
    }

    /// Returns the multiline text area.
    ///
    /// @return the area
    public TextArea area() {
        return area;
    }

    /// Returns the polite live-region status.
    ///
    /// @return the status
    public LiveStatus status() {
        return status;
    }

    /// Returns the popup.
    ///
    /// @return the popup
    public Popup popup() {
        return popup;
    }

    /// Returns the menu.
    ///
    /// @return the menu
    public Menu menu() {
        return menu;
    }

    /// Returns the dialog.
    ///
    /// @return the dialog
    public Dialog dialog() {
        return dialog;
    }

    /// Returns the tooltip.
    ///
    /// @return the tooltip
    public Tooltip tooltip() {
        return tooltip;
    }

    /// Returns the tabs.
    ///
    /// @return the tabs
    public Tabs tabs() {
        return tabs;
    }

    /// Returns the split pane.
    ///
    /// @return the split
    public SplitPane split() {
        return split;
    }

    /// Returns the flex row.
    ///
    /// @return the flex
    public Flex flex() {
        return flex;
    }

    /// Returns the wrapping flow.
    ///
    /// @return the flow
    public Flow flow() {
        return flow;
    }

    /// Returns the constraint grid.
    ///
    /// @return the grid
    public Grid layoutGrid() {
        return layoutGrid;
    }

    /// Returns the tree.
    ///
    /// @return the tree
    public Tree tree() {
        return tree;
    }

    /// Returns the theme tokens.
    ///
    /// @return the tokens
    public ThemeTokens theme() {
        return theme;
    }

    /// Returns the extra theme surfaces.
    ///
    /// @return the surfaces
    public ThemeSurfaces surfaces() {
        return surfaces;
    }

    /// Returns the extra theme overlays.
    ///
    /// @return the overlays
    public ThemeOverlays overlays() {
        return overlays;
    }

    /// Returns the extra theme washes.
    ///
    /// @return the washes
    public ThemeWashes washes() {
        return washes;
    }

    /// Returns the extra theme glazes.
    ///
    /// @return the glazes
    public ThemeGlazes glazes() {
        return glazes;
    }

    /// Returns the extra theme sheens.
    ///
    /// @return the sheens
    public ThemeSheens sheens() {
        return sheens;
    }

    /// Returns the extra theme films.
    ///
    /// @return the films
    public ThemeFilms films() {
        return films;
    }

    /// Returns the last font family installed by [#applyFontReload(ResourceReload, String)].
    ///
    /// @return the family, or `null` before the first successful font reload
    public @Nullable String fontFamily() {
        return fontFamily;
    }

    /// Returns the gallery gesture arena.
    ///
    /// @return the arena
    public GestureArena gestures() {
        return gestures;
    }

    /// Replaces the theme tokens used by the next [#create(LayoutTree)].
    ///
    /// @param theme the tokens
    public void setTheme(ThemeTokens theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    /// Applies the current published theme generation for `key` from `reload`.
    ///
    /// @param reload the resource generations
    /// @param key the theme consumer key
    /// @return whether a theme payload was installed
    public boolean applyThemeReload(ResourceReload reload, String key) {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(key, "key");
        @Nullable ResourceSnapshot snapshot = reload.current(ResourceKind.THEME, key);
        if (snapshot == null) {
            return false;
        }
        this.theme = ThemeTokens.decode(snapshot.bytes().toArray(ValueLayout.JAVA_BYTE));
        return true;
    }

    /// Replaces the extra theme surfaces used by the next [#create(LayoutTree)].
    ///
    /// @param surfaces the surfaces
    public void setSurfaces(ThemeSurfaces surfaces) {
        this.surfaces = Objects.requireNonNull(surfaces, "surfaces");
    }

    /// Applies the current published extra-surface generation for `key` from `reload`.
    ///
    /// @param reload the resource generations
    /// @param key the surface consumer key
    /// @return whether a surface payload was installed
    public boolean applySurfacesReload(ResourceReload reload, String key) {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(key, "key");
        @Nullable ResourceSnapshot snapshot = reload.current(ResourceKind.THEME, key);
        if (snapshot == null) {
            return false;
        }
        this.surfaces = ThemeSurfaces.decode(snapshot.bytes().toArray(ValueLayout.JAVA_BYTE));
        return true;
    }

    /// Replaces the extra theme overlays used by the next [#create(LayoutTree)].
    ///
    /// @param overlays the overlays
    public void setOverlays(ThemeOverlays overlays) {
        this.overlays = Objects.requireNonNull(overlays, "overlays");
    }

    /// Applies the current published extra-overlay generation for `key` from `reload`.
    ///
    /// @param reload the resource generations
    /// @param key the overlay consumer key
    /// @return whether an overlay payload was installed
    public boolean applyOverlaysReload(ResourceReload reload, String key) {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(key, "key");
        @Nullable ResourceSnapshot snapshot = reload.current(ResourceKind.THEME, key);
        if (snapshot == null) {
            return false;
        }
        this.overlays = ThemeOverlays.decode(snapshot.bytes().toArray(ValueLayout.JAVA_BYTE));
        return true;
    }

    /// Replaces the extra theme washes used by the next [#create(LayoutTree)].
    ///
    /// @param washes the washes
    public void setWashes(ThemeWashes washes) {
        this.washes = Objects.requireNonNull(washes, "washes");
    }

    /// Applies the current published extra-wash generation for `key` from `reload`.
    ///
    /// @param reload the resource generations
    /// @param key the wash consumer key
    /// @return whether a wash payload was installed
    public boolean applyWashesReload(ResourceReload reload, String key) {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(key, "key");
        @Nullable ResourceSnapshot snapshot = reload.current(ResourceKind.THEME, key);
        if (snapshot == null) {
            return false;
        }
        this.washes = ThemeWashes.decode(snapshot.bytes().toArray(ValueLayout.JAVA_BYTE));
        return true;
    }

    /// Replaces the extra theme glazes used by the next [#create(LayoutTree)].
    ///
    /// @param glazes the glazes
    public void setGlazes(ThemeGlazes glazes) {
        this.glazes = Objects.requireNonNull(glazes, "glazes");
    }

    /// Applies the current published extra-glaze generation for `key` from `reload`.
    ///
    /// @param reload the resource generations
    /// @param key the glaze consumer key
    /// @return whether a glaze payload was installed
    public boolean applyGlazesReload(ResourceReload reload, String key) {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(key, "key");
        @Nullable ResourceSnapshot snapshot = reload.current(ResourceKind.THEME, key);
        if (snapshot == null) {
            return false;
        }
        this.glazes = ThemeGlazes.decode(snapshot.bytes().toArray(ValueLayout.JAVA_BYTE));
        return true;
    }

    /// Replaces the extra theme sheens used by the next [#create(LayoutTree)].
    ///
    /// @param sheens the sheens
    public void setSheens(ThemeSheens sheens) {
        this.sheens = Objects.requireNonNull(sheens, "sheens");
    }

    /// Applies the current published extra-sheen generation for `key` from `reload`.
    ///
    /// @param reload the resource generations
    /// @param key the sheen consumer key
    /// @return whether a sheen payload was installed
    public boolean applySheensReload(ResourceReload reload, String key) {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(key, "key");
        @Nullable ResourceSnapshot snapshot = reload.current(ResourceKind.THEME, key);
        if (snapshot == null) {
            return false;
        }
        this.sheens = ThemeSheens.decode(snapshot.bytes().toArray(ValueLayout.JAVA_BYTE));
        return true;
    }

    /// Replaces the extra theme films used by the next [#create(LayoutTree)].
    ///
    /// @param films the films
    public void setFilms(ThemeFilms films) {
        this.films = Objects.requireNonNull(films, "films");
    }

    /// Applies the current published extra-film generation for `key` from `reload`.
    ///
    /// @param reload the resource generations
    /// @param key the film consumer key
    /// @return whether a film payload was installed
    public boolean applyFilmsReload(ResourceReload reload, String key) {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(key, "key");
        @Nullable ResourceSnapshot snapshot = reload.current(ResourceKind.THEME, key);
        if (snapshot == null) {
            return false;
        }
        this.films = ThemeFilms.decode(snapshot.bytes().toArray(ValueLayout.JAVA_BYTE));
        return true;
    }

    /// Applies the current published style generation for `key` from `reload`.
    ///
    /// A payload containing `reducedMotion=true` or `rtl=true` updates those tokens. Other
    /// tokens are left unchanged.
    ///
    /// @param reload the resource generations
    /// @param key the style consumer key
    /// @return whether a style payload was installed
    public boolean applyStyleReload(ResourceReload reload, String key) {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(key, "key");
        @Nullable ResourceSnapshot snapshot = reload.current(ResourceKind.STYLE, key);
        if (snapshot == null) {
            return false;
        }
        String payload = new String(snapshot.bytes().toArray(ValueLayout.JAVA_BYTE), java.nio.charset.StandardCharsets.UTF_8);
        if (payload.contains("reducedMotion=true")) {
            this.theme = this.theme.withReducedMotion(true);
        }
        if (payload.contains("rtl=true")) {
            this.theme = this.theme.withTextDirection(TextDirection.RTL);
        }
        return true;
    }

    /// Applies the current published image generation for `key` from `reload`.
    ///
    /// A UTF-8 payload becomes the gallery icon-button name used by the next
    /// [#create(LayoutTree)].
    ///
    /// @param reload the resource generations
    /// @param key the image consumer key
    /// @return whether an image payload was installed
    public boolean applyImageReload(ResourceReload reload, String key) {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(key, "key");
        @Nullable ResourceSnapshot snapshot = reload.current(ResourceKind.IMAGE, key);
        if (snapshot == null) {
            return false;
        }
        String name = new String(snapshot.bytes().toArray(ValueLayout.JAVA_BYTE), java.nio.charset.StandardCharsets.UTF_8);
        if (name.isEmpty()) {
            return false;
        }
        iconButton.setIcon(name);
        image.setSource(name);
        return true;
    }

    /// Applies the current published font generation for `key` from `reload`.
    ///
    /// A UTF-8 payload becomes the gallery font family.
    ///
    /// @param reload the resource generations
    /// @param key the font consumer key
    /// @return whether a font payload was installed
    public boolean applyFontReload(ResourceReload reload, String key) {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(key, "key");
        @Nullable ResourceSnapshot snapshot = reload.current(ResourceKind.FONT, key);
        if (snapshot == null) {
            return false;
        }
        String family = new String(snapshot.bytes().toArray(ValueLayout.JAVA_BYTE), java.nio.charset.StandardCharsets.UTF_8);
        if (family.isEmpty()) {
            return false;
        }
        this.fontFamily = family;
        return true;
    }

    /// Resolves one animation transaction from the current theme's reduced-motion policy.
    ///
    /// @param transactionId the positive transaction identity
    /// @param requested the motion requested by the control
    /// @param importance whether the target is essential
    /// @return the transaction whose effective motion follows [#theme()]
    public AnimationTransaction resolveMotion(
            long transactionId,
            MotionSpec requested,
            MotionImportance importance
    ) {
        return MotionPolicy.resolve(
                theme.reducedMotion(),
                importance,
                transactionId,
                transactionId,
                1L,
                requested,
                AnimationReplacementPolicy.PRESERVE_VELOCITY
        );
    }

    /// Returns external button activations.
    ///
    /// @return the count
    public int externalClicks() {
        return externalClicks.get();
    }

    /// Builds the gallery tree root.
    ///
    /// @param tree the layout tree
    /// @return the root
    public LayoutNode create(LayoutTree tree) {
        Objects.requireNonNull(tree, "tree");
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode tallContent = factory.leaf(
                "scroll-content",
                new Size(180.0f, 200.0f),
                List.of(),
                false,
                SemanticsRole.NONE,
                "Content",
                Set.of(),
                null
        );
        Alignment alignment = theme.textDirection() == TextDirection.RTL ? Alignment.END : Alignment.START;
        List<LayoutModifier> modifiers = theme.textDirection() == TextDirection.RTL
                ? List.of(new LayoutModifier.MinSize(400.0f, 0.0f), new LayoutModifier.Padding(8.0f))
                : List.of(new LayoutModifier.Padding(8.0f));
        return factory.column(
                "gallery",
                alignment,
                modifiers,
                BootstrapLabel.create(factory, "title", "Controls"),
                button.create(factory, "button"),
                iconButton.create(factory, "icon-button"),
                image.create(factory, "image"),
                canvas.create(factory, "canvas"),
                spacer.create(factory, "spacer"),
                checkbox.create(factory, "checkbox"),
                radio.create(factory, "radio"),
                combo.create(factory, "combo"),
                datePicker.create(factory, "date"),
                timePicker.create(factory, "time"),
                colorPicker.create(factory, "color"),
                stepper.create(factory, "stepper"),
                disclosure.create(factory, "disclosure"),
                search.create(factory, "search"),
                separator.create(factory, "separator"),
                toolbar.create(factory, "toolbar"),
                breadcrumb.create(factory, "breadcrumb"),
                link.create(factory, "link"),
                accordion.create(factory, "accordion"),
                shared.create(factory, "shared"),
                toggle.create(factory, "toggle"),
                slider.create(factory, "slider"),
                scrollbar.create(factory, "scrollbar"),
                progress.create(factory, "progress"),
                scroll.create(factory, "scroll", tallContent),
                list.create(factory, "list"),
                table.create(factory, "table"),
                grid.create(factory, "grid"),
                field.create(factory, "field"),
                area.create(factory, "area"),
                status.create(factory, "status"),
                popup.create(factory, "popup"),
                menu.create(factory, "menu"),
                dialog.create(factory, "dialog"),
                tooltip.create(factory, "tooltip"),
                tabs.create(factory, "tabs"),
                split.create(factory, "split"),
                flex.create(factory, "flex"),
                flow.create(factory, "flow"),
                layoutGrid.create(factory, "layout-grid"),
                this.tree.create(factory, "tree")
        );
    }

    /// Dispatches a pointer event to open popups first, then the layout tree.
    ///
    /// @param tree the placed tree built by [#create(LayoutTree)]
    /// @param event the pointer event
    /// @return whether a popup or layout node handled the event
    public boolean dispatchPointer(LayoutTree tree, PointerEvent event) {
        pointerClock += 16_000_000L;
        return dispatchPointer(tree, event, pointerClock);
    }

    /// Dispatches a timestamped pointer event through popups, the gesture arena, then the tree.
    ///
    /// An accepted drag applies its last vertical delta to the scroll viewport. An accepted
    /// drag `UP` starts a fling from the arena velocity. A long press
    /// announces through the gallery status. Down and up events still reach the layout tree so
    /// existing activation and focus routing remain unchanged.
    ///
    /// @param tree the placed tree built by [#create(LayoutTree)]
    /// @param event the pointer event
    /// @param timestampNanos the nonnegative event timestamp
    /// @return whether a popup, gesture, or layout node handled the event
    public boolean dispatchPointer(LayoutTree tree, PointerEvent event, long timestampNanos) {
        Objects.requireNonNull(tree, "tree");
        Objects.requireNonNull(event, "event");
        if (dialog.handlePointer(tree, event)
                || menu.handlePointer(tree, event)
                || tooltip.handlePointer(tree, event)
                || popup.handlePointer(tree, event)) {
            return true;
        }
        gestures.dispatch(event, timestampNanos);
        if (gestures.longPressAccepted() && event.type() != PointerEventType.MOVE) {
            status.announce("Long press");
        }
        if (gestures.dragAccepted() && event.type() == PointerEventType.MOVE) {
            scroll.scrollBy(Math.max(0.0f, -gestures.lastDeltaY()));
            return true;
        }
        if (gestures.dragAccepted() && event.type() == PointerEventType.UP) {
            scroll.fling(-gestures.velocity().y());
        }
        if (gestures.scrollAccepted() && event.type() == PointerEventType.WHEEL) {
            scroll.scrollBy(-gestures.lastScrollDelta() * scroll.step());
            return true;
        }
        if (gestures.scrollAccepted() && event.type() == PointerEventType.WHEEL_HORIZONTAL) {
            scroll.scrollByHorizontal(-gestures.lastScrollDelta() * scroll.step());
            return true;
        }
        if (event.type() == PointerEventType.MOVE) {
            return gestures.winner() != null;
        }
        return tree.dispatch(event);
    }

    /// Dispatches a key event to open popups first, then the layout tree.
    ///
    /// @param tree the placed tree built by [#create(LayoutTree)]
    /// @param event the key event
    /// @return whether a popup or layout node handled the event
    public boolean dispatchKey(LayoutTree tree, KeyEvent event) {
        Objects.requireNonNull(tree, "tree");
        Objects.requireNonNull(event, "event");
        if (dialog.handleKey(event)
                || menu.handleKey(event)
                || tooltip.handleKey(event)
                || popup.handleKey(event)) {
            return true;
        }
        return tree.dispatch(event);
    }
}
