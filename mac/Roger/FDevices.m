//
//  FDevices.m
//  Roger
//
//  Created by Bill Phillips on 5/30/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FDevices.h"

@interface FDevices ()

@property (nonatomic, strong) NSMutableArray *mutableDevices;

-(void)checkForDisconnectedDevices;
-(void)addNewDevicesFromSerials:(NSSet *)allSerials;

@end

@implementation FDevices

@synthesize devices=_devices;
@synthesize mutableDevices=_mutableDevices;

+ (FDevices *)sharedInstance
{
    static dispatch_once_t once;
    static FDevices *sharedInstance;
    dispatch_once(&once, ^{
        sharedInstance = [[FDevices alloc] init];
        sharedInstance.mutableDevices = [[NSMutableArray alloc] init];
        sharedInstance.devices = sharedInstance.mutableDevices;
    });
    
    return sharedInstance;
}

-(void)onMainThread:(dispatch_block_t)block
{
    if ([NSThread isMainThread]) 
        block();
    else
        dispatch_sync(dispatch_get_main_queue(), block);
}

-(void)addNewDevicesFromSerials:(NSSet *)allSerials
{
    NSMutableSet *newSerials = [allSerials mutableCopy];

    for (FDevice *device in self.devices) {
        [newSerials removeObject:device.serial];
    }

    NSMutableArray *newDevices = [[NSMutableArray alloc] init];
    [self onMainThread:^{
        for (NSString *serial in newSerials) {
            FDevice *device = [[FDevice alloc] initWithSerial:serial];
            [newDevices addObject:device];
        }
    }];

    [self.mutableDevices addObjectsFromArray:newDevices];
}

-(void)checkForDisconnectedDevices
{
    NSMutableArray *removeArray = [[NSMutableArray alloc] init];

    for (FDevice *device in self.devices) {
        if (!device.isConnected) {
            [removeArray addObject:device];
        }
    }

    [self.mutableDevices removeObjectsInArray:removeArray];
    self.devices = self.mutableDevices;
    NSLog(@"device connections: %@", self.devices);
}

-(void)latestWifiConnections:(NSArray *)wifiConnections
{
    NSSet *serials = [NSSet setWithArray:wifiConnections];
    [self addNewDevicesFromSerials:serials];

    for (FDevice *device in self.devices) {
        device.hasWifiConnection = [serials containsObject:device.serial];
    }

    [self checkForDisconnectedDevices];
}

-(void)latestAdbConnections:(NSArray *)adbConnections
{
    NSSet *serials = [NSSet setWithArray:adbConnections];
    [self addNewDevicesFromSerials:serials];

    for (FDevice *device in self.devices) {
        device.hasAdbConnection = [serials containsObject:device.serial];
    }

    [self checkForDisconnectedDevices];
}


@end
