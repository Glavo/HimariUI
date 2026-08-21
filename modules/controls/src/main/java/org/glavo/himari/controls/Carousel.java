package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled labeled slide carousel.
///
/// Unlike [`Pagination`], slides have titles and activation advances without wrapping.
@NotNullByDefault
public final class Carousel {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 24.0f);

    /// Ordered slide titles.
    private final @Unmodifiable List<String> slides;

    /// Zero-based current slide.
    private int index;

    /// Whether the control ignores adjustment.
    private boolean disabled;

    /// Mounted leaf that receives the published slide.
    private @Nullable LayoutNode node;

    /// Creates a carousel starting at the first slide.
    ///
    /// @param slides the titles, at least two non-empty strings
    public Carousel(List<String> slides) {
        Objects.requireNonNull(slides, "slides");
        if (slides.size() < 2) {
            throw new IllegalArgumentException("Carousel must contain at least two slides");
        }
        for (String slide : slides) {
            Objects.requireNonNull(slide, "slide");
            if (slide.isEmpty()) {
                throw new IllegalArgumentException("Carousel slide titles must not be empty");
            }
        }
        this.slides = List.copyOf(slides);
        this.index = 0;
    }

    /// Returns the slide titles.
    ///
    /// @return the titles
    public @Unmodifiable List<String> slides() {
        return slides;
    }

    /// Returns the zero-based current slide.
    ///
    /// @return the index
    public int index() {
        return index;
    }

    /// Returns the current slide title.
    ///
    /// @return the title
    public String value() {
        return slides.get(index);
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Selects `index` and publishes it when mounted.
    ///
    /// @param index the zero-based slide
    public void setIndex(int index) {
        if (index < 0 || index >= slides.size()) {
            throw new IllegalArgumentException("Carousel index is out of range");
        }
        if (disabled) {
            return;
        }
        this.index = index;
        publish();
    }

    /// Advances to the next slide, clamping at the last title.
    public void next() {
        if (disabled) {
            return;
        }
        if (index < slides.size() - 1) {
            index++;
            publish();
        }
    }

    /// Moves to the previous slide, clamping at the first title.
    public void previous() {
        if (disabled) {
            return;
        }
        if (index > 0) {
            index--;
            publish();
        }
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the carousel leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        LayoutNode leaf = factory.leaf(
                name,
                SIZE,
                List.of(new LayoutModifier.Padding(0.0f)),
                true,
                SemanticsRole.CAROUSEL,
                value(),
                Set.of(SemanticsAction.ACTIVATE, SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                this::next,
                delta -> {
                    if (delta > 0) {
                        next();
                    } else {
                        previous();
                    }
                }
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes slide and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(value());
        node.setDisabled(disabled);
        node.setPositionInSet(index);
        node.setSizeOfSet(slides.size());
        node.setRangeValue(index);
        node.setRangeExtent(0, slides.size() - 1);
        node.setItemStatus(value());
    }
}
