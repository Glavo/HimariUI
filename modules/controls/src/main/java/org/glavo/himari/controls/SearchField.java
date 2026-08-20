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

/// Creates an unstyled single-line search field.
@NotNullByDefault
public final class SearchField {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 24.0f);

    /// Current query text.
    private String query;

    /// Last submitted query, or empty when none has been submitted.
    private String submitted = "";

    /// Whether the control ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published query.
    private @Nullable LayoutNode node;

    /// Creates an empty search field.
    public SearchField() {
        this("");
    }

    /// Creates a search field.
    ///
    /// @param query the initial query
    public SearchField(String query) {
        this.query = Objects.requireNonNull(query, "query");
    }

    /// Returns the current query.
    ///
    /// @return the query
    public String query() {
        return query;
    }

    /// Returns the last submitted query, or empty when none has been submitted.
    ///
    /// @return the submitted query
    public String submitted() {
        return submitted;
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Replaces the query and publishes it when mounted.
    ///
    /// @param query the next query
    public void setQuery(String query) {
        this.query = Objects.requireNonNull(query, "query");
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Submits the current query.
    public void submit() {
        if (disabled) {
            return;
        }
        submitted = query;
        publish();
    }

    /// Builds the search-field leaf.
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
                SemanticsRole.SEARCH_BOX,
                query.isEmpty() ? "Search" : query,
                Set.of(SemanticsAction.ACTIVATE),
                this::submit
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes the query and disabled state onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(query.isEmpty() ? "Search" : query);
        node.setDisabled(disabled);
    }
}
