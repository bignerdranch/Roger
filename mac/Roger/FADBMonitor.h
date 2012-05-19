//
//  FADBMonitor.h
//  Roger
//
//  Created by Bill Phillips on 5/13/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@class FADB;
@class FIntent;

@interface FADBMonitor : NSObject

@property (readonly, strong) FADB *adb;
@property (readonly, strong) NSArray *devices;

-(id)initWithAdb:(FADB *)adb;
-(void)checkDevices;
- (void)sendIntent:(FIntent *)intent;

-(void)kill;

@end
