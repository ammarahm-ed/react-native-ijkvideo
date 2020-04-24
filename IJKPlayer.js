import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { StyleSheet, requireNativeComponent, View, ViewPropTypes,NativeModules, findNodeHandle} from 'react-native';
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';

const styles = StyleSheet.create({
  base: {
    overflow: 'hidden',
  },
});

const IJKPlayerModule = NativeModules.IJKPlayerModule;
export default class IJKPlayer extends Component {

  constructor(props) {
    super(props);
  }

  setNativeProps(nativeProps) {
    this._root.setNativeProps(nativeProps);
  }

  seek = (time,pauseAfterSeek) => {
    IJKPlayerModule.seek(time,pauseAfterSeek);
    //this.setNativeProps({ seek: time, pauseAfterSeek: pauseAfterSeek });
  };

  setVolume = (volume) => {
    this.setNativeProps({volume:volume});
  }
  setPaused  = (paused) => {
    this.setNativeProps({paused:paused})
  }

  setMute = (mute) => {
    this.setNativeProps({mute:mute});
  }

  takeSnapshot = async (path) => {
    return await IJKPlayerModule.takeSnapshot(path);
  }

  setPan = (pan) => {
    let l = 1;
    let r = 1;
    if (pan < 0) {
      l = 1;
      r = 1 + pan;
    } else {
      r = 1;
      l = 1 - pan;
    }
    console.log(l,r);
    IJKPlayerModule.setPan(l,r)
  }

  snapshot = (snapshotPath) => {
    this.setNativeProps({ snapshotPath });
  };

  _assignRoot = (component) => {
    this._root = component;
  };

  setEQPreset = (preset) => {
    //IJKPlayerModule.seek(10);
    IJKPlayerModule.setEQPreset(preset);
  }

  _onLoadStart = (event) => {
    if (this.props.onLoadStart) {
      this.props.onLoadStart(event.nativeEvent);
    }
  };

  _onLoad = (event) => {
   
    IJKPlayerModule.init();
    if (this.props.onLoad) {
      this.props.onLoad(event.nativeEvent);
      
    }
  };

  _onError = (event) => {
    if (this.props.onError) {
      this.props.onError(event.nativeEvent);
    }
  };

  _onProgress = (event) => {
  
    if (this.props.onProgress) {
      this.props.onProgress(event.nativeEvent);
    }
  };

  _onPause = (event) => {
    if (this.props.onPause) {
      this.props.onPause(event.nativeEvent);
    }
  };

  _onStop = (event) => {
    if (this.props.onStop) {
      this.props.onStop(event.nativeEvent);
    }
  };

  _onEnd = (event) => {
    if (this.props.onEnd) {
      this.props.onEnd(event.nativeEvent);
    }
  };

  _onBuffer = (event) => {
    if (this.props.onBuffer) {
      this.props.onBuffer(event.nativeEvent);
    }
  };

  _onTimedText = (event) => {
    console.log("HERE",event.nativeEvent);
  if (this.props.onTimedText) {
    this.props.onTimedText(event.nativeEvent);
  }
  }

  setTextTrackIndex = (index) => {
    this.setNativeProps({selectedTextTrack:index})

  }

  setAudioTrackIndex = (index) => {
    IJKPlayerModule.getSelectedTracks().then(e => console.log(e));
    console.log(index, 'setting track');
    this.setNativeProps({selectedAudioTrack:index})
    
  }

  deselectTrack = (index) => {
    this.setNativeProps({deselectTrack:index});
  }

  _onPlay = () => {
    if (this.props.onPlay) {
      this.props.onPlay();
    }
  }



  render() {
    const source = resolveAssetSource(this.props.source) || {};
    const headers = source.headers? source.headers : {};
    const userAgent = source.userAgent? source.userAgent : '';

    let uri = source.uri || '';
    if (uri && uri.match(/^\//)) {
      uri = `file://${uri}`;
    }

    const nativeProps = Object.assign({}, this.props);
    Object.assign(nativeProps, {
      style: [ styles.base, nativeProps.style ],
      src: {
        uri,
        headers,
        userAgent,
      },
      onVideoLoadStart: this._onLoadStart,
      onVideoLoad: this._onLoad,
      onVideoError: this._onError,
      onVideoProgress: this._onProgress,
      onVideoPause: this._onPause,
      onVideoStop: this._onStop,
      onVideoEnd: this._onEnd,
      onVideoBuffer: this._onBuffer,
      onTimedText: this._onTimedText,
      onPlay:this._onPlay
    });

    return (
      <RCTIJKPlayer
        ref={this._assignRoot}
        
        {...nativeProps} />
    );
  }

}

IJKPlayer.propTypes = {
  /* Native only */
  src: PropTypes.object,
  seek: PropTypes.number,
  snapshotPath: PropTypes.string,
  onVideoLoadStart: PropTypes.func,
  onVideoLoad: PropTypes.func,
  onVideoBuffer: PropTypes.func,
  onVideoError: PropTypes.func,
  onVideoProgress: PropTypes.func,
  onVideoPause: PropTypes.func,
  onVideoStop: PropTypes.func,
  onVideoEnd: PropTypes.func,

  /* Wrapper component */
  source: PropTypes.oneOfType([
    PropTypes.shape({
      uri: PropTypes.string,
      headers: PropTypes.object,
      userAgent: PropTypes.string,
    }),
    // Opaque type returned by require('./video.mp4')
    PropTypes.number
  ]),
  muted: PropTypes.bool,
  volume: PropTypes.number,
  onLoadStart: PropTypes.func,
  onLoad: PropTypes.func,
  onBuffer: PropTypes.func,
  onError: PropTypes.func,
  onProgress: PropTypes.func,
  onPause: PropTypes.func,
  onStop: PropTypes.func,
  onEnd: PropTypes.func,

  /* Required by react-native */
  scaleX: PropTypes.number,
  scaleY: PropTypes.number,
  translateX: PropTypes.number,
  translateY: PropTypes.number,
  rotation: PropTypes.number,
  ...ViewPropTypes,
};

const RCTIJKPlayer = requireNativeComponent('RCTIJKPlayer', IJKPlayer, {
  nativeOnly: {
    src: true,
    seek: true,
    snapshotPath: true,
  },
});
