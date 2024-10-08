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

        // Load user profile data
        loadProfileData()

        // Set click listener for "Change Profile" button
        buttonChangeProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadProfileData() {
        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Directly set the values from Firestore without conversion
                        textViewAge.text = "${document.getLong("age") ?: ""}"
                        textViewHeight.text = "${document.getDouble("height") ?: 0}"
                        textViewWeight.text = "${document.getDouble("weight") ?: 0}"
                        textViewAllergens.text = document.getString("allergens") ?: ""
                        textViewGoal.text = document.getString("goal") ?: ""
                        textViewWeeklyWeightChange.text = "${document.getDouble("weeklyWeightChange") ?: 0}"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
