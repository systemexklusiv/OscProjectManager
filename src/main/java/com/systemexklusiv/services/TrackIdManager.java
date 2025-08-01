package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.ControllerHost;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TrackIdManager {
    
    private ControllerHost host;
    private ProjectDiscoveryService projectDiscoveryService;
    
    // Pattern to match $id$ in track names - accepts any ASCII characters except $
    private static final Pattern TRACK_ID_PATTERN = Pattern.compile("\\$([^$]+)\\$");
    
    public void initialize(ControllerHost host, ProjectDiscoveryService projectDiscoveryService) {
        this.host = host;
        this.projectDiscoveryService = projectDiscoveryService;
        host.println("TrackIdManager initialized");
    }
    
    public boolean checkForDuplicateIds() {
        List<TrackSnapshot> tracksWithIds = getTracksWithIds();
        Map<String, String> duplicateCheck = new HashMap<>();
        boolean foundDuplicates = false;
        
        host.println("=== Duplicate ID Check ===");
        
        for (TrackSnapshot track : tracksWithIds) {
            String trackId = extractTrackId(track.trackName);
            if (trackId != null) {
                if (duplicateCheck.containsKey(trackId)) {
                    // Duplicate found!
                    if (!foundDuplicates) {
                        host.errorln("DUPLICATE TRACK IDs DETECTED!");
                        foundDuplicates = true;
                    }
                    String existingTrack = duplicateCheck.get(trackId);
                    host.errorln("ERROR: ID $" + trackId + "$ used by multiple tracks:");
                    host.errorln("   - \"" + existingTrack + "\"");
                    host.errorln("   - \"" + track.trackName + "\"");
                } else {
                    duplicateCheck.put(trackId, track.trackName);
                }
            }
        }
        
        if (foundDuplicates) {
            host.errorln("WARNING: Please fix duplicate IDs before saving snapshots!");
            host.println("TIP: Use unique IDs like $bass$, $lead$, $drums$, etc.");
        } else {
            host.println("OK: No duplicate IDs found - all track IDs are unique");
        }
        
        host.println("=== Duplicate Check Complete ===");
        return !foundDuplicates;
    }
    
    public String extractTrackId(String trackName) {
        if (trackName == null || trackName.trim().isEmpty()) {
            return null;
        }
        
        Matcher matcher = TRACK_ID_PATTERN.matcher(trackName);
        if (matcher.find()) {
            String id = matcher.group(1);
            if (id != null && !id.trim().isEmpty()) {
                return id.trim(); // Return the ID string as-is
            }
        }
        return null; // No ID found
    }
    
    public boolean hasTrackId(String trackName) {
        return extractTrackId(trackName) != null;
    }
    
    public List<TrackSnapshot> getTracksWithIds() {
        projectDiscoveryService.spiderCurrentProject();
        List<TrackSnapshot> allTracks = projectDiscoveryService.getDiscoveredTracks();
        List<TrackSnapshot> tracksWithIds = new ArrayList<>();
        
        for (TrackSnapshot track : allTracks) {
            if (hasTrackId(track.trackName)) {
                tracksWithIds.add(track);
            }
        }
        
        return tracksWithIds;
    }
    
    public Map<String, TrackSnapshot> buildTrackIdMap() {
        List<TrackSnapshot> tracksWithIds = getTracksWithIds();
        Map<String, TrackSnapshot> idMap = new HashMap<>();
        Map<String, String> duplicateCheck = new HashMap<>();
        
        for (TrackSnapshot track : tracksWithIds) {
            String trackId = extractTrackId(track.trackName);
            if (trackId != null) {
                if (idMap.containsKey(trackId)) {
                    // Duplicate ID found
                    String existingTrack = duplicateCheck.get(trackId);
                    host.errorln("WARNING: Duplicate track ID $" + trackId + "$ found!");
                    host.errorln("  Existing: \"" + existingTrack + "\"");
                    host.errorln("  Duplicate: \"" + track.trackName + "\"");
                    host.errorln("  Using first occurrence, ignoring duplicate.");
                } else {
                    idMap.put(trackId, track);
                    duplicateCheck.put(trackId, track.trackName);
                }
            }
        }
        
        return idMap;
    }
    
    public void printTrackIdReport() {
        host.println("=== Track ID Report ===");
        
        projectDiscoveryService.spiderCurrentProject();
        List<TrackSnapshot> allTracks = projectDiscoveryService.getDiscoveredTracks();
        Map<String, TrackSnapshot> idMap = buildTrackIdMap();
        
        int tracksWithIds = 0;
        int tracksWithoutIds = 0;
        
        host.println("Tracks WITH IDs (included in snapshots):");
        for (TrackSnapshot track : allTracks) {
            String trackId = extractTrackId(track.trackName);
            if (trackId != null) {
                tracksWithIds++;
                String typeIndicator = getTypeIndicator(track.trackType);
                host.println(String.format("  ID %-8s: %s \"%s\"", 
                    "$" + trackId + "$", typeIndicator, track.trackName));
            }
        }
        
        host.println("Tracks WITHOUT IDs (excluded from snapshots):");
        for (TrackSnapshot track : allTracks) {
            if (!hasTrackId(track.trackName)) {
                tracksWithoutIds++;
                String typeIndicator = getTypeIndicator(track.trackType);
                host.println(String.format("        %s \"%s\"", 
                    typeIndicator, track.trackName));
            }
        }
        
        host.println("=== Summary ===");
        host.println("Tracks included: " + tracksWithIds);
        host.println("Tracks excluded: " + tracksWithoutIds);
        host.println("Total tracks: " + allTracks.size());
        
        // Show all used IDs (no gap checking for string IDs)
        if (!idMap.isEmpty()) {
            host.println("Used IDs: " + idMap.keySet().toString());
        }
        
        host.println("=== Track ID Report Complete ===");
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
    
    public String generateSuggestedId(String trackName) {
        if (hasTrackId(trackName)) {
            return trackName; // Already has ID
        } else {
            // Generate a simple ID based on track name
            String baseId = trackName.toLowerCase()
                .replaceAll("[^a-z0-9]", "") // Remove special chars
                .substring(0, Math.min(trackName.length(), 8)); // Max 8 chars
            
            Map<String, TrackSnapshot> idMap = buildTrackIdMap();
            String suggestedId = baseId;
            int suffix = 1;
            
            // Find unique ID
            while (idMap.containsKey(suggestedId)) {
                suggestedId = baseId + suffix;
                suffix++;
            }
            
            return trackName + " $" + suggestedId + "$";
        }
    }
}