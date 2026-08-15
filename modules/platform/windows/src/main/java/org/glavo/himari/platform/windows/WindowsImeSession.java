package org.glavo.himari.platform.windows;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Owns one Windows IME composition session used by editing controls.
///
/// Composition updates may be injected by tests or delivered from a later TSF adapter. The session
/// never writes application state itself.
@NotNullByDefault
public final class WindowsImeSession {
    /// Current composition text, or `null` when idle.
    private @Nullable String composition;

    /// Surrounding document text supplied by the editor.
    private String surroundingText = "";

    /// Composition start offset in surrounding text.
    private int compositionStart;

    /// Composition end offset in surrounding text.
    private int compositionEnd;

    /// Candidate-window x in logical pixels.
    private float candidateX;

    /// Candidate-window y in logical pixels.
    private float candidateY;

    /// Candidate-window width in logical pixels.
    private float candidateWidth;

    /// Candidate-window height in logical pixels.
    private float candidateHeight;

    /// Last committed fragment, used for reconversion.
    private @Nullable String lastCommitted;

    /// Whether the last composition was committed.
    private boolean committed;

    /// Last rejected composition, or `null`.
    private @Nullable String lastRejected;

    /// Creates an idle session.
    public WindowsImeSession() {
    }

    /// Begins or updates composition text.
    ///
    /// @param text the non-null composition
    public void updateComposition(String text) {
        this.composition = Objects.requireNonNull(text, "text");
        this.committed = false;
        this.compositionEnd = compositionStart + text.length();
    }

    /// Publishes the editor's surrounding text and caret.
    ///
    /// @param text the surrounding document
    /// @param caret the caret offset, in `[0, text.length]`
    public void setSurroundingText(String text, int caret) {
        Objects.requireNonNull(text, "text");
        if (caret < 0 || caret > text.length()) {
            throw new IllegalArgumentException("caret must lie within surrounding text");
        }
        this.surroundingText = text;
        this.compositionStart = caret;
        this.compositionEnd = caret + (composition == null ? 0 : composition.length());
    }

    /// Replaces `[start, end)` of the surrounding document and moves the caret to the insert end.
    ///
    /// @param start the inclusive start ACP
    /// @param end the exclusive end ACP
    /// @param text the replacement
    public void replaceRange(int start, int end, String text) {
        Objects.requireNonNull(text, "text");
        if (start < 0 || end < start || end > surroundingText.length()) {
            throw new IllegalArgumentException("range must lie within surrounding text");
        }
        surroundingText = surroundingText.substring(0, start) + text + surroundingText.substring(end);
        composition = null;
        compositionStart = start + text.length();
        compositionEnd = compositionStart;
        if (!text.isEmpty()) {
            lastCommitted = text;
            committed = true;
        }
    }

    /// Publishes the candidate-window rectangle in logical pixels.
    ///
    /// @param x the origin x
    /// @param y the origin y
    /// @param width the nonnegative width
    /// @param height the nonnegative height
    public void setCandidateRectangle(float x, float y, float width, float height) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(width) || !Float.isFinite(height)
                || width < 0.0f || height < 0.0f) {
            throw new IllegalArgumentException("Candidate rectangle must be finite with nonnegative extents");
        }
        this.candidateX = x;
        this.candidateY = y;
        this.candidateWidth = width;
        this.candidateHeight = height;
    }

    /// Commits the current composition.
    ///
    /// @return the committed text, or `null` if idle
    public @Nullable String commit() {
        @Nullable String text = composition;
        composition = null;
        committed = text != null;
        lastCommitted = text;
        if (text != null) {
            lastRejected = null;
            surroundingText = surroundingText.substring(0, compositionStart) + text
                    + surroundingText.substring(Math.min(compositionStart, surroundingText.length()));
            compositionStart += text.length();
            compositionEnd = compositionStart;
        }
        return text;
    }

    /// Restores the last committed fragment as live composition.
    ///
    /// @return the restored composition, or `null` if none
    public @Nullable String reconvert() {
        if (lastCommitted == null) {
            return null;
        }
        composition = lastCommitted;
        committed = false;
        compositionStart = Math.max(0, compositionStart - lastCommitted.length());
        compositionEnd = compositionStart + lastCommitted.length();
        return composition;
    }

    /// Cancels the current composition.
    public void cancel() {
        composition = null;
        committed = false;
        compositionEnd = compositionStart;
    }

    /// Discards the live composition without changing surrounding text.
    ///
    /// @return the rejected fragment, or `null` if idle
    public @Nullable String reject() {
        @Nullable String pending = composition;
        if (pending == null) {
            return null;
        }
        composition = null;
        committed = false;
        lastRejected = pending;
        compositionEnd = compositionStart;
        return pending;
    }

    /// Returns the live composition, or `null`.
    ///
    /// @return the composition
    public @Nullable String composition() {
        return composition;
    }

    /// Returns whether the last action committed text.
    ///
    /// @return whether a commit occurred
    public boolean committed() {
        return committed;
    }

    /// Returns the surrounding document text.
    ///
    /// @return the surrounding text
    public String surroundingText() {
        return surroundingText;
    }

    /// Returns the last rejected composition, or `null`.
    ///
    /// @return the rejected fragment
    public @Nullable String lastRejected() {
        return lastRejected;
    }

    /// Returns the composition start offset.
    ///
    /// @return the start
    public int compositionStart() {
        return compositionStart;
    }

    /// Returns the composition end offset.
    ///
    /// @return the end
    public int compositionEnd() {
        return compositionEnd;
    }

    /// Returns the candidate-window x.
    ///
    /// @return the x
    public float candidateX() {
        return candidateX;
    }

    /// Returns the candidate-window y.
    ///
    /// @return the y
    public float candidateY() {
        return candidateY;
    }

    /// Returns the candidate-window width.
    ///
    /// @return the width
    public float candidateWidth() {
        return candidateWidth;
    }

    /// Returns the candidate-window height.
    ///
    /// @return the height
    public float candidateHeight() {
        return candidateHeight;
    }
}
