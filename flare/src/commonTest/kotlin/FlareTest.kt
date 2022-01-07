import enchant.flare.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
open class FlareTest {
    val useLocal = false //Whether local firebase classes should be used instead of production
    protected val testId: String = Random.nextInt().toString()

    public fun runTest(
        context: CoroutineContext = EmptyCoroutineContext,
        dispatchTimeoutMs: Long = 60000L,
        testBody: suspend TestScope.() -> Unit
    ): TestResult { kotlinx.coroutines.test.runTest(context, dispatchTimeoutMs, testBody) }

    @BeforeTest
    fun initializeFirebase() {
        if (!useLocal && FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initialize(
                context, null,
                options
                    ?: error("Need to specify [options] in FlareConfig.kt files for running production tests")
            )
        }
    }
}
/** Included Firebase options for running tests in production, specify in iosTest/kotlin/FlareConfig.kt
 * and androidAndroidTest/kotlin/FlareConfig.kt
 * ```
 * actual val options: FirebaseOptions? = null
 * or
 * actual val options: FirebaseOptions? FirebaseOptions(...)
 * ```
 *
 * */
expect val options: FirebaseOptions?