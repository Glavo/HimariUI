package org.glavo.himari.controls;

import org.glavo.himari.layout.semantics.TextDirection;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Stores the first-stable unstyled theme tokens.
///
/// @param name the theme name
/// @param backgroundArgb the background color
/// @param textArgb the text color
/// @param accentArgb the accent color
/// @param disabledArgb the disabled-content color
/// @param focusArgb the focus-ring color
/// @param selectionArgb the selected-content color
/// @param errorArgb the error-content color
/// @param hoverArgb the hover-content color
/// @param borderArgb the border color
/// @param fontScale the relative type size; `1` is the unscaled default
/// @param density the relative spacing scale; `1` is the unscaled default
/// @param highContrast whether this is the high-contrast theme
/// @param textDirection the reading direction for themed subtrees
/// @param reducedMotion whether nonessential motion must be suppressed
@NotNullByDefault
public record ThemeTokens(
        String name,
        int backgroundArgb,
        int textArgb,
        int accentArgb,
        int disabledArgb,
        int focusArgb,
        int selectionArgb,
        int errorArgb,
        int hoverArgb,
        int borderArgb,
        float fontScale,
        float density,
        boolean highContrast,
        TextDirection textDirection,
        boolean reducedMotion
) {
    /// Validates the tokens.
    public ThemeTokens {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Theme name must be nonblank");
        }
        Objects.requireNonNull(textDirection, "textDirection");
        if (!Float.isFinite(fontScale) || fontScale <= 0.0f) {
            throw new IllegalArgumentException("fontScale must be finite and positive");
        }
        if (!Float.isFinite(density) || density <= 0.0f) {
            throw new IllegalArgumentException("density must be finite and positive");
        }
    }

    /// Returns the default light theme.
    ///
    /// @return the tokens
    public static ThemeTokens standard() {
        return new ThemeTokens(
                "standard",
                0xFFFFFFFF,
                0xFF1A1A1A,
                0xFF1565C0,
                0xFF9E9E9E,
                0xFF1565C0,
                0xFFBBDEFB,
                0xFFC62828,
                0xFFE3F2FD,
                0xFFE0E0E0,
                1.0f,
                1.0f,
                false,
                TextDirection.LTR,
                false
        );
    }

    /// Returns the high-contrast theme.
    ///
    /// @return the tokens
    public static ThemeTokens highContrastTheme() {
        return new ThemeTokens(
                "high-contrast",
                0xFF000000,
                0xFFFFFF00,
                0xFF00FFFF,
                0xFF808080,
                0xFFFFFFFF,
                0xFFFFFF00,
                0xFFFF0000,
                0xFF00FFFF,
                0xFFFFFF00,
                1.0f,
                1.0f,
                true,
                TextDirection.LTR,
                false
        );
    }

    /// Returns a copy with the supplied reading direction.
    ///
    /// @param textDirection the reading direction
    /// @return the tokens
    public ThemeTokens withTextDirection(TextDirection textDirection) {
        return new ThemeTokens(
                name,
                backgroundArgb,
                textArgb,
                accentArgb,
                disabledArgb,
                focusArgb,
                selectionArgb,
                errorArgb,
                hoverArgb,
                borderArgb,
                fontScale,
                density,
                highContrast,
                textDirection,
                reducedMotion
        );
    }

    /// Encodes these tokens as a first-stable theme resource payload.
    ///
    /// @return UTF-8 fields separated by `|`
    public byte[] encode() {
        return (name
                + '|'
                + Integer.toUnsignedString(backgroundArgb, 16)
                + '|'
                + Integer.toUnsignedString(textArgb, 16)
                + '|'
                + Integer.toUnsignedString(accentArgb, 16)
                + '|'
                + Integer.toUnsignedString(disabledArgb, 16)
                + '|'
                + Integer.toUnsignedString(focusArgb, 16)
                + '|'
                + Integer.toUnsignedString(selectionArgb, 16)
                + '|'
                + Integer.toUnsignedString(errorArgb, 16)
                + '|'
                + Integer.toUnsignedString(hoverArgb, 16)
                + '|'
                + Integer.toUnsignedString(borderArgb, 16)
                + '|'
                + Float.toString(fontScale)
                + '|'
                + Float.toString(density)
                + '|'
                + highContrast
                + '|'
                + textDirection.name()
                + '|'
                + reducedMotion).getBytes(StandardCharsets.UTF_8);
    }

    /// Decodes a payload produced by [`#encode()`].
    ///
    /// @param bytes the resource bytes
    /// @return the tokens
    public static ThemeTokens decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        String[] fields = new String(bytes, StandardCharsets.UTF_8).split("\\|", -1);
        if (fields.length != 15) {
            throw new IllegalArgumentException("Theme resource must have fifteen fields");
        }
        return new ThemeTokens(
                fields[0],
                parseArgb(fields[1]),
                parseArgb(fields[2]),
                parseArgb(fields[3]),
                parseArgb(fields[4]),
                parseArgb(fields[5]),
                parseArgb(fields[6]),
                parseArgb(fields[7]),
                parseArgb(fields[8]),
                parseArgb(fields[9]),
                parseFontScale(fields[10]),
                parseDensity(fields[11]),
                Boolean.parseBoolean(fields[12]),
                TextDirection.valueOf(fields[13]),
                Boolean.parseBoolean(fields[14])
        );
    }

    /// Parses the relative type-size field.
    private static float parseFontScale(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme fontScale field is not a finite number", failure);
        }
    }

    /// Parses the relative spacing-scale field.
    private static float parseDensity(String field) {
        try {
            return Float.parseFloat(field);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme density field is not a finite number", failure);
        }
    }

    /// Parses one unsigned hex ARGB field.
    private static int parseArgb(String field) {
        try {
            return Integer.parseUnsignedInt(field, 16);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Theme ARGB field is not hexadecimal", failure);
        }
    }

    /// Returns a copy with the supplied reduced-motion policy.
    ///
    /// @param reducedMotion whether nonessential motion must be suppressed
    /// @return the tokens
    public ThemeTokens withReducedMotion(boolean reducedMotion) {
        return new ThemeTokens(
                name,
                backgroundArgb,
                textArgb,
                accentArgb,
                disabledArgb,
                focusArgb,
                selectionArgb,
                errorArgb,
                hoverArgb,
                borderArgb,
                fontScale,
                density,
                highContrast,
                textDirection,
                reducedMotion
        );
    }
}
