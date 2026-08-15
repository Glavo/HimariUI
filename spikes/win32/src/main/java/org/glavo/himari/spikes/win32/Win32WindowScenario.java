package org.glavo.himari.spikes.win32;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.spikes.win32.generated.Win32FfmBindings;
import org.glavo.himari.spikes.win32.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/// Runs one Unicode top-level Win32 window through deterministic lifecycle and callback stimuli.
@NotNullByDefault
final class Win32WindowScenario {
    /// Horizontal and vertical redraw class styles.
    private static final int CLASS_STYLE = 0x0003;

    /// Standard overlapped top-level window style.
    private static final int WINDOW_STYLE = 0x00cf0000;

    /// Non-activating tool-window extended styles used to avoid stealing user focus.
    private static final int WINDOW_EXTENDED_STYLE = 0x08000080;

    /// `SW_HIDE` command.
    private static final int SW_HIDE = 0;

    /// `SW_SHOWNOACTIVATE` command.
    private static final int SW_SHOWNOACTIVATE = 4;

    /// `SWP_NOSIZE`-independent flags preserving position, z-order, and activation.
    private static final int SET_WINDOW_POSITION_FLAGS = 0x0016;

    /// `PM_REMOVE` message-pump flag.
    private static final int PM_REMOVE = 0x0001;

    /// `MONITOR_DEFAULTTOPRIMARY` selection policy.
    private static final int MONITOR_DEFAULTTOPRIMARY = 0x0001;

    /// System `COLOR_WINDOW` brush index.
    private static final int COLOR_WINDOW = 5;

    /// `WM_DESTROY` message.
    private static final int WM_DESTROY = 0x0002;

    /// `WM_SIZE` message.
    private static final int WM_SIZE = 0x0005;

    /// `WM_PAINT` message.
    private static final int WM_PAINT = 0x000f;

    /// `WM_CLOSE` message.
    private static final int WM_CLOSE = 0x0010;

    /// `WM_QUIT` thread message.
    private static final int WM_QUIT = 0x0012;

    /// `WM_KEYDOWN` message.
    private static final int WM_KEYDOWN = 0x0100;

    /// `WM_CHAR` message.
    private static final int WM_CHAR = 0x0102;

    /// `WM_MOUSEMOVE` message.
    private static final int WM_MOUSEMOVE = 0x0200;

    /// First application-private message.
    private static final int WM_APP = 0x8000;

    /// Message that injects one contained Java failure.
    private static final int WM_APP_FAILURE = WM_APP + 1;

    /// Message that enters a synchronous nested `SendMessageW` call.
    private static final int WM_APP_REENTRANT = WM_APP + 2;

    /// Nested message delivered by the reentrant stimulus.
    private static final int WM_APP_NESTED = WM_APP + 3;

    /// Maximum wait for a posted event or close transition.
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(5);

    /// The generated native bindings.
    private final Win32FfmBindings bindings;

    /// The arena retaining the upcall stub and window-owned native data.
    private final Arena arena;

    /// Failures caught by the generated `WndProc` containment adapter.
    private final CallbackFailureQueue callbackFailures;

    /// The normalized target-event sequence.
    private final List<String> eventSequence;

    /// The reusable native `MSG` structure.
    private final MemorySegment messageRecord;

    /// The reusable native `PAINTSTRUCT` structure.
    private final MemorySegment paintRecord;

    /// The system background brush used for the solid-color paint.
    private final MemorySegment backgroundBrush;

    /// The native window handle while the window exists.
    private MemorySegment window;

    /// The stimulus cycle currently accepting normalized event records.
    private int currentCycle;

    /// Whether target event recording is enabled.
    private boolean recording;

    /// Current native callback nesting depth.
    private int callbackDepth;

    /// Maximum native callback nesting depth.
    private int maximumCallbackDepth;

    /// Number of target resize messages.
    private int resizeEvents;

    /// Number of target pointer messages.
    private int pointerEvents;

    /// Number of target key messages.
    private int keyEvents;

    /// Number of target character messages.
    private int characterEvents;

    /// Number of paint messages across the full window lifetime.
    private int paintEvents;

    /// Last client width decoded from `WM_SIZE`.
    private int finalClientWidth;

    /// Last client height decoded from `WM_SIZE`.
    private int finalClientHeight;

    /// Whether the close callback ran.
    private boolean closeObserved;

    /// Whether the destroy callback ran.
    private boolean destroyObserved;

    /// Whether the message pump removed `WM_QUIT`.
    private boolean quitObserved;

    /// Creates one scenario using resources owned by `arena`.
    ///
    /// @param bindings the generated bindings
    /// @param arena the scenario lifetime arena
    private Win32WindowScenario(Win32FfmBindings bindings, Arena arena) {
        this.bindings = bindings;
        this.arena = arena;
        this.callbackFailures = new CallbackFailureQueue();
        this.eventSequence = new ArrayList<>();
        this.messageRecord = arena.allocate(Win32Layouts.MSG);
        this.paintRecord = arena.allocate(Win32Layouts.PAINTSTRUCT);
        this.backgroundBrush = bindings.getSysColorBrush(COLOR_WINDOW);
        this.window = MemorySegment.NULL;
        this.currentCycle = -1;
        if (isNull(backgroundBrush)) {
            throw new IllegalStateException("GetSysColorBrush(COLOR_WINDOW) returned NULL");
        }
    }

    /// Runs one complete window lifetime.
    ///
    /// @param bindings the generated bindings
    /// @param repetitions the positive number of event cycles
    /// @param soakSeconds the non-negative minimum cycle duration
    /// @return normalized lifecycle and capability evidence
    static Win32WindowResult run(Win32FfmBindings bindings, int repetitions, int soakSeconds) {
        if (repetitions < 1) {
            throw new IllegalArgumentException("repetitions must be positive");
        }
        if (soakSeconds < 0) {
            throw new IllegalArgumentException("soakSeconds must be non-negative");
        }
        try (Arena arena = Arena.ofConfined()) {
            return new Win32WindowScenario(bindings, arena).execute(repetitions, soakSeconds);
        }
    }

    /// Registers the class, creates the window, executes cycles, and tears every resource down.
    ///
    /// @param repetitions the event-cycle count
    /// @param soakSeconds the minimum cycle duration
    /// @return the complete result
    private Win32WindowResult execute(int repetitions, int soakSeconds) {
        MemorySegment instance = requireValue(bindings.getModuleHandleW(MemorySegment.NULL), "GetModuleHandleW");
        MemorySegment className = arena.allocateFrom("HimariUIWin32Conformance", StandardCharsets.UTF_16LE);
        MemorySegment title = arena.allocateFrom("HimariUI Win32 Conformance", StandardCharsets.UTF_16LE);
        MemorySegment callback = bindings.createWndProcStub(this::windowProcedure, callbackFailures, arena);
        MemorySegment windowClass = createWindowClass(instance, className, callback);
        boolean classRegistered = false;
        try {
            Win32FfmBindings.RegisterClassExWResult registration = bindings.registerClassExW(windowClass);
            if (registration.value() == 0) {
                throw windowsFailure("RegisterClassExW", registration.errorCode());
            }
            classRegistered = true;

            Win32FfmBindings.CreateWindowExWResult creation = bindings.createWindowExW(
                    WINDOW_EXTENDED_STYLE,
                    className,
                    title,
                    WINDOW_STYLE,
                    -32_000,
                    -32_000,
                    320,
                    240,
                    MemorySegment.NULL,
                    MemorySegment.NULL,
                    instance,
                    MemorySegment.NULL
            );
            if (isNull(creation.value())) {
                throw windowsFailure("CreateWindowExW", creation.errorCode());
            }
            window = creation.value();

            bindings.showWindow(window, SW_SHOWNOACTIVATE);
            Win32FfmBindings.InvalidateRectResult invalidation = bindings.invalidateRect(
                    window,
                    MemorySegment.NULL,
                    1
            );
            if (invalidation.value() == 0) {
                throw windowsFailure("InvalidateRect", invalidation.errorCode());
            }
            bindings.updateWindow(window);
            if (paintEvents < 1) {
                throw new IllegalStateException("UpdateWindow did not synchronously deliver WM_PAINT");
            }
            bindings.showWindow(window, SW_HIDE);

            MemorySegment monitor = bindings.monitorFromWindow(window, MONITOR_DEFAULTTOPRIMARY);
            if (isNull(monitor)) {
                throw new IllegalStateException("MonitorFromWindow returned NULL");
            }
            DxgiOutputSnapshot output = DxgiOutputQuery.query(bindings, monitor);

            long started = System.nanoTime();
            long soakNanos = Duration.ofSeconds(soakSeconds).toNanos();
            recording = true;
            for (int cycle = 0; cycle < repetitions; cycle++) {
                currentCycle = cycle;
                long target = started + multiplyFraction(soakNanos, cycle + 1L, repetitions);
                pumpUntilTime(target);
                executeCycle(cycle);
            }
            long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
            recording = false;
            closeWindow();
            int containedFailures = callbackFailures.size();
            validateResult(repetitions, soakSeconds, elapsedMillis);
            return new Win32WindowResult(
                    repetitions,
                    soakSeconds,
                    elapsedMillis,
                    eventSequence,
                    resizeEvents,
                    pointerEvents,
                    keyEvents,
                    characterEvents,
                    paintEvents,
                    containedFailures,
                    maximumCallbackDepth,
                    finalClientWidth,
                    finalClientHeight,
                    closeObserved,
                    destroyObserved,
                    quitObserved,
                    output
            );
        } finally {
            recording = false;
            if (!isNull(window) && !destroyObserved) {
                Win32FfmBindings.DestroyWindowResult destruction = bindings.destroyWindow(window);
                if (destruction.value() == 0) {
                    throw windowsFailure("DestroyWindow during cleanup", destruction.errorCode());
                }
            }
            window = MemorySegment.NULL;
            if (classRegistered) {
                Win32FfmBindings.UnregisterClassWResult unregistration = bindings.unregisterClassW(className, instance);
                if (unregistration.value() == 0) {
                    throw windowsFailure("UnregisterClassW", unregistration.errorCode());
                }
            }
        }
    }

    /// Initializes one native `WNDCLASSEXW` structure.
    ///
    /// @param instance the current module handle
    /// @param className the retained UTF-16 class name
    /// @param callback the retained `WndProc` upcall stub
    /// @return the initialized structure
    private MemorySegment createWindowClass(
            MemorySegment instance,
            MemorySegment className,
            MemorySegment callback
    ) {
        MemorySegment windowClass = arena.allocate(Win32Layouts.WNDCLASSEXW);
        windowClass.fill((byte) 0);
        windowClass.set(
                ValueLayout.JAVA_INT,
                Win32Layouts.WNDCLASSEXW_CB_SIZE_OFFSET,
                Math.toIntExact(Win32Layouts.WNDCLASSEXW.byteSize())
        );
        windowClass.set(ValueLayout.JAVA_INT, Win32Layouts.WNDCLASSEXW_STYLE_OFFSET, CLASS_STYLE);
        windowClass.set(ValueLayout.ADDRESS, Win32Layouts.WNDCLASSEXW_WND_PROC_OFFSET, callback);
        windowClass.set(ValueLayout.ADDRESS, Win32Layouts.WNDCLASSEXW_INSTANCE_OFFSET, instance);
        windowClass.set(ValueLayout.ADDRESS, Win32Layouts.WNDCLASSEXW_BACKGROUND_OFFSET, backgroundBrush);
        windowClass.set(ValueLayout.ADDRESS, Win32Layouts.WNDCLASSEXW_CLASS_NAME_OFFSET, className);
        return windowClass;
    }

    /// Executes one resize, input, reentrancy, and failure-containment cycle.
    ///
    /// @param cycle the zero-based cycle index
    private void executeCycle(int cycle) {
        int previousResize = resizeEvents;
        int previousPointer = pointerEvents;
        int previousKey = keyEvents;
        int previousCharacter = characterEvents;
        int previousFailures = callbackFailures.size();

        int width = cycle % 2 == 0 ? 400 : 420;
        int height = cycle % 2 == 0 ? 300 : 320;
        Win32FfmBindings.SetWindowPosResult positioned = bindings.setWindowPos(
                window,
                MemorySegment.NULL,
                0,
                0,
                width,
                height,
                SET_WINDOW_POSITION_FLAGS
        );
        if (positioned.value() == 0) {
            throw windowsFailure("SetWindowPos", positioned.errorCode());
        }

        post(WM_MOUSEMOVE, 0L, packCoordinates(10 + cycle, 20 + cycle));
        post(WM_KEYDOWN, 'A', 0L);
        post(WM_CHAR, 'a', 0L);
        bindings.sendMessageW(window, WM_APP_REENTRANT, 0L, 0L);
        bindings.sendMessageW(window, WM_APP_FAILURE, 0L, 0L);

        pumpUntil(() -> pointerEvents > previousPointer
                && keyEvents > previousKey
                && characterEvents > previousCharacter);
        if (resizeEvents != previousResize + 1) {
            throw new IllegalStateException("Cycle " + cycle + " observed "
                    + (resizeEvents - previousResize) + " WM_SIZE messages");
        }
        if (pointerEvents != previousPointer + 1
                || keyEvents != previousKey + 1
                || characterEvents != previousCharacter + 1) {
            throw new IllegalStateException("Cycle " + cycle + " input counts were not exact");
        }
        if (callbackFailures.size() != previousFailures + 1) {
            throw new IllegalStateException("Cycle " + cycle + " did not contain exactly one callback failure");
        }
    }

    /// Posts one message and reports its immediate Win32 failure state.
    ///
    /// @param message the message identifier
    /// @param wParam the pointer-sized unsigned parameter
    /// @param lParam the pointer-sized signed parameter
    private void post(int message, long wParam, long lParam) {
        Win32FfmBindings.PostMessageWResult result = bindings.postMessageW(window, message, wParam, lParam);
        if (result.value() == 0) {
            throw windowsFailure("PostMessageW(" + message + ")", result.errorCode());
        }
    }

    /// Posts close and pumps until both destruction and the thread quit record are observed.
    private void closeWindow() {
        post(WM_CLOSE, 0L, 0L);
        pumpUntil(() -> destroyObserved && quitObserved);
        window = MemorySegment.NULL;
    }

    /// Dispatches every pending message once.
    ///
    /// @return whether at least one message was removed
    private boolean pumpAvailableMessages() {
        boolean processed = false;
        while (bindings.peekMessageW(messageRecord, MemorySegment.NULL, 0, 0, PM_REMOVE) != 0) {
            processed = true;
            int message = messageRecord.get(ValueLayout.JAVA_INT, Win32Layouts.MSG_MESSAGE_OFFSET);
            if (message == WM_QUIT) {
                quitObserved = true;
                continue;
            }
            if (message != WM_KEYDOWN) {
                bindings.translateMessage(messageRecord);
            }
            bindings.dispatchMessageW(messageRecord);
        }
        return processed;
    }

    /// Pumps until a condition holds or the fixed event timeout expires.
    ///
    /// @param condition the condition inspected on the UI thread
    private void pumpUntil(BooleanCondition condition) {
        long deadline = System.nanoTime() + EVENT_TIMEOUT.toNanos();
        while (!condition.test()) {
            if (!pumpAvailableMessages()) {
                parkBriefly();
            }
            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException("Timed out while pumping the Win32 message queue");
            }
        }
    }

    /// Pumps messages until an absolute monotonic deadline is reached.
    ///
    /// @param targetNanos the absolute `System.nanoTime()` target
    private void pumpUntilTime(long targetNanos) {
        while (System.nanoTime() < targetNanos) {
            if (!pumpAvailableMessages()) {
                parkBriefly();
            }
        }
    }

    /// Parks the UI thread briefly without relying on an application timer.
    private static void parkBriefly() {
        LockSupport.parkNanos(500_000L);
        if (Thread.interrupted()) {
            throw new IllegalStateException("Interrupted while pumping the Win32 message queue");
        }
    }

    /// Handles one native `WndProc` callback.
    ///
    /// @param callbackWindow the native window handle
    /// @param message the message identifier
    /// @param wParam the pointer-sized unsigned parameter
    /// @param lParam the pointer-sized signed parameter
    /// @return the native `LRESULT`
    private long windowProcedure(MemorySegment callbackWindow, int message, long wParam, long lParam) {
        callbackDepth++;
        maximumCallbackDepth = Math.max(maximumCallbackDepth, callbackDepth);
        try {
            return switch (message) {
                case WM_SIZE -> onResize(lParam);
                case WM_PAINT -> onPaint(callbackWindow);
                case WM_MOUSEMOVE -> onTargetEvent("pointer", () -> pointerEvents++);
                case WM_KEYDOWN -> onTargetEvent("key", () -> keyEvents++);
                case WM_CHAR -> onTargetEvent("character", () -> characterEvents++);
                case WM_APP_REENTRANT -> onReentrant(callbackWindow);
                case WM_APP_NESTED -> onNested();
                case WM_APP_FAILURE -> onFailure();
                case WM_CLOSE -> onClose(callbackWindow);
                case WM_DESTROY -> onDestroy();
                default -> bindings.defWindowProcW(callbackWindow, message, wParam, lParam);
            };
        } finally {
            callbackDepth--;
        }
    }

    /// Handles a resize notification.
    ///
    /// @param lParam the packed client dimensions
    /// @return zero after handling
    private long onResize(long lParam) {
        finalClientWidth = Short.toUnsignedInt((short) lParam);
        finalClientHeight = Short.toUnsignedInt((short) (lParam >>> 16));
        if (recording) {
            resizeEvents++;
            eventSequence.add(event("resize"));
        }
        return 0L;
    }

    /// Paints the invalid region with the system window color.
    ///
    /// @param callbackWindow the window being painted
    /// @return zero after handling
    private long onPaint(MemorySegment callbackWindow) {
        paintRecord.fill((byte) 0);
        MemorySegment deviceContext = bindings.beginPaint(callbackWindow, paintRecord);
        if (isNull(deviceContext)) {
            throw new IllegalStateException("BeginPaint returned NULL");
        }
        try {
            MemorySegment rectangle = paintRecord.asSlice(
                    Win32Layouts.PAINTSTRUCT_PAINT_RECT_OFFSET,
                    Win32Layouts.RECT.byteSize()
            );
            if (bindings.fillRect(deviceContext, rectangle, backgroundBrush) == 0) {
                throw new IllegalStateException("FillRect failed");
            }
            paintEvents++;
        } finally {
            if (bindings.endPaint(callbackWindow, paintRecord) == 0) {
                throw new IllegalStateException("EndPaint failed");
            }
        }
        return 0L;
    }

    /// Records one target input event.
    ///
    /// @param name the normalized event name
    /// @param counter the exact counter increment
    /// @return zero after handling
    private long onTargetEvent(String name, Counter counter) {
        if (recording) {
            counter.increment();
            eventSequence.add(event(name));
        }
        return 0L;
    }

    /// Enters a synchronous nested callback through `SendMessageW`.
    ///
    /// @param callbackWindow the current window
    /// @return the nested message result
    private long onReentrant(MemorySegment callbackWindow) {
        if (recording) {
            eventSequence.add(event("reentrant-enter"));
        }
        long result = bindings.sendMessageW(callbackWindow, WM_APP_NESTED, 0L, 0L);
        if (recording) {
            eventSequence.add(event("reentrant-exit"));
        }
        return result;
    }

    /// Handles the nested message reached through synchronous reentrancy.
    ///
    /// @return a deterministic sentinel
    private long onNested() {
        if (recording) {
            eventSequence.add(event("nested"));
        }
        return 0x48494d415249L;
    }

    /// Throws the deliberate callback failure after recording its boundary.
    ///
    /// @return this method never returns normally
    private long onFailure() {
        if (recording) {
            eventSequence.add(event("failure-contained"));
        }
        throw new DeliberateCallbackFailure("deliberate WndProc failure for cycle " + currentCycle);
    }

    /// Destroys the native window in response to close.
    ///
    /// @param callbackWindow the owned window
    /// @return zero after handling
    private long onClose(MemorySegment callbackWindow) {
        closeObserved = true;
        eventSequence.add("close");
        Win32FfmBindings.DestroyWindowResult result = bindings.destroyWindow(callbackWindow);
        if (result.value() == 0) {
            throw windowsFailure("DestroyWindow", result.errorCode());
        }
        return 0L;
    }

    /// Posts the thread quit message after destruction.
    ///
    /// @return zero after handling
    private long onDestroy() {
        destroyObserved = true;
        eventSequence.add("destroy");
        bindings.postQuitMessage(0);
        return 0L;
    }

    /// Validates all exact counts, callback failures, sequence entries, and lifecycle transitions.
    ///
    /// @param repetitions the expected cycle count
    /// @param soakSeconds the requested duration
    /// @param elapsedMillis the measured duration
    private void validateResult(int repetitions, int soakSeconds, long elapsedMillis) {
        if (resizeEvents != repetitions
                || pointerEvents != repetitions
                || keyEvents != repetitions
                || characterEvents != repetitions) {
            throw new IllegalStateException("Win32 target-event counts do not match repetitions");
        }
        if (callbackFailures.size() != repetitions) {
            throw new IllegalStateException("Expected " + repetitions + " contained failures, got "
                    + callbackFailures.size());
        }
        @Unmodifiable List<Throwable> failures = callbackFailures.drain();
        if (failures.size() != repetitions || failures.stream()
                .anyMatch(failure -> !(failure instanceof DeliberateCallbackFailure))) {
            throw new IllegalStateException("WndProc failure queue contained an unexpected failure");
        }
        if (maximumCallbackDepth < 2) {
            throw new IllegalStateException("Synchronous WndProc reentrancy was not observed");
        }
        if (paintEvents < 1 || !closeObserved || !destroyObserved || !quitObserved) {
            throw new IllegalStateException("Win32 lifecycle did not reach paint, close, destroy, and quit");
        }
        if (elapsedMillis < Duration.ofSeconds(soakSeconds).toMillis()) {
            throw new IllegalStateException("Soak duration was shorter than requested: " + elapsedMillis + " ms");
        }
        List<String> expected = expectedSequence(repetitions);
        if (!eventSequence.equals(expected)) {
            throw new IllegalStateException("Normalized event sequence mismatch: expected " + expected
                    + ", got " + eventSequence);
        }
    }

    /// Builds the exact normalized sequence for the configured cycle count.
    ///
    /// @param repetitions the cycle count
    /// @return the immutable expected sequence
    private static @Unmodifiable List<String> expectedSequence(int repetitions) {
        List<String> expected = new ArrayList<>(repetitions * 8 + 2);
        for (int cycle = 0; cycle < repetitions; cycle++) {
            String prefix = "cycle-" + cycle + ':';
            expected.add(prefix + "resize");
            expected.add(prefix + "reentrant-enter");
            expected.add(prefix + "nested");
            expected.add(prefix + "reentrant-exit");
            expected.add(prefix + "failure-contained");
            expected.add(prefix + "pointer");
            expected.add(prefix + "key");
            expected.add(prefix + "character");
        }
        expected.add("close");
        expected.add("destroy");
        return List.copyOf(expected);
    }

    /// Returns one cycle-qualified normalized event name.
    ///
    /// @param name the event name
    /// @return the qualified name
    private String event(String name) {
        return "cycle-" + currentCycle + ':' + name;
    }

    /// Packs unsigned client coordinates into a Win32 `LPARAM`.
    ///
    /// @param x the x coordinate
    /// @param y the y coordinate
    /// @return the packed parameter
    private static long packCoordinates(int x, int y) {
        return ((long) y & 0xffffL) << 16 | ((long) x & 0xffffL);
    }

    /// Computes `value * numerator / denominator` without overflowing for profile-scale durations.
    ///
    /// @param value the duration value
    /// @param numerator the positive numerator
    /// @param denominator the positive denominator
    /// @return the scaled duration
    private static long multiplyFraction(long value, long numerator, long denominator) {
        return Math.addExact(
                Math.multiplyExact(value / denominator, numerator),
                Math.multiplyExact(value % denominator, numerator) / denominator
        );
    }

    /// Extracts a non-null pointer result or throws with its captured Windows error.
    ///
    /// @param result the generated call result
    /// @param operation the operation name
    /// @return the non-null native value
    private static MemorySegment requireValue(
            Win32FfmBindings.GetModuleHandleWResult result,
            String operation
    ) {
        if (isNull(result.value())) {
            throw windowsFailure(operation, result.errorCode());
        }
        return result.value();
    }

    /// Creates one Win32 failure with unsigned decimal and hexadecimal error representations.
    ///
    /// @param operation the failed operation
    /// @param errorCode the captured `GetLastError` value
    /// @return the failure exception
    private static IllegalStateException windowsFailure(String operation, int errorCode) {
        return new IllegalStateException(operation + " failed with Win32 error "
                + Integer.toUnsignedString(errorCode) + " (0x" + Integer.toHexString(errorCode) + ')');
    }

    /// Returns whether a native pointer has address zero.
    ///
    /// @param segment the native address
    /// @return whether the address is null
    private static boolean isNull(MemorySegment segment) {
        return segment.address() == 0L;
    }

    /// Evaluates one message-pump completion condition.
    @FunctionalInterface
    @NotNullByDefault
    private interface BooleanCondition {
        /// Returns whether pumping may stop.
        ///
        /// @return whether the awaited condition holds
        boolean test();
    }

    /// Increments one exact target-event counter.
    @FunctionalInterface
    @NotNullByDefault
    private interface Counter {
        /// Increments the selected counter once.
        void increment();
    }

    /// Marks the only application failure deliberately injected by this scenario.
    @NotNullByDefault
    private static final class DeliberateCallbackFailure extends RuntimeException {
        /// Stable serialization identifier.
        private static final long serialVersionUID = 1L;

        /// Creates one deliberate callback failure.
        ///
        /// @param message the diagnostic message
        private DeliberateCallbackFailure(String message) {
            super(message);
        }
    }
}
