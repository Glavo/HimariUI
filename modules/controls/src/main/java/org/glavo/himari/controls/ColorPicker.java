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

/// Creates an unstyled 8-bit RGB color picker.
@NotNullByDefault
public final class ColorPicker {
    /// Default control size.
    private static final Size SIZE = new Size(96.0f, 24.0f);

    /// Red in `[0, 255]`.
    private int red;

    /// Green in `[0, 255]`.
    private int green;

    /// Blue in `[0, 255]`.
    private int blue;

    /// Whether the control ignores adjustment.
    private boolean disabled;

    /// Mounted leaf that receives the published hex color.
    private @Nullable LayoutNode node;

    /// Creates a color picker.
    ///
    /// @param red the red channel in `[0, 255]`
    /// @param green the green channel in `[0, 255]`
    /// @param blue the blue channel in `[0, 255]`
    public ColorPicker(int red, int green, int blue) {
        validate(red, green, blue);
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    /// Returns the red channel in `[0, 255]`.
    ///
    /// @return the red channel
    public int red() {
        return red;
    }

    /// Returns the green channel in `[0, 255]`.
    ///
    /// @return the green channel
    public int green() {
        return green;
    }

    /// Returns the blue channel in `[0, 255]`.
    ///
    /// @return the blue channel
    public int blue() {
        return blue;
    }

    /// Returns the `#RRGGBB` color.
    ///
    /// @return the hex color
    public String value() {
        return '#' + hex(red) + hex(green) + hex(blue);
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the RGB color and publishes it when mounted.
    ///
    /// @param red the red channel in `[0, 255]`
    /// @param green the green channel in `[0, 255]`
    /// @param blue the blue channel in `[0, 255]`
    public void setColor(int red, int green, int blue) {
        validate(red, green, blue);
        this.red = red;
        this.green = green;
        this.blue = blue;
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Advances red by one, wrapping at `255`.
    public void increment() {
        if (disabled) {
            return;
        }
        red = (red + 1) & 0xFF;
        publish();
    }

    /// Moves red back by one, wrapping at `0`.
    public void decrement() {
        if (disabled) {
            return;
        }
        red = (red + 255) & 0xFF;
        publish();
    }

    /// Builds the color-picker leaf.
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
                SemanticsRole.COLOR_PICKER,
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

    /// Publishes the hex color and disabled state onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(value());
        node.setDisabled(disabled);
    }

    /// Rejects a channel outside `[0, 255]`.
    private static void validate(int red, int green, int blue) {
        if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
            throw new IllegalArgumentException("ColorPicker channels must be in [0, 255]");
        }
    }

    /// Encodes one channel as two uppercase hex digits.
    private static String hex(int value) {
        String digits = Integer.toHexString(value).toUpperCase();
        return digits.length() == 1 ? "0" + digits : digits;
    }
}
