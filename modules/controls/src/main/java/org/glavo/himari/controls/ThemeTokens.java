package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores the first-stable unstyled theme tokens.
///
/// @param name the theme name
/// @param backgroundArgb the background color
/// @param textArgb the text color
/// @param accentArgb the accent color
/// @param highContrast whether this is the high-contrast theme
@NotNullByDefault
public record ThemeTokens(
        String name,
        int backgroundArgb,
        int textArgb,
        int accentArgb,
        boolean highContrast
) {
    /// Validates the tokens.
    public ThemeTokens {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Theme name must be nonblank");
        }
    }

    /// Returns the default light theme.
    ///
    /// @return the tokens
    public static ThemeTokens standard() {
        return new ThemeTokens("standard", 0xFFFFFFFF, 0xFF1A1A1A, 0xFF1565C0, false);
    }

    /// Returns the high-contrast theme.
    ///
    /// @return the tokens
    public static ThemeTokens highContrastTheme() {
        return new ThemeTokens("high-contrast", 0xFF000000, 0xFFFFFF00, 0xFF00FFFF, true);
    }
}
