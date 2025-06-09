package com.yourdomain.chaosoul.processing

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourdomain.chaosoul.data.database.AppDatabase
import com.yourdomain.chaosoul.data.repository.DataRepository
import com.yourdomain.chaosoul.util.Constants
import java.io.File
import java.io.FileOutputStream
import java.util.*

class DailyProcessorWorker(private val appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    // Define a tag for our logs
    private val TAG = "DailyProcessorWorker"

    // Define safe limits for the driving forces
    private val MAX_DRIVING_FORCE = 5.0f
    private val MIN_DRIVING_FORCE = -5.0f

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting daily processing work.")
        return try {
            val repository = DataRepository(appContext)
            val prefs = appContext.getSharedPreferences(Constants.LAST_STATE_PREFS, Context.MODE_PRIVATE)

            // 1. Define time window for the last 24 hours
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (24 * 60 * 60 * 1000)

            // 2. GATHER DATA
            val data = repository.getDailyInteractionData(startTime, endTime)
            Log.d(TAG, "Interaction Data: $data")

            // 3. CALCULATE FORCES & PARAMETERS
            val fSocial = data.socialTimeMs * Constants.K_SOCIAL_FORCE
            val fWork = data.workTimeMs * Constants.K_WORK_FORCE
            val fCreation = data.totalTypingDurationMs * Constants.K_CREATION

            val fInterface = Constants.K_INTERFACE * (data.avgBrightness + (if (data.isSilent) -0.5f else 0.5f) + (if (data.headphonesOn) -0.5f else 0.5f))

            val rawFxDriving = (fSocial + fCreation) - fWork + fInterface
            val fxDriving = rawFxDriving.coerceIn(MIN_DRIVING_FORCE, MAX_DRIVING_FORCE)

            val fExplore = data.locationEntropy * Constants.K_EXPLORE_FORCE
            val fDynamism = data.orientationChanges * Constants.K_DYNAMISM

            val rawFyDriving = fExplore + fDynamism
            val fyDriving = rawFyDriving.coerceIn(MIN_DRIVING_FORCE, MAX_DRIVING_FORCE)

            val entropy = (1 + data.locationEntropy + (data.workTimeMs + data.socialTimeMs + data.entertainmentTimeMs) / 3_600_000f).coerceAtLeast(1.0f) // ensure entropy is at least 1
            val alpha = Constants.K_ALPHA / entropy
            val beta = Constants.K_BETA * (data.locationEntropy + data.totalTypingDurationMs / 60_000f)

            val lastX = prefs.getFloat(Constants.LAST_X_KEY, 0.1f)
            val lastY = prefs.getFloat(Constants.LAST_Y_KEY, 0.1f)

            Log.d(TAG, "--- Calculated Parameters ---")
            Log.d(TAG, "Alpha: $alpha, Beta: $beta")
            Log.d(TAG, "Fx Driving (Raw -> Clamped): $rawFxDriving -> $fxDriving")
            Log.d(TAG, "Fy Driving (Raw -> Clamped): $rawFyDriving -> $fyDriving")
            Log.d(TAG, "Starting Coords (X, Y): ($lastX, $lastY)")
            Log.d(TAG, "---------------------------")

            val params = DailyParams(
                alpha = alpha, beta = beta, Fx_driving = fxDriving, Fy_driving = fyDriving,
                startX = lastX, startY = lastY
            )

            // 4. GENERATE & SAVE
            Log.d(TAG, "Generating wallpaper with ChaosEngine...")
            val (bitmap, finalCoords) = ChaosEngine.generateWallpaper(params)
            Log.d(TAG, "Wallpaper generated. Saving to file.")

            val file = File(appContext.filesDir, Constants.WALLPAPER_FILENAME)
            FileOutputStream(file).use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            Log.d(TAG, "File saved to ${file.absolutePath}. Size: ${file.length()} bytes.")

            // 5. SAVE FINAL STATE AND RESET COUNTERS
            prefs.edit()
                .putFloat(Constants.LAST_X_KEY, finalCoords.first)
                .putFloat(Constants.LAST_Y_KEY, finalCoords.second)
                .putInt(Constants.PREF_ORIENTATION_CHANGES, 0)
                .apply()

            // 6. PRUNE OLD DATA FROM THE DATABASE
            val db = AppDatabase.getDatabase(appContext) // This db instance is scoped to the try block
            val deletedRows = db.typingEventDao().clearOldEvents(startTime)
            Log.d(TAG, "Pruned $deletedRows old typing events from database.")

            // 7. NOTIFY THE WALLPAPER SERVICE TO REFRESH
            Log.d(TAG, "Sending refresh broadcast to wallpaper service.")
            val refreshIntent = Intent(Constants.REFRESH_WALLPAPER_ACTION)
            appContext.sendBroadcast(refreshIntent)

            Log.d(TAG, "Work finished successfully.")
            Result.success()
        } catch (e: Exception) {
            // Log the actual exception
            Log.e(TAG, "Worker failed with an exception", e)
            // The problematic line has been removed from here.
            Result.failure()
        }
    }
}