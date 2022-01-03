package enchant.flare

import cocoapods.FirebaseFirestore.*
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.dataWithBytes
import kotlin.coroutines.resume

private class DocumentImpl(val document: FIRDocumentSnapshot) :
    MapDocument((document.data() ?: emptyMap<String, Any>()) as Map<String, Any>) {
    override val id: String = document.documentID
    override val metadata: Map<FirestoreMetadata, Any> = mapOf(
        FirestoreMetadata.PendingWrites to document.metadata.hasPendingWrites(),
        FirestoreMetadata.FromCache to document.metadata.fromCache
    )
}

private class CollectionImpl(val collection: FIRQuerySnapshot, override val id: String) :
    ListCollection(collection.documents.map { DocumentImpl(it as FIRDocumentSnapshot) }) {
    override val metadata: Map<FirestoreMetadata, Any> = mapOf(
        FirestoreMetadata.PendingWrites to collection.metadata.hasPendingWrites(),
        FirestoreMetadata.FromCache to collection.metadata.fromCache
    )
}

private class FirebaseFirestoreImpl(private val firestore: FIRFirestore) :
    FirebaseFirestore {
    override fun getDocument(path: String, metadataChanges: Boolean): Flow<Document> =
        callbackFlow {
            val registration = firestore.documentWithPath(path)
                .addSnapshotListenerWithIncludeMetadataChanges(metadataChanges) { data, error ->
                    when {
                        data != null -> trySendBlocking(DocumentImpl(data))
                        error!!.code == FIRFirestoreErrorCodeCancelled -> return@addSnapshotListenerWithIncludeMetadataChanges
                        else -> throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
                    }
                }
            awaitClose { registration.remove() }
        }

    override suspend fun getDocumentOnce(path: String, source: Source): Document =
        suspendCancellableCoroutine { c ->
            firestore.documentWithPath(path)
                .getDocumentWithSource(toFIRSource(source)) { data, error ->
                    if (data != null) c.resume(DocumentImpl(data))
                    else throw FirebaseFirestoreException(toFirestoreExceptionCode(error!!.code))
                }
        }


    override suspend fun setDocument(
        path: String,
        data: Map<String, Any>,
        merge: Merge,
        changes: (Changes.() -> Unit)?
    ): Unit =
        suspendCancellableCoroutine { c ->
            val d = if(changes == null) data else ChangesImpl(data).apply(changes).newData
            val completion: (NSError?) -> Unit = { error ->
                if (error == null) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
            }
            val document = firestore.documentWithPath(path)
            if (merge is Merge.Fields)
                document.setData(d as Map<Any?, *>, merge.fields.asList(), completion)
            else document.setData(d as Map<Any?, *>, merge == Merge.All, completion)
        }

    override suspend fun updateDocument(path: String, data: Map<String, Any>, changes: (Changes.() -> Unit)?): Unit =
        suspendCancellableCoroutine { c ->
            val d = if(changes == null) data else ChangesImpl(data).apply(changes).newData
            val document = firestore.documentWithPath(path)
            document.updateData(d as Map<Any?, *>) { error ->
                if (error == null) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
            }
        }

    override suspend fun deleteDocument(path: String): Unit =
        suspendCancellableCoroutine { c ->
            val document = firestore.documentWithPath(path)
            document.deleteDocumentWithCompletion { error ->
                if (error == null) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
            }
        }

    override fun getCollection(
        path: String,
        metadataChanges: Boolean,
        query: Query.() -> Unit
    ): Flow<Collection> = callbackFlow {
        val collection = firestore.collectionWithPath(path)
        val q = QueryImpl(collection).also { query(it) }.query
        val registration =
            q.addSnapshotListenerWithIncludeMetadataChanges(metadataChanges) { data, error ->
                when {
                    data != null ->
                        trySendBlocking(CollectionImpl(data, path.takeLastWhile { it != '/' }))
                    error!!.code == FIRFirestoreErrorCodeCancelled -> return@addSnapshotListenerWithIncludeMetadataChanges
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
        val collection = firestore.collectionWithPath(path)
        val q = QueryImpl(collection).also { query(it) }.query
        q.getDocumentsWithSource(toFIRSource(source)) { data, error ->
            if (data != null) c.resume(CollectionImpl(data, path.takeLastWhile { it != '/' }))
            else throw FirebaseFirestoreException(toFirestoreExceptionCode(error!!.code))
        }
    }

    override fun getNamedQuery(
        name: String,
        metadataChanges: Boolean,
        query: Query.() -> Unit
    ): Flow<Collection> = callbackFlow {

        var registration: FIRListenerRegistrationProtocol? = null
        firestore.getQueryNamed(name) {
            if (it == null) throw FirebaseFirestoreException(
                toFirestoreExceptionCode(FIRFirestoreErrorCodeNotFound)
            )
            val q = QueryImpl(it).also { query(it) }.query
            registration =
                q.addSnapshotListenerWithIncludeMetadataChanges(metadataChanges) { data, error ->
                    when {
                        data != null -> trySendBlocking(
                            CollectionImpl(
                                data, (data.documents[0] as FIRDocumentSnapshot)
                                    .reference.parent.collectionID
                            )
                        )
                        error!!.code == FIRFirestoreErrorCodeCancelled -> return@addSnapshotListenerWithIncludeMetadataChanges
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

        firestore.getQueryNamed(name) {
            if (it == null) throw FirebaseFirestoreException(
                toFirestoreExceptionCode(FIRFirestoreErrorCodeNotFound)
            )
            val q = QueryImpl(it).also { query(it) }.query
            q.getDocumentsWithSource(toFIRSource(source)) { data, error ->
                when {
                    data != null -> c.resume(
                        CollectionImpl(
                            data,
                            (data.documents[0] as FIRDocumentSnapshot).reference.parent.collectionID
                        )
                    )
                    error!!.code == FIRFirestoreErrorCodeCancelled -> return@getDocumentsWithSource
                    else -> throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
                }
            }
        }
    }

    override suspend fun batch(batch: WriteBatch.() -> Unit): Unit =
        suspendCancellableCoroutine { c ->
            firestore.batch().also { batch(WriteBatchImpl(firestore, it)) }.commitWithCompletion {
                if (it == null) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode(it.code))
            }
        }

    override suspend fun transaction(transaction: Transaction.() -> Unit): Unit =
        suspendCancellableCoroutine { c ->
            firestore.runTransactionWithBlock({ transaction, _ ->
                transaction(TransactionImpl(firestore, transaction!!))
            }) { data, error ->
                if (error == null) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
            }
        }

    override val config: FirebaseFirestore.Config = object : FirebaseFirestore.Config {
        override var settings: FirebaseFirestoreSettings
            get() = toFirestoreSettings(firestore.settings)
            set(value) {
                firestore.settings = toFIRFirestoreSettings(value)
            }

        override fun useEmulator(host: String, port: Int): Unit =
            firestore.useEmulatorWithHost(host, port.toLong())

        override suspend fun loadBundle(data: Array<Byte>): Unit =
            suspendCancellableCoroutine { c ->
                memScoped {
                    val p = allocArrayOf(data.toByteArray())
                    firestore.loadBundle(
                        NSData.dataWithBytes(
                            p,
                            data.size.toULong()
                        )
                    ) { data, error ->
                        if (error == null) c.resume(Unit)
                        else throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
                    }
                }
            }

        override suspend fun snapshotsInSync(action: () -> Unit): Unit =
            suspendCancellableCoroutine { c ->
                val registration = firestore.addSnapshotsInSyncListener(action)
                c.invokeOnCancellation { registration.remove() }
            }

        override suspend fun clearPersistence(): Unit = suspendCancellableCoroutine { c ->
            firestore.clearPersistenceWithCompletion { error ->
                if (error == null) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
            }
        }

        override suspend fun enableNetwork(enabled: Boolean): Unit =
            suspendCancellableCoroutine { c ->
                val completion: (NSError?) -> Unit = { error ->
                    if (error == null) c.resume(Unit)
                    else throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
                }
                if (enabled) firestore.enableNetworkWithCompletion(completion)
                else firestore.disableNetworkWithCompletion(completion)
            }

        override suspend fun terminate(): Unit = suspendCancellableCoroutine { c ->
            firestore.terminateWithCompletion { error ->
                if (error == null) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
            }
        }

        override suspend fun waitForPendingWrites(): Unit = suspendCancellableCoroutine { c ->
            firestore.waitForPendingWritesWithCompletion { error ->
                if (error == null) c.resume(Unit)
                else throw FirebaseFirestoreException(toFirestoreExceptionCode(error.code))
            }
        }

    }

    private fun toFirestoreExceptionCode(code: Long): FirebaseFirestoreException.Code =
        when (code) {
            FIRFirestoreErrorCodeInvalidArgument -> FirebaseFirestoreException.Code.InvalidArgument
            FIRFirestoreErrorCodeAlreadyExists -> FirebaseFirestoreException.Code.AlreadyExists
            FIRFirestoreErrorCodeDeadlineExceeded -> FirebaseFirestoreException.Code.DeadlineExceeded
            FIRFirestoreErrorCodeNotFound -> FirebaseFirestoreException.Code.NotFound
            FIRFirestoreErrorCodePermissionDenied -> FirebaseFirestoreException.Code.PermissionDenied
            FIRFirestoreErrorCodeResourceExhausted -> FirebaseFirestoreException.Code.ResourceExhausted
            FIRFirestoreErrorCodeFailedPrecondition -> FirebaseFirestoreException.Code.FailedPrecondition
            FIRFirestoreErrorCodeAborted -> FirebaseFirestoreException.Code.Aborted
            FIRFirestoreErrorCodeOutOfRange -> FirebaseFirestoreException.Code.OutOfRange
            FIRFirestoreErrorCodeUnimplemented -> FirebaseFirestoreException.Code.Unimplemented
            FIRFirestoreErrorCodeInternal -> FirebaseFirestoreException.Code.Internal
            FIRFirestoreErrorCodeUnavailable -> FirebaseFirestoreException.Code.Unavailable
            FIRFirestoreErrorCodeDataLoss -> FirebaseFirestoreException.Code.DataLoss
            FIRFirestoreErrorCodeUnauthenticated -> FirebaseFirestoreException.Code.Unauthenticated
            else -> FirebaseFirestoreException.Code.Unknown.also { println("Encountered unknown firestore error code: $code") }
            //TODO: Better unknown error output
        }

    private fun toFIRSource(source: Source): FIRFirestoreSource = when (source) {
        Source.Default -> FIRFirestoreSource.FIRFirestoreSourceDefault
        Source.Cache -> FIRFirestoreSource.FIRFirestoreSourceCache
        Source.Server -> FIRFirestoreSource.FIRFirestoreSourceServer
    }

    private fun toFirestoreSettings(settings: FIRFirestoreSettings): FirebaseFirestoreSettings =
        FirebaseFirestoreSettings(
            cacheSize = settings.cacheSizeBytes,
            host = settings.host,
            persistenceEnabled = settings.persistenceEnabled,
            sslEnabled = settings.sslEnabled
        )

    private fun toFIRFirestoreSettings(settings: FirebaseFirestoreSettings): FIRFirestoreSettings =
        FIRFirestoreSettings().apply {
            cacheSizeBytes = settings.cacheSize
            host = settings.host
            persistenceEnabled = settings.persistenceEnabled
            sslEnabled = settings.sslEnabled
        }
}

private class QueryImpl(var query: FIRQuery) : Query {

    override fun limit(limit: Long, toLast: Boolean) {
        query = if(toLast)query.queryLimitedToLast(limit) else query.queryLimitedTo(limit)
    }

    override fun orderBy(field: String, direction: Direction) {
        query = query.queryOrderedByField(field, direction == Direction.Descending)
    }

    override fun whereArrayContains(field: String, vararg value: Any) {
        query = query.queryWhereField(field, arrayContains = value)
    }

    override fun whereEqualTo(field: String, value: Any) {
        query = query.queryWhereField(field, isEqualTo = value)
    }

    override fun whereNotEqualTo(field: String, value: Any) {
        query = query.queryWhereField(field, isNotEqualTo = value)
    }

    override fun whereGreaterThan(field: String, value: Any) {
        query = query.queryWhereField(field, isGreaterThan = value)
    }

    override fun whereGreaterThanOrEqualTo(field: String, value: Any) {
        query = query.queryWhereField(field, isGreaterThanOrEqualTo = value)
    }

    override fun whereIn(field: String, vararg values: Any) {
        query = query.queryWhereField(field, `in` = values.toList())
    }

    override fun whereNotIn(field: String, vararg values: Any) {
        query = query.queryWhereField(field, notIn = values.toList())
    }

    override fun whereLessThan(field: String, value: Any) {
        query = query.queryWhereField(field, isLessThan = value)
    }

    override fun whereLessThanOrEqualTo(field: String, value: Any) {
        query = query.queryWhereField(field, isLessThanOrEqualTo = value)
    }

}

private class WriteBatchImpl(var firestore: FIRFirestore, var batch: FIRWriteBatch) :
    WriteBatch {
    override fun set(path: String, data: Map<String, Any>, merge: Merge) {
        batch = if (merge is Merge.Fields) batch.setData(
            data as Map<Any?, *>, firestore.documentWithPath(path), merge.fields.asList()
        )
        else batch.setData(
            data as Map<Any?, *>, firestore.documentWithPath(path), merge == Merge.All
        )
    }

    override fun update(path: String, data: Map<String, Any>) {
        batch = batch.updateData(data as Map<Any?, *>, firestore.documentWithPath(path))
    }

    override fun delete(path: String) {
        batch = batch.deleteDocument(firestore.documentWithPath(path))
    }
}

private class TransactionImpl(
    var firestore: FIRFirestore,
    var transaction: FIRTransaction
) : Transaction {
    override fun get(path: String): Document =
        DocumentImpl(transaction.getDocument(firestore.documentWithPath("path"), null)!!)


    override fun set(path: String, data: Map<String, Any>, merge: Merge) {
        transaction = if (merge is Merge.Fields) transaction.setData(
            data as Map<Any?, *>, firestore.documentWithPath(path), merge.fields.asList()
        )
        else transaction.setData(
            data as Map<Any?, *>, firestore.documentWithPath(path), merge == Merge.All
        )
    }

    override fun update(path: String, data: Map<String, Any>) {
        transaction = transaction.updateData(data as Map<Any?, *>, firestore.documentWithPath(path))
    }

    override fun delete(path: String) {
        transaction = transaction.deleteDocument(firestore.documentWithPath(path))
    }
}

private class ChangesImpl(data: Map<String, Any>): Changes {
    val newData: MutableMap<String, Any> = data.toMutableMap()

    override fun arrayRemove(field: String, vararg elements: Any) {
        newData[field] = FIRFieldValue.fieldValueForArrayRemove(elements.toList())
    }

    override fun arrayUnion(field: String, vararg elements: Any) {
        newData[field] = FIRFieldValue.fieldValueForArrayUnion(elements.toList())
    }

    override fun delete(field: String) {
        newData[field] = FIRFieldValue.fieldValueForDelete()
    }

    override fun increment(field: String, amount: Double) {
        newData[field] = FIRFieldValue.fieldValueForDoubleIncrement(amount)
    }

    override fun increment(field: String, amount: Long) {
        newData[field] = FIRFieldValue.fieldValueForIntegerIncrement(amount)
    }

    override fun serverTimestamp(field: String) {
        newData[field] = FIRFieldValue.fieldValueForServerTimestamp()
    }

}

internal actual val firestoreInstance: FirebaseFirestore by lazy {
    FirebaseFirestoreImpl(FIRFirestore.firestore())
}

@Suppress("TYPE_MISMATCH")
internal actual fun getFirestoreInstance(app: FirebaseApp): FirebaseFirestore =
    FirebaseFirestoreImpl(FIRFirestore.firestoreForApp(app.app))

