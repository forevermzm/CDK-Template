package cdk

import software.amazon.awscdk.core.App
import software.amazon.awscdk.core.Environment
import software.amazon.awscdk.core.StackProps

// Must exists in CodeCommit before this CDK runs
private const val CODECOMMIT_REPO_NAME = "pipeline"

fun main(args: Array<String>) {
    val app = App()

    val stage = AppStage.getCurrentStage()

    val lambdaStack = LambdaStack(app, "LambdaStack")
    val lambdaCode = lambdaStack.buildStack()

    val pipelineStack = PipelineStack(app, "PipelineStack", StackProps.builder()
        .env(Environment.builder()
            .account(stage.accountId)
            .region(stage.region)
            .build())
        .build())
//    val pipelineStack = PipelineStack(app, "PipelineStack", null)
    pipelineStack.buildStack(lambdaCode, CODECOMMIT_REPO_NAME)

    CdkAppStack(app, "AppStack", StackProps.builder()
        // If you don't specify 'env', this stack will be environment-agnostic.
        // Account/Region-dependent features and context lookups will not work,
        // but a single synthesized template can be deployed anywhere.

        // Uncomment the next block to specialize this stack for the AWS Account
        // and Region that are implied by the current CLI configuration.
        /*
        .env(Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build())
        */

        // Uncomment the next block if you know exactly what Account and Region you
        // want to deploy the stack to.

//        .env(Environment.builder()
//            .account(stage.accountId)
//            .region(stage.region)
//            .build())

        // For more information, see https://docs.aws.amazon.com/cdk/latest/guide/environments.html
        .build())

    app.synth()
}