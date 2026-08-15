package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Captures one atomic display enumeration and its semantic topology generation.
///
/// The enumeration may be empty for a host temporarily lacking an output, although a platform may
/// impose a stronger availability requirement. Each snapshot's `enumerationIndex` must match its
/// list position and identifiers must be unique.
///
/// @param generation the nonnegative topology generation
/// @param displays the display snapshots in deterministic enumeration order
@NotNullByDefault
public record DisplayTopologySnapshot(
        long generation,
        @Unmodifiable List<DisplaySnapshot> displays
) {
    /// Creates an atomic topology snapshot and takes an immutable copy of the display list.
    ///
    /// @throws IllegalArgumentException if the generation is negative, an enumeration index is
    /// inconsistent, or an identifier is duplicated
    public DisplayTopologySnapshot {
        Objects.requireNonNull(displays, "displays");
        if (generation < 0L) {
            throw new IllegalArgumentException("Display topology generation must be nonnegative");
        }
        displays = List.copyOf(displays);
        Set<DisplayId> identifiers = new HashSet<>();
        for (int index = 0; index < displays.size(); index++) {
            DisplaySnapshot display = displays.get(index);
            if (display.enumerationIndex() != index) {
                throw new IllegalArgumentException("Display enumeration index does not match list position");
            }
            if (!identifiers.add(display.id())) {
                throw new IllegalArgumentException("Display topology contains a duplicate identifier");
            }
        }
    }
}
