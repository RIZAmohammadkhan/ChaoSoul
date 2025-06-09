package com.yourdomain.chaosoul.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import com.yourdomain.chaosoul.R
import com.yourdomain.chaosoul.data.database.AppDatabase
import com.yourdomain.chaosoul.data.model.InteractionData
import com.yourdomain.chaosoul.util.Constants
import org.xmlpull.v1.XmlPullParser
import java.util.*

class DataRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val workApps = mutableSetOf<String>()
    private val socialApps = mutableSetOf<String>()
    private val entertainmentApps = mutableSetOf<String>()

    init {
        parseAppCategories()
    }

    private fun parseAppCategories() {
        val parser = context.resources.getXml(R.xml.app_categories)
        var currentCategory: MutableSet<String>? = null
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "category" -> {
                                val categoryName = parser.getAttributeValue(null, "name")
                                currentCategory = when (categoryName) {
                                    "work" -> workApps
                                    "social" -> socialApps
                                    "entertainment" -> entertainmentApps
                                    else -> null
                                }
                            }
                            "package" -> {
                                currentCategory?.add(parser.nextText())
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "category") {
                            currentCategory = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            parser.close()
        }
    }


    suspend fun getDailyInteractionData(startTime: Long, endTime: Long): InteractionData {
        // ... (app usage and keyboard usage code is unchanged) ...
        var workTime = 0L
        var socialTime = 0L
        var entertainmentTime = 0L
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        usageStats?.forEach {
            when (it.packageName) {
                in workApps -> workTime += it.totalTimeInForeground
                in socialApps -> socialTime += it.totalTimeInForeground
                in entertainmentApps -> entertainmentTime += it.totalTimeInForeground
            }
        }

        val totalTypingDuration = db.typingEventDao().getTotalDurationBetween(startTime, endTime) ?: 0L

        // 3. System State
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isSilent = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
        val headphonesOn = audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn

        // --- THE FIX IS HERE ---
        val rawBrightness = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) { 128 } // Default to 50% brightness on error

        // Clamp the raw value to the expected 0-255 range before normalizing
        val clampedBrightness = rawBrightness.coerceIn(0, 255)
        val avgBrightness = clampedBrightness / 255f
        // --- END OF FIX ---

        val prefs = context.getSharedPreferences(Constants.LAST_STATE_PREFS, Context.MODE_PRIVATE)
        val orientationChanges = prefs.getInt(Constants.PREF_ORIENTATION_CHANGES, 0)

        var activityDiversity = 0
        if (workTime > 0) activityDiversity++
        if (socialTime > 0) activityDiversity++
        if (entertainmentTime > 0) activityDiversity++

        return InteractionData(
            workTimeMs = workTime,
            socialTimeMs = socialTime,
            entertainmentTimeMs = entertainmentTime,
            totalTypingDurationMs = totalTypingDuration,
            avgBrightness = avgBrightness, // This will now be correctly between 0.0 and 1.0
            isSilent = isSilent,
            headphonesOn = headphonesOn,
            locationEntropy = activityDiversity,
            orientationChanges = orientationChanges
        )
    }
}