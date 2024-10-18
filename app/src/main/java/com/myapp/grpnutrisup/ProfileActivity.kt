package com.myapp.grpnutrisup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var textViewAge: TextView
    private lateinit var textViewHeight: TextView
    private lateinit var textViewWeight: TextView
    private lateinit var textViewAllergens: TextView
    private lateinit var textViewGoal: TextView
    private lateinit var textViewWeeklyWeightChange: TextView
    private lateinit var buttonChangeProfile: Button
    private lateinit var buttonLogout: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var hasHealthComplication: Boolean = false // Track health complication status

    private val profileUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadProfileData()  // Refresh profile data on broadcast
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        textViewAge = findViewById(R.id.textViewAge)
        textViewHeight = findViewById(R.id.textViewHeight)
        textViewWeight = findViewById(R.id.textViewWeight)
        textViewAllergens = findViewById(R.id.textViewAllergens)
        textViewGoal = findViewById(R.id.textViewGoal)
        textViewWeeklyWeightChange = findViewById(R.id.textViewWeeklyWeightChange)
        buttonChangeProfile = findViewById(R.id.buttonChangeProfile)
        buttonLogout = findViewById(R.id.buttonLogout)

        // Load user profile data
        loadProfileData()

        // Fetch user health complication status
        checkHealthComplication()

        // Set click listeners for buttons
        buttonChangeProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        buttonLogout.setOnClickListener {
            logoutUser()  // Handle logout
        }

        // Set up Bottom Navigation View
        setupBottomNavigation()
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(profileUpdateReceiver,
            IntentFilter("profile_updated"))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(profileUpdateReceiver)
    }

    private fun loadProfileData() {
        val userEmail = auth.currentUser?.email  // Get the current user's email
        if (userEmail != null) {
            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Populate fields with user's data
                        textViewAge.text = document.getLong("age")?.toString() ?: "N/A"
                        textViewHeight.text = document.getDouble("height")?.toString() ?: "N/A"
                        textViewWeight.text = document.getDouble("weight")?.toString() ?: "N/A"

                        // Handle allergens as a list
                        val allergensList = document.get("allergens") as? List<String> ?: emptyList()
                        textViewAllergens.text = if (allergensList.isNotEmpty()) {
                            allergensList.joinToString(", ") // Join list into a string
                        } else {
                            "None"
                        }

                        textViewGoal.text = document.getString("goal") ?: "N/A"
                        textViewWeeklyWeightChange.text = document.getDouble("weeklyWeightChange")?.toString() ?: "N/A"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    // Method to check if user has health complications
    private fun checkHealthComplication() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return
        }

        val userEmail = currentUser.email ?: return
        db.collection("users").document(userEmail).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val healthComp = document.getString("healthComp") ?: "no"
                    hasHealthComplication = healthComp.equals("yes", ignoreCase = true)

                    // Setup bottom navigation after fetching health complication status
                    setupBottomNavigation()
                } else {
                    // Proceed with default behavior if no user data is found
                    setupBottomNavigation()
                }
            }
            .addOnFailureListener {
                // Proceed with default behavior in case of failure
                setupBottomNavigation()
            }
    }

    // Method to handle logging out
    private fun logoutUser() {
        auth.signOut()  // Sign out the user
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Redirect to the login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  // Clear the back stack
        startActivity(intent)
        finish()
    }

    // Bottom Navigation Setup
    private fun setupBottomNavigation() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    navigateTo(HomeActivity::class.java)
                    true
                }
                R.id.navigation_search -> {
                    navigateTo(FoodSearchActivity::class.java)
                    true
                }
                R.id.navigation_meal -> {
                    if (hasHealthComplication) {
                        // Show the dialog message and prevent access to MealActivity
                        showHealthComplicationDialog()
                        false // Prevent navigation to MealActivity
                    } else {
                        navigateTo(MealActivity::class.java)
                        true
                    }
                }
                R.id.navigation_profile -> {
                    // Already in ProfileActivity, do nothing
                    true
                }
                else -> false
            }
        }
    }

    private fun showHealthComplicationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Health Advisory")
        builder.setMessage("You have reported a health complication. Please consult a healthcare professional for personalized meal plans.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this, destination)
        startActivity(intent)
        finish() // Close current activity to prevent stack buildup
    }
}
