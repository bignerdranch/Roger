//
//  FADBConnection.h
//  Roger
//
//  Created by Bill Phillips on 6/3/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@class FADB;
@class FADBDevice;
@protocol FADBConnectionDelegate;

@interface FADBConnection : NSObject

+ (FADBConnection *)connectionWithADB:(FADB *)adb serial:(NSString *)serial;

- (void)connect;
- (void)ping;

@property (nonatomic, weak) id<FADBConnectionDelegate> delegate;
@property (readonly) NSString *serial;
@property (readonly) FADB *adb;
@property (readonly) FADBDevice *device;
@property (nonatomic, copy) NSString *clientId;
@property (nonatomic, copy) NSString *storagePath;

@end

@protocol FADBConnectionDelegate <NSObject>

- (void)adbConnection:(FADBConnection *)connection pingResponseDevice:(FADBDevice *)device;
- (void)adbConnectionDisconnected:(FADBConnection *)connection;

@end

