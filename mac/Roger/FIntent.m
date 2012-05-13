//
//  FIntent.m
//  Roger
//
//  Created by Bill Phillips on 5/12/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FIntent.h"
#import "CJSONSerializer.h"

NSString * const kFIntent_Broadcast = @"broadcast";
NSString * const kFIntent_Activity = @"activity";
NSString * const kFIntent_Service = @"service";

NSString * const kJSONAction = @"action";
NSString * const kJSONData = @"data";
NSString * const kJSONCategories = @"categories";
NSString * const kJSONType = @"type";
NSString * const kJSONExtras = @"extras";

@interface FIntent ()
    
@property (nonatomic, strong) NSMutableDictionary *extraStore;
@property (nonatomic, strong) NSMutableArray *categories;

-(id)objectOrNSNull:(id)object;

@end

@implementation FIntent

@synthesize action=_action;
@synthesize data=_data;
@synthesize categories=_categories;
@synthesize type=_type;

@synthesize extraStore;

-(id)initWithAction:(NSString *)action type:(NSString *)type
{
    if ((self = [super init])) {
        self.type = type;
        self.action = action;

        self.extraStore = [[NSMutableDictionary alloc] init];
        self.categories = [[NSMutableArray alloc] init];
    }

    return self;
}

-(id)initBroadcastWithAction:(NSString *)action
{
    return [self initWithAction:action type:kFIntent_Broadcast];
}

-(void)setBroadcast
{
    self.type = kFIntent_Broadcast;
}

-(void)setActivity
{
    self.type = kFIntent_Activity;
}

-(void)setService
{
    self.type = kFIntent_Service;
}

-(void)setExtra:(NSString *)key string:(NSString *)string
{
    [self.extraStore setObject:string forKey:key];
}

-(void)setExtra:(NSString *)key number:(NSNumber *)number
{
    [self.extraStore setObject:number forKey:key];
}

-(void)addCategory:(NSString *)category
{
    [self.categories addObject:category];
}

-(NSDictionary *)extras
{
    return self.extraStore;
}

-(id)objectOrNSNull:(id)object
{
    return object ? object : [NSNull null];
}

-(NSDictionary *)simpleRepresentation
{
    return [NSDictionary dictionaryWithObjectsAndKeys:
        [self objectOrNSNull:self.action], kJSONAction, 
        [self objectOrNSNull:self.data], kJSONData, 
        [self objectOrNSNull:self.categories], kJSONCategories, 
        [self objectOrNSNull:self.type], kJSONType, 
        [self objectOrNSNull:self.extras], kJSONExtras, 
        nil];
}

-(NSData *)json
{
    NSDictionary *representation = [self simpleRepresentation];
    NSError *error = nil;
    NSData *json = [[CJSONSerializer serializer]
        serializeObject:representation error:&error];

    if (!json) {
        NSLog(@"failed to serialize intent to JSON: %@", error);
    }

    return json;
}

- (id)copyWithZone:(NSZone *)zone
{
    FIntent *new = [[FIntent allocWithZone:zone]
                            initWithAction:[self action]
                                      type:[self type]];
    new.data = self.data;

    for (NSString *key in [self.extraStore keyEnumerator]) {
        NSObject *value = [self.extraStore objectForKey:key];
        if ([value isKindOfClass:[NSString class]]) {

            [new setExtra:key string:(NSString *)value];
        } else if ([value isKindOfClass:[NSNumber class]]) {
            [new setExtra:key number:(NSNumber *)value];
        }
    }

    for (NSString *category in self.categories) {
        [new addCategory:category];
    }

    return new;
}

@end
