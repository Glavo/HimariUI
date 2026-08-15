package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Controls the lifetime of one platform event-listener registration.
@NotNullByDefault
public interface ListenerRegistration extends AutoCloseable {
    /// Cancels future listener delivery.
    ///
    /// This method may be called from any thread and does not interrupt a listener already running.
    ///
    /// @return `true` if this call cancelled an active registration, otherwise `false`
    boolean cancel();

    /// Returns whether the registration no longer accepts event delivery.
    ///
    /// @return whether the registration is cancelled
    boolean isCancelled();

    /// Cancels future listener delivery.
    @Override
    default void close() {
        cancel();
    }
}
