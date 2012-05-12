//
//  FTaskStream.h
//  Roger
//
//  Created by Bill Phillips on 5/12/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef void (^FTaskEvent)(NSString *matchingLine);

@interface FTaskStream : NSObject

-(id)initWithUnlaunchedTask:(NSTask *)task;

-(void)addOutputEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block;
-(void)removeOutputEvent:(NSString *)regexEvent;
-(void)addErrorEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block;
-(void)removeErrorEvent:(NSString *)regexEvent;

@end
