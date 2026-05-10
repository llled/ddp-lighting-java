package org.llled.ddp;

/**
 * Stateless packet decoder for DDP protocol with zero-copy buffer operations.
 *
 * All methods are static and thread-safe. The caller provides buffers and this
 * class reads directly from them without allocation.
 *
 * Symmetric counterpart to {@link DdpPacketEncoder}.
 */
public class DdpPacketDecoder {

    private DdpPacketDecoder() {
        // Prevent instantiation - this is a utility class
    }

    /**
     * Extracts the flags byte (byte 0) from a DDP packet.
     *
     * @param buffer Raw packet buffer
     * @return flags byte (version + flags)
     */
    public static byte getFlags(byte[] buffer) {
        return buffer[0];
    }

    /**
     * Checks if the PUSH flag is set in the packet header.
     *
     * @param buffer Raw packet buffer
     * @return true if this is the last packet in a frame
     */
    public static boolean isPush(byte[] buffer) {
        return (buffer[0] & DdpProtocol.FLAG_PUSH) != 0;
    }

    /**
     * Extracts the data type (byte 2) from a DDP packet.
     *
     * @param buffer Raw packet buffer
     * @return data type constant (e.g., TYPE_RGB_8BIT)
     */
    public static byte getDataType(byte[] buffer) {
        return buffer[2];
    }

    /**
     * Extracts the destination ID (byte 3) from a DDP packet.
     *
     * @param buffer Raw packet buffer
     * @return destination ID (e.g., ID_DISPLAY)
     */
    public static byte getDestination(byte[] buffer) {
        return buffer[3];
    }

    /**
     * Extracts the frame byte offset (bytes 4-7) from a DDP packet.
     *
     * @param buffer Raw packet buffer
     * @return 32-bit big-endian offset
     */
    public static int getOffset(byte[] buffer) {
        return ((buffer[4] & 0xFF) << 24) |
               ((buffer[5] & 0xFF) << 16) |
               ((buffer[6] & 0xFF) << 8) |
               (buffer[7] & 0xFF);
    }

    /**
     * Extracts the payload length (bytes 8-9) from a DDP packet.
     *
     * @param buffer Raw packet buffer
     * @return 16-bit big-endian payload length
     */
    public static int getPayloadLength(byte[] buffer) {
        return ((buffer[8] & 0xFF) << 8) | (buffer[9] & 0xFF);
    }

    /**
     * Validates that a buffer contains a plausible DDP packet.
     *
     * @param buffer Raw packet buffer
     * @param receivedLength Number of bytes actually received
     * @throws IllegalArgumentException if the buffer is invalid
     */
    public static void validatePacket(byte[] buffer, int receivedLength) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }
        if (receivedLength < DdpProtocol.HEADER_LENGTH) {
            throw new IllegalArgumentException(
                    "Packet too short: need at least " + DdpProtocol.HEADER_LENGTH
                            + " bytes, got " + receivedLength);
        }
        int payloadLength = getPayloadLength(buffer);
        if (receivedLength < DdpProtocol.HEADER_LENGTH + payloadLength) {
            throw new IllegalArgumentException(
                    "Packet truncated: header says " + (DdpProtocol.HEADER_LENGTH + payloadLength)
                            + " bytes, got " + receivedLength);
        }
    }
}
