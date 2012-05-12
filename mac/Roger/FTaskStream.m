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

@property (atomic, strong) NSMutableDictionary *outputEvents;
@property (atomic, strong) NSMutableDictionary *errorEvents;
@property (atomic, strong) NSFileHandle *outputHandle;
@property (atomic, strong) NSFileHandle *errorHandle;
@property (atomic, strong) NSMutableData *outputData;
@property (atomic, strong) NSMutableData *errorData;

@property (nonatomic, strong) NSTask *task;

-(void)readComplete:(NSNotification *)notification;
-(BOOL)processData:(NSData *)data withEventMap:(NSMutableDictionary *)eventMap dataStash:(NSMutableData *)dataStash;
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

-(id)initWithUnlaunchedTask:(NSTask *)task
{
    if ((self = [super init])) {
        self.task = task;

        // associate ourselves with the task; means only one instance
        // per nstask
        objc_setAssociatedObject(task, @"FTaskStream_TaskAssociation", self, OBJC_ASSOCIATION_RETAIN);

        self.outputEvents = [[NSMutableDictionary alloc] init];
        self.errorEvents = [[NSMutableDictionary alloc] init];

        NSPipe *standardOutputPipe = [NSPipe pipe];
        NSPipe *standardErrorPipe = [NSPipe pipe];

        task.standardOutput = standardOutputPipe;
        task.standardError = standardErrorPipe;

        self.outputHandle = [standardOutputPipe fileHandleForReading];
        self.errorHandle = [standardErrorPipe fileHandleForReading];

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
    [self.outputEvents setObject:[block copy] forKey:regexEvent];
}

-(void)removeOutputEvent:(NSString *)regexEvent
{
    [self.outputEvents removeObjectForKey:regexEvent];
}

-(void)addErrorEvent:(NSString *)regexEvent withBlock:(FTaskEvent)block
{
    [self.errorEvents setObject:[block copy] forKey:regexEvent];
}

-(void)removeErrorEvent:(NSString *)regexEvent
{
    [self.errorEvents removeObjectForKey:regexEvent];
}

-(void)readComplete:(NSNotification *)notification
{
    NSData *data = [notification.userInfo objectForKey:NSFileHandleNotificationDataItem];

    NSMutableDictionary *eventMap;
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
    }
}

-(BOOL)processData:(NSData *)data withEventMap:(NSMutableDictionary *)eventMap dataStash:(NSMutableData *)dataStash
{
    NSArray *lines = [self linesFromData:data withDataStash:dataStash];
    if (lines) {
        for (NSString *line in lines) {
            for (NSString *pattern in [eventMap keyEnumerator]) {
                FTaskEvent eventBlock = [eventMap objectForKey:pattern];

                if ([line stringsFromFirstMatchOfPattern:pattern]) {
                    eventBlock(line);
                }
            }
        }
        return YES;
    } else { 
        for (FTaskEvent eventBlock in [eventMap objectEnumerator]) {
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
