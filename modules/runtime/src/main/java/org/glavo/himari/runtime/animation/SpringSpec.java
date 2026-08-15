package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

/// Evolves a scalar according to an analytic damped harmonic oscillator.
///
/// Scalar displacement uses the property's unit and velocity uses that unit per second.
/// Completion requires both thresholds or the hard maximum duration, at which point the target is
/// published exactly.
///
/// @param mass the positive finite mass
/// @param stiffness the positive finite stiffness
/// @param damping the nonnegative finite damping coefficient
/// @param displacementThreshold the nonnegative finite settling displacement
/// @param velocityThreshold the nonnegative finite settling velocity per second
/// @param maximumDurationNanos the positive hard duration bound
@NotNullByDefault
public record SpringSpec(
        double mass,
        double stiffness,
        double damping,
        double displacementThreshold,
        double velocityThreshold,
        long maximumDurationNanos
) implements MotionSpec {
    /// A bounded, moderately damped reference spring in unit scalar coordinates.
    public static final SpringSpec DEFAULT = new SpringSpec(
            1.0,
            170.0,
            26.0,
            1.0e-4,
            1.0e-4,
            10_000_000_000L
    );

    /// Validates physical parameters, settling bounds, and derived reference coefficients.
    ///
    /// @throws IllegalArgumentException if a parameter violates its declared range or the natural
    /// frequency, damping ratio, or overdamped decay rate is not representable as a finite `double`
    public SpringSpec {
        requireFinite(mass, "mass");
        requireFinite(stiffness, "stiffness");
        requireFinite(damping, "damping");
        requireFinite(displacementThreshold, "displacementThreshold");
        requireFinite(velocityThreshold, "velocityThreshold");
        if (mass <= 0.0 || stiffness <= 0.0) {
            throw new IllegalArgumentException("Spring mass and stiffness must be positive");
        }
        if (damping < 0.0 || displacementThreshold < 0.0 || velocityThreshold < 0.0) {
            throw new IllegalArgumentException("Spring damping and thresholds must be non-negative");
        }
        if (maximumDurationNanos <= 0L) {
            throw new IllegalArgumentException("maximumDurationNanos must be positive");
        }
        double frequencySquared = stiffness / mass;
        double stiffnessMass = stiffness * mass;
        if (!Double.isFinite(frequencySquared)
                || frequencySquared <= 0.0
                || !Double.isFinite(stiffnessMass)
                || stiffnessMass <= 0.0) {
            throw new IllegalArgumentException("Spring frequency coefficients must remain finite");
        }
        double naturalFrequency = StrictMath.sqrt(frequencySquared);
        double criticalDamping = 2.0 * StrictMath.sqrt(stiffnessMass);
        double dampingRatio = damping / criticalDamping;
        if (!Double.isFinite(criticalDamping)
                || !Double.isFinite(dampingRatio)
                || !Double.isFinite(dampingRatio * dampingRatio)
                || !Double.isFinite(dampingRatio * naturalFrequency)) {
            throw new IllegalArgumentException("Spring damping coefficients must remain finite");
        }
        if (dampingRatio > 1.0) {
            double root = StrictMath.sqrt(dampingRatio * dampingRatio - 1.0);
            if (!Double.isFinite(naturalFrequency * (dampingRatio + root))) {
                throw new IllegalArgumentException("Spring overdamped decay rate must remain finite");
            }
        }
    }

    /// Returns that a spring has an active physical timeline.
    ///
    /// @return `false`
    @Override
    public boolean isImmediate() {
        return false;
    }

    /// Returns that compatible spring replacement preserves incoming velocity.
    ///
    /// @return `true`
    @Override
    public boolean supportsVelocityRetargeting() {
        return true;
    }

    /// Validates one finite physical scalar.
    ///
    /// @param value the candidate value
    /// @param name the diagnostic field name
    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
