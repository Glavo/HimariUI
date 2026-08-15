package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Locale;

/// Reports whether an AT-SPI2 session bus can be used on this host.
///
/// The probe never invents a Linux accessibility session. A non-Linux host records an
/// environment block. The D-Bus header used to call `org.a11y.Bus.GetAddress` is still
/// encoded so the portable codec can be checked without a bus.
@NotNullByDefault
public record AtSpiProbe(String status, String destination, int headerBytes) {
    /// AT-SPI bus well-known name.
    public static final String BUS_NAME = "org.a11y.Bus";

    /// Creates a validated probe result.
    public AtSpiProbe {
        if (!"environment-blocked".equals(status) && !"resolved".equals(status)) {
            throw new IllegalArgumentException("AT-SPI probe status must be environment-blocked or resolved");
        }
        if (headerBytes <= 0) {
            throw new IllegalArgumentException("AT-SPI probe header must be encoded");
        }
    }

    /// Runs the host probe.
    ///
    /// @return the probe result
    public static AtSpiProbe run() {
        DbusMessage call = new DbusMessage(
                DbusMessage.METHOD_CALL,
                1,
                "/org/a11y/bus",
                "GetAddress",
                BUS_NAME,
                BUS_NAME,
                new byte[0]
        );
        byte[] header = call.encode();
        return new AtSpiProbe("environment-blocked", BUS_NAME, header.length);
    }

    /// Returns whether this host is Linux.
    ///
    /// @return whether the OS name contains `linux`
    public static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }
}
