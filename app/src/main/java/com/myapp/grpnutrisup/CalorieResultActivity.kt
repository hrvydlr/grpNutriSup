package com.myapp.grpnutrisup

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CalorieResultActivity : AppCompatActivity() {

    private lateinit var textViewCalorieResult: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calorie_result)

        // Initialize FirebaseAuth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        textViewCalorieResult = findViewById(R.id.textViewCalorieResult)

        // Get the calorie result from the intent
        val calorieResult = intent.getIntExtra("calorieResult", 0)

        // Display the calorie result
        textViewCalorieResult.text = "Your daily calorie requirement is: $calorieResult kcal"

        // Save the calorie result to Firestore
        saveCalorieResult(calorieResult)
    }

    private fun saveCalorieResult(calorieResult: Int) {
        val user = auth.currentUser
        if (user != null) {
            val userEmail = user.email

            if (userEmail != null) {
                // Create a map to hold the calorie result
                val calorieData: Map<String, Any> = hashMapOf(
                    "calorieResult" to calorieResult
                )

                // Save the calorie result to the user's document in Firestore using the email as the document ID
                db.collection("users").document(userEmail)
                    .update(calorieData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Calorie result saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save calorie result: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "User email not available!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
        }
    }
}
