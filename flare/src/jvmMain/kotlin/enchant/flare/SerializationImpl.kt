package enchant.flare

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Blob
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant

internal actual fun toBlob(array: ByteArray): Any = Blob.fromBytes(array)
internal actual fun fromBlob(blob: Any): ByteArray = (blob as Blob).toBytes()
internal actual fun isBlob(blob: Any?): Boolean = blob is Blob
internal actual fun toDate(instant: Instant): Any = Timestamp.ofTimeSecondsAndNanos(instant.epochSeconds, instant.nanosecondsOfSecond)

internal actual fun fromDate(date: Any): Instant {
    val timestamp: Timestamp = (date as Timestamp)
    return Instant.fromEpochSeconds(timestamp.seconds, timestamp.nanos)
}

internal actual fun isDate(date: Any?): Boolean = date is Instant