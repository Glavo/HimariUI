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

/// Creates an unstyled non-interactive loading skeleton.
@NotNullByDefault
public final class Skeleton {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 16.0f);

    /// Whether the skeleton is currently loading.
    private boolean loading = true;

    /// Whether the skeleton is disabled.
    private boolean disabled;

    /// Mounted leaf that receives the published loading state.
    private @Nullable LayoutNode node;

    /// Creates a loading skeleton.
    public Skeleton() {
    }

    /// Returns whether the skeleton is loading.
    ///
    /// @return whether it is loading
    public boolean loading() {
        return loading;
    }

    /// Returns whether the skeleton is disabled.
    ///
    /// @return whether the skeleton is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the loading state and publishes it when mounted.
    ///
    /// @param loading the next state
    public void setLoading(boolean loading) {
        this.loading = loading;
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the skeleton leaf.
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
                false,
                SemanticsRole.SKELETON,
                "Loading",
                Set.of(),
                null
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes loading and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel("Loading");
        node.setDisabled(disabled);
        node.setItemStatus(loading ? "loading" : "ready");
    }
}
