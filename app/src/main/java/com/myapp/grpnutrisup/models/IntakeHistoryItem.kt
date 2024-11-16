package com.myapp.grpnutrisup.models

import java.util.Date

data class IntakeHistoryItem(
    val foodName: String,
    val calories: Int,
    val timestamp: Date
)
