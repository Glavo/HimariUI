package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.concurrent.CompletionStage;

/// Opens a target-specific platform session through an asynchronous capability boundary.
///
/// This interface is a typed host contract, not a runtime provider registry. Applications select
/// and construct a concrete target backend explicitly.
///
/// @param <P> the concrete session type produced by this backend
@NotNullByDefault
public interface PlatformBackend<P extends PlatformSession<?>> {
    /// Begins initialization of an independent platform session.
    ///
    /// @return a stage completed with the initialized session or exceptionally on failure
    CompletionStage<P> open();
}
