package com.systemexklusiv.services;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProjectSnapshot {
    
    public static final int MAX_SNAPSHOT_SLOTS = 5;
    
    public String timestamp;
    public String snapshotName;
    public List<TrackSnapshot> tracks;
    
    public ProjectSnapshot() {
        this.tracks = new ArrayList<>();
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public ProjectSnapshot(String snapshotName) {
        this();
        this.snapshotName = snapshotName;
    }
    
    public ProjectSnapshot(List<TrackSnapshot> tracks, String snapshotName) {
        this();
        this.tracks = new ArrayList<>(tracks); // Copy to prevent external modification
        this.snapshotName = snapshotName;
    }
    
    public int getTrackCount() {
        return tracks.size();
    }
    
    @Override
    public String toString() {
        return String.format("ProjectSnapshot{name='%s', timestamp='%s', tracks=%d}", 
                           snapshotName, timestamp, getTrackCount());
    }
}