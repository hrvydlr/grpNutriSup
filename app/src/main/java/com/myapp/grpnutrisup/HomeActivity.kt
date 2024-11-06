package com.myapp.grpnutrisup

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.work.*

// Suppress deprecation warnings due to usage of old APIs
@Suppress("DEPRECATION")
class HomeActivity : AppCompatActivity() {

    // Declare variables for the meal-related views
    private lateinit var greetingTextView: TextView
    private lateinit var caloriesValueTextView: TextView
    private lateinit var caloriesProgressBar: ProgressBar
    private lateinit var proteinValueTextView: TextView
    private lateinit var fatsValueTextView: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    // Firebase auth and Firestore references
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Views for displaying meal selections
    private lateinit var breakfastView: TextView
    private lateinit var lunchView: TextView
    private lateinit var dinnerView: TextView

    // Variable to track health complication status
    private var hasHealthComplication = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize views
        greetingTextView = findViewById(R.id.greeting)
        caloriesValueTextView = findViewById(R.id.calories_value)
        caloriesProgressBar = findViewById(R.id.calories_progress)
        proteinValueTextView = findViewById(R.id.protein_value)
        fatsValueTextView = findViewById(R.id.fats_value)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        // Initialize new meal-related views
        breakfastView = findViewById(R.id.breakfastView)
        lunchView = findViewById(R.id.lunchView)
        dinnerView = findViewById(R.id.dinnerView)

        // Initialize Firebase auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Fetch user data and update UI
        fetchUserDataAndUpdateUI()
        setupBottomNavigation()

        // Fetch food selections and update meal views
        fetchMealSelectionsAndDisplay()

        // Schedule daily intake reset worker
        scheduleDailyIntakeReset()
    }

    private fun fetchMealSelectionsAndDisplay() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val userFoodSelectionsRef = firestore.collection("daily_food_selections").document(userId)

            // Fetch the food selections for each meal (Breakfast, Lunch, Dinner)
            userFoodSelectionsRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Log the entire document data to inspect it
                        Log.d("HomeActivity", "Fetched document data: ${document.data}")

                        // Retrieve food arrays from Firestore document (assuming food items are objects with a "food_name" field)
                        val breakfastList = document.get("Breakfast") as? List<Map<String, Any>> ?: listOf()
                        val lunchList = document.get("Lunch") as? List<Map<String, Any>> ?: listOf()
                        val dinnerList = document.get("Dinner") as? List<Map<String, Any>> ?: listOf()

                        // Extract only the "food_name" for each item in the list
                        val breakfastFoodNames = breakfastList.map { it["food_name"] as? String ?: "No food selected" }
                        val lunchFoodNames = lunchList.map { it["food_name"] as? String ?: "No food selected" }
                        val dinnerFoodNames = dinnerList.map { it["food_name"] as? String ?: "No food selected" }

                        // Join the food names into a single string, with each name separated by a comma
                        val breakfastText = "${breakfastFoodNames.joinToString(", ")}"
                        val lunchText = "${lunchFoodNames.joinToString(", ")}"
                        val dinnerText = "${dinnerFoodNames.joinToString(", ")}"

                        // Update the TextViews with the food names
                        breakfastView.text = breakfastText
                        lunchView.text = lunchText
                        dinnerView.text = dinnerText
                    } else {
                        Log.d("HomeActivity", "No food selection document found for user $userId")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("HomeActivity", "Failed to get food selections", exception)
                }
        }
    }




    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close the current activity to prevent users from going back to it
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_search -> {
                    startActivity(Intent(this, FoodSearchActivity::class.java))
                    true
                }
                R.id.navigation_meal -> {
                    // Check for health complication
                    if (hasHealthComplication) {
                        // Show dialog if user has a health complication
                        showHealthComplicationDialog()
                    } else {
                        // Otherwise, start MealActivity
                        startActivity(Intent(this, MealActivity::class.java))
                    }
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchUserDataAndUpdateUI() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userEmail = user.email
            if (userEmail != null) {
                val userRef = firestore.collection("users").document(userEmail)

                userRef.get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val calorieGoal = document.getLong("calorieResult")?.toInt() ?: 2000
                            val calorieIntake = document.getLong("calorieIntake")?.toInt() ?: 0

                            caloriesValueTextView.text = "$calorieIntake/$calorieGoal"
                            caloriesProgressBar.max = calorieGoal
                            caloriesProgressBar.progress = calorieIntake

                            val proteinIntake = document.getLong("proteinIntake")?.toInt() ?: 0
                            val fatsIntake = document.getLong("fatIntake")?.toInt() ?: 0

                            proteinValueTextView.text = "$proteinIntake"
                            fatsValueTextView.text = "$fatsIntake"

                            // Check health complication status and update flag
                            val healthCompStatus = document.getString("healthComp") ?: "no"
                            hasHealthComplication = (healthCompStatus == "yes")

                        } else {
                            Log.d("HomeActivity", "No such document")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d("HomeActivity", "Get failed with ", exception)
                    }
            } else {
                Log.d("HomeActivity", "User email is null")
            }
        } ?: run {
            Log.d("HomeActivity", "User is not logged in")
        }
    }

    private fun scheduleDailyIntakeReset() {
        val workRequest = PeriodicWorkRequestBuilder<ResetIntakeWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "dailyIntakeReset",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (currentTime.after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        return targetTime.timeInMillis - currentTime.timeInMillis
    }

    // Function to show health complication dialog
    private fun showHealthComplicationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Health Advisory")
        builder.setMessage("You have reported a health complication. Please consult a healthcare professional for personalized meal plans.")

        // Set the dialog to require OK click
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        // Disable canceling the dialog by tapping outside or pressing the back button
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()
    }
}

// Worker class to reset calorie, protein, and fats intake
class ResetIntakeWorker(appContext: android.content.Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userEmail = user.email
            if (userEmail != null) {
                val userRef = firestore.collection("users").document(userEmail)

                userRef.update(
                    mapOf(
                        "calorieIntake" to 0,
                        "proteinIntake" to 0,
                        "fatIntake" to 0
                    )
                ).addOnSuccessListener {
                    Log.d("ResetIntakeWorker", "Daily intake reset successfully")
                }.addOnFailureListener { e ->
                    Log.e("ResetIntakeWorker", "Failed to reset intake", e)
                }
            }
        }

        return Result.success()
    }
}
