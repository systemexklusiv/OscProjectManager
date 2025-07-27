package com.systemexklusiv.services;

public interface CueMarkerService {
    
    void initialize(APIService apiService, OSCManager oscManager);
    
    void startMonitoring();
    
    void stopMonitoring();
    
    void broadcastAllCueMarkers();
    
    void broadcastCueMarker(int index);
    
    void broadcastCueMarkerCount();
}