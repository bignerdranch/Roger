//
//  FADBConnection.m
//  Roger
//
//  Created by Bill Phillips on 6/3/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FADBConnection.h"
#import "FADBDevice.h"
#import "FADB.h"
#import "FTaskStream.h"
#import "FIntent.h"

#import "NSString+Regexen.h"

@interface FADBConnection ()

@property (assign) int connectionTime;
@property (assign) BOOL parsingPing;
@property (nonatomic, weak) NSTimer *pingTimeoutTimer;

- (FADBConnection *)initWithADB:(FADB *)adb serial:(NSString *)serial;
- (void)connect;
- (void)pingTimeout:(NSTimer *)timer;

@end

@implementation FADBConnection

@synthesize delegate=_delegate;
@synthesize serial=_serial;
@synthesize adb=_adb;
@synthesize clientId=_clientId;
@synthesize storagePath=_storagePath;

@synthesize connectionTime=_connectionTime;
@synthesize parsingPing=_parsingPing;
@synthesize pingTimeoutTimer=_pingTimeoutTimer;

+ (FADBConnection *)connectionWithADB:(FADB *)adb serial:(NSString *)serial
{
    return [[FADBConnection alloc] initWithADB:adb serial:serial];
}

- (id)initWithADB:(FADB *)adb serial:(NSString *)serial
{
    if ((self = [super init])) {
        _serial = [serial copy];
        _adb = adb;
    }

    return self;
}

- (void)connect
{
    NSLog(@"ADBConnection: attempting to connect to new device: %@", self.serial);
    
    self.connectionTime = [[NSDate date] timeIntervalSince1970];

    // run logcat
    NSArray *args = [NSArray arrayWithObjects:
        @"-s", self.serial, @"logcat", nil];

    void (^disconnect)(void) = ^{
        NSLog(@"ADBConnection: disconnected from %@", self.serial);
        [self.delegate adbConnectionDisconnected:self];
    };

    // run logcat... (may want to preserve this task later)
    NSTask *task = [self.adb runAdbTaskWithArgs:args
                                      logPrefix:nil //ugh, don't log this
                                     completion:disconnect];
    FTaskStream *stream = [FTaskStream taskStreamForLaunchedTask:task];

    self.parsingPing = NO;
    [stream addOutputEvent:[NSString stringWithFormat:@"begin ping \\{%d\\}", self.connectionTime] withBlock:^(NSString *line) {
        if (line) {
            self.parsingPing = YES;
        }
    }];
    // when we see no external files dir, unmap the device
    [stream addOutputEvent:@"no external files dir" withBlock:^(NSString *line) {
        if (line && self.parsingPing) {
            self.storagePath = nil;
        }
    }];
    // and when we do find one, map it
    [stream addOutputEvent:@"external files dir ::::= " withBlock:^(NSString *line) {
        if (line && self.parsingPing) {
            line = [line stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
            NSArray *components = [line componentsSeparatedByString:@" ::::= "];
            if ([components count] == 2) {
                self.storagePath = [components objectAtIndex:1];
            }
        }
    }];
    [stream addOutputEvent:@"git android hash: " withBlock:^(NSString *line) {
        if (line && self.parsingPing) {
            self.clientId = [[line stringsFromFirstMatchOfPattern:@"\\{([0-9a-fA-F-]+)\\}"] objectAtIndex:0];
        }
    }];
    [stream addOutputEvent:[NSString stringWithFormat:@"end ping"] withBlock:^(NSString *line) {
        if (line && self.parsingPing) {
            self.parsingPing = NO;
            [self.pingTimeoutTimer invalidate];
            [self.delegate adbConnection:self pingResponseDevice:[self device]];
        }
    }];

    // now go ahead and ping our device
    [self ping];
}

-(void)ping
{
    FIntent *pingIntent = [[FIntent alloc]
        initBroadcastWithAction:@"com.bignerdranch.franklin.roger.ACTION_PING"];
    [pingIntent setExtra:@"com.bignerdranch.franklin.roger.EXTRA_LAYOUT_TXN_ID" 
                  number:[NSNumber numberWithInt:self.connectionTime]];

    [self.adb sendIntent:pingIntent toDevice:self.serial completion:nil];
    self.pingTimeoutTimer = [NSTimer 
        scheduledTimerWithTimeInterval:2.0
                                target:self
                              selector:@selector(pingTimeout:)
                              userInfo:nil
                               repeats:NO];
}

- (void)pingTimeout:(NSTimer *)timer
{
    // we got nothing back from the ping, but go ahead and give a ping
    // response. this will yield an FADBDevice without much in it.
    [self.delegate adbConnection:self pingResponseDevice:[self device]];
}

- (FADBDevice *)device
{
    return [[FADBDevice alloc] 
        initWithSerial:self.serial
           storagePath:self.storagePath
              clientId:self.clientId
            connection:self];
}

@end
