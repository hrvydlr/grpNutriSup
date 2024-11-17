package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CalorieResultActivity : AppCompatActivity() {

    private lateinit var textViewCalorieResult: TextView
    private lateinit var buttonNext: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calorie_result)

        // Initialize FirebaseAuth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        textViewCalorieResult = findViewById(R.id.textViewCalorieResult)
        buttonNext = findViewById(R.id.buttonNext) // Find the button by its ID

        // Get the calorie result from the intent
        val calorieResult = intent.getIntExtra("calorieResult", 0)

        // Display the calorie result
        textViewCalorieResult.text = "Your daily calorie requirement is: $calorieResult kcal"

        // Save the calorie result to Firestore with additional fields
        saveCalorieResult(calorieResult)

        // Set click listener for the "Next" button
        buttonNext.setOnClickListener {
            // Navigate to HomeActivity
            val intent = Intent(this@CalorieResultActivity, HomeActivity::class.java)
            startActivity(intent)
            finish() // Optionally, close the current activity to prevent returning to it
        }
    }

    private fun saveCalorieResult(calorieResult: Int) {
        val user = auth.currentUser
        if (user != null) {
            val userEmail = user.email

            if (userEmail != null) {
                // Calculate remaining calories and set calorie goals for today and tomorrow
                val remainingCalories = calorieResult // Initial remaining calories could be the calorie result itself
                val calorieGoalForToday = calorieResult // Goal for today, it can be adjusted dynamically based on other factors
                val calorieGoalForTomorrow = calorieResult // Similarly, tomorrow's goal can be the same or adjusted

                // Create a map to hold the calorie result and additional data
                val calorieData: Map<String, Any> = hashMapOf(
                    "calorieResult" to calorieResult,
                    "remainingCalories" to remainingCalories,
                    "calorieGoalForToday" to calorieGoalForToday,
                    "calorieGoalForTomorrow" to calorieGoalForTomorrow
                )

                // Save the calorie result and other fields to the user's document in Firestore
                db.collection("users").document(userEmail)
                    .update(calorieData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Calorie result and goals saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save calorie result: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "User email not available!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
        }
    }
}
