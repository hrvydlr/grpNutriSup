package com.myapp.grpnutrisup

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var calorieTextView: TextView
    private lateinit var calorieProgressBar: ProgressBar
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind UI elements to variables
        calorieTextView = findViewById(R.id.calories_value)
        calorieProgressBar = findViewById(R.id.calories_progress)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().reference

        // Fetch calorie result from Firebase and update the UI
        fetchCalorieResultFromFirebase()
    }

    private fun fetchCalorieResultFromFirebase() {
        val calorieRef = database.child("calorieresult")

        // Add listener to read data from Firebase
        calorieRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Get the calorie result value from Firebase
                    val calorieResult = snapshot.getValue(Int::class.java)
                    if (calorieResult != null) {
                        // Update the UI with fetched calorie result
                        updateCalorieUI(calorieResult)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
                // Log the error or show a user message
            }
        })
    }

    private fun updateCalorieUI(dailyCaloriesNeeded: Int) {
        // Example current consumed calories (you would replace this with actual data)
        val currentCaloriesConsumed = 500 // This should be dynamic, based on real data

        // Update TextView with the calorie count
        calorieTextView.text = "$currentCaloriesConsumed/$dailyCaloriesNeeded"

        // Update ProgressBar
        calorieProgressBar.max = dailyCaloriesNeeded
        calorieProgressBar.progress = currentCaloriesConsumed
    }
}
