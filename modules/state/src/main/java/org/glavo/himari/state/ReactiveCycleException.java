package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

/// Reports a synchronous cycle encountered while pulling derived state.
///
/// The path begins with the first repeated computation and ends with that same computation. Names
/// are allocated deterministically by each [ReactiveGraph].
@NotNullByDefault
public final class ReactiveCycleException extends IllegalStateException {
    /// The serialization version for this exception representation.
    @Serial
    private static final long serialVersionUID = 1L;

    /// The private serializable closed cycle path.
    private final String @Unmodifiable [] path;

    /// Creates an exception for a non-empty closed cycle path.
    ///
    /// @param path the repeated computation path
    ReactiveCycleException(List<String> path) {
        super("Reactive dependency cycle: " + String.join(" -> ", path));
        Objects.requireNonNull(path, "path");
        if (path.size() < 2 || !path.getFirst().equals(path.getLast())) {
            throw new IllegalArgumentException("Reactive cycle path must be closed");
        }
        this.path = path.toArray(String[]::new);
    }

    /// Returns the immutable closed cycle path.
    ///
    /// @return the diagnostic computation names
    public @Unmodifiable List<String> path() {
        return List.of(path);
    }
}
