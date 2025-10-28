# audio2mqtt Configuration

retro2mqtt integrates with [audio2mqtt](https://github.com/ezand/audio2mqtt) for audio playback monitoring and capturing in-game events through audio fingerprinting.

For audio2mqtt setup and installation instructions, see the [audio2mqtt repository](https://github.com/ezand/audio2mqtt).

## Configuration

### Basic Setup

Enable audio2mqtt integration in your `config.edn`:

```edn
{:retro2mqtt
 {:audio {:enabled? true
          :topic-prefix "audio_events"}
  :integrations {:home-assistant {:discovery? true}}}}
```

### Configuration Options

- **`:enabled?`** - Enable or disable audio2mqtt integration (default: `false`)
- **`:topic-prefix`** - MQTT topic prefix for audio events (default: `"audio_events"`)

### Custom Topic Prefix

You can customize the MQTT topic prefix to avoid conflicts or organize topics:

```edn
{:audio {:enabled? true
         :topic-prefix "custom_audio"}}
```

This changes all audio topics from `audio_events/*` to `custom_audio/*`.

## MQTT Topics

audio2mqtt publishes to the following topics (using default prefix):

### System Topics

- **`audio_events/system/running`** - System running status
- **`audio_events/system/details`** - System information and version details

### Event Topics

- **`audio_events/event`** - Individual audio detection events with metadata
- **`audio_events/event/last_song`** - Most recent audio event detected

All topics support MQTT wildcards for subscription:
- `audio_events/#` - Subscribe to all audio events
- `audio_events/event/#` - Subscribe to all event topics
- `audio_events/system/+` - Subscribe to all system topics

## Home Assistant Integration

When Home Assistant discovery is enabled (`:discovery? true`), retro2mqtt automatically creates sensors for audio events:

- **audio2mqtt** sensor - Shows system running status with attributes containing version and configuration details
- **Last Audio Event** sensor - Shows the most recently detected audio event with metadata attributes

These sensors update in real-time as audio events are detected by audio2mqtt.

## How It Works

retro2mqtt subscribes to MQTT topics published by audio2mqtt and translates them into Home Assistant-compatible entities. The integration:

1. Monitors system status from audio2mqtt
2. Receives audio fingerprint detection events
3. Publishes Home Assistant discovery messages
4. Updates entity attributes with audio metadata
