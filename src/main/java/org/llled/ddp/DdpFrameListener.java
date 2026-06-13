package org.llled.ddp;

import java.net.InetAddress;

/**
 * Callback interface for receiving complete DDP frames.
 *
 * Implementations are called on the receiver's listener thread. Keep
 * processing fast to avoid blocking subsequent frame delivery.
 */
public interface DdpFrameListener {

    /**
     * Called when a complete frame has been reassembled.
     *
     * @param frameData Buffer containing the frame pixel data
     * @param dataLength Number of valid bytes in frameData
     * @param dataType DDP data type (e.g., TYPE_RGB_8BIT)
     */
    void onFrameReceived(byte[] frameData, int dataLength, byte dataType);

    /**
     * Called when a complete frame has been reassembled, with the address the
     * packets were received from.
     *
     * <p>The default implementation discards the source and delegates to
     * {@link #onFrameReceived(byte[], int, byte)}, so existing listeners keep
     * working unchanged. Override this method instead when you need to know
     * which host sent the frame.
     *
     * @param frameData Buffer containing the frame pixel data
     * @param dataLength Number of valid bytes in frameData
     * @param dataType DDP data type (e.g., TYPE_RGB_8BIT)
     * @param source address of the host that sent the frame (may be {@code null}
     *               if delivered through a path that does not carry it)
     */
    default void onFrameReceived(byte[] frameData, int dataLength, byte dataType, InetAddress source) {
        onFrameReceived(frameData, dataLength, dataType);
    }

    /**
     * Called when an error occurs during reception.
     * Default implementation does nothing.
     *
     * @param e the exception that occurred
     */
    default void onError(DdpException e) {
        // optional
    }
}
