package com.github.chadsmith.RCTIJKPlayer;

import android.graphics.Bitmap;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.RequiresPermission;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Task;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaMeta;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.MediaInfo;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

public class RCTIJKPlayer extends FrameLayout implements LifecycleEventListener,AudioEffect.OnEnableStatusChangeListener, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnCompletionListener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnBufferingUpdateListener {

    public enum Events {
        EVENT_LOAD_START("onVideoLoadStart"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_STALLED("onVideoBuffer"),
        EVENT_ERROR("onVideoError"),
        EVENT_PROGRESS("onVideoProgress"),
        EVENT_PAUSE("onVideoPause"),
        EVENT_STOP("onVideoStop"),
        EVENT_END("onVideoEnd");
        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    public static final String EVENT_PROP_DURATION = "duration";
    public static final String EVENT_PROP_CURRENT_TIME = "currentTime";

    public static final String EVENT_PROP_ERROR = "error";
    public static final String EVENT_PROP_WHAT = "what";
    public static final String EVENT_PROP_EXTRA = "extra";
    public static final String EVENT_PROP_DATASOURCE = "dataSource";

    public static final String EVENT_PROP_BUFFERING_PROG = "progress";

    public RCTEventEmitter mEventEmitter;

    private boolean mPaused = false;
    private boolean mMuted = false;
    private float mVolume = 1.0f;
    private float mPlaybackSpeed = 1.0f;
    private boolean mRepeat = false;
    private boolean mLoaded = false;
    private boolean mStalled = false;
    private int mCurrentAudioSessionId;

    private Equalizer mEqualizer;
    private boolean mEqualizerEnabled = false;
    private int mPreset = 0;
    private ArrayList<ReadableMap> mBandLevels = new ArrayList<>();

    private AudioBassBoost mBassBoost;
    private boolean mBassBoostEnabled = false;
    private int mBassBoostStrength;

    private WritableArray mVideoTracks = null;
    private WritableArray mAudioTracks = null;
    private WritableArray mTextTracks = null;

    private ThemedReactContext themedReactContext;


    private float mProgressUpdateInterval = 250;

    private Handler mProgressUpdateHandler = new Handler();
    private Runnable mProgressUpdateRunnable = null;

    private IjkVideoView ijkVideoView;


    public RCTIJKPlayer(ThemedReactContext themedReactContext) {
        super(themedReactContext);

        mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);
        themedReactContext.addLifecycleEventListener(this);

        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        ijkVideoView = new IjkVideoView(themedReactContext);


        setProgressUpdateRunnable();

        ijkVideoView.setOnPreparedListener(this);
        ijkVideoView.setOnErrorListener(this);
        ijkVideoView.setOnCompletionListener(this);
        ijkVideoView.setOnInfoListener(this);
        ijkVideoView.setOnBufferingUpdateListener(this);

        addView(ijkVideoView);
    }

    private void setProgressUpdateRunnable() {
        if (ijkVideoView != null)
            mProgressUpdateRunnable = new Runnable() {
                @Override
                public void run() {
                    if (ijkVideoView != null && ijkVideoView.isPlaying() && !mPaused) {
                        WritableMap event = Arguments.createMap();
                        event.putDouble(EVENT_PROP_CURRENT_TIME, ijkVideoView.getCurrentPosition() / 1000.0);
                        event.putDouble(EVENT_PROP_DURATION, ijkVideoView.getDuration() / 1000.0);
                        mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);

                        mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, Math.round(mProgressUpdateInterval));
                    }
                }
            };

    }


    private void releasePlayer() {
        if (ijkVideoView != null)
            Task.callInBackground(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    ijkVideoView.setOnPreparedListener(null);
                    ijkVideoView.setOnErrorListener(null);
                    ijkVideoView.setOnCompletionListener(null);
                    ijkVideoView.setOnInfoListener(null);
                    ijkVideoView.setOnBufferingUpdateListener(null);
                    ijkVideoView.stopPlayback();
                    mAudioTracks = null;
                    mVideoTracks = null;
                    mTextTracks = null;
                    mProgressUpdateRunnable = null;
                    ijkVideoView = null;
                    return null;
                }

            });
    }

    public void setSrc(final String uriString, final ReadableMap readableMap, final String userAgent) {
        if (uriString == null)
            return;


        mLoaded = false;
        mStalled = false;

        WritableMap src = Arguments.createMap();
        src.putString(RCTIJKPlayerManager.PROP_SRC_URI, uriString);
        WritableMap event = Arguments.createMap();
        event.putMap(RCTIJKPlayerManager.PROP_SRC, src);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD_START.toString(), event);


        if (userAgent != null)
            ijkVideoView.setUserAgent(userAgent);


        if (readableMap != null) {
            Map<String, String> headerMap = new HashMap<>();
            ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
            StringBuilder headers = new StringBuilder();
            while (iterator.hasNextKey()) {
                String key = iterator.nextKey();
                ReadableType type = readableMap.getType(key);
                switch (type) {
                    case String:
                        headers
                                .append(key)
                                .append(": ")
                                .append(readableMap.getString(key))
                                .append("\r\n");
                        break;
                }
            }
            headerMap.put("Headers", headers.toString());

            mAudioTracks = null;
            mVideoTracks = null;
            mTextTracks = null;

            ijkVideoView.setVideoPath(uriString, headerMap);
        } else
            ijkVideoView.setVideoPath(uriString);
    }

    public void setPausedModifier(final boolean paused) {
        mPaused = paused;
        Log.i("PAUSE", "i am pausing");
        if (ijkVideoView == null) return;
        if (mPaused) {
            if (ijkVideoView.isPlaying()) {
                ijkVideoView.pause();
            }
        } else {
            if (!ijkVideoView.isPlaying()) {
                ijkVideoView.start();
                mProgressUpdateHandler.post(mProgressUpdateRunnable);
            }
        }
    }

    public void setSeekModifier(final double seekTime) {
        Log.i("HLLOO", "HELLOOO");
        if (ijkVideoView != null)

            ijkVideoView.seekTo((int) (seekTime * 1000));

        Log.i("HLLOO", "HELLOOO");
    }

    public void setResizeModifier(final String resizeMode) {
        if (ijkVideoView != null)
            ijkVideoView.setVideoAspect(resizeMode);
    }

    public void setMutedModifier(final boolean muted) {
        mMuted = muted;
        if (ijkVideoView == null) return;
        if (mMuted) {
            ijkVideoView.setVolume(0, 0);
        } else {
            ijkVideoView.setVolume(mVolume, mVolume);
        }
    }

    public void setVolumeModifier(final float volume) {
        mVolume = volume;
        if (ijkVideoView != null) {

            if (volume > 1.0f || volume < 0.0f) return;
            ijkVideoView.setVolume(mVolume, mVolume);
        }
    }

    public void setRepeatModifer(final boolean repeat) {
        mRepeat = repeat;
        if (ijkVideoView != null)
            ijkVideoView.repeat(mRepeat);

    }

    public void setPlaybackRateModifer(final float rate) {
        mPlaybackSpeed = rate;

        if (ijkVideoView != null)
            ijkVideoView.setPlaybackRate(mPlaybackSpeed);

    }

    public void setProgressUpdateInterval(final int progressUpdateInterval) {

        mProgressUpdateInterval = progressUpdateInterval;
        mProgressUpdateRunnable = null;
        setProgressUpdateRunnable();

    }

    public void selectAudioTrack(int trackID) {
        if (ijkVideoView != null && mAudioTracks != null)

            for (int i = 0; i <= mAudioTracks.size(); i++) {
                if (mAudioTracks.getMap(i).getInt("track_id") == trackID) {

                    ijkVideoView.selectTrack(trackID);

                }
            }

    }

    public void selectVideoTrack(int trackID) {
        if (ijkVideoView != null) ;

        for (int i = 0; i <= mVideoTracks.size(); i++) {
            if (mAudioTracks.getMap(i).getInt("track_id") == trackID) {

                ijkVideoView.selectTrack(trackID);

            }
        }
    }

    public void selectTextTrack(int trackID) {
        if (ijkVideoView != null) ;

        for (int i = 0; i <= mTextTracks.size(); i++) {
            if (mAudioTracks.getMap(i).getInt("track_id") == trackID) {

                ijkVideoView.selectTrack(trackID);

            }
        }
    }

    public void setSubtitleDisplay(int textSize, String color, String position, String backgroundColor) {

        ijkVideoView.setSubtitleDisplay(getContext(), textSize, position, color, backgroundColor);

    }

    public void setSubtitles(final boolean subtitlesEnabled) {

        ijkVideoView.setSubtitles(subtitlesEnabled);

    }

    public void setAudio(final boolean audioEnabled) {

        ijkVideoView.setAudio(audioEnabled);

    }

    public void setVideo(final boolean videoEnabled) {

        ijkVideoView.setVideo(videoEnabled);

    }

    public void setAudioFocus(final boolean audioFocus) {
        if (audioFocus) {
            ijkVideoView.getAudioFocus();
        } else {
            ijkVideoView.abandonAudioFocus();
        }

    }

    public void setBackgroundPlay(final boolean playInBackground) {
        if (ijkVideoView != null) ;
        ijkVideoView.enableBackgroundPlayback(playInBackground);
    }


    public void setSnapshotPath(final String snapshotPath) throws IOException {
        if (ijkVideoView == null)
            return;
        Bitmap bitmap = ijkVideoView.getBitmap();
        if (bitmap == null)
            return;
        File file = new File(snapshotPath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } finally {
            bitmap.recycle();
            if (fos != null)
                fos.close();
        }
    }


    public void setEqualizerSettings(ReadableMap eqSettings) {

        if (ijkVideoView != null && mEqualizerEnabled)
            if (mEqualizer == null) {
                return;
            }
        ReadableArray values = eqSettings.getArray("frequencies");
        if (values == null) return;
        Equalizer.Settings settings = new Equalizer.Settings();
        settings.numBands = mEqualizer.getNumberOfBands();
        short[] array = new short[mEqualizer.getNumberOfBands()];
        for (short i = 0; i < values.size(); i++) {
            array[i] = (short) values.getInt(i);
        }
        settings.bandLevels = array;
        settings.curPreset = (short) eqSettings.getInt("currentPreset");
        ijkVideoView.setEqualizerSettings(eqSettings.getBoolean("enabled"), settings);
    }


    public void getEQBandLevels(final Promise promise) {

        if (ijkVideoView != null && mEqualizerEnabled)
            mEqualizer = ijkVideoView.getEqualizer();
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

    public void setEqualizerModifier(boolean equalizerEnabled) {
        mEqualizerEnabled = equalizerEnabled;
    }

    public void setEqualizerEnabled(boolean enabled) {

        if (ijkVideoView != null && mEqualizerEnabled)
            mEqualizer = ijkVideoView.getEqualizer();
        if (mEqualizer == null) {
            return;
        }
        mEqualizer.setEnabled(enabled);

    }

    public void setEQBandLevel(ReadableMap bandLevel) {
        if (ijkVideoView != null && mEqualizerEnabled)
            mEqualizer = ijkVideoView.getEqualizer();


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


    public void setEQPreset(int presetIndex) {
        if (ijkVideoView != null && mEqualizerEnabled && presetIndex <= mEqualizer.getNumberOfPresets())

            mPreset = presetIndex;

        mEqualizer = ijkVideoView.getEqualizer();
        mEqualizer.usePreset((short) presetIndex);
        mEqualizer.setEnabled(true);
        Log.d("EQUALIZER", mEqualizer.getPresetName(mEqualizer.getCurrentPreset()));


    }

    public void getEQPresets(Promise promise) {
        WritableArray args = new Arguments().createArray();
        if (ijkVideoView != null && mEqualizerEnabled)

            mEqualizer = ijkVideoView.getEqualizer();

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


    public void getEQConfig(final Promise promise) {

        if (ijkVideoView != null && mEqualizerEnabled)
            mEqualizer = ijkVideoView.getEqualizer();

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


    public void setBassBoostModifier (int strength)  {
        if (strength > 0 && strength < 1000)
        if (mBassBoost == null)
        mBassBoost = new AudioBassBoost();

        if (mCurrentAudioSessionId != 0)
        mBassBoost.initializeBassEngine(mCurrentAudioSessionId);

        if (mBassBoost.getBassBoostSupported()) {
            mBassBoostStrength = strength;

            mBassBoost.setBassStrength((short)strength);

        }

    }


    public void applyModifiers() {
        setPausedModifier(mPaused);
        setMutedModifier(mMuted);
    }


    @Override
    public void onEnableStatusChange(AudioEffect effect, boolean enabled) {

        Log.i("STATUS_CHANGE", String.valueOf(effect));

    }

    @Override
    protected void onDetachedFromWindow() {
        releasePlayer();
        super.onDetachedFromWindow();
    }

    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        WritableMap event = Arguments.createMap();
        mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), event);
        mBassBoost.destroyBassEngine();

    }

    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int frameworkErr, int implErr) {
        WritableMap event = Arguments.createMap();
        WritableMap error = Arguments.createMap();
        error.putInt(EVENT_PROP_WHAT, frameworkErr);
        event.putMap(EVENT_PROP_ERROR, error);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), event);
        releasePlayer();
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int message, int val) {

        switch (message) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:

                mStalled = true;
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:

                mStalled = false;
                break;
        }

        return true;
    }

    private void restoreEqualizerState() {
        if (mEqualizerEnabled) {
            if (mEqualizer != null) {
                ijkVideoView.unbindCustomEqualizer();
                mEqualizer = null;
            }
            mEqualizer = ijkVideoView.getEqualizer();
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

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        if (mLoaded)
            return;
        mCurrentAudioSessionId = iMediaPlayer.getAudioSessionId();
        restoreEqualizerState();
        setBassBoostModifier(1);

        WritableMap event = Arguments.createMap();
        WritableArray videoTracks = new Arguments().createArray();
        WritableArray audioTracks = new Arguments().createArray();
        WritableArray textTracks = new Arguments().createArray();

        MediaInfo mediainfo = iMediaPlayer.getMediaInfo();
        ArrayList<IjkMediaMeta.IjkStreamMeta> streams = mediainfo.mMeta.mStreams;

        event.putString("format", mediainfo.mMeta.mFormat);
        event.putString("bitrate", String.valueOf(mediainfo.mMeta.mBitrate));
        event.putString("startTime", String.valueOf(mediainfo.mMeta.mStartUS));
        event.putString("audioDecoder", String.valueOf(mediainfo.mAudioDecoder));
        event.putString("audioDecoderImpl", String.valueOf(mediainfo.mAudioDecoderImpl));
        event.putString("videoDecoder", String.valueOf(mediainfo.mVideoDecoder));
        event.putString("videoDecoderImpl", String.valueOf(mediainfo.mVideoDecoderImpl));
        event.putInt("audioSessionID", iMediaPlayer.getAudioSessionId());
        event.putInt("selectedAudioTrack", ijkVideoView.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO));
        event.putInt("selectedVideoTrack", ijkVideoView.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO));
        event.putInt("selectedTextTrack", ijkVideoView.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT));
        event.putString("dataSource", iMediaPlayer.getDataSource());
        event.putBoolean("canGoForward", ijkVideoView.canSeekForward());
        event.putBoolean("canGoBackward", ijkVideoView.canSeekBackward());
        event.putBoolean("canPause", ijkVideoView.canPause());

        for (int i = 0; i < streams.size(); i++) {

            WritableMap args = new Arguments().createMap();

            IjkMediaMeta.IjkStreamMeta streamMeta = streams.get(i);

            args.putInt("track_id", streamMeta.mIndex);
            args.putString("type", streamMeta.mType);
            args.putString("codecShortName", streamMeta.mCodecName);
            args.putString("codecLongName", streamMeta.mCodecLongName);
            args.putString("codecProfile", streamMeta.mCodecProfile);
            args.putString("language", streamMeta.mLanguage);
            args.putInt("width", streamMeta.mWidth);

            args.putInt("height", streamMeta.mHeight);
            args.putString("bitrate", String.valueOf(streamMeta.mBitrate));
            args.putInt("sampleRate", streamMeta.mSampleRate);
            args.putInt("channelLayout", (int) streamMeta.mChannelLayout);
            args.putInt("fps_den", streamMeta.mFpsDen);
            args.putInt("fps_num", streamMeta.mFpsNum);
            args.putInt("sar_den", streamMeta.mSarDen);
            args.putInt("sar_num", streamMeta.mSarNum);
            args.putInt("tbr_den", streamMeta.mTbrDen);
            args.putInt("tbr_num", streamMeta.mTbrNum);

            if (streamMeta.mType.equals("video")) {
                videoTracks.pushMap(args);
            } else if (streamMeta.mType.equals("audio")) {
                audioTracks.pushMap(args);
            } else if (streamMeta.mType.equals("timedtext")) {
                textTracks.pushMap(args);
            } else {
                event.putMap("extra", args);
            }


        }

        mVideoTracks = videoTracks;
        mAudioTracks = audioTracks;
        mTextTracks = textTracks;

        event.putArray("videoTracks", videoTracks);
        event.putArray("audioTracks", audioTracks);
        event.putArray("textTracks", textTracks);

        event.putDouble(EVENT_PROP_DURATION, ijkVideoView.getDuration() / 1000.0);
        event.putDouble(EVENT_PROP_CURRENT_TIME, ijkVideoView.getCurrentPosition() / 1000.0);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD.toString(), event);
        mLoaded = true;

        applyModifiers();
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int percent) {
        if (!mStalled)
            return;
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_BUFFERING_PROG, percent / 100);
        event.putString(EVENT_PROP_DATASOURCE, iMediaPlayer.getDataSource());
        mEventEmitter.receiveEvent(getId(), Events.EVENT_STALLED.toString(), event);
    }

    @Override
    public void onHostPause() {
    }

    @Override
    public void onHostResume() {
        applyModifiers();
    }

    @Override
    public void onHostDestroy() {


    }

}
