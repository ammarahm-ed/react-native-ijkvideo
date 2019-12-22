package com.github.chadsmith.RCTIJKPlayer;


import androidx.annotation.Nullable;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.github.chadsmith.RCTIJKPlayer.RCTIJKPlayer.Events;

import java.io.IOException;
import java.util.Map;

public class RCTIJKPlayerManager extends ViewGroupManager<RCTIJKPlayer> {

    private static final String REACT_CLASS = "RCTIJKPlayer";

    private RCTIJKPlayer videoView;

    public RCTIJKPlayerManager(ReactApplicationContext reactContext) {
        super();
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public RCTIJKPlayer createViewInstance(ThemedReactContext context) {
        videoView = new RCTIJKPlayer(context);

        return videoView;

    }

    public static final String PROP_SRC = "src";
    public static final String PROP_SRC_HEADERS = "headers";
    public static final String PROP_SRC_URI = "uri";
    public static final String PROP_SRC_USER_AGENT = "userAgent";
    public static final String PROP_MUTED = "muted";
    public static final String PROP_VIDEO_PAUSED = "paused";
    public static final String PROP_SEEK = "seek";
    public static final String PROP_VOLUME = "volume";
    public static final String PROP_SNAPSHOT_PATH = "snapshotPath";
    public static final String PROP_RESIZE_MODE = "resizeMode";

    private static final String PROP_REPEAT = "repeat";
    private static final String PROP_PROGRESS_UPDATE_INTERVAL = "progressUpdateInterval";
    private static final String PROP_RATE = "rate";
    private static final String PROP_AUDIO_TRACK = "selectedAudioTrack";
    private static final String PROP_VIDEO_TRACK = "selectedVideoTrack";
    private static final String PROP_TEXT_TRACK = "selectedTextTrack";
    private static final String PROP_SUBTITLE_DISPLAY = "subtitleStyle";
    private static final String PROP_SUBTITLES = "subtitlesEnabled";
    private static final String PROP_DISABLE_AUDIO = "audioDisabled";
    private static final String PROP_DISABLE_VIDEO = "videoDisabled";
    private static final String PROP_AUDIO_FOCUS = "audioFocus";
    private static final String PROP_PLAY_IN_BACKGROUND = "playInBackground";
    private static final String PROP_EQUALIZER_ENABLED = "equalizerEnabled";
    private static final String PROP_ASYNC_DECODING = "asyncDecoding";




    public RCTIJKPlayer getPlayerInstance() { // <-- returns the View instance

        return videoView;
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (Events event : Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }

    @ReactProp(name = PROP_SRC)
    public void setSrc(final RCTIJKPlayer videoView, @Nullable ReadableMap src) throws IOException {
        String uri = src.getString(PROP_SRC_URI);
        ReadableMap headers = null;
        if (src.hasKey(PROP_SRC_HEADERS))
            headers = src.getMap(PROP_SRC_HEADERS);
        String userAgent = src.getString(PROP_SRC_USER_AGENT);
        videoView.setSrc(uri, headers, userAgent);
    }

    @ReactProp(name = PROP_MUTED, defaultBoolean = false)
    public void setMuted(final RCTIJKPlayer videoView, final boolean muted) {
        videoView.setMutedModifier(muted);
    }

    @ReactProp(name = PROP_SEEK, defaultDouble = 0.0)
    public void setSeek(final RCTIJKPlayer videoView, final double seekTime) {
        videoView.setSeekModifier(seekTime);
    }

    @ReactProp(name = PROP_SNAPSHOT_PATH)
    public void setSnapshotPath(final RCTIJKPlayer videoView, final String snapshotPath) throws IOException {
        videoView.setSnapshotPath(snapshotPath);
    }

    @ReactProp(name = PROP_VIDEO_PAUSED, defaultBoolean = false)
    public void setPaused(final RCTIJKPlayer videoView, final boolean paused) {
        videoView.setPausedModifier(paused);
    }

    @ReactProp(name = PROP_RESIZE_MODE)
    public void setResizeMode(final RCTIJKPlayer videoView, final String resizeMode) {

        videoView.setResizeModifier(resizeMode);
    }

    @ReactProp(name = PROP_VOLUME, defaultFloat = 1.0f)
    public void setVolume(final RCTIJKPlayer videoView, final float volume) {

        videoView.setVolumeModifier(volume);

    }

    @ReactProp(name = PROP_REPEAT, defaultBoolean = false)
    public void setRepeat(final RCTIJKPlayer videoView, final boolean repeat) {
        videoView.setRepeatModifer(repeat);
    }

    @ReactProp(name = PROP_RATE, defaultFloat = 1.0f)
    public void setPlaybackRate(final RCTIJKPlayer videoView, final float rate) {
        videoView.setPlaybackRateModifer(rate);
    }

    @ReactProp(name = PROP_PROGRESS_UPDATE_INTERVAL, defaultInt = 250)
    public void setProgressUpdateInterval(final RCTIJKPlayer videoView, final int progressUpdateInterval) {
        videoView.setProgressUpdateInterval(progressUpdateInterval);
    }

    @ReactProp(name = PROP_AUDIO_TRACK)
    public void selectAudioTrack(final RCTIJKPlayer videoView, final int trackID) {
        videoView.selectAudioTrack(trackID);
    }

    @ReactProp(name = PROP_VIDEO_TRACK)
    public void selectVideoTrack(final RCTIJKPlayer videoView, final int trackID) {
        videoView.selectVideoTrack(trackID);
    }

    @ReactProp(name = PROP_TEXT_TRACK)
    public void selectTextTrack(final RCTIJKPlayer videoView, final int trackID) {
        videoView.selectTextTrack(trackID);
    }

    @ReactProp(name = PROP_SUBTITLE_DISPLAY)
    public void setSubtitleDisplay(final RCTIJKPlayer videoView, ReadableMap subtitleStyle) {

        String color = subtitleStyle.getString("color");
        String backgroundColor = subtitleStyle.getString("backgroundColor");
        int textSize = subtitleStyle.getInt("textSize");
        String position = subtitleStyle.getString("position");

        videoView.setSubtitleDisplay(textSize, color, position, backgroundColor);
    }

    @ReactProp(name = PROP_SUBTITLES, defaultBoolean = false)
    public void setSubtitle(final RCTIJKPlayer videoView, final boolean subtitlesEnabled) {
        videoView.setSubtitles(subtitlesEnabled);
    }

    @ReactProp(name = PROP_DISABLE_AUDIO, defaultBoolean = false)
    public void setAudio(final RCTIJKPlayer videoView, final boolean audioDisabled) {
        videoView.setAudio(audioDisabled);
    }

    @ReactProp(name = PROP_DISABLE_VIDEO, defaultBoolean = false)
    public void setVideo(final RCTIJKPlayer videoView, final boolean videoDisabled) {
        videoView.setVideo(videoDisabled);
    }

    @ReactProp(name = PROP_AUDIO_FOCUS, defaultBoolean = true)
    public void setAudioFocus(final RCTIJKPlayer videoView, final boolean audioFocus) {

        videoView.setAudioFocus(audioFocus);
    }


    @ReactProp(name = PROP_PLAY_IN_BACKGROUND, defaultBoolean = false)
    public void setBackgroundPlay(final RCTIJKPlayer videoView, final boolean playInBackground) {

        videoView.setBackgroundPlay(playInBackground);
    }

    @ReactProp(name = PROP_EQUALIZER_ENABLED, defaultBoolean = false)
    public void setEqualizerEnabled(final RCTIJKPlayer videoView, final boolean equalizerEnabled) {

        videoView.setEqualizerModifier(equalizerEnabled);
    }

    @ReactProp(name = PROP_ASYNC_DECODING, defaultBoolean = false)
    public void setAsyncDecoding(final RCTIJKPlayer videoView, final boolean asyncDecoding) {

        // TODO

        //videoView.setEqualizerModifier(asyncDecoding);

    }








}
