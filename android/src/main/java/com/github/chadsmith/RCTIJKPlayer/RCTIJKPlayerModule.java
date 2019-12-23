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
    private Equalizer mEqualizer;

    public RCTIJKPlayerModule(ReactApplicationContext reactContext, RCTIJKPlayerManager playerManager) {

        super(reactContext);
        this.playerManager = playerManager;

    }

    @Override
    public String getName() {
        return "RCTIJKPlayerModule";
    }

    @ReactMethod
    public void init() {
        if (playerManager != null) {
            videoView = playerManager.getPlayerInstance();
            mEqualizer = playerManager.getEqualizerInstance();
        }
    }


    @ReactMethod
    public void seek(final double seekTime) {
        init();
        videoView.setSeekModifier(seekTime);
    }

    @ReactMethod
    public void setEqualizerSettings(ReadableMap eqSettings) {
        init();
        mEqualizer.setEqualizerSettings(eqSettings);
    }

    @ReactMethod
    public void getEQBandLevels(final Promise promise) {
        init();
        mEqualizer.getEQBandLevels(promise);
    }

    @ReactMethod
    public void setEqualizerEnabled(boolean enabled) {
        init();
        mEqualizer.setEqualizerEnabled(enabled);
    }

    @ReactMethod
    public void setEQBandLevel(ReadableMap bandLevel) {
        init();
        mEqualizer.setEqualizerBandLevel(bandLevel);
    }

    @ReactMethod
    public void setEQPreset(int presetIndex) {
        init();
        mEqualizer.setEqualizerPreset(presetIndex);
    }

    @ReactMethod
    public void getEQConfig(final Promise promise) {
        init();
        mEqualizer.getEqualizerConfig(promise);
    }

    @ReactMethod
    public void getEQPresets(final Promise promise) {
        init();
        mEqualizer.getEqualizerPresets(promise);
    }

    @ReactMethod
    public void getSupportEffects(final Promise promise) {
        init();
        AudioEffect.Descriptor[] effects = AudioEffect.queryEffects();
        WritableArray args = new Arguments().fromArray(effects);

        promise.resolve(args);
    }

    @ReactMethod
    public void getBassBoostStrength(final Promise promise) {
        init();
        mEqualizer.getBassBoostStrength(promise);

    }

    @ReactMethod
    public void setBassBoostStrength(final int strength) {
        init();
        mEqualizer.setBassBoostModifier(strength);

    }

    @ReactMethod
    public void getLoudnessGain(final Promise promise) {
        init();
        mEqualizer.getCurrentLoudness(promise);

    }


    @ReactMethod
    public void setLoudnessGain(final int gain) {
        init();
        mEqualizer.setLoudnessModifier(gain);

    }


}
