package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
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

    /// IMM32 reading-window / guideline text, empty when the host has none.
    private String guideline = "";

    /// Pending IMM32 conversion flags, or `-1` when no write is queued.
    private int pendingConversion = -1;

    /// Pending IMM32 sentence flags.
    private int pendingSentence;

    /// Pending IMM32 open status; `null` when no write is queued.
    private @Nullable Boolean pendingOpen;

    /// Pending IMM32 composition face name; `null` when no write is queued.
    private @Nullable String pendingFontFace;

    /// IMM32 `GCS_CURSORPOS` character offset, or `-1` when the host has none.
    private int compositionCursor = -1;

    /// IMM32 `GCS_COMPATTR` bytes, empty when the host has none.
    private byte @Unmodifiable [] compositionAttributes = new byte[0];

    /// IMM32 `GCS_COMPREADSTR` reading text, empty when the host has none.
    private String compositionReading = "";

    /// IMM32 `GCS_COMPCLAUSE` character offsets, empty when the host has none.
    private int @Unmodifiable [] compositionClause = new int[0];

    /// IMM32 `GCS_RESULTREADSTR` reading text, empty when the host has none.
    private String resultReading = "";

    /// IMM32 `GCS_RESULTCLAUSE` character offsets, empty when the host has none.
    private int @Unmodifiable [] resultClause = new int[0];

    /// IMM32 `GCS_DELTASTART` character offset, or `-1` when the host has none.
    private int compositionDeltaStart = -1;

    /// IMM32 `GCS_COMPREADATTR` bytes, empty when the host has none.
    private byte @Unmodifiable [] compositionReadingAttributes = new byte[0];

    /// IMM32 `GCS_COMPREADCLAUSE` character offsets, empty when the host has none.
    private int @Unmodifiable [] compositionReadingClause = new int[0];

    /// IMM32 `GCS_RESULTREADCLAUSE` character offsets, empty when the host has none.
    private int @Unmodifiable [] resultReadingClause = new int[0];

    /// Creates an idle session.
    public ImeSession() {
    }

    @Override
    public void updateComposition(String text) {
        this.composition = Objects.requireNonNull(text, "text");
        this.committed = false;
        this.compositionEnd = compositionStart + text.length();
        if (compositionCursor > text.length()) {
            compositionCursor = text.length();
        }
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
        clearCompositionPreview();
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
        clearCompositionPreview();
        if (text != null) {
            lastRejected = null;
            surroundingText = surroundingText.substring(0, compositionStart) + text
                    + surroundingText.substring(Math.min(compositionStart, surroundingText.length()));
            compositionStart += text.length();
            compositionEnd = compositionStart;
        }
        return text;
    }

    /// Returns the last committed fragment, or `null` when none exists.
    ///
    /// @return the committed text
    public @Nullable String lastCommitted() {
        return lastCommitted;
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
        clearCompositionPreview();
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
        clearCompositionPreview();
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

    /// Publishes IMM32 `ImmGetGuideLineW` reading-window text.
    ///
    /// @param text the guideline, possibly empty
    public void setGuideline(String text) {
        this.guideline = Objects.requireNonNull(text, "text");
    }

    /// Returns the last guideline string published by the host or a test.
    ///
    /// @return the guideline, possibly empty
    public String guideline() {
        return guideline;
    }

    /// Queues IMM32 conversion bits for the next host write-back.
    ///
    /// @param conversion the conversion flags
    /// @param sentence the sentence flags
    public void setConversionStatus(int conversion, int sentence) {
        this.pendingConversion = conversion;
        this.pendingSentence = sentence;
    }

    /// Queues IMM32 open status for the next host write-back.
    ///
    /// @param open whether the IME should be open
    public void setOpenStatus(boolean open) {
        this.pendingOpen = open;
    }

    /// Returns queued conversion flags, or `-1` when none.
    ///
    /// @return the flags
    public int pendingConversion() {
        return pendingConversion;
    }

    /// Returns queued sentence flags.
    ///
    /// @return the flags
    public int pendingSentence() {
        return pendingSentence;
    }

    /// Returns queued open status, or `null` when none.
    ///
    /// @return the status
    public @Nullable Boolean pendingOpen() {
        return pendingOpen;
    }

    /// Clears queued conversion bits after a successful host write.
    public void clearPendingConversion() {
        pendingConversion = -1;
        pendingSentence = 0;
    }

    /// Clears queued open status after a successful host write.
    public void clearPendingOpen() {
        pendingOpen = null;
    }

    /// Queues an IMM32 composition face for the next host write-back.
    ///
    /// @param face the LOGFONT face name, at most 31 UTF-16 units
    public void setCompositionFontFace(String face) {
        Objects.requireNonNull(face, "face");
        if (face.length() > 31) {
            throw new IllegalArgumentException("Composition font face must fit in LF_FACESIZE");
        }
        this.pendingFontFace = face;
    }

    /// Returns the queued composition face, or `null` when none.
    ///
    /// @return the face
    public @Nullable String pendingFontFace() {
        return pendingFontFace;
    }

    /// Clears the queued composition face after a host write.
    public void clearPendingFontFace() {
        pendingFontFace = null;
    }

    /// Publishes IMM32 `GCS_CURSORPOS`.
    ///
    /// @param cursor the character offset, or `-1` when the host has none
    public void setCompositionCursor(int cursor) {
        this.compositionCursor = cursor < 0 ? -1 : cursor;
    }

    /// Returns the last `GCS_CURSORPOS` offset published by the host or a test.
    ///
    /// @return the offset, or `-1` when none
    public int compositionCursor() {
        return compositionCursor;
    }

    /// Publishes IMM32 `GCS_COMPATTR` bytes.
    ///
    /// @param attributes the attribute bytes, possibly empty
    public void setCompositionAttributes(byte[] attributes) {
        Objects.requireNonNull(attributes, "attributes");
        this.compositionAttributes = attributes.length == 0
                ? new byte[0]
                : Arrays.copyOf(attributes, attributes.length);
    }

    /// Returns the last `GCS_COMPATTR` bytes published by the host or a test.
    ///
    /// @return the attributes, possibly empty
    public byte @Unmodifiable [] compositionAttributes() {
        return compositionAttributes;
    }

    /// Publishes IMM32 `GCS_COMPREADSTR`.
    ///
    /// @param text the reading string, possibly empty
    public void setCompositionReading(String text) {
        this.compositionReading = Objects.requireNonNull(text, "text");
    }

    /// Returns the last `GCS_COMPREADSTR` published by the host or a test.
    ///
    /// @return the reading string, possibly empty
    public String compositionReading() {
        return compositionReading;
    }

    /// Publishes IMM32 `GCS_COMPCLAUSE` offsets.
    ///
    /// @param clause the character offsets, possibly empty
    public void setCompositionClause(int[] clause) {
        Objects.requireNonNull(clause, "clause");
        this.compositionClause = clause.length == 0 ? new int[0] : Arrays.copyOf(clause, clause.length);
    }

    /// Returns the last `GCS_COMPCLAUSE` offsets published by the host or a test.
    ///
    /// @return the offsets, possibly empty
    public int @Unmodifiable [] compositionClause() {
        return compositionClause;
    }

    /// Publishes IMM32 `GCS_RESULTREADSTR`.
    ///
    /// @param text the result reading string, possibly empty
    public void setResultReading(String text) {
        this.resultReading = Objects.requireNonNull(text, "text");
    }

    /// Returns the last `GCS_RESULTREADSTR` published by the host or a test.
    ///
    /// @return the reading string, possibly empty
    public String resultReading() {
        return resultReading;
    }

    /// Publishes IMM32 `GCS_RESULTCLAUSE` offsets.
    ///
    /// @param clause the character offsets, possibly empty
    public void setResultClause(int[] clause) {
        Objects.requireNonNull(clause, "clause");
        this.resultClause = clause.length == 0 ? new int[0] : Arrays.copyOf(clause, clause.length);
    }

    /// Returns the last `GCS_RESULTCLAUSE` offsets published by the host or a test.
    ///
    /// @return the offsets, possibly empty
    public int @Unmodifiable [] resultClause() {
        return resultClause;
    }

    /// Publishes IMM32 `GCS_DELTASTART`.
    ///
    /// @param start the character offset, or `-1` when the host has none
    public void setCompositionDeltaStart(int start) {
        this.compositionDeltaStart = start < 0 ? -1 : start;
    }

    /// Returns the last `GCS_DELTASTART` offset published by the host or a test.
    ///
    /// @return the offset, or `-1` when none
    public int compositionDeltaStart() {
        return compositionDeltaStart;
    }

    /// Publishes IMM32 `GCS_COMPREADATTR` bytes.
    ///
    /// @param attributes the reading-window attributes, possibly empty
    public void setCompositionReadingAttributes(byte[] attributes) {
        Objects.requireNonNull(attributes, "attributes");
        this.compositionReadingAttributes = attributes.length == 0
                ? new byte[0]
                : Arrays.copyOf(attributes, attributes.length);
    }

    /// Returns the last `GCS_COMPREADATTR` bytes published by the host or a test.
    ///
    /// @return the attributes, possibly empty
    public byte @Unmodifiable [] compositionReadingAttributes() {
        return compositionReadingAttributes;
    }

    /// Publishes IMM32 `GCS_COMPREADCLAUSE` offsets.
    ///
    /// @param clause the character offsets, possibly empty
    public void setCompositionReadingClause(int[] clause) {
        Objects.requireNonNull(clause, "clause");
        this.compositionReadingClause = clause.length == 0 ? new int[0] : Arrays.copyOf(clause, clause.length);
    }

    /// Returns the last `GCS_COMPREADCLAUSE` offsets published by the host or a test.
    ///
    /// @return the offsets, possibly empty
    public int @Unmodifiable [] compositionReadingClause() {
        return compositionReadingClause;
    }

    /// Publishes IMM32 `GCS_RESULTREADCLAUSE` offsets.
    ///
    /// @param clause the character offsets, possibly empty
    public void setResultReadingClause(int[] clause) {
        Objects.requireNonNull(clause, "clause");
        this.resultReadingClause = clause.length == 0 ? new int[0] : Arrays.copyOf(clause, clause.length);
    }

    /// Returns the last `GCS_RESULTREADCLAUSE` offsets published by the host or a test.
    ///
    /// @return the offsets, possibly empty
    public int @Unmodifiable [] resultReadingClause() {
        return resultReadingClause;
    }

    /// Clears cursor, attribute, clause, and reading preview after commit, cancel, or reject.
    private void clearCompositionPreview() {
        compositionCursor = -1;
        compositionAttributes = new byte[0];
        compositionReading = "";
        compositionClause = new int[0];
        resultReading = "";
        resultClause = new int[0];
        compositionDeltaStart = -1;
        compositionReadingAttributes = new byte[0];
        compositionReadingClause = new int[0];
        resultReadingClause = new int[0];
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
