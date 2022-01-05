package enchant.flare

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import kotlinx.datetime.Instant
import java.util.*
import kotlin.math.pow
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

internal actual fun toBlob(array: ByteArray): Any = Blob.fromBytes(array)
internal actual fun fromBlob(blob: Any): ByteArray = (blob as Blob).toBytes()
internal actual fun isBlob(blob: Any?): Boolean = blob is Blob
internal actual fun toDate(instant: Instant): Any =
    Timestamp(instant.epochSeconds, instant.nanosecondsOfSecond)

internal actual fun fromDate(date: Any): Instant =
    Instant.fromEpochMilliseconds(
        ((date as Timestamp).seconds.seconds + (date.nanoseconds).nanoseconds).inWholeMilliseconds
    )

internal actual fun isDate(date: Any?): Boolean = date is Date