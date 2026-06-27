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
    CIRCLE, DROP
}

object MarkerDrawableUtils {

    private val cache = mutableMapOf<String, Drawable>()

    fun getMarkerDrawable(
        context: Context,
        shape: MarkerShape,
        color: Int,
        method: FishingMethod
    ): Drawable {
        val key = "${shape.name}_${color}_${method.name}"
        return cache.getOrPut(key) {
            createCompositedDrawable(context, shape, color, method)
        }
    }

    private fun createCompositedDrawable(
        context: Context,
        shape: MarkerShape,
        color: Int,
        method: FishingMethod
    ): Drawable {
        val bgRes = when (shape) {
            MarkerShape.CIRCLE -> R.drawable.ic_marker_circle
            MarkerShape.DROP -> R.drawable.ic_marker_drop
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
                val iconSize = (width * 0.65f).toInt()
                val iconLeft = (width - iconSize) / 2
                val iconTop = when (shape) {
                    MarkerShape.CIRCLE -> (height - iconSize) / 2
                    MarkerShape.DROP -> (height / 3 - iconSize / 2)
                }

                iconDrawable.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                iconDrawable.draw(canvas)
            }
        }

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
