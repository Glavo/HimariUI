package org.glavo.himari.controls;

import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies unstyled breadcrumb and hyperlink controls through the shipped gallery.
@NotNullByDefault
final class BreadcrumbHyperlinkTest {
    /// Moves a breadcrumb selection through pointer activation and arrow keys.
    @Test
    void breadcrumbSelectsThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("API", gallery.breadcrumb().value());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode crumb = first(tree, SemanticsRole.BREADCRUMB);
        assertEquals("API", crumb.label());
        click(tree, crumb);
        assertEquals("Docs", gallery.breadcrumb().value());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ARROW_RIGHT)));
        assertEquals("API", gallery.breadcrumb().value());
        gallery.breadcrumb().setDisabled(true);
        gallery.breadcrumb().select(0);
        assertEquals("API", gallery.breadcrumb().value());
        assertTrue(gallery.breadcrumb().disabled());
    }

    /// Activates a hyperlink through pointer and the enter key.
    @Test
    void hyperlinkActivatesThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("plans", gallery.link().href());
        assertEquals(0, gallery.link().activations());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode link = first(tree, SemanticsRole.LINK);
        assertEquals("Plans", link.label());
        assertEquals("plans", link.itemStatus());
        click(tree, link);
        assertEquals(1, gallery.link().activations());
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ENTER)));
        assertEquals(2, gallery.link().activations());
        gallery.link().setDisabled(true);
        gallery.link().activate();
        assertEquals(2, gallery.link().activations());
        assertTrue(gallery.link().disabled());
    }

    /// Dispatches a pointer press on `node`.
    private static void click(LayoutTree tree, SemanticsNode node) {
        float x = node.bounds().x() + 1.0f;
        float y = node.bounds().y() + 1.0f;
        tree.dispatch(new PointerEvent(PointerEventType.DOWN, x, y));
        tree.dispatch(new PointerEvent(PointerEventType.UP, x, y));
    }

    /// Returns the first node with the role.
    private static SemanticsNode first(LayoutTree tree, SemanticsRole role) {
        for (SemanticsNode node : tree.semantics().nodes()) {
            if (node.role() == role) {
                return node;
            }
        }
        throw new AssertionError("Missing " + role);
    }
}
