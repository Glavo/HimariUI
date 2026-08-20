package org.glavo.himari.layout.hit;

import org.glavo.himari.layout.LayoutNode;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Stores a reverse-z flattened spatial index of placed layout nodes.
///
/// Children are recorded before their parent, last child first, so the first
/// bounds or shape hit that also lies inside every ancestor clip is the
/// front-most deepest node.
@NotNullByDefault
public final class SpatialIndex {
    /// Reverse-z slots.
    private final Slot @Unmodifiable [] slots;

    /// Creates an index over `slots`.
    ///
    /// @param slots the reverse-z slots
    private SpatialIndex(Slot @Unmodifiable [] slots) {
        this.slots = slots;
    }

    /// Builds an index from a placed root.
    ///
    /// @param root the layout root
    /// @return the index
    public static SpatialIndex build(LayoutNode root) {
        Objects.requireNonNull(root, "root");
        ArrayList<Slot> slots = new ArrayList<>();
        flatten(root, List.of(), slots);
        return new SpatialIndex(slots.toArray(Slot[]::new));
    }

    /// Returns the front-most node whose shape contains the point and that is
    /// not excluded by an ancestor clip.
    ///
    /// @param x the root-relative horizontal coordinate
    /// @param y the root-relative vertical coordinate
    /// @return the hit node, or `null`
    public @Nullable LayoutNode hit(float x, float y) {
        for (Slot slot : slots) {
            if (!containsAll(slot.ancestorClips, x, y)) {
                continue;
            }
            HitClip clip = slot.node.hitClip();
            if (clip != null) {
                if (clip.contains(x, y)) {
                    return slot.node;
                }
                continue;
            }
            if (slot.node.bounds().contains(x, y)) {
                return slot.node;
            }
        }
        return null;
    }

    /// Returns the number of indexed nodes.
    ///
    /// @return the count
    public int size() {
        return slots.length;
    }

    /// Records `node`'s descendants last-to-first, then `node`.
    ///
    /// @param node the current node
    /// @param ancestorClips clips inherited from ancestors
    /// @param slots the accumulator
    private static void flatten(LayoutNode node, List<HitClip> ancestorClips, List<Slot> slots) {
        if (node.ignoresPointer()) {
            return;
        }
        if (node.absorbsPointer()) {
            slots.add(new Slot(node, ancestorClips.toArray(HitClip[]::new)));
            return;
        }
        List<HitClip> descendantClips = ancestorClips;
        HitClip self = node.hitClip();
        if (self != null) {
            ArrayList<HitClip> next = new ArrayList<>(ancestorClips.size() + 1);
            next.addAll(ancestorClips);
            next.add(self);
            descendantClips = next;
        }
        List<LayoutNode> children = node.children();
        for (int index = children.size() - 1; index >= 0; index--) {
            flatten(children.get(index), descendantClips, slots);
        }
        slots.add(new Slot(node, ancestorClips.toArray(HitClip[]::new)));
    }

    /// Returns whether every clip contains the point.
    ///
    /// @param clips the ancestor clips
    /// @param x the root-relative x
    /// @param y the root-relative y
    /// @return whether every clip contains the point
    private static boolean containsAll(HitClip[] clips, float x, float y) {
        for (HitClip clip : clips) {
            if (!clip.contains(x, y)) {
                return false;
            }
        }
        return true;
    }

    /// One indexed node plus clips inherited from ancestors.
    ///
    /// @param node the layout node
    /// @param ancestorClips ancestor clips, empty when none clip
    private record Slot(LayoutNode node, HitClip @Unmodifiable [] ancestorClips) {
        /// Validates the slot.
        private Slot {
            Objects.requireNonNull(node, "node");
            Objects.requireNonNull(ancestorClips, "ancestorClips");
        }
    }
}
