package enchant.flare

import enchant.flare.StorageException.Code.*
import kotlinx.datetime.Clock

class LocalStorage : FirebaseStorage {

    private sealed class Node {
        class FileNode(val name: String, val data: ByteArray, var metadata: StorageMetadata) :
            Node()

        class FolderNode(
            val name: String,
            val nodes: MutableMap<String, Node> = mutableMapOf()
        ) : Node()
    }

    private val db: Node.FolderNode = Node.FolderNode("")

    override suspend fun deleteFile(path: String) {
        var folder: Node.FolderNode = db
        val paths = path.split("/")
        paths.forEachIndexed { index, s ->
            if (index == paths.lastIndex) {
                if (folder.nodes[s] is Node.FolderNode) throw StorageException(
                    ObjectNotFound,
                    "The provided path points to a folder: $path"
                )
                folder.nodes[s] as? Node.FileNode ?: throw
                StorageException(ObjectNotFound, "Could not find file at path: $path")

                folder.nodes -= s
                return@deleteFile
            } else {
                folder = folder.nodes[s] as? Node.FolderNode ?: throw
                StorageException(ObjectNotFound, "Could not find file at path: $path")
            }
        }
        throw StorageException(ObjectNotFound, "File path is invalid: $path")
    }

    private fun getFileNode(path: String): Node.FileNode {
        var folder: Node.FolderNode = db
        val paths = path.split("/")
        paths.forEachIndexed { index, s ->
            if (index == paths.lastIndex) {
                if (folder.nodes[s] is Node.FolderNode) throw StorageException(
                    ObjectNotFound,
                    "The provided path points to a folder: $path"
                )
                return@getFileNode folder.nodes[s] as? Node.FileNode ?: throw
                StorageException(ObjectNotFound, "Could not find file at path: $path")
            } else {
                folder = folder.nodes[s] as? Node.FolderNode ?: throw
                StorageException(ObjectNotFound, "Could not find file at path: $path")
            }
        }
        throw StorageException(ObjectNotFound, "File path is invalid: $path")
    }

    private fun getOrCreateFolderNode(path: String): Node.FolderNode {
        if (path.isEmpty()) return db
        var folder: Node.FolderNode = db
        val paths = path.split("/")
        paths.dropWhile {
            if (folder.nodes[it] is Node.FolderNode) {
                folder = folder.nodes[it] as Node.FolderNode
                true
            } else false
        }.forEach {
            val newFolder = Node.FolderNode(it)
            folder.nodes[it] = newFolder
            folder = newFolder
        }
        return folder
    }

    override suspend fun getBytes(path: String, maxDownloadSize: Long): ByteArray {
        val file = getFileNode(path)
        if (file.data.size > maxDownloadSize) throw StorageException(
            Unknown, "File bytes exceeded maxDownloadSize: $maxDownloadSize"
        )
        return file.data
    }

    override suspend fun getDownloadUrl(path: String): String =
        "https://firebasestorage.googleapis.com/v0/b/${config.bucket}/o/${
            path.replace("/", "%2F")
        }"

    override suspend fun getFile(
        path: String,
        filePath: String,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)?
    ) = TODO("Not yet implemented")

    override suspend fun getMetadata(path: String): StorageMetadata = getFileNode(path).metadata

    override suspend fun list(path: String, maxResults: Int?, pageToken: String?): ListResult {
        TODO("Not yet implemented")
    }

    override suspend fun putBytes(
        path: String,
        bytes: ByteArray,
        metadata: FileMetadata?,
        onProgress: ((bytesUploaded: Long, totalBytes: Long) -> Unit)?
    ): StorageMetadata {
        val fileName = path.takeLastWhile { it != '/' }
        val folder = getOrCreateFolderNode(path.dropLast(fileName.length + 1))
        if (folder.nodes.contains(fileName)) {
            val type = if (folder.nodes[fileName] is Node.FileNode) "File" else "Folder"
            throw StorageException(
                Unknown,
                "$type at path $path already exists"
            )
        }
        val time = Clock.System.now().toEpochMilliseconds()
        val newMetadata = StorageMetadata(
            bucket = config.bucket,
            cacheControl = metadata?.cacheControl,
            contentDisposition = metadata?.contentDisposition,
            contentEncoding = metadata?.contentEncoding,
            contentLanguage = metadata?.contentLanguage,
            contentType = metadata?.contentType,
            creationTime = time,
            generation = 0,
            md5Hash = null,
            metadataGeneration = 0,
            name = fileName,
            path = path,
            size = bytes.size.toLong(),
            updatedTime = time,
            customMetadata = metadata?.customMetadata ?: mapOf()
        )
        if (onProgress != null) {
            bytes.forEachIndexed { i, _ ->
                onProgress(i.toLong(), bytes.size.toLong())
            }
        }
        folder.nodes[fileName] = Node.FileNode(fileName, bytes, newMetadata)
        onProgress?.invoke(bytes.size.toLong(), bytes.size.toLong())
        return newMetadata
    }

    override suspend fun putFile(
        path: String,
        filePath: String,
        metadata: FileMetadata?,
        onProgress: ((bytesUploaded: Long, totalBytes: Long) -> Unit)?
    ): StorageMetadata = TODO("Not yet implemented")

    override suspend fun updateMetadata(path: String, metadata: FileMetadata): StorageMetadata {
        val file = getFileNode(path)
        file.metadata = file.metadata.copy(
            cacheControl = metadata.cacheControl,
            contentDisposition = metadata.contentDisposition,
            contentEncoding = metadata.contentEncoding,
            contentLanguage = metadata.contentLanguage,
            contentType = metadata.contentType,
            customMetadata = metadata.customMetadata
        )
        return file.metadata
    }

    override val config: FirebaseStorage.Config
        get() = object : FirebaseStorage.Config {
            override var maxDownloadRetryTime: Long
                get() = TODO("Not yet implemented")
                set(value) {
                    TODO("Not yet implemented")
                }
            override var maxOperationRetryTime: Long
                get() = TODO("Not yet implemented")
                set(value) {
                    TODO("Not yet implemented")
                }
            override var maxUploadRetryTime: Long
                get() = TODO("Not yet implemented")
                set(value) {
                    TODO("Not yet implemented")
                }
            override val bucket: String
                get() = "my-firebase-project.appspot.com"

            override fun useEmulator(host: String, port: Int) {
            }

        }
    companion object {
        val instance: LocalStorage by lazy { LocalStorage() }
    }
}