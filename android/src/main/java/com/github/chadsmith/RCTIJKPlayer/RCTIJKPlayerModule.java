package com.github.chadsmith.RCTIJKPlayer;

import android.media.audiofx.AudioEffect;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;

public class RCTIJKPlayerModule extends ReactContextBaseJavaModule {


    private RCTIJKPlayer videoView;
    private RCTIJKPlayerManager playerManager;

    public RCTIJKPlayerModule(ReactApplicationContext reactContext, RCTIJKPlayerManager playerManager) {

        super(reactContext);
        this.playerManager = playerManager;

        Log.i("PLAYER_MANAGER",String.valueOf(playerManager));


    }

    @Override
    public String getName() {
        return "RCTIJKPlayerModule";
    }

    @ReactMethod
    public void init() {
        if (playerManager != null) {
            videoView = playerManager.getPlayerInstance();
        }
    }


    @ReactMethod
    public void seek(final double seekTime) {
        init();
        Log.i("HLLOO",String.valueOf(videoView));
        videoView.setSeekModifier(seekTime);
    }

    @ReactMethod
    public void setEqualizerSettings(ReadableMap eqSettings) {
        init();
        videoView.setEqualizerSettings(eqSettings);
    }

    @ReactMethod
    public void getEQBandLevels(final Promise promise){
        init();
        videoView.getEQBandLevels(promise);
    }

    @ReactMethod
    public void setEqualizerEnabled(boolean enabled){
        init();
        videoView.setEqualizerEnabled(enabled);
    }

    @ReactMethod
    public void setEQBandLevel(ReadableMap bandLevel){
        init();
        videoView.setEQBandLevel(bandLevel);
    }

    @ReactMethod
    public void setEQPreset(int presetIndex){
        init();
        videoView.setEQPreset(presetIndex);
    }

    @ReactMethod
    public void getEQConfig(final Promise promise) {
        init();
        videoView.getEQConfig(promise);
    }

    @ReactMethod
    public void getEQPresets(final Promise promise) {
        init();
        videoView.getEQPresets(promise);
    }

    @ReactMethod
    public void getSupportEffects(final Promise promise) {
        init();
        AudioEffect.Descriptor[] effects = AudioEffect.queryEffects();
        WritableArray args = new Arguments().fromArray(effects);


        promise.resolve(args);


    }





}
