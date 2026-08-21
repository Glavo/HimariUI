package org.glavo.himari.controls;

import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies unstyled avatar and banner controls through the shipped gallery.
@NotNullByDefault
final class AvatarBannerTest {
    /// Publishes initials, then an image source, through the gallery leaf.
    @Test
    void avatarPublishesInitialsThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("Ada Lovelace", gallery.avatar().name());
        assertEquals("AL", gallery.avatar().initials());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode avatar = first(tree, SemanticsRole.AVATAR);
        assertEquals("Ada Lovelace", avatar.label());
        assertEquals("AL", avatar.itemStatus());
        assertTrue(avatar.bounds().height() > 0.0f);
        gallery.avatar().setSource("ada.png");
        assertEquals("ada.png", first(tree, SemanticsRole.AVATAR).itemStatus());
        gallery.avatar().setDisabled(true);
        assertTrue(gallery.avatar().disabled());
        assertTrue(first(tree, SemanticsRole.AVATAR).disabled());
    }

    /// Dismisses a banner through pointer activation.
    @Test
    void bannerDismissesThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertTrue(gallery.banner().visible());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode banner = first(tree, SemanticsRole.BANNER);
        assertEquals("Saved", banner.label());
        assertEquals("visible", banner.itemStatus());
        click(tree, banner);
        assertFalse(gallery.banner().visible());
        assertEquals("dismissed", first(tree, SemanticsRole.BANNER).itemStatus());
        gallery.banner().setDisabled(true);
        gallery.banner().show();
        assertFalse(gallery.banner().visible());
        assertTrue(gallery.banner().disabled());
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
