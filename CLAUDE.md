# OSCProjectManager - Bitwig Controller Extension

## Project Overview
This is a Bitwig controller script which is written in Java and uses the Bitwig Studio API 24. It aims to read all Cue Markers as well as Scenes out of the 
Project and sends this information via OSC ( Open Sound Control ). It also receives OSC messages in order to trigger Cue Markers and Scenes as well. 
On the Other side there will be a device in the network running TouchOsc or Lemur f.i. who have dedicated Button wodgets which will display the name of 
the Cue Markers or Scenes respectivly. On the device one can select one of the scenes to jump to.


## Architecture
To be done .. please check "Future plans" in this document about whats to be implemented

## Key Features

### Core Functionality
- **Cue Marker Integration**: Sends all cue marker names via OSC and allows triggering them remotely
- **Scene Management**: Sends all scene names via OSC and allows triggering them remotely  
- **Transition Track Support**: Automatically sends clip/sub-scene names from selected track for transition control
- **Bidirectional OSC**: Full two-way communication between Bitwig and OSC clients

### Track Management
- **Smart Track Duplication**: Duplicate selected track with proper settings transfer
- **Group-based Recording Workflow**: Archive `<REC>` groups with timestamped copies
- **Monitoring Control**: Turn off monitoring for all tracks (except `<G>` groups)
- **Track Disarming**: Disarm all tracks with a single OSC command

### Advanced Features
- **Automatic Track Selection Response**: Transition names sent automatically when track changes
- **Group Track Support**: Works with both regular tracks and group sub-scenes
- **Robust Error Handling**: Comprehensive OSC message error handling without crashes
- **Live Performance Focused**: Streamlined interface for live electronic musicians to maintain overview and control of large projects

## Implementation Notes
Check these links as OSC implementation reference https://github.com/git-moss/DrivenByMoss/tree/master/src/main/java/de/mossgrabers/controller/osc
check my other repository for implementation reference on how to use the API with Cue Marker: https://github.com/systemexklusiv/bwProjectControl2/tree/miditwister_claude

### Bitwig API Documentation
- **Local API docs**: /Applications/Bitwig\ Studio.app/Contents/Resources/Documentation/control-surface/api/index.html
- **Included API docs**: This repository includes a copy of the Bitwig Studio Extension API documentation in `bitwig-api-documentation/` folder
  - Browse `bitwig-api-documentation/index.html` for the main API overview
  - All interfaces and classes are documented with HTML files
  - Use this for offline reference when implementing new features

### OSC Message Formats

**Outgoing Messages (Bitwig → OSC Client):**
- Cue Marker names: `/cue/name/{0..n}` with name as parameter
- Scene names: `/scene/name/{0..n}` with name as parameter  
- Cue Marker count: `/cue/amount` with count as parameter
- Transition names: `/transition/name/{0..n}` with clip/sub-scene name as parameter

**Incoming Messages (OSC Client → Bitwig):**
- Trigger Cue Markers: `/cue/trigger/{0..n}` (0-based indexing)
- Trigger Scenes: `/scene/trigger/{0..n}` (0-based indexing)
- Duplicate selected track: `/track/duplicateToNew`
- Turn off all monitoring: `/track/allMonitoringOff`
- Disarm all tracks: `/track/allArmOff`
- Make record group: `/track/makeRecordGroup`
- Send transition names: `/track/sendTransitionNames`
- Trigger transition slots: `/transition/trigger/{0..n}` (0-based indexing)

The way OSC via UDP is used and with which library can be taken from this code: Check these links as OSC implementation reference https://github.com/git-moss/DrivenByMoss/tree/master/src/main/java/de/mossgrabers/controller/osc

The OSC connection properties like remote adress of external device and ports should be done via properties in the Bitwig UI. take this implementation as reference:
https://github.com/systemexklusiv/bwProjectControl2/blob/miditwister_claude/src/main/java/de/systemexklusiv/sysexprojectcontrol/SysexProjectControlExtension.java
startig a line 160

## Build Commands

```bash
./gradlew build          # Build the extension
./gradlew clean build    # Clean build
```

## Development Setup
<!-- Document any special setup requirements for development -->

## Testing
<!-- Document testing approach and how to run tests -->

## Deployment
The build automatically:
1. Creates `OSCProjectManager-0.1.bwextension` in the `build` directory
2. Deploys it to `/Users/davidrival/Documents/Bitwig Studio/Extensions`

## Known Issues
<!-- Document any known issues or limitations -->
- When using `application.selectAll(); application.deleteSelection();`, this will delete content from ALL tracks, not just the newly copied track. Need a more targeted approach to delete only the recordings of the newly copied track.

## Architecture (Implemented)

The extension follows a service-oriented architecture:

- **OSCProjectManagerExtension**: Main entry point that coordinates all services
- **APIServiceImpl**: Handles all Bitwig API interactions (tracks, clips, cue markers, scenes)
- **OSCManagerImpl**: Manages bidirectional OSC communication via JavaOSC library
- **CueMarkerServiceImpl**: Monitors cue markers and sends updates via OSC
- **SceneServiceImpl**: Monitors scenes and sends updates via OSC

## Current Implementation Status

✅ **Completed Features**:
- Cue marker monitoring and OSC sending
- Scene monitoring and OSC sending  
- OSC message receiving and handling
- Track duplication with settings transfer
- Group-based recording workflow (`<REC>` groups → `<T>_timestamp` archives)
- Transition track support (automatic clip name sending on track selection)
- Bidirectional transition control (send names + receive triggers)
- Comprehensive error handling
- Group track sub-scene support

## Future Plans
- **I/O Selection Control**: Implement set/reset of track input/output selection when accessible through the Bitwig API
- **Convenient Performance Controls**:
  - One-button arrangement for minimizing windows (shrinks browser, device windows, clip-launcher, leaving only arrangement view visible)
  - Toggle Metronom on/off
  - Toggle custom guide track on/off
  - Quick jump back to automation
  - Save and recall all mixer and device states