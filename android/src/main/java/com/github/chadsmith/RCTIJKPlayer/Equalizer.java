package com.github.chadsmith.RCTIJKPlayer;

import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.BassBoost;
import android.media.audiofx.EnvironmentalReverb;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.PresetReverb;
import android.util.Log;



import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;

public class Equalizer implements OnAudioSessionIdRecieved, IMediaPlayer.OnCompletionListener,AudioEffect.OnEnableStatusChangeListener {

    private String TAG = "EQUALIZER";

    private Context context;
    private int mCurrentAudioSessionId;
    private IMediaPlayer mMediaPlayer;


    /**
     * Equalizer
     */

    private android.media.audiofx.Equalizer mEqualizer;
    private android.media.audiofx.Equalizer.Settings mEqualizerSettings;
    private boolean mEqualizerEnabled = false;
    private int mNumOfBands = -1;
    private int mNumOfPresets = -1;
    private int mPreset = 0;
    private ArrayList<ReadableMap> mBandLevels = new ArrayList<>();



    /**
     * BassBoost
     */

    private BassBoost mBassBoost;
    private BassBoost.Settings mBassBoostSettings;
    private boolean mBassBoostEnabled = false;
    private int mBassBoostStrength = 250;
    private short mCurrentBassStrength = 0;

    /**
     * Loudness Enhancer
     */

    private LoudnessEnhancer mLoudnessEnhancer;
    private boolean mLoudnessEnhancerEnabled = false;
    private int mDefaultLoudnessGain = 0;





    /**
     * Constructor
     *
     * @param context
     */

    public Equalizer(Context context) {
        this.context = context;


    }


    /**
     * Equalizer
     */


    public android.media.audiofx.Equalizer initializeEqualizer() {
        if (mEqualizer == null) {
            updateEqualizerPrefs(true, true);
        }

        return mEqualizer;
    }


    /**
     * Initialize the Equalizer if it has been released and update equalizer preferences if any
     *
     * @param useCustom Using custom Equalizer
     * @param wasSystem Using System Equalizer
     */


    private void updateEqualizerPrefs(boolean useCustom, boolean wasSystem) {


        if (mCurrentAudioSessionId == 0) {
            // No equalizer is currently bound. Nothing to do.
            return;
        }

        if (useCustom) {
            if (wasSystem || mEqualizer == null) {
                // System -> custom
                unbindSystemEqualizer(mCurrentAudioSessionId);
                bindCustomEqualizer(mCurrentAudioSessionId);
            } else {
                // Custom -> custom
                mEqualizer.setProperties(mEqualizerSettings);
            }
        } else {
            if (!wasSystem) {
                // Custom -> system
                unbindCustomEqualizer();
                bindSystemEqualizer(mCurrentAudioSessionId);
            }
            // Nothing to do for system -> system
        }
    }


    /**
     * Bind custom Equalizer to control MediaPlayer Audio Output.
     *
     * @param audioSessionId audioSessionId for current instance of MediaPlayer.
     */


    private void bindCustomEqualizer(int audioSessionId) {
        mEqualizer = new android.media.audiofx.Equalizer(0, audioSessionId);

        if (mEqualizerSettings != null)
            mEqualizer.setProperties(mEqualizerSettings);

        mEqualizer.setEnabled(true);
    }


    /**
     * Restore Settings from last media playback to new Equalizer session.
     *
     * @param eqSettings Equalizer.Settings from last session if any.
     */

    public void setEqualizerSettings(ReadableMap eqSettings) {

        if (mEqualizerEnabled)
            if (mEqualizer == null) {
                return;
            }

        ReadableArray values = eqSettings.getArray("frequencies");
        if (values == null) return;
        android.media.audiofx.Equalizer.Settings settings = new android.media.audiofx.Equalizer.Settings();
        settings.numBands = mEqualizer.getNumberOfBands();
        short[] array = new short[mEqualizer.getNumberOfBands()];
        for (short i = 0; i < values.size(); i++) {
            array[i] = (short) values.getInt(i);
        }
        settings.bandLevels = array;
        settings.curPreset = (short) eqSettings.getInt("currentPreset");

        boolean isEnabled = eqSettings.getBoolean("enabled");

        boolean invalidate = mEqualizerEnabled != isEnabled || mEqualizerEnabled;
        boolean wasSystem = isUsingSystemEqualizer();


        mEqualizerEnabled = isEnabled;
        mEqualizerSettings = settings;

        if (invalidate) {
            updateEqualizerPrefs(isEnabled, wasSystem);
        }

    }


    public void getEQBandLevels(final Promise promise) {




        if (mMediaPlayer != null && mEqualizerEnabled)
            initializeEqualizer();

        if (mEqualizer == null) {
            promise.resolve(null);
            return;
        }
        short numberFrequencyBands = mEqualizer.getNumberOfBands();
        List<Short> bandLevels = new ArrayList<>();
        for (short i = 0; i < numberFrequencyBands; i++) {
            bandLevels.add(mEqualizer.getBandLevel(i));
        }
        WritableArray array = Arguments.fromList(bandLevels);
        promise.resolve(array);

    }

    /**
     * Set Equalizer Enabled or disabled
     *
     * @param enabled
     */

    public void setEqualizerEnabled(boolean enabled) {

        if (mMediaPlayer != null && mEqualizerEnabled)
            initializeEqualizer();
        if (mEqualizer == null) {
            return;
        }
        mEqualizerEnabled = enabled;
        mEqualizer.setEnabled(enabled);

    }

    /**
     * Set level for current selected band.
     *
     * @param bandLevel
     */

    public void setEqualizerBandLevel(ReadableMap bandLevel) {
        if (mMediaPlayer != null && mEqualizerEnabled)
            initializeEqualizer();

        if (mEqualizer == null) {
            return;
        }
        short bandIndex = (short) bandLevel.getInt("bandIndex");
        int bandValue = bandLevel.getInt("level");

        int hasBandIndex = -1;

        if (mBandLevels.size() > 0) {
            for (int i = 0; i == mBandLevels.size(); i++) {

                if (mBandLevels.get(i).getInt("bandIndex") == bandIndex) {
                    hasBandIndex = i;
                }
            }
        } else {
            mBandLevels.add(bandLevel);
        }
        if (hasBandIndex != -1) {
            mBandLevels.add(hasBandIndex, bandLevel);

        }
        final short lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0];
        mEqualizer.setBandLevel(bandIndex,
                (short) (bandValue + lowerEqualizerBandLevel));

    }

    /**
     * Set Equalizer preset, eg. Flat, BassBoost etc. Use getEqualizerPresets to get the available
     * presets.
     *
     * @param presetIndex index value of preset.
     */


    public void setEqualizerPreset(int presetIndex) {
        if (mMediaPlayer != null && mEqualizerEnabled && presetIndex <= mEqualizer.getNumberOfPresets())

            mPreset = presetIndex;

        initializeEqualizer();

        mEqualizer.usePreset((short) presetIndex);
        mEqualizer.setEnabled(true);
        Log.i(TAG, mEqualizer.getPresetName(mEqualizer.getCurrentPreset()));


    }

    /**
     * Returns available equalizer presets.
     *
     * @param promise return available presets. Each preset has an indexValue and a name.
     */

    public void getEqualizerPresets(Promise promise) {
        WritableArray args = new Arguments().createArray();
        if (mMediaPlayer != null && mEqualizerEnabled)

            initializeEqualizer();

        if (mEqualizer == null) {
            promise.resolve(null);
            return;
        }

        int numOfPresets = mEqualizer.getNumberOfPresets();
        for (int i = 0; i == numOfPresets; i++) {
            WritableMap preset = new Arguments().createMap();
            preset.putInt("index", i);
            preset.putString("name", mEqualizer.getPresetName((short) i));
            args.pushMap(preset);
        }
        promise.resolve(args);

    }

    /**
     * Get the current Equalizer Config. Usually required to save settings between different
     * media player sessions.
     *
     * @param promise returns current equalizer settings.
     */


    public void getEqualizerConfig(final Promise promise) {

        if (mMediaPlayer != null && mEqualizerEnabled)
            initializeEqualizer();

        if (mEqualizer == null) {
            promise.resolve(null);
            return;
        }
        short numberFrequencyBands = mEqualizer.getNumberOfBands();

        // get the level ranges to be used in setting the band level
        // get lower limit of the range in decibels
        final int lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0] / 100;
        // get the upper limit of the range in decibels
        final int upperEqualizerBandLevel = mEqualizer.getBandLevelRange()[1] / 100;
        List<String> frequencyList = new ArrayList<>();
        for (short i = 0; i < numberFrequencyBands; i++) {
            String frequency = (mEqualizer.getCenterFreq(i) / 1000) + " Hz";
            frequencyList.add(frequency);
        }
        String[] presets = new String[mEqualizer.getNumberOfPresets()];
        for (int k = 0; k < mEqualizer.getNumberOfPresets(); k++)
            presets[k] = mEqualizer.getPresetName((short) k);

        WritableMap map = Arguments.createMap();
        map.putInt("lowerBandLevel", lowerEqualizerBandLevel);
        map.putInt("upperBandLevel", upperEqualizerBandLevel);
        map.putInt("noOfBands", numberFrequencyBands);
        map.putArray("frequencies", Arguments.fromList(frequencyList));
        map.putArray("presets", Arguments.fromArray(presets));
        promise.resolve(map);
    }


    /**
     * Loudness Enhancer
     *
     */


    /**
     * Set loudness enhancer enabled or disabled.
     *
     * @param enabled
     */

    public void setLoudnessEnabled(boolean enabled) {
        mLoudnessEnhancerEnabled = enabled;
        if (mMediaPlayer != null && mCurrentAudioSessionId != 0 && mLoudnessEnhancerEnabled) {
            setLoudnessModifier(mDefaultLoudnessGain);
        } else {
            if (mLoudnessEnhancer != null)
                destroyLoudnessEnhancer();
        }

    }

    /**
     * @param promise return current loudness gain.
     */


    public void getCurrentLoudness(Promise promise) {
        if (mLoudnessEnhancer != null) {
            promise.resolve(mLoudnessEnhancer.getTargetGain());
        } else {
            promise.reject("error", "loudness enhancer is disabled");
        }

    }


    /**
     * @param gain Set loudness gain.
     */

    public void setLoudnessModifier(int gain) {
        if (!mLoudnessEnhancerEnabled) return;

        if (mLoudnessEnhancer == null) {
            mLoudnessEnhancer = new LoudnessEnhancer(mCurrentAudioSessionId);
        }
        mLoudnessEnhancer.setTargetGain(gain);

    }

    /**
     * Destroy Loudness Enhancer;
     */

    public void destroyLoudnessEnhancer() {
        if (mLoudnessEnhancer != null) {
            mLoudnessEnhancer.setEnabled(false);
            mLoudnessEnhancer.release();
            mLoudnessEnhancer = null;
        }

    }






    /**
     * Bass Boost Settings.
     *
     */


    /**
     * Set BassBoost Enabled. Requires Headphones Plugged.
     *
     * @param enabled
     */
    public void setBassBoostEnabled(final boolean enabled) {

        mBassBoostEnabled = enabled;

        if (mMediaPlayer != null && mBassBoostEnabled && mCurrentAudioSessionId != 0) {
            setBassBoostModifier(mBassBoostStrength);

        } else {
            if (mBassBoost != null)
                destroyBassEngine();
        }

    }


    /**
     * Initialize the BassBoost Engine.
     *
     * @param audioSessionId
     * @return
     */

    public BassBoost initializeBassEngine(int audioSessionId) {
        if (mBassBoost == null) {
            mBassBoost = new BassBoost(0, audioSessionId);
        }
        mBassBoost.setEnabled(true);

        return mBassBoost;
    }

    /**
     * Set BassBoost Strength. A value between 0 - 1000.
     *
     * @param strength default:250, min:0, max:1000
     */

    public void setBassBoostModifier(int strength) {

        mBassBoostStrength = strength;

        if (strength > 0 && strength < 1000 && mMediaPlayer != null && mCurrentAudioSessionId != 0 && mBassBoostEnabled) {

            if (mBassBoost == null) {
                initializeBassEngine(mCurrentAudioSessionId);
            }

            initializeBassEngine(mCurrentAudioSessionId);
            setBassStrength((short) mBassBoostStrength);

        }

    }


    /**
     * Get current BassBoost Strength.
     *
     * @param promise
     */
    public void getBassBoostStrength(Promise promise) {
        if (mBassBoost == null) {

            initializeBassEngine(mCurrentAudioSessionId);

        }
        if (mBassBoost != null) {
            BassBoost.Settings settings = mBassBoost.getProperties();

            promise.resolve(settings.strength);
        }


    }

    /**
     * Set the Bass Boost Strength
     *
     * @param strength
     */

    public void setBassStrength(short strength) {
        mBassBoost.setStrength(strength);
    }

    /**
     * Get the current rounded BassBoost Strength.
     *
     * @return
     */

    public short getRoundedStrength() {

        return mBassBoost.getRoundedStrength();

    }


    /**
     * Destroy the BassBoost Engine.
     */

    public void destroyBassEngine() {
        if (mBassBoost != null) {
            mBassBoostSettings = mBassBoost.getProperties();
            mBassBoost.release();
            mBassBoost.setEnabled(false);
            mBassBoost = null;
        }


    }


    private void restoreEqualizerState() {
        if (mEqualizerEnabled) {
            if (mEqualizer != null) {
                unbindCustomEqualizer();
                mEqualizer = null;
            }
            initializeEqualizer();
            mEqualizer.usePreset((short) mPreset);

            if (mBandLevels.size() < 0) {
                for (int i = 0; i == mBandLevels.size(); i++) {

                    int bandIndex = mBandLevels.get(i).getInt("bandIndex");
                    int bandValue = mBandLevels.get(i).getInt("bandValue");
                    final short lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0];
                    mEqualizer.setBandLevel((short) bandIndex,
                            (short) (bandValue + lowerEqualizerBandLevel));
                }
            }
            Log.d("EQUALIZER", mEqualizer.getPresetName(mEqualizer.getCurrentPreset()));
            mEqualizer.setEnabled(true);
        }
    }


    /**
     * Destroy the Equalizer.
     */


    public void destroy() {
        if (mEqualizer != null) {
            Log.i("EQExoPlayer", "Destroying equalizer...");
            mEqualizer.setEnabled(false);
            mEqualizer.setEnableStatusListener(null);
            mEqualizer.release();
            mEqualizer = null;
        }
    }


    public void unbindCustomEqualizer() {
        destroy();
    }


    private boolean isUsingSystemEqualizer() {
        return false; // mEqualizerSettings == null || !mEqualizerEnabled;
    }


    /**
     * Give MediaPlayer AudioEffect control back to System.
     *
     * @param audioSessionId audioSessionId for current instance of MediaPlayer.
     */


    private void bindSystemEqualizer(int audioSessionId) {
        Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(intent);
    }


    /**
     * Take MediaPlayer AudioEffect Control from System.
     *
     * @param audioSessionId Audio Session Id for current instance of MediaPlayer.
     */

    private void unbindSystemEqualizer(int audioSessionId) {
        Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(intent);
    }

    @Override
    public void onAudioSessionId(IMediaPlayer mp) {

        if (mEqualizer != null) {

            unbindCustomEqualizer();
        }
       if (mBassBoost != null) {
           destroyBassEngine();
       }
      if (mLoudnessEnhancer != null) {
          destroyLoudnessEnhancer();
      }


        mCurrentAudioSessionId = 0;

        Log.i(TAG,"HERE");
        mMediaPlayer = mp;
        mCurrentAudioSessionId = mp.getAudioSessionId();
        if (mEqualizerEnabled) {
            restoreEqualizerState();
            setBassBoostModifier(mBassBoostStrength);
            setLoudnessModifier(mDefaultLoudnessGain);
        }


    }

    /**
     * Listener to get AudioSessionId from MediaPlayer on Video Playback Start.
     *
     * @param mp current MediaPlayer instance
     */




    @Override
    public void onCompletion(IMediaPlayer mp) {


    }

    @Override
    public void onEnableStatusChange(AudioEffect effect, boolean enabled) {
        Log.i(TAG,"Enabled");
    }


}

