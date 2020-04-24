package com.github.chadsmith.RCTIJKPlayer;

public class Constants {

    public static enum Events {
        EVENT_LOAD_START("onVideoLoadStart"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_STALLED("onVideoBuffer"),
        EVENT_ERROR("onVideoError"),
        EVENT_PROGRESS("onVideoProgress"),
        EVENT_PAUSE("onVideoPause"),
        EVENT_STOP("onVideoStop"),
        EVENT_END("onVideoEnd"),
        EVENT_TIMED_TEXT("onTimedText"),
        EVENT_VIDEO_PLAYING("onPlay");
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
}
