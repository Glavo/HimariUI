package org.glavo.himari.controls;

import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the unstyled badge through the shipped gallery control.
@NotNullByDefault
final class BadgeTest {
    /// Publishes a label through the gallery leaf and ignores activation.
    @Test
    void publishesLabelThroughShippedLeaf() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        assertEquals("New", gallery.badge().label());
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 3000.0f));
        tree.place();
        SemanticsNode badge = first(tree, SemanticsRole.BADGE);
        assertEquals("New", badge.label());
        assertTrue(badge.bounds().height() > 0.0f);
        gallery.badge().setLabel("Beta");
        assertEquals("Beta", first(tree, SemanticsRole.BADGE).label());
        gallery.badge().setDisabled(true);
        assertTrue(gallery.badge().disabled());
        assertTrue(first(tree, SemanticsRole.BADGE).disabled());
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
