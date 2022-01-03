package enchant.flare

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule


inline fun <reified T> Document.data(strategy: DeserializationStrategy<T> = serializer()): T {
    val decoder = FirebaseDecoder(this)
    return decoder.decodeSerializableValue(strategy)
}

suspend inline fun <reified T> FirebaseFirestore.setDocument(
    path: String, data: T, options: Merge = Merge.None,
    strategy: SerializationStrategy<T> = serializer(),
    noinline changes: (Changes.() -> Unit)? = null
) {
    val encoder = FirebaseEncoder()
    encoder.encodeSerializableValue(strategy, data)
    return setDocument(path, encoder.map!!, options, changes)
}

suspend inline fun <reified T> FirebaseFirestore.updateDocument(
    path: String, data: T, strategy: SerializationStrategy<T> = serializer(),
    noinline changes: (Changes.() -> Unit)? = null
) {
    val encoder = FirebaseEncoder()
    encoder.encodeSerializableValue(strategy, data)
    return updateDocument(path, encoder.map!!, changes)
}

enum class SerialType { Input, Output, InputOutput }

suspend inline fun <reified E : Any, reified T> FirebaseFunctions.call(
    name: String,
    data: E? = null,
    timeout: Long? = null,
    serial: SerialType,
    inputStrategy: SerializationStrategy<E> = serializer(),
    outputStrategy: DeserializationStrategy<T> = serializer()
): T {
    val newData: Any? = if (serial != SerialType.Output && data != null) {
        val encoder = FirebaseEncoder()
        encoder.encodeSerializableValue(inputStrategy, data)
        encoder.map
    } else data
    val output = call<T>(name, data, timeout)
    return if (serial != SerialType.Input) {
        val decoder = FirebaseDecoder(output as Map<String, Any>)
        decoder.decodeSerializableValue(outputStrategy)
    } else output
}

@OptIn(ExperimentalSerializationApi::class)
class FirebaseEncoder(
    var list: MutableList<Any>? = null,
    var map: MutableMap<String, Any>? = null,
    val kind: StructureKind = StructureKind.MAP,
    val descriptor: SerialDescriptor? = null
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule

    var index = 0

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (this.descriptor == null) {
            map = mutableMapOf()
            return FirebaseEncoder(null, map, descriptor.kind as StructureKind, descriptor)
        }
        val output: Any =
            if (descriptor.kind == StructureKind.LIST) mutableListOf<Any>() else mutableMapOf<String, Any>()
        if (kind == StructureKind.LIST) list!! += output
        else map!![this.descriptor.getElementName(index)] = output
        index++
        return FirebaseEncoder(
            (if (descriptor.kind == StructureKind.LIST) output else null) as MutableList<Any>?,
            (if (descriptor.kind != StructureKind.LIST) output else null) as MutableMap<String, Any>?,
            descriptor.kind as StructureKind, descriptor
        )
    }


    override fun encodeByte(value: Byte) = encodeValue(value.toLong())

    override fun encodeChar(value: Char) = encodeValue(value.toString())

    override fun encodeEnum(descriptor: SerialDescriptor, index: Int) = encodeValue(index.toLong())

    override fun encodeFloat(value: Float) = encodeValue(value.toDouble())

    override fun encodeInt(value: Int) = encodeValue(value.toLong())

    override fun encodeShort(value: Short) = encodeValue(value.toLong())

    override fun encodeValue(value: Any) {
        if (kind == StructureKind.LIST) list!! += value else {
            map!![descriptor!!.getElementName(index++)] = value
        }
    }

    override fun encodeNull() {
        index++
    }
}

@OptIn(ExperimentalSerializationApi::class)
class FirebaseDecoder(
    val map: Map<String, Any>? = null,
    val list: List<Any>? = null,
    val kind: StructureKind = StructureKind.MAP,
    val descriptor: SerialDescriptor? = null
) : AbstractDecoder() {

    var index = 0

    override val serializersModule: SerializersModule = EmptySerializersModule

    @ExperimentalSerializationApi
    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
        map?.size ?: list!!.size

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val data: Any =
            if (kind == StructureKind.LIST) list!![index]
            else if (this.descriptor == null) map!!
            else map!![this.descriptor.getElementName(index)]!!
        index++
        return FirebaseDecoder(
            (if (descriptor.kind != StructureKind.LIST) data else null) as Map<String, Any>?,
            (if (descriptor.kind == StructureKind.LIST) data else null) as List<Any>?,
            descriptor.kind as StructureKind,
            descriptor
        )
    }


    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = when {
        index >= map?.size ?: list!!.size -> DECODE_DONE
        map?.containsKey(descriptor.getElementName(index)) == false -> UNKNOWN_NAME
        else -> index
    }

    override fun decodeByte(): Byte =
        ((map?.get(descriptor!!.getElementName(index++)) ?: list!![index++]) as Long).toByte()

    override fun decodeChar(): Char {
        val s: String =
            (map?.get(descriptor!!.getElementName(index++)) ?: list!![index++]) as String
        if (s.length > 1) error("Decoded invalid char, instead was a string with multiple characters")
        return s[0]
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
        ((map?.get(descriptor!!.getElementName(index++)) ?: list!![index++]) as Long).toInt()

    override fun decodeFloat(): Float =
        ((map?.get(descriptor!!.getElementName(index++)) ?: list!![index++]) as Double).toFloat()

    override fun decodeInt(): Int =
        ((map?.get(descriptor!!.getElementName(index++)) ?: list!![index++]) as Long).toInt()

    override fun decodeValue(): Any =
        (map?.get(descriptor!!.getElementName(index++)) ?: list!![index++])

    override fun decodeShort(): Short =
        ((map?.get(descriptor!!.getElementName(index++)) ?: list!![index++]) as Long).toShort()


    override fun decodeNotNullMark(): Boolean =
        map!!.containsKey(descriptor!!.getElementName(index))

    override fun decodeNull(): Nothing? {
        index++
        return super.decodeNull()
    }
}