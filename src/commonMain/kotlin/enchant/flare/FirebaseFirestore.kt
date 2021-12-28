package enchant.flare

import kotlinx.coroutines.flow.Flow

interface Document : Map<String, Any> {

    //TODO: Ensure data types are converted to the same Kotlin type (blob, boolean, date, double, geopoint, string, timestamp)
    val id: String
    val hasPendingWrites: Boolean
    val isFromCache: Boolean
}

interface Collection : Iterable<Document> {

    val id: String
    val documents: List<Document>
    val hasPendingWrites: Boolean
    val isFromCache: Boolean
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

    fun getNamedQuery(path: String, metadataChanges: Boolean = false, query: Query.() -> Unit)
            : Flow<Collection>

    suspend fun getNamedQueryOnce(
        path: String, source: Source = Source.Default, query: Query.() -> Unit
    ): Collection

    suspend fun batch(batch: WriteBatch.() -> Unit)
    fun transaction(transaction: Transaction.() -> Unit)

    val config: Config

    interface Config {
        var settings: FirebaseFirestoreSettings
        var enableLogging: Boolean
        fun useEmulator(host: String, port: Int)
        fun loadBundle(data: Array<Byte>)
        suspend fun snapshotsInSync(action: () -> Unit)
        suspend fun clearPersistence()
        suspend fun enableNetwork(enabled: Boolean)
        suspend fun terminate()
        suspend fun waitForPendingWrites()

    }
}

enum class Source { Cache, Default, Server }

sealed class Merge {
    object None : Merge()
    object All : Merge()
    class Fields(vararg fields: String) : Merge()
}

interface Query {

    enum class Direction { Ascending, Descending }

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
    fun set(path: String, data: Map<String, Any>, options: Merge = Merge.None)
    fun update(path: String, data: Map<String, Any>)
    fun delete(path: String)
}

interface Transaction {
    fun get(path: String): Document
    fun set(path: String, data: Map<String, Any>, options: Merge = Merge.None)
    fun update(path: String, data: Map<String, Any>)
    fun delete(path: String)
}

data class FirebaseFirestoreSettings(
    val cacheSize: Long = 104857600,
    val host: String = "",
    val persistenceEnabled: Boolean = true,
    val sslEnabled: Boolean = true,
    var loggingEnabled: Boolean
)

class FirestoreException(val code: Code): Exception("Firestore database operation failed with code: $code") {

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