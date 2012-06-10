//
//  DeviceSettings.h
//  Roger
//
//  Created by Bill Phillips on 6/2/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreData/CoreData.h>


@interface DeviceSettings : NSManagedObject

@property (nonatomic, retain) NSNumber * managed;
@property (nonatomic, retain) NSString * serial;

@end
