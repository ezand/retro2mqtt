# retro2mqtt

A Clojure application for bridging retro systems to MQTT.

## Development

### Prerequisites

- [Clojure CLI tools](https://clojure.org/guides/install_clojure)

### Project Structure

- `src/` - Source code
- `resources/` - Application resources
- `target/` - Build artifacts (gitignored)

### Development Workflow

Start a REPL:

```bash
clj -M:repl
```

Or use a basic REPL:

```bash
clj
```

Run the application directly:

```bash
clj -M:run
```

### Building

Build an uberjar:

```bash
clj -T:build uber
```

This creates a standalone JAR at `target/retro2mqtt-1.0.<git-rev>-standalone.jar`.

Clean build artifacts:

```bash
clj -T:build clean
```

### Running

Run the uberjar:

```bash
java -jar target/retro2mqtt-*-standalone.jar
```

## License

See LICENSE file for details.
