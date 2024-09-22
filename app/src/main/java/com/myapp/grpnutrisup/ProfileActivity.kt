package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.*
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
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {  // Start of onCreate method
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)  // Make sure this layout exists

        // Initialize Firebase Auth and Firestore correctly
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()  // Proper Firestore initialization

        // Initialize UI components with proper IDs
        textViewAge = findViewById(R.id.textViewAge)
        textViewHeight = findViewById(R.id.textViewHeight)
        textViewWeight = findViewById(R.id.textViewWeight)
        textViewAllergens = findViewById(R.id.textViewAllergens)
        textViewGoal = findViewById(R.id.textViewGoal)
        textViewWeeklyWeightChange = findViewById(R.id.textViewWeeklyWeightChange)
        buttonChangeProfile = findViewById(R.id.buttonChangeProfile)

        // Load user profile data from Firestore
        loadProfileData()

        // Set click listener for "Change Profile" button
        buttonChangeProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
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
                        textViewAllergens.text = document.getString("allergens") ?: ""
                        textViewGoal.text = document.getString("goal") ?: ""
                        textViewWeeklyWeightChange.text = document.getDouble("weeklyWeightChange")?.toString() ?: ""
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
