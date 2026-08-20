package org.glavo.himari.layout;

import org.glavo.himari.layout.bootstrap.BootstrapCounterPane;
import org.glavo.himari.layout.hit.HitTester;
import org.glavo.himari.layout.hit.SpatialIndex;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsTextRange;
import org.glavo.himari.layout.semantics.TextDirection;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies measurement, placement, hit testing, focus, and bootstrap activation.
@NotNullByDefault
final class LayoutTreeTest {
    /// Verifies single-measure and placement-only invalidation for a column.
    @Test
    void measuresOnceAndPlacesChildren() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "box",
                new Size(10.0f, 20.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "box",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.column("root", Alignment.START, java.util.List.of(), leaf));
        Size size = tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(10.0f, size.width());
        assertEquals(20.0f, size.height());
        assertEquals(20.0f, leaf.baseline());
        assertEquals(20.0f, leaf.alignmentLines().baseline());
        assertEquals(5.0f, leaf.alignmentLines().centerX());
        assertEquals(10.0f, leaf.alignmentLines().centerY());
        assertEquals(5.0f, tree.root().alignmentLines().centerX());
        assertEquals(10.0f, tree.root().alignmentLines().centerY());
        tree.place();
        assertEquals(0.0f, leaf.origin().x());
        assertEquals(0.0f, leaf.origin().y());
        assertThrows(IllegalStateException.class, () -> tree.root().measure(Constraints.loose(100.0f, 100.0f)));
    }

    /// Aligns a short leaf to a taller column's published baseline on a row.
    @Test
    void rowBaselineAlignsToFirstChildBaseline() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode stacked = factory.leaf(
                "stacked",
                new Size(10.0f, 12.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "stacked",
                java.util.Set.of(),
                null
        );
        LayoutNode trailing = factory.leaf(
                "trailing",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "trailing",
                java.util.Set.of(),
                null
        );
        LayoutNode column = factory.column("column", Alignment.START, java.util.List.of(), stacked, trailing);
        LayoutNode shortLeaf = factory.leaf(
                "short",
                new Size(8.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "short",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.row("row", Alignment.BASELINE, java.util.List.of(), column, shortLeaf));
        Size size = tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(18.0f, size.width());
        assertEquals(20.0f, size.height());
        assertEquals(12.0f, column.baseline());
        assertEquals(8.0f, shortLeaf.baseline());
        tree.place();
        assertEquals(0.0f, column.origin().y());
        assertEquals(4.0f, shortLeaf.origin().y());
        assertEquals(10.0f, shortLeaf.origin().x());
    }

    /// Aligns a short leaf to a taller column's published baseline on a flex row.
    @Test
    void flexBaselineAlignsToFirstChildBaseline() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode stacked = factory.leaf(
                "stacked",
                new Size(10.0f, 12.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "stacked",
                java.util.Set.of(),
                null
        );
        LayoutNode trailing = factory.leaf(
                "trailing",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "trailing",
                java.util.Set.of(),
                null
        );
        LayoutNode column = factory.column("column", Alignment.START, java.util.List.of(), stacked, trailing);
        LayoutNode shortLeaf = factory.leaf(
                "short",
                new Size(8.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "short",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.flex("flex", Alignment.BASELINE, java.util.List.of(), column, shortLeaf));
        Size size = tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(18.0f, size.width());
        assertEquals(20.0f, size.height());
        tree.place();
        assertEquals(0.0f, column.origin().y());
        assertEquals(4.0f, shortLeaf.origin().y());
    }

    /// Aligns a short leaf to a taller column's published baseline on a wrapping flow line.
    @Test
    void flowBaselineAlignsToFirstChildBaseline() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode stacked = factory.leaf(
                "stacked",
                new Size(10.0f, 12.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "stacked",
                java.util.Set.of(),
                null
        );
        LayoutNode trailing = factory.leaf(
                "trailing",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "trailing",
                java.util.Set.of(),
                null
        );
        LayoutNode column = factory.column("column", Alignment.START, java.util.List.of(), stacked, trailing);
        LayoutNode shortLeaf = factory.leaf(
                "short",
                new Size(8.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "short",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.flow("flow", Alignment.BASELINE, java.util.List.of(), column, shortLeaf));
        Size size = tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(18.0f, size.width());
        assertEquals(20.0f, size.height());
        tree.place();
        assertEquals(0.0f, column.origin().y());
        assertEquals(4.0f, shortLeaf.origin().y());
    }

    /// Aligns a short leaf to a taller column's published baseline inside a grid cell.
    @Test
    void gridBaselineAlignsToFirstChildBaseline() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode stacked = factory.leaf(
                "stacked",
                new Size(10.0f, 12.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "stacked",
                java.util.Set.of(),
                null
        );
        LayoutNode trailing = factory.leaf(
                "trailing",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "trailing",
                java.util.Set.of(),
                null
        );
        LayoutNode column = factory.column("column", Alignment.START, java.util.List.of(), stacked, trailing);
        LayoutNode shortLeaf = factory.leaf(
                "short",
                new Size(8.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "short",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.grid("grid", 2, Alignment.BASELINE, java.util.List.of(), column, shortLeaf));
        Size size = tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(18.0f, size.width());
        assertEquals(20.0f, size.height());
        assertEquals(12.0f, column.baseline());
        assertEquals(8.0f, shortLeaf.baseline());
        tree.place();
        assertEquals(0.0f, column.origin().y());
        assertEquals(4.0f, shortLeaf.origin().y());
        assertEquals(10.0f, shortLeaf.origin().x());
    }

    /// Centers a short leaf inside a taller, wider grid cell.
    @Test
    void gridCenterAlignsWithinCell() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode shortLeaf = factory.leaf(
                "short",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "short",
                java.util.Set.of(),
                null
        );
        LayoutNode tall = factory.leaf(
                "tall",
                new Size(20.0f, 16.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "tall",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.grid("grid", 2, Alignment.CENTER, java.util.List.of(), shortLeaf, tall));
        Size size = tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(30.0f, size.width());
        assertEquals(16.0f, size.height());
        tree.place();
        assertEquals(0.0f, shortLeaf.origin().x());
        assertEquals(4.0f, shortLeaf.origin().y());
        assertEquals(10.0f, tall.origin().x());
        assertEquals(0.0f, tall.origin().y());
    }

    /// Distributes leftover width to a growing flex child after an inflexible sibling.
    @Test
    void flexGrowsAfterInflexibleSibling() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode fixed = factory.leaf(
                "fixed",
                new Size(20.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "fixed",
                java.util.Set.of(),
                null
        );
        LayoutNode growing = factory.leaf(
                "grow",
                new Size(10.0f, 10.0f),
                java.util.List.of(new LayoutModifier.FlexGrow(1.0f)),
                false,
                SemanticsRole.NONE,
                "grow",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.flex("flex", Alignment.START, java.util.List.of(), fixed, growing));
        Size size = tree.measure(Constraints.loose(100.0f, 40.0f));
        assertEquals(100.0f, size.width());
        assertEquals(10.0f, size.height());
        tree.place();
        assertEquals(20.0f, fixed.size().width());
        assertEquals(80.0f, growing.size().width());
        assertEquals(20.0f, growing.offset().x());
    }

    /// Clamps an oversized leaf through MaxSize.
    @Test
    void maxSizeClampsOversizedLeaf() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "box",
                new Size(80.0f, 50.0f),
                java.util.List.of(new LayoutModifier.MaxSize(30.0f, 20.0f)),
                false,
                SemanticsRole.NONE,
                "box",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.box("root", java.util.List.of(), leaf));
        Size size = tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(30.0f, size.width());
        assertEquals(20.0f, size.height());
        tree.place();
        assertEquals(30.0f, leaf.size().width());
        assertEquals(20.0f, leaf.size().height());
    }

    /// Publishes the leaf intrinsic size independently of constrained measure.
    @Test
    void intrinsicSizeSurvivesConstrainedMeasure() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "box",
                new Size(10.0f, 20.0f),
                java.util.List.of(new LayoutModifier.MaxSize(6.0f, 8.0f)),
                false,
                SemanticsRole.NONE,
                "box",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.box("root", java.util.List.of(), leaf));
        tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(10.0f, leaf.intrinsicSize().width());
        assertEquals(20.0f, leaf.intrinsicSize().height());
        assertEquals(6.0f, leaf.size().width());
        assertEquals(8.0f, leaf.size().height());
    }

    /// Rebuilds a reverse-z spatial index that agrees with tree hit testing.
    @Test
    void spatialIndexHitsFrontMostLeaf() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode top = factory.leaf(
                "top",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "top",
                java.util.Set.of(),
                null
        );
        LayoutNode bottom = factory.leaf(
                "bottom",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "bottom",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.column("root", Alignment.START, java.util.List.of(), top, bottom));
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        org.glavo.himari.layout.hit.SpatialIndex index = tree.spatialIndex();
        assertNotNull(index);
        assertEquals(top, index.hit(5.0f, 4.0f));
        assertEquals(bottom, index.hit(5.0f, 12.0f));
        assertEquals(
                org.glavo.himari.layout.hit.HitTester.hit(tree.root(), 5.0f, 4.0f),
                index.hit(5.0f, 4.0f)
        );
        assertEquals(
                org.glavo.himari.layout.hit.HitTester.hit(tree.root(), 5.0f, 12.0f),
                index.hit(5.0f, 12.0f)
        );
    }

    /// Shears a leaf horizontally and publishes the expanded AABB.
    @Test
    void skewExpandsHorizontalAabb() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "box",
                new Size(10.0f, 8.0f),
                java.util.List.of(new LayoutModifier.Skew(0.5f, 0.0f)),
                false,
                SemanticsRole.NONE,
                "box",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.box("root", java.util.List.of(), leaf));
        Size size = tree.measure(Constraints.loose(100.0f, 40.0f));
        assertEquals(14.0f, size.width(), 0.001f);
        assertEquals(8.0f, size.height(), 0.001f);
        assertEquals(0.5f, leaf.shear().x());
        assertEquals(0.0f, leaf.shear().y());
    }

    /// Rotates a leaf 90 degrees and publishes the swapped AABB.
    @Test
    void rotateNinetySwapsLeafSize() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "box",
                new Size(10.0f, 8.0f),
                java.util.List.of(new LayoutModifier.Rotate(90.0f)),
                false,
                SemanticsRole.NONE,
                "box",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.box("root", java.util.List.of(), leaf));
        Size size = tree.measure(Constraints.loose(100.0f, 40.0f));
        assertEquals(8.0f, size.width(), 0.001f);
        assertEquals(10.0f, size.height(), 0.001f);
        assertEquals(90.0f, leaf.rotationDegrees());
    }

    /// Translates a child inside an expanded box.
    @Test
    void translateOffsetsChildInsideExpandedBox() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "box",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "box",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.box("root", java.util.List.of(new LayoutModifier.Translate(6.0f, 4.0f)), leaf));
        Size size = tree.measure(Constraints.loose(100.0f, 40.0f));
        assertEquals(16.0f, size.width());
        assertEquals(12.0f, size.height());
        tree.place();
        assertEquals(6.0f, leaf.offset().x());
        assertEquals(4.0f, leaf.offset().y());
        assertEquals(6.0f, tree.root().translation().x());
        assertEquals(4.0f, tree.root().translation().y());
    }

    /// Scales a leaf through LayoutModifier.Scale.
    @Test
    void scaleDoublesLeafSize() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "box",
                new Size(10.0f, 8.0f),
                java.util.List.of(new LayoutModifier.Scale(2.0f)),
                false,
                SemanticsRole.NONE,
                "box",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.box("root", java.util.List.of(), leaf));
        Size size = tree.measure(Constraints.loose(100.0f, 40.0f));
        assertEquals(20.0f, size.width());
        assertEquals(16.0f, size.height());
    }

    /// Places a row from the trailing edge when the reading direction is RTL.
    @Test
    void rtlRowPlacesFromTrailingEdge() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode left = factory.leaf(
                "a",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "a",
                java.util.Set.of(),
                null
        );
        LayoutNode right = factory.leaf(
                "b",
                new Size(12.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "b",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.row(
                "row",
                Alignment.START,
                java.util.List.of(new LayoutModifier.ReadingDirection(TextDirection.RTL)),
                left,
                right
        ));
        Size size = tree.measure(Constraints.loose(100.0f, 40.0f));
        assertEquals(22.0f, size.width());
        tree.place();
        assertEquals(12.0f, left.offset().x());
        assertEquals(0.0f, right.offset().x());
    }

    /// Publishes center alignment lines in addition to the baseline.
    @Test
    void alignmentLinesPublishCenter() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "box",
                new Size(12.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "box",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.box("root", java.util.List.of(), leaf));
        tree.measure(Constraints.loose(100.0f, 40.0f));
        assertEquals(8.0f, leaf.alignmentLines().baseline());
        assertEquals(6.0f, leaf.alignmentLines().centerX());
        assertEquals(4.0f, leaf.alignmentLines().centerY());
        assertEquals(6.0f, tree.root().alignmentLines().centerX());
        assertEquals(4.0f, tree.root().alignmentLines().centerY());
    }

    /// Fits a 2:1 aspect ratio to the tighter incoming height.
    @Test
    void aspectRatioFitsMaxHeight() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "box",
                new Size(10.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "box",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.box("root", java.util.List.of(new LayoutModifier.AspectRatio(2.0f)), leaf));
        Size size = tree.measure(Constraints.loose(100.0f, 40.0f));
        assertEquals(80.0f, size.width());
        assertEquals(40.0f, size.height());
    }

    /// Delegates measure and place to a custom gap-row policy.
    @Test
    void customLayoutPlacesWithGap() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode left = factory.leaf(
                "left",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "left",
                java.util.Set.of(),
                null
        );
        LayoutNode right = factory.leaf(
                "right",
                new Size(12.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "right",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.custom("custom", java.util.List.of(), new CustomLayout() {
            @Override
            public Size measure(Constraints constraints, java.util.List<LayoutNode> children) {
                float width = 0.0f;
                float height = 0.0f;
                for (LayoutNode child : children) {
                    Size childSize = child.measure(Constraints.loose(constraints.maxWidth(), constraints.maxHeight()));
                    width += childSize.width() + 4.0f;
                    height = Math.max(height, childSize.height());
                }
                return new Size(Math.max(0.0f, width - 4.0f), height);
            }

            @Override
            public void place(Offset inner, Offset root, Size size, java.util.List<LayoutNode> children) {
                float x = 0.0f;
                for (LayoutNode child : children) {
                    Offset childOffset = inner.plus(new Offset(x, 0.0f));
                    child.place(childOffset, root.plus(new Offset(x, 0.0f)));
                    x += child.size().width() + 4.0f;
                }
            }
        }, left, right));
        Size size = tree.measure(Constraints.loose(100.0f, 40.0f));
        assertEquals(26.0f, size.width());
        assertEquals(8.0f, size.height());
        tree.place();
        assertEquals(0.0f, left.offset().x());
        assertEquals(14.0f, right.offset().x());
    }

    /// Sizes an overlay to the union of offset children.
    @Test
    void overlayUnionsOffsetChildren() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode host = factory.leaf(
                "host",
                new Size(20.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "host",
                java.util.Set.of(),
                null
        );
        LayoutNode badge = factory.leaf(
                "badge",
                new Size(8.0f, 8.0f),
                java.util.List.of(new LayoutModifier.OverlayOffset(16.0f, 6.0f)),
                false,
                SemanticsRole.NONE,
                "badge",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.overlay("overlay", java.util.List.of(), host, badge));
        Size size = tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(24.0f, size.width());
        assertEquals(14.0f, size.height());
        tree.place();
        assertEquals(0.0f, host.offset().x());
        assertEquals(16.0f, badge.offset().x());
        assertEquals(6.0f, badge.offset().y());
    }

    /// Keeps a portal slot sized to the first child.
    @Test
    void portalKeepsFirstChildSlot() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode slot = factory.leaf(
                "slot",
                new Size(20.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "slot",
                java.util.Set.of(),
                null
        );
        LayoutNode floating = factory.leaf(
                "float",
                new Size(8.0f, 8.0f),
                java.util.List.of(new LayoutModifier.OverlayOffset(16.0f, 6.0f)),
                false,
                SemanticsRole.NONE,
                "float",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.portal("portal", java.util.List.of(), slot, floating));
        Size size = tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(20.0f, size.width());
        assertEquals(10.0f, size.height());
        tree.place();
        assertEquals(16.0f, floating.offset().x());
        assertEquals(6.0f, floating.offset().y());
    }

    /// Hits a portaled child that overflows the slot when the portal does not clip.
    @Test
    void portalOverflowRemainsHittable() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode slot = factory.leaf(
                "slot",
                new Size(20.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "slot",
                java.util.Set.of(),
                null
        );
        LayoutNode floating = factory.leaf(
                "float",
                new Size(8.0f, 8.0f),
                java.util.List.of(new LayoutModifier.OverlayOffset(16.0f, 6.0f)),
                false,
                SemanticsRole.NONE,
                "float",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.portal("portal", java.util.List.of(), slot, floating));
        tree.measure(Constraints.loose(100.0f, 100.0f));
        tree.place();
        assertEquals(floating, HitTester.hit(tree.root(), 22.0f, 8.0f));
        SpatialIndex index = tree.spatialIndex();
        assertNotNull(index);
        assertEquals(floating, index.hit(22.0f, 8.0f));
    }

    /// Clips a portaled child that overflows the slot.
    @Test
    void clipExcludesOverflowingPortalChild() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode slot = factory.leaf(
                "slot",
                new Size(20.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "slot",
                java.util.Set.of(),
                null
        );
        LayoutNode floating = factory.leaf(
                "float",
                new Size(8.0f, 8.0f),
                java.util.List.of(new LayoutModifier.OverlayOffset(16.0f, 6.0f)),
                false,
                SemanticsRole.NONE,
                "float",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.portal("portal", java.util.List.of(new LayoutModifier.Clip()), slot, floating));
        tree.measure(Constraints.loose(100.0f, 100.0f));
        tree.place();
        assertTrue(tree.root().clipsHits());
        assertNull(HitTester.hit(tree.root(), 22.0f, 8.0f));
        SpatialIndex index = tree.spatialIndex();
        assertNotNull(index);
        assertNull(index.hit(22.0f, 8.0f));
        assertEquals(slot, HitTester.hit(tree.root(), 10.0f, 5.0f));
        assertEquals(slot, index.hit(10.0f, 5.0f));
    }

    /// Scroll viewports clip overflowing content from hit testing.
    @Test
    void scrollClipsOverflowingContent() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode content = factory.leaf(
                "content",
                new Size(20.0f, 40.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "content",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.scroll("scroll", java.util.List.of(), content));
        tree.measure(Constraints.loose(20.0f, 10.0f));
        tree.place();
        assertTrue(tree.root().clipsHits());
        assertEquals(10.0f, tree.root().size().height());
        assertNull(HitTester.hit(tree.root(), 10.0f, 30.0f));
        SpatialIndex index = tree.spatialIndex();
        assertNotNull(index);
        assertNull(index.hit(10.0f, 30.0f));
        assertEquals(content, HitTester.hit(tree.root(), 10.0f, 5.0f));
        assertEquals(content, index.hit(10.0f, 5.0f));
    }

    /// Ignores a front-most leaf so the sibling behind it receives the hit.
    @Test
    void ignorePointerSkipsFrontMostLeaf() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode back = factory.leaf(
                "back",
                new Size(20.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "back",
                java.util.Set.of(),
                null
        );
        LayoutNode front = factory.leaf(
                "front",
                new Size(20.0f, 10.0f),
                java.util.List.of(new LayoutModifier.IgnorePointer()),
                false,
                SemanticsRole.NONE,
                "front",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.box("root", java.util.List.of(), back, front));
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        assertTrue(front.ignoresPointer());
        assertEquals(back, HitTester.hit(tree.root(), 10.0f, 5.0f));
        SpatialIndex index = tree.spatialIndex();
        assertNotNull(index);
        assertEquals(back, index.hit(10.0f, 5.0f));
    }

    /// Absorbs a hit on a parent so its descendant is not the target.
    @Test
    void absorbPointerStopsOnParent() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode child = factory.leaf(
                "child",
                new Size(20.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "child",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.box("root", java.util.List.of(new LayoutModifier.AbsorbPointer()), child));
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        assertTrue(tree.root().absorbsPointer());
        assertEquals(tree.root(), HitTester.hit(tree.root(), 10.0f, 5.0f));
        SpatialIndex index = tree.spatialIndex();
        assertNotNull(index);
        assertEquals(tree.root(), index.hit(10.0f, 5.0f));
    }

    /// Rejects a rounded-rect corner that sits inside the AABB.
    @Test
    void clipRRectRejectsCornerOutsideArc() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "round",
                new Size(20.0f, 20.0f),
                java.util.List.of(new LayoutModifier.ClipRRect(10.0f)),
                false,
                SemanticsRole.NONE,
                "round",
                java.util.Set.of(),
                null
        );
        tree.setRoot(leaf);
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        assertTrue(leaf.clipsHits());
        assertNull(HitTester.hit(tree.root(), 0.5f, 0.5f));
        assertEquals(leaf, HitTester.hit(tree.root(), 10.0f, 10.0f));
        SpatialIndex index = tree.spatialIndex();
        assertNotNull(index);
        assertNull(index.hit(0.5f, 0.5f));
        assertEquals(leaf, index.hit(10.0f, 10.0f));
    }

    /// Rejects an oval AABB corner that sits outside the ellipse.
    @Test
    void clipOvalRejectsCornerOutsideEllipse() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "oval",
                new Size(20.0f, 10.0f),
                java.util.List.of(new LayoutModifier.ClipOval()),
                false,
                SemanticsRole.NONE,
                "oval",
                java.util.Set.of(),
                null
        );
        tree.setRoot(leaf);
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        assertNull(HitTester.hit(tree.root(), 1.0f, 1.0f));
        assertEquals(leaf, HitTester.hit(tree.root(), 10.0f, 5.0f));
        SpatialIndex index = tree.spatialIndex();
        assertNotNull(index);
        assertNull(index.hit(1.0f, 1.0f));
        assertEquals(leaf, index.hit(10.0f, 5.0f));
    }

    /// Rejects a point inside the AABB but outside a triangular path clip.
    @Test
    void clipPathRejectsOutsideTriangle() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode leaf = factory.leaf(
                "path",
                new Size(20.0f, 20.0f),
                java.util.List.of(new LayoutModifier.ClipPath(new float[] {0.0f, 0.0f, 20.0f, 0.0f, 10.0f, 20.0f})),
                false,
                SemanticsRole.NONE,
                "path",
                java.util.Set.of(),
                null
        );
        tree.setRoot(leaf);
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        assertNull(HitTester.hit(tree.root(), 1.0f, 18.0f));
        assertEquals(leaf, HitTester.hit(tree.root(), 10.0f, 6.0f));
        SpatialIndex index = tree.spatialIndex();
        assertNotNull(index);
        assertNull(index.hit(1.0f, 18.0f));
        assertEquals(leaf, index.hit(10.0f, 6.0f));
    }

    /// Applies a parent rounded clip to a full-size child.
    @Test
    void clipRRectOnParentExcludesChildCorner() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode child = factory.leaf(
                "child",
                new Size(20.0f, 20.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "child",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.box("root", java.util.List.of(new LayoutModifier.ClipRRect(10.0f)), child));
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        assertNull(HitTester.hit(tree.root(), 0.5f, 0.5f));
        assertEquals(child, HitTester.hit(tree.root(), 10.0f, 10.0f));
        SpatialIndex index = tree.spatialIndex();
        assertNotNull(index);
        assertNull(index.hit(0.5f, 0.5f));
        assertEquals(child, index.hit(10.0f, 10.0f));
    }

    /// Captures the DOWN target and routes later MOVE/UP to it even outside its bounds.
    @Test
    void pointerCaptureRoutesMoveAndUpOutsideTarget() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        AtomicInteger activations = new AtomicInteger();
        LayoutNode target = factory.leaf(
                "target",
                new Size(20.0f, 10.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "target",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                activations::incrementAndGet
        );
        LayoutNode sibling = factory.leaf(
                "sibling",
                new Size(20.0f, 10.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "sibling",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> {
                }
        );
        tree.setRoot(factory.row("root", Alignment.START, java.util.List.of(), target, sibling));
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        assertEquals(0.0f, target.offset().x());
        assertEquals(20.0f, sibling.offset().x());
        assertTrue(tree.dispatch(new PointerEvent(PointerEventType.DOWN, 5.0f, 5.0f)));
        assertEquals(target, tree.pointerCapture());
        assertEquals(sibling, HitTester.hit(tree.root(), 30.0f, 5.0f));
        assertTrue(tree.dispatch(new PointerEvent(PointerEventType.MOVE, 30.0f, 5.0f)));
        assertEquals(target, tree.pointerCapture());
        assertTrue(tree.dispatch(new PointerEvent(PointerEventType.UP, 30.0f, 5.0f)));
        assertEquals(1, activations.get());
        assertNull(tree.pointerCapture());
    }

    /// Clears pointer capture on `LEAVE` and `CAPTURE_CHANGED`.
    @Test
    void pointerCaptureClearsOnLeaveAndCaptureChanged() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode target = factory.leaf(
                "target",
                new Size(20.0f, 10.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "target",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> {
                }
        );
        tree.setRoot(factory.box("root", java.util.List.of(), target));
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        assertTrue(tree.dispatch(new PointerEvent(PointerEventType.DOWN, 5.0f, 5.0f)));
        assertEquals(target, tree.pointerCapture());
        assertTrue(tree.dispatch(new PointerEvent(PointerEventType.LEAVE, 80.0f, 80.0f)));
        assertNull(tree.pointerCapture());
        assertTrue(tree.dispatch(new PointerEvent(PointerEventType.DOWN, 5.0f, 5.0f)));
        assertEquals(target, tree.pointerCapture());
        assertTrue(tree.dispatch(new PointerEvent(PointerEventType.CAPTURE_CHANGED, 5.0f, 5.0f)));
        assertNull(tree.pointerCapture());
    }

    /// Bubbles a pointer event from the target toward the root until a listener consumes it.
    @Test
    void pointerListenersBubbleUntilConsumed() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        AtomicInteger targetHits = new AtomicInteger();
        AtomicInteger rootHits = new AtomicInteger();
        LayoutNode target = factory.leaf(
                "target",
                new Size(20.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "target",
                java.util.Set.of(),
                null
        );
        LayoutNode root = factory.box("root", java.util.List.of(), target);
        target.addPointerListener(event -> {
            targetHits.incrementAndGet();
            return false;
        });
        root.addPointerListener(event -> {
            rootHits.incrementAndGet();
            return true;
        });
        tree.setRoot(root);
        tree.measure(Constraints.loose(100.0f, 40.0f));
        tree.place();
        assertTrue(tree.dispatch(new PointerEvent(PointerEventType.MOVE, 5.0f, 5.0f)));
        assertEquals(1, targetHits.get());
        assertEquals(1, rootHits.get());
        target.addPointerListener(event -> true);
        assertTrue(tree.dispatch(new PointerEvent(PointerEventType.MOVE, 5.0f, 5.0f)));
        assertEquals(2, targetHits.get());
        assertEquals(1, rootHits.get());
    }

    /// Wraps a third flow child onto the next line.
    @Test
    void flowWrapsToNextLine() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode a = factory.leaf(
                "a",
                new Size(40.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "a",
                java.util.Set.of(),
                null
        );
        LayoutNode b = factory.leaf(
                "b",
                new Size(40.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "b",
                java.util.Set.of(),
                null
        );
        LayoutNode c = factory.leaf(
                "c",
                new Size(40.0f, 12.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "c",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.flow("flow", Alignment.START, java.util.List.of(), a, b, c));
        Size size = tree.measure(Constraints.loose(90.0f, 100.0f));
        assertEquals(80.0f, size.width());
        assertEquals(22.0f, size.height());
        tree.place();
        assertEquals(0.0f, a.offset().x());
        assertEquals(40.0f, b.offset().x());
        assertEquals(0.0f, c.offset().x());
        assertEquals(10.0f, c.offset().y());
    }

    /// Places four grid children on two columns using the widest cell per column.
    @Test
    void gridPlacesTwoColumns() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode a = factory.leaf(
                "a",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "a",
                java.util.Set.of(),
                null
        );
        LayoutNode b = factory.leaf(
                "b",
                new Size(20.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "b",
                java.util.Set.of(),
                null
        );
        LayoutNode c = factory.leaf(
                "c",
                new Size(12.0f, 9.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "c",
                java.util.Set.of(),
                null
        );
        LayoutNode d = factory.leaf(
                "d",
                new Size(8.0f, 9.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "d",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.grid("grid", 2, Alignment.START, java.util.List.of(), a, b, c, d));
        Size size = tree.measure(Constraints.loose(100.0f, 100.0f));
        assertEquals(32.0f, size.width());
        assertEquals(17.0f, size.height());
        tree.place();
        assertEquals(0.0f, a.offset().x());
        assertEquals(12.0f, b.offset().x());
        assertEquals(0.0f, c.offset().x());
        assertEquals(8.0f, c.offset().y());
        assertEquals(12.0f, d.offset().x());
        assertEquals(8.0f, d.offset().y());
    }

    /// Places wrapping flow children from the trailing edge when the direction is RTL.
    @Test
    void rtlFlowPlacesFromTrailingEdge() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode a = factory.leaf(
                "a",
                new Size(40.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "a",
                java.util.Set.of(),
                null
        );
        LayoutNode b = factory.leaf(
                "b",
                new Size(40.0f, 10.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "b",
                java.util.Set.of(),
                null
        );
        LayoutNode c = factory.leaf(
                "c",
                new Size(40.0f, 12.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "c",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.flow(
                "flow",
                Alignment.START,
                java.util.List.of(new LayoutModifier.ReadingDirection(TextDirection.RTL)),
                a,
                b,
                c
        ));
        Size size = tree.measure(Constraints.loose(90.0f, 100.0f));
        assertEquals(80.0f, size.width());
        tree.place();
        assertEquals(40.0f, a.offset().x());
        assertEquals(0.0f, b.offset().x());
        assertEquals(40.0f, c.offset().x());
        assertEquals(10.0f, c.offset().y());
    }

    /// Places grid columns from the trailing edge when the direction is RTL.
    @Test
    void rtlGridPlacesFromTrailingEdge() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode a = factory.leaf(
                "a",
                new Size(10.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "a",
                java.util.Set.of(),
                null
        );
        LayoutNode b = factory.leaf(
                "b",
                new Size(20.0f, 8.0f),
                java.util.List.of(),
                false,
                SemanticsRole.NONE,
                "b",
                java.util.Set.of(),
                null
        );
        tree.setRoot(factory.grid(
                "grid",
                2,
                Alignment.START,
                java.util.List.of(new LayoutModifier.ReadingDirection(TextDirection.RTL)),
                a,
                b
        ));
        Size size = tree.measure(Constraints.loose(100.0f, 40.0f));
        assertEquals(30.0f, size.width());
        tree.place();
        assertEquals(20.0f, a.offset().x());
        assertEquals(0.0f, b.offset().x());
    }

    /// Verifies pointer and keyboard activation of the bootstrap increment button.
    @Test
    void pointerAndKeyboardActivateBootstrapCounter() {
        LayoutTree tree = new LayoutTree();
        AtomicInteger count = new AtomicInteger();
        tree.setRoot(BootstrapCounterPane.create(tree, count));
        tree.measure(Constraints.loose(200.0f, 200.0f));
        tree.place();
        SemanticsNode button = tree.semantics().nodeWith(SemanticsAction.ACTIVATE);
        assertEquals(SemanticsRole.BUTTON, button.role());
        assertEquals("Increment", button.label());
        assertNotNull(tree.focus().focusedId());
        assertTrue(tree.dispatch(new PointerEvent(
                PointerEventType.DOWN,
                button.bounds().x() + 1.0f,
                button.bounds().y() + 1.0f
        )));
        assertTrue(tree.dispatch(new PointerEvent(
                PointerEventType.UP,
                button.bounds().x() + 1.0f,
                button.bounds().y() + 1.0f
        )));
        assertEquals(1, count.get());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ENTER)));
        assertEquals(2, count.get());
        assertEquals(button.bounds(), tree.semantics().nodeWith(SemanticsAction.ACTIVATE).bounds());
    }

    /// Moves document-order focus backward when Tab is dispatched with shift.
    @Test
    void shiftTabMovesFocusToPreviousNode() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode first = factory.leaf(
                "first",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "One",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        LayoutNode second = factory.leaf(
                "second",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "Two",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        tree.setRoot(factory.column("root", Alignment.START, java.util.List.of(), first, second));
        tree.measure(Constraints.loose(100.0f, 100.0f));
        tree.place();
        assertEquals(first.id(), tree.focus().focusedId());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.TAB)));
        assertEquals(second.id(), tree.focus().focusedId());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.TAB, true)));
        assertEquals(first.id(), tree.focus().focusedId());
        assertTrue(tree.focus().focusVisible());
        assertTrue(tree.dispatch(new PointerEvent(
                PointerEventType.DOWN,
                second.bounds().x() + 1.0f,
                second.bounds().y() + 1.0f
        )));
        assertEquals(second.id(), tree.focus().focusedId());
        assertFalse(tree.focus().focusVisible());
    }

    /// Restricts Tab traversal to a trapped subtree until the trap is cleared.
    @Test
    void trapKeepsTabInsideSubtree() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode outside = factory.leaf(
                "outside",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "Outside",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        LayoutNode innerFirst = factory.leaf(
                "inner-first",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "InnerOne",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        LayoutNode innerSecond = factory.leaf(
                "inner-second",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "InnerTwo",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        LayoutNode dialog = factory.column(
                "dialog",
                Alignment.START,
                java.util.List.of(),
                innerFirst,
                innerSecond
        );
        tree.setRoot(factory.column("root", Alignment.START, java.util.List.of(), outside, dialog));
        tree.measure(Constraints.loose(100.0f, 100.0f));
        tree.place();
        assertEquals(outside.id(), tree.focus().focusedId());
        tree.focus().trap(dialog);
        assertEquals(dialog.id(), tree.focus().trapId());
        assertEquals(innerFirst.id(), tree.focus().focusedId());
        assertFalse(tree.focus().request(outside));
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.TAB)));
        assertEquals(innerSecond.id(), tree.focus().focusedId());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.TAB)));
        assertEquals(innerFirst.id(), tree.focus().focusedId());
        tree.focus().clearTrap();
        assertEquals(null, tree.focus().trapId());
        assertTrue(tree.focus().request(outside));
        assertEquals(outside.id(), tree.focus().focusedId());
    }

    /// Transfers keyboard focus from one window tree to another and restores it.
    @Test
    void transferMovesFocusBetweenWindowTrees() {
        LayoutTree firstTree = new LayoutTree();
        LayoutFactory firstFactory = new LayoutFactory(firstTree);
        LayoutNode first = firstFactory.leaf(
                "first",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "One",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        firstTree.setRoot(firstFactory.column("root", Alignment.START, java.util.List.of(), first));
        firstTree.measure(Constraints.loose(100.0f, 100.0f));
        firstTree.place();
        firstTree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.TAB));
        LayoutTree secondTree = new LayoutTree();
        LayoutFactory secondFactory = new LayoutFactory(secondTree);
        LayoutNode second = secondFactory.leaf(
                "second",
                new Size(20.0f, 12.0f),
                java.util.List.of(),
                true,
                SemanticsRole.BUTTON,
                "Two",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        secondTree.setRoot(secondFactory.column("root", Alignment.START, java.util.List.of(), second));
        secondTree.measure(Constraints.loose(100.0f, 100.0f));
        secondTree.place();
        assertTrue(firstTree.focus().transferTo(secondTree.focus()));
        assertFalse(firstTree.focus().focusVisible());
        assertEquals(first.id(), firstTree.focus().focusedId());
        assertTrue(secondTree.focus().focusVisible());
        assertEquals(second.id(), secondTree.focus().focusedId());
        assertTrue(secondTree.focus().transferTo(firstTree.focus()));
        assertTrue(firstTree.focus().focusVisible());
        assertEquals(first.id(), firstTree.focus().focusedId());
        assertFalse(secondTree.focus().focusVisible());
    }

    /// Publishes live-region politeness through the semantics snapshot.
    @Test
    void publishesLiveRegionOnSemanticsSnapshot() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode status = factory.leaf(
                "status",
                new Size(80.0f, 16.0f),
                java.util.List.of(),
                false,
                SemanticsRole.STATUS,
                "Ready",
                java.util.Set.of(),
                null
        );
        status.setLiveRegion(SemanticsLiveRegion.ASSERTIVE);
        AtomicInteger announced = new AtomicInteger();
        status.addLabelListener(announced::incrementAndGet);
        status.setLabel("Updated");
        assertEquals(1, announced.get());
        tree.setRoot(factory.column("root", Alignment.START, java.util.List.of(), status));
        tree.measure(Constraints.loose(100.0f, 100.0f));
        tree.place();
        SemanticsNode snapshot = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.STATUS)
                .findFirst()
                .orElseThrow();
        assertEquals(SemanticsLiveRegion.ASSERTIVE, snapshot.liveRegion());
        assertEquals("Updated", snapshot.label());
        assertEquals(SemanticsLiveRegion.OFF, tree.semantics().nodes().getFirst().liveRegion());
    }

    /// Publishes a UTF-16 text range through the semantics snapshot.
    @Test
    void publishesTextRangeOnSemanticsSnapshot() {
        LayoutTree tree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode field = factory.leaf(
                "field",
                new Size(80.0f, 16.0f),
                java.util.List.of(),
                true,
                SemanticsRole.TEXT_FIELD,
                "hello",
                java.util.Set.of(),
                null
        );
        field.setTextRange(new SemanticsTextRange(1, 4, 4));
        tree.setRoot(factory.column("root", Alignment.START, java.util.List.of(), field));
        tree.measure(Constraints.loose(100.0f, 100.0f));
        tree.place();
        SemanticsNode snapshot = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertEquals(new SemanticsTextRange(1, 4, 4), snapshot.textRange());
        assertFalse(snapshot.disabled());
        assertFalse(snapshot.readOnly());
        field.setDisabled(true);
        field.setReadOnly(true);
        SemanticsNode gated = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertTrue(gated.disabled());
        assertTrue(gated.readOnly());
        field.setHint("Greeting");
        SemanticsNode hinted = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertEquals("Greeting", hinted.hint());
        assertTrue(hinted.focusable());
        assertFalse(hinted.password());
        field.setPassword(true);
        SemanticsNode secret = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertTrue(secret.password());
        field.setAccessKey("G");
        field.setAcceleratorKey("Ctrl+G");
        SemanticsNode keyed = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertEquals("G", keyed.accessKey());
        assertEquals("Ctrl+G", keyed.acceleratorKey());
        field.setRequired(true);
        field.setItemStatus("invalid");
        field.setItemType("edit");
        field.setLandmarkType(80002);
        field.setLocalizedLandmarkType("main");
        field.setAriaRole("textbox");
        field.setAriaProperties("required=true");
        field.setControllerFor("submit");
        field.setDescribedBy("hint");
        field.setFlowsTo("next");
        field.setLabeledBy("title");
        field.setFlowsFrom("prev");
        field.setOptimizeForVisualContent(true);
        field.setFillColor(0xFF1565C0);
        field.setOutlineColor(0xFFE0E0E0);
        field.setFillType(1);
        field.setVisualEffects(1);
        field.setOutlineThickness(2);
        field.setRotation(90);
        field.setPeripheral(true);
        field.setAnnotationType(60000);
        field.setAnnotationObjects("note");
        field.setLocale("en-US");
        SemanticsNode form = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertTrue(form.required());
        assertEquals("invalid", form.itemStatus());
        assertEquals("edit", form.itemType());
        assertEquals(80002, form.landmarkType());
        assertEquals("main", form.localizedLandmarkType());
        assertEquals("textbox", form.ariaRole());
        assertEquals("required=true", form.ariaProperties());
        assertEquals("submit", form.controllerFor());
        assertEquals("hint", form.describedBy());
        assertEquals("next", form.flowsTo());
        assertEquals("title", form.labeledBy());
        assertEquals("prev", form.flowsFrom());
        assertTrue(form.optimizeForVisualContent());
        assertEquals(0xFF1565C0, form.fillColor());
        assertEquals(0xFFE0E0E0, form.outlineColor());
        assertEquals(1, form.fillType());
        assertEquals(1, form.visualEffects());
        assertEquals(2, form.outlineThickness());
        assertEquals(90, form.rotation());
        assertTrue(form.peripheral());
        assertEquals(60000, form.annotationType());
        assertEquals("note", form.annotationObjects());
        assertEquals("en-US", form.locale());
        field.setLevel(2);
        field.setPositionInSet(1);
        field.setSizeOfSet(3);
        field.setDescription("Guest name");
        SemanticsNode set = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertEquals(2, set.level());
        assertEquals(1, set.positionInSet());
        assertEquals(3, set.sizeOfSet());
        assertEquals("Guest name", set.description());
        field.setError(true);
        SemanticsNode invalid = tree.semantics().nodes().stream()
                .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                .findFirst()
                .orElseThrow();
        assertTrue(invalid.error());
        assertEquals("", tree.semantics().nodes().getFirst().hint());
        assertEquals(null, tree.semantics().nodes().getFirst().textRange());
        assertThrows(IllegalArgumentException.class, () -> new SemanticsTextRange(2, 1, 2));
    }
}
