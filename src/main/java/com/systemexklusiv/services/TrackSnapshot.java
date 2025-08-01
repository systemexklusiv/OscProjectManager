package com.systemexklusiv.services;

public class TrackSnapshot {
    
    // Constants for fixed sizes
    public static final int SEND_BANK_SIZE = 8;
    
    public String trackName;
    public String trackType;        // "audio", "instrument", "group", "master"
    public double volume;           // 0.0 to 1.0
    public double pan;              // -1.0 to 1.0  
    public boolean muted;
    public boolean armed;
    public int trackPosition;       // backup identifier
    
    // NEW: FX Send levels (fixed array)
    public double[] sendLevels;     // 0.0 to 1.0, size = SEND_BANK_SIZE
    
    // NEW: Monitor state
    public String monitorMode;      // "OFF", "IN", "AUTO"
    
    public TrackSnapshot() {
        // Initialize arrays
        sendLevels = new double[SEND_BANK_SIZE];
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("TrackSnapshot{name='%s', type='%s', pos=%d, vol=%.2f, pan=%.2f, muted=%s, armed=%s, monitor=%s", 
                           trackName, trackType, trackPosition, volume, pan, muted, armed, monitorMode));
        
        // Show send levels (only non-zero values to keep output clean)
        boolean hasActiveSends = false;
        for (int i = 0; i < sendLevels.length; i++) {
            if (sendLevels[i] > 0.0) {
                if (!hasActiveSends) {
                    sb.append(", sends=[");
                    hasActiveSends = true;
                } else {
                    sb.append(",");
                }
                sb.append(String.format("S%d:%.2f", i, sendLevels[i]));
            }
        }
        if (hasActiveSends) {
            sb.append("]");
        }
        
        sb.append("}");
        return sb.toString();
    }
}