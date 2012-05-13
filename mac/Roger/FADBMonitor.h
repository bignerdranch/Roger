//
//  FADBMonitor.h
//  Roger
//
//  Created by Bill Phillips on 5/13/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@class FADB;

@interface FADBMonitor : NSObject

@property (readonly, strong) FADB *adb;
@property (readonly, strong) NSArray *devices;

-(id)initWithAdb:(FADB *)adb;

-(void)kill;

@end
