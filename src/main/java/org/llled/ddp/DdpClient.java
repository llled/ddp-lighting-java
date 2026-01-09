package org.llled.ddp;

import java.io.IOException;
import java.net.*;

/**
 * UDP client for DDP (Distributed Display Protocol) with zero-copy buffer API.
 *
 * This class manages a UDP socket connection to a DDP device and provides
 * methods to send pixel data frames. The caller provides all buffers for
 * zero-copy operation.
 *
 * Thread Safety: Each instance should be used by a single thread, or external
 * synchronization must be provided.
 */
public class DdpClient {

    private final DatagramSocket socket;
    private final InetAddress targetAddress;
    private final int targetPort;

    // Reusable DatagramPacket for sending (caller manages buffer)
    private final DatagramPacket sendPacket;

    /**
     * Creates DDP client for specific target.
     *
     * @param hostname Target IP or hostname
     * @param port Target port (typically 4048)
     * @throws DdpException if socket or hostname resolution fails
     */
    public DdpClient(String hostname, int port) throws DdpException {
        this.targetPort = port;

        try {
            this.socket = new DatagramSocket();
            this.targetAddress = InetAddress.getByName(hostname);

            // Pre-configure reusable packet with destination
            this.sendPacket = new DatagramPacket(
                    new byte[0], 0, 0, targetAddress, port
            );

        } catch (SocketException e) {
            throw new DdpException("Failed to create UDP socket", e);
        } catch (UnknownHostException e) {
            throw new DdpException("Invalid hostname: " + hostname, e);
        }
    }

    /**
     * Convenience constructor using default DDP port.
     *
     * @param hostname Target IP or hostname
     * @throws DdpException if socket or hostname resolution fails
     */
    public DdpClient(String hostname) throws DdpException {
        this(hostname, DdpProtocol.DEFAULT_PORT);
    }

    /**
     * Sends raw packet buffer (zero-copy).
     *
     * @param buffer Packet buffer (header + payload already encoded)
     * @param length Total packet length to send
     * @throws DdpException if send fails
     */
    public void sendPacket(byte[] buffer, int length) throws DdpException {
        try {
            sendPacket.setData(buffer, 0, length);
            socket.send(sendPacket);
        } catch (IOException e) {
            throw new DdpException("Failed to send DDP packet to "
                    + targetAddress + ":" + targetPort, e);
        }
    }

    /**
     * Sends pixel frame data with automatic packetization.
     *
     * Generic method that supports any pixel format (RGB, RGBW, etc.).
     * Uses caller-provided packetBuffer for zero-copy operation. Automatically
     * fragments the frame into multiple packets if needed based on MAX_PAYLOAD_LENGTH.
     *
     * @param pixelData Pixel data buffer
     * @param pixelCount Number of pixels to send
     * @param bytesPerPixel Bytes per pixel (3 for RGB, 4 for RGBW)
     * @param dataType DDP data type constant (e.g., TYPE_RGB_8BIT, TYPE_RGBA_8BIT)
     * @param packetBuffer Reusable buffer (min size: HEADER_LENGTH + MAX_PAYLOAD_LENGTH)
     * @throws DdpException if send fails or buffer invalid
     */
    public void sendFrame(byte[] pixelData, int pixelCount, int bytesPerPixel,
                         byte dataType, byte[] packetBuffer) throws DdpException {

        // Validate buffer
        try {
            DdpPacketEncoder.validatePacketBuffer(
                    packetBuffer,
                    DdpProtocol.MAX_PAYLOAD_LENGTH
            );
        } catch (IllegalArgumentException e) {
            throw new DdpException("Invalid buffer: " + e.getMessage(), e);
        }

        int totalBytes = pixelCount * bytesPerPixel;

        // Validate pixel data size
        if (pixelData.length < totalBytes) {
            throw new DdpException("Pixel data buffer too small: need " + totalBytes
                    + " bytes, got " + pixelData.length);
        }

        int frameOffset = 0;
        int sourcePosition = 0;

        while (sourcePosition < totalBytes) {
            int remainingBytes = totalBytes - sourcePosition;
            int payloadLength = Math.min(remainingBytes, DdpProtocol.MAX_PAYLOAD_LENGTH);
            boolean isLastPacket = (sourcePosition + payloadLength >= totalBytes);

            // Encode header
            DdpPacketEncoder.encodeHeader(
                    packetBuffer,
                    frameOffset,
                    payloadLength,
                    isLastPacket,
                    dataType,
                    DdpProtocol.ID_DISPLAY
            );

            // Copy payload data
            System.arraycopy(
                    pixelData, sourcePosition,
                    packetBuffer, DdpProtocol.HEADER_LENGTH,
                    payloadLength
            );

            // Send packet
            sendPacket(packetBuffer, DdpProtocol.HEADER_LENGTH + payloadLength);

            frameOffset += payloadLength;
            sourcePosition += payloadLength;
        }
    }

    /**
     * Sends RGB frame data with automatic packetization.
     *
     * Convenience method for RGB (3 bytes per pixel) frames.
     * Uses caller-provided packetBuffer for zero-copy operation.
     *
     * @param rgbData RGB pixel data (3 bytes per pixel)
     * @param pixelCount Number of pixels to send
     * @param packetBuffer Reusable buffer (min size: HEADER_LENGTH + MAX_PAYLOAD_LENGTH)
     * @throws DdpException if send fails or buffer invalid
     */
    public void sendRgbFrame(byte[] rgbData, int pixelCount, byte[] packetBuffer)
            throws DdpException {
        sendFrame(rgbData, pixelCount, DdpProtocol.BYTES_PER_PIXEL_RGB,
                 DdpProtocol.TYPE_RGB_8BIT, packetBuffer);
    }

    /**
     * Sends RGBW frame data with automatic packetization.
     *
     * Convenience method for RGBW (4 bytes per pixel) frames.
     * Uses caller-provided packetBuffer for zero-copy operation.
     *
     * @param rgbwData RGBW pixel data (4 bytes per pixel)
     * @param pixelCount Number of pixels to send
     * @param packetBuffer Reusable buffer (min size: HEADER_LENGTH + MAX_PAYLOAD_LENGTH)
     * @throws DdpException if send fails or buffer invalid
     */
    public void sendRgbwFrame(byte[] rgbwData, int pixelCount, byte[] packetBuffer)
            throws DdpException {
        sendFrame(rgbwData, pixelCount, DdpProtocol.BYTES_PER_PIXEL_RGBA,
                 DdpProtocol.TYPE_RGBA_8BIT, packetBuffer);
    }

    /**
     * Closes UDP socket and releases resources.
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Checks if the socket is closed.
     *
     * @return true if socket is closed or null
     */
    public boolean isClosed() {
        return socket == null || socket.isClosed();
    }

    /**
     * Gets the target address.
     *
     * @return target InetAddress
     */
    public InetAddress getTargetAddress() {
        return targetAddress;
    }

    /**
     * Gets the target port.
     *
     * @return target port number
     */
    public int getTargetPort() {
        return targetPort;
    }
}
