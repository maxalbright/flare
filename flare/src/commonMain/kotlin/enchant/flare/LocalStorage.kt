package enchant.flare

class LocalStorage: FirebaseStorage {

    sealed class Node {
        private class FileNode(val name: String, val data: ByteArray)
        private class FolderNode(val name: String, val nodes: MutableMap<String, Node>)
    }
    private val db: MutableMap<String, Node> = mutableMapOf()

    override suspend fun deleteFile(path: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getBytes(path: String, maxDownloadSize: Long): Array<Byte> {
        TODO("Not yet implemented")
    }

    override suspend fun getDownloadUrl(path: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getFile(
        path: String,
        filePath: String,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getMetadata(path: String): StorageMetadata {
        TODO("Not yet implemented")
    }

    override suspend fun list(path: String, maxResults: Int?, pageToken: String?): ListResult {
        TODO("Not yet implemented")
    }

    override suspend fun putBytes(
        path: String,
        bytes: ByteArray,
        metadata: StorageMetadata?,
        onProgress: ((bytesUploaded: Long, totalBytes: Long) -> Unit)?
    ): StorageMetadata {
        TODO("Not yet implemented")
    }

    override suspend fun putFile(
        path: String,
        filePath: String,
        metadata: StorageMetadata?,
        onProgress: ((bytesUploaded: Long, totalBytes: Long) -> Unit)?
    ): StorageMetadata {
        TODO("Not yet implemented")
    }

    override suspend fun updateMetadata(path: String, metadata: StorageMetadata): StorageMetadata {
        TODO("Not yet implemented")
    }

    override val config: FirebaseStorage.Config
        get() = TODO("Not yet implemented")
}