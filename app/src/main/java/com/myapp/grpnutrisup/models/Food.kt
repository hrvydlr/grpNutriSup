package com.myapp.grpnutrisup.models

data class Food(
    var food_name: String = "",       // Match Firestore field
    var food_desc: String = "",       // Match Firestore field
    var calories: Int = 0,
    var carbohydrates: Int = 0,
    var fat: Int = 0,                 // Match Firestore field
    var fiber: Int = 0,
    var proteins: Int = 0,
    var serving_size: String = "",    // Match Firestore field
    var goal_type: String = "",       // Match Firestore field
    var meal_type: String = "",       // Match Firestore field
    var allergens: String = "",       // Assuming allergens is a comma-separated string
    var image_url: String = ""
) {

    // Convert Food object to HashMap for Firestore storage
    fun toHashMap(): HashMap<String, Any> {
        return hashMapOf(
            "food_name" to food_name,
            "food_desc" to food_desc,
            "calories" to calories,
            "carbohydrates" to carbohydrates,
            "fat" to fat,
            "fiber" to fiber,
            "proteins" to proteins,
            "serving_size" to serving_size,
            "goal_type" to goal_type,
            "meal_type" to meal_type,
            "allergens" to allergens,
            "image_url" to image_url
        )
    }

    // Create a Food object from a Firestore HashMap
    companion object {
        fun fromHashMap(map: HashMap<String, Any>): Food {
            return Food(
                food_name = map["food_name"] as? String ?: "",
                food_desc = map["food_desc"] as? String ?: "",
                calories = (map["calories"] as? Number)?.toInt() ?: 0,
                carbohydrates = (map["carbohydrates"] as? Number)?.toInt() ?: 0,
                fat = (map["fat"] as? Number)?.toInt() ?: 0,
                fiber = (map["fiber"] as? Number)?.toInt() ?: 0,
                proteins = (map["proteins"] as? Number)?.toInt() ?: 0,
                serving_size = map["serving_size"] as? String ?: "",
                goal_type = map["goal_type"] as? String ?: "",
                meal_type = map["meal_type"] as? String ?: "",
                allergens = map["allergens"] as? String ?: "",
                image_url = map["image_url"] as? String ?: ""
            )
        }
    }
}
