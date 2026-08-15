package org.glavo.himari.objc;

import org.jetbrains.annotations.NotNullByDefault;

/// Records the 64-bit Apple Objective-C block object layout.
///
/// @param byteSize the block object size in bytes
/// @param isaOffset the `isa` pointer offset
/// @param flagsOffset the flags offset
/// @param reservedOffset the reserved word offset
/// @param invokeOffset the invoke-function offset
/// @param descriptorOffset the descriptor pointer offset
@NotNullByDefault
public record ObjcBlockLayout(
        long byteSize,
        long isaOffset,
        long flagsOffset,
        long reservedOffset,
        long invokeOffset,
        long descriptorOffset
) {
    /// The verified 64-bit Apple block object layout.
    public static final ObjcBlockLayout ABI64 = new ObjcBlockLayout(32L, 0L, 8L, 12L, 16L, 24L);

    /// Validates the layout.
    public ObjcBlockLayout {
        if (byteSize != 32L || isaOffset != 0L || flagsOffset != 8L || reservedOffset != 12L
                || invokeOffset != 16L || descriptorOffset != 24L) {
            throw new IllegalArgumentException("Only the documented 64-bit Apple block layout is accepted");
        }
    }
}
