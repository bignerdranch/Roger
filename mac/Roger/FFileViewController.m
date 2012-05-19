//
//  FFileViewController.m
//  Roger
//
//  Created by Chris Stewart on 4/18/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FFileViewController.h"
#import "FakeProjectBuilder.h"
#import "FResourceName.h"
#import "FTaskStream.h"
#import "NSString+Regexen.h"
#import "FIntent.h"
#import "FADB.h"
#import "FADBMonitor.h"
#import "FADBDevice.h"
#import "FNodeServer.h"

@interface FFileViewController ()

@property (nonatomic, strong) FADB *adb;
@property (nonatomic, strong) FADBMonitor *adbMonitor;
@property (nonatomic, strong) FNodeServer *nodeServer;

- (BOOL)buildAppWithManifest:(NSString *)manifest resourceName:(FResourceName *)resName;

- (void)sendIntentToAllWithTxnId:(FIntent *)intent;
- (void)sendIntentToAll:(FIntent *)intent;
- (void)sendToAdbApkPath:(NSString *)apkPath intent:(FIntent *)intent;
- (void)sendToNodeApkPath:(NSString *)apkPath intent:(FIntent *)intent;

- (NSString *)rogerConstant:(NSString *)name;

- (BOOL)isSupportedType:(NSString *)type;

- (void)initTxnId;
- (NSString *)adbPath;
- (NSString *)fileServerPath;

@end

static NSString* const serverUrl = @"http://%@:8081/post?apk=%@&layout=%@&type=%@&pack=%@&minSdk=%d&txnId=%d";

void fsevents_callback(ConstFSEventStreamRef streamRef,
                       void *userData,
                       size_t numEvents,
                       void *eventPaths,
                       const FSEventStreamEventFlags eventFlags[],
                       const FSEventStreamEventId eventIds[])
{
    FFileViewController *controller = (__bridge FFileViewController *)userData;
	size_t i;
	for(i=0; i<numEvents; i++){
        [controller addModifiedFilesAtPath:[(__bridge NSArray *)eventPaths objectAtIndex:i]];
		[controller updateLastEventId:eventIds[i]];
	}
    
}

@implementation FFileViewController

@dynamic sdkPath;
@synthesize apkPath=_apkPath;
@synthesize tableView;
@synthesize statusText;
@synthesize statusProgress;

@synthesize adb=_adb;
@synthesize adbMonitor=_adbMonitor;
@synthesize nodeServer=_nodeServer;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        fm = [NSFileManager defaultManager];
        [self setAdb:[[FADB alloc] init]];
        [self setAdbMonitor:[[FADBMonitor alloc] initWithAdb:[self adb]]];
        [self setApkPath:[[self fileServerPath] stringByAppendingPathComponent:@"stripped.apk"]];
        [self setSdkPath:[[NSUserDefaults standardUserDefaults] stringForKey:@"SdkDirKey"]];
        if (![self sdkPath]) [self setSdkPath:@""];
        
        recentFiles = [[NSMutableArray alloc] init];
        recentEditTimes = [[NSMutableArray alloc] init];
        dateFormatter = [[NSDateFormatter alloc] init];
        [dateFormatter setDateStyle:NSDateFormatterShortStyle];
        [dateFormatter setTimeStyle:NSDateFormatterShortStyle];
    }
    
    return self;
}

- (void)setSdkPath:(NSString *)sdkPath
{
    _sdkPath = [sdkPath copy];

    [[self adb] setAdbPath:[self adbPath]];
}

- (NSString *)sdkPath
{
    return _sdkPath;
}

- (IBAction) checkDevicesButtonClicked:(id)sender
{
    [self.adbMonitor checkDevices];
}

- (void) awakeFromNib
{
	[self registerDefaults];
	appStartedTimestamp = [NSDate date];
    pathModificationDates = [[[NSUserDefaults standardUserDefaults] dictionaryForKey:@"pathModificationDates"] mutableCopy];
	lastEventId = [[NSUserDefaults standardUserDefaults] objectForKey:@"lastEventId"];
    
    [self hideStatus];
	[self initializeEventStream];
    
    NSString *ip = [self currentIPAddress];
    NSLog(@"Current ip: %@", ip);

    [self initTxnId];
}

- (void) initializeEventStream
{
    NSString *myPath = NSHomeDirectory();
    NSArray *pathsToWatch = [NSArray arrayWithObject:myPath];
    void *appPointer = (__bridge_retained void *)self;
    FSEventStreamContext context = {0, appPointer, NULL, NULL, NULL};
    NSTimeInterval latency = 3.0;
    
	stream = FSEventStreamCreate(NULL,
	                             &fsevents_callback,
	                             &context,
	                             (__bridge_retained CFArrayRef) pathsToWatch,
	                             [lastEventId unsignedLongLongValue],
	                             (CFAbsoluteTime) latency,
	                             kFSEventStreamCreateFlagUseCFTypes 
                                 );
    
	FSEventStreamScheduleWithRunLoop(stream, CFRunLoopGetCurrent(), kCFRunLoopDefaultMode);
	FSEventStreamStart(stream);
}

- (NSApplicationTerminateReply)applicationShouldTerminate: (NSApplication *)app
{
	NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
	[defaults setObject:lastEventId forKey:@"lastEventId"];
	[defaults setObject:pathModificationDates forKey:@"pathModificationDates"];
	[defaults synchronize];
    FSEventStreamStop(stream);
    FSEventStreamInvalidate(stream);
    return NSTerminateNow;
}

- (void)updateStatusWithText:(NSString *)text
{
    [statusText setTitleWithMnemonic:text];
}

- (void)hideStatus
{
    [statusText setHidden:YES];
    [statusProgress setHidden:YES];
    [statusProgress stopAnimation:self];
}

- (void)showStatus
{
    [statusText setHidden:NO];
    [statusProgress setHidden:NO];
    [statusProgress startAnimation:self];
}

- (void) registerDefaults
{
	NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
	NSDictionary *appDefaults = [NSDictionary
	                             dictionaryWithObjects:[NSArray arrayWithObjects:[NSNumber numberWithUnsignedLongLong:kFSEventStreamEventIdSinceNow], [NSMutableDictionary new], nil]
	                             forKeys:[NSArray arrayWithObjects:@"lastEventId", @"pathModificationDates", nil]];
	[defaults registerDefaults:appDefaults];
}


- (void)updateLastModificationDateForPath: (NSString *)path
{
	[pathModificationDates setObject:[NSDate date] forKey:path];
}

- (NSDate *)lastModificationDateForPath: (NSString *)path
{
	if(nil != [pathModificationDates valueForKey:path]) {
		return [pathModificationDates valueForKey:path];
	}
	else{
		return appStartedTimestamp;
	}
}


- (void)updateLastEventId: (uint64_t) eventId
{
	lastEventId = [NSNumber numberWithUnsignedLongLong:eventId];
}

- (void)initTxnId
{
    currentTxnId = random();
}

- (void) addModifiedFilesAtPath: (NSString *)path
{
    if ([path rangeOfString:@"FakeProjectByRogerByFranklin"].location != NSNotFound)
        return;

    NSError *err = nil;
	NSArray *contents = [fm contentsOfDirectoryAtPath:path error:&err];
    if (err) {
        NSLog(@"Got error: %@", err);
        return;
    }
    
	NSString* fullPath = nil;
    
	for (NSString* node in contents) {
        fullPath = [NSString stringWithFormat:@"%@/%@",path,node];
        NSString *type = [self resourceTypeForPath:fullPath];

        if (!type || ![self isSupportedType:type]) {
            continue;
        }

        NSDictionary *fileAttributes = [fm attributesOfItemAtPath:fullPath error:NULL];
        NSDate *fileModDate = [fileAttributes objectForKey:NSFileModificationDate];

        if ([fileModDate compare:[self lastModificationDateForPath:path]] != NSOrderedDescending) {
            continue;
        }

        NSLog(@"File change at: %@", fullPath);
        
        [recentFiles addObject:[fullPath lastPathComponent]];
        [recentEditTimes addObject:[NSDate date]];
        [[self tableView] reloadData];
        
        [self showStatus];
        [self updateStatusWithText:[NSString stringWithFormat:@"Processing %@", [fullPath lastPathComponent]]];

        FResourceName *resourceName = [FResourceName 
            resourceNameWithType:type
                            name:[[fullPath lastPathComponent] stringByDeletingPathExtension]];
        [self androidProjectChangedWithPath:[self androidProjectDirectoryFromPath:fullPath] 
                               resourceName:resourceName];
        break;
	}
    
	[self updateLastModificationDateForPath:path];
}

- (NSString *)apparentResourceTypeForPath:(NSString *)path
{
    NSArray *results = [path stringsFromFirstMatchOfPattern:@"res/([a-z]+)[-a-z0-9]*//*[^/]*\\.xml$"];

    if ([results count]) {
        return [results objectAtIndex:0];
    } else {
        return nil;
    }
}

- (BOOL)isLayoutPath:(NSString *)path
{
    NSError *err = nil;
    NSRegularExpression *regex = [NSRegularExpression         
        regularExpressionWithPattern:@"res/layout[-a-z0-9]*//*[^/]*\\.xml$"
        options:NSRegularExpressionCaseInsensitive
        error:&err];
    NSArray *matches = [regex matchesInString:path options:NSRegularExpressionCaseInsensitive range:NSMakeRange(0, [path length])];
    
    BOOL isLayout = matches && [matches count];
    return isLayout;
}

- (BOOL)isSupportedType:(NSString *)type
{
    return 
        [type isEqualToString:@"layout"] ||
        [type isEqualToString:@"drawable"];
}

- (NSString *)resourceTypeForPath:(NSString *)path
{
    NSString *resourceType = [self apparentResourceTypeForPath:path];

    if (resourceType && [self androidProjectDirectoryFromPath:[path stringByDeletingLastPathComponent]]) {
        return resourceType;
    } else {
        return nil;
    }
}

- (BOOL)pathIsLayoutXml: (NSString *)path
{
    return [self isLayoutPath:path] && 
        [self androidProjectDirectoryFromPath:[path stringByDeletingLastPathComponent]];
}

- (NSString *)androidProjectDirectoryFromPath:(NSString *)path;
{
    if (!path || [path length] <= 1) {
        return nil;
    }
    
    NSError *err = nil;
    NSArray *contents = [fm contentsOfDirectoryAtPath:path error:&err];
    if (err) {
        NSLog(@"directoryIsAndroidProject got error: %@", err);
    }
    
    for (NSString *file in contents) {
        if ([file hasSuffix:@"AndroidManifest.xml"]) {
            NSLog(@"Found AndroidManifest.xml!");
            return path;
        }
    }
    
    return [self androidProjectDirectoryFromPath:[path stringByDeletingLastPathComponent]];
}

- (void)androidProjectChangedWithPath:(NSString *)path resourceName:(FResourceName *)resourceName
{
    NSString *manifest = [NSString stringWithFormat:@"%@/AndroidManifest.xml", path];

    if (![self sdkPath]) return;

    FIntent *beginBuild = [[FIntent alloc] 
        initBroadcastWithAction:[self rogerConstant:@"ACTION_BUILD_START"]];
    NSString *message = [NSString stringWithFormat:@"Building %@...", [path lastPathComponent]];
    [beginBuild setExtra:[self rogerConstant:@"EXTRA_MESSAGE"] string:message];
    [self sendIntentToAllWithTxnId:beginBuild];

    BOOL success = [self buildAppWithManifest:manifest resourceName:resourceName];

    if (!success) {
        NSLog(@"bailing on publish");
        return;
    }

    NSString *package = [self packageForManifest:manifest];
    int minSdkVersion = [self minSdkForManifest:manifest];
    NSLog(@"Got package %@", package);
    
    NSString *apkFile = [self apkPath];
    NSLog(@"Got apk file %@", apkFile);
    
    [self updateStatusWithText:@"Uploading"];

    int txnId = [self nextTxnId];
    // Send it over to the server
    [self sendChangesWithPath:apkFile 
                       layout:[resourceName name] 
                         type:[resourceName type] 
                      package:package 
                       minSdk:minSdkVersion 
                        txnId:txnId];
}

- (int)nextTxnId
{
    return ++currentTxnId;
}

- (NSString *)rogerConstant:(NSString *)name
{
    return [@"com.bignerdranch.franklin.roger." stringByAppendingString:name];
}

- (void)sendChangesWithPath:(NSString *)apkPath layout:(NSString *)layout type:(NSString *)type package:(NSString *)package minSdk:(int)minSdk txnId:(int)txnId
{
    FIntent *newLayoutIntent = [[FIntent alloc]
        initBroadcastWithAction:[self rogerConstant:@"ACTION_NEW_LAYOUT"]];
    [newLayoutIntent setExtra:[self rogerConstant:@"EXTRA_LAYOUT_LAYOUT_NAME"]
                       string:layout];
    [newLayoutIntent setExtra:[self rogerConstant:@"EXTRA_LAYOUT_LAYOUT_TYPE"]
                       string:type];
    [newLayoutIntent setExtra:[self rogerConstant:@"EXTRA_LAYOUT_PACKAGE_NAME"]
                       string:package];
    [newLayoutIntent setExtra:[self rogerConstant:@"EXTRA_LAYOUT_MIN_VERSION"]
                       number:[NSNumber numberWithInt:minSdk]];
    [newLayoutIntent setExtra:[self rogerConstant:@"EXTRA_LAYOUT_TXN_ID"]
                       number:[NSNumber numberWithInt:txnId]];

    [self sendToAdbApkPath:apkPath intent:newLayoutIntent];
    [self sendToNodeApkPath:apkPath intent:newLayoutIntent];
}

- (void)sendIntentToAllWithTxnId:(FIntent *)intent
{
    [intent setExtra:[self rogerConstant:@"EXTRA_LAYOUT_TXN_ID"]
              number:[NSNumber numberWithInt:[self nextTxnId]]];
    [intent addCategory:[self rogerConstant:@"CATEGORY_DEDUPE"]];

    [self sendIntentToAll:intent];
}

- (void)sendIntentToAll:(FIntent *)intent
{
    [self.adbMonitor sendIntent:intent];
    [self.nodeServer sendIntent:intent];
}

- (void)sendToAdbApkPath:(NSString *)apkPath intent:(FIntent *)newLayoutIntent
{
    // build a notification of an upcoming transaction
    FIntent *incomingIntent = [[FIntent alloc] 
        initBroadcastWithAction:[self rogerConstant:@"ACTION_INCOMING_TXN"]];
    [incomingIntent copyExtra:[self rogerConstant:@"EXTRA_LAYOUT_TXN_ID"]
                   fromIntent:newLayoutIntent];

    NSString *fileName = [apkPath lastPathComponent];

    for (FADBDevice *device in [self.adbMonitor devices]) {
        FIntent *deviceLayoutIntent = [newLayoutIntent copy];

        NSString *devicePath = [device.externalStoragePath stringByAppendingPathComponent:fileName];

        [deviceLayoutIntent addCategory:[self rogerConstant:@"CATEGORY_LOCAL"]];
        [deviceLayoutIntent setExtra:[self rogerConstant:@"EXTRA_LAYOUT_APK_PATH"] 
                              string:devicePath];

        // send incoming notification first
        [self.adb sendIntent:incomingIntent toDevice:[device serial] completion:nil];
        [self.adb copyLocalPath:apkPath toDevicePath:devicePath device:[device serial] completion:^{
            // then send the new layout notification
            [self.adb sendIntent:deviceLayoutIntent toDevice:[device serial] completion:nil];
        }];

    }
}

- (void)sendToNodeApkPath:(NSString *)apkPath intent:(FIntent *)newLayoutIntent
{
    newLayoutIntent = [newLayoutIntent copy];

    NSString *fileName = [apkPath lastPathComponent];
    NSString *serverPath = [self.nodeServer urlPathForFile:fileName];

    [newLayoutIntent addCategory:[self rogerConstant:@"CATEGORY_REMOTE"]];
    [newLayoutIntent setExtra:[self rogerConstant:@"EXTRA_LAYOUT_APK_PATH"] 
                          string:serverPath];
    [self.nodeServer sendIntent:newLayoutIntent];
}

- (NSString *)apkFileInPath:(NSString *)path
{
    NSLog(@"apkFileInPath with path: %@", path);
    NSError *err = nil;
    NSArray *contents = [fm contentsOfDirectoryAtPath:path error:&err];
    if (err) {
        NSLog(@"apkFileInPath got error: %@", err);
        return nil;
    }
    
    for (NSString *file in contents) {
        if ([file hasSuffix:@"-debug.apk"]) {
            return [NSString stringWithFormat:@"%@/%@", path, file];
        }
    }

    return nil;
}

- (int)minSdkForManifest:(NSString *)manifest
{
    NSLog(@"Package for manifest: %@", manifest);
    NSError *err = nil;
    NSString *manifestContents = [NSString stringWithContentsOfFile:manifest encoding:NSUTF8StringEncoding error:&err];
    
    if (err) {
        NSLog(@"Unable to read file: %@ error %@", manifest, err);
        return 0;
    }
    
    NSRegularExpression *regex = [NSRegularExpression         
        regularExpressionWithPattern:@"android:minSdkVersion=\"([^\"]*)\""
        options:NSRegularExpressionCaseInsensitive
        error:&err];

    NSArray *matches = [regex matchesInString:manifestContents 
                                      options:NSRegularExpressionCaseInsensitive 
                                        range:NSMakeRange(0, [manifestContents length])];
    
    if (err) {
        NSLog(@"Got regex error: %@", err);
        return 0;
    }
    
    for (NSTextCheckingResult *match in matches) {
        NSString *sdkVersionString = [manifestContents substringWithRange:[match rangeAtIndex:1]];

        return [sdkVersionString intValue];
    }
    
    return 0;
}

- (NSString *)packageForManifest:(NSString *)manifest
{
    NSLog(@"Package for manifest: %@", manifest);
    NSError *err = nil;
    NSString *manifestContents = [NSString stringWithContentsOfFile:manifest encoding:NSUTF8StringEncoding error:&err];
    
    if (err) {
        NSLog(@"Unable to read file: %@ error %@", manifest, err);
        return nil;
    }
    
    NSRegularExpression *regex = [NSRegularExpression         
                                  regularExpressionWithPattern:@"package=\"([^\"]*)\""
                                  options:NSRegularExpressionCaseInsensitive
                                  error:&err];
    NSArray *matches = [regex matchesInString:manifestContents options:NSRegularExpressionCaseInsensitive range:NSMakeRange(0, [manifestContents length])];
    
    if (err) {
        NSLog(@"Got regex error: %@", err);
        return nil;
    }
    
    for (NSTextCheckingResult *match in matches) {
        return [manifestContents substringWithRange:[match rangeAtIndex:1]];
    }
    
    return nil;
}

- (NSString *)fileServerPath
{
    // return something temporary
    NSString *fileServerPath = [NSTemporaryDirectory() stringByAppendingPathComponent:@"rogerFileServer"];
    [[NSFileManager defaultManager] 
        createDirectoryAtPath:fileServerPath 
  withIntermediateDirectories:YES 
                   attributes:[NSDictionary dictionaryWithObjectsAndKeys:
                               [NSNumber numberWithBool:NO], NSFileImmutable,
                    nil]
                        error:nil];
    return fileServerPath;
}

- (NSString *)adbPath
{
    return [[self sdkPath] stringByAppendingPathComponent:@"platform-tools/adb"];
}

- (NSString *)buildScriptPath
{
    NSBundle *bundle = [NSBundle mainBundle];
    return [bundle pathForResource:@"build_fake_package" ofType:@"sh"];
}

- (NSString *)fakeManifestPath 
{
    NSBundle *bundle = [NSBundle mainBundle];
    return [bundle pathForResource:@"AndroidManifest" ofType:@"xml"];
}

- (NSString *)publishApkToAdbDevicesPath
{
    NSBundle *bundle = [NSBundle mainBundle];
    return [bundle pathForResource:@"publish_apk_to_adb_devices" ofType:@"sh"];
}

- (NSString *)ipAddressScriptPath
{
    NSBundle *bundle = [NSBundle mainBundle];
    return [bundle pathForResource:@"en1_addr" ofType:@"sh"];
}

- (NSString *)serverPath
{
    NSBundle *bundle = [NSBundle mainBundle];
    return [bundle pathForResource:@"RogerServer" ofType:@"js"];
}

- (NSString *)buildFakeProjectForProjectPath:(NSString *)projectPath resourceName:(FResourceName *)resName
{
    NSLog(@"building fake project for project path:%@ resourceName:%@", projectPath, resName);

    NSString *targetPath = [projectPath stringByAppendingPathComponent:@"FakeProjectByRogerByFranklin"];

    [[[FakeProjectBuilder alloc] init] buildFakeResourcesAtPath:targetPath
                                                 forProjectPath:projectPath
                                             targetResourceName:resName];
    return targetPath;
}

- (void)cleanupFakeProjectAtPath:(NSString *)targetPath
{
    NSError *err = nil;
    // delete the targetpath
    if (![[NSFileManager defaultManager] removeItemAtPath:targetPath error:&err]) {
        NSLog(@"failed to delete %@: %@", targetPath, err);
    }
}

- (BOOL)buildAppWithManifest:(NSString *)manifest resourceName:(FResourceName *)resName
{
    NSString *projectPath = [manifest stringByDeletingLastPathComponent];
    NSLog(@"Building apk with manifest: %@", projectPath);

    NSString *fakeProjectPath = [self buildFakeProjectForProjectPath:projectPath resourceName:resName];
    NSLog(@"Fake project path is: %@", fakeProjectPath);

    NSTask *task = [[NSTask alloc] init];
    NSMutableArray *args = [NSMutableArray array];
    
    NSMutableDictionary *env = [NSMutableDictionary dictionaryWithObjectsAndKeys:
                                @"/usr/share/ant", @"ANT_HOME",
                                nil];
    
    NSString *buildScriptPath = [self buildScriptPath];
    NSLog(@"build script path: %@", buildScriptPath);
    NSLog(@"sdkPath: %@", [self sdkPath]);
    NSLog(@"apkPath: %@", [self apkPath]);

    [args addObject:buildScriptPath]; [args addObject:[self sdkPath]];
    [args addObject:manifest];
    [args addObject:[self apkPath]];
    [args addObject:fakeProjectPath];
    [task setCurrentDirectoryPath:NSHomeDirectory()];
    [task setEnvironment:env];
    [task setLaunchPath:@"/bin/sh"];
    [task setArguments:args];

    //// If the output from this task is not piped somewhere else, regular NSLog messages will not show up
    //// after the task logs any messages
    NSPipe *outputPipe = [NSPipe pipe];
    [task setStandardInput:[NSPipe pipe]];
    [task setStandardOutput:outputPipe];
    
    FTaskStream *taskStream = [FTaskStream taskStreamForUnlaunchedTask:task];
    [taskStream addLogEventsWithPrefix:@"BUILD" isOutput:YES];
    // rewrite error lines
    NSMutableArray *errorLines = [[NSMutableArray alloc] init];
    [taskStream addErrorEvent:@"." withBlock:^(NSString *line) {
        if ([line stringsFromFirstMatchOfPattern:@"^/.*:[1-9]+[0-9]*: "]) {
            
            line = [line stringByReplacingPattern:@"^/.*/(res/[^/]*/[^.]*\\.xml)"
                                     withTemplate:@"$1"];
        }
        if (line) {
            NSLog(@"BUILD ERROR: %@", line);
            [errorLines addObject:line];
        } else if ([errorLines count] > 0) {
            // publish it
            NSString *errorString = [errorLines componentsJoinedByString:@"\n"];
            FIntent *intent = [[FIntent alloc]
                initBroadcastWithAction:[self rogerConstant:@"ACTION_BUILD_ERROR"]];
            [intent setExtra:[self rogerConstant:@"EXTRA_MESSAGE"] string:errorString];
            [self sendIntentToAllWithTxnId:intent];
        }
    }];
    
    [task launch];
    [task waitUntilExit];

    [self cleanupFakeProjectAtPath:fakeProjectPath];

    if ([task terminationStatus]) {
        NSLog(@"failed to build package");
        return NO;
    } else {
        NSLog(@"succeeded in building package");
        return YES;
    }
}

- (void)buildAppWithBuildFile:(NSString *)buildFile 
{
    NSLog(@"Building apk with build.xml: %@", [buildFile stringByDeletingLastPathComponent]);
    NSTask *task = [[NSTask alloc] init];
    NSMutableArray *args = [NSMutableArray array];
    
    NSMutableDictionary *env = [NSMutableDictionary dictionaryWithObjectsAndKeys:
                                @"/usr/share/ant", @"ANT_HOME",
                                nil];
    
    [args addObject:@"ant"];
    [args addObject:@"clean"];
    [args addObject:@"debug"];
    [args addObject:@"-buildfile"];
    [args addObject:buildFile];
    [task setCurrentDirectoryPath:NSHomeDirectory()];
    [task setEnvironment:env];
    [task setLaunchPath:@"/bin/sh"];
    [task setArguments:args];
    
    // If the output from this task is not piped somewhere else, regular NSLog messages will not show up
    // after the task logs any messages
    NSPipe *outputPipe = [NSPipe pipe];
    [task setStandardInput:[NSPipe pipe]];
    [task setStandardOutput:outputPipe];
    
    [task launch];
    [task waitUntilExit];
}

- (NSString *)currentMulticastAddress
{
    return @"234.5.6.7";
    //NSMutableArray *components = [[[self currentIPAddress] componentsSeparatedByString:@"."] mutableCopy];
    //[components removeObjectAtIndex:3];
    //[components addObject:@"255"];

    //NSString *multicastAddress = [components componentsJoinedByString:@"."];
    //return multicastAddress;
}

- (void)startServer
{
    if (self.nodeServer) return;

    if (!ipAddress) {
        NSLog(@"unable to start server - no wifi ip address");
        return;
    }

    self.nodeServer = [[FNodeServer alloc] initWithIpAddress:ipAddress fileServerPath:[self fileServerPath]];
}

- (void)stopServer
{
    NSLog(@"Stopping server");
    [nodeTask terminate];
}

- (NSString *)currentIPAddress
{
    if (ipAddress) {
        return ipAddress;
    }
    
    NSTask* task = [[NSTask alloc] init];
    [task setStandardOutput:[NSPipe pipe]];
    [task setLaunchPath:@"/bin/sh"];
    [task setArguments:[NSArray arrayWithObjects:
        [self ipAddressScriptPath], 
        nil]];

    [task setStandardOutput:[NSPipe pipe]];
    [task setStandardInput:[NSPipe pipe]];
    [task launch];
    [task waitUntilExit];
    
    if ([task terminationStatus]) {
        return nil;
    } else {
        NSData *data = [[[task standardOutput] fileHandleForReading] availableData];
        if ((data != nil) && [data length]) {
            ipAddress = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            ipAddress = [ipAddress stringByTrimmingCharactersInSet:[NSCharacterSet newlineCharacterSet]];
        }
    }
    
    return ipAddress;
}

- (NSInteger)numberOfRowsInTableView:(NSTableView *)tableView
{
    return [recentFiles count];
}

- (id)tableView:(NSTableView *)tableView objectValueForTableColumn:(NSTableColumn *)tableColumn row:(NSInteger)row
{
    int index = (([recentFiles count] - 1) - row);
    
    if ([[tableColumn identifier] isEqualToString:@"file"]) {
        return [recentFiles objectAtIndex:index];
    } else {
        NSDate *date = [recentEditTimes objectAtIndex:index];
        return [dateFormatter stringFromDate:date];
    }

}

-(void)dealloc
{
    // kill the monitor so that its timer will stop
    [[self adbMonitor] kill];
}

@end
