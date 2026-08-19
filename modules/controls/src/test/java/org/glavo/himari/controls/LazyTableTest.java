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
        table.setColumnWidth(1, 120.0f);
        assertEquals(80.0f, table.columnWidth(0));
        assertEquals(120.0f, table.columnWidth(1));
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
        table.setDisabled(true);
        table.scrollTo(1);
        table.insertRow(0, "blocked", 16.0f);
        assertEquals("alpha", table.firstMaterializedKey());
        assertTrue(table.disabled());
        assertTrue(tree.root().disabled());
        table.setDisabled(false);
        assertTrue(tree.semantics().nodes().stream().anyMatch(node ->
                node.role() == SemanticsRole.TABLE_CELL
                        && node.gridItem() != null
                        && node.gridItem().row() == 0
                        && node.gridItem().column() == 1));
        assertEquals(List.of("alpha"), table.materializedKeys());
    }

    /// Scrolls a 100,000-row table by index and page through the shipped viewport.
    @Test
    void scrollToAndPageVirtualizeOneHundredThousandRows() {
        LazyTable table = new LazyTable(2, 0);
        for (int index = 0; index < 100_000; index++) {
            table.addRow("r" + index, 16.0f);
        }
        table.setViewport(0.0f, 64.0f);
        assertEquals(0, table.firstVisible());
        table.scrollTo(99_990);
        assertEquals(99_990, table.firstVisible());
        assertEquals(99_990 * 16.0f, table.viewportOffset(), 0.01f);
        assertEquals(List.of("r99990", "r99991", "r99992", "r99993"), table.materializedKeys());
        table.page(-1);
        assertEquals(99_986, table.firstVisible());
        table.page(1);
        assertEquals(99_990, table.firstVisible());
        assertEquals(100_000, table.logicalLabels().size());
        assertEquals("r0", table.logicalLabels().getFirst());
        assertEquals("r99999", table.logicalLabels().getLast());
        assertTrue(table.unmountedLabels().contains("r0"));
        assertTrue(table.unmountedLabels().contains("r99999"));
        assertEquals(100_000 - table.materializedKeys().size(), table.unmountedLabels().size());
    }

    /// Publishes column/row headers and a scroll snapshot through [`LazyTable#create`].
    @Test
    void createPublishesHeadersAndScroll() {
        LayoutTree tree = new LayoutTree();
        LazyTable table = new LazyTable(2, 0);
        table.addRow("alpha", 16.0f);
        table.addRow("beta", 16.0f);
        table.addRow("gamma", 16.0f);
        table.addRow("delta", 16.0f);
        table.setColumnHeader(0, "Name");
        table.setColumnHeader(1, "Value");
        table.setRowHeader(0, "A");
        table.setViewport(0.0f, 16.0f);
        tree.setRoot(table.create(new LayoutFactory(tree), "people"));
        tree.measure(Constraints.loose(400.0f, 400.0f));
        tree.place();
        assertEquals("Name", table.columnHeader(0));
        assertEquals("A", table.rowHeader(0));
        assertEquals(2, tree.root().grid().columnHeaders().length);
        assertEquals("Name", tree.root().grid().columnHeaders()[0]);
        assertEquals("Value", tree.root().grid().columnHeaders()[1]);
        assertEquals("A", tree.root().grid().rowHeaders()[0]);
        assertNotNull(tree.root().scroll());
        assertTrue(tree.root().scroll().verticallyScrollable());
        assertEquals(0.0, tree.root().scroll().verticalPercent(), 0.1);
        assertEquals(25.0, tree.root().scroll().verticalViewSize(), 0.1);
        assertEquals(0.0, table.scrollSnapshot().verticalPercent(), 0.1);
        assertTrue(tree.semantics().nodes().stream().anyMatch(node ->
                node.role() == SemanticsRole.TABLE_COLUMN_HEADER && "Name".equals(node.label())));
        assertTrue(tree.semantics().nodes().stream().anyMatch(node ->
                node.role() == SemanticsRole.TABLE_ROW_HEADER && "A".equals(node.label())));
        assertTrue(tree.semantics().nodes().stream().anyMatch(node ->
                node.gridItem() != null
                        && node.gridItem().column() == 1
                        && "Value".equals(node.gridItem().columnHeader())));
    }
}
