package enchant.flare

import kotlinx.coroutines.flow.Flow

interface Document : Map<String, Any?> {
    val id: String
    val metadata: Map<FirestoreMetadata, Any?>
}

interface Collection : List<Document> {
    val id: String
    val documents: List<Document>
    val metadata: Map<FirestoreMetadata, Any>
}

interface FirebaseFirestore {

    fun getDocument(path: String, metadataChanges: Boolean = false): Flow<Document>
    suspend fun getDocumentOnce(path: String, source: Source = Source.Default): Document
    suspend fun getDocumentOnceOrNull(path: String, source: Source = Source.Default): Document?

    suspend fun setDocument(
        path: String,
        map: Map<String, Any?>,
        merge: Merge = Merge.None,
        changes: (Changes.() -> Unit)? = null
    )

    suspend fun updateDocument(
        path: String,
        map: Map<String, Any?>,
        changes: (Changes.() -> Unit)? = null
    )

    suspend fun deleteDocument(path: String)

    fun getCollection(
        path: String, metadataChanges: Boolean = false, query: (Query.() -> Unit)? = null
    ): Flow<Collection>

    suspend fun getCollectionOnce(
        path: String, source: Source = Source.Default, query: (Query.() -> Unit)? = null
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

interface Query {

    fun limit(limit: Long, toLast: Boolean = false)

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

interface Changes {
    fun arrayRemove(field: String, vararg elements: Any)
    fun arrayUnion(field: String, vararg elements: Any)
    fun delete(field: String)
    fun increment(field: String, amount: Double)
    fun increment(field: String, amount: Long)
    fun serverTimestamp(field: String)
}

internal expect val firestoreInstance: FirebaseFirestore
internal expect fun getFirestoreInstance(app: FirebaseApp): FirebaseFirestore