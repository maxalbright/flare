package enchant.flare

import kotlinx.coroutines.flow.Flow


/**
* The Document interface represents a 
* map that holds all of the document's 
* metadata. 
*/

interface Document : Map<String, Any?> {
    /**
    * The id corresponds to the name of
    * the document / how it will be accessed
    */
    val id: String

    val metadata: Map<FirestoreMetadata, Any?>
}

/**
* This interface corresponds to a list of documents.
*/

interface Collection : List<Document> {
    /**
    * The id is the name of the Collection in the database
    * containing the list of documents.
    */
    val id: String
    val documents: List<Document>
    val metadata: Map<FirestoreMetadata, Any>
}

/**
* Firebase Firestore is a NoSQL cloud database to store
* and sync data for client- and server-side development.
*/

interface FirebaseFirestore {

    /**
    * Reads the data and starts listening to the document
    * referenced by this path.
    */

    fun getDocument(path: String, metadataChanges: Boolean = false): Flow<Document>
    
    /**
    * Reads the data of the document at the given path and source
    */
    suspend fun getDocumentOnce(path: String, source: Source = Source.Default): Document
    suspend fun getDocumentOnceOrNull(path: String, source: Source = Source.Default): Document?

    /**
    * Overwrites the document at this path with the given changes,
    * See "Changes" interface below.
    */
    suspend fun setDocument(
        path: String,
        map: Map<String, Any?>,
        merge: Merge = Merge.None,
        changes: (Changes.() -> Unit)? = null
    )

    /**
    * Updates fields in the document referred to by this object
    * with the given changes. See "Changes" interface below.
    */
    suspend fun updateDocument(
        path: String,
        map: Map<String, Any?>,
        changes: (Changes.() -> Unit)? = null
    )

    suspend fun deleteDocument(path: String)


    /**
    * Returns the a collection of document snapshots yielded by the given query at the given path.
    * The syntax of this might look like the following:
    * firestore.getCollectionOnce("cities") {
    *   where EqualTo("state", "CA")
    * }
    */
    suspend fun getCollectionOnce(
        path: String, source: Source = Source.Default, query: (Query.() -> Unit)? = null
    ): Collection


    /**
    * Similar to getCollectionOnce() except it returns a listener to 
    * the colleciton of documents yielded by the given query at the given path.
    */
    fun getCollection(
        path: String, metadataChanges: Boolean = false, query: (Query.() -> Unit)? = null
    ): Flow<Collection>


    suspend fun getNamedQueryOnce(
        name: String, source: Source = Source.Default, query: Query.() -> Unit
    ): Collection


    fun getNamedQuery(name: String, metadataChanges: Boolean = false, query: Query.() -> Unit)
            : Flow<Collection>



    suspend fun batch(batch: WriteBatch.() -> Unit)
    suspend fun transaction(transaction: Transaction.() -> Unit)

    val config: Config

    interface Config {
        var settings: FirebaseFirestoreSettings
        fun useEmulator(host: String, port: Int)
        suspend fun loadBundle(data: ByteArray)
        suspend fun snapshotsInSync(action: () -> Unit)
        suspend fun clearPersistence()
        suspend fun enableNetwork(enabled: Boolean)
        suspend fun terminate()
        suspend fun waitForPendingWrites()

    }

    companion object {
        val instance: FirebaseFirestore get() = firestoreInstance
        fun getInstance(app: FirebaseApp) = getFirestoreInstance(app)
    }
}

enum class Source { Cache, Default, Server }

sealed class Merge {
    object None : Merge()
    object All : Merge()
    class Fields(vararg val fields: String) : Merge()
}

enum class Direction { Ascending, Descending }


/**
* This interface represents a query that you can read or listen to.
* You can also construct refined Query objects by adding filters and ordering. 
*/
interface Query {
    /**
    * Creates and returns a new Query that only returns the
    * first matching documents up to the specified number.
    */
    fun limit(limit: Long, toLast: Boolean = false)

    /**
    * Creates and returns a new Query that's additionally sorted by the
    * specified field, optionally in descending order instead of ascending.
    */
    fun orderBy(field: String, direction: Direction = Direction.Ascending)

    /** 
    * Creates and returns a new Query with the additional filter that
    * documents must contain the specified field, the value must
    * be an array, and that the array must contain the provided value
    */
    fun whereArrayContains(field: String, vararg value: Any)

    /** 
    * Creates and returns a new Query with the additional filter
    * that documents must contain the specified field and the
    * value should be equal to the specified value. 
    */
    fun whereEqualTo(field: String, value: Any)

    /** 
    * Creates and returns a new Query with the additional filter
    * that documents must contain the specified field and the value
    * does not equal the specified value. 
    */
    fun whereNotEqualTo(field: String, value: Any)

    /** 
    * Creates and returns a new Query with the additional filter
    * that documents must contain the specified field and the value
    * should be greater than the specified value.
    */
    fun whereGreaterThan(field: String, value: Any)

    /** 
    * Creates and returns a new Query with the additional filter
    * that documents must contain the specified field and the value
    * should be greater than or equal to the specified value.
    */
    fun whereGreaterThanOrEqualTo(field: String, value: Any)

    /** 
    * Creates and returns a new Query with the additional filter
    * that documents must contain the specified field and the
    * value must equal one of the values from the provided list.
    */
    fun whereIn(field: String, vararg value: Any)

    /** 
    * Creates and returns a new Query with the additional filter
    * that documents must contain the specified field and the value
    * does not equal any of the values from the provided list.
    */
    fun whereNotIn(field: String, vararg value: Any)
  
    /** 
    * Creates and returns a new Query with the additional filter
    * that documents must contain the specified field and the value
    * should be less than the specified value.
    */
    fun whereLessThan(field: String, value: Any)

    /** 
    * Creates and returns a new Query with the additional filter
    * that documents must contain the specified field and the value
    * should be less than or equal to the specified value.
    */
    fun whereLessThanOrEqualTo(field: String, value: Any)
}

interface WriteBatch {
    fun set(path: String, data: Map<String, Any>, merge: Merge = Merge.None)
    fun update(path: String, data: Map<String, Any>)
    fun delete(path: String)
}

interface Transaction {
    fun get(path: String): Document
    fun set(path: String, data: Map<String, Any>, merge: Merge = Merge.None)
    fun update(path: String, data: Map<String, Any>)
    fun delete(path: String)
}

data class FirebaseFirestoreSettings(
    val cacheSize: Long = 104857600,
    val host: String = "",
    val persistenceEnabled: Boolean = true,
    val sslEnabled: Boolean = true,
    var loggingEnabled: Boolean = false
)

/**
* This class represents the type of error 
* that Flare's FirebaseFirestore throws.
*
* @param code the code that is included in the error
* @param description a string that provides a description of the error
* in addition to code
*/
class FirestoreException(val code: Code, val description: String? = null) :
    Exception("Firebase firestore operation failed with code ${code.name}: $description") {

    enum class Code {
        Aborted,
        AlreadyExists,
        DataLoss,
        DeadlineExceeded,
        FailedPrecondition,
        Internal,
        InvalidArgument,
        NotFound,
        OutOfRange,
        PermissionDenied,
        ResourceExhausted,
        Unauthenticated,
        Unavailable,
        Unimplemented,
        Unknown
    }

    companion object {

        internal fun throwCode(code: Code, description: String? = null) {
            throw FirestoreException(code, description)
        }
    }
}

enum class FirestoreMetadata {

    //Android, iOS, JS
    PendingWrites,
    FromCache,

    //Jvm
    CreateTime,
    ReadTime,
    UpdateTime
}

abstract class MapDocument(private val data: Map<String, Any?>) : Document {
    override val entries: Set<Map.Entry<String, Any?>> = data.entries
    override val keys: Set<String> = data.keys
    override val size: Int = data.size
    override val values: kotlin.collections.Collection<Any?> = data.values
    override fun containsKey(key: String): Boolean = data.containsKey(key)
    override fun containsValue(value: Any?): Boolean = data.containsValue(value)
    override fun get(key: String): Any? = data[key]
    override fun isEmpty(): Boolean = data.isEmpty()
}

abstract class ListCollection(override val documents: List<Document>) : Collection {
    override val size: Int get() = documents.size

    override fun contains(element: Document): Boolean = documents.contains(element)

    override fun containsAll(elements: kotlin.collections.Collection<Document>): Boolean =
        elements.containsAll(elements)

    override fun get(index: Int): Document = documents[index]

    override fun indexOf(element: Document): Int = documents.indexOf(element)

    override fun isEmpty(): Boolean = documents.isEmpty()

    override fun iterator(): Iterator<Document> = documents.iterator()

    override fun lastIndexOf(element: Document): Int = documents.lastIndexOf(element)

    override fun listIterator(): ListIterator<Document> = documents.listIterator()
    override fun listIterator(index: Int): ListIterator<Document> = documents.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<Document> =
        documents.subList(fromIndex, toIndex)
}


/**
* 
*
*
*/
interface Changes {
    /**
    * Returns a special value that can be used with set() or update() that
    * tells the server to remove the given elements from any array value
    * that already exists on the server.
    */
    fun arrayRemove(field: String, vararg elements: Any)

    /**
    * Returns a special value that can be used with set() or update() that
    * tells the server to union the given elements with any array value that
    * already exists on the server.
    */
    fun arrayUnion(field: String, vararg elements: Any)

    /**
    * Returns a sentinel for use with update() to mark a field for deletion.
    */
    fun delete(field: String)

    /**
    * Returns a special value that can be used with set() or update() that
    * tells the server to increment the field's current value by the given value.
    */
    fun increment(field: String, amount: Double)
    fun increment(field: String, amount: Long)

    /**
    * Returns a sentinel for use with set() or update() to include a server-generated
    * timestamp in the written data.
    */
    fun serverTimestamp(field: String)
}

internal expect val firestoreInstance: FirebaseFirestore
internal expect fun getFirestoreInstance(app: FirebaseApp): FirebaseFirestore