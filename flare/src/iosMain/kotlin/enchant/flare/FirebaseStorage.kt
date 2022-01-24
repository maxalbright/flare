package enchant.flare

import cocoapods.FirebaseStorage.*
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseStorageImpl(private val storage: FIRStorage) : FirebaseStorage {

    override suspend fun deleteFile(path: String): Unit = suspendCancellableCoroutine { c ->
        storage.referenceWithPath(path).deleteWithCompletion { error ->
            if (error == null) c.resume(Unit)
            else c.resumeWithException(toStorageException(error))
        }
    }

    override suspend fun getBytes(
        path: String, maxDownloadSize: Long
    ): ByteArray = suspendCancellableCoroutine { c ->
        storage.referenceWithPath(path).dataWithMaxSize(maxDownloadSize) { data, error ->

            if (data != null) memScoped {
                val p = StableRef.create(ByteArray(data.length.toInt()))
                data.getBytes(p.asCPointer())
                c.resume(p.get())
            }
            else c.resumeWithException(toStorageException(error!!))
        }
    }

    override suspend fun getDownloadUrl(path: String): String = suspendCancellableCoroutine { c ->
        storage.referenceWithPath(path).downloadURLWithCompletion { data, error ->
            if (data != null) c.resume(data.toString())
            else c.resumeWithException(toStorageException(error!!))
        }
    }

    override suspend fun getFile(
        path: String, filePath: String,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)?
    ): Unit = suspendCancellableCoroutine { c ->
        val task =
            storage.referenceWithPath(path)
                .writeToFile(NSURL(fileURLWithPath = filePath)) { data, error ->
                    if (data != null) c.resume(Unit)
                    else if (error!!.code == FIRStorageErrorCodeCancelled) return@writeToFile
                    else c.resumeWithException(toStorageException(error))
                }
        if (onProgress != null) task.observeStatus(FIRStorageTaskStatus.FIRStorageTaskStatusProgress) { snapshot ->
            onProgress(snapshot!!.progress!!.completedUnitCount, snapshot.progress!!.totalUnitCount)
        }
        c.invokeOnCancellation { task.cancel() }
    }

    override suspend fun getMetadata(path: String): StorageMetadata =
        suspendCancellableCoroutine { c ->
            storage.referenceWithPath(path).metadataWithCompletion { data, error ->
                if (data != null) c.resume(toStorageMetadata(data))
                else c.resumeWithException(toStorageException(error!!))
            }
        }

    override suspend fun list(path: String, maxResults: Int?, pageToken: String?): ListResult {
        return suspendCancellableCoroutine { c ->
            val completion: (FIRStorageListResult?, NSError?) -> Unit = { data, error ->
                if (data != null) c.resume(
                    ListResult(
                        data.items.map { (it as FIRStorageReference).fullPath }, data.pageToken
                    )
                )
                else c.resumeWithException(toStorageException(error!!))
            }
            when {
                maxResults == null -> storage.referenceWithPath(path)
                    .listAllWithCompletion(completion)
                pageToken == null -> storage.referenceWithPath(path)
                    .listWithMaxResults(maxResults.toLong(), completion)
                else -> storage.referenceWithPath(path)
                    .listWithMaxResults(maxResults.toLong(), pageToken, completion)
            }
        }
    }

    override suspend fun putBytes(
        path: String,
        bytes: ByteArray,
        metadata: FileMetadata?,
        onProgress: ((bytesUploaded: Long, totalBytes: Long) -> Unit)?
    ): StorageMetadata = suspendCancellableCoroutine { c ->
        var d: NSData? = null
        memScoped {
            d = NSData.dataWithBytesNoCopy(
                StableRef.create(bytes).asCPointer(), bytes.size.toULong()
            )
        }
        val task = storage.referenceWithPath(path).putData(d!!,
            metadata?.let { toFIRMetadata(it) }) { data, error ->
            if (data != null) c.resume(toStorageMetadata(data))
            else if (error!!.code == FIRStorageErrorCodeCancelled) return@putData
            else c.resumeWithException(toStorageException(error))
        }
        if (onProgress != null) task.observeStatus(FIRStorageTaskStatus.FIRStorageTaskStatusProgress) { snapshot ->
            onProgress(snapshot!!.progress!!.completedUnitCount, snapshot.progress!!.totalUnitCount)
        }
        c.invokeOnCancellation { task.cancel() }
    }

    override suspend fun putFile(
        path: String,
        filePath: String,
        metadata: FileMetadata?,
        onProgress: ((bytesUploaded: Long, totalBytes: Long) -> Unit)?
    ): StorageMetadata = suspendCancellableCoroutine { c ->
        val task = storage.referenceWithPath(path).putFile(NSURL(fileURLWithPath = filePath),
            metadata?.let { toFIRMetadata(it) }) { data, error ->
            if (data != null) c.resume(toStorageMetadata(data))
            else if (error!!.code == FIRStorageErrorCodeCancelled) return@putFile
            else c.resumeWithException(toStorageException(error))
        }
        if (onProgress != null) task.observeStatus(FIRStorageTaskStatus.FIRStorageTaskStatusProgress) { snapshot ->
            onProgress(snapshot!!.progress!!.completedUnitCount, snapshot.progress!!.totalUnitCount)
        }
        c.invokeOnCancellation { task.cancel() }
    }

    override suspend fun updateMetadata(path: String, metadata: FileMetadata): StorageMetadata =
        suspendCancellableCoroutine { c ->
            storage.referenceWithPath(path).updateMetadata(toFIRMetadata(metadata)) { data, error ->
                if (data != null) c.resume(toStorageMetadata(data))
                else c.resumeWithException(toStorageException(error!!))
            }
        }


    override val config: FirebaseStorage.Config
        get() = object : FirebaseStorage.Config {
            override var maxDownloadRetryTime: Long
                get() = storage.maxDownloadRetryTime.toLong()
                set(value) {
                    storage.setMaxDownloadRetryTime(value.toDouble())
                }
            override var maxOperationRetryTime: Long
                get() = storage.maxOperationRetryTime.toLong()
                set(value) {
                    storage.setMaxOperationRetryTime(value.toDouble())
                }
            override var maxUploadRetryTime: Long
                get() = storage.maxUploadRetryTime.toLong()
                set(value) {
                    storage.setMaxUploadRetryTime(value.toDouble())
                }
            override val bucket: String
                get() = storage.reference().bucket

            override fun useEmulator(host: String, port: Int) =
                storage.useEmulatorWithHost(host, port.toLong())

        }

    private fun toFIRMetadata(metadata: FileMetadata): FIRStorageMetadata =
        FIRStorageMetadata().apply {
            cacheControl = metadata.cacheControl
            contentDisposition = metadata.contentDisposition
            contentEncoding = metadata.contentEncoding
            contentLanguage = metadata.contentLanguage
            contentType = metadata.contentType
            setCustomMetadata(metadata.customMetadata as Map<Any?, *>)
        }

    private fun toStorageMetadata(metadata: FIRStorageMetadata?): StorageMetadata = StorageMetadata(
        bucket = metadata!!.bucket,
        cacheControl = metadata.cacheControl,
        contentDisposition = metadata.contentDisposition,
        contentEncoding = metadata.contentEncoding,
        contentLanguage = metadata.contentLanguage,
        contentType = metadata.contentType,
        creationTime = metadata.timeCreated!!.timeIntervalSince1970.toLong(),
        generation = metadata.generation,
        md5Hash = metadata.md5Hash,
        metadataGeneration = metadata.metageneration,
        name = metadata.name,
        path = metadata.path!!,
        size = metadata.size,
        updatedTime = metadata.updated!!.timeIntervalSince1970.toLong(),
        customMetadata = metadata.customMetadata as Map<String, String>
    )
}

private fun toStorageException(error: NSError): StorageException {
    val code = when (error.code) {
        FIRStorageErrorCodeBucketNotFound -> StorageException.Code.BucketNotFound
        FIRStorageErrorCodeNonMatchingChecksum -> StorageException.Code.InvalidChecksum
        FIRStorageErrorCodeUnauthenticated -> StorageException.Code.NotAuthenticated
        FIRStorageErrorCodeUnauthorized -> StorageException.Code.NotAuthorized
        FIRStorageErrorCodeProjectNotFound -> StorageException.Code.ProjectNotFound
        FIRStorageErrorCodeQuotaExceeded -> StorageException.Code.QuotaExceeded
        FIRStorageErrorCodeRetryLimitExceeded -> StorageException.Code.RetryLimitExceeded
        FIRStorageErrorCodeObjectNotFound -> StorageException.Code.ObjectNotFound
        else -> StorageException.Code.Unknown
    }
    return StorageException(code, error.description)
}

internal actual val firebaseStorageInstance: FirebaseStorage by lazy {
    FirebaseStorageImpl(FIRStorage.storage())
}

@Suppress("TYPE_MISMATCH")
internal actual fun getStorageInstance(app: FirebaseApp): FirebaseStorage =
    FirebaseStorageImpl(FIRStorage.storageForApp(app.app))