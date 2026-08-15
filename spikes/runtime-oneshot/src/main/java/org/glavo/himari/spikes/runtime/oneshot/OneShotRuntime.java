package org.glavo.himari.spikes.runtime.oneshot;

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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/// Implements the one-shot owner and explicit reactive-control model evaluated in M1.
///
/// An [Owner] initializer executes exactly once for each owner instance. Fine-grained [Binding]
/// callbacks poll semantic producer versions without rerunning that initializer. Dynamic topology
/// is confined to explicit [Show] and [KeyedItems] controllers, each of which owns a stable anchor
/// and stages new owners before changing the committed tree.
@NotNullByDefault
final class OneShotRuntime implements AutoCloseable {
    /// Deterministic shallow-byte estimate for an owner.
    private static final long OWNER_BYTES = 80L;

    /// Deterministic shallow-byte estimate for a mounted-node record.
    private static final long NODE_BYTES = 48L;

    /// Deterministic shallow-byte estimate for a local integer cell.
    private static final long LOCAL_BYTES = 24L;

    /// Deterministic shallow-byte estimate for a binding record.
    private static final long BINDING_BYTES = 64L;

    /// Deterministic shallow-byte estimate for an effect record.
    private static final long EFFECT_BYTES = 48L;

    /// Deterministic shallow-byte estimate for a structural anchor.
    private static final long ANCHOR_BYTES = 40L;

    /// Deterministic shallow-byte estimate for a structural controller.
    private static final long CONTROLLER_BYTES = 72L;

    /// The shared comparison instrumentation sink.
    private final ComparisonProbe probe;

    /// Every registered fine-grained binding, including bindings in retained inactive owners.
    private final ArrayList<Binding> bindings = new ArrayList<>();

    /// Every registered structural controller, including controllers in retained inactive owners.
    private final ArrayList<StructuralController> controllers = new ArrayList<>();

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

    /// Creates an unmounted one-shot runtime.
    ///
    /// @param probe the shared instrumentation sink
    OneShotRuntime(ComparisonProbe probe) {
        this.probe = Objects.requireNonNull(probe, "probe");
    }

    /// Executes the root initializer once and atomically publishes its owner tree.
    ///
    /// @param initializer the root initializer
    /// @throws IllegalStateException if the runtime is already mounted or closed
    /// @throws OneShotMutationException if initialization rejects its staged tree
    void mount(Initializer initializer) {
        checkOpen();
        if (root != null) {
            throw new IllegalStateException("One-shot runtime is already mounted");
        }
        Owner stagedRoot = new Owner("root");
        stagedMutations++;
        try {
            initialize(stagedRoot, Objects.requireNonNull(initializer, "initializer"));
            registerOwner(stagedRoot, true);
            root = stagedRoot;
            refreshMountedNodes();
        } catch (OneShotMutationException failure) {
            abortOwner(stagedRoot);
            trace(failure);
            throw failure;
        } catch (RuntimeException | Error failure) {
            abortOwner(stagedRoot);
            OneShotMutationException wrapped = new OneShotMutationException(
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

    /// Polls explicit structural controllers, then changed fine-grained bindings.
    ///
    /// Component initializers do not execute during this method.
    ///
    /// @throws IllegalStateException if the runtime is unmounted or closed
    /// @throws OneShotMutationException if a staged structural update fails
    void flush() {
        Owner currentRoot = requireRoot();
        boolean structureChanged = false;
        try {
            for (int index = 0; index < controllers.size(); index++) {
                StructuralController controller = controllers.get(index);
                if (controller.active && controller.poll()) {
                    structureChanged = true;
                }
            }
            for (int index = 0; index < bindings.size(); index++) {
                Binding binding = bindings.get(index);
                if (binding.active) {
                    binding.poll();
                }
            }
        } catch (OneShotMutationException failure) {
            trace(failure);
            throw failure;
        } catch (RuntimeException | Error failure) {
            OneShotMutationException wrapped = new OneShotMutationException(
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

    /// Disposes every owner, controller, edge, effect, anchor, node, and local cell.
    ///
    /// Closure is idempotent.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (stagedMutations != 0L) {
            throw new IllegalStateException("One-shot runtime cannot close during a staged mutation");
        }
        @Nullable Owner currentRoot = root;
        if (currentRoot != null) {
            releaseOwner(currentRoot);
        }
        root = null;
        mountedNodes = List.of();
        bindings.clear();
        controllers.clear();
        closed = true;
    }

    /// Executes one owner initializer exactly once.
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
                anchor.controller.register(active);
            } else {
                throw new IllegalStateException("Unknown one-shot owner entry");
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

    /// Activates bindings and effects owned directly by one registered owner.
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

    /// Deactivates one retained owner subtree child-first without releasing its records.
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
                anchor.controller.deactivate();
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
                anchor.controller.activate();
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
                anchor.controller.release();
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
                anchor.controller.abortStagedChildren();
            }
        }
        for (Runnable cleanup : owner.abortCleanups) {
            cleanup.run();
        }
    }

    /// Rebuilds the semantic node list from active owners and anchors.
    private void refreshMountedNodes() {
        Owner currentRoot = requireRootOrStaged();
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
                for (Owner anchored : anchor.controller.activeOwners()) {
                    flatten(anchored, nodes);
                }
            }
        }
    }

    /// Emits a deterministic structured trace for one rejected staged mutation.
    ///
    /// @param failure the contained failure
    private void trace(OneShotMutationException failure) {
        probe.trace(new DiagnosticTrace(
                failure.code(),
                "One-shot structural controller rejected its staged mutation",
                "oneshot-app",
                failure.ownerPath(),
                null,
                "discard-staged-owners-and-preserve-anchors"
        ));
    }

    /// Returns the mounted root owner.
    ///
    /// @return the root owner
    private Owner requireRoot() {
        checkOpen();
        @Nullable Owner currentRoot = root;
        if (currentRoot == null) {
            throw new IllegalStateException("One-shot runtime is not mounted");
        }
        return currentRoot;
    }

    /// Returns either the mounted root or the root currently being registered.
    ///
    /// @return the available root owner
    private Owner requireRootOrStaged() {
        @Nullable Owner currentRoot = root;
        if (currentRoot != null) {
            return currentRoot;
        }
        throw new IllegalStateException("One-shot root is unavailable");
    }

    /// Verifies that the runtime remains open.
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("One-shot runtime is closed");
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

    /// Defines an initializer that executes once for one owner instance.
    @FunctionalInterface
    @NotNullByDefault
    interface Initializer {
        /// Initializes one new owner.
        ///
        /// @param owner the new owner
        void initialize(Owner owner);
    }

    /// Creates an application payload while initializing one keyed item owner.
    ///
    /// @param <K> the semantic key type
    /// @param <V> the item payload type
    @FunctionalInterface
    @NotNullByDefault
    interface ItemInitializer<K, V> {
        /// Initializes a new keyed owner and returns its application payload.
        ///
        /// @param owner the new item owner
        /// @param key the semantic key
        /// @return the payload
        V initialize(Owner owner, K key);
    }

    /// Creates an application payload while initializing one conditional owner.
    ///
    /// @param <V> the branch payload type
    @FunctionalInterface
    @NotNullByDefault
    interface BranchInitializer<V> {
        /// Initializes a new branch owner and returns its payload.
        ///
        /// @param owner the new branch owner
        /// @return the payload
        V initialize(Owner owner);
    }

    /// Selects the hidden lifetime of a conditional owner.
    @NotNullByDefault
    enum Retention {
        /// Keep owner-local state but deactivate bindings and effects while hidden.
        RETAIN,

        /// Dispose the owner and recreate it if the branch becomes visible again.
        DISPOSE
    }

    /// Owns one component instance, its declared entries, fine-grained bindings, and effects.
    @NotNullByDefault
    final class Owner {
        /// The stable diagnostic owner path.
        private final String path;

        /// Nodes, component owners, and structural anchors in semantic declaration order.
        private final ArrayList<Object> entries = new ArrayList<>();

        /// Direct owner-local integer cells.
        private final ArrayList<LocalInt> locals = new ArrayList<>();

        /// Direct fine-grained property bindings.
        private final ArrayList<Binding> bindings = new ArrayList<>();

        /// Direct effect descriptors.
        private final ArrayList<Effect> effects = new ArrayList<>();

        /// Cleanups for resources created only by a staged owner.
        private final ArrayList<Runnable> abortCleanups = new ArrayList<>();

        /// Callbacks that expose staged payloads only after commit.
        private final ArrayList<Runnable> afterCommit = new ArrayList<>();

        /// Whether the initializer has executed.
        private boolean initialized;

        /// Whether this owner is registered with the runtime.
        private boolean registered;

        /// Whether this owner participates in the mounted tree.
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
            OneShotRuntime.this.initialize(child, Objects.requireNonNull(initializer, "initializer"));
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

        /// Declares an explicit conditional controller at a stable anchor.
        ///
        /// @param key the anchor identity
        /// @param source the versioned condition producer
        /// @param condition the deferred Boolean expression
        /// @param retention the hidden owner policy
        /// @param initializer the branch initializer
        /// @param phases the phases affected by visibility changes
        /// @param <V> the branch payload type
        /// @return the controller
        <V> Show<V> show(
                String key,
                StateSource source,
                BooleanSupplier condition,
                Retention retention,
                BranchInitializer<V> initializer,
                RuntimePhase... phases
        ) {
            checkMutable();
            Show<V> controller = new Show<>(
                    this,
                    Objects.requireNonNull(key, "key"),
                    Objects.requireNonNull(source, "source"),
                    Objects.requireNonNull(condition, "condition"),
                    Objects.requireNonNull(retention, "retention"),
                    Objects.requireNonNull(initializer, "initializer"),
                    phaseList(phases)
            );
            entries.add(new Anchor(controller));
            return controller;
        }

        /// Declares an explicit semantic-keyed collection controller at a stable anchor.
        ///
        /// @param anchorKey the anchor identity
        /// @param source the versioned key-list producer
        /// @param keys the deferred immutable key list
        /// @param initializer the new-item initializer
        /// @param phases the phases affected by topology changes
        /// @param <K> the key type
        /// @param <V> the item payload type
        /// @return the controller
        <K, V> KeyedItems<K, V> forEach(
                String anchorKey,
                StateSource source,
                Supplier<@Unmodifiable List<K>> keys,
                ItemInitializer<K, V> initializer,
                RuntimePhase... phases
        ) {
            checkMutable();
            KeyedItems<K, V> controller = new KeyedItems<>(
                    this,
                    Objects.requireNonNull(anchorKey, "anchorKey"),
                    Objects.requireNonNull(source, "source"),
                    Objects.requireNonNull(keys, "keys"),
                    Objects.requireNonNull(initializer, "initializer"),
                    phaseList(phases)
            );
            entries.add(new Anchor(controller));
            return controller;
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
            throw new OneShotMutationException(
                    Objects.requireNonNull(code, "code"),
                    path,
                    null
            );
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

    /// Controls one explicit conditional owner at a stable anchor.
    ///
    /// @param <V> the application payload type
    @NotNullByDefault
    final class Show<V> extends StructuralController {
        /// The deferred condition.
        private final BooleanSupplier condition;

        /// The hidden owner lifetime policy.
        private final Retention retention;

        /// The one-shot branch initializer.
        private final BranchInitializer<V> initializer;

        /// The retained or active branch owner, or `null` before first visibility or after disposal.
        private @Nullable Owner branchOwner;

        /// The retained or active payload, or `null` when no owner exists.
        private @Nullable V payload;

        /// Whether the branch currently participates in mounted structure.
        private boolean visible;

        /// Creates one staged conditional controller and its initially visible owner if needed.
        ///
        /// @param parent the anchor owner
        /// @param key the anchor key
        /// @param source the condition source
        /// @param condition the deferred condition
        /// @param retention the retention policy
        /// @param initializer the branch initializer
        /// @param phases the structural phases
        private Show(
                Owner parent,
                String key,
                StateSource source,
                BooleanSupplier condition,
                Retention retention,
                BranchInitializer<V> initializer,
                @Unmodifiable List<RuntimePhase> phases
        ) {
            super(parent, key, source, phases);
            this.condition = condition;
            this.retention = retention;
            this.initializer = initializer;
            this.visible = condition.getAsBoolean();
            if (visible) {
                BranchRecord<V> branch = stageBranch();
                branchOwner = branch.owner();
                payload = branch.payload();
            }
        }

        /// Returns whether the branch is committed and visible.
        ///
        /// @return whether the branch is visible
        boolean visible() {
            return visible;
        }

        /// Returns the branch payload, which remains available while a retained branch is hidden.
        ///
        /// @return the payload, or `null` before creation or after disposal
        @Nullable V value() {
            return payload;
        }

        /// Reconciles condition state without rerunning a retained owner initializer.
        ///
        /// @return whether mounted structure changed
        @Override
        boolean reconcile() {
            boolean nextVisible = condition.getAsBoolean();
            if (nextVisible == visible) {
                return false;
            }
            stagedMutations++;
            try {
                probe.callbackExecuted(RuntimeCallbackKind.STRUCTURE);
                if (nextVisible) {
                    @Nullable Owner retainedOwner = branchOwner;
                    if (retainedOwner == null) {
                        BranchRecord<V> branch = stageBranch();
                        registerOwner(branch.owner(), true);
                        branchOwner = branch.owner();
                        payload = branch.payload();
                    } else {
                        activateOwner(retainedOwner);
                    }
                } else {
                    Owner current = requireBranchOwner();
                    if (retention == Retention.RETAIN) {
                        deactivateOwner(current);
                    } else {
                        releaseOwner(current);
                        branchOwner = null;
                        payload = null;
                    }
                }
                visible = nextVisible;
                invalidatePhases();
                return true;
            } finally {
                stagedMutations--;
            }
        }

        /// Registers this controller and its initial branch owner.
        ///
        /// @param parentActive whether the anchor owner is active
        @Override
        void registerChildren(boolean parentActive) {
            @Nullable Owner child = branchOwner;
            if (child != null) {
                registerOwner(child, parentActive && visible);
            }
        }

        /// Returns the currently active branch owner.
        ///
        /// @return zero or one active owner
        @Override
        @Unmodifiable List<Owner> activeOwners() {
            @Nullable Owner child = branchOwner;
            return visible && child != null ? List.of(child) : List.of();
        }

        /// Deactivates the visible branch with its parent owner.
        @Override
        void deactivateChildren() {
            @Nullable Owner child = branchOwner;
            if (visible && child != null) {
                deactivateOwner(child);
            }
        }

        /// Reactivates the visible branch without rerunning initialization.
        @Override
        void activateChildren() {
            @Nullable Owner child = branchOwner;
            if (visible && child != null) {
                activateOwner(child);
            }
        }

        /// Releases the retained or active branch owner.
        @Override
        void releaseChildren() {
            @Nullable Owner child = branchOwner;
            if (child != null) {
                releaseOwner(child);
                branchOwner = null;
                payload = null;
            }
        }

        /// Aborts an initially staged branch when its parent mount fails.
        @Override
        void abortStagedChildren() {
            @Nullable Owner child = branchOwner;
            if (child != null && !child.registered) {
                abortOwner(child);
            }
        }

        /// Creates one unregistered branch owner and payload.
        ///
        /// @return the staged branch
        private BranchRecord<V> stageBranch() {
            Owner owner = new Owner(path + "/branch");
            try {
                PayloadBox<V> box = new PayloadBox<>();
                initialize(owner, candidate -> box.value = Objects.requireNonNull(
                        initializer.initialize(candidate),
                        "branch payload"
                ));
                @Nullable V stagedPayload = box.value;
                if (stagedPayload == null) {
                    throw new NullPointerException("Branch payload must not be null");
                }
                return new BranchRecord<>(owner, stagedPayload);
            } catch (OneShotMutationException failure) {
                abortOwner(owner);
                throw failure;
            } catch (RuntimeException | Error failure) {
                abortOwner(owner);
                throw new OneShotMutationException("branch-initialization-failed", owner.path, failure);
            }
        }

        /// Returns the required branch owner.
        ///
        /// @return the owner
        private Owner requireBranchOwner() {
            @Nullable Owner owner = branchOwner;
            if (owner == null) {
                throw new IllegalStateException("Visible branch has no owner: " + path);
            }
            return owner;
        }
    }

    /// Controls a semantic-keyed owner collection at one stable anchor.
    ///
    /// @param <K> the key type
    /// @param <V> the item payload type
    @NotNullByDefault
    final class KeyedItems<K, V> extends StructuralController {
        /// The deferred key-list read.
        private final Supplier<@Unmodifiable List<K>> keys;

        /// The one-shot item initializer.
        private final ItemInitializer<K, V> initializer;

        /// Committed items by semantic key.
        private LinkedHashMap<K, ItemRecord<K, V>> items = new LinkedHashMap<>();

        /// Committed item records in semantic order.
        private ArrayList<ItemRecord<K, V>> ordered = new ArrayList<>();

        /// Creates one staged collection controller and its initial item owners.
        ///
        /// @param parent the anchor owner
        /// @param key the anchor key
        /// @param source the key-list producer
        /// @param keys the deferred key list
        /// @param initializer the item initializer
        /// @param phases the structural phases
        private KeyedItems(
                Owner parent,
                String key,
                StateSource source,
                Supplier<@Unmodifiable List<K>> keys,
                ItemInitializer<K, V> initializer,
                @Unmodifiable List<RuntimePhase> phases
        ) {
            super(parent, key, source, phases);
            this.keys = keys;
            this.initializer = initializer;
            StagedItems<K, V> initial = stage(keys.get(), Map.of());
            items = initial.items();
            ordered = initial.ordered();
        }

        /// Returns committed semantic keys in mounted order.
        ///
        /// @return the immutable keys
        @Unmodifiable List<K> keys() {
            return ordered.stream().map(ItemRecord::key).toList();
        }

        /// Returns one committed item payload.
        ///
        /// @param key the semantic key
        /// @return the payload, or `null` when absent
        @Nullable V value(K key) {
            @Nullable ItemRecord<K, V> item = items.get(Objects.requireNonNull(key, "key"));
            return item == null ? null : item.payload();
        }

        /// Reconciles keys atomically while preserving surviving owner instances.
        ///
        /// @return whether mounted structure changed
        @Override
        boolean reconcile() {
            stagedMutations++;
            try {
                probe.callbackExecuted(RuntimeCallbackKind.STRUCTURE);
                StagedItems<K, V> next = stage(keys.get(), items);
                boolean changed = !sameOrder(ordered, next.ordered());
                if (!changed) {
                    return false;
                }
                HashSet<K> nextKeys = new HashSet<>(next.items().keySet());
                for (ItemRecord<K, V> previous : ordered) {
                    if (!nextKeys.contains(previous.key())) {
                        releaseOwner(previous.owner());
                    }
                }
                for (ItemRecord<K, V> item : next.newItems()) {
                    registerOwner(item.owner(), true);
                }
                items = next.items();
                ordered = next.ordered();
                invalidatePhases();
                return true;
            } finally {
                stagedMutations--;
            }
        }

        /// Registers initial item-owner subtrees.
        ///
        /// @param parentActive whether the anchor owner is active
        @Override
        void registerChildren(boolean parentActive) {
            for (ItemRecord<K, V> item : ordered) {
                registerOwner(item.owner(), parentActive);
            }
        }

        /// Returns active item owners in semantic order.
        ///
        /// @return the immutable owner order
        @Override
        @Unmodifiable List<Owner> activeOwners() {
            return ordered.stream().map(ItemRecord::owner).toList();
        }

        /// Deactivates every item owner child-first.
        @Override
        void deactivateChildren() {
            for (int index = ordered.size() - 1; index >= 0; index--) {
                deactivateOwner(ordered.get(index).owner());
            }
        }

        /// Reactivates every item owner in semantic order.
        @Override
        void activateChildren() {
            for (ItemRecord<K, V> item : ordered) {
                activateOwner(item.owner());
            }
        }

        /// Releases every item owner.
        @Override
        void releaseChildren() {
            for (int index = ordered.size() - 1; index >= 0; index--) {
                releaseOwner(ordered.get(index).owner());
            }
            ordered.clear();
            items.clear();
        }

        /// Aborts every unregistered initial item owner.
        @Override
        void abortStagedChildren() {
            for (ItemRecord<K, V> item : ordered) {
                if (!item.owner().registered) {
                    abortOwner(item.owner());
                }
            }
        }

        /// Builds an uncommitted collection using prior records for surviving keys.
        ///
        /// @param requestedKeys the requested keys
        /// @param previous the prior records
        /// @return the staged collection
        private StagedItems<K, V> stage(
                @Unmodifiable List<K> requestedKeys,
                Map<K, ItemRecord<K, V>> previous
        ) {
            Objects.requireNonNull(requestedKeys, "requestedKeys");
            HashSet<K> uniqueKeys = new HashSet<>();
            for (K key : requestedKeys) {
                Objects.requireNonNull(key, "item key");
                if (!uniqueKeys.add(key)) {
                    throw new OneShotMutationException("duplicate-key", path + '[' + key.toString() + ']', null);
                }
            }

            LinkedHashMap<K, ItemRecord<K, V>> nextItems = new LinkedHashMap<>();
            ArrayList<ItemRecord<K, V>> newItems = new ArrayList<>();
            try {
                for (K key : requestedKeys) {
                    @Nullable ItemRecord<K, V> existing = previous.get(key);
                    if (existing != null) {
                        nextItems.put(key, existing);
                    } else {
                        ItemRecord<K, V> created = stageItem(key);
                        nextItems.put(key, created);
                        newItems.add(created);
                    }
                }
            } catch (RuntimeException | Error failure) {
                for (int index = newItems.size() - 1; index >= 0; index--) {
                    abortOwner(newItems.get(index).owner());
                }
                throw failure;
            }
            ArrayList<ItemRecord<K, V>> nextOrder = new ArrayList<>(nextItems.values());
            return new StagedItems<>(nextItems, nextOrder, newItems);
        }

        /// Initializes one staged item owner and payload.
        ///
        /// @param key the item key
        /// @return the staged item
        private ItemRecord<K, V> stageItem(K key) {
            Owner owner = new Owner(path + '[' + key.toString() + ']');
            try {
                PayloadBox<V> box = new PayloadBox<>();
                initialize(owner, candidate -> box.value = Objects.requireNonNull(
                        initializer.initialize(candidate, key),
                        "item payload"
                ));
                @Nullable V payload = box.value;
                if (payload == null) {
                    throw new NullPointerException("Item payload must not be null");
                }
                return new ItemRecord<>(key, owner, payload);
            } catch (OneShotMutationException failure) {
                abortOwner(owner);
                throw failure;
            } catch (RuntimeException | Error failure) {
                abortOwner(owner);
                throw new OneShotMutationException("item-initialization-failed", owner.path, failure);
            }
        }

        /// Compares two item-record lists by key order.
        ///
        /// @param first the first order
        /// @param second the second order
        /// @return whether key order is identical
        private boolean sameOrder(List<ItemRecord<K, V>> first, List<ItemRecord<K, V>> second) {
            if (first.size() != second.size()) {
                return false;
            }
            for (int index = 0; index < first.size(); index++) {
                if (!first.get(index).key().equals(second.get(index).key())) {
                    return false;
                }
            }
            return true;
        }
    }

    /// Stores common source-version and lifecycle behavior for an explicit structural controller.
    @NotNullByDefault
    abstract class StructuralController {
        /// The anchor owner.
        final Owner parent;

        /// The stable diagnostic controller path.
        final String path;

        /// The producer that invalidates this controller.
        final StateSource source;

        /// The phases invalidated by committed topology changes.
        final @Unmodifiable List<RuntimePhase> phases;

        /// The source version incorporated by the last successful reconciliation.
        long observedVersion;

        /// Whether the controller record is registered.
        boolean registered;

        /// Whether its dependency edge and owned children are active.
        boolean active;

        /// Creates one staged controller.
        ///
        /// @param parent the anchor owner
        /// @param key the anchor key
        /// @param source the invalidating producer
        /// @param phases the topology phases
        StructuralController(
                Owner parent,
                String key,
                StateSource source,
                @Unmodifiable List<RuntimePhase> phases
        ) {
            this.parent = parent;
            this.path = parent.path + "/anchor:" + key;
            this.source = source;
            this.phases = List.copyOf(phases);
            this.observedVersion = source.version();
        }

        /// Registers the controller, dependency edge, and staged children.
        ///
        /// @param parentActive whether the anchor owner is active
        final void register(boolean parentActive) {
            probe.retained(this, CONTROLLER_BYTES);
            registered = true;
            controllers.add(this);
            registerChildren(parentActive);
            if (parentActive) {
                active = true;
                probe.dependencyAttached(this);
            }
        }

        /// Polls the source and reconciles only after a semantic version change.
        ///
        /// @return whether mounted structure changed
        final boolean poll() {
            long version = source.version();
            if (version == observedVersion) {
                return false;
            }
            boolean changed = reconcile();
            observedVersion = version;
            return changed;
        }

        /// Deactivates the controller edge and child owners.
        final void deactivate() {
            if (!active) {
                return;
            }
            deactivateChildren();
            probe.dependencyDetached(this);
            active = false;
        }

        /// Reactivates the controller edge and currently committed children.
        final void activate() {
            if (active) {
                return;
            }
            observedVersion = source.version();
            probe.dependencyAttached(this);
            active = true;
            activateChildren();
        }

        /// Releases controller children, edge state, and retained registration.
        final void release() {
            deactivate();
            releaseChildren();
            controllers.remove(this);
            probe.released(this);
            registered = false;
        }

        /// Invalidates every phase declared for a committed topology change.
        final void invalidatePhases() {
            for (RuntimePhase phase : phases) {
                probe.phaseInvalidated(phase);
            }
        }

        /// Reconciles a changed source version.
        ///
        /// @return whether mounted structure changed
        abstract boolean reconcile();

        /// Registers initially staged child owners.
        ///
        /// @param parentActive whether the anchor owner is active
        abstract void registerChildren(boolean parentActive);

        /// Returns currently active owners in semantic order.
        ///
        /// @return the immutable owner list
        abstract @Unmodifiable List<Owner> activeOwners();

        /// Deactivates owned children.
        abstract void deactivateChildren();

        /// Reactivates committed children.
        abstract void activateChildren();

        /// Permanently releases owned children.
        abstract void releaseChildren();

        /// Aborts unregistered children after an enclosing mount failure.
        abstract void abortStagedChildren();
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

        /// Creates one staged binding.
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

    /// Stores one stable structural position and its explicit controller.
    @NotNullByDefault
    private static final class Anchor {
        /// The structural controller at this position.
        private final StructuralController controller;

        /// Whether the anchor is registered.
        private boolean registered;

        /// Creates one staged anchor.
        ///
        /// @param controller the controller
        private Anchor(StructuralController controller) {
            this.controller = controller;
        }
    }

    /// Temporarily captures one non-null initializer payload.
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

    /// Stores one conditional owner and its application payload.
    ///
    /// @param owner the staged owner
    /// @param payload the non-null application payload
    /// @param <V> the payload type
    @NotNullByDefault
    private record BranchRecord<V>(Owner owner, V payload) {
        /// Creates a validated branch record.
        private BranchRecord {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(payload, "payload");
        }
    }

    /// Stores one keyed owner and its non-null application payload.
    ///
    /// @param key the semantic key
    /// @param owner the owner
    /// @param payload the payload
    /// @param <K> the key type
    /// @param <V> the payload type
    @NotNullByDefault
    private record ItemRecord<K, V>(K key, Owner owner, V payload) {
        /// Creates a validated item record.
        private ItemRecord {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(payload, "payload");
        }
    }

    /// Stores one fully staged keyed reconciliation result.
    ///
    /// @param items the next records by key
    /// @param ordered the next semantic order
    /// @param newItems the records that require registration
    /// @param <K> the key type
    /// @param <V> the payload type
    @NotNullByDefault
    private record StagedItems<K, V>(
            LinkedHashMap<K, ItemRecord<K, V>> items,
            ArrayList<ItemRecord<K, V>> ordered,
            ArrayList<ItemRecord<K, V>> newItems
    ) {
        /// Creates a staged result with non-null owned collections.
        private StagedItems {
            Objects.requireNonNull(items, "items");
            Objects.requireNonNull(ordered, "ordered");
            Objects.requireNonNull(newItems, "newItems");
        }
    }

    /// Reports one contained owner initialization or structural-control failure.
    @NotNullByDefault
    static final class OneShotMutationException extends RuntimeException {
        /// The serialization identifier for diagnostic transport compatibility.
        private static final long serialVersionUID = 1L;

        /// The stable diagnostic code.
        private final String code;

        /// The owner or anchor path at failure.
        private final String ownerPath;

        /// Creates one contained mutation failure.
        ///
        /// @param code the stable code
        /// @param ownerPath the owner path
        /// @param cause the underlying failure, or `null`
        private OneShotMutationException(String code, String ownerPath, @Nullable Throwable cause) {
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

        /// Returns the owner path.
        ///
        /// @return the owner path
        String ownerPath() {
            return ownerPath;
        }
    }
}
