package enchant.flare

import kotlinx.coroutines.flow.Flow

interface FirebaseAuth {

    suspend fun applyActionCode(code: String)
    suspend fun checkActionCode(code: String): ActionCodeInfo

    suspend fun confirmPasswordReset(code: String, newPassword: String)
    suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult
    suspend fun fetchSignInProvidersForEmail(email: String): List<AuthProvider>
    val currentUser: FirebaseUser?

    suspend fun sendPasswordResetEmail(email: String, settings: ActionCodeSettings? = null)
    suspend fun sendSignInLinkToEmail(email: String, settings: ActionCodeSettings)
    suspend fun signInAnonymously(): AuthResult
    suspend fun signIn(method: AuthMethod): AuthResult
    suspend fun signIn(credential: AuthCredential): AuthResult
    suspend fun signInWithCustomToken(token: String): AuthResult
    suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult
    suspend fun signInWithEmailLink(email: String, link: String)
    suspend fun verifyPasswordResetCode(code: String): String
    fun signOut()

    val config: Config

    interface Config {
        var settings: FirebaseAuthSettings

        data class FirebaseAuthSettings(
            val appVerificationDisabledForTesting: Boolean = false,
        )

        fun onAuthStateChange(): Flow<Unit>
        fun onIdTokenChange(): Flow<Unit>
        fun useAppLanguage()
        fun useEmulator(host: String, port: Int)

        val tenantId: String?
        val languageCode: String?
    }

    companion object {
        val instance: FirebaseAuth = firebaseAuthInstance
        fun getInstance(app: FirebaseApp) = getAuthInstance(app)
    }
}

class FirebaseAuthException(val code: Code) :
    Exception("Firebase auth operation failed with code: $code") {

    enum class Code {
        ActionCode,
        Email,
        InvalidCredentials,
        InvalidUser,
        MultiFactor,
        RecentLoginRequired,
        UserCollision,
        AuthWeb,
        WeakPassword,
        Unknown
    }
}

sealed class ActionCodeInfo {


    data class PasswordReset(val email: String) : ActionCodeInfo()
    class SignInWithEmailLink : ActionCodeInfo()
    data class RevertSecondFactorAddition(
        val email: String,
    ) : ActionCodeInfo()

    data class RecoverEmail(val email: String, val previousEmail: String) : ActionCodeInfo()
    data class VerifyBeforeChangeEmail(
        val email: String,
        val previousEmail: String
    ) : ActionCodeInfo()

    data class VerifyEmail(val email: String) : ActionCodeInfo()
}

sealed class AuthMethod {
    data class AppleAndroid(
        val params: Map<String, String> = emptyMap(),
        val scopes: List<String> = emptyList()
    ) : AuthMethod()

    data class AppleiOS(val idToken: String, val rawNonce: String): AuthMethod()

    data class EmailPassword(val email: String, val password: String) : AuthMethod()
    data class EmailLink(val email: String, val emailLink: String, val isLink: Boolean = false) :
        AuthMethod()

    data class Facebook(val accessToken: String) : AuthMethod()
    data class Github(val token: String) : AuthMethod()
    data class Google(val idToken: String, val accessToken: String) : AuthMethod()

    data class Phone(val verificationId: String, val smsCode: String) : AuthMethod()
    data class PlayGames(val serverAuthCode: String) : AuthMethod()
    data class Twitter(val token: String, val secret: String) : AuthMethod()
    data class Yahoo(
        val params: Map<String, String> = emptyMap(),
        val scopes: List<String> = emptyList()
    ) : AuthMethod()
}

enum class AuthProvider { Apple, EmailPassword, EmailLink, Facebook, GitHub, Google, Phone, PlayGames, Twitter, Yahoo }

internal fun toAuthProvider(method: String): AuthProvider =
    when (method) {
        "apple.com" -> AuthProvider.Apple
        "password" -> AuthProvider.EmailPassword
        "emailLink" -> AuthProvider.EmailLink
        "facebook.com" -> AuthProvider.Facebook
        "github.com" -> AuthProvider.GitHub
        "google.com" -> AuthProvider.Google
        "phone" -> AuthProvider.Phone
        "playgames.google.com" -> AuthProvider.PlayGames
        "twitter.com" -> AuthProvider.Twitter
        "yahoo.com" -> AuthProvider.Yahoo
        else -> error("Unknown auth method encountered: $method")
    }

internal fun toAuthString(provider: AuthProvider): String =
    when (provider) {
        AuthProvider.Apple -> "apple.com"
        AuthProvider.EmailPassword -> "password"
        AuthProvider.EmailLink -> "emailLink"
        AuthProvider.Facebook -> "facebook.com"
        AuthProvider.GitHub -> "github.com"
        AuthProvider.Google -> "google.com"
        AuthProvider.Phone -> "phone"
        AuthProvider.PlayGames -> "playgames.google.com"
        AuthProvider.Twitter -> "twitter.com"
        AuthProvider.Yahoo -> "yahoo.com"
    }

interface AuthCredential {
    val provider: AuthProvider
}

data class AuthResult(
    val additionUserInfo: AdditionalUserInfo?,
    val credential: AuthCredential?
)

data class AdditionalUserInfo(
    val profile: Map<String, Any>,
    val providerId: String?,
    val username: String?,
    val isNewUser: Boolean,
)

interface FirebaseUser {

    suspend fun delete()
    val displayName: String?
    val email: String?
    suspend fun getIdToken(forceRefresh: Boolean): TokenResult
    val creationTimestamp: Long
    val lastSignInTimestamp: Long
    val phoneNumber: String?
    val photoUrl: String?
    val providerData: List<UserInfo>
    val tenantId: String?
    val uid: String
    val isAnonymous: Boolean
    suspend fun linkWithMethod(method: AuthMethod): AuthResult
    suspend fun linkWithMethod(credential: AuthCredential): AuthResult
    suspend fun reauthenticate(method: AuthMethod): AuthResult
    suspend fun reauthenticate(credential: AuthCredential): AuthResult
    suspend fun reload()
    suspend fun sendEmailVerification(settings: ActionCodeSettings? = null)

    suspend fun unlink(provider: AuthProvider)
    suspend fun updateEmail(email: String)
    suspend fun updatePassword(password: String)
    suspend fun updateProfile(displayName: String? = null, photoUrl: String? = null)
    suspend fun verifyBeforeUpdateEmail(newEmail: String, settings: ActionCodeSettings? = null)


}

data class ActionCodeSettings(
    val androidConfig: AndroidConfig? = null,
    val dynamicLinkDomain: String?,
    val handleCodeInApp: Boolean = false,
    val iOSBundleId: String? = null,
    val url: String? = null
) {
    data class AndroidConfig(
        val androidPackageName: String,
        val installIfNotAvailable: Boolean,
        val minimumVersion: String
    )
}

data class UserInfo(
    val displayName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val providerId: String,
    val uid: String,
    val isEmailVerified: Boolean = false
)

data class TokenResult(
    val authTimestamp: Long,
    val claims: Map<String, Any>,
    val expirationTimestamp: Long,
    val issuedAtTimestamp: Long,
    val signInProvider: AuthProvider?,
    val signInSecondFactor: AuthProvider?,
    val token: String?
)

internal expect val firebaseAuthInstance: FirebaseAuth
internal expect fun getAuthInstance(app: FirebaseApp): FirebaseAuth