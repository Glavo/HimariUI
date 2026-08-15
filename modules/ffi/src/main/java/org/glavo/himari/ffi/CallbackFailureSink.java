package org.glavo.himari.ffi;

import org.jetbrains.annotations.NotNullByDefault;

/// Receives a Java failure caught at a generated native callback boundary.
///
/// Callback adapters invoke this sink only after preventing the failure from unwinding into native code.
@FunctionalInterface
@NotNullByDefault
public interface CallbackFailureSink {
    /// Publishes one contained callback failure.
    ///
    /// @param failure the failure caught by the generated callback adapter
    /// @implSpec Implementations must not throw. Generated adapters will suppress a sink failure to preserve the
    /// native-boundary containment guarantee.
    void publish(Throwable failure);
}
