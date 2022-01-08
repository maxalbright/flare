package enchant.flare

import cocoapods.FirebaseFirestore.FIRTimestamp
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toNSDate
import platform.Foundation.*
import platform.posix.free
import platform.posix.memcpy

internal actual fun toBlob(array: ByteArray): Any {
    var nsData: NSData = NSData.new()!!
    memScoped {
        val p = allocArrayOf(array)
        nsData = NSData.dataWithBytes(p, array.size.toULong())
    }
    return nsData
}

internal actual fun fromBlob(blob: Any): ByteArray {
    val nsData = (blob as NSData)
    val bytes: ByteArray = ByteArray(nsData.length.toInt())
    memScoped {
        val p = allocArrayOf(bytes)
        memcpy(p, nsData.bytes, nsData.length)
        bytes.indices.forEach { bytes[it] = p[it] }
    }
    return bytes
}

internal actual fun isBlob(blob: Any?): Boolean = blob is NSData
internal actual fun toDate(instant: Instant): Any =
    FIRTimestamp(instant.epochSeconds, instant.nanosecondsOfSecond)

internal actual fun fromDate(date: Any): Instant =
    (date as FIRTimestamp).dateValue().toKotlinInstant()

internal actual fun isDate(date: Any?): Boolean = date is NSDate