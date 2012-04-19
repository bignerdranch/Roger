//
//  FAppDelegate.m
//  Roger
//
//  Created by Chris Stewart on 4/18/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FAppDelegate.h"
#import "FFileViewController.h"

@implementation FAppDelegate

@synthesize window = _window;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
    controller = [[FFileViewController alloc] initWithNibName:@"FFileViewController" bundle:nil];
    [_window setContentView:[controller view]];

    NSDictionary *appDefaults = [NSDictionary
        dictionaryWithObject:@"" forKey:@"SdkDirKey"];

    [[NSUserDefaults standardUserDefaults] registerDefaults:appDefaults];
    
    [controller startServer];
}

- (void)applicationWillTerminate:(NSNotification *)notification
{
    NSLog(@"stopping server");
    [controller stopServer];
    controller = nil;
}

@end
