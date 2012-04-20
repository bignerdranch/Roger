//
//  FFileViewController.m
//  Roger
//
//  Created by Chris Stewart on 4/18/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FFileViewController.h"

//#define DEBUG_NODE 1

@interface FFileViewController ()

@end

static NSString* const serverUrl = @"http://%@:8081/post?apk=%@&layout=%@&pack=%@&minSdk=%d";

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

@synthesize sdkPath;
@synthesize apkPath;
@synthesize tableView;
@synthesize statusText;
@synthesize statusProgress;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        fm = [NSFileManager defaultManager];
        [self setApkPath:[NSString stringWithFormat:@"%@/stripped.apk", NSHomeDirectory()]];
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

- (void) addModifiedFilesAtPath: (NSString *)path
{
    NSError *err = nil;
	NSArray *contents = [fm contentsOfDirectoryAtPath:path error:&err];
    if (err) {
        NSLog(@"Got error: %@", err);
        return;
    }
    
	NSString* fullPath = nil;
    
	for (NSString* node in contents) {
        fullPath = [NSString stringWithFormat:@"%@/%@",path,node];
        if ([self fileIsAndroidXml:fullPath])
		{
            NSDictionary *fileAttributes = [fm attributesOfItemAtPath:fullPath error:NULL];
			NSDate *fileModDate = [fileAttributes objectForKey:NSFileModificationDate];
			if([fileModDate compare:[self lastModificationDateForPath:path]] == NSOrderedDescending) {
                NSLog(@"File change at: %@", fullPath);
                
                [recentFiles addObject:[fullPath lastPathComponent]];
                [recentEditTimes addObject:[NSDate date]];
                [[self tableView] reloadData];
                
                [self showStatus];
                [self updateStatusWithText:[NSString stringWithFormat:@"Processing %@", [fullPath lastPathComponent]]];
                [self androidProjectChangedWithPath:[self androidProjectDirectoryFromPath:fullPath] layout:[fullPath lastPathComponent]];
                break;
			}
		}
	}
    
	[self updateLastModificationDateForPath:path];
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

- (BOOL)fileIsAndroidXml: (NSString *)path
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

- (void)androidProjectChangedWithPath:(NSString *)path layout:(NSString *)layout
{
    NSString *manifest = [NSString stringWithFormat:@"%@/AndroidManifest.xml", path];

    if (![self sdkPath]) return;

    [self buildAppWithManifest:manifest];

    NSString *package = [self packageForManifest:manifest];
    int minSdkVersion = [self minSdkForManifest:manifest];
    NSLog(@"Got package %@", package);
    
    NSString *apkFile = [self apkPath];
    NSLog(@"Got apk file %@", apkFile);
    
    [self updateStatusWithText:@"Uploading"];
    // Send it over to the server
    [self sendChangesWithPath:apkFile layout:layout package:package minSdk:minSdkVersion];
}

- (void)sendChangesWithPath:(NSString *)apk layout:(NSString *)layout package:(NSString *)package minSdk:(int)minSdk
{
    NSString *reqUrl = [NSString stringWithFormat:serverUrl, [self currentIPAddress], apk, layout, package, minSdk];
    NSLog(@"Sending request: %@", reqUrl);
    NSLog(@"Our file is this many bytes: %ld", [[NSData dataWithContentsOfFile:apk] length]);
    
    NSURLRequest *req = [NSURLRequest requestWithURL:[NSURL URLWithString:reqUrl]]; 
    [NSURLConnection sendAsynchronousRequest:req queue:[NSOperationQueue mainQueue] completionHandler:^(NSURLResponse *response, NSData *data, NSError *error) {
        
        if (error) {
            NSLog(@"Unable to send to server: %@", error);
        }
        
        [self hideStatus];
    }];
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
        return nil;
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

- (void)buildAppWithManifest:(NSString *)manifest
{
    NSLog(@"Building apk with manifest: %@", [manifest stringByDeletingLastPathComponent]);
    NSTask *aTask = [[NSTask alloc] init];
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
    [args addObject:[self fakeManifestPath]];
    [aTask setCurrentDirectoryPath:NSHomeDirectory()];
    [aTask setEnvironment:env];
    [aTask setLaunchPath:@"/bin/sh"];
    [aTask setArguments:args];
    
    // If the output from this task is not piped somewhere else, regular NSLog messages will not show up
    // after the task logs any messages
    NSPipe *outputPipe = [NSPipe pipe];
    [aTask setStandardInput:[NSPipe pipe]];
    [aTask setStandardOutput:outputPipe];
    
    [aTask launch];
    [aTask waitUntilExit];
}

- (void)buildAppWithBuildFile:(NSString *)buildFile 
{
    NSLog(@"Building apk with build.xml: %@", [buildFile stringByDeletingLastPathComponent]);
    NSTask *aTask = [[NSTask alloc] init];
    NSMutableArray *args = [NSMutableArray array];
    
    NSMutableDictionary *env = [NSMutableDictionary dictionaryWithObjectsAndKeys:
                                @"/usr/share/ant", @"ANT_HOME",
                                nil];
    
    [args addObject:@"ant"];
    [args addObject:@"clean"];
    [args addObject:@"debug"];
    [args addObject:@"-buildfile"];
    [args addObject:buildFile];
    [aTask setCurrentDirectoryPath:NSHomeDirectory()];
    [aTask setEnvironment:env];
    [aTask setLaunchPath:@"/bin/sh"];
    [aTask setArguments:args];
    
    // If the output from this task is not piped somewhere else, regular NSLog messages will not show up
    // after the task logs any messages
    NSPipe *outputPipe = [NSPipe pipe];
    [aTask setStandardInput:[NSPipe pipe]];
    [aTask setStandardOutput:outputPipe];
    
    [aTask launch];
    [aTask waitUntilExit];
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
    NSOperationQueue *queue = [[NSOperationQueue alloc] init];
    [queue addOperationWithBlock:^(void) {
        nodeTask = [[NSTask alloc] init];
        NSMutableArray *args = [NSMutableArray array];
     
        NSString *path = [self serverPath];
        NSLog(@"Server path: %@", path);
        
        [args addObject:path];
        [args addObject:ipAddress];
        [args addObject:[self currentMulticastAddress]];
        [nodeTask setLaunchPath:@"/usr/local/bin/node"];
        [nodeTask setArguments:args];
        
#ifndef DEBUG_NODE
        NSPipe *output = [NSPipe pipe];
        [nodeTask setStandardOutput:output];
        [nodeTask setStandardInput:[NSPipe pipe]];
#endif
        
        [nodeTask launch];
        
#ifndef DEBUG_NODE
        NSData *data = [[output fileHandleForReading] availableData];
        NSLog(@"Output %@", [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding]);
#endif

    }];
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
    [task setArguments:[NSArray arrayWithObjects:[self ipAddressScriptPath], nil]];

    [task setStandardOutput:[NSPipe pipe]];
    [task setStandardInput:[NSPipe pipe]];
    [task launch];
    [task waitUntilExit];
    
    NSData *data = [[[task standardOutput] fileHandleForReading] availableData];
    if ((data != nil) && [data length]) {
        ipAddress = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        ipAddress = [ipAddress stringByTrimmingCharactersInSet:[NSCharacterSet newlineCharacterSet]];
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


@end
