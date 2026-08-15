package org.glavo.himari.spikes.d3d12;

import org.glavo.himari.spikes.d3d12.generated.D3d12FfmBindings;
import org.glavo.himari.spikes.d3d12.generated.D3d12Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// Provides checked COM pointer, GUID, and `HRESULT` operations for the D3D12 spike.
@SuppressWarnings("restricted")
@NotNullByDefault
final class D3d12Native {
    /// `IUnknown::QueryInterface` byte offset in every COM vtable.
    static final long QUERY_INTERFACE_OFFSET = 0L;

    /// `IUnknown::Release` byte offset in every COM vtable.
    private static final long RELEASE_OFFSET = 16L;

    /// Prevents instantiation of this utility class.
    private D3d12Native() {
    }

    /// Allocates and encodes one Windows GUID.
    ///
    /// @param arena the destination arena
    /// @param spelling the canonical UUID spelling
    /// @return the 16-byte native GUID
    static MemorySegment guid(Arena arena, String spelling) {
        UUID value = UUID.fromString(spelling);
        long mostSignificant = value.getMostSignificantBits();
        long leastSignificant = value.getLeastSignificantBits();
        MemorySegment result = arena.allocate(D3d12Layouts.GUID);
        result.set(ValueLayout.JAVA_INT, D3d12Layouts.GUID_DATA1_OFFSET, (int) (mostSignificant >>> 32));
        result.set(ValueLayout.JAVA_SHORT, D3d12Layouts.GUID_DATA2_OFFSET, (short) (mostSignificant >>> 16));
        result.set(ValueLayout.JAVA_SHORT, D3d12Layouts.GUID_DATA3_OFFSET, (short) mostSignificant);
        for (int index = 0; index < 8; index++) {
            int shift = 56 - index * 8;
            result.set(ValueLayout.JAVA_BYTE, D3d12Layouts.GUID_DATA4_0_OFFSET + index,
                    (byte) (leastSignificant >>> shift));
        }
        return result;
    }

    /// Allocates a zero-initialized native pointer output cell.
    ///
    /// @param arena the destination arena
    /// @return the pointer cell
    static MemorySegment pointerCell(Arena arena) {
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return result;
    }

    /// Reads and validates a non-null pointer output cell.
    ///
    /// @param pointerCell the native pointer cell
    /// @param operation the producing operation
    /// @return the non-null native address
    /// @throws IllegalStateException if the cell contains `NULL`
    static MemorySegment requirePointer(MemorySegment pointerCell, String operation) {
        MemorySegment result = pointerCell.get(ValueLayout.ADDRESS, 0L);
        if (result.address() == 0L) {
            throw new IllegalStateException(operation + " succeeded but returned NULL");
        }
        return result;
    }

    /// Returns one COM vtable function pointer.
    ///
    /// @param interfacePointer the non-null COM interface pointer
    /// @param byteOffset the function's byte offset in the interface vtable
    /// @return the non-null function address
    /// @throws IllegalStateException if the interface, vtable, or function pointer is null
    static MemorySegment functionAt(MemorySegment interfacePointer, long byteOffset) {
        if (interfacePointer.address() == 0L) {
            throw new IllegalStateException("Cannot read a vtable from a NULL COM interface");
        }
        MemorySegment object = interfacePointer.reinterpret(D3d12Layouts.COM_INTERFACE_OBJECT.byteSize());
        MemorySegment vtableAddress = object.get(ValueLayout.ADDRESS, D3d12Layouts.COM_INTERFACE_OBJECT_VTABLE_OFFSET);
        if (vtableAddress.address() == 0L) {
            throw new IllegalStateException("COM interface has a NULL vtable");
        }
        MemorySegment vtable = vtableAddress.reinterpret(Math.addExact(byteOffset, ValueLayout.ADDRESS.byteSize()));
        MemorySegment function = vtable.get(ValueLayout.ADDRESS, byteOffset);
        if (function.address() == 0L) {
            throw new IllegalStateException("COM vtable function at byte offset " + byteOffset + " is NULL");
        }
        return function;
    }

    /// Requires a successful non-negative `HRESULT`.
    ///
    /// @param operation the failed operation's name
    /// @param result the native `HRESULT`
    /// @throws IllegalStateException if the result represents failure
    static void requireSuccess(String operation, int result) {
        if (result < 0) {
            throw hresultFailure(operation, result);
        }
    }

    /// Creates an exception containing signed and hexadecimal `HRESULT` spellings.
    ///
    /// @param operation the failed operation's name
    /// @param result the native `HRESULT`
    /// @return the diagnostic exception
    static IllegalStateException hresultFailure(String operation, int result) {
        return new IllegalStateException(operation + " failed with HRESULT " + result
                + " (0x" + Integer.toHexString(result) + ')');
    }

    /// Tracks and releases every owned COM reference in reverse acquisition order.
    @NotNullByDefault
    static final class ComTracker implements AutoCloseable {
        /// Owned COM references in acquisition order.
        private final List<MemorySegment> references = new ArrayList<>();

        /// Whether [#close()] has completed.
        private boolean closed;

        /// Number of successfully released owned references.
        private int releasedCount;

        /// Registers one newly owned non-null COM reference.
        ///
        /// @param reference the reference to release during [#close()]
        /// @return the same reference
        /// @throws IllegalStateException if this tracker is closed
        MemorySegment own(MemorySegment reference) {
            if (closed) {
                throw new IllegalStateException("COM tracker is closed");
            }
            if (reference.address() == 0L) {
                throw new IllegalArgumentException("Owned COM reference must not be NULL");
            }
            references.add(reference);
            return reference;
        }

        /// Returns the number of references registered with this tracker.
        ///
        /// @return the ownership count
        int ownedCount() {
            return references.size();
        }

        /// Returns the number of references released by this tracker.
        ///
        /// @return the release count
        int releasedCount() {
            return releasedCount;
        }

        /// Releases all owned references in reverse acquisition order.
        ///
        /// Repeated calls have no effect. A zero return from `IUnknown::Release` is valid and means that the native
        /// object was destroyed.
        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            @Nullable Throwable firstFailure = null;
            for (int index = references.size() - 1; index >= 0; index--) {
                MemorySegment reference = references.get(index);
                try {
                    D3d12FfmBindings.invokeIunknownReleasePointer(
                            functionAt(reference, RELEASE_OFFSET),
                            reference
                    );
                    releasedCount++;
                } catch (RuntimeException | Error failure) {
                    if (firstFailure == null) {
                        firstFailure = failure;
                    } else {
                        firstFailure.addSuppressed(failure);
                    }
                }
            }
            references.clear();
            if (firstFailure instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            if (firstFailure instanceof Error error) {
                throw error;
            }
        }
    }
}
