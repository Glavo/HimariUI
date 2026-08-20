package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

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
/// @param location the physical key location
/// @param timestampMillis host message time in milliseconds; `0` when unreported
/// @param text `ToUnicodeW` translation for this virtual key, or `null` when the host produced none
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
        boolean meta,
        KeyLocation location,
        long timestampMillis,
        @Nullable String text
) {
    /// Validates the event.
    public KeyEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(location, "location");
        if (scanCode < 0) {
            throw new IllegalArgumentException("scanCode must be non-negative");
        }
        if (timestampMillis < 0L) {
            throw new IllegalArgumentException("timestampMillis must be non-negative");
        }
    }

    /// Creates an event without modifiers.
    ///
    /// @param type the event kind
    /// @param key the logical key
    public KeyEvent(KeyEventType type, LogicalKey key) {
        this(type, key, false, false, false, 0, false, false, false, KeyLocation.STANDARD);
    }

    /// Creates an event with only a shift modifier.
    ///
    /// @param type the event kind
    /// @param key the logical key
    /// @param shift whether a shift modifier was latched
    public KeyEvent(KeyEventType type, LogicalKey key, boolean shift) {
        this(type, key, shift, false, false, 0, false, false, false, KeyLocation.STANDARD);
    }

    /// Creates an event with modifiers and no physical scan code.
    ///
    /// @param type the event kind
    /// @param key the logical key
    /// @param shift whether a shift modifier was latched
    /// @param ctrl whether a control modifier was latched
    /// @param alt whether an alt modifier was latched
    public KeyEvent(KeyEventType type, LogicalKey key, boolean shift, boolean ctrl, boolean alt) {
        this(type, key, shift, ctrl, alt, 0, false, false, false, KeyLocation.STANDARD);
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
        this(type, key, shift, ctrl, alt, scanCode, repeat, false, false, KeyLocation.STANDARD);
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
        this(type, key, shift, ctrl, alt, scanCode, repeat, extended, false, KeyLocation.STANDARD);
    }

    /// Creates an event with modifiers, scan, repeat, extended, and meta, and a standard location.
    ///
    /// @param type the event kind
    /// @param key the logical key
    /// @param shift whether a shift modifier was latched
    /// @param ctrl whether a control modifier was latched
    /// @param alt whether an alt modifier was latched
    /// @param scanCode the physical OEM scan code
    /// @param repeat whether this is an auto-repeat
    /// @param extended whether Win32 `KF_EXTENDED` was set
    /// @param meta whether a Windows / Super modifier was latched
    public KeyEvent(
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
        this(type, key, shift, ctrl, alt, scanCode, repeat, extended, meta, KeyLocation.STANDARD);
    }

    /// Creates an event with a physical location and no host timestamp.
    ///
    /// @param type the event kind
    /// @param key the logical key
    /// @param shift whether a shift modifier was latched
    /// @param ctrl whether a control modifier was latched
    /// @param alt whether an alt modifier was latched
    /// @param scanCode the physical OEM scan code
    /// @param repeat whether this is an auto-repeat
    /// @param extended whether Win32 `KF_EXTENDED` was set
    /// @param meta whether a Windows / Super modifier was latched
    /// @param location the physical key location
    public KeyEvent(
            KeyEventType type,
            LogicalKey key,
            boolean shift,
            boolean ctrl,
            boolean alt,
            int scanCode,
            boolean repeat,
            boolean extended,
            boolean meta,
            KeyLocation location
    ) {
        this(type, key, shift, ctrl, alt, scanCode, repeat, extended, meta, location, 0L, null);
    }

    /// Creates an event with a host timestamp and no translated text.
    ///
    /// @param type the event kind
    /// @param key the logical key
    /// @param shift whether a shift modifier was latched
    /// @param ctrl whether a control modifier was latched
    /// @param alt whether an alt modifier was latched
    /// @param scanCode the physical OEM scan code
    /// @param repeat whether this is an auto-repeat
    /// @param extended whether Win32 `KF_EXTENDED` was set
    /// @param meta whether a Windows / Super modifier was latched
    /// @param location the physical key location
    /// @param timestampMillis host message time in milliseconds
    public KeyEvent(
            KeyEventType type,
            LogicalKey key,
            boolean shift,
            boolean ctrl,
            boolean alt,
            int scanCode,
            boolean repeat,
            boolean extended,
            boolean meta,
            KeyLocation location,
            long timestampMillis
    ) {
        this(type, key, shift, ctrl, alt, scanCode, repeat, extended, meta, location, timestampMillis, null);
    }
}
