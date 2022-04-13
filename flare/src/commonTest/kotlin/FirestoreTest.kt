import enchant.flare.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTest : FlareTest() {

    val firestore by lazy {
        if (useLocal) LocalFirestore() else FirebaseFirestore.instance
    }

    @Test
    fun setGetOnce() = runTest {
        firestore.setDocument("test/$testId/setGetOnce/ethan", ethan)
        val data: Dog = firestore.getDocumentOnce("test/$testId/setGetOnce/ethan").data()
    }

    @Test
    fun getOnceNull() = runTest {
        val data: Dog? = firestore.getDocumentOnceOrNull("test/data")?.data()
        assertNull(data)
    }


    @Test
    fun setGetOnceAllTypes() = runTest {
        firestore.setDocument("test/$testId/getSetAll/myData", myData)
        val data: MyData = firestore.getDocumentOnce("test/$testId/getSetAll/myData").data()
        assertEquals(myData.map, data.map)
        assertEquals(
            myData.toString().replace("map=\\{.*\\)\\},".toRegex(), ""),
            data.toString().replace("map=\\{.*\\)\\},".toRegex(), "")
        )
    }

    @Test
    fun getSetMultiple() = runTest {
        firestore.setDocument("test/$testId/getSetMultiple/megan", megan)
        var updates = 0
        val job = launch {
            firestore.getDocument("test/$testId/getSetMultiple/megan").collect {
                updates++
                val dog: Dog = it.data()
                assertEquals(dog.name, "Megan")
            }
        }
        firestore.setDocument("test/$testId/getSetMultiple/megan", megan)
        yield()
        assertEquals(1, updates)
        firestore.setDocument("test/$testId/getSetMultiple/megan", megan.copy(age = 3))
        yield()
        assertEquals(2, updates)
        job.cancel()
    }

    @Test
    fun update(): Unit = runTest {
        firestore.setDocument("test/$testId/update/megan", megan)
        firestore.updateDocument("test/$testId/update/megan", mapOf("age" to 5L))
        val age = firestore.getDocumentOnce("test/$testId/update/megan").data<Dog>().age
        assertEquals(5, age)
        delay(500)
        try {
            firestore.updateDocument("test/$testId/update/invalid", map = mapOf<String, Any>())
            fail()
        } catch (t: FirestoreException) {
        }
    }

    @Test
    fun delete(): Unit = runTest {
        firestore.setDocument("test/$testId/delete/megan", megan)
        firestore.deleteDocument("test/$testId/delete/megan")
        delay(500)
        try {
            firestore.getDocumentOnce("test/$testId/delete/megan")
            fail()
        } catch (t: FirestoreException) {
        }
    }

    @Test
    fun getCollectionOnce() = runTest {
        listOf(hailey, michael, jerry).forEach {
            firestore.setDocument("test/$testId/dogs/${it.name}", it)
        }
        val dogs: List<Dog> = firestore.getCollectionOnce("test/$testId/dogs").data()
        listOf(hailey, michael, jerry).forEach {
            assertContains(dogs, it)
        }
    }

    @Test
    fun getCollection() = runTest {
        var updates = 0
        val job = launch {
            firestore.getCollection("test/${testId}/dogs").collect {
                updates++
                val dogs: List<Dog> = it.data()
                assertEquals(updates, dogs.size)
                assertTrue(dogs.all { it.name in setOf("Hailey", "Michael", "Jerry") })

            }
        }
        yield()
        firestore.setDocument("test/$testId/dogs/Hailey", hailey.copy(age = Random.nextInt()))
        yield()
        firestore.setDocument("test/$testId/dogs/Michael", michael.copy(age = Random.nextInt()))
        yield()
        firestore.setDocument("test/$testId/dogs/Jerry", jerry.copy(age = Random.nextInt()))
        yield()
        assertEquals(3, updates)
        job.cancel()
    }

    @Test
    fun doesThrow(): Unit = runTest {
        try {
            firestore.deleteDocument("test/${testId}/blankk/hi")
        } catch (e: Exception) {
        }
    }

    @Test
    fun changes() = runTest {
        val firestore = LocalFirestore()
        firestore.setDocument(
            "collection/document", mapOf(
                "double" to 1.0,
                "long" to 2L,
                "arrayUnion" to listOf(1, 2),
                "arrayRemove" to listOf(3, 4)
            )
        )
        firestore.updateDocument("collection/document", mapOf()) {
            increment("double", 2.5)
            increment("long", 3L)
            arrayUnion("arrayUnion", 3)
            arrayRemove("arrayRemove", 4)
        }
        val document = firestore.getDocumentOnce("collection/document")

        assertEquals(3.5, document["double"])
        assertEquals(5L, document["long"])
        assertEquals(listOf(1,2,3), document["arrayUnion"] as List<Int>)
        assertContentEquals(listOf(3), document["arrayRemove"] as List<Int>)
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

