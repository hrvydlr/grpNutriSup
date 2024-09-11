package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class UserCollectionActivity : AppCompatActivity() {

    private lateinit var editTextAge: EditText
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var editTextHeight: EditText
    private lateinit var editTextWeight: EditText
    private lateinit var buttonSubmit: Button

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
                // Save the user details to the database
                val db = DatabaseHelper(this)
                val isSuccess = db.addUserDetails("username", age, gender, height, weight) // Ensure you pass a valid username
                if (isSuccess > -1) {
                    Toast.makeText(this, "Details Saved Successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HealthComplicationActivity::class.java)
                    startActivity(intent)
                    finish()
                    // Redirect to another activity or clear the form after saving
                } else {
                    Toast.makeText(this, "Failed to Save Details", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill out all fields!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
