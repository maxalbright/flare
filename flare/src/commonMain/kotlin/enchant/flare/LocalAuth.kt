package enchant.flare

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlin.random.Random

class LocalAuth : FirebaseAuth {

    private val users: MutableMap<AuthProvider, MutableMap<String, FirebaseUserImpl>> =
        mutableMapOf()

    override suspend fun applyActionCode(code: String) {
        if (code !in actionCodes) throw AuthException(
            AuthException.Code.ActionCode, "The action code is invalid: $code"
        )
        when (val actionCode = actionCodes[code]) {
            is ActionCodeInfo.PasswordReset -> throw AuthException(
                AuthException.Code.ActionCode,
                "Password reset code cannot be applied: $code"
            )
            is ActionCodeInfo.VerifyBeforeChangeEmail -> {
                val user = _currentUser ?: throw (AuthException(
                    AuthException.Code.InvalidUser, "No user is currently signed in"
                ))
                val method =
                    (user.methods.first { it is AuthMethod.EmailPassword } as AuthMethod.EmailPassword)
                user.methods -= method
                user.methods += AuthMethod.EmailPassword(actionCode.email, method.password)
            }
            is ActionCodeInfo.VerifyEmail -> Unit
            else -> error("Not implemented")
        }
        actionCodes -= code
    }

    private val actionCodes: MutableMap<String, ActionCodeInfo> = mutableMapOf()
    private var codeAmount = 0

    override suspend fun checkActionCode(code: String): ActionCodeInfo =
        actionCodes[code] ?: throw AuthException(
            AuthException.Code.ActionCode, "The supplied action code is invalid: $code"
        )

    override suspend fun confirmPasswordReset(code: String, newPassword: String) {
        if (code !in actionCodes && actionCodes[code] !is ActionCodeInfo.PasswordReset) throw AuthException(
            AuthException.Code.ActionCode, "The password reset code is invalid: $code"
        )
        val user = _currentUser ?: throw (AuthException(
            AuthException.Code.InvalidUser, "No user is currently signed in"
        ))
        val method =
            (user.methods.first { it is AuthMethod.EmailPassword } as AuthMethod.EmailPassword)
        actionCodes -= code
        user.methods -= method
        user.methods += AuthMethod.EmailPassword(method.email, newPassword)
    }

    override suspend fun fetchSignInProvidersForEmail(email: String): List<AuthProvider> =
        users.filter { it.value.contains(email) }.keys.toList()

    override val currentUser: FirebaseUser? get() = _currentUser
    private var _currentUser: FirebaseUserImpl? = null

    /** Generates a code equal to 100000 plus the amount that have previously been generated */
    override suspend fun sendPasswordResetEmail(email: String, settings: ActionCodeSettings?) {
        actionCodes[(100000 + codeAmount++).toString()] = ActionCodeInfo.PasswordReset(email)
    }

    override suspend fun sendSignInLinkToEmail(email: String, settings: ActionCodeSettings) {
        actionCodes[(100000 + codeAmount++).toString()] = ActionCodeInfo.SignInWithEmailLink()
    }

    /** For OAuth providers (ex. Apple, Google, GitHub), pass in the user's email into the ui parameter */
    override suspend fun signIn(method: AuthMethod): AdditionalUserInfo {

        val methodUsers = users.getOrPut(method.provider) { mutableMapOf() }
        if (methodUsers[getId(method)]?.methods?.contains(method) == false) throw AuthException(
            AuthException.Code.InvalidCredentials,
            if (method is AuthMethod.EmailPassword) "Could not authenticate with invalid password"
            else "User cannot be authenticated with method: $method. If using email password, the password might be incorrect"
        )

        var isNewUser = false
        if (method is AuthMethod.EmailLink) {
            val user = users.values.firstOrNull { method.email in it }
                ?.get(method.email) ?: throw AuthException(
                AuthException.Code.InvalidUser,
                "Email link can only be added to an existing user"
            )
            if (!actionCodes.contains(method.link)) throw AuthException(
                AuthException.Code.InvalidCredentials,
                "Email link provided is invalid: ${method.link}"
            )
            actionCodes -= method.link
            user.methods += AuthMethod.EmailLink("", "")

            _currentUser = user
            _authChanges.value++
            return AdditionalUserInfo(null, null, false)
        }
        val user = methodUsers.getOrPut(getId(method)) {
            if (users.values.any { it.contains(getId(method)) }) throw AuthException(
                AuthException.Code.UserCollision,
                "User with email ${getId(method)} already exists"
            )
            isNewUser = true
            FirebaseUserImpl(method)
        }
        if (!isNewUser) {
            user._lastSignInTimestamp = Clock.System.now().toEpochMilliseconds()
            user.lastMethod = method
        }
        _currentUser = user
        _authChanges.value++
        return AdditionalUserInfo(null, null, isNewUser)
    }

    override suspend fun verifyPasswordResetCode(code: String): String {
        if (code !in actionCodes && actionCodes[code] !is ActionCodeInfo.PasswordReset) throw AuthException(
            AuthException.Code.ActionCode, "The password reset code is invalid: $code"
        )
        return (actionCodes[code] as ActionCodeInfo.PasswordReset).email
    }

    override fun isSignInWithEmailLink(link: String): Boolean = false

    override fun signOut() {
        _currentUser = null
        _authChanges.value++
    }

    val _authChanges = MutableStateFlow(0)
    val _idTokenChanges = MutableStateFlow(0)

    override val config: FirebaseAuth.Config
        get() = object : FirebaseAuth.Config {

            override var settings: FirebaseAuth.Config.FirebaseAuthSettings =
                FirebaseAuth.Config.FirebaseAuthSettings()

            override fun onAuthStateChange(): Flow<Unit> = _authChanges.map { }

            override fun onIdTokenChange(): Flow<Unit> =
                _authChanges.combine(_idTokenChanges) { _, _ -> }

            override fun useAppLanguage() {}

            override fun useEmulator(host: String, port: Int) {}

            override var tenantId: String? = null
            override val languageCode: String? get() = null

        }

    private fun getId(method: AuthMethod): String = when (method) {
        is AuthMethod.Custom -> method.token
        is AuthMethod.EmailLink -> method.email
        is AuthMethod.EmailPassword -> method.email
        is AuthMethod.Apple -> method.ui as? String ?: throw AuthException(
            AuthException.Code.AuthWeb,
            "Need to pass in Apple account's email in ui parameter"
        )

        is AuthMethod.GitHub -> method.activity as? String ?: throw AuthException(
            AuthException.Code.AuthWeb,
            "Need to pass in GitHub account's email in activity parameter"
        )
        is AuthMethod.Google -> method.ui as? String ?: throw AuthException(
            AuthException.Code.AuthWeb,
            "Need to pass in GitHub account's email in ui parameter"
        )
        is AuthMethod.Twitter -> method.activity as? String ?: throw AuthException(
            AuthException.Code.AuthWeb,
            "Need to pass in GitHub account's email in activity parameter"
        )
        is AuthMethod.Yahoo -> method.activity as? String ?: throw AuthException(
            AuthException.Code.AuthWeb,
            "Need to pass in GitHub account's email in activity parameter"
        )
        else -> "singleId"
    }

    private open inner class FirebaseUserImpl(var lastMethod: AuthMethod) : FirebaseUser {


        override suspend fun delete() {
            methods.forEach { method ->
                users[method.provider]?.get(getId(method)) ?: throw AuthException(
                    AuthException.Code.InvalidUser,
                    "User with id [${getId(method)}] could not be deleted because they do not exist"
                )
                users[method.provider]!!.remove(getId(method))
            }
            _currentUser = null
        }

        private var _displayName: String? = null
        override val displayName: String? get() = _displayName

        override val email: String?
            get() = (methods.firstOrNull { it is AuthMethod.EmailPassword }
                    as? AuthMethod.EmailPassword)?.email

        final override val creationTimestamp: Long = Clock.System.now().toEpochMilliseconds()
        override val lastSignInTimestamp: Long get() = _lastSignInTimestamp
        var _lastSignInTimestamp: Long = creationTimestamp

        override val phoneNumber: String? = null
        private var _photoUrl: String? = null
        override val photoUrl: String? get() = _photoUrl
        override val providerData: List<UserInfo>
            get() = listOf()
        override val tenantId: String? get() = this@LocalAuth.config.tenantId
        override val uid: String = buildString {
            repeat(28) {
                val c = Random.nextInt(62)
                append(
                    (c + when {
                        c >= 36 -> 61
                        c >= 10 -> 55
                        else -> 48
                    }).toChar()
                )
            }
        }

        private fun generateToken(): String = buildString {
            repeat(64) {
                val i = Random.nextInt(16)
                append((i + 48 + (if (i > 9) 39 else 0)).toChar())
            }
        }

        private var idToken = TokenResult(
            authTimestamp = creationTimestamp,
            claims = mapOf(),
            expirationTimestamp = creationTimestamp + 3_600_000,
            issuedAtTimestamp = creationTimestamp,
            signInProvider = if (lastMethod.provider !in setOf(
                    AuthProvider.Anonymous,
                    AuthProvider.Custom
                )
            ) lastMethod.provider else null,
            signInSecondFactor = null,
            token = generateToken()
        )

        override suspend fun getIdToken(forceRefresh: Boolean): TokenResult {
            if (forceRefresh) {
                val time = Clock.System.now().toEpochMilliseconds()
                idToken = TokenResult(
                    authTimestamp = _lastSignInTimestamp,
                    claims = mapOf(),
                    expirationTimestamp = time + 3_600_000,
                    issuedAtTimestamp = time,
                    signInProvider = if (lastMethod.provider !in setOf(
                            AuthProvider.Anonymous,
                            AuthProvider.Custom
                        )
                    ) lastMethod.provider else null,
                    signInSecondFactor = null,
                    token = generateToken()
                )
                _idTokenChanges.value = _idTokenChanges.value++
            }
            return idToken
        }

        override val isAnonymous: Boolean = lastMethod is AuthMethod.Anonymous

        val methods: MutableSet<AuthMethod> = mutableSetOf(lastMethod)

        override suspend fun linkMethod(method: AuthMethod): AdditionalUserInfo {

            if (methods.any { it.provider == method.provider }) throw AuthException(
                AuthException.Code.InvalidCredentials,
                "The current user already has the provider for method: $method"
            )
            methods.add(method)
            users.getOrPut(method.provider) { mutableMapOf() }[getId(method)] = this
            return AdditionalUserInfo(null, null, false)
        }

        override suspend fun reauthenticate(method: AuthMethod): AdditionalUserInfo {
            if (users[method.provider]?.get(getId(method))?.methods?.contains(method) == true)
                return AdditionalUserInfo(null, null, false)
            else throw AuthException(
                AuthException.Code.InvalidCredentials,
                "Cannot reauthenticate with invalid method: $method"
            )
        }

        override suspend fun reload() {
            _authChanges.value = _authChanges.value++
        }

        override suspend fun sendEmailVerification(settings: ActionCodeSettings?) {
            actionCodes[(100000 + codeAmount++).toString()] = ActionCodeInfo.VerifyEmail(email!!)
        }

        override suspend fun unlinkMethod(provider: AuthProvider) {
            if (!methods.any { it.provider == provider }) throw AuthException(
                AuthException.Code.InvalidCredentials,
                "The current user does not have the provider linked: $provider"
            )
            val method = methods.first { it.provider == provider }
            methods.remove(method)
            users[provider]!! -= getId(method)
        }

        override suspend fun updateEmail(email: String) {
            if (!methods.any { it is AuthMethod.EmailPassword }) throw AuthException(
                AuthException.Code.Unknown,
                "Cannot update the email of a non email/password user"
            )
            val oldMethod =
                methods.first { it is AuthMethod.EmailPassword } as AuthMethod.EmailPassword
            methods.remove(oldMethod)
            methods.add(AuthMethod.EmailPassword(email, oldMethod.password))
        }

        override suspend fun updatePassword(password: String) {
            if (!methods.any { it is AuthMethod.EmailPassword }) throw AuthException(
                AuthException.Code.Unknown,
                "Cannot update the email of a non email/password user"
            )
            val oldMethod =
                methods.first { it is AuthMethod.EmailPassword } as AuthMethod.EmailPassword
            methods.remove(oldMethod)
            methods.add(AuthMethod.EmailPassword(oldMethod.email, password))
        }

        override suspend fun updateProfile(displayName: String?, photoUrl: String?) {
            _displayName = displayName ?: _displayName
            _photoUrl = photoUrl ?: _photoUrl
        }

        /** Usually this works automagically, but in LocalAuth, call applyActionCode with the
         * appropriate action code to update the email
         */
        override suspend fun verifyBeforeUpdateEmail(
            newEmail: String,
            settings: ActionCodeSettings?
        ) {
            actionCodes[(100000 + codeAmount++).toString()] =
                ActionCodeInfo.VerifyBeforeChangeEmail(email!!, newEmail)

        }

    }
}