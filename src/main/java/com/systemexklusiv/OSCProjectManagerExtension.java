package com.systemexklusiv;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableStringValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.ControllerExtension;

import com.systemexklusiv.services.APIService;
import com.systemexklusiv.services.OSCManager;
import com.systemexklusiv.services.CueMarkerService;
import com.systemexklusiv.services.SceneService;
import com.systemexklusiv.services.impl.APIServiceImpl;
import com.systemexklusiv.services.impl.OSCManagerImpl;
import com.systemexklusiv.services.impl.CueMarkerServiceImpl;
import com.systemexklusiv.services.impl.SceneServiceImpl;

public class OSCProjectManagerExtension extends ControllerExtension
{
   private APIService apiService;
   private OSCManager oscManager;
   private CueMarkerService cueMarkerService;
   private SceneService sceneService;
   
   private SettableStringValue sendHostSetting;
   private SettableRangedValue sendPortSetting;
   private SettableRangedValue receivePortSetting;
   private SettableBooleanValue debugSetting;

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
      
      // Force preference values to be ready
      sendHostSetting.markInterested();
      sendPortSetting.markInterested();
      receivePortSetting.markInterested();
      debugSetting.markInterested();
      
      getHost().println("Preferences initialized with defaults: Host=127.0.0.1, SendPort=9000, ReceivePort=8000, Debug=true");
   }
   
   private void initializeServices() {
      apiService = new APIServiceImpl();
      oscManager = new OSCManagerImpl();
      cueMarkerService = new CueMarkerServiceImpl();
      sceneService = new SceneServiceImpl();
      
      apiService.initialize(getHost());
      
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
      
      setupPreferenceObservers();
   }
   
   private void setupPreferenceObservers() {
      sendHostSetting.addValueObserver(host -> {
          restartOSC();
      });
      
      sendPortSetting.addValueObserver(port -> {
          restartOSC();
      });
      
      receivePortSetting.addValueObserver(port -> {
          restartOSC();
      });
      
      debugSetting.addValueObserver(debug -> {
          oscManager.setDebugMode(debug);
          getHost().println("Debug mode " + (debug ? "enabled" : "disabled"));
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
      oscManager.setOSCCallback(new OSCManager.OSCCallback() {
          @Override
          public void onCueTrigger(int index) {
              apiService.triggerCueMarker(index);
          }
          
          @Override
          public void onSceneTrigger(int index) {
              apiService.triggerScene(index);
          }
      });
   }
   
   private void startServices() {
      oscManager.start();
      cueMarkerService.startMonitoring();
      sceneService.startMonitoring();
      
      // Test message to verify OSC is working - delay it a bit
//      getHost().scheduleTask(() -> {
//          oscManager.sendCueMarkerName(99, "TEST_MESSAGE_ON_STARTUP");
//          oscManager.sendCueMarkerName(100, "SECOND_TEST_MESSAGE");
//      }, 1000); // 1 second delay
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
