package org.glavo.himari.objc;

import org.glavo.himari.objc.generated.ObjcBlockLayouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/// Fills one 64-bit Apple Objective-C block object using the documented ABI offsets.
///
/// Construction is host-independent and does not load `libobjc` or invoke the block.
@NotNullByDefault
public final class ObjcBlockObject {
    /// Native 32-byte object.
    private final MemorySegment object;

    /// Creates an owner for an already-filled object.
    ///
    /// @param object the native storage
    private ObjcBlockObject(MemorySegment object) {
        this.object = object;
    }

    /// Allocates and fills one block object in `arena`.
    ///
    /// @param arena the owning arena
    /// @param isa the `isa` pointer
    /// @param flags the block flags word
    /// @param invoke the invoke-function pointer
    /// @param descriptor the descriptor pointer
    /// @return the filled object
    public static ObjcBlockObject allocate(
            Arena arena,
            MemorySegment isa,
            int flags,
            MemorySegment invoke,
            MemorySegment descriptor
    ) {
        Objects.requireNonNull(arena, "arena");
        Objects.requireNonNull(isa, "isa");
        Objects.requireNonNull(invoke, "invoke");
        Objects.requireNonNull(descriptor, "descriptor");
        MemorySegment object = arena.allocate(ObjcBlockLayouts.BLOCK_LAYOUT);
        object.fill((byte) 0);
        object.set(ValueLayout.ADDRESS, ObjcBlockLayouts.BLOCK_LAYOUT_ISA_OFFSET, isa);
        object.set(ValueLayout.JAVA_INT, ObjcBlockLayouts.BLOCK_LAYOUT_FLAGS_OFFSET, flags);
        object.set(ValueLayout.JAVA_INT, ObjcBlockLayouts.BLOCK_LAYOUT_RESERVED_OFFSET, 0);
        object.set(ValueLayout.ADDRESS, ObjcBlockLayouts.BLOCK_LAYOUT_INVOKE_OFFSET, invoke);
        object.set(ValueLayout.ADDRESS, ObjcBlockLayouts.BLOCK_LAYOUT_DESCRIPTOR_OFFSET, descriptor);
        return new ObjcBlockObject(object);
    }

    /// Returns the native object pointer.
    ///
    /// @return the pointer
    public MemorySegment pointer() {
        return object;
    }

    /// Returns the stored `isa` pointer.
    ///
    /// @return the `isa` pointer
    public MemorySegment isa() {
        return object.get(ValueLayout.ADDRESS, ObjcBlockLayouts.BLOCK_LAYOUT_ISA_OFFSET);
    }

    /// Returns the stored flags word.
    ///
    /// @return the flags
    public int flags() {
        return object.get(ValueLayout.JAVA_INT, ObjcBlockLayouts.BLOCK_LAYOUT_FLAGS_OFFSET);
    }

    /// Returns the stored invoke-function pointer.
    ///
    /// @return the invoke pointer
    public MemorySegment invoke() {
        return object.get(ValueLayout.ADDRESS, ObjcBlockLayouts.BLOCK_LAYOUT_INVOKE_OFFSET);
    }

    /// Returns the stored descriptor pointer.
    ///
    /// @return the descriptor pointer
    public MemorySegment descriptor() {
        return object.get(ValueLayout.ADDRESS, ObjcBlockLayouts.BLOCK_LAYOUT_DESCRIPTOR_OFFSET);
    }
}
