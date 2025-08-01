package com.systemexklusiv.services;

public class TrackSnapshot {
    public String trackName;
    public String trackType;        // "audio", "instrument", "group", "master"
    public double volume;           // 0.0 to 1.0
    public double pan;              // -1.0 to 1.0  
    public boolean muted;
    public boolean armed;
    public int trackPosition;       // backup identifier
    
    public TrackSnapshot() {
        // Default constructor
    }
    
    @Override
    public String toString() {
        return String.format("TrackSnapshot{name='%s', type='%s', pos=%d, vol=%.2f, pan=%.2f, muted=%s, armed=%s}", 
                           trackName, trackType, trackPosition, volume, pan, muted, armed);
    }
}