package com.yourdomain.chaosoul.processing

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.set
import com.yourdomain.chaosoul.util.Constants
import kotlin.math.abs

data class DailyParams(
    val alpha: Float, val beta: Float, val Fx_driving: Float, val Fy_driving: Float,
    val startX: Float, val startY: Float
)

object ChaosEngine {

    fun generateWallpaper(params: DailyParams): Pair<Bitmap, Pair<Float, Float>> {
        val bitmap = Bitmap.createBitmap(Constants.WALLPAPER_WIDTH, Constants.WALLPAPER_HEIGHT, Bitmap.Config.ARGB_8888)

        var x = params.startX
        var y = params.startY

        val color1 = if (params.Fx_driving > 0) Color.rgb(255, 69, 0) else Color.rgb(30, 144, 255) // OrangeRed vs DodgerBlue
        val color2 = if (params.Fy_driving > 0) Color.rgb(255, 215, 0) else Color.rgb(60, 179, 113) // Gold vs MediumSeaGreen

        for (i in 0 until Constants.ITERATIONS) {
            val dX = (-params.alpha * x + params.beta * y + params.Fx_driving) * Constants.DT
            val dY = (-params.alpha * y - params.beta * x + params.Fy_driving) * Constants.DT

            x += dX
            y += dY

            val screenX = (x * 150 + Constants.WALLPAPER_WIDTH / 2).toInt()
            val screenY = (y * 150 + Constants.WALLPAPER_HEIGHT / 2).toInt()

            if (screenX in 0 until Constants.WALLPAPER_WIDTH && screenY in 0 until Constants.WALLPAPER_HEIGHT) {
                val ratio = (abs(x) + abs(y)) / 10.0f
                val blendedColor = blendColors(color1, color2, ratio.coerceIn(0f, 1f))
                bitmap[screenX, screenY] = blendedColor
            }
        }
        return Pair(bitmap, Pair(x, y)) // Return bitmap and final coordinates
    }

    private fun blendColors(c1: Int, c2: Int, r: Float): Int {
        val ir = 1f - r
        val r1 = Color.red(c1) * ir; val g1 = Color.green(c1) * ir; val b1 = Color.blue(c1) * ir
        val r2 = Color.red(c2) * r;  val g2 = Color.green(c2) * r;  val b2 = Color.blue(c2) * r
        return Color.rgb((r1 + r2).toInt(), (g1 + g2).toInt(), (b1 + b2).toInt())
    }
}