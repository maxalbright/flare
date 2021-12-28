package enchant.flare

import cocoapods.FirebaseCore.FIRApp
import cocoapods.FirebaseCore.FIROptions

internal actual class FirebaseAppImpl(app: FIRApp) : FirebaseApp {
    override val name: String = app.name
    override val options: FirebaseOptions = toFirebaseOptions(app.options)
}

private fun toFirebaseOptions(options: FIROptions): FirebaseOptions =
    FirebaseOptions(
        apiKey = options.APIKey!!,
        applicationId = options.googleAppID,
        databaseUrl = options.databaseURL,
        gcmSenderId = options.GCMSenderID,
        projectId = options.projectID,
        gaTrackingId = options.trackingID,
        storageBucket = options.storageBucket,
        bundleId = options.bundleID,
        clientId = options.clientID
    )

private fun toFIROptions(options: FirebaseOptions): FIROptions =
    FIROptions().apply {
        setAPIKey(options.apiKey)
        setGoogleAppID(options.applicationId)
        setDatabaseURL(options.databaseUrl)
        setProjectID(options.projectId)
        setTrackingID(options.gaTrackingId)
        if (options.gcmSenderId != null) setGCMSenderID(options.gcmSenderId!!)
        setStorageBucket(options.gcmSenderId)
        setBundleID(bundleID)
        setClientID(clientID)
    }

actual fun initializeApp(
    context: Any?,
    name: String?,
    options: FirebaseOptions?
) {
    when (true) {
        name == null && options == null -> FIRApp.configure()
        name == null -> FIRApp.configureWithOptions(toFIROptions(options!!))
        else -> FIRApp.configureWithName(name, toFIROptions(options!!))
    }
}

internal actual val appInstance: FirebaseApp by lazy { FirebaseAppImpl(FIRApp.defaultApp()!!) }
internal actual fun getApps(context: Any?): List<FirebaseApp> =
    FIRApp.allApps?.values?.map { FirebaseAppImpl(it as FIRApp) } ?: emptyList()

internal actual fun getAppInstance(name: String): FirebaseApp =
    FirebaseAppImpl(FIRApp.appNamed(name)!!)