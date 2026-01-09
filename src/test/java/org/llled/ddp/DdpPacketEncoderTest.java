package org.llled.ddp;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DdpPacketEncoderTest {

    @Test
    void testEncodeHeader_BasicPacket() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        DdpPacketEncoder.encodeHeader(
                buffer,
                0,      // offset
                480,    // payload (160 pixels RGB)
                true,   // push
                DdpProtocol.TYPE_RGB_8BIT,
                DdpProtocol.ID_DISPLAY
        );

        // Verify version + push flag (0x40 | 0x01 = 0x41)
        assertThat("Byte 0 should have version and push flag",
                buffer[0], equalTo((byte) 0x41));

        // Verify sequence number
        assertThat("Byte 1 should be sequence (0)",
                buffer[1], equalTo((byte) 0));

        // Verify data type
        assertThat("Byte 2 should be RGB 8-bit type",
                buffer[2], equalTo(DdpProtocol.TYPE_RGB_8BIT));

        // Verify destination ID
        assertThat("Byte 3 should be display ID",
                buffer[3], equalTo(DdpProtocol.ID_DISPLAY));

        // Verify offset (big-endian) - should be 0
        int decodedOffset = ((buffer[4] & 0xFF) << 24) |
                ((buffer[5] & 0xFF) << 16) |
                ((buffer[6] & 0xFF) << 8) |
                (buffer[7] & 0xFF);
        assertThat("Offset should be 0", decodedOffset, equalTo(0));

        // Verify payload length (big-endian)
        int length = ((buffer[8] & 0xFF) << 8) | (buffer[9] & 0xFF);
        assertThat("Length should be 480", length, equalTo(480));
    }

    @Test
    void testEncodeHeader_WithoutPushFlag() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        DdpPacketEncoder.encodeHeader(
                buffer,
                0,
                100,
                false,  // no push
                DdpProtocol.TYPE_RGB_8BIT,
                DdpProtocol.ID_DISPLAY
        );

        // Verify version without push flag (0x40)
        assertThat("Byte 0 should have version only",
                buffer[0], equalTo((byte) 0x40));
    }

    @Test
    void testEncodeHeader_WithNonZeroOffset() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        int testOffset = 1440; // One full packet worth of data

        DdpPacketEncoder.encodeHeader(
                buffer,
                testOffset,
                480,
                false,
                DdpProtocol.TYPE_RGB_8BIT,
                DdpProtocol.ID_DISPLAY
        );

        // Verify offset (big-endian)
        int decodedOffset = ((buffer[4] & 0xFF) << 24) |
                ((buffer[5] & 0xFF) << 16) |
                ((buffer[6] & 0xFF) << 8) |
                (buffer[7] & 0xFF);
        assertThat("Offset should be 1440", decodedOffset, equalTo(1440));
    }

    @Test
    void testEncodeHeader_MaxPayload() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        DdpPacketEncoder.encodeHeader(
                buffer,
                0,
                DdpProtocol.MAX_PAYLOAD_LENGTH,
                true,
                DdpProtocol.TYPE_RGB_8BIT,
                DdpProtocol.ID_DISPLAY
        );

        // Verify max payload length
        int length = ((buffer[8] & 0xFF) << 8) | (buffer[9] & 0xFF);
        assertThat("Length should be max payload",
                length, equalTo(DdpProtocol.MAX_PAYLOAD_LENGTH));
    }

    @Test
    void testEncodeRgbPixel_CorrectByteOrder() {
        byte[] buffer = new byte[3];
        int color = 0xFF8040; // R=255, G=128, B=64

        DdpPacketEncoder.encodeRgbPixel(buffer, 0, color);

        assertThat("R byte should be 255", buffer[0] & 0xFF, equalTo(255));
        assertThat("G byte should be 128", buffer[1] & 0xFF, equalTo(128));
        assertThat("B byte should be 64", buffer[2] & 0xFF, equalTo(64));
    }

    @Test
    void testEncodeRgbPixel_BlackColor() {
        byte[] buffer = new byte[3];
        int color = 0x000000;

        DdpPacketEncoder.encodeRgbPixel(buffer, 0, color);

        assertThat("R should be 0", buffer[0], equalTo((byte) 0));
        assertThat("G should be 0", buffer[1], equalTo((byte) 0));
        assertThat("B should be 0", buffer[2], equalTo((byte) 0));
    }

    @Test
    void testEncodeRgbPixel_WhiteColor() {
        byte[] buffer = new byte[3];
        int color = 0xFFFFFF;

        DdpPacketEncoder.encodeRgbPixel(buffer, 0, color);

        assertThat("R should be 255", buffer[0] & 0xFF, equalTo(255));
        assertThat("G should be 255", buffer[1] & 0xFF, equalTo(255));
        assertThat("B should be 255", buffer[2] & 0xFF, equalTo(255));
    }

    @Test
    void testValidatePacketBuffer_ValidBuffer() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        // Should not throw
        DdpPacketEncoder.validatePacketBuffer(buffer, 480);
    }

    @Test
    void testValidatePacketBuffer_NullBuffer() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                DdpPacketEncoder.validatePacketBuffer(null, 100)
        );

        assertThat(exception.getMessage(), containsString("cannot be null"));
    }

    @Test
    void testValidatePacketBuffer_BufferTooSmall() {
        byte[] buffer = new byte[100];

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                DdpPacketEncoder.validatePacketBuffer(buffer, 200)
        );

        assertThat(exception.getMessage(), containsString("too small"));
    }

    @Test
    void testValidatePacketBuffer_PayloadTooLarge() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE * 2];

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                DdpPacketEncoder.validatePacketBuffer(buffer, DdpProtocol.MAX_PAYLOAD_LENGTH + 1)
        );

        assertThat(exception.getMessage(), containsString("too large"));
    }

    @Test
    void testValidatePacketBuffer_NegativePayload() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                DdpPacketEncoder.validatePacketBuffer(buffer, -1)
        );

        assertThat(exception.getMessage(), containsString("cannot be negative"));
    }

    @Test
    void testValidatePacketBuffer_ZeroPayload() {
        byte[] buffer = new byte[DdpProtocol.HEADER_LENGTH];

        // Should not throw - zero payload is valid
        DdpPacketEncoder.validatePacketBuffer(buffer, 0);
    }
}
