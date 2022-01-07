package enchant.flare

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.firebase.functions.FirebaseFunctions as AndroidFunctions
import com.google.firebase.functions.FirebaseFunctionsException as AndroidException
import com.google.firebase.functions.FirebaseFunctionsException.Code as AndroidCode

private class FirebaseFunctionsImpl(private val functions: AndroidFunctions) : FirebaseFunctions {
    override suspend fun call(name: String, data: Any?, timeout: Long?): Any? =
        suspendCancellableCoroutine { c ->
            functions.getHttpsCallable(name).call(data).addOnCompleteListener {
                if (it.isSuccessful) c.resume(it.result.data)
                else functionsException(it.exception!!)
            }
        }

    override val config = object : FirebaseFunctions.Config {
        override fun useEmulator(host: String, port: Int): Unit = functions.useEmulator(host, port)
    }

    private fun functionsException(exception: Exception): Nothing {
        val code: FunctionsException.Code =
            if (exception is AndroidException) when (exception.code) {
                AndroidCode.INVALID_ARGUMENT -> FunctionsException.Code.InvalidArgument
                AndroidCode.ALREADY_EXISTS -> FunctionsException.Code.AlreadyExists
                AndroidCode.DEADLINE_EXCEEDED -> FunctionsException.Code.DeadlineExceeded
                AndroidCode.NOT_FOUND -> FunctionsException.Code.NotFound
                AndroidCode.PERMISSION_DENIED -> FunctionsException.Code.PermissionDenied
                AndroidCode.RESOURCE_EXHAUSTED -> FunctionsException.Code.ResourceExhausted
                AndroidCode.FAILED_PRECONDITION -> FunctionsException.Code.FailedPrecondition
                AndroidCode.ABORTED -> FunctionsException.Code.Aborted
                AndroidCode.OUT_OF_RANGE -> FunctionsException.Code.OutOfRange
                AndroidCode.UNIMPLEMENTED -> FunctionsException.Code.Unimplemented
                AndroidCode.INTERNAL -> FunctionsException.Code.Internal
                AndroidCode.UNAVAILABLE -> FunctionsException.Code.Unavailable
                AndroidCode.DATA_LOSS -> FunctionsException.Code.DataLoss
                AndroidCode.UNAUTHENTICATED -> FunctionsException.Code.Unauthenticated
                else -> FunctionsException.Code.Unknown
            } else FunctionsException.Code.Unknown
        throw FunctionsException(code, exception.message)
    }
}

internal actual val firebaseFunctionsInstance: FirebaseFunctions by lazy {
    FirebaseFunctionsImpl(AndroidFunctions.getInstance())
}
internal actual fun getFunctionsInstance(app: FirebaseApp): FirebaseFunctions =
    FirebaseFunctionsImpl(AndroidFunctions.getInstance(app.app))