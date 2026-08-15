package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;

/// Describes one application-domain action without exposing a candidate runtime API.
///
/// @param operation the stable lower-kebab-case operation identifier
/// @param arguments immutable string-valued arguments interpreted by the named fixture
@NotNullByDefault
public record FixtureCommand(
        String operation,
        @Unmodifiable Map<String, String> arguments
) {
    /// Creates a validated immutable command.
    public FixtureCommand {
        operation = ComparisonContracts.requireIdentifier(operation, "fixture operation");
        arguments = ComparisonContracts.immutableSortedMap(arguments, "fixture arguments");
    }

    /// Creates an argument-free command.
    ///
    /// @param operation the operation identifier
    /// @return the command
    public static FixtureCommand of(String operation) {
        return new FixtureCommand(operation, Map.of());
    }

    /// Creates a command with one argument.
    ///
    /// @param operation the operation identifier
    /// @param key the argument key
    /// @param value the argument value
    /// @return the command
    public static FixtureCommand of(String operation, String key, String value) {
        return new FixtureCommand(operation, Map.of(key, value));
    }
}
