package org.glavo.himari.controls;

import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.runtime.transition.TransitionLifetime;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies matched-geometry overlay capture through the shipped gallery control.
@NotNullByDefault
final class SharedElementTest {
    /// Interpolates a shared-element overlay from placed source and destination leaves.
    @Test
    void interpolatesOverlayFromPlacedLeaves() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 1200.0f));
        tree.place();
        LogicalRect overlay = gallery.shared().overlay(0.5);
        assertNotNull(gallery.shared().link());
        LogicalRect source = gallery.shared().link().source();
        LogicalRect destination = gallery.shared().link().destination();
        assertTrue(source.width() > 0.0);
        assertTrue(destination.width() > source.width());
        assertEquals((source.x() + destination.x()) / 2.0, overlay.x(), 0.001);
        assertEquals((source.y() + destination.y()) / 2.0, overlay.y(), 0.001);
        assertEquals((source.width() + destination.width()) / 2.0, overlay.width(), 0.001);
        assertEquals((source.height() + destination.height()) / 2.0, overlay.height(), 0.001);
        assertEquals("gallery", gallery.shared().key().namespace());
        assertEquals("hero", gallery.shared().key().id());
    }

    /// Distinguishes detach owner retention from remove-owned exit presentation.
    @Test
    void detachesRetainedOwnerWithoutDisposingPresentation() {
        LayoutTree tree = new LayoutTree();
        ControlGallery gallery = new ControlGallery();
        tree.setRoot(gallery.create(tree));
        tree.measure(Constraints.loose(400.0f, 1200.0f));
        tree.place();
        gallery.shared().overlay(0.0);
        gallery.shared().show(0L);
        gallery.shared().sample(1_000_000_000L);
        gallery.shared().detach(1_000_000_000L);
        assertEquals(TransitionLifetime.DETACHED, gallery.shared().lifetime());
        assertFalse(gallery.shared().ownerDisposed());
        assertNull(gallery.shared().retainedPresentation());
        gallery.shared().show(2_000_000_000L);
        gallery.shared().sample(3_000_000_000L);
        gallery.shared().remove(3_000_000_000L);
        assertEquals(TransitionLifetime.REMOVED, gallery.shared().lifetime());
        assertTrue(gallery.shared().ownerDisposed());
        assertNotNull(gallery.shared().retainedPresentation());
    }
}
