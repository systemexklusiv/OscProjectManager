package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.ControllerHost;
import java.util.List;

public class SnapshotService {
    
    private ControllerHost host;
    private ProjectDiscoveryService projectDiscoveryService;
    private TrackIdManager trackIdManager;
    
    public void initialize(ControllerHost host, ProjectDiscoveryService projectDiscoveryService) {
        this.host = host;
        this.projectDiscoveryService = projectDiscoveryService;
        
        // Initialize track ID manager
        this.trackIdManager = new TrackIdManager();
        this.trackIdManager.initialize(host, projectDiscoveryService);
        
        host.println("SnapshotService initialized");
    }
    
    public void printCurrentTrackSnapshots() {
        host.println("=== Current Track Snapshots ===");
        
        // First, discover current project state
        projectDiscoveryService.spiderCurrentProject();
        
        // Get all discovered tracks
        List<TrackSnapshot> allTracks = projectDiscoveryService.getDiscoveredTracks();
        
        if (allTracks.isEmpty()) {
            host.println("No tracks found in current project!");
            return;
        }
        
        // Count included tracks
        int includedCount = 0;
        for (TrackSnapshot track : allTracks) {
            if (trackIdManager.hasTrackId(track.trackName)) {
                includedCount++;
            }
        }
        
        // Print summary
        host.println("Found " + allTracks.size() + " tracks (" + includedCount + " included in snapshots, " + 
                     (allTracks.size() - includedCount) + " excluded):");
        host.println("----------------------------------------");
        
        // Print each track with inclusion status
        for (int i = 0; i < allTracks.size(); i++) {
            TrackSnapshot track = allTracks.get(i);
            boolean included = trackIdManager.hasTrackId(track.trackName);
            String trackId = trackIdManager.extractTrackId(track.trackName);
            printTrackSnapshotWithInclusion(i, track, included, trackId);
        }
        
        host.println("----------------------------------------");
        if (includedCount == 0) {
            host.println("WARNING: No tracks have IDs - snapshots will be empty!");
            host.println("TIP: Add track IDs using format: 'Track Name $id$'");
        }
        host.println("=== Track Snapshots Complete ===");
    }
    
    private void printTrackSnapshotWithInclusion(int index, TrackSnapshot track, boolean included, String trackId) {
        String typeIndicator = getTypeIndicator(track.trackType);
        String muteIndicator = track.muted ? "[MUTED]" : "";
        String armIndicator = track.armed ? "[ARMED]" : "";
        String monitorIndicator = getMonitorIndicator(track.monitorMode);
        
        // Inclusion indicator
        String inclusionIndicator = included ? "O" : "-";
        String idInfo = included ? " (ID: $" + trackId + "$)" : " (no ID)";
        
        // Basic track info with inclusion status
        host.println(String.format("  %s %2d: %-20s %s Vol:%.2f Pan:%+.2f %s%s%s%s", 
            inclusionIndicator,
            index,
            "\"" + track.trackName + "\"",
            typeIndicator,
            track.volume,
            track.pan,
            muteIndicator,
            armIndicator,
            monitorIndicator,
            idInfo
        ));
        
        // Show active sends (only if > 0.0 and track is included)
        if (included) {
            StringBuilder sendInfo = new StringBuilder();
            boolean hasActiveSends = false;
            for (int s = 0; s < track.sendLevels.length; s++) {
                if (track.sendLevels[s] > 0.0) {
                    if (hasActiveSends) sendInfo.append(", ");
                    sendInfo.append(String.format("S%d:%.2f", s, track.sendLevels[s]));
                    hasActiveSends = true;
                }
            }
            
            if (hasActiveSends) {
                host.println(String.format("       Sends: %s", sendInfo.toString()));
            }
        }
    }
    
    private void printTrackSnapshot(int index, TrackSnapshot track) {
        String typeIndicator = getTypeIndicator(track.trackType);
        String muteIndicator = track.muted ? "[MUTED]" : "";
        String armIndicator = track.armed ? "[ARMED]" : "";
        String monitorIndicator = getMonitorIndicator(track.monitorMode);
        
        // Basic track info
        host.println(String.format("  %2d: %-20s %s Vol:%.2f Pan:%+.2f %s%s%s", 
            index,
            "\"" + track.trackName + "\"",
            typeIndicator,
            track.volume,
            track.pan,
            muteIndicator,
            armIndicator,
            monitorIndicator
        ));
        
        // Show active sends (only if > 0.0)
        StringBuilder sendInfo = new StringBuilder();
        boolean hasActiveSends = false;
        for (int s = 0; s < track.sendLevels.length; s++) {
            if (track.sendLevels[s] > 0.0) {
                if (hasActiveSends) sendInfo.append(", ");
                sendInfo.append(String.format("S%d:%.2f", s, track.sendLevels[s]));
                hasActiveSends = true;
            }
        }
        
        if (hasActiveSends) {
            host.println(String.format("      Sends: %s", sendInfo.toString()));
        }
    }
    
    private String getMonitorIndicator(String monitorMode) {
        if (monitorMode == null) return "[MON:NULL]";
        switch (monitorMode) {
            case "IN": return "[MON:IN]";
            case "AUTO": return "[MON:AUTO]";
            case "OFF": return "[MON:OFF]";
            default: return "[MON:" + monitorMode + "]";
        }
    }
    
    private String getTypeIndicator(String trackType) {
        switch (trackType) {
            case "audio":      return "[AUD]";
            case "instrument": return "[INS]";
            case "group":      return "[GRP]";
            case "master":     return "[MST]";
            case "hybrid":     return "[HYB]";
            default:           return "[???]";
        }
    }
}