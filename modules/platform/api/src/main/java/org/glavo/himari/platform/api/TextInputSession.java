package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Owns one IME composition session used by editing controls.
///
/// The session never writes application or editor state. Editors apply
/// [`#composition()`], [`#surroundingText()`], and [`#lastRejected()`] through their own
/// transactional update path.
@NotNullByDefault
public interface TextInputSession {
    /// Begins or updates composition text.
    ///
    /// @param text the non-null composition
    void updateComposition(String text);

    /// Publishes the editor's surrounding text and caret.
    ///
    /// @param text the surrounding document
    /// @param caret the caret offset, in `[0, text.length]`
    void setSurroundingText(String text, int caret);

    /// Replaces `[start, end)` of the surrounding document and moves the caret to the insert end.
    ///
    /// @param start the inclusive start ACP
    /// @param end the exclusive end ACP
    /// @param text the replacement
    void replaceRange(int start, int end, String text);

    /// Publishes the candidate-window rectangle in logical pixels.
    ///
    /// @param x the origin x
    /// @param y the origin y
    /// @param width the nonnegative width
    /// @param height the nonnegative height
    void setCandidateRectangle(float x, float y, float width, float height);

    /// Commits the current composition.
    ///
    /// @return the committed text, or `null` if idle
    @Nullable String commit();

    /// Restores the last committed fragment as live composition.
    ///
    /// @return the restored composition, or `null` if none
    @Nullable String reconvert();

    /// Cancels the current composition.
    void cancel();

    /// Discards the live composition without changing surrounding text.
    ///
    /// @return the rejected fragment, or `null` if idle
    @Nullable String reject();

    /// Returns the live composition, or `null`.
    ///
    /// @return the composition
    @Nullable String composition();

    /// Returns whether the last action committed text.
    ///
    /// @return whether a commit occurred
    boolean committed();

    /// Returns the surrounding document text.
    ///
    /// @return the surrounding text
    String surroundingText();

    /// Returns the caret offset in surrounding text.
    ///
    /// @return the caret
    int caret();

    /// Returns the last rejected composition, or `null`.
    ///
    /// @return the rejected fragment
    @Nullable String lastRejected();
}
