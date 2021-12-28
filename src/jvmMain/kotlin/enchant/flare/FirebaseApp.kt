package enchant.flare

import com.google.firebase.FirebaseApp as JvmFirebaseApp
import com.google.firebase.FirebaseOptions as JvmFirebaseOptions

internal actual class FirebaseAppImpl(app: JvmFirebaseApp) : FirebaseApp {
    override val name: String = app.name
    override val options: FirebaseOptions = toFirebaseOptions(app.options)
}

private fun toFirebaseOptions(options: JvmFirebaseOptions): FirebaseOptions =
    FirebaseOptions(
        apiKey = "admin",
        applicationId = options.serviceAccountId,
        databaseUrl = options.databaseUrl,
        projectId = options.projectId,
        storageBucket = options.storageBucket,
    )

private fun toJvmOptions(options: FirebaseOptions): JvmFirebaseOptions =
    com.google.firebase.FirebaseOptions.builder().apply {
        setServiceAccountId(options.applicationId)
        setDatabaseUrl(options.databaseUrl)
        setProjectId(options.projectId)
        setStorageBucket(options.gcmSenderId)
    }.build()

internal actual val appInstance: FirebaseApp by lazy { FirebaseAppImpl(JvmFirebaseApp.getInstance()) }
internal actual fun getAppInstance(name: String): FirebaseApp =
    FirebaseAppImpl(JvmFirebaseApp.getInstance(name))

internal actual fun getApps(context: Any?): List<FirebaseApp> =
    JvmFirebaseApp.getApps().map { FirebaseAppImpl(it) }


actual fun initializeApp(
    context: Any?,
    name: String?,
    options: FirebaseOptions?
) {
    when (true) {
        name == null && options == null -> JvmFirebaseApp.initializeApp()
        name == null -> JvmFirebaseApp.initializeApp(toJvmOptions(options!!))
        else -> JvmFirebaseApp.initializeApp(toJvmOptions(options!!), name)
    }
}