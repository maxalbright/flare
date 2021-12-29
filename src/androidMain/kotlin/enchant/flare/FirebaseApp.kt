package enchant.flare

import android.content.Context
import com.google.firebase.FirebaseApp as AndroidFirebaseApp
import com.google.firebase.FirebaseOptions as AndroidFirebaseOptions

internal actual class FirebaseAppImpl(app: AndroidFirebaseApp) : FirebaseApp {
    override val name: String = app.name
    override val options: FirebaseOptions = toFirebaseOptions(app.options)
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

internal actual fun initializeApp(
    context: Any?,
    name: String?,
    options: FirebaseOptions?
) {
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

internal actual val appInstance: FirebaseApp by lazy { FirebaseAppImpl(AndroidFirebaseApp.getInstance()) }
internal actual fun getApps(context: Any?): List<FirebaseApp> =
    AndroidFirebaseApp.getApps(context!! as Context).map { FirebaseAppImpl(it) }

internal actual fun getAppInstance(name: String): FirebaseApp =
    FirebaseAppImpl(AndroidFirebaseApp.getInstance(name))