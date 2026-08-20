package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

/// Receives one pointer event during target-to-bubble routing.
@FunctionalInterface
@NotNullByDefault
public interface PointerListener {
    /// Handles one event on the current node.
    ///
    /// Returning `true` stops further bubbling toward the root.
    ///
    /// @param event the routed event
    /// @return whether this listener consumed the event
    boolean onPointer(PointerEvent event);
}
