package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class HealthComplicationActivity : AppCompatActivity() {

    private lateinit var buttonYes: Button
    private lateinit var buttonNo: Button
    private val db = FirebaseFirestore.getInstance() // Initialize Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_complication)

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
        // Assume a username or unique ID for the user
        val username = "username" // This should be dynamically obtained or passed

        // Create a map to hold the user's response with proper type
        val userResponse: Map<String, Any> = hashMapOf(
            "healthComp" to response
        )

        // Save user response to Firestore
        db.collection("users").document(username)
            .update(userResponse)
            .addOnSuccessListener {
                Toast.makeText(this, "Response Saved Successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, GoalSelectionActivity::class.java)
                startActivity(intent)
                finish() // Optionally close this activity
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to Save Response: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
