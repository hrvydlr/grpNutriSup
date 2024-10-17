package com.myapp.grpnutrisup

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

@Suppress("DEPRECATION")
class HomeActivity : AppCompatActivity() {

    private lateinit var greetingTextView: TextView
    private lateinit var caloriesValueTextView: TextView
    private lateinit var caloriesProgressBar: ProgressBar
    private lateinit var proteinValueTextView: TextView
    private lateinit var proteinProgressBar: ProgressBar
    private lateinit var fatsValueTextView: TextView
    private lateinit var fatsProgressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // List of quotes
    private val quotes = listOf(
        "The greatest wealth is health.",
        "Take care of your body. It’s the only place you have to live.",
        "Healthy citizens are the greatest asset any country can have.",
        "You are what you eat, so don’t be fast, cheap, easy, or fake.",
        "Health is not about the weight you lose, but about the life you gain.",
        "Your body is your most priceless possession, take care of it."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize views
        greetingTextView = findViewById(R.id.greeting)
        caloriesValueTextView = findViewById(R.id.calories_value)
        caloriesProgressBar = findViewById(R.id.calories_progress)
        proteinValueTextView = findViewById(R.id.protein_value)
        fatsValueTextView = findViewById(R.id.fats_value)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Fetch data from Firebase and update the UI
        fetchUserDataAndUpdateUI()

        // Set a random quote in greeting TextView
        displayRandomQuote()

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
                    startActivity(Intent(this, MealActivity::class.java))
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

    // Display random quote in the greeting TextView
    private fun displayRandomQuote() {
        val randomIndex = Random.nextInt(quotes.size)
        val randomQuote = quotes[randomIndex]
        greetingTextView.text = randomQuote
    }

    @SuppressLint("SetTextI18n")
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
                            val calorieIntake = document.getLong("calorieIntake")?.toInt() ?: 0

                            // Update UI
                            caloriesValueTextView.text = "$calorieIntake/$calorieGoal"
                            caloriesProgressBar.max = calorieGoal
                            caloriesProgressBar.progress = calorieIntake

                            val proteinIntake = document.getLong("proteinIntake")?.toInt() ?: 0
                            val fatsIntake = document.getLong("fatIntake")?.toInt() ?: 0

                            // Update protein and fats UI
                            proteinValueTextView.text = "$proteinIntake"

                            fatsValueTextView.text = "$fatsIntake"

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
