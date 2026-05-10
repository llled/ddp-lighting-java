package org.llled.ddp;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DdpPacketDecoderTest {

    @Test
    void testGetFlags_VersionAndPush() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(buffer, 0, 480, true,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        byte flags = DdpPacketDecoder.getFlags(buffer);

        assertThat("Should have version 1 and push flag",
                flags, equalTo((byte) 0x41));
    }

    @Test
    void testIsPush_WhenSet() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(buffer, 0, 480, true,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        assertThat("Push should be true", DdpPacketDecoder.isPush(buffer), is(true));
    }

    @Test
    void testIsPush_WhenNotSet() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(buffer, 0, 480, false,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        assertThat("Push should be false", DdpPacketDecoder.isPush(buffer), is(false));
    }

    @Test
    void testGetDataType() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(buffer, 0, 480, true,
                DdpProtocol.TYPE_RGBA_8BIT, DdpProtocol.ID_DISPLAY);

        assertThat("Data type should be RGBA 8-bit",
                DdpPacketDecoder.getDataType(buffer), equalTo(DdpProtocol.TYPE_RGBA_8BIT));
    }

    @Test
    void testGetDestination() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(buffer, 0, 480, true,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_CONFIG);

        assertThat("Destination should be config",
                DdpPacketDecoder.getDestination(buffer), equalTo(DdpProtocol.ID_CONFIG));
    }

    @Test
    void testGetOffset_Zero() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(buffer, 0, 480, true,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        assertThat("Offset should be 0", DdpPacketDecoder.getOffset(buffer), equalTo(0));
    }

    @Test
    void testGetOffset_NonZero() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(buffer, 1440, 480, false,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        assertThat("Offset should be 1440", DdpPacketDecoder.getOffset(buffer), equalTo(1440));
    }

    @Test
    void testGetOffset_LargeValue() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        int largeOffset = 100000;
        DdpPacketEncoder.encodeHeader(buffer, largeOffset, 480, false,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        assertThat("Offset should be 100000", DdpPacketDecoder.getOffset(buffer), equalTo(largeOffset));
    }

    @Test
    void testGetPayloadLength() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(buffer, 0, 480, true,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        assertThat("Payload length should be 480",
                DdpPacketDecoder.getPayloadLength(buffer), equalTo(480));
    }

    @Test
    void testGetPayloadLength_MaxPayload() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(buffer, 0, DdpProtocol.MAX_PAYLOAD_LENGTH, true,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        assertThat("Payload length should be max",
                DdpPacketDecoder.getPayloadLength(buffer),
                equalTo(DdpProtocol.MAX_PAYLOAD_LENGTH));
    }

    @Test
    void testValidatePacket_Valid() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(buffer, 0, 480, true,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        // Should not throw
        DdpPacketDecoder.validatePacket(buffer, DdpProtocol.HEADER_LENGTH + 480);
    }

    @Test
    void testValidatePacket_NullBuffer() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                DdpPacketDecoder.validatePacket(null, 100)
        );
        assertThat(exception.getMessage(), containsString("cannot be null"));
    }

    @Test
    void testValidatePacket_TooShort() {
        byte[] buffer = new byte[5];

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                DdpPacketDecoder.validatePacket(buffer, 5)
        );
        assertThat(exception.getMessage(), containsString("too short"));
    }

    @Test
    void testValidatePacket_Truncated() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(buffer, 0, 480, true,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        // Say we only received the header, but header declares 480 payload bytes
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                DdpPacketDecoder.validatePacket(buffer, DdpProtocol.HEADER_LENGTH)
        );
        assertThat(exception.getMessage(), containsString("truncated"));
    }

    @Test
    void testRoundTrip_EncoderDecoder() {
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        int offset = 2880;
        int payloadLength = 720;
        byte dataType = DdpProtocol.TYPE_RGB_8BIT;
        byte destination = DdpProtocol.ID_DISPLAY;

        DdpPacketEncoder.encodeHeader(buffer, offset, payloadLength, true, dataType, destination);

        assertThat(DdpPacketDecoder.isPush(buffer), is(true));
        assertThat(DdpPacketDecoder.getOffset(buffer), equalTo(offset));
        assertThat(DdpPacketDecoder.getPayloadLength(buffer), equalTo(payloadLength));
        assertThat(DdpPacketDecoder.getDataType(buffer), equalTo(dataType));
        assertThat(DdpPacketDecoder.getDestination(buffer), equalTo(destination));
    }
}
