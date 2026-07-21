package com.example.fishing.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.fishing.R
import com.example.fishing.model.FishingMethod

enum class MarkerShape {
    CIRCLE, DROP, DOT
}

object MarkerDrawableUtils {

    private val cache = mutableMapOf<String, Drawable>()

    fun getMarkerDrawable(
        context: Context,
        shape: MarkerShape,
        color: Int,
        method: FishingMethod,
        iconColor: Int = android.graphics.Color.WHITE
    ): Drawable {
        val key = "${shape.name}_${color}_${method.name}_${iconColor}"
        return cache.getOrPut(key) {
            createCompositedDrawable(context, shape, color, method, iconColor)
        }
    }

    private fun createCompositedDrawable(
        context: Context,
        shape: MarkerShape,
        color: Int,
        method: FishingMethod,
        iconColor: Int
    ): Drawable {
        if (shape == MarkerShape.DOT) {
            return createDotMarker(context, color)
        }

        val bgRes = when (shape) {
            MarkerShape.CIRCLE -> R.drawable.ic_marker_circle
            MarkerShape.DROP -> R.drawable.ic_marker_drop
            else -> R.drawable.ic_marker_drop
        }

        val bgDrawable = ContextCompat.getDrawable(context, bgRes)!!.mutate()
        DrawableCompat.setTint(bgDrawable, color)

        val width = bgDrawable.intrinsicWidth
        val height = bgDrawable.intrinsicHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        bgDrawable.setBounds(0, 0, width, height)
        bgDrawable.draw(canvas)

        if (method != FishingMethod.NONE) {
            val iconRes = getMethodIconRes(method)
            if (iconRes != null) {
                val iconDrawable = ContextCompat.getDrawable(context, iconRes)!!
                DrawableCompat.setTint(iconDrawable.mutate(), iconColor)
                val iconSize = (width * 0.65f).toInt()
                val iconLeft = (width - iconSize) / 2
                val iconTop = when (shape) {
                    MarkerShape.CIRCLE -> (height - iconSize) / 2
                    MarkerShape.DROP -> (height / 3 - iconSize / 2)
                    else -> (height / 3 - iconSize / 2)
                }

                iconDrawable.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                iconDrawable.draw(canvas)
            }
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun createDotMarker(context: Context, color: Int): Drawable {
        val density = context.resources.displayMetrics.density
        val size = (60 * density).toInt() // Outer circle diameter
        val innerSize = (10 * density).toInt()
        val strokeWidth = (2 * density)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // 1. Outer circle
        paint.color = android.graphics.Color.BLACK
        paint.alpha = (0.2 * 255).toInt()
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // 2. White stroke for inner dot
        paint.alpha = 255
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, innerSize / 2f + strokeWidth, paint)

        // 3. Inner dot
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, innerSize / 2f, paint)

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun getMethodIconRes(method: FishingMethod): Int? {
        return when (method) {
            FishingMethod.BOBBER -> R.drawable.ic_method_bobber
            FishingMethod.SPINNING -> R.drawable.ic_method_spinning
            FishingMethod.FEEDER -> R.drawable.ic_method_feeder
            FishingMethod.FLY_FISHING -> R.drawable.ic_method_fly
            FishingMethod.NONE -> null
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
