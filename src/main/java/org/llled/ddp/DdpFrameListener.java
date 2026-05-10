package org.llled.ddp;

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
     * Called when an error occurs during reception.
     * Default implementation does nothing.
     *
     * @param e the exception that occurred
     */
    default void onError(DdpException e) {
        // optional
    }
}
