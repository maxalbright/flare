import enchant.flare.FirebaseApp
import enchant.flare.FirebaseFirestore
import enchant.flare.FirebaseOptions
import kotlin.random.Random
import kotlin.test.BeforeTest


open class FlareTest {
    val useLocal = false //Whether local firebase classes should be used instead of production
    protected val testId: String = Random.nextInt().toString()

    @BeforeTest
    fun initializeFirebase() {
        if (!useLocal && FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initialize(
                context, null,
                options
                    ?: error("Need to specify [options] in FlareConfig.kt files for running production tests")
            )
        }
        FirebaseFirestore.instance
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