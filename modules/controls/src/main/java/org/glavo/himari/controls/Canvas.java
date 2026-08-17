package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled drawing-surface leaf.
@NotNullByDefault
public final class Canvas {
    /// Accessible name.
    private final String label;

    /// Intrinsic size.
    private final Size size;

    /// Whether the canvas is disabled.
    private boolean disabled;

    /// Mounted leaf that receives the published disabled state.
    private @Nullable LayoutNode node;

    /// Creates a canvas.
    ///
    /// @param label the accessible name
    /// @param size the positive intrinsic size
    public Canvas(String label, Size size) {
        this.label = Objects.requireNonNull(label, "label");
        this.size = Objects.requireNonNull(size, "size");
        if (label.isEmpty() || size.width() <= 0.0f || size.height() <= 0.0f) {
            throw new IllegalArgumentException("Canvas label must be non-empty and size must be positive");
        }
    }

    /// Returns the accessible name.
    ///
    /// @return the name
    public String label() {
        return label;
    }

    /// Returns the intrinsic size.
    ///
    /// @return the size
    public Size size() {
        return size;
    }

    /// Builds the canvas leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        LayoutNode created = factory.leaf(
                name,
                size,
                List.of(),
                false,
                SemanticsRole.CANVAS,
                label,
                Set.of(),
                null
        );
        created.setDisabled(disabled);
        this.node = created;
        return created;
    }

    /// Returns whether the canvas is disabled.
    ///
    /// @return whether the canvas is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to the mounted leaf when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (node != null) {
            node.setDisabled(disabled);
        }
    }
}
