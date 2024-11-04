package com.myapp.grpnutrisup

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserCollectionActivity : AppCompatActivity() {

    private lateinit var editTextAge: EditText
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var editTextHeight: EditText
    private lateinit var editTextWeight: EditText
    private lateinit var buttonSubmit: Button
    private lateinit var buttonBack: Button
    private lateinit var progressBar: ProgressBar

    // Firebase Firestore and Auth instances
    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    // SharedPreferences for saving user details temporarily
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_details)

        // Initialize FirebaseAuth and SharedPreferences
        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("UserDetails", MODE_PRIVATE)

        // Initialize views
        editTextAge = findViewById(R.id.editTextAge)
        radioGroupGender = findViewById(R.id.radioGroupGender)
        editTextHeight = findViewById(R.id.editTextHeight)
        editTextWeight = findViewById(R.id.editTextWeight)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        buttonBack = findViewById(R.id.buttonBack)
        progressBar = findViewById(R.id.progressBar)

        // Set progress bar visibility
        progressBar.visibility = View.VISIBLE // Always show progress bar

        // Update progress bar for the current activity
        updateProgressBar(1) // UserCollection is the 1st page

        // Restore input data
        restoreInputData()

        // Submit button click listener
        buttonSubmit.setOnClickListener {
            saveUserDetails()
        }
    }

    private fun updateProgressBar(currentPageIndex: Int) {
        val totalPages = 6 // Total number of pages
        val progress = (currentPageIndex.toFloat() / totalPages) * 100
        progressBar.progress = progress.toInt()
    }

    private fun saveInputData() {
        val editor = sharedPreferences.edit()
        editor.putString("age", editTextAge.text.toString())
        editor.putString("height", editTextHeight.text.toString())
        editor.putString("weight", editTextWeight.text.toString())
        editor.putInt("gender", radioGroupGender.checkedRadioButtonId)
        editor.apply()
    }

    private fun restoreInputData() {
        val age = sharedPreferences.getString("age", "")
        val height = sharedPreferences.getString("height", "")
        val weight = sharedPreferences.getString("weight", "")
        val genderId = sharedPreferences.getInt("gender", -1)

        editTextAge.setText(age)
        editTextHeight.setText(height)
        editTextWeight.setText(weight)
        if (genderId != -1) {
            radioGroupGender.check(genderId)
        }
    }

    private fun saveUserDetails() {
        // Get the input values
        val age = editTextAge.text.toString().toIntOrNull()
        val height = editTextHeight.text.toString().toFloatOrNull()
        val weight = editTextWeight.text.toString().toFloatOrNull()
        val gender = when (radioGroupGender.checkedRadioButtonId) {
            R.id.radioMale -> "Male"
            R.id.radioFemale -> "Female"
            else -> ""
        }

        // Input validation
        if (age == null) {
            editTextAge.error = "Please enter a valid age"
            return
        }
        if (height == null) {
            editTextHeight.error = "Please enter a valid height"
            return
        }
        if (weight == null) {
            editTextWeight.error = "Please enter a valid weight"
            return
        }
        if (gender.isEmpty()) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the currently signed-in user's email
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not signed in!", Toast.LENGTH_SHORT).show()
            return
        }
        val email = currentUser.email ?: ""

        // Show progress bar while saving details
        progressBar.visibility = View.VISIBLE

        // Create a map to hold the user details
        val userDetails = hashMapOf(
            "age" to age,
            "height" to height,
            "weight" to weight,
            "gender" to gender
        )

        // Save user details to Firestore using the user's email as the document ID
        db.collection("users").document(email)
            .set(userDetails)
            .addOnSuccessListener {
                // Hide progress bar and navigate to the next activity
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Details Saved Successfully!", Toast.LENGTH_SHORT).show()
                navigateToNextScreen()
            }
            .addOnFailureListener { e ->
                // Hide progress bar and show error message
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to Save Details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToNextScreen() {
        // Move to the next screen (HealthComplicationActivity)
        val intent = Intent(this, HealthComplicationActivity::class.java)
        startActivity(intent)
        finish() // Close this activity to prevent going back to it
    }
}
