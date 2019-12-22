#import <React/RCTConvert.h>
#import "RCTIJKPlayer.h"
#import <React/RCTBridgeModule.h>
#import <React/RCTEventDispatcher.h>
#import <React/UIView+React.h>
#import <IJKMediaFrameworkWithSSL/IJKFFMoviePlayerController.h>

@implementation RCTIJKPlayer
{
    RCTEventDispatcher *_eventDispatcher;
    IJKFFMoviePlayerController *_player;

    BOOL _paused;
    BOOL _muted;
    BOOL _started;

    Float64 _progressUpdateInterval;
    id _timeObserver;
}

- (instancetype)initWithEventDispatcher:(RCTEventDispatcher *)eventDispatcher
{
    if ((self = [super init])) {
        _eventDispatcher = eventDispatcher;

        _paused = NO;
        _muted = NO;
        _started = NO;

        _progressUpdateInterval = 250;

        NSNotificationCenter *defaultCenter = [NSNotificationCenter defaultCenter];

        [defaultCenter addObserver:self
                          selector:@selector(applicationWillResignActive:)
                              name:UIApplicationWillResignActiveNotification
                            object:nil];
        [defaultCenter addObserver:self
                          selector:@selector(applicationWillEnterForeground:)
                              name:UIApplicationWillEnterForegroundNotification
                            object:nil];
        [defaultCenter addObserver:self
                          selector:@selector(loadStateDidChange:)
                              name:IJKMPMoviePlayerLoadStateDidChangeNotification
                            object:_player];
        [defaultCenter addObserver:self
                          selector:@selector(moviePlayBackDidFinish:)
                              name:IJKMPMoviePlayerPlaybackDidFinishNotification
                            object:_player];
        [defaultCenter addObserver:self
                          selector:@selector(mediaIsPreparedToPlayDidChange:)
                              name:IJKMPMediaPlaybackIsPreparedToPlayDidChangeNotification
                            object:_player];
        [defaultCenter addObserver:self
                          selector:@selector(moviePlayBackStateDidChange:)
                              name:IJKMPMoviePlayerPlaybackStateDidChangeNotification
                            object:_player];
        [defaultCenter addObserver:self
                          selector:@selector(movieSeekDidComplete:)
                              name:IJKMPMoviePlayerDidSeekCompleteNotification
                            object:_player];
    }

    return self;
}

- (void)applicationWillResignActive:(NSNotification *)notification
{
    if (_paused)
        return;
    if(_player && _player.isPlaying)
        [_player pause];
}

- (void)applicationWillEnterForeground:(NSNotification *)notification
{
    [self applyModifiers];
}

- (void)applyModifiers
{
    if(_player && _player.isPreparedToPlay) {
        if (_muted)
            [_player setPlaybackVolume:0.0];
        else
            [_player setPlaybackVolume:1.0];
    }
    [self setPaused:_paused];
}

- (void)setPaused:(BOOL)paused
{
    if(_player && _player.isPreparedToPlay) {
        if (paused)
            [_player pause];
        else
            [_player play];
    }
    _paused = paused;
}

- (void)setMuted:(BOOL)muted
{
    _muted = muted;
    [self applyModifiers];
}

-(void)setSrc:(NSDictionary *)source
{
    if(_player)
        [_player shutdown];
    _started = NO;

    NSDictionary* headers = [source objectForKey:@"headers"];
    NSString* uri = [source objectForKey:@"uri"];
    NSString* userAgent = [source objectForKey:@"userAgent"];
    NSURL* url = [NSURL URLWithString:uri];

    IJKFFOptions *ijkOptions = [IJKFFOptions optionsByDefault];
    [ijkOptions setOptionIntValue:1 forKey:@"infbuf" ofCategory:kIJKFFOptionCategoryPlayer];
    [ijkOptions setOptionIntValue:0 forKey:@"packet-buffering" ofCategory:kIJKFFOptionCategoryPlayer];
    if(headers) {
        NSMutableArray *headerArray = [[NSMutableArray alloc] init];
        for(id key in headers)
            [headerArray addObject:[NSString stringWithFormat:@"%@: %@", key, [headers objectForKey:key] ]];
        [ijkOptions setFormatOptionValue:[headerArray componentsJoinedByString:@"\r\n"] forKey:@"headers"];
    }
    if(userAgent)
        [ijkOptions setFormatOptionValue:userAgent forKey:@"user-agent"];
    _player = [[IJKFFMoviePlayerController alloc] initWithContentURL:url withOptions:ijkOptions];
    _player.view.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
    _player.view.frame = self.bounds;
    _player.scalingMode = IJKMPMovieScalingModeAspectFit;
    _player.shouldAutoplay = YES;
    [_player setPauseInBackground:YES];
    self.autoresizesSubviews = YES;
    [self addSubview:_player.view];
    if(!_timeObserver)
        _timeObserver = [NSTimer scheduledTimerWithTimeInterval: _progressUpdateInterval / 1000
                                                         target: self
                                                       selector: @selector(onProgressUpdate)
                                                       userInfo: nil
                                                        repeats: YES];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        if(self.onVideoLoadStart)
            self.onVideoLoadStart(@{@"src": @{
                                            @"uri": uri ? uri : [NSNull null],
                                            },
                                    @"target": self.reactTag
                                    });
    });
    [_player prepareToPlay];
}

- (void)setSeek:(int)seekTime
{
    if(_player && _player.isPreparedToPlay)
        [_player setCurrentPlaybackTime:seekTime];
}

- (void)setSnapshotPath:(NSString *)snapshotPath
{
    if(_player && _player.isPlaying) {
        UIImage *image = [_player thumbnailImageAtCurrentTime];
        [UIImagePNGRepresentation(image) writeToFile:snapshotPath atomically:YES];
    }
}

- (void)onProgressUpdate
{
    if(_player && _player.isPlaying && self.onVideoProgress) {
        float currentTime   = (float) [_player currentPlaybackTime];
        float duration      = (float) [_player duration];
        float remainingTime = duration - currentTime;
        float position = duration > 0 ? currentTime / duration : 0.0;
        if(currentTime >= 0 && (duration == 0 || currentTime < duration))
            self.onVideoProgress(@{
                                   @"target": self.reactTag,
                                   @"currentTime": [NSNumber numberWithFloat:currentTime],
                                   @"remainingTime": [NSNumber numberWithFloat:remainingTime],
                                   @"duration":[NSNumber numberWithFloat:duration],
                                   @"position":[NSNumber numberWithFloat:position]
                                   });
    }
}

- (void)loadStateDidChange:(NSNotification*)notification
{
    //    MPMovieLoadStateUnknown        = 0,
    //    MPMovieLoadStatePlayable       = 1 << 0,
    //    MPMovieLoadStatePlaythroughOK  = 1 << 1, // Playback will be automatically started in this state when shouldAutoplay is YES
    //    MPMovieLoadStateStalled        = 1 << 2, // Playback will be automatically paused in this state, if started
    IJKMPMovieLoadState loadState = _player.loadState;
    if ((loadState & IJKMPMovieLoadStatePlaythroughOK) != 0) {
        NSLog(@"loadStateDidChange: IJKMPMovieLoadStatePlaythroughOK: %d\n", (int)loadState);
    } else if ((loadState & IJKMPMovieLoadStateStalled) != 0) {
        NSLog(@"loadStateDidChange: IJKMPMovieLoadStateStalled: %d\n", (int)loadState);
        if(self.onVideoBuffer)
            self.onVideoBuffer(@{ @"target": self.reactTag });
    } else {
        NSLog(@"loadStateDidChange: ???: %d\n", (int)loadState);
    }
}

- (void)moviePlayBackDidFinish:(NSNotification*)notification
{
    //    MPMovieFinishReasonPlaybackEnded,
    //    MPMovieFinishReasonPlaybackError,
    //    MPMovieFinishReasonUserExited
    int reason = [[[notification userInfo] valueForKey:IJKMPMoviePlayerPlaybackDidFinishReasonUserInfoKey] intValue];
    switch (reason) {
        case IJKMPMovieFinishReasonPlaybackEnded:
            NSLog(@"playbackStateDidChange: IJKMPMovieFinishReasonPlaybackEnded: %d\n", reason);
            if(self.onVideoEnd)
                self.onVideoEnd(@{ @"target": self.reactTag });
            break;
        case IJKMPMovieFinishReasonUserExited:
            NSLog(@"playbackStateDidChange: IJKMPMovieFinishReasonUserExited: %d\n", reason);
            if(self.onVideoEnd)
                self.onVideoEnd(@{ @"target": self.reactTag });
            break;
        case IJKMPMovieFinishReasonPlaybackError:
            NSLog(@"playbackStateDidChange: IJKMPMovieFinishReasonPlaybackError: %d\n", reason);
            if(self.onVideoError)
                self.onVideoError(@{ @"target": self.reactTag });
            break;
        default:
            NSLog(@"playbackPlayBackDidFinish: ???: %d\n", reason);
            break;
    }
}

- (void)mediaIsPreparedToPlayDidChange:(NSNotification*)notification
{
    NSLog(@"mediaIsPreparedToPlayDidChange");
}

- (void)moviePlayBackStateDidChange:(NSNotification*)notification
{
    //    MPMoviePlaybackStateStopped,
    //    MPMoviePlaybackStatePlaying,
    //    MPMoviePlaybackStatePaused,
    //    MPMoviePlaybackStateInterrupted,
    //    MPMoviePlaybackStateSeekingForward,
    //    MPMoviePlaybackStateSeekingBackward
    switch (_player.playbackState)
    {
        case IJKMPMoviePlaybackStateStopped: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: stopped", (int)_player.playbackState);
            if(self.onVideoStop)
                self.onVideoStop(@{ @"target": self.reactTag });
            break;
        }
        case IJKMPMoviePlaybackStatePlaying: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: playing", (int)_player.playbackState);
            if(!_started) {
                _started = YES;
                _paused = NO;
                [self applyModifiers];
                if(self.onVideoLoad)
                    self.onVideoLoad(@{
                                       @"target": self.reactTag,
                                       @"duration": [NSNumber numberWithFloat:(float) _player.duration]
                                       });
            }
            break;
        }
        case IJKMPMoviePlaybackStatePaused: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: paused", (int)_player.playbackState);
            if(self.onVideoPause)
                self.onVideoPause(@{ @"target": self.reactTag });
            break;
        }
        case IJKMPMoviePlaybackStateInterrupted: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: interrupted", (int)_player.playbackState);
            break;
        }
        case IJKMPMoviePlaybackStateSeekingForward:
        case IJKMPMoviePlaybackStateSeekingBackward: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: seeking", (int)_player.playbackState);
            break;
        }
        default: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: unknown", (int)_player.playbackState);
            break;
        }
    }
}

- (void)movieSeekDidComplete:(NSNotification*)notification
{
    if(![_player isPlaying])
        [_player play];
}

#pragma mark - Lifecycle
- (void) removeFromSuperview
{
    if(_player)
        [_player shutdown];
    if(_timeObserver) {
        [_timeObserver invalidate];
        _timeObserver = nil;
    }
    _eventDispatcher = nil;
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [super removeFromSuperview];
}

@end
