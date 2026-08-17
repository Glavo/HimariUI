package org.glavo.himari.layout.focus;

import org.glavo.himari.layout.LayoutNode;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

/// Maintains document-order focus traversal for one layout tree.
@NotNullByDefault
public final class FocusTree {
    /// Focusable nodes in document order.
    private final ArrayList<LayoutNode> order = new ArrayList<>();

    /// The focused node identity, or `null`.
    private @Nullable Long focusedId;

    /// Identity of the trap root, or `null` when traversal is unscoped.
    private @Nullable Long trapId;

    /// Last rebuilt layout root, or `null` before the first rebuild.
    private @Nullable LayoutNode lastRoot;

    /// Whether the current focus came from keyboard traversal.
    private boolean focusVisible;

    /// Creates an empty focus tree.
    public FocusTree() {
    }

    /// Rebuilds the traversal order from the committed layout tree.
    ///
    /// @param root the layout root
    public void rebuild(LayoutNode root) {
        Objects.requireNonNull(root, "root");
        lastRoot = root;
        recCollect();
    }

    /// Restricts Tab traversal to `node` and its focusable descendants.
    ///
    /// A later [#rebuild(LayoutNode)] keeps the trap until [#clearTrap()] runs.
    ///
    /// @param node the trap root
    public void trap(LayoutNode node) {
        Objects.requireNonNull(node, "node");
        trapId = node.id();
        if (lastRoot != null) {
            recCollect();
        }
    }

    /// Removes the current trap and restores document-order traversal.
    public void clearTrap() {
        trapId = null;
        if (lastRoot != null) {
            recCollect();
        }
    }

    /// Returns the trap root identity, or `null` when unscoped.
    ///
    /// @return the trap identity
    public @Nullable Long trapId() {
        return trapId;
    }

    /// Returns the focused node identity, or `null`.
    ///
    /// @return the focused identity
    public @Nullable Long focusedId() {
        return focusedId;
    }

    /// Returns the focused node, or `null`.
    ///
    /// @return the focused node
    public @Nullable LayoutNode focusedNode() {
        return focusedId == null ? null : find(focusedId);
    }

    /// Moves focus to the next focusable node.
    ///
    /// Keyboard traversal marks focus as visible.
    ///
    /// @return the newly focused node, or `null`
    public @Nullable LayoutNode next() {
        @Nullable LayoutNode node = move(1);
        if (node != null) {
            focusVisible = true;
        }
        return node;
    }

    /// Moves focus to the previous focusable node.
    ///
    /// Keyboard traversal marks focus as visible.
    ///
    /// @return the newly focused node, or `null`
    public @Nullable LayoutNode previous() {
        @Nullable LayoutNode node = move(-1);
        if (node != null) {
            focusVisible = true;
        }
        return node;
    }

    /// Requests focus on a specific node if it is focusable.
    ///
    /// Pointer-driven requests should call [#setFocusVisible(boolean)] with `false`.
    ///
    /// @param node the candidate
    /// @return whether focus changed
    public boolean request(LayoutNode node) {
        Objects.requireNonNull(node, "node");
        if (find(node.id()) == null) {
            return false;
        }
        focusedId = node.id();
        return true;
    }

    /// Returns whether the current focus should show a keyboard focus ring.
    ///
    /// @return whether focus is keyboard-visible
    public boolean focusVisible() {
        return focusVisible;
    }

    /// Sets whether the current focus should show a keyboard focus ring.
    ///
    /// @param focusVisible whether focus is keyboard-visible
    public void setFocusVisible(boolean focusVisible) {
        this.focusVisible = focusVisible;
    }

    /// Transfers keyboard focus from this window tree to `destination`.
    ///
    /// This tree keeps its last focused node so a later transfer can restore it, but marks
    /// focus not visible. The destination restores its last focused node, or the first
    /// focusable node, and marks focus visible.
    ///
    /// @param destination the tree that becomes active
    /// @return whether the destination has a focused node after the transfer
    public boolean transferTo(FocusTree destination) {
        Objects.requireNonNull(destination, "destination");
        focusVisible = false;
        if (destination.focusedId == null && !destination.order.isEmpty()) {
            destination.focusedId = destination.order.getFirst().id();
        }
        destination.focusVisible = destination.focusedId != null;
        return destination.focusedId != null;
    }

    /// Moves focus by a signed step.
    ///
    /// @param delta the step
    /// @return the newly focused node
    private @Nullable LayoutNode move(int delta) {
        if (order.isEmpty()) {
            focusedId = null;
            return null;
        }
        int index = 0;
        if (focusedId != null) {
            for (int candidate = 0; candidate < order.size(); candidate++) {
                if (order.get(candidate).id() == focusedId) {
                    index = candidate;
                    break;
                }
            }
        }
        int next = Math.floorMod(index + delta, order.size());
        LayoutNode node = order.get(next);
        focusedId = node.id();
        return node;
    }

    /// Rebuilds [order] from [lastRoot], honoring the current trap.
    private void recCollect() {
        order.clear();
        if (lastRoot != null) {
            collect(lastRoot, trapId == null);
        }
        if (focusedId != null && find(focusedId) == null) {
            focusedId = order.isEmpty() ? null : order.getFirst().id();
        } else if (focusedId == null && !order.isEmpty()) {
            focusedId = order.getFirst().id();
        }
    }

    /// Collects focusable descendants in document order.
    ///
    /// @param node the current node
    /// @param insideTrap whether `node` is the trap or a descendant of it
    private void collect(LayoutNode node, boolean insideTrap) {
        boolean nextInside = insideTrap || (trapId != null && node.id() == trapId);
        if (node.focusable() && nextInside) {
            order.add(node);
        }
        for (LayoutNode child : node.children()) {
            collect(child, nextInside);
        }
    }

    /// Finds a focusable node by identity.
    ///
    /// @param id the identity
    /// @return the node, or `null`
    private @Nullable LayoutNode find(long id) {
        for (LayoutNode node : order) {
            if (node.id() == id) {
                return node;
            }
        }
        return null;
    }
}
