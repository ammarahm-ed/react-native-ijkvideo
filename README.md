

# react-native-ijkvideo
A Cross Platform (Android & iOS) video & audio component with react-native-video like API & all formats support based on IJKPlayer FFMpeg 3.2.

# Work in Progress... 
This project is in very early stages and not suggested for production level apps.

**Features:**

 - FFMPEG 3.2 based IJKPlayer Backend
 - IJKplayer library size is small so don't worry about app size. its around 6 Megabytes on Android and compressed, it is even smaller.
 - Supports every format, including those that exoplayer does not support
 - react-native-video like api with some extra features.
 - MediaCodec Hardware decoding
 - Disable audio or video rendering completely.
 - Equalizer, bassboost & loudness enhancer support
 - Video resizing & scaling
 - Native subtitle (SubRip ass and mov_text etc.) render on Android.
 - Multi audio, video & subtitle trackselection. 
 - Snapshot support
 - Video Mirroring
 - Video playback rate
 - Background playback
 - Support for H.265/HEVC playback
 - SoundTouch Support
 - Async Decoding Multi Thread
 - Uses TextureView on Android


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

# Reference

## Props

### `src`

Sets the media source. 

| Type | Required |Platform|
| ---- | -------- |-------|
| FILE,RTMP,HTTP,HTTPS,RTSP | Yes |Android, iOS

---
### `seek`

Seek to the specified position represented by seconds.

| Type | Required |Platform|
| ---- | -------- |-------|
| number(seconds) | no |Android, iOS

    Example:
    
    seek={200} // seek video to 3 minutes & 2 seconds

---

# TODO

- [x]  Android Implementation
- [ ]  iOS Implementation
- [ ]  Publish binaries to Cocoapods and Bintray
- [ ]  Add Typescript Definations
- [ ]  Create full documentation on now.sh
- [ ]  Cleanup code


### MIT Licensed

