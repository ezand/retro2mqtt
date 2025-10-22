# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive test suite
- GitHub Actions workflow for running tests on push and pull requests
- Cognitect test-runner for automatic test discovery
- LaunchBox platform support (in development)
- Generic IPC implementation for cross-platform Unix domain socket and Windows named pipe support
- SLF4J structured logging with logback
- Development logback configuration with key-value pair logging

### Changed
- Improved logger implementation with JSON serialization for collections

### Documentation
- Added testing documentation to development guide
- Added instructions for running tests with `clj -X:test-runner`

## Initial Development

### Added
- **RetroArch Support**
  - Real-time log monitoring and parsing
  - ROM metadata extraction (SNES supported)
  - Core information from `.info` files
  - UDP command interface integration
  - Configuration file extraction
  - Home Assistant MQTT discovery

- **Core Features**
  - MQTT client with HiveMQ library
  - Multi-topic subscription with state merging
  - Home Assistant integration
  - Docker support with docker-compose setup
  - Configuration management via EDN files

- **Infrastructure**
  - Clojure CLI tools build system
  - Dockerfile for containerized deployment
  - Development and production configurations
  - MIT License

[unreleased]: https://github.com/ezand/retro2mqtt/compare/HEAD
