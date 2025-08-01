package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableStringValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Track;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class SnapshotManager {
    
    private ControllerHost host;
    private Preferences preferences;
    private ProjectDiscoveryService projectDiscoveryService;
    private String snapshotDirectoryPath;
    private TrackBank allTracksBank;
    
    // In-memory storage for quick access
    private Map<Integer, ProjectSnapshot> memorySnapshots;
    
    // Bitwig preferences for slot metadata
    private SettableStringValue[] slotNames;
    private SettableStringValue[] slotTimestamps;
    private SettableStringValue projectNameSetting;
    
    // Current project name for JSON file naming
    private String currentProjectName = "unknown_project";
    
    public void initialize(ControllerHost host, Preferences preferences, ProjectDiscoveryService projectDiscoveryService, String snapshotDirectoryPath, TrackBank allTracksBank) {
        this.host = host;
        this.preferences = preferences;
        this.projectDiscoveryService = projectDiscoveryService;
        this.snapshotDirectoryPath = snapshotDirectoryPath != null ? snapshotDirectoryPath : "snapshots";
        this.allTracksBank = allTracksBank;
        this.memorySnapshots = new HashMap<>();
        
        setupPreferences();
        host.println("SnapshotManager initialized with dual storage (Preferences + JSON)");
        host.println("Snapshot directory: " + this.snapshotDirectoryPath);
    }
    
    public void updateSnapshotPath(String snapshotDirectoryPath) {
        this.snapshotDirectoryPath = snapshotDirectoryPath != null ? snapshotDirectoryPath : "snapshots";
        host.println("Snapshot directory updated to: " + this.snapshotDirectoryPath);
    }
    
    private void setupPreferences() {
        // Setup slot metadata storage in Bitwig preferences
        slotNames = new SettableStringValue[ProjectSnapshot.MAX_SNAPSHOT_SLOTS];
        slotTimestamps = new SettableStringValue[ProjectSnapshot.MAX_SNAPSHOT_SLOTS];
        
        for (int i = 0; i < ProjectSnapshot.MAX_SNAPSHOT_SLOTS; i++) {
            slotNames[i] = preferences.getStringSetting(
                "Slot " + i + " Name", "Snapshots", 16, "Empty");
            slotNames[i].markInterested();
            
            slotTimestamps[i] = preferences.getStringSetting(
                "Slot " + i + " Timestamp", "Snapshots", 32, "");
            slotTimestamps[i].markInterested();
        }
        
        // Store current project name for JSON file naming
        projectNameSetting = preferences.getStringSetting(
            "Current Project", "Snapshots", 64, "unknown_project");
        projectNameSetting.markInterested();
        
        // Update project name when it changes
        projectNameSetting.addValueObserver(newName -> {
            if (newName != null && !newName.trim().isEmpty()) {
                currentProjectName = newName.trim();
                host.println("Project name updated to: " + currentProjectName);
            }
        });
    }
    
    public boolean saveSnapshot(int slotIndex, String snapshotName) {
        if (!isValidSlotIndex(slotIndex)) {
            host.errorln("Invalid snapshot slot index: " + slotIndex + " (valid: 0-" + (ProjectSnapshot.MAX_SNAPSHOT_SLOTS - 1) + ")");
            return false;
        }
        
        host.println("=== Saving Snapshot to Slot " + slotIndex + " ===");
        
        try {
            // Discover current project state
            projectDiscoveryService.spiderCurrentProject();
            List<TrackSnapshot> tracks = projectDiscoveryService.getDiscoveredTracks();
            
            if (tracks.isEmpty()) {
                host.errorln("No tracks found - cannot save empty snapshot");
                return false;
            }
            
            // Create project snapshot
            ProjectSnapshot snapshot = new ProjectSnapshot(tracks, snapshotName);
            
            // Store in memory for quick access
            memorySnapshots.put(slotIndex, snapshot);
            
            // Store metadata in Bitwig preferences
            slotNames[slotIndex].set(snapshotName);
            slotTimestamps[slotIndex].set(snapshot.timestamp);
            
            // Store full data in JSON file
            SnapshotJsonUtils.saveSnapshotToFile(host, snapshotDirectoryPath, currentProjectName, slotIndex, snapshot);
            
            host.println("Snapshot saved successfully:");
            host.println("  Slot: " + slotIndex);
            host.println("  Name: \"" + snapshotName + "\"");
            host.println("  Tracks: " + tracks.size());
            host.println("  Storage: Memory + Preferences + JSON file");
            
            return true;
            
        } catch (Exception e) {
            host.errorln("Failed to save snapshot: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean recallSnapshot(int slotIndex) {
        if (!isValidSlotIndex(slotIndex)) {
            host.errorln("Invalid snapshot slot index: " + slotIndex);
            return false;
        }
        
        host.println("=== Recalling Snapshot from Slot " + slotIndex + " ===");
        
        // Try to get from memory first
        ProjectSnapshot snapshot = memorySnapshots.get(slotIndex);
        if (snapshot == null) {
            // Try to load from JSON file
            snapshot = SnapshotJsonUtils.loadSnapshotFromFile(host, snapshotDirectoryPath, currentProjectName, slotIndex);
        }
        
        if (snapshot == null) {
            host.errorln("No snapshot found in slot " + slotIndex);
            return false;
        }
        
        host.println("Found snapshot: \"" + snapshot.snapshotName + "\" (" + snapshot.getTrackCount() + " tracks)");
        
        // Restore track states
        return restoreTrackStates(snapshot);
    }
    
    private boolean restoreTrackStates(ProjectSnapshot snapshot) {
        host.println("=== Restoring Track States ===");
        
        int tracksRestored = 0;
        int tracksNotFound = 0;
        int tracksWithErrors = 0;
        
        for (TrackSnapshot trackSnapshot : snapshot.tracks) {
            try {
                Track matchedTrack = findMatchingTrack(trackSnapshot);
                
                if (matchedTrack != null) {
                    restoreTrackState(matchedTrack, trackSnapshot);
                    tracksRestored++;
                    host.println("✓ Restored: \"" + trackSnapshot.trackName + "\"");
                } else {
                    tracksNotFound++;
                    host.println("✗ Not found: \"" + trackSnapshot.trackName + "\"");
                }
                
            } catch (Exception e) {
                tracksWithErrors++;
                host.errorln("✗ Error restoring \"" + trackSnapshot.trackName + "\": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        host.println("=== Restore Complete ===");
        host.println("Tracks restored: " + tracksRestored);
        host.println("Tracks not found: " + tracksNotFound);
        host.println("Tracks with errors: " + tracksWithErrors);
        
        return tracksRestored > 0;
    }
    
    private Track findMatchingTrack(TrackSnapshot trackSnapshot) {
        // First try to match by name (primary method)
        for (int i = 0; i < allTracksBank.getSizeOfBank(); i++) {
            Track track = allTracksBank.getItemAt(i);
            if (track.exists().get() && trackSnapshot.trackName.equals(track.name().get())) {
                return track;
            }
        }
        
        // Fallback: try to match by position if within bounds
        if (trackSnapshot.trackPosition >= 0 && trackSnapshot.trackPosition < allTracksBank.getSizeOfBank()) {
            Track track = allTracksBank.getItemAt(trackSnapshot.trackPosition);
            if (track.exists().get()) {
                host.println("  Matched by position: \"" + trackSnapshot.trackName + "\" -> \"" + track.name().get() + "\"");
                return track;
            }
        }
        
        return null; // No match found
    }
    
    private void restoreTrackState(Track track, TrackSnapshot trackSnapshot) {
        // Restore basic mixer properties
        track.volume().set(trackSnapshot.volume);
        track.pan().set(trackSnapshot.pan);
        track.mute().set(trackSnapshot.muted);
        track.arm().set(trackSnapshot.armed);
        
        // Restore monitor mode
        if (trackSnapshot.monitorMode != null) {
            track.monitorMode().set(trackSnapshot.monitorMode);
        }
        
        // Restore send levels
        if (trackSnapshot.sendLevels != null) {
            for (int s = 0; s < Math.min(trackSnapshot.sendLevels.length, TrackSnapshot.SEND_BANK_SIZE); s++) {
                try {
                    track.sendBank().getItemAt(s).set(trackSnapshot.sendLevels[s]);
                } catch (Exception e) {
                    host.errorln("  Error setting send " + s + " on \"" + trackSnapshot.trackName + "\": " + e.getMessage());
                }
            }
        }
        
        // Log detailed restoration info
        host.println("  Vol:" + String.format("%.2f", trackSnapshot.volume) + 
                     " Pan:" + String.format("%+.2f", trackSnapshot.pan) + 
                     " Mute:" + trackSnapshot.muted + 
                     " Arm:" + trackSnapshot.armed + 
                     " Mon:" + (trackSnapshot.monitorMode != null ? trackSnapshot.monitorMode : "NULL"));
    }
    
    public void listSnapshots() {
        host.println("=== Snapshot Slots Overview ===");
        
        for (int i = 0; i < ProjectSnapshot.MAX_SNAPSHOT_SLOTS; i++) {
            String name = slotNames[i].get();
            String timestamp = slotTimestamps[i].get();
            boolean hasMemory = memorySnapshots.containsKey(i);
            
            if (name.equals("Empty") || timestamp.isEmpty()) {
                host.println("  Slot " + i + ": [Empty]");
            } else {
                host.println("  Slot " + i + ": \"" + name + "\" (" + timestamp + ")" + 
                           (hasMemory ? " [In Memory]" : " [File Only]"));
            }
        }
        
        host.println("Project: " + currentProjectName);
    }
    
    public void setProjectName(String projectName) {
        if (projectName != null && !projectName.trim().isEmpty()) {
            currentProjectName = projectName.trim();
            projectNameSetting.set(currentProjectName);
            host.println("Project name set to: " + currentProjectName);
        }
    }
    
    private boolean isValidSlotIndex(int slotIndex) {
        return slotIndex >= 0 && slotIndex < ProjectSnapshot.MAX_SNAPSHOT_SLOTS;
    }
}