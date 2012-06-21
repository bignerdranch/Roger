//
//  DeviceSettings.h
//  Roger
//
//  Created by Bill Phillips on 6/2/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreData/CoreData.h>

typedef enum _DeviceSettingsManaged {
    kDeviceSettingsManaged_Unset,
    kDeviceSettingsManaged_Managed,
    kDeviceSettingsManaged_Unmanaged 
} DeviceSettingsManaged;

@interface DeviceSettings : NSManagedObject

// internal storage
@property (nonatomic, retain) NSNumber * managed;
@property (nonatomic, retain) NSString * serial;

// public interface
@property (nonatomic, assign) BOOL wasSetByUser;
@property (nonatomic, assign) BOOL managedByUser;

@end
