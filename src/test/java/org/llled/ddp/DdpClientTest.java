package org.llled.ddp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DdpClientTest {

    private DdpClient client;

    @AfterEach
    void cleanup() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testConstructor_ValidHostname() throws DdpException {
        client = new DdpClient("localhost");

        assertThat("Client should not be closed", client.isClosed(), is(false));
        assertThat("Target port should be default", client.getTargetPort(), equalTo(DdpProtocol.DEFAULT_PORT));
        assertThat("Target address should be set", client.getTargetAddress(), is(notNullValue()));
    }

    @Test
    void testConstructor_WithCustomPort() throws DdpException {
        client = new DdpClient("localhost", 5000);

        assertThat("Client should not be closed", client.isClosed(), is(false));
        assertThat("Target port should be 5000", client.getTargetPort(), equalTo(5000));
    }

    @Test
    void testConstructor_InvalidHostname() {
        // Use an invalid IP format that will definitely fail
        Exception exception = assertThrows(DdpException.class, () ->
                new DdpClient("999.999.999.999")
        );

        assertThat(exception.getMessage(), containsString("Invalid hostname"));
    }

    @Test
    void testClose_ClosesSocket() throws DdpException {
        client = new DdpClient("localhost");
        assertThat("Client should not be closed initially", client.isClosed(), is(false));

        client.close();

        assertThat("Client should be closed after close()", client.isClosed(), is(true));
    }

    @Test
    void testClose_Idempotent() throws DdpException {
        client = new DdpClient("localhost");
        client.close();
        client.close(); // Should not throw

        assertThat("Client should remain closed", client.isClosed(), is(true));
    }

    @Test
    void testSendRgbFrame_SmallFrame() throws DdpException {
        client = new DdpClient("localhost");

        // Create small RGB frame (10 pixels)
        int pixelCount = 10;
        byte[] rgbData = new byte[pixelCount * DdpProtocol.BYTES_PER_PIXEL_RGB];
        for (int i = 0; i < rgbData.length; i++) {
            rgbData[i] = (byte) (i % 256);
        }

        byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        // Should not throw (sending to localhost)
        client.sendRgbFrame(rgbData, pixelCount, packetBuffer);
    }

    @Test
    void testSendRgbFrame_LargeFrame_MultiplePackets() throws DdpException {
        client = new DdpClient("localhost");

        // Create large RGB frame that requires multiple packets (500 pixels)
        int pixelCount = 500;
        byte[] rgbData = new byte[pixelCount * DdpProtocol.BYTES_PER_PIXEL_RGB];
        for (int i = 0; i < rgbData.length; i++) {
            rgbData[i] = (byte) (i % 256);
        }

        byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        // Should not throw (automatically fragments into multiple packets)
        client.sendRgbFrame(rgbData, pixelCount, packetBuffer);
    }

    @Test
    void testSendRgbFrame_BufferTooSmall() throws DdpException {
        client = new DdpClient("localhost");

        int pixelCount = 10;
        byte[] rgbData = new byte[pixelCount * DdpProtocol.BYTES_PER_PIXEL_RGB];
        byte[] packetBuffer = new byte[100]; // Too small

        Exception exception = assertThrows(DdpException.class, () ->
                client.sendRgbFrame(rgbData, pixelCount, packetBuffer)
        );

        assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
    }

    @Test
    void testSendRgbFrame_RgbDataTooSmall() throws DdpException {
        client = new DdpClient("localhost");

        int pixelCount = 100;
        byte[] rgbData = new byte[50]; // Too small for 100 pixels
        byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        Exception exception = assertThrows(DdpException.class, () ->
                client.sendRgbFrame(rgbData, pixelCount, packetBuffer)
        );

        assertThat(exception.getMessage(), containsString("buffer too small"));
    }

    @Test
    void testSendPacket_AfterClose() throws DdpException {
        client = new DdpClient("localhost");
        byte[] buffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
        client.close();

        Exception exception = assertThrows(DdpException.class, () ->
                client.sendPacket(buffer, 100)
        );

        assertThat(exception.getMessage(), containsString("Failed to send"));
    }

    @Test
    void testSendRgbFrame_ZeroPixels() throws DdpException {
        client = new DdpClient("localhost");

        byte[] rgbData = new byte[0];
        byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        // Should handle zero pixels gracefully (sends empty frame)
        client.sendRgbFrame(rgbData, 0, packetBuffer);
    }

    @Test
    void testGetTargetAddress_ReturnsCorrectAddress() throws DdpException {
        client = new DdpClient("127.0.0.1");

        assertThat("Target address should be localhost",
                client.getTargetAddress().getHostAddress(), equalTo("127.0.0.1"));
    }
}
