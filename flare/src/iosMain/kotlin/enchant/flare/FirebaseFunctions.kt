package enchant.flare

import cocoapods.FirebaseFunctions.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume

private class FirebaseFunctionsImpl(val functions: FIRFunctions) : FirebaseFunctions {
    override suspend fun call(name: String, data: Any?, timeout: Long?): Any? =
        suspendCancellableCoroutine { c ->
            functions.HTTPSCallableWithName(name).callWithObject(data) { data, error ->
                if (data != null) c.resume(data)
                else functionsException(error!!)
            }
        }

    override fun useEmulator(host: String, port: Int): Unit =
        functions.useEmulatorWithHost(host, port.toLong())

    private fun functionsException(exception: NSError): Nothing {
        val code: FunctionsException.Code =
            when (exception.code) {
                FIRFunctionsErrorCodeInvalidArgument -> FunctionsException.Code.InvalidArgument
                FIRFunctionsErrorCodeAlreadyExists -> FunctionsException.Code.AlreadyExists
                FIRFunctionsErrorCodeDeadlineExceeded -> FunctionsException.Code.DeadlineExceeded
                FIRFunctionsErrorCodeNotFound -> FunctionsException.Code.NotFound
                FIRFunctionsErrorCodePermissionDenied -> FunctionsException.Code.PermissionDenied
                FIRFunctionsErrorCodeResourceExhausted -> FunctionsException.Code.ResourceExhausted
                FIRFunctionsErrorCodeFailedPrecondition -> FunctionsException.Code.FailedPrecondition
                FIRFunctionsErrorCodeAborted -> FunctionsException.Code.Aborted
                FIRFunctionsErrorCodeOutOfRange -> FunctionsException.Code.OutOfRange
                FIRFunctionsErrorCodeUnimplemented -> FunctionsException.Code.Unimplemented
                FIRFunctionsErrorCodeInternal -> FunctionsException.Code.Internal
                FIRFunctionsErrorCodeUnavailable -> FunctionsException.Code.Unavailable
                FIRFunctionsErrorCodeDataLoss -> FunctionsException.Code.DataLoss
                FIRFunctionsErrorCodeUnauthenticated -> FunctionsException.Code.Unauthenticated
                else -> FunctionsException.Code.Unknown
            }
        throw FunctionsException(code, exception.description)
    }
}

internal actual val firebaseFunctionsInstance: FirebaseFunctions by lazy {
    FirebaseFunctionsImpl(FIRFunctions.functions())
}

@Suppress("TYPE_MISMATCH")
internal actual fun getFunctionsInstance(app: FirebaseApp): FirebaseFunctions =
    FirebaseFunctionsImpl(FIRFunctions.functionsForApp(app.app))