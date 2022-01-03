package enchant.flare

import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.firebase.firestore.FirebaseFirestore as AndroidFirestore
import com.google.firebase.firestore.FirebaseFirestoreException.Code as AndroidCode
import com.google.firebase.firestore.FirebaseFirestoreException as AndroidFirestoreException
import com.google.firebase.firestore.Query as AndroidQuery
import com.google.firebase.firestore.Query.Direction as AndroidDirection
import com.google.firebase.firestore.Source as AndroidSource
import com.google.firebase.firestore.WriteBatch as AndroidWriteBatch
import com.google.firebase.firestore.Transaction as AndroidTransaction
import com.google.firebase.firestore.FirebaseFirestoreSettings as AndroidFirestoreSettings

private class DocumentImpl(val document: DocumentSnapshot) :
    MapDocument(document.data ?: emptyMap()) {
    override val id: String = document.id
    override val metadata: Map<FirestoreMetadata, Any> = mapOf(
        FirestoreMetadata.PendingWrites to document.metadata.hasPendingWrites(),
        FirestoreMetadata.FromCache to document.metadata.isFromCache
    )
}

private class CollectionImpl(val collection: QuerySnapshot, override val id: String) :
    ListCollection(collection.documents.map { DocumentImpl(it) }) {
    override val metadata: Map<FirestoreMetadata, Any> = mapOf(
        FirestoreMetadata.PendingWrites to collection.metadata.hasPendingWrites(),
        FirestoreMetadata.FromCache to collection.metadata.isFromCache
    )
}

private class FirebaseFirestoreImpl(private val firestore: AndroidFirestore) :
    FirebaseFirestore {
    override fun getDocument(path: String, metadataChanges: Boolean): Flow<Document> =
        callbackFlow {
            val registration = firestore.document(path)
                .addSnapshotListener(if (metadataChanges) MetadataChanges.INCLUDE else MetadataChanges.EXCLUDE) { data, error ->
                    when {
                        data != null -> trySendBlocking(DocumentImpl(data))
                        error!!.code == AndroidCode.CANCELLED -> return@addSnapshotListener
                        else -> throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
                    }
                }
            awaitClose { registration.remove() }
        }

    override suspend fun getDocumentOnce(path: String, source: Source): Document =
        suspendCancellableCoroutine { c ->
            firestore.document(path).get(toAndroidSource(source)).addOnCompleteListener {
                if (it.isSuccessful) c.resume(DocumentImpl(it.result))
                else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
            }
        }


    override suspend fun setDocument(
        path: String,
        data: Map<String, Any>,
        merge: Merge,
        changes: (Changes.() -> Unit)?
    ): Unit = suspendCancellableCoroutine { c ->
        val d = if(changes == null) data else ChangesImpl(data).apply(changes).newData
        val task = if (merge == Merge.None) firestore.document(path).set(d)
        else firestore.document(path).set(data, toSetOptions(merge)!!)
        task.addOnCompleteListener {
            if (it.isSuccessful) c.resume(Unit)
            else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
        }
    }

    override suspend fun updateDocument(path: String, data: Map<String, Any>, changes: (Changes.() -> Unit)?): Unit =
        suspendCancellableCoroutine { c ->
            val d = if(changes == null) data else ChangesImpl(data).apply(changes).newData
            firestore.document(path).update(d).addOnCompleteListener {
                if (it.isSuccessful) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
            }
        }

    override suspend fun deleteDocument(path: String): Unit =
        suspendCancellableCoroutine { c ->
            firestore.document(path).delete().addOnCompleteListener {
                if (it.isSuccessful) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
            }
        }

    override fun getCollection(
        path: String,
        metadataChanges: Boolean,
        query: Query.() -> Unit
    ): Flow<Collection> = callbackFlow {
        val collection = firestore.collection(path)
        val q = QueryImpl(collection).also { query(it) }.query
        val registration = q.addSnapshotListener(
            if (metadataChanges) MetadataChanges.INCLUDE else MetadataChanges.EXCLUDE
        ) { data, error ->
            when {
                data != null ->
                    trySendBlocking(CollectionImpl(data, path.takeLastWhile { it != '/' }))
                error!!.code == AndroidCode.CANCELLED -> return@addSnapshotListener
                else -> throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getCollectionOnce(
        path: String,
        source: Source,
        query: Query.() -> Unit
    ): Collection = suspendCancellableCoroutine { c ->
        val collection = firestore.collection(path)
        val q = QueryImpl(collection).also { query(it) }.query
        q.get(toAndroidSource(source)).addOnCompleteListener {
            if (it.isSuccessful)
                c.resume(CollectionImpl(it.result, path.takeLastWhile { it != '/' }))
            else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
        }
    }

    override fun getNamedQuery(
        name: String,
        metadataChanges: Boolean,
        query: Query.() -> Unit
    ): Flow<Collection> = callbackFlow {

        var registration: ListenerRegistration? = null
        firestore.getNamedQuery(name).addOnCompleteListener {
            if (!it.isSuccessful) throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
            val q = QueryImpl(it.result).also { query(it) }.query

            registration = q.addSnapshotListener(
                if (metadataChanges) MetadataChanges.INCLUDE else MetadataChanges.EXCLUDE
            ) { data, error ->
                when {
                    data != null ->
                        trySendBlocking(CollectionImpl(data, data.documents[0].reference.parent.id))
                    error!!.code == AndroidCode.CANCELLED -> return@addSnapshotListener
                    else -> throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
                }
            }
        }
        awaitClose { registration?.remove() }
    }

    override suspend fun getNamedQueryOnce(
        name: String,
        source: Source,
        query: Query.() -> Unit
    ): Collection = suspendCancellableCoroutine { c ->
        firestore.getNamedQuery(name).addOnCompleteListener {
            if (!it.isSuccessful) throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
            val q = QueryImpl(it.result).also { query(it) }.query
            q.get(toAndroidSource(source)).addOnCompleteListener {
                if (it.isSuccessful)
                    c.resume(CollectionImpl(it.result, it.result.documents[0].reference.parent.id))
                else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
            }
        }
    }

    override suspend fun batch(batch: WriteBatch.() -> Unit): Unit =
        suspendCancellableCoroutine { c ->
            firestore.runBatch { batch(WriteBatchImpl(firestore, it)) }.addOnCompleteListener {
                if (!it.isSuccessful) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
            }
        }

    override suspend fun transaction(transaction: Transaction.() -> Unit): Unit =
        suspendCancellableCoroutine { c ->
            firestore.runTransaction { transaction(TransactionImpl(firestore, it)) }
                .addOnCompleteListener {
                    if (!it.isSuccessful) c.resume(Unit)
                    else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
                }
        }

    override val config: FirebaseFirestore.Config = object : FirebaseFirestore.Config {
        override var settings: FirebaseFirestoreSettings
            get() = toFirestoreSettings(firestore.firestoreSettings)
            set(value) {
                firestore.firestoreSettings = toAndroidFirestoreSettings(value)
            }

        override fun useEmulator(host: String, port: Int): Unit = firestore.useEmulator(host, port)

        override suspend fun loadBundle(data: Array<Byte>): Unit =
            suspendCancellableCoroutine { c ->
                firestore.loadBundle(data.toByteArray()).addOnCompleteListener {
                    if (it.isSuccessful) c.resume(Unit)
                    else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
                }
            }

        override suspend fun snapshotsInSync(action: () -> Unit): Unit =
            suspendCancellableCoroutine { c ->
                val registration = firestore.addSnapshotsInSyncListener(action)
                c.invokeOnCancellation { registration.remove() }
            }

        override suspend fun clearPersistence(): Unit = suspendCancellableCoroutine { c ->
            firestore.clearPersistence().addOnCompleteListener {
                if (it.isSuccessful) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
            }
        }

        override suspend fun enableNetwork(enabled: Boolean): Unit =
            suspendCancellableCoroutine { c ->
                (if (enabled) firestore.enableNetwork() else firestore.disableNetwork()).addOnCompleteListener {
                    if (it.isSuccessful) c.resume(Unit)
                    else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
                }
            }

        override suspend fun terminate(): Unit = suspendCancellableCoroutine { c ->
            firestore.terminate().addOnCompleteListener {
                if (it.isSuccessful) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
            }
        }

        override suspend fun waitForPendingWrites(): Unit = suspendCancellableCoroutine { c ->
            firestore.waitForPendingWrites().addOnCompleteListener {
                if (it.isSuccessful) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode((it.exception as AndroidFirestoreException).code))
            }
        }

    }

    private fun toFirestoreExceptionCode(code: AndroidCode): FirebaseFirestoreException.Code =
        when (code) {
            AndroidCode.INVALID_ARGUMENT -> FirebaseFirestoreException.Code.InvalidArgument
            AndroidCode.ALREADY_EXISTS -> FirebaseFirestoreException.Code.AlreadyExists
            AndroidCode.DEADLINE_EXCEEDED -> FirebaseFirestoreException.Code.DeadlineExceeded
            AndroidCode.NOT_FOUND -> FirebaseFirestoreException.Code.NotFound
            AndroidCode.PERMISSION_DENIED -> FirebaseFirestoreException.Code.PermissionDenied
            AndroidCode.RESOURCE_EXHAUSTED -> FirebaseFirestoreException.Code.ResourceExhausted
            AndroidCode.FAILED_PRECONDITION -> FirebaseFirestoreException.Code.FailedPrecondition
            AndroidCode.ABORTED -> FirebaseFirestoreException.Code.Aborted
            AndroidCode.OUT_OF_RANGE -> FirebaseFirestoreException.Code.OutOfRange
            AndroidCode.UNIMPLEMENTED -> FirebaseFirestoreException.Code.Unimplemented
            AndroidCode.INTERNAL -> FirebaseFirestoreException.Code.Internal
            AndroidCode.UNAVAILABLE -> FirebaseFirestoreException.Code.Unavailable
            AndroidCode.DATA_LOSS -> FirebaseFirestoreException.Code.DataLoss
            AndroidCode.UNAUTHENTICATED -> FirebaseFirestoreException.Code.Unauthenticated
            else -> FirebaseFirestoreException.Code.Unknown.also { println("Encountered unknown firestore error code: ${code.name}") }
        }

    private fun toAndroidSource(source: Source): AndroidSource = when (source) {
        Source.Default -> AndroidSource.DEFAULT
        Source.Cache -> AndroidSource.CACHE
        Source.Server -> AndroidSource.SERVER
    }

    private fun toFirestoreSettings(settings: AndroidFirestoreSettings): FirebaseFirestoreSettings =
        FirebaseFirestoreSettings(
            cacheSize = settings.cacheSizeBytes,
            host = settings.host,
            persistenceEnabled = settings.isPersistenceEnabled,
            sslEnabled = settings.isSslEnabled
        )

    private fun toAndroidFirestoreSettings(settings: FirebaseFirestoreSettings): AndroidFirestoreSettings =
        AndroidFirestoreSettings.Builder().apply {
            cacheSizeBytes = settings.cacheSize
            host = settings.host
            isPersistenceEnabled = settings.persistenceEnabled
            isSslEnabled = settings.sslEnabled
        }.build()
}

private class QueryImpl(var query: AndroidQuery) : Query {

    private fun toQueryDirection(direction: Direction): AndroidDirection = when (direction) {
        Direction.Ascending -> AndroidDirection.ASCENDING
        Direction.Descending -> AndroidDirection.DESCENDING
    }

    override fun limit(limit: Long, toLast: Boolean) {
        query = if(toLast) query.limitToLast(limit) else query.limit(limit)
    }

    override fun orderBy(field: String, direction: Direction) {
        query = query.orderBy(field, toQueryDirection(direction))
    }

    override fun whereArrayContains(field: String, vararg value: Any) {
        query = query.whereArrayContains(field, value)
    }

    override fun whereEqualTo(field: String, value: Any) {
        query = query.whereEqualTo(field, value)
    }

    override fun whereNotEqualTo(field: String, value: Any) {
        query = query.whereNotEqualTo(field, value)
    }

    override fun whereGreaterThan(field: String, value: Any) {
        query = query.whereGreaterThan(field, value)
    }

    override fun whereGreaterThanOrEqualTo(field: String, value: Any) {
        query = query.whereGreaterThanOrEqualTo(field, value)
    }

    override fun whereIn(field: String, vararg values: Any) {
        query = query.whereIn(field, values.toMutableList())
    }

    override fun whereNotIn(field: String, vararg values: Any) {
        query = query.whereNotIn(field, values.toMutableList())
    }

    override fun whereLessThan(field: String, value: Any) {
        query = query.whereLessThan(field, value)
    }

    override fun whereLessThanOrEqualTo(field: String, value: Any) {
        query = query.whereLessThanOrEqualTo(field, value)
    }

}

private class WriteBatchImpl(var firestore: AndroidFirestore, var batch: AndroidWriteBatch) :
    WriteBatch {
    override fun set(path: String, data: Map<String, Any>, merge: Merge) {
        batch = if (merge == Merge.None) batch.set(firestore.document(path), data)
        else batch.set(firestore.document(path), data, toSetOptions(merge)!!)
    }

    override fun update(path: String, data: Map<String, Any>) {
        batch = batch.update(firestore.document(path), data)
    }

    override fun delete(path: String) {
        batch = batch.delete(firestore.document(path))
    }
}

private class TransactionImpl(
    var firestore: AndroidFirestore,
    var transaction: AndroidTransaction
) : Transaction {
    override fun get(path: String): Document =
        DocumentImpl(transaction.get(firestore.document("path")))

    override fun set(path: String, data: Map<String, Any>, merge: Merge) {
        transaction = if (merge == Merge.None) transaction.set(firestore.document(path), data)
        else transaction.set(firestore.document(path), data, toSetOptions(merge)!!)
    }

    override fun update(path: String, data: Map<String, Any>) {
        transaction = transaction.update(firestore.document(path), data)
    }

    override fun delete(path: String) {
        transaction = transaction.delete(firestore.document(path))
    }
}

private class ChangesImpl(data: Map<String, Any>): Changes {
    val newData: MutableMap<String, Any> = data.toMutableMap()

    override fun arrayRemove(field: String, vararg elements: Any) {
        newData[field] = FieldValue.arrayRemove(elements)
    }

    override fun arrayUnion(field: String, vararg elements: Any) {
        newData[field] = FieldValue.arrayUnion(elements)
    }

    override fun delete(field: String) {
        newData[field] = FieldValue.delete()
    }

    override fun increment(field: String, amount: Double) {
        newData[field] = FieldValue.increment(amount)
    }

    override fun increment(field: String, amount: Long) {
        newData[field] = FieldValue.increment(amount)
    }

    override fun serverTimestamp(field: String) {
        newData[field] = FieldValue.serverTimestamp()
    }

}

private fun toSetOptions(merge: Merge): SetOptions? = when (merge) {
    Merge.None -> null
    Merge.All -> SetOptions.merge()
    is Merge.Fields -> SetOptions.mergeFields(*merge.fields)
}

internal actual val firestoreInstance: FirebaseFirestore by lazy {
    FirebaseFirestoreImpl(AndroidFirestore.getInstance())
}

internal actual fun getFirestoreInstance(app: FirebaseApp): FirebaseFirestore =
    FirebaseFirestoreImpl(AndroidFirestore.getInstance(app.app))

