package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/// Provides shared validation and immutable-copy operations for the comparison protocol.
@NotNullByDefault
final class ComparisonContracts {
    /// The syntax used by stable machine-readable identifiers.
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z][a-z0-9]*(?:-[a-z0-9]+)*");

    /// Prevents construction.
    private ComparisonContracts() {
    }

    /// Validates one stable machine-readable identifier.
    ///
    /// @param value the identifier
    /// @param name the diagnostic field name
    /// @return `value`
    /// @throws IllegalArgumentException if the identifier is malformed
    static String requireIdentifier(String value, String name) {
        Objects.requireNonNull(value, name);
        if (!IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must use lower-kebab-case: " + value);
        }
        return value;
    }

    /// Validates a nonblank human-readable value.
    ///
    /// @param value the value
    /// @param name the diagnostic field name
    /// @return `value`
    /// @throws IllegalArgumentException if the value is blank
    static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    /// Returns an immutable key-sorted copy whose keys and values are non-null.
    ///
    /// @param values the input map
    /// @param name the diagnostic field name
    /// @param <V> the value type
    /// @return the immutable sorted copy
    static <V> @Unmodifiable Map<String, V> immutableSortedMap(Map<String, ? extends V> values, String name) {
        Objects.requireNonNull(values, name);
        TreeMap<String, V> copy = new TreeMap<>();
        for (Map.Entry<String, ? extends V> entry : values.entrySet()) {
            String key = requireText(entry.getKey(), name + " key");
            V value = Objects.requireNonNull(entry.getValue(), name + "[" + key + "]");
            if (copy.put(key, value) != null) {
                throw new IllegalArgumentException(name + " repeats key " + key);
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    /// Returns an immutable defensive copy of an enum set.
    ///
    /// @param values the input set
    /// @param type the enum class
    /// @param name the diagnostic field name
    /// @param <E> the enum type
    /// @return the immutable copy
    static <E extends Enum<E>> @Unmodifiable Set<E> immutableEnumSet(
            Set<E> values,
            Class<E> type,
            String name
    ) {
        Objects.requireNonNull(values, name);
        EnumSet<E> copy = EnumSet.noneOf(type);
        for (E value : values) {
            copy.add(Objects.requireNonNull(value, name + " element"));
        }
        return Collections.unmodifiableSet(copy);
    }

    /// Validates a nonnegative metric.
    ///
    /// @param value the metric value
    /// @param name the diagnostic field name
    /// @return `value`
    /// @throws IllegalArgumentException if the value is negative
    static long requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be nonnegative");
        }
        return value;
    }
}
