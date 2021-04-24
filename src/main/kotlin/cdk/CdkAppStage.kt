package cdk

sealed class AppStage(
        open val username: String,
        open val accountId: String,
        open val region: String
) {
    companion object {
        fun getCurrentStage() = DeploymentAppStage("app-local-user", "accountId", "us-west-2")
    }

    data class DeploymentAppStage(
        override val username: String,
        override val accountId: String,
        override val region: String
    ) : AppStage(username, accountId, region)

    data class BetaAppStage(
            override val username: String,
            override val accountId: String,
            override val region: String
    ) : AppStage(username, accountId, region)

    data class ProdAppStage(
            override val username: String,
            override val accountId: String,
            override val region: String
    ) : AppStage(username, accountId, region)
}

