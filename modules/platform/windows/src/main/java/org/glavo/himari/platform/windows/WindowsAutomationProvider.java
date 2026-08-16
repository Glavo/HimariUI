package org.glavo.himari.platform.windows;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsGrid;
import org.glavo.himari.layout.semantics.SemanticsGridItem;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsScroll;
import org.glavo.himari.layout.semantics.SemanticsTextRange;
import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/// Implements `IRawElementProviderSimple` plus Invoke, Toggle, RangeValue, Value,
/// ExpandCollapse, SelectionItem, Grid, Table, Scroll, ScrollItem, Window, and Text COM patterns.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsAutomationProvider implements AutoCloseable {
    /// `IUnknown`.
    private static final UUID IUNKNOWN = UUID.fromString("00000000-0000-0000-c000-000000000046");

    /// `IRawElementProviderSimple`.
    private static final UUID IRAW_ELEMENT_PROVIDER_SIMPLE =
            UUID.fromString("d6dd68d1-86fd-4332-8666-9abedea2d24c");

    /// `IInvokeProvider`.
    private static final UUID IINVOKE_PROVIDER = UUID.fromString("619be086-1f4e-4ee4-839e-4544a54da35d");

    /// `IToggleProvider`.
    private static final UUID ITOGGLE_PROVIDER = UUID.fromString("56d00bd0-c4f4-433c-a836-1a52a57e0892");

    /// `IRangeValueProvider`.
    private static final UUID IRANGE_VALUE_PROVIDER = UUID.fromString("36dc7aef-33e6-4691-afe1-2be7274b3d33");

    /// `IExpandCollapseProvider`.
    private static final UUID IEXPAND_COLLAPSE_PROVIDER =
            UUID.fromString("d847d3a5-cab0-4a98-8c32-ecb45c59ad24");

    /// `ISelectionItemProvider`.
    private static final UUID ISELECTION_ITEM_PROVIDER =
            UUID.fromString("2acad808-b2d4-452d-a407-91ff1ad167c9");

    /// `IGridProvider`.
    private static final UUID IGRID_PROVIDER = UUID.fromString("b17d6187-0907-464b-a168-0ef17a1572b1");

    /// `IGridItemProvider`.
    private static final UUID IGRID_ITEM_PROVIDER = UUID.fromString("d02541f1-fb81-4d64-ae32-f520f8a6dbd1");

    /// `ITableProvider`.
    private static final UUID ITABLE_PROVIDER = UUID.fromString("9c860395-97b3-490a-b52a-858cc22af166");

    /// `ITableItemProvider`.
    private static final UUID ITABLE_ITEM_PROVIDER = UUID.fromString("b9734fa6-771f-4d78-9c90-2517999349cd");

    /// `IScrollProvider`.
    private static final UUID ISCROLL_PROVIDER = UUID.fromString("b38b8077-1fc3-42a5-8cae-d40c2215055a");

    /// `IScrollItemProvider`.
    private static final UUID ISCROLL_ITEM_PROVIDER = UUID.fromString("2360c714-4bf1-4b26-ba65-9b21316127eb");

    /// `IValueProvider`.
    private static final UUID IVALUE_PROVIDER = UUID.fromString("c7935180-6fb3-4201-b174-7df73adbf64a");

    /// `IWindowProvider`.
    private static final UUID IWINDOW_PROVIDER = UUID.fromString("987df77b-db06-4d77-8f8a-86a9c3bb90b9");

    /// `ITextProvider`.
    private static final UUID ITEXT_PROVIDER = UUID.fromString("3589c92c-63f3-4367-99bb-ada653b77cf2");

    /// `ITextRangeProvider`.
    private static final UUID ITEXT_RANGE_PROVIDER = UUID.fromString("534729dc-411e-4aaa-9d3a-eb1d1d2c9d87");

    /// `UIA_ControlTypePropertyId`.
    static final int UIA_CONTROL_TYPE_PROPERTY_ID = 30003;

    /// `UIA_LiveSettingPropertyId`.
    static final int UIA_LIVE_SETTING_PROPERTY_ID = 30135;

    /// `UIA_ButtonControlTypeId`.
    static final int UIA_BUTTON_CONTROL_TYPE_ID = 50000;

    /// `UIA_StatusBarControlTypeId`.
    static final int UIA_STATUS_BAR_CONTROL_TYPE_ID = 50017;

    /// `LiveSetting_Off`.
    static final int LIVE_SETTING_OFF = 0;

    /// `LiveSetting_Polite`.
    static final int LIVE_SETTING_POLITE = 1;

    /// `LiveSetting_Assertive`.
    static final int LIVE_SETTING_ASSERTIVE = 2;

    /// `UIA_InvokePatternId`.
    static final int UIA_INVOKE_PATTERN_ID = 10000;

    /// `UIA_ValuePatternId`.
    static final int UIA_VALUE_PATTERN_ID = 10002;

    /// `UIA_RangeValuePatternId`.
    static final int UIA_RANGE_VALUE_PATTERN_ID = 10003;

    /// `UIA_ScrollPatternId`.
    static final int UIA_SCROLL_PATTERN_ID = 10004;

    /// `UIA_ScrollItemPatternId`.
    static final int UIA_SCROLL_ITEM_PATTERN_ID = 10017;

    /// `UIA_ExpandCollapsePatternId`.
    static final int UIA_EXPAND_COLLAPSE_PATTERN_ID = 10005;

    /// `UIA_GridPatternId`.
    static final int UIA_GRID_PATTERN_ID = 10006;

    /// `UIA_GridItemPatternId`.
    static final int UIA_GRID_ITEM_PATTERN_ID = 10007;

    /// `UIA_WindowPatternId`.
    static final int UIA_WINDOW_PATTERN_ID = 10009;

    /// `UIA_SelectionItemPatternId`.
    static final int UIA_SELECTION_ITEM_PATTERN_ID = 10010;

    /// `UIA_TablePatternId`.
    static final int UIA_TABLE_PATTERN_ID = 10012;

    /// `UIA_TableItemPatternId`.
    static final int UIA_TABLE_ITEM_PATTERN_ID = 10013;

    /// `UIA_TextPatternId`.
    static final int UIA_TEXT_PATTERN_ID = 10014;

    /// `UIA_TogglePatternId`.
    static final int UIA_TOGGLE_PATTERN_ID = 10015;

    /// `SupportedTextSelection_Single`.
    static final int SUPPORTED_TEXT_SELECTION_SINGLE = 1;

    /// `TextUnit_Character`.
    static final int TEXT_UNIT_CHARACTER = 0;

    /// `TextUnit_Document`.
    static final int TEXT_UNIT_DOCUMENT = 6;

    /// `TextPatternRangeEndpoint_Start`.
    static final int TEXT_PATTERN_RANGE_ENDPOINT_START = 0;

    /// `TextPatternRangeEndpoint_End`.
    static final int TEXT_PATTERN_RANGE_ENDPOINT_END = 1;

    /// `ToggleState_Off`.
    static final int TOGGLE_STATE_OFF = 0;

    /// `ToggleState_On`.
    static final int TOGGLE_STATE_ON = 1;

    /// `ToggleState_Indeterminate`.
    static final int TOGGLE_STATE_INDETERMINATE = 2;

    /// `WindowVisualState_Normal`.
    static final int WINDOW_VISUAL_STATE_NORMAL = 0;

    /// `WindowVisualState_Maximized`.
    static final int WINDOW_VISUAL_STATE_MAXIMIZED = 1;

    /// `WindowInteractionState_ReadyForUserInteraction`.
    static final int WINDOW_INTERACTION_READY = 2;

    /// `ExpandCollapseState_Collapsed`.
    static final int EXPAND_COLLAPSE_STATE_COLLAPSED = 0;

    /// `ExpandCollapseState_Expanded`.
    static final int EXPAND_COLLAPSE_STATE_EXPANDED = 1;

    /// `ExpandCollapseState_LeafNode`.
    static final int EXPAND_COLLAPSE_STATE_LEAF = 3;

    /// `RowOrColumnMajor_RowMajor`.
    static final int ROW_OR_COLUMN_MAJOR_ROW = 0;

    /// `ScrollAmount_SmallIncrement`.
    static final int SCROLL_AMOUNT_SMALL_INCREMENT = 1;

    /// `ScrollAmount_NoAmount`.
    static final int SCROLL_AMOUNT_NO_AMOUNT = 2;

    /// `ScrollAmount_LargeIncrement`.
    static final int SCROLL_AMOUNT_LARGE_INCREMENT = 4;

    /// `UIA_ScrollPatternNoScroll`.
    static final double SCROLL_NO_AMOUNT = -1.0;

    /// `VT_I4`.
    private static final int VT_I4 = 3;

    /// `S_OK`.
    private static final int S_OK = 0;

    /// `E_NOINTERFACE`.
    private static final int E_NOINTERFACE = 0x8000_4002;

    /// `E_POINTER`.
    private static final int E_POINTER = 0x8000_4003;

    /// `E_NOTIMPL`.
    private static final int E_NOTIMPL = 0x8000_4001;

    /// Arena owning the COM objects.
    private final Arena arena;

    /// Contained callback failures.
    private final CallbackFailureQueue failures = new CallbackFailureQueue();

    /// Projected semantics node.
    private final SemanticsNode node;

    /// Simple provider COM object.
    private final MemorySegment simpleObject;

    /// Simple provider vtable.
    private final MemorySegment simpleVtable;

    /// Invoke provider COM object.
    private final MemorySegment invokeObject;

    /// Scroll-item provider COM object.
    private final MemorySegment scrollItemObject;

    /// Value provider COM object.
    private final MemorySegment valueObject;

    /// Window provider COM object.
    private final MemorySegment windowObject;

    /// Toggle provider COM object.
    private final MemorySegment toggleObject;

    /// Range provider COM object.
    private final MemorySegment rangeObject;

    /// Expand/collapse provider COM object.
    private final MemorySegment expandObject;

    /// Selection-item provider COM object.
    private final MemorySegment selectionObject;

    /// Grid provider COM object.
    private final MemorySegment gridObject;

    /// Table provider COM object.
    private final MemorySegment tableObject;

    /// Grid-item provider COM object for this node.
    private final MemorySegment gridItemObject;

    /// Table-item provider COM object for this node.
    private final MemorySegment tableItemObject;

    /// Cell COM object returned by [`#getGridItem`].
    private final MemorySegment fetchedCellObject;

    /// Scroll provider COM object.
    private final MemorySegment scrollObject;

    /// Text provider COM object.
    private final MemorySegment textObject;

    /// Document text-range COM object.
    private final MemorySegment textRangeObject;

    /// Outstanding references for the simple provider.
    private int references = 1;

    /// Number of `IInvokeProvider::Invoke` calls.
    private int invokeCount;

    /// Live toggle state.
    private int toggleState;

    /// Live range value.
    private double rangeValue;

    /// Live expand/collapse state.
    private int expandState;

    /// Live selection-item state.
    private boolean itemSelected;

    /// Published grid row count.
    private int gridRows;

    /// Published grid column count.
    private int gridColumns;

    /// This node's cell row, or `-1`.
    private int cellRow;

    /// This node's cell column, or `-1`.
    private int cellColumn;

    /// This node's cell row span.
    private int cellRowSpan;

    /// This node's cell column span.
    private int cellColumnSpan;

    /// Row of the last successful [`#getGridItem`] fetch.
    private int fetchedRow;

    /// Column of the last successful [`#getGridItem`] fetch.
    private int fetchedColumn;

    /// COM object returned by the last successful [`#invokeGetItem(int, int)`].
    private MemorySegment lastFetchedCell = MemorySegment.NULL;

    /// Vertical scroll percent in `[0, 100]`.
    private double verticalScrollPercent;

    /// Vertical view-size percent in `(0, 100]`.
    private double verticalViewSize;

    /// Whether the viewport can move vertically.
    private boolean verticallyScrollable;

    /// Horizontal scroll percent in `[0, 100]`.
    private double horizontalScrollPercent;

    /// Horizontal view-size percent in `(0, 100]`.
    private double horizontalViewSize;

    /// Whether the viewport can move horizontally.
    private boolean horizontallyScrollable;

    /// Inclusive UTF-16 start of the current text range.
    private int rangeStart;

    /// Exclusive UTF-16 end of the current text range.
    private int rangeEnd;

    /// Whether [`#selectRange`] has published this range as the selection.
    private boolean rangeSelected;

    /// Whether [`#scrollIntoView`] has been invoked.
    private boolean scrolledIntoView;

    /// Number of `IScrollItemProvider::ScrollIntoView` invocations.
    private int scrollItemCount;

    /// Current `IValueProvider` string.
    private String valueText;

    /// Current `IWindowProvider` visual state.
    private int windowVisualState;

    /// Number of `IWindowProvider::Close` invocations.
    private int windowCloseCount;

    /// Whether closed.
    private boolean closed;

    /// Creates one provider.
    private WindowsAutomationProvider(Win32FfmBindings bindings, SemanticsNode node) {
        this.node = node;
        this.valueText = node.label();
        this.windowVisualState = WINDOW_VISUAL_STATE_NORMAL;
        this.toggleState = initialToggleState(node);
        this.rangeValue = node.rangeValue() == null ? 0.0 : node.rangeValue();
        this.expandState = initialExpandState(node);
        this.itemSelected = node.selected() != null && node.selected();
        SemanticsGrid grid = node.grid();
        this.gridRows = grid == null ? 0 : grid.rowCount();
        this.gridColumns = grid == null ? 0 : grid.columnCount();
        SemanticsScroll scroll = node.scroll();
        if (scroll != null) {
            this.verticalScrollPercent = scroll.verticalPercent();
            this.verticalViewSize = scroll.verticalViewSize();
            this.verticallyScrollable = scroll.verticallyScrollable();
            this.horizontalScrollPercent = scroll.horizontalPercent();
            this.horizontalViewSize = scroll.horizontalViewSize();
            this.horizontallyScrollable = scroll.horizontallyScrollable();
        } else if (node.rangeValue() != null && node.role() == SemanticsRole.SCROLLBAR) {
            this.verticalScrollPercent = Math.min(100.0, Math.max(0.0, node.rangeValue()));
            this.verticalViewSize = 10.0;
            this.verticallyScrollable = true;
            this.horizontalScrollPercent = 0.0;
            this.horizontalViewSize = 100.0;
            this.horizontallyScrollable = false;
        } else {
            this.verticalScrollPercent = 0.0;
            this.verticalViewSize = 100.0;
            this.verticallyScrollable = false;
            this.horizontalScrollPercent = 0.0;
            this.horizontalViewSize = 100.0;
            this.horizontallyScrollable = false;
        }
        SemanticsGridItem item = node.gridItem();
        this.cellRow = item == null ? -1 : item.row();
        this.cellColumn = item == null ? -1 : item.column();
        this.cellRowSpan = item == null ? 1 : item.rowSpan();
        this.cellColumnSpan = item == null ? 1 : item.columnSpan();
        this.fetchedRow = -1;
        this.fetchedColumn = -1;
        this.rangeStart = 0;
        this.rangeEnd = node.label().length();
        this.arena = Arena.ofConfined();
        this.simpleVtable = arena.allocate(ValueLayout.ADDRESS, 6);
        this.simpleObject = arena.allocate(ValueLayout.ADDRESS);
        simpleObject.set(ValueLayout.ADDRESS, 0L, simpleVtable);
        simpleVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryInterface, failures, arena));
        simpleVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        simpleVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        simpleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIrawElementProviderGetPatternProviderStub(this::getPatternProvider, failures, arena)
        );
        simpleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIrawElementProviderGetPropertyValueStub(this::getPropertyValue, failures, arena)
        );
        this.invokeObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment invokeVtable = arena.allocate(ValueLayout.ADDRESS, 4);
        invokeObject.set(ValueLayout.ADDRESS, 0L, invokeVtable);
        invokeVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryInvoke, failures, arena));
        invokeVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        invokeVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        invokeVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIinvokeProviderInvokeStub(this::invoke, failures, arena));
        this.scrollItemObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment scrollItemVtable = arena.allocate(ValueLayout.ADDRESS, 4);
        scrollItemObject.set(ValueLayout.ADDRESS, 0L, scrollItemVtable);
        scrollItemVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryScrollItem, failures, arena));
        scrollItemVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        scrollItemVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        scrollItemVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIscrollItemProviderScrollIntoViewStub(this::scrollItemIntoView, failures, arena)
        );
        this.valueObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment valueVtable = arena.allocate(ValueLayout.ADDRESS, 6);
        valueObject.set(ValueLayout.ADDRESS, 0L, valueVtable);
        valueVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryValue, failures, arena));
        valueVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        valueVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        valueVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIvalueProviderSetValueStub(this::setValue, failures, arena));
        valueVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIvalueProviderGetValueStub(this::getValue, failures, arena));
        valueVtable.setAtIndex(ValueLayout.ADDRESS, 5L, bindings.createIvalueProviderGetIsReadOnlyStub(this::getValueReadOnly, failures, arena));
        this.windowObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment windowVtable = arena.allocate(ValueLayout.ADDRESS, 12);
        windowObject.set(ValueLayout.ADDRESS, 0L, windowVtable);
        windowVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryWindow, failures, arena));
        windowVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        windowVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        windowVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIwindowProviderSetVisualStateStub(this::setVisualState, failures, arena));
        windowVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIwindowProviderCloseStub(this::closeWindow, failures, arena));
        windowVtable.setAtIndex(ValueLayout.ADDRESS, 5L, bindings.createIwindowProviderWaitForInputIdleStub(this::waitForInputIdle, failures, arena));
        windowVtable.setAtIndex(ValueLayout.ADDRESS, 6L, bindings.createIwindowProviderGetCanMaximizeStub(this::getCanMaximize, failures, arena));
        windowVtable.setAtIndex(ValueLayout.ADDRESS, 7L, bindings.createIwindowProviderGetCanMinimizeStub(this::getCanMinimize, failures, arena));
        windowVtable.setAtIndex(ValueLayout.ADDRESS, 8L, bindings.createIwindowProviderGetIsModalStub(this::getIsModal, failures, arena));
        windowVtable.setAtIndex(ValueLayout.ADDRESS, 9L, bindings.createIwindowProviderGetWindowVisualStateStub(this::getWindowVisualState, failures, arena));
        windowVtable.setAtIndex(
                ValueLayout.ADDRESS,
                10L,
                bindings.createIwindowProviderGetWindowInteractionStateStub(this::getWindowInteractionState, failures, arena)
        );
        windowVtable.setAtIndex(ValueLayout.ADDRESS, 11L, bindings.createIwindowProviderGetIsTopmostStub(this::getIsTopmost, failures, arena));
        this.toggleObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment toggleVtable = arena.allocate(ValueLayout.ADDRESS, 5);
        toggleObject.set(ValueLayout.ADDRESS, 0L, toggleVtable);
        toggleVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryToggle, failures, arena));
        toggleVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        toggleVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        toggleVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createItoggleProviderToggleStub(this::toggle, failures, arena));
        toggleVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createItoggleProviderGetToggleStateStub(this::getToggleState, failures, arena));
        this.rangeObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment rangeVtable = arena.allocate(ValueLayout.ADDRESS, 8);
        rangeObject.set(ValueLayout.ADDRESS, 0L, rangeVtable);
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryRange, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIrangeValueProviderSetValueStub(this::setRangeValue, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIrangeValueProviderGetValueStub(this::getRangeValue, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 5L, bindings.createIrangeValueProviderGetIsReadOnlyStub(this::getRangeReadOnly, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 6L, bindings.createIrangeValueProviderGetMaximumStub(this::getRangeMaximum, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 7L, bindings.createIrangeValueProviderGetMinimumStub(this::getRangeMinimum, failures, arena));
        this.expandObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment expandVtable = arena.allocate(ValueLayout.ADDRESS, 6);
        expandObject.set(ValueLayout.ADDRESS, 0L, expandVtable);
        expandVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryExpand, failures, arena));
        expandVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        expandVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        expandVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIexpandCollapseProviderExpandStub(this::expand, failures, arena));
        expandVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIexpandCollapseProviderCollapseStub(this::collapse, failures, arena));
        expandVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIexpandCollapseProviderGetExpandCollapseStateStub(this::getExpandState, failures, arena)
        );
        this.selectionObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment selectionVtable = arena.allocate(ValueLayout.ADDRESS, 8);
        selectionObject.set(ValueLayout.ADDRESS, 0L, selectionVtable);
        selectionVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::querySelection, failures, arena));
        selectionVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        selectionVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        selectionVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIselectionItemProviderSelectStub(this::selectItem, failures, arena));
        selectionVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIselectionItemProviderAddToSelectionStub(this::addItemToSelection, failures, arena)
        );
        selectionVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIselectionItemProviderRemoveFromSelectionStub(this::removeItemFromSelection, failures, arena)
        );
        selectionVtable.setAtIndex(
                ValueLayout.ADDRESS,
                6L,
                bindings.createIselectionItemProviderGetIsSelectedStub(this::getItemSelected, failures, arena)
        );
        selectionVtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createIselectionItemProviderGetSelectionContainerStub(this::getSelectionContainer, failures, arena)
        );
        this.gridObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment gridVtable = arena.allocate(ValueLayout.ADDRESS, 6);
        gridObject.set(ValueLayout.ADDRESS, 0L, gridVtable);
        gridVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryGrid, failures, arena));
        gridVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        gridVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        gridVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIgridProviderGetRowCountStub(this::getGridRowCount, failures, arena));
        gridVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIgridProviderGetColumnCountStub(this::getGridColumnCount, failures, arena));
        gridVtable.setAtIndex(ValueLayout.ADDRESS, 5L, bindings.createIgridProviderGetItemStub(this::getGridItem, failures, arena));
        this.tableObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment tableVtable = arena.allocate(ValueLayout.ADDRESS, 6);
        tableObject.set(ValueLayout.ADDRESS, 0L, tableVtable);
        tableVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryTable, failures, arena));
        tableVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        tableVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        tableVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createItableProviderGetRowHeadersStub(this::getRowHeaders, failures, arena));
        tableVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createItableProviderGetColumnHeadersStub(this::getColumnHeaders, failures, arena));
        tableVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createItableProviderGetRowOrColumnMajorStub(this::getRowOrColumnMajor, failures, arena)
        );
        this.gridItemObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment gridItemVtable = arena.allocate(ValueLayout.ADDRESS, 8);
        gridItemObject.set(ValueLayout.ADDRESS, 0L, gridItemVtable);
        gridItemVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryGridItem, failures, arena));
        gridItemVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        gridItemVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        gridItemVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIgridItemProviderGetRowStub(this::getCellRow, failures, arena));
        gridItemVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIgridItemProviderGetColumnStub(this::getCellColumn, failures, arena));
        gridItemVtable.setAtIndex(ValueLayout.ADDRESS, 5L, bindings.createIgridItemProviderGetRowSpanStub(this::getCellRowSpan, failures, arena));
        gridItemVtable.setAtIndex(ValueLayout.ADDRESS, 6L, bindings.createIgridItemProviderGetColumnSpanStub(this::getCellColumnSpan, failures, arena));
        gridItemVtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createIgridItemProviderGetContainingGridStub(this::getContainingGrid, failures, arena)
        );
        this.tableItemObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment tableItemVtable = arena.allocate(ValueLayout.ADDRESS, 5);
        tableItemObject.set(ValueLayout.ADDRESS, 0L, tableItemVtable);
        tableItemVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryTableItem, failures, arena));
        tableItemVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        tableItemVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        tableItemVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createItableItemProviderGetRowHeaderItemsStub(this::getRowHeaderItems, failures, arena)
        );
        tableItemVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createItableItemProviderGetColumnHeaderItemsStub(this::getColumnHeaderItems, failures, arena)
        );
        this.fetchedCellObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment fetchedVtable = arena.allocate(ValueLayout.ADDRESS, 8);
        fetchedCellObject.set(ValueLayout.ADDRESS, 0L, fetchedVtable);
        fetchedVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryFetchedCell, failures, arena));
        fetchedVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        fetchedVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        fetchedVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIgridItemProviderGetRowStub(this::getFetchedRow, failures, arena));
        fetchedVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIgridItemProviderGetColumnStub(this::getFetchedColumn, failures, arena));
        fetchedVtable.setAtIndex(ValueLayout.ADDRESS, 5L, bindings.createIgridItemProviderGetRowSpanStub(this::getFetchedRowSpan, failures, arena));
        fetchedVtable.setAtIndex(ValueLayout.ADDRESS, 6L, bindings.createIgridItemProviderGetColumnSpanStub(this::getFetchedColumnSpan, failures, arena));
        fetchedVtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createIgridItemProviderGetContainingGridStub(this::getContainingGrid, failures, arena)
        );
        this.scrollObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment scrollVtable = arena.allocate(ValueLayout.ADDRESS, 11);
        scrollObject.set(ValueLayout.ADDRESS, 0L, scrollVtable);
        scrollVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryScroll, failures, arena));
        scrollVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        scrollVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        scrollVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIscrollProviderScrollStub(this::scrollByAmount, failures, arena));
        scrollVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIscrollProviderSetScrollPercentStub(this::setScrollPercent, failures, arena));
        scrollVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIscrollProviderGetHorizontalScrollPercentStub(this::getHorizontalScrollPercent, failures, arena)
        );
        scrollVtable.setAtIndex(
                ValueLayout.ADDRESS,
                6L,
                bindings.createIscrollProviderGetVerticalScrollPercentStub(this::getVerticalScrollPercent, failures, arena)
        );
        scrollVtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createIscrollProviderGetHorizontalViewSizeStub(this::getHorizontalViewSize, failures, arena)
        );
        scrollVtable.setAtIndex(
                ValueLayout.ADDRESS,
                8L,
                bindings.createIscrollProviderGetVerticalViewSizeStub(this::getVerticalViewSize, failures, arena)
        );
        scrollVtable.setAtIndex(
                ValueLayout.ADDRESS,
                9L,
                bindings.createIscrollProviderGetHorizontallyScrollableStub(this::getHorizontallyScrollable, failures, arena)
        );
        scrollVtable.setAtIndex(
                ValueLayout.ADDRESS,
                10L,
                bindings.createIscrollProviderGetVerticallyScrollableStub(this::getVerticallyScrollable, failures, arena)
        );
        this.textObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment textVtable = arena.allocate(ValueLayout.ADDRESS, 9);
        textObject.set(ValueLayout.ADDRESS, 0L, textVtable);
        textVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryText, failures, arena));
        textVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        textVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        textVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createItextProviderRangeFromPointStub(this::rangeFromPoint, failures, arena)
        );
        MemorySegment emptyRange = bindings.createItextProviderGetRangeStub(this::emptyRange, failures, arena);
        textVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createItextProviderGetRangeStub(this::selectionRange, failures, arena)
        );
        textVtable.setAtIndex(ValueLayout.ADDRESS, 5L, emptyRange);
        textVtable.setAtIndex(ValueLayout.ADDRESS, 6L, emptyRange);
        textVtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createItextProviderGetRangeStub(this::documentRange, failures, arena)
        );
        textVtable.setAtIndex(
                ValueLayout.ADDRESS,
                8L,
                bindings.createItextProviderGetSupportedTextSelectionStub(this::supportedTextSelection, failures, arena)
        );
        this.textRangeObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment textRangeVtable = arena.allocate(ValueLayout.ADDRESS, 21);
        textRangeObject.set(ValueLayout.ADDRESS, 0L, textRangeVtable);
        textRangeVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryTextRange, failures, arena));
        textRangeVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        textRangeVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        MemorySegment notImplemented = bindings.createItextRangeNotimplStub(this::notImplemented, failures, arena);
        MemorySegment cloneSlot = bindings.createItextProviderGetRangeStub(this::cloneRange, failures, arena);
        textRangeVtable.setAtIndex(ValueLayout.ADDRESS, 3L, cloneSlot);
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createItextRangeProviderCompareStub(this::compareRange, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createItextRangeProviderCompareEndpointsStub(this::compareEndpoints, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                6L,
                bindings.createItextRangeProviderExpandStub(this::expandToEnclosingUnit, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createItextRangeProviderFindAttributeStub(this::findAttribute, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                8L,
                bindings.createItextRangeProviderFindTextStub(this::findText, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                9L,
                bindings.createItextRangeProviderGetAttributeValueStub(this::getAttributeValue, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                10L,
                bindings.createItextRangeProviderGetBoundingRectanglesStub(this::getBoundingRectangles, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                11L,
                bindings.createItextProviderGetRangeStub(this::enclosingElement, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                12L,
                bindings.createItextRangeProviderGetTextStub(this::getText, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                13L,
                bindings.createItextRangeProviderMoveStub(this::moveRange, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                14L,
                bindings.createItextRangeProviderMoveEndpointByUnitStub(this::moveEndpointByUnit, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                15L,
                bindings.createItextRangeProviderMoveEndpointByRangeStub(this::moveEndpointByRange, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                16L,
                bindings.createItextRangeProviderSelectStub(this::selectRange, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                17L,
                bindings.createItextRangeProviderSelectStub(this::addToSelection, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                18L,
                bindings.createItextRangeProviderSelectStub(this::removeFromSelection, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                19L,
                bindings.createItextRangeProviderScrollIntoViewStub(this::scrollIntoView, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                20L,
                bindings.createItextRangeProviderGetChildrenStub(this::getChildren, failures, arena)
        );
    }

    /// Creates a provider for one semantics node.
    ///
    /// @param libraries the session libraries
    /// @param node the projected node
    /// @return the provider
    public static WindowsAutomationProvider of(WindowsLibraries libraries, SemanticsNode node) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(node, "node");
        return new WindowsAutomationProvider(libraries.bindings(), node);
    }

    /// Invokes `GetPropertyValue` through the generated COM vtable.
    ///
    /// @param propertyId the UIA property identifier
    /// @return the `VT_I4` payload
    public int invokePropertyValue(int propertyId) {
        requireOpen();
        MemorySegment getProperty = simpleVtable.getAtIndex(ValueLayout.ADDRESS, 5L);
        MemorySegment value = arena.allocate(Win32Layouts.VARIANT);
        value.fill((byte) 0);
        int result = Win32FfmBindings.invokeIrawElementProviderGetPropertyValuePointer(
                getProperty,
                simpleObject,
                propertyId,
                value
        );
        requireSuccess("GetPropertyValue", result);
        return value.get(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET);
    }

    /// Invokes `GetPatternProvider` through the generated COM vtable.
    ///
    /// @param patternId the UIA pattern identifier
    /// @return whether a pattern object was returned
    public boolean invokePatternProvider(int patternId) {
        requireOpen();
        MemorySegment getPattern = simpleVtable.getAtIndex(ValueLayout.ADDRESS, 4L);
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "GetPatternProvider",
                Win32FfmBindings.invokeIrawElementProviderGetPatternProviderPointer(
                        getPattern,
                        simpleObject,
                        patternId,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `IInvokeProvider::Invoke` through the generated COM vtable.
    ///
    /// @return the invoke count after the call
    public int invoke() {
        requireOpen();
        requireSuccess(
                "IInvokeProvider::Invoke",
                Win32FfmBindings.invokeIinvokeProviderInvokePointer(
                        functionAt(invokeObject, 3),
                        invokeObject
                )
        );
        return invokeCount;
    }

    /// Invokes `IScrollItemProvider::ScrollIntoView` through the generated COM vtable.
    ///
    /// @return the invocation count after the call
    public int invokeScrollItem() {
        requireOpen();
        requireSuccess(
                "IScrollItemProvider::ScrollIntoView",
                Win32FfmBindings.invokeIscrollItemProviderScrollIntoViewPointer(
                        functionAt(scrollItemObject, 3),
                        scrollItemObject
                )
        );
        return scrollItemCount;
    }

    /// Invokes `IValueProvider::SetValue` through the generated COM vtable.
    ///
    /// @param value the new string
    /// @return the stored string
    public String setValue(String value) {
        requireOpen();
        Objects.requireNonNull(value, "value");
        MemorySegment chars = arena.allocate((value.length() + 1L) * 2L);
        byte[] utf16 = value.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment.copy(utf16, 0, chars, ValueLayout.JAVA_BYTE, 0L, utf16.length);
        requireSuccess(
                "IValueProvider::SetValue",
                Win32FfmBindings.invokeIvalueProviderSetValuePointer(
                        functionAt(valueObject, 3),
                        valueObject,
                        chars
                )
        );
        return valueText;
    }

    /// Reads `IValueProvider::get_Value` through the generated COM vtable.
    ///
    /// @return the stored string
    public String value() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IValueProvider::get_Value",
                Win32FfmBindings.invokeIvalueProviderGetValuePointer(
                        functionAt(valueObject, 4),
                        valueObject,
                        result
                )
        );
        MemorySegment chars = result.get(ValueLayout.ADDRESS, 0L);
        if (chars.address() == 0L) {
            return "";
        }
        if (chars.byteSize() == 0L) {
            chars = chars.reinterpret(4096);
        }
        StringBuilder text = new StringBuilder();
        int limit = Math.toIntExact(Math.min(2048L, chars.byteSize() / 2L));
        for (int index = 0; index < limit; index++) {
            char unit = chars.getAtIndex(ValueLayout.JAVA_CHAR, index);
            if (unit == 0) {
                break;
            }
            text.append(unit);
        }
        return text.toString();
    }

    /// Reads `IValueProvider::get_IsReadOnly` through the generated COM vtable.
    ///
    /// @return whether the value is read-only
    public boolean valueReadOnly() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IValueProvider::get_IsReadOnly",
                Win32FfmBindings.invokeIvalueProviderGetIsReadOnlyPointer(
                        functionAt(valueObject, 5),
                        valueObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Invokes `IWindowProvider::SetVisualState` through the generated COM vtable.
    ///
    /// @param state the visual state
    /// @return the stored state
    public int setWindowVisualState(int state) {
        requireOpen();
        requireSuccess(
                "IWindowProvider::SetVisualState",
                Win32FfmBindings.invokeIwindowProviderSetVisualStatePointer(
                        functionAt(windowObject, 3),
                        windowObject,
                        state
                )
        );
        return windowVisualState;
    }

    /// Invokes `IWindowProvider::Close` through the generated COM vtable.
    ///
    /// @return the close count
    public int closeWindow() {
        requireOpen();
        requireSuccess(
                "IWindowProvider::Close",
                Win32FfmBindings.invokeIwindowProviderClosePointer(functionAt(windowObject, 4), windowObject)
        );
        return windowCloseCount;
    }

    /// Reads `IWindowProvider::get_CanMaximize` through the generated COM vtable.
    ///
    /// @return whether maximize is advertised
    public boolean canMaximize() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IWindowProvider::get_CanMaximize",
                Win32FfmBindings.invokeIwindowProviderGetCanMaximizePointer(
                        functionAt(windowObject, 6),
                        windowObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Invokes `IToggleProvider::Toggle` through the generated COM vtable.
    ///
    /// @return the new toggle state
    public int toggle() {
        requireOpen();
        requireSuccess(
                "IToggleProvider::Toggle",
                Win32FfmBindings.invokeItoggleProviderTogglePointer(
                        functionAt(toggleObject, 3),
                        toggleObject
                )
        );
        return toggleState;
    }

    /// Reads `IToggleProvider::get_ToggleState` through the generated COM vtable.
    ///
    /// @return the toggle state
    public int toggleState() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IToggleProvider::get_ToggleState",
                Win32FfmBindings.invokeItoggleProviderGetToggleStatePointer(
                        functionAt(toggleObject, 4),
                        toggleObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `IRangeValueProvider::SetValue` through the generated COM vtable.
    ///
    /// @param value the new value
    /// @return the stored value
    public double setRangeValue(double value) {
        requireOpen();
        requireSuccess(
                "IRangeValueProvider::SetValue",
                Win32FfmBindings.invokeIrangeValueProviderSetValuePointer(
                        functionAt(rangeObject, 3),
                        rangeObject,
                        value
                )
        );
        return rangeValue;
    }

    /// Invokes `IExpandCollapseProvider::Expand` through the generated COM vtable.
    ///
    /// @return the expand/collapse state after the call
    public int expand() {
        requireOpen();
        requireSuccess(
                "IExpandCollapseProvider::Expand",
                Win32FfmBindings.invokeIexpandCollapseProviderExpandPointer(
                        functionAt(expandObject, 3),
                        expandObject
                )
        );
        return expandState;
    }

    /// Invokes `IExpandCollapseProvider::Collapse` through the generated COM vtable.
    ///
    /// @return the expand/collapse state after the call
    public int collapse() {
        requireOpen();
        requireSuccess(
                "IExpandCollapseProvider::Collapse",
                Win32FfmBindings.invokeIexpandCollapseProviderCollapsePointer(
                        functionAt(expandObject, 4),
                        expandObject
                )
        );
        return expandState;
    }

    /// Reads `IExpandCollapseProvider::get_ExpandCollapseState` through the generated COM vtable.
    ///
    /// @return the expand/collapse state
    public int expandState() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IExpandCollapseProvider::get_ExpandCollapseState",
                Win32FfmBindings.invokeIexpandCollapseProviderGetExpandCollapseStatePointer(
                        functionAt(expandObject, 5),
                        expandObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ISelectionItemProvider::Select` through the generated COM vtable.
    ///
    /// @return whether the item is selected after the call
    public boolean selectItem() {
        requireOpen();
        requireSuccess(
                "ISelectionItemProvider::Select",
                Win32FfmBindings.invokeIselectionItemProviderSelectPointer(
                        functionAt(selectionObject, 3),
                        selectionObject
                )
        );
        return itemSelected;
    }

    /// Invokes `ISelectionItemProvider::RemoveFromSelection` through the generated COM vtable.
    ///
    /// @return whether the item is selected after the call
    public boolean removeItemFromSelection() {
        requireOpen();
        requireSuccess(
                "ISelectionItemProvider::RemoveFromSelection",
                Win32FfmBindings.invokeIselectionItemProviderRemoveFromSelectionPointer(
                        functionAt(selectionObject, 5),
                        selectionObject
                )
        );
        return itemSelected;
    }

    /// Reads `ISelectionItemProvider::get_IsSelected` through the generated COM vtable.
    ///
    /// @return whether the item is selected
    public boolean itemSelected() {
        requireOpen();
        MemorySegment selected = arena.allocate(ValueLayout.JAVA_INT);
        selected.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ISelectionItemProvider::get_IsSelected",
                Win32FfmBindings.invokeIselectionItemProviderGetIsSelectedPointer(
                        functionAt(selectionObject, 6),
                        selectionObject,
                        selected
                )
        );
        return selected.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Reads `IGridProvider::get_RowCount` through the generated COM vtable.
    ///
    /// @return the row count
    public int gridRowCount() {
        requireOpen();
        MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
        count.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IGridProvider::get_RowCount",
                Win32FfmBindings.invokeIgridProviderGetRowCountPointer(
                        functionAt(gridObject, 3),
                        gridObject,
                        count
                )
        );
        return count.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Reads `IGridProvider::get_ColumnCount` through the generated COM vtable.
    ///
    /// @return the column count
    public int gridColumnCount() {
        requireOpen();
        MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
        count.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IGridProvider::get_ColumnCount",
                Win32FfmBindings.invokeIgridProviderGetColumnCountPointer(
                        functionAt(gridObject, 4),
                        gridObject,
                        count
                )
        );
        return count.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `IGridProvider::GetItem` through the generated COM vtable.
    ///
    /// @param row the zero-based row
    /// @param column the zero-based column
    /// @return whether an item pointer was returned
    public boolean invokeGetItem(int row, int column) {
        requireOpen();
        MemorySegment item = arena.allocate(ValueLayout.ADDRESS);
        item.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IGridProvider::GetItem",
                Win32FfmBindings.invokeIgridProviderGetItemPointer(
                        functionAt(gridObject, 5),
                        gridObject,
                        row,
                        column,
                        item
                )
        );
        MemorySegment fetched = item.get(ValueLayout.ADDRESS, 0L);
        if (fetched.address() == 0L) {
            lastFetchedCell = MemorySegment.NULL;
            return false;
        }
        lastFetchedCell = fetched.byteSize() == 0L
                ? fetched.reinterpret(ValueLayout.ADDRESS.byteSize())
                : fetched;
        return true;
    }

    /// Reads `IGridItemProvider::get_Row` on the last fetched cell.
    ///
    /// @return the row
    public int invokeFetchedItemRow() {
        return readFetchedInt(3);
    }

    /// Reads `IGridItemProvider::get_Column` on the last fetched cell.
    ///
    /// @return the column
    public int invokeFetchedItemColumn() {
        return readFetchedInt(4);
    }

    /// Reads `IGridItemProvider::get_ContainingGrid` on the last fetched cell.
    ///
    /// @return whether a grid pointer was returned
    public boolean invokeFetchedContainingGrid() {
        requireOpen();
        if (lastFetchedCell.address() == 0L) {
            return false;
        }
        MemorySegment grid = arena.allocate(ValueLayout.ADDRESS);
        grid.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IGridItemProvider::get_ContainingGrid",
                Win32FfmBindings.invokeIgridItemProviderGetContainingGridPointer(
                        functionAt(lastFetchedCell, 7),
                        lastFetchedCell,
                        grid
                )
        );
        return grid.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `IGridItemProvider::get_Row` for this node.
    ///
    /// @return the row
    public int gridItemRow() {
        return readGridItemInt(3);
    }

    /// Reads `IGridItemProvider::get_Column` for this node.
    ///
    /// @return the column
    public int gridItemColumn() {
        return readGridItemInt(4);
    }

    /// Reads `ITableItemProvider::GetRowHeaderItems` for this node.
    ///
    /// @return the header count
    public int invokeRowHeaderItems() {
        return packedCount("ITableItemProvider::GetRowHeaderItems", tableItemObject, 3);
    }

    /// Reads `ITableProvider::GetRowHeaders` through the generated COM vtable.
    ///
    /// @return the header count
    public int invokeRowHeaders() {
        return packedCount("ITableProvider::GetRowHeaders", tableObject, 3);
    }

    /// Reads `ITableProvider::GetColumnHeaders` through the generated COM vtable.
    ///
    /// @return the header count
    public int invokeColumnHeaders() {
        return packedCount("ITableProvider::GetColumnHeaders", tableObject, 4);
    }

    /// Reads `ITableProvider::get_RowOrColumnMajor` through the generated COM vtable.
    ///
    /// @return the major-order identifier
    public int rowOrColumnMajor() {
        requireOpen();
        MemorySegment major = arena.allocate(ValueLayout.JAVA_INT);
        major.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITableProvider::get_RowOrColumnMajor",
                Win32FfmBindings.invokeItableProviderGetRowOrColumnMajorPointer(
                        functionAt(tableObject, 5),
                        tableObject,
                        major
                )
        );
        return major.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `IScrollProvider::SetScrollPercent` through the generated COM vtable.
    ///
    /// @param verticalPercent the new vertical percent
    /// @return the stored vertical percent
    public double setVerticalScrollPercent(double verticalPercent) {
        requireOpen();
        requireSuccess(
                "IScrollProvider::SetScrollPercent",
                Win32FfmBindings.invokeIscrollProviderSetScrollPercentPointer(
                        functionAt(scrollObject, 4),
                        scrollObject,
                        SCROLL_NO_AMOUNT,
                        verticalPercent
                )
        );
        return verticalScrollPercent;
    }

    /// Invokes `IScrollProvider::Scroll` through the generated COM vtable.
    ///
    /// @param verticalAmount a `ScrollAmount`
    /// @return the stored vertical percent
    public double scrollVertical(int verticalAmount) {
        requireOpen();
        requireSuccess(
                "IScrollProvider::Scroll",
                Win32FfmBindings.invokeIscrollProviderScrollPointer(
                        functionAt(scrollObject, 3),
                        scrollObject,
                        SCROLL_AMOUNT_NO_AMOUNT,
                        verticalAmount
                )
        );
        return verticalScrollPercent;
    }

    /// Reads `IScrollProvider::get_VerticalScrollPercent` through the generated COM vtable.
    ///
    /// @return the vertical percent
    public double verticalScrollPercent() {
        requireOpen();
        MemorySegment percent = arena.allocate(ValueLayout.JAVA_DOUBLE);
        percent.set(ValueLayout.JAVA_DOUBLE, 0L, 0.0);
        requireSuccess(
                "IScrollProvider::get_VerticalScrollPercent",
                Win32FfmBindings.invokeIscrollProviderGetVerticalScrollPercentPointer(
                        functionAt(scrollObject, 6),
                        scrollObject,
                        percent
                )
        );
        return percent.get(ValueLayout.JAVA_DOUBLE, 0L);
    }

    /// Reads `IScrollProvider::get_VerticallyScrollable` through the generated COM vtable.
    ///
    /// @return whether the viewport can move
    public boolean verticallyScrollable() {
        requireOpen();
        MemorySegment flag = arena.allocate(ValueLayout.JAVA_INT);
        flag.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IScrollProvider::get_VerticallyScrollable",
                Win32FfmBindings.invokeIscrollProviderGetVerticallyScrollablePointer(
                        functionAt(scrollObject, 10),
                        scrollObject,
                        flag
                )
        );
        return flag.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Invokes `IScrollProvider::SetScrollPercent` for the horizontal axis.
    ///
    /// @param horizontalPercent the new horizontal percent
    /// @return the stored horizontal percent
    public double setHorizontalScrollPercent(double horizontalPercent) {
        requireOpen();
        requireSuccess(
                "IScrollProvider::SetScrollPercent",
                Win32FfmBindings.invokeIscrollProviderSetScrollPercentPointer(
                        functionAt(scrollObject, 4),
                        scrollObject,
                        horizontalPercent,
                        SCROLL_NO_AMOUNT
                )
        );
        return horizontalScrollPercent;
    }

    /// Invokes `IScrollProvider::Scroll` for the horizontal axis.
    ///
    /// @param horizontalAmount a `ScrollAmount`
    /// @return the stored horizontal percent
    public double scrollHorizontal(int horizontalAmount) {
        requireOpen();
        requireSuccess(
                "IScrollProvider::Scroll",
                Win32FfmBindings.invokeIscrollProviderScrollPointer(
                        functionAt(scrollObject, 3),
                        scrollObject,
                        horizontalAmount,
                        SCROLL_AMOUNT_NO_AMOUNT
                )
        );
        return horizontalScrollPercent;
    }

    /// Reads `IScrollProvider::get_HorizontalScrollPercent` through the generated COM vtable.
    ///
    /// @return the horizontal percent
    public double horizontalScrollPercent() {
        requireOpen();
        MemorySegment percent = arena.allocate(ValueLayout.JAVA_DOUBLE);
        percent.set(ValueLayout.JAVA_DOUBLE, 0L, 0.0);
        requireSuccess(
                "IScrollProvider::get_HorizontalScrollPercent",
                Win32FfmBindings.invokeIscrollProviderGetHorizontalScrollPercentPointer(
                        functionAt(scrollObject, 5),
                        scrollObject,
                        percent
                )
        );
        return percent.get(ValueLayout.JAVA_DOUBLE, 0L);
    }

    /// Reads `IScrollProvider::get_HorizontallyScrollable` through the generated COM vtable.
    ///
    /// @return whether the viewport can move
    public boolean horizontallyScrollable() {
        requireOpen();
        MemorySegment flag = arena.allocate(ValueLayout.JAVA_INT);
        flag.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IScrollProvider::get_HorizontallyScrollable",
                Win32FfmBindings.invokeIscrollProviderGetHorizontallyScrollablePointer(
                        functionAt(scrollObject, 9),
                        scrollObject,
                        flag
                )
        );
        return flag.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Reads `IRangeValueProvider::get_Value` through the generated COM vtable.
    ///
    /// @return the stored value
    public double rangeValue() {
        requireOpen();
        MemorySegment value = arena.allocate(ValueLayout.JAVA_DOUBLE);
        value.set(ValueLayout.JAVA_DOUBLE, 0L, 0.0);
        requireSuccess(
                "IRangeValueProvider::get_Value",
                Win32FfmBindings.invokeIrangeValueProviderGetValuePointer(
                        functionAt(rangeObject, 4),
                        rangeObject,
                        value
                )
        );
        return value.get(ValueLayout.JAVA_DOUBLE, 0L);
    }

    /// Invokes `ITextProvider::get_DocumentRange` through the generated COM vtable.
    ///
    /// @return whether a document range was returned
    public boolean invokeDocumentRange() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextProvider::get_DocumentRange",
                Win32FfmBindings.invokeItextProviderGetRangePointer(
                        functionAt(textObject, 7),
                        textObject,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `ITextProvider::get_SupportedTextSelection` through the generated COM vtable.
    ///
    /// @return the supported-selection identifier
    public int invokeSupportedTextSelection() {
        requireOpen();
        MemorySegment value = arena.allocate(ValueLayout.JAVA_INT);
        value.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITextProvider::get_SupportedTextSelection",
                Win32FfmBindings.invokeItextProviderGetSupportedTextSelectionPointer(
                        functionAt(textObject, 8),
                        textObject,
                        value
                )
        );
        return value.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ITextRangeProvider::Clone` through the generated COM vtable.
    ///
    /// @return whether a range object was returned
    public boolean invokeClone() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextRangeProvider::Clone",
                Win32FfmBindings.invokeItextProviderGetRangePointer(
                        functionAt(textRangeObject, 3),
                        textRangeObject,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `ITextRangeProvider::Compare` through the generated COM vtable.
    ///
    /// @return whether the range compared equal to itself
    public boolean invokeCompareSelf() {
        requireOpen();
        MemorySegment equal = arena.allocate(ValueLayout.JAVA_INT);
        equal.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITextRangeProvider::Compare",
                Win32FfmBindings.invokeItextRangeProviderComparePointer(
                        functionAt(textRangeObject, 4),
                        textRangeObject,
                        textRangeObject,
                        equal
                )
        );
        return equal.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Invokes `ITextRangeProvider::GetEnclosingElement` through the generated COM vtable.
    ///
    /// @return whether the raw element provider was returned
    public boolean invokeEnclosingElement() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextRangeProvider::GetEnclosingElement",
                Win32FfmBindings.invokeItextProviderGetRangePointer(
                        functionAt(textRangeObject, 11),
                        textRangeObject,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `ITextRangeProvider::GetText` through the generated COM vtable.
    ///
    /// @param maxLength the maximum UTF-16 length, or `-1` for the full document
    /// @return the document text
    public String invokeGetText(int maxLength) {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextRangeProvider::GetText",
                Win32FfmBindings.invokeItextRangeProviderGetTextPointer(
                        functionAt(textRangeObject, 12),
                        textRangeObject,
                        maxLength,
                        result
                )
        );
        MemorySegment chars = result.get(ValueLayout.ADDRESS, 0L);
        if (chars.address() == 0L) {
            return "";
        }
        long available = chars.byteSize();
        if (available == 0L) {
            chars = chars.reinterpret(4096);
            available = 4096;
        }
        int limit = Math.toIntExact(Math.min(2048L, available / 2L));
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < limit; index++) {
            char unit = chars.getAtIndex(ValueLayout.JAVA_CHAR, index);
            if (unit == 0) {
                break;
            }
            text.append(unit);
        }
        return text.toString();
    }

    /// Invokes `ITextRangeProvider::ExpandToEnclosingUnit` through the generated COM vtable.
    ///
    /// @param unit a `TextUnit` identifier
    /// @return the range after expansion
    public SemanticsTextRange invokeExpandToEnclosingUnit(int unit) {
        requireOpen();
        requireSuccess(
                "ITextRangeProvider::ExpandToEnclosingUnit",
                Win32FfmBindings.invokeItextRangeProviderExpandPointer(
                        functionAt(textRangeObject, 6),
                        textRangeObject,
                        unit
                )
        );
        return new SemanticsTextRange(rangeStart, rangeEnd, rangeEnd);
    }

    /// Reads `ITextRangeProvider::GetBoundingRectangles` through the generated COM vtable.
    ///
    /// The first-stable encoding is a count-prefixed double quad `{count, x, y, width, height}`
    /// rather than an oleaut32 `SAFEARRAY`.
    ///
    /// @return the node's layout rectangle
    public double[] invokeGetBoundingRectangles() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextRangeProvider::GetBoundingRectangles",
                Win32FfmBindings.invokeItextRangeProviderGetBoundingRectanglesPointer(
                        functionAt(textRangeObject, 10),
                        textRangeObject,
                        result
                )
        );
        MemorySegment block = result.get(ValueLayout.ADDRESS, 0L);
        if (block.address() == 0L) {
            return new double[0];
        }
        block = block.byteSize() == 0L ? block.reinterpret(40) : block;
        int count = block.get(ValueLayout.JAVA_INT, 0L);
        if (count < 1) {
            return new double[0];
        }
        return new double[] {
                block.get(ValueLayout.JAVA_DOUBLE, 8L),
                block.get(ValueLayout.JAVA_DOUBLE, 16L),
                block.get(ValueLayout.JAVA_DOUBLE, 24L),
                block.get(ValueLayout.JAVA_DOUBLE, 32L)
        };
    }

    /// Invokes `ITextRangeProvider::Move` through the generated COM vtable.
    ///
    /// @param unit a `TextUnit` identifier
    /// @param count the signed unit count
    /// @return the number of units moved
    public int invokeMove(int unit, int count) {
        requireOpen();
        MemorySegment moved = arena.allocate(ValueLayout.JAVA_INT);
        moved.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITextRangeProvider::Move",
                Win32FfmBindings.invokeItextRangeProviderMovePointer(
                        functionAt(textRangeObject, 13),
                        textRangeObject,
                        unit,
                        count,
                        moved
                )
        );
        return moved.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ITextRangeProvider::CompareEndpoints` through the generated COM vtable.
    ///
    /// @param endpoint this range endpoint
    /// @param targetEndpoint the compared endpoint on this same range object
    /// @return negative, zero, or positive
    public int invokeCompareEndpoints(int endpoint, int targetEndpoint) {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.JAVA_INT);
        result.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITextRangeProvider::CompareEndpoints",
                Win32FfmBindings.invokeItextRangeProviderCompareEndpointsPointer(
                        functionAt(textRangeObject, 5),
                        textRangeObject,
                        endpoint,
                        textRangeObject,
                        targetEndpoint,
                        result
                )
        );
        return result.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ITextRangeProvider::FindText` through the generated COM vtable.
    ///
    /// `needle` is encoded as an `int32` UTF-16 unit count followed by those units.
    ///
    /// @param needle the search text
    /// @param backward whether to search toward the start
    /// @return whether a match range was returned
    public boolean invokeFindText(String needle, boolean backward) {
        requireOpen();
        Objects.requireNonNull(needle, "needle");
        MemorySegment packed = arena.allocate(4L + (long) needle.length() * 2L);
        packed.set(ValueLayout.JAVA_INT, 0L, needle.length());
        for (int index = 0; index < needle.length(); index++) {
            packed.set(ValueLayout.JAVA_CHAR, 4L + (long) index * 2L, needle.charAt(index));
        }
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextRangeProvider::FindText",
                Win32FfmBindings.invokeItextRangeProviderFindTextPointer(
                        functionAt(textRangeObject, 8),
                        textRangeObject,
                        packed,
                        backward ? 1 : 0,
                        0,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `ITextRangeProvider::MoveEndpointByUnit` through the generated COM vtable.
    ///
    /// @param endpoint the endpoint to move
    /// @param unit a `TextUnit` identifier
    /// @param count the signed unit count
    /// @return the number of units moved
    public int invokeMoveEndpointByUnit(int endpoint, int unit, int count) {
        requireOpen();
        MemorySegment moved = arena.allocate(ValueLayout.JAVA_INT);
        moved.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITextRangeProvider::MoveEndpointByUnit",
                Win32FfmBindings.invokeItextRangeProviderMoveEndpointByUnitPointer(
                        functionAt(textRangeObject, 14),
                        textRangeObject,
                        endpoint,
                        unit,
                        count,
                        moved
                )
        );
        return moved.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ITextRangeProvider::MoveEndpointByRange` through the generated COM vtable.
    ///
    /// @param endpoint the endpoint to move
    /// @param targetEndpoint the source endpoint on this same range object
    public void invokeMoveEndpointByRange(int endpoint, int targetEndpoint) {
        requireOpen();
        requireSuccess(
                "ITextRangeProvider::MoveEndpointByRange",
                Win32FfmBindings.invokeItextRangeProviderMoveEndpointByRangePointer(
                        functionAt(textRangeObject, 15),
                        textRangeObject,
                        endpoint,
                        textRangeObject,
                        targetEndpoint
                )
        );
    }

    /// Invokes `ITextRangeProvider::FindAttribute` through the generated COM vtable.
    ///
    /// First-stable faces publish no text attributes, so this always returns an empty range.
    ///
    /// @param attributeId a `TextAttributeId`
    /// @return whether a matching range was returned
    public boolean invokeFindAttribute(int attributeId) {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextRangeProvider::FindAttribute",
                Win32FfmBindings.invokeItextRangeProviderFindAttributePointer(
                        functionAt(textRangeObject, 7),
                        textRangeObject,
                        attributeId,
                        MemorySegment.NULL,
                        0,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `ITextRangeProvider::GetAttributeValue` through the generated COM vtable.
    ///
    /// @param attributeId a `TextAttributeId`
    /// @return the VARIANT `vt` field
    public int invokeGetAttributeValue(int attributeId) {
        requireOpen();
        MemorySegment variant = arena.allocate(Win32Layouts.VARIANT);
        variant.fill((byte) 0);
        requireSuccess(
                "ITextRangeProvider::GetAttributeValue",
                Win32FfmBindings.invokeItextRangeProviderGetAttributeValuePointer(
                        functionAt(textRangeObject, 9),
                        textRangeObject,
                        attributeId,
                        variant
                )
        );
        return variant.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET);
    }

    /// Invokes `ITextRangeProvider::AddToSelection` through the generated COM vtable.
    public void invokeAddToSelection() {
        requireOpen();
        requireSuccess(
                "ITextRangeProvider::AddToSelection",
                Win32FfmBindings.invokeItextRangeProviderSelectPointer(
                        functionAt(textRangeObject, 17),
                        textRangeObject
                )
        );
    }

    /// Invokes `ITextRangeProvider::RemoveFromSelection` through the generated COM vtable.
    public void invokeRemoveFromSelection() {
        requireOpen();
        requireSuccess(
                "ITextRangeProvider::RemoveFromSelection",
                Win32FfmBindings.invokeItextRangeProviderSelectPointer(
                        functionAt(textRangeObject, 18),
                        textRangeObject
                )
        );
    }

    /// Invokes `ITextRangeProvider::ScrollIntoView` through the generated COM vtable.
    ///
    /// @param alignToTop whether to align the range to the top
    /// @return whether the call recorded a scroll
    public boolean invokeScrollIntoView(boolean alignToTop) {
        requireOpen();
        requireSuccess(
                "ITextRangeProvider::ScrollIntoView",
                Win32FfmBindings.invokeItextRangeProviderScrollIntoViewPointer(
                        functionAt(textRangeObject, 19),
                        textRangeObject,
                        alignToTop ? 1 : 0
                )
        );
        return scrolledIntoView;
    }

    /// Reads `ITextRangeProvider::GetChildren` through the generated COM vtable.
    ///
    /// The first-stable encoding is an `int32` child count. This provider publishes zero children.
    ///
    /// @return the child count
    public int invokeGetChildren() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextRangeProvider::GetChildren",
                Win32FfmBindings.invokeItextRangeProviderGetChildrenPointer(
                        functionAt(textRangeObject, 20),
                        textRangeObject,
                        result
                )
        );
        MemorySegment block = result.get(ValueLayout.ADDRESS, 0L);
        if (block.address() == 0L) {
            return -1;
        }
        block = block.byteSize() == 0L ? block.reinterpret(4) : block;
        return block.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ITextRangeProvider::Select` through the generated COM vtable.
    public void invokeSelect() {
        requireOpen();
        requireSuccess(
                "ITextRangeProvider::Select",
                Win32FfmBindings.invokeItextRangeProviderSelectPointer(
                        functionAt(textRangeObject, 16),
                        textRangeObject
                )
        );
    }

    /// Invokes `ITextProvider::GetSelection` through the generated COM vtable.
    ///
    /// @return whether a selected range was returned
    public boolean invokeGetSelection() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextProvider::GetSelection",
                Win32FfmBindings.invokeItextProviderGetRangePointer(
                        functionAt(textObject, 4),
                        textObject,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Returns the current range endpoints after the last vtable call.
    ///
    /// @return the range
    public SemanticsTextRange currentRange() {
        return new SemanticsTextRange(rangeStart, rangeEnd, rangeEnd);
    }

    /// Returns the number of successful invoke calls.
    ///
    /// @return the count
    public int invokeCount() {
        return invokeCount;
    }

    /// Releases this owner's COM reference.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        release(simpleObject);
        arena.close();
    }

    /// Implements `IRawElementProviderSimple::QueryInterface`.
    private int queryInterface(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IRAW_ELEMENT_PROVIDER_SIMPLE, simpleObject);
    }

    /// Implements Invoke QI.
    private int queryInvoke(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IINVOKE_PROVIDER, invokeObject);
    }

    /// Implements Toggle QI.
    private int queryToggle(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITOGGLE_PROVIDER, toggleObject);
    }

    /// Implements RangeValue QI.
    private int queryRange(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IRANGE_VALUE_PROVIDER, rangeObject);
    }

    /// Implements ExpandCollapse QI.
    private int queryExpand(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IEXPAND_COLLAPSE_PROVIDER, expandObject);
    }

    /// Implements SelectionItem QI.
    private int querySelection(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ISELECTION_ITEM_PROVIDER, selectionObject);
    }

    /// Implements Grid QI.
    private int queryGrid(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IGRID_PROVIDER, gridObject);
    }

    /// Implements Table QI.
    private int queryTable(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITABLE_PROVIDER, tableObject);
    }

    /// Implements GridItem QI.
    private int queryGridItem(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IGRID_ITEM_PROVIDER, gridItemObject);
    }

    /// Implements TableItem QI.
    private int queryTableItem(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITABLE_ITEM_PROVIDER, tableItemObject);
    }

    /// Implements fetched-cell GridItem QI.
    private int queryFetchedCell(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IGRID_ITEM_PROVIDER, fetchedCellObject);
    }

    /// Implements Scroll QI.
    private int queryScroll(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ISCROLL_PROVIDER, scrollObject);
    }

    /// Implements ScrollItem QI.
    private int queryScrollItem(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ISCROLL_ITEM_PROVIDER, scrollItemObject);
    }

    /// Implements Value QI.
    private int queryValue(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IVALUE_PROVIDER, valueObject);
    }

    /// Implements Window QI.
    private int queryWindow(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IWINDOW_PROVIDER, windowObject);
    }

    /// Implements Text QI.
    private int queryText(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITEXT_PROVIDER, textObject);
    }

    /// Implements TextRange QI.
    private int queryTextRange(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITEXT_RANGE_PROVIDER, textRangeObject);
    }

    /// Shared QI implementation for one identity.
    private int query(MemorySegment interfaceId, MemorySegment result, UUID identity, MemorySegment object) {
        if (result.address() == 0L) {
            return E_POINTER;
        }
        if (WindowsCom.matches(interfaceId, IUNKNOWN) || WindowsCom.matches(interfaceId, identity)) {
            result.set(ValueLayout.ADDRESS, 0L, object);
            addRef(object);
            return S_OK;
        }
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return E_NOINTERFACE;
    }

    /// Implements `IUnknown::AddRef`.
    private int addRef(MemorySegment self) {
        references = Math.incrementExact(references);
        return references;
    }

    /// Implements `IUnknown::Release`.
    private int release(MemorySegment self) {
        references = Math.max(0, references - 1);
        return references;
    }

    /// Implements `IRawElementProviderSimple::GetPatternProvider`.
    private int getPatternProvider(MemorySegment self, int patternId, MemorySegment provider) {
        if (provider.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment out = provider.reinterpret(ValueLayout.ADDRESS.byteSize());
        MemorySegment selected = MemorySegment.NULL;
        if (patternId == UIA_INVOKE_PATTERN_ID && node.actions().contains(SemanticsAction.ACTIVATE)) {
            selected = invokeObject;
        } else if (patternId == UIA_TOGGLE_PATTERN_ID && node.role() == SemanticsRole.TOGGLE) {
            selected = toggleObject;
        } else if (patternId == UIA_RANGE_VALUE_PATTERN_ID && node.rangeValue() != null) {
            selected = rangeObject;
        } else if (patternId == UIA_EXPAND_COLLAPSE_PATTERN_ID && expandable(node)) {
            selected = expandObject;
        } else if (patternId == UIA_SELECTION_ITEM_PATTERN_ID && selectableItem(node)) {
            selected = selectionObject;
        } else if (patternId == UIA_GRID_PATTERN_ID && node.grid() != null) {
            selected = gridObject;
        } else if (patternId == UIA_TABLE_PATTERN_ID && node.grid() != null) {
            selected = tableObject;
        } else if (patternId == UIA_GRID_ITEM_PATTERN_ID && node.gridItem() != null) {
            selected = gridItemObject;
        } else if (patternId == UIA_TABLE_ITEM_PATTERN_ID && node.gridItem() != null) {
            selected = tableItemObject;
        } else if (patternId == UIA_SCROLL_PATTERN_ID && node.scroll() != null) {
            selected = scrollObject;
        } else if (patternId == UIA_SCROLL_ITEM_PATTERN_ID
                && node.actions().contains(SemanticsAction.SCROLL_INTO_VIEW)) {
            selected = scrollItemObject;
        } else if (patternId == UIA_VALUE_PATTERN_ID
                && (node.role() == SemanticsRole.TEXT_FIELD || node.role() == SemanticsRole.TEXT_AREA)) {
            selected = valueObject;
        } else if (patternId == UIA_WINDOW_PATTERN_ID && node.role() == SemanticsRole.DIALOG) {
            selected = windowObject;
        } else if (patternId == UIA_TEXT_PATTERN_ID && node.textRange() != null) {
            selected = textObject;
        }
        out.set(ValueLayout.ADDRESS, 0L, selected);
        if (selected.address() != 0L) {
            addRef(selected);
        }
        return S_OK;
    }

    /// Implements `IRawElementProviderSimple::GetPropertyValue`.
    private int getPropertyValue(MemorySegment self, int propertyId, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment variant = value.reinterpret(Win32Layouts.VARIANT.byteSize());
        variant.fill((byte) 0);
        if (propertyId == UIA_CONTROL_TYPE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, controlTypeId(node));
        } else if (propertyId == UIA_LIVE_SETTING_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, liveSettingId(node));
        }
        return S_OK;
    }

    /// Implements `IInvokeProvider::Invoke`.
    private int invoke(MemorySegment self) {
        invokeCount++;
        return S_OK;
    }

    /// Implements `IScrollItemProvider::ScrollIntoView`.
    private int scrollItemIntoView(MemorySegment self) {
        scrollItemCount++;
        return S_OK;
    }

    /// Implements `IValueProvider::SetValue`.
    private int setValue(MemorySegment self, MemorySegment value) {
        if (value.address() == 0L) {
            valueText = "";
            return S_OK;
        }
        MemorySegment chars = value.reinterpret(256);
        StringBuilder text = new StringBuilder();
        int limit = 64;
        for (int index = 0; index < limit; index++) {
            char unit = chars.getAtIndex(ValueLayout.JAVA_CHAR, index);
            if (unit == 0) {
                break;
            }
            text.append(unit);
        }
        valueText = text.toString();
        return S_OK;
    }

    /// Implements `IValueProvider::get_Value`.
    private int getValue(MemorySegment self, MemorySegment result) {
        if (result.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment chars = arena.allocate((valueText.length() + 1L) * 2L);
        byte[] utf16 = valueText.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment.copy(utf16, 0, chars, ValueLayout.JAVA_BYTE, 0L, utf16.length);
        result.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, chars);
        return S_OK;
    }

    /// Implements `IValueProvider::get_IsReadOnly`.
    private int getValueReadOnly(MemorySegment self, MemorySegment state) {
        return writeInt(state, 0);
    }

    /// Implements `IWindowProvider::SetVisualState`.
    private int setVisualState(MemorySegment self, int state) {
        windowVisualState = state;
        return S_OK;
    }

    /// Implements `IWindowProvider::Close`.
    private int closeWindow(MemorySegment self) {
        windowCloseCount++;
        return S_OK;
    }

    /// Implements `IWindowProvider::WaitForInputIdle`.
    private int waitForInputIdle(MemorySegment self, int milliseconds, MemorySegment success) {
        return writeInt(success, 1);
    }

    /// Implements `IWindowProvider::get_CanMaximize`.
    private int getCanMaximize(MemorySegment self, MemorySegment value) {
        return writeInt(value, 1);
    }

    /// Implements `IWindowProvider::get_CanMinimize`.
    private int getCanMinimize(MemorySegment self, MemorySegment value) {
        return writeInt(value, 1);
    }

    /// Implements `IWindowProvider::get_IsModal`.
    private int getIsModal(MemorySegment self, MemorySegment value) {
        return writeInt(value, node.role() == SemanticsRole.DIALOG ? 1 : 0);
    }

    /// Implements `IWindowProvider::get_WindowVisualState`.
    private int getWindowVisualState(MemorySegment self, MemorySegment value) {
        return writeInt(value, windowVisualState);
    }

    /// Implements `IWindowProvider::get_WindowInteractionState`.
    private int getWindowInteractionState(MemorySegment self, MemorySegment value) {
        return writeInt(value, WINDOW_INTERACTION_READY);
    }

    /// Implements `IWindowProvider::get_IsTopmost`.
    private int getIsTopmost(MemorySegment self, MemorySegment value) {
        return writeInt(value, 0);
    }

    /// Implements `IToggleProvider::Toggle`.
    private int toggle(MemorySegment self) {
        toggleState = toggleState == TOGGLE_STATE_ON ? TOGGLE_STATE_OFF : TOGGLE_STATE_ON;
        return S_OK;
    }

    /// Implements `IToggleProvider::get_ToggleState`.
    private int getToggleState(MemorySegment self, MemorySegment state) {
        if (state.address() == 0L) {
            return E_POINTER;
        }
        state.set(ValueLayout.JAVA_INT, 0L, toggleState);
        return S_OK;
    }

    /// Implements `IGridProvider::get_RowCount`.
    private int getGridRowCount(MemorySegment self, MemorySegment count) {
        return writeInt(count, gridRows);
    }

    /// Implements `IGridProvider::get_ColumnCount`.
    private int getGridColumnCount(MemorySegment self, MemorySegment count) {
        return writeInt(count, gridColumns);
    }

    /// Implements `IGridProvider::GetItem`.
    private int getGridItem(MemorySegment self, int row, int column, MemorySegment item) {
        if (item.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment out = item.reinterpret(ValueLayout.ADDRESS.byteSize());
        if (row < 0 || column < 0 || row >= gridRows || column >= gridColumns) {
            out.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            return S_OK;
        }
        fetchedRow = row;
        fetchedColumn = column;
        out.set(ValueLayout.ADDRESS, 0L, fetchedCellObject);
        addRef(fetchedCellObject);
        return S_OK;
    }

    /// Implements `IGridItemProvider::get_Row` for this node.
    private int getCellRow(MemorySegment self, MemorySegment row) {
        return writeInt(row, cellRow);
    }

    /// Implements `IGridItemProvider::get_Column` for this node.
    private int getCellColumn(MemorySegment self, MemorySegment column) {
        return writeInt(column, cellColumn);
    }

    /// Implements `IGridItemProvider::get_RowSpan` for this node.
    private int getCellRowSpan(MemorySegment self, MemorySegment span) {
        return writeInt(span, cellRowSpan);
    }

    /// Implements `IGridItemProvider::get_ColumnSpan` for this node.
    private int getCellColumnSpan(MemorySegment self, MemorySegment span) {
        return writeInt(span, cellColumnSpan);
    }

    /// Implements `IGridItemProvider::get_Row` for a fetched cell.
    private int getFetchedRow(MemorySegment self, MemorySegment row) {
        return writeInt(row, fetchedRow);
    }

    /// Implements `IGridItemProvider::get_Column` for a fetched cell.
    private int getFetchedColumn(MemorySegment self, MemorySegment column) {
        return writeInt(column, fetchedColumn);
    }

    /// Implements `IGridItemProvider::get_RowSpan` for a fetched cell.
    private int getFetchedRowSpan(MemorySegment self, MemorySegment span) {
        return writeInt(span, 1);
    }

    /// Implements `IGridItemProvider::get_ColumnSpan` for a fetched cell.
    private int getFetchedColumnSpan(MemorySegment self, MemorySegment span) {
        return writeInt(span, 1);
    }

    /// Implements `IGridItemProvider::get_ContainingGrid`.
    private int getContainingGrid(MemorySegment self, MemorySegment grid) {
        if (grid.address() == 0L) {
            return E_POINTER;
        }
        grid.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, simpleObject);
        addRef(simpleObject);
        return S_OK;
    }

    /// Implements `ITableItemProvider::GetRowHeaderItems` as an empty list.
    private int getRowHeaderItems(MemorySegment self, MemorySegment headers) {
        return writeEmptyPacked(headers);
    }

    /// Implements `ITableItemProvider::GetColumnHeaderItems` as an empty list.
    private int getColumnHeaderItems(MemorySegment self, MemorySegment headers) {
        return writeEmptyPacked(headers);
    }

    /// Implements `ITableProvider::GetRowHeaders` as an empty list.
    private int getRowHeaders(MemorySegment self, MemorySegment headers) {
        return writeEmptyPacked(headers);
    }

    /// Implements `ITableProvider::GetColumnHeaders` as an empty list.
    private int getColumnHeaders(MemorySegment self, MemorySegment headers) {
        return writeEmptyPacked(headers);
    }

    /// Implements `ITableProvider::get_RowOrColumnMajor`.
    private int getRowOrColumnMajor(MemorySegment self, MemorySegment major) {
        return writeInt(major, ROW_OR_COLUMN_MAJOR_ROW);
    }

    /// Implements `IScrollProvider::Scroll`.
    private int scrollByAmount(MemorySegment self, int horizontalAmount, int verticalAmount) {
        if (verticallyScrollable && verticalAmount != SCROLL_AMOUNT_NO_AMOUNT) {
            verticalScrollPercent = clampPercent(verticalScrollPercent + scrollDelta(verticalAmount));
        }
        if (horizontallyScrollable && horizontalAmount != SCROLL_AMOUNT_NO_AMOUNT) {
            horizontalScrollPercent = clampPercent(horizontalScrollPercent + scrollDelta(horizontalAmount));
        }
        return S_OK;
    }

    /// Implements `IScrollProvider::SetScrollPercent`.
    private int setScrollPercent(MemorySegment self, double horizontalPercent, double verticalPercent) {
        if (verticalPercent != SCROLL_NO_AMOUNT) {
            if (!Double.isFinite(verticalPercent)) {
                return E_POINTER;
            }
            verticalScrollPercent = clampPercent(verticalPercent);
        }
        if (horizontalPercent != SCROLL_NO_AMOUNT) {
            if (!Double.isFinite(horizontalPercent)) {
                return E_POINTER;
            }
            horizontalScrollPercent = clampPercent(horizontalPercent);
        }
        return S_OK;
    }

    /// Implements `IScrollProvider::get_HorizontalScrollPercent`.
    private int getHorizontalScrollPercent(MemorySegment self, MemorySegment percent) {
        return writeDouble(percent, horizontallyScrollable ? horizontalScrollPercent : SCROLL_NO_AMOUNT);
    }

    /// Implements `IScrollProvider::get_VerticalScrollPercent`.
    private int getVerticalScrollPercent(MemorySegment self, MemorySegment percent) {
        return writeDouble(percent, verticalScrollPercent);
    }

    /// Implements `IScrollProvider::get_HorizontalViewSize`.
    private int getHorizontalViewSize(MemorySegment self, MemorySegment size) {
        return writeDouble(size, horizontalViewSize);
    }

    /// Implements `IScrollProvider::get_VerticalViewSize`.
    private int getVerticalViewSize(MemorySegment self, MemorySegment size) {
        return writeDouble(size, verticalViewSize);
    }

    /// Implements `IScrollProvider::get_HorizontallyScrollable`.
    private int getHorizontallyScrollable(MemorySegment self, MemorySegment scrollable) {
        return writeInt(scrollable, horizontallyScrollable ? 1 : 0);
    }

    /// Implements `IScrollProvider::get_VerticallyScrollable`.
    private int getVerticallyScrollable(MemorySegment self, MemorySegment scrollable) {
        return writeInt(scrollable, verticallyScrollable ? 1 : 0);
    }

    /// Implements `ISelectionItemProvider::Select`.
    private int selectItem(MemorySegment self) {
        itemSelected = true;
        return S_OK;
    }

    /// Implements `ISelectionItemProvider::AddToSelection` as single-selection select.
    private int addItemToSelection(MemorySegment self) {
        return selectItem(self);
    }

    /// Implements `ISelectionItemProvider::RemoveFromSelection`.
    private int removeItemFromSelection(MemorySegment self) {
        itemSelected = false;
        return S_OK;
    }

    /// Implements `ISelectionItemProvider::get_IsSelected`.
    private int getItemSelected(MemorySegment self, MemorySegment selected) {
        if (selected.address() == 0L) {
            return E_POINTER;
        }
        selected.set(ValueLayout.JAVA_INT, 0L, itemSelected ? 1 : 0);
        return S_OK;
    }

    /// Implements `ISelectionItemProvider::get_SelectionContainer` as no published container.
    private int getSelectionContainer(MemorySegment self, MemorySegment container) {
        if (container.address() == 0L) {
            return E_POINTER;
        }
        container.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return S_OK;
    }

    /// Implements `IExpandCollapseProvider::Expand`.
    private int expand(MemorySegment self) {
        if (expandState != EXPAND_COLLAPSE_STATE_LEAF) {
            expandState = EXPAND_COLLAPSE_STATE_EXPANDED;
        }
        return S_OK;
    }

    /// Implements `IExpandCollapseProvider::Collapse`.
    private int collapse(MemorySegment self) {
        if (expandState != EXPAND_COLLAPSE_STATE_LEAF) {
            expandState = EXPAND_COLLAPSE_STATE_COLLAPSED;
        }
        return S_OK;
    }

    /// Implements `IExpandCollapseProvider::get_ExpandCollapseState`.
    private int getExpandState(MemorySegment self, MemorySegment state) {
        if (state.address() == 0L) {
            return E_POINTER;
        }
        state.set(ValueLayout.JAVA_INT, 0L, expandState);
        return S_OK;
    }

    /// Implements `IRangeValueProvider::SetValue`.
    private int setRangeValue(MemorySegment self, double value) {
        if (!Double.isFinite(value)) {
            return E_POINTER;
        }
        rangeValue = value;
        return S_OK;
    }

    /// Implements `IRangeValueProvider::get_Value`.
    private int getRangeValue(MemorySegment self, MemorySegment value) {
        return writeDouble(value, rangeValue);
    }

    /// Implements `IRangeValueProvider::get_IsReadOnly`.
    private int getRangeReadOnly(MemorySegment self, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        value.set(ValueLayout.JAVA_INT, 0L, 0);
        return S_OK;
    }

    /// Implements `IRangeValueProvider::get_Maximum`.
    private int getRangeMaximum(MemorySegment self, MemorySegment value) {
        return writeDouble(value, 100.0);
    }

    /// Implements `IRangeValueProvider::get_Minimum`.
    private int getRangeMinimum(MemorySegment self, MemorySegment value) {
        return writeDouble(value, 0.0);
    }

    /// Implements `ITextProvider::RangeFromPoint` with an honest empty result.
    private int rangeFromPoint(MemorySegment self, double x, double y, MemorySegment range) {
        return emptyRange(self, range);
    }

    /// Writes a null range out-parameter.
    private int emptyRange(MemorySegment self, MemorySegment range) {
        if (range.address() == 0L) {
            return E_POINTER;
        }
        range.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return S_OK;
    }

    /// Implements `ITextProvider::get_DocumentRange`.
    private int documentRange(MemorySegment self, MemorySegment range) {
        if (range.address() == 0L) {
            return E_POINTER;
        }
        range.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, textRangeObject);
        addRef(textRangeObject);
        return S_OK;
    }

    /// Implements `ITextProvider::GetSelection` after [`#selectRange`].
    private int selectionRange(MemorySegment self, MemorySegment range) {
        if (!rangeSelected) {
            return emptyRange(self, range);
        }
        return documentRange(self, range);
    }

    /// Implements `ITextProvider::get_SupportedTextSelection`.
    private int supportedTextSelection(MemorySegment self, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        value.reinterpret(ValueLayout.JAVA_INT.byteSize()).set(ValueLayout.JAVA_INT, 0L, SUPPORTED_TEXT_SELECTION_SINGLE);
        return S_OK;
    }

    /// Implements unused `ITextRangeProvider` slots.
    private int notImplemented(MemorySegment self) {
        return E_NOTIMPL;
    }

    /// Implements `ITextRangeProvider::ExpandToEnclosingUnit`.
    private int expandToEnclosingUnit(MemorySegment self, int unit) {
        int length = node.label().length();
        if (unit == TEXT_UNIT_DOCUMENT) {
            rangeStart = 0;
            rangeEnd = length;
            return S_OK;
        }
        if (unit == TEXT_UNIT_CHARACTER && rangeStart == rangeEnd && rangeEnd < length) {
            rangeEnd = rangeStart + 1;
        }
        return S_OK;
    }

    /// Implements `ITextRangeProvider::GetBoundingRectangles`.
    private int getBoundingRectangles(MemorySegment self, MemorySegment rectangles) {
        if (rectangles.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment block = arena.allocate(40);
        block.set(ValueLayout.JAVA_INT, 0L, 1);
        block.set(ValueLayout.JAVA_DOUBLE, 8L, node.bounds().x());
        block.set(ValueLayout.JAVA_DOUBLE, 16L, node.bounds().y());
        block.set(ValueLayout.JAVA_DOUBLE, 24L, node.bounds().width());
        block.set(ValueLayout.JAVA_DOUBLE, 32L, node.bounds().height());
        rectangles.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, block);
        return S_OK;
    }

    /// Implements `ITextRangeProvider::CompareEndpoints`.
    private int compareEndpoints(
            MemorySegment self,
            int endpoint,
            MemorySegment other,
            int targetEndpoint,
            MemorySegment result
    ) {
        if (result.address() == 0L) {
            return E_POINTER;
        }
        int left = endpoint == TEXT_PATTERN_RANGE_ENDPOINT_END ? rangeEnd : rangeStart;
        int right = left;
        if (other.address() != 0L && other.address() == textRangeObject.address()) {
            right = targetEndpoint == TEXT_PATTERN_RANGE_ENDPOINT_END ? rangeEnd : rangeStart;
        }
        result.reinterpret(ValueLayout.JAVA_INT.byteSize()).set(ValueLayout.JAVA_INT, 0L, Integer.compare(left, right));
        return S_OK;
    }

    /// Implements `ITextRangeProvider::FindText`.
    private int findText(
            MemorySegment self,
            MemorySegment text,
            int backward,
            int ignoreCase,
            MemorySegment range
    ) {
        if (range.address() == 0L) {
            return E_POINTER;
        }
        if (text.address() == 0L) {
            return emptyRange(self, range);
        }
        MemorySegment packed = text.byteSize() == 0L ? text.reinterpret(4) : text;
        int units = packed.get(ValueLayout.JAVA_INT, 0L);
        if (units <= 0) {
            return emptyRange(self, range);
        }
        packed = packed.byteSize() < 4L + (long) units * 2L
                ? packed.reinterpret(4L + (long) units * 2L)
                : packed;
        StringBuilder needle = new StringBuilder(units);
        for (int index = 0; index < units; index++) {
            needle.append(packed.get(ValueLayout.JAVA_CHAR, 4L + (long) index * 2L));
        }
        String document = node.label();
        String haystack = ignoreCase != 0 ? document.toLowerCase(java.util.Locale.ROOT) : document;
        String search = ignoreCase != 0 ? needle.toString().toLowerCase(java.util.Locale.ROOT) : needle.toString();
        int found = backward != 0 ? haystack.lastIndexOf(search) : haystack.indexOf(search);
        if (found < 0) {
            return emptyRange(self, range);
        }
        rangeStart = found;
        rangeEnd = found + units;
        return documentRange(self, range);
    }

    /// Implements `ITextRangeProvider::MoveEndpointByUnit`.
    private int moveEndpointByUnit(
            MemorySegment self,
            int endpoint,
            int unit,
            int count,
            MemorySegment moved
    ) {
        if (moved.address() == 0L) {
            return E_POINTER;
        }
        int length = node.label().length();
        int current = endpoint == TEXT_PATTERN_RANGE_ENDPOINT_END ? rangeEnd : rangeStart;
        int next = current;
        if (unit == TEXT_UNIT_DOCUMENT) {
            if (count > 0) {
                next = length;
            } else if (count < 0) {
                next = 0;
            }
        } else if (unit == TEXT_UNIT_CHARACTER) {
            next = Math.min(length, Math.max(0, current + count));
        }
        int applied = next - current;
        if (endpoint == TEXT_PATTERN_RANGE_ENDPOINT_END) {
            rangeEnd = next;
            if (rangeEnd < rangeStart) {
                rangeStart = rangeEnd;
            }
        } else {
            rangeStart = next;
            if (rangeStart > rangeEnd) {
                rangeEnd = rangeStart;
            }
        }
        moved.reinterpret(ValueLayout.JAVA_INT.byteSize()).set(ValueLayout.JAVA_INT, 0L, applied);
        return S_OK;
    }

    /// Implements `ITextRangeProvider::MoveEndpointByRange`.
    private int moveEndpointByRange(
            MemorySegment self,
            int endpoint,
            MemorySegment other,
            int targetEndpoint
    ) {
        if (other.address() == 0L || other.address() != textRangeObject.address()) {
            return S_OK;
        }
        int value = targetEndpoint == TEXT_PATTERN_RANGE_ENDPOINT_END ? rangeEnd : rangeStart;
        if (endpoint == TEXT_PATTERN_RANGE_ENDPOINT_END) {
            rangeEnd = value;
            if (rangeEnd < rangeStart) {
                rangeStart = rangeEnd;
            }
        } else {
            rangeStart = value;
            if (rangeStart > rangeEnd) {
                rangeEnd = rangeStart;
            }
        }
        return S_OK;
    }

    /// Implements `ITextRangeProvider::Select`.
    private int selectRange(MemorySegment self) {
        rangeSelected = true;
        return S_OK;
    }

    /// Implements `ITextRangeProvider::FindAttribute` with no published attributes.
    private int findAttribute(
            MemorySegment self,
            int attributeId,
            MemorySegment value,
            int backward,
            MemorySegment range
    ) {
        return emptyRange(self, range);
    }

    /// Implements `ITextRangeProvider::GetAttributeValue` as `VT_EMPTY`.
    private int getAttributeValue(MemorySegment self, int attributeId, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment variant = value.reinterpret(Win32Layouts.VARIANT.byteSize());
        variant.fill((byte) 0);
        return S_OK;
    }

    /// Implements `ITextRangeProvider::AddToSelection` as single-selection select.
    private int addToSelection(MemorySegment self) {
        return selectRange(self);
    }

    /// Implements `ITextRangeProvider::RemoveFromSelection`.
    private int removeFromSelection(MemorySegment self) {
        rangeSelected = false;
        return S_OK;
    }

    /// Implements `ITextRangeProvider::ScrollIntoView`.
    private int scrollIntoView(MemorySegment self, int alignToTop) {
        scrolledIntoView = true;
        return S_OK;
    }

    /// Implements `ITextRangeProvider::GetChildren` as an empty child list.
    private int getChildren(MemorySegment self, MemorySegment children) {
        if (children.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment block = arena.allocate(4);
        block.set(ValueLayout.JAVA_INT, 0L, 0);
        children.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, block);
        return S_OK;
    }

    /// Implements `ITextRangeProvider::Move`.
    private int moveRange(MemorySegment self, int unit, int count, MemorySegment moved) {
        if (moved.address() == 0L) {
            return E_POINTER;
        }
        int length = node.label().length();
        int applied = 0;
        if (unit == TEXT_UNIT_DOCUMENT) {
            if (count > 0 && rangeEnd < length) {
                rangeStart = length;
                rangeEnd = length;
                applied = 1;
            } else if (count < 0 && rangeStart > 0) {
                rangeStart = 0;
                rangeEnd = 0;
                applied = -1;
            }
        } else if (unit == TEXT_UNIT_CHARACTER && count != 0) {
            int caret = rangeEnd;
            int next = Math.min(length, Math.max(0, caret + count));
            applied = next - caret;
            rangeStart = next;
            rangeEnd = next;
        }
        moved.reinterpret(ValueLayout.JAVA_INT.byteSize()).set(ValueLayout.JAVA_INT, 0L, applied);
        return S_OK;
    }

    /// Implements `ITextRangeProvider::Clone`.
    private int cloneRange(MemorySegment self, MemorySegment range) {
        return documentRange(self, range);
    }

    /// Implements `ITextRangeProvider::Compare`.
    private int compareRange(MemorySegment self, MemorySegment other, MemorySegment equal) {
        if (equal.address() == 0L) {
            return E_POINTER;
        }
        int same = other.address() != 0L && other.address() == textRangeObject.address() ? 1 : 0;
        equal.reinterpret(ValueLayout.JAVA_INT.byteSize()).set(ValueLayout.JAVA_INT, 0L, same);
        return S_OK;
    }

    /// Implements `ITextRangeProvider::GetEnclosingElement`.
    private int enclosingElement(MemorySegment self, MemorySegment element) {
        if (element.address() == 0L) {
            return E_POINTER;
        }
        element.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, simpleObject);
        addRef(simpleObject);
        return S_OK;
    }

    /// Implements `ITextRangeProvider::GetText`.
    private int getText(MemorySegment self, int maxLength, MemorySegment text) {
        if (text.address() == 0L) {
            return E_POINTER;
        }
        String document = node.label();
        int from = Math.min(rangeStart, document.length());
        int to = Math.min(rangeEnd, document.length());
        if (from > to) {
            from = to;
        }
        String slice = document.substring(from, to);
        int units = slice.length();
        if (maxLength >= 0 && maxLength < units) {
            units = maxLength;
        }
        MemorySegment block = arena.allocate(4L + ((long) units + 1L) * 2L);
        block.set(ValueLayout.JAVA_INT, 0L, units * 2);
        for (int index = 0; index < units; index++) {
            block.set(ValueLayout.JAVA_CHAR, 4L + (long) index * 2L, slice.charAt(index));
        }
        block.set(ValueLayout.JAVA_CHAR, 4L + (long) units * 2L, (char) 0);
        text.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, block.asSlice(4L));
        return S_OK;
    }

    /// Writes one double out-parameter.
    private static int writeDouble(MemorySegment value, double payload) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        value.set(ValueLayout.JAVA_DOUBLE, 0L, payload);
        return S_OK;
    }

    /// Writes one `int32` out-parameter.
    private static int writeInt(MemorySegment value, int payload) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        value.set(ValueLayout.JAVA_INT, 0L, payload);
        return S_OK;
    }

    /// Writes a count-prefixed empty COM array.
    private int writeEmptyPacked(MemorySegment output) {
        if (output.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment block = arena.allocate(4);
        block.set(ValueLayout.JAVA_INT, 0L, 0);
        output.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, block);
        return S_OK;
    }

    /// Reads a count-prefixed COM array produced by a generated vtable slot.
    private int packedCount(String name, MemorySegment object, int slot) {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                name,
                Win32FfmBindings.invokeItableProviderGetRowHeadersPointer(
                        functionAt(object, slot),
                        object,
                        result
                )
        );
        MemorySegment block = result.get(ValueLayout.ADDRESS, 0L);
        if (block.address() == 0L) {
            return -1;
        }
        block = block.byteSize() == 0L ? block.reinterpret(4) : block;
        return block.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Reads an `int32` property from the last fetched cell vtable.
    private int readFetchedInt(int slot) {
        requireOpen();
        if (lastFetchedCell.address() == 0L) {
            return -1;
        }
        MemorySegment value = arena.allocate(ValueLayout.JAVA_INT);
        value.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IGridItemProvider",
                Win32FfmBindings.invokeIgridItemProviderGetRowPointer(
                        functionAt(lastFetchedCell, slot),
                        lastFetchedCell,
                        value
                )
        );
        return value.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Reads an `int32` property from this node's grid-item vtable.
    private int readGridItemInt(int slot) {
        requireOpen();
        MemorySegment value = arena.allocate(ValueLayout.JAVA_INT);
        value.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IGridItemProvider",
                Win32FfmBindings.invokeIgridItemProviderGetRowPointer(
                        functionAt(gridItemObject, slot),
                        gridItemObject,
                        value
                )
        );
        return value.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Maps a `ScrollAmount` onto a percent delta.
    private static double scrollDelta(int amount) {
        if (amount == SCROLL_AMOUNT_LARGE_INCREMENT || amount == SCROLL_AMOUNT_SMALL_INCREMENT) {
            return amount == SCROLL_AMOUNT_LARGE_INCREMENT ? 25.0 : 10.0;
        }
        return amount == 3 ? -25.0 : -10.0;
    }

    /// Clamps a scroll percent into `[0, 100]`.
    private static double clampPercent(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 100.0) {
            return 100.0;
        }
        return value;
    }

    /// Maps the initial toggle state from semantics.
    private static int initialToggleState(SemanticsNode node) {
        if (node.selected() == null) {
            return TOGGLE_STATE_INDETERMINATE;
        }
        return node.selected() ? TOGGLE_STATE_ON : TOGGLE_STATE_OFF;
    }

    /// Returns whether the node publishes expand/collapse.
    private static boolean expandable(SemanticsNode node) {
        return node.role() == SemanticsRole.TREE_ITEM
                && node.actions().contains(SemanticsAction.INCREMENT);
    }

    /// Returns whether the node publishes a selection item.
    private static boolean selectableItem(SemanticsNode node) {
        return node.role() == SemanticsRole.RADIO || node.role() == SemanticsRole.TAB;
    }

    /// Maps the initial expand/collapse state from semantics.
    private static int initialExpandState(SemanticsNode node) {
        if (!expandable(node)) {
            return EXPAND_COLLAPSE_STATE_LEAF;
        }
        return EXPAND_COLLAPSE_STATE_EXPANDED;
    }

    /// Maps the semantics role onto a UIA control-type identifier.
    private static int controlTypeId(SemanticsNode node) {
        return switch (node.role()) {
            case BUTTON -> UIA_BUTTON_CONTROL_TYPE_ID;
            case TOGGLE -> 50002;
            case CHECKBOX -> 50002;
            case RADIO -> 50013;
            case SLIDER -> 50015;
            case SCROLLBAR -> 50014;
            case PROGRESS -> 50010;
            case TEXT_FIELD, TEXT_AREA -> 50004;
            case LIST -> 50008;
            case TABLE -> 50036;
            case TABLE_ROW, TABLE_CELL -> 50029;
            case TEXT -> 50020;
            case STATUS -> UIA_STATUS_BAR_CONTROL_TYPE_ID;
            case NONE -> 50033;
            case POPUP -> 50033;
            case MENU -> 50009;
            case MENU_ITEM -> 50011;
            case DIALOG -> 50032;
            case TOOLTIP -> 50022;
            case TAB_LIST -> 50018;
            case TAB -> 50019;
            case TAB_PANEL -> 50033;
            case SPLIT_PANE -> 50033;
            case TREE -> 50023;
            case TREE_ITEM -> 50024;
        };
    }

    /// Maps live-region politeness onto a UIA LiveSetting identifier.
    ///
    /// @param node the semantics node
    /// @return the live-setting identifier
    private static int liveSettingId(SemanticsNode node) {
        return switch (node.liveRegion()) {
            case OFF -> LIVE_SETTING_OFF;
            case POLITE -> LIVE_SETTING_POLITE;
            case ASSERTIVE -> LIVE_SETTING_ASSERTIVE;
        };
    }

    /// Reads one vtable slot from a COM object.
    private static MemorySegment functionAt(MemorySegment object, int slot) {
        MemorySegment vtable = object.get(ValueLayout.ADDRESS, 0L)
                .reinterpret(ValueLayout.ADDRESS.byteSize() * (slot + 1L));
        return vtable.getAtIndex(ValueLayout.ADDRESS, slot);
    }

    /// Rejects a failing HRESULT and contained callback failures.
    private void requireSuccess(String name, int result) {
        if (result < 0) {
            throw new IllegalStateException(name + " failed with HRESULT " + result
                    + " (0x" + Integer.toHexString(result) + ')');
        }
        @Nullable Throwable failure = failures.poll();
        if (failure != null) {
            throw new IllegalStateException(name + " callback failed", failure);
        }
    }

    /// Verifies the provider is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Windows automation provider is closed");
        }
    }
}
