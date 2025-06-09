package com.yourdomain.chaosoul.util

object Constants {
    // --- CORE SIMULATION CONSTANTS ---
    const val DT = 0.02f // A slightly larger time step is fine for this model
    const val ITERATIONS = 350_000 // More iterations to let the chaos develop
    const val WALLPAPER_WIDTH = 1080
    const val WALLPAPER_HEIGHT = 2400

    // --- SYSTEM CONSTANTS ---
    const val DAILY_PROCESSING_WORK_TAG = "daily_processing_work"
    const val LAST_STATE_PREFS = "last_state_prefs"
    const val LAST_X_KEY = "last_x"
    const val LAST_Y_KEY = "last_y"
    const val WALLPAPER_FILENAME = "soul_wallpaper.png"
    const val REFRESH_WALLPAPER_ACTION = "com.yourdomain.chaosoul.REFRESH_WALLPAPER"
    const val PREF_ORIENTATION_CHANGES = "orientation_changes_count"
    const val APP_CATEGORIES_PREFS = "app_categories_prefs"
}