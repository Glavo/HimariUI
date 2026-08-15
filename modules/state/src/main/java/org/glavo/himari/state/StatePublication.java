package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.IdentityHashMap;

/// Stores one immutable, atomically published set of source values and versions.
///
/// The storage is split into fixed-size chunks. Appending a source or publishing a transaction
/// copies the outer arrays and only the affected chunks, so an older [StateSnapshot] remains stable
/// without copying every source on every commit.
@NotNullByDefault
final class StatePublication {
    /// The base-two logarithm of the number of slots in a chunk.
    private static final int CHUNK_SHIFT = 5;

    /// The number of slots in each chunk.
    private static final int CHUNK_SIZE = 1 << CHUNK_SHIFT;

    /// Selects the offset within a chunk.
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;

    /// The domain epoch represented by this publication.
    private final long epoch;

    /// The number of registered source slots represented by this publication.
    private final int size;

    /// The immutable value chunks.
    private final @Nullable Object @Unmodifiable [] @Unmodifiable [] values;

    /// The immutable version chunks.
    private final long @Unmodifiable [] @Unmodifiable [] versions;

    /// Creates an immutable publication over already-private chunk arrays.
    ///
    /// @param epoch the represented domain epoch
    /// @param size the number of registered source slots
    /// @param values the private value chunks
    /// @param versions the private version chunks
    private StatePublication(
            long epoch,
            int size,
            @Nullable Object @Unmodifiable [] @Unmodifiable [] values,
            long @Unmodifiable [] @Unmodifiable [] versions
    ) {
        this.epoch = epoch;
        this.size = size;
        this.values = values;
        this.versions = versions;
    }

    /// Creates the initial empty publication.
    ///
    /// @return an empty publication at epoch zero
    static StatePublication empty() {
        return new StatePublication(0L, 0, new Object[0][], new long[0][]);
    }

    /// Returns the represented domain epoch.
    ///
    /// @return the epoch
    long epoch() {
        return epoch;
    }

    /// Returns the number of represented sources.
    ///
    /// @return the source count
    int size() {
        return size;
    }

    /// Returns the value stored in a represented source slot.
    ///
    /// @param slot the source slot
    /// @return the stored value, which may be `null`
    /// @throws IllegalArgumentException if the slot was created after this publication
    @Nullable Object value(int slot) {
        checkSlot(slot);
        return values[chunkIndex(slot)][chunkOffset(slot)];
    }

    /// Returns the version stored in a represented source slot.
    ///
    /// @param slot the source slot
    /// @return the stored source version
    /// @throws IllegalArgumentException if the slot was created after this publication
    long version(int slot) {
        checkSlot(slot);
        return versions[chunkIndex(slot)][chunkOffset(slot)];
    }

    /// Returns a same-epoch publication with one newly registered source.
    ///
    /// @param initialValue the initial value, which may be `null`
    /// @return a publication containing the appended source at version zero
    StatePublication append(@Nullable Object initialValue) {
        int slot = size;
        int chunkIndex = chunkIndex(slot);
        int chunkOffset = chunkOffset(slot);
        int requiredChunkCount = chunkIndex + 1;

        @Nullable Object[][] newValues = Arrays.copyOf(values, requiredChunkCount);
        long[][] newVersions = Arrays.copyOf(versions, requiredChunkCount);
        if (chunkOffset == 0) {
            newValues[chunkIndex] = new Object[CHUNK_SIZE];
            newVersions[chunkIndex] = new long[CHUNK_SIZE];
        } else {
            newValues[chunkIndex] = values[chunkIndex].clone();
            newVersions[chunkIndex] = versions[chunkIndex].clone();
        }
        newValues[chunkIndex][chunkOffset] = initialValue;
        return new StatePublication(epoch, size + 1, newValues, newVersions);
    }

    /// Returns a new-epoch publication containing every supplied semantic change.
    ///
    /// @param changes the final values for sources that changed semantically
    /// @param nextEpoch the next domain epoch
    /// @return the updated publication
    StatePublication publish(
            IdentityHashMap<AbstractStateSource, @Nullable Object> changes,
            long nextEpoch
    ) {
        @Nullable Object[][] newValues = values.clone();
        long[][] newVersions = versions.clone();
        boolean[] copiedChunks = new boolean[values.length];

        for (var entry : changes.entrySet()) {
            int slot = entry.getKey().slot();
            int chunkIndex = chunkIndex(slot);
            int chunkOffset = chunkOffset(slot);
            if (!copiedChunks[chunkIndex]) {
                newValues[chunkIndex] = values[chunkIndex].clone();
                newVersions[chunkIndex] = versions[chunkIndex].clone();
                copiedChunks[chunkIndex] = true;
            }
            newValues[chunkIndex][chunkOffset] = entry.getValue();
            newVersions[chunkIndex][chunkOffset]++;
        }
        return new StatePublication(nextEpoch, size, newValues, newVersions);
    }

    /// Verifies that a source slot is represented by this publication.
    ///
    /// @param slot the source slot
    /// @throws IllegalArgumentException if the slot is outside this publication
    private void checkSlot(int slot) {
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException(
                    "State source slot " + slot + " is not present in snapshot with " + size + " sources"
            );
        }
    }

    /// Returns the chunk index for a source slot.
    ///
    /// @param slot the non-negative source slot
    /// @return the chunk index
    private static int chunkIndex(int slot) {
        return slot >>> CHUNK_SHIFT;
    }

    /// Returns the offset within a chunk for a source slot.
    ///
    /// @param slot the non-negative source slot
    /// @return the chunk offset
    private static int chunkOffset(int slot) {
        return slot & CHUNK_MASK;
    }
}
