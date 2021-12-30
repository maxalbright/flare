package enchant.flare

import cocoapods.FirebaseAuth.*
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import objcnames.classes.FIRApp
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import kotlin.coroutines.resume


private class FirebaseAuthImpl(val auth: FIRAuth) : FirebaseAuth {
    override suspend fun applyActionCode(code: String): Unit = suspendCancellableCoroutine { c ->
        auth.applyActionCode(code) { error ->
            if (!c.isActive) return@applyActionCode
            if (error == null) c.resume(Unit)
            else throw toAuthException(error)
        }
    }

    override suspend fun checkActionCode(code: String): ActionCodeInfo =
        suspendCancellableCoroutine { c ->
            auth.checkActionCode(code) { data, error ->
                if (!c.isActive) return@checkActionCode
                if (data != null) c.resume(toActionCodeInfo(data))
                else throw toAuthException(error!!)
            }
        }

    override suspend fun confirmPasswordReset(code: String, newPassword: String): Unit =
        suspendCancellableCoroutine { c ->
            auth.confirmPasswordResetWithCode(code, newPassword) { error ->
                if (!c.isActive) return@confirmPasswordResetWithCode
                if (error == null) c.resume(Unit)
                else throw toAuthException(error)
            }
        }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): AuthResult = suspendCancellableCoroutine { c ->
        auth.createUserWithEmail(email, password) { data, error ->
            if (!c.isActive) return@createUserWithEmail
            if (data != null) c.resume(toAuthResult(data))
            else throw toAuthException(error!!)
        }
    }

    override suspend fun fetchSignInProvidersForEmail(email: String): List<AuthProvider> =
        suspendCancellableCoroutine { c ->
            auth.fetchSignInMethodsForEmail(email) { data, error ->
                if (!c.isActive) return@fetchSignInMethodsForEmail
                if (data != null) c.resume(data?.map { toAuthProvider(it as String) }
                    ?: emptyList())
                else throw toAuthException(error!!)
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
            val completion: (NSError?) -> Unit = { error ->
                if (c.isActive) {
                    if (error == null) c.resume(Unit)
                    else throw toAuthException(error)
                }
            }
            if (settings == null) auth.sendPasswordResetWithEmail(email, completion) else
                auth.sendPasswordResetWithEmail(email, toFIRCodeSettings(settings), completion)
        }

    override suspend fun sendSignInLinkToEmail(email: String, settings: ActionCodeSettings): Unit =
        suspendCancellableCoroutine { c ->
            auth.sendSignInLinkToEmail(email, toFIRCodeSettings(settings)) { error ->
                if (!c.isActive) return@sendSignInLinkToEmail
                if (error == null) c.resume(Unit)
                else throw toAuthException(error)
            }
        }

    override suspend fun signInAnonymously(): AuthResult = suspendCancellableCoroutine { c ->
        auth.signInAnonymouslyWithCompletion { data, error ->
            if (!c.isActive) return@signInAnonymouslyWithCompletion
            if (data != null) c.resume(toAuthResult(data))
            else throw toAuthException(error!!)
        }
    }

    override suspend fun signIn(method: AuthMethod): AuthResult =
        signIn(toAuthCredential(method, auth))

    @Suppress("UNREACHABLE_CODE")
    override suspend fun signIn(credential: AuthCredential): AuthResult =
        suspendCancellableCoroutine { c ->
            auth.signInWithCredential((credential as AuthCredentialImpl).credential) { data, error ->
                if (!c.isActive) return@signInWithCredential
                if (data != null) c.resume(toAuthResult(data))
                else throw toAuthException(error!!)
            }
        }

    override suspend fun signInWithCustomToken(token: String): AuthResult =
        suspendCancellableCoroutine { c ->
            auth.signInWithCustomToken(token) { data, error ->
                if (!c.isActive) return@signInWithCustomToken
                if (data != null) c.resume(toAuthResult(data))
                else throw toAuthException(error!!)
            }
        }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult =
        suspendCancellableCoroutine { c ->
            auth.signInWithEmail(email, password = password) { data, error ->
                if (!c.isActive) return@signInWithEmail
                if (data != null) c.resume(toAuthResult(data))
                else throw toAuthException(error!!)
            }
        }

    override suspend fun signInWithEmailLink(email: String, link: String): Unit =
        suspendCancellableCoroutine { c ->
            auth.sendSignInLinkToEmail(
                email,
                FIRActionCodeSettings().apply { URL = NSURL(string = link) }) { error ->
                if (!c.isActive) return@sendSignInLinkToEmail
                if (error == null) c.resume(Unit)
                else throw toAuthException(error!!)
            }
        }

    override suspend fun verifyPasswordResetCode(code: String): String =
        suspendCancellableCoroutine { c ->
            auth.verifyPasswordResetCode(code) { data, error ->
                if (!c.isActive) return@verifyPasswordResetCode
                if (data != null) c.resume(data)
                else throw toAuthException(error!!)
            }
        }

    override fun signOut() {
        memScoped {
            val p: ObjCObjectVar<NSError?> = alloc()
            auth.signOut(p.ptr)
            if (p.value != null) throw toAuthException(p.value!!)
        }
    }

    override val config: FirebaseAuth.Config = object : FirebaseAuth.Config {
        override var settings: FirebaseAuth.Config.FirebaseAuthSettings
            get() = auth.settings?.let { toAuthSettings(it) }
                ?: FirebaseAuth.Config.FirebaseAuthSettings()
            set(value) {
                auth.settings = toFIRSettings(value)
            }

        override fun onAuthStateChange(): Flow<Unit> = callbackFlow {
            val handle = auth.addAuthStateDidChangeListener { _, _ -> trySendBlocking(Unit) }
            awaitClose { auth.removeAuthStateDidChangeListener(handle) }
        }

        override fun onIdTokenChange(): Flow<Unit> = callbackFlow {
            val handle = auth.addIDTokenDidChangeListener { _, _ -> trySendBlocking(Unit) }
            awaitClose { auth.removeIDTokenDidChangeListener(handle) }
        }

        override var tenantId: String?
            get() = auth.tenantID
            set(value) {
                auth.tenantID = value
            }
        override var languageCode: String?
            get() = auth.languageCode
            set(value) {
                auth.languageCode = value
            }

        override fun useAppLanguage(): Unit = auth.useAppLanguage()

        override fun useEmulator(host: String, port: Int) =
            auth.useEmulatorWithHost(host, port.toLong())

    }

    private fun toActionCodeInfo(result: FIRActionCodeInfo): ActionCodeInfo =
        when (result.operation) {
            FIRActionCodeOperationVerifyEmail -> ActionCodeInfo.VerifyEmail(result.email!!)
            FIRActionCodeOperationPasswordReset -> ActionCodeInfo.PasswordReset(result.email!!)
            FIRActionCodeOperationRevertSecondFactorAddition ->
                ActionCodeInfo.RevertSecondFactorAddition(email = result.email!!)
            FIRActionCodeOperationRecoverEmail ->
                ActionCodeInfo.RecoverEmail(result.email!!, result.previousEmail!!)
            FIRActionCodeOperationVerifyAndChangeEmail
            -> ActionCodeInfo.VerifyBeforeChangeEmail(result.email!!, result.previousEmail!!)
            FIRActionCodeOperationEmailLink -> ActionCodeInfo.SignInWithEmailLink()
            else -> error("Unknown action code operation encountered: $result")
        }
}

private class FirebaseUserImpl(val user: FIRUser, val auth: FIRAuth) : FirebaseUser {
    override suspend fun delete(): Unit = suspendCancellableCoroutine { c ->
        user.deleteWithCompletion { error ->
            if (!c.isActive) return@deleteWithCompletion
            if (error == null) c.resume(Unit)
            else throw toAuthException(error)
        }
    }

    override val displayName: String? get() = user.displayName
    override val email: String? get() = user.email

    override suspend fun getIdToken(forceRefresh: Boolean): TokenResult =
        suspendCancellableCoroutine { c ->
            user.getIDTokenResultForcingRefresh(forceRefresh) { data, error ->
                if (!c.isActive) return@getIDTokenResultForcingRefresh
                if (data != null) c.resume(toTokenResult(data))
                else throw toAuthException(error!!)
            }
        }

    override val creationTimestamp: Long
        get() = user.metadata.creationDate?.timeIntervalSince1970?.toLong() ?: -1
    override val lastSignInTimestamp: Long
        get() = user.metadata.lastSignInDate?.timeIntervalSince1970?.toLong() ?: -1
    override val phoneNumber: String? get() = user.phoneNumber
    override val photoUrl: String? get() = user.photoURL?.toString()
    override val providerData: List<UserInfo>
        get() = user.providerData.map {
            val info = it as FIRUserInfoProtocol
            UserInfo(
                displayName = info.displayName,
                email = info.email,
                phoneNumber = info.phoneNumber,
                photoUrl = info.photoURL.toString(),
                providerId = info.providerID,
                uid = info.uid,
            )
        }
    override val tenantId: String? get() = user.tenantID
    override val uid: String get() = user.uid
    override val isAnonymous: Boolean get() = user.isAnonymous()

    override suspend fun linkWithMethod(method: AuthMethod): AuthResult =
        linkWithMethod(toAuthCredential(method, auth))

    override suspend fun linkWithMethod(credential: AuthCredential): AuthResult =
        suspendCancellableCoroutine { c ->
            user.linkWithCredential((credential as AuthCredentialImpl).credential) { data, error ->
                if (!c.isActive) return@linkWithCredential
                if (data != null) c.resume(toAuthResult(data))
                else throw toAuthException(error!!)
            }
        }

    override suspend fun reauthenticate(method: AuthMethod): AuthResult =
        reauthenticate(toAuthCredential(method, auth))

    override suspend fun reauthenticate(credential: AuthCredential): AuthResult =
        suspendCancellableCoroutine { c ->
            user.reauthenticateWithCredential((credential as AuthCredentialImpl).credential) { data, error ->
                if (!c.isActive) return@reauthenticateWithCredential
                if (data != null) c.resume(toAuthResult(data))
                else throw toAuthException(error!!)
            }
        }

    override suspend fun reload(): Unit = suspendCancellableCoroutine { c ->
        user.reloadWithCompletion { error ->
            if (!c.isActive) return@reloadWithCompletion
            if (error == null) c.resume(Unit)
            else throw toAuthException(error)
        }
    }

    override suspend fun sendEmailVerification(settings: ActionCodeSettings?): Unit =
        suspendCancellableCoroutine { c ->
            val completion: (NSError?) -> Unit = { error ->
                if (c.isActive) {
                    if (error == null) c.resume(Unit) else throw toAuthException(error)
                }
            }
            if (settings == null) user.sendEmailVerificationWithCompletion(completion)
            else user.sendEmailVerificationWithActionCodeSettings(
                toFIRCodeSettings(settings), completion
            )
        }

    override suspend fun unlink(provider: AuthProvider): Unit =
        suspendCancellableCoroutine { c ->
            user.unlinkFromProvider(toAuthString(provider)) { data, error ->
                if (!c.isActive) return@unlinkFromProvider
                if (data != null) c.resume(Unit)
                else throw toAuthException(error!!)
            }
        }

    override suspend fun updateEmail(email: String): Unit = suspendCancellableCoroutine { c ->
        user.updateEmail(email) { error ->
            if (!c.isActive) return@updateEmail
            if (error == null) c.resume(Unit)
            else throw toAuthException(error)
        }
    }

    override suspend fun updatePassword(password: String): Unit =
        suspendCancellableCoroutine { c ->
            user.updatePassword(password) { error ->
                if (!c.isActive) return@updatePassword
                if (error == null) c.resume(Unit)
                else throw toAuthException(error)
            }
        }

    override suspend fun updateProfile(displayName: String?, photoUrl: String?): Unit =
        suspendCancellableCoroutine { c ->
            user.profileChangeRequest().apply {
                if (displayName != null) setDisplayName(displayName)
                if (photoUrl != null) photoURL = NSURL(string = photoUrl)
            }.commitChangesWithCompletion { error ->
                if (!c.isActive) return@commitChangesWithCompletion
                if (error == null) c.resume(Unit)
                else throw toAuthException(error)
            }
        }

    override suspend fun verifyBeforeUpdateEmail(
        newEmail: String,
        settings: ActionCodeSettings?
    ): Unit = suspendCancellableCoroutine { c ->
        val completion: (NSError?) -> Unit = { error ->
            if (c.isActive) {
                if (error == null) c.resume(Unit)
                else throw toAuthException(error)
            }
        }

        if (settings == null) user.sendEmailVerificationBeforeUpdatingEmail(newEmail, completion)
        else user.sendEmailVerificationBeforeUpdatingEmail(
            newEmail, toFIRCodeSettings(settings), completion
        )

    }

    fun toTokenResult(result: FIRAuthTokenResult): TokenResult = TokenResult(
        authTimestamp = result.authDate.timeIntervalSince1970.toLong(),
        claims = result.claims as Map<String, Any>,
        expirationTimestamp = result.expirationDate.timeIntervalSince1970.toLong(),
        issuedAtTimestamp = result.issuedAtDate.timeIntervalSince1970.toLong(),
        signInProvider = toAuthProvider(result.signInProvider),
        signInSecondFactor = toAuthProvider(result.signInSecondFactor),
        token = result.token
    )
}


private fun toAuthCredential(method: AuthMethod, auth: FIRAuth): AuthCredential =
    when (method) {
        is AuthMethod.AppleAndroid -> error("Android Apple authentication is not supported on iOS")
        is AuthMethod.AppleiOS ->
            AuthCredentialImpl(
                FIROAuthProvider.credentialWithProviderID(
                    "apple.com",
                    method.idToken,
                    rawNonce = method.rawNonce
                ), provider = AuthProvider.Apple
            )
        is AuthMethod.EmailLink ->
            AuthCredentialImpl(
                FIREmailAuthProvider.credentialWithEmail(method.email, link = method.emailLink),
                provider = AuthProvider.EmailLink
            )
        is AuthMethod.EmailPassword ->
            AuthCredentialImpl(
                FIREmailAuthProvider.credentialWithEmail(method.email, password = method.password),
                provider = AuthProvider.EmailPassword
            )
        is AuthMethod.Facebook ->
            AuthCredentialImpl(
                FIRFacebookAuthProvider.credentialWithAccessToken(method.accessToken),
                provider = AuthProvider.Facebook
            )
        is AuthMethod.Github ->
            AuthCredentialImpl(
                FIRGitHubAuthProvider.credentialWithToken(method.token),
                provider = AuthProvider.GitHub
            )
        is AuthMethod.Google ->
            AuthCredentialImpl(
                FIRGoogleAuthProvider.credentialWithIDToken(method.idToken, method.accessToken),
                provider = AuthProvider.Google
            )

        is AuthMethod.Phone -> AuthCredentialImpl(
            FIRPhoneAuthProvider.providerWithAuth(auth)
                .credentialWithVerificationID(method.verificationId, method.smsCode),
            provider = AuthProvider.Phone
        )
        is AuthMethod.PlayGames -> error("AuthMethod.PlayGames is not supproted on iOS")
        is AuthMethod.Twitter -> AuthCredentialImpl(
            FIRTwitterAuthProvider.credentialWithToken(method.token, method.secret),
            provider = AuthProvider.Twitter
        )
        is AuthMethod.Yahoo -> AuthCredentialImpl(
            oAuthProvider = FIROAuthProvider.providerWithProviderID("yahoo.com").apply {
                scopes = method.scopes
                customParameters = method.params as Map<Any?, *>
            }, provider = AuthProvider.Yahoo
        )
    }

private fun toFIRCodeSettings(settings: ActionCodeSettings): FIRActionCodeSettings =
    FIRActionCodeSettings().apply {
        if (settings.androidConfig != null) setAndroidPackageName(
            settings.androidConfig.androidPackageName,
            settings.androidConfig.installIfNotAvailable,
            settings.androidConfig.minimumVersion
        )
        if (settings.dynamicLinkDomain != null) dynamicLinkDomain = settings.dynamicLinkDomain
        handleCodeInApp = settings.handleCodeInApp
        if (settings.iOSBundleId != null) setIOSBundleID(settings.iOSBundleId)
        if (settings.url != null) URL = NSURL(string = settings.url)
    }

private fun toAuthException(exception: NSError): FirebaseAuthException = FirebaseAuthException(
    when (exception.code) {
        FIRAuthErrorCodeInvalidActionCode,
        FIRAuthErrorCodeExpiredActionCode -> FirebaseAuthException.Code.ActionCode

        FIRAuthErrorCodeInvalidEmail -> FirebaseAuthException.Code.Email

        FIRAuthErrorCodeCaptchaCheckFailed,
        FIRAuthErrorCodeInvalidPhoneNumber,
        FIRAuthErrorCodeMissingPhoneNumber,
        FIRAuthErrorCodeInvalidVerificationID,
        FIRAuthErrorCodeInvalidVerificationCode,
        FIRAuthErrorCodeMissingVerificationID,
        FIRAuthErrorCodeMissingVerificationCode,
        FIRAuthErrorCodeInvalidCredential -> FirebaseAuthException.Code.InvalidCredentials

        FIRAuthErrorCodeInvalidUserToken -> FirebaseAuthException.Code.InvalidUser

        FIRAuthErrorCodeSecondFactorAlreadyEnrolled,
        FIRAuthErrorCodeSecondFactorRequired,
        FIRAuthErrorCodeMaximumSecondFactorCountExceeded,
        FIRAuthErrorCodeMultiFactorInfoNotFound -> FirebaseAuthException.Code.MultiFactor

        FIRAuthErrorCodeRequiresRecentLogin -> FirebaseAuthException.Code.RecentLoginRequired

        FIRAuthErrorCodeEmailAlreadyInUse,
        FIRAuthErrorCodeAccountExistsWithDifferentCredential,
        FIRAuthErrorCodeCredentialAlreadyInUse -> FirebaseAuthException.Code.UserCollision

        FIRAuthErrorCodeWebContextAlreadyPresented,
        FIRAuthErrorCodeWebContextCancelled,
        FIRAuthErrorCodeWebNetworkRequestFailed,
        FIRAuthErrorCodeWebSignInUserInteractionFailure,
        FIRAuthErrorCodeWebInternalError -> FirebaseAuthException.Code.AuthWeb

        FIRAuthErrorCodeWeakPassword -> FirebaseAuthException.Code.WeakPassword

        else -> error("Unknown Firebase Auth error: ${exception.localizedDescription()}")
    }
)

private fun toCredential(credential: FIRAuthCredential?): AuthCredential? {
    if (credential == null) return null
    val provider: AuthProvider = toAuthProvider(credential.provider)
    return AuthCredentialImpl(credential, provider = provider)
}

private fun toAuthResult(result: FIRAuthDataResult): AuthResult = AuthResult(
    if (result.additionalUserInfo != null) AdditionalUserInfo(
        (result.additionalUserInfo!!.profile ?: emptyMap<String, Any>()) as Map<String, Any>,
        result.additionalUserInfo!!.providerID,
        result.additionalUserInfo!!.username,
        result.additionalUserInfo!!.newUser
    ) else null, toCredential(result.credential)
)

private fun toFIRSettings(settings: FirebaseAuth.Config.FirebaseAuthSettings): FIRAuthSettings =
    FIRAuthSettings().apply {
        appVerificationDisabledForTesting = settings.appVerificationDisabledForTesting
    }

private fun toAuthSettings(settings: FIRAuthSettings): FirebaseAuth.Config.FirebaseAuthSettings =
    FirebaseAuth.Config.FirebaseAuthSettings(settings.appVerificationDisabledForTesting)

private class AuthCredentialImpl(
    private val androidCredential: FIRAuthCredential? = null,
    val oAuthProvider: FIROAuthProvider? = null,
    override val provider: AuthProvider,
) : AuthCredential {
    val credential
        get() = androidCredential
            ?: error("Auth operation not supported for AuthMethod.${provider.name}")
}

internal actual val firebaseAuthInstance: FirebaseAuth by lazy { FirebaseAuthImpl(FIRAuth.auth()) }
internal actual fun getAuthInstance(app: FirebaseApp): FirebaseAuth =
    FirebaseAuthImpl(FIRAuth.authWithApp(app.app as FIRApp))
