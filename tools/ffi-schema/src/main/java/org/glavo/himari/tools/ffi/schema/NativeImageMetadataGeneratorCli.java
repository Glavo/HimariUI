package org.glavo.himari.tools.ffi.schema;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;

/// Generates GraalVM Native Image FFM reachability metadata from one canonical ABI schema.
@NotNullByDefault
public final class NativeImageMetadataGeneratorCli {
    /// Prevents instantiation of this command-line utility.
    private NativeImageMetadataGeneratorCli() {
    }

    /// Reads a canonical schema and writes one `reachability-metadata.json` document.
    ///
    /// @param arguments the input schema path followed by the output metadata path
    /// @throws IllegalArgumentException if the arguments, schema, or output are invalid
    public static void main(String[] arguments) {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected: <schema.json> <reachability-metadata.json>");
        }
        AbiSchema schema = AbiSchemaCodec.read(Path.of(arguments[0]));
        NativeImageMetadataGenerator.generate(schema, Path.of(arguments[1]));
    }
}
