package org.glavo.himari.ffi;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/// Captures successful direct system-library lookups during an isolated conformance run.
///
/// Generated-binding boundary code calls [#recordSuccessfulLoad(String)] immediately after a successful
/// `SymbolLookup.libraryLookup` operation. Recording is inactive unless one session is open, so normal application
/// execution retains no process-global load history. At most one process-wide session may be active.
@NotNullByDefault
public final class NativeLibraryLoadAudit {
    /// The active process-wide conformance session, or `null` while recording is disabled.
    private static final AtomicReference<@Nullable Session> ACTIVE_SESSION = new AtomicReference<>();

    /// Prevents instantiation of this utility class.
    private NativeLibraryLoadAudit() {
    }

    /// Starts an empty process-wide recording session.
    ///
    /// @return the session that owns recording until closed
    /// @throws IllegalStateException if another session is active
    public static Session begin() {
        Session session = new Session();
        if (!ACTIVE_SESSION.compareAndSet(null, session)) {
            throw new IllegalStateException("A native-library load audit session is already active");
        }
        return session;
    }

    /// Records a system library after its direct lookup succeeds.
    ///
    /// Calls made without an active session have no effect. Paths are reduced to their final component on the current
    /// host, and repeated names are retained once.
    ///
    /// @param libraryName the non-empty name or path supplied to the successful lookup
    /// @throws IllegalArgumentException if `libraryName` is blank or has no final path component
    public static void recordSuccessfulLoad(String libraryName) {
        Objects.requireNonNull(libraryName, "libraryName");
        if (libraryName.isBlank()) {
            throw new IllegalArgumentException("libraryName must not be blank");
        }
        @Nullable Path fileName = Path.of(libraryName).getFileName();
        if (fileName == null || fileName.toString().isBlank()) {
            throw new IllegalArgumentException("libraryName must have a final path component: " + libraryName);
        }
        @Nullable Session session = ACTIVE_SESSION.get();
        if (session != null) {
            session.record(fileName.toString());
        }
    }

    /// Owns one process-wide interval of direct-load recording.
    ///
    /// Closing is idempotent. Snapshots remain available after closure and never change once closure begins.
    @NotNullByDefault
    public static final class Session implements AutoCloseable {
        /// Successfully looked-up library basenames in deterministic order.
        private final ConcurrentSkipListSet<String> libraries = new ConcurrentSkipListSet<>();

        /// Whether this session no longer accepts records.
        private final AtomicBoolean closed = new AtomicBoolean();

        /// Creates an empty session before it is installed as the active recorder.
        private Session() {
        }

        /// Returns an immutable snapshot of every observed successful lookup.
        ///
        /// A snapshot taken before closure may omit a concurrent record that completes later.
        ///
        /// @return sorted, duplicate-free library basenames
        public @Unmodifiable List<String> loadedLibraries() {
            synchronized (libraries) {
                return List.copyOf(libraries);
            }
        }

        /// Stops this session from accepting records and releases the process-wide recorder slot.
        @Override
        public void close() {
            synchronized (libraries) {
                if (closed.compareAndSet(false, true)) {
                    if (!ACTIVE_SESSION.compareAndSet(this, null)) {
                        throw new IllegalStateException(
                                "The active native-library audit session changed unexpectedly"
                        );
                    }
                }
            }
        }

        /// Records one normalized basename unless closure has begun.
        ///
        /// @param libraryName normalized non-empty basename
        private void record(String libraryName) {
            synchronized (libraries) {
                if (!closed.get()) {
                    libraries.add(libraryName);
                }
            }
        }
    }
}
