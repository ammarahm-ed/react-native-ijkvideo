
# react-native-ijkvideo
A Cross Platform (Android & iOS) video & audio component with react-native-video like API & all formats support based on IJKPlayer FFMpeg 3.2.

**Features:**

 - FFMPEG 3.2 based IJKPlayer Backend
 - IJKplayer library size is small so don't worry about app size. its around 6 Megabytes on Android and compressed, it is even smaller.
 - Supports every format, including those that exoplayer does not support
 - react-native-video like api with some extra features.
 - Hardware decoding & async decoding
 - Disable audio or video rendering completely.
 - Equalizer, bassboost & loudness enhancer support
 - Video resizing & scaling
 - Native subtitle (SubRip ass and mov_text etc.) render on Android.
 - Multi audio, video & subtitle trackselection. 
 - Snapshotsupport
 - Video playback rate

|      Type   |  Supported   |
|:----------:|:---------------------:|
|   Protocols    |     HLS, RTMP, RTSP, HTTP, HTTPS, FILE,RTP |
|   Containers |   FLV, TS, MPEG, MOV, M4V, MP3, AAC, GIF, ASF, RM, MKV, AVI, WEBM |
| Video Encoders |  H263, H264, H265, MPEG1, MPEG2, MPEG4, MJPEG, VC-1, WMV, RV40, PNG, JPEG, YUV, WEBP, TIFF, VP* |
| Audio Encoders |  AAC, MP3, NELLYMOSER, AMRNB, AMRWB, WMV1, WMV2, WMV3, OGG, FLAC, DTS, COOK |

## Run the Example
To run the example app clone the project

    git clone https://github.com/ammarahm-ed/react-native-ijkvideo.git

      

   then run ` yarn or npm install` in the example folder and finally to run the example app:
       
   
    react-native run-android

## Installation

    npm install react-native-ijkvideo --save
or if you use yarn:

    yarn add react-native-ijkvideo

## Usage
For complete usage, see the example project.

##  IJKVideo Props


## IJKVideo Methods
ActionSheet can be opened or closed using its ref.
#

### MIT Licensed

