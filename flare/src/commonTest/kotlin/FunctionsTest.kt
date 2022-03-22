import enchant.flare.FirebaseFunctions
import enchant.flare.FunctionsException
import enchant.flare.LocalFunctions
import kotlin.test.Test
import kotlin.test.assertEquals

class FunctionsTest : FlareTest() {

    @Test
    fun callFunction() = runTest {
        val functions: FirebaseFunctions = if (useLocal) MyLocalFunctions()
        else FirebaseFunctions.instance
        val response = functions.call("increment",1)
        assertEquals(2, response, "Ensure function incremented value")
    }
}

private class MyLocalFunctions : LocalFunctions() {
    override suspend fun callFunction(name: String, data: Any?): Any? {
        if (name == "increment") {
            return (data as Int) + 1
        } else {
            throw FunctionsException(FunctionsException.Code.NotFound, "Function not found")
        }
    }

}