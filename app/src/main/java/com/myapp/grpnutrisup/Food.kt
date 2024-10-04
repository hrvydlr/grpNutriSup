package com.myapp.grpnutrisup.models

data class Food(
    val calories: Int = 0,
    val carbohydrates: Int = 0,
    val description: String = "",
    val fats: Int = 0,
    val fiber: Int = 0,
    val foodName: String = "",
    val goal: String = "",
    val mealType: String = "",
    val proteins: Int = 0,
    val servingSize: String = ""
)
