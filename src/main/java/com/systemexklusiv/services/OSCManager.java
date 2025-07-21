package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.ControllerHost;

public interface OSCManager {
    
    void initialize(ControllerHost host, String sendHost, int sendPort, int receivePort);
    
    void sendCueMarkerName(int index, String name);
    
    void sendSceneName(int index, String name);
    
    void start();
    
    void stop();
    
    void setOSCCallback(OSCCallback callback);
    
    void setDebugMode(boolean debug);
    
    interface OSCCallback {
        void onCueTrigger(int index);
        void onSceneTrigger(int index);
    }
}