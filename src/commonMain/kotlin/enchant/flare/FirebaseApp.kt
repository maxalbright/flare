package enchant.flare

data class FirebaseOptions(
    val apiKey: String,
    val applicationId: String,
    val databaseUrl: String? = null,
    val gcmSenderId: String? = null,
    val projectId: String? = null,
    val gaTrackingId: String? = null,
    val storageBucket: String? = null,

    //JS
    val authDomain: String? = null,

    //iOS
    val bundleId: String? = null,
    val clientId: String? = null
)

interface FirebaseApp {
    val name: String
    val options: FirebaseOptions

    companion object {
        val instance: FirebaseApp = appInstance
        val apps: List<FirebaseApp> get () = getApps() as List<FirebaseApp>
        fun getInstance(name: String): FirebaseApp = getAppInstance(name)
        fun initialize(
            context: Any? = null,
            name: String? = null,
            options: FirebaseOptions? = null
        ): Unit = initializeApp(context, name, options)
    }
}

internal expect class FirebaseAppImpl : FirebaseApp

expect fun initializeApp(
    context: Any? = null,
    name: String? = null,
    options: FirebaseOptions? = null
)

internal expect val appInstance: FirebaseApp
internal expect fun getApps(context: Any? = null): List<FirebaseApp>
internal expect fun getAppInstance(name: String): FirebaseApp