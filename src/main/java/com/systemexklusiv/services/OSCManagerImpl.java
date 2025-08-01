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
    public static final String SNAPSHOT_SAVE_OSC_PATH = "/snapshot/save/";
    public static final String SNAPSHOT_RECALL_OSC_PATH = "/snapshot/recall/";
    public static final String SNAPSHOT_LIST_OSC_PATH = "/snapshot/list";
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
            
            oscReceiver.addListener("/track/makeRecordGroup", new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleMakeRecordGroup(message);
                }
            });
            
            oscReceiver.addListener("/track/sendTransitionNames", new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleSendTransitionNames(message);
                }
            });
            
            oscReceiver.addListener("/transition/trigger/*", new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleTransitionTrigger(message);
                }
            });
            
            oscReceiver.addListener(SNAPSHOT_SAVE_OSC_PATH+"*", new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleSnapshotSave(message);
                }
            });
            
            oscReceiver.addListener(SNAPSHOT_RECALL_OSC_PATH+"*", new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleSnapshotRecall(message);
                }
            });
            
            oscReceiver.addListener(SNAPSHOT_LIST_OSC_PATH, new OSCListener() {
                @Override
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    handleSnapshotList(message);
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
        
        try {
            String address = message.getAddress();
            String indexStr = address.substring(CUE_TRIGGER_OSC_PATH.length());
            int index = (int) Float.parseFloat(indexStr);  // 0-based from TouchOSC
            
            if (debugMode) {
                host.println("[DEBUG] Received cue trigger: " + address + " -> triggering cue " + index);
            }
            
            callback.onCueTrigger(index);
            
        } catch (NumberFormatException e) {
            host.errorln("Invalid cue trigger format in message: " + message.getAddress() + " - " + e.getMessage());
        } catch (StringIndexOutOfBoundsException e) {
            host.errorln("Malformed cue trigger address: " + message.getAddress() + " - " + e.getMessage());
        } catch (Exception e) {
            host.errorln("Error processing cue trigger message: " + message.getAddress() + " - " + e.getMessage());
        }
    }
    
    private void handleSceneTrigger(OSCMessage message) {
        if (callback == null) return;
        
        try {
            String address = message.getAddress();
            String indexStr = address.substring(SCENE_TRIGGER_OSC_PATH.length());
            int index = Integer.parseInt(indexStr);  // 0-based from TouchOSC
            
            if (debugMode) {
                host.println("[DEBUG] Received scene trigger: " + address + " -> triggering scene " + index);
            }
            
            callback.onSceneTrigger(index);
            
        } catch (NumberFormatException e) {
            host.errorln("Invalid scene trigger format in message: " + message.getAddress() + " - " + e.getMessage());
        } catch (StringIndexOutOfBoundsException e) {
            host.errorln("Malformed scene trigger address: " + message.getAddress() + " - " + e.getMessage());
        } catch (Exception e) {
            host.errorln("Error processing scene trigger message: " + message.getAddress() + " - " + e.getMessage());
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
    
    public void sendTransitionName(int index, String name) {
        if (oscSender == null) return;
        
        try {
            String address = "/transition/name/" + index; // 0-based indexing
            OSCMessage message = new OSCMessage(address, Arrays.asList(name));
            oscSender.send(message);
            
            if (debugMode) {
                host.println("[DEBUG] Sent transition name: " + address + " -> \"" + name + "\"");
            }
            
        } catch (IOException e) {
            host.errorln("Failed to send transition name: " + e.getMessage());
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
        
        try {
            if (debugMode) {
                host.println("[DEBUG] Received track duplicate to new request");
            }
            
            callback.onTrackDuplicateToNew();
        } catch (Exception e) {
            host.errorln("Error processing track duplicate message: " + message.getAddress() + " - " + e.getMessage());
        }
    }
    
    private void handleAllMonitoringOff(OSCMessage message) {
        if (callback == null) return;
        
        try {
            if (debugMode) {
                host.println("[DEBUG] Received all monitoring off request");
            }
            
            callback.onAllMonitoringOff();
        } catch (Exception e) {
            host.errorln("Error processing all monitoring off message: " + message.getAddress() + " - " + e.getMessage());
        }
    }
    
    private void handleAllArmOff(OSCMessage message) {
        if (callback == null) return;
        
        try {
            if (debugMode) {
                host.println("[DEBUG] Received all arm off request");
            }
            
            callback.onAllArmOff();
        } catch (Exception e) {
            host.errorln("Error processing all arm off message: " + message.getAddress() + " - " + e.getMessage());
        }
    }
    
    private void handleMakeRecordGroup(OSCMessage message) {
        if (callback == null) return;
        
        try {
            if (debugMode) {
                host.println("[DEBUG] Received make record group request");
            }
            
            callback.onMakeRecordGroup();
        } catch (Exception e) {
            host.errorln("Error processing make record group message: " + message.getAddress() + " - " + e.getMessage());
        }
    }
    
    private void handleSendTransitionNames(OSCMessage message) {
        if (callback == null) return;
        
        try {
            if (debugMode) {
                host.println("[DEBUG] Received send transition names request");
            }
            
            callback.onSendTransitionNames();
        } catch (Exception e) {
            host.errorln("Error processing send transition names message: " + message.getAddress() + " - " + e.getMessage());
        }
    }
    
    private void handleTransitionTrigger(OSCMessage message) {
        if (callback == null) return;
        
        try {
            String address = message.getAddress();
            // Extract index from "/transition/trigger/X"
            String indexStr = address.substring("/transition/trigger/".length());
            int index = Integer.parseInt(indexStr); // 0-based from OSC client
            
            if (debugMode) {
                host.println("[DEBUG] Received transition trigger: " + address + " -> triggering slot " + index);
            }
            
            callback.onTransitionTrigger(index);
            
        } catch (NumberFormatException e) {
            host.errorln("Invalid transition trigger format in message: " + message.getAddress() + " - " + e.getMessage());
        } catch (StringIndexOutOfBoundsException e) {
            host.errorln("Malformed transition trigger address: " + message.getAddress() + " - " + e.getMessage());
        } catch (Exception e) {
            host.errorln("Error processing transition trigger message: " + message.getAddress() + " - " + e.getMessage());
        }
    }
    
    private void handleSnapshotSave(OSCMessage message) {
        if (callback == null) return;
        
        try {
            String address = message.getAddress();
            // Extract slot index from "/snapshot/save/X"
            String indexStr = address.substring(SNAPSHOT_SAVE_OSC_PATH.length());
            int slotIndex = Integer.parseInt(indexStr); // 0-based from OSC client
            
            // Get snapshot name from message arguments (optional, default to auto-generated)
            String snapshotName = "Snapshot " + slotIndex;
            if (message.getArguments().size() > 0) {
                Object arg = message.getArguments().get(0);
                if (arg instanceof String) {
                    snapshotName = (String) arg;
                }
            }
            
            if (debugMode) {
                host.println("[DEBUG] Received snapshot save: " + address + " -> saving to slot " + slotIndex + " as \"" + snapshotName + "\"");
            }
            
            callback.onSnapshotSave(slotIndex, snapshotName);
            
        } catch (NumberFormatException e) {
            host.errorln("Invalid snapshot save format in message: " + message.getAddress() + " - " + e.getMessage());
        } catch (StringIndexOutOfBoundsException e) {
            host.errorln("Malformed snapshot save address: " + message.getAddress() + " - " + e.getMessage());
        } catch (Exception e) {
            host.errorln("Error processing snapshot save message: " + message.getAddress() + " - " + e.getMessage());
        }
    }
    
    private void handleSnapshotRecall(OSCMessage message) {
        if (callback == null) return;
        
        try {
            String address = message.getAddress();
            // Extract slot index from "/snapshot/recall/X"
            String indexStr = address.substring(SNAPSHOT_RECALL_OSC_PATH.length());
            int slotIndex = Integer.parseInt(indexStr); // 0-based from OSC client
            
            if (debugMode) {
                host.println("[DEBUG] Received snapshot recall: " + address + " -> recalling from slot " + slotIndex);
            }
            
            callback.onSnapshotRecall(slotIndex);
            
        } catch (NumberFormatException e) {
            host.errorln("Invalid snapshot recall format in message: " + message.getAddress() + " - " + e.getMessage());
        } catch (StringIndexOutOfBoundsException e) {
            host.errorln("Malformed snapshot recall address: " + message.getAddress() + " - " + e.getMessage());
        } catch (Exception e) {
            host.errorln("Error processing snapshot recall message: " + message.getAddress() + " - " + e.getMessage());
        }
    }
    
    private void handleSnapshotList(OSCMessage message) {
        if (callback == null) return;
        
        try {
            if (debugMode) {
                host.println("[DEBUG] Received snapshot list request");
            }
            
            callback.onSnapshotList();
        } catch (Exception e) {
            host.errorln("Error processing snapshot list message: " + message.getAddress() + " - " + e.getMessage());
        }
    }
    
    public interface OSCCallback {
        void onCueTrigger(int index);
        void onSceneTrigger(int index);
        void onTrackDuplicateToNew();
        void onAllMonitoringOff();
        void onAllArmOff();
        void onMakeRecordGroup();
        void onSendTransitionNames();
        void onTransitionTrigger(int index);
        void onSnapshotSave(int slotIndex, String snapshotName);
        void onSnapshotRecall(int slotIndex);
        void onSnapshotList();
    }
}