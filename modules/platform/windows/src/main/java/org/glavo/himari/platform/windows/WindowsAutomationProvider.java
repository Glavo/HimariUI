package org.glavo.himari.platform.windows;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import java.util.UUID;

/// Implements `IRawElementProviderSimple::GetPropertyValue` for a semantics node.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsAutomationProvider implements AutoCloseable {
    /// `IUnknown`.
    private static final UUID IUNKNOWN = UUID.fromString("00000000-0000-0000-c000-000000000046");

    /// `IRawElementProviderSimple`.
    private static final UUID IRAW_ELEMENT_PROVIDER_SIMPLE =
            UUID.fromString("d6dd68d1-86fd-4332-8666-9abedea2d24c");

    /// `UIA_ControlTypePropertyId`.
    static final int UIA_CONTROL_TYPE_PROPERTY_ID = 30003;

    /// `UIA_ButtonControlTypeId`.
    static final int UIA_BUTTON_CONTROL_TYPE_ID = 50000;

    /// `VT_I4`.
    private static final int VT_I4 = 3;

    /// `S_OK`.
    private static final int S_OK = 0;

    /// `E_NOINTERFACE`.
    private static final int E_NOINTERFACE = 0x8000_4002;

    /// `E_POINTER`.
    private static final int E_POINTER = 0x8000_4003;

    /// Arena owning the COM object.
    private final Arena arena;

    /// Contained callback failures.
    private final CallbackFailureQueue failures = new CallbackFailureQueue();

    /// Projected semantics node.
    private final SemanticsNode node;

    /// COM object.
    private final MemorySegment object;

    /// COM vtable. Slot 5 is `GetPropertyValue`.
    private final MemorySegment vtable;

    /// Outstanding references.
    private int references = 1;

    /// Whether closed.
    private boolean closed;

    /// Creates one provider.
    private WindowsAutomationProvider(Win32FfmBindings bindings, SemanticsNode node) {
        this.node = node;
        this.arena = Arena.ofConfined();
        this.vtable = arena.allocate(ValueLayout.ADDRESS, 6);
        this.object = arena.allocate(ValueLayout.ADDRESS);
        object.set(ValueLayout.ADDRESS, 0L, vtable);
        vtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryInterface, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        vtable.setAtIndex(
                ValueLayout.ADDRESS,
                5L,
                bindings.createIrawElementProviderGetPropertyValueStub(this::getPropertyValue, failures, arena)
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
        MemorySegment getProperty = vtable.getAtIndex(ValueLayout.ADDRESS, 5L);
        MemorySegment value = arena.allocate(Win32Layouts.VARIANT);
        value.fill((byte) 0);
        int result = Win32FfmBindings.invokeIrawElementProviderGetPropertyValuePointer(
                getProperty,
                object,
                propertyId,
                value
        );
        if (result < 0) {
            throw new IllegalStateException("GetPropertyValue failed with HRESULT " + result
                    + " (0x" + Integer.toHexString(result) + ')');
        }
        return value.get(ValueLayout.JAVA_INT, Win32Layouts.VARIANT_L_VAL_OFFSET);
    }

    /// Releases this owner's COM reference.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        release(object);
        arena.close();
    }

    /// Implements `IUnknown::QueryInterface`.
    private int queryInterface(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        if (result.address() == 0L) {
            return E_POINTER;
        }
        if (WindowsCom.matches(interfaceId, IUNKNOWN)
                || WindowsCom.matches(interfaceId, IRAW_ELEMENT_PROVIDER_SIMPLE)) {
            result.set(ValueLayout.ADDRESS, 0L, object);
            addRef(self);
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

    /// Implements `IRawElementProviderSimple::GetPropertyValue`.
    private int getPropertyValue(MemorySegment self, int propertyId, MemorySegment value) {
        if (value.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment variant = value.reinterpret(Win32Layouts.VARIANT.byteSize());
        variant.fill((byte) 0);
        if (propertyId == UIA_CONTROL_TYPE_PROPERTY_ID) {
            variant.set(ValueLayout.JAVA_SHORT, Win32Layouts.VARIANT_VT_OFFSET, (short) VT_I4);
            variant.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.VARIANT_L_VAL_OFFSET,
                    controlTypeId(node)
            );
        }
        return S_OK;
    }

    /// Maps the semantics role onto a UIA control-type identifier.
    private static int controlTypeId(SemanticsNode node) {
        return switch (node.role()) {
            case BUTTON -> UIA_BUTTON_CONTROL_TYPE_ID;
            case TOGGLE -> 50002;
            case SLIDER -> 50015;
            case TEXT_FIELD -> 50004;
            case LIST -> 50008;
            case TEXT -> 50020;
            case NONE -> 50033;
        };
    }

    /// Verifies the provider is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Windows automation provider is closed");
        }
    }
}
