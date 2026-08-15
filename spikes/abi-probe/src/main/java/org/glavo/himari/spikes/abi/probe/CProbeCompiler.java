package org.glavo.himari.spikes.abi.probe;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/// Selects a host C compiler and builds the non-published ABI probe executable.
@NotNullByDefault
final class CProbeCompiler {
    /// Maximum time allowed for one compiler process.
    private static final Duration COMPILER_TIMEOUT = Duration.ofMinutes(2);

    /// Prevents instantiation of this utility class.
    private CProbeCompiler() {
    }

    /// Compiles one C17 source file into an executable under `outputDirectory`.
    ///
    /// `HIMARI_CC` may select an executable explicitly. `HIMARI_CC_DRIVER` may select `zig`, `gnu`, or `msvc`;
    /// otherwise the driver is inferred from the executable name. Zig caches are redirected below `outputDirectory`.
    ///
    /// @param source the C source file
    /// @param outputDirectory the private build-output directory
    /// @return the compiled executable and selected compiler command
    /// @throws IllegalStateException if no compiler is available or compilation fails
    static Result compile(Path source, Path outputDirectory) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("C probe source does not exist: " + source);
        }

        Selection selection = selectCompiler();
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create ABI probe build directory " + outputDirectory, exception);
        }
        String executableName = isWindows() ? "abi-probe.exe" : "abi-probe";
        Path executable = outputDirectory.resolve(executableName).toAbsolutePath().normalize();
        Path object = outputDirectory.resolve("abi-probe.obj").toAbsolutePath().normalize();
        List<String> command = compilerCommand(selection, source.toAbsolutePath().normalize(), executable, object);
        Path log = outputDirectory.resolve("compiler.log");
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(outputDirectory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        if (selection.driver() == Driver.ZIG) {
            Map<String, String> environment = builder.environment();
            environment.put("ZIG_GLOBAL_CACHE_DIR", zigGlobalCache(outputDirectory).toString());
            environment.put("ZIG_LOCAL_CACHE_DIR", outputDirectory.resolve("zig-local-cache").toString());
        }
        runCompiler(builder, log, command);
        if (!Files.isRegularFile(executable)) {
            throw new IllegalStateException("C compiler reported success without producing " + executable);
        }
        return new Result(executable, command);
    }

    /// Selects an explicit or discoverable compiler executable and command-line driver.
    ///
    /// @return the selected compiler
    private static Selection selectCompiler() {
        @Nullable String explicit = nonBlank(System.getenv("HIMARI_CC"));
        @Nullable String explicitDriver = nonBlank(System.getenv("HIMARI_CC_DRIVER"));
        if (explicit != null) {
            Path executable = resolveExecutable(explicit);
            Driver driver = explicitDriver == null
                    ? inferDriver(executable)
                    : parseDriver(explicitDriver);
            return new Selection(executable, driver);
        }
        if (explicitDriver != null) {
            throw new IllegalStateException("HIMARI_CC_DRIVER requires HIMARI_CC");
        }

        List<Candidate> candidates = isWindows()
                ? List.of(
                        new Candidate("zig", Driver.ZIG),
                        new Candidate("clang", Driver.GNU),
                        new Candidate("gcc", Driver.GNU),
                        new Candidate("cl", Driver.MSVC)
                )
                : List.of(
                        new Candidate("cc", Driver.GNU),
                        new Candidate("clang", Driver.GNU),
                        new Candidate("gcc", Driver.GNU),
                        new Candidate("zig", Driver.ZIG)
                );
        for (Candidate candidate : candidates) {
            @Nullable Path executable = findOnPath(candidate.command());
            if (executable != null) {
                return new Selection(executable, candidate.driver());
            }
        }
        throw new IllegalStateException(
                "No C compiler found. Set HIMARI_CC and optionally HIMARI_CC_DRIVER=zig|gnu|msvc"
        );
    }

    /// Builds the compiler command for one supported driver family.
    ///
    /// @param selection the selected compiler
    /// @param source the absolute source path
    /// @param executable the absolute output executable path
    /// @param object the absolute MSVC object path
    /// @return the immutable command arguments
    private static @Unmodifiable List<String> compilerCommand(
            Selection selection,
            Path source,
            Path executable,
            Path object
    ) {
        List<String> command = new ArrayList<>();
        command.add(selection.executable().toString());
        switch (selection.driver()) {
            case ZIG -> command.add("cc");
            case GNU -> {
            }
            case MSVC -> {
                command.add("/nologo");
                command.add("/std:c17");
                command.add("/W4");
                command.add("/WX");
                command.add("/O2");
                command.add("/Fo:" + object);
                command.add("/Fe:" + executable);
                command.add(source.toString());
                return List.copyOf(command);
            }
        }
        command.add("-std=c17");
        command.add("-O2");
        command.add("-Wall");
        command.add("-Wextra");
        command.add("-Werror");
        command.add("-pedantic-errors");
        command.add("-o");
        command.add(executable.toString());
        command.add(source.toString());
        return List.copyOf(command);
    }

    /// Runs one compiler process and reports its complete output after failure.
    ///
    /// @param builder the configured compiler process
    /// @param log the redirected output log
    /// @param command the stable command used in diagnostics
    private static void runCompiler(ProcessBuilder builder, Path log, List<String> command) {
        try {
            Process process = builder.start();
            if (!process.waitFor(COMPILER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("C compiler timed out: " + String.join(" ", command));
            }
            if (process.exitValue() != 0) {
                String output = Files.readString(log, StandardCharsets.UTF_8);
                throw new IllegalStateException(
                        "C compiler failed with exit code " + process.exitValue() + ": "
                                + String.join(" ", command) + System.lineSeparator() + output
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot execute C compiler: " + String.join(" ", command), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while compiling the C ABI probe", exception);
        }
    }

    /// Resolves an explicit compiler path or command name.
    ///
    /// @param value the configured path or command
    /// @return the resolved executable
    private static Path resolveExecutable(String value) {
        Path candidate = Path.of(value);
        if (candidate.getNameCount() > 1 || candidate.isAbsolute()) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (!Files.isRegularFile(absolute)) {
                throw new IllegalStateException("HIMARI_CC is not a file: " + absolute);
            }
            return absolute;
        }
        @Nullable Path executable = findOnPath(value);
        if (executable == null) {
            throw new IllegalStateException("HIMARI_CC is not available on PATH: " + value);
        }
        return executable;
    }

    /// Finds one command on the process search path without invoking a shell.
    ///
    /// @param command the command basename
    /// @return the absolute executable path, or `null` when absent
    private static @Nullable Path findOnPath(String command) {
        @Nullable String searchPath = System.getenv("PATH");
        if (searchPath == null || searchPath.isBlank()) {
            return null;
        }
        List<String> suffixes = executableSuffixes(command);
        for (String directory : searchPath.split(Patterns.PATH_SEPARATOR, -1)) {
            if (directory.isBlank()) {
                continue;
            }
            for (String suffix : suffixes) {
                Path candidate = Path.of(directory, command + suffix).toAbsolutePath().normalize();
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /// Returns platform executable suffixes for one command name.
    ///
    /// @param command the command name
    /// @return suffixes in search order
    private static @Unmodifiable List<String> executableSuffixes(String command) {
        if (!isWindows() || command.contains(".")) {
            return List.of("");
        }
        @Nullable String pathExtensions = System.getenv("PATHEXT");
        if (pathExtensions == null || pathExtensions.isBlank()) {
            return List.of(".exe", ".cmd", ".bat", "");
        }
        List<String> suffixes = new ArrayList<>();
        for (String suffix : pathExtensions.split(";")) {
            if (!suffix.isBlank()) {
                suffixes.add(suffix.toLowerCase(Locale.ROOT));
            }
        }
        suffixes.add("");
        return List.copyOf(suffixes);
    }

    /// Infers the driver syntax from a compiler executable basename.
    ///
    /// @param executable the compiler executable
    /// @return the inferred driver
    private static Driver inferDriver(Path executable) {
        String name = executable.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.startsWith("zig")) {
            return Driver.ZIG;
        }
        if (name.equals("cl") || name.equals("cl.exe")) {
            return Driver.MSVC;
        }
        return Driver.GNU;
    }

    /// Parses an explicit driver spelling.
    ///
    /// @param value the configured spelling
    /// @return the selected driver
    private static Driver parseDriver(String value) {
        try {
            return Driver.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Unknown HIMARI_CC_DRIVER: " + value, exception);
        }
    }

    /// Returns a trimmed non-empty string or `null`.
    ///
    /// @param value the candidate value
    /// @return the trimmed value or `null`
    private static @Nullable String nonBlank(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /// Returns the workspace-local Zig global cache when the launcher supplied a workspace root.
    ///
    /// @param outputDirectory the fallback private build directory
    /// @return the cache directory
    private static Path zigGlobalCache(Path outputDirectory) {
        @Nullable String workspace = nonBlank(System.getProperty("himari.workspace"));
        if (workspace == null) {
            return outputDirectory.resolve("zig-global-cache");
        }
        return Path.of(workspace).toAbsolutePath().normalize()
                .resolve(".gradle-user-home")
                .resolve("zig-global-cache");
    }

    /// Returns whether the current Java runtime is running on Windows.
    ///
    /// @return whether Windows executable conventions apply
    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
    }

    /// Describes a completed probe compilation.
    ///
    /// @param executable the compiled executable path
    /// @param command the exact compiler command
    @NotNullByDefault
    record Result(Path executable, @Unmodifiable List<String> command) {
        /// Creates an immutable compilation result.
        Result {
            Objects.requireNonNull(executable, "executable");
            command = List.copyOf(command);
        }
    }

    /// Describes one compiler candidate name and driver family.
    ///
    /// @param command the executable basename
    /// @param driver the command-line convention
    @NotNullByDefault
    private record Candidate(String command, Driver driver) {
    }

    /// Describes a resolved compiler executable and driver family.
    ///
    /// @param executable the absolute executable path
    /// @param driver the command-line convention
    @NotNullByDefault
    private record Selection(Path executable, Driver driver) {
    }

    /// Selects a supported compiler command-line convention.
    @NotNullByDefault
    private enum Driver {
        /// `zig cc` command-line syntax.
        ZIG,

        /// GCC/Clang-compatible command-line syntax.
        GNU,

        /// Microsoft C/C++ compiler command-line syntax.
        MSVC
    }

    /// Holds regex literals that otherwise obscure path-search code.
    @NotNullByDefault
    private static final class Patterns {
        /// A quoted platform path separator suitable for [String#split(String, int)].
        private static final String PATH_SEPARATOR = java.util.regex.Pattern.quote(File.pathSeparator);

        /// Prevents instantiation of this constants holder.
        private Patterns() {
        }
    }
}
