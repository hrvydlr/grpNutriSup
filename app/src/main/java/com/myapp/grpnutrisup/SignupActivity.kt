package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var buttonSignup: Button
    private lateinit var buttonLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize the views
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        buttonSignup = findViewById(R.id.buttonSignup)
        buttonLogin = findViewById(R.id.buttonLogin)

        buttonSignup.setOnClickListener {
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()
            val confirmPassword = editTextConfirmPassword.text.toString()

            if (password == confirmPassword) {
                val db = DatabaseHelper(this)
                val isSuccess = db.addUser(email, password) // Ensure this matches your method
                if (isSuccess > -1) {
                    Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show()

                    // Start UserCollectionActivity
                    val intent = Intent(this, UserCollectionActivity::class.java)
                    startActivity(intent)
                    finish() // Optionally finish this activity to remove it from the back stack
                } else {
                    Toast.makeText(this, "Signup Failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
        }

        buttonLogin.setOnClickListener {
            // Handle login button click, if needed
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
