import enchant.flare.FirebaseStorage
import enchant.flare.LocalStorage
import kotlin.test.*


class StorageTest : FlareTest() {
    lateinit var storage: FirebaseStorage

    @BeforeTest
    fun initializeStorage() {
        storage = if (useLocal) LocalStorage() else FirebaseStorage.instance
    }

    private val sampleBytes = byteArrayOf(12, 127, -128, -65, 23, -98, 123, -87, 98, -17)

    @Test
    fun putGetFileBytes() = runTest {
        var updates = 0
        val onProgress: (bytesUploaded: Long, totalBytes: Long) -> Unit =
            { bytesUploaded, totalBytes ->
                assertEquals(updates.toLong(), bytesUploaded, "Check bytesUploaded is accurate")
                assertEquals(sampleBytes.size.toLong(), totalBytes, "Check totalBytes is accurate")
                updates++
            }
        storage.putBytes(
            "$testId/folder/folder/myFile.txt", sampleBytes, onProgress = if (useLocal) onProgress else null
        )
        if (useLocal) assertEquals(
            sampleBytes.size + 1, updates, "Ensure correct amount of updates happened"
        )

        val bytes = storage.getBytes("$testId/folder/folder/myFile.txt", 1000)
        assertContentEquals(sampleBytes, bytes)
    }

    @Test
    fun deleteFile() = runTest {
        storage.putBytes("$testId/folder/folder/myDeleteFile", sampleBytes)
        storage.deleteFile("$testId/folder/folder/myDeleteFile")
        assertFails {
            storage.getBytes("$testId/folder/folder/myDeleteFile", 10)
        }
    }
}