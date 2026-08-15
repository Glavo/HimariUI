package org.glavo.himari.layout.bootstrap;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates a private, non-stable text leaf for architecture samples.
@NotNullByDefault
public final class BootstrapLabel {
    /// Approximate glyph cell size used before the text pipeline exists.
    private static final float CELL_WIDTH = 8.0f;

    /// Approximate line height used before the text pipeline exists.
    private static final float LINE_HEIGHT = 16.0f;

    /// Prevents instantiation.
    private BootstrapLabel() {
    }

    /// Creates a text leaf sized from the label string.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @param text the visible text
    /// @return the leaf
    public static LayoutNode create(LayoutFactory factory, String name, String text) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(text, "text");
        return factory.leaf(
                name,
                new Size(Math.max(CELL_WIDTH, text.length() * CELL_WIDTH), LINE_HEIGHT),
                List.of(),
                false,
                SemanticsRole.TEXT,
                text,
                Set.of(),
                null
        );
    }
}
