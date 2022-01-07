package enchant.flare

import kotlinx.cinterop.StableRef
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toNSDate
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.dataWithBytes
import platform.Foundation.getBytes
import kotlin.coroutines.resume

internal actual fun toBlob(array: ByteArray): Any {
    var nsData: NSData? = null
    memScoped {
        val p = allocArrayOf(array)
        nsData = NSData.dataWithBytes(p, array.size.toULong())
    }
    return nsData!!
}
internal actual fun fromBlob(blob: Any): ByteArray {
    memScoped {
        val nsData = (blob as NSData).bytes
        val p = StableRef.create(1)
        data.getBytes(p.asCPointer())
        c.resume(p.get().toTypedArray())
    }
}
internal actual fun isBlob(blob: Any?): Boolean = blob is NSData
internal actual fun toDate(instant: Instant): Any = instant.toNSDate()
internal actual fun fromDate(date: Any): Instant = (date as NSDate).toKotlinInstant()
internal actual fun isDate(date: Any?): Boolean = date is NSDate