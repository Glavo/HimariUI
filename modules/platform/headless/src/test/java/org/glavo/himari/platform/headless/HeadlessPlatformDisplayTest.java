package org.glavo.himari.platform.headless;

import org.glavo.himari.platform.api.Chromaticity;
import org.glavo.himari.platform.api.DisplayColorDescription;
import org.glavo.himari.platform.api.DisplayEvent;
import org.glavo.himari.platform.api.DisplayEventType;
import org.glavo.himari.platform.api.DisplayId;
import org.glavo.himari.platform.api.DisplayPrimaries;
import org.glavo.himari.platform.api.DisplaySnapshot;
import org.glavo.himari.platform.api.ListenerRegistration;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.PhysicalSize;
import org.glavo.himari.platform.api.PresentationMode;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowEvent;
import org.glavo.himari.platform.api.WindowEventType;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowSnapshot;
import org.glavo.himari.platform.api.WindowState;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies programmable display topology, capability generations, migration, and listener lifetime.
@NotNullByDefault
final class HeadlessPlatformDisplayTest {
    /// Verifies atomic topology replacement and independent color-capability generations.
    @Test
    void publishesProgrammableColorAndMigratesWindowsAtomically() {
        HeadlessPlatform platform = openDefaultPlatform();
        ArrayList<DisplayEvent> displayEvents = new ArrayList<>();
        ArrayList<WindowEvent> windowEvents = new ArrayList<>();
        platform.addDisplayEventHandler(displayEvents::add);
        CompletableFuture<HeadlessWindow> creation = platform.createWindow(
                WindowRequest.toplevel(new WindowConfiguration(
                        "Color window",
                        new LogicalRect(900.0, 50.0, 100.0, 50.0),
                        true,
                        WindowState.NORMAL
                )),
                windowEvents::add
        ).toCompletableFuture();
        assertFalse(creation.isDone());
        platform.eventLoop().runUntilIdle();
        HeadlessWindow window = creation.join();
        windowEvents.clear();

        HeadlessDisplayConfiguration left = display(
                "display-a",
                new LogicalRect(0.0, 0.0, 800.0, 600.0),
                1.0,
                false,
                DisplayColorDescription.SRGB_SDR
        );
        DisplayColorDescription hdr = hdrDescription(1_000.0, 203.0, 4.0);
        HeadlessDisplayConfiguration right = display(
                "display-b",
                new LogicalRect(800.0, 0.0, 800.0, 600.0),
                2.0,
                true,
                hdr
        );
        CompletableFuture<Long> replacement = platform.replaceDisplays(List.of(left, right))
                .toCompletableFuture();
        assertFalse(replacement.isDone());
        platform.eventLoop().runUntilIdle();

        assertEquals(1L, replacement.join());
        assertEquals(1L, platform.displayTopologyGeneration());
        assertEquals(List.of(DisplayEventType.REMOVED, DisplayEventType.ADDED, DisplayEventType.ADDED),
                displayEvents.stream().map(DisplayEvent::type).toList());
        assertEquals(1, windowEvents.size());
        assertEquals(WindowEventType.CONFIGURATION_CHANGED, windowEvents.getFirst().type());
        assertTrue(displayEvents.getLast().sequence() < windowEvents.getFirst().sequence());

        DisplaySnapshot rightSnapshot = platform.displays().get(1);
        assertEquals(new DisplayId("display-b"), rightSnapshot.id());
        assertEquals(1, rightSnapshot.enumerationIndex());
        assertEquals(0L, rightSnapshot.configurationGeneration());
        assertEquals(0L, rightSnapshot.colorCapabilities().generation());
        assertEquals(hdr, rightSnapshot.colorCapabilities().description());
        assertEquals(new DisplayId("display-b"), window.snapshot().displayId());
        assertEquals(2.0, window.snapshot().scaleFactor());
        assertEquals(new PhysicalSize(200, 100), window.snapshot().surfaceSize());

        displayEvents.clear();
        windowEvents.clear();
        DisplayColorDescription brighter = hdrDescription(1_500.0, 203.0, 6.0);
        HeadlessDisplayConfiguration brighterRight = display(
                "display-b",
                right.bounds(),
                2.0,
                true,
                brighter
        );
        platform.replaceDisplays(List.of(left, brighterRight));
        platform.eventLoop().runUntilIdle();

        assertEquals(2L, platform.displayTopologyGeneration());
        assertEquals(1, displayEvents.size());
        assertEquals(DisplayEventType.CHANGED, displayEvents.getFirst().type());
        DisplaySnapshot brighterSnapshot = platform.displays().get(1);
        assertEquals(1L, brighterSnapshot.configurationGeneration());
        assertEquals(1L, brighterSnapshot.colorCapabilities().generation());
        assertEquals(brighter, brighterSnapshot.colorCapabilities().description());
        assertTrue(windowEvents.isEmpty());

        displayEvents.clear();
        CompletableFuture<Long> unchanged = platform.replaceDisplays(List.of(left, brighterRight))
                .toCompletableFuture();
        platform.eventLoop().runUntilIdle();
        assertEquals(2L, unchanged.join());
        assertTrue(displayEvents.isEmpty());
    }

    /// Verifies deterministic enumeration changes and cancellation before queued delivery begins.
    @Test
    void tracksEnumerationOrderAndHonorsListenerCancellation() {
        HeadlessDisplayConfiguration first = display(
                "first",
                new LogicalRect(0.0, 0.0, 640.0, 480.0),
                1.0,
                true,
                DisplayColorDescription.SRGB_SDR
        );
        HeadlessDisplayConfiguration second = display(
                "second",
                new LogicalRect(640.0, 0.0, 640.0, 480.0),
                1.0,
                false,
                DisplayColorDescription.SRGB_SDR
        );
        HeadlessPlatform platform = new HeadlessBackend(List.of(first, second), 0L)
                .open().toCompletableFuture().join();
        ArrayList<DisplayEvent> events = new ArrayList<>();
        ListenerRegistration registration = platform.addDisplayEventHandler(events::add);

        CompletableFuture<Long> replacement = platform.replaceDisplays(List.of(second, first))
                .toCompletableFuture();
        platform.eventLoop().post(registration::cancel);
        platform.eventLoop().runUntilIdle();

        assertEquals(1L, replacement.join());
        assertTrue(registration.isCancelled());
        assertTrue(events.isEmpty());
        assertEquals(new DisplayId("second"), platform.displays().getFirst().id());
        assertEquals(0, platform.displays().getFirst().enumerationIndex());
        assertEquals(1L, platform.displays().getFirst().configurationGeneration());
        assertEquals(1L, platform.displays().getLast().configurationGeneration());
        assertEquals(0L, platform.displays().getFirst().colorCapabilities().generation());
    }

    /// Verifies complete-topology validation before any asynchronous mutation is submitted.
    @Test
    void rejectsInvalidDisplayTopologies() {
        HeadlessDisplayConfiguration primary = HeadlessDisplayConfiguration.defaultDisplay();
        HeadlessDisplayConfiguration duplicate = new HeadlessDisplayConfiguration(
                primary.id(),
                primary.bounds(),
                primary.workArea(),
                primary.physicalSize(),
                primary.scaleFactor(),
                false,
                primary.colorDescription()
        );

        assertThrows(IllegalArgumentException.class, () -> new HeadlessBackend(List.of(), 0L));
        assertThrows(IllegalArgumentException.class, () -> new HeadlessBackend(
                List.of(primary, duplicate),
                0L
        ));
        assertThrows(IllegalArgumentException.class, () -> new HeadlessBackend(
                List.of(duplicate),
                0L
        ));
        assertThrows(IllegalArgumentException.class, () -> new HeadlessBackend(
                List.of(primary, primary),
                0L
        ));
    }

    /// Verifies that a display change unable to represent an affected surface publishes nothing.
    @Test
    void rollsBackDisplayReplacementWhenWindowRecalculationFails() {
        HeadlessPlatform platform = openDefaultPlatform();
        ArrayList<DisplayEvent> displayEvents = new ArrayList<>();
        ArrayList<WindowEvent> windowEvents = new ArrayList<>();
        platform.addDisplayEventHandler(displayEvents::add);
        CompletableFuture<HeadlessWindow> creation = platform.createWindow(
                WindowRequest.toplevel(new WindowConfiguration(
                        "Huge",
                        new LogicalRect(0.0, 0.0, Integer.MAX_VALUE, 1.0),
                        true,
                        WindowState.NORMAL
                )),
                windowEvents::add
        ).toCompletableFuture();
        platform.eventLoop().runUntilIdle();
        HeadlessWindow window = creation.join();
        WindowEvent createdEvent = windowEvents.getFirst();
        WindowSnapshot before = window.snapshot();
        displayEvents.clear();
        windowEvents.clear();

        HeadlessDisplayConfiguration scaled = display(
                "scaled",
                HeadlessDisplayConfiguration.defaultDisplay().bounds(),
                2.0,
                true,
                DisplayColorDescription.SRGB_SDR
        );
        CompletableFuture<Long> replacement = platform.replaceDisplays(List.of(scaled))
                .toCompletableFuture();
        platform.eventLoop().runUntilIdle();

        CompletionException failure = assertThrows(CompletionException.class, replacement::join);
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        assertEquals(0L, platform.displayTopologyGeneration());
        assertEquals(HeadlessDisplayConfiguration.DEFAULT_DISPLAY_ID, platform.displays().getFirst().id());
        assertSame(before, window.snapshot());
        assertEquals(WindowEventType.CREATED, createdEvent.type());
        assertTrue(displayEvents.isEmpty());
        assertTrue(windowEvents.isEmpty());
    }

    /// Opens the default completed Headless backend stage.
    ///
    /// @return the new session
    private static HeadlessPlatform openDefaultPlatform() {
        return new HeadlessBackend().open().toCompletableFuture().join();
    }

    /// Creates one virtual display whose physical mode matches the declared scale.
    ///
    /// @param identifier the display identifier
    /// @param bounds the logical bounds
    /// @param scale the physical-pixel scale
    /// @param primary whether the display is primary
    /// @param color the color capability description
    /// @return the display configuration
    private static HeadlessDisplayConfiguration display(
            String identifier,
            LogicalRect bounds,
            double scale,
            boolean primary,
            DisplayColorDescription color
    ) {
        return new HeadlessDisplayConfiguration(
                new DisplayId(identifier),
                bounds,
                bounds,
                new PhysicalSize(
                        (int) Math.ceil(bounds.width() * scale),
                        (int) Math.ceil(bounds.height() * scale)
                ),
                scale,
                primary,
                color
        );
    }

    /// Creates a programmable Display-P3-like HDR capability description.
    ///
    /// @param maximumNits the maximum luminance
    /// @param referenceWhiteNits the SDR reference white
    /// @param headroom the relative headroom
    /// @return the color capability description
    private static DisplayColorDescription hdrDescription(
            double maximumNits,
            double referenceWhiteNits,
            double headroom
    ) {
        DisplayPrimaries p3 = new DisplayPrimaries(
                new Chromaticity(0.680, 0.320),
                new Chromaticity(0.265, 0.690),
                new Chromaticity(0.150, 0.060),
                new Chromaticity(0.3127, 0.3290)
        );
        return new DisplayColorDescription(
                p3,
                0.001,
                maximumNits,
                referenceWhiteNits,
                headroom,
                List.of(PresentationMode.EXTENDED_LINEAR, PresentationMode.PQ, PresentationMode.SDR)
        );
    }
}
