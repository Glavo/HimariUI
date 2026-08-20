package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled 24-hour time picker.
@NotNullByDefault
public final class TimePicker {
    /// Default control size.
    private static final Size SIZE = new Size(96.0f, 24.0f);

    /// Hour in `[0, 23]`.
    private int hour;

    /// Minute in `[0, 59]`.
    private int minute;

    /// Whether the control ignores adjustment.
    private boolean disabled;

    /// Mounted leaf that receives the published clock time.
    private @Nullable LayoutNode node;

    /// Creates a time picker.
    ///
    /// @param hour the hour in `[0, 23]`
    /// @param minute the minute in `[0, 59]`
    public TimePicker(int hour, int minute) {
        validate(hour, minute);
        this.hour = hour;
        this.minute = minute;
    }

    /// Returns the hour in `[0, 23]`.
    ///
    /// @return the hour
    public int hour() {
        return hour;
    }

    /// Returns the minute in `[0, 59]`.
    ///
    /// @return the minute
    public int minute() {
        return minute;
    }

    /// Returns the `HH:MM` clock time.
    ///
    /// @return the formatted time
    public String value() {
        return padded(hour) + ':' + padded(minute);
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the clock time and publishes it when mounted.
    ///
    /// @param hour the hour in `[0, 23]`
    /// @param minute the minute in `[0, 59]`
    public void setTime(int hour, int minute) {
        validate(hour, minute);
        this.hour = hour;
        this.minute = minute;
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Advances one minute, wrapping the hour at midnight.
    public void increment() {
        if (disabled) {
            return;
        }
        if (minute < 59) {
            minute++;
        } else {
            minute = 0;
            hour = (hour + 1) % 24;
        }
        publish();
    }

    /// Moves back one minute, wrapping the hour at midnight.
    public void decrement() {
        if (disabled) {
            return;
        }
        if (minute > 0) {
            minute--;
        } else {
            minute = 59;
            hour = (hour + 23) % 24;
        }
        publish();
    }

    /// Builds the time-picker leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        LayoutNode leaf = factory.leaf(
                name,
                SIZE,
                List.of(new LayoutModifier.Padding(0.0f)),
                true,
                SemanticsRole.TIME_PICKER,
                value(),
                Set.of(SemanticsAction.ACTIVATE, SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                this::increment,
                delta -> {
                    if (delta > 0) {
                        increment();
                    } else {
                        decrement();
                    }
                }
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes the clock time and disabled state onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(value());
        node.setDisabled(disabled);
    }

    /// Rejects a time outside 24-hour clock bounds.
    private static void validate(int hour, int minute) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("TimePicker hour must be in [0, 23]");
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("TimePicker minute must be in [0, 59]");
        }
    }

    /// Zero-pads `value` to two decimal digits.
    private static String padded(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }
}
