package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes portable D-Bus / AT-SPI2 probe evidence.
@NotNullByDefault
public final class AtSpiConformance {
    /// Prevents instantiation.
    private AtSpiConformance() {
    }

    /// Encodes an AT-SPI GetAddress call and records the host probe.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        DbusMessage call = new DbusMessage(
                DbusMessage.METHOD_CALL,
                1,
                "/org/a11y/bus",
                "GetAddress",
                AtSpiProbe.BUS_NAME,
                AtSpiProbe.BUS_NAME,
                new byte[0]
        );
        DbusMessage decoded = DbusMessage.decode(call.encode());
        if (!"/org/a11y/bus".equals(decoded.path()) || !"GetAddress".equals(decoded.member())) {
            throw new IllegalStateException("D-Bus AT-SPI header did not round-trip");
        }
        AtSpiProbe probe = AtSpiProbe.run();
        if (!"environment-blocked".equals(probe.status()) && !AtSpiProbe.isLinux()) {
            throw new IllegalStateException("Non-Linux host invented an AT-SPI session");
        }
        if (probe.headerBytes() <= 16) {
            throw new IllegalStateException("AT-SPI probe did not encode a D-Bus header");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m9-linux-a11y",
                          "workPackage": "LINUX-A11Y-001",
                          "status": "passed",
                          "dbusRoundTrip": true,
                          "path": "%s",
                          "member": "%s",
                          "probeStatus": "%s",
                          "headerBytes": %d
                        }
                        """.formatted(decoded.path(), decoded.member(), probe.status(), probe.headerBytes()),
                StandardCharsets.UTF_8
        );
    }
}
