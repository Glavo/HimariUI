package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.bootstrap.BootstrapLabel;
import org.glavo.himari.layout.semantics.SemanticsRole;
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

    /// Toggle used by the gallery.
    private final Toggle toggle;

    /// Slider used by the gallery.
    private final Slider slider;

    /// Scroll viewport used by the gallery.
    private final ScrollViewport scroll;

    /// Lazy list used by the gallery.
    private final LazyList list;

    /// Text field used by the gallery.
    private final TextField field;

    /// In-window popup used by the gallery.
    private final Popup popup;

    /// Theme tokens used by the gallery.
    private final ThemeTokens theme;

    /// External button activation counter.
    private final AtomicInteger externalClicks = new AtomicInteger();

    /// Creates a gallery with default initial values.
    public ControlGallery() {
        this.button = new Button("Increment", externalClicks::incrementAndGet);
        this.toggle = new Toggle("Muted");
        this.slider = new Slider("Volume", 0.0f, 10.0f, 1.0f, 3.0f);
        this.scroll = new ScrollViewport(16.0f);
        this.list = new LazyList(20, 4);
        this.field = new TextField();
        this.popup = new Popup("Menu");
        this.theme = ThemeTokens.standard();
    }

    /// Returns the button.
    ///
    /// @return the button
    public Button button() {
        return button;
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

    /// Returns the text field.
    ///
    /// @return the field
    public TextField field() {
        return field;
    }

    /// Returns the popup.
    ///
    /// @return the popup
    public Popup popup() {
        return popup;
    }

    /// Returns the theme tokens.
    ///
    /// @return the tokens
    public ThemeTokens theme() {
        return theme;
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
        return factory.column(
                "gallery",
                Alignment.START,
                List.of(new LayoutModifier.Padding(8.0f)),
                BootstrapLabel.create(factory, "title", "Controls"),
                button.create(factory, "button"),
                toggle.create(factory, "toggle"),
                slider.create(factory, "slider"),
                scroll.create(factory, "scroll", tallContent),
                list.create(factory, "list"),
                field.create(factory, "field"),
                popup.create(factory, "popup")
        );
    }
}
