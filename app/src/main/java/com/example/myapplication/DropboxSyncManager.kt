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
import okhttp3.OkHttpClient
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
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val requestor = DropboxOkHttpRequestor(okHttpClient)

        return DbxRequestConfig.newBuilder("MAList/1.0")
            .withHttpRequestor(requestor)
            .build()
    }

    fun startOAuth(context: Context) {
        // Передаем конфиг с OkHttp, чтобы избежать SSL ошибок при входе
        Auth.startOAuth2PKCE(context, APP_KEY, buildConfig())
    }

    fun onOAuthResult() {
        val credential = Auth.getDbxCredential()

        if (credential != null) {
            val refreshToken = credential.refreshToken
            if (refreshToken != null) {
                Log.d(TAG, "OAuth success: Refresh Token received")
                prefs.edit().putString(REFRESH_TOKEN_KEY, refreshToken).apply()

                // Передаем access token сразу, если он есть, чтобы не делать лишний рефреш
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

        // --- ИСПРАВЛЕНИЕ КРЭША ---
        // Нельзя передавать null в accessToken. Передаем пустую строку "" или реальный токен.
        // SDK поймет, что токен пустой/протухший, и использует refreshToken для обновления.
        val credential = DbxCredential(initialAccessToken, -1L, refreshToken, APP_KEY)

        client = DbxClientV2(config, credential)
    }

    suspend fun syncNow(): SyncResult = withContext(Dispatchers.IO) {
        if (client == null) {
            _syncState.value = SyncState.AUTH_REQUIRED
            return@withContext SyncResult.Error("No client")
        }

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
            // Если рефреш токен отозван или протух
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

    private fun performTwoWaySync(remoteFiles: Map<String, com.dropbox.core.v2.files.FileMetadata>): SyncResult {
        JSON_FILES.forEach { fileName ->
            val localFile = File(rootDir, fileName)
            val remoteMeta = remoteFiles[fileName]
            try {
                if (!localFile.exists() && remoteMeta != null) {
                    downloadFile(remoteMeta.pathLower, localFile)
                } else if (localFile.exists() && remoteMeta == null) {
                    uploadFile(localFile, "/$fileName")
                } else if (localFile.exists() && remoteMeta != null) {
                    if (remoteMeta.serverModified.time > localFile.lastModified() + 5000) {
                        downloadFile(remoteMeta.pathLower, localFile)
                    } else if (localFile.lastModified() > remoteMeta.serverModified.time + 5000) {
                        uploadFile(localFile, "/$fileName")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing $fileName", e)
            }
        }
        syncCollectionFolder(remoteFiles)
        return SyncResult.Success
    }

    private fun syncCollectionFolder(remoteFiles: Map<String, com.dropbox.core.v2.files.FileMetadata>) {
        val imgDir = File(rootDir, COLLECTION_DIR)
        if (!imgDir.exists()) imgDir.mkdirs()

        val localImages = imgDir.listFiles() ?: emptyArray()
        Log.d(TAG, "Syncing collection. Local: ${localImages.size} items")

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

        var uploadCount = 0
        for (file in sortedImages) {
            if (!remoteFiles.containsKey(file.name)) {
                try {
                    Log.d(TAG, ">>> Uploading: ${file.name} (${file.length() / 1024} KB)")
                    FileInputStream(file).use { inputStream ->
                        client!!.files().uploadBuilder("/$COLLECTION_DIR/${file.name}")
                            .withMode(WriteMode.OVERWRITE)
                            .withClientModified(Date(file.lastModified()))
                            .withMute(true)
                            .uploadAndFinish(inputStream)
                    }
                    Log.d(TAG, "✅ Success: ${file.name}")
                    uploadCount++
                    Thread.sleep(500)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ FAILURE on ${file.name}: ${e.message}")
                }
            }
        }
        Log.d(TAG, "Upload session finished. Uploaded: $uploadCount files")

        remoteFiles.values.forEach { metadata ->
            if (metadata.pathDisplay.contains("/$COLLECTION_DIR/")) {
                val localFile = File(imgDir, metadata.name)
                if (!localFile.exists()) {
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
        JSON_FILES.forEach { fileName ->
            val file = File(rootDir, fileName)
            if (file.exists()) try { uploadFile(file, "/$fileName") } catch(e:Exception){}
        }
        val imgDir = File(rootDir, COLLECTION_DIR)
        val sortedImages = imgDir.listFiles()?.sortedBy {
            it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE
        } ?: emptyList()

        sortedImages.forEach { file ->
            try {
                uploadFile(file, "/$COLLECTION_DIR/${file.name}")
                Thread.sleep(100)
            } catch (e: Exception) {
                Log.e(TAG, "First upload failed for ${file.name}", e)
            }
        }
    }

    private fun downloadAll() {
        try {
            var result = client!!.files().listFolderBuilder("").withRecursive(true).start()
            while (true) {
                result.entries.forEach { metadata ->
                    if (metadata is com.dropbox.core.v2.files.FileMetadata) {
                        val localPath = File(rootDir, metadata.pathDisplay.removePrefix("/"))
                        try { downloadFile(metadata.pathLower, localPath) } catch(e:Exception){}
                    }
                }
                if (!result.hasMore) break
                result = client!!.files().listFolderContinue(result.cursor)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    private fun downloadFile(remotePath: String, localFile: File) {
        localFile.parentFile?.mkdirs()
        FileOutputStream(localFile).use { outputStream ->
            client!!.files().download(remotePath).download(outputStream)
        }
        val metadata = client!!.files().getMetadata(remotePath) as? com.dropbox.core.v2.files.FileMetadata
        metadata?.let { localFile.setLastModified(it.serverModified.time) }
    }

    private fun isLocalEmpty(): Boolean {
        val jsonExists = JSON_FILES.any { File(rootDir, it).exists() }
        val imgDir = File(rootDir, COLLECTION_DIR)
        val imgsExist = imgDir.exists() && (imgDir.listFiles()?.isNotEmpty() == true)
        return !jsonExists && !imgsExist
    }
}

// --- OKHTTP WRAPPER ---
class DropboxOkHttpRequestor(private val client: OkHttpClient) : HttpRequestor() {

    override fun doGet(url: String, headers: Iterable<HttpRequestor.Header>): DbxResponse {
        val builder = OkHttpRequest.Builder().url(url).get()
        headers.forEach { builder.addHeader(it.key, it.value) }
        val response = client.newCall(builder.build()).execute()
        return toDropboxResponse(response)
    }

    override fun startPut(url: String, headers: Iterable<HttpRequestor.Header>): HttpRequestor.Uploader {
        return startPost(url, headers)
    }

    override fun startPost(url: String, headers: Iterable<HttpRequestor.Header>): HttpRequestor.Uploader {
        return object : HttpRequestor.Uploader() {
            private var currentResponse: OkHttpResponse? = null
            private val buffer = java.io.ByteArrayOutputStream()
            private val requestBuilder = OkHttpRequest.Builder().url(url)

            init {
                headers.forEach { requestBuilder.addHeader(it.key, it.value) }
            }

            override fun getBody(): OutputStream {
                return buffer
            }

            override fun finish(): DbxResponse {
                val bodyBytes = buffer.toByteArray()
                val requestBody = OkHttpRequestBody.create(null, bodyBytes)
                requestBuilder.post(requestBody)
                val response = client.newCall(requestBuilder.build()).execute()
                currentResponse = response
                return toDropboxResponse(response)
            }

            override fun close() {
                try { buffer.close() } catch(e: Exception) {}
                currentResponse?.close()
            }

            override fun abort() {
                close()
            }
        }
    }

    private fun toDropboxResponse(response: OkHttpResponse): DbxResponse {
        val headersMap = mutableMapOf<String, MutableList<String>>()
        for (i in 0 until response.headers.size) {
            val name = response.headers.name(i)
            val value = response.headers.value(i)
            headersMap.getOrPut(name) { mutableListOf() }.add(value)
        }
        val bodyBytes = response.body?.bytes() ?: ByteArray(0)
        val bodyStream = java.io.ByteArrayInputStream(bodyBytes)
        response.close()
        return DbxResponse(
            response.code,
            bodyStream,
            headersMap as Map<String, List<String>>
        )
    }
}