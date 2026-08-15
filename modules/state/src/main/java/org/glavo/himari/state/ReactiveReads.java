package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;
import java.util.function.Supplier;

/// Provides explicit synchronous reads that do not become reactive dependencies.
///
/// Tracking is restored before this method returns or propagates a failure. The capture context is
/// held in an ordinary [ThreadLocal], so work started on another thread is also untracked until that
/// thread enters its own reactive consumer execution.
@NotNullByDefault
public final class ReactiveReads {
    /// Prevents instantiation of this utility class.
    private ReactiveReads() {
    }

    /// Runs a supplier without attaching its state reads to the enclosing consumer.
    ///
    /// Nested derived states still track their own dependencies if they must recompute.
    ///
    /// @param supplier the synchronous supplier
    /// @param <T> the result type
    /// @return the supplier result
    public static <T> T untracked(Supplier<? extends T> supplier) {
        return ReactiveTracking.untracked(Objects.requireNonNull(supplier, "supplier"));
    }

    /// Runs an action without attaching its state reads to the enclosing consumer.
    ///
    /// @param action the synchronous action
    public static void untracked(Runnable action) {
        ReactiveTracking.untracked(Objects.requireNonNull(action, "action"));
    }
}
