import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi

actual val context: Any? = null

@OptIn(ExperimentalCoroutinesApi::class)
actual fun runTest(test: suspend CoroutineScope.() -> Unit) = kotlinx.coroutines.test.runTest(testBody = test)