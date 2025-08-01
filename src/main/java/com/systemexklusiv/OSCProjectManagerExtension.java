package com.systemexklusiv;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableStringValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.ControllerExtension;

import com.systemexklusiv.services.APIServiceImpl;
import com.systemexklusiv.services.OSCManagerImpl;
import com.systemexklusiv.services.CueMarkerServiceImpl;
import com.systemexklusiv.services.SceneServiceImpl;

public class OSCProjectManagerExtension extends ControllerExtension
{
   private APIServiceImpl apiService;
   private OSCManagerImpl oscManager;
   private CueMarkerServiceImpl cueMarkerService;
   private SceneServiceImpl sceneService;
   
   private SettableStringValue sendHostSetting;
   private SettableRangedValue sendPortSetting;
   private SettableRangedValue receivePortSetting;
   private SettableBooleanValue debugSetting;
   private SettableBooleanValue printSnapshotButton;
   private SettableBooleanValue saveSnapshotButton;
   private SettableBooleanValue recallSnapshotButton;
   private SettableStringValue snapshotPathSetting;
   
   private boolean initializationComplete = false;

   protected OSCProjectManagerExtension(final OSCProjectManagerExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();
      
      setupPreferences();
      initializeServices();
      setupOSCCallback();
      startServices();
      
      initializationComplete = true;  // Set flag after everything is initialized

      // Send transition names on startup
      host.scheduleTask(() -> {
          apiService.sendTransitionNames();
      }, 1000); // Wait 1 second for everything to be fully initialized

      host.showPopupNotification("OSCProjectManager Initialized yo");
      host.println("OSCProjectManager Initialized with OSC communication yo!");
   }
   
   private void setupPreferences() {
      Preferences preferences = getHost().getPreferences();
      
      sendHostSetting = preferences.getStringSetting(
          "Send Host", "OSC Settings", 16, "127.0.0.1");
      
      sendPortSetting = preferences.getNumberSetting(
          "Send Port", "OSC Settings", 1024, 65535, 1, "", 9000);
      
      receivePortSetting = preferences.getNumberSetting(
          "Receive Port", "OSC Settings", 1024, 65535, 1, "", 8000);
      
      debugSetting = preferences.getBooleanSetting(
          "Debug Logging", "OSC Settings", true);
      
      // Add button for printing current project snapshot
      printSnapshotButton = preferences.getBooleanSetting(
          "Print Current Project Snapshot", "Snapshot Tools", false);
      
      // Add buttons for snapshot save/recall
      saveSnapshotButton = preferences.getBooleanSetting(
          "Save Snapshot to Slot 0", "Snapshot Tools", false);
      
      recallSnapshotButton = preferences.getBooleanSetting(
          "Recall Snapshot from Slot 0", "Snapshot Tools", false);
      
      // Add configurable snapshot path
      snapshotPathSetting = preferences.getStringSetting(
          "Snapshot Directory Path", "Snapshot Tools", 64, "snapshots");
      
      // Force preference values to be ready
      sendHostSetting.markInterested();
      sendPortSetting.markInterested();
      receivePortSetting.markInterested();
      debugSetting.markInterested();
      printSnapshotButton.markInterested();
      saveSnapshotButton.markInterested();
      recallSnapshotButton.markInterested();
      snapshotPathSetting.markInterested();
      
      getHost().println("Preferences initialized with defaults: Host=127.0.0.1, SendPort=9000, ReceivePort=8000, Debug=true");
   }
   
   private void initializeServices() {
      apiService = new APIServiceImpl();
      oscManager = new OSCManagerImpl();
      cueMarkerService = new CueMarkerServiceImpl();
      sceneService = new SceneServiceImpl();
      
      apiService.initialize(getHost());
      
      // Set OSC manager reference so API service can send messages
      apiService.setOSCManager(oscManager);
      
      String sendHost = sendHostSetting.get();
      if (sendHost == null || sendHost.isEmpty()) {
          sendHost = "192.168.1.100";
      }
      
      double sendPortRaw = sendPortSetting.get();
      double receivePortRaw = receivePortSetting.get();
      
      int sendPort = (int) sendPortSetting.get();
      if (sendPort == 0) {
          sendPort = 9000;
      }
      
      int receivePort = (int) receivePortSetting.get();
      if (receivePort == 0) {
          receivePort = 8000;
      }
      
      getHost().println("OSC Config DEBUG - Raw values: sendPort=" + sendPortRaw + ", receivePort=" + receivePortRaw);
      getHost().println("OSC Config - Send Host: " + sendHost + ", Send Port: " + sendPort + ", Receive Port: " + receivePort);
      
      oscManager.initialize(getHost(), sendHost, sendPort, receivePort);
      oscManager.setDebugMode(debugSetting.get());
      
      cueMarkerService.initialize(apiService, oscManager);
      sceneService.initialize(apiService, oscManager);
      
      // Set initial snapshot path
      String initialSnapshotPath = snapshotPathSetting.get();
      if (initialSnapshotPath == null || initialSnapshotPath.trim().isEmpty()) {
          initialSnapshotPath = "snapshots";
      }
      apiService.setSnapshotPath(initialSnapshotPath);
      
      setupPreferenceObservers();
   }
   
   private void setupPreferenceObservers() {
      sendHostSetting.addValueObserver(host -> {
          if (initializationComplete) {
              restartOSC();
          }
      });
      
      sendPortSetting.addValueObserver(port -> {
          if (initializationComplete) {
              restartOSC();
          }
      });
      
      receivePortSetting.addValueObserver(port -> {
          if (initializationComplete) {
              restartOSC();
          }
      });
      
      debugSetting.addValueObserver(debug -> {
          oscManager.setDebugMode(debug);
          if (initializationComplete) {
              getHost().println("Debug mode " + (debug ? "enabled" : "disabled"));
          }
      });
      
      printSnapshotButton.addValueObserver(pressed -> {
          if (initializationComplete && pressed) {
              getHost().println("Print Current Project Snapshot button pressed");
              apiService.printCurrentTrackSnapshots();
              
              // Reset button to false after use (makes it act like a momentary button)
              getHost().scheduleTask(() -> {
                  printSnapshotButton.set(false);
              }, 100);
          }
      });
      
      saveSnapshotButton.addValueObserver(pressed -> {
          if (initializationComplete && pressed) {
              getHost().println("Save Snapshot button pressed - saving to slot 0");
              boolean success = apiService.saveSnapshot(0, "Quick Save");
              if (success) {
                  getHost().showPopupNotification("Snapshot saved to slot 0");
              } else {
                  getHost().showPopupNotification("Failed to save snapshot");
              }
              
              // Reset button to false after use (makes it act like a momentary button)
              getHost().scheduleTask(() -> {
                  saveSnapshotButton.set(false);
              }, 100);
          }
      });
      
      recallSnapshotButton.addValueObserver(pressed -> {
          if (initializationComplete && pressed) {
              getHost().println("Recall Snapshot button pressed - recalling from slot 0");
              boolean success = apiService.recallSnapshot(0);
              if (success) {
                  getHost().showPopupNotification("Snapshot recalled from slot 0");
              } else {
                  getHost().showPopupNotification("No snapshot found in slot 0");
              }
              
              // Reset button to false after use (makes it act like a momentary button)
              getHost().scheduleTask(() -> {
                  recallSnapshotButton.set(false);
              }, 100);
          }
      });
      
      snapshotPathSetting.addValueObserver(path -> {
          if (initializationComplete) {
              String newPath = path != null && !path.trim().isEmpty() ? path.trim() : "snapshots";
              getHost().println("Snapshot path updated to: " + newPath);
              apiService.setSnapshotPath(newPath);
          }
      });
   }
   
   private void restartOSC() {
      oscManager.stop();
      
      String sendHost = sendHostSetting.get();
      if (sendHost == null || sendHost.isEmpty()) {
          sendHost = "192.168.1.100";
      }
      
      int sendPort = (int) sendPortSetting.get();
      if (sendPort == 0) {
          sendPort = 9000;
      }
      
      int receivePort = (int) receivePortSetting.get();
      if (receivePort == 0) {
          receivePort = 8000;
      }
      
      getHost().println("OSC Restart - Send Host: " + sendHost + ", Send Port: " + sendPort + ", Receive Port: " + receivePort);
      
      oscManager.initialize(getHost(), sendHost, sendPort, receivePort);
      oscManager.setDebugMode(debugSetting.get());
      oscManager.start();
   }
   
   private void setupOSCCallback() {
      oscManager.setOSCCallback(new OSCManagerImpl.OSCCallback() {
          @Override
          public void onCueTrigger(int index) {
              apiService.triggerCueMarker(index);
          }
          
          @Override
          public void onSceneTrigger(int index) {
              apiService.triggerScene(index);
          }
          
          @Override
          public void onTrackDuplicateToNew() {
              apiService.duplicateSelectedTrackToNew();
          }
          
          @Override
          public void onAllMonitoringOff() {
              apiService.turnOffAllMonitoringExceptGroups();
          }
          
          @Override
          public void onAllArmOff() {
              apiService.disarmAllTracks();
          }
          
          @Override
          public void onMakeRecordGroup() {
              apiService.makeRecordGroup();
          }
          
          @Override
          public void onSendTransitionNames() {
              apiService.sendTransitionNames();
          }
          
          @Override
          public void onTransitionTrigger(int index) {
              apiService.triggerTransitionSlot(index);
          }
          
          @Override
          public void onSnapshotSave(int slotIndex, String snapshotName) {
              apiService.saveSnapshot(slotIndex, snapshotName);
          }
          
          @Override
          public void onSnapshotRecall(int slotIndex) {
              apiService.recallSnapshot(slotIndex);
          }
          
          @Override
          public void onSnapshotList() {
              apiService.listSnapshots();
          }
      });
   }
   
   private void startServices() {
      oscManager.start();
      cueMarkerService.startMonitoring();
      sceneService.startMonitoring();
   }

   @Override
   public void exit()
   {
      if (cueMarkerService != null) {
          cueMarkerService.stopMonitoring();
      }
      
      if (sceneService != null) {
          sceneService.stopMonitoring();
      }
      
      if (oscManager != null) {
          oscManager.stop();
      }
      
      getHost().showPopupNotification("OSCProjectManager Exited");
   }

   @Override
   public void flush()
   {
      // OSC communication handles real-time updates via observers
   }
}
