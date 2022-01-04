package enchant.flare

import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toNSDate
import platform.Foundation.NSDate

internal actual fun toBlob(array: ByteArray): Any = array
internal actual fun fromBlob(blob: Any): ByteArray = blob as ByteArray
internal actual fun isBlob(blob: Any?): Boolean = blob is ByteArray
internal actual fun toDate(instant: Instant): Any = instant.toNSDate()
internal actual fun fromDate(date: Any): Instant = (date as NSDate).toKotlinInstant()
internal actual fun isDate(date: Any?): Boolean = date is NSDate