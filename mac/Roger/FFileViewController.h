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
    NSString *ipAddress;
    
    NSTask *nodeTask;
    
    IBOutlet NSTextField *statusText;
}

@property (nonatomic, copy) NSString *sdkPath;
@property (nonatomic, copy) NSString *apkPath;
@property (nonatomic, strong) IBOutlet NSTextField *sdkPathField;

- (IBAction)selectSdkClicked:(id)sender;

- (void) registerDefaults;
- (void) initializeEventStream;
- (void) addModifiedFilesAtPath: (NSString *)path;
- (void) updateLastEventId: (uint64_t) eventId;
- (BOOL)fileIsAndroidXml: (NSString *)path;
- (NSString *)androidProjectDirectoryFromPath:(NSString *)path;
- (void)buildAppWithBuildFile:(NSString *)buildFile;
- (void)androidProjectChangedWithPath:(NSString *)path layout:(NSString *)layout;
- (NSString *)apkFileInPath:(NSString *)path;
- (NSString *)packageForManifest:(NSString *)manifest;
- (void)sendChangesWithPath:(NSString *)apkPath layout:(NSString *)layout;
- (NSString *)currentIPAddress;

- (void)startServer;
- (void)stopServer;

@end
