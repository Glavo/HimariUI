package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.api.ImeSession;
import org.jetbrains.annotations.NotNullByDefault;

/// Windows host adapter for the portable [ImeSession] contract.
///
/// Composition updates may be injected by tests or delivered from a TSF or IMM32 adapter. The
/// session never writes application or editor state.
@NotNullByDefault
public final class WindowsImeSession extends ImeSession {
    /// Creates an idle session.
    public WindowsImeSession() {
    }
}
