package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled spacer leaf that occupies space without a control role.
@NotNullByDefault
public final class Spacer {
    /// Intrinsic size.
    private final Size size;

    /// Creates a spacer.
    ///
    /// @param size the positive intrinsic size
    public Spacer(Size size) {
        this.size = Objects.requireNonNull(size, "size");
        if (size.width() <= 0.0f || size.height() <= 0.0f) {
            throw new IllegalArgumentException("Spacer size must be positive");
        }
    }

    /// Returns the intrinsic size.
    ///
    /// @return the size
    public Size size() {
        return size;
    }

    /// Builds the spacer leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        return factory.leaf(
                name,
                size,
                List.of(),
                false,
                SemanticsRole.NONE,
                "spacer",
                Set.of(),
                null
        );
    }
}
