//
//  FTaskCorral.h
//  Roger
//
//  A singleton stash for ongoing tasks. Free-running NSTasks
//  can retain themselves in this singleton, and then remove
//  themselves on task completion. Used in FTaskStream.m.
//
//  Created by Bill Phillips on 5/16/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface FTaskCorral : NSObject

+ (FTaskCorral *)sharedInstance;

-(void)addTask:(NSTask *)task;
-(void)removeTask:(NSTask *)task;

@end
