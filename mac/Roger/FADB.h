//
//  FADB.h
//  Roger
//
//  Created by Bill Phillips on 5/13/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@class FIntent;

@interface FADB : NSObject

@property (nonatomic, copy) NSString *adbPath;

- (NSTask *)adbTaskWithArgs:(NSArray *)arguments;
- (NSTask *)runAdbTaskWithArgs:(NSArray *)arguments 
                     logPrefix:(NSString *)logPrefix
                    completion:(void (^)(void))completion;
- (void)copyLocalPath:(NSString *)localPath 
         toDevicePath:(NSString *)devicePath
               device:(NSString *)serial
           completion:(void (^)(void))completion;
- (void)sendIntent:(FIntent *)intent toDevice:(NSString *)serial completion:(void(^)(void))completion;
- (void)listDevicesWithBlock:(void (^)(NSArray *devices))block;

@end
