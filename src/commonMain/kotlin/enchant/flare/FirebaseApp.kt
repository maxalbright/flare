package enchant.flare

data class FirebaseOptions(
    val apiKey: String,
    val applicationId: String,
    val databaseUrl: String,
    val gcmSenderId: String,
    val projectId: String,
    val storageBucket: String
)

interface FirebaseApp {
    val name: String
    val options: FirebaseOptions
    fun enableAutomaticResourceManagement(enabled: Boolean)
}