package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies semantic failures required by the minimum FFI schema profile.
@NotNullByDefault
final class AbiSchemaValidatorTest {
    /// Verifies that every schema contains a type vocabulary.
    @Test
    void rejectsEmptyTypeList() {
        AbiSchema schema = fixtureSchema();
        AbiSchema invalid = new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                List.of(),
                List.of(),
                List.of()
        );

        assertIssue(invalid, "must contain at least one type declaration");
    }

    /// Verifies that duplicate declaration names are rejected.
    @Test
    void rejectsDuplicateTypeNames() {
        AbiSchema schema = fixtureSchema();
        List<AbiSchema.TypeDefinition> types = new ArrayList<>(schema.types());
        types.add(schema.types().getFirst());

        assertIssue(withTypes(schema, types), "duplicates type declaration");
    }

    /// Verifies that every pointee reference resolves within the schema.
    @Test
    void rejectsUnresolvedTypeReferences() {
        AbiSchema schema = fixtureSchema();
        List<AbiSchema.TypeDefinition> types = new ArrayList<>(schema.types());
        types.add(new AbiSchema.PointerType(
                "missing_ptr",
                new AbiSchema.TypeRef("not_declared"),
                false,
                AbiSchema.Availability.UNRESTRICTED
        ));

        assertIssue(withTypes(schema, types), "does not resolve declaration 'not_declared'");
    }

    /// Verifies that non-power-of-two scalar alignment is rejected.
    @Test
    void rejectsIllegalPrimitiveAlignment() {
        AbiSchema schema = fixtureSchema();
        List<AbiSchema.TypeDefinition> types = new ArrayList<>(schema.types());
        types.add(new AbiSchema.PrimitiveType(
                "broken_integer",
                AbiSchema.PrimitiveKind.INTEGER,
                4,
                3,
                AbiSchema.Signedness.SIGNED,
                AbiSchema.Availability.UNRESTRICTED
        ));

        assertIssue(withTypes(schema, types), "must be a power of two no greater than byteSize");
    }

    /// Verifies that unsupported native callback conventions are rejected.
    @Test
    void rejectsUnsupportedCallbackConvention() {
        AbiSchema schema = fixtureSchema();
        AbiSchema.CallbackDefinition original = schema.callbacks().getFirst();
        AbiSchema.CallbackDefinition callback = new AbiSchema.CallbackDefinition(
                original.name(),
                original.result(),
                original.parameters(),
                AbiSchema.CallingConvention.STDCALL,
                original.threadRestriction(),
                original.lifetime(),
                original.exceptionPolicy(),
                original.availability()
        );
        AbiSchema invalid = new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                schema.types(),
                List.of(callback),
                schema.functions()
        );

        assertIssue(invalid, "is not supported for generated upcalls");
    }

    /// Verifies that callbacks cannot declare a void parameter.
    @Test
    void rejectsVoidCallbackParameter() {
        AbiSchema schema = fixtureSchema();
        AbiSchema.CallbackDefinition original = schema.callbacks().getFirst();
        AbiSchema.CallbackDefinition callback = new AbiSchema.CallbackDefinition(
                "void_parameter_callback",
                original.result(),
                List.of(new AbiSchema.Parameter(
                        "invalid",
                        new AbiSchema.TypeRef("void"),
                        AbiSchema.ParameterDirection.IN,
                        AbiSchema.Nullability.UNSPECIFIED,
                        AbiSchema.Ownership.NONE
                )),
                AbiSchema.CallingConvention.SYSTEM,
                AbiSchema.ThreadRestriction.ANY,
                AbiSchema.CallbackLifetime.SCOPED,
                AbiSchema.CallbackExceptionPolicy.CONTAIN,
                AbiSchema.Availability.UNRESTRICTED
        );
        List<AbiSchema.CallbackDefinition> callbacks = new ArrayList<>(schema.callbacks());
        callbacks.add(callback);
        AbiSchema invalid = new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                schema.types(),
                callbacks,
                schema.functions()
        );

        assertIssue(invalid, "VOID is not a valid parameter type");
    }

    /// Verifies that ordinary structure fields cannot overlap.
    @Test
    void rejectsOverlappingStructureFields() {
        AbiSchema schema = fixtureSchema();
        List<AbiSchema.TypeDefinition> types = new ArrayList<>(schema.types());
        types.add(new AbiSchema.AggregateType(
                "overlap",
                AbiSchema.AggregateKind.STRUCT,
                4,
                4,
                0,
                List.of(
                        new AbiSchema.AggregateField("first", new AbiSchema.TypeRef("i32"), 0, null, null),
                        new AbiSchema.AggregateField("second", new AbiSchema.TypeRef("u32"), 0, null, null)
                ),
                AbiSchema.Availability.UNRESTRICTED
        ));

        assertIssue(withTypes(schema, types), "overlaps ordinary field");
    }

    /// Verifies that aggregate sizes preserve their declared alignment.
    @Test
    void rejectsMisalignedAggregateSize() {
        AbiSchema schema = fixtureSchema();
        List<AbiSchema.TypeDefinition> types = new ArrayList<>(schema.types());
        types.add(new AbiSchema.AggregateType(
                "misaligned_size",
                AbiSchema.AggregateKind.STRUCT,
                6,
                4,
                0,
                List.of(new AbiSchema.AggregateField(
                        "value",
                        new AbiSchema.TypeRef("i32"),
                        0,
                        null,
                        null
                )),
                AbiSchema.Availability.UNRESTRICTED
        ));

        assertIssue(withTypes(schema, types), "must be a multiple of alignment");
    }

    /// Verifies that directly embedded aggregates cannot form an infinite layout cycle.
    @Test
    void rejectsDirectAggregateLayoutCycle() {
        AbiSchema schema = fixtureSchema();
        List<AbiSchema.TypeDefinition> types = new ArrayList<>(schema.types());
        types.add(new AbiSchema.AggregateType(
                "cycle_a",
                AbiSchema.AggregateKind.STRUCT,
                8,
                8,
                0,
                List.of(new AbiSchema.AggregateField(
                        "b",
                        new AbiSchema.TypeRef("cycle_b"),
                        0,
                        null,
                        null
                )),
                AbiSchema.Availability.UNRESTRICTED
        ));
        types.add(new AbiSchema.AggregateType(
                "cycle_b",
                AbiSchema.AggregateKind.STRUCT,
                8,
                8,
                0,
                List.of(new AbiSchema.AggregateField(
                        "a",
                        new AbiSchema.TypeRef("cycle_a"),
                        0,
                        null,
                        null
                )),
                AbiSchema.Availability.UNRESTRICTED
        ));

        assertIssue(withTypes(schema, types), "contains a direct aggregate layout cycle");
    }

    /// Verifies that bitfields may share storage only when their occupied bit ranges are disjoint.
    @Test
    void rejectsOverlappingBitfields() {
        AbiSchema schema = fixtureSchema();
        List<AbiSchema.TypeDefinition> types = new ArrayList<>(schema.types());
        types.add(new AbiSchema.AggregateType(
                "overlapping_bits",
                AbiSchema.AggregateKind.STRUCT,
                4,
                4,
                0,
                List.of(
                        new AbiSchema.AggregateField("low", new AbiSchema.TypeRef("u32"), 0, 0, 16),
                        new AbiSchema.AggregateField("middle", new AbiSchema.TypeRef("u32"), 0, 8, 8)
                ),
                AbiSchema.Availability.UNRESTRICTED
        ));

        assertIssue(withTypes(schema, types), "overlaps bitfield");
    }

    /// Verifies that a variadic boundary equals the fixed parameter count.
    @Test
    void rejectsInvalidVariadicBoundary() {
        AbiSchema schema = fixtureSchema();
        AbiSchema.FunctionDefinition original = function(schema, "fixture_format");
        AbiSchema.FunctionDefinition invalidFunction = new AbiSchema.FunctionDefinition(
                original.name(),
                original.symbol(),
                original.result(),
                original.parameters(),
                original.callingConvention(),
                2,
                original.errorPolicy(),
                original.threadRestriction(),
                original.availability()
        );
        List<AbiSchema.FunctionDefinition> functions = new ArrayList<>(schema.functions());
        functions.replaceAll(function -> function.name().equals(original.name()) ? invalidFunction : function);
        AbiSchema invalid = new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                schema.types(),
                schema.callbacks(),
                functions
        );

        assertIssue(invalid, "must equal the positive fixed-parameter count");
    }

    /// Returns a schema with a replacement type list.
    ///
    /// @param schema the source schema
    /// @param types the replacement types
    /// @return the modified schema
    private static AbiSchema withTypes(AbiSchema schema, List<AbiSchema.TypeDefinition> types) {
        return new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                types,
                schema.callbacks(),
                schema.functions()
        );
    }

    /// Returns a required fixture function by name.
    ///
    /// @param schema the fixture schema
    /// @param name the required function name
    /// @return the matching function
    private static AbiSchema.FunctionDefinition function(AbiSchema schema, String name) {
        for (AbiSchema.FunctionDefinition function : schema.functions()) {
            if (function.name().equals(name)) {
                return function;
            }
        }
        throw new IllegalArgumentException("Missing fixture function " + name);
    }

    /// Requires at least one issue to contain an expected diagnostic fragment.
    ///
    /// @param schema the invalid schema
    /// @param fragment the expected diagnostic fragment
    private static void assertIssue(AbiSchema schema, String fragment) {
        assertTrue(
                AbiSchemaValidator.validate(schema).stream()
                        .map(AbiSchemaValidator.Issue::toString)
                        .anyMatch(message -> message.contains(fragment)),
                () -> "Missing issue containing '" + fragment + "': " + AbiSchemaValidator.validate(schema)
        );
    }

    /// Reads and validates the minimum fixture.
    ///
    /// @return the fixture schema
    private static AbiSchema fixtureSchema() {
        try (InputStream input = Objects.requireNonNull(
                AbiSchemaValidatorTest.class.getResourceAsStream("/ffi-minimum-schema-v1.json"),
                "Missing ffi-minimum-schema-v1.json"
        )) {
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return AbiSchemaCodec.read("ffi-minimum-schema-v1", json);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read ffi-minimum-schema-v1.json", exception);
        }
    }
}
