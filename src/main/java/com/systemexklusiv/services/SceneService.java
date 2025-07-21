package com.systemexklusiv.services;

public interface SceneService {
    
    void initialize(APIService apiService, OSCManager oscManager);
    
    void startMonitoring();
    
    void stopMonitoring();
    
    void broadcastAllScenes();
    
    void broadcastScene(int index);
}