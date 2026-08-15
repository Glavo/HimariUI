package org.glavo.himari.ffi;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies isolated, deterministic direct system-library load recording.
@NotNullByDefault
final class NativeLibraryLoadAuditTest {
    /// Verifies sorting, deduplication, session exclusion, and post-close immutability.
    @Test
    void recordsSuccessfulLoadsWithinOneSession() {
        NativeLibraryLoadAudit.Session first = NativeLibraryLoadAudit.begin();
        try {
            NativeLibraryLoadAudit.recordSuccessfulLoad("user32.dll");
            NativeLibraryLoadAudit.recordSuccessfulLoad("kernel32.dll");
            NativeLibraryLoadAudit.recordSuccessfulLoad("user32.dll");
            assertEquals(List.of("kernel32.dll", "user32.dll"), first.loadedLibraries());
            assertThrows(IllegalStateException.class, NativeLibraryLoadAudit::begin);
        } finally {
            first.close();
        }

        NativeLibraryLoadAudit.recordSuccessfulLoad("ignored.dll");
        assertEquals(List.of("kernel32.dll", "user32.dll"), first.loadedLibraries());
        assertDoesNotThrow(first::close);

        try (NativeLibraryLoadAudit.Session second = NativeLibraryLoadAudit.begin()) {
            assertEquals(List.of(), second.loadedLibraries());
        }
    }

    /// Verifies that invalid names fail even when recording is inactive.
    @Test
    void rejectsBlankLibraryName() {
        assertThrows(IllegalArgumentException.class, () -> NativeLibraryLoadAudit.recordSuccessfulLoad("  "));
    }
}
