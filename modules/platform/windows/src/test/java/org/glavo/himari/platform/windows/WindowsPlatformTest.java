package org.glavo.himari.platform.windows;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.bootstrap.BootstrapCounterPane;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerDeviceKind;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsGrid;
import org.glavo.himari.layout.semantics.SemanticsGridItem;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsScroll;
import org.glavo.himari.layout.semantics.SemanticsTextRange;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.SurfaceRole;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowEvent;
import org.glavo.himari.platform.api.WindowEventType;
import org.glavo.himari.platform.api.WindowLifecycle;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowSnapshot;
import org.glavo.himari.platform.api.WindowState;
import org.glavo.himari.rhi.vulkan.VulkanDevice;
import org.glavo.himari.rhi.vulkan.VulkanPresentation;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the shipped Windows platform session, WndProc input, IME session, and UIA projection.
@NotNullByDefault
@EnabledOnOs(OS.WINDOWS)
final class WindowsPlatformTest {
    /// Opens a real HWND, reconfigures it, and closes it through the production backend.
    @Test
    void createsConfiguresAndClosesWindow() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            List<WindowEventType> types = new ArrayList<>();
            WindowConfiguration initial = new WindowConfiguration(
                    "HimariUI Windows",
                    new LogicalRect(32.0, 32.0, 320.0, 240.0),
                    false,
                    WindowState.NORMAL
            );
            WindowsWindow window = platform.createWindow(
                    WindowRequest.toplevel(initial),
                    event -> types.add(event.type())
            ).toCompletableFuture().get();
            platform.pump();
            assertEquals(WindowLifecycle.OPEN, window.snapshot().lifecycle());
            assertTrue(window.nativeHandle().address() != 0L);
            assertTrue(window.dpi() >= 96);
            assertTrue(window.snapshot().scaleFactor() > 0.0);
            assertTrue(window.snapshot().surfaceSize().width() > 0);
            WindowConfiguration next = new WindowConfiguration(
                    "HimariUI Windows",
                    new LogicalRect(40.0, 40.0, 400.0, 300.0),
                    true,
                    WindowState.NORMAL
            );
            WindowSnapshot configured = window.configure(next).toCompletableFuture().get();
            platform.pump();
            assertTrue(configured.effectivelyVisible());
            assertEquals(400.0, configured.effectiveFrame().width());
            window.closeAsync().toCompletableFuture().get();
            platform.pump();
            assertTrue(window.isClosed());
            assertTrue(types.contains(WindowEventType.CREATED));
            assertTrue(types.contains(WindowEventType.CONFIGURATION_CHANGED));
            assertTrue(types.contains(WindowEventType.CLOSED));
            assertFalse(platform.displayTopology().displays().isEmpty());
            assertTrue(platform.displayTopology().displays().getFirst()
                    .colorCapabilities().description().presentationModes()
                    .contains(org.glavo.himari.platform.api.PresentationMode.SDR));
            assertTrue(platform.displayTopology().displays().getFirst().physicalSize().width() > 0);
        } finally {
            platform.close();
        }
    }

    /// Keeps a second HWND alive after the first is destroyed.
    @Test
    void closesOneWindowWithoutQuittingTheSession() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow first = openToplevel(platform, "First", 16.0, 16.0);
            WindowsWindow second = openToplevel(platform, "Second", 80.0, 80.0);
            platform.pump();
            assertEquals(2, platform.openWindowCount());
            first.closeAsync().toCompletableFuture().get();
            platform.pump();
            assertTrue(first.isClosed());
            assertFalse(second.isClosed());
            assertEquals(1, platform.openWindowCount());
            assertEquals(WindowLifecycle.OPEN, second.snapshot().lifecycle());
            second.closeAsync().toCompletableFuture().get();
            platform.pump();
            assertEquals(0, platform.openWindowCount());
        } finally {
            platform.close();
        }
    }

    /// Creates an owner-relative popup and closes it when the owner closes.
    @Test
    void closesOwnedPopupWithParent() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow owner = openToplevel(platform, "Owner", 24.0, 24.0);
            WindowsWindow popup = platform.createWindow(
                    WindowRequest.popup(owner.id(), new WindowConfiguration(
                            "Popup",
                            new LogicalRect(40.0, 40.0, 120.0, 80.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> { }
            ).toCompletableFuture().get();
            platform.pump();
            assertEquals(SurfaceRole.POPUP, popup.snapshot().role());
            assertEquals(owner.id(), popup.snapshot().ownerId());
            owner.closeAsync().toCompletableFuture().get();
            platform.pump();
            assertTrue(owner.isClosed());
            assertTrue(popup.isClosed());
        } finally {
            platform.close();
        }
    }

    /// Blits a software RGBA frame into a live HWND through generated GDI bindings.
    @Test
    void presentsSoftwareRgbaThroughGdi() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Present", 40.0, 40.0);
            platform.pump();
            int width = 16;
            int height = 8;
            byte[] rgba = new byte[width * height * 4];
            for (int pixel = 0; pixel < rgba.length; pixel += 4) {
                rgba[pixel] = (byte) 0x20;
                rgba[pixel + 1] = (byte) 0x40;
                rgba[pixel + 2] = (byte) 0x80;
                rgba[pixel + 3] = (byte) 0xFF;
            }
            int scanlines = window.presentSdrRgba(java.lang.foreign.MemorySegment.ofArray(rgba), width, height);
            platform.pump();
            assertEquals(height, scanlines);
        } finally {
            platform.close();
        }
    }

    /// Delivers pointer, key, and character input through the production WndProc.
    @Test
    void deliversPostedInputThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Input", 32.0, 32.0);
            platform.pump();
            window.postPointer(PointerEventType.DOWN, 12, 18);
            window.postPointer(PointerEventType.UP, 12, 18);
            window.postVirtualKey(true, 0x0D);
            window.postVirtualKey(true, 0x1B);
            window.postChar('n');
            window.postChar('i');
            platform.pump();
            List<PointerEvent> pointers = window.takePointerEvents();
            assertEquals(2, pointers.size());
            assertEquals(PointerEventType.DOWN, pointers.getFirst().type());
            assertEquals(12.0f, pointers.getFirst().x());
            assertEquals(18.0f, pointers.getFirst().y());
            assertEquals(PointerDeviceKind.MOUSE, pointers.getFirst().device());
            assertEquals(PointerEventType.UP, pointers.get(1).type());
            List<org.glavo.himari.layout.input.KeyEvent> keys = window.takeKeyEvents();
            assertEquals(2, keys.size());
            assertEquals(LogicalKey.ENTER, keys.get(0).key());
            assertEquals(LogicalKey.ESCAPE, keys.get(1).key());
            assertEquals("ni", window.ime().surroundingText());
            assertTrue(window.ime().committed());
        } finally {
            platform.close();
        }
    }

    /// Classifies `GetPointerType` results and queries the generated binding.
    @Test
    void classifiesPenAndQueriesGetPointerType() throws Exception {
        assertEquals(PointerDeviceKind.PEN, WindowsNativeWindow.deviceKindFromPointerType(WindowsNativeWindow.PT_PEN));
        assertEquals(PointerDeviceKind.MOUSE, WindowsNativeWindow.deviceKindFromPointerType(WindowsNativeWindow.PT_MOUSE));
        assertEquals(PointerDeviceKind.TOUCH, WindowsNativeWindow.deviceKindFromPointerType(WindowsNativeWindow.PT_TOUCH));
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "PointerType", 32.0, 32.0);
            platform.pump();
            assertEquals(0, window.queryPointerType(0x00FFFFFF));
        } finally {
            platform.close();
        }
    }

    /// Delivers `WM_POINTER*` through the production WndProc as touch events.
    @Test
    void deliversPostedTouchThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Touch", 32.0, 32.0);
            platform.pump();
            window.postPointer(PointerEventType.DOWN, 20, 24, PointerDeviceKind.TOUCH);
            window.postPointer(PointerEventType.UP, 20, 24, PointerDeviceKind.TOUCH);
            platform.pump();
            List<PointerEvent> pointers = window.takePointerEvents();
            assertEquals(2, pointers.size());
            assertEquals(PointerEventType.DOWN, pointers.getFirst().type());
            assertEquals(PointerDeviceKind.TOUCH, pointers.getFirst().device());
            assertEquals(20.0f, pointers.getFirst().x());
            assertEquals(24.0f, pointers.getFirst().y());
            assertEquals(PointerDeviceKind.TOUCH, pointers.get(1).device());
        } finally {
            platform.close();
        }
    }

    /// Hosts an owner-relative popup HWND and treats `WM_CLOSE` as a dismiss.
    @Test
    void popupHostReportsCloseAsDismiss() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow owner = openToplevel(platform, "PopupOwner", 24.0, 24.0);
            platform.pump();
            AtomicBoolean dismissed = new AtomicBoolean();
            WindowsWindow popup = WindowsPopupHost.show(
                    platform,
                    owner,
                    "MenuHost",
                    new LogicalRect(40.0, 40.0, 120.0, 80.0),
                    () -> dismissed.set(true)
            );
            platform.pump();
            assertEquals(SurfaceRole.POPUP, popup.snapshot().role());
            popup.nativeWindow().postMessage(0x0010, 0L, 0L);
            platform.pump();
            assertTrue(dismissed.get());
            assertFalse(popup.isClosed());
            popup.closeAsync().toCompletableFuture().get();
            platform.pump();
        } finally {
            platform.close();
        }
    }

    /// Emits `CLOSE_REQUESTED` from `WM_CLOSE` without destroying the HWND.
    @Test
    void closeMessageRequestsWithoutDestroying() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            List<WindowEventType> types = new ArrayList<>();
            WindowsWindow window = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "Close",
                            new LogicalRect(16.0, 16.0, 200.0, 120.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> types.add(event.type())
            ).toCompletableFuture().get();
            platform.pump();
            window.nativeWindow().postMessage(0x0010, 0L, 0L);
            platform.pump();
            assertTrue(types.contains(WindowEventType.CLOSE_REQUESTED));
            assertFalse(window.isClosed());
            window.closeAsync().toCompletableFuture().get();
            platform.pump();
            assertTrue(window.isClosed());
        } finally {
            platform.close();
        }
    }

    /// Returns from the stay-open pump after queued close requests destroy the last HWND.
    @Test
    void pumpUntilClosedReturnsAfterLastCloseRequest() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow[] hosted = new WindowsWindow[2];
            hosted[0] = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "Loop-1",
                            new LogicalRect(16.0, 16.0, 200.0, 120.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> closeOnRequest(hosted, event)
            ).toCompletableFuture().get();
            hosted[1] = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "Loop-2",
                            new LogicalRect(80.0, 80.0, 200.0, 120.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> closeOnRequest(hosted, event)
            ).toCompletableFuture().get();
            platform.pump();
            assertEquals(2, platform.openWindowCount());
            hosted[0].nativeWindow().postMessage(0x0010, 0L, 0L);
            hosted[1].nativeWindow().postMessage(0x0010, 0L, 0L);
            platform.pumpUntilClosed();
            assertEquals(0, platform.openWindowCount());
            assertTrue(hosted[0].isClosed());
            assertTrue(hosted[1].isClosed());
        } finally {
            platform.close();
        }
    }

    /// Drives the shipped IME session and UIA projection.
    @Test
    void imeAndAutomationUseShippedContracts() {
        WindowsImeSession ime = new WindowsImeSession();
        ime.setSurroundingText("", 0);
        ime.setCandidateRectangle(8.0f, 16.0f, 24.0f, 12.0f);
        ime.updateComposition("ni");
        assertEquals("ni", ime.composition());
        assertEquals(0, ime.compositionStart());
        assertEquals(2, ime.compositionEnd());
        assertEquals(8.0f, ime.candidateX());
        assertEquals("ni", ime.commit());
        assertTrue(ime.committed());
        assertEquals("ni", ime.surroundingText());
        assertEquals("ni", ime.reconvert());
        ime.cancel();
        assertEquals(null, ime.composition());
        ime.updateComposition("hao");
        assertEquals("hao", ime.reject());
        assertEquals("hao", ime.lastRejected());
        assertEquals(null, ime.composition());
        assertEquals("ni", ime.surroundingText());
        LayoutTree tree = new LayoutTree();
        tree.setRoot(BootstrapCounterPane.create(tree, new AtomicInteger()));
        tree.measure(Constraints.loose(200.0f, 200.0f));
        tree.place();
        List<WindowsAutomationNode> nodes = WindowsAutomationBridge.inspect(tree.semantics());
        assertTrue(nodes.stream().anyMatch(WindowsAutomationNode::invokeSupported));
        assertTrue(nodes.stream().anyMatch(node -> node.name().equals("Increment")));
        WindowsAutomationNode increment = nodes.stream()
                .filter(WindowsAutomationNode::invokeSupported)
                .findFirst()
                .orElseThrow();
        assertEquals("Button", increment.controlType());
        assertTrue(increment.width() > 0.0f && increment.height() > 0.0f);
        assertEquals(null, increment.textRange());
        assertEquals(tree.semantics().nodeWith(SemanticsAction.ACTIVATE).id(), increment.id());
        LayoutTree editorTree = new LayoutTree();
        LayoutFactory editorFactory = new LayoutFactory(editorTree);
        LayoutNode field = editorFactory.leaf(
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
        editorTree.setRoot(editorFactory.column("root", Alignment.START, List.of(), field));
        editorTree.measure(Constraints.loose(200.0f, 200.0f));
        editorTree.place();
        WindowsAutomationNode edit = WindowsAutomationBridge.inspect(editorTree.semantics()).stream()
                .filter(node -> node.controlType().equals("Edit"))
                .findFirst()
                .orElseThrow();
        assertEquals(new SemanticsTextRange(1, 4, 4), edit.textRange());
        LayoutTree barTree = new LayoutTree();
        LayoutFactory barFactory = new LayoutFactory(barTree);
        LayoutNode bar = barFactory.leaf(
                "bar",
                new Size(160.0f, 16.0f),
                List.of(),
                true,
                SemanticsRole.SCROLLBAR,
                "Thumb",
                java.util.Set.of(SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                null
        );
        bar.setRangeValue(20.0);
        barTree.setRoot(barFactory.column("root", Alignment.START, List.of(), bar));
        barTree.measure(Constraints.loose(200.0f, 200.0f));
        barTree.place();
        WindowsAutomationNode scrollBar = WindowsAutomationBridge.inspect(barTree.semantics()).stream()
                .filter(node -> node.controlType().equals("ScrollBar"))
                .findFirst()
                .orElseThrow();
        assertEquals(20.0, scrollBar.rangeValue());
    }

    /// Writes and reads Unicode clipboard text through generated User32/Kernel32 bindings.
    @Test
    void clipboardRoundTripsUnicodeThroughWin32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Clipboard", 48.0, 48.0);
            platform.pump();
            String marker = "HimariUI-clipboard-" + Long.toUnsignedString(System.nanoTime());
            try {
                window.writeClipboard(marker);
            } catch (WindowsClipboard.ClipboardUnavailableException unavailable) {
                Assumptions.assumeFalse(
                        unavailable.accessDenied(),
                        "host denied OpenClipboard: " + unavailable.getMessage()
                );
                throw unavailable;
            }
            assertEquals(marker, window.readClipboard());
        } finally {
            platform.close();
        }
    }

    /// Continues scheduled UI work from a move/resize modal-loop timer.
    @Test
    void modalLoopTimerDrainsScheduledWork() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Modal", 56.0, 56.0);
            platform.pump();
            window.nativeWindow().postMessage(0x0231, 0L, 0L);
            platform.pump();
            assertTrue(window.modalLoopActive());
            AtomicBoolean ran = new AtomicBoolean();
            platform.eventLoop().post(() -> ran.set(true));
            window.nativeWindow().sendMessage(0x0113, WindowsNativeWindow.MODAL_TIMER_ID, 0L);
            assertTrue(ran.get());
            assertTrue(window.modalTimerTicks() >= 1);
            window.nativeWindow().postMessage(0x0232, 0L, 0L);
            platform.pump();
            assertFalse(window.modalLoopActive());
        } finally {
            platform.close();
        }
    }

    /// Registers an OLE IDropTarget and invokes Drop through the COM vtable.
    @Test
    void oleDropTargetDispatchesThroughVtable() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Drop", 64.0, 64.0);
            platform.pump();
            try (WindowsDropTarget target = window.registerDropTarget()) {
                target.invokeDrop(40, 50);
                assertEquals(1, target.dropCount());
                assertEquals(40, target.lastDropX());
                assertEquals(50, target.lastDropY());
            }
        } finally {
            platform.close();
        }
    }

    /// Extracts Unicode text through `IDataObject::GetData` during `IDropTarget::Drop`.
    @Test
    void oleDropExtractsUnicodeThroughGetData() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "GetData", 72.0, 72.0);
            platform.pump();
            try (
                    WindowsDropTarget target = window.registerDropTarget();
                    WindowsDataObject data = window.createUnicodeDataObject("HimariUI-drop")
            ) {
                target.invokeDrop(data.nativeObject(), 24, 32);
                assertEquals(1, target.dropCount());
                assertEquals("HimariUI-drop", target.lastDroppedText());
            }
        } finally {
            platform.close();
        }
    }

    /// Creates the TSF thread manager, applies IMM32 placement, and reads a UIA control type.
    @Test
    void tsfImm32AndUiaUseNativeCom() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "TsfUia", 88.0, 88.0);
            platform.pump();
            window.ime().setCandidateRectangle(6.0f, 10.0f, 20.0f, 14.0f);
            window.applyImeCandidate();
            window.ime().setSurroundingText("hello", 5);
            try (
                    WindowsTsfSession tsf = window.openTsf();
                    WindowsTextStore store = window.createTextStore()
            ) {
                assertTrue(tsf.available(), "CoCreateInstance(ITfThreadMgr) HRESULT=" + tsf.createResult());
                assertTrue(tsf.activate(), "ITfThreadMgr::Activate HRESULT=" + tsf.activateResult());
                assertTrue(tsf.clientId() != 0);
                assertEquals(0, store.invokeRequestLock(WindowsTextStore.TS_LF_READWRITE));
                assertTrue(store.lockCount() >= 1);
                store.invokeSetText(0, 5, "nihao");
                assertEquals("nihao", store.invokeGetText(0, -1));
                assertEquals("nihao", window.ime().surroundingText());
                store.invokeSetSelection(2);
                WindowsTextStore.Selection selection = store.invokeGetSelection();
                assertEquals(2, selection.start());
                assertEquals(2, selection.end());
                assertEquals(2, window.ime().compositionStart());
                assertEquals(0, store.invokeGetAcpFromPoint(6, 10));
                assertEquals(5, store.invokeGetAcpFromPoint(26, 10));
                WindowsTextStore.ScreenExtent extent = store.invokeGetScreenExt();
                assertEquals(6, extent.left());
                assertEquals(10, extent.top());
                assertEquals(26, extent.right());
                assertEquals(24, extent.bottom());
                assertFalse(store.invokeQueryInsertEmbedded());
                assertTrue(store.invokeGetFormattedText() < 0);
                assertEquals(0, store.invokeRetrieveRequestedAttrs());
                assertFalse(store.invokeFindNextAttrTransition());
                assertTrue(tsf.attach(store), "CreateDocumentMgr/CreateContext/Push failed");
                assertTrue(tsf.documentAttached());
            }
            LayoutTree tree = new LayoutTree();
            tree.setRoot(BootstrapCounterPane.create(tree, new AtomicInteger()));
            tree.measure(Constraints.loose(200.0f, 200.0f));
            tree.place();
            SemanticsNode increment = tree.semantics().nodeWith(SemanticsAction.ACTIVATE);
            try (WindowsAutomationProvider provider = window.automationProvider(increment)) {
                assertEquals(
                        WindowsAutomationProvider.UIA_BUTTON_CONTROL_TYPE_ID,
                        provider.invokePropertyValue(WindowsAutomationProvider.UIA_CONTROL_TYPE_PROPERTY_ID)
                );
                assertTrue(provider.invokePatternProvider(WindowsAutomationProvider.UIA_INVOKE_PATTERN_ID));
                assertEquals(1, provider.invoke());
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
            LayoutNode option = factory.leaf(
                    "option",
                    new Size(120.0f, 20.0f),
                    List.of(),
                    true,
                    SemanticsRole.RADIO,
                    "Left",
                    java.util.Set.of(SemanticsAction.ACTIVATE),
                    () -> { }
            );
            option.setSelected(false);
            LayoutNode item = factory.leaf(
                    "item",
                    new Size(160.0f, 20.0f),
                    List.of(),
                    true,
                    SemanticsRole.TREE_ITEM,
                    "Folder",
                    java.util.Set.of(SemanticsAction.ACTIVATE, SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                    () -> { }
            );
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
            LayoutNode table = factory.leaf(
                    "table",
                    new Size(160.0f, 40.0f),
                    List.of(),
                    false,
                    SemanticsRole.TABLE,
                    "People",
                    java.util.Set.of(),
                    null
            );
            table.setGrid(new SemanticsGrid(2, 3));
            LayoutNode cell = factory.leaf(
                    "cell",
                    new Size(80.0f, 20.0f),
                    List.of(),
                    false,
                    SemanticsRole.TABLE_CELL,
                    "r0c1",
                    java.util.Set.of(),
                    null
            );
            cell.setGridItem(new SemanticsGridItem(0, 1));
            LayoutNode list = factory.leaf(
                    "list",
                    new Size(160.0f, 40.0f),
                    List.of(),
                    true,
                    SemanticsRole.LIST,
                    "Items",
                    java.util.Set.of(SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                    null
            );
            list.setScroll(new SemanticsScroll(25.0, 20.0, true, 10.0, 30.0, true));
            valueTree.setRoot(factory.column(
                    "root",
                    Alignment.START,
                    List.of(),
                    toggle,
                    slider,
                    option,
                    item,
                    table,
                    cell,
                    list,
                    status,
                    field
            ));
            valueTree.measure(Constraints.loose(400.0f, 400.0f));
            valueTree.place();
            SemanticsNode toggleNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TOGGLE)
                    .findFirst()
                    .orElseThrow();
            SemanticsNode sliderNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.SLIDER)
                    .findFirst()
                    .orElseThrow();
            SemanticsNode optionNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.RADIO)
                    .findFirst()
                    .orElseThrow();
            SemanticsNode itemNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TREE_ITEM)
                    .findFirst()
                    .orElseThrow();
            SemanticsNode statusNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.STATUS)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider toggleProvider = window.automationProvider(toggleNode)) {
                assertTrue(toggleProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TOGGLE_PATTERN_ID));
                assertEquals(WindowsAutomationProvider.TOGGLE_STATE_OFF, toggleProvider.toggleState());
                assertEquals(WindowsAutomationProvider.TOGGLE_STATE_ON, toggleProvider.toggle());
            }
            try (WindowsAutomationProvider rangeProvider = window.automationProvider(sliderNode)) {
                assertTrue(rangeProvider.invokePatternProvider(WindowsAutomationProvider.UIA_RANGE_VALUE_PATTERN_ID));
                assertEquals(3.0, rangeProvider.rangeValue());
                assertEquals(7.5, rangeProvider.setRangeValue(7.5));
                assertEquals(7.5, rangeProvider.rangeValue());
            }
            try (WindowsAutomationProvider selectionProvider = window.automationProvider(optionNode)) {
                assertTrue(selectionProvider.invokePatternProvider(WindowsAutomationProvider.UIA_SELECTION_ITEM_PATTERN_ID));
                assertFalse(selectionProvider.itemSelected());
                assertTrue(selectionProvider.selectItem());
                assertFalse(selectionProvider.removeItemFromSelection());
            }
            try (WindowsAutomationProvider expandProvider = window.automationProvider(itemNode)) {
                assertTrue(expandProvider.invokePatternProvider(WindowsAutomationProvider.UIA_EXPAND_COLLAPSE_PATTERN_ID));
                assertEquals(WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_EXPANDED, expandProvider.expandState());
                assertEquals(WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_COLLAPSED, expandProvider.collapse());
                assertEquals(WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_EXPANDED, expandProvider.expand());
            }
            SemanticsNode tableNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TABLE)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider gridProvider = window.automationProvider(tableNode)) {
                assertTrue(gridProvider.invokePatternProvider(WindowsAutomationProvider.UIA_GRID_PATTERN_ID));
                assertTrue(gridProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TABLE_PATTERN_ID));
                assertEquals(2, gridProvider.gridRowCount());
                assertEquals(3, gridProvider.gridColumnCount());
                assertTrue(gridProvider.invokeGetItem(1, 2));
                assertEquals(1, gridProvider.invokeFetchedItemRow());
                assertEquals(2, gridProvider.invokeFetchedItemColumn());
                assertTrue(gridProvider.invokeFetchedContainingGrid());
                assertFalse(gridProvider.invokeGetItem(2, 0));
                assertEquals(0, gridProvider.invokeRowHeaders());
                assertEquals(0, gridProvider.invokeColumnHeaders());
                assertEquals(WindowsAutomationProvider.ROW_OR_COLUMN_MAJOR_ROW, gridProvider.rowOrColumnMajor());
            }
            SemanticsNode cellNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TABLE_CELL)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider cellProvider = window.automationProvider(cellNode)) {
                assertTrue(cellProvider.invokePatternProvider(WindowsAutomationProvider.UIA_GRID_ITEM_PATTERN_ID));
                assertTrue(cellProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TABLE_ITEM_PATTERN_ID));
                assertEquals(0, cellProvider.gridItemRow());
                assertEquals(1, cellProvider.gridItemColumn());
                assertEquals(0, cellProvider.invokeRowHeaderItems());
            }
            SemanticsNode listNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.LIST)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider scrollProvider = window.automationProvider(listNode)) {
                assertTrue(scrollProvider.invokePatternProvider(WindowsAutomationProvider.UIA_SCROLL_PATTERN_ID));
                assertTrue(scrollProvider.verticallyScrollable());
                assertTrue(scrollProvider.horizontallyScrollable());
                assertEquals(25.0, scrollProvider.verticalScrollPercent());
                assertEquals(10.0, scrollProvider.horizontalScrollPercent());
                assertEquals(40.0, scrollProvider.setVerticalScrollPercent(40.0));
                assertEquals(50.0, scrollProvider.scrollVertical(WindowsAutomationProvider.SCROLL_AMOUNT_SMALL_INCREMENT));
                assertEquals(20.0, scrollProvider.setHorizontalScrollPercent(20.0));
                assertEquals(30.0, scrollProvider.scrollHorizontal(WindowsAutomationProvider.SCROLL_AMOUNT_SMALL_INCREMENT));
            }
            try (WindowsAutomationProvider statusProvider = window.automationProvider(statusNode)) {
                assertEquals(
                        WindowsAutomationProvider.UIA_STATUS_BAR_CONTROL_TYPE_ID,
                        statusProvider.invokePropertyValue(WindowsAutomationProvider.UIA_CONTROL_TYPE_PROPERTY_ID)
                );
                assertEquals(
                        WindowsAutomationProvider.LIVE_SETTING_POLITE,
                        statusProvider.invokePropertyValue(WindowsAutomationProvider.UIA_LIVE_SETTING_PROPERTY_ID)
                );
            }
            SemanticsNode fieldNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider textProvider = window.automationProvider(fieldNode)) {
                assertTrue(textProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TEXT_PATTERN_ID));
                assertTrue(textProvider.invokeDocumentRange());
                assertEquals(
                        WindowsAutomationProvider.SUPPORTED_TEXT_SELECTION_SINGLE,
                        textProvider.invokeSupportedTextSelection()
                );
                assertEquals("hello", textProvider.invokeGetText(-1));
                assertEquals("he", textProvider.invokeGetText(2));
                assertTrue(textProvider.invokeClone());
                assertTrue(textProvider.invokeCompareSelf());
                assertTrue(textProvider.invokeEnclosingElement());
                textProvider.invokeExpandToEnclosingUnit(WindowsAutomationProvider.TEXT_UNIT_DOCUMENT);
                assertEquals("hello", textProvider.invokeGetText(-1));
                assertEquals(-2, textProvider.invokeMove(WindowsAutomationProvider.TEXT_UNIT_CHARACTER, -2));
                assertEquals(
                        4,
                        textProvider.invokeExpandToEnclosingUnit(WindowsAutomationProvider.TEXT_UNIT_CHARACTER).end()
                );
                assertEquals("l", textProvider.invokeGetText(-1));
                double[] rects = textProvider.invokeGetBoundingRectangles();
                assertEquals(4, rects.length);
                assertEquals(fieldNode.bounds().width(), rects[2], 0.01);
                assertEquals(fieldNode.bounds().height(), rects[3], 0.01);
                textProvider.invokeExpandToEnclosingUnit(WindowsAutomationProvider.TEXT_UNIT_DOCUMENT);
                assertTrue(textProvider.invokeCompareEndpoints(
                        WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_START,
                        WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_END
                ) < 0);
                assertTrue(textProvider.invokeFindText("ell", false));
                assertEquals("ell", textProvider.invokeGetText(-1));
                assertEquals(
                        1,
                        textProvider.invokeMoveEndpointByUnit(
                                WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_END,
                                WindowsAutomationProvider.TEXT_UNIT_CHARACTER,
                                1
                        )
                );
                assertEquals("ello", textProvider.invokeGetText(-1));
                textProvider.invokeMoveEndpointByRange(
                        WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_END,
                        WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_START
                );
                assertEquals("", textProvider.invokeGetText(-1));
                assertFalse(textProvider.invokeGetSelection());
                textProvider.invokeSelect();
                assertTrue(textProvider.invokeGetSelection());
                assertFalse(textProvider.invokeFindAttribute(40013));
                assertEquals(0, textProvider.invokeGetAttributeValue(40013));
                textProvider.invokeRemoveFromSelection();
                assertFalse(textProvider.invokeGetSelection());
                textProvider.invokeAddToSelection();
                assertTrue(textProvider.invokeGetSelection());
                assertTrue(textProvider.invokeScrollIntoView(true));
                assertEquals(0, textProvider.invokeGetChildren());
            }
        } finally {
            platform.close();
        }
    }

    /// Creates a Vulkan logical device and a `VkSurfaceKHR` for the production HWND.
    @Test
    void vulkanCreatesWin32SurfaceForHwnd() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "VulkanSurface", 96.0, 96.0);
            platform.pump();
            try (VulkanDevice device = VulkanDevice.open()) {
                assertTrue(device.capabilities().logicalDeviceCreated());
                assertTrue(device.capabilities().graphicsQueueFamily() >= 0);
                VulkanPresentation presentation = device.presentSdr(
                        window.moduleHandle(),
                        window.nativeHandle(),
                        window.nativeWindow().clientWidth(),
                        window.nativeWindow().clientHeight()
                );
                assertTrue(presentation.swapchainCreated());
                assertTrue(presentation.cleared());
                assertTrue(presentation.presented());
                assertTrue(device.capabilities().win32SurfaceCreated());
                assertFalse(presentation.hdrMetadataApplied());
            }
        } finally {
            platform.close();
        }
    }

    /// Closes the matching hosted HWND when the host asks the application to decide.
    ///
    /// @param hosted the windows that may honor the request
    /// @param event the host event
    private static void closeOnRequest(WindowsWindow[] hosted, WindowEvent event) {
        if (event.type() != WindowEventType.CLOSE_REQUESTED) {
            return;
        }
        for (WindowsWindow candidate : hosted) {
            if (candidate != null
                    && !candidate.isClosed()
                    && candidate.id().equals(event.snapshot().id())) {
                candidate.closeAsync();
            }
        }
    }

    /// Opens one visible top-level window.
    private static WindowsWindow openToplevel(WindowsPlatform platform, String title, double x, double y)
            throws Exception {
        return platform.createWindow(
                WindowRequest.toplevel(new WindowConfiguration(
                        title,
                        new LogicalRect(x, y, 240.0, 160.0),
                        true,
                        WindowState.NORMAL
                )),
                event -> { }
        ).toCompletableFuture().get();
    }
}
