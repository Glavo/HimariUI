package org.glavo.himari.packaging;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/// Builds or records a Native Image of the Headless Counter sample.
///
/// [`#probe(Path, List)`] requires the sample classpath. [`#builtImage()`] is true only when the
/// `himari-counter` executable exists after the invocation. A missing toolchain is
/// `environment-blocked`. An empty classpath or a failed compile is recorded without claiming an
/// image.
@NotNullByDefault
public final class NativeImageProbe {
    /// Sample main class packaged by first-stable Counter.
    public static final String COUNTER_MAIN = "org.glavo.himari.samples.counter.V0CounterApp";

    /// Image base name written under the output directory.
    public static final String IMAGE_NAME = "himari-counter";

    /// Prevents instantiation.
    private NativeImageProbe() {
    }

    /// Returns the `-o` path passed to Native Image, without a Windows `.exe` suffix.
    ///
    /// @param outputDirectory the image directory
    /// @return `outputDirectory/himari-counter`
    public static Path imageOutputBase(Path outputDirectory) {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        return outputDirectory.resolve(IMAGE_NAME);
    }

    /// Returns the executable Native Image writes for `outputDirectory`.
    ///
    /// On Windows the toolchain appends `.exe` to [`#imageOutputBase(Path)`].
    ///
    /// @param outputDirectory the image directory
    /// @return the executable path, preferring a file that already exists
    public static Path imageFile(Path outputDirectory) {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Path unix = imageOutputBase(outputDirectory);
        Path windows = outputDirectory.resolve(IMAGE_NAME + ".exe");
        if (Files.isRegularFile(windows)) {
            return windows;
        }
        if (Files.isRegularFile(unix)) {
            return unix;
        }
        return windowsHost() ? windows : unix;
    }

    /// Builds the `native-image` command for the Counter classpath.
    ///
    /// @param nativeImage the toolchain executable
    /// @param outputDirectory the image directory
    /// @param classPath the Counter jar and its runtime jars
    /// @return the argument list, including `-cp` and [`#COUNTER_MAIN`]
    public static @Unmodifiable List<String> commandLine(
            Path nativeImage,
            Path outputDirectory,
            List<String> classPath
    ) {
        Objects.requireNonNull(nativeImage, "nativeImage");
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(classPath, "classPath");
        if (classPath.isEmpty()) {
            throw new IllegalArgumentException("Counter classpath must not be empty");
        }
        ArrayList<String> command = new ArrayList<>();
        command.add(nativeImage.toString());
        command.add("--no-fallback");
        command.add("-cp");
        command.add(String.join(File.pathSeparator, classPath));
        command.add("-o");
        command.add(imageOutputBase(outputDirectory).toString());
        command.add(COUNTER_MAIN);
        return List.copyOf(command);
    }

    /// Probes the toolchain and, when `classPath` is present, invokes Native Image.
    ///
    /// @param outputDirectory destination for `himari-counter`
    /// @param classPath the Counter jar and runtime jars, empty when unknown
    /// @return the observation
    public static Result probe(Path outputDirectory, List<String> classPath) {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(classPath, "classPath");
        List<String> path = List.copyOf(classPath);
        @Nullable Path executable = locate();
        if (path.isEmpty()) {
            return new Result(
                    false,
                    executable == null,
                    -1,
                    outputDirectory.toString(),
                    executable == null ? null : executable.toString(),
                    "",
                    COUNTER_MAIN,
                    executable == null
                            ? "environment-blocked: native-image was not found on GRAALVM_HOME or PATH"
                            : "classpath was not supplied"
            );
        }
        if (!referencesCounter(path)) {
            return new Result(
                    false,
                    false,
                    -1,
                    outputDirectory.toString(),
                    executable == null ? null : executable.toString(),
                    String.join(File.pathSeparator, path),
                    COUNTER_MAIN,
                    "classpath does not include the Counter sample"
            );
        }
        if (executable == null) {
            return new Result(
                    false,
                    true,
                    -1,
                    outputDirectory.toString(),
                    null,
                    String.join(File.pathSeparator, path),
                    COUNTER_MAIN,
                    "environment-blocked: native-image was not found on GRAALVM_HOME or PATH"
            );
        }
        List<String> command = commandLine(executable, outputDirectory, path);
        try {
            Files.createDirectories(outputDirectory);
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            Path image = imageFile(outputDirectory);
            boolean built = Files.isRegularFile(image);
            String detail = log.isBlank() ? "native-image exited " + exit : log.strip();
            if (!built && exit == 0) {
                detail = "native-image exited 0 but " + image + " is missing; " + detail;
            }
            return new Result(
                    built,
                    false,
                    exit,
                    outputDirectory.toString(),
                    executable.toString(),
                    String.join(File.pathSeparator, path),
                    COUNTER_MAIN,
                    detail
            );
        } catch (IOException | InterruptedException failure) {
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new Result(
                    false,
                    false,
                    -1,
                    outputDirectory.toString(),
                    executable.toString(),
                    String.join(File.pathSeparator, path),
                    COUNTER_MAIN,
                    String.valueOf(failure.getMessage())
            );
        }
    }

    /// Returns whether `classPath` names the Counter sample jar or classes.
    ///
    /// @param classPath the entries
    /// @return whether a Counter artifact is present
    public static boolean referencesCounter(List<String> classPath) {
        Objects.requireNonNull(classPath, "classPath");
        for (int index = 0; index < classPath.size(); index++) {
            String entry = classPath.get(index).replace('\\', '/').toLowerCase(Locale.ROOT);
            if (entry.contains("himari-samples-counter") || entry.contains("/samples/counter/")) {
                return true;
            }
        }
        return false;
    }

    /// Finds `native-image` under `GRAALVM_HOME` or `PATH`.
    ///
    /// @return the executable, or `null`
    public static @Nullable Path locate() {
        @Nullable String home = System.getenv("GRAALVM_HOME");
        if (home != null && !home.isBlank()) {
            @Nullable Path underHome = firstExisting(Path.of(home), nativeImageNames());
            if (underHome != null) {
                return underHome;
            }
        }
        @Nullable String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return null;
        }
        String separator = path.contains(";") ? ";" : ":";
        for (String entry : path.split(separator)) {
            if (entry.isBlank()) {
                continue;
            }
            @Nullable Path found = firstExisting(Path.of(entry), nativeImageNames());
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /// Candidate file names on this host.
    private static List<String> nativeImageNames() {
        if (windowsHost()) {
            return List.of("native-image.cmd", "native-image.exe", "bin/native-image.cmd", "bin/native-image.exe");
        }
        return List.of("native-image", "bin/native-image");
    }

    /// Returns whether this process is a Windows host.
    private static boolean windowsHost() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /// Returns the first existing candidate under `root`.
    private static @Nullable Path firstExisting(Path root, List<String> names) {
        for (int index = 0; index < names.size(); index++) {
            Path candidate = root.resolve(names.get(index));
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /// Observation of one Native Image probe.
    ///
    /// @param builtImage whether [`#imageFile(Path)`] exists after the invocation
    /// @param environmentBlocked whether `native-image` was absent
    /// @param exitCode the process exit, or `-1` when it did not start
    /// @param outputDirectory the requested image directory
    /// @param nativeImagePath the toolchain path, or `null`
    /// @param classPath the Counter classpath that was passed, empty when none
    /// @param mainClass the sample main class
    /// @param detail stdout/stderr or the block reason
    public record Result(
            boolean builtImage,
            boolean environmentBlocked,
            int exitCode,
            String outputDirectory,
            @Nullable String nativeImagePath,
            String classPath,
            String mainClass,
            String detail
    ) {
        /// Validates the observation.
        public Result {
            Objects.requireNonNull(outputDirectory, "outputDirectory");
            Objects.requireNonNull(classPath, "classPath");
            Objects.requireNonNull(mainClass, "mainClass");
            Objects.requireNonNull(detail, "detail");
            if (builtImage && environmentBlocked) {
                throw new IllegalArgumentException("A blocked probe cannot claim a built image");
            }
        }

        /// Encodes the observation as JSON.
        ///
        /// @return the document
        public String toJson() {
            String path = nativeImagePath == null ? "null" : "\"" + escape(nativeImagePath) + "\"";
            return """
                    {
                      "schema": "himari-native-image-probe-v1",
                      "builtImage": %s,
                      "environmentBlocked": %s,
                      "exitCode": %d,
                      "outputDirectory": "%s",
                      "nativeImagePath": %s,
                      "classPath": "%s",
                      "mainClass": "%s",
                      "referencedCounter": %s,
                      "detail": "%s"
                    }
                    """.formatted(
                    builtImage,
                    environmentBlocked,
                    exitCode,
                    escape(outputDirectory),
                    path,
                    escape(classPath),
                    escape(mainClass),
                    NativeImageProbe.referencesCounter(
                            classPath.isEmpty() ? List.of() : List.of(classPath.split(java.util.regex.Pattern.quote(File.pathSeparator)))
                    ),
                    escape(detail)
            );
        }

        /// Escapes one JSON string fragment.
        private static String escape(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
        }
    }
}
