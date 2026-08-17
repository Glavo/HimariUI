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

/// Creates an unstyled image leaf identified by a source name.
@NotNullByDefault
public final class Image {
    /// Default control size.
    private static final Size SIZE = new Size(32.0f, 32.0f);

    /// Source name used as the accessible label.
    private String source;

    /// Whether the image is disabled.
    private boolean disabled;

    /// Mounted leaf that receives the published disabled state.
    private @Nullable LayoutNode node;

    /// Creates an image.
    ///
    /// @param source the non-empty source name
    public Image(String source) {
        this.source = requireSource(source);
    }

    /// Returns the source name.
    ///
    /// @return the name
    public String source() {
        return source;
    }

    /// Replaces the source name used by the next [#create(LayoutFactory, String)].
    ///
    /// @param source the non-empty source name
    public void setSource(String source) {
        this.source = requireSource(source);
    }

    /// Builds the image leaf.
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
                SemanticsRole.IMAGE,
                source,
                Set.of(),
                null
        );
        created.setDisabled(disabled);
        this.node = created;
        return created;
    }

    /// Returns whether the image is disabled.
    ///
    /// @return whether the image is disabled
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

    /// Rejects a blank source name.
    private static String requireSource(String source) {
        Objects.requireNonNull(source, "source");
        if (source.isEmpty()) {
            throw new IllegalArgumentException("Image source must not be empty");
        }
        return source;
    }
}
