package org.llled.ddp;

/**
 * DDP (Distributed Display Protocol) constants and specifications.
 *
 * DDP is a simple protocol for controlling addressable LED pixels over UDP.
 * Protocol specification: http://www.3waylabs.com/ddp/
 */
public final class DdpProtocol {

    private DdpProtocol() {
        // Prevent instantiation - this is a constants-only class
    }

    // Network Configuration
    public static final int DEFAULT_PORT = 4048;
    public static final int HEADER_LENGTH = 10;
    public static final int MAX_PAYLOAD_LENGTH = 1440; // MTU-friendly size
    public static final int MAX_PACKET_SIZE = HEADER_LENGTH + MAX_PAYLOAD_LENGTH;

    // Protocol Version & Flags (byte 0)
    public static final byte VERSION_1 = (byte) 0x40;  // version=1
    public static final byte FLAG_PUSH = (byte) 0x01;   // Push flag - display this data immediately
    public static final byte FLAG_QUERY = (byte) 0x02;  // Query flag
    public static final byte FLAG_REPLY = (byte) 0x04;  // Reply flag
    public static final byte FLAG_STORAGE = (byte) 0x08; // Storage flag
    public static final byte FLAG_TIME = (byte) 0x10;   // Timecode flag

    // Destination IDs (byte 3)
    public static final byte ID_DISPLAY = 1;           // Display data destination
    public static final byte ID_CONFIG = (byte) 250;   // Configuration destination
    public static final byte ID_STATUS = (byte) 251;   // Status destination

    // Data Types (byte 2): TTT=type, SSS=size
    // TTT: 001=RGB/RGBA, 010=HSL, 011=HSV, 100=RGBW
    // SSS: 000=default, 001=4-bit, 010=5-bit, 011=8-bit, 100=16-bit, 101=24-bit, 110=32-bit
    public static final byte TYPE_RGB_8BIT = (byte) 0x0B;   // TTT=001 (RGB), SSS=011 (8-bit)
    public static final byte TYPE_RGBA_8BIT = (byte) 0x0C;  // TTT=001 (RGB), SSS=100 (8-bit + alpha)

    // Bytes per pixel for common data types
    public static final int BYTES_PER_PIXEL_RGB = 3;
    public static final int BYTES_PER_PIXEL_RGBA = 4;
}
