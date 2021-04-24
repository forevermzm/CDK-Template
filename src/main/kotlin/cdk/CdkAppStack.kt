package cdk

import software.amazon.awscdk.core.Construct
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.core.StackProps

class CdkAppStack(scope: Construct, id: String, props: StackProps) : Stack(scope, id, props)