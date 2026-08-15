package org.glavo.himari.platform.windows;

import org.glavo.himari.layout.LayoutRect;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsSnapshot;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Projects a HimariUI semantics snapshot into a UI Automation inspection tree.
@NotNullByDefault
public final class WindowsAutomationBridge {
    /// Prevents instantiation.
    private WindowsAutomationBridge() {
    }

    /// Builds one inspection tree from the shipped semantics snapshot.
    ///
    /// @param snapshot the semantics snapshot
    /// @return the UIA-facing nodes
    public static @Unmodifiable List<WindowsAutomationNode> inspect(SemanticsSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        ArrayList<WindowsAutomationNode> nodes = new ArrayList<>();
        for (SemanticsNode node : snapshot.nodes()) {
            LayoutRect bounds = node.bounds();
            nodes.add(new WindowsAutomationNode(
                    node.id(),
                    controlType(node.role()),
                    node.label(),
                    node.actions().contains(SemanticsAction.ACTIVATE),
                    node.focused(),
                    bounds.x(),
                    bounds.y(),
                    bounds.width(),
                    bounds.height(),
                    toggleState(node),
                    node.rangeValue()
            ));
        }
        return List.copyOf(nodes);
    }

    /// Maps a semantics role to a UIA control-type name.
    ///
    /// @param role the role
    /// @return the control type
    static String controlType(SemanticsRole role) {
        return switch (role) {
            case BUTTON -> "Button";
            case TOGGLE -> "CheckBox";
            case SLIDER -> "Slider";
            case TEXT_FIELD -> "Edit";
            case LIST -> "List";
            case TEXT -> "Text";
            case NONE -> "Pane";
        };
    }

    /// Returns a toggle-pattern state when the snapshot publishes one.
    ///
    /// @param node the semantics node
    /// @return `On`, `Off`, `Indeterminate` for a toggle without a boolean, or `null`
    static @Nullable String toggleState(SemanticsNode node) {
        if (node.role() != SemanticsRole.TOGGLE) {
            return null;
        }
        if (node.selected() == null) {
            return "Indeterminate";
        }
        return node.selected() ? "On" : "Off";
    }
}
