package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ActivityLevelActivity : AppCompatActivity() {

    private lateinit var radioGroupActivityLevel: RadioGroup
    private lateinit var buttonSubmit: Button
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance() // Initialize Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_selection)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize the views
        radioGroupActivityLevel = findViewById(R.id.radioGroupActivityLevel)
        buttonSubmit = findViewById(R.id.buttonSubmit)

        buttonSubmit.setOnClickListener {
            val selectedId = radioGroupActivityLevel.checkedRadioButtonId
            val selectedRadioButton = findViewById<RadioButton>(selectedId)

            // Get the selected activity level as a string
            val activityLevel = selectedRadioButton.text.toString()

            // Save the activity level to Firestore using email as the document ID
            saveActivityLevel(activityLevel)
        }
    }

    private fun saveActivityLevel(activityLevel: String) {
        val user = auth.currentUser
        if (user != null) {
            val userEmail = user.email

            if (userEmail != null) {
                // Create a map to hold the selected activity level
                val userActivity: Map<String, Any> = hashMapOf(
                    "activityLevel" to activityLevel
                )

                // Save activity level to Firestore with merge option to preserve other fields
                db.collection("users").document(userEmail)
                    .set(userActivity, SetOptions.merge()) // Use merge to avoid overwriting other fields
                    .addOnSuccessListener {
                        Toast.makeText(this, "Activity Level Saved Successfully!", Toast.LENGTH_SHORT).show()

                        // Navigate to the next activity (e.g., GoalSelectionActivity)
                        val intent = Intent(this, GoalSelectionActivity::class.java)
                        startActivity(intent)
                        finish() // Optionally close the current activity
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to Save Activity Level: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "User email not available!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
        }
    }
}
