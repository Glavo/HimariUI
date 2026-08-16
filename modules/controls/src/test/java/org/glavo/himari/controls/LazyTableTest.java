package org.glavo.himari.controls;

import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies keyed table virtualization, overscan, height correction, and anchors.
@NotNullByDefault
final class LazyTableTest {
    /// Materializes the viewport plus one overscan row on each side.
    @Test
    void materializesViewportWithOverscan() {
        LazyTable table = new LazyTable(2, 1);
        for (int index = 0; index < 8; index++) {
            table.addRow("r" + index, 20.0f);
        }
        table.setViewport(40.0f, 40.0f);
        assertEquals(2, table.firstVisible());
        assertEquals(1, table.firstMaterialized());
        assertEquals(5, table.lastMaterialized());
        assertEquals("r2", table.firstMaterializedKey());
        assertEquals(List.of("r1", "r2", "r3", "r4"), table.materializedKeys());
    }

    /// Keeps the first materialized key after an insert above it.
    @Test
    void insertPreservesAnchorKey() {
        LazyTable table = new LazyTable(1, 0);
        table.addRow("a", 20.0f);
        table.addRow("b", 20.0f);
        table.addRow("c", 20.0f);
        table.setViewport(20.0f, 20.0f);
        assertEquals("b", table.firstMaterializedKey());
        table.insertRow(0, "z", 20.0f);
        assertEquals("b", table.firstMaterializedKey());
        assertEquals("z", table.keyAt(0));
    }

    /// Recalculates the window after a measured height change without losing the anchor.
    @Test
    void heightCorrectionKeepsAnchor() {
        LazyTable table = new LazyTable(1, 0);
        table.addRow("a", 20.0f);
        table.addRow("b", 20.0f);
        table.addRow("c", 20.0f);
        table.setViewport(20.0f, 20.0f);
        table.correctHeight(0, 40.0f);
        assertEquals("b", table.firstMaterializedKey());
        assertEquals(40.0f, table.heightAt(0));
    }

    /// Builds TABLE/ROW/CELL semantics through the shipped factory.
    @Test
    void createExposesTableSemantics() {
        LayoutTree tree = new LayoutTree();
        LazyTable table = new LazyTable(2, 0);
        table.addRow("alpha", 16.0f);
        table.addRow("beta", 16.0f);
        table.setViewport(0.0f, 16.0f);
        tree.setRoot(table.create(new LayoutFactory(tree), "table"));
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        assertEquals(SemanticsRole.TABLE, tree.root().role());
        assertNotNull(tree.root().grid());
        assertEquals(2, tree.root().grid().rowCount());
        assertEquals(2, tree.root().grid().columnCount());
        assertTrue(tree.semantics().nodes().stream().anyMatch(node -> node.role() == SemanticsRole.TABLE));
        assertTrue(tree.semantics().nodes().stream().anyMatch(node -> node.role() == SemanticsRole.TABLE_ROW));
        assertTrue(tree.semantics().nodes().stream().anyMatch(node -> node.role() == SemanticsRole.TABLE_CELL));
        assertTrue(tree.semantics().nodes().stream().anyMatch(node ->
                node.role() == SemanticsRole.TABLE_CELL
                        && node.gridItem() != null
                        && node.gridItem().row() == 0
                        && node.gridItem().column() == 1));
        assertEquals(List.of("alpha"), table.materializedKeys());
    }
}
