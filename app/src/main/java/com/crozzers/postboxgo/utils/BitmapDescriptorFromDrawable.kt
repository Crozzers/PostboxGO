package com.crozzers.postboxgo.utils

import android.content.Context
import android.graphics.Canvas
import android.util.TypedValue
import androidx.core.graphics.createBitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun bitmapDescriptorFromDrawable(
    context: Context,
    resource: Int,
    scaleToHeight: Int = 48
): BitmapDescriptor {
    val vectorDrawable = context.getDrawable(resource)!!
    val sizeInPixels = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        scaleToHeight.toFloat(),
        context.resources.displayMetrics
    ).toInt()
    val width = vectorDrawable.intrinsicWidth / (vectorDrawable.intrinsicHeight / sizeInPixels)
    val height = sizeInPixels
    vectorDrawable.setBounds(
        0, 0,
        width, height
    )
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}