package com.example.myapplication.data.local

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "LegacyMigration"

class LegacyMigrationRepositoryImpl(
    private val context: Context
) : com.example.myapplication.data.repository.LegacyMigrationRepository {

    companion object {
        private const val OLD_ROOT_FOLDER = "MyAnimeList"
        private const val NEW_ROOT_FOLDER = "Vetro"
    }

    private fun getBaseDir(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    }

    override suspend fun needsMigration(): Boolean = withContext(Dispatchers.IO) {
        val oldFolder = File(getBaseDir(), OLD_ROOT_FOLDER)
        oldFolder.exists() && oldFolder.isDirectory
    }

    override suspend fun migrateLegacyDataIfNeeded(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val baseDir = getBaseDir()
            val oldFolder = File(baseDir, OLD_ROOT_FOLDER)
            val newFolder = File(baseDir, NEW_ROOT_FOLDER)

            if (!oldFolder.exists() || !oldFolder.isDirectory) return@runCatching
            if (!newFolder.exists()) newFolder.mkdirs()

            // УМНЫЙ ПЕРЕНОС: Берем ВСЁ (картинки, list.json, updates.json, ignored.json)
            oldFolder.listFiles()?.forEach { fileOrDir ->
                val targetFile = File(newFolder, fileOrDir.name)
                if (targetFile.exists()) targetFile.deleteRecursively()

                if (fileOrDir.isFile && fileOrDir.extension == "json") {
                    val fixedJsonContent = fileOrDir.readText().replace(
                        "/$OLD_ROOT_FOLDER/",
                        "/$NEW_ROOT_FOLDER/"
                    )
                    targetFile.writeText(fixedJsonContent)
                    fileOrDir.delete()
                } else {
                    val isMoved = fileOrDir.renameTo(targetFile)
                    if (!isMoved) {
                        fileOrDir.copyRecursively(targetFile, overwrite = true)
                        fileOrDir.deleteRecursively()
                    }
                }
            }

            Log.d(TAG, "All legacy files successfully moved to $NEW_ROOT_FOLDER")
            oldFolder.deleteRecursively()
        }.onFailure { error ->
            Log.e(TAG, "Legacy migration failed", error)
        }
    }
}
