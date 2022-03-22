import enchant.flare.FirebaseOptions

/** Included Firebase options for running tests in production, specify in iosTest/kotlin/FlareConfig.kt
 * and androidAndroidTest/kotlin/FlareConfig.kt
 * ```
 * actual val options: FirebaseOptions? = null
 * or
 * actual val options: FirebaseOptions? FirebaseOptions(...)
 * ```
 *
 * */
actual val options: FirebaseOptions?
    get() = null