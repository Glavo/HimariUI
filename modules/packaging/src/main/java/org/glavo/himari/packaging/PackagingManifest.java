package org.glavo.himari.packaging;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/// Emits first-stable BOM, SBOM, NOTICE, and Native Image backend registry documents.
@NotNullByDefault
public final class PackagingManifest {
    /// Production modules shipped by the first-stable desktop BOM.
    private static final @Unmodifiable List<String> MODULES = List.of(
            "org.glavo.himari.ffi",
            "org.glavo.himari.font",
            "org.glavo.himari.graphics",
            "org.glavo.himari.layout",
            "org.glavo.himari.platform.api",
            "org.glavo.himari.platform.headless",
            "org.glavo.himari.platform.windows",
            "org.glavo.himari.platform.wayland",
            "org.glavo.himari.platform.macos",
            "org.glavo.himari.render.software",
            "org.glavo.himari.rhi.d3d12",
            "org.glavo.himari.rhi.vulkan",
            "org.glavo.himari.rhi.metal",
            "org.glavo.himari.objc",
            "org.glavo.himari.runtime",
            "org.glavo.himari.state",
            "org.glavo.himari.text",
            "org.glavo.himari.controls",
            "org.glavo.himari.inspector",
            "org.glavo.himari.packaging"
    );

    /// Generated Native Image metadata resource names.
    private static final @Unmodifiable List<String> NATIVE_IMAGE_METADATA = List.of(
            "META-INF/native-image/org.glavo.himari/himari-platform-windows/reachability-metadata.json",
            "META-INF/native-image/org.glavo.himari/himari-platform-wayland/reachability-metadata.json",
            "META-INF/native-image/org.glavo.himari/himari-platform-macos/reachability-metadata.json",
            "META-INF/native-image/org.glavo.himari/himari-rhi-d3d12/reachability-metadata.json",
            "META-INF/native-image/org.glavo.himari/himari-rhi-vulkan/reachability-metadata.json",
            "META-INF/native-image/org.glavo.himari/himari-rhi-metal/reachability-metadata.json",
            "META-INF/native-image/org.glavo.himari/himari-objc/reachability-metadata.json"
    );

    /// Published artifact version.
    public static final String VERSION = "0.1.0-SNAPSHOT";

    /// Prevents instantiation.
    private PackagingManifest() {
    }

    /// Returns the production module names in BOM order.
    ///
    /// @return the modules
    public static @Unmodifiable List<String> modules() {
        return MODULES;
    }

    /// Encodes the Maven-style BOM as JSON.
    ///
    /// @return the document
    public static String bomJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"schema\":\"himari-bom-v1\",\"group\":\"org.glavo.himari\",\"version\":\"")
                .append(VERSION)
                .append("\",\"modules\":[");
        for (int index = 0; index < MODULES.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(MODULES.get(index)).append('"');
        }
        json.append("]}");
        return json.toString();
    }

    /// Encodes a CycloneDX-shaped SBOM without claiming signed provenance.
    ///
    /// @return the document
    public static String sbomJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"bomFormat\":\"CycloneDX\",\"specVersion\":\"1.5\",\"version\":1,\"components\":[");
        for (int index = 0; index < MODULES.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"type\":\"library\",\"name\":\"")
                    .append(MODULES.get(index))
                    .append("\",\"version\":\"")
                    .append(VERSION)
                    .append("\",\"licenses\":[{\"license\":{\"id\":\"Apache-2.0\"}}]}");
        }
        json.append("]}");
        return json.toString();
    }

    /// Returns the first-stable NOTICE text.
    ///
    /// @return the notice
    public static String notice() {
        return """
                HimariUI
                Copyright 2026 Glavo and contributors

                Licensed under the Apache License, Version 2.0.
                First-stable desktop artifacts contain no project-built native libraries.
                Native access uses generated FFM bindings to operating-system libraries only.
                """;
    }

    /// Encodes the static Native Image backend registry.
    ///
    /// @return the document
    public static String nativeImageRegistryJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"schema\":\"himari-ni-registry-v1\",\"backends\":[")
                .append("{\"id\":\"windows\",\"module\":\"org.glavo.himari.platform.windows\"},")
                .append("{\"id\":\"wayland\",\"module\":\"org.glavo.himari.platform.wayland\"},")
                .append("{\"id\":\"macos\",\"module\":\"org.glavo.himari.platform.macos\"},")
                .append("{\"id\":\"d3d12\",\"module\":\"org.glavo.himari.rhi.d3d12\"},")
                .append("{\"id\":\"vulkan\",\"module\":\"org.glavo.himari.rhi.vulkan\"},")
                .append("{\"id\":\"metal\",\"module\":\"org.glavo.himari.rhi.metal\"},")
                .append("{\"id\":\"objc\",\"module\":\"org.glavo.himari.objc\"}")
                .append("],\"metadata\":[");
        for (int index = 0; index < NATIVE_IMAGE_METADATA.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(NATIVE_IMAGE_METADATA.get(index)).append('"');
        }
        json.append("]}");
        return json.toString();
    }
}
