# Contributing to ddp-lighting-java

Thank you for your interest in contributing to ddp-lighting-java! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [License](#license)

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment for all contributors.

## Getting Started

### Prerequisites

- Java 17 or higher
- Git
- Gradle (wrapper included in project)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR-USERNAME/ddp-lighting-java.git
   cd ddp-lighting-java
   ```
3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/ORIGINAL-OWNER/ddp-lighting-java.git
   ```

## Development Setup

1. Build the project:
   ```bash
   ./gradlew build
   ```

2. Run tests:
   ```bash
   ./gradlew test
   ```

3. Verify everything works:
   ```bash
   ./gradlew check
   ```

4. Publish to local Maven repo to use your changes in other local code.
   ```bash
   ./gradlew publishToMavenLocal
   ```

## Making Changes

### Branch Naming

Create a descriptive branch for your changes:

```bash
git checkout -b feature/add-ipv6-support
git checkout -b fix/packet-fragmentation-bug
git checkout -b docs/improve-api-examples
```

Branch prefixes:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation changes
- `refactor/` - Code refactoring
- `test/` - Test improvements

### Commit Messages

Write clear, descriptive commit messages:

```
Add IPv6 support to DdpClient

- Implement IPv6 address detection
- Update socket binding for dual-stack support
- Add tests for IPv6 connections

Fixes #123
```

Format:
- First line: Brief summary (50 chars or less)
- Blank line
- Detailed description (wrap at 72 chars)
- Reference issues/PRs if applicable

## Coding Standards

### Java Style

Follow standard Java conventions:

```java
// Good
public class DdpClient {
    private final DatagramSocket socket;

    public void sendFrame(byte[] data) {
        // Implementation
    }
}

// Use clear variable names
int pixelCount = 100;  // Good
int n = 100;           // Avoid
```

### Documentation

- Add Javadoc for all public classes and methods
- Include parameter descriptions and return values
- Provide usage examples for complex APIs

```java
/**
 * Sends RGB frame data with automatic packetization.
 *
 * @param rgbData RGB pixel data (3 bytes per pixel)
 * @param pixelCount Number of pixels to send
 * @param packetBuffer Reusable buffer (min size: MAX_PACKET_SIZE)
 * @throws DdpException if send fails or buffer invalid
 */
public void sendRgbFrame(byte[] rgbData, int pixelCount, byte[] packetBuffer)
        throws DdpException {
    // Implementation
}
```

### Performance

- Avoid allocations in hot paths (animation loops)
- Reuse buffers where possible
- Document performance characteristics

### Error Handling

- Use checked exceptions for recoverable errors
- Provide meaningful error messages
- Include context in exception messages

```java
// Good
throw new DdpException("Pixel data buffer too small: need " + required
    + " bytes, got " + actual);

// Avoid
throw new DdpException("Invalid buffer");
```

## Testing

### Writing Tests

- Write tests for all new functionality
- Use descriptive test names
- Follow existing test patterns

```java
@Test
void testSendRgbFrame_LargeFrame_MultiplePackets() throws DdpException {
    // Arrange
    client = new DdpClient("localhost");
    int pixelCount = 500;
    byte[] rgbData = new byte[pixelCount * 3];
    byte[] packetBuffer = new byte[DdpProtocol.MAX_PACKET_SIZE];

    // Act & Assert
    client.sendRgbFrame(rgbData, pixelCount, packetBuffer);
}
```

### Test Coverage

- Aim for comprehensive coverage of new code
- Test both success and failure cases
- Include edge cases (zero pixels, max payload, etc.)

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests DdpClientTest

# Run with verbose output
./gradlew test --info
```

## Submitting Changes

### Before Submitting

1. **Update your branch** with latest upstream:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Run all tests**:
   ```bash
   ./gradlew clean test
   ```

3. **Check code style**:
   ```bash
   ./gradlew check
   ```

4. **Update documentation** if needed:
   - README.md for user-facing changes
   - CHANGELOG.md for version changes
   - Javadoc for API changes

### Creating a Pull Request

1. Push your branch to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

2. Open a Pull Request on GitHub

3. Fill out the PR template:
   - **Description**: What does this PR do?
   - **Motivation**: Why is this change needed?
   - **Testing**: How was it tested?
   - **Breaking Changes**: Any compatibility issues?

### PR Checklist

- [ ] Tests pass locally
- [ ] New tests added for new functionality
- [ ] Documentation updated
- [ ] CHANGELOG.md updated (if applicable)
- [ ] Code follows project style guidelines
- [ ] Commit messages are clear and descriptive
- [ ] No unnecessary dependencies added

### Review Process

- Maintainers will review your PR
- Address any feedback or requested changes
- Once approved, your PR will be merged

## Types of Contributions

### Bug Fixes

- Include steps to reproduce the bug
- Add a test that fails without your fix
- Reference the issue number

### New Features

- Discuss major features in an issue first
- Keep changes focused and incremental
- Update README with usage examples
- Add comprehensive tests

### Documentation

- Fix typos and improve clarity
- Add examples and use cases
- Update API documentation

### Performance Improvements

- Include benchmarks showing improvement
- Ensure no functionality is broken
- Document any trade-offs

## Questions?

If you have questions:
- Open an issue with the `question` label
- Check existing issues and discussions
- Read the README and API documentation

## License

By contributing, you agree that your contributions will be licensed under the LGPL v3 License, matching the project's license.

Thank you for contributing to ddp-lighting-java! 🎉
