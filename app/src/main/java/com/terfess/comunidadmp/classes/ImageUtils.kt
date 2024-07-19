package com.terfess.comunidadmp.classes

import android.graphics.Bitmap
import android.util.Log
import java.io.ByteArrayOutputStream

object ImageUtils {

    private const val TAG = "ImageUtils"

    /**
     * Comprime una imagen Bitmap para reducir su tamaño.
     *
     * @param bitmap Imagen Bitmap a comprimir.
     * @param maxSize Tamaño máximo en bytes después de la compresión.
     * @return ByteArray del Bitmap comprimido.
     */
    fun compressBitmap(bitmap: Bitmap, maxSize: Long): ByteArray {
        val outputStream = ByteArrayOutputStream()
        var quality = 100 // Calidad inicial máxima
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        // Reducir la calidad iterativamente hasta alcanzar el tamaño deseado
        while (outputStream.size() > maxSize && quality > 0) {
            outputStream.reset() // Limpiar el OutputStream
            quality -= 10 // Reducir la calidad en 10 cada vez
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        return outputStream.toByteArray()
    }
}