//
//  FADBDevice.h
//  Roger
//
//  Created by Bill Phillips on 5/13/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface FADBDevice : NSObject

@property (readonly) NSString *serial;
@property (readonly) NSString *externalStoragePath;

-(id)initWithSerial:(NSString *)serial storagePath:(NSString *)storagePath;

@end
