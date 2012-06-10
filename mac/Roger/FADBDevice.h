//
//  FADBDevice.h
//  Roger
//
//  Created by Bill Phillips on 5/13/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@class FADBConnection;

@interface FADBDevice : NSObject

@property (readonly) NSString *serial;
@property (readonly) NSString *externalStoragePath;
@property (readonly) NSString *clientId;
@property (nonatomic, weak, readonly) FADBConnection *connection;

-(id)initWithSerial:(NSString *)serial storagePath:(NSString *)storagePath clientId:(NSString *)clientId 
         connection:(FADBConnection *)connection;

@end
