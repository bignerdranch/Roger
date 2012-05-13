//
//  FIntent.h
//  Roger
//
//  Created by Bill Phillips on 5/12/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

extern NSString * const kFIntent_Broadcast;
extern NSString * const kFIntent_Activity;
extern NSString * const kFIntent_Service;

@interface FIntent : NSObject

@property (nonatomic, copy) NSString *action;
@property (nonatomic, copy) NSString *data;
@property (nonatomic, copy) NSString *type;

-(id)initWithAction:(NSString *)action type:(NSString *)type;
-(id)initBroadcastWithAction:(NSString *)action;

-(void)setBroadcast;
-(void)setActivity;
-(void)setService;

-(NSDictionary *)extras;

-(void)setExtra:(NSString *)key string:(NSString *)string;
-(void)setExtra:(NSString *)key number:(NSNumber *)number;

-(void)addCategory:(NSString *)category;

-(NSDictionary *)simpleRepresentation;
-(NSData *)json;

@end
