package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.FoodSearchActivity

class MainActivity : AppCompatActivity() {

    private lateinit var caloriesValueTextView: TextView
    private lateinit var caloriesProgressBar: ProgressBar
    private lateinit var proteinValueTextView: TextView
    private lateinit var proteinProgressBar: ProgressBar
    private lateinit var fatsValueTextView: TextView
    private lateinit var fatsProgressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize views
        caloriesValueTextView = findViewById(R.id.calories_value)
        caloriesProgressBar = findViewById(R.id.calories_progress)
        proteinValueTextView = findViewById(R.id.protein_value)
        proteinProgressBar = findViewById(R.id.protein_progress)
        fatsValueTextView = findViewById(R.id.fats_value)
        fatsProgressBar = findViewById(R.id.fats_progress)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Fetch data from Firebase and update the UI
        fetchUserDataAndUpdateUI()

        // Initialize the Bottom Navigation View
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Set up the navigation item selection listener
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // No action needed as this is the current activity
                    true
                }
                R.id.navigation_search -> {
                    // Start FoodSearchActivity
                    startActivity(Intent(this, FoodSearchActivity::class.java))
                    true
                }
                R.id.navigation_meal -> {
                    // Start MealPlanActivity
                    startActivity(Intent(this, MealPlanActivity::class.java))
                    true
                }
                R.id.navigation_profile -> {
                    // Start ProfileActivity
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchUserDataAndUpdateUI() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userEmail = user.email
            if (userEmail != null) {
                val userRef = firestore.collection("users").document(userEmail)

                // Fetch user data from Firebase Firestore
                userRef.get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val calorieGoal = document.getLong("calorieResult")?.toInt() ?: 2000
                            val calorieIntake = document.getLong("calorieIntake")?.toInt() ?: 600

                            // Update UI
                            caloriesValueTextView.text = "$calorieIntake/$calorieGoal"
                            caloriesProgressBar.max = calorieGoal
                            caloriesProgressBar.progress = calorieIntake

                            val proteinIntake = document.getLong("proteinIntake")?.toInt() ?: 150
                            val proteinGoal = document.getLong("proteinGoal")?.toInt() ?: 800
                            val fatsIntake = document.getLong("fatsIntake")?.toInt() ?: 90
                            val fatsGoal = document.getLong("fatsGoal")?.toInt() ?: 500

                            // Update protein and fats UI
                            proteinValueTextView.text = "$proteinIntake/$proteinGoal"
                            proteinProgressBar.max = proteinGoal
                            proteinProgressBar.progress = proteinIntake

                            fatsValueTextView.text = "$fatsIntake/$fatsGoal"
                            fatsProgressBar.max = fatsGoal
                            fatsProgressBar.progress = fatsIntake
                        } else {
                            Log.d("MainActivity", "No such document")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d("MainActivity", "Get failed with ", exception)
                    }
            } else {
                Log.d("MainActivity", "User email is null")
            }
        } ?: run {
            Log.d("MainActivity", "User is not logged in")
        }
    }
}
