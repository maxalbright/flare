package enchant.flare

interface FirebaseFunctions {
    suspend fun <T> call(name: String, data: Any? = null, timeout: Long? = null): T
    var timeout: Long
    fun useEmulator(host: String, port: Int)
}

class FunctionsException(val code: Code, val data: Any?): Exception("Firestore database operation failed with code: $code") {

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