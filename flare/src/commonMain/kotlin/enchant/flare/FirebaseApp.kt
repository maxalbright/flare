package enchant.flare

data class FirebaseOptions(
    val apiKey: String,
    val appId: String,
    val databaseUrl: String? = null,
    val gcmSenderId: String? = null,
    val projectId: String? = null,
    val measurementId: String? = null,
    val storageBucket: String? = null,

    //Only for JS
    val authDomain: String? = null,

    //Only for iOS
    val bundleId: String? = null,
    val clientId: String? = null,
    val androidClientId: String? = null
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