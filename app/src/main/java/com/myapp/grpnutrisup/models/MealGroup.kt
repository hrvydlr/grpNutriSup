package com.myapp.grpnutrisup.models

data class MealGroup(
    val date: String,
    val meals: MutableList<Meal> = mutableListOf(),
    var totalCalories: Int = 0
)
