![alt text](docs/retro2mqtt_banner.png)

[![Deploy](https://github.com/ezand/retro2mqtt/actions/workflows/build.yml/badge.svg)](https://github.com/ezand/retro2mqtt/actions/workflows/build.yml)
[![GitHub License](https://img.shields.io/github/license/ezand/retro2mqtt)](https://choosealicense.com/licenses/mit/)
![GitHub top language](https://img.shields.io/github/languages/top/ezand/retro2mqtt)

# retro2mqtt

A Clojure application that bridges retro gaming systems to MQTT, enabling real-time monitoring and automation of retro
gaming sessions.

## 👀 Overview

retro2mqtt monitors retro gaming platforms and publishes detailed information about gaming sessions to MQTT topics. This
enables home automation systems, status displays, and other integrations to respond to gaming activity in real-time.

The application extracts information from multiple sources:

- **Platform log files** - Real-time monitoring of emulator activity and system information
- **ROM files** - Metadata extraction from game files (title, region, ROM type, etc.)
- **Platform metadata** - Core information, system specifications, and configuration details

### Supported Platforms

Currently supported:

- **RetroArch** - Comprehensive support for log monitoring and ROM analysis

Planned support:

- **LaunchBox** - Integration with LaunchBox frontend
- **HyperSpin** - Integration with HyperSpin frontend

## Setup

### 🎮 RetroArch Configuration

To enable retro2mqtt to monitor RetroArch activity, you must enable logging in RetroArch:

1. Open RetroArch
2. Navigate to **Settings → Logging**
3. Enable the following options:
    - **Logging Verbosity** - Set to `1 (Info)` or higher
    - **Log to File** - Enable this option
    - **Timestamp Log Files** - Enable this option (recommended)
4. Note the log file directory location (typically `~/.config/retroarch/logs` on Linux/macOS or
   `%APPDATA%\RetroArch\logs` on Windows)
5. Configure retro2mqtt to point to this directory

The application will automatically detect and tail the most recent log file, switching to newer logs as they are
created.

### 📡 MQTT Broker

You'll need an MQTT broker running and accessible. Popular options include:

- Mosquitto
- HiveMQ
- EMQX

### 🏠 Home Assistant Integration

retro2mqtt supports [MQTT Discovery](https://www.home-assistant.io/integrations/mqtt/#mqtt-discovery) for seamless
Home Assistant integration. When enabled in the configuration, entities are automatically created in Home Assistant
without manual setup:

```edn
:integrations {:home-assistant {:discovery? true}}
```

With discovery enabled, gaming session information appears automatically as sensors in Home Assistant, enabling
automations based on what game you're playing, which core is running, and other metadata.

## 💻 Development

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
clj -A:dev -M:run
```

### 🔨 Building

Build an uberjar:

```bash
clj -T:build uber
```

This creates a standalone JAR at `target/retro2mqtt-1.0.<git-rev>-standalone.jar`.

Clean build artifacts:

```bash
clj -T:build clean
```

### ▶️ Running

Run the uberjar:

```bash
java -jar target/retro2mqtt-*-standalone.jar

java -Dconfig=dev-config.edn -jar target/retro2mqtt-0.0.14-standalone.jar
```

### 🐳 Docker

#### Docker Compose

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

#### Standalone Docker

Build the Docker image:

```bash
docker build -t retro2mqtt .
```

##### 🌍 Networking

To connect to services running on the host machine (like an MQTT broker), use `--network host`:

```bash
docker run --network host retro2mqtt
```

This allows the container to access `localhost` services on the host. Alternatively, you can configure the MQTT host
as `host.docker.internal` in your config file (works on Docker Desktop for Mac/Windows).

##### ▶️ Running

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

## 📃 License

MIT License - see [LICENSE](LICENSE) file for details.
