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

/// Creates an unstyled integer stepper.
@NotNullByDefault
public final class NumberStepper {
    /// Default control size.
    private static final Size SIZE = new Size(80.0f, 24.0f);

    /// Inclusive minimum.
    private final int minimum;

    /// Inclusive maximum.
    private final int maximum;

    /// Positive step applied by increment or decrement.
    private final int step;

    /// Current value.
    private int value;

    /// Whether the control ignores adjustment.
    private boolean disabled;

    /// Mounted leaf that receives the published value.
    private @Nullable LayoutNode node;

    /// Creates a stepper.
    ///
    /// @param minimum the inclusive minimum
    /// @param maximum the inclusive maximum
    /// @param step the positive step
    /// @param value the initial value
    public NumberStepper(int minimum, int maximum, int step, int value) {
        if (maximum < minimum) {
            throw new IllegalArgumentException("NumberStepper maximum must be at least the minimum");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("NumberStepper step must be positive");
        }
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.value = clamp(value);
    }

    /// Returns the current value.
    ///
    /// @return the value
    public int value() {
        return value;
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the value, clamped to the published range, and publishes it when mounted.
    ///
    /// @param value the next value
    public void setValue(int value) {
        this.value = clamp(value);
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Advances by [`#step`], clamping at the maximum.
    public void increment() {
        if (disabled) {
            return;
        }
        value = clamp(value + step);
        publish();
    }

    /// Moves back by [`#step`], clamping at the minimum.
    public void decrement() {
        if (disabled) {
            return;
        }
        value = clamp(value - step);
        publish();
    }

    /// Builds the stepper leaf.
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
                SemanticsRole.STEPPER,
                Integer.toString(value),
                Set.of(SemanticsAction.ACTIVATE, SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                this::increment,
                delta -> {
                    if (delta > 0) {
                        increment();
                    } else {
                        decrement();
                    }
                }
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes the decimal value and disabled state onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(Integer.toString(value));
        node.setDisabled(disabled);
        node.setRangeValue(value);
        node.setRangeExtent(minimum, maximum);
    }

    /// Clamps `value` into `[minimum, maximum]`.
    private int clamp(int value) {
        if (value < minimum) {
            return minimum;
        }
        if (value > maximum) {
            return maximum;
        }
        return value;
    }
}
