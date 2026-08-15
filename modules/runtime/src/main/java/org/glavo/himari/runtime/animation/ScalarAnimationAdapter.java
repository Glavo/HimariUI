package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

/// Defines allocation-free normalization and equality for one scalar presentation property.
///
/// Values and velocities use the property's declared scalar unit; velocities are expressed per
/// second. Every accepted value must be finite. Normalization clamps only when the adapter declares
/// an explicit finite range.
///
/// @param minimumValue the inclusive finite minimum
/// @param maximumValue the inclusive finite maximum
/// @param equalityTolerance the nonnegative finite absolute equality tolerance
@NotNullByDefault
public record ScalarAnimationAdapter(
        double minimumValue,
        double maximumValue,
        double equalityTolerance
) {
    /// Preserves every finite scalar without clamping and compares exactly.
    public static final ScalarAnimationAdapter UNBOUNDED = new ScalarAnimationAdapter(
            -Double.MAX_VALUE,
            Double.MAX_VALUE,
            0.0
    );

    /// Clamps values to the inclusive unit interval and compares exactly.
    public static final ScalarAnimationAdapter UNIT_INTERVAL = new ScalarAnimationAdapter(
            0.0,
            1.0,
            0.0
    );

    /// Clamps values to the nonnegative finite range and compares exactly.
    public static final ScalarAnimationAdapter NON_NEGATIVE = new ScalarAnimationAdapter(
            0.0,
            Double.MAX_VALUE,
            0.0
    );

    /// Validates range and tolerance values.
    ///
    /// @throws IllegalArgumentException if a component is non-finite, the range is inverted, or the
    /// tolerance is negative
    public ScalarAnimationAdapter {
        requireFinite(minimumValue, "minimumValue");
        requireFinite(maximumValue, "maximumValue");
        requireFinite(equalityTolerance, "equalityTolerance");
        if (minimumValue > maximumValue) {
            throw new IllegalArgumentException("minimumValue must not exceed maximumValue");
        }
        if (equalityTolerance < 0.0) {
            throw new IllegalArgumentException("equalityTolerance must be non-negative");
        }
    }

    /// Validates and clamps a model or presentation value.
    ///
    /// @param value the candidate scalar
    /// @return the normalized scalar
    /// @throws IllegalArgumentException if `value` is not finite
    public double normalize(double value) {
        requireFinite(value, "value");
        return Math.clamp(value, minimumValue, maximumValue);
    }

    /// Validates a velocity without changing its unit or magnitude.
    ///
    /// @param velocity the candidate scalar-per-second velocity
    /// @return the unchanged velocity
    /// @throws IllegalArgumentException if `velocity` is not finite
    public double normalizeVelocity(double velocity) {
        requireFinite(velocity, "velocity");
        return velocity;
    }

    /// Returns whether two normalized scalars are semantically interchangeable.
    ///
    /// @param first the first normalized scalar
    /// @param second the second normalized scalar
    /// @return whether their absolute difference is within the declared tolerance
    public boolean equivalent(double first, double second) {
        return Math.abs(first - second) <= equalityTolerance;
    }

    /// Verifies that a scalar is finite.
    ///
    /// @param value the scalar
    /// @param name the diagnostic field name
    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
