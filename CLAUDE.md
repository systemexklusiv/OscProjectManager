# OSCProjectManager - Bitwig Controller Extension

## Project Overview
This is a Bitwig controller script which is written in Java and uses the Bitwig Studio API 24. It aims to read all Cue Markers as well as Scenes out of the 
Project and sends this information via OSC ( Open Sound Control ). It also receives OSC messages in order to trigger Cue Markers and Scenes as well. 
On the Other side there will be a device in the network running TouchOsc or Lemur f.i. who have dedicated Button wodgets which will display the name of 
the Cue Markers or Scenes respectivly. On the device one can select one of the scenes to jump to.


## Architecture
To be done .. please check "Future plans" in this document about whats to be implemented

## Key Features
It provides a streamlind interfae to a Bitwig arrangement with Cue Markers and Scenes very much in focus. It enables the live electronic musician to keep 
overview over a very large project and helps to jump on demand to veryious parts of the arrangement in order to kamke it playably and flexible but also alows to get easily back into the linear arrangement

## Implementation Notes
Check these links as OSC implementation reference https://github.com/git-moss/DrivenByMoss/tree/master/src/main/java/de/mossgrabers/controller/osc
check my other repository for implementation reference on how to use the API with Cue Marker: https://github.com/systemexklusiv/bwProjectControl2/tree/miditwister_claude
Check locally on this device the API documantation. Here the path to the HTML documentation: /Applications/Bitwig\ Studio.app/Contents/Resources/Documentation/control-surface/api/index.html

The OSC message format which sends out the Cue Marker names should be for each: /cue{0..n}/name "my super Cue Marker Name" 
The OSC message format which sends out the Scene names should be for each: /scene{0..n}/name "my super Scene Name" 
The OSC message format shall be for triggering Cue Markers which will be received by this script: /cue/trigger{0..n}
The OSC message format shall be for triggering Scenes which will be received by this script: /scene/trigger{0..n}

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

## Future Plans
Besides the Entrypoint of the Script there should be  Services which retrieves all the Cue Marker informations.
Another Service will be there which retrieves all the scene informations. Another service will be the OSCManager which is then used
by the Cue Marker and Scene servives to send this informtion out via OSC in UDP. On the receiving part of this scripts there will be
a listener implemented in the OSCManager which listens for incomming OSC messages in order to trigger Cue Marker or Scenes. The OSCManager has
an instance of another Service the API Service which interacts with Bitwig and in the end triggers Cue Markers and Scenes with the use of the API.

## Endpoint Implementation Ideas
- Add a new endpoint "track/makeRecordGroup" which does the following:
  * Looks for a Bitwig group track with "<REC>" in its name
  * When found, duplicates the track using Bitwig API's duplicate track function
  * On the original source group:
    - Turn all monitor settings to off
    - Unarmed all tracks inside this group
    - Mute the group itself
  * Name the new group track as "<Take> # " with current date appended in format: YYYY-MM-DD-Hour-Minute