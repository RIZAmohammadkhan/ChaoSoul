package com.yourdomain.chaosoul.ui.main

import android.app.AppOpsManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yourdomain.chaosoul.R
import com.yourdomain.chaosoul.databinding.ActivityMainBinding
import com.yourdomain.chaosoul.processing.DailyProcessorWorker
import com.yourdomain.chaosoul.services.ChaosWallpaperService
import com.yourdomain.chaosoul.util.Constants
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun setupClickListeners() {
        // Setup Step 1: Usage Stats
        binding.stepUsageStats.stepActionButton.setOnClickListener {
            if (!hasUsageStatsPermission()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } else {
                Toast.makeText(this, R.string.toast_permission_granted, Toast.LENGTH_SHORT).show()
            }
        }

        // Setup Step 2: Accessibility
        binding.stepAccessibility.stepActionButton.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } else {
                Toast.makeText(this, R.string.toast_service_enabled, Toast.LENGTH_SHORT).show()
            }
        }

        // Setup Step 3: Set Wallpaper
        binding.stepSetWallpaper.stepActionButton.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, ChaosWallpaperService::class.java)
                )
            }
            startActivity(intent)
        }

        // Main Action Button: Analyze & Generate
        binding.btnRunAnalysis.setOnClickListener {
            Toast.makeText(this, R.string.toast_analysis_triggered, Toast.LENGTH_SHORT).show()
            val analysisWorkRequest = OneTimeWorkRequestBuilder<DailyProcessorWorker>().build()
            WorkManager.getInstance(this).enqueue(analysisWorkRequest)
        }
    }

    private fun updateUiState() {
        loadWallpaperPreview()

        val usageGranted = hasUsageStatsPermission()
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val wallpaperActive = isWallpaperServiceActive()

        // Update Usage Stats Step
        binding.stepUsageStats.apply {
            stepIcon.setImageResource(R.drawable.ic_usage_stats) // Create this icon
            stepTitle.text = getString(R.string.step_title_usage)
            stepActionButton.text = getString(R.string.action_grant)
            if (usageGranted) {
                stepStatus.text = getString(R.string.status_granted)
                stepStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.secondary_teal_dark))
                stepActionButton.isEnabled = false
                stepActionButton.alpha = 0.5f
            } else {
                stepStatus.text = getString(R.string.status_required)
                stepStatus.setTextColor(Color.RED)
                stepActionButton.isEnabled = true
                stepActionButton.alpha = 1.0f
            }
        }

        // Update Accessibility Step
        binding.stepAccessibility.apply {
            stepIcon.setImageResource(R.drawable.ic_keyboard) // Create this icon
            stepTitle.text = getString(R.string.step_title_accessibility)
            stepActionButton.text = getString(R.string.action_enable)
            if (accessibilityEnabled) {
                stepStatus.text = getString(R.string.status_enabled)
                stepStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.secondary_teal_dark))
                stepActionButton.isEnabled = false
                stepActionButton.alpha = 0.5f
            } else {
                stepStatus.text = getString(R.string.status_required)
                stepStatus.setTextColor(Color.RED)
                stepActionButton.isEnabled = true
                stepActionButton.alpha = 1.0f
            }
        }

        // Update Set Wallpaper Step
        binding.stepSetWallpaper.apply {
            stepIcon.setImageResource(R.drawable.ic_wallpaper) // Create this icon
            stepTitle.text = getString(R.string.step_title_wallpaper)
            stepActionButton.text = getString(R.string.action_set)
            if (wallpaperActive) {
                stepStatus.text = getString(R.string.status_active)
                stepStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.secondary_teal_dark))
                stepActionButton.isEnabled = false
                stepActionButton.alpha = 0.5f
            } else {
                stepStatus.text = getString(R.string.status_inactive)
                stepStatus.setTextColor(Color.DKGRAY)
                stepActionButton.isEnabled = true
                stepActionButton.alpha = 1.0f
            }
        }


        // Enable the main analysis button only if all core permissions are granted
        binding.btnRunAnalysis.isEnabled = usageGranted && accessibilityEnabled
    }

    private fun loadWallpaperPreview() {
        val wallpaperFile = File(filesDir, Constants.WALLPAPER_FILENAME)
        if (wallpaperFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(wallpaperFile.absolutePath)
            binding.wallpaperPreview.setImageBitmap(bitmap)
        } else {
            // You can set a placeholder image or leave the background
            binding.wallpaperPreview.setImageResource(0) // Clears the image
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${com.yourdomain.chaosoul.services.KeyboardMonitorService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(service) == true
    }

    private fun isWallpaperServiceActive(): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(this)
        val wallpaperInfo = wallpaperManager.wallpaperInfo
        return wallpaperInfo != null && wallpaperInfo.packageName == packageName
    }
}