package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.ControllerHost;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;
import com.illposed.osc.OSCListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class OSCManagerImpl {

    public static final String CUE_TRIGGER_OSC_PATH = "/cue/trigger/";
    public static final String SCENE_SEND_NAME_OSC_PATH = "/scene/name/";
    public static final String SCENE_TRIGGER_OSC_PATH = "/scene/trigger/";
    public static final String CUE_SEND_NAME_OSC_PATH = "/cue/name/";
    public static final String CUE_AMOUNT_PATH = "/cue/amount";
    private ControllerHost host;
    private OSCPortIn oscReceiver;
    private OSCPortOut oscSender;
    private OSCCallback callback;
    private String sendHost;
    private int sendPort;
    private int receivePort;
    private boolean debugMode = false;
    
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
            
            oscReceiver.addListener("/track/duplicateToNew", new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleTrackDuplicateToNew(message);
                }
            });
            
            oscReceiver.addListener("/track/allMonitoringOff", new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleAllMonitoringOff(message);
                }
            });
            
            oscReceiver.addListener("/track/allArmOff", new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleAllArmOff(message);
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
            int index = (int) Float.parseFloat(indexStr);  // 0-based from TouchOSC
            
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
            String indexStr = address.substring(SCENE_TRIGGER_OSC_PATH.length());
            int index = Integer.parseInt(indexStr);  // 0-based from TouchOSC
            
            if (debugMode) {
                host.println("[DEBUG] Received scene trigger: " + address + " -> triggering scene " + index);
            }
            
            callback.onSceneTrigger(index);
            
        } catch (NumberFormatException e) {
            host.errorln("Invalid " + SCENE_SEND_NAME_OSC_PATH + " trigger format: " + address);
        }
    }
    
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
    
    public void sendCueMarkerCount(int count) {
        if (oscSender == null) return;
        
        try {
            String address = CUE_AMOUNT_PATH;
            OSCMessage message = new OSCMessage(address, Arrays.asList(count));
            oscSender.send(message);
            
            if (debugMode) {
                host.println("[DEBUG] Sent cue marker count: " + address + " -> " + count);
            }
            
        } catch (IOException e) {
            host.errorln("Failed to send cue marker count: " + e.getMessage());
        }
    }
    
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
    
    public void setOSCCallback(OSCCallback callback) {
        this.callback = callback;
    }
    
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
    
    private void handleTrackDuplicateToNew(OSCMessage message) {
        if (callback == null) return;
        
        if (debugMode) {
            host.println("[DEBUG] Received track duplicate to new request");
        }
        
        callback.onTrackDuplicateToNew();
    }
    
    private void handleAllMonitoringOff(OSCMessage message) {
        if (callback == null) return;
        
        if (debugMode) {
            host.println("[DEBUG] Received all monitoring off request");
        }
        
        callback.onAllMonitoringOff();
    }
    
    private void handleAllArmOff(OSCMessage message) {
        if (callback == null) return;
        
        if (debugMode) {
            host.println("[DEBUG] Received all arm off request");
        }
        
        callback.onAllArmOff();
    }
    
    public interface OSCCallback {
        void onCueTrigger(int index);
        void onSceneTrigger(int index);
        void onTrackDuplicateToNew();
        void onAllMonitoringOff();
        void onAllArmOff();
    }
}