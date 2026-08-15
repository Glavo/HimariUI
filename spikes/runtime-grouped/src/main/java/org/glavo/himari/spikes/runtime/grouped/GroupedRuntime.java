package org.glavo.himari.spikes.runtime.grouped;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Implements the explicit grouped-recomposition storage model evaluated by the M1 candidate.
///
/// The runtime has no generated source keys or restart metadata. Application code declares every
/// group explicitly. Unkeyed groups use their position inside the enclosing group, while keyed
/// groups reconcile by an application key. A composition attempt writes only a draft; topology,
/// effects, dependency edges, and positional memory become observable together at commit.
@NotNullByDefault
final class GroupedRuntime implements AutoCloseable {
    /// Deterministic shallow-byte estimate for one group record.
    private static final long GROUP_BYTES = 96L;

    /// Deterministic shallow-byte estimate for one positional-memory slot.
    private static final long MEMORY_BYTES = 32L;

    /// Deterministic shallow-byte estimate for one effect record.
    private static final long EFFECT_BYTES = 48L;

    /// The application callback rerun for each structural attempt.
    private final Composition composition;

    /// The shared instrumentation sink.
    private final ComparisonProbe probe;

    /// The stable root group.
    private final GroupNode root;

    /// The committed semantic node order.
    private @Unmodifiable List<String> mountedNodes = List.of();

    /// Whether one draft is currently being built.
    private boolean staging;

    /// Whether the runtime has been closed.
    private boolean closed;

    /// Creates an empty runtime and registers its stable root owner.
    ///
    /// @param composition the application structure callback
    /// @param probe the shared probe
    GroupedRuntime(Composition composition, ComparisonProbe probe) {
        this.composition = Objects.requireNonNull(composition, "composition");
        this.probe = Objects.requireNonNull(probe, "probe");
        this.root = new GroupNode("root", null);
        registerGroup(root);
    }

    /// Reruns application structure and atomically commits the resulting group draft.
    ///
    /// @throws GroupedCompositionException if the application declares an invalid or failed draft
    /// @throws IllegalStateException if called reentrantly or after closure
    void recompose() {
        checkOpen();
        if (staging) {
            throw new IllegalStateException("Grouped composition cannot be reentered");
        }
        staging = true;
        GroupDraft draft = new GroupDraft(root);
        Composer composer = new Composer(draft);
        try {
            probe.callbackExecuted(RuntimeCallbackKind.STRUCTURE);
            composition.compose(composer.scope());
            composer.checkBalanced();
            commit(draft);
        } catch (GroupedCompositionException failure) {
            abort(draft);
            trace(failure);
            throw failure;
        } catch (RuntimeException | Error failure) {
            abort(draft);
            GroupedCompositionException wrapped = new GroupedCompositionException(
                    "structure-callback-failed",
                    composer.ownerPath(),
                    failure
            );
            trace(wrapped);
            throw wrapped;
        } finally {
            staging = false;
        }
    }

    /// Returns the committed semantic node identifiers.
    ///
    /// @return the immutable mounted-node order
    @Unmodifiable List<String> mountedNodes() {
        checkOpen();
        return mountedNodes;
    }

    /// Returns live candidate-owned resource counts.
    ///
    /// @return the health snapshot
    RuntimeHealth health() {
        if (closed) {
            return RuntimeHealth.CLEAN;
        }
        ResourceCount count = new ResourceCount();
        count(root, count);
        return new RuntimeHealth(
                mountedNodes.size(),
                count.groups,
                count.effects,
                staging ? 1L : 0L,
                0L
        );
    }

    /// Disposes committed effects child-first, detaches every edge, and releases all registrations.
    ///
    /// Closure is idempotent.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (staging) {
            throw new IllegalStateException("Grouped runtime cannot close during composition");
        }
        releaseTree(root, true);
        mountedNodes = List.of();
        closed = true;
    }

    /// Commits one successfully built root draft.
    ///
    /// @param draft the root draft
    private void commit(GroupDraft draft) {
        commitGroup(draft);
        mountedNodes = List.copyOf(draft.nodes);
        for (Runnable callback : draft.afterCommit) {
            callback.run();
        }
    }

    /// Reconciles one group and all descendants.
    ///
    /// @param draft the group draft
    private void commitGroup(GroupDraft draft) {
        GroupNode group = draft.group;
        if (!group.registered) {
            registerGroup(group);
        }

        IdentityHashMap<GroupNode, Boolean> preserved = new IdentityHashMap<>();
        for (GroupDraft child : draft.activeChildren) {
            preserved.put(child.group, Boolean.TRUE);
        }
        for (GroupNode child : draft.dormantChildren) {
            preserved.put(child, Boolean.TRUE);
        }
        for (GroupNode child : group.activeChildren) {
            if (!preserved.containsKey(child)) {
                releaseTree(child, true);
            }
        }
        for (GroupNode child : group.dormantChildren) {
            if (!preserved.containsKey(child)) {
                releaseTree(child, true);
            }
        }

        IdentityHashMap<GroupNode, Boolean> active = new IdentityHashMap<>();
        ArrayList<GroupNode> committedChildren = new ArrayList<>(draft.activeChildren.size());
        for (GroupDraft child : draft.activeChildren) {
            active.put(child.group, Boolean.TRUE);
            commitGroup(child);
            committedChildren.add(child.group);
        }
        for (GroupNode dormant : draft.dormantChildren) {
            if (!containsIdentity(group.dormantChildren, dormant)) {
                deactivate(dormant);
            }
        }

        reconcileEdges(group, draft.edges);
        reconcileEffects(group, draft.effects);
        for (MemorySlot memory : group.memories) {
            if (!containsIdentity(draft.memories, memory)) {
                probe.released(memory);
            }
        }
        for (MemorySlot slot : draft.newMemories) {
            probe.retained(slot, MEMORY_BYTES);
        }
        group.activeChildren = committedChildren;
        group.dormantChildren = new ArrayList<>(draft.dormantChildren);
        group.memories = new ArrayList<>(draft.memories);
        group.edges = new LinkedHashMap<>(draft.edges);
    }

    /// Reconciles dependency edges and reports phase invalidations from changed producers.
    ///
    /// @param group the committed group
    /// @param next the next edge declarations
    private void reconcileEdges(GroupNode group, LinkedHashMap<EdgeKey, EdgeRecord> next) {
        for (Map.Entry<EdgeKey, EdgeRecord> entry : group.edges.entrySet()) {
            if (!next.containsKey(entry.getKey())) {
                probe.dependencyDetached(entry.getValue());
            }
        }
        for (Map.Entry<EdgeKey, EdgeRecord> entry : next.entrySet()) {
            @Nullable EdgeRecord previous = group.edges.get(entry.getKey());
            EdgeRecord current = entry.getValue();
            if (previous == null) {
                probe.dependencyAttached(current);
            } else {
                current.token = previous.token;
                if (current.version != previous.version) {
                    for (RuntimePhase phase : current.phases) {
                        probe.phaseInvalidated(phase);
                    }
                }
            }
        }
    }

    /// Reconciles effect declarations after the structure draft is known to be valid.
    ///
    /// @param group the committed group
    /// @param next the next effect declarations
    private void reconcileEffects(GroupNode group, LinkedHashMap<String, EffectRecord> next) {
        for (Map.Entry<String, EffectRecord> entry : group.effects.entrySet()) {
            if (!next.containsKey(entry.getKey())) {
                disposeEffect(entry.getValue());
            }
        }
        LinkedHashMap<String, EffectRecord> committed = new LinkedHashMap<>();
        for (Map.Entry<String, EffectRecord> entry : next.entrySet()) {
            @Nullable EffectRecord previous = group.effects.get(entry.getKey());
            EffectRecord effect = entry.getValue();
            if (previous == null) {
                probe.retained(effect, EFFECT_BYTES);
                probe.callbackExecuted(RuntimeCallbackKind.EFFECT);
                effect.mount.run();
                committed.put(entry.getKey(), effect);
            } else {
                previous.mount = effect.mount;
                previous.cleanup = effect.cleanup;
                committed.put(entry.getKey(), previous);
            }
        }
        group.effects = committed;
    }

    /// Deactivates a retained subtree without releasing positional memory or group identity.
    ///
    /// @param group the retained subtree root
    private void deactivate(GroupNode group) {
        for (GroupNode child : group.activeChildren) {
            deactivate(child);
        }
        disposeEffects(group);
        detachEdges(group);
    }

    /// Runs draft abort cleanups in declaration order.
    ///
    /// @param draft the failed root draft
    private static void abort(GroupDraft draft) {
        for (Runnable cleanup : draft.abortCleanups) {
            cleanup.run();
        }
    }

    /// Emits a structured trace for one failed composition attempt.
    ///
    /// @param failure the contained failure
    private void trace(GroupedCompositionException failure) {
        probe.trace(new DiagnosticTrace(
                failure.code(),
                "Grouped composition rejected the staged structure",
                "grouped-app",
                failure.ownerPath(),
                null,
                "discard-draft-and-preserve-committed-tree"
        ));
    }

    /// Releases one group subtree and optionally its root registration.
    ///
    /// @param group the subtree
    /// @param includeRoot whether to release the supplied group itself
    private void releaseTree(GroupNode group, boolean includeRoot) {
        for (int index = group.activeChildren.size() - 1; index >= 0; index--) {
            releaseTree(group.activeChildren.get(index), true);
        }
        for (int index = group.dormantChildren.size() - 1; index >= 0; index--) {
            releaseTree(group.dormantChildren.get(index), true);
        }
        disposeEffects(group);
        detachEdges(group);
        for (int index = group.memories.size() - 1; index >= 0; index--) {
            probe.released(group.memories.get(index));
        }
        group.activeChildren.clear();
        group.dormantChildren.clear();
        group.memories.clear();
        if (includeRoot && group.registered) {
            probe.released(group);
            group.registered = false;
        }
    }

    /// Disposes every active effect owned directly by a group.
    ///
    /// @param group the group
    private void disposeEffects(GroupNode group) {
        ArrayList<EffectRecord> effects = new ArrayList<>(group.effects.values());
        for (int index = effects.size() - 1; index >= 0; index--) {
            disposeEffect(effects.get(index));
        }
        group.effects.clear();
    }

    /// Runs and releases one effect cleanup.
    ///
    /// @param effect the effect
    private void disposeEffect(EffectRecord effect) {
        probe.callbackExecuted(RuntimeCallbackKind.CLEANUP);
        effect.cleanup.run();
        probe.released(effect);
    }

    /// Detaches every dependency edge owned directly by a group.
    ///
    /// @param group the group
    private void detachEdges(GroupNode group) {
        for (EdgeRecord edge : group.edges.values()) {
            probe.dependencyDetached(edge.token);
        }
        group.edges.clear();
    }

    /// Registers one group record with the comparison probe.
    ///
    /// @param group the group
    private void registerGroup(GroupNode group) {
        probe.retained(group, GROUP_BYTES);
        group.registered = true;
    }

    /// Accumulates live group and effect counts.
    ///
    /// @param group the subtree root
    /// @param count the mutable accumulator
    private static void count(GroupNode group, ResourceCount count) {
        count.groups++;
        count.effects += group.effects.size();
        for (GroupNode child : group.activeChildren) {
            count(child, count);
        }
        for (GroupNode child : group.dormantChildren) {
            count(child, count);
        }
    }

    /// Tests list membership with identity semantics.
    ///
    /// @param groups the group list
    /// @param candidate the candidate identity
    /// @return whether the exact object occurs
    private static boolean containsIdentity(List<GroupNode> groups, GroupNode candidate) {
        for (GroupNode group : groups) {
            if (group == candidate) {
                return true;
            }
        }
        return false;
    }

    /// Tests memory-list membership with identity semantics.
    ///
    /// @param memories the memory list
    /// @param candidate the candidate identity
    /// @return whether the exact object occurs
    private static boolean containsIdentity(List<MemorySlot> memories, MemorySlot candidate) {
        for (MemorySlot memory : memories) {
            if (memory == candidate) {
                return true;
            }
        }
        return false;
    }

    /// Verifies that the runtime remains open.
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Grouped runtime is closed");
        }
    }

    /// Supplies one application structure declaration.
    @FunctionalInterface
    @NotNullByDefault
    interface Composition {
        /// Declares application structure through one explicit scope.
        ///
        /// @param scope the current root scope
        void compose(Scope scope);
    }

    /// Exposes the explicit ordinary-Java grouped API to candidate applications.
    @NotNullByDefault
    final class Scope {
        /// The composer whose current group this scope represents.
        private final Composer composer;

        /// Creates a scope facade.
        ///
        /// @param composer the active composer
        private Scope(Composer composer) {
            this.composer = composer;
        }

        /// Declares an unkeyed group whose memory identity is positional within its parent.
        ///
        /// @param sourceKey the stable handwritten source identity
        /// @param content the group content
        void group(String sourceKey, Runnable content) {
            composer.enter(sourceKey, null, content);
        }

        /// Declares a semantic-keyed group reconciled independently of sibling order.
        ///
        /// @param sourceKey the stable handwritten collection source identity
        /// @param key the application semantic key
        /// @param content the group content
        void keyedGroup(String sourceKey, String key, Runnable content) {
            composer.enter(sourceKey, Objects.requireNonNull(key, "key"), content);
        }

        /// Declares an explicit conditional branch with retained or disposed positional memory.
        ///
        /// @param sourceKey the stable handwritten branch identity
        /// @param visible whether the branch participates in the committed tree
        /// @param retainMemory whether hidden positional memory remains retained
        /// @param content the visible branch content
        void branch(String sourceKey, boolean visible, boolean retainMemory, Runnable content) {
            composer.branch(sourceKey, visible, retainMemory, content);
        }

        /// Returns one positional integer memory cell from the current group.
        ///
        /// @param initialValue the value used only when the slot is first created
        /// @return the stable memory cell
        LocalInt rememberInt(int initialValue) {
            return composer.rememberInt(initialValue);
        }

        /// Declares one committed semantic node.
        ///
        /// @param identifier the fixture-visible node identity
        void node(String identifier) {
            composer.node(identifier);
        }

        /// Declares a phase-attributed reactive read site.
        ///
        /// @param source the versioned producer read by application code
        /// @param site the stable read-site name inside the current group
        /// @param phases the phases invalidated when the source version changes
        void binding(StateSource source, String site, RuntimePhase... phases) {
            composer.binding(source, site, phases);
        }

        /// Declares a mount/cleanup effect scoped to the current group.
        ///
        /// @param key the effect identity inside the group
        /// @param mount the callback after a new effect commits
        /// @param cleanup the callback before the effect is removed
        void effect(String key, Runnable mount, Runnable cleanup) {
            composer.effect(key, mount, cleanup);
        }

        /// Registers cleanup for resources created only by the current draft.
        ///
        /// @param cleanup the callback run if composition aborts
        void onAbort(Runnable cleanup) {
            composer.onAbort(cleanup);
        }

        /// Registers work that may expose draft-local references only after structural commit.
        ///
        /// @param callback the post-commit callback
        void onCommit(Runnable callback) {
            composer.onCommit(callback);
        }

        /// Rejects the current draft with a stable diagnostic code.
        ///
        /// @param code the failure code
        void fail(String code) {
            throw new GroupedCompositionException(code, composer.ownerPath(), null);
        }
    }

    /// Stores one framework-owned mutable integer in a positional memory slot.
    @NotNullByDefault
    static final class LocalInt {
        /// The current value.
        private int value;

        /// Creates one cell.
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

        /// Increments the current value by one.
        void increment() {
            value = Math.incrementExact(value);
        }
    }

    /// Builds nested drafts while maintaining the explicit group stack.
    @NotNullByDefault
    private final class Composer {
        /// The active group stack.
        private final ArrayList<GroupDraft> stack = new ArrayList<>();

        /// The single scope facade passed to all application callbacks.
        private final Scope scope = new Scope(this);

        /// Creates a composer rooted at one draft.
        ///
        /// @param rootDraft the root draft
        private Composer(GroupDraft rootDraft) {
            stack.add(rootDraft);
        }

        /// Returns the application scope facade.
        ///
        /// @return the scope
        private Scope scope() {
            return scope;
        }

        /// Enters and leaves one declared group.
        ///
        /// @param sourceKey the handwritten source identity
        /// @param semanticKey the semantic key, or `null` for positional identity
        /// @param content the group content
        private void enter(
                String sourceKey,
                @Nullable String semanticKey,
                Runnable content
        ) {
            Objects.requireNonNull(sourceKey, "sourceKey");
            Objects.requireNonNull(content, "content");
            GroupDraft parent = current();
            String duplicateIdentity = sourceKey + '\u0000' + semanticKey;
            if (semanticKey != null && !parent.semanticKeys.add(duplicateIdentity)) {
                throw new GroupedCompositionException("duplicate-key", ownerPath(), null);
            }
            @Nullable GroupNode previous = semanticKey == null
                    ? parent.positional(sourceKey)
                    : parent.keyed(sourceKey, semanticKey);
            GroupNode group = previous == null ? new GroupNode(sourceKey, semanticKey) : previous;
            GroupDraft child = new GroupDraft(group);
            parent.activeChildren.add(child);
            stack.add(child);
            probe.callbackExecuted(RuntimeCallbackKind.STRUCTURE);
            try {
                content.run();
            } finally {
                stack.removeLast();
            }
        }

        /// Handles one explicit conditional branch.
        ///
        /// @param sourceKey the branch identity
        /// @param visible whether the branch is visible
        /// @param retainMemory whether hidden memory remains retained
        /// @param content the visible content
        private void branch(String sourceKey, boolean visible, boolean retainMemory, Runnable content) {
            Objects.requireNonNull(sourceKey, "sourceKey");
            Objects.requireNonNull(content, "content");
            GroupDraft parent = current();
            String semanticKey = "branch:" + sourceKey;
            if (visible) {
                enter(sourceKey, semanticKey, content);
                return;
            }
            @Nullable GroupNode previous = parent.keyed(sourceKey, semanticKey);
            if (retainMemory && previous != null) {
                parent.used.put(previous, Boolean.TRUE);
                parent.dormantChildren.add(previous);
            }
        }

        /// Returns or creates one positional integer memory slot.
        ///
        /// @param initialValue the first value
        /// @return the memory cell
        private LocalInt rememberInt(int initialValue) {
            GroupDraft draft = current();
            int position = draft.memoryCursor++;
            if (position < draft.group.memories.size()) {
                MemorySlot slot = draft.group.memories.get(position);
                if (!(slot.value instanceof LocalInt local)) {
                    throw new GroupedCompositionException("positional-memory-type-mismatch", ownerPath(), null);
                }
                draft.memories.add(slot);
                return local;
            }
            MemorySlot slot = new MemorySlot(new LocalInt(initialValue));
            draft.memories.add(slot);
            draft.newMemories.add(slot);
            return (LocalInt) slot.value;
        }

        /// Adds one semantic node to the root draft.
        ///
        /// @param identifier the node identity
        private void node(String identifier) {
            String validated = Objects.requireNonNull(identifier, "identifier");
            rootDraft().nodes.add(validated);
            probe.nodesVisited(1L);
        }

        /// Declares one dependency edge and captures the producer's current version.
        ///
        /// @param source the producer
        /// @param site the stable site
        /// @param phases the invalidation phases
        private void binding(StateSource source, String site, RuntimePhase... phases) {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(site, "site");
            Objects.requireNonNull(phases, "phases");
            RuntimePhase[] phaseCopy = phases.clone();
            for (RuntimePhase phase : phaseCopy) {
                Objects.requireNonNull(phase, "phase");
            }
            GroupDraft draft = current();
            EdgeKey key = new EdgeKey(source, site);
            if (draft.edges.containsKey(key)) {
                throw new GroupedCompositionException("duplicate-binding-site", ownerPath(), null);
            }
            @Nullable EdgeRecord previous = draft.group.edges.get(key);
            EdgeRecord edge = new EdgeRecord(source.version(), List.of(phaseCopy));
            if (previous != null) {
                edge.token = previous;
            }
            draft.edges.put(key, edge);
            probe.callbackExecuted(RuntimeCallbackKind.BINDING);
        }

        /// Declares one effect in the current group.
        ///
        /// @param key the effect key
        /// @param mount the mount callback
        /// @param cleanup the cleanup callback
        private void effect(String key, Runnable mount, Runnable cleanup) {
            GroupDraft draft = current();
            EffectRecord effect = new EffectRecord(
                    Objects.requireNonNull(mount, "mount"),
                    Objects.requireNonNull(cleanup, "cleanup")
            );
            if (draft.effects.put(Objects.requireNonNull(key, "key"), effect) != null) {
                throw new GroupedCompositionException("duplicate-effect-key", ownerPath(), null);
            }
        }

        /// Adds one abort cleanup to the root attempt.
        ///
        /// @param cleanup the cleanup
        private void onAbort(Runnable cleanup) {
            rootDraft().abortCleanups.add(Objects.requireNonNull(cleanup, "cleanup"));
        }

        /// Adds one callback to the root commit.
        ///
        /// @param callback the callback
        private void onCommit(Runnable callback) {
            rootDraft().afterCommit.add(Objects.requireNonNull(callback, "callback"));
        }

        /// Returns the current group draft.
        ///
        /// @return the current draft
        private GroupDraft current() {
            return stack.getLast();
        }

        /// Returns the root group draft.
        ///
        /// @return the root draft
        private GroupDraft rootDraft() {
            return stack.getFirst();
        }

        /// Returns a stable path for diagnostics.
        ///
        /// @return the current owner path
        private String ownerPath() {
            StringBuilder path = new StringBuilder("root");
            for (int index = 1; index < stack.size(); index++) {
                GroupNode group = stack.get(index).group;
                path.append('/').append(group.sourceKey);
                if (group.semanticKey != null) {
                    path.append('[').append(group.semanticKey).append(']');
                }
            }
            return path.toString();
        }

        /// Verifies that callbacks left the composer at its root.
        private void checkBalanced() {
            if (stack.size() != 1) {
                throw new IllegalStateException("Grouped composition stack is unbalanced");
            }
        }
    }

    /// Stores committed identity, positional memory, effects, edges, and descendants for one group.
    @NotNullByDefault
    private static final class GroupNode {
        /// The handwritten source identity.
        private final String sourceKey;

        /// The semantic identity, or `null` for positional groups.
        private final @Nullable String semanticKey;

        /// Directly owned positional memory.
        private ArrayList<MemorySlot> memories = new ArrayList<>();

        /// Active children in semantic order.
        private ArrayList<GroupNode> activeChildren = new ArrayList<>();

        /// Hidden children whose memory policy is retain.
        private ArrayList<GroupNode> dormantChildren = new ArrayList<>();

        /// Active dependency edges by site.
        private LinkedHashMap<EdgeKey, EdgeRecord> edges = new LinkedHashMap<>();

        /// Active effects by local key.
        private LinkedHashMap<String, EffectRecord> effects = new LinkedHashMap<>();

        /// Whether the probe holds this group registration.
        private boolean registered;

        /// Creates an unregistered group record.
        ///
        /// @param sourceKey the source identity
        /// @param semanticKey the semantic identity, or `null`
        private GroupNode(String sourceKey, @Nullable String semanticKey) {
            this.sourceKey = sourceKey;
            this.semanticKey = semanticKey;
        }
    }

    /// Stores a mutable draft for one group without altering the committed record.
    @NotNullByDefault
    private static final class GroupDraft {
        /// The committed or newly allocated group identity.
        private final GroupNode group;

        /// The active child drafts.
        private final ArrayList<GroupDraft> activeChildren = new ArrayList<>();

        /// Hidden child identities retained by policy.
        private final ArrayList<GroupNode> dormantChildren = new ArrayList<>();

        /// The positional memories selected by this execution.
        private final ArrayList<MemorySlot> memories = new ArrayList<>();

        /// Memories created only by this draft.
        private final ArrayList<MemorySlot> newMemories = new ArrayList<>();

        /// Declared dependency edges.
        private final LinkedHashMap<EdgeKey, EdgeRecord> edges = new LinkedHashMap<>();

        /// Declared effects.
        private final LinkedHashMap<String, EffectRecord> effects = new LinkedHashMap<>();

        /// Old children already selected by this attempt.
        private final IdentityHashMap<GroupNode, Boolean> used = new IdentityHashMap<>();

        /// Semantic identities declared in this parent during the attempt.
        private final HashSet<String> semanticKeys = new HashSet<>();

        /// Root-only semantic node order.
        private final ArrayList<String> nodes = new ArrayList<>();

        /// Root-only cleanups for an aborted attempt.
        private final ArrayList<Runnable> abortCleanups = new ArrayList<>();

        /// Root-only callbacks run after commit.
        private final ArrayList<Runnable> afterCommit = new ArrayList<>();

        /// Next unkeyed child position.
        private int positionalCursor;

        /// Next positional memory index.
        private int memoryCursor;

        /// Creates an empty draft over one group identity.
        ///
        /// @param group the group identity
        private GroupDraft(GroupNode group) {
            this.group = group;
        }

        /// Selects the old active child at the next positional location when source identity agrees.
        ///
        /// @param sourceKey the declared source identity
        /// @return the reusable group, or `null`
        private @Nullable GroupNode positional(String sourceKey) {
            int position = positionalCursor++;
            int unkeyedPosition = 0;
            for (GroupNode candidate : group.activeChildren) {
                if (candidate.semanticKey != null) {
                    continue;
                }
                if (unkeyedPosition == position) {
                    if (candidate.sourceKey.equals(sourceKey) && used.put(candidate, Boolean.TRUE) == null) {
                        return candidate;
                    }
                    return null;
                }
                unkeyedPosition++;
            }
            return null;
        }

        /// Selects a matching old active or dormant semantic group.
        ///
        /// @param sourceKey the source identity
        /// @param semanticKey the semantic identity
        /// @return the reusable group, or `null`
        private @Nullable GroupNode keyed(String sourceKey, String semanticKey) {
            @Nullable GroupNode active = find(group.activeChildren, sourceKey, semanticKey);
            if (active != null) {
                used.put(active, Boolean.TRUE);
                return active;
            }
            @Nullable GroupNode dormant = find(group.dormantChildren, sourceKey, semanticKey);
            if (dormant != null) {
                used.put(dormant, Boolean.TRUE);
            }
            return dormant;
        }

        /// Finds one semantic child that has not already been consumed.
        ///
        /// @param children the candidate children
        /// @param sourceKey the source identity
        /// @param semanticKey the semantic identity
        /// @return the matching child, or `null`
        private @Nullable GroupNode find(List<GroupNode> children, String sourceKey, String semanticKey) {
            for (GroupNode child : children) {
                if (!used.containsKey(child)
                        && child.sourceKey.equals(sourceKey)
                        && semanticKey.equals(child.semanticKey)) {
                    return child;
                }
            }
            return null;
        }
    }

    /// Wraps one positional-memory value so its retention can be counted independently.
    @NotNullByDefault
    private static final class MemorySlot {
        /// The application-visible value.
        private final Object value;

        /// Creates a slot.
        ///
        /// @param value the retained value
        private MemorySlot(Object value) {
            this.value = value;
        }
    }

    /// Identifies a dependency read site by producer identity and local site name.
    @NotNullByDefault
    private static final class EdgeKey {
        /// The producer identity.
        private final StateSource source;

        /// The group-local site identity.
        private final String site;

        /// Creates an edge key.
        ///
        /// @param source the producer
        /// @param site the read site
        private EdgeKey(StateSource source, String site) {
            this.source = source;
            this.site = site;
        }

        /// Compares producer identity and site text.
        ///
        /// @param other the candidate object
        /// @return whether both keys identify the same read site
        @Override
        public boolean equals(@Nullable Object other) {
            return this == other || other instanceof EdgeKey key && source == key.source && site.equals(key.site);
        }

        /// Returns an identity-based hash consistent with [#equals(Object)].
        ///
        /// @return the hash code
        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(source) + site.hashCode();
        }
    }

    /// Stores one active or staged dependency registration.
    @NotNullByDefault
    private static final class EdgeRecord {
        /// The producer version observed by this execution.
        private final long version;

        /// The phases attributed to this read site.
        private final @Unmodifiable List<RuntimePhase> phases;

        /// The stable probe token, replaced with the prior record on reuse.
        private Object token = this;

        /// Creates an edge record.
        ///
        /// @param version the observed version
        /// @param phases the invalidation phases
        private EdgeRecord(long version, @Unmodifiable List<RuntimePhase> phases) {
            this.version = version;
            this.phases = phases;
        }
    }

    /// Stores effect callbacks retained by one committed group.
    @NotNullByDefault
    private static final class EffectRecord {
        /// The current mount callback.
        private Runnable mount;

        /// The current cleanup callback.
        private Runnable cleanup;

        /// Creates an effect declaration.
        ///
        /// @param mount the mount callback
        /// @param cleanup the cleanup callback
        private EffectRecord(Runnable mount, Runnable cleanup) {
            this.mount = mount;
            this.cleanup = cleanup;
        }
    }

    /// Accumulates live resource counts without allocating per visited group.
    @NotNullByDefault
    private static final class ResourceCount {
        /// The number of retained groups.
        private long groups;

        /// The number of active effects.
        private long effects;

        /// Creates an empty accumulator.
        private ResourceCount() {
        }
    }

    /// Reports one contained composition failure to the fixture adapter.
    @NotNullByDefault
    static final class GroupedCompositionException extends RuntimeException {
        /// The serialization identifier for diagnostic transport compatibility.
        private static final long serialVersionUID = 1L;

        /// The stable diagnostic code.
        private final String code;

        /// The explicit group owner path at the failure point.
        private final String ownerPath;

        /// Creates a contained failure.
        ///
        /// @param code the stable code
        /// @param ownerPath the explicit group path
        /// @param cause the application failure, or `null`
        private GroupedCompositionException(String code, String ownerPath, @Nullable Throwable cause) {
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

        /// Returns the explicit owner path.
        ///
        /// @return the owner path
        String ownerPath() {
            return ownerPath;
        }
    }
}
