//
//  FAppDelegate.h
//  Roger
//
//  Created by Chris Stewart on 4/18/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import "FFileViewController.h"

@interface FAppDelegate : NSObject <NSApplicationDelegate> {
    FFileViewController *controller;
}

@property (assign) IBOutlet NSWindow *window;

@end
