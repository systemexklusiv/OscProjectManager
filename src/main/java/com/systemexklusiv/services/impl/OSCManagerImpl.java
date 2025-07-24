package com.systemexklusiv.services.impl;

import com.bitwig.extension.controller.api.ControllerHost;
import com.systemexklusiv.services.OSCManager;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;
import com.illposed.osc.OSCListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class OSCManagerImpl implements OSCManager {

    public static final String CUE_TRIGGER_OSC_PATH = "/cue/trigger";
    public static final String SCENE_SEND_NAME_OSC_PATH = "/scene";
    public static final String SCENE_TRIGGER_OSC_PATH = "/scene/trigger";
    public static final String CUE_SEND_NAME_OSC_PATH = "/cue";
    private ControllerHost host;
    private OSCPortIn oscReceiver;
    private OSCPortOut oscSender;
    private OSCCallback callback;
    private String sendHost;
    private int sendPort;
    private int receivePort;
    private boolean debugMode = false;
    
    @Override
    public void initialize(ControllerHost host, String sendHost, int sendPort, int receivePort) {
        this.host = host;
        this.sendHost = sendHost;
        this.sendPort = sendPort;
        this.receivePort = receivePort;
        
        setupOSCReceiver();
        setupOSCSender();
    }
    
    private void setupOSCReceiver() {
        try {
            oscReceiver = new OSCPortIn(receivePort);
            
            oscReceiver.addListener(CUE_TRIGGER_OSC_PATH+"*", new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleCueTrigger(message);
                }
            });
            
            oscReceiver.addListener(SCENE_TRIGGER_OSC_PATH+"*", new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleSceneTrigger(message);
                }
            });
            
        } catch (SocketException e) {
            host.errorln("Failed to create OSC receiver on port " + receivePort + ": " + e.getMessage());
        }
    }
    
    private void setupOSCSender() {
        try {
            InetAddress address = InetAddress.getByName(sendHost);
            oscSender = new OSCPortOut(address, sendPort);
            
        } catch (SocketException | UnknownHostException e) {
            host.errorln("Failed to create OSC sender to " + sendHost + ":" + sendPort + ": " + e.getMessage());
        }
    }
    
    private void handleCueTrigger(OSCMessage message) {
        if (callback == null) return;
        
        String address = message.getAddress();
        
        try {
            String indexStr = address.substring(CUE_TRIGGER_OSC_PATH.length());
            int oscIndex = (int) Float.parseFloat(indexStr);  // 1-based from TouchOSC
            int apiIndex = oscIndex - 1;  // Convert to 0-based for API
            
            if (debugMode) {
                host.println("[DEBUG] Received cue trigger: " + address + " -> triggering cue " + oscIndex + " (API index " + apiIndex + ")");
            }
            
            callback.onCueTrigger(apiIndex);
            
        } catch (NumberFormatException e) {
            host.errorln("Invalid cue trigger format: " + address);
        }
    }
    
    private void handleSceneTrigger(OSCMessage message) {
        if (callback == null) return;
        
        String address = message.getAddress();
        
        try {
            String indexStr = address.substring(SCENE_TRIGGER_OSC_PATH.length());
            int oscIndex = Integer.parseInt(indexStr);  // 1-based from TouchOSC
            int apiIndex = oscIndex - 1;  // Convert to 0-based for API
            
            if (debugMode) {
                host.println("[DEBUG] Received " + SCENE_SEND_NAME_OSC_PATH + " trigger: " + address + " -> triggering " + SCENE_SEND_NAME_OSC_PATH + " " + oscIndex + " (API index " + apiIndex + ")");
            }
            
            callback.onSceneTrigger(apiIndex);
            
        } catch (NumberFormatException e) {
            host.errorln("Invalid " + SCENE_SEND_NAME_OSC_PATH + " trigger format: " + address);
        }
    }
    
    @Override
    public void sendCueMarkerName(int index, String name) {
        if (oscSender == null) return;
        
        try {
            String address = CUE_SEND_NAME_OSC_PATH + index;

            OSCMessage message = new OSCMessage(address, Arrays.asList(name));
            oscSender.send(message);

            if (debugMode && name.contains("TEST")) {
                host.println("[DEBUG] Sent cue marker: " + address + " -> \"" + name + "\"");
//                host.println("[DEBUG] Sent cue marker: " + address1 + " -> \"" + name + "\"");
//                host.println("[DEBUG] Sent cue marker: " + address2 + " -> \"" + name + "\"");
//                host.println("[DEBUG] Sent cue marker: " + address3 + " -> \"" + name + "\"");
//                host.println("[DEBUG] Sent cue marker: " + address4 + " -> \"" + name + "\"");
            }
            
        } catch (IOException e) {
            host.errorln("Failed to send cue marker name: " + e.getMessage());
        }
    }
    
    @Override
    public void sendSceneName(int index, String name) {
        if (oscSender == null) return;
        
        try {
            String address = SCENE_SEND_NAME_OSC_PATH + index;
            OSCMessage message = new OSCMessage(address, Arrays.asList(name));
            oscSender.send(message);
            
            if (debugMode && name.contains("TEST")) {
                host.println("[DEBUG] Sent " + SCENE_SEND_NAME_OSC_PATH + ": " + address + " -> \"" + name + "\"");
            }
            
        } catch (IOException e) {
            host.errorln("Failed to send " + SCENE_SEND_NAME_OSC_PATH + " name: " + e.getMessage());
        }
    }
    
    @Override
    public void start() {
        if (oscReceiver != null) {
            try {
                oscReceiver.startListening();
                host.println("OSC Receiver started on port " + receivePort);
            } catch (Exception e) {
                host.errorln("Failed to start OSC receiver: " + e.getMessage());
            }
        }
        
        if (oscSender != null) {
            host.println("OSC Sender ready to " + sendHost + ":" + sendPort);
        }
    }
    
    @Override
    public void stop() {
        if (oscReceiver != null) {
            try {
                oscReceiver.stopListening();
                oscReceiver.close();
                host.println("OSC Receiver stopped");
            } catch (Exception e) {
                host.errorln("Error stopping OSC receiver: " + e.getMessage());
            }
        }
        
        if (oscSender != null) {
            try {
                oscSender.close();
                host.println("OSC Sender closed");
            } catch (Exception e) {
                host.errorln("Error closing OSC sender: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void setOSCCallback(OSCCallback callback) {
        this.callback = callback;
    }
    
    @Override
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
}