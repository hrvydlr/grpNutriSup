package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var textViewAge: TextView
    private lateinit var textViewHeight: TextView
    private lateinit var textViewWeight: TextView
    private lateinit var textViewAllergens: TextView
    private lateinit var textViewGoal: TextView
    private lateinit var textViewWeeklyWeightChange: TextView
    private lateinit var buttonChangeProfile: Button
    private lateinit var buttonLogout: Button  // Added Logout button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        textViewAge = findViewById(R.id.textViewAge)
        textViewHeight = findViewById(R.id.textViewHeight)
        textViewWeight = findViewById(R.id.textViewWeight)
        textViewAllergens = findViewById(R.id.textViewAllergens)
        textViewGoal = findViewById(R.id.textViewGoal)
        textViewWeeklyWeightChange = findViewById(R.id.textViewWeeklyWeightChange)
        buttonChangeProfile = findViewById(R.id.buttonChangeProfile)
        buttonLogout = findViewById(R.id.buttonLogout)  // Initialize the logout button

        // Load user profile data
        loadProfileData()

        // Set click listener for "Change Profile" button
        buttonChangeProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for "Logout" button
        buttonLogout.setOnClickListener {
            logoutUser()  // Handle logout
        }
    }

    private fun loadProfileData() {
        val userEmail = auth.currentUser?.email  // Get the current user's email
        if (userEmail != null) {
            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Populate fields with user's data
                        textViewAge.text = document.getLong("age")?.toString() ?: ""
                        textViewHeight.text = document.getDouble("height")?.toString() ?: ""
                        textViewWeight.text = document.getDouble("weight")?.toString() ?: ""

                        // Handle allergens as a list
                        val allergensList = document.get("allergens") as? List<String> ?: emptyList()
                        textViewAllergens.text = allergensList.joinToString(", ") // Join list into a string

                        textViewGoal.text = document.getString("goal") ?: ""
                        textViewWeeklyWeightChange.text = document.getDouble("weeklyWeightChange")?.toString() ?: ""
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Method to handle logging out
    private fun logoutUser() {
        auth.signOut()  // Sign out the user
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Redirect to the login screen (or any other starting activity)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  // Clear the back stack
        startActivity(intent)
        finish()  // Close the current activity
    }
}
