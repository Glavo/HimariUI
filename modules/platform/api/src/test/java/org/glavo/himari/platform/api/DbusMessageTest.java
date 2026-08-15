package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies little-endian D-Bus method-call header round trips.
@NotNullByDefault
final class DbusMessageTest {
    /// Encodes and decodes an AT-SPI GetAddress call.
    @Test
    void roundTripsAtSpiGetAddress() {
        DbusMessage call = new DbusMessage(
                DbusMessage.METHOD_CALL,
                7,
                "/org/a11y/bus",
                "GetAddress",
                AtSpiProbe.BUS_NAME,
                AtSpiProbe.BUS_NAME,
                new byte[] {1, 2, 3}
        );
        DbusMessage decoded = DbusMessage.decode(call.encode());
        assertEquals(DbusMessage.METHOD_CALL, decoded.type());
        assertEquals(7, decoded.serial());
        assertEquals("/org/a11y/bus", decoded.path());
        assertEquals("GetAddress", decoded.member());
        assertEquals(AtSpiProbe.BUS_NAME, decoded.iface());
        assertEquals(AtSpiProbe.BUS_NAME, decoded.destination());
        assertArrayEquals(new byte[] {1, 2, 3}, decoded.body());
    }

    /// Records an environment block on this Windows host.
    @Test
    void atSpiProbeIsEnvironmentBlockedOnNonLinux() {
        AtSpiProbe probe = AtSpiProbe.run();
        assertEquals("environment-blocked", probe.status());
        assertEquals(AtSpiProbe.BUS_NAME, probe.destination());
        assertTrue(probe.headerBytes() > 16);
        DbusMessage decoded = DbusMessage.decode(new DbusMessage(
                DbusMessage.METHOD_CALL,
                1,
                "/org/a11y/bus",
                "GetAddress",
                AtSpiProbe.BUS_NAME,
                AtSpiProbe.BUS_NAME,
                new byte[0]
        ).encode());
        assertEquals("GetAddress", decoded.member());
    }
}
