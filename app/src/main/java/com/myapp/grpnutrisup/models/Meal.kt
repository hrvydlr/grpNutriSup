package com.myapp.grpnutrisup.models

import com.google.firebase.Timestamp

data class Meal(
    val foodName: String = "",
    val calories: Int = 0,
    val carbohydrates: Int = 0,
    val proteins: Int = 0,
    val fats: Int = 0,
    val allergens: String = "",
    val timestamp: Timestamp? = null
)
