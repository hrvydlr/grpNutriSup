package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

class EditProfileActivity : AppCompatActivity() {

    private lateinit var editTextAge: EditText
    private lateinit var editTextHeight: EditText
    private lateinit var editTextWeight: EditText
    private lateinit var editTextAllergens: EditText
    private lateinit var editTextWeightGoal: EditText // New weight goal field
    private lateinit var spinnerGoal: Spinner
    private lateinit var spinnerWeeklyWeightChange: Spinner
    private lateinit var spinnerActivityLevel: Spinner  // New spinner for activity level
    private lateinit var buttonSaveProfile: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var gender: String = "male"  // Default to male, but this will be fetched from Firestore

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
        editTextWeightGoal = findViewById(R.id.editTextWeightGoal) // Initialize weight goal field
        spinnerGoal = findViewById(R.id.spinnerGoal)
        spinnerWeeklyWeightChange = findViewById(R.id.spinnerWeeklyWeightChange)
        spinnerActivityLevel = findViewById(R.id.spinnerActivityLevel)  // New spinner for activity level
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
        val activityLevels = arrayOf(
            "Sedentary (little or no exercise)",
            "Lightly Active (light exercise/sports 1-3 days/week)",
            "Moderately Active (moderate exercise/sports 3-5 days/week)",
            "Very Active (hard exercise/sports 6-7 days a week)",
            "Extra Active (very hard exercise/physical job)"
        )

        // Set up the Goal spinner
        val goalAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, goals)
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGoal.adapter = goalAdapter

        // Set up the Weekly Weight Change spinner
        val weeklyWeightChangeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, weeklyWeightChanges)
        weeklyWeightChangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWeeklyWeightChange.adapter = weeklyWeightChangeAdapter

        // Set up the Activity Level spinner
        val activityLevelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, activityLevels)
        activityLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerActivityLevel.adapter = activityLevelAdapter

        // Handle Goal spinner selection to enable/disable fields accordingly
        spinnerGoal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedGoal = parent.getItemAtPosition(position).toString()

                if (selectedGoal == "Maintain Weight") {
                    spinnerWeeklyWeightChange.isEnabled = false // Disable spinner
                    spinnerWeeklyWeightChange.setSelection(0)    // Optionally reset selection
                    editTextWeightGoal.isEnabled = false // Disable weight goal
                    editTextWeightGoal.setText("") // Clear weight goal
                } else {
                    spinnerWeeklyWeightChange.isEnabled = true  // Enable spinner for other goals
                    editTextWeightGoal.isEnabled = true // Enable weight goal
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
                        val activityLevel = document.getString("activityLevel") ?: "Sedentary (little or no exercise)"
                        gender = document.getString("gender") ?: "male"  // Fetch gender

                        val goalIndex = (spinnerGoal.adapter as ArrayAdapter<String>).getPosition(goal)
                        spinnerGoal.setSelection(goalIndex)

                        // Enable or disable Weekly Weight Change and Weight Goal based on goal
                        if (goal == "Maintain Weight") {
                            spinnerWeeklyWeightChange.isEnabled = false
                            editTextWeightGoal.isEnabled = false
                        } else {
                            spinnerWeeklyWeightChange.isEnabled = true
                            editTextWeightGoal.isEnabled = true
                            editTextWeightGoal.setText(document.getDouble("weightGoal")?.toString() ?: "")
                        }

                        val weeklyWeightChangeIndex = when (weeklyWeightChange) {
                            0.25 -> 0
                            0.5 -> 1
                            0.75 -> 2
                            else -> 0
                        }
                        spinnerWeeklyWeightChange.setSelection(weeklyWeightChangeIndex)

                        val activityLevelIndex = (spinnerActivityLevel.adapter as ArrayAdapter<String>).getPosition(activityLevel)
                        spinnerActivityLevel.setSelection(activityLevelIndex)
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
            val weightGoal = editTextWeightGoal.text.toString().toDoubleOrNull() ?: weight // Default to current weight if no goal set

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

            val activityLevel = spinnerActivityLevel.selectedItem.toString()

            // Calculate BMR (Basal Metabolic Rate) based on gender
            val bmr = calculateBMR(age, height, weight, gender)

            // Calculate TDEE (Total Daily Energy Expenditure)
            val tdee = calculateTDEE(bmr, activityLevel)

            // Adjust calories based on weight goal
            val adjustedCalories = adjustCaloriesForGoal(tdee, weight, weightGoal, weeklyWeightChange)

            // Prepare profile data to save
            val profileData = mapOf(
                "age" to age,
                "height" to height,
                "weight" to weight,
                "allergens" to allergensList, // Save as a list
                "goal" to goal,
                "weeklyWeightChange" to if (goal != "Maintain Weight") weeklyWeightChange else 0.0,
                "activityLevel" to activityLevel,
                "gender" to gender,
                "BMR" to bmr,  // Save BMR
                "TDEE" to tdee, // Save TDEE
                "calorieResult" to adjustedCalories, // Save adjusted calorie intake
                "weightGoal" to weightGoal // Save weight goal
            )

            // Update the user profile in Firestore
            db.collection("users").document(userEmail)
                .update(profileData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    // After successful save, go back to the ProfileActivity
                    goToProfileActivity()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun goToProfileActivity() {
        // Create an intent to navigate back to ProfileActivity
        val intent = Intent(this, ProfileActivity::class.java)
        // Set flags to clear the current task and start the new activity fresh
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        // Finish the current EditProfileActivity to prevent going back to it
        finish()
    }

    private fun calculateBMR(age: Int, height: Double, weight: Double, gender: String): Double {
        return if (gender == "male") {
            (10 * weight) + (6.25 * height) - (5 * age) + 5  // BMR formula for males
        } else {
            (10 * weight) + (6.25 * height) - (5 * age) - 161  // BMR formula for females
        }
    }

    // TDEE calculation function (provided in the prompt)
    private fun calculateTDEE(bmr: Double, activityLevel: String): Double {
        return when (activityLevel) {
            "Sedentary (little or no exercise)" -> bmr * 1.2
            "Lightly Active (light exercise/sports 1-3 days/week)" -> bmr * 1.375
            "Moderately Active (moderate exercise/sports 3-5 days/week)" -> bmr * 1.55
            "Very Active (hard exercise/sports 6-7 days a week)" -> bmr * 1.725
            "Extra Active (very hard exercise/physical job)" -> bmr * 1.9
            else -> bmr * 1.2 // Default to Sedentary if activity level is unknown
        }
    }

    // Adjust calories based on weight goal (weekly weight change)
    private fun adjustCaloriesForGoal(tdee: Double, currentWeight: Double, desiredWeight: Double, weeklyWeightChange: Double): Int {
        val dailyCalorieAdjustment: Double

        // If user wants to lose weight
        if (desiredWeight < currentWeight) {
            // Caloric deficit for weight loss (7700 kcal = 1kg of body weight)
            dailyCalorieAdjustment = (7700 * weeklyWeightChange) / 7
            return (tdee - dailyCalorieAdjustment).roundToInt()
        }
        // If user wants to gain weight
        else if (desiredWeight > currentWeight) {
            // Caloric surplus for weight gain (7700 kcal = 1kg of body weight)
            dailyCalorieAdjustment = (7700 * weeklyWeightChange) / 7
            return (tdee + dailyCalorieAdjustment).roundToInt()
        }

        // If the user wants to maintain weight
        return tdee.roundToInt()
    }
}
