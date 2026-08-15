package org.glavo.himari.runtime.mount;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.glavo.himari.runtime.animation.AnimationTransaction;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/// Stages mounted topology, property targets, and inherited animation metadata for one atomic commit.
///
/// A transaction is single-use. [#commit()] publishes every staged change together.
/// [#abort()] discards the draft and restores previously committed property targets. Failure while
/// applying staged appliers aborts remaining appliers and restores previous values.
@NotNullByDefault
public final class UiCommitTransaction {
    /// The tree that owns this transaction.
    private final MountTree tree;

    /// Staged element insertions in declaration order.
    private final ArrayList<MountedNode> insertions = new ArrayList<>();

    /// Staged element removals in declaration order.
    private final ArrayList<MountedNode> removals = new ArrayList<>();

    /// Staged property publications keyed by binding identity.
    private final LinkedHashMap<PropertyBinding<?>, StagedProperty<?>> properties = new LinkedHashMap<>();

    /// Inherited animation metadata staged with this commit, or `null`.
    private @Nullable AnimationTransaction animationTransaction;

    /// Whether the transaction remains open.
    private boolean open = true;

    /// Creates one empty transaction.
    ///
    /// @param tree the owning mount tree
    UiCommitTransaction(MountTree tree) {
        this.tree = Objects.requireNonNull(tree, "tree");
    }

    /// Stages one newly committed mounted element.
    ///
    /// @param node the inserted node
    void stageInsert(MountedNode node) {
        checkOpen();
        insertions.add(Objects.requireNonNull(node, "node"));
    }

    /// Stages one disposed mounted element.
    ///
    /// @param node the removed node
    void stageRemove(MountedNode node) {
        checkOpen();
        removals.add(Objects.requireNonNull(node, "node"));
    }

    /// Stages one property model-target replacement.
    ///
    /// @param binding the binding whose target changed
    /// @param value the next non-null value
    /// @param <T> the property type
    <T> void stageProperty(PropertyBinding<T> binding, T value) {
        checkOpen();
        Objects.requireNonNull(binding, "binding");
        Objects.requireNonNull(value, "value");
        properties.put(binding, new StagedProperty<>(binding, value));
    }

    /// Stages inherited animation metadata for this commit.
    ///
    /// @param transaction the immutable animation metadata
    public void stageAnimation(AnimationTransaction transaction) {
        checkOpen();
        animationTransaction = Objects.requireNonNull(transaction, "transaction");
    }

    /// Returns whether this transaction still accepts staged work.
    ///
    /// @return whether the transaction is open
    public boolean isOpen() {
        return open;
    }

    /// Publishes every staged change atomically.
    ///
    /// @return the commit observation
    /// @throws IllegalStateException if the transaction is already closed
    public UiCommitResult commit() {
        checkOpen();
        AnimationPhaseImpact phases = AnimationPhaseImpact.NONE;
        ArrayList<AppliedProperty<?>> applied = new ArrayList<>();
        try {
            for (StagedProperty<?> staged : properties.values()) {
                phases = phases.union(staged.publish(applied));
            }
            for (MountedNode node : removals) {
                node.dispose();
            }
            open = false;
            return tree.recordCommit(
                    insertions.size(),
                    removals.size(),
                    applied.size(),
                    phases,
                    animationTransaction
            );
        } catch (RuntimeException | Error failure) {
            restore(applied);
            open = false;
            throw failure;
        }
    }

    /// Discards staged work without publishing property targets.
    ///
    /// Repeated abort is permitted after commit or abort.
    public void abort() {
        if (!open) {
            return;
        }
        open = false;
        for (MountedNode node : insertions) {
            node.dispose();
        }
    }

    /// Restores previously committed property values after a failed apply.
    ///
    /// @param applied the properties already published
    private static void restore(List<AppliedProperty<?>> applied) {
        for (int index = applied.size() - 1; index >= 0; index--) {
            applied.get(index).restore();
        }
    }

    /// Verifies that the transaction remains open.
    private void checkOpen() {
        if (!open) {
            throw new IllegalStateException("UiCommitTransaction is closed");
        }
    }

    /// Holds one staged property replacement.
    ///
    /// @param <T> the property type
    @NotNullByDefault
    private record StagedProperty<T>(PropertyBinding<T> binding, T value) {
        /// Publishes this replacement and records an undo entry.
        ///
        /// @param applied the undo log
        /// @return the binding phase impact
        private AnimationPhaseImpact publish(List<AppliedProperty<?>> applied) {
            @Nullable T previous = binding.publish(value);
            applied.add(new AppliedProperty<>(binding, previous));
            @Nullable PropertyApplier<T> applier = binding.applier();
            if (applier != null) {
                applier.apply(value);
            }
            return binding.phaseImpact();
        }
    }

    /// Holds one published property that can be restored.
    ///
    /// @param <T> the property type
    @NotNullByDefault
    private record AppliedProperty<T>(PropertyBinding<T> binding, @Nullable T previous) {
        /// Restores the previous committed value, or disposes the first publication.
        private void restore() {
            binding.restore(previous);
        }
    }
}
