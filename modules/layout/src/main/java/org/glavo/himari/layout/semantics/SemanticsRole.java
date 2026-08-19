package org.glavo.himari.layout.semantics;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the semantic role of one layout node.
@NotNullByDefault
public enum SemanticsRole {
    /// A generic container.
    NONE,

    /// Static text.
    TEXT,

    /// A non-activatable image.
    IMAGE,

    /// A drawing surface.
    CANVAS,

    /// An activatable control.
    BUTTON,

    /// A two-state switch.
    TOGGLE,

    /// An independently checkable box.
    CHECKBOX,

    /// One exclusive radio option.
    RADIO,

    /// A bounded numeric value.
    SLIDER,

    /// A scrollbar that publishes a bounded range value.
    SCROLLBAR,

    /// A determinate progress indicator.
    PROGRESS,

    /// A scrollable or lazy collection.
    LIST,

    /// A virtualized table of keyed rows.
    TABLE,

    /// One row inside a table.
    TABLE_ROW,

    /// One cell inside a table row.
    TABLE_CELL,

    /// One column header cell.
    TABLE_COLUMN_HEADER,

    /// One row header cell.
    TABLE_ROW_HEADER,

    /// An editable text field.
    TEXT_FIELD,

    /// A multiline editable text area.
    TEXT_AREA,

    /// A status or live-region announcement.
    STATUS,

    /// A generic in-window overlay popup.
    POPUP,

    /// A menu surface that hosts activatable items.
    MENU,

    /// One activatable item inside a menu.
    MENU_ITEM,

    /// A modal dialog surface.
    DIALOG,

    /// A non-activating tooltip surface.
    TOOLTIP,

    /// A tab strip that hosts tab items.
    TAB_LIST,

    /// One selectable tab.
    TAB,

    /// The panel shown for the selected tab.
    TAB_PANEL,

    /// A resizable two-pane split.
    SPLIT_PANE,

    /// A hierarchical outline.
    TREE,

    /// One row inside a tree.
    TREE_ITEM
}
