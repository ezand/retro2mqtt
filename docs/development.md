# Development Guide

## Prerequisites

- [Clojure CLI tools](https://clojure.org/guides/install_clojure)

## Project Structure

- `src/` - Source code
- `resources/` - Application resources
- `target/` - Build artifacts (gitignored)

## Development Workflow

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
clj -A:dev -M:run
```

## 🧪 Testing

Run all tests:

```bash
clj -M:-runner "$@" # Directly

# Conveniance scripts
bin/kaocha       # Unix / Mac OS
bin/kaocha.ps1   # Powershell
bin/kaocha.bat   # Windows Command
```

Run specific test namespace:

```bash
clj -M:test-runner "$@" --focus ezand.retro2mqtt.ipc.core-test # Directly
bin/kaocha --focus ezand.retro2mqtt.ipc.core-test       # Conveniance script
```

With detailed outout:
```bash
bin/kaocha --reporter documentation
```

With profiling:
```bash
bin/kaocha --plugin kaocha.plugin/profiling
```

Watch for changes:

```bash
bin/kaocha --watch
```

## 🔨 Building

Build an uberjar:

```bash
clj -T:build uber
```

This creates a standalone JAR at `target/retro2mqtt-1.0.<git-rev>-standalone.jar`.

Clean build artifacts:

```bash
clj -T:build clean
```

## ▶️ Running

Run the uberjar:

```bash
java -jar target/retro2mqtt-*-standalone.jar

java -Dconfig=dev-config.edn -jar target/retro2mqtt-0.0.14-standalone.jar
```

## 🐳 Docker

### Docker Compose

The project includes a `docker-compose.yml` file that runs
Mosquitto MQTT broker and Home Assistant on a shared network.

Start all services:

```bash
docker compose up -d
```

Stop all services:

```bash
docker compose down
```

**Default credentials:**

- **Home Assistant**: username: `retro`, password: `retro` (http://localhost:8123)
- **Mosquitto MQTT**: username: `mosquitto`, password: `mosquitto`

### Standalone Docker

Build the Docker image:

```bash
docker build -t retro2mqtt .
```

#### 🌍 Networking

To connect to services running on the host machine (like an MQTT broker), use `--network host`:

```bash
docker run --network host retro2mqtt
```

This allows the container to access `localhost` services on the host. Alternatively, you can configure the MQTT host
as `host.docker.internal` in your config file (works on Docker Desktop for Mac/Windows).

#### ▶️ Running

Run with script:

```bash
./run-docker.sh /Users/user/Documents/RetroArch/logs
```

Run with bundled config:

```bash
docker run --name retro2mqtt_app --network host retro2mqtt
```

Run with custom config file:

```bash
docker run --name retro2mqtt_app --network host \
  -e CONFIG_FILE=custom-config.edn \
  -v /path/to/custom-config.edn:/app/custom-config.edn \
  retro2mqtt
```

Mount RetroArch logs directory for monitoring:

```bash
docker run --name retro2mqtt_app  --network host \
  -e CONFIG_FILE=custom-config.edn \
  -v /path/to/custom-config.edn:/app/custom-config.edn \
  -v ~/.config/retroarch/logs:/app/retroarch/logs:ro \
  retro2mqtt
```