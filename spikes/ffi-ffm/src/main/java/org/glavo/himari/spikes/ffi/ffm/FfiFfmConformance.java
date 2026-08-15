package org.glavo.himari.spikes.ffi.ffm;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/// Writes machine-readable evidence for the M0 fixed-signature FFM profile.
@NotNullByDefault
public final class FfiFfmConformance {
    /// Prevents instantiation of this command-line utility.
    private FfiFfmConformance() {
    }

    /// Runs the requested repetitions and soak duration and writes `results.json`.
    ///
    /// @param arguments the result path, minimum repetition count, and minimum soak seconds
    /// @throws IllegalArgumentException if the arguments are invalid
    /// @throws IllegalStateException if execution or evidence writing fails
    public static void main(String[] arguments) {
        if (arguments.length != 3) {
            throw new IllegalArgumentException("Expected: <results.json> <minimum-repetitions> <soak-seconds>");
        }
        Path resultPath = Path.of(arguments[0]);
        int repetitions = parseNonNegative(arguments[1], "minimum-repetitions");
        int soakSeconds = parseNonNegative(arguments[2], "soak-seconds");
        Instant started = Instant.now();
        FfiFfmScenarios.Summary summary = FfiFfmScenarios.run(
                repetitions,
                Duration.ofSeconds(soakSeconds)
        );
        Instant finished = Instant.now();
        write(resultPath, report(started, finished, summary));
    }

    /// Parses one non-negative command-line integer.
    ///
    /// @param value the source text
    /// @param name the argument name
    /// @return the parsed value
    /// @throws IllegalArgumentException if the value is not a non-negative integer
    private static int parseNonNegative(String value, String name) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(name + " must be non-negative");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a signed 32-bit integer", exception);
        }
    }

    /// Creates the complete conformance result document.
    ///
    /// @param started the execution start time
    /// @param finished the execution completion time
    /// @param summary the verified scenario summary
    /// @return the formatted JSON document
    private static String report(
            Instant started,
            Instant finished,
            FfiFfmScenarios.Summary summary
    ) {
        return """
                {
                  "profileId": "m0-ffi-ffm",
                  "profileVersion": 1,
                  "fixtures": [
                    "ffi-primitives-v1",
                    "ffi-struct-by-value-v1",
                    "ffi-callback-reentrant-v1"
                  ],
                  "environment": {
                    "osName": %s,
                    "osArchitecture": %s,
                    "javaRuntime": %s,
                    "systemLibrary": %s
                  },
                  "startedAt": %s,
                  "finishedAt": %s,
                  "durationNanos": %d,
                  "repetitions": %d,
                  "callbackInvocations": %d,
                  "reentrantDowncalls": %d,
                  "assertions": {
                    "exactPrimitiveAndPointerCalls": true,
                    "structureReturnByValue": true,
                    "threadConfinedCallbacks": %s,
                    "callbackArenaLifetimeRejected": %s,
                    "callbackExceptionContained": %s,
                    "projectNativeShimLoaded": false
                  },
                  "result": "passed"
                }
                """.formatted(
                quote(System.getProperty("os.name", "")),
                quote(System.getProperty("os.arch", "")),
                quote(Runtime.version().toString()),
                quote(summary.systemLibrary()),
                quote(started.toString()),
                quote(finished.toString()),
                summary.durationNanos(),
                summary.repetitions(),
                summary.callbackInvocations(),
                summary.reentrantDowncalls(),
                summary.threadConfined(),
                summary.callbackArenaLifetimeRejected(),
                summary.exceptionContained()
        );
    }

    /// Returns a JSON string literal.
    ///
    /// @param value the raw value
    /// @return the quoted JSON text
    private static String quote(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) {
                        result.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }

    /// Writes the UTF-8 result document and creates its parent directory.
    ///
    /// @param path the result path
    /// @param content the complete JSON document
    private static void write(Path path, String content) {
        try {
            @Nullable Path parent = path.getParent();
            if (parent == null) {
                throw new IllegalArgumentException("Result path must have a parent directory: " + path);
            }
            Files.createDirectories(parent);
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write FFM conformance results " + path, exception);
        }
    }
}
