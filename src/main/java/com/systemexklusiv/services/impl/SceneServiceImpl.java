package com.systemexklusiv.services.impl;

import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.systemexklusiv.services.APIService;
import com.systemexklusiv.services.OSCManager;
import com.systemexklusiv.services.SceneService;

public class SceneServiceImpl implements SceneService {
    
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
        
        SceneBank sceneBank = apiService.getSceneBank();
        
        for (int i = 0; i < sceneBank.getSizeOfBank(); i++) {
            final int index = i + 1;  // OSC uses 1-based indexing
            Scene scene = sceneBank.getItemAt(i);
            
            scene.exists().addValueObserver(exists -> {
                if (exists) {
                    broadcastScene(index);
                } else {
                    oscManager.sendSceneName(index, "");
                }
            });
            
            scene.getName().addValueObserver(name -> {
                if (scene.exists().get()) {
                    broadcastScene(index);
                }
            });
        }
        
        isMonitoring = true;
        broadcastAllScenes();
    }
    
    @Override
    public void stopMonitoring() {
        isMonitoring = false;
    }
    
    @Override
    public void broadcastAllScenes() {
        for (int i = 0; i < apiService.getSceneBank().getSizeOfBank(); i++) {
            broadcastScene(i + 1);  // OSC uses 1-based indexing
        }
    }
    
    @Override
    public void broadcastScene(int index) {
        String name = apiService.getSceneName(index - 1);  // API uses 0-based indexing
        oscManager.sendSceneName(index, name);
    }
}