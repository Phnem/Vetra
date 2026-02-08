package com.example.myapplication

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    // Упаковать список файлов в один ZIP (Стриминг без лишней памяти)
    fun zipFiles(files: List<File>, zipFile: File) {
        // Создаем поток записи в файл
        val fos = FileOutputStream(zipFile)
        // Оборачиваем в ZipOutputStream. Важно: Не используем BufferedOutputStream большого размера.
        // ZipOutputStream сам по себе достаточно эффективен для записи.
        ZipOutputStream(fos).use { zipOut ->
            val buffer = ByteArray(1024 * 8) // Буфер 8 КБ - безопасно для памяти

            files.forEach { file ->
                if (file.exists() && file.isFile) { // Проверка, что это файл, а не папка
                    FileInputStream(file).use { fis ->
                        // Важно: имя в архиве должно быть относительным
                        val entryName = if (file.parentFile?.name == "collection") {
                            "collection/${file.name}"
                        } else {
                            file.name
                        }

                        val entry = ZipEntry(entryName)
                        zipOut.putNextEntry(entry)

                        // Переливаем данные порциями по 8 КБ
                        var length: Int
                        while (fis.read(buffer).also { length = it } >= 0) {
                            zipOut.write(buffer, 0, length)
                        }
                        zipOut.closeEntry()
                    }
                }
            }
        }
    }

    // Распаковать ZIP в папку
    // Распаковать ZIP в папку
    fun unzip(zipFile: File, targetDir: File) {
        if (!targetDir.exists()) targetDir.mkdirs()
        val buffer = ByteArray(1024 * 8)

        FileInputStream(zipFile).use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val fileName = entry!!.name
                    val newFile = File(targetDir, fileName)

                    // Защита от Zip Slip
                    if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        entry = zis.nextEntry
                        continue
                    }

                    if (entry!!.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()

                        // --- ДОБАВЛЕНО: Удаляем старый файл перед записью для надежности ---
                        if (newFile.exists()) newFile.delete()
                        // -----------------------------------------------------------------

                        FileOutputStream(newFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        }
    }
}