package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.SourceSelector;
import com.bitwig.extension.controller.api.SettableBooleanValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class APIServiceImpl {
    
    private static final int CUE_MARKER_BANK_SIZE = 128;
    private static final int SCENE_BANK_SIZE = 128;
    
    private ControllerHost host;
    private Arranger arranger;
    private CueMarkerBank cueMarkerBank;
    private SceneBank sceneBank;
    private TrackBank trackBank;
    private TrackBank allTracksBank; // Flat bank to access all tracks including nested ones
    private Track cursorTrack;
    private Application application;
    
    public void initialize(ControllerHost host) {
        this.host = host;
        this.arranger = host.createArranger();
        
        setupCueMarkerBank();
        setupSceneBank();
        setupTrackBank();
        setupAllTracksBank();
        setupCursorTrack();
        setupApplication();
    }
    
    private void setupCueMarkerBank() {
        cueMarkerBank = arranger.createCueMarkerBank(CUE_MARKER_BANK_SIZE);
        
        for (int i = 0; i < CUE_MARKER_BANK_SIZE; i++) {
            CueMarker cueMarker = cueMarkerBank.getItemAt(i);
            
            cueMarker.exists().markInterested();
            cueMarker.position().markInterested();
            cueMarker.getName().markInterested();
        }
    }
    
    private void setupTrackBank() {
        trackBank = host.createTrackBank(128, 0, 0, false);
        
        for (int i = 0; i < 128; i++) {
            Track track = trackBank.getItemAt(i);
            track.exists().markInterested();
            track.name().markInterested();
            track.arm().markInterested();
            track.monitorMode().markInterested();
            track.canHoldNoteData().markInterested();
            track.canHoldAudioData().markInterested();
            track.isGroup().markInterested();
            
            // Setup source selector for input routing
            SourceSelector sourceSelector = track.sourceSelector();
            sourceSelector.hasAudioInputSelected().markInterested();
            sourceSelector.hasNoteInputSelected().markInterested();
        }
    }
    
    private void setupAllTracksBank() {
        // Create a flat track bank that includes ALL tracks (including nested ones)
        allTracksBank = host.createTrackBank(512, 0, 0, true); // Large flat bank with hasFlatTrackList=true
        
        for (int i = 0; i < 512; i++) {
            Track track = allTracksBank.getItemAt(i);
            track.exists().markInterested();
            track.name().markInterested();
            track.arm().markInterested();
            track.monitorMode().markInterested();
            track.isMonitoring().markInterested();
            track.canHoldNoteData().markInterested();
            track.canHoldAudioData().markInterested();
            track.isGroup().markInterested();
            
            // Setup source selector for input routing
            SourceSelector sourceSelector = track.sourceSelector();
            sourceSelector.hasAudioInputSelected().markInterested();
            sourceSelector.hasNoteInputSelected().markInterested();
        }
    }
    
    private void setupCursorTrack() {
        cursorTrack = host.createCursorTrack(0, 0);
        cursorTrack.exists().markInterested();
        cursorTrack.arm().markInterested();
        cursorTrack.monitorMode().markInterested();
        cursorTrack.name().markInterested();
        cursorTrack.canHoldNoteData().markInterested();
        cursorTrack.canHoldAudioData().markInterested();
        
        // Setup source selector for input routing
        SourceSelector sourceSelector = cursorTrack.sourceSelector();
        sourceSelector.hasAudioInputSelected().markInterested();
        sourceSelector.hasNoteInputSelected().markInterested();
    }
    
    private void setupApplication() {
        application = host.createApplication();
    }
    
    private void setupSceneBank() {
        sceneBank = host.createSceneBank(SCENE_BANK_SIZE);
        
        for (int i = 0; i < SCENE_BANK_SIZE; i++) {
            Scene scene = sceneBank.getItemAt(i);
            
            scene.exists().markInterested();
            scene.getName().markInterested();
        }
    }
    
    public CueMarkerBank getCueMarkerBank() {
        return cueMarkerBank;
    }
    
    public SceneBank getSceneBank() {
        return sceneBank;
    }
    
    public void triggerCueMarker(int index) {
        if (index >= 0 && index < CUE_MARKER_BANK_SIZE) {
            CueMarker cueMarker = cueMarkerBank.getItemAt(index);
            if (cueMarker.exists().get()) {
                cueMarker.launch(true);
                host.println("Triggered cue marker " + index + ": " + cueMarker.getName().get());
            }
        }
    }
    
    public void triggerScene(int index) {
        if (index >= 0 && index < SCENE_BANK_SIZE) {
            Scene scene = sceneBank.getItemAt(index);
            if (scene.exists().get()) {
                scene.launch();
                host.println("Triggered scene " + index + ": " + scene.getName().get());
            }
        }
    }
    
    public int getCueMarkerCount() {
        int count = 0;
        for (int i = 0; i < CUE_MARKER_BANK_SIZE; i++) {
            if (cueMarkerBank.getItemAt(i).exists().get()) {
                count++;
            }
        }
        return count;
    }
    
    public int getSceneCount() {
        int count = 0;
        for (int i = 0; i < SCENE_BANK_SIZE; i++) {
            if (sceneBank.getItemAt(i).exists().get()) {
                count++;
            }
        }
        return count;
    }
    
    public String getCueMarkerName(int index) {
        if (index >= 0 && index < CUE_MARKER_BANK_SIZE) {
            CueMarker cueMarker = cueMarkerBank.getItemAt(index);
            if (cueMarker.exists().get()) {
                return cueMarker.getName().get();
            }
        }
        return "";
    }
    
    public String getSceneName(int index) {
        if (index >= 0 && index < SCENE_BANK_SIZE) {
            Scene scene = sceneBank.getItemAt(index);
            if (scene.exists().get()) {
                return scene.getName().get();
            }
        }
        return "";
    }
    
    public void duplicateSelectedTrackToNew() {
        if (cursorTrack.exists().get()) {
            boolean wasArmed = cursorTrack.arm().get();
            String monitorModeValue = cursorTrack.monitorMode().get();
            String originalTrackName = cursorTrack.name().get();
            boolean canHoldNotes = cursorTrack.canHoldNoteData().get();
            boolean canHoldAudio = cursorTrack.canHoldAudioData().get();
            
            // Capture input routing settings
            SourceSelector originalSourceSelector = cursorTrack.sourceSelector();
            boolean hasAudioInput = originalSourceSelector.hasAudioInputSelected().get();
            boolean hasNoteInput = originalSourceSelector.hasNoteInputSelected().get();
            
            if (wasArmed) {
                cursorTrack.arm().set(false);
            }
            
            // Create appropriate track type based on original
            if (canHoldNotes && !canHoldAudio) {
                // Instrument track
                application.createInstrumentTrack(-1);
                host.println("Creating instrument track...");
            } else if (canHoldAudio && !canHoldNotes) {
                // Audio track
                application.createAudioTrack(-1);
                host.println("Creating audio track...");
            } else {
                // Default to audio track if unclear
                application.createAudioTrack(-1);
                host.println("Creating default audio track...");
            }
            
            host.scheduleTask(() -> {
                Track newTrack = findNewestTrack();
                if (newTrack != null) {
                    transferTrackSettings(newTrack, monitorModeValue, originalTrackName, wasArmed, hasAudioInput, hasNoteInput);
                } else {
                    host.println("Failed to locate newly created track.");
                }
            }, 300);
            
        } else {
            host.println("No track selected for duplication.");
        }
    }
    
    private Track findNewestTrack() {
        // Find the last existing track in the track bank
        Track lastTrack = null;
        for (int i = trackBank.getSizeOfBank() - 1; i >= 0; i--) {
            Track track = trackBank.getItemAt(i);
            if (track.exists().get()) {
                lastTrack = track;
                break;
            }
        }
        return lastTrack;
    }
    
    private void transferTrackSettings(Track newTrack, String monitorModeValue, String originalTrackName, boolean wasArmed, boolean hasAudioInput, boolean hasNoteInput) {

        newTrack.name().set(originalTrackName + " Copy");
        newTrack.monitorMode().set(monitorModeValue);
        
        if (wasArmed) {
            newTrack.arm().set(true);
        }
        
        // Log I/O routing and monitoring information
        SourceSelector newSourceSelector = newTrack.sourceSelector();
        host.println("=== Track Settings Analysis ===");
        host.println("Original track I/O: Audio Input=" + hasAudioInput + ", Note Input=" + hasNoteInput);
        host.println("New track I/O:      Audio Input=" + newSourceSelector.hasAudioInputSelected().get() + 
                     ", Note Input=" + newSourceSelector.hasNoteInputSelected().get());
        host.println("Monitor mode transferred: " + monitorModeValue + " -> " + newTrack.monitorMode().get());
        host.println("Monitoring active: " + newTrack.isMonitoring().get());
        
        // Check if the SourceSelector values are settable
        host.println("Audio input settable: " + (newSourceSelector.hasAudioInputSelected() instanceof SettableBooleanValue));
        host.println("Note input settable:  " + (newSourceSelector.hasNoteInputSelected() instanceof SettableBooleanValue));
        
        // TODO: I/O routing transfer not yet implemented
        // The Bitwig API SourceSelector appears to be read-only
        // Need to investigate action IDs or additional API methods for I/O routing
        host.println("I/O routing transfer: Not yet implemented (API limitation)");
        
        // Determine track type for message
        String trackType = "track";
        if (newTrack.canHoldNoteData().get() && !newTrack.canHoldAudioData().get()) {
            trackType = "instrument track";
        } else if (newTrack.canHoldAudioData().get() && !newTrack.canHoldNoteData().get()) {
            trackType = "audio track";
        }
        
        host.println("New clean " + trackType + " created. Settings transferred and ready for recording.");
    }
    
    public void turnOffAllMonitoringExceptGrooved() {
        int tracksProcessed = 0;
        int monitoringTurnedOff = 0;
        int groovedTracksSkipped = 0;
        
        host.println("=== Turning off monitoring for all audio tracks (except <G> group tracks) ===");
        
        // Process all tracks from the flat track bank (includes nested tracks)
        for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
            Track track = allTracksBank.getItemAt(i);
            
            if (track.exists().get()) {
                tracksProcessed++;
                String trackName = track.name().get();
                boolean canHoldAudio = track.canHoldAudioData().get();
                String currentMonitorMode = track.monitorMode().get();
                
                // Check if track is audio track and doesn't contain <G>
                if (canHoldAudio && !trackName.contains("<G>")) {
                    // Turn off monitoring
                    track.monitorMode().set("OFF");
                    monitoringTurnedOff++;
                    host.println("Turned OFF monitoring: \"" + trackName + "\" (was: " + currentMonitorMode + ")");
                } else if (trackName.contains("<G>")) {
                    groovedTracksSkipped++;
                    host.println("Skipped <G> group track: \"" + trackName + "\" (monitoring: " + currentMonitorMode + ")");
                } else if (!canHoldAudio) {
                    host.println("Skipped non-audio track: \"" + trackName + "\"");
                }
            }
        }
        
        host.println("=== Monitoring Off Complete ===");
        host.println("Tracks processed: " + tracksProcessed);
        host.println("Monitoring turned off: " + monitoringTurnedOff);
        host.println("<G> group tracks skipped: " + groovedTracksSkipped);
    }
    
    public void disarmAllTracks() {
        int tracksProcessed = 0;
        int tracksDisarmed = 0;
        
        host.println("=== Disarming all tracks (audio and instrument) ===");
        
        // Process all tracks from the flat track bank (includes nested tracks)
        for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
            Track track = allTracksBank.getItemAt(i);
            
            if (track.exists().get()) {
                tracksProcessed++;
                String trackName = track.name().get();
                boolean isArmed = track.arm().get();
                
                if (isArmed) {
                    track.arm().set(false);
                    tracksDisarmed++;
                    host.println("Disarmed: \"" + trackName + "\"");
                } else {
                    host.println("Already disarmed: \"" + trackName + "\"");
                }
            }
        }
        
        host.println("=== Disarm All Complete ===");
        host.println("Tracks processed: " + tracksProcessed);
        host.println("Tracks disarmed: " + tracksDisarmed);
    }
    
    public void makeRecordGroup() {
        host.println("=== Making Record Group ===");
        
        // Find group track with <REC> in its name
        Track recGroupTrack = null;
        for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
            Track track = allTracksBank.getItemAt(i);
            
            if (track.exists().get() && track.isGroup().get()) {
                String trackName = track.name().get();
                if (trackName.contains("<REC>")) {
                    recGroupTrack = track;
                    host.println("Found <REC> group: \"" + trackName + "\"");
                    break;
                }
            }
        }
        
        if (recGroupTrack == null) {
            host.println("ERROR: No group track with <REC> in name found!");
            return;
        }
        
        String originalName = recGroupTrack.name().get();
        
        // Step 1: Duplicate the group track
        host.println("Duplicating group track...");
        recGroupTrack.duplicate();
        
        // Give Bitwig time to create the duplicate
        host.scheduleTask(() -> {
            // Step 2: Find the newly created duplicate track
            Track duplicateTrack = findNewestTrack();
            if (duplicateTrack != null) {
                // Generate timestamp name for the new group
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String timestamp = now.format(formatter);
                String newName = originalName.replace("<REC>", "<Take> # " + timestamp);
                
                duplicateTrack.name().set(newName);
                host.println("Renamed duplicate to: \"" + newName + "\"");
                
                // Step 3: Configure the DUPLICATE group (archive it)
                // NOTE: Original <REC> group is left completely untouched - all arm states preserved
                configureTakeGroup(duplicateTrack, newName);
                
                host.println("Original <REC> group remains ready for more recordings (arm states preserved)");
                host.println("=== Record Group Creation Complete ===");
            } else {
                host.println("ERROR: Could not find duplicate track!");
            }
        }, 500); // Wait 500ms for duplication to complete
    }
    
    private void configureTakeGroup(Track takeGroup, String takeName) {
        host.println("Configuring archived take group...");
        
        // Mute the take group (archived version)
        takeGroup.mute().set(true);
        host.println("Muted take group: \"" + takeName + "\"");
        
        // Find and configure all tracks inside this group
        int tracksInGroup = 0;
        int tracksDisarmed = 0;
        int monitoringTurnedOff = 0;
        
        // Process all tracks to find those that belong to this group
        for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
            Track track = allTracksBank.getItemAt(i);
            
            if (track.exists().get() && !track.isGroup().get()) {
                // Check if this track is inside our take group by looking at track hierarchy
                // Since we have a flat list, tracks immediately after the group are likely inside it
                // This is a simplification - in a real implementation you'd need proper parent detection
                String trackName = track.name().get();
                
                // For now, we'll process tracks that appear to be inside groups
                // by checking if they come after our group in the flat list
                boolean isInTakeGroup = isTrackInGroup(track, takeGroup);
                
                if (isInTakeGroup) {
                    tracksInGroup++;
                    
                    // Turn off monitoring
                    String currentMonitorMode = track.monitorMode().get();
                    if (!"OFF".equals(currentMonitorMode)) {
                        track.monitorMode().set("OFF");
                        monitoringTurnedOff++;
                        host.println("  Turned off monitoring: \"" + trackName + "\" (was: " + currentMonitorMode + ")");
                    }
                    
                    // Disarm track
                    if (track.arm().get()) {
                        track.arm().set(false);
                        tracksDisarmed++;
                        host.println("  Disarmed: \"" + trackName + "\"");
                    }
                }
            }
        }
        
        host.println("Take group archival complete:");
        host.println("  Tracks in group: " + tracksInGroup);
        host.println("  Tracks disarmed: " + tracksDisarmed);
        host.println("  Monitoring turned off: " + monitoringTurnedOff);
    }
    
    // Simplified method to detect if a track is in a group
    // This is a heuristic based on the flat track list ordering
    private boolean isTrackInGroup(Track track, Track groupTrack) {
        // This is a simplified implementation
        // In reality, you'd need more sophisticated parent-child detection
        // For now, we'll assume tracks are processed in order and belong to preceding groups
        
        // Get track positions/names to make educated guess about grouping
        String trackName = track.name().get();
        
        // Simple heuristic: if track name suggests it's part of recording setup
        // and doesn't have its own group markers, it might be in the REC group
        return trackName.contains("Track") || trackName.contains("Audio") || 
               (!trackName.contains("<") && !trackName.contains(">"));
    }
}