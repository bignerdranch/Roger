//
//  FADB.m
//  Roger
//
//  Created by Bill Phillips on 5/13/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FADB.h"
#import "FIntent.h"
#import "FTaskStream.h"

@implementation FADB

@synthesize adbPath=_adbPath;

- (NSTask *)adbTaskWithArgs:(NSArray *)args
{
    if (![self adbPath]) return nil;

    NSMutableDictionary *env = [NSMutableDictionary dictionaryWithObjectsAndKeys:
        nil];

    NSTask *task = [[NSTask alloc] init];
    [task setLaunchPath:[self adbPath]];
    [task setCurrentDirectoryPath:NSHomeDirectory()];
    [task setEnvironment:env];
    [task setArguments:args];
    //[task setStandardInput:[NSPipe pipe]];

    return task;
}

- (NSTask *)runAdbTaskWithArgs:(NSArray *)args 
                     logPrefix:(NSString *)logPrefix
                    completion:(void (^)(void))completion
{
    if (![self adbPath]) return nil;

    completion = [completion copy];
    NSTask *task = [self adbTaskWithArgs:args];

    FTaskStream *taskStream = [FTaskStream taskStreamForUnlaunchedTask:task];

    if (logPrefix) {
        [taskStream addLogEventsWithPrefix:logPrefix];
    }

    if (completion) {
        [taskStream addOutputEvent:@"." withBlock:^(NSString *line) {
            if (!line) {
                completion();
            }
        }];
    }

    [task launch];
    return task;
}

- (void)copyLocalPath:(NSString *)localPath 
         toDevicePath:(NSString *)devicePath
               device:(NSString *)serial
           completion:(void (^)(void))completion
{
    NSArray *args = [NSArray arrayWithObjects:
        @"-s", serial, @"push", localPath, devicePath, nil];

    NSString *logPrefix = [NSString stringWithFormat:@"ADB copy %@", serial];
    [self runAdbTaskWithArgs:args logPrefix:logPrefix completion:completion];
}

- (void)sendIntent:(FIntent *)intent toDevice:(NSString *)serial completion:(void(^)(void))completion
{
    NSMutableArray *args = [[NSMutableArray alloc] init];

    [args addObjectsFromArray:[NSArray arrayWithObjects:
        @"-s", serial, @"shell", @"am", intent.type, nil]];
    if (intent.action) {
        [args addObjectsFromArray:[NSArray arrayWithObjects:@"-a", intent.action, nil]];
    }

    NSDictionary *extras = [intent extras];
    for (NSString *key in [extras keyEnumerator]) {
        NSObject *value = [extras objectForKey:key];
        if ([value isKindOfClass:[NSString class]]) { 
            [args addObjectsFromArray:[NSArray arrayWithObjects:@"--es", key, value, nil]];
        } else if ([value isKindOfClass:[NSNumber class]]) {
            NSNumber *number = (NSNumber *)value;
            BOOL isBool = NO;
            int intValue;

            // technique ripped off from CJSONSerializer
            switch (CFNumberGetType((__bridge CFNumberRef)number)) {
                case kCFNumberCharType:
                    intValue = [number intValue];
                    if (intValue == 0 || intValue == 1) {
                        isBool = YES;
                    }
                    break;
                case kCFNumberFloat32Type:
                case kCFNumberFloat64Type:
                case kCFNumberFloatType:
                case kCFNumberDoubleType:
                case kCFNumberSInt8Type:
                case kCFNumberSInt16Type:
                case kCFNumberSInt32Type:
                case kCFNumberSInt64Type:
                case kCFNumberShortType:
                case kCFNumberIntType:
                case kCFNumberLongType:
                case kCFNumberLongLongType:
                case kCFNumberCFIndexType:
                default:
                    intValue = [number intValue];
                    break;
            }

            if (isBool) {
                NSString *boolString = intValue ? @"true" : @"false";
                [args addObjectsFromArray:[NSArray arrayWithObjects:
                    @"--ez", key, boolString, nil]];
            } else {
                [args addObjectsFromArray:[NSArray arrayWithObjects:
                    @"--ei", key, [NSString stringWithFormat:@"%d", intValue], nil]];
            }
        } else {
            NSLog(@"unsupported extra type: %@", value);
        }
    }

    NSLog(@"send intent args: %@", args);
    NSTask *task = [self adbTaskWithArgs:args];

    FTaskStream *taskStream = [FTaskStream taskStreamForUnlaunchedTask:task];
    [taskStream addLogEventsWithPrefix:@"ADB sendIntent" isOutput:NO];
    if (completion) {
        [taskStream addOutputEvent:@"." withBlock:^(NSString *line) {
            if (!line) {
                completion();
            }
        }];
    }

    NSLog(@"sending intent to device %@", serial);
    [task launch];
}

- (void)listDevicesWithBlock:(void (^)(NSArray *devices))block
{
    block = [block copy];

    NSMutableArray *results = [[NSMutableArray alloc] init];
    NSTask *task = [self adbTaskWithArgs:[NSArray arrayWithObjects:@"devices", nil]];

    FTaskStream *taskStream = [FTaskStream taskStreamForUnlaunchedTask:task];
    [taskStream addLogEventsWithPrefix:@"ADB listDevices" isOutput:NO];
    [taskStream addOutputEvent:@"device$" withBlock:^(NSString *line) {
        if (line) {
            NSArray *components = [line componentsSeparatedByCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
            if ([components count] > 0) {
                NSString *serial = [components objectAtIndex:0];
                [results addObject:serial];
            }
        } else {
            block(results);
        }
    }];

    [task launch];
}

-(void)dealloc
{
    NSLog(@"FADB dealloc");
}
@end
