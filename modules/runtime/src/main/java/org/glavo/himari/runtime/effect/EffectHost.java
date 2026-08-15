package org.glavo.himari.runtime.effect;

import org.glavo.himari.state.StateDomain;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/// Schedules keyed effect mount, update, and cleanup after a successful UI commit.
///
/// Each state-domain epoch is applied at most once. New keys mount, surviving keys whose
/// dependency identity changed update, and missing keys clean up. Cleanup always runs
/// child-before-parent relative to declaration order by reversing the missing-key set.
/// Asynchronous work launched during mount or update is cancelled when the effect is cleaned up.
@NotNullByDefault
public final class EffectHost implements AutoCloseable {
    /// The state domain whose epochs gate apply.
    private final StateDomain domain;

    /// Shared virtual-thread workers for asynchronous effect work.
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();

    /// Committed live effects in first-declaration order.
    private final LinkedHashMap<EffectKey, LiveEffect> live = new LinkedHashMap<>();

    /// Draft declarations collected since the last apply.
    private final LinkedHashMap<EffectKey, DraftEffect> draft = new LinkedHashMap<>();

    /// The latest epoch that received an apply, or `-1` before the first apply.
    private long appliedEpoch = -1L;

    /// Whether this host has been closed.
    private boolean closed;

    /// Creates an empty host bound to one state domain.
    ///
    /// @param domain the state domain on its owner thread
    public EffectHost(StateDomain domain) {
        this.domain = Objects.requireNonNull(domain, "domain");
        domain.checkOwnerThread();
    }

    /// Declares one keyed effect for the next apply.
    ///
    /// Repeated declarations of the same key replace the previous draft. Apply publishes the last
    /// declaration for each key.
    ///
    /// @param key the effect identity
    /// @param dependencies the comparable dependency identity
    /// @param callbacks the lifecycle callbacks
    public void declare(EffectKey key, EffectDependencies dependencies, EffectCallbacks callbacks) {
        checkMutationEntry();
        draft.put(
                Objects.requireNonNull(key, "key"),
                new DraftEffect(
                        key,
                        Objects.requireNonNull(dependencies, "dependencies"),
                        Objects.requireNonNull(callbacks, "callbacks")
                )
        );
    }

    /// Applies staged declarations against the current state-domain epoch.
    ///
    /// A second call for the same epoch returns [EffectApplyStatus#ALREADY_APPLIED] and does not
    /// rerun callbacks.
    ///
    /// @return the apply result
    public EffectApplyResult apply() {
        checkMutationEntry();
        long epoch = domain.epoch();
        if (draft.isEmpty() && appliedEpoch == epoch) {
            return new EffectApplyResult(EffectApplyStatus.ALREADY_APPLIED, epoch, 0, 0, 0, null);
        }

        ArrayList<LiveEffect> toClean = new ArrayList<>();
        ArrayList<LiveEffect> toUpdate = new ArrayList<>();
        ArrayList<LiveEffect> toMount = new ArrayList<>();
        LinkedHashMap<EffectKey, LiveEffect> next = new LinkedHashMap<>();
        for (Map.Entry<EffectKey, LiveEffect> entry : live.entrySet()) {
            @Nullable DraftEffect declared = draft.get(entry.getKey());
            if (declared == null) {
                toClean.add(entry.getValue());
            } else {
                LiveEffect current = entry.getValue();
                current.callbacks = declared.callbacks;
                if (!current.dependencies.equals(declared.dependencies)) {
                    current.dependencies = declared.dependencies;
                    toUpdate.add(current);
                }
                next.put(entry.getKey(), current);
            }
        }
        for (DraftEffect declared : draft.values()) {
            if (!next.containsKey(declared.key)) {
                LiveEffect created = new LiveEffect(declared.key, declared.dependencies, declared.callbacks);
                toMount.add(created);
                next.put(declared.key, created);
            }
        }

        int mounted = 0;
        int updated = 0;
        int cleaned = 0;
        @Nullable String failure = null;
        for (int index = toClean.size() - 1; index >= 0; index--) {
            try {
                toClean.get(index).cleanup();
                cleaned++;
            } catch (RuntimeException | Error error) {
                if (failure == null) {
                    failure = error.getClass().getSimpleName();
                }
            }
        }
        for (LiveEffect effect : toMount) {
            try {
                effect.mount(workers);
                mounted++;
            } catch (RuntimeException | Error error) {
                try {
                    effect.cleanup();
                } catch (RuntimeException | Error ignored) {
                    // Cleanup after a failed mount still proceeds for remaining effects.
                }
                next.remove(effect.key);
                if (failure == null) {
                    failure = error.getClass().getSimpleName();
                }
            }
        }
        for (LiveEffect effect : toUpdate) {
            try {
                effect.update(workers);
                updated++;
            } catch (RuntimeException | Error error) {
                if (failure == null) {
                    failure = error.getClass().getSimpleName();
                }
            }
        }

        live.clear();
        live.putAll(next);
        draft.clear();
        appliedEpoch = epoch;
        if (failure != null) {
            return new EffectApplyResult(EffectApplyStatus.FAILED, epoch, mounted, updated, cleaned, failure);
        }
        if (mounted == 0 && updated == 0 && cleaned == 0) {
            return new EffectApplyResult(EffectApplyStatus.NO_CHANGES, epoch, 0, 0, 0, null);
        }
        return new EffectApplyResult(EffectApplyStatus.APPLIED, epoch, mounted, updated, cleaned, null);
    }

    /// Returns the number of currently mounted keyed effects.
    ///
    /// @return the live count
    public int liveCount() {
        checkOpen();
        domain.checkOwnerThread();
        return live.size();
    }

    /// Disposes every live effect and shuts down asynchronous workers.
    ///
    /// Closure is idempotent.
    @Override
    public void close() {
        domain.checkOwnerThread();
        if (closed) {
            return;
        }
        ArrayList<LiveEffect> remaining = new ArrayList<>(live.values());
        for (int index = remaining.size() - 1; index >= 0; index--) {
            try {
                remaining.get(index).cleanup();
            } catch (RuntimeException | Error ignored) {
                // Host close aggregates cleanup and continues.
            }
        }
        live.clear();
        draft.clear();
        workers.close();
        closed = true;
    }

    /// Verifies mutation entry conditions.
    private void checkMutationEntry() {
        checkOpen();
        domain.checkOwnerThread();
        if (domain.hasActiveTransaction()) {
            throw new IllegalStateException("Effect host cannot mutate inside a state transaction");
        }
    }

    /// Verifies that the host remains open.
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Effect host is closed");
        }
    }

    /// Stores one staged declaration.
    ///
    /// @param key the effect identity
    /// @param dependencies the dependency identity
    /// @param callbacks the lifecycle callbacks
    @NotNullByDefault
    private record DraftEffect(EffectKey key, EffectDependencies dependencies, EffectCallbacks callbacks) {
    }

    /// Stores one committed keyed effect.
    @NotNullByDefault
    private static final class LiveEffect {
        /// The effect identity.
        private final EffectKey key;

        /// The last published dependency identity.
        private EffectDependencies dependencies;

        /// The current callbacks.
        private EffectCallbacks callbacks;

        /// Asynchronous handles launched by the latest mount or update.
        private final ArrayList<AsyncEffectHandle> handles = new ArrayList<>();

        /// Whether cleanup has run.
        private boolean cleaned;

        /// Creates one unmounted live record.
        ///
        /// @param key the identity
        /// @param dependencies the dependencies
        /// @param callbacks the callbacks
        private LiveEffect(EffectKey key, EffectDependencies dependencies, EffectCallbacks callbacks) {
            this.key = key;
            this.dependencies = dependencies;
            this.callbacks = callbacks;
        }

        /// Mounts the effect.
        ///
        /// @param workers the worker pool
        private void mount(ExecutorService workers) {
            EffectSession session = new EffectSession(workers);
            try {
                callbacks.onMount(session);
                handles.addAll(session.launched());
            } finally {
                session.close();
            }
        }

        /// Updates the effect after a dependency change.
        ///
        /// @param workers the worker pool
        private void update(ExecutorService workers) {
            EffectSession session = new EffectSession(workers);
            try {
                callbacks.onUpdate(session);
                handles.addAll(session.launched());
            } finally {
                session.close();
            }
        }

        /// Cancels asynchronous work and runs cleanup.
        private void cleanup() {
            if (cleaned) {
                return;
            }
            cleaned = true;
            for (AsyncEffectHandle handle : handles) {
                handle.cancel();
            }
            handles.clear();
            callbacks.onCleanup();
        }
    }
}
