package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.ControllerHost;
import java.util.List;

public class SnapshotService {
    
    private ControllerHost host;
    private ProjectDiscoveryService projectDiscoveryService;
    
    public void initialize(ControllerHost host, ProjectDiscoveryService projectDiscoveryService) {
        this.host = host;
        this.projectDiscoveryService = projectDiscoveryService;
        
        host.println("SnapshotService initialized");
    }
    
    public void printCurrentTrackSnapshots() {
        host.println("=== Current Track Snapshots ===");
        
        // First, discover current project state
        projectDiscoveryService.spiderCurrentProject();
        
        // Get the discovered tracks
        List<TrackSnapshot> tracks = projectDiscoveryService.getDiscoveredTracks();
        
        if (tracks.isEmpty()) {
            host.println("No tracks found in current project!");
            return;
        }
        
        // Print header
        host.println("Found " + tracks.size() + " tracks:");
        host.println("----------------------------------------");
        
        // Print each track snapshot
        for (int i = 0; i < tracks.size(); i++) {
            TrackSnapshot track = tracks.get(i);
            printTrackSnapshot(i, track);
        }
        
        host.println("----------------------------------------");
        host.println("=== Track Snapshots Complete ===");
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