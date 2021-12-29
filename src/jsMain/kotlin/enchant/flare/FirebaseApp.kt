package enchant.flare

internal actual class FirebaseAppImpl(app: JsFirebaseApp) : FirebaseApp {
    override val name: String = app.name
    override val options: FirebaseOptions = toFirebaseOptions(app.options)
}

private fun toFirebaseOptions(options: JsFirebaseOptions): FirebaseOptions =
    FirebaseOptions(
        apiKey = options.apiKey!!,
        applicationId = options.appId!!,
        databaseUrl = options.databaseURL,
        gcmSenderId = options.messagingSenderId,
        projectId = options.projectId!!,
        gaTrackingId = options.measurementId,
        storageBucket = options.storageBucket,
        authDomain = options.authDomain
    )

private class JsFirebaseOptionsImpl(options: FirebaseOptions) : JsFirebaseOptions {
    override val apiKey: String = options.apiKey
    override val appId: String = options.applicationId
    override val authDomain: String? = options.authDomain
    override val databaseURL: String? = options.databaseUrl
    override val measurementId: String? = options.gaTrackingId
    override val messagingSenderId: String? = options.gcmSenderId
    override val projectId: String? = options.projectId
    override val storageBucket: String? = options.storageBucket
}

private fun toJsOptions(options: FirebaseOptions): JsFirebaseOptions =
    JsFirebaseOptionsImpl(options)

internal actual fun initializeApp(
    context: Any?,
    name: String?,
    options: FirebaseOptions?
) {
    initializeApp(toJsOptions(options!!), name)
}

internal actual val appInstance: FirebaseApp by lazy { FirebaseAppImpl(getApp(null)) }
internal actual fun getApps(context: Any?): List<FirebaseApp> = getApps().map { FirebaseAppImpl(it) }

internal actual fun getAppInstance(name: String): FirebaseApp = FirebaseAppImpl(getApp(name))