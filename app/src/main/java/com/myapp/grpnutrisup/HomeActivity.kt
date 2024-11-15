package com.myapp.grpnutrisup

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.work.*
import com.myapp.grpnutrisup.activities.IntakeHistoryActivity

@Suppress("DEPRECATION")
class HomeActivity : AppCompatActivity() {

    private lateinit var greetingTextView: TextView
    private lateinit var caloriesValueTextView: TextView
    private lateinit var caloriesProgressBar: ProgressBar
    private lateinit var proteinValueTextView: TextView
    private lateinit var fatsValueTextView: TextView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var cardView4: CardView
    private lateinit var historyButton: ImageButton // Added historyButton

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var breakfastView: TextView
    private lateinit var lunchView: TextView
    private lateinit var dinnerView: TextView

    private var hasHealthComplication = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize views
        caloriesValueTextView = findViewById(R.id.calories_value)
        caloriesProgressBar = findViewById(R.id.calories_progress)
        proteinValueTextView = findViewById(R.id.protein_value)
        fatsValueTextView = findViewById(R.id.fats_value)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        cardView4 = findViewById(R.id.cardView4)
        historyButton = findViewById(R.id.historyButton) // Initialize historyButton

        breakfastView = findViewById(R.id.breakfastView)
        lunchView = findViewById(R.id.lunchView)
        dinnerView = findViewById(R.id.dinnerView)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Set up functionalities
        setupHistoryButtonClickListener()
        setupBottomNavigation()
        setupCardViewClickListener()

        fetchUserDataAndUpdateUI()
        fetchMealSelectionsAndDisplay()
        scheduleDailyIntakeReset()
    }

    private fun setupHistoryButtonClickListener() {
        historyButton.setOnClickListener {
            val intent = Intent(this, IntakeHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchMealSelectionsAndDisplay() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val userFoodSelectionsRef = firestore.collection("daily_food_selections").document(userId)

            userFoodSelectionsRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val breakfastList = document.get("Breakfast") as? List<Map<String, Any>> ?: listOf()
                        val lunchList = document.get("Lunch") as? List<Map<String, Any>> ?: listOf()
                        val dinnerList = document.get("Dinner") as? List<Map<String, Any>> ?: listOf()

                        val breakfastFoodNames = breakfastList.map { it["food_name"] as? String ?: " " }
                        val lunchFoodNames = lunchList.map { it["food_name"] as? String ?: " " }
                        val dinnerFoodNames = dinnerList.map { it["food_name"] as? String ?: " " }

                        breakfastView.text = breakfastFoodNames.joinToString(", ")
                        lunchView.text = lunchFoodNames.joinToString(", ")
                        dinnerView.text = dinnerFoodNames.joinToString(", ")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("HomeActivity", "Failed to get food selections", exception)
                }
        }
    }

    private fun setupCardViewClickListener() {
        cardView4.setOnClickListener {
            val intent = Intent(this, MealHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.navigation_search -> {
                    startActivity(Intent(this, FoodSearchActivity::class.java))
                    true
                }
                R.id.navigation_meal -> {
                    if (hasHealthComplication) {
                        showHealthComplicationDialog()
                    } else {
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
            userEmail?.let {
                val userRef = firestore.collection("users").document(it)
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

                            val healthCompStatus = document.getString("healthComp") ?: "no"
                            hasHealthComplication = (healthCompStatus == "yes")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d("HomeActivity", "Failed to fetch user data", exception)
                    }
            }
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

    private fun showHealthComplicationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Health Advisory")
            .setMessage("You have reported a health complication. Please consult a healthcare professional for personalized meal plans.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }.show()
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
            userEmail?.let {
                firestore.collection("users").document(it)
                    .update("calorieIntake", 0, "proteinIntake", 0, "fatIntake", 0)
                    .addOnSuccessListener {
                        Log.d("ResetIntakeWorker", "Intake reset successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ResetIntakeWorker", "Failed to reset intake", e)
                    }
            }
        }
        return Result.success()
    }
}
