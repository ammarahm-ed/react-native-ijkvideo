package com.github.chadsmith.RCTIJKPlayer;

import android.graphics.Bitmap;
import android.media.audiofx.AudioEffect;

import android.os.Handler;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import bolts.Task;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaMeta;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.MediaInfo;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

public class RCTIJKPlayer extends FrameLayout implements LifecycleEventListener, onTimedTextAvailable, AudioEffect.OnEnableStatusChangeListener, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnCompletionListener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnBufferingUpdateListener {


    private ThemedReactContext themedContext;

    private IjkVideoView mVideoView;
    public RCTEventEmitter mEventEmitter;

    public boolean mPlayInBackground = true;
    private boolean mPaused = false;
    private boolean mMuted = false;
    private float mVolume = 1.0f;
    private float mPlaybackSpeed = 1.0f;
    private boolean mRepeat = false;
    private boolean mLoaded = false;
    private boolean mStalled = false;
    private String mVideoSource;
    private String mUserAgent;
    private ReadableMap mHeaders;

    private float mProgressUpdateInterval = 250;
    private Handler mProgressUpdateHandler = new Handler();
    private Runnable mProgressUpdateRunnable = null;

    public RCTIJKPlayer(ThemedReactContext themedReactContext) {
        super(themedReactContext);

        themedContext = themedReactContext;
        loadNativeJNI();
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        setFocusable(false);
        setFocusableInTouchMode(false);
        initializePlayer();

    }

    /**
     * Initialize the player
     *
     * Attach VideoView
     * Add Progress update event
     * Attach all event listeners for the video player
     */

    public void initializePlayer() {

        setVideoView();
        setProgressUpdateRunnable();
        setEventListeners();
    }

    /**
     * Set the video view and attach it to the screen.
     */

    private void setVideoView() {
        if (themedContext != null) {

            mVideoView = new IjkVideoView(themedContext);
            addView(mVideoView);
            mVideoView.setContext(themedContext, getId());
            mVideoView.setFocusable(false);
            mVideoView.setFocusableInTouchMode(false);
            setupLayoutHack();
        }
    }

    /**
     * Load ijkplayer native lib
     */

    private void loadNativeJNI() {
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
    }


    /**
     * Set event listeners for the video player
     */

    private void setEventListeners() {
        if (themedContext != null && mVideoView != null) {
            mEventEmitter = themedContext.getJSModule(RCTEventEmitter.class);
            themedContext.addLifecycleEventListener(this);
            mVideoView.setOnTimedTextAvailableListener(this);
            mVideoView.setOnPreparedListener(this);
            mVideoView.setOnErrorListener(this);
            mVideoView.setOnCompletionListener(this);
            mVideoView.setOnInfoListener(this);
            mVideoView.setOnBufferingUpdateListener(this);
        }

    }


    /**
     * Set the progressUpdateInterval
     */

    private void setProgressUpdateRunnable() {
        if (mVideoView != null)
            mProgressUpdateRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mVideoView != null && mVideoView.isPlaying() && !mPaused) {
                        WritableMap event = Arguments.createMap();
                        event.putDouble(Constants.EVENT_PROP_CURRENT_TIME, mVideoView.getCurrentPosition() / 1000.0);
                        event.putDouble(Constants.EVENT_PROP_DURATION, mVideoView.getDuration() / 1000.0);
                        event.putInt("tcpSpeed", (int) mVideoView.getTcpSpeed());
                        event.putInt("fileSize", (int) mVideoView.getFileSize());
                        mEventEmitter.receiveEvent(getId(), Constants.Events.EVENT_PROGRESS.toString(), event);

                        mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, Math.round(mProgressUpdateInterval));
                    }
                }
            };

    }


    /**
     * Set Video Source
     *
     * @param uriString   Local path or url to the video
     * @param readableMap Headers for the url if any
     * @param userAgent   Adds a userAgent
     */


    public void setSrc(final String uriString, @Nullable final ReadableMap readableMap, @Nullable final String userAgent) {
        if (uriString == null)
            return;

        WritableMap src = Arguments.createMap();
        src.putString(RCTIJKPlayerManager.PROP_SRC_URI, uriString);
        WritableMap event = Arguments.createMap();
        event.putMap(RCTIJKPlayerManager.PROP_SRC, src);
        mEventEmitter.receiveEvent(getId(), Constants.Events.EVENT_LOAD_START.toString(), event);

        mLoaded = false;
        mStalled = false;
        mVideoSource = uriString;
        mHeaders = readableMap;
        mUserAgent = userAgent;
        if (mVideoView.isPlaying()) {
            mVideoView.stopPlayback();
            mVideoView.removeView(mVideoView);
            mVideoView.release(true);
            mVideoView = null;
        }

        if (mVideoView == null) {
            initializePlayer();
        }
        if (userAgent != null && mVideoView != null)
            mVideoView.setUserAgent(userAgent);

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
            mVideoView.setVideoPath(uriString, headerMap);
        } else
            mVideoView.setVideoPath(uriString);
    }


    /**
     *
     * Play or pause the video
     * @param paused pause video if true
     */


    public void setPausedModifier(final boolean paused) {
        mPaused = paused;
        if (mVideoView == null) return;
        if (mPaused) {
            if (mVideoView.isPlaying()) {
                mVideoView.pause();
            }
        } else {
            if (!mVideoView.isPlaying()) {
                mVideoView.start();
                mProgressUpdateHandler.post(mProgressUpdateRunnable);
            }
        }
    }

    /**
     * Seek the video to given time in ms
     * @param seekTime  seek time in ms
     * @param pauseAfterSeek Should the video pause after seek has completed
     */

    public void setSeekModifier(final double seekTime, final boolean pauseAfterSeek) {
        if (mVideoView != null)
            mVideoView.seekTo((int) (seekTime * 1000));
    }


    /**
     * Set the resizeMode for the video
     * It can be one of
     * FILL_VERTICAL
     * FILL_HORIZONTAL
     * CONTAIN
     * COVER
     * STRETCH
     * ORIGINAL
     *
     * @param resizeMode type of resizeMode for the video
     *
     */

    public void setResizeModifier(final String resizeMode) {
        if (mVideoView != null)
            mVideoView.setVideoAspect(resizeMode);
    }


    /**
     * Mute the audio
      * @param muted
     */

    public void setMutedModifier(final boolean muted) {
        mMuted = muted;
        if (mVideoView == null) return;
        if (mMuted) {
            mVideoView.setVolume(0.0f, 0.0f);
        } else {
            mVideoView.setVolume(mVolume, mVolume);
        }
    }

    /**
     * Set the volume of the video, independant from Android system volume.
     * @param volume
     */

    public void setVolumeModifier(final float volume) {
        mVolume = volume;
        if (mVideoView != null) {
            if (volume > 1.0f || volume < 0.0f) return;
            mVideoView.setVolume(mVolume, mVolume);

        }
    }


    /**
     * Change audio balance between left and right Channels
     *
     * @param left left audio channel volume level
     * @param right right audio channel volume level
     *
     */



    public void setStereoPanModifier(final float left, final float right) {
        if (mVideoView != null) {
            if (left > 1.0f || left < 0.0f || right > 1.0f || right < 0.0f) return;
            mVideoView.setVolume(left, right);
        }

    }

    /**
     * set if the video should repeat itself after playing is ended
     *
     * @param repeat
     */

    public void setRepeatModifer(final boolean repeat) {
        mRepeat = repeat;
        if (mVideoView != null)
            mVideoView.repeat(mRepeat);

    }

    /**
     * The the speed of video playback
     *
     * @param rate
     */

    public void setPlaybackRateModifer(final float rate) {
        mPlaybackSpeed = rate;
        if (mVideoView != null)
            mVideoView.setPlaybackRate(mPlaybackSpeed);
    }

    /**
     * Set the interval after which progress should be updated.
     *
     * @param progressUpdateInterval
     */

    public void setProgressUpdateInterval(final int progressUpdateInterval) {

        mProgressUpdateInterval = progressUpdateInterval;
        mProgressUpdateRunnable = null;
        setProgressUpdateRunnable();

    }


    /**
     * Get the current selected track for audio, video and subtitle
     *
     * @param promise returns a promise with an object
     */

    public void getCurrentSelectedTracks(Promise promise) {
        WritableMap args = new Arguments().createMap();
        args.putInt("selectedAudioTrack", mVideoView.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO));
        args.putInt("selectedVideoTrack", mVideoView.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO));
        args.putInt("selectedTextTrack", mVideoView.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT));
        promise.resolve(args);

    }

    /**
     * Select the audio track for the given ID
     * @param trackID
     */

    public void selectAudioTrack(int trackID) {
        if (mVideoView != null)
            mVideoView.selectTrack(trackID);
    }

    /**
     * Select the video track for the given ID
     * @param trackID
     */


    public void selectVideoTrack(int trackID) {
        if (mVideoView != null)
            mVideoView.selectTrack(trackID);
    }

    /**
     * Deselect the track on a given ID
     * @param trackID
     */

    public void deselectTrack(int trackID) {
        if (mVideoView != null)
            mVideoView.deSelectTrack(trackID);
    }

    /**
     * Select text track for the given ID
     *
     * @param trackID
     */
    public void selectTextTrack(int trackID) {
        if (mVideoView != null)
            mVideoView.selectTrack(trackID);
    }



    public void setSubtitleDisplay(int textSize, String color, String position, String backgroundColor) {
        if (mVideoView != null)
            mVideoView.setSubtitleDisplay(getContext(), textSize, position, color, backgroundColor);
    }

    public void setSubtitles(final boolean subtitlesEnabled) {
        if (mVideoView != null)
            mVideoView.setSubtitles(subtitlesEnabled);
    }



    public void setAudio(final boolean audioEnabled) {
        if (mVideoView != null)
            mVideoView.setAudio(audioEnabled);

    }

    public void setVideo(final boolean videoEnabled) {
        if (mVideoView != null)
            mVideoView.setVideo(videoEnabled);

    }

    public void setAudioFocus(final boolean audioFocus) {
        if (mVideoView != null)
            if (audioFocus) {
                mVideoView.getAudioFocus();
            } else {
                mVideoView.abandonAudioFocus();
            }

    }


    public void setBackgroundPlay(final boolean playInBackground) {
        if (mVideoView != null)
            mPlayInBackground = playInBackground;


    }

    public void takeSnapshot(final String path, Promise promise) throws IOException {
        if (mVideoView == null) {
            promise.reject("Error", "video not loaded yet");
        }

        Bitmap bitmap = mVideoView.getBitmap();

        if (bitmap == null) {
            promise.reject("Error", "bitmap is null");
        }

        File dir = new File(path.replace("file://", ""));

        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "snapshots-" + UUID.randomUUID().toString() + "." + "jpeg";
        String filePath = path + fileName;
        File file = new File(filePath.replace("file://", ""));

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            promise.resolve(filePath);

        } catch (Exception e) {
            Log.d("ERROR", e.getMessage());
        } finally {

            bitmap.recycle();
            if (fos != null)
                fos.close();
        }
    }


    public void setSnapshotPath(final String snapshotPath) throws IOException {
        if (mVideoView == null)
            return;
        Bitmap bitmap = mVideoView.getBitmap();
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

    public void applyModifiers() {
        setPausedModifier(mPaused);
        setMutedModifier(mMuted);
    }


    @Override
    public void onEnableStatusChange(AudioEffect effect, boolean enabled) {

    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mPlayInBackground) {
            initializePlayer();
            setSrc(mVideoSource, mHeaders, mUserAgent);
        }

    }

    @Override
    protected void onDetachedFromWindow() {
        if (!mPlayInBackground) {

            releasePlayer();
        }
        super.onDetachedFromWindow();
    }


    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        WritableMap event = Arguments.createMap();
        mEventEmitter.receiveEvent(getId(), Constants.Events.EVENT_END.toString(), event);
    }

    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int frameworkErr, int implErr) {

        WritableMap event = Arguments.createMap();
        WritableMap error = Arguments.createMap();
        error.putInt(Constants.EVENT_PROP_WHAT, frameworkErr);
        event.putMap(Constants.EVENT_PROP_ERROR, error);
        mEventEmitter.receiveEvent(getId(), Constants.Events.EVENT_ERROR.toString(), event);
        releasePlayer();

        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int message, int val) {

        switch (message) {
            case IMediaPlayer.MEDIA_INFO_OPEN_INPUT:
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:

                setMutedModifier(true);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setMutedModifier(false);
                        setVolumeModifier(1);
                    }
                }, 2000);

                mStalled = true;
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                setMutedModifier(false);

                mStalled = false;
                break;
            case IjkMediaPlayer.MEDIA_INFO_AUDIO_SEEK_RENDERING_START:
            case IjkMediaPlayer.MEDIA_INFO_VIDEO_SEEK_RENDERING_START:
            case IjkMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                handlePlayPauseOnVideo();
                break;
        }

        return true;
    }


    private void handlePlayPauseOnVideo() {
        if (mPaused) {
            setMutedModifier(false);
            mVideoView.pause();
            WritableMap event = Arguments.createMap();
            event.putString("paused", "true");
            mEventEmitter.receiveEvent(getId(), Constants.Events.EVENT_PAUSE.toString(), event);
        } else {
            setMutedModifier(false);
            WritableMap event = Arguments.createMap();
            event.putString("paused", "false");
            mEventEmitter.receiveEvent(getId(), Constants.Events.EVENT_VIDEO_PLAYING.toString(), event);
        }

    }


    private OnAudioSessionIdRecieved mAudioSessionIdListener;

    public void setOnAudioSessionIdListener(OnAudioSessionIdRecieved audioSessionIdListener) {
        mAudioSessionIdListener = audioSessionIdListener;
    }


    @Override
    public void onTimedText(String subtitle) {
        WritableMap event = Arguments.createMap();
        event.putString("text", subtitle);
        mEventEmitter.receiveEvent(getId(), Constants.Events.EVENT_TIMED_TEXT.toString(), event);

    }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        if (mLoaded)
            return;
        if (mAudioSessionIdListener != null)
            mAudioSessionIdListener.onAudioSessionId(iMediaPlayer);

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
        event.putInt("selectedAudioTrack", mVideoView.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO));
        event.putInt("selectedVideoTrack", mVideoView.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO));
        event.putInt("selectedTextTrack", mVideoView.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT));
        event.putString("dataSource", iMediaPlayer.getDataSource());
        event.putBoolean("canGoForward", mVideoView.canSeekForward());
        event.putBoolean("canGoBackward", mVideoView.canSeekBackward());
        event.putBoolean("canPause", mVideoView.canPause());


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

        event.putArray("videoTracks", videoTracks);
        event.putArray("audioTracks", audioTracks);
        event.putArray("textTracks", textTracks);

        event.putDouble(Constants.EVENT_PROP_DURATION, mVideoView.getDuration() / 1000.0);
        event.putDouble(Constants.EVENT_PROP_CURRENT_TIME, mVideoView.getCurrentPosition() / 1000.0);
        mEventEmitter.receiveEvent(getId(), Constants.Events.EVENT_LOAD.toString(), event);
        mLoaded = true;

        applyModifiers();
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int percent) {
        if (!mStalled)
            return;
        WritableMap event = Arguments.createMap();
        event.putDouble(Constants.EVENT_PROP_BUFFERING_PROG, percent / 100);
        event.putInt("tcpSpeed", (int) mVideoView.getTcpSpeed());
        event.putInt("fileSize", (int) mVideoView.getFileSize());
        event.putString(Constants.EVENT_PROP_DATASOURCE, iMediaPlayer.getDataSource());
        mEventEmitter.receiveEvent(getId(), Constants.Events.EVENT_STALLED.toString(), event);
    }

    @Override
    public void onHostPause() {

        if (!mPlayInBackground) {
            mVideoView.pause();
        }

    }

    @Override
    public void onHostResume() {


        if (!mPlayInBackground && !mPaused) {
            setPausedModifier(false);

        }
    }

    @Override
    public void onHostDestroy() {


    }

    void setupLayoutHack() {

        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                manuallyLayoutChildren();
                getViewTreeObserver().dispatchOnGlobalLayout();
                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }

    void manuallyLayoutChildren() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        }
    }


    private void releasePlayer() {
        if (mVideoView != null)
            Task.callInBackground(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    mVideoView.setOnPreparedListener(null);
                    mVideoView.setOnErrorListener(null);
                    mVideoView.setOnCompletionListener(null);
                    mVideoView.setOnInfoListener(null);
                    mVideoView.setOnBufferingUpdateListener(null);
                    mVideoView.setOnTimedTextAvailableListener(null);
                    mVideoView.stopPlayback();
                    mProgressUpdateRunnable = null;
                    removeView(mVideoView);
                    mVideoView = null;
                    return null;
                }

            });
    }
}
