package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Locale;
import java.util.Objects;

/// Returns first-stable dictionary hyphenation breaks for lowercase Latin words.
///
/// Breaks are cluster offsets after which a hyphen may be inserted. Unknown words use a
/// suffix rule for `tion` when the stem is at least three letters. The hyphenator does not
/// mutate the source string.
@NotNullByDefault
public final class LatinHyphenator {
    /// Prevents instantiation.
    private LatinHyphenator() {
    }

    /// Returns the last break exclusive index in `[1, maxPrefix]` for `word`, or `-1`.
    ///
    /// `maxPrefix` is the number of letters that may stay on the current line, not counting
    /// the hyphen. The empty string and a `maxPrefix` less than `2` yield `-1`.
    ///
    /// @param word the word, compared case-insensitively
    /// @param maxPrefix the inclusive maximum prefix length
    /// @return the break, or `-1`
    public static int lastBreak(String word, int maxPrefix) {
        Objects.requireNonNull(word, "word");
        if (word.isEmpty() || maxPrefix < 2) {
            return -1;
        }
        String folded = word.toLowerCase(Locale.ROOT);
        int[] breaks = breaks(folded);
        int chosen = -1;
        for (int index = 0; index < breaks.length; index++) {
            int breakAt = breaks[index];
            if (breakAt >= 2 && breakAt <= maxPrefix && breakAt < folded.length()) {
                chosen = breakAt;
            }
        }
        return chosen;
    }

    /// Returns dictionary or suffix breaks for `word`.
    ///
    /// @param word a lowercase Latin word
    /// @return break offsets, possibly empty
    static int[] breaks(String word) {
        return switch (word) {
            case "hyphenation" -> new int[] {2, 6};
            case "justification" -> new int[] {4, 7};
            case "information" -> new int[] {2, 5};
            default -> suffixTion(word);
        };
    }

    /// Breaks before a trailing `tion` when the stem is long enough.
    private static int[] suffixTion(String word) {
        if (word.length() >= 7 && word.endsWith("tion")) {
            return new int[] {word.length() - 4};
        }
        return new int[0];
    }
}
