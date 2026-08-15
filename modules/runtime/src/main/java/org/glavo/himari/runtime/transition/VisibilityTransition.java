package org.glavo.himari.runtime.transition;

import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.runtime.animation.MotionSpec;
import org.glavo.himari.runtime.animation.TweenSpec;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Drives one element's enter, exit, and visibility state machine.
///
/// Time is taken from caller-supplied timestamps so Headless tests can replay traces without
/// sleeping. Remaining distance scales a tween's active duration so a reverse from mid-progress
/// completes in the remaining time. Removal disposes the element's owner immediately and retains
/// only presentation data needed to draw the exit. Hidden and detached lifetimes keep that owner.
@NotNullByDefault
public final class VisibilityTransition {
    /// Structural identity used to retarget this presentation.
    private final TransitionIdentity identity;

    /// Diagnostic name.
    private final String debugName;

    /// Enter, exit, and participation policy.
    private TransitionSpec spec;

    /// Current presentation phase.
    private TransitionPhase phase = TransitionPhase.GONE;

    /// Current element lifetime.
    private TransitionLifetime lifetime = TransitionLifetime.REMOVED;

    /// Whether the element's owner has been disposed.
    private boolean ownerDisposed = true;

    /// Presentation progress from gone (`0`) to visible (`1`).
    private double progress;

    /// Progress at the start of the active motion.
    private double startProgress;

    /// Progress at the end of the active motion.
    private double endProgress;

    /// Timestamp at which the active motion began.
    private long startNanos;

    /// Active motion, or `null` when idle.
    private @Nullable MotionSpec activeMotion;

    /// Last committed presentation bounds, or `null` before the first capture.
    private @Nullable LogicalRect lastBounds;

    /// Exit presentation retained after removal, or `null`.
    private @Nullable RetainedPresentation retained;

    /// Creates a gone, removed transition with a disposed owner.
    ///
    /// @param identity the structural identity
    /// @param debugName the diagnostic name
    /// @param spec the enter and exit specification
    public VisibilityTransition(TransitionIdentity identity, String debugName, TransitionSpec spec) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.debugName = Objects.requireNonNull(debugName, "debugName");
        this.spec = Objects.requireNonNull(spec, "spec");
    }

    /// Returns the structural identity.
    ///
    /// @return the identity
    public TransitionIdentity identity() {
        return identity;
    }

    /// Returns the diagnostic name.
    ///
    /// @return the name
    public String debugName() {
        return debugName;
    }

    /// Returns the current specification.
    ///
    /// @return the spec
    public TransitionSpec spec() {
        return spec;
    }

    /// Replaces the enter and exit specification for later motion.
    ///
    /// @param spec the spec
    public void setSpec(TransitionSpec spec) {
        this.spec = Objects.requireNonNull(spec, "spec");
    }

    /// Returns the current phase.
    ///
    /// @return the phase
    public TransitionPhase phase() {
        return phase;
    }

    /// Returns the current lifetime.
    ///
    /// @return the lifetime
    public TransitionLifetime lifetime() {
        return lifetime;
    }

    /// Returns presentation progress in `[0, 1]`.
    ///
    /// @return the progress
    public double progress() {
        return progress;
    }

    /// Returns whether the element's owner is disposed.
    ///
    /// @return whether the owner is gone
    public boolean ownerDisposed() {
        return ownerDisposed;
    }

    /// Returns the last committed bounds, or `null`.
    ///
    /// @return the bounds
    public @Nullable LogicalRect bounds() {
        return lastBounds;
    }

    /// Returns the retained exit presentation, or `null`.
    ///
    /// @return the retained presentation
    public @Nullable RetainedPresentation retainedPresentation() {
        return retained;
    }

    /// Publishes the latest laid-out bounds.
    ///
    /// @param bounds the bounds
    public void setBounds(LogicalRect bounds) {
        this.lastBounds = Objects.requireNonNull(bounds, "bounds");
    }

    /// Returns whether this presentation occupies layout space.
    ///
    /// @return whether layout includes the presentation
    public boolean participatesInLayout() {
        return switch (phase) {
            case ENTERING, VISIBLE -> true;
            case EXITING -> spec.exitParticipation() == TransitionParticipation.LAYOUT;
            case GONE -> false;
        };
    }

    /// Returns whether the live element remains in hit testing.
    ///
    /// @return whether hit testing includes the element
    public boolean inHitTest() {
        return interactive();
    }

    /// Returns whether the live element remains in semantics.
    ///
    /// @return whether semantics includes the element
    public boolean inSemantics() {
        return interactive();
    }

    /// Returns whether the live element remains focusable.
    ///
    /// @return whether focus includes the element
    public boolean inFocus() {
        return interactive();
    }

    /// Begins or continues becoming visible and remounts a disposed owner.
    ///
    /// @param nowNanos the monotonic timestamp
    public void show(long nowNanos) {
        ownerDisposed = false;
        lifetime = TransitionLifetime.MOUNTED;
        retained = null;
        if (phase == TransitionPhase.VISIBLE || phase == TransitionPhase.ENTERING) {
            return;
        }
        begin(nowNanos, 1.0, spec.enter(), TransitionPhase.ENTERING);
    }

    /// Hides the element while retaining local state.
    ///
    /// @param nowNanos the monotonic timestamp
    public void hide(long nowNanos) {
        beginExit(nowNanos, TransitionLifetime.HIDDEN, false);
    }

    /// Detaches the element while retaining local state.
    ///
    /// @param nowNanos the monotonic timestamp
    public void detach(long nowNanos) {
        beginExit(nowNanos, TransitionLifetime.DETACHED, false);
    }

    /// Removes the element, disposes its owner, and retains only exit presentation.
    ///
    /// @param nowNanos the monotonic timestamp
    public void remove(long nowNanos) {
        beginExit(nowNanos, TransitionLifetime.REMOVED, true);
    }

    /// Reverses the active motion toward the opposite terminal phase.
    ///
    /// @param nowNanos the monotonic timestamp
    public void reverse(long nowNanos) {
        if (phase == TransitionPhase.ENTERING) {
            beginExit(nowNanos, TransitionLifetime.HIDDEN, false);
            return;
        }
        if (phase == TransitionPhase.EXITING) {
            show(nowNanos);
        }
    }

    /// Samples presentation progress at one timestamp and applies terminal phase changes.
    ///
    /// @param nowNanos the monotonic timestamp
    /// @return the phase after sampling
    public TransitionPhase sample(long nowNanos) {
        if (activeMotion == null) {
            return phase;
        }
        progress = sampleMotion(activeMotion, nowNanos);
        if (progress == endProgress) {
            activeMotion = null;
            if (endProgress == 1.0) {
                phase = TransitionPhase.VISIBLE;
                lifetime = TransitionLifetime.MOUNTED;
                retained = null;
            } else {
                phase = TransitionPhase.GONE;
                progress = 0.0;
                if (lifetime == TransitionLifetime.REMOVED) {
                    retained = null;
                }
            }
        }
        return phase;
    }

    /// Starts an exit toward gone for the requested lifetime.
    ///
    /// @param nowNanos the timestamp
    /// @param nextLifetime the lifetime after the command
    /// @param disposeOwner whether to dispose the owner immediately
    private void beginExit(long nowNanos, TransitionLifetime nextLifetime, boolean disposeOwner) {
        lifetime = nextLifetime;
        if (disposeOwner) {
            ownerDisposed = true;
            retained = lastBounds == null
                    ? null
                    : new RetainedPresentation(
                            identity,
                            debugName,
                            lastBounds,
                            spec.exitParticipation(),
                            true
                    );
        } else {
            retained = null;
        }
        if (phase == TransitionPhase.GONE || phase == TransitionPhase.EXITING) {
            if (phase == TransitionPhase.GONE) {
                progress = 0.0;
                activeMotion = null;
            }
            return;
        }
        begin(nowNanos, 0.0, spec.exit(), TransitionPhase.EXITING);
    }

    /// Starts motion from the current progress toward `target`.
    ///
    /// @param nowNanos the timestamp
    /// @param target the terminal progress
    /// @param motion the motion
    /// @param nextPhase the phase while motion is active
    private void begin(long nowNanos, double target, MotionSpec motion, TransitionPhase nextPhase) {
        startProgress = progress;
        endProgress = target;
        startNanos = nowNanos;
        activeMotion = motion;
        phase = nextPhase;
        if (motion.isImmediate()) {
            progress = target;
            sample(nowNanos);
        }
    }

    /// Evaluates the active motion at `nowNanos`.
    ///
    /// @param motion the motion
    /// @param nowNanos the timestamp
    /// @return the progress
    private double sampleMotion(MotionSpec motion, long nowNanos) {
        if (motion.isImmediate()) {
            return endProgress;
        }
        if (!(motion instanceof TweenSpec tween)) {
            throw new IllegalStateException("Active transition motion must be a tween or snap");
        }
        long elapsed = nowNanos - startNanos;
        if (elapsed <= tween.delayNanos()) {
            return startProgress;
        }
        double span = Math.abs(endProgress - startProgress);
        if (span == 0.0) {
            return endProgress;
        }
        long duration = Math.max(1L, Math.round(tween.durationNanos() * span));
        long active = elapsed - tween.delayNanos();
        if (active >= duration) {
            return endProgress;
        }
        double unit = (double) active / (double) duration;
        double curved = tween.curve().value(unit);
        return startProgress + (endProgress - startProgress) * curved;
    }

    /// Returns whether the live element remains interactive.
    ///
    /// @return whether the element is mounted and not exiting or gone
    private boolean interactive() {
        return !ownerDisposed
                && (phase == TransitionPhase.ENTERING || phase == TransitionPhase.VISIBLE);
    }
}
