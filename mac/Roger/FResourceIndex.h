//
//  ResourceIndex.h
//  Roger
//
//  Created by Bill Phillips on 4/22/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@class FResourceName;

@interface FResourceIndex : NSObject

- (void)log;
- (void)indexResourcesAtProjectPath:(NSString *)projectPath;
- (NSArray *)filePathsForResourceName:(FResourceName *)resName;
- (NSArray *)resourceNames;


@end
