package org.glavo.himari.controls;

import org.glavo.himari.layout.semantics.TextDirection;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores the first-stable unstyled theme tokens.
///
/// @param name the theme name
/// @param backgroundArgb the background color
/// @param textArgb the text color
/// @param accentArgb the accent color
/// @param highContrast whether this is the high-contrast theme
/// @param textDirection the reading direction for themed subtrees
/// @param reducedMotion whether nonessential motion must be suppressed
@NotNullByDefault
public record ThemeTokens(
        String name,
        int backgroundArgb,
        int textArgb,
        int accentArgb,
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
                highContrast,
                textDirection,
                reducedMotion
        );
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
                highContrast,
                textDirection,
                reducedMotion
        );
    }
}
