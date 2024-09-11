package com.myapp.grpnutrisup

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ActivityLevelActivity : AppCompatActivity() {

    private lateinit var radioGroupActivityLevel: RadioGroup
    private lateinit var buttonSubmit: Button
    private val db = FirebaseFirestore.getInstance() // Initialize Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_selection)

        // Initialize the views
        radioGroupActivityLevel = findViewById(R.id.radioGroupActivityLevel)
        buttonSubmit = findViewById(R.id.buttonSubmit)

        buttonSubmit.setOnClickListener {
            val selectedId = radioGroupActivityLevel.checkedRadioButtonId
            val selectedRadioButton = findViewById<RadioButton>(selectedId)
            val activityFactor = when (selectedRadioButton.text.toString()) {
                "Sedentary" -> 1.2
                "Lightly active" -> 1.375
                "Moderately active" -> 1.55
                "Very active" -> 1.725
                "Super active" -> 1.9
                else -> 1.0
            }

            // Assume BMR is provided or calculated elsewhere in your app
            val bmr = 1500 // Replace with actual BMR value
            val tdee = bmr * activityFactor

            saveActivityLevelAndTDEE(selectedRadioButton.text.toString(), tdee)
        }
    }

    private fun saveActivityLevelAndTDEE(activityLevel: String, tdee: Double) {
        // Assume a username or unique ID for the user
        val username = "username" // Replace with actual user identifier

        // Create a map to hold the selected activity level and TDEE
        val userActivity: Map<String, Any> = hashMapOf(
            "activityLevel" to activityLevel,
            "tdee" to tdee
        )

        // Save activity level and TDEE to Firestore
        db.collection("users").document(username)
            .update(userActivity)
            .addOnSuccessListener {
                Toast.makeText(this, "Activity Level and TDEE Saved Successfully!", Toast.LENGTH_SHORT).show()
                // Optionally, navigate to another activity or update the UI
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to Save Data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
