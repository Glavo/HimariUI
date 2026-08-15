package org.glavo.himari.objc;

import org.glavo.himari.objc.generated.ObjcBlockLayouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records one Objective-C block ABI observation.
///
/// @param status `layout-verified` or `environment-blocked`
/// @param detail a pointer-free diagnostic
/// @param policy the production block policy
/// @param layoutByteSize the verified layout size
@NotNullByDefault
public record ObjcBlockProbe(String status, String detail, ObjcBlockPolicy policy, long layoutByteSize) {
    /// Validates the observation.
    public ObjcBlockProbe {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(policy, "policy");
        if (layoutByteSize != ObjcBlockLayout.ABI64.byteSize()) {
            throw new IllegalArgumentException("Block layout size must match the documented 64-bit ABI");
        }
    }

    /// Verifies the generated layout and, on macOS, that block symbols resolve.
    ///
    /// @return the observation
    public static ObjcBlockProbe run() {
        if (ObjcBlockLayouts.BLOCK_LAYOUT.byteSize() != ObjcBlockLayout.ABI64.byteSize()) {
            throw new IllegalStateException("Generated block layout does not match the documented ABI");
        }
        if (!ObjcBlockLibraries.supportedHost()) {
            return new ObjcBlockProbe(
                    "environment-blocked",
                    "Objective-C blocks require macOS; host is " + System.getProperty("os.name", ""),
                    ObjcBlockPolicy.PREFER_BLOCK_FREE_APIS,
                    ObjcBlockLayout.ABI64.byteSize()
            );
        }
        try (ObjcBlockLibraries libraries = ObjcBlockLibraries.open()) {
            Objects.requireNonNull(libraries.bindings(), "bindings");
            return new ObjcBlockProbe(
                    "layout-verified",
                    "_Block_copy and _Block_release symbols resolved",
                    ObjcBlockPolicy.PREFER_BLOCK_FREE_APIS,
                    ObjcBlockLayout.ABI64.byteSize()
            );
        } catch (RuntimeException failure) {
            return new ObjcBlockProbe(
                    "environment-blocked",
                    "Objective-C block symbols failed: " + failure.getMessage(),
                    ObjcBlockPolicy.PREFER_BLOCK_FREE_APIS,
                    ObjcBlockLayout.ABI64.byteSize()
            );
        }
    }
}
