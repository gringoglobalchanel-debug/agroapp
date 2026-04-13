package com.agroapp.utils

import android.widget.ImageView
import com.agroapp.R

object ImageHelper {
    fun loadImage(productName: String, imageView: ImageView) {
        imageView.setImageDrawable(null)
        imageView.setBackgroundColor(imageView.context.getColor(R.color.green_light))
    }
}
