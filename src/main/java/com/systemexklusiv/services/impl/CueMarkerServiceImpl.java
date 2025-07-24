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
            final int index = i + 1;  // OSC uses 1-based indexing
            CueMarker cueMarker = cueMarkerBank.getItemAt(i);
            
            cueMarker.exists().addValueObserver(exists -> {
                if (exists) {
                    broadcastCueMarker(index);
                } else {
                    oscManager.sendCueMarkerName(index, "");
                }
            });
            
            cueMarker.getName().addValueObserver(name -> {
                if (cueMarker.exists().get()) {
                    broadcastCueMarker(index);
                }
            });
        }
        
        isMonitoring = true;
        broadcastAllCueMarkers();
    }
    
    @Override
    public void stopMonitoring() {
        isMonitoring = false;
    }
    
    @Override
    public void broadcastAllCueMarkers() {
        for (int i = 0; i < apiService.getCueMarkerBank().getSizeOfBank(); i++) {
            broadcastCueMarker(i + 1);  // OSC uses 1-based indexing
        }
    }
    
    @Override
    public void broadcastCueMarker(int index) {
        String name = apiService.getCueMarkerName(index - 1);  // API uses 0-based indexing
        oscManager.sendCueMarkerName(index, name);
    }
}