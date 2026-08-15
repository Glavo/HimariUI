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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies deterministic fixed-signature FFM source generation and profile rejection.
@NotNullByDefault
final class FfmJavaGeneratorTest {
    /// Verifies that equal schemas produce byte-identical layout and binding sources.
    ///
    /// @param temporaryDirectory the JUnit-managed output root
    @Test
    void generatesDeterministicExactBindings(@TempDir Path temporaryDirectory) throws IOException {
        AbiSchema schema = supportedSchema();
        List<Path> first = FfmJavaGenerator.generate(schema, temporaryDirectory.resolve("first"));
        List<Path> second = FfmJavaGenerator.generate(schema, temporaryDirectory.resolve("second"));

        assertEquals(2, first.size());
        assertEquals(2, second.size());
        String layouts = Files.readString(first.getFirst(), StandardCharsets.UTF_8);
        String bindings = Files.readString(first.getLast(), StandardCharsets.UTF_8);
        assertEquals(layouts, Files.readString(second.getFirst(), StandardCharsets.UTF_8));
        assertEquals(bindings, Files.readString(second.getLast(), StandardCharsets.UTF_8));
        assertTrue(layouts.contains("MemoryLayout.unionLayout("));
        assertTrue(layouts.contains("FIXTURE_BITS_MODE_BIT_OFFSET"));
        assertTrue(bindings.contains("invokeExact("));
        assertTrue(bindings.contains("upcallStub("));
        assertTrue(bindings.contains("invokeFixtureVisitCallbackPointer("));
        assertTrue(bindings.contains("Linker.Option.captureCallState(\"errno\")"));
        assertTrue(bindings.contains("record FixtureOpenResult(MemorySegment value, int errorCode)"));
        assertTrue(bindings.contains("@SuppressWarnings(\"restricted\")"));
        assertFalse(bindings.contains("Object[]"));
        assertFalse(bindings.contains("Object..."));
    }

    /// Verifies aggregate-return ordering, typed Windows error capture, and generated-local name isolation.
    ///
    /// @param temporaryDirectory the JUnit-managed output root
    @Test
    void generatesImmediateGetLastErrorCapture(@TempDir Path temporaryDirectory) throws IOException {
        AbiSchema schema = fixtureSchema();
        AbiSchema.FunctionDefinition original = schema.functions().stream()
                .filter(function -> function.name().equals("fixture_pair_sum"))
                .toList()
                .getFirst();
        AbiSchema.FunctionDefinition windowsFunction = new AbiSchema.FunctionDefinition(
                original.name(),
                original.symbol(),
                new AbiSchema.ReturnValue(
                        new AbiSchema.TypeRef("fixture_pair"),
                        original.result().nullability(),
                        original.result().ownership()
                ),
                original.parameters(),
                original.callingConvention(),
                original.variadicFrom(),
                AbiSchema.ErrorPolicy.GET_LAST_ERROR,
                original.threadRestriction(),
                original.availability()
        );
        AbiSchema windowsSchema = new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                schema.types(),
                schema.callbacks(),
                List.of(windowsFunction)
        );

        List<Path> generated = FfmJavaGenerator.generate(windowsSchema, temporaryDirectory);
        String bindings = Files.readString(generated.getLast(), StandardCharsets.UTF_8);
        assertTrue(bindings.contains("Linker.Option.captureCallState(\"GetLastError\")"));
        assertTrue(
                bindings.contains("SegmentAllocator.class, MemorySegment.class, MemorySegment.class"),
                bindings
        );
        assertTrue(bindings.contains("fixture_pair_sumHandle.invokeExact(resultAllocator, callState, value)"));
        assertTrue(bindings.contains("MemorySegment nativeResult = (MemorySegment)"));
        assertFalse(bindings.contains("MemorySegment value = (MemorySegment)"));
        assertTrue(bindings.contains("record FixturePairSumResult(MemorySegment value, int errorCode)"));
    }

    /// Verifies exact aggregate-return function-pointer downcalls and contained upcall fallback generation.
    ///
    /// @param temporaryDirectory the JUnit-managed output root
    @Test
    void generatesAggregateCallbackReturn(@TempDir Path temporaryDirectory) throws IOException {
        AbiSchema schema = fixtureSchema();
        AbiSchema.CallbackDefinition original = schema.callbacks().getFirst();
        AbiSchema.CallbackDefinition aggregateCallback = new AbiSchema.CallbackDefinition(
                original.name(),
                new AbiSchema.ReturnValue(
                        new AbiSchema.TypeRef("fixture_pair"),
                        original.result().nullability(),
                        original.result().ownership()
                ),
                original.parameters(),
                original.callingConvention(),
                original.threadRestriction(),
                original.lifetime(),
                original.exceptionPolicy(),
                original.availability()
        );
        AbiSchema aggregateSchema = new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                schema.types(),
                List.of(aggregateCallback),
                List.of()
        );

        List<Path> generated = FfmJavaGenerator.generate(aggregateSchema, temporaryDirectory);
        String bindings = Files.readString(generated.getLast(), StandardCharsets.UTF_8);
        assertTrue(bindings.contains("MethodType.methodType(MemorySegment.class, MemorySegment.class, "
                + "SegmentAllocator.class, MemorySegment.class, MemorySegment.class)"));
        assertTrue(bindings.contains("invokeFixtureVisitCallbackPointer(MemorySegment function, "
                + "SegmentAllocator resultAllocator, MemorySegment value, MemorySegment context)"));
        assertTrue(bindings.contains("invokeExact(function, resultAllocator, value, context)"));
        assertTrue(bindings.contains("Arena.global().allocate(HimariFfiFixtureLayouts.FIXTURE_PAIR)"));
        assertTrue(bindings.contains("return FIXTURE_VISIT_CALLBACK_ZERO_RETURN;"));
    }

    /// Verifies that a variadic declaration cannot silently degrade to a generic invocation path.
    ///
    /// @param temporaryDirectory the JUnit-managed output root
    @Test
    void rejectsVariadicFunctionWithoutExactVariants(@TempDir Path temporaryDirectory) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FfmJavaGenerator.generate(fixtureSchema(), temporaryDirectory)
        );

        assertTrue(exception.getMessage().contains("variadic functions require explicit generated variants"));
    }

    /// Verifies that Java reserved words are rejected before source emission.
    ///
    /// @param temporaryDirectory the JUnit-managed output root
    @Test
    void rejectsReservedFunctionName(@TempDir Path temporaryDirectory) {
        AbiSchema schema = supportedSchema();
        AbiSchema.FunctionDefinition original = schema.functions().getFirst();
        AbiSchema.FunctionDefinition reserved = new AbiSchema.FunctionDefinition(
                "class",
                original.symbol(),
                original.result(),
                original.parameters(),
                original.callingConvention(),
                original.variadicFrom(),
                original.errorPolicy(),
                original.threadRestriction(),
                original.availability()
        );
        AbiSchema invalid = new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                schema.types(),
                schema.callbacks(),
                List.of(reserved)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FfmJavaGenerator.generate(invalid, temporaryDirectory)
        );
        assertTrue(exception.getMessage().contains("Java reserved word 'class'"));
    }

    /// Verifies that a library name cannot collapse to an empty generated class prefix.
    ///
    /// @param temporaryDirectory the JUnit-managed output root
    @Test
    void rejectsEmptyGeneratedClassPrefix(@TempDir Path temporaryDirectory) {
        AbiSchema schema = supportedSchema();
        AbiSchema invalid = new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                "_",
                schema.target(),
                schema.types(),
                schema.callbacks(),
                schema.functions()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FfmJavaGenerator.generate(invalid, temporaryDirectory)
        );
        assertTrue(exception.getMessage().contains("not a valid Java identifier"));
    }

    /// Returns a fixed-signature subset of the canonical minimum fixture.
    ///
    /// @return the supported generator schema
    private static AbiSchema supportedSchema() {
        AbiSchema schema = fixtureSchema();
        List<AbiSchema.FunctionDefinition> functions = schema.functions().stream()
                .filter(function -> function.name().equals("fixture_open")
                        || function.name().equals("fixture_pair_sum"))
                .toList();
        return new AbiSchema(
                schema.schemaVersion(),
                schema.namespace(),
                schema.library(),
                schema.target(),
                schema.types(),
                schema.callbacks(),
                functions
        );
    }

    /// Reads the canonical minimum fixture.
    ///
    /// @return the validated fixture schema
    private static AbiSchema fixtureSchema() {
        try (InputStream input = Objects.requireNonNull(
                FfmJavaGeneratorTest.class.getResourceAsStream("/ffi-minimum-schema-v1.json"),
                "Missing ffi-minimum-schema-v1.json"
        )) {
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return AbiSchemaCodec.read("ffi-minimum-schema-v1", json);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read ffi-minimum-schema-v1.json", exception);
        }
    }
}
