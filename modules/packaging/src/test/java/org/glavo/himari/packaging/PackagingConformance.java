package org.glavo.himari.packaging;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// Writes M11 packaging evidence, including a real `jlink` image when the module path is supplied.
@NotNullByDefault
public final class PackagingConformance {
    /// Prevents instantiation.
    private PackagingConformance() {
    }

    /// Writes BOM, SBOM, NOTICE, Native Image registry, and jlink artifacts.
    ///
    /// @param arguments output directory, optional module-path, optional image directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1 && arguments.length != 3) {
            throw new IllegalArgumentException("Expected output directory or output, module-path, and image directory");
        }
        String bom = PackagingManifest.bomJson();
        String sbom = PackagingManifest.sbomJson();
        String notice = PackagingManifest.notice();
        String registry = PackagingManifest.nativeImageRegistryJson();
        String diagnostics = CapabilityDiagnostics.windowsD3d12Sdr().toCanonicalJson();
        if (!bom.contains("org.glavo.himari.desktop") || !registry.contains("d3d12")) {
            throw new IllegalStateException("Packaging documents omitted a required backend or desktop entry");
        }
        if (diagnostics.contains("\"hdrEnabled\":true")) {
            throw new IllegalStateException("Diagnostics claimed HDR output");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        JlinkRecipe recipe;
        JlinkImage.Result image;
        if (arguments.length == 3) {
            ArrayList<String> modulePath = new ArrayList<>();
            for (String entry : arguments[1].split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (!entry.isBlank()) {
                    modulePath.add(entry);
                }
            }
            recipe = new JlinkRecipe(
                    List.copyOf(modulePath),
                    PackagingManifest.modules(),
                    arguments[2],
                    "himari=org.glavo.himari.desktop/org.glavo.himari.desktop.HimariDesktop",
                    List.of(
                            "--strip-debug",
                            "--no-header-files",
                            "--no-man-pages",
                            "--add-options=--enable-native-access=org.glavo.himari.platform.windows"
                    )
            );
            image = JlinkImage.build(recipe);
            if (!image.builtImage()) {
                throw new IllegalStateException("jlink did not produce an image: " + image.detail());
            }
        } else {
            recipe = PackagingManifest.jlinkRecipe();
            image = new JlinkImage.Result(false, -1, recipe.outputDirectory(), null, "module-path was not supplied");
        }
        Files.writeString(output.resolve("bom.json"), bom, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("sbom.json"), sbom, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("notice.txt"), notice, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("native-image-registry.json"), registry, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("jlink-recipe.json"), recipe.toJson(), StandardCharsets.UTF_8);
        Files.writeString(output.resolve("jlink-image.json"), image.toJson(), StandardCharsets.UTF_8);
        Files.writeString(output.resolve("diagnostics.json"), diagnostics, StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m11-packaging",
                          "workPackage": "PACK-001",
                          "status": "passed",
                          "moduleCount": %d,
                          "nativePayload": false,
                          "jlinkBuiltImage": %s
                        }
                        """.formatted(PackagingManifest.modules().size(), image.builtImage()),
                StandardCharsets.UTF_8
        );
    }
}
