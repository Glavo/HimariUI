package org.glavo.himari.layout;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;

/// Measures and places children of a [LayoutKind#CUSTOM] node.
///
/// Implementations call [LayoutNode#measure(Constraints)] and
/// [LayoutNode#place(Offset, Offset)] on each child.
@NotNullByDefault
public interface CustomLayout {
    /// Measures `children` under `constraints` and returns the container size.
    ///
    /// Each child must be measured at most once through [LayoutNode#measure(Constraints)].
    ///
    /// @param constraints the inner constraints
    /// @param children the children in document order
    /// @return the container size before parent constraint clamping
    Size measure(Constraints constraints, List<LayoutNode> children);

    /// Places previously measured children.
    ///
    /// Each child must be placed exactly once through [LayoutNode#place(Offset, Offset)].
    ///
    /// @param inner the inner origin relative to the custom node
    /// @param root the inner origin in root coordinates
    /// @param size the custom node's measured size
    /// @param children the children in document order
    void place(Offset inner, Offset root, Size size, List<LayoutNode> children);
}
