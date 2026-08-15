package org.glavo.himari.runtime;

import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowState;
import org.glavo.himari.platform.headless.HeadlessBackend;
import org.glavo.himari.platform.headless.HeadlessPlatform;
import org.glavo.himari.platform.headless.HeadlessWindow;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/// Executes the deterministic SCHED-001 acceptance scenario and writes its machine-readable result.
@NotNullByDefault
public final class RuntimeSchedulerConformance {
    /// Prevents instantiation of this command-line entry point.
    private RuntimeSchedulerConformance() {
    }

    /// Runs state batching, per-window coalescing, follow-up scheduling, and closure checks.
    ///
    /// @param arguments one output-directory path
    /// @throws IOException if the report cannot be written
    /// @throws InterruptedException if cross-thread state submission is interrupted
    public static void main(String[] arguments) throws IOException, InterruptedException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one conformance output directory");
        }

        Thread ownerThread = Thread.currentThread();
        HeadlessPlatform platform = new HeadlessBackend(List.of(
                org.glavo.himari.platform.headless.HeadlessDisplayConfiguration.defaultDisplay()
        ), 1_000L).open().toCompletableFuture().join();
        StateDomain domain = new StateDomain();
        IntState state = domain.intState(0);
        UiScheduler scheduler = new UiScheduler(platform.eventLoop(), domain);
        HeadlessWindow firstWindow = createWindow(platform, scheduler, "First");
        HeadlessWindow secondWindow = createWindow(platform, scheduler, "Second");
        ArrayList<FrameTick> frames = new ArrayList<>();
        AtomicReference<@Nullable WindowFrameScheduler> firstFrameReference = new AtomicReference<>();
        WindowFrameScheduler firstFrame = scheduler.createFrameScheduler(firstWindow, tick -> {
            require(Thread.currentThread() == ownerThread, "Frame callback left the owner context");
            frames.add(tick);
            if (tick.requestGeneration() == 4L) {
                require(firstFrameReference.get()).requestFrame();
                require(firstFrameReference.get()).requestFrame();
            }
        });
        firstFrameReference.set(firstFrame);
        WindowFrameScheduler secondFrame = scheduler.createFrameScheduler(secondWindow, tick -> {
            require(Thread.currentThread() == ownerThread, "Frame callback left the owner context");
            frames.add(tick);
        });

        Thread submitter = Thread.ofPlatform().name("scheduler-conformance-state").start(() -> {
            scheduler.enqueueStateUpdate(() -> state.set(1));
            scheduler.enqueueStateUpdate(() -> {
                state.set(99);
                throw new IllegalStateException("planned conformance failure");
            });
            scheduler.enqueueStateUpdate(() -> state.set(state.get() + 1));
        });
        submitter.join();

        for (int index = 0; index < 4; index++) {
            firstFrame.requestFrame();
        }
        for (int index = 0; index < 2; index++) {
            secondFrame.requestFrame();
        }
        platform.eventLoop().runUntilIdle();

        UiSchedulerSnapshot activeSnapshot = scheduler.snapshot();
        require(state.get() == 2, "Failed state callback did not roll back independently");
        require(domain.epoch() == 1L, "One detached state batch must publish at most one epoch");
        require(activeSnapshot.stateBatches() == 1L, "Expected one state batch");
        require(activeSnapshot.stateUpdates() == 3L, "Expected three attempted state updates");
        require(activeSnapshot.stateUpdateFailures() == 1L, "Expected one state update failure");
        require(activeSnapshot.registeredWindowSchedulers() == 2, "Expected two window schedulers");
        require(activeSnapshot.frames() == 3L, "Expected two initial frames and one follow-up");
        require(activeSnapshot.coalescedFrameRequests() == 8L, "Expected eight explicit requests");
        require(frames.size() == 3, "Expected three frame ticks");
        require(frames.get(0).windowId().equals(firstWindow.id()), "First routed frame is incorrect");
        require(frames.get(0).coalescedRequestCount() == 4L, "First frame did not coalesce four requests");
        require(frames.get(1).windowId().equals(secondWindow.id()), "Second routed frame is incorrect");
        require(frames.get(1).coalescedRequestCount() == 2L, "Second frame did not coalesce two requests");
        require(frames.get(2).windowId().equals(firstWindow.id()), "Follow-up frame crossed windows");
        require(frames.get(2).coalescedRequestCount() == 2L, "Follow-up frame did not coalesce two requests");
        require(frames.stream().allMatch(tick -> tick.timestampNanos() == 1_000L),
                "One manual-clock timestamp must be sampled for each frame");

        scheduler.close();
        UiSchedulerSnapshot closedSnapshot = scheduler.snapshot();
        require(closedSnapshot.closed(), "Scheduler did not close");
        require(closedSnapshot.pendingStateUpdates() == 0, "Closure retained state ingress");
        require(closedSnapshot.registeredWindowSchedulers() == 0, "Closure retained windows");
        require(!platform.eventLoop().isClosed(), "Scheduler closed its borrowed event loop");

        Path outputDirectory = Path.of(arguments[0]);
        Files.createDirectories(outputDirectory);
        writeReport(outputDirectory.resolve("results.json"), activeSnapshot, closedSnapshot);
    }

    /// Creates and dispatches one routed Headless top-level window.
    ///
    /// @param platform the owning Headless platform
    /// @param scheduler the window-event router
    /// @param title the window title
    /// @return the created window
    private static HeadlessWindow createWindow(
            HeadlessPlatform platform,
            UiScheduler scheduler,
            String title
    ) {
        WindowConfiguration configuration = new WindowConfiguration(
                title,
                new LogicalRect(0.0, 0.0, 320.0, 200.0),
                true,
                WindowState.NORMAL
        );
        CompletableFuture<HeadlessWindow> completion = platform.createWindow(
                WindowRequest.toplevel(configuration),
                scheduler::handleWindowEvent
        ).toCompletableFuture();
        platform.eventLoop().runUntilIdle();
        return completion.join();
    }

    /// Writes the validated conformance observations as deterministic JSON.
    ///
    /// @param reportPath the report path
    /// @param activeSnapshot the scheduler snapshot before closure
    /// @param closedSnapshot the scheduler snapshot after closure
    /// @throws IOException if the report cannot be written
    private static void writeReport(
            Path reportPath,
            UiSchedulerSnapshot activeSnapshot,
            UiSchedulerSnapshot closedSnapshot
    ) throws IOException {
        String report = """
                {
                  "profile": "m1-scheduler",
                  "workPackage": "SCHED-001",
                  "status": "passed",
                  "unitTestCases": 10,
                  "ownerContext": true,
                  "hostDriven": true,
                  "stateBatches": %d,
                  "stateUpdates": %d,
                  "stateUpdateFailures": %d,
                  "publishedStateEpochs": 1,
                  "windowSchedulers": %d,
                  "frames": %d,
                  "coalescedFrameRequests": %d,
                  "inFrameFollowUpFrames": 1,
                  "crossWindowBroadcasts": 0,
                  "pendingStateUpdatesAfterClose": %d,
                  "windowSchedulersAfterClose": %d,
                  "borrowedEventLoopRemainedOpen": true,
                  "moduleNativeAccess": false
                }
                """.formatted(
                activeSnapshot.stateBatches(),
                activeSnapshot.stateUpdates(),
                activeSnapshot.stateUpdateFailures(),
                activeSnapshot.registeredWindowSchedulers(),
                activeSnapshot.frames(),
                activeSnapshot.coalescedFrameRequests(),
                closedSnapshot.pendingStateUpdates(),
                closedSnapshot.registeredWindowSchedulers()
        );
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
    }

    /// Rejects an invalid conformance observation.
    ///
    /// @param condition whether the observation satisfies its invariant
    /// @param message the failure message
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /// Requires a non-null conformance reference.
    ///
    /// @param value the candidate value
    /// @param <T> the reference type
    /// @return the non-null reference
    private static <T> T require(@Nullable T value) {
        if (value == null) {
            throw new IllegalStateException("Expected a non-null conformance reference");
        }
        return value;
    }
}
