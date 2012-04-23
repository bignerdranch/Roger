//
//  FResourceIndex.m
//  Roger
//
//  Created by Bill Phillips on 4/22/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FResourceIndex.h"
#import "NSString+Resources.h"

@interface FResourceIndex ()

@property (nonatomic, strong) NSMutableDictionary *index;

- (void)stashFullPath:(NSString *)fullPath;

@end

@implementation FResourceIndex

@synthesize index;

- (id)init
{
    if ((self = [super init])) {
        [self setIndex:[[NSMutableDictionary alloc] init]];
    }

    return self;
}

- (void)indexResourcesAtProjectPath:(NSString *)projectPath
{
    NSFileManager *manager = [NSFileManager defaultManager];

    NSString *resourcePath = [projectPath stringByAppendingPathComponent:@"res"];
    NSDirectoryEnumerator *paths = [manager enumeratorAtPath:resourcePath];
    NSString *path;

    while ((path = [paths nextObject])) {
        NSString *fullPath = [resourcePath stringByAppendingPathComponent:path];
        if ([fullPath isResourcePath]) {
            [self stashFullPath:fullPath];
        }
    }
}

- (NSArray *)filePathsForResourceName:(FResourceName *)resName
{
    return [[self index] objectForKey:resName];
}

- (void)log
{
    NSLog(@"index contents - ");
    for (FResourceName *resName in [[self index] allKeys]) {
        NSLog(@"    %@:", resName);
        for (NSString *fullPath in [[self index] objectForKey:resName]) {
            NSLog(@"            %@", fullPath);
        }
    }
}

- (void)stashFullPath:(NSString *)fullPath
{
    FResourceName *resName = [fullPath fResourceName];

    NSMutableArray *nameEntries = [[self index] objectForKey:resName];

    if (!nameEntries) {
        nameEntries = [[NSMutableArray alloc] init];
        [[self index] setObject:nameEntries forKey:resName];
    }

    [nameEntries addObject:fullPath];
}

- (NSArray *)resourceNames 
{
    return [[self index] allKeys];
}

@end
