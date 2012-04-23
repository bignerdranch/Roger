//
//  FResourceName.m
//  Roger
//
//  Created by Bill Phillips on 4/22/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FResourceName.h"

@implementation FResourceName

@synthesize type;
@synthesize name;

+ (FResourceName *)resourceNameWithType:(NSString *)type name:(NSString *)name
{
    return [[FResourceName alloc] initWithType:type name:name];
}

- (FResourceName *)initWithType:(NSString *)type name:(NSString *)name
{
    if ((self = [super init])) {
        [self setType:type];
        [self setName:name];
    }

    return self;
}

- (NSString *)description
{
    return [NSString stringWithFormat:@"%@:%@", [self type], [self name]];
}

- (NSArray *)asArray
{
    return [NSArray arrayWithObjects:[self type], [self name], nil];
}

- (NSUInteger)hash
{
    return [[self asArray] hash];
}

- (BOOL)isEqual:(NSObject *)other
{
    if (![other isKindOfClass:[self class]]) {
        return NO;
    } else {
        FResourceName *fOther = (FResourceName *)other;
        return [[self asArray] isEqual:[fOther asArray]];
    }
}

- (id)copyWithZone:(NSZone *)zone
{
    return [[FResourceName allocWithZone:zone] initWithType:[self type] name:[self name]];
}

@end
