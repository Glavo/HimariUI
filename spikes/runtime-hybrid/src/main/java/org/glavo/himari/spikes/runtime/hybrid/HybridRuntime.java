package org.glavo.himari.spikes.runtime.hybrid;

import org.glavo.himari.spikes.runtime.sample.ComparisonProbe;
import org.glavo.himari.spikes.runtime.sample.DiagnosticTrace;
import org.glavo.himari.spikes.runtime.sample.RuntimeCallbackKind;
import org.glavo.himari.spikes.runtime.sample.RuntimeHealth;
import org.glavo.himari.spikes.runtime.sample.RuntimePhase;
import org.glavo.himari.state.StateSource;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Implements the fine-grained binding and small rerunnable structural-scope model evaluated in M1.
///
/// Stable component owners initialize once and update properties through [Binding] callbacks. A
/// [StructuralScope] reruns only its explicit declaration when its topology source advances. That
/// declaration reconciles semantic-keyed fragments whose own owner initializers execute only when
/// a new identity is introduced.
@NotNullByDefault
final class HybridRuntime implements AutoCloseable {
    /// Deterministic shallow-byte estimate for an owner.
    private static final long OWNER_BYTES = 80L;

    /// Deterministic shallow-byte estimate for a mounted-node record.
    private static final long NODE_BYTES = 48L;

    /// Deterministic shallow-byte estimate for a local integer cell.
    private static final long LOCAL_BYTES = 24L;

    /// Deterministic shallow-byte estimate for a property binding.
    private static final long BINDING_BYTES = 64L;

    /// Deterministic shallow-byte estimate for an effect descriptor.
    private static final long EFFECT_BYTES = 48L;

    /// Deterministic shallow-byte estimate for a structural anchor.
    private static final long ANCHOR_BYTES = 40L;

    /// Deterministic shallow-byte estimate for a structural-scope controller.
    private static final long STRUCTURAL_SCOPE_BYTES = 88L;

    /// Deterministic shallow-byte estimate for a semantic fragment record.
    private static final long FRAGMENT_BYTES = 48L;

    /// The shared comparison instrumentation sink.
    private final ComparisonProbe probe;

    /// Every registered property binding, including bindings in retained inactive fragments.
    private final ArrayList<Binding> bindings = new ArrayList<>();

    /// Every registered structural scope, including scopes in retained inactive fragments.
    private final ArrayList<StructuralScope<?, ?>> structuralScopes = new ArrayList<>();

    /// The mounted root owner, or `null` before mount and after close.
    private @Nullable Owner root;

    /// The immutable committed semantic-node order.
    private @Unmodifiable List<String> mountedNodes = List.of();

    /// The number of registered owner records.
    private long liveOwners;

    /// The number of registered mounted-node records.
    private long liveNodes;

    /// The number of registered effect descriptors.
    private long liveEffects;

    /// The number of structural mutations currently being staged.
    private long stagedMutations;

    /// Whether the runtime has been closed.
    private boolean closed;

    /// Creates an unmounted hybrid runtime.
    ///
    /// @param probe the shared instrumentation sink
    HybridRuntime(ComparisonProbe probe) {
        this.probe = Objects.requireNonNull(probe, "probe");
    }

    /// Initializes stable root ownership once and publishes its initial fragment tree.
    ///
    /// @param initializer the root initializer
    /// @throws IllegalStateException if the runtime is already mounted or closed
    /// @throws HybridMutationException if initialization rejects its staged tree
    void mount(Initializer initializer) {
        checkOpen();
        if (root != null) {
            throw new IllegalStateException("Hybrid runtime is already mounted");
        }
        Owner stagedRoot = new Owner("root");
        stagedMutations++;
        try {
            initialize(stagedRoot, Objects.requireNonNull(initializer, "initializer"));
            registerOwner(stagedRoot, true);
            root = stagedRoot;
            refreshMountedNodes();
        } catch (HybridMutationException failure) {
            abortOwner(stagedRoot);
            trace(failure);
            throw failure;
        } catch (RuntimeException | Error failure) {
            abortOwner(stagedRoot);
            HybridMutationException wrapped = new HybridMutationException(
                    "owner-initialization-failed",
                    stagedRoot.path,
                    failure
            );
            trace(wrapped);
            throw wrapped;
        } finally {
            stagedMutations--;
        }
    }

    /// Polls changed small structural scopes, then changed fine-grained property bindings.
    ///
    /// Stable component and fragment initializers do not rerun during this method.
    ///
    /// @throws IllegalStateException if the runtime is unmounted or closed
    /// @throws HybridMutationException if a structural declaration rejects its draft
    void flush() {
        Owner currentRoot = requireRoot();
        boolean structureChanged = false;
        try {
            for (int index = 0; index < structuralScopes.size(); index++) {
                StructuralScope<?, ?> scope = structuralScopes.get(index);
                if (scope.active && scope.poll()) {
                    structureChanged = true;
                }
            }
            for (int index = 0; index < bindings.size(); index++) {
                Binding binding = bindings.get(index);
                if (binding.active) {
                    binding.poll();
                }
            }
        } catch (HybridMutationException failure) {
            trace(failure);
            throw failure;
        } catch (RuntimeException | Error failure) {
            HybridMutationException wrapped = new HybridMutationException(
                    "reactive-callback-failed",
                    currentRoot.path,
                    failure
            );
            trace(wrapped);
            throw wrapped;
        }
        if (structureChanged) {
            refreshMountedNodes();
        }
    }

    /// Returns the committed semantic-node order.
    ///
    /// @return the immutable mounted nodes
    @Unmodifiable List<String> mountedNodes() {
        requireRoot();
        return mountedNodes;
    }

    /// Returns registered resource counts for the active session.
    ///
    /// @return the health snapshot
    RuntimeHealth health() {
        if (closed) {
            return RuntimeHealth.CLEAN;
        }
        return new RuntimeHealth(liveNodes, liveOwners, liveEffects, stagedMutations, 0L);
    }

    /// Disposes every owner, fragment, scope, binding, effect, anchor, node, and local cell.
    ///
    /// Closure is idempotent.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (stagedMutations != 0L) {
            throw new IllegalStateException("Hybrid runtime cannot close during a staged mutation");
        }
        @Nullable Owner currentRoot = root;
        if (currentRoot != null) {
            releaseOwner(currentRoot);
        }
        root = null;
        mountedNodes = List.of();
        bindings.clear();
        structuralScopes.clear();
        closed = true;
    }

    /// Executes one stable owner initializer exactly once.
    ///
    /// @param owner the staged owner
    /// @param initializer the initializer
    private void initialize(Owner owner, Initializer initializer) {
        if (owner.initialized) {
            throw new IllegalStateException("Owner initializer executed more than once: " + owner.path);
        }
        owner.initialized = true;
        probe.callbackExecuted(RuntimeCallbackKind.STRUCTURE);
        initializer.initialize(owner);
    }

    /// Registers a complete staged owner subtree and optionally activates it.
    ///
    /// @param owner the staged owner
    /// @param active whether its bindings and effects are active
    private void registerOwner(Owner owner, boolean active) {
        if (owner.registered) {
            throw new IllegalStateException("Owner is already registered: " + owner.path);
        }
        owner.registered = true;
        owner.active = active;
        probe.retained(owner, OWNER_BYTES);
        liveOwners++;

        for (LocalInt local : owner.locals) {
            probe.retained(local, LOCAL_BYTES);
            local.registered = true;
        }
        for (Binding binding : owner.bindings) {
            probe.retained(binding, BINDING_BYTES);
            binding.registered = true;
            bindings.add(binding);
        }
        for (Effect effect : owner.effects) {
            probe.retained(effect, EFFECT_BYTES);
            effect.registered = true;
            liveEffects++;
        }
        for (Object entry : owner.entries) {
            if (entry instanceof Node node) {
                probe.retained(node, NODE_BYTES);
                node.registered = true;
                liveNodes++;
            } else if (entry instanceof Owner child) {
                registerOwner(child, active);
            } else if (entry instanceof Anchor anchor) {
                probe.retained(anchor, ANCHOR_BYTES);
                anchor.registered = true;
                anchor.scope.register(active);
            } else {
                throw new IllegalStateException("Unknown hybrid owner entry");
            }
        }
        if (active) {
            activateDirect(owner);
        }
        for (Runnable callback : owner.afterCommit) {
            callback.run();
        }
        owner.afterCommit.clear();
        owner.abortCleanups.clear();
    }

    /// Activates direct property bindings and effects for one registered owner.
    ///
    /// @param owner the owner
    private void activateDirect(Owner owner) {
        for (Binding binding : owner.bindings) {
            binding.activate();
        }
        for (Effect effect : owner.effects) {
            effect.activate();
        }
    }

    /// Deactivates one retained owner subtree child-first without releasing records.
    ///
    /// @param owner the active owner
    private void deactivateOwner(Owner owner) {
        if (!owner.active) {
            return;
        }
        for (int index = owner.entries.size() - 1; index >= 0; index--) {
            Object entry = owner.entries.get(index);
            if (entry instanceof Owner child) {
                deactivateOwner(child);
            } else if (entry instanceof Anchor anchor) {
                anchor.scope.deactivate();
            }
        }
        for (int index = owner.effects.size() - 1; index >= 0; index--) {
            owner.effects.get(index).deactivate();
        }
        for (Binding binding : owner.bindings) {
            binding.deactivate();
        }
        owner.active = false;
    }

    /// Reactivates one retained owner without rerunning its initializer.
    ///
    /// @param owner the retained owner
    private void activateOwner(Owner owner) {
        if (owner.active) {
            return;
        }
        owner.active = true;
        activateDirect(owner);
        for (Object entry : owner.entries) {
            if (entry instanceof Owner child) {
                activateOwner(child);
            } else if (entry instanceof Anchor anchor) {
                anchor.scope.activate();
            }
        }
    }

    /// Releases one registered owner subtree and every directly owned record.
    ///
    /// @param owner the owner
    private void releaseOwner(Owner owner) {
        if (!owner.registered) {
            return;
        }
        deactivateOwner(owner);
        for (int index = owner.entries.size() - 1; index >= 0; index--) {
            Object entry = owner.entries.get(index);
            if (entry instanceof Owner child) {
                releaseOwner(child);
            } else if (entry instanceof Anchor anchor) {
                anchor.scope.release();
                probe.released(anchor);
                anchor.registered = false;
            } else if (entry instanceof Node node) {
                probe.released(node);
                node.registered = false;
                liveNodes--;
            }
        }
        for (int index = owner.effects.size() - 1; index >= 0; index--) {
            Effect effect = owner.effects.get(index);
            probe.released(effect);
            effect.registered = false;
            liveEffects--;
        }
        for (int index = owner.bindings.size() - 1; index >= 0; index--) {
            Binding binding = owner.bindings.get(index);
            bindings.remove(binding);
            probe.released(binding);
            binding.registered = false;
        }
        for (int index = owner.locals.size() - 1; index >= 0; index--) {
            LocalInt local = owner.locals.get(index);
            probe.released(local);
            local.registered = false;
        }
        probe.released(owner);
        owner.registered = false;
        owner.disposed = true;
        liveOwners--;
    }

    /// Runs abort callbacks for one unregistered staged owner subtree.
    ///
    /// @param owner the staged owner
    private static void abortOwner(Owner owner) {
        for (Object entry : owner.entries) {
            if (entry instanceof Owner child) {
                abortOwner(child);
            } else if (entry instanceof Anchor anchor) {
                anchor.scope.abortStagedChildren();
            }
        }
        for (Runnable cleanup : owner.abortCleanups) {
            cleanup.run();
        }
    }

    /// Rebuilds the semantic node list from active stable owners and structural fragments.
    private void refreshMountedNodes() {
        Owner currentRoot = requireRoot();
        ArrayList<String> nodes = new ArrayList<>();
        flatten(currentRoot, nodes);
        mountedNodes = List.copyOf(nodes);
    }

    /// Flattens one active owner subtree in declaration order.
    ///
    /// @param owner the owner
    /// @param nodes the result list
    private void flatten(Owner owner, List<String> nodes) {
        if (!owner.active) {
            return;
        }
        for (Object entry : owner.entries) {
            if (entry instanceof Node node) {
                nodes.add(node.identifier);
                probe.nodesVisited(1L);
            } else if (entry instanceof Owner child) {
                flatten(child, nodes);
            } else if (entry instanceof Anchor anchor) {
                for (Owner fragmentOwner : anchor.scope.activeOwners()) {
                    flatten(fragmentOwner, nodes);
                }
            }
        }
    }

    /// Emits a deterministic structured trace for one rejected structural mutation.
    ///
    /// @param failure the contained failure
    private void trace(HybridMutationException failure) {
        probe.trace(new DiagnosticTrace(
                failure.code(),
                "Hybrid structural scope rejected its staged mutation",
                "hybrid-app",
                failure.ownerPath(),
                null,
                "discard-staged-fragments-and-preserve-scope-anchor"
        ));
    }

    /// Returns the mounted root owner.
    ///
    /// @return the root owner
    private Owner requireRoot() {
        checkOpen();
        @Nullable Owner currentRoot = root;
        if (currentRoot == null) {
            throw new IllegalStateException("Hybrid runtime is not mounted");
        }
        return currentRoot;
    }

    /// Verifies that the runtime remains open.
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Hybrid runtime is closed");
        }
    }

    /// Returns an immutable defensive copy of a phase declaration.
    ///
    /// @param phases the declared phases
    /// @return the immutable phases
    private static @Unmodifiable List<RuntimePhase> phaseList(RuntimePhase... phases) {
        RuntimePhase[] copy = Objects.requireNonNull(phases, "phases").clone();
        for (RuntimePhase phase : copy) {
            Objects.requireNonNull(phase, "phase");
        }
        return List.of(copy);
    }

    /// Defines an initializer that executes once for one owner identity.
    @FunctionalInterface
    @NotNullByDefault
    interface Initializer {
        /// Initializes one new owner.
        ///
        /// @param owner the new owner
        void initialize(Owner owner);
    }

    /// Creates an application payload while initializing one new structural fragment owner.
    ///
    /// @param <V> the payload type
    @FunctionalInterface
    @NotNullByDefault
    interface FragmentInitializer<V> {
        /// Initializes one new fragment owner and returns its payload.
        ///
        /// @param owner the new fragment owner
        /// @return the non-null payload
        V initialize(Owner owner);
    }

    /// Declares the desired fragments for one execution of a small structural scope.
    ///
    /// @param <K> the semantic key type
    /// @param <V> the fragment payload type
    @FunctionalInterface
    @NotNullByDefault
    interface StructuralDeclaration<K, V> {
        /// Declares active semantic fragments in mounted order.
        ///
        /// @param scope the current draft writer
        void declare(FragmentScope<K, V> scope);
    }

    /// Selects what happens to a fragment owner omitted by the next structural execution.
    @NotNullByDefault
    enum Retention {
        /// Keep owner-local state while deactivating bindings and effects.
        RETAIN,

        /// Dispose the omitted owner and recreate it if its key returns.
        DISPOSE
    }

    /// Owns one stable component or fragment instance and its direct runtime records.
    @NotNullByDefault
    final class Owner {
        /// The stable diagnostic owner path.
        private final String path;

        /// Nodes, component owners, and structural anchors in declaration order.
        private final ArrayList<Object> entries = new ArrayList<>();

        /// Direct owner-local integer cells.
        private final ArrayList<LocalInt> locals = new ArrayList<>();

        /// Direct property bindings.
        private final ArrayList<Binding> bindings = new ArrayList<>();

        /// Direct effect descriptors.
        private final ArrayList<Effect> effects = new ArrayList<>();

        /// Cleanups for resources created only by this staged owner.
        private final ArrayList<Runnable> abortCleanups = new ArrayList<>();

        /// Callbacks that run only after this owner becomes committed.
        private final ArrayList<Runnable> afterCommit = new ArrayList<>();

        /// Whether the owner initializer has executed.
        private boolean initialized;

        /// Whether this owner is registered with the runtime.
        private boolean registered;

        /// Whether this owner participates in mounted structure.
        private boolean active;

        /// Whether this owner has been permanently disposed.
        private boolean disposed;

        /// Creates one staged owner.
        ///
        /// @param path the diagnostic path
        private Owner(String path) {
            this.path = path;
        }

        /// Declares one persistent semantic node.
        ///
        /// @param identifier the node identifier
        void node(String identifier) {
            checkMutable();
            entries.add(new Node(Objects.requireNonNull(identifier, "identifier")));
        }

        /// Initializes one stable child component exactly once.
        ///
        /// @param key the diagnostic child identity
        /// @param initializer the child initializer
        /// @return the child owner
        Owner component(String key, Initializer initializer) {
            checkMutable();
            Owner child = new Owner(path + '/' + Objects.requireNonNull(key, "key"));
            HybridRuntime.this.initialize(child, Objects.requireNonNull(initializer, "initializer"));
            entries.add(child);
            return child;
        }

        /// Creates one explicit owner-local integer cell.
        ///
        /// @param initialValue the initial value
        /// @return the cell
        LocalInt localInt(int initialValue) {
            checkMutable();
            LocalInt local = new LocalInt(initialValue);
            locals.add(local);
            return local;
        }

        /// Declares one deferred fine-grained property binding.
        ///
        /// @param source the versioned producer
        /// @param site the stable local read-site identity
        /// @param update the deferred property update
        /// @param phases the phases affected by a semantic producer change
        void bind(StateSource source, String site, Runnable update, RuntimePhase... phases) {
            checkMutable();
            bindings.add(new Binding(
                    this,
                    Objects.requireNonNull(source, "source"),
                    Objects.requireNonNull(site, "site"),
                    Objects.requireNonNull(update, "update"),
                    phaseList(phases)
            ));
        }

        /// Declares an effect whose lifetime is this owner's active lifetime.
        ///
        /// @param mount the activation callback
        /// @param cleanup the deactivation callback
        void effect(Runnable mount, Runnable cleanup) {
            checkMutable();
            effects.add(new Effect(
                    Objects.requireNonNull(mount, "mount"),
                    Objects.requireNonNull(cleanup, "cleanup")
            ));
        }

        /// Declares one small rerunnable structural scope at a stable anchor.
        ///
        /// The scope callback reruns when `source` advances. Fragment owner initializers execute only
        /// for semantic keys not already active or retained by this scope.
        ///
        /// @param anchorKey the anchor identity
        /// @param source the topology producer
        /// @param declaration the rerunnable fragment declaration
        /// @param phases the phases affected by topology changes
        /// @param <K> the semantic key type
        /// @param <V> the fragment payload type
        /// @return the structural scope
        <K, V> StructuralScope<K, V> structure(
                String anchorKey,
                StateSource source,
                StructuralDeclaration<K, V> declaration,
                RuntimePhase... phases
        ) {
            checkMutable();
            StructuralScope<K, V> scope = new StructuralScope<>(
                    this,
                    Objects.requireNonNull(anchorKey, "anchorKey"),
                    Objects.requireNonNull(source, "source"),
                    Objects.requireNonNull(declaration, "declaration"),
                    phaseList(phases)
            );
            entries.add(new Anchor(scope));
            return scope;
        }

        /// Registers cleanup for resources allocated only by this staged owner.
        ///
        /// @param cleanup the abort cleanup
        void onAbort(Runnable cleanup) {
            checkMutable();
            abortCleanups.add(Objects.requireNonNull(cleanup, "cleanup"));
        }

        /// Registers a callback that runs after this owner is committed.
        ///
        /// @param callback the callback
        void onCommit(Runnable callback) {
            checkMutable();
            afterCommit.add(Objects.requireNonNull(callback, "callback"));
        }

        /// Rejects the staged owner with a stable diagnostic code.
        ///
        /// @param code the failure code
        void fail(String code) {
            throw new HybridMutationException(Objects.requireNonNull(code, "code"), path, null);
        }

        /// Verifies that declarations remain legal for this owner.
        private void checkMutable() {
            if (!initialized || registered || disposed) {
                throw new IllegalStateException("Owner is not accepting declarations: " + path);
            }
        }
    }

    /// Stores one explicit owner-local mutable integer.
    @NotNullByDefault
    static final class LocalInt {
        /// The current value.
        private int value;

        /// Whether the cell is registered with the runtime.
        private boolean registered;

        /// Creates one staged local cell.
        ///
        /// @param value the initial value
        private LocalInt(int value) {
            this.value = value;
        }

        /// Returns the current value.
        ///
        /// @return the value
        int get() {
            return value;
        }

        /// Increments the value by one.
        void increment() {
            if (!registered) {
                throw new IllegalStateException("Owner-local state is not committed");
            }
            value = Math.incrementExact(value);
        }
    }

    /// Writes one structural-scope draft using semantic fragment keys.
    ///
    /// @param <K> the semantic key type
    /// @param <V> the fragment payload type
    @NotNullByDefault
    final class FragmentScope<K, V> {
        /// The structural controller whose prior fragments may be reused.
        private final StructuralScope<K, V> controller;

        /// The next active fragments by semantic key.
        private final LinkedHashMap<K, Fragment<K, V>> next = new LinkedHashMap<>();

        /// The next fragments in semantic mounted order.
        private final ArrayList<Fragment<K, V>> ordered = new ArrayList<>();

        /// Newly initialized fragments that require registration or abort cleanup.
        private final ArrayList<Fragment<K, V>> newFragments = new ArrayList<>();

        /// Creates an empty draft writer for one structural execution.
        ///
        /// @param controller the owning scope
        private FragmentScope(StructuralScope<K, V> controller) {
            this.controller = controller;
        }

        /// Declares one active semantic fragment at the current mounted position.
        ///
        /// A retained fragment omitted by an earlier execution is reused without rerunning
        /// `initializer`. A disposed or never-seen key creates a new fragment owner.
        ///
        /// @param key the semantic key
        /// @param retention the policy applied when this fragment is omitted
        /// @param initializer the initializer for a newly introduced identity
        void fragment(K key, Retention retention, FragmentInitializer<V> initializer) {
            Objects.requireNonNull(key, "fragment key");
            Objects.requireNonNull(retention, "retention");
            Objects.requireNonNull(initializer, "initializer");
            if (next.containsKey(key)) {
                throw new HybridMutationException(
                        "duplicate-key",
                        controller.path + '[' + key.toString() + ']',
                        null
                );
            }
            @Nullable Fragment<K, V> fragment = controller.activeFragments.get(key);
            if (fragment == null) {
                fragment = controller.dormantFragments.get(key);
            }
            if (fragment == null) {
                fragment = controller.createFragment(key, retention, initializer);
                newFragments.add(fragment);
            } else if (fragment.retention != retention) {
                throw new HybridMutationException(
                        "retention-policy-changed",
                        fragment.owner.path,
                        null
                );
            }
            next.put(key, fragment);
            ordered.add(fragment);
        }

        /// Returns the fully staged fragment set.
        ///
        /// @return the staged set
        private StagedFragments<K, V> finish() {
            return new StagedFragments<>(next, ordered, newFragments);
        }

        /// Aborts every newly initialized fragment in reverse declaration order.
        private void abortNewFragments() {
            for (int index = newFragments.size() - 1; index >= 0; index--) {
                abortOwner(newFragments.get(index).owner);
            }
            newFragments.clear();
        }
    }

    /// Controls one stable anchor whose small structural declaration may rerun.
    ///
    /// @param <K> the semantic fragment key type
    /// @param <V> the fragment payload type
    @NotNullByDefault
    final class StructuralScope<K, V> {
        /// The anchor owner.
        private final Owner parent;

        /// The stable diagnostic path.
        private final String path;

        /// The producer that invalidates this scope.
        private final StateSource source;

        /// The rerunnable small structural declaration.
        private final StructuralDeclaration<K, V> declaration;

        /// The phases invalidated by a committed topology change.
        private final @Unmodifiable List<RuntimePhase> phases;

        /// Active fragments by semantic key.
        private LinkedHashMap<K, Fragment<K, V>> activeFragments = new LinkedHashMap<>();

        /// Active fragments in mounted order.
        private ArrayList<Fragment<K, V>> orderedFragments = new ArrayList<>();

        /// Retained inactive fragments by semantic key.
        private LinkedHashMap<K, Fragment<K, V>> dormantFragments = new LinkedHashMap<>();

        /// The source version incorporated by the last successful execution.
        private long observedVersion;

        /// Whether the scope record is registered.
        private boolean registered;

        /// Whether its dependency edge and active fragments are active.
        private boolean active;

        /// Creates one staged structural scope and executes its initial declaration.
        ///
        /// @param parent the anchor owner
        /// @param key the anchor key
        /// @param source the topology producer
        /// @param declaration the structural callback
        /// @param phases the topology phases
        private StructuralScope(
                Owner parent,
                String key,
                StateSource source,
                StructuralDeclaration<K, V> declaration,
                @Unmodifiable List<RuntimePhase> phases
        ) {
            this.parent = parent;
            this.path = parent.path + "/scope:" + key;
            this.source = source;
            this.declaration = declaration;
            this.phases = List.copyOf(phases);
            this.observedVersion = source.version();
            StagedFragments<K, V> initial = stage();
            activeFragments = initial.fragments;
            orderedFragments = initial.ordered;
        }

        /// Returns active semantic keys in mounted order.
        ///
        /// @return the immutable active keys
        @Unmodifiable List<K> keys() {
            return orderedFragments.stream().map(fragment -> fragment.key).toList();
        }

        /// Returns whether one semantic key is currently active.
        ///
        /// @param key the semantic key
        /// @return whether its fragment participates in mounted structure
        boolean visible(K key) {
            return activeFragments.containsKey(Objects.requireNonNull(key, "key"));
        }

        /// Returns one active or retained fragment payload.
        ///
        /// @param key the semantic key
        /// @return the payload, or `null` when the scope does not own that key
        @Nullable V value(K key) {
            Objects.requireNonNull(key, "key");
            @Nullable Fragment<K, V> fragment = activeFragments.get(key);
            if (fragment == null) {
                fragment = dormantFragments.get(key);
            }
            return fragment == null ? null : fragment.payload;
        }

        /// Registers the scope, initial fragments, and optionally its dependency edge.
        ///
        /// @param parentActive whether the anchor owner is active
        private void register(boolean parentActive) {
            if (registered) {
                throw new IllegalStateException("Structural scope is already registered: " + path);
            }
            probe.retained(this, STRUCTURAL_SCOPE_BYTES);
            registered = true;
            structuralScopes.add(this);
            for (Fragment<K, V> fragment : orderedFragments) {
                registerFragment(fragment, parentActive);
            }
            if (parentActive) {
                active = true;
                probe.dependencyAttached(this);
            }
        }

        /// Polls and reruns only this structural callback after a source-version change.
        ///
        /// @return whether mounted topology changed
        private boolean poll() {
            long version = source.version();
            if (version == observedVersion) {
                return false;
            }
            boolean changed = reconcile();
            observedVersion = version;
            return changed;
        }

        /// Stages and commits the smallest declared fragment set.
        ///
        /// @return whether active fragment topology changed
        private boolean reconcile() {
            stagedMutations++;
            try {
                StagedFragments<K, V> staged = stage();
                if (sameOrder(orderedFragments, staged.ordered)) {
                    return false;
                }
                commit(staged);
                for (RuntimePhase phase : phases) {
                    probe.phaseInvalidated(phase);
                }
                return true;
            } finally {
                stagedMutations--;
            }
        }

        /// Executes the structural declaration into a private fragment draft.
        ///
        /// @return the staged fragments
        private StagedFragments<K, V> stage() {
            FragmentScope<K, V> writer = new FragmentScope<>(this);
            try {
                probe.callbackExecuted(RuntimeCallbackKind.STRUCTURE);
                declaration.declare(writer);
                return writer.finish();
            } catch (HybridMutationException failure) {
                writer.abortNewFragments();
                throw failure;
            } catch (RuntimeException | Error failure) {
                writer.abortNewFragments();
                throw new HybridMutationException("structure-callback-failed", path, failure);
            }
        }

        /// Creates one new unregistered fragment owner and payload.
        ///
        /// @param key the semantic key
        /// @param retention the omission policy
        /// @param initializer the owner initializer
        /// @return the staged fragment
        private Fragment<K, V> createFragment(
                K key,
                Retention retention,
                FragmentInitializer<V> initializer
        ) {
            Owner owner = new Owner(path + '[' + key.toString() + ']');
            try {
                PayloadBox<V> box = new PayloadBox<>();
                initialize(owner, candidate -> box.value = Objects.requireNonNull(
                        initializer.initialize(candidate),
                        "fragment payload"
                ));
                @Nullable V payload = box.value;
                if (payload == null) {
                    throw new NullPointerException("Fragment payload must not be null");
                }
                return new Fragment<>(key, retention, owner, payload);
            } catch (HybridMutationException failure) {
                abortOwner(owner);
                throw failure;
            } catch (RuntimeException | Error failure) {
                abortOwner(owner);
                throw new HybridMutationException("fragment-initialization-failed", owner.path, failure);
            }
        }

        /// Commits one fully staged fragment set while preserving retained identities.
        ///
        /// @param staged the staged fragments
        private void commit(StagedFragments<K, V> staged) {
            LinkedHashMap<K, Fragment<K, V>> nextDormant = new LinkedHashMap<>(dormantFragments);
            for (K key : staged.fragments.keySet()) {
                nextDormant.remove(key);
            }
            for (Fragment<K, V> previous : orderedFragments) {
                if (!staged.fragments.containsKey(previous.key)) {
                    if (previous.retention == Retention.RETAIN) {
                        deactivateOwner(previous.owner);
                        nextDormant.put(previous.key, previous);
                    } else {
                        releaseFragment(previous);
                    }
                }
            }
            for (Fragment<K, V> fragment : staged.ordered) {
                if (!fragment.registered) {
                    registerFragment(fragment, true);
                } else if (!fragment.owner.active) {
                    activateOwner(fragment.owner);
                }
            }
            activeFragments = staged.fragments;
            orderedFragments = staged.ordered;
            dormantFragments = nextDormant;
        }

        /// Registers one semantic fragment and its owner subtree.
        ///
        /// @param fragment the fragment
        /// @param fragmentActive whether its owner starts active
        private void registerFragment(Fragment<K, V> fragment, boolean fragmentActive) {
            if (fragment.registered) {
                throw new IllegalStateException("Fragment is already registered: " + fragment.owner.path);
            }
            probe.retained(fragment, FRAGMENT_BYTES);
            fragment.registered = true;
            registerOwner(fragment.owner, fragmentActive);
        }

        /// Releases one registered semantic fragment and owner subtree.
        ///
        /// @param fragment the fragment
        private void releaseFragment(Fragment<K, V> fragment) {
            if (!fragment.registered) {
                return;
            }
            releaseOwner(fragment.owner);
            probe.released(fragment);
            fragment.registered = false;
        }

        /// Deactivates the scope edge and every active fragment owner.
        private void deactivate() {
            if (!active) {
                return;
            }
            for (int index = orderedFragments.size() - 1; index >= 0; index--) {
                deactivateOwner(orderedFragments.get(index).owner);
            }
            probe.dependencyDetached(this);
            active = false;
        }

        /// Reactivates the scope edge and its committed active fragment owners.
        private void activate() {
            if (active) {
                return;
            }
            observedVersion = source.version();
            probe.dependencyAttached(this);
            active = true;
            for (Fragment<K, V> fragment : orderedFragments) {
                activateOwner(fragment.owner);
            }
        }

        /// Releases every active and dormant fragment plus the scope record.
        private void release() {
            deactivate();
            for (int index = orderedFragments.size() - 1; index >= 0; index--) {
                releaseFragment(orderedFragments.get(index));
            }
            ArrayList<Fragment<K, V>> dormant = new ArrayList<>(dormantFragments.values());
            for (int index = dormant.size() - 1; index >= 0; index--) {
                releaseFragment(dormant.get(index));
            }
            activeFragments.clear();
            orderedFragments.clear();
            dormantFragments.clear();
            structuralScopes.remove(this);
            probe.released(this);
            registered = false;
        }

        /// Returns active fragment owners in mounted order.
        ///
        /// @return the immutable owner list
        private @Unmodifiable List<Owner> activeOwners() {
            return orderedFragments.stream().map(fragment -> fragment.owner).toList();
        }

        /// Aborts every unregistered initial fragment after an enclosing mount failure.
        private void abortStagedChildren() {
            for (Fragment<K, V> fragment : orderedFragments) {
                if (!fragment.registered) {
                    abortOwner(fragment.owner);
                }
            }
        }

        /// Compares two fragment lists by stable record identity and order.
        ///
        /// @param first the first list
        /// @param second the second list
        /// @return whether both lists contain the same fragment records in the same order
        private boolean sameOrder(List<Fragment<K, V>> first, List<Fragment<K, V>> second) {
            if (first.size() != second.size()) {
                return false;
            }
            for (int index = 0; index < first.size(); index++) {
                if (first.get(index) != second.get(index)) {
                    return false;
                }
            }
            return true;
        }
    }

    /// Stores one fine-grained producer-to-property callback.
    @NotNullByDefault
    private final class Binding {
        /// The owner of this binding.
        private final Owner owner;

        /// The versioned producer.
        private final StateSource source;

        /// The stable read-site identity.
        private final String site;

        /// The deferred property update callback.
        private final Runnable update;

        /// The affected phases.
        private final @Unmodifiable List<RuntimePhase> phases;

        /// The last applied semantic source version.
        private long observedVersion;

        /// Whether this binding record is registered.
        private boolean registered;

        /// Whether its dependency edge is active.
        private boolean active;

        /// Creates one staged property binding.
        ///
        /// @param owner the owner
        /// @param source the producer
        /// @param site the read site
        /// @param update the property callback
        /// @param phases the phase impacts
        private Binding(
                Owner owner,
                StateSource source,
                String site,
                Runnable update,
                @Unmodifiable List<RuntimePhase> phases
        ) {
            this.owner = owner;
            this.source = source;
            this.site = site;
            this.update = update;
            this.phases = List.copyOf(phases);
        }

        /// Activates the edge and initializes the property from the current producer value.
        private void activate() {
            if (active) {
                return;
            }
            observedVersion = source.version();
            probe.dependencyAttached(this);
            active = true;
            execute();
        }

        /// Applies a changed producer and invalidates its declared phases.
        private void poll() {
            long version = source.version();
            if (version == observedVersion) {
                return;
            }
            execute();
            observedVersion = version;
            for (RuntimePhase phase : phases) {
                probe.phaseInvalidated(phase);
            }
        }

        /// Detaches the active dependency edge.
        private void deactivate() {
            if (active) {
                probe.dependencyDetached(this);
                active = false;
            }
        }

        /// Executes the deferred application property callback.
        private void execute() {
            if (!owner.active) {
                throw new IllegalStateException("Inactive owner binding executed: " + owner.path + '/' + site);
            }
            probe.callbackExecuted(RuntimeCallbackKind.BINDING);
            probe.nodesVisited(1L);
            update.run();
        }
    }

    /// Stores one owner-scoped effect descriptor.
    @NotNullByDefault
    private final class Effect {
        /// The activation callback.
        private final Runnable mount;

        /// The deactivation callback.
        private final Runnable cleanup;

        /// Whether the descriptor is registered.
        private boolean registered;

        /// Whether the effect is currently mounted.
        private boolean active;

        /// Creates one staged effect.
        ///
        /// @param mount the mount callback
        /// @param cleanup the cleanup callback
        private Effect(Runnable mount, Runnable cleanup) {
            this.mount = mount;
            this.cleanup = cleanup;
        }

        /// Mounts the effect once for an owner activation.
        private void activate() {
            if (active) {
                return;
            }
            probe.callbackExecuted(RuntimeCallbackKind.EFFECT);
            mount.run();
            active = true;
        }

        /// Cleans up the effect once for an owner deactivation.
        private void deactivate() {
            if (!active) {
                return;
            }
            probe.callbackExecuted(RuntimeCallbackKind.CLEANUP);
            cleanup.run();
            active = false;
        }
    }

    /// Stores one persistent semantic node record.
    @NotNullByDefault
    private static final class Node {
        /// The semantic identifier.
        private final String identifier;

        /// Whether the node record is registered.
        private boolean registered;

        /// Creates one staged node.
        ///
        /// @param identifier the identifier
        private Node(String identifier) {
            this.identifier = identifier;
        }
    }

    /// Stores one stable structural position and its small rerunnable scope.
    @NotNullByDefault
    private final class Anchor {
        /// The structural scope at this position.
        private final StructuralScope<?, ?> scope;

        /// Whether the anchor record is registered.
        private boolean registered;

        /// Creates one staged anchor.
        ///
        /// @param scope the structural scope
        private Anchor(StructuralScope<?, ?> scope) {
            this.scope = scope;
        }
    }

    /// Stores one semantic fragment identity, owner, policy, and application payload.
    ///
    /// @param <K> the key type
    /// @param <V> the payload type
    @NotNullByDefault
    private final class Fragment<K, V> {
        /// The semantic key.
        private final K key;

        /// The omission policy.
        private final Retention retention;

        /// The fragment owner.
        private final Owner owner;

        /// The application payload.
        private final V payload;

        /// Whether the fragment record is registered.
        private boolean registered;

        /// Creates one staged fragment.
        ///
        /// @param key the semantic key
        /// @param retention the omission policy
        /// @param owner the owner
        /// @param payload the payload
        private Fragment(K key, Retention retention, Owner owner, V payload) {
            this.key = Objects.requireNonNull(key, "key");
            this.retention = Objects.requireNonNull(retention, "retention");
            this.owner = Objects.requireNonNull(owner, "owner");
            this.payload = Objects.requireNonNull(payload, "payload");
        }
    }

    /// Stores one fully staged structural-scope result.
    ///
    /// @param <K> the key type
    /// @param <V> the payload type
    @NotNullByDefault
    private final class StagedFragments<K, V> {
        /// The next fragments by semantic key.
        private final LinkedHashMap<K, Fragment<K, V>> fragments;

        /// The next fragments in mounted order.
        private final ArrayList<Fragment<K, V>> ordered;

        /// The fragments initialized by this draft.
        private final ArrayList<Fragment<K, V>> newFragments;

        /// Creates one staged result.
        ///
        /// @param fragments the keyed fragments
        /// @param ordered the mounted order
        /// @param newFragments the newly initialized fragments
        private StagedFragments(
                LinkedHashMap<K, Fragment<K, V>> fragments,
                ArrayList<Fragment<K, V>> ordered,
                ArrayList<Fragment<K, V>> newFragments
        ) {
            this.fragments = Objects.requireNonNull(fragments, "fragments");
            this.ordered = Objects.requireNonNull(ordered, "ordered");
            this.newFragments = Objects.requireNonNull(newFragments, "newFragments");
        }
    }

    /// Temporarily captures one non-null fragment initializer payload.
    ///
    /// @param <V> the payload type
    @NotNullByDefault
    private static final class PayloadBox<V> {
        /// The captured value, or `null` before initialization returns.
        private @Nullable V value;

        /// Creates an empty box.
        private PayloadBox() {
        }
    }

    /// Reports one contained owner or structural-scope mutation failure.
    @NotNullByDefault
    static final class HybridMutationException extends RuntimeException {
        /// The serialization identifier for diagnostic transport compatibility.
        private static final long serialVersionUID = 1L;

        /// The stable diagnostic code.
        private final String code;

        /// The owner or structural-scope path at failure.
        private final String ownerPath;

        /// Creates one contained mutation failure.
        ///
        /// @param code the stable code
        /// @param ownerPath the owner path
        /// @param cause the underlying failure, or `null`
        private HybridMutationException(String code, String ownerPath, @Nullable Throwable cause) {
            super(code, cause);
            this.code = code;
            this.ownerPath = ownerPath;
        }

        /// Returns the stable diagnostic code.
        ///
        /// @return the code
        String code() {
            return code;
        }

        /// Returns the owner or scope path.
        ///
        /// @return the path
        String ownerPath() {
            return ownerPath;
        }
    }
}
