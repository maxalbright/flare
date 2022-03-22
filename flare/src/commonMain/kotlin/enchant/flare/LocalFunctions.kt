package enchant.flare

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

abstract class LocalFunctions : FirebaseFunctions {

    abstract suspend fun callFunction(name: String, data: Any?): Any?
    override suspend fun call(name: String, data: Any?, timeout: Long?): Any? {
        return if (timeout != null) {
            try {
                withTimeout(timeout) {
                    callFunction(name, data)
                }
            } catch (e: TimeoutCancellationException) {
                throw FunctionsException(
                    FunctionsException.Code.DeadlineExceeded,
                    "Function exceeded timeout: $timeout"
                )
            }
        } else callFunction(name, data)
    }

    override val config: FirebaseFunctions.Config
        get() = object : FirebaseFunctions.Config {
            override fun useEmulator(host: String, port: Int) {
            }
        }
}