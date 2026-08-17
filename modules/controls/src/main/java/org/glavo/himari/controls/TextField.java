package org.glavo.himari.controls;

import org.glavo.himari.platform.api.TextInputSession;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsTextRange;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled single-line editor that stores committed text in a [`GapBuffer`].
@NotNullByDefault
public final class TextField {
    /// Default field size.
    private static final Size SIZE = new Size(160.0f, 24.0f);

    /// Committed gap-buffer storage.
    private final GapBuffer committed;

    /// Live composition, or `null` when idle.
    private @Nullable String composition;

    /// Undo stack of previously committed text.
    private final ArrayList<String> undo = new ArrayList<>();

    /// Redo stack of undone text.
    private final ArrayList<String> redo = new ArrayList<>();

    /// Inclusive selection start in displayed text.
    private int selectionStart;

    /// Exclusive selection end and caret in displayed text.
    private int selectionEnd;

    /// Last rejected composition, or `null`.
    private @Nullable String rejected;

    /// Whether committed text is masked as a password.
    private boolean password;

    /// Whether committed edits are rejected.
    private boolean readOnly;

    /// Whether the field ignores activation and committed edits.
    private boolean disabled;

    /// Mounted leaf that receives the published disabled and read-only states.
    private @Nullable LayoutNode node;

    /// Creates an empty field.
    public TextField() {
        this.committed = new GapBuffer();
    }

    /// Returns the committed gap buffer.
    ///
    /// @return the storage
    public GapBuffer committed() {
        return committed;
    }

    /// Returns the committed text.
    ///
    /// @return the text
    public String text() {
        return committed.toString();
    }

    /// Returns the live composition, or `null`.
    ///
    /// @return the composition
    public @Nullable String composition() {
        return composition;
    }

    /// Returns the displayed text, including live composition.
    ///
    /// Password fields replace each committed UTF-16 unit with `U+2022`. Live composition is
    /// shown unmasked so IME candidates remain editable.
    ///
    /// @return the displayed text
    public String displayedText() {
        String text = committed.toString();
        if (password) {
            text = "\u2022".repeat(text.length());
        }
        return composition == null ? text : text + composition;
    }

    /// Enables or disables password masking.
    ///
    /// @param password whether committed text is masked
    public void setPassword(boolean password) {
        this.password = password;
    }

    /// Returns whether committed text is masked.
    ///
    /// @return whether this is a password field
    public boolean password() {
        return password;
    }

    /// Returns the caret offset in displayed text.
    ///
    /// @return the caret
    public int caret() {
        return selectionEnd;
    }

    /// Returns the inclusive selection start.
    ///
    /// @return the start
    public int selectionStart() {
        return Math.min(selectionStart, selectionEnd);
    }

    /// Returns the exclusive selection end.
    ///
    /// @return the end
    public int selectionEnd() {
        return Math.max(selectionStart, selectionEnd);
    }

    /// Returns the last rejected composition, or `null`.
    ///
    /// @return the rejected fragment
    public @Nullable String lastRejected() {
        return rejected;
    }

    /// Replaces the selection in displayed text.
    ///
    /// @param start the inclusive start
    /// @param end the exclusive end
    public void setSelection(int start, int end) {
        String displayed = displayedText();
        if (start < 0 || end < 0 || start > displayed.length() || end > displayed.length()) {
            throw new IllegalArgumentException("Selection must lie within displayed text");
        }
        this.selectionStart = start;
        this.selectionEnd = end;
    }

    /// Selects the word covering `offset` in displayed text.
    ///
    /// @param offset a UTF-16 offset in displayed text
    public void selectWordAt(int offset) {
        int[] range = EditorSelection.wordRange(displayedText(), offset);
        setSelection(range[0], range[1]);
    }

    /// Selects the line covering `offset` in displayed text.
    ///
    /// @param offset a UTF-16 offset in displayed text
    public void selectLineAt(int offset) {
        int[] range = EditorSelection.lineRange(displayedText(), offset);
        setSelection(range[0], range[1]);
    }

    /// Moves the caret by one grapheme cluster.
    ///
    /// @param delta `-1` for previous, `1` for next
    public void moveCaretByGrapheme(int delta) {
        if (delta != -1 && delta != 1) {
            throw new IllegalArgumentException("delta must be -1 or 1");
        }
        String displayed = displayedText();
        int caret = caret();
        int next = delta < 0 ? Graphemes.previous(displayed, caret) : Graphemes.next(displayed, caret);
        setSelection(next, next);
    }

    /// Moves the caret to the start of the line covering the caret.
    public void moveToLineStart() {
        int[] range = EditorSelection.lineRange(displayedText(), caret());
        setSelection(range[0], range[0]);
    }

    /// Moves the caret to the end of the line covering the caret.
    public void moveToLineEnd() {
        int[] range = EditorSelection.lineRange(displayedText(), caret());
        setSelection(range[1], range[1]);
    }

    /// Deletes the selection, or the grapheme before the caret, while composition is idle.
    public void deleteBackward() {
        int start = selectionStart();
        int end = selectionEnd();
        if (start != end) {
            replaceRange(start, end, "");
            return;
        }
        if (start == 0) {
            return;
        }
        replaceRange(Graphemes.previous(text(), start), start, "");
    }

    /// Deletes the selection, or the grapheme after the caret, while composition is idle.
    public void deleteForward() {
        int start = selectionStart();
        int end = selectionEnd();
        if (start != end) {
            replaceRange(start, end, "");
            return;
        }
        String committedText = text();
        if (start >= committedText.length()) {
            return;
        }
        replaceRange(start, Graphemes.next(committedText, start), "");
    }

    /// Copies the displayed selection into `clipboard`.
    ///
    /// Password fields leave `clipboard` unchanged so committed plaintext cannot leak.
    ///
    /// @param clipboard the destination bag
    public void copy(EditorClipboard clipboard) {
        Objects.requireNonNull(clipboard, "clipboard");
        if (password) {
            return;
        }
        clipboard.setTextAndHtml(displayedText().substring(selectionStart(), selectionEnd()));
    }

    /// Copies the displayed selection and deletes it while composition is idle.
    ///
    /// Password fields neither copy nor delete.
    ///
    /// @param clipboard the destination bag
    public void cut(EditorClipboard clipboard) {
        Objects.requireNonNull(clipboard, "clipboard");
        if (password) {
            return;
        }
        copy(clipboard);
        int start = selectionStart();
        int end = selectionEnd();
        if (start != end) {
            replaceRange(start, end, "");
        }
    }

    /// Replaces the committed selection with `clipboard` text while composition is idle.
    ///
    /// @param clipboard the source bag
    public void paste(EditorClipboard clipboard) {
        Objects.requireNonNull(clipboard, "clipboard");
        replaceRange(selectionStart(), selectionEnd(), clipboard.text());
    }

    /// Replaces a committed-text range while composition is idle.
    ///
    /// @param start the inclusive start
    /// @param end the exclusive end
    /// @param replacement the replacement
    public void replaceRange(int start, int end, String replacement) {
        Objects.requireNonNull(replacement, "replacement");
        if (readOnly || disabled) {
            return;
        }
        if (composition != null) {
            throw new IllegalStateException("Cannot replace committed text during composition");
        }
        if (start < 0 || end < start || end > committed.length()) {
            throw new IllegalArgumentException("Range must lie within committed text");
        }
        undo.add(committed.toString());
        redo.clear();
        committed.replace(start, end, replacement);
        selectionStart = start + replacement.length();
        selectionEnd = selectionStart;
    }

    /// Begins or updates composition without committing.
    ///
    /// @param value the composition
    public void updateComposition(String value) {
        this.composition = Objects.requireNonNull(value, "value");
        selectionStart = committed.length();
        selectionEnd = displayedText().length();
    }

    /// Commits the current composition onto the stored text.
    ///
    /// @return the committed fragment, or `null` if idle
    public @Nullable String commitComposition() {
        @Nullable String pending = composition;
        if (pending == null) {
            return null;
        }
        undo.add(committed.toString());
        redo.clear();
        committed.replace(committed.length(), committed.length(), pending);
        composition = null;
        rejected = null;
        selectionStart = committed.length();
        selectionEnd = committed.length();
        return pending;
    }

    /// Discards the live composition without changing committed text.
    ///
    /// @return the rejected fragment, or `null` if idle
    public @Nullable String rejectComposition() {
        @Nullable String pending = composition;
        if (pending == null) {
            return null;
        }
        composition = null;
        rejected = pending;
        selectionStart = committed.length();
        selectionEnd = committed.length();
        return pending;
    }

    /// Restores the previous committed text.
    ///
    /// @return whether an undo entry was applied
    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        redo.add(committed.toString());
        committed.assign(undo.removeLast());
        composition = null;
        selectionStart = committed.length();
        selectionEnd = committed.length();
        return true;
    }

    /// Reapplies the last undone text.
    ///
    /// @return whether a redo entry was applied
    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        undo.add(committed.toString());
        committed.assign(redo.removeLast());
        composition = null;
        selectionStart = committed.length();
        selectionEnd = committed.length();
        return true;
    }

    /// Cancels the live composition.
    public void cancelComposition() {
        composition = null;
        selectionStart = committed.length();
        selectionEnd = committed.length();
    }

    /// Applies one IME session snapshot without letting the session write this editor.
    ///
    /// A live composition is copied into the editor. A rejected composition discards the live
    /// fragment. Otherwise the committed surrounding text and caret replace the idle buffer.
    ///
    /// @param session the session
    public void apply(TextInputSession session) {
        Objects.requireNonNull(session, "session");
        @Nullable String live = session.composition();
        if (live != null) {
            updateComposition(live);
            return;
        }
        if (session.lastRejected() != null && composition != null) {
            rejectComposition();
            return;
        }
        String surrounding = session.surroundingText();
        if (composition != null) {
            composition = null;
        }
        if (!surrounding.equals(committed.toString())) {
            undo.add(committed.toString());
            redo.clear();
            committed.assign(surrounding);
        }
        int caret = Math.min(Math.max(0, session.caret()), committed.length());
        selectionStart = caret;
        selectionEnd = caret;
    }

    /// Builds the field leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        String displayed = displayedText();
        String label = password ? "Password" : displayed.isEmpty() ? "Empty" : displayed;
        LayoutNode node = factory.leaf(
                name,
                SIZE,
                List.of(new LayoutModifier.Padding(2.0f)),
                true,
                SemanticsRole.TEXT_FIELD,
                label,
                Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        int start = composition == null ? selectionStart() : committed.length();
        int end = composition == null ? selectionEnd() : displayed.length();
        int caret = composition == null ? selectionEnd : displayed.length();
        node.setTextRange(new SemanticsTextRange(start, end, caret));
        node.setReadOnly(readOnly);
        node.setDisabled(disabled);
        node.setPassword(password);
        this.node = node;
        return node;
    }

    /// Returns whether committed edits are rejected.
    ///
    /// @return whether the field is read-only
    public boolean readOnly() {
        return readOnly;
    }

    /// Sets whether committed edits are rejected.
    ///
    /// @param readOnly the state
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (node != null) {
            node.setReadOnly(readOnly);
        }
    }

    /// Returns whether the field is disabled.
    ///
    /// @return whether the field is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to the mounted leaf when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (node != null) {
            node.setDisabled(disabled);
        }
    }
}
