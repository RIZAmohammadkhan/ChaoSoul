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
import kotlin.math.abs
import kotlin.math.tanh

class DailyProcessorWorker(private val appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    private val TAG = "DailyProcessorWorker"

    override suspend fun doWork(): Result {
        return try {
            val prefs = appContext.getSharedPreferences(Constants.LAST_STATE_PREFS, Context.MODE_PRIVATE)
            val isSimulation = inputData.getBoolean("is_simulation", false)

            val workEnergy: Double
            val socialEnergy: Double
            val creativeEnergy: Double
            val physicalEnergy: Double

            if (isSimulation) {
                Log.d(TAG, "Starting SIMULATED processing with Duffing model.")
                workEnergy = inputData.getFloat("work_energy", 0f).toDouble()
                socialEnergy = inputData.getFloat("social_energy", 0f).toDouble()
                creativeEnergy = inputData.getFloat("creative_energy", 0f).toDouble()
                physicalEnergy = inputData.getFloat("physical_energy", 0f).toDouble()
                Log.d(TAG, "Simulated Energies: W=$workEnergy, S=$socialEnergy, C=$creativeEnergy, P=$physicalEnergy")
            } else {
                Log.d(TAG, "Starting REAL DATA processing with Duffing model.")
                val repository = DataRepository(appContext)
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (24 * 60 * 60 * 1000)
                val data = repository.getDailyInteractionData(startTime, endTime)
                Log.d(TAG, "Interaction Data: $data")

                val timeScaleFactor = 1.0 / (60 * 60 * 1000)
                workEnergy = data.workTimeMs * timeScaleFactor
                socialEnergy = data.socialTimeMs * timeScaleFactor
                creativeEnergy = data.totalTypingDurationMs * (timeScaleFactor * 10)
                physicalEnergy = (data.orientationChanges * 0.1 + data.locationEntropy)
            }

            val focusAxis = (socialEnergy + creativeEnergy) - workEnergy
            val dynamismAxis = (socialEnergy + workEnergy + creativeEnergy + physicalEnergy)
            val focusTanh = tanh(focusAxis.toFloat())
            val dynamismTanh = tanh(dynamismAxis.toFloat() * 0.2f)

            val delta = (0.2f - dynamismTanh * 0.18f).coerceIn(0.02f, 0.2f)
            val gamma = (0.36f + focusTanh * 0.04f).coerceIn(0.32f, 0.4f)
            val omega = 1.2f
            val colorFactor = (focusTanh + 1.0f) / 2.0f

            // --- THIS IS THE CRITICAL ROBUSTNESS FIX ---
            // 1. Sanitize Initial Conditions to prevent crashes from bad data
            var lastX = prefs.getFloat(Constants.LAST_X_KEY, 0.1f)
            var lastY = prefs.getFloat(Constants.LAST_Y_KEY, 0.1f)

            if (!lastX.isFinite() || !lastY.isFinite() || abs(lastX) > 100f || abs(lastY) > 100f) {
                Log.w(TAG, "Invalid start coordinates detected ($lastX, $lastY). Resetting to default.")
                lastX = 0.1f
                lastY = 0.1f
            }
            // --- END OF FIX ---

            Log.d(TAG, "--- Duffing Model Parameters ---")
            Log.d(TAG, "Delta (Damping): $delta, Gamma (Force): $gamma, Color Factor: $colorFactor")
            Log.d(TAG, "Starting Coords (X, Y): ($lastX, $lastY)")
            Log.d(TAG, "--------------------------------")

            val params = DailyParams(
                delta = delta, gamma = gamma, omega = omega,
                colorFactor = colorFactor, startX = lastX, startY = lastY
            )

            val (bitmap, finalCoords) = ChaosEngine.generateWallpaper(params)
            val file = File(appContext.filesDir, Constants.WALLPAPER_FILENAME)
            FileOutputStream(file).use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }

            prefs.edit()
                .putFloat(Constants.LAST_X_KEY, finalCoords.first)
                .putFloat(Constants.LAST_Y_KEY, finalCoords.second)
                .putInt(Constants.PREF_ORIENTATION_CHANGES, 0)
                .apply()

            if (!isSimulation) {
                val db = AppDatabase.getDatabase(appContext)
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (24 * 60 * 60 * 1000)
                db.typingEventDao().clearOldEvents(startTime)
            }

            val refreshIntent = Intent(Constants.REFRESH_WALLPAPER_ACTION)
            appContext.sendBroadcast(refreshIntent)

            Log.d(TAG, "Work finished successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed with an exception", e)
            Result.failure()
        }
    }
}