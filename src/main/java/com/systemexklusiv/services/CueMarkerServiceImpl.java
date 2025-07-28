package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.CueMarkerBank;

public class CueMarkerServiceImpl {
    
    private APIServiceImpl apiService;
    private OSCManagerImpl oscManager;
    private boolean isMonitoring = false;
    private int lastCueMarkerCount = -1;
    
    public void initialize(APIServiceImpl apiService, OSCManagerImpl oscManager) {
        this.apiService = apiService;
        this.oscManager = oscManager;
    }
    
    public void startMonitoring() {
        if (isMonitoring) return;
        
        CueMarkerBank cueMarkerBank = apiService.getCueMarkerBank();
        
        for (int i = 0; i < cueMarkerBank.getSizeOfBank(); i++) {
            final int index = i;  // OSC uses 0-based indexing
            CueMarker cueMarker = cueMarkerBank.getItemAt(i);
            
            cueMarker.exists().addValueObserver(exists -> {
                if (exists) {
                    broadcastCueMarker(index);
                } else {
                    oscManager.sendCueMarkerName(index, "");
                }
                updateCueMarkerCountIfChanged();
            });
            
            cueMarker.getName().addValueObserver(name -> {
                if (cueMarker.exists().get()) {
                    broadcastCueMarker(index);
                }
            });
        }
        
        isMonitoring = true;
        broadcastAllCueMarkers();
        broadcastCueMarkerCount();
    }
    
    public void stopMonitoring() {
        isMonitoring = false;
    }
    
    public void broadcastAllCueMarkers() {
        for (int i = 0; i < apiService.getCueMarkerBank().getSizeOfBank(); i++) {
            broadcastCueMarker(i);  // OSC uses 0-based indexing
        }
    }
    
    public void broadcastCueMarker(int index) {
        String name = apiService.getCueMarkerName(index);  // Both OSC and API use 0-based indexing
        oscManager.sendCueMarkerName(index, name);
    }
    
    public void broadcastCueMarkerCount() {
        int count = apiService.getCueMarkerCount();
        oscManager.sendCueMarkerCount(count);
        lastCueMarkerCount = count;
    }
    
    private void updateCueMarkerCountIfChanged() {
        int currentCount = apiService.getCueMarkerCount();
        if (currentCount != lastCueMarkerCount) {
            oscManager.sendCueMarkerCount(currentCount);
            lastCueMarkerCount = currentCount;
        }
    }
}