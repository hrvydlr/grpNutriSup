package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

class CalorieCalculationActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            // Retrieve the user details from Firestore
            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val age = document.getLong("age")?.toInt() ?: 0
                        val gender = document.getString("gender") ?: "Male"
                        val weight = document.getDouble("weight") ?: 0.0
                        val height = document.getDouble("height") ?: 0.0
                        val activityLevel = document.getString("activityLevel") ?: "Sedentary"

                        // Retrieve user's weight goal and weekly weight change
                        val desiredWeight = document.getDouble("desiredWeight") ?: weight
                        val weeklyWeightChange = document.getDouble("weeklyWeightChange") ?: 0.0

                        // Step 1: Calculate BMR
                        val bmr = calculateBMR(age, gender, weight, height)

                        // Step 2: Calculate TDEE based on BMR and activity level
                        val tdee = calculateTDEE(bmr, activityLevel)

                        // Step 3: Adjust based on weight goal
                        val adjustedCalories = adjustCaloriesForGoal(tdee, weight, desiredWeight, weeklyWeightChange)

                        // Save BMR and TDEE to Firestore
                        saveBmrAndTdeeToFirestore(userEmail, bmr, tdee)

                        // Pass the final calorie result to CalorieResultActivity
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
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
        }
    }

    // Step 1: Calculate BMR using Mifflin-St Jeor Equation
    private fun calculateBMR(age: Int, gender: String, weight: Double, height: Double): Double {
        return if (gender == "Male") {
            (10 * weight) + (6.25 * height) - (5 * age) + 5
        } else {
            (10 * weight) + (6.25 * height) - (5 * age) - 161
        }
    }

    // Step 2: Calculate TDEE by multiplying BMR by activity factor
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

    // Step 3: Adjust calories based on weight goal (weekly weight change)
    private fun adjustCaloriesForGoal(tdee: Double, currentWeight: Double, desiredWeight: Double, weeklyWeightChange: Double): Int {
        val dailyCalorieAdjustment: Double

        // If user wants to lose weight
        if (desiredWeight < currentWeight) {
            // Caloric deficit for weight loss (7700 kcal = 1kg of body weight)
            dailyCalorieAdjustment = (7700 * weeklyWeightChange) / 7
            return (tdee - dailyCalorieAdjustment).roundToInt()
        }
        // If user wants to gain weight
        else if (desiredWeight > currentWeight) {
            // Caloric surplus for weight gain (7700 kcal = 1kg of body weight)
            dailyCalorieAdjustment = (7700 * weeklyWeightChange) / 7
            return (tdee + dailyCalorieAdjustment).roundToInt()
        }

        // If the user wants to maintain weight
        return tdee.roundToInt()
    }

    // Save BMR and TDEE to Firestore
    private fun saveBmrAndTdeeToFirestore(userEmail: String, bmr: Double, tdee: Double) {
        val data = mapOf(
            "BMR" to bmr,
            "TDEE" to tdee
        )

        db.collection("users").document(userEmail)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "BMR and TDEE saved successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save BMR and TDEE: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
