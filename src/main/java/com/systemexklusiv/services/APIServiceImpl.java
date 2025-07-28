package com.systemexklusiv.services;

import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Track;

public class APIServiceImpl {
    
    private static final int CUE_MARKER_BANK_SIZE = 128;
    private static final int SCENE_BANK_SIZE = 128;
    
    private ControllerHost host;
    private Arranger arranger;
    private CueMarkerBank cueMarkerBank;
    private SceneBank sceneBank;
    private TrackBank trackBank;
    private Track cursorTrack;
    
    public void initialize(ControllerHost host) {
        this.host = host;
        this.arranger = host.createArranger();
        
        setupCueMarkerBank();
        setupSceneBank();
        setupTrackBank();
        setupCursorTrack();
    }
    
    private void setupCueMarkerBank() {
        cueMarkerBank = arranger.createCueMarkerBank(CUE_MARKER_BANK_SIZE);
        
        for (int i = 0; i < CUE_MARKER_BANK_SIZE; i++) {
            CueMarker cueMarker = cueMarkerBank.getItemAt(i);
            
            cueMarker.exists().markInterested();
            cueMarker.position().markInterested();
            cueMarker.getName().markInterested();
        }
    }
    
    private void setupTrackBank() {
        trackBank = host.createTrackBank(128, 0, 0, false);
        
        for (int i = 0; i < 128; i++) {
            Track track = trackBank.getItemAt(i);
            track.exists().markInterested();
            track.name().markInterested();
            track.arm().markInterested();
        }
    }
    
    private void setupCursorTrack() {
        cursorTrack = host.createCursorTrack(0, 0);
        cursorTrack.exists().markInterested();
        cursorTrack.arm().markInterested();
    }
    
    private void setupSceneBank() {
        sceneBank = host.createSceneBank(SCENE_BANK_SIZE);
        
        for (int i = 0; i < SCENE_BANK_SIZE; i++) {
            Scene scene = sceneBank.getItemAt(i);
            
            scene.exists().markInterested();
            scene.getName().markInterested();
        }
    }
    
    public CueMarkerBank getCueMarkerBank() {
        return cueMarkerBank;
    }
    
    public SceneBank getSceneBank() {
        return sceneBank;
    }
    
    public void triggerCueMarker(int index) {
        if (index >= 0 && index < CUE_MARKER_BANK_SIZE) {
            CueMarker cueMarker = cueMarkerBank.getItemAt(index);
            if (cueMarker.exists().get()) {
                cueMarker.launch(true);
                host.println("Triggered cue marker " + index + ": " + cueMarker.getName().get());
            }
        }
    }
    
    public void triggerScene(int index) {
        if (index >= 0 && index < SCENE_BANK_SIZE) {
            Scene scene = sceneBank.getItemAt(index);
            if (scene.exists().get()) {
                scene.launch();
                host.println("Triggered scene " + index + ": " + scene.getName().get());
            }
        }
    }
    
    public int getCueMarkerCount() {
        int count = 0;
        for (int i = 0; i < CUE_MARKER_BANK_SIZE; i++) {
            if (cueMarkerBank.getItemAt(i).exists().get()) {
                count++;
            }
        }
        return count;
    }
    
    public int getSceneCount() {
        int count = 0;
        for (int i = 0; i < SCENE_BANK_SIZE; i++) {
            if (sceneBank.getItemAt(i).exists().get()) {
                count++;
            }
        }
        return count;
    }
    
    public String getCueMarkerName(int index) {
        if (index >= 0 && index < CUE_MARKER_BANK_SIZE) {
            CueMarker cueMarker = cueMarkerBank.getItemAt(index);
            if (cueMarker.exists().get()) {
                return cueMarker.getName().get();
            }
        }
        return "";
    }
    
    public String getSceneName(int index) {
        if (index >= 0 && index < SCENE_BANK_SIZE) {
            Scene scene = sceneBank.getItemAt(index);
            if (scene.exists().get()) {
                return scene.getName().get();
            }
        }
        return "";
    }
    
    public void duplicateSelectedTrackToNew() {
        if (cursorTrack.exists().get()) {
            boolean wasArmed = cursorTrack.arm().get();
            
            cursorTrack.duplicate();
            
            if (wasArmed) {
                cursorTrack.arm().set(false);
            }
            
            host.println("Track duplicated. Original track disarmed, duplicated track ready for recording.");
        } else {
            host.println("No track selected for duplication.");
        }
    }
}