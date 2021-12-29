package enchant.flare

import cocoapods.FirebaseCore.FIRApp
import cocoapods.FirebaseCore.FIROptions

actual class FirebaseApp(val app: FIRApp) {
    actual val name: String = app.name
    actual val options: FirebaseOptions = toFirebaseOptions(app.options)

    actual companion object {
        actual val instance: FirebaseApp by lazy { FirebaseApp(FIRApp.defaultApp()!!) }

        actual fun getApps(context: Any?): List<FirebaseApp> =
            FIRApp.allApps?.values?.map { FirebaseApp(it as FIRApp) } ?: emptyList()

        actual fun getInstance(name: String): FirebaseApp =
            FirebaseApp(FIRApp.appNamed(name)!!)

        actual fun initialize(context: Any?, name: String?, options: FirebaseOptions?) {
            when (true) {
                name == null && options == null -> FIRApp.configure()
                name == null -> FIRApp.configureWithOptions(toFIROptions(options!!))
                else -> FIRApp.configureWithName(name, toFIROptions(options!!))
            }
        }
    }
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