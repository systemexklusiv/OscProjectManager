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
}