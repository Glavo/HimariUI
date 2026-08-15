package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.SurfaceRole;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowEventType;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowState;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/// Opens an owner-relative Win32 popup HWND and reports host close as a dismiss.
@NotNullByDefault
public final class WindowsPopupHost {
    /// Prevents instantiation.
    private WindowsPopupHost() {
    }

    /// Creates one owned popup window.
    ///
    /// `WM_CLOSE` is delivered as `CLOSE_REQUESTED` and invokes `onDismiss` without destroying the
    /// HWND. The caller remains responsible for closing the window.
    ///
    /// @param platform the session
    /// @param owner the owner window
    /// @param title the popup title
    /// @param frame the requested frame
    /// @param onDismiss the dismiss callback
    /// @return the popup window
    /// @throws Exception if window creation fails
    public static WindowsWindow show(
            WindowsPlatform platform,
            WindowsWindow owner,
            String title,
            LogicalRect frame,
            Runnable onDismiss
    ) throws Exception {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(onDismiss, "onDismiss");
        AtomicBoolean dismissed = new AtomicBoolean();
        WindowsWindow popup = platform.createWindow(
                WindowRequest.popup(owner.id(), new WindowConfiguration(
                        title,
                        frame,
                        true,
                        WindowState.NORMAL
                )),
                event -> {
                    if (event.type() == WindowEventType.CLOSE_REQUESTED && dismissed.compareAndSet(false, true)) {
                        onDismiss.run();
                    }
                }
        ).toCompletableFuture().get();
        if (popup.snapshot().role() != SurfaceRole.POPUP) {
            throw new IllegalStateException("Windows popup host did not create a popup surface");
        }
        return popup;
    }
}
