package org.glavo.himari.runtime;

import org.glavo.himari.platform.api.WindowId;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Records one failure contained at an application scheduler boundary.
///
/// @param sequence the positive application-wide diagnostic sequence
/// @param kind the failed operation category
/// @param windowId the affected window for frame failures, or `null` for state failures
/// @param contextSequence the zero-based state-batch index, positive redraw event sequence, or
/// `-1` for an outer batch or deferred redraw-request failure
/// @param cause the original exception or error
@NotNullByDefault
public record UiSchedulerFailure(
        long sequence,
        UiSchedulerFailureKind kind,
        @Nullable WindowId windowId,
        long contextSequence,
        Throwable cause
) {
    /// Validates the diagnostic identity and kind-specific context.
    public UiSchedulerFailure {
        if (sequence <= 0L) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(cause, "cause");
        switch (kind) {
            case STATE_UPDATE -> {
                if (windowId != null || contextSequence < 0L) {
                    throw new IllegalArgumentException("State failures require a batch index and no window");
                }
            }
            case STATE_BATCH -> {
                if (windowId != null || contextSequence != -1L) {
                    throw new IllegalArgumentException("State-batch failures require no window and -1 context");
                }
            }
            case FRAME_CALLBACK -> {
                if (windowId == null || contextSequence <= 0L) {
                    throw new IllegalArgumentException("Frame failures require a window and event sequence");
                }
            }
            case REDRAW_REQUEST -> {
                if (windowId == null || contextSequence != -1L) {
                    throw new IllegalArgumentException("Redraw-request failures require a window and -1 context");
                }
            }
        }
    }
}
