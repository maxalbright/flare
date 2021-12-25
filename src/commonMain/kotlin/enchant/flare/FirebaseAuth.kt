package enchant.flare

interface FirebaseAuth {

    suspend fun applyActionCode(code: String)
    suspend fun checkActionCode(code: String): ActionCodeInfo

    suspend fun confirmPasswordReset(code: String, newPassword: String)
    suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult
    suspend fun fetchSignInMethodsForEmail(email: String): List<AuthMethod>
    var currentUser: FirebaseUser?

    fun sendPasswordResetEmail(email: String, settings: ActionCodeSettings? = null)
    fun sendSignInLinkToEmail(email: String, settings: ActionCodeSettings)
    suspend fun signInAnonymously(): AuthResult
    suspend fun signInWithCredential(credential: AuthCredential): AuthResult
    suspend fun signInWithCustomToken(token: String): AuthResult
    suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult
    suspend fun signInWithEmailLink(email: String, link: String)
    suspend fun verifyPasswordResetCode(code: String): String

    val config: Config

    interface Config {
        var settings: FirebaseAuthSettings

        data class FirebaseAuthSettings(
            val forceRecaptchaFlowForTesting: Boolean = false,
            val appVerificationDisabledForTesting: Boolean = false,
            val autoRetrievePhoneNumber: String,
            val autoRetrieveSmsCode: String
        )

        fun onAuthStateChange(listener: () -> Unit): Listener
        fun onIdTokenChange(listener: () -> Unit): Listener

        suspend fun getPendingAuthResult(): AuthResult?
        var tenantId: String?
        var languageCode: String
        fun useAppLanguage()
        fun useEmulator(host: String, port: Int)
    }
}

interface Listener {
    fun remove()
}

sealed class ActionCodeInfo {

    object None : ActionCodeInfo()

    data class VerifyEmail(val email: String) : ActionCodeInfo()
    data class PasswordReset(val email: String) : ActionCodeInfo()
    data class RevertSecondFactorAddition(
        val email: String,
        val displayName: String,
        val enrollmentTimestamp: Long,
        val factorId: String,
        val uid: String
    ) : ActionCodeInfo()

    data class RecoverEmail(val email: String, val previousEmail: String) : ActionCodeInfo()
    data class VerifyBeforeChangeEmail(
        val email: String,
        val previousEmail: String
    ) : ActionCodeInfo()
}

sealed class AuthMethod {
    object EmailPassword : AuthMethod()
    object EmailLink : AuthMethod()
    object Facebook : AuthMethod()
    object GitHub : AuthMethod()
    object Google : AuthMethod()
    data class OAuth(val signInMethod: String) : AuthMethod()
    object Phone : AuthMethod()
    object PlayGames : AuthMethod()
    object Twitter : AuthMethod()
}

sealed class AuthCredential {
    data class Email(val email: String, val password: String, val isLink: Boolean = false) :
        AuthCredential()

    data class Facebook(val accessToken: String) : AuthCredential()
    data class Github(val token: String) : AuthCredential()
    data class Google(val idToken: String, val accessToken: String) : AuthCredential()
    data class OAuth(
        val providerId: String, val idToken: String, val rawNonce: String? = null,
        val accessToken: String
    ) : AuthCredential()

    data class Phone(val verificationId: String, val smsCode: String) : AuthCredential()
    data class PlayGames(val serverAuthCode: String) : AuthCredential()
    data class Twitter(val token: String, val secret: String) : AuthCredential()
}

interface AuthResult {
    val additionUserInfo: AdditionalUserInfo?
    val credential: AuthCredential
}

interface AdditionalUserInfo {
    val profile: Map<String, Any>
    val providerId: String
    val username: String?
    val isNewUser: Boolean
}

interface FirebaseUser {

    suspend fun delete()
    val displayName: String
    val email: String
    suspend fun getIdToken(forceRefresh: Boolean): TokenResult
    val creationTimestamp: Long
    val lastSignInTimestamp: Long
    val phoneNumber: String?
    val photoUrl: String
    val providerData: List<UserInfo>
    val tenantId: String?
    val uid: String
    val isAnonymous: Boolean
    suspend fun linkWithCredential(credential: AuthCredential): AuthResult
    suspend fun reauthenticate(credential: AuthCredential)
    suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult
    suspend fun reload()
    suspend fun sendEmailVerification(settings: ActionCodeSettings? = null)
    suspend fun verifyPhoneNumber(
        requireSmsValidation: Boolean = true,
        phoneNumber: String,
        timeout: Long = 30
    )

    suspend fun unlink(provider: String): AuthResult
    suspend fun updateEmail(email: String)
    suspend fun updatePassword(password: String)
    suspend fun updatePhoneNumber(phoneNumber: String)
    suspend fun updateProfile(displayName: String? = null, photoUrl: String? = null)
    suspend fun verifyBeforeUpdateEmail(newEmail: String, settings: ActionCodeSettings? = null)


}

data class ActionCodeSettings(
    val androidConfig: AndroidConfig? = null,
    val dynamicLinkDomain: String = "",
    val handleCodeInApp: Boolean = false,
    val iOSBundleId: String = "",
    val url: String? = null
) {
    data class AndroidConfig(
        val androidPackageName: String,
        val installIfNotAvailable: Boolean,
        val minimumVersion: String
    )
}

interface UserInfo {
    val displayName: String?
    val email: String?
    val phoneNumber: String?
    val photoUrl: String?
    val providerId: String
    val uid: String
    val isEmailVerified: Boolean
}

interface TokenResult {
    val authTimestamp: Long
    val claims: Map<String, Any>
    val expirationTimestamp: Long
    val issuedAtTimestamp: Long
    val signInProvider: String
    val token: String
}