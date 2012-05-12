//
//  NSString+Regexen.m
//  Roger
//
//  Created by Bill Phillips on 4/28/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "NSString+Regexen.h"

@implementation NSString (Regexen)

- (NSString *)stringByReplacingPattern:(NSString *)pattern 
                          withTemplate:(NSString *)replacement
{
    NSError *err = nil;
    NSRegularExpression *regex = [NSRegularExpression
        regularExpressionWithPattern:pattern
                             options:0
                               error:&err];
    return [regex stringByReplacingMatchesInString:self
                                           options:0
                                             range:NSMakeRange(0, [self length])
                                      withTemplate:replacement];
}

- (NSArray *)stringsFromFirstMatchOfPattern:(NSString *)pattern
{
    NSError *err = nil;
    NSRegularExpression *regex = [NSRegularExpression
        regularExpressionWithPattern:pattern
                             options:0
                               error:&err];
    NSArray *matches = [regex matchesInString:self 
                                      options:0
                                        range:NSMakeRange(0, [self length])];
    if ([matches count] < 1) {
        return nil;
    }

    NSTextCheckingResult *match = [matches objectAtIndex:0];

    NSMutableArray *results = [[NSMutableArray alloc] init];
    for (int i = 1; i < [match numberOfRanges]; i++) {
        [results addObject:[self substringWithRange:[match rangeAtIndex:i]]];
    }

    return results;
}

@end
