package enchant.flare

import kotlin.test.assertTrue
import kotlin.test.Test

class AndroidGreetingTest {

    @Test
    fun testExample() {
        assertTrue(Greeting().greeting().contains("Android"), "Check Android is mentioned")
    }
}