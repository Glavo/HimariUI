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
    TREE_ITEM,

    /// A compact selectable list with a collapsed value.
    COMBO_BOX,

    /// A virtualized two-dimensional item grid.
    GRID,

    /// A Gregorian calendar date.
    DATE_PICKER,

    /// A 24-hour clock time.
    TIME_PICKER,

    /// An 8-bit RGB color well.
    COLOR_PICKER,

    /// An integer spin button.
    STEPPER,

    /// A labeled section that expands or collapses.
    DISCLOSURE,

    /// A single-line search field.
    SEARCH_BOX,

    /// A non-interactive visual separator.
    SEPARATOR,

    /// A strip of labeled commands.
    TOOLBAR,

    /// A trail of path segments.
    BREADCRUMB,

    /// An activatable destination.
    LINK,

    /// A mutually exclusive set of expandable sections.
    ACCORDION,

    /// A numbered page selector.
    PAGINATION,

    /// A target that opens a context menu on secondary pointer press.
    CONTEXT_MENU,

    /// A non-interactive status badge.
    BADGE,

    /// A selectable filter chip.
    CHIP,

    /// A titled grouping card.
    CARD,

    /// A non-interactive identity avatar.
    AVATAR,

    /// A dismissible inline banner.
    BANNER,

    /// A transient snackbar with an optional action.
    SNACKBAR,

    /// A non-interactive loading skeleton.
    SKELETON,

    /// A discrete star rating.
    RATING,

    /// A no-content placeholder.
    EMPTY,

    /// A labeled slide carousel that clamps at the ends.
    CAROUSEL,

    /// A non-modal drawer pane.
    DRAWER
}
