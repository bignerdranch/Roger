
#import <Cocoa/Cocoa.h>

@class FResourceName;

@interface FakeProjectBuilder : NSObject

- (void)buildFakeResourcesAtPath:targetPath 
                  forProjectPath:(NSString *)projectPath 
              targetResourceName:(FResourceName *)resName;

@end
