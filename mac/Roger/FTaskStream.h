//
//  FTaskStream.h
//  Roger
//
//  FTaskStream is an event-based block programming interface for NSTask. 
//  FTaskStream plugs in to its task's stdin and stdout. When a line is read
//  for an input, it attempts to match each event's regex against the line.
//  For each matching event, the associated block is run. If multiple
//  events match, all matching events are run in the order they were added
//  to the FTaskStream instance.
//
//  Finally, on EOF all blocks are run with a "nil" value for line.
//
//  FTaskStream also manages the lifetime of its associated NSTask, so it is
//  safe to let your task run to completion without without retaining it.
//
//  An example: Say you had a utility that you were running whose output you 
//  usually wanted to ignore, but for some reason marked error lines on stdout
//  like so:
//  
//  ---
//  Boring logging
//  More boring logging
//  BEGIN ERROR
//  It's probably an error that this utility marks errors in such a bizarre
//  and hard to parse manner.
//  END ERROR
//  Boring, boring log information
//  ---
//
//  And you wanted to only output the following:
//  
//  ---
//  It's probably an error that this utility marks errors in such a bizarre
//  and hard to parse manner.
//  ---
//
//  You'd do something like this:
//
//  ---
//  NSTask *task = [[NSTask alloc] init];
//  /* setup arguments, launch path, etc. for task */
//
//  // create a task stream
//  FTaskStream *taskStream = [FTaskStream taskStreamForUnlaunchedTask:task];
//
//  __block BOOL isError = NO;
//  [taskStream addOutputEvent:@"END ERROR" withBlock:^(NSString *line) {
//      if (line) { 
//          isError = NO; 
//      }
//  }];
//  [taskStream addOutputEvent:@"END ERROR" withBlock:^(NSString *line) {
//      if (line && isError) { 
//          NSLog(@"Error line: %@", line); 
//      }
//  }];
//  [taskStream addOutputEvent:@"BEGIN ERROR" withBlock:^(NSString *line) {
//      if (line) { 
//          isError = YES; 
//      }
//  }];
//
//  [task launch];
//  ---
//
//  
//
//  Created by Bill Phillips on 5/12/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef void (^FTaskEvent)(NSString *matchingLine);

@interface FTaskStream : NSObject

// Recommended interface. Keeps only one TaskStream for each task.
+(FTaskStream *)taskStreamForUnlaunchedTask:(NSTask *)task;
// Same as above, but will not create if it doesn't exist.
+(FTaskStream *)taskStreamForLaunchedTask:(NSTask *)task;

// Raw interface. Will break for multiple streams on one task.
-(id)initWithUnlaunchedTask:(NSTask *)task;

// Adds a regex matching output event to stdout. Every time
// regexEvent is matched in a line in stdout, block is executed.
// block will also executed with "nil" on EOF.
-(void)addOutputEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block;
// remove an event from stdout.
-(void)removeOutputEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block;

// Same as addOutputEvent, but on stderr instead of stdin.
-(void)addErrorEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block;
// Same as removeOutputEvent, but on stderr instead of stdin.
-(void)removeErrorEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block;

// Convenience methods for common usages of addOutputEvent and addErrorEvent.

// Adds a standard set of log events. If isOutput, equivalent to:
//
// [obj addOutputEvent:@"." withBlock:^(NSString *line) { 
//     if (line) { 
//         NSLog(@"%@: %@", logPrefix, line);
//     }
// }];
// 
// Else, equivalent to:
//
// [obj addErrorEvent:@"." withBlock:^(NSString *line) { 
//     if (line) { 
//         NSLog(@"%@ ERROR: %@", logPrefix, line);
//     }
// }];
-(void)addLogEventsWithPrefix:(NSString *)logPrefix isOutput:(BOOL)isOutput;

// This method is equivalent to:
//
// [obj addLogEventsWithPrefix:logPrefix isOutput:YES];
// [obj addLogEventsWithPrefix:logPrefix isOutput:NO];
//
-(void)addLogEventsWithPrefix:(NSString *)logPrefix;

@end
