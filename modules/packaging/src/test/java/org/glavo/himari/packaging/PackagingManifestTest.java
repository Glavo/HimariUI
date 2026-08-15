package org.glavo.himari.packaging;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies first-stable packaging documents.
@NotNullByDefault
final class PackagingManifestTest {
    /// Checks that BOM, SBOM, NOTICE, and the Native Image registry name production modules.
    @Test
    void emitsRequiredDocuments() {
        assertTrue(PackagingManifest.bomJson().contains("org.glavo.himari.rhi.d3d12"));
        assertTrue(PackagingManifest.sbomJson().contains("Apache-2.0"));
        assertTrue(PackagingManifest.notice().contains("Apache License"));
        assertTrue(PackagingManifest.nativeImageRegistryJson().contains("himari-rhi-d3d12"));
        assertFalse(PackagingManifest.modules().isEmpty());
        CapabilityDiagnostics diagnostics = CapabilityDiagnostics.windowsD3d12Sdr();
        assertFalse(diagnostics.hdrEnabled());
        assertTrue(diagnostics.toCanonicalJson().contains("color-managed-sdr"));
    }
}
