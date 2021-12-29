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

expect class FirebaseApp {
    val name: String
    val options: FirebaseOptions

    companion object {
        fun getApps(context: Any?): List<FirebaseApp>
        val instance: FirebaseApp
        fun getInstance(name: String): FirebaseApp
        fun initialize(
            context: Any? = null,
            name: String? = null,
            options: FirebaseOptions? = null
        )
    }
}