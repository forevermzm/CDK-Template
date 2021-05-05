package cdk

import software.amazon.awscdk.core.Construct
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.core.StackProps
import software.amazon.awscdk.services.codebuild.*
import software.amazon.awscdk.services.codecommit.Repository
import software.amazon.awscdk.services.codepipeline.Artifact
import software.amazon.awscdk.services.codepipeline.Pipeline
import software.amazon.awscdk.services.codepipeline.StageProps
import software.amazon.awscdk.services.codepipeline.actions.CloudFormationCreateUpdateStackAction
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction
import software.amazon.awscdk.services.codepipeline.actions.CodeCommitSourceAction
import software.amazon.awscdk.services.lambda.CfnParametersCode

class PipelineStack(scope: Construct, id: String, props: StackProps?) : Stack(scope, id, props) {
    fun buildStack(lambdaCode: CfnParametersCode, repoName: String) {
        val code = Repository.fromRepositoryName(this, "ImportedRepo", repoName)

        val cdkBuild = PipelineProject.Builder.create(this, "CDKBuild")
            .buildSpec(BuildSpec.fromObject(mapOf(
                "version" to "0.2",
                "phases" to mapOf(
                    "install" to mapOf<String, Any>(
                        "commands" to "npm install aws-cdk"
                    ),
                    "build" to mapOf<String, Any>(
                        "commands" to listOf("./gradlew build",
                            "npx cdk synth -o cdk.out")
                    )
                ),
                "artifacts" to mapOf(
                    "base-directory" to "cdk.out",
                    "files" to listOf("LambdaStack.template.json")
                ))))
            .environment(BuildEnvironment.builder().buildImage(
                LinuxBuildImage.AMAZON_LINUX_2_3).build())
            .build()

        val lambdaBuild = PipelineProject.Builder.create(this, "LambdaBuild")
            .buildSpec(BuildSpec.fromObject(mapOf(
                "version" to "0.2",
                "phases" to mapOf(
                    "install" to mapOf<String, Any>(
                        "runtime-versions" to mapOf("java" to "corretto11")
                    ),
                    "build" to mapOf<String, Any>(
                        "commands" to listOf(
                            "java -version",
                            "./gradlew build",
                            "ls -al build/distributions/"
                        )
                    ),
                    // This is necessary, as code build would compress all
                    // files in the specified folder to a zip, which ends
                    // up a nested zip file
                    "post_build" to mapOf<String, Any>(
                        "commands" to listOf(
                            "/usr/bin/unzip build/distributions/cdk.zip -d build/flat"
                        )
                    )
                ),
                "artifacts" to mapOf(
                    "base-directory" to "build/flat/",
                    "files" to listOf("**/*"),
                ))))
            .environment(BuildEnvironment.builder().buildImage(
                LinuxBuildImage.AMAZON_LINUX_2_3).build())
            .build()

        val sourceOutput = Artifact()
        val cdkBuildOutput = Artifact("CdkBuildOutput")
        val lambdaBuildOutput = Artifact("LambdaBuildOutput")

        Pipeline.Builder
            .create(this, "Pipeline")
                .stages(listOf(
                    StageProps
                        .builder()
                        .stageName("Source")
                        .actions(listOf(
                            CodeCommitSourceAction.Builder.create()
                                .actionName("Source")
                                .repository(code)
                                .output(sourceOutput)
                                .build()))
                        .build(),
                    StageProps.builder()
                        .stageName("Build")
                        .actions(listOf(
                            CodeBuildAction.Builder.create()
                                .actionName("Lambda_Build")
                                .project(lambdaBuild)
                                .input(sourceOutput)
                                .outputs(listOf(lambdaBuildOutput)).build(),
                            CodeBuildAction.Builder.create()
                                .actionName("CDK_Build")
                                .project(cdkBuild)
                                .input(sourceOutput)
                                .outputs(listOf(cdkBuildOutput))
                                .build()))
                        .build(),
                    StageProps.builder()
                        .stageName("Deploy")
                        .actions(listOf(
                            CloudFormationCreateUpdateStackAction.Builder.create()
                                .actionName("Lambda_CFN_Deploy")
                                // Comes from previous defined output
                                .templatePath(cdkBuildOutput.atPath("LambdaStack.template.json"))
                                .adminPermissions(true)
                                .parameterOverrides(lambdaCode.assign(lambdaBuildOutput.s3Location))
                                .extraInputs(listOf(lambdaBuildOutput))
                                .stackName("LambdaDeploymentStack")
                                .build()))
                        .build()))
            .build()
    }
}