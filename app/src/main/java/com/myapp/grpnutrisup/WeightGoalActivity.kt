package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WeightGoalActivity : AppCompatActivity() {

    private lateinit var editTextDesiredWeight: EditText
    private lateinit var radioGroupWeightChange: RadioGroup
    private lateinit var buttonSubmitWeightGoal: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight_goal)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        editTextDesiredWeight = findViewById(R.id.editTextDesiredWeight)
        radioGroupWeightChange = findViewById(R.id.radioGroupWeightChange)
        buttonSubmitWeightGoal = findViewById(R.id.buttonSubmitWeightGoal)

        // Set onClickListener for the submit button
        buttonSubmitWeightGoal.setOnClickListener {
            submitWeightGoal()
        }
    }

    private fun submitWeightGoal() {
        val userEmail = auth.currentUser?.email

        // Get the desired weight from the EditText
        val desiredWeight = editTextDesiredWeight.text.toString().toDoubleOrNull()

        // Get the selected weekly weight change from the RadioGroup
        val selectedRadioButtonId = radioGroupWeightChange.checkedRadioButtonId
        val weeklyWeightChange = when (selectedRadioButtonId) {
            R.id.radioButton0_25kg -> 0.25
            R.id.radioButton0_5kg -> 0.5
            R.id.radioButton0_75kg -> 0.75
            else -> null
        }

        if (desiredWeight == null || weeklyWeightChange == null || userEmail == null) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepare data to be saved in Firestore
        val weightGoalData = mapOf(
            "desiredWeight" to desiredWeight,
            "weeklyWeightChange" to weeklyWeightChange
        )

        // Save to Firestore under the user's document
        db.collection("users").document(userEmail)
            .set(weightGoalData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Weight goal saved successfully!", Toast.LENGTH_SHORT).show()

                // Navigate to CalorieCalculationActivity
                val intent = Intent(this, CalorieCalculationActivity::class.java)
                startActivity(intent)
                finish() // Close the current activity
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving weight goal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
