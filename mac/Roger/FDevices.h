//
//  FDevices.h
//  Roger
//
//  Created by Bill Phillips on 5/30/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "FDevice.h"

@interface FDevices : NSObject

@property (nonatomic, strong) NSMutableArray *devices;

+ (FDevices *)sharedInstance;

// An array of NSString serial numbers.
-(void)latestWifiConnections:(NSArray *)wifiConnections;
// An array of NSString serial numbers.
-(void)latestAdbConnections:(NSArray *)adbConnections;

@end
