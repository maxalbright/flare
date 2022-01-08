import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*

actual val context: Any? = InstrumentationRegistry.getInstrumentation().context

@OptIn(ExperimentalCoroutinesApi::class)
actual fun runTest(test: suspend CoroutineScope.() -> Unit) = kotlinx.coroutines.test.runTest(testBody = test)