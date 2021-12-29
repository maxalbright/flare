package enchant.flare

import kotlinx.coroutines.flow.Flow

interface Document : Map<String, Any> {

    //TODO: Ensure data types are converted to the same Kotlin type (blob, boolean, date, double, geopoint, string, timestamp)
    val id: String
    val metadata: Map<FirestoreMetadata, Any>
}

interface Collection : List<Document> {

    val id: String
    val documents: List<Document>
    val metadata: Map<FirestoreMetadata, Any>
}

interface FirebaseFirestore {

    fun getDocument(path: String, metadataChanges: Boolean = false): Flow<Document>
    suspend fun getDocumentOnce(path: String, source: Source = Source.Default): Document
    suspend fun setDocument(path: String, data: Map<String, Any>, options: Merge = Merge.None)
    suspend fun updateDocument(path: String, data: Map<String, Any>)
    suspend fun deleteDocument(path: String)

    fun getCollection(
        path: String, metadataChanges: Boolean = false, query: Query.() -> Unit
    ): Flow<Collection>

    suspend fun getCollectionOnce(
        path: String, source: Source = Source.Default, query: Query.() -> Unit
    ): Collection

    fun getNamedQuery(name: String, metadataChanges: Boolean = false, query: Query.() -> Unit)
            : Flow<Collection>

    suspend fun getNamedQueryOnce(
        name: String, source: Source = Source.Default, query: Query.() -> Unit
    ): Collection

    suspend fun batch(batch: WriteBatch.() -> Unit)
    suspend fun transaction(transaction: Transaction.() -> Unit)

    val config: Config

    interface Config {
        var settings: FirebaseFirestoreSettings
        fun useEmulator(host: String, port: Int)
        suspend fun loadBundle(data: Array<Byte>)
        suspend fun snapshotsInSync(action: () -> Unit)
        suspend fun clearPersistence()
        suspend fun enableNetwork(enabled: Boolean)
        suspend fun terminate()
        suspend fun waitForPendingWrites()

    }

    companion object {
        fun getInstance(app: FirebaseApp? = null): FirebaseFirestore =
            if (app == null) firestoreInstance else getFirestoreInstance(app)
    }
}

enum class Source { Cache, Default, Server }

sealed class Merge {
    object None : Merge()
    object All : Merge()
    class Fields(vararg val fields: String) : Merge()
}

enum class Direction { Ascending, Descending }

interface Query {

    fun limit(limit: Long)
    fun limitToLast(limit: Long)

    fun orderBy(field: String, direction: Direction = Direction.Ascending)
    fun whereArrayContains(field: String, vararg value: Any)

    fun whereEqualTo(field: String, value: Any)
    fun whereNotEqualTo(field: String, value: Any)

    fun whereGreaterThan(field: String, value: Any)
    fun whereGreaterThanOrEqualTo(field: String, value: Any)

    fun whereIn(field: String, vararg value: Any)
    fun whereNotIn(field: String, vararg value: Any)

    fun whereLessThan(field: String, value: Any)
    fun whereLessThanOrEqualTo(field: String, value: Any)
}

sealed class Field {

    class ArrayRemove(elements: Any) : Field()
    class ArrayUnion(elements: Any) : Field()
    object Delete
    class Increment(amount: Long) : Field() {
        constructor(amount: Double) : this(amount.toLong())
    }

    object ServerTimestamp
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

class FirebaseFirestoreException(val code: Code) :
    Exception("Firebase firestore operation failed with code: $code") {

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

abstract class MapDocument(private val data: Map<String, Any>) : Document {
    override val entries: Set<Map.Entry<String, Any>> = data.entries
    override val keys: Set<String> = data.keys
    override val size: Int = data.size
    override val values: kotlin.collections.Collection<Any> = data.values
    override fun containsKey(key: String): Boolean = data.containsKey(key)
    override fun containsValue(value: Any): Boolean = data.containsValue(value)
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

internal expect val firestoreInstance: FirebaseFirestore
expect fun getFirestoreInstance(app: FirebaseApp): FirebaseFirestore