package org.glavo.himari.platform.windows;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Stores one UI Automation inspection node projected from semantics.
///
/// @param id the layout node identity
/// @param controlType the UIA control type name
/// @param name the accessible name
/// @param invokeSupported whether Invoke is exposed
/// @param focused whether the node is focused
/// @param x the bounding-rectangle origin x
/// @param y the bounding-rectangle origin y
/// @param width the bounding-rectangle width
/// @param height the bounding-rectangle height
/// @param toggleState the UIA toggle state, or `null` when the pattern is absent
/// @param rangeValue the UIA range value, or `null` when the pattern is absent
@NotNullByDefault
public record WindowsAutomationNode(
        long id,
        String controlType,
        String name,
        boolean invokeSupported,
        boolean focused,
        float x,
        float y,
        float width,
        float height,
        @Nullable String toggleState,
        @Nullable Double rangeValue
) {
    /// Validates the node.
    public WindowsAutomationNode {
        if (id <= 0L) {
            throw new IllegalArgumentException("id must be positive");
        }
        Objects.requireNonNull(controlType, "controlType");
        Objects.requireNonNull(name, "name");
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(width) || !Float.isFinite(height)
                || width < 0.0f || height < 0.0f) {
            throw new IllegalArgumentException("Bounding rectangle must be finite with nonnegative extents");
        }
    }
}
