package org.glavo.himari.platform.windows;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsGrid;
import org.glavo.himari.layout.semantics.SemanticsGridItem;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
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
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

/// Implements `IRawElementProviderSimple` plus Invoke, Toggle, RangeValue, Value,
/// ExpandCollapse, SelectionItem, Grid, Table, Scroll, ScrollItem, VirtualizedItem, Dock,
/// Transform, Transform2, ItemContainer, Window, Text, TextChild, Styles, Spreadsheet,
/// CustomNavigation, and ObjectModel COM patterns.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsAutomationProvider implements AutoCloseable {
    /// `IUnknown`.
    private static final UUID IUNKNOWN = UUID.fromString("00000000-0000-0000-c000-000000000046");

    /// `IRawElementProviderSimple`.
    private static final UUID IRAW_ELEMENT_PROVIDER_SIMPLE =
            UUID.fromString("d6dd68d1-86fd-4332-8666-9abedea2d24c");

    /// `IRawElementProviderSimple2`.
    private static final UUID IRAW_ELEMENT_PROVIDER_SIMPLE2 =
            UUID.fromString("a0a839a9-8da1-4a82-806a-8e0d44e79f56");

    /// `IRawElementProviderFragment`.
    private static final UUID IRAW_ELEMENT_PROVIDER_FRAGMENT =
            UUID.fromString("f7063da8-8359-439c-9297-bbc5299a7d87");

    /// `IRawElementProviderFragmentRoot`.
    private static final UUID IRAW_ELEMENT_PROVIDER_FRAGMENT_ROOT =
            UUID.fromString("620ce2a5-ab8f-40a9-86cb-de3c75599b58");

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

    /// `IVirtualizedItemProvider`.
    private static final UUID IVIRTUALIZED_ITEM_PROVIDER =
            UUID.fromString("6ba3d5f4-3935-4117-8b15-0c437c2e6c2e");

    /// `IDockProvider`.
    private static final UUID IDOCK_PROVIDER = UUID.fromString("159bc72c-4ad3-485e-9637-d7052edf0146");

    /// `ITransformProvider`.
    private static final UUID ITRANSFORM_PROVIDER = UUID.fromString("6829ddc4-4f91-4ffa-b86f-bd3e5267c311");

    /// `IItemContainerProvider`.
    private static final UUID IITEM_CONTAINER_PROVIDER =
            UUID.fromString("e3fad038-4e21-4237-a4d6-55676557c162");

    /// `ISynchronizedInputProvider`.
    private static final UUID ISYNCHRONIZED_INPUT_PROVIDER =
            UUID.fromString("29db1a06-02ce-4cf7-9b42-565d4fab20ee");

    /// `IMultipleViewProvider`.
    private static final UUID IMULTIPLE_VIEW_PROVIDER =
            UUID.fromString("6278cab1-b556-4a1a-b4e0-418d48aeb45b");

    /// `IDropTargetProvider`.
    private static final UUID IDROP_TARGET_PROVIDER =
            UUID.fromString("bae82bfd-358a-481c-85a0-d8b4d90a5d61");

    /// `IDragProvider`.
    private static final UUID IDRAG_PROVIDER = UUID.fromString("6aa7bbbb-7ff9-497d-904f-d20b897929d8");

    /// `IAnnotationProvider`.
    private static final UUID IANNOTATION_PROVIDER =
            UUID.fromString("f95c7e80-bd63-4601-9782-445ebff011fc");

    /// `ITextChildProvider`.
    private static final UUID ITEXT_CHILD_PROVIDER =
            UUID.fromString("4c2de2b9-c88f-4f88-a111-f24c2400987a");

    /// `IStylesProvider`.
    private static final UUID ISTYLES_PROVIDER =
            UUID.fromString("19b6b649-f5d7-4a6d-bdcb-129252be588a");

    /// `ISpreadsheetProvider`.
    private static final UUID ISPREADSHEET_PROVIDER =
            UUID.fromString("6f6b5d35-5525-4f80-b758-85473832ffc7");

    /// `ICustomNavigationProvider`.
    private static final UUID ICUSTOM_NAVIGATION_PROVIDER =
            UUID.fromString("01ea217a-1766-47ed-a6fc-0ed7656d026e");

    /// `IObjectModelProvider`.
    private static final UUID IOBJECT_MODEL_PROVIDER =
            UUID.fromString("3ad86ebd-f5ef-483d-bb18-b1042a475d64");

    /// `ITextEditProvider`.
    private static final UUID ITEXT_EDIT_PROVIDER =
            UUID.fromString("ea3605b4-3a05-400e-b5f9-4e91b40f6176");

    /// `ISelectionProvider`.
    private static final UUID ISELECTION_PROVIDER =
            UUID.fromString("fb8b03af-3bdf-48d4-bd36-1a65793be168");

    /// `ILegacyIAccessibleProvider`.
    private static final UUID ILEGACY_IACCESSIBLE_PROVIDER =
            UUID.fromString("e44c3566-915d-4070-99c6-047bff5a08f5");

    /// `ITextProvider2`.
    private static final UUID ITEXT_PROVIDER2 =
            UUID.fromString("0eb205d3-a011-4ff4-a575-437f56c4dd83");

    /// `ISpreadsheetItemProvider`.
    private static final UUID ISPREADSHEET_ITEM_PROVIDER =
            UUID.fromString("ea51a14a-bd5d-4c2f-8b8a-5e1d0d3e4e3e");

    /// `ISelectionProvider2`.
    private static final UUID ISELECTION_PROVIDER2 =
            UUID.fromString("14f68475-ee1c-44f6-a869-d239381f0fe7");

    /// `ITransformProvider2`.
    private static final UUID ITRANSFORM_PROVIDER2 =
            UUID.fromString("4758742f-7ac2-460c-bc48-09fc09308a13");

    /// `IValueProvider`.
    private static final UUID IVALUE_PROVIDER = UUID.fromString("c7935180-6fb3-4201-b174-7df73adbf64a");

    /// `IWindowProvider`.
    private static final UUID IWINDOW_PROVIDER = UUID.fromString("987df77b-db06-4d77-8f8a-86a9c3bb90b9");

    /// `ITextProvider`.
    private static final UUID ITEXT_PROVIDER = UUID.fromString("3589c92c-63f3-4367-99bb-ada653b77cf2");

    /// `ITextRangeProvider`.
    private static final UUID ITEXT_RANGE_PROVIDER = UUID.fromString("534729dc-411e-4aaa-9d3a-eb1d1d2c9d87");

    /// `ITextRangeProvider2`.
    private static final UUID ITEXT_RANGE_PROVIDER2 =
            UUID.fromString("9bbce42c-1921-4f18-89ca-dba1910a0386");

    /// `UIA_BoundingRectanglePropertyId`.
    static final int UIA_BOUNDING_RECTANGLE_PROPERTY_ID = 30001;

    /// `UIA_ProcessIdPropertyId`.
    static final int UIA_PROCESS_ID_PROPERTY_ID = 30002;

    /// `UIA_ControlTypePropertyId`.
    static final int UIA_CONTROL_TYPE_PROPERTY_ID = 30003;

    /// `UIA_HeaderItemControlTypeId`.
    static final int UIA_HEADER_ITEM_CONTROL_TYPE_ID = 50034;

    /// `UIA_AcceleratorKeyPropertyId`.
    static final int UIA_ACCELERATOR_KEY_PROPERTY_ID = 30006;

    /// `UIA_AccessKeyPropertyId`.
    static final int UIA_ACCESS_KEY_PROPERTY_ID = 30007;

    /// `UIA_HasKeyboardFocusPropertyId`.
    static final int UIA_HAS_KEYBOARD_FOCUS_PROPERTY_ID = 30008;

    /// `UIA_ValueValuePropertyId`.
    static final int UIA_VALUE_VALUE_PROPERTY_ID = 30045;

    /// `UIA_IsKeyboardFocusablePropertyId`.
    static final int UIA_IS_KEYBOARD_FOCUSABLE_PROPERTY_ID = 30009;

    /// `UIA_NamePropertyId`.
    static final int UIA_NAME_PROPERTY_ID = 30005;

    /// `UIA_ClassNamePropertyId`.
    static final int UIA_CLASS_NAME_PROPERTY_ID = 30012;

    /// `UIA_NativeWindowHandlePropertyId`.
    static final int UIA_NATIVE_WINDOW_HANDLE_PROPERTY_ID = 30020;

    /// `UIA_IsPasswordPropertyId`.
    static final int UIA_IS_PASSWORD_PROPERTY_ID = 30019;

    /// `UIA_IsControlElementPropertyId`.
    static final int UIA_IS_CONTROL_ELEMENT_PROPERTY_ID = 30016;

    /// `UIA_IsContentElementPropertyId`.
    static final int UIA_IS_CONTENT_ELEMENT_PROPERTY_ID = 30017;

    /// `UIA_IsOffscreenPropertyId`.
    static final int UIA_IS_OFFSCREEN_PROPERTY_ID = 30022;

    /// `UIA_CulturePropertyId`.
    static final int UIA_CULTURE_PROPERTY_ID = 30015;

    /// `UIA_IsRequiredForFormPropertyId`.
    static final int UIA_IS_REQUIRED_FOR_FORM_PROPERTY_ID = 30025;

    /// `UIA_ItemStatusPropertyId`.
    static final int UIA_ITEM_STATUS_PROPERTY_ID = 30026;

    /// `UIA_ItemTypePropertyId`.
    static final int UIA_ITEM_TYPE_PROPERTY_ID = 30021;

    /// `UIA_LandmarkTypePropertyId`.
    static final int UIA_LANDMARK_TYPE_PROPERTY_ID = 30157;

    /// `UIA_LocalizedLandmarkTypePropertyId`.
    static final int UIA_LOCALIZED_LANDMARK_TYPE_PROPERTY_ID = 30158;

    /// `UIA_AriaRolePropertyId`.
    static final int UIA_ARIA_ROLE_PROPERTY_ID = 30101;

    /// `UIA_AriaPropertiesPropertyId`.
    static final int UIA_ARIA_PROPERTIES_PROPERTY_ID = 30102;

    /// `UIA_ControllerForPropertyId`.
    static final int UIA_CONTROLLER_FOR_PROPERTY_ID = 30104;

    /// `UIA_DescribedByPropertyId`.
    static final int UIA_DESCRIBED_BY_PROPERTY_ID = 30105;

    /// `UIA_FlowsToPropertyId`.
    static final int UIA_FLOWS_TO_PROPERTY_ID = 30106;

    /// `UIA_LabeledByPropertyId`.
    static final int UIA_LABELED_BY_PROPERTY_ID = 30018;

    /// `UIA_FlowsFromPropertyId`.
    static final int UIA_FLOWS_FROM_PROPERTY_ID = 30148;

    /// `UIA_OptimizeForVisualContentPropertyId`.
    static final int UIA_OPTIMIZE_FOR_VISUAL_CONTENT_PROPERTY_ID = 30111;

    /// `UIA_FillColorPropertyId`.
    static final int UIA_FILL_COLOR_PROPERTY_ID = 30160;

    /// `UIA_OutlineColorPropertyId`.
    static final int UIA_OUTLINE_COLOR_PROPERTY_ID = 30161;

    /// `UIA_FillTypePropertyId`.
    static final int UIA_FILL_TYPE_PROPERTY_ID = 30162;

    /// `UIA_VisualEffectsPropertyId`.
    static final int UIA_VISUAL_EFFECTS_PROPERTY_ID = 30163;

    /// `UIA_OutlineThicknessPropertyId`.
    static final int UIA_OUTLINE_THICKNESS_PROPERTY_ID = 30164;

    /// `UIA_CenterPointPropertyId`.
    static final int UIA_CENTER_POINT_PROPERTY_ID = 30165;

    /// `UIA_RotationPropertyId`.
    static final int UIA_ROTATION_PROPERTY_ID = 30166;

    /// `UIA_SizePropertyId`.
    static final int UIA_SIZE_PROPERTY_ID = 30167;

    /// `UIA_RuntimeIdPropertyId`.
    static final int UIA_RUNTIME_ID_PROPERTY_ID = 30000;

    /// `UIA_IsPeripheralPropertyId`.
    static final int UIA_IS_PERIPHERAL_PROPERTY_ID = 30150;

    /// `UIA_AnnotationTypesPropertyId`.
    static final int UIA_ANNOTATION_TYPES_PROPERTY_ID = 30155;

    /// `UIA_AnnotationObjectsPropertyId`.
    static final int UIA_ANNOTATION_OBJECTS_PROPERTY_ID = 30156;

    /// `UIA_ValueIsReadOnlyPropertyId`.
    static final int UIA_VALUE_IS_READ_ONLY_PROPERTY_ID = 30046;

    /// `UIA_IsInvokePatternAvailablePropertyId`.
    static final int UIA_IS_INVOKE_PATTERN_AVAILABLE_PROPERTY_ID = 30031;

    /// `UIA_IsValuePatternAvailablePropertyId`.
    static final int UIA_IS_VALUE_PATTERN_AVAILABLE_PROPERTY_ID = 30043;

    /// `UIA_IsRangeValuePatternAvailablePropertyId`.
    static final int UIA_IS_RANGE_VALUE_PATTERN_AVAILABLE_PROPERTY_ID = 30033;

    /// `UIA_IsTogglePatternAvailablePropertyId`.
    static final int UIA_IS_TOGGLE_PATTERN_AVAILABLE_PROPERTY_ID = 30041;

    /// `UIA_IsScrollPatternAvailablePropertyId`.
    static final int UIA_IS_SCROLL_PATTERN_AVAILABLE_PROPERTY_ID = 30034;

    /// `UIA_IsWindowPatternAvailablePropertyId`.
    static final int UIA_IS_WINDOW_PATTERN_AVAILABLE_PROPERTY_ID = 30044;

    /// `UIA_IsExpandCollapsePatternAvailablePropertyId`.
    static final int UIA_IS_EXPAND_COLLAPSE_PATTERN_AVAILABLE_PROPERTY_ID = 30028;

    /// `UIA_IsSelectionItemPatternAvailablePropertyId`.
    static final int UIA_IS_SELECTION_ITEM_PATTERN_AVAILABLE_PROPERTY_ID = 30036;

    /// `UIA_IsGridPatternAvailablePropertyId`.
    static final int UIA_IS_GRID_PATTERN_AVAILABLE_PROPERTY_ID = 30030;

    /// `UIA_IsGridItemPatternAvailablePropertyId`.
    static final int UIA_IS_GRID_ITEM_PATTERN_AVAILABLE_PROPERTY_ID = 30029;

    /// `UIA_IsTablePatternAvailablePropertyId`.
    static final int UIA_IS_TABLE_PATTERN_AVAILABLE_PROPERTY_ID = 30038;

    /// `UIA_IsScrollItemPatternAvailablePropertyId`.
    static final int UIA_IS_SCROLL_ITEM_PATTERN_AVAILABLE_PROPERTY_ID = 30035;

    /// `UIA_IsMultipleViewPatternAvailablePropertyId`.
    static final int UIA_IS_MULTIPLE_VIEW_PATTERN_AVAILABLE_PROPERTY_ID = 30032;

    /// `UIA_IsSelectionPatternAvailablePropertyId`.
    static final int UIA_IS_SELECTION_PATTERN_AVAILABLE_PROPERTY_ID = 30037;

    /// `UIA_IsTableItemPatternAvailablePropertyId`.
    static final int UIA_IS_TABLE_ITEM_PATTERN_AVAILABLE_PROPERTY_ID = 30039;

    /// `UIA_IsTextPatternAvailablePropertyId`.
    static final int UIA_IS_TEXT_PATTERN_AVAILABLE_PROPERTY_ID = 30040;

    /// `UIA_IsTransformPatternAvailablePropertyId`.
    static final int UIA_IS_TRANSFORM_PATTERN_AVAILABLE_PROPERTY_ID = 30042;

    /// `UIA_IsLegacyIAccessiblePatternAvailablePropertyId`.
    static final int UIA_IS_LEGACY_IACCESSIBLE_PATTERN_AVAILABLE_PROPERTY_ID = 30090;

    /// `UIA_IsItemContainerPatternAvailablePropertyId`.
    static final int UIA_IS_ITEM_CONTAINER_PATTERN_AVAILABLE_PROPERTY_ID = 30108;

    /// `UIA_IsVirtualizedItemPatternAvailablePropertyId`.
    static final int UIA_IS_VIRTUALIZED_ITEM_PATTERN_AVAILABLE_PROPERTY_ID = 30109;

    /// `UIA_IsTextPattern2AvailablePropertyId`.
    static final int UIA_IS_TEXT_PATTERN2_AVAILABLE_PROPERTY_ID = 30119;

    /// `UIA_IsSynchronizedInputPatternAvailablePropertyId`.
    static final int UIA_IS_SYNCHRONIZED_INPUT_PATTERN_AVAILABLE_PROPERTY_ID = 30110;

    /// `UIA_IsObjectModelPatternAvailablePropertyId`.
    static final int UIA_IS_OBJECT_MODEL_PATTERN_AVAILABLE_PROPERTY_ID = 30112;

    /// `UIA_IsAnnotationPatternAvailablePropertyId`.
    static final int UIA_IS_ANNOTATION_PATTERN_AVAILABLE_PROPERTY_ID = 30118;

    /// `UIA_IsStylesPatternAvailablePropertyId`.
    static final int UIA_IS_STYLES_PATTERN_AVAILABLE_PROPERTY_ID = 30127;

    /// `UIA_IsSpreadsheetPatternAvailablePropertyId`.
    static final int UIA_IS_SPREADSHEET_PATTERN_AVAILABLE_PROPERTY_ID = 30128;

    /// `UIA_IsSpreadsheetItemPatternAvailablePropertyId`.
    static final int UIA_IS_SPREADSHEET_ITEM_PATTERN_AVAILABLE_PROPERTY_ID = 30132;

    /// `UIA_IsTransformPattern2AvailablePropertyId`.
    static final int UIA_IS_TRANSFORM_PATTERN2_AVAILABLE_PROPERTY_ID = 30134;

    /// `UIA_IsTextChildPatternAvailablePropertyId`.
    static final int UIA_IS_TEXT_CHILD_PATTERN_AVAILABLE_PROPERTY_ID = 30136;

    /// `UIA_IsDragPatternAvailablePropertyId`.
    static final int UIA_IS_DRAG_PATTERN_AVAILABLE_PROPERTY_ID = 30137;

    /// `UIA_IsDropTargetPatternAvailablePropertyId`.
    static final int UIA_IS_DROP_TARGET_PATTERN_AVAILABLE_PROPERTY_ID = 30141;

    /// `UIA_IsTextEditPatternAvailablePropertyId`.
    static final int UIA_IS_TEXT_EDIT_PATTERN_AVAILABLE_PROPERTY_ID = 30149;

    /// `UIA_IsCustomNavigationPatternAvailablePropertyId`.
    static final int UIA_IS_CUSTOM_NAVIGATION_PATTERN_AVAILABLE_PROPERTY_ID = 30151;

    /// `UIA_IsDockPatternAvailablePropertyId`.
    static final int UIA_IS_DOCK_PATTERN_AVAILABLE_PROPERTY_ID = 30027;

    /// `UIA_IsSelectionPattern2AvailablePropertyId`.
    static final int UIA_IS_SELECTION_PATTERN2_AVAILABLE_PROPERTY_ID = 30168;

    /// `UIA_DockDockPositionPropertyId`.
    static final int UIA_DOCK_DOCK_POSITION_PROPERTY_ID = 30069;

    /// `UIA_TransformCanMovePropertyId`.
    static final int UIA_TRANSFORM_CAN_MOVE_PROPERTY_ID = 30087;

    /// `UIA_TransformCanResizePropertyId`.
    static final int UIA_TRANSFORM_CAN_RESIZE_PROPERTY_ID = 30088;

    /// `UIA_TransformCanRotatePropertyId`.
    static final int UIA_TRANSFORM_CAN_ROTATE_PROPERTY_ID = 30089;

    /// `UIA_ExpandCollapseExpandCollapseStatePropertyId`.
    static final int UIA_EXPAND_COLLAPSE_EXPAND_COLLAPSE_STATE_PROPERTY_ID = 30070;

    /// `UIA_ToggleToggleStatePropertyId`.
    static final int UIA_TOGGLE_TOGGLE_STATE_PROPERTY_ID = 30086;

    /// `UIA_MultipleViewCurrentViewPropertyId`.
    static final int UIA_MULTIPLE_VIEW_CURRENT_VIEW_PROPERTY_ID = 30071;

    /// `UIA_SelectionItemIsSelectedPropertyId`.
    static final int UIA_SELECTION_ITEM_IS_SELECTED_PROPERTY_ID = 30079;

    /// `UIA_GridRowCountPropertyId`.
    static final int UIA_GRID_ROW_COUNT_PROPERTY_ID = 30062;

    /// `UIA_GridColumnCountPropertyId`.
    static final int UIA_GRID_COLUMN_COUNT_PROPERTY_ID = 30063;

    /// `UIA_GridItemRowPropertyId`.
    static final int UIA_GRID_ITEM_ROW_PROPERTY_ID = 30064;

    /// `UIA_GridItemColumnPropertyId`.
    static final int UIA_GRID_ITEM_COLUMN_PROPERTY_ID = 30065;

    /// `UIA_GridItemRowSpanPropertyId`.
    static final int UIA_GRID_ITEM_ROW_SPAN_PROPERTY_ID = 30066;

    /// `UIA_GridItemColumnSpanPropertyId`.
    static final int UIA_GRID_ITEM_COLUMN_SPAN_PROPERTY_ID = 30067;

    /// `UIA_GridItemContainingGridPropertyId`.
    static final int UIA_GRID_ITEM_CONTAINING_GRID_PROPERTY_ID = 30068;

    /// `UIA_SelectionItemSelectionContainerPropertyId`.
    static final int UIA_SELECTION_ITEM_SELECTION_CONTAINER_PROPERTY_ID = 30080;

    /// `UIA_LegacyIAccessibleSelectionPropertyId`.
    static final int UIA_LEGACY_IACCESSIBLE_SELECTION_PROPERTY_ID = 30099;

    /// `UIA_WindowCanMaximizePropertyId`.
    static final int UIA_WINDOW_CAN_MAXIMIZE_PROPERTY_ID = 30073;

    /// `UIA_WindowCanMinimizePropertyId`.
    static final int UIA_WINDOW_CAN_MINIMIZE_PROPERTY_ID = 30074;

    /// `UIA_WindowWindowVisualStatePropertyId`.
    static final int UIA_WINDOW_WINDOW_VISUAL_STATE_PROPERTY_ID = 30075;

    /// `UIA_WindowWindowInteractionStatePropertyId`.
    static final int UIA_WINDOW_WINDOW_INTERACTION_STATE_PROPERTY_ID = 30076;

    /// `UIA_WindowIsModalPropertyId`.
    static final int UIA_WINDOW_IS_MODAL_PROPERTY_ID = 30077;

    /// `UIA_WindowIsTopmostPropertyId`.
    static final int UIA_WINDOW_IS_TOPMOST_PROPERTY_ID = 30078;

    /// `UIA_TableRowHeadersPropertyId`.
    static final int UIA_TABLE_ROW_HEADERS_PROPERTY_ID = 30081;

    /// `UIA_TableColumnHeadersPropertyId`.
    static final int UIA_TABLE_COLUMN_HEADERS_PROPERTY_ID = 30082;

    /// `UIA_TableRowOrColumnMajorPropertyId`.
    static final int UIA_TABLE_ROW_OR_COLUMN_MAJOR_PROPERTY_ID = 30083;

    /// `UIA_TableItemRowHeaderItemsPropertyId`.
    static final int UIA_TABLE_ITEM_ROW_HEADER_ITEMS_PROPERTY_ID = 30084;

    /// `UIA_TableItemColumnHeaderItemsPropertyId`.
    static final int UIA_TABLE_ITEM_COLUMN_HEADER_ITEMS_PROPERTY_ID = 30085;

    /// `UIA_SelectionSelectionPropertyId`.
    static final int UIA_SELECTION_SELECTION_PROPERTY_ID = 30059;

    /// `UIA_SelectionCanSelectMultiplePropertyId`.
    static final int UIA_SELECTION_CAN_SELECT_MULTIPLE_PROPERTY_ID = 30060;

    /// `UIA_SelectionIsSelectionRequiredPropertyId`.
    static final int UIA_SELECTION_IS_SELECTION_REQUIRED_PROPERTY_ID = 30061;

    /// `UIA_ScrollHorizontalScrollPercentPropertyId`.
    static final int UIA_SCROLL_HORIZONTAL_SCROLL_PERCENT_PROPERTY_ID = 30053;

    /// `UIA_ScrollHorizontalViewSizePropertyId`.
    static final int UIA_SCROLL_HORIZONTAL_VIEW_SIZE_PROPERTY_ID = 30054;

    /// `UIA_ScrollVerticalScrollPercentPropertyId`.
    static final int UIA_SCROLL_VERTICAL_SCROLL_PERCENT_PROPERTY_ID = 30055;

    /// `UIA_ScrollHorizontallyScrollablePropertyId`.
    static final int UIA_SCROLL_HORIZONTALLY_SCROLLABLE_PROPERTY_ID = 30057;

    /// `UIA_ScrollVerticalViewSizePropertyId`.
    static final int UIA_SCROLL_VERTICAL_VIEW_SIZE_PROPERTY_ID = 30056;

    /// `UIA_ScrollVerticallyScrollablePropertyId`.
    static final int UIA_SCROLL_VERTICALLY_SCROLLABLE_PROPERTY_ID = 30058;

    /// `UIA_RangeValueMinimumPropertyId`.
    static final int UIA_RANGE_VALUE_MINIMUM_PROPERTY_ID = 30049;

    /// `UIA_RangeValueMaximumPropertyId`.
    static final int UIA_RANGE_VALUE_MAXIMUM_PROPERTY_ID = 30050;

    /// `UIA_RangeValueIsReadOnlyPropertyId`.
    static final int UIA_RANGE_VALUE_IS_READ_ONLY_PROPERTY_ID = 30048;

    /// First-stable range minimum published by `IRangeValueProvider::get_Minimum`.
    static final double RANGE_MINIMUM = 0.0;

    /// First-stable range maximum published by `IRangeValueProvider::get_Maximum`.
    static final double RANGE_MAXIMUM = 100.0;

    /// First-stable large-change step, one tenth of `[#RANGE_MINIMUM, #RANGE_MAXIMUM]`.
    static final double RANGE_LARGE_CHANGE = 10.0;

    /// First-stable small-change step, one hundredth of `[#RANGE_MINIMUM, #RANGE_MAXIMUM]`.
    static final double RANGE_SMALL_CHANGE = 1.0;

    /// `UIA_RangeValueLargeChangePropertyId`.
    static final int UIA_RANGE_VALUE_LARGE_CHANGE_PROPERTY_ID = 30051;

    /// `UIA_RangeValueSmallChangePropertyId`.
    static final int UIA_RANGE_VALUE_SMALL_CHANGE_PROPERTY_ID = 30052;

    /// `UIA_AnnotationAnnotationTypeIdPropertyId`.
    static final int UIA_ANNOTATION_ANNOTATION_TYPE_ID_PROPERTY_ID = 30113;

    /// `UIA_AnnotationAnnotationTypeNamePropertyId`.
    static final int UIA_ANNOTATION_ANNOTATION_TYPE_NAME_PROPERTY_ID = 30114;

    /// `UIA_AnnotationAuthorPropertyId`.
    static final int UIA_ANNOTATION_AUTHOR_PROPERTY_ID = 30115;

    /// `UIA_AnnotationDateTimePropertyId`.
    static final int UIA_ANNOTATION_DATE_TIME_PROPERTY_ID = 30116;

    /// `UIA_StylesStyleIdPropertyId`.
    static final int UIA_STYLES_STYLE_ID_PROPERTY_ID = 30120;

    /// `UIA_StylesStyleNamePropertyId`.
    static final int UIA_STYLES_STYLE_NAME_PROPERTY_ID = 30121;

    /// `UIA_StylesFillColorPropertyId`.
    static final int UIA_STYLES_FILL_COLOR_PROPERTY_ID = 30122;

    /// `UIA_StylesFillPatternStylePropertyId`.
    static final int UIA_STYLES_FILL_PATTERN_STYLE_PROPERTY_ID = 30123;

    /// `UIA_StylesShapePropertyId`.
    static final int UIA_STYLES_SHAPE_PROPERTY_ID = 30124;

    /// `UIA_StylesFillPatternColorPropertyId`.
    static final int UIA_STYLES_FILL_PATTERN_COLOR_PROPERTY_ID = 30125;

    /// `UIA_StylesExtendedPropertiesPropertyId`.
    static final int UIA_STYLES_EXTENDED_PROPERTIES_PROPERTY_ID = 30126;

    /// `UIA_SpreadsheetItemFormulaPropertyId`.
    static final int UIA_SPREADSHEET_ITEM_FORMULA_PROPERTY_ID = 30129;

    /// `UIA_SpreadsheetItemAnnotationObjectsPropertyId`.
    static final int UIA_SPREADSHEET_ITEM_ANNOTATION_OBJECTS_PROPERTY_ID = 30130;

    /// `UIA_SpreadsheetItemAnnotationTypesPropertyId`.
    static final int UIA_SPREADSHEET_ITEM_ANNOTATION_TYPES_PROPERTY_ID = 30131;

    /// `UIA_DragIsGrabbedPropertyId`.
    static final int UIA_DRAG_IS_GRABBED_PROPERTY_ID = 30138;

    /// `UIA_DragDropEffectPropertyId`.
    static final int UIA_DRAG_DROP_EFFECT_PROPERTY_ID = 30139;

    /// `UIA_DragDropEffectsPropertyId`.
    static final int UIA_DRAG_DROP_EFFECTS_PROPERTY_ID = 30140;

    /// `UIA_DropTargetDropTargetEffectPropertyId`.
    static final int UIA_DROP_TARGET_DROP_TARGET_EFFECT_PROPERTY_ID = 30142;

    /// `UIA_DropTargetDropTargetEffectsPropertyId`.
    static final int UIA_DROP_TARGET_DROP_TARGET_EFFECTS_PROPERTY_ID = 30143;

    /// `UIA_DragGrabbedItemsPropertyId`.
    static final int UIA_DRAG_GRABBED_ITEMS_PROPERTY_ID = 30144;

    /// `UIA_Transform2ZoomLevelPropertyId`.
    static final int UIA_TRANSFORM2_ZOOM_LEVEL_PROPERTY_ID = 30145;

    /// `UIA_Transform2ZoomMinimumPropertyId`.
    static final int UIA_TRANSFORM2_ZOOM_MINIMUM_PROPERTY_ID = 30146;

    /// `UIA_Transform2ZoomMaximumPropertyId`.
    static final int UIA_TRANSFORM2_ZOOM_MAXIMUM_PROPERTY_ID = 30147;

    /// `UIA_Transform2CanZoomPropertyId`.
    static final int UIA_TRANSFORM2_CAN_ZOOM_PROPERTY_ID = 30133;

    /// `UIA_Selection2ItemCountPropertyId`.
    static final int UIA_SELECTION2_ITEM_COUNT_PROPERTY_ID = 30172;

    /// `UIA_LegacyIAccessibleChildIdPropertyId`.
    static final int UIA_LEGACY_IACCESSIBLE_CHILD_ID_PROPERTY_ID = 30091;

    /// `UIA_LegacyIAccessibleNamePropertyId`.
    static final int UIA_LEGACY_IACCESSIBLE_NAME_PROPERTY_ID = 30092;

    /// `UIA_LegacyIAccessibleValuePropertyId`.
    static final int UIA_LEGACY_IACCESSIBLE_VALUE_PROPERTY_ID = 30093;

    /// `UIA_LegacyIAccessibleDescriptionPropertyId`.
    static final int UIA_LEGACY_IACCESSIBLE_DESCRIPTION_PROPERTY_ID = 30094;

    /// `UIA_LegacyIAccessibleRolePropertyId`.
    static final int UIA_LEGACY_IACCESSIBLE_ROLE_PROPERTY_ID = 30095;

    /// `UIA_LegacyIAccessibleStatePropertyId`.
    static final int UIA_LEGACY_IACCESSIBLE_STATE_PROPERTY_ID = 30096;

    /// `UIA_LegacyIAccessibleHelpPropertyId`.
    static final int UIA_LEGACY_IACCESSIBLE_HELP_PROPERTY_ID = 30097;

    /// `UIA_LegacyIAccessibleKeyboardShortcutPropertyId`.
    static final int UIA_LEGACY_IACCESSIBLE_KEYBOARD_SHORTCUT_PROPERTY_ID = 30098;

    /// `UIA_LegacyIAccessibleDefaultActionPropertyId`.
    static final int UIA_LEGACY_IACCESSIBLE_DEFAULT_ACTION_PROPERTY_ID = 30100;

    /// `UIA_MultipleViewSupportedViewsPropertyId`.
    static final int UIA_MULTIPLE_VIEW_SUPPORTED_VIEWS_PROPERTY_ID = 30072;

    /// `UIA_AnimationStyleAttributeId`.
    static final int UIA_ANIMATION_STYLE_ATTRIBUTE_ID = 40000;

    /// `UIA_FontNameAttributeId`.
    static final int UIA_FONT_NAME_ATTRIBUTE_ID = 40005;

    /// `UIA_FontSizeAttributeId`.
    static final int UIA_FONT_SIZE_ATTRIBUTE_ID = 40006;

    /// `UIA_Selection2FirstSelectedItemPropertyId`.
    static final int UIA_SELECTION2_FIRST_SELECTED_ITEM_PROPERTY_ID = 30169;

    /// `UIA_Selection2LastSelectedItemPropertyId`.
    static final int UIA_SELECTION2_LAST_SELECTED_ITEM_PROPERTY_ID = 30170;

    /// `UIA_Selection2CurrentSelectedItemPropertyId`.
    static final int UIA_SELECTION2_CURRENT_SELECTED_ITEM_PROPERTY_ID = 30171;

    /// `UIA_AnnotationTargetPropertyId`.
    static final int UIA_ANNOTATION_TARGET_PROPERTY_ID = 30117;

    /// `UIA_BackgroundColorAttributeId`.
    static final int UIA_BACKGROUND_COLOR_ATTRIBUTE_ID = 40001;

    /// `UIA_FontWeightAttributeId`.
    static final int UIA_FONT_WEIGHT_ATTRIBUTE_ID = 40007;

    /// `UIA_ForegroundColorAttributeId`.
    static final int UIA_FOREGROUND_COLOR_ATTRIBUTE_ID = 40008;

    /// `UIA_IsHiddenAttributeId`.
    static final int UIA_IS_HIDDEN_ATTRIBUTE_ID = 40013;

    /// `UIA_BulletStyleAttributeId`.
    static final int UIA_BULLET_STYLE_ATTRIBUTE_ID = 40002;

    /// `UIA_CapStyleAttributeId`.
    static final int UIA_CAP_STYLE_ATTRIBUTE_ID = 40003;

    /// `UIA_CultureAttributeId`.
    static final int UIA_CULTURE_ATTRIBUTE_ID = 40004;

    /// `UIA_HorizontalTextAlignmentAttributeId`.
    static final int UIA_HORIZONTAL_TEXT_ALIGNMENT_ATTRIBUTE_ID = 40009;

    /// `UIA_IsItalicAttributeId`.
    static final int UIA_IS_ITALIC_ATTRIBUTE_ID = 40014;

    /// `UIA_IsReadOnlyAttributeId`.
    static final int UIA_IS_READ_ONLY_ATTRIBUTE_ID = 40015;

    /// `UIA_IndentationFirstLineAttributeId`.
    static final int UIA_INDENTATION_FIRST_LINE_ATTRIBUTE_ID = 40010;

    /// `UIA_IndentationLeadingAttributeId`.
    static final int UIA_INDENTATION_LEADING_ATTRIBUTE_ID = 40011;

    /// `UIA_IndentationTrailingAttributeId`.
    static final int UIA_INDENTATION_TRAILING_ATTRIBUTE_ID = 40012;

    /// `UIA_IsSubscriptAttributeId`.
    static final int UIA_IS_SUBSCRIPT_ATTRIBUTE_ID = 40016;

    /// `UIA_IsSuperscriptAttributeId`.
    static final int UIA_IS_SUPERSCRIPT_ATTRIBUTE_ID = 40017;

    /// `UIA_MarginBottomAttributeId`.
    static final int UIA_MARGIN_BOTTOM_ATTRIBUTE_ID = 40018;

    /// `UIA_MarginLeadingAttributeId`.
    static final int UIA_MARGIN_LEADING_ATTRIBUTE_ID = 40019;

    /// `UIA_MarginTopAttributeId`.
    static final int UIA_MARGIN_TOP_ATTRIBUTE_ID = 40020;

    /// `UIA_MarginTrailingAttributeId`.
    static final int UIA_MARGIN_TRAILING_ATTRIBUTE_ID = 40021;

    /// First-stable first-line indent in points published with [`#UIA_INDENTATION_FIRST_LINE_ATTRIBUTE_ID`].
    static final double ATTRIBUTE_INDENT_FIRST_LINE = 0.0;

    /// First-stable leading indent in points published with [`#UIA_INDENTATION_LEADING_ATTRIBUTE_ID`].
    static final double ATTRIBUTE_INDENT_LEADING = 0.0;

    /// First-stable trailing indent in points published with [`#UIA_INDENTATION_TRAILING_ATTRIBUTE_ID`].
    static final double ATTRIBUTE_INDENT_TRAILING = 0.0;

    /// First-stable margin in points published with the margin TextAttributeIds.
    static final double ATTRIBUTE_MARGIN = 0.0;

    /// `UIA_OutlineStylesAttributeId`.
    static final int UIA_OUTLINE_STYLES_ATTRIBUTE_ID = 40022;

    /// `UIA_OverlineColorAttributeId`.
    static final int UIA_OVERLINE_COLOR_ATTRIBUTE_ID = 40023;

    /// `UIA_OverlineStyleAttributeId`.
    static final int UIA_OVERLINE_STYLE_ATTRIBUTE_ID = 40024;

    /// `UIA_StrikethroughColorAttributeId`.
    static final int UIA_STRIKETHROUGH_COLOR_ATTRIBUTE_ID = 40025;

    /// `UIA_StrikethroughStyleAttributeId`.
    static final int UIA_STRIKETHROUGH_STYLE_ATTRIBUTE_ID = 40026;

    /// `UIA_TabsAttributeId`.
    static final int UIA_TABS_ATTRIBUTE_ID = 40027;

    /// `UIA_TextFlowDirectionsAttributeId`.
    static final int UIA_TEXT_FLOW_DIRECTIONS_ATTRIBUTE_ID = 40028;

    /// `UIA_UnderlineColorAttributeId`.
    static final int UIA_UNDERLINE_COLOR_ATTRIBUTE_ID = 40029;

    /// `UIA_UnderlineStyleAttributeId`.
    static final int UIA_UNDERLINE_STYLE_ATTRIBUTE_ID = 40030;

    /// `UIA_AnnotationTypesAttributeId`.
    static final int UIA_ANNOTATION_TYPES_ATTRIBUTE_ID = 40031;

    /// `UIA_AnnotationObjectsAttributeId`.
    static final int UIA_ANNOTATION_OBJECTS_ATTRIBUTE_ID = 40032;

    /// `UIA_StyleNameAttributeId`.
    static final int UIA_STYLE_NAME_ATTRIBUTE_ID = 40033;

    /// `UIA_StyleIdAttributeId`.
    static final int UIA_STYLE_ID_ATTRIBUTE_ID = 40034;

    /// `UIA_LinkAttributeId`.
    static final int UIA_LINK_ATTRIBUTE_ID = 40035;

    /// `UIA_IsActiveAttributeId`.
    static final int UIA_IS_ACTIVE_ATTRIBUTE_ID = 40036;

    /// `UIA_SelectionActiveEndAttributeId`.
    static final int UIA_SELECTION_ACTIVE_END_ATTRIBUTE_ID = 40037;

    /// `UIA_CaretPositionAttributeId`.
    static final int UIA_CARET_POSITION_ATTRIBUTE_ID = 40038;

    /// `UIA_CaretBidiModeAttributeId`.
    static final int UIA_CARET_BIDI_MODE_ATTRIBUTE_ID = 40039;

    /// `UIA_LineSpacingAttributeId`.
    static final int UIA_LINE_SPACING_ATTRIBUTE_ID = 40040;

    /// `UIA_BeforeParagraphSpacingAttributeId`.
    static final int UIA_BEFORE_PARAGRAPH_SPACING_ATTRIBUTE_ID = 40041;

    /// `UIA_AfterParagraphSpacingAttributeId`.
    static final int UIA_AFTER_PARAGRAPH_SPACING_ATTRIBUTE_ID = 40042;

    /// `UIA_SayAsInterpretAsAttributeId`.
    static final int UIA_SAY_AS_INTERPRET_AS_ATTRIBUTE_ID = 40043;

    /// First-stable line spacing multiplier published with [`#UIA_LINE_SPACING_ATTRIBUTE_ID`].
    static final double ATTRIBUTE_LINE_SPACING = 1.0;

    /// First-stable paragraph spacing in points published with the paragraph-spacing TextAttributeIds.
    static final double ATTRIBUTE_PARAGRAPH_SPACING = 0.0;

    /// First-stable styles font name published with [`#UIA_FONT_NAME_ATTRIBUTE_ID`].
    static final String ATTRIBUTE_FONT_NAME = "Segoe UI";

    /// First-stable styles font size in points published with [`#UIA_FONT_SIZE_ATTRIBUTE_ID`].
    static final double ATTRIBUTE_FONT_SIZE = 12.0;

    /// First-stable `FW_NORMAL` published with [`#UIA_FONT_WEIGHT_ATTRIBUTE_ID`].
    static final int ATTRIBUTE_FONT_WEIGHT = 400;

    /// First-stable opaque black published with [`#UIA_FOREGROUND_COLOR_ATTRIBUTE_ID`].
    static final int ATTRIBUTE_FOREGROUND_COLOR = 0xFF000000;

    /// First-stable opaque white published with [`#UIA_BACKGROUND_COLOR_ATTRIBUTE_ID`].
    static final int ATTRIBUTE_BACKGROUND_COLOR = 0xFFFFFFFF;

    /// First-stable styles shape name published with [`#STYLE_ID_NORMAL`].
    static final String STYLE_SHAPE_RECTANGLE = "Rectangle";

    /// `FillType_Color`.
    static final int FILL_TYPE_COLOR = 1;

    /// `VisualEffects_Shadow`.
    static final int VISUAL_EFFECTS_SHADOW = 1;

    /// `LandmarkType_None`.
    static final int LANDMARK_TYPE_NONE = 0;

    /// `LandmarkType_Main`.
    static final int LANDMARK_TYPE_MAIN = 80002;

    /// `UIA_PositionInSetPropertyId`.
    static final int UIA_POSITION_IN_SET_PROPERTY_ID = 30152;

    /// `UIA_SizeOfSetPropertyId`.
    static final int UIA_SIZE_OF_SET_PROPERTY_ID = 30153;

    /// `UIA_LevelPropertyId`.
    static final int UIA_LEVEL_PROPERTY_ID = 30154;

    /// `UIA_FullDescriptionPropertyId`.
    static final int UIA_FULL_DESCRIPTION_PROPERTY_ID = 30159;

    /// `UIA_IsDataValidForFormPropertyId`.
    static final int UIA_IS_DATA_VALID_FOR_FORM_PROPERTY_ID = 30103;

    /// `UIA_ProviderDescriptionPropertyId`.
    static final int UIA_PROVIDER_DESCRIPTION_PROPERTY_ID = 30107;

    /// `UIA_HeadingLevelPropertyId`.
    static final int UIA_HEADING_LEVEL_PROPERTY_ID = 30173;

    /// `HeadingLevel_None`.
    static final int HEADING_LEVEL_NONE = 80050;

    /// `HeadingLevel_1`.
    static final int HEADING_LEVEL_1 = 80051;

    /// `UIA_IsDialogPropertyId`.
    static final int UIA_IS_DIALOG_PROPERTY_ID = 30174;

    /// `UIA_OrientationPropertyId`.
    static final int UIA_ORIENTATION_PROPERTY_ID = 30023;

    /// First-stable `en-US` LCID used when a non-empty locale has no dedicated mapping.
    static final int LCID_EN_US = 0x0409;

    /// `OrientationType_None`.
    static final int ORIENTATION_NONE = 0;

    /// `OrientationType_Horizontal`.
    static final int ORIENTATION_HORIZONTAL = 1;

    /// `OrientationType_Vertical`.
    static final int ORIENTATION_VERTICAL = 2;

    /// `UIA_FrameworkIdPropertyId`.
    static final int UIA_FRAMEWORK_ID_PROPERTY_ID = 30024;

    /// `UIA_AutomationIdPropertyId`.
    static final int UIA_AUTOMATION_ID_PROPERTY_ID = 30011;

    /// `UIA_IsEnabledPropertyId`.
    static final int UIA_IS_ENABLED_PROPERTY_ID = 30010;

    /// `UIA_ClickablePointPropertyId`.
    static final int UIA_CLICKABLE_POINT_PROPERTY_ID = 30014;

    /// `UIA_HelpTextPropertyId`.
    static final int UIA_HELP_TEXT_PROPERTY_ID = 30013;

    /// `UIA_IsReadOnlyPropertyId`.
    static final int UIA_IS_READ_ONLY_PROPERTY_ID = 30047;

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

    /// `UIA_LiveRegionChangedEventId`.
    static final int UIA_LIVE_REGION_CHANGED_EVENT_ID = 20024;

    /// `UIA_Text_TextSelectionChangedEventId`.
    static final int UIA_TEXT_SELECTION_CHANGED_EVENT_ID = 20014;

    /// `UIA_Text_TextChangedEventId`.
    static final int UIA_TEXT_CHANGED_EVENT_ID = 20015;

    /// `UIA_LocalizedControlTypePropertyId`.
    static final int UIA_LOCALIZED_CONTROL_TYPE_PROPERTY_ID = 30004;

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

    /// `UIA_VirtualizedItemPatternId`.
    static final int UIA_VIRTUALIZED_ITEM_PATTERN_ID = 10020;

    /// `UIA_DockPatternId`.
    static final int UIA_DOCK_PATTERN_ID = 10011;

    /// `UIA_TransformPatternId`.
    static final int UIA_TRANSFORM_PATTERN_ID = 10016;

    /// `UIA_ItemContainerPatternId`.
    static final int UIA_ITEM_CONTAINER_PATTERN_ID = 10019;

    /// `UIA_SynchronizedInputPatternId`.
    static final int UIA_SYNCHRONIZED_INPUT_PATTERN_ID = 10021;

    /// `UIA_MultipleViewPatternId`.
    static final int UIA_MULTIPLE_VIEW_PATTERN_ID = 10008;

    /// `UIA_AnnotationPatternId`.
    static final int UIA_ANNOTATION_PATTERN_ID = 10023;

    /// `UIA_DragPatternId`.
    static final int UIA_DRAG_PATTERN_ID = 10030;

    /// `UIA_DropTargetPatternId`.
    static final int UIA_DROP_TARGET_PATTERN_ID = 10031;

    /// `UIA_TextChildPatternId`.
    static final int UIA_TEXT_CHILD_PATTERN_ID = 10029;

    /// `UIA_StylesPatternId`.
    static final int UIA_STYLES_PATTERN_ID = 10025;

    /// `UIA_SpreadsheetPatternId`.
    static final int UIA_SPREADSHEET_PATTERN_ID = 10026;

    /// `UIA_CustomNavigationPatternId`.
    static final int UIA_CUSTOM_NAVIGATION_PATTERN_ID = 10033;

    /// `UIA_ObjectModelPatternId`.
    static final int UIA_OBJECT_MODEL_PATTERN_ID = 10022;

    /// `UIA_TextEditPatternId`.
    static final int UIA_TEXT_EDIT_PATTERN_ID = 10032;

    /// `UIA_SelectionPatternId`.
    static final int UIA_SELECTION_PATTERN_ID = 10001;

    /// `UIA_LegacyIAccessiblePatternId`.
    static final int UIA_LEGACY_IACCESSIBLE_PATTERN_ID = 10018;

    /// `UIA_TextPattern2Id`.
    static final int UIA_TEXT_PATTERN2_ID = 10024;

    /// `UIA_SpreadsheetItemPatternId`.
    static final int UIA_SPREADSHEET_ITEM_PATTERN_ID = 10027;

    /// `UIA_SelectionPattern2Id`.
    static final int UIA_SELECTION_PATTERN2_ID = 10034;

    /// `UIA_TransformPattern2Id`.
    static final int UIA_TRANSFORM_PATTERN2_ID = 10028;

    /// `ZoomUnit_LargeIncrement`.
    static final int ZOOM_UNIT_LARGE_INCREMENT = 1;

    /// First-stable `ITransformProvider2` zoom minimum.
    static final double ZOOM_MINIMUM = 0.5;

    /// First-stable `ITransformProvider2` zoom maximum.
    static final double ZOOM_MAXIMUM = 4.0;

    /// `ROLE_SYSTEM_CLIENT`.
    static final int ROLE_SYSTEM_CLIENT = 10;

    /// `ROLE_SYSTEM_PUSHBUTTON`.
    static final int ROLE_SYSTEM_PUSHBUTTON = 43;

    /// `STATE_SYSTEM_FOCUSABLE`.
    static final int STATE_SYSTEM_FOCUSABLE = 0x0010_0000;

    /// `StyleId_Normal`.
    static final int STYLE_ID_NORMAL = 70000;

    /// `NavigateDirection_Parent`.
    static final int NAVIGATE_DIRECTION_PARENT = 0;

    /// `ProviderOptions_ServerSideProvider`.
    static final int PROVIDER_OPTIONS_SERVER_SIDE = 0x1;

    /// `AnnotationType_Comment`.
    static final int ANNOTATION_TYPE_COMMENT = 60003;

    /// `SynchronizedInputType_KeyDown`.
    static final int SYNCHRONIZED_INPUT_KEY_DOWN = 2;

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

    /// `DockPosition_None`.
    static final int DOCK_POSITION_NONE = 5;

    /// `DockPosition_Top`.
    static final int DOCK_POSITION_TOP = 0;

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

    /// `VT_BSTR`.
    private static final int VT_BSTR = 8;

    /// `VT_R8`.
    private static final int VT_R8 = 5;

    /// `VT_ARRAY`.
    private static final int VT_ARRAY = 0x2000;

    /// `S_OK`.
    private static final int S_OK = 0;

    /// `E_NOINTERFACE`.
    private static final int E_NOINTERFACE = 0x8000_4002;

    /// `E_POINTER`.
    private static final int E_POINTER = 0x8000_4003;

    /// `E_INVALIDARG`.
    private static final int E_INVALIDARG = 0x8007_0057;

    /// `E_OUTOFMEMORY`.
    private static final int E_OUTOFMEMORY = 0x8007_000E;

    /// `VT_UNKNOWN`.
    private static final short VT_UNKNOWN = 13;

    /// `E_NOTIMPL`.
    private static final int E_NOTIMPL = 0x8000_4001;

    /// Generated bindings used to raise UI Automation events.
    private final Win32FfmBindings bindings;

    /// HWND that owns this provider, used by `UiaHostProviderFromHwnd`.
    private final MemorySegment hwnd;

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

    /// Fragment provider COM object.
    private final MemorySegment fragmentObject;

    /// Fragment-root provider COM object.
    private final MemorySegment fragmentRootObject;

    /// Invoke provider COM object.
    private final MemorySegment invokeObject;

    /// Scroll-item provider COM object.
    private final MemorySegment scrollItemObject;

    /// Virtualized-item provider COM object.
    private final MemorySegment virtualizedItemObject;

    /// Dock provider COM object.
    private final MemorySegment dockObject;

    /// Transform provider COM object.
    private final MemorySegment transformObject;

    /// Item-container provider COM object.
    private final MemorySegment itemContainerObject;

    /// COM object returned by a successful [`#invokeFindItemByProperty(String)`].
    private final MemorySegment foundItemObject;

    /// Synchronized-input provider COM object.
    private final MemorySegment synchronizedInputObject;

    /// Multiple-view provider COM object.
    private final MemorySegment multipleViewObject;

    /// Drop-target provider COM object.
    private final MemorySegment dropTargetObject;

    /// Drag provider COM object.
    private final MemorySegment dragObject;

    /// Annotation provider COM object.
    private final MemorySegment annotationObject;

    /// Text-child provider COM object.
    private final MemorySegment textChildObject;

    /// Styles provider COM object.
    private final MemorySegment stylesObject;

    /// Spreadsheet provider COM object.
    private final MemorySegment spreadsheetObject;

    /// Custom-navigation provider COM object.
    private final MemorySegment customNavigationObject;

    /// Object-model provider COM object.
    private final MemorySegment objectModelObject;

    /// Text-edit provider COM object.
    private final MemorySegment textEditObject;

    /// Selection-container provider COM object.
    private final MemorySegment selectionContainerObject;

    /// Legacy IAccessible provider COM object.
    private final MemorySegment legacyAccessibleObject;

    /// Text provider 2 COM object.
    private final MemorySegment text2Object;

    /// Spreadsheet-item provider COM object.
    private final MemorySegment spreadsheetItemObject;

    /// Selection provider 2 COM object.
    private final MemorySegment selection2Object;

    /// Transform provider 2 COM object.
    private final MemorySegment transform2Object;

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

    /// Column-header names from [`SemanticsGrid#columnHeaders()`].
    private final String[] columnHeaderNames;

    /// Row-header names from [`SemanticsGrid#rowHeaders()`].
    private final String[] rowHeaderNames;

    /// This cell's column-header name, empty when absent.
    private final String cellColumnHeader;

    /// This cell's row-header name, empty when absent.
    private final String cellRowHeader;

    /// COM objects returned by [`#getColumnHeaders`].
    private MemorySegment[] columnHeaderObjects = new MemorySegment[0];

    /// COM objects returned by [`#getRowHeaders`].
    private MemorySegment[] rowHeaderObjects = new MemorySegment[0];

    /// COM object returned by [`#getColumnHeaderItems`] when this cell names a column header.
    private MemorySegment cellColumnHeaderObject = MemorySegment.NULL;

    /// COM object returned by [`#getRowHeaderItems`] when this cell names a row header.
    private MemorySegment cellRowHeaderObject = MemorySegment.NULL;

    /// Name and control type for each header provider, keyed by COM object address.
    private final HashMap<Long, HeaderInfo> headerInfos = new HashMap<>();

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

    /// Live layout node whose label is authoritative for text ranges, or `null`.
    private final @Nullable LayoutNode liveNode;

    /// Document length when the current range last covered the full document.
    private int previousDocumentLength;

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

    /// Number of `IVirtualizedItemProvider::Realize` invocations.
    private int virtualizedItemCount;

    /// Number of `ITextRangeProvider2::ShowContextMenu` invocations.
    private int textRangeContextMenuCount;

    /// Number of `IRawElementProviderSimple2::ShowContextMenu` invocations.
    private int simpleContextMenuCount;

    /// `UiaRaiseAutomationEvent` invocations for this provider.
    private int liveRegionChangedCount;

    /// Last HRESULT from [`#raiseLiveRegionChanged()`].
    private int lastLiveRegionEventResult;

    /// Raises `UIA_LiveRegionChangedEventId` when [`LayoutNode#setLabel(String)`] runs on [`#liveNode`].
    private final Runnable liveLabelListener = this::onLiveLabelChanged;

    /// `IRawElementProviderFragment::SetFocus` invocations.
    private int fragmentFocusCount;

    /// `ILegacyIAccessibleProvider::SetValue` invocations.
    private int legacySetValueCount;

    /// Last `ILegacyIAccessibleProvider::SetValue` payload.
    private String lastLegacyValue = "";

    /// Current `IDockProvider` dock position.
    private int dockPosition;

    /// Current `ITransformProvider` X origin.
    private double transformX;

    /// Current `ITransformProvider` Y origin.
    private double transformY;

    /// Current `ITransformProvider` width.
    private double transformWidth;

    /// Current `ITransformProvider` height.
    private double transformHeight;

    /// Current `ITransformProvider` rotation in degrees.
    private double transformRotate;

    /// Current `ITransformProvider2` zoom level.
    private double transformZoom = 1.0;

    /// COM object returned by the last successful [`#invokeFindItemByProperty(String)`].
    private MemorySegment lastFoundItem = MemorySegment.NULL;

    /// Last `ISynchronizedInputProvider::StartListening` input type.
    private int synchronizedInputType;

    /// Number of `ISynchronizedInputProvider::StartListening` invocations.
    private int synchronizedInputStarts;

    /// Number of `ISynchronizedInputProvider::Cancel` invocations.
    private int synchronizedInputCancels;

    /// Current `IMultipleViewProvider` view identifier.
    private int currentView;

    /// Current `IValueProvider` string.
    private String valueText;

    /// Current `IWindowProvider` visual state.
    private int windowVisualState;

    /// Number of `IWindowProvider::Close` invocations.
    private int windowCloseCount;

    /// Whether closed.
    private boolean closed;

    /// Creates one provider.
    private WindowsAutomationProvider(
            Win32FfmBindings bindings,
            SemanticsNode node,
            MemorySegment hwnd,
            @Nullable LayoutNode liveNode
    ) {
        this.bindings = bindings;
        this.node = node;
        this.hwnd = hwnd;
        this.liveNode = liveNode;
        this.valueText = documentText();
        this.windowVisualState = WINDOW_VISUAL_STATE_NORMAL;
        this.dockPosition = DOCK_POSITION_NONE;
        this.transformX = 0.0;
        this.transformY = 0.0;
        this.transformWidth = 1.0;
        this.transformHeight = 1.0;
        this.transformRotate = 0.0;
        this.currentView = 1;
        this.toggleState = initialToggleState(node);
        this.rangeValue = node.rangeValue() == null ? 0.0 : node.rangeValue();
        this.expandState = initialExpandState(node);
        this.itemSelected = node.selected() != null && node.selected();
        SemanticsGrid grid = node.grid();
        this.gridRows = grid == null ? 0 : grid.rowCount();
        this.gridColumns = grid == null ? 0 : grid.columnCount();
        this.columnHeaderNames = grid == null ? new String[0] : grid.columnHeaders();
        this.rowHeaderNames = grid == null ? new String[0] : grid.rowHeaders();
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
        this.cellColumnHeader = item == null ? "" : item.columnHeader();
        this.cellRowHeader = item == null ? "" : item.rowHeader();
        this.fetchedRow = -1;
        this.fetchedColumn = -1;
        this.rangeStart = 0;
        this.previousDocumentLength = documentText().length();
        this.rangeEnd = previousDocumentLength;
        this.arena = Arena.ofConfined();
        this.simpleVtable = arena.allocate(ValueLayout.ADDRESS, 8);
        this.simpleObject = arena.allocate(ValueLayout.ADDRESS);
        simpleObject.set(ValueLayout.ADDRESS, 0L, simpleVtable);
        simpleVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryInterface, failures, arena));
        simpleVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        simpleVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        simpleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIrawElementProviderGetProviderOptionsStub(this::getProviderOptions, failures, arena)
        );
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
        simpleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                6L,
                bindings.createIrawElementProviderSimple2ShowContextMenuStub(this::showSimpleContextMenu, failures, arena)
        );
        simpleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createIrawElementProviderGetHostRawElementProviderStub(this::getHostRawElementProvider, failures, arena)
        );
        this.fragmentObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment fragmentVtable = arena.allocate(ValueLayout.ADDRESS, 9);
        fragmentObject.set(ValueLayout.ADDRESS, 0L, fragmentVtable);
        fragmentVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryFragment, failures, arena));
        fragmentVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        fragmentVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        fragmentVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIrawElementProviderFragmentNavigateStub(this::navigateFragment, failures, arena)
        );
        fragmentVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIrawElementProviderFragmentSetFocusStub(this::setFragmentFocus, failures, arena)
        );
        fragmentVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIrawElementProviderFragmentGetFragmentRootStub(this::getFragmentRoot, failures, arena)
        );
        fragmentVtable.setAtIndex(
                ValueLayout.ADDRESS,
                6L,
                bindings.createIrawElementProviderFragmentGetBoundingRectangleStub(this::getFragmentBoundingRectangle, failures, arena)
        );
        fragmentVtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createIrawElementProviderFragmentGetRuntimeIdStub(this::getFragmentRuntimeId, failures, arena)
        );
        fragmentVtable.setAtIndex(
                ValueLayout.ADDRESS,
                8L,
                bindings.createIrawElementProviderFragmentGetEmbeddedFragmentRootsStub(this::getEmbeddedFragmentRoots, failures, arena)
        );
        this.fragmentRootObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment fragmentRootVtable = arena.allocate(ValueLayout.ADDRESS, 5);
        fragmentRootObject.set(ValueLayout.ADDRESS, 0L, fragmentRootVtable);
        fragmentRootVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryFragmentRoot, failures, arena));
        fragmentRootVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        fragmentRootVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        fragmentRootVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIrawElementProviderFragmentRootElementProviderFromPointStub(this::elementProviderFromPoint, failures, arena)
        );
        fragmentRootVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIrawElementProviderFragmentRootGetFocusStub(this::getFragmentRootFocus, failures, arena)
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
        this.virtualizedItemObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment virtualizedItemVtable = arena.allocate(ValueLayout.ADDRESS, 4);
        virtualizedItemObject.set(ValueLayout.ADDRESS, 0L, virtualizedItemVtable);
        virtualizedItemVtable.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createIunknownQueryInterfaceStub(this::queryVirtualizedItem, failures, arena)
        );
        virtualizedItemVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        virtualizedItemVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        virtualizedItemVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIvirtualizedItemProviderRealizeStub(this::realizeVirtualizedItem, failures, arena)
        );
        this.dockObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment dockVtable = arena.allocate(ValueLayout.ADDRESS, 5);
        dockObject.set(ValueLayout.ADDRESS, 0L, dockVtable);
        dockVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryDock, failures, arena));
        dockVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        dockVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        dockVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIdockProviderSetDockPositionStub(this::setDockPosition, failures, arena));
        dockVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIdockProviderGetDockPositionStub(this::getDockPosition, failures, arena));
        this.transformObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment transformVtable = arena.allocate(ValueLayout.ADDRESS, 9);
        transformObject.set(ValueLayout.ADDRESS, 0L, transformVtable);
        transformVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryTransform, failures, arena));
        transformVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        transformVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        transformVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createItransformProviderMoveStub(this::moveTransform, failures, arena));
        transformVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createItransformProviderResizeStub(this::resizeTransform, failures, arena));
        transformVtable.setAtIndex(ValueLayout.ADDRESS, 5L, bindings.createItransformProviderRotateStub(this::rotateTransform, failures, arena));
        transformVtable.setAtIndex(ValueLayout.ADDRESS, 6L, bindings.createItransformProviderGetCanMoveStub(this::getCanMove, failures, arena));
        transformVtable.setAtIndex(ValueLayout.ADDRESS, 7L, bindings.createItransformProviderGetCanResizeStub(this::getCanResize, failures, arena));
        transformVtable.setAtIndex(ValueLayout.ADDRESS, 8L, bindings.createItransformProviderGetCanRotateStub(this::getCanRotate, failures, arena));
        this.itemContainerObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment itemContainerVtable = arena.allocate(ValueLayout.ADDRESS, 4);
        itemContainerObject.set(ValueLayout.ADDRESS, 0L, itemContainerVtable);
        itemContainerVtable.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createIunknownQueryInterfaceStub(this::queryItemContainer, failures, arena)
        );
        itemContainerVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        itemContainerVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        itemContainerVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIitemContainerProviderFindItemByPropertyStub(this::findItemByProperty, failures, arena)
        );
        this.foundItemObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment foundItemVtable = arena.allocate(ValueLayout.ADDRESS, 6);
        foundItemObject.set(ValueLayout.ADDRESS, 0L, foundItemVtable);
        foundItemVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryFoundItem, failures, arena));
        foundItemVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        foundItemVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        foundItemVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIrawElementProviderGetPatternProviderStub(this::getFoundItemPatternProvider, failures, arena)
        );
        foundItemVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIrawElementProviderGetPropertyValueStub(this::getFoundItemPropertyValue, failures, arena)
        );
        this.synchronizedInputObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment synchronizedInputVtable = arena.allocate(ValueLayout.ADDRESS, 5);
        synchronizedInputObject.set(ValueLayout.ADDRESS, 0L, synchronizedInputVtable);
        synchronizedInputVtable.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createIunknownQueryInterfaceStub(this::querySynchronizedInput, failures, arena)
        );
        synchronizedInputVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        synchronizedInputVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        synchronizedInputVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIsynchronizedInputProviderStartListeningStub(this::startListening, failures, arena)
        );
        synchronizedInputVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIsynchronizedInputProviderCancelStub(this::cancelSynchronizedInput, failures, arena)
        );
        this.multipleViewObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment multipleViewVtable = arena.allocate(ValueLayout.ADDRESS, 6);
        multipleViewObject.set(ValueLayout.ADDRESS, 0L, multipleViewVtable);
        multipleViewVtable.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createIunknownQueryInterfaceStub(this::queryMultipleView, failures, arena)
        );
        multipleViewVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        multipleViewVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        multipleViewVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createImultipleViewProviderSetCurrentViewStub(this::setCurrentView, failures, arena)
        );
        multipleViewVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createImultipleViewProviderGetCurrentViewStub(this::getCurrentView, failures, arena)
        );
        multipleViewVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createImultipleViewProviderGetViewNameStub(this::getViewName, failures, arena)
        );
        this.dropTargetObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment dropTargetVtable = arena.allocate(ValueLayout.ADDRESS, 4);
        dropTargetObject.set(ValueLayout.ADDRESS, 0L, dropTargetVtable);
        dropTargetVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryDropTarget, failures, arena));
        dropTargetVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        dropTargetVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        dropTargetVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIdropTargetProviderGetDropTargetEffectStub(this::getDropTargetEffect, failures, arena)
        );
        this.dragObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment dragVtable = arena.allocate(ValueLayout.ADDRESS, 5);
        dragObject.set(ValueLayout.ADDRESS, 0L, dragVtable);
        dragVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryDrag, failures, arena));
        dragVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        dragVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        dragVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIdragProviderGetIsGrabbedStub(this::getIsGrabbed, failures, arena));
        dragVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIdragProviderGetDropEffectStub(this::getDropEffect, failures, arena));
        this.annotationObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment annotationVtable = arena.allocate(ValueLayout.ADDRESS, 8);
        annotationObject.set(ValueLayout.ADDRESS, 0L, annotationVtable);
        annotationVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryAnnotation, failures, arena));
        annotationVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        annotationVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        annotationVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIannotationProviderGetAnnotationTypeIdStub(this::getAnnotationTypeId, failures, arena)
        );
        annotationVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIannotationProviderGetAnnotationTypeNameStub(this::getAnnotationTypeName, failures, arena)
        );
        annotationVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIannotationProviderGetAuthorStub(this::getAnnotationAuthor, failures, arena)
        );
        annotationVtable.setAtIndex(
                ValueLayout.ADDRESS,
                6L,
                bindings.createIannotationProviderGetDateTimeStub(this::getAnnotationDateTime, failures, arena)
        );
        annotationVtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createIannotationProviderGetTargetStub(this::getAnnotationTarget, failures, arena)
        );
        this.textChildObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment textChildVtable = arena.allocate(ValueLayout.ADDRESS, 5);
        textChildObject.set(ValueLayout.ADDRESS, 0L, textChildVtable);
        textChildVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryTextChild, failures, arena));
        textChildVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        textChildVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        textChildVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createItextChildProviderGetTextContainerStub(this::getTextContainer, failures, arena)
        );
        textChildVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createItextChildProviderGetTextRangeStub(this::getTextChildRange, failures, arena)
        );
        this.stylesObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment stylesVtable = arena.allocate(ValueLayout.ADDRESS, 5);
        stylesObject.set(ValueLayout.ADDRESS, 0L, stylesVtable);
        stylesVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryStyles, failures, arena));
        stylesVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        stylesVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        stylesVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIstylesProviderGetStyleIdStub(this::getStyleId, failures, arena)
        );
        stylesVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIstylesProviderGetStyleNameStub(this::getStyleName, failures, arena)
        );
        this.spreadsheetObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment spreadsheetVtable = arena.allocate(ValueLayout.ADDRESS, 4);
        spreadsheetObject.set(ValueLayout.ADDRESS, 0L, spreadsheetVtable);
        spreadsheetVtable.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createIunknownQueryInterfaceStub(this::querySpreadsheet, failures, arena)
        );
        spreadsheetVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        spreadsheetVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        spreadsheetVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIspreadsheetProviderGetItemByNameStub(this::getSpreadsheetItemByName, failures, arena)
        );
        this.customNavigationObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment customNavigationVtable = arena.allocate(ValueLayout.ADDRESS, 4);
        customNavigationObject.set(ValueLayout.ADDRESS, 0L, customNavigationVtable);
        customNavigationVtable.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createIunknownQueryInterfaceStub(this::queryCustomNavigation, failures, arena)
        );
        customNavigationVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        customNavigationVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        customNavigationVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIcustomNavigationProviderNavigateStub(this::navigateCustom, failures, arena)
        );
        this.objectModelObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment objectModelVtable = arena.allocate(ValueLayout.ADDRESS, 4);
        objectModelObject.set(ValueLayout.ADDRESS, 0L, objectModelVtable);
        objectModelVtable.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createIunknownQueryInterfaceStub(this::queryObjectModel, failures, arena)
        );
        objectModelVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        objectModelVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        objectModelVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIobjectModelProviderGetUnderlyingObjectModelStub(this::getUnderlyingObjectModel, failures, arena)
        );
        this.textEditObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment textEditVtable = arena.allocate(ValueLayout.ADDRESS, 5);
        textEditObject.set(ValueLayout.ADDRESS, 0L, textEditVtable);
        textEditVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryTextEdit, failures, arena));
        textEditVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        textEditVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        textEditVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createItextEditProviderGetActiveCompositionStub(this::getActiveComposition, failures, arena)
        );
        textEditVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createItextEditProviderGetConversionTargetStub(this::getConversionTarget, failures, arena)
        );
        this.selectionContainerObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment selectionContainerVtable = arena.allocate(ValueLayout.ADDRESS, 5);
        selectionContainerObject.set(ValueLayout.ADDRESS, 0L, selectionContainerVtable);
        selectionContainerVtable.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createIunknownQueryInterfaceStub(this::querySelectionContainer, failures, arena)
        );
        selectionContainerVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        selectionContainerVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        selectionContainerVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIselectionProviderGetCanSelectMultipleStub(this::getCanSelectMultiple, failures, arena)
        );
        selectionContainerVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIselectionProviderGetIsSelectionRequiredStub(this::getIsSelectionRequired, failures, arena)
        );
        this.legacyAccessibleObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment legacyAccessibleVtable = arena.allocate(ValueLayout.ADDRESS, 14);
        legacyAccessibleObject.set(ValueLayout.ADDRESS, 0L, legacyAccessibleVtable);
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createIunknownQueryInterfaceStub(this::queryLegacyAccessible, failures, arena)
        );
        legacyAccessibleVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        legacyAccessibleVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIlegacyIaccessibleProviderGetChildIdStub(this::getLegacyChildId, failures, arena)
        );
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIlegacyIaccessibleProviderGetNameStub(this::getLegacyName, failures, arena)
        );
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIlegacyIaccessibleProviderGetRoleStub(this::getLegacyRole, failures, arena)
        );
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                6L,
                bindings.createIlegacyIaccessibleProviderDoDefaultActionStub(this::doLegacyDefaultAction, failures, arena)
        );
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createIlegacyIaccessibleProviderGetValueStub(this::getLegacyValue, failures, arena)
        );
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                8L,
                bindings.createIlegacyIaccessibleProviderGetStateStub(this::getLegacyState, failures, arena)
        );
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                9L,
                bindings.createIlegacyIaccessibleProviderGetDescriptionStub(this::getLegacyDescription, failures, arena)
        );
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                10L,
                bindings.createIlegacyIaccessibleProviderGetDefaultActionStub(this::getLegacyDefaultAction, failures, arena)
        );
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                11L,
                bindings.createIlegacyIaccessibleProviderGetKeyboardShortcutStub(this::getLegacyKeyboardShortcut, failures, arena)
        );
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                12L,
                bindings.createIlegacyIaccessibleProviderGetHelpStub(this::getLegacyHelp, failures, arena)
        );
        legacyAccessibleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                13L,
                bindings.createIlegacyIaccessibleProviderSetValueStub(this::setLegacyValue, failures, arena)
        );
        this.text2Object = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment text2Vtable = arena.allocate(ValueLayout.ADDRESS, 5);
        text2Object.set(ValueLayout.ADDRESS, 0L, text2Vtable);
        text2Vtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryText2, failures, arena));
        text2Vtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        text2Vtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        text2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createItextProvider2GetCaretRangeStub(this::getCaretRange, failures, arena)
        );
        text2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createItextProvider2RangeFromAnnotationStub(this::rangeFromAnnotation, failures, arena)
        );
        this.spreadsheetItemObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment spreadsheetItemVtable = arena.allocate(ValueLayout.ADDRESS, 4);
        spreadsheetItemObject.set(ValueLayout.ADDRESS, 0L, spreadsheetItemVtable);
        spreadsheetItemVtable.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createIunknownQueryInterfaceStub(this::querySpreadsheetItem, failures, arena)
        );
        spreadsheetItemVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        spreadsheetItemVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        spreadsheetItemVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIspreadsheetItemProviderGetFormulaStub(this::getSpreadsheetFormula, failures, arena)
        );
        this.selection2Object = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment selection2Vtable = arena.allocate(ValueLayout.ADDRESS, 7);
        selection2Object.set(ValueLayout.ADDRESS, 0L, selection2Vtable);
        selection2Vtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::querySelection2, failures, arena));
        selection2Vtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        selection2Vtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        selection2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIselectionProvider2GetItemCountStub(this::getSelectionItemCount, failures, arena)
        );
        selection2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIselectionProvider2GetCurrentSelectedItemStub(this::getCurrentSelectedItem, failures, arena)
        );
        selection2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIselectionProvider2GetFirstSelectedItemStub(this::getFirstSelectedItem, failures, arena)
        );
        selection2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                6L,
                bindings.createIselectionProvider2GetLastSelectedItemStub(this::getLastSelectedItem, failures, arena)
        );
        this.transform2Object = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment transform2Vtable = arena.allocate(ValueLayout.ADDRESS, 9);
        transform2Object.set(ValueLayout.ADDRESS, 0L, transform2Vtable);
        transform2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createIunknownQueryInterfaceStub(this::queryTransform2, failures, arena)
        );
        transform2Vtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        transform2Vtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        transform2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createItransformProvider2ZoomStub(this::zoomTransform, failures, arena)
        );
        transform2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createItransformProvider2GetCanZoomStub(this::getCanZoom, failures, arena)
        );
        transform2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createItransformProvider2GetZoomLevelStub(this::getZoomLevel, failures, arena)
        );
        transform2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                6L,
                bindings.createItransformProvider2ZoomByUnitStub(this::zoomByUnit, failures, arena)
        );
        transform2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createItransformProvider2GetZoomMinimumStub(this::getZoomMinimum, failures, arena)
        );
        transform2Vtable.setAtIndex(
                ValueLayout.ADDRESS,
                8L,
                bindings.createItransformProvider2GetZoomMaximumStub(this::getZoomMaximum, failures, arena)
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
        this.columnHeaderObjects = allocateHeaderObjects(columnHeaderNames, failures);
        this.rowHeaderObjects = allocateHeaderObjects(rowHeaderNames, failures);
        this.cellColumnHeaderObject = cellColumnHeader.isEmpty()
                ? MemorySegment.NULL
                : allocateHeaderObject(cellColumnHeader, failures);
        this.cellRowHeaderObject = cellRowHeader.isEmpty()
                ? MemorySegment.NULL
                : allocateHeaderObject(cellRowHeader, failures);
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
        textVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createItextProviderGetRangeStub(this::getVisibleRanges, failures, arena)
        );
        textVtable.setAtIndex(
                ValueLayout.ADDRESS,
                6L,
                bindings.createItextProviderGetRangeStub(this::rangeFromChild, failures, arena)
        );
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
        MemorySegment textRangeVtable = arena.allocate(ValueLayout.ADDRESS, 22);
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
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                21L,
                bindings.createItextRangeProvider2ShowContextMenuStub(this::showTextRangeContextMenu, failures, arena)
        );
        if (liveNode != null) {
            liveNode.addLabelListener(liveLabelListener);
        }
    }

    /// Creates a provider for one semantics node.
    ///
    /// @param libraries the session libraries
    /// @param node the projected node
    /// @return the provider
    public static WindowsAutomationProvider of(WindowsLibraries libraries, SemanticsNode node) {
        return of(libraries, node, MemorySegment.NULL);
    }

    /// Creates a provider for one semantics node on `hwnd`.
    ///
    /// @param libraries the session libraries
    /// @param node the projected node
    /// @param hwnd the owning window
    /// @return the provider
    public static WindowsAutomationProvider of(
            WindowsLibraries libraries,
            SemanticsNode node,
            MemorySegment hwnd
    ) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(hwnd, "hwnd");
        return new WindowsAutomationProvider(libraries.bindings(), node, hwnd, null);
    }

    /// Creates a provider whose text ranges follow `live` after [`LayoutNode#setLabel(String)`].
    ///
    /// @param libraries the session libraries
    /// @param live the live layout node
    /// @param hwnd the owning window
    /// @return the provider
    public static WindowsAutomationProvider of(
            WindowsLibraries libraries,
            LayoutNode live,
            MemorySegment hwnd
    ) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(live, "live");
        Objects.requireNonNull(hwnd, "hwnd");
        SemanticsNode snapshot = new SemanticsNode(
                live.id(),
                live.role(),
                live.label(),
                live.actions(),
                live.bounds(),
                false,
                live.selected(),
                live.rangeValue(),
                live.liveRegion(),
                live.textRange(),
                live.grid(),
                live.scroll(),
                live.gridItem(),
                live.disabled(),
                live.readOnly(),
                live.hint(),
                live.focusable(),
                live.password(),
                live.accessKey(),
                live.acceleratorKey(),
                live.required(),
                live.itemStatus(),
                live.itemType(),
                live.locale(),
                live.level(),
                live.positionInSet(),
                live.sizeOfSet(),
                live.description(),
                live.error(),
                live.landmarkType(),
                live.localizedLandmarkType(),
                live.ariaRole(),
                live.ariaProperties(),
                live.controllerFor(),
                live.describedBy(),
                live.flowsTo(),
                live.labeledBy(),
                live.flowsFrom(),
                live.optimizeForVisualContent(),
                live.fillColor(),
                live.outlineColor(),
                live.fillType(),
                live.visualEffects(),
                live.outlineThickness(),
                live.rotation(),
                live.peripheral(),
                live.annotationType(),
                live.annotationObjects()
        );
        return new WindowsAutomationProvider(libraries.bindings(), snapshot, hwnd, live);
    }

    /// Raises `UIA_LiveRegionChangedEventId` through generated `UiaRaiseAutomationEvent`.
    ///
    /// Raises `UIA_LiveRegionChangedEventId` after a live-region [`LayoutNode#setLabel(String)`].
    ///
    /// [`#of(WindowsLibraries, LayoutNode, MemorySegment)`] registers this method as the
    /// live node's label listener so announcement `setLabel` callers raise without invoking
    /// this method directly.
    ///
    /// @return the native HRESULT
    public int raiseLiveRegionChanged() {
        requireOpen();
        syncDocumentRange();
        liveRegionChangedCount++;
        if (hwnd.address() == 0L) {
            lastLiveRegionEventResult = E_POINTER;
            return lastLiveRegionEventResult;
        }
        MemorySegment hostOut = arena.allocate(ValueLayout.ADDRESS);
        hostOut.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        int hostResult = bindings.uiaHostProviderFromHwnd(hwnd, hostOut);
        if (hostResult < 0) {
            lastLiveRegionEventResult = hostResult;
            return lastLiveRegionEventResult;
        }
        MemorySegment host = hostOut.get(ValueLayout.ADDRESS, 0L);
        if (host.address() == 0L) {
            lastLiveRegionEventResult = E_POINTER;
            return lastLiveRegionEventResult;
        }
        lastLiveRegionEventResult = raiseOnHost(host, UIA_LIVE_REGION_CHANGED_EVENT_ID);
        return lastLiveRegionEventResult;
    }

    /// Raises a live-region event after the live node's announcement text changes.
    private void onLiveLabelChanged() {
        if (closed || liveNode == null || liveNode.liveRegion() == SemanticsLiveRegion.OFF) {
            return;
        }
        raiseLiveRegionChanged();
    }

    /// Raises `UIA_Text_TextChangedEventId` through generated `UiaRaiseAutomationEvent`.
    ///
    /// @return the native HRESULT
    public int raiseTextChanged() {
        return raiseHostEvent(UIA_TEXT_CHANGED_EVENT_ID);
    }

    /// Raises `UIA_Text_TextSelectionChangedEventId` through generated `UiaRaiseAutomationEvent`.
    ///
    /// @return the native HRESULT
    public int raiseTextSelectionChanged() {
        return raiseHostEvent(UIA_TEXT_SELECTION_CHANGED_EVENT_ID);
    }

    /// Raises one UIA event on the host provider for this HWND.
    ///
    /// @param eventId the UIA event identifier
    /// @return the native HRESULT
    private int raiseHostEvent(int eventId) {
        requireOpen();
        if (hwnd.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment hostOut = arena.allocate(ValueLayout.ADDRESS);
        hostOut.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        int hostResult = bindings.uiaHostProviderFromHwnd(hwnd, hostOut);
        if (hostResult < 0) {
            return hostResult;
        }
        MemorySegment host = hostOut.get(ValueLayout.ADDRESS, 0L);
        if (host.address() == 0L) {
            return E_POINTER;
        }
        return raiseOnHost(host, eventId);
    }

    /// Raises `eventId` on `host` and releases the host provider.
    ///
    /// @param host the `UiaHostProviderFromHwnd` object
    /// @param eventId the UIA event identifier
    /// @return the native HRESULT
    private int raiseOnHost(MemorySegment host, int eventId) {
        int result = bindings.uiaRaiseAutomationEvent(host, eventId);
        MemorySegment vtable = host.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L);
        MemorySegment release = vtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 3L)
                .getAtIndex(ValueLayout.ADDRESS, 2L);
        Win32FfmBindings.invokeIunknownReleasePointer(release, host);
        return result;
    }

    /// Returns whether generated `UiaClientsAreListening` reports an automation client.
    ///
    /// @return whether a client is listening
    public boolean clientsAreListening() {
        requireOpen();
        return bindings.uiaClientsAreListening() != 0;
    }

    /// Returns how many times [`#raiseLiveRegionChanged()`] invoked the generated symbol.
    ///
    /// @return the count
    public int liveRegionChangedCount() {
        return liveRegionChangedCount;
    }

    /// Returns the last HRESULT from [`#raiseLiveRegionChanged()`].
    ///
    /// @return the HRESULT, or `0` before the first raise
    public int lastLiveRegionEventResult() {
        return lastLiveRegionEventResult;
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

    /// Invokes `GetPropertyValue` and reads a `VT_R8` payload.
    ///
    /// @param propertyId the UIA property identifier
    /// @return the `VT_R8` payload
    public double invokePropertyValueDouble(int propertyId) {
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
        return value.get(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET);
    }

    /// Invokes `GetPropertyValue` for `UIA_BoundingRectanglePropertyId`.
    ///
    /// The first-stable payload is a packed `VT_ARRAY | VT_R8` of four doubles:
    /// left, top, width, height.
    ///
    /// @return `{left, top, width, height}`
    public float[] invokeBoundingRectangle() {
        requireOpen();
        MemorySegment getProperty = simpleVtable.getAtIndex(ValueLayout.ADDRESS, 5L);
        MemorySegment value = arena.allocate(Win32Layouts.VARIANT);
        value.fill((byte) 0);
        int result = Win32FfmBindings.invokeIrawElementProviderGetPropertyValuePointer(
                getProperty,
                simpleObject,
                UIA_BOUNDING_RECTANGLE_PROPERTY_ID,
                value
        );
        requireSuccess("GetPropertyValue", result);
        short vt = value.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET);
        if (vt != (short) (VT_ARRAY | VT_R8)) {
            throw new IllegalStateException("BoundingRectangle variant is " + vt);
        }
        MemorySegment rect = value.get(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET).reinterpret(32);
        return new float[] {
                (float) rect.get(ValueLayout.JAVA_DOUBLE, 0L),
                (float) rect.get(ValueLayout.JAVA_DOUBLE, 8L),
                (float) rect.get(ValueLayout.JAVA_DOUBLE, 16L),
                (float) rect.get(ValueLayout.JAVA_DOUBLE, 24L)
        };
    }

    /// Invokes `GetPropertyValue` for `UIA_ClickablePointPropertyId`.
    ///
    /// The first-stable payload is a packed `VT_ARRAY | VT_R8` of two doubles: center X, center Y.
    ///
    /// @return `{centerX, centerY}`
    public float[] invokeClickablePoint() {
        requireOpen();
        MemorySegment getProperty = simpleVtable.getAtIndex(ValueLayout.ADDRESS, 5L);
        MemorySegment value = arena.allocate(Win32Layouts.VARIANT);
        value.fill((byte) 0);
        int result = Win32FfmBindings.invokeIrawElementProviderGetPropertyValuePointer(
                getProperty,
                simpleObject,
                UIA_CLICKABLE_POINT_PROPERTY_ID,
                value
        );
        requireSuccess("GetPropertyValue", result);
        short vt = value.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET);
        if (vt != (short) (VT_ARRAY | VT_R8)) {
            throw new IllegalStateException("ClickablePoint variant is " + vt);
        }
        MemorySegment point = value.get(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET).reinterpret(16);
        return new float[] {
                (float) point.get(ValueLayout.JAVA_DOUBLE, 0L),
                (float) point.get(ValueLayout.JAVA_DOUBLE, 8L)
        };
    }

    /// Invokes `GetPropertyValue` for `UIA_CenterPointPropertyId`.
    ///
    /// The first-stable payload is a packed `VT_ARRAY | VT_R8` of two doubles: center X, center Y.
    ///
    /// @return `{centerX, centerY}`
    public float[] invokeCenterPoint() {
        requireOpen();
        MemorySegment getProperty = simpleVtable.getAtIndex(ValueLayout.ADDRESS, 5L);
        MemorySegment value = arena.allocate(Win32Layouts.VARIANT);
        value.fill((byte) 0);
        int result = Win32FfmBindings.invokeIrawElementProviderGetPropertyValuePointer(
                getProperty,
                simpleObject,
                UIA_CENTER_POINT_PROPERTY_ID,
                value
        );
        requireSuccess("GetPropertyValue", result);
        short vt = value.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET);
        if (vt != (short) (VT_ARRAY | VT_R8)) {
            throw new IllegalStateException("CenterPoint variant is " + vt);
        }
        MemorySegment point = value.get(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET).reinterpret(16);
        return new float[] {
                (float) point.get(ValueLayout.JAVA_DOUBLE, 0L),
                (float) point.get(ValueLayout.JAVA_DOUBLE, 8L)
        };
    }

    /// Invokes `GetPropertyValue` for `UIA_SizePropertyId`.
    ///
    /// The first-stable payload is a packed `VT_ARRAY | VT_R8` of two doubles: width, height.
    ///
    /// @return `{width, height}`
    public float[] invokeSize() {
        requireOpen();
        MemorySegment getProperty = simpleVtable.getAtIndex(ValueLayout.ADDRESS, 5L);
        MemorySegment value = arena.allocate(Win32Layouts.VARIANT);
        value.fill((byte) 0);
        int result = Win32FfmBindings.invokeIrawElementProviderGetPropertyValuePointer(
                getProperty,
                simpleObject,
                UIA_SIZE_PROPERTY_ID,
                value
        );
        requireSuccess("GetPropertyValue", result);
        short vt = value.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET);
        if (vt != (short) (VT_ARRAY | VT_R8)) {
            throw new IllegalStateException("Size variant is " + vt);
        }
        MemorySegment size = value.get(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET).reinterpret(16);
        return new float[] {
                (float) size.get(ValueLayout.JAVA_DOUBLE, 0L),
                (float) size.get(ValueLayout.JAVA_DOUBLE, 8L)
        };
    }

    /// Invokes `GetPropertyValue` and decodes a `VT_BSTR` payload.
    ///
    /// @param propertyId the UIA property identifier
    /// @return the decoded string, or empty when the variant is not a `BSTR`
    public String invokePropertyValueString(int propertyId) {
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
        if (value.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET) != VT_BSTR) {
            return "";
        }
        return decodeUtf16(value.get(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET));
    }

    /// Reads `GetPropertyValue` as a `VT_UNKNOWN` pointer.
    ///
    /// @param propertyId the UIA property identifier
    /// @return whether a non-null interface pointer was returned
    public boolean invokePropertyValueUnknown(int propertyId) {
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
        if (value.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET) != VT_UNKNOWN) {
            return false;
        }
        return value.get(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET).address() != 0L;
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

    /// Invokes `IVirtualizedItemProvider::Realize` through the generated COM vtable.
    ///
    /// @return the invocation count after the call
    public int invokeVirtualizedItem() {
        requireOpen();
        requireSuccess(
                "IVirtualizedItemProvider::Realize",
                Win32FfmBindings.invokeIvirtualizedItemProviderRealizePointer(
                        functionAt(virtualizedItemObject, 3),
                        virtualizedItemObject
                )
        );
        return virtualizedItemCount;
    }

    /// Invokes `IDockProvider::SetDockPosition` through the generated COM vtable.
    ///
    /// @param position the `DockPosition` value
    /// @return the stored dock position
    public int setDockPosition(int position) {
        requireOpen();
        requireSuccess(
                "IDockProvider::SetDockPosition",
                Win32FfmBindings.invokeIdockProviderSetDockPositionPointer(
                        functionAt(dockObject, 3),
                        dockObject,
                        position
                )
        );
        return dockPosition;
    }

    /// Reads `IDockProvider::get_DockPosition` through the generated COM vtable.
    ///
    /// @return the stored dock position
    public int dockPosition() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IDockProvider::get_DockPosition",
                Win32FfmBindings.invokeIdockProviderGetDockPositionPointer(
                        functionAt(dockObject, 4),
                        dockObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ITransformProvider::Move` through the generated COM vtable.
    ///
    /// @param x the origin X
    /// @param y the origin Y
    /// @return the stored X origin
    public double moveTransform(double x, double y) {
        requireOpen();
        requireSuccess(
                "ITransformProvider::Move",
                Win32FfmBindings.invokeItransformProviderMovePointer(functionAt(transformObject, 3), transformObject, x, y)
        );
        return transformX;
    }

    /// Invokes `ITransformProvider::Resize` through the generated COM vtable.
    ///
    /// @param width the width
    /// @param height the height
    /// @return the stored width
    public double resizeTransform(double width, double height) {
        requireOpen();
        requireSuccess(
                "ITransformProvider::Resize",
                Win32FfmBindings.invokeItransformProviderResizePointer(
                        functionAt(transformObject, 4),
                        transformObject,
                        width,
                        height
                )
        );
        return transformWidth;
    }

    /// Invokes `ITransformProvider::Rotate` through the generated COM vtable.
    ///
    /// @param degrees the rotation in degrees
    /// @return the stored rotation
    public double rotateTransform(double degrees) {
        requireOpen();
        requireSuccess(
                "ITransformProvider::Rotate",
                Win32FfmBindings.invokeItransformProviderRotatePointer(
                        functionAt(transformObject, 5),
                        transformObject,
                        degrees
                )
        );
        return transformRotate;
    }

    /// Reads `ITransformProvider::get_CanMove` through the generated COM vtable.
    ///
    /// @return whether move is advertised
    public boolean canMove() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITransformProvider::get_CanMove",
                Win32FfmBindings.invokeItransformProviderGetCanMovePointer(
                        functionAt(transformObject, 6),
                        transformObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Reads `ITransformProvider::get_CanResize` through the generated COM vtable.
    ///
    /// @return whether resize is advertised
    public boolean canResize() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITransformProvider::get_CanResize",
                Win32FfmBindings.invokeItransformProviderGetCanResizePointer(
                        functionAt(transformObject, 7),
                        transformObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Reads `ITransformProvider::get_CanRotate` through the generated COM vtable.
    ///
    /// @return whether rotate is advertised
    public boolean canRotate() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITransformProvider::get_CanRotate",
                Win32FfmBindings.invokeItransformProviderGetCanRotatePointer(
                        functionAt(transformObject, 8),
                        transformObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Invokes `IItemContainerProvider::FindItemByProperty` through the generated COM vtable.
    ///
    /// @param name the `UIA_NamePropertyId` string
    /// @return whether a matching item pointer was returned
    public boolean invokeFindItemByProperty(String name) {
        requireOpen();
        Objects.requireNonNull(name, "name");
        MemorySegment variant = arena.allocate(Win32Layouts.VARIANT);
        variant.fill((byte) 0);
        variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_BSTR);
        MemorySegment chars = arena.allocate((name.length() + 1L) * 2L);
        byte[] utf16 = name.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment.copy(utf16, 0, chars, ValueLayout.JAVA_BYTE, 0L, utf16.length);
        variant.set(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET, chars);
        MemorySegment found = arena.allocate(ValueLayout.ADDRESS);
        found.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IItemContainerProvider::FindItemByProperty",
                Win32FfmBindings.invokeIitemContainerProviderFindItemByPropertyPointer(
                        functionAt(itemContainerObject, 3),
                        itemContainerObject,
                        MemorySegment.NULL,
                        UIA_NAME_PROPERTY_ID,
                        variant,
                        found
                )
        );
        MemorySegment fetched = found.get(ValueLayout.ADDRESS, 0L);
        if (fetched.address() == 0L) {
            lastFoundItem = MemorySegment.NULL;
            return false;
        }
        lastFoundItem = fetched.byteSize() == 0L
                ? fetched.reinterpret(ValueLayout.ADDRESS.byteSize())
                : fetched;
        return true;
    }

    /// Invokes `ISynchronizedInputProvider::StartListening` through the generated COM vtable.
    ///
    /// @param inputType the `SynchronizedInputType` value
    /// @return the invocation count after the call
    public int startListening(int inputType) {
        requireOpen();
        requireSuccess(
                "ISynchronizedInputProvider::StartListening",
                Win32FfmBindings.invokeIsynchronizedInputProviderStartListeningPointer(
                        functionAt(synchronizedInputObject, 3),
                        synchronizedInputObject,
                        inputType
                )
        );
        return synchronizedInputStarts;
    }

    /// Invokes `ISynchronizedInputProvider::Cancel` through the generated COM vtable.
    ///
    /// @return the cancel count after the call
    public int cancelSynchronizedInput() {
        requireOpen();
        requireSuccess(
                "ISynchronizedInputProvider::Cancel",
                Win32FfmBindings.invokeIsynchronizedInputProviderCancelPointer(
                        functionAt(synchronizedInputObject, 4),
                        synchronizedInputObject
                )
        );
        return synchronizedInputCancels;
    }

    /// Invokes `IMultipleViewProvider::SetCurrentView` through the generated COM vtable.
    ///
    /// @param viewId the view identifier
    /// @return the stored view identifier
    public int setCurrentView(int viewId) {
        requireOpen();
        requireSuccess(
                "IMultipleViewProvider::SetCurrentView",
                Win32FfmBindings.invokeImultipleViewProviderSetCurrentViewPointer(
                        functionAt(multipleViewObject, 3),
                        multipleViewObject,
                        viewId
                )
        );
        return currentView;
    }

    /// Reads `IMultipleViewProvider::get_CurrentView` through the generated COM vtable.
    ///
    /// @return the stored view identifier
    public int currentView() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IMultipleViewProvider::get_CurrentView",
                Win32FfmBindings.invokeImultipleViewProviderGetCurrentViewPointer(
                        functionAt(multipleViewObject, 4),
                        multipleViewObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Reads `IMultipleViewProvider::GetViewName` through the generated COM vtable.
    ///
    /// @param viewId the view identifier
    /// @return the view name
    public String viewName(int viewId) {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IMultipleViewProvider::GetViewName",
                Win32FfmBindings.invokeImultipleViewProviderGetViewNamePointer(
                        functionAt(multipleViewObject, 5),
                        multipleViewObject,
                        viewId,
                        result
                )
        );
        MemorySegment chars = result.get(ValueLayout.ADDRESS, 0L);
        if (chars.address() == 0L) {
            return "";
        }
        MemorySegment readable = chars.byteSize() < 2L ? chars.reinterpret(256) : chars;
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < 64; index++) {
            char unit = readable.getAtIndex(ValueLayout.JAVA_CHAR, index);
            if (unit == 0) {
                break;
            }
            text.append(unit);
        }
        return text.toString();
    }

    /// Reads `IDropTargetProvider::get_DropTargetEffect` through the generated COM vtable.
    ///
    /// @return the advertised drop effect
    public String dropTargetEffect() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IDropTargetProvider::get_DropTargetEffect",
                Win32FfmBindings.invokeIdropTargetProviderGetDropTargetEffectPointer(
                        functionAt(dropTargetObject, 3),
                        dropTargetObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Reads `IDragProvider::get_IsGrabbed` through the generated COM vtable.
    ///
    /// @return whether the item is grabbed
    public boolean isGrabbed() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IDragProvider::get_IsGrabbed",
                Win32FfmBindings.invokeIdragProviderGetIsGrabbedPointer(functionAt(dragObject, 3), dragObject, state)
        );
        return state.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Reads `IDragProvider::get_DropEffect` through the generated COM vtable.
    ///
    /// @return the advertised drop effect
    public String dropEffect() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IDragProvider::get_DropEffect",
                Win32FfmBindings.invokeIdragProviderGetDropEffectPointer(functionAt(dragObject, 4), dragObject, result)
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Reads `IAnnotationProvider::get_AnnotationTypeId` through the generated COM vtable.
    ///
    /// @return the annotation type identifier
    public int annotationTypeId() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IAnnotationProvider::get_AnnotationTypeId",
                Win32FfmBindings.invokeIannotationProviderGetAnnotationTypeIdPointer(
                        functionAt(annotationObject, 3),
                        annotationObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Reads `IAnnotationProvider::get_AnnotationTypeName` through the generated COM vtable.
    ///
    /// @return the annotation type name
    public String annotationTypeName() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IAnnotationProvider::get_AnnotationTypeName",
                Win32FfmBindings.invokeIannotationProviderGetAnnotationTypeNamePointer(
                        functionAt(annotationObject, 4),
                        annotationObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Reads `IAnnotationProvider::get_Author` through the generated COM vtable.
    ///
    /// @return the author string
    public String annotationAuthor() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IAnnotationProvider::get_Author",
                Win32FfmBindings.invokeIannotationProviderGetAuthorPointer(
                        functionAt(annotationObject, 5),
                        annotationObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Reads `IAnnotationProvider::get_DateTime` through the generated COM vtable.
    ///
    /// @return the date-time string
    public String annotationDateTime() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IAnnotationProvider::get_DateTime",
                Win32FfmBindings.invokeIannotationProviderGetDateTimePointer(
                        functionAt(annotationObject, 6),
                        annotationObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Reads `IAnnotationProvider::get_Target` through the generated COM vtable.
    ///
    /// @return whether a target pointer was returned
    public boolean invokeAnnotationTarget() {
        requireOpen();
        MemorySegment target = arena.allocate(ValueLayout.ADDRESS);
        target.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IAnnotationProvider::get_Target",
                Win32FfmBindings.invokeIannotationProviderGetTargetPointer(
                        functionAt(annotationObject, 7),
                        annotationObject,
                        target
                )
        );
        return target.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `ITextChildProvider::get_TextContainer` through the generated COM vtable.
    ///
    /// @return whether a container pointer was returned
    public boolean invokeTextContainer() {
        requireOpen();
        MemorySegment container = arena.allocate(ValueLayout.ADDRESS);
        container.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextChildProvider::get_TextContainer",
                Win32FfmBindings.invokeItextChildProviderGetTextContainerPointer(
                        functionAt(textChildObject, 3),
                        textChildObject,
                        container
                )
        );
        return container.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `ITextChildProvider::get_TextRange` through the generated COM vtable.
    ///
    /// @return whether a range pointer was returned
    public boolean invokeTextChildRange() {
        requireOpen();
        MemorySegment range = arena.allocate(ValueLayout.ADDRESS);
        range.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextChildProvider::get_TextRange",
                Win32FfmBindings.invokeItextChildProviderGetTextRangePointer(
                        functionAt(textChildObject, 4),
                        textChildObject,
                        range
                )
        );
        return range.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `IStylesProvider::get_StyleId` through the generated COM vtable.
    ///
    /// @return the style identifier
    public int styleId() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IStylesProvider::get_StyleId",
                Win32FfmBindings.invokeIstylesProviderGetStyleIdPointer(functionAt(stylesObject, 3), stylesObject, state)
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Reads `IStylesProvider::get_StyleName` through the generated COM vtable.
    ///
    /// @return the style name
    public String styleName() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IStylesProvider::get_StyleName",
                Win32FfmBindings.invokeIstylesProviderGetStyleNamePointer(
                        functionAt(stylesObject, 4),
                        stylesObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Invokes `ISpreadsheetProvider::GetItemByName` through the generated COM vtable.
    ///
    /// @param name the item name
    /// @return whether a matching item pointer was returned
    public boolean invokeSpreadsheetItem(String name) {
        requireOpen();
        Objects.requireNonNull(name, "name");
        MemorySegment chars = arena.allocate((name.length() + 1L) * 2L);
        byte[] utf16 = name.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment.copy(utf16, 0, chars, ValueLayout.JAVA_BYTE, 0L, utf16.length);
        MemorySegment found = arena.allocate(ValueLayout.ADDRESS);
        found.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ISpreadsheetProvider::GetItemByName",
                Win32FfmBindings.invokeIspreadsheetProviderGetItemByNamePointer(
                        functionAt(spreadsheetObject, 3),
                        spreadsheetObject,
                        chars,
                        found
                )
        );
        return found.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `ICustomNavigationProvider::Navigate` through the generated COM vtable.
    ///
    /// @param direction a `NavigateDirection` value
    /// @return whether a target pointer was returned
    public boolean invokeNavigate(int direction) {
        requireOpen();
        MemorySegment target = arena.allocate(ValueLayout.ADDRESS);
        target.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ICustomNavigationProvider::Navigate",
                Win32FfmBindings.invokeIcustomNavigationProviderNavigatePointer(
                        functionAt(customNavigationObject, 3),
                        customNavigationObject,
                        direction,
                        target
                )
        );
        return target.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `IObjectModelProvider::GetUnderlyingObjectModel` through the generated COM vtable.
    ///
    /// @return whether an object-model pointer was returned
    public boolean invokeObjectModel() {
        requireOpen();
        MemorySegment model = arena.allocate(ValueLayout.ADDRESS);
        model.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IObjectModelProvider::GetUnderlyingObjectModel",
                Win32FfmBindings.invokeIobjectModelProviderGetUnderlyingObjectModelPointer(
                        functionAt(objectModelObject, 3),
                        objectModelObject,
                        model
                )
        );
        return model.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `ITextEditProvider::GetActiveComposition` through the generated COM vtable.
    ///
    /// @return whether a composition range pointer was returned
    public boolean invokeActiveComposition() {
        requireOpen();
        MemorySegment range = arena.allocate(ValueLayout.ADDRESS);
        range.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextEditProvider::GetActiveComposition",
                Win32FfmBindings.invokeItextEditProviderGetActiveCompositionPointer(
                        functionAt(textEditObject, 3),
                        textEditObject,
                        range
                )
        );
        return range.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `ITextEditProvider::GetConversionTarget` through the generated COM vtable.
    ///
    /// @return whether a conversion-target pointer was returned
    public boolean invokeConversionTarget() {
        requireOpen();
        MemorySegment range = arena.allocate(ValueLayout.ADDRESS);
        range.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextEditProvider::GetConversionTarget",
                Win32FfmBindings.invokeItextEditProviderGetConversionTargetPointer(
                        functionAt(textEditObject, 4),
                        textEditObject,
                        range
                )
        );
        return range.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `ISelectionProvider::get_CanSelectMultiple` through the generated COM vtable.
    ///
    /// @return whether multiple selection is advertised
    public boolean canSelectMultiple() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ISelectionProvider::get_CanSelectMultiple",
                Win32FfmBindings.invokeIselectionProviderGetCanSelectMultiplePointer(
                        functionAt(selectionContainerObject, 3),
                        selectionContainerObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Reads `ISelectionProvider::get_IsSelectionRequired` through the generated COM vtable.
    ///
    /// @return whether a selection is required
    public boolean isSelectionRequired() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ISelectionProvider::get_IsSelectionRequired",
                Win32FfmBindings.invokeIselectionProviderGetIsSelectionRequiredPointer(
                        functionAt(selectionContainerObject, 4),
                        selectionContainerObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Reads `ILegacyIAccessibleProvider::get_ChildId` through the generated COM vtable.
    ///
    /// @return the MSAA child identifier
    public int legacyChildId() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, -1);
        requireSuccess(
                "ILegacyIAccessibleProvider::get_ChildId",
                Win32FfmBindings.invokeIlegacyIaccessibleProviderGetChildIdPointer(
                        functionAt(legacyAccessibleObject, 3),
                        legacyAccessibleObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Reads `ILegacyIAccessibleProvider::get_Name` through the generated COM vtable.
    ///
    /// @return the accessible name
    public String legacyName() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ILegacyIAccessibleProvider::get_Name",
                Win32FfmBindings.invokeIlegacyIaccessibleProviderGetNamePointer(
                        functionAt(legacyAccessibleObject, 4),
                        legacyAccessibleObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Reads `ILegacyIAccessibleProvider::get_Role` through the generated COM vtable.
    ///
    /// @return the MSAA role
    public int legacyRole() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ILegacyIAccessibleProvider::get_Role",
                Win32FfmBindings.invokeIlegacyIaccessibleProviderGetRolePointer(
                        functionAt(legacyAccessibleObject, 5),
                        legacyAccessibleObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ILegacyIAccessibleProvider::DoDefaultAction` through the generated COM vtable.
    ///
    /// @return the number of default-action invocations
    public int invokeLegacyDefaultAction() {
        requireOpen();
        requireSuccess(
                "ILegacyIAccessibleProvider::DoDefaultAction",
                Win32FfmBindings.invokeIlegacyIaccessibleProviderDoDefaultActionPointer(
                        functionAt(legacyAccessibleObject, 6),
                        legacyAccessibleObject
                )
        );
        return invokeCount;
    }

    /// Reads `ILegacyIAccessibleProvider::get_Value` through the generated COM vtable.
    ///
    /// @return the accessible value
    public String legacyValue() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ILegacyIAccessibleProvider::get_Value",
                Win32FfmBindings.invokeIlegacyIaccessibleProviderGetValuePointer(
                        functionAt(legacyAccessibleObject, 7),
                        legacyAccessibleObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Reads `ILegacyIAccessibleProvider::get_State` through the generated COM vtable.
    ///
    /// @return the MSAA state bits
    public int legacyState() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ILegacyIAccessibleProvider::get_State",
                Win32FfmBindings.invokeIlegacyIaccessibleProviderGetStatePointer(
                        functionAt(legacyAccessibleObject, 8),
                        legacyAccessibleObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Reads `ILegacyIAccessibleProvider::get_Description` through the generated COM vtable.
    ///
    /// @return the accessible description
    public String legacyDescription() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ILegacyIAccessibleProvider::get_Description",
                Win32FfmBindings.invokeIlegacyIaccessibleProviderGetDescriptionPointer(
                        functionAt(legacyAccessibleObject, 9),
                        legacyAccessibleObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Reads `ILegacyIAccessibleProvider::get_DefaultAction` through the generated COM vtable.
    ///
    /// @return the default-action name
    public String legacyDefaultAction() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ILegacyIAccessibleProvider::get_DefaultAction",
                Win32FfmBindings.invokeIlegacyIaccessibleProviderGetDefaultActionPointer(
                        functionAt(legacyAccessibleObject, 10),
                        legacyAccessibleObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Reads `ILegacyIAccessibleProvider::get_KeyboardShortcut` through the generated COM vtable.
    ///
    /// @return the keyboard shortcut, which first-stable leaves empty
    public String legacyKeyboardShortcut() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ILegacyIAccessibleProvider::get_KeyboardShortcut",
                Win32FfmBindings.invokeIlegacyIaccessibleProviderGetKeyboardShortcutPointer(
                        functionAt(legacyAccessibleObject, 11),
                        legacyAccessibleObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Reads `ILegacyIAccessibleProvider::get_Help` through the generated COM vtable.
    ///
    /// @return the accessible help string
    public String legacyHelp() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ILegacyIAccessibleProvider::get_Help",
                Win32FfmBindings.invokeIlegacyIaccessibleProviderGetHelpPointer(
                        functionAt(legacyAccessibleObject, 12),
                        legacyAccessibleObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Invokes `IRawElementProviderFragment::Navigate` through the generated COM vtable.
    ///
    /// @param direction a `NavigateDirection` value
    /// @return whether a fragment pointer was returned
    public boolean invokeFragmentNavigate(int direction) {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IRawElementProviderFragment::Navigate",
                Win32FfmBindings.invokeIrawElementProviderFragmentNavigatePointer(
                        functionAt(fragmentObject, 3),
                        fragmentObject,
                        direction,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `IRawElementProviderSimple::get_ProviderOptions` through the generated COM vtable.
    ///
    /// @return the provider-options bitfield
    public int invokeProviderOptions() {
        requireOpen();
        MemorySegment options = arena.allocate(ValueLayout.JAVA_INT);
        options.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IRawElementProviderSimple::get_ProviderOptions",
                Win32FfmBindings.invokeIrawElementProviderGetProviderOptionsPointer(
                        functionAt(simpleObject, 3),
                        simpleObject,
                        options
                )
        );
        return options.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `IRawElementProviderSimple::get_HostRawElementProvider` through the generated COM vtable.
    ///
    /// @return whether a host provider pointer was returned
    public boolean invokeHostRawElementProvider() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IRawElementProviderSimple::get_HostRawElementProvider",
                Win32FfmBindings.invokeIrawElementProviderGetHostRawElementProviderPointer(
                        functionAt(simpleObject, 7),
                        simpleObject,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `IRawElementProviderFragment::SetFocus` through the generated COM vtable.
    ///
    /// @return the invocation count after the call
    public int invokeFragmentSetFocus() {
        requireOpen();
        requireSuccess(
                "IRawElementProviderFragment::SetFocus",
                Win32FfmBindings.invokeIrawElementProviderFragmentSetFocusPointer(
                        functionAt(fragmentObject, 4),
                        fragmentObject
                )
        );
        return fragmentFocusCount;
    }

    /// Invokes `IRawElementProviderFragment::get_FragmentRoot` through the generated COM vtable.
    ///
    /// @return whether a fragment-root pointer was returned
    public boolean invokeFragmentRoot() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IRawElementProviderFragment::get_FragmentRoot",
                Win32FfmBindings.invokeIrawElementProviderFragmentGetFragmentRootPointer(
                        functionAt(fragmentObject, 5),
                        fragmentObject,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `IRawElementProviderFragment::get_BoundingRectangle` through the generated COM vtable.
    ///
    /// @return `{left, top, width, height}` in logical pixels
    public double[] invokeFragmentBoundingRectangle() {
        requireOpen();
        MemorySegment rect = arena.allocate(32);
        requireSuccess(
                "IRawElementProviderFragment::get_BoundingRectangle",
                Win32FfmBindings.invokeIrawElementProviderFragmentGetBoundingRectanglePointer(
                        functionAt(fragmentObject, 6),
                        fragmentObject,
                        rect
                )
        );
        return new double[] {
                rect.get(ValueLayout.JAVA_DOUBLE, 0L),
                rect.get(ValueLayout.JAVA_DOUBLE, 8L),
                rect.get(ValueLayout.JAVA_DOUBLE, 16L),
                rect.get(ValueLayout.JAVA_DOUBLE, 24L)
        };
    }

    /// Invokes `IRawElementProviderFragment::GetRuntimeId` through the generated COM vtable.
    ///
    /// The first-stable encoding is a count-prefixed `int32` vector rather than an oleaut32 `SAFEARRAY`.
    ///
    /// @return the packed `{count, id}` pair
    public int[] invokeFragmentRuntimeId() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IRawElementProviderFragment::GetRuntimeId",
                Win32FfmBindings.invokeIrawElementProviderFragmentGetRuntimeIdPointer(
                        functionAt(fragmentObject, 7),
                        fragmentObject,
                        result
                )
        );
        MemorySegment block = result.get(ValueLayout.ADDRESS, 0L);
        if (block.address() == 0L) {
            return new int[0];
        }
        block = block.byteSize() == 0L ? block.reinterpret(8) : block;
        int count = block.get(ValueLayout.JAVA_INT, 0L);
        if (count < 1) {
            return new int[] {count};
        }
        return new int[] {count, block.get(ValueLayout.JAVA_INT, 4L)};
    }

    /// Invokes `IRawElementProviderFragment::GetEmbeddedFragmentRoots` through the generated COM vtable.
    ///
    /// @return the packed root count
    public int invokeEmbeddedFragmentRoots() {
        return packedCount("IRawElementProviderFragment::GetEmbeddedFragmentRoots", fragmentObject, 8);
    }

    /// Invokes `IRawElementProviderFragmentRoot::ElementProviderFromPoint` through the generated COM vtable.
    ///
    /// @param x the horizontal point
    /// @param y the vertical point
    /// @return whether a fragment pointer was returned
    public boolean invokeFragmentRootFromPoint(double x, double y) {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IRawElementProviderFragmentRoot::ElementProviderFromPoint",
                Win32FfmBindings.invokeIrawElementProviderFragmentRootElementProviderFromPointPointer(
                        functionAt(fragmentRootObject, 3),
                        fragmentRootObject,
                        x,
                        y,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `IRawElementProviderFragmentRoot::GetFocus` through the generated COM vtable.
    ///
    /// @return whether a focused fragment pointer was returned
    public boolean invokeFragmentRootFocus() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "IRawElementProviderFragmentRoot::GetFocus",
                Win32FfmBindings.invokeIrawElementProviderFragmentRootGetFocusPointer(
                        functionAt(fragmentRootObject, 4),
                        fragmentRootObject,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `ILegacyIAccessibleProvider::SetValue` through the generated COM vtable.
    ///
    /// @param value the accessible value
    /// @return the invocation count after the call
    public int invokeLegacySetValue(String value) {
        requireOpen();
        MemorySegment chars = arena.allocate((value.length() + 1L) * 2L);
        byte[] utf16 = value.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment.copy(utf16, 0, chars, ValueLayout.JAVA_BYTE, 0L, utf16.length);
        requireSuccess(
                "ILegacyIAccessibleProvider::SetValue",
                Win32FfmBindings.invokeIlegacyIaccessibleProviderSetValuePointer(
                        functionAt(legacyAccessibleObject, 13),
                        legacyAccessibleObject,
                        chars
                )
        );
        return legacySetValueCount;
    }

    /// Returns the last `SetValue` payload.
    ///
    /// @return the stored value
    public String lastLegacyValue() {
        return lastLegacyValue;
    }

    /// Invokes `ITextProvider2::GetCaretRange` through the generated COM vtable.
    ///
    /// @return whether an active caret range pointer was returned
    public boolean invokeCaretRange() {
        requireOpen();
        MemorySegment active = arena.allocate(ValueLayout.JAVA_INT);
        active.set(ValueLayout.JAVA_INT, 0L, 0);
        MemorySegment range = arena.allocate(ValueLayout.ADDRESS);
        range.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextProvider2::GetCaretRange",
                Win32FfmBindings.invokeItextProvider2GetCaretRangePointer(
                        functionAt(text2Object, 3),
                        text2Object,
                        active,
                        range
                )
        );
        return active.get(ValueLayout.JAVA_INT, 0L) != 0 && range.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `ISpreadsheetItemProvider::get_Formula` through the generated COM vtable.
    ///
    /// @return the formula string
    public String spreadsheetFormula() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ISpreadsheetItemProvider::get_Formula",
                Win32FfmBindings.invokeIspreadsheetItemProviderGetFormulaPointer(
                        functionAt(spreadsheetItemObject, 3),
                        spreadsheetItemObject,
                        result
                )
        );
        return decodeUtf16(result.get(ValueLayout.ADDRESS, 0L));
    }

    /// Reads `ISelectionProvider2::get_ItemCount` through the generated COM vtable.
    ///
    /// @return the advertised item count
    public int selectionItemCount() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ISelectionProvider2::get_ItemCount",
                Win32FfmBindings.invokeIselectionProvider2GetItemCountPointer(
                        functionAt(selection2Object, 3),
                        selection2Object,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Reads `ISelectionProvider2::get_CurrentSelectedItem` through the generated COM vtable.
    ///
    /// @return whether a selected-item pointer was returned
    public boolean invokeCurrentSelectedItem() {
        requireOpen();
        MemorySegment item = arena.allocate(ValueLayout.ADDRESS);
        item.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ISelectionProvider2::get_CurrentSelectedItem",
                Win32FfmBindings.invokeIselectionProvider2GetCurrentSelectedItemPointer(
                        functionAt(selection2Object, 4),
                        selection2Object,
                        item
                )
        );
        return item.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `ISelectionProvider2::get_FirstSelectedItem` through the generated COM vtable.
    ///
    /// @return whether a selected-item pointer was returned
    public boolean invokeFirstSelectedItem() {
        requireOpen();
        MemorySegment item = arena.allocate(ValueLayout.ADDRESS);
        item.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ISelectionProvider2::get_FirstSelectedItem",
                Win32FfmBindings.invokeIselectionProvider2GetFirstSelectedItemPointer(
                        functionAt(selection2Object, 5),
                        selection2Object,
                        item
                )
        );
        return item.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `ISelectionProvider2::get_LastSelectedItem` through the generated COM vtable.
    ///
    /// @return whether a selected-item pointer was returned
    public boolean invokeLastSelectedItem() {
        requireOpen();
        MemorySegment item = arena.allocate(ValueLayout.ADDRESS);
        item.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ISelectionProvider2::get_LastSelectedItem",
                Win32FfmBindings.invokeIselectionProvider2GetLastSelectedItemPointer(
                        functionAt(selection2Object, 6),
                        selection2Object,
                        item
                )
        );
        return item.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `ITextProvider2::RangeFromAnnotation` through the generated COM vtable.
    ///
    /// @return whether a text-range pointer was returned
    public boolean invokeRangeFromAnnotation() {
        requireOpen();
        MemorySegment range = arena.allocate(ValueLayout.ADDRESS);
        range.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextProvider2::RangeFromAnnotation",
                Win32FfmBindings.invokeItextProvider2RangeFromAnnotationPointer(
                        functionAt(text2Object, 4),
                        text2Object,
                        MemorySegment.NULL,
                        range
                )
        );
        return range.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `ITransformProvider2::Zoom` through the generated COM vtable.
    ///
    /// @param zoom the requested zoom level
    /// @return the stored zoom level
    public double zoomTransform(double zoom) {
        requireOpen();
        requireSuccess(
                "ITransformProvider2::Zoom",
                Win32FfmBindings.invokeItransformProvider2ZoomPointer(
                        functionAt(transform2Object, 3),
                        transform2Object,
                        zoom
                )
        );
        return transformZoom;
    }

    /// Reads `ITransformProvider2::get_CanZoom` through the generated COM vtable.
    ///
    /// @return whether zoom is advertised
    public boolean canZoom() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITransformProvider2::get_CanZoom",
                Win32FfmBindings.invokeItransformProvider2GetCanZoomPointer(
                        functionAt(transform2Object, 4),
                        transform2Object,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Reads `ITransformProvider2::get_ZoomLevel` through the generated COM vtable.
    ///
    /// @return the stored zoom level
    public double zoomLevel() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_DOUBLE);
        state.set(ValueLayout.JAVA_DOUBLE, 0L, 0.0);
        requireSuccess(
                "ITransformProvider2::get_ZoomLevel",
                Win32FfmBindings.invokeItransformProvider2GetZoomLevelPointer(
                        functionAt(transform2Object, 5),
                        transform2Object,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_DOUBLE, 0L);
    }

    /// Invokes `ITransformProvider2::ZoomByUnit` through the generated COM vtable.
    ///
    /// @param zoomUnit a `ZoomUnit` identifier
    /// @return the stored zoom level
    public double zoomByUnit(int zoomUnit) {
        requireOpen();
        requireSuccess(
                "ITransformProvider2::ZoomByUnit",
                Win32FfmBindings.invokeItransformProvider2ZoomByUnitPointer(
                        functionAt(transform2Object, 6),
                        transform2Object,
                        zoomUnit
                )
        );
        return transformZoom;
    }

    /// Reads `ITransformProvider2::get_ZoomMinimum` through the generated COM vtable.
    ///
    /// @return the advertised minimum zoom
    public double zoomMinimum() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_DOUBLE);
        state.set(ValueLayout.JAVA_DOUBLE, 0L, 0.0);
        requireSuccess(
                "ITransformProvider2::get_ZoomMinimum",
                Win32FfmBindings.invokeItransformProvider2GetZoomMinimumPointer(
                        functionAt(transform2Object, 7),
                        transform2Object,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_DOUBLE, 0L);
    }

    /// Reads `ITransformProvider2::get_ZoomMaximum` through the generated COM vtable.
    ///
    /// @return the advertised maximum zoom
    public double zoomMaximum() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_DOUBLE);
        state.set(ValueLayout.JAVA_DOUBLE, 0L, 0.0);
        requireSuccess(
                "ITransformProvider2::get_ZoomMaximum",
                Win32FfmBindings.invokeItransformProvider2GetZoomMaximumPointer(
                        functionAt(transform2Object, 8),
                        transform2Object,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_DOUBLE, 0L);
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

    /// Invokes `IWindowProvider::WaitForInputIdle` through the generated COM vtable.
    ///
    /// @param milliseconds the idle timeout
    /// @return whether the provider reported idle success
    public boolean waitForInputIdle(int milliseconds) {
        requireOpen();
        MemorySegment success = arena.allocate(ValueLayout.JAVA_INT);
        success.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IWindowProvider::WaitForInputIdle",
                Win32FfmBindings.invokeIwindowProviderWaitForInputIdlePointer(
                        functionAt(windowObject, 5),
                        windowObject,
                        milliseconds,
                        success
                )
        );
        return success.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Reads `IWindowProvider::get_CanMinimize` through the generated COM vtable.
    ///
    /// @return whether minimize is advertised
    public boolean canMinimize() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IWindowProvider::get_CanMinimize",
                Win32FfmBindings.invokeIwindowProviderGetCanMinimizePointer(
                        functionAt(windowObject, 7),
                        windowObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Reads `IWindowProvider::get_IsModal` through the generated COM vtable.
    ///
    /// @return whether the window is advertised as modal
    public boolean isModal() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IWindowProvider::get_IsModal",
                Win32FfmBindings.invokeIwindowProviderGetIsModalPointer(
                        functionAt(windowObject, 8),
                        windowObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Reads `IWindowProvider::get_WindowInteractionState` through the generated COM vtable.
    ///
    /// @return the interaction state
    public int windowInteractionState() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IWindowProvider::get_WindowInteractionState",
                Win32FfmBindings.invokeIwindowProviderGetWindowInteractionStatePointer(
                        functionAt(windowObject, 10),
                        windowObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Reads `IWindowProvider::get_IsTopmost` through the generated COM vtable.
    ///
    /// @return whether the window is advertised as topmost
    public boolean isTopmost() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IWindowProvider::get_IsTopmost",
                Win32FfmBindings.invokeIwindowProviderGetIsTopmostPointer(
                        functionAt(windowObject, 11),
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

    /// Reads `ITableItemProvider::GetColumnHeaderItems` for this node.
    ///
    /// @return the header count
    public int invokeColumnHeaderItems() {
        return packedCount("ITableItemProvider::GetColumnHeaderItems", tableItemObject, 4);
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

    /// Reads `Name` from the header returned by `GetColumnHeaders` at `index`.
    ///
    /// @param index the zero-based header index
    /// @return the accessible name
    public String invokeColumnHeaderName(int index) {
        return invokeHeaderString(columnHeaderObjects, index, UIA_NAME_PROPERTY_ID);
    }

    /// Reads `ControlType` from the header returned by `GetColumnHeaders` at `index`.
    ///
    /// @param index the zero-based header index
    /// @return the UIA control-type identifier
    public int invokeColumnHeaderControlType(int index) {
        return invokeHeaderInt(columnHeaderObjects, index, UIA_CONTROL_TYPE_PROPERTY_ID);
    }

    /// Reads `Name` from the header returned by `GetRowHeaders` at `index`.
    ///
    /// @param index the zero-based header index
    /// @return the accessible name
    public String invokeRowHeaderName(int index) {
        return invokeHeaderString(rowHeaderObjects, index, UIA_NAME_PROPERTY_ID);
    }

    /// Reads `ControlType` from the header returned by `GetRowHeaders` at `index`.
    ///
    /// @param index the zero-based header index
    /// @return the UIA control-type identifier
    public int invokeRowHeaderControlType(int index) {
        return invokeHeaderInt(rowHeaderObjects, index, UIA_CONTROL_TYPE_PROPERTY_ID);
    }

    /// Reads `Name` from this cell's column-header item.
    ///
    /// @return the accessible name
    public String invokeColumnHeaderItemName() {
        return invokeHeaderString(new MemorySegment[] {cellColumnHeaderObject}, 0, UIA_NAME_PROPERTY_ID);
    }

    /// Invokes `GetPropertyValue` on a header provider as `VT_BSTR`.
    private String invokeHeaderString(MemorySegment[] headers, int index, int propertyId) {
        MemorySegment value = invokeHeaderVariant(headers, index, propertyId);
        if (value.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET) != VT_BSTR) {
            return "";
        }
        return decodeUtf16(value.get(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET));
    }

    /// Invokes `GetPropertyValue` on a header provider as `VT_I4`.
    private int invokeHeaderInt(MemorySegment[] headers, int index, int propertyId) {
        MemorySegment value = invokeHeaderVariant(headers, index, propertyId);
        if (value.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET) != VT_I4) {
            return 0;
        }
        return value.get(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET);
    }

    /// Invokes generated `IRawElementProviderSimple::GetPropertyValue` on `headers[index]`.
    private MemorySegment invokeHeaderVariant(MemorySegment[] headers, int index, int propertyId) {
        requireOpen();
        if (index < 0 || index >= headers.length || headers[index].address() == 0L) {
            throw new IllegalArgumentException("Header index is out of range");
        }
        MemorySegment header = headers[index];
        MemorySegment vtable = header.get(ValueLayout.ADDRESS, 0L);
        if (vtable.byteSize() == 0L) {
            vtable = vtable.reinterpret(7 * ValueLayout.ADDRESS.byteSize());
        }
        MemorySegment getProperty = vtable.getAtIndex(ValueLayout.ADDRESS, 5L);
        MemorySegment value = arena.allocate(Win32Layouts.VARIANT);
        value.fill((byte) 0);
        requireSuccess(
                "header GetPropertyValue",
                Win32FfmBindings.invokeIrawElementProviderGetPropertyValuePointer(
                        getProperty,
                        header,
                        propertyId,
                        value
                )
        );
        return value;
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

    /// Invokes `ITextProvider::RangeFromPoint` through the generated COM vtable.
    ///
    /// @param x the horizontal point
    /// @param y the vertical point
    /// @return whether a range was returned
    public boolean invokeRangeFromPoint(double x, double y) {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextProvider::RangeFromPoint",
                Win32FfmBindings.invokeItextProviderRangeFromPointPointer(
                        functionAt(textObject, 3),
                        textObject,
                        x,
                        y,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `ITextProvider::RangeFromChild` through the generated COM vtable.
    ///
    /// @return whether a child range was returned
    public boolean invokeRangeFromChild() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextProvider::RangeFromChild",
                Win32FfmBindings.invokeItextProviderGetRangePointer(
                        functionAt(textObject, 6),
                        textObject,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `ITextProvider::GetVisibleRanges` through the generated COM vtable.
    ///
    /// The official oleaut32 `SAFEARRAY(ITextRangeProvider*)` is decoded with generated
    /// `SafeArrayGetUBound` / `SafeArrayGetElement` / `SafeArrayDestroy`.
    ///
    /// @return whether a visible range was returned
    public boolean invokeGetVisibleRanges() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextProvider::GetVisibleRanges",
                Win32FfmBindings.invokeItextProviderGetRangePointer(
                        functionAt(textObject, 5),
                        textObject,
                        result
                )
        );
        MemorySegment array = result.get(ValueLayout.ADDRESS, 0L);
        if (array.address() == 0L) {
            return false;
        }
        MemorySegment bound = arena.allocate(ValueLayout.JAVA_INT);
        int boundResult = bindings.safeArrayGetUBound(array, 1, bound);
        if (boundResult < 0 || bound.get(ValueLayout.JAVA_INT, 0L) < 0) {
            bindings.safeArrayDestroy(array);
            return false;
        }
        MemorySegment indices = arena.allocate(ValueLayout.JAVA_INT);
        indices.set(ValueLayout.JAVA_INT, 0L, 0);
        MemorySegment element = arena.allocate(ValueLayout.ADDRESS);
        element.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        int getResult = bindings.safeArrayGetElement(array, indices, element);
        MemorySegment range = element.get(ValueLayout.ADDRESS, 0L);
        if (range.address() != 0L) {
            MemorySegment vtable = range.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0L);
            MemorySegment release = vtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 3L)
                    .getAtIndex(ValueLayout.ADDRESS, 2L);
            Win32FfmBindings.invokeIunknownReleasePointer(release, range);
        }
        bindings.safeArrayDestroy(array);
        return getResult >= 0 && range.address() != 0L;
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

    /// Reads `ITextRangeProvider::GetAttributeValue` as a `VT_I4` payload.
    ///
    /// @param attributeId a `TextAttributeId`
    /// @return the integer payload, or `0` when the variant is not `VT_I4`
    public int invokeGetAttributeValueInt(int attributeId) {
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
        if (variant.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET) != VT_I4) {
            return 0;
        }
        return variant.get(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET);
    }

    /// Reads `ITextRangeProvider::GetAttributeValue` as a `VT_BSTR` payload.
    ///
    /// @param attributeId a `TextAttributeId`
    /// @return the string payload, or `""` when the variant is not `VT_BSTR`
    public String invokeGetAttributeValueString(int attributeId) {
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
        if (variant.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET) != VT_BSTR) {
            return "";
        }
        return decodeUtf16(variant.get(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET));
    }

    /// Reads `ITextRangeProvider::GetAttributeValue` as a `VT_R8` payload.
    ///
    /// @param attributeId a `TextAttributeId`
    /// @return the double payload, or `0.0` when the variant is not `VT_R8`
    public double invokeGetAttributeValueDouble(int attributeId) {
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
        if (variant.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET) != VT_R8) {
            return 0.0;
        }
        return variant.get(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET);
    }

    /// Reads `ITextRangeProvider::GetAttributeValue` as a `VT_UNKNOWN` pointer.
    ///
    /// @param attributeId a `TextAttributeId`
    /// @return whether a non-null interface pointer was returned
    public boolean invokeGetAttributeValueUnknown(int attributeId) {
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
        if (variant.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET) != VT_UNKNOWN) {
            return false;
        }
        return variant.get(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET).address() != 0L;
    }

    /// Reads `GetPropertyValue` as a packed count-prefixed `int32` vector.
    ///
    /// @param propertyId the UIA property identifier
    /// @return `{count, first}` when present
    public int[] invokePropertyValueInts(int propertyId) {
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
        MemorySegment block = value.get(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET);
        if (block.address() == 0L) {
            return new int[0];
        }
        block = block.byteSize() == 0L ? block.reinterpret(8) : block;
        return new int[] {block.get(ValueLayout.JAVA_INT, 0L), block.get(ValueLayout.JAVA_INT, 4L)};
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

    /// Invokes `ITextRangeProvider2::ShowContextMenu` through the generated COM vtable.
    ///
    /// @return the invocation count after the call
    public int invokeShowContextMenu() {
        requireOpen();
        requireSuccess(
                "ITextRangeProvider2::ShowContextMenu",
                Win32FfmBindings.invokeItextRangeProvider2ShowContextMenuPointer(
                        functionAt(textRangeObject, 21),
                        textRangeObject
                )
        );
        return textRangeContextMenuCount;
    }

    /// Invokes `IRawElementProviderSimple2::ShowContextMenu` through the generated COM vtable.
    ///
    /// @return the invocation count after the call
    public int invokeSimpleShowContextMenu() {
        requireOpen();
        requireSuccess(
                "IRawElementProviderSimple2::ShowContextMenu",
                Win32FfmBindings.invokeIrawElementProviderSimple2ShowContextMenuPointer(
                        functionAt(simpleObject, 6),
                        simpleObject
                )
        );
        return simpleContextMenuCount;
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
        if (liveNode != null) {
            liveNode.removeLabelListener(liveLabelListener);
        }
        release(simpleObject);
        arena.close();
    }

    /// Implements `IRawElementProviderSimple::QueryInterface`.
    private int queryInterface(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        if (WindowsCom.matches(interfaceId, IRAW_ELEMENT_PROVIDER_SIMPLE2)) {
            return query(interfaceId, result, IRAW_ELEMENT_PROVIDER_SIMPLE2, simpleObject);
        }
        if (WindowsCom.matches(interfaceId, IRAW_ELEMENT_PROVIDER_FRAGMENT)) {
            return query(interfaceId, result, IRAW_ELEMENT_PROVIDER_FRAGMENT, fragmentObject);
        }
        if (WindowsCom.matches(interfaceId, IRAW_ELEMENT_PROVIDER_FRAGMENT_ROOT)) {
            return query(interfaceId, result, IRAW_ELEMENT_PROVIDER_FRAGMENT_ROOT, fragmentRootObject);
        }
        return query(interfaceId, result, IRAW_ELEMENT_PROVIDER_SIMPLE, simpleObject);
    }

    /// Implements Fragment QI.
    private int queryFragment(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IRAW_ELEMENT_PROVIDER_FRAGMENT, fragmentObject);
    }

    /// Implements FragmentRoot QI.
    private int queryFragmentRoot(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IRAW_ELEMENT_PROVIDER_FRAGMENT_ROOT, fragmentRootObject);
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

    /// Implements VirtualizedItem QI.
    private int queryVirtualizedItem(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IVIRTUALIZED_ITEM_PROVIDER, virtualizedItemObject);
    }

    /// Implements Dock QI.
    private int queryDock(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IDOCK_PROVIDER, dockObject);
    }

    /// Implements Transform QI.
    private int queryTransform(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITRANSFORM_PROVIDER, transformObject);
    }

    /// Implements ItemContainer QI.
    private int queryItemContainer(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IITEM_CONTAINER_PROVIDER, itemContainerObject);
    }

    /// Implements SynchronizedInput QI.
    private int querySynchronizedInput(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ISYNCHRONIZED_INPUT_PROVIDER, synchronizedInputObject);
    }

    /// Implements MultipleView QI.
    private int queryMultipleView(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IMULTIPLE_VIEW_PROVIDER, multipleViewObject);
    }

    /// Implements DropTarget QI.
    private int queryDropTarget(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IDROP_TARGET_PROVIDER, dropTargetObject);
    }

    /// Implements Drag QI.
    private int queryDrag(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IDRAG_PROVIDER, dragObject);
    }

    /// Implements Annotation QI.
    private int queryAnnotation(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IANNOTATION_PROVIDER, annotationObject);
    }

    /// Implements TextChild QI.
    private int queryTextChild(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITEXT_CHILD_PROVIDER, textChildObject);
    }

    /// Implements Styles QI.
    private int queryStyles(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ISTYLES_PROVIDER, stylesObject);
    }

    /// Implements Spreadsheet QI.
    private int querySpreadsheet(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ISPREADSHEET_PROVIDER, spreadsheetObject);
    }

    /// Implements CustomNavigation QI.
    private int queryCustomNavigation(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ICUSTOM_NAVIGATION_PROVIDER, customNavigationObject);
    }

    /// Implements ObjectModel QI.
    private int queryObjectModel(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IOBJECT_MODEL_PROVIDER, objectModelObject);
    }

    /// Implements TextEdit QI.
    private int queryTextEdit(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITEXT_EDIT_PROVIDER, textEditObject);
    }

    /// Implements Selection-container QI.
    private int querySelectionContainer(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ISELECTION_PROVIDER, selectionContainerObject);
    }

    /// Implements LegacyIAccessible QI.
    private int queryLegacyAccessible(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ILEGACY_IACCESSIBLE_PROVIDER, legacyAccessibleObject);
    }

    /// Implements TextProvider2 QI.
    private int queryText2(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITEXT_PROVIDER2, text2Object);
    }

    /// Implements SpreadsheetItem QI.
    private int querySpreadsheetItem(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ISPREADSHEET_ITEM_PROVIDER, spreadsheetItemObject);
    }

    /// Implements SelectionProvider2 QI.
    private int querySelection2(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ISELECTION_PROVIDER2, selection2Object);
    }

    /// Implements TransformProvider2 QI.
    private int queryTransform2(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITRANSFORM_PROVIDER2, transform2Object);
    }

    /// Implements found-item `IRawElementProviderSimple` QI.
    private int queryFoundItem(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IRAW_ELEMENT_PROVIDER_SIMPLE, foundItemObject);
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
        if (WindowsCom.matches(interfaceId, ITEXT_RANGE_PROVIDER2)) {
            return query(interfaceId, result, ITEXT_RANGE_PROVIDER2, textRangeObject);
        }
        return query(interfaceId, result, ITEXT_RANGE_PROVIDER, textRangeObject);
    }

    /// Shared QI implementation for one identity.
    private int query(MemorySegment interfaceId, MemorySegment result, UUID identity, MemorySegment object) {
        if (result.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment out = result.byteSize() >= ValueLayout.ADDRESS.byteSize()
                ? result
                : result.reinterpret(ValueLayout.ADDRESS.byteSize());
        if (WindowsCom.matches(interfaceId, IUNKNOWN) || WindowsCom.matches(interfaceId, identity)) {
            out.set(ValueLayout.ADDRESS, 0L, object);
            addRef(object);
            return S_OK;
        }
        out.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
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

    /// Returns whether [`#getPatternProvider`] would expose `patternId`.
    ///
    /// @param patternId the UIA pattern identifier
    /// @return `true` when this snapshot exposes the pattern
    private boolean patternAvailable(int patternId) {
        if (patternId == UIA_INVOKE_PATTERN_ID) {
            return node.actions().contains(SemanticsAction.ACTIVATE);
        }
        if (patternId == UIA_VALUE_PATTERN_ID) {
            return node.role() == SemanticsRole.TEXT_FIELD || node.role() == SemanticsRole.TEXT_AREA;
        }
        if (patternId == UIA_RANGE_VALUE_PATTERN_ID) {
            return node.rangeValue() != null;
        }
        if (patternId == UIA_TOGGLE_PATTERN_ID) {
            return node.role() == SemanticsRole.TOGGLE;
        }
        if (patternId == UIA_SCROLL_PATTERN_ID) {
            return node.scroll() != null;
        }
        if (patternId == UIA_WINDOW_PATTERN_ID) {
            return node.role() == SemanticsRole.DIALOG;
        }
        if (patternId == UIA_EXPAND_COLLAPSE_PATTERN_ID) {
            return expandable(node);
        }
        if (patternId == UIA_SELECTION_ITEM_PATTERN_ID) {
            return selectableItem(node);
        }
        if (patternId == UIA_GRID_PATTERN_ID) {
            return node.grid() != null;
        }
        if (patternId == UIA_GRID_ITEM_PATTERN_ID) {
            return node.gridItem() != null;
        }
        if (patternId == UIA_TABLE_PATTERN_ID) {
            return node.grid() != null;
        }
        if (patternId == UIA_SCROLL_ITEM_PATTERN_ID) {
            return node.actions().contains(SemanticsAction.SCROLL_INTO_VIEW);
        }
        if (patternId == UIA_MULTIPLE_VIEW_PATTERN_ID) {
            return node.role() == SemanticsRole.LIST;
        }
        if (patternId == UIA_SELECTION_PATTERN_ID) {
            return node.role() == SemanticsRole.LIST;
        }
        if (patternId == UIA_TABLE_ITEM_PATTERN_ID) {
            return node.gridItem() != null;
        }
        if (patternId == UIA_TEXT_PATTERN_ID) {
            return node.textRange() != null;
        }
        if (patternId == UIA_TRANSFORM_PATTERN_ID) {
            return node.role() == SemanticsRole.DIALOG;
        }
        if (patternId == UIA_LEGACY_IACCESSIBLE_PATTERN_ID) {
            return true;
        }
        if (patternId == UIA_ITEM_CONTAINER_PATTERN_ID) {
            return node.role() == SemanticsRole.LIST;
        }
        if (patternId == UIA_VIRTUALIZED_ITEM_PATTERN_ID) {
            return node.actions().contains(SemanticsAction.REALIZE);
        }
        if (patternId == UIA_TEXT_PATTERN2_ID) {
            return node.textRange() != null;
        }
        if (patternId == UIA_SYNCHRONIZED_INPUT_PATTERN_ID) {
            return node.actions().contains(SemanticsAction.ACTIVATE);
        }
        if (patternId == UIA_OBJECT_MODEL_PATTERN_ID) {
            return node.role() == SemanticsRole.DIALOG;
        }
        if (patternId == UIA_ANNOTATION_PATTERN_ID) {
            return node.role() == SemanticsRole.STATUS;
        }
        if (patternId == UIA_STYLES_PATTERN_ID) {
            return node.role() == SemanticsRole.STATUS;
        }
        if (patternId == UIA_SPREADSHEET_PATTERN_ID) {
            return node.grid() != null;
        }
        if (patternId == UIA_SPREADSHEET_ITEM_PATTERN_ID) {
            return node.gridItem() != null;
        }
        if (patternId == UIA_TRANSFORM_PATTERN2_ID) {
            return node.role() == SemanticsRole.DIALOG;
        }
        if (patternId == UIA_TEXT_CHILD_PATTERN_ID) {
            return node.textRange() != null;
        }
        if (patternId == UIA_DRAG_PATTERN_ID) {
            return node.role() == SemanticsRole.LIST;
        }
        if (patternId == UIA_DROP_TARGET_PATTERN_ID) {
            return node.role() == SemanticsRole.LIST;
        }
        if (patternId == UIA_TEXT_EDIT_PATTERN_ID) {
            return node.textRange() != null;
        }
        if (patternId == UIA_CUSTOM_NAVIGATION_PATTERN_ID) {
            return node.role() == SemanticsRole.DIALOG;
        }
        if (patternId == UIA_DOCK_PATTERN_ID) {
            return node.role() == SemanticsRole.DIALOG;
        }
        if (patternId == UIA_SELECTION_PATTERN2_ID) {
            return node.role() == SemanticsRole.LIST;
        }
        return false;
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
        } else if (patternId == UIA_VIRTUALIZED_ITEM_PATTERN_ID
                && node.actions().contains(SemanticsAction.REALIZE)) {
            selected = virtualizedItemObject;
        } else if (patternId == UIA_DOCK_PATTERN_ID && node.role() == SemanticsRole.DIALOG) {
            selected = dockObject;
        } else if (patternId == UIA_TRANSFORM_PATTERN_ID && node.role() == SemanticsRole.DIALOG) {
            selected = transformObject;
        } else if (patternId == UIA_TRANSFORM_PATTERN2_ID && node.role() == SemanticsRole.DIALOG) {
            selected = transform2Object;
        } else if (patternId == UIA_ITEM_CONTAINER_PATTERN_ID && node.role() == SemanticsRole.LIST) {
            selected = itemContainerObject;
        } else if (patternId == UIA_MULTIPLE_VIEW_PATTERN_ID && node.role() == SemanticsRole.LIST) {
            selected = multipleViewObject;
        } else if (patternId == UIA_DROP_TARGET_PATTERN_ID && node.role() == SemanticsRole.LIST) {
            selected = dropTargetObject;
        } else if (patternId == UIA_DRAG_PATTERN_ID && node.role() == SemanticsRole.LIST) {
            selected = dragObject;
        } else if (patternId == UIA_ANNOTATION_PATTERN_ID && node.role() == SemanticsRole.STATUS) {
            selected = annotationObject;
        } else if (patternId == UIA_TEXT_CHILD_PATTERN_ID && node.textRange() != null) {
            selected = textChildObject;
        } else if (patternId == UIA_STYLES_PATTERN_ID && node.role() == SemanticsRole.STATUS) {
            selected = stylesObject;
        } else if (patternId == UIA_SPREADSHEET_PATTERN_ID && node.grid() != null) {
            selected = spreadsheetObject;
        } else if (patternId == UIA_CUSTOM_NAVIGATION_PATTERN_ID && node.role() == SemanticsRole.DIALOG) {
            selected = customNavigationObject;
        } else if (patternId == UIA_OBJECT_MODEL_PATTERN_ID && node.role() == SemanticsRole.DIALOG) {
            selected = objectModelObject;
        } else if (patternId == UIA_TEXT_EDIT_PATTERN_ID && node.textRange() != null) {
            selected = textEditObject;
        } else if (patternId == UIA_SELECTION_PATTERN_ID && node.role() == SemanticsRole.LIST) {
            selected = selectionContainerObject;
        } else if (patternId == UIA_LEGACY_IACCESSIBLE_PATTERN_ID) {
            selected = legacyAccessibleObject;
        } else if (patternId == UIA_TEXT_PATTERN2_ID && node.textRange() != null) {
            selected = text2Object;
        } else if (patternId == UIA_SPREADSHEET_ITEM_PATTERN_ID && node.gridItem() != null) {
            selected = spreadsheetItemObject;
        } else if (patternId == UIA_SELECTION_PATTERN2_ID && node.role() == SemanticsRole.LIST) {
            selected = selection2Object;
        } else if (patternId == UIA_SYNCHRONIZED_INPUT_PATTERN_ID
                && node.actions().contains(SemanticsAction.ACTIVATE)) {
            selected = synchronizedInputObject;
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
        if (propertyId == UIA_BOUNDING_RECTANGLE_PROPERTY_ID) {
            writeBoundingRectangle(variant, node.bounds());
        } else if (propertyId == UIA_PROCESS_ID_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    (int) ProcessHandle.current().pid()
            );
        } else if (propertyId == UIA_CONTROL_TYPE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, controlTypeId(node));
        } else if (propertyId == UIA_LOCALIZED_CONTROL_TYPE_PROPERTY_ID) {
            writeBstrVariant(variant, node.role().name());
        } else if (propertyId == UIA_NAME_PROPERTY_ID) {
            writeBstrVariant(variant, node.label());
        } else if (propertyId == UIA_ACCELERATOR_KEY_PROPERTY_ID) {
            writeBstrVariant(variant, node.acceleratorKey());
        } else if (propertyId == UIA_ACCESS_KEY_PROPERTY_ID) {
            writeBstrVariant(variant, node.accessKey());
        } else if (propertyId == UIA_VALUE_VALUE_PROPERTY_ID) {
            writeBstrVariant(variant, node.label());
        } else if (propertyId == UIA_CLASS_NAME_PROPERTY_ID) {
            writeBstrVariant(variant, node.role().name());
        } else if (propertyId == UIA_HAS_KEYBOARD_FOCUS_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.focused() ? 1 : 0);
        } else if (propertyId == UIA_IS_KEYBOARD_FOCUSABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    node.focusable() && !node.disabled() ? 1 : 0
            );
        } else if (propertyId == UIA_AUTOMATION_ID_PROPERTY_ID) {
            writeBstrVariant(variant, liveNode != null ? liveNode.name() : node.label());
        } else if (propertyId == UIA_HELP_TEXT_PROPERTY_ID) {
            writeBstrVariant(variant, node.hint());
        } else if (propertyId == UIA_IS_ENABLED_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.disabled() ? 0 : 1);
        } else if (propertyId == UIA_IS_READ_ONLY_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.readOnly() ? 1 : 0);
        } else if (propertyId == UIA_VALUE_IS_READ_ONLY_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.readOnly() ? 1 : 0);
        } else if (propertyId == UIA_IS_INVOKE_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_INVOKE_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_VALUE_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_VALUE_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_RANGE_VALUE_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_RANGE_VALUE_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_TOGGLE_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TOGGLE_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_SCROLL_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_SCROLL_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_WINDOW_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_WINDOW_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_EXPAND_COLLAPSE_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_EXPAND_COLLAPSE_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_SELECTION_ITEM_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_SELECTION_ITEM_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_GRID_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_GRID_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_GRID_ITEM_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_GRID_ITEM_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_TABLE_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TABLE_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_SCROLL_ITEM_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_SCROLL_ITEM_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_MULTIPLE_VIEW_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_MULTIPLE_VIEW_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_SELECTION_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_SELECTION_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_TABLE_ITEM_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TABLE_ITEM_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_TEXT_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TEXT_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_TRANSFORM_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TRANSFORM_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_LEGACY_IACCESSIBLE_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_LEGACY_IACCESSIBLE_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_ITEM_CONTAINER_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_ITEM_CONTAINER_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_VIRTUALIZED_ITEM_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_VIRTUALIZED_ITEM_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_TEXT_PATTERN2_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TEXT_PATTERN2_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_SYNCHRONIZED_INPUT_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_SYNCHRONIZED_INPUT_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_OBJECT_MODEL_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_OBJECT_MODEL_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_ANNOTATION_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_ANNOTATION_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_STYLES_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_STYLES_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_SPREADSHEET_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_SPREADSHEET_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_SPREADSHEET_ITEM_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_SPREADSHEET_ITEM_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_TRANSFORM_PATTERN2_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TRANSFORM_PATTERN2_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_TEXT_CHILD_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TEXT_CHILD_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_DRAG_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_DRAG_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_DROP_TARGET_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_DROP_TARGET_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_TEXT_EDIT_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TEXT_EDIT_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_CUSTOM_NAVIGATION_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_CUSTOM_NAVIGATION_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_DOCK_PATTERN_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_DOCK_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_IS_SELECTION_PATTERN2_AVAILABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_SELECTION_PATTERN2_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_DOCK_DOCK_POSITION_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, dockPosition);
        } else if (propertyId == UIA_TRANSFORM_CAN_MOVE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TRANSFORM_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_TRANSFORM_CAN_RESIZE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TRANSFORM_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_TRANSFORM_CAN_ROTATE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TRANSFORM_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_EXPAND_COLLAPSE_EXPAND_COLLAPSE_STATE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, expandState);
        } else if (propertyId == UIA_TOGGLE_TOGGLE_STATE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, toggleState);
        } else if (propertyId == UIA_MULTIPLE_VIEW_SUPPORTED_VIEWS_PROPERTY_ID) {
            MemorySegment views = arena.allocate(8);
            views.set(ValueLayout.JAVA_INT, 0L, 1);
            views.set(ValueLayout.JAVA_INT, 4L, currentView);
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) (VT_ARRAY | VT_I4));
            variant.set(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET, views);
        } else if (propertyId == UIA_MULTIPLE_VIEW_CURRENT_VIEW_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, currentView);
        } else if (propertyId == UIA_SELECTION_ITEM_IS_SELECTED_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, itemSelected ? 1 : 0);
        } else if (propertyId == UIA_GRID_ROW_COUNT_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, gridRows);
        } else if (propertyId == UIA_GRID_COLUMN_COUNT_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, gridColumns);
        } else if (propertyId == UIA_GRID_ITEM_ROW_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, cellRow);
        } else if (propertyId == UIA_GRID_ITEM_COLUMN_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, cellColumn);
        } else if (propertyId == UIA_GRID_ITEM_ROW_SPAN_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, cellRowSpan);
        } else if (propertyId == UIA_GRID_ITEM_COLUMN_SPAN_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, cellColumnSpan);
        } else if (propertyId == UIA_GRID_ITEM_CONTAINING_GRID_PROPERTY_ID) {
            writeUnknownVariant(variant, simpleObject);
        } else if (propertyId == UIA_SELECTION_ITEM_SELECTION_CONTAINER_PROPERTY_ID) {
            writeUnknownVariant(variant, simpleObject);
        } else if (propertyId == UIA_LEGACY_IACCESSIBLE_SELECTION_PROPERTY_ID) {
            writeUnknownVariant(variant, foundItemObject);
        } else if (propertyId == UIA_WINDOW_CAN_MAXIMIZE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_WINDOW_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_WINDOW_CAN_MINIMIZE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_WINDOW_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_WINDOW_WINDOW_VISUAL_STATE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, windowVisualState);
        } else if (propertyId == UIA_WINDOW_WINDOW_INTERACTION_STATE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, WINDOW_INTERACTION_READY);
        } else if (propertyId == UIA_WINDOW_IS_MODAL_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    node.role() == SemanticsRole.DIALOG ? 1 : 0
            );
        } else if (propertyId == UIA_WINDOW_IS_TOPMOST_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (propertyId == UIA_TABLE_ROW_HEADERS_PROPERTY_ID
                || propertyId == UIA_TABLE_COLUMN_HEADERS_PROPERTY_ID
                || propertyId == UIA_TABLE_ITEM_ROW_HEADER_ITEMS_PROPERTY_ID
                || propertyId == UIA_TABLE_ITEM_COLUMN_HEADER_ITEMS_PROPERTY_ID) {
            int count = 0;
            if (propertyId == UIA_TABLE_COLUMN_HEADERS_PROPERTY_ID) {
                count = columnHeaderNames.length;
            } else if (propertyId == UIA_TABLE_ROW_HEADERS_PROPERTY_ID) {
                count = rowHeaderNames.length;
            } else if (propertyId == UIA_TABLE_ITEM_COLUMN_HEADER_ITEMS_PROPERTY_ID) {
                count = cellColumnHeader.isEmpty() ? 0 : 1;
            } else {
                count = cellRowHeader.isEmpty() ? 0 : 1;
            }
            MemorySegment headers = arena.allocate(8);
            headers.set(ValueLayout.JAVA_INT, 0L, count);
            headers.set(ValueLayout.JAVA_INT, 4L, 0);
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) (VT_ARRAY | VT_I4));
            variant.set(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET, headers);
        } else if (propertyId == UIA_TABLE_ROW_OR_COLUMN_MAJOR_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, ROW_OR_COLUMN_MAJOR_ROW);
        } else if (propertyId == UIA_SELECTION_SELECTION_PROPERTY_ID) {
            writeUnknownVariant(variant, foundItemObject);
        } else if (propertyId == UIA_SELECTION_CAN_SELECT_MULTIPLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_SELECTION_PATTERN_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_SELECTION_IS_SELECTION_REQUIRED_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (propertyId == UIA_SCROLL_HORIZONTAL_SCROLL_PERCENT_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(
                    ValueLayout.JAVA_DOUBLE,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    horizontallyScrollable ? horizontalScrollPercent : SCROLL_NO_AMOUNT
            );
        } else if (propertyId == UIA_SCROLL_HORIZONTAL_VIEW_SIZE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, horizontalViewSize);
        } else if (propertyId == UIA_SCROLL_VERTICAL_SCROLL_PERCENT_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, verticalScrollPercent);
        } else if (propertyId == UIA_SCROLL_HORIZONTALLY_SCROLLABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, horizontallyScrollable ? 1 : 0);
        } else if (propertyId == UIA_SCROLL_VERTICAL_VIEW_SIZE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, verticalViewSize);
        } else if (propertyId == UIA_SCROLL_VERTICALLY_SCROLLABLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, verticallyScrollable ? 1 : 0);
        } else if (propertyId == UIA_RANGE_VALUE_MINIMUM_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, RANGE_MINIMUM);
        } else if (propertyId == UIA_RANGE_VALUE_MAXIMUM_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, RANGE_MAXIMUM);
        } else if (propertyId == UIA_RANGE_VALUE_IS_READ_ONLY_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.readOnly() ? 1 : 0);
        } else if (propertyId == UIA_RANGE_VALUE_LARGE_CHANGE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, RANGE_LARGE_CHANGE);
        } else if (propertyId == UIA_RANGE_VALUE_SMALL_CHANGE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, RANGE_SMALL_CHANGE);
        } else if (propertyId == UIA_ANNOTATION_ANNOTATION_TYPE_ID_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, ANNOTATION_TYPE_COMMENT);
        } else if (propertyId == UIA_ANNOTATION_ANNOTATION_TYPE_NAME_PROPERTY_ID) {
            writeBstrVariant(variant, "Comment");
        } else if (propertyId == UIA_ANNOTATION_AUTHOR_PROPERTY_ID) {
            writeBstrVariant(variant, "Himari");
        } else if (propertyId == UIA_ANNOTATION_DATE_TIME_PROPERTY_ID) {
            writeBstrVariant(variant, "2026-08-17");
        } else if (propertyId == UIA_STYLES_STYLE_ID_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, STYLE_ID_NORMAL);
        } else if (propertyId == UIA_STYLES_STYLE_NAME_PROPERTY_ID) {
            writeBstrVariant(variant, "Normal");
        } else if (propertyId == UIA_STYLES_FILL_COLOR_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.fillColor());
        } else if (propertyId == UIA_STYLES_FILL_PATTERN_STYLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.fillType());
        } else if (propertyId == UIA_STYLES_SHAPE_PROPERTY_ID) {
            writeBstrVariant(variant, STYLE_SHAPE_RECTANGLE);
        } else if (propertyId == UIA_STYLES_FILL_PATTERN_COLOR_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.outlineColor());
        } else if (propertyId == UIA_STYLES_EXTENDED_PROPERTIES_PROPERTY_ID) {
            writeBstrVariant(variant, node.ariaProperties());
        } else if (propertyId == UIA_SPREADSHEET_ITEM_FORMULA_PROPERTY_ID) {
            writeBstrVariant(variant, "=" + node.label());
        } else if (propertyId == UIA_SPREADSHEET_ITEM_ANNOTATION_OBJECTS_PROPERTY_ID) {
            writeUnknownVariant(variant, foundItemObject);
        } else if (propertyId == UIA_SPREADSHEET_ITEM_ANNOTATION_TYPES_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, ANNOTATION_TYPE_COMMENT);
        } else if (propertyId == UIA_DRAG_IS_GRABBED_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (propertyId == UIA_DRAG_DROP_EFFECT_PROPERTY_ID) {
            writeBstrVariant(variant, "copy");
        } else if (propertyId == UIA_DRAG_DROP_EFFECTS_PROPERTY_ID) {
            writeBstrVariant(variant, "copy");
        } else if (propertyId == UIA_DROP_TARGET_DROP_TARGET_EFFECT_PROPERTY_ID) {
            writeBstrVariant(variant, "move");
        } else if (propertyId == UIA_DROP_TARGET_DROP_TARGET_EFFECTS_PROPERTY_ID) {
            writeBstrVariant(variant, "move");
        } else if (propertyId == UIA_DRAG_GRABBED_ITEMS_PROPERTY_ID) {
            writeUnknownVariant(variant, foundItemObject);
        } else if (propertyId == UIA_TRANSFORM2_ZOOM_LEVEL_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, transformZoom);
        } else if (propertyId == UIA_TRANSFORM2_ZOOM_MINIMUM_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, ZOOM_MINIMUM);
        } else if (propertyId == UIA_TRANSFORM2_ZOOM_MAXIMUM_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, ZOOM_MAXIMUM);
        } else if (propertyId == UIA_TRANSFORM2_CAN_ZOOM_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    patternAvailable(UIA_TRANSFORM_PATTERN2_ID) ? 1 : 0
            );
        } else if (propertyId == UIA_SELECTION2_ITEM_COUNT_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 1);
        } else if (propertyId == UIA_SELECTION2_FIRST_SELECTED_ITEM_PROPERTY_ID
                || propertyId == UIA_SELECTION2_LAST_SELECTED_ITEM_PROPERTY_ID
                || propertyId == UIA_SELECTION2_CURRENT_SELECTED_ITEM_PROPERTY_ID) {
            writeUnknownVariant(variant, foundItemObject);
        } else if (propertyId == UIA_ANNOTATION_TARGET_PROPERTY_ID) {
            writeUnknownVariant(variant, simpleObject);
        } else if (propertyId == UIA_LEGACY_IACCESSIBLE_CHILD_ID_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (propertyId == UIA_LEGACY_IACCESSIBLE_NAME_PROPERTY_ID) {
            writeBstrVariant(variant, node.label());
        } else if (propertyId == UIA_LEGACY_IACCESSIBLE_VALUE_PROPERTY_ID) {
            writeBstrVariant(variant, node.label());
        } else if (propertyId == UIA_LEGACY_IACCESSIBLE_DESCRIPTION_PROPERTY_ID) {
            writeBstrVariant(variant, node.label());
        } else if (propertyId == UIA_LEGACY_IACCESSIBLE_ROLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    node.actions().contains(SemanticsAction.ACTIVATE)
                            ? ROLE_SYSTEM_PUSHBUTTON
                            : ROLE_SYSTEM_CLIENT
            );
        } else if (propertyId == UIA_LEGACY_IACCESSIBLE_STATE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, STATE_SYSTEM_FOCUSABLE);
        } else if (propertyId == UIA_LEGACY_IACCESSIBLE_HELP_PROPERTY_ID) {
            writeBstrVariant(variant, node.label());
        } else if (propertyId == UIA_LEGACY_IACCESSIBLE_KEYBOARD_SHORTCUT_PROPERTY_ID) {
            writeBstrVariant(variant, "");
        } else if (propertyId == UIA_LEGACY_IACCESSIBLE_DEFAULT_ACTION_PROPERTY_ID) {
            writeBstrVariant(
                    variant,
                    node.actions().contains(SemanticsAction.ACTIVATE) ? "Press" : ""
            );
        } else if (propertyId == UIA_LIVE_SETTING_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, liveSettingId(node));
        } else if (propertyId == UIA_IS_PASSWORD_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.password() ? 1 : 0);
        } else if (propertyId == UIA_IS_OFFSCREEN_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    node.bounds().width() <= 0.0f || node.bounds().height() <= 0.0f ? 1 : 0
            );
        } else if (propertyId == UIA_FRAMEWORK_ID_PROPERTY_ID) {
            writeBstrVariant(variant, "HimariUI");
        } else if (propertyId == UIA_IS_CONTROL_ELEMENT_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 1);
        } else if (propertyId == UIA_IS_CONTENT_ELEMENT_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    node.role() == SemanticsRole.NONE ? 0 : 1
            );
        } else if (propertyId == UIA_ORIENTATION_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, orientationId(node));
        } else if (propertyId == UIA_CULTURE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, cultureId(node.locale()));
        } else if (propertyId == UIA_IS_REQUIRED_FOR_FORM_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.required() ? 1 : 0);
        } else if (propertyId == UIA_ITEM_STATUS_PROPERTY_ID) {
            writeBstrVariant(variant, node.itemStatus());
        } else if (propertyId == UIA_ITEM_TYPE_PROPERTY_ID) {
            writeBstrVariant(variant, node.itemType());
        } else if (propertyId == UIA_LANDMARK_TYPE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.landmarkType());
        } else if (propertyId == UIA_LOCALIZED_LANDMARK_TYPE_PROPERTY_ID) {
            writeBstrVariant(variant, node.localizedLandmarkType());
        } else if (propertyId == UIA_ARIA_ROLE_PROPERTY_ID) {
            writeBstrVariant(variant, node.ariaRole());
        } else if (propertyId == UIA_ARIA_PROPERTIES_PROPERTY_ID) {
            writeBstrVariant(variant, node.ariaProperties());
        } else if (propertyId == UIA_CONTROLLER_FOR_PROPERTY_ID) {
            writeBstrVariant(variant, node.controllerFor());
        } else if (propertyId == UIA_DESCRIBED_BY_PROPERTY_ID) {
            writeBstrVariant(variant, node.describedBy());
        } else if (propertyId == UIA_FLOWS_TO_PROPERTY_ID) {
            writeBstrVariant(variant, node.flowsTo());
        } else if (propertyId == UIA_LABELED_BY_PROPERTY_ID) {
            writeBstrVariant(variant, node.labeledBy());
        } else if (propertyId == UIA_FLOWS_FROM_PROPERTY_ID) {
            writeBstrVariant(variant, node.flowsFrom());
        } else if (propertyId == UIA_OPTIMIZE_FOR_VISUAL_CONTENT_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    node.optimizeForVisualContent() ? 1 : 0
            );
        } else if (propertyId == UIA_FILL_COLOR_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.fillColor());
        } else if (propertyId == UIA_OUTLINE_COLOR_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.outlineColor());
        } else if (propertyId == UIA_FILL_TYPE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.fillType());
        } else if (propertyId == UIA_VISUAL_EFFECTS_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.visualEffects());
        } else if (propertyId == UIA_OUTLINE_THICKNESS_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.outlineThickness());
        } else if (propertyId == UIA_CENTER_POINT_PROPERTY_ID) {
            writeClickablePoint(variant, node.bounds());
        } else if (propertyId == UIA_ROTATION_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.rotation());
        } else if (propertyId == UIA_SIZE_PROPERTY_ID) {
            writeSize(variant, node.bounds());
        } else if (propertyId == UIA_RUNTIME_ID_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, (int) node.id());
        } else if (propertyId == UIA_IS_PERIPHERAL_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.peripheral() ? 1 : 0);
        } else if (propertyId == UIA_ANNOTATION_TYPES_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.annotationType());
        } else if (propertyId == UIA_ANNOTATION_OBJECTS_PROPERTY_ID) {
            writeBstrVariant(variant, node.annotationObjects());
        } else if (propertyId == UIA_LEVEL_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.level());
        } else if (propertyId == UIA_POSITION_IN_SET_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.positionInSet());
        } else if (propertyId == UIA_SIZE_OF_SET_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.sizeOfSet());
        } else if (propertyId == UIA_FULL_DESCRIPTION_PROPERTY_ID) {
            writeBstrVariant(variant, node.description());
        } else if (propertyId == UIA_IS_DATA_VALID_FOR_FORM_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.error() ? 0 : 1);
        } else if (propertyId == UIA_PROVIDER_DESCRIPTION_PROPERTY_ID) {
            writeBstrVariant(variant, "HimariUI." + node.role().name());
        } else if (propertyId == UIA_IS_DIALOG_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    node.role() == SemanticsRole.DIALOG ? 1 : 0
            );
        } else if (propertyId == UIA_CLICKABLE_POINT_PROPERTY_ID) {
            writeClickablePoint(variant, node.bounds());
        } else if (propertyId == UIA_NATIVE_WINDOW_HANDLE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, (int) hwnd.address());
        } else if (propertyId == UIA_HEADING_LEVEL_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, headingLevelId(node));
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

    /// Implements `IVirtualizedItemProvider::Realize`.
    private int realizeVirtualizedItem(MemorySegment self) {
        virtualizedItemCount++;
        return S_OK;
    }

    /// Implements `IDockProvider::SetDockPosition`.
    private int setDockPosition(MemorySegment self, int position) {
        dockPosition = position;
        return S_OK;
    }

    /// Implements `IDockProvider::get_DockPosition`.
    private int getDockPosition(MemorySegment self, MemorySegment value) {
        return writeInt(value, dockPosition);
    }

    /// Implements `ITransformProvider::Move`.
    private int moveTransform(MemorySegment self, double x, double y) {
        transformX = x;
        transformY = y;
        return S_OK;
    }

    /// Implements `ITransformProvider::Resize`.
    private int resizeTransform(MemorySegment self, double width, double height) {
        transformWidth = width;
        transformHeight = height;
        return S_OK;
    }

    /// Implements `ITransformProvider::Rotate`.
    private int rotateTransform(MemorySegment self, double degrees) {
        transformRotate = degrees;
        return S_OK;
    }

    /// Implements `ITransformProvider::get_CanMove`.
    private int getCanMove(MemorySegment self, MemorySegment value) {
        return writeInt(value, 1);
    }

    /// Implements `ITransformProvider::get_CanResize`.
    private int getCanResize(MemorySegment self, MemorySegment value) {
        return writeInt(value, 1);
    }

    /// Implements `ITransformProvider::get_CanRotate`.
    private int getCanRotate(MemorySegment self, MemorySegment value) {
        return writeInt(value, 1);
    }

    /// Implements `ITransformProvider2::Zoom`.
    private int zoomTransform(MemorySegment self, double zoom) {
        transformZoom = zoom;
        return S_OK;
    }

    /// Implements `ITransformProvider2::get_CanZoom`.
    private int getCanZoom(MemorySegment self, MemorySegment value) {
        return writeInt(value, 1);
    }

    /// Implements `ITransformProvider2::get_ZoomLevel`.
    private int getZoomLevel(MemorySegment self, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        value.reinterpret(ValueLayout.JAVA_DOUBLE.byteSize()).set(ValueLayout.JAVA_DOUBLE, 0L, transformZoom);
        return S_OK;
    }

    /// Implements `ITransformProvider2::ZoomByUnit`.
    private int zoomByUnit(MemorySegment self, int zoomUnit) {
        if (zoomUnit != ZOOM_UNIT_LARGE_INCREMENT) {
            return E_INVALIDARG;
        }
        transformZoom += 1.0;
        return S_OK;
    }

    /// Implements `ITransformProvider2::get_ZoomMinimum`.
    private int getZoomMinimum(MemorySegment self, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        value.reinterpret(ValueLayout.JAVA_DOUBLE.byteSize()).set(ValueLayout.JAVA_DOUBLE, 0L, ZOOM_MINIMUM);
        return S_OK;
    }

    /// Implements `ITransformProvider2::get_ZoomMaximum`.
    private int getZoomMaximum(MemorySegment self, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        value.reinterpret(ValueLayout.JAVA_DOUBLE.byteSize()).set(ValueLayout.JAVA_DOUBLE, 0L, ZOOM_MAXIMUM);
        return S_OK;
    }

    /// Implements `IItemContainerProvider::FindItemByProperty`.
    private int findItemByProperty(
            MemorySegment self,
            MemorySegment startAfter,
            int propertyId,
            MemorySegment value,
            MemorySegment found
    ) {
        if (found.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment out = found.reinterpret(ValueLayout.ADDRESS.byteSize());
        if (propertyId != UIA_NAME_PROPERTY_ID || startAfter.address() != 0L || value.address() == 0L) {
            out.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            return S_OK;
        }
        MemorySegment variant = value.reinterpret(Win32Layouts.VARIANT.byteSize());
        short vt = variant.get(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET);
        if (vt != VT_BSTR) {
            out.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            return S_OK;
        }
        MemorySegment chars = variant.get(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET);
        if (chars.address() == 0L) {
            out.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            return S_OK;
        }
        MemorySegment readable = chars.byteSize() < 2L ? chars.reinterpret(256) : chars;
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < 64; index++) {
            char unit = readable.getAtIndex(ValueLayout.JAVA_CHAR, index);
            if (unit == 0) {
                break;
            }
            text.append(unit);
        }
        if (!node.label().equals(text.toString())) {
            out.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            return S_OK;
        }
        out.set(ValueLayout.ADDRESS, 0L, foundItemObject);
        addRef(foundItemObject);
        return S_OK;
    }

    /// Implements `GetPatternProvider` on a found item.
    private int getFoundItemPatternProvider(MemorySegment self, int patternId, MemorySegment provider) {
        if (provider.address() == 0L) {
            return E_POINTER;
        }
        provider.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return S_OK;
    }

    /// Implements `ISynchronizedInputProvider::StartListening`.
    private int startListening(MemorySegment self, int inputType) {
        synchronizedInputType = inputType;
        synchronizedInputStarts++;
        return S_OK;
    }

    /// Implements `ISynchronizedInputProvider::Cancel`.
    private int cancelSynchronizedInput(MemorySegment self) {
        synchronizedInputCancels++;
        return S_OK;
    }

    /// Implements `IMultipleViewProvider::SetCurrentView`.
    private int setCurrentView(MemorySegment self, int viewId) {
        currentView = viewId;
        return S_OK;
    }

    /// Implements `IMultipleViewProvider::get_CurrentView`.
    private int getCurrentView(MemorySegment self, MemorySegment value) {
        return writeInt(value, currentView);
    }

    /// Implements `IDropTargetProvider::get_DropTargetEffect`.
    private int getDropTargetEffect(MemorySegment self, MemorySegment effect) {
        return writeUtf16(effect, "move");
    }

    /// Implements `IDragProvider::get_IsGrabbed`.
    private int getIsGrabbed(MemorySegment self, MemorySegment value) {
        return writeInt(value, 0);
    }

    /// Implements `IDragProvider::get_DropEffect`.
    private int getDropEffect(MemorySegment self, MemorySegment effect) {
        return writeUtf16(effect, "copy");
    }

    /// Implements `IAnnotationProvider::get_AnnotationTypeId`.
    private int getAnnotationTypeId(MemorySegment self, MemorySegment value) {
        return writeInt(value, ANNOTATION_TYPE_COMMENT);
    }

    /// Implements `IAnnotationProvider::get_AnnotationTypeName`.
    private int getAnnotationTypeName(MemorySegment self, MemorySegment name) {
        return writeUtf16(name, "Comment");
    }

    /// Implements `IAnnotationProvider::get_Author`.
    private int getAnnotationAuthor(MemorySegment self, MemorySegment author) {
        return writeUtf16(author, "Himari");
    }

    /// Implements `IAnnotationProvider::get_DateTime`.
    private int getAnnotationDateTime(MemorySegment self, MemorySegment dateTime) {
        return writeUtf16(dateTime, "2026-08-17");
    }

    /// Implements `IAnnotationProvider::get_Target`.
    private int getAnnotationTarget(MemorySegment self, MemorySegment target) {
        if (target.address() == 0L) {
            return E_POINTER;
        }
        target.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, simpleObject);
        addRef(simpleObject);
        return S_OK;
    }

    /// Implements `ITextChildProvider::get_TextContainer`.
    private int getTextContainer(MemorySegment self, MemorySegment container) {
        if (container.address() == 0L) {
            return E_POINTER;
        }
        container.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, simpleObject);
        addRef(simpleObject);
        return S_OK;
    }

    /// Implements `ITextChildProvider::get_TextRange`.
    private int getTextChildRange(MemorySegment self, MemorySegment range) {
        if (range.address() == 0L) {
            return E_POINTER;
        }
        range.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, textRangeObject);
        addRef(textRangeObject);
        return S_OK;
    }

    /// Implements `IStylesProvider::get_StyleId`.
    private int getStyleId(MemorySegment self, MemorySegment value) {
        return writeInt(value, STYLE_ID_NORMAL);
    }

    /// Implements `IStylesProvider::get_StyleName`.
    private int getStyleName(MemorySegment self, MemorySegment name) {
        return writeUtf16(name, "Normal");
    }

    /// Implements `ISpreadsheetProvider::GetItemByName`.
    private int getSpreadsheetItemByName(MemorySegment self, MemorySegment name, MemorySegment found) {
        if (found.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment out = found.reinterpret(ValueLayout.ADDRESS.byteSize());
        if (name.address() == 0L || !node.label().equals(decodeUtf16(name))) {
            out.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            return S_OK;
        }
        out.set(ValueLayout.ADDRESS, 0L, foundItemObject);
        addRef(foundItemObject);
        return S_OK;
    }

    /// Implements `ICustomNavigationProvider::Navigate`.
    private int navigateCustom(MemorySegment self, int direction, MemorySegment retVal) {
        if (retVal.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment out = retVal.reinterpret(ValueLayout.ADDRESS.byteSize());
        if (direction != NAVIGATE_DIRECTION_PARENT) {
            out.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            return S_OK;
        }
        out.set(ValueLayout.ADDRESS, 0L, simpleObject);
        addRef(simpleObject);
        return S_OK;
    }

    /// Implements `IObjectModelProvider::GetUnderlyingObjectModel`.
    private int getUnderlyingObjectModel(MemorySegment self, MemorySegment retVal) {
        if (retVal.address() == 0L) {
            return E_POINTER;
        }
        retVal.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, simpleObject);
        addRef(simpleObject);
        return S_OK;
    }

    /// Implements `ITextEditProvider::GetActiveComposition`.
    private int getActiveComposition(MemorySegment self, MemorySegment range) {
        if (range.address() == 0L) {
            return E_POINTER;
        }
        range.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, textRangeObject);
        addRef(textRangeObject);
        return S_OK;
    }

    /// Implements `ITextEditProvider::GetConversionTarget`.
    private int getConversionTarget(MemorySegment self, MemorySegment range) {
        if (range.address() == 0L) {
            return E_POINTER;
        }
        range.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, textRangeObject);
        addRef(textRangeObject);
        return S_OK;
    }

    /// Implements `ISelectionProvider::get_CanSelectMultiple`.
    private int getCanSelectMultiple(MemorySegment self, MemorySegment value) {
        return writeInt(value, 1);
    }

    /// Implements `ISelectionProvider::get_IsSelectionRequired`.
    private int getIsSelectionRequired(MemorySegment self, MemorySegment value) {
        return writeInt(value, 0);
    }

    /// Implements `ILegacyIAccessibleProvider::get_ChildId`.
    private int getLegacyChildId(MemorySegment self, MemorySegment value) {
        return writeInt(value, 0);
    }

    /// Implements `ILegacyIAccessibleProvider::get_Name`.
    private int getLegacyName(MemorySegment self, MemorySegment name) {
        return writeUtf16(name, node.label());
    }

    /// Implements `ILegacyIAccessibleProvider::get_Role`.
    private int getLegacyRole(MemorySegment self, MemorySegment value) {
        int role = node.actions().contains(SemanticsAction.ACTIVATE)
                ? ROLE_SYSTEM_PUSHBUTTON
                : ROLE_SYSTEM_CLIENT;
        return writeInt(value, role);
    }

    /// Implements `ILegacyIAccessibleProvider::DoDefaultAction`.
    private int doLegacyDefaultAction(MemorySegment self) {
        invokeCount++;
        return S_OK;
    }

    /// Implements `ILegacyIAccessibleProvider::get_Value`.
    ///
    /// @param self the unused COM this pointer
    /// @param value the out-parameter that receives the accessible value
    /// @return `S_OK`, or `E_POINTER` when `value` is null
    private int getLegacyValue(MemorySegment self, MemorySegment value) {
        return writeUtf16(value, node.label());
    }

    /// Implements `ILegacyIAccessibleProvider::get_State`.
    ///
    /// @param self the unused COM this pointer
    /// @param state the out-parameter that receives the MSAA state bits
    /// @return `S_OK`, or `E_POINTER` when `state` is null
    private int getLegacyState(MemorySegment self, MemorySegment state) {
        return writeInt(state, STATE_SYSTEM_FOCUSABLE);
    }

    /// Implements `ILegacyIAccessibleProvider::get_Description`.
    ///
    /// @param self the unused COM this pointer
    /// @param description the out-parameter that receives the accessible description
    /// @return `S_OK`, or `E_POINTER` when `description` is null
    private int getLegacyDescription(MemorySegment self, MemorySegment description) {
        return writeUtf16(description, node.label());
    }

    /// Implements `ILegacyIAccessibleProvider::get_DefaultAction`.
    ///
    /// @param self the unused COM this pointer
    /// @param action the out-parameter that receives the default-action name
    /// @return `S_OK`, or `E_POINTER` when `action` is null
    private int getLegacyDefaultAction(MemorySegment self, MemorySegment action) {
        String name = node.actions().contains(SemanticsAction.ACTIVATE) ? "Press" : "";
        return writeUtf16(action, name);
    }

    /// Implements `ILegacyIAccessibleProvider::get_KeyboardShortcut`.
    ///
    /// @param self the unused COM this pointer
    /// @param shortcut the out-parameter that receives the shortcut string
    /// @return `S_OK`, or `E_POINTER` when `shortcut` is null
    private int getLegacyKeyboardShortcut(MemorySegment self, MemorySegment shortcut) {
        return writeUtf16(shortcut, "");
    }

    /// Implements `ILegacyIAccessibleProvider::get_Help`.
    ///
    /// @param self the unused COM this pointer
    /// @param help the out-parameter that receives the help string
    /// @return `S_OK`, or `E_POINTER` when `help` is null
    private int getLegacyHelp(MemorySegment self, MemorySegment help) {
        return writeUtf16(help, node.label());
    }

    /// Implements `IRawElementProviderFragment::Navigate`.
    ///
    /// @param self the unused COM this pointer
    /// @param direction a `NavigateDirection` value
    /// @param retVal the out-parameter that receives the fragment
    /// @return `S_OK`, or `E_POINTER` when `retVal` is null
    private int navigateFragment(MemorySegment self, int direction, MemorySegment retVal) {
        if (retVal.address() == 0L) {
            return E_POINTER;
        }
        if (direction != NAVIGATE_DIRECTION_PARENT) {
            retVal.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            return S_OK;
        }
        retVal.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, fragmentObject);
        addRef(fragmentObject);
        return S_OK;
    }

    /// Implements `IRawElementProviderSimple::get_ProviderOptions`.
    ///
    /// @param self the unused COM this pointer
    /// @param options the out-parameter that receives the bitfield
    /// @return `S_OK`, or `E_POINTER` when `options` is null
    private int getProviderOptions(MemorySegment self, MemorySegment options) {
        if (options.address() == 0L) {
            return E_POINTER;
        }
        options.reinterpret(ValueLayout.JAVA_INT.byteSize()).set(ValueLayout.JAVA_INT, 0L, PROVIDER_OPTIONS_SERVER_SIDE);
        return S_OK;
    }

    /// Implements `IRawElementProviderSimple::get_HostRawElementProvider`.
    ///
    /// @param self the unused COM this pointer
    /// @param host the out-parameter that receives the host provider
    /// @return `S_OK`, or `E_POINTER` when `host` is null
    private int getHostRawElementProvider(MemorySegment self, MemorySegment host) {
        if (host.address() == 0L) {
            return E_POINTER;
        }
        host.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, simpleObject);
        addRef(simpleObject);
        return S_OK;
    }

    /// Implements `IRawElementProviderFragment::SetFocus`.
    ///
    /// @param self the unused COM this pointer
    /// @return `S_OK`
    private int setFragmentFocus(MemorySegment self) {
        fragmentFocusCount++;
        return S_OK;
    }

    /// Implements `IRawElementProviderFragment::get_FragmentRoot`.
    ///
    /// @param self the unused COM this pointer
    /// @param root the out-parameter that receives the fragment root
    /// @return `S_OK`, or `E_POINTER` when `root` is null
    private int getFragmentRoot(MemorySegment self, MemorySegment root) {
        if (root.address() == 0L) {
            return E_POINTER;
        }
        root.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, fragmentObject);
        addRef(fragmentObject);
        return S_OK;
    }

    /// Implements `IRawElementProviderFragment::get_BoundingRectangle`.
    ///
    /// @param self the unused COM this pointer
    /// @param rect the out-parameter that receives the UIA rectangle
    /// @return `S_OK`, or `E_POINTER` when `rect` is null
    private int getFragmentBoundingRectangle(MemorySegment self, MemorySegment rect) {
        if (rect.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment writable = rect.byteSize() < 32L ? rect.reinterpret(32) : rect;
        writable.set(ValueLayout.JAVA_DOUBLE, 0L, node.bounds().x());
        writable.set(ValueLayout.JAVA_DOUBLE, 8L, node.bounds().y());
        writable.set(ValueLayout.JAVA_DOUBLE, 16L, node.bounds().width());
        writable.set(ValueLayout.JAVA_DOUBLE, 24L, node.bounds().height());
        return S_OK;
    }

    /// Implements `IRawElementProviderFragment::GetRuntimeId`.
    ///
    /// @param self the unused COM this pointer
    /// @param runtimeId the out-parameter that receives the packed identifier
    /// @return `S_OK`, or `E_POINTER` when `runtimeId` is null
    private int getFragmentRuntimeId(MemorySegment self, MemorySegment runtimeId) {
        if (runtimeId.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment block = arena.allocate(8);
        block.set(ValueLayout.JAVA_INT, 0L, 1);
        block.set(ValueLayout.JAVA_INT, 4L, (int) node.id());
        runtimeId.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, block);
        return S_OK;
    }

    /// Implements `IRawElementProviderFragment::GetEmbeddedFragmentRoots`.
    ///
    /// @param self the unused COM this pointer
    /// @param roots the out-parameter that receives the packed empty root list
    /// @return `S_OK`, or `E_POINTER` when `roots` is null
    private int getEmbeddedFragmentRoots(MemorySegment self, MemorySegment roots) {
        return writeEmptyPacked(roots);
    }

    /// Implements `IRawElementProviderFragmentRoot::ElementProviderFromPoint`.
    ///
    /// @param self the unused COM this pointer
    /// @param x the unused horizontal point
    /// @param y the unused vertical point
    /// @param retVal the out-parameter that receives the fragment
    /// @return `S_OK`, or `E_POINTER` when `retVal` is null
    private int elementProviderFromPoint(MemorySegment self, double x, double y, MemorySegment retVal) {
        if (retVal.address() == 0L) {
            return E_POINTER;
        }
        retVal.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, fragmentObject);
        addRef(fragmentObject);
        return S_OK;
    }

    /// Implements `IRawElementProviderFragmentRoot::GetFocus`.
    ///
    /// @param self the unused COM this pointer
    /// @param retVal the out-parameter that receives the focused fragment
    /// @return `S_OK`, or `E_POINTER` when `retVal` is null
    private int getFragmentRootFocus(MemorySegment self, MemorySegment retVal) {
        if (retVal.address() == 0L) {
            return E_POINTER;
        }
        retVal.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, fragmentObject);
        addRef(fragmentObject);
        return S_OK;
    }

    /// Implements `ILegacyIAccessibleProvider::SetValue`.
    ///
    /// @param self the unused COM this pointer
    /// @param value the UTF-16 value
    /// @return `S_OK`
    private int setLegacyValue(MemorySegment self, MemorySegment value) {
        lastLegacyValue = decodeUtf16(value);
        legacySetValueCount++;
        return S_OK;
    }

    /// Implements `ITextRangeProvider2::ShowContextMenu`.
    ///
    /// @param self the unused COM this pointer
    /// @return `S_OK`
    private int showTextRangeContextMenu(MemorySegment self) {
        textRangeContextMenuCount++;
        return S_OK;
    }

    /// Implements `IRawElementProviderSimple2::ShowContextMenu`.
    ///
    /// @param self the unused COM this pointer
    /// @return `S_OK`
    private int showSimpleContextMenu(MemorySegment self) {
        simpleContextMenuCount++;
        return S_OK;
    }

    /// Implements `ITextProvider2::GetCaretRange`.
    private int getCaretRange(MemorySegment self, MemorySegment isActive, MemorySegment range) {
        if (isActive.address() == 0L || range.address() == 0L) {
            return E_POINTER;
        }
        writeInt(isActive, 1);
        range.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, textRangeObject);
        addRef(textRangeObject);
        return S_OK;
    }

    /// Implements `ISpreadsheetItemProvider::get_Formula`.
    private int getSpreadsheetFormula(MemorySegment self, MemorySegment formula) {
        return writeUtf16(formula, "=" + node.label());
    }

    /// Implements `ISelectionProvider2::get_ItemCount`.
    private int getSelectionItemCount(MemorySegment self, MemorySegment value) {
        return writeInt(value, 1);
    }

    /// Implements `ISelectionProvider2::get_CurrentSelectedItem`.
    private int getCurrentSelectedItem(MemorySegment self, MemorySegment item) {
        return writeSelectedItem(item);
    }

    /// Implements `ISelectionProvider2::get_FirstSelectedItem`.
    ///
    /// @param self the unused COM this pointer
    /// @param item the out-parameter that receives the selected item
    /// @return `S_OK`, or `E_POINTER` when `item` is null
    private int getFirstSelectedItem(MemorySegment self, MemorySegment item) {
        return writeSelectedItem(item);
    }

    /// Implements `ISelectionProvider2::get_LastSelectedItem`.
    ///
    /// @param self the unused COM this pointer
    /// @param item the out-parameter that receives the selected item
    /// @return `S_OK`, or `E_POINTER` when `item` is null
    private int getLastSelectedItem(MemorySegment self, MemorySegment item) {
        return writeSelectedItem(item);
    }

    /// Implements `ITextProvider2::RangeFromAnnotation`.
    ///
    /// @param self the unused COM this pointer
    /// @param annotation unused; first-stable returns the document range
    /// @param range the out-parameter that receives the text range
    /// @return `S_OK`, or `E_POINTER` when `range` is null
    private int rangeFromAnnotation(MemorySegment self, MemorySegment annotation, MemorySegment range) {
        if (range.address() == 0L) {
            return E_POINTER;
        }
        range.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, textRangeObject);
        addRef(textRangeObject);
        return S_OK;
    }

    /// Writes the first-stable selected-item pointer.
    ///
    /// @param item the out-parameter that receives [`#foundItemObject`]
    /// @return `S_OK`, or `E_POINTER` when `item` is null
    private int writeSelectedItem(MemorySegment item) {
        if (item.address() == 0L) {
            return E_POINTER;
        }
        item.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, foundItemObject);
        addRef(foundItemObject);
        return S_OK;
    }

    /// Implements `IMultipleViewProvider::GetViewName`.
    private int getViewName(MemorySegment self, int viewId, MemorySegment name) {
        if (name.address() == 0L) {
            return E_POINTER;
        }
        String label = viewId == 1 ? "List" : node.label();
        MemorySegment chars = arena.allocate((label.length() + 1L) * 2L);
        byte[] utf16 = label.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment.copy(utf16, 0, chars, ValueLayout.JAVA_BYTE, 0L, utf16.length);
        name.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, chars);
        return S_OK;
    }

    /// Implements `GetPropertyValue` on a found item.
    private int getFoundItemPropertyValue(MemorySegment self, int propertyId, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment variant = value.reinterpret(Win32Layouts.VARIANT.byteSize());
        variant.fill((byte) 0);
        if (propertyId == UIA_NAME_PROPERTY_ID) {
            writeBstrVariant(variant, node.label());
        } else if (propertyId == UIA_HELP_TEXT_PROPERTY_ID) {
            writeBstrVariant(variant, node.hint());
        }
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
        return writeInt(state, node.readOnly() ? 1 : 0);
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

    /// Implements `ITableItemProvider::GetRowHeaderItems`.
    private int getRowHeaderItems(MemorySegment self, MemorySegment headers) {
        if (cellRowHeaderObject.address() == 0L) {
            return writeEmptyPacked(headers);
        }
        return writePacked(headers, new MemorySegment[] {cellRowHeaderObject});
    }

    /// Implements `ITableItemProvider::GetColumnHeaderItems`.
    private int getColumnHeaderItems(MemorySegment self, MemorySegment headers) {
        if (cellColumnHeaderObject.address() == 0L) {
            return writeEmptyPacked(headers);
        }
        return writePacked(headers, new MemorySegment[] {cellColumnHeaderObject});
    }

    /// Implements `ITableProvider::GetRowHeaders`.
    private int getRowHeaders(MemorySegment self, MemorySegment headers) {
        return writePacked(headers, rowHeaderObjects);
    }

    /// Implements `ITableProvider::GetColumnHeaders`.
    private int getColumnHeaders(MemorySegment self, MemorySegment headers) {
        return writePacked(headers, columnHeaderObjects);
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
        value.set(ValueLayout.JAVA_INT, 0L, node.readOnly() ? 1 : 0);
        return S_OK;
    }

    /// Implements `IRangeValueProvider::get_Maximum`.
    private int getRangeMaximum(MemorySegment self, MemorySegment value) {
        return writeDouble(value, RANGE_MAXIMUM);
    }

    /// Implements `IRangeValueProvider::get_Minimum`.
    private int getRangeMinimum(MemorySegment self, MemorySegment value) {
        return writeDouble(value, RANGE_MINIMUM);
    }

    /// Implements `ITextProvider::RangeFromPoint` for a point inside the node bounds.
    private int rangeFromPoint(MemorySegment self, double x, double y, MemorySegment range) {
        if (node.bounds().contains((float) x, (float) y)) {
            return documentRange(self, range);
        }
        return emptyRange(self, range);
    }

    /// Implements `ITextProvider::GetVisibleRanges` as a one-element oleaut32 `SAFEARRAY`.
    private int getVisibleRanges(MemorySegment self, MemorySegment range) {
        if (range.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment bound = arena.allocate(Win32Layouts.SAFEARRAYBOUND);
        bound.set(ValueLayout.JAVA_INT, Win32Layouts.SAFEARRAYBOUND_C_ELEMENTS_OFFSET, 1);
        bound.set(ValueLayout.JAVA_INT, Win32Layouts.SAFEARRAYBOUND_L_LBOUND_OFFSET, 0);
        MemorySegment created = bindings.safeArrayCreate(VT_UNKNOWN, 1, bound);
        if (created.address() == 0L) {
            return E_OUTOFMEMORY;
        }
        MemorySegment indices = arena.allocate(ValueLayout.JAVA_INT);
        indices.set(ValueLayout.JAVA_INT, 0L, 0);
        int put = bindings.safeArrayPutElement(created, indices, textRangeObject);
        if (put < 0) {
            bindings.safeArrayDestroy(created);
            return put;
        }
        range.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, created);
        return S_OK;
    }

    /// Implements `ITextProvider::RangeFromChild` as the document range.
    private int rangeFromChild(MemorySegment self, MemorySegment range) {
        return documentRange(self, range);
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

    /// Returns the live document when a layout node is bound, otherwise the snapshot label.
    private String documentText() {
        return liveNode != null ? liveNode.label() : node.label();
    }

    /// Expands a full-document range when [`LayoutNode#setLabel(String)`] changes the length.
    private void syncDocumentRange() {
        int length = documentText().length();
        if (rangeStart == 0 && rangeEnd == previousDocumentLength) {
            rangeEnd = length;
        } else {
            rangeStart = Math.min(rangeStart, length);
            rangeEnd = Math.min(rangeEnd, length);
            if (rangeStart > rangeEnd) {
                rangeStart = rangeEnd;
            }
        }
        previousDocumentLength = length;
    }

    /// Implements `ITextRangeProvider::ExpandToEnclosingUnit`.
    private int expandToEnclosingUnit(MemorySegment self, int unit) {
        syncDocumentRange();
        int length = documentText().length();
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
        syncDocumentRange();
        String document = documentText();
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
        syncDocumentRange();
        int length = documentText().length();
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
        if (attributeId == UIA_ANIMATION_STYLE_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_BACKGROUND_COLOR_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_BACKGROUND_COLOR);
        } else if (attributeId == UIA_BULLET_STYLE_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_CAP_STYLE_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_CULTURE_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, cultureId(node.locale()));
        } else if (attributeId == UIA_FONT_NAME_ATTRIBUTE_ID) {
            writeBstrVariant(variant, ATTRIBUTE_FONT_NAME);
        } else if (attributeId == UIA_FONT_SIZE_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_FONT_SIZE);
        } else if (attributeId == UIA_FONT_WEIGHT_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_FONT_WEIGHT);
        } else if (attributeId == UIA_FOREGROUND_COLOR_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_FOREGROUND_COLOR);
        } else if (attributeId == UIA_IS_HIDDEN_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_HORIZONTAL_TEXT_ALIGNMENT_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_IS_ITALIC_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_IS_READ_ONLY_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.readOnly() ? 1 : 0);
        } else if (attributeId == UIA_INDENTATION_FIRST_LINE_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_INDENT_FIRST_LINE);
        } else if (attributeId == UIA_INDENTATION_LEADING_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_INDENT_LEADING);
        } else if (attributeId == UIA_INDENTATION_TRAILING_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_INDENT_TRAILING);
        } else if (attributeId == UIA_IS_SUBSCRIPT_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_IS_SUPERSCRIPT_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_MARGIN_BOTTOM_ATTRIBUTE_ID
                || attributeId == UIA_MARGIN_LEADING_ATTRIBUTE_ID
                || attributeId == UIA_MARGIN_TOP_ATTRIBUTE_ID
                || attributeId == UIA_MARGIN_TRAILING_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_MARGIN);
        } else if (attributeId == UIA_OUTLINE_STYLES_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_OVERLINE_COLOR_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_FOREGROUND_COLOR);
        } else if (attributeId == UIA_OVERLINE_STYLE_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_STRIKETHROUGH_COLOR_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_FOREGROUND_COLOR);
        } else if (attributeId == UIA_STRIKETHROUGH_STYLE_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_TABS_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_TEXT_FLOW_DIRECTIONS_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_UNDERLINE_COLOR_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_FOREGROUND_COLOR);
        } else if (attributeId == UIA_UNDERLINE_STYLE_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_ANNOTATION_TYPES_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, ANNOTATION_TYPE_COMMENT);
        } else if (attributeId == UIA_ANNOTATION_OBJECTS_ATTRIBUTE_ID) {
            writeUnknownVariant(variant, foundItemObject);
        } else if (attributeId == UIA_STYLE_NAME_ATTRIBUTE_ID) {
            writeBstrVariant(variant, "Normal");
        } else if (attributeId == UIA_STYLE_ID_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, STYLE_ID_NORMAL);
        } else if (attributeId == UIA_LINK_ATTRIBUTE_ID) {
            writeBstrVariant(variant, "");
        } else if (attributeId == UIA_IS_ACTIVE_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, node.focused() ? 1 : 0);
        } else if (attributeId == UIA_SELECTION_ACTIVE_END_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_CARET_POSITION_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_CARET_BIDI_MODE_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, 0);
        } else if (attributeId == UIA_LINE_SPACING_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_LINE_SPACING);
        } else if (attributeId == UIA_BEFORE_PARAGRAPH_SPACING_ATTRIBUTE_ID
                || attributeId == UIA_AFTER_PARAGRAPH_SPACING_ATTRIBUTE_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_R8);
            variant.set(ValueLayout.JAVA_DOUBLE, Win32Layouts.VARIANT_L_VAL_OFFSET, ATTRIBUTE_PARAGRAPH_SPACING);
        } else if (attributeId == UIA_SAY_AS_INTERPRET_AS_ATTRIBUTE_ID) {
            writeBstrVariant(variant, "");
        }
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
        syncDocumentRange();
        int length = documentText().length();
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
        syncDocumentRange();
        String document = documentText();
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

    /// Decodes a NUL-terminated UTF-16 string from a COM out-parameter pointer.
    private static String decodeUtf16(MemorySegment pointer) {
        if (pointer.address() == 0L) {
            return "";
        }
        MemorySegment readable = pointer.byteSize() < 256L ? pointer.reinterpret(256) : pointer;
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < 64; index++) {
            char unit = readable.getAtIndex(ValueLayout.JAVA_CHAR, index);
            if (unit == 0) {
                break;
            }
            text.append(unit);
        }
        return text.toString();
    }

    /// Writes a packed `VT_ARRAY | VT_R8` bounding rectangle.
    ///
    /// @param variant the VARIANT storage
    /// @param bounds the root-relative bounds
    private void writeBoundingRectangle(MemorySegment variant, org.glavo.himari.layout.LayoutRect bounds) {
        MemorySegment rect = arena.allocate(32);
        rect.set(ValueLayout.JAVA_DOUBLE, 0L, bounds.x());
        rect.set(ValueLayout.JAVA_DOUBLE, 8L, bounds.y());
        rect.set(ValueLayout.JAVA_DOUBLE, 16L, bounds.width());
        rect.set(ValueLayout.JAVA_DOUBLE, 24L, bounds.height());
        variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) (VT_ARRAY | VT_R8));
        variant.set(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET, rect);
    }

    /// Writes a packed `VT_ARRAY | VT_R8` clickable point at the bounds center.
    ///
    /// @param variant the VARIANT storage
    /// @param bounds the root-relative bounds
    private void writeClickablePoint(MemorySegment variant, org.glavo.himari.layout.LayoutRect bounds) {
        MemorySegment point = arena.allocate(16);
        point.set(ValueLayout.JAVA_DOUBLE, 0L, bounds.x() + bounds.width() * 0.5);
        point.set(ValueLayout.JAVA_DOUBLE, 8L, bounds.y() + bounds.height() * 0.5);
        variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) (VT_ARRAY | VT_R8));
        variant.set(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET, point);
    }

    /// Writes a packed `VT_ARRAY | VT_R8` size from the bounds extent.
    ///
    /// @param variant the VARIANT storage
    /// @param bounds the root-relative bounds
    private void writeSize(MemorySegment variant, org.glavo.himari.layout.LayoutRect bounds) {
        MemorySegment size = arena.allocate(16);
        size.set(ValueLayout.JAVA_DOUBLE, 0L, bounds.width());
        size.set(ValueLayout.JAVA_DOUBLE, 8L, bounds.height());
        variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) (VT_ARRAY | VT_R8));
        variant.set(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET, size);
    }

    /// Maps [`SemanticsNode#level()`] onto a UIA heading-level constant.
    ///
    /// @param node the snapshot
    /// @return `HeadingLevel_None` or `HeadingLevel_1` through `HeadingLevel_9`
    private static int headingLevelId(SemanticsNode node) {
        int level = node.level();
        if (level < 1 || level > 9) {
            return HEADING_LEVEL_NONE;
        }
        return HEADING_LEVEL_NONE + level;
    }

    /// Writes a `VT_BSTR` VARIANT from `text`.
    ///
    /// @param variant the VARIANT storage
    /// @param text the UTF-16 payload
    private void writeBstrVariant(MemorySegment variant, String text) {
        variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_BSTR);
        MemorySegment chars = arena.allocate((text.length() + 1L) * 2L);
        byte[] utf16 = text.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment.copy(utf16, 0, chars, ValueLayout.JAVA_BYTE, 0L, utf16.length);
        variant.set(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET, chars);
    }

    /// Writes a `VT_UNKNOWN` VARIANT that aliases an existing COM object.
    ///
    /// @param variant the VARIANT storage
    /// @param object the IUnknown pointer
    private void writeUnknownVariant(MemorySegment variant, MemorySegment object) {
        variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, VT_UNKNOWN);
        variant.set(ValueLayout.ADDRESS, Win32Layouts.VARIANT_L_VAL_OFFSET, object);
        addRef(object);
    }

    /// Writes a UTF-16 C string through a `BSTR` out-parameter.
    private int writeUtf16(MemorySegment out, String text) {
        if (out.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment chars = arena.allocate((text.length() + 1L) * 2L);
        byte[] utf16 = text.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment.copy(utf16, 0, chars, ValueLayout.JAVA_BYTE, 0L, utf16.length);
        out.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, chars);
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

    /// Writes a count-prefixed COM array of header provider pointers.
    private int writePacked(MemorySegment output, MemorySegment[] objects) {
        if (objects.length == 0) {
            return writeEmptyPacked(output);
        }
        if (output.address() == 0L) {
            return E_POINTER;
        }
        long pointerSize = ValueLayout.ADDRESS.byteSize();
        MemorySegment block = arena.allocate(8 + objects.length * pointerSize);
        block.set(ValueLayout.JAVA_INT, 0L, objects.length);
        block.set(ValueLayout.JAVA_INT, 4L, 0);
        for (int index = 0; index < objects.length; index++) {
            block.set(ValueLayout.ADDRESS, 8 + index * pointerSize, objects[index]);
        }
        output.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, block);
        return S_OK;
    }

    /// Allocates one `IRawElementProviderSimple` header object per name.
    private MemorySegment[] allocateHeaderObjects(String[] names, CallbackFailureQueue failures) {
        MemorySegment[] objects = new MemorySegment[names.length];
        for (int index = 0; index < names.length; index++) {
            objects[index] = allocateHeaderObject(names[index], failures);
        }
        return objects;
    }

    /// Allocates one header that answers `Name` and `ControlType` through generated vtable slots.
    private MemorySegment allocateHeaderObject(String name, CallbackFailureQueue failures) {
        MemorySegment object = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment vtable = arena.allocate(ValueLayout.ADDRESS, 7);
        object.set(ValueLayout.ADDRESS, 0L, vtable);
        vtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryHeader, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        vtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createIrawElementProviderGetProviderOptionsStub(this::getProviderOptions, failures, arena)
        );
        vtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIrawElementProviderGetPatternProviderStub(this::getHeaderPatternProvider, failures, arena)
        );
        vtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIrawElementProviderGetPropertyValueStub(this::getHeaderPropertyValue, failures, arena)
        );
        vtable.setAtIndex(
                ValueLayout.ADDRESS,
                6L,
                bindings.createIrawElementProviderGetHostRawElementProviderStub(this::getHeaderHost, failures, arena)
        );
        headerInfos.put(object.address(), new HeaderInfo(name, UIA_HEADER_ITEM_CONTROL_TYPE_ID));
        return object;
    }

    /// Implements header-object `IUnknown::QueryInterface` for `IRawElementProviderSimple`.
    private int queryHeader(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IRAW_ELEMENT_PROVIDER_SIMPLE, self);
    }

    /// Implements header `GetPatternProvider` as no pattern.
    private int getHeaderPatternProvider(MemorySegment self, int patternId, MemorySegment provider) {
        if (provider.address() == 0L) {
            return E_POINTER;
        }
        provider.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return S_OK;
    }

    /// Implements header `get_HostRawElementProvider` as no host.
    private int getHeaderHost(MemorySegment self, MemorySegment host) {
        if (host.address() == 0L) {
            return E_POINTER;
        }
        host.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return S_OK;
    }

    /// Implements header `GetPropertyValue` for `Name` and `ControlType`.
    private int getHeaderPropertyValue(MemorySegment self, int propertyId, MemorySegment variant) {
        HeaderInfo info = headerInfos.get(self.address());
        if (info == null || variant.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment out = variant.byteSize() >= Win32Layouts.VARIANT.byteSize()
                ? variant
                : variant.reinterpret(Win32Layouts.VARIANT.byteSize());
        out.fill((byte) 0);
        if (propertyId == UIA_NAME_PROPERTY_ID) {
            writeBstrVariant(out, info.name());
            return S_OK;
        }
        if (propertyId == UIA_CONTROL_TYPE_PROPERTY_ID) {
            out.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            out.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, info.controlType());
            return S_OK;
        }
        if (propertyId == UIA_LOCALIZED_CONTROL_TYPE_PROPERTY_ID) {
            writeBstrVariant(out, "HeaderItem");
            return S_OK;
        }
        return S_OK;
    }

    /// Stores one header provider's published name and control type.
    ///
    /// @param name the accessible name
    /// @param controlType the UIA control-type identifier
    private record HeaderInfo(String name, int controlType) {
        /// Validates the header.
        private HeaderInfo {
            Objects.requireNonNull(name, "name");
        }
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

    /// Maps a BCP-47 locale onto a Win32 LCID.
    ///
    /// @param locale the BCP-47 tag, empty when unspecified
    /// @return the LCID, or `0` when unspecified
    static int cultureId(String locale) {
        if (locale.isEmpty()) {
            return 0;
        }
        if (locale.equalsIgnoreCase("zh-CN")) {
            return 0x0804;
        }
        return LCID_EN_US;
    }

    /// Maps a role onto `OrientationType`.
    ///
    /// @param node the semantics node
    /// @return `None`, `Horizontal`, or `Vertical`
    private static int orientationId(SemanticsNode node) {
        return switch (node.role()) {
            case SLIDER, PROGRESS, SPLIT_PANE -> ORIENTATION_HORIZONTAL;
            case SCROLLBAR -> ORIENTATION_VERTICAL;
            default -> ORIENTATION_NONE;
        };
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
            case TABLE_COLUMN_HEADER, TABLE_ROW_HEADER -> UIA_HEADER_ITEM_CONTROL_TYPE_ID;
            case TEXT -> 50020;
            case IMAGE -> 50006;
            case CANVAS -> 50033;
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
