package com.myapp.grpnutrisup.models

data class Food(
    var food_name: String = "",       // Match Firestore field
    var food_desc: String = "",       // Match Firestore field
    var calories: Int = 0,
    var carbohydrate: Int = 0,
    var fat: Int = 0,                 // Match Firestore field
    var fiber: Int = 0,
    var proteins: Int = 0,
    var serving_size: String = "",    // Match Firestore field
    var goal_type: String = "",       // Match Firestore field
    var meal_type: String = "",       // Match Firestore field
    var allergens: String = "",
    var imageUrl: String = ""
)
