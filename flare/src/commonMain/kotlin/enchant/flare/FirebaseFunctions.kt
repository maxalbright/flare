package enchant.flare



interface FirebaseFunctions {
    /**
    * Triggers Firebase cloud functions to run on the server
    *
    * @param name the name of the cloud function
    * @param data the optional "input" to the function
    * @param timeout the optional maximum time that the function is allowed to run 
    *
    * @throws FirebaseFunctionsException for errors thrown
    */    
    suspend fun call(name: String, data: Any? = null, timeout: Long? = null): Any?

    interface Config {
        fun useEmulator(host: String, port: Int)
    }
    val config: Config

    companion object {
        val instance: FirebaseFunctions = firebaseFunctionsInstance
        fun getInstance(app: FirebaseApp) = getFunctionsInstance(app)
    }
}


/**
* This class represents the type of error 
* that Flare's FirebaseFunctions throws.
*
* @param code the code that is included in the error
* @param description a string that provides a description of the error
* in addition to code
*/

class FunctionsException(val code: Code, val description: String? = null) :
    Exception("Firebase functions operation failed with code ${code.name}: $description") {

    enum class Code {
        Aborted,
        AlreadyExists,
        DataLoss,
        DeadlineExceeded,
        FailedPrecondition,
        Internal,
        InvalidArgument,
        NotFound,
        OutOfRange,
        PermissionDenied,
        ResourceExhausted,
        Unauthenticated,
        Unavailable,
        Unimplemented,
        Unknown
    }
}
internal expect val firebaseFunctionsInstance: FirebaseFunctions
internal expect fun getFunctionsInstance(app: FirebaseApp): FirebaseFunctions