import enchant.flare.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.test.*

class FirestoreTest : FlareTest() {

    val firestore by lazy {
        if (useLocal) LocalFirestore() else FirebaseFirestore.instance
    }

    @Test
    fun setGetOnce() = runTest {
        firestore.setDocument("test/$testId", ethan)
        val data: Dog = firestore.getDocumentOnce("test/$testId").data()

        assertEquals(ethan, data)
    }

    @Test
    fun setGetOnceAllTypes() = runTest {
        firestore.setDocument("test/$testId", myData)
        val data: MyData = firestore.getDocumentOnce("test/$testId").data()

        println(data)
        println(myData)
        assertEquals(myData.map, data.map)
        assertEquals(
            myData.toString().replace("map=\\{.*\\)\\},".toRegex(), ""),
            data.toString().replace("map=\\{.*\\)\\},".toRegex(), "")
        )
    }

    @Test
    fun getSetMultiple() = runBlocking {
        firestore.setDocument("test/megan", megan)
        var updates = 0
        val job = launch {
            firestore.getDocument("test/megan").collect {
                updates++
                val dog: Dog = it.data()
                assertEquals(dog.name, "Megan")
            }
        }
        firestore.setDocument("test/megan", megan)
        delay(5)
        assertEquals(1, updates)
        firestore.setDocument("test/megan", megan.copy(age = 3))
        delay(5)
        assertEquals(2, updates)
        job.cancel()
    }

    @Test
    fun update(): Unit = runBlocking {
        firestore.setDocument("test/megan", megan)
        firestore.updateDocument("test/megan", mapOf("age" to 5L))
        val age = firestore.getDocumentOnce("test/megan").data<Dog>().age
        assertEquals(5, age)
        delay(500)
        try {
            println("update1")
            firestore.updateDocument("test/invalid", map = mapOf<String, Any>())
            fail()
        } catch (t: FirestoreException) {
        }
    }

    @Test
    fun delete(): Unit = runBlocking {
        firestore.setDocument("test/megan", megan)
        firestore.deleteDocument("test/megan")
        delay(500)
        try {
            firestore.getDocumentOnce("test/megan")
            fail()
        } catch (t: FirestoreException) {
        }
    }

    @Test
    fun getCollectionOnce() = runTest {
        listOf(hailey, michael, jerry).forEach {
            firestore.setDocument("test/dogDoc/dogs/${it.name}", it)
        }
        val dogs: List<Dog> = firestore.getCollectionOnce("test/dogDoc/dogs").data()
        listOf(hailey, michael, jerry).forEach {
            assertContains(dogs, it)
        }
    }

    @Test
    fun getCollection() = runTest {
        var updates = 0
        val job = launch {
            firestore.getCollection("test/dogDocs/dogs").collect {
                updates++
                val dogs: List<Dog> = it.data()
                assertEquals(updates, dogs.size)
                assertTrue(dogs.all { it.name in setOf("Hailey", "Michael", "Jerry") })

            }
        }
        firestore.setDocument("test/dogDocs/dogs/Hailey", hailey.copy(age = 2))
        delay(100)
        firestore.setDocument("test/dogDocs/dogs/Michael", michael.copy(age = 8))
        delay(100)
        firestore.setDocument("test/dogDocs/dogs/Jerry", jerry.copy(age = 4))
        delay(100)
        assertEquals(3, updates)
        job.cancel()
    }

    @Test
    fun doesThrow(): Unit = runBlocking {
        try {
            firestore.deleteDocument("test/blankk")
        } catch (e: Exception) {
        }
    }

    @Serializable
    data class Dog(
        val name: String,
        val age: Int,
        val friends: List<Dog>? = null
    )

    val hailey = Dog("Hailey", 1)
    val michael = Dog("Michael", 9)
    val jerry = Dog("Jerry", 3)
    val megan = Dog("Megan", 2)
    val mark = Dog("Mark", Int.MAX_VALUE)

    val kate = Dog("Kate", 4, listOf(hailey, michael))
    val kelley = Dog("Kelley", 8)
    val ray = Dog("Ray", 4, listOf(jerry, megan, mark))

    val ethan = Dog("Ethan", 10, listOf(kate, kelley, ray))
}

