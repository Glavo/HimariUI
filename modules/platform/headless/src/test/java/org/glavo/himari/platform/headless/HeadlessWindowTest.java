package org.glavo.himari.platform.headless;

import org.glavo.himari.platform.api.DisplayColorDescription;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.PhysicalSize;
import org.glavo.himari.platform.api.SurfaceKind;
import org.glavo.himari.platform.api.SurfaceRole;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowEvent;
import org.glavo.himari.platform.api.WindowEventType;
import org.glavo.himari.platform.api.WindowLifecycle;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowSnapshot;
import org.glavo.himari.platform.api.WindowState;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies virtual-window lifecycle, popup ownership, coalescing, failure, and cross-thread ingress.
@NotNullByDefault
final class HeadlessWindowTest {
    /// Verifies asynchronous creation, atomic configuration, redraw coalescing, and explicit close.
    @Test
    void runsCompleteWindowLifecycleWithoutReentrantCallbacks() {
        HeadlessPlatform platform = openPlatform();
        WindowConfiguration initial = configuration("Initial", 10.0, 20.0, 100.0, 50.0, true);
        ArrayList<WindowEvent> events = new ArrayList<>();
        CompletableFuture<HeadlessWindow> creation = platform.createWindow(
                WindowRequest.toplevel(initial),
                events::add
        ).toCompletableFuture();

        assertFalse(creation.isDone());
        assertTrue(events.isEmpty());
        platform.eventLoop().runUntilIdle();
        HeadlessWindow window = creation.join();
        assertEquals(List.of(WindowEventType.CREATED), eventTypes(events));
        assertEquals(SurfaceKind.SOFTWARE, window.surface().kind());
        assertEquals(SurfaceRole.TOPLEVEL, window.surface().role());
        assertEquals(0L, window.snapshot().configurationGeneration());

        events.clear();
        CompletableFuture<WindowSnapshot> unchanged = window.configure(initial).toCompletableFuture();
        assertFalse(unchanged.isDone());
        platform.eventLoop().runUntilIdle();
        assertSame(window.snapshot(), unchanged.join());
        assertTrue(events.isEmpty());

        WindowConfiguration replacement = configuration(
                "Replacement",
                30.0,
                40.0,
                120.5,
                60.25,
                true
        );
        window.configure(replacement);
        window.requestRedraw();
        window.requestRedraw();
        window.injectCloseRequest();
        platform.eventLoop().runUntilIdle();

        assertEquals(List.of(
                WindowEventType.CONFIGURATION_CHANGED,
                WindowEventType.REDRAW_REQUESTED,
                WindowEventType.CLOSE_REQUESTED
        ), eventTypes(events));
        assertEquals(1L, window.snapshot().configurationGeneration());
        assertEquals(replacement, window.snapshot().configuration());
        assertFalse(window.isClosed());

        events.clear();
        CompletableFuture<Void> close = window.closeAsync().toCompletableFuture();
        assertFalse(close.isDone());
        platform.eventLoop().runUntilIdle();
        assertTrue(close.isDone());
        assertTrue(window.isClosed());
        assertEquals(WindowLifecycle.CLOSED, window.snapshot().lifecycle());
        assertFalse(window.snapshot().effectivelyVisible());
        assertEquals(List.of(WindowEventType.CLOSED), eventTypes(events));
        assertEquals(0, platform.retainedWindowCount());
        assertSame(close, window.closeAsync());

        CompletableFuture<WindowSnapshot> rejected = window.configure(replacement).toCompletableFuture();
        platform.eventLoop().runUntilIdle();
        CompletionException failure = assertThrows(CompletionException.class, rejected::join);
        assertInstanceOf(IllegalStateException.class, failure.getCause());
    }

    /// Verifies that mutations requested by completion dependents follow the already queued event.
    @Test
    void ordersCompletionDependentMutationsAfterCreatedEvent() {
        HeadlessPlatform platform = openPlatform();
        ArrayList<WindowEvent> events = new ArrayList<>();
        WindowConfiguration initial = configuration("Initial", 0.0, 0.0, 10.0, 10.0, true);
        WindowConfiguration replacement = configuration("Replacement", 1.0, 1.0, 10.0, 10.0, true);
        CompletableFuture<HeadlessWindow> creation = platform.createWindow(
                WindowRequest.toplevel(initial),
                events::add
        ).toCompletableFuture();
        creation.thenAccept(window -> window.configure(replacement));

        platform.eventLoop().runUntilIdle();

        HeadlessWindow window = creation.join();
        assertEquals(List.of(WindowEventType.CREATED, WindowEventType.CONFIGURATION_CHANGED),
                eventTypes(events));
        assertEquals(initial, events.getFirst().snapshot().configuration());
        assertEquals(replacement, events.getLast().snapshot().configuration());
        assertEquals(replacement, window.snapshot().configuration());
    }

    /// Verifies owner-relative popup placement, inherited visibility, and child-first closure.
    @Test
    void updatesPopupTreeAtomicallyAndClosesChildFirst() {
        HeadlessPlatform platform = openPlatform();
        ArrayList<WindowEvent> orderedEvents = new ArrayList<>();
        HeadlessWindow owner = createWindow(
                platform,
                WindowRequest.toplevel(configuration("Owner", 100.0, 100.0, 300.0, 200.0, true)),
                orderedEvents
        );
        HeadlessWindow popup = createWindow(
                platform,
                WindowRequest.popup(
                        owner.id(),
                        configuration("Popup", 10.0, 20.0, 80.0, 40.0, true)
                ),
                orderedEvents
        );
        HeadlessWindow nested = createWindow(
                platform,
                WindowRequest.popup(
                        popup.id(),
                        configuration("Nested", 5.0, 6.0, 20.0, 10.0, true)
                ),
                orderedEvents
        );
        orderedEvents.clear();

        assertEquals(new LogicalRect(110.0, 120.0, 80.0, 40.0), popup.snapshot().effectiveFrame());
        assertEquals(new LogicalRect(115.0, 126.0, 20.0, 10.0), nested.snapshot().effectiveFrame());
        assertTrue(popup.snapshot().effectivelyVisible());
        assertTrue(nested.snapshot().effectivelyVisible());

        owner.configure(configuration("Owner", 200.0, 300.0, 300.0, 200.0, false));
        platform.eventLoop().runUntilIdle();

        assertEquals(List.of(owner.id(), popup.id(), nested.id()),
                orderedEvents.stream().map(event -> event.snapshot().id()).toList());
        assertEquals(new LogicalRect(210.0, 320.0, 80.0, 40.0), popup.snapshot().effectiveFrame());
        assertEquals(new LogicalRect(215.0, 326.0, 20.0, 10.0), nested.snapshot().effectiveFrame());
        assertFalse(owner.snapshot().effectivelyVisible());
        assertFalse(popup.snapshot().effectivelyVisible());
        assertFalse(nested.snapshot().effectivelyVisible());

        orderedEvents.clear();
        owner.closeAsync();
        platform.eventLoop().runUntilIdle();

        assertEquals(List.of(nested.id(), popup.id(), owner.id()),
                orderedEvents.stream().map(event -> event.snapshot().id()).toList());
        assertTrue(orderedEvents.stream().allMatch(event -> event.type() == WindowEventType.CLOSED));
        assertTrue(owner.isClosed());
        assertTrue(popup.isClosed());
        assertTrue(nested.isClosed());
        assertEquals(0, platform.retainedWindowCount());
    }

    /// Verifies deterministic maximize, full-screen, minimize, and restored-frame semantics.
    @Test
    void appliesTopLevelPresentationStates() {
        LogicalRect bounds = new LogicalRect(0.0, 0.0, 1_000.0, 800.0);
        LogicalRect workArea = new LogicalRect(0.0, 0.0, 1_000.0, 760.0);
        HeadlessDisplayConfiguration display = new HeadlessDisplayConfiguration(
                HeadlessDisplayConfiguration.DEFAULT_DISPLAY_ID,
                bounds,
                workArea,
                new PhysicalSize(1_000, 800),
                1.0,
                true,
                DisplayColorDescription.SRGB_SDR
        );
        HeadlessPlatform platform = new HeadlessBackend(List.of(display), 0L)
                .open().toCompletableFuture().join();
        LogicalRect restoredFrame = new LogicalRect(100.0, 100.0, 300.0, 200.0);
        HeadlessWindow window = createWindow(
                platform,
                WindowRequest.toplevel(new WindowConfiguration(
                        "Window",
                        restoredFrame,
                        true,
                        WindowState.NORMAL
                )),
                new ArrayList<>()
        );

        window.configure(new WindowConfiguration("Window", restoredFrame, true, WindowState.MAXIMIZED));
        platform.eventLoop().runUntilIdle();
        assertEquals(workArea, window.snapshot().effectiveFrame());
        assertEquals(new PhysicalSize(1_000, 760), window.snapshot().surfaceSize());

        window.configure(new WindowConfiguration("Window", restoredFrame, true, WindowState.FULLSCREEN));
        platform.eventLoop().runUntilIdle();
        assertEquals(bounds, window.snapshot().effectiveFrame());
        assertEquals(new PhysicalSize(1_000, 800), window.snapshot().surfaceSize());

        window.configure(new WindowConfiguration("Window", restoredFrame, true, WindowState.MINIMIZED));
        platform.eventLoop().runUntilIdle();
        assertEquals(restoredFrame, window.snapshot().effectiveFrame());
        assertFalse(window.snapshot().effectivelyVisible());
    }

    /// Verifies that callback failure preserves committed state and later callback queue entries.
    @Test
    void containsCallbackFailureAtEventLoopTaskBoundary() {
        HeadlessPlatform platform = openPlatform();
        AtomicBoolean failFirstCallback = new AtomicBoolean(true);
        ArrayList<String> successfulCallbacks = new ArrayList<>();
        CompletableFuture<HeadlessWindow> first = platform.createWindow(
                WindowRequest.toplevel(configuration("First", 0.0, 0.0, 10.0, 10.0, true)),
                event -> {
                    if (failFirstCallback.getAndSet(false)) {
                        throw new IllegalStateException("planned callback failure");
                    }
                    successfulCallbacks.add("first-" + event.type());
                }
        ).toCompletableFuture();
        CompletableFuture<HeadlessWindow> second = platform.createWindow(
                WindowRequest.toplevel(configuration("Second", 20.0, 0.0, 10.0, 10.0, true)),
                event -> successfulCallbacks.add("second-" + event.type())
        ).toCompletableFuture();

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                platform.eventLoop()::runUntilIdle
        );
        assertEquals("planned callback failure", failure.getMessage());
        assertTrue(first.isDone());
        assertTrue(second.isDone());
        assertFalse(first.join().isClosed());
        assertFalse(second.join().isClosed());

        platform.eventLoop().runUntilIdle();
        assertEquals(List.of("second-CREATED"), successfulCallbacks);
        first.join().configure(configuration("Updated", 0.0, 0.0, 10.0, 10.0, true));
        platform.eventLoop().runUntilIdle();
        assertEquals(List.of("second-CREATED", "first-CONFIGURATION_CHANGED"), successfulCallbacks);
    }

    /// Verifies that a descendant calculation failure leaves the complete owner tree unchanged.
    @Test
    void rollsBackOwnerTreeWhenPopupRecalculationFails() {
        HeadlessPlatform platform = openPlatform();
        ArrayList<WindowEvent> events = new ArrayList<>();
        HeadlessWindow owner = createWindow(
                platform,
                WindowRequest.toplevel(configuration("Owner", 0.0, 0.0, 0.0, 0.0, true)),
                events
        );
        HeadlessWindow popup = createWindow(
                platform,
                WindowRequest.popup(
                        owner.id(),
                        configuration("Popup", 1.0e308, 0.0, 0.0, 0.0, true)
                ),
                events
        );
        WindowSnapshot ownerBefore = owner.snapshot();
        WindowSnapshot popupBefore = popup.snapshot();
        events.clear();

        CompletableFuture<WindowSnapshot> replacement = owner.configure(configuration(
                "Owner moved",
                1.0e308,
                0.0,
                0.0,
                0.0,
                true
        )).toCompletableFuture();
        platform.eventLoop().runUntilIdle();

        CompletionException failure = assertThrows(CompletionException.class, replacement::join);
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        assertSame(ownerBefore, owner.snapshot());
        assertSame(popupBefore, popup.snapshot());
        assertTrue(events.isEmpty());
    }

    /// Verifies cross-thread submission and owner-thread callback execution.
    ///
    /// @throws InterruptedException if the test thread is interrupted while joining the worker
    @Test
    void acceptsCrossThreadIngressButDispatchesOnOwner() throws InterruptedException {
        HeadlessPlatform platform = openPlatform();
        Thread ownerThread = Thread.currentThread();
        AtomicReference<@Nullable CompletableFuture<HeadlessWindow>> creationReference =
                new AtomicReference<>();
        AtomicReference<@Nullable Thread> callbackThread = new AtomicReference<>();
        Thread worker = Thread.ofPlatform().name("headless-platform-worker").start(() ->
                creationReference.set(platform.createWindow(
                        WindowRequest.toplevel(configuration(
                                "Worker",
                                0.0,
                                0.0,
                                20.0,
                                20.0,
                                true
                        )),
                        event -> callbackThread.set(Thread.currentThread())
                ).toCompletableFuture())
        );
        worker.join();

        CompletableFuture<HeadlessWindow> creation = require(creationReference.get());
        assertFalse(creation.isDone());
        platform.eventLoop().runUntilIdle();
        assertTrue(creation.isDone());
        assertSame(ownerThread, callbackThread.get());
    }

    /// Verifies orderly session shutdown closes windows before closing the host loop.
    @Test
    void closesSessionAfterQueuedWindowCallbacks() {
        HeadlessPlatform platform = openPlatform();
        ArrayList<WindowEvent> events = new ArrayList<>();
        HeadlessWindow window = createWindow(
                platform,
                WindowRequest.toplevel(configuration("Window", 0.0, 0.0, 10.0, 10.0, true)),
                events
        );
        events.clear();

        CompletableFuture<Void> close = platform.closeAsync().toCompletableFuture();
        assertFalse(close.isDone());
        platform.eventLoop().runUntilIdle();

        assertTrue(close.isDone());
        assertTrue(platform.isClosed());
        assertTrue(platform.eventLoop().isClosed());
        assertTrue(window.isClosed());
        assertEquals(0, platform.retainedWindowCount());
        assertEquals(List.of(WindowEventType.CLOSED), eventTypes(events));
        assertThrows(IllegalStateException.class, () -> platform.createWindow(
                WindowRequest.toplevel(configuration("Late", 0.0, 0.0, 1.0, 1.0, true)),
                event -> {
                }
        ));
    }

    /// Creates and dispatches one window before returning it.
    ///
    /// @param platform the owning session
    /// @param request the creation request
    /// @param events the event destination
    /// @return the created window
    private static HeadlessWindow createWindow(
            HeadlessPlatform platform,
            WindowRequest request,
            List<WindowEvent> events
    ) {
        CompletableFuture<HeadlessWindow> completion = platform.createWindow(request, events::add)
                .toCompletableFuture();
        platform.eventLoop().runUntilIdle();
        return completion.join();
    }

    /// Creates a normal-state window configuration.
    ///
    /// @param title the title
    /// @param x the logical horizontal origin
    /// @param y the logical vertical origin
    /// @param width the logical width
    /// @param height the logical height
    /// @param visible whether presentation is requested
    /// @return the configuration
    private static WindowConfiguration configuration(
            String title,
            double x,
            double y,
            double width,
            double height,
            boolean visible
    ) {
        return new WindowConfiguration(
                title,
                new LogicalRect(x, y, width, height),
                visible,
                WindowState.NORMAL
        );
    }

    /// Returns event types in their delivered order.
    ///
    /// @param events the events
    /// @return the event types
    private static List<WindowEventType> eventTypes(List<WindowEvent> events) {
        return events.stream().map(WindowEvent::type).toList();
    }

    /// Requires a non-null test value.
    ///
    /// @param value the candidate value
    /// @param <T> the value type
    /// @return the non-null value
    private static <T> T require(@Nullable T value) {
        if (value == null) {
            throw new AssertionError("Expected non-null test value");
        }
        return value;
    }

    /// Opens one default Headless session.
    ///
    /// @return the session
    private static HeadlessPlatform openPlatform() {
        return new HeadlessBackend().open().toCompletableFuture().join();
    }
}
