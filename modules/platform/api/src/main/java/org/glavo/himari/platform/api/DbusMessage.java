package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Encodes and decodes little-endian D-Bus message headers used by AT-SPI2.
///
/// The codec covers method-call headers with path, interface, member, and destination
/// string fields. The body is carried as a borrowed byte slice and is not interpreted.
@NotNullByDefault
public record DbusMessage(
        int type,
        int serial,
        String path,
        String member,
        @Nullable String iface,
        @Nullable String destination,
        byte[] body
) {
    /// Method-call message type.
    public static final int METHOD_CALL = 1;

    /// Little-endian header endianness byte.
    private static final byte LITTLE = 'l';

    /// Protocol version 1.
    private static final byte VERSION = 1;

    /// Header field PATH.
    private static final byte FIELD_PATH = 1;

    /// Header field INTERFACE.
    private static final byte FIELD_INTERFACE = 2;

    /// Header field MEMBER.
    private static final byte FIELD_MEMBER = 3;

    /// Header field DESTINATION.
    private static final byte FIELD_DESTINATION = 6;

    /// Creates a validated method-call header.
    public DbusMessage {
        if (type != METHOD_CALL) {
            throw new IllegalArgumentException("Only D-Bus method-call messages are supported");
        }
        if (serial <= 0) {
            throw new IllegalArgumentException("D-Bus serial must be positive");
        }
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(body, "body");
        if (path.isEmpty() || member.isEmpty()) {
            throw new IllegalArgumentException("D-Bus path and member must be non-empty");
        }
    }

    /// Encodes this header and body as a little-endian D-Bus message.
    ///
    /// @return the wire bytes
    public byte[] encode() {
        ByteBuffer fields = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        writeField(fields, FIELD_PATH, 'o', path);
        if (iface != null) {
            writeField(fields, FIELD_INTERFACE, 's', iface);
        }
        writeField(fields, FIELD_MEMBER, 's', member);
        if (destination != null) {
            writeField(fields, FIELD_DESTINATION, 's', destination);
        }
        int fieldBytes = fields.position();
        int headerEnd = align(16 + fieldBytes, 8);
        ByteBuffer message = ByteBuffer.allocate(headerEnd + body.length).order(ByteOrder.LITTLE_ENDIAN);
        message.put(LITTLE);
        message.put((byte) type);
        message.put((byte) 0);
        message.put(VERSION);
        message.putInt(body.length);
        message.putInt(serial);
        message.putInt(fieldBytes);
        message.put(fields.array(), 0, fieldBytes);
        message.position(headerEnd);
        message.put(body);
        return message.array();
    }

    /// Decodes a little-endian method-call message.
    ///
    /// @param bytes the wire bytes
    /// @return the message
    public static DbusMessage decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < 16) {
            throw new IllegalArgumentException("D-Bus header is truncated");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.get() != LITTLE) {
            throw new IllegalArgumentException("D-Bus message must be little-endian");
        }
        int type = Byte.toUnsignedInt(buffer.get());
        buffer.get();
        if (buffer.get() != VERSION) {
            throw new IllegalArgumentException("Unsupported D-Bus protocol version");
        }
        int bodyLength = buffer.getInt();
        int serial = buffer.getInt();
        int fieldBytes = buffer.getInt();
        if (fieldBytes < 0 || 16 + fieldBytes > bytes.length) {
            throw new IllegalArgumentException("D-Bus header fields are truncated");
        }
        int fieldsEnd = 16 + fieldBytes;
        String path = "";
        String member = "";
        @Nullable String iface = null;
        @Nullable String destination = null;
        while (buffer.position() < fieldsEnd) {
            alignBuffer(buffer, 8);
            if (buffer.position() >= fieldsEnd) {
                break;
            }
            int field = Byte.toUnsignedInt(buffer.get());
            int signatureLength = Byte.toUnsignedInt(buffer.get());
            if (signatureLength < 1 || buffer.remaining() < signatureLength + 1) {
                throw new IllegalArgumentException("D-Bus field signature is truncated");
            }
            byte signature = buffer.get();
            buffer.get();
            alignBuffer(buffer, 4);
            String value = readString(buffer);
            if (signature == 'o' && field == FIELD_PATH) {
                path = value;
            } else if (signature == 's' && field == FIELD_INTERFACE) {
                iface = value;
            } else if (signature == 's' && field == FIELD_MEMBER) {
                member = value;
            } else if (signature == 's' && field == FIELD_DESTINATION) {
                destination = value;
            }
        }
        int headerEnd = align(fieldsEnd, 8);
        if (headerEnd + bodyLength > bytes.length) {
            throw new IllegalArgumentException("D-Bus body is truncated");
        }
        byte[] body = new byte[bodyLength];
        System.arraycopy(bytes, headerEnd, body, 0, bodyLength);
        return new DbusMessage(type, serial, path, member, iface, destination, body);
    }

    /// Writes one header field.
    private static void writeField(ByteBuffer buffer, byte field, char signature, String value) {
        alignBuffer(buffer, 8);
        buffer.put(field);
        buffer.put((byte) 1);
        buffer.put((byte) signature);
        buffer.put((byte) 0);
        alignBuffer(buffer, 4);
        writeString(buffer, value);
    }

    /// Writes a D-Bus string.
    private static void writeString(ByteBuffer buffer, String value) {
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(utf8.length);
        buffer.put(utf8);
        buffer.put((byte) 0);
    }

    /// Reads a D-Bus string.
    private static String readString(ByteBuffer buffer) {
        int length = buffer.getInt();
        if (length < 0 || buffer.remaining() < length + 1) {
            throw new IllegalArgumentException("D-Bus string is truncated");
        }
        byte[] utf8 = new byte[length];
        buffer.get(utf8);
        if (buffer.get() != 0) {
            throw new IllegalArgumentException("D-Bus string is not NUL-terminated");
        }
        return new String(utf8, StandardCharsets.UTF_8);
    }

    /// Advances `buffer` to the next multiple of `alignment`.
    private static void alignBuffer(ByteBuffer buffer, int alignment) {
        buffer.position(align(buffer.position(), alignment));
    }

    /// Rounds `offset` up to `alignment`.
    private static int align(int offset, int alignment) {
        return (offset + alignment - 1) & -alignment;
    }
}
