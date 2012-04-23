//
//  NSString+Resources.m
//  Roger
//
//  Created by Bill Phillips on 4/22/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "NSString+Resources.h"
#import "FResourceName.h"

@implementation NSString (Resources)


- (BOOL)isResourcePath
{
    if ([self rangeOfString:@".svn"].location != NSNotFound) 
        return NO;

    NSArray *components = [self pathComponents];
    int count = [components count];
    if (count < 3) {
        return NO;
    }

    NSString *grandparent = [components objectAtIndex:count - 3];

    return [grandparent isEqual:@"res"];
}

- (NSString *)resourceQualifiedType
{
    NSArray *components = [self pathComponents];
    int count = [components count];
    if (count < 2) return @"";

    return (NSString *)[components objectAtIndex:count - 2];
}

- (NSString *)resourceType
{
    NSArray *components = [[self resourceQualifiedType] componentsSeparatedByString:@"-"];

    return (NSString *)[components objectAtIndex:0];
}

- (NSString *)resourceFileName
{
    NSArray *components = [self pathComponents];
    return [components objectAtIndex:[components count] - 1];
}

- (NSString *)resourceName
{
    // twice, for 9patches
    return [[[self resourceFileName] stringByDeletingPathExtension] stringByDeletingPathExtension];
}

- (FResourceName *)fResourceName
{
    NSString *type = [self resourceType];
    NSString *name = [self resourceName];

    return [FResourceName resourceNameWithType:type name:name];
}

- (NSArray *)resourceNamesFromXmlPath
{
    NSMutableArray *results = [[NSMutableArray alloc] init];

    NSURL *url = [NSURL fileURLWithPath:self];
    NSError *err = nil;
    NSXMLDocument *doc = [[NSXMLDocument alloc] initWithContentsOfURL:url options:0 error:&err];

    if (err) {
        NSLog(@"failed to parse xml: %@", err);
        return results;
    }

    NSString *androidPrefix = nil;
    BOOL isResource = NO;

    if ([[doc rootElement] isKindOfClass:[NSXMLElement class]]) {
        // for now we'll just deal with the guaranteed android xml namespace
        NSXMLElement *el = (NSXMLElement *)[doc rootElement];
        androidPrefix = [el resolvePrefixForNamespaceURI:@"http://schemas.android.com/apk/res/android"];
    }

    if (!androidPrefix && [self rangeOfString:@"/values"].location != NSNotFound) {
        isResource = YES;
        NSLog(@"unable to find android namespace; entering resource mode isResource:%d", isResource);
    } else if (!androidPrefix) {
        NSLog(@"unable to find android namespace, and we're not a values file. abort");
        return results;
    }


    if (isResource) for (NSXMLNode *node in [doc nodesForXPath:@"//*" error:&err]) {
        NSString *withNewline = [[node stringValue] stringByAppendingString:@"\n"];

        FResourceName *resName = [withNewline attributeValueAsFResourceName];
        if (resName) {
            [results addObject:resName];
        }
    }
 
    for (NSXMLNode *attribute in [doc nodesForXPath:@"//@*" error:&err]) {
        if (!([[attribute prefix] isEqual:androidPrefix] ||
                [[attribute name] isEqual:@"style"] ||
                [[attribute name] isEqual:@"layout"])) {
            continue;
        }

        //NSLog(@"attribute name:%@ stringValue:%@", [attribute name], [attribute stringValue]);
        NSString *withNewline = [[attribute stringValue] stringByAppendingString:@"\n"];
        FResourceName *resName = [withNewline attributeValueAsFResourceName];
        if (resName) {
            [results addObject:resName];
        }
    }

    return results;
}

- (FResourceName *)attributeValueAsFResourceName
{
    NSError *err = nil;
    NSRegularExpression *regex = [NSRegularExpression
        regularExpressionWithPattern:@"^@\\+?([a-z_:]+)/([a-z._0-9]+)$"
                             options:NSRegularExpressionCaseInsensitive
                               error:&err];
    NSArray *matches = [regex matchesInString:self 
                                      options:NSRegularExpressionCaseInsensitive 
                                        range:NSMakeRange(0, [self length])];
    // should only be one of these
    if ([matches count] != 1) {
        return nil;
    }

    NSMutableArray *results = [[NSMutableArray alloc] init];
    for (NSTextCheckingResult *match in matches) {
        for (int i = 1; i < [match numberOfRanges]; i++) {
            [results addObject:[self substringWithRange:[match rangeAtIndex:i]]];
        }
    }
    
    if ([results count] != 2) {
        return nil;
    }

    // grab our type and name
    NSString *type = [results objectAtIndex:0];
    NSString *name = [results objectAtIndex:1];

    // if it has : in it, it's not in our local
    // namespace so reject it. may extend for libraries
    // at a later point, but not today
    if ([type rangeOfString:@":"].location != NSNotFound) {
        return nil;
    }

    return [FResourceName resourceNameWithType:type name:name];
}

@end
