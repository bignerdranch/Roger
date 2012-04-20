//
//  FPreferenceWindowController.m
//  Roger
//
//  Created by Chris Stewart on 4/19/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FPreferenceWindowController.h"

@interface FPreferenceWindowController ()

@end

@implementation FPreferenceWindowController
@synthesize sdkPath, sdkPathField;

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
    
    [self setSdkPath:[[NSUserDefaults standardUserDefaults] stringForKey:@"SdkDirKey"]];
    [self updatePath];
}

- (void)updatePath
{
    [sdkPathField setTitleWithMnemonic:sdkPath];
}

- (IBAction)selectSdkClicked:(id)sender
{
    NSLog(@"selectSdkClicked");
    NSOpenPanel *openPanel = [NSOpenPanel openPanel];
    
    [openPanel setCanChooseDirectories:YES];
    [openPanel setCanChooseFiles:NO];
    
    [openPanel beginWithCompletionHandler:^(NSInteger result) {
        if (result != NSOKButton ) return;
        
        for (NSURL *url in [openPanel URLs]) {
            NSString *fileName = [url path];
            [[NSUserDefaults standardUserDefaults] setObject:fileName forKey:@"SdkDirKey"];
            [self setSdkPath:fileName];
            [self updatePath];
        }
    }];
}


@end
