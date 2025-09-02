package com.helptech.abraham.ui.theme

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

fun base64ToImageBitmapOrNull(dataUrl: String?): ImageBitmap? {
    if (dataUrl.isNullOrBlank()) return null
    // aceita tanto com prefixo "data:image/...;base64," quanto apenas o base64
    val commaIdx = dataUrl.indexOf(",")
    val pure = if (dataUrl.startsWith("data:") && commaIdx >= 0) {
        dataUrl.substring(commaIdx + 1)
    } else dataUrl

    return try {
        val bytes = Base64.decode(pure, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}
