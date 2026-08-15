package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.function.Consumer;
import java.util.function.Supplier;

/// Implements callback-local structural operations for [StructuralScope].
@NotNullByDefault
interface StructuralScopeSession {
    /// Declares one positional group.
    void group(String sourceIdentity, StructuralContent content);

    /// Declares one semantic-keyed group.
    void keyedGroup(String sourceIdentity, Object semanticKey, StructuralContent content);

    /// Declares one conditional group.
    void branch(String sourceIdentity, boolean visible, BranchRetention retention, StructuralContent content);

    /// Returns one positional value.
    <T> T remember(Class<T> valueType, Supplier<? extends T> factory);

    /// Returns one positional local cell.
    <T> StructuralLocal<T> rememberLocal(Class<T> valueType, T initialValue);

    /// Returns one positional owned resource.
    <T> T rememberResource(
            Class<T> valueType,
            Supplier<? extends T> factory,
            Consumer<? super T> disposer
    );

    /// Declares one effect.
    void effect(String key, Runnable mount, Runnable cleanup);

    /// Declares one mounted element with typed property bindings.
    void mount(String key, org.glavo.himari.runtime.mount.MountedElementContent content);

    /// Declares one keyed post-commit effect.
    void keyedEffect(
            String key,
            org.glavo.himari.runtime.effect.EffectDependencies dependencies,
            org.glavo.himari.runtime.effect.EffectCallbacks callbacks
    );

    /// Reads one ambient value.
    <T> T ambient(AmbientKey<T> key);

    /// Declares one ambient provider.
    <T> void provideAmbient(String sourceIdentity, AmbientKey<T> key, T value, StructuralContent content);

    /// Declares one error boundary.
    void errorBoundary(
            String sourceIdentity,
            ErrorBoundaryKey key,
            StructuralContent content,
            StructuralContent fallback
    );

    /// Declares one current-measure materialization group.
    <I> void measureGroup(
            String sourceIdentity,
            MeasureMaterializationKey<I> key,
            MeasureStructuralContent<I> content
    );

    /// Checks cooperative cancellation.
    void checkpoint();

    /// Rejects the current draft.
    void fail(String code);
}
