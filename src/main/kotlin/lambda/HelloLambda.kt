package lambda
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import mu.KotlinLogging

class HelloLambda : RequestHandler<String, String> {
    private val logger = KotlinLogging.logger {}

    override fun handleRequest(input: String, context: Context?): String {
        logger.info("Input is $input")
        return "Hello $input"
    }
}