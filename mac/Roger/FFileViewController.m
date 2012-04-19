//
//  FFileViewController.m
//  Roger
//
//  Created by Chris Stewart on 4/18/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FFileViewController.h"

@interface FFileViewController ()

@end

static NSString* const serverUrl = @"http://127.0.0.1:8081/post?apk=%@&package=%@";

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

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        fm = [NSFileManager defaultManager];
        [self setApkPath:[NSString stringWithFormat:@"%@/stripped.apk", NSHomeDirectory()]];
    }
    
    return self;
}

- (void) awakeFromNib
{
	[self registerDefaults];
	appStartedTimestamp = [NSDate date];
    pathModificationDates = [[[NSUserDefaults standardUserDefaults] dictionaryForKey:@"pathModificationDates"] mutableCopy];
	lastEventId = [[NSUserDefaults standardUserDefaults] objectForKey:@"lastEventId"];
	[self initializeEventStream];
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

- (IBAction)selectSdkClicked:(id)sender
{
    NSLog(@"selectSdkClicked");
    NSOpenPanel *openPanel = [NSOpenPanel openPanel];

    [openPanel setCanChooseDirectories:YES];
    [openPanel setCanChooseFiles:NO];

    [openPanel beginWithCompletionHandler:^(NSInteger result) {
        if (result != NSOKButton ) return;

        for (NSURL *url in [openPanel URLs]) {
            NSString *fileName = [url path];
            [self setSdkPath:[fileName stringByReplacingOccurrencesOfString:@"file://localhost" withString:@""]];
        }
    }];
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
                [statusText setTitleWithMnemonic:[NSString stringWithFormat:@"%@ changed at %@", [fullPath lastPathComponent], [NSDate date]]];
                [self androidProjectChangedWithPath:[self androidProjectDirectoryFromPath:fullPath]];
                break;
			}
		}
	}
    
	[self updateLastModificationDateForPath:path];
}

- (BOOL)fileIsAndroidXml: (NSString *)path
{
    return ([path hasSuffix:@".xml"] && [self androidProjectDirectoryFromPath:[path stringByDeletingLastPathComponent]]);
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

- (void)androidProjectChangedWithPath:(NSString *)path
{
    NSString *manifest = [NSString stringWithFormat:@"%@/AndroidManifest.xml", path];

    if (![self sdkPath]) return;

    [self buildAppWithManifest:manifest];

    //[self buildAppWithBuildFile:[NSString stringWithFormat:@"%@/%@", path, @"build.xml"]];
    NSString *apkFile = [self apkPath];
    NSLog(@"Got apk file %@", apkFile);
    
    NSString *appPackage = @"com.bignerdranch.franklin.roger.fakepackagename";
    NSLog(@"Got app package %@", appPackage);
    
    // Send it over to the server
    [self sendChangesWithPath:apkFile classname:appPackage];
}

- (void)sendChangesWithPath:(NSString *)apkPath classname:(NSString *)classname
{
    NSString *reqUrl = [NSString stringWithFormat:serverUrl, apkPath, classname];
    NSLog(@"Sending request: %@", reqUrl);
    
    NSURLRequest *req = [NSURLRequest requestWithURL:[NSURL URLWithString:reqUrl]]; 
    [NSURLConnection sendAsynchronousRequest:req queue:[NSOperationQueue mainQueue] completionHandler:^(NSURLResponse *response, NSData *data, NSError *error) {
        
        if (error) {
            NSLog(@"Unable to send to server: %@", error);
        }
        
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

    [args addObject:buildScriptPath];
    [args addObject:[self sdkPath]];
    [args addObject:manifest];
    [args addObject:[self apkPath]];
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

@end
