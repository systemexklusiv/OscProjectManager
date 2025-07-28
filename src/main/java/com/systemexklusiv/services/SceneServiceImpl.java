package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
public class SceneServiceImpl {
    
    private APIServiceImpl apiService;
    private OSCManagerImpl oscManager;
    private boolean isMonitoring = false;
    
    public void initialize(APIServiceImpl apiService, OSCManagerImpl oscManager) {
        this.apiService = apiService;
        this.oscManager = oscManager;
    }
    
    public void startMonitoring() {
        if (isMonitoring) return;
        
        SceneBank sceneBank = apiService.getSceneBank();
        
        for (int i = 0; i < sceneBank.getSizeOfBank(); i++) {
            final int index = i;  // OSC uses 0-based indexing
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
    
    public void stopMonitoring() {
        isMonitoring = false;
    }
    
    public void broadcastAllScenes() {
        for (int i = 0; i < apiService.getSceneBank().getSizeOfBank(); i++) {
            broadcastScene(i);  // OSC uses 0-based indexing
        }
    }
    
    public void broadcastScene(int index) {
        String name = apiService.getSceneName(index);  // Both OSC and API use 0-based indexing
        oscManager.sendSceneName(index, name);
    }
}