package com.mraphaelpy.terriflow.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reduz a foto escolhida e a converte em data URI JPEG, pequena o bastante
 * para ser salva direto no documento do usuário no Firestore.
 */
@Singleton
class ProfilePhotoEncoder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun encode(uri: Uri): String? = withContext(Dispatchers.IO) {
        val bitmap = decodeSampled(uri) ?: return@withContext null
        val scaled = scaleToFit(bitmap, MAX_SIZE_PX)
        val bytes = ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray()
        }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Decodifica já reduzida para não carregar fotos de vários megapixels inteiras na memória. */
    private fun decodeSampled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIZE_PX) sampleSize *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun scaleToFit(bitmap: Bitmap, maxSize: Int): Bitmap {
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val (width, height) = if (ratio > 1) {
            maxSize to (maxSize / ratio).toInt()
        } else {
            (maxSize * ratio).toInt() to maxSize
        }
        return Bitmap.createScaledBitmap(bitmap, width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    private companion object {
        const val MAX_SIZE_PX = 300
        const val JPEG_QUALITY = 70
    }
}
