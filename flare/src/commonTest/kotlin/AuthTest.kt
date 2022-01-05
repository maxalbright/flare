import enchant.flare.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AuthTest {
    val useLocal = false
    val auth: FirebaseAuth by lazy {
        if (useLocal) LocalAuth()
        else {
            if (FirebaseApp.getApps(context).isEmpty()) {

                //FirebaseApp.initialize(
                   // context, null, FirebaseOptions(

                   // ))
            }
            FirebaseAuth.instance
        }
    }

    @Test
    fun signInWithEmail() = runTest {
        val info = auth.signIn(AuthMethod.EmailPassword("ethan@hi.com", "mypassword"))
        assertTrue(info.isNewUser)
        assertEquals("ethan@hi.com", auth.currentUser?.email)
    }

    @Test
    fun signOut() = runTest {
        auth.signIn(AuthMethod.EmailPassword("ethan@hi.com", "mypassword"))
        auth.signOut()
        assertNull(auth.currentUser)
    }

    @Test
    fun failSignIn() = runTest {
        auth.signIn(AuthMethod.EmailPassword("ethan@hi.com", "mypassword"))
        auth.signOut()
        try {
            auth.signIn(AuthMethod.EmailPassword("ethan@hi.com", "mywrongpassword"))
            fail()
        } catch (e: AuthException) {

        }
    }
}