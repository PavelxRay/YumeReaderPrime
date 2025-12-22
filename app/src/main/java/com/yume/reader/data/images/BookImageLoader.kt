// data/images/BookImageLoader.kt (переименовать файл)
package com.yume.reader.data.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookImageLoader @Inject constructor(
    private val context: Context
) {

    fun createDataUrlFromBytes(imageData: ByteArray): String {
        return try {
            val mimeType = detectMimeType(imageData)
            val base64 = Base64.encodeToString(imageData, Base64.DEFAULT)
            "data:$mimeType;base64,$base64"
        } catch (e: Exception) {
            ""
        }
    }

    private fun detectMimeType(data: ByteArray): String {
        return try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
            options.outMimeType ?: "image/jpeg"
        } catch (e: Exception) {
            "image/jpeg"
        }
    }

    suspend fun saveImageToCache(imageData: ByteArray, bookId: Long, imageId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "book_images/$bookId")
                cacheDir.mkdirs()

                val imageFile = File(cacheDir, "$imageId.jpg")
                imageFile.writeBytes(imageData)

                imageFile.absolutePath
            } catch (e: Exception) {
                // Если не удалось сохранить, создаем Data URL
                createDataUrlFromBytes(imageData)
            }
        }
    }

    fun clearCacheForBook(bookId: Long) {
        try {
            val cacheDir = File(context.cacheDir, "book_images/$bookId")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        } catch (e: Exception) {
            // Игнорируем ошибки очистки кеша
        }
    }
}