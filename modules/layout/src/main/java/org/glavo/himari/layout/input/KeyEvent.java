package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one normalized keyboard event.
///
/// @param type the event kind
/// @param key the logical key
/// @param shift whether a shift modifier was latched
/// @param ctrl whether a control modifier was latched
/// @param alt whether an alt modifier was latched
@NotNullByDefault
public record KeyEvent(KeyEventType type, LogicalKey key, boolean shift, boolean ctrl, boolean alt) {
    /// Validates the event.
    public KeyEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(key, "key");
    }

    /// Creates an event without modifiers.
    ///
    /// @param type the event kind
    /// @param key the logical key
    public KeyEvent(KeyEventType type, LogicalKey key) {
        this(type, key, false, false, false);
    }

    /// Creates an event with only a shift modifier.
    ///
    /// @param type the event kind
    /// @param key the logical key
    /// @param shift whether a shift modifier was latched
    public KeyEvent(KeyEventType type, LogicalKey key, boolean shift) {
        this(type, key, shift, false, false);
    }
}
