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

/// Creates an unstyled Gregorian date picker.
@NotNullByDefault
public final class DatePicker {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 24.0f);

    /// Inclusive year.
    private int year;

    /// Month in `[1, 12]`.
    private int month;

    /// Day of month, starting at `1`.
    private int day;

    /// Whether the control ignores adjustment.
    private boolean disabled;

    /// Mounted leaf that receives the published ISO date.
    private @Nullable LayoutNode node;

    /// Creates a date picker.
    ///
    /// @param year the year
    /// @param month the month in `[1, 12]`
    /// @param day the day of month
    public DatePicker(int year, int month, int day) {
        validate(year, month, day);
        this.year = year;
        this.month = month;
        this.day = day;
    }

    /// Returns the year.
    ///
    /// @return the year
    public int year() {
        return year;
    }

    /// Returns the month in `[1, 12]`.
    ///
    /// @return the month
    public int month() {
        return month;
    }

    /// Returns the day of month.
    ///
    /// @return the day
    public int day() {
        return day;
    }

    /// Returns the ISO-8601 calendar date.
    ///
    /// @return `YYYY-MM-DD`
    public String value() {
        return padded(year, 4) + '-' + padded(month, 2) + '-' + padded(day, 2);
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the calendar date and publishes it when mounted.
    ///
    /// @param year the year
    /// @param month the month in `[1, 12]`
    /// @param day the day of month
    public void setDate(int year, int month, int day) {
        validate(year, month, day);
        this.year = year;
        this.month = month;
        this.day = day;
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Advances one calendar day, wrapping the month and year.
    public void increment() {
        if (disabled) {
            return;
        }
        if (day < daysInMonth(year, month)) {
            day++;
        } else {
            day = 1;
            if (month < 12) {
                month++;
            } else {
                month = 1;
                year++;
            }
        }
        publish();
    }

    /// Moves back one calendar day, wrapping the month and year.
    public void decrement() {
        if (disabled) {
            return;
        }
        if (day > 1) {
            day--;
        } else if (month > 1) {
            month--;
            day = daysInMonth(year, month);
        } else {
            year--;
            month = 12;
            day = 31;
        }
        publish();
    }

    /// Builds the date-picker leaf.
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
                SemanticsRole.DATE_PICKER,
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

    /// Publishes the ISO date and disabled state onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(value());
        node.setDisabled(disabled);
    }

    /// Rejects a non-Gregorian calendar triple.
    private static void validate(int year, int month, int day) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("DatePicker month must be in [1, 12]");
        }
        if (day < 1 || day > daysInMonth(year, month)) {
            throw new IllegalArgumentException("DatePicker day is out of range for the month");
        }
    }

    /// Returns the number of days in `month` of `year`.
    private static int daysInMonth(int year, int month) {
        return switch (month) {
            case 2 -> leap(year) ? 29 : 28;
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
    }

    /// Returns whether `year` is a Gregorian leap year.
    private static boolean leap(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    /// Zero-pads `value` to `width` decimal digits.
    private static String padded(int value, int width) {
        String digits = Integer.toString(value);
        if (digits.length() >= width) {
            return digits;
        }
        return "0".repeat(width - digits.length()) + digits;
    }
}
