package com.myapp.grpnutrisup.models

data class Food(
    val foodName: String = "",
    val description: String = "",
    val calories: Int = 0,
    val carbohydrates: Int = 0,
    val proteins: Int = 0,
    val fats: Int = 0,
    val fiber: Int = 0,
    val servingSize: String = "",
    val mealType: String = "",
    val goal: String = ""
)
