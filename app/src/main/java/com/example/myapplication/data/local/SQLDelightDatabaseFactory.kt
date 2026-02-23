package com.example.myapplication.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.myapplication.data.local.AnimeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.lang.reflect.Method

class SQLDelightDatabaseFactory(private val context: Context) {
    private var cachedDriver: SqlDriver? = null
    private var database: AnimeDatabase? = null

    /** При изменении триггера Flow в DataSource переподписываются на новое подключение. */
    val dbConnectionTrigger = MutableStateFlow(0)

    private fun getDriver(): SqlDriver {
        if (cachedDriver == null) {
            cachedDriver = AndroidSqliteDriver(
                schema = AnimeDatabase.Schema,
                context = context,
                name = "anime.db"
            )
        }
        return cachedDriver!!
    }

    fun getDatabase(): AnimeDatabase {
        if (database == null) {
            database = AnimeDatabase(getDriver())
        }
        return database!!
    }

    /** Закрывает старый коннект и при следующем доступе открывает новый (после .copyTo миграции). */
    fun reconnectDatabase() {
        cachedDriver?.close()
        cachedDriver = null
        database = null
        dbConnectionTrigger.value += 1
    }

    fun createDriver(): SqlDriver = getDriver()

    suspend fun checkpoint() {
        withContext(Dispatchers.IO) {
            try {
                getDriver().let { driver ->
                    if (driver is AndroidSqliteDriver) {
                        // Get SQLiteDatabase through reflection
                        val aClass = Class.forName("app.cash.sqldelight.driver.android.AndroidSqliteDriver")
                        val method = aClass.getDeclaredMethod("getDatabase")
                        method.isAccessible = true
                        val database = method.invoke(driver) as? SQLiteDatabase
                        database?.rawQuery("PRAGMA wal_checkpoint(FULL);", null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val busy = cursor.getInt(0)
                                val log = cursor.getInt(1)
                                val checkpointed = cursor.getInt(2)
                                Log.d("SQLDelight", "WAL checkpoint: busy=$busy, log=$log, checkpointed=$checkpointed")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Checkpoint is not critical, log and continue
                Log.w("SQLDelight", "Failed to checkpoint WAL", e)
            }
        }
    }
}
