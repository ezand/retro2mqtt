# RetroArch Configuration

retro2mqtt provides comprehensive support for [RetroArch](https://www.retroarch.com/), including log monitoring and ROM analysis.

**Tested with:** v1.21.0

## Configuration

To enable retro2mqtt to monitor RetroArch activity, you need to configure both logging and network commands:

### Logging Setup

1. Open RetroArch
2. Navigate to **Settings → Logging**
3. Enable the following options:
    - **Logging Verbosity** - Set to `1 (Info)` or higher
    - **Log to File** - Enable this option
    - **Timestamp Log Files** - Enable this option (recommended)
4. Note the log file directory location (typically `~/.config/retroarch/logs` on Linux/macOS or
   `%APPDATA%\RetroArch\logs` on Windows)

### Network Commands Setup

1. Navigate to **Settings → Network**
2. Enable the following options:
    - **Network Commands** - Enable this option
    - **Network Command Port** - Note the UDP port (default: `55355`)
3. Configure retro2mqtt with the appropriate host and port

**Note:** Depending on your RetroArch distribution, the network options may not be available in the GUI. In this case,
you can manually configure them in `retroarch.cfg`:

```
network_cmd_enable = "true"
network_cmd_port = "55355"
```

## How It Works

The application will automatically detect and tail the most recent log file, switching to newer logs as they are
created. Additionally, it will fetch configuration details via UDP network commands to retrieve user-specific
information like netplay nickname and achievements settings.