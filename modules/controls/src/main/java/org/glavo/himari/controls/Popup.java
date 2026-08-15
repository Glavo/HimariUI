package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an in-window overlay popup with explicit show and dismiss.
@NotNullByDefault
public final class Popup {
    /// Accessible name.
    private final String label;

    /// Whether the popup is visible.
    private boolean open;

    /// Creates a closed popup.
    ///
    /// @param label the accessible name
    public Popup(String label) {
        this.label = Objects.requireNonNull(label, "label");
    }

    /// Returns whether the popup is open.
    ///
    /// @return whether it is open
    public boolean isOpen() {
        return open;
    }

    /// Shows the popup.
    public void show() {
        open = true;
    }

    /// Dismisses the popup.
    public void dismiss() {
        open = false;
    }

    /// Builds the overlay leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        float height = open ? 48.0f : 0.0f;
        return factory.leaf(
                name,
                new Size(160.0f, height),
                List.of(new LayoutModifier.Padding(open ? 4.0f : 0.0f)),
                open,
                SemanticsRole.NONE,
                open ? label : "",
                open ? Set.of(SemanticsAction.ACTIVATE) : Set.of(),
                this::dismiss
        );
    }
}
