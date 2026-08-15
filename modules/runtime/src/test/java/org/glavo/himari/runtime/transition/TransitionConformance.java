package org.glavo.himari.runtime.transition;

import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.runtime.animation.SnapMotionSpec;
import org.glavo.himari.runtime.animation.TweenSpec;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes M9 structural-transition and matched-geometry evidence.
@NotNullByDefault
public final class TransitionConformance {
    /// Prevents instantiation.
    private TransitionConformance() {
    }

    /// Exercises enter/exit lifetimes, reversal, and matched geometry.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        TransitionSpec tween = TransitionSpec.symmetric(
                TweenSpec.linear(1_000_000_000L),
                TransitionParticipation.OVERLAY
        );
        VisibilityTransition enter = new VisibilityTransition(
                new TransitionIdentity("pane", "enter"),
                "enter",
                tween
        );
        enter.show(0L);
        enter.sample(250_000_000L);
        if (enter.phase() != TransitionPhase.ENTERING || enter.progress() != 0.25 || !enter.inHitTest()) {
            throw new IllegalStateException("Enter tween did not publish quarter progress");
        }
        enter.sample(1_000_000_000L);
        if (enter.phase() != TransitionPhase.VISIBLE || enter.progress() != 1.0) {
            throw new IllegalStateException("Enter tween did not complete");
        }
        enter.setBounds(new LogicalRect(0.0, 0.0, 10.0, 10.0));
        enter.remove(1_000_000_000L);
        if (enter.phase() != TransitionPhase.EXITING
                || !enter.ownerDisposed()
                || enter.inSemantics()
                || enter.participatesInLayout()
                || enter.retainedPresentation() == null) {
            throw new IllegalStateException("Removal did not retain an overlay presentation");
        }
        enter.sample(2_000_000_000L);
        if (enter.phase() != TransitionPhase.GONE || enter.retainedPresentation() != null) {
            throw new IllegalStateException("Exit tween did not drop the retained presentation");
        }

        VisibilityTransition reverse = new VisibilityTransition(
                new TransitionIdentity("pane", "reverse"),
                "reverse",
                tween
        );
        reverse.show(0L);
        reverse.sample(400_000_000L);
        reverse.reverse(400_000_000L);
        reverse.sample(800_000_000L);
        if (reverse.phase() != TransitionPhase.GONE || reverse.lifetime() != TransitionLifetime.HIDDEN) {
            throw new IllegalStateException("Reversed enter did not hide the presentation");
        }

        TransitionSpec snap = TransitionSpec.symmetric(
                SnapMotionSpec.INSTANCE,
                TransitionParticipation.LAYOUT
        );
        VisibilityTransition hidden = new VisibilityTransition(
                new TransitionIdentity("pane", "hidden"),
                "hidden",
                snap
        );
        hidden.show(0L);
        hidden.hide(0L);
        if (hidden.ownerDisposed() || hidden.lifetime() != TransitionLifetime.HIDDEN) {
            throw new IllegalStateException("Hide disposed the owner");
        }
        VisibilityTransition detached = new VisibilityTransition(
                new TransitionIdentity("pane", "detached"),
                "detached",
                snap
        );
        detached.show(0L);
        detached.detach(0L);
        if (detached.ownerDisposed() || detached.lifetime() != TransitionLifetime.DETACHED) {
            throw new IllegalStateException("Detach disposed the owner");
        }

        TransitionRegistry registry = new TransitionRegistry();
        TransitionIdentity identity = new TransitionIdentity("pane", "card");
        VisibilityTransition first = registry.open(identity, "card", snap);
        first.show(0L);
        first.hide(0L);
        registry.beginFrame();
        if (registry.open(identity, "card", snap) != first) {
            throw new IllegalStateException("Hidden identity was not retargeted");
        }
        first.remove(0L);
        registry.beginFrame();
        if (registry.open(identity, "card", snap) == first) {
            throw new IllegalStateException("Removed identity was not replaced");
        }
        registry.beginFrame();
        registry.open(identity, "first", snap);
        registry.open(identity, "second", snap);
        MatchedGeometryKey key = new MatchedGeometryKey("shared", "hero");
        registry.captureSource(key, new LogicalRect(0.0, 0.0, 10.0, 10.0));
        registry.captureDestination(key, new LogicalRect(10.0, 20.0, 20.0, 30.0));
        MatchedGeometryLink link = registry.link(key);
        if (link == null || !link.interpolate(0.5).equals(new LogicalRect(5.0, 10.0, 15.0, 20.0))) {
            throw new IllegalStateException("Matched geometry did not interpolate");
        }
        registry.captureSource(key, new LogicalRect(1.0, 1.0, 1.0, 1.0));
        if (registry.link(key) != null || !registry.sourceConflicts().contains(key)
                || !registry.identityConflicts().contains(identity)) {
            throw new IllegalStateException("Duplicate identity or geometry was not diagnosed");
        }

        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m9-transition",
                          "workPackage": "TRANSITION-001",
                          "status": "passed",
                          "enterComplete": true,
                          "exitRetained": true,
                          "reversed": true,
                          "hiddenRetainedOwner": true,
                          "detachedRetainedOwner": true,
                          "removedDisposedOwner": true,
                          "retargetedHidden": true,
                          "replacedRemoved": true,
                          "matchedGeometry": true,
                          "duplicateDiagnosed": true
                        }
                        """,
                StandardCharsets.UTF_8
        );
    }
}
