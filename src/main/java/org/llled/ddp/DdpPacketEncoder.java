package org.llled.ddp;

/**
 * Stateless packet encoder for DDP protocol with zero-copy buffer operations.
 *
 * All methods are static and thread-safe. The caller provides buffers and this
 * class writes directly to them without allocation.
 */
public class DdpPacketEncoder {

    private DdpPacketEncoder() {
        // Prevent instantiation - this is a utility class
    }

    /**
     * Writes DDP header into buffer at offset 0.
     *
     * @param buffer Target buffer (must be at least HEADER_LENGTH bytes)
     * @param offset Frame byte offset for this packet (big-endian 32-bit)
     * @param payloadLength Number of data bytes in payload (big-endian 16-bit)
     * @param push True if this is the last packet in frame (set PUSH flag)
     * @param dataType Data type constant (e.g., TYPE_RGB_8BIT)
     * @param destination Destination ID (typically ID_DISPLAY)
     */
    public static void encodeHeader(
            byte[] buffer,
            int offset,
            int payloadLength,
            boolean push,
            byte dataType,
            byte destination
    ) {
        // Byte 0: Flags (version + optional push flag)
        buffer[0] = (byte) (DdpProtocol.VERSION_1 | (push ? DdpProtocol.FLAG_PUSH : 0));

        // Byte 1: Sequence number (unused by most implementations)
        buffer[1] = 0;

        // Byte 2: Data type
        buffer[2] = dataType;

        // Byte 3: Destination ID
        buffer[3] = destination;

        // Bytes 4-7: Offset (32-bit big-endian)
        buffer[4] = (byte) ((offset >>> 24) & 0xFF);
        buffer[5] = (byte) ((offset >>> 16) & 0xFF);
        buffer[6] = (byte) ((offset >>> 8) & 0xFF);
        buffer[7] = (byte) (offset & 0xFF);

        // Bytes 8-9: Length (16-bit big-endian)
        buffer[8] = (byte) ((payloadLength >>> 8) & 0xFF);
        buffer[9] = (byte) (payloadLength & 0xFF);
    }

    /**
     * Encodes RGB pixel (24-bit) into buffer.
     *
     * @param buffer Target buffer
     * @param position Write position in buffer
     * @param rgb Packed RGB color (0xRRGGBB)
     */
    public static void encodeRgbPixel(byte[] buffer, int position, int rgb) {
        buffer[position] = (byte) ((rgb >>> 16) & 0xFF); // R
        buffer[position + 1] = (byte) ((rgb >>> 8) & 0xFF); // G
        buffer[position + 2] = (byte) (rgb & 0xFF); // B
    }

    /**
     * Validates buffer size for packet encoding.
     *
     * @param buffer Packet buffer to validate
     * @param payloadLength Expected payload length
     * @throws IllegalArgumentException if buffer is null or too small
     */
    public static void validatePacketBuffer(byte[] buffer, int payloadLength) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }
        if (buffer.length < DdpProtocol.HEADER_LENGTH + payloadLength) {
            throw new IllegalArgumentException(
                    "Buffer too small: need " + (DdpProtocol.HEADER_LENGTH + payloadLength)
                            + " bytes, got " + buffer.length);
        }
        if (payloadLength > DdpProtocol.MAX_PAYLOAD_LENGTH) {
            throw new IllegalArgumentException(
                    "Payload too large: max " + DdpProtocol.MAX_PAYLOAD_LENGTH
                            + " bytes, got " + payloadLength);
        }
        if (payloadLength < 0) {
            throw new IllegalArgumentException("Payload length cannot be negative: " + payloadLength);
        }
    }
}
