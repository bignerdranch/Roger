//
//  FTaskCorral.m
//  Roger
//
//  Created by Bill Phillips on 5/16/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FTaskCorral.h"

@interface FTaskCorral ()

@property (strong) NSMutableSet *tasks;

@end

@implementation FTaskCorral

@synthesize tasks=_tasks;

+ (FTaskCorral *)sharedInstance
{
    static dispatch_once_t once;
    static id sharedInstance;
    dispatch_once(&once, ^{
        sharedInstance = [[FTaskCorral alloc] init];
    });
    
    return sharedInstance;
}

-(id)init
{
    if ((self = [super init])) {
        self.tasks = [[NSMutableSet alloc] init];
    }

    return self;
}

-(void)addTask:(NSTask *)task
{
    [self.tasks addObject:task];
}

-(void)removeTask:(NSTask *)task
{
    [self.tasks removeObject:task];
}

@end
