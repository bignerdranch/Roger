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

    NSDictionary *appDefaults = [NSDictionary dictionaryWithObjects:[NSArray arrayWithObjects:@"", @"/usr/local/bin/node", nil]
                                                            forKeys:[NSArray arrayWithObjects:@"SdkDirKey", @"NodeDirKey", nil]];

    [[NSUserDefaults standardUserDefaults] registerDefaults:appDefaults];
    
    [controller startServer];
}

- (void)applicationWillTerminate:(NSNotification *)notification
{
    NSLog(@"stopping server");
    [controller stopServer];
    controller = nil;
}

- (IBAction)showPreferencePanel:(id)sender
{
    if (!pref) {
        pref = [[FPreferenceWindowController alloc] initWithWindowNibName:@"FPreferenceWindowController"];
    }

    [pref showWindow:self];
}

- (void)restartServer
{
    [controller restartServer];
}

@end
