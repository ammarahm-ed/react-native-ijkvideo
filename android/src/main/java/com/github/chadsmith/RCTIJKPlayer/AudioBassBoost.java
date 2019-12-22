package com.github.chadsmith.RCTIJKPlayer;

import android.media.audiofx.AudioEffect;
import android.media.audiofx.BassBoost;
import android.util.Log;

public class AudioBassBoost implements BassBoost.OnParameterChangeListener{

    private android.media.audiofx.BassBoost mBassBoost;
    private android.media.audiofx.BassBoost.Settings mBassBoostSettings;
    private boolean mBassBoostEnabled = false;
    private short mCurrentBassStrength = 0;
    private int currentAudioSessionId = 0;

    public boolean getBassBoostSupported() {

        return mBassBoost.getStrengthSupported();
    }


    public BassBoost initializeBassEngine(int audioSessionId) {
        if (mBassBoost == null) {
            mBassBoost = new BassBoost(0,audioSessionId);

        }
        mBassBoost.setEnabled(true);

        return mBassBoost;
    }

    public BassBoost.Settings getProperties() {
        return mBassBoost.getProperties();
    }

    public void setBassBoostSettings(boolean enabled, BassBoost.Settings settings) {

        mBassBoostEnabled = enabled;
        mBassBoostSettings = settings;

    }

    public void setBassStrength(short strength) {

        mBassBoost.setStrength(strength);
    }

    public short getRoundedStrength(short strength) {

        return mBassBoost.getRoundedStrength();

    }


    @Override
    public void onParameterChange(BassBoost effect, int status, int param, short value) {

        Log.i("BassBoost", String.valueOf(effect));
    }



    public void destroyBassEngine() {
        mBassBoost.release();

        mBassBoost = null;


    }












}

