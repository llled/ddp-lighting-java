# ddp-lighting-java

A lightweight Java library for DDP (Distributed Display Protocol) used to control addressable LED pixels over UDP.

## Features

- **Zero-copy architecture** - Reusable buffers minimize allocation overhead
- **Thread-safe** - Stateless encoders and per-instance clients
- **Simple API** - Send RGB/RGBW frames with minimal boilerplate
- **Automatic packetization** - Handles frame fragmentation for large pixel counts
- **Well-tested** - Comprehensive unit test coverage
- **No dependencies** - Uses only Java standard library

## What is DDP?

DDP (Distributed Display Protocol) is a simple protocol for controlling addressable LED pixels over UDP. It's designed to be lightweight and easy to implement, making it ideal for real-time LED control applications.

Protocol specification: http://www.3waylabs.com/ddp/

## Installation

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>org.llled</groupId>
    <artifactId>ddp-lighting-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'org.llled:ddp-lighting-java:0.1.0'
}
```

## Quick Start

### Basic RGB Example

```java
import org.llled.ddp.DdpClient;
import org.llled.ddp.DdpProtocol;
import org.llled.ddp.DdpException;

public class Example {
    public static void main(String[] args) throws DdpException {
        // Create client for target device
        DdpClient client = new DdpClient("192.168.1.100");

        // Prepare RGB data (3 bytes per pixel)
        int pixelCount = 100;
        byte[] rgbData = new byte[pixelCount * 3];

        // Fill with red color
        for (int i = 0; i < pixelCount; i++) {
            rgbData[i * 3] = (byte) 255;     // R
            rgbData[i * 3 + 1] = (byte) 0;   // G
            rgbData[i * 3 + 2] = (byte) 0;   // B
        }

        // Reusable packet buffer for zero-copy operation
        byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

        // Send frame
        client.sendRgbFrame(rgbData, pixelCount, packetBuffer);

        // Clean up
        client.close();
    }
}
```

### Animation Loop

```java
DdpClient client = new DdpClient("192.168.1.100");
byte[] rgbData = new byte[100 * 3];
byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

try {
    // Animation loop
    for (int frame = 0; frame < 1000; frame++) {
        // Update pixel colors
        for (int i = 0; i < 100; i++) {
            rgbData[i * 3] = (byte) ((frame + i) % 256);     // R
            rgbData[i * 3 + 1] = (byte) ((frame * 2) % 256); // G
            rgbData[i * 3 + 2] = (byte) ((frame * 3) % 256); // B
        }

        // Send frame (automatically handles fragmentation)
        client.sendRgbFrame(rgbData, 100, packetBuffer);

        // Frame rate control
        Thread.sleep(16); // ~60 FPS
    }
} finally {
    client.close();
}
```

## API Documentation

### DdpClient

Main client class for sending DDP frames.

#### Constructors

```java
// Connect to device on default port (4048)
DdpClient client = new DdpClient("hostname");

// Connect to device on custom port
DdpClient client = new DdpClient("hostname", 5000);
```

#### Methods

**`sendRgbFrame(byte[] rgbData, int pixelCount, byte[] packetBuffer)`**

Sends RGB frame (3 bytes per pixel). Automatically fragments into multiple packets if needed.

- `rgbData` - RGB pixel data (length must be >= pixelCount * 3)
- `pixelCount` - Number of pixels to send
- `packetBuffer` - Reusable buffer (min size: `DdpProtocol.MAX_PACKET_SIZE`)

**`sendRgbwFrame(byte[] rgbwData, int pixelCount, byte[] packetBuffer)`**

Sends RGBW frame (4 bytes per pixel).

**`sendFrame(byte[] pixelData, int pixelCount, int bytesPerPixel, byte dataType, byte[] packetBuffer)`**

Generic method for sending any pixel format.

**`sendPacket(byte[] buffer, int length)`**

Low-level method to send raw DDP packet (pre-encoded header + payload).

**`close()`**

Closes UDP socket and releases resources.

**`isClosed()`**

Returns true if socket is closed.

### DdpProtocol

Protocol constants class.

```java
DdpProtocol.DEFAULT_PORT           // 4048
DdpProtocol.HEADER_LENGTH          // 10 bytes
DdpProtocol.MAX_PAYLOAD_LENGTH     // 1440 bytes
DdpProtocol.MAX_PACKET_SIZE        // 1450 bytes

DdpProtocol.TYPE_RGB_8BIT          // RGB data type constant
DdpProtocol.TYPE_RGBA_8BIT         // RGBA data type constant

DdpProtocol.BYTES_PER_PIXEL_RGB    // 3
DdpProtocol.BYTES_PER_PIXEL_RGBA   // 4
```

### DdpPacketEncoder

Static utility class for low-level packet encoding.

```java
// Encode DDP header
DdpPacketEncoder.encodeHeader(buffer, offset, payloadLength, push, dataType, destination);

// Encode RGB pixel into buffer
DdpPacketEncoder.encodeRgbPixel(buffer, position, rgbColor);

// Validate packet buffer
DdpPacketEncoder.validatePacketBuffer(buffer, payloadLength);
```

### DdpException

Exception thrown when DDP operations fail (extends `Exception`).

## Advanced Usage

### Custom Pixel Formats

```java
// Send HSV data (example - requires custom data type)
byte dataType = (byte) 0x13; // HSV 8-bit
int bytesPerPixel = 3;

client.sendFrame(hsvData, pixelCount, bytesPerPixel, dataType, packetBuffer);
```

### Multi-Device Setup

```java
DdpClient strip1 = new DdpClient("192.168.1.100");
DdpClient strip2 = new DdpClient("192.168.1.101");

byte[] strip1Data = new byte[100 * 3];
byte[] strip2Data = new byte[200 * 3];
byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

// Update both strips
strip1.sendRgbFrame(strip1Data, 100, packetBuffer);
strip2.sendRgbFrame(strip2Data, 200, packetBuffer);
```

## Performance Considerations

### Buffer Reuse

Always reuse buffers in animation loops to avoid allocation overhead:

```java
// Good - buffers allocated once
byte[] rgbData = new byte[pixelCount * 3];
byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];
while (animating) {
    updatePixels(rgbData);
    client.sendRgbFrame(rgbData, pixelCount, packetBuffer);
}

// Bad - allocates every frame
while (animating) {
    byte[] rgbData = new byte[pixelCount * 3]; // DON'T DO THIS
    byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE]; // DON'T DO THIS
    updatePixels(rgbData);
    client.sendRgbFrame(rgbData, pixelCount, packetBuffer);
}
```

### Threading

Each `DdpClient` instance should be used by a single thread, or external synchronization must be provided.

```java
// Safe - one client per thread
ExecutorService executor = Executors.newFixedThreadPool(2);

executor.execute(() -> {
    DdpClient client1 = new DdpClient("device1");
    // ... use client1
});

executor.execute(() -> {
    DdpClient client2 = new DdpClient("device2");
    // ... use client2
});
```

## Building from Source

```bash
# Clone repository
git clone https://github.com/yourusername/ddp-lighting-java.git
cd ddp-lighting-java

# Build
./gradlew build

# Run tests
./gradlew test

# Publish to local Maven repository
./gradlew publishToMavenLocal
```

## Requirements

- Java 17 or higher
- No external dependencies (uses only Java standard library)

## Testing

The library includes comprehensive unit tests:

```bash
./gradlew test
```

Test coverage includes:
- Protocol constant verification
- Packet encoding/decoding
- Buffer validation
- Socket creation and communication
- Frame fragmentation
- Error handling

## License

[LGPL 3.0](https://www.gnu.org/licenses/lgpl-3.0.txt)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For bugs and feature requests, please open an issue on GitHub.

## Acknowledgments

DDP Protocol specification: http://www.3waylabs.com/ddp/
