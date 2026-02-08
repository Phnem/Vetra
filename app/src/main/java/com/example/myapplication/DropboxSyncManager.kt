package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.HttpRequestor
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Date
import java.util.concurrent.TimeUnit

// Алиасы
import okhttp3.Response as OkHttpResponse
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody as OkHttpRequestBody
import com.dropbox.core.http.HttpRequestor.Response as DbxResponse

sealed class SyncResult {
    object Success : SyncResult()
    object Conflict : SyncResult()
    data class Error(val msg: String) : SyncResult()
}

enum class SyncState { IDLE, SYNCING, DONE, CONFLICT, ERROR, AUTH_REQUIRED }

object DropboxSyncManager {

    private const val TAG = "DropboxSync"
    private const val APP_KEY = "0isabzy5qvb6owr" // Твой ключ
    private const val PREFS_NAME = "dropbox_prefs"
    private const val REFRESH_TOKEN_KEY = "refresh_token"
    private const val LAST_SYNC_KEY = "last_sync_time"

    private val JSON_FILES = listOf("settings.json", "list.json", "ignored.json")
    private const val COLLECTION_DIR = "collection"

    private var client: DbxClientV2? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var rootDir: File

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var debounceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val documentsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
        rootDir = File(documentsDir, "MyAnimeList")
        if (!rootDir.exists()) rootDir.mkdirs()

        val refreshToken = prefs.getString(REFRESH_TOKEN_KEY, null)
        if (refreshToken != null) {
            setupClient(refreshToken)
            Log.d(TAG, "Client initialized with Refresh Token")
        } else {
            _syncState.value = SyncState.AUTH_REQUIRED
        }
    }

    private fun buildConfig(): DbxRequestConfig {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        // Используем наш ГИБРИДНЫЙ загрузчик (память + диск)
        val requestor = DropboxOkHttpRequestor(okHttpClient)

        return DbxRequestConfig.newBuilder("MAList/1.0")
            .withHttpRequestor(requestor)
            .build()
    }

    fun startOAuth(context: Context) {
        Auth.startOAuth2PKCE(context, APP_KEY, buildConfig())
    }

    fun onOAuthResult() {
        val credential = Auth.getDbxCredential()
        if (credential != null) {
            val refreshToken = credential.refreshToken
            if (refreshToken != null) {
                Log.d(TAG, "OAuth success: Refresh Token received")
                prefs.edit().putString(REFRESH_TOKEN_KEY, refreshToken).apply()
                val accessToken = credential.accessToken ?: ""
                setupClient(refreshToken, initialAccessToken = accessToken)
                scope.launch { syncNow() }
            } else {
                Log.e(TAG, "OAuth Error: No Refresh Token found")
            }
        }
    }

    fun hasToken(): Boolean = prefs.contains(REFRESH_TOKEN_KEY)

    fun scheduleAutoSync() {
        if (client == null) return
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(3000)
            syncNow()
        }
    }

    private fun setupClient(refreshToken: String, initialAccessToken: String = "") {
        val config = buildConfig()
        val credential = DbxCredential(initialAccessToken, -1L, refreshToken, APP_KEY)
        client = DbxClientV2(config, credential)
    }

    suspend fun syncNow(): SyncResult = withContext(Dispatchers.IO) {
        if (client == null) {
            _syncState.value = SyncState.AUTH_REQUIRED
            return@withContext SyncResult.Error("No client")
        }

        // ШАГ 0: Чистим мусор перед стартом, чтобы не качать сломанные файлы
        cleanupGarbage()

        Log.d(TAG, "--- SYNC STARTED ---")
        _syncState.value = SyncState.SYNCING
        try {
            val account = client!!.users().currentAccount
            Log.d(TAG, "Connected as: ${account.name.displayName}")

            val remoteFiles = getAllRemoteFiles()
            val remoteEmpty = remoteFiles.isEmpty()
            val localEmpty = isLocalEmpty()

            Log.d(TAG, "Remote files: ${remoteFiles.size}, Local empty: $localEmpty")

            when {
                !localEmpty && remoteEmpty -> {
                    Log.d(TAG, "Scenario: First Upload")
                    uploadAll()
                    SyncResult.Success
                }
                localEmpty && !remoteEmpty -> {
                    Log.d(TAG, "Scenario: Restore")
                    downloadAll()
                    SyncResult.Success
                }
                else -> {
                    Log.d(TAG, "Scenario: Two-Way Sync")
                    performTwoWaySync(remoteFiles)
                }
            }
        } catch (e: Exception) {
            if (e is com.dropbox.core.InvalidAccessTokenException ||
                e.message?.contains("expired_access_token") == true) {
                Log.e(TAG, "Refresh Token Expired or Revoked", e)
                logout()
                SyncResult.Error("Auth required")
            } else {
                Log.e(TAG, "Sync Fatal Error", e)
                _syncState.value = SyncState.ERROR
                SyncResult.Error(e.message ?: "Unknown error")
            }
        } finally {
            if (_syncState.value == SyncState.SYNCING) {
                _syncState.value = SyncState.DONE
            }
            Log.d(TAG, "--- SYNC FINISHED ---")
            prefs.edit().putLong(LAST_SYNC_KEY, System.currentTimeMillis()).apply()
        }
    }

    // Удаляет временные файлы и файлы 0 байт, чтобы разорвать цикл ошибок
    private fun cleanupGarbage() {
        try {
            val imgDir = File(rootDir, COLLECTION_DIR)
            if (imgDir.exists()) {
                imgDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".tmp") || file.length() == 0L) {
                        // Log.d(TAG, "Removing garbage file: ${file.name}")
                        file.delete()
                    }
                }
            }
            JSON_FILES.forEach { name ->
                val f = File(rootDir, name)
                if (f.exists() && f.length() == 0L) f.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup warning: ${e.message}")
        }
    }

    fun logout() {
        prefs.edit().remove(REFRESH_TOKEN_KEY).apply()
        client = null
        _syncState.value = SyncState.AUTH_REQUIRED
    }

    private fun getAllRemoteFiles(): Map<String, com.dropbox.core.v2.files.FileMetadata> {
        val result = mutableMapOf<String, com.dropbox.core.v2.files.FileMetadata>()
        try {
            var listing = client!!.files().listFolderBuilder("").withRecursive(true).start()
            while (true) {
                listing.entries.forEach { metadata ->
                    if (metadata is com.dropbox.core.v2.files.FileMetadata) {
                        result[metadata.name] = metadata
                    }
                }
                if (!listing.hasMore) break
                listing = client!!.files().listFolderContinue(listing.cursor)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting remote files: ${e.message}")
        }
        return result
    }

    // --- БЭКАП (Вызывается отдельно, чтобы не падать из-за памяти) ---
    private suspend fun createAndUploadBackupZip() = withContext(Dispatchers.IO) {
        val zipFile = File(rootDir, "backup.zip")
        try {
            // Log.d(TAG, "Creating Auto-Backup ZIP...")
            val filesToZip = mutableListOf<File>()
            JSON_FILES.forEach { name ->
                val f = File(rootDir, name)
                if (f.exists()) filesToZip.add(f)
            }
            val imgDir = File(rootDir, COLLECTION_DIR)
            imgDir.listFiles()?.forEach { filesToZip.add(it) }

            if (filesToZip.isEmpty()) return@withContext

            // ZipUtils.zipFiles теперь должен использовать стриминг (маленький буфер)
            ZipUtils.zipFiles(filesToZip, zipFile)

            Log.d(TAG, "Uploading backup.zip (${zipFile.length() / 1024} KB)...")

            // Здесь теперь работает Hybrid Uploader, который переключится на файл
            // и не уронит приложение OOM
            FileInputStream(zipFile).use { inputStream ->
                client!!.files().uploadBuilder("/backup.zip")
                    .withMode(WriteMode.OVERWRITE)
                    .withMute(true)
                    .uploadAndFinish(inputStream)
            }

            Log.d(TAG, "Auto-Backup completed successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create/upload auto-backup: ${e.message}")
        } finally {
            if (zipFile.exists()) zipFile.delete()
        }
    }

    private suspend fun performTwoWaySync(remoteFiles: Map<String, com.dropbox.core.v2.files.FileMetadata>): SyncResult {
        // 1. JSON
        JSON_FILES.forEach { fileName ->
            val localFile = File(rootDir, fileName)
            val remoteMeta = remoteFiles.values.find { it.name.equals(fileName, ignoreCase = true) }
            try {
                if (remoteMeta != null) {
                    val needDownload = !localFile.exists() ||
                            localFile.length() == 0L ||
                            remoteMeta.serverModified.time > localFile.lastModified() + 5000

                    if (needDownload) {
                        downloadFile(remoteMeta.pathLower, localFile)
                    } else if (localFile.exists() && localFile.lastModified() > remoteMeta.serverModified.time + 5000) {
                        uploadFile(localFile, "/$fileName")
                    }
                } else if (localFile.exists() && localFile.length() > 0L) {
                    uploadFile(localFile, "/$fileName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing $fileName", e)
            }
        }

        // 2. Картинки
        syncCollectionFolder(remoteFiles)

        // 3. Бэкап в фоне
        scope.launch { createAndUploadBackupZip() }

        return SyncResult.Success
    }

    private suspend fun syncCollectionFolder(remoteFiles: Map<String, com.dropbox.core.v2.files.FileMetadata>) {
        val imgDir = File(rootDir, COLLECTION_DIR)
        if (!imgDir.exists()) imgDir.mkdirs()

        val localImages = imgDir.listFiles() ?: emptyArray()

        // Сортировка для красоты логов и порядка
        val sortedImages = localImages.sortedWith(Comparator { f1, f2 ->
            val n1 = f1.nameWithoutExtension.toIntOrNull()
            val n2 = f2.nameWithoutExtension.toIntOrNull()
            when {
                n1 != null && n2 != null -> n1.compareTo(n2)
                n1 != null -> -1
                n2 != null -> 1
                else -> f1.name.compareTo(f2.name)
            }
        })

        for (file in sortedImages) {
            if (!remoteFiles.containsKey(file.name)) {
                try {
                    Log.d(TAG, ">>> Uploading: ${file.name}")
                    FileInputStream(file).use { inputStream ->
                        client!!.files().uploadBuilder("/$COLLECTION_DIR/${file.name}")
                            .withMode(WriteMode.OVERWRITE)
                            .withClientModified(Date(file.lastModified()))
                            .withMute(true)
                            .uploadAndFinish(inputStream)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Upload failed: ${file.name}", e)
                }
            }
        }

        remoteFiles.values.forEach { metadata ->
            if (metadata.pathDisplay.contains("/$COLLECTION_DIR/")) {
                val localFile = File(imgDir, metadata.name)
                // Качаем только если нет или битый
                if (!localFile.exists() || localFile.length() == 0L) {
                    try {
                        downloadFile(metadata.pathLower, localFile)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to download ${metadata.name}", e)
                    }
                }
            }
        }
    }

    private fun uploadAll() {
        // Сначала пробуем создать бэкап в фоне
        scope.launch { createAndUploadBackupZip() }

        JSON_FILES.forEach { fileName ->
            val file = File(rootDir, fileName)
            if (file.exists()) try { uploadFile(file, "/$fileName") } catch(e:Exception){}
        }
        val imgDir = File(rootDir, COLLECTION_DIR)
        imgDir.listFiles()?.forEach { file ->
            try {
                uploadFile(file, "/$COLLECTION_DIR/${file.name}")
            } catch (e: Exception) {
                Log.e(TAG, "First upload failed for ${file.name}", e)
            }
        }
    }

    // ... (внутри DropboxSyncManager)

    private suspend fun downloadAll() {
        Log.d(TAG, "Starting Restore process...")

        var restoredFromZip = false
        val localZip = File(rootDir, "backup.zip")

        // 1. Пытаемся скачать и распаковать ZIP
        try {
            // Проверяем наличие файла в облаке (бросит исключение, если нет)
            val metadata = client!!.files().getMetadata("/backup.zip")

            Log.d(TAG, "Found backup.zip (${metadata.name})! Downloading...")

            // Скачиваем
            FileOutputStream(localZip).use { output ->
                client!!.files().download("/backup.zip").download(output)
            }

            if (localZip.exists() && localZip.length() > 0) {
                Log.d(TAG, "Unzipping directly to rootDir...")

                // ВАЖНО: Распаковываем прямо в корень.
                // ZipUtils сам создаст папку collection, так как пути в архиве сохранены как "collection/img.jpg"
                ZipUtils.unzip(localZip, rootDir)

                Log.d(TAG, "Restore from ZIP finished successfully!")
                restoredFromZip = true
            }

        } catch (e: Exception) {
            Log.w(TAG, "Zip restore skipped or failed: ${e.message}")
            // Если ошибка (например, zip не найден или битый) - идем к пофайловому методу
        } finally {
            // Удаляем скачанный архив, чтобы не занимал место
            if (localZip.exists()) localZip.delete()
        }

        // 2. Если ZIP сработал — завершаем (можно дернуть sync для проверки свежести)
        if (restoredFromZip) {
            // На всякий случай проверяем, не изменилось ли что-то в облаке, пока мы качали архив
            // Но для сценария "восстановление" это обычно не требуется
            return
        }

        // 3. Fallback (Пофайлово - старый метод, если ZIP нет)
        Log.d(TAG, "Falling back to file-by-file download...")

        val allRemoteFiles = mutableListOf<com.dropbox.core.v2.files.FileMetadata>()
        try {
            var result = client!!.files().listFolderBuilder("").withRecursive(true).start()
            while (true) {
                result.entries.forEach { if (it is com.dropbox.core.v2.files.FileMetadata) allRemoteFiles.add(it) }
                if (!result.hasMore) break
                result = client!!.files().listFolderContinue(result.cursor)
            }
        } catch (e: Exception) { return }

        allRemoteFiles.filter { it.name.endsWith(".json") }.forEach {
            try { downloadFile(it.pathLower, File(rootDir, it.name)) } catch(e:Exception){}
        }
        allRemoteFiles.filter { !it.name.endsWith(".json") && !it.name.endsWith(".zip") }.forEach {
            try { downloadFile(it.pathLower, File(rootDir, COLLECTION_DIR + "/" + it.name)) } catch(e:Exception){}
        }
    }

    private fun uploadFile(localFile: File, remotePath: String) {
        FileInputStream(localFile).use { inputStream ->
            client!!.files().uploadBuilder(remotePath)
                .withMode(WriteMode.OVERWRITE)
                .withClientModified(Date(localFile.lastModified()))
                .uploadAndFinish(inputStream)
        }
    }

    private suspend fun downloadFile(remotePath: String, localFile: File) = withContext(Dispatchers.IO) {
        localFile.parentFile?.mkdirs()

        // Убираем битые файлы
        if (localFile.exists() && localFile.length() == 0L) localFile.delete()

        var attempt = 0
        val maxAttempts = 3
        var success = false

        while (attempt < maxAttempts && !success) {
            try {
                attempt++

                if (remotePath.endsWith(".json", ignoreCase = true) ||
                    remotePath.endsWith(".txt", ignoreCase = true)) {

                    // Мелкие конфиги - в память
                    val downloader = client!!.files().download(remotePath)
                    val bytes = downloader.inputStream.use { it.readBytes() }
                    downloader.close()

                    if (bytes.isNotEmpty()) {
                        if (localFile.exists()) localFile.delete()
                        localFile.writeBytes(bytes)
                        success = true
                    } else {
                        throw java.io.IOException("Content empty")
                    }

                } else {
                    // Картинки и ZIP - в файл
                    val tempFile = File(localFile.parent, "${localFile.name}.tmp")
                    if (tempFile.exists()) tempFile.delete()

                    FileOutputStream(tempFile).use { output ->
                        client!!.files().download(remotePath).download(output)
                    }

                    if (tempFile.length() > 0L) {
                        if (localFile.exists()) localFile.delete()
                        tempFile.renameTo(localFile)
                        success = true
                    } else {
                        tempFile.delete()
                        throw java.io.IOException("Temp file 0 bytes")
                    }
                }

                if (success) {
                    val metadata = client!!.files().getMetadata(remotePath) as? com.dropbox.core.v2.files.FileMetadata
                    metadata?.let { localFile.setLastModified(it.serverModified.time) }
                }

            } catch (e: Exception) {
                Log.w(TAG, "Attempt $attempt failed for $remotePath: ${e.message}")
                if (attempt == maxAttempts) {
                    Log.e(TAG, "Final failure for $remotePath")
                } else {
                    delay(1000)
                }
            }
        }
    }

    private fun isLocalEmpty(): Boolean {
        val jsonExists = JSON_FILES.any { File(rootDir, it).exists() }
        val imgDir = File(rootDir, COLLECTION_DIR)
        val imgsExist = imgDir.exists() && (imgDir.listFiles()?.isNotEmpty() == true)
        return !jsonExists && !imgsExist
    }
}

// --- ГИБРИДНЫЙ ЗАГРУЗЧИК (Memory < 5MB < File) ---
class DropboxOkHttpRequestor(private val client: OkHttpClient) : HttpRequestor() {

    override fun doGet(url: String, headers: Iterable<HttpRequestor.Header>): DbxResponse {
        val builder = OkHttpRequest.Builder().url(url).get()
        headers.forEach { builder.addHeader(it.key, it.value) }
        val response = client.newCall(builder.build()).execute()
        return toDropboxResponse(response)
    }

    override fun startPost(url: String, headers: Iterable<HttpRequestor.Header>): HttpRequestor.Uploader {
        return object : HttpRequestor.Uploader() {
            private val MEMORY_LIMIT = 5 * 1024 * 1024 // 5 MB

            private var memoryBuffer: java.io.ByteArrayOutputStream? = java.io.ByteArrayOutputStream()
            private var tempFile: File? = null
            private var fileStream: FileOutputStream? = null

            private val requestBuilder = OkHttpRequest.Builder().url(url)

            init {
                headers.forEach { requestBuilder.addHeader(it.key, it.value) }
            }

            override fun getBody(): OutputStream {
                return object : OutputStream() {
                    override fun write(b: Int) { write(byteArrayOf(b.toByte()), 0, 1) }

                    override fun write(b: ByteArray, off: Int, len: Int) {
                        if (memoryBuffer != null) {
                            if (memoryBuffer!!.size() + len > MEMORY_LIMIT) {
                                switchToDisk()
                                fileStream!!.write(b, off, len)
                            } else {
                                memoryBuffer!!.write(b, off, len)
                            }
                        } else {
                            fileStream!!.write(b, off, len)
                        }
                    }

                    private fun switchToDisk() {
                        try {
                            tempFile = File.createTempFile("dbx_stream_", ".tmp")
                            fileStream = FileOutputStream(tempFile)
                            memoryBuffer!!.writeTo(fileStream)
                            memoryBuffer = null
                        } catch (e: Exception) {
                            Log.e("DropboxReq", "Switch to disk failed", e)
                        }
                    }

                    override fun close() { fileStream?.close() }
                }
            }

            override fun finish(): DbxResponse {
                val requestBody = if (tempFile != null) {
                    fileStream?.close()
                    tempFile!!.asRequestBody("application/octet-stream".toMediaType())
                } else {
                    OkHttpRequestBody.create(null, memoryBuffer!!.toByteArray())
                }

                requestBuilder.post(requestBody)

                try {
                    val response = client.newCall(requestBuilder.build()).execute()
                    return toDropboxResponse(response)
                } finally {
                    if (tempFile != null && tempFile!!.exists()) {
                        tempFile!!.delete()
                    }
                }
            }

            override fun close() {
                try { fileStream?.close() } catch (e: Exception) {}
                if (tempFile != null && tempFile!!.exists()) tempFile!!.delete()
            }

            override fun abort() { close() }
        }
    }

    override fun startPut(url: String, headers: Iterable<HttpRequestor.Header>): HttpRequestor.Uploader {
        return startPost(url, headers)
    }

    private fun toDropboxResponse(response: OkHttpResponse): DbxResponse {
        val headersMap = mutableMapOf<String, MutableList<String>>()
        for (i in 0 until response.headers.size) {
            val name = response.headers.name(i)
            val value = response.headers.value(i)
            headersMap.getOrPut(name) { mutableListOf() }.add(value)
        }

        val bodyStream = response.body?.byteStream()?.let { raw ->
            object : java.io.FilterInputStream(raw) {
                override fun close() {
                    super.close()
                    response.close()
                }
            }
        } ?: java.io.ByteArrayInputStream(ByteArray(0))

        return DbxResponse(
            response.code,
            bodyStream,
            headersMap as Map<String, List<String>>
        )
    }
}