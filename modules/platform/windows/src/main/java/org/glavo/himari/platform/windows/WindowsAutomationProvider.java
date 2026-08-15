package org.glavo.himari.platform.windows;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import java.util.UUID;

/// Implements `IRawElementProviderSimple` plus Invoke, Toggle, RangeValue, and Text COM patterns.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsAutomationProvider implements AutoCloseable {
    /// `IUnknown`.
    private static final UUID IUNKNOWN = UUID.fromString("00000000-0000-0000-c000-000000000046");

    /// `IRawElementProviderSimple`.
    private static final UUID IRAW_ELEMENT_PROVIDER_SIMPLE =
            UUID.fromString("d6dd68d1-86fd-4332-8666-9abedea2d24c");

    /// `IInvokeProvider`.
    private static final UUID IINVOKE_PROVIDER = UUID.fromString("619be086-1f4e-4ee4-839e-4544a54da35d");

    /// `IToggleProvider`.
    private static final UUID ITOGGLE_PROVIDER = UUID.fromString("56d00bd0-c4f4-433c-a836-1a52a57e0892");

    /// `IRangeValueProvider`.
    private static final UUID IRANGE_VALUE_PROVIDER = UUID.fromString("36dc7aef-33e6-4691-afe1-2be7274b3d33");

    /// `ITextProvider`.
    private static final UUID ITEXT_PROVIDER = UUID.fromString("3589c92c-63f3-4367-99bb-ada653b77cf2");

    /// `ITextRangeProvider`.
    private static final UUID ITEXT_RANGE_PROVIDER = UUID.fromString("534729dc-411e-4aaa-9d3a-eb1d1d2c9d87");

    /// `UIA_ControlTypePropertyId`.
    static final int UIA_CONTROL_TYPE_PROPERTY_ID = 30003;

    /// `UIA_LiveSettingPropertyId`.
    static final int UIA_LIVE_SETTING_PROPERTY_ID = 30135;

    /// `UIA_ButtonControlTypeId`.
    static final int UIA_BUTTON_CONTROL_TYPE_ID = 50000;

    /// `UIA_StatusBarControlTypeId`.
    static final int UIA_STATUS_BAR_CONTROL_TYPE_ID = 50017;

    /// `LiveSetting_Off`.
    static final int LIVE_SETTING_OFF = 0;

    /// `LiveSetting_Polite`.
    static final int LIVE_SETTING_POLITE = 1;

    /// `LiveSetting_Assertive`.
    static final int LIVE_SETTING_ASSERTIVE = 2;

    /// `UIA_InvokePatternId`.
    static final int UIA_INVOKE_PATTERN_ID = 10000;

    /// `UIA_RangeValuePatternId`.
    static final int UIA_RANGE_VALUE_PATTERN_ID = 10003;

    /// `UIA_TextPatternId`.
    static final int UIA_TEXT_PATTERN_ID = 10014;

    /// `UIA_TogglePatternId`.
    static final int UIA_TOGGLE_PATTERN_ID = 10015;

    /// `SupportedTextSelection_Single`.
    static final int SUPPORTED_TEXT_SELECTION_SINGLE = 1;

    /// `ToggleState_Off`.
    static final int TOGGLE_STATE_OFF = 0;

    /// `ToggleState_On`.
    static final int TOGGLE_STATE_ON = 1;

    /// `ToggleState_Indeterminate`.
    static final int TOGGLE_STATE_INDETERMINATE = 2;

    /// `VT_I4`.
    private static final int VT_I4 = 3;

    /// `S_OK`.
    private static final int S_OK = 0;

    /// `E_NOINTERFACE`.
    private static final int E_NOINTERFACE = 0x8000_4002;

    /// `E_POINTER`.
    private static final int E_POINTER = 0x8000_4003;

    /// `E_NOTIMPL`.
    private static final int E_NOTIMPL = 0x8000_4001;

    /// Arena owning the COM objects.
    private final Arena arena;

    /// Contained callback failures.
    private final CallbackFailureQueue failures = new CallbackFailureQueue();

    /// Projected semantics node.
    private final SemanticsNode node;

    /// Simple provider COM object.
    private final MemorySegment simpleObject;

    /// Simple provider vtable.
    private final MemorySegment simpleVtable;

    /// Invoke provider COM object.
    private final MemorySegment invokeObject;

    /// Toggle provider COM object.
    private final MemorySegment toggleObject;

    /// Range provider COM object.
    private final MemorySegment rangeObject;

    /// Text provider COM object.
    private final MemorySegment textObject;

    /// Document text-range COM object.
    private final MemorySegment textRangeObject;

    /// Outstanding references for the simple provider.
    private int references = 1;

    /// Number of `IInvokeProvider::Invoke` calls.
    private int invokeCount;

    /// Live toggle state.
    private int toggleState;

    /// Live range value.
    private double rangeValue;

    /// Whether closed.
    private boolean closed;

    /// Creates one provider.
    private WindowsAutomationProvider(Win32FfmBindings bindings, SemanticsNode node) {
        this.node = node;
        this.toggleState = initialToggleState(node);
        this.rangeValue = node.rangeValue() == null ? 0.0 : node.rangeValue();
        this.arena = Arena.ofConfined();
        this.simpleVtable = arena.allocate(ValueLayout.ADDRESS, 6);
        this.simpleObject = arena.allocate(ValueLayout.ADDRESS);
        simpleObject.set(ValueLayout.ADDRESS, 0L, simpleVtable);
        simpleVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryInterface, failures, arena));
        simpleVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        simpleVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        simpleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createIrawElementProviderGetPatternProviderStub(this::getPatternProvider, failures, arena)
        );
        simpleVtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIrawElementProviderGetPropertyValueStub(this::getPropertyValue, failures, arena)
        );
        this.invokeObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment invokeVtable = arena.allocate(ValueLayout.ADDRESS, 4);
        invokeObject.set(ValueLayout.ADDRESS, 0L, invokeVtable);
        invokeVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryInvoke, failures, arena));
        invokeVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        invokeVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        invokeVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIinvokeProviderInvokeStub(this::invoke, failures, arena));
        this.toggleObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment toggleVtable = arena.allocate(ValueLayout.ADDRESS, 5);
        toggleObject.set(ValueLayout.ADDRESS, 0L, toggleVtable);
        toggleVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryToggle, failures, arena));
        toggleVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        toggleVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        toggleVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createItoggleProviderToggleStub(this::toggle, failures, arena));
        toggleVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createItoggleProviderGetToggleStateStub(this::getToggleState, failures, arena));
        this.rangeObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment rangeVtable = arena.allocate(ValueLayout.ADDRESS, 8);
        rangeObject.set(ValueLayout.ADDRESS, 0L, rangeVtable);
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryRange, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIrangeValueProviderSetValueStub(this::setRangeValue, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIrangeValueProviderGetValueStub(this::getRangeValue, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 5L, bindings.createIrangeValueProviderGetIsReadOnlyStub(this::getRangeReadOnly, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 6L, bindings.createIrangeValueProviderGetMaximumStub(this::getRangeMaximum, failures, arena));
        rangeVtable.setAtIndex(ValueLayout.ADDRESS, 7L, bindings.createIrangeValueProviderGetMinimumStub(this::getRangeMinimum, failures, arena));
        this.textObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment textVtable = arena.allocate(ValueLayout.ADDRESS, 9);
        textObject.set(ValueLayout.ADDRESS, 0L, textVtable);
        textVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryText, failures, arena));
        textVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        textVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        textVtable.setAtIndex(
                ValueLayout.ADDRESS,
                3L,
                bindings.createItextProviderRangeFromPointStub(this::rangeFromPoint, failures, arena)
        );
        MemorySegment emptyRange = bindings.createItextProviderGetRangeStub(this::emptyRange, failures, arena);
        textVtable.setAtIndex(ValueLayout.ADDRESS, 4L, emptyRange);
        textVtable.setAtIndex(ValueLayout.ADDRESS, 5L, emptyRange);
        textVtable.setAtIndex(ValueLayout.ADDRESS, 6L, emptyRange);
        textVtable.setAtIndex(
                ValueLayout.ADDRESS,
                7L,
                bindings.createItextProviderGetRangeStub(this::documentRange, failures, arena)
        );
        textVtable.setAtIndex(
                ValueLayout.ADDRESS,
                8L,
                bindings.createItextProviderGetSupportedTextSelectionStub(this::supportedTextSelection, failures, arena)
        );
        this.textRangeObject = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment textRangeVtable = arena.allocate(ValueLayout.ADDRESS, 13);
        textRangeObject.set(ValueLayout.ADDRESS, 0L, textRangeVtable);
        textRangeVtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryTextRange, failures, arena));
        textRangeVtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        textRangeVtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        MemorySegment notImplemented = bindings.createItextRangeNotimplStub(this::notImplemented, failures, arena);
        MemorySegment cloneSlot = bindings.createItextProviderGetRangeStub(this::cloneRange, failures, arena);
        textRangeVtable.setAtIndex(ValueLayout.ADDRESS, 3L, cloneSlot);
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                4L,
                bindings.createItextRangeProviderCompareStub(this::compareRange, failures, arena)
        );
        for (int slot = 5; slot < 11; slot++) {
            textRangeVtable.setAtIndex(ValueLayout.ADDRESS, slot, notImplemented);
        }
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                11L,
                bindings.createItextProviderGetRangeStub(this::enclosingElement, failures, arena)
        );
        textRangeVtable.setAtIndex(
                ValueLayout.ADDRESS,
                12L,
                bindings.createItextRangeProviderGetTextStub(this::getText, failures, arena)
        );
    }

    /// Creates a provider for one semantics node.
    ///
    /// @param libraries the session libraries
    /// @param node the projected node
    /// @return the provider
    public static WindowsAutomationProvider of(WindowsLibraries libraries, SemanticsNode node) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(node, "node");
        return new WindowsAutomationProvider(libraries.bindings(), node);
    }

    /// Invokes `GetPropertyValue` through the generated COM vtable.
    ///
    /// @param propertyId the UIA property identifier
    /// @return the `VT_I4` payload
    public int invokePropertyValue(int propertyId) {
        requireOpen();
        MemorySegment getProperty = simpleVtable.getAtIndex(ValueLayout.ADDRESS, 5L);
        MemorySegment value = arena.allocate(Win32Layouts.VARIANT);
        value.fill((byte) 0);
        int result = Win32FfmBindings.invokeIrawElementProviderGetPropertyValuePointer(
                getProperty,
                simpleObject,
                propertyId,
                value
        );
        requireSuccess("GetPropertyValue", result);
        return value.get(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET);
    }

    /// Invokes `GetPatternProvider` through the generated COM vtable.
    ///
    /// @param patternId the UIA pattern identifier
    /// @return whether a pattern object was returned
    public boolean invokePatternProvider(int patternId) {
        requireOpen();
        MemorySegment getPattern = simpleVtable.getAtIndex(ValueLayout.ADDRESS, 4L);
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "GetPatternProvider",
                Win32FfmBindings.invokeIrawElementProviderGetPatternProviderPointer(
                        getPattern,
                        simpleObject,
                        patternId,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `IInvokeProvider::Invoke` through the generated COM vtable.
    ///
    /// @return the invoke count after the call
    public int invoke() {
        requireOpen();
        requireSuccess(
                "IInvokeProvider::Invoke",
                Win32FfmBindings.invokeIinvokeProviderInvokePointer(
                        functionAt(invokeObject, 3),
                        invokeObject
                )
        );
        return invokeCount;
    }

    /// Invokes `IToggleProvider::Toggle` through the generated COM vtable.
    ///
    /// @return the new toggle state
    public int toggle() {
        requireOpen();
        requireSuccess(
                "IToggleProvider::Toggle",
                Win32FfmBindings.invokeItoggleProviderTogglePointer(
                        functionAt(toggleObject, 3),
                        toggleObject
                )
        );
        return toggleState;
    }

    /// Reads `IToggleProvider::get_ToggleState` through the generated COM vtable.
    ///
    /// @return the toggle state
    public int toggleState() {
        requireOpen();
        MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
        state.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "IToggleProvider::get_ToggleState",
                Win32FfmBindings.invokeItoggleProviderGetToggleStatePointer(
                        functionAt(toggleObject, 4),
                        toggleObject,
                        state
                )
        );
        return state.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `IRangeValueProvider::SetValue` through the generated COM vtable.
    ///
    /// @param value the new value
    /// @return the stored value
    public double setRangeValue(double value) {
        requireOpen();
        requireSuccess(
                "IRangeValueProvider::SetValue",
                Win32FfmBindings.invokeIrangeValueProviderSetValuePointer(
                        functionAt(rangeObject, 3),
                        rangeObject,
                        value
                )
        );
        return rangeValue;
    }

    /// Reads `IRangeValueProvider::get_Value` through the generated COM vtable.
    ///
    /// @return the stored value
    public double rangeValue() {
        requireOpen();
        MemorySegment value = arena.allocate(ValueLayout.JAVA_DOUBLE);
        value.set(ValueLayout.JAVA_DOUBLE, 0L, 0.0);
        requireSuccess(
                "IRangeValueProvider::get_Value",
                Win32FfmBindings.invokeIrangeValueProviderGetValuePointer(
                        functionAt(rangeObject, 4),
                        rangeObject,
                        value
                )
        );
        return value.get(ValueLayout.JAVA_DOUBLE, 0L);
    }

    /// Invokes `ITextProvider::get_DocumentRange` through the generated COM vtable.
    ///
    /// @return whether a document range was returned
    public boolean invokeDocumentRange() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextProvider::get_DocumentRange",
                Win32FfmBindings.invokeItextProviderGetRangePointer(
                        functionAt(textObject, 7),
                        textObject,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `ITextProvider::get_SupportedTextSelection` through the generated COM vtable.
    ///
    /// @return the supported-selection identifier
    public int invokeSupportedTextSelection() {
        requireOpen();
        MemorySegment value = arena.allocate(ValueLayout.JAVA_INT);
        value.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITextProvider::get_SupportedTextSelection",
                Win32FfmBindings.invokeItextProviderGetSupportedTextSelectionPointer(
                        functionAt(textObject, 8),
                        textObject,
                        value
                )
        );
        return value.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ITextRangeProvider::Clone` through the generated COM vtable.
    ///
    /// @return whether a range object was returned
    public boolean invokeClone() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextRangeProvider::Clone",
                Win32FfmBindings.invokeItextProviderGetRangePointer(
                        functionAt(textRangeObject, 3),
                        textRangeObject,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Invokes `ITextRangeProvider::Compare` through the generated COM vtable.
    ///
    /// @return whether the range compared equal to itself
    public boolean invokeCompareSelf() {
        requireOpen();
        MemorySegment equal = arena.allocate(ValueLayout.JAVA_INT);
        equal.set(ValueLayout.JAVA_INT, 0L, 0);
        requireSuccess(
                "ITextRangeProvider::Compare",
                Win32FfmBindings.invokeItextRangeProviderComparePointer(
                        functionAt(textRangeObject, 4),
                        textRangeObject,
                        textRangeObject,
                        equal
                )
        );
        return equal.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Invokes `ITextRangeProvider::GetEnclosingElement` through the generated COM vtable.
    ///
    /// @return whether the raw element provider was returned
    public boolean invokeEnclosingElement() {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextRangeProvider::GetEnclosingElement",
                Win32FfmBindings.invokeItextProviderGetRangePointer(
                        functionAt(textRangeObject, 11),
                        textRangeObject,
                        result
                )
        );
        return result.get(ValueLayout.ADDRESS, 0L).address() != 0L;
    }

    /// Reads `ITextRangeProvider::GetText` through the generated COM vtable.
    ///
    /// @param maxLength the maximum UTF-16 length, or `-1` for the full document
    /// @return the document text
    public String invokeGetText(int maxLength) {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.ADDRESS);
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        requireSuccess(
                "ITextRangeProvider::GetText",
                Win32FfmBindings.invokeItextRangeProviderGetTextPointer(
                        functionAt(textRangeObject, 12),
                        textRangeObject,
                        maxLength,
                        result
                )
        );
        MemorySegment chars = result.get(ValueLayout.ADDRESS, 0L);
        if (chars.address() == 0L) {
            return "";
        }
        long available = chars.byteSize();
        if (available == 0L) {
            chars = chars.reinterpret(4096);
            available = 4096;
        }
        int limit = Math.toIntExact(Math.min(2048L, available / 2L));
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < limit; index++) {
            char unit = chars.getAtIndex(ValueLayout.JAVA_CHAR, index);
            if (unit == 0) {
                break;
            }
            text.append(unit);
        }
        return text.toString();
    }

    /// Returns the number of successful invoke calls.
    ///
    /// @return the count
    public int invokeCount() {
        return invokeCount;
    }

    /// Releases this owner's COM reference.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        release(simpleObject);
        arena.close();
    }

    /// Implements `IRawElementProviderSimple::QueryInterface`.
    private int queryInterface(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IRAW_ELEMENT_PROVIDER_SIMPLE, simpleObject);
    }

    /// Implements Invoke QI.
    private int queryInvoke(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IINVOKE_PROVIDER, invokeObject);
    }

    /// Implements Toggle QI.
    private int queryToggle(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITOGGLE_PROVIDER, toggleObject);
    }

    /// Implements RangeValue QI.
    private int queryRange(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, IRANGE_VALUE_PROVIDER, rangeObject);
    }

    /// Implements Text QI.
    private int queryText(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITEXT_PROVIDER, textObject);
    }

    /// Implements TextRange QI.
    private int queryTextRange(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        return query(interfaceId, result, ITEXT_RANGE_PROVIDER, textRangeObject);
    }

    /// Shared QI implementation for one identity.
    private int query(MemorySegment interfaceId, MemorySegment result, UUID identity, MemorySegment object) {
        if (result.address() == 0L) {
            return E_POINTER;
        }
        if (WindowsCom.matches(interfaceId, IUNKNOWN) || WindowsCom.matches(interfaceId, identity)) {
            result.set(ValueLayout.ADDRESS, 0L, object);
            addRef(object);
            return S_OK;
        }
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return E_NOINTERFACE;
    }

    /// Implements `IUnknown::AddRef`.
    private int addRef(MemorySegment self) {
        references = Math.incrementExact(references);
        return references;
    }

    /// Implements `IUnknown::Release`.
    private int release(MemorySegment self) {
        references = Math.max(0, references - 1);
        return references;
    }

    /// Implements `IRawElementProviderSimple::GetPatternProvider`.
    private int getPatternProvider(MemorySegment self, int patternId, MemorySegment provider) {
        if (provider.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment out = provider.reinterpret(ValueLayout.ADDRESS.byteSize());
        MemorySegment selected = MemorySegment.NULL;
        if (patternId == UIA_INVOKE_PATTERN_ID && node.actions().contains(SemanticsAction.ACTIVATE)) {
            selected = invokeObject;
        } else if (patternId == UIA_TOGGLE_PATTERN_ID && node.role() == SemanticsRole.TOGGLE) {
            selected = toggleObject;
        } else if (patternId == UIA_RANGE_VALUE_PATTERN_ID && node.role() == SemanticsRole.SLIDER) {
            selected = rangeObject;
        } else if (patternId == UIA_TEXT_PATTERN_ID && node.textRange() != null) {
            selected = textObject;
        }
        out.set(ValueLayout.ADDRESS, 0L, selected);
        if (selected.address() != 0L) {
            addRef(selected);
        }
        return S_OK;
    }

    /// Implements `IRawElementProviderSimple::GetPropertyValue`.
    private int getPropertyValue(MemorySegment self, int propertyId, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment variant = value.reinterpret(Win32Layouts.VARIANT.byteSize());
        variant.fill((byte) 0);
        if (propertyId == UIA_CONTROL_TYPE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, controlTypeId(node));
        } else if (propertyId == UIA_LIVE_SETTING_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET, liveSettingId(node));
        }
        return S_OK;
    }

    /// Implements `IInvokeProvider::Invoke`.
    private int invoke(MemorySegment self) {
        invokeCount++;
        return S_OK;
    }

    /// Implements `IToggleProvider::Toggle`.
    private int toggle(MemorySegment self) {
        toggleState = toggleState == TOGGLE_STATE_ON ? TOGGLE_STATE_OFF : TOGGLE_STATE_ON;
        return S_OK;
    }

    /// Implements `IToggleProvider::get_ToggleState`.
    private int getToggleState(MemorySegment self, MemorySegment state) {
        if (state.address() == 0L) {
            return E_POINTER;
        }
        state.set(ValueLayout.JAVA_INT, 0L, toggleState);
        return S_OK;
    }

    /// Implements `IRangeValueProvider::SetValue`.
    private int setRangeValue(MemorySegment self, double value) {
        if (!Double.isFinite(value)) {
            return E_POINTER;
        }
        rangeValue = value;
        return S_OK;
    }

    /// Implements `IRangeValueProvider::get_Value`.
    private int getRangeValue(MemorySegment self, MemorySegment value) {
        return writeDouble(value, rangeValue);
    }

    /// Implements `IRangeValueProvider::get_IsReadOnly`.
    private int getRangeReadOnly(MemorySegment self, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        value.set(ValueLayout.JAVA_INT, 0L, 0);
        return S_OK;
    }

    /// Implements `IRangeValueProvider::get_Maximum`.
    private int getRangeMaximum(MemorySegment self, MemorySegment value) {
        return writeDouble(value, 100.0);
    }

    /// Implements `IRangeValueProvider::get_Minimum`.
    private int getRangeMinimum(MemorySegment self, MemorySegment value) {
        return writeDouble(value, 0.0);
    }

    /// Implements `ITextProvider::RangeFromPoint` with an honest empty result.
    private int rangeFromPoint(MemorySegment self, double x, double y, MemorySegment range) {
        return emptyRange(self, range);
    }

    /// Writes a null range out-parameter.
    private int emptyRange(MemorySegment self, MemorySegment range) {
        if (range.address() == 0L) {
            return E_POINTER;
        }
        range.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return S_OK;
    }

    /// Implements `ITextProvider::get_DocumentRange`.
    private int documentRange(MemorySegment self, MemorySegment range) {
        if (range.address() == 0L) {
            return E_POINTER;
        }
        range.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, textRangeObject);
        addRef(textRangeObject);
        return S_OK;
    }

    /// Implements `ITextProvider::get_SupportedTextSelection`.
    private int supportedTextSelection(MemorySegment self, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        value.reinterpret(ValueLayout.JAVA_INT.byteSize()).set(ValueLayout.JAVA_INT, 0L, SUPPORTED_TEXT_SELECTION_SINGLE);
        return S_OK;
    }

    /// Implements unused `ITextRangeProvider` slots.
    private int notImplemented(MemorySegment self) {
        return E_NOTIMPL;
    }

    /// Implements `ITextRangeProvider::Clone`.
    private int cloneRange(MemorySegment self, MemorySegment range) {
        return documentRange(self, range);
    }

    /// Implements `ITextRangeProvider::Compare`.
    private int compareRange(MemorySegment self, MemorySegment other, MemorySegment equal) {
        if (equal.address() == 0L) {
            return E_POINTER;
        }
        int same = other.address() != 0L && other.address() == textRangeObject.address() ? 1 : 0;
        equal.reinterpret(ValueLayout.JAVA_INT.byteSize()).set(ValueLayout.JAVA_INT, 0L, same);
        return S_OK;
    }

    /// Implements `ITextRangeProvider::GetEnclosingElement`.
    private int enclosingElement(MemorySegment self, MemorySegment element) {
        if (element.address() == 0L) {
            return E_POINTER;
        }
        element.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, simpleObject);
        addRef(simpleObject);
        return S_OK;
    }

    /// Implements `ITextRangeProvider::GetText`.
    private int getText(MemorySegment self, int maxLength, MemorySegment text) {
        if (text.address() == 0L) {
            return E_POINTER;
        }
        String document = node.label();
        int units = document.length();
        if (maxLength >= 0 && maxLength < units) {
            units = maxLength;
        }
        MemorySegment block = arena.allocate(4L + ((long) units + 1L) * 2L);
        block.set(ValueLayout.JAVA_INT, 0L, units * 2);
        for (int index = 0; index < units; index++) {
            block.set(ValueLayout.JAVA_CHAR, 4L + (long) index * 2L, document.charAt(index));
        }
        block.set(ValueLayout.JAVA_CHAR, 4L + (long) units * 2L, (char) 0);
        text.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, block.asSlice(4L));
        return S_OK;
    }

    /// Writes one double out-parameter.
    private static int writeDouble(MemorySegment value, double payload) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        value.set(ValueLayout.JAVA_DOUBLE, 0L, payload);
        return S_OK;
    }

    /// Maps the initial toggle state from semantics.
    private static int initialToggleState(SemanticsNode node) {
        if (node.selected() == null) {
            return TOGGLE_STATE_INDETERMINATE;
        }
        return node.selected() ? TOGGLE_STATE_ON : TOGGLE_STATE_OFF;
    }

    /// Maps the semantics role onto a UIA control-type identifier.
    private static int controlTypeId(SemanticsNode node) {
        return switch (node.role()) {
            case BUTTON -> UIA_BUTTON_CONTROL_TYPE_ID;
            case TOGGLE -> 50002;
            case SLIDER -> 50015;
            case TEXT_FIELD, TEXT_AREA -> 50004;
            case LIST -> 50008;
            case TEXT -> 50020;
            case STATUS -> UIA_STATUS_BAR_CONTROL_TYPE_ID;
            case NONE -> 50033;
            case POPUP -> 50033;
            case MENU -> 50009;
            case MENU_ITEM -> 50011;
            case DIALOG -> 50032;
            case TOOLTIP -> 50022;
        };
    }

    /// Maps live-region politeness onto a UIA LiveSetting identifier.
    ///
    /// @param node the semantics node
    /// @return the live-setting identifier
    private static int liveSettingId(SemanticsNode node) {
        return switch (node.liveRegion()) {
            case OFF -> LIVE_SETTING_OFF;
            case POLITE -> LIVE_SETTING_POLITE;
            case ASSERTIVE -> LIVE_SETTING_ASSERTIVE;
        };
    }

    /// Reads one vtable slot from a COM object.
    private static MemorySegment functionAt(MemorySegment object, int slot) {
        MemorySegment vtable = object.get(ValueLayout.ADDRESS, 0L)
                .reinterpret(ValueLayout.ADDRESS.byteSize() * (slot + 1L));
        return vtable.getAtIndex(ValueLayout.ADDRESS, slot);
    }

    /// Rejects a failing HRESULT and contained callback failures.
    private void requireSuccess(String name, int result) {
        if (result < 0) {
            throw new IllegalStateException(name + " failed with HRESULT " + result
                    + " (0x" + Integer.toHexString(result) + ')');
        }
        @Nullable Throwable failure = failures.poll();
        if (failure != null) {
            throw new IllegalStateException(name + " callback failed", failure);
        }
    }

    /// Verifies the provider is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Windows automation provider is closed");
        }
    }
}
