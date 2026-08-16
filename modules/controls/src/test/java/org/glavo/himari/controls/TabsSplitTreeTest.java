package org.glavo.himari.controls;

import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies Tabs, SplitPane, and Tree through the shipped factories.
@NotNullByDefault
final class TabsSplitTreeTest {
    /// Selects the second tab and publishes TAB semantics.
    @Test
    void selectsTabAndExposesPanel() {
        Tabs tabs = new Tabs(List.of("One", "Two"));
        assertEquals(0, tabs.selected());
        tabs.select(1);
        assertEquals(1, tabs.selected());
        LayoutTree tree = new LayoutTree();
        tree.setRoot(tabs.create(new LayoutFactory(tree), "tabs"));
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        assertTrue(tree.semantics().nodes().stream().anyMatch(node -> node.role() == SemanticsRole.TAB_LIST));
        assertTrue(tree.semantics().nodes().stream().anyMatch(node ->
                node.role() == SemanticsRole.TAB_PANEL && node.label().equals("Two")));
    }

    /// Stores a first-pane fraction used by the shipped split row.
    @Test
    void splitPaneKeepsFraction() {
        SplitPane split = new SplitPane(0.25f);
        assertEquals(0.25f, split.fraction());
        split.setFraction(0.75f);
        assertEquals(0.75f, split.fraction());
        LayoutTree tree = new LayoutTree();
        tree.setRoot(split.create(new LayoutFactory(tree), "split"));
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        assertEquals(SemanticsRole.SPLIT_PANE, tree.root().role());
    }

    /// Collapses an expandable root and hides the child.
    @Test
    void treeToggleHidesDescendants() {
        Tree outline = new Tree(List.of(
                new Tree.Item("root", "Root", 0, true),
                new Tree.Item("child", "Child", 1, false)
        ));
        assertEquals(List.of(0, 1), outline.visibleIndices());
        outline.toggle(0);
        assertFalse(outline.isExpanded(0));
        assertEquals(List.of(0), outline.visibleIndices());
        outline.toggle(0);
        outline.select(1);
        assertEquals(1, outline.selected());
        LayoutTree tree = new LayoutTree();
        tree.setRoot(outline.create(new LayoutFactory(tree), "tree"));
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        assertEquals(SemanticsRole.TREE, tree.root().role());
        assertTrue(tree.semantics().nodes().stream().anyMatch(node -> node.role() == SemanticsRole.TREE_ITEM));
    }
}
