//
//  NSString+Resources.h
//  Roger
//
//  Created by Bill Phillips on 4/22/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@class FResourceName;

@interface NSString (Resources)

- (BOOL)isResourcePath;
- (NSString *)resourceQualifiedType;
- (NSString *)resourceType;
- (NSString *)resourceFileName;
- (NSString *)resourceName;
- (FResourceName *)fResourceName;
- (FResourceName *)attributeValueAsFResourceName;
- (NSArray *)resourceNamesFromXmlPath;

@end
