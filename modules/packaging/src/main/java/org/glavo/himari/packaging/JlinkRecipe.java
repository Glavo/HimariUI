package org.glavo.himari.packaging;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Describes a `jlink` invocation without claiming that an image was built.
///
/// @param modulePath the `--module-path` entries
/// @param addModules the `--add-modules` names
/// @param outputDirectory the `--output` directory
/// @param launcher the optional `--launcher` value, or `null`
/// @param extraOptions additional `jlink` flags
@NotNullByDefault
public record JlinkRecipe(
        @Unmodifiable List<String> modulePath,
        @Unmodifiable List<String> addModules,
        String outputDirectory,
        @Nullable String launcher,
        @Unmodifiable List<String> extraOptions
) {
    /// Validates the recipe.
    public JlinkRecipe {
        Objects.requireNonNull(modulePath, "modulePath");
        Objects.requireNonNull(addModules, "addModules");
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(extraOptions, "extraOptions");
        modulePath = List.copyOf(modulePath);
        addModules = List.copyOf(addModules);
        extraOptions = List.copyOf(extraOptions);
        if (outputDirectory.isBlank()) {
            throw new IllegalArgumentException("jlink output directory must not be blank");
        }
        if (addModules.isEmpty()) {
            throw new IllegalArgumentException("jlink must name at least one module");
        }
        for (String module : addModules) {
            Objects.requireNonNull(module, "addModules element");
        }
        for (String entry : modulePath) {
            Objects.requireNonNull(entry, "modulePath element");
        }
        for (String option : extraOptions) {
            Objects.requireNonNull(option, "extraOptions element");
        }
        if (launcher != null && launcher.isBlank()) {
            throw new IllegalArgumentException("jlink launcher must not be blank");
        }
    }

    /// Returns the first-stable desktop `jlink` recipe.
    ///
    /// The recipe is a command description only. It does not run `jlink` or assert that an image
    /// exists.
    ///
    /// @return the recipe
    public static JlinkRecipe firstStable() {
        return new JlinkRecipe(
                List.of("$JAVA_HOME/jmods", "build/modules"),
                PackagingManifest.modules(),
                "build/conformance/m11-packaging/image",
                "himari=org.glavo.himari.desktop/org.glavo.himari.desktop.HimariDesktop",
                List.of(
                        "--strip-debug",
                        "--no-header-files",
                        "--no-man-pages",
                        "--add-options=--enable-native-access=org.glavo.himari.platform.windows"
                )
        );
    }

    /// Returns the argv that would be passed to `jlink`.
    ///
    /// @return the command line
    public @Unmodifiable List<String> commandLine() {
        ArrayList<String> command = new ArrayList<>();
        command.add("jlink");
        if (!modulePath.isEmpty()) {
            command.add("--module-path");
            command.add(String.join(File.pathSeparator, modulePath));
        }
        command.add("--add-modules");
        command.add(String.join(",", addModules));
        command.add("--output");
        command.add(outputDirectory);
        if (launcher != null) {
            command.add("--launcher");
            command.add(launcher);
        }
        command.addAll(extraOptions);
        return List.copyOf(command);
    }

    /// Encodes the recipe as JSON without claiming that an image exists.
    ///
    /// @return the document
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"schema\":\"himari-jlink-recipe-v1\",\"outputDirectory\":\"")
                .append(escape(outputDirectory))
                .append("\",\"addModules\":");
        appendStrings(json, addModules);
        json.append(",\"modulePath\":");
        appendStrings(json, modulePath);
        json.append(",\"extraOptions\":");
        appendStrings(json, extraOptions);
        json.append(",\"launcher\":");
        if (launcher == null) {
            json.append("null");
        } else {
            json.append('"').append(escape(launcher)).append('"');
        }
        json.append(",\"command\":");
        appendStrings(json, commandLine());
        json.append('}');
        return json.toString();
    }

    /// Appends a JSON string array.
    private static void appendStrings(StringBuilder json, List<String> values) {
        json.append('[');
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(escape(values.get(index))).append('"');
        }
        json.append(']');
    }

    /// Escapes one JSON string fragment.
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
