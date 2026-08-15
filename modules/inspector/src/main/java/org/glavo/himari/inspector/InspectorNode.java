package org.glavo.himari.inspector;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one inspected layout or semantics node.
///
/// @param id the layout identity
/// @param name the diagnostic name
/// @param role the semantics role name
/// @param label the accessible name
/// @param x the root-relative origin x
/// @param y the root-relative origin y
/// @param width the measured width
/// @param height the measured height
/// @param focused whether the node owns focus
/// @param liveRegion the live-region politeness name
@NotNullByDefault
public record InspectorNode(
        long id,
        String name,
        String role,
        String label,
        float x,
        float y,
        float width,
        float height,
        boolean focused,
        String liveRegion
) {
    /// Validates the node.
    public InspectorNode {
        if (id <= 0L) {
            throw new IllegalArgumentException("id must be positive");
        }
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(liveRegion, "liveRegion");
    }
}
