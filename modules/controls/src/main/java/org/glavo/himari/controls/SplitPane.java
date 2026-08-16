package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled two-pane split with a stored first-pane fraction.
@NotNullByDefault
public final class SplitPane {
    /// Total width used when materializing the panes.
    private static final float TOTAL_WIDTH = 200.0f;

    /// Pane height.
    private static final float PANE_HEIGHT = 40.0f;

    /// First-pane share in `(0, 1)`.
    private float fraction;

    /// Creates a split.
    ///
    /// @param fraction the first-pane share
    public SplitPane(float fraction) {
        this.fraction = clamp(fraction);
    }

    /// Returns the first-pane share.
    ///
    /// @return the fraction
    public float fraction() {
        return fraction;
    }

    /// Replaces the first-pane share.
    ///
    /// @param fraction the next share
    public void setFraction(float fraction) {
        this.fraction = clamp(fraction);
    }

    /// Builds a row of first pane, divider, and second pane.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the row
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        float firstWidth = TOTAL_WIDTH * fraction;
        float secondWidth = TOTAL_WIDTH - firstWidth;
        LayoutNode first = factory.leaf(
                name + "-first",
                new Size(firstWidth, PANE_HEIGHT),
                List.of(),
                false,
                SemanticsRole.NONE,
                "First",
                Set.of(),
                null
        );
        LayoutNode divider = factory.leaf(
                name + "-divider",
                new Size(4.0f, PANE_HEIGHT),
                List.of(),
                false,
                SemanticsRole.NONE,
                "Divider",
                Set.of(),
                null
        );
        LayoutNode second = factory.leaf(
                name + "-second",
                new Size(secondWidth, PANE_HEIGHT),
                List.of(),
                false,
                SemanticsRole.NONE,
                "Second",
                Set.of(),
                null
        );
        return factory.row(
                name,
                Alignment.START,
                List.of(new LayoutModifier.Padding(0.0f)),
                SemanticsRole.SPLIT_PANE,
                name,
                first,
                divider,
                second
        );
    }

    /// Clamps a share into `(0, 1)`.
    private static float clamp(float fraction) {
        if (!Float.isFinite(fraction)) {
            throw new IllegalArgumentException("Split fraction must be finite");
        }
        if (fraction <= 0.0f || fraction >= 1.0f) {
            throw new IllegalArgumentException("Split fraction must be in (0, 1)");
        }
        return fraction;
    }
}
