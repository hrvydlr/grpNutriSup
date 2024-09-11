package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

class CalorieCalculationActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Assume a username or unique ID for the user
        val username = "username" // Replace with actual user identifier

        // Retrieve the user details from Firestore
        db.collection("users").document(username).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val age = document.getLong("age")?.toInt() ?: 0
                    val gender = document.getString("gender") ?: "Male"
                    val weight = document.getDouble("weight") ?: 0.0
                    val height = document.getDouble("height") ?: 0.0
                    val activityLevel = document.getString("activityLevel") ?: "Sedentary"
                    val goal = document.getString("goal") ?: "Maintain Weight"

                    val bmr = calculateBMR(age, gender, weight, height)
                    val tdee = calculateTDEE(bmr, activityLevel)
                    val adjustedCalories = adjustCaloriesForGoal(tdee, goal)

                    // Pass the calorie result to CalorieResultActivity
                    val intent = Intent(this, CalorieResultActivity::class.java)
                    intent.putExtra("calorieResult", adjustedCalories)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error retrieving data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateBMR(age: Int, gender: String, weight: Double, height: Double): Double {
        return if (gender == "Male") {
            10 * weight + 6.25 * height - 5 * age + 5
        } else {
            10 * weight + 6.25 * height - 5 * age - 161
        }
    }

    private fun calculateTDEE(bmr: Double, activityLevel: String): Double {
        return when (activityLevel) {
            "Sedentary" -> bmr * 1.2
            "Lightly active" -> bmr * 1.375
            "Moderately active" -> bmr * 1.55
            "Very active" -> bmr * 1.725
            "Super active" -> bmr * 1.9
            else -> bmr * 1.2 // Default to Sedentary if activity level is unknown
        }
    }

    private fun adjustCaloriesForGoal(tdee: Double, goal: String): Int {
        return when (goal) {
            "Lose Weight" -> (tdee * 0.75).roundToInt() // 25% calorie deficit
            "Gain Weight" -> (tdee * 1.15).roundToInt() // 15% calorie surplus
            else -> tdee.roundToInt() // Maintain Weight
        }
    }
}
