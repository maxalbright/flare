import kotlinx.coroutines.*

expect val context: Any?

expect fun runTest(test: suspend CoroutineScope.() -> Unit)