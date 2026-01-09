package org.llled.ddp;

/**
 * Exception thrown when DDP operations fail.
 */
public class DdpException extends Exception {

    public DdpException(String message) {
        super(message);
    }

    public DdpException(String message, Throwable cause) {
        super(message, cause);
    }

    public DdpException(Throwable cause) {
        super(cause);
    }
}
