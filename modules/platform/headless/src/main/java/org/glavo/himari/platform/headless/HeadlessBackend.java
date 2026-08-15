package org.glavo.himari.platform.headless;

import org.glavo.himari.platform.api.PlatformBackend;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/// Opens independent deterministic Headless platform sessions without loading operating-system
/// libraries or requiring a display server.
@NotNullByDefault
public final class HeadlessBackend implements PlatformBackend<HeadlessPlatform> {
    /// The immutable initial virtual display topology for each opened session.
    private final @Unmodifiable List<HeadlessDisplayConfiguration> initialDisplays;

    /// The initial manual-clock timestamp for each opened session.
    private final long initialTimestampNanos;

    /// Creates a backend with one default 1920×1080 sRGB SDR display and a zero timestamp.
    public HeadlessBackend() {
        this(List.of(HeadlessDisplayConfiguration.defaultDisplay()), 0L);
    }

    /// Creates a backend with an explicit initial display topology and clock timestamp.
    ///
    /// The display list is validated and copied during construction. It must be nonempty, contain
    /// unique identifiers, and designate exactly one primary display.
    ///
    /// @param initialDisplays the initial display configurations in deterministic enumeration order
    /// @param initialTimestampNanos the nonnegative initial manual-clock timestamp
    /// @throws IllegalArgumentException if the display topology or timestamp is invalid
    public HeadlessBackend(
            @Unmodifiable List<HeadlessDisplayConfiguration> initialDisplays,
            long initialTimestampNanos
    ) {
        Objects.requireNonNull(initialDisplays, "initialDisplays");
        this.initialDisplays = HeadlessPlatform.validatedDisplayConfigurations(initialDisplays);
        if (initialTimestampNanos < 0L) {
            throw new IllegalArgumentException("Initial Headless timestamp must be nonnegative");
        }
        this.initialTimestampNanos = initialTimestampNanos;
    }

    /// Opens a session owned by the calling thread.
    ///
    /// Headless initialization has no external capability acquisition and therefore returns an
    /// already-completed stage while preserving the asynchronous platform contract.
    ///
    /// @return an already-completed stage containing a new independent session
    @Override
    public CompletionStage<HeadlessPlatform> open() {
        ManualFrameClock clock = new ManualFrameClock(initialTimestampNanos);
        return CompletableFuture.completedFuture(new HeadlessPlatform(
                new HeadlessEventLoop(clock),
                initialDisplays
        ));
    }
}
