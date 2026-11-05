package com.baff.launcher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import kotlin.math.min

class IconRenderer(context: Context) {
    private val packageManager = context.packageManager
    private val cache = object : LruCache<String, Bitmap>(24) {}

    fun getIcon(packageName: String, sizePx: Int): Bitmap? {
        cache.get(packageName)?.let { return it }
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            val bitmap = render(drawable, sizePx)
            cache.put(packageName, bitmap)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun render(drawable: Drawable, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val path = Path().apply {
            addRoundRect(
                RectF(0f, 0f, size.toFloat(), size.toFloat()),
                size * 0.23f,
                size * 0.23f,
                Path.Direction.CW
            )
        }
        canvas.save()
        canvas.clipPath(path)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            drawable is AdaptiveIconDrawable
        ) {
            drawable.background?.let { drawLayer(canvas, it, size) }
            drawable.foreground?.let { drawLayer(canvas, it, size) }
        } else {
            drawLayer(canvas, drawable, size)
        }

        canvas.restore()
        return output
    }

    private fun drawLayer(canvas: Canvas, drawable: Drawable, size: Int) {
        val sourceWidth = drawable.intrinsicWidth.coerceAtLeast(1)
        val sourceHeight = drawable.intrinsicHeight.coerceAtLeast(1)
        val scale = min(size.toFloat() / sourceWidth, size.toFloat() / sourceHeight)
        val width = (sourceWidth * scale).toInt()
        val height = (sourceHeight * scale).toInt()
        val left = (size - width) / 2
        val top = (size - height) / 2
        drawable.setBounds(left, top, left + width, top + height)
        drawable.draw(canvas)
    }
}
