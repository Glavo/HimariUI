package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
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

/// Creates an unstyled two-child horizontal flex row.
@NotNullByDefault
public final class Flex {
    /// Shared child height.
    private static final float HEIGHT = 16.0f;

    /// First child's grow weight.
    private final float firstGrow;

    /// Second child's grow weight.
    private final float secondGrow;

    /// Whether the flex ignores grow changes.
    private boolean disabled;

    /// Mounted flex row that receives the published disabled state.
    private @Nullable LayoutNode node;

    /// Creates a flex row.
    ///
    /// @param firstGrow the nonnegative first-child weight
    /// @param secondGrow the nonnegative second-child weight
    public Flex(float firstGrow, float secondGrow) {
        if (!Float.isFinite(firstGrow) || firstGrow < 0.0f || !Float.isFinite(secondGrow) || secondGrow < 0.0f) {
            throw new IllegalArgumentException("Flex grow must be finite and nonnegative");
        }
        this.firstGrow = firstGrow;
        this.secondGrow = secondGrow;
    }

    /// Returns the first-child grow weight.
    ///
    /// @return the weight
    public float firstGrow() {
        return firstGrow;
    }

    /// Returns the second-child grow weight.
    ///
    /// @return the weight
    public float secondGrow() {
        return secondGrow;
    }

    /// Returns whether the flex is disabled.
    ///
    /// @return whether the flex is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to the mounted row when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (node != null) {
            node.setDisabled(disabled);
        }
    }

    /// Builds the flex row.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the row
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        LayoutNode first = factory.leaf(
                name + "-first",
                new Size(16.0f, HEIGHT),
                List.of(new LayoutModifier.FlexGrow(firstGrow)),
                false,
                SemanticsRole.NONE,
                "First",
                Set.of(),
                null
        );
        LayoutNode second = factory.leaf(
                name + "-second",
                new Size(16.0f, HEIGHT),
                List.of(new LayoutModifier.FlexGrow(secondGrow)),
                false,
                SemanticsRole.NONE,
                "Second",
                Set.of(),
                null
        );
        LayoutNode row = factory.flex(
                name,
                Alignment.START,
                List.of(new LayoutModifier.Padding(0.0f)),
                first,
                second
        );
        row.setDisabled(disabled);
        this.node = row;
        return row;
    }
}
