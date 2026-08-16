package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;

/// Interns first-stable software-pipeline identities until a declared occupancy budget is full.
///
/// A miss allocates the next positive pipeline id. Insertion that would exceed [`#maxEntries()`]
/// is rejected. The cache is not thread-safe.
@NotNullByDefault
public final class PipelineCache {
    /// Maximum interned keys.
    private final int maxEntries;

    /// Interned entries by key.
    private final HashMap<Key, Entry> entries = new HashMap<>();

    /// Next pipeline id to assign.
    private int nextId = 1;

    /// Creates an empty cache with a positive occupancy budget.
    ///
    /// @param maxEntries the positive maximum interned key count
    public PipelineCache(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("Pipeline cache occupancy budget must be positive");
        }
        this.maxEntries = maxEntries;
    }

    /// Returns the occupancy budget.
    ///
    /// @return the positive maximum interned key count
    public int maxEntries() {
        return maxEntries;
    }

    /// Returns the number of interned keys.
    ///
    /// @return the nonnegative occupancy
    public int entryCount() {
        return entries.size();
    }

    /// Returns a previously interned entry.
    ///
    /// @param key the pipeline identity
    /// @return the entry, or `null` when the key is not cached
    public @Nullable Entry locate(Key key) {
        Objects.requireNonNull(key, "key");
        return entries.get(key);
    }

    /// Interns `key` when the remaining budget can hold it.
    ///
    /// @param key the pipeline identity
    /// @return the interned entry, or `null` when the budget is exhausted
    public @Nullable Entry intern(Key key) {
        Objects.requireNonNull(key, "key");
        @Nullable Entry existing = entries.get(key);
        if (existing != null) {
            return existing;
        }
        if (entries.size() >= maxEntries) {
            return null;
        }
        Entry created = new Entry(key, nextId++);
        entries.put(key, created);
        return created;
    }

    /// Removes every interned key.
    public void clear() {
        entries.clear();
        nextId = 1;
    }

    /// Identifies one software pipeline configuration.
    ///
    /// @param topology the primitive topology name
    /// @param format the destination pixel-format name
    /// @param blend the blend-mode name
    public record Key(String topology, String format, String blend) {
        /// Validates the key.
        public Key {
            Objects.requireNonNull(topology, "topology");
            Objects.requireNonNull(format, "format");
            Objects.requireNonNull(blend, "blend");
            if (topology.isEmpty() || format.isEmpty() || blend.isEmpty()) {
                throw new IllegalArgumentException("Pipeline key fields must be non-empty");
            }
        }
    }

    /// Stores one interned pipeline identity.
    ///
    /// @param key the configuration
    /// @param id the positive assigned identifier
    public record Entry(Key key, int id) {
        /// Validates the entry.
        public Entry {
            Objects.requireNonNull(key, "key");
            if (id <= 0) {
                throw new IllegalArgumentException("Pipeline id must be positive");
            }
        }
    }
}
