package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.SendBank;

import java.util.ArrayList;
import java.util.List;

public class ProjectDiscoveryService {
    
    private ControllerHost host;
    private TrackBank allTracksBank;
    private MasterTrack masterTrack;
    private List<TrackSnapshot> discoveredTracks;
    
    public void initialize(ControllerHost host, TrackBank allTracksBank, MasterTrack masterTrack) {
        this.host = host;
        this.allTracksBank = allTracksBank;
        this.masterTrack = masterTrack;
        this.discoveredTracks = new ArrayList<>();
        
        // Mark master track properties as interested
        if (masterTrack != null) {
            masterTrack.exists().markInterested();
            masterTrack.volume().markInterested();
            masterTrack.pan().markInterested();
            masterTrack.mute().markInterested();
            masterTrack.name().markInterested();
        }
        
        host.println("ProjectDiscoveryService initialized");
    }
    
    public void spiderCurrentProject() {
        host.println("=== Project Discovery: Spidering Tracks ===");
        
        discoveredTracks.clear(); // Clear previous results
        
        // Spider all tracks (including nested group tracks)
        spiderAllTracks();
        
        // Spider master track
        spiderMasterTrack();
        
        host.println("=== Project Discovery Complete ===");
        host.println("Discovered " + discoveredTracks.size() + " track snapshots");
    }
    
    private void spiderAllTracks() {
        int tracksFound = 0;
        
        for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
            Track track = allTracksBank.getItemAt(i);
            
            if (track.exists().get()) {
                TrackSnapshot snapshot = captureTrackState(track, i);
                discoveredTracks.add(snapshot);
                tracksFound++;
                
                host.println("  Captured track " + i + ": \"" + snapshot.trackName + "\" (" + snapshot.trackType + ")");
            }
        }
        
        host.println("Spidered " + tracksFound + " regular tracks");
    }
    
    private void spiderMasterTrack() {
        if (masterTrack != null && masterTrack.exists().get()) {
            TrackSnapshot masterSnapshot = captureMasterTrackState();
            discoveredTracks.add(masterSnapshot);
            
            host.println("  Captured master track: \"" + masterSnapshot.trackName + "\"");
            host.println("Spidered master track");
        } else {
            host.println("WARNING: Master track not available");
        }
    }
    
    private TrackSnapshot captureTrackState(Track track, int position) {
        TrackSnapshot snapshot = new TrackSnapshot();
        
        // Basic identification
        snapshot.trackName = track.name().get();
        snapshot.trackPosition = position;
        
        // Determine track type
        boolean canHoldNotes = track.canHoldNoteData().get();
        boolean canHoldAudio = track.canHoldAudioData().get();
        boolean isGroup = track.isGroup().get();
        
        if (isGroup) {
            snapshot.trackType = "group";
        } else if (canHoldNotes && !canHoldAudio) {
            snapshot.trackType = "instrument";
        } else if (canHoldAudio && !canHoldNotes) {
            snapshot.trackType = "audio";
        } else {
            snapshot.trackType = "hybrid"; // Can hold both
        }
        
        // Capture mixer states (these properties are already marked as interested in setupAllTracksBank)
        snapshot.volume = track.volume().get();
        snapshot.pan = track.pan().get();
        snapshot.muted = track.mute().get();
        snapshot.armed = track.arm().get();
        snapshot.monitorMode = track.monitorMode().get();
        
        // Capture send levels
        SendBank sendBank = track.sendBank();
        for (int s = 0; s < TrackSnapshot.SEND_BANK_SIZE; s++) {
            snapshot.sendLevels[s] = sendBank.getItemAt(s).get();
        }
        
        return snapshot;
    }
    
    private TrackSnapshot captureMasterTrackState() {
        TrackSnapshot snapshot = new TrackSnapshot();
        
        // Master track identification
        snapshot.trackName = "Master";
        snapshot.trackType = "master";
        snapshot.trackPosition = -1; // Special position for master
        
        // Capture master mixer states
        snapshot.volume = masterTrack.volume().get();
        snapshot.pan = masterTrack.pan().get();
        snapshot.muted = masterTrack.mute().get();
        snapshot.armed = false; // Master track doesn't have arm state
        
        return snapshot;
    }
    
    public List<TrackSnapshot> getDiscoveredTracks() {
        return new ArrayList<>(discoveredTracks); // Return copy to prevent external modification
    }
    
    public int getDiscoveredTrackCount() {
        return discoveredTracks.size();
    }
}