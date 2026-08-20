package org.glavo.himari.platform.windows;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.KeyLocation;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerDeviceKind;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Owns one HWND created through generated FFM bindings and normalizes host input.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsNativeWindow implements AutoCloseable {
    /// Standard overlapped style.
    private static final int WS_OVERLAPPEDWINDOW = 0x00CF0000;

    /// Popup style for owner-relative windows.
    private static final int WS_POPUP = 0x80000000;

    /// Tool window style.
    private static final int WS_EX_TOOLWINDOW = 0x00000080;

    /// No-activate style for conformance windows.
    private static final int WS_EX_NOACTIVATE = 0x08000000;

    /// Show without activation.
    private static final int SW_SHOWNOACTIVATE = 4;

    /// Hide.
    private static final int SW_HIDE = 0;

    /// Close request.
    private static final int WM_CLOSE = 0x0010;

    /// Cursor hit-test query (`WM_SETCURSOR`).
    private static final int WM_SETCURSOR = 0x0020;

    /// Client-area hit test (`HTCLIENT`).
    private static final int HTCLIENT = 1;

    /// Destroyed.
    private static final int WM_DESTROY = 0x0002;

    /// Size change.
    private static final int WM_SIZE = 0x0005;

    /// Per-monitor DPI change.
    private static final int WM_DPICHANGED = 0x02E0;

    /// Clipboard contents changed (`WM_CLIPBOARDUPDATE`).
    private static final int WM_CLIPBOARDUPDATE = 0x031D;

    /// System-key down (`WM_SYSKEYDOWN`).
    private static final int WM_SYSKEYDOWN = 0x0104;

    /// System-key up (`WM_SYSKEYUP`).
    private static final int WM_SYSKEYUP = 0x0105;

    /// Pointer move.
    private static final int WM_MOUSEMOVE = 0x0200;

    /// Left button down.
    private static final int WM_LBUTTONDOWN = 0x0201;

    /// Left button up.
    private static final int WM_LBUTTONUP = 0x0202;

    /// Right button down.
    private static final int WM_RBUTTONDOWN = 0x0204;

    /// Right button up.
    private static final int WM_RBUTTONUP = 0x0205;

    /// Middle button down.
    private static final int WM_MBUTTONDOWN = 0x0207;

    /// Middle button up.
    private static final int WM_MBUTTONUP = 0x0208;

    /// Extra button down.
    private static final int WM_XBUTTONDOWN = 0x020B;

    /// Extra button up.
    private static final int WM_XBUTTONUP = 0x020C;

    /// `VK_SHIFT`.
    private static final int VK_SHIFT = 0x10;

    /// `VK_CONTROL`.
    private static final int VK_CONTROL = 0x11;

    /// `VK_MENU` (Alt).
    private static final int VK_MENU = 0x12;

    /// `VK_LWIN`.
    private static final int VK_LWIN = 0x5B;

    /// `VK_RWIN`.
    private static final int VK_RWIN = 0x5C;

    /// `MAPVK_VK_TO_VSC`.
    private static final int MAPVK_VK_TO_VSC = 0;

    /// Vertical mouse wheel.
    private static final int WM_MOUSEWHEEL = 0x020A;

    /// Horizontal mouse wheel.
    private static final int WM_MOUSEHWHEEL = 0x020E;

    /// Pointer entered the window (`WM_POINTERENTER`).
    private static final int WM_POINTERENTER = 0x0249;

    /// Pointer left the window (`WM_POINTERLEAVE`).
    private static final int WM_POINTERLEAVE = 0x024A;

    /// Pointer capture changed (`WM_POINTERCAPTURECHANGED`).
    private static final int WM_POINTERCAPTURECHANGED = 0x024C;

    /// Pointer activation of an inactive window (`WM_POINTERACTIVATE`).
    private static final int WM_POINTERACTIVATE = 0x024B;

    /// Non-client pointer update (`WM_NCPOINTERUPDATE`).
    private static final int WM_NCPOINTERUPDATE = 0x0241;

    /// Non-client pointer press (`WM_NCPOINTERDOWN`).
    private static final int WM_NCPOINTERDOWN = 0x0242;

    /// Non-client pointer release (`WM_NCPOINTERUP`).
    private static final int WM_NCPOINTERUP = 0x0243;

    /// `PA_ACTIVATE` — activate the window that received the pointer.
    private static final int PA_ACTIVATE = 1;

    /// Vertical pointer wheel (`WM_POINTERWHEEL`).
    private static final int WM_POINTERWHEEL = 0x024E;

    /// Horizontal pointer wheel (`WM_POINTERHWHEEL`).
    private static final int WM_POINTERHWHEEL = 0x024F;

    /// Pointer routed to this window (`WM_POINTERROUTEDTO`).
    private static final int WM_POINTERROUTEDTO = 0x0251;

    /// Pointer routed away from this window (`WM_POINTERROUTEDAWAY`).
    private static final int WM_POINTERROUTEDAWAY = 0x0252;

    /// Routed pointer released (`WM_POINTERROUTEDRELEASED`).
    private static final int WM_POINTERROUTEDRELEASED = 0x0253;

    /// `IDC_ARROW`.
    public static final int IDC_ARROW = 32512;

    /// `IDC_IBEAM`.
    public static final int IDC_IBEAM = 32513;

    /// `IDC_WAIT`.
    public static final int IDC_WAIT = 32514;

    /// `IDC_CROSS`.
    public static final int IDC_CROSS = 32515;

    /// `IDC_SIZEWE`.
    public static final int IDC_SIZEWE = 32644;

    /// `IDC_SIZENS`.
    public static final int IDC_SIZENS = 32645;

    /// `IDC_SIZEALL`.
    public static final int IDC_SIZEALL = 32646;

    /// `IDC_NO`.
    public static final int IDC_NO = 32648;

    /// `IDC_HAND`.
    public static final int IDC_HAND = 32649;

    /// `IDC_APPSTARTING`.
    public static final int IDC_APPSTARTING = 32650;

    /// `IDC_HELP`.
    public static final int IDC_HELP = 32651;

    /// `SPI_GETWHEELSCROLLLINES`.
    public static final int SPI_GETWHEELSCROLLLINES = 0x0068;

    /// `SPI_GETWHEELSCROLLCHARS`.
    public static final int SPI_GETWHEELSCROLLCHARS = 0x006C;

    /// `SPI_GETHIGHCONTRAST`.
    public static final int SPI_GETHIGHCONTRAST = 0x0042;

    /// `SPI_GETCLIENTAREAANIMATION`.
    public static final int SPI_GETCLIENTAREAANIMATION = 0x1042;

    /// `HCF_HIGHCONTRASTON`.
    public static final int HCF_HIGHCONTRASTON = 0x0000_0001;

    /// `CURSOR_SHOWING`.
    public static final int CURSOR_SHOWING = 0x00000001;

    /// `WHEEL_DELTA`.
    public static final int WHEEL_DELTA = 120;

    /// `PT_TOUCH`.
    public static final int PT_TOUCH = 2;

    /// `PT_PEN`.
    public static final int PT_PEN = 3;

    /// `PT_MOUSE`.
    public static final int PT_MOUSE = 4;

    /// Pointer update.
    private static final int WM_POINTERUPDATE = 0x0245;

    /// Pointer down.
    private static final int WM_POINTERDOWN = 0x0246;

    /// Pointer up.
    private static final int WM_POINTERUP = 0x0247;

    /// Key down.
    private static final int WM_KEYDOWN = 0x0100;

    /// Key up.
    private static final int WM_KEYUP = 0x0101;

    /// Translated character.
    private static final int WM_CHAR = 0x0102;

    /// Dead-key character (`WM_DEADCHAR`).
    private static final int WM_DEADCHAR = 0x0103;

    /// System character (`WM_SYSCHAR`).
    private static final int WM_SYSCHAR = 0x0106;

    /// System dead-key character (`WM_SYSDEADCHAR`).
    private static final int WM_SYSDEADCHAR = 0x0107;

    /// Unicode character (`WM_UNICHAR`).
    private static final int WM_UNICHAR = 0x0109;

    /// `UNICODE_NOCHAR` probe sent with `WM_UNICHAR`.
    private static final int UNICODE_NOCHAR = 0xFFFF;

    /// `CF_TEXT`.
    private static final int CF_TEXT = 1;

    /// `CF_UNICODETEXT`.
    private static final int CF_UNICODETEXT = 13;

    /// IME composition start (`WM_IME_STARTCOMPOSITION`).
    private static final int WM_IME_STARTCOMPOSITION = 0x010D;

    /// IME composition end (`WM_IME_ENDCOMPOSITION`).
    private static final int WM_IME_ENDCOMPOSITION = 0x010E;

    /// IME composition update (`WM_IME_COMPOSITION`).
    private static final int WM_IME_COMPOSITION = 0x010F;

    /// IME character (`WM_IME_CHAR`).
    private static final int WM_IME_CHAR = 0x0286;

    /// IME notify (`WM_IME_NOTIFY`).
    private static final int WM_IME_NOTIFY = 0x0282;

    /// IME request (`WM_IME_REQUEST`).
    private static final int WM_IME_REQUEST = 0x0288;

    /// IME set-context (`WM_IME_SETCONTEXT`).
    private static final int WM_IME_SETCONTEXT = 0x0281;

    /// IME select (`WM_IME_SELECT`).
    private static final int WM_IME_SELECT = 0x0285;

    /// `IMR_COMPOSITIONWINDOW`.
    static final int IMR_COMPOSITIONWINDOW = 0x0001;

    /// `IMR_CANDIDATEWINDOW`.
    static final int IMR_CANDIDATEWINDOW = 0x0002;

    /// `IMR_COMPOSITIONFONT`.
    static final int IMR_COMPOSITIONFONT = 0x0003;

    /// `IMR_RECONVERTSTRING`.
    static final int IMR_RECONVERTSTRING = 0x0004;

    /// `IMR_CONFIRMRECONVERTSTRING`.
    static final int IMR_CONFIRMRECONVERTSTRING = 0x0005;

    /// `IMR_QUERYCHARPOSITION`.
    static final int IMR_QUERYCHARPOSITION = 0x0006;

    /// `IMR_DOCUMENTFEED`.
    static final int IMR_DOCUMENTFEED = 0x0007;

    /// `IACE_DEFAULT`.
    private static final int IACE_DEFAULT = 0x0010;

    /// `IGIMII_CMODE | IGIMII_CONFIGURE | IGIMII_TOOLS | IGIMII_HELP | IGIMII_OTHER | IGIMII_INPUTTOOLS`.
    private static final int IGIMII_MENU_TYPES = 0x0001 | 0x0004 | 0x0008 | 0x0010 | 0x0020 | 0x0040;

    /// `IME_ESC_QUERY_SUPPORT`.
    private static final int IME_ESC_QUERY_SUPPORT = 0x0003;

    /// `IME_ESC_IME_NAME`.
    private static final int IME_ESC_IME_NAME = 0x1006;

    /// `IGP_PROPERTY`.
    private static final int IGP_PROPERTY = 0x00000004;

    /// `IME_CHOTKEY_IME_NONIME_TOGGLE`.
    private static final int IME_CHOTKEY_IME_NONIME_TOGGLE = 0x10;

    /// `IME_ITHOTKEY_RESEND_RESULTSTR`.
    private static final int IME_ITHOTKEY_RESEND_RESULTSTR = 0x0200;

    /// `GCL_CONVERSION`.
    private static final int GCL_CONVERSION = 0x0001;

    /// `GCL_REVERSECONVERSION`.
    private static final int GCL_REVERSECONVERSION = 0x0002;

    /// `GCL_REVERSE_LENGTH`.
    private static final int GCL_REVERSE_LENGTH = 0x0003;

    /// `IME_ITHOTKEY_UISTYLE_TOGGLE`.
    private static final int IME_ITHOTKEY_UISTYLE_TOGGLE = 0x0202;

    /// IME control (`WM_IME_CONTROL`).
    private static final int WM_IME_CONTROL = 0x0283;

    /// `IMC_GETSTATUSWINDOWPOS`.
    static final int IMC_GETSTATUSWINDOWPOS = 0x000F;

    /// `IMN_OPENCANDIDATE`.
    static final int IMN_OPENCANDIDATE = 0x0002;

    /// `IMN_CHANGECANDIDATE`.
    static final int IMN_CHANGECANDIDATE = 0x0003;

    /// `IMN_CLOSECANDIDATE`.
    static final int IMN_CLOSECANDIDATE = 0x0004;

    /// `IMN_GUIDELINE`.
    static final int IMN_GUIDELINE = 0x000D;

    /// `GGL_STRING`.
    private static final int GGL_STRING = 0x0003;

    /// Maximum `GetPointerInfoHistory` entries requested per contact.
    private static final int POINTER_HISTORY_CAPACITY = 16;

    /// Mouse leave after `TrackMouseEvent` (`WM_MOUSELEAVE`).
    private static final int WM_MOUSELEAVE = 0x02A3;

    /// `GCS_COMPREADSTR`.
    static final int GCS_COMPREADSTR = 0x0001;

    /// `GCS_COMPREADATTR`.
    static final int GCS_COMPREADATTR = 0x0002;

    /// `GCS_COMPREADCLAUSE`.
    static final int GCS_COMPREADCLAUSE = 0x0004;

    /// `GCS_COMPSTR`.
    static final int GCS_COMPSTR = 0x0008;

    /// `GCS_COMPATTR`.
    static final int GCS_COMPATTR = 0x0010;

    /// `GCS_COMPCLAUSE`.
    static final int GCS_COMPCLAUSE = 0x0020;

    /// `GCS_CURSORPOS`.
    static final int GCS_CURSORPOS = 0x0080;

    /// `GCS_DELTASTART`.
    static final int GCS_DELTASTART = 0x0100;

    /// `GCS_RESULTREADSTR`.
    static final int GCS_RESULTREADSTR = 0x0200;

    /// `GCS_RESULTREADCLAUSE`.
    static final int GCS_RESULTREADCLAUSE = 0x0400;

    /// `GCS_RESULTSTR`.
    static final int GCS_RESULTSTR = 0x0800;

    /// `GCS_RESULTCLAUSE`.
    static final int GCS_RESULTCLAUSE = 0x1000;

    /// `NI_COMPOSITIONSTR`.
    private static final int NI_COMPOSITIONSTR = 0x0015;

    /// `CPS_COMPLETE`.
    private static final int CPS_COMPLETE = 0x0001;

    /// `CPS_CANCEL`.
    private static final int CPS_CANCEL = 0x0004;

    /// `TME_LEAVE`.
    private static final int TME_LEAVE = 0x00000002;

    /// `ToUnicodeW` `wFlags` bit 2: do not change keyboard state.
    private static final int TO_UNICODE_NO_STATE_CHANGE = 0x4;

    /// Enter move/resize modal loop.
    private static final int WM_ENTERSIZEMOVE = 0x0231;

    /// Exit move/resize modal loop.
    private static final int WM_EXITSIZEMOVE = 0x0232;

    /// Paint request.
    private static final int WM_PAINT = 0x000F;

    /// Background erase request.
    private static final int WM_ERASEBKGND = 0x0014;

    /// Timer.
    private static final int WM_TIMER = 0x0113;

    /// Raw input (`WM_INPUT`).
    private static final int WM_INPUT = 0x00FF;

    /// `HID_USAGE_PAGE_GENERIC`.
    private static final int HID_USAGE_PAGE_GENERIC = 0x01;

    /// `HID_USAGE_GENERIC_KEYBOARD`.
    private static final int HID_USAGE_GENERIC_KEYBOARD = 0x06;

    /// `HID_USAGE_GENERIC_MOUSE`.
    private static final int HID_USAGE_GENERIC_MOUSE = 0x02;

    /// `RID_HEADER`.
    private static final int RID_HEADER = 0x10000005;

    /// `sizeof(RAWINPUTHEADER)` on this x64 schema.
    private static final int RAWINPUTHEADER_SIZE = 24;

    /// `RIDI_DEVICENAME`.
    private static final int RIDI_DEVICENAME = 0x20000007;

    /// `sizeof(RAWINPUTDEVICELIST)` on this x64 schema.
    private static final int RAWINPUTDEVICELIST_SIZE = 16;

    /// Raw device arrival/removal (`WM_INPUT_DEVICE_CHANGE`).
    private static final int WM_INPUT_DEVICE_CHANGE = 0x00FE;

    /// `WM_POWERBROADCAST`.
    static final int WM_POWERBROADCAST = 0x0218;

    /// `PBT_APMSUSPEND`.
    static final int PBT_APMSUSPEND = 0x0004;

    /// `PBT_APMRESUMESUSPEND`.
    static final int PBT_APMRESUMESUSPEND = 0x0007;

    /// Timer id used for modal-loop reentry.
    static final long MODAL_TIMER_ID = 0x484D5249L;

    /// Set-position flags: no z-order or activation.
    private static final int SWP_NOZORDER = 0x0004;

    /// Set-position flags: no activation.
    private static final int SWP_NOACTIVATE = 0x0010;

    /// Default USER32 DPI.
    private static final int USER_DEFAULT_SCREEN_DPI = 96;

    /// Generated bindings.
    private final Win32FfmBindings bindings;

    /// Confined arena for this window.
    private final Arena arena;

    /// Contained callback failures.
    private final CallbackFailureQueue callbackFailures;

    /// Module handle.
    private final MemorySegment instance;

    /// Class name.
    private final MemorySegment className;

    /// Reusable RECT for geometry queries.
    private final MemorySegment rectRecord;

    /// Reusable `PAINTSTRUCT` for `WM_PAINT`.
    private final MemorySegment paintRecord;

    /// Reusable 256-byte `GetKeyboardState` / `ToUnicodeW` key-state buffer.
    private final MemorySegment keyboardStateBuffer;

    /// Reusable `ToUnicodeW` output buffer.
    private final MemorySegment unicodeBuffer;

    /// Reusable `GetKeyNameTextW` output buffer.
    private final MemorySegment keyNameBuffer;

    /// Reusable `TRACKMOUSEEVENT` for `TME_LEAVE`.
    private final MemorySegment trackMouseEventRecord;

    /// Close and destroy listener.
    private final Lifecycle lifecycle;

    /// Pointer events received through WndProc since the last drain.
    private final ArrayList<PointerEvent> pointerEvents = new ArrayList<>();

    /// Synthetic pen axes used when `GetPointerPenInfo` has no live contact.
    private @Nullable PenAxes syntheticPenAxes;

    /// Pointer identity that [`#syntheticPenAxes`] applies to.
    private int syntheticPenPointerId = -1;

    /// Synthetic contact ellipse used when `GetPointerTouchInfo` has no live contact.
    private @Nullable ContactArea syntheticContact;

    /// Pointer identity that [`#syntheticContact`] applies to.
    private int syntheticContactPointerId = -1;

    /// Synthetic `POINTER_INFO` flags used when `GetPointerInfo` has no live contact.
    private @Nullable PointerFlags syntheticPointerFlags;

    /// Pointer identity that [`#syntheticPointerFlags`] applies to.
    private int syntheticPointerFlagsId = -1;

    /// Host delivery sequence assigned to the next pointer event.
    private int pointerSequence;

    /// Key events received through WndProc since the last drain.
    private final ArrayList<KeyEvent> keyEvents = new ArrayList<>();

    /// UTF-16 code units received as `WM_CHAR` or `WM_UNICHAR` since the last drain.
    private final StringBuilder characters = new StringBuilder();

    /// UTF-16 code units received as `WM_DEADCHAR` since the last drain.
    private final StringBuilder deadCharacters = new StringBuilder();

    /// Printable `ToUnicodeW` text produced on `WM_KEYDOWN` since the last drain.
    private final StringBuilder translatedCharacters = new StringBuilder();

    /// IMM32 composition preview from `GCS_COMPSTR` since the last drain.
    private final StringBuilder imeComposition = new StringBuilder();

    /// IMM32 reading-window text from `GCS_COMPREADSTR` since the last drain.
    private final StringBuilder imeReading = new StringBuilder();

    /// IMM32 result reading-window text from `GCS_RESULTREADSTR` since the last drain.
    private final StringBuilder imeResultReading = new StringBuilder();

    /// IMM32 committed result from `GCS_RESULTSTR` since the last drain.
    private final StringBuilder imeResult = new StringBuilder();

    /// Last `GetKeyNameTextW` string for a delivered key-down.
    private String lastKeyName = "";

    /// Last `VkKeyScanW` result for a delivered `WM_CHAR`.
    private short lastCharVirtualKeyScan;

    /// Last `GetKeyboardLayout` handle address observed on key-down.
    private long lastKeyboardLayout;

    /// Last `ImmGetCompositionStringW` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastCompositionStringBytes = Integer.MIN_VALUE;

    /// Last `ImmGetCompositionStringW` `GCS_CURSORPOS` result, or `Integer.MIN_VALUE` before a query.
    private int lastCompositionCursor = Integer.MIN_VALUE;

    /// Last `ImmGetCompositionStringW` `GCS_COMPATTR` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastCompositionAttrBytes = Integer.MIN_VALUE;

    /// Last `GCS_COMPATTR` bytes, empty when the host reports none.
    private byte[] lastCompositionAttributes = new byte[0];

    /// Last `ImmGetCompositionStringW` `GCS_COMPREADSTR` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastCompositionReadingBytes = Integer.MIN_VALUE;

    /// Last `ImmGetCompositionStringW` `GCS_COMPCLAUSE` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastCompositionClauseBytes = Integer.MIN_VALUE;

    /// Last `GCS_COMPCLAUSE` offsets, empty when the host reports none.
    private int[] lastCompositionClause = new int[0];

    /// Last `ImmGetCompositionStringW` `GCS_RESULTREADSTR` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastResultReadingBytes = Integer.MIN_VALUE;

    /// Last `ImmGetCompositionStringW` `GCS_RESULTCLAUSE` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastResultClauseBytes = Integer.MIN_VALUE;

    /// Last `GCS_RESULTCLAUSE` offsets, empty when the host reports none.
    private int[] lastResultClause = new int[0];

    /// Last `ImmGetCompositionStringW` `GCS_DELTASTART` result, or `Integer.MIN_VALUE` before a query.
    private int lastCompositionDeltaStart = Integer.MIN_VALUE;

    /// Last `ImmGetCompositionStringW` `GCS_COMPREADATTR` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastCompositionReadingAttrBytes = Integer.MIN_VALUE;

    /// Last `GCS_COMPREADATTR` bytes, empty when the host reports none.
    private byte[] lastCompositionReadingAttributes = new byte[0];

    /// Last `ImmGetCompositionStringW` `GCS_COMPREADCLAUSE` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastCompositionReadingClauseBytes = Integer.MIN_VALUE;

    /// Last `GCS_COMPREADCLAUSE` offsets, empty when the host reports none.
    private int[] lastCompositionReadingClause = new int[0];

    /// Last `ImmGetCompositionStringW` `GCS_RESULTREADCLAUSE` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastResultReadingClauseBytes = Integer.MIN_VALUE;

    /// Last `GCS_RESULTREADCLAUSE` offsets, empty when the host reports none.
    private int[] lastResultReadingClause = new int[0];

    /// Last `GetPointerFramePenInfoHistory` entry count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerFramePenHistoryEntries = Integer.MIN_VALUE;

    /// Last `GetPointerFrameTouchInfoHistory` entry count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerFrameTouchHistoryEntries = Integer.MIN_VALUE;

    /// Last `ImmGetVirtualKey` result, or `Integer.MIN_VALUE` before a query.
    private int lastImeVirtualKey = Integer.MIN_VALUE;

    /// Last `WM_IME_REQUEST` `wParam`, or `Integer.MIN_VALUE` before a request.
    private int lastImeRequest = Integer.MIN_VALUE;

    /// Last `WM_IME_REQUEST` byte count returned to the host, or `Integer.MIN_VALUE` before a request.
    private int lastImeRequestBytes = Integer.MIN_VALUE;

    /// Last `IMECHARPOSITION.dwCharPos`, or `Integer.MIN_VALUE` before a query.
    private int lastImeCharPos = Integer.MIN_VALUE;

    /// Previous `HIMC` from `ImmAssociateContext`, or `-1` before a call.
    private long lastAssociateContext = -1L;

    /// Last `ImmAssociateContextEx` result, or `Integer.MIN_VALUE` before a call.
    private int lastAssociateContextExResult = Integer.MIN_VALUE;

    /// Last `ImmIsIME` result, or `Integer.MIN_VALUE` before a query.
    private int lastImeIsIme = Integer.MIN_VALUE;

    /// Last `ImmGetImeMenuItemsW` item count, or `Integer.MIN_VALUE` before a query.
    private int lastImeMenuItemCount = Integer.MIN_VALUE;

    /// Last `ImmEscapeW` result, or `Long.MIN_VALUE` before a query.
    private long lastImeEscapeResult = Long.MIN_VALUE;

    /// Last `ImmGetDescriptionW` character count, or `Integer.MIN_VALUE` before a query.
    private int lastImeDescriptionChars = Integer.MIN_VALUE;

    /// Last `ImmGetDescriptionW` string, empty when the host reports none.
    private String lastImeDescription = "";

    /// Last `ImmGetProperty` bits, or `Integer.MIN_VALUE` before a query.
    private int lastImeProperty = Integer.MIN_VALUE;

    /// Last `ImmGetDefaultIMEWnd` handle address, or `-1` before a query.
    private long lastDefaultImeWnd = -1L;

    /// Last `ImmGetStatusWindowPos` result, or `Integer.MIN_VALUE` before a query.
    private int lastStatusWindowPosResult = Integer.MIN_VALUE;

    /// Last `ImmSetStatusWindowPos` result, or `Integer.MIN_VALUE` before a write.
    private int lastSetStatusWindowPosResult = Integer.MIN_VALUE;

    /// Last `ImmGetHotKey` result, or `Integer.MIN_VALUE` before a query.
    private int lastImeHotKeyResult = Integer.MIN_VALUE;

    /// Last `ImmSetHotKey` result, or `Integer.MIN_VALUE` before a write.
    private int lastSetHotKeyResult = Integer.MIN_VALUE;

    /// Last `ImmGetConversionListW` `GCL_REVERSECONVERSION` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastConversionReverseBytes = Integer.MIN_VALUE;

    /// Last `ImmGetConversionListW` `GCL_REVERSECONVERSION` candidate count.
    private int lastConversionReverseCount;

    /// Last `ImmIsUIMessageW` result, or `Integer.MIN_VALUE` before a query.
    private int lastImeIsUiMessage = Integer.MIN_VALUE;

    /// Last `ImmGetRegisterWordStyleW` style count, or `Integer.MIN_VALUE` before a query.
    private int lastRegisterWordStyleCount = Integer.MIN_VALUE;

    /// Last `ImmEnumInputContext` BOOL result, or `Integer.MIN_VALUE` before a query.
    private int lastEnumInputContextResult = Integer.MIN_VALUE;

    /// Number of `IMCENUMPROC` invocations during the last `ImmEnumInputContext`.
    private int lastEnumInputContextCount;

    /// Last `ImmEnumRegisterWordW` count, or `Integer.MIN_VALUE` before a query.
    private int lastEnumRegisterWordCount = Integer.MIN_VALUE;

    /// Number of `REGISTERWORDENUMPROCW` invocations during the last `ImmEnumRegisterWordW`.
    private int lastEnumRegisterWordHits;

    /// Last `ImmRequestMessageW` result, or `Long.MIN_VALUE` before a query.
    private long lastImmRequestMessageResult = Long.MIN_VALUE;

    /// Last `ImmRegisterWordW` BOOL result, or `Integer.MIN_VALUE` before a query.
    private int lastRegisterWordResult = Integer.MIN_VALUE;

    /// Last `ImmUnregisterWordW` BOOL result, or `Integer.MIN_VALUE` before a query.
    private int lastUnregisterWordResult = Integer.MIN_VALUE;

    /// Last `CountClipboardFormats` result, or `Integer.MIN_VALUE` before a query.
    private int lastClipboardFormatCount = Integer.MIN_VALUE;

    /// Last `GetClipboardSequenceNumber` result, or `Integer.MIN_VALUE` before a query.
    private int lastClipboardSequence = Integer.MIN_VALUE;

    /// Last `GetClipboardOwner` handle address, or `-1` before a query.
    private long lastClipboardOwner = -1L;

    /// Last `GetOpenClipboardWindow` handle address, or `-1` before a query.
    private long lastOpenClipboardWindow = -1L;

    /// Last `IsClipboardFormatAvailable(CF_UNICODETEXT)` result, or `Integer.MIN_VALUE` before a query.
    private int lastClipboardUnicodeAvailable = Integer.MIN_VALUE;

    /// Last `GetPriorityClipboardFormat` result, or `Integer.MIN_VALUE` before a query.
    private int lastPriorityClipboardFormat = Integer.MIN_VALUE;

    /// First format from `EnumClipboardFormats(0)`, or `Integer.MIN_VALUE` before a query.
    private int lastEnumClipboardFormat = Integer.MIN_VALUE;

    /// Number of formats walked by `EnumClipboardFormats`, or `Integer.MIN_VALUE` before a query.
    private int lastEnumClipboardFormatCount = Integer.MIN_VALUE;

    /// Last `GetClipboardFormatNameW` character count, or `Integer.MIN_VALUE` before a query.
    private int lastClipboardFormatNameChars = Integer.MIN_VALUE;

    /// Last `GetUpdatedClipboardFormats` BOOL result, or `Integer.MIN_VALUE` before a query.
    private int lastUpdatedClipboardFormatsResult = Integer.MIN_VALUE;

    /// Last `GetUpdatedClipboardFormats` reported format count, or `Integer.MIN_VALUE` before a query.
    private int lastUpdatedClipboardFormatCount = Integer.MIN_VALUE;

    /// Last `AddClipboardFormatListener` BOOL result, or `Integer.MIN_VALUE` before a query.
    private int lastAddClipboardFormatListenerResult = Integer.MIN_VALUE;

    /// Last `RemoveClipboardFormatListener` BOOL result, or `Integer.MIN_VALUE` before a query.
    private int lastRemoveClipboardFormatListenerResult = Integer.MIN_VALUE;

    /// Number of `WM_CLIPBOARDUPDATE` deliveries.
    private int lastClipboardUpdateCount;

    /// Last `ImmGetConversionListW` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastConversionListBytes = Integer.MIN_VALUE;

    /// Last `ImmGetConversionListW` `GCL_REVERSE_LENGTH` result, or `Integer.MIN_VALUE` before a query.
    private int lastConversionReverseLength = Integer.MIN_VALUE;

    /// Last `ImmGetConversionListW` candidate count.
    private int lastConversionListCount;

    /// Last `ImmSimulateHotKey` result, or `Integer.MIN_VALUE` before a call.
    private int lastSimulateHotKeyResult = Integer.MIN_VALUE;

    /// Last `WM_IME_CONTROL` `wParam`, or `Integer.MIN_VALUE` before a delivery.
    private int lastImeControl = Integer.MIN_VALUE;

    /// `HIMC` created for this HWND, or `-1` before `ImmCreateContext`.
    private long lastCreateContext = -1L;

    /// Last `ImmDestroyContext` result, or `Integer.MIN_VALUE` before a call.
    private int lastDestroyContextResult = Integer.MIN_VALUE;

    /// Last `WM_IME_SETCONTEXT` `wParam`, or `Integer.MIN_VALUE` before a delivery.
    private int lastImeSetContext = Integer.MIN_VALUE;

    /// Last `WM_IME_SELECT` `wParam`, or `Integer.MIN_VALUE` before a delivery.
    private int lastImeSelect = Integer.MIN_VALUE;

    /// Owned IMM32 context created for this HWND, or `NULL`.
    private MemorySegment createdImeContext = MemorySegment.NULL;

    /// Surrounding document text used to fill `IMR_DOCUMENTFEED`.
    private String imeDocument = "";

    /// Last committed fragment used to fill `IMR_RECONVERTSTRING`.
    private String imeReconvert = "";

    /// Candidate-window x used to fill `IMR_QUERYCHARPOSITION`.
    private int imeCandidateX;

    /// Candidate-window y used to fill `IMR_QUERYCHARPOSITION`.
    private int imeCandidateY;

    /// Candidate-window width used to fill `IMR_QUERYCHARPOSITION`.
    private int imeCandidateWidth;

    /// Candidate-window height used to fill `IMR_QUERYCHARPOSITION`.
    private int imeCandidateHeight;

    /// Last `ImmGetCompositionWindow` result, or `Integer.MIN_VALUE` before a query.
    private int lastCompositionWindowResult = Integer.MIN_VALUE;

    /// Last `ImmGetCandidateWindow` result, or `Integer.MIN_VALUE` before a query.
    private int lastCandidateWindowResult = Integer.MIN_VALUE;

    /// Last `ImmNotifyIME` result, or `Integer.MIN_VALUE` before a notify.
    private int lastImmNotifyResult = Integer.MIN_VALUE;

    /// Last `ImmGetCandidateListW` candidate count, or `Integer.MIN_VALUE` before a query.
    private int lastCandidateCount = Integer.MIN_VALUE;

    /// Last `ImmGetCandidateListW` selection index.
    private int lastCandidateSelection;

    /// Last `ImmGetCandidateListW` page strings.
    private List<String> lastCandidatePage = List.of();

    /// Whether `WM_IME_STARTCOMPOSITION` is unmatched by `WM_IME_ENDCOMPOSITION`.
    private boolean imeActive;

    /// Whether `WM_IME_ENDCOMPOSITION` arrived since the last drain.
    private boolean imeEnded;

    /// Whether `TrackMouseEvent(TME_LEAVE)` is outstanding.
    private boolean mouseLeaveTracked;

    /// Whether the last generated `TrackMouseEvent` call succeeded.
    private boolean lastTrackMouseEventSucceeded;

    /// Last client x delivered by `WM_MOUSEMOVE`.
    private int lastMouseX;

    /// Last client y delivered by `WM_MOUSEMOVE`.
    private int lastMouseY;

    /// Last `GetPointerFrameInfo` pointer count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerFrameCount = Integer.MIN_VALUE;

    /// Last `GetPointerFrameInfoHistory` entry count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerFrameHistoryEntries = Integer.MIN_VALUE;

    /// Last `GetPointerFrameInfoHistory` pointer count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerFrameHistoryPointers = Integer.MIN_VALUE;

    /// Last `POINTER_INFO.sourceDevice` handle address, or `-1` before a query.
    private long lastPointerSourceDevice = -1L;

    /// Last `POINTER_INFO.hwndTarget` handle address, or `-1` before a query.
    private long lastPointerHwndTarget = -1L;

    /// Last `GetPointerFramePenInfo` pointer count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerFramePenCount = Integer.MIN_VALUE;

    /// Last `GetPointerFrameTouchInfo` pointer count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerFrameTouchCount = Integer.MIN_VALUE;

    /// Last `ImmGetCandidateListCountW` list count, or `Integer.MIN_VALUE` before a query.
    private int lastCandidateListCount = Integer.MIN_VALUE;

    /// Whether generated `RegisterRawInputDevices` succeeded for this HWND.
    private boolean rawInputRegistered;

    /// Last `GetRawInputData` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastRawInputBytes = Integer.MIN_VALUE;

    /// Last `GetRawInputBuffer` reported byte count, or `Integer.MIN_VALUE` before a query.
    private int lastRawInputBufferBytes = Integer.MIN_VALUE;

    /// Last `GetRawInputBuffer` packet count, or `Integer.MIN_VALUE` before a query.
    private int lastRawInputBufferPackets = Integer.MIN_VALUE;

    /// Last `GetRegisteredRawInputDevices` device count, or `Integer.MIN_VALUE` before a query.
    private int lastRegisteredRawInputDevices = Integer.MIN_VALUE;

    /// Last `GetPointerInfoHistory` entry count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerHistoryCount = Integer.MIN_VALUE;

    /// Last `GetPointerPenInfoHistory` entry count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerPenHistoryCount = Integer.MIN_VALUE;

    /// Last `GetPointerTouchInfoHistory` entry count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerTouchHistoryCount = Integer.MIN_VALUE;

    /// Last `ImmGetGuideLineW` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastGuideLineBytes = Integer.MIN_VALUE;

    /// Last `ImmGetCompositionFontW` result, or `Integer.MIN_VALUE` before a query.
    private int lastCompositionFontResult = Integer.MIN_VALUE;

    /// Last `ImmGetCompositionFontW` face name, empty when the host reports none.
    private String lastCompositionFontFace = "";

    /// Last `GetPointerCursorId` value, or `Integer.MIN_VALUE` before a query.
    private int lastPointerCursorId = Integer.MIN_VALUE;

    /// Last `GetPointerDevice` handle address, or `-1` before a query.
    private long lastPointerDevice = -1L;

    /// Last `GetPointerDeviceRects` result, or `Integer.MIN_VALUE` before a query.
    private int lastPointerDeviceRectsResult = Integer.MIN_VALUE;

    /// Last `GetPointerDeviceProperties` property count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerDevicePropertyCount = Integer.MIN_VALUE;

    /// Last `GetPointerDevices` device count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerDeviceCount = Integer.MIN_VALUE;

    /// Last `GetPointerDeviceCursors` cursor count, or `Integer.MIN_VALUE` before a query.
    private int lastPointerDeviceCursorCount = Integer.MIN_VALUE;

    /// Last `ImmSetCompositionFontW` result, or `Integer.MIN_VALUE` before a write.
    private int lastSetCompositionFontResult = Integer.MIN_VALUE;

    /// Last `IsMouseInPointerEnabled` result, or `Integer.MIN_VALUE` before a query.
    private int lastMouseInPointerEnabled = Integer.MIN_VALUE;

    /// Last `SkipPointerFrameMessages` result, or `Integer.MIN_VALUE` before a query.
    private int lastSkipPointerFrameResult = Integer.MIN_VALUE;

    /// Last `ImmSetConversionStatus` result, or `Integer.MIN_VALUE` before a write.
    private int lastSetConversionStatusResult = Integer.MIN_VALUE;

    /// Last `ImmSetOpenStatus` result, or `Integer.MIN_VALUE` before a write.
    private int lastSetOpenStatusResult = Integer.MIN_VALUE;

    /// Last `GetRawInputDeviceInfoW` byte count, or `Integer.MIN_VALUE` before a query.
    private int lastRawInputDeviceInfoBytes = Integer.MIN_VALUE;

    /// Last `GetRawInputDeviceList` device count, or `Integer.MIN_VALUE` before a query.
    private int lastRawInputDeviceListCount = Integer.MIN_VALUE;

    /// Last `WM_INPUT_DEVICE_CHANGE` `wParam`, or `Integer.MIN_VALUE` before a delivery.
    private int lastInputDeviceChange = Integer.MIN_VALUE;

    /// Last `ImmGetConversionStatus` conversion bits, or `Integer.MIN_VALUE` before a query.
    private int lastConversionStatus = Integer.MIN_VALUE;

    /// Last `ImmGetConversionStatus` sentence bits.
    private int lastSentenceStatus;

    /// Last `ImmGetOpenStatus` result, or `Integer.MIN_VALUE` before a query.
    private int lastImeOpenStatus = Integer.MIN_VALUE;

    /// Last `ImmGetIMEFileNameW` path, empty when the host reports none.
    private String lastImeFileName = "";

    /// IMM32 guideline text since the last drain.
    private final StringBuilder guideline = new StringBuilder();

    /// Whether `VK_SHIFT` is latched from `WM_KEYDOWN`/`WM_KEYUP`.
    private boolean shiftDown;

    /// Whether `VK_CONTROL` is latched from `WM_KEYDOWN`/`WM_KEYUP`.
    private boolean ctrlDown;

    /// Whether `VK_MENU` is latched from `WM_KEYDOWN`/`WM_KEYUP`.
    private boolean altDown;

    /// Whether `VK_LWIN` or `VK_RWIN` is latched from `WM_KEYDOWN`/`WM_KEYUP`.
    private boolean metaDown;

    /// Whether the class is registered.
    private boolean classRegistered;

    /// Whether WM_DESTROY ran.
    private boolean destroyObserved;

    /// Native handle.
    private MemorySegment window = MemorySegment.NULL;

    /// Latest client width in physical pixels.
    private int clientWidth;

    /// Latest client height in physical pixels.
    private int clientHeight;

    /// Latest window DPI, or `96` when the host reports zero.
    private int dpi = USER_DEFAULT_SCREEN_DPI;

    /// Whether a move/resize modal loop is active.
    private boolean modalLoopActive;

    /// Number of `PBT_APMSUSPEND` deliveries observed through WndProc.
    private int sleepEvents;

    /// Number of resume power broadcasts observed through WndProc.
    private int wakeEvents;

    /// Number of modal-loop timer ticks delivered through WndProc.
    private int modalTimerTicks;

    /// Last presented unassociated 8-bit sRGB RGBA pixels, or `null` before the first present.
    private @Nullable MemorySegment lastRgba;

    /// Last system cursor installed by [`#setSystemCursor(int)`], or a null segment before one.
    private MemorySegment lastSystemCursor = MemorySegment.NULL;

    /// Width of [#lastRgba].
    private int lastWidth;

    /// Height of [#lastRgba].
    private int lastHeight;

    /// Whether closed.
    private boolean closed;

    /// Receives host close and destroy notifications for one HWND.
    @NotNullByDefault
    interface Lifecycle {
        /// The host asked the application to close the window.
        void closeRequested();

        /// The HWND has been destroyed.
        void destroyed();

        /// A modal-loop timer fired and scheduled UI work may run.
        void modalTick();
    }

    /// Creates the native owner.
    ///
    /// @param libraries the shared libraries
    /// @param arena the window arena
    /// @param lifecycle the close and destroy listener
    private WindowsNativeWindow(WindowsLibraries libraries, Arena arena, Lifecycle lifecycle) {
        this.bindings = libraries.bindings();
        this.arena = arena;
        this.lifecycle = lifecycle;
        this.callbackFailures = new CallbackFailureQueue();
        this.instance = bindings.getModuleHandleW(MemorySegment.NULL).value();
        if (instance.address() == 0L) {
            throw new IllegalStateException("GetModuleHandleW returned NULL");
        }
        this.className = arena.allocateFrom(
                "HimariUIWindow" + Long.toUnsignedString(System.nanoTime()),
                StandardCharsets.UTF_16LE
        );
        this.rectRecord = arena.allocate(Win32Layouts.RECT);
        this.paintRecord = arena.allocate(Win32Layouts.PAINTSTRUCT);
        this.keyboardStateBuffer = arena.allocate(256);
        this.unicodeBuffer = arena.allocate(16);
        this.keyNameBuffer = arena.allocate(128);
        this.trackMouseEventRecord = arena.allocate(Win32Layouts.TRACKMOUSEEVENT);
    }

    /// Creates a hidden native window.
    ///
    /// @param libraries the shared libraries
    /// @param title the title
    /// @param x the origin x
    /// @param y the origin y
    /// @param width the width
    /// @param height the height
    /// @param popup whether the window is a popup
    /// @param owner the owner HWND, or NULL
    /// @param lifecycle the close and destroy listener
    /// @return the window
    static WindowsNativeWindow create(
            WindowsLibraries libraries,
            String title,
            int x,
            int y,
            int width,
            int height,
            boolean popup,
            MemorySegment owner,
            Lifecycle lifecycle
    ) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Window dimensions must be positive");
        }
        Arena arena = Arena.ofConfined();
        WindowsNativeWindow nativeWindow = new WindowsNativeWindow(
                libraries,
                arena,
                Objects.requireNonNull(lifecycle, "lifecycle")
        );
        try {
            nativeWindow.initialize(title, x, y, width, height, popup, owner);
            return nativeWindow;
        } catch (RuntimeException | Error failure) {
            nativeWindow.close();
            throw failure;
        }
    }

    /// Returns the HWND.
    ///
    /// @return the handle
    public MemorySegment handle() {
        requireOpen();
        return window;
    }

    /// Returns the module handle used to create the HWND.
    ///
    /// @return the `HINSTANCE`
    public MemorySegment instance() {
        requireOpen();
        return instance;
    }

    /// Returns the latest client width in physical pixels.
    ///
    /// @return the width
    public int clientWidth() {
        return clientWidth;
    }

    /// Returns the latest client height in physical pixels.
    ///
    /// @return the height
    public int clientHeight() {
        return clientHeight;
    }

    /// Returns the latest window DPI.
    ///
    /// @return the DPI, at least `96` when the host reports zero
    public int dpi() {
        return dpi;
    }

    /// Returns the physical-pixels-per-logical-pixel scale implied by [#dpi()].
    ///
    /// @return the positive scale
    public double scaleFactor() {
        return dpi / (double) USER_DEFAULT_SCREEN_DPI;
    }

    /// `MONITOR_DEFAULTTONEAREST`.
    public static final int MONITOR_DEFAULTTONEAREST = 2;

    /// `MDT_EFFECTIVE_DPI`.
    public static final int MDT_EFFECTIVE_DPI = 0;

    /// Effective X/Y DPI reported by `GetDpiForMonitor`.
    ///
    /// @param x the horizontal DPI
    /// @param y the vertical DPI
    public record MonitorDpi(int x, int y) {
        /// Validates the pair.
        public MonitorDpi {
            if (x <= 0 || y <= 0) {
                throw new IllegalArgumentException("Monitor DPI must be positive");
            }
        }
    }

    /// Reads effective monitor DPI through generated `MonitorFromWindow` and `GetDpiForMonitor`.
    ///
    /// @return the positive X/Y DPI pair
    public MonitorDpi monitorDpi() {
        requireOpen();
        MemorySegment monitor = bindings.monitorFromWindow(window, MONITOR_DEFAULTTONEAREST);
        if (monitor.address() == 0L) {
            throw new IllegalStateException("MonitorFromWindow returned NULL");
        }
        MemorySegment dpiX = arena.allocate(ValueLayout.JAVA_INT);
        MemorySegment dpiY = arena.allocate(ValueLayout.JAVA_INT);
        int status = bindings.getDpiForMonitor(monitor, MDT_EFFECTIVE_DPI, dpiX, dpiY);
        if (status < 0) {
            throw new IllegalStateException("GetDpiForMonitor failed with HRESULT " + status
                    + " (0x" + Integer.toHexString(status) + ')');
        }
        return new MonitorDpi(dpiX.get(ValueLayout.JAVA_INT, 0L), dpiY.get(ValueLayout.JAVA_INT, 0L));
    }

    /// `MONITORINFOF_PRIMARY`.
    public static final int MONITORINFOF_PRIMARY = 0x00000001;

    /// `SM_SWAPBUTTON`.
    public static final int SM_SWAPBUTTON = 23;

    /// `SM_CXDRAG`.
    public static final int SM_CXDRAG = 68;

    /// `SM_CYDRAG`.
    public static final int SM_CYDRAG = 69;

    /// `SM_CXDOUBLECLK`.
    public static final int SM_CXDOUBLECLK = 36;

    /// `SM_CYDOUBLECLK`.
    public static final int SM_CYDOUBLECLK = 37;

    /// `SM_CXICON`.
    public static final int SM_CXICON = 11;

    /// `SM_CYICON`.
    public static final int SM_CYICON = 12;

    /// `SM_CXSMICON`.
    public static final int SM_CXSMICON = 49;

    /// `SM_CYSMICON`.
    public static final int SM_CYSMICON = 50;

    /// `SM_CXCURSOR`.
    public static final int SM_CXCURSOR = 13;

    /// `SM_CYCURSOR`.
    public static final int SM_CYCURSOR = 14;

    /// `SM_CYCAPTION`.
    public static final int SM_CYCAPTION = 4;

    /// `SM_CYMENU`.
    public static final int SM_CYMENU = 15;

    /// `SM_CXBORDER`.
    public static final int SM_CXBORDER = 5;

    /// `SM_CYBORDER`.
    public static final int SM_CYBORDER = 6;

    /// `SM_CXFRAME` / `SM_CXSIZEFRAME`.
    public static final int SM_CXFRAME = 32;

    /// `SM_CYFRAME` / `SM_CYSIZEFRAME`.
    public static final int SM_CYFRAME = 33;

    /// `SM_CXFULLSCREEN`.
    public static final int SM_CXFULLSCREEN = 16;

    /// `SM_CYFULLSCREEN`.
    public static final int SM_CYFULLSCREEN = 17;

    /// `SM_CXHSCROLL`.
    public static final int SM_CXHSCROLL = 21;

    /// `SM_CYHSCROLL`.
    public static final int SM_CYHSCROLL = 3;

    /// `SM_CXVSCROLL`.
    public static final int SM_CXVSCROLL = 2;

    /// `SM_CYVSCROLL`.
    public static final int SM_CYVSCROLL = 20;

    /// `COLOR_WINDOW`.
    public static final int COLOR_WINDOW = 5;

    /// `COLOR_WINDOWTEXT`.
    public static final int COLOR_WINDOWTEXT = 8;

    /// `COLOR_HIGHLIGHT`.
    public static final int COLOR_HIGHLIGHT = 13;

    /// `COLOR_HIGHLIGHTTEXT`.
    public static final int COLOR_HIGHLIGHTTEXT = 14;

    /// `COLOR_GRAYTEXT`.
    public static final int COLOR_GRAYTEXT = 17;

    /// `COLOR_BTNFACE` / `COLOR_3DFACE`.
    public static final int COLOR_BTNFACE = 15;

    /// `COLOR_BTNTEXT`.
    public static final int COLOR_BTNTEXT = 18;

    /// `COLOR_INACTIVEBORDER`.
    public static final int COLOR_INACTIVEBORDER = 11;

    /// `SPI_GETMOUSESPEED`.
    public static final int SPI_GETMOUSESPEED = 0x0070;

    /// `SPI_GETKEYBOARDDELAY`.
    public static final int SPI_GETKEYBOARDDELAY = 0x0016;

    /// `SPI_GETKEYBOARDSPEED`.
    public static final int SPI_GETKEYBOARDSPEED = 0x000A;

    /// `SPI_GETCARETWIDTH`.
    public static final int SPI_GETCARETWIDTH = 0x2004;

    /// `SPI_GETMOUSEHOVERTIME`.
    public static final int SPI_GETMOUSEHOVERTIME = 0x0066;

    /// `SPI_GETMOUSEHOVERWIDTH`.
    public static final int SPI_GETMOUSEHOVERWIDTH = 0x0062;

    /// `SPI_GETMOUSEHOVERHEIGHT`.
    public static final int SPI_GETMOUSEHOVERHEIGHT = 0x0064;

    /// `SPI_GETFONTSMOOTHING`.
    public static final int SPI_GETFONTSMOOTHING = 0x004B;

    /// `SPI_GETKEYBOARDPREF`.
    public static final int SPI_GETKEYBOARDPREF = 0x0044;

    /// `SPI_GETDROPSHADOW`.
    public static final int SPI_GETDROPSHADOW = 0x1024;

    /// `SPI_GETMENUANIMATION`.
    public static final int SPI_GETMENUANIMATION = 0x1002;

    /// `SPI_GETFLATMENU`.
    public static final int SPI_GETFLATMENU = 0x1022;

    /// `SPI_GETMENUDROPALIGNMENT`.
    public static final int SPI_GETMENUDROPALIGNMENT = 0x001B;

    /// `SPI_GETMENUFADE`.
    public static final int SPI_GETMENUFADE = 0x1012;

    /// `SPI_GETCOMBOBOXANIMATION`.
    public static final int SPI_GETCOMBOBOXANIMATION = 0x1004;

    /// `SPI_GETTOOLTIPANIMATION`.
    public static final int SPI_GETTOOLTIPANIMATION = 0x1016;

    /// `SPI_GETSELECTIONFADE`.
    public static final int SPI_GETSELECTIONFADE = 0x1014;

    /// `SPI_GETLISTBOXSMOOTHSCROLLING`.
    public static final int SPI_GETLISTBOXSMOOTHSCROLLING = 0x1006;

    /// `SPI_GETSNAPTODEFBUTTON`.
    public static final int SPI_GETSNAPTODEFBUTTON = 0x005F;

    /// `SPI_GETMENUUNDERLINES` / `SPI_GETKEYBOARDCUES`.
    public static final int SPI_GETMENUUNDERLINES = 0x100A;

    /// `SPI_GETHOTTRACKING`.
    public static final int SPI_GETHOTTRACKING = 0x100E;

    /// `KL_NAMELENGTH`.
    public static final int KL_NAMELENGTH = 9;

    /// Monitor and work rectangles from generated `GetMonitorInfoW`.
    ///
    /// @param monitor the display bounds in screen pixels
    /// @param work the working area excluding taskbars
    /// @param primary whether `MONITORINFOF_PRIMARY` is set
    public record MonitorInfo(ClipRect monitor, ClipRect work, boolean primary) {
        /// Validates the rectangles.
        public MonitorInfo {
            Objects.requireNonNull(monitor, "monitor");
            Objects.requireNonNull(work, "work");
        }
    }

    /// Reads monitor bounds through generated `MonitorFromWindow` and `GetMonitorInfoW`.
    ///
    /// @return the monitor and work rectangles
    public MonitorInfo monitorInfo() {
        requireOpen();
        MemorySegment monitor = bindings.monitorFromWindow(window, MONITOR_DEFAULTTONEAREST);
        if (monitor.address() == 0L) {
            throw new IllegalStateException("MonitorFromWindow returned NULL");
        }
        MemorySegment info = arena.allocate(Win32Layouts.MONITORINFO);
        info.fill((byte) 0);
        info.set(ValueLayout.JAVA_INT, Win32Layouts.MONITORINFO_CB_SIZE_OFFSET,
                Math.toIntExact(Win32Layouts.MONITORINFO.byteSize()));
        Win32FfmBindings.GetMonitorInfoWResult result = bindings.getMonitorInfoW(monitor, info);
        if (result.value() == 0) {
            throw new IllegalStateException("GetMonitorInfoW failed: " + result.errorCode());
        }
        ClipRect display = new ClipRect(
                info.get(ValueLayout.JAVA_INT, Win32Layouts.MONITORINFO_RC_MONITOR_LEFT_OFFSET),
                info.get(ValueLayout.JAVA_INT, Win32Layouts.MONITORINFO_RC_MONITOR_TOP_OFFSET),
                info.get(ValueLayout.JAVA_INT, Win32Layouts.MONITORINFO_RC_MONITOR_RIGHT_OFFSET),
                info.get(ValueLayout.JAVA_INT, Win32Layouts.MONITORINFO_RC_MONITOR_BOTTOM_OFFSET)
        );
        ClipRect work = new ClipRect(
                info.get(ValueLayout.JAVA_INT, Win32Layouts.MONITORINFO_RC_WORK_LEFT_OFFSET),
                info.get(ValueLayout.JAVA_INT, Win32Layouts.MONITORINFO_RC_WORK_TOP_OFFSET),
                info.get(ValueLayout.JAVA_INT, Win32Layouts.MONITORINFO_RC_WORK_RIGHT_OFFSET),
                info.get(ValueLayout.JAVA_INT, Win32Layouts.MONITORINFO_RC_WORK_BOTTOM_OFFSET)
        );
        int flags = info.get(ValueLayout.JAVA_INT, Win32Layouts.MONITORINFO_DW_FLAGS_OFFSET);
        return new MonitorInfo(display, work, (flags & MONITORINFOF_PRIMARY) != 0);
    }

    /// Reads `SM_SWAPBUTTON` through generated `GetSystemMetrics`.
    ///
    /// @return whether the primary and secondary mouse buttons are swapped
    public boolean swapButtons() {
        requireOpen();
        return bindings.getSystemMetrics(SM_SWAPBUTTON) != 0;
    }

    /// Reads `SM_CXDRAG` through generated `GetSystemMetrics`.
    ///
    /// @return the horizontal drag threshold in pixels
    public int dragThresholdX() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CXDRAG);
    }

    /// Reads `SM_CYDRAG` through generated `GetSystemMetrics`.
    ///
    /// @return the vertical drag threshold in pixels
    public int dragThresholdY() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYDRAG);
    }

    /// Reads `SPI_GETMOUSESPEED` through generated `SystemParametersInfoW`.
    ///
    /// @return the mouse speed in `[1, 20]`
    public int mouseSpeed() {
        int speed = systemParameterUint(SPI_GETMOUSESPEED);
        if (speed < 1 || speed > 20) {
            throw new IllegalStateException("SPI_GETMOUSESPEED returned " + speed);
        }
        return speed;
    }

    /// Reads `GetDoubleClickTime` through the generated User32 binding.
    ///
    /// @return the double-click interval in milliseconds
    public int doubleClickTime() {
        requireOpen();
        int time = bindings.getDoubleClickTime();
        if (time <= 0) {
            throw new IllegalStateException("GetDoubleClickTime returned " + time);
        }
        return time;
    }

    /// Reads `SM_CXDOUBLECLK` through generated `GetSystemMetrics`.
    ///
    /// @return the horizontal double-click rectangle width in pixels
    public int doubleClickThresholdX() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CXDOUBLECLK);
    }

    /// Reads `SM_CYDOUBLECLK` through generated `GetSystemMetrics`.
    ///
    /// @return the vertical double-click rectangle height in pixels
    public int doubleClickThresholdY() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYDOUBLECLK);
    }

    /// Reads `GetCaretBlinkTime` through the generated User32 binding.
    ///
    /// A host that disables caret blink reports `INFINITE` (`0xFFFFFFFF`).
    ///
    /// @return the caret blink interval in milliseconds, or `-1` when blink is disabled
    public int caretBlinkTime() {
        requireOpen();
        return bindings.getCaretBlinkTime();
    }

    /// Reads `SPI_GETKEYBOARDDELAY` through generated `SystemParametersInfoW`.
    ///
    /// @return the keyboard repeat delay in `[0, 3]`
    public int keyboardDelay() {
        int delay = systemParameterUint(SPI_GETKEYBOARDDELAY);
        if (delay < 0 || delay > 3) {
            throw new IllegalStateException("SPI_GETKEYBOARDDELAY returned " + delay);
        }
        return delay;
    }

    /// Reads `SPI_GETKEYBOARDSPEED` through generated `SystemParametersInfoW`.
    ///
    /// @return the keyboard repeat speed in `[0, 31]`
    public int keyboardSpeed() {
        int speed = systemParameterUint(SPI_GETKEYBOARDSPEED);
        if (speed < 0 || speed > 31) {
            throw new IllegalStateException("SPI_GETKEYBOARDSPEED returned " + speed);
        }
        return speed;
    }

    /// Reads `SPI_GETCARETWIDTH` through generated `SystemParametersInfoW`.
    ///
    /// @return the caret width in pixels
    public int caretWidth() {
        int width = systemParameterUint(SPI_GETCARETWIDTH);
        if (width <= 0) {
            throw new IllegalStateException("SPI_GETCARETWIDTH returned " + width);
        }
        return width;
    }

    /// Reads `SPI_GETMOUSEHOVERTIME` through generated `SystemParametersInfoW`.
    ///
    /// @return the hover time in milliseconds
    public int mouseHoverTime() {
        int time = systemParameterUint(SPI_GETMOUSEHOVERTIME);
        if (time <= 0) {
            throw new IllegalStateException("SPI_GETMOUSEHOVERTIME returned " + time);
        }
        return time;
    }

    /// Reads `SPI_GETMOUSEHOVERWIDTH` through generated `SystemParametersInfoW`.
    ///
    /// @return the hover rectangle width in pixels
    public int mouseHoverWidth() {
        int width = systemParameterUint(SPI_GETMOUSEHOVERWIDTH);
        if (width <= 0) {
            throw new IllegalStateException("SPI_GETMOUSEHOVERWIDTH returned " + width);
        }
        return width;
    }

    /// Reads `SPI_GETMOUSEHOVERHEIGHT` through generated `SystemParametersInfoW`.
    ///
    /// @return the hover rectangle height in pixels
    public int mouseHoverHeight() {
        int height = systemParameterUint(SPI_GETMOUSEHOVERHEIGHT);
        if (height <= 0) {
            throw new IllegalStateException("SPI_GETMOUSEHOVERHEIGHT returned " + height);
        }
        return height;
    }

    /// Reads `SM_CXICON` through generated `GetSystemMetrics`.
    ///
    /// @return the default icon width in pixels
    public int iconWidth() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CXICON);
    }

    /// Reads `SM_CYICON` through generated `GetSystemMetrics`.
    ///
    /// @return the default icon height in pixels
    public int iconHeight() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYICON);
    }

    /// Reads loaded keyboard layouts through generated `GetKeyboardLayoutList`.
    ///
    /// @return the layout handle addresses, at least one
    public long @Unmodifiable [] keyboardLayouts() {
        requireOpen();
        int count = bindings.getKeyboardLayoutList(0, MemorySegment.NULL);
        if (count <= 0) {
            throw new IllegalStateException("GetKeyboardLayoutList returned " + count);
        }
        MemorySegment list = arena.allocate(ValueLayout.ADDRESS, count);
        int written = bindings.getKeyboardLayoutList(count, list);
        if (written <= 0 || written > count) {
            throw new IllegalStateException("GetKeyboardLayoutList filled " + written);
        }
        long[] layouts = new long[written];
        for (int index = 0; index < written; index++) {
            long handle = list.getAtIndex(ValueLayout.ADDRESS, index).address();
            if (handle == 0L) {
                throw new IllegalStateException("GetKeyboardLayoutList returned a NULL HKL");
            }
            layouts[index] = handle;
        }
        return layouts;
    }

    /// Reads `GetKeyboardLayoutNameW` through the generated User32 binding.
    ///
    /// @return the KLID string, eight hexadecimal digits
    public String keyboardLayoutName() {
        requireOpen();
        MemorySegment buffer = arena.allocate(KL_NAMELENGTH * 2L);
        buffer.fill((byte) 0);
        int ok = bindings.getKeyboardLayoutNameW(buffer);
        if (ok == 0) {
            throw new IllegalStateException("GetKeyboardLayoutNameW failed");
        }
        String name = buffer.getString(0L, StandardCharsets.UTF_16LE);
        if (name.isEmpty()) {
            throw new IllegalStateException("GetKeyboardLayoutNameW returned an empty KLID");
        }
        return name;
    }

    /// Reads `SPI_GETFONTSMOOTHING` through generated `SystemParametersInfoW`.
    ///
    /// @return whether font smoothing is enabled
    public boolean fontSmoothingEnabled() {
        return systemParameterUint(SPI_GETFONTSMOOTHING) != 0;
    }

    /// Reads `SPI_GETKEYBOARDPREF` through generated `SystemParametersInfoW`.
    ///
    /// @return whether the user prefers the keyboard over the mouse
    public boolean keyboardPreferred() {
        return systemParameterUint(SPI_GETKEYBOARDPREF) != 0;
    }

    /// Reads `SM_CXSMICON` through generated `GetSystemMetrics`.
    ///
    /// @return the small icon width in pixels
    public int smallIconWidth() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CXSMICON);
    }

    /// Reads `SM_CYSMICON` through generated `GetSystemMetrics`.
    ///
    /// @return the small icon height in pixels
    public int smallIconHeight() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYSMICON);
    }

    /// Reads `SM_CXCURSOR` through generated `GetSystemMetrics`.
    ///
    /// @return the cursor width in pixels
    public int cursorWidth() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CXCURSOR);
    }

    /// Reads `SM_CYCURSOR` through generated `GetSystemMetrics`.
    ///
    /// @return the cursor height in pixels
    public int cursorHeight() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYCURSOR);
    }

    /// Reads `GetSysColor` through the generated User32 binding.
    ///
    /// @param index the `COLOR_*` index
    /// @return the `COLORREF` (`0x00BBGGRR`)
    public int sysColor(int index) {
        requireOpen();
        return bindings.getSysColor(index);
    }

    /// Reads `COLOR_WINDOW`.
    ///
    /// @return the window background `COLORREF`
    public int windowColor() {
        return sysColor(COLOR_WINDOW);
    }

    /// Reads `COLOR_WINDOWTEXT`.
    ///
    /// @return the window text `COLORREF`
    public int windowTextColor() {
        return sysColor(COLOR_WINDOWTEXT);
    }

    /// Reads `SPI_GETDROPSHADOW` through generated `SystemParametersInfoW`.
    ///
    /// @return whether drop shadows are enabled
    public boolean dropShadowEnabled() {
        return systemParameterUint(SPI_GETDROPSHADOW) != 0;
    }

    /// Reads `SM_CYCAPTION` through generated `GetSystemMetrics`.
    ///
    /// @return the caption height in pixels
    public int captionHeight() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYCAPTION);
    }

    /// Reads `SPI_GETMENUANIMATION` through generated `SystemParametersInfoW`.
    ///
    /// @return whether menu animation is enabled
    public boolean menuAnimationEnabled() {
        return systemParameterUint(SPI_GETMENUANIMATION) != 0;
    }

    /// Reads `SPI_GETFLATMENU` through generated `SystemParametersInfoW`.
    ///
    /// @return whether menus use a flat appearance
    public boolean flatMenuEnabled() {
        return systemParameterUint(SPI_GETFLATMENU) != 0;
    }

    /// Reads `SM_CYMENU` through generated `GetSystemMetrics`.
    ///
    /// @return the single-line menu bar height in pixels
    public int menuHeight() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYMENU);
    }

    /// Reads `SM_CXBORDER` through generated `GetSystemMetrics`.
    ///
    /// @return the window border width in pixels
    public int borderWidth() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CXBORDER);
    }

    /// Reads `SM_CYBORDER` through generated `GetSystemMetrics`.
    ///
    /// @return the window border height in pixels
    public int borderHeight() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYBORDER);
    }

    /// Reads `SPI_GETMENUDROPALIGNMENT` through generated `SystemParametersInfoW`.
    ///
    /// @return whether popup menus drop to the left of the menu item
    public boolean menuDropAlignsLeft() {
        return systemParameterUint(SPI_GETMENUDROPALIGNMENT) != 0;
    }

    /// Reads `SPI_GETMENUFADE` through generated `SystemParametersInfoW`.
    ///
    /// @return whether menu fade animation is enabled
    public boolean menuFadeEnabled() {
        return systemParameterUint(SPI_GETMENUFADE) != 0;
    }

    /// Reads `SPI_GETCOMBOBOXANIMATION` through generated `SystemParametersInfoW`.
    ///
    /// @return whether combo-box slide animation is enabled
    public boolean comboBoxAnimationEnabled() {
        return systemParameterUint(SPI_GETCOMBOBOXANIMATION) != 0;
    }

    /// Reads `COLOR_HIGHLIGHT`.
    ///
    /// @return the selection background `COLORREF`
    public int highlightColor() {
        return sysColor(COLOR_HIGHLIGHT);
    }

    /// Reads `COLOR_HIGHLIGHTTEXT`.
    ///
    /// @return the selection text `COLORREF`
    public int highlightTextColor() {
        return sysColor(COLOR_HIGHLIGHTTEXT);
    }

    /// Reads `SM_CXFRAME` through generated `GetSystemMetrics`.
    ///
    /// @return the sizing-border width in pixels
    public int frameWidth() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CXFRAME);
    }

    /// Reads `SM_CYFRAME` through generated `GetSystemMetrics`.
    ///
    /// @return the sizing-border height in pixels
    public int frameHeight() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYFRAME);
    }

    /// Reads `SPI_GETTOOLTIPANIMATION` through generated `SystemParametersInfoW`.
    ///
    /// @return whether tooltip animation is enabled
    public boolean tooltipAnimationEnabled() {
        return systemParameterUint(SPI_GETTOOLTIPANIMATION) != 0;
    }

    /// Reads `COLOR_GRAYTEXT`.
    ///
    /// @return the disabled-text `COLORREF`
    public int grayTextColor() {
        return sysColor(COLOR_GRAYTEXT);
    }

    /// Reads `SM_CXFULLSCREEN` through generated `GetSystemMetrics`.
    ///
    /// @return the maximized-window client width in pixels
    public int fullscreenWidth() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CXFULLSCREEN);
    }

    /// Reads `SM_CYFULLSCREEN` through generated `GetSystemMetrics`.
    ///
    /// @return the maximized-window client height in pixels
    public int fullscreenHeight() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYFULLSCREEN);
    }

    /// Reads `SPI_GETSELECTIONFADE` through generated `SystemParametersInfoW`.
    ///
    /// @return whether menu-selection fade is enabled
    public boolean selectionFadeEnabled() {
        return systemParameterUint(SPI_GETSELECTIONFADE) != 0;
    }

    /// Reads `SPI_GETLISTBOXSMOOTHSCROLLING` through generated `SystemParametersInfoW`.
    ///
    /// @return whether list-box smooth scrolling is enabled
    public boolean listBoxSmoothScrollingEnabled() {
        return systemParameterUint(SPI_GETLISTBOXSMOOTHSCROLLING) != 0;
    }

    /// Reads `SPI_GETSNAPTODEFBUTTON` through generated `SystemParametersInfoW`.
    ///
    /// @return whether the mouse snaps to the default button
    public boolean snapToDefaultButtonEnabled() {
        return systemParameterUint(SPI_GETSNAPTODEFBUTTON) != 0;
    }

    /// Reads `COLOR_BTNFACE`.
    ///
    /// @return the 3-D face `COLORREF`
    public int buttonFaceColor() {
        return sysColor(COLOR_BTNFACE);
    }

    /// Reads `SM_CXHSCROLL` through generated `GetSystemMetrics`.
    ///
    /// @return the horizontal-scrollbar arrow width in pixels
    public int horizontalScrollArrowWidth() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CXHSCROLL);
    }

    /// Reads `SM_CYHSCROLL` through generated `GetSystemMetrics`.
    ///
    /// @return the horizontal-scrollbar height in pixels
    public int horizontalScrollBarHeight() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYHSCROLL);
    }

    /// Reads `SM_CXVSCROLL` through generated `GetSystemMetrics`.
    ///
    /// @return the vertical-scrollbar width in pixels
    public int verticalScrollBarWidth() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CXVSCROLL);
    }

    /// Reads `SM_CYVSCROLL` through generated `GetSystemMetrics`.
    ///
    /// @return the vertical-scrollbar arrow height in pixels
    public int verticalScrollArrowHeight() {
        requireOpen();
        return bindings.getSystemMetrics(SM_CYVSCROLL);
    }

    /// Reads `SPI_GETMENUUNDERLINES` through generated `SystemParametersInfoW`.
    ///
    /// @return whether menu access-key underlines are always shown
    public boolean menuUnderlinesEnabled() {
        return systemParameterUint(SPI_GETMENUUNDERLINES) != 0;
    }

    /// Reads `SPI_GETHOTTRACKING` through generated `SystemParametersInfoW`.
    ///
    /// @return whether hot-tracking of user-interface elements is enabled
    public boolean hotTrackingEnabled() {
        return systemParameterUint(SPI_GETHOTTRACKING) != 0;
    }

    /// Reads `COLOR_BTNTEXT`.
    ///
    /// @return the button-text `COLORREF`
    public int buttonTextColor() {
        return sysColor(COLOR_BTNTEXT);
    }

    /// Reads `COLOR_INACTIVEBORDER`.
    ///
    /// @return the inactive-window border `COLORREF`
    public int inactiveBorderColor() {
        return sysColor(COLOR_INACTIVEBORDER);
    }

    /// Shows the window without activation.
    public void show() {
        requireOpen();
        bindings.showWindow(window, SW_SHOWNOACTIVATE);
        bindings.updateWindow(window);
        refreshGeometry();
        throwContained();
    }

    /// Hides the window.
    public void hide() {
        requireOpen();
        bindings.showWindow(window, SW_HIDE);
    }

    /// Moves and resizes the window.
    ///
    /// @param x the origin x
    /// @param y the origin y
    /// @param width the width
    /// @param height the height
    public void setBounds(int x, int y, int width, int height) {
        requireOpen();
        bindings.setWindowPos(window, MemorySegment.NULL, x, y, width, height, SWP_NOZORDER | SWP_NOACTIVATE);
        refreshGeometry();
    }

    /// Returns whether a move/resize modal loop is active.
    ///
    /// @return whether the loop is active
    public boolean modalLoopActive() {
        return modalLoopActive;
    }

    /// Returns the number of modal-loop timer ticks observed.
    ///
    /// @return the tick count
    public int modalTimerTicks() {
        return modalTimerTicks;
    }

    /// Returns observed `PBT_APMSUSPEND` deliveries.
    ///
    /// @return the sleep count
    public int sleepEvents() {
        return sleepEvents;
    }

    /// Returns observed `PBT_APMRESUMESUSPEND` deliveries.
    ///
    /// @return the wake count
    public int wakeEvents() {
        return wakeEvents;
    }

    /// Delivers `WM_DPICHANGED` through the production WndProc with a suggested `RECT`.
    ///
    /// @param nextDpi the new DPI for both axes
    /// @param left the suggested left
    /// @param top the suggested top
    /// @param right the suggested right
    /// @param bottom the suggested bottom
    /// @return the `WndProc` result
    public long applyDpiChange(int nextDpi, int left, int top, int right, int bottom) {
        if (nextDpi <= 0) {
            throw new IllegalArgumentException("DPI must be positive");
        }
        MemorySegment rect = arena.allocate(Win32Layouts.RECT);
        rect.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_LEFT_OFFSET, left);
        rect.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_TOP_OFFSET, top);
        rect.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_RIGHT_OFFSET, right);
        rect.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_BOTTOM_OFFSET, bottom);
        long packed = (nextDpi & 0xFFFFL) | ((long) nextDpi << 16);
        return sendMessage(WM_DPICHANGED, packed, rect.address());
    }

    /// Sends one message synchronously through the production WndProc.
    ///
    /// @param message the Win32 message identifier
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    /// @return the `WndProc` result
    public long sendMessage(int message, long wParam, long lParam) {
        requireOpen();
        return bindings.sendMessageW(window, message, wParam, lParam);
    }

    /// Blits unassociated 8-bit sRGB RGBA pixels into this HWND and retains them for `WM_PAINT`.
    ///
    /// @param rgba unassociated 8-bit sRGB pixels in row-major RGBA order
    /// @param width the pixel width
    /// @param height the pixel height
    /// @return the scanline count reported by `SetDIBitsToDevice`
    public int presentSdrRgba(MemorySegment rgba, int width, int height) {
        requireOpen();
        Objects.requireNonNull(rgba, "rgba");
        MemorySegment deviceContext = bindings.getDc(window);
        if (deviceContext.address() == 0L) {
            throw new IllegalStateException("GetDC returned NULL");
        }
        int scanlines;
        try {
            scanlines = WindowsSoftwarePresent.presentSdrRgba(bindings, deviceContext, rgba, width, height);
        } finally {
            if (bindings.releaseDc(window, deviceContext) == 0) {
                throw new IllegalStateException("ReleaseDC failed");
            }
        }
        lastRgba = MemorySegment.ofArray(rgba.toArray(ValueLayout.JAVA_BYTE));
        lastWidth = width;
        lastHeight = height;
        Win32FfmBindings.InvalidateRectResult invalidated = bindings.invalidateRect(window, MemorySegment.NULL, 0);
        if (invalidated.value() == 0) {
            throw new IllegalStateException("InvalidateRect failed: " + invalidated.errorCode());
        }
        return scanlines;
    }

    /// Maps a `POINTER_INPUT_TYPE` onto a device kind.
    ///
    /// @param pointerType the `GetPointerType` result
    /// @return the device kind
    public static PointerDeviceKind deviceKindFromPointerType(int pointerType) {
        return switch (pointerType) {
            case PT_PEN -> PointerDeviceKind.PEN;
            case PT_MOUSE -> PointerDeviceKind.MOUSE;
            default -> PointerDeviceKind.TOUCH;
        };
    }

    /// Loads a system cursor and installs it with generated `LoadCursorW` / `SetCursor`.
    ///
    /// @param cursorId a `MAKEINTRESOURCE` identifier such as [`#IDC_ARROW`]
    /// @return whether both calls returned a non-null handle
    public boolean setSystemCursor(int cursorId) {
        requireOpen();
        if (cursorId <= 0 || cursorId > 0xFFFF) {
            throw new IllegalArgumentException("cursorId must be a 16-bit resource identifier");
        }
        Win32FfmBindings.LoadCursorWResult loaded = bindings.loadCursorW(
                MemorySegment.NULL,
                MemorySegment.ofAddress(cursorId)
        );
        if (loaded.value().address() == 0L) {
            return false;
        }
        lastSystemCursor = loaded.value();
        MemorySegment previous = bindings.setCursor(loaded.value());
        return previous.address() != 0L || loaded.value().address() != 0L;
    }

    /// Returns the cursor currently installed by generated `GetCursor`.
    ///
    /// @return the cursor handle, or a null segment when none is installed
    public MemorySegment currentCursor() {
        requireOpen();
        return bindings.getCursor();
    }

    /// Reads the screen cursor location through generated `GetCursorPos`.
    ///
    /// @return the screen coordinates
    public ScreenPoint cursorPosition() {
        requireOpen();
        MemorySegment point = arena.allocate(Win32Layouts.POINT);
        Win32FfmBindings.GetCursorPosResult result = bindings.getCursorPos(point);
        if (result.value() == 0) {
            throw new IllegalStateException("GetCursorPos failed: " + result.errorCode());
        }
        return new ScreenPoint(
                point.get(ValueLayout.JAVA_INT, Win32Layouts.POINT_X_OFFSET),
                point.get(ValueLayout.JAVA_INT, Win32Layouts.POINT_Y_OFFSET)
        );
    }

    /// Writes the screen cursor location through generated `SetCursorPos`.
    ///
    /// @param x the screen x in pixels
    /// @param y the screen y in pixels
    /// @return whether the host accepted the coordinates
    public boolean setCursorPosition(int x, int y) {
        requireOpen();
        Win32FfmBindings.SetCursorPosResult result = bindings.setCursorPos(x, y);
        if (result.value() == 0) {
            throw new IllegalStateException("SetCursorPos failed: " + result.errorCode());
        }
        return true;
    }

    /// Reads cursor visibility and screen position through generated `GetCursorInfo`.
    ///
    /// @return the host cursor snapshot
    public CursorInfo cursorInfo() {
        requireOpen();
        MemorySegment info = arena.allocate(Win32Layouts.CURSORINFO);
        info.set(ValueLayout.JAVA_INT, Win32Layouts.CURSORINFO_CB_SIZE_OFFSET, (int) Win32Layouts.CURSORINFO.byteSize());
        Win32FfmBindings.GetCursorInfoResult result = bindings.getCursorInfo(info);
        if (result.value() == 0) {
            throw new IllegalStateException("GetCursorInfo failed: " + result.errorCode());
        }
        int flags = info.get(ValueLayout.JAVA_INT, Win32Layouts.CURSORINFO_FLAGS_OFFSET);
        MemorySegment cursor = info.get(ValueLayout.ADDRESS, Win32Layouts.CURSORINFO_CURSOR_OFFSET);
        return new CursorInfo(
                (flags & CURSOR_SHOWING) != 0,
                cursor.address(),
                new ScreenPoint(
                        info.get(
                                ValueLayout.JAVA_INT,
                                Win32Layouts.CURSORINFO_SCREEN_POS_OFFSET + Win32Layouts.POINT_X_OFFSET
                        ),
                        info.get(
                                ValueLayout.JAVA_INT,
                                Win32Layouts.CURSORINFO_SCREEN_POS_OFFSET + Win32Layouts.POINT_Y_OFFSET
                        )
                )
        );
    }

    /// Reads the current cursor clip rectangle through generated `GetClipCursor`.
    ///
    /// @return the clip rectangle in screen pixels
    public ClipRect clipCursorRect() {
        requireOpen();
        MemorySegment area = arena.allocate(Win32Layouts.RECT);
        Win32FfmBindings.GetClipCursorResult result = bindings.getClipCursor(area);
        if (result.value() == 0) {
            throw new IllegalStateException("GetClipCursor failed: " + result.errorCode());
        }
        return new ClipRect(
                area.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_LEFT_OFFSET),
                area.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_TOP_OFFSET),
                area.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_RIGHT_OFFSET),
                area.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_BOTTOM_OFFSET)
        );
    }

    /// Releases the thread cursor clip through generated `ClipCursor(NULL)`.
    ///
    /// @return whether the host accepted the release
    public boolean releaseCursorClip() {
        requireOpen();
        Win32FfmBindings.ClipCursorResult result = bindings.clipCursor(MemorySegment.NULL);
        if (result.value() == 0) {
            throw new IllegalStateException("ClipCursor(NULL) failed: " + result.errorCode());
        }
        return true;
    }

    /// Adjusts the thread cursor display count through generated `ShowCursor`.
    ///
    /// @param show `true` increments the display count; `false` decrements it
    /// @return the display count after the adjustment
    public int showCursor(boolean show) {
        requireOpen();
        return bindings.showCursor(show ? 1 : 0);
    }

    /// Reads `SPI_GETWHEELSCROLLLINES` through generated `SystemParametersInfoW`.
    ///
    /// @return the unsigned line count, or `0xFFFFFFFF` when one page is configured
    public int wheelScrollLines() {
        return systemParameterUint(SPI_GETWHEELSCROLLLINES);
    }

    /// Reads `SPI_GETWHEELSCROLLCHARS` through generated `SystemParametersInfoW`.
    ///
    /// @return the unsigned character count, or `0xFFFFFFFF` when one page is configured
    public int wheelScrollChars() {
        return systemParameterUint(SPI_GETWHEELSCROLLCHARS);
    }

    /// Reads `SPI_GETHIGHCONTRAST` through generated `SystemParametersInfoW`.
    ///
    /// @return whether `HCF_HIGHCONTRASTON` is set
    public boolean highContrastOn() {
        requireOpen();
        MemorySegment info = arena.allocate(Win32Layouts.HIGHCONTRASTW);
        info.fill((byte) 0);
        int size = Math.toIntExact(Win32Layouts.HIGHCONTRASTW.byteSize());
        info.set(ValueLayout.JAVA_INT, Win32Layouts.HIGHCONTRASTW_CB_SIZE_OFFSET, size);
        Win32FfmBindings.SystemParametersInfoWResult result = bindings.systemParametersInfoW(
                SPI_GETHIGHCONTRAST,
                size,
                info,
                0
        );
        if (result.value() == 0) {
            throw new IllegalStateException("SystemParametersInfoW failed for SPI_GETHIGHCONTRAST: "
                    + result.errorCode());
        }
        int flags = info.get(ValueLayout.JAVA_INT, Win32Layouts.HIGHCONTRASTW_DW_FLAGS_OFFSET);
        return (flags & HCF_HIGHCONTRASTON) != 0;
    }

    /// Reads `SPI_GETCLIENTAREAANIMATION` through generated `SystemParametersInfoW`.
    ///
    /// @return whether client-area animations are enabled
    public boolean clientAreaAnimationEnabled() {
        return systemParameterUint(SPI_GETCLIENTAREAANIMATION) != 0;
    }

    /// Reads one `UINT` SystemParametersInfo getter.
    ///
    /// @param action the `SPI_GET*` action
    /// @return the stored `UINT` bit pattern as a Java `int`
    private int systemParameterUint(int action) {
        requireOpen();
        MemorySegment value = arena.allocate(ValueLayout.JAVA_INT);
        Win32FfmBindings.SystemParametersInfoWResult result = bindings.systemParametersInfoW(
                action,
                0,
                value,
                0
        );
        if (result.value() == 0) {
            throw new IllegalStateException("SystemParametersInfoW failed for 0x"
                    + Integer.toHexString(action) + ": " + result.errorCode());
        }
        return value.get(ValueLayout.JAVA_INT, 0);
    }

    /// Screen coordinates reported by [`#cursorPosition()`].
    ///
    /// @param x the screen x in pixels
    /// @param y the screen y in pixels
    public record ScreenPoint(int x, int y) {
    }

    /// Snapshot reported by [`#cursorInfo()`].
    ///
    /// @param showing whether `CURSOR_SHOWING` is set
    /// @param cursorHandle the `HCURSOR` address, or `0` when none
    /// @param position the screen coordinates
    public record CursorInfo(boolean showing, long cursorHandle, ScreenPoint position) {
        /// Validates the snapshot.
        public CursorInfo {
            Objects.requireNonNull(position, "position");
        }
    }

    /// Screen rectangle reported by [`#clipCursorRect()`].
    ///
    /// @param left the inclusive left edge
    /// @param top the inclusive top edge
    /// @param right the exclusive right edge
    /// @param bottom the exclusive bottom edge
    public record ClipRect(int left, int top, int right, int bottom) {
    }

    /// Returns whether generated `GetCapture` reports this HWND.
    ///
    /// @return whether this window owns mouse capture
    public boolean captured() {
        requireOpen();
        return bindings.getCapture().address() == window.address();
    }

    /// `MK_LBUTTON`.
    private static final int MK_LBUTTON = 0x0001;

    /// `MK_RBUTTON`.
    private static final int MK_RBUTTON = 0x0002;

    /// `MK_MBUTTON`.
    private static final int MK_MBUTTON = 0x0010;

    /// `POINTER_MESSAGE_FLAG_FIRSTBUTTON`.
    private static final int POINTER_MESSAGE_FLAG_FIRSTBUTTON = 0x0010;

    /// `POINTER_MESSAGE_FLAG_SECONDBUTTON`.
    private static final int POINTER_MESSAGE_FLAG_SECONDBUTTON = 0x0020;

    /// `POINTER_MESSAGE_FLAG_THIRDBUTTON`.
    private static final int POINTER_MESSAGE_FLAG_THIRDBUTTON = 0x0040;

    /// Builds one wheel event from `wParam`/`lParam`.
    ///
    /// @param type `WHEEL` or `WHEEL_HORIZONTAL`
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    /// @param device the physical pointer
    /// @return the event
    private PointerEvent wheelEvent(
            PointerEventType type,
            long wParam,
            long lParam,
            PointerDeviceKind device
    ) {
        short delta = (short) ((wParam >>> 16) & 0xFFFFL);
        return new PointerEvent(
                type,
                lowWord(lParam),
                highWord(lParam),
                device,
                delta / (float) WHEEL_DELTA,
                0,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                messageTime(),
                mouseButtons(PointerEventType.WHEEL, wParam),
                nextPointerSequence(),
                false
        );
    }

    /// Returns `GetMessageTime` for the message currently dispatched to WndProc.
    ///
    /// @return milliseconds since boot, or `0` when the host reports none
    private long messageTime() {
        int time = bindings.getMessageTime();
        if (time == 0) {
            time = bindings.getTickCount();
        }
        return Integer.toUnsignedLong(time);
    }

    /// Assigns the next per-window pointer sequence identifier.
    ///
    /// @return the positive sequence
    private int nextPointerSequence() {
        pointerSequence++;
        return pointerSequence;
    }

    /// Decodes `MK_*` mouse buttons plus the message kind.
    ///
    /// @param type the normalized type
    /// @param wParam the message `wParam`
    /// @return the button mask
    static int mouseButtons(PointerEventType type, long wParam) {
        int mk = (int) (wParam & 0xFFFFL);
        int buttons = 0;
        if ((mk & MK_LBUTTON) != 0 || type == PointerEventType.DOWN) {
            buttons |= PointerEvent.BUTTON_PRIMARY;
        }
        if ((mk & MK_RBUTTON) != 0 || type == PointerEventType.SECONDARY_DOWN) {
            buttons |= PointerEvent.BUTTON_SECONDARY;
        }
        if ((mk & MK_MBUTTON) != 0 || type == PointerEventType.MIDDLE_DOWN) {
            buttons |= PointerEvent.BUTTON_MIDDLE;
        }
        return buttons;
    }

    /// Decodes `POINTER_MESSAGE_FLAG_*` from the high word of a `WM_POINTER*` `wParam`.
    ///
    /// @param type the normalized type
    /// @param wParam the message `wParam`
    /// @return the button mask
    static int pointerButtons(PointerEventType type, long wParam) {
        int flags = highWord(wParam);
        int buttons = 0;
        if ((flags & POINTER_MESSAGE_FLAG_FIRSTBUTTON) != 0 || type == PointerEventType.DOWN) {
            buttons |= PointerEvent.BUTTON_PRIMARY;
        }
        if ((flags & POINTER_MESSAGE_FLAG_SECONDBUTTON) != 0 || type == PointerEventType.SECONDARY_DOWN) {
            buttons |= PointerEvent.BUTTON_SECONDARY;
        }
        if ((flags & POINTER_MESSAGE_FLAG_THIRDBUTTON) != 0 || type == PointerEventType.MIDDLE_DOWN) {
            buttons |= PointerEvent.BUTTON_MIDDLE;
        }
        return buttons;
    }

    /// Queries `GetPointerType` for `pointerId`.
    ///
    /// @param pointerId the pointer identity from `wParam`
    /// @return the type, or `0` when the query fails
    public int queryPointerType(int pointerId) {
        requireOpen();
        MemorySegment typeOut = arena.allocate(ValueLayout.JAVA_INT);
        Win32FfmBindings.GetPointerTypeResult result = bindings.getPointerType(pointerId, typeOut);
        if (result.value() == 0) {
            return 0;
        }
        return typeOut.get(ValueLayout.JAVA_INT, 0);
    }

    /// `PEN_MASK_PRESSURE`.
    public static final int PEN_MASK_PRESSURE = 0x00000001;

    /// `PEN_FLAG_INVERTED`.
    public static final int PEN_FLAG_INVERTED = 0x00000002;

    /// `PEN_FLAG_ERASER`.
    public static final int PEN_FLAG_ERASER = 0x00000004;

    /// `PEN_MASK_ROTATION`.
    public static final int PEN_MASK_ROTATION = 0x00000002;

    /// `PEN_MASK_TILT_X`.
    public static final int PEN_MASK_TILT_X = 0x00000004;

    /// `PEN_MASK_TILT_Y`.
    public static final int PEN_MASK_TILT_Y = 0x00000008;

    /// Maximum Win32 pen pressure.
    public static final int PEN_MAX_PRESSURE = 1024;

    /// Stores decoded pen axes from [`#queryPenInfo(int)`].
    ///
    /// @param pressure normalized pressure in `[0, 1]`
    /// @param tiltX tilt from the YZ plane in degrees
    /// @param tiltY tilt from the XZ plane in degrees
    /// @param rotation clockwise barrel rotation in degrees in `[0, 359]`
    /// @param inverted whether `PEN_FLAG_INVERTED` is set
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    public record PenAxes(
            float pressure,
            float tiltX,
            float tiltY,
            float rotation,
            boolean inverted,
            boolean eraser
    ) {
        /// Creates axes with no invert or eraser bit.
        ///
        /// @param pressure normalized pressure
        /// @param tiltX tilt from the YZ plane
        /// @param tiltY tilt from the XZ plane
        /// @param rotation clockwise barrel rotation
        public PenAxes(float pressure, float tiltX, float tiltY, float rotation) {
            this(pressure, tiltX, tiltY, rotation, false, false);
        }

        /// Creates axes with invert and no eraser bit.
        ///
        /// @param pressure normalized pressure
        /// @param tiltX tilt from the YZ plane
        /// @param tiltY tilt from the XZ plane
        /// @param rotation clockwise barrel rotation
        /// @param inverted whether the stylus is inverted
        public PenAxes(float pressure, float tiltX, float tiltY, float rotation, boolean inverted) {
            this(pressure, tiltX, tiltY, rotation, inverted, false);
        }
    }

    /// `TOUCH_MASK_CONTACTAREA`.
    public static final int TOUCH_MASK_CONTACTAREA = 0x00000001;

    /// `TOUCH_MASK_ORIENTATION`.
    public static final int TOUCH_MASK_ORIENTATION = 0x00000002;

    /// Stores a decoded `POINTER_TOUCH_INFO` contact ellipse and orientation.
    ///
    /// @param width contact width in pixels
    /// @param height contact height in pixels
    /// @param orientation clockwise contact angle in degrees in `[0, 359]`; `0` when unreported
    public record ContactArea(float width, float height, float orientation) {
        /// Validates the ellipse.
        public ContactArea {
            if (!Float.isFinite(width) || !Float.isFinite(height) || !Float.isFinite(orientation)
                    || width < 0.0f || height < 0.0f) {
                throw new IllegalArgumentException("contact size must be finite and nonnegative");
            }
            if (orientation < 0.0f || orientation > 359.0f) {
                throw new IllegalArgumentException("orientation must be in [0, 359] degrees");
            }
        }

        /// Creates an ellipse with no reported orientation.
        ///
        /// @param width contact width
        /// @param height contact height
        public ContactArea(float width, float height) {
            this(width, height, 0.0f);
        }
    }

    /// `POINTER_FLAG_INRANGE`.
    public static final int POINTER_FLAG_INRANGE = 0x00000002;

    /// `POINTER_FLAG_INCONTACT`.
    public static final int POINTER_FLAG_INCONTACT = 0x00000004;

    /// `POINTER_FLAG_CANCELED`.
    public static final int POINTER_FLAG_CANCELED = 0x00008000;

    /// `POINTER_FLAG_PRIMARY`.
    public static final int POINTER_FLAG_PRIMARY = 0x00002000;

    /// `POINTER_FLAG_FIRSTBUTTON`.
    public static final int POINTER_FLAG_FIRSTBUTTON = 0x00000010;

    /// `POINTER_FLAG_SECONDBUTTON`.
    public static final int POINTER_FLAG_SECONDBUTTON = 0x00000020;

    /// `POINTER_FLAG_THIRDBUTTON`.
    public static final int POINTER_FLAG_THIRDBUTTON = 0x00000040;

    /// `POINTER_FLAG_FOURTHBUTTON`.
    public static final int POINTER_FLAG_FOURTHBUTTON = 0x00000080;

    /// `POINTER_FLAG_FIFTHBUTTON`.
    public static final int POINTER_FLAG_FIFTHBUTTON = 0x00000100;

    /// `POINTER_FLAG_NEW`.
    public static final int POINTER_FLAG_NEW = 0x00000001;

    /// `POINTER_FLAG_CONFIDENCE`.
    public static final int POINTER_FLAG_CONFIDENCE = 0x00004000;

    /// `POINTER_FLAG_DOWN`.
    public static final int POINTER_FLAG_DOWN = 0x00010000;

    /// `POINTER_FLAG_UPDATE`.
    public static final int POINTER_FLAG_UPDATE = 0x00020000;

    /// `POINTER_FLAG_WHEEL`.
    public static final int POINTER_FLAG_WHEEL = 0x00080000;

    /// `POINTER_FLAG_HWHEEL`.
    public static final int POINTER_FLAG_HWHEEL = 0x00100000;

    /// `POINTER_FLAG_CAPTURECHANGED`.
    public static final int POINTER_FLAG_CAPTURECHANGED = 0x00200000;

    /// `POINTER_FLAG_HASTRANSFORM`.
    public static final int POINTER_FLAG_HASTRANSFORM = 0x00400000;

    /// `POINTER_FLAG_UP`.
    public static final int POINTER_FLAG_UP = 0x00040000;

    /// `POINTER_MOD_SHIFT` in `POINTER_INFO.dwKeyStates`.
    public static final int POINTER_MOD_SHIFT = 0x00000004;

    /// `POINTER_MOD_CTRL` in `POINTER_INFO.dwKeyStates`.
    public static final int POINTER_MOD_CTRL = 0x00000008;

    /// `POINTER_CHANGE_FIRSTBUTTON_DOWN`.
    public static final int POINTER_CHANGE_FIRSTBUTTON_DOWN = 1;

    /// Stores decoded `POINTER_INFO` frame id plus hover, contact, canceled, primary, and button bits.
    ///
    /// @param frameId the host frame identity; `0` when unreported
    /// @param inRange whether `POINTER_FLAG_INRANGE` is set
    /// @param inContact whether `POINTER_FLAG_INCONTACT` is set
    /// @param canceled whether `POINTER_FLAG_CANCELED` is set
    /// @param primary whether `POINTER_FLAG_PRIMARY` is set
    /// @param firstButton whether `POINTER_FLAG_FIRSTBUTTON` is set
    /// @param secondButton whether `POINTER_FLAG_SECONDBUTTON` is set
    /// @param thirdButton whether `POINTER_FLAG_THIRDBUTTON` is set
    /// @param fourthButton whether `POINTER_FLAG_FOURTHBUTTON` is set
    /// @param fifthButton whether `POINTER_FLAG_FIFTHBUTTON` is set
    /// @param newPointer whether `POINTER_FLAG_NEW` is set
    /// @param confidence whether `POINTER_FLAG_CONFIDENCE` is set
    /// @param down whether `POINTER_FLAG_DOWN` is set
    /// @param update whether `POINTER_FLAG_UPDATE` is set
    /// @param wheel whether `POINTER_FLAG_WHEEL` is set
    /// @param horizontalWheel whether `POINTER_FLAG_HWHEEL` is set
    /// @param captureChanged whether `POINTER_FLAG_CAPTURECHANGED` is set
    /// @param hasTransform whether `POINTER_FLAG_HASTRANSFORM` is set
    /// @param up whether `POINTER_FLAG_UP` is set
    /// @param historyCount host `POINTER_INFO.historyCount`; `0` when unreported
    /// @param keyStates host `POINTER_INFO.dwKeyStates`; `0` when unreported
    /// @param buttonChangeType host `POINTER_INFO.ButtonChangeType`; `0` when unreported
    /// @param inputData host `POINTER_INFO.InputData`; `0` when unreported
    /// @param performanceCount host `POINTER_INFO.PerformanceCount`; `0` when unreported
    /// @param rawX host `POINTER_INFO.ptPixelLocationRaw.x`; `0` when unreported
    /// @param rawY host `POINTER_INFO.ptPixelLocationRaw.y`; `0` when unreported
    /// @param himetricX host `POINTER_INFO.ptHimetricLocation.x`; `0` when unreported
    /// @param himetricY host `POINTER_INFO.ptHimetricLocation.y`; `0` when unreported
    /// @param himetricRawX host `POINTER_INFO.ptHimetricLocationRaw.x`; `0` when unreported
    /// @param himetricRawY host `POINTER_INFO.ptHimetricLocationRaw.y`; `0` when unreported
    /// @param pointerTime host `POINTER_INFO.dwTime`; `0` when unreported
    /// @param sourceDevice host `POINTER_INFO.sourceDevice` handle address; `0` when unreported
    /// @param hwndTarget host `POINTER_INFO.hwndTarget` handle address; `0` when unreported
    public record PointerFlags(
            int frameId,
            boolean inRange,
            boolean inContact,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer,
            boolean confidence,
            boolean down,
            boolean update,
            boolean wheel,
            boolean horizontalWheel,
            boolean captureChanged,
            boolean hasTransform,
            boolean up,
            int historyCount,
            int keyStates,
            int buttonChangeType,
            int inputData,
            long performanceCount,
            int rawX,
            int rawY,
            int himetricX,
            int himetricY,
            int himetricRawX,
            int himetricRawY,
            int pointerTime,
            long sourceDevice,
            long hwndTarget
    ) {
        /// Validates the frame identity, history count, and remaining `POINTER_INFO` integers.
        public PointerFlags {
            if (frameId < 0) {
                throw new IllegalArgumentException("frameId must be non-negative");
            }
            if (historyCount < 0) {
                throw new IllegalArgumentException("historyCount must be non-negative");
            }
            if (keyStates < 0) {
                throw new IllegalArgumentException("keyStates must be non-negative");
            }
            if (buttonChangeType < 0) {
                throw new IllegalArgumentException("buttonChangeType must be non-negative");
            }
            if (performanceCount < 0L) {
                throw new IllegalArgumentException("performanceCount must be non-negative");
            }
            if (pointerTime < 0) {
                throw new IllegalArgumentException("pointerTime must be non-negative");
            }
        }

        /// Creates flags with a first-button bit and no second-button bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, false, false);
        }

        /// Creates flags with a second-button bit and no third-button bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, secondButton, false, false, false);
        }

        /// Creates flags with a third-button bit and no fourth-button bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, secondButton, thirdButton, false, false, false, false);
        }

        /// Creates flags with a fourth-button bit and no fifth-button bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, secondButton, thirdButton, fourthButton, false, false, false);
        }

        /// Creates flags with a fifth-button bit and no new-pointer bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, secondButton, thirdButton, fourthButton, fifthButton, false, false);
        }

        /// Creates flags with a new-pointer bit and no confidence bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, secondButton, thirdButton, fourthButton, fifthButton, newPointer, false);
        }

        /// Creates flags with a confidence bit and no remaining `POINTER_INFO` flags.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }

        /// Creates flags with remaining `POINTER_INFO` bits except `POINTER_FLAG_UP`.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    false
            );
        }

        /// Creates flags with `POINTER_FLAG_UP` and no reported history count.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    0
            );
        }

        /// Creates flags with a reported history count and no key-state or button-change fields.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        /// @param historyCount host history count
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up,
                int historyCount
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    historyCount,
                    0,
                    0
            );
        }

        /// Creates flags with key-state and button-change fields, and no remaining `POINTER_INFO` integers.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        /// @param historyCount host history count
        /// @param keyStates host modifier bits
        /// @param buttonChangeType host button-change kind
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up,
                int historyCount,
                int keyStates,
                int buttonChangeType
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    historyCount,
                    keyStates,
                    buttonChangeType,
                    0,
                    0L
            );
        }

        /// Creates flags with input-data and performance-count, and no raw or himetric locations.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        /// @param historyCount host history count
        /// @param keyStates host modifier bits
        /// @param buttonChangeType host button-change kind
        /// @param inputData host extra input data
        /// @param performanceCount host performance counter
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up,
                int historyCount,
                int keyStates,
                int buttonChangeType,
                int inputData,
                long performanceCount
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    historyCount,
                    keyStates,
                    buttonChangeType,
                    inputData,
                    performanceCount,
                    0,
                    0,
                    0,
                    0
            );
        }

        /// Creates flags with raw and himetric locations, and no raw himetric locations.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        /// @param historyCount host history count
        /// @param keyStates host modifier bits
        /// @param buttonChangeType host button-change kind
        /// @param inputData host extra input data
        /// @param performanceCount host performance counter
        /// @param rawX raw pixel X
        /// @param rawY raw pixel Y
        /// @param himetricX himetric X
        /// @param himetricY himetric Y
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up,
                int historyCount,
                int keyStates,
                int buttonChangeType,
                int inputData,
                long performanceCount,
                int rawX,
                int rawY,
                int himetricX,
                int himetricY
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    historyCount,
                    keyStates,
                    buttonChangeType,
                    inputData,
                    performanceCount,
                    rawX,
                    rawY,
                    himetricX,
                    himetricY,
                    0,
                    0
            );
        }

        /// Creates flags with raw himetric locations and no `POINTER_INFO.dwTime`.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        /// @param historyCount host history count
        /// @param keyStates host modifier bits
        /// @param buttonChangeType host button-change kind
        /// @param inputData host extra input data
        /// @param performanceCount host performance counter
        /// @param rawX raw pixel X
        /// @param rawY raw pixel Y
        /// @param himetricX himetric X
        /// @param himetricY himetric Y
        /// @param himetricRawX raw himetric X
        /// @param himetricRawY raw himetric Y
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up,
                int historyCount,
                int keyStates,
                int buttonChangeType,
                int inputData,
                long performanceCount,
                int rawX,
                int rawY,
                int himetricX,
                int himetricY,
                int himetricRawX,
                int himetricRawY
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    historyCount,
                    keyStates,
                    buttonChangeType,
                    inputData,
                    performanceCount,
                    rawX,
                    rawY,
                    himetricX,
                    himetricY,
                    himetricRawX,
                    himetricRawY,
                    0
            );
        }

        /// Creates flags with `POINTER_INFO.dwTime` and no producer handles.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        /// @param historyCount host history count
        /// @param keyStates host modifier bits
        /// @param buttonChangeType host button-change kind
        /// @param inputData host extra input data
        /// @param performanceCount host performance counter
        /// @param rawX raw pixel X
        /// @param rawY raw pixel Y
        /// @param himetricX himetric X
        /// @param himetricY himetric Y
        /// @param himetricRawX raw himetric X
        /// @param himetricRawY raw himetric Y
        /// @param pointerTime host message time
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up,
                int historyCount,
                int keyStates,
                int buttonChangeType,
                int inputData,
                long performanceCount,
                int rawX,
                int rawY,
                int himetricX,
                int himetricY,
                int himetricRawX,
                int himetricRawY,
                int pointerTime
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    historyCount,
                    keyStates,
                    buttonChangeType,
                    inputData,
                    performanceCount,
                    rawX,
                    rawY,
                    himetricX,
                    himetricY,
                    himetricRawX,
                    himetricRawY,
                    pointerTime,
                    0L,
                    0L
            );
        }

        /// Creates flags with a primary-contact bit and no first-button bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        public PointerFlags(int frameId, boolean inRange, boolean inContact, boolean canceled, boolean primary) {
            this(frameId, inRange, inContact, canceled, primary, false, false, false);
        }

        /// Creates flags with a canceled bit and no primary-contact bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        public PointerFlags(int frameId, boolean inRange, boolean inContact, boolean canceled) {
            this(frameId, inRange, inContact, canceled, false, false, false, false);
        }

        /// Creates flags with a frame identity and no canceled bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        public PointerFlags(int frameId, boolean inRange, boolean inContact) {
            this(frameId, inRange, inContact, false, false, false, false, false);
        }

        /// Creates flags with no reported frame identity.
        ///
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        public PointerFlags(boolean inRange, boolean inContact) {
            this(0, inRange, inContact, false, false, false, false, false);
        }
    }

    /// Installs pen axes used by the next [`#queryPenInfo(int)`] when the host has no contact.
    ///
    /// @param pointerId the pointer identity
    /// @param axes the axes
    public void installPenAxes(int pointerId, PenAxes axes) {
        Objects.requireNonNull(axes, "axes");
        this.syntheticPenPointerId = pointerId;
        this.syntheticPenAxes = axes;
    }

    /// Installs a contact ellipse used by the next [`#queryTouchInfo(int)`] when the host has no contact.
    ///
    /// @param pointerId the pointer identity
    /// @param contact the ellipse
    public void installTouchContact(int pointerId, ContactArea contact) {
        Objects.requireNonNull(contact, "contact");
        this.syntheticContactPointerId = pointerId;
        this.syntheticContact = contact;
    }

    /// Queries generated `GetPointerTouchInfo` for `pointerId`.
    ///
    /// A failed query returns [#installTouchContact] for `pointerId`, or zeros when none exist.
    ///
    /// @param pointerId the pointer identity
    /// @return the contact ellipse
    public ContactArea queryTouchInfo(int pointerId) {
        requireOpen();
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_TOUCH_INFO);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerTouchInfoResult result = bindings.getPointerTouchInfo(pointerId, info);
        if (result.value() == 0) {
            if (syntheticContact != null && pointerId == syntheticContactPointerId) {
                return syntheticContact;
            }
            return new ContactArea(0.0f, 0.0f);
        }
        return decodeTouchInfo(info);
    }

    /// Reads `rcContact` and `orientation` from a packed `POINTER_TOUCH_INFO`.
    ///
    /// @param info the packed structure
    /// @return the ellipse and orientation; unset mask bits report `0`
    public static ContactArea decodeTouchInfo(MemorySegment info) {
        Objects.requireNonNull(info, "info");
        int mask = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_TOUCHMASK_OFFSET);
        float width = 0.0f;
        float height = 0.0f;
        if ((mask & TOUCH_MASK_CONTACTAREA) != 0) {
            int left = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTLEFT_OFFSET);
            int top = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTTOP_OFFSET);
            int right = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTRIGHT_OFFSET);
            int bottom = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTBOTTOM_OFFSET);
            width = Math.max(0, right - left);
            height = Math.max(0, bottom - top);
        }
        float orientation = 0.0f;
        if ((mask & TOUCH_MASK_ORIENTATION) != 0) {
            int raw = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_ORIENTATION_OFFSET);
            orientation = Math.clamp(raw, 0.0f, 359.0f);
        }
        return new ContactArea(width, height, orientation);
    }

    /// Installs pointer-info flags used by the next [`#queryPointerInfo(int)`] when the host has no contact.
    ///
    /// @param pointerId the pointer identity
    /// @param flags the hover and contact bits
    public void installPointerFlags(int pointerId, PointerFlags flags) {
        Objects.requireNonNull(flags, "flags");
        this.syntheticPointerFlagsId = pointerId;
        this.syntheticPointerFlags = flags;
    }

    /// Queries generated `GetPointerInfo` for `pointerId`.
    ///
    /// A failed query returns [#installPointerFlags] for `pointerId`, or both bits unset when none exist.
    ///
    /// @param pointerId the pointer identity
    /// @return the hover and contact bits
    public PointerFlags queryPointerInfo(int pointerId) {
        requireOpen();
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_INFO);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerInfoResult result = bindings.getPointerInfo(pointerId, info);
        lastPointerSourceDevice = info.get(ValueLayout.ADDRESS, Win32Layouts.POINTER_INFO_SOURCEDEVICE_OFFSET).address();
        lastPointerHwndTarget = info.get(ValueLayout.ADDRESS, Win32Layouts.POINTER_INFO_HWNDTARGET_OFFSET).address();
        if (result.value() == 0) {
            if (syntheticPointerFlags != null && pointerId == syntheticPointerFlagsId) {
                return syntheticPointerFlags;
            }
            return new PointerFlags(false, false);
        }
        return decodePointerInfo(info);
    }

    /// Queries generated `GetPointerFrameInfo` and records the frame pointer count.
    ///
    /// A failed query stores `0` so posted synthetic contacts still prove the production call.
    ///
    /// @param pointerId the pointer identity
    /// @return the frame pointer count, or `0` when the host has no live frame
    public int queryPointerFrameCount(int pointerId) {
        requireOpen();
        MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
        count.set(ValueLayout.JAVA_INT, 0L, 1);
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_INFO);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerFrameInfoResult result = bindings.getPointerFrameInfo(pointerId, count, info);
        if (result.value() == 0) {
            lastPointerFrameCount = 0;
            return 0;
        }
        lastPointerFrameCount = Math.max(0, count.get(ValueLayout.JAVA_INT, 0L));
        return lastPointerFrameCount;
    }

    /// Returns the last `GetPointerFrameInfo` pointer count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFrameCount() {
        return lastPointerFrameCount;
    }

    /// Queries generated `GetPointerFrameInfoHistory` and records the frame history counts.
    ///
    /// A failed query stores `0` so posted synthetic contacts still prove the production call.
    ///
    /// @param pointerId the pointer identity
    /// @return the historical frame entry count, or `0` when the host has no samples
    public int queryPointerFrameHistoryCount(int pointerId) {
        requireOpen();
        MemorySegment entries = arena.allocate(ValueLayout.JAVA_INT);
        MemorySegment pointers = arena.allocate(ValueLayout.JAVA_INT);
        entries.set(ValueLayout.JAVA_INT, 0L, POINTER_HISTORY_CAPACITY);
        pointers.set(ValueLayout.JAVA_INT, 0L, 8);
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_INFO, POINTER_HISTORY_CAPACITY * 8L);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerFrameInfoHistoryResult result = bindings.getPointerFrameInfoHistory(
                pointerId,
                entries,
                pointers,
                info
        );
        if (result.value() == 0) {
            lastPointerFrameHistoryEntries = 0;
            lastPointerFrameHistoryPointers = 0;
            return 0;
        }
        lastPointerFrameHistoryEntries = Math.max(0, entries.get(ValueLayout.JAVA_INT, 0L));
        lastPointerFrameHistoryPointers = Math.max(0, pointers.get(ValueLayout.JAVA_INT, 0L));
        return lastPointerFrameHistoryEntries;
    }

    /// Queries generated `GetPointerFramePenInfoHistory` and records the frame history count.
    ///
    /// @param pointerId the pointer identity
    /// @return the historical frame entry count, or `0` when the host has no samples
    public int queryPointerFramePenHistoryCount(int pointerId) {
        requireOpen();
        MemorySegment entries = arena.allocate(ValueLayout.JAVA_INT);
        MemorySegment pointers = arena.allocate(ValueLayout.JAVA_INT);
        entries.set(ValueLayout.JAVA_INT, 0L, POINTER_HISTORY_CAPACITY);
        pointers.set(ValueLayout.JAVA_INT, 0L, 8);
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_PEN_INFO, POINTER_HISTORY_CAPACITY * 8L);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerFramePenInfoHistoryResult result = bindings.getPointerFramePenInfoHistory(
                pointerId,
                entries,
                pointers,
                info
        );
        if (result.value() == 0) {
            lastPointerFramePenHistoryEntries = 0;
            return 0;
        }
        lastPointerFramePenHistoryEntries = Math.max(0, entries.get(ValueLayout.JAVA_INT, 0L));
        return lastPointerFramePenHistoryEntries;
    }

    /// Queries generated `GetPointerFrameTouchInfoHistory` and records the frame history count.
    ///
    /// @param pointerId the pointer identity
    /// @return the historical frame entry count, or `0` when the host has no samples
    public int queryPointerFrameTouchHistoryCount(int pointerId) {
        requireOpen();
        MemorySegment entries = arena.allocate(ValueLayout.JAVA_INT);
        MemorySegment pointers = arena.allocate(ValueLayout.JAVA_INT);
        entries.set(ValueLayout.JAVA_INT, 0L, POINTER_HISTORY_CAPACITY);
        pointers.set(ValueLayout.JAVA_INT, 0L, 8);
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_TOUCH_INFO, POINTER_HISTORY_CAPACITY * 8L);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerFrameTouchInfoHistoryResult result = bindings.getPointerFrameTouchInfoHistory(
                pointerId,
                entries,
                pointers,
                info
        );
        if (result.value() == 0) {
            lastPointerFrameTouchHistoryEntries = 0;
            return 0;
        }
        lastPointerFrameTouchHistoryEntries = Math.max(0, entries.get(ValueLayout.JAVA_INT, 0L));
        return lastPointerFrameTouchHistoryEntries;
    }

    /// Returns the last `GetPointerFrameInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFrameHistoryEntries() {
        return lastPointerFrameHistoryEntries;
    }

    /// Returns the last `GetPointerFrameInfoHistory` pointer count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFrameHistoryPointers() {
        return lastPointerFrameHistoryPointers;
    }

    /// Returns the last `POINTER_INFO.sourceDevice` handle address.
    ///
    /// @return the handle, or `-1` before a query
    public long lastPointerSourceDevice() {
        return lastPointerSourceDevice;
    }

    /// Returns the last `POINTER_INFO.hwndTarget` handle address.
    ///
    /// @return the handle, or `-1` before a query
    public long lastPointerHwndTarget() {
        return lastPointerHwndTarget;
    }

    /// Queries generated `GetPointerFramePenInfo` and records the frame pointer count.
    ///
    /// @param pointerId the pointer identity
    /// @return the frame pointer count, or `0` when the host has no live pen frame
    public int queryPointerFramePenCount(int pointerId) {
        requireOpen();
        MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
        count.set(ValueLayout.JAVA_INT, 0L, 1);
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_PEN_INFO);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerFramePenInfoResult result = bindings.getPointerFramePenInfo(pointerId, count, info);
        if (result.value() == 0) {
            lastPointerFramePenCount = 0;
            return 0;
        }
        lastPointerFramePenCount = Math.max(0, count.get(ValueLayout.JAVA_INT, 0L));
        return lastPointerFramePenCount;
    }

    /// Queries generated `GetPointerFrameTouchInfo` and records the frame pointer count.
    ///
    /// @param pointerId the pointer identity
    /// @return the frame pointer count, or `0` when the host has no live touch frame
    public int queryPointerFrameTouchCount(int pointerId) {
        requireOpen();
        MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
        count.set(ValueLayout.JAVA_INT, 0L, 1);
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_TOUCH_INFO);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerFrameTouchInfoResult result = bindings.getPointerFrameTouchInfo(pointerId, count, info);
        if (result.value() == 0) {
            lastPointerFrameTouchCount = 0;
            return 0;
        }
        lastPointerFrameTouchCount = Math.max(0, count.get(ValueLayout.JAVA_INT, 0L));
        return lastPointerFrameTouchCount;
    }

    /// Returns the last `GetPointerFramePenInfo` pointer count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFramePenCount() {
        return lastPointerFramePenCount;
    }

    /// Returns the last `GetPointerFrameTouchInfo` pointer count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFrameTouchCount() {
        return lastPointerFrameTouchCount;
    }

    /// Queries generated `GetPointerInfoHistory` and records the historical entry count.
    ///
    /// A failed query stores `0` so posted synthetic contacts still prove the production call.
    ///
    /// @param pointerId the pointer identity
    /// @return the history count, or `0` when the host has no samples
    public int queryPointerHistoryCount(int pointerId) {
        requireOpen();
        MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
        count.set(ValueLayout.JAVA_INT, 0L, POINTER_HISTORY_CAPACITY);
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_INFO, POINTER_HISTORY_CAPACITY);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerInfoHistoryResult result = bindings.getPointerInfoHistory(pointerId, count, info);
        if (result.value() == 0) {
            lastPointerHistoryCount = 0;
            return 0;
        }
        lastPointerHistoryCount = Math.max(0, count.get(ValueLayout.JAVA_INT, 0L));
        return lastPointerHistoryCount;
    }

    /// Queries generated `GetPointerPenInfoHistory` and records the historical entry count.
    ///
    /// @param pointerId the pointer identity
    /// @return the history count, or `0` when the host has no samples
    public int queryPointerPenHistoryCount(int pointerId) {
        requireOpen();
        MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
        count.set(ValueLayout.JAVA_INT, 0L, POINTER_HISTORY_CAPACITY);
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_PEN_INFO, POINTER_HISTORY_CAPACITY);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerPenInfoHistoryResult result = bindings.getPointerPenInfoHistory(pointerId, count, info);
        if (result.value() == 0) {
            lastPointerPenHistoryCount = 0;
            return 0;
        }
        lastPointerPenHistoryCount = Math.max(0, count.get(ValueLayout.JAVA_INT, 0L));
        return lastPointerPenHistoryCount;
    }

    /// Queries generated `GetPointerTouchInfoHistory` and records the historical entry count.
    ///
    /// @param pointerId the pointer identity
    /// @return the history count, or `0` when the host has no samples
    public int queryPointerTouchHistoryCount(int pointerId) {
        requireOpen();
        MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
        count.set(ValueLayout.JAVA_INT, 0L, POINTER_HISTORY_CAPACITY);
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_TOUCH_INFO, POINTER_HISTORY_CAPACITY);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerTouchInfoHistoryResult result = bindings.getPointerTouchInfoHistory(pointerId, count, info);
        if (result.value() == 0) {
            lastPointerTouchHistoryCount = 0;
            return 0;
        }
        lastPointerTouchHistoryCount = Math.max(0, count.get(ValueLayout.JAVA_INT, 0L));
        return lastPointerTouchHistoryCount;
    }

    /// Returns the last `GetPointerInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerHistoryCount() {
        return lastPointerHistoryCount;
    }

    /// Returns the last `GetPointerPenInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerPenHistoryCount() {
        return lastPointerPenHistoryCount;
    }

    /// Returns the last `GetPointerTouchInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerTouchHistoryCount() {
        return lastPointerTouchHistoryCount;
    }

    /// Returns the last `GetRawInputBuffer` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastRawInputBufferBytes() {
        return lastRawInputBufferBytes;
    }

    /// Returns the last `GetRawInputBuffer` packet count.
    ///
    /// @return the packet count, or `Integer.MIN_VALUE` before a query
    public int lastRawInputBufferPackets() {
        return lastRawInputBufferPackets;
    }

    /// Returns the last `GetRegisteredRawInputDevices` device count.
    ///
    /// @return the device count, or `Integer.MIN_VALUE` before a query
    public int lastRegisteredRawInputDevices() {
        return lastRegisteredRawInputDevices;
    }

    /// Returns the last `ImmGetGuideLineW` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastGuideLineBytes() {
        return lastGuideLineBytes;
    }

    /// Removes and returns `ImmGetGuideLineW` text delivered since the last drain.
    ///
    /// @return the guideline, possibly empty
    public String takeGuideline() {
        String text = guideline.toString();
        guideline.setLength(0);
        return text;
    }

    /// Returns the last `ImmGetCandidateListCountW` list count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastCandidateListCount() {
        return lastCandidateListCount;
    }

    /// Returns whether generated `RegisterRawInputDevices` succeeded for this HWND.
    ///
    /// @return whether keyboard and mouse raw input is registered
    public boolean rawInputRegistered() {
        return rawInputRegistered;
    }

    /// Returns the last `GetRawInputData` header size.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastRawInputBytes() {
        return lastRawInputBytes;
    }

    /// Registers keyboard and mouse raw input for this HWND through generated `RegisterRawInputDevices`.
    private void registerRawInput() {
        MemorySegment devices = arena.allocate(Win32Layouts.RAWINPUTDEVICE, 2);
        devices.fill((byte) 0);
        long stride = Win32Layouts.RAWINPUTDEVICE.byteSize();
        writeRawInputDevice(devices, 0L, HID_USAGE_GENERIC_KEYBOARD);
        writeRawInputDevice(devices, stride, HID_USAGE_GENERIC_MOUSE);
        Win32FfmBindings.RegisterRawInputDevicesResult registered = bindings.registerRawInputDevices(
                devices,
                2,
                Math.toIntExact(stride)
        );
        rawInputRegistered = registered.value() != 0;
        MemorySegment deviceCount = arena.allocate(ValueLayout.JAVA_INT);
        deviceCount.set(ValueLayout.JAVA_INT, 0L, 0);
        Win32FfmBindings.GetRegisteredRawInputDevicesResult probe = bindings.getRegisteredRawInputDevices(
                MemorySegment.NULL,
                deviceCount,
                Math.toIntExact(stride)
        );
        int reported = deviceCount.get(ValueLayout.JAVA_INT, 0L);
        if (probe.value() != -1 && reported > 0) {
            lastRegisteredRawInputDevices = reported;
        } else if (reported > 0) {
            MemorySegment registeredDevices = arena.allocate(Win32Layouts.RAWINPUTDEVICE, reported);
            deviceCount.set(ValueLayout.JAVA_INT, 0L, reported);
            Win32FfmBindings.GetRegisteredRawInputDevicesResult copied = bindings.getRegisteredRawInputDevices(
                    registeredDevices,
                    deviceCount,
                    Math.toIntExact(stride)
            );
            lastRegisteredRawInputDevices = copied.value() == -1
                    ? reported
                    : Math.max(0, copied.value());
        } else {
            lastRegisteredRawInputDevices = Math.max(0, reported);
        }
        MemorySegment listCount = arena.allocate(ValueLayout.JAVA_INT);
        listCount.set(ValueLayout.JAVA_INT, 0L, 0);
        Win32FfmBindings.GetRawInputDeviceListResult listed = bindings.getRawInputDeviceList(
                MemorySegment.NULL,
                listCount,
                RAWINPUTDEVICELIST_SIZE
        );
        int listedCount = listCount.get(ValueLayout.JAVA_INT, 0L);
        lastRawInputDeviceListCount = listed.value() == -1 ? Math.max(0, listedCount) : Math.max(0, listed.value());
    }

    /// Packs one `RAWINPUTDEVICE` targeting this HWND.
    private void writeRawInputDevice(MemorySegment devices, long offset, int usage) {
        devices.set(ValueLayout.JAVA_SHORT, offset + Win32Layouts.RAWINPUTDEVICE_US_USAGE_PAGE_OFFSET, (short) HID_USAGE_PAGE_GENERIC);
        devices.set(ValueLayout.JAVA_SHORT, offset + Win32Layouts.RAWINPUTDEVICE_US_USAGE_OFFSET, (short) usage);
        devices.set(ValueLayout.JAVA_INT, offset + Win32Layouts.RAWINPUTDEVICE_DW_FLAGS_OFFSET, 0);
        devices.set(ValueLayout.ADDRESS, offset + Win32Layouts.RAWINPUTDEVICE_HWND_TARGET_OFFSET, window);
    }

    /// Reads the `RAWINPUTHEADER` for a `WM_INPUT` `lParam` through generated `GetRawInputData`.
    private void readRawInput(long raw) {
        MemorySegment size = arena.allocate(ValueLayout.JAVA_INT);
        size.set(ValueLayout.JAVA_INT, 0L, RAWINPUTHEADER_SIZE);
        MemorySegment header = arena.allocate(RAWINPUTHEADER_SIZE);
        lastRawInputBytes = bindings.getRawInputData(
                MemorySegment.ofAddress(raw),
                RID_HEADER,
                header,
                size,
                RAWINPUTHEADER_SIZE
        );
        MemorySegment bufferSize = arena.allocate(ValueLayout.JAVA_INT);
        bufferSize.set(ValueLayout.JAVA_INT, 0L, 0);
        Win32FfmBindings.GetRawInputBufferResult probe = bindings.getRawInputBuffer(
                MemorySegment.NULL,
                bufferSize,
                RAWINPUTHEADER_SIZE
        );
        lastRawInputBufferBytes = Math.max(0, bufferSize.get(ValueLayout.JAVA_INT, 0L));
        lastRawInputBufferPackets = probe.value();
        if (lastRawInputBufferBytes > 0) {
            MemorySegment buffer = arena.allocate(lastRawInputBufferBytes);
            bufferSize.set(ValueLayout.JAVA_INT, 0L, lastRawInputBufferBytes);
            Win32FfmBindings.GetRawInputBufferResult drained = bindings.getRawInputBuffer(
                    buffer,
                    bufferSize,
                    RAWINPUTHEADER_SIZE
            );
            lastRawInputBufferPackets = drained.value();
            lastRawInputBufferBytes = Math.max(lastRawInputBufferBytes, bufferSize.get(ValueLayout.JAVA_INT, 0L));
        }
        MemorySegment device = header.get(ValueLayout.ADDRESS, 8L);
        MemorySegment nameSize = arena.allocate(ValueLayout.JAVA_INT);
        nameSize.set(ValueLayout.JAVA_INT, 0L, 0);
        lastRawInputDeviceInfoBytes = bindings.getRawInputDeviceInfoW(
                device,
                RIDI_DEVICENAME,
                MemorySegment.NULL,
                nameSize
        );
        int required = nameSize.get(ValueLayout.JAVA_INT, 0L);
        if (required > 0) {
            MemorySegment name = arena.allocate((long) required * 2L);
            nameSize.set(ValueLayout.JAVA_INT, 0L, required);
            lastRawInputDeviceInfoBytes = bindings.getRawInputDeviceInfoW(
                    device,
                    RIDI_DEVICENAME,
                    name,
                    nameSize
            );
        }
    }

    /// Reads `frameId`, `historyCount`, and `POINTER_FLAG_*` bits from a packed `POINTER_INFO`.
    ///
    /// @param info the packed structure
    /// @return the frame identity plus hover and contact bits
    public static PointerFlags decodePointerInfo(MemorySegment info) {
        Objects.requireNonNull(info, "info");
        int frameId = Math.max(0, info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_FRAMEID_OFFSET));
        int flags = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_POINTERFLAGS_OFFSET);
        int historyCount = Math.max(0, info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_HISTORYCOUNT_OFFSET));
        int inputData = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_INPUTDATA_OFFSET);
        int keyStates = Math.max(0, info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_KEYSTATES_OFFSET));
        long performanceCount = Math.max(0L, info.get(ValueLayout.JAVA_LONG, Win32Layouts.POINTER_INFO_PERFORMANCECOUNT_OFFSET));
        int buttonChangeType = Math.max(0, info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_BUTTONCHANGETYPE_OFFSET));
        int rawX = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_PIXELLOCATIONRAWX_OFFSET);
        int rawY = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_PIXELLOCATIONRAWY_OFFSET);
        int himetricX = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_HIMETRICLOCATIONX_OFFSET);
        int himetricY = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_HIMETRICLOCATIONY_OFFSET);
        int himetricRawX = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_HIMETRICLOCATIONRAWX_OFFSET);
        int himetricRawY = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_HIMETRICLOCATIONRAWY_OFFSET);
        int pointerTime = Math.max(0, info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_DWTIME_OFFSET));
        long sourceDevice = info.get(ValueLayout.ADDRESS, Win32Layouts.POINTER_INFO_SOURCEDEVICE_OFFSET).address();
        long hwndTarget = info.get(ValueLayout.ADDRESS, Win32Layouts.POINTER_INFO_HWNDTARGET_OFFSET).address();
        return new PointerFlags(
                frameId,
                (flags & POINTER_FLAG_INRANGE) != 0,
                (flags & POINTER_FLAG_INCONTACT) != 0,
                (flags & POINTER_FLAG_CANCELED) != 0,
                (flags & POINTER_FLAG_PRIMARY) != 0,
                (flags & POINTER_FLAG_FIRSTBUTTON) != 0,
                (flags & POINTER_FLAG_SECONDBUTTON) != 0,
                (flags & POINTER_FLAG_THIRDBUTTON) != 0,
                (flags & POINTER_FLAG_FOURTHBUTTON) != 0,
                (flags & POINTER_FLAG_FIFTHBUTTON) != 0,
                (flags & POINTER_FLAG_NEW) != 0,
                (flags & POINTER_FLAG_CONFIDENCE) != 0,
                (flags & POINTER_FLAG_DOWN) != 0,
                (flags & POINTER_FLAG_UPDATE) != 0,
                (flags & POINTER_FLAG_WHEEL) != 0,
                (flags & POINTER_FLAG_HWHEEL) != 0,
                (flags & POINTER_FLAG_CAPTURECHANGED) != 0,
                (flags & POINTER_FLAG_HASTRANSFORM) != 0,
                (flags & POINTER_FLAG_UP) != 0,
                historyCount,
                keyStates,
                buttonChangeType,
                inputData,
                performanceCount,
                rawX,
                rawY,
                himetricX,
                himetricY,
                himetricRawX,
                himetricRawY,
                pointerTime,
                sourceDevice,
                hwndTarget
        );
    }

    /// Queries generated `GetPointerPenInfo` for `pointerId`.
    ///
    /// A failed query returns [#installPenAxes] axes for `pointerId`, or zeros when none exist.
    ///
    /// @param pointerId the pointer identity
    /// @return the axes
    public PenAxes queryPenInfo(int pointerId) {
        requireOpen();
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_PEN_INFO);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerPenInfoResult result = bindings.getPointerPenInfo(pointerId, info);
        if (result.value() == 0) {
            if (syntheticPenAxes != null && pointerId == syntheticPenPointerId) {
                return syntheticPenAxes;
            }
            return new PenAxes(0.0f, 0.0f, 0.0f, 0.0f);
        }
        return decodePenInfo(info);
    }

    /// Reads pressure and tilt from a `POINTER_PEN_INFO` record.
    ///
    /// @param info the packed structure
    /// @return the axes
    public static PenAxes decodePenInfo(MemorySegment info) {
        Objects.requireNonNull(info, "info");
        int mask = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_PENMASK_OFFSET);
        float pressure = 0.0f;
        if ((mask & PEN_MASK_PRESSURE) != 0) {
            int raw = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_PRESSURE_OFFSET);
            pressure = Math.clamp(raw / (float) PEN_MAX_PRESSURE, 0.0f, 1.0f);
        }
        float tiltX = 0.0f;
        if ((mask & PEN_MASK_TILT_X) != 0) {
            tiltX = Math.clamp(
                    info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_TILTX_OFFSET),
                    -90.0f,
                    90.0f
            );
        }
        float tiltY = 0.0f;
        if ((mask & PEN_MASK_TILT_Y) != 0) {
            tiltY = Math.clamp(
                    info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_TILTY_OFFSET),
                    -90.0f,
                    90.0f
            );
        }
        float rotation = 0.0f;
        if ((mask & PEN_MASK_ROTATION) != 0) {
            int raw = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_ROTATION_OFFSET);
            rotation = Math.clamp(raw, 0.0f, 359.0f);
        }
        int flags = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_PENFLAGS_OFFSET);
        boolean inverted = (flags & PEN_FLAG_INVERTED) != 0;
        boolean eraser = (flags & PEN_FLAG_ERASER) != 0;
        return new PenAxes(pressure, tiltX, tiltY, rotation, inverted, eraser);
    }

    /// Resolves the device for one `WM_POINTER*` `wParam`.
    private PointerDeviceKind pointerDevice(long wParam) {
        int pointerId = lowWord(wParam);
        if (syntheticPenAxes != null && pointerId == syntheticPenPointerId) {
            return PointerDeviceKind.PEN;
        }
        if (syntheticContact != null && pointerId == syntheticContactPointerId) {
            return PointerDeviceKind.TOUCH;
        }
        int type = queryPointerType(pointerId);
        if (type == 0) {
            return PointerDeviceKind.TOUCH;
        }
        return deviceKindFromPointerType(type);
    }

    /// Builds one `WM_POINTER*` event, including pen axes when the contact is a stylus.
    ///
    /// @param type the normalized type
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    /// @return the event
    private PointerEvent pointerMessage(PointerEventType type, long wParam, long lParam) {
        PointerDeviceKind device = pointerDevice(wParam);
        int pointerId = lowWord(wParam);
        float pressure = 0.0f;
        float tiltX = 0.0f;
        float tiltY = 0.0f;
        float rotation = 0.0f;
        boolean inverted = false;
        boolean eraser = false;
        if (device == PointerDeviceKind.PEN) {
            PenAxes axes = queryPenInfo(pointerId);
            pressure = axes.pressure();
            tiltX = axes.tiltX();
            tiltY = axes.tiltY();
            rotation = axes.rotation();
            inverted = axes.inverted();
            eraser = axes.eraser();
        }
        ContactArea contact = queryTouchInfo(pointerId);
        PointerFlags flags = queryPointerInfo(pointerId);
        queryPointerFrameCount(pointerId);
        queryPointerFrameHistoryCount(pointerId);
        int history = queryPointerHistoryCount(pointerId);
        queryPointerCursorId(pointerId);
        long pointerDevice = queryPointerDevice(pointerId);
        queryPointerDeviceRects(pointerDevice);
        queryPointerDeviceProperties(pointerDevice);
        queryPointerDeviceCursors(pointerDevice);
        skipPointerFrame(pointerId);
        if (device == PointerDeviceKind.PEN) {
            queryPointerFramePenCount(pointerId);
            queryPointerPenHistoryCount(pointerId);
            queryPointerFramePenHistoryCount(pointerId);
        } else if (device == PointerDeviceKind.TOUCH) {
            queryPointerFrameTouchCount(pointerId);
            queryPointerTouchHistoryCount(pointerId);
            queryPointerFrameTouchHistoryCount(pointerId);
        }
        return new PointerEvent(
                type,
                lowWord(lParam),
                highWord(lParam),
                device,
                0.0f,
                pointerId,
                pressure,
                tiltX,
                tiltY,
                rotation,
                messageTime(),
                pointerButtons(type, wParam),
                nextPointerSequence(),
                false,
                inverted,
                eraser,
                contact.width(),
                contact.height(),
                contact.orientation(),
                flags.inRange(),
                flags.inContact(),
                flags.frameId(),
                flags.canceled(),
                flags.primary(),
                flags.firstButton(),
                flags.secondButton(),
                flags.thirdButton(),
                flags.fourthButton(),
                flags.fifthButton(),
                flags.newPointer(),
                flags.confidence(),
                flags.down(),
                flags.update(),
                flags.wheel(),
                flags.horizontalWheel(),
                flags.captureChanged(),
                flags.hasTransform(),
                flags.up(),
                Math.max(flags.historyCount(), history),
                flags.keyStates() | asyncModifierStates(),
                flags.buttonChangeType(),
                flags.inputData(),
                flags.performanceCount(),
                flags.rawX(),
                flags.rawY(),
                flags.himetricX(),
                flags.himetricY(),
                flags.himetricRawX(),
                flags.himetricRawY(),
                flags.pointerTime(),
                flags.sourceDevice(),
                flags.hwndTarget()
        );
    }

    /// Builds one `WM_XBUTTON*` event with `BUTTON_X1` or `BUTTON_X2`.
    ///
    /// @param down whether the extra button pressed
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    /// @return the event
    private PointerEvent xButtonEvent(boolean down, long wParam, long lParam) {
        int which = highWord(wParam);
        int buttons = 0;
        if (down && which == 1) {
            buttons = PointerEvent.BUTTON_X1;
        } else if (down && which == 2) {
            buttons = PointerEvent.BUTTON_X2;
        }
        return new PointerEvent(
                down ? PointerEventType.DOWN : PointerEventType.UP,
                lowWord(lParam),
                highWord(lParam),
                PointerDeviceKind.MOUSE,
                0.0f,
                0,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                messageTime(),
                buttons,
                nextPointerSequence(),
                false
        );
    }

    /// Builds one `WM_MOUSE*` event with host timestamp, buttons, and sequence.
    ///
    /// @param type the normalized type
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    /// @return the event
    private PointerEvent mouseEvent(PointerEventType type, long wParam, long lParam) {
        return new PointerEvent(
                type,
                lowWord(lParam),
                highWord(lParam),
                PointerDeviceKind.MOUSE,
                0.0f,
                0,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                messageTime(),
                mouseButtons(type, wParam),
                nextPointerSequence(),
                false
        );
    }

    /// Builds one `WM_KEY*` event from `lParam` scan-code and previous-state bits.
    ///
    /// @param type the normalized type
    /// @param key the logical key
    /// @param virtualKey the Win32 virtual-key code
    /// @param lParam the message `lParam`
    /// @param text `ToUnicodeW` translation, or `null`
    /// @return the event
    private KeyEvent keyEvent(
            KeyEventType type,
            LogicalKey key,
            int virtualKey,
            long lParam,
            @Nullable String text
    ) {
        int scanCode = (int) ((lParam >>> 16) & 0xFFL);
        if (scanCode == 0) {
            scanCode = mapVirtualKeyToScan(virtualKey);
        }
        boolean repeat = type == KeyEventType.DOWN && (lParam & (1L << 30)) != 0L;
        boolean extended = (lParam & (1L << 24)) != 0L;
        boolean[] snapshot = keyboardSnapshot();
        return new KeyEvent(
                type,
                key,
                shiftDown || keyIsDown(VK_SHIFT) || snapshot[VK_SHIFT],
                ctrlDown || keyIsDown(VK_CONTROL) || snapshot[VK_CONTROL],
                altDown || keyIsDown(VK_MENU) || snapshot[VK_MENU],
                scanCode,
                repeat,
                extended,
                metaDown || keyIsDown(VK_LWIN) || keyIsDown(VK_RWIN) || snapshot[VK_LWIN] || snapshot[VK_RWIN],
                keyLocation(virtualKey, extended),
                messageTime(),
                text
        );
    }

    /// Returns whether generated `GetKeyState` reports `virtualKey` currently down.
    ///
    /// The high bit of the `SHORT` result is the down state. The call is the live modifier query
    /// used by [`#keyEvent(KeyEventType, LogicalKey, int, long)`].
    ///
    /// @param virtualKey a Win32 virtual-key code
    /// @return the raw `GetKeyState` result
    public short keyState(int virtualKey) {
        return bindings.getKeyState(virtualKey);
    }

    /// Copies generated `GetKeyboardState` into `state`.
    ///
    /// @param state a 256-byte destination
    /// @return whether `GetKeyboardState` succeeded
    public boolean copyKeyboardState(byte[] state) {
        Objects.requireNonNull(state, "state");
        if (state.length < 256) {
            throw new IllegalArgumentException("GetKeyboardState requires 256 bytes");
        }
        if (bindings.getKeyboardState(keyboardStateBuffer) == 0) {
            return false;
        }
        MemorySegment.copy(keyboardStateBuffer, ValueLayout.JAVA_BYTE, 0L, state, 0, 256);
        return true;
    }

    /// Returns down flags from generated `GetKeyboardState`.
    private boolean[] keyboardSnapshot() {
        boolean[] down = new boolean[256];
        byte[] state = new byte[256];
        if (!copyKeyboardState(state)) {
            return down;
        }
        for (int index = 0; index < 256; index++) {
            down[index] = (state[index] & 0x80) != 0;
        }
        return down;
    }

    /// Returns whether generated `GetAsyncKeyState` reports `virtualKey` currently down.
    ///
    /// @param virtualKey a Win32 virtual-key code
    /// @return the raw `GetAsyncKeyState` result
    public short asyncKeyState(int virtualKey) {
        return bindings.getAsyncKeyState(virtualKey);
    }

    /// Returns whether `GetKeyState` reports the high bit of `virtualKey`.
    private boolean keyIsDown(int virtualKey) {
        return (keyState(virtualKey) & 0x8000) != 0;
    }

    /// Returns whether `GetAsyncKeyState` reports the high bit of `virtualKey`.
    private boolean asyncKeyIsDown(int virtualKey) {
        return (asyncKeyState(virtualKey) & 0x8000) != 0;
    }

    /// Maps `virtualKey` to an OEM scan code through generated `MapVirtualKeyW`.
    private int mapVirtualKeyToScan(int virtualKey) {
        return bindings.mapVirtualKeyW(virtualKey, MAPVK_VK_TO_VSC) & 0xFF;
    }

    /// Translates `virtualKey` through generated `ToUnicodeW` without mutating dead-key state.
    ///
    /// @param virtualKey the virtual-key code
    /// @param scanCode the OEM scan code
    /// @return the translated string, or `null` when `ToUnicodeW` wrote no characters
    public @Nullable String translateVirtualKey(int virtualKey, int scanCode) {
        if (bindings.getKeyboardState(keyboardStateBuffer) == 0) {
            return null;
        }
        unicodeBuffer.fill((byte) 0);
        MemorySegment layout = bindings.getKeyboardLayout(0);
        lastKeyboardLayout = layout.address();
        int written = bindings.toUnicodeEx(
                virtualKey,
                scanCode,
                keyboardStateBuffer,
                unicodeBuffer,
                8,
                TO_UNICODE_NO_STATE_CHANGE,
                layout
        );
        if (written <= 0) {
            return null;
        }
        char[] units = new char[written];
        for (int index = 0; index < written; index++) {
            units[index] = (char) unicodeBuffer.get(ValueLayout.JAVA_SHORT, index * 2L);
        }
        String text = new String(units);
        if (isPrintableTranslation(text)) {
            translatedCharacters.append(text);
        }
        return text;
    }

    /// Returns whether `text` is safe to treat as committed character input.
    private static boolean isPrintableTranslation(String text) {
        for (int index = 0; index < text.length(); index++) {
            char unit = text.charAt(index);
            if (unit < 0x20 || unit == 0x7F) {
                return false;
            }
        }
        return !text.isEmpty();
    }

    /// Records generated `GetKeyNameTextW` for the current key-down `lParam` scan bits.
    private void rememberKeyName(int scanCode, boolean extended) {
        int packed = (scanCode & 0xFF) << 16;
        if (extended) {
            packed |= 1 << 24;
        }
        keyNameBuffer.fill((byte) 0);
        int length = bindings.getKeyNameTextW(packed, keyNameBuffer, 64);
        if (length <= 0) {
            lastKeyName = "";
            return;
        }
        char[] units = new char[length];
        for (int index = 0; index < length; index++) {
            units[index] = (char) keyNameBuffer.get(ValueLayout.JAVA_SHORT, index * 2L);
        }
        lastKeyName = new String(units);
    }

    /// Records generated `GetKeyboardLayout` for the current thread.
    private void rememberKeyboardLayout() {
        MemorySegment layout = bindings.getKeyboardLayout(0);
        lastKeyboardLayout = layout.address();
    }

    /// Maps `character` onto a virtual-key plus shift state through generated `VkKeyScanW`.
    ///
    /// @param character the UTF-16 code unit
    /// @return the raw `SHORT` result
    public short scanVirtualKey(char character) {
        lastCharVirtualKeyScan = bindings.vkKeyScanW((short) character);
        return lastCharVirtualKeyScan;
    }

    /// Returns the last `GetKeyNameTextW` string observed on key-down.
    ///
    /// @return the name, possibly empty
    public String lastKeyName() {
        return lastKeyName;
    }

    /// Returns the last `VkKeyScanW` result observed on `WM_CHAR`.
    ///
    /// @return the raw `SHORT` result
    public short lastCharVirtualKeyScan() {
        return lastCharVirtualKeyScan;
    }

    /// Returns the last `GetKeyboardLayout` handle address observed on key-down.
    ///
    /// @return the layout handle, or `0` before the first key-down
    public long lastKeyboardLayout() {
        return lastKeyboardLayout;
    }

    /// Returns the last `ImmGetCompositionStringW` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionStringBytes() {
        return lastCompositionStringBytes;
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_CURSORPOS` result.
    ///
    /// @return the cursor, or `Integer.MIN_VALUE` before a query
    public int lastCompositionCursor() {
        return lastCompositionCursor;
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_COMPATTR` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionAttrBytes() {
        return lastCompositionAttrBytes;
    }

    /// Returns the last `GCS_COMPATTR` bytes.
    ///
    /// @return the attributes, possibly empty
    public byte @Unmodifiable [] lastCompositionAttributes() {
        return lastCompositionAttributes;
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_COMPREADSTR` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionReadingBytes() {
        return lastCompositionReadingBytes;
    }

    /// Returns the last `ImmGetCompositionWindow` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastCompositionWindowResult() {
        return lastCompositionWindowResult;
    }

    /// Returns the last `ImmGetCandidateWindow` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastCandidateWindowResult() {
        return lastCandidateWindowResult;
    }

    /// Removes and returns `GCS_COMPREADSTR` text delivered through WndProc since the last drain.
    ///
    /// @return the reading string, possibly empty
    public String takeImeReading() {
        String text = imeReading.toString();
        imeReading.setLength(0);
        return text;
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_COMPCLAUSE` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionClauseBytes() {
        return lastCompositionClauseBytes;
    }

    /// Returns the last `GCS_COMPCLAUSE` offsets.
    ///
    /// @return the offsets, possibly empty
    public int @Unmodifiable [] lastCompositionClause() {
        return lastCompositionClause;
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_RESULTREADSTR` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastResultReadingBytes() {
        return lastResultReadingBytes;
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_RESULTCLAUSE` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastResultClauseBytes() {
        return lastResultClauseBytes;
    }

    /// Returns the last `GCS_RESULTCLAUSE` offsets.
    ///
    /// @return the offsets, possibly empty
    public int @Unmodifiable [] lastResultClause() {
        return lastResultClause;
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_DELTASTART` result.
    ///
    /// @return the offset, or `Integer.MIN_VALUE` before a query
    public int lastCompositionDeltaStart() {
        return lastCompositionDeltaStart;
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_COMPREADATTR` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionReadingAttrBytes() {
        return lastCompositionReadingAttrBytes;
    }

    /// Returns the last `GCS_COMPREADATTR` bytes.
    ///
    /// @return the attributes, possibly empty
    public byte @Unmodifiable [] lastCompositionReadingAttributes() {
        return lastCompositionReadingAttributes;
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_COMPREADCLAUSE` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionReadingClauseBytes() {
        return lastCompositionReadingClauseBytes;
    }

    /// Returns the last `GCS_COMPREADCLAUSE` offsets.
    ///
    /// @return the offsets, possibly empty
    public int @Unmodifiable [] lastCompositionReadingClause() {
        return lastCompositionReadingClause;
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_RESULTREADCLAUSE` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastResultReadingClauseBytes() {
        return lastResultReadingClauseBytes;
    }

    /// Returns the last `GCS_RESULTREADCLAUSE` offsets.
    ///
    /// @return the offsets, possibly empty
    public int @Unmodifiable [] lastResultReadingClause() {
        return lastResultReadingClause;
    }

    /// Returns the last `GetPointerFramePenInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFramePenHistoryEntries() {
        return lastPointerFramePenHistoryEntries;
    }

    /// Returns the last `GetPointerFrameTouchInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFrameTouchHistoryEntries() {
        return lastPointerFrameTouchHistoryEntries;
    }

    /// Removes and returns `GCS_RESULTREADSTR` text delivered through WndProc since the last drain.
    ///
    /// @return the result reading string, possibly empty
    public String takeImeResultReading() {
        String text = imeResultReading.toString();
        imeResultReading.setLength(0);
        return text;
    }

    /// Returns the last `ImmNotifyIME` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a notify
    public int lastImmNotifyResult() {
        return lastImmNotifyResult;
    }

    /// Returns whether `WM_IME_STARTCOMPOSITION` is unmatched.
    ///
    /// @return whether an IMM32 composition is active
    public boolean imeActive() {
        return imeActive;
    }

    /// Returns whether `TrackMouseEvent(TME_LEAVE)` is outstanding.
    ///
    /// @return whether leave tracking is armed
    public boolean mouseLeaveTracked() {
        return mouseLeaveTracked;
    }

    /// Returns whether the last generated `TrackMouseEvent` call succeeded.
    ///
    /// @return whether the host accepted leave tracking
    public boolean lastTrackMouseEventSucceeded() {
        return lastTrackMouseEventSucceeded;
    }

    /// Packs `POINTER_MOD_SHIFT` / `POINTER_MOD_CTRL` from generated `GetAsyncKeyState`.
    private int asyncModifierStates() {
        int states = 0;
        if (asyncKeyIsDown(VK_SHIFT)) {
            states |= POINTER_MOD_SHIFT;
        }
        if (asyncKeyIsDown(VK_CONTROL)) {
            states |= POINTER_MOD_CTRL;
        }
        return states;
    }

    /// Maps a virtual-key and `KF_EXTENDED` bit onto a physical location.
    ///
    /// @param virtualKey the `wParam` virtual-key code
    /// @param extended whether `KF_EXTENDED` was set
    /// @return the location
    private static KeyLocation keyLocation(int virtualKey, boolean extended) {
        if (virtualKey == VK_LWIN) {
            return KeyLocation.LEFT;
        }
        if (virtualKey == VK_RWIN) {
            return KeyLocation.RIGHT;
        }
        if (!extended && ((virtualKey >= 0x21 && virtualKey <= 0x28) || virtualKey == 0x2E)) {
            return KeyLocation.NUMPAD;
        }
        return KeyLocation.STANDARD;
    }

    /// Posts one message to this HWND so the production WndProc delivers it.
    ///
    /// @param message the Win32 message identifier
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    public void postMessage(int message, long wParam, long lParam) {
        requireOpen();
        Win32FfmBindings.PostMessageWResult result = bindings.postMessageW(window, message, wParam, lParam);
        if (result.value() == 0) {
            throw new IllegalStateException("PostMessageW failed: " + result.errorCode());
        }
    }

    /// Removes and returns pointer events delivered through WndProc since the last drain.
    ///
    /// @return the events in delivery order
    public @Unmodifiable List<PointerEvent> takePointerEvents() {
        List<PointerEvent> copy = List.copyOf(pointerEvents);
        pointerEvents.clear();
        return copy;
    }

    /// Removes and returns key events delivered through WndProc since the last drain.
    ///
    /// @return the events in delivery order
    public @Unmodifiable List<KeyEvent> takeKeyEvents() {
        List<KeyEvent> copy = List.copyOf(keyEvents);
        keyEvents.clear();
        return copy;
    }

    /// Removes and returns `WM_CHAR` / `WM_UNICHAR` text delivered through WndProc since the last drain.
    ///
    /// @return the committed characters, possibly empty
    public String takeCharacters() {
        String text = characters.toString();
        characters.setLength(0);
        return text;
    }

    /// Removes and returns `WM_DEADCHAR` text delivered through WndProc since the last drain.
    ///
    /// @return the dead-key characters, possibly empty
    public String takeDeadCharacters() {
        String text = deadCharacters.toString();
        deadCharacters.setLength(0);
        return text;
    }

    /// Removes and returns printable `ToUnicodeW` text delivered on key-down since the last drain.
    ///
    /// @return the translated characters, possibly empty
    public String takeTranslatedCharacters() {
        String text = translatedCharacters.toString();
        translatedCharacters.setLength(0);
        return text;
    }

    /// Removes and returns `GCS_COMPSTR` text delivered through WndProc since the last drain.
    ///
    /// @return the composition preview, possibly empty
    public String takeImeComposition() {
        String text = imeComposition.toString();
        imeComposition.setLength(0);
        return text;
    }

    /// Removes and returns `GCS_RESULTSTR` text delivered through WndProc since the last drain.
    ///
    /// @return the committed IMM32 result, possibly empty
    public String takeImeResult() {
        String text = imeResult.toString();
        imeResult.setLength(0);
        return text;
    }

    /// Removes and returns whether `WM_IME_ENDCOMPOSITION` arrived since the last drain.
    ///
    /// @return whether composition ended
    public boolean takeImeEnded() {
        boolean ended = imeEnded;
        imeEnded = false;
        return ended;
    }

    /// Reads `GCS_COMPSTR` / `GCS_RESULTSTR` through generated `ImmGetCompositionStringW`.
    private void readImeComposition(int gcs) {
        if ((gcs & GCS_COMPSTR) != 0 || gcs == 0) {
            String composition = compositionString(GCS_COMPSTR);
            if (!composition.isEmpty()) {
                imeComposition.append(composition);
            }
        }
        if ((gcs & GCS_RESULTSTR) != 0) {
            String result = compositionString(GCS_RESULTSTR);
            if (!result.isEmpty()) {
                imeResult.append(result);
            }
            notifyIme(CPS_COMPLETE);
        }
        if ((gcs & GCS_CURSORPOS) != 0 || (gcs & GCS_COMPSTR) != 0 || gcs == 0) {
            lastCompositionCursor = compositionCursor();
        }
        if ((gcs & GCS_COMPATTR) != 0 || (gcs & GCS_COMPSTR) != 0 || gcs == 0) {
            lastCompositionAttributes = compositionAttributes();
        }
        if ((gcs & GCS_COMPREADSTR) != 0 || (gcs & GCS_COMPSTR) != 0 || gcs == 0) {
            String reading = compositionString(GCS_COMPREADSTR);
            lastCompositionReadingBytes = lastCompositionStringBytes;
            if (!reading.isEmpty()) {
                imeReading.append(reading);
            }
        }
        if ((gcs & GCS_COMPCLAUSE) != 0 || (gcs & GCS_COMPSTR) != 0 || gcs == 0) {
            lastCompositionClause = compositionClause(GCS_COMPCLAUSE);
        }
        if ((gcs & GCS_RESULTREADSTR) != 0 || (gcs & GCS_RESULTSTR) != 0 || (gcs & GCS_COMPSTR) != 0 || gcs == 0) {
            String reading = compositionString(GCS_RESULTREADSTR);
            lastResultReadingBytes = lastCompositionStringBytes;
            if (!reading.isEmpty()) {
                imeResultReading.append(reading);
            }
        }
        if ((gcs & GCS_RESULTCLAUSE) != 0 || (gcs & GCS_RESULTSTR) != 0 || (gcs & GCS_COMPSTR) != 0 || gcs == 0) {
            lastResultClause = compositionClause(GCS_RESULTCLAUSE);
        }
        if ((gcs & GCS_DELTASTART) != 0 || (gcs & GCS_COMPSTR) != 0 || gcs == 0) {
            lastCompositionDeltaStart = compositionDeltaStart();
        }
        if ((gcs & GCS_COMPREADATTR) != 0 || (gcs & GCS_COMPSTR) != 0 || gcs == 0) {
            lastCompositionReadingAttributes = compositionReadingAttributes();
        }
        if ((gcs & GCS_COMPREADCLAUSE) != 0 || (gcs & GCS_COMPSTR) != 0 || gcs == 0) {
            lastCompositionReadingClause = compositionClause(GCS_COMPREADCLAUSE);
        }
        if ((gcs & GCS_RESULTREADCLAUSE) != 0 || (gcs & GCS_RESULTSTR) != 0 || (gcs & GCS_COMPSTR) != 0 || gcs == 0) {
            lastResultReadingClause = compositionClause(GCS_RESULTREADCLAUSE);
        }
    }

    /// Reads the current IMM32 candidate page through generated `ImmGetCandidateListW`.
    private void readCandidateList(int listIndex) {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastCandidateCount = 0;
            lastCandidateSelection = 0;
            lastCandidatePage = List.of();
            lastCandidateWindowResult = 0;
            return;
        }
        try {
            MemorySegment candidateForm = arena.allocate(Win32Layouts.CANDIDATEFORM);
            candidateForm.fill((byte) 0);
            lastCandidateWindowResult = bindings.immGetCandidateWindow(context, listIndex, candidateForm);
            MemorySegment listCount = arena.allocate(ValueLayout.JAVA_INT);
            lastCandidateListCount = bindings.immGetCandidateListCountW(context, listCount);
            int bytes = bindings.immGetCandidateListW(context, listIndex, MemorySegment.NULL, 0);
            if (bytes <= 0) {
                lastCandidateCount = 0;
                lastCandidateSelection = 0;
                lastCandidatePage = List.of();
                return;
            }
            MemorySegment buffer = arena.allocate(bytes);
            int written = bindings.immGetCandidateListW(context, listIndex, buffer, bytes);
            if (written <= 0) {
                lastCandidateCount = 0;
                lastCandidateSelection = 0;
                lastCandidatePage = List.of();
                return;
            }
            int count = buffer.get(ValueLayout.JAVA_INT, Win32Layouts.CANDIDATELIST_DW_COUNT_OFFSET);
            int selection = buffer.get(ValueLayout.JAVA_INT, Win32Layouts.CANDIDATELIST_DW_SELECTION_OFFSET);
            int pageStart = buffer.get(ValueLayout.JAVA_INT, Win32Layouts.CANDIDATELIST_DW_PAGE_START_OFFSET);
            int pageSize = buffer.get(ValueLayout.JAVA_INT, Win32Layouts.CANDIDATELIST_DW_PAGE_SIZE_OFFSET);
            lastCandidateCount = Math.max(0, count);
            lastCandidateSelection = Math.max(0, selection);
            if (pageSize <= 0 || pageStart < 0 || pageStart >= lastCandidateCount) {
                lastCandidatePage = List.of();
                return;
            }
            int pageEnd = Math.min(lastCandidateCount, pageStart + pageSize);
            ArrayList<String> page = new ArrayList<>(pageEnd - pageStart);
            for (int index = pageStart; index < pageEnd; index++) {
                int offset = buffer.get(ValueLayout.JAVA_INT, 24L + (long) index * 4L);
                if (offset <= 0 || offset >= written) {
                    page.add("");
                    continue;
                }
                page.add(buffer.getString(offset, StandardCharsets.UTF_16LE));
            }
            lastCandidatePage = List.copyOf(page);
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Returns the last `ImmGetCandidateListW` candidate count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastCandidateCount() {
        return lastCandidateCount;
    }

    /// Returns the last `ImmGetCandidateListW` selection index.
    ///
    /// @return the selection
    public int lastCandidateSelection() {
        return lastCandidateSelection;
    }

    /// Returns the last `ImmGetCandidateListW` page strings.
    ///
    /// @return the page, possibly empty
    public @Unmodifiable List<String> lastCandidatePage() {
        return lastCandidatePage;
    }

    /// Notifies the IMM32 composition through generated `ImmNotifyIME`.
    private void notifyIme(int command) {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastImmNotifyResult = 0;
            return;
        }
        try {
            lastImmNotifyResult = bindings.immNotifyIME(context, NI_COMPOSITIONSTR, command, 0);
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Reads conversion, open-status, and IME file name through generated IMM32 bindings.
    private void readImeStatus() {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastConversionStatus = 0;
            lastSentenceStatus = 0;
            lastImeOpenStatus = 0;
            lastImeFileName = "";
            return;
        }
        try {
            MemorySegment conversion = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment sentence = arena.allocate(ValueLayout.JAVA_INT);
            conversion.set(ValueLayout.JAVA_INT, 0L, 0);
            sentence.set(ValueLayout.JAVA_INT, 0L, 0);
            if (bindings.immGetConversionStatus(context, conversion, sentence) != 0) {
                lastConversionStatus = conversion.get(ValueLayout.JAVA_INT, 0L);
                lastSentenceStatus = sentence.get(ValueLayout.JAVA_INT, 0L);
            } else {
                lastConversionStatus = 0;
                lastSentenceStatus = 0;
            }
            lastImeOpenStatus = bindings.immGetOpenStatus(context);
        } finally {
            bindings.immReleaseContext(window, context);
        }
        MemorySegment layout = bindings.getKeyboardLayout(0);
        int units = bindings.immGetIMEFileNameW(layout, MemorySegment.NULL, 0);
        if (units <= 0) {
            lastImeFileName = "";
            return;
        }
        MemorySegment name = arena.allocate((long) (units + 1) * 2L);
        name.fill((byte) 0);
        bindings.immGetIMEFileNameW(layout, name, units + 1);
        lastImeFileName = name.getString(0L, StandardCharsets.UTF_16LE);
    }

    /// Reads the virtual key that started the composition through generated `ImmGetVirtualKey`.
    private void readImeVirtualKey() {
        lastImeVirtualKey = bindings.immGetVirtualKey(window);
    }

    /// Queries generated `ImmIsIME` for the current-thread keyboard layout.
    private void readImeIsIme() {
        MemorySegment layout = bindings.getKeyboardLayout(0);
        lastImeIsIme = bindings.immIsIME(layout);
    }

    /// Reads IME menu items through generated `ImmGetImeMenuItemsW`.
    private void readImeMenuItems() {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastImeMenuItemCount = 0;
            return;
        }
        try {
            lastImeMenuItemCount = bindings.immGetImeMenuItemsW(
                    context,
                    0,
                    IGIMII_MENU_TYPES,
                    MemorySegment.NULL,
                    MemorySegment.NULL,
                    0
            );
            if (lastImeMenuItemCount <= 0) {
                return;
            }
            int bytes = Math.toIntExact(Win32Layouts.IMEMENUITEMINFOW.byteSize() * (long) lastImeMenuItemCount);
            MemorySegment items = arena.allocate(bytes);
            items.fill((byte) 0);
            for (int index = 0; index < lastImeMenuItemCount; index++) {
                long offset = Win32Layouts.IMEMENUITEMINFOW.byteSize() * (long) index;
                items.set(
                        ValueLayout.JAVA_INT,
                        offset + Win32Layouts.IMEMENUITEMINFOW_CB_SIZE_OFFSET,
                        Math.toIntExact(Win32Layouts.IMEMENUITEMINFOW.byteSize())
                );
            }
            lastImeMenuItemCount = bindings.immGetImeMenuItemsW(
                    context,
                    0,
                    IGIMII_MENU_TYPES,
                    MemorySegment.NULL,
                    items,
                    bytes
            );
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Queries generated `ImmEscapeW` for IME name support and the IME name string.
    private void escapeIme() {
        MemorySegment layout = bindings.getKeyboardLayout(0);
        MemorySegment context = bindings.immGetContext(window);
        try {
            MemorySegment requested = arena.allocate(ValueLayout.JAVA_INT);
            requested.set(ValueLayout.JAVA_INT, 0L, IME_ESC_IME_NAME);
            lastImeEscapeResult = bindings.immEscapeW(layout, context, IME_ESC_QUERY_SUPPORT, requested);
            MemorySegment name = arena.allocate(128);
            name.fill((byte) 0);
            long named = bindings.immEscapeW(layout, context, IME_ESC_IME_NAME, name);
            if (named != 0L) {
                lastImeEscapeResult = named;
            }
        } finally {
            if (context.address() != 0L) {
                bindings.immReleaseContext(window, context);
            }
        }
    }

    /// Reads the IME description and property bits, and the default IME window.
    private void readImeDescription() {
        MemorySegment layout = bindings.getKeyboardLayout(0);
        lastImeProperty = bindings.immGetProperty(layout, IGP_PROPERTY);
        int units = bindings.immGetDescriptionW(layout, MemorySegment.NULL, 0);
        lastImeDescriptionChars = units;
        if (units <= 0) {
            lastImeDescription = "";
        } else {
            MemorySegment buffer = arena.allocate((long) (units + 1) * 2L);
            buffer.fill((byte) 0);
            lastImeDescriptionChars = bindings.immGetDescriptionW(layout, buffer, units + 1);
            lastImeDescription = buffer.getString(0L, StandardCharsets.UTF_16LE);
        }
        lastDefaultImeWnd = bindings.immGetDefaultIMEWnd(window).address();
    }

    /// Reads the status-window position through generated `ImmGetStatusWindowPos`.
    private void readImeStatusWindow() {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastStatusWindowPosResult = 0;
            return;
        }
        try {
            MemorySegment position = arena.allocate(Win32Layouts.POINT);
            position.fill((byte) 0);
            lastStatusWindowPosResult = bindings.immGetStatusWindowPos(context, position);
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Writes the status-window position through generated `ImmSetStatusWindowPos`.
    ///
    /// @param x the window x
    /// @param y the window y
    /// @return whether the host accepted the write
    public boolean setStatusWindowPos(int x, int y) {
        requireOpen();
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastSetStatusWindowPosResult = 0;
            return false;
        }
        try {
            MemorySegment position = arena.allocate(Win32Layouts.POINT);
            position.set(ValueLayout.JAVA_INT, Win32Layouts.POINT_X_OFFSET, x);
            position.set(ValueLayout.JAVA_INT, Win32Layouts.POINT_Y_OFFSET, y);
            lastSetStatusWindowPosResult = bindings.immSetStatusWindowPos(context, position);
            return lastSetStatusWindowPosResult != 0;
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Queries generated `ImmGetHotKey` for the IME/non-IME toggle.
    private void readImeHotKey() {
        MemorySegment modifiers = arena.allocate(ValueLayout.JAVA_INT);
        MemorySegment virtualKey = arena.allocate(ValueLayout.JAVA_INT);
        MemorySegment layout = arena.allocate(ValueLayout.ADDRESS);
        modifiers.set(ValueLayout.JAVA_INT, 0L, 0);
        virtualKey.set(ValueLayout.JAVA_INT, 0L, 0);
        layout.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        lastImeHotKeyResult = bindings.immGetHotKey(IME_CHOTKEY_IME_NONIME_TOGGLE, modifiers, virtualKey, layout);
        int modifierBits = modifiers.get(ValueLayout.JAVA_INT, 0L);
        int key = virtualKey.get(ValueLayout.JAVA_INT, 0L);
        MemorySegment hotkeyLayout = layout.get(ValueLayout.ADDRESS, 0L);
        if (lastImeHotKeyResult != 0 && key != 0) {
            lastSetHotKeyResult = bindings.immSetHotKey(
                    IME_CHOTKEY_IME_NONIME_TOGGLE,
                    modifierBits,
                    key,
                    hotkeyLayout
            );
        } else {
            lastSetHotKeyResult = bindings.immSetHotKey(
                    IME_ITHOTKEY_UISTYLE_TOGGLE,
                    0,
                    0,
                    bindings.getKeyboardLayout(0)
            );
        }
    }

    /// Forwards one IME notify through generated `ImmIsUIMessageW`.
    private void readImeUiMessage() {
        MemorySegment imeWindow = bindings.immGetDefaultIMEWnd(window);
        lastImeIsUiMessage = bindings.immIsUIMessageW(imeWindow, WM_IME_NOTIFY, IMN_GUIDELINE, 0L);
    }

    /// Reads register-word styles through generated `ImmGetRegisterWordStyleW`.
    private void readRegisterWordStyles() {
        MemorySegment layout = bindings.getKeyboardLayout(0);
        lastRegisterWordStyleCount = bindings.immGetRegisterWordStyleW(layout, 0, MemorySegment.NULL);
        if (lastRegisterWordStyleCount <= 0) {
            return;
        }
        int count = Math.min(lastRegisterWordStyleCount, 8);
        MemorySegment styles = arena.allocate(Win32Layouts.STYLEBUFW, count);
        styles.fill((byte) 0);
        lastRegisterWordStyleCount = bindings.immGetRegisterWordStyleW(layout, count, styles);
    }

    /// Enumerates this thread's input contexts through generated `ImmEnumInputContext`.
    private void enumerateImeContexts() {
        lastEnumInputContextCount = 0;
        try (Arena enumArena = Arena.ofConfined()) {
            MemorySegment stub = bindings.createImcEnumProcStub(this::onImeContext, callbackFailures, enumArena);
            lastEnumInputContextResult = bindings.immEnumInputContext(0, stub, 0L);
        }
    }

    /// Continues `ImmEnumInputContext` and counts each `HIMC`.
    ///
    /// @param context the enumerated context
    /// @param lParam the caller-supplied parameter, unused
    /// @return nonzero to continue
    private int onImeContext(MemorySegment context, long lParam) {
        lastEnumInputContextCount++;
        return 1;
    }

    /// Enumerates register words through generated `ImmEnumRegisterWordW`.
    private void enumerateRegisterWords() {
        lastEnumRegisterWordHits = 0;
        MemorySegment layout = bindings.getKeyboardLayout(0);
        try (Arena enumArena = Arena.ofConfined()) {
            MemorySegment stub = bindings.createRegisterWordEnumProcWStub(
                    this::onRegisterWord,
                    callbackFailures,
                    enumArena
            );
            lastEnumRegisterWordCount = bindings.immEnumRegisterWordW(
                    layout,
                    stub,
                    MemorySegment.NULL,
                    0,
                    MemorySegment.NULL,
                    MemorySegment.NULL
            );
        }
    }

    /// Continues `ImmEnumRegisterWordW` and counts each registered word.
    ///
    /// @param reading the reading string
    /// @param style the word style
    /// @param string the registered string
    /// @param data the caller-supplied parameter, unused
    /// @return nonzero to continue
    private int onRegisterWord(MemorySegment reading, int style, MemorySegment string, MemorySegment data) {
        lastEnumRegisterWordHits++;
        return 1;
    }

    /// Probes generated `ImmRegisterWordW` and `ImmUnregisterWordW` with null strings.
    ///
    /// Null reading and registered strings are rejected by the host and do not write the
    /// user dictionary.
    private void probeRegisterWord() {
        MemorySegment layout = bindings.getKeyboardLayout(0);
        lastRegisterWordResult = bindings.immRegisterWordW(
                layout,
                MemorySegment.NULL,
                0,
                MemorySegment.NULL
        );
        lastUnregisterWordResult = bindings.immUnregisterWordW(
                layout,
                MemorySegment.NULL,
                0,
                MemorySegment.NULL
        );
    }

    /// Sends `IMR_COMPOSITIONWINDOW` through generated `ImmRequestMessageW`.
    private void requestImeMessage() {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastImmRequestMessageResult = 0L;
            return;
        }
        try {
            MemorySegment form = arena.allocate(Win32Layouts.COMPOSITIONFORM);
            form.fill((byte) 0);
            lastImmRequestMessageResult = bindings.immRequestMessageW(
                    context,
                    Integer.toUnsignedLong(IMR_COMPOSITIONWINDOW),
                    form.address()
            );
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Queries generated `ImmSimulateHotKey` without toggling IME open/close state.
    private void simulateImeHotKey() {
        lastSimulateHotKeyResult = bindings.immSimulateHotKey(window, IME_ITHOTKEY_RESEND_RESULTSTR);
    }

    /// Reads conversion candidates through generated `ImmGetConversionListW`.
    ///
    /// @param source the source string, or empty to probe with a one-letter fallback
    private void readConversionList(String source) {
        MemorySegment layout = bindings.getKeyboardLayout(0);
        MemorySegment context = bindings.immGetContext(window);
        String text = source == null || source.isEmpty() ? "a" : source;
        MemorySegment encoded = arena.allocateFrom(text, StandardCharsets.UTF_16LE);
        try {
            lastConversionListBytes = bindings.immGetConversionListW(
                    layout,
                    context,
                    encoded,
                    MemorySegment.NULL,
                    0,
                    GCL_CONVERSION
            );
            lastConversionReverseLength = bindings.immGetConversionListW(
                    layout,
                    context,
                    encoded,
                    MemorySegment.NULL,
                    0,
                    GCL_REVERSE_LENGTH
            );
            int reverseBytes = lastConversionReverseLength > 0
                    ? lastConversionReverseLength
                    : Math.toIntExact(Win32Layouts.CANDIDATELIST.byteSize());
            MemorySegment reverse = arena.allocate(reverseBytes);
            reverse.fill((byte) 0);
            lastConversionReverseBytes = bindings.immGetConversionListW(
                    layout,
                    context,
                    encoded,
                    reverse,
                    reverseBytes,
                    GCL_REVERSECONVERSION
            );
            if (lastConversionReverseBytes > 0) {
                lastConversionReverseCount = Math.max(
                        0,
                        reverse.get(ValueLayout.JAVA_INT, Win32Layouts.CANDIDATELIST_DW_COUNT_OFFSET)
                );
            } else {
                lastConversionReverseCount = 0;
            }
            if (lastConversionListBytes <= 0) {
                lastConversionListCount = 0;
                return;
            }
            MemorySegment buffer = arena.allocate(lastConversionListBytes);
            buffer.fill((byte) 0);
            int written = bindings.immGetConversionListW(
                    layout,
                    context,
                    encoded,
                    buffer,
                    lastConversionListBytes,
                    GCL_CONVERSION
            );
            lastConversionListBytes = written;
            if (written <= 0) {
                lastConversionListCount = 0;
                return;
            }
            lastConversionListCount = Math.max(
                    0,
                    buffer.get(ValueLayout.JAVA_INT, Win32Layouts.CANDIDATELIST_DW_COUNT_OFFSET)
            );
        } finally {
            if (context.address() != 0L) {
                bindings.immReleaseContext(window, context);
            }
        }
    }

    /// Creates an IMM32 context and associates it with this HWND.
    private void associateImeContext() {
        createdImeContext = bindings.immCreateContext();
        lastCreateContext = createdImeContext.address();
        lastAssociateContext = bindings.immAssociateContext(window, createdImeContext).address();
        lastAssociateContextExResult = bindings.immAssociateContextEx(window, MemorySegment.NULL, IACE_DEFAULT);
    }

    /// Dissociates and destroys the IMM32 context created for this HWND.
    private void destroyImeContext() {
        if (window.address() != 0L) {
            bindings.immAssociateContext(window, MemorySegment.NULL);
        }
        if (createdImeContext.address() != 0L) {
            lastDestroyContextResult = bindings.immDestroyContext(createdImeContext);
            createdImeContext = MemorySegment.NULL;
        }
    }

    /// Publishes editor document, reconversion, and candidate geometry used by `WM_IME_REQUEST`.
    ///
    /// @param document the surrounding document text
    /// @param reconvert the last committed fragment, possibly empty
    /// @param candidateX the candidate-window x
    /// @param candidateY the candidate-window y
    /// @param candidateWidth the candidate-window width
    /// @param candidateHeight the candidate-window height
    public void publishImeDocument(
            String document,
            String reconvert,
            float candidateX,
            float candidateY,
            float candidateWidth,
            float candidateHeight
    ) {
        this.imeDocument = Objects.requireNonNull(document, "document");
        this.imeReconvert = Objects.requireNonNull(reconvert, "reconvert");
        this.imeCandidateX = Math.round(candidateX);
        this.imeCandidateY = Math.round(candidateY);
        this.imeCandidateWidth = Math.round(candidateWidth);
        this.imeCandidateHeight = Math.round(candidateHeight);
    }

    /// Returns the last `WM_IME_REQUEST` command.
    ///
    /// @return the `IMR_*` command, or `Integer.MIN_VALUE` before a request
    public int lastImeRequest() {
        return lastImeRequest;
    }

    /// Returns the last `WM_IME_REQUEST` byte count returned to the host.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a request
    public int lastImeRequestBytes() {
        return lastImeRequestBytes;
    }

    /// Returns the last `IMECHARPOSITION.dwCharPos`.
    ///
    /// @return the character offset, or `Integer.MIN_VALUE` before a query
    public int lastImeCharPos() {
        return lastImeCharPos;
    }

    /// Returns the previous `HIMC` from `ImmAssociateContext`.
    ///
    /// @return the handle address, or `-1` before a call
    public long lastAssociateContext() {
        return lastAssociateContext;
    }

    /// Returns the last `ImmAssociateContextEx` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a call
    public int lastAssociateContextExResult() {
        return lastAssociateContextExResult;
    }

    /// Returns the last `ImmIsIME` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastImeIsIme() {
        return lastImeIsIme;
    }

    /// Returns the last `ImmGetImeMenuItemsW` item count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastImeMenuItemCount() {
        return lastImeMenuItemCount;
    }

    /// Returns the last `ImmEscapeW` result.
    ///
    /// @return the `LRESULT`, or `Long.MIN_VALUE` before a query
    public long lastImeEscapeResult() {
        return lastImeEscapeResult;
    }

    /// Returns the last `ImmGetDescriptionW` character count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastImeDescriptionChars() {
        return lastImeDescriptionChars;
    }

    /// Returns the last `ImmGetDescriptionW` string.
    ///
    /// @return the description, possibly empty
    public String lastImeDescription() {
        return lastImeDescription;
    }

    /// Returns the last `ImmGetProperty` bits.
    ///
    /// @return the bits, or `Integer.MIN_VALUE` before a query
    public int lastImeProperty() {
        return lastImeProperty;
    }

    /// Returns the last `ImmGetDefaultIMEWnd` handle address.
    ///
    /// @return the handle, or `-1` before a query
    public long lastDefaultImeWnd() {
        return lastDefaultImeWnd;
    }

    /// Returns the last `ImmGetStatusWindowPos` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastStatusWindowPosResult() {
        return lastStatusWindowPosResult;
    }

    /// Returns the last `ImmSetStatusWindowPos` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a write
    public int lastSetStatusWindowPosResult() {
        return lastSetStatusWindowPosResult;
    }

    /// Returns the last `ImmGetHotKey` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastImeHotKeyResult() {
        return lastImeHotKeyResult;
    }

    /// Returns the last `ImmSetHotKey` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a write
    public int lastSetHotKeyResult() {
        return lastSetHotKeyResult;
    }

    /// Returns the last `ImmGetConversionListW` `GCL_REVERSECONVERSION` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastConversionReverseBytes() {
        return lastConversionReverseBytes;
    }

    /// Returns the last `ImmGetConversionListW` `GCL_REVERSECONVERSION` candidate count.
    ///
    /// @return the count
    public int lastConversionReverseCount() {
        return lastConversionReverseCount;
    }

    /// Returns the last `ImmIsUIMessageW` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastImeIsUiMessage() {
        return lastImeIsUiMessage;
    }

    /// Returns the last `ImmGetRegisterWordStyleW` style count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastRegisterWordStyleCount() {
        return lastRegisterWordStyleCount;
    }

    /// Returns the last `ImmEnumInputContext` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastEnumInputContextResult() {
        return lastEnumInputContextResult;
    }

    /// Returns the number of contexts delivered to the last `IMCENUMPROC`.
    ///
    /// @return the count
    public int lastEnumInputContextCount() {
        return lastEnumInputContextCount;
    }

    /// Returns the last `ImmEnumRegisterWordW` count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastEnumRegisterWordCount() {
        return lastEnumRegisterWordCount;
    }

    /// Returns the number of words delivered to the last `REGISTERWORDENUMPROCW`.
    ///
    /// @return the count
    public int lastEnumRegisterWordHits() {
        return lastEnumRegisterWordHits;
    }

    /// Returns the last `ImmRequestMessageW` result.
    ///
    /// @return the `LRESULT`, or `Long.MIN_VALUE` before a query
    public long lastImmRequestMessageResult() {
        return lastImmRequestMessageResult;
    }

    /// Returns the last `ImmRegisterWordW` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastRegisterWordResult() {
        return lastRegisterWordResult;
    }

    /// Returns the last `ImmUnregisterWordW` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastUnregisterWordResult() {
        return lastUnregisterWordResult;
    }

    /// Returns the last `CountClipboardFormats` result.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastClipboardFormatCount() {
        return lastClipboardFormatCount;
    }

    /// Returns the last `GetClipboardSequenceNumber` result.
    ///
    /// @return the sequence, or `Integer.MIN_VALUE` before a query
    public int lastClipboardSequence() {
        return lastClipboardSequence;
    }

    /// Returns the last `GetClipboardOwner` handle address.
    ///
    /// @return the handle address, or `-1` before a query
    public long lastClipboardOwner() {
        return lastClipboardOwner;
    }

    /// Returns the last `GetOpenClipboardWindow` handle address.
    ///
    /// @return the handle address, or `-1` before a query
    public long lastOpenClipboardWindow() {
        return lastOpenClipboardWindow;
    }

    /// Returns the last `IsClipboardFormatAvailable(CF_UNICODETEXT)` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastClipboardUnicodeAvailable() {
        return lastClipboardUnicodeAvailable;
    }

    /// Returns the last `GetPriorityClipboardFormat` result.
    ///
    /// @return the format, `0`, or `-1`, or `Integer.MIN_VALUE` before a query
    public int lastPriorityClipboardFormat() {
        return lastPriorityClipboardFormat;
    }

    /// Returns the first format from `EnumClipboardFormats(0)`.
    ///
    /// @return the format, `0` when the clipboard is empty, or `Integer.MIN_VALUE` before a query
    public int lastEnumClipboardFormat() {
        return lastEnumClipboardFormat;
    }

    /// Returns the number of formats walked by `EnumClipboardFormats`.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastEnumClipboardFormatCount() {
        return lastEnumClipboardFormatCount;
    }

    /// Returns the last `GetClipboardFormatNameW` character count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastClipboardFormatNameChars() {
        return lastClipboardFormatNameChars;
    }

    /// Returns the last `GetUpdatedClipboardFormats` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastUpdatedClipboardFormatsResult() {
        return lastUpdatedClipboardFormatsResult;
    }

    /// Returns the last `GetUpdatedClipboardFormats` reported format count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastUpdatedClipboardFormatCount() {
        return lastUpdatedClipboardFormatCount;
    }

    /// Returns the last `AddClipboardFormatListener` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastAddClipboardFormatListenerResult() {
        return lastAddClipboardFormatListenerResult;
    }

    /// Returns the last `RemoveClipboardFormatListener` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastRemoveClipboardFormatListenerResult() {
        return lastRemoveClipboardFormatListenerResult;
    }

    /// Returns the number of `WM_CLIPBOARDUPDATE` deliveries.
    ///
    /// @return the count
    public int lastClipboardUpdateCount() {
        return lastClipboardUpdateCount;
    }

    /// Returns the last `ImmGetConversionListW` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastConversionListBytes() {
        return lastConversionListBytes;
    }

    /// Returns the last `ImmGetConversionListW` `GCL_REVERSE_LENGTH` result.
    ///
    /// @return the length, or `Integer.MIN_VALUE` before a query
    public int lastConversionReverseLength() {
        return lastConversionReverseLength;
    }

    /// Returns the last `ImmGetConversionListW` candidate count.
    ///
    /// @return the count
    public int lastConversionListCount() {
        return lastConversionListCount;
    }

    /// Returns the last `ImmSimulateHotKey` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a call
    public int lastSimulateHotKeyResult() {
        return lastSimulateHotKeyResult;
    }

    /// Returns the last `WM_IME_CONTROL` command.
    ///
    /// @return the `IMC_*` command, or `Integer.MIN_VALUE` before a delivery
    public int lastImeControl() {
        return lastImeControl;
    }

    /// Returns the `HIMC` created by `ImmCreateContext`.
    ///
    /// @return the handle address, or `-1` before a call
    public long lastCreateContext() {
        return lastCreateContext;
    }

    /// Returns the last `ImmDestroyContext` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a call
    public int lastDestroyContextResult() {
        return lastDestroyContextResult;
    }

    /// Returns the last `WM_IME_SETCONTEXT` `wParam`.
    ///
    /// @return the activation flag, or `Integer.MIN_VALUE` before a delivery
    public int lastImeSetContext() {
        return lastImeSetContext;
    }

    /// Returns the last `WM_IME_SELECT` `wParam`.
    ///
    /// @return the selection flag, or `Integer.MIN_VALUE` before a delivery
    public int lastImeSelect() {
        return lastImeSelect;
    }

    /// Fills `RECONVERTSTRING` or `IMECHARPOSITION` for one `WM_IME_REQUEST`.
    ///
    /// @param command the `IMR_*` command
    /// @param lParam the host buffer address, or `0` when the host is probing size
    /// @return the required or written byte count
    private long handleImeRequest(int command, long lParam) {
        if (command == IMR_RECONVERTSTRING || command == IMR_CONFIRMRECONVERTSTRING || command == IMR_DOCUMENTFEED) {
            String text = command == IMR_DOCUMENTFEED ? imeDocument : imeReconvert;
            if (text.isEmpty() && command != IMR_DOCUMENTFEED) {
                text = imeDocument;
            }
            int units = text.length();
            int bytes = Math.toIntExact(Win32Layouts.RECONVERTSTRING.byteSize()) + units * 2 + 2;
            lastImeRequestBytes = bytes;
            if (lParam == 0L) {
                return bytes;
            }
            MemorySegment dest = MemorySegment.ofAddress(lParam).reinterpret(bytes);
            dest.fill((byte) 0);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.RECONVERTSTRING_SIZE_OFFSET, bytes);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.RECONVERTSTRING_VERSION_OFFSET, 0);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.RECONVERTSTRING_STR_LEN_OFFSET, units);
            dest.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.RECONVERTSTRING_STR_OFFSET_OFFSET,
                    Math.toIntExact(Win32Layouts.RECONVERTSTRING.byteSize())
            );
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.RECONVERTSTRING_COMP_STR_LEN_OFFSET, units);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.RECONVERTSTRING_COMP_STR_OFFSET_OFFSET, 0);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.RECONVERTSTRING_TARGET_STR_LEN_OFFSET, units);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.RECONVERTSTRING_TARGET_STR_OFFSET_OFFSET, 0);
            if (units > 0) {
                MemorySegment encoded = arena.allocateFrom(text, StandardCharsets.UTF_16LE);
                MemorySegment.copy(encoded, 0L, dest, Win32Layouts.RECONVERTSTRING.byteSize(), (long) units * 2L);
            }
            return bytes;
        }
        if (command == IMR_QUERYCHARPOSITION) {
            int bytes = Math.toIntExact(Win32Layouts.IMECHARPOSITION.byteSize());
            lastImeRequestBytes = bytes;
            if (lParam == 0L) {
                return bytes;
            }
            MemorySegment dest = MemorySegment.ofAddress(lParam).reinterpret(bytes);
            lastImeCharPos = dest.get(ValueLayout.JAVA_INT, Win32Layouts.IMECHARPOSITION_CHAR_POS_OFFSET);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.IMECHARPOSITION_SIZE_OFFSET, bytes);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.POINT_X_OFFSET + Win32Layouts.IMECHARPOSITION_PT_OFFSET, imeCandidateX);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.POINT_Y_OFFSET + Win32Layouts.IMECHARPOSITION_PT_OFFSET, imeCandidateY);
            dest.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.IMECHARPOSITION_LINE_HEIGHT_OFFSET,
                    Math.max(1, imeCandidateHeight)
            );
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_LEFT_OFFSET + Win32Layouts.IMECHARPOSITION_DOCUMENT_OFFSET, imeCandidateX);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_TOP_OFFSET + Win32Layouts.IMECHARPOSITION_DOCUMENT_OFFSET, imeCandidateY);
            dest.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.RECT_RIGHT_OFFSET + Win32Layouts.IMECHARPOSITION_DOCUMENT_OFFSET,
                    imeCandidateX + Math.max(0, imeCandidateWidth)
            );
            dest.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.RECT_BOTTOM_OFFSET + Win32Layouts.IMECHARPOSITION_DOCUMENT_OFFSET,
                    imeCandidateY + Math.max(0, imeCandidateHeight)
            );
            return bytes;
        }
        if (command == IMR_COMPOSITIONWINDOW) {
            int bytes = Math.toIntExact(Win32Layouts.COMPOSITIONFORM.byteSize());
            lastImeRequestBytes = bytes;
            if (lParam == 0L) {
                return bytes;
            }
            MemorySegment dest = MemorySegment.ofAddress(lParam).reinterpret(bytes);
            dest.fill((byte) 0);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.COMPOSITIONFORM_STYLE_OFFSET, 0x0020);
            dest.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.COMPOSITIONFORM_CURRENT_POS_OFFSET + Win32Layouts.POINT_X_OFFSET,
                    imeCandidateX
            );
            dest.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.COMPOSITIONFORM_CURRENT_POS_OFFSET + Win32Layouts.POINT_Y_OFFSET,
                    imeCandidateY
            );
            return bytes;
        }
        if (command == IMR_CANDIDATEWINDOW) {
            int bytes = Math.toIntExact(Win32Layouts.CANDIDATEFORM.byteSize());
            lastImeRequestBytes = bytes;
            if (lParam == 0L) {
                return bytes;
            }
            MemorySegment dest = MemorySegment.ofAddress(lParam).reinterpret(bytes);
            dest.fill((byte) 0);
            dest.set(ValueLayout.JAVA_INT, Win32Layouts.CANDIDATEFORM_STYLE_OFFSET, 0x0040);
            dest.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.CANDIDATEFORM_CURRENT_POS_OFFSET + Win32Layouts.POINT_X_OFFSET,
                    imeCandidateX
            );
            dest.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.CANDIDATEFORM_CURRENT_POS_OFFSET + Win32Layouts.POINT_Y_OFFSET,
                    imeCandidateY
            );
            return bytes;
        }
        if (command == IMR_COMPOSITIONFONT) {
            int bytes = Math.toIntExact(Win32Layouts.LOGFONTW.byteSize());
            lastImeRequestBytes = bytes;
            if (lParam == 0L) {
                return bytes;
            }
            MemorySegment dest = MemorySegment.ofAddress(lParam).reinterpret(bytes);
            dest.fill((byte) 0);
            dest.setString(28L, lastCompositionFontFace, StandardCharsets.UTF_16LE);
            return bytes;
        }
        lastImeRequestBytes = 0;
        return 0L;
    }

    /// Returns the last `ImmGetVirtualKey` result.
    ///
    /// @return the virtual key, or `Integer.MIN_VALUE` before a query
    public int lastImeVirtualKey() {
        return lastImeVirtualKey;
    }

    /// Reads the composition window through generated `ImmGetCompositionWindow`.
    private void readCompositionWindow() {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastCompositionWindowResult = 0;
            return;
        }
        try {
            MemorySegment form = arena.allocate(Win32Layouts.COMPOSITIONFORM);
            form.fill((byte) 0);
            lastCompositionWindowResult = bindings.immGetCompositionWindow(context, form);
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Reads the IME composition face through generated `ImmGetCompositionFontW`.
    private void readCompositionFont() {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastCompositionFontResult = 0;
            lastCompositionFontFace = "";
            return;
        }
        try {
            MemorySegment font = arena.allocate(Win32Layouts.LOGFONTW);
            font.fill((byte) 0);
            lastCompositionFontResult = bindings.immGetCompositionFontW(context, font);
            if (lastCompositionFontResult == 0) {
                lastCompositionFontFace = "";
                return;
            }
            lastCompositionFontFace = font.getString(28L, StandardCharsets.UTF_16LE);
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Returns the last `ImmGetCompositionFontW` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastCompositionFontResult() {
        return lastCompositionFontResult;
    }

    /// Returns the last `ImmGetCompositionFontW` face name.
    ///
    /// @return the face, possibly empty
    public String lastCompositionFontFace() {
        return lastCompositionFontFace;
    }

    /// Queries generated `GetPointerCursorId`.
    ///
    /// @param pointerId the pointer identity
    /// @return the cursor id, or `0` when the host has no mapping
    public int queryPointerCursorId(int pointerId) {
        requireOpen();
        MemorySegment cursorId = arena.allocate(ValueLayout.JAVA_INT);
        cursorId.set(ValueLayout.JAVA_INT, 0L, 0);
        Win32FfmBindings.GetPointerCursorIdResult result = bindings.getPointerCursorId(pointerId, cursorId);
        if (result.value() == 0) {
            lastPointerCursorId = 0;
            return 0;
        }
        lastPointerCursorId = Math.max(0, cursorId.get(ValueLayout.JAVA_INT, 0L));
        return lastPointerCursorId;
    }

    /// Queries generated `GetPointerDevice`.
    ///
    /// @param pointerId the pointer identity
    /// @return the device handle address, or `0` when the host has no mapping
    public long queryPointerDevice(int pointerId) {
        requireOpen();
        MemorySegment device = arena.allocate(ValueLayout.ADDRESS);
        device.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        Win32FfmBindings.GetPointerDeviceResult result = bindings.getPointerDevice(pointerId, device);
        if (result.value() == 0) {
            lastPointerDevice = 0L;
            return 0L;
        }
        lastPointerDevice = device.get(ValueLayout.ADDRESS, 0L).address();
        return lastPointerDevice;
    }

    /// Returns the last `GetPointerCursorId` value.
    ///
    /// @return the cursor id, or `Integer.MIN_VALUE` before a query
    public int lastPointerCursorId() {
        return lastPointerCursorId;
    }

    /// Returns the last `GetPointerDevice` handle address.
    ///
    /// @return the handle, or `-1` before a query
    public long lastPointerDevice() {
        return lastPointerDevice;
    }

    /// Queries generated `GetPointerDeviceRects` for `device`.
    ///
    /// @param device the pointer-device handle address
    /// @return whether the host filled both rectangles
    public boolean queryPointerDeviceRects(long device) {
        requireOpen();
        MemorySegment handle = MemorySegment.ofAddress(device);
        MemorySegment pointerRect = arena.allocate(Win32Layouts.RECT);
        MemorySegment displayRect = arena.allocate(Win32Layouts.RECT);
        Win32FfmBindings.GetPointerDeviceRectsResult result = bindings.getPointerDeviceRects(
                handle,
                pointerRect,
                displayRect
        );
        lastPointerDeviceRectsResult = result.value();
        return result.value() != 0;
    }

    /// Queries generated `GetPointerDeviceProperties` for `device`.
    ///
    /// @param device the pointer-device handle address
    /// @return the property count, or `0` when the host has none
    public int queryPointerDeviceProperties(long device) {
        requireOpen();
        MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
        count.set(ValueLayout.JAVA_INT, 0L, 0);
        MemorySegment handle = MemorySegment.ofAddress(device);
        Win32FfmBindings.GetPointerDevicePropertiesResult probe = bindings.getPointerDeviceProperties(
                handle,
                count,
                MemorySegment.NULL
        );
        int required = Math.max(0, count.get(ValueLayout.JAVA_INT, 0L));
        if (probe.value() != 0 && required == 0) {
            lastPointerDevicePropertyCount = 0;
            return 0;
        }
        if (required <= 0) {
            lastPointerDevicePropertyCount = 0;
            return 0;
        }
        MemorySegment properties = arena.allocate(Win32Layouts.POINTER_DEVICE_PROPERTY, required);
        count.set(ValueLayout.JAVA_INT, 0L, required);
        Win32FfmBindings.GetPointerDevicePropertiesResult filled = bindings.getPointerDeviceProperties(
                handle,
                count,
                properties
        );
        if (filled.value() == 0) {
            lastPointerDevicePropertyCount = 0;
            return 0;
        }
        lastPointerDevicePropertyCount = Math.max(0, count.get(ValueLayout.JAVA_INT, 0L));
        return lastPointerDevicePropertyCount;
    }

    /// Returns the last `GetPointerDeviceProperties` property count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerDevicePropertyCount() {
        return lastPointerDevicePropertyCount;
    }

    /// Queries generated `GetPointerDevices` and records the attached device count.
    ///
    /// @return the device count, or `0` when the host reports none
    public int queryPointerDevices() {
        requireOpen();
        MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
        count.set(ValueLayout.JAVA_INT, 0L, 0);
        Win32FfmBindings.GetPointerDevicesResult result = bindings.getPointerDevices(count, MemorySegment.NULL);
        lastPointerDeviceCount = Math.max(0, count.get(ValueLayout.JAVA_INT, 0L));
        if (result.value() == 0 && lastPointerDeviceCount == 0) {
            return 0;
        }
        return lastPointerDeviceCount;
    }

    /// Queries generated `GetPointerDeviceCursors` for `device`.
    ///
    /// @param device the pointer-device handle address
    /// @return the cursor count, or `0` when the host has none
    public int queryPointerDeviceCursors(long device) {
        requireOpen();
        MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
        count.set(ValueLayout.JAVA_INT, 0L, 0);
        Win32FfmBindings.GetPointerDeviceCursorsResult result = bindings.getPointerDeviceCursors(
                MemorySegment.ofAddress(device),
                count,
                MemorySegment.NULL
        );
        lastPointerDeviceCursorCount = Math.max(0, count.get(ValueLayout.JAVA_INT, 0L));
        if (result.value() == 0 && lastPointerDeviceCursorCount == 0) {
            return 0;
        }
        return lastPointerDeviceCursorCount;
    }

    /// Returns the last `GetPointerDevices` device count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerDeviceCount() {
        return lastPointerDeviceCount;
    }

    /// Returns the last `GetPointerDeviceCursors` cursor count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerDeviceCursorCount() {
        return lastPointerDeviceCursorCount;
    }

    /// Writes an IMM32 composition face through generated `ImmSetCompositionFontW`.
    ///
    /// @param face the LOGFONT face name
    /// @return whether the host accepted the write
    public boolean setCompositionFontFace(String face) {
        Objects.requireNonNull(face, "face");
        requireOpen();
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastSetCompositionFontResult = 0;
            return false;
        }
        try {
            MemorySegment font = arena.allocate(Win32Layouts.LOGFONTW);
            font.fill((byte) 0);
            bindings.immGetCompositionFontW(context, font);
            writeLogFontFace(font, face);
            lastSetCompositionFontResult = bindings.immSetCompositionFontW(context, font);
            if (lastSetCompositionFontResult != 0) {
                lastCompositionFontResult = bindings.immGetCompositionFontW(context, font);
                lastCompositionFontFace = font.getString(28L, StandardCharsets.UTF_16LE);
            }
            return lastSetCompositionFontResult != 0;
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Writes `face` into `LOGFONTW.lfFaceName`.
    private static void writeLogFontFace(MemorySegment font, String face) {
        int units = Math.min(face.length(), 31);
        for (int index = 0; index < units; index++) {
            font.set(ValueLayout.JAVA_SHORT, 28L + (long) index * 2L, (short) face.charAt(index));
        }
        font.set(ValueLayout.JAVA_SHORT, 28L + (long) units * 2L, (short) 0);
    }

    /// Returns the last `ImmSetCompositionFontW` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a write
    public int lastSetCompositionFontResult() {
        return lastSetCompositionFontResult;
    }

    /// Skips remaining coalesced pointer-frame messages through generated `SkipPointerFrameMessages`.
    ///
    /// @param pointerId the pointer identity
    /// @return whether the host accepted the skip
    public boolean skipPointerFrame(int pointerId) {
        requireOpen();
        Win32FfmBindings.SkipPointerFrameMessagesResult skipped = bindings.skipPointerFrameMessages(pointerId);
        lastSkipPointerFrameResult = skipped.value();
        return skipped.value() != 0;
    }

    /// Enables `WM_POINTER` for mouse through generated `EnableMouseInPointer`.
    private void enableMouseInPointer() {
        bindings.enableMouseInPointer(1);
        lastMouseInPointerEnabled = bindings.isMouseInPointerEnabled();
    }

    /// Writes IMM32 conversion bits through generated `ImmSetConversionStatus`.
    ///
    /// @param conversion the conversion flags
    /// @param sentence the sentence flags
    /// @return whether the host accepted the write
    public boolean setConversionStatus(int conversion, int sentence) {
        requireOpen();
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastSetConversionStatusResult = 0;
            return false;
        }
        try {
            lastSetConversionStatusResult = bindings.immSetConversionStatus(context, conversion, sentence);
            return lastSetConversionStatusResult != 0;
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Writes IMM32 open status through generated `ImmSetOpenStatus`.
    ///
    /// @param open whether the IME should be open
    /// @return whether the host accepted the write
    public boolean setOpenStatus(boolean open) {
        requireOpen();
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastSetOpenStatusResult = 0;
            return false;
        }
        try {
            lastSetOpenStatusResult = bindings.immSetOpenStatus(context, open ? 1 : 0);
            return lastSetOpenStatusResult != 0;
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Returns the last `GetPointerDeviceRects` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastPointerDeviceRectsResult() {
        return lastPointerDeviceRectsResult;
    }

    /// Returns the last `IsMouseInPointerEnabled` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastMouseInPointerEnabled() {
        return lastMouseInPointerEnabled;
    }

    /// Returns the last `SkipPointerFrameMessages` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastSkipPointerFrameResult() {
        return lastSkipPointerFrameResult;
    }

    /// Returns the last `ImmSetConversionStatus` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a write
    public int lastSetConversionStatusResult() {
        return lastSetConversionStatusResult;
    }

    /// Returns the last `ImmSetOpenStatus` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a write
    public int lastSetOpenStatusResult() {
        return lastSetOpenStatusResult;
    }

    /// Returns the last `GetRawInputDeviceInfoW` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastRawInputDeviceInfoBytes() {
        return lastRawInputDeviceInfoBytes;
    }

    /// Returns the last `GetRawInputDeviceList` device count.
    ///
    /// @return the device count, or `Integer.MIN_VALUE` before a query
    public int lastRawInputDeviceListCount() {
        return lastRawInputDeviceListCount;
    }

    /// Returns the last `WM_INPUT_DEVICE_CHANGE` `wParam`.
    ///
    /// @return the change kind, or `Integer.MIN_VALUE` before a delivery
    public int lastInputDeviceChange() {
        return lastInputDeviceChange;
    }

    /// Returns the last `ImmGetConversionStatus` conversion bits.
    ///
    /// @return the bits, or `Integer.MIN_VALUE` before a query
    public int lastConversionStatus() {
        return lastConversionStatus;
    }

    /// Returns the last `ImmGetConversionStatus` sentence bits.
    ///
    /// @return the bits
    public int lastSentenceStatus() {
        return lastSentenceStatus;
    }

    /// Returns the last `ImmGetOpenStatus` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastImeOpenStatus() {
        return lastImeOpenStatus;
    }

    /// Returns the last `ImmGetIMEFileNameW` path.
    ///
    /// @return the path, possibly empty
    public String lastImeFileName() {
        return lastImeFileName;
    }

    /// Reads `GGL_STRING` through generated `ImmGetGuideLineW`.
    private void readGuideLine() {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastGuideLineBytes = 0;
            return;
        }
        try {
            int bytes = bindings.immGetGuideLineW(context, GGL_STRING, MemorySegment.NULL, 0);
            lastGuideLineBytes = bytes;
            if (bytes <= 0) {
                return;
            }
            MemorySegment buffer = arena.allocate(bytes + 2);
            buffer.fill((byte) 0);
            int written = bindings.immGetGuideLineW(context, GGL_STRING, buffer, bytes);
            lastGuideLineBytes = written;
            if (written <= 0) {
                return;
            }
            String text = buffer.getString(0L, StandardCharsets.UTF_16LE);
            if (!text.isEmpty()) {
                guideline.append(text);
            }
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Reads `GCS_CURSORPOS` through generated `ImmGetCompositionStringW`.
    ///
    /// @return the character offset, or a negative IMM32 error
    public int compositionCursor() {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastCompositionCursor = Integer.MIN_VALUE;
            return Integer.MIN_VALUE;
        }
        try {
            int cursor = bindings.immGetCompositionStringW(context, GCS_CURSORPOS, MemorySegment.NULL, 0);
            lastCompositionCursor = cursor;
            return cursor;
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Reads `GCS_DELTASTART` through generated `ImmGetCompositionStringW`.
    ///
    /// @return the character offset, or a negative IMM32 error
    public int compositionDeltaStart() {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastCompositionDeltaStart = Integer.MIN_VALUE;
            return Integer.MIN_VALUE;
        }
        try {
            int start = bindings.immGetCompositionStringW(context, GCS_DELTASTART, MemorySegment.NULL, 0);
            lastCompositionDeltaStart = start;
            return start;
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Reads `GCS_COMPCLAUSE` or `GCS_RESULTCLAUSE` through generated `ImmGetCompositionStringW`.
    ///
    /// @param index `GCS_COMPCLAUSE` or `GCS_RESULTCLAUSE`
    /// @return the character offsets, possibly empty
    public int @Unmodifiable [] compositionClause(int index) {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            publishClause(index, Integer.MIN_VALUE, new int[0]);
            return new int[0];
        }
        try {
            int bytes = bindings.immGetCompositionStringW(context, index, MemorySegment.NULL, 0);
            if (bytes <= 0) {
                publishClause(index, bytes, new int[0]);
                return new int[0];
            }
            MemorySegment buffer = arena.allocate(bytes);
            int written = bindings.immGetCompositionStringW(context, index, buffer, bytes);
            if (written <= 0) {
                publishClause(index, written, new int[0]);
                return new int[0];
            }
            int count = written / 4;
            int[] offsets = new int[count];
            for (int offset = 0; offset < count; offset++) {
                offsets[offset] = buffer.get(ValueLayout.JAVA_INT, (long) offset * 4L);
            }
            publishClause(index, written, offsets);
            return offsets;
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Stores one clause query on the matching last-result fields.
    private void publishClause(int index, int bytes, int[] offsets) {
        if (index == GCS_RESULTCLAUSE) {
            lastResultClauseBytes = bytes;
            lastResultClause = offsets;
        } else if (index == GCS_COMPREADCLAUSE) {
            lastCompositionReadingClauseBytes = bytes;
            lastCompositionReadingClause = offsets;
        } else if (index == GCS_RESULTREADCLAUSE) {
            lastResultReadingClauseBytes = bytes;
            lastResultReadingClause = offsets;
        } else {
            lastCompositionClauseBytes = bytes;
            lastCompositionClause = offsets;
        }
    }

    /// Reads `GCS_COMPREADATTR` through generated `ImmGetCompositionStringW`.
    ///
    /// @return the reading-window attributes, possibly empty
    public byte @Unmodifiable [] compositionReadingAttributes() {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastCompositionReadingAttrBytes = Integer.MIN_VALUE;
            lastCompositionReadingAttributes = new byte[0];
            return lastCompositionReadingAttributes;
        }
        try {
            int bytes = bindings.immGetCompositionStringW(context, GCS_COMPREADATTR, MemorySegment.NULL, 0);
            lastCompositionReadingAttrBytes = bytes;
            if (bytes <= 0) {
                lastCompositionReadingAttributes = new byte[0];
                return lastCompositionReadingAttributes;
            }
            MemorySegment buffer = arena.allocate(bytes);
            int written = bindings.immGetCompositionStringW(context, GCS_COMPREADATTR, buffer, bytes);
            lastCompositionReadingAttrBytes = written;
            if (written <= 0) {
                lastCompositionReadingAttributes = new byte[0];
                return lastCompositionReadingAttributes;
            }
            byte[] attributes = new byte[written];
            for (int offset = 0; offset < written; offset++) {
                attributes[offset] = buffer.get(ValueLayout.JAVA_BYTE, offset);
            }
            lastCompositionReadingAttributes = attributes;
            return attributes;
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Reads `GCS_COMPATTR` through generated `ImmGetCompositionStringW`.
    ///
    /// @return the attribute bytes, possibly empty
    public byte @Unmodifiable [] compositionAttributes() {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastCompositionAttrBytes = Integer.MIN_VALUE;
            lastCompositionAttributes = new byte[0];
            return lastCompositionAttributes;
        }
        try {
            int bytes = bindings.immGetCompositionStringW(context, GCS_COMPATTR, MemorySegment.NULL, 0);
            lastCompositionAttrBytes = bytes;
            if (bytes <= 0) {
                lastCompositionAttributes = new byte[0];
                return lastCompositionAttributes;
            }
            MemorySegment buffer = arena.allocate(bytes);
            int written = bindings.immGetCompositionStringW(context, GCS_COMPATTR, buffer, bytes);
            lastCompositionAttrBytes = written;
            if (written <= 0) {
                lastCompositionAttributes = new byte[0];
                return lastCompositionAttributes;
            }
            byte[] attributes = new byte[written];
            for (int offset = 0; offset < written; offset++) {
                attributes[offset] = buffer.get(ValueLayout.JAVA_BYTE, offset);
            }
            lastCompositionAttributes = attributes;
            return attributes;
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Reads one IMM32 composition index through generated `ImmGetCompositionStringW`.
    ///
    /// @param index `GCS_COMPSTR` or `GCS_RESULTSTR`
    /// @return the string, or empty when the host has no composition
    public String compositionString(int index) {
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            lastCompositionStringBytes = Integer.MIN_VALUE;
            return "";
        }
        try {
            int bytes = bindings.immGetCompositionStringW(context, index, MemorySegment.NULL, 0);
            lastCompositionStringBytes = bytes;
            if (bytes <= 0) {
                return "";
            }
            MemorySegment buffer = arena.allocate(bytes);
            int written = bindings.immGetCompositionStringW(context, index, buffer, bytes);
            lastCompositionStringBytes = written;
            if (written <= 0) {
                return "";
            }
            int units = written / 2;
            char[] chars = new char[units];
            for (int offset = 0; offset < units; offset++) {
                chars[offset] = (char) buffer.get(ValueLayout.JAVA_SHORT, offset * 2L);
            }
            return new String(chars);
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Writes one IMM32 composition string through generated `ImmSetCompositionStringW`.
    ///
    /// @param text the composition string
    /// @return whether the host accepted the string
    public boolean setCompositionString(String text) {
        Objects.requireNonNull(text, "text");
        MemorySegment context = bindings.immGetContext(window);
        if (context.address() == 0L) {
            return false;
        }
        try {
            MemorySegment encoded = arena.allocateFrom(text, StandardCharsets.UTF_16LE);
            int bytes = text.length() * 2;
            return bindings.immSetCompositionStringW(
                    context,
                    0x0009,
                    encoded,
                    bytes,
                    MemorySegment.NULL,
                    0
            ) != 0;
        } finally {
            bindings.immReleaseContext(window, context);
        }
    }

    /// Arms `TrackMouseEvent(TME_LEAVE)` so `WM_MOUSELEAVE` is delivered after the cursor exits.
    private void trackMouseLeave() {
        if (mouseLeaveTracked) {
            return;
        }
        trackMouseEventRecord.fill((byte) 0);
        trackMouseEventRecord.set(
                ValueLayout.JAVA_INT,
                Win32Layouts.TRACKMOUSEEVENT_CB_SIZE_OFFSET,
                Math.toIntExact(Win32Layouts.TRACKMOUSEEVENT.byteSize())
        );
        trackMouseEventRecord.set(ValueLayout.JAVA_INT, Win32Layouts.TRACKMOUSEEVENT_DW_FLAGS_OFFSET, TME_LEAVE);
        trackMouseEventRecord.set(ValueLayout.ADDRESS, Win32Layouts.TRACKMOUSEEVENT_HWND_TRACK_OFFSET, window);
        Win32FfmBindings.TrackMouseEventResult tracked = bindings.trackMouseEvent(trackMouseEventRecord);
        lastTrackMouseEventSucceeded = tracked.value() != 0;
        mouseLeaveTracked = lastTrackMouseEventSucceeded;
    }

    /// Appends one BMP character from `WM_CHAR`.
    private void appendCharacter(int codeUnit) {
        if (codeUnit >= 0x20 && codeUnit != 0x7F && codeUnit <= 0xFFFF) {
            characters.append((char) codeUnit);
        }
    }

    /// Appends one Unicode scalar from `WM_UNICHAR`.
    private void appendUnichar(int codePoint) {
        if (!Character.isValidCodePoint(codePoint) || codePoint < 0x20 || codePoint == 0x7F) {
            return;
        }
        characters.append(Character.toChars(codePoint));
    }

    /// Destroys the HWND.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        endModalLoop();
        destroyImeContext();
        if (window.address() != 0L && lastAddClipboardFormatListenerResult != 0
                && lastAddClipboardFormatListenerResult != Integer.MIN_VALUE) {
            lastRemoveClipboardFormatListenerResult = bindings.removeClipboardFormatListener(window).value();
        }
        if (window.address() != 0L && !destroyObserved) {
            bindings.destroyWindow(window);
        }
        window = MemorySegment.NULL;
        if (classRegistered) {
            bindings.unregisterClassW(className, instance);
            classRegistered = false;
        }
        arena.close();
    }

    /// Registers the class and creates the HWND.
    private void initialize(
            String title,
            int x,
            int y,
            int width,
            int height,
            boolean popup,
            MemorySegment owner
    ) {
        MemorySegment callback = bindings.createWndProcStub(this::windowProcedure, callbackFailures, arena);
        MemorySegment windowClass = arena.allocate(Win32Layouts.WNDCLASSEXW);
        windowClass.fill((byte) 0);
        windowClass.set(
                ValueLayout.JAVA_INT,
                Win32Layouts.WNDCLASSEXW_CB_SIZE_OFFSET,
                Math.toIntExact(Win32Layouts.WNDCLASSEXW.byteSize())
        );
        windowClass.set(ValueLayout.ADDRESS, Win32Layouts.WNDCLASSEXW_WND_PROC_OFFSET, callback);
        windowClass.set(ValueLayout.ADDRESS, Win32Layouts.WNDCLASSEXW_INSTANCE_OFFSET, instance);
        windowClass.set(ValueLayout.ADDRESS, Win32Layouts.WNDCLASSEXW_CLASS_NAME_OFFSET, className);
        Win32FfmBindings.RegisterClassExWResult registration = bindings.registerClassExW(windowClass);
        if (registration.value() == 0) {
            throw new IllegalStateException("RegisterClassExW failed: " + registration.errorCode());
        }
        classRegistered = true;
        MemorySegment nativeTitle = arena.allocateFrom(title, StandardCharsets.UTF_16LE);
        int style = popup ? WS_POPUP : WS_OVERLAPPEDWINDOW;
        Win32FfmBindings.CreateWindowExWResult creation = bindings.createWindowExW(
                WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW,
                className,
                nativeTitle,
                style,
                x,
                y,
                width,
                height,
                owner,
                MemorySegment.NULL,
                instance,
                MemorySegment.NULL
        );
        if (creation.value().address() == 0L) {
            throw new IllegalStateException("CreateWindowExW failed: " + creation.errorCode());
        }
        window = creation.value();
        refreshGeometry();
        registerRawInput();
        enableMouseInPointer();
        queryPointerDevices();
        associateImeContext();
        readClipboardInventory();
        throwContained();
    }

    /// Reads clipboard format count and sequence through generated User32 bindings.
    private void readClipboardInventory() {
        lastClipboardFormatCount = bindings.countClipboardFormats();
        lastClipboardSequence = bindings.getClipboardSequenceNumber();
        lastClipboardOwner = bindings.getClipboardOwner().address();
        lastOpenClipboardWindow = bindings.getOpenClipboardWindow().address();
        lastClipboardUnicodeAvailable = bindings.isClipboardFormatAvailable(CF_UNICODETEXT);
        MemorySegment formats = arena.allocate(ValueLayout.JAVA_INT, 2);
        formats.setAtIndex(ValueLayout.JAVA_INT, 0L, CF_UNICODETEXT);
        formats.setAtIndex(ValueLayout.JAVA_INT, 1L, CF_TEXT);
        lastPriorityClipboardFormat = bindings.getPriorityClipboardFormat(formats, 2);
        int format = 0;
        int enumerated = 0;
        int first = 0;
        Win32FfmBindings.OpenClipboardResult opened = bindings.openClipboard(window);
        if (opened.value() != 0) {
            try {
                while (enumerated < 32) {
                    Win32FfmBindings.EnumClipboardFormatsResult next = bindings.enumClipboardFormats(format);
                    format = next.value();
                    if (format == 0) {
                        break;
                    }
                    if (enumerated == 0) {
                        first = format;
                    }
                    enumerated++;
                }
            } finally {
                bindings.closeClipboard();
            }
        }
        lastEnumClipboardFormat = first;
        lastEnumClipboardFormatCount = enumerated;
        MemorySegment name = arena.allocate(64L);
        name.fill((byte) 0);
        lastClipboardFormatNameChars = bindings.getClipboardFormatNameW(first == 0 ? CF_UNICODETEXT : first, name, 32)
                .value();
        MemorySegment updated = arena.allocate(ValueLayout.JAVA_INT, 8);
        MemorySegment updatedCount = arena.allocate(ValueLayout.JAVA_INT);
        updatedCount.set(ValueLayout.JAVA_INT, 0L, 0);
        lastUpdatedClipboardFormatsResult = bindings.getUpdatedClipboardFormats(updated, 8, updatedCount).value();
        lastUpdatedClipboardFormatCount = updatedCount.get(ValueLayout.JAVA_INT, 0L);
        lastAddClipboardFormatListenerResult = bindings.addClipboardFormatListener(window).value();
    }

    /// Dispatches lifecycle, DPI, and normalized input messages.
    ///
    /// Destroying one window does not post `WM_QUIT`; the session stays alive for remaining HWNDs.
    private long windowProcedure(MemorySegment callbackWindow, int message, long wParam, long lParam) {
        return switch (message) {
            case WM_CLOSE -> {
                lifecycle.closeRequested();
                yield 0L;
            }
            case WM_SETCURSOR -> {
                int hit = (int) (lParam & 0xFFFFL);
                if (hit == HTCLIENT && lastSystemCursor.address() != 0L) {
                    bindings.setCursor(lastSystemCursor);
                    yield 1L;
                }
                yield bindings.defWindowProcW(callbackWindow, message, wParam, lParam);
            }
            case WM_DESTROY -> {
                destroyObserved = true;
                lifecycle.destroyed();
                yield 0L;
            }
            case WM_PAINT -> paint(callbackWindow);
            case WM_ERASEBKGND -> lastRgba == null
                    ? bindings.defWindowProcW(callbackWindow, message, wParam, lParam)
                    : 1L;
            case WM_SIZE -> {
                clientWidth = lowWord(lParam);
                clientHeight = highWord(lParam);
                yield 0L;
            }
            case WM_CLIPBOARDUPDATE -> {
                lastClipboardUpdateCount++;
                yield 0L;
            }
            case WM_DPICHANGED -> {
                int nextDpi = lowWord(wParam);
                if (nextDpi > 0) {
                    dpi = nextDpi;
                }
                applySuggestedDpiRect(lParam);
                refreshClientRect();
                yield 0L;
            }
            case WM_MOUSEMOVE -> {
                lastMouseX = lowWord(lParam);
                lastMouseY = highWord(lParam);
                trackMouseLeave();
                pointerEvents.add(mouseEvent(PointerEventType.MOVE, wParam, lParam));
                yield 0L;
            }
            case WM_MOUSELEAVE -> {
                mouseLeaveTracked = false;
                pointerEvents.add(mouseEvent(
                        PointerEventType.LEAVE,
                        0L,
                        packPointer(lastMouseX, lastMouseY)
                ));
                yield 0L;
            }
            case WM_LBUTTONDOWN -> {
                bindings.setCapture(window);
                pointerEvents.add(mouseEvent(PointerEventType.DOWN, wParam, lParam));
                yield 0L;
            }
            case WM_LBUTTONUP -> {
                bindings.releaseCapture();
                pointerEvents.add(mouseEvent(PointerEventType.UP, wParam, lParam));
                yield 0L;
            }
            case WM_RBUTTONDOWN -> {
                pointerEvents.add(mouseEvent(PointerEventType.SECONDARY_DOWN, wParam, lParam));
                yield 0L;
            }
            case WM_RBUTTONUP -> {
                pointerEvents.add(mouseEvent(PointerEventType.SECONDARY_UP, wParam, lParam));
                yield 0L;
            }
            case WM_MBUTTONDOWN -> {
                pointerEvents.add(mouseEvent(PointerEventType.MIDDLE_DOWN, wParam, lParam));
                yield 0L;
            }
            case WM_MBUTTONUP -> {
                pointerEvents.add(mouseEvent(PointerEventType.MIDDLE_UP, wParam, lParam));
                yield 0L;
            }
            case WM_XBUTTONDOWN -> {
                pointerEvents.add(xButtonEvent(true, wParam, lParam));
                yield 0L;
            }
            case WM_XBUTTONUP -> {
                pointerEvents.add(xButtonEvent(false, wParam, lParam));
                yield 0L;
            }
            case WM_MOUSEWHEEL -> {
                pointerEvents.add(wheelEvent(PointerEventType.WHEEL, wParam, lParam, PointerDeviceKind.MOUSE));
                yield 0L;
            }
            case WM_MOUSEHWHEEL -> {
                pointerEvents.add(wheelEvent(PointerEventType.WHEEL_HORIZONTAL, wParam, lParam, PointerDeviceKind.MOUSE));
                yield 0L;
            }
            case WM_POINTERENTER -> {
                pointerEvents.add(pointerMessage(PointerEventType.ENTER, wParam, lParam));
                yield 0L;
            }
            case WM_POINTERLEAVE -> {
                pointerEvents.add(pointerMessage(PointerEventType.LEAVE, wParam, lParam));
                yield 0L;
            }
            case WM_POINTERROUTEDTO -> {
                pointerEvents.add(pointerMessage(PointerEventType.ROUTED_TO, wParam, lParam));
                yield 0L;
            }
            case WM_POINTERROUTEDAWAY -> {
                pointerEvents.add(pointerMessage(PointerEventType.ROUTED_AWAY, wParam, lParam));
                yield 0L;
            }
            case WM_POINTERROUTEDRELEASED -> {
                pointerEvents.add(pointerMessage(PointerEventType.ROUTED_RELEASED, wParam, lParam));
                yield 0L;
            }
            case WM_POINTERCAPTURECHANGED -> {
                pointerEvents.add(pointerMessage(PointerEventType.CAPTURE_CHANGED, wParam, lParam));
                yield 0L;
            }
            case WM_POINTERACTIVATE -> {
                pointerEvents.add(pointerMessage(PointerEventType.ACTIVATE, wParam, lParam));
                yield PA_ACTIVATE;
            }
            case WM_NCPOINTERUPDATE -> {
                pointerEvents.add(pointerMessage(PointerEventType.NON_CLIENT_MOVE, wParam, lParam));
                yield 0L;
            }
            case WM_NCPOINTERDOWN -> {
                pointerEvents.add(pointerMessage(PointerEventType.NON_CLIENT_DOWN, wParam, lParam));
                yield 0L;
            }
            case WM_NCPOINTERUP -> {
                pointerEvents.add(pointerMessage(PointerEventType.NON_CLIENT_UP, wParam, lParam));
                yield 0L;
            }
            case WM_POINTERWHEEL -> {
                pointerEvents.add(wheelEvent(PointerEventType.WHEEL, wParam, lParam, pointerDevice(wParam)));
                yield 0L;
            }
            case WM_POINTERHWHEEL -> {
                pointerEvents.add(wheelEvent(PointerEventType.WHEEL_HORIZONTAL, wParam, lParam, pointerDevice(wParam)));
                yield 0L;
            }
            case WM_POINTERUPDATE -> {
                pointerEvents.add(pointerMessage(PointerEventType.MOVE, wParam, lParam));
                yield 0L;
            }
            case WM_POINTERDOWN -> {
                pointerEvents.add(pointerMessage(PointerEventType.DOWN, wParam, lParam));
                yield 0L;
            }
            case WM_POINTERUP -> {
                pointerEvents.add(pointerMessage(PointerEventType.UP, wParam, lParam));
                yield 0L;
            }
            case WM_KEYDOWN, WM_SYSKEYDOWN -> {
                int virtualKey = (int) wParam;
                latchModifier(virtualKey, true);
                int scanCode = (int) ((lParam >>> 16) & 0xFFL);
                if (scanCode == 0) {
                    scanCode = mapVirtualKeyToScan(virtualKey);
                }
                boolean extended = (lParam & (1L << 24)) != 0L;
                @Nullable String text = translateVirtualKey(virtualKey, scanCode);
                rememberKeyName(scanCode, extended);
                rememberKeyboardLayout();
                @Nullable LogicalKey key = logicalKey(virtualKey);
                if (key != null) {
                    keyEvents.add(keyEvent(KeyEventType.DOWN, key, virtualKey, lParam, text));
                }
                yield 0L;
            }
            case WM_KEYUP, WM_SYSKEYUP -> {
                int virtualKey = (int) wParam;
                latchModifier(virtualKey, false);
                @Nullable LogicalKey key = logicalKey(virtualKey);
                if (key != null) {
                    keyEvents.add(keyEvent(KeyEventType.UP, key, virtualKey, lParam, null));
                }
                yield 0L;
            }
            case WM_CHAR, WM_SYSCHAR, WM_IME_CHAR -> {
                int codeUnit = (int) wParam;
                scanVirtualKey((char) codeUnit);
                appendCharacter(codeUnit);
                yield 0L;
            }
            case WM_IME_STARTCOMPOSITION -> {
                imeActive = true;
                imeEnded = false;
                readCompositionFont();
                readCompositionWindow();
                readImeVirtualKey();
                readImeIsIme();
                readImeMenuItems();
                escapeIme();
                readImeDescription();
                readImeStatusWindow();
                readImeHotKey();
                simulateImeHotKey();
                readConversionList(imeDocument);
                readImeUiMessage();
                readRegisterWordStyles();
                enumerateImeContexts();
                enumerateRegisterWords();
                probeRegisterWord();
                requestImeMessage();
                readImeStatus();
                yield 0L;
            }
            case WM_IME_COMPOSITION -> {
                readImeComposition((int) lParam);
                readGuideLine();
                readConversionList(imeComposition.toString());
                yield 0L;
            }
            case WM_IME_CONTROL -> {
                lastImeControl = (int) wParam;
                if (lastImeControl == IMC_GETSTATUSWINDOWPOS && lParam != 0L) {
                    MemorySegment dest = MemorySegment.ofAddress(lParam).reinterpret(Win32Layouts.POINT.byteSize());
                    dest.set(ValueLayout.JAVA_INT, Win32Layouts.POINT_X_OFFSET, imeCandidateX);
                    dest.set(ValueLayout.JAVA_INT, Win32Layouts.POINT_Y_OFFSET, imeCandidateY);
                    yield 1L;
                }
                yield 0L;
            }
            case WM_IME_ENDCOMPOSITION -> {
                notifyIme(CPS_CANCEL);
                imeActive = false;
                imeEnded = true;
                yield 0L;
            }
            case WM_IME_SETCONTEXT -> {
                lastImeSetContext = (int) wParam;
                yield bindings.defWindowProcW(callbackWindow, message, wParam, lParam);
            }
            case WM_IME_SELECT -> {
                lastImeSelect = (int) wParam;
                readImeIsIme();
                yield 0L;
            }
            case WM_IME_REQUEST -> {
                lastImeRequest = (int) wParam;
                yield handleImeRequest((int) wParam, lParam);
            }
            case WM_IME_NOTIFY -> {
                int command = (int) wParam;
                if (command == IMN_OPENCANDIDATE || command == IMN_CHANGECANDIDATE) {
                    readCandidateList((int) lParam);
                } else if (command == IMN_CLOSECANDIDATE) {
                    lastCandidateCount = 0;
                    lastCandidateSelection = 0;
                    lastCandidatePage = List.of();
                } else if (command == IMN_GUIDELINE) {
                    readGuideLine();
                }
                yield 0L;
            }
            case WM_DEADCHAR, WM_SYSDEADCHAR -> {
                int codeUnit = (int) wParam;
                if (codeUnit > 0 && codeUnit <= 0xFFFF) {
                    deadCharacters.append((char) codeUnit);
                }
                yield 0L;
            }
            case WM_UNICHAR -> {
                if ((int) wParam == UNICODE_NOCHAR) {
                    yield 1L;
                }
                appendUnichar((int) wParam);
                yield 0L;
            }
            case WM_ENTERSIZEMOVE -> {
                beginModalLoop();
                yield 0L;
            }
            case WM_EXITSIZEMOVE -> {
                endModalLoop();
                yield 0L;
            }
            case WM_INPUT -> {
                readRawInput(lParam);
                yield 0L;
            }
            case WM_INPUT_DEVICE_CHANGE -> {
                lastInputDeviceChange = (int) wParam;
                yield 0L;
            }
            case WM_TIMER -> {
                if (wParam == MODAL_TIMER_ID && modalLoopActive) {
                    modalTimerTicks++;
                    lifecycle.modalTick();
                }
                yield 0L;
            }
            case WM_POWERBROADCAST -> {
                int event = (int) wParam;
                if (event == PBT_APMSUSPEND) {
                    sleepEvents++;
                } else if (event == PBT_APMRESUMESUSPEND) {
                    wakeEvents++;
                }
                yield 1L;
            }
            default -> bindings.defWindowProcW(callbackWindow, message, wParam, lParam);
        };
    }

    /// Replays the last presented RGBA frame through `BeginPaint` and `SetDIBitsToDevice`.
    ///
    /// @param callbackWindow the HWND receiving `WM_PAINT`
    /// @return zero after handling
    private long paint(MemorySegment callbackWindow) {
        if (lastRgba == null) {
            return bindings.defWindowProcW(callbackWindow, WM_PAINT, 0L, 0L);
        }
        paintRecord.fill((byte) 0);
        MemorySegment deviceContext = bindings.beginPaint(callbackWindow, paintRecord);
        if (deviceContext.address() == 0L) {
            throw new IllegalStateException("BeginPaint returned NULL");
        }
        try {
            WindowsSoftwarePresent.presentSdrRgba(bindings, deviceContext, lastRgba, lastWidth, lastHeight);
        } finally {
            if (bindings.endPaint(callbackWindow, paintRecord) == 0) {
                throw new IllegalStateException("EndPaint failed");
            }
        }
        return 0L;
    }

    /// Starts the move/resize modal timer.
    private void beginModalLoop() {
        modalLoopActive = true;
        Win32FfmBindings.SetTimerResult timer = bindings.setTimer(window, MODAL_TIMER_ID, 16, MemorySegment.NULL);
        if (timer.value() == 0L) {
            throw new IllegalStateException("SetTimer failed: " + timer.errorCode());
        }
    }

    /// Stops the move/resize modal timer.
    private void endModalLoop() {
        if (modalLoopActive) {
            bindings.killTimer(window, MODAL_TIMER_ID);
        }
        modalLoopActive = false;
    }

    /// Reads DPI and client size from the live HWND.
    private void refreshGeometry() {
        int queried = bindings.getDpiForWindow(window);
        if (queried > 0) {
            dpi = queried;
        }
        refreshClientRect();
    }

    /// Reads the client rectangle.
    private void refreshClientRect() {
        rectRecord.fill((byte) 0);
        Win32FfmBindings.GetClientRectResult result = bindings.getClientRect(window, rectRecord);
        if (result.value() == 0) {
            throw new IllegalStateException("GetClientRect failed: " + result.errorCode());
        }
        int right = rectRecord.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_RIGHT_OFFSET);
        int bottom = rectRecord.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_BOTTOM_OFFSET);
        clientWidth = Math.max(0, right);
        clientHeight = Math.max(0, bottom);
    }

    /// Applies the suggested window rectangle supplied with `WM_DPICHANGED`.
    ///
    /// @param lParam the pointer to the suggested `RECT`
    private void applySuggestedDpiRect(long lParam) {
        if (lParam == 0L) {
            return;
        }
        MemorySegment suggested = MemorySegment.ofAddress(lParam).reinterpret(Win32Layouts.RECT.byteSize());
        int left = suggested.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_LEFT_OFFSET);
        int top = suggested.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_TOP_OFFSET);
        int right = suggested.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_RIGHT_OFFSET);
        int bottom = suggested.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_BOTTOM_OFFSET);
        bindings.setWindowPos(
                window,
                MemorySegment.NULL,
                left,
                top,
                Math.max(1, right - left),
                Math.max(1, bottom - top),
                SWP_NOZORDER | SWP_NOACTIVATE
        );
    }

    /// Latches Shift, Control, or Alt from a virtual-key code.
    ///
    /// @param virtualKey the `wParam` virtual-key code
    /// @param down whether the key is pressed
    private void latchModifier(int virtualKey, boolean down) {
        if (virtualKey == VK_SHIFT) {
            shiftDown = down;
        } else if (virtualKey == VK_CONTROL) {
            ctrlDown = down;
        } else if (virtualKey == VK_MENU) {
            altDown = down;
        } else if (virtualKey == VK_LWIN || virtualKey == VK_RWIN) {
            metaDown = down;
        }
    }

    /// Maps a virtual-key code onto the layout logical-key set.
    ///
    /// @param virtualKey the `wParam` virtual-key code
    /// @return the logical key, or `null` when the key is outside the first-stable set
    private static @Nullable LogicalKey logicalKey(int virtualKey) {
        return switch (virtualKey) {
            case 0x08 -> LogicalKey.BACKSPACE;
            case 0x09 -> LogicalKey.TAB;
            case 0x1B -> LogicalKey.ESCAPE;
            case 0x0D -> LogicalKey.ENTER;
            case 0x20 -> LogicalKey.SPACE;
            case 0x21 -> LogicalKey.PAGE_UP;
            case 0x22 -> LogicalKey.PAGE_DOWN;
            case 0x23 -> LogicalKey.END;
            case 0x24 -> LogicalKey.HOME;
            case 0x25 -> LogicalKey.ARROW_LEFT;
            case 0x26 -> LogicalKey.ARROW_UP;
            case 0x27 -> LogicalKey.ARROW_RIGHT;
            case 0x28 -> LogicalKey.ARROW_DOWN;
            case 0x2E -> LogicalKey.DELETE;
            case 0x5B, 0x5C -> LogicalKey.META;
            default -> null;
        };
    }

    /// Returns the signed low word of a packed `LPARAM`.
    ///
    /// @param value the packed value
    /// @return the low 16 bits as a signed coordinate
    static int lowWord(long value) {
        return (short) value;
    }

    /// Returns the signed high word of a packed `LPARAM`.
    ///
    /// @param value the packed value
    /// @return the high 16 bits as a signed coordinate
    static int highWord(long value) {
        return (short) (value >>> 16);
    }

    /// Packs client coordinates into a mouse `LPARAM`.
    ///
    /// @param x the client x
    /// @param y the client y
    /// @return the packed parameter
    static long packPointer(int x, int y) {
        return (Integer.toUnsignedLong(y & 0xFFFF) << 16) | Integer.toUnsignedLong(x & 0xFFFF);
    }

    /// Throws a contained callback failure.
    private void throwContained() {
        @Nullable Throwable failure = callbackFailures.poll();
        if (failure != null) {
            throw new IllegalStateException("Windows window callback failed", failure);
        }
    }

    /// Verifies the window is open.
    private void requireOpen() {
        if (closed || window.address() == 0L) {
            throw new IllegalStateException("Windows native window is closed");
        }
    }
}
