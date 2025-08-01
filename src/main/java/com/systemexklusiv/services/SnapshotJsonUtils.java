package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.ControllerHost;
import java.util.List;
import java.io.*;
import java.nio.file.*;

public class SnapshotJsonUtils {
    
    public static String trackSnapshotToJson(TrackSnapshot track) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"trackName\": \"").append(escapeJson(track.trackName)).append("\",\n");
        json.append("  \"trackType\": \"").append(track.trackType).append("\",\n");
        json.append("  \"volume\": ").append(track.volume).append(",\n");
        json.append("  \"pan\": ").append(track.pan).append(",\n");
        json.append("  \"muted\": ").append(track.muted).append(",\n");
        json.append("  \"armed\": ").append(track.armed).append(",\n");
        json.append("  \"trackPosition\": ").append(track.trackPosition).append(",\n");
        json.append("  \"monitorMode\": \"").append(track.monitorMode != null ? track.monitorMode : "OFF").append("\",\n");
        
        // Send levels array
        json.append("  \"sendLevels\": [");
        for (int i = 0; i < track.sendLevels.length; i++) {
            if (i > 0) json.append(", ");
            json.append(track.sendLevels[i]);
        }
        json.append("]\n");
        
        json.append("}");
        return json.toString();
    }
    
    public static String projectSnapshotToJson(ProjectSnapshot snapshot) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(snapshot.timestamp).append("\",\n");
        json.append("  \"snapshotName\": \"").append(escapeJson(snapshot.snapshotName)).append("\",\n");
        json.append("  \"tracks\": [\n");
        
        for (int i = 0; i < snapshot.tracks.size(); i++) {
            if (i > 0) json.append(",\n");
            String trackJson = trackSnapshotToJson(snapshot.tracks.get(i));
            // Indent each line of track JSON
            json.append(indentJson(trackJson, 4));
        }
        
        json.append("\n  ]\n");
        json.append("}");
        return json.toString();
    }
    
    public static void saveSnapshotToFile(ControllerHost host, String snapshotDirectory, String projectName, int slotIndex, ProjectSnapshot snapshot) {
        try {
            // Create snapshots directory if it doesn't exist
            Path snapshotsDir = Paths.get(snapshotDirectory);
            if (!Files.exists(snapshotsDir)) {
                Files.createDirectories(snapshotsDir);
                host.println("Created snapshots directory: " + snapshotsDir.toAbsolutePath());
            }
            
            // Generate filename
            String filename = String.format("%s_slot_%d.json", sanitizeFilename(projectName), slotIndex);
            Path filePath = snapshotsDir.resolve(filename);
            
            // Convert to JSON and save
            String jsonContent = projectSnapshotToJson(snapshot);
            Files.write(filePath, jsonContent.getBytes("UTF-8"));
            
            host.println("Saved snapshot to: " + filePath.toAbsolutePath());
            
        } catch (Exception e) {
            host.errorln("Failed to save snapshot to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static ProjectSnapshot loadSnapshotFromFile(ControllerHost host, String snapshotDirectory, String projectName, int slotIndex) {
        try {
            String filename = String.format("%s_slot_%d.json", sanitizeFilename(projectName), slotIndex);
            Path filePath = Paths.get(snapshotDirectory, filename);
            
            if (!Files.exists(filePath)) {
                host.println("Snapshot file not found: " + filePath);
                return null;
            }
            
            // For now, just log that we found the file
            // Full JSON parsing would require a JSON library or manual parsing
            byte[] content = Files.readAllBytes(filePath);
            host.println("Found snapshot file: " + filePath + " (" + content.length + " bytes)");
            host.println("JSON parsing not yet implemented - returning null");
            
            return null; // TODO: Implement JSON parsing
            
        } catch (Exception e) {
            host.errorln("Failed to load snapshot from file: " + e.getMessage());
            return null;
        }
    }
    
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private static String indentJson(String json, int spaces) {
        String indent = " ".repeat(spaces);
        return indent + json.replace("\n", "\n" + indent);
    }
    
    private static String sanitizeFilename(String filename) {
        if (filename == null) return "unknown_project";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}