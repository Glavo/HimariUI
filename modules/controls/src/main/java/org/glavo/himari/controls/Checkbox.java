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

/// Creates an unstyled independently checkable box.
@NotNullByDefault
public final class Checkbox {
    /// Default control size.
    private static final Size SIZE = new Size(20.0f, 20.0f);

    /// Accessible name.
    private final String label;

    /// Whether the box is checked.
    private boolean checked;

    /// Whether the box ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published checked state.
    private @Nullable LayoutNode node;

    /// Creates an unchecked box.
    ///
    /// @param label the accessible name
    public Checkbox(String label) {
        this.label = Objects.requireNonNull(label, "label");
    }

    /// Returns whether the box is checked.
    ///
    /// @return the state
    public boolean isChecked() {
        return checked;
    }

    /// Sets the checked state and publishes it to the mounted leaf when present.
    ///
    /// @param checked the state
    public void setChecked(boolean checked) {
        this.checked = checked;
        if (node != null) {
            node.setSelected(checked);
        }
    }

    /// Returns whether the box is disabled.
    ///
    /// @return whether the box is disabled
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

    /// Builds the checkbox leaf.
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
                SemanticsRole.CHECKBOX,
                label,
                Set.of(SemanticsAction.ACTIVATE),
                this::toggle
        );
        created.setSelected(checked);
        created.setDisabled(disabled);
        this.node = created;
        return created;
    }

    /// Flips the checked state.
    private void toggle() {
        if (disabled) {
            return;
        }
        checked = !checked;
        if (node != null) {
            node.setSelected(checked);
        }
    }
}
