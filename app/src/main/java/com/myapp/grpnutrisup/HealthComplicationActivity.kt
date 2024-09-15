package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HealthComplicationActivity : AppCompatActivity() {

    private lateinit var buttonYes: Button
    private lateinit var buttonNo: Button
    private val db = FirebaseFirestore.getInstance() // Initialize Firestore instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_complication)

        auth = FirebaseAuth.getInstance()

        buttonYes = findViewById(R.id.buttonYes)
        buttonNo = findViewById(R.id.buttonNo)

        buttonYes.setOnClickListener {
            showWarningDialog()
            saveUserResponse("yes") // Call function with "yes" as response
        }

        buttonNo.setOnClickListener {
            saveUserResponse("no") // Call function with "no" as response
        }
    }

    private fun showWarningDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Warning")
        builder.setMessage("Please consult a healthcare provider before proceeding.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun saveUserResponse(response: String) {
        val user = auth.currentUser
        if (user != null) {
            val userEmail = user.email

            if (userEmail != null) {
                // Create a map to hold the user's response with proper type
                val userResponse: Map<String, Any> = hashMapOf(
                    "healthComp" to response
                )

                // Save user response to Firestore
                db.collection("users").document(userEmail)
                    .update(userResponse)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Response Saved Successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, AllergenSelectionActivity::class.java)
                        startActivity(intent)
                        finish() // Optionally close this activity
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to Save Response: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "User email not available!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
        }
    }
}
