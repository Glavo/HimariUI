package org.glavo.himari.platform.headless;

import org.glavo.himari.platform.api.DisplayColorCapabilities;
import org.glavo.himari.platform.api.DisplayEvent;
import org.glavo.himari.platform.api.DisplayEventHandler;
import org.glavo.himari.platform.api.DisplayEventType;
import org.glavo.himari.platform.api.DisplayId;
import org.glavo.himari.platform.api.DisplaySnapshot;
import org.glavo.himari.platform.api.DisplayTopologySnapshot;
import org.glavo.himari.platform.api.ListenerRegistration;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.PhysicalSize;
import org.glavo.himari.platform.api.PlatformSession;
import org.glavo.himari.platform.api.SurfaceDescriptor;
import org.glavo.himari.platform.api.SurfaceId;
import org.glavo.himari.platform.api.SurfaceKind;
import org.glavo.himari.platform.api.SurfaceRole;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowEvent;
import org.glavo.himari.platform.api.WindowEventHandler;
import org.glavo.himari.platform.api.WindowEventType;
import org.glavo.himari.platform.api.WindowId;
import org.glavo.himari.platform.api.WindowLifecycle;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowSnapshot;
import org.glavo.himari.platform.api.WindowState;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/// Implements a deterministic virtual platform session with programmable displays and windows.
///
/// All topology and window transitions commit on the [HeadlessEventLoop] owner thread. Immutable
/// snapshots are published before callbacks are queued, display and window event callbacks never run
/// reentrantly inside a mutation, and one display replacement updates every affected window before
/// publishing callbacks. Callback failures propagate from event-loop dispatch without rolling back
/// committed platform state; remaining callbacks retain their queue order for a later drain.
@NotNullByDefault
public final class HeadlessPlatform implements PlatformSession<HeadlessWindow> {
    /// The deterministic host event loop and manual clock.
    private final HeadlessEventLoop eventLoop;

    /// The monitor serializing submission against session shutdown.
    private final Object lifecycleLock = new Object();

    /// Current display configurations in enumeration order; owner-thread confined.
    private final LinkedHashMap<DisplayId, HeadlessDisplayConfiguration> displayConfigurations =
            new LinkedHashMap<>();

    /// All windows in creation order, including closed windows; owner-thread confined.
    private final LinkedHashMap<WindowId, HeadlessWindow> windows = new LinkedHashMap<>();

    /// Active display listener registrations with thread-safe cancellation.
    private final CopyOnWriteArrayList<DisplayRegistration> displayRegistrations =
            new CopyOnWriteArrayList<>();

    /// The shared session-close completion.
    private final CompletableFuture<Void> closeCompletion = new CompletableFuture<>();

    /// The latest atomic display enumeration and generation published for cross-thread reads.
    private volatile DisplayTopologySnapshot displayTopology;

    /// Whether new session operations may be submitted, guarded by [#lifecycleLock].
    private boolean accepting = true;

    /// Whether final shutdown completed, published for cross-thread reads.
    private volatile boolean closed;

    /// The next positive window and software-surface identifier; owner-thread confined.
    private long nextWindowId = 1L;

    /// The next positive total event-order sequence; owner-thread confined.
    private long nextEventSequence = 1L;

    /// Creates an initialized Headless session on the event-loop owner thread.
    ///
    /// @param eventLoop the session event loop
    /// @param initialDisplays the validated initial display topology
    HeadlessPlatform(
            HeadlessEventLoop eventLoop,
            @Unmodifiable List<HeadlessDisplayConfiguration> initialDisplays
    ) {
        this.eventLoop = Objects.requireNonNull(eventLoop, "eventLoop");
        eventLoop.checkOwnerThread();
        List<HeadlessDisplayConfiguration> configurations = validatedDisplayConfigurations(initialDisplays);
        ArrayList<DisplaySnapshot> snapshots = new ArrayList<>(configurations.size());
        for (int index = 0; index < configurations.size(); index++) {
            HeadlessDisplayConfiguration configuration = configurations.get(index);
            displayConfigurations.put(configuration.id(), configuration);
            snapshots.add(displaySnapshot(configuration, index, 0L, 0L));
        }
        displayTopology = new DisplayTopologySnapshot(0L, snapshots);
    }

    /// Returns the manual frame clock.
    ///
    /// @return the session clock
    @Override
    public ManualFrameClock clock() {
        return eventLoop.clock();
    }

    /// Returns the deterministic event loop.
    ///
    /// @return the event loop
    @Override
    public HeadlessEventLoop eventLoop() {
        return eventLoop;
    }

    /// Returns the latest immutable display enumeration.
    ///
    /// @return the current display snapshots
    @Override
    public @Unmodifiable List<DisplaySnapshot> displays() {
        return displayTopology.displays();
    }

    /// Returns the atomic display enumeration and topology generation.
    ///
    /// @return the current display topology
    @Override
    public DisplayTopologySnapshot displayTopology() {
        return displayTopology;
    }

    /// Returns the current display-topology generation.
    ///
    /// @return the nonnegative topology generation
    @Override
    public long displayTopologyGeneration() {
        return displayTopology.generation();
    }

    /// Registers a display listener without replaying the current topology.
    ///
    /// @param handler the listener callback
    /// @return the cancellable listener registration
    /// @throws IllegalStateException if session shutdown began
    @Override
    public ListenerRegistration addDisplayEventHandler(DisplayEventHandler handler) {
        Objects.requireNonNull(handler, "handler");
        synchronized (lifecycleLock) {
            checkAcceptingUnderLock();
            DisplayRegistration registration = new DisplayRegistration(this, handler);
            displayRegistrations.add(registration);
            return registration;
        }
    }

    /// Requests atomic replacement of the complete virtual display topology.
    ///
    /// The replacement must be nonempty, use unique identifiers, and contain exactly one primary
    /// display. Semantically unchanged replacement completes without advancing generations or
    /// publishing events. Display and all affected window snapshots commit together before any
    /// callback runs.
    ///
    /// @param configurations the complete display topology in enumeration order
    /// @return a stage completed with the resulting topology generation
    /// @throws IllegalArgumentException if the topology is invalid
    /// @throws IllegalStateException if session shutdown began
    public CompletionStage<Long> replaceDisplays(
            @Unmodifiable List<HeadlessDisplayConfiguration> configurations
    ) {
        List<HeadlessDisplayConfiguration> replacement = validatedDisplayConfigurations(configurations);
        CompletableFuture<Long> completion = new CompletableFuture<>();
        postAccepted(() -> {
            try {
                replaceDisplaysNow(replacement, completion);
            } catch (RuntimeException | Error failure) {
                completion.completeExceptionally(failure);
            }
        });
        return completion;
    }

    /// Requests asynchronous virtual-window creation.
    ///
    /// @param request the complete window request
    /// @param eventHandler the application event callback
    /// @return a stage completed with the committed window before its created callback runs
    /// @throws IllegalStateException if session shutdown began
    @Override
    public CompletionStage<HeadlessWindow> createWindow(
            WindowRequest request,
            WindowEventHandler eventHandler
    ) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(eventHandler, "eventHandler");
        CompletableFuture<HeadlessWindow> completion = new CompletableFuture<>();
        postAccepted(() -> {
            try {
                createWindowNow(request, eventHandler, completion);
            } catch (RuntimeException | Error failure) {
                completion.completeExceptionally(failure);
            }
        });
        return completion;
    }

    /// Requests orderly child-first closure of every virtual window and the event loop.
    ///
    /// @return the shared stage completed after close callbacks and final shutdown work
    /// @throws IllegalStateException if the event loop was independently closed before submission
    @Override
    public CompletionStage<Void> closeAsync() {
        synchronized (lifecycleLock) {
            if (!accepting) {
                return closeCompletion;
            }
            accepting = false;
            try {
                eventLoop.post(this::beginSessionClose);
            } catch (RuntimeException | Error failure) {
                accepting = true;
                throw failure;
            }
            return closeCompletion;
        }
    }

    /// Returns whether final session shutdown completed.
    ///
    /// @return whether the session is closed
    @Override
    public boolean isClosed() {
        return closed;
    }

    /// Validates and snapshots a complete display configuration list.
    ///
    /// @param configurations the candidate list
    /// @return the immutable validated snapshot
    /// @throws IllegalArgumentException if the list is empty, repeats an identifier, or does not
    /// contain exactly one primary display
    static @Unmodifiable List<HeadlessDisplayConfiguration> validatedDisplayConfigurations(
            @Unmodifiable List<HeadlessDisplayConfiguration> configurations
    ) {
        Objects.requireNonNull(configurations, "configurations");
        List<HeadlessDisplayConfiguration> copy = List.copyOf(configurations);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("A Headless session requires at least one virtual display");
        }
        Set<DisplayId> identifiers = new HashSet<>();
        int primaryCount = 0;
        for (HeadlessDisplayConfiguration configuration : copy) {
            Objects.requireNonNull(configuration, "display configuration");
            if (!identifiers.add(configuration.id())) {
                throw new IllegalArgumentException("Duplicate virtual display identifier: " + configuration.id());
            }
            if (configuration.primary()) {
                primaryCount++;
            }
        }
        if (primaryCount != 1) {
            throw new IllegalArgumentException("A Headless topology must contain exactly one primary display");
        }
        return copy;
    }

    /// Submits a window configuration replacement.
    ///
    /// @param window the target window
    /// @param configuration the complete replacement configuration
    /// @return a stage completed with the committed target snapshot
    CompletionStage<WindowSnapshot> configureWindow(
            HeadlessWindow window,
            WindowConfiguration configuration
    ) {
        CompletableFuture<WindowSnapshot> completion = new CompletableFuture<>();
        postAccepted(() -> {
            try {
                configureWindowNow(window, configuration, completion);
            } catch (RuntimeException | Error failure) {
                completion.completeExceptionally(failure);
            }
        });
        return completion;
    }

    /// Submits a coalescible redraw request.
    ///
    /// @param window the target window
    void requestRedraw(HeadlessWindow window) {
        postAccepted(() -> requestRedrawNow(window));
    }

    /// Submits a synthetic host close request.
    ///
    /// @param window the target window
    void injectCloseRequest(HeadlessWindow window) {
        postAccepted(() -> injectCloseRequestNow(window));
    }

    /// Submits explicit child-first window closure.
    ///
    /// @param window the root window to close
    void closeWindow(HeadlessWindow window) {
        postAccepted(() -> closeWindowNow(window));
    }

    /// Replaces display state and affected window state atomically on the owner thread.
    ///
    /// @param replacement the validated replacement topology
    /// @param completion the operation completion
    private void replaceDisplaysNow(
            @Unmodifiable List<HeadlessDisplayConfiguration> replacement,
            CompletableFuture<Long> completion
    ) {
        eventLoop.checkOwnerThread();
        List<HeadlessDisplayConfiguration> previousConfigurations = List.copyOf(displayConfigurations.values());
        if (previousConfigurations.equals(replacement)) {
            completion.complete(displayTopology.generation());
            return;
        }

        long nextTopologyGeneration = incrementGeneration(
                displayTopology.generation(),
                "Display topology generation"
        );
        Map<DisplayId, HeadlessDisplayConfiguration> previousConfigurationById = new HashMap<>();
        for (HeadlessDisplayConfiguration configuration : previousConfigurations) {
            previousConfigurationById.put(configuration.id(), configuration);
        }
        Map<DisplayId, DisplaySnapshot> previousSnapshotById = new HashMap<>();
        for (DisplaySnapshot snapshot : displayTopology.displays()) {
            previousSnapshotById.put(snapshot.id(), snapshot);
        }

        ArrayList<DisplaySnapshot> nextSnapshots = new ArrayList<>(replacement.size());
        for (int index = 0; index < replacement.size(); index++) {
            HeadlessDisplayConfiguration configuration = replacement.get(index);
            @Nullable HeadlessDisplayConfiguration previousConfiguration =
                    previousConfigurationById.get(configuration.id());
            @Nullable DisplaySnapshot previousSnapshot = previousSnapshotById.get(configuration.id());
            long configurationGeneration = 0L;
            long colorGeneration = 0L;
            if (previousConfiguration != null && previousSnapshot != null) {
                boolean configurationChanged = !previousConfiguration.equals(configuration)
                        || previousSnapshot.enumerationIndex() != index;
                configurationGeneration = configurationChanged
                        ? incrementGeneration(
                                previousSnapshot.configurationGeneration(),
                                "Display configuration generation"
                        )
                        : previousSnapshot.configurationGeneration();
                colorGeneration = previousConfiguration.colorDescription().equals(configuration.colorDescription())
                        ? previousSnapshot.colorCapabilities().generation()
                        : incrementGeneration(
                                previousSnapshot.colorCapabilities().generation(),
                                "Display color capability generation"
                        );
            }
            nextSnapshots.add(displaySnapshot(
                    configuration,
                    index,
                    configurationGeneration,
                    colorGeneration
            ));
        }
        List<DisplaySnapshot> immutableNextSnapshots = List.copyOf(nextSnapshots);
        WindowRecalculation recalculation = recalculateWindows(immutableNextSnapshots, null, null);

        long timestamp = clock().nowNanos();
        ArrayList<DisplayEvent> displayEvents = createDisplayEvents(
                previousConfigurations,
                previousSnapshotById,
                replacement,
                immutableNextSnapshots,
                nextTopologyGeneration,
                timestamp
        );
        ArrayList<QueuedWindowEvent> windowEvents = createWindowEvents(recalculation.transitions(), timestamp);

        displayConfigurations.clear();
        for (HeadlessDisplayConfiguration configuration : replacement) {
            displayConfigurations.put(configuration.id(), configuration);
        }
        displayTopology = new DisplayTopologySnapshot(nextTopologyGeneration, immutableNextSnapshots);
        publishWindowTransitions(recalculation.transitions());

        queueDisplayEvents(displayEvents);
        queueWindowEvents(windowEvents);
        completion.complete(nextTopologyGeneration);
    }

    /// Creates a window and queues its created callback on the owner thread.
    ///
    /// @param request the creation request
    /// @param eventHandler the window callback
    /// @param completion the creation completion
    private void createWindowNow(
            WindowRequest request,
            WindowEventHandler eventHandler,
            CompletableFuture<HeadlessWindow> completion
    ) {
        eventLoop.checkOwnerThread();
        @Nullable HeadlessWindow owner = null;
        if (request.ownerId() != null) {
            owner = windows.get(request.ownerId());
            if (owner == null || owner.isClosed()) {
                throw new IllegalArgumentException("Popup owner is absent or closed: " + request.ownerId());
            }
        }
        if (nextWindowId == Long.MAX_VALUE) {
            throw new IllegalStateException("Headless window identifiers are exhausted");
        }

        WindowId id = new WindowId(nextWindowId);
        SurfaceDescriptor surface = new SurfaceDescriptor(
                new SurfaceId(nextWindowId),
                request.role(),
                SurfaceKind.SOFTWARE
        );
        nextWindowId++;
        LogicalRect requestedGlobalFrame = requestedGlobalFrame(
                request.role(),
                request.configuration(),
                owner
        );
        boolean effectivelyVisible = effectiveVisibility(request.configuration(), owner);
        DisplaySnapshot display = selectDisplay(requestedGlobalFrame, displayTopology.displays());
        LogicalRect effectiveFrame = applyWindowState(
                request.role(),
                request.configuration(),
                requestedGlobalFrame,
                display
        );
        WindowSnapshot initialSnapshot = new WindowSnapshot(
                id,
                request.role(),
                request.ownerId(),
                request.configuration(),
                effectiveFrame,
                effectivelyVisible,
                physicalSize(effectiveFrame, display.scaleFactor()),
                display.scaleFactor(),
                display.id(),
                surface,
                0L,
                WindowLifecycle.OPEN
        );
        HeadlessWindow window = new HeadlessWindow(this, initialSnapshot, eventHandler);
        WindowEvent event = new WindowEvent(
                allocateEventSequence(),
                clock().nowNanos(),
                WindowEventType.CREATED,
                initialSnapshot,
                null
        );

        windows.put(id, window);
        queueWindowEvent(new QueuedWindowEvent(window, event));
        completion.complete(window);
    }

    /// Applies a window configuration and any owner-derived descendant changes atomically.
    ///
    /// @param window the target window
    /// @param configuration the replacement configuration
    /// @param completion the operation completion
    private void configureWindowNow(
            HeadlessWindow window,
            WindowConfiguration configuration,
        CompletableFuture<WindowSnapshot> completion
    ) {
        eventLoop.checkOwnerThread();
        if (window.isClosed()) {
            throw new IllegalStateException("Cannot configure a closed Headless window");
        }
        requireOwnedWindow(window);
        WindowRecalculation recalculation = recalculateWindows(
                displayTopology.displays(),
                window,
                configuration
        );
        WindowSnapshot targetSnapshot = recalculation.resolved().get(window);
        if (targetSnapshot == null) {
            throw new IllegalStateException("Window recalculation omitted its target");
        }
        long timestamp = clock().nowNanos();
        ArrayList<QueuedWindowEvent> events = createWindowEvents(recalculation.transitions(), timestamp);
        publishWindowTransitions(recalculation.transitions());
        queueWindowEvents(events);
        completion.complete(targetSnapshot);
    }

    /// Coalesces and queues one redraw callback on the owner thread.
    ///
    /// @param window the target window
    private void requestRedrawNow(HeadlessWindow window) {
        eventLoop.checkOwnerThread();
        if (window.isClosed()) {
            return;
        }
        requireOwnedWindow(window);
        if (window.redrawPending()) {
            return;
        }
        window.setRedrawPending(true);
        WindowEvent event = new WindowEvent(
                allocateEventSequence(),
                clock().nowNanos(),
                WindowEventType.REDRAW_REQUESTED,
                window.snapshot(),
                null
        );
        queueWindowEvent(new QueuedWindowEvent(window, event));
    }

    /// Queues one synthetic close-request callback on the owner thread.
    ///
    /// @param window the target window
    private void injectCloseRequestNow(HeadlessWindow window) {
        eventLoop.checkOwnerThread();
        if (window.isClosed()) {
            return;
        }
        requireOwnedWindow(window);
        WindowEvent event = new WindowEvent(
                allocateEventSequence(),
                clock().nowNanos(),
                WindowEventType.CLOSE_REQUESTED,
                window.snapshot(),
                null
        );
        queueWindowEvent(new QueuedWindowEvent(window, event));
    }

    /// Closes one window tree child-first on the owner thread.
    ///
    /// @param window the root window
    private void closeWindowNow(HeadlessWindow window) {
        eventLoop.checkOwnerThread();
        if (window.isClosed()) {
            return;
        }
        requireOwnedWindow(window);
        List<HeadlessWindow> closeOrder = closeOrderFor(window);
        ArrayList<QueuedWindowEvent> events = prepareCloseTransitions(closeOrder, clock().nowNanos());
        publishCloseTransitions(events);
        queueWindowEvents(events);
        completeWindowCloses(events);
    }

    /// Begins session shutdown, queues all close callbacks, then queues final event-loop closure.
    private void beginSessionClose() {
        eventLoop.checkOwnerThread();
        ArrayList<HeadlessWindow> closeOrder = new ArrayList<>();
        List<HeadlessWindow> creationOrder = List.copyOf(windows.values());
        for (int index = creationOrder.size() - 1; index >= 0; index--) {
            HeadlessWindow window = creationOrder.get(index);
            if (!window.isClosed()) {
                closeOrder.add(window);
            }
        }
        ArrayList<QueuedWindowEvent> events = prepareCloseTransitions(closeOrder, clock().nowNanos());
        publishCloseTransitions(events);
        queueWindowEvents(events);
        completeWindowCloses(events);
        eventLoop.post(this::finishSessionClose);
    }

    /// Finalizes session shutdown after all previously queued close callbacks.
    private void finishSessionClose() {
        eventLoop.checkOwnerThread();
        for (DisplayRegistration registration : displayRegistrations) {
            registration.cancel();
        }
        closed = true;
        eventLoop.close();
        closeCompletion.complete(null);
    }

    /// Recalculates every open window against a display topology and optional configuration override.
    ///
    /// @param displays the display topology used for association and scale
    /// @param overrideWindow the window receiving `overrideConfiguration`, or `null`
    /// @param overrideConfiguration the replacement configuration, or `null`
    /// @return resolved snapshots for every open window and changed transitions in creation order
    private WindowRecalculation recalculateWindows(
            @Unmodifiable List<DisplaySnapshot> displays,
            @Nullable HeadlessWindow overrideWindow,
            @Nullable WindowConfiguration overrideConfiguration
    ) {
        IdentityHashMap<HeadlessWindow, WindowSnapshot> resolved = new IdentityHashMap<>();
        ArrayList<WindowTransition> transitions = new ArrayList<>();
        for (HeadlessWindow window : windows.values()) {
            WindowSnapshot current = window.snapshot();
            if (current.lifecycle() == WindowLifecycle.CLOSED) {
                continue;
            }
            WindowConfiguration configuration = window == overrideWindow
                    ? Objects.requireNonNull(overrideConfiguration, "overrideConfiguration")
                    : current.configuration();
            @Nullable HeadlessWindow owner = null;
            @Nullable WindowSnapshot ownerSnapshot = null;
            if (current.ownerId() != null) {
                owner = windows.get(current.ownerId());
                if (owner == null) {
                    throw new IllegalStateException("Open popup has no owner: " + current.id());
                }
                ownerSnapshot = resolved.get(owner);
                if (ownerSnapshot == null || ownerSnapshot.lifecycle() != WindowLifecycle.OPEN) {
                    throw new IllegalStateException("Open popup owner is unresolved or closed: " + current.id());
                }
            }
            LogicalRect requestedGlobalFrame = requestedGlobalFrame(
                    current.role(),
                    configuration,
                    ownerSnapshot
            );
            boolean effectivelyVisible = effectiveVisibility(configuration, ownerSnapshot);
            DisplaySnapshot display = selectDisplay(requestedGlobalFrame, displays);
            LogicalRect effectiveFrame = applyWindowState(
                    current.role(),
                    configuration,
                    requestedGlobalFrame,
                    display
            );
            WindowSnapshot candidate = new WindowSnapshot(
                    current.id(),
                    current.role(),
                    current.ownerId(),
                    configuration,
                    effectiveFrame,
                    effectivelyVisible,
                    physicalSize(effectiveFrame, display.scaleFactor()),
                    display.scaleFactor(),
                    display.id(),
                    current.surface(),
                    current.configurationGeneration(),
                    current.lifecycle()
            );
            WindowSnapshot replacement = sameWindowState(current, candidate)
                    ? current
                    : new WindowSnapshot(
                            candidate.id(),
                            candidate.role(),
                            candidate.ownerId(),
                            candidate.configuration(),
                            candidate.effectiveFrame(),
                            candidate.effectivelyVisible(),
                            candidate.surfaceSize(),
                            candidate.scaleFactor(),
                            candidate.displayId(),
                            candidate.surface(),
                            incrementGeneration(
                                    current.configurationGeneration(),
                                    "Window configuration generation"
                            ),
                            candidate.lifecycle()
                    );
            resolved.put(window, replacement);
            if (replacement != current) {
                transitions.add(new WindowTransition(window, current, replacement));
            }
        }
        return new WindowRecalculation(resolved, List.copyOf(transitions));
    }

    /// Creates ordered display events for one atomic topology replacement.
    ///
    /// @param previousConfigurations the previous configurations in enumeration order
    /// @param previousSnapshots previous snapshots by identifier
    /// @param nextConfigurations the next configurations in enumeration order
    /// @param nextSnapshots the next snapshots in enumeration order
    /// @param topologyGeneration the new topology generation
    /// @param timestamp the shared transition timestamp
    /// @return the ordered display events
    private ArrayList<DisplayEvent> createDisplayEvents(
            @Unmodifiable List<HeadlessDisplayConfiguration> previousConfigurations,
            Map<DisplayId, DisplaySnapshot> previousSnapshots,
            @Unmodifiable List<HeadlessDisplayConfiguration> nextConfigurations,
            @Unmodifiable List<DisplaySnapshot> nextSnapshots,
            long topologyGeneration,
            long timestamp
    ) {
        Set<DisplayId> nextIdentifiers = new HashSet<>();
        for (HeadlessDisplayConfiguration configuration : nextConfigurations) {
            nextIdentifiers.add(configuration.id());
        }
        ArrayList<DisplayEvent> events = new ArrayList<>();
        for (HeadlessDisplayConfiguration previous : previousConfigurations) {
            if (!nextIdentifiers.contains(previous.id())) {
                events.add(new DisplayEvent(
                        allocateEventSequence(),
                        timestamp,
                        topologyGeneration,
                        DisplayEventType.REMOVED,
                        previousSnapshots.get(previous.id()),
                        null
                ));
            }
        }
        for (DisplaySnapshot current : nextSnapshots) {
            @Nullable DisplaySnapshot previous = previousSnapshots.get(current.id());
            if (previous == null) {
                events.add(new DisplayEvent(
                        allocateEventSequence(),
                        timestamp,
                        topologyGeneration,
                        DisplayEventType.ADDED,
                        null,
                        current
                ));
            } else if (!previous.equals(current)) {
                events.add(new DisplayEvent(
                        allocateEventSequence(),
                        timestamp,
                        topologyGeneration,
                        DisplayEventType.CHANGED,
                        previous,
                        current
                ));
            }
        }
        return events;
    }

    /// Creates window configuration events without publishing the prepared snapshots.
    ///
    /// @param transitions the prepared transitions
    /// @param timestamp the shared transition timestamp
    /// @return queued events in transition order
    private ArrayList<QueuedWindowEvent> createWindowEvents(
            @Unmodifiable List<WindowTransition> transitions,
            long timestamp
    ) {
        ArrayList<QueuedWindowEvent> events = new ArrayList<>(transitions.size());
        for (WindowTransition transition : transitions) {
            WindowEvent event = new WindowEvent(
                    allocateEventSequence(),
                    timestamp,
                    WindowEventType.CONFIGURATION_CHANGED,
                    transition.current(),
                    transition.previous()
            );
            events.add(new QueuedWindowEvent(transition.window(), event));
        }
        return events;
    }

    /// Prepares child-first close snapshots and events without publishing them.
    ///
    /// @param closeOrder open windows in child-first order
    /// @param timestamp the shared close timestamp
    /// @return queued close events
    private ArrayList<QueuedWindowEvent> prepareCloseTransitions(
            @Unmodifiable List<HeadlessWindow> closeOrder,
            long timestamp
    ) {
        ArrayList<QueuedWindowEvent> events = new ArrayList<>(closeOrder.size());
        for (HeadlessWindow window : closeOrder) {
            if (window.isClosed()) {
                continue;
            }
            WindowSnapshot previous = window.snapshot();
            WindowSnapshot closedSnapshot = new WindowSnapshot(
                    previous.id(),
                    previous.role(),
                    previous.ownerId(),
                    previous.configuration(),
                    previous.effectiveFrame(),
                    false,
                    previous.surfaceSize(),
                    previous.scaleFactor(),
                    previous.displayId(),
                    previous.surface(),
                    incrementGeneration(
                            previous.configurationGeneration(),
                            "Window configuration generation"
                    ),
                    WindowLifecycle.CLOSED
            );
            WindowEvent event = new WindowEvent(
                    allocateEventSequence(),
                    timestamp,
                    WindowEventType.CLOSED,
                    closedSnapshot,
                    previous
            );
            events.add(new QueuedWindowEvent(window, event));
        }
        return events;
    }

    /// Publishes prepared window-configuration transitions.
    ///
    /// @param transitions the transitions to publish
    private static void publishWindowTransitions(@Unmodifiable List<WindowTransition> transitions) {
        for (WindowTransition transition : transitions) {
            transition.window().publish(transition.current());
        }
    }

    /// Publishes prepared close transitions.
    ///
    /// @param events the close events carrying their replacement snapshots
    private void publishCloseTransitions(@Unmodifiable List<QueuedWindowEvent> events) {
        for (QueuedWindowEvent queued : events) {
            HeadlessWindow window = queued.window();
            window.setRedrawPending(false);
            window.publish(queued.event().snapshot());
            windows.remove(window.id(), window);
            window.releasePlatform();
        }
    }

    /// Completes window-close stages after all corresponding close callbacks are queued.
    ///
    /// Completion-stage dependents may submit new work, but that work is ordered after the already
    /// queued close callbacks and cannot mutate platform state reentrantly.
    ///
    /// @param events the prepared close events
    private static void completeWindowCloses(@Unmodifiable List<QueuedWindowEvent> events) {
        for (QueuedWindowEvent queued : events) {
            queued.window().completeClose();
        }
    }

    /// Returns child-first closure order for one owner tree.
    ///
    /// Creation order guarantees every popup follows its owner, so one forward membership pass and
    /// reversal produces deterministic descendant-before-owner order without recursion.
    ///
    /// @param root the root window
    /// @return the open tree members in child-first order
    private @Unmodifiable List<HeadlessWindow> closeOrderFor(HeadlessWindow root) {
        Set<WindowId> selectedIds = new HashSet<>();
        ArrayList<HeadlessWindow> selected = new ArrayList<>();
        selectedIds.add(root.id());
        for (HeadlessWindow candidate : windows.values()) {
            if (candidate.isClosed()) {
                continue;
            }
            if (candidate == root
                    || candidate.ownerId() != null && selectedIds.contains(candidate.ownerId())) {
                selectedIds.add(candidate.id());
                selected.add(candidate);
            }
        }
        ArrayList<HeadlessWindow> reversed = new ArrayList<>(selected.size());
        for (int index = selected.size() - 1; index >= 0; index--) {
            reversed.add(selected.get(index));
        }
        return List.copyOf(reversed);
    }

    /// Queues display callbacks after an atomic platform mutation completes.
    ///
    /// @param events the display events
    private void queueDisplayEvents(@Unmodifiable List<DisplayEvent> events) {
        List<DisplayRegistration> registrations = List.copyOf(displayRegistrations);
        for (DisplayEvent event : events) {
            for (DisplayRegistration registration : registrations) {
                eventLoop.post(() -> registration.deliver(event));
            }
        }
    }

    /// Queues window callbacks after an atomic platform mutation completes.
    ///
    /// @param events the window events
    private void queueWindowEvents(@Unmodifiable List<QueuedWindowEvent> events) {
        for (QueuedWindowEvent event : events) {
            queueWindowEvent(event);
        }
    }

    /// Queues one window callback.
    ///
    /// @param queued the target and event
    private void queueWindowEvent(QueuedWindowEvent queued) {
        eventLoop.post(() -> queued.window().deliver(queued.event()));
    }

    /// Returns a platform snapshot for a virtual display configuration.
    ///
    /// @param configuration the virtual display configuration
    /// @param enumerationIndex the display enumeration position
    /// @param configurationGeneration the general configuration generation
    /// @param colorGeneration the color capability generation
    /// @return the immutable platform snapshot
    private static DisplaySnapshot displaySnapshot(
            HeadlessDisplayConfiguration configuration,
            int enumerationIndex,
            long configurationGeneration,
            long colorGeneration
    ) {
        return new DisplaySnapshot(
                configuration.id(),
                enumerationIndex,
                configuration.bounds(),
                configuration.workArea(),
                configuration.physicalSize(),
                configuration.scaleFactor(),
                configuration.primary(),
                configurationGeneration,
                new DisplayColorCapabilities(configuration.colorDescription(), colorGeneration)
        );
    }

    /// Resolves a top-level or owner-relative popup request to global logical coordinates.
    ///
    /// @param role the surface role
    /// @param configuration the requested configuration
    /// @param owner the popup owner, or `null`
    /// @return the requested global frame before top-level state is applied
    private static LogicalRect requestedGlobalFrame(
            SurfaceRole role,
            WindowConfiguration configuration,
            @Nullable HeadlessWindow owner
    ) {
        return requestedGlobalFrame(role, configuration, owner == null ? null : owner.snapshot());
    }

    /// Resolves a top-level or owner-relative popup request from an owner snapshot.
    ///
    /// @param role the surface role
    /// @param configuration the requested configuration
    /// @param ownerSnapshot the popup owner snapshot, or `null`
    /// @return the requested global frame before top-level state is applied
    private static LogicalRect requestedGlobalFrame(
            SurfaceRole role,
            WindowConfiguration configuration,
            @Nullable WindowSnapshot ownerSnapshot
    ) {
        LogicalRect requested = configuration.frame();
        if (role == SurfaceRole.TOPLEVEL) {
            return requested;
        }
        if (ownerSnapshot == null) {
            throw new IllegalArgumentException("A popup requires an open owner snapshot");
        }
        return new LogicalRect(
                ownerSnapshot.effectiveFrame().x() + requested.x(),
                ownerSnapshot.effectiveFrame().y() + requested.y(),
                requested.width(),
                requested.height()
        );
    }

    /// Applies a deterministic top-level presentation state to a requested global frame.
    ///
    /// Maximized windows use the selected display work area, full-screen windows use its complete
    /// bounds, and normal or minimized windows retain the requested frame. Popups always retain their
    /// owner-relative resolved frame.
    ///
    /// @param role the surface role
    /// @param configuration the requested window configuration
    /// @param requestedGlobalFrame the global frame before state is applied
    /// @param display the selected display
    /// @return the effective global frame
    private static LogicalRect applyWindowState(
            SurfaceRole role,
            WindowConfiguration configuration,
            LogicalRect requestedGlobalFrame,
            DisplaySnapshot display
    ) {
        if (role == SurfaceRole.POPUP) {
            return requestedGlobalFrame;
        }
        return switch (configuration.state()) {
            case NORMAL, MINIMIZED -> requestedGlobalFrame;
            case MAXIMIZED -> display.workArea();
            case FULLSCREEN -> display.bounds();
        };
    }

    /// Computes effective top-level or popup visibility from an owner window.
    ///
    /// @param configuration the requested configuration
    /// @param owner the popup owner, or `null`
    /// @return effective presentation visibility
    private static boolean effectiveVisibility(
            WindowConfiguration configuration,
            @Nullable HeadlessWindow owner
    ) {
        return effectiveVisibility(configuration, owner == null ? null : owner.snapshot());
    }

    /// Computes effective top-level or popup visibility from an owner snapshot.
    ///
    /// @param configuration the requested configuration
    /// @param ownerSnapshot the popup owner snapshot, or `null`
    /// @return effective presentation visibility
    private static boolean effectiveVisibility(
            WindowConfiguration configuration,
            @Nullable WindowSnapshot ownerSnapshot
    ) {
        if (!configuration.visible() || configuration.state() == WindowState.MINIMIZED) {
            return false;
        }
        return ownerSnapshot == null || ownerSnapshot.effectivelyVisible();
    }

    /// Selects the display containing the greatest window area, with deterministic nearest fallback.
    ///
    /// Overlap ties prefer the primary display and then the lexicographically smaller identifier. A
    /// window with no positive overlap selects the nearest display rectangle using its center and the
    /// same tie rule.
    ///
    /// @param frame the effective global window frame
    /// @param displays the nonempty display snapshots
    /// @return the selected display
    private static DisplaySnapshot selectDisplay(
            LogicalRect frame,
            @Unmodifiable List<DisplaySnapshot> displays
    ) {
        DisplaySnapshot best = displays.getFirst();
        double bestArea = frame.intersectionArea(best.bounds());
        for (int index = 1; index < displays.size(); index++) {
            DisplaySnapshot candidate = displays.get(index);
            double area = frame.intersectionArea(candidate.bounds());
            if (area > bestArea || area == bestArea && preferredDisplay(candidate, best)) {
                best = candidate;
                bestArea = area;
            }
        }
        if (bestArea > 0.0) {
            return best;
        }

        best = displays.getFirst();
        double bestDistance = distanceToDisplay(frame, best.bounds());
        for (int index = 1; index < displays.size(); index++) {
            DisplaySnapshot candidate = displays.get(index);
            double distance = distanceToDisplay(frame, candidate.bounds());
            if (distance < bestDistance || distance == bestDistance && preferredDisplay(candidate, best)) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    /// Returns whether a display wins a deterministic selection tie.
    ///
    /// @param candidate the candidate display
    /// @param current the current display
    /// @return whether the candidate is preferred
    private static boolean preferredDisplay(DisplaySnapshot candidate, DisplaySnapshot current) {
        if (candidate.primary() != current.primary()) {
            return candidate.primary();
        }
        return candidate.id().compareTo(current.id()) < 0;
    }

    /// Returns the Euclidean distance from a window center to a display rectangle.
    ///
    /// @param frame the window frame
    /// @param displayBounds the display bounds
    /// @return the nonnegative distance in logical pixels
    private static double distanceToDisplay(LogicalRect frame, LogicalRect displayBounds) {
        double centerX = frame.x() + frame.width() / 2.0;
        double centerY = frame.y() + frame.height() / 2.0;
        double deltaX = centerX < displayBounds.x()
                ? displayBounds.x() - centerX
                : centerX > displayBounds.maxX() ? centerX - displayBounds.maxX() : 0.0;
        double deltaY = centerY < displayBounds.y()
                ? displayBounds.y() - centerY
                : centerY > displayBounds.maxY() ? centerY - displayBounds.maxY() : 0.0;
        return Math.hypot(deltaX, deltaY);
    }

    /// Converts logical window extents to a covering physical surface size.
    ///
    /// @param frame the effective logical frame
    /// @param scaleFactor the positive physical-pixel scale
    /// @return the physical surface size using ceiling conversion
    /// @throws IllegalArgumentException if either physical extent exceeds `int` range
    private static PhysicalSize physicalSize(LogicalRect frame, double scaleFactor) {
        return new PhysicalSize(
                physicalExtent(frame.width(), scaleFactor),
                physicalExtent(frame.height(), scaleFactor)
        );
    }

    /// Converts one logical extent to a covering physical-pixel extent.
    ///
    /// @param logicalExtent the nonnegative finite logical extent
    /// @param scaleFactor the finite positive scale
    /// @return the nonnegative physical extent
    /// @throws IllegalArgumentException if the result exceeds `int` range
    private static int physicalExtent(double logicalExtent, double scaleFactor) {
        double scaled = Math.ceil(logicalExtent * scaleFactor);
        if (!Double.isFinite(scaled) || scaled > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Window surface extent exceeds supported physical-pixel range");
        }
        return (int) scaled;
    }

    /// Returns whether two snapshots have the same state apart from configuration generation.
    ///
    /// @param left the first snapshot
    /// @param right the second snapshot
    /// @return whether their semantic state is equal
    private static boolean sameWindowState(WindowSnapshot left, WindowSnapshot right) {
        return left.id().equals(right.id())
                && left.role() == right.role()
                && Objects.equals(left.ownerId(), right.ownerId())
                && left.configuration().equals(right.configuration())
                && left.effectiveFrame().equals(right.effectiveFrame())
                && left.effectivelyVisible() == right.effectivelyVisible()
                && left.surfaceSize().equals(right.surfaceSize())
                && Double.compare(left.scaleFactor(), right.scaleFactor()) == 0
                && left.displayId().equals(right.displayId())
                && left.surface().equals(right.surface())
                && left.lifecycle() == right.lifecycle();
    }

    /// Verifies that a window belongs to this session.
    ///
    /// @param window the candidate window
    /// @throws IllegalArgumentException if the identifier resolves to another or no window
    private void requireOwnedWindow(HeadlessWindow window) {
        if (windows.get(window.id()) != window) {
            throw new IllegalArgumentException("Window does not belong to this Headless session");
        }
    }

    /// Posts one operation while serializing it against session shutdown.
    ///
    /// @param operation the owner-thread mutation
    /// @throws IllegalStateException if shutdown began or the event loop is closed
    private void postAccepted(Runnable operation) {
        synchronized (lifecycleLock) {
            checkAcceptingUnderLock();
            eventLoop.post(operation);
        }
    }

    /// Verifies that session submission remains open while holding the lifecycle monitor.
    ///
    /// @throws IllegalStateException if shutdown began
    private void checkAcceptingUnderLock() {
        if (!accepting) {
            throw new IllegalStateException("Headless platform session is closing or closed");
        }
    }

    /// Allocates the next positive session-wide event sequence.
    ///
    /// @return the allocated sequence
    /// @throws IllegalStateException if event sequences are exhausted
    private long allocateEventSequence() {
        if (nextEventSequence == Long.MAX_VALUE) {
            throw new IllegalStateException("Headless platform event sequences are exhausted");
        }
        long sequence = nextEventSequence;
        nextEventSequence++;
        return sequence;
    }

    /// Advances a nonnegative semantic generation by one.
    ///
    /// @param current the current generation
    /// @param label the diagnostic label
    /// @return the next generation
    /// @throws IllegalStateException if `current` is exhausted
    private static long incrementGeneration(long current, String label) {
        if (current == Long.MAX_VALUE) {
            throw new IllegalStateException(label + " is exhausted");
        }
        return current + 1L;
    }

    /// Removes one cancelled display listener registration.
    ///
    /// @param registration the registration to remove
    private void removeDisplayRegistration(DisplayRegistration registration) {
        displayRegistrations.remove(registration);
    }

    /// Returns the number of open windows retained by the session for package-level diagnostics.
    ///
    /// @return the retained open-window count
    int retainedWindowCount() {
        eventLoop.checkOwnerThread();
        return windows.size();
    }

    /// Carries one prepared window snapshot transition.
    ///
    /// @param window the target window
    /// @param previous the committed prior snapshot
    /// @param current the prepared replacement snapshot
    @NotNullByDefault
    private record WindowTransition(
            HeadlessWindow window,
            WindowSnapshot previous,
            WindowSnapshot current
    ) {
        /// Creates a prepared window transition.
        private WindowTransition {
            Objects.requireNonNull(window, "window");
            Objects.requireNonNull(previous, "previous");
            Objects.requireNonNull(current, "current");
        }
    }

    /// Carries all resolved window snapshots and the changed subset for one atomic recalculation.
    ///
    /// @param resolved resolved snapshots by window identity
    /// @param transitions changed snapshots in creation order
    @NotNullByDefault
    private record WindowRecalculation(
            IdentityHashMap<HeadlessWindow, WindowSnapshot> resolved,
            @Unmodifiable List<WindowTransition> transitions
    ) {
        /// Creates a window recalculation result.
        private WindowRecalculation {
            Objects.requireNonNull(resolved, "resolved");
            transitions = List.copyOf(transitions);
        }
    }

    /// Associates one target window with an immutable event prepared for later delivery.
    ///
    /// @param window the callback target
    /// @param event the event to deliver
    @NotNullByDefault
    private record QueuedWindowEvent(HeadlessWindow window, WindowEvent event) {
        /// Creates a queued window event.
        private QueuedWindowEvent {
            Objects.requireNonNull(window, "window");
            Objects.requireNonNull(event, "event");
        }
    }

    /// Implements one thread-safe display-listener registration.
    @NotNullByDefault
    private static final class DisplayRegistration implements ListenerRegistration {
        /// The session retaining this registration while active, released on cancellation.
        private volatile @Nullable HeadlessPlatform platform;

        /// The application display callback, released on cancellation.
        private volatile @Nullable DisplayEventHandler handler;

        /// Whether cancellation won the registration lifecycle transition.
        private final AtomicBoolean cancelled = new AtomicBoolean();

        /// Creates an active display registration.
        ///
        /// @param platform the owning session
        /// @param handler the callback
        private DisplayRegistration(HeadlessPlatform platform, DisplayEventHandler handler) {
            this.platform = platform;
            this.handler = handler;
        }

        /// Cancels future callback delivery and releases the session's registration reference.
        ///
        /// @return whether this call changed the registration state
        @Override
        public boolean cancel() {
            if (!cancelled.compareAndSet(false, true)) {
                return false;
            }
            @Nullable HeadlessPlatform currentPlatform = platform;
            platform = null;
            handler = null;
            if (currentPlatform != null) {
                currentPlatform.removeDisplayRegistration(this);
            }
            return true;
        }

        /// Returns whether cancellation completed.
        ///
        /// @return whether the registration is cancelled
        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        /// Delivers an event if cancellation has not begun its callback exclusion point.
        ///
        /// @param event the event to deliver
        private void deliver(DisplayEvent event) {
            if (cancelled.get()) {
                return;
            }
            @Nullable DisplayEventHandler currentHandler = handler;
            if (currentHandler != null) {
                currentHandler.handleDisplayEvent(event);
            }
        }
    }
}
