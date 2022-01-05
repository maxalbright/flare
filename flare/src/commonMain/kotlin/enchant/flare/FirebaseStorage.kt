package enchant.flare

interface FirebaseStorage {

    suspend fun deleteFile(path: String)

    suspend fun getBytes(path: String, maxDownloadSize: Long): Array<Byte>

    suspend fun getDownloadUrl(path: String): String
    suspend fun getFile(
        path: String, filePath: String, onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null
    )

    suspend fun getMetadata(path: String): StorageMetadata
    suspend fun list(path: String, maxResults: Int? = null, pageToken: String? = null): ListResult
    suspend fun putBytes(
        path: String, bytes: ByteArray, metadata: StorageMetadata? = null,
        onProgress: ((bytesUploaded: Long, totalBytes: Long) -> Unit)? = null
    ): StorageMetadata

    suspend fun putFile(
        path: String, filePath: String, metadata: StorageMetadata? = null,
        onProgress: ((bytesUploaded: Long, totalBytes: Long) -> Unit)? = null
    ): StorageMetadata

    suspend fun updateMetadata(path: String, metadata: StorageMetadata): StorageMetadata

    val config: Config

    interface Config {
        var maxDownloadRetryTime: Long
        var maxOperationRetryTime: Long
        var maxUploadRetryTime: Long
        val bucket: String
        fun useEmulator(host: String, port: Int)
    }

    companion object {
        val instance: FirebaseStorage = firebaseStorageInstance
        fun getInstance(app: FirebaseApp) = getStorageInstance(app)
    }
}

data class StorageMetadata(
    val bucket: String?,
    val cacheControl: String?,
    val contentDisposition: String?,
    val contentEncoding: String?,
    val contentLanguage: String?,
    val contentType: String?,
    val creationTime: Long,
    val generation: Long,
    val md5Hash: String?,
    val metadataGeneration: Long,
    val name: String?,
    val path: String,
    val size: Long,
    val updatedTime: Long,
    val customMetadata: Map<String, String>
)

data class ListResult(
    val items: List<String>,
    val pageToken: String?
)

class StorageException(val code: Code, val description: String? = null) :
    Exception("Firebase storage operation failed with code ${code.name}: $description") {

    enum class Code {
        BucketNotFound,
        InvalidChecksum,
        NotAuthenticated,
        NotAuthorized,
        ObjectNotFound,
        ProjectNotFound,
        QuotaExceeded,
        RetryLimitExceeded,
        Unknown
    }
}

internal expect val firebaseStorageInstance: FirebaseStorage
internal expect fun getStorageInstance(app: FirebaseApp): FirebaseStorage