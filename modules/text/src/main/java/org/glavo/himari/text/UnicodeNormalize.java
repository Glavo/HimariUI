package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;

import java.text.Normalizer;
import java.util.Objects;

/// Applies the first-stable Unicode normalization stage.
@NotNullByDefault
public final class UnicodeNormalize {
    /// Prevents instantiation.
    private UnicodeNormalize() {
    }

    /// Returns NFC for `text`.
    ///
    /// @param text the source
    /// @return the composed string, which may be `text` itself when already NFC
    public static String nfc(String text) {
        Objects.requireNonNull(text, "text");
        if (text.isEmpty() || Normalizer.isNormalized(text, Normalizer.Form.NFC)) {
            return text;
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    /// Returns NFD for `text`.
    ///
    /// @param text the source
    /// @return the decomposed string, which may be `text` itself when already NFD
    public static String nfd(String text) {
        Objects.requireNonNull(text, "text");
        if (text.isEmpty() || Normalizer.isNormalized(text, Normalizer.Form.NFD)) {
            return text;
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD);
    }

    /// Returns NFKC for `text`.
    ///
    /// @param text the source
    /// @return the compatibility-composed string, which may be `text` itself when already NFKC
    public static String nfkc(String text) {
        Objects.requireNonNull(text, "text");
        if (text.isEmpty() || Normalizer.isNormalized(text, Normalizer.Form.NFKC)) {
            return text;
        }
        return Normalizer.normalize(text, Normalizer.Form.NFKC);
    }

    /// Returns NFKD for `text`.
    ///
    /// @param text the source
    /// @return the compatibility-decomposed string, which may be `text` itself when already NFKD
    public static String nfkd(String text) {
        Objects.requireNonNull(text, "text");
        if (text.isEmpty() || Normalizer.isNormalized(text, Normalizer.Form.NFKD)) {
            return text;
        }
        return Normalizer.normalize(text, Normalizer.Form.NFKD);
    }
}
