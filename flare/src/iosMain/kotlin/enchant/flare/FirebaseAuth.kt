package enchant.flare

import cocoapods.FirebaseAuth.*
import cocoapods.GoogleSignIn.GIDConfiguration
import cocoapods.GoogleSignIn.GIDSignIn
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import objcnames.classes.Protocol
import platform.AuthenticationServices.*
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.*
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.UIKit.UIViewController
import platform.UIKit.window
import platform.darwin.NSObject
import platform.darwin.NSUInteger
import kotlin.coroutines.resume


private class FirebaseAuthImpl(val auth: FIRAuth) : FirebaseAuth {
    override suspend fun applyActionCode(code: String): Unit = suspendCancellableCoroutine { c ->
        auth.applyActionCode(code) { error ->
            if (error == null) c.resume(Unit)
            else throw toAuthException(error)
        }
    }

    override suspend fun checkActionCode(code: String): ActionCodeInfo =
        suspendCancellableCoroutine { c ->
            auth.checkActionCode(code) { data, error ->
                if (data != null) c.resume(toActionCodeInfo(data))
                else throw toAuthException(error!!)
            }
        }

    override suspend fun confirmPasswordReset(code: String, newPassword: String): Unit =
        suspendCancellableCoroutine { c ->
            auth.confirmPasswordResetWithCode(code, newPassword) { error ->
                if (error == null) c.resume(Unit)
                else throw toAuthException(error)
            }
        }

    override suspend fun fetchSignInProvidersForEmail(email: String): List<AuthProvider> =
        suspendCancellableCoroutine { c ->
            auth.fetchSignInMethodsForEmail(email) { data, error ->
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
                if (error == null) c.resume(Unit)
                else throw toAuthException(error)
            }
            if (settings == null) auth.sendPasswordResetWithEmail(email, completion) else
                auth.sendPasswordResetWithEmail(email, toFIRCodeSettings(settings), completion)
        }

    override suspend fun sendSignInLinkToEmail(email: String, settings: ActionCodeSettings): Unit =
        suspendCancellableCoroutine { c ->
            auth.sendSignInLinkToEmail(email, toFIRCodeSettings(settings)) { error ->
                if (error == null) c.resume(Unit)
                else throw toAuthException(error)
            }
        }

    override suspend fun signIn(method: AuthMethod): AdditionalUserInfo {
        val credential = toCredentialHolder(method, auth)
        return if (credential.credential == null)
            toUserInfo(getAltResult(credential.method!!, auth, AuthAction.SignIn))
        else suspendCancellableCoroutine { c ->
            auth.signInWithCredential(credential.credential!!) { data, error ->
                if (data != null) c.resume(toUserInfo(data))
                else throw toAuthException(error!!)
            }
        }
    }

    override suspend fun verifyPasswordResetCode(code: String): String =
        suspendCancellableCoroutine { c ->
            auth.verifyPasswordResetCode(code) { data, error ->
                if (data != null) c.resume(data)
                else throw toAuthException(error!!)
            }
        }

    override fun isSignInWithEmailLink(link: String): Boolean =
        auth.isSignInWithEmailLink(link)

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
            if (error == null) c.resume(Unit)
            else throw toAuthException(error)
        }
    }

    override val displayName: String? get() = user.displayName
    override val email: String? get() = user.email

    override suspend fun getIdToken(forceRefresh: Boolean): TokenResult =
        suspendCancellableCoroutine { c ->
            user.getIDTokenResultForcingRefresh(forceRefresh) { data, error ->
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

    override suspend fun linkMethod(method: AuthMethod): AdditionalUserInfo {
        val credential = toCredentialHolder(method, auth)
        return if (credential.credential == null)
            toUserInfo(getAltResult(credential.method!!, auth, AuthAction.Link))
        else suspendCancellableCoroutine { c ->
            user.linkWithCredential(credential.credential) { data, error ->
                if (data != null) c.resume(toUserInfo(data))
                else throw toAuthException(error!!)
            }
        }
    }

    override suspend fun reauthenticate(method: AuthMethod): AdditionalUserInfo {
        val credential = toCredentialHolder(method, auth)
        return if (credential.credential == null)
            toUserInfo(getAltResult(credential.method!!, auth, AuthAction.Reauthenticate))
        else suspendCancellableCoroutine { c ->
            user.reauthenticateWithCredential(credential.credential) { data, error ->
                if (data != null) c.resume(toUserInfo(data))
                else throw toAuthException(error!!)
            }
        }
    }

    override suspend fun reload(): Unit = suspendCancellableCoroutine { c ->
        user.reloadWithCompletion { error ->
            if (error == null) c.resume(Unit)
            else throw toAuthException(error)
        }
    }

    override suspend fun sendEmailVerification(settings: ActionCodeSettings?): Unit =
        suspendCancellableCoroutine { c ->
            val completion: (NSError?) -> Unit = { error ->
                if (error == null) c.resume(Unit) else throw toAuthException(error)
            }
            if (settings == null) user.sendEmailVerificationWithCompletion(completion)
            else user.sendEmailVerificationWithActionCodeSettings(
                toFIRCodeSettings(settings), completion
            )
        }

    override suspend fun unlinkMethod(provider: AuthProvider): Unit =
        suspendCancellableCoroutine { c ->
            user.unlinkFromProvider(toAuthString(provider)) { data, error ->
                if (data != null) c.resume(Unit)
                else throw toAuthException(error!!)
            }
        }

    override suspend fun updateEmail(email: String): Unit = suspendCancellableCoroutine { c ->
        user.updateEmail(email) { error ->
            if (error == null) c.resume(Unit)
            else throw toAuthException(error)
        }
    }

    override suspend fun updatePassword(password: String): Unit =
        suspendCancellableCoroutine { c ->
            user.updatePassword(password) { error ->
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
                if (error == null) c.resume(Unit)
                else throw toAuthException(error)
            }
        }

    override suspend fun verifyBeforeUpdateEmail(
        newEmail: String,
        settings: ActionCodeSettings?
    ): Unit = suspendCancellableCoroutine { c ->
        val completion: (NSError?) -> Unit = { error ->
            if (error == null) c.resume(Unit)
            else throw toAuthException(error)
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

private enum class AuthAction { SignIn, Reauthenticate, Link }

private suspend fun getAltResult(method: AuthMethod, auth: FIRAuth, action: AuthAction)
        : FIRAuthDataResult = when (method) {
    AuthMethod.Annonymous -> getAnnonymousResult(auth, action)
    is AuthMethod.Custom -> getCustomResult(method, auth, action)
    is AuthMethod.EmailLink -> getEmailLinkResult(method, auth, action)
    is AuthMethod.EmailPassword -> getEmailPasswordResult(method, auth, action)
    else -> error("Unsupported AuthMethod $method used")
}

private suspend fun getAnnonymousResult(
    auth: FIRAuth,
    action: AuthAction
): FIRAuthDataResult = suspendCancellableCoroutine { c ->
    if (action != AuthAction.SignIn) error("Annonymous reauthentication and linking are invalid operations")
    auth.signInAnonymouslyWithCompletion { data, error ->
        if (data != null) c.resume(data)
        else throw toAuthException(error!!)
    }
}

private suspend fun getCustomResult(
    method: AuthMethod.Custom,
    auth: FIRAuth,
    action: AuthAction
): FIRAuthDataResult = suspendCancellableCoroutine { c ->
    if (action != AuthAction.SignIn) error("Custom token reauthentication and linking are invalid operations")
    auth.signInWithCustomToken(method.token) { data, error ->
        if (data != null) c.resume(data)
        else throw toAuthException(error!!)
    }
}

private suspend fun getEmailPasswordResult(
    method: AuthMethod.EmailPassword,
    auth: FIRAuth,
    action: AuthAction
): FIRAuthDataResult = suspendCancellableCoroutine { c ->
    if (action == AuthAction.SignIn) auth.signInWithEmail(
        email = method.email,
        password = method.password
    ) { data, error ->
        if (data != null) c.resume(data)
        else auth.createUserWithEmail(method.email, method.password) { data, error ->
            if (data != null) c.resume(data) else throw toAuthException(error!!)
        }
    }
    else {
        val completion: (FIRAuthDataResult?, NSError?) -> Unit = { data, error ->
            if (data != null) c.resume(data)
            else throw toAuthException(error!!)
        }
        val credential =
            FIREmailAuthProvider.credentialWithEmail(method.email, password = method.password)
        if (action == AuthAction.Reauthenticate)
            auth.currentUser!!.reauthenticateWithCredential(credential, completion)
        else auth.currentUser!!.linkWithCredential(credential, completion)
    }
}

private suspend fun getEmailLinkResult(
    method: AuthMethod.EmailLink,
    auth: FIRAuth,
    action: AuthAction
): FIRAuthDataResult = suspendCancellableCoroutine { c ->
    if (action == AuthAction.SignIn) auth.signInWithEmail(
        email = method.email,
        link = method.link
    ) { data, error ->
        if (data != null) c.resume(data)
        else throw toAuthException(error!!)
    }
    else {
        val completion: (FIRAuthDataResult?, NSError?) -> Unit = { data, error ->
            if (data != null) c.resume(data)
            else throw toAuthException(error!!)
        }
        val credential = FIREmailAuthProvider.credentialWithEmail(method.email, link = method.link)
        if (action == AuthAction.Reauthenticate)
            auth.currentUser!!.reauthenticateWithCredential(credential, completion)
        else auth.currentUser!!.linkWithCredential(credential, completion)
    }
}

private suspend fun getOAuthCredential(oAuthProvider: FIROAuthProvider): FIRAuthCredential =
    suspendCancellableCoroutine { c ->
        oAuthProvider.getCredentialWithUIDelegate(null) { data, error ->
            if (error != null) throw toAuthException(error)
            c.resume(data!!)
        }
    }

private suspend fun getTwitterCredential(
    method: AuthMethod.Twitter,
    auth: FIRAuth,
): FIRAuthCredential {
    val provider: FIROAuthProvider =
        FIROAuthProvider.providerWithProviderID("twitter.com", auth)
    return getOAuthCredential(
        provider.apply {
            setCustomParameters(buildMap {
                if (method.locale != null) put("lang", method.locale)
            } as Map<Any?, *>)
        })
}


private suspend fun getGitHubCredential(
    method: AuthMethod.GitHub,
    auth: FIRAuth,
): FIRAuthCredential {
    val provider: FIROAuthProvider = FIROAuthProvider.providerWithProviderID("github.com", auth)
    return getOAuthCredential(
        provider.apply {
            scopes = buildList {
                if (method.requestEmail) add("user:email")
            }
            setCustomParameters(buildMap {
                if (method.loginHint != null) put("login", method.loginHint)
                if (method.allowSignUp) put("allow_signup", "true")
            } as Map<Any?, *>)

        })
}

private suspend fun getAppleCredential(
    method: AuthMethod.Apple,
): FIRAuthCredential = suspendCancellableCoroutine { c ->
    val nonce = randomNonceString()
    val provider = ASAuthorizationAppleIDProvider()
    val request = provider.createRequest()
    request.requestedScopes = buildList {
        if (method.requestEmail) add(ASAuthorizationScopeEmail)
        if (method.requestName) add(ASAuthorizationScopeFullName)
    }
    request.nonce = sha256(nonce)

    val controller = ASAuthorizationController(listOf(request))
    val delegate = AppleDelegate(method.ui as UIViewController) { authorization ->
        if (authorization == null)
            throw FirebaseAuthException(FirebaseAuthException.Code.Unknown)
        else {
            val appleIDCredential = (authorization.credential as? ASAuthorizationAppleIDCredential)
                ?: return@AppleDelegate

            val appleIDToken = appleIDCredential.identityToken
                ?: run { println("Unable to fetch identity token"); return@AppleDelegate }

            val idToken = NSString.create(appleIDToken, NSUTF8StringEncoding)
                ?: error("Unable to serialize token string from data: $appleIDToken")

            // Initialize a Firebase credential.
            c.resume(
                FIROAuthProvider.credentialWithProviderID(
                    "apple.com",
                    IDToken = idToken.toString(),
                    rawNonce = nonce
                )
            )
        }
    }
    controller.delegate = delegate
    controller.presentationContextProvider = delegate
    controller.performRequests()
}

private fun sha256(input: String): String {
    val hashed = StringBuilder()
    memScoped {
        val string = input.utf8
        val result = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)
        CC_SHA256(string, string.size.toUInt(), result)

        (0 until CC_SHA256_DIGEST_LENGTH).map {
            hashed.append(NSString.stringWithFormat("%02x", result[it]))
        }
    }
    return hashed.toString()
}

private fun randomNonceString(): String {
    val charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._"
    var result = ""
    var remainingLength = 32

    memScoped {
        while (remainingLength > 0) {
            val randoms: List<Byte> = (0 until 16).map {
                val random = ByteArray(1).toCValues()
                val errorCode = SecRandomCopyBytes(kSecRandomDefault, 1, random)
                if (errorCode != errSecSuccess)
                    throw(FirebaseAuthException(FirebaseAuthException.Code.Unknown))
                random.getBytes()[0]
            }

            randoms.forEach { random ->
                if (remainingLength == 0) return@forEach

                if (random < charset.count()) {
                    result += (charset[random.toInt()])
                    remainingLength -= 1
                }
            }
        }
    }

    return result
}

private suspend fun getYahooCredential(method: AuthMethod.Yahoo, auth: FIRAuth):
        FIRAuthCredential {
    val provider: FIROAuthProvider = FIROAuthProvider.providerWithProviderID("yahoo.com", auth)
    return getOAuthCredential(
        provider.apply {
            scopes = buildList {
                if (method.requestEmail) add("email")
                if (method.requestProfile) add("profile")
            }
            setCustomParameters(buildMap {
                if (method.language != null) put("language", method.language)
                if (method.prompt != null) put("prompt", method.prompt)
                if (method.maxAge != null) put("max_age", method.maxAge.toString())
            } as Map<Any?, *>)

        })

}

private class AppleDelegate(val ui: UIViewController, val onComplete: (ASAuthorization?) -> Unit) : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {
    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError
    ) {
        onComplete(null)
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization
    ) {
        onComplete(didCompleteWithAuthorization)
    }

    override fun `class`(): ObjCClass = TODO("Not yet implemented")

    override fun description(): String? = null

    override fun hash(): NSUInteger = hashCode().toULong()

    override fun isEqual(`object`: Any?): Boolean {
        return `object`.hashCode() == hashCode()
    }

    override fun isKindOfClass(aClass: ObjCClass?): Boolean = TODO("Not yet implemented")

    override fun isMemberOfClass(aClass: ObjCClass?): Boolean = TODO("Not yet implemented")

    override fun isProxy(): Boolean = TODO("Not yet implemented")

    override fun performSelector(aSelector: COpaquePointer?): Any =
        TODO("Not yet implemented")

    override fun performSelector(aSelector: COpaquePointer?, withObject: Any?, _withObject: Any?)
            : Any = TODO("Not yet implemented")

    override fun performSelector(aSelector: COpaquePointer?, withObject: Any?): Any =
        TODO("Not yet implemented")

    override fun respondsToSelector(aSelector: COpaquePointer?): Boolean =
        TODO("Not yet implemented")

    @Suppress
    override fun superclass(): ObjCClass? =
        (ASAuthorizationControllerDelegateProtocol::`class`)(this)

    override fun conformsToProtocol(aProtocol: Protocol?): Boolean = TODO("Not yet implemented")
    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor =
        ui.view.window
}


private suspend fun toCredentialHolder(method: AuthMethod, auth: FIRAuth): CredentialHolder =
    when (method) {
        is AuthMethod.Apple -> CredentialHolder(getAppleCredential(method))
        AuthMethod.Annonymous -> CredentialHolder(method = method)
        is AuthMethod.Custom -> CredentialHolder(method = method)
        is AuthMethod.EmailLink -> CredentialHolder(method = method)
        is AuthMethod.EmailPassword -> CredentialHolder(method = method)
        is AuthMethod.GitHub -> CredentialHolder(
            getGitHubCredential(method, auth)
        )
        is AuthMethod.Google -> suspendCancellableCoroutine { c ->
            GIDSignIn.sharedInstance.signInWithConfiguration(
                GIDConfiguration(method.webClientId),
                method.ui as UIViewController
            ) { data, error ->
                if (error != null) throw toAuthException(error)
                c.resume(
                    CredentialHolder(
                        FIRGoogleAuthProvider.credentialWithIDToken(
                            data!!.authentication.idToken!!, data.authentication.accessToken
                        )
                    )
                )
            }
        }
        is AuthMethod.Twitter -> CredentialHolder(getTwitterCredential(method, auth))
        is AuthMethod.Yahoo -> CredentialHolder(getYahooCredential(method, auth))
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


private fun toUserInfo(result: FIRAuthDataResult): AdditionalUserInfo = AdditionalUserInfo(
    (result.additionalUserInfo!!.profile ?: emptyMap<String, Any>()) as Map<String, Any>,
    result.additionalUserInfo!!.username,
    result.additionalUserInfo!!.newUser
)

private fun toFIRSettings(settings: FirebaseAuth.Config.FirebaseAuthSettings): FIRAuthSettings =
    FIRAuthSettings().apply {
        appVerificationDisabledForTesting = settings.appVerificationDisabledForTesting
    }

private fun toAuthSettings(settings: FIRAuthSettings): FirebaseAuth.Config.FirebaseAuthSettings =
    FirebaseAuth.Config.FirebaseAuthSettings(settings.appVerificationDisabledForTesting)

private class CredentialHolder(
    val credential: FIRAuthCredential? = null, val method: AuthMethod? = null
)

internal actual val firebaseAuthInstance: FirebaseAuth by lazy { FirebaseAuthImpl(FIRAuth.auth()) }

@Suppress("TYPE_MISMATCH")
internal actual fun getAuthInstance(app: FirebaseApp): FirebaseAuth =
    FirebaseAuthImpl(FIRAuth.authWithApp(app.app))
