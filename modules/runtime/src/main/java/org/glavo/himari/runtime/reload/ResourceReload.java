package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

/// Publishes theme, style, image, and font payloads as atomic generations.
///
/// A successful publish increments the generation, replaces only the named key, notifies only
/// watchers of that key, and retains the previous payload on the released list so callers can drop
/// superseded native or GPU owners. A rejected publish leaves the last valid generation and
/// payload unchanged.
@NotNullByDefault
public final class ResourceReload {
    /// Visible generation. Zero means no payload has been published.
    private int generation;

    /// Last valid payload per key.
    private final LinkedHashMap<ResourceKey, ResourceSnapshot> current = new LinkedHashMap<>();

    /// Watchers keyed by resource identity.
    private final LinkedHashMap<ResourceKey, IntConsumer> watchers = new LinkedHashMap<>();

    /// Superseded payloads that consumers must release.
    private final ArrayList<MemorySegment> released = new ArrayList<>();

    /// Creates an idle coordinator.
    public ResourceReload() {
    }

    /// Returns the published generation.
    ///
    /// @return `0` before the first successful publish
    public int generation() {
        return generation;
    }

    /// Registers a watcher that receives the generation after a successful publish of `key`.
    ///
    /// @param kind the resource family
    /// @param key the consumer key
    /// @param watcher the generation callback
    public void watch(ResourceKind kind, String key, IntConsumer watcher) {
        Objects.requireNonNull(watcher, "watcher");
        watchers.put(new ResourceKey(kind, key), watcher);
    }

    /// Publishes one payload and notifies only watchers of that key.
    ///
    /// @param kind the resource family
    /// @param key the consumer key
    /// @param bytes the new payload
    /// @return the published outcome
    public ResourceReloadOutcome publish(ResourceKind kind, String key, MemorySegment bytes) {
        Objects.requireNonNull(bytes, "bytes");
        ResourceKey identity = new ResourceKey(kind, key);
        generation++;
        @Nullable ResourceSnapshot previous = current.put(identity, new ResourceSnapshot(kind, key, bytes, generation));
        if (previous != null) {
            released.add(previous.bytes());
        }
        @Nullable IntConsumer watcher = watchers.get(identity);
        int notified = 0;
        if (watcher != null) {
            watcher.accept(generation);
            notified = 1;
        }
        return new ResourceReloadOutcome(generation, true, previous != null, notified, false);
    }

    /// Publishes several payloads under one generation and notifies only those keys.
    ///
    /// @param kinds the resource families, same length as `keys` and `payloads`
    /// @param keys the consumer keys
    /// @param payloads the new payloads
    /// @return the published outcome
    public ResourceReloadOutcome publishAll(ResourceKind[] kinds, String[] keys, MemorySegment[] payloads) {
        Objects.requireNonNull(kinds, "kinds");
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(payloads, "payloads");
        if (kinds.length == 0 || kinds.length != keys.length || kinds.length != payloads.length) {
            throw new IllegalArgumentException("Resource batch lengths must match and be positive");
        }
        generation++;
        int notified = 0;
        boolean retained = false;
        for (int index = 0; index < kinds.length; index++) {
            ResourceKey identity = new ResourceKey(kinds[index], keys[index]);
            @Nullable ResourceSnapshot previous = current.put(
                    identity,
                    new ResourceSnapshot(kinds[index], keys[index], payloads[index], generation)
            );
            if (previous != null) {
                released.add(previous.bytes());
                retained = true;
            }
            @Nullable IntConsumer watcher = watchers.get(identity);
            if (watcher != null) {
                watcher.accept(generation);
                notified++;
            }
        }
        return new ResourceReloadOutcome(generation, true, retained, notified, false);
    }

    /// Rejects an unverified payload and leaves the last valid generation unchanged.
    ///
    /// @param kind the resource family
    /// @param key the consumer key
    /// @return the rejected outcome
    public ResourceReloadOutcome reject(ResourceKind kind, String key) {
        ResourceKey identity = new ResourceKey(kind, key);
        return new ResourceReloadOutcome(generation, false, current.containsKey(identity), 0, true);
    }

    /// Returns the last valid payload for `key`.
    ///
    /// @param kind the resource family
    /// @param key the consumer key
    /// @return the snapshot, or `null` when none has been published
    public @Nullable ResourceSnapshot current(ResourceKind kind, String key) {
        return current.get(new ResourceKey(kind, key));
    }

    /// Returns superseded payloads in publish order.
    ///
    /// @return the released segments
    public @Unmodifiable List<MemorySegment> released() {
        return List.copyOf(released);
    }

    /// Identifies one watched resource.
    ///
    /// @param kind the resource family
    /// @param key the consumer key
    private record ResourceKey(ResourceKind kind, String key) {
        /// Validates the identity.
        private ResourceKey {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(key, "key");
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Resource key must not be empty");
            }
        }
    }
}
