//
//  FAppDelegate.h
//  Roger
//
//  Created by Chris Stewart on 4/18/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import <CoreData/CoreData.h>
#import "FFileViewController.h"
#import "FPreferenceWindowController.h"

@interface FAppDelegate : NSObject <NSApplicationDelegate> {
    FFileViewController *controller;
    FPreferenceWindowController *pref;
}

@property (assign) IBOutlet NSWindow *window;
@property (nonatomic, strong, readonly) NSManagedObjectContext *managedObjectContext;
@property (nonatomic, strong, readonly) NSManagedObjectModel *managedObjectModel;
@property (nonatomic, strong, readonly) NSPersistentStoreCoordinator *persistentStoreCoordinator;

- (IBAction)showPreferencePanel:(id)sender;

- (void)restartServer;

@end
