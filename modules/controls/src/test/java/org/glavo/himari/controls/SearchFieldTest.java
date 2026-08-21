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

/// Verifies the unstyled search field through the shipped gallery control.
@NotNullByDefault
final class SearchFieldTest {
    /// Submits a query through pointer activation and the enter key.
    @Test
    void submitsThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("", gallery.search().query());
        assertEquals("", gallery.search().submitted());
        gallery.search().setQuery("fonts");
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode search = first(tree, SemanticsRole.SEARCH_BOX);
        assertEquals("fonts", search.label());
        click(tree, search);
        assertEquals("fonts", gallery.search().submitted());
        gallery.search().setQuery("layout");
        assertTrue(tree.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ENTER)));
        assertEquals("layout", gallery.search().submitted());
        gallery.search().setDisabled(true);
        gallery.search().setQuery("ignored");
        gallery.search().submit();
        assertEquals("layout", gallery.search().submitted());
        assertTrue(gallery.search().disabled());
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
