package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Provides immutable, cross-source reads from one published [StateDomain] epoch.
///
/// A snapshot retains the values reachable from its publication until the snapshot itself becomes
/// unreachable. A source created after capture is not present and cannot be read through the older
/// snapshot. Derived caches and semantic versions are intentionally excluded because they are lazy
/// graph state rather than members of the atomic source publication.
@NotNullByDefault
public final class StateSnapshot {
    /// The domain from which this snapshot was captured.
    private final StateDomain domain;

    /// The immutable publication represented by this snapshot.
    private final StatePublication publication;

    /// Creates a snapshot over an immutable publication.
    ///
    /// @param domain the publishing domain
    /// @param publication the captured publication
    StateSnapshot(StateDomain domain, StatePublication publication) {
        this.domain = domain;
        this.publication = publication;
    }

    /// Returns the domain from which this snapshot was captured.
    ///
    /// @return the publishing domain
    public StateDomain domain() {
        return domain;
    }

    /// Returns the represented publication epoch.
    ///
    /// @return the captured epoch
    public long epoch() {
        return publication.epoch();
    }

    /// Returns the number of source slots represented by this snapshot.
    ///
    /// @return the captured source count
    public int sourceCount() {
        return publication.size();
    }

    /// Returns an object state's captured value.
    ///
    /// @param state the source to read
    /// @param <T> the value type
    /// @return the captured value
    /// @throws IllegalArgumentException if the state is not managed by this implementation, belongs
    /// to another domain, or was created after this snapshot
    @SuppressWarnings("unchecked")
    public <T> T get(State<T> state) {
        AbstractStateSource source = requireSource(state);
        return (T) publication.value(source.slot());
    }

    /// Returns an integer state's captured value.
    ///
    /// @param state the source to read
    /// @return the captured value
    /// @throws IllegalArgumentException if the state is not managed by this implementation, belongs
    /// to another domain, or was created after this snapshot
    public int get(IntState state) {
        return (Integer) value(state);
    }

    /// Returns a long state's captured value.
    ///
    /// @param state the source to read
    /// @return the captured value
    /// @throws IllegalArgumentException if the state is not managed by this implementation, belongs
    /// to another domain, or was created after this snapshot
    public long get(LongState state) {
        return (Long) value(state);
    }

    /// Returns a float state's captured value.
    ///
    /// @param state the source to read
    /// @return the captured value
    /// @throws IllegalArgumentException if the state is not managed by this implementation, belongs
    /// to another domain, or was created after this snapshot
    public float get(FloatState state) {
        return (Float) value(state);
    }

    /// Returns a boolean state's captured value.
    ///
    /// @param state the source to read
    /// @return the captured value
    /// @throws IllegalArgumentException if the state is not managed by this implementation, belongs
    /// to another domain, or was created after this snapshot
    public boolean get(BooleanState state) {
        return (Boolean) value(state);
    }

    /// Returns a source's captured semantic version.
    ///
    /// @param state the source to inspect
    /// @return the captured version
    /// @throws IllegalArgumentException if the state is not managed by this implementation, belongs
    /// to another domain, or was created after this snapshot
    public long version(StateSource state) {
        AbstractStateSource source = requireSource(state);
        return publication.version(source.slot());
    }

    /// Returns a primitive source's boxed captured value.
    ///
    /// @param state the primitive source to read
    /// @return the non-null boxed value
    private Object value(StateSource state) {
        AbstractStateSource source = requireSource(state);
        @Nullable Object value = publication.value(source.slot());
        return Objects.requireNonNull(value, "Primitive state value");
    }

    /// Returns the internal source after verifying snapshot compatibility.
    ///
    /// @param state the non-null source
    /// @return the internal source
    /// @throws IllegalArgumentException if the source belongs to another domain
    private AbstractStateSource requireSource(StateSource state) {
        Objects.requireNonNull(state, "state");
        if (state instanceof DerivedState<?>) {
            throw new IllegalArgumentException("State snapshots do not contain derived-state caches");
        }
        if (!(state instanceof AbstractStateSource source)) {
            throw new IllegalArgumentException("State source is not managed by this implementation");
        }
        if (source.owningDomain() != domain) {
            throw new IllegalArgumentException("State source belongs to another domain");
        }
        return source;
    }
}
