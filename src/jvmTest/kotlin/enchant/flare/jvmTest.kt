package enchant.flare

import kotlin.test.Test
import kotlin.test.assertTrue

class JvmGreetingTest {

    @Test
    fun testExample() {
        assertTrue(Greeting().greeting().contains("JVM"), "Check JVM is mentioned")
    }
}