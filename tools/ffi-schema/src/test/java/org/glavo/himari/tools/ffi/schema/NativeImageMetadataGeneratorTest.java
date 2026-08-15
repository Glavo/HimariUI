package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies deterministic Native Image reflection and foreign-call metadata generation.
@NotNullByDefault
final class NativeImageMetadataGeneratorTest {
    /// Verifies exact Java-layout names, aggregate padding, callback registrations, and error-state options.
    @Test
    void generatesExactReachabilityMetadata() {
        AbiSchema schema = fixedSignatureSchema();
        String metadata = NativeImageMetadataGenerator.generate(schema);

        assertTrue(metadata.contains("\"type\": \"org.glavo.himari.ffi.fixture.HimariFfiFixtureFfmBindings\""));
        assertTrue(metadata.contains("\"name\": \"invokeFixtureVisitCallback\""));
        assertTrue(metadata.contains("\"org.glavo.himari.ffi.fixture.HimariFfiFixtureFfmBindings$"
                + "FixtureVisitCallback\""));
        assertTrue(metadata.contains("\"returnType\": \"jint\""));
        assertTrue(metadata.contains("\"struct(jint,jint)\""));
        assertTrue(metadata.contains("\"captureCallState\": true"));
        assertTrue(metadata.contains("\"upcalls\": ["));
        assertTrue(metadata.contains("\"void*\""));
    }

    /// Verifies that equal schemas produce byte-identical files and create missing parent directories.
    ///
    /// @param temporaryDirectory the JUnit-managed output root
    @Test
    void writesDeterministicMetadata(@TempDir Path temporaryDirectory) throws IOException {
        AbiSchema schema = fixedSignatureSchema();
        Path first = NativeImageMetadataGenerator.generate(
                schema,
                temporaryDirectory.resolve("first/META-INF/native-image/reachability-metadata.json")
        );
        Path second = NativeImageMetadataGenerator.generate(
                schema,
                temporaryDirectory.resolve("second/META-INF/native-image/reachability-metadata.json")
        );

        assertEquals(Files.readString(first, StandardCharsets.UTF_8), Files.readString(second, StandardCharsets.UTF_8));
    }

    /// Verifies explicit structure padding and declared-size union members.
    @Test
    void preservesAggregatePhysicalLayout() {
        AbiSchema schema = fixedSignatureSchema();
        AbiSchema.AggregateType padded = new AbiSchema.AggregateType(
                "padded_pair",
                AbiSchema.AggregateKind.STRUCT,
                16L,
                8,
                0,
                List.of(
                        new AbiSchema.AggregateField("head", new AbiSchema.TypeRef("u8"), 0L, null, null),
                        new AbiSchema.AggregateField("tail", new AbiSchema.TypeRef("f64"), 8L, null, null)
                ),
                AbiSchema.Availability.UNRESTRICTED
        );
        AbiSchema.FunctionDefinition function = new AbiSchema.FunctionDefinition(
                "consume_padded_pair",
                "consume_padded_pair",
                new AbiSchema.ReturnValue(
                        new AbiSchema.TypeRef("void"),
                        AbiSchema.Nullability.UNSPECIFIED,
                        AbiSchema.Ownership.NONE
                ),
                List.of(new AbiSchema.Parameter(
                        "value",
                        new AbiSchema.TypeRef("padded_pair"),
                        AbiSchema.ParameterDirection.IN,
                        AbiSchema.Nullability.UNSPECIFIED,
                        AbiSchema.Ownership.NONE
                )),
                AbiSchema.CallingConvention.SYSTEM,
                null,
                AbiSchema.ErrorPolicy.NONE,
                AbiSchema.ThreadRestriction.ANY,
                AbiSchema.Availability.UNRESTRICTED
        );
        AbiSchema paddedSchema = new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                java.util.stream.Stream.concat(schema.types().stream(), java.util.stream.Stream.of(padded)).toList(),
                List.of(),
                List.of(function)
        );

        String metadata = NativeImageMetadataGenerator.generate(paddedSchema);
        assertTrue(metadata.contains("\"struct(jbyte,padding(7),jdouble)\""), metadata);
    }

    /// Verifies that variadic functions cannot receive misleading fixed-call metadata.
    @Test
    void rejectsVariadicFunction() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> NativeImageMetadataGenerator.generate(fixtureSchema())
        );
        assertTrue(exception.getMessage().contains("variadic functions require explicit generated variants"));
    }

    /// Returns a fixed-signature subset of the canonical fixture.
    ///
    /// @return a schema accepted by both Java and metadata generators
    private static AbiSchema fixedSignatureSchema() {
        AbiSchema schema = fixtureSchema();
        return new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                schema.types(),
                schema.callbacks(),
                schema.functions().stream()
                        .filter(function -> !function.name().equals("fixture_format"))
                        .toList()
        );
    }

    /// Reads the canonical minimum FFM fixture.
    ///
    /// @return the parsed fixture schema
    private static AbiSchema fixtureSchema() {
        try (InputStream input = Objects.requireNonNull(
                NativeImageMetadataGeneratorTest.class.getResourceAsStream("/ffi-minimum-schema-v1.json"),
                "Missing ffi-minimum-schema-v1.json"
        )) {
            return AbiSchemaCodec.read(
                    "ffi-minimum-schema-v1",
                    new String(input.readAllBytes(), StandardCharsets.UTF_8)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read ffi-minimum-schema-v1.json", exception);
        }
    }
}
