package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.api.PlatformBackend;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/// Opens Windows desktop sessions through generated FFM bindings.
@NotNullByDefault
public final class WindowsBackend implements PlatformBackend<WindowsPlatform> {
    /// Creates the backend.
    public WindowsBackend() {
    }

    /// Opens a session owned by the calling thread.
    ///
    /// @return the completed session
    @Override
    public CompletionStage<WindowsPlatform> open() {
        WindowsLibraries libraries = WindowsLibraries.open();
        try {
            return CompletableFuture.completedFuture(new WindowsPlatform(libraries, new WindowsEventLoop()));
        } catch (RuntimeException | Error failure) {
            libraries.close();
            throw failure;
        }
    }
}
