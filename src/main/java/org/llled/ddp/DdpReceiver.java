package org.llled.ddp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * UDP listener that receives DDP packets and reassembles multi-packet frames.
 *
 * Binds to a UDP port and runs a daemon thread that receives packets,
 * reassembles frames using the offset field, and delivers complete frames
 * when the PUSH flag is seen.
 *
 * Uses pre-allocated buffers for zero-copy style operation, consistent
 * with the library's architecture.
 *
 * Thread Safety: The listener callback is invoked on the receiver thread.
 */
public class DdpReceiver {

    private static final int DEFAULT_FRAME_BUFFER_SIZE = 512 * 1024; // 512KB default

    private final int port;
    private final DdpFrameListener listener;
    private final byte[] receiveBuffer;
    private final byte[] frameBuffer;

    private DatagramSocket socket;
    private Thread receiverThread;
    private volatile boolean running;

    private int frameDataLength;
    private byte currentDataType;

    /**
     * Creates a DDP receiver on the specified port.
     *
     * @param port UDP port to listen on
     * @param listener Callback for complete frames
     */
    public DdpReceiver(int port, DdpFrameListener listener) {
        this(port, listener, DEFAULT_FRAME_BUFFER_SIZE);
    }

    /**
     * Creates a DDP receiver with a custom frame buffer size.
     *
     * @param port UDP port to listen on
     * @param listener Callback for complete frames
     * @param frameBufferSize Maximum frame size in bytes
     */
    public DdpReceiver(int port, DdpFrameListener listener, int frameBufferSize) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        this.port = port;
        this.listener = listener;
        this.receiveBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        this.frameBuffer = new byte[frameBufferSize];
        this.frameDataLength = 0;
        this.currentDataType = DdpProtocol.TYPE_RGB_8BIT;
    }

    /**
     * Starts listening for DDP packets.
     *
     * @throws DdpException if the socket cannot be created
     */
    public void start() throws DdpException {
        if (running) {
            return;
        }

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new DdpException("Failed to bind UDP socket on port " + port, e);
        }

        running = true;
        receiverThread = new Thread(this::receiveLoop, "ddp-receiver-" + port);
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    /**
     * Stops listening and releases resources.
     */
    public void close() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (receiverThread != null) {
            receiverThread.interrupt();
            receiverThread = null;
        }
    }

    /**
     * Checks if the receiver is currently running.
     *
     * @return true if the receiver is actively listening
     */
    public boolean isRunning() {
        return running && socket != null && !socket.isClosed();
    }

    /**
     * Gets the port this receiver is bound to.
     *
     * @return UDP port number
     */
    public int getPort() {
        return port;
    }

    private void receiveLoop() {
        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);

        while (running) {
            try {
                socket.receive(packet);
                processPacket(receiveBuffer, packet.getLength());
            } catch (IOException e) {
                if (running) {
                    listener.onError(new DdpException("Error receiving DDP packet", e));
                }
                // If not running, socket was closed intentionally
            }
        }
    }

    void processPacket(byte[] buffer, int receivedLength) {
        try {
            DdpPacketDecoder.validatePacket(buffer, receivedLength);
        } catch (IllegalArgumentException e) {
            listener.onError(new DdpException("Invalid DDP packet: " + e.getMessage()));
            return;
        }

        int offset = DdpPacketDecoder.getOffset(buffer);
        int payloadLength = DdpPacketDecoder.getPayloadLength(buffer);
        boolean push = DdpPacketDecoder.isPush(buffer);
        byte dataType = DdpPacketDecoder.getDataType(buffer);

        // If offset is 0, this is the start of a new frame — reset
        if (offset == 0) {
            frameDataLength = 0;
        }

        currentDataType = dataType;

        // Copy payload into frame buffer at the correct offset
        int endPosition = offset + payloadLength;
        if (endPosition > frameBuffer.length) {
            listener.onError(new DdpException(
                    "Frame exceeds buffer size: " + endPosition + " > " + frameBuffer.length));
            return;
        }

        System.arraycopy(buffer, DdpProtocol.HEADER_LENGTH, frameBuffer, offset, payloadLength);

        // Track total frame data length
        if (endPosition > frameDataLength) {
            frameDataLength = endPosition;
        }

        // If PUSH flag is set, deliver the complete frame
        if (push) {
            listener.onFrameReceived(frameBuffer, frameDataLength, currentDataType);
            frameDataLength = 0;
        }
    }
}
