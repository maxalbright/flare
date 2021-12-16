package enchant.flare

import kotlinx.coroutines.flow.Flow

abstract class Document : Map<String, Any> {

    //TODO: Ensure data types are converted to the same Kotlin type (blob, boolean, date, double, geopoint, string, timestamp)
    abstract val id: String

    abstract val hasPendingWrites: Boolean
    abstract val isFromCache: Boolean

    override fun toString(): String {
        return super.toString() //TODO: Include nice toString output
    }
}

abstract class Collection : Iterable<Document> {

    abstract val id: String
    abstract val documents: List<Document>

    abstract val hasPendingWrites: Boolean
    abstract val isFromCache: Boolean

    override fun iterator(): Iterator<Document> = documents.iterator()

    override fun toString(): String {
        return super.toString() //TODO: Include nice toString output
    }
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