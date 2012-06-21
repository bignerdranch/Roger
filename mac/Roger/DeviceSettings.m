//
//  DeviceSettings.m
//  Roger
//
//  Created by Bill Phillips on 6/2/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "DeviceSettings.h"

@implementation DeviceSettings

@dynamic managed;
@dynamic serial;

-(BOOL) wasSetByUser
{
    return [self.managed intValue] != kDeviceSettingsManaged_Unset;
}

-(void)setWasSetByUser:(BOOL)wasSetByUser
{
    BOOL isUnset = [self.managed intValue] == kDeviceSettingsManaged_Unset;
    if (wasSetByUser && isUnset) {
        self.managed = [NSNumber numberWithInt:kDeviceSettingsManaged_Unmanaged];
    } else if (!wasSetByUser && !isUnset) {
        self.managed = [NSNumber numberWithInt:kDeviceSettingsManaged_Unset];
    }
}

-(BOOL)managedByUser
{
    return [self.managed intValue] == kDeviceSettingsManaged_Managed;
}

-(void)setManagedByUser:(BOOL)managedByUser
{
    if (managedByUser) {
        self.managed = [NSNumber numberWithInt:kDeviceSettingsManaged_Managed];
    } else {
        self.managed = [NSNumber numberWithInt:kDeviceSettingsManaged_Unmanaged];
    }
}

@end
