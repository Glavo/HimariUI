package org.glavo.himari.objc;

import org.jetbrains.annotations.NotNullByDefault;

/// States the first-stable production policy for Objective-C blocks.
@NotNullByDefault
public enum ObjcBlockPolicy {
    /// Production code prefers block-free system APIs until a later ADR permits verified block use.
    PREFER_BLOCK_FREE_APIS
}
