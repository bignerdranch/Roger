//
//  FPreferenceWindowController.m
//  Roger
//
//  Created by Chris Stewart on 4/19/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FPreferenceWindowController.h"
#import "FAppDelegate.h"

@interface FPreferenceWindowController ()
- (void)restartServer;
- (void)updateNodePath:(NSString *)path;
@end

@implementation FPreferenceWindowController
@synthesize sdkPath, sdkPathField, nodePath, nodePathField;

static NSString * const sdkKey = @"SdkDirKey";
static NSString * const nodeKey = @"NodeDirKey";

- (id)initWithWindow:(NSWindow *)window
{
    self = [super initWithWindow:window];
    if (self) {
        // Initialization code here.
    }
    
    return self;
}

- (void)windowDidLoad
{
    [super windowDidLoad];
    
    [self setSdkPath:[[NSUserDefaults standardUserDefaults] stringForKey:sdkKey]];
    [self setNodePath:[[NSUserDefaults standardUserDefaults] stringForKey:nodeKey]];
    [self updatePath];
}

- (void)updatePath
{
    [sdkPathField setTitleWithMnemonic:sdkPath];
    [nodePathField setTitleWithMnemonic:nodePath];
}

- (IBAction)selectSdkClicked:(id)sender
{
    NSOpenPanel *openPanel = [NSOpenPanel openPanel];
    
    [openPanel setCanChooseDirectories:YES];
    [openPanel setCanChooseFiles:NO];
    
    [openPanel beginWithCompletionHandler:^(NSInteger result) {
        if (result != NSOKButton ) return;
        
        for (NSURL *url in [openPanel URLs]) {
            NSString *fileName = [url path];
            [[NSUserDefaults standardUserDefaults] setObject:fileName forKey:sdkKey];
            [self setSdkPath:fileName];
            [self updatePath];
        }
    }];
}

- (IBAction)selectNodePathClicked:(id)sender
{
    NSOpenPanel *openPanel = [NSOpenPanel openPanel];
    
    [openPanel setCanChooseDirectories:YES];
    [openPanel setCanChooseFiles:NO];
    
    [openPanel beginWithCompletionHandler:^(NSInteger result) {
        if (result != NSOKButton ) return;
        
        for (NSURL *url in [openPanel URLs]) {
            [self updateNodePath:[url path]];
        }
    }];
}

- (IBAction)resetNodePath:(id)sender
{
    [self updateNodePath:@"/usr/local/bin/node"];
}

- (void)updateNodePath:(NSString *)path
{
    [[NSUserDefaults standardUserDefaults] setObject:path forKey:nodeKey];
    [self setNodePath:path];
    [self updatePath];
    [self restartServer];
}

- (void)restartServer
{
    FAppDelegate *delegate = (FAppDelegate *) [[NSApplication sharedApplication] delegate];
    [delegate restartServer];
}

@end
