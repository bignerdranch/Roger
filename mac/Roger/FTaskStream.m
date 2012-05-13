//
//  FTaskStream.m
//  Roger
//
//  Created by Bill Phillips on 5/12/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <objc/runtime.h>
#import "FTaskStream.h"
#import "NSString+Regexen.h"

@interface FTaskStream ()

@property (atomic, strong) NSMutableArray *outputEvents;
@property (atomic, strong) NSMutableArray *errorEvents;
@property (atomic, strong) NSFileHandle *outputHandle;
@property (atomic, strong) NSFileHandle *errorHandle;
@property (atomic, strong) NSMutableData *outputData;
@property (atomic, strong) NSMutableData *errorData;

@property (nonatomic, strong) NSTask *task;

-(void)readComplete:(NSNotification *)notification;
-(BOOL)processData:(NSData *)data withEventMap:(NSMutableArray *)eventMap dataStash:(NSMutableData *)dataStash;
-(NSArray *)linesFromData:(NSData *)available withDataStash:(NSMutableData *)data;

@end

@implementation FTaskStream

@synthesize outputEvents;
@synthesize errorEvents;
@synthesize outputHandle;
@synthesize errorHandle;
@synthesize outputData;
@synthesize errorData;

@synthesize task=_task;

+(FTaskStream *)taskStreamForLaunchedTask:(NSTask *)task
{
    FTaskStream *taskStream = (FTaskStream *)objc_getAssociatedObject(task, @"FTaskStream_TaskAssociation");

    return taskStream;
}

+(FTaskStream *)taskStreamForUnlaunchedTask:(NSTask *)task
{
    FTaskStream *taskStream = (FTaskStream *)objc_getAssociatedObject(task, @"FTaskStream_TaskAssociation");

    if (!taskStream) {
        taskStream = [[FTaskStream alloc] initWithUnlaunchedTask:task];
    }

    return taskStream;
}

-(id)initWithUnlaunchedTask:(NSTask *)task
{
    // can't launch without a task
    if (task == nil) return nil;

    if ((self = [super init])) {
        self.task = task;

        // associate ourselves with the task; means only one instance
        // per nstask
        objc_setAssociatedObject(task, @"FTaskStream_TaskAssociation", self, OBJC_ASSOCIATION_RETAIN);

        self.outputEvents = [[NSMutableArray alloc] init];
        self.errorEvents = [[NSMutableArray alloc] init];

        NSPipe *standardOutputPipe = [NSPipe pipe];
        NSPipe *standardErrorPipe = [NSPipe pipe];
        if (!(standardOutputPipe && standardErrorPipe)) {
            NSLog(@"error, a pipe is nil standardOutput: %@ standardError: %@", standardOutputPipe, standardErrorPipe);
            return nil;
        }

        task.standardOutput = standardOutputPipe;
        task.standardError = standardErrorPipe;

        self.outputHandle = [standardOutputPipe fileHandleForReading];
        self.errorHandle = [standardErrorPipe fileHandleForReading];

        if (!(self.outputHandle && self.errorHandle)) {
            NSLog(@"error, a handle is nil standardOutput: %@ standardError: %@", self.outputHandle, self.errorHandle);
            return nil;
        }

        self.outputData = [[NSMutableData alloc] init];
        self.errorData = [[NSMutableData alloc] init];

        [[NSNotificationCenter defaultCenter] 
            addObserver:self 
               selector:@selector(readComplete:)
                   name:NSFileHandleReadCompletionNotification
                 object:self.outputHandle];

        [[NSNotificationCenter defaultCenter] 
            addObserver:self 
               selector:@selector(readComplete:)
                   name:NSFileHandleReadCompletionNotification
                 object:self.errorHandle];

        [self.outputHandle readInBackgroundAndNotify];
        [self.errorHandle readInBackgroundAndNotify];
    }

    return self;
}

-(void)addOutputEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block
{
    block = [block copy];
    NSArray *event = [NSArray arrayWithObjects:regexEvent, block, nil];
    [self.outputEvents addObject:event];
}

-(void)removeOutputEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block
{
    block = [block copy];
    NSArray *event = [NSArray arrayWithObjects:regexEvent, block, nil];
    [self.outputEvents removeObject:event];
}

-(void)addErrorEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block
{
    block = [block copy];
    NSArray *event = [NSArray arrayWithObjects:regexEvent, block, nil];
    [self.errorEvents addObject:event];
}

-(void)removeErrorEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block
{
    block = [block copy];
    NSArray *event = [NSArray arrayWithObjects:regexEvent, block, nil];
    [self.errorEvents removeObject:event];
}

-(void)addLogEventsWithPrefix:(NSString *)logPrefix isOutput:(BOOL)isOutput
{
    if (isOutput) {
        [self addOutputEvent:@"." withBlock:^(NSString *line) {
            if (line) {
                NSLog(@"%@: %@", logPrefix, line);
            }
        }];
    } else {
        [self addErrorEvent:@"." withBlock:^(NSString *line) {
            if (line) {
                NSLog(@"%@ ERROR: %@", logPrefix, line);
            }
        }];
    }
}

-(void)addLogEventsWithPrefix:(NSString *)logPrefix
{
    [self addLogEventsWithPrefix:logPrefix isOutput:YES];
    [self addLogEventsWithPrefix:logPrefix isOutput:NO];
}

-(void)readComplete:(NSNotification *)notification
{
    NSData *data = [notification.userInfo objectForKey:NSFileHandleNotificationDataItem];

    NSMutableArray *eventMap;
    NSMutableData *dataStash;
    NSFileHandle *handle = notification.object;

    if (handle == self.outputHandle) {
        eventMap = self.outputEvents;
        dataStash = self.outputData;
    } else if (handle == self.errorHandle) {
        eventMap = self.errorEvents;
        dataStash = self.errorData;
    } else {
        return;
    }

    if ([self processData:data withEventMap:eventMap dataStash:dataStash]) {
        [handle readInBackgroundAndNotify];
    } else {
        [[NSNotificationCenter defaultCenter] 
            removeObserver:self 
                      name:NSFileHandleReadCompletionNotification
                    object:handle];
        // signal that a handle is done by nilling it
        if (handle == self.outputHandle) {
            self.outputHandle = nil;
        }
        if (handle == self.errorHandle) {
            self.errorHandle = nil;
        }

        // if both handles are done, release the task
        if (!self.errorHandle && !self.outputHandle) {
            self.task = nil;
        }
    }
}

-(BOOL)processData:(NSData *)data withEventMap:(NSMutableArray *)eventMap dataStash:(NSMutableData *)dataStash
{
    NSArray *lines = [self linesFromData:data withDataStash:dataStash];
    if (lines) {
        for (NSString *line in lines) {
            for (NSArray *event in eventMap ) {
                NSString *pattern = [event objectAtIndex:0];

                if ([line stringsFromFirstMatchOfPattern:pattern]) {
                    FTaskEvent eventBlock = [event objectAtIndex:1];
                    eventBlock(line);
                }
            }
        }
        return YES;
    } else { 
        for (NSArray *event in eventMap ) {
            FTaskEvent eventBlock = [event objectAtIndex:1];
            eventBlock(nil);
        }
        return NO;
    }

}

-(NSArray *)linesFromData:(NSData *)available withDataStash:(NSMutableData *)data
{
    NSMutableArray *results = [[NSMutableArray alloc] init];

    if (![available length]) {
        return nil;
    }

    [data appendData:available];

    NSString *latestData = [[NSString alloc] initWithData:data encoding:NSASCIIStringEncoding];
    NSArray *lines = [latestData componentsSeparatedByString:@"\n"];
    int dataIndex = 0;
    NSString *lastLine;

    for (NSString *line in lines) {
        if (lastLine) {
            [results addObject:lastLine];
            dataIndex += [lastLine length] + 1;
        }
        lastLine = line;
    }

    if (dataIndex) {
        [data replaceBytesInRange:NSMakeRange(0, dataIndex)
                        withBytes:NULL
                           length:0];
    }

    return results;
}

@end
