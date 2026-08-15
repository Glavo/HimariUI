package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/// Emits deterministic evidence for the `ffi-minimum-schema-v1` conformance fixture.
@NotNullByDefault
public final class FfiSchemaConformance {
    /// Prevents instantiation of this command-line utility.
    private FfiSchemaConformance() {
    }

    /// Validates the input fixture and writes canonical schema and report artifacts.
    ///
    /// @param arguments the input fixture path followed by the output directory
    /// @throws IllegalArgumentException if the arguments or input schema are invalid
    /// @throws IllegalStateException if deterministic round-trip verification or artifact writing fails
    public static void main(String[] arguments) {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected: <fixture.json> <output-directory>");
        }

        Path fixture = Path.of(arguments[0]);
        Path outputDirectory = Path.of(arguments[1]);
        AbiSchema schema = AbiSchemaCodec.read(fixture);
        String canonical = AbiSchemaCodec.write(schema);
        AbiSchema roundTripped = AbiSchemaCodec.read("canonical ffi-minimum-schema-v1", canonical);
        if (!schema.equals(roundTripped)) {
            throw new IllegalStateException("Canonical ABI schema round-trip changed the model");
        }
        if (!canonical.equals(AbiSchemaCodec.write(roundTripped))) {
            throw new IllegalStateException("Canonical ABI schema output is not deterministic");
        }

        write(outputDirectory.resolve("schema.json"), canonical);
        write(outputDirectory.resolve("report.json"), report(schema, canonical));
    }

    /// Creates the deterministic fixture report.
    ///
    /// @param schema the validated fixture model
    /// @param canonical the canonical schema JSON
    /// @return canonical report JSON
    private static String report(AbiSchema schema, String canonical) {
        return """
                {
                  "profileId": "m0-ffi-schema",
                  "profileVersion": 1,
                  "fixture": "ffi-minimum-schema-v1",
                  "schemaVersion": %d,
                  "schemaSha256": "%s",
                  "typeCount": %d,
                  "callbackCount": %d,
                  "functionCount": %d,
                  "assertions": [
                    "strict-round-trip",
                    "deterministic-canonical-json",
                    "semantic-reference-and-layout-validation",
                    "fixed-callback-containment-contract"
                  ],
                  "result": "passed"
                }
                """.formatted(
                schema.schemaVersion(),
                sha256(canonical),
                schema.types().size(),
                schema.callbacks().size(),
                schema.functions().size()
        );
    }

    /// Computes a lower-case SHA-256 digest for UTF-8 text.
    ///
    /// @param value the text to hash
    /// @return the hexadecimal digest
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("The Java runtime does not provide SHA-256", exception);
        }
    }

    /// Writes a UTF-8 artifact and creates its parent directory.
    ///
    /// @param path the output path
    /// @param content the complete file contents
    private static void write(Path path, String content) {
        try {
            @Nullable Path parent = path.getParent();
            if (parent == null) {
                throw new IllegalArgumentException("Output path must have a parent directory: " + path);
            }
            Files.createDirectories(parent);
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write conformance artifact " + path, exception);
        }
    }
}
