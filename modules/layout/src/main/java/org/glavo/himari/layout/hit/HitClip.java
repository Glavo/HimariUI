package org.glavo.himari.layout.hit;

import org.glavo.himari.layout.LayoutRect;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Shape used after the bounding-box reject in hit testing.
@NotNullByDefault
public final class HitClip {
    /// Identifies the clip geometry.
    public enum Kind {
        /// Axis-aligned rectangle.
        RECT,

        /// Rounded rectangle.
        ROUNDED,

        /// Ellipse inscribed in the bounds.
        OVAL,

        /// Closed polygon in local coordinates.
        PATH
    }

    /// Root-relative bounds of the clip.
    private final LayoutRect bounds;

    /// Geometry kind.
    private final Kind kind;

    /// Corner radius for [`Kind#ROUNDED`]; unused otherwise.
    private final float radius;

    /// Local `x,y` pairs for [`Kind#PATH`]; empty otherwise.
    private final float @Unmodifiable [] localPoints;

    /// Creates a validated clip.
    ///
    /// @param bounds the root-relative rectangle
    /// @param kind the geometry
    /// @param radius the rounded-rect radius
    /// @param localPoints path vertices, or empty
    private HitClip(LayoutRect bounds, Kind kind, float radius, float[] localPoints) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.radius = radius;
        this.localPoints = Arrays.copyOf(localPoints, localPoints.length);
    }

    /// Creates a rectangular clip.
    ///
    /// @param bounds the root-relative rectangle
    /// @return the clip
    public static HitClip rect(LayoutRect bounds) {
        return new HitClip(bounds, Kind.RECT, 0.0f, new float[0]);
    }

    /// Creates a rounded-rectangle clip.
    ///
    /// @param bounds the root-relative rectangle
    /// @param radius the nonnegative corner radius
    /// @return the clip
    public static HitClip rounded(LayoutRect bounds, float radius) {
        if (!Float.isFinite(radius) || radius < 0.0f) {
            throw new IllegalArgumentException("Corner radius must be finite and nonnegative");
        }
        return new HitClip(bounds, Kind.ROUNDED, radius, new float[0]);
    }

    /// Creates an oval clip inscribed in `bounds`.
    ///
    /// @param bounds the root-relative rectangle
    /// @return the clip
    public static HitClip oval(LayoutRect bounds) {
        return new HitClip(bounds, Kind.OVAL, 0.0f, new float[0]);
    }

    /// Creates a polygon clip whose vertices are local to `bounds` origin.
    ///
    /// @param bounds the root-relative rectangle
    /// @param localPoints even-length `x,y` pairs, at least three vertices
    /// @return the clip
    public static HitClip path(LayoutRect bounds, float[] localPoints) {
        Objects.requireNonNull(localPoints, "localPoints");
        if (localPoints.length < 6 || (localPoints.length & 1) != 0) {
            throw new IllegalArgumentException("Path clip requires at least three x,y vertices");
        }
        for (float value : localPoints) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Path clip vertices must be finite");
            }
        }
        return new HitClip(bounds, Kind.PATH, 0.0f, localPoints);
    }

    /// Returns the clip geometry kind.
    ///
    /// @return the kind
    public Kind kind() {
        return kind;
    }

    /// Returns whether the root-relative point lies inside this clip.
    ///
    /// The bounding box is rejected first. Rounded, oval, and path clips then
    /// apply their shape policy.
    ///
    /// @param x the root-relative x
    /// @param y the root-relative y
    /// @return whether the point is inside
    public boolean contains(float x, float y) {
        if (!bounds.contains(x, y)) {
            return false;
        }
        return switch (kind) {
            case RECT -> true;
            case ROUNDED -> containsRounded(x, y);
            case OVAL -> containsOval(x, y);
            case PATH -> containsPath(x, y);
        };
    }

    /// Tests the rounded-rectangle corner circles.
    ///
    /// @param x the root-relative x
    /// @param y the root-relative y
    /// @return whether the point is inside the rounded rectangle
    private boolean containsRounded(float x, float y) {
        float radius = Math.min(this.radius, Math.min(bounds.width(), bounds.height()) * 0.5f);
        if (radius <= 0.0f) {
            return true;
        }
        float left = bounds.x();
        float top = bounds.y();
        float right = left + bounds.width();
        float bottom = top + bounds.height();
        if (x >= left + radius && x < right - radius) {
            return true;
        }
        if (y >= top + radius && y < bottom - radius) {
            return true;
        }
        float cx = x < left + radius ? left + radius : right - radius;
        float cy = y < top + radius ? top + radius : bottom - radius;
        float dx = x - cx;
        float dy = y - cy;
        return Math.fma(dx, dx, dy * dy) <= radius * radius;
    }

    /// Tests the inscribed ellipse.
    ///
    /// @param x the root-relative x
    /// @param y the root-relative y
    /// @return whether the point is inside the oval
    private boolean containsOval(float x, float y) {
        float rx = bounds.width() * 0.5f;
        float ry = bounds.height() * 0.5f;
        if (rx <= 0.0f || ry <= 0.0f) {
            return false;
        }
        float nx = (x - (bounds.x() + rx)) / rx;
        float ny = (y - (bounds.y() + ry)) / ry;
        return Math.fma(nx, nx, ny * ny) <= 1.0f;
    }

    /// Even-odd point-in-polygon test in local coordinates.
    ///
    /// @param x the root-relative x
    /// @param y the root-relative y
    /// @return whether the point is inside the polygon
    private boolean containsPath(float x, float y) {
        float localX = x - bounds.x();
        float localY = y - bounds.y();
        int vertices = localPoints.length / 2;
        boolean inside = false;
        int previous = vertices - 1;
        for (int index = 0; index < vertices; index++) {
            float x1 = localPoints[previous * 2];
            float y1 = localPoints[previous * 2 + 1];
            float x2 = localPoints[index * 2];
            float y2 = localPoints[index * 2 + 1];
            boolean straddles = (y2 > localY) != (y1 > localY);
            if (straddles) {
                float atX = Math.fma((localY - y2) / (y1 - y2), x1 - x2, x2);
                if (localX < atX) {
                    inside = !inside;
                }
            }
            previous = index;
        }
        return inside;
    }
}
