package enchant.flare

import android.net.Uri
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.firebase.auth.FirebaseAuth as AndroidAuth
import com.google.firebase.auth.FirebaseAuthException as AndroidAuthException
import com.google.firebase.auth.ActionCodeResult
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.AuthResult as AndroidAuthResult
import com.google.firebase.auth.AuthCredential as AndroidAuthCredential
import com.google.firebase.auth.FirebaseUser as AndroidUser
import com.google.firebase.auth.ActionCodeSettings as AndroidCodeSettings


private class FirebaseAuthImpl(val auth: AndroidAuth) : FirebaseAuth {
    override suspend fun applyActionCode(code: String): Unit = suspendCancellableCoroutine { c ->
        auth.applyActionCode(code).addOnCompleteListener {
            if (!c.isActive) return@addOnCompleteListener
            if (it.isSuccessful) c.resume(Unit)
            else throw toAuthException(it.exception!!)
        }
    }

    override suspend fun checkActionCode(code: String): ActionCodeInfo =
        suspendCancellableCoroutine { c ->
            auth.checkActionCode(code).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(
                    toActionCodeInfo(it.result)
                        ?: throw FirebaseAuthException(FirebaseAuthException.Code.ActionCode)
                )
                else throw toAuthException(it.exception!!)
            }
        }

    override suspend fun confirmPasswordReset(code: String, newPassword: String): Unit =
        suspendCancellableCoroutine { c ->
            auth.confirmPasswordReset(code, newPassword).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(Unit)
                else throw toAuthException(it.exception!!)
            }
        }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): AuthResult = suspendCancellableCoroutine { c ->
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            if (!c.isActive) return@addOnCompleteListener
            if (it.isSuccessful) c.resume(toAuthResult(it.result))
            else throw toAuthException(it.exception!!)
        }
    }

    override suspend fun fetchSignInProvidersForEmail(email: String): List<AuthProvider> =
        suspendCancellableCoroutine { c ->
            auth.fetchSignInMethodsForEmail(email).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(it.result.signInMethods?.map { toAuthProvider(it) }
                    ?: emptyList())
                else throw toAuthException(it.exception!!)
            }
        }

    override val currentUser: FirebaseUser?
        get() = auth.currentUser?.let {
            FirebaseUserImpl(
                it,
                auth
            )
        }

    override suspend fun sendPasswordResetEmail(
        email: String,
        settings: ActionCodeSettings?
    ): Unit =
        suspendCancellableCoroutine { c ->
            auth.sendPasswordResetEmail(email, settings?.let { toAndroidCodeSettings(settings) })
                .addOnCompleteListener {
                    if (!c.isActive) return@addOnCompleteListener
                    if (it.isSuccessful) c.resume(Unit)
                    else throw toAuthException(it.exception!!)
                }
        }

    override suspend fun sendSignInLinkToEmail(email: String, settings: ActionCodeSettings): Unit =
        suspendCancellableCoroutine { c ->
            auth.sendSignInLinkToEmail(email, toAndroidCodeSettings(settings))
                .addOnCompleteListener {
                    if (!c.isActive) return@addOnCompleteListener
                    if (it.isSuccessful) c.resume(Unit)
                    else throw toAuthException(it.exception!!)
                }
        }

    override suspend fun signInAnonymously(): AuthResult = suspendCancellableCoroutine { c ->
        auth.signInAnonymously().addOnCompleteListener {
            if (!c.isActive) return@addOnCompleteListener
            if (it.isSuccessful) c.resume(toAuthResult(it.result))
            else throw toAuthException(it.exception!!)
        }
    }

    override suspend fun signIn(method: AuthMethod): AuthResult =
        signIn(toAuthCredential(method))

    override suspend fun signIn(credential: AuthCredential): AuthResult =
        suspendCancellableCoroutine { c ->
            auth.signInWithCredential((credential as AuthCredentialImpl).credential)
                .addOnCompleteListener {
                    if (!c.isActive) return@addOnCompleteListener
                    if (it.isSuccessful) c.resume(toAuthResult(it.result))
                    else throw toAuthException(it.exception!!)
                }
        }

    override suspend fun signInWithCustomToken(token: String): AuthResult =
        suspendCancellableCoroutine { c ->
            auth.signInWithCustomToken(token).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(toAuthResult(it.result))
                else throw toAuthException(it.exception!!)
            }
        }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult =
        suspendCancellableCoroutine { c ->
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(toAuthResult(it.result))
                else throw toAuthException(it.exception!!)
            }
        }

    override suspend fun signInWithEmailLink(email: String, link: String): Unit =
        suspendCancellableCoroutine { c ->
            auth.signInWithEmailLink(email, link).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(Unit)
                else throw toAuthException(it.exception!!)
            }
        }

    override suspend fun verifyPasswordResetCode(code: String): String =
        suspendCancellableCoroutine { c ->
            auth.verifyPasswordResetCode(code).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(it.result)
                else throw toAuthException(it.exception!!)
            }
        }

    override fun signOut() {
        auth.signOut()
    }

    override val config: FirebaseAuth.Config = object : FirebaseAuth.Config {
        override var settings: FirebaseAuth.Config.FirebaseAuthSettings
            get() = TODO("Not yet implemented")
            set(value) {}

        override fun onAuthStateChange(listener: () -> Unit): Listener {
            TODO("Not yet implemented")
        }

        override fun onIdTokenChange(listener: () -> Unit): Listener {
            TODO("Not yet implemented")
        }

        override suspend fun getPendingAuthResult(): AuthResult? {
            TODO("Not yet implemented")
        }

        override var tenantId: String?
            get() = TODO("Not yet implemented")
            set(value) {}
        override var languageCode: String
            get() = TODO("Not yet implemented")
            set(value) {}

        override fun useAppLanguage() {
            TODO("Not yet implemented")
        }

        override fun useEmulator(host: String, port: Int) {
            TODO("Not yet implemented")
        }

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
            if (!c.isActive) return@addOnCompleteListener
            if (it.isSuccessful) c.resume(Unit)
            else throw toAuthException(it.exception!!)
        }
    }

    override val displayName: String? get() = user.displayName
    override val email: String? get() = user.email

    override suspend fun getIdToken(forceRefresh: Boolean): TokenResult =
        suspendCancellableCoroutine { c ->
            user.getIdToken(forceRefresh).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(toTokenResult(it.result))
                else throw toAuthException(it.exception!!)
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

    override suspend fun linkWithMethod(method: AuthMethod): AuthResult =
        linkWithMethod(toAuthCredential(method))

    override suspend fun linkWithMethod(credential: AuthCredential): AuthResult =
        suspendCancellableCoroutine { c ->
            user.linkWithCredential((credential as AuthCredentialImpl).credential)
                .addOnCompleteListener {
                    if (!c.isActive) return@addOnCompleteListener
                    if (it.isSuccessful) c.resume(toAuthResult(it.result))
                    else throw toAuthException(it.exception!!)
                }
        }

    override suspend fun reauthenticate(method: AuthMethod): AuthResult =
        reauthenticate(toAuthCredential(method))

    override suspend fun reauthenticate(credential: AuthCredential): AuthResult =
        suspendCancellableCoroutine { c ->
            user.reauthenticateAndRetrieveData((credential as AuthCredentialImpl).credential)
                .addOnCompleteListener {
                    if (!c.isActive) return@addOnCompleteListener
                    if (it.isSuccessful) c.resume(toAuthResult(it.result))
                    else throw toAuthException(it.exception!!)
                }
        }

    override suspend fun reload(): Unit = suspendCancellableCoroutine { c ->
        user.reload().addOnCompleteListener {
            if (!c.isActive) return@addOnCompleteListener
            if (it.isSuccessful) c.resume(Unit)
            else throw toAuthException(it.exception!!)
        }
    }

    override suspend fun sendEmailVerification(settings: ActionCodeSettings?): Unit =
        suspendCancellableCoroutine { c ->
            (if (settings == null) user.sendEmailVerification()
            else user.sendEmailVerification(toAndroidCodeSettings(settings))).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(Unit)
                else throw toAuthException(it.exception!!)
            }
        }

    override suspend fun unlink(provider: AuthProvider): Unit =
        suspendCancellableCoroutine { c ->
            user.unlink(toAuthString(provider)).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(Unit)
                else throw toAuthException(it.exception!!)
            }
        }

    override suspend fun updateEmail(email: String): Unit = suspendCancellableCoroutine { c ->
        user.updateEmail(email).addOnCompleteListener {
            if (!c.isActive) return@addOnCompleteListener
            if (it.isSuccessful) c.resume(Unit)
            else throw toAuthException(it.exception!!)
        }
    }

    override suspend fun updatePassword(password: String): Unit =
        suspendCancellableCoroutine { c ->
            user.updatePassword(password).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(Unit)
                else throw toAuthException(it.exception!!)
            }
        }

    override suspend fun updateProfile(displayName: String?, photoUrl: String?): Unit =
        suspendCancellableCoroutine { c ->
            user.updateProfile(UserProfileChangeRequest.Builder().apply {
                if (displayName != null) setDisplayName(displayName)
                if (photoUrl != null) photoUri = Uri.parse(photoUrl)
            }.build()).addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(Unit)
                else throw toAuthException(it.exception!!)
            }
        }

    override suspend fun verifyBeforeUpdateEmail(
        newEmail: String,
        settings: ActionCodeSettings?
    ): Unit = suspendCancellableCoroutine { c ->
        (if (settings == null) user.verifyBeforeUpdateEmail(newEmail)
        else user.verifyBeforeUpdateEmail(newEmail, toAndroidCodeSettings(settings)))
            .addOnCompleteListener {
                if (!c.isActive) return@addOnCompleteListener
                if (it.isSuccessful) c.resume(Unit)
                else throw toAuthException(it.exception!!)
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


private fun toAuthCredential(method: AuthMethod): AuthCredential =
    when (method) {
        is AuthMethod.AppleAndroid ->
            AuthCredentialImpl(oAuthProvider = OAuthProvider.newBuilder("apple.com").apply {
                scopes = method.scopes
                addCustomParameters(method.params)
            }.build(), provider = AuthProvider.Apple)
        is AuthMethod.AppleiOS -> error("iOS Apple authentication is not supported on Android")
        is AuthMethod.EmailLink ->
            AuthCredentialImpl(
                EmailAuthProvider.getCredentialWithLink(method.email, method.emailLink),
                provider = AuthProvider.EmailLink
            )
        is AuthMethod.EmailPassword ->
            AuthCredentialImpl(
                EmailAuthProvider.getCredentialWithLink(method.email, method.password),
                provider = AuthProvider.EmailPassword
            )
        is AuthMethod.Facebook ->
            AuthCredentialImpl(
                FacebookAuthProvider.getCredential(method.accessToken),
                provider = AuthProvider.Facebook
            )
        is AuthMethod.Github ->
            AuthCredentialImpl(
                GithubAuthProvider.getCredential(method.token),
                provider = AuthProvider.GitHub
            )
        is AuthMethod.Google ->
            AuthCredentialImpl(
                GoogleAuthProvider.getCredential(method.idToken, method.accessToken),
                provider = AuthProvider.Google
            )

        is AuthMethod.Phone -> AuthCredentialImpl(
            PhoneAuthProvider.getCredential(method.verificationId, method.smsCode),
            provider = AuthProvider.Phone
        )
        is AuthMethod.PlayGames -> AuthCredentialImpl(
            PlayGamesAuthProvider.getCredential(method.serverAuthCode),
            provider = AuthProvider.PlayGames
        )
        is AuthMethod.Twitter -> AuthCredentialImpl(
            TwitterAuthProvider.getCredential(method.token, method.secret),
            provider = AuthProvider.Twitter
        )
        is AuthMethod.Yahoo -> AuthCredentialImpl(
            oAuthProvider = OAuthProvider.newBuilder("yahoo.com").apply {
                scopes = method.scopes
                addCustomParameters(method.params)
            }.build(), provider = AuthProvider.Yahoo
        )
    }

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

private fun toAuthException(exception: Exception): FirebaseAuthException {
    if (exception !is AndroidAuthException) throw(exception)
    return FirebaseAuthException(when (exception) {
        is FirebaseAuthActionCodeException -> FirebaseAuthException.Code.ActionCode
        is FirebaseAuthEmailException -> FirebaseAuthException.Code.Email
        is FirebaseAuthInvalidCredentialsException -> FirebaseAuthException.Code.InvalidCredentials
        is FirebaseAuthInvalidUserException -> FirebaseAuthException.Code.InvalidUser
        is FirebaseAuthMultiFactorException -> FirebaseAuthException.Code.MultiFactor
        is FirebaseAuthRecentLoginRequiredException -> FirebaseAuthException.Code.RecentLoginRequired
        is FirebaseAuthUserCollisionException -> FirebaseAuthException.Code.UserCollision
        is FirebaseAuthWebException -> FirebaseAuthException.Code.AuthWeb
        is FirebaseAuthWeakPasswordException -> FirebaseAuthException.Code.WeakPassword
        else -> FirebaseAuthException.Code.Unknown.also { println("Encountered unknown firebase auth error code: ${exception.errorCode}") }
    })
}

private fun toCredential(credential: AndroidAuthCredential?): AuthCredential? {
    if (credential == null) return null
    val provider: AuthProvider = toAuthProvider(credential.provider)
    return AuthCredentialImpl(credential, provider = provider)
}

private fun toAuthResult(result: AndroidAuthResult): AuthResult = AuthResult(
    if (result.additionalUserInfo != null) AdditionalUserInfo(
        result.additionalUserInfo?.profile ?: emptyMap(),
        result.additionalUserInfo!!.providerId,
        result.additionalUserInfo!!.username,
        result.additionalUserInfo!!.isNewUser
    ) else null, toCredential(result.credential)
)

private class AuthCredentialImpl(
    private val androidCredential: AndroidAuthCredential? = null,
    val oAuthProvider: OAuthProvider? = null,
    override val provider: AuthProvider,
) : AuthCredential {
    val credential
        get() = androidCredential
            ?: error("Auth operation not supported for AuthMethod.${provider.name}")
}

internal actual val firebaseAuthInstance: FirebaseAuth by lazy { FirebaseAuthImpl(AndroidAuth.getInstance()) }
internal actual fun getAuthInstance(app: FirebaseApp): FirebaseAuth =
    FirebaseAuthImpl(AndroidAuth.getInstance(app.app))
