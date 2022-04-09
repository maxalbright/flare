package enchant.flare

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import enchant.flare.FirestoreException.Code.*
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import enchant.flare.LocalFieldValue.*
import enchant.flare.QueryStep.*
import kotlinx.coroutines.flow.drop

class LocalFirestore : FirebaseFirestore {

    private val db: MutableMap<String, CollectionNode> = mutableMapOf()

    private class DocumentNode(
        val parent: CollectionNode,
        val id: String,
        val data: MutableMap<String, Any?> = mutableMapOf()
    ) {
        val collections: MutableMap<String, CollectionNode> = mutableMapOf()
        val updates: MutableStateFlow<Boolean> = MutableStateFlow(false)
        fun update() {
            updates.value = !updates.value
        }
    }

    private class DocumentImpl(
        val data: Map<String, Any?>,
        override val id: String,
        override val metadata: Map<FirestoreMetadata, Any> = mapOf()
    ) : MapDocument(data)

    private class CollectionNode(val id: String) {
        val documents: MutableMap<String, DocumentNode> = mutableMapOf()
        val updates: MutableStateFlow<Boolean> = MutableStateFlow(false)
        fun update() {
            updates.value = !updates.value
        }
    }

    private class CollectionImpl(
        documents: List<Document>,
        override val id: String,
        override val metadata: Map<FirestoreMetadata, Any> = mapOf()
    ) : ListCollection(documents)


    private fun getDocumentNode(path: String, error: Boolean = false): DocumentNode? {
        var node: Any? = null
        val ids = path.split("/")
        if (ids.size % 2 == 1) throw FirestoreException(
            InvalidArgument, "$path is an invalid document path"
        )
        ids.forEach { id ->
            node = when (node) {
                is DocumentNode -> (node as DocumentNode).collections[id]
                is CollectionNode -> (node as CollectionNode).documents[id]
                else -> db[id]
            }
            if (error) node ?: throw FirestoreException(
                NotFound, "Document not found at path $path"
            )
            node ?: return null
        }
        return node as DocumentNode
    }

    private fun getCollectionNode(path: String, error: Boolean = false): CollectionNode? {
        var node: Any? = null
        val ids = path.split("/")
        if (ids.size % 2 == 0) throw FirestoreException(
            InvalidArgument, "$path is an invalid collection path"
        )
        ids.forEach { id ->
            node = when (node) {
                null -> db[id]
                is DocumentNode -> (node as DocumentNode).collections[id]
                is CollectionNode -> (node as CollectionNode).documents[id]
                else -> error("Unexpected state")
            }
            if (error) node ?: throw FirestoreException(
                NotFound,
                "Collection not found at path $path"
            )
        }
        return node as? CollectionNode
    }

    private fun createDocument(path: String): DocumentNode {
        var node: Any? = null
        var ids = path.split("/")
        val idsAmount = ids.size
        if (ids.size % 2 == 1) throw FirestoreException(
            InvalidArgument, "$path is an invalid document path"
        )
        ids = ids.dropWhile { id ->
            val newNode: Any? = when (node) {
                is DocumentNode -> (node as DocumentNode).collections[id]
                is CollectionNode -> (node as CollectionNode).documents[id]
                else -> db[id]
            }
            if (newNode != null) node = newNode
            newNode != null
        }
        if (ids.size == idsAmount && ids.isNotEmpty() && ids.first() !in db) {
            node = CollectionNode(ids.first())
            db[ids.first()] = node as CollectionNode
            ids = ids.drop(1)
        }
        ids.forEach { id ->
            node = when (node) {
                is DocumentNode -> {
                    val collection = CollectionNode(id)
                    (node as DocumentNode).collections[id] = collection
                    collection
                }
                else -> {
                    val document = DocumentNode(node as CollectionNode, id)
                    (node as CollectionNode).documents[id] = document
                    document
                }
            }
        }
        return node as DocumentNode
    }

    private fun writeToDocument(node: DocumentNode, data: Map<String, Any?>) {
        data.forEach {
            if (it.value !is LocalFieldValue) node.data[it.key] = it.value
            else when (it.value as LocalFieldValue) {
                is IncrementDouble -> node.data[it.key] =
                    node.data[it.key] ?: 0.0 + (it.value as IncrementDouble).amount
                is IncrementLong -> node.data[it.key] =
                    node.data[it.key] ?: 0L + (it.value as IncrementDouble).amount
                ServerTimestamp -> node.data[it.key] = toDate(Clock.System.now())
                is ArrayRemove -> {
                    val array = node.data[it.key] as? List<*>
                    val set = (it.value as ArrayRemove).elements.toSet()
                    if (array != null) node.data[it.key] = array.filter { it !in set }
                }
                is ArrayUnion -> {
                    val array = node.data[it.key] as? List<*>
                    val set = (it.value as ArrayRemove).elements.toSet()
                    if (array != null) node.data[it.key] = array.filter { it !in set }
                }
                Delete -> node.data.remove(it.key)
            }
        }
    }

    /**
     * @param metadataChanges Has no effect
     */
    override fun getDocument(path: String, metadataChanges: Boolean): Flow<Document> {
        val node = getDocumentNode(path, true)!!
        return node.updates.map { DocumentImpl(node.data.toMap(), node.id) }
    }

    /**
     * @param source Has no effect
     */
    override suspend fun getDocumentOnce(path: String, source: Source): Document {
        val node = getDocumentNode(path, true)!!
        return DocumentImpl(node.data.toMap(), node.id)
    }

    override suspend fun getDocumentOnceOrNull(path: String, source: Source): Document? =
        try {
            getDocumentOnce(path, source)
        } catch (e: FirestoreException) {
            if (e.code == NotFound) null else throw e
        }

    override suspend fun setDocument(
        path: String,
        map: Map<String, Any?>,
        merge: Merge,
        changes: (Changes.() -> Unit)?
    ) {
        val localChanges = LocalChangesImpl(map)
        if (changes != null) localChanges.changes()
        var data = if (changes != null) localChanges.data else map.toMap()

        var node = getDocumentNode(path, false)

        if (node == null) node = createDocument(path)
        else if (merge == Merge.None) node.data.clear()
        else if (merge is Merge.Fields) data = merge.fields.associateWith {
            data[it] ?: throw FirestoreException(
                InvalidArgument,
                "Field [$it] does not appear in the contents of the data being set"
            )
        }

        writeToDocument(node, data)
        node.update()
        node.parent.update()
    }


    override suspend fun updateDocument(
        path: String,
        map: Map<String, Any?>,
        changes: (Changes.() -> Unit)?
    ) {
        val node = getDocumentNode(path, true)!!
        val localChanges = LocalChangesImpl(map)
        if (changes != null) localChanges.changes()
        val data = if (changes != null) localChanges.data else map.toMap()

        writeToDocument(node, data)
        node.update()
    }

    override suspend fun deleteDocument(path: String) {
        val node = getDocumentNode(path)
        node?.parent?.documents?.remove(node.id)
        node?.parent?.update()
    }

    override fun getCollection(
        path: String,
        metadataChanges: Boolean,
        query: (Query.() -> Unit)?
    ): Flow<Collection> {
        val node = getCollectionNode(path)
            ?: createDocument("$path/_tmp").parent.also { it.documents.remove("_tmp") }
        val localQuery = LocalQuery()
        if (query != null) localQuery.query()
        return node.updates.map {
            CollectionImpl(localQuery.apply(node.documents.map {
                DocumentImpl(it.value.data, it.key)
            }), node.id)
        }.drop(1)
    }

    override suspend fun getCollectionOnce(
        path: String,
        source: Source,
        query: (Query.() -> Unit)?
    ): Collection {
        val node = getCollectionNode(path)
            ?: createDocument("$path/_tmp").parent.also { it.documents.remove("_tmp") }
        val localQuery = LocalQuery()
        if (query != null) localQuery.query()
        return CollectionImpl(localQuery.apply(node.documents.map {
            DocumentImpl(it.value.data, it.key)
        }), node.id)
    }

    override fun getNamedQuery(
        name: String,
        metadataChanges: Boolean,
        query: Query.() -> Unit
    ): Flow<Collection> = TODO("Not yet implemented")

    override suspend fun getNamedQueryOnce(
        name: String,
        source: Source,
        query: Query.() -> Unit
    ): Collection = TODO("Not yet implemented")

    override suspend fun batch(batch: WriteBatch.() -> Unit) {
        TODO("Not yet implemented")
    }

    override suspend fun transaction(transaction: Transaction.() -> Unit) {
        TODO("Not yet implemented")
    }

    override val config: FirebaseFirestore.Config
        get() = TODO("Not yet implemented")

    companion object {
        val instance: LocalFirestore by lazy { LocalFirestore() }
    }
}

private class LocalChangesImpl(data: Map<String, Any?>) : Changes {

    val data: MutableMap<String, Any?> = data.toMutableMap()

    fun check(field: String) {
        if (data[field] is LocalFieldValue) throw FirestoreException(
            InvalidArgument,
            "Field [$field] attempted to apply multiple changes, which is not permitted"
        )
    }

    override fun arrayRemove(field: String, vararg elements: Any) {
        check(field)
        data[field] = ArrayRemove(elements)
    }

    override fun arrayUnion(field: String, vararg elements: Any) {
        check(field)
        data[field] = ArrayUnion(elements)
    }

    override fun delete(field: String) {
        check(field)
        data[field] = Delete
    }

    override fun increment(field: String, amount: Double) {
        check(field)
        data[field] = IncrementDouble(amount)
    }

    override fun increment(field: String, amount: Long) {
        check(field)
        data[field] = IncrementLong(amount)
    }

    override fun serverTimestamp(field: String) {
        check(field)
        data[field] = ServerTimestamp
    }
}

private sealed class LocalFieldValue {
    class ArrayRemove(vararg val elements: Any) : LocalFieldValue()
    class ArrayUnion(vararg val elements: Any) : LocalFieldValue()

    object Delete : LocalFieldValue()
    class IncrementDouble(val amount: Double) : LocalFieldValue()
    class IncrementLong(val amount: Long) : LocalFieldValue()
    object ServerTimestamp : LocalFieldValue()
}

private class LocalQuery() : Query {

    var compoundField: String? = null //One field for range and not equals clauses
    var arrayContains: Boolean = false // One array-contains clause
    var arrayContainsAny: Boolean = false //One in, not-in, array-contains-any clause
    val inFields: MutableSet<String> = mutableSetOf()
    val instructions = ArrayDeque<QueryStep>()

    fun apply(data: List<Document>): List<Document> {

        val docs = data.asSequence()
        instructions.forEach { step ->
            when (step) {
                is Limit -> if (step.toLast) docs.take(step.limit.toInt()) else docs.take(step.limit.toInt())
                is OrderBy -> if (step.direction == Direction.Ascending)
                    docs.sortedBy { it[step.field] as Comparable<Any> }
                else docs.sortedByDescending { it[step.field] as Comparable<Any> }
                is WhereArrayContains -> docs.filter {
                    (it[step.field] as List<Any>).containsAll(step.value.toList())
                }
                is WhereEqualTo -> docs.filter { it[step.field] == step.value }
                is WhereGreaterThan -> docs.filter { (it[step.field] as Comparable<Any>) > step.value }
                is WhereGreaterThanOrEqualTo -> docs.filter { (it[step.field] as Comparable<Any>) >= step.value }
                is WhereIn -> docs.filter { it[step.field] in step.value }
                is WhereLessThan -> docs.filter { (it[step.field] as Comparable<Any>) < step.value }
                is WhereLessThanOrEqualTo -> docs.filter { (it[step.field] as Comparable<Any>) <= step.value }
                is WhereNotEqualTo -> docs.filter { it[step.field] != step.value }
                is WhereNotIn -> docs.filter { it[step.field] !in step.value }
            }
        }
        return docs.toList()
    }

    private fun checkCompoundField(field: String) {
        if (field != compoundField && compoundField != null) {
            throw FirestoreException(
                InvalidArgument, "Range and not equals comparison must all filter on the same field"
            )
        }
        compoundField = field
    }

    private fun checkArrayContains() {
        if (!arrayContains) {
            throw FirestoreException(
                InvalidArgument, "Only one array contains clause is allowed per query"
            )
        }
        arrayContains = true
    }

    private fun checkArrayContainsAny() {
        if (!arrayContainsAny) {
            throw FirestoreException(
                InvalidArgument,
                "Only one in, not-in or array-contains-any clause is allowed per query"
            )
        }
        arrayContainsAny = true
    }

    private fun checkOrderBy(field: String) {
        if (field in inFields) {
            throw FirestoreException(
                InvalidArgument, "Cannot order query by a field included in an in clause"
            )
        }
    }

    override fun limit(limit: Long, toLast: Boolean) {
        instructions += Limit(limit, toLast)
    }

    override fun orderBy(field: String, direction: Direction) {
        checkOrderBy(field)
        instructions += OrderBy(field, direction)
    }

    override fun whereArrayContains(field: String, vararg value: Any) {
        checkArrayContains()
        instructions += WhereArrayContains(field, value)
    }

    override fun whereEqualTo(field: String, value: Any) {
        instructions += WhereEqualTo(field, value)
    }

    override fun whereNotEqualTo(field: String, value: Any) {
        checkCompoundField(field)
        instructions += WhereNotEqualTo(field, value)
    }

    override fun whereGreaterThan(field: String, value: Any) {
        checkCompoundField(field)
        instructions += WhereGreaterThan(field, value)
    }

    override fun whereGreaterThanOrEqualTo(field: String, value: Any) {
        checkCompoundField(field)
        instructions += WhereGreaterThanOrEqualTo(field, value)
    }

    override fun whereIn(field: String, vararg value: Any) {
        checkArrayContainsAny()
        inFields += field
        instructions += WhereIn(field, value)
    }

    override fun whereNotIn(field: String, vararg value: Any) {
        checkArrayContainsAny()
        checkCompoundField(field)
        instructions += WhereNotIn(field, value)
    }

    override fun whereLessThan(field: String, value: Any) {
        checkCompoundField(field)
        instructions += WhereLessThan(field, value)
    }

    override fun whereLessThanOrEqualTo(field: String, value: Any) {
        checkCompoundField(field)
        instructions += WhereLessThanOrEqualTo(field, value)
    }
}

private sealed class QueryStep {
    class Limit(val limit: Long, val toLast: Boolean) : QueryStep()

    class OrderBy(val field: String, val direction: Direction) : QueryStep()

    class WhereArrayContains(val field: String, vararg val value: Any) : QueryStep()

    class WhereEqualTo(val field: String, val value: Any) : QueryStep()

    class WhereNotEqualTo(val field: String, val value: Any) : QueryStep()

    class WhereGreaterThan(val field: String, val value: Any) : QueryStep()

    class WhereGreaterThanOrEqualTo(val field: String, val value: Any) : QueryStep()

    class WhereIn(val field: String, vararg val value: Any) : QueryStep()

    class WhereNotIn(val field: String, vararg val value: Any) : QueryStep()

    class WhereLessThan(val field: String, val value: Any) : QueryStep()

    class WhereLessThanOrEqualTo(val field: String, val value: Any) : QueryStep()
}