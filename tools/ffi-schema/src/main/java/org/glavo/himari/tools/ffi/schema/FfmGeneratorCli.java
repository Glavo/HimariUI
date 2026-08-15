package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;

/// Generates Java FFM sources from one canonical schema document.
@NotNullByDefault
public final class FfmGeneratorCli {
    /// Prevents instantiation of this command-line utility.
    private FfmGeneratorCli() {
    }

    /// Reads a canonical schema and writes generated Java sources.
    ///
    /// @param arguments the input schema path followed by the generated-source directory
    /// @throws IllegalArgumentException if the arguments, schema, or output are invalid
    public static void main(String[] arguments) {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected: <schema.json> <generated-source-directory>");
        }
        AbiSchema schema = AbiSchemaCodec.read(Path.of(arguments[0]));
        FfmJavaGenerator.generate(schema, Path.of(arguments[1]));
    }
}
