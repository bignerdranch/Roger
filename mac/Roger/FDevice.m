//
//  FDevice.m
//  Roger
//
//  Created by Bill Phillips on 5/30/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FDevice.h"
#import "FAppDelegate.h"
#import "DeviceSettings.h"

@interface FDevice ()

@property (nonatomic, readwrite, copy) NSString *serial;
@property (nonatomic, readwrite, strong) DeviceSettings *deviceSettings;

@end

@implementation FDevice

@synthesize serial=_serial;
@synthesize hasWifiConnection=_hasWifiConnection;
@synthesize hasAdbConnection=_hasAdbConnection;
@synthesize deviceSettings=_deviceSettings;

-(id)initWithSerial:(NSString *)serial
{
    if ((self = [super init])) {
        self.serial = serial;

        FAppDelegate *appDelegate = [[NSApplication sharedApplication] delegate];
        NSManagedObjectContext *objectContext = appDelegate.managedObjectContext;

        NSFetchRequest *request = [NSFetchRequest fetchRequestWithEntityName:@"DeviceSettings"];
        request.predicate = [NSPredicate predicateWithFormat:@"serial like[cd] %@", serial];
        NSArray *results = [objectContext executeFetchRequest:request error:nil];

        if (results && [results count] > 0) {
            self.deviceSettings = [results objectAtIndex:0];
        } else {
            self.deviceSettings = [NSEntityDescription 
                insertNewObjectForEntityForName:@"DeviceSettings" 
                         inManagedObjectContext:objectContext];
            self.deviceSettings.wasSetByUser = NO;
        }
    }

    return self;
}

-(BOOL)isConnected
{
    return self.hasWifiConnection || self.hasAdbConnection;
}

-(NSString *)description
{
    return [NSString stringWithFormat:@"<Device serial:%@ hasWifiConnection:%d hasAdbConnection:%d>", 
           self.serial, self.hasWifiConnection, self.hasAdbConnection];
}

@end
