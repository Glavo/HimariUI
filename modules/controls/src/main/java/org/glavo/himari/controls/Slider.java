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

/// Creates an unstyled bounded numeric slider.
@NotNullByDefault
public final class Slider {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 24.0f);

    /// Accessible name.
    private final String label;

    /// Inclusive minimum.
    private final float minimum;

    /// Inclusive maximum.
    private final float maximum;

    /// Step applied by increment or decrement.
    private final float step;

    /// Current value.
    private float value;

    /// Whether the slider ignores adjustment.
    private boolean disabled;

    /// Mounted leaf that receives the published range value.
    private @Nullable LayoutNode node;

    /// Creates a slider.
    ///
    /// @param label the accessible name
    /// @param minimum the inclusive minimum
    /// @param maximum the inclusive maximum
    /// @param step the positive step
    /// @param value the initial value
    public Slider(String label, float minimum, float maximum, float step, float value) {
        this.label = Objects.requireNonNull(label, "label");
        if (!Float.isFinite(minimum) || !Float.isFinite(maximum) || !Float.isFinite(step) || !Float.isFinite(value)) {
            throw new IllegalArgumentException("Slider extents must be finite");
        }
        if (maximum < minimum) {
            throw new IllegalArgumentException("Slider maximum must be at least the minimum");
        }
        if (step <= 0.0f) {
            throw new IllegalArgumentException("Slider step must be positive");
        }
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.value = clamp(value);
    }

    /// Returns the current value.
    ///
    /// @return the value
    public float value() {
        return value;
    }

    /// Returns whether the slider is disabled.
    ///
    /// @return whether the slider is disabled
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

    /// Builds the slider leaf.
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
                SemanticsRole.SLIDER,
                label,
                Set.of(SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                null,
                this::adjust
        );
        created.setRangeValue(value);
        created.setDisabled(disabled);
        this.node = created;
        return created;
    }

    /// Applies one signed step.
    ///
    /// @param delta `1` or `-1`
    private void adjust(int delta) {
        if (disabled) {
            return;
        }
        value = clamp(value + delta * step);
        if (node != null) {
            node.setRangeValue(value);
        }
    }

    /// Clamps a candidate into the published range.
    ///
    /// @param candidate the candidate
    /// @return the clamped value
    private float clamp(float candidate) {
        return Math.min(maximum, Math.max(minimum, candidate));
    }
}
