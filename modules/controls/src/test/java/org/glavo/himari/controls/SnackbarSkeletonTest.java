package org.glavo.himari.controls;

import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies unstyled snackbar and skeleton controls through the shipped gallery.
@NotNullByDefault
final class SnackbarSkeletonTest {
    /// Shows a snackbar, runs its action through pointer activation, and hides it.
    @Test
    void snackbarActionDismissesThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertFalse(gallery.snackbar().visible());
        assertEquals(0, gallery.snackbar().actions());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode snackbar = first(tree, SemanticsRole.SNACKBAR);
        assertEquals("Deleted", snackbar.label());
        assertEquals("hidden", snackbar.itemStatus());
        assertEquals(SemanticsLiveRegion.OFF, snackbar.liveRegion());
        gallery.snackbar().show();
        assertTrue(gallery.snackbar().visible());
        assertEquals("Undo", first(tree, SemanticsRole.SNACKBAR).itemStatus());
        assertEquals(SemanticsLiveRegion.POLITE, first(tree, SemanticsRole.SNACKBAR).liveRegion());
        click(tree, first(tree, SemanticsRole.SNACKBAR));
        assertEquals(1, gallery.snackbar().actions());
        assertFalse(gallery.snackbar().visible());
        assertEquals("hidden", first(tree, SemanticsRole.SNACKBAR).itemStatus());
        gallery.snackbar().setDisabled(true);
        gallery.snackbar().show();
        assertFalse(gallery.snackbar().visible());
        assertTrue(gallery.snackbar().disabled());
    }

    /// Publishes loading then ready through the gallery skeleton leaf.
    @Test
    void skeletonPublishesLoadingThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertTrue(gallery.skeleton().loading());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode skeleton = first(tree, SemanticsRole.SKELETON);
        assertEquals("Loading", skeleton.label());
        assertEquals("loading", skeleton.itemStatus());
        assertTrue(skeleton.bounds().height() > 0.0f);
        gallery.skeleton().setLoading(false);
        assertEquals("ready", first(tree, SemanticsRole.SKELETON).itemStatus());
        gallery.skeleton().setDisabled(true);
        assertTrue(gallery.skeleton().disabled());
        assertTrue(first(tree, SemanticsRole.SKELETON).disabled());
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
