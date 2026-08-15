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

    /// Creates an empty focus tree.
    public FocusTree() {
    }

    /// Rebuilds the traversal order from the committed layout tree.
    ///
    /// @param root the layout root
    public void rebuild(LayoutNode root) {
        Objects.requireNonNull(root, "root");
        order.clear();
        collect(root);
        if (focusedId != null && find(focusedId) == null) {
            focusedId = order.isEmpty() ? null : order.getFirst().id();
        } else if (focusedId == null && !order.isEmpty()) {
            focusedId = order.getFirst().id();
        }
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
    /// @return the newly focused node, or `null`
    public @Nullable LayoutNode next() {
        return move(1);
    }

    /// Moves focus to the previous focusable node.
    ///
    /// @return the newly focused node, or `null`
    public @Nullable LayoutNode previous() {
        return move(-1);
    }

    /// Requests focus on a specific node if it is focusable.
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

    /// Collects focusable descendants in document order.
    ///
    /// @param node the current node
    private void collect(LayoutNode node) {
        if (node.focusable()) {
            order.add(node);
        }
        for (LayoutNode child : node.children()) {
            collect(child);
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
