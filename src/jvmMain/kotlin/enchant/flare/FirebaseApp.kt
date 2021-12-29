package enchant.flare

import com.google.firebase.FirebaseApp as JvmFirebaseApp
import com.google.firebase.FirebaseOptions as JvmFirebaseOptions

actual class FirebaseApp(app: JvmFirebaseApp) {
    actual val name: String = app.name
    actual val options: FirebaseOptions = toFirebaseOptions(app.options)

    actual companion object {
        private val instance: FirebaseApp by lazy { FirebaseApp(JvmFirebaseApp.getInstance()) }

        actual fun getApps(context: Any?): List<FirebaseApp> =
            JvmFirebaseApp.getApps().map { FirebaseApp(it) }

        actual fun getInstance(name: String?): FirebaseApp =
            if (name == null) instance else FirebaseApp(JvmFirebaseApp.getInstance(name))

        actual fun initialize(context: Any?, name: String?, options: FirebaseOptions?) {
            when (true) {
                name == null && options == null -> JvmFirebaseApp.initializeApp()
                name == null -> JvmFirebaseApp.initializeApp(toJvmOptions(options!!))
                else -> JvmFirebaseApp.initializeApp(toJvmOptions(options!!), name)
            }
        }
    }
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
    JvmFirebaseOptions.builder().apply {
        setServiceAccountId(options.applicationId)
        setDatabaseUrl(options.databaseUrl)
        setProjectId(options.projectId)
        setStorageBucket(options.gcmSenderId)
    }.build()