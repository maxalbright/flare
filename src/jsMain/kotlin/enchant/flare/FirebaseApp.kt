package enchant.flare

actual class FirebaseApp(val app: JsFirebaseApp) {
    actual val name: String = app.name
    actual val options: FirebaseOptions = toFirebaseOptions(app.options)

    actual companion object {
        private val instance: FirebaseApp by lazy { FirebaseApp(getApp(null)) }

        actual fun getApps(context: Any?): List<FirebaseApp> = getApps().map { FirebaseApp(it) }

        actual fun getInstance(name: String?): FirebaseApp =
            if (name == null) instance else FirebaseApp(getApp(name))

        actual fun initialize(context: Any?, name: String?, options: FirebaseOptions?) {
            initializeApp(toJsOptions(options!!), name)
        }
    }
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
