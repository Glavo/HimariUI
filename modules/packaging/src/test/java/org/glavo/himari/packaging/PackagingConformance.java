package org.glavo.himari.packaging;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes M11 packaging evidence.
@NotNullByDefault
public final class PackagingConformance {
    /// Prevents instantiation.
    private PackagingConformance() {
    }

    /// Writes BOM, SBOM, NOTICE, and Native Image registry artifacts.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        String bom = PackagingManifest.bomJson();
        String sbom = PackagingManifest.sbomJson();
        String notice = PackagingManifest.notice();
        String registry = PackagingManifest.nativeImageRegistryJson();
        String diagnostics = CapabilityDiagnostics.windowsD3d12Sdr().toCanonicalJson();
        if (!bom.contains("org.glavo.himari.platform.windows") || !registry.contains("d3d12")) {
            throw new IllegalStateException("Packaging documents omitted a required backend");
        }
        if (diagnostics.contains("\"hdrEnabled\":true")) {
            throw new IllegalStateException("Diagnostics claimed HDR output");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(output.resolve("bom.json"), bom, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("sbom.json"), sbom, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("notice.txt"), notice, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("native-image-registry.json"), registry, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("diagnostics.json"), diagnostics, StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m11-packaging",
                          "workPackage": "PACK-001",
                          "status": "passed",
                          "moduleCount": %d,
                          "nativePayload": false
                        }
                        """.formatted(PackagingManifest.modules().size()),
                StandardCharsets.UTF_8
        );
    }
}
