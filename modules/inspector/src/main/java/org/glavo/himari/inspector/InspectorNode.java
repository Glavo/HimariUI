package org.glavo.himari.inspector;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one inspected layout or semantics node.
///
/// @param id the layout identity
/// @param name the diagnostic name
/// @param kind the layout kind name
/// @param phase the invalidation phase name (`MEASURE`, `PLACE`, or `NONE`)
/// @param clipKind the hit-clip kind name (`NONE`, `RECT`, `ROUNDED`, `OVAL`, or `PATH`)
/// @param role the semantics role name
/// @param label the accessible name
/// @param x the root-relative origin x
/// @param y the root-relative origin y
/// @param width the measured width
/// @param height the measured height
/// @param focused whether the node owns focus
/// @param liveRegion the live-region politeness name
/// @param textStart the inclusive selection start, or `-1` when absent
/// @param textEnd the exclusive selection end, or `-1` when absent
/// @param caret the caret offset, or `-1` when absent
/// @param rotation the clockwise rotation in degrees
/// @param translationX the x translation
/// @param translationY the y translation
/// @param shearX the horizontal shear factor
/// @param shearY the vertical shear factor
/// @param rangeMinimum the inclusive range minimum
/// @param rangeMaximum the inclusive range maximum
@NotNullByDefault
public record InspectorNode(
        long id,
        String name,
        String kind,
        String phase,
        String clipKind,
        String role,
        String label,
        float x,
        float y,
        float width,
        float height,
        boolean focused,
        String liveRegion,
        int textStart,
        int textEnd,
        int caret,
        float rotation,
        float translationX,
        float translationY,
        float shearX,
        float shearY,
        double rangeMinimum,
        double rangeMaximum
) {
    /// Validates the node.
    public InspectorNode {
        if (id <= 0L) {
            throw new IllegalArgumentException("id must be positive");
        }
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(clipKind, "clipKind");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(liveRegion, "liveRegion");
        if (!Double.isFinite(rangeMinimum) || !Double.isFinite(rangeMaximum)) {
            throw new IllegalArgumentException("range extent must be finite");
        }
        if (rangeMaximum < rangeMinimum) {
            throw new IllegalArgumentException("range maximum must be at least the minimum");
        }
    }
}
