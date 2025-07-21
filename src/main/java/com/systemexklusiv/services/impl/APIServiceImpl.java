package com.systemexklusiv.services.impl;

import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Scene;
import com.systemexklusiv.services.APIService;

public class APIServiceImpl implements APIService {
    
    private static final int CUE_MARKER_BANK_SIZE = 128;
    private static final int SCENE_BANK_SIZE = 128;
    
    private ControllerHost host;
    private Arranger arranger;
    private CueMarkerBank cueMarkerBank;
    private SceneBank sceneBank;
    
    @Override
    public void initialize(ControllerHost host) {
        this.host = host;
        this.arranger = host.createArranger();
        
        setupCueMarkerBank();
        setupSceneBank();
    }
    
    private void setupCueMarkerBank() {
        cueMarkerBank = arranger.createCueMarkerBank(CUE_MARKER_BANK_SIZE);
        
        for (int i = 0; i < CUE_MARKER_BANK_SIZE; i++) {
            CueMarker cueMarker = cueMarkerBank.getItemAt(i);
            
            cueMarker.exists().markInterested();
            cueMarker.position().markInterested();
            cueMarker.getName().markInterested();
        }
    }
    
    private void setupSceneBank() {
        sceneBank = host.createSceneBank(SCENE_BANK_SIZE);
        
        for (int i = 0; i < SCENE_BANK_SIZE; i++) {
            Scene scene = sceneBank.getItemAt(i);
            
            scene.exists().markInterested();
            scene.getName().markInterested();
        }
    }
    
    @Override
    public CueMarkerBank getCueMarkerBank() {
        return cueMarkerBank;
    }
    
    @Override
    public SceneBank getSceneBank() {
        return sceneBank;
    }
    
    @Override
    public void triggerCueMarker(int index) {
        if (index >= 0 && index < CUE_MARKER_BANK_SIZE) {
            CueMarker cueMarker = cueMarkerBank.getItemAt(index);
            if (cueMarker.exists().get()) {
                cueMarker.launch(true);
                host.println("Triggered cue marker " + index + ": " + cueMarker.getName().get());
            }
        }
    }
    
    @Override
    public void triggerScene(int index) {
        if (index >= 0 && index < SCENE_BANK_SIZE) {
            Scene scene = sceneBank.getItemAt(index);
            if (scene.exists().get()) {
                scene.launch();
                host.println("Triggered scene " + index + ": " + scene.getName().get());
            }
        }
    }
    
    @Override
    public int getCueMarkerCount() {
        int count = 0;
        for (int i = 0; i < CUE_MARKER_BANK_SIZE; i++) {
            if (cueMarkerBank.getItemAt(i).exists().get()) {
                count++;
            }
        }
        return count;
    }
    
    @Override
    public int getSceneCount() {
        int count = 0;
        for (int i = 0; i < SCENE_BANK_SIZE; i++) {
            if (sceneBank.getItemAt(i).exists().get()) {
                count++;
            }
        }
        return count;
    }
    
    @Override
    public String getCueMarkerName(int index) {
        if (index >= 0 && index < CUE_MARKER_BANK_SIZE) {
            CueMarker cueMarker = cueMarkerBank.getItemAt(index);
            if (cueMarker.exists().get()) {
                return cueMarker.getName().get();
            }
        }
        return "";
    }
    
    @Override
    public String getSceneName(int index) {
        if (index >= 0 && index < SCENE_BANK_SIZE) {
            Scene scene = sceneBank.getItemAt(index);
            if (scene.exists().get()) {
                return scene.getName().get();
            }
        }
        return "";
    }
}