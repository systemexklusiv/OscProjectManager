package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableStringValue;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class SnapshotManager {
    
    private ControllerHost host;
    private Preferences preferences;
    private ProjectDiscoveryService projectDiscoveryService;
    private String snapshotDirectoryPath;
    
    // In-memory storage for quick access
    private Map<Integer, ProjectSnapshot> memorySnapshots;
    
    // Bitwig preferences for slot metadata
    private SettableStringValue[] slotNames;
    private SettableStringValue[] slotTimestamps;
    private SettableStringValue projectNameSetting;
    
    // Current project name for JSON file naming
    private String currentProjectName = "unknown_project";
    
    public void initialize(ControllerHost host, Preferences preferences, ProjectDiscoveryService projectDiscoveryService, String snapshotDirectoryPath) {
        this.host = host;
        this.preferences = preferences;
        this.projectDiscoveryService = projectDiscoveryService;
        this.snapshotDirectoryPath = snapshotDirectoryPath != null ? snapshotDirectoryPath : "snapshots";
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
        host.println("TODO: Implement track state restoration");
        
        // TODO: Implement actual track state restoration
        // This would involve:
        // 1. Match tracks by name (primary) or position (fallback)
        // 2. Set volume, pan, mute, arm, monitor mode
        // 3. Set send levels for each track
        // 4. Log any tracks that couldn't be matched
        
        return true;
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