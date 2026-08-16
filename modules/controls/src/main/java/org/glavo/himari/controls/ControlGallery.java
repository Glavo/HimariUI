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
import org.jetbrains.annotations.NotNullByDefault;

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

    /// Checkbox used by the gallery.
    private final Checkbox checkbox;

    /// Radio group used by the gallery.
    private final Radio radio;

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

    /// Tree used by the gallery.
    private final Tree tree;

    /// Theme tokens used by the gallery.
    private ThemeTokens theme;

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
        this.checkbox = new Checkbox("Agree");
        this.radio = new Radio(List.of("A", "B"));
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
        this.field = new TextField();
        this.area = new TextArea();
        this.status = new LiveStatus("Ready");
        this.popup = new Popup("Overlay");
        this.menu = new Menu("File", List.of(new Button("Open", () -> { })));
        this.dialog = new Dialog("Confirm");
        this.tooltip = new Tooltip("Hint");
        this.tabs = new Tabs(List.of("One", "Two"));
        this.split = new SplitPane(0.4f);
        this.tree = new Tree(List.of(
                new Tree.Item("root", "Root", 0, true),
                new Tree.Item("child", "Child", 1, false)
        ));
        this.theme = ThemeTokens.standard();
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
                checkbox.create(factory, "checkbox"),
                radio.create(factory, "radio"),
                toggle.create(factory, "toggle"),
                slider.create(factory, "slider"),
                scrollbar.create(factory, "scrollbar"),
                progress.create(factory, "progress"),
                scroll.create(factory, "scroll", tallContent),
                list.create(factory, "list"),
                table.create(factory, "table"),
                field.create(factory, "field"),
                area.create(factory, "area"),
                status.create(factory, "status"),
                popup.create(factory, "popup"),
                menu.create(factory, "menu"),
                dialog.create(factory, "dialog"),
                tooltip.create(factory, "tooltip"),
                tabs.create(factory, "tabs"),
                split.create(factory, "split"),
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
    /// An accepted drag applies its last vertical delta to the scroll viewport. A long press
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
