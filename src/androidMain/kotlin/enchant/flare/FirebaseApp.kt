package enchant.flare

import android.content.Context
import com.google.firebase.FirebaseApp as AndroidFirebaseApp
import com.google.firebase.FirebaseOptions as AndroidFirebaseOptions

actual class FirebaseApp(val app: AndroidFirebaseApp) {
    actual val name: String = app.name
    actual val options: FirebaseOptions = toFirebaseOptions(app.options)

    actual companion object {
        private val instance: FirebaseApp by lazy { FirebaseApp(AndroidFirebaseApp.getInstance()) }

        actual fun getApps(context: Any?): List<FirebaseApp> =
            AndroidFirebaseApp.getApps(context!! as Context).map { FirebaseApp(it) }

        actual fun getInstance(name: String?): FirebaseApp =
            if (name == null) instance else FirebaseApp(AndroidFirebaseApp.getInstance(name))

        actual fun initialize(context: Any?, name: String?, options: FirebaseOptions?) {
            when (true) {
                name == null && options == null -> AndroidFirebaseApp.initializeApp(context!! as Context)
                name == null -> AndroidFirebaseApp.initializeApp(
                    context!! as Context, toAndroidOptions(options!!)
                )
                else -> AndroidFirebaseApp.initializeApp(
                    context!! as Context, toAndroidOptions(options!!), name
                )
            }
        }
    }
}

private fun toFirebaseOptions(options: AndroidFirebaseOptions): FirebaseOptions =
    FirebaseOptions(
        apiKey = options.apiKey,
        applicationId = options.applicationId,
        databaseUrl = options.databaseUrl,
        gcmSenderId = options.gcmSenderId,
        projectId = options.projectId,
        gaTrackingId = options.gaTrackingId,
        storageBucket = options.storageBucket,
    )

private fun toAndroidOptions(options: FirebaseOptions): AndroidFirebaseOptions =
    AndroidFirebaseOptions.Builder().apply {
        setApiKey(options.apiKey)
        setApplicationId(options.applicationId)
        setDatabaseUrl(options.databaseUrl)
        setProjectId(options.projectId)
        setGaTrackingId(options.gaTrackingId)
        setGcmSenderId(options.gcmSenderId)
        setStorageBucket(options.gcmSenderId)
    }.build()