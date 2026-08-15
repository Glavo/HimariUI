package org.glavo.himari.layout.bootstrap;

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

/// Creates a private, non-stable activatable leaf for architecture samples.
@NotNullByDefault
public final class BootstrapButton {
    /// Approximate glyph cell size used before the text pipeline exists.
    private static final float CELL_WIDTH = 8.0f;

    /// Approximate control height used before the text pipeline exists.
    private static final float HEIGHT = 24.0f;

    /// Prevents instantiation.
    private BootstrapButton() {
    }

    /// Creates a focusable button that exposes [SemanticsAction#ACTIVATE].
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @param label the visible and accessible name
    /// @param onActivate the activation callback
    /// @return the leaf
    public static LayoutNode create(LayoutFactory factory, String name, String label, Runnable onActivate) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(onActivate, "onActivate");
        return factory.leaf(
                name,
                new Size(Math.max(48.0f, label.length() * CELL_WIDTH + 16.0f), HEIGHT),
                List.of(new LayoutModifier.Padding(4.0f)),
                true,
                SemanticsRole.BUTTON,
                label,
                Set.of(SemanticsAction.ACTIVATE),
                onActivate
        );
    }
}
