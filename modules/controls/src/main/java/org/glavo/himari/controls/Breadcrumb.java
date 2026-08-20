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

/// Creates an unstyled breadcrumb trail of path segments.
@NotNullByDefault
public final class Breadcrumb {
    /// Default control size.
    private static final Size SIZE = new Size(200.0f, 24.0f);

    /// Path segments in document order.
    private final List<String> segments;

    /// Selected segment index.
    private int selected;

    /// Whether the control ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published selection.
    private @Nullable LayoutNode node;

    /// Creates a breadcrumb.
    ///
    /// The last segment is selected.
    ///
    /// @param segments the non-empty path labels
    public Breadcrumb(List<String> segments) {
        this.segments = List.copyOf(segments);
        if (this.segments.size() < 2) {
            throw new IllegalArgumentException("Breadcrumb must contain at least two segments");
        }
        for (String segment : this.segments) {
            Objects.requireNonNull(segment, "segment");
            if (segment.isEmpty()) {
                throw new IllegalArgumentException("Breadcrumb segment must not be empty");
            }
        }
        this.selected = this.segments.size() - 1;
    }

    /// Returns the path segments.
    ///
    /// @return the labels
    public @Unmodifiable List<String> segments() {
        return segments;
    }

    /// Returns the selected index.
    ///
    /// @return the index
    public int selected() {
        return selected;
    }

    /// Returns the selected segment label.
    ///
    /// @return the label
    public String value() {
        return segments.get(selected);
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Selects `index` and publishes it when mounted.
    ///
    /// @param index the segment index
    public void select(int index) {
        if (index < 0 || index >= segments.size()) {
            throw new IllegalArgumentException("Breadcrumb index is out of range");
        }
        if (disabled) {
            return;
        }
        selected = index;
        publish();
    }

    /// Selects the next segment, wrapping at the end.
    public void selectNext() {
        if (disabled) {
            return;
        }
        selected = (selected + 1) % segments.size();
        publish();
    }

    /// Selects the previous segment, wrapping at the start.
    public void selectPrevious() {
        if (disabled) {
            return;
        }
        selected = (selected + segments.size() - 1) % segments.size();
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the breadcrumb leaf.
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
                SemanticsRole.BREADCRUMB,
                value(),
                Set.of(SemanticsAction.ACTIVATE, SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                this::selectPrevious,
                delta -> {
                    if (delta > 0) {
                        selectNext();
                    } else {
                        selectPrevious();
                    }
                }
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes selection and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(value());
        node.setDisabled(disabled);
        node.setPositionInSet(selected);
        node.setSizeOfSet(segments.size());
    }
}
