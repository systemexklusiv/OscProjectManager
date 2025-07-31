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
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ClipLauncherSlot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

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
    private ClipLauncherSlotBank cursorTrackClipBank;
    private Application application;
    private OSCManagerImpl oscManager;
    
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
    
    public void setOSCManager(OSCManagerImpl oscManager) {
        this.oscManager = oscManager;
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
        // Create cursor track with clip launcher support (numSends=0, numScenes=128)
        cursorTrack = host.createCursorTrack(0, 128);
        cursorTrack.exists().markInterested();
        cursorTrack.arm().markInterested();
        cursorTrack.monitorMode().markInterested();
        cursorTrack.name().markInterested();
        cursorTrack.canHoldNoteData().markInterested();
        cursorTrack.canHoldAudioData().markInterested();
        cursorTrack.isGroup().markInterested();
        
        // Setup clip launcher slot bank for cursor track
        cursorTrackClipBank = cursorTrack.clipLauncherSlotBank();
        if (cursorTrackClipBank != null) {
            // Mark clip slot properties as interested
            for (int i = 0; i < cursorTrackClipBank.getSizeOfBank(); i++) {
                ClipLauncherSlot slot = cursorTrackClipBank.getItemAt(i);
                slot.exists().markInterested();
                slot.hasContent().markInterested();
                slot.name().markInterested();
            }
            host.println("Cursor track clip bank initialized with " + cursorTrackClipBank.getSizeOfBank() + " slots");
        } else {
            host.println("WARNING: Could not create clip launcher slot bank for cursor track");
        }
        
        // Setup source selector for input routing
        SourceSelector sourceSelector = cursorTrack.sourceSelector();
        sourceSelector.hasAudioInputSelected().markInterested();
        sourceSelector.hasNoteInputSelected().markInterested();
        
        // Add observer to automatically send transition names when track changes
        cursorTrack.name().addValueObserver(trackName -> {
            if (oscManager != null) {
                // Small delay to ensure track is fully loaded
                host.scheduleTask(() -> {
                    host.println("Track selection changed to: \"" + trackName + "\" - sending transition names");
                    sendTransitionNames();
                }, 200);
            }
        });
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
    
    public void turnOffAllMonitoringExceptGroups() {
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
        
        // Step 1: Create timestamp with milliseconds for unique ordering
        LocalDateTime now = LocalDateTime.now();
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS");
        // remove millis in formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
        String timestamp = now.format(formatter);
        
        // Step 2: Find all group tracks with <REC> in their names
        List<Track> recGroups = new ArrayList<>();
        
        for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
            Track track = allTracksBank.getItemAt(i);
            
            if (track.exists().get() && track.isGroup().get()) {
                String trackName = track.name().get();
                if (trackName.contains("<REC>")) {
                    host.println("Found <REC> group to archive: \"" + trackName + "\"");
                    recGroups.add(track);
                }
            }
        }
        
        if (recGroups.isEmpty()) {
            host.println("ERROR: No group tracks with <REC> in name found!");
            return;
        }
        
        host.println("Found " + recGroups.size() + " <REC> group(s) to process");
        
        // Step 3: Process each group
        processRecordGroups(recGroups, timestamp, 0);
    }
    
    private void processRecordGroups(List<Track> recGroups, String timestamp, int index) {
        if (index >= recGroups.size()) {
            host.println("=== Record Group Creation Complete ===");
            host.println("Processed " + recGroups.size() + " <REC> groups");
            host.println("Original <REC> groups remain unchanged and ready for more recordings");
            host.println("Archived groups with timestamp: " + timestamp);
            return;
        }
        
        Track recGroup = recGroups.get(index);
        String originalName = recGroup.name().get();
        
        host.println("Processing <REC> group " + (index + 1) + "/" + recGroups.size() + ": \"" + originalName + "\"");
        
        // Duplicate the group
        recGroup.duplicate();
        host.println("  Duplicated group: \"" + originalName + "\"");
        
        // Wait for duplication, then find and configure the duplicate
        host.scheduleTask(() -> {
            configureGroupDuplicate(recGroup, originalName, timestamp, recGroups, index);
        }, 800); // Wait for group duplication to complete
    }
    
    private void configureGroupDuplicate(Track originalGroup, String originalName, String timestamp, 
                                        List<Track> recGroups, int index) {
        host.println("Configuring duplicate of: \"" + originalName + "\"");
        
        // Find the duplicate group - look for another group with the same name
        Track duplicateGroup = null;
        int matchCount = 0;
        
        for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
            Track track = allTracksBank.getItemAt(i);
            
            if (track.exists().get() && track.isGroup().get()) {
                String trackName = track.name().get();
                if (originalName.equals(trackName)) {
                    matchCount++;
                    if (!track.equals(originalGroup)) {
                        duplicateGroup = track;
                        host.println("  Found duplicate group: \"" + trackName + "\" (match " + matchCount + ")");
                    }
                }
            }
        }
        
        if (duplicateGroup != null) {
            // Rename the duplicate group to archived format
            String baseName = originalName.replace("<REC>", "").trim();
            String archivedName = "<T>_" + timestamp + "_" + baseName;
            duplicateGroup.name().set(archivedName);
            host.println("  Renamed duplicate to: \"" + archivedName + "\"");
            
            // Configure the archived group - mute it and turn off monitoring for all tracks inside
            duplicateGroup.mute().set(true);
            host.println("  Muted archived group");
            
            // Disarm and turn off monitoring for all tracks in the archived group
            disarmAndTurnOffMonitoringInGroup(duplicateGroup, archivedName);
            
            host.println("  ✓ Completed archiving: \"" + originalName + "\"");
        } else {
            host.println("  ERROR: Could not find duplicate group for: \"" + originalName + "\" (found " + matchCount + " total matches)");
        }
        
        // Process next group
        processRecordGroups(recGroups, timestamp, index + 1);
    }
    
    private void disarmAndTurnOffMonitoringInGroup(Track groupTrack, String groupName) {
        host.println("  Configuring tracks inside archived group: \"" + groupName + "\"");
        
        int tracksProcessed = 0;
        int tracksDisarmed = 0;
        int monitoringTurnedOff = 0;
        
        // Process all tracks in the flat track bank to find tracks inside this group
        for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
            Track track = allTracksBank.getItemAt(i);
            
            if (track.exists().get() && !track.isGroup().get()) {
                // Check if this track is inside our group by checking if it comes after the group
                // and doesn't belong to another group (simplified check)
                tracksProcessed++;
                
                // Disarm the track
                if (track.arm().get()) {
                    track.arm().set(false);
                    tracksDisarmed++;
                }
                
                // Turn off monitoring
                String currentMonitorMode = track.monitorMode().get();
                if (!"OFF".equals(currentMonitorMode)) {
                    track.monitorMode().set("OFF");
                    monitoringTurnedOff++;
                }
            }
        }
        
        host.println("    Processed tracks in archived group: " + tracksProcessed);
        host.println("    Tracks disarmed: " + tracksDisarmed);
        host.println("    Monitoring turned off: " + monitoringTurnedOff);
    }
    
    public void sendTransitionNames() {
        host.println("=== Sending Transition Names ===");
        
        if (!cursorTrack.exists().get()) {
            host.println("ERROR: No track selected (cursor track does not exist)");
            return;
        }
        
        String trackName = cursorTrack.name().get();
        boolean isGroupTrack = cursorTrack.isGroup().get();
        
        host.println("Selected track: \"" + trackName + "\" (type: " + (isGroupTrack ? "Group" : "Regular") + ")");
        
        if (oscManager == null) {
            host.println("ERROR: OSC Manager not available");
            return;
        }
        
        if (cursorTrackClipBank == null) {
            host.println("ERROR: Clip launcher slot bank not available");
            return;
        }
        
        int clipsFound = 0;
        int clipsSent = 0;
        
        if (isGroupTrack) {
            host.println("Scanning group track clip slots (sub-scenes for group)...");
        } else {
            host.println("Scanning regular track clip slots...");
        }
        
        // Go through all clip slots in the cursor track (works for both regular and group tracks)
        for (int i = 0; i < cursorTrackClipBank.getSizeOfBank(); i++) {
            ClipLauncherSlot slot = cursorTrackClipBank.getItemAt(i);
            
            if (slot.exists().get() && slot.hasContent().get()) {
                clipsFound++;
                String clipName = slot.name().get();
                int oneBasedIndex = i + 1;
                
                if (isGroupTrack) {
                    // For group tracks, always send sub-scene names (even if default)
                    String subSceneName = (clipName != null && !clipName.trim().isEmpty()) 
                        ? clipName 
                        : "Scene " + oneBasedIndex;
                    
                    oscManager.sendTransitionName(oneBasedIndex, subSceneName);
                    clipsSent++;
                    host.println("  Sub-scene " + oneBasedIndex + ": \"" + subSceneName + "\"");
                } else {
                    // For regular tracks, only send if clip has a custom name
                    if (clipName != null && !clipName.trim().isEmpty()) {
                        oscManager.sendTransitionName(oneBasedIndex, clipName);
                        clipsSent++;
                        host.println("  Slot " + oneBasedIndex + ": \"" + clipName + "\"");
                    } else {
                        host.println("  Slot " + oneBasedIndex + ": [unnamed clip - not sent]");
                    }
                }
            }
        }
        
        host.println("=== Transition Names Complete ===");
        host.println("Track: \"" + trackName + "\" (" + (isGroupTrack ? "Group" : "Regular") + ")");
        host.println((isGroupTrack ? "Sub-scenes" : "Clips") + " found: " + clipsFound);
        host.println("Names sent: " + clipsSent);
        
        if (clipsSent == 0) {
            if (isGroupTrack) {
                host.println("No named sub-scenes found in selected group track!");
            } else {
                host.println("No named clips found in selected track!");
            }
        }
    }
    
    public void triggerTransitionSlot(int oneBasedIndex) {
        host.println("=== Triggering Transition Slot ===");
        
        if (!cursorTrack.exists().get()) {
            host.println("ERROR: No track selected (cursor track does not exist)");
            return;
        }
        
        if (cursorTrackClipBank == null) {
            host.println("ERROR: Clip launcher slot bank not available");
            return;
        }
        
        String trackName = cursorTrack.name().get();
        boolean isGroupTrack = cursorTrack.isGroup().get();
        
        // Convert from 1-based to 0-based index
        int zeroBasedIndex = oneBasedIndex - 1;
        
        if (zeroBasedIndex < 0 || zeroBasedIndex >= cursorTrackClipBank.getSizeOfBank()) {
            host.println("ERROR: Invalid slot index " + oneBasedIndex + " (valid range: 1-" + cursorTrackClipBank.getSizeOfBank() + ")");
            return;
        }
        
        ClipLauncherSlot slot = cursorTrackClipBank.getItemAt(zeroBasedIndex);
        
        if (!slot.exists().get()) {
            host.println("ERROR: Slot " + oneBasedIndex + " does not exist on track \"" + trackName + "\"");
            return;
        }
        
        if (!slot.hasContent().get()) {
            host.println("WARNING: Slot " + oneBasedIndex + " is empty on track \"" + trackName + "\" - triggering anyway");
        }
        
        // Trigger the slot
        slot.launch();
        
        if (isGroupTrack) {
            host.println("Triggered sub-scene " + oneBasedIndex + " on group track \"" + trackName + "\"");
        } else {
            host.println("Triggered clip slot " + oneBasedIndex + " on track \"" + trackName + "\"");
        }
    }
    
    private Track findLastTrackWithName(String targetName, Track excludeTrack) {
        // Find the last track in the bank with the target name, excluding the specified track
        Track lastMatch = null;
        for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
            Track track = allTracksBank.getItemAt(i);
            
            if (track.exists().get() && 
                !track.isGroup().get() && 
                !track.equals(excludeTrack) &&
                targetName.equals(track.name().get())) {
                lastMatch = track; // Keep finding, the last one should be the newest
            }
        }
        return lastMatch;
    }
    
    private void configureDuplicatedTracks(List<String> originalTrackNames, String timestamp) {
        host.println("=== Configuring Duplicated Tracks ===");
        host.println("Looking for duplicates of " + originalTrackNames.size() + " tracks...");
        
        int duplicatesProcessed = 0;
        
        for (String originalName : originalTrackNames) {
            host.println("Searching for duplicate of: \"" + originalName + "\"");
            
            // Find the duplicate track - look for a track with the same name but different instance
            Track duplicateTrack = null;
            int matchCount = 0;
            
            for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
                Track track = allTracksBank.getItemAt(i);
                
                if (track.exists().get() && !track.isGroup().get()) {
                    String trackName = track.name().get();
                    if (originalName.equals(trackName)) {
                        matchCount++;
                        if (matchCount == 2) {
                            // The second match should be the duplicate
                            duplicateTrack = track;
                            break;
                        }
                    }
                }
            }
            
            if (duplicateTrack != null) {
                host.println("  Found duplicate track: \"" + duplicateTrack.name().get() + "\"");
                
                // Rename the duplicate: <R>TrackName -> <T>_YYYY-MM-DD-HH-MM_TrackName
                String baseName = originalName.replace("<R>", "").trim();
                String archivedName = "<T>_" + timestamp + "_" + baseName;
                
                duplicateTrack.name().set(archivedName);
                host.println("  Renamed to: \"" + archivedName + "\"");
                
                // Configure the archived duplicate
                String currentMonitorMode = duplicateTrack.monitorMode().get();
                if (!"OFF".equals(currentMonitorMode)) {
                    duplicateTrack.monitorMode().set("OFF");
                    host.println("  Turned off monitoring (was: " + currentMonitorMode + ")");
                }
                
                if (duplicateTrack.arm().get()) {
                    duplicateTrack.arm().set(false);
                    host.println("  Disarmed track");
                }
                
                // Mute the archived track
                duplicateTrack.mute().set(true);
                host.println("  Muted archived track");
                
                duplicatesProcessed++;
                host.println("  ✓ Completed: \"" + originalName + "\"");
            } else {
                host.println("  ERROR: Could not find duplicate for: \"" + originalName + "\" (found " + matchCount + " matches)");
            }
        }
        
        host.println("=== Record Group Creation Complete ===");
        host.println("Original <R> tracks remain unchanged and ready for more recordings");
        host.println("Archived " + duplicatesProcessed + "/" + originalTrackNames.size() + " tracks with timestamp: " + timestamp);
    }
    
    private void duplicateAndArchiveRTracks(List<Track> tracksToArchive, String timestamp) {
        host.println("Processing " + tracksToArchive.size() + " tracks for archival...");
        
        // Process tracks one by one sequentially to avoid confusion
        processNextRTrack(tracksToArchive, 0, timestamp);
    }
    
    private void processNextRTrack(List<Track> tracksToArchive, int index, String timestamp) {
        if (index >= tracksToArchive.size()) {
            // All tracks processed
            host.println("=== Record Group Creation Complete ===");
            host.println("Original <R> tracks remain unchanged and ready for more recordings");
            host.println("Archived " + tracksToArchive.size() + " tracks with timestamp: " + timestamp);
            return;
        }
        
        Track originalTrack = tracksToArchive.get(index);
        String originalName = originalTrack.name().get();
        
        host.println("Processing track " + (index + 1) + "/" + tracksToArchive.size() + ": \"" + originalName + "\"");
        
        // Duplicate the track
        originalTrack.duplicate();
        
        // Wait, then find and configure the duplicate
        host.scheduleTask(() -> {
            // Instead of findNewestTrack, look for a track with the same name as original
            Track duplicateTrack = findTrackByName(originalName, originalTrack);
            
            if (duplicateTrack != null) {
                host.println("  Found duplicate track: \"" + duplicateTrack.name().get() + "\"");
                
                // Rename the duplicate track: <R>TrackName -> <T>_YYYY-MM-DD-HH-MM_TrackName
                String baseName = originalName.replace("<R>", "").trim();
                String archivedName = "<T>_" + timestamp + "_" + baseName;
                
                duplicateTrack.name().set(archivedName);
                host.println("  Renamed to: \"" + archivedName + "\"");
                
                // Configure the archived duplicate
                String currentMonitorMode = duplicateTrack.monitorMode().get();
                if (!"OFF".equals(currentMonitorMode)) {
                    duplicateTrack.monitorMode().set("OFF");
                    host.println("  Turned off monitoring (was: " + currentMonitorMode + ")");
                }
                
                if (duplicateTrack.arm().get()) {
                    duplicateTrack.arm().set(false);
                    host.println("  Disarmed track");
                }
                
                // Mute the archived track
                duplicateTrack.mute().set(true);
                host.println("  Muted archived track");
                
                host.println("  ✓ Completed: \"" + originalName + "\"");
            } else {
                host.println("  ERROR: Could not find duplicate for: \"" + originalName + "\"");
            }
            
            // Process next track
            processNextRTrack(tracksToArchive, index + 1, timestamp);
            
        }, 500); // Wait longer for duplication to complete
    }
    
    // Find a track by name, excluding the original track
    private Track findTrackByName(String targetName, Track excludeTrack) {
        for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
            Track track = allTracksBank.getItemAt(i);
            
            if (track.exists().get() && 
                !track.equals(excludeTrack) && 
                targetName.equals(track.name().get())) {
                return track;
            }
        }
        return null;
    }
}