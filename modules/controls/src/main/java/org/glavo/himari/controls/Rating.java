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

/// Creates an unstyled discrete star rating.
///
/// The value is a clamped star count in `[0, maximum]` published as `N of M`.
/// Activation increments without wrapping, which distinguishes this control from
/// [`Pagination`] and from [`NumberStepper`]'s unlabeled integer spin.
@NotNullByDefault
public final class Rating {
    /// Default control size.
    private static final Size SIZE = new Size(120.0f, 24.0f);

    /// Inclusive maximum star count.
    private final int maximum;

    /// Current star count.
    private int value;

    /// Whether the control ignores adjustment.
    private boolean disabled;

    /// Mounted leaf that receives the published value.
    private @Nullable LayoutNode node;

    /// Creates a rating.
    ///
    /// @param maximum the inclusive maximum star count, at least `1`
    /// @param value the initial star count
    public Rating(int maximum, int value) {
        if (maximum < 1) {
            throw new IllegalArgumentException("Rating maximum must be at least 1");
        }
        this.maximum = maximum;
        this.value = clamp(value);
    }

    /// Returns the inclusive maximum star count.
    ///
    /// @return the maximum
    public int maximum() {
        return maximum;
    }

    /// Returns the current star count.
    ///
    /// @return the value
    public int value() {
        return value;
    }

    /// Returns the published `N of M` label.
    ///
    /// @return the label
    public String label() {
        return value + " of " + maximum;
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the star count, clamped to `[0, maximum]`, and publishes it when mounted.
    ///
    /// @param value the next value
    public void setValue(int value) {
        if (disabled) {
            return;
        }
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

    /// Advances by one star, clamping at [`#maximum()`] without wrapping.
    public void increment() {
        if (disabled) {
            return;
        }
        value = clamp(value + 1);
        publish();
    }

    /// Moves back by one star, clamping at `0` without wrapping.
    public void decrement() {
        if (disabled) {
            return;
        }
        value = clamp(value - 1);
        publish();
    }

    /// Builds the rating leaf.
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
                SemanticsRole.RATING,
                label(),
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

    /// Publishes the star count and disabled state onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(label());
        node.setDisabled(disabled);
        node.setRangeValue(value);
        node.setRangeExtent(0, maximum);
        node.setItemStatus(label());
    }

    /// Clamps `value` into `[0, maximum]`.
    private int clamp(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > maximum) {
            return maximum;
        }
        return value;
    }
}
