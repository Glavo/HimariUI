package org.glavo.himari.runtime.transition;

import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.runtime.animation.SnapMotionSpec;
import org.glavo.himari.runtime.animation.SpringSpec;
import org.glavo.himari.runtime.animation.TweenSpec;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies enter/exit phases, lifetime distinctions, and matched-geometry pairing.
@NotNullByDefault
final class TransitionRegistryTest {
    /// Completes an enter tween at the declared duration.
    @Test
    void samplesEnterTweenToVisible() {
        VisibilityTransition transition = new VisibilityTransition(
                new TransitionIdentity("pane", "card"),
                "card",
                TransitionSpec.symmetric(TweenSpec.linear(1_000_000_000L), TransitionParticipation.LAYOUT)
        );
        transition.show(0L);
        assertEquals(TransitionPhase.ENTERING, transition.phase());
        assertEquals(0.0, transition.progress());
        assertTrue(transition.participatesInLayout());
        assertTrue(transition.inHitTest());
        assertFalse(transition.ownerDisposed());
        transition.sample(250_000_000L);
        assertEquals(0.25, transition.progress());
        assertEquals(TransitionPhase.VISIBLE, transition.sample(1_000_000_000L));
        assertEquals(1.0, transition.progress());
    }

    /// Reverses a mid-enter presentation toward gone.
    @Test
    void reversesMidEnterTowardGone() {
        VisibilityTransition transition = new VisibilityTransition(
                new TransitionIdentity("pane", "card"),
                "card",
                TransitionSpec.symmetric(TweenSpec.linear(1_000_000_000L), TransitionParticipation.OVERLAY)
        );
        transition.show(0L);
        transition.sample(400_000_000L);
        transition.reverse(400_000_000L);
        assertEquals(TransitionPhase.EXITING, transition.phase());
        assertEquals(TransitionLifetime.HIDDEN, transition.lifetime());
        assertFalse(transition.participatesInLayout());
        transition.sample(800_000_000L);
        assertEquals(0.0, transition.progress(), 1.0e-12);
        assertEquals(TransitionPhase.GONE, transition.phase());
    }

    /// Distinguishes hide, detach, and remove lifetimes.
    @Test
    void distinguishesHiddenDetachedAndRemoved() {
        TransitionSpec spec = TransitionSpec.symmetric(
                SnapMotionSpec.INSTANCE,
                TransitionParticipation.OVERLAY
        );
        VisibilityTransition hidden = new VisibilityTransition(
                new TransitionIdentity("pane", "hidden"),
                "hidden",
                spec
        );
        hidden.show(0L);
        hidden.hide(0L);
        assertEquals(TransitionPhase.GONE, hidden.phase());
        assertEquals(TransitionLifetime.HIDDEN, hidden.lifetime());
        assertFalse(hidden.ownerDisposed());
        assertNull(hidden.retainedPresentation());

        VisibilityTransition detached = new VisibilityTransition(
                new TransitionIdentity("pane", "detached"),
                "detached",
                spec
        );
        detached.show(0L);
        detached.detach(0L);
        assertEquals(TransitionLifetime.DETACHED, detached.lifetime());
        assertFalse(detached.ownerDisposed());

        VisibilityTransition removed = new VisibilityTransition(
                new TransitionIdentity("pane", "removed"),
                "removed",
                spec
        );
        removed.setBounds(new LogicalRect(1.0, 2.0, 8.0, 4.0));
        removed.show(0L);
        removed.remove(0L);
        assertEquals(TransitionLifetime.REMOVED, removed.lifetime());
        assertTrue(removed.ownerDisposed());
        assertFalse(removed.inSemantics());
        assertFalse(removed.inFocus());
        assertNull(removed.retainedPresentation());
    }

    /// Retains overlay presentation while a non-immediate removal is still exiting.
    @Test
    void retainsOverlayPresentationDuringRemoval() {
        VisibilityTransition transition = new VisibilityTransition(
                new TransitionIdentity("pane", "card"),
                "card",
                TransitionSpec.symmetric(TweenSpec.linear(1_000_000_000L), TransitionParticipation.OVERLAY)
        );
        transition.setBounds(new LogicalRect(0.0, 0.0, 10.0, 10.0));
        transition.show(0L);
        transition.sample(1_000_000_000L);
        transition.remove(1_000_000_000L);
        assertEquals(TransitionPhase.EXITING, transition.phase());
        assertTrue(transition.ownerDisposed());
        assertFalse(transition.inHitTest());
        assertFalse(transition.participatesInLayout());
        RetainedPresentation retained = transition.retainedPresentation();
        if (retained == null) {
            throw new AssertionError("Removal omitted the retained presentation");
        }
        assertEquals(new LogicalRect(0.0, 0.0, 10.0, 10.0), retained.bounds());
        assertTrue(retained.ownerDisposed());
        transition.sample(2_000_000_000L);
        assertEquals(TransitionPhase.GONE, transition.phase());
        assertNull(transition.retainedPresentation());
    }

    /// Retargets a hidden identity and replaces a removed one.
    @Test
    void retargetsHiddenAndReplacesRemoved() {
        TransitionRegistry registry = new TransitionRegistry();
        TransitionIdentity identity = new TransitionIdentity("pane", "card");
        TransitionSpec spec = TransitionSpec.symmetric(
                SnapMotionSpec.INSTANCE,
                TransitionParticipation.LAYOUT
        );
        VisibilityTransition first = registry.open(identity, "card", spec);
        first.show(0L);
        first.hide(0L);
        registry.beginFrame();
        VisibilityTransition hidden = registry.open(identity, "card", spec);
        assertSame(first, hidden);
        hidden.remove(0L);
        registry.beginFrame();
        VisibilityTransition replacement = registry.open(identity, "card", spec);
        assertNotSame(first, replacement);
        assertEquals(TransitionPhase.GONE, replacement.phase());
        assertTrue(replacement.ownerDisposed());
    }

    /// Records duplicate identity and geometry claims without picking a winner.
    @Test
    void diagnosesDuplicateIdentityAndGeometry() {
        TransitionRegistry registry = new TransitionRegistry();
        TransitionIdentity identity = new TransitionIdentity("pane", "card");
        TransitionSpec spec = TransitionSpec.symmetric(
                SnapMotionSpec.INSTANCE,
                TransitionParticipation.LAYOUT
        );
        registry.open(identity, "first", spec);
        registry.open(identity, "second", spec);
        assertTrue(registry.identityConflicts().contains(identity));
        MatchedGeometryKey key = new MatchedGeometryKey("shared", "hero");
        registry.captureSource(key, new LogicalRect(0.0, 0.0, 4.0, 4.0));
        registry.captureSource(key, new LogicalRect(8.0, 8.0, 4.0, 4.0));
        registry.captureDestination(key, new LogicalRect(10.0, 10.0, 8.0, 8.0));
        assertTrue(registry.sourceConflicts().contains(key));
        assertNull(registry.link(key));
    }

    /// Interpolates an accepted matched-geometry pair.
    @Test
    void interpolatesAcceptedMatchedGeometry() {
        TransitionRegistry registry = new TransitionRegistry();
        MatchedGeometryKey key = new MatchedGeometryKey("shared", "hero");
        registry.captureSource(key, new LogicalRect(0.0, 0.0, 10.0, 10.0));
        registry.captureDestination(key, new LogicalRect(10.0, 20.0, 20.0, 30.0));
        MatchedGeometryLink link = registry.link(key);
        if (link == null) {
            throw new AssertionError("Accepted geometry pair was omitted");
        }
        assertEquals(new LogicalRect(5.0, 10.0, 15.0, 20.0), link.interpolate(0.5));
    }

    /// Rejects spring motion in a transition specification.
    @Test
    void rejectsSpringTransitionSpec() {
        assertThrows(IllegalArgumentException.class, () -> new TransitionSpec(
                SpringSpec.DEFAULT,
                SnapMotionSpec.INSTANCE,
                TransitionParticipation.LAYOUT
        ));
    }
}
