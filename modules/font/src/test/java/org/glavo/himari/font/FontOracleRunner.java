package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/// Launches isolated FreeType and HarfBuzz oracle processes when they exist on this host.
///
/// Production font and text modules never link these binaries. A missing executable is reported
/// as `environment-blocked` rather than a failed comparison.
@NotNullByDefault
final class FontOracleRunner {
    /// Environment variable naming the FreeType outline oracle executable.
    static final String FREETYPE_ENV = "HIMARI_FREETYPE_ORACLE";

    /// Environment variable naming the HarfBuzz shape oracle executable.
    static final String HARFBUZZ_ENV = "HIMARI_HARFBUZZ_ORACLE";

    /// Prevents instantiation.
    private FontOracleRunner() {
    }

    /// Probes the FreeType outline oracle.
    ///
    /// @param repositoryRoot the repository root
    /// @return the probe result
    static OracleProbe probeFreeType(Path repositoryRoot) {
        return probe(repositoryRoot, FREETYPE_ENV, "oracles/freetype", "outline-oracle", "outline_oracle");
    }

    /// Probes the HarfBuzz shape oracle.
    ///
    /// @param repositoryRoot the repository root
    /// @return the probe result
    static OracleProbe probeHarfBuzz(Path repositoryRoot) {
        return probe(repositoryRoot, HARFBUZZ_ENV, "oracles/harfbuzz", "shape-oracle", "shape_oracle");
    }

    /// Runs the FreeType oracle on `font` glyph `glyphId` when available.
    ///
    /// @param repositoryRoot the repository root
    /// @param fontFile the font file
    /// @param glyphId the glyph identity
    /// @return the process JSON, or a blocked probe
    static OracleProbe runFreeType(Path repositoryRoot, Path fontFile, int glyphId) {
        OracleProbe probe = probeFreeType(repositoryRoot);
        if (!"resolved".equals(probe.status()) || probe.executable() == null) {
            return probe;
        }
        return exec(probe.executable(), List.of(fontFile.toString(), Integer.toString(glyphId)));
    }

    /// Runs the HarfBuzz oracle on `font` and `text` when available.
    ///
    /// @param repositoryRoot the repository root
    /// @param fontFile the font file
    /// @param text the source text
    /// @return the process JSON, or a blocked probe
    static OracleProbe runHarfBuzz(Path repositoryRoot, Path fontFile, String text) {
        OracleProbe probe = probeHarfBuzz(repositoryRoot);
        if (!"resolved".equals(probe.status()) || probe.executable() == null) {
            return probe;
        }
        return exec(probe.executable(), List.of(fontFile.toString(), text));
    }

    /// Locates an oracle executable without running it.
    private static OracleProbe probe(
            Path repositoryRoot,
            String envName,
            String directory,
            String hyphenName,
            String underscoreName
    ) {
        @Nullable String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            Path path = Path.of(env);
            if (Files.isRegularFile(path)) {
                return new OracleProbe("resolved", path, "");
            }
            return new OracleProbe(
                    "environment-blocked",
                    null,
                    envName + " is set but " + path + " is not a file"
            );
        }
        Path folder = repositoryRoot.resolve(directory);
        Path[] candidates = {
                folder.resolve(hyphenName + ".exe"),
                folder.resolve(hyphenName),
                folder.resolve(underscoreName + ".exe"),
                folder.resolve(underscoreName)
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return new OracleProbe("resolved", candidate, "");
            }
        }
        return new OracleProbe(
                "environment-blocked",
                null,
                "no " + hyphenName + " executable under " + folder + " and " + envName + " is unset"
        );
    }

    /// Executes `executable` with `arguments`.
    private static OracleProbe exec(Path executable, List<String> arguments) {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command().add(executable.toString());
        builder.command().addAll(arguments);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new OracleProbe("environment-blocked", executable, "oracle timed out");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                return new OracleProbe("environment-blocked", executable, "oracle exit " + process.exitValue() + ": " + output);
            }
            return new OracleProbe("compared", executable, output);
        } catch (IOException exception) {
            return new OracleProbe("environment-blocked", executable, exception.toString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new OracleProbe("environment-blocked", executable, exception.toString());
        }
    }

    /// Records one oracle probe.
    ///
    /// @param status `resolved`, `compared`, or `environment-blocked`
    /// @param executable the resolved file, or `null`
    /// @param detail the block reason or process output
    record OracleProbe(String status, @Nullable Path executable, String detail) {
        /// Validates the probe.
        OracleProbe {
            if (!"resolved".equals(status) && !"compared".equals(status) && !"environment-blocked".equals(status)) {
                throw new IllegalArgumentException("Oracle status must be resolved, compared, or environment-blocked");
            }
        }
    }
}
