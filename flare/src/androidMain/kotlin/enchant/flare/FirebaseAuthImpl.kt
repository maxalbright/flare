package enchant.flare
import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.firebase.auth.ActionCodeSettings as AndroidCodeSettings
import com.google.firebase.auth.AuthCredential as AndroidAuthCredential
import com.google.firebase.auth.FirebaseAuth as AndroidAuth
import com.google.firebase.auth.FirebaseAuthException as AndroidAuthException
import com.google.firebase.auth.FirebaseUser as AndroidUser


private class FirebaseAuthImpl(private val auth: AndroidAuth) : FirebaseAuth {
    override suspend fun applyActionCode(code: String): Unit = suspendCancellableCoroutine { c ->
        auth.applyActionCode(code).addOnCompleteListener {
            if (it.isSuccessful) c.resume(Unit)
            else authException(it.exception!!)
        }
    }

    override suspend fun checkActionCode(code: String): ActionCodeInfo =
        suspendCancellableCoroutine { c ->
            auth.checkActionCode(code).addOnCompleteListener {
                if (it.isSuccessful) c.resume(
                    toActionCodeInfo(it.result)
                        ?: throw AuthException(AuthException.Code.ActionCode)
                )
                else authException(it.exception!!)
            }
        }

    override suspend fun confirmPasswordReset(code: String, newPassword: String): Unit =
        suspendCancellableCoroutine { c ->
            auth.confirmPasswordReset(code, newPassword).addOnCompleteListener {
                if (it.isSuccessful) c.resume(Unit)
                else authException(it.exception!!)
            }
        }

    override suspend fun fetchSignInProvidersForEmail(email: String): List<AuthProvider> =
        suspendCancellableCoroutine { c ->
            auth.fetchSignInMethodsForEmail(email).addOnCompleteListener {
                if (it.isSuccessful) c.resume(it.result.signInMethods?.map { toAuthProvider(it) }
                    ?: emptyList())
                else authException(it.exception!!)
            }
        }

    override val currentUser: FirebaseUser?
        get() = auth.currentUser?.let {
            FirebaseUserImpl(it, auth)
        }

    override suspend fun sendPasswordResetEmail(
        email: String,
        settings: ActionCodeSettings?
    ): Unit =
        suspendCancellableCoroutine { c ->
            auth.sendPasswordResetEmail(email, settings?.let { toAndroidCodeSettings(settings) })
                .addOnCompleteListener {
                    if (it.isSuccessful) c.resume(Unit)
                    else authException(it.exception!!)
                }
        }

    override suspend fun sendSignInLinkToEmail(email: String, settings: ActionCodeSettings): Unit =
        suspendCancellableCoroutine { c ->
            auth.sendSignInLinkToEmail(email, toAndroidCodeSettings(settings))
                .addOnCompleteListener {
                    if (it.isSuccessful) c.resume(Unit)
                    else authException(it.exception!!)
                }
        }

    override suspend fun signIn(method: AuthMethod): AdditionalUserInfo {
        val credential = toCredentialHolder(method)
        return if (credential.credential == null) {
            toUserInfo(getAltResult(credential.method!!, auth, AuthAction.SignIn))
        } else suspendCancellableCoroutine { c ->
            auth.signInWithCredential(credential.credential)
                .addOnCompleteListener {
                    if (it.isSuccessful) c.resume(toUserInfo(it.result!!))
                    else authException(it.exception!!)
                }
        }
    }

    override suspend fun verifyPasswordResetCode(code: String): String =
        suspendCancellableCoroutine { c ->
            auth.verifyPasswordResetCode(code).addOnCompleteListener {
                if (it.isSuccessful) c.resume(it.result)
                else authException(it.exception!!)
            }
        }

    override fun isSignInWithEmailLink(link: String): Boolean =
        auth.isSignInWithEmailLink(link)

    override fun signOut() {
        auth.signOut()
    }

    override val config: FirebaseAuth.Config = object : FirebaseAuth.Config {
        override var settings: FirebaseAuth.Config.FirebaseAuthSettings =
            FirebaseAuth.Config.FirebaseAuthSettings()
            set(value) {
                field = value
                auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(settings.appVerificationDisabledForTesting)
            }

        override fun onAuthStateChange(): Flow<Unit> = callbackFlow {
            val listener: AndroidAuth.AuthStateListener =
                AndroidAuth.AuthStateListener { trySendBlocking(Unit) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }

        override fun onIdTokenChange(): Flow<Unit> = callbackFlow {
            val listener: AndroidAuth.IdTokenListener =
                AndroidAuth.IdTokenListener { trySendBlocking(Unit) }
            auth.addIdTokenListener(listener)
            awaitClose { auth.removeIdTokenListener(listener) }
        }

        override var tenantId: String?
            get() = auth.tenantId
            set(value) = auth.setTenantId(value ?: "")
        override var languageCode: String?
            get() = auth.languageCode
            set(value) = auth.setLanguageCode(value ?: "")

        override fun useAppLanguage(): Unit = auth.useAppLanguage()

        override fun useEmulator(host: String, port: Int) = auth.useEmulator(host, port)

    }

    private fun toActionCodeInfo(result: ActionCodeResult): ActionCodeInfo? =
        when (result.operation) {
            ActionCodeResult.VERIFY_EMAIL -> ActionCodeInfo.VerifyEmail(result.info!!.email)
            ActionCodeResult.PASSWORD_RESET -> ActionCodeInfo.PasswordReset(result.info!!.email)
            ActionCodeResult.REVERT_SECOND_FACTOR_ADDITION ->
                ActionCodeInfo.RevertSecondFactorAddition(email = result.info!!.email)

            ActionCodeResult.RECOVER_EMAIL -> {
                val info = (result.info!! as ActionCodeEmailInfo)
                ActionCodeInfo.RecoverEmail(info.email, info.previousEmail)
            }
            ActionCodeResult.VERIFY_BEFORE_CHANGE_EMAIL -> {
                val info = (result.info!! as ActionCodeEmailInfo)
                ActionCodeInfo.VerifyBeforeChangeEmail(info.email, info.previousEmail)
            }
            ActionCodeResult.SIGN_IN_WITH_EMAIL_LINK -> ActionCodeInfo.SignInWithEmailLink()
            else -> null
        }
}

private class FirebaseUserImpl(val user: AndroidUser, val auth: AndroidAuth) : FirebaseUser {
    override suspend fun delete(): Unit = suspendCancellableCoroutine { c ->
        user.delete().addOnCompleteListener {
            if (it.isSuccessful) c.resume(Unit)
            else authException(it.exception!!)
        }
    }

    override val displayName: String? get() = user.displayName
    override val email: String? get() = user.email

    override suspend fun getIdToken(forceRefresh: Boolean): TokenResult =
        suspendCancellableCoroutine { c ->
            user.getIdToken(forceRefresh).addOnCompleteListener {
                if (it.isSuccessful) c.resume(toTokenResult(it.result))
                else authException(it.exception!!)
            }
        }

    override val creationTimestamp: Long get() = user.metadata!!.creationTimestamp
    override val lastSignInTimestamp: Long get() = user.metadata!!.lastSignInTimestamp
    override val phoneNumber: String? get() = user.phoneNumber
    override val photoUrl: String? get() = user.photoUrl?.toString()
    override val providerData: List<UserInfo>
        get() = user.providerData.map {
            UserInfo(
                displayName = it.displayName,
                email = it.email,
                phoneNumber = it.phoneNumber,
                photoUrl = it.photoUrl.toString(),
                providerId = it.providerId,
                uid = it.uid,
                isEmailVerified = it.isEmailVerified
            )
        }
    override val tenantId: String? get() = user.tenantId
    override val uid: String get() = user.uid
    override val isAnonymous: Boolean get() = user.isAnonymous

    override suspend fun linkMethod(method: AuthMethod): AdditionalUserInfo {
        val credential = toCredentialHolder(method)
        return if (credential.credential == null) {
            toUserInfo(getAltResult(credential.method!!, auth, AuthAction.Link))
        } else suspendCancellableCoroutine { c ->
            user.linkWithCredential(credential.credential)
                .addOnCompleteListener {
                    if (it.isSuccessful) c.resume(toUserInfo(it.result!!))
                    else authException(it.exception!!)
                }
        }
    }


    override suspend fun reauthenticate(method: AuthMethod): AdditionalUserInfo {
        val credential = toCredentialHolder(method)
        return if (credential.credential == null) {
            toUserInfo(getAltResult(credential.method!!, auth, AuthAction.Reauthenticate))
        } else suspendCancellableCoroutine { c ->
            user.reauthenticateAndRetrieveData(credential.credential)
                .addOnCompleteListener {
                    if (it.isSuccessful) c.resume(toUserInfo(it.result!!))
                    else authException(it.exception!!)
                }
        }
    }

    override suspend fun reload(): Unit = suspendCancellableCoroutine { c ->
        user.reload().addOnCompleteListener {
            if (it.isSuccessful) c.resume(Unit)
            else authException(it.exception!!)
        }
    }

    override suspend fun sendEmailVerification(settings: ActionCodeSettings?): Unit =
        suspendCancellableCoroutine { c ->
            (if (settings == null) user.sendEmailVerification()
            else user.sendEmailVerification(toAndroidCodeSettings(settings))).addOnCompleteListener {
                if (it.isSuccessful) c.resume(Unit)
                else authException(it.exception!!)
            }
        }

    override suspend fun unlinkMethod(provider: AuthProvider): Unit =
        suspendCancellableCoroutine { c ->
            user.unlink(toAuthString(provider)).addOnCompleteListener {
                if (it.isSuccessful) c.resume(Unit)
                else authException(it.exception!!)
            }
        }

    override suspend fun updateEmail(email: String): Unit = suspendCancellableCoroutine { c ->
        user.updateEmail(email).addOnCompleteListener {
            if (it.isSuccessful) c.resume(Unit)
            else authException(it.exception!!)
        }
    }

    override suspend fun updatePassword(password: String): Unit =
        suspendCancellableCoroutine { c ->
            user.updatePassword(password).addOnCompleteListener {
                if (it.isSuccessful) c.resume(Unit)
                else authException(it.exception!!)
            }
        }

    override suspend fun updateProfile(displayName: String?, photoUrl: String?): Unit =
        suspendCancellableCoroutine { c ->
            user.updateProfile(UserProfileChangeRequest.Builder().apply {
                if (displayName != null) setDisplayName(displayName)
                if (photoUrl != null) photoUri = Uri.parse(photoUrl)
            }.build()).addOnCompleteListener {
                if (it.isSuccessful) c.resume(Unit)
                else authException(it.exception!!)
            }
        }

    override suspend fun verifyBeforeUpdateEmail(
        newEmail: String,
        settings: ActionCodeSettings?
    ): Unit = suspendCancellableCoroutine { c ->
        (if (settings == null) user.verifyBeforeUpdateEmail(newEmail)
        else user.verifyBeforeUpdateEmail(newEmail, toAndroidCodeSettings(settings)))
            .addOnCompleteListener {
                if (it.isSuccessful) c.resume(Unit)
                else authException(it.exception!!)
            }
    }

    fun toTokenResult(result: GetTokenResult): TokenResult = TokenResult(
        authTimestamp = result.authTimestamp,
        claims = result.claims,
        expirationTimestamp = result.expirationTimestamp,
        issuedAtTimestamp = result.issuedAtTimestamp,
        signInProvider = result.signInProvider?.let { toAuthProvider(it) },
        signInSecondFactor = result.signInSecondFactor?.let { toAuthProvider(it) },
        token = result.token
    )
}

private const val RC_SIGN_IN = 420

private suspend fun toCredentialHolder(method: AuthMethod): CredentialHolder =
    when (method) {
        AuthMethod.Annonymous -> CredentialHolder(method = method)
        is AuthMethod.Apple -> CredentialHolder(method = method)
        is AuthMethod.Custom -> CredentialHolder(method = method)
        is AuthMethod.EmailLink -> CredentialHolder(method = method)
        is AuthMethod.EmailPassword -> CredentialHolder(method = method)
        is AuthMethod.GitHub -> CredentialHolder(method = method)
        is AuthMethod.Google -> {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(method.webClientId).apply {
                    if (method.requestEmail) requestEmail()
                    if (method.requestProfile) requestProfile()
                }.build()
            (method.ui as Activity).startActivityForResult(
                GoogleSignIn.getClient(method.ui, options).signInIntent, RC_SIGN_IN
            )
            val data = (method.ui as AuthActivity).results.untilResult(RC_SIGN_IN).second
            suspendCancellableCoroutine { c ->
                try {
                    val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
                    c.resume(
                        CredentialHolder(
                            GoogleAuthProvider.getCredential(account.idToken, null),
                        )
                    )
                } catch (e: java.lang.Exception) {
                    throw(AuthException(AuthException.Code.Unknown))
                }
            }
        }
        is AuthMethod.Twitter -> CredentialHolder(method = method)
        is AuthMethod.Yahoo -> CredentialHolder(method = method)
    }

private suspend fun getAltResult(
    method: AuthMethod,
    auth: AndroidAuth,
    action: AuthAction
): AuthResult =
    when (method) {
        is AuthMethod.Apple -> getAppleResult(method, auth, action)
        is AuthMethod.Annonymous -> getAnnonymousResult(auth, action)
        is AuthMethod.Custom -> getCustomResult(method, auth, action)
        is AuthMethod.EmailLink -> getEmailLinkResult(method, auth, action)
        is AuthMethod.EmailPassword -> getEmailPasswordResult(method, auth, action)
        is AuthMethod.GitHub -> getGitHubResult(method, auth, action)
        is AuthMethod.Twitter -> getTwitterResult(method, auth, action)
        is AuthMethod.Yahoo -> getYahooResult(method, auth, action)
        else -> error("Unsupported AuthMethod $method used")
    }

private suspend fun getAnnonymousResult(
    auth: AndroidAuth,
    action: AuthAction
): AuthResult = suspendCancellableCoroutine { c ->
    if (action != AuthAction.SignIn) error("Annonymous reauthentication and linking are invalid operations")
    auth.signInAnonymously().addOnCompleteListener {
        if (it.isSuccessful) c.resume(it.result)
        else authException(it.exception!!)
    }
}

private suspend fun getCustomResult(
    method: AuthMethod.Custom,
    auth: AndroidAuth,
    action: AuthAction
): AuthResult = suspendCancellableCoroutine { c ->
    if (action != AuthAction.SignIn) error("Custom token reauthentication and linking are invalid operations")
    auth.signInWithCustomToken(method.token).addOnCompleteListener {
        if (it.isSuccessful) c.resume(it.result)
        else authException(it.exception!!)
    }
}

private suspend fun getEmailPasswordResult(
    method: AuthMethod.EmailPassword,
    auth: AndroidAuth,
    action: AuthAction
): AuthResult = suspendCancellableCoroutine { c ->
    if (action == AuthAction.SignIn) auth.signInWithEmailAndPassword(method.email, method.password)
        .addOnCompleteListener {
            if (it.isSuccessful) c.resume(it.result)
            else if (it.exception!! is FirebaseAuthInvalidUserException) auth.createUserWithEmailAndPassword(
                method.email, method.password
            ).addOnCompleteListener {
                if (it.isSuccessful) c.resume(it.result) else authException(
                    it.exception!!
                )
            }
        }
    else
        (if (action == AuthAction.Reauthenticate) auth.currentUser!!.reauthenticateAndRetrieveData(
            EmailAuthProvider.getCredential(method.email, method.password)
        ) else auth.currentUser!!.linkWithCredential(
            EmailAuthProvider.getCredential(method.email, method.password)
        )).addOnCompleteListener {
            if (it.isSuccessful) c.resume(it.result)
            else authException(it.exception!!)
        }
}

private suspend fun getEmailLinkResult(
    method: AuthMethod.EmailLink,
    auth: AndroidAuth,
    action: AuthAction
): AuthResult = suspendCancellableCoroutine { c ->
    (when (action) {
        AuthAction.SignIn -> auth.signInWithEmailLink(method.email, method.link)
        AuthAction.Reauthenticate -> auth.currentUser!!.reauthenticateAndRetrieveData(
            EmailAuthProvider.getCredentialWithLink(method.email, method.link)
        )
        AuthAction.Link -> auth.currentUser!!.linkWithCredential(
            EmailAuthProvider.getCredentialWithLink(method.email, method.link)
        )
    }).addOnCompleteListener {
        if (it.isSuccessful) c.resume(it.result)
        else authException(it.exception!!)
    }
}

private enum class AuthAction { SignIn, Reauthenticate, Link }

private suspend fun getOAuthResult(
    activity: Activity,
    auth: AndroidAuth,
    oAuthProvider: OAuthProvider,
    action: AuthAction
): AuthResult = suspendCancellableCoroutine { c ->
    if (auth.pendingAuthResult != null) auth.pendingAuthResult!!.addOnCompleteListener {
        if (!it.isSuccessful) authException(it.exception!!)
        (auth.currentUser?.reauthenticateAndRetrieveData(it.result.credential!!)
            ?: auth.signInWithCredential(it.result.credential!!)).addOnCompleteListener {
            if (it.isSuccessful) c.resume(it.result)
            else authException(it.exception!!)
        }
    } else {
        (when (action) {
            AuthAction.SignIn -> auth.startActivityForSignInWithProvider(
                activity, oAuthProvider
            )
            AuthAction.Reauthenticate -> auth.currentUser!!.startActivityForReauthenticateWithProvider(
                activity, oAuthProvider
            )
            AuthAction.Link -> auth.currentUser!!.startActivityForLinkWithProvider(
                activity, oAuthProvider
            )
        }).addOnCompleteListener {
            if (it.isSuccessful) c.resume(it.result)
            else authException(it.exception!!)
        }
    }
}

private suspend fun getTwitterResult(
    method: AuthMethod.Twitter,
    auth: AndroidAuth,
    action: AuthAction
): AuthResult = getOAuthResult(
    method.activity as Activity, auth,
    OAuthProvider.newBuilder("twitter.com").apply {
        if (method.locale != null) addCustomParameter("lang", method.locale)
    }.build(), action
)


private suspend fun getGitHubResult(
    method: AuthMethod.GitHub,
    auth: AndroidAuth,
    action: AuthAction
): AuthResult = getOAuthResult(
    method.activity as Activity, auth,
    OAuthProvider.newBuilder("github.com").apply {
        scopes = buildList {
            if (method.requestEmail) add("user:email")
            if (method.allowSignUp) addCustomParameter("allow_signup", "true")
        }
        if (method.loginHint != null) addCustomParameter("login", method.loginHint)
    }.build(), action
)

private suspend fun getAppleResult(
    method: AuthMethod.Apple,
    auth: AndroidAuth,
    action: AuthAction
): AuthResult = getOAuthResult(
    method.ui as Activity, auth, OAuthProvider.newBuilder("apple.com").apply {
        scopes = buildList {
            if (method.requestEmail) add("email")
            if (method.requestName) add("name")
        }
        if (method.locale != null) addCustomParameter("locale", method.locale)
    }.build(), action
)

private suspend fun getYahooResult(method: AuthMethod.Yahoo, auth: AndroidAuth, action: AuthAction):
        AuthResult = getOAuthResult(
    method.activity as Activity, auth,
    OAuthProvider.newBuilder("yahoo.com").apply {
        scopes = buildList {
            if (method.requestEmail) add("email")
            if (method.requestProfile) add("profile")
        }
        if (method.language != null) addCustomParameter("language", method.language)
        if (method.prompt != null) addCustomParameter("prompt", method.prompt)
        if (method.maxAge != null) addCustomParameter("max_age", method.maxAge.toString())
    }.build(), action
)

private fun toAndroidCodeSettings(settings: ActionCodeSettings): AndroidCodeSettings =
    AndroidCodeSettings.newBuilder().apply {
        if (settings.androidConfig != null) setAndroidPackageName(
            settings.androidConfig.androidPackageName,
            settings.androidConfig.installIfNotAvailable,
            settings.androidConfig.minimumVersion
        )
        if (settings.dynamicLinkDomain != null) dynamicLinkDomain = settings.dynamicLinkDomain
        handleCodeInApp = settings.handleCodeInApp
        if (settings.iOSBundleId != null) setIOSBundleId(settings.iOSBundleId)
        if (settings.url != null) url = settings.url
    }.build()

private fun authException(exception: Exception): Nothing {
    val code = if (exception is AndroidAuthException) when (exception) {
        is FirebaseAuthActionCodeException -> AuthException.Code.ActionCode
        is FirebaseAuthEmailException -> AuthException.Code.Email
        is FirebaseAuthInvalidCredentialsException -> AuthException.Code.InvalidCredentials
        is FirebaseAuthInvalidUserException -> AuthException.Code.InvalidUser
        is FirebaseAuthMultiFactorException -> AuthException.Code.MultiFactor
        is FirebaseAuthRecentLoginRequiredException -> AuthException.Code.RecentLoginRequired
        is FirebaseAuthUserCollisionException -> AuthException.Code.UserCollision
        is FirebaseAuthWebException -> AuthException.Code.AuthWeb
        is FirebaseAuthWeakPasswordException -> AuthException.Code.WeakPassword
        else -> AuthException.Code.Unknown
    } else AuthException.Code.Unknown
    throw AuthException(code, exception.message)
}

private fun toUserInfo(result: AuthResult): AdditionalUserInfo = AdditionalUserInfo(
    result.additionalUserInfo!!.profile ?: emptyMap(),
    result.additionalUserInfo!!.username,
    result.additionalUserInfo!!.isNewUser
)

private class CredentialHolder(
    val credential: AndroidAuthCredential? = null, val method: AuthMethod? = null
)

class ActivityResults {
    private val listeners: MutableList<(Int, Int, Intent) -> Unit> = mutableListOf()
    internal suspend fun untilResult(code: Int): Pair<Int, Intent> =
        suspendCancellableCoroutine { c ->
            var listener: ((Int, Int, Intent) -> Unit)? = null
            listener = { requestCode, resultCode, data ->
                listeners.remove(listener)
                if (code == requestCode) c.resume(resultCode to data)
            }
            listeners.add(listener)
        }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        listeners.forEach { it(requestCode, resultCode, data) }
    }
}

interface AuthActivity {
    val results: ActivityResults
}

internal actual val firebaseAuthInstance: FirebaseAuth by lazy { FirebaseAuthImpl(AndroidAuth.getInstance()) }
internal actual fun getAuthInstance(app: FirebaseApp): FirebaseAuth =
    FirebaseAuthImpl(AndroidAuth.getInstance(app.app))
