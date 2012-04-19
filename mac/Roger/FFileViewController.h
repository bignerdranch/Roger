//
//  FFileViewController.h
//  Roger
//
//  Created by Chris Stewart on 4/18/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Cocoa/Cocoa.h>

@interface FFileViewController : NSViewController {
    NSFileManager* fm;
    NSMutableDictionary* pathModificationDates;
    NSDate* appStartedTimestamp;
    NSNumber* lastEventId;
    FSEventStreamRef stream;
    
    IBOutlet NSTextField *statusText;
}

- (void) registerDefaults;
- (void) initializeEventStream;
- (void) addModifiedFilesAtPath: (NSString *)path;
- (void) updateLastEventId: (uint64_t) eventId;
- (BOOL)fileIsAndroidXml: (NSString *)path;
- (NSString *)androidProjectDirectoryFromPath:(NSString *)path;
- (void)buildAppWithBuildFile:(NSString *)buildFile;
- (void)androidPojectChangedWithPath:(NSString *)path;
- (NSString *)apkFileInPath:(NSString *)path;
- (NSString *)packageForManifest:(NSString *)manifest;
- (void)sendChangesWithPath:(NSString *)apkPath classname:(NSString *)classname;

@end
