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
            
            oscReceiver.addListener("/cue/trigger*", new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleCueTrigger(message);
                }
            });
            
            oscReceiver.addListener("/scene/trigger*", new OSCListener() {
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
            String indexStr = address.substring("/cue/trigger".length());
            int index = Integer.parseInt(indexStr);
            
            if (debugMode) {
                host.println("[DEBUG] Received cue trigger: " + address + " -> triggering cue " + index);
            }
            
            callback.onCueTrigger(index);
            
        } catch (NumberFormatException e) {
            host.errorln("Invalid cue trigger format: " + address);
        }
    }
    
    private void handleSceneTrigger(OSCMessage message) {
        if (callback == null) return;
        
        String address = message.getAddress();
        
        try {
            String indexStr = address.substring("/scene/trigger".length());
            int index = Integer.parseInt(indexStr);
            
            if (debugMode) {
                host.println("[DEBUG] Received scene trigger: " + address + " -> triggering scene " + index);
            }
            
            callback.onSceneTrigger(index);
            
        } catch (NumberFormatException e) {
            host.errorln("Invalid scene trigger format: " + address);
        }
    }
    
    @Override
    public void sendCueMarkerName(int index, String name) {
        if (oscSender == null) return;
        
        try {
            String address = "/cue" + index;

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
            String address = "/scene" + index + "/name";
            OSCMessage message = new OSCMessage(address, Arrays.asList(name));
            oscSender.send(message);
            
            if (debugMode && name.contains("TEST")) {
                host.println("[DEBUG] Sent scene: " + address + " -> \"" + name + "\"");
            }
            
        } catch (IOException e) {
            host.errorln("Failed to send scene name: " + e.getMessage());
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