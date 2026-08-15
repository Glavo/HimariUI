package org.glavo.himari.spikes.win32;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/// Captures normalized Win32 lifecycle and callback evidence from one window lifetime.
///
/// @param repetitions the number of completed stimulus cycles
/// @param requestedSoakSeconds the configured minimum soak duration
/// @param elapsedMillis the measured duration from the first cycle wait through the last cycle
/// @param eventSequence the deterministic normalized event sequence
/// @param resizeEvents the observed `WM_SIZE` count during stimulus cycles
/// @param pointerEvents the observed `WM_MOUSEMOVE` count during stimulus cycles
/// @param keyEvents the observed `WM_KEYDOWN` count during stimulus cycles
/// @param characterEvents the observed `WM_CHAR` count during stimulus cycles
/// @param paintEvents the observed `WM_PAINT` count across the window lifetime
/// @param callbackFailures the number of deliberately injected callback failures contained by the generated adapter
/// @param maximumCallbackDepth the maximum synchronous `WndProc` nesting depth
/// @param finalClientWidth the final client width reported by `WM_SIZE`
/// @param finalClientHeight the final client height reported by `WM_SIZE`
/// @param closeObserved whether `WM_CLOSE` was observed
/// @param destroyObserved whether `WM_DESTROY` was observed
/// @param quitObserved whether the message pump removed `WM_QUIT`
/// @param output the target-matched dynamic DXGI output snapshot
@NotNullByDefault
record Win32WindowResult(
        int repetitions,
        int requestedSoakSeconds,
        long elapsedMillis,
        @Unmodifiable List<String> eventSequence,
        int resizeEvents,
        int pointerEvents,
        int keyEvents,
        int characterEvents,
        int paintEvents,
        int callbackFailures,
        int maximumCallbackDepth,
        int finalClientWidth,
        int finalClientHeight,
        boolean closeObserved,
        boolean destroyObserved,
        boolean quitObserved,
        DxgiOutputSnapshot output
) {
    /// Creates a result with an immutable event-sequence snapshot.
    Win32WindowResult {
        eventSequence = List.copyOf(eventSequence);
    }
}
