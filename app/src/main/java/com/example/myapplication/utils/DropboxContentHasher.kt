package com.example.myapplication.utils

import java.io.File
import java.security.MessageDigest

/**
 * Имплементация официального алгоритма Dropbox Content Hash.
 * Позволяет проверять идентичность локальных и облачных файлов без скачивания.
 */
object DropboxContentHasher {
    private const val BLOCK_SIZE = 4 * 1024 * 1024 // 4 MB

    fun hashFile(file: File): String {
        if (!file.exists() || file.length() == 0L) return ""

        val mdOverall = MessageDigest.getInstance("SHA-256")
        val mdBlock = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BLOCK_SIZE)

        file.inputStream().buffered().use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                mdBlock.update(buffer, 0, bytesRead)
                mdOverall.update(mdBlock.digest())
                mdBlock.reset()
            }
        }
        return mdOverall.digest().joinToString("") { "%02x".format(it) }
    }
}
