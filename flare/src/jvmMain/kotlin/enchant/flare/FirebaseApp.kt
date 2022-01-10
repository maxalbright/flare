package enchant.flare

actual class FirebaseApp() {
    actual val name: String = TODO()
    actual val options: FirebaseOptions = TODO()

    actual companion object {
        actual val instance: FirebaseApp = TODO()

        actual fun getApps(context: Any?): List<FirebaseApp> =
            TODO()

        actual fun getInstance(name: String): FirebaseApp =
            TODO()

        actual fun initialize(context: Any?, name: String?, options: FirebaseOptions?): Unit = TODO()
    }
}