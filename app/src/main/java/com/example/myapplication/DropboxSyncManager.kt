package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.util.Log
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.HttpRequestor
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.WriteMode
import com.example.myapplication.data.local.AnimeLocalDataSource
import com.example.myapplication.data.local.SQLDelightDatabaseFactory
import com.example.myapplication.utils.DropboxContentHasher
import com.example.myapplication.worker.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Date
import java.util.concurrent.TimeUnit
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response as OkHttpResponse
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody as OkHttpRequestBody
import com.dropbox.core.http.HttpRequestor.Response as DbxResponse

sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val msg: String) : SyncResult()
}
enum class SyncMode { AUTO, MANUAL }
enum class NetworkMode { WIFI_AND_MOBILE, WIFI_ONLY, MOBILE_ONLY }
enum class SyncState { IDLE, SYNCING, DONE, CONFLICT, ERROR, AUTH_REQUIRED }

private const val TAG = "DropboxSync"
private const val APP_KEY = "0isabzy5qvb6owr"
private const val PREFS_NAME = "dropbox_prefs"
private const val REFRESH_TOKEN_KEY = "refresh_token"
private const val LAST_SYNC_KEY = "last_sync_time"
private const val PREF_NETWORK_MODE = "pref_network_mode"
private const val PREF_SYNC_MODE = "pref_sync_mode"
private const val DB_NAME = "anime.db"
private const val COLLECTION_DIR = "collection"

class DropboxSyncManager(
    private val context: Context,
    private val databaseFactory: SQLDelightDatabaseFactory,
    private val animeLocalDataSource: AnimeLocalDataSource
) {
    private val _syncMode = MutableStateFlow(SyncMode.AUTO)
    private val _networkMode = MutableStateFlow(NetworkMode.WIFI_AND_MOBILE)
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    private val _hasTokenFlow = MutableStateFlow(false)

    var client: DbxClientV2? = null
    lateinit var prefs: SharedPreferences
    lateinit var rootDir: File
    lateinit var appContext: Context
    private var debounceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        companionInstance = this
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        rootDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Vetro")
        if (!rootDir.exists()) rootDir.mkdirs()
        _networkMode.value = getNetworkMode()
        val savedSyncMode = prefs.getString(PREF_SYNC_MODE, SyncMode.AUTO.name)
        _syncMode.value = try { SyncMode.valueOf(savedSyncMode ?: SyncMode.AUTO.name) } catch (e: Exception) { SyncMode.AUTO }
        val refreshToken = prefs.getString(REFRESH_TOKEN_KEY, null)
        _hasTokenFlow.value = refreshToken != null
        if (refreshToken != null) {
            setupClient(refreshToken)
            Log.d(TAG, "Client initialized with Refresh Token")
        } else {
            _syncState.value = SyncState.AUTH_REQUIRED
        }
    }

    companion object {
        @Volatile
        private var companionInstance: DropboxSyncManager? = null
        private fun instance(): DropboxSyncManager = companionInstance ?: error("DropboxSyncManager not initialized")
        fun hasToken(): Boolean = companionInstance?.hasToken() ?: false
        fun logout() = companionInstance?.logout()
        fun setSyncMode(mode: SyncMode) = companionInstance?.setSyncMode(mode)
        fun setNetworkMode(mode: NetworkMode) = companionInstance?.setNetworkMode(mode)
        val syncMode: StateFlow<SyncMode> get() = instance()._syncMode.asStateFlow()
        val networkMode: StateFlow<NetworkMode> get() = instance()._networkMode.asStateFlow()
        val syncState: StateFlow<SyncState> get() = instance()._syncState.asStateFlow()
        val hasTokenFlow: StateFlow<Boolean> get() = instance()._hasTokenFlow.asStateFlow()
        suspend fun calculateStorageStats(): StorageStats = instance().calculateStorageStats()
        fun scheduleAutoSync() = companionInstance?.scheduleAutoSync()
        suspend fun syncNow(): SyncResult = instance().syncNow()
        fun onOAuthResult() = companionInstance?.onOAuthResult()
    }

    data class StorageStats(
        val fileCount: Int,
        val totalSize: Long,
        val isConsistent: Boolean,
        val lastSyncTime: String
    )

    fun hasToken(): Boolean = prefs.contains(REFRESH_TOKEN_KEY)

    fun getNetworkMode(): NetworkMode {
        val modeStr = prefs.getString(PREF_NETWORK_MODE, NetworkMode.WIFI_AND_MOBILE.name)
        return try { NetworkMode.valueOf(modeStr ?: NetworkMode.WIFI_AND_MOBILE.name) } catch (e: Exception) { NetworkMode.WIFI_AND_MOBILE }
    }

    fun setNetworkMode(mode: NetworkMode) {
        prefs.edit().putString(PREF_NETWORK_MODE, mode.name).apply()
        _networkMode.value = mode
    }

    fun setSyncMode(mode: SyncMode) {
        _syncMode.value = mode
        prefs.edit().putString(PREF_SYNC_MODE, mode.name).apply()
        if (mode == SyncMode.AUTO) scheduleAutoSync()
    }

    private fun isNetworkAllowed(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        val mode = getNetworkMode()
        return when (mode) {
            NetworkMode.WIFI_AND_MOBILE -> caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            NetworkMode.WIFI_ONLY -> caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            NetworkMode.MOBILE_ONLY -> caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }
    }

    suspend fun calculateStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DB_NAME)
        var size = if (dbFile.exists()) dbFile.length() else 0L
        var count = animeLocalDataSource.getAllAnimeList().size
        val imgDir = File(rootDir, COLLECTION_DIR)
        if (imgDir.exists()) {
            imgDir.listFiles()?.forEach { f ->
                count++
                size += f.length()
            }
        }
        val lastSyncTs = prefs.getLong(LAST_SYNC_KEY, 0L)
        val timeStr = if (lastSyncTs == 0L) "--:--" else android.text.format.DateFormat.format("HH:mm dd/MM", lastSyncTs).toString()
        StorageStats(count, size, _syncState.value == SyncState.IDLE || _syncState.value == SyncState.DONE, timeStr)
    }

    fun scheduleAutoSync() {
        if (client == null || !::appContext.isInitialized) return
        if (_syncMode.value == SyncMode.MANUAL) return
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(3000)
            if (_syncMode.value == SyncMode.MANUAL) return@launch
            if (isNetworkAllowed(appContext)) {
                SyncWorker.enqueue(appContext)
            }
        }
    }

    fun startOAuth(activity: android.app.Activity) {
        Auth.startOAuth2PKCE(activity, APP_KEY, buildConfig())
    }

    fun onOAuthResult() {
        val credential = Auth.getDbxCredential()
        if (credential != null) {
            val refreshToken = credential.refreshToken
            val accessToken = credential.accessToken
            Log.d(TAG, "OAuth result: hasRefreshToken=${!refreshToken.isNullOrBlank()}, hasAccessToken=${!accessToken.isNullOrBlank()}")
            if (!refreshToken.isNullOrBlank()) {
                prefs.edit().putString(REFRESH_TOKEN_KEY, refreshToken).apply()
                setupClient(refreshToken, accessToken ?: "")
                _hasTokenFlow.value = true
                Log.d(TAG, "Token saved, hasTokenFlow=true")
                scope.launch { syncNow(); SyncWorker.enqueue(appContext) }
            }
        } else {
            Log.d(TAG, "OAuth result: no credential returned")
        }
    }

    private fun buildConfig(): DbxRequestConfig {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        return DbxRequestConfig.newBuilder("Vetro/1.0")
            .withHttpRequestor(DropboxOkHttpRequestor(okHttp))
            .build()
    }

    private fun setupClient(refreshToken: String, initialAccessToken: String = "") {
        client = DbxClientV2(buildConfig(), DbxCredential(initialAccessToken, -1L, refreshToken, APP_KEY))
    }

    suspend fun syncNow(): SyncResult = withContext(Dispatchers.IO) {
        if (client == null) {
            _syncState.value = SyncState.AUTH_REQUIRED
            return@withContext SyncResult.Error("No client")
        }
        migrateRemoteFolderIfNeeded()
        _syncState.value = SyncState.SYNCING
        Log.d(TAG, "🚀 STARTING HASH-BASED SYNC")

        try {
            val remoteFiles = getAllRemoteFiles()

            syncCollection(remoteFiles)
            syncDatabase(remoteFiles)

            SyncResult.Success
        } catch (e: Exception) {
            if (e is com.dropbox.core.InvalidAccessTokenException || e.message?.contains("expired_access_token") == true) {
                logout()
                return@withContext SyncResult.Error("Auth required")
            }
            Log.e(TAG, "Sync error", e)
            _syncState.value = SyncState.ERROR
            SyncResult.Error(e.message ?: "Unknown")
        } finally {
            if (_syncState.value == SyncState.SYNCING) _syncState.value = SyncState.DONE
            prefs.edit().putLong(LAST_SYNC_KEY, System.currentTimeMillis()).apply()
            Log.d(TAG, "✅ SYNC COMPLETED")
        }
    }

    private fun migrateRemoteFolderIfNeeded() {
        try {
            try { client!!.files().getMetadata("/MAList") } catch (e: Exception) { return }
            try { client!!.files().getMetadata("/Vetro"); return } catch (e: Exception) { }
            client!!.files().moveV2("/MAList", "/Vetro")
        } catch (e: Exception) { Log.e(TAG, "Remote migration: ${e.message}") }
    }

    private suspend fun syncCollection(remoteFiles: Map<String, FileMetadata>) {
        val imgDir = File(rootDir, COLLECTION_DIR)
        if (!imgDir.exists()) imgDir.mkdirs()

        val localImages = imgDir.listFiles()?.filter { it.isFile && it.extension == "jpg" } ?: emptyList()

        localImages.forEach { localImg ->
            val remotePath = "/$COLLECTION_DIR/${localImg.name}"
            val pathLower = remotePath.lowercase()
            val remoteMeta = remoteFiles[pathLower]

            val localHash = DropboxContentHasher.hashFile(localImg)

            if (remoteMeta == null || remoteMeta.contentHash != localHash) {
                Log.d(TAG, "📤 Uploading image: ${localImg.name}")
                uploadFile(localImg, remotePath)
            }
        }

        remoteFiles.values
            .filter { it.pathLower?.startsWith("/$COLLECTION_DIR/") == true }
            .forEach { remoteMeta ->
                val localImg = File(imgDir, remoteMeta.name)
                if (!localImg.exists() || DropboxContentHasher.hashFile(localImg) != remoteMeta.contentHash) {
                    Log.d(TAG, "📥 Downloading image: ${remoteMeta.name}")
                    downloadFile(remoteMeta.pathLower!!, localImg, remoteMeta.clientModified)
                }
            }
    }

    private suspend fun syncDatabase(remoteFiles: Map<String, FileMetadata>) {
        databaseFactory.checkpoint()

        val localDb = context.getDatabasePath(DB_NAME)
        // Узнаем, сколько аниме в локальной БД (защита от свежесозданной пустой базы)
        val dbCount = try { animeLocalDataSource.getAnimeCount() } catch (e: Exception) { 0 }

        // Делаем копию и сохраняем оригинальное время (этот фикс мы обсуждали)
        val tempDb = File(context.cacheDir, "sync_temp_$DB_NAME")
        if (localDb.exists()) {
            localDb.copyTo(tempDb, overwrite = true)
            tempDb.setLastModified(localDb.lastModified())
        }

        val remotePath = "/$DB_NAME"
        val pathLower = remotePath.lowercase()
        val remoteMeta = remoteFiles[pathLower]
        val localHash = if (tempDb.exists()) DropboxContentHasher.hashFile(tempDb) else ""

        if (remoteMeta == null) {
            // В облаке пусто. Загружаем, только если у нас РЕАЛЬНО есть данные
            if (dbCount > 0 && tempDb.exists()) {
                Log.d(TAG, "📤 Cloud DB is empty. Uploading local DB...")
                uploadFile(tempDb, remotePath)
            } else {
                Log.d(TAG, "⏭ Cloud is empty, but local is also empty. Skipping DB upload.")
            }
        } else if (remoteMeta.contentHash != localHash) {
            val localTime = if (tempDb.exists()) tempDb.lastModified() else 0L
            val remoteTime = remoteMeta.clientModified.time

            // Если локально 0 тайтлов - это 100% переустановка. Игнорируем локальное время и принудительно качаем из облака.
            if (dbCount == 0 || remoteTime > localTime) {
                Log.d(TAG, "📥 Downloading DB from cloud (Reason: dbCount=$dbCount or Cloud is newer)...")
                val newDb = downloadFile(
                    remotePath = remoteMeta.pathLower!!,
                    targetFile = File(context.cacheDir, "downloaded_$DB_NAME"),
                    clientModified = remoteMeta.clientModified
                )

                newDb.copyTo(localDb, overwrite = true)
                File(localDb.parent, "$DB_NAME-wal").delete()
                File(localDb.parent, "$DB_NAME-shm").delete()

                Log.d(TAG, "🔄 DB Replaced successfully.")
            } else {
                Log.d(TAG, "📤 Local DB is newer and has data. Uploading...")
                uploadFile(tempDb, remotePath)
            }
        } else {
            Log.d(TAG, "⏭ DB is identical. Skipping.")
        }

        if (tempDb.exists()) tempDb.delete()
    }

    private fun uploadFile(file: File, remotePath: String) {
        file.inputStream().buffered().use { input ->
            client!!.files().uploadBuilder(remotePath)
                .withMode(WriteMode.OVERWRITE)
                .withMute(true)
                .withClientModified(Date(file.lastModified()))
                .uploadAndFinish(input)
        }
    }

    private fun downloadFile(remotePath: String, targetFile: File, clientModified: Date): File {
        targetFile.parentFile?.mkdirs()
        FileOutputStream(targetFile).buffered().use { output ->
            client!!.files().download(remotePath).download(output)
        }
        targetFile.setLastModified(clientModified.time)
        return targetFile
    }

    private fun getAllRemoteFiles(): Map<String, FileMetadata> {
        val result = mutableMapOf<String, FileMetadata>()
        try {
            var listing = client!!.files().listFolderBuilder("").withRecursive(true).start()
            while (true) {
                listing.entries.forEach { entry ->
                    if (entry is FileMetadata && entry.pathLower != null) {
                        result[entry.pathLower!!] = entry
                    }
                }
                if (!listing.hasMore) break
                listing = client!!.files().listFolderContinue(listing.cursor)
            }
        } catch (e: Exception) { Log.w(TAG, "getAllRemoteFiles error: ${e.message}") }
        return result
    }

    fun logout() {
        prefs.edit().remove(REFRESH_TOKEN_KEY).apply()
        client = null
        _syncState.value = SyncState.AUTH_REQUIRED
        _hasTokenFlow.value = false
    }
}

class DropboxOkHttpRequestor(private val client: OkHttpClient) : HttpRequestor() {
    override fun doGet(url: String, headers: Iterable<HttpRequestor.Header>): DbxResponse {
        val b = OkHttpRequest.Builder().url(url).get()
        headers.forEach { b.addHeader(it.key, it.value) }
        return toDropboxResponse(client.newCall(b.build()).execute())
    }
    override fun startPost(url: String, headers: Iterable<HttpRequestor.Header>): HttpRequestor.Uploader = object : HttpRequestor.Uploader() {
        private val MEMORY_LIMIT = 5 * 1024 * 1024
        private var memoryBuffer: java.io.ByteArrayOutputStream? = java.io.ByteArrayOutputStream()
        private var tempFile: File? = null
        private var fileStream: FileOutputStream? = null
        private val requestBuilder = OkHttpRequest.Builder().url(url).apply { headers.forEach { addHeader(it.key, it.value) } }
        override fun getBody(): OutputStream = object : OutputStream() {
            override fun write(b: Int) { write(byteArrayOf(b.toByte()), 0, 1) }
            override fun write(b: ByteArray, off: Int, len: Int) {
                if (memoryBuffer != null) {
                    if (memoryBuffer!!.size() + len > MEMORY_LIMIT) {
                        tempFile = File.createTempFile("dbx_", ".tmp")
                        fileStream = FileOutputStream(tempFile)
                        memoryBuffer!!.writeTo(fileStream!!)
                        memoryBuffer = null
                    } else memoryBuffer!!.write(b, off, len)
                } else fileStream!!.write(b, off, len)
            }
            override fun close() { fileStream?.close() }
        }
        override fun finish(): DbxResponse {
            fileStream?.close()
            val body = if (tempFile != null) tempFile!!.asRequestBody("application/octet-stream".toMediaType()) else memoryBuffer!!.toByteArray().toRequestBody(null)
            val r = client.newCall(requestBuilder.post(body).build()).execute()
            tempFile?.delete()
            return toDropboxResponse(r)
        }
        override fun close() { fileStream?.close(); tempFile?.delete() }
        override fun abort() { close() }
    }
    override fun startPut(url: String, headers: Iterable<HttpRequestor.Header>) = startPost(url, headers)
    private fun toDropboxResponse(r: OkHttpResponse): DbxResponse {
        val headersMap = mutableMapOf<String, MutableList<String>>()
        for (i in 0 until r.headers.size) headersMap.getOrPut(r.headers.name(i)) { mutableListOf() }.add(r.headers.value(i))
        val bodyStream = r.body?.byteStream()?.let { raw -> object : java.io.FilterInputStream(raw) { override fun close() { super.close(); r.close() } } } ?: java.io.ByteArrayInputStream(ByteArray(0))
        return DbxResponse(r.code, bodyStream, headersMap as Map<String, List<String>>)
    }
}
