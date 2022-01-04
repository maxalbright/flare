package enchant.flare

import com.google.firebase.firestore.Blob
import kotlinx.datetime.Instant
import java.util.*

internal actual fun toBlob(array: ByteArray): Any = Blob.fromBytes(array)
internal actual fun fromBlob(blob: Any): ByteArray = (blob as Blob).toBytes()
internal actual fun isBlob(blob: Any?): Boolean = blob is Blob
internal actual fun toDate(instant: Instant): Any = Date(instant.toEpochMilliseconds())
internal actual fun fromDate(date: Any): Instant = Instant.fromEpochMilliseconds((date as Date).time)
internal actual fun isDate(date: Any?): Boolean = date is Date