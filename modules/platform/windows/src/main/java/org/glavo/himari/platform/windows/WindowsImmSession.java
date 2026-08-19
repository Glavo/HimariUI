package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/// Applies IMM32 composition-window placement as the documented TSF fallback.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsImmSession {
    /// `CFS_FORCE_POSITION`.
    private static final int CFS_FORCE_POSITION = 0x0020;

    /// `CFS_CANDIDATEPOS`.
    private static final int CFS_CANDIDATEPOS = 0x0040;

    /// Prevents instantiation.
    private WindowsImmSession() {
    }

    /// Pushes the IME candidate rectangle into the IMM32 composition window.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the focused HWND
    /// @param ime the editor session that owns the candidate rectangle
    /// @return whether `ImmSetCompositionWindow` and `ImmSetCandidateWindow` both succeeded
    public static boolean applyCandidateRectangle(
            WindowsLibraries libraries,
            MemorySegment hwnd,
            WindowsImeSession ime
    ) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(hwnd, "hwnd");
        Objects.requireNonNull(ime, "ime");
        if (hwnd.address() == 0L) {
            throw new IllegalArgumentException("HWND must not be NULL");
        }
        Win32FfmBindings bindings = libraries.bindings();
        MemorySegment context = bindings.immGetContext(hwnd);
        if (context.address() == 0L) {
            return false;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment form = arena.allocate(Win32Layouts.COMPOSITIONFORM);
            form.fill((byte) 0);
            form.set(ValueLayout.JAVA_INT, Win32Layouts.COMPOSITIONFORM_STYLE_OFFSET, CFS_FORCE_POSITION);
            form.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.COMPOSITIONFORM_CURRENT_POS_OFFSET + Win32Layouts.POINT_X_OFFSET,
                    Math.round(ime.candidateX())
            );
            form.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.COMPOSITIONFORM_CURRENT_POS_OFFSET + Win32Layouts.POINT_Y_OFFSET,
                    Math.round(ime.candidateY())
            );
            int left = Math.round(ime.candidateX());
            int top = Math.round(ime.candidateY());
            form.set(ValueLayout.JAVA_INT, Win32Layouts.COMPOSITIONFORM_AREA_OFFSET + Win32Layouts.RECT_LEFT_OFFSET, left);
            form.set(ValueLayout.JAVA_INT, Win32Layouts.COMPOSITIONFORM_AREA_OFFSET + Win32Layouts.RECT_TOP_OFFSET, top);
            form.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.COMPOSITIONFORM_AREA_OFFSET + Win32Layouts.RECT_RIGHT_OFFSET,
                    left + Math.round(ime.candidateWidth())
            );
            form.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.COMPOSITIONFORM_AREA_OFFSET + Win32Layouts.RECT_BOTTOM_OFFSET,
                    top + Math.round(ime.candidateHeight())
            );
            boolean composition = bindings.immSetCompositionWindow(context, form) != 0;
            MemorySegment candidate = arena.allocate(Win32Layouts.CANDIDATEFORM);
            candidate.fill((byte) 0);
            candidate.set(ValueLayout.JAVA_INT, Win32Layouts.CANDIDATEFORM_INDEX_OFFSET, 0);
            candidate.set(ValueLayout.JAVA_INT, Win32Layouts.CANDIDATEFORM_STYLE_OFFSET, CFS_CANDIDATEPOS);
            candidate.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.CANDIDATEFORM_CURRENT_POS_OFFSET + Win32Layouts.POINT_X_OFFSET,
                    left
            );
            candidate.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.CANDIDATEFORM_CURRENT_POS_OFFSET + Win32Layouts.POINT_Y_OFFSET,
                    top
            );
            candidate.set(ValueLayout.JAVA_INT, Win32Layouts.CANDIDATEFORM_AREA_OFFSET + Win32Layouts.RECT_LEFT_OFFSET, left);
            candidate.set(ValueLayout.JAVA_INT, Win32Layouts.CANDIDATEFORM_AREA_OFFSET + Win32Layouts.RECT_TOP_OFFSET, top);
            candidate.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.CANDIDATEFORM_AREA_OFFSET + Win32Layouts.RECT_RIGHT_OFFSET,
                    left + Math.round(ime.candidateWidth())
            );
            candidate.set(
                    ValueLayout.JAVA_INT,
                    Win32Layouts.CANDIDATEFORM_AREA_OFFSET + Win32Layouts.RECT_BOTTOM_OFFSET,
                    top + Math.round(ime.candidateHeight())
            );
            boolean candidateWindow = bindings.immSetCandidateWindow(context, candidate) != 0;
            return composition && candidateWindow;
        } finally {
            bindings.immReleaseContext(hwnd, context);
        }
    }
}
