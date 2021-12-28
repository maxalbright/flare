package enchant.flare

import kotlin.js.Promise

external fun deleteApp(app: JsFirebaseApp): Promise<Unit>
external fun getApp(name: String?): JsFirebaseApp
external fun getApps(): Array<JsFirebaseApp>
external fun initializeApp(options: JsFirebaseOptions, name: String?): JsFirebaseApp
external fun initializeApp(options: JsFirebaseOptions, config: JsFirebaseAppSettings?): JsFirebaseApp

@JsName("FirebaseApp")
external interface JsFirebaseApp {
    var automaticDataCollectedEnabled: Boolean
    val name: String
    val options: JsFirebaseOptions
}

@JsName("FirebaseOptions")
external interface JsFirebaseOptions {
    val apiKey: String?
    val appId: String?
    val authDomain: String?
    val databaseURL: String?
    val measurementId: String?
    val messagingSenderId: String?
    val projectId: String?
    val storageBucket: String?
}

@JsName("FirebaseAppSettings")
external interface JsFirebaseAppSettings {
    var automaticDataCollectedEnabled: Boolean?
    var name: String?
}