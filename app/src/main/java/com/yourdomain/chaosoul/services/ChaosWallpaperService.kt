package com.yourdomain.chaosoul.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.yourdomain.chaosoul.util.Constants
import java.io.File
import kotlin.math.max

class ChaosWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = ChaosWallpaperEngine()

    private inner class ChaosWallpaperEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        @Volatile
        private var wallpaperBitmap: Bitmap? = loadBitmap()
        private val paint = Paint()

        private var lastSurfaceWidth = 0
        private var lastSurfaceHeight = 0

        private val drawRunner = Runnable { draw() }

        private val refreshReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                // Use the constant for the action
                if (intent?.action == Constants.REFRESH_WALLPAPER_ACTION) {
                    wallpaperBitmap = loadBitmap()
                    handler.post(drawRunner)
                }
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val bitmap = wallpaperBitmap
                    if (bitmap != null) {
                        val scaleX = canvas.width.toFloat() / bitmap.width
                        val scaleY = canvas.height.toFloat() / bitmap.height
                        val scale = max(scaleX, scaleY)

                        val scaledWidth = bitmap.width * scale
                        val scaledHeight = bitmap.height * scale
                        val left = (canvas.width - scaledWidth) / 2
                        val top = (canvas.height - scaledHeight) / 2

                        canvas.drawColor(Color.BLACK)
                        canvas.drawBitmap(bitmap, null, RectF(left, top, left + scaledWidth, top + scaledHeight), paint)
                    } else {
                        canvas.drawColor(Color.BLACK)
                        paint.color = Color.WHITE
                        paint.textSize = 60f
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText("Awaiting first analysis...", canvas.width / 2f, canvas.height / 2f, paint)
                    }
                }
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
        }

        private fun loadBitmap(): Bitmap? {
            val file = File(applicationContext.filesDir, Constants.WALLPAPER_FILENAME)
            return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            // Use the constant for the intent filter
            applicationContext.registerReceiver(refreshReceiver, IntentFilter(Constants.REFRESH_WALLPAPER_ACTION))
            handler.post(drawRunner)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            // Check if this is a meaningful size change (i.e., a rotation)
            if (width > 0 && height > 0) {
                if (lastSurfaceWidth > 0 && lastSurfaceHeight > 0) {
                    if (width != lastSurfaceWidth || height != lastSurfaceHeight) {
                        // This is an orientation change. Increment our counter.
                        val prefs = applicationContext.getSharedPreferences(Constants.LAST_STATE_PREFS, Context.MODE_PRIVATE)
                        val currentCount = prefs.getInt(Constants.PREF_ORIENTATION_CHANGES, 0)
                        prefs.edit().putInt(Constants.PREF_ORIENTATION_CHANGES, currentCount + 1).apply()
                    }
                }
                lastSurfaceWidth = width
                lastSurfaceHeight = height
            }
            handler.post(drawRunner) // Redraw on surface change
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                wallpaperBitmap = loadBitmap()
                handler.post(drawRunner)
            } else {
                handler.removeCallbacks(drawRunner)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            applicationContext.unregisterReceiver(refreshReceiver)
            handler.removeCallbacks(drawRunner)
        }
    }
}