//
//  FNodeServer.m
//  Roger
//
//  Created by Bill Phillips on 5/16/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FNodeServer.h"
#import "FTaskStream.h"
#import "FIntent.h"

@interface FNodeServer ()

@property (retain) NSPipe *taskInput;
@property (copy) NSString *ipAddress;
@property (copy) NSString *fileServerPath;

- (NSString *)serverPath;
- (NSString *)currentMulticastAddress;
- (void)showNodeErrorDialog;

@end

static NSString* const serverUrl = @"http://localhost:8081/sendIntent";

@implementation FNodeServer

@synthesize remoteDeviceSerialList=_remoteDeviceSerialList;
@synthesize taskInput=_taskInput;
@synthesize ipAddress=_ipAddress;
@synthesize fileServerPath=_fileServerPath;

-(id)initWithIpAddress:(NSString *)ipAddress fileServerPath:(NSString *)fileServerPath
{
    if ((self = [super init])) {
        self.ipAddress = ipAddress;
        self.fileServerPath = fileServerPath;
        unableToStart = NO;

        if (![self startServer]) {
            self = nil;
        }
    }

    return self;
}

- (NSString *)serverPath
{
    NSBundle *bundle = [NSBundle mainBundle];
    return [bundle pathForResource:@"RogerServer" ofType:@"js"];
}

- (NSString *)currentMulticastAddress
{
    return @"234.5.6.7";
}

- (BOOL)startServer
{
    if (unableToStart) {
        NSLog(@"Not restarting server. Unable to start");
        return NO;
    }
    
    if (!self.ipAddress) {
        NSLog(@"unable to start server - no wifi ip address");
        return NO;
    }
    if (!self.fileServerPath) {
        NSLog(@"unable to start server - no file server path");
        return NO;
    }

    NSTask *nodeTask = [[NSTask alloc] init];
    NSMutableArray *args = [NSMutableArray array];
 
    NSString *path = [self serverPath];
    NSLog(@"Server path: %@", path);
    
    [args addObject:path];
    [args addObject:self.ipAddress];
    [args addObject:[self currentMulticastAddress]];
    [args addObject:self.fileServerPath];
    [nodeTask setLaunchPath:[[NSUserDefaults standardUserDefaults] stringForKey:@"NodeDirKey"]]; //@"/usr/local/bin/node"];
    [nodeTask setArguments:args];
    self.taskInput = [NSPipe pipe];
    [nodeTask setStandardInput:self.taskInput];
    
    __block NSMutableArray *workingRemoteDeviceSerialList = nil;

    FTaskStream *taskStream = [[FTaskStream alloc] initWithUnlaunchedTask:nodeTask];
    [taskStream addLogEventsWithPrefix:@"NODE"];
    
    [taskStream addOutputEvent:@"end current client list." withBlock:^(NSString *line) {
        if (line && workingRemoteDeviceSerialList) {
            _remoteDeviceSerialList = workingRemoteDeviceSerialList;
            NSLog(@"new remoteDeviceSerialList: %@", self.remoteDeviceSerialList);
            workingRemoteDeviceSerialList = nil;
        }
    }];

    [taskStream addOutputEvent:@"NODE ERROR" withBlock:^(NSString *line) {
        unableToStart = YES;
        [self showNodeErrorDialog];
    }];
    
    [taskStream addOutputEvent:@"." withBlock:^(NSString *line) {
        if (line && workingRemoteDeviceSerialList) {
            [workingRemoteDeviceSerialList addObject:line];
        } else if (!line) {
            [self startServer];
        }
    }];

    [taskStream addOutputEvent:@"begin current client list:" withBlock:^(NSString *line) {
        if (line) {
            workingRemoteDeviceSerialList = [[NSMutableArray alloc] init];
        }
    }];

    [nodeTask launch];

    return YES;
}

- (void)showNodeErrorDialog
{
    NSAlert *alert = [NSAlert alertWithMessageText:@"Unable to start the Node.js server" 
                                     defaultButton:@"OK" 
                                   alternateButton:nil 
                                       otherButton:nil 
                         informativeTextWithFormat:@"Do you have the correct path to the server in the preferences?"];
    [alert runModal];
}

- (void)sendIntent:(FIntent *)intent
{
    NSString *reqUrl = serverUrl;
    NSLog(@"Attempting to send intent: %@", reqUrl);
    
    NSMutableURLRequest *req = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:reqUrl]]; 
    [req setHTTPMethod:@"POST"];
    [req setHTTPBody:[intent json]];
    [NSURLConnection sendAsynchronousRequest:req 
                                       queue:[NSOperationQueue mainQueue] 
                           completionHandler:^(NSURLResponse *response, NSData *data, NSError *error) {
        if (error) {
            NSLog(@"Unable to send to server: %@", error);
        }
    }];
}

- (NSString *)urlPathForFile:(NSString *)fileName
{
    return [NSString stringWithFormat:@"http://%@:8081/file/%@", self.ipAddress, fileName];
}

@end
