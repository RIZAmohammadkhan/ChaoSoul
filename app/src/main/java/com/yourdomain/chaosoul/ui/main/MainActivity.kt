package com.yourdomain.chaosoul.ui.main

import android.app.AppOpsManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo // Import the required ApplicationInfo class
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.Data
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

        // --- THIS IS THE NEW, ROBUST WAY TO CHECK FOR DEBUG MODE ---
        // It checks the application's flags at runtime, avoiding build issues.
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            binding.debugPanel.visibility = View.VISIBLE
        }
        // --- END OF CHANGE ---

        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun setupClickListeners() {
        // ... (The rest of this function is unchanged)
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

        // Main Action Button: Analyze & Generate (Real Data)
        binding.btnRunAnalysis.setOnClickListener {
            Toast.makeText(this, R.string.toast_analysis_triggered, Toast.LENGTH_SHORT).show()
            val analysisWorkRequest = OneTimeWorkRequestBuilder<DailyProcessorWorker>().build()
            WorkManager.getInstance(this).enqueue(analysisWorkRequest)
        }

        // Simulation Button Listener
        binding.btnSimulate.setOnClickListener {
            // Safely parse float values from EditTexts. Default to 0.0f if empty or invalid.
            val work = binding.editWorkEnergy.text.toString().toFloatOrNull() ?: 0.0f
            val social = binding.editSocialEnergy.text.toString().toFloatOrNull() ?: 0.0f
            val creative = binding.editCreativeEnergy.text.toString().toFloatOrNull() ?: 0.0f
            val physical = binding.editPhysicalEnergy.text.toString().toFloatOrNull() ?: 0.0f

            val simulationData = Data.Builder()
                .putBoolean("is_simulation", true)
                .putFloat("work_energy", work)
                .putFloat("social_energy", social)
                .putFloat("creative_energy", creative)
                .putFloat("physical_energy", physical)
                .build()

            val simulationWorkRequest = OneTimeWorkRequestBuilder<DailyProcessorWorker>()
                .setInputData(simulationData)
                .build()

            WorkManager.getInstance(this).enqueue(simulationWorkRequest)
            Toast.makeText(this, "Simulation started with your values!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUiState() {
        // ... (The rest of this file is unchanged) ...
        loadWallpaperPreview()
        val usageGranted = hasUsageStatsPermission()
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val wallpaperActive = isWallpaperServiceActive()

        binding.stepUsageStats.apply {
            stepIcon.setImageResource(R.drawable.ic_usage_stats)
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
        binding.stepAccessibility.apply {
            stepIcon.setImageResource(R.drawable.ic_keyboard)
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
        binding.stepSetWallpaper.apply {
            stepIcon.setImageResource(R.drawable.ic_wallpaper)
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
        binding.btnRunAnalysis.isEnabled = usageGranted && accessibilityEnabled
    }

    private fun loadWallpaperPreview() {
        val wallpaperFile = File(filesDir, Constants.WALLPAPER_FILENAME)
        if (wallpaperFile.exists()) {
            binding.wallpaperPreview.setImageBitmap(BitmapFactory.decodeFile(wallpaperFile.absolutePath))
        } else {
            binding.wallpaperPreview.setImageResource(0)
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${com.yourdomain.chaosoul.services.KeyboardMonitorService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(service) == true
    }

    private fun isWallpaperServiceActive(): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(this)
        return wallpaperManager.wallpaperInfo?.packageName == packageName
    }
}