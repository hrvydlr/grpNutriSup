package com.myapp.grpnutrisup.models

data class Food(
    val calories: Int ,
    val carbohydrates: String,
    val description: String ,
    val fats: Int ,
    val fiber: String ,
    val foodName: String,
    val goal: String,
    val mealType: String,
    val proteins: Int,
    val servingSize: String,
    val allergens: String
)
