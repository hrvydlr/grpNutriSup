package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
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
    private lateinit var cardView4: CardView
    private lateinit var historyButton: TextView
    private lateinit var progHistoryButton : TextView

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
        historyButton = findViewById(R.id.historyButton)
        progHistoryButton = findViewById(R.id.progHistory)

        breakfastView = findViewById(R.id.breakfastView)
        lunchView = findViewById(R.id.lunchView)
        dinnerView = findViewById(R.id.dinnerView)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Set up functionalities
        setupProressHistoryButtonClickListener()
        setupHistoryButtonClickListener()
        setupBottomNavigation()

        fetchUserDataAndUpdateUI()
        fetchMealSelectionsAndDisplay()
        scheduleDailyIntakeReset()
    }
    private fun setupHistoryButtonClickListener() {
        historyButton.setOnClickListener {
            val intent = Intent(this, MealHistoryActivity::class.java)
            startActivity(intent)
        }
    }
    private fun setupProressHistoryButtonClickListener() {
        progHistoryButton.setOnClickListener {
            val intent = Intent(this, ProgressHistoryActivity::class.java)
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
                        val breakfastList = document.get("Breakfast") as? List<Map<String, Any>> ?: emptyList()
                        val lunchList = document.get("Lunch") as? List<Map<String, Any>> ?: emptyList()
                        val dinnerList = document.get("Dinner") as? List<Map<String, Any>> ?: emptyList()

                        breakfastView.text = breakfastList.map { it["food_name"] as? String ?: " " }.joinToString(", ")
                        lunchView.text = lunchList.map { it["food_name"] as? String ?: " " }.joinToString(", ")
                        dinnerView.text = dinnerList.map { it["food_name"] as? String ?: " " }.joinToString(", ")
                    } else {
                        Log.d("HomeActivity", "No food selections found for user: $userId")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("HomeActivity", "Failed to fetch food selections", exception)
                }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_activity -> {
                    startActivity(Intent(this, LogActivityPage::class.java))
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

    private fun fetchUserDataAndUpdateUI() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userEmail = user.email
            userEmail?.let {
                val userRef = firestore.collection("users").document(it)

                userRef.addSnapshotListener { document, exception ->
                    if (exception != null) {
                        Log.d("HomeActivity", "Failed to listen for changes", exception)
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {
                        val calorieGoal = document.getLong("calorieResult")?.toInt() ?: 2000
                        val calorieIntake = document.getLong("calorieIntake")?.toInt() ?: 0

                        caloriesValueTextView.text = "$calorieIntake/$calorieGoal"
                        caloriesProgressBar.max = calorieGoal
                        caloriesProgressBar.progress = calorieIntake

                        val proteinIntake = document.getLong("proteinIntake")?.toInt() ?: 0
                        val fatsIntake = document.getLong("fatIntake")?.toInt() ?: 0
                        proteinValueTextView.text = proteinIntake.toString()
                        fatsValueTextView.text = fatsIntake.toString()

                        hasHealthComplication = document.getString("healthComp") == "yes"
                    } else {
                        Log.d("HomeActivity", "Document does not exist")
                    }
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
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, MealActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }.show()
    }
}

// Worker class to reset intake
class ResetIntakeWorker(appContext: android.content.Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userEmail = user.email
            if (userEmail != null) {
                try {
                    val userRef = firestore.collection("users").document(userEmail)

                    val document = userRef.get().await()
                    if (document.exists()) {
                        val calorieGoalForToday = document.getLong("calorieGoalForToday")?.toInt() ?: 2000
                        val calorieIntake = document.getLong("calorieIntake")?.toInt() ?: 0
                        val remainingCalories = (calorieGoalForToday - calorieIntake)
                        val calorieGoalForTomorrow = calorieGoalForToday + remainingCalories

                        // Reset the intake values and update calorieGoalForToday and calorieGoalForTomorrow
                        userRef.update(
                            mapOf(
                                "calorieIntake" to 0,
                                "proteinIntake" to 0,
                                "fatIntake" to 0,
                                "remainingCalories" to 0,
                                "calorieGoalForToday" to calorieGoalForTomorrow,
                                "calorieGoalForTomorrow" to calorieGoalForTomorrow
                            )
                        ).await()
                    }
                } catch (e: Exception) {
                    Log.e("ResetIntakeWorker", "Failed to reset intake", e)
                    return Result.failure()
                }
            }
        }
        return Result.success()
    }
}