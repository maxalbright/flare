package enchant.flare

import kotlin.test.Test
import kotlin.test.assertTrue

class JsGreetingTest {

    @Test
    fun testExample() {
        assertTrue(Greeting().greeting().contains("JS"), "Check JS is mentioned")
    }
}