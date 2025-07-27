package com.systemexklusiv.services.impl;

import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.systemexklusiv.services.APIService;
import com.systemexklusiv.services.CueMarkerService;
import com.systemexklusiv.services.OSCManager;

public class CueMarkerServiceImpl implements CueMarkerService {
    
    private APIService apiService;
    private OSCManager oscManager;
    private boolean isMonitoring = false;
    private int lastCueMarkerCount = -1;
    
    @Override
    public void initialize(APIService apiService, OSCManager oscManager) {
        this.apiService = apiService;
        this.oscManager = oscManager;
    }
    
    @Override
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
    
    @Override
    public void stopMonitoring() {
        isMonitoring = false;
    }
    
    @Override
    public void broadcastAllCueMarkers() {
        for (int i = 0; i < apiService.getCueMarkerBank().getSizeOfBank(); i++) {
            broadcastCueMarker(i);  // OSC uses 0-based indexing
        }
    }
    
    @Override
    public void broadcastCueMarker(int index) {
        String name = apiService.getCueMarkerName(index);  // Both OSC and API use 0-based indexing
        oscManager.sendCueMarkerName(index, name);
    }
    
    @Override
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