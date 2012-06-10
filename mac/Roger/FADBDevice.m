//
//  FADBDevice.m
//  Roger
//
//  Created by Bill Phillips on 5/13/12.
//  Copyright (c) 2012 Big Nerd Ranch. All rights reserved.
//

#import "FADBDevice.h"
#import "FADBConnection.h"

@interface FADBDevice ()

@property (nonatomic, weak, readwrite) FADBConnection *connection;

@end

@implementation FADBDevice

@synthesize serial=_serial;
@synthesize externalStoragePath=_externalStoragePath;
@synthesize clientId=_clientId;
@synthesize connection=_connection;

-(id)initWithSerial:(NSString *)serial storagePath:(NSString *)storagePath clientId:(NSString *)clientId
         connection:(FADBConnection *)connection
{
    if ((self = [super init])) {
        _serial = [serial copy];
        _externalStoragePath = [storagePath copy];
        _clientId = [clientId copy];
        self.connection = connection;
    }
    
    return self;
}

-(NSString *)description
{
    return [NSString stringWithFormat:@"<ADBDevice serial:%@ storage:%@ clientId:%@>", 
           self.serial, self.externalStoragePath, self.clientId];
}

@end
