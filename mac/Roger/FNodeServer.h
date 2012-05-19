//
//  FNodeServer.h
//  Roger
//
//  Eventually, this should encapsulate all long-running management of the node server.
//  For now, that basically means "restart it if we detect that it is dead."
//
//  Created by Bill Phillips on 5/16/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@class FIntent;

@interface FNodeServer : NSObject

-(id)initWithIpAddress:(NSString *)ipAddress fileServerPath:(NSString *)fileServerPath;
- (void)sendIntent:(FIntent *)intent;
- (NSString *)urlPathForFile:(NSString *)fileName;

@end
