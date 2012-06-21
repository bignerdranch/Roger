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
#import "FDevices.h"
#import "RogerBuild.h"
#import "NSString+Regexen.h"
#import "FADBConnection.h"

#import "RogerBuild.h"

@interface FADBMonitor () <FADBConnectionDelegate>

@property (nonatomic, strong) NSMutableSet *connections;
@property (assign) BOOL dead;
@property (assign) int lastPingTime;

-(void)checkDevices:(NSTimer *)timer;
-(void)connect:(NSString *)deviceName;

@end

@implementation FADBMonitor

@synthesize adb=_adb;
@dynamic devices;
@synthesize delegate=_delegate;

@synthesize connections=_connections;
@synthesize dead=_dead;
@synthesize lastPingTime=_lastPingTime;

-(id)initWithAdb:(FADB *)adb
{
    if ((self = [super init])) {
        _adb = adb;
        self.connections = [[NSMutableSet alloc] init];
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

    for (FADBConnection *connection in self.connections) {
        FADBDevice *device = [connection device];
        // only add devices that have reported a real storage path
        if (device.externalStoragePath) {
            [result addObject:device];
        }
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

    [self checkDevicesWithPing:NO];
}

-(void)checkDevicesWithPing:(BOOL)withPing
{
    [self.adb listDevicesWithBlock:^(NSArray *deviceNames) {
        NSMutableSet *unconnectedDevices = [[NSSet setWithArray:deviceNames] mutableCopy];

        @synchronized (self.connections) {
            for (FADBConnection *connection in self.connections) {
                [unconnectedDevices removeObject:connection.serial];
                if (withPing) {
                    [connection pingWithTimeout:YES];
                }
            }

        }

        for (NSString *name in unconnectedDevices) {
            [self connect:name];
        }
    }];
}

-(void)pingConnectedDevices
{
    @synchronized (self.connections) {
        for (FADBConnection *connection in self.connections) {
            [connection pingWithTimeout:NO];
        }
    }
}

-(BOOL)checkLineForTxnId:(NSString *)line
{
    NSString *lastTxnId = [NSString stringWithFormat:@"{%d}", self.lastPingTime];
    return [line rangeOfString:lastTxnId].location != NSNotFound;
}

-(void)connect:(NSString *)name
{
    FADBConnection *connection = [FADBConnection connectionWithADB:self.adb serial:name];
    [self.connections addObject:connection];
    connection.delegate = self;
    [connection connect];
}

- (void)sendIntent:(FIntent *)intent
{
    for (FADBDevice *device in self.devices) {
        [self.adb sendIntent:intent toDevice:device.serial completion:nil];
    }
}

-(void)refreshConnections
{
    NSMutableArray *connectedSerials = [[NSMutableArray alloc] init];

    for (FADBConnection *connection in self.connections) {
        NSString *serial = connection.device.serial;
        if (serial) {
            [connectedSerials addObject:serial];
        }
    }

    [[FDevices sharedInstance] 
        latestAdbConnections:connectedSerials];
}

#pragma mark ADBConnectionDelegate

- (void)adbConnection:(FADBConnection *)connection pingResponseDevice:(FADBDevice *)device
{
    NSLog(@"got a ping response: %@", device);
    NSLog(@"device clientId: %@ our clientId: %@",
            device.clientId, kClientVersionId);
    if (!device.hasLatestClient) {
        NSLog(@"    device is out of date");
        [self.delegate adbMonitor:self outOfDateDeviceDetected:device];
    }
    [self refreshConnections];
}

- (void)adbConnectionDisconnected:(FADBConnection *)connection
{
    [self.connections removeObject:connection];
    [self refreshConnections];
}

-(void)dealloc
{
    NSLog(@"FADBMonitor dealloc");
}

@end
