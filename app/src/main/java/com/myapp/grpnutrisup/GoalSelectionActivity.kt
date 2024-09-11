package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class GoalSelectionActivity : AppCompatActivity() {

    private lateinit var buttonMaintainWeight: Button
    private lateinit var buttonLoseWeight: Button
    private lateinit var buttonGainWeight: Button
    private val db = FirebaseFirestore.getInstance() // Initialize Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_selection)

        // Initialize buttons
        buttonMaintainWeight = findViewById(R.id.buttonMaintainWeight)
        buttonLoseWeight = findViewById(R.id.buttonLoseWeight)
        buttonGainWeight = findViewById(R.id.buttonGainWeight)

        // Set up click listeners
        buttonMaintainWeight.setOnClickListener {
            handleGoalSelection("Maintain Weight")
        }

        buttonLoseWeight.setOnClickListener {
            handleGoalSelection("Lose Weight")
        }

        buttonGainWeight.setOnClickListener {
            handleGoalSelection("Gain Weight")
        }
    }

    private fun handleGoalSelection(goal: String) {
        val username = "username" // Replace with actual user identifier

        val userGoal: Map<String, Any> = hashMapOf(
            "goal" to goal
        )

        db.collection("users").document(username)
            .update(userGoal)
            .addOnSuccessListener {
                Toast.makeText(this, "Goal Saved Successfully!", Toast.LENGTH_SHORT).show()

                // Navigate to CalorieCalculationActivity
                val intent = Intent(this, CalorieCalculationActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to Save Goal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
