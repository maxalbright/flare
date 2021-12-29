package enchant.flare

interface File {

}

interface FirebaseStorage {

    suspend fun deleteFile(path: String)
    fun getActiveDownloadPaths(path: String): List<String>
    fun getActiveUploadPaths(path: String): List<String>

    suspend fun getBytes(
        path: String, maxDownloadSize: Long,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null
    ): Array<Byte>

    suspend fun getDownloadUrl(path: String): String
    suspend fun getFile(
        path: String, onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null
    ): File

    suspend fun getMetadata(path: String): StorageMetadata
    suspend fun list(path: String, maxResults: Int? = null, pageToken: String? = null): ListResult
    suspend fun putBytes(
        path: String, bytes: Array<Byte>, metadata: StorageMetadata? = null,
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
}

data class StorageMetadata(
    val bucket: String,
    val cacheControl: String,
    val contentDisposition: String,
    val contentEncoding: String,
    val contentLanguage: String,
    val contentType: String,
    val creationTime: Long,
    val customMetadataKeys: Set<String>,
    val generation: String,
    val md5Hash: String,
    val metadataGeneration: String,
    val name: String,
    val path: String,
    val size: Long,
    val updatedTime: Long,
    val customMetadata: Map<String, String>
)

interface ListResult {
    val items: List<String>
    val pageToken: String
}

class StorageException(val code: Code) : Exception("Cloud storage operation failed with code: $code") {

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