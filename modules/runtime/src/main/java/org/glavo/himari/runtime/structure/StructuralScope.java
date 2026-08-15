package org.glavo.himari.runtime.structure;

import org.glavo.himari.runtime.effect.EffectCallbacks;
import org.glavo.himari.runtime.effect.EffectDependencies;
import org.glavo.himari.runtime.mount.MountedElementContent;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
/// Exposes the operations permitted while one explicit structural group is executing.
///
/// A scope is callback-local. Retaining it or invoking it after its callback returns is invalid.
/// Every source identity is handwritten application text and must remain stable at that call site.
@NotNullByDefault
public final class StructuralScope {
    /// The active runtime session implementing this facade.
    private final StructuralScopeSession session;

    /// Whether the declaring callback is still active.
    private boolean active = true;

    /// Creates one callback-local facade.
    ///
    /// @param session the active runtime session
    StructuralScope(StructuralScopeSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    /// Declares one positional restartable child group.
    ///
    /// @param sourceIdentity the stable nonblank handwritten source identity
    /// @param content the child callback
    public void group(String sourceIdentity, StructuralContent content) {
        checkActive();
        session.group(sourceIdentity, content);
    }

    /// Declares one semantic-keyed restartable child group.
    ///
    /// The key must have stable [Object#equals(Object)] and [Object#hashCode()] behavior for its
    /// complete committed lifetime.
    ///
    /// @param sourceIdentity the stable nonblank handwritten collection identity
    /// @param semanticKey the non-null application identity
    /// @param content the child callback
    public void keyedGroup(String sourceIdentity, Object semanticKey, StructuralContent content) {
        checkActive();
        session.keyedGroup(sourceIdentity, semanticKey, content);
    }

    /// Declares one conditional group with an explicit hidden-state policy.
    ///
    /// @param sourceIdentity the stable nonblank handwritten branch identity
    /// @param visible whether the branch participates in active structure
    /// @param retention whether hiding disposes or deactivates the prior branch
    /// @param content the visible branch callback
    public void branch(
            String sourceIdentity,
            boolean visible,
            BranchRetention retention,
            StructuralContent content
    ) {
        checkActive();
        session.branch(sourceIdentity, visible, retention, content);
    }

    /// Returns one stable positional value owned by the current group.
    ///
    /// The factory runs only when the slot is first created. The value is not disposed by the
    /// runtime and must not be mutated as an unmanaged side effect during structural callbacks.
    ///
    /// @param valueType the runtime value type used to validate positional reuse
    /// @param factory the non-null first-value factory
    /// @param <T> the value type
    /// @return the stable value
    public <T> T remember(Class<T> valueType, Supplier<? extends T> factory) {
        checkActive();
        return session.remember(valueType, factory);
    }

    /// Returns one positional local reactive value owned by the current group.
    ///
    /// @param valueType the runtime value type
    /// @param initialValue the non-null value used only when the slot is first created
    /// @param <T> the value type
    /// @return the stable local cell
    public <T> StructuralLocal<T> rememberLocal(Class<T> valueType, T initialValue) {
        checkActive();
        return session.rememberLocal(valueType, initialValue);
    }

    /// Returns one stable positional resource owned by the current group.
    ///
    /// The disposer runs exactly once when a committed slot leaves structure or when a new slot's
    /// attempt aborts. The first declaration's disposer remains attached to the resource. Cleanup
    /// continues after a disposer failure and reports it diagnostically. Disposers must not write
    /// application state directly.
    ///
    /// @param valueType the runtime resource type used to validate positional reuse
    /// @param factory the non-null first-resource factory
    /// @param disposer the resource disposer
    /// @param <T> the resource type
    /// @return the stable resource
    public <T> T rememberResource(
            Class<T> valueType,
            Supplier<? extends T> factory,
            Consumer<? super T> disposer
    ) {
        checkActive();
        return session.rememberResource(valueType, factory, disposer);
    }

    /// Declares one structural mount and cleanup pair owned by the current group.
    ///
    /// A new key mounts only after the complete draft is valid. Removing the key or its group runs
    /// cleanup child-before-parent. Lifecycle callbacks must not write application state directly.
    ///
    /// @param key the nonblank group-local effect identity
    /// @param mount the activation callback
    /// @param cleanup the deactivation callback
    public void effect(String key, Runnable mount, Runnable cleanup) {
        checkActive();
        session.effect(key, mount, cleanup);
    }

    /// Declares one mounted element whose property bindings are independent reactive consumers.
    ///
    /// Binding reads do not become structural-group dependencies. Changing a bound source invalidates
    /// only the binding and the phases declared by its impact.
    ///
    /// @param key the nonblank group-local mount key
    /// @param content the binding declaration callback
    public void mount(String key, MountedElementContent content) {
        checkActive();
        session.mount(key, content);
    }

    /// Declares one keyed effect that mounts or updates only after a successful structural commit.
    ///
    /// A changed dependency identity schedules [EffectCallbacks#onUpdate(EffectSession)] at most
    /// once per apply.
    /// Removing the key schedules cleanup. Lifecycle callbacks must not write application state
    /// directly.
    ///
    /// @param key the nonblank group-local effect identity
    /// @param dependencies the comparable dependency identity
    /// @param callbacks the lifecycle callbacks
    public void keyedEffect(String key, EffectDependencies dependencies, EffectCallbacks callbacks) {
        checkActive();
        session.keyedEffect(key, dependencies, callbacks);
    }

    /// Reads the nearest inherited value and records its structural dependency.
    ///
    /// @param key the typed ambient key
    /// @param <T> the value type
    /// @return the nearest override or the key default
    public <T> T ambient(AmbientKey<T> key) {
        checkActive();
        return session.ambient(key);
    }

    /// Declares one positional subtree ambient override.
    ///
    /// Successive values are compared with [Object#equals(Object)]. A value must not be mutated in
    /// place while committed; publish a distinct immutable value when its semantics change.
    ///
    /// @param sourceIdentity the stable nonblank provider identity
    /// @param key the typed ambient key
    /// @param value the non-null override value
    /// @param content the overridden subtree
    /// @param <T> the value type
    public <T> void provideAmbient(
            String sourceIdentity,
            AmbientKey<T> key,
            T value,
            StructuralContent content
    ) {
        checkActive();
        session.provideAmbient(sourceIdentity, key, value, content);
    }

    /// Declares one positional application error boundary.
    ///
    /// @param sourceIdentity the stable nonblank boundary source identity
    /// @param key the resettable boundary identity
    /// @param content the normal subtree
    /// @param fallback the fallback subtree used after a contained failure
    public void errorBoundary(
            String sourceIdentity,
            ErrorBoundaryKey key,
            StructuralContent content,
            StructuralContent fallback
    ) {
        checkActive();
        session.errorBoundary(sourceIdentity, key, content, fallback);
    }

    /// Declares one positional layout-owned current-measure materialization group.
    ///
    /// The declaration preserves the last committed viewport until [StructuralRuntime#materialize]
    /// publishes a replacement.
    ///
    /// @param sourceIdentity the stable nonblank materialization source identity
    /// @param key the typed materialization identity
    /// @param content the measure callback
    /// @param <I> the immutable measure-input type
    public <I> void measureGroup(
            String sourceIdentity,
            MeasureMaterializationKey<I> key,
            MeasureStructuralContent<I> content
    ) {
        checkActive();
        session.measureGroup(sourceIdentity, key, content);
    }

    /// Observes cooperative cancellation at an explicit safe point.
    public void checkpoint() {
        checkActive();
        session.checkpoint();
    }

    /// Rejects the current draft with an application-defined stable code.
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
            throw new IllegalStateException("Structural scope is no longer active");
        }
    }
}
