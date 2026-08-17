package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled two-state switch.
@NotNullByDefault
public final class Toggle {
    /// Default control size.
    private static final Size SIZE = new Size(48.0f, 24.0f);

    /// Accessible name.
    private final String label;

    /// Whether the switch is on.
    private boolean on;

    /// Whether the switch ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published toggle state.
    private @Nullable LayoutNode node;

    /// Creates an off switch.
    ///
    /// @param label the accessible name
    public Toggle(String label) {
        this.label = Objects.requireNonNull(label, "label");
    }

    /// Returns whether the switch is on.
    ///
    /// @return the state
    public boolean isOn() {
        return on;
    }

    /// Sets the switch state and publishes it to the mounted leaf when present.
    ///
    /// @param on the new state
    public void setOn(boolean on) {
        this.on = on;
        if (node != null) {
            node.setSelected(on);
        }
    }

    /// Returns whether the switch is disabled.
    ///
    /// @return whether the switch is disabled
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

    /// Builds the switch leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        LayoutNode created = factory.leaf(
                name,
                SIZE,
                List.of(new LayoutModifier.Padding(2.0f)),
                true,
                SemanticsRole.TOGGLE,
                label,
                Set.of(SemanticsAction.ACTIVATE),
                this::flip
        );
        created.setSelected(on);
        created.setDisabled(disabled);
        this.node = created;
        return created;
    }

    /// Flips the switch.
    private void flip() {
        if (disabled) {
            return;
        }
        on = !on;
        if (node != null) {
            node.setSelected(on);
        }
    }
}
