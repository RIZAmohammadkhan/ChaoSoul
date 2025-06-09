package com.yourdomain.chaosoul.data.model

data class InteractionData(
    val workTimeMs: Long,
    val socialTimeMs: Long,
    val entertainmentTimeMs: Long,
    val totalTypingDurationMs: Long,
    val avgBrightness: Float, // 0.0 to 1.0
    val isSilent: Boolean,
    val headphonesOn: Boolean,
    val locationEntropy: Int, // Placeholder for now
    val orientationChanges: Int // Placeholder for now
)