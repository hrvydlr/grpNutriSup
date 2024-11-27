package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.adapters.ActivityLogAdapter
import com.myapp.grpnutrisup.models.ActivityLog

class LogActivityPage : AppCompatActivity() {

    private lateinit var activityTypeSpinner: Spinner
    private lateinit var durationInput: EditText
    private lateinit var logActivityButton: Button
    private lateinit var successMessage: TextView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var loggedActivitiesRecyclerView: RecyclerView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var userWeight: Int = 70 // Default weight
    private var tdee: Int = 0
    private var bmr: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.log_activity_page)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Bind views
        activityTypeSpinner = findViewById(R.id.activity_type_spinner)
        durationInput = findViewById(R.id.duration_input)
        logActivityButton = findViewById(R.id.log_activity_button)
        successMessage = findViewById(R.id.success_message)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        loggedActivitiesRecyclerView = findViewById(R.id.logged_activities_recycler_view)

        // Populate Spinner with data from strings.xml
        setupActivityTypeSpinner()

        // Setup bottom navigation
        setupBottomNavigation()

        // Fetch user details
        fetchUserDetails()

        // Fetch logged activities
        fetchLoggedActivities()

        // Set button click listener
        logActivityButton.setOnClickListener { logActivity() }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.navigation_activity
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.navigation_activity -> true
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

    private fun setupActivityTypeSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.activity_types, // Array from strings.xml
            android.R.layout.simple_spinner_item // Default Spinner layout
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            activityTypeSpinner.adapter = adapter
        }
    }

    private fun fetchUserDetails() {
        val currentUser = auth.currentUser ?: return
        val userEmail = currentUser.email ?: return

        firestore.collection("users").document(userEmail).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userWeight = document.getLong("weight")?.toInt() ?: 70
                    bmr = document.getLong("BMR")?.toInt() ?: 0
                    tdee = document.getLong("TDEE")?.toInt() ?: 0
                    Log.d("LogActivityPage", "User details fetched: Weight=$userWeight, BMR=$bmr, TDEE=$tdee")
                } else {
                    Log.w("LogActivityPage", "User document does not exist.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("LogActivityPage", "Failed to fetch user details", e)
            }
    }

    private fun logActivity() {
        val selectedActivity = activityTypeSpinner.selectedItem.toString()
        val durationStr = durationInput.text.toString()

        if (durationStr.isEmpty()) {
            Toast.makeText(this, "Please enter a valid duration.", Toast.LENGTH_SHORT).show()
            return
        }

        val duration = durationStr.toInt()
        val caloriesBurned = calculateCaloriesBurned(selectedActivity, duration)

        if (caloriesBurned > 0) {
            updateCaloriesAndLogActivity(selectedActivity, duration, caloriesBurned)
        } else {
            Toast.makeText(this, "Activity type not supported.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateCaloriesBurned(activity: String, duration: Int): Int {
        val metValues = mapOf(
            "Walk" to 3.5,
            "Run" to 7.5,
            "Cycle" to 6.0,
            "Swim" to 8.0
        )

        val met = metValues[activity] ?: return 0
        return ((met * userWeight * duration) / 60).toInt()
    }

    private fun updateCaloriesAndLogActivity(activity: String, duration: Int, caloriesBurned: Int) {
        val currentUser = auth.currentUser ?: return
        val userEmail = currentUser.email ?: return

        firestore.collection("users").document(userEmail).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentCalories = document.getLong("calorieIntake")?.toInt() ?: 0
                    val updatedCalories = (currentCalories - caloriesBurned).coerceAtLeast(0)

                    firestore.collection("users").document(userEmail)
                        .update("calorieIntake", updatedCalories)
                        .addOnSuccessListener {
                            saveActivityLog(activity, duration, caloriesBurned)
                        }
                        .addOnFailureListener { e ->
                            Log.e("LogActivityPage", "Failed to update calories", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LogActivityPage", "Failed to fetch user data", e)
            }
    }

    private fun saveActivityLog(activity: String, duration: Int, caloriesBurned: Int) {
        val currentUser = auth.currentUser ?: return
        val userEmail = currentUser.email ?: return

        val activityLog = hashMapOf(
            "activity" to activity,
            "duration" to duration,
            "caloriesBurned" to caloriesBurned,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("users").document(userEmail).collection("activityLogs")
            .add(activityLog)
            .addOnSuccessListener {
                successMessage.text = "Activity logged! Calories burned: $caloriesBurned"
                successMessage.visibility = View.VISIBLE
                durationInput.text.clear()
                fetchLoggedActivities() // Refresh activity logs
            }
            .addOnFailureListener { e ->
                Log.e("LogActivityPage", "Failed to log activity", e)
            }
    }

    private fun fetchLoggedActivities() {
        val currentUser = auth.currentUser ?: return
        val userEmail = currentUser.email ?: return

        firestore.collection("users").document(userEmail).collection("activityLogs")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val activities = querySnapshot.documents.map { doc ->
                    ActivityLog(
                        doc.getString("activity") ?: "",
                        doc.getLong("duration")?.toInt() ?: 0,
                        doc.getLong("caloriesBurned")?.toDouble() ?: 0.0,
                        doc.getLong("timestamp") ?: 0L
                    )
                }
                displayLoggedActivities(activities)
            }
            .addOnFailureListener { e ->
                Log.e("LogActivityPage", "Failed to fetch activity logs", e)
            }
    }

    private fun displayLoggedActivities(activities: List<ActivityLog>) {
        loggedActivitiesRecyclerView.layoutManager = LinearLayoutManager(this)
        loggedActivitiesRecyclerView.adapter = ActivityLogAdapter(activities)
    }
}
