package org.glavo.himari.controls;

import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies keyed grid virtualization, overscan, and row-preserving inserts.
@NotNullByDefault
final class LazyGridTest {
    /// Materializes only a window of rows plus overscan.
    @Test
    void materializesViewportWithOverscan() {
        LazyGrid grid = new LazyGrid(20, 2, 2, 1);
        grid.scrollTo(4);
        assertEquals(4, grid.firstVisible());
        assertEquals(3, grid.materializedFirst());
        assertEquals(7, grid.materializedLast());
        LayoutTree tree = new LayoutTree();
        LayoutNode root = grid.create(new LayoutFactory(tree), "grid");
        assertEquals(4, root.children().size());
        assertEquals("Cell 6", root.children().getFirst().children().getFirst().label());
        assertEquals("Cell 13", root.children().getLast().children().getLast().label());
        assertFalse(grid.unmountedLabels().contains("Cell 8"));
        assertTrue(grid.unmountedLabels().contains("Cell 0"));
        assertTrue(grid.unmountedLabels().contains("Cell 19"));
    }

    /// Scrolls by row and pages without leaving the valid window.
    @Test
    void scrollsAndPagesRows() {
        LazyGrid grid = new LazyGrid(20, 2, 2);
        grid.scrollTo(8);
        assertEquals(8, grid.firstVisible());
        grid.page(-1);
        assertEquals(6, grid.firstVisible());
        grid.page(1);
        assertEquals(8, grid.firstVisible());
        grid.scrollTo(100);
        assertEquals(8, grid.firstVisible());
    }

    /// Keeps the first visible item's row after an insert above it.
    @Test
    void insertPreservesFirstVisibleItem() {
        LazyGrid grid = new LazyGrid(8, 2, 2);
        grid.scrollTo(2);
        assertEquals(2, grid.firstVisible());
        grid.insert(0);
        assertEquals(2, grid.firstVisible());
        assertEquals(9, grid.itemCount());
        grid.insert(0);
        assertEquals(3, grid.firstVisible());
        grid.remove(0);
        assertEquals(2, grid.firstVisible());
        assertEquals(9, grid.itemCount());
    }

    /// Builds GRID/ROW/CELL semantics through the shipped factory.
    @Test
    void createExposesGridSemantics() {
        LayoutTree tree = new LayoutTree();
        LazyGrid grid = new LazyGrid(6, 3, 2);
        tree.setRoot(grid.create(new LayoutFactory(tree), "grid"));
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        assertEquals(SemanticsRole.GRID, tree.root().role());
        assertNotNull(tree.root().grid());
        assertEquals(2, tree.root().grid().rowCount());
        assertEquals(3, tree.root().grid().columnCount());
        assertTrue(tree.semantics().nodes().stream().anyMatch(node -> node.role() == SemanticsRole.GRID));
        assertTrue(tree.semantics().nodes().stream().anyMatch(node -> node.role() == SemanticsRole.TABLE_ROW));
        assertTrue(tree.semantics().nodes().stream().anyMatch(node -> node.role() == SemanticsRole.TABLE_CELL));
        assertEquals(2, tree.root().children().size());
        assertEquals(3, tree.root().children().getFirst().children().size());
    }
}
