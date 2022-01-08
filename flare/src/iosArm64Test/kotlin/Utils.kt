import kotlinx.coroutines.*
import platform.Foundation.*

actual val context: Any? = null

actual fun runTest(test: suspend CoroutineScope.() -> Unit): Unit = runBlocking {
    val testRun = MainScope().async { test() }
    while (testRun.isActive) {
        NSRunLoop.mainRunLoop.runMode(NSDefaultRunLoopMode, NSDate.create(1.0, NSDate())
        )
        yield()
    }
    testRun.await()
}