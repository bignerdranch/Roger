//
//  NSString+Regexen.h
//  Roger
//
//  Created by Bill Phillips on 4/28/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NSString (Regexen)

- (NSArray *)stringsFromFirstMatchOfPattern:(NSString *)pattern;
- (NSString *)stringByReplacingPattern:(NSString *)pattern 
                          withTemplate:(NSString *)replacement;
@end
