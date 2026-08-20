package org.glavo.himari.layout;

import org.glavo.himari.layout.hit.HitClip;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerListener;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsGrid;
import org.glavo.himari.layout.semantics.SemanticsGridItem;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsScroll;
import org.glavo.himari.layout.semantics.SemanticsTextRange;
import org.glavo.himari.layout.semantics.TextDirection;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;

/// Stores one layout node, its children, and the last measure/place result.
@NotNullByDefault
public final class LayoutNode {
    /// The stable node identity.
    private final long id;

    /// The diagnostic name.
    private final String name;

    /// The measure policy.
    private final LayoutKind kind;

    /// Cross-axis alignment for row and column.
    private final Alignment alignment;

    /// Applied modifiers in declaration order.
    private final @Unmodifiable List<LayoutModifier> modifiers;

    /// Direct children in z-order / document order.
    private final ArrayList<LayoutNode> children = new ArrayList<>();

    /// The leaf intrinsic size, or `null` for a container.
    private final @Nullable Size intrinsicSize;

    /// Whether this node can receive focus.
    private final boolean focusable;

    /// The semantics role.
    private final SemanticsRole role;

    /// The semantics label.
    private String label;

    /// The accessible hint, empty when absent.
    private String hint = "";

    /// Declared semantics actions.
    private final @Unmodifiable Set<SemanticsAction> actions;

    /// The activation callback, or `null`.
    private final @Nullable Runnable onActivate;

    /// The increment/decrement callback, or `null`.
    private final @Nullable IntConsumer onAdjust;

    /// Custom measure/place delegate used when [`#kind`] is [LayoutKind#CUSTOM].
    private final @Nullable CustomLayout customLayout;

    /// Toggle state published to semantics, or `null`.
    private @Nullable Boolean selected;

    /// Range value published to semantics, or `null`.
    private @Nullable Double rangeValue;

    /// Inclusive range minimum published to UIA, default `0`.
    private double rangeMinimum = 0.0;

    /// Inclusive range maximum published to UIA, default `100`.
    private double rangeMaximum = 100.0;

    /// Live-region politeness published to semantics.
    private SemanticsLiveRegion liveRegion = SemanticsLiveRegion.OFF;

    /// Published text selection, or `null` when the node is not an editor.
    private @Nullable SemanticsTextRange textRange;

    /// Published grid extent, or `null`.
    private @Nullable SemanticsGrid grid;

    /// Published vertical scroll snapshot, or `null`.
    private @Nullable SemanticsScroll scroll;

    /// Published cell position, or `null`.
    private @Nullable SemanticsGridItem gridItem;

    /// Whether the node is disabled.
    private boolean disabled;

    /// Whether the node is read-only.
    private boolean readOnly;

    /// Whether the node is a password field.
    private boolean password;

    /// Access key published to semantics, empty when absent.
    private String accessKey = "";

    /// Accelerator key published to semantics, empty when absent.
    private String acceleratorKey = "";

    /// Whether the node is required for form submission.
    private boolean required;

    /// Item-status text published to semantics, empty when absent.
    private String itemStatus = "";

    /// Item-type text published to semantics, empty when absent.
    private String itemType = "";

    /// BCP-47 locale published to semantics, empty when unspecified.
    private String locale = "";

    /// Hierarchical level published to semantics; `0` when unspecified.
    private int level;

    /// One-based position in a set; `0` when unspecified.
    private int positionInSet;

    /// Set size published to semantics; `0` when unspecified.
    private int sizeOfSet;

    /// Full description published to semantics, empty when absent.
    private String description = "";

    /// Whether the node currently reports invalid form data.
    private boolean error;

    /// UIA landmark type published to semantics; `0` when unspecified.
    private int landmarkType;

    /// Localized landmark type published to semantics, empty when absent.
    private String localizedLandmarkType = "";

    /// ARIA role published to semantics, empty when absent.
    private String ariaRole = "";

    /// ARIA properties published to semantics, empty when absent.
    private String ariaProperties = "";

    /// Controller-for target identity published to semantics, empty when absent.
    private String controllerFor = "";

    /// Described-by target identity published to semantics, empty when absent.
    private String describedBy = "";

    /// Flows-to target identity published to semantics, empty when absent.
    private String flowsTo = "";

    /// Labeled-by target identity published to semantics, empty when absent.
    private String labeledBy = "";

    /// Flows-from target identity published to semantics, empty when absent.
    private String flowsFrom = "";

    /// Whether the node prefers visual content over textual equivalents.
    private boolean optimizeForVisualContent;

    /// Fill color published to semantics as ARGB; `0` when unspecified.
    private int fillColor;

    /// Outline color published to semantics as ARGB; `0` when unspecified.
    private int outlineColor;

    /// UIA fill type published to semantics; `0` when unspecified.
    private int fillType;

    /// UIA visual-effects flags published to semantics; `0` when unspecified.
    private int visualEffects;

    /// UIA outline thickness published to semantics; `0` when unspecified.
    private int outlineThickness;

    /// UIA element rotation in degrees; `0` when unspecified.
    private int rotation;

    /// Whether the node is peripheral to the main task.
    private boolean peripheral;

    /// UIA annotation type published to semantics; `0` when unspecified.
    private int annotationType;

    /// Annotation-object identities published to semantics, empty when absent.
    private String annotationObjects = "";

    /// Listeners invoked after [#setLabel(String)] when this node is a live region.
    private final ArrayList<Runnable> labelListeners = new ArrayList<>(0);

    /// Pointer listeners invoked from the target toward the root during dispatch.
    private final ArrayList<PointerListener> pointerListeners = new ArrayList<>(0);

    /// Scroll offset applied when this node uses [LayoutKind#SCROLL].
    private float scrollOffset;

    /// Constraints used by the current measure pass, or `null` before measure.
    private @Nullable Constraints measuredConstraints;

    /// The size published by the current measure pass.
    private Size size = Size.ZERO;

    /// The origin relative to the parent.
    private Offset offset = Offset.ZERO;

    /// The origin relative to the layout root.
    private Offset origin = Offset.ZERO;

    /// Whether this node was measured in the current pass.
    private boolean measured;

    /// Whether this node was placed in the current pass.
    private boolean placed;

    /// Baseline relative to this node's top edge after measure.
    private float baseline;

    /// First-class alignment lines published after measure.
    private AlignmentLines alignmentLines = AlignmentLines.ZERO;

    /// Creates one node.
    ///
    /// @param id the identity
    /// @param name the diagnostic name
    /// @param kind the policy
    /// @param alignment the cross-axis alignment
    /// @param modifiers the modifiers
    /// @param intrinsicSize the leaf size, or `null`
    /// @param focusable whether the node is focusable
    /// @param role the semantics role
    /// @param label the semantics label
    /// @param actions the semantics actions
    /// @param onActivate the activation callback, or `null`
    /// @param onAdjust the increment/decrement callback, or `null`
    LayoutNode(
            long id,
            String name,
            LayoutKind kind,
            Alignment alignment,
            List<LayoutModifier> modifiers,
            @Nullable Size intrinsicSize,
            boolean focusable,
            SemanticsRole role,
            String label,
            Set<SemanticsAction> actions,
            @Nullable Runnable onActivate,
            @Nullable IntConsumer onAdjust
    ) {
        this(
                id,
                name,
                kind,
                alignment,
                modifiers,
                intrinsicSize,
                focusable,
                role,
                label,
                actions,
                onActivate,
                onAdjust,
                null
        );
    }

    /// Creates a node that may own a custom layout delegate.
    ///
    /// @param id the identity
    /// @param name the diagnostic name
    /// @param kind the policy
    /// @param alignment the cross-axis alignment
    /// @param modifiers the modifiers
    /// @param intrinsicSize the leaf size, or `null`
    /// @param focusable whether the node is focusable
    /// @param role the semantics role
    /// @param label the semantics label
    /// @param actions the semantics actions
    /// @param onActivate the activation callback, or `null`
    /// @param onAdjust the increment/decrement callback, or `null`
    /// @param customLayout the custom delegate, or `null`
    LayoutNode(
            long id,
            String name,
            LayoutKind kind,
            Alignment alignment,
            List<LayoutModifier> modifiers,
            @Nullable Size intrinsicSize,
            boolean focusable,
            SemanticsRole role,
            String label,
            Set<SemanticsAction> actions,
            @Nullable Runnable onActivate,
            @Nullable IntConsumer onAdjust,
            @Nullable CustomLayout customLayout
    ) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.alignment = Objects.requireNonNull(alignment, "alignment");
        this.modifiers = List.copyOf(modifiers);
        this.intrinsicSize = intrinsicSize;
        this.focusable = focusable;
        this.role = Objects.requireNonNull(role, "role");
        this.label = Objects.requireNonNull(label, "label");
        this.actions = Set.copyOf(actions);
        this.onActivate = onActivate;
        this.onAdjust = onAdjust;
        this.customLayout = customLayout;
        if (kind == LayoutKind.CUSTOM && customLayout == null) {
            throw new IllegalArgumentException("CUSTOM layout requires a CustomLayout");
        }
    }

    /// Returns the identity.
    ///
    /// @return the identity
    public long id() {
        return id;
    }

    /// Returns the diagnostic name.
    ///
    /// @return the name
    public String name() {
        return name;
    }

    /// Returns the layout policy.
    ///
    /// @return the kind
    public LayoutKind kind() {
        return kind;
    }

    /// Returns the pending invalidation phase for inspector localization.
    ///
    /// `MEASURE` means this node has not been measured in the current pass.
    /// `PLACE` means it has a size but has not been placed. `NONE` means layout
    /// is committed and remaining faults are outside measure/place.
    ///
    /// @return `MEASURE`, `PLACE`, or `NONE`
    public String invalidationPhase() {
        if (!measured) {
            return "MEASURE";
        }
        if (!placed) {
            return "PLACE";
        }
        return "NONE";
    }

    /// Returns the leaf intrinsic size, or [`Size#ZERO`] for a container.
    ///
    /// @return the intrinsic size
    public Size intrinsicSize() {
        return intrinsicSize == null ? Size.ZERO : intrinsicSize;
    }

    /// Returns the clockwise rotation in degrees declared on this node.
    ///
    /// @return the rotation, or `0` when absent
    public float rotationDegrees() {
        for (LayoutModifier modifier : modifiers) {
            if (modifier instanceof LayoutModifier.Rotate rotate) {
                return rotate.degrees();
            }
        }
        return 0.0f;
    }

    /// Returns the translation declared on this node.
    ///
    /// @return the translation, or zero when absent
    public Offset translation() {
        for (LayoutModifier modifier : modifiers) {
            if (modifier instanceof LayoutModifier.Translate translate) {
                return new Offset(translate.x(), translate.y());
            }
        }
        return Offset.ZERO;
    }

    /// Returns the shear factors declared on this node.
    ///
    /// @return the shear, or zero when absent
    public Offset shear() {
        for (LayoutModifier modifier : modifiers) {
            if (modifier instanceof LayoutModifier.Skew skew) {
                return new Offset(skew.x(), skew.y());
            }
        }
        return Offset.ZERO;
    }

    /// Returns whether hit testing clips descendants to this node's clip shape.
    ///
    /// Scroll viewports always clip to their bounds. Other nodes clip when they
    /// declare [`LayoutModifier.Clip`], [`LayoutModifier.ClipRRect`],
    /// [`LayoutModifier.ClipOval`], or [`LayoutModifier.ClipPath`].
    ///
    /// @return whether hits are clipped
    public boolean clipsHits() {
        return hitClip() != null;
    }

    /// Returns this node's hit-testing clip in root coordinates, or `null`.
    ///
    /// @return the clip, or `null` when hits are not clipped
    public @Nullable HitClip hitClip() {
        if (kind == LayoutKind.SCROLL) {
            return HitClip.rect(bounds());
        }
        for (LayoutModifier modifier : modifiers) {
            if (modifier instanceof LayoutModifier.Clip) {
                return HitClip.rect(bounds());
            }
            if (modifier instanceof LayoutModifier.ClipRRect rounded) {
                return HitClip.rounded(bounds(), rounded.radius());
            }
            if (modifier instanceof LayoutModifier.ClipOval) {
                return HitClip.oval(bounds());
            }
            if (modifier instanceof LayoutModifier.ClipPath path) {
                return HitClip.path(bounds(), path.points());
            }
        }
        return null;
    }

    /// Returns the hit-clip kind name, or `NONE` when unclipped.
    ///
    /// @return `NONE`, `RECT`, `ROUNDED`, `OVAL`, or `PATH`
    public String clipKind() {
        HitClip clip = hitClip();
        return clip == null ? "NONE" : clip.kind().name();
    }

    /// Returns whether this node and its descendants are excluded from hit testing.
    ///
    /// @return whether pointer hits are ignored
    public boolean ignoresPointer() {
        for (LayoutModifier modifier : modifiers) {
            if (modifier instanceof LayoutModifier.IgnorePointer) {
                return true;
            }
        }
        return false;
    }

    /// Returns whether this node absorbs pointer hits without targeting descendants.
    ///
    /// @return whether pointer hits stop on this node
    public boolean absorbsPointer() {
        for (LayoutModifier modifier : modifiers) {
            if (modifier instanceof LayoutModifier.AbsorbPointer) {
                return true;
            }
        }
        return false;
    }

    /// Returns the children in document order.
    ///
    /// @return the children
    public @Unmodifiable List<LayoutNode> children() {
        return List.copyOf(children);
    }

    /// Returns the last measured size.
    ///
    /// @return the size
    public Size size() {
        return size;
    }

    /// Returns the origin relative to the parent.
    ///
    /// @return the offset
    public Offset offset() {
        return offset;
    }

    /// Returns the origin relative to the layout root.
    ///
    /// @return the origin
    public Offset origin() {
        return origin;
    }

    /// Returns the axis-aligned bounds in root coordinates.
    ///
    /// @return the bounds
    public LayoutRect bounds() {
        return new LayoutRect(origin.x(), origin.y(), size.width(), size.height());
    }

    /// Returns whether the node is focusable.
    ///
    /// @return whether the node is focusable
    public boolean focusable() {
        return focusable;
    }

    /// Returns the semantics role.
    ///
    /// @return the role
    public SemanticsRole role() {
        return role;
    }

    /// Returns the semantics label.
    ///
    /// @return the label
    public String label() {
        return label;
    }

    /// Replaces the semantics label published by the next snapshot.
    ///
    /// A live-region node uses this when its announcement text changes so the next snapshot
    /// and host accessibility bridge observe the new name.
    ///
    /// When [`#liveRegion()`] is not [`SemanticsLiveRegion#OFF`], every registered
    /// [`#addLabelListener(Runnable)`] listener runs after the field is replaced.
    ///
    /// @param label the non-blank label
    public void setLabel(String label) {
        Objects.requireNonNull(label, "label");
        if (label.isBlank()) {
            throw new IllegalArgumentException("Semantics label must be nonblank");
        }
        this.label = label;
        if (liveRegion != SemanticsLiveRegion.OFF && !labelListeners.isEmpty()) {
            Runnable[] snapshot = labelListeners.toArray(Runnable[]::new);
            for (Runnable listener : snapshot) {
                listener.run();
            }
        }
    }

    /// Registers a listener invoked after [#setLabel(String)] when this node is a live region.
    ///
    /// @param listener the callback
    public void addLabelListener(Runnable listener) {
        labelListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /// Removes a listener previously passed to [#addLabelListener(Runnable)].
    ///
    /// @param listener the callback
    public void removeLabelListener(Runnable listener) {
        labelListeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    /// Registers a pointer listener for target-to-bubble routing.
    ///
    /// @param listener the callback
    public void addPointerListener(PointerListener listener) {
        pointerListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /// Removes a listener previously passed to [#addPointerListener(PointerListener)].
    ///
    /// @param listener the callback
    public void removePointerListener(PointerListener listener) {
        pointerListeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    /// Delivers one pointer event to listeners registered on this node.
    ///
    /// The first listener that returns `true` stops later listeners on this node.
    ///
    /// @param event the routed event
    /// @return whether a listener consumed the event
    public boolean dispatchPointer(PointerEvent event) {
        Objects.requireNonNull(event, "event");
        if (pointerListeners.isEmpty()) {
            return false;
        }
        PointerListener[] snapshot = pointerListeners.toArray(PointerListener[]::new);
        for (PointerListener listener : snapshot) {
            if (listener.onPointer(event)) {
                return true;
            }
        }
        return false;
    }

    /// Returns the accessible hint.
    ///
    /// @return the hint, empty when absent
    public String hint() {
        return hint;
    }

    /// Replaces the accessible hint published by the next snapshot.
    ///
    /// @param hint the hint, possibly empty
    public void setHint(String hint) {
        this.hint = Objects.requireNonNull(hint, "hint");
    }

    /// Returns the declared semantics actions.
    ///
    /// @return the actions
    public @Unmodifiable Set<SemanticsAction> actions() {
        return actions;
    }

    /// Returns the published toggle state, or `null`.
    ///
    /// @return the state
    public @Nullable Boolean selected() {
        return selected;
    }

    /// Returns the published range value, or `null`.
    ///
    /// @return the value
    public @Nullable Double rangeValue() {
        return rangeValue;
    }

    /// Returns the published inclusive range minimum.
    ///
    /// @return the minimum
    public double rangeMinimum() {
        return rangeMinimum;
    }

    /// Returns the published inclusive range maximum.
    ///
    /// @return the maximum
    public double rangeMaximum() {
        return rangeMaximum;
    }

    /// Returns the published live-region politeness.
    ///
    /// @return the politeness, never `null`
    public SemanticsLiveRegion liveRegion() {
        return liveRegion;
    }

    /// Returns the published text selection, or `null`.
    ///
    /// @return the range
    public @Nullable SemanticsTextRange textRange() {
        return textRange;
    }

    /// Returns the published grid extent, or `null`.
    ///
    /// @return the extent
    public @Nullable SemanticsGrid grid() {
        return grid;
    }

    /// Returns the published vertical scroll snapshot, or `null`.
    ///
    /// @return the snapshot
    public @Nullable SemanticsScroll scroll() {
        return scroll;
    }

    /// Returns the published cell position, or `null`.
    ///
    /// @return the cell
    public @Nullable SemanticsGridItem gridItem() {
        return gridItem;
    }

    /// Publishes a toggle state for the next semantics snapshot.
    ///
    /// @param selected the state
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /// Returns whether the node is disabled.
    ///
    /// @return whether the node is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Publishes the disabled state for the next semantics snapshot.
    ///
    /// Disabled nodes do not activate, adjust, or take focus.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /// Returns whether the node is read-only.
    ///
    /// @return whether the node is read-only
    public boolean readOnly() {
        return readOnly;
    }

    /// Publishes the read-only state for the next semantics snapshot.
    ///
    /// @param readOnly the state
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /// Returns whether the node is a password field.
    ///
    /// @return whether the node is a password field
    public boolean password() {
        return password;
    }

    /// Publishes the password state for the next semantics snapshot.
    ///
    /// @param password the state
    public void setPassword(boolean password) {
        this.password = password;
    }

    /// Returns the access key.
    ///
    /// @return the access key, empty when absent
    public String accessKey() {
        return accessKey;
    }

    /// Publishes the access key for the next semantics snapshot.
    ///
    /// @param accessKey the key, possibly empty
    public void setAccessKey(String accessKey) {
        this.accessKey = Objects.requireNonNull(accessKey, "accessKey");
    }

    /// Returns the accelerator key.
    ///
    /// @return the accelerator key, empty when absent
    public String acceleratorKey() {
        return acceleratorKey;
    }

    /// Publishes the accelerator key for the next semantics snapshot.
    ///
    /// @param acceleratorKey the key, possibly empty
    public void setAcceleratorKey(String acceleratorKey) {
        this.acceleratorKey = Objects.requireNonNull(acceleratorKey, "acceleratorKey");
    }

    /// Returns whether the node is required for form submission.
    ///
    /// @return whether the node is required
    public boolean required() {
        return required;
    }

    /// Publishes the required-for-form state for the next semantics snapshot.
    ///
    /// @param required the state
    public void setRequired(boolean required) {
        this.required = required;
    }

    /// Returns the item-status text.
    ///
    /// @return the status, empty when absent
    public String itemStatus() {
        return itemStatus;
    }

    /// Publishes the item-status text for the next semantics snapshot.
    ///
    /// @param itemStatus the status, possibly empty
    public void setItemStatus(String itemStatus) {
        this.itemStatus = Objects.requireNonNull(itemStatus, "itemStatus");
    }

    /// Returns the item-type text.
    ///
    /// @return the type, empty when absent
    public String itemType() {
        return itemType;
    }

    /// Publishes the item-type text for the next semantics snapshot.
    ///
    /// @param itemType the type, possibly empty
    public void setItemType(String itemType) {
        this.itemType = Objects.requireNonNull(itemType, "itemType");
    }

    /// Returns the landmark type.
    ///
    /// @return the landmark type, or `0` when unspecified
    public int landmarkType() {
        return landmarkType;
    }

    /// Publishes the landmark type for the next semantics snapshot.
    ///
    /// @param landmarkType the landmark type; `0` when unspecified
    public void setLandmarkType(int landmarkType) {
        if (landmarkType < 0) {
            throw new IllegalArgumentException("landmarkType must be nonnegative");
        }
        this.landmarkType = landmarkType;
    }

    /// Returns the localized landmark type.
    ///
    /// @return the localized type, empty when absent
    public String localizedLandmarkType() {
        return localizedLandmarkType;
    }

    /// Publishes the localized landmark type for the next semantics snapshot.
    ///
    /// @param localizedLandmarkType the localized type, possibly empty
    public void setLocalizedLandmarkType(String localizedLandmarkType) {
        this.localizedLandmarkType = Objects.requireNonNull(localizedLandmarkType, "localizedLandmarkType");
    }

    /// Returns the ARIA role.
    ///
    /// @return the role, empty when absent
    public String ariaRole() {
        return ariaRole;
    }

    /// Publishes the ARIA role for the next semantics snapshot.
    ///
    /// @param ariaRole the role, possibly empty
    public void setAriaRole(String ariaRole) {
        this.ariaRole = Objects.requireNonNull(ariaRole, "ariaRole");
    }

    /// Returns the ARIA properties string.
    ///
    /// @return the properties, empty when absent
    public String ariaProperties() {
        return ariaProperties;
    }

    /// Publishes the ARIA properties string for the next semantics snapshot.
    ///
    /// @param ariaProperties the properties, possibly empty
    public void setAriaProperties(String ariaProperties) {
        this.ariaProperties = Objects.requireNonNull(ariaProperties, "ariaProperties");
    }

    /// Returns the controller-for target identity.
    ///
    /// @return the identity, empty when absent
    public String controllerFor() {
        return controllerFor;
    }

    /// Publishes the controller-for target identity for the next semantics snapshot.
    ///
    /// @param controllerFor the identity, possibly empty
    public void setControllerFor(String controllerFor) {
        this.controllerFor = Objects.requireNonNull(controllerFor, "controllerFor");
    }

    /// Returns the described-by target identity.
    ///
    /// @return the identity, empty when absent
    public String describedBy() {
        return describedBy;
    }

    /// Publishes the described-by target identity for the next semantics snapshot.
    ///
    /// @param describedBy the identity, possibly empty
    public void setDescribedBy(String describedBy) {
        this.describedBy = Objects.requireNonNull(describedBy, "describedBy");
    }

    /// Returns the flows-to target identity.
    ///
    /// @return the identity, empty when absent
    public String flowsTo() {
        return flowsTo;
    }

    /// Publishes the flows-to target identity for the next semantics snapshot.
    ///
    /// @param flowsTo the identity, possibly empty
    public void setFlowsTo(String flowsTo) {
        this.flowsTo = Objects.requireNonNull(flowsTo, "flowsTo");
    }

    /// Returns the labeled-by target identity.
    ///
    /// @return the identity, empty when absent
    public String labeledBy() {
        return labeledBy;
    }

    /// Publishes the labeled-by target identity for the next semantics snapshot.
    ///
    /// @param labeledBy the identity, possibly empty
    public void setLabeledBy(String labeledBy) {
        this.labeledBy = Objects.requireNonNull(labeledBy, "labeledBy");
    }

    /// Returns the flows-from target identity.
    ///
    /// @return the identity, empty when absent
    public String flowsFrom() {
        return flowsFrom;
    }

    /// Publishes the flows-from target identity for the next semantics snapshot.
    ///
    /// @param flowsFrom the identity, possibly empty
    public void setFlowsFrom(String flowsFrom) {
        this.flowsFrom = Objects.requireNonNull(flowsFrom, "flowsFrom");
    }

    /// Returns whether the node prefers visual content over textual equivalents.
    ///
    /// @return `true` when visual content is preferred
    public boolean optimizeForVisualContent() {
        return optimizeForVisualContent;
    }

    /// Publishes the visual-content preference for the next semantics snapshot.
    ///
    /// @param optimizeForVisualContent `true` when visual content is preferred
    public void setOptimizeForVisualContent(boolean optimizeForVisualContent) {
        this.optimizeForVisualContent = optimizeForVisualContent;
    }

    /// Returns the fill color.
    ///
    /// @return the ARGB color, or `0` when unspecified
    public int fillColor() {
        return fillColor;
    }

    /// Publishes the fill color for the next semantics snapshot.
    ///
    /// @param fillColor the ARGB color, or `0` when unspecified
    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
    }

    /// Returns the outline color.
    ///
    /// @return the ARGB color, or `0` when unspecified
    public int outlineColor() {
        return outlineColor;
    }

    /// Publishes the outline color for the next semantics snapshot.
    ///
    /// @param outlineColor the ARGB color, or `0` when unspecified
    public void setOutlineColor(int outlineColor) {
        this.outlineColor = outlineColor;
    }

    /// Returns the fill type.
    ///
    /// @return the fill type, or `0` when unspecified
    public int fillType() {
        return fillType;
    }

    /// Publishes the fill type for the next semantics snapshot.
    ///
    /// @param fillType the nonnegative fill type
    public void setFillType(int fillType) {
        if (fillType < 0) {
            throw new IllegalArgumentException("fillType must be nonnegative");
        }
        this.fillType = fillType;
    }

    /// Returns the visual-effects flags.
    ///
    /// @return the flags, or `0` when unspecified
    public int visualEffects() {
        return visualEffects;
    }

    /// Publishes the visual-effects flags for the next semantics snapshot.
    ///
    /// @param visualEffects the nonnegative flags
    public void setVisualEffects(int visualEffects) {
        if (visualEffects < 0) {
            throw new IllegalArgumentException("visualEffects must be nonnegative");
        }
        this.visualEffects = visualEffects;
    }

    /// Returns the outline thickness.
    ///
    /// @return the thickness, or `0` when unspecified
    public int outlineThickness() {
        return outlineThickness;
    }

    /// Publishes the outline thickness for the next semantics snapshot.
    ///
    /// @param outlineThickness the nonnegative thickness
    public void setOutlineThickness(int outlineThickness) {
        if (outlineThickness < 0) {
            throw new IllegalArgumentException("outlineThickness must be nonnegative");
        }
        this.outlineThickness = outlineThickness;
    }

    /// Returns the element rotation in degrees.
    ///
    /// @return the rotation, or `0` when unspecified
    public int rotation() {
        return rotation;
    }

    /// Publishes the element rotation for the next semantics snapshot.
    ///
    /// @param rotation the rotation in degrees
    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    /// Returns whether the node is peripheral to the main task.
    ///
    /// @return `true` when the node is peripheral
    public boolean peripheral() {
        return peripheral;
    }

    /// Publishes the peripheral flag for the next semantics snapshot.
    ///
    /// @param peripheral `true` when the node is peripheral
    public void setPeripheral(boolean peripheral) {
        this.peripheral = peripheral;
    }

    /// Returns the annotation type.
    ///
    /// @return the type, or `0` when unspecified
    public int annotationType() {
        return annotationType;
    }

    /// Publishes the annotation type for the next semantics snapshot.
    ///
    /// @param annotationType the nonnegative type
    public void setAnnotationType(int annotationType) {
        if (annotationType < 0) {
            throw new IllegalArgumentException("annotationType must be nonnegative");
        }
        this.annotationType = annotationType;
    }

    /// Returns the annotation-object identities.
    ///
    /// @return the identities, empty when absent
    public String annotationObjects() {
        return annotationObjects;
    }

    /// Publishes the annotation-object identities for the next semantics snapshot.
    ///
    /// @param annotationObjects the identities, possibly empty
    public void setAnnotationObjects(String annotationObjects) {
        this.annotationObjects = Objects.requireNonNull(annotationObjects, "annotationObjects");
    }

    /// Returns the BCP-47 locale.
    ///
    /// @return the locale, empty when unspecified
    public String locale() {
        return locale;
    }

    /// Publishes the BCP-47 locale for the next semantics snapshot.
    ///
    /// @param locale the locale, possibly empty
    public void setLocale(String locale) {
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    /// Returns the hierarchical level.
    ///
    /// @return the level, or `0` when unspecified
    public int level() {
        return level;
    }

    /// Publishes the hierarchical level for the next semantics snapshot.
    ///
    /// @param level the nonnegative level
    public void setLevel(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("level must be nonnegative");
        }
        this.level = level;
    }

    /// Returns the one-based position in a set.
    ///
    /// @return the position, or `0` when unspecified
    public int positionInSet() {
        return positionInSet;
    }

    /// Publishes the one-based set position for the next semantics snapshot.
    ///
    /// @param positionInSet the nonnegative position
    public void setPositionInSet(int positionInSet) {
        if (positionInSet < 0) {
            throw new IllegalArgumentException("positionInSet must be nonnegative");
        }
        this.positionInSet = positionInSet;
    }

    /// Returns the set size.
    ///
    /// @return the size, or `0` when unspecified
    public int sizeOfSet() {
        return sizeOfSet;
    }

    /// Publishes the set size for the next semantics snapshot.
    ///
    /// @param sizeOfSet the nonnegative size
    public void setSizeOfSet(int sizeOfSet) {
        if (sizeOfSet < 0) {
            throw new IllegalArgumentException("sizeOfSet must be nonnegative");
        }
        this.sizeOfSet = sizeOfSet;
    }

    /// Returns the full description.
    ///
    /// @return the description, empty when absent
    public String description() {
        return description;
    }

    /// Publishes the full description for the next semantics snapshot.
    ///
    /// @param description the description, possibly empty
    public void setDescription(String description) {
        this.description = Objects.requireNonNull(description, "description");
    }

    /// Returns whether the node currently reports invalid form data.
    ///
    /// @return whether the node is in an error state
    public boolean error() {
        return error;
    }

    /// Publishes the form-validation error state for the next semantics snapshot.
    ///
    /// @param error the state
    public void setError(boolean error) {
        this.error = error;
    }

    /// Publishes a finite range value for the next semantics snapshot.
    ///
    /// @param value the value
    public void setRangeValue(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Range value must be finite");
        }
        this.rangeValue = value;
    }

    /// Publishes the inclusive range extent for UIA `IRangeValueProvider`.
    ///
    /// @param minimum the inclusive minimum
    /// @param maximum the inclusive maximum
    public void setRangeExtent(double minimum, double maximum) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)) {
            throw new IllegalArgumentException("Range extent must be finite");
        }
        if (maximum < minimum) {
            throw new IllegalArgumentException("Range maximum must be at least the minimum");
        }
        this.rangeMinimum = minimum;
        this.rangeMaximum = maximum;
    }

    /// Publishes live-region politeness for the next semantics snapshot.
    ///
    /// @param liveRegion the politeness
    public void setLiveRegion(SemanticsLiveRegion liveRegion) {
        this.liveRegion = Objects.requireNonNull(liveRegion, "liveRegion");
    }

    /// Publishes a text selection for the next semantics snapshot.
    ///
    /// @param textRange the range
    public void setTextRange(SemanticsTextRange textRange) {
        this.textRange = Objects.requireNonNull(textRange, "textRange");
    }

    /// Publishes a grid extent for the next semantics snapshot.
    ///
    /// @param grid the extent
    public void setGrid(SemanticsGrid grid) {
        this.grid = Objects.requireNonNull(grid, "grid");
    }

    /// Publishes a vertical scroll snapshot for the next semantics snapshot.
    ///
    /// @param scroll the snapshot
    public void setScroll(SemanticsScroll scroll) {
        this.scroll = Objects.requireNonNull(scroll, "scroll");
    }

    /// Publishes a cell position for the next semantics snapshot.
    ///
    /// @param gridItem the cell
    public void setGridItem(SemanticsGridItem gridItem) {
        this.gridItem = Objects.requireNonNull(gridItem, "gridItem");
    }

    /// Adds a child in document order.
    ///
    /// @param child the child
    void add(LayoutNode child) {
        children.add(Objects.requireNonNull(child, "child"));
    }

    /// Clears per-pass measure and place flags.
    void beginPass() {
        measured = false;
        placed = false;
        measuredConstraints = null;
        for (LayoutNode child : children) {
            child.beginPass();
        }
    }

    /// Measures this node once under the incoming constraints.
    ///
    /// A [CustomLayout] must measure each child through this method at most once per pass.
    /// A second call in the same pass throws [IllegalStateException].
    ///
    /// @param incoming the parent constraints
    /// @return the published size
    public Size measure(Constraints incoming) {
        if (measured) {
            throw new IllegalStateException("Layout node " + name + " was measured more than once");
        }
        Constraints current = incoming;
        for (LayoutModifier modifier : modifiers) {
            current = modifier.apply(current);
        }
        measuredConstraints = current;
        Size inner = switch (kind) {
            case LEAF -> Objects.requireNonNull(intrinsicSize, "intrinsicSize");
            case BOX -> measureBox(current);
            case ROW -> measureRow(current);
            case COLUMN -> measureColumn(current);
            case SCROLL -> measureScroll(current);
            case FLEX -> measureFlex(current);
            case FLOW -> measureFlow(current);
            case GRID -> measureGrid(current);
            case CUSTOM -> measureCustom(current);
            case OVERLAY -> measureOverlay(current, false);
            case PORTAL -> measureOverlay(current, true);
        };
        Size wrapped = inner;
        for (int index = modifiers.size() - 1; index >= 0; index--) {
            wrapped = modifiers.get(index).wrap(wrapped);
        }
        size = incoming.constrain(wrapped.width(), wrapped.height());
        baseline = resolveBaseline();
        alignmentLines = new AlignmentLines(baseline, size.width() * 0.5f, size.height() * 0.5f);
        measured = true;
        return size;
    }

    /// Returns the baseline distance from this node's top edge.
    ///
    /// Leaves use their measured height. Containers use the first child's baseline.
    ///
    /// @return the baseline
    public float baseline() {
        return baseline;
    }

    /// Returns the alignment lines published by the last measure.
    ///
    /// @return the lines
    public AlignmentLines alignmentLines() {
        return alignmentLines;
    }

    /// Computes the published baseline after measure.
    ///
    /// @return the baseline
    private float resolveBaseline() {
        if (kind == LayoutKind.LEAF || children.isEmpty()) {
            return size.height();
        }
        return children.getFirst().baseline();
    }

    /// Places this node at a parent-relative offset and a root origin.
    ///
    /// A [CustomLayout] must place each previously measured child through this method
    /// exactly once per pass.
    ///
    /// @param parentOffset the parent-relative origin
    /// @param rootOrigin the root-relative origin
    public void place(Offset parentOffset, Offset rootOrigin) {
        if (!measured) {
            throw new IllegalStateException("Layout node " + name + " was placed before measure");
        }
        if (placed) {
            throw new IllegalStateException("Layout node " + name + " was placed more than once");
        }
        offset = parentOffset;
        origin = rootOrigin;
        Offset inner = Offset.ZERO;
        for (LayoutModifier modifier : modifiers) {
            inner = inner.plus(modifier.childOrigin());
        }
        Constraints childConstraints = Objects.requireNonNull(measuredConstraints, "measuredConstraints");
        switch (kind) {
            case LEAF -> {
            }
            case BOX -> placeBox(inner, rootOrigin.plus(inner), childConstraints);
            case ROW -> placeRow(inner, rootOrigin.plus(inner), childConstraints);
            case COLUMN -> placeColumn(inner, rootOrigin.plus(inner), childConstraints);
            case SCROLL -> placeScroll(inner, rootOrigin.plus(inner));
            case FLEX -> placeRow(inner, rootOrigin.plus(inner), childConstraints);
            case FLOW -> placeFlow(inner, rootOrigin.plus(inner), childConstraints);
            case GRID -> placeGrid(inner, rootOrigin.plus(inner));
            case CUSTOM -> placeCustom(inner, rootOrigin.plus(inner));
            case OVERLAY, PORTAL -> placeOverlay(inner, rootOrigin.plus(inner));
        }
        placed = true;
    }

    /// Activates this node when it declares [SemanticsAction#ACTIVATE].
    ///
    /// @return whether an activation callback ran
    boolean activate() {
        if (disabled || onActivate == null || !actions.contains(SemanticsAction.ACTIVATE)) {
            return false;
        }
        onActivate.run();
        return true;
    }

    /// Applies one signed adjustment when this node exposes increment or decrement.
    ///
    /// @param delta `1` to increment or `-1` to decrement
    /// @return whether an adjustment callback ran
    boolean adjust(int delta) {
        if (disabled || onAdjust == null || delta == 0) {
            return false;
        }
        SemanticsAction required = delta > 0 ? SemanticsAction.INCREMENT : SemanticsAction.DECREMENT;
        if (!actions.contains(required)) {
            return false;
        }
        onAdjust.accept(delta > 0 ? 1 : -1);
        return true;
    }

    /// Returns the current scroll offset in logical pixels.
    ///
    /// @return the nonnegative offset
    public float scrollOffset() {
        return scrollOffset;
    }

    /// Replaces the scroll offset used by a [LayoutKind#SCROLL] node.
    ///
    /// The next [#place(Offset, Offset)] call applies the new offset. Callers must measure
    /// again so placement flags reset.
    ///
    /// @param offset the nonnegative offset
    public void setScrollOffset(float offset) {
        if (!Float.isFinite(offset) || offset < 0.0f) {
            throw new IllegalArgumentException("Scroll offset must be finite and nonnegative");
        }
        this.scrollOffset = offset;
    }

    /// Measures stacked children.
    ///
    /// @param constraints the inner constraints
    /// @return the stack size
    private Size measureBox(Constraints constraints) {
        float width = constraints.minWidth();
        float height = constraints.minHeight();
        for (LayoutNode child : children) {
            Size childSize = child.measure(constraints);
            width = Math.max(width, childSize.width());
            height = Math.max(height, childSize.height());
        }
        return constraints.constrain(width, height);
    }

    /// Measures children on a horizontal axis.
    ///
    /// @param constraints the inner constraints
    /// @return the row size
    private Size measureRow(Constraints constraints) {
        float width = 0.0f;
        float height = constraints.minHeight();
        float remaining = constraints.maxWidth();
        float rowBaseline = 0.0f;
        float belowBaseline = 0.0f;
        for (LayoutNode child : children) {
            Size childSize = child.measure(Constraints.loose(remaining, constraints.maxHeight()));
            width += childSize.width();
            remaining = Math.max(0.0f, remaining - childSize.width());
            height = Math.max(height, childSize.height());
            rowBaseline = Math.max(rowBaseline, child.baseline());
            belowBaseline = Math.max(belowBaseline, childSize.height() - child.baseline());
        }
        if (alignment == Alignment.BASELINE) {
            height = Math.max(height, rowBaseline + belowBaseline);
        }
        return constraints.constrain(width, height);
    }

    /// Measures scroll content against an unbounded block axis and reports the viewport size.
    ///
    /// @param constraints the inner constraints
    /// @return the viewport size
    private Size measureScroll(Constraints constraints) {
        float width = constraints.minWidth();
        float contentHeight = 0.0f;
        Constraints childConstraints = Constraints.loose(constraints.maxWidth(), Float.MAX_VALUE);
        for (LayoutNode child : children) {
            Size childSize = child.measure(childConstraints);
            width = Math.max(width, childSize.width());
            contentHeight += childSize.height();
        }
        float viewportHeight = constraints.maxHeight() == Float.MAX_VALUE
                ? Math.max(constraints.minHeight(), contentHeight)
                : constraints.maxHeight();
        return constraints.constrain(width, viewportHeight);
    }

    /// Measures children on a horizontal axis and distributes leftover width by grow weights.
    ///
    /// Inflexible children are measured first. Remaining finite width is then split among
    /// children whose [`LayoutModifier.FlexGrow`] weight is positive. When the incoming
    /// maximum width is unbounded, growers measure as loose children.
    ///
    /// @param constraints the inner constraints
    /// @return the flex size
    private Size measureFlex(Constraints constraints) {
        float height = constraints.minHeight();
        float remaining = constraints.maxWidth();
        float totalGrow = 0.0f;
        int growerCount = 0;
        for (LayoutNode child : children) {
            float grow = child.flexGrow();
            if (grow > 0.0f) {
                totalGrow += grow;
                growerCount++;
                continue;
            }
            Size childSize = child.measure(Constraints.loose(remaining, constraints.maxHeight()));
            remaining = Math.max(0.0f, remaining - childSize.width());
            height = Math.max(height, childSize.height());
        }
        float leftover = remaining;
        float assigned = 0.0f;
        int seen = 0;
        for (LayoutNode child : children) {
            float grow = child.flexGrow();
            if (grow <= 0.0f) {
                continue;
            }
            seen++;
            Constraints childConstraints;
            if (leftover == Float.MAX_VALUE || totalGrow <= 0.0f) {
                childConstraints = Constraints.loose(leftover, constraints.maxHeight());
            } else {
                float share = seen == growerCount ? leftover - assigned : leftover * (grow / totalGrow);
                assigned += share;
                childConstraints = new Constraints(share, share, 0.0f, constraints.maxHeight());
            }
            Size childSize = child.measure(childConstraints);
            height = Math.max(height, childSize.height());
        }
        if (alignment == Alignment.BASELINE) {
            float rowBaseline = 0.0f;
            float belowBaseline = 0.0f;
            for (LayoutNode child : children) {
                rowBaseline = Math.max(rowBaseline, child.baseline());
                belowBaseline = Math.max(belowBaseline, child.size.height() - child.baseline());
            }
            height = Math.max(height, rowBaseline + belowBaseline);
        }
        float used = 0.0f;
        for (LayoutNode child : children) {
            used += child.size.width();
        }
        float width = growerCount > 0 && constraints.maxWidth() != Float.MAX_VALUE
                ? constraints.maxWidth()
                : used;
        return constraints.constrain(width, height);
    }

    /// Measures wrapping rows that fill the available width.
    ///
    /// @param constraints the inner constraints
    /// @return the flow size
    private Size measureFlow(Constraints constraints) {
        float maxWidth = constraints.maxWidth();
        float x = 0.0f;
        float lineHeight = 0.0f;
        float lineBaseline = 0.0f;
        float lineBelow = 0.0f;
        float width = constraints.minWidth();
        float height = 0.0f;
        for (LayoutNode child : children) {
            Size childSize = child.measure(Constraints.loose(maxWidth, constraints.maxHeight()));
            if (x > 0.0f && maxWidth != Float.MAX_VALUE && x + childSize.width() > maxWidth) {
                width = Math.max(width, x);
                height += flowLineHeight(lineHeight, lineBaseline, lineBelow);
                x = 0.0f;
                lineHeight = 0.0f;
                lineBaseline = 0.0f;
                lineBelow = 0.0f;
            }
            x += childSize.width();
            lineHeight = Math.max(lineHeight, childSize.height());
            lineBaseline = Math.max(lineBaseline, child.baseline());
            lineBelow = Math.max(lineBelow, childSize.height() - child.baseline());
        }
        width = Math.max(width, x);
        height += flowLineHeight(lineHeight, lineBaseline, lineBelow);
        return constraints.constrain(width, Math.max(constraints.minHeight(), height));
    }

    /// Returns the height of one wrapping line.
    ///
    /// @param lineHeight the maximum child height
    /// @param lineBaseline the maximum published baseline
    /// @param lineBelow the maximum distance below the baseline
    /// @return the line height
    private float flowLineHeight(float lineHeight, float lineBaseline, float lineBelow) {
        if (alignment == Alignment.BASELINE) {
            return Math.max(lineHeight, lineBaseline + lineBelow);
        }
        return lineHeight;
    }

    /// Measures a fixed-column grid from child intrinsic sizes.
    ///
    /// When [`Alignment#BASELINE`] is set, each row height is the maximum published
    /// baseline plus the maximum distance below that baseline.
    ///
    /// @param constraints the inner constraints
    /// @return the grid size
    private Size measureGrid(Constraints constraints) {
        int columns = gridColumns();
        float[] columnWidths = new float[columns];
        float height = 0.0f;
        float rowHeight = 0.0f;
        float rowBaseline = 0.0f;
        float rowBelow = 0.0f;
        int column = 0;
        for (LayoutNode child : children) {
            Size childSize = child.measure(Constraints.loose(constraints.maxWidth(), constraints.maxHeight()));
            columnWidths[column] = Math.max(columnWidths[column], childSize.width());
            rowHeight = Math.max(rowHeight, childSize.height());
            rowBaseline = Math.max(rowBaseline, child.baseline());
            rowBelow = Math.max(rowBelow, childSize.height() - child.baseline());
            column++;
            if (column == columns) {
                height += gridRowHeight(rowHeight, rowBaseline, rowBelow);
                rowHeight = 0.0f;
                rowBaseline = 0.0f;
                rowBelow = 0.0f;
                column = 0;
            }
        }
        if (column != 0) {
            height += gridRowHeight(rowHeight, rowBaseline, rowBelow);
        }
        float width = 0.0f;
        for (float columnWidth : columnWidths) {
            width += columnWidth;
        }
        return constraints.constrain(
                Math.max(constraints.minWidth(), width),
                Math.max(constraints.minHeight(), height)
        );
    }

    /// Returns the first [`LayoutModifier.FlexGrow`] weight, or `0` when absent.
    ///
    /// @return the nonnegative weight
    private float flexGrow() {
        for (LayoutModifier modifier : modifiers) {
            if (modifier instanceof LayoutModifier.FlexGrow grow) {
                return grow.weight();
            }
        }
        return 0.0f;
    }

    /// Returns the [`LayoutModifier.GridColumns`] count, or `1` when absent.
    ///
    /// @return the positive column count
    private int gridColumns() {
        for (LayoutModifier modifier : modifiers) {
            if (modifier instanceof LayoutModifier.GridColumns columns) {
                return columns.columns();
            }
        }
        return 1;
    }

    /// Measures children on a vertical axis.
    ///
    /// @param constraints the inner constraints
    /// @return the column size
    private Size measureColumn(Constraints constraints) {
        float width = constraints.minWidth();
        float height = 0.0f;
        float remaining = constraints.maxHeight();
        for (LayoutNode child : children) {
            Size childSize = child.measure(Constraints.loose(constraints.maxWidth(), remaining));
            height += childSize.height();
            remaining = Math.max(0.0f, remaining - childSize.height());
            width = Math.max(width, childSize.width());
        }
        return constraints.constrain(width, height);
    }

    /// Places stacked children.
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    /// @param constraints the inner constraints
    private void placeBox(Offset inner, Offset root, Constraints constraints) {
        for (LayoutNode child : children) {
            float x = alignment.place(constraints.maxWidth(), child.size.width());
            float y = Alignment.START.place(constraints.maxHeight(), child.size.height());
            Offset childOffset = inner.plus(new Offset(x, y));
            child.place(childOffset, root.plus(new Offset(x, y)));
        }
    }

    /// Places row children.
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    /// @param constraints the inner constraints
    private void placeRow(Offset inner, Offset root, Constraints constraints) {
        float rowBaseline = 0.0f;
        if (alignment == Alignment.BASELINE) {
            for (LayoutNode child : children) {
                rowBaseline = Math.max(rowBaseline, child.baseline());
            }
        }
        if (readingDirection() == TextDirection.RTL) {
            float x = size.width();
            for (LayoutNode child : children) {
                x -= child.size.width();
                float y = rowCrossAxis(child, rowBaseline);
                Offset childOffset = inner.plus(new Offset(x, y));
                child.place(childOffset, root.plus(new Offset(x, y)));
            }
            return;
        }
        float x = 0.0f;
        for (LayoutNode child : children) {
            float y = rowCrossAxis(child, rowBaseline);
            Offset childOffset = inner.plus(new Offset(x, y));
            child.place(childOffset, root.plus(new Offset(x, y)));
            x += child.size.width();
        }
    }

    /// Returns the row cross-axis origin for `child`.
    ///
    /// @param child the child
    /// @param rowBaseline the maximum published baseline when [`Alignment#BASELINE`] is set
    /// @return the Y origin relative to the row
    private float rowCrossAxis(LayoutNode child, float rowBaseline) {
        if (alignment == Alignment.BASELINE) {
            return Math.max(0.0f, rowBaseline - child.baseline());
        }
        return alignment.place(size.height(), child.size.height());
    }

    /// Returns the reading direction declared on this node.
    ///
    /// @return the direction, defaulting to LTR
    private TextDirection readingDirection() {
        for (LayoutModifier modifier : modifiers) {
            if (modifier instanceof LayoutModifier.ReadingDirection direction) {
                return direction.direction();
            }
        }
        return TextDirection.LTR;
    }

    /// Places scroll children using the stored offset.
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    private void placeScroll(Offset inner, Offset root) {
        float y = -scrollOffset;
        for (LayoutNode child : children) {
            Offset childOffset = inner.plus(new Offset(0.0f, y));
            child.place(childOffset, root.plus(new Offset(0.0f, y)));
            y += child.size.height();
        }
    }

    /// Places column children.
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    /// @param constraints the inner constraints
    private void placeColumn(Offset inner, Offset root, Constraints constraints) {
        float y = 0.0f;
        for (LayoutNode child : children) {
            float x = alignment.place(size.width(), child.size.width());
            Offset childOffset = inner.plus(new Offset(x, y));
            child.place(childOffset, root.plus(new Offset(x, y)));
            y += child.size.height();
        }
    }

    /// Places wrapping flow children.
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    /// @param constraints the inner constraints
    private void placeFlow(Offset inner, Offset root, Constraints constraints) {
        float maxWidth = constraints.maxWidth();
        boolean rtl = readingDirection() == TextDirection.RTL;
        int index = 0;
        int count = children.size();
        float y = 0.0f;
        while (index < count) {
            int start = index;
            float x = 0.0f;
            float lineHeight = 0.0f;
            float lineBaseline = 0.0f;
            float lineBelow = 0.0f;
            while (index < count) {
                LayoutNode child = children.get(index);
                if (x > 0.0f && maxWidth != Float.MAX_VALUE && x + child.size.width() > maxWidth) {
                    break;
                }
                x += child.size.width();
                lineHeight = Math.max(lineHeight, child.size.height());
                lineBaseline = Math.max(lineBaseline, child.baseline());
                lineBelow = Math.max(lineBelow, child.size.height() - child.baseline());
                index++;
            }
            float used = 0.0f;
            for (int childIndex = start; childIndex < index; childIndex++) {
                LayoutNode child = children.get(childIndex);
                float placedX = rtl ? size.width() - used - child.size.width() : used;
                float placedY = y + flowCrossAxis(child, lineHeight, lineBaseline);
                Offset childOffset = inner.plus(new Offset(placedX, placedY));
                child.place(childOffset, root.plus(new Offset(placedX, placedY)));
                used += child.size.width();
            }
            y += flowLineHeight(lineHeight, lineBaseline, lineBelow);
        }
    }

    /// Returns the in-line cross-axis origin for a wrapping child.
    ///
    /// @param child the child
    /// @param lineHeight the line height
    /// @param lineBaseline the line baseline
    /// @return the Y origin relative to the line
    private float flowCrossAxis(LayoutNode child, float lineHeight, float lineBaseline) {
        if (alignment == Alignment.BASELINE) {
            return Math.max(0.0f, lineBaseline - child.baseline());
        }
        return alignment.place(lineHeight, child.size.height());
    }

    /// Places fixed-column grid children.
    ///
    /// Each child is aligned inside its cell. [`Alignment#BASELINE`] uses published
    /// alignment lines on the row cross-axis and [`Alignment#place(float, float)`] for
    /// leftover column width.
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    private void placeGrid(Offset inner, Offset root) {
        int columns = gridColumns();
        float[] columnWidths = new float[columns];
        int index = 0;
        for (LayoutNode child : children) {
            int column = index % columns;
            columnWidths[column] = Math.max(columnWidths[column], child.size.width());
            index++;
        }
        boolean rtl = readingDirection() == TextDirection.RTL;
        float x = rtl ? size.width() : 0.0f;
        float y = 0.0f;
        float rowHeight = 0.0f;
        float rowBaseline = 0.0f;
        int column = 0;
        index = 0;
        for (LayoutNode child : children) {
            if (column == 0) {
                rowHeight = 0.0f;
                rowBaseline = 0.0f;
                float rowBelow = 0.0f;
                for (int look = index; look < children.size() && look - index < columns; look++) {
                    LayoutNode rowChild = children.get(look);
                    rowHeight = Math.max(rowHeight, rowChild.size.height());
                    rowBaseline = Math.max(rowBaseline, rowChild.baseline());
                    rowBelow = Math.max(rowBelow, rowChild.size.height() - rowChild.baseline());
                }
                rowHeight = gridRowHeight(rowHeight, rowBaseline, rowBelow);
            }
            if (rtl) {
                x -= columnWidths[column];
            }
            float placedX = x + alignment.place(columnWidths[column], child.size.width());
            float placedY = y + gridCrossAxis(child, rowHeight, rowBaseline);
            Offset childOffset = inner.plus(new Offset(placedX, placedY));
            child.place(childOffset, root.plus(new Offset(placedX, placedY)));
            if (!rtl) {
                x += columnWidths[column];
            }
            column++;
            index++;
            if (column == columns) {
                y += rowHeight;
                x = rtl ? size.width() : 0.0f;
                column = 0;
            }
        }
    }

    /// Returns the in-cell cross-axis origin for a grid child.
    ///
    /// @param child the child
    /// @param rowHeight the row height
    /// @param rowBaseline the maximum published baseline when [`Alignment#BASELINE`] is set
    /// @return the Y origin relative to the row
    private float gridCrossAxis(LayoutNode child, float rowHeight, float rowBaseline) {
        if (alignment == Alignment.BASELINE) {
            return Math.max(0.0f, rowBaseline - child.baseline());
        }
        return alignment.place(rowHeight, child.size.height());
    }

    /// Returns the height of one grid row.
    ///
    /// @param rowHeight the maximum child height
    /// @param rowBaseline the maximum published baseline
    /// @param rowBelow the maximum distance below the published baseline
    /// @return the row height
    private float gridRowHeight(float rowHeight, float rowBaseline, float rowBelow) {
        if (alignment == Alignment.BASELINE) {
            return Math.max(rowHeight, rowBaseline + rowBelow);
        }
        return rowHeight;
    }

    /// Measures children through [`#customLayout`].
    ///
    /// @param constraints the inner constraints
    /// @return the custom size
    private Size measureCustom(Constraints constraints) {
        CustomLayout delegate = Objects.requireNonNull(customLayout, "customLayout");
        return delegate.measure(constraints, List.copyOf(children));
    }

    /// Measures overlay or portal children.
    ///
    /// Overlay size is the union of each child's box at its overlay offset. Portal size is
    /// the first child's size, or zero when empty.
    ///
    /// @param constraints the inner constraints
    /// @param portal whether trailing children are excluded from the slot size
    /// @return the overlay size
    private Size measureOverlay(Constraints constraints, boolean portal) {
        float width = constraints.minWidth();
        float height = constraints.minHeight();
        int index = 0;
        for (LayoutNode child : children) {
            Size childSize = child.measure(Constraints.loose(constraints.maxWidth(), constraints.maxHeight()));
            Offset placed = overlayOffset(child);
            if (portal) {
                if (index == 0) {
                    width = Math.max(width, childSize.width());
                    height = Math.max(height, childSize.height());
                }
            } else {
                width = Math.max(width, placed.x() + childSize.width());
                height = Math.max(height, placed.y() + childSize.height());
            }
            index++;
        }
        return constraints.constrain(width, height);
    }

    /// Places children through [`#customLayout`].
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    private void placeCustom(Offset inner, Offset root) {
        CustomLayout delegate = Objects.requireNonNull(customLayout, "customLayout");
        delegate.place(inner, root, size, List.copyOf(children));
    }

    /// Places overlay or portal children at their overlay offsets.
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    private void placeOverlay(Offset inner, Offset root) {
        for (LayoutNode child : children) {
            Offset placed = overlayOffset(child);
            Offset childOffset = inner.plus(placed);
            child.place(childOffset, root.plus(placed));
        }
    }

    /// Returns the [`LayoutModifier.OverlayOffset`] of `child`, or zero.
    ///
    /// @param child the child
    /// @return the offset
    private static Offset overlayOffset(LayoutNode child) {
        for (LayoutModifier modifier : child.modifiers) {
            if (modifier instanceof LayoutModifier.OverlayOffset offset) {
                return new Offset(offset.x(), offset.y());
            }
        }
        return Offset.ZERO;
    }
}
