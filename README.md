# OSCProjectManager

A Bitwig Studio controller extension that provides comprehensive OSC (Open Sound Control) integration for live performance and project management.

## Overview

OSCProjectManager enables bidirectional communication between Bitwig Studio and OSC clients (like TouchOSC, Lemur, or custom applications). It's designed for live electronic musicians who need streamlined control over large projects with focus on cue markers, scenes, and transition management.

## Features

### 🎯 Core Functionality
- **Cue Marker Control**: Send/receive cue marker names and trigger them remotely
- **Scene Management**: Send/receive scene names and trigger them remotely
- **Transition Support**: Automatic clip/sub-scene name sending from selected tracks
- **Bidirectional OSC**: Full two-way communication with comprehensive error handling

### 🎛️ Track Management
- **Smart Track Duplication**: Copy tracks with proper settings transfer
- **Recording Workflow**: Archive `<REC>` groups with timestamped copies (`<T>_YYYY-MM-DD-HH-MM_name`)
- **Quick Controls**: Turn off monitoring, disarm all tracks via OSC
- **Group Support**: Works with both regular tracks and group sub-scenes

### 🚀 Live Performance Ready
- **Automatic Updates**: Transition names sent when track selection changes
- **Error Resilience**: Malformed OSC messages won't crash the extension
- **Real-time Feedback**: Comprehensive logging for debugging and monitoring

## Quick Start

### Installation
1. Build the extension: `./gradlew build`
2. The extension is automatically deployed to your Bitwig Extensions folder
3. Enable "OSCProjectManager" in Bitwig Studio → Settings → Controllers

### Basic Setup
1. Configure OSC settings in Bitwig's controller preferences:
   - Send Host: IP of your OSC client (default: 127.0.0.1)
   - Send Port: Port for outgoing messages (default: 9000)
   - Receive Port: Port for incoming messages (default: 8000)

2. Your OSC client will automatically receive:
   - Cue marker names on startup
   - Scene names on startup  
   - Transition names when you select tracks

### OSC Endpoints

**Outgoing (Bitwig → OSC Client):**
```
/cue/name/{0..n}        - Cue marker names
/scene/name/{0..n}      - Scene names
/cue/amount             - Number of cue markers
/transition/name/{0..n} - Clip/sub-scene names from selected track
```

**Incoming (OSC Client → Bitwig):**
```
/cue/trigger/{0..n}           - Trigger cue markers (0-based)
/scene/trigger/{0..n}         - Trigger scenes (0-based)
/transition/trigger/{0..n}    - Trigger clips/sub-scenes (0-based)
/track/duplicateToNew         - Duplicate selected track
/track/allMonitoringOff       - Turn off all track monitoring
/track/allArmOff             - Disarm all tracks
/track/makeRecordGroup       - Archive <REC> groups
/track/sendTransitionNames   - Refresh transition names
```

## Usage Examples

### Basic Cue/Scene Control
1. Your OSC client receives all cue/scene names automatically
2. Display them on buttons in your OSC interface
3. Send trigger messages to jump to specific cues/scenes

### Transition Management
1. Select a track with clips in Bitwig
2. OSC client automatically receives clip names
3. Use `/transition/trigger/X` to trigger specific clips
4. Works with both regular tracks and group sub-scenes

### Recording Workflow
1. Create group tracks named `<REC> Session 1`, etc.
2. Send `/track/makeRecordGroup` to archive current takes
3. Original groups remain ready for new recordings
4. Archives are timestamped and muted automatically

## Development

### Build Commands
```bash
./gradlew build          # Build extension
./gradlew clean build    # Clean build
```

### Project Structure
```
src/main/java/com/systemexklusiv/
├── OSCProjectManagerExtension.java    # Main extension entry point
└── services/
    ├── APIServiceImpl.java            # Bitwig API interactions
    ├── OSCManagerImpl.java            # OSC communication
    ├── CueMarkerServiceImpl.java      # Cue marker monitoring
    └── SceneServiceImpl.java          # Scene monitoring
```

### API Documentation
- Local docs: `bitwig-api-documentation/index.html`
- Official: `/Applications/Bitwig Studio.app/Contents/Resources/Documentation/control-surface/api/index.html`

## Requirements

- Bitwig Studio with Extension API 24 support
- Java development environment (for building)
- OSC client application (TouchOSC, Lemur, custom app, etc.)

## License

<!-- Add your license information here -->

## Contributing

<!-- Add contribution guidelines here -->

## Support

For issues and feature requests, please check the documentation in `CLAUDE.md` or create an issue in the repository.