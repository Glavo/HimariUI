package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsScroll;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled bounded scrollbar.
@NotNullByDefault
public final class Scrollbar {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 16.0f);

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

    /// Mounted leaf that receives the published range value.
    private @Nullable LayoutNode node;

    /// Creates a scrollbar.
    ///
    /// @param label the accessible name
    /// @param minimum the inclusive minimum
    /// @param maximum the inclusive maximum
    /// @param step the positive step
    /// @param value the initial value
    public Scrollbar(String label, float minimum, float maximum, float step, float value) {
        this.label = Objects.requireNonNull(label, "label");
        if (!Float.isFinite(minimum) || !Float.isFinite(maximum) || !Float.isFinite(step) || !Float.isFinite(value)) {
            throw new IllegalArgumentException("Scrollbar extents must be finite");
        }
        if (maximum < minimum) {
            throw new IllegalArgumentException("Scrollbar maximum must be at least the minimum");
        }
        if (step <= 0.0f) {
            throw new IllegalArgumentException("Scrollbar step must be positive");
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

    /// Builds the scrollbar leaf.
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
                SemanticsRole.SCROLLBAR,
                label,
                Set.of(SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                null,
                this::adjust
        );
        created.setRangeValue(value);
        created.setScroll(scrollSnapshot());
        this.node = created;
        return created;
    }

    /// Applies one signed step.
    ///
    /// @param delta `1` or `-1`
    private void adjust(int delta) {
        value = clamp(value + delta * step);
        if (node != null) {
            node.setRangeValue(value);
            node.setScroll(scrollSnapshot());
        }
    }

    /// Builds the vertical-scroll snapshot for the current value.
    ///
    /// @return the snapshot
    public SemanticsScroll scrollSnapshot() {
        float span = maximum - minimum;
        if (span <= 0.0f) {
            return new SemanticsScroll(0.0, 100.0, false);
        }
        double percent = 100.0 * (value - minimum) / span;
        return new SemanticsScroll(percent, 10.0, true);
    }

    /// Clamps a candidate into the published range.
    private float clamp(float candidate) {
        return Math.min(maximum, Math.max(minimum, candidate));
    }
}
