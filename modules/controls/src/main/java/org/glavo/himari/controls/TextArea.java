package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled multiline editor that accepts injected IME composition.
@NotNullByDefault
public final class TextArea {
    /// Default area size.
    private static final Size SIZE = new Size(160.0f, 72.0f);

    /// Committed text.
    private String text;

    /// Live composition, or `null` when idle.
    private @Nullable String composition;

    /// Undo stack of previously committed text.
    private final ArrayList<String> undo = new ArrayList<>();

    /// Redo stack of undone text.
    private final ArrayList<String> redo = new ArrayList<>();

    /// Creates an empty area.
    public TextArea() {
        this.text = "";
    }

    /// Returns the committed text.
    ///
    /// @return the text
    public String text() {
        return text;
    }

    /// Returns the live composition, or `null`.
    ///
    /// @return the composition
    public @Nullable String composition() {
        return composition;
    }

    /// Begins or updates composition without committing.
    ///
    /// @param value the composition
    public void updateComposition(String value) {
        this.composition = Objects.requireNonNull(value, "value");
    }

    /// Commits the current composition onto the stored text.
    ///
    /// @return the committed fragment, or `null` if idle
    public @Nullable String commitComposition() {
        @Nullable String pending = composition;
        if (pending == null) {
            return null;
        }
        undo.add(text);
        redo.clear();
        text = text.isEmpty() ? pending : text + "\n" + pending;
        composition = null;
        return pending;
    }

    /// Restores the previous committed text.
    ///
    /// @return whether an undo entry was applied
    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        redo.add(text);
        text = undo.removeLast();
        composition = null;
        return true;
    }

    /// Reapplies the last undone text.
    ///
    /// @return whether a redo entry was applied
    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        undo.add(text);
        text = redo.removeLast();
        composition = null;
        return true;
    }

    /// Cancels the live composition.
    public void cancelComposition() {
        composition = null;
    }

    /// Builds the area leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        String label = composition == null ? text : text + composition;
        return factory.leaf(
                name,
                SIZE,
                List.of(new LayoutModifier.Padding(2.0f)),
                true,
                SemanticsRole.TEXT_AREA,
                label.isEmpty() ? "Empty" : label,
                Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
    }
}
