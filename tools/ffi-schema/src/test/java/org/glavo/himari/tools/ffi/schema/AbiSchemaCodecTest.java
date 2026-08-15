package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies strict parsing and deterministic canonical serialization.
@NotNullByDefault
final class AbiSchemaCodecTest {
    /// Verifies that the published tool resources contain a parseable canonical schema definition.
    @Test
    void packagesSchemaDefinition() {
        StrictJson.JsonObject definition = StrictJson.parseObject(
                "schema/ffi-schema.schema.json",
                resourceText("/schema/ffi-schema.schema.json")
        );
        StrictJson.JsonString dialect = assertInstanceOf(
                StrictJson.JsonString.class,
                definition.members().get("$schema")
        );

        assertEquals("https://json-schema.org/draft/2020-12/schema", dialect.value());
    }

    /// Verifies that the minimum fixture survives a lossless canonical round trip.
    @Test
    void roundTripsMinimumFixture() {
        AbiSchema schema = AbiSchemaCodec.read("ffi-minimum-schema-v1", fixtureJson());
        String canonical = AbiSchemaCodec.write(schema);
        AbiSchema decoded = AbiSchemaCodec.read("canonical", canonical);

        assertEquals(schema, decoded);
        assertEquals(canonical, AbiSchemaCodec.write(decoded));
        assertTrue(canonical.endsWith("\n"));
    }

    /// Verifies that declaration input order cannot change canonical output.
    @Test
    void canonicalizesDeclarationOrder() {
        AbiSchema schema = AbiSchemaCodec.read("ffi-minimum-schema-v1", fixtureJson());
        List<AbiSchema.TypeDefinition> types = new ArrayList<>(schema.types());
        List<AbiSchema.CallbackDefinition> callbacks = new ArrayList<>(schema.callbacks());
        List<AbiSchema.FunctionDefinition> functions = new ArrayList<>(schema.functions());
        Collections.reverse(types);
        Collections.reverse(callbacks);
        Collections.reverse(functions);

        AbiSchema reordered = new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                types,
                callbacks,
                functions
        );
        assertEquals(AbiSchemaCodec.write(schema), AbiSchemaCodec.write(reordered));
    }

    /// Verifies that unknown members cannot silently alter the schema contract.
    @Test
    void rejectsUnknownMembers() {
        String malformed = fixtureJson().replace(
                "  \"schemaVersion\": 1,",
                "  \"schemaVersion\": 1,\n  \"unexpected\": true,"
        );
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AbiSchemaCodec.read("unknown-member", malformed)
        );
        assertTrue(exception.getMessage().contains("unexpected"));
    }

    /// Verifies that duplicate JSON keys fail before model construction.
    @Test
    void rejectsDuplicateJsonKeys() {
        String malformed = fixtureJson().replace(
                "  \"schemaVersion\": 1,",
                "  \"schemaVersion\": 1,\n  \"schemaVersion\": 1,"
        );
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AbiSchemaCodec.read("duplicate-key", malformed)
        );
        assertTrue(exception.getMessage().contains("Duplicate object member"));
    }

    /// Verifies that unsupported callback failure policies cannot enter the model.
    @Test
    void rejectsUnsupportedCallbackPolicy() {
        String malformed = fixtureJson().replace(
                "\"exceptionPolicy\": \"contain\"",
                "\"exceptionPolicy\": \"propagate\""
        );
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AbiSchemaCodec.read("callback-policy", malformed)
        );
        assertTrue(exception.getMessage().contains("unsupported value 'propagate'"));
    }

    /// Reads the minimum fixture from the test runtime class path.
    ///
    /// @return the UTF-8 fixture JSON
    private static String fixtureJson() {
        return resourceText("/ffi-minimum-schema-v1.json");
    }

    /// Reads one UTF-8 resource from the test runtime class path.
    ///
    /// @param name the absolute resource name
    /// @return the complete resource text
    private static String resourceText(String name) {
        try (InputStream input = Objects.requireNonNull(
                AbiSchemaCodecTest.class.getResourceAsStream(name),
                "Missing resource " + name
        )) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read resource " + name, exception);
        }
    }
}
