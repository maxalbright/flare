package enchant.flare

import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.firebase.storage.FirebaseStorage as AndroidStorage
import com.google.firebase.storage.StorageException as AndroidException
import com.google.firebase.storage.StorageMetadata as AndroidMetadata

class FirebaseStorageImpl(val storage: AndroidStorage) : FirebaseStorage {

    override suspend fun deleteFile(path: String): Unit = suspendCancellableCoroutine { c ->
        storage.getReference(path).delete().addOnCompleteListener {
            if (it.isSuccessful) c.resume(Unit)
            else throw toStorageException(it.exception!!)
        }
    }

    override suspend fun getBytes(
        path: String, maxDownloadSize: Long
    ): Array<Byte> = suspendCancellableCoroutine { c ->
        storage.getReference(path).getBytes(maxDownloadSize).addOnCompleteListener {
            if (it.isSuccessful) c.resume(it.result!!.toTypedArray())
            else throw toStorageException(it.exception!!)
        }
    }

    override suspend fun getDownloadUrl(path: String): String = suspendCancellableCoroutine { c ->
        storage.getReference(path).downloadUrl.addOnCompleteListener {
            if (it.isSuccessful) c.resume(it.result!!.toString())
            else throw toStorageException(it.exception!!)
        }
    }

    override suspend fun getFile(
        path: String, filePath: String,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)?
    ): Unit = suspendCancellableCoroutine { c ->
        val task = storage.getReference(path).getFile(Uri.parse(filePath)).addOnCompleteListener {
            if (it.isSuccessful) c.resume(Unit)
            else if ((it.exception!! as? AndroidException)?.errorCode == AndroidException.ERROR_CANCELED)
                return@addOnCompleteListener
            else throw toStorageException(it.exception!!)
        }
        if (onProgress != null) task.addOnProgressListener {
            onProgress(it.bytesTransferred, it.totalByteCount)
        }
        c.invokeOnCancellation { task.cancel() }
    }

    override suspend fun getMetadata(path: String): StorageMetadata =
        suspendCancellableCoroutine { c ->
            storage.getReference(path).metadata.addOnCompleteListener {
                if (it.isSuccessful) c.resume(toStorageMetadata(it.result!!))
                else throw toStorageException(it.exception!!)
            }
        }

    override suspend fun list(path: String, maxResults: Int?, pageToken: String?): ListResult =
        suspendCancellableCoroutine { c ->
            when {
                maxResults == null -> storage.getReference(path).listAll()
                pageToken == null -> storage.getReference(path).list(maxResults)
                else -> storage.getReference(path).list(maxResults, pageToken)
            }.addOnCompleteListener {
                if (it.isSuccessful) c.resume(
                    ListResult(
                        it.result.items.map { it.path }, it.result.pageToken
                    )
                )
                else throw toStorageException(it.exception!!)
            }
        }

    override suspend fun putBytes(
        path: String,
        bytes: Array<Byte>,
        metadata: StorageMetadata?,
        onProgress: ((bytesUploaded: Long, totalBytes: Long) -> Unit)?
    ): StorageMetadata = suspendCancellableCoroutine { c ->

        val task = if (metadata == null) storage.getReference(path)
            .putBytes(bytes.toByteArray()) else storage.getReference(path)
            .putBytes(bytes.toByteArray(), toAndroidMetadata(metadata))

        task.addOnCompleteListener {
            if (it.isSuccessful) c.resume(toStorageMetadata(it.result!!.metadata))
            else if ((it.exception!! as? AndroidException)?.errorCode == AndroidException.ERROR_CANCELED)
                return@addOnCompleteListener
            else throw toStorageException(it.exception!!)
        }
        if (onProgress != null) task.addOnProgressListener {
            onProgress(it.bytesTransferred, it.totalByteCount)
        }
        c.invokeOnCancellation { task.cancel() }
    }

    override suspend fun putFile(
        path: String,
        filePath: String,
        metadata: StorageMetadata?,
        onProgress: ((bytesUploaded: Long, totalBytes: Long) -> Unit)?
    ): StorageMetadata = suspendCancellableCoroutine { c ->
        val task = if (metadata == null) storage.getReference(path)
            .putFile(Uri.parse(filePath)) else storage.getReference(path)
            .putFile(Uri.parse(filePath), toAndroidMetadata(metadata))
        task.addOnCompleteListener {
            if (it.isSuccessful) c.resume(toStorageMetadata(it.result!!.metadata))
            else if ((it.exception!! as? AndroidException)?.errorCode == AndroidException.ERROR_CANCELED)
                return@addOnCompleteListener
            else throw toStorageException(it.exception!!)
        }
        if (onProgress != null) task.addOnProgressListener {
            onProgress(it.bytesTransferred, it.totalByteCount)
        }
        c.invokeOnCancellation { task.cancel() }
    }

    override suspend fun updateMetadata(path: String, metadata: StorageMetadata): StorageMetadata =
        suspendCancellableCoroutine { c ->
            storage.getReference(path).updateMetadata(toAndroidMetadata(metadata))
                .addOnCompleteListener {
                    if (it.isSuccessful) c.resume(toStorageMetadata(it.result!!))
                    else throw toStorageException(it.exception!!)
                }
        }


    override val config: FirebaseStorage.Config
        get() = object : FirebaseStorage.Config {
            override var maxDownloadRetryTime: Long
                get() = storage.maxDownloadRetryTimeMillis
                set(value) {
                    storage.maxDownloadRetryTimeMillis = value
                }
            override var maxOperationRetryTime: Long
                get() = storage.maxOperationRetryTimeMillis
                set(value) {
                    storage.maxOperationRetryTimeMillis = value
                }
            override var maxUploadRetryTime: Long
                get() = storage.maxUploadRetryTimeMillis
                set(value) {
                    storage.maxUploadRetryTimeMillis = value
                }
            override val bucket: String
                get() = storage.reference.bucket

            override fun useEmulator(host: String, port: Int) =
                storage.useEmulator(host, port)

        }

    private fun toAndroidMetadata(metadata: StorageMetadata): AndroidMetadata =
        AndroidMetadata.Builder().apply {
            cacheControl = metadata.cacheControl
            contentDisposition = metadata.contentDisposition
            contentEncoding = metadata.contentEncoding
            contentLanguage = metadata.contentLanguage
            contentType = metadata.contentType
            metadata.customMetadata.forEach { setCustomMetadata(it.key, it.value) }
        }.build()

    private fun toStorageMetadata(metadata: AndroidMetadata?): StorageMetadata = StorageMetadata(
        bucket = metadata!!.bucket,
        cacheControl = metadata.cacheControl,
        contentDisposition = metadata.contentDisposition,
        contentEncoding = metadata.contentEncoding,
        contentLanguage = metadata.contentLanguage,
        contentType = metadata.contentType,
        creationTime = metadata.creationTimeMillis,
        generation = metadata.generation!!.toLong(),
        md5Hash = metadata.md5Hash,
        metadataGeneration = metadata.metadataGeneration!!.toLong(),
        name = metadata.name,
        path = metadata.path,
        size = metadata.sizeBytes,
        updatedTime = metadata.updatedTimeMillis,
        customMetadata = metadata.customMetadataKeys.associateWith { metadata.getCustomMetadata(it)!! }
    )
}

private fun toStorageException(exception: Exception): StorageException {
    if (exception !is AndroidException) throw(exception)
    return StorageException(when (exception.errorCode) {
        AndroidException.ERROR_BUCKET_NOT_FOUND -> StorageException.Code.BucketNotFound
        AndroidException.ERROR_INVALID_CHECKSUM -> StorageException.Code.InvalidChecksum
        AndroidException.ERROR_NOT_AUTHENTICATED -> StorageException.Code.NotAuthenticated
        AndroidException.ERROR_NOT_AUTHORIZED -> StorageException.Code.NotAuthorized
        AndroidException.ERROR_PROJECT_NOT_FOUND -> StorageException.Code.ProjectNotFound
        AndroidException.ERROR_QUOTA_EXCEEDED -> StorageException.Code.QuotaExceeded
        AndroidException.ERROR_RETRY_LIMIT_EXCEEDED -> StorageException.Code.RetryLimitExceeded
        AndroidException.ERROR_OBJECT_NOT_FOUND -> StorageException.Code.ObjectNotFound
        else -> StorageException.Code.Unknown.also { println("Encountered unknown firebase storage error code: ${exception.errorCode}") }
    })
}

internal actual val firebaseStorageInstance: FirebaseStorage by lazy {
    FirebaseStorageImpl(AndroidStorage.getInstance())
}

internal actual fun getStorageInstance(app: FirebaseApp): FirebaseStorage =
    FirebaseStorageImpl(AndroidStorage.getInstance(app.app))