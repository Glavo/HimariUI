package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled determinate progress indicator.
@NotNullByDefault
public final class Progress {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 16.0f);

    /// Accessible name.
    private final String label;

    /// Inclusive minimum.
    private final float minimum;

    /// Inclusive maximum.
    private final float maximum;

    /// Current value.
    private float value;

    /// Mounted leaf that receives the published range value.
    private @Nullable LayoutNode node;

    /// Creates a progress indicator.
    ///
    /// @param label the accessible name
    /// @param minimum the inclusive minimum
    /// @param maximum the inclusive maximum
    /// @param value the initial value
    public Progress(String label, float minimum, float maximum, float value) {
        this.label = Objects.requireNonNull(label, "label");
        if (!Float.isFinite(minimum) || !Float.isFinite(maximum) || !Float.isFinite(value)) {
            throw new IllegalArgumentException("Progress extents must be finite");
        }
        if (maximum < minimum) {
            throw new IllegalArgumentException("Progress maximum must be at least the minimum");
        }
        this.minimum = minimum;
        this.maximum = maximum;
        this.value = clamp(value);
    }

    /// Returns the current value.
    ///
    /// @return the value
    public float value() {
        return value;
    }

    /// Replaces the current value.
    ///
    /// @param value the next value
    public void setValue(float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Progress value must be finite");
        }
        this.value = clamp(value);
        if (node != null) {
            node.setRangeValue(this.value);
        }
    }

    /// Builds the progress leaf.
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
                false,
                SemanticsRole.PROGRESS,
                label,
                Set.of(),
                null
        );
        created.setRangeValue(value);
        this.node = created;
        return created;
    }

    /// Clamps a candidate into the published range.
    private float clamp(float candidate) {
        return Math.min(maximum, Math.max(minimum, candidate));
    }
}
