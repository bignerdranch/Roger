//
//  FTaskCorral.h
//  Roger
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
