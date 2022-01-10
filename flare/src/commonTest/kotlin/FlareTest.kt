import enchant.flare.FirebaseApp
import enchant.flare.FirebaseOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
open class FlareTest {
    val useLocal = true //Whether local firebase classes should be used instead of production
    protected val testId: String = Random.nextInt().toString()

    init {
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