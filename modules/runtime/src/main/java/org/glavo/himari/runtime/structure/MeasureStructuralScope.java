package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Exposes the bounded operations permitted by current-measure materialization.
///
/// This scope may reconcile only direct semantic-keyed children of its declared materialization
/// group. Each child receives a normal [StructuralScope] for its own nested structure. The scope is
/// invalid after the measure callback returns.
@NotNullByDefault
public final class MeasureStructuralScope {
    /// The active runtime session implementing this facade.
    private final MeasureStructuralScopeSession session;

    /// Whether the declaring callback is still active.
    private boolean active = true;

    /// Creates one callback-local facade.
    ///
    /// @param session the active materialization session
    MeasureStructuralScope(MeasureStructuralScopeSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    /// Declares one direct semantic-keyed materialized child.
    ///
    /// @param sourceIdentity the stable nonblank handwritten collection identity
    /// @param semanticKey the non-null application identity
    /// @param content the child structural callback
    public void keyedGroup(String sourceIdentity, Object semanticKey, StructuralContent content) {
        checkActive();
        session.keyedGroup(sourceIdentity, semanticKey, content);
    }

    /// Reads the nearest inherited value and records it as a materialization dependency.
    ///
    /// @param key the typed ambient key
    /// @param <T> the value type
    /// @return the nearest override or the key default
    public <T> T ambient(AmbientKey<T> key) {
        checkActive();
        return session.ambient(key);
    }

    /// Observes cooperative cancellation at an explicit safe point.
    public void checkpoint() {
        checkActive();
        session.checkpoint();
    }

    /// Rejects the current materialization draft with an application-defined stable code.
    ///
    /// @param code the nonblank diagnostic code
    public void fail(String code) {
        checkActive();
        session.fail(code);
    }

    /// Deactivates this scope after its callback returns.
    void deactivate() {
        active = false;
    }

    /// Verifies callback-local lifetime.
    private void checkActive() {
        if (!active) {
            throw new IllegalStateException("Measure structural scope is no longer active");
        }
    }
}
