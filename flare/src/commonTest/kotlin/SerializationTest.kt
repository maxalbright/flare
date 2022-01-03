import enchant.flare.FirebaseDecoder
import enchant.flare.FirebaseEncoder
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTest {

    @Test
    fun encode() {
        val encoder = FirebaseEncoder()
        println(encoder)
        encoder.encodeSerializableValue(serializer(), ethan)
        println(encoder.map)
    }

    @Test
    fun decode() {
        val map = mapOf(
            "name" to "Ethan",
            "age" to 23L,
            "height" to mapOf("first" to 5L, "second" to 10L),
            "favoriteColor" to 0L,
            "friends" to listOf(
                mapOf(
                    "name" to "Vikram",
                    "age" to 19L,
                    "height" to mapOf("first" to 5L, "second" to 11L),
                    "favoriteColor" to 2L,
                ),
                mapOf(
                    "name" to "Jeff",
                    "age" to 34L,
                    "height" to mapOf("first" to 6L, "second" to 5L),
                    "favoriteColor" to 1L,
                )
            )

        )
        val decoder = FirebaseDecoder(map)
        val person: Person = decoder.decodeSerializableValue(serializer())
        assertEquals(ethan, person)
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