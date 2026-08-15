package org.glavo.himari.packaging;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/// Runs one `jlink` recipe and records whether a custom runtime image was produced.
@NotNullByDefault
public final class JlinkImage {
    /// Prevents instantiation.
    private JlinkImage() {
    }

    /// Builds the image described by `recipe`.
    ///
    /// Existing output is deleted first because `jlink` refuses a nonempty destination. The
    /// result reports `builtImage` only when `jlink` exits 0 and the output directory exists.
    ///
    /// @param recipe the invocation
    /// @return the observation
    public static Result build(JlinkRecipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        Path output = Path.of(recipe.outputDirectory());
        try {
            deleteRecursively(output);
            Path jlink = jlinkExecutable();
            ArrayList<String> command = new ArrayList<>();
            command.add(jlink.toString());
            command.addAll(recipe.commandLine().subList(1, recipe.commandLine().size()));
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            boolean built = exit == 0 && Files.isDirectory(output);
            @Nullable Path launcher = built ? launcherPath(output) : null;
            return new Result(built, exit, output.toString(), launcher == null ? null : launcher.toString(), log.strip());
        } catch (IOException | InterruptedException failure) {
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new Result(false, -1, output.toString(), null, failure.getMessage());
        }
    }

    /// Resolves `jlink` from `java.home`.
    private static Path jlinkExecutable() {
        String file = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "jlink.exe"
                : "jlink";
        return Path.of(System.getProperty("java.home", ""), "bin", file);
    }

    /// Finds the generated launcher, if present.
    private static @Nullable Path launcherPath(Path image) {
        Path unix = image.resolve("bin").resolve("himari");
        if (Files.isRegularFile(unix)) {
            return unix;
        }
        Path windows = image.resolve("bin").resolve("himari.bat");
        return Files.isRegularFile(windows) ? windows : null;
    }

    /// Deletes `root` if it exists.
    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    /// Observation of one `jlink` invocation.
    ///
    /// @param builtImage whether the image directory exists after a zero exit
    /// @param exitCode the process exit code, or `-1` when the process did not start
    /// @param outputDirectory the requested image directory
    /// @param launcherPath the generated launcher, or `null`
    /// @param detail the combined stdout/stderr or failure message
    public record Result(
            boolean builtImage,
            int exitCode,
            String outputDirectory,
            @Nullable String launcherPath,
            String detail
    ) {
        /// Validates the observation.
        public Result {
            Objects.requireNonNull(outputDirectory, "outputDirectory");
            Objects.requireNonNull(detail, "detail");
        }

        /// Encodes the observation as JSON.
        ///
        /// @return the document
        public String toJson() {
            String launcher = launcherPath == null ? "null" : "\"" + escape(launcherPath) + "\"";
            return """
                    {
                      "schema": "himari-jlink-image-v1",
                      "builtImage": %s,
                      "exitCode": %d,
                      "outputDirectory": "%s",
                      "launcherPath": %s,
                      "detail": "%s"
                    }
                    """.formatted(
                    builtImage,
                    exitCode,
                    escape(outputDirectory),
                    launcher,
                    escape(detail)
            );
        }

        /// Escapes one JSON string fragment.
        private static String escape(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
        }
    }
}
