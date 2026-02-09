package com.example.myapplication

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    fun zipFiles(files: List<File>, zipFile: File) {
        val fos = FileOutputStream(zipFile)
        ZipOutputStream(fos).use { zipOut ->
            val buffer = ByteArray(1024 * 8)

            files.forEach { file ->
                if (file.exists() && file.isFile) {
                    FileInputStream(file).use { fis ->
                        val entryName = if (file.parentFile?.name == "collection") {
                            "collection/${file.name}"
                        } else {
                            file.name
                        }

                        val entry = ZipEntry(entryName)
                        zipOut.putNextEntry(entry)

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

    fun unzip(zipFile: File, targetDir: File) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        val buffer = ByteArray(1024 * 8)

        FileInputStream(zipFile).use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val fileName = entry!!.name
                    val newFile = File(targetDir, fileName)

                    if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        entry = zis.nextEntry
                        continue
                    }

                    if (entry!!.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()

                        if (newFile.exists()) {
                            newFile.delete()
                        }

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