package enchant.flare

import kotlinx.coroutines.flow.Flow

interface FirebaseAuth {

    suspend fun applyActionCode(code: String)
    suspend fun checkActionCode(code: String): ActionCodeInfo

    suspend fun confirmPasswordReset(code: String, newPassword: String)
    suspend fun fetchSignInProvidersForEmail(email: String): List<AuthProvider>
    val currentUser: FirebaseUser?

    suspend fun sendPasswordResetEmail(email: String, settings: ActionCodeSettings? = null)
    suspend fun sendSignInLinkToEmail(email: String, settings: ActionCodeSettings)
    suspend fun signIn(method: AuthMethod): AdditionalUserInfo
    suspend fun verifyPasswordResetCode(code: String): String

    fun isSignInWithEmailLink(link: String): Boolean
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

class AuthException(val code: Code, val description: String? = null) :
    Exception("Firebase auth operation failed with code ${code.name}: $description") {

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

    data class Apple(
        val ui: Any,
        val requestEmail: Boolean = false,
        val requestName: Boolean = false,
        val locale: String?
    ) : AuthMethod()

    object Annonymous : AuthMethod()

    data class EmailPassword(val email: String, val password: String) : AuthMethod()
    data class EmailLink(val email: String, val link: String, val isLink: Boolean = false) :
        AuthMethod()

    data class Custom(val token: String): AuthMethod()

    data class GitHub(
        val activity: Any? = null,
        val loginHint: String? = null,
        val allowSignUp: Boolean = false,
        val requestEmail: Boolean = false
    ) : AuthMethod()

    data class Google(
        val ui: Any,
        val webClientId: String,
        val requestEmail: Boolean = false,
        val requestProfile: Boolean = false
    ) : AuthMethod()

    data class Twitter(val activity: Any? = null, val locale: String? = null) : AuthMethod()
    data class Yahoo(
        val activity: Any? = null,
        val requestProfile: Boolean = false,
        val requestEmail: Boolean = false,
        val language: String? = null,
        val prompt: String? = null,
        val maxAge: Int? = null
    ) : AuthMethod()
}

enum class AuthProvider { Apple, EmailPassword, EmailLink, GitHub, Google, Twitter, Yahoo }

internal fun toAuthProvider(method: String): AuthProvider =
    when (method) {
        "apple.com" -> AuthProvider.Apple
        "password" -> AuthProvider.EmailPassword
        "emailLink" -> AuthProvider.EmailLink
        "github.com" -> AuthProvider.GitHub
        "google.com" -> AuthProvider.Google
        "twitter.com" -> AuthProvider.Twitter
        "yahoo.com" -> AuthProvider.Yahoo
        else -> error("Unknown auth method encountered: $method")
    }

internal fun toAuthString(provider: AuthProvider): String =
    when (provider) {
        AuthProvider.Apple -> "apple.com"
        AuthProvider.EmailPassword -> "password"
        AuthProvider.EmailLink -> "emailLink"
        AuthProvider.GitHub -> "github.com"
        AuthProvider.Google -> "google.com"
        AuthProvider.Twitter -> "twitter.com"
        AuthProvider.Yahoo -> "yahoo.com"
    }

data class AdditionalUserInfo(
    val profile: Map<String, Any>?,
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
    suspend fun linkMethod(method: AuthMethod): AdditionalUserInfo
    suspend fun reauthenticate(method: AuthMethod): AdditionalUserInfo
    suspend fun reload()
    suspend fun sendEmailVerification(settings: ActionCodeSettings? = null)

    suspend fun unlinkMethod(provider: AuthProvider)
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