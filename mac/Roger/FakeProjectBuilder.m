#import "FakeProjectBuilder.h"
#import "FResourceIndex.h"
#import "FResourceName.h"
#import "NSString+Resources.h"

@interface FakeProjectBuilder () 

- (BOOL)isExpandable:(NSString *)fullPath;
- (BOOL)symlinkAllFiles:(NSArray *)filePaths pathDepth:(int)depth inDirectory:(NSString *)targetPath;

@end

@implementation FakeProjectBuilder

- (void)buildFakeResourcesAtPath:targetPath 
                  forProjectPath:(NSString *)projectPath 
              targetResourceName:(FResourceName *)resName
{
    projectPath = [[projectPath stringByStandardizingPath] stringByResolvingSymlinksInPath];
    NSString *manifest = [projectPath stringByAppendingPathComponent:@"AndroidManifest.xml"];

    NSFileManager *manager = [NSFileManager defaultManager];

    NSLog(@"building index...");
    FResourceIndex *index = [[FResourceIndex alloc] init];
    [index indexResourcesAtProjectPath:projectPath];
    NSLog(@"done.");

    NSMutableSet *visitedFiles = [[NSMutableSet alloc] init];
    NSMutableSet *visitedResourceNames = [[NSMutableSet alloc] init];
    NSMutableArray *pendingFiles = [[index filePathsForResourceName:resName] mutableCopy];

    // manifest can refer to things, too
    [pendingFiles addObject:manifest];
    // as can things in /values
    for (FResourceName *resName in [index resourceNames]) {
        if ([[resName type] isEqual:@"values"]) {
            [pendingFiles addObjectsFromArray:[index filePathsForResourceName:resName]];
        }
    }

    NSLog(@"building dependency list...");
    while ([pendingFiles count] > 0) {
        NSString *fullPath = [pendingFiles objectAtIndex:0];
        [pendingFiles removeObjectAtIndex:0];
        [visitedResourceNames addObject:[fullPath fResourceName]];

        if (![self isExpandable:fullPath]) {
            NSLog(@"visited %@", [fullPath lastPathComponent]);
            continue;
        }
        NSLog(@"expanding %@", [fullPath lastPathComponent]);

        for (FResourceName *resName in [fullPath resourceNamesFromXmlPath]) {
            NSLog(@"   looking at resName %@", resName);
            NSArray *paths = [index filePathsForResourceName:resName];
            if (!paths || [paths count] == 0) {
                NSLog(@"        found no resource paths!");
            } else for (NSString *path in paths) {
                if (![visitedFiles containsObject:path]) {
                    [pendingFiles addObject:path];
                }
            }
        }
    }
    NSLog(@"done. ");
    if (0) {
        NSLog(@"full set of resource names:");
        for (FResourceName *resName in visitedResourceNames) {
            NSLog(@"    %@", resName);
        }
    }

    NSMutableArray *includedFilePaths = [[NSMutableArray alloc] init];
    for (FResourceName *resName in visitedResourceNames) {
        [includedFilePaths addObjectsFromArray:[index filePathsForResourceName:resName]];
    }

    // anything in values automatically gets included, too
    for (FResourceName *resName in [index resourceNames]) {
        if ([[resName type] isEqual:@"values"]) {
            [includedFilePaths addObjectsFromArray:[index filePathsForResourceName:resName]];
        }
    }

    NSLog(@"included files:");
    for (NSString *path in includedFilePaths) {
        // include file size
        NSError *err = nil;
        NSDictionary *attributes = [manager attributesOfItemAtPath:path error:&err];
        int size = -1;
        if (attributes) {
            size = [[attributes objectForKey:NSFileSize] intValue];
        }
        NSLog(@"    %@ %d", path, size);
    }

    [self symlinkAllFiles:includedFilePaths pathDepth:3 inDirectory:targetPath];
}

- (BOOL)isExpandable:(NSString *)fullPath
{
    FResourceName *resName = [fullPath fResourceName];

    if (![[fullPath pathExtension] isEqual:@"xml"]) {
        return NO;
    } else if ([[fullPath lastPathComponent] isEqual:@"AndroidManifest.xml"]) {
        return YES;
    } else if ([[resName type] isEqual:@"xml"]) {
        return YES;
    } else if ([[resName type] isEqual:@"values"]) {
        return YES;
    } else if ([[resName type] isEqual:@"drawable"]) {
        return YES;
    } else if ([[resName type] isEqual:@"layout"]) {
        return YES;
    } else {
        return NO;
    }
}

- (BOOL)symlinkAllFiles:(NSArray *)filePaths pathDepth:(int)depth inDirectory:(NSString *)targetPath
{
    NSFileManager *manager = [NSFileManager defaultManager];
    NSError *err = nil;
    targetPath = [[targetPath stringByStandardizingPath] stringByResolvingSymlinksInPath];
    NSMutableSet *links = [[NSMutableSet alloc] init];

    for (NSString *rawFullPath in filePaths) {
        NSString *fullPath = [[rawFullPath stringByStandardizingPath] stringByResolvingSymlinksInPath];
        NSArray *components = [fullPath pathComponents];
        int count = [components count];
        components = [components subarrayWithRange:NSMakeRange(count - depth, depth)];
        NSString *subPath = [components componentsJoinedByString:@"/"];
        NSString *targetSymlinkPath = [targetPath stringByAppendingPathComponent:subPath];
        NSString *targetSubPath = [targetSymlinkPath stringByDeletingLastPathComponent];;

        if ([links containsObject:targetSymlinkPath]) {
            continue;
        } 

        if (![manager createDirectoryAtPath:targetSubPath withIntermediateDirectories:YES attributes:nil error:&err]) {
            NSLog(@"wat, creating dir %@ failed: %@", targetPath, err);
            return NO;
        }

        NSLog(@"creating symlink from %@ to %@", targetSymlinkPath, fullPath);
        if (![manager createSymbolicLinkAtPath:targetSymlinkPath 
                           withDestinationPath:fullPath
                                         error:&err]) {
            NSLog(@"wat, creating symlink %@ to %@ failed: %@", targetSymlinkPath, fullPath, err);
            return NO;
        }

        [links addObject:targetSymlinkPath];
    }

    return YES;
}

@end

