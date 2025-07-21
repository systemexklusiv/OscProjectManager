package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.SceneBank;

public interface APIService {
    
    void initialize(ControllerHost host);
    
    CueMarkerBank getCueMarkerBank();
    
    SceneBank getSceneBank();
    
    void triggerCueMarker(int index);
    
    void triggerScene(int index);
    
    int getCueMarkerCount();
    
    int getSceneCount();
    
    String getCueMarkerName(int index);
    
    String getSceneName(int index);
}