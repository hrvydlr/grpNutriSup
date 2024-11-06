package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SignupActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var textViewEmailError: TextView
    private lateinit var textViewPasswordError: TextView
    private lateinit var textViewConfirmPasswordError: TextView
    private lateinit var buttonSignup: Button
    private lateinit var buttonLogin: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        textViewEmailError = findViewById(R.id.textViewEmailError)
        textViewPasswordError = findViewById(R.id.textViewPasswordError)
        textViewConfirmPasswordError = findViewById(R.id.textViewConfirmPasswordError)
        buttonSignup = findViewById(R.id.buttonSignup)
        buttonLogin = findViewById(R.id.buttonLogin)

        buttonSignup.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            val confirmPassword = editTextConfirmPassword.text.toString().trim()

            if (validateInput(email, password, confirmPassword)) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            saveUserData(email)

                            val intent = Intent(this, UserCollectionActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            displayEmailError("Signup Failed: ${task.exception?.message}")
                        }
                    }
            }
        }

        buttonLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        // Email validation
        if (email.isEmpty()) {
            displayEmailError("Please enter an email.")
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            displayEmailError("Please enter a valid email.")
            isValid = false
        } else {
            textViewEmailError.visibility = TextView.GONE
        }

        // Password validation
        if (password.isEmpty()) {
            displayPasswordError("Please enter a password.")
            isValid = false
        } else if (password.length < 6) {
            displayPasswordError("Password must be at least 6 characters.")
            isValid = false
        } else {
            textViewPasswordError.visibility = TextView.GONE
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            displayConfirmPasswordError("Please confirm your password.")
            isValid = false
        } else if (confirmPassword != password) {
            displayConfirmPasswordError("Passwords do not match.")
            isValid = false
        } else {
            textViewConfirmPasswordError.visibility = TextView.GONE
        }

        return isValid
    }

    private fun displayEmailError(message: String) {
        textViewEmailError.text = message
        textViewEmailError.visibility = TextView.VISIBLE
    }

    private fun displayPasswordError(message: String) {
        textViewPasswordError.text = message
        textViewPasswordError.visibility = TextView.VISIBLE
    }

    private fun displayConfirmPasswordError(message: String) {
        textViewConfirmPasswordError.text = message
        textViewConfirmPasswordError.visibility = TextView.VISIBLE
    }

    private fun saveUserData(email: String) {
        val createdAt = System.currentTimeMillis()

        val userData = hashMapOf(
            "email" to email,
            "createdAt" to createdAt
        )

        db.collection("users").document(email)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "User data saved to Firestore!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                displayEmailError("Failed to save user data: ${e.message}")
            }
    }
}
