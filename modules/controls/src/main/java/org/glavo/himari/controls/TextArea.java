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

/// Creates an unstyled multiline editor that stores committed text in a [`PieceTable`].
@NotNullByDefault
public final class TextArea {
    /// Default area size.
    private static final Size SIZE = new Size(160.0f, 72.0f);

    /// Committed piece-table storage.
    private final PieceTable committed;

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

    /// Creates an empty area.
    public TextArea() {
        this.committed = new PieceTable();
    }

    /// Returns the committed piece table.
    ///
    /// @return the storage
    public PieceTable committed() {
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
    /// @return the displayed text
    public String displayedText() {
        String text = committed.toString();
        return composition == null ? text : text + composition;
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

    /// Replaces a committed-text range while composition is idle.
    ///
    /// @param start the inclusive start
    /// @param end the exclusive end
    /// @param replacement the replacement
    public void replaceRange(int start, int end, String replacement) {
        Objects.requireNonNull(replacement, "replacement");
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
        if (committed.length() > 0) {
            committed.append("\n");
        }
        committed.append(pending);
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

    /// Builds the area leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        String displayed = displayedText();
        String label = displayed.isEmpty() ? "Empty" : displayed;
        LayoutNode node = factory.leaf(
                name,
                SIZE,
                List.of(new LayoutModifier.Padding(2.0f)),
                true,
                SemanticsRole.TEXT_AREA,
                label,
                Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        int start = composition == null ? selectionStart() : committed.length();
        int end = composition == null ? selectionEnd() : displayed.length();
        int caret = composition == null ? selectionEnd : displayed.length();
        node.setTextRange(new SemanticsTextRange(start, end, caret));
        return node;
    }
}
