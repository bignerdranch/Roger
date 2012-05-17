//
//  FADBMonitor.m
//  Roger
//
//  Created by Bill Phillips on 5/13/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FADBMonitor.h"
#import "FADB.h"
#import "FADBDevice.h"
#import "FIntent.h"
#import "FTaskStream.h"

@interface FADBMonitor ()

@property (nonatomic, strong) NSMutableSet *connectedDeviceNames;
@property (nonatomic, strong) NSMutableDictionary *deviceMap;
@property (assign) BOOL dead;

-(void)checkDevices:(NSTimer *)timer;
-(void)connect:(NSString *)deviceName;

@end

@implementation FADBMonitor

@synthesize adb=_adb;
@dynamic devices;

@synthesize connectedDeviceNames=_connectedDevices;
@synthesize deviceMap=_deviceMap;
@synthesize dead=_dead;

-(id)initWithAdb:(FADB *)adb
{
    if ((self = [super init])) {
        _adb = adb;
        self.connectedDeviceNames = [[NSMutableSet alloc] init];
        self.deviceMap = [[NSMutableDictionary alloc] init];
        [NSTimer scheduledTimerWithTimeInterval:1.0
                                         target:self 
                                       selector:@selector(checkDevices:)
                                       userInfo:nil
                                        repeats:YES];
    }

    return self;
}

-(NSArray *)devices
{
    NSMutableArray *result = [[NSMutableArray alloc] init];

    for (FADBDevice *device in [self.deviceMap objectEnumerator]) {
        [result addObject:device];
    }

    return result;
}

-(void)kill
{
    self.dead = YES;
}

-(void)checkDevices:(NSTimer *)timer
{
    if (self.dead) {
        [timer invalidate];
        return;
    }

    [self checkDevices];
}

-(void)checkDevices
{
    [self.adb listDevicesWithBlock:^(NSArray *deviceNames) {
        NSMutableArray *newConnections = [[NSMutableArray alloc] init];

        @synchronized (self.connectedDeviceNames) {
            for (NSString *name in deviceNames) {
                if (![self.connectedDeviceNames containsObject:name]) {
                    [newConnections addObject:name];
                    [self.connectedDeviceNames addObject:name];
                }
            }
        }

        for (NSString *name in newConnections) {
            [self connect:name];
        }
    }];
}

-(void)pingDeviceName:(NSString *)name
{
    FIntent *pingIntent = [[FIntent alloc]
        initBroadcastWithAction:@"com.bignerdranch.franklin.roger.ACTION_PING"];

    [self.adb sendIntent:pingIntent toDevice:name completion:nil];
}

-(void)pingConnectedDevices
{
    @synchronized (self.connectedDeviceNames) {
        for (NSString *name in self.connectedDeviceNames) {
            [self pingDeviceName:name];
        }
    }
}

-(void)connect:(NSString *)name
{
    NSLog(@"ADBMonitor: attempting to connect to new device: %@", name);

    // run logcat
    NSArray *args = [NSArray arrayWithObjects:
        @"-s", name, @"logcat", nil];

    void (^disconnect)(void) = ^{
        NSLog(@"ADBMonitor: disconnected from %@", name);
        @synchronized (self.connectedDeviceNames) {
            [self.connectedDeviceNames removeObject:name];
            [self.deviceMap removeObjectForKey:name];
        }
    };
    // run logcat... (may want to preserve this task later)
    NSTask *task = [self.adb runAdbTaskWithArgs:args
                                      logPrefix:nil //ugh, don't log this
                                     completion:disconnect];
    FTaskStream *stream = [FTaskStream taskStreamForLaunchedTask:task];
    //[stream addLogEventsWithPrefix:@"ADBMonitor connection"];
    // when we see no external files dir, unmap the device
    [stream addOutputEvent:@"no external files dir" withBlock:^(NSString *line) {
        if (line) {
            [self.deviceMap removeObjectForKey:name];
        }
    }];
    // and when we do find one, map it
    [stream addOutputEvent:@"external files dir ::::= " withBlock:^(NSString *line) {
        if (line) {
            line = [line stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
            NSArray *components = [line componentsSeparatedByString:@" ::::= "];
            if ([components count] == 2) {
                NSString *storagePath = [components objectAtIndex:1];
                FADBDevice *device = [[FADBDevice alloc] 
                    initWithSerial:name
                       storagePath:storagePath];

                NSLog(@"added device: %@", device);
                [self.deviceMap setObject:device forKey:name];
            }
        }
    }];

    // now go ahead and ping the device
    [self pingDeviceName:name];
}

-(void)dealloc
{
    NSLog(@"FADBMonitor dealloc");
}

@end
