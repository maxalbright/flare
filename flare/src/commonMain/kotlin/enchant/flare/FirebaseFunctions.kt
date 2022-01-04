package enchant.flare

interface FirebaseFunctions {
    suspend fun call(name: String, data: Any? = null, timeout: Long? = null): Any?
    fun useEmulator(host: String, port: Int)
}

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

    companion object {
        val instance: FirebaseFunctions = firebaseFunctionsInstance
        fun getInstance(app: FirebaseApp) = getFunctionsInstance(app)
    }
}
internal expect val firebaseFunctionsInstance: FirebaseFunctions
internal expect fun getFunctionsInstance(app: FirebaseApp): FirebaseFunctions