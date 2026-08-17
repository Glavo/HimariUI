package org.glavo.himari.platform.windows;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.bootstrap.BootstrapCounterPane;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsTextRange;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/// Writes Windows IME session and UIA projection evidence.
@NotNullByDefault
public final class WindowsImeA11yConformance {
    /// Prevents instantiation.
    private WindowsImeA11yConformance() {
    }

    /// Exercises the shipped IME session and UIA projection.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        WindowsImeSession ime = new WindowsImeSession();
        ime.setSurroundingText("", 0);
        ime.setCandidateRectangle(4.0f, 8.0f, 16.0f, 12.0f);
        ime.updateComposition("ni");
        String committed = ime.commit();
        if (!"ni".equals(committed) || !ime.committed() || !"ni".equals(ime.surroundingText())) {
            throw new IllegalStateException("IME composition did not commit");
        }
        if (!"ni".equals(ime.reconvert()) || ime.candidateWidth() != 16.0f) {
            throw new IllegalStateException("IME reconversion or candidate rectangle failed");
        }
        ime.updateComposition("hao");
        if (!"hao".equals(ime.reject()) || ime.composition() != null || !"hao".equals(ime.lastRejected())
                || !"ni".equals(ime.surroundingText())) {
            throw new IllegalStateException("IME rejection changed surrounding text or lost the fragment");
        }
        LayoutTree tree = new LayoutTree();
        tree.setRoot(BootstrapCounterPane.create(tree, new AtomicInteger()));
        tree.measure(Constraints.loose(200.0f, 200.0f));
        tree.place();
        List<WindowsAutomationNode> nodes = WindowsAutomationBridge.inspect(tree.semantics());
        boolean invoke = nodes.stream().anyMatch(WindowsAutomationNode::invokeSupported);
        boolean increment = nodes.stream().anyMatch(node -> node.name().equals("Increment"));
        if (!invoke || !increment) {
            throw new IllegalStateException("UIA projection omitted the increment control");
        }
        long activateId = tree.semantics().nodeWith(SemanticsAction.ACTIVATE).id();
        long invokeId = nodes.stream()
                .filter(WindowsAutomationNode::invokeSupported)
                .findFirst()
                .orElseThrow()
                .id();
        if (activateId != invokeId) {
            throw new IllegalStateException("UIA invoke target does not match semantics");
        }
        LayoutTree valueTree = new LayoutTree();
        LayoutFactory factory = new LayoutFactory(valueTree);
        LayoutNode toggle = factory.leaf(
                "toggle",
                new Size(48.0f, 24.0f),
                List.of(),
                true,
                SemanticsRole.TOGGLE,
                "Muted",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        toggle.setSelected(false);
        LayoutNode slider = factory.leaf(
                "slider",
                new Size(160.0f, 24.0f),
                List.of(),
                true,
                SemanticsRole.SLIDER,
                "Volume",
                java.util.Set.of(SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                null
        );
        slider.setRangeValue(3.0);
        LayoutNode status = factory.leaf(
                "status",
                new Size(120.0f, 20.0f),
                List.of(),
                false,
                SemanticsRole.STATUS,
                "Saved",
                java.util.Set.of(),
                null
        );
        status.setLiveRegion(SemanticsLiveRegion.POLITE);
        LayoutNode field = factory.leaf(
                "field",
                new Size(160.0f, 24.0f),
                List.of(),
                true,
                SemanticsRole.TEXT_FIELD,
                "hello",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        field.setTextRange(new SemanticsTextRange(1, 4, 4));
        LayoutNode list = factory.leaf(
                "list",
                new Size(160.0f, 40.0f),
                List.of(),
                true,
                SemanticsRole.LIST,
                "Items",
                java.util.Set.of(SemanticsAction.SCROLL_INTO_VIEW, SemanticsAction.REALIZE),
                null
        );
        LayoutNode dialog = factory.leaf(
                "dialog",
                new Size(80.0f, 40.0f),
                List.of(),
                true,
                SemanticsRole.DIALOG,
                "Confirm",
                java.util.Set.of(),
                null
        );
        valueTree.setRoot(factory.column(
                "root",
                Alignment.START,
                List.of(),
                toggle,
                slider,
                status,
                field,
                list,
                dialog
        ));
        valueTree.measure(Constraints.loose(400.0f, 400.0f));
        valueTree.place();
        List<WindowsAutomationNode> valueNodes = WindowsAutomationBridge.inspect(valueTree.semantics());
        boolean toggleOff = valueNodes.stream().anyMatch(node -> "Off".equals(node.toggleState()));
        boolean sliderRange = valueNodes.stream().anyMatch(node ->
                node.controlType().equals("Slider") && node.rangeValue() != null && node.rangeValue() == 3.0);
        boolean livePolite = valueNodes.stream().anyMatch(node ->
                node.controlType().equals("StatusBar") && "Polite".equals(node.liveSetting()));
        boolean editRange = valueNodes.stream().anyMatch(node ->
                node.controlType().equals("Edit")
                        && node.textRange() != null
                        && node.textRange().start() == 1
                        && node.textRange().end() == 4
                        && node.textRange().caret() == 4);
        if (!toggleOff || !sliderRange || !livePolite || !editRange) {
            throw new IllegalStateException("UIA projection omitted toggle, range, live-setting, or text-range values");
        }
        boolean uiaGetPropertyValue = false;
        boolean tsfAvailable = false;
        boolean imm32Applied = false;
        boolean textStoreLock = false;
        boolean textStoreGeometry = false;
        boolean documentAttached = false;
        boolean uiaInvoke = false;
        boolean uiaToggleCom = false;
        boolean uiaRangeCom = false;
        boolean uiaLiveSetting = false;
        boolean uiaTextCom = false;
        boolean uiaScrollItemCom = false;
        boolean uiaVirtualizedItemCom = false;
        boolean uiaDockCom = false;
        boolean uiaTransformCom = false;
        boolean uiaTransform2Com = false;
        boolean uiaItemContainerCom = false;
        boolean uiaSynchronizedInputCom = false;
        boolean uiaMultipleViewCom = false;
        boolean uiaDropTargetCom = false;
        boolean uiaDragCom = false;
        boolean uiaAnnotationCom = false;
        boolean uiaTextChildCom = false;
        boolean uiaStylesCom = false;
        boolean uiaCustomNavigationCom = false;
        boolean uiaObjectModelCom = false;
        boolean uiaTextEditCom = false;
        boolean uiaSelectionCom = false;
        boolean uiaLegacyAccessibleCom = false;
        boolean uiaText2Com = false;
        boolean uiaSelection2Com = false;
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = platform.createWindow(
                    org.glavo.himari.platform.api.WindowRequest.toplevel(
                            new org.glavo.himari.platform.api.WindowConfiguration(
                                    "IME-A11Y",
                                    new org.glavo.himari.platform.api.LogicalRect(16.0, 16.0, 240.0, 160.0),
                                    true,
                                    org.glavo.himari.platform.api.WindowState.NORMAL
                            )
                    ),
                    event -> { }
            ).toCompletableFuture().get();
            platform.pump();
            window.ime().setCandidateRectangle(4.0f, 8.0f, 16.0f, 12.0f);
            imm32Applied = window.applyImeCandidate();
            window.ime().setSurroundingText("hello", 5);
            try (
                    WindowsTsfSession tsf = window.openTsf();
                    WindowsTextStore store = window.createTextStore()
            ) {
                tsfAvailable = tsf.available() && tsf.activate();
                if (tsfAvailable) {
                    store.invokeSetText(0, 5, "nihao");
                    store.invokeSetSelection(3);
                    WindowsTextStore.Selection selection = store.invokeGetSelection();
                    textStoreLock = store.invokeRequestLock(WindowsTextStore.TS_LF_READWRITE) == 0
                            && "nihao".equals(store.invokeGetText(0, -1))
                            && selection.start() == 3
                            && selection.end() == 3;
                    WindowsTextStore.ScreenExtent extent = store.invokeGetScreenExt();
                    textStoreGeometry = store.invokeGetAcpFromPoint(4, 8) == 0
                            && store.invokeGetAcpFromPoint(20, 8) == 5
                            && extent.left() == 4
                            && extent.top() == 8
                            && extent.right() == 20
                            && extent.bottom() == 20
                            && !store.invokeQueryInsertEmbedded()
                            && store.invokeGetFormattedText() < 0
                            && store.invokeRetrieveRequestedAttrs() == 0
                            && !store.invokeFindNextAttrTransition();
                    documentAttached = tsf.attach(store);
                }
            }
            SemanticsNode incrementNode = tree.semantics().nodeWith(SemanticsAction.ACTIVATE);
            try (WindowsAutomationProvider provider = window.automationProvider(incrementNode)) {
                uiaGetPropertyValue = provider.invokePropertyValue(
                        WindowsAutomationProvider.UIA_CONTROL_TYPE_PROPERTY_ID
                ) == WindowsAutomationProvider.UIA_BUTTON_CONTROL_TYPE_ID;
                uiaInvoke = provider.invokePatternProvider(WindowsAutomationProvider.UIA_INVOKE_PATTERN_ID)
                        && provider.invoke() == 1;
                uiaSynchronizedInputCom = provider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SYNCHRONIZED_INPUT_PATTERN_ID
                )
                        && provider.startListening(WindowsAutomationProvider.SYNCHRONIZED_INPUT_KEY_DOWN) == 1
                        && provider.cancelSynchronizedInput() == 1;
                uiaLegacyAccessibleCom = provider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_PATTERN_ID
                )
                        && provider.legacyChildId() == 0
                        && incrementNode.label().equals(provider.legacyName())
                        && provider.legacyRole() == WindowsAutomationProvider.ROLE_SYSTEM_PUSHBUTTON
                        && provider.invokeLegacyDefaultAction() == 2
                        && incrementNode.label().equals(provider.legacyValue())
                        && provider.legacyState() == WindowsAutomationProvider.STATE_SYSTEM_FOCUSABLE
                        && incrementNode.label().equals(provider.legacyDescription())
                        && "Press".equals(provider.legacyDefaultAction())
                        && provider.legacyKeyboardShortcut().isEmpty()
                        && incrementNode.label().equals(provider.legacyHelp())
                        && provider.invokeFragmentNavigate(WindowsAutomationProvider.NAVIGATE_DIRECTION_PARENT)
                        && !provider.invokeFragmentNavigate(1)
                        && provider.invokeFragmentSetFocus() == 1
                        && provider.invokeFragmentRoot()
                        && provider.invokeFragmentBoundingRectangle()[2] == incrementNode.bounds().width()
                        && provider.invokeFragmentRootFromPoint(incrementNode.bounds().x(), incrementNode.bounds().y())
                        && provider.invokeFragmentRootFocus()
                        && provider.invokeLegacySetValue(incrementNode.label()) == 1
                        && incrementNode.label().equals(provider.lastLegacyValue())
                        && provider.invokeFragmentRuntimeId()[1] == (int) incrementNode.id()
                        && provider.invokeEmbeddedFragmentRoots() == 0
                        && provider.invokeProviderOptions() == WindowsAutomationProvider.PROVIDER_OPTIONS_SERVER_SIDE
                        && provider.invokeHostRawElementProvider();
            }
            SemanticsNode toggleNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TOGGLE)
                    .findFirst()
                    .orElseThrow();
            SemanticsNode sliderNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.SLIDER)
                    .findFirst()
                    .orElseThrow();
            SemanticsNode statusNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.STATUS)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider toggleProvider = window.automationProvider(toggleNode)) {
                uiaToggleCom = toggleProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TOGGLE_PATTERN_ID)
                        && toggleProvider.toggle() == WindowsAutomationProvider.TOGGLE_STATE_ON;
            }
            try (WindowsAutomationProvider rangeProvider = window.automationProvider(sliderNode)) {
                uiaRangeCom = rangeProvider.invokePatternProvider(WindowsAutomationProvider.UIA_RANGE_VALUE_PATTERN_ID)
                        && rangeProvider.setRangeValue(8.0) == 8.0;
            }
            try (WindowsAutomationProvider statusProvider = window.automationProvider(statusNode)) {
                uiaLiveSetting = statusProvider.invokePropertyValue(
                        WindowsAutomationProvider.UIA_CONTROL_TYPE_PROPERTY_ID
                ) == WindowsAutomationProvider.UIA_STATUS_BAR_CONTROL_TYPE_ID
                        && statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_LIVE_SETTING_PROPERTY_ID
                        ) == WindowsAutomationProvider.LIVE_SETTING_POLITE;
                uiaAnnotationCom = statusProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_ANNOTATION_PATTERN_ID
                )
                        && statusProvider.annotationTypeId() == WindowsAutomationProvider.ANNOTATION_TYPE_COMMENT
                        && "Comment".equals(statusProvider.annotationTypeName())
                        && "Himari".equals(statusProvider.annotationAuthor())
                        && statusProvider.invokeAnnotationTarget();
                uiaStylesCom = statusProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_STYLES_PATTERN_ID
                )
                        && statusProvider.styleId() == WindowsAutomationProvider.STYLE_ID_NORMAL
                        && "Normal".equals(statusProvider.styleName());
            }
            SemanticsNode fieldNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider textProvider = window.automationProvider(fieldNode)) {
                uiaTextChildCom = textProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_TEXT_CHILD_PATTERN_ID
                )
                        && textProvider.invokeTextContainer()
                        && textProvider.invokeTextChildRange();
                uiaTextEditCom = textProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_TEXT_EDIT_PATTERN_ID
                )
                        && textProvider.invokeActiveComposition()
                        && textProvider.invokeConversionTarget();
                uiaText2Com = textProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_TEXT_PATTERN2_ID
                )
                        && textProvider.invokeCaretRange()
                        && textProvider.invokeRangeFromAnnotation();
                uiaTextCom = textProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TEXT_PATTERN_ID)
                        && textProvider.invokeDocumentRange()
                        && textProvider.invokeSupportedTextSelection()
                        == WindowsAutomationProvider.SUPPORTED_TEXT_SELECTION_SINGLE
                        && "hello".equals(textProvider.invokeGetText(-1))
                        && textProvider.invokeClone()
                        && textProvider.invokeCompareSelf()
                        && textProvider.invokeEnclosingElement()
                        && textProvider.invokeMove(WindowsAutomationProvider.TEXT_UNIT_CHARACTER, -1) == -1
                        && textProvider.invokeExpandToEnclosingUnit(
                                WindowsAutomationProvider.TEXT_UNIT_CHARACTER
                        ).end() > 0
                        && textProvider.invokeGetBoundingRectangles().length == 4
                        && textProvider.invokeFindText("ell", false)
                        && "ell".equals(textProvider.invokeGetText(-1))
                        && textProvider.invokeCompareEndpoints(
                                WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_START,
                                WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_END
                        ) < 0
                        && textProvider.invokeMoveEndpointByUnit(
                                WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_END,
                                WindowsAutomationProvider.TEXT_UNIT_CHARACTER,
                                1
                        ) == 1
                        && !textProvider.invokeGetSelection();
                textProvider.invokeSelect();
                uiaTextCom = uiaTextCom
                        && textProvider.invokeGetSelection()
                        && !textProvider.invokeFindAttribute(40013)
                        && textProvider.invokeGetAttributeValue(40013) == 0
                        && textProvider.invokeScrollIntoView(true)
                        && textProvider.invokeGetChildren() == 0
                        && textProvider.invokeShowContextMenu() == 1
                        && textProvider.invokeSimpleShowContextMenu() == 1;
                textProvider.invokeRemoveFromSelection();
                uiaTextCom = uiaTextCom && !textProvider.invokeGetSelection();
                textProvider.invokeAddToSelection();
                uiaTextCom = uiaTextCom && textProvider.invokeGetSelection();
            }
            SemanticsNode listNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.LIST)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider scrollItemProvider = window.automationProvider(listNode)) {
                uiaScrollItemCom = scrollItemProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SCROLL_ITEM_PATTERN_ID
                ) && scrollItemProvider.invokeScrollItem() == 1;
                uiaVirtualizedItemCom = scrollItemProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_VIRTUALIZED_ITEM_PATTERN_ID
                ) && scrollItemProvider.invokeVirtualizedItem() == 1;
                uiaItemContainerCom = scrollItemProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_ITEM_CONTAINER_PATTERN_ID
                ) && scrollItemProvider.invokeFindItemByProperty("Items");
                uiaMultipleViewCom = scrollItemProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_MULTIPLE_VIEW_PATTERN_ID
                )
                        && scrollItemProvider.currentView() == 1
                        && "List".equals(scrollItemProvider.viewName(1))
                        && scrollItemProvider.setCurrentView(2) == 2;
                uiaDropTargetCom = scrollItemProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_DROP_TARGET_PATTERN_ID
                ) && "move".equals(scrollItemProvider.dropTargetEffect());
                uiaDragCom = scrollItemProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_DRAG_PATTERN_ID
                )
                        && !scrollItemProvider.isGrabbed()
                        && "copy".equals(scrollItemProvider.dropEffect());
                uiaSelectionCom = scrollItemProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SELECTION_PATTERN_ID
                )
                        && scrollItemProvider.canSelectMultiple()
                        && !scrollItemProvider.isSelectionRequired();
                uiaSelection2Com = scrollItemProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SELECTION_PATTERN2_ID
                )
                        && scrollItemProvider.selectionItemCount() == 1
                        && scrollItemProvider.invokeCurrentSelectedItem()
                        && scrollItemProvider.invokeFirstSelectedItem()
                        && scrollItemProvider.invokeLastSelectedItem();
            }
            SemanticsNode dialogNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.DIALOG)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider dockProvider = window.automationProvider(dialogNode)) {
                uiaDockCom = dockProvider.invokePatternProvider(WindowsAutomationProvider.UIA_DOCK_PATTERN_ID)
                        && dockProvider.setDockPosition(WindowsAutomationProvider.DOCK_POSITION_TOP)
                                == WindowsAutomationProvider.DOCK_POSITION_TOP;
                uiaTransformCom = dockProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TRANSFORM_PATTERN_ID)
                        && dockProvider.canMove()
                        && dockProvider.moveTransform(8.0, 16.0) == 8.0
                        && dockProvider.resizeTransform(64.0, 32.0) == 64.0
                        && dockProvider.rotateTransform(9.0) == 9.0;
                uiaTransform2Com = dockProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_TRANSFORM_PATTERN2_ID
                )
                        && dockProvider.canZoom()
                        && dockProvider.zoomTransform(1.5) == 1.5
                        && dockProvider.zoomLevel() == 1.5
                        && dockProvider.zoomByUnit(WindowsAutomationProvider.ZOOM_UNIT_LARGE_INCREMENT) == 2.5
                        && dockProvider.zoomLevel() == 2.5
                        && dockProvider.zoomMinimum() == 0.5
                        && dockProvider.zoomMaximum() == 4.0;
                uiaCustomNavigationCom = dockProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_CUSTOM_NAVIGATION_PATTERN_ID
                )
                        && dockProvider.invokeNavigate(WindowsAutomationProvider.NAVIGATE_DIRECTION_PARENT);
                uiaObjectModelCom = dockProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_OBJECT_MODEL_PATTERN_ID
                )
                        && dockProvider.invokeObjectModel();
            }
            if (!textStoreLock || !textStoreGeometry || !documentAttached) {
                throw new IllegalStateException("ITextStoreACP lock, geometry, or TSF document attach failed");
            }
            if (!uiaInvoke
                    || !uiaToggleCom
                    || !uiaRangeCom
                    || !uiaLiveSetting
                    || !uiaTextCom
                    || !uiaScrollItemCom
                    || !uiaVirtualizedItemCom
                    || !uiaDockCom
                    || !uiaTransformCom
                    || !uiaTransform2Com
                    || !uiaItemContainerCom
                    || !uiaSynchronizedInputCom
                    || !uiaMultipleViewCom
                    || !uiaDropTargetCom
                    || !uiaDragCom
                    || !uiaAnnotationCom
                    || !uiaTextChildCom
                    || !uiaStylesCom
                    || !uiaCustomNavigationCom
                    || !uiaObjectModelCom
                    || !uiaTextEditCom
                    || !uiaSelectionCom
                    || !uiaLegacyAccessibleCom
                    || !uiaText2Com
                    || !uiaSelection2Com) {
                throw new IllegalStateException(
                        "UIA Invoke/Toggle/Range/LiveSetting/Text/ScrollItem/VirtualizedItem/Dock/Transform/ItemContainer COM properties failed"
                );
            }
            window.closeAsync().toCompletableFuture().get();
            platform.pump();
        } finally {
            platform.close();
        }
        if (!tsfAvailable) {
            throw new IllegalStateException("ITfThreadMgr was not created or activated");
        }
        if (!uiaGetPropertyValue) {
            throw new IllegalStateException("IRawElementProviderSimple::GetPropertyValue missed Button");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m9-windows-ime-a11y",
                          "workPackage": "A11Y-CORE-001",
                          "status": "passed",
                          "imeCommitted": true,
                          "imeReconvert": true,
                          "imeRejected": true,
                          "uiaInvoke": true,
                          "uiaBounds": true,
                          "uiaToggle": true,
                          "uiaRange": true,
                          "uiaGetPropertyValue": %s,
                          "uiaInvokeCom": %s,
                          "uiaToggleCom": %s,
                          "uiaRangeCom": %s,
                          "uiaLiveSetting": %s,
                          "uiaTextCom": %s,
                          "uiaScrollItemCom": %s,
                          "uiaVirtualizedItemCom": %s,
                          "uiaDockCom": %s,
                          "uiaTransformCom": %s,
                          "uiaTransform2Com": %s,
                          "uiaItemContainerCom": %s,
                          "uiaSynchronizedInputCom": %s,
                          "uiaMultipleViewCom": %s,
                          "uiaDropTargetCom": %s,
                          "uiaDragCom": %s,
                          "uiaAnnotationCom": %s,
                          "uiaTextChildCom": %s,
                          "uiaStylesCom": %s,
                          "uiaCustomNavigationCom": %s,
                          "uiaObjectModelCom": %s,
                          "uiaTextEditCom": %s,
                          "uiaSelectionCom": %s,
                          "uiaLegacyAccessibleCom": %s,
                          "uiaText2Com": %s,
                          "uiaSelection2Com": %s,
                          "uiaTextRange": true,
                          "tsfThreadMgr": %s,
                          "textStoreAcp": %s,
                          "textStoreGeometry": %s,
                          "documentAttached": %s,
                          "imm32Candidate": %s,
                          "nodeCount": %d
                        }
                        """.formatted(
                        uiaGetPropertyValue,
                        uiaInvoke,
                        uiaToggleCom,
                        uiaRangeCom,
                        uiaLiveSetting,
                        uiaTextCom,
                        uiaScrollItemCom,
                        uiaVirtualizedItemCom,
                        uiaDockCom,
                        uiaTransformCom,
                        uiaTransform2Com,
                        uiaItemContainerCom,
                        uiaSynchronizedInputCom,
                        uiaMultipleViewCom,
                        uiaDropTargetCom,
                        uiaDragCom,
                        uiaAnnotationCom,
                        uiaTextChildCom,
                        uiaStylesCom,
                        uiaCustomNavigationCom,
                        uiaObjectModelCom,
                        uiaTextEditCom,
                        uiaSelectionCom,
                        uiaLegacyAccessibleCom,
                        uiaText2Com,
                        uiaSelection2Com,
                        tsfAvailable,
                        textStoreLock,
                        textStoreGeometry,
                        documentAttached,
                        imm32Applied,
                        nodes.size()
                ),
                StandardCharsets.UTF_8
        );
    }
}
