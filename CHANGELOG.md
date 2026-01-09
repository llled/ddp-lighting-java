# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-01-09

### Added
- Initial release of ddp-lighting-java library
- Core DDP protocol implementation
  - `DdpProtocol` - Protocol constants and specifications
  - `DdpException` - Typed exception for library errors
  - `DdpPacketEncoder` - Stateless packet encoding with zero-copy operations
  - `DdpClient` - UDP client with reusable buffer API
- RGB frame transmission with automatic packetization
- RGBW frame transmission support
- Generic `sendFrame()` method for custom pixel formats
- Zero-copy architecture with reusable buffers
- Thread-safe stateless packet encoding
- Automatic frame fragmentation for large pixel counts (>480 pixels)
- Comprehensive unit test suite (25 tests)
  - Protocol constant validation
  - Packet encoding/decoding tests
  - Buffer validation tests
  - Socket communication tests
  - Frame fragmentation tests
  - Error handling tests
- Complete API documentation with Javadoc
- README with usage examples and best practices
- Build system with Gradle
  - Java 17+ support
  - Maven publication support
  - JUnit 5 test framework
  - Hamcrest assertion library

### Technical Details
- No external dependencies (Java standard library only)
- MTU-friendly packet size (1450 bytes max)
- Big-endian byte order per DDP specification
- DDP protocol version 1.0 support
- Default port: 4048 (configurable)

### Performance
- Zero allocation in hot paths (when buffers are reused)
- Efficient packet encoding with direct buffer writes
- Minimal object creation per frame
- Suitable for real-time LED control (60+ FPS)

### Documentation
- Comprehensive README with:
  - Quick start examples
  - API reference
  - Performance guidelines
  - Threading considerations
  - Advanced usage patterns
- CONTRIBUTING.md with development guidelines
- CHANGELOG.md (this file)
- LICENSE (LGPL v3)

[Unreleased]: https://github.com/yourusername/ddp-lighting-java/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/yourusername/ddp-lighting-java/releases/tag/v1.0.0
