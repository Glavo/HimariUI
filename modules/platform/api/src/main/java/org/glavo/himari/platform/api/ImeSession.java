package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Portable IME composition session used by editors and host adapters.
///
/// Composition updates may be injected by tests or delivered from a host IME. The session never
/// writes application or editor state.
@NotNullByDefault
public class ImeSession implements TextInputSession {
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
    public ImeSession() {
    }

    @Override
    public void updateComposition(String text) {
        this.composition = Objects.requireNonNull(text, "text");
        this.committed = false;
        this.compositionEnd = compositionStart + text.length();
    }

    @Override
    public void setSurroundingText(String text, int caret) {
        Objects.requireNonNull(text, "text");
        if (caret < 0 || caret > text.length()) {
            throw new IllegalArgumentException("caret must lie within surrounding text");
        }
        this.surroundingText = text;
        this.compositionStart = caret;
        this.compositionEnd = caret + (composition == null ? 0 : composition.length());
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void cancel() {
        composition = null;
        committed = false;
        compositionEnd = compositionStart;
    }

    @Override
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

    @Override
    public @Nullable String composition() {
        return composition;
    }

    @Override
    public boolean committed() {
        return committed;
    }

    @Override
    public String surroundingText() {
        return surroundingText;
    }

    @Override
    public int caret() {
        return compositionEnd;
    }

    @Override
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
