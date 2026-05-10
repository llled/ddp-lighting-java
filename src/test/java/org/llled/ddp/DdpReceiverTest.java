package org.llled.ddp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DdpReceiverTest {

    private DdpReceiver receiver;
    private DdpClient client;

    @AfterEach
    void cleanup() {
        if (receiver != null) {
            receiver.close();
        }
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testConstructor_NullListener() {
        assertThrows(IllegalArgumentException.class, () ->
                new DdpReceiver(4048, null)
        );
    }

    @Test
    void testStartAndClose() throws DdpException {
        receiver = new DdpReceiver(0, (data, len, type) -> {});
        receiver.start();

        assertThat("Receiver should be running", receiver.isRunning(), is(true));

        receiver.close();

        assertThat("Receiver should not be running after close", receiver.isRunning(), is(false));
    }

    @Test
    void testStartIdempotent() throws DdpException {
        receiver = new DdpReceiver(0, (data, len, type) -> {});
        receiver.start();
        receiver.start(); // Should not throw

        assertThat("Receiver should be running", receiver.isRunning(), is(true));
    }

    @Test
    void testCloseIdempotent() throws DdpException {
        receiver = new DdpReceiver(0, (data, len, type) -> {});
        receiver.start();
        receiver.close();
        receiver.close(); // Should not throw

        assertThat("Receiver should not be running", receiver.isRunning(), is(false));
    }

    @Test
    void testProcessPacket_SinglePacketFrame() {
        AtomicReference<byte[]> receivedData = new AtomicReference<>();
        AtomicInteger receivedLength = new AtomicInteger();
        AtomicReference<Byte> receivedType = new AtomicReference<>();

        receiver = new DdpReceiver(0, (data, len, type) -> {
            byte[] copy = new byte[len];
            System.arraycopy(data, 0, copy, 0, len);
            receivedData.set(copy);
            receivedLength.set(len);
            receivedType.set(type);
        });

        // Build a single-packet frame with PUSH
        byte[] packet = new byte[DdpProtocol.MAX_PACKET_SIZE];
        int pixelCount = 10;
        int payloadLength = pixelCount * DdpProtocol.BYTES_PER_PIXEL_RGB;
        DdpPacketEncoder.encodeHeader(packet, 0, payloadLength, true,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        // Fill payload with test pattern
        for (int i = 0; i < payloadLength; i++) {
            packet[DdpProtocol.HEADER_LENGTH + i] = (byte) (i % 256);
        }

        receiver.processPacket(packet, DdpProtocol.HEADER_LENGTH + payloadLength);

        assertThat("Should have received data", receivedData.get(), is(notNullValue()));
        assertThat("Data length should match", receivedLength.get(), equalTo(payloadLength));
        assertThat("Data type should be RGB", receivedType.get(), equalTo(DdpProtocol.TYPE_RGB_8BIT));

        // Verify pixel data
        for (int i = 0; i < payloadLength; i++) {
            assertThat("Pixel data byte " + i,
                    receivedData.get()[i], equalTo((byte) (i % 256)));
        }
    }

    @Test
    void testProcessPacket_MultiPacketFrame() {
        AtomicReference<byte[]> receivedData = new AtomicReference<>();
        AtomicInteger receivedLength = new AtomicInteger();

        receiver = new DdpReceiver(0, (data, len, type) -> {
            byte[] copy = new byte[len];
            System.arraycopy(data, 0, copy, 0, len);
            receivedData.set(copy);
            receivedLength.set(len);
        });

        // First packet: offset=0, no push
        byte[] packet1 = new byte[DdpProtocol.MAX_PACKET_SIZE];
        int payload1Length = DdpProtocol.MAX_PAYLOAD_LENGTH;
        DdpPacketEncoder.encodeHeader(packet1, 0, payload1Length, false,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);
        for (int i = 0; i < payload1Length; i++) {
            packet1[DdpProtocol.HEADER_LENGTH + i] = (byte) 0xAA;
        }

        receiver.processPacket(packet1, DdpProtocol.HEADER_LENGTH + payload1Length);

        // Should not have delivered frame yet
        assertThat("Frame should not be delivered yet", receivedData.get(), is(nullValue()));

        // Second packet: offset=1440, push
        byte[] packet2 = new byte[DdpProtocol.MAX_PACKET_SIZE];
        int payload2Length = 480;
        DdpPacketEncoder.encodeHeader(packet2, payload1Length, payload2Length, true,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);
        for (int i = 0; i < payload2Length; i++) {
            packet2[DdpProtocol.HEADER_LENGTH + i] = (byte) 0xBB;
        }

        receiver.processPacket(packet2, DdpProtocol.HEADER_LENGTH + payload2Length);

        // Now frame should be delivered
        assertThat("Frame should be delivered", receivedData.get(), is(notNullValue()));
        assertThat("Total length should be sum of payloads",
                receivedLength.get(), equalTo(payload1Length + payload2Length));

        // Verify first part
        assertThat("First byte should be 0xAA",
                receivedData.get()[0], equalTo((byte) 0xAA));
        assertThat("Last byte of first packet should be 0xAA",
                receivedData.get()[payload1Length - 1], equalTo((byte) 0xAA));

        // Verify second part
        assertThat("First byte of second part should be 0xBB",
                receivedData.get()[payload1Length], equalTo((byte) 0xBB));
        assertThat("Last byte should be 0xBB",
                receivedData.get()[payload1Length + payload2Length - 1], equalTo((byte) 0xBB));
    }

    @Test
    void testProcessPacket_InvalidPacket() {
        AtomicReference<DdpException> receivedError = new AtomicReference<>();

        receiver = new DdpReceiver(0, new DdpFrameListener() {
            @Override
            public void onFrameReceived(byte[] frameData, int dataLength, byte dataType) {}

            @Override
            public void onError(DdpException e) {
                receivedError.set(e);
            }
        });

        // Send packet that's too short
        byte[] shortPacket = new byte[5];
        receiver.processPacket(shortPacket, 5);

        assertThat("Error should have been reported", receivedError.get(), is(notNullValue()));
        assertThat("Error message should mention invalid",
                receivedError.get().getMessage(), containsString("Invalid"));
    }

    @Test
    void testProcessPacket_FrameExceedsBuffer() {
        AtomicReference<DdpException> receivedError = new AtomicReference<>();

        // Create receiver with very small frame buffer
        receiver = new DdpReceiver(0, new DdpFrameListener() {
            @Override
            public void onFrameReceived(byte[] frameData, int dataLength, byte dataType) {}

            @Override
            public void onError(DdpException e) {
                receivedError.set(e);
            }
        }, 100); // Only 100 bytes frame buffer

        // Send packet with payload that would exceed frame buffer
        byte[] packet = new byte[DdpProtocol.MAX_PACKET_SIZE];
        DdpPacketEncoder.encodeHeader(packet, 0, 200, true,
                DdpProtocol.TYPE_RGB_8BIT, DdpProtocol.ID_DISPLAY);

        receiver.processPacket(packet, DdpProtocol.HEADER_LENGTH + 200);

        assertThat("Error should have been reported", receivedError.get(), is(notNullValue()));
        assertThat("Error message should mention buffer size",
                receivedError.get().getMessage(), containsString("exceeds buffer"));
    }

    @Test
    void testIntegration_SendAndReceive() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> receivedData = new AtomicReference<>();
        AtomicInteger receivedLength = new AtomicInteger();

        // Start receiver on a random port
        receiver = new DdpReceiver(0, (data, len, type) -> {
            byte[] copy = new byte[len];
            System.arraycopy(data, 0, copy, 0, len);
            receivedData.set(copy);
            receivedLength.set(len);
            latch.countDown();
        });
        receiver.start();

        // DdpReceiver binds to port 0 (OS-assigned), but we need the actual port.
        // For this test, we'll use processPacket directly since we can't easily
        // get the OS-assigned port from DatagramSocket without exposing it.
        // Instead, let's test the real UDP path with a known port.

        // Actually, let's test using a specific port
        receiver.close();

        // Find a free port by briefly opening and closing a socket
        java.net.DatagramSocket tempSocket = new java.net.DatagramSocket(0);
        int freePort = tempSocket.getLocalPort();
        tempSocket.close();

        receiver = new DdpReceiver(freePort, (data, len, type) -> {
            byte[] copy = new byte[len];
            System.arraycopy(data, 0, copy, 0, len);
            receivedData.set(copy);
            receivedLength.set(len);
            latch.countDown();
        });
        receiver.start();

        // Create client targeting the receiver
        client = new DdpClient("127.0.0.1", freePort);

        // Send a small RGB frame
        int pixelCount = 10;
        byte[] rgbData = new byte[pixelCount * DdpProtocol.BYTES_PER_PIXEL_RGB];
        for (int i = 0; i < rgbData.length; i++) {
            rgbData[i] = (byte) ((i + 1) % 256);
        }
        byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        client.sendRgbFrame(rgbData, pixelCount, packetBuffer);

        // Wait for frame to arrive
        boolean received = latch.await(5, TimeUnit.SECONDS);

        assertThat("Frame should have been received within timeout", received, is(true));
        assertThat("Received data should not be null", receivedData.get(), is(notNullValue()));
        assertThat("Received length should match", receivedLength.get(),
                equalTo(pixelCount * DdpProtocol.BYTES_PER_PIXEL_RGB));

        // Verify pixel data integrity
        for (int i = 0; i < rgbData.length; i++) {
            assertThat("Pixel byte " + i + " should match",
                    receivedData.get()[i], equalTo(rgbData[i]));
        }
    }

    @Test
    void testIntegration_LargeFrame_MultiPacket() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> receivedData = new AtomicReference<>();
        AtomicInteger receivedLength = new AtomicInteger();

        java.net.DatagramSocket tempSocket = new java.net.DatagramSocket(0);
        int freePort = tempSocket.getLocalPort();
        tempSocket.close();

        receiver = new DdpReceiver(freePort, (data, len, type) -> {
            byte[] copy = new byte[len];
            System.arraycopy(data, 0, copy, 0, len);
            receivedData.set(copy);
            receivedLength.set(len);
            latch.countDown();
        });
        receiver.start();

        client = new DdpClient("127.0.0.1", freePort);

        // Send a large frame that requires multiple packets (500 pixels = 1500 bytes > 1440 max payload)
        int pixelCount = 500;
        byte[] rgbData = new byte[pixelCount * DdpProtocol.BYTES_PER_PIXEL_RGB];
        for (int i = 0; i < rgbData.length; i++) {
            rgbData[i] = (byte) (i % 256);
        }
        byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        client.sendRgbFrame(rgbData, pixelCount, packetBuffer);

        boolean received = latch.await(5, TimeUnit.SECONDS);

        assertThat("Large frame should have been received", received, is(true));
        assertThat("Received length should match",
                receivedLength.get(), equalTo(pixelCount * DdpProtocol.BYTES_PER_PIXEL_RGB));

        // Verify data integrity
        for (int i = 0; i < rgbData.length; i++) {
            assertThat("Byte " + i + " should match",
                    receivedData.get()[i], equalTo(rgbData[i]));
        }
    }
}
