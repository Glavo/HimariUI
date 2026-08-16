package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.IntConsumer;

/// Advances VM-independent reload generations for Headless simulation.
///
/// This coordinator does not replace bytecode. Compatible applies increment the generation, keep
/// keyed values, install the new callback, and restart owned effects. Incompatible applies choose
/// an explicit fallback: subtree and full-UI reset drop retained values; process restart reports
/// the fallback and also drops values. A rejected compile or verification leaves the previous
/// generation unchanged.
@NotNullByDefault
public final class ReloadCoordinator {
    /// Visible generation. Zero means no apply has been published.
    private int generation;

    /// Keyed values retained across compatible generations.
    private final LinkedHashMap<Object, Object> retained = new LinkedHashMap<>();

    /// Installed callback, or `null` before the first compatible apply.
    private @Nullable IntConsumer callback;

    /// Creates an idle coordinator.
    public ReloadCoordinator() {
    }

    /// Returns the published generation.
    ///
    /// @return `0` before the first successful apply
    public int generation() {
        return generation;
    }

    /// Stores a keyed value that a compatible apply will keep.
    ///
    /// @param key the semantic key
    /// @param value the non-null value
    public void retain(Object key, Object value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        retained.put(key, value);
    }

    /// Returns the value stored under `key`.
    ///
    /// @param key the semantic key
    /// @return the value, or `null` when absent
    public @Nullable Object retained(Object key) {
        Objects.requireNonNull(key, "key");
        return retained.get(key);
    }

    /// Returns the installed callback.
    ///
    /// @return the callback, or `null`
    public @Nullable IntConsumer callback() {
        return callback;
    }

    /// Publishes a compatible generation: retain state, replace the callback, restart effects.
    ///
    /// @param replacement the callback for the new generation
    /// @param restartEffects runs after the generation is published
    /// @return the published outcome
    public ReloadOutcome applyCompatible(IntConsumer replacement, Runnable restartEffects) {
        Objects.requireNonNull(replacement, "replacement");
        Objects.requireNonNull(restartEffects, "restartEffects");
        generation++;
        callback = replacement;
        restartEffects.run();
        replacement.accept(generation);
        return new ReloadOutcome(generation, true, true, true, ReloadFallback.NONE, false);
    }

    /// Rejects an unverified generation and leaves the running UI unchanged.
    ///
    /// @return the rejected outcome
    public ReloadOutcome rejectUnverified() {
        return new ReloadOutcome(generation, !retained.isEmpty(), false, false, ReloadFallback.NONE, true);
    }

    /// Publishes an incompatible generation that uses `fallback`.
    ///
    /// @param fallback the explicit fallback; must not be [`ReloadFallback#NONE`]
    /// @return the published outcome
    public ReloadOutcome applyIncompatible(ReloadFallback fallback) {
        Objects.requireNonNull(fallback, "fallback");
        if (fallback == ReloadFallback.NONE) {
            throw new IllegalArgumentException("Incompatible reload requires an explicit fallback");
        }
        generation++;
        callback = null;
        if (fallback != ReloadFallback.PROCESS_RESTART) {
            retained.clear();
        } else {
            retained.clear();
        }
        return new ReloadOutcome(generation, false, false, false, fallback, false);
    }
}
