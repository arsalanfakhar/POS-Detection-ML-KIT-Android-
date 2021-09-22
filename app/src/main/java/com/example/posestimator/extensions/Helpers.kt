package com.example.posestimator.extensions

import android.graphics.Color

fun MutableList<Int>.averageColor(): Int {
    var redBucket = 0
    var greenBucket = 0
    var blueBucket = 0
    var alphaBucket = 0
    val pixelCount: Int = this.size

    for (color: Int in this) {
        redBucket += Color.red(color)
        greenBucket += Color.green(color)
        blueBucket += Color.blue(color)
        alphaBucket += Color.alpha(color)
    }

    return Color.argb(
        alphaBucket / pixelCount,
        redBucket / pixelCount,
        greenBucket / pixelCount,
        blueBucket / pixelCount
    )

}