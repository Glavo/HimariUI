package org.glavo.himari.layout.hit;

import org.glavo.himari.layout.LayoutNode;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Performs bounds-based hit testing in reverse document order.
@NotNullByDefault
public final class HitTester {
    /// Prevents instantiation.
    private HitTester() {
    }

    /// Returns the front-most node whose bounds contain the point.
    ///
    /// @param root the layout root
    /// @param x the root-relative horizontal coordinate
    /// @param y the root-relative vertical coordinate
    /// @return the hit node, or `null`
    public static @Nullable LayoutNode hit(LayoutNode root, float x, float y) {
        Objects.requireNonNull(root, "root");
        ArrayList<LayoutNode> path = new ArrayList<>();
        collectHits(root, x, y, path);
        return path.isEmpty() ? null : path.getLast();
    }

    /// Returns the capture-to-bubble path for a point.
    ///
    /// @param root the layout root
    /// @param x the root-relative horizontal coordinate
    /// @param y the root-relative vertical coordinate
    /// @return the path from root to the front-most hit
    public static List<LayoutNode> path(LayoutNode root, float x, float y) {
        Objects.requireNonNull(root, "root");
        ArrayList<LayoutNode> path = new ArrayList<>();
        collectHits(root, x, y, path);
        return List.copyOf(path);
    }

    /// Walks children in reverse document order and then records this node.
    ///
    /// @param node the current node
    /// @param x the point x
    /// @param y the point y
    /// @param path the accumulator from root to leaf
    private static boolean collectHits(LayoutNode node, float x, float y, List<LayoutNode> path) {
        if (!node.bounds().contains(x, y)) {
            return false;
        }
        List<LayoutNode> children = node.children();
        for (int index = children.size() - 1; index >= 0; index--) {
            if (collectHits(children.get(index), x, y, path)) {
                path.addFirst(node);
                return true;
            }
        }
        path.add(node);
        return true;
    }
}
