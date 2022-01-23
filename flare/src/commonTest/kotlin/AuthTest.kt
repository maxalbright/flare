import enchant.flare.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthTest : FlareTest() {
    lateinit var auth: FirebaseAuth

    @BeforeTest
    fun initializeAuth() {
        auth = if (useLocal) LocalAuth() else FirebaseAuth.instance
    }

    @Test
    fun signInProviders() = runTest {
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        val providers = auth.fetchSignInProvidersForEmail("$testId@hi.com")
        assertContentEquals(listOf(AuthProvider.EmailPassword), providers)
    }

    @Test
    fun resetPassword() = runTest {
        if (!useLocal) println("Skipped since test only supports LocalAuth").also { return@runTest }
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        auth.sendPasswordResetEmail("$testId@hi.com")
        auth.verifyPasswordResetCode("100000")
        auth.confirmPasswordReset("100000", "mypassword2")
        auth.signOut()
        val info = auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword2"))
        assertFalse(info.isNewUser, "User should not be recreated after password change")
        assertNotNull(auth.currentUser, "User should be signed in")
    }

    @Test
    fun signInWithEmailLink() = runTest {
        if (!useLocal) println("Skipped since test only supports LocalAuth").also { return@runTest }
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        auth.signOut()
        auth.sendSignInLinkToEmail("$testId@hi.com", ActionCodeSettings())
        val info = auth.signIn(AuthMethod.EmailLink("$testId@hi.com", "100000"))
        assertFalse(info.isNewUser, "User should not be recreated after email link sign-in")
        assertNotNull(auth.currentUser, "User should be signed in")
    }

    @Test
    fun signInWithEmail() = runTest {
        val info = auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        assertTrue(info.isNewUser)
        assertEquals("$testId@hi.com", auth.currentUser?.email)
        assertNotNull(auth.currentUser, "User should be signed in")
    }

    @Test
    fun signInAnonymous() = runTest {
        auth.signIn(AuthMethod.Anonymous)
        assertNotNull(auth.currentUser, "User should be signed in")
        assertTrue(auth.currentUser!!.isAnonymous)
    }

    @Test
    fun signInWithWeb() = runTest {
        if (!useLocal) println("Skipped since test only supports LocalAuth").also { return@runTest }
        auth.signIn(AuthMethod.Google(webClientId = "", ui = "$testId@hi.com"))
        assertNotNull(auth.currentUser, "User should be signed in")
        auth.signOut()
        val info = auth.signIn(AuthMethod.Google(webClientId = "", ui = "$testId@hi.com"))
        assertFalse(info.isNewUser, "User should not be recreated after web sign-in")
    }

    @Test
    fun authStateChanges() = runTest {
        var updates = 0
        val job = launch {
            auth.config.onAuthStateChange().collect {
                updates++
                println("update$updates")
            }
        }
        repeat(2) {
            auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
            yield()
            auth.signOut()
            yield()
        }
        assertEquals(4, updates, "Five auth state changes should have occurred")
        job.cancel()

    }

    @Test
    fun deleteUser() = runTest {
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        auth.currentUser!!.delete()
        assertNull(auth.currentUser, "User should be signed out after their account is deleted")
        val info = auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        assertTrue(info.isNewUser, "User should be recreated after deleted")
    }

    @Test
    fun userInfo() = runTest {
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        val user = auth.currentUser ?: fail("User should be signed in")
        assertNull(user.displayName)
        assertNull(user.phoneNumber)
        assertNull(user.photoUrl)
        assertNull(user.tenantId)
        assertEquals("$testId@hi.com", user.email)
        assertEquals(
            user.creationTimestamp,
            user.lastSignInTimestamp,
            "Check the first sign in time is the creation time"
        )
        assertTrue("Uid needs to be 28 character letter and digit identifier") {
            user.uid.all(Char::isLetterOrDigit) && user.uid.length == 28
        }
    }

    @Test
    fun linkMethod() = runTest {
        if (!useLocal) println("Skipped since test only supports LocalAuth").also { return@runTest }
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        val user = auth.currentUser ?: fail("User should be signed in")
        user.linkMethod(AuthMethod.GitHub("$testId@hi.com"))
        val providers = auth.fetchSignInProvidersForEmail("$testId@hi.com")
        assertEquals(
            setOf(AuthProvider.EmailPassword, AuthProvider.GitHub),
            providers.toSet(),
            "User's methods should include EmailPassword and GitHub after GitHub is linked"
        )
        auth.signOut()
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        auth.signOut()
        auth.signIn(AuthMethod.GitHub("$testId@hi.com"))
    }

    @Test
    fun unlinkMethod() = runTest {
        if (!useLocal) println("Skipped since test only supports LocalAuth").also { return@runTest }
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        val user = auth.currentUser ?: fail("User should be signed in")
        user.linkMethod(AuthMethod.GitHub("$testId@hi.com"))
        user.unlinkMethod(AuthProvider.GitHub)
        val providers = auth.fetchSignInProvidersForEmail("$testId@hi.com")
        assertEquals(
            listOf(AuthProvider.EmailPassword), providers,
            "User's methods should include EmailPassword after GitHub is unlinked"
        )
        auth.signOut()
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        auth.signOut()
        assertFails {
            auth.signIn(AuthMethod.GitHub("$testId@hi.com"))
        }
    }

    @Test
    fun reauthenticate() = runTest {
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        val user = auth.currentUser ?: fail("User should be signed in")
        user.reauthenticate(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
    }

    @Test
    fun updateEmail() = runTest {
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        val user = auth.currentUser ?: fail("User should be signed in")
        user.updateEmail("$testId@hi2.com")
        assertEquals("$testId@hi2.com", user.email)
        auth.signOut()
        auth.signIn(AuthMethod.EmailPassword("$testId@hi2.com", "mypassword"))
        auth.currentUser ?: fail("User should be signed in")
    }

    @Test
    fun updatePassword() = runTest {
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        val user = auth.currentUser ?: fail("User should be signed in")
        user.updatePassword("mypassword2")
        auth.signOut()
        assertFails { auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword")) }
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword2"))
        auth.currentUser ?: fail("User should be signed in")
    }

    @Test
    fun updateProfile() = runTest {
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        val user = auth.currentUser ?: fail("User should be signed in")
        user.updateProfile("My Name", "https://testphoto.com")
        assertEquals("My Name", user.displayName, "Ensure name was changed")
        assertEquals("https://testphoto.com", user.photoUrl, "Ensure photo url was changed")
    }


    @Test
    fun signOut() = runTest {
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        auth.signOut()
        assertNull(auth.currentUser)
    }

    @Test
    fun failSignIn() = runTest {
        auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mypassword"))
        auth.signOut()
        try {
            auth.signIn(AuthMethod.EmailPassword("$testId@hi.com", "mywrongpassword"))
            fail()
        } catch (e: AuthException) {

        }
    }
}