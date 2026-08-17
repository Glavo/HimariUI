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
/// @param scanCode the physical OEM scan code; `0` when the host does not report one
/// @param repeat whether this is an auto-repeat `KEYDOWN` while the key was already down
/// @param extended whether Win32 `KF_EXTENDED` (lParam bit 24) was set
/// @param meta whether a Windows / Super modifier was latched
@NotNullByDefault
public record KeyEvent(
        KeyEventType type,
        LogicalKey key,
        boolean shift,
        boolean ctrl,
        boolean alt,
        int scanCode,
        boolean repeat,
        boolean extended,
        boolean meta
) {
    /// Validates the event.
    public KeyEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(key, "key");
        if (scanCode < 0) {
            throw new IllegalArgumentException("scanCode must be non-negative");
        }
    }

    /// Creates an event without modifiers.
    ///
    /// @param type the event kind
    /// @param key the logical key
    public KeyEvent(KeyEventType type, LogicalKey key) {
        this(type, key, false, false, false, 0, false, false, false);
    }

    /// Creates an event with only a shift modifier.
    ///
    /// @param type the event kind
    /// @param key the logical key
    /// @param shift whether a shift modifier was latched
    public KeyEvent(KeyEventType type, LogicalKey key, boolean shift) {
        this(type, key, shift, false, false, 0, false, false, false);
    }

    /// Creates an event with modifiers and no physical scan code.
    ///
    /// @param type the event kind
    /// @param key the logical key
    /// @param shift whether a shift modifier was latched
    /// @param ctrl whether a control modifier was latched
    /// @param alt whether an alt modifier was latched
    public KeyEvent(KeyEventType type, LogicalKey key, boolean shift, boolean ctrl, boolean alt) {
        this(type, key, shift, ctrl, alt, 0, false, false, false);
    }

    /// Creates an event with modifiers, scan code, and repeat, and no extended bit.
    ///
    /// @param type the event kind
    /// @param key the logical key
    /// @param shift whether a shift modifier was latched
    /// @param ctrl whether a control modifier was latched
    /// @param alt whether an alt modifier was latched
    /// @param scanCode the physical OEM scan code
    /// @param repeat whether this is an auto-repeat
    public KeyEvent(
            KeyEventType type,
            LogicalKey key,
            boolean shift,
            boolean ctrl,
            boolean alt,
            int scanCode,
            boolean repeat
    ) {
        this(type, key, shift, ctrl, alt, scanCode, repeat, false, false);
    }

    /// Creates an event with modifiers, scan code, repeat, and the extended bit, and no meta modifier.
    ///
    /// @param type the event kind
    /// @param key the logical key
    /// @param shift whether a shift modifier was latched
    /// @param ctrl whether a control modifier was latched
    /// @param alt whether an alt modifier was latched
    /// @param scanCode the physical OEM scan code
    /// @param repeat whether this is an auto-repeat
    /// @param extended whether Win32 `KF_EXTENDED` was set
    public KeyEvent(
            KeyEventType type,
            LogicalKey key,
            boolean shift,
            boolean ctrl,
            boolean alt,
            int scanCode,
            boolean repeat,
            boolean extended
    ) {
        this(type, key, shift, ctrl, alt, scanCode, repeat, extended, false);
    }
}
