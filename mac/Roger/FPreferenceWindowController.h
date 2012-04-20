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
@property (nonatomic, copy) NSString *sdkPath;

- (IBAction)selectSdkClicked:(id)sender;
- (void)updatePath;

@end
