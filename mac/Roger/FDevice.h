//
//  FDevice.h
//  Roger
//
//  Created by Bill Phillips on 5/30/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@class DeviceSettings;

@interface FDevice : NSObject

-(id)initWithSerial:(NSString *)serial;

@property (nonatomic, readonly, copy) NSString *serial;
@property (nonatomic, assign) BOOL hasWifiConnection;
@property (nonatomic, assign) BOOL hasAdbConnection;
@property (nonatomic, readonly) BOOL isConnected;
@property (nonatomic, readonly, strong) DeviceSettings *deviceSettings;

@end
