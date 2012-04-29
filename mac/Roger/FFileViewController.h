//
//  FFileViewController.h
//  Roger
//
//  Created by Chris Stewart on 4/18/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Cocoa/Cocoa.h>

@class FResourceName;

@interface FFileViewController : NSViewController <NSTableViewDelegate, NSTableViewDataSource> {
    NSFileManager* fm;
    NSMutableDictionary* pathModificationDates;
    NSDate* appStartedTimestamp;
    NSNumber* lastEventId;
    FSEventStreamRef stream;
    NSString *ipAddress;
    
    NSMutableArray *recentFiles;
    NSMutableArray *recentEditTimes;
    NSDateFormatter *dateFormatter;
    
    NSTask *nodeTask;

    int currentTxnId;
}

@property (nonatomic, strong) IBOutlet NSTableView *tableView;
@property (nonatomic, strong) IBOutlet NSTextField *statusText;
@property (nonatomic, strong) IBOutlet NSProgressIndicator *statusProgress;
@property (nonatomic, copy) NSString *sdkPath;
@property (nonatomic, copy) NSString *apkPath;

- (void) registerDefaults;
- (void) initializeEventStream;
- (void) addModifiedFilesAtPath: (NSString *)path;
- (void) updateLastEventId: (uint64_t) eventId;
- (BOOL)pathIsLayoutXml: (NSString *)path;
- (NSString *)resourceTypeForPath:(NSString *)path;
- (NSString *)androidProjectDirectoryFromPath:(NSString *)path;
- (void)buildAppWithBuildFile:(NSString *)buildFile;
- (void)androidProjectChangedWithPath:(NSString *)path resourceName:(FResourceName *)resourceName; 
- (NSString *)apkFileInPath:(NSString *)path;
- (NSString *)packageForManifest:(NSString *)manifest;
- (void)sendChangesWithPath:(NSString *)apk layout:(NSString *)layout type:(NSString *)type package:(NSString *)package minSdk:(int)minSdk txnId:(int)txnId;
- (NSString *)currentIPAddress;

- (int)nextTxnId;

- (void)updateStatusWithText:(NSString *)text;
- (void)hideStatus;
- (void)showStatus;

- (BOOL)startServer;
- (void)stopServer;

@end
