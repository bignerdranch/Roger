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

// Recommended interface. Keeps only on TaskStream for each task.
+(FTaskStream *)taskStreamForUnlaunchedTask:(NSTask *)task;
// Same as above, but will not create if it doesn't exist.
+(FTaskStream *)taskStreamForLaunchedTask:(NSTask *)task;

// Raw interface. Will break for multiple streams on one task.
-(id)initWithUnlaunchedTask:(NSTask *)task;

-(void)addOutputEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block;
-(void)removeOutputEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block;
-(void)addErrorEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block;
-(void)removeErrorEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block;
-(void)addLogEventsWithPrefix:(NSString *)logPrefix;
-(void)addLogEventsWithPrefix:(NSString *)logPrefix isOutput:(BOOL)isOutput;

@end
