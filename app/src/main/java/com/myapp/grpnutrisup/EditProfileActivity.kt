package com.myapp.grpnutrisup

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var editTextAge: EditText
    private lateinit var editTextHeight: EditText
    private lateinit var editTextWeight: EditText
    private lateinit var editTextAllergens: EditText
    private lateinit var spinnerGoal: Spinner
    private lateinit var spinnerWeeklyWeightChange: Spinner
    private lateinit var buttonSaveProfile: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        editTextAge = findViewById(R.id.editTextAge)
        editTextHeight = findViewById(R.id.editTextHeight)
        editTextWeight = findViewById(R.id.editTextWeight)
        editTextAllergens = findViewById(R.id.editTextAllergens)
        spinnerGoal = findViewById(R.id.spinnerGoal)
        spinnerWeeklyWeightChange = findViewById(R.id.spinnerWeeklyWeightChange)
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile)

        // Populate spinners with goal and weekly weight change options
        setupSpinners()

        // Load user profile data from Firestore
        loadProfileData()

        // Save profile data when button is clicked
        buttonSaveProfile.setOnClickListener {
            saveProfileData()
        }
    }

    private fun setupSpinners() {
        val goals = arrayOf("Maintain Weight", "Lose Weight", "Gain Weight")
        val weeklyWeightChanges = arrayOf("0.25kg", "0.5kg", "0.75kg")

        // Set up the Goal spinner
        val goalAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, goals)
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGoal.adapter = goalAdapter

        // Set up the Weekly Weight Change spinner
        val weeklyWeightChangeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, weeklyWeightChanges)
        weeklyWeightChangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWeeklyWeightChange.adapter = weeklyWeightChangeAdapter

        // Disable Weekly Weight Change spinner if "Maintain Weight" is selected
        spinnerGoal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedGoal = parent.getItemAtPosition(position).toString()
                if (selectedGoal == "Maintain Weight") {
                    spinnerWeeklyWeightChange.isEnabled = false // Disable spinner
                    spinnerWeeklyWeightChange.setSelection(0)    // Optionally reset selection
                } else {
                    spinnerWeeklyWeightChange.isEnabled = true  // Enable spinner for other goals
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed here
            }
        }
    }

    private fun loadProfileData() {
        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Populate fields with user's data
                        editTextAge.setText(document.getLong("age")?.toString() ?: "")
                        editTextHeight.setText(document.getDouble("height")?.toString() ?: "")
                        editTextWeight.setText(document.getDouble("weight")?.toString() ?: "")

                        // Retrieve the allergens as a list of strings
                        val allergensList = document.get("allergens") as? List<String> ?: emptyList()
                        // Join the list into a single string with commas
                        val allergensString = allergensList.joinToString(", ")
                        editTextAllergens.setText(allergensString)

                        // Set spinner values
                        val goal = document.getString("goal") ?: "Maintain Weight"
                        val weeklyWeightChange = document.getDouble("weeklyWeightChange") ?: 0.25

                        val goalIndex = (spinnerGoal.adapter as ArrayAdapter<String>).getPosition(goal)
                        spinnerGoal.setSelection(goalIndex)

                        // Enable or disable Weekly Weight Change based on goal
                        spinnerWeeklyWeightChange.isEnabled = goal != "Maintain Weight"

                        val weeklyWeightChangeIndex = when (weeklyWeightChange) {
                            0.25 -> 0
                            0.5 -> 1
                            0.75 -> 2
                            else -> 0
                        }
                        spinnerWeeklyWeightChange.setSelection(weeklyWeightChangeIndex)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveProfileData() {
        val userEmail = auth.currentUser?.email

        if (userEmail != null) {
            val age = editTextAge.text.toString().toIntOrNull() ?: 0
            val height = editTextHeight.text.toString().toDoubleOrNull() ?: 0.0
            val weight = editTextWeight.text.toString().toDoubleOrNull() ?: 0.0

            // Convert the allergens string back to a list of strings
            val allergensString = editTextAllergens.text.toString()
            val allergensList = allergensString.split(",").map { it.trim() }

            val goal = spinnerGoal.selectedItem.toString()
            val weeklyWeightChange = when (spinnerWeeklyWeightChange.selectedItem.toString()) {
                "0.25kg" -> 0.25
                "0.5kg" -> 0.5
                "0.75kg" -> 0.75
                else -> 0.25
            }

            val profileData = mapOf(
                "age" to age,
                "height" to height,
                "weight" to weight,
                "allergens" to allergensList, // Save as a list
                "goal" to goal,
                "weeklyWeightChange" to if (goal != "Maintain Weight") weeklyWeightChange else 0.0
            )

            // Save profile data to Firestore
            db.collection("users").document(userEmail)
                .set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
