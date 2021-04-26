package cdk

import software.amazon.awscdk.core.Construct
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.core.StackProps
import software.amazon.awscdk.services.codedeploy.LambdaDeploymentConfig
import software.amazon.awscdk.services.lambda.CfnParametersCode
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.lambda.Alias
import software.amazon.awscdk.services.codedeploy.LambdaDeploymentGroup

class LambdaStack(scope: Construct, id: String) : Stack(scope, id, null) {
    fun buildStack() : CfnParametersCode {
        val lambdaCode = CfnParametersCode.fromCfnParameters()

        val func = Function.Builder.create(this, "HelloWorldLambdaFunction")
                .code(lambdaCode)
                .handler("lambda.HelloLambda::handleRequest")
                .runtime(Runtime.JAVA_8)
                .description("Hello World Lambda function")
                .build()

        val version= func.currentVersion
        val alias = Alias.Builder.create(this, "LambdaAlias")
                .aliasName("LambdaAlias")
                .version(version).build()

        LambdaDeploymentGroup.Builder.create(this, "DeploymentGroup")
                .alias(alias)
                .deploymentConfig(LambdaDeploymentConfig.LINEAR_10_PERCENT_EVERY_1_MINUTE).build()

        return lambdaCode
    }
}