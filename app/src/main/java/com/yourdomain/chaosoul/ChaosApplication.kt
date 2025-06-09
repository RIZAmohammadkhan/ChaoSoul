package com.yourdomain.chaosoul

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yourdomain.chaosoul.processing.DailyProcessorWorker
import com.yourdomain.chaosoul.util.Constants
import java.util.concurrent.TimeUnit

class ChaosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupDailyWorker()
    }

    private fun setupDailyWorker() {
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyProcessorWorker>(24, TimeUnit.HOURS)
            // For testing, you can use a shorter interval like 15 minutes
            // val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyProcessorWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.DAILY_PROCESSING_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}