package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class UserCollectionActivity : AppCompatActivity() {

    private lateinit var editTextAge: EditText
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var editTextHeight: EditText
    private lateinit var editTextWeight: EditText
    private lateinit var buttonSubmit: Button

    // Initialize Firestore
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_details)

        // Initialize the views
        editTextAge = findViewById(R.id.editTextAge)
        radioGroupGender = findViewById(R.id.radioGroupGender)
        editTextHeight = findViewById(R.id.editTextHeight)
        editTextWeight = findViewById(R.id.editTextWeight)
        buttonSubmit = findViewById(R.id.buttonSubmit)

        buttonSubmit.setOnClickListener {
            // Get the input values
            val age = editTextAge.text.toString().toIntOrNull()
            val height = editTextHeight.text.toString().toFloatOrNull()
            val weight = editTextWeight.text.toString().toFloatOrNull()
            val gender = when (radioGroupGender.checkedRadioButtonId) {
                R.id.radioMale -> "Male"
                R.id.radioFemale -> "Female"
                R.id.radioOther -> "Other"
                else -> ""
            }

            // Validate inputs
            if (age != null && height != null && weight != null && gender.isNotEmpty()) {
                // Create a map to hold the user details
                val userDetails = hashMapOf(
                    "age" to age,
                    "height" to height,
                    "weight" to weight,
                    "gender" to gender
                )

                // Assume a username or unique ID for the user
                val username = "username" // This should be dynamically obtained or passed

                // Save user details to Firestore
                db.collection("users").document(username)
                    .set(userDetails)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Details Saved Successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, HealthComplicationActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to Save Details: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please fill out all fields!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
