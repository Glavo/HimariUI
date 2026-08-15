package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;

/// Defines a monotonic-time cubic Bézier unit curve with finite endpoint slopes.
///
/// @param x1 the first control point's horizontal coordinate in `[0, 1]`
/// @param y1 the first control point's finite vertical coordinate
/// @param x2 the second control point's horizontal coordinate in `[0, 1]`
/// @param y2 the second control point's finite vertical coordinate
@NotNullByDefault
public record CubicBezierCurve(double x1, double y1, double x2, double y2) {
    /// The identity unit curve.
    public static final CubicBezierCurve LINEAR = new CubicBezierCurve(0.0, 0.0, 1.0, 1.0);

    /// A symmetric standard ease-in-out curve.
    public static final CubicBezierCurve EASE_IN_OUT =
            new CubicBezierCurve(0.42, 0.0, 0.58, 1.0);

    /// A standard accelerating ease-in curve.
    public static final CubicBezierCurve EASE_IN =
            new CubicBezierCurve(0.42, 0.0, 1.0, 1.0);

    /// A standard decelerating ease-out curve.
    public static final CubicBezierCurve EASE_OUT =
            new CubicBezierCurve(0.0, 0.0, 0.58, 1.0);

    /// Validates monotonic time and finite-slope endpoint constraints.
    ///
    /// @throws IllegalArgumentException if a coordinate is non-finite, a horizontal control lies
    /// outside `[0, 1]`, or an endpoint slope is not representable as a finite `double`
    public CubicBezierCurve {
        requireFinite(x1, "x1");
        requireFinite(y1, "y1");
        requireFinite(x2, "x2");
        requireFinite(y2, "y2");
        if (x1 < 0.0 || x1 > 1.0 || x2 < 0.0 || x2 > 1.0) {
            throw new IllegalArgumentException("Cubic Bézier time controls must be within [0, 1]");
        }
        if (!Double.isFinite(y2 - y1)
                || !Double.isFinite(startSlope(x1, y1, x2, y2))
                || !Double.isFinite(endSlope(x1, y1, x2, y2))) {
            throw new IllegalArgumentException(
                    "A cubic Bézier curve must have finite representable slopes"
            );
        }
    }

    /// Returns the curve value at a unit time progress.
    ///
    /// @param progress the finite progress in `[0, 1]`
    /// @return the corresponding curve value
    /// @throws IllegalArgumentException if `progress` is non-finite or outside `[0, 1]`
    public double value(double progress) {
        validateProgress(progress);
        if (isIdentity()) {
            return progress;
        }
        double parameter = solveParameter(progress);
        return coordinate(parameter, y1, y2);
    }

    /// Returns the curve derivative with respect to unit time progress.
    ///
    /// @param progress the finite progress in `[0, 1]`
    /// @return the finite value-per-progress slope
    /// @throws IllegalArgumentException if `progress` is non-finite or outside `[0, 1]`
    /// @throws IllegalStateException if an interior slope is not representable as a finite `double`
    public double slope(double progress) {
        validateProgress(progress);
        if (isIdentity()) {
            return 1.0;
        }
        if (progress == 0.0) {
            return startSlope(x1, y1, x2, y2);
        }
        if (progress == 1.0) {
            return endSlope(x1, y1, x2, y2);
        }
        double parameter = solveParameter(progress);
        double horizontalDerivative = derivativeBasis(parameter, x1, x2);
        double verticalDerivative = derivativeBasis(parameter, y1, y2);
        double result = verticalDerivative / horizontalDerivative;
        if (!Double.isFinite(result)) {
            throw new IllegalStateException("Cubic Bézier slope became non-finite");
        }
        return result;
    }

    /// Returns whether both curve coordinates are identical polynomials.
    ///
    /// @return whether this is an identity curve
    private boolean isIdentity() {
        return Double.doubleToLongBits(x1) == Double.doubleToLongBits(y1)
                && Double.doubleToLongBits(x2) == Double.doubleToLongBits(y2);
    }

    /// Solves the monotonic horizontal polynomial for a unit progress.
    ///
    /// @param progress the horizontal coordinate
    /// @return the polynomial parameter
    private double solveParameter(double progress) {
        double parameter = progress;
        for (int iteration = 0; iteration < 8; iteration++) {
            double difference = coordinate(parameter, x1, x2) - progress;
            if (Math.abs(difference) <= 1.0e-12) {
                return parameter;
            }
            double slope = derivative(parameter, x1, x2);
            if (Math.abs(slope) <= 1.0e-12) {
                break;
            }
            double candidate = parameter - difference / slope;
            if (candidate <= 0.0 || candidate >= 1.0) {
                break;
            }
            parameter = candidate;
        }

        double lower = 0.0;
        double upper = 1.0;
        for (int iteration = 0; iteration < 48; iteration++) {
            parameter = (lower + upper) * 0.5;
            double coordinate = coordinate(parameter, x1, x2);
            if (coordinate < progress) {
                lower = parameter;
            } else {
                upper = parameter;
            }
        }
        return (lower + upper) * 0.5;
    }

    /// Evaluates a unit-endpoint cubic Bézier coordinate.
    ///
    /// @param parameter the polynomial parameter
    /// @param first the first control coordinate
    /// @param second the second control coordinate
    /// @return the coordinate value
    private static double coordinate(double parameter, double first, double second) {
        double inverse = 1.0 - parameter;
        return 3.0 * inverse * inverse * parameter * first
                + 3.0 * inverse * parameter * parameter * second
                + parameter * parameter * parameter;
    }

    /// Evaluates the parameter derivative of a unit-endpoint cubic Bézier coordinate.
    ///
    /// @param parameter the polynomial parameter
    /// @param first the first control coordinate
    /// @param second the second control coordinate
    /// @return the coordinate derivative
    private static double derivative(double parameter, double first, double second) {
        return 3.0 * derivativeBasis(parameter, first, second);
    }

    /// Evaluates a cubic Bézier derivative without its common factor of three.
    ///
    /// Omitting the common factor prevents an avoidable overflow when only a derivative ratio is
    /// required.
    ///
    /// @param parameter the polynomial parameter
    /// @param first the first control coordinate
    /// @param second the second control coordinate
    /// @return one third of the coordinate derivative
    private static double derivativeBasis(double parameter, double first, double second) {
        double inverse = 1.0 - parameter;
        return inverse * inverse * first
                + 2.0 * inverse * parameter * (second - first)
                + parameter * parameter * (1.0 - second);
    }

    /// Returns the first nonzero derivative ratio at the start endpoint.
    ///
    /// @param x1 the first horizontal control
    /// @param y1 the first vertical control
    /// @param x2 the second horizontal control
    /// @param y2 the second vertical control
    /// @return the start slope, which may be non-finite before constructor validation
    private static double startSlope(double x1, double y1, double x2, double y2) {
        if (x1 != 0.0) {
            return y1 / x1;
        }
        if (y1 != 0.0) {
            return Double.NaN;
        }
        if (x2 != 0.0) {
            return y2 / x2;
        }
        return y2 == 0.0 ? 1.0 : Double.NaN;
    }

    /// Returns the first nonzero derivative ratio at the end endpoint.
    ///
    /// @param x1 the first horizontal control
    /// @param y1 the first vertical control
    /// @param x2 the second horizontal control
    /// @param y2 the second vertical control
    /// @return the end slope, which may be non-finite before constructor validation
    private static double endSlope(double x1, double y1, double x2, double y2) {
        if (x2 != 1.0) {
            return (1.0 - y2) / (1.0 - x2);
        }
        if (y2 != 1.0) {
            return Double.NaN;
        }
        if (x1 != 1.0) {
            return (1.0 - y1) / (1.0 - x1);
        }
        return y1 == 1.0 ? 1.0 : Double.NaN;
    }

    /// Validates a unit progress value.
    ///
    /// @param progress the candidate progress
    private static void validateProgress(double progress) {
        requireFinite(progress, "progress");
        if (progress < 0.0 || progress > 1.0) {
            throw new IllegalArgumentException("progress must be within [0, 1]");
        }
    }

    /// Validates one finite coordinate.
    ///
    /// @param value the coordinate
    /// @param name the diagnostic name
    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
