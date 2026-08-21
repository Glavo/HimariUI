package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates an unstyled numbered page selector.
@NotNullByDefault
public final class Pagination {
    /// Default control size.
    private static final Size SIZE = new Size(160.0f, 24.0f);

    /// Inclusive page count.
    private final int pageCount;

    /// Zero-based current page.
    private int page;

    /// Whether the control ignores activation.
    private boolean disabled;

    /// Mounted leaf that receives the published page.
    private @Nullable LayoutNode node;

    /// Creates a pagination control starting at page `0`.
    ///
    /// @param pageCount the number of pages, at least `2`
    public Pagination(int pageCount) {
        if (pageCount < 2) {
            throw new IllegalArgumentException("Pagination must contain at least two pages");
        }
        this.pageCount = pageCount;
        this.page = 0;
    }

    /// Returns the page count.
    ///
    /// @return the count
    public int pageCount() {
        return pageCount;
    }

    /// Returns the zero-based current page.
    ///
    /// @return the page
    public int page() {
        return page;
    }

    /// Returns the one-based current page label.
    ///
    /// @return the label
    public String value() {
        return Integer.toString(page + 1);
    }

    /// Returns whether the control is disabled.
    ///
    /// @return whether the control is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Selects `page` and publishes it when mounted.
    ///
    /// @param page the zero-based page
    public void setPage(int page) {
        if (page < 0 || page >= pageCount) {
            throw new IllegalArgumentException("Pagination page is out of range");
        }
        if (disabled) {
            return;
        }
        this.page = page;
        publish();
    }

    /// Advances to the next page, wrapping at the end.
    public void next() {
        if (disabled) {
            return;
        }
        page = (page + 1) % pageCount;
        publish();
    }

    /// Moves to the previous page, wrapping at the start.
    public void previous() {
        if (disabled) {
            return;
        }
        page = (page + pageCount - 1) % pageCount;
        publish();
    }

    /// Sets the disabled state and publishes it when mounted.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        publish();
    }

    /// Builds the pagination leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        LayoutNode leaf = factory.leaf(
                name,
                SIZE,
                List.of(new LayoutModifier.Padding(0.0f)),
                true,
                SemanticsRole.PAGINATION,
                value(),
                Set.of(SemanticsAction.ACTIVATE, SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                this::next,
                delta -> {
                    if (delta > 0) {
                        next();
                    } else {
                        previous();
                    }
                }
        );
        this.node = leaf;
        publish();
        return leaf;
    }

    /// Publishes page and disabled onto the mounted leaf.
    private void publish() {
        if (node == null) {
            return;
        }
        node.setLabel(value());
        node.setDisabled(disabled);
        node.setPositionInSet(page);
        node.setSizeOfSet(pageCount);
        node.setRangeValue(page);
        node.setRangeExtent(0, pageCount - 1);
    }
}
