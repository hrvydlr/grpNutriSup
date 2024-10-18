package com.myapp.grpnutrisup

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class HomeActivity : AppCompatActivity() {

    private lateinit var greetingTextView: TextView
    private lateinit var caloriesValueTextView: TextView
    private lateinit var caloriesProgressBar: ProgressBar
    private lateinit var proteinValueTextView: TextView
    private lateinit var fatsValueTextView: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

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

        greetingTextView = findViewById(R.id.greeting)
        caloriesValueTextView = findViewById(R.id.calories_value)
        caloriesProgressBar = findViewById(R.id.calories_progress)
        proteinValueTextView = findViewById(R.id.protein_value)
        fatsValueTextView = findViewById(R.id.fats_value)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        fetchUserDataAndUpdateUI()
        displayRandomQuote()
        setupBottomNavigation()
        scheduleDailyIntakeReset()
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
                    startActivity(Intent(this, MealActivity::class.java))
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

    private fun displayRandomQuote() {
        val randomIndex = Random.nextInt(quotes.size)
        greetingTextView.text = quotes[randomIndex]
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

                            // Check health complication status
                            if (document.getString("healthComp") == "yes") {
                                bottomNavigation.menu.findItem(R.id.navigation_meal).isEnabled = false
                                // Optionally provide a warning to the user
                                // showHealthComplicationDialog() // Uncomment to show a dialog if necessary
                            }
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
