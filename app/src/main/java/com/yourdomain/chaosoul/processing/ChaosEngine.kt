package com.yourdomain.chaosoul.processing

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.set
import com.yourdomain.chaosoul.util.Constants
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max

object ChaosEngine {

    private const val WARMUP_ITERATIONS = 5_000 // Run simulation briefly before drawing

    fun generateWallpaper(params: DailyParams): Pair<Bitmap, Pair<Float, Float>> {
        val width = Constants.WALLPAPER_WIDTH
        val height = Constants.WALLPAPER_HEIGHT

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)

        var x = params.startX
        var y = params.startY

        // --- PASS 1: Calculate Density Map ---
        // This array will store how many times each pixel is "hit" by the simulation.
        val density = IntArray(width * height) { 0 }
        var maxDensity = 1 // Start at 1 to avoid division by zero

        // Run the simulation for a bit without drawing to let it "settle" onto the attractor.
        for (i in 0 until WARMUP_ITERATIONS) {
            val t = i * Constants.DT
            val dx = y
            val dy = x - (x * x * x) - (params.delta * y) + (params.gamma * cos(params.omega * t))
            x += dx * Constants.DT
            y += dy * Constants.DT
        }

        // Now run the main simulation and record the density
        for (i in 0 until Constants.ITERATIONS) {
            val t = (i + WARMUP_ITERATIONS) * Constants.DT
            val dx = y
            val dy = x - (x * x * x) - (params.delta * y) + (params.gamma * cos(params.omega * t))
            x += dx * Constants.DT
            y += dy * Constants.DT

            // Scale to screen coordinates
            val screenX = (x * 350 + width / 2).toInt()
            val screenY = (y * 350 + height / 2).toInt()

            if (screenX in 0 until width && screenY in 0 until height) {
                val index = screenY * width + screenX
                density[index]++
                if (density[index] > maxDensity) {
                    maxDensity = density[index]
                }
            }
        }

        // --- PASS 2: Render Bitmap from Density Map ---
        // This pass converts the density values into beautiful colors.
        val coolColor = Color.rgb(0, 150, 255) // Electric Blue
        val warmColor = Color.rgb(255, 20, 147) // Deep Pink

        // Use a logarithmic scale for density to bring out detail in less-visited areas
        val logMaxDensity = log10(maxDensity.toFloat() + 1.0f)

        for (py in 0 until height) {
            for (px in 0 until width) {
                val index = py * width + px
                val count = density[index]
                if (count > 0) {
                    // 1. Determine base color by position (horizontal gradient)
                    // The 'colorFactor' from the worker shifts the balance
                    val xRatio = (px.toFloat() / width).coerceIn(0f, 1f)
                    val baseColor = blendColors(coolColor, warmColor, xRatio * 0.5f + params.colorFactor * 0.5f)

                    // 2. Determine brightness by density (logarithmic scale)
                    val logDensity = log10(count.toFloat() + 1.0f)
                    val brightness = (logDensity / logMaxDensity).coerceIn(0.1f, 1.0f)

                    // 3. Combine base color and brightness
                    val finalAlpha = 255
                    val finalRed = (Color.red(baseColor) * brightness).toInt()
                    val finalGreen = (Color.green(baseColor) * brightness).toInt()
                    val finalBlue = (Color.blue(baseColor) * brightness).toInt()

                    bitmap[px, py] = Color.argb(finalAlpha, finalRed, finalGreen, finalBlue)
                }
            }
        }

        return Pair(bitmap, Pair(x, y)) // Return final coordinates for the next day
    }

    private fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
        val invRatio = 1f - ratio
        val r = Color.red(c1) * invRatio + Color.red(c2) * ratio
        val g = Color.green(c1) * invRatio + Color.green(c2) * ratio
        val b = Color.blue(c1) * invRatio + Color.blue(c2) * ratio
        return Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }
}