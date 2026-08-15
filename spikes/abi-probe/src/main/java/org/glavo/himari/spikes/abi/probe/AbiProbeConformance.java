package org.glavo.himari.spikes.abi.probe;

import org.glavo.himari.tools.ffi.schema.AbiProbe;
import org.glavo.himari.tools.ffi.schema.AbiProbeCodec;
import org.glavo.himari.tools.ffi.schema.AbiSchema;
import org.glavo.himari.tools.ffi.schema.AbiSchemaCodec;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/// Compiles the native C probe and writes deterministic ABI comparison evidence.
@NotNullByDefault
public final class AbiProbeConformance {
    /// Maximum time allowed for one native probe execution.
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(30);

    /// Prevents instantiation of this command-line utility.
    private AbiProbeConformance() {
    }

    /// Compiles, executes twice, and compares one ABI probe fixture.
    ///
    /// @param arguments the C source path, canonical ABI schema path, and evidence directory
    /// @throws IllegalArgumentException if the arguments or input documents are invalid
    /// @throws IllegalStateException if compilation, execution, determinism, comparison, or evidence writing fails
    public static void main(String[] arguments) {
        if (arguments.length != 3) {
            throw new IllegalArgumentException("Expected: <probe.c> <abi-schema.json> <evidence-directory>");
        }
        Path source = Path.of(arguments[0]).toAbsolutePath().normalize();
        Path schemaPath = Path.of(arguments[1]).toAbsolutePath().normalize();
        Path evidenceDirectory = Path.of(arguments[2]).toAbsolutePath().normalize();
        createDirectories(evidenceDirectory);

        Path nativeDirectory = evidenceDirectory.resolve(".native");
        CProbeCompiler.Result compilation = CProbeCompiler.compile(source, nativeDirectory);
        byte[] first = runProbe(compilation.executable(), nativeDirectory.resolve("probe-first.json"));
        byte[] second = runProbe(compilation.executable(), nativeDirectory.resolve("probe-second.json"));
        if (!java.util.Arrays.equals(first, second)) {
            throw new IllegalStateException("Native ABI probe output is not byte-for-byte deterministic");
        }

        AbiProbe probe = AbiProbeCodec.read("native ABI probe", new String(first, StandardCharsets.UTF_8));
        String canonicalProbe = AbiProbeCodec.write(probe);
        Path probePath = evidenceDirectory.resolve("probe.json");
        write(probePath, canonicalProbe);

        AbiSchema schema = AbiSchemaCodec.read(schemaPath);
        AbiProbeVerifier.Comparison comparison = AbiProbeVerifier.compare(
                schema,
                probe,
                AbiProbeVerifier.hostTarget()
        );
        String comparisonJson = comparisonJson(
                probe,
                comparison,
                sha256(schemaPath),
                sha256(canonicalProbe.getBytes(StandardCharsets.UTF_8)),
                sha256(source)
        );
        Path comparisonPath = evidenceDirectory.resolve("comparison.json");
        write(comparisonPath, comparisonJson);
        if (!comparison.passed()) {
            throw new IllegalStateException(
                    "Native ABI probe comparison failed:" + System.lineSeparator()
                            + String.join(System.lineSeparator(), comparison.mismatches())
            );
        }
    }

    /// Runs the compiled probe with redirected output and a strict timeout.
    ///
    /// @param executable the compiled probe executable
    /// @param rawOutputPath the private raw-output path
    /// @return the exact standard-output bytes
    private static byte[] runProbe(Path executable, Path rawOutputPath) {
        Path errorPath = rawOutputPath.resolveSibling(rawOutputPath.getFileName() + ".stderr");
        ProcessBuilder builder = new ProcessBuilder(executable.toString())
                .directory(executable.getParent().toFile())
                .redirectOutput(rawOutputPath.toFile())
                .redirectError(errorPath.toFile());
        try {
            Process process = builder.start();
            if (!process.waitFor(PROBE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Native ABI probe timed out: " + executable);
            }
            if (process.exitValue() != 0) {
                String error = Files.readString(errorPath, StandardCharsets.UTF_8);
                throw new IllegalStateException(
                        "Native ABI probe failed with exit code " + process.exitValue() + ": " + error
                );
            }
            return Files.readAllBytes(rawOutputPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot execute native ABI probe " + executable, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while executing native ABI probe", exception);
        }
    }

    /// Encodes the deterministic comparison evidence document.
    ///
    /// @param probe the decoded native probe
    /// @param comparison the complete comparison result
    /// @param schemaHash the canonical ABI schema hash
    /// @param probeHash the canonical probe document hash
    /// @param sourceHash the compiled C source hash
    /// @return the complete comparison JSON
    private static String comparisonJson(
            AbiProbe probe,
            AbiProbeVerifier.Comparison comparison,
            String schemaHash,
            String probeHash,
            String sourceHash
    ) {
        StringBuilder output = new StringBuilder(2048);
        output.append("{\n");
        output.append("  \"profileId\": \"m0-abi-probe\",\n");
        output.append("  \"profileVersion\": 1,\n");
        output.append("  \"protocolVersion\": ").append(probe.protocolVersion()).append(",\n");
        output.append("  \"fixtures\": [");
        for (int index = 0; index < probe.fixtures().size(); index++) {
            if (index > 0) {
                output.append(", ");
            }
            appendQuoted(output, probe.fixtures().get(index));
        }
        output.append("],\n");
        output.append("  \"target\": {\n");
        quotedMember(output, 4, "operatingSystem", probe.target().operatingSystem(), true);
        quotedMember(output, 4, "architecture", probe.target().architecture(), true);
        quotedMember(
                output,
                4,
                "byteOrder",
                probe.target().byteOrder().name().toLowerCase(Locale.ROOT),
                false
        );
        output.append("  },\n");
        output.append("  \"compiler\": {\n");
        quotedMember(output, 4, "family", probe.compiler().family(), true);
        numberMember(output, 4, "major", probe.compiler().major(), true);
        numberMember(output, 4, "minor", probe.compiler().minor(), true);
        numberMember(output, 4, "patch", probe.compiler().patch(), false);
        output.append("  },\n");
        quotedMember(output, 2, "schemaSha256", schemaHash, true);
        quotedMember(output, 2, "probeSha256", probeHash, true);
        quotedMember(output, 2, "probeSourceSha256", sourceHash, true);
        output.append("  \"verified\": {\n");
        numberMember(output, 4, "typeLayouts", comparison.typeLayouts(), true);
        numberMember(output, 4, "aggregateLayouts", comparison.aggregateLayouts(), true);
        numberMember(output, 4, "fieldLayouts", comparison.fieldLayouts(), true);
        numberMember(output, 4, "callbacks", comparison.callbacks(), true);
        numberMember(output, 4, "functionalChecks", comparison.functionalChecks(), false);
        output.append("  },\n");
        output.append("  \"assertions\": {\n");
        output.append("    \"rawProbeDeterministic\": true,\n");
        output.append("    \"targetMatchesJavaProcess\": ").append(comparison.passed()).append(",\n");
        output.append("    \"schemaMatchesNativeProbe\": ").append(comparison.passed()).append(",\n");
        output.append("    \"generatedLayoutsMatchNativeProbe\": ").append(comparison.passed()).append(",\n");
        output.append("    \"callbackConventionAndInvocationMatch\": ").append(comparison.passed()).append(",\n");
        output.append("    \"structureReturnAndVariadicCallMatch\": ").append(comparison.passed()).append(",\n");
        output.append("    \"probeExecutablePackaged\": false\n");
        output.append("  },\n");
        output.append("  \"mismatches\": [");
        List<String> mismatches = comparison.mismatches();
        if (!mismatches.isEmpty()) {
            output.append('\n');
            for (int index = 0; index < mismatches.size(); index++) {
                output.append("    ");
                appendQuoted(output, mismatches.get(index));
                output.append(index + 1 < mismatches.size() ? ",\n" : "\n");
            }
            output.append("  ");
        }
        output.append("],\n");
        quotedMember(output, 2, "result", comparison.passed() ? "passed" : "failed", false);
        output.append("}\n");
        return output.toString();
    }

    /// Creates a directory hierarchy.
    ///
    /// @param directory the directory to create
    private static void createDirectories(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create ABI evidence directory " + directory, exception);
        }
    }

    /// Writes one UTF-8 evidence document.
    ///
    /// @param path the destination path
    /// @param content the complete document
    private static void write(Path path, String content) {
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write ABI evidence " + path, exception);
        }
    }

    /// Computes a lowercase SHA-256 digest for one file.
    ///
    /// @param path the source file
    /// @return the hexadecimal digest
    private static String sha256(Path path) {
        try {
            return sha256(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot hash " + path, exception);
        }
    }

    /// Computes a lowercase SHA-256 digest for one byte sequence.
    ///
    /// @param content the source bytes
    /// @return the hexadecimal digest
    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by Java", exception);
        }
    }

    /// Appends one JSON number member.
    ///
    /// @param output the output buffer
    /// @param indentation the indentation width
    /// @param name the member name
    /// @param value the number value
    /// @param comma whether to append a trailing comma
    private static void numberMember(
            StringBuilder output,
            int indentation,
            String name,
            long value,
            boolean comma
    ) {
        output.append(" ".repeat(indentation));
        appendQuoted(output, name);
        output.append(": ").append(value);
        if (comma) {
            output.append(',');
        }
        output.append('\n');
    }

    /// Appends one JSON string member.
    ///
    /// @param output the output buffer
    /// @param indentation the indentation width
    /// @param name the member name
    /// @param value the decoded string value
    /// @param comma whether to append a trailing comma
    private static void quotedMember(
            StringBuilder output,
            int indentation,
            String name,
            String value,
            boolean comma
    ) {
        output.append(" ".repeat(indentation));
        appendQuoted(output, name);
        output.append(": ");
        appendQuoted(output, value);
        if (comma) {
            output.append(',');
        }
        output.append('\n');
    }

    /// Appends a quoted JSON string.
    ///
    /// @param output the output buffer
    /// @param value the decoded value
    private static void appendQuoted(StringBuilder output, String value) {
        Objects.requireNonNull(value, "value");
        output.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20) {
                        output.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        output.append('"');
    }
}
