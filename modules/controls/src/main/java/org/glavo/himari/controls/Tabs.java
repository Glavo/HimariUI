package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled tab strip and one visible panel.
@NotNullByDefault
public final class Tabs {
    /// Tab titles in document order.
    private final List<String> titles;

    /// Selected tab index.
    private int selected;

    /// Whether the strip ignores selection.
    private boolean disabled;

    /// Mounted tab leaves that receive the published disabled state.
    private @Nullable List<LayoutNode> nodes;

    /// Creates a tab set.
    ///
    /// @param titles the non-empty titles
    public Tabs(List<String> titles) {
        this.titles = List.copyOf(titles);
        if (this.titles.isEmpty()) {
            throw new IllegalArgumentException("Tabs must contain at least one title");
        }
        for (String title : this.titles) {
            Objects.requireNonNull(title, "title");
            if (title.isEmpty()) {
                throw new IllegalArgumentException("Tab title must not be empty");
            }
        }
    }

    /// Returns the titles.
    ///
    /// @return the titles
    public @Unmodifiable List<String> titles() {
        return titles;
    }

    /// Returns the selected index.
    ///
    /// @return the index
    public int selected() {
        return selected;
    }

    /// Selects `index`.
    ///
    /// @param index the tab index
    public void select(int index) {
        if (index < 0 || index >= titles.size()) {
            throw new IllegalArgumentException("Tab index is out of range");
        }
        if (disabled) {
            return;
        }
        selected = index;
        if (nodes != null) {
            for (int tab = 0; tab < nodes.size(); tab++) {
                nodes.get(tab).setSelected(tab == selected);
            }
        }
    }

    /// Returns whether the strip is disabled.
    ///
    /// @return whether the strip is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to mounted tab leaves when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (nodes != null) {
            for (LayoutNode tab : nodes) {
                tab.setDisabled(disabled);
            }
        }
    }

    /// Builds the tab list and the selected panel.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the column
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        ArrayList<LayoutNode> tabs = new ArrayList<>();
        for (int index = 0; index < titles.size(); index++) {
            int target = index;
            LayoutNode tab = factory.leaf(
                    name + "-tab-" + index,
                    new Size(64.0f, 24.0f),
                    List.of(),
                    true,
                    SemanticsRole.TAB,
                    titles.get(index),
                    Set.of(SemanticsAction.ACTIVATE),
                    () -> select(target)
            );
            tab.setSelected(index == selected);
            tab.setDisabled(disabled);
            tabs.add(tab);
        }
        this.nodes = List.copyOf(tabs);
        LayoutNode strip = factory.row(
                name + "-strip",
                Alignment.START,
                List.of(),
                SemanticsRole.TAB_LIST,
                name,
                tabs.toArray(LayoutNode[]::new)
        );
        LayoutNode panel = factory.leaf(
                name + "-panel",
                new Size(160.0f, 40.0f),
                List.of(),
                false,
                SemanticsRole.TAB_PANEL,
                titles.get(selected),
                Set.of(),
                null
        );
        return factory.column(
                name,
                Alignment.START,
                List.of(new LayoutModifier.Padding(0.0f)),
                strip,
                panel
        );
    }
}
