package org.glavo.himari.platform.windows;

import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.PresentationMode;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowEventType;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowState;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes production Windows platform conformance evidence.
@NotNullByDefault
public final class WindowsPlatformConformance {
    /// Prevents instantiation.
    private WindowsPlatformConformance() {
    }

    /// Opens two windows, delivers WndProc input, and records DPI-backed topology.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        int dpi;
        double scale;
        int displayWidth;
        int pointerCount;
        int presentedScanlines = 0;
        boolean clipboardRoundTrip = false;
        boolean clipboardAccessDenied = false;
        boolean modalTick = false;
        boolean oleDrop = false;
        boolean dataObjectGetData = false;
        boolean tsfThreadMgr = false;
        boolean messageLoop = false;
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow first = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "HimariUI Conformance",
                            new LogicalRect(16.0, 16.0, 240.0, 160.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> { }
            ).toCompletableFuture().get();
            WindowsWindow second = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "HimariUI Conformance 2",
                            new LogicalRect(64.0, 64.0, 200.0, 140.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> { }
            ).toCompletableFuture().get();
            platform.pump();
            if (first.nativeHandle().address() == 0L || second.nativeHandle().address() == 0L) {
                throw new IllegalStateException("HWND was null");
            }
            if (platform.openWindowCount() != 2) {
                throw new IllegalStateException("Expected two open windows");
            }
            if (!platform.displayTopology().displays().getFirst()
                    .colorCapabilities().description().presentationModes().contains(PresentationMode.SDR)) {
                throw new IllegalStateException("Windows display did not report SDR fallback");
            }
            dpi = first.dpi();
            scale = first.snapshot().scaleFactor();
            displayWidth = platform.displayTopology().displays().getFirst().physicalSize().width();
            if (dpi < 96 || scale <= 0.0 || displayWidth <= 0) {
                throw new IllegalStateException("Windows display DPI or metrics were not queried");
            }
            first.postPointer(PointerEventType.DOWN, 8, 8);
            platform.pump();
            if (!first.captured()) {
                throw new IllegalStateException("SetCapture did not report this HWND after WM_LBUTTONDOWN");
            }
            first.postPointer(PointerEventType.UP, 8, 8);
            first.postWheel(8, 8, 1);
            platform.pump();
            if (first.captured()) {
                throw new IllegalStateException("ReleaseCapture did not drop capture after WM_LBUTTONUP");
            }
            if (!first.setSystemCursor(WindowsNativeWindow.IDC_ARROW)) {
                throw new IllegalStateException("LoadCursorW/SetCursor failed for IDC_ARROW");
            }
            java.util.List<org.glavo.himari.layout.input.PointerEvent> delivered = first.takePointerEvents();
            pointerCount = delivered.size();
            if (pointerCount < 3
                    || delivered.get(2).type() != PointerEventType.WHEEL
                    || delivered.get(2).wheelDelta() != 1.0f) {
                throw new IllegalStateException("WndProc did not deliver posted pointer and wheel events");
            }
            byte[] rgba = new byte[16 * 8 * 4];
            for (int pixel = 0; pixel < rgba.length; pixel += 4) {
                rgba[pixel] = (byte) 0x20;
                rgba[pixel + 1] = (byte) 0x40;
                rgba[pixel + 2] = (byte) 0x80;
                rgba[pixel + 3] = (byte) 0xFF;
            }
            presentedScanlines = first.presentSdrRgba(java.lang.foreign.MemorySegment.ofArray(rgba), 16, 8);
            platform.pump();
            if (presentedScanlines != 8) {
                throw new IllegalStateException("SetDIBitsToDevice did not present 8 scanlines");
            }
            String marker = "HimariUI-conformance-clipboard";
            try {
                first.writeClipboard(marker);
                clipboardRoundTrip = marker.equals(first.readClipboard());
            } catch (WindowsClipboard.ClipboardUnavailableException unavailable) {
                if (!unavailable.accessDenied()) {
                    throw unavailable;
                }
                clipboardAccessDenied = true;
            }
            if (!clipboardRoundTrip && !clipboardAccessDenied) {
                throw new IllegalStateException("Clipboard Unicode round-trip failed");
            }
            first.nativeWindow().postMessage(0x0231, 0L, 0L);
            platform.pump();
            java.util.concurrent.atomic.AtomicBoolean ran = new java.util.concurrent.atomic.AtomicBoolean();
            platform.eventLoop().post(() -> ran.set(true));
            first.nativeWindow().sendMessage(0x0113, WindowsNativeWindow.MODAL_TIMER_ID, 0L);
            modalTick = ran.get() && first.modalTimerTicks() >= 1;
            if (!modalTick) {
                throw new IllegalStateException("Modal-loop timer did not drain scheduled work");
            }
            first.nativeWindow().postMessage(0x0232, 0L, 0L);
            platform.pump();
            try (
                    WindowsDropTarget target = first.registerDropTarget();
                    WindowsDataObject data = first.createUnicodeDataObject("HimariUI-conformance-drop")
            ) {
                target.invokeDrop(12, 16);
                oleDrop = target.dropCount() == 1;
                target.invokeDrop(data.nativeObject(), 20, 24);
                dataObjectGetData = "HimariUI-conformance-drop".equals(target.lastDroppedText());
            }
            if (!oleDrop) {
                throw new IllegalStateException("OLE IDropTarget::Drop was not dispatched");
            }
            if (!dataObjectGetData) {
                throw new IllegalStateException("IDataObject::GetData did not yield Unicode text");
            }
            try (WindowsTsfSession tsf = first.openTsf()) {
                tsfThreadMgr = tsf.available() && tsf.activate();
            }
            if (!tsfThreadMgr) {
                throw new IllegalStateException("ITfThreadMgr was not created or activated");
            }
            first.closeAsync().toCompletableFuture().get();
            platform.pump();
            if (first.isClosed() && second.isClosed()) {
                throw new IllegalStateException("Closing one window closed the session peer");
            }
            second.closeAsync().toCompletableFuture().get();
            platform.pump();
            WindowsWindow[] hosted = new WindowsWindow[2];
            hosted[0] = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "HimariUI Loop 1",
                            new LogicalRect(24.0, 24.0, 200.0, 120.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> {
                        if (event.type() == WindowEventType.CLOSE_REQUESTED && hosted[0] != null) {
                            hosted[0].closeAsync();
                        }
                    }
            ).toCompletableFuture().get();
            hosted[1] = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "HimariUI Loop 2",
                            new LogicalRect(88.0, 88.0, 200.0, 120.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> {
                        if (event.type() == WindowEventType.CLOSE_REQUESTED && hosted[1] != null) {
                            hosted[1].closeAsync();
                        }
                    }
            ).toCompletableFuture().get();
            platform.pump();
            hosted[0].nativeWindow().postMessage(0x0010, 0L, 0L);
            hosted[1].nativeWindow().postMessage(0x0010, 0L, 0L);
            platform.pumpUntilClosed();
            messageLoop = platform.openWindowCount() == 0 && hosted[0].isClosed() && hosted[1].isClosed();
            if (!messageLoop) {
                throw new IllegalStateException("Stay-open pump did not return after the last HWND closed");
            }
        } finally {
            platform.close();
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m6-windows",
                          "workPackage": "WIN-001",
                          "status": "passed",
                          "hwndCreated": true,
                          "multiWindow": true,
                          "dpi": %d,
                          "scaleFactor": %s,
                          "displayWidth": %d,
                          "pointerEvents": %d,
                          "presentedScanlines": %d,
                          "clipboard": %s,
                          "clipboardAccessDenied": %s,
                          "modalLoop": %s,
                          "oleDrop": %s,
                          "dataObjectGetData": %s,
                          "tsfThreadMgr": %s,
                          "messageLoop": %s,
                          "sdrFallback": true
                        }
                        """.formatted(
                        dpi,
                        Double.toString(scale),
                        displayWidth,
                        pointerCount,
                        presentedScanlines,
                        clipboardRoundTrip,
                        clipboardAccessDenied,
                        modalTick,
                        oleDrop,
                        dataObjectGetData,
                        tsfThreadMgr,
                        messageLoop
                ),
                StandardCharsets.UTF_8
        );
    }
}
