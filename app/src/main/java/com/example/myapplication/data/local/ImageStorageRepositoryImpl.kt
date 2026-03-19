package com.example.myapplication.data.local

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.myapplication.data.repository.ImageStorageRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val ROOT = "Vetro"
private const val IMG_DIR = "collection"

class ImageStorageRepositoryImpl(
    private val context: Context,
    private val httpClient: HttpClient
) : ImageStorageRepository {

    private fun getImgDir(): File {
        val root = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            ROOT
        )
        if (!root.exists()) root.mkdirs()
        val imgDir = File(root, IMG_DIR)
        if (!imgDir.exists()) imgDir.mkdirs()
        return imgDir
    }

    override suspend fun saveImage(uri: String, animeId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val name = "img_${animeId}_${System.currentTimeMillis()}.jpg"
            val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
                ?: error("Failed to open URI: $uri")
            inputStream.use { input ->
                FileOutputStream(File(getImgDir(), name)).use { output ->
                    input.copyTo(output)
                }
            }
            name
        }
    }

    override fun getImageFilePath(fileName: String): String? {
        val file = File(getImgDir(), fileName)
        return file.takeIf { it.exists() }?.absolutePath
    }

    override fun deleteImage(fileName: String): Boolean {
        val file = File(getImgDir(), fileName)
        return file.exists() && file.delete()
    }

    override suspend fun saveImageFromUrl(url: String, animeId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!url.startsWith("http")) error("Invalid URL: $url")
            val name = "img_${animeId}_${System.currentTimeMillis()}.jpg"
            val bytes = httpClient.get(url).bodyAsBytes()
            File(getImgDir(), name).writeBytes(bytes)
            name
        }
    }
}
