package org.glavo.himari.platform.windows;

import org.glavo.himari.layout.LayoutRect;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
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
                    node.rangeValue(),
                    liveSetting(node.liveRegion()),
                    node.textRange()
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
            case CHECKBOX -> "CheckBox";
            case RADIO -> "RadioButton";
            case SLIDER -> "Slider";
            case SCROLLBAR -> "ScrollBar";
            case PROGRESS -> "ProgressBar";
            case TEXT_FIELD, TEXT_AREA -> "Edit";
            case LIST -> "List";
            case TABLE -> "Table";
            case TABLE_ROW -> "DataItem";
            case TABLE_CELL -> "DataItem";
            case TABLE_COLUMN_HEADER, TABLE_ROW_HEADER -> "HeaderItem";
            case TEXT -> "Text";
            case IMAGE -> "Image";
            case CANVAS -> "Pane";
            case STATUS -> "StatusBar";
            case NONE -> "Pane";
            case POPUP -> "Pane";
            case MENU -> "Menu";
            case MENU_ITEM -> "MenuItem";
            case DIALOG -> "Window";
            case TOOLTIP -> "ToolTip";
            case TAB_LIST -> "Tab";
            case TAB -> "TabItem";
            case TAB_PANEL -> "Pane";
            case SPLIT_PANE -> "Pane";
            case TREE -> "Tree";
            case TREE_ITEM -> "TreeItem";
            case COMBO_BOX -> "ComboBox";
            case GRID -> "DataGrid";
            case DATE_PICKER -> "Calendar";
            case TIME_PICKER -> "Spinner";
            case COLOR_PICKER -> "Custom";
            case STEPPER -> "Spinner";
            case DISCLOSURE -> "Button";
            case SEARCH_BOX -> "Edit";
            case SEPARATOR -> "Separator";
            case TOOLBAR -> "ToolBar";
            case BREADCRUMB -> "Group";
            case LINK -> "Hyperlink";
            case ACCORDION -> "Group";
            case PAGINATION -> "Group";
            case CONTEXT_MENU -> "Menu";
            case BADGE -> "Text";
            case CHIP -> "CheckBox";
            case CARD -> "Group";
            case AVATAR -> "Image";
            case BANNER -> "Text";
            case SNACKBAR -> "Text";
            case SKELETON -> "Pane";
            case RATING -> "Slider";
            case EMPTY -> "Text";
            case CAROUSEL -> "Group";
            case DRAWER -> "Pane";
        };
    }

    /// Maps live-region politeness onto a UIA LiveSetting name.
    ///
    /// @param liveRegion the politeness
    /// @return the live-setting name
    static String liveSetting(SemanticsLiveRegion liveRegion) {
        return switch (liveRegion) {
            case OFF -> "Off";
            case POLITE -> "Polite";
            case ASSERTIVE -> "Assertive";
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
