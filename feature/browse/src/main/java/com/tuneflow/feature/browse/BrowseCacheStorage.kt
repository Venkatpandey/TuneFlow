package com.tuneflow.feature.browse

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val CACHE_FILE_NAME = "browse-cache.json"

interface BrowseCacheStorage {
    suspend fun read(): String?

    suspend fun write(value: String)

    suspend fun clear()
}

internal object NoOpBrowseCacheStorage : BrowseCacheStorage {
    override suspend fun read(): String? = null

    override suspend fun write(value: String) = Unit

    override suspend fun clear() = Unit
}

class FileBrowseCacheStorage(context: Context) : BrowseCacheStorage {
    private val file = AtomicFile(File(context.applicationContext.noBackupFilesDir, CACHE_FILE_NAME))

    override suspend fun read(): String? =
        withContext(Dispatchers.IO) {
            if (file.baseFile.exists()) {
                file.readFully().toString(Charsets.UTF_8)
            } else {
                null
            }
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun write(value: String) {
        withContext(Dispatchers.IO) {
            var output: FileOutputStream? = null
            try {
                output = file.startWrite()
                output.write(value.toByteArray(Charsets.UTF_8))
                file.finishWrite(output)
            } catch (error: Throwable) {
                file.failWrite(output)
                throw error
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            file.delete()
        }
    }
}
