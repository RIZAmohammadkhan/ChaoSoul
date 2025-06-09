package com.yourdomain.chaosoul.util

object Constants {
    const val K_ALPHA = 5.0f
    const val K_BETA = 0.15f
    const val K_INTERFACE = 0.8f
    const val K_DYNAMISM = 0.02f
    const val K_CREATION = 0.0005f
    const val K_SOCIAL_FORCE = 0.00005f
    const val K_WORK_FORCE = 0.00005f
    const val K_EXPLORE_FORCE = 0.1f

    const val DT = 0.01f
    const val ITERATIONS = 250_000
    const val WALLPAPER_WIDTH = 1080
    const val WALLPAPER_HEIGHT = 2400

    const val DAILY_PROCESSING_WORK_TAG = "daily_processing_work"
    const val LAST_STATE_PREFS = "last_state_prefs"
    const val LAST_X_KEY = "last_x"
    const val LAST_Y_KEY = "last_y"
    const val WALLPAPER_FILENAME = "soul_wallpaper.png"

    // --- NEW/MODIFIED CONSTANTS ---
    const val REFRESH_WALLPAPER_ACTION = "com.yourdomain.chaosoul.REFRESH_WALLPAPER"
    const val PREF_ORIENTATION_CHANGES = "orientation_changes_count"
    const val APP_CATEGORIES_PREFS = "app_categories_prefs" // For app categories
}