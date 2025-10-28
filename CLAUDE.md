# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

retro2mqtt is a Clojure application that bridges retro gaming platforms to MQTT, enabling real-time monitoring and automation. It monitors RetroArch logs, LaunchBox events, and audio2mqtt fingerprints, publishing gaming session data to MQTT topics for Home Assistant integration.

## Essential Commands

### Testing
```bash
# Run all tests
bin/kaocha

# Run with verbose output
bin/kaocha --reporter documentation

# Run with profiling
bin/kaocha --reporter documentation --plugin profiling

# Run specific test namespace
bin/kaocha --focus ezand.retro2mqtt.launchbox.mqtt-test

# Watch mode
bin/kaocha --watch
```

### Building
```bash
# Build uberjar (creates target/retro2mqtt-1.0.<git-rev>-standalone.jar)
clj -T:build uber

# Clean build artifacts
clj -T:build clean
```

### Running
```bash
# Run directly with Clojure CLI
clj -M:run
clj -A:dev -M:run

# Run uberjar
java -jar target/retro2mqtt-*-standalone.jar

# Run with custom config
java -Dconfig=dev-config.edn -jar target/retro2mqtt-*-standalone.jar
```

### REPL
```bash
clj -M:repl
```

## Architecture

### Provider Pattern

The application uses a **provider pattern** where each platform integration implements the `RetroProvider` protocol:

```clojure
(defprotocol RetroProvider
  (start-listening! [this])
  (stop-listening! [this]))
```

Providers are initialized in `core.clj` based on `:enabled?` flags in config:
- **RetroArch**: Tails log files, extracts ROM metadata, queries UDP for config
- **LaunchBox**: Subscribes to MQTT topics published by launchbox2mqtt plugin
- **audio2mqtt**: Subscribes to MQTT topics published by audio2mqtt for audio fingerprinting

Each provider namespace has two key files:
- `core.clj` - Provider implementation with lifecycle management
- `mqtt.clj` - MQTT topic definitions and Home Assistant discovery

### Configuration

Configuration is in EDN format at `resources/config.edn` with environment-specific overrides supported via `dev-config.edn`. Key structure:

```edn
{:retro2mqtt
 {:mqtt {:host "..." :port 1883 :username "..." :password "..."}
  :retroarch {:enabled? true :log-dir "..." :config-dir "..."}
  :launchbox {:enabled? false :topic-prefix "launchbox"}
  :audio {:enabled? false :topic-prefix "audio_events"}
  :integrations {:home-assistant {:discovery? true}}}}
```

**Topic Prefix Pattern**: LaunchBox and audio2mqtt support configurable `:topic-prefix` to customize MQTT topic namespaces. Topics are built dynamically via functions like `launchbox-topics` and `audio-topics` that accept the prefix and return a map of topic keys to strings.

### MQTT Integration

**Two-way MQTT patterns:**

1. **State Publishing** (RetroArch): Application publishes to MQTT topics as events occur
2. **State Subscribing** (LaunchBox, audio2mqtt): Application subscribes to MQTT topics published by external sources

**Multi-topic aggregation** (`mqtt/multi_topic.clj`): Subscribes to multiple MQTT topics and aggregates their payloads into a single state map, then publishes to a target topic. Uses `topic-matches?` utility for MQTT wildcard support (`#` for multi-level, `+` for single-level).

**Home Assistant Discovery**: When `:discovery? true`, providers call `publish-homeassistant-discovery!` to auto-create entities. Entity configurations specify:
- `state_topic` - Main state value
- `json_attributes_topic` - Aggregated attributes from multiple sources
- `attribute-state-topics` - Map of source topics to merge into attributes

### RetroArch Log Tailing

RetroArch monitoring (`retroarch/log_tailer.clj`) uses pattern matching against log lines:

```clojure
{:pattern-key {:regexp #"..."
               :update-fn (fn [match] ...)
               :state-topic "topic"
               :retain? true
               :on-match-fn (fn [mqtt-client data] ...)}}
```

The log tailer:
- Monitors log directory for new files (timestamped format: `retroarch__YYYY_MM_DD__HH_MM_SS.log`)
- Automatically switches to newer logs as they're created
- Maintains file position to avoid re-processing
- Extracts ROM metadata on content load
- Queries RetroArch UDP interface for runtime config

### Key Utilities

- **`utils/topic-matches?`**: MQTT wildcard pattern matching (supports `#` and `+`)
- **`rom/core`**: Extracts metadata from ROM filenames (region, revision, tags)
- **`retroarch/info-file`**: Parses `.info` files for core metadata
- **`ipc/core`**: Inter-process communication via named pipes

## Testing Patterns

Tests use standard `clojure.test`. Key patterns:

- **Mocking MQTT**: Use `with-redefs` to stub `mqtt/subscribe!` and `mqtt/publish!`
- **Provider tests**: Create providers with `->ProviderRecord` and test lifecycle
- **Pure functions**: Log parsing and pattern matching are pure functions tested without I/O
- **Test data**: Use `def test-topics` for shared topic maps in tests

When refactoring providers or MQTT integration:
1. Update both `mqtt.clj` (topic definitions, entity configs) and `core.clj` (provider lifecycle)
2. Update corresponding test files to match new function signatures
3. Run `bin/kaocha` to verify all tests pass

## Code Style

- Use `defonce` for stateful atoms
- Pure functions are marked with `-` prefix (e.g., `-start-listening!` is private)
- MQTT topics are built dynamically from config, not hardcoded constants
- Home Assistant entities use descriptive `unique_id` and `name` fields
- Log with structured data: `(log/debug logger "message" {:key value})`
