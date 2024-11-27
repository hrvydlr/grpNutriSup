package com.myapp.grpnutrisup.models

data class ActivityLog(
    val activityType: String,
    val duration: Int,
    val caloriesBurned: Double,
    val timestamp: Long
)
