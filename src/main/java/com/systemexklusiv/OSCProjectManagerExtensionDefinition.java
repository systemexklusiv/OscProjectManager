package com.systemexklusiv;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class OSCProjectManagerExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("2240bf24-36da-4e1f-8ddc-38b60ef07bf4");
   
   public OSCProjectManagerExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "OSCProjectManager";
   }
   
   @Override
   public String getAuthor()
   {
      return "systemexklusiv";
   }

   @Override
   public String getVersion()
   {
      return "0.1";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }
   
   @Override
   public String getHardwareVendor()
   {
      return "systemexklusiv";
   }
   
   @Override
   public String getHardwareModel()
   {
      return "OSCProjectManager";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 24;
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 0;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 0;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
   }

   @Override
   public OSCProjectManagerExtension createInstance(final ControllerHost host)
   {
      return new OSCProjectManagerExtension(this, host);
   }
}
