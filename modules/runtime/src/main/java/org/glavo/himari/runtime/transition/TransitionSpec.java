package org.glavo.himari.runtime.transition;

import org.glavo.himari.runtime.animation.MotionSpec;
import org.glavo.himari.runtime.animation.SnapMotionSpec;
import org.glavo.himari.runtime.animation.TweenSpec;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Declares asymmetric enter and exit motion plus exit layout participation.
///
/// @param enter the motion used while becoming visible
/// @param exit the motion used while becoming gone
/// @param exitParticipation whether an exiting presentation occupies layout or an overlay
@NotNullByDefault
public record TransitionSpec(
        MotionSpec enter,
        MotionSpec exit,
        TransitionParticipation exitParticipation
) {
    /// Validates tween-or-snap motion and a declared participation policy.
    public TransitionSpec {
        Objects.requireNonNull(enter, "enter");
        Objects.requireNonNull(exit, "exit");
        Objects.requireNonNull(exitParticipation, "exitParticipation");
        requireSupported(enter, "enter");
        requireSupported(exit, "exit");
    }

    /// Creates a spec whose enter and exit use the same motion.
    ///
    /// @param motion the enter and exit motion
    /// @param exitParticipation the exit participation
    /// @return the spec
    public static TransitionSpec symmetric(MotionSpec motion, TransitionParticipation exitParticipation) {
        return new TransitionSpec(motion, motion, exitParticipation);
    }

    /// Rejects spring or other unsupported transition motion.
    ///
    /// @param motion the candidate
    /// @param name the parameter name
    private static void requireSupported(MotionSpec motion, String name) {
        if (!(motion instanceof TweenSpec || motion instanceof SnapMotionSpec)) {
            throw new IllegalArgumentException(name + " must be a tween or snap specification");
        }
    }
}
