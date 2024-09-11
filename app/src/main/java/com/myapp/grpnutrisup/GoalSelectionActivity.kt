package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GoalSelectionActivity : AppCompatActivity() {

    private lateinit var buttonMaintainWeight: Button
    private lateinit var buttonLoseWeight: Button
    private lateinit var buttonGainWeight: Button
    private val db = FirebaseFirestore.getInstance() // Initialize Firestore instance
    private lateinit var auth: FirebaseAuth // Initialize FirebaseAuth instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_selection)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

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
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userEmail = currentUser.email

            val userGoal: Map<String, Any> = hashMapOf(
                "goal" to goal
            )

            // Use the user's email as the document ID
            db.collection("users").document(userEmail!!)
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
        } else {
            Toast.makeText(this, "User not signed in!", Toast.LENGTH_SHORT).show()
        }
    }
}
