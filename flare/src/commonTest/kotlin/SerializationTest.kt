import enchant.flare.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SerializationTest {

    @Test
    fun encode() {
        val encoder = FirebaseEncoder()
        encoder.encodeSerializableValue(serializer(), ethan)
        assertEquals(ethanMap, encoder.map!!)
    }

    @Test
    fun decode() {
        val decoder = FirebaseDecoder(ethanMap)
        val person: Person = decoder.decodeSerializableValue(serializer())
        assertEquals(ethan, person)
    }

    @Test
    fun fullEncode() {
        val encoder = FirebaseEncoder()
        encoder.encodeSerializableValue(serializer(), myData)
        val data = encoder.map!!
        if(myDataMap["blob"] is ByteArray) {
            assertTrue((myDataMap["blob"] as ByteArray).contentEquals(data["blob"] as ByteArray))
            data["blob"] = myDataMap["blob"]!!
        }
        assertEquals(myDataMap.toString(), encoder.map!!.toString())
    }

    @Test
    fun fullDecode() {
        val decoder = FirebaseDecoder(myDataMap)
        val data: MyData = decoder.decodeSerializableValue(serializer())
        assertEquals(myData.toString(), data.toString())
    }
}

@Serializable
data class Person(
    val name: String,
    val age: Byte,
    val height: Pair<Int, Int>,
    val favoriteColor: Color,
    val friends: List<Person>? = null
)

enum class Color { Red, Green, Blue }

val vikram = Person(
    name = "Vikram",
    age = 19,
    height = 5 to 11,
    favoriteColor = Color.Blue,
)
val jeff = Person(
    name = "Jeff",
    age = 34,
    height = 6 to 5,
    favoriteColor = Color.Green,
)

val ethan = Person(
    name = "Ethan",
    age = 23,
    height = 5 to 10,
    favoriteColor = Color.Red,
    friends = listOf(vikram, jeff)
)
val vikramMap: Map<String, Any?> = mapOf(
    "name" to "Vikram",
    "age" to 19L,
    "height" to mapOf("first" to 5L, "second" to 11L),
    "favoriteColor" to 2L,
)
val jeffMap: Map<String, Any?> = mapOf(
    "name" to "Jeff",
    "age" to 34L,
    "height" to mapOf("first" to 6L, "second" to 5L),
    "favoriteColor" to 1L,
)
val ethanMap: Map<String, Any?> = mapOf(
    "name" to "Ethan",
    "age" to 23L,
    "height" to mapOf("first" to 5L, "second" to 10L),
    "favoriteColor" to 0L,
    "friends" to listOf(
        vikramMap,
        jeffMap
    )
)
val blob = byteArrayOf(1,127,123,34,6,4,12)
val long: Long = 13L
val myData = MyData(
    array = arrayOf(1, 5, 2),
    blob = blob,
    list = setOf(ethan, vikram, jeff),
    boolean = true,
    date = Instant.fromEpochMilliseconds(1504645379673),
    byte = long.toByte(),
    short = long.toShort(),
    int = long.toInt(),
    long = long,
    float = 4f,
    double = 4.0,
    map = mapOf("Vikram" to vikram, "Jeff" to jeff),
    nullable = null,
    string = "Hello"
)

@Serializable
data class MyData(

    val array: Array<Int> = arrayOf(),
    val blob: ByteArray = ByteArray(0),
    val list: Set<Person> = emptySet(),
    val boolean: Boolean = false,
    val date: Instant = Clock.System.now(),
    val byte: Byte = 0,
    val short: Short = 0,
    val int: Int = 0,
    val long: Long = 0,
    val float: Float = 0f,
    val double: Double = 0.0,
    val map: Map<String, Person>? = null, //Not written if null
    val nullable: String? = "", //Not written if null
    val string: String = "",
)

val myDataMap = mapOf(
    "array" to listOf(1L, 5L, 2L),
    "blob" to toBlob(blob),
    "list" to listOf(ethanMap, vikramMap, jeffMap),
    "boolean" to true,
    "date" to toDate(Instant.fromEpochMilliseconds(1504645379673)),
    "byte" to 13L,
    "short" to 13L,
    "int" to 13L,
    "long" to 13L,
    "float" to 4.0,
    "double" to 4.0,
    "map" to mapOf("Vikram" to vikramMap, "Jeff" to jeffMap),
    "string" to "Hello"
)