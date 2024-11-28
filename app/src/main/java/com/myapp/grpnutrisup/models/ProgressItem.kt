package com.myapp.grpnutrisup.models

data class ProgressItem(
    val day: String,
    val calorieIntake: Int,
    val calorieGoal: Int,
    val fats: Int,
    val protein: Int
)
