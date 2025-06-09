package com.yourdomain.chaosoul.processing

// Parameters for the new Duffing Oscillator model
data class DailyParams(
    // Model parameters
    val delta: Float,
    val gamma: Float,
    val omega: Float,

    // Aesthetic parameter
    val colorFactor: Float, // 0.0 for work/cool, 1.0 for social/warm

    // Initial conditions
    val startX: Float,
    val startY: Float
)