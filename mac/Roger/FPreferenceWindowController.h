//
//  FPreferenceWindowController.h
//  Roger
//
//  Created by Chris Stewart on 4/19/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Cocoa/Cocoa.h>

@interface FPreferenceWindowController : NSWindowController {
    NSString *sdkPath;
}

@property (nonatomic, strong) IBOutlet NSTextField *sdkPathField;
@property (nonatomic, strong) IBOutlet NSTextField *nodePathField;
@property (nonatomic, copy) NSString *sdkPath;
@property (nonatomic, copy) NSString *nodePath;

- (IBAction)selectSdkClicked:(id)sender;
- (IBAction)selectNodePathClicked:(id)sender;
- (IBAction)resetNodePath:(id)sender;
- (void)updatePath;

@end
