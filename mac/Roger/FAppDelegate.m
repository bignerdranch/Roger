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
@synthesize managedObjectContext=_managedObjectContext;
@synthesize managedObjectModel=_managedObjectModel;
@synthesize persistentStoreCoordinator=_persistentStoreCoordinator;

- (void)initializeCoreData
{
    if (self.managedObjectModel && self.persistentStoreCoordinator && self.managedObjectContext) 
        return;

    _managedObjectModel = [NSManagedObjectModel mergedModelFromBundles:nil];

    NSString *dir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) lastObject];
    NSURL *storeURL = [NSURL fileURLWithPath:[dir stringByAppendingPathComponent:@"Roger.sqlite"]];

    NSDictionary *options = [NSDictionary dictionaryWithObjectsAndKeys:

        [NSNumber numberWithBool:YES], NSMigratePersistentStoresAutomaticallyOption,
        [NSNumber numberWithBool:YES], NSInferMappingModelAutomaticallyOption,
        nil];

    NSError *err = nil;
    _persistentStoreCoordinator = [[NSPersistentStoreCoordinator alloc] 
        initWithManagedObjectModel:self.managedObjectModel];

    if (![self.persistentStoreCoordinator 
            addPersistentStoreWithType:NSSQLiteStoreType
                         configuration:nil
                                   URL:storeURL
                               options:options
                                 error:&err]) {

        NSLog(@"Failed to add persistent store: %@, %@", err, [err userInfo]);
        abort();
    }

    _managedObjectContext = [[NSManagedObjectContext alloc] init];

    self.managedObjectContext.persistentStoreCoordinator = self.persistentStoreCoordinator;

    NSLog(@"CoreData back end initialized");
}

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
    [self initializeCoreData];

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

    NSError *error = nil;
    if (![self.managedObjectContext save:&error]) {
        NSLog(@"failed to save managed object context: %@", error);
    } else {
        NSLog(@"managed object context saved successfully");
    }

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
