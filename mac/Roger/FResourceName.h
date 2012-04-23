//
//  FResourceName.h
//  Roger
//
//  Created by Bill Phillips on 4/22/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface FResourceName : NSObject <NSCopying>

+ (FResourceName *)resourceNameWithType:(NSString *)type name:(NSString *)name;
- (id)initWithType:(NSString *)type name:(NSString *)name;

@property (nonatomic, copy) NSString *type;
@property (nonatomic, copy) NSString *name;

@end
