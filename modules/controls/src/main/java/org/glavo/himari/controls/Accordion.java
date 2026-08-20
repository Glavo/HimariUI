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

/// Creates an unstyled accordion of mutually exclusive expandable sections.
@NotNullByDefault
public final class Accordion {
    /// Default control size.
    private static final Size SIZE = new Size(200.0f, 24.0f);

    /// Section titles in document order.
    private final List<String> sections;

    /// Expanded section index.
    private int expanded;

    /// Whether the control ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published expansion.
    private @Nullable LayoutNode node;

    /// Creates an accordion with the first section expanded.
    ///
    /// @param sections the non-empty section titles
    public Accordion(List<String> sections) {
        this.sections = List.copyOf(sections);
        if (this.sections.size() < 2) {
            throw new IllegalArgumentException("Accordion must contain at least two sections");
        }
        for (String section : this.sections) {
            Objects.requireNonNull(section, "section");
            if (section.isEmpty()) {
                throw new IllegalArgumentException("Accordion section must not be empty");
            }
        }
        this.expanded = 0;
    }

    /// Returns the section titles.
    ///
    /// @return the titles
    public @Unmodifiable List<String> sections() {
        return sections;
    }

    /// Returns the expanded index.
    ///
    /// @return the index
    public int expanded() {
        return expanded;
    }

    /// Returns the expanded section title.
    ///
    /// @return the title
    public String value() {
        return sections.get(expanded);
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Expands `index` and publishes it when mounted.
    ///
    /// @param index the section index
    public void expand(int index) {
        if (index < 0 || index >= sections.size()) {
            throw new IllegalArgumentException("Accordion index is out of range");
        }
        if (disabled) {
            return;
        }
        expanded = index;
        publish();
    }

    /// Expands the next section, wrapping at the end.
    public void expandNext() {
        if (disabled) {
            return;
        }
        expanded = (expanded + 1) % sections.size();
        publish();
    }

    /// Expands the previous section, wrapping at the start.
    public void expandPrevious() {
        if (disabled) {
            return;
        }
        expanded = (expanded + sections.size() - 1) % sections.size();
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the accordion leaf.
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
                SemanticsRole.ACCORDION,
                value(),
                Set.of(SemanticsAction.ACTIVATE, SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                this::expandNext,
                delta -> {
                    if (delta > 0) {
                        expandNext();
                    } else {
                        expandPrevious();
                    }
                }
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes expansion and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(value());
        node.setDisabled(disabled);
        node.setItemStatus("expanded");
        node.setPositionInSet(expanded);
        node.setSizeOfSet(sections.size());
    }
}
