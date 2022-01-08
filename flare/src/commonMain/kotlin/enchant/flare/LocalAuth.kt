package enchant.flare

import kotlinx.datetime.Clock
import kotlin.properties.Delegates
import kotlin.random.Random

class LocalAuth : FirebaseAuth {

    private val users: MutableMap<AuthProvider, MutableMap<String, FirebaseUserImpl>> =
        mutableMapOf()

    override suspend fun applyActionCode(code: String) {
        TODO("Not yet implemented")
    }

    override suspend fun checkActionCode(code: String): ActionCodeInfo {
        TODO("Not yet implemented")
    }

    override suspend fun confirmPasswordReset(code: String, newPassword: String) {
        TODO("Not yet implemented")
    }

    override suspend fun fetchSignInProvidersForEmail(email: String): List<AuthProvider> {
        TODO("Not yet implemented")
    }

    override val currentUser: FirebaseUser? get() = _currentUser
    private var _currentUser: FirebaseUser? = null

    override suspend fun sendPasswordResetEmail(email: String, settings: ActionCodeSettings?) {
        TODO("Not yet implemented")
    }

    override suspend fun sendSignInLinkToEmail(email: String, settings: ActionCodeSettings) {
        TODO("Not yet implemented")
    }

    override suspend fun signIn(method: AuthMethod): AdditionalUserInfo {
        val infoMethod = when (method) {
            is AuthMethod.Apple -> AuthMethod.Apple(Unit)
            is AuthMethod.GitHub -> AuthMethod.GitHub()
            is AuthMethod.Google -> AuthMethod.Google(Unit, "")
            is AuthMethod.Twitter -> AuthMethod.Twitter()
            is AuthMethod.Yahoo -> AuthMethod.Yahoo()
            else -> method
        }
        val methodUsers = users.getOrPut(infoMethod.provider) { mutableMapOf() }
        var isNewUser = false
        val user = methodUsers.getOrPut(getId(method)) {
            isNewUser = true
            FirebaseUserImpl(infoMethod)
        }
        if (infoMethod != user.method) throw AuthException(
            AuthException.Code.InvalidUser,
            "Could not find a user with id [${getId(method)}] for method ${infoMethod.provider}"
        )
        _currentUser = user
        user._lastSignInTimestamp = Clock.System.now().toEpochMilliseconds()
        return AdditionalUserInfo(null, null, isNewUser)
    }

    override suspend fun verifyPasswordResetCode(code: String): String {
        TODO("Not yet implemented")
    }

    override fun isSignInWithEmailLink(link: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun signOut() {
        _currentUser = null
    }

    override val config: FirebaseAuth.Config
        get() = TODO("Not yet implemented")

    private fun getId(method: AuthMethod): String = when (method) {
        is AuthMethod.Custom -> method.token
        is AuthMethod.EmailLink -> method.email
        is AuthMethod.EmailPassword -> method.email
        else -> ""
    }

    private inner class FirebaseUserImpl(
        var method: AuthMethod,
        createTimestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) : FirebaseUser {

        override suspend fun delete() {
            users[method.provider]?.get(getId(method)) ?: throw AuthException(
                AuthException.Code.InvalidUser,
                "User with id [${getId(method)}] could not be deleted because they do not exist"
            )
            users[method.provider]!!.remove(getId(method))
        }

        override val displayName: String?
            get() = TODO("Not yet implemented")

        override val email: String? = when (method) {
            is AuthMethod.EmailPassword -> (method as AuthMethod.EmailPassword).email
            is AuthMethod.EmailLink -> (method as AuthMethod.EmailLink).email
            else -> null
        }

        override suspend fun getIdToken(forceRefresh: Boolean): TokenResult {
            TODO("Not yet implemented")
        }

        override val creationTimestamp: Long = createTimestamp
        override val lastSignInTimestamp: Long get() = _lastSignInTimestamp
        var _lastSignInTimestamp: Long by Delegates.notNull()

        override val phoneNumber: String?
            get() = TODO("Not yet implemented")
        override val photoUrl: String?
            get() = TODO("Not yet implemented")
        override val providerData: List<UserInfo>
            get() = TODO("Not yet implemented")
        override val tenantId: String?
            get() = TODO("Not yet implemented")
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
        override val isAnonymous: Boolean = method is AuthMethod.Anonymous

        override suspend fun linkMethod(method: AuthMethod): AdditionalUserInfo {
            TODO("Not yet implemented")
        }

        override suspend fun reauthenticate(method: AuthMethod): AdditionalUserInfo {
            TODO("Not yet implemented")
        }

        override suspend fun reload() {
            TODO("Not yet implemented")
        }

        override suspend fun sendEmailVerification(settings: ActionCodeSettings?) {
            TODO("Not yet implemented")
        }

        override suspend fun unlinkMethod(provider: AuthProvider) {
            TODO("Not yet implemented")
        }

        override suspend fun updateEmail(email: String) {
            if (method !is AuthMethod.EmailPassword) throw AuthException(
                AuthException.Code.Unknown,
                "Cannot update the email of a non email/password user"
            )
            val oldMethod = method as AuthMethod.EmailPassword
            method = AuthMethod.EmailPassword(oldMethod.email, oldMethod.password)
        }

        override suspend fun updatePassword(password: String) {
            if (method !is AuthMethod.EmailPassword) throw AuthException(
                AuthException.Code.Unknown,
                "Cannot update the password of a non email/password user"
            )
            val oldMethod = method as AuthMethod.EmailPassword
            method = AuthMethod.EmailPassword(oldMethod.email, oldMethod.password)
        }

        override suspend fun updateProfile(displayName: String?, photoUrl: String?) {
            TODO("Not yet implemented")
        }

        override suspend fun verifyBeforeUpdateEmail(
            newEmail: String,
            settings: ActionCodeSettings?
        ) {
            TODO("Not yet implemented")
        }

    }
}