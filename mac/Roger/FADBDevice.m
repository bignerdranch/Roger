//
//  FADBDevice.m
//  Roger
//
//  Created by Bill Phillips on 5/13/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FADBDevice.h"

@implementation FADBDevice

@synthesize serial=_serial;
@synthesize externalStoragePath=_externalStoragePath;

-(id)initWithSerial:(NSString *)serial storagePath:(NSString *)storagePath
{
    if ((self = [super init])) {
        _serial = [serial copy];
        _externalStoragePath = [storagePath copy];
    }
    
    return self;
}

-(NSString *)description
{
    return [NSString stringWithFormat:@"(ADBDevice serial:%@ storage:%@)", self.serial, self.externalStoragePath];
}

@end
