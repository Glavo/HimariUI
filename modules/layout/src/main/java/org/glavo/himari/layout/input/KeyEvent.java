package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one normalized keyboard event.
///
/// @param type the event kind
/// @param key the logical key
@NotNullByDefault
public record KeyEvent(KeyEventType type, LogicalKey key) {
    /// Validates the event.
    public KeyEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(key, "key");
    }
}
